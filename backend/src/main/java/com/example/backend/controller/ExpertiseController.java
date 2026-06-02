package com.example.backend.controller;

import com.example.backend.dto.ExpertiseRequest;
import com.example.backend.dto.ExpertiseResponse;
import com.example.backend.service.ExpertiseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST gérant les endpoints d'expertise.
 * Toutes les routes sont préfixées par /api/expertises.
 */
@RestController
@RequestMapping("/api/expertises")
@RequiredArgsConstructor
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    /**
     * Crée une demande d'expertise (gestionnaire).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ExpertiseResponse> createExpertise(@Valid @RequestBody ExpertiseRequest request) {
        ExpertiseResponse response = expertiseService.createExpertise(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère une expertise par son identifiant.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ExpertiseResponse> getExpertiseById(@PathVariable String id) {
        return ResponseEntity.ok(expertiseService.getExpertiseById(id));
    }

    /**
     * Récupère les expertises d'un sinistre.
     */
    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<List<ExpertiseResponse>> getExpertisesByClaim(@PathVariable String claimId) {
        return ResponseEntity.ok(expertiseService.getExpertisesByClaim(claimId));
    }

    /**
     * Récupère les expertises de l'expert connecté.
     */
    @GetMapping("/mes-expertises")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<List<ExpertiseResponse>> getMyExpertises() {
        return ResponseEntity.ok(expertiseService.getMyExpertises());
    }

    /**
     * Récupère les expertises du gestionnaire connecté.
     */
    @GetMapping("/gestionnaire")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<List<ExpertiseResponse>> getExpertisesByGestionnaire() {
        return ResponseEntity.ok(expertiseService.getExpertisesByGestionnaire());
    }

    /**
     * Récupère toutes les expertises (admin).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExpertiseResponse>> getAllExpertises() {
        return ResponseEntity.ok(expertiseService.getAllExpertises());
    }

    /**
     * Soumet un rapport d'expertise (expert).
     */
    @PutMapping("/{id}/rapport")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertiseResponse> submitRapport(
            @PathVariable String id,
            @Valid @RequestBody ExpertiseRequest request) {
        return ResponseEntity.ok(expertiseService.submitRapport(id, request));
    }

    /**
     * Enregistre un brouillon de rapport d'expertise (expert).
     */
    @PutMapping("/{id}/brouillon")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ExpertiseResponse> saveBrouillon(
            @PathVariable String id,
            @Valid @RequestBody ExpertiseRequest request) {
        return ResponseEntity.ok(expertiseService.saveBrouillon(id, request));
    }

    /**
     * Valide une expertise (gestionnaire).
     */
    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ExpertiseResponse> validerExpertise(@PathVariable String id) {
        return ResponseEntity.ok(expertiseService.validerExpertise(id));
    }

    /**
     * Refuse une expertise avec justification optionnelle (gestionnaire).
     */
    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ExpertiseResponse> refuserExpertise(
            @PathVariable String id,
            @RequestBody(required = false) String justification) {
        return ResponseEntity.ok(expertiseService.refuserExpertise(id, justification != null ? justification : "Refus sans justification"));
    }
}
