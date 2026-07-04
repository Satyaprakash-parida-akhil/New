import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { FileUploadComponent } from '../../../../shared/components/file-upload/file-upload.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-dealer-products',
  standalone: true,
  imports: [CommonModule, FormsModule, DataTableComponent, FileUploadComponent, TranslateModule],
  templateUrl: './dealer-products.component.html',
  styleUrls: ['./dealer-products.component.css']
})
export class DealerProductsComponent implements OnInit {
  
  products: any[] = [];
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
  editingProduct: any = null;
  productForm = {
    name: '',
    category: '',
    description: '',
    imageUrl: ''
  };

  // Table Configuration
  headers: any[] = [
    { key: 'name', label: 'Product Name' },
    { key: 'category', label: 'Category' },
    { key: 'status', label: 'Status', type: 'status' },
    { key: 'actions', label: 'Actions', type: 'master_detail_actions' }
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
        { key: 'name', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_PRODUCT_NAME') },
        { key: 'category', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_CATEGORY') },
        { key: 'status', label: this.translate.instant('COMMON.STATUS'), type: 'status' },
        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'master_detail_actions' }
      ];
    });
  }

  isAdmin = false;

  ngOnInit(): void {
    this.headers = [
      { key: 'name', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_PRODUCT_NAME') },
      { key: 'category', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_CATEGORY') },
      { key: 'status', label: this.translate.instant('COMMON.STATUS'), type: 'status' },
      { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'master_detail_actions' }
    ];

    const userProfile = this.auth.getUserProfile();
    this.isAdmin = userProfile?.roles?.includes('ROLE_ADMIN');
    
    if (!this.isAdmin) {
      this.headers = this.headers.filter(h => h.key !== 'actions');
    }
    
    this.loadProducts();
  }

  toggleFilter(): void {
    this.isFilterOpen = !this.isFilterOpen;
  }

  loadProducts(): void {
    this.isLoading = true;
    
    const params = {
      searchTerm: this.filters.searchTerm || null,
      page: this.page,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };

    this.api.getAllDealerProducts(params).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.products = res.data.content;
          this.totalItems = res.data.totalElements;
        }
      },
      error: () => {
        this.isLoading = false;
        this.toast.showError('Failed to load products');
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadProducts();
  }

  resetFilters(): void {
    this.filters = { searchTerm: '' };
    this.page = 0;
    this.loadProducts();
  }

  onPageChange(pageIndex: number): void {
    this.page = pageIndex;
    this.loadProducts();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.page = 0;
    this.loadProducts();
  }

  openAddModal(): void {
    this.editingProduct = null;
    this.productForm = { name: '', category: '', description: '', imageUrl: '' };
    this.isModalOpen = true;
  }

  openEditModal(product: any): void {
    this.editingProduct = product;
    this.productForm = { 
      name: product.name, 
      category: product.category, 
      description: product.description, 
      imageUrl: product.imageUrl 
    };
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.resetForm();
  }

  onImageUploaded(url: string): void {
    this.productForm.imageUrl = url;
  }

  resetForm(): void {
    this.productForm = { name: '', category: '', description: '', imageUrl: '' };
  }

  saveProduct(): void {
    const apiCall = this.editingProduct 
      ? this.api.updateDealerProduct(this.editingProduct.id, this.productForm)
      : this.api.createDealerProduct(this.productForm);

    apiCall.subscribe({
      next: (res) => {
        if (res.success) {
          this.toast.showSuccess(this.editingProduct ? 'Product updated successfully' : 'Product created successfully');
          this.closeModal();
          this.loadProducts();
        } else {
          this.toast.showError('Failed to save product');
        }
      },
      error: () => this.toast.showError('Error saving product')
    });
  }

  onEdit(row: any): void {
    this.openEditModal(row);
  }

  onDelete(row: any): void {
    if (confirm(`Are you sure you want to delete ${row.name}?`)) {
      this.api.deleteDealerProduct(row.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast.showSuccess('Product deleted successfully');
            this.loadProducts();
          } else {
            this.toast.showError('Failed to delete product');
          }
        },
        error: () => this.toast.showError('Error deleting product')
      });
    }
  }

  onRecover(row: any): void {
    if (confirm(`Are you sure you want to recover ${row.name}?`)) {
      this.api.recoverDealerProduct(row.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast.showSuccess('Product recovered successfully');
            this.loadProducts();
          } else {
            this.toast.showError('Failed to recover product');
          }
        },
        error: () => this.toast.showError('Error recovering product')
      });
    }
  }

  onPermanentDelete(row: any): void {
    if (confirm(`Are you sure you want to permanently delete ${row.name}? This action cannot be undone.`)) {
      this.api.permanentDeleteDealerProduct(row.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast.showSuccess('Product permanently deleted');
            this.loadProducts();
          } else {
            this.toast.showError('Failed to permanently delete product');
          }
        },
        error: () => this.toast.showError('Error deleting product permanently')
      });
    }
  }
}
