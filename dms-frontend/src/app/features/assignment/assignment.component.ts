import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../core/services/document.service';
import { UserService } from '../../core/services/user.service';
import { ReviewService } from '../../core/services/review.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfigService } from '../../core/services/config.service';
import { UserResponse } from '../../shared/models/user.model';
import { DocumentResponse } from '../../shared/models/document.model';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';

@Component({
    selector: 'app-assignment',
    standalone: true,
    imports: [CommonModule, FormsModule, DataTableComponent],
    templateUrl: './assignment.component.html'
})
export class AssignmentComponent implements OnInit {
    documents: DocumentResponse[] = [];
    reviewers: UserResponse[] = [];
    isLoading = false;
    selectedDocs: DocumentResponse[] = [];
    selectedDoc: DocumentResponse | null = null;
    selectedReviewerId: number | null = null;
    readonly text = this.config.text;

    // Table Config
    tableHeaders = [
        { label: 'Document', key: 'title' },
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
        private readonly userService: UserService,
        private readonly reviewService: ReviewService,
        private readonly toast: ToastService,
        private readonly config: ConfigService
    ) { }

    ngOnInit() {
        this.loadData(0);
        this.loadReviewers();
    }

    onPageSizeChange(newSize: number) {
        this.pageSize = newSize;
        this.loadData(0);
    }

    loadData(page: number = 0) {
        this.isLoading = true;
        this.currentPage = page;
        this.documentService.getPagedDocuments(page, this.pageSize, ['UPLOADED']).subscribe({
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

    loadReviewers() {
        this.userService.getReviewers().subscribe({
            next: (res) => {
                if (res.success) {
                    this.reviewers = res.data;
                }
            }
        });
    }

    onSelectionChange(selected: DocumentResponse[]) {
        this.selectedDocs = selected;
        if (selected.length > 0) {
            this.selectedDoc = selected.at(-1) || null;
        } else {
            this.selectedDoc = null;
        }
    }

    selectDocument(doc: DocumentResponse) {
        const index = this.selectedDocs.findIndex(s => s.id === doc.id);
        if (index > -1) {
            // Toggle off
            this.selectedDocs.splice(index, 1);
        } else {
            // Toggle on
            this.selectedDocs.push(doc);
        }
        this.selectedDocs = [...this.selectedDocs];
        this.onSelectionChange(this.selectedDocs);
    }

    assignDocument() {
        if (this.selectedDocs.length === 0 || !this.selectedReviewerId) return;

        this.isLoading = true;
        const requests = this.selectedDocs.map(doc =>
            this.reviewService.assignReviewer(doc.id, this.selectedReviewerId!)
        );

        let completed = 0;
        let successCount = 0;

        requests.forEach(req => {
            req.subscribe({
                next: () => {
                    successCount++;
                    completed++;
                    if (completed === requests.length) this.onAssignmentFinished(successCount);
                },
                error: () => {
                    completed++;
                    if (completed === requests.length) this.onAssignmentFinished(successCount);
                }
            });
        });
    }

    private onAssignmentFinished(successCount: number) {
        this.isLoading = false;
        this.toast.showSuccess(`Successfully assigned ${successCount} documents.`);
        this.selectedDocs = [];
        this.selectedDoc = null;
        this.selectedReviewerId = null;
        this.loadData();
    }

    formatStatus(status: string): string {
        return status === 'UPLOADED' ? 'Waiting for Assignment' : status;
    }

    formatSize(bytes?: number): string {
        if (!bytes) return '0 KB';
        const kb = bytes / 1024;
        return kb > 1024 ? (kb / 1024).toFixed(2) + ' MB' : kb.toFixed(2) + ' KB';
    }
}
