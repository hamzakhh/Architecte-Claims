# Documentation Technique — L'Architecte Claims

## 1. Architecture Générale

| Couche | Technologie | Port |
|--------|-------------|------|
| Frontend | Angular 17+ (standalone), Nginx | 80 |
| Backend | Spring Boot 3, Spring Security + JWT | 8081 |
| Base de données | MongoDB 8 | 27017 |
| IA | Ollama (llama3) | 11434 |
| Paiement | Stripe Checkout | — |

## 2. Modèles de Données

### Role (Enum)
`ASSURE` | `GESTIONNAIRE` | `EXPERT` | `ADMIN`

### User — `users`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| prenom/nom | String | Nom complet |
| email | String (unique) | Email = login |
| password | String | BCrypt |
| telephone | String | Téléphone |
| role | Role | Rôle utilisateur |
| specialite/zoneIntervention/notePerformance/chargeMax/certifications | — | Champs expert |
| enabled | boolean | Compte actif |
| createdAt/updatedAt | LocalDateTime | Timestamps |

### Claim — `claims`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| reference | String (unique) | SIN-YYYY-XXXX |
| assureId | FK→User | Assuré déclarant |
| categorie/type | String | Catégorie sinistre |
| latitude/longitude | Double | Géolocalisation |
| description/dateSinistre/heureSinistre/lieu | — | Infos incident |
| piecesJointes | List<String> | Fichiers joints |
| estimation | String | Estimation dégâts |
| notesInternes | String | Notes équipe |
| gravite | Enum | MINEURE/MODEREE/MAJEURE/CRITIQUE |
| couvertureContractuelle/franchise/plafondCouverture | — | Qualification |
| montantIndemnisationPropose/montantIndemnisationFinal | Double | Indemnisation |
| indemnisationAcceptee/recoursEnCours | Boolean | Statut indemnisation |
| statut | StatutSinistre | 11 statuts (voir ci-dessous) |
| gestionnaireId/expertId | FK→User | Assignations |
| analyseIAId | FK→AnalyseIA | Analyse IA |

**StatutSinistre**: EN_COURS → EN_REVISION → EXPERTISE → VALIDE/REFUSE → INDEMNISATION_PROPOSEE → INDEMNISATION_ACCEPTEE → PAIEMENT_EN_COURS → CLOTURE / RECOURS / ARCHIVE

### ClaimHistory — `claim_history`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| claimId | FK→Claim | Sinistre concerné |
| action | String | Type d'action |
| description | String | Description action |
| utilisateurId/utilisateurNom/utilisateurRole | — | Acteur |
| ancienStatut/nouveauStatut | String | Changement statut |
| createdAt | LocalDateTime | Timestamp |

### Expertise — `expertises`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| claimId | FK→Claim | Sinistre |
| expertId | FK→User | Expert |
| gestionnaireId | FK→User | Gestionnaire |
| conclusion/montantEstime/recommandation | — | Résultat expertise |
| piecesJointes/commentaires | — | Justifications |
| statut | StatutExpertise | EN_ATTENTE/EN_COURS/SOUMISE/VALIDEE/REFUSEE |
| dateRapport | LocalDateTime | Date rapport |

### AnalyseIA — `analyses_ia`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| claimId | FK→Claim | Sinistre analysé |
| scoreComplexite/scoreRisque/scoreConfiance | int (0-100) | Scores |
| montantEstime/devise | — | Estimation IA |
| severite | Enum | FAIBLE/MODEREE/ELEVEE/CRITIQUE |
| categorieDetectee/motsCles | — | Classification |
| necessiteExpertHumain/recommandation/justification | — | Recommandation |
| resumeAnalyse/pointsAttention/elementsFraude | — | Analyse détaillée |
| typeAnalyse | Enum | INITIALE/APPROFONDIE/REANALYSE/VERIFICATION_FRAUDE |
| statut | Enum | EN_COURS/TERMINEE/ERREUR/EXPIREE |
| modeleIA/tokensUtilises/coutEstime | — | Métadonnées IA |

### FraudAlert — `fraud_alerts`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| claimId | FK→Claim | Sinistre suspect |
| signalePar | FK→User | Gestionnaire signalant |
| motif/description | String | Motif signalement |
| niveauRisque | Enum | FAIBLE/MOYEN/ELEVE/CRITIQUE |
| statut | StatutAlerte | SOUMISE→EN_COURS_ANALYSE→CONFIRMEE/INFONDEE/CLOTUREE |
| piecesJustificatives | List<String> | Pièces |
| resoluPar/decision/notesResolution/dateResolution | — | Résolution admin |

### Reimbursement — `reimbursements`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| claimId | FK→Claim | Sinistre |
| assureId | FK→User | Bénéficiaire |
| reference | String | REM-YYYY-XXXX |
| montantDegats/capitalAssure/franchise/plafondGarantie/tauxRemboursement | — | Calcul |
| montantApresFranchise/montantIndemnisationCalcule | — | Résultat calcul |
| montantPropose/montantFinal | Double | Montants |
| stripeSessionId/stripePaymentIntentId/transactionId | — | Stripe |
| methodePaiement | Enum | CARTE_BANCAIRE |
| statut | Enum | EN_ATTENTE/VALIDEE/EN_COURS_TRAITEMENT/PAYE/REFUSE |
| historiqueWorkflow | Embedded[] | Traçabilité |
| gestionnaireId/motifRefus/notes | — | Gestion |

### Conversation — `conversations`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| participant1Id | FK→User | Gestionnaire |
| participant2Id | FK→User | Assuré |
| claimId | FK→Claim (optionnel) | Sinistre lié |
| dernierMessage/dernierMessageDate | — | Dernier message |
| messagesNonLusParticipant1/messagesNonLusParticipant2 | int | Non lus |

### Message — `messages`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| conversationId | FK→Conversation | Conversation |
| expediteurId | FK→User | Expéditeur |
| contenu | String | Contenu |
| lu | boolean | Lu |

### Notification — `notifications`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| utilisateurId | FK→User | Destinataire |
| titre/message | String | Contenu |
| type | String | STATUT_CHANGE/EXPERTISE/REMBOURSEMENT/MESSAGE/SYSTEME |
| claimId | FK→Claim (optionnel) | Sinistre lié |
| lu | boolean | Lu |

### Ticket — `tickets`
| Champ | Type | Description |
|-------|------|-------------|
| id | String | ID MongoDB |
| assureId | FK→User | Assuré créateur |
| claimId | FK→Claim (optionnel) | Sinistre lié |
| sujet/description/categorie | String | Infos ticket |
| statut | Enum | OUVERT/EN_COURS/EN_ATTENTE/RESOLU/FERME |
| assigneA | FK→User | Gestionnaire assigné |
| messages | Embedded TicketMessage[] | Messages du ticket |

---

## 3. Relations entre Modèles

- **User → Claim**: 1:N (assureId, gestionnaireId, expertId)
- **User → Expertise**: 1:N (expertId, gestionnaireId)
- **User → Notification**: 1:N (utilisateurId)
- **User → Conversation**: 1:N (participant1Id, participant2Id)
- **User → Message**: 1:N (expediteurId)
- **User → Ticket**: 1:N (assureId)
- **User → Reimbursement**: 1:N (assureId)
- **User → FraudAlert**: 1:N (signalePar, resoluPar)
- **Claim → ClaimHistory**: 1:N (claimId)
- **Claim → AnalyseIA**: 1:1 (analyseIAId / claimId)
- **Claim → Expertise**: 1:N (claimId)
- **Claim → Reimbursement**: 1:N (claimId)
- **Claim → FraudAlert**: 1:1 (claimId)
- **Claim → Conversation**: 0:1 (claimId)
- **Claim → Ticket**: 0:1 (claimId)
- **Conversation → Message**: 1:N (conversationId)
- **Ticket → TicketMessage**: 1:N (embedded)
- **Reimbursement → EtapeWorkflow**: 1:N (embedded)

---

## 4. API REST — Endpoints

### `/api/auth`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | /login | Public | Connexion → JWT |
| POST | /register | Public | Inscription |

