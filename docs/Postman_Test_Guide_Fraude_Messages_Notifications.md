# Guide de Test Postman - Fraude, Messagerie & Notifications

Ce guide détaille comment tester les fonctionnalités de fraude, messagerie et notifications avec Postman.

---

## Prérequis

### Configuration Postman

1. **Créer une nouvelle collection** : `Fraude Messages Notifications API`
2. **Variables d'environnement** :

| Variable | Valeur par défaut | Description |
|----------|------------------|-------------|
| `base_url` | `http://localhost:8080` | URL de base de l'API |
| `token` | - | Token JWT (à remplir après login) |
| `token_gestionnaire` | - | Token gestionnaire |
| `token_admin` | - | Token admin |
| `token_assure` | - | Token assuré |
| `claim_id` | - | ID d'un sinistre existant |
| `fraud_alert_id` | - | ID d'une alerte de fraude |
| `conversation_id` | - | ID d'une conversation |
| `notification_id` | - | ID d'une notification |

### Headers par défaut

Ajouter ces headers à chaque requête :
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

---

# PARTIE 1 : TESTS FONCTIONNELS — FRAUDE

## Scénario 1 : Flux complet de signalement de fraude

### Étape 1.1 : Connexion Gestionnaire

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
    pm.environment.set("token_gestionnaire", jsonData.token);
});
```

---

### Étape 1.2 : Récupérer un sinistre existant

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/claims`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save first claim ID", function () {
    var jsonData = pm.response.json();
    if (jsonData.length > 0) {
        pm.environment.set("claim_id", jsonData[0].id);
    }
});
```

---

### Étape 1.3 : Créer un signalement de fraude (Gestionnaire)

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/fraud-alerts`

**Body (raw JSON)** :
```json
{
  "claimId": "{{claim_id}}",
  "motif": "DOCUMENTS_SUSPECTS",
  "description": "Les documents fournis semblent falsifiés. Incohérences dans les dates et les montants.",
  "niveauRisque": "ELEVE"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has fraud alert ID", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('id');
    pm.expect(jsonData).to.have.property('statut');
    pm.expect(jsonData.statut).to.eql('EN_ATTENTE');
});

pm.test("Save fraud alert ID", function () {
    var jsonData = pm.response.json();
    pm.environment.set("fraud_alert_id", jsonData.id);
});
```

**Réponse attendue** :
```json
{
  "id": "fraud-alert-123",
  "claimId": "claim-id-123",
  "motif": "DOCUMENTS_SUSPECTS",
  "description": "Les documents fournis semblent falsifiés...",
  "gravite": "HAUTE",
  "statut": "EN_ATTENTE",
  "dateCreation": "2025-01-20T10:00:00"
}
```

---

### Étape 1.4 : Vérifier les signalements du gestionnaire

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/fraud-alerts/mes-signalements`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is array", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('array');
});

pm.test("Alert created is in list", function () {
    var jsonData = pm.response.json();
    var alertId = pm.environment.get("fraud_alert_id");
    var found = jsonData.some(function(alert) {
        return alert.id === alertId;
    });
    pm.expect(found).to.be.true;
});
```

---

### Étape 1.5 : Connexion Admin

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "admin@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save admin token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
    pm.environment.set("token_admin", jsonData.token);
});
```

---

### Étape 1.6 : Vérifier les alertes en attente (Admin)

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/fraud-alerts/en-attente`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("New alert is in pending list", function () {
    var jsonData = pm.response.json();
    var alertId = pm.environment.get("fraud_alert_id");
    var found = jsonData.some(function(alert) {
        return alert.id === alertId && alert.statut === 'EN_ATTENTE';
    });
    pm.expect(found).to.be.true;
});
```

---

### Étape 1.7 : Vérifier les notifications de l'admin

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Admin received notification about fraud alert", function () {
    var jsonData = pm.response.json();
    var fraudNotification = jsonData.some(function(notif) {
        return notif.type === 'FRAUDE_ALERT' || notif.message.includes('fraude');
    });
    pm.expect(fraudNotification).to.be.true;
});
```

---

### Étape 1.8 : Mettre l'alerte en cours d'analyse (Admin)

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/fraud-alerts/{{fraud_alert_id}}/analyser`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status changed to ANALYSE", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.statut).to.eql('ANALYSE');
});
```

