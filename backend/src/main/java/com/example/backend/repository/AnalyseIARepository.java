package com.example.backend.repository;

import com.example.backend.model.AnalyseIA;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository MongoDB pour les analyses IA.
 */
@Repository
public interface AnalyseIARepository extends MongoRepository<AnalyseIA, String> {

    Optional<AnalyseIA> findByClaimId(String claimId);

    List<AnalyseIA> findByClaimIdIn(List<String> claimIds);

    List<AnalyseIA> findByStatut(AnalyseIA.StatutAnalyse statut);

    List<AnalyseIA> findBySeverite(AnalyseIA.Severite severite);

    List<AnalyseIA> findByNecessiteExpertHumain(boolean necessite);

    List<AnalyseIA> findByTypeAnalyse(AnalyseIA.TypeAnalyse typeAnalyse);

    long countByStatut(AnalyseIA.StatutAnalyse statut);

    long countBySeverite(AnalyseIA.Severite severite);

    long countByNecessiteExpertHumain(boolean necessite);
}
