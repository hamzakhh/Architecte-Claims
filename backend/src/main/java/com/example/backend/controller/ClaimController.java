package com.example.backend.controller;

import com.example.backend.dto.ClaimRequest;
import com.example.backend.dto.ClaimResponse;
import com.example.backend.dto.DashboardStatsResponse;
import com.example.backend.dto.IndemnisationRequest;
import com.example.backend.dto.QualificationRequest;
import com.example.backend.dto.StatutUpdateRequest;
import com.example.backend.model.Claim;
import com.example.backend.model.Expertise;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ExpertiseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrôleur REST gérant les endpoints de déclaration de sinistre.
 * Toutes les routes sont préfixées par /api/claims.
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimRepository claimRepository;
    private final ExpertiseRepository expertiseRepository;
    private final UserRepository userRepository;

    /**
     * Crée une nouvelle déclaration de sinistre.
     * Accessible par les assurés authentifiés.
     *
     * @param request les données du sinistre
     * @return le sinistre créé avec le statut 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request) {
        ClaimResponse response = claimService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère les sinistres de l'assuré connecté.
     *
     * @return la liste des sinistres de l'assuré
     */
    @GetMapping("/mes-sinistres")
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<List<ClaimResponse>> getMyClaims() {
        return ResponseEntity.ok(claimService.getMyClaims());
    }

    /**
     * Récupère un sinistre par son identifiant.
     *
     * @param id l'identifiant du sinistre
     * @return les informations du sinistre
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable String id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    /**
     * Récupère tous les sinistres (pour gestionnaires, experts, admins).
     *
     * @return la liste de tous les sinistres
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    /**
     * Met à jour le statut d'un sinistre.
     *
     * @param id     l'identifiant du sinistre
     * @param statut le nouveau statut
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> updateClaimStatus(
            @PathVariable String id,
            @RequestBody StatutUpdateRequest request) {
        return ResponseEntity.ok(claimService.updateClaimStatus(id, request.getStatut()));
    }

    /**
     * Permet à un gestionnaire de prendre en charge un sinistre.
     *
     * @param id l'identifiant du sinistre
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/prendre-en-charge")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<ClaimResponse> prendreEnCharge(@PathVariable String id) {
        return ResponseEntity.ok(claimService.prendreEnCharge(id));
    }

    /**
     * Assigne un gestionnaire à un sinistre.
     *
     * @param id             l'identifiant du sinistre
     * @param gestionnaireId l'identifiant du gestionnaire
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/assign-gestionnaire")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ClaimResponse> assignGestionnaire(
            @PathVariable String id,
            @RequestBody String gestionnaireId) {
        return ResponseEntity.ok(claimService.assignGestionnaire(id, gestionnaireId));
    }

    /**
     * Assigne un expert à un sinistre.
     *
     * @param id       l'identifiant du sinistre
     * @param expertId l'identifiant de l'expert
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/assign-expert")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> assignExpert(
            @PathVariable String id,
            @RequestBody String expertId) {
        return ResponseEntity.ok(claimService.assignExpert(id, expertId));
    }

    /**
     * Assigne automatiquement un expert spécialisé au sinistre.
     *
     * @param id l'identifiant du sinistre
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/auto-assign-expert")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> autoAssignExpert(@PathVariable String id) {
        return ResponseEntity.ok(claimService.autoAssignExpert(id));
    }

    /**
     * Récupère les sinistres assignés au gestionnaire connecté.
     *
     * @return la liste des sinistres assignés
     */
    @GetMapping("/dossiers-assignes")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<List<ClaimResponse>> getAssignedClaims() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        List<ClaimResponse> claims = claimRepository.findByGestionnaireId(gestionnaire.getId()).stream()
                .map(claimService::mapToResponsePublic)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(claims);
    }

    /**
     * Récupère les statistiques du tableau de bord gestionnaire.
     *
     * @return les statistiques du dashboard
     */
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String gestionnaireId = gestionnaire.getId();

        // Stats existantes
        long dossiersEnCours = claimRepository.findByGestionnaireIdAndStatut(gestionnaireId, Claim.StatutSinistre.EN_REVISION).size();
        long enAttenteApprobation = claimRepository.findByStatut(Claim.StatutSinistre.EXPERTISE).size();
        long expertisesEnCours = expertiseRepository.findByStatut(com.example.backend.model.Expertise.StatutExpertise.EN_COURS).size();

        List<User> experts = userRepository.findByRole(Role.EXPERT);
        long totalExperts = experts.size();
        long expertsDisponibles = experts.stream().filter(User::isEnabled).count();

        // Sinistres créés ce mois
        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long sinistresCeMois = claimRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(debutMois))
                .count();

        // Stats personnelles (2.2.5)
        List<Claim> mesDossiers = claimRepository.findByGestionnaireId(gestionnaireId);
        long dossiersOuverts = mesDossiers.stream()
                .filter(c -> c.getStatut() == Claim.StatutSinistre.EN_COURS || c.getStatut() == Claim.StatutSinistre.EN_REVISION)
                .count();
        long dossiersEnAttente = mesDossiers.stream()
                .filter(c -> c.getStatut() == Claim.StatutSinistre.EXPERTISE)
                .count();
        long dossiersUrgence = mesDossiers.stream()
                .filter(c -> c.getStatut() != Claim.StatutSinistre.CLOTURE && c.getStatut() != Claim.StatutSinistre.ARCHIVE
                        && c.getCreatedAt() != null && c.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7)))
                .count();

        // Dossiers sans gestionnaire (nouveaux à prendre en charge)
        long dossiersSansGestionnaire = claimRepository.findByStatut(Claim.StatutSinistre.EN_COURS).stream()
                .filter(c -> c.getGestionnaireId() == null)
                .count();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .dossiersEnCours(dossiersEnCours)
                .enAttenteApprobation(enAttenteApprobation)
                .expertisesEnCours(expertisesEnCours)
                .expertsDisponibles(expertsDisponibles)
                .totalExperts(totalExperts)
                .sinistresCeMois(sinistresCeMois)
                .dossiersOuverts(dossiersOuverts)
                .dossiersEnAttente(dossiersEnAttente)
                .dossiersUrgence(dossiersUrgence)
                .dossiersSansGestionnaire(dossiersSansGestionnaire)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les sinistres assignés à l'expert connecté.
     *
     * @return la liste des sinistres assignés à l'expert
     */
    @GetMapping("/mes-dossiers")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<List<ClaimResponse>> getExpertDossiers() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        List<ClaimResponse> claims = claimRepository.findByExpertId(expert.getId()).stream()
                .map(claimService::mapToResponsePublic)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(claims);
    }

    /**
     * Récupère les statistiques du tableau de bord expert.
     *
     * @return les statistiques du dashboard expert
     */
    @GetMapping("/expert-dashboard-stats")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<com.example.backend.dto.ExpertDashboardStatsResponse> getExpertDashboardStats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        long missionsEnCours = claimRepository.findByExpertIdAndStatut(expert.getId(), Claim.StatutSinistre.EXPERTISE).size();
        long rapportsARendre = expertiseRepository.findByExpertIdAndStatut(expert.getId(), Expertise.StatutExpertise.EN_COURS).size()
                + expertiseRepository.findByExpertIdAndStatut(expert.getId(), Expertise.StatutExpertise.EN_ATTENTE).size();
        long totalMissions = claimRepository.findByExpertId(expert.getId()).size();

        // Complétés ce mois
        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long completesCeMois = expertiseRepository.findByExpertId(expert.getId()).stream()
                .filter(e -> e.getDateRapport() != null && e.getDateRapport().isAfter(debutMois))
                .filter(e -> e.getStatut() == Expertise.StatutExpertise.SOUMISE || e.getStatut() == Expertise.StatutExpertise.VALIDEE)
                .count();

        com.example.backend.dto.ExpertDashboardStatsResponse stats = com.example.backend.dto.ExpertDashboardStatsResponse.builder()
                .missionsEnCours(missionsEnCours)
                .rapportsARendre(rapportsARendre)
                .completesCeMois(completesCeMois)
                .totalMissions(totalMissions)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère la liste des experts (pour le gestionnaire).
     *
     * @return la liste des utilisateurs avec le rôle EXPERT
     */
    @GetMapping("/experts")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<com.example.backend.dto.UserProfileResponse>> getExperts() {
        List<User> experts = userRepository.findByRole(Role.EXPERT);
        List<com.example.backend.dto.UserProfileResponse> responses = experts.stream()
                .map(user -> com.example.backend.dto.UserProfileResponse.builder()
                        .id(user.getId())
                        .prenom(user.getPrenom())
                        .nom(user.getNom())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .telephone(user.getTelephone())
                        .role(user.getRole() != null ? user.getRole().name() : null)
                        .specialite(user.getSpecialite())
                        .zoneIntervention(user.getZoneIntervention())
                        .notePerformance(user.getNotePerformance())
                        .chargeMax(user.getChargeMax())
                        .certifications(user.getCertifications())
                        .enabled(user.isEnabled())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupère la liste des gestionnaires (pour le transfert de dossiers).
     *
     * @return la liste des utilisateurs avec le rôle GESTIONNAIRE
     */
    @GetMapping("/gestionnaires")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<com.example.backend.dto.UserProfileResponse>> getGestionnaires() {
        List<User> gestionnaires = userRepository.findByRole(Role.GESTIONNAIRE);
        List<com.example.backend.dto.UserProfileResponse> responses = gestionnaires.stream()
                .map(user -> com.example.backend.dto.UserProfileResponse.builder()
                        .id(user.getId())
                        .prenom(user.getPrenom())
                        .nom(user.getNom())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .telephone(user.getTelephone())
                        .role(user.getRole() != null ? user.getRole().name() : null)
                        .specialite(user.getSpecialite())
                        .zoneIntervention(user.getZoneIntervention())
                        .notePerformance(user.getNotePerformance())
                        .chargeMax(user.getChargeMax())
                        .certifications(user.getCertifications())
                        .enabled(user.isEnabled())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Transfère un dossier à un autre gestionnaire.
     *
     * @param id               l'identifiant du sinistre
     * @param nouveauGestionnaireId l'identifiant du nouveau gestionnaire
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/transferer-gestionnaire")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<ClaimResponse> transfererGestionnaire(
            @PathVariable String id,
            @RequestBody String nouveauGestionnaireId) {
        return ResponseEntity.ok(claimService.transfererGestionnaire(id, nouveauGestionnaireId));
    }

    
    /**
     * Ajoute ou met à jour les notes internes d'un sinistre.
     *
     * @param id     l'identifiant du sinistre
     * @param notes  les notes internes
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/notes-internes")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> updateNotesInternes(
            @PathVariable String id,
            @RequestBody String notes) {
        return ResponseEntity.ok(claimService.updateNotesInternes(id, notes));
    }

    /**
     * Archive un sinistre.
     *
     * @param id l'identifiant du sinistre
     * @return le sinistre mis à jour
     */
    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> archiverSinistre(@PathVariable String id) {
        return ResponseEntity.ok(claimService.archiverSinistre(id));
    }

    // ==================== Qualification du sinistre (2.2.1) ====================

    /**
     * Qualifie un sinistre : gravité, couverture contractuelle, franchise, plafond.
     */
    @PatchMapping("/{id}/qualifier")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> qualifierSinistre(
            @PathVariable String id,
            @RequestBody QualificationRequest request) {
        return ResponseEntity.ok(claimService.qualifierSinistre(id, request));
    }

    // ==================== Indemnisation (2.2.4) ====================

    /**
     * Propose un montant d'indemnisation à l'assuré.
     */
    @PatchMapping("/{id}/proposer-indemnisation")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> proposerIndemnisation(
            @PathVariable String id,
            @RequestBody IndemnisationRequest request) {
        return ResponseEntity.ok(claimService.proposerIndemnisation(id, request));
    }

    /**
     * L'assuré accepte la proposition d'indemnisation.
     */
    @PatchMapping("/{id}/accepter-indemnisation")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> accepterIndemnisation(@PathVariable String id) {
        return ResponseEntity.ok(claimService.accepterIndemnisation(id));
    }

    /**
     * L'assuré refuse la proposition d'indemnisation (recours).
     */
    @PatchMapping("/{id}/refuser-indemnisation")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> refuserIndemnisation(
            @PathVariable String id,
            @RequestBody String motifRefus) {
        return ResponseEntity.ok(claimService.refuserIndemnisation(id, motifRefus));
    }

    /**
     * Initie le paiement de l'indemnisation.
     */
    @PatchMapping("/{id}/initier-paiement")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> initierPaiement(@PathVariable String id) {
        return ResponseEntity.ok(claimService.initierPaiement(id));
    }

    /**
     * Confirme le paiement de l'indemnisation et clôture le dossier.
     */
    @PatchMapping("/{id}/confirmer-paiement")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> confirmerPaiement(@PathVariable String id) {
        return ResponseEntity.ok(claimService.confirmerPaiement(id));
    }

    /**
     * Déclare un recours/litige sur le sinistre.
     */
    @PatchMapping("/{id}/declarer-recours")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ClaimResponse> declarerRecours(@PathVariable String id) {
        return ResponseEntity.ok(claimService.declarerRecours(id));
    }

    /**
     * Endpoint temporaire admin pour migrer les expertises manquantes.
     * Crée automatiquement les expertises pour les claims en statut EXPERTISE qui n'ont pas d'expertise associée.
     *
     * @return le nombre d'expertises créées
     */
    @PostMapping("/migrate-expertises")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> migrateExpertises() {
        List<Claim> expertiseClaims = claimRepository.findByStatut(Claim.StatutSinistre.EXPERTISE);

        int created = 0;
        for (Claim claim : expertiseClaims) {
            boolean exists = !expertiseRepository.findByClaimId(claim.getId()).isEmpty();
            if (!exists && claim.getExpertId() != null) {
                Expertise e = new Expertise();
                e.setClaimId(claim.getId());
                e.setExpertId(claim.getExpertId());
                e.setGestionnaireId(claim.getGestionnaireId());
                e.setStatut(Expertise.StatutExpertise.EN_ATTENTE);
                e.setCreatedAt(LocalDateTime.now());
                e.setUpdatedAt(LocalDateTime.now());
                expertiseRepository.save(e);
                created++;
            }
        }
        return ResponseEntity.ok(created + " expertise(s) créée(s)");
    }
}
