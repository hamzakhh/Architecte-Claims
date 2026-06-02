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
public class MessageResponse {

    private String id;
    private String conversationId;
    private String expediteurId;
    private String expediteurNom;
    private String expediteurRole;
    private String contenu;
    private boolean lu;
    private LocalDateTime createdAt;
}
