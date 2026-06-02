package com.example.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pour les statistiques des alertes de fraude (dashboard admin).
 */
@Data
@Builder
public class FraudAlertStatsResponse {

    private long totalAlertes;
    private long alertesEnAttente;
    private long alertesEnCoursAnalyse;
    private long alertesConfirmees;
    private long alertesInfondees;
    private long alertesCritiques;
    private long alertesCeMois;
}
