package com.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 * Intercepte les exceptions levées par les contrôleurs et retourne
 * des réponses JSON structurées avec les codes HTTP appropriés.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les RuntimeException génériques (ex: email déjà utilisé, token invalide).
     * Retourne 400 Bad Request avec le message d'erreur.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Gère les erreurs d'identifiants invalides (mauvais email/mot de passe).
     * Retourne 401 Unauthorized avec un message générique pour éviter la divulgation d'informations.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Email ou mot de passe incorrect");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gère les autres erreurs d'authentification Spring Security.
     * Retourne 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Erreur d'authentification");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gère les erreurs de validation des DTO (@NotBlank, @Email, @Size, etc.).
     * Retourne 400 Bad Request avec la liste des champs en erreur et leurs messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Gère les erreurs de désérialisation JSON (corps de requête mal formé, type incompatible).
     * Retourne 400 Bad Request avec le message d'erreur.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMostSpecificCause().getMessage();
        error.put("error", "Requête invalide : " + (message != null ? message : ex.getMessage()));
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Gère les erreurs d'accès refusé (autorisation insuffisante, rôle non autorisé).
     * Retourne 403 Forbidden avec un message explicatif.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Accès refusé : vous n'avez pas les droits nécessaires");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