### `/api/claims`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | / | ASSURE, ADMIN | Créer sinistre |
| GET | /mes-sinistres | ASSURE | Sinistres de l'assuré |
| GET | /{id} | Tous | Détail sinistre |
| GET | / | GESTIONNAIRE, EXPERT, ADMIN | Tous sinistres |
| PATCH | /{id}/statut | GESTIONNAIRE, ADMIN | Changer statut |
| PATCH | /{id}/prendre-en-charge | GESTIONNAIRE | Prendre en charge |
| PATCH | /{id}/assign-gestionnaire | ADMIN | Assigner gestionnaire |
| PATCH | /{id}/assign-expert | GESTIONNAIRE, ADMIN | Assigner expert |
| PATCH | /{id}/auto-assign-expert | GESTIONNAIRE, ADMIN | Auto-assignation |
| GET | /dossiers-assignes | GESTIONNAIRE | Dossiers assignés |
| GET | /dashboard-stats | GESTIONNAIRE | Stats dashboard |
| GET | /mes-dossiers | EXPERT | Dossiers expert |
| GET | /expert-dashboard-stats | EXPERT | Stats expert |
| GET | /experts | GESTIONNAIRE, ADMIN | Liste experts |
| GET | /gestionnaires | GESTIONNAIRE, ADMIN | Liste gestionnaires |
| PATCH | /{id}/transferer-gestionnaire | GESTIONNAIRE | Transférer dossier |
| POST | /{id}/demander-documents | GESTIONNAIRE | Demander documents |
| PATCH | /{id}/notes-internes | GESTIONNAIRE, ADMIN | Notes internes |
| PATCH | /{id}/archiver | GESTIONNAIRE, ADMIN | Archiver |
| PATCH | /{id}/qualifier | GESTIONNAIRE, ADMIN | Qualifier sinistre |
| PATCH | /{id}/proposer-indemnisation | GESTIONNAIRE, ADMIN | Proposer indemnisation |
| PATCH | /{id}/accepter-indemnisation | ASSURE+ | Accepter |
| PATCH | /{id}/refuser-indemnisation | ASSURE+ | Refuser |
| PATCH | /{id}/initier-paiement | GESTIONNAIRE, ADMIN | Initier paiement |
| PATCH | /{id}/confirmer-paiement | GESTIONNAIRE, ADMIN | Confirmer paiement |
| PATCH | /{id}/declarer-recours | GESTIONNAIRE, ADMIN | Déclarer recours |
| POST | /migrate-expertises | ADMIN | Migration |

### `/api/claims/{claimId}/history`
| GET | / | Authentifié | Historique sinistre |

### `/api/expertises`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | / | GESTIONNAIRE, ADMIN | Créer expertise |
| GET | /{id} | GESTIONNAIRE, EXPERT, ADMIN | Détail |
| GET | /claim/{claimId} | GESTIONNAIRE, EXPERT, ADMIN | Par sinistre |
| GET | /mes-expertises | EXPERT | Mes expertises |
| GET | /gestionnaire | GESTIONNAIRE | Expertises gestionnaire |
| GET | / | ADMIN | Toutes |
| PUT | /{id}/rapport | EXPERT | Soumettre rapport |
| PUT | /{id}/brouillon | EXPERT | Sauver brouillon |
| PATCH | /{id}/valider | GESTIONNAIRE, ADMIN | Valider |
| PATCH | /{id}/refuser | GESTIONNAIRE, ADMIN | Refuser |

### `/api/analyses-ia`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | / | GESTIONNAIRE, ADMIN | Lancer analyse |
| GET | /claim/{claimId} | Tous | Analyse sinistre |
| GET | /expert-requis | GESTIONNAIRE, ADMIN | Expert requis |
| GET | / | ADMIN | Toutes analyses |
| GET | /stats | GESTIONNAIRE, ADMIN | Stats IA |

### `/api/fraud-alerts`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | / | GESTIONNAIRE | Créer signalement |
| GET | /mes-signalements | GESTIONNAIRE | Mes signalements |
| GET | /{id} | GESTIONNAIRE, ADMIN | Détail |
| GET | / | ADMIN | Toutes alertes |
| GET | /en-attente | ADMIN | En attente |
| PATCH | /{id}/analyser | ADMIN | Mettre en analyse |
| PATCH | /{id}/resoudre | ADMIN | Résoudre |
| GET | /stats | ADMIN | Stats fraude |

### `/api/reimbursements`
| Méthode | Route | Rôle | Description |
|---------|-------|------|-------------|
| POST | /calculer | GESTIONNAIRE, ADMIN | Calculer indemnisation |
| POST | / | GESTIONNAIRE, ADMIN | Créer remboursement |
| GET | /{id}/proposition | Authentifié | Proposition |
| GET | /my | ASSURE | Mes remboursements |
| GET | / | GESTIONNAIRE, ADMIN | Tous |
| GET | /{id} | Authentifié | Détail |
| GET | /statut/{statut} | GESTIONNAIRE, ADMIN | Par statut |
| GET | /stats | GESTIONNAIRE, ADMIN | Stats |
| PUT | /{id}/validate | ASSURE | Valider |
| PUT | /{id}/refuse | ASSURE | Refuser |
| POST | /{id}/stripe/checkout | GESTIONNAIRE, ADMIN | Session Stripe |
| GET | /{id}/stripe/verify | Authentifié | Vérifier Stripe |
| POST | /stripe/webhook | Public | Webhook Stripe |
| PUT | /{id}/process | GESTIONNAIRE, ADMIN | Traiter |
| PUT | /{id}/pay | GESTIONNAIRE, ADMIN | Confirmer paiement |
| GET | /{id}/lettre-remboursement | GESTIONNAIRE, ADMIN | PDF remboursement |
| POST | /{id}/lettre-rejet | GESTIONNAIRE, ADMIN | PDF rejet |

### `/api/messages`
| POST | / | Tous | Envoyer message |
| GET | /conversations | Tous | Mes conversations |
| GET | /conversations/{id} | Tous | Messages conversation |

### `/api/notifications`
| GET | / | Authentifié | Mes notifications |
| GET | /unread | Authentifié | Non lues |
| GET | /unread-count | Authentifié | Compteur |
| PUT | /{id}/read | Authentifié | Marquer lue |
| PUT | /read-all | Authentifié | Tout marquer lu |

### `/api/tickets`
| POST | / | ASSURE | Créer ticket |
| GET | /my | ASSURE | Mes tickets |
| GET | / | GESTIONNAIRE, ADMIN | Tous tickets |
| GET | /{id} | Authentifié | Détail |
| POST | /{id}/messages | Authentifié | Ajouter message |
| PUT | /{id}/assign | GESTIONNAIRE, ADMIN | Assigner |
| PUT | /{id}/resolve | GESTIONNAIRE, ADMIN | Résoudre |
| PUT | /{id}/close | GESTIONNAIRE, ADMIN | Fermer |

### `/api/users`
| GET | /me | Authentifié | Mon profil |
| PUT | /me | Authentifié | Modifier profil |
| PUT | /me/password | Authentifié | Changer mot de passe |
| PATCH | /{id}/toggle-status | GESTIONNAIRE, ADMIN | Activer/désactiver |
| GET | / | ADMIN | Tous utilisateurs |
| GET | /role/{role} | ADMIN | Par rôle |
| POST | / | ADMIN | Créer utilisateur |
| PATCH | /{id}/role | ADMIN | Changer rôle |
| DELETE | /{id} | ADMIN | Supprimer |
| GET | /search | ADMIN | Rechercher |
| GET | /admin-stats | ADMIN | Stats admin |

### `/api/admin`
| GET | /analytics | ADMIN | Stats analytics |
| GET | /audit-logs | ADMIN | Journaux audit |
| GET | /workload | ADMIN | Charge travail |

### `/api/files`
| POST | /upload | Authentifié | Upload fichiers |
| POST | /upload-single | Authentifié | Upload un fichier |
| DELETE | /{filename} | Authentifié | Supprimer fichier |
| GET | /download/{filename} | Authentifié | Télécharger fichier |

### `/api/reports`
| GET | /claims/csv | GESTIONNAIRE, ADMIN | Export CSV sinistres |
| GET | /reimbursements/csv | GESTIONNAIRE, ADMIN | Export CSV remboursements |

---

## 5. Services Backend — Responsabilités

