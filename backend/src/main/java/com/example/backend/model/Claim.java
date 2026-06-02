package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité Sinistre stockée dans MongoDB (collection "claims").
 * Représente une déclaration de sinistre faite par un assuré.
 */
@Data
@Document(collection = "claims")
public class Claim {

    @Id
    private String id;

    @Indexed
    private String reference; // Numéro de référence unique (SIN-YYYY-XXXX)

    @Indexed
    private String assureId; // Identifiant de l'assuré ayant déclaré le sinistre

    private String categorie; // Catégorie automatique : accident, incendie, vol, degat_eaux, catastrophe, autre

    private String type; // Type de sinistre : water, fire, theft, auto, natural

    private Double latitude; // Latitude de géolocalisation
    private Double longitude; // Longitude de géolocalisation
    private String description; // Description détaillée de l'incident
    private String dateSinistre; // Date du sinistre (format yyyy-MM-dd)
    private String heureSinistre; // Heure du sinistre (format HH:mm)
    private String lieu; // Lieu du sinistre
    private String notesLieu; // Notes supplémentaires sur le lieu
    private List<String> piecesJointes; // Noms des fichiers pièce jointes
    private String estimation; // Estimation du montant des dégâts

    private String notesInternes; // Notes internes visibles uniquement par l'équipe

    // Qualification du sinistre (2.2.1)
    private GraviteSinistre gravite; // Gravité : MINEURE, MODEREE, MAJEURE, CRITIQUE
    private String couvertureContractuelle; // Couverture contractuelle applicable
    private Double franchise; // Montant de la franchise applicable
    private Double plafondCouverture; // Plafond de couverture du contrat

    // Indemnisation (2.2.4)
    private Double montantIndemnisationPropose; // Montant d'indemnisation proposé par le gestionnaire
    private Double montantIndemnisationFinal; // Montant final après acceptation/ajustement
    private String motifIndemnisation; // Motif/calcul de l'indemnisation
    private Boolean indemnisationAcceptee; // L'assuré a-t-il accepté la proposition
    private String motifRefusIndemnisation; // Motif de refus par l'assuré (recours)
    private Boolean recoursEnCours; // Un recours/litige est-il en cours
    private LocalDateTime datePaiement; // Date du paiement de l'indemnisation

    private StatutSinistre statut; // Statut du sinistre

    private String gestionnaireId; // Identifiant du gestionnaire assigné
    private String expertId; // Identifiant de l'expert assigné
    private String analyseIAId; // Identifiant de l'analyse IA associée

    private LocalDateTime createdAt; // Date de création de la déclaration
    private LocalDateTime updatedAt; // Date de dernière mise à jour

    /**
     * Énumération des statuts possibles d'un sinistre.
     */
    public enum StatutSinistre {
        EN_COURS,       // Déclaration soumise, en attente de traitement
        EN_REVISION,    // En cours de révision par un gestionnaire
        EXPERTISE,      // Envoyé à un expert pour évaluation
        VALIDE,         // Sinistre validé
        REFUSE,         // Sinistre refusé
        INDEMNISATION_PROPOSEE, // Proposition d'indemnisation faite à l'assuré
        INDEMNISATION_ACCEPTEE, // Indemnisation acceptée par l'assuré
        PAIEMENT_EN_COURS,      // Paiement en cours de traitement
        RECOURS,        // Recours/litige en cours
        CLOTURE,        // Sinistre clôturé
        ARCHIVE         // Sinistre archivé
    }

    public enum GraviteSinistre {
        MINEURE,
        MODEREE,
        MAJEURE,
        CRITIQUE
    }
}
