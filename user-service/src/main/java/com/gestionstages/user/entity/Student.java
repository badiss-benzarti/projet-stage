package com.gestionstages.user.entity;

import com.gestionstages.user.enums.Governorate;
import com.gestionstages.user.enums.InstitutionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Profil etudiant.
 *
 * Les credentials (email, mot de passe, role) vivent dans l'auth-service ;
 * ce service ne detient que les donnees de scolarite.
 */
@Entity
@Table(name = "students",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_students_user", columnNames = "user_id"),
           @UniqueConstraint(name = "uk_students_cin", columnNames = "cin")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte ETUDIANT correspondant dans l'auth-service. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    /** Numero de carte d'identite, exige sur la convention de stage. */
    @Column(length = 20)
    private String cin;

    /** Classe : 4SAE3, 5SIM1... */
    @Column(nullable = false, length = 20)
    private String classe;

    @Column(nullable = false, length = 80)
    private String departement;

    // ---- Etablissement d'origine ----

    @Column(name = "institution_name", length = 150)
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", length = 20)
    private InstitutionType institutionType;

    /** Niveau d'etudes exprime en annees apres le baccalaureat. */
    @Column(name = "academic_level")
    private Integer academicLevel;

    // ---- Adresse ----

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Governorate governorate;

    // ---- Photo ----

    /**
     * Nom du fichier sur disque, jamais le nom d'origine : celui-ci
     * permettrait d'ecrire hors du repertoire de stockage.
     */
    @Column(name = "photo_name", length = 120)
    private String photoName;

    @Column(name = "photo_content_type", length = 100)
    private String photoContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public String fullName() { return firstName + " " + lastName; }
}