**Réponse attendue** :
```json
{
  "id": "fraud-alert-123",
  "statut": "ANALYSE",
  "dateAnalyse": "2025-01-20T11:00:00"
}
```

---

### Étape 1.9 : Résoudre l'alerte comme FRAUDE_CONFIRMEE (Admin)

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/fraud-alerts/{{fraud_alert_id}}/resoudre`

**Body (raw JSON)** :
```json
{
  "decision": "FRAUDE_CONFIRMEE",
  "justification": "Analyse approfondie confirmant la fraude. Documents falsifiés identifiés.",
  "action": "SUSPENDRE_SINISTRE"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status changed to RESOLU", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.statut).to.eql('RESOLU');
    pm.expect(jsonData.decision).to.eql('FRAUDE_CONFIRMEE');
});
```

**Réponse attendue** :
```json
{
  "id": "fraud-alert-123",
  "statut": "RESOLU",
  "decision": "FRAUDE_CONFIRMEE",
  "justification": "Analyse approfondie confirmant la fraude...",
  "action": "SUSPENDRE_SINISTRE",
  "dateResolution": "2025-01-20T12:00:00"
}
```

---

## Scénario 2 : Vérification de l'immuabilité après résolution

### Étape 2.1 : Tentative de modification après résolution

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/fraud-alerts/{{fraud_alert_id}}/analyser`

**Tests Postman** :
```javascript
pm.test("Status code is 403 or 400", function () {
    pm.expect(pm.response.code).to.be.oneOf([403, 400]);
});

pm.test("Error message about immutability", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('error');
});
```

**Réponse attendue** :
```json
{
  "error": "Impossible de modifier une alerte résolue"
}
```

---

### Étape 2.2 : Vérifier que l'alerte est bien résolue

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/fraud-alerts/{{fraud_alert_id}}`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Alert is still RESOLU", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.statut).to.eql('RESOLU');
    pm.expect(jsonData.decision).to.eql('FRAUDE_CONFIRMEE');
});
```

---

## Scénario 3 : Test des statistiques de fraude

### Étape 3.1 : Récupérer les statistiques de fraude

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/fraud-alerts/stats`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has statistics", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('totalAlertes');
    pm.expect(jsonData).to.have.property('alertesEnAttente');
    pm.expect(jsonData).to.have.property('alertesResolues');
    pm.expect(jsonData).to.have.property('tauxFraude');
});

pm.test("Statistics are numbers", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.totalAlertes).to.be.a('number');
    pm.expect(jsonData.tauxFraude).to.be.a('number');
});
```

**Réponse attendue** :
```json
{
  "totalAlertes": 15,
  "alertesEnAttente": 3,
  "alertesEnAnalyse": 2,
  "alertesResolues": 10,
  "fraudeConfirmee": 7,
  "fausseAlerte": 3,
  "tauxFraude": 46.67,
  "tendance": "STABLE"
}
```

---

### Étape 3.2 : Créer plusieurs alertes pour tester les tendances

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/fraud-alerts`

**Body (raw JSON)** :
```json
{
  "claimId": "{{claim_id}}",
  "motif": "MONTANT_EXCESSIF",
  "description": "Montant réclamé anormalement élevé pour ce type de sinistre",
  "gravite": "MOYENNE"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});
```

---

### Étape 3.3 : Vérifier la mise à jour des statistiques

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/fraud-alerts/stats`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Statistics updated", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.totalAlertes).to.be.above(15);
});
```

---

# PARTIE 2 : TESTS FONCTIONNELS — MESSAGERIE

## Scénario 1 : Envoi de message d'un gestionnaire à un assuré

### Étape 1.1 : Connexion Gestionnaire

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
    pm.environment.set("token_gestionnaire", jsonData.token);
});
```

---

### Étape 1.2 : Envoyer un message (création de conversation)

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/messages`

**Body (raw JSON)** :
```json
{
  "conversationId": null,
  "destinataireId": "assure-user-id",
  "contenu": "Bonjour, concernant votre sinistre {{claim_id}}, nous avons besoin de documents supplémentaires.",
  "claimId": "{{claim_id}}"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has conversation ID", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('conversationId');
    pm.expect(jsonData).to.have.property('id');
});

