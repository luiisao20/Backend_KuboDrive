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
public class SharedByMeFolderResponse {
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime sharedAt;
    private String sharedWithEmail;
    private String sharedWithFirstName;
    private String sharedWithLastName;
}
