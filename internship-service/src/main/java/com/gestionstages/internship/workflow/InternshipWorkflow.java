package com.gestionstages.internship.workflow;

import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.gestionstages.internship.enums.InternshipStatus.*;
import static com.gestionstages.internship.enums.Role.*;

/**
 * Table des transitions du workflow de stage.
 *
 * Toute la regle metier tient dans cette table : un etat de depart, un
 * etat d'arrivee, les roles habilites, et l'obligation eventuelle d'un
 * motif. Le service n'a plus qu'a interroger cette table, ce qui rend le
 * workflow lisible d'un coup d'oeil et testable sans base de donnees.
 *
 * ADMIN traverse toutes les transitions : c'est le role de deblocage.
 */
@Component
public class InternshipWorkflow {

    private static final Map<InternshipStatus, List<Transition>> TABLE = Map.of(

            // Le service des stages instruit d'abord ; l'entreprise n'est
            // sollicitee qu'une fois le dossier valide par l'ecole.
            DRAFT, List.of(
                    new Transition(SUBMITTED, Set.of(ETUDIANT), false,
                            "Envoyer ma demande",
                            "Votre dossier part au service des stages, qui "
                                    + "l'examine avant de le transmettre a "
                                    + "l'entreprise. Vous ne pourrez plus le "
                                    + "modifier : relisez-le avant d'envoyer.")),

            // Deux issues seulement pour le service des stages : il
            // accepte, et le dossier part chez l'entreprise, ou il
            // refuse. L'instruction en trois temps - prendre en charge,
            // approuver, transmettre - imposait trois clics pour un seul
            // arbitrage, sans que les etats intermediaires servent a
            // quiconque.
            SUBMITTED, List.of(
                    new Transition(COMPANY_PENDING, Set.of(CHEF_DEPARTEMENT_STAGE), false,
                            "Accepter la demande",
                            "Vous validez le dossier et le transmettez a "
                                    + "l'entreprise, a qui revient la decision "
                                    + "finale d'accueillir le stagiaire."),
                    new Transition(REJECTED, Set.of(CHEF_DEPARTEMENT_STAGE), true,
                            "Refuser la demande",
                            "Le dossier est clos et l'etudiant recoit votre motif. "
                                    + "L'entreprise n'en sera pas informee. "
                                    + "Le motif est obligatoire.")),

            // L'entreprise, et elle seule, accepte ou refuse le stagiaire.
            COMPANY_PENDING, List.of(
                    new Transition(ACCEPTED, Set.of(ENTREPRISE), false,
                            "Accepter cet etudiant en stage",
                            "Vous prenez l'etudiant en stage. L'encadrant qu'il a "
                                    + "choisi lui est affecte, et le stage peut demarrer."),
                    new Transition(REFUSED, Set.of(ENTREPRISE), true,
                            "Refuser cette demande",
                            "L'etudiant est prevenu avec votre motif et devra "
                                    + "chercher une autre entreprise. Le motif est obligatoire.")),

            ACCEPTED, List.of(
                    new Transition(IN_PROGRESS, Set.of(CHEF_DEPARTEMENT_STAGE, ENTREPRISE), false,
                            "Demarrer le stage",
                            "Le stage commence : l'etudiant peut des maintenant "
                                    + "remplir son journal de stage.")),

            IN_PROGRESS, List.of(
                    new Transition(COMPLETED, Set.of(ENCADRANT, CHEF_DEPARTEMENT_STAGE), false,
                            "Cloturer le stage",
                            "Le stage est termine. Le journal se ferme et la note "
                                    + "de l'evaluation devient definitive."))
    );

    /** Transitions possibles depuis un etat, tous roles confondus. */
    public List<Transition> from(InternshipStatus status) {
        return TABLE.getOrDefault(status, List.of());
    }

    /** Transitions que CE role peut declencher depuis CET etat. */
    public List<Transition> availableFor(InternshipStatus status, Role role) {
        return from(status).stream().filter(t -> t.isAllowedFor(role)).toList();
    }

    /** Cherche la transition menant a l'etat cible, si elle existe. */
    public Optional<Transition> find(InternshipStatus from, InternshipStatus to) {
        return from(from).stream().filter(t -> t.to() == to).findFirst();
    }
}
