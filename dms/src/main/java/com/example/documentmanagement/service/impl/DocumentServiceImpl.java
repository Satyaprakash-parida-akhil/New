package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.dto.response.PagedResponse;
import com.example.documentmanagement.entity.Document;
import com.example.documentmanagement.entity.DocumentStatus;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.exception.ResourceNotFoundException;
import com.example.documentmanagement.mapper.DocumentMapper;
import com.example.documentmanagement.repository.DocumentRepository;
import com.example.documentmanagement.repository.ReviewRepository;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.AuditService;
import com.example.documentmanagement.service.DocumentService;
import com.example.documentmanagement.util.MessageConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final DocumentMapper documentMapper;
    private final MessageSource messageSource;
    private final ReviewRepository reviewRepository;

    @Value("${app.file.storage.location}")
    private String uploadDir;

    public DocumentServiceImpl(DocumentRepository documentRepository, UserRepository userRepository,
            AuditService auditService, DocumentMapper documentMapper, MessageSource messageSource,
            ReviewRepository reviewRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.documentMapper = documentMapper;
        this.messageSource = messageSource;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title)
            throws IOException, NoSuchAlgorithmException {
        if (file.isEmpty()) {
            String msg = messageSource.getMessage(MessageConstants.ERROR_FILE_EMPTY, null, "File cannot be empty",
                    LocaleContextHolder.getLocale());
            throw new IllegalArgumentException(msg);
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, file.getBytes());

        String checksum = calculateChecksum(file.getBytes());
        User currentUser = getCurrentUser();

        Document document = Document.builder()
                .title(title)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .checksum(checksum)
                .status(DocumentStatus.UPLOADED)
                .uploader(currentUser)
                .build();

        Document savedDoc = documentRepository.save(document);
        auditService.logAction("UPLOAD_DOCUMENT", "DOCUMENT", savedDoc.getId(), "Uploaded " + title);

        return documentMapper.toResponse(savedDoc);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> {
                    String msg = messageSource.getMessage(MessageConstants.DOCUMENT_NOT_FOUND, null,
                            "Document not found", LocaleContextHolder.getLocale());
                    return new ResourceNotFoundException(msg);
                });
        return documentMapper.toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentResponse> getPagedDocuments(int page, int size, List<String> statuses,
            Long reviewerId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage;

        List<DocumentStatus> enumStatuses = (statuses != null && !statuses.isEmpty())
                ? statuses.stream().map(DocumentStatus::valueOf).toList()
                : null;

        if (enumStatuses != null && reviewerId != null) {
            documentPage = documentRepository.findByStatusInAndReviewerId(enumStatuses, reviewerId, pageable);
        } else if (enumStatuses != null) {
            documentPage = documentRepository.findByStatusIn(enumStatuses, pageable);
        } else {
            // Default: Exclude SOFT_DELETED
            List<DocumentStatus> activeStatuses = List.of(
                    DocumentStatus.UPLOADED,
                    DocumentStatus.IN_REVIEW,
                    DocumentStatus.APPROVED,
                    DocumentStatus.REJECTED);
            documentPage = documentRepository.findByStatusIn(activeStatuses, pageable);
        }

        List<DocumentResponse> content = documentPage.getContent().stream()
                .map(doc -> {
                    if (doc.getReviewer() == null && doc.getStatus() == DocumentStatus.IN_REVIEW) {
                        reviewRepository.findFirstByDocumentIdOrderByCreatedAtDesc(doc.getId())
                                .ifPresent(r -> doc.setReviewer(r.getReviewer()));
                    }
                    return documentMapper.toResponse(doc);
                })
                .toList();

        return PagedResponse.<DocumentResponse>builder()
                .content(content)
                .pageNumber(documentPage.getNumber())
                .pageSize(documentPage.getSize())
                .totalElements(documentPage.getTotalElements())
                .totalPages(documentPage.getTotalPages())
                .last(documentPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> {
                    String msg = messageSource.getMessage(MessageConstants.DOCUMENT_NOT_FOUND, null,
                            "Document not found", LocaleContextHolder.getLocale());
                    return new ResourceNotFoundException(msg);
                });
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Could not read file: " + doc.getFilePath());
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("Error reading file: " + doc.getFilePath());
        }
    }

    @Override
    @Transactional
    public void softDeleteDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        doc.setStatus(DocumentStatus.SOFT_DELETED);
        documentRepository.save(doc);
        auditService.logAction("DELETE_DOCUMENT", "DOCUMENT", id, "Soft deleted document: " + doc.getTitle());
    }

    @Override
    @Transactional
    public void restoreDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (doc.getStatus() == DocumentStatus.SOFT_DELETED) {
            doc.setStatus(DocumentStatus.UPLOADED); // Back to pending/uploaded
            documentRepository.save(doc);
            auditService.logAction("RESTORE_DOCUMENT", "DOCUMENT", id, "Restored document: " + doc.getTitle());
        }
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
                    String msg = messageSource.getMessage(MessageConstants.USER_NOT_FOUND, null, "User not found",
                            LocaleContextHolder.getLocale());
                    return new ResourceNotFoundException(msg);
                });
    }

    private String calculateChecksum(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(data);
        StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
