package com.gestionstages.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Un message du fil de reclamation. */
@Entity
@Table(name = "claim_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClaimMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_message_claim"))
    private Claim claim;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(name = "author_role", nullable = false, length = 40)
    private String authorRole;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
