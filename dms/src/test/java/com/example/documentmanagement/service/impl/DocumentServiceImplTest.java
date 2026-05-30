package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.entity.Document;
import com.example.documentmanagement.entity.DocumentStatus;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.mapper.DocumentMapper;
import com.example.documentmanagement.repository.DocumentRepository;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.AuditService;
import com.example.documentmanagement.repository.ReviewRepository;
import com.example.documentmanagement.util.MessageConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {
    private static final String TEST_DOC = "Test Doc";
    private static final String UPLOAD_ACTION = "UPLOAD_DOCUMENT";
    private static final String UPLOADER_USER = "uploader_user";

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private MessageSource messageSource;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private User uploader;
    private Document document;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "uploadDir", "target/test-uploads");

        uploader = new User();
        uploader.setId(1L);
        uploader.setUsername(UPLOADER_USER);

        document = new Document();
        document.setId(100L);
        document.setTitle(TEST_DOC);
        document.setStatus(DocumentStatus.UPLOADED);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(UPLOADER_USER)
                .password("password")
                .roles("UPLOADER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void uploadDocumentSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());

        when(userRepository.findByUsername(UPLOADER_USER)).thenReturn(Optional.of(uploader));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        DocumentResponse docResponse = DocumentResponse.builder().id(100L).title(TEST_DOC).build();
        when(documentMapper.toResponse(document)).thenReturn(docResponse);

        DocumentResponse response = documentService.uploadDocument(file, TEST_DOC);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(TEST_DOC, response.getTitle());

        verify(documentRepository, times(1)).save(any(Document.class));
        verify(auditService, times(1)).logAction(eq(UPLOAD_ACTION), eq("DOCUMENT"), eq(100L), anyString());
    }

    @Test
    void uploadDocumentEmptyFileThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        when(messageSource.getMessage(eq(MessageConstants.ERROR_FILE_EMPTY), any(), anyString(), any()))
                .thenReturn("File cannot be empty");

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> documentService.uploadDocument(file, TEST_DOC));

        assertEquals("File cannot be empty", exception.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
    }
}
