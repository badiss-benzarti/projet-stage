package com.gestionstages.evaluation.enums;

/** Etat de la grille d'evaluation. */
public enum EvaluationStatus {

    /** Grille en cours de saisie par l'encadrant, modifiable. */
    DRAFT,

    /** Grille validee : la note est definitive et l'etudiant peut la voir. */
    SUBMITTED
}
