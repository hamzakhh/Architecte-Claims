package com.example.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse pour une alerte de fraude.
 */
@Data
@Builder
public class FraudAlertResponse {

    private String id;
    private String claimId;
    private String claimReference;
    private String assureNom;
    private String signalePar;
    private String signaleParNom;
    private String motif;
    private String description;
    private String niveauRisque;
    private String statut;
    private List<String> piecesJustificatives;

    // Résolution
    private String resoluPar;
    private String resoluParNom;
    private String decision;
    private String notesResolution;
    private LocalDateTime dateResolution;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
