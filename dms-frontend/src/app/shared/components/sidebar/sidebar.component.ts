import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {
  menus = [
    { label: 'Home', path: '/home', roles: ['ROLE_ADMIN', 'ROLE_UPLOADER', 'ROLE_REVIEWER'], icon: 'home' },
    { label: 'Dashboard', path: '/dashboard', roles: ['ROLE_ADMIN', 'ROLE_UPLOADER', 'ROLE_REVIEWER'], icon: 'grid' },
    { label: 'Upload Document', path: '/docs/upload', roles: ['ROLE_ADMIN', 'ROLE_UPLOADER'], icon: 'upload' },
    { label: 'Review Queue', path: '/reviews/pending', roles: ['ROLE_ADMIN', 'ROLE_REVIEWER'], icon: 'check-square' },
    { label: 'Assign Document', path: '/admin/assign', roles: ['ROLE_ADMIN'], icon: 'user-plus' },
    { label: 'User Requests', path: '/admin/registrations', roles: ['ROLE_ADMIN'], icon: 'users' },
    { label: 'Approved Users', path: '/admin/users', roles: ['ROLE_ADMIN'], icon: 'user-check' }
  ];

  allowedMenus: any[] = [];

  ngOnInit() {
    this.filterMenus();
  }

  filterMenus() {
    const token = localStorage.getItem('access_token');
    let userRoles: string[] = [];
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (payload && payload.roles && Array.isArray(payload.roles)) {
          userRoles = payload.roles.map((r: any) => r.authority || r);
        }
      } catch (error) {
        console.error('Error parsing token in sidebar:', error);
      }
    }
    this.allowedMenus = this.menus.filter(m => m.roles.some(role => userRoles.includes(role)));
  }
}
