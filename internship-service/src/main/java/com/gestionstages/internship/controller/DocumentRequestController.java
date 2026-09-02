package com.gestionstages.internship.controller;

import com.gestionstages.internship.dto.DocumentRequestDto;
import com.gestionstages.internship.security.AuthenticatedUser;
import com.gestionstages.internship.service.DocumentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Demandes de convention et de lettre d'affectation. */
@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class DocumentRequestController {

    private final DocumentRequestService requests;

    @PostMapping("/{id}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public DocumentRequestDto.Response request(@AuthenticationPrincipal AuthenticatedUser me,
                                               @PathVariable Long id,
                                               @Valid @RequestBody DocumentRequestDto.Request body) {
        return requests.request(me, id, body);
    }

    /**
     * Demande sans dossier : demande de stage, attestation de scolarite.
     * Volontairement hors de /{id}/requests, puisqu'il n'y a pas d'{id}.
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public DocumentRequestDto.Response requestStandalone(
            @AuthenticationPrincipal AuthenticatedUser me,
            @Valid @RequestBody DocumentRequestDto.Request body) {
        return requests.requestStandalone(me, body);
    }

    /** Les demandes de l'etudiant connecte, tous dossiers confondus. */
    @GetMapping("/requests/mine")
    @PreAuthorize("hasRole('ETUDIANT')")
    public List<DocumentRequestDto.Response> mine(@AuthenticationPrincipal AuthenticatedUser me) {
        return requests.mine(me);
    }

    @GetMapping("/{id}/requests")
    public List<DocumentRequestDto.Response> forInternship(@PathVariable Long id) {
        return requests.forInternship(id);
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public Page<DocumentRequestDto.Response> pending(@PageableDefault(size = 20) Pageable pageable) {
        return requests.pending(pageable);
    }

    /** Les attestations de stage que ses propres stagiaires lui demandent. */
    @GetMapping("/requests/company")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public List<DocumentRequestDto.Response> pendingForCompany(
            @AuthenticationPrincipal AuthenticatedUser me) {
        return requests.pendingForCompany(me);
    }

    @PatchMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('ENTREPRISE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentRequestDto.Response decide(@AuthenticationPrincipal AuthenticatedUser me,
                                              @PathVariable Long requestId,
                                              @Valid @RequestBody DocumentRequestDto.Decision decision) {
        return requests.decide(me, requestId, decision);
    }

    /**
     * Rattache le PDF produit a la demande. Appele par le
     * document-service, jamais par le navigateur.
     */
    @PatchMapping("/requests/{requestId}/issued")
    @PreAuthorize("hasAnyRole('ENTREPRISE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentRequestDto.Response markIssued(@PathVariable Long requestId,
                                                  @Valid @RequestBody DocumentRequestDto.Issued body) {
        return requests.markIssued(requestId, body);
    }
}
