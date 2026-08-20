package com.gestionstages.document.service;

import com.gestionstages.document.client.Lookup;
import com.gestionstages.document.dto.DocumentDto;
import com.gestionstages.document.entity.Document;
import com.gestionstages.document.enums.DocumentStatus;
import com.gestionstages.document.enums.DocumentType;
import com.gestionstages.document.enums.Role;
import com.gestionstages.document.exception.ApiExceptions;
import com.gestionstages.document.repository.DocumentRepository;
import com.gestionstages.document.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/** Depot, validation et telechargement des documents de stage. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documents;
    private final StorageService storage;
    private final Lookup lookup;

    /**
     * Depot d'un document par l'etudiant.
     *
     * Un document deja approuve du meme type bloque un nouveau depot :
     * sans cela, un etudiant pourrait remplacer une convention validee.
     */
    @Transactional
    public DocumentDto.Response upload(AuthenticatedUser me, Long internshipId,
                                       DocumentType type, MultipartFile fichier) {
        var stage = lookup.internship(internshipId);
        var etudiant = lookup.student();

        if (!stage.studentId().equals(etudiant.id())) {
            throw new ApiExceptions.ForbiddenException("Ce stage ne vous appartient pas");
        }
        if (documents.existsByInternshipIdAndTypeAndStatusIn(
                internshipId, type, List.of(DocumentStatus.APPROVED))) {
            throw new ApiExceptions.BusinessRuleException(
                    "Un document " + type + " est deja valide pour ce stage");
        }

        String storedName = storage.store(fichier, ".pdf");

        Document d = Document.builder()
                .internshipId(internshipId)
                .studentId(etudiant.id())
                .studentName(etudiant.fullName())
                .type(type)
                .originalName(fichier.getOriginalFilename())
                .storedName(storedName)
                .contentType(fichier.getContentType())
                .sizeBytes(fichier.getSize())
                .status(DocumentStatus.UPLOADED)
                .uploadedBy(me.fullName())
                .generated(false)
                .build();

        documents.save(d);
        log.info("Document {} depose pour le stage {}", type, internshipId);

        return DocumentDto.Response.from(d);
    }

    /** Acceptation ou refus par le service des stages. Un refus exige un motif. */
    @Transactional
    public DocumentDto.Response decide(AuthenticatedUser me, Long documentId,
                                       DocumentDto.Decision decision) {
        Document d = load(documentId);

        if (d.getStatus() == DocumentStatus.APPROVED) {
            throw new ApiExceptions.InvalidTransitionException("Ce document est deja valide");
        }
        if (decision.status() != DocumentStatus.APPROVED
                && decision.status() != DocumentStatus.REJECTED) {
            throw new ApiExceptions.BusinessRuleException(
                    "La decision doit etre APPROVED ou REJECTED");
        }
        if (decision.status() == DocumentStatus.REJECTED
                && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ApiExceptions.BusinessRuleException("Un motif est obligatoire pour un refus");
        }

        d.setStatus(decision.status());
        d.setRejectionReason(decision.status() == DocumentStatus.REJECTED ? decision.reason() : null);
        d.setValidatedBy(me.fullName());
        d.setValidatedAt(Instant.now());

        log.info("Document {} : {} par {}", documentId, decision.status(), me.email());
        return DocumentDto.Response.from(d);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto.Response> forInternship(Long internshipId) {
        return documents.findByInternshipIdOrderByCreatedAtDesc(internshipId).stream()
                .map(DocumentDto.Response::from)
                .toList();
    }

    /** File d'attente du service des stages. */
    @Transactional(readOnly = true)
    public Page<DocumentDto.Response> pending(Pageable pageable) {
        return documents.findByStatusIn(
                        List.of(DocumentStatus.UPLOADED, DocumentStatus.UNDER_REVIEW), pageable)
                .map(DocumentDto.Response::from);
    }

    @Transactional(readOnly = true)
    public Document entity(Long id) {
        return load(id);
    }

    public byte[] content(Long documentId) {
        return storage.read(load(documentId).getStoredName());
    }

    /** Un etudiant peut retirer un depot tant qu'il n'est pas valide. */
    @Transactional
    public void delete(AuthenticatedUser me, Long documentId) {
        Document d = load(documentId);
        Role role = Role.of(me.role());

        if (role != Role.ADMIN && !d.getStudentId().equals(lookup.student().id())) {
            throw new ApiExceptions.ForbiddenException("Ce document ne vous appartient pas");
        }
        if (d.getStatus() == DocumentStatus.APPROVED) {
            throw new ApiExceptions.BusinessRuleException(
                    "Un document valide ne peut pas etre supprime");
        }

        storage.delete(d.getStoredName());
        documents.delete(d);
    }

    private Document load(Long id) {
        return documents.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Document", id));
    }
}
