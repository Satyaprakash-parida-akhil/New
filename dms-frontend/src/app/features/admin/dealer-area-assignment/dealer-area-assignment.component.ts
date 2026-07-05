import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-dealer-area-assignment',
  standalone: true,
  imports: [CommonModule, FormsModule, DataTableComponent, TranslateModule],
  templateUrl: './dealer-area-assignment.component.html',
  styleUrls: ['./dealer-area-assignment.component.css']
})
export class DealerAreaAssignmentComponent implements OnInit {
  
  dealers: any[] = [];
  totalItems = 0;
  isLoading = false;

  // Filter state
  isFilterOpen = false;
  filters = {
    searchTerm: ''
  };

  // Pagination state
  page = 0;
  pageSize = 10;
  sortBy = 'createdAt';
  sortDir = 'desc';

  // Modal State
  isModalOpen = false;
  editingDealer: any = null;
  areaForm = {
    state: '',
    district: '',
    area: '',
    pinCode: ''
  };

  // Table Configuration
  headers: any[] = [
    { key: 'username', label: 'Dealer' },
    { key: 'email', label: 'Email' },
    { key: 'area', label: 'Area' },
    { key: 'state', label: 'State' },
    { key: 'district', label: 'District' },
    { key: 'active', label: 'Status', type: 'boolean_status' },
    { key: 'actions', label: 'Actions', type: 'dealer_area_actions' }
  ];

  constructor(private api: ApiService, private toast: ToastService, private translate: TranslateService) {
    this.initHeaders();
  }

  initHeaders() {
    this.translate.onLangChange.subscribe(() => {
      this.headers = [
        { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },
        { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },
        { key: 'area', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.AREA') },
        { key: 'state', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.STATE') },
        { key: 'district', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.DISTRICT') },
        { key: 'active', label: this.translate.instant('COMMON.STATUS'), type: 'boolean_status' },
        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'dealer_area_actions' }
      ];
    });
  }

  ngOnInit(): void {
    this.headers = [
      { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },
      { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },
      { key: 'area', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.AREA') },
      { key: 'state', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.STATE') },
      { key: 'district', label: this.translate.instant('DEALER_AREA_ASSIGNMENT.DISTRICT') },
      { key: 'active', label: this.translate.instant('COMMON.STATUS'), type: 'boolean_status' },
      { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'dealer_area_actions' }
    ];

    this.loadDealers();
  }

  toggleFilter(): void {
    this.isFilterOpen = !this.isFilterOpen;
  }

  loadDealers(): void {
    this.isLoading = true;
    
    const params = {
      searchTerm: this.filters.searchTerm || null,
      page: this.page,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.api.getAllDealers(params).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        if (res.success) {
          this.dealers = res.data.content;
          this.totalItems = res.data.totalElements;
        }
      },
      error: () => {
        this.isLoading = false;
        this.toast.showError('Failed to load dealers');
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadDealers();
  }

  resetFilters(): void {
    this.filters = { searchTerm: '' };
    this.page = 0;
    this.loadDealers();
  }

  onPageChange(pageIndex: number): void {
    this.page = pageIndex;
    this.loadDealers();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.page = 0;
    this.loadDealers();
  }

  openEditModal(dealer: any): void {
    this.editingDealer = dealer;
    this.areaForm = { 
      state: dealer.state || '', 
      district: dealer.district || '', 
      area: dealer.area || '', 
      pinCode: dealer.pinCode || '' 
    };
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.editingDealer = null;
  }

  saveArea(): void {
    if (!this.editingDealer) return;

    this.api.updateDealerArea(this.editingDealer.id, this.areaForm).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toast.showSuccess('Area assignment updated successfully');
          this.closeModal();
          this.loadDealers();
        } else {
          this.toast.showError('Failed to update area');
        }
      },
      error: () => this.toast.showError('Error updating area')
    });
  }

  onEdit(row: any): void {
    this.openEditModal(row);
  }

  onDelete(row: any): void {
    if (confirm(`Are you sure you want to deactivate the area assignment for dealer ${row.username}?`)) {
      this.api.deleteDealerArea(row.id).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toast.showSuccess('Area assignment deactivated successfully');
            this.loadDealers();
          } else {
            this.toast.showError('Failed to deactivate area');
          }
        },
        error: () => this.toast.showError('Error deactivating area')
      });
    }
  }

  onRecover(row: any): void {
    if (confirm(`Are you sure you want to recover the area assignment for dealer ${row.username}?`)) {
      this.api.recoverDealerArea(row.id).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toast.showSuccess('Area assignment recovered successfully');
            this.loadDealers();
          } else {
            this.toast.showError('Failed to recover area');
          }
        },
        error: () => this.toast.showError('Error recovering area')
      });
    }
  }

  onPermanentDelete(row: any): void {
    if (confirm(`Are you sure you want to PERMANENTLY clear the area assignment for dealer ${row.username}? This cannot be undone.`)) {
      this.api.permanentDeleteDealerArea(row.id).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toast.showSuccess('Area assignment cleared permanently');
            this.loadDealers();
          } else {
            this.toast.showError('Failed to clear area permanently');
          }
        },
        error: () => this.toast.showError('Error clearing area permanently')
      });
    }
  }
}
