package com.example.documentmanagement.repository;

import com.example.documentmanagement.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmailAndType(String email, String type);

    void deleteByEmailAndType(String email, String type);
}
