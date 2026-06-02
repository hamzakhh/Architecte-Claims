# Diagrammes UML par Sprint — L'Architecte Claims

---

## Sprint 1 — Authentification & Gestion des Utilisateurs

**Objectif** : Mise en place de l'authentification JWT, inscription, gestion des rôles et profils utilisateurs.

### Diagramme de classes — Sprint 1

```plantuml
@startuml sprint1_classes
skinparam monochrome true
skinparam shadowing false

enum Role {
    ASSURE
    GESTIONNAIRE
    EXPERT
    ADMIN
}

class User {
    - id : String
    - prenom : String
    - nom : String
    - email : String
    - password : String
    - telephone : String
    - role : Role
    - specialite : String
    - zoneIntervention : String
    - notePerformance : double
    - chargeMax : int
    - certifications : List<String>
    - enabled : boolean
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
    + getAuthorities() : Collection<GrantedAuthority>
    + getUsername() : String
    + getFullName() : String
}

interface UserDetails {
    + getUsername() : String
    + getPassword() : String
    + getAuthorities() : Collection<GrantedAuthority>
    + isAccountNonExpired() : boolean
    + isAccountNonLocked() : boolean
    + isCredentialsNonExpired() : boolean
    + isEnabled() : boolean
}

User ..|> UserDetails : implements
User --> Role : a un

@enduml
```

### Diagramme de séquence — Sprint 1

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

---

## Sprint 2 — Déclaration & Gestion des Sinistres

**Objectif** : Permettre aux assurés de déclarer des sinistres, aux gestionnaires de les qualifier et d'assigner des experts.

### Diagramme de classes — Sprint 2

```plantuml
@startuml sprint2_classes
skinparam monochrome true
skinparam shadowing false

enum Role {
    ASSURE
    GESTIONNAIRE
    EXPERT
    ADMIN
}

class User {
    - id : String
    - prenom : String
    - nom : String
    - email : String
    - role : Role
    - specialite : String
    - zoneIntervention : String
}

enum StatutSinistre {
    EN_COURS
    EN_REVISION
    EXPERTISE
    VALIDE
    REFUSE
    INDEMNISATION_PROPOSEE
    INDEMNISATION_ACCEPTEE
    PAIEMENT_EN_COURS
    RECOURS
    CLOTURE
    ARCHIVE
}

enum GraviteSinistre {
    MINEURE
    MODEREE
    MAJEURE
    CRITIQUE
}

class Claim {
    - id : String
    - reference : String
    - assureId : String
    - categorie : String
    - type : String
    - description : String
    - latitude : Double
    - longitude : Double
    - dateSinistre : String
    - heureSinistre : String
    - lieu : String
    - piecesJointes : List<String>
    - estimation : String
    - gravite : GraviteSinistre
    - franchise : Double
    - plafondCouverture : Double
    - statut : StatutSinistre
    - gestionnaireId : String
    - expertId : String
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
}

class ClaimHistory {
    - id : String
    - claimId : String
    - action : String
    - description : String
    - utilisateurId : String
    - utilisateurNom : String
    - ancienStatut : String
    - nouveauStatut : String
    - createdAt : LocalDateTime
}

class Notification {
    - id : String
    - utilisateurId : String
    - titre : String
    - message : String
    - type : String
    - claimId : String
    - lu : boolean
    - createdAt : LocalDateTime
}

User "1" -- "n" Claim : déclare
User "1" -- "n" Claim : traite (gestionnaire)
Claim "1" -- "n" ClaimHistory : historique
Claim "1" -- "n" Notification : notifie
Claim --> StatutSinistre : a un
Claim --> GraviteSinistre : a une

@enduml
```

### Diagramme de séquence — Sprint 2

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

---

## Sprint 3 — Expertise & Analyse IA

**Objectif** : Intégrer l'analyse IA via Ollama et la gestion des rapports d'expertise.

### Diagramme de classes — Sprint 3

