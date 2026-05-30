package com.example.documentmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String title;
    private Long fileSize;
    private String fileType;
    private String status;
    private String uploaderUsername;
    private Long reviewerId;
    private String reviewerUsername;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
