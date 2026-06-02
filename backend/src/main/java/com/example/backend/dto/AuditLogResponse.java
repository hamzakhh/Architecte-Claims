package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private String id;
    private String action;
    private String utilisateur;
    private String role;
    private String cible;
    private String details;
    private String timestamp;
}
