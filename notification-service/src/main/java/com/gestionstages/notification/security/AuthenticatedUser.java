package com.gestionstages.notification.security;

/**
 * Identite du porteur du jeton, reconstituee a partir des claims JWT.
 *
 * Devient le principal du SecurityContext : un controleur declare
 * AuthenticatedUser en parametre et connait l'id de l'utilisateur
 * sans avoir a appeler l'auth-service.
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