pm.test("Save conversation ID", function () {
    var jsonData = pm.response.json();
    pm.environment.set("conversation_id", jsonData.conversationId);
});
```

**Réponse attendue** :
```json
{
  "id": "msg-123",
  "conversationId": "conv-456",
  "expediteurId": "gestionnaire-id",
  "destinataireId": "assure-id",
  "contenu": "Bonjour, concernant votre sinistre...",
  "dateEnvoi": "2025-01-20T10:00:00",
  "lu": false
}
```

---

### Étape 1.3 : Vérifier les conversations du gestionnaire

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("New conversation is in list", function () {
    var jsonData = pm.response.json();
    var convId = pm.environment.get("conversation_id");
    var found = jsonData.some(function(conv) {
        return conv.id === convId;
    });
    pm.expect(found).to.be.true;
});

pm.test("Conversation has unread count", function () {
    var jsonData = pm.response.json();
    var convId = pm.environment.get("conversation_id");
    var conversation = jsonData.find(function(conv) {
        return conv.id === convId;
    });
    pm.expect(conversation).to.have.property('nonLus');
});
```

---

### Étape 1.4 : Connexion Assuré

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
    pm.environment.set("token_assure", jsonData.token);
});
```

---

### Étape 1.5 : Vérifier le compteur de non lus de l'assuré

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Assure has unread messages", function () {
    var jsonData = pm.response.json();
    var convId = pm.environment.get("conversation_id");
    var conversation = jsonData.find(function(conv) {
        return conv.id === convId;
    });
    pm.expect(conversation.nonLus).to.be.above(0);
});
```

---

## Scénario 2 : Lecture des messages (mise à zéro du compteur)

### Étape 2.1 : Lire les messages de la conversation

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations/{{conversation_id}}`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is array of messages", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('array');
});

pm.test("Messages have required fields", function () {
    var jsonData = pm.response.json();
    jsonData.forEach(function(msg) {
        pm.expect(msg).to.have.property('id');
        pm.expect(msg).to.have.property('contenu');
        pm.expect(msg).to.have.property('dateEnvoi');
    });
});
```

**Réponse attendue** :
```json
[
  {
    "id": "msg-123",
    "expediteurId": "gestionnaire-id",
    "destinataireId": "assure-id",
    "contenu": "Bonjour, concernant votre sinistre...",
    "dateEnvoi": "2025-01-20T10:00:00",
    "lu": true
  }
]
```

---

### Étape 2.2 : Vérifier que le compteur est remis à zéro

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Unread count is now 0", function () {
    var jsonData = pm.response.json();
    var convId = pm.environment.get("conversation_id");
    var conversation = jsonData.find(function(conv) {
        return conv.id === convId;
    });
    pm.expect(conversation.nonLus).to.eql(0);
});
```

---

### Étape 2.3 : Envoyer une réponse de l'assuré

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/messages`

**Body (raw JSON)** :
```json
{
  "conversationId": "{{conversation_id}}",
  "contenu": "Je vais envoyer les documents demandés dès que possible."
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});
```

---

### Étape 2.4 : Vérifier l'incrémentation du compteur pour le gestionnaire

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

---

### Étape 2.5 : Vérifier les conversations du gestionnaire

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Gestionnaire has unread messages", function () {
    var jsonData = pm.response.json();
    var convId = pm.environment.get("conversation_id");
    var conversation = jsonData.find(function(conv) {
        return conv.id === convId;
    });
    pm.expect(conversation.nonLus).to.be.above(0);
});
```

---

## Scénario 3 : Conversations liées à un sinistre vs conversations générales

### Étape 3.1 : Créer une conversation liée à un sinistre

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/messages`

**Body (raw JSON)** :
```json
{
  "conversationId": null,
  "destinataireId": "assure-user-id",
  "contenu": "Question spécifique sur le sinistre {{claim_id}}",
  "claimId": "{{claim_id}}"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Save claim-linked conversation ID", function () {
    var jsonData = pm.response.json();
    pm.environment.set("conversation_claim_id", jsonData.conversationId);
});
```

---

