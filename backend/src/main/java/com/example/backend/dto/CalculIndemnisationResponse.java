package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalculIndemnisationResponse {
    private String claimId;
    private double montantDegats;
    private double franchise;
    private double montantApresFranchise;
    private double tauxRemboursement;
    private double montantIndemnisationCalcule;
    private double plafondGarantie;
    private boolean plafondAtteint;
    private double montantFinalCalcule;
    private String detailCalcul;
}
