import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { TranslateModule } from '@ngx-translate/core';
import { AppSelectComponent } from '../../../../shared/components/app-select/app-select.component';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-dealer-verification',
  standalone: true,
  imports: [CommonModule, FormsModule, DataTableComponent, TranslateModule, AppSelectComponent],
  templateUrl: './dealer-verification.component.html',
  styleUrls: ['./dealer-verification.component.css']
})
export class DealerVerificationComponent implements OnInit {
  
  dealers: any[] = [];
  totalItems = 0;
  isLoading = false;

  // Filter state
  isFilterOpen = false;
  filters = {
    searchTerm: '',
    status: 'ALL'
  };

  statusOptions = [
    { id: 'ALL', name: 'All Statuses' },
    { id: 'PENDING', name: 'Pending' },
    { id: 'VERIFIED', name: 'Verified' },
    { id: 'REJECTED', name: 'Rejected' }
  ];

  // Pagination state
  page = 0;
  pageSize = 10;
  sortBy = 'createdAt';
  sortDir = 'desc';

  // Table Configuration
  headers: any[] = [
    { key: 'username', label: 'Dealer' },
    { key: 'email', label: 'Email' },
    { key: 'state', label: 'State' },
    { key: 'district', label: 'District' },
    { key: 'verificationStatus', label: 'Status', type: 'status' },
    { key: 'actions', label: 'Actions', type: 'actions' }
  ];

  constructor(
    private api: ApiService, 
    private toast: ToastService,
    private auth: AuthService,
    private translate: TranslateService
  ) {
    this.initHeaders();
  }

