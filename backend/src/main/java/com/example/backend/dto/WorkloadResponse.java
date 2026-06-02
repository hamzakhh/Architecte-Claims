package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadResponse {
    private String userId;
    private String fullName;
    private String role;
    private long dossiersActifs;
    private long dossiersTotal;
    private double chargePourcentage;
    private int chargeMax;
}
