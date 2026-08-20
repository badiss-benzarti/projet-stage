package com.gestionstages.user.dto;

import com.gestionstages.user.entity.Student;
import jakarta.validation.constraints.*;

/** Requetes et reponses du profil etudiant. */
public final class StudentDto {

    private StudentDto() {}

    public record Request(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = "^$|^[0-9+ ]{8,20}$", message = "Numero de telephone invalide") String phone,
            @Pattern(regexp = "^$|^[0-9]{8}$", message = "Le CIN doit contenir 8 chiffres") String cin,
            @NotBlank @Size(max = 20) String classe,
            @NotBlank @Size(max = 80) String departement
    ) {}

    public record Response(
            Long id, Long userId, String firstName, String lastName,
            String email, String phone, String cin, String classe, String departement
    ) {
        public static Response from(Student s) {
            return new Response(s.getId(), s.getUserId(), s.getFirstName(), s.getLastName(),
                    s.getEmail(), s.getPhone(), s.getCin(), s.getClasse(), s.getDepartement());
        }
    }
}
