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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gestionstages.user.enums.Governorate;
import com.gestionstages.user.enums.InstitutionType;

import java.util.Arrays;
import java.util.List;

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

    /**
     * Fiche complete d'un etudiant.
     *
     * Ouverte a l'entreprise : elle doit pouvoir joindre le candidat
     * qu'elle envisage d'accueillir, et connaitre son cursus pour se
     * prononcer. Reste fermee a l'etudiant, qui passe par /me : sans
     * cela, n'importe lequel lirait la fiche d'un autre.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENTREPRISE','ENCADRANT','CHEF_DEPARTEMENT_STAGE',"
            + "'CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public StudentDto.Response byId(@PathVariable Long id) {
        return students.findById(id);
    }

    // ---- Photo de profil ----

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response savePhoto(@AuthenticationPrincipal AuthenticatedUser me,
                                         @RequestParam("file") MultipartFile file) {
        return students.savePhoto(me, file);
    }

    /**
     * Photo d'un etudiant. Non protegee par un role : elle est affichee
     * dans les listes vues par l'encadrant et les departements, et ne
     * revele rien de plus que le nom deja present dans ces listes.
     */
    @GetMapping("/{id}/photo")
    public ResponseEntity<ByteArrayResource> photo(@PathVariable Long id) {
        var photo = students.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        photo.contentType() == null ? "image/jpeg" : photo.contentType()))
                .contentLength(photo.contenu().length)
                .body(new ByteArrayResource(photo.contenu()));
    }

    // ---- CV ----

    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response saveCv(@AuthenticationPrincipal AuthenticatedUser me,
                                      @RequestParam("file") MultipartFile file) {
        return students.saveCv(me, file);
    }

    @DeleteMapping("/me/cv")
    @PreAuthorize("hasRole('ETUDIANT')")
    public StudentDto.Response deleteCv(@AuthenticationPrincipal AuthenticatedUser me) {
        return students.deleteCv(me);
    }

    /** Son propre CV : l'etudiant n'a pas acces a l'endpoint par identifiant. */
    @GetMapping("/me/cv")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<ByteArrayResource> myCv(@AuthenticationPrincipal AuthenticatedUser me) {
        return pdf(students.myCv(me));
    }

    /**
     * Le CV d'un etudiant. Contrairement a la photo, il est reserve :
     * un CV porte des coordonnees personnelles et un parcours, il n'a
     * pas a etre lisible par n'importe quel porteur de jeton.
     */
    @GetMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ENTREPRISE','ENCADRANT','CHEF_DEPARTEMENT_STAGE',"
            + "'CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public ResponseEntity<ByteArrayResource> cv(@PathVariable Long id) {
        return pdf(students.cv(id));
    }

    private ResponseEntity<ByteArrayResource> pdf(StudentService.Cv cv) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + cv.nomFichier() + "\"")
                .contentLength(cv.contenu().length)
                .body(new ByteArrayResource(cv.contenu()));
    }

    // ---- Referentiels, pour alimenter les listes du frontend ----

    @GetMapping("/referentiels")
    public java.util.Map<String, List<StudentDto.Option>> referentiels() {
        return java.util.Map.of(
                "gouvernorats", Arrays.stream(Governorate.values())
                        .map(g -> new StudentDto.Option(g.name(), g.libelle()))
                        .toList(),
                "typesEtablissement", Arrays.stream(InstitutionType.values())
                        .map(t -> new StudentDto.Option(t.name(),
                                t == InstitutionType.PUBLIQUE ? "Publique / étatique" : "Privée"))
                        .toList());
    }
}
