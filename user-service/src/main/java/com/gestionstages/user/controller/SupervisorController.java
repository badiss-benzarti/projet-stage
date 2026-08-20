package com.gestionstages.user.controller;

import com.gestionstages.user.dto.SupervisorDto;
import com.gestionstages.user.security.AuthenticatedUser;
import com.gestionstages.user.service.SupervisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/supervisors")
@RequiredArgsConstructor
public class SupervisorController {

    private final SupervisorService supervisors;

    /** Seule une entreprise declare ses encadrants. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENTREPRISE')")
    public SupervisorDto.Response create(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody SupervisorDto.Request request) {
        return supervisors.createForOwnCompany(me, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ENCADRANT')")
    public SupervisorDto.Response own(@AuthenticationPrincipal AuthenticatedUser me) {
        return supervisors.findOwn(me);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENTREPRISE','CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public SupervisorDto.Response byId(@PathVariable Long id) {
        return supervisors.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ENTREPRISE')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        supervisors.deleteFromOwnCompany(me, id);
    }
}
