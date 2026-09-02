package com.gestionstages.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entreprise d'accueil.
 *
 * userId pointe vers le compte ENTREPRISE de l'auth-service. Il n'y a pas
 * de cle etrangere : chaque microservice possede sa propre base, le lien
 * se fait par identifiant logique.
 */
@Entity
@Table(name = "companies",
       uniqueConstraints = @UniqueConstraint(name = "uk_companies_user", columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte ENTREPRISE correspondant dans l'auth-service. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    /** Matricule fiscal, facultatif : utile sur la convention de stage. */
    @Column(name = "tax_id", length = 40)
    private String taxId;

    /**
     * Presentation libre de l'entreprise : activite, taille, ce qu'elle
     * propose aux stagiaires. Lue par l'etudiant qui choisit ou postuler,
     * elle n'entre dans aucun traitement - d'ou du texte libre plutot
     * qu'un jeu de champs.
     */
    @Column(length = 2000)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Supervisor> supervisors = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
