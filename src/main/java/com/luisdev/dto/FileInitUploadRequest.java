package com.luisdev.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class FileInitUploadRequest {
  private UUID folderId; // can be null for root
  private String originalName;
  private Long sizeBytes;
  private String mimeType;
  private Boolean starred;
}
