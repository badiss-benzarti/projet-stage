package com.gestionstages.internship.enums;

/**
 * Les dix etats du cycle de vie d'un stage.
 *
 *   DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED -> COMPANY_PENDING
 *         -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 *
 * Deux sorties laterales, toutes deux avec motif obligatoire :
 *   UNDER_REVIEW    -> REJECTED   (refus du service des stages)
 *   COMPANY_PENDING -> REFUSED    (refus de l'entreprise)
 *
 * Les transitions autorisees et les roles habilites sont declares dans
 * InternshipWorkflow, pas ici : l'enum reste une simple liste de valeurs.
 */
public enum InternshipStatus {

    /** Brouillon, visible du seul etudiant, modifiable librement. */
    DRAFT,

    /** Soumis au service des stages. */
    SUBMITTED,

    /** Pris en charge par le chef de departement des stages. */
    UNDER_REVIEW,

    /** Valide par le service des stages. */
    APPROVED,

    /** Refuse par le service des stages : motif obligatoire. */
    REJECTED,

    /** Transmis a l'entreprise, en attente de sa reponse. */
    COMPANY_PENDING,

    /** L'entreprise accepte le stagiaire et designe un encadrant. */
    ACCEPTED,

    /** L'entreprise refuse : motif obligatoire. */
    REFUSED,

    /** Stage en cours. */
    IN_PROGRESS,

    /** Stage termine, evaluation close. */
    COMPLETED;

    /** Un etat terminal n'admet plus aucune transition. */
    public boolean isTerminal() {
        return this == REJECTED || this == REFUSED || this == COMPLETED;
    }
}
