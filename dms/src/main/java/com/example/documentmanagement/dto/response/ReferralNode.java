package com.example.documentmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralNode {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String paymentStatus;

    @JsonProperty("isActive")
    private boolean isActive;

    private String role;
    private String createdAt;

    private int level;
    private List<ReferralNode> children;
    private int totalDownlineCount;
    private int referralCount;
    private String referralCode;
    private java.time.LocalDateTime joinedDate;

    private String referredByName;
    private String referredByCode;
}
