package com.luisdev.dto;

import com.luisdev.domain.enums.FileStatus;
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
public class FileResponse {
    private UUID id;
    private String originalName;
    private Long sizeBytes;
    private String mimeType;
    private UUID folderId;
    private LocalDateTime createdAt;
    private FileStatus status;
    private Boolean starred;
}
