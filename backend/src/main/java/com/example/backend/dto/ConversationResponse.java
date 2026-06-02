package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponse {

    private String id;
    private String participant1Id;
    private String participant1Nom;
    private String participant1Role;
    private String participant2Id;
    private String participant2Nom;
    private String participant2Role;
    private String claimId;
    private String dernierMessage;
    private LocalDateTime dernierMessageDate;
    private int messagesNonLus;
    private LocalDateTime createdAt;
}
