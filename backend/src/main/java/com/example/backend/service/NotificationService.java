package com.example.backend.service;

import com.example.backend.dto.NotificationResponse;
import com.example.backend.model.Notification;
import com.example.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Crée et envoie une notification à un utilisateur.
     */
    public void envoyerNotification(String utilisateurId, String titre, String message,
                                     String type, String claimId) {
        Notification notification = new Notification();
        notification.setUtilisateurId(utilisateurId);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setClaimId(claimId);
        notification.setLu(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("Notification envoyée à {}: {}", utilisateurId, titre);
    }

    /**
     * Récupère les notifications d'un utilisateur.
     */
    public List<NotificationResponse> getNotifications(String utilisateurId) {
        return notificationRepository.findByUtilisateurIdOrderByCreatedAtDesc(utilisateurId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les notifications non lues d'un utilisateur.
     */
    public List<NotificationResponse> getUnreadNotifications(String utilisateurId) {
        return notificationRepository.findByUtilisateurIdAndLuFalse(utilisateurId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Compte les notifications non lues d'un utilisateur.
     */
    public long countUnread(String utilisateurId) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateurId);
    }

    /**
     * Marque une notification comme lue.
     */
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        notification.setLu(true);
        notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     */
    public void markAllAsRead(String utilisateurId) {
        List<Notification> unread = notificationRepository.findByUtilisateurIdAndLuFalse(utilisateurId);
        unread.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .utilisateurId(notification.getUtilisateurId())
                .titre(notification.getTitre())
                .message(notification.getMessage())
                .type(notification.getType())
                .claimId(notification.getClaimId())
                .lu(notification.isLu())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
