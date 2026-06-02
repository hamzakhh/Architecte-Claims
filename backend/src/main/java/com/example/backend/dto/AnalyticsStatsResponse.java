package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsStatsResponse {

    private long totalSinistresDeclares;
    private long totalIndemnisations;
    private double delaiMoyenTraitementJours;
    private double tauxSatisfaction;

    private List<TypeStat> sinistresParType;
    private List<MonthlyStat> tendanceMensuelle;
    private List<RegionStat> performanceParRegion;
    private List<FinancialIndicator> indicateursFinanciers;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeStat {
        private String type;
        private String label;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyStat {
        private String mois;
        private String label;
        private long count;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionStat {
        private String region;
        private long sinistres;
        private double delaiMoyenJours;
        private double satisfaction;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FinancialIndicator {
        private String label;
        private String description;
        private String value;
        private String trend;
    }
}
