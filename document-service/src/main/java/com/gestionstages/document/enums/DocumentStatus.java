package com.gestionstages.document.enums;

/**
 * Cycle de vie d'un depot.
 *
 *   UPLOADED -> UNDER_REVIEW -> APPROVED
 *                            -> REJECTED (motif obligatoire)
 *
 * Un document refuse peut etre redepose : c'est la boucle de correction
 * prevue par le cahier des charges.
 */
public enum DocumentStatus {

    UPLOADED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    public boolean isFinal() {
        return this == APPROVED;
    }
}
