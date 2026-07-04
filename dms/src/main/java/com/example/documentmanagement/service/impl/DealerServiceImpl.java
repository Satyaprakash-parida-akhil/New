package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.request.DealerProfileRequest;
import com.example.documentmanagement.dto.response.DealerProfileResponse;
import com.example.documentmanagement.entity.DealerProfile;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.DealerProfileRepository;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.dto.request.DealerProductRequest;
import com.example.documentmanagement.dto.response.DealerProductResponse;
import com.example.documentmanagement.entity.DealerProduct;
import com.example.documentmanagement.repository.DealerProductRepository;
import com.example.documentmanagement.dto.request.DistributionAssignmentRequest;
import com.example.documentmanagement.dto.response.DistributionAssignmentResponse;
import com.example.documentmanagement.entity.DistributionAssignment;
import com.example.documentmanagement.repository.DistributionAssignmentRepository;
import com.example.documentmanagement.dto.request.DistributionVerificationRequest;
import com.example.documentmanagement.dto.response.DistributionVerificationResponse;
import com.example.documentmanagement.entity.DistributionVerification;
import com.example.documentmanagement.repository.DistributionVerificationRepository;
import com.example.documentmanagement.dto.request.DealerAreaAssignmentRequest;
import com.example.documentmanagement.service.DealerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.documentmanagement.exception.ResourceNotFoundException;
import com.example.documentmanagement.util.MessageConstants;

@Service
@RequiredArgsConstructor
public class DealerServiceImpl implements DealerService {

