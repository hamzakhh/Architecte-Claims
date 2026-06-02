package com.example.backend.dto;

import com.example.backend.model.Reimbursement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReimbursementResponse {
    private String id;
    private String claimId;
    private String claimReference;
    private String assureId;
    private String assureNom;
    private String reference;

    // Calcul automatisé
    private double montantDegats;
    private double capitalAssure;
    private double franchise;
    private double plafondGarantie;
    private double tauxRemboursement;
    private String typeSinistre;
    private double montantApresFranchise;
    private double montantIndemnisationCalcule;
    private String detailCalcul;
    private String justification;

    // Montants
    private double montantPropose;
    private double montantFinal;

    // Paiement via Stripe
    private Reimbursement.MethodePaiement methodePaiement;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String transactionId;
    private String referencePaiement;
    private String confirmationPaiement;

    // Workflow
    private Reimbursement.StatutRemboursement statut;
    private List<Reimbursement.EtapeWorkflow> historiqueWorkflow;

    private String gestionnaireId;
    private String gestionnaireNom;
    private String motifRefus;
    private String notes;

    private LocalDateTime dateProposition;
    private LocalDateTime dateValidation;
    private LocalDateTime dateTraitement;
    private LocalDateTime datePaiement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
