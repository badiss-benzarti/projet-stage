package com.gestionstages.evaluation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> onValidation(MethodArgumentNotValidException ex) {
        Map<String, String> champs = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> champs.put(e.getField(), e.getDefaultMessage()));

        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "Donnees invalides");
        body.put("champs", champs);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ApiExceptions.NotFoundException.class)
    public ResponseEntity<Map<String, Object>> onNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(base(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> onForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(base(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    /**
     * 409 et non 400 : la requete est bien formee, c'est l'etat courant de
     * la ressource qui rend l'operation impossible.
     */
    @ExceptionHandler(ApiExceptions.InvalidTransitionException.class)
    public ResponseEntity<Map<String, Object>> onInvalidTransition(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> onBusinessRule(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(base(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    private Map<String, Object> base(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
