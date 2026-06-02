package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimHistoryResponse {
    private String id;
    private String claimId;
    private String action;
    private String description;
    private String utilisateurId;
    private String utilisateurNom;
    private String utilisateurRole;
    private String ancienStatut;
    private String nouveauStatut;
    private LocalDateTime createdAt;
}
