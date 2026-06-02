package com.example.backend.dto;

import com.example.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de demande d'inscription.
 * Contient les informations nécessaires pour créer un nouveau compte utilisateur.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom; // Prénom de l'utilisateur

    @NotBlank(message = "Le nom est obligatoire")
    private String nom; // Nom de famille de l'utilisateur

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email; // Adresse email (identifiant unique)

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password; // Mot de passe (min. 6 caractères)

    private String telephone; // Numéro de téléphone (optionnel)

    private Role role; // Rôle de l'utilisateur (défaut : ASSURE si null)

    // Champs spécifiques au profil expert
    private String specialite;
    private String zoneIntervention;
    private Double notePerformance;
    private Integer chargeMax;
}
