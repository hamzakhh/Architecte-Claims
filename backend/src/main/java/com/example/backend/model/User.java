package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entité utilisateur stockée dans MongoDB (collection "users").
 * Implémente UserDetails pour l'intégration avec Spring Security.
 */
@Data
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id; // Identifiant MongoDB auto-généré

    private String prenom; // Prénom de l'utilisateur
    private String nom; // Nom de famille

    @Indexed(unique = true)
    private String email; // Email utilisé comme identifiant de connexion (unique)

    private String password; // Mot de passe encodé (BCrypt)

    private String telephone; // Numéro de téléphone

    private Role role; // Rôle de l'utilisateur (ASSURE, GESTIONNAIRE, EXPERT, ADMIN)

    private String specialite; // Spécialité de l'expert (accident, incendie, vol, degat_eaux, catastrophe, autre)

    // Champs spécifiques à la gestion des experts
    private String zoneIntervention; // Zone géographique d'intervention de l'expert
    private Double notePerformance; // Note moyenne de performance (0-5)
    private Integer chargeMax; // Charge maximale de dossiers simultanés recommandée
    private java.util.List<String> certifications; // Liste des certifications de l'expert

    private boolean enabled = true; // Compte actif/désactivé

    private LocalDateTime createdAt; // Date de création du compte
    private LocalDateTime updatedAt; // Date de dernière mise à jour


    /**
     * Retourne les autorités Spring Security dérivées du rôle.
     * Le préfixe "ROLE_" est ajouté automatiquement (ex: ROLE_ASSURE).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * L'email est utilisé comme nom d'utilisateur pour l'authentification.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Retourne le nom complet (prénom + nom) de l'utilisateur.
     */
    public String getFullName() {
        return prenom + " " + nom;
    }
}
