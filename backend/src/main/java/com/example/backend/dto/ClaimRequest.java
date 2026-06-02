package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de demande de déclaration de sinistre.
 * Utilisé pour la création d'un nouveau sinistre par un assuré.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimRequest {

    @NotBlank(message = "Le type de sinistre est obligatoire")
    private String type; // Type de sinistre : water, fire, theft, auto, natural

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
    private String description; // Description détaillée de l'incident

    @NotBlank(message = "La date du sinistre est obligatoire")
    private String dateSinistre; // Date du sinistre (format yyyy-MM-dd)

    private String heureSinistre; // Heure du sinistre (format HH:mm)

    private String lieu; // Lieu du sinistre

    private String notesLieu; // Notes supplémentaires sur le lieu

    private String estimation; // Estimation du montant des dégâts

    private Double latitude; // Latitude de géolocalisation
    private Double longitude; // Longitude de géolocalisation

    private List<String> piecesJointes; // Noms des fichiers pièce jointes
}
