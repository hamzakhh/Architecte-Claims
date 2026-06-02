package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requête pour déclencher une analyse IA sur un sinistre.
 */
@Data
public class AnalyseIARequest {
    @NotBlank(message = "L'identifiant du sinistre est requis")
    private String claimId;

    private String typeAnalyse; // INITIALE, APPROFONDIE, REANALYSE, VERIFICATION_FRAUDE (optionnel, INITIALE par défaut)
}