| Service | Responsabilité |
|---------|---------------|
| AuthService | Login, inscription, génération JWT |
| UserService | CRUD utilisateurs, profil, changement mot de passe, stats admin |
| ClaimService | CRUD sinistres, workflow statut, qualification, indemnisation, assignation, archivage |
| ClaimHistoryService | Historique des actions sur sinistres |
| ExpertiseService | CRUD expertises, soumission rapport, validation/refus, brouillons |
| AnalyseIAService | Analyse IA via Ollama, scores, recommandations, stats |
| FraudAlertService | Signalement fraude, résolution admin, stats fraude |
| ReimbursementService | Calcul indemnisation, workflow remboursement, validation/refus assuré |
| StripeService | Sessions Stripe Checkout, vérification paiement, webhook |
| PdfGenerationService | Génération PDF lettres de remboursement et de rejet |
| MessageService | Messagerie, conversations, envoi de messages |
| NotificationService | Notifications in-app, marquage lu, compteur non lues |
| TicketService | Tickets support, messages, assignation, résolution |
| FileStorageService | Upload/download/suppression de fichiers |
| AdminService | Analytics, audit logs, charge de travail |
| CustomUserDetailsService | Implémentation UserDetailsService pour Spring Security |

---

## 6. Architecture Frontend

### 6.1 Structure des modules

```
src/app/
├── auth/            → Connexion, Inscription
├── assure/          → Espace Assuré (layout + 9 pages)
├── gestionnaire/    → Espace Gestionnaire (layout + 11 pages)
├── expert/          → Espace Expert (layout + 5 pages)
├── admin/           → Espace Admin (layout + 7 pages)
├── core/            → 14 services + auth guard + interceptor
└── shared/          → Composants partagés (notification-bell, helpers)
```

### 6.2 Routage par rôle

**Assuré** `/assure`:
- tableau-de-bord, mes-sinistres, declarer-sinistre, detail-sinistre/:id
- remboursements, messagerie, faq, support, profil

**Gestionnaire** `/gestionnaire`:
- tableau-de-bord, dossiers-assignes, tous-sinistres, messagerie
- remboursements, support, analyse-ia, approbation, rapports
- signalement-fraude, profil

**Expert** `/expert`:
- tableau-de-bord, dossiers, rapport-expertise, messagerie, profil

**Admin** `/admin`:
- tableau-de-bord, utilisateurs, experts, rapports-analytics
- monitoring-ia, gestion-technique, surveillance-fraude

### 6.3 Services Frontend (core/)

| Service | Rôle |
|---------|------|
| auth.service | Auth HTTP + stockage JWT |
| auth.guard | Protection routes par rôle |
| auth.interceptor | Injection JWT dans headers HTTP |
| claim.service | Appels API sinistres |
| expertise.service | Appels API expertises |
| analyse-ia.service | Appels API analyses IA |
| fraud-alert.service | Appels API fraude |
| reimbursement.service | Appels API remboursements |
| message.service | Appels API messagerie |
| notification.service | Appels API notifications |
| ticket.service | Appels API tickets |
| user.service | Appels API utilisateurs |
| admin.service | Appels API admin |
| file.service | Upload/download fichiers |

---

## 7. Sécurité

