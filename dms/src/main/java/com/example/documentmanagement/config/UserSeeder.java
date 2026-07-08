package com.example.documentmanagement.config;

import com.example.documentmanagement.entity.Role;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.RoleRepository;
import com.example.documentmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

/**
 * UserSeeder — runs on every startup.
 * Ensures all required Roles exist, and creates a default admin user
 * if no admin user is found in the database. This is critical for
 * first-time cloud deployments (e.g., Render) where the database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(0) // Run before MasterDataSeeder (Order 1)
public class UserSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.default.admin.password:admin_Sec#2026}")
    private String adminPassword;

    private static final String[] ALL_ROLES = {
            "ROLE_ADMIN", "ROLE_REVIEWER", "ROLE_UPLOADER", "ROLE_DEALER"
    };

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== UserSeeder START ===");

        // 1. Ensure all roles exist
        for (String roleName : ALL_ROLES) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }

        // 2. Create default admin user if no admin exists
        boolean adminExists = userRepository.existsByUsername("admin");
        if (!adminExists) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found after seeding"));

            User admin = User.builder()
                    .username("admin")
                    .email("admin@bazaarjanakalyan.com")
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName("System Administrator")
                    .referralCode("ADM00001")
                    .isActive(true)
                    .otpVerified(true)
                    .paymentStatus("APPROVED")
                    .registrationStatus("APPROVED")
                    .requestedRole("ROLE_ADMIN")
                    .roles(new HashSet<>(Collections.singletonList(adminRole)))
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            log.info("✅ Default admin user created: username=admin, password={}", adminPassword);
        }
        
        // 3. Sync database integrity: ensure any users with APPROVED/COMPLETED/PAID status are set to active
        log.info("Syncing user activation status based on payment status...");
        jdbcTemplate.execute("UPDATE users SET is_active = true WHERE payment_status IN ('COMPLETED', 'PAID', 'APPROVED') AND is_active = false");

        log.info("=== UserSeeder DONE ===");
    }
}
