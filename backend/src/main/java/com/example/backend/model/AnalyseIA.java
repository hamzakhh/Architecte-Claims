package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité AnalyseIA stockée dans MongoDB (collection "analyses_ia").
 * Représente l'analyse automatique d'un sinistre par l'IA (Ollama).
 */
@Data
@Document(collection = "analyses_ia")
public class AnalyseIA {

    @Id
    private String id;

    @Indexed
    private String claimId; // Identifiant du sinistre analysé

    // Scores d'analyse
    private int scoreComplexite; // 0-100 : complexité du sinistre
    private int scoreRisque;     // 0-100 : niveau de risque (fraude, etc.)
    private int scoreConfiance;  // 0-100 : confiance dans l'analyse

    // Estimation des dommages
    private Double montantEstime; // Montant estimé des dégâts par l'IA
    private String devise;        // Devise (TND par défaut)

    // Classification
    private Severite severite;    // Sévérité déterminée par l'IA
    private String categorieDetectee; // Catégorie détectée automatiquement
    private List<String> motsCles;    // Mots-clés extraits de la description

    // Recommandation de l'IA
    private boolean necessiteExpertHumain; // L'IA recommande-t-elle un expert ?
    private String recommandation;         // Texte de recommandation
    private String justification;          // Justification de la recommandation

    // Analyse détaillée (réponse IA)
    private String resumeAnalyse;       // Résumé de l'analyse
    private String pointsAttention;     // Points d'attention identifiés
    private String elementsFraude;      // Éléments suspects (fraude)
    private String recommandationsAction; // Actions recommandées

    // Métadonnées
    private TypeAnalyse typeAnalyse;    // Type d'analyse effectuée
    private StatutAnalyse statut;       // Statut de l'analyse
    private String modeleIA;            // Modèle IA utilisé (ex: llama3)
    private int tokensUtilises;         // Nombre de tokens consommés
    private double coutEstime;          // Coût estimé de l'appel API

    private LocalDateTime dateAnalyse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Type d'analyse effectuée.
     */
    public enum TypeAnalyse {
        INITIALE,          // Analyse initiale automatique à la déclaration
        APPROFONDIE,       // Analyse approfondie demandée par le gestionnaire
        REANALYSE,         // Ré-analyse après complément d'informations
        VERIFICATION_FRAUDE // Vérification anti-fraude ciblée
    }

    /**
     * Statut de l'analyse.
     */
    public enum StatutAnalyse {
        EN_COURS,      // Analyse en cours de traitement
        TERMINEE,      // Analyse terminée avec succès
        ERREUR,        // Erreur lors de l'analyse
        EXPIREE        // Analyse expirée (données trop anciennes)
    }

    /**
     * Sévérité du sinistre selon l'IA.
     */
    public enum Severite {
        FAIBLE,       // Sinistre mineur, traitement rapide
        MODEREE,      // Sinistre modéré, traitement standard
        ELEVEE,       // Sinistre important, attention requise
        CRITIQUE      // Sinistre critique, expert requis
    }
}
