package com.gestionstages.document.enums;

/**
 * Les roles de la plateforme, redeclares ici pour que le moteur de
 * workflow puisse exprimer ses regles sans dependre d'un artefact commun.
 */
public enum Role {
    ETUDIANT,
    ENTREPRISE,
    ENCADRANT,
    CHEF_DEPARTEMENT_STAGE,
    CHEF_DEPARTEMENT_PEDAGOGIQUE,
    ADMIN;

    public static Role of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
