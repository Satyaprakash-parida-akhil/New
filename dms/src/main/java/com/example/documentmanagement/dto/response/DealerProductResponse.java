package com.example.documentmanagement.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DealerProductResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String imageUrl;

    private String status;
    private LocalDateTime createdAt;
}
