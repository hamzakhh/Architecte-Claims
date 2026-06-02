package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Utilitaire de gestion des tokens JWT.
 * Fournit les opérations de création, validation et extraction des claims.
 */
@Component
public class JwtUtil {

    private final SecretKey key;       // Clé HMAC utilisée pour signer les tokens
    private final long expiration;     // Durée de validité du token en millisecondes

    /**
     * Constructeur injectant la clé secrète et la durée d'expiration
     * depuis les propriétés application.properties (jwt.secret, jwt.expiration).
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expiration = expiration;
    }

    /**
     * Génère un token JWT à partir de l'authentification Spring Security.
     * Le token contient le subject (email), le rôle et les dates d'émission/expiration.
     *
     * @param authentication l'objet Authentication de Spring Security
     * @return le token JWT signé
     */
    public String generateToken(Authentication authentication) {
        // Extraction du premier rôle (ex: ROLE_ASSURE)
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_ASSURE");

        return Jwts.builder()
                .subject(authentication.getName()) // Email de l'utilisateur
                .claim("role", role)               // Rôle personnalisé dans les claims
                .issuedAt(new Date())               // Date de création
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Date d'expiration
                .signWith(key)                      // Signature HMAC
                .compact();
    }

    /**
     * Extrait l'email (subject) du token JWT.
     *
     * @param token le token JWT
     * @return l'email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrait le rôle du token JWT.
     *
     * @param token le token JWT
     * @return le rôle (ex: ROLE_ASSURE)
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Valide un token JWT (signature + expiration).
     *
     * @param token le token JWT à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parse et vérifie un token JWT, puis retourne ses claims.
     *
     * @param token le token JWT
     * @return les claims du token
     * @throws JwtException si le token est invalide ou expiré
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
