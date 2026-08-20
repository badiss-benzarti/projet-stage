package com.gestionstages.internship.enums;

/** Cycle de vie d'une demande de document administratif. */
public enum RequestStatus {

    /** Demandee par l'etudiant, en attente du service des stages. */
    PENDING,

    /** Document edite et mis a disposition. */
    ISSUED,

    /** Demande refusee : motif obligatoire. */
    REJECTED
}
