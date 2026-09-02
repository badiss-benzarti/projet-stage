package com.gestionstages.user.dto;

import com.gestionstages.user.entity.Student;
import com.gestionstages.user.enums.Governorate;
import com.gestionstages.user.enums.InstitutionType;
import jakarta.validation.constraints.*;

/** Requetes et reponses du profil etudiant. */
public final class StudentDto {

    private StudentDto() {}

    public record Request(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Email @Size(max = 120) String email,

            @Pattern(regexp = "^$|^[0-9+ ]{8,20}$", message = "Numero de telephone invalide")
            String phone,

            @Pattern(regexp = "^$|^[0-9]{8}$", message = "Le CIN doit contenir 8 chiffres")
            String cin,

            @NotBlank @Size(max = 20) String classe,
            @NotBlank @Size(max = 80) String departement,

            // ---- Etablissement ----
            @Size(max = 150) String institutionName,
            InstitutionType institutionType,

            @Min(value = 1, message = "Le niveau va de Bac+1 a Bac+8")
            @Max(value = 8, message = "Le niveau va de Bac+1 a Bac+8")
            Integer academicLevel,

            // ---- Adresse ----
            @Size(max = 255) String address,
            @Size(max = 80) String city,
            Governorate governorate
    ) {}

    public record Response(
            Long id, Long userId, String firstName, String lastName,
            String email, String phone, String cin, String classe, String departement,
            String institutionName, InstitutionType institutionType, Integer academicLevel,
            String address, String city, Governorate governorate, String governorateLabel,
            boolean hasPhoto,
            boolean hasCv, String cvName
    ) {
        public static Response from(Student s) {
            return new Response(
                    s.getId(), s.getUserId(), s.getFirstName(), s.getLastName(),
                    s.getEmail(), s.getPhone(), s.getCin(), s.getClasse(), s.getDepartement(),
                    s.getInstitutionName(), s.getInstitutionType(), s.getAcademicLevel(),
                    s.getAddress(), s.getCity(), s.getGovernorate(),
                    s.getGovernorate() == null ? null : s.getGovernorate().libelle(),
                    s.getPhotoName() != null,
                    s.getCvName() != null, s.getCvOriginalName());
        }
    }

    /** Une valeur d'enumeration, telle que le frontend l'affiche. */
    public record Option(String value, String label) {}
}
