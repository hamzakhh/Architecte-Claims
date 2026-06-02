package com.example.backend.repository;

import com.example.backend.model.FraudAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository MongoDB pour l'entité FraudAlert.
 */
@Repository
public interface FraudAlertRepository extends MongoRepository<FraudAlert, String> {

    List<FraudAlert> findBySignalePar(String signalePar);

    List<FraudAlert> findByStatut(FraudAlert.StatutAlerte statut);

    List<FraudAlert> findByClaimId(String claimId);

    List<FraudAlert> findByNiveauRisque(FraudAlert.NiveauRisque niveauRisque);

    List<FraudAlert> findByStatutIn(List<FraudAlert.StatutAlerte> statuts);

    long countByStatut(FraudAlert.StatutAlerte statut);

    long countByStatutIn(List<FraudAlert.StatutAlerte> statuts);
}
