import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TranslateService } from '@ngx-translate/core';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { MessageConstants } from '../../../../core/constants/message.constants';
import { FileUploadComponent } from '../../../../shared/components/file-upload/file-upload.component';
import { TranslateModule } from '@ngx-translate/core';
import { AppSelectComponent } from '../../../../shared/components/app-select/app-select.component';
import { AppMultiSelectComponent } from '../../../../shared/components/app-multi-select/app-multi-select.component';

@Component({
  selector: 'app-dealer-assignment',
  standalone: true,
  imports: [CommonModule, FormsModule, DataTableComponent, TranslateModule, AppSelectComponent, AppMultiSelectComponent],
  templateUrl: './dealer-assignment.component.html',
  styleUrls: ['./dealer-assignment.component.css']
})
export class DealerAssignmentComponent implements OnInit {
  
  assignments: any[] = [];
  totalItems = 0;
  isLoading = false;

  // Dropdown data
  dealers: any[] = [];
  products: any[] = [];
  dealerOptions: any[] = [];
  productOptions: any[] = [];
  dealerSearchOptions: any[] = [];
  productSearchOptions: any[] = [];
  areaSearchOptions: any[] = [];
  states: any[] = [];
  districts: any[] = [];
  blocks: any[] = [];
  pinCodes: any[] = [];
  availableUsers: any[] = [];
  availableUserOptions: any[] = [];

  // Filter state
  isFilterOpen = false;
  filters = {
    searchTerm: '',
    dealerName: '',
    area: '',
    productName: '',
    status: ''
  };

  statusOptions = [
    { id: '', name: 'All Statuses' },
    { id: 'ACTIVE', name: 'Active' },
    { id: 'INACTIVE', name: 'Inactive' }
  ];

  // Pagination state
  page = 0;
  pageSize = 10;
  sortBy = 'createdAt';
  sortDir = 'desc';

  // Modal State
  isModalOpen = false;
  isExporting = false;
  editingAssignment: any = null;
  selectedDealer: any = null;
  selectedDealerMetrics: any = null;
  isLoadingMetrics = false;

  assignmentForm = {
    dealerId: null,
    productIds: [] as number[],
    targetQuantity: 0,
    state: '',
    district: '',
    block: '',
    pinCode: '',
    assignedUserIds: [] as number[]
  };

  // Table Configuration
  headers: any[] = [
    { key: 'dealerUsername', label: 'Dealer' },
    { key: 'state', label: 'State' },
    { key: 'district', label: 'District' },
    { key: 'block', label: 'Block' },
    { key: 'pinCode', label: 'Pin Code' },
    { key: 'assignedUsersCount', label: 'Assigned Users' },
    { key: 'status', label: 'Status', type: 'status' },
    { key: 'actions', label: 'Actions', type: 'master_detail_actions' }
  ];

  constructor(private api: ApiService, private toast: ToastService, private translate: TranslateService) {
    this.initHeaders();
  }

