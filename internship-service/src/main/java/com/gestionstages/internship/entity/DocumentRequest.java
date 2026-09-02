package com.gestionstages.internship.entity;

import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.enums.RequestType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Demande d'un document administratif au service des stages.
 *
 * Distincte du document-service : ici on gere la DEMANDE et son
 * instruction ; le fichier, lui, est produit et conserve par le
 * document-service.
 *
 * Le stage est FACULTATIF. Une demande de stage est reclamee avant que
 * l'etudiant ait trouve son entreprise : la rattacher a un dossier
 * obligerait a creer un dossier vide pour pouvoir demander la lettre qui
 * sert justement a le remplir. L'etudiant, lui, est toujours connu.
 */
@Entity
@Table(name = "document_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Etudiant demandeur : la seule partie toujours presente. */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 120)
    private String studentName;

    @Column(name = "student_email", length = 120)
    private String studentEmail;

    /** Nul pour une demande de stage, renseigne pour les autres types. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id",
                foreignKey = @ForeignKey(name = "fk_request_internship"))
    private Internship internship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    /** Motif obligatoire en cas de refus. */
    @Column(length = 1000)
    private String reason;

    @Column(name = "processed_by", length = 120)
    private String processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    /** Identifiant du fichier produit par le document-service, une fois delivre. */
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
