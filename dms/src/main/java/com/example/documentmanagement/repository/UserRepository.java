package com.example.documentmanagement.repository;

import com.example.documentmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    java.util.List<User> findByRoles_Name(String roleName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<User> findByRegistrationStatus(String status,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> findByRegistrationStatusIn(java.util.List<String> statuses,
            org.springframework.data.domain.Pageable pageable);
}
