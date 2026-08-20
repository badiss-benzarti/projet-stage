package com.gestionstages.auth.enums;

/**
 * Les six roles de la plateforme.
 *
 * Le nom de l'enum est stocke tel quel en base et place dans le jeton JWT.
 * Spring Security attend un prefixe ROLE_ pour @PreAuthorize("hasRole(...)"),
 * d'ou la methode authority().
 */
public enum Role {

    /** Depose une demande de stage, saisit son journal, consulte sa note. */
    ETUDIANT,

    /** Service RH de l'entreprise d'accueil : accepte ou refuse un stagiaire. */
    ENTREPRISE,

    /** Encadrant en entreprise : valide les taches, remplit la grille. */
    ENCADRANT,

    /** Chef du departement des stages : approuve les demandes et les documents. */
    CHEF_DEPARTEMENT_STAGE,

    /** Chef du departement pedagogique : notes, reclamations, statistiques. */
    CHEF_DEPARTEMENT_PEDAGOGIQUE,

    /** Administrateur technique. */
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
