# L'Architecte Claims

**Système de Gestion des Sinistres d'Assistance et d'Indemnisation**

> Projet de développement (approche agile SCRUM) — Application web full-stack de gestion des sinistres d'assurance avec analyse IA, paiement en ligne et détection de fraude.

---

## Table des matières

- [Présentation](#présentation)
- [Fonctionnalités](#fonctionnalités)
- [Architecture technique](#architecture-technique)
- [Stack technologique](#stack-technologique)
- [Structure du projet](#structure-du-projet)
- [Modèle de données](#modèle-de-données)
- [Installation et lancement](#installation-et-lancement)
- [Variables d'environnement](#variables-denvironnement)
- [API REST](#api-rest)
- [Auteurs](#auteurs)
- [Licence](#licence)

---

## Présentation

**L'Architecte Claims** est une plateforme web complète de gestion des sinistres d'assurance. Elle permet la déclaration, le traitement, l'expertise et l'indemnisation des sinistres en intégrant des fonctionnalités avancées telles que l'analyse par intelligence artificielle (LLaMA 3 via Ollama), le paiement en ligne (Stripe) et la détection automatique de fraude.

Le système supporte **quatre rôles utilisateurs** distincts, chacun disposant d'un espace dédié avec tableau de bord et fonctionnalités spécifiques :

| Rôle | Description |
|------|-------------|
| **Assuré** | Déclare des sinistres, suit l'avancement, consulte les remboursements |
| **Gestionnaire** | Traite les dossiers, qualifie les sinistres, gère l'indemnisation |
| **Expert** | Réalise les expertises terrain, rédige les rapports |
| **Administrateur** | Supervise le système, gère les utilisateurs et le monitoring IA |

---

## Fonctionnalités

### Authentification et sécurité
- Inscription / connexion avec JWT (Bearer Token)
- Réinitialisation du mot de passe par token sécurisé
- Contrôle d'accès basé sur les rôles (RBAC) via Spring Security
- Guard de routage Angular par rôle

### Gestion des sinistres
- Déclaration de sinistre avec géolocalisation (Leaflet)
- Upload de pièces jointes (fichiers, photos)
- Workflow de statut complet : `EN_COURS` → `EN_REVISION` → `EXPERTISE` → `VALIDE` → `INDEMNISATION_PROPOSEE` → `INDEMNISATION_ACCEPTEE` → `PAIEMENT_EN_COURS` → `CLOTURE`
- Historique des actions sur chaque sinistre
- Qualification automatique de la gravité (MINEURE, MODEREE, MAJEURE, CRITIQUE)
- Assignation automatique d'expert selon la spécialité et la zone

### Analyse IA (Ollama / LLaMA 3)
- Analyse automatique à la création du sinistre
- Évaluation de la sévérité et du risque de fraude
- Recommandations de traitement
- Monitoring des analyses par l'administrateur

### Indemnisation et paiement
- Calcul automatisé de l'indemnisation (franchise, taux, plafond)
- Proposition détaillée avec justificatif
- Acceptation / refus par l'assuré
- Paiement en ligne via **Stripe Checkout**
- Webhooks Stripe pour confirmation automatique
- Génération de PDF (lettre de remboursement, lettre de rejet)

### Détection de fraude
- Alertes automatiques de fraude
- Signalement par les gestionnaires
- Surveillance centralisée par l'administrateur

### Communication
- Système de messagerie entre utilisateurs
- Notifications en temps réel
- Tickets de support

### Reporting et tableaux de bord
- Statistiques par rôle (assuré, gestionnaire, expert, admin)
- Rapports et exports
- Analytics d'administration

---

## Architecture technique

```
┌─────────────────────────────────────────────────────┐
│                   Navigateur                         │
│              Angular 21 + TailwindCSS                │
└────────────────────┬────────────────────────────────┘
                     │ HTTP :80
┌────────────────────▼────────────────────────────────┐
│                  Nginx                               │
│     Service des fichiers statiques + Reverse Proxy   │
│            /api/* → http://backend:8080               │
└────────────────────┬────────────────────────────────┘
                     │ HTTP :8080
┌────────────────────▼────────────────────────────────┐
│            Spring Boot 4 (Backend)                   │
│   REST API · Spring Security · JWT · WebFlux         │
│   Ollama Client · Stripe SDK · PDF Generation        │
└────────────────────┬────────────────────────────────┘
                     │ MongoDB Driver
┌────────────────────▼────────────────────────────────┐
│               MongoDB 8                              │
│          Base de données NoSQL (27017)                │
└─────────────────────────────────────────────────────┘
```

**Conteneurs Docker :**

| Service | Image de base | Port exposé |
|---------|--------------|-------------|
| `frontend` | `node:22-alpine` (build) → `nginx:alpine` (runtime) | 80 |
| `backend` | `maven:3.9-eclipse-temurin-21` (build) → `eclipse-temurin:21-jre-alpine` (runtime) | 8081 → 8080 |
| `mongodb` | `mongo:8` | 27017 |

---

## Stack technologique

### Backend
| Technologie | Version | Rôle |
|------------|---------|------|
| Java | 21 | Langage principal |
| Spring Boot | 4.0.5 | Framework applicatif |
| Spring Data MongoDB | — | Persistance NoSQL |
| Spring Security | — | Authentification et autorisation |
| Spring WebFlux | — | Client HTTP réactif (Ollama) |
| JWT (jjwt) | 0.12.6 | Gestion des tokens |
| Stripe Java SDK | 25.5.0 | Paiement en ligne |
| Lombok | — | Réduction du boilerplate |
| Maven | 3.9 | Build et gestion des dépendances |

### Frontend
| Technologie | Version | Rôle |
|------------|---------|------|
| Angular | 21.2 | Framework SPA |
| TypeScript | 5.9 | Langage typé |
| TailwindCSS | 4.2 | Framework CSS utilitaire |
| Leaflet | 1.9 | Cartes interactives / géolocalisation |
| Stripe JS SDK | 9.4 | Intégration paiement côté client |
| Nginx | Alpine | Serveur web & reverse proxy |

### Infrastructure
| Technologie | Rôle |
|------------|------|
| MongoDB 8 | Base de données NoSQL |
| Docker + Docker Compose | Conteneurisation et orchestration |
| Ollama (LLaMA 3) | Modèle IA local pour l'analyse des sinistres |

---

## Structure du projet

```
Projet/
├── docker-compose.yml              # Orchestration des conteneurs
├── docs/
│   └── API_Postman_Documentation.md # Documentation complète de l'API
├── backend/                        # Serveur Spring Boot
│   ├── Dockerfile                   # Build multi-stage (Maven + JRE)
│   ├── pom.xml                      # Dépendances Maven
│   └── src/main/java/com/example/backend/
│       ├── BackendApplication.java  # Point d'entrée
│       ├── config/                  # Configuration (Ollama, Stripe, Security)
│       ├── controller/              # 14 contrôleurs REST
│       ├── dto/                     # Objets de transfert de données
│       ├── model/                   # 12 entités MongoDB
│       ├── repository/              # Accès aux données
│       ├── security/                # Filtres JWT, configuration Security
│       └── service/                 # 16 services métier
├── front/
│   └── larchitecte-claims/         # Application Angular
│       ├── Dockerfile               # Build multi-stage (Node + Nginx)
│       ├── nginx.conf               # Configuration reverse proxy
│       ├── angular.json
│       ├── package.json
│       └── src/app/
│           ├── app.routes.ts        # Routes avec lazy loading
│           ├── app.config.ts        # Configuration Angular
│           ├── auth/                # Connexion, inscription
│           ├── core/                # AuthGuard, AuthService, Interceptor
│           ├── shared/              # Composants partagés
│           ├── assure/              # Espace assuré (9 pages)
│           ├── gestionnaire/        # Espace gestionnaire (11 pages)
│           ├── expert/              # Espace expert (5 pages)
│           └── admin/               # Espace administrateur (8 pages)
```

---

## Modèle de données

```
User ────────────────────────────────────────────────
  id, prenom, nom, email, password, telephone
  role (ASSURE|GESTIONNAIRE|EXPERT|ADMIN)
  specialite, zoneIntervention, notePerformance
  chargeMax, certifications, enabled

Claim (Sinistre) ────────────────────────────────────
  id, reference (SIN-YYYY-XXXX), assureId
  categorie, type, description, dateSinistre, heureSinistre
  lieu, latitude, longitude, piecesJointes, estimation
  gravite (MINEURE|MODEREE|MAJEURE|CRITIQUE)
  statut (EN_COURS → ... → CLOTURE|ARCHIVE)
  gestionnaireId, expertId, analyseIAId

Expertise ───────────────────────────────────────────
  id, claimId, expertId, rapport, conclusion
  montantEstime, photos, dateExpertise

Reimbursement (Remboursement) ──────────────────────
  id, claimId, assureId, reference (REM-YYYY-XXXX)
  montantDegats, franchise, plafondGarantie, tauxRemboursement
  montantIndemnisationCalcule, montantPropose, montantFinal
  stripeSessionId, stripePaymentIntentId
  methodePaiement, statut

AnalyseIA ───────────────────────────────────────────
  id, claimId, severite, risqueFraude
  recommandations, typeAnalyse

FraudAlert ──────────────────────────────────────────
  id, claimId, type, gravite, details, statut

Notification ────────────────────────────────────────
  id, userId, titre, message, type, lue

Message / Conversation ──────────────────────────────
  id, expediteurId, destinataireId, contenu, conversationId

Ticket ──────────────────────────────────────────────
  id, assureId, sujet, description, statut, priorite

ClaimHistory ────────────────────────────────────────
  id, claimId, action, description, effectuePar, statutAvant, statutApres
```

---

## Installation et lancement

### Prérequis

- **Docker** & **Docker Compose** (recommandé)
- Ou, pour le développement local :
  - **Java 21** (JDK)
  - **Maven 3.9+**
  - **Node.js 22+** & **npm 11+**
  - **MongoDB 8**
  - **Ollama** (pour l'analyse IA)

### Méthode 1 : Docker Compose (recommandé)

```bash
# Cloner le dépôt
git clone <url-du-depot>
cd Projet

# Lancer tous les services
docker compose up --build

# L'application est accessible sur :
#   Frontend : http://localhost
#   Backend  : http://localhost:8081/api
#   MongoDB  : localhost:27017
```

### Méthode 2 : Développement local

**Backend :**
```bash
cd backend
./mvnw spring-boot:run
# Serveur démarré sur http://localhost:8080
```

**Frontend :**
```bash
cd front/larchitecte-claims
npm install
ng serve
# Application démarrée sur http://localhost:4200
```

**MongoDB :**
```bash
# Via Docker
docker run -d -p 27017:27017 --name mongodb mongo:8
```

**Ollama (analyse IA) :**
```bash
# Installer Ollama : https://ollama.com
ollama pull llama3
ollama serve
# API disponible sur http://localhost:11434
```

---

## Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `MONGODB_URI` | URI de connexion MongoDB | `mongodb://localhost:27017/larchitecte_claims` |
| `OLLAMA_API_URL` | URL de l'API Ollama | `http://localhost:11434/api/chat` |
| `FRONTEND_URL` | URL du frontend (CORS) | `http://localhost:4200` |
| `JWT_SECRET` | Clé secrète JWT | Configurée dans `application.properties` |
| `STRIPE_SECRET_KEY` | Clé secrète Stripe | `sk_test_...` |

---

## API REST

L'API REST complète est documentée dans [`docs/API_Postman_Documentation.md`](docs/API_Postman_Documentation.md).

**Aperçu des endpoints principaux :**

| Endpoint | Contrôleur | Description |
|----------|-----------|-------------|
| `/api/auth/**` | `AuthController` | Authentification (login, register) |
| `/api/users/**` | `UserController` | Gestion des profils et administration |
| `/api/claims/**` | `ClaimController` | Déclaration et gestion des sinistres |
| `/api/expertises/**` | `ExpertiseController` | Expertises et rapports |
| `/api/reimbursements/**` | `ReimbursementController` | Indemnisation et paiement Stripe |
| `/api/analyses-ia/**` | `AnalyseIAController` | Analyses IA des sinistres |
| `/api/fraud-alerts/**` | `FraudAlertController` | Alertes de fraude |
| `/api/messages/**` | `MessageController` | Messagerie |
| `/api/notifications/**` | `NotificationController` | Notifications |
| `/api/tickets/**` | `TicketController` | Tickets de support |
| `/api/files/**` | `FileController` | Upload de fichiers |
| `/api/reports/**` | `ReportController` | Rapports et exports |
| `/api/admin/**` | `AdminController` | Administration système |

**Authentification :** Bearer Token JWT — Header `Authorization: Bearer <token>`

---

## Auteurs

Projet réalisé dans le cadre d'un **Projet de développement (approche agile SCRUM)**.

---

## Licence

Ce projet est réalisé à des fins académiques dans le cadre d'un PFA universitaire.
