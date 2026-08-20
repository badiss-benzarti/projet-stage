package com.gestionstages.evaluation.dto;

import com.gestionstages.evaluation.entity.Evaluation;
import com.gestionstages.evaluation.enums.EvaluationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public final class EvaluationDto {

    private EvaluationDto() {}

    /** Saisie de la grille. La note finale n'est jamais envoyee par le client. */
    public record Request(
            @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double technicalScore,
            @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double qualityScore,
            @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double autonomyScore,
            @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double communicationScore,
            @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double punctualityScore,
            @Size(max = 2000) String globalComment,
            @Size(max = 2000) String remarks
    ) {}

    public record Response(
            Long id, Long internshipId, Long studentId, String studentName,
            String supervisorName, String companyName, String internshipType,
            Double technicalScore, Double qualityScore, Double autonomyScore,
            Double communicationScore, Double punctualityScore,
            String globalComment, String remarks,
            Double finalScore, EvaluationStatus status,
            Map<String, Object> breakdown
    ) {
        public static Response from(Evaluation e, Map<String, Object> breakdown) {
            return new Response(e.getId(), e.getInternshipId(), e.getStudentId(), e.getStudentName(),
                    e.getSupervisorName(), e.getCompanyName(), e.getInternshipType(),
                    e.getTechnicalScore(), e.getQualityScore(), e.getAutonomyScore(),
                    e.getCommunicationScore(), e.getPunctualityScore(),
                    e.getGlobalComment(), e.getRemarks(),
                    e.getFinalScore(), e.getStatus(), breakdown);
        }
    }
}
