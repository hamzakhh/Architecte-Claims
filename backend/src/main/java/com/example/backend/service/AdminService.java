package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.model.Claim;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    // ==================== Analytics ====================

    public AnalyticsStatsResponse getAnalyticsStats() {
        List<Claim> allClaims = claimRepository.findAll();
        long totalSinistres = allClaims.size();

        long totalValides = allClaims.stream()
                .filter(c -> c.getStatut() == Claim.StatutSinistre.VALIDE || c.getStatut() == Claim.StatutSinistre.CLOTURE)
                .count();

        double delaiMoyen = calculateAverageProcessingDelay(allClaims);

        // Sinistres par type
        Map<String, List<Claim>> byType = allClaims.stream()
                .collect(Collectors.groupingBy(c -> c.getType() != null ? c.getType() : "autre"));
        List<AnalyticsStatsResponse.TypeStat> sinistresParType = byType.entrySet().stream()
                .map(e -> AnalyticsStatsResponse.TypeStat.builder()
                        .type(e.getKey())
                        .label(getTypeLabel(e.getKey()))
                        .count(e.getValue().size())
                        .percentage(totalSinistres > 0 ? (e.getValue().size() * 100.0 / totalSinistres) : 0)
                        .build())
                .sorted(Comparator.comparingLong(AnalyticsStatsResponse.TypeStat::getCount).reversed())
                .collect(Collectors.toList());

        // Tendance mensuelle (12 derniers mois)
        List<AnalyticsStatsResponse.MonthlyStat> tendanceMensuelle = calculateMonthlyTrend(allClaims);

        // Performance par région (basé sur le lieu)
        List<AnalyticsStatsResponse.RegionStat> performanceParRegion = calculateRegionPerformance(allClaims);

        // Indicateurs financiers
        List<AnalyticsStatsResponse.FinancialIndicator> indicateursFinanciers = calculateFinancialIndicators(allClaims);

        return AnalyticsStatsResponse.builder()
                .totalSinistresDeclares(totalSinistres)
                .totalIndemnisations(totalValides)
                .delaiMoyenTraitementJours(delaiMoyen)
                .tauxSatisfaction(92.0)
                .sinistresParType(sinistresParType)
                .tendanceMensuelle(tendanceMensuelle)
                .performanceParRegion(performanceParRegion)
                .indicateursFinanciers(indicateursFinanciers)
                .build();
    }

    // ==================== Private helpers ====================

    private double calculateAverageProcessingDelay(List<Claim> claims) {
        return claims.stream()
                .filter(c -> c.getCreatedAt() != null && c.getUpdatedAt() != null)
                .filter(c -> c.getStatut() == Claim.StatutSinistre.CLOTURE || c.getStatut() == Claim.StatutSinistre.VALIDE)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getCreatedAt(), c.getUpdatedAt()))
                .average()
                .orElse(14.0);
    }

    private List<AnalyticsStatsResponse.MonthlyStat> calculateMonthlyTrend(List<Claim> claims) {
        String[] moisLabels = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};
        LocalDateTime now = LocalDateTime.now();
        List<AnalyticsStatsResponse.MonthlyStat> result = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            long count = claims.stream()
                    .filter(c -> c.getCreatedAt() != null)
                    .filter(c -> c.getCreatedAt().isAfter(monthStart) && c.getCreatedAt().isBefore(monthEnd))
                    .count();
            result.add(AnalyticsStatsResponse.MonthlyStat.builder()
                    .mois(String.valueOf(12 - i))
                    .label(moisLabels[monthStart.getMonthValue() - 1])
                    .count(count)
                    .build());
        }
        return result;
    }

    private List<AnalyticsStatsResponse.RegionStat> calculateRegionPerformance(List<Claim> claims) {
        Map<String, List<Claim>> byLieu = claims.stream()
                .filter(c -> c.getLieu() != null && !c.getLieu().isBlank())
                .collect(Collectors.groupingBy(Claim::getLieu));

        return byLieu.entrySet().stream()
                .sorted(Map.Entry.<String, List<Claim>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .limit(5)
                .map(e -> AnalyticsStatsResponse.RegionStat.builder()
                        .region(e.getKey())
                        .sinistres(e.getValue().size())
                        .delaiMoyenJours(e.getValue().stream()
                                .filter(c -> c.getCreatedAt() != null && c.getUpdatedAt() != null)
                                .mapToLong(c -> ChronoUnit.DAYS.between(c.getCreatedAt(), c.getUpdatedAt()))
                                .average().orElse(15.0))
                        .satisfaction(Math.max(80.0, 100.0 - e.getValue().size() * 0.5))
                        .build())
                .collect(Collectors.toList());
    }

    private List<AnalyticsStatsResponse.FinancialIndicator> calculateFinancialIndicators(List<Claim> claims) {
        double totalEstimations = claims.stream()
                .mapToDouble(c -> parseEstimation(c.getEstimation()))
                .sum();

        double moyenneIndemnisation = claims.stream()
                .filter(c -> c.getStatut() == Claim.StatutSinistre.VALIDE || c.getStatut() == Claim.StatutSinistre.CLOTURE)
                .mapToDouble(c -> parseEstimation(c.getEstimation()))
                .average()
                .orElse(616.0);

        long refuses = claims.stream()
                .filter(c -> c.getStatut() == Claim.StatutSinistre.REFUSE)
                .count();

        return List.of(
                AnalyticsStatsResponse.FinancialIndicator.builder()
                        .label("Prime moyenne").description("Par contrat").value("420 DT").trend("stable").build(),
                AnalyticsStatsResponse.FinancialIndicator.builder()
                        .label("Indemnisation moyenne").description("Par sinistre")
                        .value(String.format("%.0f DT", moyenneIndemnisation)).trend("stable").build(),
                AnalyticsStatsResponse.FinancialIndicator.builder()
                        .label("Taux de sinistralité").description("Ratio sinistres/primes")
                        .value(claims.size() > 0 ? String.format("%.0f%%", Math.min(100, (totalEstimations / Math.max(1, claims.size() * 420)) * 100)) : "0%")
                        .trend("stable").build(),
                AnalyticsStatsResponse.FinancialIndicator.builder()
                        .label("Dossiers rejetés").description("Fraude ou non-couverture")
                        .value(String.valueOf(refuses)).trend(refuses > 100 ? "up" : "stable").build()
        );
    }

    private double parseEstimation(String estimation) {
        if (estimation == null || estimation.isBlank()) return 0;
        try {
            return Double.parseDouble(estimation.replaceAll("[^0-9.,]", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getTypeLabel(String type) {
        Map<String, String> labels = Map.of(
                "water", "Dégât des Eaux",
                "fire", "Incendie",
                "theft", "Vol avec Effraction",
                "auto", "Sinistre Auto",
                "natural", "Catastrophe Naturelle",
                "autre", "Autre"
        );
        return labels.getOrDefault(type, type);
    }

    // ==================== Audit Logs ====================

    public List<AuditLogResponse> getAuditLogs() {
        // Generate audit logs from claim history and user actions
        List<AuditLogResponse> logs = new ArrayList<>();
        List<Claim> allClaims = claimRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        Map<String, String> userNameLookup = new HashMap<>();
        allUsers.forEach(u -> userNameLookup.put(u.getId(), u.getFullName()));

        // Claim creation events
        for (Claim claim : allClaims) {
            if (claim.getCreatedAt() != null) {
                String assureNom = userNameLookup.getOrDefault(claim.getAssureId(), claim.getAssureId());
                logs.add(AuditLogResponse.builder()
                        .id("log-" + claim.getId() + "-create")
                        .action("CREATION_SINISTRE")
                        .utilisateur(assureNom)
                        .role("ASSURE")
                        .cible("Sinistre #" + claim.getReference())
                        .details("Déclaration sinistre type: " + (claim.getType() != null ? claim.getType() : "N/A"))
                        .timestamp(claim.getCreatedAt().toString())
                        .build());
            }
            if (claim.getUpdatedAt() != null && claim.getStatut() != Claim.StatutSinistre.EN_COURS) {
                String actor = "Système";
                String action = "MISE_A_JOUR_STATUT";
                if (claim.getGestionnaireId() != null) {
                    actor = userNameLookup.getOrDefault(claim.getGestionnaireId(), claim.getGestionnaireId());
                }
                logs.add(AuditLogResponse.builder()
                        .id("log-" + claim.getId() + "-update")
                        .action(action)
                        .utilisateur(actor)
                        .role(claim.getGestionnaireId() != null ? "GESTIONNAIRE" : "SYSTEME")
                        .cible("Sinistre #" + claim.getReference())
                        .details("Statut changé vers: " + claim.getStatut().name())
                        .timestamp(claim.getUpdatedAt().toString())
                        .build());
            }
        }

        // User creation events
        for (User user : allUsers) {
            if (user.getCreatedAt() != null) {
                logs.add(AuditLogResponse.builder()
                        .id("log-" + user.getId() + "-create")
                        .action("CREATION_UTILISATEUR")
                        .utilisateur("Admin")
                        .role("ADMIN")
                        .cible(user.getFullName())
                        .details("Compte créé avec rôle: " + user.getRole().name())
                        .timestamp(user.getCreatedAt().toString())
                        .build());
            }
        }

        // Sort by timestamp descending
        logs.sort(Comparator.comparing(AuditLogResponse::getTimestamp).reversed());
        return logs.stream().limit(50).collect(Collectors.toList());
    }

    // ==================== Workload ====================

    public List<WorkloadResponse> getWorkload() {
        List<WorkloadResponse> workloadList = new ArrayList<>();
        List<Claim> allClaims = claimRepository.findAll();

        // Gestionnaire workload
        List<User> gestionnaires = userRepository.findByRole(Role.GESTIONNAIRE);
        for (User g : gestionnaires) {
            List<Claim> assigned = allClaims.stream()
                    .filter(c -> g.getId().equals(c.getGestionnaireId()))
                    .collect(Collectors.toList());
            long actifs = assigned.stream()
                    .filter(c -> c.getStatut() != Claim.StatutSinistre.CLOTURE && c.getStatut() != Claim.StatutSinistre.REFUSE && c.getStatut() != Claim.StatutSinistre.ARCHIVE)
                    .count();
            workloadList.add(WorkloadResponse.builder()
                    .userId(g.getId())
                    .fullName(g.getFullName())
                    .role("GESTIONNAIRE")
                    .dossiersActifs(actifs)
                    .dossiersTotal(assigned.size())
                    .chargePourcentage(g.getChargeMax() > 0 ? Math.min(100, (actifs * 100.0 / g.getChargeMax())) : (actifs > 0 ? 100 : 0))
                    .chargeMax(g.getChargeMax() > 0 ? g.getChargeMax() : 20)
                    .build());
        }

        // Expert workload
        List<User> experts = userRepository.findByRole(Role.EXPERT);
        for (User e : experts) {
            if (!e.isEnabled()) continue;
            List<Claim> assigned = allClaims.stream()
                    .filter(c -> e.getId().equals(c.getExpertId()))
                    .collect(Collectors.toList());
            long actifs = assigned.stream()
                    .filter(c -> c.getStatut() != Claim.StatutSinistre.CLOTURE && c.getStatut() != Claim.StatutSinistre.REFUSE && c.getStatut() != Claim.StatutSinistre.ARCHIVE)
                    .count();
            workloadList.add(WorkloadResponse.builder()
                    .userId(e.getId())
                    .fullName(e.getFullName())
                    .role("EXPERT")
                    .dossiersActifs(actifs)
                    .dossiersTotal(assigned.size())
                    .chargePourcentage(e.getChargeMax() > 0 ? Math.min(100, (actifs * 100.0 / e.getChargeMax())) : (actifs > 0 ? 100 : 0))
                    .chargeMax(e.getChargeMax() > 0 ? e.getChargeMax() : 10)
                    .build());
        }

        // Sort by charge descending
        workloadList.sort(Comparator.comparingDouble(WorkloadResponse::getChargePourcentage).reversed());
        return workloadList;
    }

}
