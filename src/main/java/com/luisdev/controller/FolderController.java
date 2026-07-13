package com.luisdev.controller;

import com.luisdev.dto.FolderRequest;
import com.luisdev.dto.FolderResponse;
import com.luisdev.service.FileService;
import com.luisdev.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

  private final FileService fileService;
  private final UserService userService;

  public FolderController(FileService fileService, UserService userService) {
    this.fileService = fileService;
    this.userService = userService;
  }

  private UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return userService.findUserIdByEmail(auth.getName());
  }

  @PostMapping
  public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderRequest request) {
    return ResponseEntity.ok(fileService.createFolder(request.getName(), request.getParentId(), request.getStarred(), getCurrentUserId()));
  }

  @PutMapping("/{folderId}/rename")
  public ResponseEntity<java.util.Map<String, String>> renameFolder(@PathVariable UUID folderId, @RequestBody com.luisdev.dto.RenameRequest request) {
    fileService.renameFolder(folderId, request.getNewName(), getCurrentUserId());
    return ResponseEntity.ok(java.util.Collections.singletonMap("message", "Carpeta renombrada con éxito a " + request.getNewName()));
  }

  @DeleteMapping("/{folderId}")
  public ResponseEntity<Void> deleteFolder(@PathVariable UUID folderId) {
    fileService.deleteFolder(folderId, getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{folderId}/starred")
  public ResponseEntity<Void> updateStarred(@PathVariable UUID folderId, @RequestBody com.luisdev.dto.StarredRequest request) {
    fileService.updateFolderStarred(folderId, request.getStarred(), getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{folderId}/share")
  public ResponseEntity<Map<String, String>> shareFolder(@PathVariable UUID folderId, @RequestBody com.luisdev.dto.FileShareRequest request) {
    fileService.shareFolder(folderId, request.getTargetUserEmail(), getCurrentUserId());
    return ResponseEntity.ok(Map.of("message", "Carpeta compartida con éxito a " + request.getTargetUserEmail()));
  }

  @DeleteMapping("/{folderId}/share")
  public ResponseEntity<Map<String, String>> revokeFolderShare(@PathVariable UUID folderId, @RequestParam String targetUserEmail) {
    fileService.revokeFolderShare(folderId, targetUserEmail, getCurrentUserId());
    return ResponseEntity.ok(Map.of("message", "Acceso revocado con éxito para " + targetUserEmail));
  }
}
