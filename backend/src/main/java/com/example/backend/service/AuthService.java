package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service d'authentification gérant l'inscription et la connexion.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Inscrit un nouvel utilisateur.
     * Vérifie l'unicité de l'email, crée le compte avec un mot de passe encodé,
     * puis authentifie l'utilisateur et génère un token JWT.
     *
     * @param request les données d'inscription (nom, email, mot de passe, etc.)
     * @return les informations d'authentification incluant le token JWT
     * @throws RuntimeException si un compte avec cet email existe déjà
     */
    public AuthResponse register(RegisterRequest request) {
        // Vérification que l'email n'est pas déjà utilisé
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }

        // Création et remplissage du nouvel utilisateur
        User user = new User();
        user.setPrenom(request.getPrenom());
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encodage du mot de passe
        user.setTelephone(request.getTelephone());
        user.setRole(request.getRole() != null ? request.getRole() : Role.ASSURE); // Rôle par défaut : ASSURE
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Authentification automatique après inscription
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Génération du token JWT
        String token = jwtUtil.generateToken(auth);

        // Construction de la réponse avec les infos utilisateur et le token
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .prenom(savedUser.getPrenom())
                .nom(savedUser.getNom())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .userId(savedUser.getId())
                .build();
    }

    /**
     * Authentifie un utilisateur existant.
     * Valide les identifiants via l'AuthenticationManager, puis génère un token JWT.
     *
     * @param request les identifiants de connexion (email + mot de passe)
     * @return les informations d'authentification incluant le token JWT
     * @throws RuntimeException si les identifiants sont invalides
     */
    public AuthResponse login(LoginRequest request) {
        // Authentification via Spring Security
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Récupération de l'utilisateur en base
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Génération du token JWT
        String token = jwtUtil.generateToken(auth);

        // Construction de la réponse
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .fullName(user.getFullName())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }
}
