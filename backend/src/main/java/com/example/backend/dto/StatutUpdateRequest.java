package com.example.backend.dto;

import com.example.backend.model.Claim;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour du statut d'un sinistre.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatutUpdateRequest {
    private Claim.StatutSinistre statut;
}
