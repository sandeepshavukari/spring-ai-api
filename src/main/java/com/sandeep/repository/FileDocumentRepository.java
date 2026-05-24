package com.sandeep.repository;

import com.sandeep.model.FileDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDocumentRepository extends JpaRepository<FileDocument, Long> {

    List<FileDocument> findByUploadedByOrderByUploadedAtDesc(Long userId);
}
