package com.example.documentmanagement.service;

public interface EmailService {
    void sendOtp(String to, String otp);

    void sendApprovalEmail(String to, String role);

    void sendRejectionEmail(String to, String reason);

    void sendDeletionEmail(String to);
}
