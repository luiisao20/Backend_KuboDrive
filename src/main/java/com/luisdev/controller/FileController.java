package com.luisdev.controller;

import com.luisdev.dto.FileInitUploadRequest;
import com.luisdev.dto.FileInitUploadResponse;
import com.luisdev.dto.FileShareRequest;
import com.luisdev.dto.RenameRequest;
import com.luisdev.service.FileService;
import com.luisdev.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

  private final FileService fileService;
  private final UserService userService;

  public FileController(FileService fileService, UserService userService) {
    this.fileService = fileService;
    this.userService = userService;
  }

  private UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return userService.findUserIdByEmail(auth.getName());
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listContents(@RequestParam(required = false) UUID folderId) {
    return ResponseEntity.ok(fileService.listContents(folderId, getCurrentUserId()));
  }

  @PostMapping("/upload/initiate")
  public ResponseEntity<FileInitUploadResponse> initiateUpload(@RequestBody FileInitUploadRequest request) {
    return ResponseEntity.ok(fileService.initiateUpload(request, getCurrentUserId()));
  }

  @PostMapping("/{fileId}/upload/confirm")
  public ResponseEntity<Void> confirmUpload(@PathVariable UUID fileId) {
    fileService.confirmUpload(fileId, getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{fileId}/download-url")
  public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID fileId) {
    String url = fileService.getDownloadUrl(fileId, getCurrentUserId());
    return ResponseEntity.ok(Collections.singletonMap("downloadUrl", url));
  }

  @PostMapping("/{fileId}/share")
  public ResponseEntity<Void> shareFile(@PathVariable UUID fileId, @RequestBody FileShareRequest request) {
    fileService.shareFile(fileId, request.getTargetUserEmail(), getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{fileId}/rename")
  public ResponseEntity<Void> renameFile(@PathVariable UUID fileId, @RequestBody RenameRequest request) {
    fileService.renameFile(fileId, request.getNewName(), getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{fileId}")
  public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
    fileService.deleteFile(fileId, getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Long>> getStats() {
      return ResponseEntity.ok(fileService.getStats(getCurrentUserId()));
  }

  @PutMapping("/{fileId}/starred")
  public ResponseEntity<Void> updateStarred(@PathVariable UUID fileId, @RequestBody com.luisdev.dto.StarredRequest request) {
    fileService.updateFileStarred(fileId, request.getStarred(), getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/search")
  public ResponseEntity<Map<String, Object>> search(@RequestParam String query) {
      return ResponseEntity.ok(fileService.search(query, getCurrentUserId()));
  }
}
