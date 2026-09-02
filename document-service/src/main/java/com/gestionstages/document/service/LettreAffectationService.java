package com.gestionstages.document.service;

import com.gestionstages.document.client.InternshipClient;
import com.gestionstages.document.client.Lookup;
import com.gestionstages.document.entity.Document;
import com.gestionstages.document.enums.DocumentStatus;
import com.gestionstages.document.enums.DocumentType;
import com.gestionstages.document.exception.ApiExceptions;
import com.gestionstages.document.export.PdfExporter;
import com.gestionstages.document.repository.DocumentRepository;
import com.gestionstages.document.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Lettre d'affectation, editee par le service des stages.
 *
 * Elle affecte nominativement un etudiant a une entreprise pour une
 * periode donnee, et mentionne l'assurance souscrite par l'ecole.
 * Contrairement a l'attestation de stage, qui certifie apres coup un
 * stage accompli par l'entreprise, celle-ci est delivree AVANT ou
 * PENDANT le stage, et c'est l'ecole qui la signe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LettreAffectationService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentRepository documents;
    private final StorageService storage;
    private final PdfExporter pdf;
    private final Lookup lookup;

    @Value("${app.lettre.etablissement:esprit}")
    private String etablissement;

    @Value("${app.lettre.lieu:Tunis}")
    private String lieu;

    @Value("${app.lettre.direction:Direction des Stages}")
    private String direction;

    @Value("${app.lettre.assureur:}")
    private String assureur;

    /** Numero du contrat d'assurance de l'ecole ; vide, la mention disparait. */
    @Value("${app.lettre.contrat-assurance:}")
    private String contratAssurance;

    @Transactional
    public Document generate(AuthenticatedUser me, Long internshipId) {
        InternshipClient.InternshipRef stage = lookup.internship(internshipId);

        // La lettre nomme l'entreprise d'accueil : elle n'a de sens
        // qu'une fois celle-ci connue et le dossier accorde.
        if (!accorde(stage.status())) {
            throw new ApiExceptions.BusinessRuleException(
                    "La lettre d'affectation n'est editee qu'une fois le stage accorde "
                            + "par l'entreprise (etat actuel : " + stage.status() + ")");
        }

        // Une lettre deja editee est remplacee : les dates ou l'entreprise
        // ont pu changer entre-temps.
        documents.findFirstByInternshipIdAndTypeAndStatus(
                internshipId, DocumentType.LETTRE_AFFECTATION, DocumentStatus.APPROVED)
                .ifPresent(ancienne -> {
                    storage.delete(ancienne.getStoredName());
                    documents.delete(ancienne);
                });

        byte[] contenu = pdf.render("lettre-affectation", variables(stage, me));
        String storedName = storage.storeBytes(contenu, ".pdf");

        Document d = Document.builder()
                .internshipId(internshipId)
                .studentId(stage.studentId())
                .studentName(stage.studentName())
                .type(DocumentType.LETTRE_AFFECTATION)
                .originalName("lettre-affectation-" + internshipId + ".pdf")
                .storedName(storedName)
                .contentType("application/pdf")
                .sizeBytes((long) contenu.length)
                // Editee par l'ecole : valide d'office, il n'y a rien a verifier.
                .status(DocumentStatus.APPROVED)
                .generated(true)
                .uploadedBy(me.fullName())
                .validatedBy(me.fullName())
                .build();

        documents.save(d);
        log.info("Lettre d'affectation editee pour le stage {}", internshipId);
        return d;
    }

    private boolean accorde(String statut) {
        return "ACCEPTED".equals(statut)
                || "IN_PROGRESS".equals(statut)
                || "COMPLETED".equals(statut);
    }

    private Map<String, Object> variables(InternshipClient.InternshipRef stage, AuthenticatedUser me) {
        var etudiant = lookup.studentDetails(stage.studentId());

        Map<String, Object> v = new HashMap<>();
        v.put("etablissement", etablissement);
        v.put("lieuDate", lieu + ", le " + LocalDate.now().format(JOUR));

        v.put("entreprise", nvl(stage.companyName(), "l'entreprise d'accueil"));
        v.put("etudiant", stage.studentName());
        v.put("inscription", inscription(etudiant));

        v.put("debut", stage.startDate() == null ? "" : stage.startDate().format(JOUR));
        v.put("fin", stage.endDate() == null ? "" : stage.endDate().format(JOUR));

        v.put("assureur", assureur);
        v.put("contratAssurance", contratAssurance);

        v.put("signataire", me.fullName());
        v.put("direction", direction);
        v.put("reference", "Reference LA-" + stage.id() + "-" + LocalDate.now().getYear()
                + " | Document genere par la plateforme de gestion des stages");
        return v;
    }

    /**
     * "Inscrit(e) en" : la classe suffit quand elle existe, c'est ce que
     * porte l'imprime. A defaut on retombe sur la filiere.
     */
    private String inscription(com.gestionstages.document.client.UserClient.Ref etudiant) {
        if (etudiant == null) {
            return "";
        }
        if (etudiant.classe() != null && !etudiant.classe().isBlank()) {
            return etudiant.classe();
        }
        return "";
    }

    private String nvl(String v, String defaut) {
        return (v == null || v.isBlank()) ? defaut : v;
    }
}
