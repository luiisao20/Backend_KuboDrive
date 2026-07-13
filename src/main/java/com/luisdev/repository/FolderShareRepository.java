package com.luisdev.repository;

import com.luisdev.domain.entity.FolderShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderShareRepository extends JpaRepository<FolderShare, UUID> {
    Optional<FolderShare> findByFolderIdAndSharedWithId(UUID folderId, UUID sharedWithId);
    List<FolderShare> findBySharedWithId(UUID sharedWithId);
    
    @Query("SELECT fs.folder.id FROM FolderShare fs WHERE fs.sharedWith.id = :userId")
    List<UUID> findSharedFolderIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT fs FROM FolderShare fs WHERE fs.folder.owner.id = :ownerId")
    List<FolderShare> findByFolderOwnerId(@Param("ownerId") UUID ownerId);
}
