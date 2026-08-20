package com.gestionstages.auth.dto;

/**
 * Reponse d'un login reussi.
 *
 * Le front stocke le token et le renvoie dans l'en-tete
 * Authorization: Bearer <token> a chaque appel via le gateway.
 */
public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
