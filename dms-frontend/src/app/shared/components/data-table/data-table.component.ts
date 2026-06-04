import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigService } from '../../../core/services/config.service';

@Component({
    selector: 'app-data-table',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './data-table.component.html',
    styleUrls: ['./data-table.component.css']
})
export class DataTableComponent {
    @Input() title = '';
    @Input() headers: { label: string, key: string, type?: string, btnLabel?: string }[] = [];
    @Input() data: any[] = [];
    @Input() totalElements = 0;
    @Input() totalPages = 0;
    @Input() currentPage = 0;
    @Input() pageSize = 5;
    @Input() isLoading = false;
    @Input() isLast = false;
    @Input() pageSizeOptions = [5, 10, 30];

    @Input() selectable = false;
    @Input() isAdmin = false;
    @Input() selectedItems: any[] = [];
    @Output() selectionChange = new EventEmitter<any[]>();

    @Output() pageChange = new EventEmitter<number>();
    @Output() pageSizeChange = new EventEmitter<number>();
    @Output() rowClick = new EventEmitter<any>();
    @Output() delete = new EventEmitter<any>();
    @Output() restore = new EventEmitter<any>();
    @Output() approve = new EventEmitter<any>();
    @Output() reject = new EventEmitter<any>();
    @Output() archive = new EventEmitter<any>();

    readonly text = this.config.text;

    constructor(private readonly config: ConfigService) { }

    isSelected(item: any): boolean {
        return this.selectedItems.some(i => i.id === item.id);
    }

    toggleSelection(item: any, event: Event) {
        event.stopPropagation();
        const index = this.selectedItems.findIndex(i => i.id === item.id);
        if (index > -1) {
            this.selectedItems.splice(index, 1);
        } else {
            this.selectedItems.push(item);
        }
        this.selectionChange.emit([...this.selectedItems]);
    }

    onRowClick(item: any) {
        this.rowClick.emit(item);
    }

    onRowKeyDown(event: KeyboardEvent, item: any) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            this.onRowClick(item);
        }
    }

    getStatusClasses(status: string) {
        if (!status) return 'bg-slate-50 text-slate-700 border-slate-200';
        const s = status.toUpperCase();
        switch (s) {
            case 'PENDING':
            case 'IN_REVIEW':
                return 'bg-amber-50 text-amber-700 border-amber-200 shadow-amber-50/50';
            case 'APPROVED':
            case 'ACTIVE':
                return 'bg-emerald-50 text-emerald-700 border-emerald-200 shadow-emerald-50/50';
            case 'REJECTED':
            case 'INACTIVE':
                return 'bg-rose-50 text-rose-700 border-rose-200 shadow-rose-50/50';
            case 'UPLOADED':
                return 'bg-indigo-50 text-indigo-700 border-indigo-200 shadow-indigo-50/50';
            case 'SOFT_DELETED':
            case 'ARCHIVED':
            case 'DELETED':
                return 'bg-slate-50 text-slate-500 border-slate-200 border-dashed';
            default:
                return 'bg-slate-50 text-slate-700 border-slate-200';
        }
    }

    formatStatus(status: string) {
        if (!status) return '';
        switch (status) {
            case 'IN_REVIEW': return 'Under Review';
            case 'UPLOADED': return 'Pending';
            case 'APPROVED': return 'Approved';
            case 'REJECTED': return 'Rejected';
            default: return status.replaceAll('_', ' ');
        }
    }

    onPrevPage() {
        if (this.currentPage > 0) {
            this.onPageChange(this.currentPage - 1);
        }
    }

    onNextPage() {
        if (!this.isLast) {
            this.onPageChange(this.currentPage + 1);
        }
    }

    onPageChange(page: number) {
        this.pageChange.emit(page);
    }

    onPageSizeChange() {
        this.pageSizeChange.emit(Number(this.pageSize));
    }

    getPages(): number[] {
        const pages = [];
        const maxPagesToShow = 5;
        let startPage = Math.max(0, this.currentPage - 2);
        let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);

        if (endPage - startPage < maxPagesToShow - 1) {
            startPage = Math.max(0, endPage - maxPagesToShow + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }
        return pages;
    }

    getDataValue(item: any, key: string | undefined): any {
        if (!key) return '';
        return item[key];
    }
}
