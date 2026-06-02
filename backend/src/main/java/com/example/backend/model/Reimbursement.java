package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité Remboursement stockée dans MongoDB (collection "reimbursements").
 * Représente un remboursement/indemnisation lié à un sinistre.
 */
@Data
@Document(collection = "reimbursements")
public class Reimbursement {

    @Id
    private String id;

    @Indexed
    private String claimId; // Identifiant du sinistre concerné

    @Indexed
    private String assureId; // Identifiant de l'assuré bénéficiaire

    private String reference; // Référence unique du remboursement (REM-YYYY-XXXX)

    // ===== Calcul automatisé de l'indemnisation (Section 1) =====
    private double montantDegats; // Montant total des dégâts déclarés
    private double capitalAssure; // Capital assuré (montant maximum couvert)
    private double franchise; // Partie que l'assuré doit payer lui-même
    private double plafondGarantie; // Limite de remboursement
    private double tauxRemboursement; // Taux de remboursement (ex: 0.90 pour 90%)
    private String typeSinistre; // Type de sinistre : accident, vol, incendie, etc.

    private double montantApresFranchise; // Montant après déduction de la franchise
    private double montantIndemnisationCalcule; // Montant calculé automatiquement

    private String detailCalcul; // Détail du calcul (texte explicatif)
    private String justification; // Justification basée sur le contrat/garanties

    // ===== Montants =====
    private double montantPropose; // Montant proposé par l'assurance
    private double montantFinal; // Montant final validé

    // ===== Paiement via Stripe (Section 3) =====
    private MethodePaiement methodePaiement; // Méthode de paiement (Stripe uniquement)
    private String stripeSessionId; // ID de la session Stripe Checkout
    private String stripePaymentIntentId; // ID du Payment Intent Stripe
    private String transactionId; // Identifiant de transaction (traçabilité)
    private String referencePaiement; // Référence du paiement
    private String confirmationPaiement; // Confirmation automatique

    // ===== Workflow / Suivi (Section 4) =====
    private StatutRemboursement statut; // Statut du remboursement
    private List<EtapeWorkflow> historiqueWorkflow; // Historique des étapes

    private String gestionnaireId; // Gestionnaire ayant validé
    private String motifRefus; // Motif en cas de refus
    private String notes; // Notes internes

    private LocalDateTime dateProposition; // Date de la proposition
    private LocalDateTime dateValidation; // Date de validation par l'assuré
    private LocalDateTime dateTraitement; // Date de traitement du paiement
    private LocalDateTime datePaiement; // Date du paiement effectif

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Énumération des statuts possibles d'un remboursement.
     */
    public enum StatutRemboursement {
        EN_ATTENTE,        // Dossier reçu mais pas encore traité
        VALIDEE,           // Proposition acceptée par l'assuré
        EN_COURS_TRAITEMENT, // Paiement en préparation ou en exécution
        PAYE,              // Argent transféré à l'assuré
        REFUSE             // Refusé par l'assuré ou l'assurance
    }

    /**
     * Méthodes de paiement via Stripe.
     * Seul le paiement par carte bancaire est supporté (Visa, Mastercard, etc.).
     */
    public enum MethodePaiement {
        CARTE_BANCAIRE  // Paiement par carte bancaire via Stripe (Visa, Mastercard, etc.)
    }

    /**
     * Étape du workflow de remboursement (traçabilité).
     */
    @Data
    public static class EtapeWorkflow {
        private StatutRemboursement statut;
        private String description;
        private String effectuePar; // ID de l'utilisateur
        private String effectueParNom; // Nom de l'utilisateur
        private LocalDateTime date;
    }
}
