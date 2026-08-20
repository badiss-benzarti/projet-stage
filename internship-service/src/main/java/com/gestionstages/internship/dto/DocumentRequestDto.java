package com.gestionstages.internship.dto;

import com.gestionstages.internship.entity.DocumentRequest;
import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DocumentRequestDto {

    private DocumentRequestDto() {}

    public record Request(
            @NotNull(message = "Le type de document est obligatoire") RequestType type
    ) {}

    public record Decision(
            @NotNull(message = "La decision est obligatoire") RequestStatus status,
            @Size(max = 1000) String reason
    ) {}

    public record Response(
            Long id, Long internshipId, RequestType type, RequestStatus status,
            String reason, String processedBy, String createdAt
    ) {
        public static Response from(DocumentRequest r) {
            return new Response(
                    r.getId(), r.getInternship().getId(), r.getType(), r.getStatus(),
                    r.getReason(), r.getProcessedBy(),
                    r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        }
    }
}
