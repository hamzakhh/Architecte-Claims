package com.example.backend.dto;

import com.example.backend.model.Expertise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse pour une expertise.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpertiseResponse {

    private String id;
    private String claimId;
    private String expertId;
    private String gestionnaireId;
    private String conclusion;
    private String montantEstime;
    private String recommandation;
    private List<String> piecesJointes;
    private Expertise.StatutExpertise statut;
    private LocalDateTime dateRapport;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Champs enrichis (noms au lieu d'IDs)
    private String expertNom;
    private String gestionnaireNom;
    private String claimReference;
}
