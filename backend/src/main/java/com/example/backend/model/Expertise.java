package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité Expertise stockée dans MongoDB (collection "expertises").
 * Représente un rapport d'expertise réalisé par un expert sur un sinistre.
 */
@Data
@Document(collection = "expertises")
public class Expertise {

    @Id
    private String id;

    @Indexed
    private String claimId; // Identifiant du sinistre concerné

    @Indexed
    private String expertId; // Identifiant de l'expert ayant réalisé l'expertise

    private String gestionnaireId; // Identifiant du gestionnaire ayant demandé l'expertise

    private String conclusion; // Conclusion de l'expertise
    private String montantEstime; // Montant estimé des dégâts par l'expert
    private String recommandation; // Recommandation de l'expert (accepter, refuser, complément)
    private List<String> piecesJointes; // Fichiers joints au rapport
    private String commentaires; // Commentaires et justifications

    private StatutExpertise statut; // Statut de l'expertise

    private LocalDateTime dateRapport; // Date du rapport d'expertise
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Énumération des statuts possibles d'une expertise.
     */
    public enum StatutExpertise {
        EN_ATTENTE,    // Expertise demandée, en attente du rapport
        EN_COURS,      // Expertise en cours de réalisation
    SOUMISE,       // Rapport soumis par l'expert
        VALIDEE,       // Expertise validée par le gestionnaire
        REFUSEE        // Expertise refusée par le gestionnaire
    }

    /**
     * Énumération des recommandations possibles.
     */
    public enum Recommandation {
        ACCEPTER,      // Accepter le sinistre
        REFUSER,       // Refuser le sinistre
    COMPLEMENT      // Demander des compléments d'information
    }
}
