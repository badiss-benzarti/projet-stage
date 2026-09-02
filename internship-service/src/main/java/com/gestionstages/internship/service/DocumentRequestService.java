package com.gestionstages.internship.service;

import com.gestionstages.internship.client.UserClient;
import com.gestionstages.internship.client.UserLookup;
import com.gestionstages.internship.dto.DocumentRequestDto;
import com.gestionstages.internship.entity.DocumentRequest;
import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.enums.RequestType;
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
 * Demandes de documents administratifs adressees au service des stages.
 *
 * Ce service instruit la DEMANDE ; le fichier remis a l'etudiant est
 * produit par le document-service, qui appelle ensuite markIssued.
 * Separer les deux evite qu'une demande passe a "delivree" sans qu'aucun
 * document n'existe : c'etait le cas avant, l'etudiant lisait "delivree"
 * et n'avait rien a telecharger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRequestService {

    private final DocumentRequestRepository requests;
    private final InternshipRepository internships;
    private final UserLookup lookup;

    /**
     * Demande rattachee a un stage : convention, lettre d'affectation,
     * attestation de presence.
     */
    @Transactional
    public DocumentRequestDto.Response request(AuthenticatedUser me, Long internshipId,
                                               DocumentRequestDto.Request req) {
        UserClient.StudentRef etudiant = lookup.student();

        if (!req.type().requiresInternship()) {
            throw new ApiExceptions.BusinessRuleException(
                    req.type().libelle() + " ne se demande pas depuis un dossier de stage : "
                            + "elle sert justement a en trouver un");
        }

        Internship i = internships.findById(internshipId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Stage", internshipId));

        if (!i.getStudentId().equals(etudiant.id())) {
            throw new ApiExceptions.ForbiddenException("Ce dossier ne vous appartient pas");
        }

        verifierEtatDuStage(i, req.type());

        if (requests.existsByInternshipIdAndTypeAndStatus(
                internshipId, req.type(), RequestStatus.PENDING)) {
            throw new ApiExceptions.BusinessRuleException(
                    "Une demande de ce type est deja en attente");
        }

        return enregistrer(etudiant, i, req.type());
    }

    /**
     * Demande independante de tout stage : demande de stage, attestation
     * de scolarite. L'etudiant n'a pas encore de dossier - ou le document
     * n'a rien a voir avec un stage.
     */
    @Transactional
    public DocumentRequestDto.Response requestStandalone(AuthenticatedUser me,
                                                         DocumentRequestDto.Request req) {
        UserClient.StudentRef etudiant = lookup.student();

        if (req.type().requiresInternship()) {
            throw new ApiExceptions.BusinessRuleException(
                    req.type().libelle() + " se demande depuis votre dossier de stage");
        }

        if (requests.existsByStudentIdAndTypeAndStatus(
                etudiant.id(), req.type(), RequestStatus.PENDING)) {
            throw new ApiExceptions.BusinessRuleException(
                    "Une demande de ce type est deja en attente");
        }

        return enregistrer(etudiant, null, req.type());
    }

    /** Les demandes de l'etudiant connecte, la plus recente d'abord. */
    @Transactional(readOnly = true)
    public List<DocumentRequestDto.Response> mine(AuthenticatedUser me) {
        return requests.findByStudentIdOrderByIdDesc(lookup.student().id())
                .stream()
                .map(DocumentRequestDto.Response::from)
                .toList();
    }

    /** Les demandes rattachees a un dossier de stage. */
    @Transactional(readOnly = true)
    public List<DocumentRequestDto.Response> forInternship(Long internshipId) {
        return requests.findByInternshipId(internshipId)
                .stream()
                .map(DocumentRequestDto.Response::from)
                .toList();
    }

    /** La file d'instruction du service des stages. */
    @Transactional(readOnly = true)
    public Page<DocumentRequestDto.Response> pending(Pageable pageable) {
        return requests.findByStatus(RequestStatus.PENDING, pageable)
                .map(DocumentRequestDto.Response::from);
    }

    /**
     * Le service des stages instruit la demande.
     *
     * Accepter ne delivre PAS le document : cela autorise seulement son
     * edition. Le fichier est produit par le document-service, qui
     * rappelle ensuite markIssued. Tant qu'il n'a pas rappele, la demande
     * reste acceptee sans piece jointe, ce qui est un etat honnete.
     */
    @Transactional
    public DocumentRequestDto.Response decide(AuthenticatedUser me, Long requestId,
                                              DocumentRequestDto.Decision decision) {
        DocumentRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Demande", requestId));

        if (r.getStatus() != RequestStatus.PENDING) {
            throw new ApiExceptions.BusinessRuleException(
                    "Cette demande a deja ete traitee");
        }
        if (decision.status() == RequestStatus.PENDING) {
            throw new ApiExceptions.BusinessRuleException(
                    "Choisissez d'accepter ou de refuser la demande");
        }
        if (decision.status() == RequestStatus.REJECTED
                && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Un motif est obligatoire pour refuser une demande");
        }

        r.setStatus(decision.status());
        r.setReason(decision.reason());
        r.setProcessedBy(me.fullName());
        r.setProcessedAt(Instant.now());
        requests.save(r);

        log.info("Demande {} ({}) : {} par {}",
                r.getId(), r.getType(), decision.status(), me.email());

        return DocumentRequestDto.Response.from(r);
    }

    /**
     * Rattache le fichier produit a la demande. Appele par le
     * document-service une fois le PDF genere et stocke.
     */
    @Transactional
    public DocumentRequestDto.Response markIssued(Long requestId, DocumentRequestDto.Issued body) {
        DocumentRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Demande", requestId));

        if (r.getStatus() != RequestStatus.ISSUED) {
            throw new ApiExceptions.BusinessRuleException(
                    "Seule une demande acceptee peut recevoir un document");
        }

        r.setDocumentId(body.documentId());
        requests.save(r);
        log.info("Demande {} : document {} rattache", r.getId(), body.documentId());

        return DocumentRequestDto.Response.from(r);
    }

    // ==================================================================
    //  Interne
    // ==================================================================

    /**
     * Un document ne se demande qu'au moment ou il a un sens.
     *
     * La convention et la lettre d'affectation scellent un stage deja
     * accorde par l'entreprise ; l'attestation de presence suppose que le
     * stage ait commence. Verifier ici evite d'editer un document qui
     * affirmerait quelque chose de faux.
     */
    private void verifierEtatDuStage(Internship i, RequestType type) {
        InternshipStatus statut = i.getStatus();

        boolean accorde = statut == InternshipStatus.ACCEPTED
                || statut == InternshipStatus.IN_PROGRESS
                || statut == InternshipStatus.COMPLETED;

        switch (type) {
            case CONVENTION, LETTRE_AFFECTATION -> {
                if (!accorde) {
                    throw new ApiExceptions.BusinessRuleException(
                            type.libelle() + " ne peut etre demandee qu'une fois "
                                    + "l'entreprise vous a accepte");
                }
            }
            case ATTESTATION_PRESENCE -> {
                if (statut != InternshipStatus.IN_PROGRESS
                        && statut != InternshipStatus.COMPLETED) {
                    throw new ApiExceptions.BusinessRuleException(
                            "L'attestation de presence suppose un stage commence");
                }
            }
            default -> { /* les types sans stage sont filtres en amont */ }
        }
    }

    /** Cree la demande en attente et renvoie sa representation. */
    private DocumentRequestDto.Response enregistrer(UserClient.StudentRef etudiant,
                                                    Internship stage, RequestType type) {
        DocumentRequest r = DocumentRequest.builder()
                .studentId(etudiant.id())
                .studentName(etudiant.fullName())
                .studentEmail(etudiant.email())
                .internship(stage)
                .type(type)
                .status(RequestStatus.PENDING)
                .build();

        requests.save(r);
        log.info("Demande de {} enregistree : {} (etudiant {}, stage {})",
                type, r.getId(), etudiant.id(), stage == null ? "aucun" : stage.getId());

        return DocumentRequestDto.Response.from(r);
    }
}
