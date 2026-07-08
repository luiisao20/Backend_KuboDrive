package com.luisdev.service;

import com.luisdev.domain.entity.FileMetadata;
import com.luisdev.domain.entity.FileShare;
import com.luisdev.domain.entity.Folder;
import com.luisdev.domain.entity.User;
import com.luisdev.domain.enums.FilePermission;
import com.luisdev.domain.enums.FileStatus;
import com.luisdev.dto.FileInitUploadRequest;
import com.luisdev.dto.FileInitUploadResponse;
import com.luisdev.repository.FileMetadataRepository;
import com.luisdev.repository.FileShareRepository;
import com.luisdev.repository.FolderRepository;
import com.luisdev.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FileShareRepository fileShareRepository;
    private final MinioService minioService;

    public FileService(FileMetadataRepository fileMetadataRepository,
                       FolderRepository folderRepository,
                       UserRepository userRepository,
                       FileShareRepository fileShareRepository,
                       MinioService minioService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.fileShareRepository = fileShareRepository;
        this.minioService = minioService;
    }

    @Transactional
    public FileInitUploadResponse initiateUpload(FileInitUploadRequest request, UUID userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Folder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder not found"));
            
            if (!folder.getOwner().getId().equals(userId)) {
                throw new SecurityException("User does not have access to this folder");
            }
        }

        String minioObjectId = UUID.randomUUID().toString();

        FileMetadata fileMetadata = FileMetadata.builder()
                .originalName(request.getOriginalName())
                .minioObjectId(minioObjectId)
                .sizeBytes(request.getSizeBytes())
                .mimeType(request.getMimeType())
                .folder(folder)
                .owner(owner)
                .status(FileStatus.PENDING)
                .build();

        fileMetadata = fileMetadataRepository.save(fileMetadata);

        String uploadUrl = minioService.generatePresignedUploadUrl(minioObjectId);

        return FileInitUploadResponse.builder()
                .fileId(fileMetadata.getId())
                .uploadUrl(uploadUrl)
                .build();
    }

    @Transactional
    public void confirmUpload(UUID fileId, UUID userId) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(userId)) {
            throw new SecurityException("User does not own this file");
        }

        file.setStatus(FileStatus.UPLOADED);
        fileMetadataRepository.save(file);
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID fileId, UUID userId) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found"));

        boolean isOwner = file.getOwner().getId().equals(userId);
        
        if (!isOwner) {
            fileShareRepository.findByFileIdAndSharedWithId(fileId, userId)
                    .orElseThrow(() -> new SecurityException("User does not have access to download this file"));
        }

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new IllegalStateException("File is not fully uploaded yet");
        }

        return minioService.generatePresignedDownloadUrl(file.getMinioObjectId(), file.getOriginalName());
    }

    @Transactional
    public void shareFile(UUID fileId, String targetUserEmail, UUID ownerUserId) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(ownerUserId)) {
            throw new SecurityException("User does not own this file");
        }

        User targetUser = userRepository.findByEmail(targetUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

        if (targetUser.getId().equals(ownerUserId)) {
            throw new IllegalArgumentException("Cannot share file with yourself");
        }

        fileShareRepository.findByFileIdAndSharedWithId(fileId, targetUser.getId())
                .ifPresentOrElse(
                        share -> { /* Already shared */ },
                        () -> {
                            FileShare share = FileShare.builder()
                                    .file(file)
                                    .sharedWith(targetUser)
                                    .permissions(FilePermission.READ)
                                    .build();
                            fileShareRepository.save(share);
                        }
                );
    }

    @Transactional
    public Folder createFolder(String name, UUID parentId, UUID userId) {
        User owner = userRepository.findById(userId).orElseThrow();
        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findById(parentId).orElseThrow();
            if (!parent.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
        }
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParent(parent);
        folder.setOwner(owner);
        return folderRepository.save(folder);
    }

    @Transactional
    public void renameFolder(UUID folderId, String newName, UUID userId) {
        Folder folder = folderRepository.findById(folderId).orElseThrow();
        if (!folder.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
        folder.setName(newName);
        folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(UUID folderId, UUID userId) {
        Folder folder = folderRepository.findById(folderId).orElseThrow();
        if (!folder.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
        folderRepository.delete(folder);
    }

    @Transactional
    public void renameFile(UUID fileId, String newName, UUID userId) {
        FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
        if (!file.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
        file.setOriginalName(newName);
        fileMetadataRepository.save(file);
    }

    @Transactional
    public void deleteFile(UUID fileId, UUID userId) {
        FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
        if (!file.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
        fileMetadataRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> listContents(UUID folderId, UUID userId) {
        java.util.List<Folder> folders;
        java.util.List<FileMetadata> files;
        if (folderId == null) {
            folders = folderRepository.findByOwnerIdAndParentIsNull(userId);
            files = fileMetadataRepository.findByOwnerIdAndFolderIsNull(userId);
        } else {
            Folder folder = folderRepository.findById(folderId).orElseThrow();
            if (!folder.getOwner().getId().equals(userId)) throw new SecurityException("Acceso denegado");
            folders = folderRepository.findByOwnerIdAndParentId(userId, folderId);
            files = fileMetadataRepository.findByOwnerIdAndFolderId(userId, folderId);
        }
        java.util.Map<String, Object> contents = new java.util.HashMap<>();
        contents.put("folders", folders);
        contents.put("files", files);
        return contents;
    }
}
