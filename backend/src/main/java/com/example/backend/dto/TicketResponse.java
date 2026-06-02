package com.example.backend.dto;

import com.example.backend.model.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private String id;
    private String assureId;
    private String assureNom;
    private String claimId;
    private String claimReference;
    private String sujet;
    private String description;
    private String categorie;
    private Ticket.StatutTicket statut;
    private String assigneA;
    private String assigneNom;
    private List<TicketMessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TicketMessageResponse {
        private String expediteurId;
        private String expediteurNom;
        private String contenu;
        private LocalDateTime createdAt;
    }
}
