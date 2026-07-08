import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class ActiveGuard implements CanActivate {

  constructor(private readonly router: Router) {}

  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // Always allow access to /payment so inactive users can complete payment
    if (state.url.startsWith('/payment')) {
      const token = localStorage.getItem('access_token');
      if (!token) {
        this.router.navigate(['/welcome']);
        return false;
      }
      return true;
    }

    const token = localStorage.getItem('access_token');

    if (!token) {
      this.router.navigate(['/welcome']);
      return false;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));

      // Check if the token has an isActive claim; if missing, allow (legacy tokens)
      if (payload.isActive === false) {
        this.router.navigate(['/payment']);
        return false;
      }

      return true;
    } catch {
      this.router.navigate(['/welcome']);
      return false;
    }
  }
}
