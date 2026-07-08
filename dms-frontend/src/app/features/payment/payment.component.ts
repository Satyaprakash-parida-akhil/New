import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  PaymentService,
  PaymentRecord,
  SubscriptionPlan,
  RazorpayOrderResponse
} from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/auth/auth.service';

/** Razorpay JS SDK is loaded via script tag; declare globally */
declare const Razorpay: any;

type Step = 'plans' | 'checkout' | 'status';

import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css'
})
export class PaymentComponent implements OnInit {

  // ── State ────────────────────────────────────────────────────────────────
  step: Step = 'plans';

  plans: SubscriptionPlan[] = [];
  selectedPlan: SubscriptionPlan | null = null;
  existingPayment: PaymentRecord | null = null;

  isLoadingPlans = false;
  isLoadingPayment = false;
  isCreatingOrder = false;
  isSubmitting = false;

  /** Sandbox simulator fields (shown when no Razorpay key) */
  sandbox = {
    cardNumber: '',
    expiry: '',
    cvv: '',
    name: ''
  };

  /** Plan badge colours */
  readonly planThemes: Record<string, { border: string; badge: string; btn: string; glow: string }> = {
    'Starter':    { border: 'border-slate-500/40',  badge: 'bg-slate-700 text-slate-300',          btn: 'from-slate-600 to-slate-700',     glow: 'shadow-slate-500/20' },
    'Pro':        { border: 'border-indigo-500/50',  badge: 'bg-indigo-600 text-white',             btn: 'from-indigo-600 to-violet-600',   glow: 'shadow-indigo-500/30' },
    'Enterprise': { border: 'border-amber-500/50',   badge: 'bg-gradient-to-r from-amber-500 to-orange-500 text-white', btn: 'from-amber-500 to-orange-600', glow: 'shadow-amber-500/30' }
  };

  isUserPaid = false;

  constructor(
    private readonly paymentService: PaymentService,
    private readonly toastService: ToastService,
    private readonly router: Router,
    private readonly profileService: ProfileService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadMyPayment();
  }

  // ── Loaders ───────────────────────────────────────────────────────────────

  loadMyPayment(): void {
    this.isLoadingPayment = true;
    this.profileService.getMyProfile().subscribe({
      next: (profileRes) => {
        const isProfilePaid = profileRes.success && profileRes.data?.paymentStatus === 'COMPLETED';
        this.fetchPaymentRecord(isProfilePaid);
      },
      error: () => {
        this.fetchPaymentRecord(false);
      }
    });
  }

  private fetchPaymentRecord(isProfilePaid: boolean): void {
    this.paymentService.getMyPayment().subscribe({
      next: (res: ApiResponse<PaymentRecord>) => {
        this.isLoadingPayment = false;
        if (res.success && res.data) {
          this.existingPayment = res.data;
          this.isUserPaid = isProfilePaid || (this.existingPayment.status === 'APPROVED' || (this.existingPayment.status as any) === 'COMPLETED');
          this.step = 'status';
        } else {
          this.isUserPaid = isProfilePaid;
          this.loadPlans();
        }
      },
      error: () => {
        this.isLoadingPayment = false;
        this.isUserPaid = isProfilePaid;
        this.loadPlans();
      }
    });
  }

  loadPlans(): void {
    this.isLoadingPlans = true;
    this.paymentService.getPlans().subscribe({
      next: (res: ApiResponse<any[]>) => {
        this.isLoadingPlans = false;
        this.plans = (res.data ?? []).map((p: any) => {
          let featuresArray: string[] = [];
          if (p.features) {
            if (Array.isArray(p.features)) {
              featuresArray = p.features;
            } else if (typeof p.features === 'string') {
              featuresArray = p.features.split(',').map((f: string) => f.trim());
            }
          }
          const durationMonths = p.durationMonths || Math.round((p.durationDays || 30) / 30);
          return {
            id: p.id,
            name: p.name,
            description: p.description,
            price: p.price,
            durationMonths: durationMonths,
            features: featuresArray
          } as SubscriptionPlan;
        });
        this.step = 'plans';
      },
      error: () => {
        this.isLoadingPlans = false;
        this.toastService.showError('Failed to load subscription plans.');
      }
    });
  }

  // ── Plan Selection ────────────────────────────────────────────────────────

  selectPlan(plan: SubscriptionPlan): void {
    this.selectedPlan = plan;
    this.step = 'checkout';
    this.sandbox = { cardNumber: '', expiry: '', cvv: '', name: '' };
  }

  selectSinglePlan(): void {
    // Attempt to use the first plan from the backend (which should be the 2000 plan)
    if (this.plans && this.plans.length > 0) {
      this.selectedPlan = this.plans[0];
    } else {
      // Fallback if backend hasn't loaded it properly
      this.selectedPlan = {
        id: 1, // Assume 1 is the ID of the Fresh Member plan
        name: 'Fresh Member',
        description: 'Activate your direct membership to unlock the initial benefits package.',
        price: 2000,
        durationMonths: 1,
        features: []
      } as SubscriptionPlan;
    }
    this.step = 'checkout';
    this.sandbox = { cardNumber: '', expiry: '', cvv: '', name: '' };
  }

