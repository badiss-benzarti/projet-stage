package com.gestionstages.document.repository;

import com.gestionstages.document.entity.Document;
import com.gestionstages.document.enums.DocumentStatus;
import com.gestionstages.document.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByInternshipIdOrderByCreatedAtDesc(Long internshipId);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    Page<Document> findByStatusIn(List<DocumentStatus> statuses, Pageable pageable);

    Optional<Document> findFirstByInternshipIdAndTypeAndStatus(
            Long internshipId, DocumentType type, DocumentStatus status);

    boolean existsByInternshipIdAndTypeAndStatusIn(
            Long internshipId, DocumentType type, List<DocumentStatus> statuses);
}
