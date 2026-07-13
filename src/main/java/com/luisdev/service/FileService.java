package com.luisdev.service;

import com.luisdev.domain.entity.FileMetadata;
import com.luisdev.domain.entity.FileShare;
import com.luisdev.domain.entity.Folder;
import com.luisdev.domain.entity.FolderShare;
import com.luisdev.domain.entity.User;
import com.luisdev.domain.enums.FilePermission;
import com.luisdev.domain.enums.FileStatus;
import com.luisdev.dto.FileInitUploadRequest;
import com.luisdev.dto.FileInitUploadResponse;
import com.luisdev.dto.FileResponse;
import com.luisdev.dto.FolderResponse;
import com.luisdev.dto.SharedFileResponse;
import com.luisdev.repository.FileMetadataRepository;
import com.luisdev.repository.FileShareRepository;
import com.luisdev.repository.FolderRepository;
import com.luisdev.repository.FolderShareRepository;
import com.luisdev.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

  private final FileMetadataRepository fileMetadataRepository;
  private final FolderRepository folderRepository;
  private final UserRepository userRepository;
  private final FileShareRepository fileShareRepository;
  private final FolderShareRepository folderShareRepository;
  private final MinioService minioService;
  private final HistoryService historyService;

  public FileService(FileMetadataRepository fileMetadataRepository,
      FolderRepository folderRepository,
      UserRepository userRepository,
      FileShareRepository fileShareRepository,
      FolderShareRepository folderShareRepository,
      MinioService minioService,
      HistoryService historyService) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.folderRepository = folderRepository;
    this.userRepository = userRepository;
    this.fileShareRepository = fileShareRepository;
    this.folderShareRepository = folderShareRepository;
    this.minioService = minioService;
    this.historyService = historyService;
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
      
      if (fileMetadataRepository.existsByOwnerIdAndFolderIdAndOriginalName(userId, folder.getId(), request.getOriginalName())) {
          throw new IllegalArgumentException("Archivo existente");
      }
    } else {
      if (fileMetadataRepository.existsByOwnerIdAndFolderIsNullAndOriginalName(userId, request.getOriginalName())) {
          throw new IllegalArgumentException("Archivo existente");
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
        .starred(request.getStarred() != null ? request.getStarred() : false)
        .build();

    fileMetadata = fileMetadataRepository.save(fileMetadata);

    historyService.recordHistory(owner, "UPLOAD", "FILE", request.getOriginalName());
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
            share -> {
              /* Already shared */ },
            () -> {
              FileShare share = FileShare.builder()
                  .file(file)
                  .sharedWith(targetUser)
                  .permissions(FilePermission.READ)
                  .build();
              fileShareRepository.save(share);
              historyService.recordHistory(file.getOwner(), "SHARE", "FILE", file.getOriginalName());
            });
  }

  @Transactional
  public void shareFolder(UUID folderId, String targetUserEmail, UUID ownerUserId) {
    Folder folder = folderRepository.findById(folderId)
        .orElseThrow(() -> new EntityNotFoundException("Folder not found"));
    if (!folder.getOwner().getId().equals(ownerUserId)) {
      throw new SecurityException("User does not own this folder");
    }
    User targetUser = userRepository.findByEmail(targetUserEmail)
        .orElseThrow(() -> new EntityNotFoundException("Target user not found"));
    if (targetUser.getId().equals(ownerUserId)) {
      throw new IllegalArgumentException("Cannot share folder with yourself");
    }
    shareFolderRecursively(folder, targetUser);
    historyService.recordHistory(folder.getOwner(), "SHARE", "FOLDER", folder.getName());
  }

  private void shareFolderRecursively(Folder folder, User targetUser) {
    folderShareRepository.findByFolderIdAndSharedWithId(folder.getId(), targetUser.getId())
        .ifPresentOrElse(share -> {
        }, () -> {
          FolderShare share = FolderShare.builder()
              .folder(folder)
              .sharedWith(targetUser)
              .permissions(FilePermission.READ)
              .build();
          folderShareRepository.save(share);
        });

    List<FileMetadata> files = fileMetadataRepository.findByOwnerIdAndFolderId(folder.getOwner().getId(),
        folder.getId());
    for (FileMetadata file : files) {
      fileShareRepository.findByFileIdAndSharedWithId(file.getId(), targetUser.getId())
          .ifPresentOrElse(share -> {
          }, () -> {
            FileShare share = FileShare.builder()
                .file(file)
                .sharedWith(targetUser)
                .permissions(FilePermission.READ)
                .build();
            fileShareRepository.save(share);
          });
    }

    List<Folder> subfolders = folderRepository.findByOwnerIdAndParentId(folder.getOwner().getId(), folder.getId());
    for (Folder subfolder : subfolders) {
      shareFolderRecursively(subfolder, targetUser);
    }
  }

  @Transactional
  public FolderResponse createFolder(String name, UUID parentId, Boolean starred, UUID userId) {
    User owner = userRepository.findById(userId).orElseThrow();
    Folder parent = null;
    if (parentId != null) {
      parent = folderRepository.findById(parentId).orElseThrow();
      if (!parent.getOwner().getId().equals(userId))
        throw new SecurityException("Acceso denegado");
        
      if (folderRepository.existsByOwnerIdAndParentIdAndName(userId, parentId, name)) {
          throw new IllegalArgumentException("Carpeta existente");
      }
    } else {
      if (folderRepository.existsByOwnerIdAndParentIsNullAndName(userId, name)) {
          throw new IllegalArgumentException("Carpeta existente");
      }
    }
    Folder folder = new Folder();
    folder.setName(name);
    folder.setParent(parent);
    folder.setOwner(owner);
    folder.setStarred(starred != null ? starred : false);

    Folder savedFolder = folderRepository.save(folder);
    historyService.recordHistory(owner, "CREATE", "FOLDER", name);
    return FolderResponse.builder()
        .id(savedFolder.getId())
        .name(savedFolder.getName())
        .parentId(savedFolder.getParent() != null ? savedFolder.getParent().getId() : null)
        .createdAt(savedFolder.getCreatedAt())
        .starred(savedFolder.getStarred())
        .itemsCount(0L)
        .build();
  }

  @Transactional
  public void renameFolder(UUID folderId, String newName, UUID userId) {
    Folder folder = folderRepository.findById(folderId).orElseThrow();
    if (!folder.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");
      
    boolean exists = folder.getParent() != null 
        ? folderRepository.existsByOwnerIdAndParentIdAndName(userId, folder.getParent().getId(), newName)
        : folderRepository.existsByOwnerIdAndParentIsNullAndName(userId, newName);
        
    if (exists && !folder.getName().equals(newName)) {
        throw new IllegalArgumentException("Carpeta existente");
    }
    
    folder.setName(newName);
    folderRepository.save(folder);
    historyService.recordHistory(folder.getOwner(), "RENAME", "FOLDER", newName);
  }

  @Transactional
  public void deleteFolder(UUID folderId, UUID userId) {
    Folder folder = folderRepository.findById(folderId).orElseThrow();
    if (!folder.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");

    String folderName = folder.getName();
    User owner = folder.getOwner();
    deleteFolderRecursively(folder, userId);
    historyService.recordHistory(owner, "DELETE", "FOLDER", folderName);
  }

  private void deleteFolderRecursively(Folder folder, UUID userId) {
    // 1. Encontrar y borrar físicamente todos los archivos en esta carpeta
    List<FileMetadata> files = fileMetadataRepository.findByOwnerIdAndFolderId(userId, folder.getId());
    for (FileMetadata file : files) {
      minioService.deleteFileFromMinio(file.getMinioObjectId());
      fileMetadataRepository.delete(file);
    }

    // 2. Encontrar y borrar recursivamente todas las subcarpetas
    List<Folder> subfolders = folderRepository.findByOwnerIdAndParentId(userId, folder.getId());
    for (Folder subfolder : subfolders) {
      deleteFolderRecursively(subfolder, userId);
    }

    // 3. Finalmente, borrar la carpeta actual
    folderRepository.delete(folder);
  }

  @Transactional
  public void updateFileStarred(UUID fileId, Boolean starred, UUID userId) {
    FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
    if (!file.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");
    file.setStarred(starred != null ? starred : false);
    fileMetadataRepository.save(file);
  }

  @Transactional
  public void updateFolderStarred(UUID folderId, Boolean starred, UUID userId) {
    Folder folder = folderRepository.findById(folderId).orElseThrow();
    if (!folder.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");
    folder.setStarred(starred != null ? starred : false);
    folderRepository.save(folder);
  }

  @Transactional
  public void renameFile(UUID fileId, String newName, UUID userId) {
    FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
    if (!file.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");
      
    boolean exists = file.getFolder() != null 
        ? fileMetadataRepository.existsByOwnerIdAndFolderIdAndOriginalName(userId, file.getFolder().getId(), newName)
        : fileMetadataRepository.existsByOwnerIdAndFolderIsNullAndOriginalName(userId, newName);
        
    if (exists && !file.getOriginalName().equals(newName)) {
        throw new IllegalArgumentException("Archivo existente");
    }
    
    file.setOriginalName(newName);
    fileMetadataRepository.save(file);
    historyService.recordHistory(file.getOwner(), "RENAME", "FILE", newName);
  }

  @Transactional
  public void deleteFile(UUID fileId, UUID userId) {
    FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
    if (!file.getOwner().getId().equals(userId))
      throw new SecurityException("Acceso denegado");

    minioService.deleteFileFromMinio(file.getMinioObjectId());
    fileMetadataRepository.delete(file);
    historyService.recordHistory(file.getOwner(), "DELETE", "FILE", file.getOriginalName());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listContents(UUID folderId, UUID userId, Boolean starred, String name) {
    List<Folder> folders;
    List<FileMetadata> files;
    Folder currentFolder = null;

    if (folderId == null) {
      if (Boolean.TRUE.equals(starred) && name != null && !name.isEmpty()) {
        folders = folderRepository.findByOwnerIdAndStarredTrueAndNameContainingIgnoreCase(userId, name);
        files = fileMetadataRepository.findByOwnerIdAndStarredTrueAndOriginalNameContainingIgnoreCase(userId, name);
      } else if (Boolean.TRUE.equals(starred)) {
        folders = folderRepository.findByOwnerIdAndStarredTrue(userId);
        files = fileMetadataRepository.findByOwnerIdAndStarredTrue(userId);
      } else if (name != null && !name.isEmpty()) {
        folders = folderRepository.findByOwnerIdAndNameContainingIgnoreCase(userId, name);
        files = fileMetadataRepository.findByOwnerIdAndOriginalNameContainingIgnoreCase(userId, name);
      } else {
        folders = folderRepository.findByOwnerIdAndParentIsNull(userId);
        files = fileMetadataRepository.findByOwnerIdAndFolderIsNull(userId);
      }
    } else {
      currentFolder = folderRepository.findById(folderId).orElseThrow();
      boolean isOwner = currentFolder.getOwner().getId().equals(userId);
      boolean isSharedWithMe = false;

      if (!isOwner) {
        isSharedWithMe = folderShareRepository.findByFolderIdAndSharedWithId(folderId, userId).isPresent();
        if (!isSharedWithMe) {
          throw new SecurityException("Acceso denegado");
        }
      }

      UUID originalOwnerId = currentFolder.getOwner().getId();
      List<Folder> allFolders = folderRepository.findByOwnerIdAndParentId(originalOwnerId, folderId);
      List<FileMetadata> allFiles = fileMetadataRepository.findByOwnerIdAndFolderId(originalOwnerId, folderId);

      if (Boolean.TRUE.equals(starred)) {
        allFolders = allFolders.stream().filter(Folder::getStarred).collect(Collectors.toList());
        allFiles = allFiles.stream().filter(FileMetadata::getStarred).collect(Collectors.toList());
      }
      if (name != null && !name.isEmpty()) {
        String lowerName = name.toLowerCase();
        allFolders = allFolders.stream().filter(f -> f.getName().toLowerCase().contains(lowerName))
            .collect(Collectors.toList());
        allFiles = allFiles.stream().filter(f -> f.getOriginalName().toLowerCase().contains(lowerName))
            .collect(Collectors.toList());
      }

      if (isOwner) {
        folders = allFolders;
        files = allFiles;
      } else {
        List<UUID> sharedFolderIds = folderShareRepository.findSharedFolderIdsByUserId(userId);
        List<UUID> sharedFileIds = fileShareRepository.findSharedFileIdsByUserId(userId);

        folders = allFolders.stream()
            .filter(f -> sharedFolderIds.contains(f.getId()))
            .collect(Collectors.toList());
        files = allFiles.stream()
            .filter(f -> sharedFileIds.contains(f.getId()))
            .collect(Collectors.toList());
      }
    }

    List<FolderResponse> folderResponses = folders.stream().map(f -> FolderResponse.builder()
        .id(f.getId())
        .name(f.getName())
        .parentId(f.getParent() != null ? f.getParent().getId() : null)
        .createdAt(f.getCreatedAt())
        .starred(f.getStarred())
        .itemsCount(folderRepository.countByParentId(f.getId()) + fileMetadataRepository.countByFolderId(f.getId()))
        .build()).collect(Collectors.toList());

    List<FileResponse> fileResponses = files.stream().map(f -> FileResponse.builder()
        .id(f.getId())
        .originalName(f.getOriginalName())
        .sizeBytes(f.getSizeBytes())
        .mimeType(f.getMimeType())
        .folderId(f.getFolder() != null ? f.getFolder().getId() : null)
        .createdAt(f.getCreatedAt())
        .status(f.getStatus())
        .starred(f.getStarred())
        .build()).collect(Collectors.toList());

    Map<String, Object> contents = new HashMap<>();
    contents.put("folders", folderResponses);
    contents.put("files", fileResponses);

    if (currentFolder != null) {
      contents.put("currentFolder", FolderResponse.builder()
          .id(currentFolder.getId())
          .name(currentFolder.getName())
          .parentId(currentFolder.getParent() != null ? currentFolder.getParent().getId() : null)
          .createdAt(currentFolder.getCreatedAt())
          .starred(currentFolder.getStarred())
          .itemsCount(folderRepository.countByParentId(currentFolder.getId()) + fileMetadataRepository.countByFolderId(currentFolder.getId()))
          .build());
    } else {
      contents.put("currentFolder", null);
    }

    return contents;
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getStats(UUID userId) {
    long totalFiles = fileMetadataRepository.countByOwnerId(userId);
    long sharedFiles = fileShareRepository.countBySharedWithId(userId);
    long starredFiles = fileMetadataRepository.countByOwnerIdAndStarredTrue(userId);
    long starredFolders = folderRepository.countByOwnerIdAndStarredTrue(userId);
    Long usedBytes = fileMetadataRepository.sumSizeBytesByOwnerId(userId);

    Map<String, Long> stats = new HashMap<>();
    stats.put("totalFiles", totalFiles);
    stats.put("sharedFiles", sharedFiles);
    stats.put("starredItems", starredFiles + starredFolders);
    stats.put("usedBytes", usedBytes);

    return stats;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> search(String query, UUID userId) {
    List<Folder> folders = folderRepository.findByOwnerIdAndNameContainingIgnoreCase(userId, query);
    List<FileMetadata> files = fileMetadataRepository.findByOwnerIdAndOriginalNameContainingIgnoreCase(userId, query);

    List<FolderResponse> folderResponses = folders.stream().map(f -> FolderResponse.builder()
        .id(f.getId())
        .name(f.getName())
        .parentId(f.getParent() != null ? f.getParent().getId() : null)
        .createdAt(f.getCreatedAt())
        .starred(f.getStarred())
        .itemsCount(folderRepository.countByParentId(f.getId()) + fileMetadataRepository.countByFolderId(f.getId()))
        .build()).collect(Collectors.toList());

    List<FileResponse> fileResponses = files.stream().map(f -> FileResponse.builder()
        .id(f.getId())
        .originalName(f.getOriginalName())
        .sizeBytes(f.getSizeBytes())
        .mimeType(f.getMimeType())
        .folderId(f.getFolder() != null ? f.getFolder().getId() : null)
        .createdAt(f.getCreatedAt())
        .status(f.getStatus())
        .starred(f.getStarred())
        .build()).collect(Collectors.toList());

    Map<String, Object> contents = new HashMap<>();
    contents.put("folders", folderResponses);
    contents.put("files", fileResponses);
    return contents;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listSharedWithMe(UUID userId) {
    List<FileShare> fileShares = fileShareRepository.findBySharedWithId(userId);
    List<SharedFileResponse> fileResponses = fileShares.stream().map(share -> SharedFileResponse.builder()
        .id(share.getFile().getId())
        .originalName(share.getFile().getOriginalName())
        .sizeBytes(share.getFile().getSizeBytes())
        .mimeType(share.getFile().getMimeType())
        .createdAt(share.getFile().getCreatedAt())
        .sharedAt(share.getCreatedAt())
        .sharedByEmail(share.getFile().getOwner().getEmail())
        .sharedByFirstName(share.getFile().getOwner().getFirstName())
        .sharedByLastName(share.getFile().getOwner().getLastName())
        .build()).collect(Collectors.toList());

    List<FolderShare> folderShares = folderShareRepository.findBySharedWithId(userId);
    List<com.luisdev.dto.SharedFolderResponse> folderResponses = folderShares.stream()
        .map(share -> com.luisdev.dto.SharedFolderResponse.builder()
            .id(share.getFolder().getId())
            .name(share.getFolder().getName())
            .createdAt(share.getFolder().getCreatedAt())
            .sharedAt(share.getCreatedAt())
            .sharedByEmail(share.getFolder().getOwner().getEmail())
            .sharedByFirstName(share.getFolder().getOwner().getFirstName())
            .sharedByLastName(share.getFolder().getOwner().getLastName())
            .build())
        .collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("folders", folderResponses);
    result.put("files", fileResponses);
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listSharedByMe(UUID userId) {
    List<FileShare> fileShares = fileShareRepository.findByFileOwnerId(userId);
    List<com.luisdev.dto.SharedByMeFileResponse> fileResponses = fileShares.stream()
        .map(share -> com.luisdev.dto.SharedByMeFileResponse.builder()
            .id(share.getFile().getId())
            .originalName(share.getFile().getOriginalName())
            .sizeBytes(share.getFile().getSizeBytes())
            .mimeType(share.getFile().getMimeType())
            .createdAt(share.getFile().getCreatedAt())
            .sharedAt(share.getCreatedAt())
            .sharedWithEmail(share.getSharedWith().getEmail())
            .sharedWithFirstName(share.getSharedWith().getFirstName())
            .sharedWithLastName(share.getSharedWith().getLastName())
            .build())
        .collect(Collectors.toList());

    List<FolderShare> folderShares = folderShareRepository.findByFolderOwnerId(userId);
    List<com.luisdev.dto.SharedByMeFolderResponse> folderResponses = folderShares.stream()
        .map(share -> com.luisdev.dto.SharedByMeFolderResponse.builder()
            .id(share.getFolder().getId())
            .name(share.getFolder().getName())
            .createdAt(share.getFolder().getCreatedAt())
            .sharedAt(share.getCreatedAt())
            .sharedWithEmail(share.getSharedWith().getEmail())
            .sharedWithFirstName(share.getSharedWith().getFirstName())
            .sharedWithLastName(share.getSharedWith().getLastName())
            .build())
        .collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("folders", folderResponses);
    result.put("files", fileResponses);
    return result;
  }

  @Transactional
  public void revokeFileShare(UUID fileId, String targetUserEmail, UUID ownerUserId) {
    FileMetadata file = fileMetadataRepository.findById(fileId)
        .orElseThrow(() -> new EntityNotFoundException("File not found"));
    if (!file.getOwner().getId().equals(ownerUserId)) {
      throw new SecurityException("User does not own this file");
    }
    User targetUser = userRepository.findByEmail(targetUserEmail)
        .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

    fileShareRepository.findByFileIdAndSharedWithId(fileId, targetUser.getId())
        .ifPresent(fileShareRepository::delete);
  }

  @Transactional
  public void revokeFolderShare(UUID folderId, String targetUserEmail, UUID ownerUserId) {
    Folder folder = folderRepository.findById(folderId)
        .orElseThrow(() -> new EntityNotFoundException("Folder not found"));
    if (!folder.getOwner().getId().equals(ownerUserId)) {
      throw new SecurityException("User does not own this folder");
    }
    User targetUser = userRepository.findByEmail(targetUserEmail)
        .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

    revokeFolderShareRecursively(folder, targetUser);
  }

  private void revokeFolderShareRecursively(Folder folder, User targetUser) {
    folderShareRepository.findByFolderIdAndSharedWithId(folder.getId(), targetUser.getId())
        .ifPresent(folderShareRepository::delete);

    List<FileMetadata> files = fileMetadataRepository.findByOwnerIdAndFolderId(folder.getOwner().getId(),
        folder.getId());
    for (FileMetadata file : files) {
      fileShareRepository.findByFileIdAndSharedWithId(file.getId(), targetUser.getId())
          .ifPresent(fileShareRepository::delete);
    }

    List<Folder> subfolders = folderRepository.findByOwnerIdAndParentId(folder.getOwner().getId(), folder.getId());
    for (Folder subfolder : subfolders) {
      revokeFolderShareRecursively(subfolder, targetUser);
    }
  }
}
