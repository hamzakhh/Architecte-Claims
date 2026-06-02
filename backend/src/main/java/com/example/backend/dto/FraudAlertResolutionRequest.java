package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la résolution d'une alerte de fraude par un administrateur.
 */
@Data
public class FraudAlertResolutionRequest {

    @NotBlank(message = "La décision est obligatoire")
    private String decision; // confirme_fraude, infonde, enquete_supplementaire

    private String notesResolution; // Notes de résolution
}
