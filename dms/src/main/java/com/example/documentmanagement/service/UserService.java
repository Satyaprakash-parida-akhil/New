package com.example.documentmanagement.service;

import com.example.documentmanagement.dto.request.RegisterRequest;
import com.example.documentmanagement.dto.request.UserUpdateRequest;
import com.example.documentmanagement.dto.response.UserResponse;
import com.example.documentmanagement.dto.response.DashboardStats;
import com.example.documentmanagement.dto.response.ReferralNode;
import com.example.documentmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    List<UserResponse> getReviewers();

    User createUser(RegisterRequest request);
    User getUserById(Long id);
    User updateUser(Long id, UserUpdateRequest request);
    void softDeleteUser(Long id);
    void blockUser(Long id);
    void restoreUser(Long id);
    void permanentDeleteUser(Long id);

    DashboardStats getDashboardStats();
    ReferralNode getReferralTree(Long userId, boolean isAdminView);
    Page<ReferralNode> getReferralTreePage(Long userId, boolean isAdminView, Pageable pageable);
    List<ReferralNode> getDirectChildren(Long targetId, Long requesterId, boolean isAdmin);
    List<UserResponse> searchReferralTree(Long requesterId, String searchTerm, boolean isAdmin);
    Page<User> getPagedUsers(String search, Boolean isActive, String registrationStatus, String paymentStatus, String zone, String country, String state, String district, String block, String town, String village, String createdAt, Pageable pageable, Long requesterId, boolean isAdmin);
    java.util.Map<String, java.util.List<String>> getUserFilterOptions();
    List<Long> getDownlineUserIds(Long userId);
    List<String> getDistinctStates();
    List<String> getDistinctDistrictsByState(String state);
    List<String> getDistinctBlocksByDistrict(String district);
    List<String> getDistinctPinCodesByBlock(String block);
    List<User> getUsersByPinCode(String pinCode);
}
