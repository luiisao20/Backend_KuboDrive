package com.luisdev.repository;

import com.luisdev.domain.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileShareRepository extends JpaRepository<FileShare, UUID> {
    Optional<FileShare> findByFileIdAndSharedWithId(UUID fileId, UUID sharedWithId);
    long countBySharedWithId(UUID sharedWithId);
}
