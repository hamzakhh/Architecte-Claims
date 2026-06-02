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
public class UserProfileResponse {
    private String id;
    private String prenom;
    private String nom;
    private String fullName;
    private String email;
    private String telephone;
    private String role;
    private String specialite;
    private String zoneIntervention;
    private Double notePerformance;
    private Integer chargeMax;
    private java.util.List<String> certifications;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
