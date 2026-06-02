package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre JWT exécuté une fois par requête.
 * Extrait le token JWT du header Authorization, le valide,
 * et place l'authentification dans le contexte Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Filtre chaque requête HTTP :
     * 1. Extrait le token du header "Authorization: Bearer &lt;token&gt;"
     * 2. Valide le token (signature + expiration)
     * 3. Extrait l'email et le rôle du token
     * 4. Place l'authentification dans le SecurityContext
     * 5. Passe au filtre suivant
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // Vérification de la présence du header Authorization avec le préfixe Bearer
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Extraction du token sans le préfixe "Bearer "

            // Validation du token et extraction des informations
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                // Création du token d'authentification Spring Security
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                // Placement de l'authentification dans le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Passe au filtre suivant dans la chaîne
        filterChain.doFilter(request, response);
    }
}
