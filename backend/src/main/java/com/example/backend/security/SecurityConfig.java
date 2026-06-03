package com.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de sécurité Spring Security.
 * Définit la chaîne de filtres, les règles d'autorisation,
 * la politique CORS, l'encodeur de mot de passe et l'AuthenticationManager.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configure la chaîne de filtres de sécurité :
     * - CSRF désactivé (API stateless)
     * - CORS configuré pour le frontend Angular (localhost:4200)
     * - Sessions stateless (JWT)
     * - Endpoints /api/auth/** publics, le reste nécessite une authentification
     * - Filtre JWT ajouté avant le filtre UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Désactivation CSRF (API REST stateless)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Configuration CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Pas de session HTTP
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Endpoints d'authentification publics
                        .requestMatchers("/api/reimbursements/stripe/webhook").permitAll() // Webhook Stripe (appelé par Stripe sans auth)
                        .requestMatchers("/api/files/**").authenticated() // Endpoints de fichiers nécessitent une authentification
                        .requestMatchers("/error").permitAll() // Endpoint d'erreur Spring Boot
                        .requestMatchers("/actuator/**").permitAll() // Endpoints actuator (pour monitoring)
                        .anyRequest().authenticated() // Toutes les autres requêtes nécessitent une authentification
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Ajout du filtre JWT

        return http.build();
    }

    /**
     * Configuration CORS autorisant les requêtes depuis le frontend Angular.
     * Autorise toutes les méthodes HTTP et tous les headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
       // config.setAllowedOrigins(List.of("http://localhost:4200")); // Origine du frontend Angular
       config.setAllowedOriginPatterns(List.of("*"));
        //config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost", "http://127.0.0.1:37661", "http://192.168.49.2:30080")); // Frontend Angular (dev + Docker)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*")); // Tous les headers autorisés
        config.setAllowCredentials(true); // Autorise les cookies/credentials

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Fournit l'encodeur de mot de passe BCrypt pour le hachage sécurisé.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose l'AuthenticationManager de Spring Security pour l'authentification programmatique.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
