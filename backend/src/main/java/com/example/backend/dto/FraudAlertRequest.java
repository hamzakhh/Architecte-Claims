package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO pour la création d'un signalement de fraude par un gestionnaire.
 */
@Data
public class FraudAlertRequest {

    @NotBlank(message = "L'identifiant du sinistre est obligatoire")
    private String claimId;

    @NotBlank(message = "Le motif du signalement est obligatoire")
    private String motif; // documents_falsifies, incoherence_reclamations, surevaluation_degats, sinistre_fictif, recurrence_suspecte, autre

    private String description; // Description détaillée des éléments suspects

    @NotNull(message = "Le niveau de risque est obligatoire")
    private String niveauRisque; // FAIBLE, MOYEN, ELEVE, CRITIQUE

    private List<String> piecesJustificatives; // Pièces justificatives
}
