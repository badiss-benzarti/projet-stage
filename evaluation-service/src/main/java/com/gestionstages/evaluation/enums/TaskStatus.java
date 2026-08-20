package com.gestionstages.evaluation.enums;

/** Cycle de vie d'une tache du journal de stage. */
public enum TaskStatus {

    /** Saisie par l'etudiant, en attente de l'encadrant. */
    PENDING,

    /** Validee par l'encadrant d'entreprise. */
    VALIDATED,

    /** Refusee : motif obligatoire, l'etudiant peut corriger et resoumettre. */
    REJECTED
}
