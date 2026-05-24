package com.sandeep.controller;

import com.sandeep.dto.response.ApiResponse;
import com.sandeep.dto.response.FileUploadResponse;
import com.sandeep.model.FileDocument;
import com.sandeep.security.CustomUserDetails;
import com.sandeep.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file, Authentication auth) {
        FileDocument doc = fileStorageService.store(file, userId(auth));

        FileUploadResponse response = FileUploadResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .contentType(doc.getContentType())
                .size(doc.getSize())
                .hasExtractedText(doc.getExtractedText() != null && !doc.getExtractedText().isBlank())
                .extractedText(doc.getExtractedText() != null && doc.getExtractedText().length() > 200
                        ? doc.getExtractedText().substring(0, 200) + "…"
                        : doc.getExtractedText())
                .uploadedAt(doc.getUploadedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        FileDocument doc = fileStorageService.getMetadata(id);
        Resource resource = fileStorageService.getResource(doc.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        doc.getContentType() != null ? doc.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileDocument>> getMetadata(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(fileStorageService.getMetadata(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        fileStorageService.delete(id, userId(auth));
        return ResponseEntity.ok(ApiResponse.ok("File deleted"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileDocument>>> listMyFiles(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(fileStorageService.getUserFiles(userId(auth))));
    }

    private Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUserId();
    }
}
