import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
    selector: 'app-pending-registrations',
    standalone: true,
    imports: [CommonModule, TranslateModule, DataTableComponent],
    templateUrl: './pending-registrations.component.html',
    styleUrl: './pending-registrations.component.css'
})
export class PendingRegistrationsComponent implements OnInit {
    requests: any[] = [];
    isLoading = false;
    currentTab: 'active' | 'inactive' = 'active';

    // Pagination State
    currentPage = 0;
    pageSize = 5;
    totalElements = 0;
    totalPages = 0;
    isLast = true;

    headers = [
        { label: 'User Details', key: 'username', type: 'user' },
        { label: 'Requested Role', key: 'requestedRole' },
        { label: 'Applied On', key: 'createdAt' },
        { label: 'Status', key: 'registrationStatus', type: 'status' },
        { label: 'Actions', key: 'actions', type: 'registration_actions' }
    ];

    constructor(
        private readonly authService: AuthService,
        private readonly toastService: ToastService,
        private readonly translate: TranslateService
    ) { }

    ngOnInit(): void {
        this.updateHeaders();
        this.loadRequests();
    }

    updateHeaders() {
        if (this.currentTab === 'active') {
            this.headers = [
                { label: 'User Details', key: 'username', type: 'user' },
                { label: 'Requested Role', key: 'requestedRole' },
                { label: 'Applied On', key: 'createdAt' },
                { label: 'Status', key: 'registrationStatus', type: 'status' },
                { label: 'Actions', key: 'actions', type: 'registration_actions' }
            ];
        } else {
            this.headers = [
                { label: 'User Details', key: 'username', type: 'user' },
                { label: 'Requested Role', key: 'requestedRole' },
                { label: 'Applied On', key: 'createdAt' },
                { label: 'Status', key: 'registrationStatus', type: 'status' },
                { label: 'Actions', key: 'actions', type: 'archived_actions' }
            ];
        }
    }

    setTab(tab: 'active' | 'inactive') {
        this.currentTab = tab;
        this.currentPage = 0; // Reset page on tab change
        this.updateHeaders();
        this.loadRequests();
    }

    loadRequests(page: number = this.currentPage) {
        this.isLoading = true;
        this.currentPage = page;

        const obs = this.currentTab === 'active'
            ? this.authService.getPendingRegistrations(this.currentPage, this.pageSize)
            : this.authService.getInactiveRegistrations(this.currentPage, this.pageSize);

        obs.subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success && response.data) {
                    this.requests = response.data.content || [];
                    this.totalElements = response.data.totalElements || 0;
                    this.totalPages = response.data.totalPages || 0;
                    this.isLast = response.data.last ?? true;
                }
            },
            error: () => {
                this.isLoading = false;
                this.requests = [];
            }
        });
    }

    onPageSizeChange(size: number) {
        this.pageSize = size;
        this.currentPage = 0;
        this.loadRequests();
    }

    onApprove(user: any) {
        this.authService.approveRegistration(user.id, user.requestedRole).subscribe({
            next: (response) => {
                if (response.success) {
                    this.toastService.showSuccess(this.translate.instant('ADMIN.REGISTRATION.MESSAGES.APPROVE_SUCCESS'));
                    this.loadRequests();
                }
            }
        });
    }

    onReject(user: any) {
        if (confirm(this.translate.instant('ADMIN.REGISTRATION.ACTIONS.CONFIRM_REJECT'))) {
            this.authService.rejectRegistration(user.id).subscribe({
                next: (response) => {
                    if (response.success) {
                        this.toastService.showSuccess(this.translate.instant('ADMIN.REGISTRATION.MESSAGES.REJECT_SUCCESS'));
                        this.loadRequests();
                    }
                }
            });
        }
    }

    onArchive(user: any) {
        if (confirm('Are you sure you want to archive this request?')) {
            this.authService.softDeleteRegistration(user.id).subscribe({
                next: (response) => {
                    if (response.success) {
                        this.toastService.showSuccess('Request archived');
                        this.loadRequests();
                    }
                }
            });
        }
    }

    onRestore(user: any) {
        this.authService.restoreRegistration(user.id).subscribe({
            next: (response) => {
                if (response.success) {
                    this.toastService.showSuccess('Request restored to active');
                    this.loadRequests();
                }
            }
        });
    }
}
