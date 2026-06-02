package com.example.backend.dto;

import com.example.backend.model.Claim;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QualificationRequest {

    @NotNull
    private Claim.GraviteSinistre gravite;

    private String couvertureContractuelle;
    private Double franchise;
    private Double plafondCouverture;
}
