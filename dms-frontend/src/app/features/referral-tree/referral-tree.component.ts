import { Component, OnInit, OnDestroy, HostListener, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../core/services/user.service';
import { ReferralNode } from '../../shared/models/user.model';
import { ToastService } from '../../core/services/toast.service';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AppSelectComponent } from '../../shared/components/app-select/app-select.component';

interface FlatNode {
  data: ReferralNode;
  level: number;
  isExpanded: boolean;
  isLoading: boolean;
  childrenLoaded: boolean;
  hasChildren: boolean;
  isVisible: boolean;
}

@Component({
  selector: 'app-referral-tree',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, AppSelectComponent],
  templateUrl: './referral-tree.component.html',
  styleUrl: './referral-tree.component.css'
})
export class ReferralTreeComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('treeContainer') treeContainer!: ElementRef;

  isLoading = true;
  isAdmin = false;

  // Flat list of all loaded nodes in display order
  flatNodes: FlatNode[] = [];

  // Viewport and virtual scroll
  visibleNodes: FlatNode[] = [];
  itemHeight = 90;
  viewportHeight = 800;
  scrollTop = 0;
  totalHeight = 0;
  paddingTop = 0;

  // Pagination State (server-side connected to backend)
  currentPage = 0;
  pageSize = 5;
  totalElements = 0;
  totalPages = 0;
  isLast = true;
  pageSizeOptions = [5, 10, 25, 50];
  pageSizeSelectOptions: { id: number, name: string }[] = [];

  get visibleFlatNodes(): FlatNode[] {
    return this.flatNodes.filter(n => n.isVisible);
  }

  // Pan and Zoom
  zoomLevel = 1;
  panX = 0;
  panY = 0;
  dragging = false;
  private dragStartX = 0;
  private dragStartY = 0;

  // Search
  searchTerm = '';
  showSearchDropdown = false;
  searchResults: any[] = [];
  isSearching = false;
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private readonly userService: UserService,
    private readonly toast: ToastService
  ) {}

  ngOnInit() {
    this.pageSizeSelectOptions = this.pageSizeOptions.map(opt => ({ id: opt, name: String(opt) }));
    this.detectRole();
    this.loadRoot();
    this.setupSearch();
  }

  ngAfterViewInit() {
    this.updateViewportHeight();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get zoomPercent(): number {
    return Math.round(this.zoomLevel * 100);
  }

  @HostListener('window:resize')
  onResize() {
    this.updateViewportHeight();
  }

  updateViewportHeight() {
    if (this.treeContainer) {
      this.viewportHeight = this.treeContainer.nativeElement.clientHeight || 800;
      this.updateVirtualScroll();
    }
  }

  detectRole() {
    const token = localStorage.getItem('access_token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const roles: string[] = (payload.roles || []).map((r: any) => r.authority || r);
        this.isAdmin = roles.includes('ROLE_ADMIN');
      } catch { /* ignore */ }
    }
  }

  loadRoot(page = this.currentPage, size = this.pageSize) {
    this.isLoading = true;
    this.currentPage = page;
    this.pageSize = size;

    this.userService.getMyReferralTree(this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success && res.data) {
          const content = res.data.content || [];
          this.totalElements = res.data.totalElements || 0;
          this.totalPages = res.data.totalPages || 0;
          this.isLast = res.data.last ?? true;

          // Map top-level nodes directly as root nodes
          this.flatNodes = content.map((node: ReferralNode) => ({
            data: node,
            level: 0,
            isExpanded: false,
            isLoading: false,
            childrenLoaded: false,
            hasChildren: (node.referralCount ?? 0) > 0,
            isVisible: true
          }));

          // If only 1 root node is returned (e.g. regular user) and it has children, auto-expand it
          if (this.flatNodes.length === 1 && this.flatNodes[0].hasChildren) {
            this.expandNode(this.flatNodes[0]);
          }

          this.updateVirtualScroll();
        }
      },
      error: () => {
        this.isLoading = false;
        this.toast.showError('Failed to load referral tree');
      }
    });
  }

  // ─── PAGINATION ACTIONS ────────────────────────────────────────────────────────

  onPrevPage() {
    if (this.currentPage > 0) {
      this.loadRoot(this.currentPage - 1);
    }
  }

  onNextPage() {
    if (!this.isLast) {
      this.loadRoot(this.currentPage + 1);
    }
  }

  onPageSizeChange(size: any) {
    const newSize = Number(size);
    if (!newSize || newSize === this.pageSize) return;
    this.currentPage = 0;
    this.flatNodes = [];
    this.visibleNodes = [];
    this.loadRoot(0, newSize);
  }

  // ─── TREE EXPAND/COLLAPSE ─────────────────────────────────────────────────────

  toggleNode(node: FlatNode) {
    if (node.isExpanded) {
      this.collapseNode(node);
    } else {
      this.expandNode(node);
    }
  }

  expandNode(node: FlatNode) {
    if (!node.hasChildren) return;
    node.isExpanded = true;

    if (node.childrenLoaded) {
      this.setVisibilityRecursive(node, true);
      this.updateVirtualScroll();
    } else {
      node.isLoading = true;
      this.userService.getReferralChildren(node.data.userId).subscribe({
        next: (res) => {
          node.isLoading = false;
          if (res.success && res.data) {
            const childrenData = res.data as ReferralNode[];
            const newNodes = childrenData.map(child => ({
              data: child,
              level: node.level + 1,
              isExpanded: false,
              isLoading: false,
              childrenLoaded: false,
              hasChildren: (child.referralCount ?? 0) > 0,
              isVisible: true
            }));
            const index = this.flatNodes.indexOf(node);
            this.flatNodes.splice(index + 1, 0, ...newNodes);
            node.childrenLoaded = true;
            this.updateVirtualScroll();
          }
        },
        error: () => {
          node.isLoading = false;
          node.isExpanded = false;
          this.toast.showError('Failed to load children');
        }
      });
    }
  }

  collapseNode(node: FlatNode) {
    node.isExpanded = false;
    this.setVisibilityRecursive(node, false);
    this.updateVirtualScroll();
  }

  private setVisibilityRecursive(parentNode: FlatNode, isVisible: boolean) {
    const parentIndex = this.flatNodes.indexOf(parentNode);
    if (parentIndex === -1) return;
    const parentLevel = parentNode.level;
    let i = parentIndex + 1;
    while (i < this.flatNodes.length && this.flatNodes[i].level > parentLevel) {
      const childNode = this.flatNodes[i];
      if (!isVisible) {
        childNode.isVisible = false;
      } else {
        if (childNode.level === parentLevel + 1) {
          childNode.isVisible = true;
        } else {
          let parentVisibleAndExpanded = false;
          for (let j = i - 1; j >= parentIndex; j--) {
            if (this.flatNodes[j].level === childNode.level - 1) {
              parentVisibleAndExpanded = this.flatNodes[j].isVisible && this.flatNodes[j].isExpanded;
              break;
            }
          }
          childNode.isVisible = parentVisibleAndExpanded;
        }
      }
      i++;
    }
  }

  // ─── SEARCH ──────────────────────────────────────────────────────────────────

  setupSearch() {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(term => {
      if (term && term.trim().length >= 1) {
        this.performSearch(term.trim());
      } else {
        this.searchResults = [];
        this.showSearchDropdown = false;
        this.isSearching = false;
      }
    });
  }

  onSearchInput() {
    const term = this.searchTerm.trim();
    if (!term) {
      this.isSearching = true;
      this.showSearchDropdown = true;
      this.searchSubject.next('');
      return;
    }
    this.isSearching = true;
    this.showSearchDropdown = true;
    this.searchSubject.next(term);
  }

  onSearchFocus() {
    // Show dropdown immediately on focus/click — triggers a blank search to load defaults
    this.showSearchDropdown = true;
    if (!this.searchTerm.trim() && this.searchResults.length === 0) {
      this.isSearching = true;
      this.searchSubject.next('');
    } else if (this.searchResults.length > 0) {
      this.showSearchDropdown = true;
    }
  }

  performSearch(term: string) {
    this.userService.searchReferralTree(term).subscribe({
      next: (res) => {
        this.isSearching = false;
        if (res.success && res.data) {
          this.searchResults = res.data as any[];
          this.showSearchDropdown = true;
        } else {
          this.searchResults = [];
        }
      },
      error: () => {
        this.isSearching = false;
        this.searchResults = [];
        this.toast.showError('Search failed');
      }
    });
  }

  selectSearchResult(user: any) {
    this.showSearchDropdown = false;
    this.searchTerm = user.username || user.fullName || '';
    this.navigateToUser(user.id || user.userId);
  }

  navigateToUser(userId: number) {
    if (!userId) return;
    this.isLoading = true;
    this.userService.getFullReferralTree(userId).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success && res.data) {
          const nodeData = res.data as ReferralNode;
          this.flatNodes = [{
            data: nodeData,
            level: 0,
            isExpanded: false,
            isLoading: false,
            childrenLoaded: false,
            hasChildren: (nodeData.referralCount ?? 0) > 0,
            isVisible: true
          }];
          this.totalElements = 1;
          this.totalPages = 1;
          this.isLast = true;
          this.currentPage = 0;
          this.updateVirtualScroll();
          if (this.flatNodes[0].hasChildren) {
            this.expandNode(this.flatNodes[0]);
          }
        }
      },
      error: () => {
        this.isLoading = false;
        this.toast.showError('Failed to load user tree');
      }
    });
  }

  clearSearch() {
    this.searchTerm = '';
    this.searchResults = [];
    this.showSearchDropdown = false;
    this.isSearching = false;
    this.currentPage = 0;
    this.loadRoot();
  }

  closeDropdown() {
    setTimeout(() => {
      this.showSearchDropdown = false;
    }, 200);
  }

  // ─── VIRTUAL SCROLL ───────────────────────────────────────────────────────────

  onScroll(event: Event) {
    const target = event.target as HTMLElement;
    this.scrollTop = target.scrollTop;
    this.updateVirtualScroll();
  }

  updateVirtualScroll() {
    const visibleDisplayNodes = this.flatNodes.filter(n => n.isVisible);
    this.totalHeight = visibleDisplayNodes.length * this.itemHeight;

    const effectiveViewportHeight = this.viewportHeight / this.zoomLevel;
    const effectiveScrollTop = this.scrollTop / this.zoomLevel;

    let startIndex = Math.floor(effectiveScrollTop / this.itemHeight);
    let endIndex = startIndex + Math.ceil(effectiveViewportHeight / this.itemHeight) + 2;

    startIndex = Math.max(0, startIndex - 5);
    endIndex = Math.min(visibleDisplayNodes.length, endIndex + 5);

    this.paddingTop = startIndex * this.itemHeight;
    this.visibleNodes = visibleDisplayNodes.slice(startIndex, endIndex);
  }

  // ─── PAN AND ZOOM ─────────────────────────────────────────────────────────────

  zoomIn() {
    this.zoomLevel = Math.min(this.zoomLevel + 0.1, 2);
    this.updateVirtualScroll();
  }

  zoomOut() {
    this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.5);
    this.updateVirtualScroll();
  }

  resetZoom() {
    this.zoomLevel = 1;
    this.panX = 0;
    this.panY = 0;
    this.updateVirtualScroll();
  }

  onWheel(event: WheelEvent) {
    if (event.ctrlKey) {
      event.preventDefault();
      if (event.deltaY < 0) this.zoomIn();
      else this.zoomOut();
    }
  }

  onMouseDown(event: MouseEvent) {
    if ((event.target as HTMLElement).closest('.node-card')) return;
    this.dragging = true;
    this.dragStartX = event.clientX - this.panX;
    this.dragStartY = event.clientY - this.panY;
    document.body.style.cursor = 'grabbing';
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    if (!this.dragging) return;
    this.panX = event.clientX - this.dragStartX;
    this.panY = event.clientY - this.dragStartY;
  }

  @HostListener('document:mouseup')
  onMouseUp() {
    if (this.dragging) {
      this.dragging = false;
      document.body.style.cursor = 'default';
    }
  }

  // ─── UTILS ────────────────────────────────────────────────────────────────────

  isPaid(node: FlatNode): boolean {
    return node.data.paymentStatus === 'COMPLETED' || node.data.paymentStatus === 'PAID' || node.data.paymentStatus === 'APPROVED';
  }

  getInitial(node: FlatNode): string {
    return (node.data.fullName || node.data.username || 'U').charAt(0).toUpperCase();
  }

  getAvatarGradient(node: FlatNode): string {
    const colors = [
      'from-violet-500 to-purple-600',
      'from-blue-500 to-indigo-600',
      'from-emerald-500 to-teal-600',
      'from-orange-500 to-amber-600',
      'from-rose-500 to-pink-600',
      'from-cyan-500 to-blue-600',
    ];
    const idx = (node.data.userId || 0) % colors.length;
    return colors[idx];
  }

  getSearchInitial(user: any): string {
    return (user.fullName || user.username || 'U').charAt(0).toUpperCase();
  }

  /** Indent px = level * 40, with max cap for very deep nodes, responsive on mobile */
  getIndent(node: FlatNode): number {
    const isMobile = typeof window !== 'undefined' && window.innerWidth < 640;
    const step = isMobile ? 16 : 40;
    const maxCap = isMobile ? 120 : 400;
    return Math.min(node.level * step, maxCap);
  }

  /** Whether this node has a visible parent connector line */
  hasParentConnector(node: FlatNode): boolean {
    return node.level > 0;
  }

  /** trackBy for *ngFor performance */
  trackNode(index: number, node: FlatNode): number {
    return node.data.userId;
  }
}
