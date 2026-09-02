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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Attestation de stage delivree par l'entreprise d'accueil.
 *
 * C'est elle qui certifie qu'un stage a bien ete effectue chez elle :
 * l'ecole ne peut pas l'attester a sa place. Le document reprend
 * l'imprime que les entreprises redigent a la main - signataire,
 * societe, etudiant, sujet, periode - et laisse le cachet et la
 * signature a apposer.
 *
 * Il est produit apres cloture du stage, a la demande de l'etudiant.
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

    @Value("${app.attestation.etablissement:ESPRIT}")
    private String etablissement;

    @Value("${app.attestation.lieu:Tunis}")
    private String lieu;

    @Transactional
    public Document generate(AuthenticatedUser me, Long internshipId) {
        InternshipClient.InternshipRef stage = lookup.internship(internshipId);

        // L'attestation est desormais delivree par l'entreprise d'accueil,
        // qui atteste d'un stage effectue chez elle. La restriction aux
        // structures internes n'a plus lieu d'etre : elle existait parce
        // que l'ecole ne pouvait pas attester a la place d'un tiers.
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

    private Map<String, Object> variables(InternshipClient.InternshipRef stage, AuthenticatedUser me) {
        var etudiant = lookup.studentDetails(stage.studentId());

        Map<String, Object> v = new HashMap<>();

        // L'entreprise signe : c'est son nom qui coiffe le document, et
        // le signataire est celui qui la represente - le contact declare
        // sur le dossier, ou a defaut la personne connectee.
        v.put("entreprise", nvl(stage.companyName(), "l'entreprise d'accueil"));
        v.put("adresseEntreprise", champ(stage.companyAddress()));
        v.put("signataire", nvl(stage.contactName(), me.fullName()));

        v.put("etudiant", stage.studentName());
        v.put("etablissement", etudiant == null || etudiant.institutionName() == null
                ? etablissement : etudiant.institutionName());
        v.put("filiere", champ(etudiant == null ? null : etudiant.classe()));
        v.put("cin", champ(etudiant == null ? null : etudiant.cin()));

        v.put("type", "PFE".equals(stage.type()) ? "de fin d'etudes" : "d'immersion");
        v.put("sujet", stage.title());
        v.put("duree", duree(stage));
        v.put("debut", stage.startDate() == null ? "" : stage.startDate().format(JOUR));
        v.put("fin", stage.endDate() == null ? "" : stage.endDate().format(JOUR));

        v.put("lieuDate", "Fait a " + lieu + ", le " + LocalDate.now().format(JOUR));
        v.put("reference", "Reference ATT-" + stage.id() + "-" + LocalDate.now().getYear()
                + " | Document genere par la plateforme de gestion des stages");
        return v;
    }

    /**
     * Duree en clair, comme sur les attestations manuscrites : on lit
     * "un mois et demi", pas "45 jours".
     */
    private String duree(InternshipClient.InternshipRef stage) {
        if (stage.startDate() == null || stage.endDate() == null) {
            return "";
        }
        long jours = ChronoUnit.DAYS.between(stage.startDate(), stage.endDate());
        long mois = jours / 30;
        long reste = jours % 30;

        if (mois == 0) {
            return jours + " jours";
        }
        String base = mois == 1 ? "un mois" : mois + " mois";
        if (reste >= 20) {
            return base + " et trois quarts";
        }
        if (reste >= 10) {
            return base + " et demi";
        }
        return base;
    }

    private String nvl(String v, String defaut) {
        return (v == null || v.isBlank()) ? defaut : v;
    }

    /** Une mention absente se laisse en blanc, comme sur l'imprime papier. */
    private String champ(String v) {
        return (v == null || v.isBlank()) ? "" : v;
    }
}