  initHeaders() {
    this.translate.onLangChange.subscribe(() => {
      this.headers = [
        { key: 'dealerUsername', label: this.translate.instant('DEALER_ASSIGNMENT.DEALER_NAME') },
        { key: 'state', label: this.translate.instant('DEALER_ASSIGNMENT.STATE') },
        { key: 'district', label: this.translate.instant('DEALER_ASSIGNMENT.DISTRICT') },
        { key: 'block', label: this.translate.instant('DEALER_ASSIGNMENT.BLOCK') },
        { key: 'pinCode', label: this.translate.instant('DEALER_ASSIGNMENT.PIN_CODE') },
        { key: 'assignedUsersCount', label: this.translate.instant('DEALER_ASSIGNMENT.ASSIGNED_USER_COUNT') || 'Assigned Count' },
        { key: 'assignedUserNames', label: this.translate.instant('DEALER_ASSIGNMENT.ASSIGNED_USER_NAMES') || 'Assigned Names' },
        { key: 'status', label: this.translate.instant('DEALER_ASSIGNMENT.STATUS'), type: 'status' },
        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'master_detail_actions' }
      ];
    });
  }

  ngOnInit(): void {
    // trigger immediately if needed
    this.headers = [
      { key: 'dealerUsername', label: this.translate.instant('DEALER_ASSIGNMENT.DEALER_NAME') },
      { key: 'state', label: this.translate.instant('DEALER_ASSIGNMENT.STATE') },
      { key: 'district', label: this.translate.instant('DEALER_ASSIGNMENT.DISTRICT') },
      { key: 'block', label: this.translate.instant('DEALER_ASSIGNMENT.BLOCK') },
      { key: 'pinCode', label: this.translate.instant('DEALER_ASSIGNMENT.PIN_CODE') },
      { key: 'assignedUsersCount', label: this.translate.instant('DEALER_ASSIGNMENT.ASSIGNED_USER_COUNT') || 'Assigned Count' },
      { key: 'assignedUserNames', label: this.translate.instant('DEALER_ASSIGNMENT.ASSIGNED_USER_NAMES') || 'Assigned Names' },
      { key: 'status', label: this.translate.instant('DEALER_ASSIGNMENT.STATUS'), type: 'status' },
      { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'master_detail_actions' }
    ];

    this.loadAssignments();
    this.loadDropdownData();
  }

  formatName(name: string): string {
    if (!name) return '-';
    name = String(name);
    return name.split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(' ');
  }

  toggleFilter(): void {
    this.isFilterOpen = !this.isFilterOpen;
  }

  loadDropdownData(): void {
    this.api.getAllDealers({ page: 0, size: 1000 }).subscribe(res => {
      if (res.success) {
        this.dealers = res.data.content;
        this.dealerOptions = this.dealers.map(d => ({ id: d.id, name: `${d.username} (${d.email})` }));
        this.dealerSearchOptions = [
          { id: '', name: 'All Dealers' },
          ...this.dealers.map(d => ({ id: d.username, name: `${d.username} (${d.email})` }))
        ];
        const uniqueAreas = Array.from(new Set(this.dealers.map(d => d.area).filter((a: any) => a && String(a).trim())));
        this.areaSearchOptions = [
          { id: '', name: 'All Areas' },
          ...uniqueAreas.map((a: any) => ({ id: a, name: a }))
        ];
      }
    });
    this.api.getAllDealerProducts({ page: 0, size: 1000 }).subscribe(res => {
      if (res.success) {
        this.products = res.data.content;
        this.productOptions = this.products.map(p => ({ id: p.id, name: `${p.name} (${p.category})` }));
        this.productSearchOptions = [
          { id: '', name: 'All Products' },
          ...this.products.map(p => ({ id: p.name, name: `${p.name} (${p.category})` }))
        ];
      }
    });
    this.api.getStates().subscribe(res => {
      if (res.success) this.states = res.data.map((s: string) => ({ id: s, name: s }));
    });
  }

  onStateChange(): void {
    this.assignmentForm.district = '';
    this.assignmentForm.block = '';
    this.assignmentForm.pinCode = '';
    this.assignmentForm.assignedUserIds = [];
    this.districts = [];
    this.blocks = [];
    this.pinCodes = [];
    this.availableUsers = [];
    if (this.assignmentForm.state) {
      this.api.getDistricts(this.assignmentForm.state).subscribe(res => {
        if (res.success) this.districts = res.data.map((d: string) => ({ id: d, name: d }));
      });
    }
  }

  onDistrictChange(): void {
    this.assignmentForm.block = '';
    this.assignmentForm.pinCode = '';
    this.assignmentForm.assignedUserIds = [];
    this.blocks = [];
    this.pinCodes = [];
    this.availableUsers = [];
    if (this.assignmentForm.district) {
      this.api.getBlocks(this.assignmentForm.district).subscribe(res => {
        if (res.success) this.blocks = res.data.map((b: string) => ({ id: b, name: b }));
      });
    }
  }

  onBlockChange(): void {
    this.assignmentForm.pinCode = '';
    this.assignmentForm.assignedUserIds = [];
    this.pinCodes = [];
    this.availableUsers = [];
    if (this.assignmentForm.block) {
      this.api.getPinCodes(this.assignmentForm.block).subscribe(res => {
        if (res.success) this.pinCodes = res.data.map((p: string) => ({ id: p, name: p }));
      });
    }
  }

  onPinCodeChange(): void {
    this.assignmentForm.assignedUserIds = [];
    this.availableUsers = [];
    this.availableUserOptions = [];
    if (this.assignmentForm.pinCode) {
      this.api.getUsersByPinCode(this.assignmentForm.pinCode).subscribe(res => {
        if (res.success) {
          this.availableUsers = res.data;
          this.availableUserOptions = this.availableUsers.map(u => ({
            id: u.id,
            name: `${u.fullName} (${u.phoneNumber})`
          }));
        }
      });
    }
  }

  loadAssignments(): void {
    this.isLoading = true;
    
    // Pass robust search parameters to backend
    // Note: Backend needs to process these fields dynamically for search to work perfectly across all fields.
    const params = {
      searchTerm: this.filters.searchTerm || null,
      dealerName: this.filters.dealerName || null,
      area: this.filters.area || null,
      productName: this.filters.productName || null,
      status: this.filters.status || null,
      page: this.page,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.api.getAllDealerAssignments(params).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.assignments = res.data.content.map((a: any) => {
            const count = a.assignedUsers ? a.assignedUsers.length : 0;
            const names = a.assignedUsers ? a.assignedUsers.map((u: any) => this.formatName(u.fullName || u.username)).join(', ') : '-';
            return {
              ...a,
              status: a.active ? 'ACTIVE' : 'INACTIVE',
              dealerUsername: this.formatName(a.dealerUsername),
              assignedUsersCount: count,
              assignedUserNames: names
            };
          });
          this.totalItems = res.data.totalElements;
        }
      },
      error: () => {
        this.isLoading = false;
        this.toast.showError('Failed to load assignments');
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadAssignments();
  }

  resetFilters(): void {
    this.filters = { searchTerm: '', dealerName: '', area: '', productName: '', status: '' };
    this.page = 0;
    this.loadAssignments();
  }

  onPageChange(pageIndex: number): void {
    this.page = pageIndex;
    this.loadAssignments();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.page = 0;
    this.loadAssignments();
  }

  openAddModal(): void {
    this.editingAssignment = null;
    this.selectedDealer = null;
    this.selectedDealerMetrics = null;
    this.assignmentForm = { 
      dealerId: null, 
      productIds: [], 
      targetQuantity: 0,
      state: '',
      district: '',
      block: '',
      pinCode: '',
      assignedUserIds: []
    };
    this.isModalOpen = true;
  }

  onView(assignment: any): void {
    this.openEditModal(assignment);
  }

  openEditModal(assignment: any): void {
    this.editingAssignment = assignment;
    this.assignmentForm = { 
      dealerId: assignment.dealerId, 
      productIds: [assignment.productId], 
      targetQuantity: assignment.targetQuantity,
      state: assignment.state || '',
      district: assignment.district || '',
      block: assignment.block || '',
      pinCode: assignment.pinCode || '',
      assignedUserIds: assignment.assignedUsers ? assignment.assignedUsers.map((u: any) => u.id) : []
    };
    this.onDealerSelect();
    
    // Load dropdowns based on existing values
    if (this.assignmentForm.state) {
      this.api.getDistricts(this.assignmentForm.state).subscribe(res => { if (res.success) this.districts = res.data.map((d: string) => ({ id: d, name: d })); });
    }
    if (this.assignmentForm.district) {
      this.api.getBlocks(this.assignmentForm.district).subscribe(res => { if (res.success) this.blocks = res.data.map((b: string) => ({ id: b, name: b })); });
    }
    if (this.assignmentForm.block) {
      this.api.getPinCodes(this.assignmentForm.block).subscribe(res => { if (res.success) this.pinCodes = res.data.map((p: string) => ({ id: p, name: p })); });
    }
    if (this.assignmentForm.pinCode) {
      this.api.getUsersByPinCode(this.assignmentForm.pinCode).subscribe(res => { 
        if (res.success) {
          this.availableUsers = res.data;
          this.availableUserOptions = this.availableUsers.map(u => ({
            id: u.id,
            name: `${u.fullName} (${u.phoneNumber})`
          }));
        }
      });
    }

    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
  }
  

  onDealerSelect(): void {
    if (this.assignmentForm.dealerId) {
      this.selectedDealer = this.dealers.find(d => d.id === this.assignmentForm.dealerId);
      this.selectedDealerMetrics = null;
      if (this.selectedDealer) {
        this.isLoadingMetrics = true;
        this.api.getAdminDealerDashboardMetrics(this.selectedDealer.id).subscribe({
          next: (res) => {
            this.isLoadingMetrics = false;
            if (res.success) {
              this.selectedDealerMetrics = res.data;
            }
          },
          error: () => {
            this.isLoadingMetrics = false;
          }
        });
      }
    } else {
      this.selectedDealer = null;
      this.selectedDealerMetrics = null;
    }
  }

  exportToExcel(): void {
    this.isExporting = true;
    
    // Pass robust search parameters to backend export API
    const params = {
      searchTerm: this.filters.searchTerm || null,
      dealerName: this.filters.dealerName || null,
      area: this.filters.area || null,
      productName: this.filters.productName || null
    };

    this.api.exportToExcel('dealer-assignments', params).subscribe({
      next: (blob) => {
        this.isExporting = false;
        // Trigger download
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Dealer_Assignments.xlsx';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        this.toast.showSuccess(MessageConstants.SUCCESS.EXCEL_DOWNLOAD_SUCCESS);
      },
      error: () => {
        this.isExporting = false;
        this.toast.showError(MessageConstants.ERROR.EXCEL_DOWNLOAD_FAILED);
      }
    });
  }

  saveAssignment(): void {
    if (!this.assignmentForm.dealerId || this.assignmentForm.productIds.length === 0 || !this.assignmentForm.targetQuantity) {
      this.toast.showError('Dealer, Products, and Target Quantity are required');
      return;
    }

    const apiCall = this.editingAssignment 
      ? this.api.updateDealerAssignment(this.editingAssignment.id, this.assignmentForm)
      : this.api.createDealerAssignment(this.assignmentForm);

    apiCall.subscribe({
      next: (res) => {
        if (res.success) {
          this.toast.showSuccess(this.editingAssignment ? MessageConstants.SUCCESS.UPDATED_SUCCESSFULLY : MessageConstants.SUCCESS.SAVED_SUCCESSFULLY);
          this.closeModal();
          this.loadAssignments();
        } else {
          this.toast.showError(MessageConstants.ERROR.DEALER_ASSIGNMENT_FAILED);
        }
      },
      error: () => this.toast.showError(MessageConstants.ERROR.DEALER_ASSIGNMENT_FAILED)
    });
  }

  toggleUserSelection(userId: number): void {
    const index = this.assignmentForm.assignedUserIds.indexOf(userId);
    if (index > -1) {
      this.assignmentForm.assignedUserIds.splice(index, 1);
    } else {
      this.assignmentForm.assignedUserIds.push(userId);
    }
  }

  onEdit(row: any): void {
    this.openEditModal(row);
  }

  onDelete(assignment: any): void {
    if (confirm('Are you sure you want to delete this assignment?')) {
      this.api.deleteDealerAssignment(assignment.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast.showSuccess('Assignment deleted successfully');
            this.loadAssignments();
          }
        },
        error: () => this.toast.showError('Failed to delete assignment')
      });
    }
  }
}