### Étape 3.2 : Créer une conversation générale

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/messages`

**Body (raw JSON)** :
```json
{
  "conversationId": null,
  "destinataireId": "assure-user-id",
  "contenu": "Question générale sur le processus d'indemnisation"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Save general conversation ID", function () {
    var jsonData = pm.response.json();
    pm.environment.set("conversation_general_id", jsonData.conversationId);
});
```

---

### Étape 3.3 : Vérifier les conversations avec filtre par sinistre

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations?claimId={{claim_id}}`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Only claim-linked conversations returned", function () {
    var jsonData = pm.response.json();
    var claimConvId = pm.environment.get("conversation_claim_id");
    var generalConvId = pm.environment.get("conversation_general_id");
    
    var hasClaimConv = jsonData.some(function(conv) {
        return conv.id === claimConvId;
    });
    var hasGeneralConv = jsonData.some(function(conv) {
        return conv.id === generalConvId;
    });
    
    pm.expect(hasClaimConv).to.be.true;
    pm.expect(hasGeneralConv).to.be.false;
});
```

---

### Étape 3.4 : Vérifier toutes les conversations (sans filtre)

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/messages/conversations`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("All conversations returned", function () {
    var jsonData = pm.response.json();
    var claimConvId = pm.environment.get("conversation_claim_id");
    var generalConvId = pm.environment.get("conversation_general_id");
    
    var hasClaimConv = jsonData.some(function(conv) {
        return conv.id === claimConvId;
    });
    var hasGeneralConv = jsonData.some(function(conv) {
        return conv.id === generalConvId;
    });
    
    pm.expect(hasClaimConv).to.be.true;
    pm.expect(hasGeneralConv).to.be.true;
});
```

---

# PARTIE 3 : TESTS DES NOTIFICATIONS

## Scénario 1 : Création automatique lors des transitions de statut

### Étape 1.1 : Connexion Gestionnaire

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

---

### Étape 1.2 : Changer le statut d'un sinistre

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/claims/{{claim_id}}/statut`

**Body (raw JSON)** :
```json
{
  "statut": "EN_REVISION"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status changed", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.statut).to.eql('EN_REVISION');
});
```

---

### Étape 1.3 : Connexion Assuré

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

---

### Étape 1.4 : Vérifier les notifications de l'assuré

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Assure received notification about status change", function () {
    var jsonData = pm.response.json();
    var statusNotification = jsonData.some(function(notif) {
        return notif.type === 'CLAIM_STATUS_CHANGE' || 
               notif.message.includes('statut') ||
               notif.message.includes('EN_REVISION');
    });
    pm.expect(statusNotification).to.be.true;
});
```

**Réponse attendue** :
```json
[
  {
    "id": "notif-123",
    "type": "CLAIM_STATUS_CHANGE",
    "message": "Le statut de votre sinistre a été mis à jour : EN_REVISION",
    "dateCreation": "2025-01-20T10:00:00",
    "lu": false,
    "claimId": "claim-id-123"
  }
]
```

---

### Étape 1.5 : Vérifier le compteur de non lus

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread-count`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Counter is greater than 0", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.above(0);
});
```

**Réponse attendue** :
```json
3
```

---

### Étape 1.6 : Changer le statut à nouveau (INDEMNISATION)

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

---

### Étape 1.7 : Mettre à jour le statut

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/claims/{{claim_id}}/statut`

**Body (raw JSON)** :
```json
{
  "statut": "INDEMNISATION"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});
```

---

### Étape 1.8 : Vérifier la nouvelle notification

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

---

### Étape 1.9 : Vérifier le compteur incrémenté

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread-count`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Counter increased", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.above(3);
});
```

---

## Scénario 2 : Marquer toutes comme lues

### Étape 2.1 : Vérifier les notifications non lues

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Has unread notifications", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.length).to.be.above(0);
});

pm.test("Save first notification ID", function () {
    var jsonData = pm.response.json();
    if (jsonData.length > 0) {
        pm.environment.set("notification_id", jsonData[0].id);
    }
});
```

---

### Étape 2.2 : Marquer une notification comme lue

**Méthode** : `PUT`  
**URL** : `{{base_url}}/api/notifications/{{notification_id}}/read`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Notification marked as read", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.lu).to.be.true;
});
```

**Réponse attendue** :
```json
{
  "id": "notif-123",
  "lu": true,
  "dateLecture": "2025-01-20T11:00:00"
}
```

---

### Étape 2.3 : Marquer toutes comme lues

