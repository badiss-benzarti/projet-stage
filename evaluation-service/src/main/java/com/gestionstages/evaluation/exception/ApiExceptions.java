package com.gestionstages.evaluation.exception;

/** Exceptions metier du module Stage. */
public final class ApiExceptions {

    private ApiExceptions() {}

    /** 404 */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String type, Object id) {
            super(type + " introuvable : " + id);
        }
    }

    /** 403 : la ressource ne concerne pas l'appelant. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    /** 409 : transition impossible depuis l'etat courant. */
    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) { super(message); }
    }

    /** 400 : la regle metier interdit l'operation. */
    public static class BusinessRuleException extends RuntimeException {
        public BusinessRuleException(String message) { super(message); }
    }
}
