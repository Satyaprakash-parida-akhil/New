package com.example.documentmanagement.repository;

import com.example.documentmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRoles_Name(String roleName);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByReferralCodeIgnoreCase(String referralCode);
    boolean existsByReferralCodeIgnoreCase(String referralCode);

    List<User> findByReferredBy_Id(Long parentId);
    long countByReferredBy_Id(Long parentId);
    List<User> findByReferredByIsNullOrderByIdAsc();

    List<User> findByReferredBy_IdAndIsActiveTrue(Long parentId);

    long countByReferredBy_IdAndIsActiveTrue(Long parentId);

    List<User> findByReferredByIsNullAndIsActiveTrueOrderByIdAsc();

    Page<User> findByReferredByIsNullAndIsActiveTrue(Pageable pageable);

    Page<User> findByIsActiveTrue(Pageable pageable);

    Page<User> findByIdInAndIsActiveTrue(List<Long> ids, Pageable pageable);

    long countByIsActiveTrue();
    long countByIsActiveFalse();
    long countByPaymentStatus(String paymentStatus);
    long countByPaymentStatusNot(String paymentStatus);

    long countByRoles_NameAndIsActive(String roleName, boolean isActive);

    List<User> findByPinCodeAndIsActiveTrueOrderByFullNameAsc(String pinCode);

    Page<User> findByRegistrationStatus(String status, Pageable pageable);
    Page<User> findByRegistrationStatusAndIsActive(String status, boolean isActive, Pageable pageable);
    Page<User> findByRegistrationStatusIn(List<String> statuses, Pageable pageable);
}
