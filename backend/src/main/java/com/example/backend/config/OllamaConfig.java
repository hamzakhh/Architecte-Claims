package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour l'intégration Ollama (LLM local).
 * Propriétés définies dans application.properties avec le préfixe ollama.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaConfig {
    private String model = "llama3";                              // Modèle Ollama à utiliser
    private String apiUrl = "http://localhost:11434/api/chat";  // URL API Ollama
    private int maxTokens = 2000;        // Max tokens pour la réponse
    private double temperature = 0.3;    // Température (0-2, plus bas = plus déterministe)
    private boolean enabled = false;     // Activer/désactiver l'analyse IA

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
