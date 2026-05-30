package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.dto.response.PagedResponse;
import com.example.documentmanagement.service.DocumentService;
import com.example.documentmanagement.util.MessageConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "Endpoints for managing documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;
    private final MessageSource messageSource;

    public DocumentController(DocumentService documentService, MessageSource messageSource) {
        this.documentService = documentService;
        this.messageSource = messageSource;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @Operation(summary = "Upload a new document")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) throws IOException, NoSuchAlgorithmException {

        DocumentResponse response = documentService.uploadDocument(file, title);
        String successMessage = messageSource.getMessage(MessageConstants.DOCUMENT_UPLOAD_SUCCESS, null,
                "Uploaded successfully", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage, response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('UPLOADER', 'REVIEWER', 'ADMIN')")
    @Operation(summary = "Get document metadata by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(@PathVariable Long id) {
        String successMessage = messageSource.getMessage(MessageConstants.DOCUMENT_RETRIEVED_SUCCESS, null,
                "Document retrieved successfully", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage, documentService.getDocumentById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('UPLOADER', 'REVIEWER', 'ADMIN')")
    @Operation(summary = "Get all documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAllDocuments() {
        String successMessage = messageSource.getMessage(MessageConstants.DOCUMENTS_RETRIEVED_SUCCESS, null,
                "Documents retrieved successfully", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(successMessage, documentService.getAllDocuments()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('UPLOADER', 'REVIEWER', 'ADMIN')")
    @Operation(summary = "Get all documents with pagination and filtering")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponse>>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Long reviewerId) {
        String successMessage = messageSource.getMessage(MessageConstants.DOCUMENTS_RETRIEVED_SUCCESS, null,
                "Documents retrieved successfully", LocaleContextHolder.getLocale());
        return ResponseEntity
                .ok(ApiResponse.success(successMessage,
                        documentService.getPagedDocuments(page, size, statuses, reviewerId)));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('UPLOADER', 'REVIEWER', 'ADMIN')")
    @Operation(summary = "Download or stream a document")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam(value = "token", required = false) String token) throws IOException {

        Resource resource = documentService.downloadDocument(id);
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(resource.getURI()));
        } catch (Exception ex) {
            // Default to application/octet-stream
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a document (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        documentService.softDeleteDocument(id);
        String msg = messageSource.getMessage("msg.document.delete.success", null,
                "Document soft deleted successfully", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(msg, null));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore a soft-deleted document (Admin only)")
    public ResponseEntity<ApiResponse<Void>> restoreDocument(@PathVariable Long id) {
        documentService.restoreDocument(id);
        String msg = messageSource.getMessage("msg.document.restore.success", null,
                "Document restored successfully", LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(msg, null));
    }
}
