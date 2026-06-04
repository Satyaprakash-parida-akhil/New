package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/registrations")
@Tag(name = "Admin Registration Management", description = "Endpoints for admin to manage user registration requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistrationController {

    private final AuthService authService;

    public AdminRegistrationController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending registration requests")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<User>>> getPendingRegistrations(
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity
                .ok(ApiResponse.success("Pending registrations fetched",
                        authService.getPendingRegistrationRequests(pageable)));
    }

    @GetMapping("/inactive")
    @Operation(summary = "Get all inactive (rejected/deleted) registration requests")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<User>>> getInactiveRegistrations(
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Inactive registrations fetched",
                        authService.getInactiveRegistrationRequests(pageable)));
    }

    @GetMapping("/approved")
    @Operation(summary = "Get all approved users")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<User>>> getApprovedUsers(
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Approved users fetched",
                        authService.getApprovedUsers(pageable)));
    }

    @PutMapping("/approve/{userId}")
    @Operation(summary = "Approve a user registration and assign a role")
    public ResponseEntity<ApiResponse<Void>> approveRegistration(@PathVariable Long userId, @RequestParam String role) {
        authService.approveRegistration(userId, role);
        return ResponseEntity.ok(ApiResponse.success("User approved successfully"));
    }

    @PutMapping("/reject/{userId}")
    @Operation(summary = "Reject a user registration")
    public ResponseEntity<ApiResponse<Void>> rejectRegistration(@PathVariable Long userId) {
        authService.rejectRegistration(userId);
        return ResponseEntity.ok(ApiResponse.success("User rejected successfully"));
    }

    @PutMapping("/soft-delete/{userId}")
    @Operation(summary = "Soft delete (archive) a registration request")
    public ResponseEntity<ApiResponse<Void>> softDeleteRegistration(@PathVariable Long userId) {
        authService.softDeleteRegistration(userId);
        return ResponseEntity.ok(ApiResponse.success("Request archived"));
    }

    @PutMapping("/restore/{userId}")
    @Operation(summary = "Restore a registration request to pending")
    public ResponseEntity<ApiResponse<Void>> restoreRegistration(@PathVariable Long userId) {
        authService.restoreRegistration(userId);
        return ResponseEntity.ok(ApiResponse.success("Request restored to pending"));
    }
}
