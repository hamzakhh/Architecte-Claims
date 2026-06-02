# API Backend - Documentation Postman

**Base URL** : `http://localhost:8080`  
**Auth** : Bearer Token (JWT) → Header `Authorization: Bearer <token>`  
**Content-Type** : `application/json` (sauf upload de fichiers)

---

## Table des matières

1. [Authentification](#1-authentification)
2. [Utilisateurs](#2-utilisateurs)
3. [Sinistres (Claims)](#3-sinistres-claims)
4. [Historique Sinistre](#4-historique-sinistre)
5. [Expertises](#5-expertises)
6. [Analyse IA](#6-analyse-ia)
7. [Alertes de Fraude](#7-alertes-de-fraude)
8. [Remboursements](#8-remboursements)
9. [Messages](#9-messages)
10. [Notifications](#10-notifications)
11. [Tickets](#11-tickets)
12. [Fichiers](#12-fichiers)
13. [Rapports / Exports](#13-rapports--exports)
14. [Admin](#14-admin)

---

## 1. Authentification

### 1.1 Connexion

```
POST /api/auth/login
```

**Rôle** : Public  
**Body** :
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
**Response** : `AuthResponse` (token JWT + infos utilisateur)

---

### 1.2 Inscription

```
POST /api/auth/register
```

**Rôle** : Public  
**Body** :
```json
{
  "email": "user@example.com",
  "password": "password123",
  "prenom": "Jean",
  "nom": "Dupont",
  "telephone": "0601020304",
  "role": "ASSURE"
}
```
**Response** : `AuthResponse` (token JWT + infos utilisateur)

---

## 2. Utilisateurs

### 2.1 Mon profil

```
GET /api/users/me
```

**Rôle** : Authentifié  
**Headers** : `Authorization: Bearer <token>`

---

### 2.2 Mettre à jour mon profil

```
PUT /api/users/me
```

**Rôle** : Authentifié  
**Body** :
```json
{
  "prenom": "Jean",
  "nom": "Dupont",
  "telephone": "0601020304"
}
```

---

### 2.3 Changer mon mot de passe

```
PUT /api/users/me/password
```

**Rôle** : Authentifié  
**Body** :
```json
{
  "ancienPassword": "oldPassword123",
  "nouveauPassword": "newPassword456"
}
```

---

### 2.4 Activer/Désactiver un utilisateur

```
PATCH /api/users/{id}/toggle-status
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Path** : `id` = identifiant de l'utilisateur

---

### 2.5 Liste de tous les utilisateurs

```
GET /api/users
```

**Rôle** : ADMIN

---

### 2.6 Utilisateurs par rôle

```
GET /api/users/role/{role}
```

**Rôle** : ADMIN  
**Path** : `role` = ASSURE | GESTIONNAIRE | EXPERT | ADMIN

---

### 2.7 Créer un utilisateur (admin)

```
POST /api/users
```

**Rôle** : ADMIN  
**Body** :
```json
{
  "email": "new@example.com",
  "password": "password123",
  "prenom": "Marie",
  "nom": "Curie",
  "telephone": "0612345678",
  "role": "EXPERT"
}
```

---

### 2.8 Modifier le rôle d'un utilisateur

```
PATCH /api/users/{id}/role
```

**Rôle** : ADMIN  
**Body** : `"EXPERT"` (string brute du rôle)

---

### 2.9 Supprimer un utilisateur

```
DELETE /api/users/{id}
```

**Rôle** : ADMIN

---

### 2.10 Rechercher des utilisateurs

```
GET /api/users/search?term=jean
```

**Rôle** : ADMIN  
**Query Param** : `term` = terme de recherche

---

### 2.11 Statistiques dashboard admin

```
GET /api/users/admin-stats
```

**Rôle** : ADMIN

---

## 3. Sinistres (Claims)

### 3.1 Créer un sinistre

```
POST /api/claims
```

**Rôle** : ASSURE, ADMIN  
**Body** :
```json
{
  "categorie": "HABITATION",
  "type": "DEGAT_DES_EAUX",
  "description": "Fuite d'eau dans la cuisine",
  "lieu": "12 Rue des Lilas, Paris",
  "dateSinistre": "2025-01-15T10:30:00",
  "documents": ["doc1.pdf", "photo1.jpg"]
}
```

---

### 3.2 Mes sinistres (assuré)

```
GET /api/claims/mes-sinistres
```

**Rôle** : ASSURE

---

### 3.3 Sinistre par ID

```
GET /api/claims/{id}
```

**Rôle** : ASSURE, GESTIONNAIRE, EXPERT, ADMIN

---

### 3.4 Tous les sinistres

```
GET /api/claims
```

**Rôle** : GESTIONNAIRE, EXPERT, ADMIN

---

### 3.5 Mettre à jour le statut

```
PATCH /api/claims/{id}/statut
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "statut": "EN_REVISION"
}
```
**Valeurs statut** : `EN_COURS` | `EN_REVISION` | `EXPERTISE` | `INDEMNISATION` | `CLOTURE` | `ARCHIVE` | `RECOURS`

---

### 3.6 Prendre en charge un sinistre

```
PATCH /api/claims/{id}/prendre-en-charge
```

**Rôle** : GESTIONNAIRE

---

### 3.7 Assigner un gestionnaire

```
PATCH /api/claims/{id}/assign-gestionnaire
```

**Rôle** : ADMIN  
**Body** : `"gestionnaireId"` (string brute, ID du gestionnaire)

---

### 3.8 Assigner un expert

```
PATCH /api/claims/{id}/assign-expert
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** : `"expertId"` (string brute, ID de l'expert)

---

### 3.9 Auto-assigner un expert

```
PATCH /api/claims/{id}/auto-assign-expert
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.10 Dossiers assignés (gestionnaire)

```
GET /api/claims/dossiers-assignes
```

**Rôle** : GESTIONNAIRE

---

### 3.11 Statistiques dashboard gestionnaire

```
GET /api/claims/dashboard-stats
```

**Rôle** : GESTIONNAIRE

---

### 3.12 Mes dossiers (expert)

```
GET /api/claims/mes-dossiers
```

**Rôle** : EXPERT

---

### 3.13 Statistiques dashboard expert

```
GET /api/claims/expert-dashboard-stats
```

**Rôle** : EXPERT

---

### 3.14 Liste des experts

```
GET /api/claims/experts
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.15 Liste des gestionnaires

```
GET /api/claims/gestionnaires
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.16 Transférer à un autre gestionnaire

```
PATCH /api/claims/{id}/transferer-gestionnaire
```

**Rôle** : GESTIONNAIRE  
**Body** : `"nouveauGestionnaireId"` (string brute)

---

### 3.17 Demander des documents supplémentaires

```
POST /api/claims/{id}/demander-documents
```

**Rôle** : GESTIONNAIRE  
**Body** :
```json
{
  "documents": ["Justificatif d'identité", "Facture de réparation"],
  "message": "Merci de fournir les documents suivants"
}
```

---

### 3.18 Notes internes

```
PATCH /api/claims/{id}/notes-internes
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** : `"Notes internes du gestionnaire..."` (string brute)

---

### 3.19 Archiver un sinistre

```
PATCH /api/claims/{id}/archiver
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.20 Qualifier un sinistre

```
PATCH /api/claims/{id}/qualifier
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "gravite": "MOYENNE",
  "couvertureContractuelle": true,
  "franchise": 150.0,
  "plafond": 5000.0
}
```

---

### 3.21 Proposer une indemnisation

```
PATCH /api/claims/{id}/proposer-indemnisation
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "montantPropose": 3500.0,
  "motif": "Indemnisation pour dégât des eaux"
}
```

---

### 3.22 Accepter l'indemnisation

```
PATCH /api/claims/{id}/accepter-indemnisation
```

**Rôle** : ASSURE, GESTIONNAIRE, ADMIN

---

### 3.23 Refuser l'indemnisation

```
PATCH /api/claims/{id}/refuser-indemnisation
```

**Rôle** : ASSURE, GESTIONNAIRE, ADMIN  
**Body** : `"Motif du refus..."` (string brute)

---

### 3.24 Initier le paiement

```
PATCH /api/claims/{id}/initier-paiement
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.25 Confirmer le paiement

```
PATCH /api/claims/{id}/confirmer-paiement
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.26 Déclarer un recours/litige

```
PATCH /api/claims/{id}/declarer-recours
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 3.27 Migration expertises (admin temporaire)

```
POST /api/claims/migrate-expertises
```

**Rôle** : ADMIN  
**Response** : `"3 expertise(s) créée(s)"`

---

## 4. Historique Sinistre

### 4.1 Historique d'un sinistre

```
GET /api/claims/{claimId}/history
```

**Rôle** : Authentifié  
**Path** : `claimId` = identifiant du sinistre

---

## 5. Expertises

### 5.1 Créer une expertise

```
POST /api/expertises
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "claimId": "claim-id-value",
  "expertId": "expert-id-value",
  "gestionnaireId": "gestionnaire-id-value"
}
```

---

### 5.2 Expertise par ID

```
GET /api/expertises/{id}
```

**Rôle** : GESTIONNAIRE, EXPERT, ADMIN

---

### 5.3 Expertises d'un sinistre

```
GET /api/expertises/claim/{claimId}
```

**Rôle** : GESTIONNAIRE, EXPERT, ADMIN

---

### 5.4 Mes expertises (expert)

```
GET /api/expertises/mes-expertises
```

**Rôle** : EXPERT

---

### 5.5 Expertises du gestionnaire

```
GET /api/expertises/gestionnaire
```

**Rôle** : GESTIONNAIRE

---

### 5.6 Toutes les expertises

```
GET /api/expertises
```

**Rôle** : ADMIN

---

### 5.7 Soumettre un rapport d'expertise

```
PUT /api/expertises/{id}/rapport
```

**Rôle** : EXPERT  
**Body** :
```json
{
  "conclusions": "Le sinistre est confirmé. Les dégâts sont évalués à 3200€.",
  "recommandation": "Indemnisation partielle recommandée",
  "montantEstime": 3200.0,
  "fichiersRapport": ["rapport_expert.pdf"]
}
```

---

### 5.8 Sauvegarder un brouillon

```
PUT /api/expertises/{id}/brouillon
```

**Rôle** : EXPERT  
**Body** : Même format que 5.7

---

### 5.9 Valider une expertise

```
PATCH /api/expertises/{id}/valider
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 5.10 Refuser une expertise

```
PATCH /api/expertises/{id}/refuser
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** (optionnel) : `"Justification du refus"` (string brute)

---

## 6. Analyse IA

### 6.1 Lancer une analyse IA

```
POST /api/analyses-ia
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "claimId": "claim-id-value"
}
```

---

### 6.2 Analyse par ID sinistre

```
GET /api/analyses-ia/claim/{claimId}
```

**Rôle** : ASSURE, GESTIONNAIRE, EXPERT, ADMIN

---

### 6.3 Analyses nécessitant un expert

```
GET /api/analyses-ia/expert-requis
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 6.4 Toutes les analyses

```
GET /api/analyses-ia
```

**Rôle** : ADMIN

---

### 6.5 Statistiques analyses IA

```
GET /api/analyses-ia/stats
```

**Rôle** : GESTIONNAIRE, ADMIN

---

## 7. Alertes de Fraude

### 7.1 Créer un signalement

```
POST /api/fraud-alerts
```

**Rôle** : GESTIONNAIRE  
**Body** :
```json
{
  "claimId": "claim-id-value",
  "motif": "DOCUMENTS_SUSPECTS",
  "description": "Les documents fournis semblent falsifiés",
  "gravite": "HAUTE"
}
```

---

### 7.2 Mes signalements (gestionnaire)

```
GET /api/fraud-alerts/mes-signalements
```

**Rôle** : GESTIONNAIRE

---

### 7.3 Alerte par ID

```
GET /api/fraud-alerts/{id}
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 7.4 Toutes les alertes

```
GET /api/fraud-alerts
```

**Rôle** : ADMIN

---

### 7.5 Alertes en attente

```
GET /api/fraud-alerts/en-attente
```

**Rôle** : ADMIN

---

### 7.6 Mettre en cours d'analyse

```
PATCH /api/fraud-alerts/{id}/analyser
```

**Rôle** : ADMIN

---

### 7.7 Résoudre une alerte

```
PATCH /api/fraud-alerts/{id}/resoudre
```

**Rôle** : ADMIN  
**Body** :
```json
{
  "decision": "FRAUDE_CONFIRMEE",
  "justification": "Analyse approfondie confirmant la fraude",
  "action": "SUSPENDRE_SINISTRE"
}
```

---

### 7.8 Statistiques fraude

```
GET /api/fraud-alerts/stats
```

**Rôle** : ADMIN

---

## 8. Remboursements

### 8.1 Calculer l'indemnisation (prévisualisation)

```
POST /api/reimbursements/calculer
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "claimId": "claim-id-value",
  "montantEstime": 3200.0,
  "franchise": 150.0,
  "plafond": 5000.0
}
```

---

### 8.2 Créer un remboursement

```
POST /api/reimbursements
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** :
```json
{
  "claimId": "claim-id-value",
  "montantPropose": 3050.0,
  "methodePaiement": "VIREMENT"
}
```
**Valeurs methodePaiement** : `VIREMENT` | `CHEQUE` | `STRIPE`

---

### 8.3 Proposition détaillée

```
GET /api/reimbursements/{id}/proposition
```

**Rôle** : Authentifié

---

### 8.4 Mes remboursements (assuré)

```
GET /api/reimbursements/my
```

**Rôle** : ASSURE

---

### 8.5 Tous les remboursements

```
GET /api/reimbursements
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 8.6 Remboursement par ID

```
GET /api/reimbursements/{id}
```

**Rôle** : Authentifié

---

### 8.7 Remboursements par statut

```
GET /api/reimbursements/statut/{statut}
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Valeurs statut** : `PROPOSE` | `VALIDE` | `REFUSE` | `EN_TRAITEMENT` | `PAYE`

---

### 8.8 Statistiques remboursements

```
GET /api/reimbursements/stats
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 8.9 Valider un remboursement (assuré)

```
PUT /api/reimbursements/{id}/validate
```

**Rôle** : ASSURE

---

### 8.10 Refuser un remboursement (assuré)

```
PUT /api/reimbursements/{id}/refuse
```

**Rôle** : ASSURE  
**Body** : `"Motif du refus"` (string brute)

---

### 8.11 Créer une session Stripe Checkout

```
POST /api/reimbursements/{id}/stripe/checkout
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Response** :
```json
{
  "sessionId": "cs_test_...",
  "url": "https://checkout.stripe.com/..."
}
```

---

### 8.12 Vérifier session Stripe

```
GET /api/reimbursements/{id}/stripe/verify
```

**Rôle** : Authentifié  
**Response** :
```json
{
  "paymentStatus": "paid"
}
```

---

### 8.13 Stripe Webhook

```
POST /api/reimbursements/stripe/webhook
```

**Rôle** : Aucun (appelé par Stripe)  
**Headers** : `Stripe-Signature: <signature>`  
**Body** : Raw payload (JSON string)

---

### 8.14 Traiter un remboursement

```
PUT /api/reimbursements/{id}/process
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 8.15 Confirmer le paiement

```
PUT /api/reimbursements/{id}/pay
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 8.16 Générer lettre de remboursement (PDF)

```
GET /api/reimbursements/{id}/lettre-remboursement
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Response** : Fichier PDF (binary)

---

### 8.17 Générer lettre de rejet (PDF)

```
POST /api/reimbursements/{id}/lettre-rejet
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Body** : `"Motif du rejet"` (string brute)  
**Response** : Fichier PDF (binary)

---

## 9. Messages

### 9.1 Envoyer un message

```
POST /api/messages
```

**Rôle** : ASSURE, GESTIONNAIRE, EXPERT, ADMIN  
**Body** :
```json
{
  "conversationId": "conv-id",
  "contenu": "Bonjour, j'ai une question sur mon dossier."
}
```

---

### 9.2 Mes conversations

```
GET /api/messages/conversations
```

**Rôle** : ASSURE, GESTIONNAIRE, EXPERT, ADMIN

---

### 9.3 Messages d'une conversation

```
GET /api/messages/conversations/{conversationId}
```

**Rôle** : ASSURE, GESTIONNAIRE, EXPERT, ADMIN

---

## 10. Notifications

### 10.1 Mes notifications

```
GET /api/notifications
```

**Rôle** : Authentifié

---

### 10.2 Notifications non lues

```
GET /api/notifications/unread
```

**Rôle** : Authentifié

---

### 10.3 Compteur non lues

```
GET /api/notifications/unread-count
```

**Rôle** : Authentifié  
**Response** : `5` (Long)

---

### 10.4 Marquer comme lue

```
PUT /api/notifications/{id}/read
```

**Rôle** : Authentifié

---

### 10.5 Tout marquer comme lu

```
PUT /api/notifications/read-all
```

**Rôle** : Authentifié

---

## 11. Tickets

### 11.1 Créer un ticket

```
POST /api/tickets
```

**Rôle** : ASSURE  
**Body** :
```json
{
  "sujet": "Problème de connexion",
  "description": "Je n'arrive pas à accéder à mon espace",
  "categorie": "TECHNIQUE"
}
```

---

### 11.2 Mes tickets (assuré)

```
GET /api/tickets/my
```

**Rôle** : ASSURE

---

### 11.3 Tous les tickets

```
GET /api/tickets
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 11.4 Ticket par ID

```
GET /api/tickets/{id}
```

**Rôle** : Authentifié

---

### 11.5 Ajouter un message au ticket

```
POST /api/tickets/{id}/messages
```

**Rôle** : Authentifié  
**Body** :
```json
{
  "contenu": "J'ai toujours le problème"
}
```

---

### 11.6 Assigner un ticket

```
PUT /api/tickets/{id}/assign?gestionnaireId=gestionnaire-id
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Query Param** : `gestionnaireId`

---

### 11.7 Résoudre un ticket

```
PUT /api/tickets/{id}/resolve
```

**Rôle** : GESTIONNAIRE, ADMIN

---

### 11.8 Fermer un ticket

```
PUT /api/tickets/{id}/close
```

**Rôle** : GESTIONNAIRE, ADMIN

---

## 12. Fichiers

### 12.1 Upload multiple fichiers

```
POST /api/files/upload
```

**Rôle** : ASSURE, EXPERT, GESTIONNAIRE, ADMIN  
**Content-Type** : `multipart/form-data`  
**Form Data** : `files` = (sélectionner plusieurs fichiers)  
**Response** :
```json
["017ee1f4-b47d-4dbd-8f0d-3e27e2ab5596.png", "0477279d-55af-444a-be02-5444bf61a799.pdf"]
```

---

### 12.2 Upload un seul fichier

```
POST /api/files/upload-single
```

**Rôle** : ASSURE, EXPERT, GESTIONNAIRE, ADMIN  
**Content-Type** : `multipart/form-data`  
**Form Data** : `file` = (sélectionner un fichier)  
**Response** :
```json
{
  "storedName": "017ee1f4-b47d-4dbd-8f0d-3e27e2ab5596.png",
  "originalName": "photo.png"
}
```

---

### 12.3 Supprimer un fichier

```
DELETE /api/files/{filename}
```

**Rôle** : ASSURE, EXPERT, GESTIONNAIRE, ADMIN  
**Path** : `filename` = nom stocké (ex: `017ee1f4-b47d-4dbd-8f0d-3e27e2ab5596.png`)

---

### 12.4 Télécharger un fichier

```
GET /api/files/download/{filename}
```

**Rôle** : ASSURE, EXPERT, GESTIONNAIRE, ADMIN  
**Path** : `filename` = nom stocké  
**Response** : Fichier binaire avec header `Content-Disposition: attachment`

---

## 13. Rapports / Exports

### 13.1 Export sinistres CSV

```
GET /api/reports/claims/csv
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Query Params** (tous optionnels) :
- `statut` = EN_COURS | EN_REVISION | EXPERTISE | INDEMNISATION | CLOTURE | ARCHIVE | RECOURS
- `categorie` = HABITATION | AUTO | VIE | etc.
- `dateDebut` = 2025-01-01 (format ISO DATE)
- `dateFin` = 2025-12-31 (format ISO DATE)

**Response** : Fichier CSV (attachment)

---

### 13.2 Export remboursements CSV

```
GET /api/reports/reimbursements/csv
```

**Rôle** : GESTIONNAIRE, ADMIN  
**Query Param** (optionnel) :
- `statut` = PROPOSE | VALIDE | REFUSE | EN_TRAITEMENT | PAYE

**Response** : Fichier CSV (attachment)

---

## 14. Admin

### 14.1 Statistiques analytiques

```
GET /api/admin/analytics
```

**Rôle** : ADMIN

---

### 14.2 Logs d'audit

```
GET /api/admin/audit-logs
```

**Rôle** : ADMIN

---

### 14.3 Charge de travail

```
GET /api/admin/workload
```

**Rôle** : ADMIN

---

## Configuration Postman

### Variable d'environnement suggérée

| Variable | Valeur |
|----------|--------|
| `base_url` | `http://localhost:8080` |
| `token` | _(rempli après login)_ |

### Workflow de test recommandé

1. **Login** → `POST /api/auth/login` → Copier le `token` de la réponse
2. **Set Token** → Dans les headers Postman : `Authorization: Bearer {{token}}`
3. **Créer un sinistre** → `POST /api/claims`
4. **Upload fichiers** → `POST /api/files/upload` (multipart/form-data)
5. **Tester les workflows** selon le rôle connecté

### Rôles disponibles

| Rôle | Accès principal |
|------|----------------|
| `ASSURE` | Créer sinistres, voir ses sinistres, messages, tickets, notifications |
| `GESTIONNAIRE` | Gérer sinistres, expertises, fraude, remboursements, rapports |
| `EXPERT` | Voir ses expertises, soumettre rapports |
| `ADMIN` | Accès complet + analytics + audit + gestion utilisateurs |
