import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { AuthService } from './core/auth/auth.service';
import { ToastComponent } from './shared/components/toast/toast.component';
import { BreadcrumbComponent } from './shared/components/breadcrumb/breadcrumb.component';
import { filter } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, ToastComponent, BreadcrumbComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'dms-frontend';
  username = 'User';
  roleDisplay = '';
  sidebarOpen = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    public readonly translate: TranslateService
  ) {
    this.translate.setDefaultLang('en');
    this.translate.use('en');

    // Automatically close sidebar on mobile when navigation ends
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.sidebarOpen = false;
    });
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  get isLoginPage(): boolean {
    const publicRoutes = ['/login', '/', '/register', '/forgot-password', '/reset-password'];
    return publicRoutes.includes(this.router.url.split('?')[0]);
  }

  ngOnInit() {
    this.extractUserInfo();
  }

  extractUserInfo() {
    const token = localStorage.getItem('access_token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username = payload.sub || 'User';

        let roles: string[] = [];
        if (payload.roles && Array.isArray(payload.roles)) {
          roles = payload.roles.map((r: any) => r.authority || r);
        }

        // Format role nicely
        if (roles.includes('ROLE_ADMIN')) {
          this.roleDisplay = 'Admin';
        } else if (roles.includes('ROLE_REVIEWER')) {
          this.roleDisplay = 'Reviewer';
        } else if (roles.includes('ROLE_UPLOADER')) {
          this.roleDisplay = 'Uploader';
        }
      } catch (e) {
        console.error('Error parsing token for UI display', e);
      }
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
