package com.example.backend.dto;

import com.example.backend.model.Claim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse pour un sinistre.
 * Retourné après la création ou la consultation d'un sinistre.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimResponse {

    private String id;
    private String reference;
    private String assureId;
    private String assureNom;
    private String categorie;
    private String type;
    private Double latitude;
    private Double longitude;
    private String description;
    private String dateSinistre;
    private String heureSinistre;
    private String lieu;
    private String notesLieu;
    private List<String> piecesJointes;
    private String estimation;
    private String notesInternes;

    // Qualification (2.2.1)
    private Claim.GraviteSinistre gravite;
    private String couvertureContractuelle;
    private Double franchise;
    private Double plafondCouverture;

    // Indemnisation (2.2.4)
    private Double montantIndemnisationPropose;
    private Double montantIndemnisationFinal;
    private String motifIndemnisation;
    private Boolean indemnisationAcceptee;
    private String motifRefusIndemnisation;
    private Boolean recoursEnCours;
    private LocalDateTime datePaiement;

    private Claim.StatutSinistre statut;
    private String gestionnaireId;
    private String gestionnaireNom;
    private String expertId;
    private String expertNom;
    private String analyseIAId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
