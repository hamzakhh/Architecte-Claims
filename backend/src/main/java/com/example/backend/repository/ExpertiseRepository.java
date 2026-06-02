package com.example.backend.repository;

import com.example.backend.model.Expertise;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository MongoDB pour l'entité Expertise.
 */
@Repository
public interface ExpertiseRepository extends MongoRepository<Expertise, String> {

    List<Expertise> findByClaimId(String claimId);

    List<Expertise> findByExpertId(String expertId);

    List<Expertise> findByGestionnaireId(String gestionnaireId);

    List<Expertise> findByStatut(Expertise.StatutExpertise statut);

    List<Expertise> findByExpertIdAndStatut(String expertId, Expertise.StatutExpertise statut);
}
