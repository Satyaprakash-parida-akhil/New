package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.request.LoginRequest;
import com.example.documentmanagement.dto.response.TokenResponse;
import com.example.documentmanagement.security.JwtUtil;
import com.example.documentmanagement.service.AuthService;
import com.example.documentmanagement.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final Random RANDOM = new Random();
    private static final String TYPE_REGISTRATION = "REGISTRATION";
    private static final String TYPE_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    private static final String MSG_USER_NOT_FOUND = "User not found";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final com.example.documentmanagement.repository.UserRepository userRepository;
    private final com.example.documentmanagement.repository.RoleRepository roleRepository;
    private final com.example.documentmanagement.repository.OtpRepository otpRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.example.documentmanagement.service.EmailService emailService;

    @Override
    public TokenResponse authenticateAndGenerateToken(LoginRequest loginRequest) {
        log.info("AUTHENTICATION START: Attempting login for user: {}", loginRequest.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.info("AUTHENTICATION SUCCESS: User {} verified. Generating token.", userDetails.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return TokenResponse.builder()
                .accessToken(token)
                .username(userDetails.getUsername())
                .build();
    }

    @Override
    @Transactional
    public void sendRegistrationOtp(String email) {
        log.info("OTP REQUEST: Email: {}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("OTP FAILURE: Email {} is already in use", email);
            throw new BusinessException("Email is already in use");
        }

        String code = String.format("%06d", RANDOM.nextInt(999999));
        com.example.documentmanagement.entity.Otp otp = com.example.documentmanagement.entity.Otp.builder()
                .email(email)
                .code(code)
                .type(TYPE_REGISTRATION)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(10))
                .build();

        otpRepository.deleteByEmailAndType(email, TYPE_REGISTRATION);
        otpRepository.save(otp);
        emailService.sendOtp(email, code);
    }

    @Override
    public void verifyRegistrationOtp(String email, String code) {
        log.info("OTP VERIFICATION: Email: {}, Code: XXXX", email);
        com.example.documentmanagement.entity.Otp otp = otpRepository.findByEmailAndType(email, TYPE_REGISTRATION)
                .orElseThrow(() -> new BusinessException("Valid OTP not found for this email"));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new BusinessException("OTP has expired");
        }

        if (!otp.getCode().equals(code)) {
            throw new BusinessException("Invalid OTP code");
        }
    }

    @Override
    @Transactional
    public void register(com.example.documentmanagement.dto.request.RegisterRequest request) {
        verifyRegistrationOtp(request.getEmail(), request.getOtpCode());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username is already taken");
        }

        com.example.documentmanagement.entity.User user = com.example.documentmanagement.entity.User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(false)
                .requestedRole(request.getRequestedRole())
                .registrationStatus("PENDING")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        userRepository.save(user);
        otpRepository.deleteByEmailAndType(request.getEmail(), TYPE_REGISTRATION);
    }

    @Override
    public org.springframework.data.domain.Page<com.example.documentmanagement.entity.User> getPendingRegistrationRequests(
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByRegistrationStatus("PENDING", pageable);
    }

    @Override
    @Transactional
    public void approveRegistration(Long userId, String finalRole) {
        log.info("APPROVE: Processing request for UserID: {}, Role: {}", userId, finalRole);

        com.example.documentmanagement.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        com.example.documentmanagement.entity.Role role = roleRepository.findByName(finalRole)
                .orElseThrow(() -> new BusinessException("Assigned role '" + finalRole + "' does not exist in system"));

        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        user.setActive(true);
        user.setRegistrationStatus("APPROVED");
        userRepository.save(user);

        log.info("APPROVE SUCCESS: User {} activated as {}", user.getUsername(), finalRole);

        try {
            emailService.sendApprovalEmail(user.getEmail(), finalRole);
        } catch (Exception e) {
            log.error("EMAIL ERROR: Could not send approval notification to {}", user.getEmail());
        }
    }

    @Override
    @Transactional
    public void rejectRegistration(Long userId) {
        log.info("REJECT: Processing request for UserID: {}", userId);
        com.example.documentmanagement.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        user.setRegistrationStatus("REJECTED");
        user.setActive(false);
        userRepository.save(user);

        try {
            emailService.sendRejectionEmail(user.getEmail(),
                    "Your application does not meet our current requirements.");
        } catch (Exception e) {
            log.error("EMAIL ERROR: Could not send rejection notification to {}", user.getEmail());
        }
    }

    @Override
    @Transactional
    public void softDeleteRegistration(Long userId) {
        log.info("DELETE: Archiving request for UserID: {}", userId);
        com.example.documentmanagement.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        user.setRegistrationStatus("DELETED");
        user.setActive(false);
        userRepository.save(user);

        try {
            emailService.sendDeletionEmail(user.getEmail());
        } catch (Exception e) {
            log.error("EMAIL ERROR: Could not send deletion notification to {}", user.getEmail());
        }
    }

    @Override
    @Transactional
    public void restoreRegistration(Long userId) {
        com.example.documentmanagement.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        user.setRegistrationStatus("PENDING");
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public org.springframework.data.domain.Page<com.example.documentmanagement.entity.User> getInactiveRegistrationRequests(
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByRegistrationStatusIn(List.of("REJECTED", "DELETED"), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<com.example.documentmanagement.entity.User> getApprovedUsers(
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByRegistrationStatus("APPROVED", pageable);
    }

    @Override
    @Transactional
    public void forgotPassword(com.example.documentmanagement.dto.request.ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(MSG_USER_NOT_FOUND + " with email: " + request.getEmail());
        }

        String code = String.format("%06d", RANDOM.nextInt(999999));
        com.example.documentmanagement.entity.Otp otp = com.example.documentmanagement.entity.Otp.builder()
                .email(request.getEmail())
                .code(code)
                .type(TYPE_FORGOT_PASSWORD)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(10))
                .build();

        otpRepository.deleteByEmailAndType(request.getEmail(), TYPE_FORGOT_PASSWORD);
        otpRepository.save(otp);
        emailService.sendOtp(request.getEmail(), code);
    }

    @Override
    public void verifyForgotPasswordOtp(String email, String code) {
        com.example.documentmanagement.entity.Otp otp = otpRepository.findByEmailAndType(email, TYPE_FORGOT_PASSWORD)
                .orElseThrow(() -> new BusinessException("Valid OTP not found for this email"));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new BusinessException("OTP has expired");
        }

        if (!otp.getCode().equals(code)) {
            throw new BusinessException("Invalid OTP code");
        }
    }

    @Override
    @Transactional
    public void resetPassword(com.example.documentmanagement.dto.request.ResetPasswordRequest request) {
        verifyForgotPasswordOtp(request.getEmail(), request.getOtpCode());

        com.example.documentmanagement.entity.User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpRepository.deleteByEmailAndType(request.getEmail(), TYPE_FORGOT_PASSWORD);
    }
}
