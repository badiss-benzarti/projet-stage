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

    /** Rattachement du fichier produit, envoye par le document-service. */
    public record Issued(
            @NotNull(message = "L'identifiant du document est obligatoire") Long documentId
    ) {}

    public record Response(
            Long id,
            Long studentId, String studentName, String studentEmail,
            Long internshipId, String internshipTitle, String companyName,
            RequestType type, String typeLabel, RequestStatus status,
            String reason, String processedBy, String processedAt,
            Long documentId, String createdAt
    ) {
        public static Response from(DocumentRequest r) {
            var stage = r.getInternship();
            return new Response(
                    r.getId(),
                    r.getStudentId(), r.getStudentName(), r.getStudentEmail(),
                    stage == null ? null : stage.getId(),
                    stage == null ? null : stage.getTitle(),
                    stage == null ? null : stage.getCompanyName(),
                    r.getType(), r.getType().libelle(), r.getStatus(),
                    r.getReason(), r.getProcessedBy(),
                    r.getProcessedAt() == null ? null : r.getProcessedAt().toString(),
                    r.getDocumentId(),
                    r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        }
    }
}
