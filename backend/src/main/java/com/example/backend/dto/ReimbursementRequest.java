package com.example.backend.dto;

import com.example.backend.model.Reimbursement;
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
public class ReimbursementRequest {
    @NotBlank(message = "L'identifiant du sinistre est obligatoire")
    private String claimId;

    // Calcul automatisé
    @PositiveOrZero(message = "Le montant des dégâts doit être positif ou nul")
    private double montantDegats;
    @PositiveOrZero(message = "Le capital assuré doit être positif ou nul")
    private double capitalAssure;
    @PositiveOrZero(message = "La franchise doit être positive ou nulle")
    private double franchise;
    @PositiveOrZero(message = "Le plafond de garantie doit être positif ou nul")
    private double plafondGarantie;
    @PositiveOrZero(message = "Le taux de remboursement doit être positif ou nul")
    private double tauxRemboursement;
    private String typeSinistre;

    // Montant proposé (peut être ajusté par le gestionnaire)
    @PositiveOrZero(message = "Le montant proposé doit être positif ou nul")
    private double montantPropose;

    private Reimbursement.MethodePaiement methodePaiement;
    private String justification;
    private String notes;
}
