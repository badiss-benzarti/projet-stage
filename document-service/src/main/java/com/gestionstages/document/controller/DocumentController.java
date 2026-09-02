package com.gestionstages.document.controller;

import com.gestionstages.document.dto.DocumentDto;
import com.gestionstages.document.entity.Document;
import com.gestionstages.document.enums.DocumentType;
import com.gestionstages.document.security.AuthenticatedUser;
import com.gestionstages.document.service.AttestationService;
import com.gestionstages.document.service.DocumentService;
import com.gestionstages.document.service.LettreAffectationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Depot, validation, telechargement et generation de documents. */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documents;
    private final AttestationService attestations;
    private final LettreAffectationService lettres;

    /** Depot d'un fichier signe : convention, lettre, rapport, attestation. */
    @PostMapping(value = "/internships/{internshipId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public DocumentDto.Response upload(@AuthenticationPrincipal AuthenticatedUser me,
                                       @PathVariable Long internshipId,
                                       @RequestParam DocumentType type,
                                       @RequestParam("file") MultipartFile file) {
        return documents.upload(me, internshipId, type, file);
    }

    @GetMapping("/internships/{internshipId}")
    public List<DocumentDto.Response> forInternship(@PathVariable Long internshipId) {
        return documents.forInternship(internshipId);
    }

    /** File d'attente du service des stages. */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public Page<DocumentDto.Response> pending(@PageableDefault(size = 20) Pageable pageable) {
        return documents.pending(pageable);
    }

    /** Acceptation ou refus avec motif, exige par le cahier des charges. */
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentDto.Response decide(@AuthenticationPrincipal AuthenticatedUser me,
                                       @PathVariable Long id,
                                       @Valid @RequestBody DocumentDto.Decision decision) {
        return documents.decide(me, id, decision);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        Document d = documents.entity(id);
        byte[] contenu = documents.content(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        d.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + d.getOriginalName() + "\"")
                .contentLength(contenu.length)
                .body(new ByteArrayResource(contenu));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ETUDIANT','ADMIN')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        documents.delete(me, id);
    }

    /**
     * Attestation de stage, delivree par l'entreprise d'accueil.
     *
     * C'est elle qui atteste qu'un stage a bien ete effectue chez elle :
     * l'ecole ne peut pas le certifier a sa place. Le service des stages
     * garde l'acces pour les entreprises qui n'ont pas de compte et
     * repondent hors plateforme.
     */
    @PostMapping("/internships/{internshipId}/attestation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ENTREPRISE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentDto.Response generateAttestation(@AuthenticationPrincipal AuthenticatedUser me,
                                                    @PathVariable Long internshipId) {
        return DocumentDto.Response.from(attestations.generate(me, internshipId));
    }

    /**
     * Lettre d'affectation, editee par le service des stages a la demande
     * de l'etudiant. C'est l'ecole qui affecte, elle seule la signe.
     */
    @PostMapping("/internships/{internshipId}/lettre-affectation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentDto.Response generateLettreAffectation(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable Long internshipId) {
        return DocumentDto.Response.from(lettres.generate(me, internshipId));
    }
}
