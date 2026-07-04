import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigService } from '../../../core/services/config.service';
import { ApiService } from '../../../core/services/api.service';
import { LocalizePipe } from '../../pipes/localize.pipe';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

import { AppSelectComponent } from '../app-select/app-select.component';

@Component({
    selector: 'app-data-table',
    standalone: true,
    imports: [CommonModule, FormsModule, LocalizePipe, AppSelectComponent],
    templateUrl: './data-table.component.html',
    styleUrls: ['./data-table.component.css']
})
export class DataTableComponent implements OnChanges, OnDestroy {
    @Input() title = '';
    @Input() tableConfig: any = null;
    @Input() headers: { label: string, key: string, type?: string, btnLabel?: string }[] = [];
    @Input() data: any[] = [];
    @Input() totalElements = 0;
    @Input() totalPages = 0;
    @Input() currentPage = 0;
    @Input() pageSize = 5;
    @Input() isLoading = false;
    @Input() isLast = false;
    @Input() pageSizeOptions = [5, 10, 30];

    get pageSizeSelectOptions() {
        return this.pageSizeOptions.map(opt => ({ id: opt, name: String(opt) }));
    }

    @Input() selectable = false;
    @Input() isAdmin = false;
    @Input() selectedItems: any[] = [];
    @Output() selectionChange = new EventEmitter<any[]>();

    // ── Search & Filter ───────────────────────────────────────────────────────
    @Input() searchable = false;
    @Input() searchPlaceholder = 'Search...';
    /** When true, filtering is handled by the backend only. No local applyFilter() runs. */
    @Input() backendFiltering = false;
    @Output() searchChange = new EventEmitter<string>();
    @Output() filterChange = new EventEmitter<any>();

    // ── Active / Inactive Filter Tab ──────────────────────────────────────────
    @Input() showActiveInactiveFilter = false;
    @Input() activeTab: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
    @Output() activeTabChange = new EventEmitter<'ACTIVE' | 'INACTIVE'>();
    @Input() activeTabLabel = 'Active';
    @Input() inactiveTabLabel = 'Inactive';

    isSearchExpanded = false;
    filters: { [key: string]: string } = {};
    /** Tracks selected IDs for app-select value bindings (separate from filters which store names) */
    filterIds: { [key: string]: any } = {};
    filteredData: any[] = [];
    unfilteredOptions: { [colKey: string]: { id: any, name: string, parentId?: any }[] } = {};
    
    // ── Sorting ───────────────────────────────────────────────────────────────
    sortColumn: string = '';
    sortDirection: 'asc' | 'desc' = 'asc';

    // ── Output Events ─────────────────────────────────────────────────────────
    @Output() pageChange = new EventEmitter<number>();
    @Output() pageSizeChange = new EventEmitter<number>();
    @Output() rowClick = new EventEmitter<any>();
    @Output() delete = new EventEmitter<any>();
    @Output() restore = new EventEmitter<any>();
    @Output() approve = new EventEmitter<any>();
    @Output() reject = new EventEmitter<any>();
    @Output() archive = new EventEmitter<any>();

    // ── User Management Action Events ─────────────────────────────────────────
    @Output() view = new EventEmitter<any>();
    @Output() edit = new EventEmitter<any>();
    @Output() block = new EventEmitter<any>();
    @Output() recover = new EventEmitter<any>();
    @Output() permanentDelete = new EventEmitter<any>();

    readonly text = this.config.text;
    
    // ── Dynamic Dropdowns ─────────────────────────────────────────────────────
    dynamicOptions: { [colKey: string]: { id: any, name: string }[] } = {};
    selectOptionsMap: { [colKey: string]: { id: any, name: string }[] } = {};
    apiFilterOptions: { [colKey: string]: { id: any, name: string }[] } = {};
    dropdownSearch: { [colKey: string]: string } = {};
    private destroy$ = new Subject<void>();

