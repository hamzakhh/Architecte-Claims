package com.example.backend.service;

import com.example.backend.dto.FraudAlertRequest;
import com.example.backend.dto.FraudAlertResolutionRequest;
import com.example.backend.dto.FraudAlertResponse;
import com.example.backend.dto.FraudAlertStatsResponse;
import com.example.backend.model.Claim;
import com.example.backend.model.FraudAlert;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.FraudAlertRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Crée un signalement de fraude par un gestionnaire.
     */
    public FraudAlertResponse createFraudAlert(FraudAlertRequest request, String gestionnaireId) {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        FraudAlert alert = new FraudAlert();
        alert.setClaimId(request.getClaimId());
        alert.setSignalePar(gestionnaireId);
        alert.setMotif(request.getMotif());
        alert.setDescription(request.getDescription());
        alert.setNiveauRisque(FraudAlert.NiveauRisque.valueOf(request.getNiveauRisque()));
        alert.setStatut(FraudAlert.StatutAlerte.SOUMISE);
        alert.setPiecesJustificatives(request.getPiecesJustificatives());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());

        FraudAlert saved = fraudAlertRepository.save(alert);

        // Notifier les admins
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.envoyerNotification(
                    admin.getId(),
                    "Nouveau signalement de fraude",
                    "Un gestionnaire a signalé une réclamation suspecte pour le sinistre " + claim.getReference(),
                    "FRAUDE",
                    claim.getId()
            );
        }

        log.info("Signalement de fraude créé pour le sinistre {} par le gestionnaire {}", claim.getReference(), gestionnaireId);

        return mapToResponse(saved);
    }

    /**
     * Récupère les signalements faits par un gestionnaire.
     */
    public List<FraudAlertResponse> getAlertsByGestionnaire(String gestionnaireId) {
        return fraudAlertRepository.findBySignalePar(gestionnaireId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les alertes de fraude (pour l'admin).
     */
    public List<FraudAlertResponse> getAllAlerts() {
        return fraudAlertRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les alertes en attente (pour l'admin).
     */
    public List<FraudAlertResponse> getPendingAlerts() {
        return fraudAlertRepository.findByStatutIn(Arrays.asList(
                FraudAlert.StatutAlerte.SOUMISE,
                FraudAlert.StatutAlerte.EN_COURS_ANALYSE,
                FraudAlert.StatutAlerte.ENQUETE_SUPPLEMENTAIRE
        )).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une alerte par son identifiant.
     */
    public FraudAlertResponse getAlertById(String id) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte de fraude non trouvée"));
        return mapToResponse(alert);
    }

    /**
     * Met à jour le statut d'une alerte (analyse en cours).
     */
    public FraudAlertResponse updateAlertStatus(String id, FraudAlert.StatutAlerte statut) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte de fraude non trouvée"));

        alert.setStatut(statut);
        alert.setUpdatedAt(LocalDateTime.now());
        FraudAlert saved = fraudAlertRepository.save(alert);

        // Notifier le gestionnaire qui a fait le signalement
        notificationService.envoyerNotification(
                alert.getSignalePar(),
                "Mise à jour signalement fraude",
                "Le statut de votre signalement de fraude a été mis à jour : " + statut.name(),
                "FRAUDE",
                alert.getClaimId()
        );

        return mapToResponse(saved);
    }

    /**
     * Résout une alerte de fraude par un administrateur.
     */
    public FraudAlertResponse resolveFraudAlert(String id, FraudAlertResolutionRequest request, String adminId) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte de fraude non trouvée"));

        alert.setResoluPar(adminId);
        alert.setDecision(request.getDecision());
        alert.setNotesResolution(request.getNotesResolution());
        alert.setDateResolution(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());

        // Mettre à jour le statut selon la décision
        switch (request.getDecision()) {
            case "confirme_fraude":
                alert.setStatut(FraudAlert.StatutAlerte.CONFIRMEE);
                break;
            case "infonde":
                alert.setStatut(FraudAlert.StatutAlerte.INFONDEE);
                break;
            case "enquete_supplementaire":
                alert.setStatut(FraudAlert.StatutAlerte.ENQUETE_SUPPLEMENTAIRE);
                break;
            default:
                alert.setStatut(FraudAlert.StatutAlerte.CLOTUREE);
        }

        FraudAlert saved = fraudAlertRepository.save(alert);

        // Notifier le gestionnaire qui a fait le signalement
        String message = switch (request.getDecision()) {
            case "confirme_fraude" -> "Votre signalement de fraude a été confirmé par l'administration.";
            case "infonde" -> "Votre signalement de fraude a été déclaré infondé par l'administration.";
            case "enquete_supplementaire" -> "Une enquête supplémentaire a été demandée pour votre signalement.";
            default -> "Votre signalement de fraude a été clôturé.";
        };
        notificationService.envoyerNotification(
                alert.getSignalePar(),
                "Résolution signalement fraude",
                message,
                "FRAUDE",
                alert.getClaimId()
        );

        log.info("Alerte de fraude {} résolue par l'admin {} : {}", id, adminId, request.getDecision());

        return mapToResponse(saved);
    }

    /**
     * Récupère les statistiques des alertes de fraude (dashboard admin).
     */
    public FraudAlertStatsResponse getFraudAlertStats() {
        long total = fraudAlertRepository.count();
        long enAttente = fraudAlertRepository.countByStatut(FraudAlert.StatutAlerte.SOUMISE);
        long enCoursAnalyse = fraudAlertRepository.countByStatut(FraudAlert.StatutAlerte.EN_COURS_ANALYSE);
        long confirmees = fraudAlertRepository.countByStatut(FraudAlert.StatutAlerte.CONFIRMEE);
        long infondees = fraudAlertRepository.countByStatut(FraudAlert.StatutAlerte.INFONDEE);
        long critiques = fraudAlertRepository.findByNiveauRisque(FraudAlert.NiveauRisque.CRITIQUE).stream()
                .filter(a -> a.getStatut() == FraudAlert.StatutAlerte.SOUMISE || a.getStatut() == FraudAlert.StatutAlerte.EN_COURS_ANALYSE)
                .count();

        // Alertes ce mois
        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long alertesCeMois = fraudAlertRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(debutMois))
                .count();

        return FraudAlertStatsResponse.builder()
                .totalAlertes(total)
                .alertesEnAttente(enAttente)
                .alertesEnCoursAnalyse(enCoursAnalyse)
                .alertesConfirmees(confirmees)
                .alertesInfondees(infondees)
                .alertesCritiques(critiques)
                .alertesCeMois(alertesCeMois)
                .build();
    }

    /**
     * Mappe une entité FraudAlert vers un DTO FraudAlertResponse.
     */
    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        // Récupérer les infos du sinistre
        String claimReference = null;
        String assureNom = null;
        Claim claim = claimRepository.findById(alert.getClaimId()).orElse(null);
        if (claim != null) {
            claimReference = claim.getReference();
            User assure = userRepository.findById(claim.getAssureId()).orElse(null);
            if (assure != null) {
                assureNom = assure.getFullName();
            }
        }

        // Récupérer le nom du gestionnaire
        String signaleParNom = null;
        User gestionnaire = userRepository.findById(alert.getSignalePar()).orElse(null);
        if (gestionnaire != null) {
            signaleParNom = gestionnaire.getFullName();
        }

        // Récupérer le nom de l'admin
        String resoluParNom = null;
        if (alert.getResoluPar() != null) {
            User admin = userRepository.findById(alert.getResoluPar()).orElse(null);
            if (admin != null) {
                resoluParNom = admin.getFullName();
            }
        }

        return FraudAlertResponse.builder()
                .id(alert.getId())
                .claimId(alert.getClaimId())
                .claimReference(claimReference)
                .assureNom(assureNom)
                .signalePar(alert.getSignalePar())
                .signaleParNom(signaleParNom)
                .motif(alert.getMotif())
                .description(alert.getDescription())
                .niveauRisque(alert.getNiveauRisque() != null ? alert.getNiveauRisque().name() : null)
                .statut(alert.getStatut() != null ? alert.getStatut().name() : null)
                .piecesJustificatives(alert.getPiecesJustificatives())
                .resoluPar(alert.getResoluPar())
                .resoluParNom(resoluParNom)
                .decision(alert.getDecision())
                .notesResolution(alert.getNotesResolution())
                .dateResolution(alert.getDateResolution())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
