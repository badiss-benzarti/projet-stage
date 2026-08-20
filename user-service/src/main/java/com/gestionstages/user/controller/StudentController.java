package com.gestionstages.user.controller;

import com.gestionstages.user.dto.StudentDto;
import com.gestionstages.user.security.AuthenticatedUser;
import com.gestionstages.user.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Profils etudiants.
 *
 * Regle de securite : un etudiant ne manipule QUE son propre profil ; la
 * consultation de la liste est reservee aux chefs de departement. Le
 * frontend peut masquer un bouton, mais la regle est appliquee ici.
 */
@RestController
@RequestMapping("/api/users/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService students;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response createOwn(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody StudentDto.Request request) {
        return students.createOwn(me, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response own(@AuthenticationPrincipal AuthenticatedUser me) {
        return students.findOwn(me);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response updateOwn(@AuthenticationPrincipal AuthenticatedUser me,
                                         @Valid @RequestBody StudentDto.Request request) {
        return students.updateOwn(me, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public Page<StudentDto.Response> list(
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String classe,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return students.findAll(departement, classe, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ENCADRANT','ADMIN')")
    public StudentDto.Response byId(@PathVariable Long id) {
        return students.findById(id);
    }
}
