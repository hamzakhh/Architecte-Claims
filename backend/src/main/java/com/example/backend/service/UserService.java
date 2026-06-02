package com.example.backend.service;

import com.example.backend.dto.AdminDashboardStatsResponse;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.dto.UserProfileResponse;
import com.example.backend.model.Claim;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.ClaimRepository;
import com.example.backend.repository.ExpertiseRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final ExpertiseRepository expertiseRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getMyProfile() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return mapToProfileResponse(user);
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        user.setPrenom(request.getPrenom());
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setTelephone(request.getTelephone());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Profil mis à jour pour l'utilisateur: {}", savedUser.getEmail());
        return mapToProfileResponse(savedUser);
    }

    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Mot de passe modifié pour l'utilisateur: {}", user.getEmail());
    }

    public UserProfileResponse toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setEnabled(!user.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("Statut de l'utilisateur {} basculé vers {}", savedUser.getEmail(), savedUser.isEnabled() ? "actif" : "inactif");
        return mapToProfileResponse(savedUser);
    }

    /**
     * Récupère tous les utilisateurs (pour l'admin).
     */
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les utilisateurs par rôle (pour l'admin).
     */
    public List<UserProfileResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crée un nouvel utilisateur (par l'admin).
     */
    public UserProfileResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = new User();
        user.setPrenom(request.getPrenom());
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelephone(request.getTelephone());
        user.setRole(request.getRole() != null ? request.getRole() : Role.ASSURE);
        user.setSpecialite(request.getSpecialite());
        user.setZoneIntervention(request.getZoneIntervention());
        user.setNotePerformance(request.getNotePerformance());
        user.setChargeMax(request.getChargeMax());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Nouvel utilisateur créé par l'admin: {} (rôle: {})", savedUser.getEmail(), savedUser.getRole());
        return mapToProfileResponse(savedUser);
    }

    /**
     * Met à jour le rôle d'un utilisateur (par l'admin).
     */
    public UserProfileResponse updateUserRole(String userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("Rôle de l'utilisateur {} mis à jour vers {}", savedUser.getEmail(), newRole);
        return mapToProfileResponse(savedUser);
    }

    /**
     * Supprime un utilisateur (par l'admin).
     */
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        userRepository.delete(user);
        log.info("Utilisateur supprimé: {}", user.getEmail());
    }

    /**
     * Recherche des utilisateurs par terme (prénom, nom, email).
     */
    public List<UserProfileResponse> searchUsers(String term) {
        if (term == null || term.isBlank()) {
            return getAllUsers();
        }
        return userRepository.findByPrenomContainingIgnoreCaseOrNomContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, term)
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les statistiques du tableau de bord admin.
     */
    public AdminDashboardStatsResponse getAdminStats() {
        long totalUtilisateurs = userRepository.count();
        long utilisateursActifs = userRepository.countByEnabled(true);
        long totalAssures = userRepository.countByRole(Role.ASSURE);
        long totalExperts = userRepository.countByRole(Role.EXPERT);
        long totalGestionnaires = userRepository.countByRole(Role.GESTIONNAIRE);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        long totalSinistres = claimRepository.count();
        long sinistresEnCours = claimRepository.findByStatut(Claim.StatutSinistre.EN_COURS).size();
        long sinistresClotures = claimRepository.findByStatut(Claim.StatutSinistre.CLOTURE).size();

        LocalDateTime debutMois = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long sinistresCeMois = claimRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(debutMois))
                .count();

        long expertisesEnCours = expertiseRepository.findByStatut(com.example.backend.model.Expertise.StatutExpertise.EN_COURS).size();

        long expertsDisponibles = userRepository.findByRole(Role.EXPERT).stream()
                .filter(User::isEnabled)
                .count();

        return AdminDashboardStatsResponse.builder()
                .totalUtilisateurs(totalUtilisateurs)
                .utilisateursActifs(utilisateursActifs)
                .totalAssures(totalAssures)
                .totalExperts(totalExperts)
                .totalGestionnaires(totalGestionnaires)
                .totalAdmins(totalAdmins)
                .totalSinistres(totalSinistres)
                .sinistresEnCours(sinistresEnCours)
                .sinistresClotures(sinistresClotures)
                .sinistresCeMois(sinistresCeMois)
                .expertisesEnCours(expertisesEnCours)
                .expertsDisponibles(expertsDisponibles)
                .build();
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
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
                .build();
    }
}
