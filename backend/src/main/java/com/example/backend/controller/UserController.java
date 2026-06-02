package com.example.backend.controller;

import com.example.backend.dto.AdminDashboardStatsResponse;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.dto.UserProfileResponse;
import com.example.backend.model.Role;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<UserProfileResponse> toggleUserStatus(@PathVariable String id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Récupère tous les utilisateurs (admin uniquement).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Récupère les utilisateurs par rôle (admin uniquement).
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getUsersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    /**
     * Crée un nouvel utilisateur (admin uniquement).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met à jour le rôle d'un utilisateur (admin uniquement).
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateUserRole(@PathVariable String id, @RequestBody Role newRole) {
        return ResponseEntity.ok(userService.updateUserRole(id, newRole));
    }

    /**
     * Supprime un utilisateur (admin uniquement).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    }

    /**
     * Recherche des utilisateurs par terme (admin uniquement).
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(@RequestParam String term) {
        return ResponseEntity.ok(userService.searchUsers(term));
    }

    /**
     * Récupère les statistiques du tableau de bord admin.
     */
    @GetMapping("/admin-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardStatsResponse> getAdminStats() {
        return ResponseEntity.ok(userService.getAdminStats());
    }
}
