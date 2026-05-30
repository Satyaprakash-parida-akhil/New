package com.example.documentmanagement.mapper;

import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.entity.Document;
import com.example.documentmanagement.entity.DocumentStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    /**
     * Maps a Document entity to a DocumentResponse DTO.
     */
    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .uploaderUsername(document.getUploader() != null ? document.getUploader().getUsername() : null)
                .reviewerId(document.getReviewer() != null ? document.getReviewer().getId() : null)
                .reviewerUsername(document.getReviewer() != null ? document.getReviewer().getUsername() : null)
                .comments(document.getReviewerComments())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * Maps a DocumentResponse DTO back to a Document entity (partial — no
     * uploader/checksum/filePath).
     * Primarily useful for update scenarios where only mutable fields need syncing.
     */
    public Document toEntity(DocumentResponse response) {
        if (response == null) {
            return null;
        }

        return Document.builder()
                .id(response.getId())
                .title(response.getTitle())
                .fileSize(response.getFileSize())
                .fileType(response.getFileType())
                .status(response.getStatus() != null ? DocumentStatus.valueOf(response.getStatus()) : null)
                .build();
    }
}
