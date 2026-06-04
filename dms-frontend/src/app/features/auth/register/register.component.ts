import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  userData = {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    requestedRole: 'ROLE_UPLOADER',
    otpCode: ''
  };

  step: 'EMAIL' | 'OTP' | 'DETAILS' = 'EMAIL';
  isLoading = false;
  showPassword = false;
  otpSent = false;

  constructor(
    private readonly authService: AuthService,
    private readonly toastService: ToastService,
    private readonly router: Router
  ) { }

  sendOtp() {
    if (!this.userData.email || !this.userData.email.includes('@gmail.com')) {
      this.toastService.showError('Please enter a valid Gmail address');
      return;
    }
    this.isLoading = true;
    this.authService.sendRegisterOtp(this.userData.email).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.otpSent = true;
        this.step = 'OTP';
        this.toastService.showSuccess('OTP sent to your email');
      },
      error: (err) => {
        this.isLoading = false;
      }
    });
  }

  verifyOtp() {
    if (!this.userData.otpCode || this.userData.otpCode.length !== 6) {
      this.toastService.showError('Please enter a valid 6-digit OTP');
      return;
    }
    this.isLoading = true;
    this.authService.verifyRegisterOtp(this.userData.email, this.userData.otpCode).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.step = 'DETAILS';
        this.toastService.showSuccess('OTP verified successfully');
      },
      error: (err) => {
        this.isLoading = false;
      }
    });
  }

  onSubmit() {
    if (this.userData.password.length < 6) {
      this.toastService.showError('Password must be at least 6 characters long');
      return;
    }
    if (this.userData.password !== this.userData.confirmPassword) {
      this.toastService.showError('Passwords do not match');
      return;
    }

    this.isLoading = true;
    const { confirmPassword, ...registerData } = this.userData;

    this.authService.register(registerData).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.toastService.showSuccess(response.message || 'Registration successful!');
          this.router.navigate(['/login']);
        } else {
          this.toastService.showError(response.message || 'Registration failed');
        }
      },
      error: (error) => {
        this.isLoading = false;
      }
    });
  }
}
