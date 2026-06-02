# Schéma d'Architecture Système

## L'Architecte Claims — Système de Gestion des Sinistres

---

## 1. Vue d'ensemble

```
┌──────────────────────────────────────────────────────────────────────┐
│                        SYSTÈME L'ARCHITECTE                         │
│                                                                      │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │  Assuré  │    │Gestionn. │    │  Expert  │    │  Admin   │      │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘      │
│       └───────────────┬┴───────────────┘                │           │
│                       ▼                                │           │
│              ┌─────────────────┐                       │           │
│              │   Navigateur    │◄──────────────────────┘           │
│              │  (Angular SPA)  │                                   │
│              └────────┬────────┘                                   │
└───────────────────────┼────────────────────────────────────────────┘
                        │ HTTP :80
                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        COUCHE PRÉSENTATION                          │
│              ┌─────────────────────────┐                             │
│              │        Nginx            │                             │
│              │  Fichiers statiques +   │                             │
│              │   Reverse Proxy /api/*  │                             │
│              └────────────┬────────────┘                             │
└──────────────────────────┼───────────────────────────────────────────┘
                           │ HTTP :8080
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        COUCHE MÉTIER                                 │
│              ┌─────────────────────────┐                             │
│              │      Spring Boot 4      │                             │
│              │                         │                             │
│              │  ┌───────────────────┐  │                             │
│              │  │  REST Controllers │  │  14 contrôleurs             │
│              │  └─────────┬─────────┘  │                             │
│              │            ▼            │                             │
│              │  ┌───────────────────┐  │                             │
│              │  │  Services Métier  │  │  16 services                │
│              │  └─────────┬─────────┘  │                             │
│              │            ▼            │                             │
│              │  ┌───────────────────┐  │                             │
│              │  │   Repositories    │  │  11 repositories            │
│              │  └─────────┬─────────┘  │                             │
│              │            │            │                             │
│              │  ┌───────┐ ┌───────┐   │                             │
│              │  │ JWT / │ │Stripe │   │                             │
│              │  │Security│ │ SDK  │   │                             │
│              │  └───────┘ └───────┘   │                             │
│              └────────────┬────────────┘                             │
└──────────────────────────┼───────────────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
┌─────────────────┐ ┌──────────┐ ┌──────────────┐
│   MongoDB 8     │ │ Ollama   │ │  Stripe API  │
│  (Données)      │ │ (LLaMA3) │ │  (Paiement)  │
│  Port 27017     │ │ Port     │ │  (Externe)    │
│                 │ │ 11434    │ │               │
└─────────────────┘ └──────────┘ └──────────────┘
```

---

## 2. Architecture en couches

```
┌─────────────────────────────────────────────────────┐
│              COUCHE CLIENT (Frontend)                │
│  Angular 21 · TailwindCSS · Leaflet · Stripe JS     │
├─────────────────────────────────────────────────────┤
│              COUCHE GATEWAY (Nginx)                  │
│  Serveur statique · Reverse Proxy · CORS            │
├─────────────────────────────────────────────────────┤
│              COUCHE API (Spring Boot)                │
│  Controllers · DTO · Security (JWT)                 │
├─────────────────────────────────────────────────────┤
│              COUCHE MÉTIER (Services)                │
│  Logique métier · Analyse IA · Paiement · PDF       │
├─────────────────────────────────────────────────────┤
│              COUCHE ACCÈS AUX DONNÉES               │
│  Repositories · MongoDB Driver                      │
├─────────────────────────────────────────────────────┤
│              COUCHE STOCKAGE                         │
│  MongoDB (NoSQL) · Système de fichiers (uploads)    │
└─────────────────────────────────────────────────────┘
```

---

## 3. Flux de données principal (Déclaration de sinistre)

```
 Assuré          Angular          Nginx         Spring Boot      MongoDB      Ollama
   │               │               │               │               │          │
   │  Déclarer     │               │               │               │          │
   │  sinistre     │               │               │               │          │
   ├──────────────►│  POST /api/claims              │               │          │
   │               ├──────────────►│               │               │          │
   │               │               ├──────────────►│               │          │
   │               │               │               │  Vérifier JWT │          │
   │               │               │               ├──────┐        │          │
   │               │               │               │◄─────┘        │          │
   │               │               │               │  Créer claim  │          │
   │               │               │               ├──────────────►│          │
   │               │               │               │  Claim sauvé  │          │
   │               │               │               │◄──────────────┤          │
   │               │               │               │  Analyse IA   │          │
   │               │               │               ├─────────────────────────►│
   │               │               │               │  Résultat IA  │          │
   │               │               │               │◄─────────────────────────┤
   │               │               │               │  Sauver analyse│         │
   │               │               │               ├──────────────►│          │
   │               │               │               │  201 Created  │          │
   │               │               │◄──────────────┤               │          │
   │               │  Réponse      │               │               │          │
   │               │◄──────────────┤               │               │          │
   │  Confirmation │               │               │               │          │
   │◄──────────────┤               │               │               │          │
```

---

## 4. Flux d'authentification (JWT)

```
 Client                    Spring Boot
   │                           │
   │  POST /api/auth/login     │
   │  {email, password}        │
   ├──────────────────────────►│
   │                           │  Vérifier credentials
   │                           │  Générer JWT
   │  200 + JWT Token          │
   │◄──────────────────────────┤
   │                           │
   │  GET /api/claims           │
   │  Authorization: Bearer JWT│
   ├──────────────────────────►│
   │                           │  Valider token + rôle
   │  200 + Données            │
   │◄──────────────────────────┤
```

