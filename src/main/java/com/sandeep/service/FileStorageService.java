package com.sandeep.service;

import com.sandeep.config.AppProperties;
import com.sandeep.exception.ResourceNotFoundException;
import com.sandeep.exception.StorageException;
import com.sandeep.exception.UnauthorizedException;
import com.sandeep.model.FileDocument;
import com.sandeep.repository.FileDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileDocumentRepository fileDocumentRepository;
    private final DocumentExtractionService extractionService;
    private final AppProperties appProperties;

    private Path uploadRoot;

    @PostConstruct
    public void init() {
        uploadRoot = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
            log.info("Upload directory: {}", uploadRoot);
        } catch (IOException e) {
            throw new StorageException("Could not create upload directory: " + e.getMessage());
        }
    }

    public FileDocument store(MultipartFile file, Long userId) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String storedName = UUID.randomUUID() + "-" + originalFilename;
        Path destination = uploadRoot.resolve(storedName);
        String contentType = file.getContentType();

        try {
            Files.copy(file.getInputStream(), destination);

            String extractedText = "";
            if (extractionService.isExtractableDocument(contentType)) {
                extractedText = extractionService.extractText(file.getInputStream(), contentType);
            }

            FileDocument doc = FileDocument.builder()
                    .storagePath(destination.toString())
                    .filename(originalFilename)
                    .contentType(contentType)
                    .size(file.getSize())
                    .uploadedBy(userId)
                    .extractedText(extractedText)
                    .build();

            return fileDocumentRepository.save(doc);

        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + e.getMessage());
        }
    }

    public Resource getResource(String storagePath) {
        Path path = Paths.get(storagePath);
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found in storage: " + storagePath);
        }
        return resource;
    }

    public InputStream openStream(String storagePath) {
        try {
            return getResource(storagePath).getInputStream();
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + e.getMessage());
        }
    }

    public byte[] readBytes(String storagePath) {
        try {
            return Files.readAllBytes(Paths.get(storagePath));
        } catch (IOException e) {
            throw new StorageException("Failed to read file bytes: " + e.getMessage());
        }
    }

    public FileDocument getMetadata(Long fileId) {
        return fileDocumentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    public void delete(Long fileId, Long userId) {
        FileDocument doc = getMetadata(fileId);
        if (!doc.getUploadedBy().equals(userId)) {
            throw new UnauthorizedException("Not authorized to delete this file");
        }
        try {
            Files.deleteIfExists(Paths.get(doc.getStoragePath()));
        } catch (IOException e) {
            log.warn("Could not delete physical file {}: {}", doc.getStoragePath(), e.getMessage());
        }
        fileDocumentRepository.delete(doc);
        log.info("Deleted file {} for user {}", fileId, userId);
    }

    public List<FileDocument> getUserFiles(Long userId) {
        return fileDocumentRepository.findByUploadedByOrderByUploadedAtDesc(userId);
    }
}
