package com.gestionstages.internship.service;

import com.gestionstages.internship.client.UserLookup;
import com.gestionstages.internship.dto.DocumentRequestDto;
import com.gestionstages.internship.entity.DocumentRequest;
import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.exception.ApiExceptions;
import com.gestionstages.internship.repository.DocumentRequestRepository;
import com.gestionstages.internship.repository.InternshipRepository;
import com.gestionstages.internship.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Demandes de convention et de lettre d'affectation.
 *
 * Le cahier des charges les distingue du depot de documents : ici c'est
 * la DEMANDE et son instruction ; le fichier signe est gere par le
 * document-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRequestService {

    private final DocumentRequestRepository requests;
    private final InternshipRepository internships;
    private final UserLookup lookup;

    /**
     * L'etudiant demande un document pour son stage.
     *
     * Deux garde-fous : le dossier doit etre approuve (demander une lettre
     * d'affectation pour un stage refuse n'a pas de sens), et une demande
     * du meme type ne peut pas etre en attente deux fois.
     */
    @Transactional
    public DocumentRequestDto.Response request(AuthenticatedUser me, Long internshipId,
                                               DocumentRequestDto.Request req) {
        Internship i = internships.findById(internshipId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Stage", internshipId));

        if (!i.getStudentId().equals(lookup.student().id())) {
            throw new ApiExceptions.ForbiddenException("Ce dossier ne vous appartient pas");
        }

        if (i.getStatus() == InternshipStatus.DRAFT
                || i.getStatus() == InternshipStatus.SUBMITTED
                || i.getStatus().isTerminal()) {
            throw new ApiExceptions.BusinessRuleException(
                    "Le dossier doit etre approuve avant de demander un document (etat "
                            + i.getStatus() + ")");
        }

        if (requests.existsByInternshipIdAndTypeAndStatus(
                internshipId, req.type(), RequestStatus.PENDING)) {
            throw new ApiExceptions.BusinessRuleException(
                    "Une demande de ce type est deja en attente");
        }

        DocumentRequest r = DocumentRequest.builder()
                .internship(i)
                .type(req.type())
                .status(RequestStatus.PENDING)
                .build();

        requests.save(r);
        log.info("Demande {} pour le stage {}", req.type(), internshipId);

        return DocumentRequestDto.Response.from(r);
    }

    /** Le service des stages edite le document ou refuse la demande. */
    @Transactional
    public DocumentRequestDto.Response decide(AuthenticatedUser me, Long requestId,
                                              DocumentRequestDto.Decision decision) {
        DocumentRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Demande", requestId));

        if (r.getStatus() != RequestStatus.PENDING) {
            throw new ApiExceptions.InvalidTransitionException(
                    "Cette demande a deja ete traitee (" + r.getStatus() + ")");
        }

        if (decision.status() == RequestStatus.REJECTED
                && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ApiExceptions.BusinessRuleException("Un motif est obligatoire pour un refus");
        }

        r.setStatus(decision.status());
        r.setReason(decision.reason());
        r.setProcessedBy(me.fullName());
        r.setProcessedAt(Instant.now());

        return DocumentRequestDto.Response.from(r);
    }

    @Transactional(readOnly = true)
    public List<DocumentRequestDto.Response> forInternship(Long internshipId) {
        return requests.findByInternshipId(internshipId).stream()
                .map(DocumentRequestDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DocumentRequestDto.Response> pending(Pageable pageable) {
        return requests.findByStatus(RequestStatus.PENDING, pageable)
                .map(DocumentRequestDto.Response::from);
    }
}
