package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.request.ReviewRequest;
import com.example.documentmanagement.entity.Document;
import com.example.documentmanagement.entity.DocumentStatus;
import com.example.documentmanagement.entity.Review;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.DocumentRepository;
import com.example.documentmanagement.repository.ReviewRepository;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.AuditService;
import com.example.documentmanagement.service.ReviewService;
import com.example.documentmanagement.util.MessageConstants;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.documentmanagement.exception.ResourceNotFoundException;
import com.example.documentmanagement.exception.InvalidDocumentStateException;

import java.time.LocalDateTime;

@Service
public class ReviewServiceImpl implements ReviewService {

        private final ReviewRepository reviewRepository;
        private final DocumentRepository documentRepository;
        private final UserRepository userRepository;
        private final AuditService auditService;
        private final MessageSource messageSource;

        public ReviewServiceImpl(ReviewRepository reviewRepository, DocumentRepository documentRepository,
                        UserRepository userRepository, AuditService auditService, MessageSource messageSource) {
                this.reviewRepository = reviewRepository;
                this.documentRepository = documentRepository;
                this.userRepository = userRepository;
                this.auditService = auditService;
                this.messageSource = messageSource;
        }

        @Override
        @Transactional
        public void assignReviewer(Long documentId, Long reviewerId) {
                Document document = documentRepository.findById(documentId)
                                .orElseThrow(() -> {
                                        String msg = messageSource.getMessage(MessageConstants.DOCUMENT_NOT_FOUND, null,
                                                        "Document not found", LocaleContextHolder.getLocale());
                                        return new ResourceNotFoundException(msg);
                                });

                User reviewer = userRepository.findById(reviewerId)
                                .orElseThrow(() -> {
                                        String msg = messageSource.getMessage(MessageConstants.USER_NOT_FOUND, null,
                                                        "User not found",
                                                        LocaleContextHolder.getLocale());
                                        return new ResourceNotFoundException(msg);
                                });

                document.setStatus(DocumentStatus.IN_REVIEW);
                document.setReviewer(reviewer);
                documentRepository.save(document);

                Review review = Review.builder()
                                .document(document)
                                .reviewer(reviewer)
                                .action("ASSIGNED")
                                .build();

                reviewRepository.save(review);
                auditService.logAction("ASSIGN_REVIEWER", "DOCUMENT", documentId,
                                "Assigned to " + reviewer.getUsername());
        }

        @Override
        @Transactional
        public void reviewDocument(Long documentId, ReviewRequest request) {
                Document document = documentRepository.findById(documentId)
                                .orElseThrow(() -> {
                                        String msg = messageSource.getMessage(MessageConstants.DOCUMENT_NOT_FOUND, null,
                                                        "Document not found", LocaleContextHolder.getLocale());
                                        return new ResourceNotFoundException(msg);
                                });

                if (document.getStatus() != DocumentStatus.IN_REVIEW
                                && document.getStatus() != DocumentStatus.UPLOADED) {
                        String msg = messageSource.getMessage(MessageConstants.ERROR_NOT_IN_REVIEW, null,
                                        "Not in IN_REVIEW or UPLOADED status", LocaleContextHolder.getLocale());
                        throw new InvalidDocumentStateException(msg);
                }

                User currentUser = getCurrentUser();

                DocumentStatus newStatus = request.getAction().equalsIgnoreCase("APPROVED") ? DocumentStatus.APPROVED
                                : DocumentStatus.REJECTED;
                document.setStatus(newStatus);
                document.setReviewerComments(request.getComments());
                documentRepository.save(document);

                Review review = Review.builder()
                                .document(document)
                                .reviewer(currentUser)
                                .action(newStatus.name())
                                .comments(request.getComments())
                                .completedAt(LocalDateTime.now())
                                .build();

                reviewRepository.save(review);
                auditService.logAction("REVIEW_DOCUMENT", "DOCUMENT", documentId, "Action: " + newStatus);
        }

        private User getCurrentUser() {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                String username;
                if (principal instanceof UserDetails userDetails) {
                        username = userDetails.getUsername();
                } else {
                        username = principal.toString();
                }
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> {
                                        String msg = messageSource.getMessage(MessageConstants.USER_NOT_FOUND, null,
                                                        "User not found",
                                                        LocaleContextHolder.getLocale());
                                        return new ResourceNotFoundException(msg);
                                });
        }
}
