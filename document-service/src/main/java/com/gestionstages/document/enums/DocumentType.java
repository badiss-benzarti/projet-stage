package com.gestionstages.document.enums;

/** Les documents echanges autour d'un stage. */
public enum DocumentType {

    /** Convention de stage signee par l'entreprise. */
    CONVENTION,

    /** Lettre d'affectation signee. */
    LETTRE_AFFECTATION,

    /**
     * Journal de stage rendu signe.
     *
     * A ne pas confondre avec le PDF que produit evaluation-service :
     * celui-ci est genere depuis les taches saisies, imprime, puis
     * signe par le maitre de stage et cachete par l'entreprise avant
     * d'etre redepose ici. C'est la version signee qui fait foi.
     */
    JOURNAL,

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

    /** Documents que la plateforme sait produire elle-meme. */
    public boolean isGenerable() {
        return this == ATTESTATION || this == LETTRE_AFFECTATION;
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
