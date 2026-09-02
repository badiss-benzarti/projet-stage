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

        // Profils detailles : absents si c'est l'etudiant qui telecharge,
        // les cases correspondantes restent alors vides.
        var etudiant = lookup.studentDetails(stage.studentId());
        var encadrant = stage.supervisorId() == null
                ? null : lookup.supervisorDetails(stage.supervisorId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("titre", stage.title());
        vars.put("etudiant", stage.studentName());
        vars.put("type", "PFE".equals(stage.type()) ? "Projet de fin d'etudes" : "Stage d'ete");
        vars.put("entreprise", nvl(stage.companyName()));
        vars.put("encadrant", nvl(stage.supervisorName()));
        vars.put("periode", periode(stage.startDate(), stage.endDate()));
        vars.put("taches", lignes);

        // --- Page de garde du formulaire ESPRIT ---------------------
        // Les trois cases a cocher de l'imprime ne correspondent pas une
        // pour une a nos deux types : un PFE est un stage ingenieur, un
        // stage d'ete une immersion en entreprise. La formation humaine
        // et sociale n'existe pas dans la plateforme, sa case reste vide.
        vars.put("caseIngenieur", "PFE".equals(stage.type()));
        vars.put("caseImmersion", !"PFE".equals(stage.type()));
        vars.put("caseHumaine", false);

        vars.put("identifiant", champ(etudiant == null ? null : etudiant.cin()));
        vars.put("classe", champ(etudiant == null ? null : etudiant.classe()));
        vars.put("sujet", champ(stage.title()));
        vars.put("debut", stage.startDate() == null ? "" : stage.startDate().format(JOUR));
        vars.put("fin", stage.endDate() == null ? "" : stage.endDate().format(JOUR));
        vars.put("organisme", champ(stage.companyName()));
        vars.put("maitreDeStage", champ(
                stage.supervisorName() != null ? stage.supervisorName() : stage.contactName()));
        vars.put("fonction", champ(encadrant == null ? null : encadrant.position()));
        vars.put("emailEntreprise", champ(stage.companyEmail()));
        vars.put("telephoneEntreprise", champ(stage.companyPhone()));
        vars.put("anneeUniversitaire", champ(stage.academicYear()));
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

    /**
     * Sur un formulaire, une information absente se laisse en blanc : on
     * la remplit a la main. Ecrire "Non renseigne" dans la case rendrait
     * le document inutilisable une fois imprime.
     */
    private String champ(String s) { return s == null || s.isBlank() ? "" : s; }
}
