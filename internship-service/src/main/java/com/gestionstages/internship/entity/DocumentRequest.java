package com.gestionstages.internship.entity;

import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.enums.RequestType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Demande de convention ou de lettre d'affectation.
 *
 * Distincte du document-service : ici on gere la DEMANDE et son
 * instruction par le service des stages ; le fichier signe, lui, est
 * depose et valide dans le document-service.
 */
@Entity
@Table(name = "document_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internship_id", nullable = false,
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
