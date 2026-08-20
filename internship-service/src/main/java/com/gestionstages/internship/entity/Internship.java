package com.gestionstages.internship.entity;

import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.InternshipType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un stage, de la demande initiale a la cloture.
 *
 * Les libelles (nom de l'etudiant, de l'entreprise, de l'encadrant) sont
 * DENORMALISES volontairement : sans eux, afficher une liste de trente
 * stages declencherait trente appels vers user-service. Les identifiants
 * restent la source de verite.
 */
@Entity
@Table(name = "internships", indexes = {
        @Index(name = "idx_internship_student", columnList = "student_id"),
        @Index(name = "idx_internship_company", columnList = "company_id"),
        @Index(name = "idx_internship_status",  columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Etudiant (source de verite : user-service) ----
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 120)
    private String studentName;

    @Column(name = "student_email", nullable = false, length = 120)
    private String studentEmail;

    // ---- Nature du stage ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InternshipType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    // ---- Entreprise d'accueil ----
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_name", length = 150)
    private String companyName;

    // Informations saisies par l'etudiant quand l'entreprise d'accueil
    // n'est pas encore referencee sur la plateforme. Le cahier des charges
    // les exige : un etudiant trouve souvent son stage dans une structure
    // qui n'a aucun compte chez nous.
    @Column(name = "company_address", length = 255)
    private String companyAddress;

    @Column(name = "company_email", length = 120)
    private String companyEmail;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    // Contact de l'encadrant en entreprise, avant qu'un compte ne lui
    // soit cree.
    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    // ---- Encadrant, designe par l'entreprise a l'acceptation ----
    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "supervisor_name", length = 120)
    private String supervisorName;

    // ---- Periode ----
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // ---- Etat ----
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InternshipStatus status = InternshipStatus.DRAFT;

    /** Renseigne uniquement pour REJECTED et REFUSED. */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Builder.Default
    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<StatusHistory> history = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentRequest> documentRequests = new ArrayList<>();

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
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
