package com.gestionstages.user.controller;

import com.gestionstages.user.dto.CompanyDto;
import com.gestionstages.user.dto.SupervisorDto;
import com.gestionstages.user.security.AuthenticatedUser;
import com.gestionstages.user.service.CompanyService;
import com.gestionstages.user.service.SupervisorService;
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

@RestController
@RequestMapping("/api/users/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companies;
    private final SupervisorService supervisors;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENTREPRISE')")
    public CompanyDto.Response createOwn(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody CompanyDto.Request request) {
        return companies.createOwn(me, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public CompanyDto.Response own(@AuthenticationPrincipal AuthenticatedUser me) {
        return companies.findOwn(me);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public CompanyDto.Response updateOwn(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody CompanyDto.Request request) {
        return companies.updateOwn(me, request);
    }

    /** Annuaire des entreprises : accessible a tout utilisateur authentifie. */
    @GetMapping
    public Page<CompanyDto.Response> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return companies.findAll(pageable);
    }

    @GetMapping("/{id}")
    public CompanyDto.Response byId(@PathVariable Long id) {
        return companies.findById(id);
    }

    @GetMapping("/{id}/supervisors")
    public List<SupervisorDto.Response> supervisorsOf(@PathVariable Long id) {
        return supervisors.findByCompany(id);
    }
}
