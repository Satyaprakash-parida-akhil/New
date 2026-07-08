import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ApiService } from '../services/api.service';
import { LoggerService } from '../services/logger.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(
    private readonly apiService: ApiService,
    private readonly router: Router,
    private readonly logger: LoggerService
  ) { }

  login(credentials: { username: string; password: string }): Observable<ApiResponse<any>> {
    return this.apiService.login(credentials).pipe(
      tap(response => {
        if (response.success && response.data?.accessToken) {
          localStorage.setItem('access_token', response.data.accessToken);
        }
      })
    );
  }

  logout(): void {
    localStorage.clear();
    sessionStorage.clear();
    this.router.navigate(['/welcome']).then(() => {
      globalThis.location.reload();
    });
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('access_token');
  }

  getUserProfile(): any {
    const token = localStorage.getItem('access_token');
    if (!token) return null;
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      this.logger.error('Error decoding token', e);
      return null;
    }
  }

  register(userData: any, photo?: File): Observable<ApiResponse<any>> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(userData)], { type: 'application/json' }));
    if (photo) {
      formData.append('photo', photo);
    }
    return this.apiService.register(formData);
  }

  sendRegisterOtp(identifier: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.apiService.sendRegisterOtp(identifier, method);
  }

  verifyRegisterOtp(identifier: string, otp: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.apiService.verifyRegisterOtp(identifier, otp, method);
  }

  forgotPassword(identifier: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.apiService.forgotPassword(identifier, method);
  }

  verifyForgotPasswordOtp(identifier: string, otp: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.apiService.verifyForgotPasswordOtp(identifier, otp, method);
  }

  resetPassword(data: any): Observable<ApiResponse<any>> {
    return this.apiService.resetPassword(data);
  }

  // Admin Registration Management
  getPendingRegistrations(page: number = 0, size: number = 5): Observable<ApiResponse<any>> {
    return this.apiService.getPendingRegistrations(page, size);
  }

  getInactiveRegistrations(page: number = 0, size: number = 5): Observable<ApiResponse<any>> {
    return this.apiService.getInactiveRegistrations(page, size);
  }

  getApprovedUsers(page: number = 0, size: number = 5, active?: boolean): Observable<ApiResponse<any>> {
    return this.apiService.getApprovedUsers(page, size, active);
  }

  approveRegistration(userId: number, role: string): Observable<ApiResponse<any>> {
    return this.apiService.approveRegistration(userId, role);
  }

  rejectRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.apiService.rejectRegistration(userId);
  }

  softDeleteRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.apiService.softDeleteRegistration(userId);
  }

  restoreRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.apiService.restoreRegistration(userId);
  }

  refreshToken(): Observable<ApiResponse<any>> {
    return this.apiService.refreshToken().pipe(
      tap(res => {
        if (res.success && res.data?.accessToken) {
          localStorage.setItem('access_token', res.data.accessToken);
        }
      })
    );
  }
}

