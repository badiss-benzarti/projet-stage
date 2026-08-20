package com.gestionstages.auth.exception;

/** Exceptions metier du service, regroupees pour eviter dix fichiers d'une ligne. */
public final class ApiExceptions {

    private ApiExceptions() {}

    /** 409 : l'email est deja pris. */
    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("Un compte existe deja avec l'email " + email);
        }
    }

    /** 404 : utilisateur inconnu. */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String identifier) {
            super("Utilisateur introuvable : " + identifier);
        }
    }
}
