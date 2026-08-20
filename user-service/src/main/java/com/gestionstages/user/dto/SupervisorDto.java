package com.gestionstages.user.dto;

import com.gestionstages.user.entity.Supervisor;
import jakarta.validation.constraints.*;

/** Requetes et reponses du profil encadrant. */
public final class SupervisorDto {

    private SupervisorDto() {}

    public record Request(
            @NotNull(message = "L'identifiant du compte encadrant est obligatoire") Long userId,
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = "^$|^[0-9+ ]{8,20}$", message = "Numero de telephone invalide") String phone,
            @Size(max = 100) String position
    ) {}

    public record Response(
            Long id, Long userId, String firstName, String lastName,
            String email, String phone, String position,
            Long companyId, String companyName
    ) {
        public static Response from(Supervisor s) {
            return new Response(s.getId(), s.getUserId(), s.getFirstName(), s.getLastName(),
                    s.getEmail(), s.getPhone(), s.getPosition(),
                    s.getCompany().getId(), s.getCompany().getName());
        }
    }
}
