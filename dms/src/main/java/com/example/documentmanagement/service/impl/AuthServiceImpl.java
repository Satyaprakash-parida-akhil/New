package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.request.LoginRequest;
import com.example.documentmanagement.dto.response.TokenResponse;
import com.example.documentmanagement.security.JwtUtil;
import com.example.documentmanagement.service.AuthService;
import com.example.documentmanagement.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.documentmanagement.service.MasterService;
import com.example.documentmanagement.util.MessageConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final Random RANDOM = new Random();
    private static final String TYPE_REGISTRATION = "REGISTRATION";
    private static final String TYPE_FORGOT_PASSWORD = "FORGOT_PASSWORD";

    @Value("${app.upload.dir:uploads/profile-photos}")
    private String uploadDir;

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final com.example.documentmanagement.repository.UserRepository userRepository;
    private final com.example.documentmanagement.repository.RoleRepository roleRepository;
    private final com.example.documentmanagement.repository.OtpRepository otpRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final MasterService masterService;
    private final com.example.documentmanagement.service.EmailService emailService;
    private final com.example.documentmanagement.service.SmsService smsService;

    @Override
    public TokenResponse authenticateAndGenerateToken(LoginRequest loginRequest) {
        log.info("AUTHENTICATION START: Attempting login for user: {}", loginRequest.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.info("AUTHENTICATION SUCCESS: User {} verified. Generating token.", userDetails.getUsername());

        // Fetch the User entity to embed isActive into the JWT claim
        boolean isActive = userRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.isActive())
                .orElse(false);

        String token = jwtUtil.generateToken(userDetails, isActive);

        return TokenResponse.builder()
                .accessToken(token)
                .username(userDetails.getUsername())
                .build();
    }

    /**
     * Send OTP to either email or phone number depending on the method chosen.
     * For phone OTP, we log to console (plug in real SMS provider when ready).
     */
    @Override
    @Transactional
    public String sendRegistrationOtp(String identifier, String method) {
        log.info("OTP REQUEST: identifier={}, method={}", identifier, method);

        if ("PHONE".equalsIgnoreCase(method)) {
            if (userRepository.existsByPhoneNumber(identifier)) {
                throw new BusinessException(MessageConstants.Validation.PHONE_REGISTERED);
            }
        } else {
            if (userRepository.existsByEmail(identifier)) {
                throw new BusinessException(MessageConstants.Validation.EMAIL_IN_USE);
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(999999));
        com.example.documentmanagement.entity.Otp otp = com.example.documentmanagement.entity.Otp.builder()
                .email(identifier)   // reuse the 'email' column to store either email or phone
                .code(code)
                .type(TYPE_REGISTRATION)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        otpRepository.deleteByEmailAndType(identifier, TYPE_REGISTRATION);
        otpRepository.save(otp);

        if ("PHONE".equalsIgnoreCase(method)) {
            smsService.sendOtp(identifier, code);
        } else {
            emailService.sendOtp(identifier, code);
        }
        return code;
    }

    @Override
    public void verifyRegistrationOtp(String identifier, String code, String method) {
        log.info("OTP VERIFICATION: identifier={}, method={}", identifier, method);
        com.example.documentmanagement.entity.Otp otp = otpRepository.findByEmailAndType(identifier, TYPE_REGISTRATION)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.OTP_NOT_FOUND));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new BusinessException(MessageConstants.Error.OTP_EXPIRED);
        }

        if (!otp.getCode().equals(code)) {
            throw new BusinessException(MessageConstants.Error.OTP_INVALID);
        }
    }

    @Override
    @Transactional
    public void register(com.example.documentmanagement.dto.request.RegisterRequest request, MultipartFile photo) {
        String identifier = "PHONE".equalsIgnoreCase(request.getRegistrationMethod())
                ? request.getPhoneNumber()
                : request.getEmail();

        // Verify OTP was completed
        verifyRegistrationOtp(identifier, request.getOtpCode(), request.getRegistrationMethod());

        // Validate both email and phone are present
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException(MessageConstants.Validation.EMAIL_REQUIRED);
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BusinessException(MessageConstants.Validation.PHONE_REQUIRED);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(MessageConstants.Validation.USERNAME_TAKEN);
        }

        // Validate secondary identifier uniqueness
        if ("PHONE".equalsIgnoreCase(request.getRegistrationMethod())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(MessageConstants.Validation.EMAIL_IN_USE);
            }
        } else {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BusinessException(MessageConstants.Validation.PHONE_IN_USE);
            }
        }

        // Save profile photo if provided
        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            photoPath = saveProfilePhoto(photo, request.getUsername());
        } else if (request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            String url = request.getPhotoUrl();
            if (url.contains("/")) {
                photoPath = url.substring(url.lastIndexOf("/") + 1);
            } else {
                photoPath = url;
            }
        }

        // Parse date of birth
        LocalDate dob = null;
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                dob = LocalDate.parse(request.getDateOfBirth());
            } catch (Exception e) {
                log.warn("Could not parse date of birth: {}", request.getDateOfBirth());
            }
        }

        // Assign default role
        com.example.documentmanagement.entity.Role defaultRole = roleRepository
                .findByName("ROLE_UPLOADER")
                .orElse(null);

        String refCode = generateUniqueReferralCode(request.getUsername());
        com.example.documentmanagement.entity.User parent = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            parent = userRepository.findByReferralCodeIgnoreCase(request.getPromoCode().trim())
                    .orElseThrow(() -> new BusinessException(MessageConstants.Error.INVALID_REFERRAL_CODE));
        }

        com.example.documentmanagement.entity.User user = com.example.documentmanagement.entity.User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .dateOfBirth(dob)
                .gender(request.getGender())
                .address(request.getAddress())
                .block(request.getBlock())
                .town(request.getTown())
                .state(request.getState())
                .village(request.getVillage())
                .landmark(request.getLandmark())
                .district(request.getDistrict())
                .country(request.getCountry())
                .pinCode(request.getPinCode())
                .zone(request.getZone())
                .registrationMethod(request.getRegistrationMethod() != null ? request.getRegistrationMethod().toUpperCase() : "EMAIL")
                .profilePhotoPath(photoPath)
                .isActive(false)
                .otpVerified(true)           // user verified OTP → can login
                .paymentStatus("PENDING")    // profile will show Inactive until paid
                .requestedRole(request.getRequestedRole() != null ? request.getRequestedRole() : "ROLE_UPLOADER")
                .registrationStatus("PENDING")
                .referralCode(refCode)
                .referredBy(parent)
                .createdAt(LocalDateTime.now())
                .roles(defaultRole != null ? new HashSet<>(Collections.singletonList(defaultRole)) : new HashSet<>())
                .build();

        // Auto create/resolve geographical hierarchy in Master Data
        masterService.autoCreateGeographicalHierarchy(
                request.getCountry(),
                request.getZone(),
                request.getState(),
                request.getDistrict(),
                request.getBlock(),
                request.getTown(),
                request.getVillage()
        );

        userRepository.save(user);
        otpRepository.deleteByEmailAndType(identifier, TYPE_REGISTRATION);

        log.info("REGISTRATION SUCCESS: User {} registered via {}", request.getUsername(), request.getRegistrationMethod());
    }

    private String saveProfilePhoto(MultipartFile photo, String username) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String extension = "";
            String originalFilename = photo.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = username + "_" + UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            log.error("Could not save profile photo for user {}: {}", username, e.getMessage());
            return null;
        }
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
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

        String roleToAssign = (finalRole != null && !finalRole.trim().isEmpty()) ? finalRole.trim() : "ROLE_UPLOADER";

        com.example.documentmanagement.entity.Role role = roleRepository.findByName(roleToAssign)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.ROLE_NOT_FOUND + ": " + roleToAssign));

        user.getRoles().clear();
        userRepository.saveAndFlush(user);
        user.getRoles().add(role);
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
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

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
            Boolean active, org.springframework.data.domain.Pageable pageable) {
        if (active != null) {
            return userRepository.findByRegistrationStatusAndIsActive("APPROVED", active, pageable);
        }
        return userRepository.findByRegistrationStatus("APPROVED", pageable);
    }

    @Override
    @Transactional
    public String forgotPassword(com.example.documentmanagement.dto.request.ForgotPasswordRequest request) {
        if ("PHONE".equalsIgnoreCase(request.getMethod())) {
            if (!userRepository.existsByPhoneNumber(request.getIdentifier())) {
                throw new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with phone: " + request.getIdentifier());
            }
        } else {
            if (!userRepository.existsByEmail(request.getIdentifier())) {
                throw new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with email: " + request.getIdentifier());
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(999999));
        com.example.documentmanagement.entity.Otp otp = com.example.documentmanagement.entity.Otp.builder()
                .email(request.getIdentifier())
                .code(code)
                .type(TYPE_FORGOT_PASSWORD)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        otpRepository.deleteByEmailAndType(request.getIdentifier(), TYPE_FORGOT_PASSWORD);
        otpRepository.save(otp);

        if ("PHONE".equalsIgnoreCase(request.getMethod())) {
            smsService.sendOtp(request.getIdentifier(), code);
        } else {
            emailService.sendOtp(request.getIdentifier(), code);
        }
        return code;
    }

    @Override
    public void verifyForgotPasswordOtp(String identifier, String code, String method) {
        com.example.documentmanagement.entity.Otp otp = otpRepository.findByEmailAndType(identifier, TYPE_FORGOT_PASSWORD)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.OTP_NOT_FOUND));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new BusinessException(MessageConstants.Error.OTP_EXPIRED);
        }

        if (!otp.getCode().equals(code)) {
            throw new BusinessException(MessageConstants.Error.OTP_INVALID);
        }
    }

    @Override
    @Transactional
    public void resetPassword(com.example.documentmanagement.dto.request.ResetPasswordRequest request) {
        verifyForgotPasswordOtp(request.getIdentifier(), request.getOtpCode(), request.getMethod());

        com.example.documentmanagement.entity.User user;
        if ("PHONE".equalsIgnoreCase(request.getMethod())) {
             user = userRepository.findByPhoneNumber(request.getIdentifier())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        } else {
             user = userRepository.findByEmail(request.getIdentifier())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpRepository.deleteByEmailAndType(request.getIdentifier(), TYPE_FORGOT_PASSWORD);
    }

    private String generateUniqueReferralCode(String username) {
        String cleanName = username.replaceAll("[^a-zA-Z]", "").toUpperCase();
        if (cleanName.isEmpty()) {
            cleanName = "USR";
        }
        String namePortion = cleanName.length() >= 3 ? cleanName.substring(0, 3) : String.format("%-3s", cleanName).replace(' ', 'X');
        char[] specials = {'@', '#', '$', '&', '%'};
        
        String code;
        int attempts = 0;
        do {
            char special = specials[RANDOM.nextInt(specials.length)];
            int digits;
            if (namePortion.length() == 3) {
                digits = 10 + RANDOM.nextInt(90);
            } else if (namePortion.length() == 2) {
                digits = 100 + RANDOM.nextInt(900);
            } else {
                digits = 1000 + RANDOM.nextInt(9000);
            }
            code = namePortion + special + digits;
            attempts++;
            if (attempts > 50) {
                code = UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "").substring(0, 6).toUpperCase();
            }
        } while (userRepository.existsByReferralCodeIgnoreCase(code));
        
        return code;
    }

    @Override
    public void validatePromoCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(MessageConstants.Error.INVALID_REFERRAL_CODE);
        }
        userRepository.findByReferralCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.INVALID_REFERRAL_CODE));
    }
}
