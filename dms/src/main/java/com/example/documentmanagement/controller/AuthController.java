package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.request.LoginRequest;
import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.TokenResponse;
import com.example.documentmanagement.service.AuthService;
import com.example.documentmanagement.util.MessageConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final AuthService authService;
    private final MessageSource messageSource;

    public AuthController(AuthService authService, MessageSource messageSource) {
        this.authService = authService;
        this.messageSource = messageSource;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT Token")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.authenticateAndGenerateToken(request);
        String successMessage = messageSource.getMessage(MessageConstants.LOGIN_SUCCESS, null, "Login successful",
                LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage, tokenResponse));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody com.example.documentmanagement.dto.request.RegisterRequest request) {
        authService.register(request);
        String successMessage = messageSource.getMessage("msg.registration.success", null,
                "Registration successful. Please wait for admin approval.",
                LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage));
    }

    @PostMapping("/register/otp")
    public ResponseEntity<ApiResponse<Void>> sendRegisterOtp(@RequestParam String email) {
        authService.sendRegistrationOtp(email);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to " + email));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<Void>> verifyRegisterOtp(@RequestParam String email, @RequestParam String otp) {
        authService.verifyRegistrationOtp(email, otp);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody com.example.documentmanagement.dto.request.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent to your email"));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(@RequestParam String email,
            @RequestParam String otp) {
        authService.verifyForgotPasswordOtp(email, otp);
        return ResponseEntity.ok(ApiResponse.success("OTP verified. You can now reset your password."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody com.example.documentmanagement.dto.request.ResetPasswordRequest request) {
        authService.resetPassword(request);
        String successMessage = messageSource.getMessage("msg.password.reset.success", null,
                "Password reset successful", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage));
    }

}
