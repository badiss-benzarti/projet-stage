package com.gestionstages.document.dto;

import com.gestionstages.document.entity.Document;
import com.gestionstages.document.enums.DocumentStatus;
import com.gestionstages.document.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DocumentDto {

    private DocumentDto() {}

    /** Decision du service des stages sur un depot. */
    public record Decision(
            @NotNull(message = "La decision est obligatoire") DocumentStatus status,
            @Size(max = 1000) String reason
    ) {}

    public record Response(
            Long id, Long internshipId, Long studentId, String studentName,
            DocumentType type, String originalName, String contentType, Long sizeBytes,
            DocumentStatus status, String rejectionReason, boolean generated,
            String uploadedBy, String validatedBy, String createdAt
    ) {
        public static Response from(Document d) {
            return new Response(d.getId(), d.getInternshipId(), d.getStudentId(), d.getStudentName(),
                    d.getType(), d.getOriginalName(), d.getContentType(), d.getSizeBytes(),
                    d.getStatus(), d.getRejectionReason(), d.isGenerated(),
                    d.getUploadedBy(), d.getValidatedBy(),
                    d.getCreatedAt() == null ? null : d.getCreatedAt().toString());
        }
    }
}
