package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service personnalisé de chargement des utilisateurs pour Spring Security.
 * Implémente UserDetailsService pour résoudre un utilisateur à partir de son email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son email (utilisé comme username).
     * Vérifie également que le compte est actif.
     *
     * @param email l'adresse email de l'utilisateur
     * @return l'entité User (qui implémente UserDetails)
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas ou est désactivé
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Recherche de l'utilisateur par email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));

        // Vérification que le compte est actif
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Compte désactivé: " + email);
        }

        return user;
    }
}
