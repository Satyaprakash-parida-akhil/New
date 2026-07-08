package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.request.RegisterRequest;
import com.example.documentmanagement.dto.request.UserUpdateRequest;
import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.UserResponse;
import com.example.documentmanagement.dto.response.DashboardStats;
import com.example.documentmanagement.dto.response.ReferralNode;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.UserService;
import com.example.documentmanagement.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import com.example.documentmanagement.util.MessageConstants;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users, referrals, and dashboard stats")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/reviewers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users with REVIEWER role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getReviewers() {
        List<UserResponse> reviewers = userService.getReviewers();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.REVIEWERS_RETRIEVED, reviewers));
    }

    private UserResponse mapToUserResponse(User user) {
        if (user == null)
            return null;
        boolean isPaid = "COMPLETED".equalsIgnoreCase(user.getPaymentStatus())
                || "PAID".equalsIgnoreCase(user.getPaymentStatus())
                || "APPROVED".equalsIgnoreCase(user.getPaymentStatus());
        boolean activeStatus = user.isActive() && isPaid;

        List<String> roleNames = new java.util.ArrayList<>();
        if (user.getRoles() != null) {
            for (com.example.documentmanagement.entity.Role r : user.getRoles()) {
                if (r != null && r.getName() != null) {
                    roleNames.add(r.getName());
                }
            }
        }

        String requestedRole = roleNames.isEmpty() ? "N/A" : roleNames.get(0);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(activeStatus)
                .isPaid(isPaid)
                .paymentStatus(user.getPaymentStatus())
                .registrationStatus(user.getRegistrationStatus())
                .requestedRole(requestedRole)
                .roles(roleNames)
                .referralCode(user.getReferralCode())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    @GetMapping
    @Operation(summary = "Get paginated user list with filters (Non-admins are restricted to downline)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String registrationStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String block,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String createdAt,
            Pageable pageable,
            Principal principal) {

        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        Page<User> users = userService.getPagedUsers(search, isActive, registrationStatus, paymentStatus, zone, country,
                state, district, block, town, village, createdAt, pageable, requester.getId(), isAdmin);
        Page<UserResponse> mappedUsers = users.map(this::mapToUserResponse);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USERS_RETRIEVED, mappedUsers));
    }

    @GetMapping("/filter-options")
    @Operation(summary = "Get distinct filter options for user fields")
    public ResponseEntity<ApiResponse<java.util.Map<String, java.util.List<String>>>> getUserFilterOptions() {
        java.util.Map<String, java.util.List<String>> options = userService.getUserFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILTER_OPTIONS_RETRIEVED, options));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information of a user")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id, Principal principal) {
        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (!isAdmin && !requester.getId().equals(id)) {
            // Check if user is in downline
            List<Long> downline = userService.getDownlineUserIds(requester.getId());
            if (downline == null || !downline.contains(id)) {
                throw new BusinessException(MessageConstants.Error.ACCESS_DENIED_DOWNLINE);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_DETAILS_RETRIEVED,
                mapToUserResponse(userService.getUserById(id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user directly (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_CREATED,
                mapToUserResponse(userService.createUser(request))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
            @RequestBody UserUpdateRequest request, Principal principal) {
        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (!isAdmin && !requester.getId().equals(id)) {
            throw new BusinessException(MessageConstants.Error.ACCESS_DENIED_OWN_DETAILS);
        }

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_UPDATED,
                mapToUserResponse(userService.updateUser(id, request))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_SOFT_DELETED));
    }

    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long id) {
        userService.blockUser(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_BLOCKED));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore/Recover soft deleted or blocked user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_RESTORED));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> permanentDeleteUser(@PathVariable Long id) {
        userService.permanentDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USER_PERMANENTLY_DELETED));
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user metrics counts for admin dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DASHBOARD_METRICS_RETRIEVED,
                userService.getDashboardStats()));
    }

    @GetMapping("/referral-tree")
    @Operation(summary = "Get referral tree root page for the logged-in user (shallow) or virtual root for admin")
    public ResponseEntity<ApiResponse<Page<ReferralNode>>> getMyReferralTree(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Principal principal) {
        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.REFERRAL_TREE_RETRIEVED,
                userService.getReferralTreePage(isAdmin ? 0L : requester.getId(), isAdmin, pageable)));
    }

    @GetMapping("/referral-tree/search")
    @Operation(summary = "Search within hierarchical referral tree by username, full name or referral code")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchReferralTree(
            @RequestParam String searchTerm,
            Principal principal) {

        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.SEARCH_COMPLETED,
                userService.searchReferralTree(requester.getId(), searchTerm, isAdmin)));
    }

    @GetMapping("/referral-tree/{targetId}")
    @Operation(summary = "Get full referral tree for a specific user")
    public ResponseEntity<ApiResponse<ReferralNode>> getReferralTree(
            @PathVariable Long targetId,
            Principal principal) {

        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if (targetId == 0L) {
                throw new BusinessException(MessageConstants.Error.UNAUTHORIZED_ACCESS);
            }
            if (!requester.getId().equals(targetId)) {
                List<Long> downlineIds = userService.getDownlineUserIds(requester.getId());
                if (!downlineIds.contains(targetId)) {
                    throw new BusinessException(MessageConstants.Error.ACCESS_DENIED_DOWNLINE);
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.REFERRAL_TREE_RETRIEVED,
                userService.getReferralTree(targetId, isAdmin && targetId == 0)));
    }

    @GetMapping("/referral-children/{targetId}")
    @Operation(summary = "Get direct children of a specific user in the referral tree")
    public ResponseEntity<ApiResponse<List<ReferralNode>>> getReferralChildren(
            @PathVariable Long targetId,
            Principal principal) {

        User requester = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.REFERRAL_TREE_RETRIEVED,
                userService.getDirectChildren(targetId, requester.getId(), isAdmin)));
    }
}
