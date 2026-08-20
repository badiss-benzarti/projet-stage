package com.gestionstages.user.exception;

/** Exceptions metier du service. */
public final class ApiExceptions {

    private ApiExceptions() {}

    /** 404 : la ressource demandee n'existe pas. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String type, Object id) {
            super(type + " introuvable : " + id);
        }
    }

    /** 409 : un profil existe deja pour ce compte. */
    public static class ProfileAlreadyExistsException extends RuntimeException {
        public ProfileAlreadyExistsException(String type) {
            super("Un profil " + type + " existe deja pour ce compte");
        }
    }

    /** 403 : la ressource appartient a quelqu'un d'autre. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