---

## 5. Flux de paiement (Stripe)

```
 Assuré      Angular      Spring Boot     Stripe API     MongoDB
   │            │              │              │            │
   │  Accepter  │              │              │            │
   │  indemnité │              │              │            │
   ├───────────►│              │              │            │
   │            │  POST /api/reimbursements/  │            │
   │            │  :id/checkout              │            │
   │            ├─────────────►│              │            │
   │            │              │  Créer session│            │
   │            │              │  Checkout     │            │
   │            │              ├─────────────►│            │
   │            │              │  Session URL  │            │
   │            │              │◄─────────────┤            │
   │            │  Redirect URL│              │            │
   │            │◄─────────────┤              │            │
   │  Redirect  │              │              │            │
   │◄───────────┤              │              │            │
   │            │              │              │            │
   │  Paiement sur Stripe.com  │              │            │
   ├──────────────────────────────────────────►│            │
   │            │              │  Webhook      │            │
   │            │              │◄─────────────┤            │
   │            │              │  Mettre à jour│            │
   │            │              │  statut paiement           │
   │            │              ├──────────────────────────►│
   │            │              │              │            │
```

---

## 6. Modèle de données simplifié

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│   User   │────►│  Claim   │────►│  Expertise   │
│          │ 1:n │ (Sinistre)│ 1:n │              │
└──────────┘     └────┬─────┘     └──────────────┘
                      │
           ┌──────────┼──────────┐
           ▼          ▼          ▼
   ┌────────────┐ ┌─────────┐ ┌───────────┐
   │ AnalyseIA  │ │Reimburs.│ │FraudAlert │
   └────────────┘ └─────────┘ └───────────┘
                      │
                      ▼
               ┌───────────┐
               │ Paiement  │
               │ (Stripe)  │
               └───────────┘

┌──────────────┐    ┌─────────────┐    ┌──────────┐
│ Notification │    │  Message /  │    │  Ticket  │
│              │    │ Conversation│    │ (Support)│
└──────────────┘    └─────────────┘    └──────────┘

┌──────────────┐
│ClaimHistory  │
│(Historique)  │
└──────────────┘
```

---

## 7. Sécurité et contrôle d'accès

```
┌─────────────────────────────────────────────────┐
│              Spring Security + JWT               │
├─────────────────────────────────────────────────┤
│                                                  │
│  Requête HTTP ──► JwtAuthFilter                  │
│                       │                          │
│                  Token valide ?                   │
│                  ┌────┴────┐                     │
│                  OUI       NON                   │
│                  │         │                     │
│                  ▼         ▼                     │
│           Vérifier rôle   401 Unauthorized        │
│                  │                               │
│        ┌─────────┼─────────┐                     │
│        ▼         ▼         ▼                     │
│    ASSURE   GESTIONN.  EXPERT  ADMIN             │
│        │         │         │      │              │
│        ▼         ▼         ▼      ▼              │
│   /api/claims  /api/claims  /api/expertises  /api/admin
│   (ses sinistres) (tous)   (ses expertises) (tout)      │
└─────────────────────────────────────────────────┘
```

---

## 8. Déploiement Docker

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
│                                                          │
│  ┌──────────────────┐  ┌──────────────────┐             │
│  │  larchitecte-    │  │  larchitecte-    │             │
│  │  frontend        │  │  backend         │             │
│  │                  │  │                  │             │
│  │  nginx:alpine    │  │  temurin:21-jre  │             │
│  │  Port 80 ────────┼──┼─► Port 8080     │             │
│  └──────────────────┘  └────────┬─────────┘             │
│                                 │                        │
│                                 ▼                        │
│                        ┌──────────────────┐             │
│                        │  larchitecte-    │             │
│                        │  mongodb         │             │
│                        │                  │             │
│                        │  mongo:8         │             │
│                        │  Port 27017      │             │
│                        └──────────────────┘             │
│                                                          │
│  Volumes:                                                │
│  • mongo-data → /data/db (persistance MongoDB)          │
│  • uploads-data → /app/uploads (fichiers uploadés)      │
└─────────────────────────────────────────────────────────┘

  Service externe (hors Docker) :
  ┌──────────────────┐
  │  Ollama (LLaMA3) │
  │  localhost:11434  │
  └──────────────────┘
```

---

## 9. Résumé des composants

| Composant | Technologie | Port | Rôle |
|-----------|------------|------|------|
| **Frontend** | Angular 21 + TailwindCSS | 80 (Nginx) | Interface utilisateur SPA |
| **Reverse Proxy** | Nginx Alpine | 80 | Fichiers statiques + proxy `/api/*` |
| **Backend API** | Spring Boot 4 + Java 21 | 8080 | Logique métier, REST API |
| **Base de données** | MongoDB 8 | 27017 | Stockage NoSQL (12 collections) |
| **IA** | Ollama (LLaMA 3) | 11434 | Analyse de sinistres, détection fraude |
| **Paiement** | Stripe API | 443 (HTTPS) | Paiement en ligne, webhooks |
| **Conteneurisation** | Docker Compose | — | Orchestration des services |
