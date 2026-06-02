package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entité Notification stockée dans MongoDB (collection "notifications").
 * Représente une notification in-app envoyée à un utilisateur.
 */
@Data
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String utilisateurId; // Identifiant de l'utilisateur destinataire

    private String titre; // Titre de la notification

    private String message; // Contenu de la notification

    private String type; // Type : STATUT_CHANGE, EXPERTISE, REMBOURSEMENT, MESSAGE, SYSTEME

    private String claimId; // Identifiant du sinistre lié (optionnel)

    private boolean lu; // Notification lue par l'utilisateur

    private LocalDateTime createdAt;
}
