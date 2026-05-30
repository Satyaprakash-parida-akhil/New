import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService } from '../../core/services/document.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfigService } from '../../core/services/config.service';
import { DocumentResponse } from '../../shared/models/document.model';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { DocumentViewerComponent } from '../../shared/components/document-viewer/document-viewer.component';
import { ApiResponse } from '../../shared/models/api-response.model';
import { PagedResponse } from '../../shared/models/paged-response.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DataTableComponent, DocumentViewerComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  documents: DocumentResponse[] = [];
  isLoading = false;
  userProfile = { username: '', role: '' };
  readonly text = this.config.text;
  selectedDocForView: DocumentResponse | null = null;
  showViewer = false;
  activeTab: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
  isAdmin = false;

  // Table Config
  tableHeaders: any[] = [];

  readonly activeHeaders = [
    { label: 'ID', key: 'id', type: 'id' },
    { label: 'Document Title', key: 'title' },
    { label: 'Status', key: 'status', type: 'status' },
    { label: 'Owner', key: 'uploaderUsername', type: 'user' },
    { label: 'Action', key: 'actions', type: 'action' }
  ];

  readonly inactiveHeaders = [
    { label: 'ID', key: 'id', type: 'id' },
    { label: 'Document Title', key: 'title' },
    { label: 'Status', key: 'status', type: 'status' },
    { label: 'Owner', key: 'uploaderUsername', type: 'user' },
    { label: 'Action', key: 'actions', type: 'inactive_action' }
  ];

  // Pagination properties
  currentPage = 0;
  pageSize = 5;
  totalElements = 0;
  totalPages = 0;
  isLast = false;

  constructor(
    private readonly documentService: DocumentService,
    private readonly toast: ToastService,
    private readonly config: ConfigService
  ) { }

  ngOnInit() {
    this.extractUserProfile();
    this.updateHeaders();
    this.loadDocuments(0);
  }

  updateHeaders() {
    this.tableHeaders = this.activeTab === 'ACTIVE' ? this.activeHeaders : this.inactiveHeaders;
  }

  setTab(tab: 'ACTIVE' | 'INACTIVE') {
    this.activeTab = tab;
    this.updateHeaders();
    this.loadDocuments(0);
  }

  extractUserProfile() {
    const token = localStorage.getItem('access_token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.userProfile.username = payload.sub || 'Unknown User';
        if (payload.roles && payload.roles.length > 0) {
          const role = payload.roles[0].authority || payload.roles[0];
          this.isAdmin = role === 'ROLE_ADMIN';
          if (role === 'ROLE_ADMIN') this.userProfile.role = 'Administrator';
          else if (role === 'ROLE_REVIEWER') this.userProfile.role = 'Reviewer';
          else if (role === 'ROLE_UPLOADER') this.userProfile.role = 'Uploader';
        }
      } catch (e) {
        console.warn('Failed to parse user profile from token', e);
      }
    }
  }

  openDocument(id: number) {
    const doc = this.documents.find(d => d.id === id);
    if (doc) {
      this.selectedDocForView = doc;
      this.showViewer = true;
    }
  }

  onPageSizeChange(newSize: number) {
    this.pageSize = newSize;
    this.loadDocuments(0);
  }

  loadDocuments(page: number = 0) {
    this.isLoading = true;
    this.currentPage = page;
    const statuses = this.activeTab === 'INACTIVE' ? ['SOFT_DELETED'] : undefined;
    this.documentService.getPagedDocuments(page, this.pageSize, statuses).subscribe({
      next: (res: ApiResponse<PagedResponse<DocumentResponse>>) => {
        if (res.success && res.data) {
          this.documents = res.data.content;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
          this.isLast = res.data.last;
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        this.isLoading = false;
        if (err.status !== 401 && err.status !== 403) {
          this.toast.showError(this.config.get('messages.load_error'));
        }
      }
    });
  }

  formatStatus(status: string): string {
    switch (status) {
      case 'IN_REVIEW': return 'In Review Process';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      case 'UPLOADED': return 'Recently Uploaded';
      case 'SOFT_DELETED': return 'Inactive/Deleted';
      default: return status;
    }
  }

  deleteDocument(id: number) {
    if (confirm('Are you sure you want to move this document to inactive?')) {
      this.documentService.deleteDocument(id).subscribe({
        next: (res) => {
          this.toast.showSuccess(res.message);
          this.loadDocuments(this.currentPage);
        },
        error: (err) => this.toast.showError('Failed to delete document')
      });
    }
  }

  restoreDocument(id: number) {
    this.documentService.restoreDocument(id).subscribe({
      next: (res) => {
        this.toast.showSuccess(res.message);
        this.loadDocuments(this.currentPage);
      },
      error: (err) => this.toast.showError('Failed to restore document')
    });
  }
}
