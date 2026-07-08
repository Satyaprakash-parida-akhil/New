import { Component, Input, Output, EventEmitter, ElementRef, OnDestroy, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app-select.component.html',
  styleUrl: './app-select.component.css'
})
export class AppSelectComponent implements OnDestroy, OnChanges {
  String = String;
  @Input() options: { id: number | string, name: string }[] = [];
  @Input() value: any;
  @Input() placeholder: string = 'Select an option';
  @Input() disabled: boolean = false;
  @Input() searchable: boolean = true;
  @Input() clearable: boolean = true;
  @Input() masterCategory?: string;
  @Input() masterParentId?: number | null;
  @Output() valueChange = new EventEmitter<any>();

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
    if (this.value === null || this.value === undefined || this.value === '') return '';
    const selected = this.options.find(o => String(o.id) === String(this.value));
    return selected ? selected.name : String(this.value);
  }

  hasExactMatch(term: string): boolean {
    if (!term || !term.trim()) return true;
    const t = term.toLowerCase().trim();
    return (this.options || []).some(o => o && o.name && String(o.name).toLowerCase().trim() === t);
  }

  get filteredOptions(): { id: number | string, name: string }[] {
    const opts = [...(this.options || [])];
    if (this.value && typeof this.value === 'string' && !opts.some(o => String(o.id) === String(this.value))) {
      opts.push({ id: this.value, name: this.value });
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

    // Enforce strictly downward opening of the dropdown
    this.dropdownOpenAbove = false;
    this.dropdownTop = rect.bottom + window.scrollY + 4;

    if (this.dropdownEl) {
      this.dropdownEl.style.position = 'absolute';
      this.dropdownEl.style.zIndex = '999999';
      this.dropdownEl.style.left = this.dropdownLeft + 'px';
      this.dropdownEl.style.width = this.dropdownWidth + 'px';
      this.dropdownEl.style.minWidth = this.dropdownWidth + 'px';

      const optionsContainer = this.dropdownEl.querySelector('.app-select-options') as HTMLElement;
      if (optionsContainer) {
        optionsContainer.style.maxHeight = '180px';
      }

      this.dropdownEl.style.bottom = 'auto';
      this.dropdownEl.style.top = this.dropdownTop + 'px';
    }
  }

  selectOption(option: any) {
    if (this.masterCategory && typeof option.id === 'string' && option.id === option.name) {
      // It's a new option meant to be auto-created in master
      this.apiService.autoCreateMasterOption(this.masterCategory, option.name, this.masterParentId).subscribe(res => {
        if (res.success && res.data) {
          const newId = res.data.id;
          const newName = res.data.displayName || res.data.value || option.name;
          this.options.push({ id: newId, name: newName });
          this.value = newId;
          this.valueChange.emit(this.value);
          this.cdr.detectChanges();
        }
      });
    } else {
      this.value = option.id;
      this.valueChange.emit(this.value);
    }
    this.closeDropdown();
  }

  clearSelection(event: MouseEvent) {
    event.stopPropagation();
    this.value = null;
    this.valueChange.emit(null);
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
