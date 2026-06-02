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
public class NotificationResponse {
    private String id;
    private String utilisateurId;
    private String titre;
    private String message;
    private String type;
    private String claimId;
    private boolean lu;
    private LocalDateTime createdAt;
}
