package com.gestionstages.user.enums;

/**
 * Les 24 gouvernorats de Tunisie.
 *
 * Enumeration plutot que texte libre : l'orthographe varie beaucoup
 * ("Le Kef", "Kef", "El Kef") et rendrait tout regroupement statistique
 * inexploitable pour le departement pedagogique.
 */
public enum Governorate {

    ARIANA("Ariana"),
    BEJA("Béja"),
    BEN_AROUS("Ben Arous"),
    BIZERTE("Bizerte"),
    GABES("Gabès"),
    GAFSA("Gafsa"),
    JENDOUBA("Jendouba"),
    KAIROUAN("Kairouan"),
    KASSERINE("Kasserine"),
    KEBILI("Kébili"),
    LE_KEF("Le Kef"),
    MAHDIA("Mahdia"),
    MANOUBA("La Manouba"),
    MEDENINE("Médenine"),
    MONASTIR("Monastir"),
    NABEUL("Nabeul"),
    SFAX("Sfax"),
    SIDI_BOUZID("Sidi Bouzid"),
    SILIANA("Siliana"),
    SOUSSE("Sousse"),
    TATAOUINE("Tataouine"),
    TOZEUR("Tozeur"),
    TUNIS("Tunis"),
    ZAGHOUAN("Zaghouan");

    private final String libelle;

    Governorate(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
