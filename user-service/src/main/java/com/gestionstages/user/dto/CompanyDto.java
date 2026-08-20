package com.gestionstages.user.dto;

import com.gestionstages.user.entity.Company;
import jakarta.validation.constraints.*;

/** Requetes et reponses du profil entreprise. */
public final class CompanyDto {

    private CompanyDto() {}

    public record Request(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Pattern(regexp = "^[0-9+ ]{8,20}$", message = "Numero de telephone invalide") String phone,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 40) String taxId
    ) {}

    public record Response(
            Long id, Long userId, String name, String address,
            String phone, String email, String taxId, int supervisorCount
    ) {
        public static Response from(Company c) {
            return new Response(c.getId(), c.getUserId(), c.getName(), c.getAddress(),
                    c.getPhone(), c.getEmail(), c.getTaxId(),
                    c.getSupervisors() == null ? 0 : c.getSupervisors().size());
        }
    }
}
