package com.gestionstages.notification.entity;

import java.time.Instant;

/**
 * Copie locale du message publie par internship-service.
 *
 * Volontairement dupliquee plutot que partagee : un contrat d'evenement
 * partage en bibliotheque recree exactement le couplage que la
 * communication asynchrone cherche a eviter. Les champs inconnus sont
 * ignores a la deserialisation, ce qui laisse le publieur evoluer.
 */
public record InternshipEvent(
        String eventType,
        Long internshipId,
        String title,
        String fromStatus,
        String toStatus,
        Long studentId,
        String studentName,
        String studentEmail,
        Long companyId,
        String companyName,
        String actorName,
        String actorRole,
        String comment,
        Instant occurredAt
) {}
