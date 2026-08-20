package com.gestionstages.auth.entity;

import com.gestionstages.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Identite et credentials d'un utilisateur.
 *
 * Ce service ne stocke QUE ce qui sert a s'authentifier. Les donnees de
 * profil (classe, departement, adresse de l'entreprise, telephone...)
 * appartiennent au user-service : un microservice, une responsabilite.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    /** Hash BCrypt. Le mot de passe en clair n'est jamais persiste ni journalise. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;

    /** Un compte desactive ne peut plus se connecter, sans etre supprime. */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