```plantuml
@startuml sprint3_classes
skinparam monochrome true
skinparam shadowing false

class Claim {
    - id : String
    - reference : String
    - description : String
    - categorie : String
    - estimation : String
    - statut : StatutSinistre
    - analyseIAId : String
    - expertId : String
}

enum StatutExpertise {
    EN_ATTENTE
    EN_COURS
    SOUMISE
    VALIDEE
    REFUSEE
}

class Expertise {
    - id : String
    - claimId : String
    - expertId : String
    - gestionnaireId : String
    - conclusion : String
    - montantEstime : String
    - recommandation : String
    - piecesJointes : List<String>
    - commentaires : String
    - statut : StatutExpertise
    - dateRapport : LocalDateTime
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
}

enum Severite {
    FAIBLE
    MODEREE
    ELEVEE
    CRITIQUE
}

enum TypeAnalyse {
    INITIALE
    APPROFONDIE
    REANALYSE
    VERIFICATION_FRAUDE
}

enum StatutAnalyse {
    EN_COURS
    TERMINEE
    ERREUR
    EXPIREE
}

class AnalyseIA {
    - id : String
    - claimId : String
    - scoreComplexite : int
    - scoreRisque : int
    - scoreConfiance : int
    - montantEstime : Double
    - devise : String
    - severite : Severite
    - necessiteExpertHumain : boolean
    - recommandation : String
    - justification : String
    - resumeAnalyse : String
    - pointsAttention : String
    - elementsFraude : String
    - typeAnalyse : TypeAnalyse
    - statut : StatutAnalyse
    - modeleIA : String
    - dateAnalyse : LocalDateTime
}

class User {
    - id : String
    - nom : String
    - role : Role
    - specialite : String
}

Claim "1" -- "n" Expertise : possède
Claim "1" -- "1" AnalyseIA : analysé par
User "1" -- "n" Expertise : réalise (Expert)
Expertise --> StatutExpertise : a un
AnalyseIA --> Severite : a une
AnalyseIA --> TypeAnalyse : a un
AnalyseIA --> StatutAnalyse : a un

@enduml
```

### Diagramme de séquence — Sprint 3

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

---

## Sprint 4 — Indemnisation & Remboursement (Stripe)

**Objectif** : Calcul d'indemnisation, proposition, validation par l'assuré, et paiement via Stripe.

### Diagramme de classes — Sprint 4

```plantuml
@startuml sprint4_classes
skinparam monochrome true
skinparam shadowing false

class Claim {
    - id : String
    - reference : String
    - statut : StatutSinistre
    - montantIndemnisationPropose : Double
    - montantIndemnisationFinal : Double
    - indemnisationAcceptee : Boolean
    - motifRefusIndemnisation : String
    - recoursEnCours : Boolean
    - datePaiement : LocalDateTime
}

enum StatutRemboursement {
    EN_ATTENTE
    VALIDEE
    EN_COURS_TRAITEMENT
    PAYE
    REFUSE
}

enum MethodePaiement {
    CARTE_BANCAIRE
}

class Reimbursement {
    - id : String
    - claimId : String
    - assureId : String
    - reference : String
    - montantDegats : double
    - capitalAssure : double
    - franchise : double
    - plafondGarantie : double
    - tauxRemboursement : double
    - montantApresFranchise : double
    - montantIndemnisationCalcule : double
    - montantPropose : double
    - montantFinal : double
    - methodePaiement : MethodePaiement
    - stripeSessionId : String
    - stripePaymentIntentId : String
    - transactionId : String
    - statut : StatutRemboursement
    - historiqueWorkflow : List<EtapeWorkflow>
    - gestionnaireId : String
    - motifRefus : String
    - dateProposition : LocalDateTime
    - dateValidation : LocalDateTime
    - datePaiement : LocalDateTime
}

class EtapeWorkflow {
    - statut : StatutRemboursement
    - description : String
    - effectuePar : String
    - effectueParNom : String
    - date : LocalDateTime
}

class User {
    - id : String
    - nom : String
    - role : Role
}

Claim "1" -- "1" Reimbursement : indemnisé par
User "1" -- "n" Reimbursement : bénéficie (Assuré)
Reimbursement *-- "n" EtapeWorkflow : contient
Reimbursement --> StatutRemboursement : a un
Reimbursement --> MethodePaiement : a une

@enduml
```

### Diagramme de séquence — Sprint 4

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

