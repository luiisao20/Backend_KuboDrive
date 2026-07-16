package com.luisdev.domain.entity;

import com.luisdev.domain.enums.FileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String originalName;

  @Column(nullable = false, unique = true)
  private String minioObjectId;

  @Column(nullable = false)
  private Long sizeBytes;

  @Column(nullable = false)
  private String mimeType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "folder_id")
  private Folder folder;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(updatable = false, columnDefinition = "timestamptz")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private FileStatus status = FileStatus.PENDING;

  @Column(nullable = false, columnDefinition = "boolean default false")
  @Builder.Default
  private Boolean starred = false;

  @Column(name = "upload_id")
  private String uploadId;
}
