package com.example.backend.service;

import com.stripe.model.checkout.Session;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;
import com.example.backend.model.Reimbursement;
import com.example.backend.repository.ReimbursementRepository;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de paiement Stripe.
 * Gère la création de sessions Checkout, la vérification des paiements
 * et le traitement des webhooks Stripe.
 * 
 * Cartes acceptées : Visa, Mastercard, et toutes les cartes supportées par Stripe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final ReimbursementRepository reimbursementRepository;
    private final NotificationService notificationService;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe API key initialized");
    }

    /**
     * Crée une session Stripe Checkout pour un remboursement.
     * L'assuré sera redirigé vers la page de paiement Stripe.
     */
    public Map<String, String> createCheckoutSession(String reimbursementId) {
        try {
            Reimbursement rem = reimbursementRepository.findById(reimbursementId)
                    .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

            if (rem.getStatut() != Reimbursement.StatutRemboursement.VALIDEE) {
                throw new RuntimeException("Le remboursement doit être validé par l'assuré avant paiement");
            }

            if (rem.getMontantFinal() <= 0) {
                throw new RuntimeException("Le montant final doit être supérieur à 0 pour créer une session de paiement");
            }

            // Montant en centimes (Stripe exige des entiers positifs, minimum 50 centimes pour EUR)
            long amountInCents = Math.round(rem.getMontantFinal() * 100);
            if (amountInCents < 50) {
                throw new RuntimeException("Le montant est trop faible pour Stripe (minimum 0.50 EUR)");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("mode", "payment");
            params.put("payment_method_types", new String[]{"card"});
            params.put("line_items", new Object[]{
                    Map.of(
                            "price_data", Map.of(
                                    "currency", "eur",  // Stripe ne supporte pas TND — montant converti en EUR pour le paiement
                                    "product_data", Map.of(
                                            "name", "Indemnisation sinistre " + rem.getReference(),
                                            "description", "Remboursement assurance — " + rem.getTypeSinistre()
                                    ),
                                    "unit_amount", amountInCents
                            ),
                            "quantity", 1
                    )
            });
            params.put("success_url", frontendUrl + "/assure/remboursements?payment=success&session_id={CHECKOUT_SESSION_ID}");
            params.put("cancel_url", frontendUrl + "/assure/remboursements?payment=cancel");
            params.put("metadata", Map.of(
                    "reimbursement_id", rem.getId(),
                    "reference", rem.getReference()
            ));

            Session session = Session.create(params);

            // Sauvegarder l'ID de session Stripe
            rem.setStripeSessionId(session.getId());
            rem.setStatut(Reimbursement.StatutRemboursement.EN_COURS_TRAITEMENT);
            rem.setDateTraitement(LocalDateTime.now());
            rem.setUpdatedAt(LocalDateTime.now());

            addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.EN_COURS_TRAITEMENT,
                    "Session Stripe créée — En attente du paiement par carte bancaire",
                    "SYSTEM", "Système Stripe");

            reimbursementRepository.save(rem);

            log.info("Session Stripe créée pour le remboursement {} — Session ID: {}", rem.getReference(), session.getId());

            return Map.of(
                    "sessionId", session.getId(),
                    "url", session.getUrl()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la création de la session Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création de la session de paiement: " + e.getMessage());
        }
    }

    /**
     * Traite les webhooks Stripe (événements de paiement).
     * Gère les événements : checkout.session.completed, payment_intent.succeeded, etc.
     */
    public void handleWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutCompleted(event);
                    break;
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                default:
                    log.info("Événement Stripe non géré: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur webhook Stripe: " + e.getMessage());
        }
    }

    /**
     * Vérifie manuellement le statut d'une session Stripe Checkout.
     */
    public Map<String, Object> verifySessionStatus(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            String paymentStatus = session.getPaymentStatus();

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", session.getId());
            result.put("paymentStatus", paymentStatus);
            result.put("paymentIntentId", session.getPaymentIntent());

            if ("paid".equals(paymentStatus)) {
                // Mettre à jour le remboursement si pas déjà fait
                Reimbursement rem = reimbursementRepository.findByStripeSessionId(sessionId);
                if (rem != null && rem.getStatut() != Reimbursement.StatutRemboursement.PAYE) {
                    rem.setStripePaymentIntentId(session.getPaymentIntent());
                    confirmStripePayment(rem, session.getPaymentIntent());
                    result.put("reimbursementUpdated", true);
                } else {
                    result.put("reimbursementUpdated", false);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("Erreur vérification session Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur vérification session: " + e.getMessage());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        deserializer.getObject().ifPresent(stripeObject -> {
            Session session = (Session) stripeObject;
            String reimbursementId = session.getMetadata().get("reimbursement_id");

            if (reimbursementId != null) {
                Reimbursement rem = reimbursementRepository.findById(reimbursementId).orElse(null);
                if (rem != null && rem.getStatut() != Reimbursement.StatutRemboursement.PAYE) {
                    rem.setStripePaymentIntentId(session.getPaymentIntent());
                    confirmStripePayment(rem, session.getPaymentIntent());
                    log.info("Paiement Stripe confirmé via webhook pour {}", rem.getReference());
                }
            }
        });
    }

    private void handlePaymentIntentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        deserializer.getObject().ifPresent(stripeObject -> {
            PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
            Reimbursement rem = reimbursementRepository.findByStripePaymentIntentId(paymentIntent.getId());
            if (rem != null && rem.getStatut() != Reimbursement.StatutRemboursement.PAYE) {
                confirmStripePayment(rem, paymentIntent.getId());
                log.info("PaymentIntent confirmé via webhook pour {}", rem.getReference());
            }
        });
    }

    private void confirmStripePayment(Reimbursement rem, String paymentIntentId) {
        rem.setStatut(Reimbursement.StatutRemboursement.PAYE);
        rem.setDatePaiement(LocalDateTime.now());
        rem.setStripePaymentIntentId(paymentIntentId);
        rem.setTransactionId(String.format("TXN-%s-%06d", Year.now().getValue(), System.nanoTime() % 1000000));
        rem.setReferencePaiement(String.format("REF-%s", rem.getReference()));
        rem.setConfirmationPaiement(String.format("CONF-STRIPE-%s-%d", rem.getReference(), System.currentTimeMillis()));
        rem.setUpdatedAt(LocalDateTime.now());

        addEtapeWorkflow(rem, Reimbursement.StatutRemboursement.PAYE,
                String.format("Paiement effectué via Stripe (Visa/Mastercard) — Confirmation : %s", rem.getConfirmationPaiement()),
                "SYSTEM", "Système Stripe");

        reimbursementRepository.save(rem);

        notificationService.envoyerNotification(rem.getAssureId(), "Paiement effectué",
                String.format("Le remboursement %s de %,.2f DT a été payé via Stripe (carte bancaire).", rem.getReference(), rem.getMontantFinal()),
                "REMBOURSEMENT", rem.getClaimId());
    }

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
}
