package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.request.RegisterRequest;
import com.example.documentmanagement.dto.request.UserUpdateRequest;
import com.example.documentmanagement.dto.response.UserResponse;
import com.example.documentmanagement.dto.response.DashboardStats;
import com.example.documentmanagement.dto.response.ReferralNode;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.entity.Role;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.util.MessageConstants;
import com.example.documentmanagement.repository.RoleRepository;
import com.example.documentmanagement.service.UserService;
import com.example.documentmanagement.service.MasterService;
import com.example.documentmanagement.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MasterService masterService;

    @Override
    public List<UserResponse> getReviewers() {
        List<User> reviewers = userRepository.findByRoles_Name("ROLE_REVIEWER");
        return reviewers.stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(MessageConstants.Validation.USERNAME_TAKEN);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(MessageConstants.Validation.EMAIL_REGISTERED);
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank() && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException(MessageConstants.Validation.PHONE_REGISTERED);
        }

        LocalDate dob = null;
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                dob = LocalDate.parse(request.getDateOfBirth());
            } catch (Exception e) {
                log.warn("Failed to parse date of birth: {}", request.getDateOfBirth());
            }
        }

        String refCode = generateUniqueReferralCode(request.getUsername());
        User parent = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            parent = userRepository.findByReferralCodeIgnoreCase(request.getPromoCode().trim())
                    .orElseThrow(() -> new BusinessException(MessageConstants.Error.INVALID_REFERRAL_CODE));
        }

        String assignedRoleName = request.getRequestedRole() != null ? request.getRequestedRole() : "ROLE_UPLOADER";
        Role role = roleRepository.findByName(assignedRoleName)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.ROLE_NOT_FOUND + ": " + assignedRoleName));

        User user = User.builder()
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
                .registrationMethod(request.getRegistrationMethod())
                .referralCode(refCode)
                .referredBy(parent)
                .isActive(true)
                .otpVerified(true)
                .paymentStatus("PENDING")
                .registrationStatus("APPROVED")
                .requestedRole(assignedRoleName)
                .roles(new HashSet<>(Collections.singletonList(role)))
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(MessageConstants.Validation.EMAIL_IN_USE);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            if (!user.getPhoneNumber().equalsIgnoreCase(request.getPhoneNumber()) && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BusinessException(MessageConstants.Validation.PHONE_IN_USE);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Failed to parse date of birth: {}", request.getDateOfBirth());
            }
        }
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getBlock() != null) user.setBlock(request.getBlock());
        if (request.getTown() != null) user.setTown(request.getTown());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getVillage() != null) user.setVillage(request.getVillage());
        if (request.getLandmark() != null) user.setLandmark(request.getLandmark());
        if (request.getDistrict() != null) user.setDistrict(request.getDistrict());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getPinCode() != null) user.setPinCode(request.getPinCode());
        if (request.getZone() != null) user.setZone(request.getZone());
        if (request.getPaymentStatus() != null) user.setPaymentStatus(request.getPaymentStatus());
        if (request.getIsActive() != null) user.setActive(request.getIsActive());

        if (request.getRequestedRole() != null) {
            Role role = roleRepository.findByName(request.getRequestedRole())
                    .orElseThrow(() -> new BusinessException(MessageConstants.Error.ROLE_NOT_FOUND + ": " + request.getRequestedRole()));
            user.setRoles(new HashSet<>(Collections.singletonList(role)));
            user.setRequestedRole(request.getRequestedRole());
        }

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

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));
        user.setRegistrationStatus("DELETED");
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void blockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));
        user.setRegistrationStatus("BLOCKED");
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void restoreUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));
        user.setRegistrationStatus("APPROVED");
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void permanentDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND + " with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public DashboardStats getDashboardStats() {
        return DashboardStats.builder()
                .activeUsers(userRepository.countByIsActiveTrue())
                .inactiveUsers(userRepository.countByIsActiveFalse())
                .paidUsers(userRepository.countByPaymentStatus("COMPLETED"))
                .unpaidUsers(userRepository.countByPaymentStatusNot("COMPLETED"))
                .build();
    }

    @Override
    public ReferralNode getReferralTree(Long userId) {
        User rootUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        return buildReferralNode(rootUser, 0, 5); // default max level depth = 5
    }

    private ReferralNode buildReferralNode(User user, int currentLevel, int maxDepth) {
        List<User> children = userRepository.findByReferredBy_Id(user.getId());
        List<ReferralNode> childNodes = new ArrayList<>();
        int downlineCount = children.size();

        if (currentLevel < maxDepth) {
            for (User child : children) {
                ReferralNode childNode = buildReferralNode(child, currentLevel + 1, maxDepth);
                childNodes.add(childNode);
                downlineCount += childNode.getTotalDownlineCount();
            }
        }

        return ReferralNode.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .paymentStatus(user.getPaymentStatus())
                .isActive(user.isActive())
                .level(currentLevel)
                .children(childNodes)
                .totalDownlineCount(downlineCount)
                .referralCount(children.size())
                .referralCode(user.getReferralCode())
                .joinedDate(user.getCreatedAt())
                .build();
    }

    @Override
    public List<UserResponse> searchReferralTree(Long requesterId, String searchTerm, boolean isAdmin) {
        String search = (searchTerm == null || searchTerm.isBlank()) ? "" : searchTerm.trim().toLowerCase();
        if (search.isEmpty()) return Collections.emptyList();

        List<User> matchingUsers;
        if (!isAdmin) {
            List<Long> downlineIds = userRepository.findDownlineUserIds(requesterId);
            if (downlineIds == null || downlineIds.isEmpty()) return Collections.emptyList();
            matchingUsers = userRepository.searchUsersByIds(downlineIds, search, null, null, null, null, null, null, null, null, null, null, null, Pageable.unpaged()).getContent();
        } else {
            matchingUsers = userRepository.searchUsers(search, null, null, null, null, null, null, null, null, null, null, null, Pageable.unpaged()).getContent();
        }

        return matchingUsers.stream().map(user -> UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .paymentStatus(user.getPaymentStatus())
                .isPaid("COMPLETED".equals(user.getPaymentStatus()))
                .isActive(user.isActive())
                .referralCode(user.getReferralCode())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build()).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<User> getPagedUsers(String search, Boolean isActive, String registrationStatus, String paymentStatus, String zone, String country, String state, String district, String block, String town, String village, String createdAt, Pageable pageable, Long requesterId, boolean isAdmin) {
        String normalizedSearch = (search == null || search.isBlank()) ? "" : search.trim();
        
        java.time.LocalDate createdDate = null;
        if (createdAt != null && !createdAt.isBlank()) {
            try {
                createdDate = java.time.LocalDate.parse(createdAt.trim());
            } catch (Exception e) {
                // Ignore parse exception, fall back to null
            }
        }

        if (!isAdmin) {
            List<Long> downlineIds = userRepository.findDownlineUserIds(requesterId);
            if (downlineIds == null || downlineIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            return userRepository.searchUsersByIds(downlineIds, normalizedSearch, isActive, registrationStatus, paymentStatus, zone, country, state, district, block, town, village, createdDate, pageable);
        }
        return userRepository.searchUsers(normalizedSearch, isActive, registrationStatus, paymentStatus, zone, country, state, district, block, town, village, createdDate, pageable);
    }

    @Override
    public java.util.Map<String, java.util.List<String>> getUserFilterOptions() {
        java.util.Map<String, java.util.List<String>> options = new java.util.HashMap<>();
        options.put("username", userRepository.findDistinctUsernames());
        options.put("fullName", userRepository.findDistinctFullNames());
        options.put("referralCode", userRepository.findDistinctReferralCodes());
        
        java.util.List<java.sql.Date> dates = userRepository.findDistinctCreatedDates();
        java.util.List<String> dateStrings = new java.util.ArrayList<>();
        if (dates != null) {
            for (java.sql.Date d : dates) {
                if (d != null) {
                    dateStrings.add(d.toString());
                }
            }
        }
        options.put("createdAt", dateStrings);
        
        return options;
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
                code = UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "").substring(0, 6).toUpperCase();
            }
        } while (userRepository.existsByReferralCodeIgnoreCase(code));
        
        return code;
    }
}
