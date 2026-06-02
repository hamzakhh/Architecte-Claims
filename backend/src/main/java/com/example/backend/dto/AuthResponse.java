package com.example.backend.dto;

import com.example.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse d'authentification.
 * Retourné après une inscription ou connexion réussie,
 * contient le token JWT et les informations de l'utilisateur.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;    // Token JWT pour les requêtes authentifiées
    private String email;    // Email de l'utilisateur
    private String prenom;   // Prénom de l'utilisateur
    private String nom;      // Nom de famille de l'utilisateur
    private String fullName; // Nom complet (prénom + nom)
    private Role role;       // Rôle de l'utilisateur
    private String userId;   // Identifiant MongoDB de l'utilisateur
}
