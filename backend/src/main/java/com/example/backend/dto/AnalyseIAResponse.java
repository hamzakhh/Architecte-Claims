package com.example.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse de l'analyse IA d'un sinistre.
 */
@Data
@Builder
public class AnalyseIAResponse {
    private String id;
    private String claimId;
    private String claimReference;

    // Scores
    private int scoreComplexite;
    private int scoreRisque;
    private int scoreConfiance;

    // Estimation
    private Double montantEstime;
    private String devise;

    // Classification
    private String severite;
    private String categorieDetectee;
    private List<String> motsCles;

    // Recommandation
    private boolean necessiteExpertHumain;
    private String recommandation;
    private String justification;

    // Analyse détaillée
    private String resumeAnalyse;
    private String pointsAttention;
    private String elementsFraude;
    private String recommandationsAction;

    // Métadonnées
    private String typeAnalyse;
    private String statut;
    private String modeleIA;
    private int tokensUtilises;
    private double coutEstime;

    private LocalDateTime dateAnalyse;
    private LocalDateTime createdAt;
}
