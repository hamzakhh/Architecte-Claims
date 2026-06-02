package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de demande de connexion.
 * Contient les identifiants de l'utilisateur (email + mot de passe).
 */
@Data
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email; // Email utilisé comme identifiant

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password; // Mot de passe en clair (sera validé par Spring Security)
}
