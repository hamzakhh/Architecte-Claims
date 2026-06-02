# Guide de Test Postman - Remboursements

Ce guide détaille comment tester les endpoints de remboursement avec Postman.

---

## Prérequis

### Configuration Postman

1. **Créer une nouvelle collection** : `Remboursements API`
2. **Variables d'environnement** :

| Variable | Valeur par défaut | Description |
|----------|------------------|-------------|
| `base_url` | `http://localhost:8080` | URL de base de l'API |
| `token` | - | Token JWT (à remplir après login) |
| `claim_id` | - | ID d'un sinistre existant |
| `reimbursement_id` | - | ID d'un remboursement créé |

### Headers par défaut

Ajouter ces headers à chaque requête (sauf webhook) :
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

---

## Étape 1 : Authentification

### 1.1 Connexion

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** (onglet Tests) :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has token", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.token).to.exist;
});

// Sauvegarder le token dans la variable d'environnement
pm.test("Save token to environment", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

**Réponse attendue** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "...",
    "email": "gestionnaire@example.com",
    "role": "GESTIONNAIRE"
  }
}
```

---

## Étape 2 : Récupérer un sinistre existant

### 2.1 Lister les sinistres

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/claims`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is array", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('array');
});

// Sauvegarder le premier ID de sinistre
pm.test("Save first claim ID", function () {
    var jsonData = pm.response.json();
    if (jsonData.length > 0) {
        pm.environment.set("claim_id", jsonData[0].id);
    }
});
```

**Réponse attendue** :
```json
[
  {
    "id": "claim-id-123",
    "categorie": "HABITATION",
    "statut": "INDEMNISATION",
    "description": "Fuite d'eau",
    "montantEstime": 3200.0
  }
]
```

---

## Étape 3 : Calculer l'indemnisation

### 3.1 Calculer (prévisualisation)

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/reimbursements/calculer`

**Body (raw JSON)** :
```json
{
  "claimId": "{{claim_id}}",
  "montantDegats": 3200.0,
  "capitalAssure": 10000.0,
  "franchise": 150.0,
  "plafondGarantie": 5000.0,
  "tauxRemboursement": 0.90,
  "typeSinistre": "DEGAT_DES_EAUX"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has calculation details", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('montantCalcule');
    pm.expect(jsonData).to.have.property('franchise');
    pm.expect(jsonData).to.have.property('plafondGarantie');
});

pm.test("Calculation is correct", function () {
    var jsonData = pm.response.json();
    // Formule: (montantDegats * tauxRemboursement) - franchise
    var expected = (3200.0 * 0.90) - 150.0;
    pm.expect(jsonData.montantCalcule).to.eql(expected);
});
```

**Réponse attendue** :
```json
{
  "montantDegats": 3200.0,
  "capitalAssure": 10000.0,
  "franchise": 150.0,
  "plafondGarantie": 5000.0,
  "tauxRemboursement": 0.90,
  "montantCalcule": 2730.0,
  "details": "Montant calculé: (3200.0 * 0.90) - 150.0 = 2730.0"
}
```

---

## Étape 4 : Créer un remboursement

### 4.1 Créer remboursement

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/reimbursements`

**Body (raw JSON)** :
```json
{
  "claimId": "{{claim_id}}",
  "montantDegats": 3200.0,
  "capitalAssure": 10000.0,
  "franchise": 150.0,
  "plafondGarantie": 5000.0,
  "tauxRemboursement": 0.90,
  "typeSinistre": "DEGAT_DES_EAUX",
  "montantPropose": 2730.0,
  "methodePaiement": "VIREMENT"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has reimbursement ID", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('id');
    pm.expect(jsonData).to.have.property('statut');
    pm.expect(jsonData.statut).to.eql('PROPOSE');
});

// Sauvegarder l'ID du remboursement
pm.test("Save reimbursement ID", function () {
    var jsonData = pm.response.json();
    pm.environment.set("reimbursement_id", jsonData.id);
});
```

**Réponse attendue** :
```json
{
  "id": "reimb-id-456",
  "claimId": "claim-id-123",
  "montantPropose": 3050.0,
  "statut": "PROPOSE",
  "methodePaiement": "VIREMENT",
  "dateCreation": "2025-01-20T10:00:00"
}
```

---

## Étape 5 : Lister les remboursements

### 5.1 Tous les remboursements

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/reimbursements`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is array", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('array');
});

pm.test("Each item has required fields", function () {
    var jsonData = pm.response.json();
    jsonData.forEach(function(item) {
        pm.expect(item).to.have.property('id');
        pm.expect(item).to.have.property('montantPropose');
        pm.expect(item).to.have.property('statut');
    });
});
```

**Réponse attendue** :
```json
[
  {
    "id": "reimb-id-456",
    "claimId": "claim-id-123",
    "montantPropose": 3050.0,
    "statut": "PROPOSE",
    "methodePaiement": "VIREMENT"
  }
]
```

---

## Étape 6 : Valider un remboursement (assuré)

**Note** : Cette étape nécessite de se connecter avec un compte ASSURE.

### 6.1 Connexion assuré

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "assure@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save assure token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

### 6.2 Valider le remboursement

**Méthode** : `PUT`  
**URL** : `{{base_url}}/api/reimbursements/{{reimbursement_id}}/validate`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status changed to VALIDE", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.statut).to.eql('VALIDE');
});
```

