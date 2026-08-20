package com.gestionstages.auth.dto;

import com.gestionstages.auth.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 120)
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caracteres")
    String password,

    @NotBlank(message = "Le prenom est obligatoire")
    @Size(max = 60)
    String firstName,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 60)
    String lastName,

    @NotNull(message = "Le role est obligatoire")
    Role role

) {}