    private final DealerProfileRepository dealerProfileRepository;
    private final DealerProductRepository dealerProductRepository;
    private final DistributionAssignmentRepository distributionAssignmentRepository;
    private final DistributionVerificationRepository distributionVerificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DealerProfileResponse submitVerification(String username, DealerProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.USER_NOT_FOUND));

        DealerProfile profile = dealerProfileRepository.findByUser(user).orElse(new DealerProfile());
        
        profile.setUser(user);
        profile.setPhotoUrl(request.getPhotoUrl());
        profile.setAadhaarNumber(request.getAadhaarNumber());
        profile.setAadhaarUrl(request.getAadhaarUrl());
        profile.setPanNumber(request.getPanNumber());
        profile.setPanUrl(request.getPanUrl());
        profile.setArea(request.getArea());
        profile.setState(request.getState());
        profile.setDistrict(request.getDistrict());
        profile.setPinCode(request.getPinCode());
        profile.setAddress(request.getAddress());
        profile.setVerificationStatus("PENDING");
        profile.setVerified(false);
        profile.setActive(true);
        profile.setDeleted(false);

        profile = dealerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Override
    public DealerProfileResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.USER_NOT_FOUND));

        return dealerProfileRepository.findByUser(user)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
    }

    @Override
    public Page<DealerProfileResponse> getAllDealers(String status, String searchTerm, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        
        if (status == null || status.isEmpty()) {
            status = "ALL";
        }
        if (searchTerm == null) {
            searchTerm = "";
        }
        
        return dealerProfileRepository.searchProfiles(status, searchTerm, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public DealerProfileResponse verifyDealer(Long id, boolean approve) {
        DealerProfile profile = dealerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        profile.setVerified(approve);
        profile.setVerificationStatus(approve ? "VERIFIED" : "REJECTED");
        return mapToResponse(dealerProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public DealerProfileResponse updateDealerArea(Long id, DealerAreaAssignmentRequest request) {
        DealerProfile profile = dealerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        profile.setState(request.getState());
        profile.setDistrict(request.getDistrict());
        profile.setArea(request.getArea());
        profile.setPinCode(request.getPinCode());
        return mapToResponse(dealerProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public DealerProfileResponse deleteDealerArea(Long id) {
        DealerProfile profile = dealerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        profile.setActive(false);
        return mapToResponse(dealerProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public DealerProfileResponse recoverDealerArea(Long id) {
        DealerProfile profile = dealerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        profile.setActive(true);
        return mapToResponse(dealerProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public DealerProfileResponse permanentDeleteDealerArea(Long id) {
        DealerProfile profile = dealerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        profile.setState(null);
        profile.setDistrict(null);
        profile.setArea(null);
        profile.setPinCode(null);
        profile.setActive(true);
        return mapToResponse(dealerProfileRepository.save(profile));
    }

    private DealerProfileResponse mapToResponse(DealerProfile profile) {
        return DealerProfileResponse.builder()
                .id(profile.getId())
                .username(profile.getUser().getUsername())
                .email(profile.getUser().getEmail())
                .photoUrl(profile.getPhotoUrl())
                .aadhaarNumber(profile.getAadhaarNumber())
                .panNumber(profile.getPanNumber())
                .area(profile.getArea() != null ? profile.getArea() : profile.getUser().getBlock())
                .state(profile.getState() != null ? profile.getState() : profile.getUser().getState())
                .district(profile.getDistrict() != null ? profile.getDistrict() : profile.getUser().getDistrict())
                .pinCode(profile.getPinCode() != null ? profile.getPinCode() : profile.getUser().getPinCode())
                .address(profile.getAddress() != null ? profile.getAddress() : profile.getUser().getAddress())
                .verified(profile.isVerified())
                .verificationStatus(profile.getVerificationStatus())
                .active(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public DealerProductResponse createProduct(DealerProductRequest request) {
        DealerProduct product = DealerProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .active(true)
                .deleted(false)
                .build();
        return mapToProductResponse(dealerProductRepository.save(product));
    }

    @Override
    public Page<DealerProductResponse> getAllProducts(String searchTerm, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        return dealerProductRepository.findAll(pageable).map(this::mapToProductResponse);
    }

    @Override
    @Transactional
    public DealerProductResponse updateProduct(Long id, DealerProductRequest request) {
        DealerProduct product = dealerProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        return mapToProductResponse(dealerProductRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        DealerProduct product = dealerProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
        product.setDeleted(true);
        product.setActive(false);
        dealerProductRepository.save(product);
    }

    @Override
    @Transactional
    public DealerProductResponse recoverProduct(Long id) {
        DealerProduct product = dealerProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
        product.setDeleted(false);
        product.setActive(true);
        return mapToProductResponse(dealerProductRepository.save(product));
    }

    @Override
    @Transactional
    public void permanentDeleteProduct(Long id) {
        DealerProduct product = dealerProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
        dealerProductRepository.delete(product);
    }

    private DealerProductResponse mapToProductResponse(DealerProduct product) {
        return DealerProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .status(product.isActive() ? "ACTIVE" : "INACTIVE")
                .createdAt(product.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public java.util.List<DistributionAssignmentResponse> assignArea(DistributionAssignmentRequest request) {
        DealerProfile dealer = dealerProfileRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        
        java.util.List<DistributionAssignmentResponse> responses = new java.util.ArrayList<>();
        
        for (Long productId : request.getProductIds()) {
            DealerProduct product = dealerProductRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
            
            DistributionAssignment assignment = DistributionAssignment.builder()
                    .dealer(dealer)
                    .product(product)
                    .targetQuantity(request.getTargetQuantity())
                    .attachmentUrl(request.getAttachmentUrl())
                    .state(request.getState())
                    .district(request.getDistrict())
                    .block(request.getBlock())
                    .pinCode(request.getPinCode())
                    .active(true)
                    .build();
                    
            if (request.getAssignedUserIds() != null && !request.getAssignedUserIds().isEmpty()) {
                java.util.List<User> users = userRepository.findAllById(request.getAssignedUserIds());
                assignment.setAssignedUsers(users);
            }
            
            responses.add(mapToAssignmentResponse(distributionAssignmentRepository.save(assignment)));
        }
        
        return responses;
    }

    @Override
    @Transactional
    public java.util.List<DistributionAssignmentResponse> assignAreaBatch(java.util.List<com.example.documentmanagement.dto.request.DistributionAssignmentRequest> requests) {
        java.util.List<DistributionAssignmentResponse> responses = new java.util.ArrayList<>();
        for (com.example.documentmanagement.dto.request.DistributionAssignmentRequest request : requests) {
            responses.addAll(assignArea(request));
        }
        return responses;
    }

    @Override
    public Page<DistributionAssignmentResponse> getAllAssignments(String searchTerm, String dealerName, String area, String productName, String status, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        
        searchTerm = searchTerm == null ? "" : searchTerm;
        dealerName = dealerName == null ? "" : dealerName;
        area = area == null ? "" : area;
        productName = productName == null ? "" : productName;
        status = status == null ? "" : status;

        return distributionAssignmentRepository.searchAssignments(searchTerm, dealerName, area, productName, status, pageable).map(this::mapToAssignmentResponse);
    }

    @Override
    @Transactional
    public DistributionAssignmentResponse updateAssignment(Long id, DistributionAssignmentRequest request) {
        DistributionAssignment assignment = distributionAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_ASSIGNMENT_NOT_FOUND));
        DealerProfile dealer = dealerProfileRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        
        Long productId = (request.getProductIds() != null && !request.getProductIds().isEmpty()) ? request.getProductIds().get(0) : null;
        if (productId == null) {
            throw new ResourceNotFoundException("Product ID is required");
        }
        
        DealerProduct product = dealerProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_PRODUCT_NOT_FOUND));
        
        assignment.setDealer(dealer);
        assignment.setProduct(product);
        assignment.setTargetQuantity(request.getTargetQuantity());
        assignment.setAttachmentUrl(request.getAttachmentUrl());
        assignment.setState(request.getState());
        assignment.setDistrict(request.getDistrict());
        assignment.setBlock(request.getBlock());
        assignment.setPinCode(request.getPinCode());
        
        if (request.getAssignedUserIds() != null) {
            java.util.List<User> users = userRepository.findAllById(request.getAssignedUserIds());
            assignment.setAssignedUsers(users);
        }
        
        return mapToAssignmentResponse(distributionAssignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id) {
        DistributionAssignment assignment = distributionAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_ASSIGNMENT_NOT_FOUND));
        assignment.setActive(false);
        distributionAssignmentRepository.save(assignment);
    }

    private DistributionAssignmentResponse mapToAssignmentResponse(DistributionAssignment assignment) {
        DistributionAssignmentResponse response = DistributionAssignmentResponse.builder()
                .id(assignment.getId())
                .dealerId(assignment.getDealer().getId())
                .dealerUsername(assignment.getDealer().getUser().getUsername())
                .productId(assignment.getProduct().getId())
                .productName(assignment.getProduct().getName())
                .targetQuantity(assignment.getTargetQuantity())
                .attachmentUrl(assignment.getAttachmentUrl())
                .active(assignment.isActive())
                .createdAt(assignment.getCreatedAt())
                .state(assignment.getState())
                .district(assignment.getDistrict())
                .block(assignment.getBlock())
                .pinCode(assignment.getPinCode())
                .build();
                
        if (assignment.getAssignedUsers() != null) {
            java.util.List<DistributionAssignmentResponse.AssignedUserResponse> userResponses = assignment.getAssignedUsers().stream()
                .map(u -> DistributionAssignmentResponse.AssignedUserResponse.builder()
                    .id(u.getId())
                    .fullName(u.getFullName())
                    .phoneNumber(u.getPhoneNumber())
                    .build())
                .collect(java.util.stream.Collectors.toList());
            response.setAssignedUsers(userResponses);
        }
        
        return response;
    }

    @Override
    @Transactional
    public DistributionVerificationResponse submitVerification(DistributionVerificationRequest request) {
        DistributionAssignment assignment = distributionAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_ASSIGNMENT_NOT_FOUND));
        DistributionVerification verification = DistributionVerification.builder()
                .assignment(assignment)
                .customerName(request.getCustomerName())
                .details(request.getDetails())
                .photoProofUrl(request.getPhotoProofUrl())
                .distributionDate(java.time.LocalDateTime.now())
                .verificationStatus("PENDING")
                .build();
        return mapToVerificationResponse(distributionVerificationRepository.save(verification));
    }

    @Override
    public Page<DistributionVerificationResponse> getAllVerifications(String searchTerm, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        return distributionVerificationRepository.findAll(pageable).map(this::mapToVerificationResponse);
    }

    @Override
    @Transactional
    public DistributionVerificationResponse verifyDistribution(Long id, String status, String remarks) {
        DistributionVerification verification = distributionVerificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_DISTRIBUTION_NOT_FOUND));
        verification.setVerificationStatus(status);
        if (remarks != null && !remarks.isEmpty()) {
            verification.setDetails(remarks);
        }
        return mapToVerificationResponse(distributionVerificationRepository.save(verification));
    }

    private DistributionVerificationResponse mapToVerificationResponse(DistributionVerification verification) {
        return DistributionVerificationResponse.builder()
                .id(verification.getId())
                .assignmentId(verification.getAssignment().getId())
                .dealerUsername(verification.getAssignment().getDealer().getUser().getUsername())
                .productName(verification.getAssignment().getProduct().getName())
                .targetQuantity(verification.getAssignment().getTargetQuantity())
                .customerName(verification.getCustomerName())
                .details(verification.getDetails())
                .photoProofUrl(verification.getPhotoProofUrl())
                .distributionDate(verification.getDistributionDate())
                .verificationStatus(verification.getVerificationStatus())
                .createdAt(verification.getCreatedAt())
                .build();
    }

    @Override
    public com.example.documentmanagement.dto.response.DealerDashboardResponse getDashboardMetrics(String username) {
        long totalDistributions = distributionVerificationRepository.countByAssignment_Dealer_User_Username(username);
        long verifiedDistributions = distributionVerificationRepository.countByAssignment_Dealer_User_UsernameAndVerificationStatus(username, "VERIFIED");
        long pendingDistributions = distributionVerificationRepository.countByAssignment_Dealer_User_UsernameAndVerificationStatus(username, "PENDING");
        long rejectedDistributions = distributionVerificationRepository.countByAssignment_Dealer_User_UsernameAndVerificationStatus(username, "REJECTED");
        
        long totalAssigned = distributionAssignmentRepository.countByDealer_User_Username(username);
        
        double successRate = 0.0;
        if (totalDistributions > 0) {
            successRate = ((double) verifiedDistributions / totalDistributions) * 100;
        }

        return com.example.documentmanagement.dto.response.DealerDashboardResponse.builder()
                .totalDistributions(totalDistributions)
                .verifiedDistributions(verifiedDistributions)
                .pendingDistributions(pendingDistributions)
                .rejectedDistributions(rejectedDistributions)
                .successRate(successRate)
                .totalProductsAssigned(totalAssigned)
                .build();
    }

    @Override
    public com.example.documentmanagement.dto.response.DealerDashboardResponse getDashboardMetricsByDealerId(Long dealerId) {
        DealerProfile profile = dealerProfileRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.Error.DEALER_NOT_FOUND));
        return getDashboardMetrics(profile.getUser().getUsername());
    }
}