- **Authentification**: JWT (24h expiration), BCrypt password encoding
- **Autorisation**: Spring Security + @PreAuthorize par rôle
- **CORS**: Autorisé pour localhost:4200 et localhost
- **Filtre**: JwtAuthFilter avant UsernamePasswordAuthenticationFilter
- **Endpoints publics**: /api/auth/**, /stripe/webhook, /error

---

## 8. Diagrammes

### 8.1 Diagramme de Classes UML (Mermaid)

```mermaid
classDiagram
    class User {
        +String id prenom nom email password telephone
        +Role role
        +String specialite zoneIntervention
        +double notePerformance
        +int chargeMax
        +boolean enabled
    }
    class Role { <<enumeration>> ASSURE GESTIONNAIRE EXPERT ADMIN }
    class Claim {
        +String id reference assureId categorie type
        +String description dateSinistre lieu
        +GraviteSinistre gravite
        +StatutSinistre statut
        +Double montantIndemnisationPropose montantIndemnisationFinal
        +String gestionnaireId expertId analyseIAId
    }
    class StatutSinistre { <<enumeration>> EN_COURS EN_REVISION EXPERTISE VALIDE REFUSE INDEMNISATION_PROPOSEE INDEMNISATION_ACCEPTEE PAIEMENT_EN_COURS RECOURS CLOTURE ARCHIVE }
    class ClaimHistory { +String id claimId action description utilisateurId ancienStatut nouveauStatut }
    class Expertise { +String id claimId expertId gestionnaireId conclusion montantEstime recommandation +StatutExpertise statut }
    class StatutExpertise { <<enumeration>> EN_ATTENTE EN_COURS SOUMISE VALIDEE REFUSEE }
    class AnalyseIA { +String id claimId +int scoreComplexite scoreRisque scoreConfiance +Severite severite +boolean necessiteExpertHumain +String recommandation resumeAnalyse elementsFraude }
    class FraudAlert { +String id claimId signalePar motif +NiveauRisque niveauRisque +StatutAlerte statut +String resoluPar decision }
    class Reimbursement { +String id claimId assureId reference +double montantDegats franchise tauxRemboursement montantIndemnisationCalcule montantPropose montantFinal +StatutRemboursement statut +String stripeSessionId }
    class Conversation { +String id participant1Id participant2Id claimId dernierMessage }
    class Message { +String id conversationId expediteurId contenu +boolean lu }
    class Notification { +String id utilisateurId titre message type claimId +boolean lu }
    class Ticket { +String id assureId claimId sujet description +StatutTicket statut +String assigneA }

    User "1" --> "*" Claim : assureId/gestionnaireId/expertId
    User "1" --> "*" Expertise : expertId/gestionnaireId
    User "1" --> "*" Notification : utilisateurId
    User "1" --> "*" Conversation : participant
    User "1" --> "*" Message : expediteurId
    User "1" --> "*" Ticket : assureId
    User "1" --> "*" Reimbursement : assureId
    User "1" --> "*" FraudAlert : signalePar/resoluPar
    Claim "1" --> "*" ClaimHistory : claimId
    Claim "1" --> "0..1" AnalyseIA : analyseIAId
    Claim "1" --> "*" Expertise : claimId
    Claim "1" --> "*" Reimbursement : claimId
    Claim "1" --> "0..1" FraudAlert : claimId
    Claim "1" --> "0..1" Conversation : claimId
    Conversation "1" --> "*" Message : conversationId
    Claim --> StatutSinistre
    User --> Role
```

### 8.2 Diagramme ER — Base de Données (Mermaid)

```mermaid
erDiagram
    users {
        String id PK
        String prenom nom
        String email UK
        String password
        String telephone
        String role
        String specialite
        String zoneIntervention
        double notePerformance
        boolean enabled
    }
    claims {
        String id PK
        String reference UK
        String assureId FK
        String categorie type
        String description
        String dateSinistre lieu
        String gravite
        String statut
        Double montantIndemnisationPropose
        Double montantIndemnisationFinal
        String gestionnaireId FK
        String expertId FK
        String analyseIAId FK
    }
    claim_history {
        String id PK
        String claimId FK
        String action
        String utilisateurId FK
        String ancienStatut nouveauStatut
    }
    expertises {
        String id PK
        String claimId FK
        String expertId FK
        String gestionnaireId FK
        String conclusion montantEstime recommandation
        String statut
    }
    analyses_ia {
        String id PK
        String claimId FK
        int scoreComplexite scoreRisque scoreConfiance
        Double montantEstime
        String severite
        boolean necessiteExpertHumain
        String recommandation resumeAnalyse
        String typeAnalyse statut
    }
    fraud_alerts {
        String id PK
        String claimId FK
        String signalePar FK
        String motif description
        String niveauRisque statut
        String resoluPar FK
        String decision
    }
    reimbursements {
        String id PK
        String claimId FK
        String assureId FK
        String reference UK
        double montantDegats capitalAssure franchise
        double tauxRemboursement montantIndemnisationCalcule
        double montantPropose montantFinal
        String stripeSessionId
        String statut
    }
    conversations {
        String id PK
        String participant1Id FK
        String participant2Id FK
        String claimId FK
        String dernierMessage
    }
    messages {
        String id PK
        String conversationId FK
        String expediteurId FK
        String contenu
        boolean lu
    }
    notifications {
        String id PK
        String utilisateurId FK
        String titre message type
        String claimId FK
        boolean lu
    }
    tickets {
        String id PK
        String assureId FK
        String claimId FK
        String sujet description categorie
        String statut
        String assigneA FK
    }

    users ||--o{ claims : assureId
    users ||--o{ claims : gestionnaireId
    users ||--o{ claims : expertId
    users ||--o{ expertises : expertId
    users ||--o{ expertises : gestionnaireId
    users ||--o{ notifications : utilisateurId
    users ||--o{ conversations : participant
    users ||--o{ messages : expediteurId
    users ||--o{ tickets : assureId
    users ||--o{ reimbursements : assureId
    users ||--o{ fraud_alerts : signalePar
    claims ||--o{ claim_history : claimId
    claims ||--o| analyses_ia : analyseIAId
    claims ||--o{ expertises : claimId
    claims ||--o{ reimbursements : claimId
    claims ||--o| fraud_alerts : claimId
    claims ||--o| conversations : claimId
    conversations ||--o{ messages : conversationId
```

### 8.3 Diagramme d'Architecture Système (Mermaid)

```mermaid
graph TB
    subgraph Client ["Client (Navigateur)"]
        UI["Angular 17+ SPA<br/>Standalone Components"]
    end

    subgraph Frontend ["Frontend Container (Nginx :80)"]
        NGINX["Nginx — Sert le build Angular"]
    end

    subgraph Backend ["Backend Container (Spring Boot :8080)"]
        API["REST API — 14 Controllers"]
        SEC["Spring Security + JWT"]
        SVC["16 Services métier"]
        REPO["11 Repositories MongoDB"]
    end

    subgraph External ["Services Externes"]
        OLLAMA["Ollama LLM (llama3)<br/>:11434"]
        STRIPE["Stripe API<br/>Checkout + Webhooks"]
    end

    subgraph Data ["Données"]
        MONGO["MongoDB 8<br/>:27017 — 11 collections"]
        FILES["File Storage<br/>/app/uploads"]
    end

    UI -->|HTTP| NGINX
    NGINX -->|REST API| API
    API --> SEC
    SEC --> SVC
    SVC --> REPO
    REPO --> MONGO
    SVC -->|Analyse IA| OLLAMA
    SVC -->|Paiement| STRIPE
    STRIPE -->|Webhook| API
    SVC -->|Fichiers| FILES
```

### 8.4 Workflow Sinistre — Cycle de Vie (Mermaid)

```mermaid
stateDiagram-v2
    [*] --> EN_COURS : Assuré déclare sinistre
    EN_COURS --> EN_REVISION : Gestionnaire prend en charge
    EN_REVISION --> EXPERTISE : Expert assigné
    EN_REVISION --> VALIDE : Qualification favorable
    EN_REVISION --> REFUSE : Qualification défavorable
    EXPERTISE --> INDEMNISATION_PROPOSEE : Expertise validée
    VALIDE --> INDEMNISATION_PROPOSEE : Proposition indemnisation
    INDEMNISATION_PROPOSEE --> INDEMNISATION_ACCEPTEE : Assuré accepte
    INDEMNISATION_PROPOSEE --> RECOURS : Assuré refuse
    INDEMNISATION_ACCEPTEE --> PAIEMENT_EN_COURS : Paiement initié
    PAIEMENT_EN_COURS --> CLOTURE : Paiement confirmé
    RECOURS --> CLOTURE : Résolution litige
    REFUSE --> CLOTURE : Clôture administrative
    CLOTURE --> ARCHIVE : Archivage
    EN_COURS --> REFUSE : Refus direct
```

### 8.5 Workflow Remboursement (Mermaid)

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE : Création remboursement
    EN_ATTENTE --> VALIDEE : Assuré valide
    EN_ATTENTE --> REFUSE : Assuré refuse
    VALIDEE --> EN_COURS_TRAITEMENT : Traitement Stripe
    EN_COURS_TRAITEMENT --> PAYE : Paiement confirmé
    EN_COURS_TRAITEMENT --> REFUSE : Échec paiement
```

---

## 9. Diagrammes de Séquence par Sprint

### Sprint 1 — Authentification & Gestion des Utilisateurs

**Objectif**: Mise en place de l'authentification JWT, inscription, gestion des rôles et profils utilisateurs.

```mermaid
sequenceDiagram
    actor Assure
    participant Frontend as Angular (Frontend)
    participant Backend as Spring Boot (Backend)
    participant DB as MongoDB
    participant SMTP as Service Email

    Note over Assure,SMTP: Inscription
    Assure->>Frontend: Remplit formulaire inscription
    Frontend->>Backend: POST /api/auth/register {nom, prenom, email, password, role}
    Backend->>DB: Vérifie email unique
    DB-->>Backend: Email disponible
    Backend->>Backend: BCrypt.encode(password)
    Backend->>DB: INSERT User {role: ASSURE, enabled: true}
    DB-->>Backend: User créé
    Backend->>Backend: generateJWT(user)
    Backend-->>Frontend: 200 {token, user}
    Frontend-->>Assure: Redirection dashboard

    Note over Assure,SMTP: Connexion
    Assure->>Frontend: Saisit email + password
    Frontend->>Backend: POST /api/auth/login {email, password}
    Backend->>DB: findByEmail(email)
    DB-->>Backend: User
    Backend->>Backend: BCrypt.matches(password, user.password)
    Backend->>Backend: generateJWT(user)
    Backend-->>Frontend: 200 {token, user}
    Frontend->>Frontend: localStorage.setItem('token', token)
    Frontend-->>Assure: Redirection selon rôle

    Note over Assure,SMTP: Admin — Créer utilisateur
    actor Admin
    Admin->>Frontend: Formulaire création utilisateur
    Frontend->>Backend: POST /api/users {nom, prenom, email, password, role}
    Backend->>Backend: Vérifie @PreAuthorize(ADMIN)
    Backend->>DB: INSERT User
    Backend-->>Frontend: 201 User créé
    Frontend-->>Admin: Utilisateur ajouté
```

### Sprint 2 — Déclaration & Gestion des Sinistres

**Objectif**: Permettre aux assurés de déclarer des sinistres, aux gestionnaires de les qualifier et d'assigner des experts.

```mermaid
sequenceDiagram
    actor Assure
    participant Frontend as Angular
    participant Backend as Spring Boot
    participant DB as MongoDB
    actor Gestionnaire
    actor Expert

    Note over Assure,Expert: Déclaration de sinistre
    Assure->>Frontend: Remplit formulaire sinistre
    Frontend->>Backend: POST /api/files/upload {piecesJointes}
    Backend->>DB: Stocke fichiers dans /uploads
    Backend-->>Frontend: 200 [filenames]
    Frontend->>Backend: POST /api/claims {categorie, description, dateSinistre, lieu, piecesJointes, estimation}
    Backend->>Backend: Vérifie @PreAuthorize(ASSURE, ADMIN)
    Backend->>Backend: Génère référence SIN-2025-XXXX
    Backend->>DB: INSERT Claim {statut: EN_COURS, assureId}
    Backend->>DB: INSERT ClaimHistory {action: CREATION}
    Backend-->>Frontend: 201 Claim
    Frontend-->>Assure: Sinistre créé

    Note over Assure,Expert: Prise en charge par gestionnaire
    Gestionnaire->>Frontend: Clique "Prendre en charge"
    Frontend->>Backend: PATCH /api/claims/{id}/prendre-en-charge
    Backend->>DB: UPDATE Claim {gestionnaireId, statut: EN_REVISION}
    Backend->>DB: INSERT ClaimHistory {action: PRISE_EN_CHARGE}
    Backend->>DB: INSERT Notification {utilisateurId: assureId, type: STATUT_CHANGE}
    Backend-->>Frontend: 200 Claim mis à jour
    Frontend-->>Gestionnaire: Dossier assigné

    Note over Assure,Expert: Qualification du sinistre
    Gestionnaire->>Frontend: Qualifie sinistre (gravité, franchise, plafond)
    Frontend->>Backend: PATCH /api/claims/{id}/qualifier {gravite, franchise, plafondCouverture}
    Backend->>DB: UPDATE Claim {gravite, franchise, plafondCouverture}
    Backend->>DB: INSERT ClaimHistory {action: QUALIFICATION}
    Backend-->>Frontend: 200

    Note over Assure,Expert: Assignation expert
    Gestionnaire->>Frontend: Sélectionne expert
    Frontend->>Backend: PATCH /api/claims/{id}/assign-expert {expertId}
    Backend->>DB: UPDATE Claim {expertId, statut: EXPERTISE}
    Backend->>DB: INSERT ClaimHistory {action: ASSIGNATION_EXPERT}
    Backend->>DB: INSERT Notification {utilisateurId: expertId, type: EXPERTISE}
    Backend-->>Frontend: 200
    Frontend-->>Expert: Notification reçue
```

### Sprint 3 — Expertise & Analyse IA

**Objectif**: Intégrer l'analyse IA via Ollama et la gestion des rapports d'expertise.

```mermaid
sequenceDiagram
    actor Gestionnaire
    participant Frontend as Angular
    participant Backend as Spring Boot
    participant DB as MongoDB
    participant Ollama as Ollama (LLM)
    actor Expert

    Note over Gestionnaire,Expert: Lancement analyse IA
    Gestionnaire->>Frontend: Clique "Analyser avec IA"
    Frontend->>Backend: POST /api/analyses-ia {claimId}
    Backend->>DB: findById(claimId)
    DB-->>Backend: Claim
    Backend->>Backend: Construit prompt (description, catégorie, estimation)
    Backend->>Ollama: POST /api/chat {model: llama3, prompt}
    Ollama-->>Backend: Réponse IA (JSON)
    Backend->>Backend: Parse réponse → scores, recommandation, sévérité
    Backend->>DB: INSERT AnalyseIA {scoreComplexite, scoreRisque, scoreConfiance, severite, necessiteExpertHumain, recommandation}
    Backend->>DB: UPDATE Claim {analyseIAId}
    Backend-->>Frontend: 200 AnalyseIA
    Frontend-->>Gestionnaire: Résultats analyse affichés

    Note over Gestionnaire,Expert: Création demande expertise
    Gestionnaire->>Frontend: Clique "Demander expertise"
    Frontend->>Backend: POST /api/expertises {claimId, expertId}
    Backend->>DB: INSERT Expertise {statut: EN_ATTENTE, claimId, expertId, gestionnaireId}
    Backend->>DB: INSERT Notification {utilisateurId: expertId, type: EXPERTISE}
    Backend-->>Frontend: 201 Expertise créée
    Frontend-->>Expert: Notification reçue

    Note over Gestionnaire,Expert: Expert rédige rapport
    Expert->>Frontend: Rédige rapport d'expertise
    Frontend->>Backend: PUT /api/expertises/{id}/rapport {conclusion, montantEstime, recommandation, piecesJointes}
    Backend->>DB: UPDATE Expertise {statut: SOUMISE, conclusion, montantEstime, recommandation}
    Backend->>DB: INSERT Notification {utilisateurId: gestionnaireId, type: EXPERTISE}
    Backend-->>Frontend: 200
    Frontend-->>Gestionnaire: Notification rapport soumis

    Note over Gestionnaire,Expert: Validation / Refus expertise
    Gestionnaire->>Frontend: Valide ou refuse
    alt Validation
        Frontend->>Backend: PATCH /api/expertises/{id}/valider
        Backend->>DB: UPDATE Expertise {statut: VALIDEE}
        Backend-->>Frontend: 200
    else Refus
        Frontend->>Backend: PATCH /api/expertises/{id}/refuser
        Backend->>DB: UPDATE Expertise {statut: REFUSEE}
        Backend-->>Frontend: 200
    end
```

### Sprint 4 — Indemnisation & Remboursement (Stripe)

**Objectif**: Calcul d'indemnisation, proposition, validation par l'assuré, et paiement via Stripe.

```mermaid
sequenceDiagram
    actor Gestionnaire
    participant Frontend as Angular
    participant Backend as Spring Boot
    participant DB as MongoDB
    participant Stripe as Stripe API
    actor Assure

    Note over Gestionnaire,Assure: Calcul indemnisation
    Gestionnaire->>Frontend: Clique "Calculer indemnisation"
    Frontend->>Backend: POST /api/reimbursements/calculer {claimId, montantDegats, capitalAssure}
    Backend->>DB: findById(claimId)
    DB-->>Backend: Claim (franchise, plafond, taux)
    Backend->>Backend: Calcule: montantApresFranchise, montantIndemnisationCalcule
    Backend-->>Frontend: 200 {détail calcul}
    Frontend-->>Gestionnaire: Détail calcul affiché

    Note over Gestionnaire,Assure: Création remboursement
    Gestionnaire->>Frontend: Valide et crée remboursement
    Frontend->>Backend: POST /api/reimbursements {claimId, assureId, ...montants}
    Backend->>Backend: Génère référence REM-2025-XXXX
    Backend->>DB: INSERT Reimbursement {statut: EN_ATTENTE}
    Backend->>DB: UPDATE Claim {statut: INDEMNISATION_PROPOSEE}
    Backend->>DB: INSERT Notification {utilisateurId: assureId, type: REMBOURSEMENT}
    Backend-->>Frontend: 201 Reimbursement
    Frontend-->>Assure: Notification reçue

    Note over Gestionnaire,Assure: Assuré valide ou refuse
    Assure->>Frontend: Consulte proposition
    Frontend->>Backend: GET /api/reimbursements/{id}/proposition
    Backend-->>Frontend: Détail complet
    alt Assuré accepte
        Assure->>Frontend: Clique "Accepter"
        Frontend->>Backend: PUT /api/reimbursements/{id}/validate
        Backend->>DB: UPDATE Reimbursement {statut: VALIDEE}
        Backend-->>Frontend: 200
    else Assuré refuse
        Assure->>Frontend: Clique "Refuser"
        Frontend->>Backend: PUT /api/reimbursements/{id}/refuse
        Backend->>DB: UPDATE Reimbursement {statut: REFUSE}
        Backend-->>Frontend: 200
    end

    Note over Gestionnaire,Assure: Paiement via Stripe
    Gestionnaire->>Frontend: Initie paiement
    Frontend->>Backend: POST /api/reimbursements/{id}/stripe/checkout
    Backend->>Stripe: Crée Checkout Session
    Stripe-->>Backend: sessionId + paymentUrl
    Backend->>DB: UPDATE Reimbursement {stripeSessionId, statut: EN_COURS_TRAITEMENT}
    Backend-->>Frontend: 200 {checkoutUrl}
    Frontend-->>Assure: Redirection Stripe Checkout

    Assure->>Stripe: Saisit carte bancaire
    Stripe->>Backend: POST /api/reimbursements/stripe/webhook {event: checkout.completed}
    Backend->>Stripe: Vérifie signature webhook
    Backend->>DB: UPDATE Reimbursement {stripePaymentIntentId, statut: PAYE}
    Backend->>DB: UPDATE Claim {statut: PAIEMENT_EN_COURS}
    Backend->>DB: INSERT Notification {utilisateurId: assureId, type: REMBOURSEMENT}
    Backend-->>Stripe: 200 OK

    Note over Gestionnaire,Assure: Génération PDF
    Gestionnaire->>Frontend: Demande lettre remboursement
    Frontend->>Backend: GET /api/reimbursements/{id}/lettre-remboursement
    Backend->>Backend: Génère PDF (détail calcul, référence, montant)
    Backend-->>Frontend: PDF téléchargeable
```

### Sprint 5 — Détection de Fraude & Messagerie

**Objectif**: Signalement de fraude, résolution admin, système de messagerie temps réel, notifications.

```mermaid
sequenceDiagram
    actor Gestionnaire
    participant Frontend as Angular
    participant Backend as Spring Boot
    participant DB as MongoDB
    actor Admin
    actor Assure

    Note over Gestionnaire,Assure: Signalement fraude
    Gestionnaire->>Frontend: Signale sinistre suspect
    Frontend->>Backend: POST /api/fraud-alerts {claimId, motif, description, niveauRisque}
    Backend->>DB: INSERT FraudAlert {statut: SOUMISE, signalePar: gestionnaireId}
    Backend->>DB: INSERT Notification {utilisateurId: adminId, type: SYSTEME}
    Backend-->>Frontend: 201 FraudAlert
    Frontend-->>Admin: Notification reçue

    Note over Gestionnaire,Assure: Admin analyse alerte
    Admin->>Frontend: Consulte alertes en attente
    Frontend->>Backend: GET /api/fraud-alerts/en-attente
    Backend->>DB: findByStatut(SOUMISE)
    DB-->>Backend: Liste alertes
    Backend-->>Frontend: 200 [FraudAlerts]
    Admin->>Frontend: Met en analyse
    Frontend->>Backend: PATCH /api/fraud-alerts/{id}/analyser
    Backend->>DB: UPDATE FraudAlert {statut: EN_COURS_ANALYSE}
    Backend-->>Frontend: 200

    Note over Gestionnaire,Assure: Résolution fraude
    Admin->>Frontend: Résout alerte (confirmée/infondée)
    Frontend->>Backend: PATCH /api/fraud-alerts/{id}/resoudre {decision, notesResolution}
    Backend->>DB: UPDATE FraudAlert {statut: CONFIRMEE/INFONDEE, resoluPar, decision}
    Backend->>DB: INSERT Notification {utilisateurId: gestionnaireId}
    Backend-->>Frontend: 200

    Note over Gestionnaire,Assure: Messagerie — Envoi message
    Gestionnaire->>Frontend: Envoie message à l'assuré
    Frontend->>Backend: POST /api/messages {destinataireId, contenu, claimId}
    Backend->>DB: Trouve ou crée Conversation {participant1Id, participant2Id, claimId}
    Backend->>DB: INSERT Message {conversationId, expediteurId, contenu}
    Backend->>DB: UPDATE Conversation {dernierMessage, messagesNonLus++}
    Backend->>DB: INSERT Notification {utilisateurId: destinataireId, type: MESSAGE}
    Backend-->>Frontend: 201 Message
    Frontend-->>Assure: Notification message

    Note over Gestionnaire,Assure: Consultation conversation
    Assure->>Frontend: Ouvre messagerie
    Frontend->>Backend: GET /api/messages/conversations
    Backend->>DB: findByParticipant1IdOrParticipant2Id(userId)
    DB-->>Backend: Liste conversations
    Backend-->>Frontend: 200 [Conversations]
    Assure->>Frontend: Sélectionne conversation
    Frontend->>Backend: GET /api/messages/conversations/{id}
    Backend->>DB: findByConversationId(id)
    Backend->>DB: UPDATE Message {lu: true} (messages de l'autre)
    Backend-->>Frontend: 200 [Messages]
```

### Sprint 6 — Administration, Rapports & Support

**Objectif**: Dashboard admin, analytics, export CSV, tickets support, monitoring IA.

```mermaid
sequenceDiagram
    actor Admin
    participant Frontend as Angular
    participant Backend as Spring Boot
    participant DB as MongoDB
    actor Assure
    actor Gestionnaire

    Note over Admin,Gestionnaire: Dashboard Admin — Analytics
    Admin->>Frontend: Accède dashboard admin
    Frontend->>Backend: GET /api/admin/analytics
    Backend->>DB: Aggrège: count users, claims by statut, reimbursements total
    DB-->>Backend: Résultats agrégés
    Backend-->>Frontend: 200 {stats}
    Frontend-->>Admin: Dashboard affiché

    Note over Admin,Gestionnaire: Audit Logs
    Admin->>Frontend: Consulte journaux audit
    Frontend->>Backend: GET /api/admin/audit-logs
    Backend->>DB: ClaimHistory.findAll() + tri date
    DB-->>Backend: Liste actions
    Backend-->>Frontend: 200 [AuditLogs]
    Frontend-->>Admin: Journaux affichés

    Note over Admin,Gestionnaire: Charge de travail
    Admin->>Frontend: Consulte charge équipe
    Frontend->>Backend: GET /api/admin/workload
    Backend->>DB: Aggrège: dossiers par gestionnaire, expertises par expert
    DB-->>Backend: Données charge
    Backend-->>Frontend: 200 {workload}
    Frontend-->>Admin: Répartition affichée

    Note over Admin,Gestionnaire: Export CSV — Sinistres
    Admin->>Frontend: Exporte sinistres CSV
    Frontend->>Backend: GET /api/reports/claims/csv?statut=&dateDebut=&dateFin=
    Backend->>DB: findClaims with filters
    DB-->>Backend: Liste claims
    Backend->>Backend: Génère CSV (référence, assuré, statut, montant, dates)
    Backend-->>Frontend: CSV download
    Frontend-->>Admin: Fichier téléchargé

    Note over Admin,Gestionnaire: Export CSV — Remboursements
    Admin->>Frontend: Exporte remboursements CSV
    Frontend->>Backend: GET /api/reports/reimbursements/csv?statut=&dateDebut=&dateFin=
    Backend->>DB: findReimbursements with filters
    DB-->>Backend: Liste remboursements
    Backend->>Backend: Génère CSV
    Backend-->>Frontend: CSV download

    Note over Admin,Gestionnaire: Ticket Support — Création
    Assure->>Frontend: Crée ticket support
    Frontend->>Backend: POST /api/tickets {sujet, description, categorie, claimId}
    Backend->>DB: INSERT Ticket {statut: OUVERT, assureId}
    Backend->>DB: INSERT Notification {utilisateurId: gestionnaireId, type: SYSTEME}
    Backend-->>Frontend: 201 Ticket
    Frontend-->>Assure: Ticket créé

    Note over Admin,Gestionnaire: Ticket — Assignation & Résolution
    Gestionnaire->>Frontend: Prend en charge ticket
    Frontend->>Backend: PUT /api/tickets/{id}/assign {assigneA}
    Backend->>DB: UPDATE Ticket {statut: EN_COURS, assigneA}
    Backend-->>Frontend: 200
    Gestionnaire->>Frontend: Ajoute message
    Frontend->>Backend: POST /api/tickets/{id}/messages {contenu}
    Backend->>DB: INSERT TicketMessage (embedded)
    Backend-->>Frontend: 200
    Gestionnaire->>Frontend: Résout ticket
    Frontend->>Backend: PUT /api/tickets/{id}/resolve
    Backend->>DB: UPDATE Ticket {statut: RESOLU}
    Backend-->>Frontend: 200

    Note over Admin,Gestionnaire: Monitoring IA
    Admin->>Frontend: Consulte stats IA
    Frontend->>Backend: GET /api/analyses-ia/stats
    Backend->>DB: Aggrège: analyses par statut, taux expert requis, coût moyen
    DB-->>Backend: Stats IA
    Backend-->>Frontend: 200 {stats}
    Frontend-->>Admin: Monitoring IA affiché
```

---

## 10. Architecture Détaillée du Système

### 10.1 Vue d'ensemble des couches

```mermaid
graph TB
    subgraph Navigateur ["🌐 Navigateur Client"]
        direction LR
        SPA["Angular 17+ SPA<br/>Standalone Components"]
        AUTH_GUARD["Auth Guard<br/>Route Protection"]
        HTTP_INT["HTTP Interceptor<br/>JWT Injection"]
        STATE["State Management<br/>Services + RxJS"]
    end

    subgraph Nginx ["🖥️ Nginx (Container :80)"]
        STATIC["Static Files<br/>dist/larchitecte-claims"]
        PROXY["Reverse Proxy<br/>/api → Backend"]
    end

    subgraph SpringBoot ["⚙️ Spring Boot (Container :8080)"]
        direction TB
        subgraph Security ["🔐 Security Layer"]
            JWT_FILTER["JwtAuthFilter<br/>Token Validation"]
            SECURITY_CTX["SecurityContext<br/>Authentication"]
            PRE_AUTH["@PreAuthorize<br/>Role-Based Access"]
        end

        subgraph Controllers ["📋 REST Controllers (14)"]
            AUTH_C["AuthController<br/>/api/auth"]
            CLAIM_C["ClaimController<br/>/api/claims"]
            EXPERT_C["ExpertiseController<br/>/api/expertises"]
            AI_C["AnalyseIAController<br/>/api/analyses-ia"]
            FRAUD_C["FraudAlertController<br/>/api/fraud-alerts"]
            REIMB_C["ReimbursementController<br/>/api/reimbursements"]
            MSG_C["MessageController<br/>/api/messages"]
            NOTIF_C["NotificationController<br/>/api/notifications"]
            TICKET_C["TicketController<br/>/api/tickets"]
            USER_C["UserController<br/>/api/users"]
            ADMIN_C["AdminController<br/>/api/admin"]
            FILE_C["FileController<br/>/api/files"]
            REPORT_C["ReportController<br/>/api/reports"]
            HIST_C["ClaimHistoryController<br/>/api/claims/{id}/history"]
        end

        subgraph Services ["🔧 Business Services (16)"]
            AUTH_S["AuthService<br/>JWT + BCrypt"]
            CLAIM_S["ClaimService<br/>Workflow Sinistre"]
            EXPERT_S["ExpertiseService<br/>Rapports"]
            AI_S["AnalyseIAService<br/>Ollama Integration"]
            FRAUD_S["FraudAlertService<br/>Détection Fraude"]
            REIMB_S["ReimbursementService<br/>Calcul Indemnisation"]
            STRIPE_S["StripeService<br/>Checkout + Webhooks"]
            PDF_S["PdfGenerationService<br/>Lettres PDF"]
            MSG_S["MessageService<br/>Messagerie"]
            NOTIF_S["NotificationService<br/>Notifications"]
            TICKET_S["TicketService<br/>Support"]
            USER_S["UserService<br/>CRUD Users"]
            ADMIN_S["AdminService<br/>Analytics"]
            FILE_S["FileStorageService<br/>Upload/Download"]
            HIST_S["ClaimHistoryService<br/>Audit Trail"]
            USERDET_S["CustomUserDetailsService<br/>Spring Security"]
        end

        subgraph Repos ["📦 Repositories (11)"]
            USER_R["UserRepository"]
            CLAIM_R["ClaimRepository"]
            EXPERT_R["ExpertiseRepository"]
            AI_R["AnalyseIARepository"]
            FRAUD_R["FraudAlertRepository"]
            REIMB_R["ReimbursementRepository"]
            CONV_R["ConversationRepository"]
            MSG_R["MessageRepository"]
            NOTIF_R["NotificationRepository"]
            TICKET_R["TicketRepository"]
            HIST_R["ClaimHistoryRepository"]
        end
    end

    subgraph Externals ["🔗 Services Externes"]
        OLLAMA["Ollama LLM<br/>llama3<br/>:11434"]
        STRIPE["Stripe API<br/>Checkout Sessions<br/>Payment Intents<br/>Webhooks"]
    end

    subgraph DataLayer ["💾 Couche Données"]
        MONGO["MongoDB 8<br/>:27017"]
        VOL["Docker Volume<br/>mongo-data"]
        UPLOADS["File Storage<br/>/app/uploads"]
        UP_VOL["Docker Volume<br/>uploads-data"]
    end

    SPA --> AUTH_GUARD
    SPA --> HTTP_INT
    SPA --> STATE
    SPA -->|HTTP Requests| Nginx
    Nginx --> STATIC
    Nginx -->|/api/*| SpringBoot

    JWT_FILTER --> SECURITY_CTX
    SECURITY_CTX --> PRE_AUTH
    PRE_AUTH --> Controllers

    Controllers --> Services
    Services --> Repos
    Repos --> MONGO
    MONGO --> VOL

    AI_S -->|HTTP| OLLAMA
    STRIPE_S -->|HTTP| STRIPE
    STRIPE -->|Webhook POST| REIMB_C
    FILE_S --> UPLOADS
    UPLOADS --> UP_VOL

    style Navigateur fill:#e3f2fd
    style Nginx fill:#e8f5e9
    style Security fill:#ffebee
    style Controllers fill:#fff3e0
    style Services fill:#f3e5f5
    style Repos fill:#e0f2f1
    style Externals fill:#fce4ec
    style DataLayer fill:#efebe9
```

### 10.2 Flux d'Authentification JWT

```mermaid
sequenceDiagram
    participant Client as Navigateur
    participant Nginx as Nginx
    participant Filter as JwtAuthFilter
    participant Security as SecurityContext
    participant Controller as Controller
    participant Service as Service
    participant DB as MongoDB

    Client->>Nginx: Requête HTTP + Authorization: Bearer <token>
    Nginx->>Filter: Forward request

    alt Token présent
        Filter->>Filter: extractToken(request)
        Filter->>Filter: jwtUtil.validateToken(token)
        alt Token valide
            Filter->>Filter: jwtUtil.extractUsername(token) → email
            Filter->>Service: userDetailsService.loadUserByUsername(email)
            Service->>DB: findByEmail(email)
            DB-->>Service: User
            Service-->>Filter: UserDetails
            Filter->>Security: SecurityContextHolder.setAuthentication(auth)
            Security-->>Controller: Authenticated Principal
            Controller->>Service: Appel métier
            Service-->>Controller: Résultat
            Controller-->>Nginx: 200 Response
            Nginx-->>Client: JSON Response
        else Token expiré/invalide
            Filter-->>Nginx: 401 Unauthorized
            Nginx-->>Client: 401 → Redirect Login
        end
    else Pas de token (endpoint public)
        Filter->>Controller: Continue sans auth
        Controller-->>Nginx: Response
        Nginx-->>Client: Response
    end
```

### 10.3 Architecture Docker — Déploiement

```mermaid
graph LR
    subgraph DockerCompose ["🐳 Docker Compose"]
        subgraph Net ["Docker Network (bridge)"]
            FE["frontend<br/>larchitecte-frontend<br/>:80 → :80<br/>Nginx + Angular build"]
            BE["backend<br/>larchitecte-backend<br/>:8081 → :8080<br/>Spring Boot JAR"]
            DB["mongodb<br/>larchitecte-mongodb<br/>:27017<br/>MongoDB 8"]
        end

        V1["📋 mongo-data<br/>(Docker Volume)"]
        V2["📋 uploads-data<br/>(Docker Volume)"]
    end

    USER["👤 Utilisateur"] -->|:80| FE
    FE -->|/api/*| BE
    BE -->|mongodb://mongodb:27017| DB
    BE -->|/app/uploads| V2
    DB -->|/data/db| V1
    BE -->|OLLAMA_API_URL| OLLAMA_EXT["🤖 Ollama<br/>(Host :11434)"]
    BE -->|Stripe API| STRIPE_EXT["💳 Stripe<br/>(Internet)"]

    style DockerCompose fill:#eceff1
    style Net fill:#e8eaf6
    style USER fill:#e1f5fe
    style OLLAMA_EXT fill:#fce4ec
    style STRIPE_EXT fill:#fce4ec
```

### 10.4 Matrice des Rôles par Fonctionnalité

```mermaid
graph LR
    subgraph Fonctionnalités
        F1["🔐 Authentification"]
        F2["📝 Déclaration Sinistre"]
        F3["📋 Qualification Sinistre"]
        F4["🔬 Expertise"]
        F5["🤖 Analyse IA"]
        F6["💰 Indemnisation"]
        F7["💳 Paiement Stripe"]
        F8["🚨 Détection Fraude"]
        F9["💬 Messagerie"]
        F10["🔔 Notifications"]
        F11["🎫 Support Tickets"]
        F12["📊 Admin Dashboard"]
        F13["📄 Rapports CSV"]
        F14["📁 Gestion Fichiers"]
    end

    A["👤 ASSURE"] --> F1
    A --> F2
    A --> F6
    A --> F7
    A --> F9
    A --> F10
    A --> F11
    A --> F14

    G["👔 GESTIONNAIRE"] --> F1
    G --> F3
    G --> F4
    G --> F5
    G --> F6
    G --> F8
    G --> F9
    G --> F10
    G --> F11
    G --> F13
    G --> F14

    E["🔬 EXPERT"] --> F1
    E --> F4
    E --> F9
    E --> F10
    E --> F14

    AD["🔑 ADMIN"] --> F1
    AD --> F2
    AD --> F3
    AD --> F4
    AD --> F5
    AD --> F6
    AD --> F8
    AD --> F9
    AD --> F10
    AD --> F11
    AD --> F12
    AD --> F13
    AD --> F14

    style A fill:#bbdefb
    style G fill:#c8e6c9
    style E fill:#fff9c4
    style AD fill:#ffcdd2
```

---

## 11. Conteneurisation Docker

### 11.1 Vue d'ensemble

L'application est entièrement conteneurisée via **Docker Compose** avec 3 services :

| Service | Image de base | Port (hôte→conteneur) | Rôle |
|---------|---------------|----------------------|------|
| `mongodb` | mongo:8 | 27017:27017 | Base de données NoSQL |
| `backend` | Multi-stage (maven + JRE alpine) | 8081:8080 | API Spring Boot |
| `frontend` | Multi-stage (node + nginx alpine) | 80:80 | SPA Angular + Reverse Proxy |

### 11.2 docker-compose.yml

```yaml
services:
  # ---- MongoDB ----
  mongodb:
    image: mongo:8
    container_name: larchitecte-mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    restart: unless-stopped

  # ---- Backend (Spring Boot) ----
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: larchitecte-backend
    ports:
      - "8081:8080"
    environment:
      - MONGODB_URI=mongodb://mongodb:27017/larchitecte_claims
      - OLLAMA_API_URL=http://host.docker.internal:11434/api/chat
      - FRONTEND_URL=http://localhost
    volumes:
      - uploads-data:/app/uploads
    depends_on:
      - mongodb
    restart: unless-stopped

  # ---- Frontend (Angular + Nginx) ----
  frontend:
    build:
      context: ./front/larchitecte-claims
      dockerfile: Dockerfile
    container_name: larchitecte-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  mongo-data:
  uploads-data:
```

### 11.3 Dockerfiles

#### Backend — Multi-stage (Build + Runtime)

```dockerfile
# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B    # Cache dépendances
COPY src ./src
RUN mvn package -DskipTests -B      # Compilation + packaging JAR

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine   # Image légère JRE only
WORKDIR /app
RUN mkdir -p /app/uploads            # Répertoire upload
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Étape | Image | Description |
|-------|-------|-------------|
| Build | maven:3.9-eclipse-temurin-21 | Compilation Maven, résolution dépendances, package JAR |
| Runtime | eclipse-temurin:21-jre-alpine | Exécution légère (~170MB), JRE uniquement |

#### Frontend — Multi-stage (Build + Nginx)

```dockerfile
# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install                      # Cache node_modules
COPY . .
RUN npm run build                    # ng build → dist/

# ---- Runtime stage ----
FROM nginx:alpine
COPY --from=build /app/dist/larchitecte-claims/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

| Étape | Image | Description |
|-------|-------|-------------|
| Build | node:22-alpine | npm install + ng build (SSR/browser output) |
| Runtime | nginx:alpine | Sert les fichiers statiques + reverse proxy /api/ |

### 11.4 Configuration Nginx (Reverse Proxy)

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # API requests -> backend
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }

    # Angular SPA fallback (client-side routing)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

| Directive | Rôle |
|-----------|------|
| `location /api/` | Proxy inverse vers le backend Spring Boot |
| `proxy_pass http://backend:8080` | Résolution DNS Docker (nom du service) |
| `proxy_read_timeout 300s` | Timeout long pour les analyses IA |
| `try_files $uri $uri/ /index.html` | Fallback SPA — toutes les routes Angular |

### 11.5 Variables d'Environnement

| Variable | Service | Valeur par défaut | Description |
|----------|---------|-------------------|-------------|
| `MONGODB_URI` | backend | mongodb://localhost:27017/larchitecte_claims | URI connexion MongoDB |
| `OLLAMA_API_URL` | backend | http://localhost:11434/api/chat | URL API Ollama (LLM) |
| `FRONTEND_URL` | backend | http://localhost:4200 | URL frontend (CORS) |
| `server.port` | backend | 8080 | Port interne Spring Boot |
| `jwt.secret` | backend | Base64 encoded key | Clé secrète JWT |
| `jwt.expiration` | backend | 86400000 (24h) | Durée validité JWT |
| `stripe.secret-key` | backend | sk_test_... | Clé secrète Stripe |
| `stripe.webhook-secret` | backend | whsec_... | Secret webhook Stripe |
| `ollama.model` | backend | llama3 | Modèle LLM utilisé |
| `ollama.temperature` | backend | 0.3 | Température génération |
| `ollama.max-tokens` | backend | 2000 | Max tokens par analyse |

### 11.6 Volumes Docker

| Volume | Point de montage | Utilisation |
|--------|-----------------|-------------|
| `mongo-data` | /data/db (mongodb) | Persistance des données MongoDB |
| `uploads-data` | /app/uploads (backend) | Fichiers uploadés (pièces jointes) |

### 11.7 Réseau & Communication Inter-Containers

```mermaid
graph LR
    subgraph Docker Network ["Docker Bridge Network"]
        FE["frontend<br/>Container"]
        BE["backend<br/>Container"]
        DB["mongodb<br/>Container"]
    end

    USER["👤 Utilisateur<br/>:80"] --> FE
    FE -->|"proxy_pass<br/>http://backend:8080"| BE
    BE -->|"mongodb://mongodb:27017"| DB
    BE -->|"http://host.docker.internal:11434"| OLLAMA["🤖 Ollama<br/>(Hôte)"]
    BE -->|"api.stripe.com"| STRIPE["💳 Stripe<br/>(Internet)"]

    style Docker Network fill:#e8eaf6
    style USER fill:#e1f5fe
    style OLLAMA fill:#fce4ec
    style STRIPE fill:#fce4ec
```

- **Frontend → Backend**: Nginx proxy via nom DNS Docker `backend:8080`
- **Backend → MongoDB**: Connexion via nom DNS Docker `mongodb:27017`
- **Backend → Ollama**: Via `host.docker.internal` (accès hôte depuis container)
- **Stripe → Backend**: Webhook vers URL publique (nécessite ngrok/tunnel en dev)

### 11.8 Commandes Docker

```bash
# Build et démarrage (tous les services)
docker-compose up --build -d

# Voir les logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Arrêter les services
docker-compose down

# Arrêter + supprimer les volumes (reset données)
docker-compose down -v

# Rebuild un seul service
docker-compose up --build -d backend

# Accéder au shell d'un container
docker exec -it larchitecte-backend sh
docker exec -it larchitecte-mongodb mongosh

# Vérifier l'état des services
docker-compose ps
```

### 11.9 .dockerignore

**Backend** (`backend/.dockerignore`):
```
target/
!.mvn/wrapper/maven-wrapper.jar
*.class
*.log
*.jar
!pom.xml
```

**Frontend** (`front/larchitecte-claims/.dockerignore`):
```
node_modules/
dist/
.angular/
.git/
```

### 11.10 Diagramme Pipeline CI/CD Docker

```mermaid
graph TB
    subgraph Source ["📂 Code Source"]
        SRC_BE["backend/<br/>Spring Boot + Maven"]
        SRC_FE["front/larchitecte-claims/<br/>Angular + npm"]
        DC["docker-compose.yml"]
    end

    subgraph Build ["🔨 Multi-Stage Build"]
        BE_BUILD["Backend Build<br/>maven:3.9 → JAR<br/>temurin:21-jre-alpine → Image"]
        FE_BUILD["Frontend Build<br/>node:22-alpine → ng build<br/>nginx:alpine → Image"]
    end

    subgraph Runtime ["🚀 Containers en cours"]
        BE_RUN["larchitecte-backend<br/>:8080<br/>Spring Boot JAR"]
        FE_RUN["larchitecte-frontend<br/>:80<br/>Nginx + Angular"]
        DB_RUN["larchitecte-mongodb<br/>:27017<br/>MongoDB 8"]
    end

    subgraph Storage ["💾 Volumes Persistants"]
        V_MONGO["mongo-data<br/>/data/db"]
        V_UPLOADS["uploads-data<br/>/app/uploads"]
    end

    SRC_BE --> BE_BUILD
    SRC_FE --> FE_BUILD
    DC --> BE_BUILD
    DC --> FE_BUILD

    BE_BUILD --> BE_RUN
    FE_BUILD --> FE_RUN
    DC -->|image: mongo:8| DB_RUN

    BE_RUN --> V_UPLOADS
    DB_RUN --> V_MONGO

    FE_RUN -->|proxy /api/| BE_RUN
    BE_RUN -->|mongodb://mongodb:27017| DB_RUN

    style Source fill:#e3f2fd
    style Build fill:#fff3e0
    style Runtime fill:#e8f5e9
    style Storage fill:#efebe9
```
