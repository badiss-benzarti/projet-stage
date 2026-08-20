package com.gestionstages.evaluation.controller;

import com.gestionstages.evaluation.dto.ClaimDto;
import com.gestionstages.evaluation.enums.ClaimStatus;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import com.gestionstages.evaluation.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Reclamations avec bouclage. */
@RestController
@RequestMapping("/api/evaluations/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claims;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public ClaimDto.Response open(@AuthenticationPrincipal AuthenticatedUser me,
                                  @Valid @RequestBody ClaimDto.Request request) {
        return claims.open(me, request);
    }

    /** Reponse du departement ou relance de l'etudiant : le bouclage. */
    @PostMapping("/{id}/messages")
    public ClaimDto.Response reply(@AuthenticationPrincipal AuthenticatedUser me,
                                   @PathVariable Long id,
                                   @Valid @RequestBody ClaimDto.MessageRequest request) {
        return claims.reply(me, id, request);
    }

    @PostMapping("/{id}/take")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public ClaimDto.Response take(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return claims.take(me, id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public ClaimDto.Response close(@AuthenticationPrincipal AuthenticatedUser me,
                                   @PathVariable Long id,
                                   @RequestBody(required = false) ClaimDto.MessageRequest request) {
        return claims.close(me, id, request);
    }

    @GetMapping("/{id}")
    public ClaimDto.Response byId(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return claims.findById(me, id);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ETUDIANT')")
    public Page<ClaimDto.Response> mine(@AuthenticationPrincipal AuthenticatedUser me,
                                        @PageableDefault(size = 20) Pageable pageable) {
        return claims.mine(me, pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_PEDAGOGIQUE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public Page<ClaimDto.Response> forDepartment(@RequestParam(required = false) ClaimStatus status,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return claims.forDepartment(status, pageable);
    }
}
