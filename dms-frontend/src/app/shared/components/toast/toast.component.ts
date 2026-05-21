import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastMessage, ToastService } from '../../../core/services/toast.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-5 right-5 z-[100] flex flex-col gap-3 w-80 pointer-events-none">
      @for (toast of activeToasts; track toast.id) {
        <div 
          class="pointer-events-auto p-4 rounded-lg shadow-xl border-l-4 transform transition-all duration-300 translate-x-0 animate-slide-in"
          [ngClass]="{
            'bg-green-50 border-green-500 text-green-800': toast.type === 'success',
            'bg-red-50 border-red-500 text-red-800': toast.type === 'error',
            'bg-amber-50 border-amber-500 text-amber-800': toast.type === 'warning',
            'bg-blue-50 border-blue-500 text-blue-800': toast.type === 'info'
          }"
        >
          <div class="flex items-start">
            <div class="flex-shrink-0">
                @if(toast.type === 'success') {
                    <svg class="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                } @else if (toast.type === 'error') {
                    <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                    </svg>
                }
            </div>
            <div class="ml-3">
              <p class="text-sm font-bold">{{ toast.title }}</p>
              <p class="text-xs mt-1 opacity-90">{{ toast.message }}</p>
            </div>
            <div class="ml-auto pl-3">
              <button (click)="remove(toast.id!)" class="inline-flex text-gray-400 hover:text-gray-500 focus:outline-none">
                <span class="sr-only">Close</span>
                <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .animate-slide-in {
      animation: slideIn 0.3s ease-out forwards;
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateX(100%); }
      to { opacity: 1; transform: translateX(0); }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  activeToasts: ToastMessage[] = [];
  private subscription: Subscription | undefined;

  constructor(private readonly toastService: ToastService) { }

  ngOnInit() {
    this.subscription = this.toastService.toasts$.subscribe(toast => {
      this.activeToasts.push(toast);
      if (toast.id) {
        setTimeout(() => this.remove(toast.id!), 5000);
      }
    });
  }

  remove(id: number) {
    this.activeToasts = this.activeToasts.filter(t => t.id !== id);
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }
}
