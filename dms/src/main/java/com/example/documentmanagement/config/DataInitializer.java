package com.example.documentmanagement.config;

import com.example.documentmanagement.entity.Role;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.RoleRepository;
import com.example.documentmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default.admin.password}")
    private String adminPassword;

    @Value("${app.default.reviewer.password}")
    private String reviewerPassword;

    @Value("${app.default.uploader.password}")
    private String uploaderPassword;

    @Override
    public void run(String... args) throws Exception {
        // Initialize Roles
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN");
        Role reviewerRole = createRoleIfNotFound("ROLE_REVIEWER");
        Role uploaderRole = createRoleIfNotFound("ROLE_UPLOADER");

        // Initialize Admin User
        User admin = userRepository.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        if (admin.getEmail() == null)
            admin.setEmail("admin@example.com");
        admin.setRoles(Set.of(adminRole, reviewerRole, uploaderRole));
        admin.setActive(true);
        admin.setRegistrationStatus("APPROVED");
        userRepository.save(admin);
        log.info("Default Admin user ensured: admin");

        // Initialize Reviewer User
        User reviewer = userRepository.findByUsername("reviewer").orElseGet(User::new);
        reviewer.setUsername("reviewer");
        reviewer.setPassword(passwordEncoder.encode(reviewerPassword));
        if (reviewer.getEmail() == null)
            reviewer.setEmail("reviewer@example.com");
        reviewer.setRoles(Set.of(reviewerRole));
        reviewer.setActive(true);
        reviewer.setRegistrationStatus("APPROVED");
        userRepository.save(reviewer);
        log.info("Default Reviewer user ensured: reviewer");

        // Initialize Uploader User
        User uploader = userRepository.findByUsername("uploader").orElseGet(User::new);
        uploader.setUsername("uploader");
        uploader.setPassword(passwordEncoder.encode(uploaderPassword));
        if (uploader.getEmail() == null)
            uploader.setEmail("uploader@example.com");
        uploader.setRoles(Set.of(uploaderRole));
        uploader.setActive(true);
        uploader.setRegistrationStatus("APPROVED");
        userRepository.save(uploader);
        log.info("Default Uploader user ensured: uploader");
    }

    private Role createRoleIfNotFound(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }
}
