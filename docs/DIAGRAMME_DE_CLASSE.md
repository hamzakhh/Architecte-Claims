  # Diagramme de Classes UML — L'Architecte Claims

  ## 1. Vue globale

  ```
  ┌─────────┐
  │ «enum»  │
  │  Role   │
  └────┬────┘
      ▲
  ┌────┴──────┐        ┌──────────┐
  │   User    │1──n───►│  Claim   │
  └──┬──┬──┬──┘        └─┬──┬──┬─┘
    │  │  │             │  │  │
    │  │  │    ┌────────┘  │  └──────┐
    │  │  │    ▼           ▼         ▼
    │  │  │ ┌─────────┐┌─────────┐┌──────────┐
    │  │  │ │Expertise││AnalyseIA││ClaimHist.│
    │  │  │ └─────────┘└─────────┘└──────────┘
    │  │  │
    │  │  └──n──►┌────────────┐
    │  │         │Notification│
    │  │         └────────────┘
    │  │
    │  └──n──►┌────────────┐   ┌────────────┐
    │         │ FraudAlert │   │Reimbursement│
    │         └─────┬──────┘   └──────┬──────┘
    │               ▲                 ▲
    └──────n────────┴─────────────────┘

  ┌────────────┐1──n──►┌─────────┐
  │Conversation│       │ Message │
  └────────────┘       └─────────┘

  ┌────────┐1──n──►┌──────────────┐
  │ Ticket │       │TicketMessage │
  └────────┘       └──────────────┘
  ```

  ## 2. Matrice des associations

  | Source | Cible | Cardinalité | Type |
  |--------|-------|-------------|------|
  | User (Assuré) | Claim | 1→n | Déclare |
  | User (Gestionnaire) | Claim | 1→n | Traite |
  | User (Expert) | Expertise | 1→n | Réalise |
  | Claim | Expertise | 1→n | Possède |
  | Claim | AnalyseIA | 1→1 | Analysé par |
  | Claim | ClaimHistory | 1→n | Historique |
  | Claim | FraudAlert | 1→n | Suspecté |
  | Claim | Reimbursement | 1→1 | Indemnisé |
  | Claim | Notification | 1→n | Notifie |
  | User | Notification | 1→n | Reçoit |
  | User | Conversation | 1→n | Participe |
  | Conversation | Message | 1→n | Contient |
  | User (Assuré) | Ticket | 1→n | Crée |
  | Ticket | TicketMessage | 1→n | Contient |

  ## 3. Classes et attributs

  ### «enum» Role
  ASSURE · GESTIONNAIRE · EXPERT · ADMIN

  ### User — `users` — implements UserDetails
  | Attribut | Type |
  |----------|------|
  | id | String |
  | prenom, nom | String |
  | email | String (unique) |
  | password | String (BCrypt) |
  | telephone | String |
  | role | Role |
  | specialite, zoneIntervention | String (Expert) |
  | notePerformance | double (Expert) |
  | chargeMax | int (Expert) |
  | certifications | List\<String\> (Expert) |
  | enabled | boolean |
    | createdAt, updatedAt | LocalDateTime |

  ### Claim — `claims`
  | Attribut | Type |
  |----------|------|
  | id, reference (SIN-YYYY-XXXX) | String |
  | assureId, gestionnaireId, expertId, analyseIAId | String (FK) |
  | categorie, type, description | String |
  | latitude, longitude | Double |
  | dateSinistre, heureSinistre, lieu, notesLieu | String |
  | piecesJointes | List\<String\> |
  | estimation, notesInternes | String |
  | gravite | GraviteSinistre (MINEURE·MODEREE·MAJEURE·CRITIQUE) |
  | franchise, plafondCouverture | Double |
  | montantIndemnisationPropose/Final | Double |
  | indemnisationAcceptee, recoursEnCours | Boolean |
  | statut | StatutSinistre (EN_COURS→EN_REVISION→EXPERTISE→VALIDE→REFUSE→INDEMNISATION_PROPOSEE→INDEMNISATION_ACCEPTEE→PAIEMENT_EN_COURS→RECOURS→CLOTURE→ARCHIVE) |
  | createdAt, updatedAt | LocalDateTime |

  ### Expertise — `expertises`
  | Attribut | Type |
  |----------|------|
  | id, claimId, expertId, gestionnaireId | String |
  | conclusion, montantEstime, recommandation, commentaires | String |
  | piecesJointes | List\<String\> |
  | statut | StatutExpertise (EN_ATTENTE·EN_COURS·SOUMISE·VALIDEE·REFUSEE) |
  | dateRapport, createdAt, updatedAt | LocalDateTime |

  ### AnalyseIA — `analyses_ia`
  | Attribut | Type |
  |----------|------|
  | id, claimId | String |
  | scoreComplexite, scoreRisque, scoreConfiance | int (0-100) |
  | montantEstime | Double |
  | devise | String (TND) |
  | severite | FAIBLE·MODEREE·ELEVEE·CRITIQUE |
  | necessiteExpertHumain | boolean |
  | recommandation, justification, resumeAnalyse | String |
  | pointsAttention, elementsFraude, recommandationsAction | String |
  | typeAnalyse | INITIALE·APPROFONDIE·REANALYSE·VERIFICATION_FRAUDE |
  | statut | EN_COURS·TERMINEE·ERREUR·EXPIREE |
  | modeleIA | String |
  | createdAt, updatedAt | LocalDateTime |

  ### Reimbursement — `reimbursements`
  | Attribut | Type |
  |----------|------|
  | id, claimId, assureId, reference (REM-YYYY-XXXX) | String |
  | montantDegats, capitalAssure, franchise, plafondGarantie, tauxRemboursement | double |
  | montantApresFranchise, montantIndemnisationCalcule | double |
  | montantPropose, montantFinal | double |
  | methodePaiement | CARTE_BANCAIRE |
  | stripeSessionId, stripePaymentIntentId, transactionId | String |
  | statut | EN_ATTENTE·VALIDEE·EN_COURS_TRAITEMENT·PAYE·REFUSE |
  | historiqueWorkflow | List\<EtapeWorkflow\> |
  | gestionnaireId, motifRefus, notes | String |
  | dateProposition/Validation/Traitement/Paiement | LocalDateTime |

  ### FraudAlert — `fraud_alerts`
  | Attribut | Type |
  |----------|------|
  | id, claimId, signalePar | String |
  | motif, description | String |
  | niveauRisque | FAIBLE·MOYEN·ELEVE·CRITIQUE |
  | statut | SOUMISE·EN_COURS_ANALYSE·CONFIRMEE·INFONDEE·ENQUETE_SUPPLEMENTAIRE·CLOTUREE |
  | resoluPar, decision, notesResolution | String |
  | dateResolution, createdAt, updatedAt | LocalDateTime |

  ### ClaimHistory — `claim_history`
  | Attribut | Type |
  |----------|------|
  | id, claimId | String |
  | action, description | String |
  | utilisateurId, utilisateurNom, utilisateurRole | String |
  | ancienStatut, nouveauStatut | String |
  | createdAt | LocalDateTime |

  ### Notification — `notifications`
  | Attribut | Type |
  |----------|------|
  | id, utilisateurId, claimId | String |
  | titre, message, type | String |
  | lu | boolean |
  | createdAt | LocalDateTime |

  ### Conversation — `conversations`
  | Attribut | Type |
  |----------|------|
  | id, participant1Id, participant2Id, claimId | String |
  | dernierMessage | String |
  | dernierMessageDate | LocalDateTime |
  | messagesNonLusParticipant1/2 | int |
  | createdAt, updatedAt | LocalDateTime |

  ### Message — `messages`
  | Attribut | Type |
  |----------|------|
  | id, conversationId, expediteurId | String |
  | contenu | String |
  | lu | boolean |
  | createdAt | LocalDateTime |

  ### Ticket — `tickets`
  | Attribut | Type |
  |----------|------|
  | id, assureId, claimId, assigneA | String |
  | sujet, description, categorie | String |
  | statut | OUVERT·EN_COURS·EN_ATTENTE·RESOLU·FERME |
  | messages | List\<TicketMessage\> |
  | createdAt, updatedAt | LocalDateTime |

  ## 4. Héritage

  ```
  ┌────────────────┐
  │ «interface»     │
  │  UserDetails     │  Spring Security
  └───────┬────────┘
          ▲ implements
  ┌───────┴────────┐
  │     User        │
  └────────────────┘
  ```

  ## 5. Diagramme PlantUML

  ```plantuml
  @startuml
  skinparam monochrome true
  skinparam shadowing false

  enum Role { ASSURE, GESTIONNAIRE, EXPERT, ADMIN }

  class User {
    +id: String
    +prenom: String
    +nom: String
    +email: String
    +password: String
    +telephone: String
    +role: Role
    +specialite: String
    +zoneIntervention: String
    +notePerformance: double
    +chargeMax: int
    +certifications: List<String>
    +enabled: boolean
  }

  class Claim {
    +id: String
    +reference: String
    +assureId: String
    +categorie: String
    +type: String
    +description: String
    +statut: StatutSinistre
    +gravite: GraviteSinistre
    +gestionnaireId: String
    +expertId: String
    +analyseIAId: String
  }

  class Expertise {
    +id: String
    +claimId: String
    +expertId: String
    +conclusion: String
    +statut: StatutExpertise
  }

  class AnalyseIA {
    +id: String
    +claimId: String
    +severite: Severite
    +scoreRisque: int
    +necessiteExpertHumain: boolean
  }

  class Reimbursement {
    +id: String
    +claimId: String
    +assureId: String
    +montantPropose: double
    +montantFinal: double
    +statut: StatutRemboursement
  }

  class FraudAlert {
    +id: String
    +claimId: String
    +signalePar: String
    +niveauRisque: NiveauRisque
    +statut: StatutAlerte
  }

  class ClaimHistory {
    +id: String
    +claimId: String
    +action: String
    +ancienStatut: String
    +nouveauStatut: String
  }

  class Notification {
    +id: String
    +utilisateurId: String
    +titre: String
    +lu: boolean
  }

  class Conversation {
    +id: String
    +participant1Id: String
    +participant2Id: String
  }

  class Message {
    +id: String
    +conversationId: String
    +expediteurId: String
    +contenu: String
  }

  class Ticket {
    +id: String
    +assureId: String
    +sujet: String
    +statut: StatutTicket
  }

  User "1" -- "n" Claim : déclare
  User "1" -- "n" Notification : reçoit
  User "1" -- "n" Conversation : participe
  User "1" -- "n" FraudAlert : signale
  User "1" -- "n" Reimbursement : bénéficie
  Claim "1" -- "n" Expertise : possède
  Claim "1" -- "1" AnalyseIA : analysé par
  Claim "1" -- "n" ClaimHistory : historique
  Claim "1" -- "1" Reimbursement : indemnisé
  Claim "1" -- "n" FraudAlert : suspecté
  Claim "1" -- "n" Notification : notifie
  Conversation "1" -- "n" Message : contient
  Ticket "1" -- "n" TicketMessage : contient
  User ..|> UserDetails : implements

  @enduml
  ```
