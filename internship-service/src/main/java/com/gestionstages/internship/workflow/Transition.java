package com.gestionstages.internship.workflow;

import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.Role;

import java.util.Set;

/**
 * Une transition autorisee de la machine a etats.
 *
 * @param to            etat cible
 * @param allowedRoles  roles habilites a la declencher
 * @param requiresReason true si un motif ecrit est obligatoire (refus)
 * @param label         libelle affiche dans l'historique et le frontend
 */
public record Transition(
        InternshipStatus to,
        Set<Role> allowedRoles,
        boolean requiresReason,
        String label
) {
    public boolean isAllowedFor(Role role) {
        return role == Role.ADMIN || allowedRoles.contains(role);
    }
}
