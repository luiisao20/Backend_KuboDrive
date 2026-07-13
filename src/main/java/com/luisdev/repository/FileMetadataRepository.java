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
    List<FileMetadata> findByOwnerIdAndOriginalNameContainingIgnoreCase(UUID ownerId, String query);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.owner.id = :ownerId")
    Long sumSizeBytesByOwnerId(@org.springframework.data.repository.query.Param("ownerId") java.util.UUID ownerId);
}
