package com.gestionstages.document.enums;

/** Les documents echanges autour d'un stage. */
public enum DocumentType {

    /** Convention de stage signee par l'entreprise. */
    CONVENTION,

    /** Lettre d'affectation signee. */
    LETTRE_AFFECTATION,

    /** Rapport de stage final. */
    RAPPORT,

    /** Attestation de stage : deposee, ou generee par la plateforme. */
    ATTESTATION,

    /**
     * Lettre de motivation, jointe par l'etudiant a sa candidature.
     * Avec le CV du profil, c'est ce que l'entreprise lit avant de se
     * prononcer.
     */
    LETTRE_MOTIVATION,

    /** Attestation de scolarite, justifiant l'inscription de l'etudiant. */
    ATTESTATION_SCOLARITE;

    /** Une attestation peut etre produite par la plateforme, pas les autres. */
    public boolean isGenerable() {
        return this == ATTESTATION;
    }

    /**
     * Vrai si la piece constitue le dossier de candidature montre a
     * l'entreprise, par opposition aux documents administratifs produits
     * pendant ou apres le stage.
     */
    public boolean isCandidature() {
        return this == LETTRE_MOTIVATION || this == ATTESTATION_SCOLARITE;
    }
}
