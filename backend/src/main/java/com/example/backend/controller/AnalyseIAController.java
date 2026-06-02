package com.example.backend.controller;

import com.example.backend.dto.AnalyseIARequest;
import com.example.backend.dto.AnalyseIAResponse;
import com.example.backend.service.AnalyseIAService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST gérant les endpoints d'analyse IA des sinistres.
 * Toutes les routes sont préfixées par /api/analyses-ia.
 */
@RestController
@RequestMapping("/api/analyses-ia")
@RequiredArgsConstructor
public class AnalyseIAController {

    private final AnalyseIAService analyseIAService;

    /**
     * Déclenche une analyse IA sur un sinistre.
     * Accessible par les gestionnaires (révision) et automatiquement à la déclaration.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<AnalyseIAResponse> analyserSinistre(@Valid @RequestBody AnalyseIARequest request) {
        AnalyseIAResponse response = analyseIAService.analyserSinistre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère l'analyse IA d'un sinistre par son identifiant de sinistre.
     */
    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<AnalyseIAResponse> getAnalyseByClaimId(@PathVariable String claimId) {
        return ResponseEntity.ok(analyseIAService.getAnalyseByClaimId(claimId));
    }

    /**
     * Récupère les analyses nécessitant un expert humain.
     */
    @GetMapping("/expert-requis")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<AnalyseIAResponse>> getAnalysesNecessitantExpert() {
        return ResponseEntity.ok(analyseIAService.getAnalysesNecessitantExpert());
    }

    /**
     * Récupère toutes les analyses (admin).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnalyseIAResponse>> getAllAnalyses() {
        return ResponseEntity.ok(analyseIAService.getAllAnalyses());
    }

    /**
     * Récupère les statistiques des analyses IA.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAnalyseStats() {
        return ResponseEntity.ok(analyseIAService.getAnalyseStats());
    }
}
