package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entité HistoriqueSinistre stockée dans MongoDB (collection "claim_history").
 * Représente une action ou un changement de statut sur un sinistre.
 */
@Data
@Document(collection = "claim_history")
public class ClaimHistory {

    @Id
    private String id;

    @Indexed
    private String claimId; // Identifiant du sinistre concerné

    private String action; // Type d'action : CREATION, CHANGEMENT_STATUT, ASSIGNATION_EXPERT, ASSIGNATION_GESTIONNAIRE, EXPERTISE_SOUMISE, REMBOURSEMENT, etc.

    private String description; // Description de l'action

    private String utilisateurId; // Identifiant de l'utilisateur ayant effectué l'action

    private String utilisateurNom; // Nom de l'utilisateur

    private String utilisateurRole; // Rôle de l'utilisateur

    private String ancienStatut; // Ancien statut (pour les changements de statut)

    private String nouveauStatut; // Nouveau statut (pour les changements de statut)

    private LocalDateTime createdAt;
}
