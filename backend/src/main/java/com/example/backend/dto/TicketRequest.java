package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketRequest {
    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 3, max = 200, message = "Le sujet doit contenir entre 3 et 200 caractères")
    private String sujet;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
    private String description;

    private String categorie; // technique, facturation, sinistre, general

    private String claimId; // Optionnel : sinistre lié
}
