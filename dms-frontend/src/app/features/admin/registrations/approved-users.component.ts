import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
    selector: 'app-approved-users',
    standalone: true,
    imports: [CommonModule, TranslateModule, DataTableComponent],
    templateUrl: './approved-users.component.html',
    styleUrl: './pending-registrations.component.css'
})
export class ApprovedUsersComponent implements OnInit {
    users: any[] = [];
    isLoading = false;

    // Pagination State
    currentPage = 0;
    pageSize = 5;
    totalElements = 0;
    totalPages = 0;
    isLast = true;

    headers = [
        { label: 'User Details', key: 'username', type: 'user' },
        { label: 'Email Address', key: 'email' },
        { label: 'Active Role', key: 'roles', type: 'roles' },
        { label: 'Account Created', key: 'createdAt' }
    ];

    constructor(
        private readonly authService: AuthService,
        private readonly toastService: ToastService,
        private readonly translate: TranslateService
    ) { }

    ngOnInit(): void {
        this.loadUsers();
    }

    loadUsers(page: number = this.currentPage) {
        this.isLoading = true;
        this.currentPage = page;

        this.authService.getApprovedUsers(this.currentPage, this.pageSize).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success && response.data) {
                    this.users = response.data.content || [];
                    this.totalElements = response.data.totalElements || 0;
                    this.totalPages = response.data.totalPages || 0;
                    this.isLast = response.data.last ?? true;
                }
            },
            error: () => {
                this.isLoading = false;
                this.users = [];
                this.toastService.showError('Failed to load approved users');
            }
        });
    }

    onPageSizeChange(size: number) {
        this.pageSize = size;
        this.currentPage = 0;
        this.loadUsers();
    }
}
