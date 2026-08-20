package com.gestionstages.evaluation.controller;

import com.gestionstages.evaluation.dto.EvaluationDto;
import com.gestionstages.evaluation.export.XlsxExporter;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import com.gestionstages.evaluation.service.EvaluationService;
import com.gestionstages.evaluation.service.JournalPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/** Grille d'evaluation, note automatique et exports. */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluations;
    private final JournalPdfService journalPdf;
    private final XlsxExporter xlsx;

    @PutMapping("/internships/{internshipId}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN')")
    public EvaluationDto.Response save(@AuthenticationPrincipal AuthenticatedUser me,
                                       @PathVariable Long internshipId,
                                       @Valid @RequestBody EvaluationDto.Request request) {
        return evaluations.save(me, internshipId, request);
    }

    /** Validation definitive : la note devient visible pour l'etudiant. */
    @PostMapping("/internships/{internshipId}/submit")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN')")
    public EvaluationDto.Response submit(@AuthenticationPrincipal AuthenticatedUser me,
                                         @PathVariable Long internshipId) {
        return evaluations.submit(me, internshipId);
    }

    @GetMapping("/internships/{internshipId}")
    public EvaluationDto.Response byInternship(@AuthenticationPrincipal AuthenticatedUser me,
                                               @PathVariable Long internshipId) {
        return evaluations.findByInternship(me, internshipId);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_PEDAGOGIQUE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public Map<String, Object> statistics() {
        return evaluations.statistics();
    }

    // ------------------------------------------------------------------
    //  Exports
    // ------------------------------------------------------------------

    /** Fichier XLSX des notes definitives, exige par le cahier des charges. */
    @GetMapping("/export/xlsx")
    @PreAuthorize("hasAnyRole('CHEF_DEPARTEMENT_PEDAGOGIQUE','CHEF_DEPARTEMENT_STAGE','ADMIN')")
    public ResponseEntity<ByteArrayResource> exportXlsx() throws IOException {
        byte[] contenu = xlsx.export(evaluations.allSubmitted());
        String nom = "notes-stages-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .contentLength(contenu.length)
                .body(new ByteArrayResource(contenu));
    }

    /** Journal de stage en PDF. */
    @GetMapping("/internships/{internshipId}/journal/pdf")
    public ResponseEntity<ByteArrayResource> journalPdf(@PathVariable Long internshipId) {
        byte[] contenu = journalPdf.generate(internshipId);
        String nom = "journal-stage-" + internshipId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .contentLength(contenu.length)
                .body(new ByteArrayResource(contenu));
    }
}
