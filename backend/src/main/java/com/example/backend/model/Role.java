package com.example.backend.model;

/**
 * Énumération des rôles utilisateurs dans le système de gestion des sinistres.
 */
public enum Role {
    ASSURE,          // Assuré — client souscrivant à une police d'assurance
    GESTIONNAIRE,    // Gestionnaire — traitant les sinistres en interne
    EXPERT,          // Expert — évaluant les sinistres sur le terrain
    ADMIN            // Administrateur — gestion complète du système
}
