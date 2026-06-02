package com.example.backend.controller;

import com.example.backend.dto.FraudAlertRequest;
import com.example.backend.dto.FraudAlertResolutionRequest;
import com.example.backend.dto.FraudAlertResponse;
import com.example.backend.dto.FraudAlertStatsResponse;
import com.example.backend.model.FraudAlert;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.FraudAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les endpoints de signalement de fraude.
 * Toutes les routes sont préfixées par /api/fraud-alerts.
 */
@RestController
@RequestMapping("/api/fraud-alerts")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;
    private final UserRepository userRepository;

    // ==================== Gestionnaire : Signalement ====================

    /**
     * Crée un signalement de réclamation suspecte (fraude potentielle).
     * Accessible par les gestionnaires authentifiés.
     */
    @PostMapping
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<FraudAlertResponse> createFraudAlert(@Valid @RequestBody FraudAlertRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        FraudAlertResponse response = fraudAlertService.createFraudAlert(request, gestionnaire.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère les signalements du gestionnaire connecté.
     */
    @GetMapping("/mes-signalements")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<List<FraudAlertResponse>> getMyFraudAlerts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(fraudAlertService.getAlertsByGestionnaire(gestionnaire.getId()));
    }

    /**
     * Récupère une alerte par son identifiant.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<FraudAlertResponse> getFraudAlertById(@PathVariable String id) {
        return ResponseEntity.ok(fraudAlertService.getAlertById(id));
    }

    // ==================== Admin : Surveillance ====================

    /**
     * Récupère toutes les alertes de fraude.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudAlertResponse>> getAllFraudAlerts() {
        return ResponseEntity.ok(fraudAlertService.getAllAlerts());
    }

    /**
     * Récupère les alertes en attente de traitement.
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudAlertResponse>> getPendingFraudAlerts() {
        return ResponseEntity.ok(fraudAlertService.getPendingAlerts());
    }

    /**
     * Met une alerte en cours d'analyse.
     */
    @PatchMapping("/{id}/analyser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> startAnalysis(@PathVariable String id) {
        return ResponseEntity.ok(fraudAlertService.updateAlertStatus(id, FraudAlert.StatutAlerte.EN_COURS_ANALYSE));
    }

    /**
     * Résout une alerte de fraude (décision de l'admin).
     */
    @PatchMapping("/{id}/resoudre")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> resolveFraudAlert(
            @PathVariable String id,
            @Valid @RequestBody FraudAlertResolutionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(fraudAlertService.resolveFraudAlert(id, request, admin.getId()));
    }

    /**
     * Récupère les statistiques des alertes de fraude.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertStatsResponse> getFraudAlertStats() {
        return ResponseEntity.ok(fraudAlertService.getFraudAlertStats());
    }
}
