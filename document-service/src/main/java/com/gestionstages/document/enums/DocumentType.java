package com.gestionstages.document.enums;

/** Les quatre types de documents prevus par le cahier des charges. */
public enum DocumentType {

    /** Convention de stage signee par l'entreprise. */
    CONVENTION,

    /** Lettre d'affectation signee. */
    LETTRE_AFFECTATION,

    /** Rapport de stage final. */
    RAPPORT,

    /** Attestation de stage : deposee, ou generee par la plateforme. */
    ATTESTATION;

    /** Une attestation peut etre produite par la plateforme, pas les autres. */
    public boolean isGenerable() {
        return this == ATTESTATION;
    }
}
