import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { UserResponse, DashboardStats, ReferralNode, MasterHeader, MasterDetail } from '../../shared/models/user.model';
import { PagedResponse } from '../../shared/models/paged-response.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  // ─── AUTHENTICATION / REGISTRATION API ──────────────────────────────────────

  login(credentials: { username: string; password: string }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/auth/login`, credentials);
  }

  register(formData: FormData): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/auth/register`, formData);
  }

  sendRegisterOtp(identifier: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/auth/register/otp?identifier=${encodeURIComponent(identifier)}&method=${method}`, {}
    );
  }

  verifyRegisterOtp(identifier: string, otp: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/auth/register/verify?identifier=${encodeURIComponent(identifier)}&otp=${otp}&method=${method}`, {}
    );
  }

  forgotPassword(identifier: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/auth/forgot-password`, { identifier, method });
  }

  verifyForgotPasswordOtp(identifier: string, otp: string, method: 'EMAIL' | 'PHONE' = 'EMAIL'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/auth/forgot-password/verify?identifier=${encodeURIComponent(identifier)}&otp=${otp}&method=${method}`, {}
    );
  }

  resetPassword(data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/auth/reset-password`, data);
  }

  validatePromoCode(code: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/auth/validate-promo?code=${encodeURIComponent(code)}`);
  }

  // ─── ADMIN REGISTRATION MANAGEMENT ──────────────────────────────────────────

  getPendingRegistrations(page: number = 0, size: number = 5): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/pending?page=${page}&size=${size}`);
  }

  getInactiveRegistrations(page: number = 0, size: number = 5): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/inactive?page=${page}&size=${size}`);
  }

  getApprovedUsers(page: number = 0, size: number = 5, active?: boolean): Observable<ApiResponse<any>> {
    let url = `${this.baseUrl}/admin/registrations/approved?page=${page}&size=${size}`;
    if (active !== undefined && active !== null) {
      url += `&active=${active}`;
    }
    return this.http.get<ApiResponse<any>>(url);
  }

  approveRegistration(userId: number, role: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/approve/${userId}?role=${role}`, {});
  }

  rejectRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/reject/${userId}`, {});
  }

  softDeleteRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/soft-delete/${userId}`, {});
  }

  restoreRegistration(userId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/registrations/restore/${userId}`, {});
  }

  // ─── USER PROFILE API ───────────────────────────────────────────────────────

  getMyProfile(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/profile/me`);
  }

  updateProfile(formData: FormData): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/profile/me`, formData);
  }

  saveDealerProfile(data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/dealers/submit-verification`, data);
  }

  getMyDealerProfile(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/dealers/my-profile`);
  }

  // ─── ADMIN USER CRUD ─────────────────────────────────────────────────────────

  getDashboardStats(): Observable<ApiResponse<DashboardStats>> {
    return this.http.get<ApiResponse<DashboardStats>>(`${this.baseUrl}/users/dashboard-stats`);
  }

  getActiveUsers(page: number = 0, size: number = 10, search?: string, filters?: Record<string, string>): Observable<ApiResponse<PagedResponse<UserResponse>>> {
    let url = `${this.baseUrl}/users?isActive=true&page=${page}&size=${size}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (filters) {
      for (const [key, value] of Object.entries(filters)) {
        if (value && value.trim()) url += `&${encodeURIComponent(key)}=${encodeURIComponent(value.trim())}`;
      }
    }
    return this.http.get<ApiResponse<PagedResponse<UserResponse>>>(url);
  }

  getInactiveUsers(page: number = 0, size: number = 10, search?: string, filters?: Record<string, string>): Observable<ApiResponse<PagedResponse<UserResponse>>> {
    let url = `${this.baseUrl}/users?isActive=false&page=${page}&size=${size}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (filters) {
      for (const [key, value] of Object.entries(filters)) {
        if (value && value.trim()) url += `&${encodeURIComponent(key)}=${encodeURIComponent(value.trim())}`;
      }
    }
    return this.http.get<ApiResponse<PagedResponse<UserResponse>>>(url);
  }

  getUserById(userId: number): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`${this.baseUrl}/users/${userId}`);
  }

  updateUser(userId: number, data: any): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(`${this.baseUrl}/users/${userId}`, data);
  }

  deleteUser(userId: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/users/${userId}`);
  }

  blockUser(userId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/users/${userId}/block`, {});
  }

  recoverUser(userId: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/users/${userId}/restore`, {});
  }

  getUserFilterOptions(): Observable<ApiResponse<Record<string, string[]>>> {
    return this.http.get<ApiResponse<Record<string, string[]>>>(`${this.baseUrl}/users/filter-options`);
  }

  // ─── REFERRAL TREE API ───────────────────────────────────────────────────────

  getMyReferralTree(): Observable<ApiResponse<ReferralNode>> {
    return this.http.get<ApiResponse<ReferralNode>>(`${this.baseUrl}/users/referral-tree`);
  }

  getFullReferralTree(): Observable<ApiResponse<ReferralNode>> {
    return this.http.get<ApiResponse<ReferralNode>>(`${this.baseUrl}/users/referral-tree`);
  }

  searchReferralTree(searchTerm: string): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>(`${this.baseUrl}/users/referral-tree/search?searchTerm=${encodeURIComponent(searchTerm)}`);
  }

  // ─── MASTER DATA API ─────────────────────────────────────────────────────────

  // Returns Page<MasterHeader> from  // Backend: GET /api/admin/masters/headers
  getMasterHeaders(status = 'ACTIVE', page = 0, size = 100): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/masters/headers?status=${status}&page=${page}&size=${size}`);
  }

  createMasterHeader(data: { dropdownName: string }): Observable<ApiResponse<MasterHeader>> {
    return this.http.post<ApiResponse<MasterHeader>>(`${this.baseUrl}/admin/masters/headers`, data);
  }

  updateMasterHeader(id: number, data: { dropdownName?: string; status?: string }): Observable<ApiResponse<MasterHeader>> {
    return this.http.put<ApiResponse<MasterHeader>>(`${this.baseUrl}/admin/masters/headers/${id}`, data);
  }

  deleteMasterHeader(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/admin/masters/headers/${id}`);
  }

  restoreMasterHeader(id: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/masters/headers/${id}/restore`, {});
  }

  permanentDeleteMasterHeader(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/admin/masters/headers/${id}/permanent`);
  }

  // Backend: GET /api/admin/masters/headers/{headerId}/details
  getMasterDetails(headerId: number, status = 'ACTIVE', page = 0, size = 100): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/masters/headers/${headerId}/details?status=${status}&page=${page}&size=${size}`);
  }

  // Backend: POST /api/admin/masters/headers/{headerId}/details
  createMasterDetail(headerId: number, data: { displayName: string; parent?: { id: number } | null }): Observable<ApiResponse<MasterDetail>> {
    return this.http.post<ApiResponse<MasterDetail>>(`${this.baseUrl}/admin/masters/headers/${headerId}/details`, data);
  }

  updateMasterDetail(id: number, data: Partial<MasterDetail>): Observable<ApiResponse<MasterDetail>> {
    return this.http.put<ApiResponse<MasterDetail>>(`${this.baseUrl}/admin/masters/details/${id}`, data);
  }

  deleteMasterDetail(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/admin/masters/details/${id}`);
  }

  restoreMasterDetail(id: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/masters/details/${id}/restore`, {});
  }

  permanentDeleteMasterDetail(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/admin/masters/details/${id}/permanent`);
  }

  getDropdownOptions(dropdownName: string, parentId?: number): Observable<ApiResponse<any[]>> {
    let params = new HttpParams().set('dropdownName', dropdownName);
    if (parentId != null) {
      params = params.set('parentId', parentId.toString());
    }
    return this.http.get<ApiResponse<any[]>>(`${this.baseUrl}/auth/masters/values`, { params });
  }

  // Add auto-create master option
  autoCreateMasterOption(dropdownName: string, value: string, parentId?: number | null): Observable<ApiResponse<any>> {
    const payload: any = { dropdownName, value };
    if (parentId != null) {
      payload.parentId = parentId;
    }
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/auth/masters/auto-create`, payload);
  }

  // ─── SUBSCRIPTION PLANS & PAYMENTS API ──────────────────────────────────────

  getPlans(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.baseUrl}/subscription-plans`);
  }

  // --- Screen/Menu APIs ---
  getScreenAssignments(role: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/screen-assignments?role=${role}`);
  }

  saveScreenAssignments(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/screen-assignments`, payload);
  }

  // --- Dealer Management APIs ---
  getAllDealers(params: any): Observable<ApiResponse<any>> {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined) {
        httpParams = httpParams.append(key, params[key]);
      }
    });
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/all`, { params: httpParams });
  }

  verifyDealer(id: number, approve: boolean): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${id}/verify?approve=${approve}`, {});
  }

  updateDealerArea(id: number, payload: any): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${id}/area`, payload);
  }

  deleteDealerArea(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${id}/area`);
  }

  recoverDealerArea(id: number): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${id}/area/recover`, {});
  }

  permanentDeleteDealerArea(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${id}/area/permanent`);
  }

  getDealerDashboardMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/dealers/dashboard`);
  }

  getAdminDealerDashboardMetrics(dealerId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/dealers/admin/${dealerId}/dashboard`);
  }

  // --- Dealer Products APIs ---
  getAllDealerProducts(params: any): Observable<any> {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined) {
        httpParams = httpParams.append(key, params[key]);
      }
    });
    return this.http.get(`${this.baseUrl}/dealers/admin/products`, { params: httpParams });
  }

  createDealerProduct(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/dealers/admin/products`, payload);
  }

  updateDealerProduct(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/dealers/admin/products/${id}`, payload);
  }

  deleteDealerProduct(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/dealers/admin/products/${id}`);
  }

  recoverDealerProduct(id: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/dealers/admin/products/${id}/recover`, {});
  }

  permanentDeleteDealerProduct(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/dealers/admin/products/${id}/permanent`);
  }

  // --- Dealer Assignments APIs ---
  getAllDealerAssignments(params: any): Observable<any> {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined) {
        httpParams = httpParams.append(key, params[key]);
      }
    });
    return this.http.get(`${this.baseUrl}/dealers/admin/assignments`, { params: httpParams });
  }

  createDealerAssignment(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/dealers/admin/assignments`, payload);
  }

  assignDealerAreaBatch(payload: any[]): Observable<any> {
    return this.http.post(`${this.baseUrl}/dealers/admin/assignments/batch`, payload);
  }

  updateDealerAssignment(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/dealers/admin/assignments/${id}`, payload);
  }

  deleteDealerAssignment(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/dealers/admin/assignments/${id}`);
  }

  // --- Dealer Verifications APIs ---
  getAllDealerVerifications(params: any): Observable<any> {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined) {
        httpParams = httpParams.append(key, params[key]);
      }
    });
    return this.http.get(`${this.baseUrl}/dealers/admin/verifications`, { params: httpParams });
  }

  submitDistributionVerification(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/dealers/dealer/verifications`, payload);
  }

  verifyDistribution(id: number, status: string, remarks?: string): Observable<any> {
    let url = `${this.baseUrl}/dealers/admin/verifications/${id}/verify?status=${status}`;
    if (remarks) {
      url += `&remarks=${encodeURIComponent(remarks)}`;
    }
    return this.http.put(url, {});
  }

  createOrder(planId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/payments/create-order?planId=${planId}`, {});
  }

  submitPayment(payload: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/payments/submit`, payload);
  }

  getMyPayment(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/payments/my`);
  }

  getAllPayments(status = 'ALL', page = 0, size = 10): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.baseUrl}/admin/payments?status=${status}&page=${page}&size=${size}&sort=submittedAt,desc`
    );
  }

  approvePayment(paymentId: number, adminRemarks?: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/payments/${paymentId}/approve`, { adminRemarks });
  }

  rejectPayment(paymentId: number, adminRemarks?: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/payments/${paymentId}/reject`, { adminRemarks });
  }

  // ─── AUDIT LOGS API ─────────────────────────────────────────────────────────

  getAuditLogs(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.baseUrl}/logs`);
  }

  // ─── USER & UTILITY SERVICES ────────────────────────────────────────────────

  getReviewers(): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>(`${this.baseUrl}/users/reviewers`);
  }

  notifyAdminFacilityInterest(payload: { facilityName: string, userDetails: any }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/facilities/interest`, payload);
  }

  // ─── SCREEN PERMISSIONS API ──────────────────────────────────────────────────

  getAllRoles(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.baseUrl}/admin/screen-permissions/roles`);
  }

  getRolePermissions(roleId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/screen-permissions/roles/${roleId}`);
  }

  updateRoleScreens(roleId: number, screens: string[]): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/screen-permissions/roles/${roleId}`, screens);
  }

  getUserScreenPermissions(page = 0, size = 20, search?: string): Observable<ApiResponse<any>> {
    let url = `${this.baseUrl}/admin/screen-permissions/users?page=${page}&size=${size}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    return this.http.get<ApiResponse<any>>(url);
  }

  updateUserRoles(userId: number, roles: string[]): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/admin/screen-permissions/users/${userId}/roles`, roles);
  }

  generateReferralCode(): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/profile/generate-referral`, {});
  }

  // ─── FILE UPLOAD & MANAGEMENT API ──────────────────────────────────────────

  uploadFile(file: File): Observable<ApiResponse<any>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/upload`, formData);
  }

  getFile(id: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/upload/${id}`);
  }

  replaceFile(id: number, file: File): Observable<ApiResponse<any>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/upload/${id}`, formData);
  }

  deleteFile(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/upload/${id}`);
  }

  recoverFile(id: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/upload/${id}/recover`, {});
  }

  permanentlyDeleteFile(id: number): Observable<ApiResponse<any>> {
    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/upload/${id}/permanent`);
  }

  // --- Location Cascade APIs ---
  getStates(): Observable<any> {
    return this.http.get(`${this.baseUrl}/locations/states`);
  }

  getDistricts(state: string): Observable<any> {
    let params = new HttpParams().set('state', state);
    return this.http.get(`${this.baseUrl}/locations/districts`, { params });
  }

  getBlocks(district: string): Observable<any> {
    let params = new HttpParams().set('district', district);
    return this.http.get(`${this.baseUrl}/locations/blocks`, { params });
  }

  getPinCodes(block: string): Observable<any> {
    let params = new HttpParams().set('block', block);
    return this.http.get(`${this.baseUrl}/locations/pincodes`, { params });
  }

  getUsersByPinCode(pinCode: string): Observable<any> {
    let params = new HttpParams().set('pinCode', pinCode);
    return this.http.get(`${this.baseUrl}/locations/users`, { params });
  }
  // ─── EXPORT API ─────────────────────────────────────────────────────────────
  
  exportToExcel(module: string, filters: any): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/export/excel/${module}`, filters, { responseType: 'blob' });
  }

}

