package com.luisdev.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipartInitUploadResponse {
  private UUID fileId;
  private String uploadId;
  private Long partSizeBytes;
  private Integer totalParts;
  private List<PartUploadUrl> partUrls;
}
