import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly apiUrl = `${environment.apiUrl}/auth`;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) { }

  login(credentials: { username: string; password: string }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response.success && response.data?.accessToken) {
          localStorage.setItem('access_token', response.data.accessToken);
          // Optional: decode roles and store them in memory/state
        }
      })
    );
  }

  logout(): void {
    localStorage.clear();
    sessionStorage.clear();
    this.router.navigate(['/login']).then(() => {
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
      console.error('Error decoding token', e);
      return null;
    }
  }
}
