package com.example.backend.service;

import com.example.backend.dto.ExpertiseRequest;
import com.example.backend.dto.ExpertiseResponse;
import com.example.backend.model.Claim;
import com.example.backend.model.Expertise;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ExpertiseRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des expertises.
 * Gère la création, la consultation et la mise à jour des rapports d'expertise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertiseService {

    private final ExpertiseRepository expertiseRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    /**
     * Crée une demande d'expertise (par un gestionnaire).
     */
    public ExpertiseResponse createExpertise(ExpertiseRequest request) {
        String email = getCurrentUserEmail();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        Expertise expertise = new Expertise();
        expertise.setClaimId(request.getClaimId());
        expertise.setGestionnaireId(gestionnaire.getId());
        expertise.setExpertId(claim.getExpertId());
        expertise.setPiecesJointes(request.getPiecesJointes() != null ? request.getPiecesJointes() : new java.util.ArrayList<>());
        expertise.setStatut(Expertise.StatutExpertise.EN_ATTENTE);
        expertise.setCreatedAt(LocalDateTime.now());
        expertise.setUpdatedAt(LocalDateTime.now());

        Expertise saved = expertiseRepository.save(expertise);
        log.info("Expertise créée pour le sinistre {} par le gestionnaire {}", claim.getId(), gestionnaire.getId());
        return mapToResponse(saved);
    }

    /**
     * Récupère les expertises d'un sinistre.
     */
    public List<ExpertiseResponse> getExpertisesByClaim(String claimId) {
        return expertiseRepository.findByClaimId(claimId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une expertise par son identifiant.
     */
    public ExpertiseResponse getExpertiseById(String id) {
        Expertise expertise = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expertise non trouvée"));
        return mapToResponse(expertise);
    }

    /**
     * Récupère les expertises assignées à l'expert connecté.
     */
    public List<ExpertiseResponse> getMyExpertises() {
        String email = getCurrentUserEmail();
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return expertiseRepository.findByExpertId(expert.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les expertises demandées par le gestionnaire connecté.
     */
    public List<ExpertiseResponse> getExpertisesByGestionnaire() {
        String email = getCurrentUserEmail();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return expertiseRepository.findByGestionnaireId(gestionnaire.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les expertises (admin).
     */
    public List<ExpertiseResponse> getAllExpertises() {
        return expertiseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Soumet un rapport d'expertise (par l'expert).
     */
    public ExpertiseResponse submitRapport(String expertiseId, ExpertiseRequest request) {
        Expertise expertise = expertiseRepository.findById(expertiseId)
                .orElseThrow(() -> new RuntimeException("Expertise non trouvée"));

        expertise.setConclusion(request.getConclusion());
        expertise.setMontantEstime(request.getMontantEstime());
        expertise.setRecommandation(request.getRecommandation());
        expertise.setPiecesJointes(request.getPiecesJointes());
        expertise.setStatut(Expertise.StatutExpertise.SOUMISE);
        expertise.setDateRapport(LocalDateTime.now());
        expertise.setUpdatedAt(LocalDateTime.now());

        Expertise saved = expertiseRepository.save(expertise);
        log.info("Rapport d'expertise soumis pour l'expertise {}", expertiseId);
        return mapToResponse(saved);
    }

    /**
     * Enregistre un brouillon de rapport d'expertise (par l'expert).
     * Le statut passe à EN_COURS sans soumission.
     */
    public ExpertiseResponse saveBrouillon(String expertiseId, ExpertiseRequest request) {
        Expertise expertise = expertiseRepository.findById(expertiseId)
                .orElseThrow(() -> new RuntimeException("Expertise non trouvée"));

        expertise.setConclusion(request.getConclusion());
        expertise.setMontantEstime(request.getMontantEstime());
        expertise.setRecommandation(request.getRecommandation());
        expertise.setPiecesJointes(request.getPiecesJointes());
        expertise.setStatut(Expertise.StatutExpertise.EN_COURS);
        expertise.setUpdatedAt(LocalDateTime.now());

        Expertise saved = expertiseRepository.save(expertise);
        log.info("Brouillon d'expertise enregistré pour l'expertise {}", expertiseId);
        return mapToResponse(saved);
    }

    /**
     * Valide une expertise (par le gestionnaire).
     */
    public ExpertiseResponse validerExpertise(String expertiseId) {
        Expertise expertise = expertiseRepository.findById(expertiseId)
                .orElseThrow(() -> new RuntimeException("Expertise non trouvée"));

        expertise.setStatut(Expertise.StatutExpertise.VALIDEE);
        expertise.setUpdatedAt(LocalDateTime.now());
        Expertise saved = expertiseRepository.save(expertise);

        // Mettre à jour le statut du sinistre
        Claim claim = claimRepository.findById(expertise.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));
        claim.setStatut(Claim.StatutSinistre.VALIDE);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        log.info("Expertise validée {} - sinistre {} passé à VALIDE", expertiseId, claim.getId());
        return mapToResponse(saved);
    }

    /**
     * Refuse une expertise avec justification (par le gestionnaire).
     */
    public ExpertiseResponse refuserExpertise(String expertiseId, String justification) {
        Expertise expertise = expertiseRepository.findById(expertiseId)
                .orElseThrow(() -> new RuntimeException("Expertise non trouvée"));

        expertise.setStatut(Expertise.StatutExpertise.REFUSEE);
        expertise.setUpdatedAt(LocalDateTime.now());
        
        // Ajouter la justification dans les commentaires si le champ existe
        if (expertise.getCommentaires() != null) {
            expertise.setCommentaires(expertise.getCommentaires() + "\n\n[REFUS GESTIONNAIRE - " + 
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + 
                    "]\nJustification: " + justification);
        } else {
            expertise.setCommentaires("[REFUS GESTIONNAIRE - " + 
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + 
                    "]\nJustification: " + justification);
        }
        
        Expertise saved = expertiseRepository.save(expertise);

        // Mettre à jour le statut du claim pour retour en révision
        Claim claim = claimRepository.findById(expertise.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));
        claim.setStatut(Claim.StatutSinistre.EN_REVISION);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        log.info("Expertise {} refusée avec justification: {}", expertiseId, justification);
        return mapToResponse(saved);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return authentication.getName();
    }

    private ExpertiseResponse mapToResponse(Expertise expertise) {
        String expertNom = null;
        String gestionnaireNom = null;

        if (expertise.getExpertId() != null) {
            expertNom = userRepository.findById(expertise.getExpertId())
                    .map(User::getFullName).orElse(null);
        }
        if (expertise.getGestionnaireId() != null) {
            gestionnaireNom = userRepository.findById(expertise.getGestionnaireId())
                    .map(User::getFullName).orElse(null);
        }

        return ExpertiseResponse.builder()
                .id(expertise.getId())
                .claimId(expertise.getClaimId())
                .expertId(expertise.getExpertId())
                .gestionnaireId(expertise.getGestionnaireId())
                .conclusion(expertise.getConclusion())
                .montantEstime(expertise.getMontantEstime())
                .recommandation(expertise.getRecommandation())
                .piecesJointes(expertise.getPiecesJointes() != null ? expertise.getPiecesJointes() : new java.util.ArrayList<>())
                .statut(expertise.getStatut())
                .dateRapport(expertise.getDateRapport())
                .createdAt(expertise.getCreatedAt())
                .updatedAt(expertise.getUpdatedAt())
                .expertNom(expertNom)
                .gestionnaireNom(gestionnaireNom)
                .claimReference("#SIN-" + expertise.getClaimId().substring(0, Math.min(8, expertise.getClaimId().length())))
                .build();
    }
}
