package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.FileRecordDto;
import com.example.documentmanagement.service.FileStorageService;
import com.example.documentmanagement.util.MessageConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "File Management", description = "Endpoints for file upload and CRUD operations")
public class FileController {

    private final FileStorageService fileStorageService;

    @Autowired
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @Operation(summary = "Upload a new file")
    public ResponseEntity<ApiResponse<FileRecordDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String uploadedBy = userDetails != null ? userDetails.getUsername() : "anonymous";
        FileRecordDto fileRecord = fileStorageService.storeFile(file, uploadedBy);
        
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_UPLOADED, fileRecord));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file details by ID")
    public ResponseEntity<ApiResponse<FileRecordDto>> getFile(@PathVariable Long id) {
        FileRecordDto fileRecord = fileStorageService.getFile(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_RETRIEVED, fileRecord));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing file")
    public ResponseEntity<ApiResponse<FileRecordDto>> replaceFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String uploadedBy = userDetails != null ? userDetails.getUsername() : "anonymous";
        FileRecordDto updatedRecord = fileStorageService.replaceFile(id, file, uploadedBy);
        
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_REPLACED, updatedRecord));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long id) {
        fileStorageService.softDeleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_DELETED));
    }

    @PostMapping("/{id}/recover")
    @Operation(summary = "Recover a soft-deleted file")
    public ResponseEntity<ApiResponse<Void>> recoverFile(@PathVariable Long id) {
        fileStorageService.recoverFile(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_RECOVERED));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Permanently delete a file")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteFile(@PathVariable Long id) {
        fileStorageService.permanentlyDeleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.FILE_PERMANENTLY_DELETED));
    }
}
