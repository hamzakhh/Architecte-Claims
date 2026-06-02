package com.example.backend.service;

import com.example.backend.dto.CalculIndemnisationRequest;
import com.example.backend.dto.CalculIndemnisationResponse;
import com.example.backend.dto.PropositionIndemnisationResponse;
import com.example.backend.dto.ReimbursementRequest;
import com.example.backend.dto.ReimbursementResponse;
import com.example.backend.model.Claim;
import com.example.backend.model.Reimbursement;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ReimbursementRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private final ReimbursementRepository reimbursementRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ===== 1. Calcul automatisé de l'indemnisation =====

    /**
     * Calcule automatiquement le montant d'indemnisation sans créer de remboursement.
     * Permet au gestionnaire de prévisualiser le calcul avant de générer la proposition.
     */
    public CalculIndemnisationResponse calculerIndemnisation(CalculIndemnisationRequest request) {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        double montantDegats = request.getMontantDegats();
        double franchise = request.getFranchise();
        double tauxRemboursement = request.getTauxRemboursement();
        double plafondGarantie = request.getPlafondGarantie();

        // Étape 1 : Montant après franchise
        double montantApresFranchise = Math.max(0, montantDegats - franchise);

        // Étape 2 : Application du taux de remboursement
        double montantIndemnisationCalcule = montantApresFranchise * tauxRemboursement;

        // Étape 3 : Vérification du plafond de garantie
        boolean plafondAtteint = montantIndemnisationCalcule > plafondGarantie;
        double montantFinalCalcule = plafondAtteint ? plafondGarantie : montantIndemnisationCalcule;

        // Génération du détail du calcul
        String detailCalcul = String.format(
                "Détail du calcul d'indemnisation :\n" +
                "• Montant des dégâts : %,.2f DT\n" +
                "• Franchise : %,.2f DT\n" +
                "• Montant après franchise : %,.2f DT (%,.2f - %,.2f)\n" +
                "• Taux de remboursement : %.0f%%\n" +
                "• Indemnisation calculée : %,.2f DT (%,.2f × %.0f%%)\n" +
                "• Plafond de garantie : %,.2f DT\n" +
                (plafondAtteint ? "⚠ Plafond atteint — montant limité au plafond\n" : "✓ Plafond non atteint\n") +
                "• MONTANT FINAL : %,.2f DT",
                montantDegats, franchise, montantApresFranchise, montantDegats, franchise,
                tauxRemboursement * 100, montantIndemnisationCalcule, montantApresFranchise, tauxRemboursement * 100,
                plafondGarantie, montantFinalCalcule
        );

        return CalculIndemnisationResponse.builder()
                .claimId(request.getClaimId())
                .montantDegats(montantDegats)
                .franchise(franchise)
                .montantApresFranchise(montantApresFranchise)
                .tauxRemboursement(tauxRemboursement)
                .montantIndemnisationCalcule(montantIndemnisationCalcule)
                .plafondGarantie(plafondGarantie)
                .plafondAtteint(plafondAtteint)
                .montantFinalCalcule(montantFinalCalcule)
                .detailCalcul(detailCalcul)
                .build();
    }

    // ===== 2. Création de remboursement avec calcul automatisé =====

    public ReimbursementResponse createReimbursement(ReimbursementRequest request) {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));

        // Calcul automatisé
        double montantDegats = request.getMontantDegats();
        double franchise = request.getFranchise();
        double tauxRemboursement = request.getTauxRemboursement();
        double plafondGarantie = request.getPlafondGarantie();
        double capitalAssure = request.getCapitalAssure();

        double montantApresFranchise = Math.max(0, montantDegats - franchise);
        double montantIndemnisationCalcule = montantApresFranchise * tauxRemboursement;
        boolean plafondAtteint = montantIndemnisationCalcule > plafondGarantie;
        double montantCalcule = plafondAtteint ? plafondGarantie : montantIndemnisationCalcule;

        // Le montant proposé peut être ajusté par le gestionnaire, sinon = montant calculé
        double montantPropose = request.getMontantPropose() > 0 ? request.getMontantPropose() : montantCalcule;

        String detailCalcul = String.format(
                "Montant dégâts: %,.2f DT | Franchise: %,.2f DT | Après franchise: %,.2f DT | Taux: %.0f%% | Calculé: %,.2f DT%s | Proposé: %,.2f DT",
                montantDegats, franchise, montantApresFranchise, tauxRemboursement * 100,
                montantIndemnisationCalcule, plafondAtteint ? " (plafonné à " + plafondGarantie + " DT)" : "",
                montantPropose
        );

        Reimbursement rem = new Reimbursement();
        rem.setClaimId(request.getClaimId());
        rem.setAssureId(claim.getAssureId());
        rem.setReference(String.format("REM-%s-%04d", Year.now().getValue(), reimbursementRepository.count() + 1));

        // Champs de calcul
        rem.setMontantDegats(montantDegats);
        rem.setCapitalAssure(capitalAssure);
        rem.setFranchise(franchise);
        rem.setPlafondGarantie(plafondGarantie);
        rem.setTauxRemboursement(tauxRemboursement);
        rem.setTypeSinistre(request.getTypeSinistre() != null ? request.getTypeSinistre() : claim.getCategorie());
        rem.setMontantApresFranchise(montantApresFranchise);
        rem.setMontantIndemnisationCalcule(montantIndemnisationCalcule);
        rem.setDetailCalcul(detailCalcul);
        rem.setJustification(request.getJustification());

        // Montants
        rem.setMontantPropose(montantPropose);
        rem.setMontantFinal(montantPropose);

        // Paiement — Stripe uniquement (carte bancaire)
        rem.setMethodePaiement(Reimbursement.MethodePaiement.CARTE_BANCAIRE);

        // Workflow initial
        rem.setStatut(Reimbursement.StatutRemboursement.EN_ATTENTE);
        rem.setGestionnaireId(gestionnaire.getId());
        rem.setNotes(request.getNotes());
        rem.setDateProposition(LocalDateTime.now());

        // Historique workflow — étape initiale
        Reimbursement.EtapeWorkflow etapeInitiale = new Reimbursement.EtapeWorkflow();
        etapeInitiale.setStatut(Reimbursement.StatutRemboursement.EN_ATTENTE);
        etapeInitiale.setDescription("Proposition d'indemnisation générée — en attente de validation par l'assuré");
        etapeInitiale.setEffectuePar(gestionnaire.getId());
        etapeInitiale.setEffectueParNom(gestionnaire.getFullName());
        etapeInitiale.setDate(LocalDateTime.now());
        rem.setHistoriqueWorkflow(new ArrayList<>(List.of(etapeInitiale)));

        rem.setCreatedAt(LocalDateTime.now());
        rem.setUpdatedAt(LocalDateTime.now());

        reimbursementRepository.save(rem);

        notificationService.envoyerNotification(claim.getAssureId(), "Proposition d'indemnisation",
                String.format("Une proposition de %,.2f DT a été émise pour votre sinistre %s", montantPropose, claim.getReference()),
                "REMBOURSEMENT", claim.getId());

        return mapToResponse(rem);
    }

    // ===== 3. Génération de proposition d'indemnisation détaillée =====

    public PropositionIndemnisationResponse getPropositionDetaillee(String id) {
        Reimbursement rem = reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        String assureNom = userRepository.findById(rem.getAssureId()).map(User::getFullName).orElse(null);
        String gestionnaireNom = rem.getGestionnaireId() != null ? userRepository.findById(rem.getGestionnaireId()).map(User::getFullName).orElse(null) : null;
        String claimRef = claimRepository.findById(rem.getClaimId()).map(Claim::getReference).orElse(null);

        return PropositionIndemnisationResponse.builder()
                .id(rem.getId()).claimId(rem.getClaimId()).claimReference(claimRef)
                .assureId(rem.getAssureId()).assureNom(assureNom).reference(rem.getReference())
                .montantDegats(rem.getMontantDegats()).capitalAssure(rem.getCapitalAssure())
                .franchise(rem.getFranchise()).plafondGarantie(rem.getPlafondGarantie())
                .tauxRemboursement(rem.getTauxRemboursement()).typeSinistre(rem.getTypeSinistre())
                .montantApresFranchise(rem.getMontantApresFranchise())
                .montantIndemnisationCalcule(rem.getMontantIndemnisationCalcule())
                .detailCalcul(rem.getDetailCalcul()).justification(rem.getJustification())
                .montantPropose(rem.getMontantPropose()).montantFinal(rem.getMontantFinal())
                .methodePaiement(rem.getMethodePaiement()).stripeSessionId(rem.getStripeSessionId())
                .stripePaymentIntentId(rem.getStripePaymentIntentId())
                .transactionId(rem.getTransactionId()).referencePaiement(rem.getReferencePaiement())
                .confirmationPaiement(rem.getConfirmationPaiement())
                .statut(rem.getStatut()).historiqueWorkflow(rem.getHistoriqueWorkflow())
                .gestionnaireId(rem.getGestionnaireId()).gestionnaireNom(gestionnaireNom)
                .motifRefus(rem.getMotifRefus()).notes(rem.getNotes())
                .dateProposition(rem.getDateProposition()).dateValidation(rem.getDateValidation())
                .dateTraitement(rem.getDateTraitement()).datePaiement(rem.getDatePaiement())
                .createdAt(rem.getCreatedAt()).build();
    }

    // ===== 4. Workflow : validation, refus, traitement, paiement =====

    public ReimbursementResponse validerParAssure(String id) {
        Reimbursement rem = reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User assure = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Assuré non trouvé"));

        rem.setStatut(Reimbursement.StatutRemboursement.VALIDEE);
        rem.setDateValidation(LocalDateTime.now());
        rem.setUpdatedAt(LocalDateTime.now());

        addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.VALIDEE,
                "Proposition acceptée par l'assuré", assure.getId(), assure.getFullName());

        reimbursementRepository.save(rem);
        notificationService.envoyerNotification(rem.getGestionnaireId(), "Indemnisation validée",
                "L'assuré a validé la proposition d'indemnisation " + rem.getReference(), "REMBOURSEMENT", rem.getClaimId());
        return mapToResponse(rem);
    }

    public ReimbursementResponse refuserParAssure(String id, String motif) {
        Reimbursement rem = reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User assure = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Assuré non trouvé"));

        rem.setStatut(Reimbursement.StatutRemboursement.REFUSE);
        rem.setMotifRefus(motif);
        rem.setDateValidation(LocalDateTime.now());
        rem.setUpdatedAt(LocalDateTime.now());

        addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.REFUSE,
                "Proposition refusée par l'assuré — Motif : " + motif, assure.getId(), assure.getFullName());

        reimbursementRepository.save(rem);
        notificationService.envoyerNotification(rem.getGestionnaireId(), "Indemnisation refusée",
                "L'assuré a refusé la proposition " + rem.getReference() + " — Motif : " + motif, "REMBOURSEMENT", rem.getClaimId());
        return mapToResponse(rem);
    }

    public ReimbursementResponse traiterRemboursement(String id) {
        Reimbursement rem = reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));

        rem.setStatut(Reimbursement.StatutRemboursement.EN_COURS_TRAITEMENT);
        rem.setDateTraitement(LocalDateTime.now());
        rem.setUpdatedAt(LocalDateTime.now());

        // Génération d'un identifiant de transaction pour traçabilité
        rem.setTransactionId(String.format("TXN-%s-%06d", Year.now().getValue(), System.nanoTime() % 1000000));
        rem.setReferencePaiement(String.format("REF-%s", rem.getReference()));

        addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.EN_COURS_TRAITEMENT,
                "Paiement en préparation — Transaction : " + rem.getTransactionId(),
                gestionnaire.getId(), gestionnaire.getFullName());

        reimbursementRepository.save(rem);
        notificationService.envoyerNotification(rem.getAssureId(), "Traitement en cours",
                "Le remboursement " + rem.getReference() + " est en cours de traitement.", "REMBOURSEMENT", rem.getClaimId());
        return mapToResponse(rem);
    }

    public ReimbursementResponse confirmerPaiement(String id) {
        Reimbursement rem = reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));

        rem.setStatut(Reimbursement.StatutRemboursement.PAYE);
        rem.setDatePaiement(LocalDateTime.now());
        rem.setConfirmationPaiement(String.format("CONF-%s-%d", rem.getReference(), System.currentTimeMillis()));
        rem.setUpdatedAt(LocalDateTime.now());

        addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.PAYE,
                String.format("Paiement effectué — %s — Confirmation : %s",
                        rem.getMethodePaiement().name(), rem.getConfirmationPaiement()),
                gestionnaire.getId(), gestionnaire.getFullName());

        reimbursementRepository.save(rem);
        notificationService.envoyerNotification(rem.getAssureId(), "Paiement effectué",
                String.format("Le remboursement %s de %,.2f DT a été payé via %s.", rem.getReference(), rem.getMontantFinal(), getMethodeLabel(rem.getMethodePaiement())),
                "REMBOURSEMENT", rem.getClaimId());
        return mapToResponse(rem);
    }

    // ===== 5. Requêtes =====

    public List<ReimbursementResponse> getReimbursementsByAssure(String assureId) {
        return reimbursementRepository.findByAssureIdOrderByCreatedAtDesc(assureId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ReimbursementResponse> getAllReimbursements() {
        return reimbursementRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public ReimbursementResponse getReimbursementById(String id) {
        return mapToResponse(reimbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé")));
    }

    public List<ReimbursementResponse> getReimbursementsByStatut(Reimbursement.StatutRemboursement statut) {
        return reimbursementRepository.findByStatut(statut).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    // ===== 6. Statistiques =====

    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRemboursements", reimbursementRepository.count());
        stats.put("enAttente", reimbursementRepository.countByStatut(Reimbursement.StatutRemboursement.EN_ATTENTE));
        stats.put("validees", reimbursementRepository.countByStatut(Reimbursement.StatutRemboursement.VALIDEE));
        stats.put("enCoursTraitement", reimbursementRepository.countByStatut(Reimbursement.StatutRemboursement.EN_COURS_TRAITEMENT));
        stats.put("payes", reimbursementRepository.countByStatut(Reimbursement.StatutRemboursement.PAYE));
        stats.put("refuses", reimbursementRepository.countByStatut(Reimbursement.StatutRemboursement.REFUSE));

        List<Reimbursement> payes = reimbursementRepository.findByStatut(Reimbursement.StatutRemboursement.PAYE);
        double totalPaye = payes.stream().mapToDouble(Reimbursement::getMontantFinal).sum();
        stats.put("montantTotalPaye", totalPaye);

        List<Reimbursement> enAttente = reimbursementRepository.findByStatut(Reimbursement.StatutRemboursement.EN_ATTENTE);
        double montantEnAttente = enAttente.stream().mapToDouble(Reimbursement::getMontantPropose).sum();
        stats.put("montantEnAttente", montantEnAttente);

        return stats;
    }

    // ===== Helpers =====

    private void addEtapeWorkflow(Reimbursement rem, Reimbursement.StatutRemboursement statut,
                                   String description, String effectuePar, String effectueParNom) {
        if (rem.getHistoriqueWorkflow() == null) {
            rem.setHistoriqueWorkflow(new ArrayList<>());
        }
        Reimbursement.EtapeWorkflow etape = new Reimbursement.EtapeWorkflow();
        etape.setStatut(statut);
        etape.setDescription(description);
        etape.setEffectuePar(effectuePar);
        etape.setEffectueParNom(effectueParNom);
        etape.setDate(LocalDateTime.now());
        rem.getHistoriqueWorkflow().add(etape);
    }

    private String getMethodeLabel(Reimbursement.MethodePaiement methode) {
        return "Carte bancaire (Stripe)";
    }

    private ReimbursementResponse mapToResponse(Reimbursement rem) {
        String assureNom = userRepository.findById(rem.getAssureId()).map(User::getFullName).orElse(null);
        String gestionnaireNom = rem.getGestionnaireId() != null ? userRepository.findById(rem.getGestionnaireId()).map(User::getFullName).orElse(null) : null;
        String claimRef = claimRepository.findById(rem.getClaimId()).map(Claim::getReference).orElse(null);

        return ReimbursementResponse.builder()
                .id(rem.getId()).claimId(rem.getClaimId()).claimReference(claimRef)
                .assureId(rem.getAssureId()).assureNom(assureNom).reference(rem.getReference())
                .montantDegats(rem.getMontantDegats()).capitalAssure(rem.getCapitalAssure())
                .franchise(rem.getFranchise()).plafondGarantie(rem.getPlafondGarantie())
                .tauxRemboursement(rem.getTauxRemboursement()).typeSinistre(rem.getTypeSinistre())
                .montantApresFranchise(rem.getMontantApresFranchise())
                .montantIndemnisationCalcule(rem.getMontantIndemnisationCalcule())
                .detailCalcul(rem.getDetailCalcul()).justification(rem.getJustification())
                .montantPropose(rem.getMontantPropose()).montantFinal(rem.getMontantFinal())
                .methodePaiement(rem.getMethodePaiement()).stripeSessionId(rem.getStripeSessionId())
                .stripePaymentIntentId(rem.getStripePaymentIntentId())
                .transactionId(rem.getTransactionId()).referencePaiement(rem.getReferencePaiement())
                .confirmationPaiement(rem.getConfirmationPaiement())
                .statut(rem.getStatut()).historiqueWorkflow(rem.getHistoriqueWorkflow())
                .gestionnaireId(rem.getGestionnaireId()).gestionnaireNom(gestionnaireNom)
                .motifRefus(rem.getMotifRefus()).notes(rem.getNotes())
                .dateProposition(rem.getDateProposition()).dateValidation(rem.getDateValidation())
                .dateTraitement(rem.getDateTraitement()).datePaiement(rem.getDatePaiement())
                .createdAt(rem.getCreatedAt()).updatedAt(rem.getUpdatedAt()).build();
    }
}
