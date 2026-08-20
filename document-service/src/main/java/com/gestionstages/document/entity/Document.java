package com.gestionstages.document.entity;

import com.gestionstages.document.enums.DocumentStatus;
import com.gestionstages.document.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Un document depose ou genere pour un stage.
 *
 * Le fichier lui-meme vit sur le disque (volume Docker) ; la base ne
 * stocke que ses metadonnees et son chemin. Mettre des PDF en BLOB
 * ferait gonfler la base et ralentirait chaque requete.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_internship", columnList = "internship_id"),
        @Index(name = "idx_document_status",     columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internship_id", nullable = false)
    private Long internshipId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 120)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type;

    /** Nom donne par l'utilisateur, affiche dans l'interface. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Nom sur le disque : UUID, pour eviter toute collision et toute
     *  traversee de repertoire a partir du nom d'origine. */
    @Column(name = "stored_name", nullable = false, length = 120)
    private String storedName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    /** Motif obligatoire en cas de refus. */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /** Vrai si le fichier a ete produit par la plateforme. */
    @Builder.Default
    @Column(name = "is_generated", nullable = false)
    private boolean generated = false;

    @Column(name = "uploaded_by", length = 120)
    private String uploadedBy;

    @Column(name = "validated_by", length = 120)
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
