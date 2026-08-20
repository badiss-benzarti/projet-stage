package com.gestionstages.evaluation.dto;

import com.gestionstages.evaluation.entity.Claim;
import com.gestionstages.evaluation.enums.ClaimStatus;
import com.gestionstages.evaluation.enums.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ClaimDto {

    private ClaimDto() {}

    public record Request(
            @NotNull(message = "Le stage concerne est obligatoire") Long internshipId,
            @NotNull(message = "Le type de reclamation est obligatoire") ClaimType type,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String message
    ) {}

    public record MessageRequest(
            @NotBlank(message = "Le message ne peut pas etre vide") @Size(max = 4000) String content
    ) {}

    public record Message(
            Long id, String authorName, String authorRole, String content, String at
    ) {}

    public record Response(
            Long id, Long internshipId, Long studentId, String studentName,
            ClaimType type, String subject, ClaimStatus status,
            int reopenCount, String createdAt, String closedAt,
            List<Message> messages
    ) {
        public static Response from(Claim c, List<Message> messages) {
            return new Response(c.getId(), c.getInternshipId(), c.getStudentId(), c.getStudentName(),
                    c.getType(), c.getSubject(), c.getStatus(), c.getReopenCount(),
                    c.getCreatedAt() == null ? null : c.getCreatedAt().toString(),
                    c.getClosedAt() == null ? null : c.getClosedAt().toString(),
                    messages);
        }

        public static Response summary(Claim c) {
            return from(c, List.of());
        }
    }
}
