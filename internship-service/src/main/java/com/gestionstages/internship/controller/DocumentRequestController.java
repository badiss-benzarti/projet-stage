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

    @GetMapping("/{id}/requests")
    public List<DocumentRequestDto.Response> forInternship(@PathVariable Long id) {
        return requests.forInternship(id);
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public Page<DocumentRequestDto.Response> pending(@PageableDefault(size = 20) Pageable pageable) {
        return requests.pending(pageable);
    }

    @PatchMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public DocumentRequestDto.Response decide(@AuthenticationPrincipal AuthenticatedUser me,
                                              @PathVariable Long requestId,
                                              @Valid @RequestBody DocumentRequestDto.Decision decision) {
        return requests.decide(me, requestId, decision);
    }
}