  initHeaders() {
    this.translate.onLangChange.subscribe(() => {
      this.headers = [
        { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },
        { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },
        { key: 'state', label: this.translate.instant('DEALER_VERIFICATION.STATE') },
        { key: 'district', label: this.translate.instant('DEALER_VERIFICATION.DISTRICT') },
        { key: 'verificationStatus', label: this.translate.instant('DEALER_VERIFICATION.SELECT_STATUS'), type: 'status' },
        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'actions' }
      ];
    });
  }
  isAdmin = false;
  
  // Dealer View State
  currentStep = 1;
  isSubmitting = false;
  myVerificationStatus: string | null = null;
  verificationForm = {
    state: '',
    district: '',
    area: '',
    pinCode: '',
    address: '',
    aadhaarNumber: '',
    panNumber: '',
    photoUrl: '',
    aadhaarUrl: '',
    panUrl: ''
  };

  // Master Data state
  states: any[] = [];
  districts: any[] = [];
  areas: any[] = [];
  pincodes: any[] = [];

  selectedStateId: number | null = null;
  selectedDistrictId: number | null = null;
  selectedAreaId: number | null = null;
  selectedPincodeId: number | null = null;

  // Custom Dropdown States
  isStateDropdownOpen = false;
  isDistrictDropdownOpen = false;
  isAreaDropdownOpen = false;
  isPincodeDropdownOpen = false;

  // Search Terms
  stateSearchTerm = '';
  districtSearchTerm = '';
  areaSearchTerm = '';
  pincodeSearchTerm = '';

  get filteredStates() {
    if (!this.stateSearchTerm) return this.states;
    return this.states.filter(s => s.name.toLowerCase().includes(this.stateSearchTerm.toLowerCase()));
  }

  get filteredDistricts() {
    if (!this.districtSearchTerm) return this.districts;
    return this.districts.filter(s => s.name.toLowerCase().includes(this.districtSearchTerm.toLowerCase()));
  }

  get filteredAreas() {
    if (!this.areaSearchTerm) return this.areas;
    return this.areas.filter(s => s.name.toLowerCase().includes(this.areaSearchTerm.toLowerCase()));
  }

  get filteredPincodes() {
    if (!this.pincodeSearchTerm) return this.pincodes;
    return this.pincodes.filter(s => s.name.toLowerCase().includes(this.pincodeSearchTerm.toLowerCase()));
  }

  hasExactMatch(list: any[], term: string): boolean {
    if (!term || !list) return false;
    return list.some(item => item.name.toLowerCase() === term.trim().toLowerCase());
  }

  // Admin View State
  viewingDealer: any = null;



  ngOnInit(): void {
    this.headers = [
      { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },
      { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },
      { key: 'state', label: this.translate.instant('DEALER_VERIFICATION.STATE') },
      { key: 'district', label: this.translate.instant('DEALER_VERIFICATION.DISTRICT') },
      { key: 'verificationStatus', label: this.translate.instant('DEALER_VERIFICATION.SELECT_STATUS'), type: 'status' },
      { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'actions' }
    ];

    const userProfile = this.auth.getUserProfile();
    this.isAdmin = userProfile?.roles?.includes('ROLE_ADMIN');
    
    if (this.isAdmin) {
      this.loadDealers();
    } else {
      this.loadMyDealerProfile();
      this.loadStates();
    }
  }

  loadStates(): void {
    this.api.getDropdownOptions('STATE').subscribe(res => {
      if (res.success) {
        this.states = res.data;
      }
    });
  }



  onStateChange(stateId: any): void {
    this.isStateDropdownOpen = false;
    this.selectedStateId = stateId || null;
    this.selectedDistrictId = null;
    this.selectedAreaId = null;
    this.selectedPincodeId = null;
    this.districts = [];
    this.areas = [];
    this.pincodes = [];
    
    if (stateId) {
      const stateObj = this.states.find(s => s.id == stateId);
      this.verificationForm.state = stateObj ? stateObj.name : '';
      
      this.api.getDropdownOptions('DISTRICT', stateId).subscribe(res => {
        if (res.success) {
          this.districts = res.data;
        }
      });
    } else {
      this.verificationForm.state = '';
    }
    this.verificationForm.district = '';
    this.verificationForm.area = '';
    this.verificationForm.pinCode = '';
  }

  onDistrictChange(districtId: any): void {
    this.isDistrictDropdownOpen = false;
    this.selectedDistrictId = districtId || null;
    this.selectedAreaId = null;
    this.selectedPincodeId = null;
    this.areas = [];
    this.pincodes = [];
    
    if (districtId) {
      const distObj = this.districts.find(d => d.id == districtId);
      this.verificationForm.district = distObj ? distObj.name : '';

      this.api.getDropdownOptions('AREA', districtId).subscribe(res => {
        if (res.success) {
          this.areas = res.data;
        }
      });
    } else {
      this.verificationForm.district = '';
    }
    this.verificationForm.area = '';
    this.verificationForm.pinCode = '';
  }

  onAreaChange(areaId: any): void {
    this.isAreaDropdownOpen = false;
    this.selectedAreaId = areaId || null;
    this.selectedPincodeId = null;
    this.pincodes = [];

    if (areaId) {
      const areaObj = this.areas.find(a => a.id == areaId);
      this.verificationForm.area = areaObj ? areaObj.name : '';

      this.api.getDropdownOptions('PINCODE', areaId).subscribe(res => {
        if (res.success) {
          this.pincodes = res.data;
        }
      });
    } else {
      this.verificationForm.area = '';
    }
    this.verificationForm.pinCode = '';
  }

  onPincodeChange(pincodeId: any): void {
    this.isPincodeDropdownOpen = false;
    this.selectedPincodeId = pincodeId || null;
    if (pincodeId) {
      const pinObj = this.pincodes.find(p => p.id == pincodeId);
      this.verificationForm.pinCode = pinObj ? pinObj.name : '';
    } else {
      this.verificationForm.pinCode = '';
    }
  }

  loadMyDealerProfile(): void {
    // Just fetch my dealer profile and check the verificationStatus
    this.api.getMyDealerProfile().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const profile = res.data;
          this.myVerificationStatus = profile.verificationStatus || null;
          
          if (profile) {
            this.verificationForm.state = profile.state || '';
            this.verificationForm.district = profile.district || '';
            this.verificationForm.area = profile.area || '';
            this.verificationForm.pinCode = profile.pinCode || '';
            this.verificationForm.address = profile.address || '';
            this.verificationForm.aadhaarNumber = profile.aadhaarNumber || '';
            this.verificationForm.panNumber = profile.panNumber || '';
            this.verificationForm.photoUrl = profile.photoUrl || '';
            this.verificationForm.aadhaarUrl = profile.aadhaarUrl || '';
            this.verificationForm.panUrl = profile.panUrl || '';
          }
        }
      },
      error: () => {
        // If 404, it means no dealer profile exists yet, which is fine.
        this.myVerificationStatus = null;
      }
    });
  }

  onFileSelected(event: any, fieldName: 'photoUrl' | 'aadhaarUrl' | 'panUrl'): void {
    const file = event.target.files[0];
    if (file) {
      this.api.uploadFile(file).subscribe({
        next: (res) => {
          if (res.success) {
            this.verificationForm[fieldName] = res.data.fileUrl || res.data.url || res.data;
            this.toast.showSuccess('File uploaded successfully');
          } else {
            this.toast.showError('File upload failed');
          }
        },
        error: () => this.toast.showError('Failed to upload file')
      });
    }
  }

  submitVerification(): void {
    if (!this.verificationForm.photoUrl || !this.verificationForm.aadhaarUrl || !this.verificationForm.panUrl) {
      this.toast.showError('Please upload all required documents');
      return;
    }

    if (this.verificationForm.aadhaarNumber) {
      this.verificationForm.aadhaarNumber = this.verificationForm.aadhaarNumber.toString().trim();
    }
    const aadhaarRegex = /^[2-9]{1}[0-9]{11}$/;
    if (!aadhaarRegex.test(this.verificationForm.aadhaarNumber)) {
      this.toast.showError('Invalid Aadhaar number format. It must be exactly 12 digits.');
      return;
    }

    if (this.verificationForm.pinCode) {
      this.verificationForm.pinCode = this.verificationForm.pinCode.toString().trim();
    }
    const pinRegex = /^[0-9]{6}$/;
    if (!pinRegex.test(this.verificationForm.pinCode)) {
      this.toast.showError('Invalid PIN code format. It must be exactly 6 digits.');
      return;
    }

    if (this.verificationForm.panNumber) {
      this.verificationForm.panNumber = this.verificationForm.panNumber.trim().toUpperCase();
    }
    const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/;
    if (!panRegex.test(this.verificationForm.panNumber)) {
      this.toast.showError('Invalid PAN number format.');
      return;
    }
    
    this.isSubmitting = true;
    
    const payload = {
      state: this.verificationForm.state,
      district: this.verificationForm.district,
      area: this.verificationForm.area,
      pinCode: this.verificationForm.pinCode,
      address: this.verificationForm.address,
      aadhaarNumber: this.verificationForm.aadhaarNumber,
      panNumber: this.verificationForm.panNumber,
      photoUrl: this.verificationForm.photoUrl,
      aadhaarUrl: this.verificationForm.aadhaarUrl,
      panUrl: this.verificationForm.panUrl
    };

    this.api.saveDealerProfile(payload).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        if (res.success) {
          this.toast.showSuccess('Verification details submitted successfully');
          this.myVerificationStatus = 'PENDING';
        } else {
          this.toast.showError('Failed to submit details');
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.toast.showError('Error submitting details');
      }
    });
  }

  toggleFilter(): void {
    this.isFilterOpen = !this.isFilterOpen;
  }

  loadDealers(): void {
    this.isLoading = true;
    
    const params = {
      searchTerm: this.filters.searchTerm || null,
      status: this.filters.status,
      page: this.page,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.api.getAllDealers(params).subscribe({
      next: (res) => {
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
    this.filters = { searchTerm: '', status: 'ALL' };
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

  onViewDetails(row: any): void {
    this.viewingDealer = row;
  }

  closeViewModal(): void {
    this.viewingDealer = null;
  }

  onApprove(row: any): void {
    this.verifyDealer(row.id, true);
  }

  onReject(row: any): void {
    this.verifyDealer(row.id, false);
  }

  verifyDealer(id: number, approve: boolean): void {
    if (confirm(`Are you sure you want to ${approve ? 'Approve' : 'Reject'} this dealer?`)) {
      this.api.verifyDealer(id, approve).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast.showSuccess(`Dealer ${approve ? 'approved' : 'rejected'} successfully`);
            this.loadDealers();
          } else {
            this.toast.showError('Verification failed');
          }
        },
        error: () => this.toast.showError('Verification error')
      });
    }
  }

  getFileUrl(url: string): string {
    if (!url) return '';
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    const baseUrl = environment.apiUrl.replace(/\/api$/, '');
    return `${baseUrl}${url.startsWith('/') ? '' : '/'}${url}`;
  }
}
