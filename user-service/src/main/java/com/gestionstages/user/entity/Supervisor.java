package com.gestionstages.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Encadrant en entreprise : valide les taches du journal et remplit la
 * grille d'evaluation. Rattache a une entreprise.
 */
@Entity
@Table(name = "supervisors",
       uniqueConstraints = @UniqueConstraint(name = "uk_supervisors_user", columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte ENCADRANT correspondant dans l'auth-service. */
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

    /** Fonction dans l'entreprise : "Ingenieur DevOps", "Chef de projet"... */
    @Column(length = 100)
    private String position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_supervisor_company"))
    private Company company;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public String fullName() { return firstName + " " + lastName; }
}
