package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.client.Lookup;
import com.gestionstages.evaluation.entity.Task;
import com.gestionstages.evaluation.enums.TaskStatus;
import com.gestionstages.evaluation.export.PdfExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Journal de stage au format PDF.
 *
 * Genere ICI et non dans le document-service : les donnees du journal
 * vivent dans ce service, un PDF produit ailleurs imposerait un appel
 * inter-services a chaque telechargement.
 */
@Service
@RequiredArgsConstructor
public class JournalPdfService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TaskService tasks;
    private final PdfExporter pdf;
    private final Lookup lookup;

    @Transactional(readOnly = true)
    public byte[] generate(Long internshipId) {
        var stage = lookup.internship(internshipId);
        List<Task> liste = tasks.allOf(internshipId);
        var synthese = tasks.summary(internshipId);

        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Task t : liste) {
            Map<String, Object> l = new HashMap<>();
            l.put("date", t.getTaskDate().format(JOUR));
            l.put("titre", t.getTitle());
            l.put("description", t.getDescription() == null ? "" : t.getDescription());
            l.put("heures", t.getHours());
            l.put("statut", t.getStatus().name());
            l.put("libelle", libelle(t.getStatus()));
            lignes.add(l);
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("titre", stage.title());
        vars.put("etudiant", stage.studentName());
        vars.put("type", "PFE".equals(stage.type()) ? "Projet de fin d'etudes" : "Stage d'ete");
        vars.put("entreprise", nvl(stage.companyName()));
        vars.put("encadrant", nvl(stage.supervisorName()));
        vars.put("periode", periode(stage.startDate(), stage.endDate()));
        vars.put("taches", lignes);
        vars.put("total", synthese.total());
        vars.put("validees", synthese.validated());
        vars.put("attente", synthese.pending());
        vars.put("refusees", synthese.rejected());
        vars.put("heures", synthese.validatedHours());
        vars.put("genereLe", "Genere le " + LocalDate.now().format(JOUR));

        return pdf.render("journal", vars);
    }

    private String libelle(TaskStatus s) {
        return switch (s) {
            case VALIDATED -> "Validee";
            case REJECTED  -> "Refusee";
            case PENDING   -> "En attente";
        };
    }

    private String periode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) return "Non definie";
        return "du " + debut.format(JOUR) + " au " + fin.format(JOUR);
    }

    private String nvl(String s) { return s == null || s.isBlank() ? "Non renseigne" : s; }
}