**Méthode** : `PUT`  
**URL** : `{{base_url}}/api/notifications/read-all`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("All notifications marked as read", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include('marquées');
});
```

**Réponse attendue** :
```json
{
  "message": "5 notifications marquées comme lues"
}
```

---

### Étape 2.4 : Vérifier le compteur remis à zéro

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread-count`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Counter is now 0", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.eql(0);
});
```

---

### Étape 2.5 : Vérifier qu'il n'y a plus de notifications non lues

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("No unread notifications", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.length).to.eql(0);
});
```

---

## Scénario 3 : Mise à jour du badge

### Étape 3.1 : Créer une nouvelle notification

**Méthode** : `POST`  
**URL** : `{{base_url}}/api/auth/login`

**Body (raw JSON)** :
```json
{
  "email": "gestionnaire@example.com",
  "password": "password123"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save gestionnaire token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

---

### Étape 3.2 : Changer le statut du sinistre

**Méthode** : `PATCH`  
**URL** : `{{base_url}}/api/claims/{{claim_id}}/statut`

**Body (raw JSON)** :
```json
{
  "statut": "CLOTURE"
}
```

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});
```

---

### Étape 3.3 : Vérifier le badge de l'assuré

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

---

### Étape 3.4 : Vérifier le compteur mis à jour

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread-count`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Badge updated to 1", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.eql(1);
});
```

---

### Étape 3.5 : Vérifier la notification de clôture

**Méthode** : `GET`  
**URL** : `{{base_url}}/api/notifications/unread`

**Tests Postman** :
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Notification about closure", function () {
    var jsonData = pm.response.json();
    var closureNotification = jsonData.some(function(notif) {
        return notif.message.includes('CLOTURE') || 
               notif.message.includes('clôturé');
    });
    pm.expect(closureNotification).to.be.true;
});
```

---

# SCÉNARIOS DE TEST COMPLETS

## Scénario Complet 1 : Flux Fraude + Notifications

1. **Login Gestionnaire**
2. **Créer signalement fraude**
3. **Login Admin**
4. **Vérifier notification fraude reçue**
5. **Mettre en analyse**
6. **Résoudre comme FRAUDE_CONFIRMEE**
7. **Vérifier immuabilité**
8. **Vérifier statistiques**

## Scénario Complet 2 : Flux Messagerie + Notifications

1. **Login Gestionnaire**
2. **Envoyer message à assuré**
3. **Login Assuré**
4. **Vérifier notification message reçu**
5. **Lire messages**
6. **Vérifier compteur remis à zéro**
7. **Répondre au message**
8. **Login Gestionnaire**
9. **Vérifier notification réponse reçue**

## Scénario Complet 3 : Flux Statut Sinistre + Notifications

1. **Login Gestionnaire**
2. **Changer statut sinistre → EN_REVISION**
3. **Login Assuré**
4. **Vérifier notification statut**
5. **Changer statut → INDEMNISATION**
6. **Vérifier nouvelle notification**
7. **Marquer toutes comme lues**
8. **Vérifier compteur à 0**
9. **Changer statut → CLOTURE**
10. **Vérifier badge mis à jour**

---

# CHECKLIST DE TEST

## Fraude
- [ ] Créer un signalement de fraude
- [ ] Vérifier les signalements du gestionnaire
- [ ] Admin reçoit notification
- [ ] Mettre en cours d'analyse
- [ ] Résoudre comme FRAUDE_CONFIRMEE
- [ ] Vérifier immuabilité après résolution
- [ ] Tester statistiques de fraude
- [ ] Vérifier tendances

## Messagerie
- [ ] Envoyer message (création conversation)
- [ ] Vérifier incrémentation compteur non lus
- [ ] Lire messages
- [ ] Vérifier remise à zéro compteur
- [ ] Répondre au message
- [ ] Vérifier incrémentation pour destinataire
- [ ] Tester conversations liées à sinistre
- [ ] Tester conversations générales

## Notifications
- [ ] Vérifier création automatique sur transition statut
- [ ] Vérifier compteur non lus
- [ ] Marquer une notification comme lue
- [ ] Marquer toutes comme lues
- [ ] Vérifier compteur remis à zéro
- [ ] Vérifier mise à jour du badge
- [ ] Tester notifications multiples