**Réponse attendue** :
```json
{
  "id": "reimb-id-456",
  "statut": "VALIDE",
  "dateValidation": "2025-01-20T11:00:00"
}
```

---

## Étape 7 : Créer une session Stripe Checkout

**Note** : Reconnectez-vous avec un compte GESTIONNAIRE ou ADMIN.

### 7.1 Reconnexion gestionnaire

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

### 7.2 Créer session Stripe

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/reimbursements/{{reimbursement_id}}/stripe/checkout`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has Stripe session", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('sessionId');
    pm.expect(jsonData).to.have.property('url');
    pm.expect(jsonData.sessionId).to.match(/^cs_test_/);
});
```

**Réponse attendue** :
```json
{
  "sessionId": "cs_test_aBcDeFgHiJkLmNoPqRsTuVwXyZ",
  "url": "https://checkout.stripe.com/c/pay/cs_test_aBcDeFgHiJkLmNoPqRsTuVwXyZ"
}
```

---

## Étape 8 : Tester le Webhook Stripe

### 8.1 Configuration du webhook

Le webhook est appelé automatiquement par Stripe. Pour tester localement :

**Option 1 : Utiliser ngrok**
```bash
ngrok http 8080
```
Cela exposera votre localhost sur une URL publique (ex: `https://abc123.ngrok.io`)

**Option 2 : Utiliser Stripe CLI**
```bash
stripe listen --forward-to localhost:8080/api/reimbursements/stripe/webhook
```

### 8.2 Test manuel du webhook

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/reimbursements/stripe/webhook`

**Headers** :
```
Content-Type: application/json
Stripe-Signature: t=...,v1=...
```

**Body (raw JSON)** - Exemple d'événement `checkout.session.completed` :
```json
{
  "id": "evt_1234567890",
  "object": "event",
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_aBcDeFgHiJkLmNoPqRsTuVwXyZ",
      "object": "checkout.session",
      "payment_status": "paid",
      "metadata": {
        "reimbursementId": "{{reimbursement_id}}"
      }
    }
  }
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Webhook processed successfully", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('received');
});
```

**Réponse attendue** :
```json
{
  "received": true,
  "message": "Webhook received"
}
```

---

## Scénarios de Test Complets

### Scénario 1 : Flux complet de remboursement (Virement)

1. **Login** (GESTIONNAIRE)
2. **Récupérer un sinistre** en statut `INDEMNISATION`
3. **Calculer l'indemnisation** avec les paramètres
4. **Créer un remboursement** avec méthode `VIREMENT`
5. **Login** (ASSURE)
6. **Valider le remboursement**
7. **Login** (GESTIONNAIRE)
8. **Traiter le remboursement** (`PUT /api/reimbursements/{id}/process`)
9. **Confirmer le paiement** (`PUT /api/reimbursements/{id}/pay`)

### Scénario 2 : Flux avec Stripe

1. **Login** (GESTIONNAIRE)
2. **Récupérer un sinistre** en statut `INDEMNISATION`
3. **Calculer l'indemnisation**
4. **Créer un remboursement** avec méthode `STRIPE`
5. **Login** (ASSURE)
6. **Valider le remboursement**
7. **Login** (GESTIONNAIRE)
8. **Créer session Stripe Checkout**
9. **Simuler paiement Stripe** (via l'URL retournée)
10. **Vérifier le webhook** est bien reçu

### Scénario 3 : Refus de remboursement

1. **Login** (GESTIONNAIRE)
2. **Créer un remboursement**
3. **Login** (ASSURE)
4. **Refuser le remboursement** (`PUT /api/reimbursements/{id}/refuse`)
5. **Vérifier le statut** est `REFUSE`

---

## Tests Automatisés Postman

### Collection Runner

1. **Exporter la collection** : File > Export
2. **Organiser les requêtes** dans l'ordre d'exécution
3. **Configurer les variables d'environnement**
4. **Run Collection** : Bouton "Run" > sélectionner la collection

### Newman (CLI)

```bash
npm install -g newman
newman run "Remboursements API.postman_collection.json" -e environment.json
```

---

## Dépannage

### Erreurs courantes

| Erreur | Cause | Solution |
|--------|-------|----------|
| `401 Unauthorized` | Token manquant ou invalide | Relancer la requête de login |
| `403 Forbidden` | Rôle insuffisant | Vérifier que le rôle a les droits nécessaires |
| `404 Not Found` | ID de remboursement invalide | Vérifier que l'ID existe dans la base |
| `400 Bad Request` | Paramètres manquants | Vérifier le body de la requête |
| `500 Internal Server Error` | Erreur serveur | Consulter les logs du backend |

### Vérifier les variables

Dans la Console Postman (View > Show Postman Console) :
```javascript
console.log("Token:", pm.environment.get("token"));
console.log("Claim ID:", pm.environment.get("claim_id"));
console.log("Reimbursement ID:", pm.environment.get("reimbursement_id"));
```

---

## Checklist de Test

- [ ] Login avec différents rôles (ASSURE, GESTIONNAIRE, ADMIN)
- [ ] Calculer l'indemnisation avec différents paramètres
- [ ] Créer un remboursement avec chaque méthode de paiement
- [ ] Valider un remboursement en tant qu'assuré
- [ ] Refuser un remboursement en tant qu'assuré
- [ ] Créer une session Stripe Checkout
- [ ] Tester le webhook Stripe
- [ ] Vérifier les statuts de remboursement
- [ ] Générer les lettres PDF (remboursement/rejet)
- [ ] Tester les exports CSV
