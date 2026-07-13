package com.luisdev.repository;

import com.luisdev.domain.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileShareRepository extends JpaRepository<FileShare, UUID> {
    Optional<FileShare> findByFileIdAndSharedWithId(UUID fileId, UUID sharedWithId);
    long countBySharedWithId(UUID sharedWithId);
    List<FileShare> findBySharedWithId(UUID sharedWithId);
    
    @org.springframework.data.jpa.repository.Query("SELECT fs.file.id FROM FileShare fs WHERE fs.sharedWith.id = :userId")
    java.util.List<UUID> findSharedFileIdsByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT fs FROM FileShare fs WHERE fs.file.owner.id = :ownerId")
    List<FileShare> findByFileOwnerId(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId);
}
