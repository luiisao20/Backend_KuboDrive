package com.luisdev.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedFileResponse {
    private UUID id;
    private String originalName;
    private Long sizeBytes;
    private String mimeType;
    private LocalDateTime createdAt;
    private LocalDateTime sharedAt;
    private String sharedByEmail;
    private String sharedByFirstName;
    private String sharedByLastName;
}
