package com.gestionstages.internship.enums;

/** Documents administratifs que le service des stages delivre a un etudiant. */
public enum RequestType {

    /**
     * Lettre de demande de stage, remise a l'etudiant pour DEMARCHER les
     * entreprises. Elle precede donc le dossier : l'exiger sur un stage
     * approuve reviendrait a la demander une fois qu'elle ne sert plus.
     */
    DEMANDE_STAGE("Demande de stage"),

    /**
     * Attestation de scolarite : atteste que l'etudiant est inscrit cette
     * annee. Elle ne parle pas de stage du tout, donc elle ne depend
     * d'aucun dossier - un etudiant peut la reclamer a tout moment.
     */
    ATTESTATION_SCOLARITE("Attestation de scolarite"),

    /** Convention de stage : facultative selon le cahier des charges. */
    CONVENTION("Convention de stage"),

    /** Lettre d'affectation, editee par le service des stages. */
    LETTRE_AFFECTATION("Lettre d'affectation"),

    /**
     * Attestation de presence, delivree PENDANT le stage. A ne pas
     * confondre avec l'attestation de stage ci-dessous.
     */
    ATTESTATION_PRESENCE("Attestation de presence"),

    /**
     * Attestation de stage : l'entreprise certifie que le stage a bien
     * ete effectue chez elle. C'est donc ELLE qui la delivre, et non le
     * service des stages, et seulement une fois le stage cloture.
     */
    ATTESTATION_STAGE("Attestation de stage");

    private final String libelle;

    RequestType(String libelle) { this.libelle = libelle; }

    public String libelle() { return libelle; }

    /**
     * Vrai si le document ne peut etre edite que pour un stage identifie.
     *
     * Deux types y echappent : la demande de stage, reclamee avant meme
     * d'avoir trouve une entreprise, et l'attestation de scolarite, qui
     * ne concerne que l'inscription de l'etudiant.
     */
    public boolean requiresInternship() {
        return this != DEMANDE_STAGE && this != ATTESTATION_SCOLARITE;
    }

    /**
     * Vrai si c'est l'entreprise d'accueil qui delivre le document, et
     * non le service des stages. La demande ne part alors pas dans la
     * file d'instruction de l'ecole.
     */
    public boolean delivreeParEntreprise() {
        return this == ATTESTATION_STAGE;
    }
}
