package com.gestionstages.internship.controller;

import com.gestionstages.internship.dto.InternshipDto;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.security.AuthenticatedUser;
import com.gestionstages.internship.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Module 1 : demandes de stage et workflow de validation.
 *
 * Le controle fin (proprietaire du dossier, entreprise concernee,
 * transition autorisee) est fait dans le service : PreAuthorize ne filtre
 * ici que le premier niveau, celui du role.
 */
@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internships;

    // ---- Etudiant ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public InternshipDto.Response create(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody InternshipDto.Request request) {
        return internships.create(me, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ETUDIANT','ADMIN')")
    public InternshipDto.Response updateDraft(@AuthenticationPrincipal AuthenticatedUser me,
                                              @PathVariable Long id,
                                              @Valid @RequestBody InternshipDto.Request request) {
        return internships.updateDraft(me, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ETUDIANT','ADMIN')")
    public void deleteDraft(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        internships.deleteDraft(me, id);
    }

    /**
     * Les dossiers de l'etudiant, le plus recent d'abord.
     *
     * Le tri est explicite : sans lui, une page de taille 1 renvoyait un
     * dossier arbitraire, et l'etudiant qui a plusieurs demandes voyait
     * les documents d'un autre que le sien.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('ETUDIANT')")
    public Page<InternshipDto.Response> mine(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return internships.mine(me, pageable);
    }

    // ---- Entreprise et encadrant ----

    @GetMapping("/company")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public Page<InternshipDto.Response> forMyCompany(@AuthenticationPrincipal AuthenticatedUser me,
                                                     @RequestParam(required = false) InternshipStatus status,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return internships.forMyCompany(me, status, pageable);
    }

    @GetMapping("/supervision")
    @PreAuthorize("hasRole('ENCADRANT')")
    public Page<InternshipDto.Response> forMySupervision(@AuthenticationPrincipal AuthenticatedUser me,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return internships.forMySupervision(me, pageable);
    }

    // ---- Departement ----

    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public Page<InternshipDto.Response> forDepartment(@RequestParam(required = false) InternshipStatus status,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return internships.forDepartment(status, pageable);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public Map<String, Long> statistics() {
        return internships.statistics();
    }

    // ---- Commun ----

    @GetMapping("/{id}")
    public InternshipDto.Response byId(@AuthenticationPrincipal AuthenticatedUser me,
                                       @PathVariable Long id) {
        return internships.findById(me, id);
    }

    /**
     * Point d'entree unique du workflow.
     *
     * Un seul endpoint plutot que /approve, /reject, /accept... : la regle
     * vit dans la table de transitions, pas dans la couche HTTP. Ajouter
     * un etat ne demandera aucune nouvelle route.
     */
    @PostMapping("/{id}/transition")
    public InternshipDto.Response transition(@AuthenticationPrincipal AuthenticatedUser me,
                                             @PathVariable Long id,
                                             @Valid @RequestBody InternshipDto.TransitionRequest request) {
        return internships.transition(me, id, request);
    }
}
