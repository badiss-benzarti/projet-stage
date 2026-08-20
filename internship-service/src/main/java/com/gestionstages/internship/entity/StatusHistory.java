package com.gestionstages.internship.entity;

import com.gestionstages.internship.enums.InternshipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Trace d'une transition du workflow.
 *
 * Repond a la question que le jury posera : "qui a approuve ce stage, et
 * quand ?". Sans cet historique, le workflow n'est qu'une colonne qui
 * change de valeur sans laisser de trace.
 */
@Entity
@Table(name = "status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internship_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_history_internship"))
    private Internship internship;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private InternshipStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private InternshipStatus toStatus;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 120)
    private String actorName;

    @Column(name = "actor_role", nullable = false, length = 40)
    private String actorRole;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
