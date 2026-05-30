import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService } from '../../core/services/document.service';
import { ReviewService } from '../../core/services/review.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfigService } from '../../core/services/config.service';
import { DocumentResponse } from '../../shared/models/document.model';
import { FormsModule } from '@angular/forms';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { DocumentViewerComponent } from '../../shared/components/document-viewer/document-viewer.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
    selector: 'app-review',
    standalone: true,
    imports: [CommonModule, FormsModule, DataTableComponent, DocumentViewerComponent],
    templateUrl: './review.component.html'
})
export class ReviewComponent implements OnInit {
    documents: DocumentResponse[] = [];
    isLoading = false;
    activeDoc: DocumentResponse | null = null;
    comments = '';
    readonly text = this.config.text;
    selectedDocForView: DocumentResponse | null = null;
    showViewer = false;

    // Selection for Multi-select
    selectedDocuments: DocumentResponse[] = [];
    selectedAction: 'APPROVE' | 'REJECT' | null = null;

    // Table Config
    tableHeaders = [
        { label: 'Document', key: 'title' },
        { label: 'Owner', key: 'uploaderUsername', type: 'user' },
        { label: 'Assigned To', key: 'reviewerUsername', type: 'user' },
        { label: 'Status', key: 'status', type: 'status' }
    ];

    // Pagination properties
    currentPage = 0;
    pageSize = 5;
    totalElements = 0;
    totalPages = 0;
    isLast = false;

    constructor(
        private readonly documentService: DocumentService,
        private readonly reviewService: ReviewService,
        private readonly toast: ToastService,
        private readonly config: ConfigService,
        private readonly auth: AuthService
    ) { }

    ngOnInit() {
        this.loadPendingDocuments(0);
    }

    onPageSizeChange(newSize: number) {
        this.pageSize = newSize;
        this.loadPendingDocuments(0);
    }

    onSelectionChange(selected: DocumentResponse[]) {
        this.selectedDocuments = selected;

        // Auto-focus the last selected item if nothing is active
        if (selected.length > 0 && !this.activeDoc) {
            this.activeDoc = selected.at(-1) || null;
        }

        // If activeDoc is deselected, clear it or pick another from selected
        if (this.activeDoc && !selected.some(s => s.id === this.activeDoc?.id)) {
            this.activeDoc = selected.length > 0 ? selected[0] : null;
        }
    }

    loadPendingDocuments(page: number = 0) {
        this.isLoading = true;
        this.currentPage = page;
        this.selectedDocuments = [];
        this.activeDoc = null;

        const profile = this.auth.getUserProfile();
        const role = profile?.roles?.[0]?.authority;
        const reviewerId = profile?.userId || profile?.id;

        // ACCESS LOGIC:
        // Admin: can process ANY document in status IN_REVIEW (assigned to them or anyone else)
        // Reviewer: can only process documents specifically assigned to them.
        const idFilter = role === 'ROLE_ADMIN' ? undefined : reviewerId;

        this.documentService.getPagedDocuments(page, this.pageSize, ['IN_REVIEW'], idFilter).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.documents = res.data.content;
                    this.totalElements = res.data.totalElements;
                    this.totalPages = res.data.totalPages;
                    this.isLast = res.data.last;
                }
                this.isLoading = false;
            },
            error: (err) => {
                this.isLoading = false;
                if (err.status !== 401 && err.status !== 403) {
                    this.toast.showError(this.config.get('messages.load_error'));
                }
            }
        });
    }

    selectDocument(doc: DocumentResponse) {
        const index = this.selectedDocuments.findIndex(s => s.id === doc.id);

        if (index > -1) {
            // Already selected -> Toggle OFF
            this.selectedDocuments.splice(index, 1);
            this.selectedDocuments = [...this.selectedDocuments];

            // If we just deselected the active doc, update the active focus
            if (this.activeDoc?.id === doc.id) {
                this.activeDoc = this.selectedDocuments.length > 0 ? this.selectedDocuments.at(-1) || null : null;
            }
        } else {
            // Not selected -> Toggle ON
            this.selectedDocuments = [...this.selectedDocuments, doc];
            this.activeDoc = doc;
            this.comments = '';
            this.selectedAction = null;
        }
    }

    toggleAction(action: 'APPROVE' | 'REJECT') {
        // Toggle behavior: If clicking same action, deselect. If other, switch to it.
        this.selectedAction = this.selectedAction === action ? null : action;
    }

    openDocument(id: number) {
        const doc = this.documents.find(d => d.id === id);
        if (doc) {
            this.selectedDocForView = doc;
            this.showViewer = true;
        }
    }

    submitReview() {
        if (!this.selectedAction) return;

        // Determine if this is a bulk operation or single operation
        if (this.selectedDocuments.length > 1) {
            this.submitBulkReview(this.selectedAction);
            return;
        }

        // Single execution fallback (or if user only selected 1 via checkbox)
        const docToProcess = this.selectedDocuments.length === 1 ? this.selectedDocuments[0] : this.activeDoc;

        if (!docToProcess) return;

        this.isLoading = true;
        this.reviewService.reviewDocument(docToProcess.id, {
            action: this.selectedAction,
            comments: this.comments
        }).subscribe({
            next: (res) => {
                this.isLoading = false;
                this.toast.showSuccess(this.config.get('messages.review_success'));
                this.activeDoc = null;
                this.selectedAction = null;
                this.comments = '';
                this.loadPendingDocuments();
            },
            error: (err: any) => {
                this.isLoading = false;
                this.toast.showError(err.error?.message || this.config.get('messages.review_error'));
            }
        });
    }

    submitBulkReview(action: 'APPROVE' | 'REJECT') {
        if (this.selectedDocuments.length === 0) return;

        this.isLoading = true;
        const bulkComments = this.comments || 'Bulk processed via administrative console.';
        const requests = this.selectedDocuments.map(doc =>
            this.reviewService.reviewDocument(doc.id, { action, comments: bulkComments })
        );

        let completed = 0;
        let successCount = 0;

        requests.forEach(req => {
            req.subscribe({
                next: () => {
                    successCount++;
                    completed++;
                    if (completed === requests.length) this.onBulkFinished(successCount);
                },
                error: () => {
                    completed++;
                    if (completed === requests.length) this.onBulkFinished(successCount);
                }
            });
        });
    }

    private onBulkFinished(successCount: number) {
        this.isLoading = false;
        this.toast.showSuccess(`Successfully processed ${successCount} documents.`);
        this.selectedDocuments = [];
        this.activeDoc = null;
        this.selectedAction = null;
        this.comments = '';
        this.loadPendingDocuments();
    }

    formatStatus(status: string): string {
        switch (status) {
            case 'IN_REVIEW': return 'In Review Process';
            case 'UPLOADED': return 'Waiting for Review';
            default: return status;
        }
    }
}
