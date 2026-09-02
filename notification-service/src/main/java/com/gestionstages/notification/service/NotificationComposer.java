package com.gestionstages.notification.service;

import com.gestionstages.notification.entity.InternshipEvent;
import com.gestionstages.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduit un evenement technique en notifications lisibles.
 *
 * Toute la regle "qui doit etre prevenu, et avec quel texte" tient dans
 * cette classe. Elle ne touche ni la base ni RabbitMQ, ce qui la rend
 * testable en isolation.
 */
@Component
public class NotificationComposer {

    private static final String ENTREPRISE  = "ENTREPRISE";
    private static final String CHEF_STAGE  = "CHEF_DEPARTEMENT_STAGE";
    private static final String CHEF_PEDAGO = "CHEF_DEPARTEMENT_PEDAGOGIQUE";

    public List<Notification> compose(InternshipEvent e) {
        List<Notification> notifications = new ArrayList<>();

        switch (e.eventType()) {

            case "stage.submitted" -> notifications.add(pourRole(e, CHEF_STAGE,
                    "Nouvelle demande de stage",
                    e.studentName() + " a soumis une demande de stage : " + e.title()));

            // Le dossier, valide par l'ecole, arrive chez l'entreprise.
            // L'etudiant est prevenu au meme moment : c'est sa seule
            // nouvelle entre l'envoi et la reponse de l'entreprise.
            case "stage.company.pending" -> {
                notifications.add(pourRole(e, ENTREPRISE,
                        "Nouvelle demande de stage",
                        e.studentName() + " demande un stage chez vous : " + e.title()));
                notifications.add(pourEtudiant(e,
                        "Votre demande est acceptee par l'ecole",
                        "Le service des stages a valide votre dossier. Il est transmis a "
                                + nvl(e.companyName(), "l'entreprise d'accueil")
                                + ", qui doit maintenant se prononcer."));
            }

            case "stage.approved" -> notifications.add(pourEtudiant(e,
                    "Votre demande de stage est approuvee",
                    "Le service des stages a approuve votre demande. Elle est transmise a "
                            + nvl(e.companyName(), "l'entreprise d'accueil") + "."));

            case "stage.rejected" -> notifications.add(pourEtudiant(e,
                    "Votre demande de stage est refusee",
                    "Motif : " + nvl(e.comment(), "non precise")));

            case "stage.company.accepted" -> {
                notifications.add(pourEtudiant(e,
                        "Votre stage est accepte",
                        nvl(e.companyName(), "L'entreprise") + " vous accepte comme stagiaire."));
                notifications.add(pourRole(e, CHEF_STAGE,
                        "Stage accepte par l'entreprise",
                        nvl(e.companyName(), "Une entreprise") + " a accepte "
                                + e.studentName() + "."));
            }

            case "stage.company.refused" -> {
                notifications.add(pourEtudiant(e,
                        "Votre stage est refuse par l'entreprise",
                        "Motif : " + nvl(e.comment(), "non precise")
                                + ". Rapprochez-vous du service des stages."));
                notifications.add(pourRole(e, CHEF_STAGE,
                        "Stage refuse par l'entreprise",
                        e.studentName() + " doit etre reoriente."));
            }

            case "stage.completed" -> {
                notifications.add(pourEtudiant(e,
                        "Votre stage est cloture",
                        "Votre stage est termine. L'evaluation sera disponible des sa validation."));
                notifications.add(pourRole(e, CHEF_PEDAGO,
                        "Stage cloture",
                        "Le stage de " + e.studentName() + " est cloture, evaluation a suivre."));
            }

            // Un evenement inconnu ne produit rien plutot que de faire
            // echouer le traitement : le publieur peut evoluer sans
            // casser ce service.
            default -> { }
        }

        return notifications;
    }

    private Notification pourEtudiant(InternshipEvent e, String titre, String message) {
        return Notification.builder()
                .recipientEmail(e.studentEmail())
                .eventType(e.eventType())
                .title(titre)
                .message(message)
                .internshipId(e.internshipId())
                .build();
    }

    private Notification pourRole(InternshipEvent e, String role, String titre, String message) {
        return Notification.builder()
                .recipientRole(role)
                .eventType(e.eventType())
                .title(titre)
                .message(message)
                .internshipId(e.internshipId())
                .build();
    }

    private String nvl(String valeur, String defaut) {
        return (valeur == null || valeur.isBlank()) ? defaut : valeur;
    }
}
