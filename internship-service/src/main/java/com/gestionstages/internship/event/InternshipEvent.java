package com.gestionstages.internship.event;

import com.gestionstages.internship.enums.InternshipStatus;

import java.time.Instant;

/**
 * Message publie sur RabbitMQ a chaque transition du workflow.
 *
 * Volontairement autoportant : le consommateur doit pouvoir composer une
 * notification lisible sans requeter le internship-service.
 */
public record InternshipEvent(
        String eventType,
        Long internshipId,
        String title,
        InternshipStatus fromStatus,
        InternshipStatus toStatus,
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
