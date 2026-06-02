package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de demande de création/mise à jour d'une expertise.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpertiseRequest {

    @NotBlank(message = "L'identifiant du sinistre est obligatoire")
    private String claimId;

    private String conclusion;

    private String montantEstime;

    private String recommandation; // ACCEPTER, REFUSER, COMPLEMENT

    private List<String> piecesJointes;
}
