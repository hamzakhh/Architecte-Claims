package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {

    @NotBlank(message = "Le contenu du message est obligatoire")
    private String contenu;

    private String conversationId; // Optionnel: si null, crée une nouvelle conversation

    private String destinataireId; // Requis si conversationId est null
}
