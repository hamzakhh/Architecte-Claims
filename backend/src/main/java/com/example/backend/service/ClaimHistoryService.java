package com.example.backend.service;

import com.example.backend.dto.ClaimHistoryResponse;
import com.example.backend.model.ClaimHistory;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimHistoryRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimHistoryService {

    private final ClaimHistoryRepository claimHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Enregistre une action dans l'historique d'un sinistre.
     */
    public void recordAction(String claimId, String action, String description,
                              String ancienStatut, String nouveauStatut) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);

        ClaimHistory history = new ClaimHistory();
        history.setClaimId(claimId);
        history.setAction(action);
        history.setDescription(description);
        history.setAncienStatut(ancienStatut);
        history.setNouveauStatut(nouveauStatut);
        history.setUtilisateurId(user != null ? user.getId() : null);
        history.setUtilisateurNom(user != null ? user.getFullName() : "Système");
        history.setUtilisateurRole(user != null ? user.getRole().name() : "SYSTEME");
        history.setCreatedAt(LocalDateTime.now());

        claimHistoryRepository.save(history);
        log.info("Historique enregistré: {} - sinistre {}", action, claimId);
    }

    /**
     * Enregistre une action système (sans utilisateur connecté).
     */
    public void recordSystemAction(String claimId, String action, String description,
                                    String ancienStatut, String nouveauStatut) {
        ClaimHistory history = new ClaimHistory();
        history.setClaimId(claimId);
        history.setAction(action);
        history.setDescription(description);
        history.setAncienStatut(ancienStatut);
        history.setNouveauStatut(nouveauStatut);
        history.setUtilisateurNom("Système");
        history.setUtilisateurRole("SYSTEME");
        history.setCreatedAt(LocalDateTime.now());

        claimHistoryRepository.save(history);
    }

    /**
     * Récupère l'historique complet d'un sinistre.
     */
    public List<ClaimHistoryResponse> getClaimHistory(String claimId) {
        return claimHistoryRepository.findByClaimIdOrderByCreatedAtAsc(claimId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ClaimHistoryResponse mapToResponse(ClaimHistory history) {
        return ClaimHistoryResponse.builder()
                .id(history.getId())
                .claimId(history.getClaimId())
                .action(history.getAction())
                .description(history.getDescription())
                .utilisateurId(history.getUtilisateurId())
                .utilisateurNom(history.getUtilisateurNom())
                .utilisateurRole(history.getUtilisateurRole())
                .ancienStatut(history.getAncienStatut())
                .nouveauStatut(history.getNouveauStatut())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }
}
