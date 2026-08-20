package com.gestionstages.evaluation.dto;

import com.gestionstages.evaluation.entity.Task;
import com.gestionstages.evaluation.enums.TaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class TaskDto {

    private TaskDto() {}

    public record Request(
            @NotNull(message = "La date de la tache est obligatoire")
            @PastOrPresent(message = "On ne saisit pas une tache future") LocalDate taskDate,

            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,

            @NotNull(message = "Le nombre d'heures est obligatoire")
            @DecimalMin(value = "0.5", message = "Une tache dure au moins 0,5 heure")
            @DecimalMax(value = "12.0", message = "Une tache ne peut pas depasser 12 heures")
            Double hours
    ) {}

    /** Decision de l'encadrant sur une tache. */
    public record Decision(
            @NotNull(message = "La decision est obligatoire") TaskStatus status,
            @Size(max = 1000) String reason
    ) {}

    public record Response(
            Long id, Long internshipId, LocalDate taskDate, String title, String description,
            Double hours, TaskStatus status, String rejectionReason,
            String validatedBy, String createdAt
    ) {
        public static Response from(Task t) {
            return new Response(t.getId(), t.getInternshipId(), t.getTaskDate(), t.getTitle(),
                    t.getDescription(), t.getHours(), t.getStatus(), t.getRejectionReason(),
                    t.getValidatedBy(), t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
        }
    }

    /** Synthese du journal, reutilisee par le PDF et par le modele de risque. */
    public record Summary(
            long total, long pending, long validated, long rejected,
            double validatedHours, LocalDate lastEntry
    ) {}
}
