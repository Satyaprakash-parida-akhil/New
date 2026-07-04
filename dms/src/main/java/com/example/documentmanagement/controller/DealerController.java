package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.request.DealerProfileRequest;
import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.DealerProfileResponse;
import com.example.documentmanagement.dto.request.DealerProductRequest;
import com.example.documentmanagement.dto.response.DealerProductResponse;
import com.example.documentmanagement.dto.request.DistributionAssignmentRequest;
import com.example.documentmanagement.dto.response.DistributionAssignmentResponse;
import com.example.documentmanagement.dto.request.DistributionVerificationRequest;
import com.example.documentmanagement.dto.response.DistributionVerificationResponse;
import com.example.documentmanagement.service.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.example.documentmanagement.util.MessageConstants;

@RestController
@RequestMapping("/api/dealers")
@Tag(name = "Dealer Module", description = "Endpoints for Dealer Management")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;

    @PostMapping("/submit-verification")
    @Operation(summary = "Submit dealer KYC for verification")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> submitVerification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DealerProfileRequest request) {
        DealerProfileResponse response = dealerService.submitVerification(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_VERIFICATION_SUBMITTED, response));
    }

    @GetMapping("/my-profile")
    @Operation(summary = "Get the logged in user's dealer profile")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        DealerProfileResponse response = dealerService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.PROFILE_LOADED, response));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Get all dealers for admin verification (Tab 1)")
    public ResponseEntity<ApiResponse<Page<DealerProfileResponse>>> getAllDealers(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<DealerProfileResponse> response = dealerService.getAllDealers(status, searchTerm, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALERS_FETCHED, response));
    }

    @PutMapping("/admin/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Approve or reject a dealer application")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> verifyDealer(
            @PathVariable Long id, 
            @RequestParam boolean approve) {
        DealerProfileResponse response = dealerService.verifyDealer(id, approve);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_VERIFICATION_UPDATED, response));
    }

    @PutMapping("/admin/{id}/area")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Assign or update dealer area mapping")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> updateDealerArea(
            @PathVariable Long id,
            @Valid @RequestBody com.example.documentmanagement.dto.request.DealerAreaAssignmentRequest request) {
        DealerProfileResponse response = dealerService.updateDealerArea(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_AREA_UPDATED, response));
    }

    @DeleteMapping("/admin/{id}/area")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete dealer area mapping")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> deleteDealerArea(@PathVariable Long id) {
        DealerProfileResponse response = dealerService.deleteDealerArea(id);
        return ResponseEntity.ok(ApiResponse.success("Area assignment soft deleted successfully", response));
    }

    @PutMapping("/admin/{id}/area/recover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recover dealer area mapping")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> recoverDealerArea(@PathVariable Long id) {
        DealerProfileResponse response = dealerService.recoverDealerArea(id);
        return ResponseEntity.ok(ApiResponse.success("Area assignment recovered successfully", response));
    }

    @DeleteMapping("/admin/{id}/area/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently clear/delete dealer area mapping")
    public ResponseEntity<ApiResponse<DealerProfileResponse>> permanentDeleteDealerArea(@PathVariable Long id) {
        DealerProfileResponse response = dealerService.permanentDeleteDealerArea(id);
        return ResponseEntity.ok(ApiResponse.success("Area assignment cleared permanently", response));
    }

    // --- Dealer Products ---
    
    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new dealer product")
    public ResponseEntity<ApiResponse<DealerProductResponse>> createProduct(
            @Valid @RequestBody DealerProductRequest request) {
        DealerProductResponse response = dealerService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_PRODUCT_CREATED, response));
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Get all dealer products")
    public ResponseEntity<ApiResponse<Page<DealerProductResponse>>> getAllProducts(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<DealerProductResponse> response = dealerService.getAllProducts(searchTerm, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_PRODUCTS_FETCHED, response));
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a dealer product")
    public ResponseEntity<ApiResponse<DealerProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody DealerProductRequest request) {
        DealerProductResponse response = dealerService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_PRODUCT_UPDATED, response));
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a dealer product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        dealerService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_PRODUCT_DELETED, null));
    }

    @PutMapping("/admin/products/{id}/recover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recover a soft-deleted dealer product")
    public ResponseEntity<ApiResponse<DealerProductResponse>> recoverProduct(@PathVariable Long id) {
        DealerProductResponse response = dealerService.recoverProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product recovered successfully", response));
    }

    @DeleteMapping("/admin/products/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a dealer product")
    public ResponseEntity<ApiResponse<Void>> permanentDeleteProduct(@PathVariable Long id) {
        dealerService.permanentDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product permanently deleted", null));
    }

    // --- Dealer Assignments ---
    
    @PostMapping("/admin/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Assign area to dealer")
    public ResponseEntity<ApiResponse<java.util.List<DistributionAssignmentResponse>>> assignArea(
            @Valid @RequestBody DistributionAssignmentRequest request) {
        java.util.List<DistributionAssignmentResponse> response = dealerService.assignArea(request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_AREA_ASSIGNED, response));
    }

    @PostMapping("/admin/assignments/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch assign areas to dealers")
    public ResponseEntity<ApiResponse<java.util.List<DistributionAssignmentResponse>>> assignAreaBatch(
            @Valid @RequestBody java.util.List<DistributionAssignmentRequest> requests) {
        java.util.List<DistributionAssignmentResponse> responses = dealerService.assignAreaBatch(requests);
        return ResponseEntity.ok(ApiResponse.success("Batch assignments completed successfully", responses));
    }

    @GetMapping("/admin/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all distribution assignments (Admin)")
    public ResponseEntity<ApiResponse<Page<com.example.documentmanagement.dto.response.DistributionAssignmentResponse>>> getAllAssignments(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String dealerName,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<com.example.documentmanagement.dto.response.DistributionAssignmentResponse> responses = dealerService.getAllAssignments(
                searchTerm, dealerName, area, productName, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched successfully", responses));
    }

    @PutMapping("/admin/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Update an assignment")
    public ResponseEntity<ApiResponse<DistributionAssignmentResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody DistributionAssignmentRequest request) {
        DistributionAssignmentResponse response = dealerService.updateAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_ASSIGNMENT_UPDATED, response));
    }

    @DeleteMapping("/admin/assignments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Delete an assignment")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        dealerService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_ASSIGNMENT_DELETED, null));
    }

    // --- Dealer Distribution Verifications ---
    
    @PostMapping("/dealer/verifications")
    @PreAuthorize("hasRole('DEALER') or hasRole('ADMIN')")
    @Operation(summary = "Submit a distribution verification")
    public ResponseEntity<ApiResponse<DistributionVerificationResponse>> submitVerification(
            @Valid @RequestBody DistributionVerificationRequest request) {
        DistributionVerificationResponse response = dealerService.submitVerification(request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_DISTRIBUTION_SUBMITTED, response));
    }

    @GetMapping("/admin/verifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Get all verifications")
    public ResponseEntity<ApiResponse<Page<DistributionVerificationResponse>>> getAllVerifications(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<DistributionVerificationResponse> response = dealerService.getAllVerifications(searchTerm, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_DISTRIBUTIONS_FETCHED, response));
    }

    @PutMapping("/admin/verifications/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    @Operation(summary = "Approve or reject a distribution verification")
    public ResponseEntity<ApiResponse<DistributionVerificationResponse>> verifyDistribution(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remarks) {
        DistributionVerificationResponse response = dealerService.verifyDistribution(id, status, remarks);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DEALER_DISTRIBUTION_UPDATED, response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DEALER')")
    @Operation(summary = "Get dealer performance dashboard metrics")
    public ResponseEntity<ApiResponse<com.example.documentmanagement.dto.response.DealerDashboardResponse>> getDashboardMetrics(
            @AuthenticationPrincipal UserDetails userDetails) {
        com.example.documentmanagement.dto.response.DealerDashboardResponse response = dealerService.getDashboardMetrics(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics fetched successfully", response));
    }

    @GetMapping("/admin/{dealerId}/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a specific dealer's dashboard metrics for Admin")
    public ResponseEntity<ApiResponse<com.example.documentmanagement.dto.response.DealerDashboardResponse>> getAdminDealerDashboardMetrics(
            @PathVariable Long dealerId) {
        com.example.documentmanagement.dto.response.DealerDashboardResponse response = dealerService.getDashboardMetricsByDealerId(dealerId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics fetched successfully", response));
    }
}
