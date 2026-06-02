package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les statistiques du tableau de bord gestionnaire.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {

    private long dossiersEnCours;
    private long enAttenteApprobation;
    private long expertisesEnCours;
    private long expertsDisponibles;
    private long totalExperts;
    private long sinistresCeMois;

    // Statistiques personnelles (2.2.5)
    private long dossiersOuverts;
    private long dossiersEnAttente;
    private long dossiersUrgence;
    private long dossiersSansGestionnaire;
}
