package com.example.documentmanagement.service;

import com.example.documentmanagement.dto.request.DealerProfileRequest;
import com.example.documentmanagement.dto.response.DealerProfileResponse;
import org.springframework.data.domain.Page;

public interface DealerService {
    DealerProfileResponse submitVerification(String username, DealerProfileRequest request);
    DealerProfileResponse getMyProfile(String username);
    Page<DealerProfileResponse> getAllDealers(String status, String searchTerm, int page, int size, String sortBy, String sortDir);
    DealerProfileResponse verifyDealer(Long id, boolean approve);
    com.example.documentmanagement.dto.response.DealerProfileResponse updateDealerArea(Long id, com.example.documentmanagement.dto.request.DealerAreaAssignmentRequest request);
    com.example.documentmanagement.dto.response.DealerProfileResponse deleteDealerArea(Long id);
    com.example.documentmanagement.dto.response.DealerProfileResponse recoverDealerArea(Long id);
    com.example.documentmanagement.dto.response.DealerProfileResponse permanentDeleteDealerArea(Long id);

    // Products
    com.example.documentmanagement.dto.response.DealerProductResponse createProduct(com.example.documentmanagement.dto.request.DealerProductRequest request);
    Page<com.example.documentmanagement.dto.response.DealerProductResponse> getAllProducts(String searchTerm, int page, int size, String sortBy, String sortDir);
    com.example.documentmanagement.dto.response.DealerProductResponse updateProduct(Long id, com.example.documentmanagement.dto.request.DealerProductRequest request);
    void deleteProduct(Long id);
    com.example.documentmanagement.dto.response.DealerProductResponse recoverProduct(Long id);
    void permanentDeleteProduct(Long id);
    // Assignments
    java.util.List<com.example.documentmanagement.dto.response.DistributionAssignmentResponse> assignArea(com.example.documentmanagement.dto.request.DistributionAssignmentRequest request);
    java.util.List<com.example.documentmanagement.dto.response.DistributionAssignmentResponse> assignAreaBatch(java.util.List<com.example.documentmanagement.dto.request.DistributionAssignmentRequest> requests);
    Page<com.example.documentmanagement.dto.response.DistributionAssignmentResponse> getAllAssignments(String searchTerm, String dealerName, String area, String productName, String status, int page, int size, String sortBy, String sortDir);
    com.example.documentmanagement.dto.response.DistributionAssignmentResponse updateAssignment(Long id, com.example.documentmanagement.dto.request.DistributionAssignmentRequest request);
    void deleteAssignment(Long id);

    // Verifications (Distribution)
    com.example.documentmanagement.dto.response.DistributionVerificationResponse submitVerification(com.example.documentmanagement.dto.request.DistributionVerificationRequest request);
    Page<com.example.documentmanagement.dto.response.DistributionVerificationResponse> getAllVerifications(String searchTerm, int page, int size, String sortBy, String sortDir);
    com.example.documentmanagement.dto.response.DistributionVerificationResponse verifyDistribution(Long id, String status, String remarks);
    // Dashboard Metrics
    com.example.documentmanagement.dto.response.DealerDashboardResponse getDashboardMetrics(String username);
    com.example.documentmanagement.dto.response.DealerDashboardResponse getDashboardMetricsByDealerId(Long dealerId);
}
