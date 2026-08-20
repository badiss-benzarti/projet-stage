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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generation de l'attestation de stage en ligne.
 *
 * Le cahier des charges la reserve aux stages effectues a la DSI et chez
 * EspritTech : ce sont les structures internes, dont l'ecole peut attester
 * elle-meme. Pour un stage externe, l'attestation vient de l'entreprise et
 * doit etre DEPOSEE, pas generee.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationService {

    private static final DateTimeFormatter JOUR =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    private final DocumentRepository documents;
    private final StorageService storage;
    private final PdfExporter pdf;
    private final Lookup lookup;

    @Value("${app.attestation.structures-internes:DSI,EspritTech}")
    private List<String> structuresInternes;

    @Value("${app.attestation.etablissement:ESPRIT}")
    private String etablissement;

    @Value("${app.attestation.lieu:Tunis}")
    private String lieu;

    @Transactional
    public Document generate(AuthenticatedUser me, Long internshipId) {
        InternshipClient.InternshipRef stage = lookup.internship(internshipId);

        if (!estStructureInterne(stage.companyName())) {
            throw new ApiExceptions.BusinessRuleException(
                    "L'attestation n'est generee que pour les stages effectues a "
                            + String.join(" ou ", structuresInternes)
                            + ". Pour une structure externe, deposez l'attestation fournie par l'entreprise.");
        }
        if (!"COMPLETED".equals(stage.status())) {
            throw new ApiExceptions.BusinessRuleException(
                    "L'attestation n'est delivree qu'apres cloture du stage (etat actuel : "
                            + stage.status() + ")");
        }

        // Une attestation deja generee est remplacee : les donnees du stage
        // ont pu changer entre-temps.
        documents.findFirstByInternshipIdAndTypeAndStatus(
                internshipId, DocumentType.ATTESTATION, DocumentStatus.APPROVED)
                .ifPresent(ancienne -> {
                    storage.delete(ancienne.getStoredName());
                    documents.delete(ancienne);
                });

        byte[] contenu = pdf.render("attestation", variables(stage, me));
        String storedName = storage.storeBytes(contenu, ".pdf");

        Document d = Document.builder()
                .internshipId(internshipId)
                .studentId(stage.studentId())
                .studentName(stage.studentName())
                .type(DocumentType.ATTESTATION)
                .originalName("attestation-stage-" + internshipId + ".pdf")
                .storedName(storedName)
                .contentType("application/pdf")
                .sizeBytes((long) contenu.length)
                // Genere par la plateforme : valide d'office, il n'y a rien a verifier.
                .status(DocumentStatus.APPROVED)
                .generated(true)
                .uploadedBy(me.fullName())
                .validatedBy(me.fullName())
                .build();

        documents.save(d);
        log.info("Attestation generee pour le stage {}", internshipId);
        return d;
    }

    private boolean estStructureInterne(String entreprise) {
        if (entreprise == null) return false;
        String normalise = entreprise.toLowerCase();
        return structuresInternes.stream()
                .anyMatch(s -> normalise.contains(s.toLowerCase()));
    }

    private Map<String, Object> variables(InternshipClient.InternshipRef stage, AuthenticatedUser me) {
        Map<String, Object> v = new HashMap<>();
        v.put("etablissement", etablissement);
        v.put("service", stage.companyName());
        v.put("etudiant", stage.studentName());
        v.put("classe", "-");
        v.put("type", "PFE".equals(stage.type()) ? "Projet de fin d'etudes" : "Stage d'ete");
        v.put("sujet", stage.title());
        v.put("entreprise", stage.companyName());
        v.put("encadrant", stage.supervisorName() == null ? "-" : stage.supervisorName());
        v.put("periode", periode(stage));
        v.put("lieuDate", "Fait a " + lieu + ", le " + LocalDate.now().format(JOUR));
        v.put("signataire", me.fullName());
        v.put("reference", "Reference ATT-" + stage.id() + "-" + LocalDate.now().getYear()
                + " | Document genere par la plateforme de gestion des stages");
        return v;
    }

    private String periode(InternshipClient.InternshipRef stage) {
        if (stage.startDate() == null || stage.endDate() == null) return "Non precisee";
        return "du " + stage.startDate().format(JOUR) + " au " + stage.endDate().format(JOUR);
    }
}
