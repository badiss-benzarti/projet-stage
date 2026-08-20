package com.gestionstages.evaluation.enums;

/**
 * Cycle de vie d'une reclamation, avec bouclage.
 *
 *   OPEN -> IN_REVIEW -> RESPONDED -> CLOSED
 *                            |
 *                            +-- REOPENED --> RESPONDED  (le bouclage)
 *
 * L'etudiant relance tant qu'il n'est pas satisfait ; seul le chef de
 * departement pedagogique cloture.
 */
public enum ClaimStatus {

    /** Deposee par l'etudiant. */
    OPEN,

    /** Prise en charge par le departement pedagogique. */
    IN_REVIEW,

    /** Une reponse a ete apportee. */
    RESPONDED,

    /** L'etudiant relance : c'est le bouclage. */
    REOPENED,

    /** Dossier clos. */
    CLOSED;

    public boolean isClosed() {
        return this == CLOSED;
    }
}
