package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String expediteurId; // ID de l'expéditeur

    private String contenu; // Contenu du message

    private boolean lu; // Message lu par le destinataire

    private LocalDateTime createdAt;
}
