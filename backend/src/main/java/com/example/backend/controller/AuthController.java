package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Contrôleur REST gérant les endpoints d'authentification.
 * Toutes les routes sont préfixées par /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Connecte un utilisateur existant.
     *
     * @param request identifiants de connexion (email + mot de passe)
     * @return token JWT et informations utilisateur
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Inscrit un nouvel utilisateur.
     *
     * @param request données d'inscription
     * @return token JWT et informations utilisateur
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

}
