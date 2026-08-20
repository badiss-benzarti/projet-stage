package com.gestionstages.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Une notification destinee a un utilisateur ou a un role.
 *
 * Le destinataire est designe par son EMAIL et non par un identifiant :
 * l'evenement RabbitMQ porte l'email de l'etudiant, et le jeton JWT porte
 * le meme email comme sujet. Cela evite un appel vers user-service pour
 * traduire un identifiant metier en compte.
 *
 * recipientRole permet les notifications collectives : "toutes les
 * demandes en attente" concerne le role, pas une personne.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_email", columnList = "recipient_email"),
        @Index(name = "idx_notif_role",  columnList = "recipient_role"),
        @Index(name = "idx_notif_read",  columnList = "is_read")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Destinataire nominatif. Null pour une notification de role. */
    @Column(name = "recipient_email", length = 120)
    private String recipientEmail;

    /** Destinataire collectif. Null pour une notification nominative. */
    @Column(name = "recipient_role", length = 40)
    private String recipientRole;

    /** Cle de routage de l'evenement d'origine : stage.approved, etc. */
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    /** Permet au frontend de renvoyer vers le dossier concerne. */
    @Column(name = "internship_id")
    private Long internshipId;

    /** "read" est un mot reserve en SQL : la colonne s'appelle is_read. */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
