package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Ticket stockée dans MongoDB (collection "tickets").
 * Représente une demande de support client.
 */
@Data
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String id;

    @Indexed
    private String assureId; // Identifiant de l'assuré ayant créé le ticket

    private String claimId; // Identifiant du sinistre lié (optionnel)

    private String sujet; // Sujet du ticket

    private String description; // Description du problème

    private String categorie; // Catégorie : technique, facturation, sinistre, general

    private StatutTicket statut; // Statut du ticket

    private String assigneA; // Identifiant du gestionnaire assigné

    private List<TicketMessage> messages = new ArrayList<>(); // Messages du ticket

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Messages d'un ticket de support.
     */
    @Data
    public static class TicketMessage {
        private String expediteurId;
        private String expediteurNom;
        private String contenu;
        private LocalDateTime createdAt;
    }

    /**
     * Énumération des statuts possibles d'un ticket.
     */
    public enum StatutTicket {
        OUVERT,         // Ticket ouvert
        EN_COURS,       // Ticket pris en charge
        EN_ATTENTE,     // En attente de réponse client
        RESOLU,         // Ticket résolu
        FERME           // Ticket fermé
    }
}
