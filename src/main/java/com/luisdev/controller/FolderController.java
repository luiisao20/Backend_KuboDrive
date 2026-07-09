package com.luisdev.controller;

import com.luisdev.dto.FolderRequest;
import com.luisdev.dto.FolderResponse;
import com.luisdev.dto.RenameRequest;
import com.luisdev.service.FileService;
import com.luisdev.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    return ResponseEntity.ok(fileService.createFolder(request.getName(), request.getParentId(), getCurrentUserId()));
  }

  @PutMapping("/{folderId}/rename")
  public ResponseEntity<Void> renameFolder(@PathVariable UUID folderId, @RequestBody RenameRequest request) {
    fileService.renameFolder(folderId, request.getNewName(), getCurrentUserId());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{folderId}")
  public ResponseEntity<Void> deleteFolder(@PathVariable UUID folderId) {
    fileService.deleteFolder(folderId, getCurrentUserId());
    return ResponseEntity.ok().build();
  }
}
