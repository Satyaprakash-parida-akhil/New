import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { TABLE_CONFIG } from '../../../shared/config/table-config';

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
    activeTab: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';

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
    ) {
        this.updateHeaders();
    }

    updateHeaders() {
        const cfg = TABLE_CONFIG['APPROVED_USERS'];
        if (cfg) {
            this.headers = Object.keys(cfg.desktopColumns).map((key: string) => {
                let label = cfg.desktopColumns[key];
                if (key === 'username') label = this.translate.instant('ADMIN.APPROVED_USERS.TABLE.USER_DETAILS') || label;
                if (key === 'email') label = this.translate.instant('ADMIN.APPROVED_USERS.TABLE.EMAIL') || label;
                if (key === 'roles') label = this.translate.instant('ADMIN.APPROVED_USERS.TABLE.ROLE') || label;
                if (key === 'createdAt') label = this.translate.instant('ADMIN.APPROVED_USERS.TABLE.JOINED') || label;
                return {
                    key,
                    label,
                    type: cfg.columnTypes?.[key]
                };
            });
        }
    }

    ngOnInit(): void {
        this.translate.onLangChange.subscribe(() => {
            this.updateHeaders();
        });
        this.loadUsers();
    }

    onTabChange(tab: 'ACTIVE' | 'INACTIVE') {
        this.activeTab = tab;
        this.currentPage = 0;
        this.loadUsers();
    }

    loadUsers(page: number = this.currentPage) {
        this.isLoading = true;
        this.currentPage = page;

        // Pass active=true for ACTIVE tab, active=false for INACTIVE tab
        const activeFilter = this.activeTab === 'ACTIVE' ? true : false;

        this.authService.getApprovedUsers(this.currentPage, this.pageSize, activeFilter).subscribe({
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
