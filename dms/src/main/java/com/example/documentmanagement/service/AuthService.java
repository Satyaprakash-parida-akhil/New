package com.example.documentmanagement.service;

import com.example.documentmanagement.dto.request.LoginRequest;
import com.example.documentmanagement.dto.response.TokenResponse;
import com.example.documentmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {
    TokenResponse authenticateAndGenerateToken(LoginRequest loginRequest);

    void register(com.example.documentmanagement.dto.request.RegisterRequest registerRequest);

    void sendRegistrationOtp(String email);

    void verifyRegistrationOtp(String email, String code);

    void forgotPassword(com.example.documentmanagement.dto.request.ForgotPasswordRequest request);

    void verifyForgotPasswordOtp(String email, String code);

    void resetPassword(com.example.documentmanagement.dto.request.ResetPasswordRequest request);

    Page<User> getPendingRegistrationRequests(Pageable pageable);

    Page<User> getInactiveRegistrationRequests(Pageable pageable);

    Page<User> getApprovedUsers(Pageable pageable);

    void approveRegistration(Long userId, String finalRole);

    void rejectRegistration(Long userId);

    void softDeleteRegistration(Long userId);

    void restoreRegistration(Long userId);
}
