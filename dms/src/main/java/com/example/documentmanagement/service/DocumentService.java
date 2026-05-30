package com.example.documentmanagement.service;

import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.dto.response.PagedResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, String title) throws IOException, NoSuchAlgorithmException;

    DocumentResponse getDocumentById(Long id);

    List<DocumentResponse> getAllDocuments();

    PagedResponse<DocumentResponse> getPagedDocuments(int page, int size, List<String> statuses, Long reviewerId);

    Resource downloadDocument(Long id);

    void softDeleteDocument(Long id);

    void restoreDocument(Long id);
}
