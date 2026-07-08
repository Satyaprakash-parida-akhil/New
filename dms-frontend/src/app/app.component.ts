import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd, NavigationStart, NavigationCancel, NavigationError } from '@angular/router';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { AuthService } from './core/auth/auth.service';
import { ToastComponent } from './shared/components/toast/toast.component';
import { BreadcrumbComponent } from './shared/components/breadcrumb/breadcrumb.component';
import { filter } from 'rxjs';
import { TranslateService, TranslateModule } from '@ngx-translate/core';
import { LocalizePipe } from './shared/pipes/localize.pipe';
import { LoggerService } from './core/services/logger.service';
import { NavigationHistoryService } from './core/services/navigation-history.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, ToastComponent, BreadcrumbComponent, TranslateModule, LocalizePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'smart-bazar';
  username = 'User';
  roleDisplay = '';
  sidebarOpen = false;
  isLoading = false;

  // Double-back-to-exit pattern variables
  private lastBackPressTime = 0;
  private readonly EXIT_DELAY = 2000;

  showExitNotification = false;
  isLangDropdownOpen = false;
  dropdownStyle: { [key: string]: string } = {};

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    public readonly translate: TranslateService,
    private readonly logger: LoggerService,
    private readonly navHistory: NavigationHistoryService
  ) {
    const savedLang = localStorage.getItem('app_lang') || 'en';
    this.translate.setDefaultLang('en');
    this.translate.use(savedLang);

    // Show loading overlay on route changes
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.isLoading = true;
      } else if (event instanceof NavigationEnd) {
        this.navHistory.push(event.urlAfterRedirects);
        setTimeout(() => { this.isLoading = false; }, 300);
      } else if (
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        setTimeout(() => { this.isLoading = false; }, 300);
      }
    });

    // Close sidebar on mobile after navigation
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.sidebarOpen = false;
    });
  }

  ngOnInit() {
    this.extractUserInfo();
  }

  @HostListener('window:popstate', ['$event'])
  onPopState(event: Event) {
    this.navHistory.pop();

    // Check if we reached a root page or the history stack is empty
    if (this.navHistory.isOnRootPage || !this.navHistory.canGoBack) {
      const currentTime = new Date().getTime();
      if (currentTime - this.lastBackPressTime < this.EXIT_DELAY) {
        // Double pressed within delay: allow default OS behavior to close the app
        // No action needed, browser history will be popped and OS will close the webview
      } else {
        // First press: prevent closing, show toast
        history.pushState(null, '', location.href); // Push state back so app doesn't close yet
        this.lastBackPressTime = currentTime;
        this.showExitToast();
      }
    }
  }

  private showExitToast() {
    this.showExitNotification = true;
    setTimeout(() => {
      this.showExitNotification = false;
    }, this.EXIT_DELAY);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    this.isLangDropdownOpen = false;
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onWindowResizeOrScroll() {
    this.isLangDropdownOpen = false;
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  get isLoginPage(): boolean {
    const publicRoutes = ['/login', '/welcome', '/', '/register', '/forgot-password', '/reset-password'];
    return publicRoutes.includes(this.router.url.split('?')[0]);
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

        // Format role nicely — values must match COMMON.ROLES keys in i18n files
        if (roles.includes('ROLE_ADMIN')) {
          this.roleDisplay = 'ADMINISTRATOR';
        } else if (roles.includes('ROLE_REVIEWER')) {
          this.roleDisplay = 'REVIEWER';
        } else if (roles.includes('ROLE_DEALER')) {
          this.roleDisplay = 'DEALER';
        } else if (roles.includes('ROLE_UPLOADER')) {
          this.roleDisplay = 'MEMBER';
        }
      } catch (e) {
        this.logger.error('Error parsing token for UI display', e);
      }
    }
  }

  toggleLangDropdown(event: Event) {
    event.stopPropagation();
    this.isLangDropdownOpen = !this.isLangDropdownOpen;
    if (this.isLangDropdownOpen) {
      const button = (event.currentTarget || event.target) as HTMLElement;
      const rect = button.getBoundingClientRect();
      const dropdownWidth = 192; // w-48 = 192px

      // Calculate top and right relative to the viewport — anchor to right edge of button
      const top = rect.bottom + 8;
      const right = window.innerWidth - rect.right;

      this.dropdownStyle = {
        top: `${top}px`,
        right: `${right}px`
      };
    }
  }

  selectLanguage(lang: string) {
    this.translate.use(lang);
    localStorage.setItem('app_lang', lang);
    this.isLangDropdownOpen = false;
  }

  getActiveLangFlag(): string {
    const lang = this.translate.currentLang || 'en';
    if (lang === 'or' || lang === 'te' || lang === 'hi') return '🇮🇳';
    return '🇬🇧';
  }

  getActiveLangName(): string {
    const lang = this.translate.currentLang || 'en';
    if (lang === 'or') return 'ଓଡ଼ିଆ';
    if (lang === 'te') return 'తెలుగు';
    if (lang === 'hi') return 'हिन्दी';
    return 'English';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/welcome']);
  }
}
