package com.example.backend.repository;

import com.example.backend.model.Reimbursement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReimbursementRepository extends MongoRepository<Reimbursement, String> {
    List<Reimbursement> findByAssureIdOrderByCreatedAtDesc(String assureId);
    List<Reimbursement> findByClaimId(String claimId);
    List<Reimbursement> findByStatut(Reimbursement.StatutRemboursement statut);
    List<Reimbursement> findByStatutNotIn(List<Reimbursement.StatutRemboursement> statuts);
    Reimbursement findByStripeSessionId(String stripeSessionId);
    Reimbursement findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Reimbursement> findByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);
    List<Reimbursement> findByAssureIdAndStatut(String assureId, Reimbursement.StatutRemboursement statut);
    long countByStatut(Reimbursement.StatutRemboursement statut);
}
