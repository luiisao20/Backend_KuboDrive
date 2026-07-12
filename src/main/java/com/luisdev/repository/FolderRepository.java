package com.luisdev.repository;

import com.luisdev.domain.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByOwnerIdAndParentId(UUID ownerId, UUID parentId);
    List<Folder> findByOwnerIdAndParentIsNull(UUID ownerId);
    long countByOwnerIdAndStarredTrue(UUID ownerId);
    List<Folder> findByOwnerIdAndNameContainingIgnoreCase(UUID ownerId, String query);
}