  backToPlans(): void {
    this.selectedPlan = null;
    this.step = 'plans';
  }

  // ── Payment ───────────────────────────────────────────────────────────────

  pay(): void {
    if (!this.selectedPlan) return;

    if (!this.sandbox.name.trim()) {
      this.toastService.showError('Please enter the cardholder name.');
      return;
    }
    if (this.sandbox.cardNumber.replace(/\s/g, '').length < 16) {
      this.toastService.showError('Please enter a valid 16-digit card number.');
      return;
    }

    this.isCreatingOrder = true;
    this.paymentService.createOrder(this.selectedPlan.id).subscribe({
      next: (res: ApiResponse<RazorpayOrderResponse>) => {
        this.isCreatingOrder = false;
        if (!res.success || !res.data) {
          this.toastService.showError('Failed to create order. Please try again.');
          return;
        }
        const order = res.data;

        // If real Razorpay key provided, launch SDK; otherwise run sandbox flow
        if (order.keyId && !order.keyId.startsWith('rzp_mock')) {
          this.launchRazorpay(order);
        } else {
          this.simulatePayment(order);
        }
      },
      error: (err: any) => {
        this.isCreatingOrder = false;
        this.toastService.showError(err.error?.message || 'Order creation failed.');
      }
    });
  }

  /** Launch the real Razorpay checkout SDK */
  private launchRazorpay(order: RazorpayOrderResponse): void {
    const options = {
      key: order.keyId,
      amount: order.amount * 100, // Razorpay SDK expects amount in paise
      currency: order.currency,
      name: 'DocVault Membership',
      description: order.planName,
      order_id: order.orderId,
      handler: (response: any) => {
        this.submitVerifiedPayment(order, response.razorpay_payment_id, response.razorpay_signature);
      },
      prefill: {},
      theme: { color: '#6366f1' },
      modal: { ondismiss: () => this.toastService.showError('Payment cancelled.') }
    };

    const rzp = new Razorpay(options);
    rzp.open();
  }

  /** Theme-matched sandbox simulator for demo / no-key mode */
  private simulatePayment(order: RazorpayOrderResponse): void {
    this.isSubmitting = true;
    // Simulate a 1.5s "processing" delay then submit with mock IDs
    setTimeout(() => {
      const mockPaymentId = `pay_mock_${Date.now()}`;
      const mockSignature = `sig_mock_${Date.now()}`;
      this.submitVerifiedPayment(order, mockPaymentId, mockSignature);
    }, 1500);
  }

  /** After Razorpay callback (or sandbox) — submit to backend */
  private submitVerifiedPayment(
    order: RazorpayOrderResponse,
    paymentId: string,
    signature: string
  ): void {
    this.isSubmitting = true;
    this.paymentService.submitPayment({
      subscriptionPlanId: order.planId,
      amount: order.amount, // backend expects rupees, no division by 100
      paymentMethod: 'ONLINE',
      razorpayOrderId: order.orderId,
      razorpayPaymentId: paymentId,
      razorpaySignature: signature
    }).subscribe({
      next: (res: ApiResponse<PaymentRecord>) => {
        this.isSubmitting = false;
        if (res.success) {
          this.existingPayment = res.data!;
          this.step = 'status';
          this.toastService.showSuccess('🎉 Payment successful! Your account is now active.');
          // Refresh token so isActive claim reflects the newly activated account,
          // then redirect the user to the home/dashboard page.
          this.authService.refreshToken().subscribe({
            next: () => {
              setTimeout(() => this.router.navigate(['/home']), 1500);
            },
            error: () => {
              // Even if token refresh fails, still navigate to home after a short delay
              setTimeout(() => this.router.navigate(['/home']), 1500);
            }
          });
        }
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.toastService.showError(err.error?.message || 'Payment submission failed.');
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  theme(planName: string) {
    if (!planName) return this.planThemes['Starter'];
    const lower = planName.toLowerCase();
    if (lower.includes('starter')) return this.planThemes['Starter'];
    if (lower.includes('pro')) return this.planThemes['Pro'];
    if (lower.includes('enterprise')) return this.planThemes['Enterprise'];
    return this.planThemes['Starter'];
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30';
      case 'REJECTED': return 'text-rose-400 bg-rose-500/10 border-rose-500/30';
      default:         return 'text-amber-400 bg-amber-500/10 border-amber-500/30';
    }
  }

  formatCard(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '').substring(0, 16);
    this.sandbox.cardNumber = value.replace(/(.{4})/g, '$1 ').trim();
  }

  formatExpiry(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '').substring(0, 4);
    if (value.length >= 2) value = value.substring(0, 2) + '/' + value.substring(2);
    this.sandbox.expiry = value;
  }

  resubmit(): void {
    this.existingPayment = null;
    this.selectedPlan = null;
    this.loadPlans();
  }
}
