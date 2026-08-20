package com.gestionstages.evaluation.entity;

import com.gestionstages.evaluation.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Grille d'evaluation remplie par l'encadrant d'entreprise.
 *
 * Cinq criteres notes sur 20. La note finale n'est PAS saisie : elle est
 * calculee par ScoringService a partir des poids definis dans
 * config-repo/evaluation-service.yml. Elle est persistee pour garder une
 * trace du bareme applique, meme si les poids changent plus tard.
 */
@Entity
@Table(name = "evaluations",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_evaluation_internship", columnNames = "internship_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Un stage, une seule grille. */
    @Column(name = "internship_id", nullable = false)
    private Long internshipId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 120)
    private String studentName;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "supervisor_name", length = 120)
    private String supervisorName;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "internship_type", length = 10)
    private String internshipType;

    // ---- Les cinq criteres, notes sur 20 ----

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "autonomy_score")
    private Double autonomyScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "punctuality_score")
    private Double punctualityScore;

    // ---- Appreciation, rubrique distincte exigee par le cahier ----

    @Column(name = "global_comment", length = 2000)
    private String globalComment;

    @Column(length = 2000)
    private String remarks;

    /** Calculee, jamais saisie. Arrondie a 0,25. */
    @Column(name = "final_score")
    private Double finalScore;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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

    /** Vrai lorsque les cinq criteres sont renseignes. */
    public boolean isComplete() {
        return technicalScore != null && qualityScore != null && autonomyScore != null
                && communicationScore != null && punctualityScore != null;
    }
}
