package com.example.backend.repository;

import com.example.backend.model.Claim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository MongoDB pour l'entité Claim.
 * Fournit les opérations CRUD de base via MongoRepository
 * ainsi que des méthodes de recherche dérivées automatiquement.
 */
@Repository
public interface ClaimRepository extends MongoRepository<Claim, String> {

    /**
     * Recherche tous les sinistres déclarés par un assuré spécifique.
     *
     * @param assureId l'identifiant de l'assuré
     * @return la liste des sinistres de l'assuré
     */
    List<Claim> findByAssureId(String assureId);

    /**
     * Recherche tous les sinistres ayant un statut donné.
     *
     * @param statut le statut du sinistre
     * @return la liste des sinistres avec ce statut
     */
    List<Claim> findByStatut(Claim.StatutSinistre statut);

    /**
     * Recherche tous les sinistres assignés à un gestionnaire.
     *
     * @param gestionnaireId l'identifiant du gestionnaire
     * @return la liste des sinistres assignés
     */
    List<Claim> findByGestionnaireId(String gestionnaireId);

    /**
     * Recherche tous les sinistres assignés à un expert.
     *
     * @param expertId l'identifiant de l'expert
     * @return la liste des sinistres assignés
     */
    List<Claim> findByExpertId(String expertId);

    /**
     * Recherche les sinistres assignés à un gestionnaire avec un statut donné.
     *
     * @param gestionnaireId l'identifiant du gestionnaire
     * @param statut le statut du sinistre
     * @return la liste des sinistres correspondants
     */
    List<Claim> findByGestionnaireIdAndStatut(String gestionnaireId, Claim.StatutSinistre statut);

    /**
     * Recherche les sinistres assignés à un expert avec un statut donné.
     *
     * @param expertId l'identifiant de l'expert
     * @param statut le statut du sinistre
     * @return la liste des sinistres correspondants
     */
    List<Claim> findByExpertIdAndStatut(String expertId, Claim.StatutSinistre statut);
}
