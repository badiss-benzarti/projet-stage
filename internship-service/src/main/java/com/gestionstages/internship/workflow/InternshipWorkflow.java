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

            DRAFT, List.of(
                    new Transition(SUBMITTED, Set.of(ETUDIANT), false,
                            "Soumettre la demande")),

            SUBMITTED, List.of(
                    new Transition(UNDER_REVIEW, Set.of(CHEF_DEPARTEMENT_STAGE), false,
                            "Prendre en charge")),

            UNDER_REVIEW, List.of(
                    new Transition(APPROVED, Set.of(CHEF_DEPARTEMENT_STAGE), false,
                            "Approuver la demande"),
                    new Transition(REJECTED, Set.of(CHEF_DEPARTEMENT_STAGE), true,
                            "Refuser la demande")),

            APPROVED, List.of(
                    new Transition(COMPANY_PENDING, Set.of(CHEF_DEPARTEMENT_STAGE), false,
                            "Transmettre a l'entreprise")),

            COMPANY_PENDING, List.of(
                    new Transition(ACCEPTED, Set.of(ENTREPRISE), false,
                            "Accepter le stagiaire"),
                    new Transition(REFUSED, Set.of(ENTREPRISE), true,
                            "Refuser le stagiaire")),

            ACCEPTED, List.of(
                    new Transition(IN_PROGRESS, Set.of(CHEF_DEPARTEMENT_STAGE, ENTREPRISE), false,
                            "Demarrer le stage")),

            IN_PROGRESS, List.of(
                    new Transition(COMPLETED, Set.of(ENCADRANT, CHEF_DEPARTEMENT_STAGE), false,
                            "Cloturer le stage"))
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
