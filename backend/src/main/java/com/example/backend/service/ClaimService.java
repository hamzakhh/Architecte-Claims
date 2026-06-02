package com.example.backend.service;

import com.example.backend.dto.AnalyseIARequest;
import com.example.backend.dto.AnalyseIAResponse;
import com.example.backend.dto.ClaimRequest;
import com.example.backend.dto.ClaimResponse;
import com.example.backend.dto.IndemnisationRequest;
import com.example.backend.dto.QualificationRequest;
import com.example.backend.model.AnalyseIA;
import com.example.backend.model.Claim;
import com.example.backend.model.Expertise;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.AnalyseIARepository;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ExpertiseRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service de gestion des sinistres.
 * Gère la création, la consultation et la mise à jour des déclarations de sinistre.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ExpertiseRepository expertiseRepository;
    private final AnalyseIARepository analyseIARepository;
    private final ClaimHistoryService claimHistoryService;
    private final NotificationService notificationService;
    private final AnalyseIAService analyseIAService;

    /**
     * Crée une nouvelle déclaration de sinistre.
     * L'assuré est identifié via le token JWT (SecurityContext).
     *
     * @param request les données du sinistre
     * @return la réponse avec les informations du sinistre créé
     */
    public ClaimResponse createClaim(ClaimRequest request) {
        log.info("Début création sinistre - Request: {}", request);
        
        String email = getCurrentUserEmail();
        log.info("Email utilisateur: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        log.info("Utilisateur trouvé: {} (ID: {})", user.getEmail(), user.getId());

        Claim claim = new Claim();
        claim.setAssureId(user.getId());
        claim.setType(request.getType());
        claim.setCategorie(classifyCategory(request.getType()));
        claim.setDescription(request.getDescription());
        claim.setDateSinistre(request.getDateSinistre());
        claim.setHeureSinistre(request.getHeureSinistre());
        claim.setLieu(request.getLieu());
        claim.setNotesLieu(request.getNotesLieu());
        claim.setEstimation(request.getEstimation());
        claim.setLatitude(request.getLatitude());
        claim.setLongitude(request.getLongitude());
        claim.setPiecesJointes(request.getPiecesJointes());
        claim.setStatut(Claim.StatutSinistre.EN_COURS);
        claim.setReference(generateReferenceNumber());
        claim.setCreatedAt(LocalDateTime.now());
        claim.setUpdatedAt(LocalDateTime.now());

        log.info("Sauvegarde du sinistre: {}", claim);
        Claim savedClaim = claimRepository.save(claim);
        log.info("Sinistre sauvegardé avec ID: {}, Réf: {}", savedClaim.getId(), savedClaim.getReference());

        claimHistoryService.recordAction(savedClaim.getId(), "CREATION", "Sinistre créé avec la référence " + savedClaim.getReference(), null, "EN_COURS");
        notificationService.envoyerNotification(user.getId(), "Sinistre déclaré", "Votre sinistre " + savedClaim.getReference() + " a été enregistré avec succès.", "STATUT_CHANGE", savedClaim.getId());

        // Déclencher automatiquement l'analyse IA
        try {
            AnalyseIARequest analyseRequest = new AnalyseIARequest();
            analyseRequest.setClaimId(savedClaim.getId());
            analyseRequest.setTypeAnalyse("INITIALE");
            AnalyseIAResponse analyseResponse = analyseIAService.analyserSinistre(analyseRequest);
            savedClaim.setAnalyseIAId(analyseResponse.getId());
            savedClaim = claimRepository.save(savedClaim);
            log.info("Analyse IA automatique déclenchée pour le sinistre {} - Sévérité: {}", savedClaim.getId(), analyseResponse.getSeverite());
        } catch (Exception e) {
            log.warn("Impossible de déclencher l'analyse IA pour le sinistre {}: {}", savedClaim.getId(), e.getMessage());
        }

        ClaimResponse response = mapToResponsePublic(savedClaim);
        log.info("Réponse créée: {}", response);
        return response;
    }

    /**
     * Récupère tous les sinistres de l'assuré connecté.
     *
     * @return la liste des sinistres de l'assuré
     */
    public List<ClaimResponse> getMyClaims() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return claimRepository.findByAssureId(user.getId()).stream()
                .map(this::mapToResponsePublic)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un sinistre par son identifiant.
     *
     * @param id l'identifiant du sinistre
     * @return la réponse avec les informations du sinistre
     */
    public ClaimResponse getClaimById(String id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));
        return mapToResponsePublic(claim);
    }

    /**
     * Récupère tous les sinistres (pour gestionnaires/admins).
     *
     * @return la liste de tous les sinistres
     */
    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(this::mapToResponsePublic)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour le statut d'un sinistre.
     *
     * @param id     l'identifiant du sinistre
     * @param statut le nouveau statut
     * @return la réponse avec les informations mises à jour
     */
    public ClaimResponse updateClaimStatus(String id, Claim.StatutSinistre statut) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        String ancienStatut = claim.getStatut().name();
        claim.setStatut(statut);
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);

        claimHistoryService.recordAction(id, "CHANGEMENT_STATUT", "Statut changé de " + ancienStatut + " à " + statut.name(), ancienStatut, statut.name());
        notificationService.envoyerNotification(claim.getAssureId(), "Statut mis à jour",
                "Le statut de votre sinistre " + claim.getReference() + " est maintenant : " + statut.name(), "STATUT_CHANGE", id);

        return mapToResponsePublic(updatedClaim);
    }

    /**
     * Permet à un gestionnaire de prendre en charge un sinistre.
     * Le gestionnaire est identifié via le token JWT (SecurityContext).
     *
     * @param claimId l'identifiant du sinistre
     * @return la réponse avec les informations mises à jour
     */
    public ClaimResponse prendreEnCharge(String claimId) {
        String email = getCurrentUserEmail();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        String ancienStatut = claim.getStatut().name();
        claim.setGestionnaireId(gestionnaire.getId());
        claim.setStatut(Claim.StatutSinistre.EN_REVISION);
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "PRISE_EN_CHARGE", "Pris en charge par " + gestionnaire.getFullName(), ancienStatut, "EN_REVISION");
        notificationService.envoyerNotification(claim.getAssureId(), "Sinistre pris en charge",
                "Votre sinistre " + claim.getReference() + " est maintenant pris en charge par " + gestionnaire.getFullName(), "STATUT_CHANGE", claimId);

        return mapToResponsePublic(updatedClaim);
    }

    /**
     * Assigne un gestionnaire à un sinistre.
     *
     * @param claimId        l'identifiant du sinistre
     * @param gestionnaireId l'identifiant du gestionnaire
     * @return la réponse avec les informations mises à jour
     */
    public ClaimResponse assignGestionnaire(String claimId, String gestionnaireId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        String ancienStatut = claim.getStatut().name();
        claim.setGestionnaireId(gestionnaireId);
        claim.setStatut(Claim.StatutSinistre.EN_REVISION);
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);

        String gestionnaireNom = userRepository.findById(gestionnaireId).map(User::getFullName).orElse(null);
        claimHistoryService.recordAction(claimId, "ASSIGNATION_GESTIONNAIRE", "Gestionnaire assigné : " + gestionnaireNom, ancienStatut, "EN_REVISION");
        notificationService.envoyerNotification(claim.getAssureId(), "Gestionnaire assigné",
                "Un gestionnaire a été assigné à votre sinistre " + claim.getReference(), "STATUT_CHANGE", claimId);

        return mapToResponsePublic(updatedClaim);
    }

    /**
     * Assigne un expert à un sinistre.
     *
     * @param claimId  l'identifiant du sinistre
     * @param expertId l'identifiant de l'expert
     * @return la réponse avec les informations mises à jour
     */
    public ClaimResponse assignExpert(String claimId, String expertId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("Expert non trouvé"));

        // Récupérer le gestionnaire connecté
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User gestionnaire = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));

        String ancienStatut = claim.getStatut().name();
        claim.setExpertId(expertId);
        claim.setStatut(Claim.StatutSinistre.EXPERTISE);
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "ASSIGNATION_EXPERT", "Expert assigné : " + expert.getFullName(), ancienStatut, "EXPERTISE");
        notificationService.envoyerNotification(claim.getAssureId(), "Expert assigné",
                "L'expert " + expert.getFullName() + " a été assigné à votre sinistre " + claim.getReference(), "EXPERTISE", claimId);
        notificationService.envoyerNotification(expertId, "Nouvelle expertise",
                "Vous avez été assigné à l'expertise du sinistre " + claim.getReference(), "EXPERTISE", claimId);

        // ✅ Créer l'expertise automatiquement si elle n'existe pas déjà
        boolean expertiseExiste = !expertiseRepository.findByClaimId(claimId).isEmpty();
        if (!expertiseExiste) {
            Expertise expertise = new Expertise();
            expertise.setClaimId(claimId);
            expertise.setExpertId(expertId);
            expertise.setGestionnaireId(gestionnaire.getId());
            expertise.setStatut(Expertise.StatutExpertise.EN_ATTENTE);
            expertise.setCreatedAt(LocalDateTime.now());
            expertise.setUpdatedAt(LocalDateTime.now());
            expertiseRepository.save(expertise);
            log.info("Expertise auto-créée pour le sinistre {} → expert {}", claimId, expertId);
        }

        return mapToResponsePublic(updatedClaim);
    }

    /**
     * Assigne automatiquement un expert spécialisé au sinistre en fonction de sa catégorie.
     * Si aucun expert spécialisé n'est trouvé, assigne un expert généraliste (spécialité "autre" ou sans spécialité).
     *
     * @param claimId l'identifiant du sinistre
     * @return la réponse avec les informations mises à jour
     */
    public ClaimResponse autoAssignExpert(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé"));

        String categorie = claim.getCategorie();
        if (categorie == null) categorie = "autre";

        // Chercher un expert avec la spécialité correspondante
        List<User> experts = userRepository.findByRole(Role.EXPERT);
        User bestExpert = null;

        // 1. Chercher un expert avec la spécialité exacte
        for (User expert : experts) {
            if (categorie.equals(expert.getSpecialite()) && expert.isEnabled()) {
                bestExpert = expert;
                break;
            }
        }

        // 2. Sinon, chercher un expert généraliste
        if (bestExpert == null) {
            for (User expert : experts) {
                if (("autre".equals(expert.getSpecialite()) || expert.getSpecialite() == null) && expert.isEnabled()) {
                    bestExpert = expert;
                    break;
                }
            }
        }

        // 3. Sinon, prendre le premier expert disponible
        if (bestExpert == null && !experts.isEmpty()) {
            bestExpert = experts.stream().filter(User::isEnabled).findFirst().orElse(null);
        }

        if (bestExpert == null) {
            throw new RuntimeException("Aucun expert disponible pour l'assignation automatique");
        }

        return assignExpert(claimId, bestExpert.getId());
    }

    /**
     * Génère un numéro de référence unique au format SIN-YYYY-XXXX.
     */
    private String generateReferenceNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = claimRepository.count() + 1;
        return String.format("SIN-%s-%04d", year, count);
    }

    /**
     * Classification automatique par catégorie basée sur le type de sinistre.
     */
    private String classifyCategory(String type) {
        if (type == null) return "autre";
        return switch (type) {
            case "auto" -> "accident";
            case "fire" -> "incendie";
            case "theft" -> "vol";
            case "water" -> "degat_eaux";
            case "natural" -> "catastrophe";
            default -> "autre";
        };
    }

    /**
     * Extrait l'email de l'utilisateur connecté depuis le SecurityContext.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return authentication.getName();
    }

    /**
     * Convertit une entité Claim en DTO ClaimResponse.
     */
    public ClaimResponse mapToResponsePublic(Claim claim) {
        String assureNom = null;
        String gestionnaireNom = null;
        String expertNom = null;

        if (claim.getAssureId() != null) {
            assureNom = userRepository.findById(claim.getAssureId())
                    .map(User::getFullName).orElse(null);
        }
        if (claim.getGestionnaireId() != null) {
            gestionnaireNom = userRepository.findById(claim.getGestionnaireId())
                    .map(User::getFullName).orElse(null);
        }
        if (claim.getExpertId() != null) {
            expertNom = userRepository.findById(claim.getExpertId())
                    .map(User::getFullName).orElse(null);
        }

        return ClaimResponse.builder()
                .id(claim.getId())
                .reference(claim.getReference())
                .assureId(claim.getAssureId())
                .assureNom(assureNom)
                .categorie(claim.getCategorie())
                .type(claim.getType())
                .latitude(claim.getLatitude())
                .longitude(claim.getLongitude())
                .description(claim.getDescription())
                .dateSinistre(claim.getDateSinistre())
                .heureSinistre(claim.getHeureSinistre())
                .lieu(claim.getLieu())
                .notesLieu(claim.getNotesLieu())
                .piecesJointes(claim.getPiecesJointes())
                .estimation(claim.getEstimation())
                .notesInternes(claim.getNotesInternes())
                .gravite(claim.getGravite())
                .couvertureContractuelle(claim.getCouvertureContractuelle())
                .franchise(claim.getFranchise())
                .plafondCouverture(claim.getPlafondCouverture())
                .montantIndemnisationPropose(claim.getMontantIndemnisationPropose())
                .montantIndemnisationFinal(claim.getMontantIndemnisationFinal())
                .motifIndemnisation(claim.getMotifIndemnisation())
                .indemnisationAcceptee(claim.getIndemnisationAcceptee())
                .motifRefusIndemnisation(claim.getMotifRefusIndemnisation())
                .recoursEnCours(claim.getRecoursEnCours())
                .datePaiement(claim.getDatePaiement())
                .statut(claim.getStatut())
                .gestionnaireId(claim.getGestionnaireId())
                .gestionnaireNom(gestionnaireNom)
                .expertId(claim.getExpertId())
                .expertNom(expertNom)
                .analyseIAId(claim.getAnalyseIAId())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }

    /**
     * Transfère un dossier à un autre gestionnaire.
     */
    public ClaimResponse transfererGestionnaire(String claimId, String nouveauGestionnaireId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        User nouveauGestionnaire = userRepository.findById(nouveauGestionnaireId)
                .orElseThrow(() -> new RuntimeException("Nouveau gestionnaire non trouvé: " + nouveauGestionnaireId));

        if (nouveauGestionnaire.getRole() != Role.GESTIONNAIRE && nouveauGestionnaire.getRole() != Role.ADMIN) {
            throw new RuntimeException("L'utilisateur n'est pas un gestionnaire");
        }

        String ancienGestionnaireId = claim.getGestionnaireId();
        claim.setGestionnaireId(nouveauGestionnaireId);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        // Historique du transfert
        claimHistoryService.recordAction(claimId, "TRANSFERT", 
                "Dossier transféré du gestionnaire " + ancienGestionnaireId + " vers " + nouveauGestionnaireId, null, null);

        log.info("Dossier {} transféré au gestionnaire {}", claimId, nouveauGestionnaire.getEmail());
        return mapToResponsePublic(claim);
    }

    
    /**
     * Met à jour les notes internes d'un sinistre.
     */
    public ClaimResponse updateNotesInternes(String claimId, String notes) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        claim.setNotesInternes(notes);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        log.info("Notes internes mises à jour pour le sinistre {}", claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * Archive un sinistre.
     */
    public ClaimResponse archiverSinistre(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.CLOTURE && claim.getStatut() != Claim.StatutSinistre.REFUSE) {
            throw new RuntimeException("Seuls les dossiers clôturés ou refusés peuvent être archivés");
        }

        claim.setStatut(Claim.StatutSinistre.ARCHIVE);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        // Historique d'archivage
        claimHistoryService.recordAction(claimId, "ARCHIVAGE", "Dossier archivé le " + LocalDateTime.now(), null, null);

        log.info("Sinistre {} archivé", claimId);
        return mapToResponsePublic(claim);
    }

    // ==================== Qualification du sinistre (2.2.1) ====================

    /**
     * Qualifie un sinistre : gravité, couverture contractuelle, franchise, plafond.
     */
    public ClaimResponse qualifierSinistre(String claimId, QualificationRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        claim.setGravite(request.getGravite());
        claim.setCouvertureContractuelle(request.getCouvertureContractuelle());
        claim.setFranchise(request.getFranchise());
        claim.setPlafondCouverture(request.getPlafondCouverture());
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "QUALIFICATION",
                "Sinistre qualifié : gravité=" + request.getGravite() + ", couverture=" + request.getCouvertureContractuelle(),
                null, null);

        log.info("Sinistre {} qualifié : gravité={}", claimId, request.getGravite());
        return mapToResponsePublic(claim);
    }

    // ==================== Indemnisation (2.2.4) ====================

    /**
     * Propose un montant d'indemnisation à l'assuré.
     */
    public ClaimResponse proposerIndemnisation(String claimId, IndemnisationRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.VALIDE && claim.getStatut() != Claim.StatutSinistre.EXPERTISE && claim.getStatut() != Claim.StatutSinistre.EN_REVISION) {
            throw new RuntimeException("Le sinistre doit être en révision, en expertise ou validé pour proposer une indemnisation");
        }

        // Calcul automatique : appliquer la franchise si définie
        double montantPropose = request.getMontantPropose();
        if (claim.getFranchise() != null && claim.getFranchise() > 0) {
            montantPropose = Math.max(0, montantPropose - claim.getFranchise());
        }
        // Appliquer le plafond si défini
        if (claim.getPlafondCouverture() != null && claim.getPlafondCouverture() > 0 && montantPropose > claim.getPlafondCouverture()) {
            montantPropose = claim.getPlafondCouverture();
        }

        claim.setMontantIndemnisationPropose(montantPropose);
        claim.setMotifIndemnisation(request.getMotifIndemnisation());
        claim.setStatut(Claim.StatutSinistre.INDEMNISATION_PROPOSEE);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "PROPOSITION_INDEMNISATION",
                "Proposition d'indemnisation : " + montantPropose + " DT" + (request.getMotifIndemnisation() != null ? " — " + request.getMotifIndemnisation() : ""),
                null, "INDEMNISATION_PROPOSEE");

        notificationService.envoyerNotification(claim.getAssureId(), "Proposition d'indemnisation",
                "Une proposition d'indemnisation de " + montantPropose + " DT a été faite pour votre sinistre " + claim.getReference(),
                "INDEMNISATION", claimId);

        log.info("Proposition d'indemnisation {} DT pour le sinistre {}", montantPropose, claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * L'assuré accepte la proposition d'indemnisation.
     */
    public ClaimResponse accepterIndemnisation(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.INDEMNISATION_PROPOSEE) {
            throw new RuntimeException("Aucune proposition d'indemnisation en attente pour ce sinistre");
        }

        claim.setIndemnisationAcceptee(true);
        claim.setMontantIndemnisationFinal(claim.getMontantIndemnisationPropose());
        claim.setStatut(Claim.StatutSinistre.INDEMNISATION_ACCEPTEE);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "ACCEPTATION_INDEMNISATION",
                "L'assuré a accepté l'indemnisation de " + claim.getMontantIndemnisationFinal() + " DT",
                "INDEMNISATION_PROPOSEE", "INDEMNISATION_ACCEPTEE");

        log.info("Indemnisation acceptée pour le sinistre {}", claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * L'assuré refuse la proposition d'indemnisation (recours).
     */
    public ClaimResponse refuserIndemnisation(String claimId, String motifRefus) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.INDEMNISATION_PROPOSEE) {
            throw new RuntimeException("Aucune proposition d'indemnisation en attente pour ce sinistre");
        }

        claim.setIndemnisationAcceptee(false);
        claim.setMotifRefusIndemnisation(motifRefus);
        claim.setRecoursEnCours(true);
        claim.setStatut(Claim.StatutSinistre.RECOURS);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "REFUS_INDEMNISATION",
                "L'assuré a refusé l'indemnisation — Motif : " + motifRefus,
                "INDEMNISATION_PROPOSEE", "RECOURS");

        log.info("Indemnisation refusée pour le sinistre {} — Recours en cours", claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * Initie le paiement de l'indemnisation.
     */
    public ClaimResponse initierPaiement(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.INDEMNISATION_ACCEPTEE) {
            throw new RuntimeException("L'indemnisation doit être acceptée avant d'initier le paiement");
        }

        claim.setStatut(Claim.StatutSinistre.PAIEMENT_EN_COURS);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "PAIEMENT_INITIE",
                "Paiement de " + claim.getMontantIndemnisationFinal() + " DT initié",
                "INDEMNISATION_ACCEPTEE", "PAIEMENT_EN_COURS");

        log.info("Paiement initié pour le sinistre {}", claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * Confirme le paiement et clôture le dossier.
     */
    public ClaimResponse confirmerPaiement(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        if (claim.getStatut() != Claim.StatutSinistre.PAIEMENT_EN_COURS) {
            throw new RuntimeException("Le paiement doit être en cours pour être confirmé");
        }

        claim.setDatePaiement(LocalDateTime.now());
        claim.setStatut(Claim.StatutSinistre.CLOTURE);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "PAIEMENT_CONFIRME_CLOTURE",
                "Paiement confirmé — Dossier clôturé. Montant : " + claim.getMontantIndemnisationFinal() + " DT",
                "PAIEMENT_EN_COURS", "CLOTURE");

        notificationService.envoyerNotification(claim.getAssureId(), "Indemnisation versée",
                "Le paiement de " + claim.getMontantIndemnisationFinal() + " DT a été effectué pour votre sinistre " + claim.getReference() + ". Dossier clôturé.",
                "INDEMNISATION", claimId);

        log.info("Paiement confirmé et dossier clôturé pour le sinistre {}", claimId);
        return mapToResponsePublic(claim);
    }

    /**
     * Déclare un recours/litige sur le sinistre.
     */
    public ClaimResponse declarerRecours(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));

        claim.setRecoursEnCours(true);
        claim.setStatut(Claim.StatutSinistre.RECOURS);
        claim.setUpdatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        claimHistoryService.recordAction(claimId, "RECOURS_DECLARE",
                "Recours/litige déclaré sur le sinistre",
                null, "RECOURS");

        log.info("Recours déclaré pour le sinistre {}", claimId);
        return mapToResponsePublic(claim);
    }
}
