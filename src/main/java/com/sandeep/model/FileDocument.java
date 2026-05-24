package com.sandeep.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_documents", indexes = @Index(name = "idx_file_uploader", columnList = "uploadedBy"))
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relative path on the server filesystem (e.g. uploads/uuid-filename.pdf) */
    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String filename;

    private String contentType;

    private long size;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
