package com.example.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Stripe pour le traitement des paiements.
 * Initialise la clé API Stripe au démarrage de l'application.
 * 
 * Cartes acceptées : Visa, Mastercard, et toutes les cartes supportées par Stripe.
 */
@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecretKey;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialisé avec succès — Cartes acceptées : Visa, Mastercard, etc.");
    }
}
