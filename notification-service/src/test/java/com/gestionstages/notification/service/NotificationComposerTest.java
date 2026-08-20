package com.gestionstages.notification.service;

import com.gestionstages.notification.entity.InternshipEvent;
import com.gestionstages.notification.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le composeur porte toute la regle "qui est prevenu, et avec quel
 * texte". Il ne touche ni la base ni RabbitMQ : testable en isolation.
 */
class NotificationComposerTest {

    private final NotificationComposer composer = new NotificationComposer();

    private InternshipEvent event(String type, String comment) {
        return new InternshipEvent(type, 42L, "Plateforme de gestion des stages",
                "UNDER_REVIEW", "APPROVED", 7L, "Ahmed Ben Salah", "ahmed@esprit.tn",
                1L, "SocieteTech Partner", "Mehdi Trabelsi", "CHEF_DEPARTEMENT_STAGE",
                comment, Instant.now());
    }

    @Test
    @DisplayName("une soumission previent le service des stages, pas l'etudiant")
    void submissionNotifiesTheDepartment() {
        List<Notification> n = composer.compose(event("stage.submitted", null));

        assertThat(n).hasSize(1);
        assertThat(n.get(0).getRecipientRole()).isEqualTo("CHEF_DEPARTEMENT_STAGE");
        assertThat(n.get(0).getRecipientEmail()).isNull();
        assertThat(n.get(0).getMessage()).contains("Ahmed Ben Salah");
    }

    @Test
    @DisplayName("une approbation previent l'etudiant nominativement")
    void approvalNotifiesTheStudent() {
        List<Notification> n = composer.compose(event("stage.approved", null));

        assertThat(n).hasSize(1);
        assertThat(n.get(0).getRecipientEmail()).isEqualTo("ahmed@esprit.tn");
        assertThat(n.get(0).getRecipientRole()).isNull();
    }

    @Test
    @DisplayName("un refus transmet le motif a l'etudiant")
    void rejectionCarriesTheReason() {
        List<Notification> n = composer.compose(
                event("stage.rejected", "Entreprise non conventionnee"));

        assertThat(n).hasSize(1);
        assertThat(n.get(0).getMessage()).contains("Entreprise non conventionnee");
    }

    @Test
    @DisplayName("un refus sans motif reste lisible plutot que d'afficher null")
    void missingReasonStaysReadable() {
        List<Notification> n = composer.compose(event("stage.rejected", null));

        assertThat(n.get(0).getMessage()).doesNotContain("null").contains("non precise");
    }

    @Test
    @DisplayName("une acceptation entreprise previent l'etudiant ET le departement")
    void companyAcceptanceNotifiesBoth() {
        List<Notification> n = composer.compose(event("stage.company.accepted", null));

        assertThat(n).hasSize(2);
        assertThat(n).anyMatch(x -> "ahmed@esprit.tn".equals(x.getRecipientEmail()));
        assertThat(n).anyMatch(x -> "CHEF_DEPARTEMENT_STAGE".equals(x.getRecipientRole()));
    }

    @Test
    @DisplayName("une cloture previent l'etudiant et le departement pedagogique")
    void completionNotifiesPedagogy() {
        List<Notification> n = composer.compose(event("stage.completed", null));

        assertThat(n).hasSize(2);
        assertThat(n).anyMatch(x -> "CHEF_DEPARTEMENT_PEDAGOGIQUE".equals(x.getRecipientRole()));
    }

    @Test
    @DisplayName("un evenement inconnu ne produit rien plutot que d'echouer")
    void unknownEventIsIgnored() {
        assertThat(composer.compose(event("stage.evenement.futur", null))).isEmpty();
    }

    @Test
    @DisplayName("chaque notification porte l'identifiant du stage, pour le lien du frontend")
    void everyNotificationCarriesTheInternshipId() {
        for (String type : List.of("stage.submitted", "stage.approved", "stage.rejected",
                "stage.company.accepted", "stage.company.refused", "stage.completed")) {
            assertThat(composer.compose(event(type, "motif")))
                    .as("evenement %s", type)
                    .isNotEmpty()
                    .allMatch(n -> n.getInternshipId().equals(42L))
                    .allMatch(n -> n.getEventType().equals(type));
        }
    }
}
