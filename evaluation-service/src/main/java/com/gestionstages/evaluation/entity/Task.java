package com.gestionstages.evaluation.entity;

import com.gestionstages.evaluation.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Une entree du journal de stage.
 *
 * L'etudiant saisit ce qu'il a fait, l'encadrant valide ou refuse. Ces
 * lignes alimentent aussi le modele de prediction du risque : nombre de
 * taches refusees, heures cumulees, retard de saisie.
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_internship", columnList = "internship_id"),
        @Index(name = "idx_task_status",     columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internship_id", nullable = false)
    private Long internshipId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** Jour couvert par la tache, pas la date de saisie. */
    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Duree consacree, en heures. */
    @Column(nullable = false)
    private Double hours;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.PENDING;

    /** Motif obligatoire en cas de refus. */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "validated_by", length = 120)
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

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
    void onUpdate() { this.updatedAt = Instant.now(); }
}
