package com.example.backend.repository;

import com.example.backend.model.Role;
import com.example.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository MongoDB pour l'entité User.
 * Fournit les opérations CRUD de base via MongoRepository
 * ainsi que des méthodes de recherche dérivées automatiquement.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email l'adresse email à rechercher
     * @return l'utilisateur correspondant, ou Optional.empty() si non trouvé
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un utilisateur existe avec l'email donné.
     *
     * @param email l'adresse email à vérifier
     * @return true si un utilisateur avec cet email existe
     */
    boolean existsByEmail(String email);

    /**
     * Recherche tous les utilisateurs ayant un rôle donné.
     *
     * @param role le rôle à rechercher
     * @return la liste des utilisateurs avec ce rôle
     */
    List<User> findByRole(Role role);

    /**
     * Compte le nombre d'utilisateurs ayant un rôle donné.
     *
     * @param role le rôle à compter
     * @return le nombre d'utilisateurs avec ce rôle
     */
    long countByRole(Role role);

    /**
     * Compte le nombre d'utilisateurs actifs (enabled = true).
     *
     * @return le nombre d'utilisateurs actifs
     */
    long countByEnabled(boolean enabled);

    /**
     * Recherche les utilisateurs dont le prénom ou le nom contient le terme donné.
     *
     * @param prenom terme à rechercher dans le prénom
     * @param nom terme à rechercher dans le nom
     * @return la liste des utilisateurs correspondants
     */
    List<User> findByPrenomContainingIgnoreCaseOrNomContainingIgnoreCaseOrEmailContainingIgnoreCase(String prenom, String nom, String email);
}
