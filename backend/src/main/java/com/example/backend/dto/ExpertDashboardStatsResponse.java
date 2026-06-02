package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les statistiques du tableau de bord expert.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpertDashboardStatsResponse {

    private long missionsEnCours;
    private long rapportsARendre;
    private long completesCeMois;
    private long totalMissions;
}
