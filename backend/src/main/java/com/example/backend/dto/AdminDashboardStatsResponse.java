package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les statistiques du tableau de bord administrateur.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardStatsResponse {

    private long totalUtilisateurs;
    private long utilisateursActifs;
    private long totalAssures;
    private long totalExperts;
    private long totalGestionnaires;
    private long totalAdmins;
    private long totalSinistres;
    private long sinistresEnCours;
    private long sinistresClotures;
    private long sinistresCeMois;
    private long expertisesEnCours;
    private long expertsDisponibles;
}
