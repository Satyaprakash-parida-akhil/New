import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { TABLE_CONFIG } from '../../../shared/config/table-config';

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

    // ── Central Table Config ─────────────────────────────────────────────
    readonly TABLE_CONFIG = TABLE_CONFIG;

    headers: any[] = [];

    constructor(
        private readonly authService: AuthService,
        private readonly toastService: ToastService,
        private readonly translate: TranslateService
    ) { }

    ngOnInit(): void {
        this.updateHeaders();
        this.translate.onLangChange.subscribe(() => {
            this.updateHeaders();
        });
        this.loadRequests();
    }

    updateHeaders() {
        const configKey = this.currentTab === 'active' ? 'PENDING_REGISTRATIONS' : 'ARCHIVED_REGISTRATIONS';
        const cfg = TABLE_CONFIG[configKey];
        if (cfg) {
            this.headers = Object.keys(cfg.desktopColumns).map((key: string) => {
                let label = cfg.desktopColumns[key];
                if (key === 'username') label = this.translate.instant('ADMIN.REGISTRATION.TABLE.USER_DETAILS') || label;
                if (key === 'requestedRole') label = this.translate.instant('ADMIN.REGISTRATION.TABLE.REQUESTED_ROLE') || label;
                if (key === 'createdAt') label = this.translate.instant('ADMIN.REGISTRATION.TABLE.APPLIED_ON') || label;
                if (key === 'registrationStatus') label = this.translate.instant('ADMIN.REGISTRATION.TABLE.STATUS') || label;
                if (key === 'actions') label = this.translate.instant('ADMIN.REGISTRATION.TABLE.ACTIONS') || label;

                return {
                    key,
                    label,
                    type: cfg.columnTypes?.[key]
                };
            });
        } else {
            this.headers = [];
        }
    }

    setTab(tab: 'active' | 'inactive') {
        this.currentTab = tab;
        this.currentPage = 0;
        this.updateHeaders();
        this.loadRequests();
    }

    onTabChange(tab: 'ACTIVE' | 'INACTIVE') {
        this.setTab(tab === 'ACTIVE' ? 'active' : 'inactive');
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
