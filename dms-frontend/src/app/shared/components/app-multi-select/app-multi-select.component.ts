import { Component, Input, Output, EventEmitter, ElementRef, OnDestroy, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-multi-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app-multi-select.component.html',
  styleUrl: './app-multi-select.component.css'
})
export class AppMultiSelectComponent implements OnDestroy, OnChanges {
  String = String;
  @Input() options: { id: number | string, name: string }[] = [];
  @Input() value: any[] = [];
  @Input() placeholder: string = 'Select an option';
  @Input() disabled: boolean = false;
  @Input() searchable: boolean = true;
  @Input() clearable: boolean = true;
  @Input() masterCategory?: string;
  @Input() masterParentId?: number | null;
  @Output() valueChange = new EventEmitter<any[]>();

  isOpen = false;
  searchTerm = '';
  dropdownEl: HTMLElement | null = null;

  // Position for fixed overlay
  dropdownTop = 0;
  dropdownLeft = 0;
  dropdownWidth = 0;
  dropdownOpenAbove = false;
  get windowHeight() { return window.innerHeight; }

  private clickListener: any = null;
  private resizeListener: any = null;
  private scrollListener: any = null;

  constructor(
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef,
    private apiService: ApiService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    // If options change while open, re-run change detection
    if (changes['options'] && this.isOpen) {
      this.cdr.detectChanges();
    }
  }

  get selectedLabel(): string {
    if (!this.value || this.value.length === 0) return '';
    if (this.value.length === 1) {
      const selected = this.options.find(o => String(o.id) === String(this.value[0]));
      return selected ? selected.name : String(this.value[0]);
    }
    return `${this.value.length} selected`;
  }

  hasExactMatch(term: string): boolean {
    if (!term || !term.trim()) return true;
    const t = term.toLowerCase().trim();
    return (this.options || []).some(o => o && o.name && String(o.name).toLowerCase().trim() === t);
  }

  get filteredOptions(): { id: number | string, name: string }[] {
    const opts = [...(this.options || [])];
    if (this.value && Array.isArray(this.value)) {
      for (const val of this.value) {
        if (!opts.some(o => String(o.id) === String(val))) {
          opts.push({ id: val, name: String(val) });
        }
      }
    }
    if (!this.searchable || !this.searchTerm.trim()) {
      return opts;
    }
    const term = this.searchTerm.toLowerCase().trim();
    return opts.filter(o => o && o.name && String(o.name).toLowerCase().includes(term));
  }

  toggleDropdown() {
    if (this.disabled) return;
    if (this.isOpen) {
      this.closeDropdown();
    } else {
      this.openDropdown();
    }
  }

  openDropdown() {
    this.isOpen = true;
    this.searchTerm = '';
    this.cdr.detectChanges();

    const dropdownEl = this.elementRef.nativeElement.querySelector('.app-select-dropdown') as HTMLElement;
    if (dropdownEl) {
      document.body.appendChild(dropdownEl);
      this.dropdownEl = dropdownEl;
      this.updatePosition();
    }

    this.clickListener = (event: MouseEvent) => {
      const triggerEl = this.elementRef.nativeElement.querySelector('.app-select-trigger');
      const clickedTrigger = triggerEl && triggerEl.contains(event.target as Node);
      const clickedDropdown = this.dropdownEl && this.dropdownEl.contains(event.target as Node);

      if (!clickedTrigger && !clickedDropdown) {
        this.closeDropdown();
        this.cdr.detectChanges();
      }
    };
    this.resizeListener = () => { this.updatePosition(); this.cdr.detectChanges(); };
    this.scrollListener = () => { this.updatePosition(); this.cdr.detectChanges(); };

    document.addEventListener('click', this.clickListener);
    window.addEventListener('resize', this.resizeListener);
    window.addEventListener('scroll', this.scrollListener, true);

    // Focus search after open
    setTimeout(() => {
      const searchInput = this.dropdownEl?.querySelector('.app-select-search-input') as HTMLInputElement;
      if (searchInput) searchInput.focus();
    }, 30);
  }

  closeDropdown() {
    if (this.dropdownEl && this.dropdownEl.parentNode === document.body) {
      // Return to host element so Angular's *ngIf can gracefully destroy it
      this.elementRef.nativeElement.appendChild(this.dropdownEl);
    }
    this.isOpen = false;
    this.searchTerm = '';
    this.dropdownEl = null;

    if (this.clickListener) {
      document.removeEventListener('click', this.clickListener);
      window.removeEventListener('resize', this.resizeListener);
      window.removeEventListener('scroll', this.scrollListener, true);
      this.clickListener = null;
      this.resizeListener = null;
      this.scrollListener = null;
    }
  }

  updatePosition() {
    const triggerEl = this.elementRef.nativeElement.querySelector('.app-select-trigger') as HTMLElement;
    if (!triggerEl) return;
    const rect = triggerEl.getBoundingClientRect();

    this.dropdownWidth = rect.width;
    this.dropdownLeft = rect.left + window.scrollX;

    // User requested to always open dropdowns downward
    this.dropdownOpenAbove = false;
    this.dropdownTop = rect.bottom + window.scrollY + 4;

    if (this.dropdownEl) {
      this.dropdownEl.style.position = 'absolute';
      this.dropdownEl.style.zIndex = '999999';
      this.dropdownEl.style.left = this.dropdownLeft + 'px';
      this.dropdownEl.style.width = this.dropdownWidth + 'px';
      this.dropdownEl.style.minWidth = this.dropdownWidth + 'px';

      const optionsContainer = this.dropdownEl.querySelector('.app-select-options') as HTMLElement;

      this.dropdownEl.style.bottom = 'auto';
      this.dropdownEl.style.top = this.dropdownTop + 'px';
      
      if (optionsContainer) {
        optionsContainer.style.maxHeight = '240px';
      }
    }
  }

  selectOption(option: any) {
    if (this.disabled) return;
    let newValue = Array.isArray(this.value) ? [...this.value] : [];
    const index = newValue.findIndex(v => String(v) === String(option.id));
    if (index >= 0) {
      newValue.splice(index, 1);
    } else {
      newValue.push(option.id);
    }
    this.value = newValue;
    this.valueChange.emit(this.value);
  }

  isSelected(option: any): boolean {
    if (!this.value || !Array.isArray(this.value)) return false;
    return this.value.some(v => String(v) === String(option.id));
  }

  clearSelection(event: Event) {
    event.stopPropagation();
    if (this.disabled) return;
    this.value = [];
    this.valueChange.emit(this.value);
    if (this.isOpen) {
      this.closeDropdown();
    }
  }

  onSearchInput(event: Event) {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    this.closeDropdown();
  }
}