    constructor(
        private readonly config: ConfigService, 
        private readonly apiService: ApiService,
        private readonly auth: AuthService
    ) { }

    ngOnInit(): void {
        const userProfile = this.auth.getUserProfile();
        // Auto-detect isAdmin if not explicitly provided
        if (!this.isAdmin) {
            this.isAdmin = userProfile?.roles?.includes('ROLE_ADMIN') || false;
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadUserFilterOptions(): void {
        if (Object.keys(this.apiFilterOptions).length > 0) return;
        this.apiService.getUserFilterOptions()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (res) => {
                    if (res?.success && res.data) {
                        Object.keys(res.data).forEach(key => {
                            const list = res.data[key] || [];
                            this.apiFilterOptions[key] = list.map((val: string) => ({
                                id: val,
                                name: this.formatPrettyName(val)
                            }));
                        });
                        this.updateSelectOptionsMap();
                    }
                },
                error: (err) => {
                    console.error('Failed to load user filter options', err);
                }
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['tableConfig'] && this.tableConfig) {
            this.headers = Object.keys(this.tableConfig.desktopColumns).map(key => ({
                key,
                label: this.tableConfig.desktopColumns[key],
                type: this.tableConfig.columnTypes?.[key] || (key === 'actions' ? 'action' : undefined)
            }));
            
            // Initialize dynamic options
            if (this.tableConfig.dynamicDropdownKeys) {
                Object.keys(this.tableConfig.dynamicDropdownKeys).forEach(colKey => {
                    this.dynamicOptions[colKey] = [];
                    const headerName = this.tableConfig.dynamicDropdownKeys[colKey];
                    if (headerName) {
                        this.loadDependentOptions(colKey, headerName);
                    }
                });
            }
        }
        if (changes['headers'] || changes['data'] || changes['tableConfig']) {
            if (this.headers && this.headers.some(h => h.key === 'username' || h.key === 'fullName')) {
                this.loadUserFilterOptions();
            }
            this.initFilters();
            this.applyFilter();
            this.updateSelectOptionsMap();
        }
    }

    initFilters(): void {
        this.headers.forEach(h => {
            if (this.isFilterable(h) && !(h.key in this.filters)) {
                this.filters[h.key] = '';
            }
        });
    }

    isFilterable(h: any): boolean {
        return h.key &&
               h.type !== 'action' &&
               h.type !== 'actions' &&
               h.type !== 'registration_actions' &&
               h.type !== 'archived_actions' &&
               h.type !== 'inactive_action' &&
               h.type !== 'user_active_actions' &&
               h.type !== 'user_inactive_actions' &&
               h.type !== 'master_detail_actions' &&
               h.type !== 'payment_actions' &&
               h.type !== 'payment_user';
    }

    isSelectField(h: any): boolean {
        if (!h || !h.key) return false;
        // If it's dynamically configured, it's a select field
        if (this.tableConfig?.dynamicDropdownKeys?.[h.key]) {
            return true;
        }
        if (this.apiFilterOptions[h.key] && this.apiFilterOptions[h.key].length > 0) {
            return true;
        }
        const k = h.key?.toLowerCase();
        const t = h.type?.toLowerCase();
        return k === 'status' || t === 'status' || k === 'registrationstatus' || k === 'requestedrole'
            || k === 'ispaid' || t === 'payment_status';
    }

    getFieldOptions(key: string): string[] {
        const k = key.toLowerCase();
        let defaults: string[] = [];
        if (k === 'status' || k === 'registrationstatus') {
            defaults = ['PENDING', 'APPROVED', 'REJECTED', 'ACTIVE', 'INACTIVE', 'IN_REVIEW', 'UPLOADED'];
        } else if (k === 'requestedrole') {
            defaults = ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_SECURITY_OFFICER'];
        } else if (k === 'ispaid') {
            return ['PAID', 'UNPAID'];
        }
        
        // Also fetch any unique values from the data
        const dataVals = this.data
            .map(item => this.getDataValue(item, key))
            .filter(Boolean)
            .map(v => String(v).toUpperCase().trim());
        
        const combined = new Set([...defaults, ...dataVals]);
        return Array.from(combined).sort();
    }

    formatPrettyName(val: string): string {
        if (!val) return '';
        const v = String(val).trim();
        const upper = v.toUpperCase();
        
        // Role mappings
        if (upper === 'ROLE_ADMIN') return 'Admin';
        if (upper === 'ROLE_SECURITY_OFFICER') return 'Security Officer';
        if (upper === 'ROLE_USER') return 'User';
        if (upper === 'ROLE_UPLOADER') return 'Member';
        if (upper === 'ROLE_REVIEWER') return 'Reviewer';
        
        // If it has underscores, format it (e.g. IN_REVIEW -> In Review)
        if (v.includes('_')) {
            return v.split('_')
                .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
                .join(' ');
        }
        
        // Standard capitalization for simple uppercase words (e.g. PENDING -> Pending, APPROVED -> Approved)
        if (v === upper && /^[A-Z]+$/.test(v)) {
            return v.charAt(0).toUpperCase() + v.slice(1).toLowerCase();
        }
        
        // Otherwise, return original value
        return v;
    }

    updateSelectOptionsMap(): void {
        this.headers.forEach(h => {
            if (this.isSelectField(h)) {
                if (this.tableConfig?.dynamicDropdownKeys?.[h.key]) {
                    const opts = this.dynamicOptions[h.key] || [];
                    const seen = new Set<string>();
                    this.selectOptionsMap[h.key] = opts.filter(opt => {
                        if (!opt || !opt.name) return false;
                        const nameLower = opt.name.trim().toLowerCase();
                        if (seen.has(nameLower)) return false;
                        seen.add(nameLower);
                        return true;
                    }).map(opt => ({
                        id: opt.id,
                        name: this.formatPrettyName(opt.name)
                    }));
                } else if (this.apiFilterOptions[h.key] && this.apiFilterOptions[h.key].length > 0) {
                    this.selectOptionsMap[h.key] = this.apiFilterOptions[h.key];
                } else {
                    this.selectOptionsMap[h.key] = this.getFieldOptions(h.key).map(opt => ({
                        id: opt,
                        name: this.formatPrettyName(opt)
                    }));
                }
            }
        });
    }

    getSelectOptions(h: any): { id: any, name: string }[] {
        if (this.tableConfig?.dynamicDropdownKeys?.[h.key]) {
            const opts = this.dynamicOptions[h.key] || [];
            // Deduplicate options by name to ensure clean UI without duplicates
            const seen = new Set<string>();
            return opts.filter(opt => {
                if (!opt || !opt.name) return false;
                const nameLower = opt.name.trim().toLowerCase();
                if (seen.has(nameLower)) return false;
                seen.add(nameLower);
                return true;
            }).map(opt => ({
                id: opt.id,
                name: this.formatPrettyName(opt.name)
            }));
        }
        // Static dropdown options formatted as {id, name}
        return this.getFieldOptions(h.key).map(opt => ({
            id: opt,
            name: this.formatPrettyName(opt)
        }));
    }

    loadDependentOptions(colKey: string, headerName: string, parentId?: number): void {
        this.apiService.getDropdownOptions(headerName, parentId)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (res) => {
                    if (res?.data) {
                        const mapped = res.data.map((item: any) => ({
                            id: item.id,
                            name: item.name || item.displayName,
                            parentId: item.parentId || null
                        }));
                        this.dynamicOptions[colKey] = mapped;
                        
                        // Seed unfiltered options on startup/initial load (when parentId is null/undefined)
                        if (parentId === undefined) {
                            this.unfilteredOptions[colKey] = mapped;
                        }
                    } else {
                        this.dynamicOptions[colKey] = [];
                        if (parentId === undefined) {
                            this.unfilteredOptions[colKey] = [];
                        }
                    }
                    this.updateSelectOptionsMap();
                },
                error: (err) => {
                    console.error(`Error loading options for ${headerName}`, err);
                    this.dynamicOptions[colKey] = [];
                    if (parentId === undefined) {
                        this.unfilteredOptions[colKey] = [];
                    }
                    this.updateSelectOptionsMap();
                }
            });
    }

    onFilterValChange(colKey: string, val: any): void {
        // LGD hierarchy — must match column keys in table-config.ts exactly.
        // 'town' is the column key for Sub-District (Tehsil) per DASHBOARD_ACTIVE_COLUMNS.
        const hierarchy = ['country', 'state', 'district', 'town', 'block', 'village'];
        const colIdx = hierarchy.indexOf(colKey);

        if (colIdx !== -1) {
            this.filterIds[colKey] = val || null;
            const fullList = this.unfilteredOptions[colKey] || [];
            const selectedOpt = fullList.find(o => String(o.id) === String(val));
            this.filters[colKey] = selectedOpt ? selectedOpt.name : '';

            // Run client-side cascade filtering
            this.filterDropdownCascade(colKey);
        } else {
            this.filterIds[colKey] = val || null;
            if (val && this.tableConfig?.dynamicDropdownKeys?.[colKey]) {
                const selectedOpt = (this.dynamicOptions[colKey] || []).find(o => String(o.id) === String(val));
                this.filters[colKey] = selectedOpt ? selectedOpt.name : String(val);
            } else {
                this.filters[colKey] = val || '';
            }
        }

        this.search();
    }

    /**
     * Unified bidirectional cascading filter engine:
     * - On initial load, all dropdowns show all available values.
     * - Selecting a value down the hierarchy restricts all child dropdowns.
     * - Selecting a child value automatically checks/selects parent fields.
     */
    filterDropdownCascade(changedKey: string): void {
        // LGD cascade order — must match onFilterValChange hierarchy exactly.
        // country → state → district → town (Sub-District) → block → village
        const hierarchy = ['country', 'state', 'district', 'town', 'block', 'village'];
        const changedIdx = hierarchy.indexOf(changedKey);
        if (changedIdx === -1) return;

        const changedValue = this.filterIds[changedKey];

        // 1. BACKWARD traverse (child selects parent automatically)
        if (changedValue) {
            for (let i = changedIdx; i > 0; i--) {
                const childKey = hierarchy[i];
                const parentKey = hierarchy[i - 1];
                const childVal = this.filterIds[childKey];

                if (childVal) {
                    const childList = this.unfilteredOptions[childKey] || [];
                    const selectedChild = childList.find(o => String(o.id) === String(childVal));
                    if (selectedChild && selectedChild.parentId) {
                        // Set parent value in UI
                        this.filterIds[parentKey] = selectedChild.parentId;
                        const parentList = this.unfilteredOptions[parentKey] || [];
                        const parentOpt = parentList.find(o => String(o.id) === String(selectedChild.parentId));
                        this.filters[parentKey] = parentOpt ? parentOpt.name : '';
                    }
                }
            }
        } else {
            // If the user clears a value, clear all children down the hierarchy
            for (let i = changedIdx + 1; i < hierarchy.length; i++) {
                const childKey = hierarchy[i];
                this.filterIds[childKey] = null;
                this.filters[childKey] = '';
            }
        }

        // 2. FORWARD traverse (parent filters child dropdown options)
        // Reset dynamicOptions first
        hierarchy.forEach(key => {
            this.dynamicOptions[key] = [...(this.unfilteredOptions[key] || [])];
        });

        // Cascading filter loop
        for (let i = 0; i < hierarchy.length; i++) {
            const currentKey = hierarchy[i];
            const currentValue = this.filterIds[currentKey];

            if (currentValue) {
                let activeParentIds = [currentValue];

                for (let j = i + 1; j < hierarchy.length; j++) {
                    const childKey = hierarchy[j];
                    const fullChildList = this.unfilteredOptions[childKey] || [];
                    
                    const filtered = fullChildList.filter(item => 
                        item.parentId && activeParentIds.includes(item.parentId)
                    );
                    this.dynamicOptions[childKey] = filtered;
                    activeParentIds = filtered.map(c => c.id);
                }
            }
        }
        this.updateSelectOptionsMap();
    }

    toggleSearchPanel(): void {
        this.isSearchExpanded = !this.isSearchExpanded;
    }

    setActiveTab(tab: 'ACTIVE' | 'INACTIVE'): void {
        if (this.activeTab === tab) return;
        this.activeTab = tab;
        this.activeTabChange.emit(tab);
        // Reset filters and selection on tab change
        this.resetFilters();
        this.selectedItems = [];
    }

    search(): void {
        // Emit filter state (key→value map) to the parent for backend filtering
        this.filterChange.emit(this.filters);

        // Only do local filtering if not backend-filtered
        if (!this.backendFiltering) {
            this.applyFilter();
        }
    }

    resetFilters(): void {
        Object.keys(this.filters).forEach(k => {
            this.filters[k] = '';
            this.filterIds[k] = null;
        });
        // Restore all dynamic dropdowns to their full unfiltered lists.
        // On initial load, loadDependentOptions() seeds unfilteredOptions.
        // After reset, every dropdown shows all available values (pre-cascade).
        Object.keys(this.dynamicOptions).forEach(k => {
            this.dynamicOptions[k] = [...(this.unfilteredOptions[k] || [])];
        });
        if (!this.backendFiltering) {
            this.applyFilter();
        }
        this.filterChange.emit(this.filters);
        this.searchChange.emit('');
    }

    private applyFilter(): void {
        const activeFilters = Object.keys(this.filters).filter(k => this.filters[k]?.trim());
        if (activeFilters.length === 0) {
            this.filteredData = [...this.data];
        } else {
            this.filteredData = this.data.filter(item => {
                return activeFilters.every(k => {
                    const val = this.getDataValue(item, k);
                    const query = this.filters[k].trim().toLowerCase();
                    if (val == null) return false;
                    if (Array.isArray(val)) {
                        return val.some(v => {
                            const str = typeof v === 'object' ? (v.name || v.key || '') : v;
                            return String(str).toLowerCase().includes(query);
                        });
                    }
                    return String(val).toLowerCase().includes(query);
                });
            });
        }
        this.applySort();
    }

    onSort(columnKey: string) {
        if (columnKey === 'actions') return; // Cannot sort actions
        if (this.sortColumn === columnKey) {
            this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortColumn = columnKey;
            this.sortDirection = 'asc';
        }
        this.applySort();
    }

    private applySort() {
        if (!this.sortColumn) return;
        
        this.filteredData.sort((a, b) => {
            let valA = this.getDataValue(a, this.sortColumn);
            let valB = this.getDataValue(b, this.sortColumn);
            
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            
            if (valA < valB) return this.sortDirection === 'asc' ? -1 : 1;
            if (valA > valB) return this.sortDirection === 'asc' ? 1 : -1;
            return 0;
        });
    }

    get displayData(): any[] {
        // In backend-filtering mode, always show the raw server data (no local filtering)
        if (this.backendFiltering) return this.data;
        return this.searchable ? this.filteredData : this.data;
    }

    get displayTotal(): number {
        if (this.backendFiltering) return this.totalElements;
        const hasActiveFilters = Object.values(this.filters).some(val => val?.trim());
        return this.searchable && hasActiveFilters
            ? this.filteredData.length
            : this.totalElements;
    }

    // ── Selection ─────────────────────────────────────────────────────────────
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
