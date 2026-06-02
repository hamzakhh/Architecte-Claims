package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed
    private String participant1Id; // Gestionnaire

    @Indexed
    private String participant2Id; // Assuré

    private String claimId; // Optionnel: lié à un sinistre

    private String dernierMessage; // Aperçu du dernier message

    private LocalDateTime dernierMessageDate; // Date du dernier message

    private int messagesNonLusParticipant1; // Non lus pour participant1 (gestionnaire)

    private int messagesNonLusParticipant2; // Non lus pour participant2 (assuré)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
