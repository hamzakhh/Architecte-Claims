package com.example.backend.controller;

import com.example.backend.dto.CalculIndemnisationRequest;
import com.example.backend.dto.CalculIndemnisationResponse;
import com.example.backend.dto.PropositionIndemnisationResponse;
import com.example.backend.dto.ReimbursementRequest;
import com.example.backend.dto.ReimbursementResponse;
import com.example.backend.model.Reimbursement;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.PdfGenerationService;
import com.example.backend.service.ReimbursementService;
import com.example.backend.service.StripeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService;

    private String getCurrentUserId() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).map(User::getId).orElseThrow();
    }

    // ===== 1. Calcul automatisé (prévisualisation) =====

    @PostMapping("/calculer")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<CalculIndemnisationResponse> calculerIndemnisation(@Valid @RequestBody CalculIndemnisationRequest request) {
        return ResponseEntity.ok(reimbursementService.calculerIndemnisation(request));
    }

    // ===== 2. Création de remboursement avec calcul automatisé =====

    @PostMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> createReimbursement(@Valid @RequestBody ReimbursementRequest request) {
        return ResponseEntity.ok(reimbursementService.createReimbursement(request));
    }

    // ===== 3. Proposition détaillée =====

    @GetMapping("/{id}/proposition")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropositionIndemnisationResponse> getPropositionDetaillee(@PathVariable String id) {
        return ResponseEntity.ok(reimbursementService.getPropositionDetaillee(id));
    }

    // ===== 4. Requêtes =====

    @GetMapping("/my")
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<List<ReimbursementResponse>> getMyReimbursements() {
        return ResponseEntity.ok(reimbursementService.getReimbursementsByAssure(getCurrentUserId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<ReimbursementResponse>> getAllReimbursements() {
        return ResponseEntity.ok(reimbursementService.getAllReimbursements());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReimbursementResponse> getReimbursementById(@PathVariable String id) {
        return ResponseEntity.ok(reimbursementService.getReimbursementById(id));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<ReimbursementResponse>> getReimbursementsByStatut(@PathVariable Reimbursement.StatutRemboursement statut) {
        return ResponseEntity.ok(reimbursementService.getReimbursementsByStatut(statut));
    }

    // ===== 5. Statistiques =====

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatistiques() {
        return ResponseEntity.ok(reimbursementService.getStatistiques());
    }

    // ===== 6. Workflow =====

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<ReimbursementResponse> validateReimbursement(@PathVariable String id) {
        return ResponseEntity.ok(reimbursementService.validerParAssure(id));
    }

    @PutMapping("/{id}/refuse")
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<ReimbursementResponse> refuseReimbursement(@PathVariable String id, @RequestBody String motif) {
        return ResponseEntity.ok(reimbursementService.refuserParAssure(id, motif));
    }

    // ===== 6a. Stripe : création de session de paiement =====

    @PostMapping("/{id}/stripe/checkout")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> createStripeCheckoutSession(@PathVariable String id) {
        return ResponseEntity.ok(stripeService.createCheckoutSession(id));
    }

    @GetMapping("/{id}/stripe/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyStripeSession(@PathVariable String id) {
        ReimbursementResponse rem = reimbursementService.getReimbursementById(id);
        if (rem.getStripeSessionId() != null) {
            return ResponseEntity.ok(stripeService.verifySessionStatus(rem.getStripeSessionId()));
        }
        return ResponseEntity.ok(Map.of("paymentStatus", "no_session"));
    }

    // ===== 6b. Stripe Webhook (pas d'auth — appelé par Stripe) =====

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        stripeService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("");
    }

    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> processReimbursement(@PathVariable String id) {
        return ResponseEntity.ok(reimbursementService.traiterRemboursement(id));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> confirmPayment(@PathVariable String id) {
        return ResponseEntity.ok(reimbursementService.confirmerPaiement(id));
    }

    // ===== 7. Génération PDF =====

    @GetMapping("/{id}/lettre-remboursement")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<byte[]> genererLettreRemboursement(@PathVariable String id) {
        byte[] pdfBytes = pdfGenerationService.genererLettreRemboursement(
                reimbursementService.getReimbursementById(id).getClaimId(), id);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"lettre-remboursement-" + id + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/lettre-rejet")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<byte[]> genererLettreRejet(
            @PathVariable String id,
            @RequestBody String motif) {
        
        byte[] pdfBytes = pdfGenerationService.genererLettreRejet(
                reimbursementService.getReimbursementById(id).getClaimId(), motif);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"lettre-rejet-" + id + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }
}