---

## Sprint 5 — Détection de Fraude & Messagerie

**Objectif** : Signalement de fraude, résolution admin, système de messagerie temps réel, notifications.

### Diagramme de classes — Sprint 5

```plantuml
@startuml sprint5_classes
skinparam monochrome true
skinparam shadowing false

class Claim {
    - id : String
    - reference : String
    - statut : StatutSinistre
}

enum NiveauRisque {
    FAIBLE
    MOYEN
    ELEVE
    CRITIQUE
}

enum StatutAlerte {
    SOUMISE
    EN_COURS_ANALYSE
    CONFIRMEE
    INFONDEE
    ENQUETE_SUPPLEMENTAIRE
    CLOTUREE
}

class FraudAlert {
    - id : String
    - claimId : String
    - signalePar : String
    - motif : String
    - description : String
    - niveauRisque : NiveauRisque
    - statut : StatutAlerte
    - piecesJustificatives : List<String>
    - resoluPar : String
    - decision : String
    - notesResolution : String
    - dateResolution : LocalDateTime
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
}

class Conversation {
    - id : String
    - participant1Id : String
    - participant2Id : String
    - claimId : String
    - dernierMessage : String
    - dernierMessageDate : LocalDateTime
    - messagesNonLusParticipant1 : int
    - messagesNonLusParticipant2 : int
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
}

class Message {
    - id : String
    - conversationId : String
    - expediteurId : String
    - contenu : String
    - lu : boolean
    - createdAt : LocalDateTime
}

class Notification {
    - id : String
    - utilisateurId : String
    - titre : String
    - message : String
    - type : String
    - claimId : String
    - lu : boolean
    - createdAt : LocalDateTime
}

class User {
    - id : String
    - nom : String
    - role : Role
}

Claim "1" -- "n" FraudAlert : suspecté dans
User "1" -- "n" FraudAlert : signale (Gestionnaire)
User "1" -- "n" FraudAlert : résout (Admin)
User "1" -- "n" Conversation : participe
Conversation "1" -- "n" Message : contient
User "1" -- "n" Notification : reçoit
FraudAlert --> NiveauRisque : a un
FraudAlert --> StatutAlerte : a un

@enduml
```

### Diagramme de séquence — Sprint 5

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

---

## Sprint 6 — Administration, Rapports & Support

**Objectif** : Dashboard admin, analytics, export CSV, tickets support, monitoring IA.

### Diagramme de classes — Sprint 6

```plantuml
@startuml sprint6_classes
skinparam monochrome true
skinparam shadowing false

class User {
    - id : String
    - nom : String
    - role : Role
}

class Claim {
    - id : String
    - reference : String
    - statut : StatutSinistre
    - createdAt : LocalDateTime
}

class AnalyseIA {
    - id : String
    - claimId : String
    - statut : StatutAnalyse
    - modeleIA : String
    - necessiteExpertHumain : boolean
    - coutEstime : double
}

enum StatutTicket {
    OUVERT
    EN_COURS
    EN_ATTENTE
    RESOLU
    FERME
}

class Ticket {
    - id : String
    - assureId : String
    - claimId : String
    - sujet : String
    - description : String
    - categorie : String
    - statut : StatutTicket
    - assigneA : String
    - messages : List<TicketMessage>
    - createdAt : LocalDateTime
    - updatedAt : LocalDateTime
}

class TicketMessage {
    - expediteurId : String
    - expediteurNom : String
    - contenu : String
    - createdAt : LocalDateTime
}

class ClaimHistory {
    - id : String
    - claimId : String
    - action : String
    - description : String
    - utilisateurId : String
    - utilisateurNom : String
    - ancienStatut : String
    - nouveauStatut : String
    - createdAt : LocalDateTime
}

User "1" -- "n" Ticket : crée (Assuré)
User "1" -- "n" Ticket : traite (Gestionnaire)
Ticket *-- "n" TicketMessage : contient
Ticket --> StatutTicket : a un
Claim "1" -- "n" ClaimHistory : historique
Claim "1" -- "1" AnalyseIA : analysé par

@enduml
```

### Diagramme de séquence — Sprint 6

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
