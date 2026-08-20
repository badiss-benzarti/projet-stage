package com.gestionstages.evaluation.entity;

import com.gestionstages.evaluation.enums.ClaimStatus;
import com.gestionstages.evaluation.enums.ClaimType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reclamation d'un etudiant, avec bouclage.
 *
 * Le cahier des charges exige explicitement le bouclage : ce n'est pas un
 * formulaire a sens unique mais un fil d'echanges. Chaque message est un
 * ClaimMessage horodate, et l'etudiant peut relancer tant que le dossier
 * n'est pas clos.
 */
@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_student", columnList = "student_id"),
        @Index(name = "idx_claim_status",  columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internship_id", nullable = false)
    private Long internshipId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 120)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.OPEN;

    @Builder.Default
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ClaimMessage> messages = new ArrayList<>();

    /** Nombre de relances de l'etudiant : mesure concrete du bouclage. */
    @Builder.Default
    @Column(name = "reopen_count", nullable = false)
    private int reopenCount = 0;

    @Column(name = "closed_at")
    private Instant closedAt;

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
