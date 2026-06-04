package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.otp.debug:false}")
    private boolean otpDebug;

    @Override
    public void sendOtp(String to, String otp) {
        log.info("Sending OTP email to {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your DMS Registration OTP");
        message.setText("Your OTP for registration is: " + otp + ". This OTP is valid for 10 minutes.");

        if (otpDebug) {
            log.info("DEBUG MODE: OTP for {} is {}", to, otp);
        }

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            if (!otpDebug) {
                throw new RuntimeException("Could not send OTP email. Please try again later.");
            }
        }
    }

    @Override
    public void sendApprovalEmail(String to, String role) {
        log.info("Sending Approval email to {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("DMS Account Approved");
        message.setText(
                "Congratulations! Your account has been approved. You can now log in with the assigned role: " + role);

        try {
            mailSender.send(message);
            log.info("Approval email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", to, e.getMessage());
            if (otpDebug) {
                log.warn("DEBUG MODE: Approval email failed but registration was updated in database.");
            } else {
                throw new RuntimeException("Account approved, but notification email failed to send.");
            }
        }
    }

    @Override
    public void sendRejectionEmail(String to, String reason) {
        log.info("Sending Rejection email to {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("DMS Application Update");
        message.setText("We regret to inform you that your application has been rejected.\nReason: "
                + (reason != null ? reason : "Submission does not meet our requirements."));

        try {
            mailSender.send(message);
            log.info("Rejection email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send rejection email to {}: {}", to, e.getMessage());
            if (!otpDebug) {
                log.warn("Non-critical: Rejection email failed to send.");
            }
        }
    }

    @Override
    public void sendDeletionEmail(String to) {
        log.info("Sending Deletion email to {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("DMS Account Status Update");
        message.setText(
                "Your registration request has been deleted from our system. If this was not expected, you may reapply through the registration portal.");

        try {
            mailSender.send(message);
            log.info("Deletion email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send deletion email to {}: {}", to, e.getMessage());
        }
    }
}
