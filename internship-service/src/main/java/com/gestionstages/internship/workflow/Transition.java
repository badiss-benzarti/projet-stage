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
 * @param label         libelle du bouton, a l'imperatif : ce que l'on fait
 * @param hint          une phrase expliquant la consequence de l'action,
 *                      affichee sous le bouton. Le libelle dit quoi, le
 *                      hint dit ce qui se passe ensuite et qui prend la
 *                      main : c'est ce qui evite d'avoir a deviner.
 */
public record Transition(
        InternshipStatus to,
        Set<Role> allowedRoles,
        boolean requiresReason,
        String label,
        String hint
) {
    public boolean isAllowedFor(Role role) {
        return role == Role.ADMIN || allowedRoles.contains(role);
    }
}
