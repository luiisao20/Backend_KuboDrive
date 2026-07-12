package com.luisdev.repository;

import com.luisdev.domain.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByOwnerIdAndFolderId(UUID ownerId, UUID folderId);
    List<FileMetadata> findByOwnerIdAndFolderIsNull(UUID ownerId);
    long countByOwnerId(UUID ownerId);
    long countByOwnerIdAndStarredTrue(UUID ownerId);
}
