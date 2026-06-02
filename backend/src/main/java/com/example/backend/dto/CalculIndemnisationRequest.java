package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalculIndemnisationRequest {

    @NotBlank(message = "L'identifiant du sinistre est obligatoire")
    private String claimId;

    @PositiveOrZero(message = "Le montant des dégâts doit être positif ou nul")
    private double montantDegats;

    @PositiveOrZero(message = "Le capital assuré doit être positif ou nul")
    private double capitalAssure;

    @PositiveOrZero(message = "La franchise doit être positive ou nulle")
    private double franchise;

    @PositiveOrZero(message = "Le plafond de garantie doit être positif ou nul")
    private double plafondGarantie;

    @PositiveOrZero(message = "Le taux de remboursement doit être positif ou nul")
    private double tauxRemboursement; // ex: 0.90 pour 90%

    private String typeSinistre; // accident, vol, incendie, etc.
}
