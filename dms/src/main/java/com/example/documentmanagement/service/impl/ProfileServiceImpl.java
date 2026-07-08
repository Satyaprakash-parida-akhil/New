package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.response.ProfileResponse;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.exception.BusinessException;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    @Value("${app.upload.dir:uploads/profile-photos}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final UserRepository userRepository;
    private final MasterService masterService;

    @Override
    public ProfileResponse getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String username, String fullName, String gender, String dateOfBirth,
                                         String address, String block, String town, String state, String village, String landmark,
                                         String district, String country, String pinCode, String zone, MultipartFile photo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (gender != null && !gender.isBlank()) {
            user.setGender(gender);
        }
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            try {
                user.setDateOfBirth(LocalDate.parse(dateOfBirth));
            } catch (Exception e) {
                log.warn("Invalid date format for DOB: {}", dateOfBirth);
            }
        }
        if (address != null) {
            user.setAddress(address);
        }
        if (block != null) {
            user.setBlock(block);
        }
        if (town != null) {
            user.setTown(town);
        }
        if (state != null) {
            user.setState(state);
        }
        if (village != null) {
            user.setVillage(village);
        }
        if (landmark != null) {
            user.setLandmark(landmark);
        }
        if (district != null) {
            user.setDistrict(district);
        }
        if (country != null) {
            user.setCountry(country);
        }
        if (pinCode != null) {
            user.setPinCode(pinCode);
        }
        if (zone != null) {
            user.setZone(zone);
        }
        if (photo != null && !photo.isEmpty()) {
            String photoPath = savePhoto(photo, username);
            if (photoPath != null) {
                user.setProfilePhotoPath(photoPath);
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(username);

        // Auto create/resolve geographical hierarchy in Master Data
        masterService.autoCreateGeographicalHierarchy(
                user.getCountry(),
                user.getZone(),
                user.getState(),
                user.getDistrict(),
                user.getBlock(),
                user.getTown(),
                user.getVillage()
        );

        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse completePayment(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));

        user.setPaymentStatus("COMPLETED");
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(username);
        userRepository.save(user);
        log.info("PAYMENT COMPLETED: User {}", username);
        return toProfileResponse(user);
    }

    private ProfileResponse toProfileResponse(User user) {
        String photoUrl = null;
        if (user.getProfilePhotoPath() != null) {
            String path = user.getProfilePhotoPath();
            if (path.startsWith("http://") || path.startsWith("https://")) {
                photoUrl = path;
            } else if (path.startsWith("/uploads/") || path.startsWith("uploads/")) {
                photoUrl = path.startsWith("/") ? path : "/" + path;
            } else {
                photoUrl = "/api/profile/photo/" + path;
            }
        }

        // profileStatus is driven by the real isActive flag (set only on admin approval)
        String profileStatus = user.isActive() ? "Active" : "Inactive";

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .address(user.getAddress())
                .block(user.getBlock())
                .town(user.getTown())
                .state(user.getState())
                .village(user.getVillage())
                .landmark(user.getLandmark())
                .district(user.getDistrict())
                .country(user.getCountry())
                .pinCode(user.getPinCode())
                .registrationMethod(user.getRegistrationMethod())
                .profilePhotoUrl(photoUrl)
                .paymentStatus(user.getPaymentStatus())
                .profileStatus(profileStatus)
                .isActive(user.isActive())
                .registrationStatus(user.getRegistrationStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .zone(user.getZone())
                .referralCode(user.getReferralCode())
                .referredByUsername(user.getReferredBy() != null ? user.getReferredBy().getUsername() : null)
                .build();
    }

    private String savePhoto(MultipartFile photo, String username) {
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
            log.error("Could not save photo: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public ProfileResponse generateReferralCode(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        if (user.getReferralCode() != null && !user.getReferralCode().isBlank()) {
            throw new BusinessException(MessageConstants.Validation.REFERRAL_CODE_EXISTS);
        }
        String refCode = generateUniqueReferralCode(username);
        user.setReferralCode(refCode);
        userRepository.save(user);
        return toProfileResponse(user);
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
            char special = specials[new java.util.Random().nextInt(specials.length)];
            int digits;
            if (namePortion.length() == 3) {
                digits = 10 + new java.util.Random().nextInt(90);
            } else if (namePortion.length() == 2) {
                digits = 100 + new java.util.Random().nextInt(900);
            } else {
                digits = 1000 + new java.util.Random().nextInt(9000);
            }
            code = namePortion + special + digits;
            attempts++;
            if (attempts > 50) {
                code = java.util.UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "").substring(0, 6).toUpperCase();
            }
        } while (userRepository.existsByReferralCodeIgnoreCase(code));
        
        return code;
    }
}
