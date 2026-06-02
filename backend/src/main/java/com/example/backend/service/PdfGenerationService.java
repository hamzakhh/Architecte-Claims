package com.example.backend.service;

import com.example.backend.model.Claim;
import com.example.backend.model.Reimbursement;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ReimbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de génération de documents PDF.
 * Permet de générer des lettres officielles de remboursement ou de rejet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final ClaimRepository claimRepository;
    private final ReimbursementRepository reimbursementRepository;

    /**
     * Génère une lettre de remboursement au format PDF.
     *
     * @param claimId l'identifiant du sinistre
     * @param reimbursementId l'identifiant du remboursement
     * @return les bytes du PDF généré
     */
    public byte[] genererLettreRemboursement(String claimId, String reimbursementId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));
        
        Reimbursement reimbursement = reimbursementRepository.findById(reimbursementId)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé: " + reimbursementId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // En-tête de la lettre
            String contenu = genererEnteteLettre();
            contenu += genererCorpsLettreRemboursement(claim, reimbursement);
            contenu += genererPiedPage();
            
            // Conversion en bytes (simulation - en production utiliser iText ou PDFBox)
            byte[] pdfBytes = contenu.getBytes("UTF-8");
            
            log.info("Lettre de remboursement générée pour le sinistre {}", claimId);
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF de remboursement", e);
            throw new RuntimeException("Erreur de génération PDF: " + e.getMessage());
        }
    }

    /**
     * Génère une lettre de rejet au format PDF.
     *
     * @param claimId l'identifiant du sinistre
     * @param motif le motif du rejet
     * @return les bytes du PDF généré
     */
    public byte[] genererLettreRejet(String claimId, String motif) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            String contenu = genererEnteteLettre();
            contenu += genererCorpsLettreRejet(claim, motif);
            contenu += genererPiedPage();
            
            byte[] pdfBytes = contenu.getBytes("UTF-8");
            
            log.info("Lettre de rejet générée pour le sinistre {}", claimId);
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF de rejet", e);
            throw new RuntimeException("Erreur de génération PDF: " + e.getMessage());
        }
    }

    private String genererEnteteLettre() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        String dateActuelle = java.time.LocalDate.now().format(dtf);
        
        return """
            L'ASSUREUR PRÉCIS SA
            12 Avenue Habib Bourguiba
            1000 Tunis
            
            À l'attention de l'assuré
            """ + dateActuelle + """
            
            Objet: Décision concernant votre déclaration de sinistre
            
            Référence: 
            """;
    }

    private String genererCorpsLettreRemboursement(Claim claim, Reimbursement reimbursement) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        return String.format("""
            Madame, Monsieur,
            
            Nous faisons suite à votre déclaration de sinistre enregistrée sous la référence %s.
            
            Après étude complète de votre dossier, nous avons le plaisir de vous informer que votre sinistre a été accepté.
            
            Détails du sinistre:
            - Date du sinistre: %s
            - Lieu: %s
            - Type: %s
            
            Décision d'indemnisation:
            - Montant accordé: %.2f DT
            - Méthode de paiement: %s
            - Date du paiement: %s
            
            Le paiement sera effectué via Stripe (carte bancaire) dans les prochains jours.
            
            Nous vous remercions de votre confiance et restons à votre disposition pour toute information complémentaire.
            
            Cordialement,
            
            Le Service des Sinistres
            L'Assureur Précis
            """,
            claim.getReference(),
            claim.getDateSinistre(),
            claim.getLieu(),
            claim.getType(),
            reimbursement.getMontantFinal(),
            "Carte bancaire (Stripe)",
            reimbursement.getDatePaiement() != null ? reimbursement.getDatePaiement().format(dtf) : "En cours"
        );
    }

    private String genererCorpsLettreRejet(Claim claim, String motif) {
        return String.format("""
            Madame, Monsieur,
            
            Nous faisons suite à votre déclaration de sinistre enregistrée sous la référence %s.
            
            Après étude approfondie de votre dossier, nous regrettons de vous informer que votre sinistre ne peut être pris en charge au titre de votre contrat d'assurance.
            
            Détails du sinistre:
            - Date du sinistre: %s
            - Lieu: %s
            - Type: %s
            
            Motif du refus:
            %s
            
            Cette décision est fondée sur les conditions générales de votre contrat qui excluent la prise en charge de ce type de sinistre.
            
            Bien entendu, vous conservez la possibilité de contester cette décision en faisant appel à notre service médiation ou en saisissant les autorités compétentes.
            
            Nous restons à votre disposition pour tout renseignement complémentaire.
            
            Cordialement,
            
            Le Service des Sinistres
            L'Assureur Précis
            """,
            claim.getReference(),
            claim.getDateSinistre(),
            claim.getLieu(),
            claim.getType(),
            motif
        );
    }

    private String genererPiedPage() {
        return """
            
            ---
            L'Assureur Précis SA
            Capital social: 5 000 000 DT
            SIRET: TN 159847202
            Agrément CGA: Actif
            Email: contact@lassureurprecis.tn
            www.lassureurprecis.tn
            """;
    }
}
