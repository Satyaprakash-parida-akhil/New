package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.DocumentResponse;
import com.example.documentmanagement.dto.response.PagedResponse;
import com.example.documentmanagement.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DocumentService documentService;

        @MockBean
        private MessageSource messageSource;

        @MockBean
        private com.example.documentmanagement.security.JwtUtil jwtUtil;

        @MockBean
        private com.example.documentmanagement.service.CustomUserDetailsService userDetailsService;

        @Test
        @WithMockUser(roles = "UPLOADER")
        void testGetAllDocuments() throws Exception {
                DocumentResponse doc = DocumentResponse.builder()
                                .id(1L)
                                .title("Test Doc")
                                .status("UPLOADED")
                                .build();

                PagedResponse<DocumentResponse> pagedResponse = PagedResponse.<DocumentResponse>builder()
                                .content(Collections.singletonList(doc))
                                .pageNumber(0)
                                .pageSize(10)
                                .totalElements(1)
                                .totalPages(1)
                                .last(true)
                                .build();

                when(documentService.getPagedDocuments(anyInt(), anyInt(), any(), any())).thenReturn(pagedResponse);
                when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Success");

                mockMvc.perform(get("/api/documents/paged")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content[0].title").value("Test Doc"));
        }
}
