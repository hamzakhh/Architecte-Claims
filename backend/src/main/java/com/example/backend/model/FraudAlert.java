package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité Alerte de Fraude stockée dans MongoDB (collection "fraud_alerts").
 * Représente un signalement de réclamation suspecte (fraude potentielle).
 */
@Data
@Document(collection = "fraud_alerts")
public class FraudAlert {

    @Id
    private String id;

    @Indexed
    private String claimId; // Identifiant du sinistre suspect

    @Indexed
    private String signalePar; // Identifiant du gestionnaire ayant signalé

    private String motif; // Motif du signalement : documents_falsifies, incoherence_reclamations, surévaluation_degats, sinistre_fictif, recurrence_suspecte, autre

    private String description; // Description détaillée des éléments suspects

    private NiveauRisque niveauRisque; // Niveau de risque : FAIBLE, MOYEN, ELEVE, CRITIQUE

    private StatutAlerte statut; // Statut de l'alerte

    private List<String> piecesJustificatives; // Pièces justificatives du signalement

    // Résolution (par l'admin)
    private String resoluPar; // Identifiant de l'admin ayant traité
    private String decision; // Décision : confirme_fraude, infonde, enquete_supplementaire
    private String notesResolution; // Notes de résolution par l'admin
    private LocalDateTime dateResolution; // Date de résolution

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Énumération des statuts possibles d'une alerte de fraude.
     */
    public enum StatutAlerte {
        SOUMISE,            // Signalement soumis par le gestionnaire
        EN_COURS_ANALYSE,   // En cours d'analyse par l'admin
        CONFIRMEE,          // Fraude confirmée par l'admin
        INFONDEE,           // Signalement infondé
        ENQUETE_SUPPLEMENTAIRE, // Enquête supplémentaire requise
        CLOTUREE            // Alerte clôturée
    }

    /**
     * Énumération des niveaux de risque.
     */
    public enum NiveauRisque {
        FAIBLE,
        MOYEN,
        ELEVE,
        CRITIQUE
    }
}
