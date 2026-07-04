const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, 'src/app/features/admin/dealer-management/dealer-verification/dealer-verification.component.html');
let html = fs.readFileSync(htmlPath, 'utf8');

// Replace strings
html = html.replace(/Pending Verifications/g, "{{ 'DEALER_VERIFICATION.TITLE_ADMIN' | translate }}");
html = html.replace(/\{\{\s*isFilterOpen\s*\?\s*'Close Filters'\s*:\s*'Search & Filter'\s*\}\}/g, "{{ isFilterOpen ? ('DEALER_VERIFICATION.CLOSE_FILTERS' | translate) : ('DEALER_VERIFICATION.SEARCH_FILTER' | translate) }}");
html = html.replace(/placeholder="Search by name, state\.\.\."/g, "[placeholder]=\"'DEALER_VERIFICATION.SEARCH_PLACEHOLDER' | translate\"");
html = html.replace(/\[placeholder\]="'Select Status'"/g, "[placeholder]=\"'DEALER_VERIFICATION.SELECT_STATUS' | translate\"");
html = html.replace(/>\s*Apply Filters\s*</g, ">{{ 'DEALER_VERIFICATION.APPLY_FILTERS' | translate }}<");
html = html.replace(/>\s*Reset\s*</g, ">{{ 'DEALER_VERIFICATION.RESET' | translate }}<");
html = html.replace(/\[title\]="'Dealers'"/g, "[title]=\"'DEALER_VERIFICATION.TABLE_TITLE' | translate\"");

html = html.replace(/Verification Complete/g, "{{ 'DEALER_VERIFICATION.VERIFIED_TITLE' | translate }}");
html = html.replace(/Your dealer profile has been successfully verified by an administrator\. You can now access all dealer features\./g, "{{ 'DEALER_VERIFICATION.VERIFIED_MSG' | translate }}");
html = html.replace(/Verification Pending/g, "{{ 'DEALER_VERIFICATION.PENDING_TITLE' | translate }}");
html = html.replace(/Your dealer profile verification is currently under review by an administrator\. Please check back later\./g, "{{ 'DEALER_VERIFICATION.PENDING_MSG' | translate }}");
html = html.replace(/Your previous verification attempt was rejected\. Please review your details and submit again\./g, "{{ 'DEALER_VERIFICATION.REJECTED_MSG' | translate }}");

html = html.replace(/\{\{\s*step === 1 \? 'Basic Details' : step === 2 \? 'Documents' : 'Review'\s*\}\}/g, "{{ step === 1 ? ('DEALER_VERIFICATION.STEP_BASIC' | translate) : step === 2 ? ('DEALER_VERIFICATION.STEP_DOCS' | translate) : ('DEALER_VERIFICATION.STEP_REVIEW' | translate) }}");
html = html.replace(/Basic Information/g, "{{ 'DEALER_VERIFICATION.BASIC_INFO' | translate }}");
html = html.replace(/>State<\/label>/g, ">{{ 'DEALER_VERIFICATION.STATE' | translate }}</label>");
html = html.replace(/>District<\/label>/g, ">{{ 'DEALER_VERIFICATION.DISTRICT' | translate }}</label>");
html = html.replace(/>Area \/ City<\/label>/g, ">{{ 'DEALER_VERIFICATION.AREA_CITY' | translate }}</label>");
html = html.replace(/>PIN Code<\/label>/g, ">{{ 'DEALER_VERIFICATION.PIN_CODE' | translate }}</label>");
html = html.replace(/>Full Address<\/label>/g, ">{{ 'DEALER_VERIFICATION.FULL_ADDRESS' | translate }}</label>");
html = html.replace(/Identity Verification/g, "{{ 'DEALER_VERIFICATION.IDENTITY_VERIFICATION' | translate }}");

fs.writeFileSync(htmlPath, html, 'utf8');

// Now TypeScript Headers
const tsPath = path.join(__dirname, 'src/app/features/admin/dealer-management/dealer-verification/dealer-verification.component.ts');
let ts = fs.readFileSync(tsPath, 'utf8');

// Inject TranslateService
if (!ts.includes('private translate: TranslateService')) {
  ts = ts.replace(/import \{ Component, OnInit \} from '@angular\/core';/, "import { Component, OnInit } from '@angular/core';\nimport { TranslateService } from '@ngx-translate/core';");
  
  if(ts.includes('constructor(private api: ApiService, private toast: ToastService) {}')) {
      ts = ts.replace(/constructor\(private api: ApiService, private toast: ToastService\) \{\}/, "constructor(private api: ApiService, private toast: ToastService, private translate: TranslateService) {\n    this.initHeaders();\n  }\n\n  initHeaders() {\n    this.translate.onLangChange.subscribe(() => {\n      this.headers = [\n        { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },\n        { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },\n        { key: 'state', label: this.translate.instant('DEALER_VERIFICATION.STATE') },\n        { key: 'district', label: this.translate.instant('DEALER_VERIFICATION.DISTRICT') },\n        { key: 'verificationStatus', label: this.translate.instant('DEALER_VERIFICATION.SELECT_STATUS'), type: 'status' },\n        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'actions' }\n      ];\n    });\n  }");
  } else {
      ts = ts.replace(/constructor\((.*?)\)\s*\{/g, "constructor($1, private translate: TranslateService) {\n    this.initHeaders();");
      ts = ts.replace(/ngOnInit\(\): void \{/, "initHeaders() {\n    this.translate.onLangChange.subscribe(() => {\n      this.headers = [\n        { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },\n        { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },\n        { key: 'state', label: this.translate.instant('DEALER_VERIFICATION.STATE') },\n        { key: 'district', label: this.translate.instant('DEALER_VERIFICATION.DISTRICT') },\n        { key: 'verificationStatus', label: this.translate.instant('DEALER_VERIFICATION.SELECT_STATUS'), type: 'status' },\n        { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'actions' }\n      ];\n    });\n  }\n\n  ngOnInit(): void {");
  }

  // Replace headers initial assignment
  ts = ts.replace(/headers: any\[\] = \[\s*\{\s*key:\s*'username'[\s\S]*?\];/m, "headers: any[] = [\n    { key: 'username', label: 'Dealer' },\n    { key: 'email', label: 'Email' },\n    { key: 'state', label: 'State' },\n    { key: 'district', label: 'District' },\n    { key: 'verificationStatus', label: 'Status', type: 'status' },\n    { key: 'actions', label: 'Actions', type: 'actions' }\n  ];");
  
  ts = ts.replace(/ngOnInit\(\): void \{/, "ngOnInit(): void {\n    this.headers = [\n      { key: 'username', label: this.translate.instant('ADMIN.DEALER_MGMT.COL_DEALER') },\n      { key: 'email', label: this.translate.instant('AUTH.REGISTER.EMAIL_LABEL') },\n      { key: 'state', label: this.translate.instant('DEALER_VERIFICATION.STATE') },\n      { key: 'district', label: this.translate.instant('DEALER_VERIFICATION.DISTRICT') },\n      { key: 'verificationStatus', label: this.translate.instant('DEALER_VERIFICATION.SELECT_STATUS'), type: 'status' },\n      { key: 'actions', label: this.translate.instant('COMMON.ACTION'), type: 'actions' }\n    ];\n");
}

fs.writeFileSync(tsPath, ts, 'utf8');
console.log('Verification Component replaced');
