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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

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
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(MessageConstants.Validation.EMAIL_REGISTERED);
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
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
                .orElseThrow(
                        () -> new BusinessException(MessageConstants.Error.ROLE_NOT_FOUND + ": " + assignedRoleName));

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

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(MessageConstants.Validation.EMAIL_IN_USE);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            if (!user.getPhoneNumber().equalsIgnoreCase(request.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BusinessException(MessageConstants.Validation.PHONE_IN_USE);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null)
            user.setGender(request.getGender());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Failed to parse date of birth: {}", request.getDateOfBirth());
            }
        }
        if (request.getAddress() != null)
            user.setAddress(request.getAddress());
        if (request.getBlock() != null)
            user.setBlock(request.getBlock());
        if (request.getTown() != null)
            user.setTown(request.getTown());
        if (request.getState() != null)
            user.setState(request.getState());
        if (request.getVillage() != null)
            user.setVillage(request.getVillage());
        if (request.getLandmark() != null)
            user.setLandmark(request.getLandmark());
        if (request.getDistrict() != null)
            user.setDistrict(request.getDistrict());
        if (request.getCountry() != null)
            user.setCountry(request.getCountry());
        if (request.getPinCode() != null)
            user.setPinCode(request.getPinCode());
        if (request.getZone() != null)
            user.setZone(request.getZone());
        if (request.getPaymentStatus() != null) {
            user.setPaymentStatus(request.getPaymentStatus());
            if ("COMPLETED".equalsIgnoreCase(request.getPaymentStatus())
                    || "PAID".equalsIgnoreCase(request.getPaymentStatus())) {
                user.setActive(true);
            }
        }
        if (request.getIsActive() != null)
            user.setActive(request.getIsActive());

        if (request.getRequestedRole() != null) {
            Role role = roleRepository.findByName(request.getRequestedRole())
                    .orElseThrow(() -> new BusinessException(
                            MessageConstants.Error.ROLE_NOT_FOUND + ": " + request.getRequestedRole()));
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
                user.getVillage());

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
    public ReferralNode getReferralTree(Long userId, boolean isAdminView) {
        if (isAdminView) {
            List<User> rootUsers = userRepository.findByReferredByIsNullAndIsActiveTrueOrderByIdAsc();
            List<ReferralNode> childNodes = new ArrayList<>();
            int totalDownline = 0;
            for (User rootUser : rootUsers) {
                ReferralNode childNode = buildReferralNode(rootUser, 1, 100);
                childNodes.add(childNode);
                totalDownline += childNode.getTotalDownlineCount() + 1; // Include the root user themselves
            }
            return ReferralNode.builder()
                    .userId(0L) // Virtual Root ID
                    .username("system_root")
                    .fullName("Bazaar Janakalyan Network")
                    .paymentStatus("APPROVED")
                    .isActive(true)
                    .level(0)
                    .children(childNodes)
                    .totalDownlineCount(totalDownline)
                    .referralCount(rootUsers.size())
                    .build();
        }

        User rootUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        return buildReferralNode(rootUser, 0, 100); // Max level depth = 100
    }

    @Override
    public Page<ReferralNode> getReferralTreePage(Long userId, boolean isAdminView, Pageable pageable) {
        if (isAdminView) {
            // Apply sorting at the Service layer for custom logic (createdAt DESC)
            Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                            "createdAt"));
            Page<User> allUsers = userRepository.findByIsActiveTrue(sortedPageable);
            return allUsers.map(user -> buildShallowNode(user));
        }

        // For regular user: only show their own node as the root of their tree page
        User self = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MessageConstants.Error.USER_NOT_FOUND));
        ReferralNode selfNode = buildShallowNode(self);
        return new PageImpl<>(List.of(selfNode), org.springframework.data.domain.PageRequest.of(0, 1), 1);
    }

    private ReferralNode buildReferralNode(User user, int currentLevel, int maxDepth) {
        List<User> children = userRepository.findByReferredBy_IdAndIsActiveTrue(user.getId());
        List<ReferralNode> childNodes = new ArrayList<>();
        int downlineCount = children.size();

        if (currentLevel < maxDepth) {
            for (User child : children) {
                ReferralNode childNode = buildReferralNode(child, currentLevel + 1, maxDepth);
                childNodes.add(childNode);
                downlineCount += childNode.getTotalDownlineCount();
            }
        }

        boolean isPaid = "COMPLETED".equalsIgnoreCase(user.getPaymentStatus())
                || "PAID".equalsIgnoreCase(user.getPaymentStatus())
                || "APPROVED".equalsIgnoreCase(user.getPaymentStatus());
        boolean activeStatus = user.isActive() && isPaid;

        String referredByName = null;
        String referredByCode = null;
        if (user.getReferredBy() != null) {
            referredByName = user.getReferredBy().getFullName();
            if (referredByName == null || referredByName.isBlank()) {
                referredByName = user.getReferredBy().getUsername();
            }
            referredByCode = user.getReferredBy().getReferralCode();
        }

        return ReferralNode.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRoles().isEmpty() ? "UPLOADER"
                        : user.getRoles().iterator().next().getName().replace("ROLE_", ""))
                .paymentStatus(user.getPaymentStatus())
                .isActive(activeStatus)
                .level(currentLevel)
                .children(childNodes)
                .totalDownlineCount(downlineCount)
                .referralCount(children.size())
                .referralCode(user.getReferralCode())
                .joinedDate(user.getCreatedAt())
                .referredByName(referredByName)
                .referredByCode(referredByCode)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    @Override
    public List<ReferralNode> getDirectChildren(Long targetId, Long requesterId, boolean isAdmin) {
        if (!isAdmin && requesterId != null) {
            if (targetId == 0L) {
                throw new BusinessException(MessageConstants.Error.UNAUTHORIZED_ACCESS);
            }
            // Check if targetId is in downline of requesterId
            if (!targetId.equals(requesterId)) {
                List<Long> downlineIds = getDownlineUserIds(requesterId);
                if (!downlineIds.contains(targetId)) {
                    throw new BusinessException(MessageConstants.Error.ACCESS_DENIED_DOWNLINE);
                }
            }
        }

        List<User> childrenUsers;
        if (targetId == 0L) {
            // If targetId is 0, return root users
            childrenUsers = userRepository.findByReferredByIsNullAndIsActiveTrueOrderByIdAsc();
        } else {
            childrenUsers = userRepository.findByReferredBy_IdAndIsActiveTrue(targetId);
        }

        List<ReferralNode> childrenNodes = new ArrayList<>();
        for (User child : childrenUsers) {
            childrenNodes.add(buildShallowNode(child));
        }

        return childrenNodes;
    }

    private ReferralNode buildShallowNode(User user) {
        long referralCount = userRepository.countByReferredBy_IdAndIsActiveTrue(user.getId());

        boolean isPaid = "COMPLETED".equalsIgnoreCase(user.getPaymentStatus())
                || "PAID".equalsIgnoreCase(user.getPaymentStatus())
                || "APPROVED".equalsIgnoreCase(user.getPaymentStatus());
        boolean activeStatus = user.isActive() && isPaid;

        String referredByName = null;
        String referredByCode = null;
        if (user.getReferredBy() != null) {
            referredByName = user.getReferredBy().getFullName();
            if (referredByName == null || referredByName.isBlank()) {
                referredByName = user.getReferredBy().getUsername();
            }
            referredByCode = user.getReferredBy().getReferralCode();
        }

        return ReferralNode.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRoles().isEmpty() ? "UPLOADER"
                        : user.getRoles().iterator().next().getName().replace("ROLE_", ""))
                .paymentStatus(user.getPaymentStatus())
                .isActive(activeStatus)
                .level(0) // Level is handled by frontend for flat structure
                .children(new ArrayList<>())
                .totalDownlineCount(0) // Too expensive to calculate on the fly for all, optional
                .referralCount((int) referralCount)
                .referralCode(user.getReferralCode())
                .joinedDate(user.getCreatedAt())
                .referredByName(referredByName)
                .referredByCode(referredByCode)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    @Override
    public List<UserResponse> searchReferralTree(Long requesterId, String searchTerm, boolean isAdmin) {
        String search = (searchTerm == null || searchTerm.isBlank()) ? "" : searchTerm.trim().toLowerCase();

        List<User> matchingUsers;
        Pageable limitPageable = search.isEmpty()
                ? org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "fullName"))
                : Pageable.unpaged();

        if (!isAdmin) {
            List<Long> downlineIds = getDownlineUserIds(requesterId);
            List<Long> searchIds = new ArrayList<>();
            if (downlineIds != null) {
                searchIds.addAll(downlineIds);
            }
            searchIds.add(requesterId);
            matchingUsers = searchUsersServiceLogic(searchIds, search.isEmpty() ? null : search, null, "APPROVED", null, null, null, null, null,
                    null, null, null, null, limitPageable).getContent();
        } else {
            matchingUsers = searchUsersServiceLogic(null, search.isEmpty() ? null : search, null, "APPROVED", null, null, null, null, null, null,
                    null, null, null, limitPageable).getContent();
        }

        return matchingUsers.stream().map(user -> {
            boolean isPaid = "COMPLETED".equalsIgnoreCase(user.getPaymentStatus())
                    || "PAID".equalsIgnoreCase(user.getPaymentStatus())
                    || "APPROVED".equalsIgnoreCase(user.getPaymentStatus());
            boolean activeStatus = user.isActive() && isPaid;
            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .paymentStatus(user.getPaymentStatus())
                    .isPaid(isPaid)
                    .isActive(activeStatus)
                    .referralCode(user.getReferralCode())
                    .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<User> getPagedUsers(String search, Boolean isActive, String registrationStatus, String paymentStatus,
            String zone, String country, String state, String district, String block, String town, String village,
            String createdAt, Pageable pageable, Long requesterId, boolean isAdmin) {
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
            List<Long> downlineIds = getDownlineUserIds(requesterId);
            if (downlineIds == null || downlineIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            return searchUsersServiceLogic(downlineIds, normalizedSearch, isActive, registrationStatus, paymentStatus,
                    zone, country, state, district, block, town, village, createdDate, pageable);
        }
        return searchUsersServiceLogic(null, normalizedSearch, isActive, registrationStatus, paymentStatus, zone,
                country, state, district, block, town, village, createdDate, pageable);
    }

    private Page<User> searchUsersServiceLogic(List<Long> ids, String search, Boolean isActive,
            String registrationStatus, String paymentStatus, String zone, String country, String state, String district,
            String block, String town, String village, java.time.LocalDate createdAt, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1 ");
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        if (ids != null) {
            if (ids.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            jpql.append("AND u.id IN :ids ");
            params.put("ids", ids);
        }

        if (search != null && !search.isBlank()) {
            jpql.append(
                    "AND (LOWER(u.username) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.fullName) LIKE :search OR LOWER(u.referralCode) LIKE :search OR LOWER(u.phoneNumber) LIKE :search) ");
            params.put("search", "%" + search.toLowerCase().trim() + "%");
        }

        if (isActive != null) {
            jpql.append("AND u.isActive = :isActive ");
            params.put("isActive", isActive);
        }

        if (registrationStatus != null && !registrationStatus.isBlank()) {
            jpql.append("AND u.registrationStatus = :registrationStatus ");
            params.put("registrationStatus", registrationStatus);
        }

        if (paymentStatus != null && !paymentStatus.isBlank()) {
            jpql.append("AND u.paymentStatus = :paymentStatus ");
            params.put("paymentStatus", paymentStatus);
        }

        if (zone != null && !zone.isBlank()) {
            jpql.append("AND LOWER(u.zone) = :zone ");
            params.put("zone", zone.toLowerCase().trim());
        }

        if (country != null && !country.isBlank()) {
            jpql.append("AND LOWER(u.country) = :country ");
            params.put("country", country.toLowerCase().trim());
        }

        if (state != null && !state.isBlank()) {
            jpql.append("AND LOWER(u.state) = :state ");
            params.put("state", state.toLowerCase().trim());
        }

        if (district != null && !district.isBlank()) {
            jpql.append("AND LOWER(u.district) = :district ");
            params.put("district", district.toLowerCase().trim());
        }

        if (block != null && !block.isBlank()) {
            jpql.append("AND LOWER(u.block) = :block ");
            params.put("block", block.toLowerCase().trim());
        }

        if (town != null && !town.isBlank()) {
            jpql.append("AND LOWER(u.town) = :town ");
            params.put("town", town.toLowerCase().trim());
        }

        if (village != null && !village.isBlank()) {
            jpql.append("AND LOWER(u.village) = :village ");
            params.put("village", village.toLowerCase().trim());
        }

        if (createdAt != null) {
            jpql.append("AND CAST(u.createdAt AS date) = :createdAt ");
            params.put("createdAt", java.sql.Date.valueOf(createdAt));
        }

        // Count Query
        String countJpql = jpql.toString().replace("SELECT u", "SELECT COUNT(u)");
        jakarta.persistence.TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        for (java.util.Map.Entry<String, Object> entry : params.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
        }
        long total = countQuery.getSingleResult();

        if (total == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Apply Sorting if present in pageable
        if (pageable.getSort().isSorted()) {
            jpql.append("ORDER BY ");
            String order = pageable.getSort().stream()
                    .map(o -> "u." + o.getProperty() + " " + o.getDirection().name())
                    .collect(java.util.stream.Collectors.joining(", "));
            jpql.append(order);
        }

        jakarta.persistence.TypedQuery<User> query = entityManager.createQuery(jpql.toString(), User.class);
        for (java.util.Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<User> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public java.util.Map<String, java.util.List<String>> getUserFilterOptions() {
        java.util.Map<String, java.util.List<String>> options = new java.util.HashMap<>();

        List<String> usernames = entityManager.createQuery(
                "SELECT DISTINCT u.username FROM User u WHERE u.username IS NOT NULL AND u.username != '' ORDER BY u.username",
                String.class).getResultList();
        List<String> fullNames = entityManager.createQuery(
                "SELECT DISTINCT u.fullName FROM User u WHERE u.fullName IS NOT NULL AND u.fullName != '' ORDER BY u.fullName",
                String.class).getResultList();
        List<String> referralCodes = entityManager.createQuery(
                "SELECT DISTINCT u.referralCode FROM User u WHERE u.referralCode IS NOT NULL AND u.referralCode != '' ORDER BY u.referralCode",
                String.class).getResultList();

        options.put("username", usernames);
        options.put("fullName", fullNames);
        options.put("referralCode", referralCodes);

        java.util.List<java.sql.Date> dates = entityManager.createQuery(
                "SELECT DISTINCT CAST(u.createdAt AS date) FROM User u WHERE u.createdAt IS NOT NULL ORDER BY CAST(u.createdAt AS date)",
                java.sql.Date.class).getResultList();
        java.util.List<String> dateStrings = new java.util.ArrayList<>();
        if (dates != null) {
            for (java.sql.Date d : dates) {
                if (d != null) {
                    dateStrings.add(d.toString());
                }
            }
        }
        options.put("createdAt", dateStrings);

        List<String> requestedRoles = entityManager.createQuery(
                "SELECT DISTINCT u.requestedRole FROM User u WHERE u.requestedRole IS NOT NULL AND u.requestedRole != '' ORDER BY u.requestedRole",
                String.class).getResultList();
        options.put("requestedRole", requestedRoles);

        List<String> registrationStatuses = entityManager.createQuery(
                "SELECT DISTINCT u.registrationStatus FROM User u WHERE u.registrationStatus IS NOT NULL AND u.registrationStatus != '' ORDER BY u.registrationStatus",
                String.class).getResultList();
        options.put("registrationStatus", registrationStatuses);

        return options;
    }

    @Override
    public List<Long> getDownlineUserIds(Long userId) {
        List<Long> downline = new ArrayList<>();
        collectDownlineIdsRecursive(userId, downline);
        return downline;
    }

    private void collectDownlineIdsRecursive(Long parentId, List<Long> accumulator) {
        List<User> children = userRepository.findByReferredBy_IdAndIsActiveTrue(parentId);
        for (User child : children) {
            accumulator.add(child.getId());
            collectDownlineIdsRecursive(child.getId(), accumulator);
        }
    }

    private String generateUniqueReferralCode(String username) {
        String cleanName = username.replaceAll("[^a-zA-Z]", "").toUpperCase();
        if (cleanName.isEmpty()) {
            cleanName = "USR";
        }
        String namePortion = cleanName.length() >= 3 ? cleanName.substring(0, 3)
                : String.format("%-3s", cleanName).replace(' ', 'X');
        char[] specials = { '@', '#', '$', '&', '%' };

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

    @Override
    public List<String> getDistinctStates() {
        return entityManager
                .createQuery("SELECT DISTINCT u.state FROM User u WHERE u.state IS NOT NULL ORDER BY u.state",
                        String.class)
                .getResultList();
    }

    @Override
    public List<String> getDistinctDistrictsByState(String state) {
        return entityManager.createQuery(
                "SELECT DISTINCT u.district FROM User u WHERE u.state = :state AND u.district IS NOT NULL ORDER BY u.district",
                String.class)
                .setParameter("state", state)
                .getResultList();
    }

    @Override
    public List<String> getDistinctBlocksByDistrict(String district) {
        return entityManager.createQuery(
                "SELECT DISTINCT u.block FROM User u WHERE u.district = :district AND u.block IS NOT NULL ORDER BY u.block",
                String.class)
                .setParameter("district", district)
                .getResultList();
    }

    @Override
    public List<String> getDistinctPinCodesByBlock(String block) {
        return entityManager.createQuery(
                "SELECT DISTINCT u.pinCode FROM User u WHERE u.block = :block AND u.pinCode IS NOT NULL ORDER BY u.pinCode",
                String.class)
                .setParameter("block", block)
                .getResultList();
    }

    @Override
    public List<User> getUsersByPinCode(String pinCode) {
        return userRepository.findByPinCodeAndIsActiveTrueOrderByFullNameAsc(pinCode);
    }
}
