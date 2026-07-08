import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NavigationHistoryService {
  private history: string[] = [];
  
  // The pages that are considered the "root" of the app
  // Pressing back on these pages should prompt to exit or exit the app
  readonly ROOT_PAGES = ['/home', '/welcome', '/login', '/'];

  constructor() {}

  push(url: string) {
    // Avoid consecutive duplicate entries
    if (this.history.length === 0 || this.history[this.history.length - 1] !== url) {
      this.history.push(url);
    }
  }

  pop() {
    this.history.pop();
  }

  get canGoBack(): boolean {
    return this.history.length > 1;
  }

  get currentPage(): string {
    return this.history[this.history.length - 1] || '/';
  }

  get isOnRootPage(): boolean {
    const current = this.currentPage.split('?')[0]; // ignore query params
    return this.ROOT_PAGES.some(r => current === r);
  }
}
