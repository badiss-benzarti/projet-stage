package com.gestionstages.internship.dto;

import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.InternshipType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public final class InternshipDto {

    private InternshipDto() {}

    /** Creation ou mise a jour d'un brouillon. */
    public record Request(
            @NotNull(message = "Le type de stage est obligatoire") InternshipType type,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$",
                    message = "Annee universitaire attendue au format 2026-2027") String academicYear,
            Long companyId,
            @Size(max = 150) String companyName,
            @Size(max = 255) String companyAddress,
            @Email(message = "Email de l'entreprise invalide") @Size(max = 120) String companyEmail,
            @Size(max = 30) String companyPhone,
            @Size(max = 120) String contactName,
            @Email(message = "Email de l'encadrant invalide") @Size(max = 120) String contactEmail,
            @Size(max = 30) String contactPhone,
            // Pas de contrainte de date future : une inscription tardive, sur un
            // stage deja commence, est un cas reel. La coherence debut/fin est
            // verifiee dans le service.
            LocalDate startDate,
            LocalDate endDate
    ) {}

    /** Declenchement d'une transition du workflow. */
    public record TransitionRequest(
            @NotNull(message = "L'etat cible est obligatoire") InternshipStatus target,
            @Size(max = 1000) String comment,
            Long supervisorId,
            @Size(max = 120) String supervisorName
    ) {}

    public record HistoryEntry(
            InternshipStatus fromStatus, InternshipStatus toStatus,
            String actorName, String actorRole, String comment, String at
    ) {}

    /** Une action proposee au frontend pour l'utilisateur courant. */
    public record AvailableAction(
            InternshipStatus target, String label, boolean requiresReason
    ) {}

    public record Response(
            Long id,
            Long studentId, String studentName, String studentEmail,
            InternshipType type, String title, String description, String academicYear,
            Long companyId, String companyName,
            String companyAddress, String companyEmail, String companyPhone,
            String contactName, String contactEmail, String contactPhone,
            Long supervisorId, String supervisorName,
            LocalDate startDate, LocalDate endDate,
            InternshipStatus status, String rejectionReason,
            List<AvailableAction> availableActions,
            List<HistoryEntry> history
    ) {
        public static Response from(Internship i, List<AvailableAction> actions, List<HistoryEntry> history) {
            return new Response(
                    i.getId(), i.getStudentId(), i.getStudentName(), i.getStudentEmail(),
                    i.getType(), i.getTitle(), i.getDescription(), i.getAcademicYear(),
                    i.getCompanyId(), i.getCompanyName(),
                    i.getCompanyAddress(), i.getCompanyEmail(), i.getCompanyPhone(),
                    i.getContactName(), i.getContactEmail(), i.getContactPhone(),
                    i.getSupervisorId(), i.getSupervisorName(),
                    i.getStartDate(), i.getEndDate(),
                    i.getStatus(), i.getRejectionReason(),
                    actions, history);
        }

        /** Version allegee pour les listes : ni actions ni historique. */
        public static Response summary(Internship i) {
            return from(i, List.of(), List.of());
        }
    }
}
