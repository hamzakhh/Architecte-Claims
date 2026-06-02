package com.example.backend.repository;

import com.example.backend.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUtilisateurIdOrderByCreatedAtDesc(String utilisateurId);
    List<Notification> findByUtilisateurIdAndLuFalse(String utilisateurId);
    long countByUtilisateurIdAndLuFalse(String utilisateurId);
}
