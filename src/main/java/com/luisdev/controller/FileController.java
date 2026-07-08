package com.luisdev.controller;

import com.luisdev.dto.FileInitUploadRequest;
import com.luisdev.dto.FileInitUploadResponse;
import com.luisdev.dto.FileShareRequest;
import com.luisdev.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // Nota: UUID.fromString("00000000-0000-0000-0000-000000000000") es un valor simulado.
    // Deberías obtener el userId del contexto de seguridad de Spring (ej: @AuthenticationPrincipal)
    private UUID getCurrentUserId() {
        // Todo: Extract from Spring Security Context
        return UUID.randomUUID(); // Simulated for now
    }

    @PostMapping("/upload/initiate")
    public ResponseEntity<FileInitUploadResponse> initiateUpload(@RequestBody FileInitUploadRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(fileService.initiateUpload(request, userId));
    }

    @PostMapping("/{fileId}/upload/confirm")
    public ResponseEntity<Void> confirmUpload(@PathVariable UUID fileId) {
        UUID userId = getCurrentUserId();
        fileService.confirmUpload(fileId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID fileId) {
        UUID userId = getCurrentUserId();
        String url = fileService.getDownloadUrl(fileId, userId);
        return ResponseEntity.ok(Collections.singletonMap("downloadUrl", url));
    }

    @PostMapping("/{fileId}/share")
    public ResponseEntity<Void> shareFile(@PathVariable UUID fileId, @RequestBody FileShareRequest request) {
        UUID userId = getCurrentUserId();
        fileService.shareFile(fileId, request.getTargetUserEmail(), userId);
        return ResponseEntity.ok().build();
    }
}
