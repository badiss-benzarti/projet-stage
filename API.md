# Référence API — via l'API Gateway

**Base URL : `http://localhost:8090`**

Tous les appels passent par le gateway. Les services métier (8081–8087) ne sont
volontairement pas exposés : ils ne sont joignables que par le réseau interne.

Authentification : en-tête `Authorization: Bearer <token>` sur tout sauf
`/api/auth/register`, `/api/auth/login` et `/api/auth/validate`.

Codes d'erreur : **401** jeton absent ou invalide · **403** rôle insuffisant ·
**404** ressource inconnue · **409** transition impossible · **400** règle métier
ou validation.

---

## Comptes de test

| Rôle | Email | Mot de passe |
|---|---|---|
| ADMIN | `admin@esprit.tn` | `Admin@2026` |
| ETUDIANT | `ahmed.bensalah@esprit.tn` | `Etudiant@2026` |
| ENTREPRISE | `soc.tech@partner.tn` | `Entreprise@2026` |
| ENCADRANT | `encadrant@partner.tn` | `Encadrant@2026` |
| CHEF_DEPARTEMENT_STAGE | `chef.stages@esprit.tn` | `ChefStage@2026` |
| CHEF_DEPARTEMENT_PEDAGOGIQUE | `chef.pedago@esprit.tn` | `ChefPedago@2026` |

---

## Routage du gateway

| Préfixe | Service | Port interne |
|---|---|---|
| `/api/auth/**` | auth-service | 8081 |
| `/api/users/**` | user-service | 8082 |
| `/api/internships/**` | internship-service | 8083 |
| `/api/evaluations/**` | evaluation-service | 8084 |
| `/api/documents/**` | document-service *(à venir)* | 8085 |
| `/api/notifications/**` | notification-service *(à venir)* | 8086 |
| `/api/ml/**` | ml-service *(à venir)* | 8087 |

---

## 1 — auth-service · `/api/auth`

| Méthode | Chemin | Rôle | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Inscription → 201 + jeton |
| POST | `/api/auth/login` | public | Connexion → 200 + jeton |
| GET | `/api/auth/me` | authentifié | Profil du porteur du jeton |
| POST | `/api/auth/validate` | public | Vérifie un jeton (débogage) |

```json
// POST /api/auth/register
{ "email": "x@esprit.tn", "password": "MotDePasse@2026",
  "firstName": "Prenom", "lastName": "Nom", "role": "ETUDIANT" }

// POST /api/auth/login
{ "email": "admin@esprit.tn", "password": "Admin@2026" }
```

Rôles valides : `ETUDIANT` `ENTREPRISE` `ENCADRANT` `CHEF_DEPARTEMENT_STAGE`
`CHEF_DEPARTEMENT_PEDAGOGIQUE` `ADMIN`

---

## 2 — user-service · `/api/users`

### Étudiants

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/users/students/me` | ETUDIANT |
| GET | `/api/users/students/me` | ETUDIANT |
| PUT | `/api/users/students/me` | ETUDIANT |
| GET | `/api/users/students?departement=&classe=&page=0&size=20` | CHEF_DEPT_STAGE · CHEF_DEPT_PEDAGO · ADMIN |
| GET | `/api/users/students/{id}` | + ENCADRANT |

```json
{ "firstName": "Ahmed", "lastName": "Ben Salah", "email": "ahmed.bensalah@esprit.tn",
  "phone": "20123456", "cin": "12345678", "classe": "4SAE3", "departement": "Genie Logiciel" }
```

### Entreprises

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/users/companies/me` | ENTREPRISE |
| GET | `/api/users/companies/me` | ENTREPRISE |
| PUT | `/api/users/companies/me` | ENTREPRISE |
| GET | `/api/users/companies` | authentifié (annuaire) |
| GET | `/api/users/companies/{id}` | authentifié |
| GET | `/api/users/companies/{id}/supervisors` | authentifié |

```json
{ "name": "SocieteTech Partner", "address": "Rue du Lac, Tunis",
  "phone": "71234567", "email": "contact@partner.tn", "taxId": "1234567A" }
```

### Encadrants

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/users/supervisors` | ENTREPRISE |
| GET | `/api/users/supervisors/me` | ENCADRANT |
| GET | `/api/users/supervisors/{id}` | ENTREPRISE · chefs · ADMIN |
| DELETE | `/api/users/supervisors/{id}` | ENTREPRISE (les siens) |

```json
{ "userId": 5, "firstName": "Youssef", "lastName": "Gharbi",
  "email": "encadrant@partner.tn", "phone": "98765432", "position": "Ingenieur DevOps" }
```

---

## 3 — internship-service · `/api/internships` ⭐ Module 1

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/internships` | ETUDIANT — crée un brouillon |
| PUT | `/api/internships/{id}` | ETUDIANT — brouillon uniquement |
| DELETE | `/api/internships/{id}` | ETUDIANT — brouillon uniquement |
| GET | `/api/internships/mine` | ETUDIANT |
| GET | `/api/internships/company?status=` | ENTREPRISE |
| GET | `/api/internships/supervision` | ENCADRANT |
| GET | `/api/internships/department?status=` | chefs · ADMIN |
| GET | `/api/internships/statistics` | chefs · ADMIN |
| GET | `/api/internships/{id}` | parties prenantes |
| **POST** | **`/api/internships/{id}/transition`** | selon la transition |

```json
// POST /api/internships
{ "type": "PFE", "title": "Plateforme de gestion des stages",
  "description": "Microservices Spring Boot et Angular",
  "academicYear": "2026-2027", "companyId": 1, "companyName": "SocieteTech Partner",
  "startDate": "2026-07-01", "endDate": "2026-12-31" }
```

### Le workflow

```
DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED -> COMPANY_PENDING -> ACCEPTED -> IN_PROGRESS -> COMPLETED
                           |                            |
                           +-> REJECTED                 +-> REFUSED
```

| Transition | Rôle | Corps |
|---|---|---|
| `SUBMITTED` | ETUDIANT | `{"target":"SUBMITTED"}` |
| `UNDER_REVIEW` | CHEF_DEPT_STAGE | `{"target":"UNDER_REVIEW"}` |
| `APPROVED` | CHEF_DEPT_STAGE | `{"target":"APPROVED"}` |
| `REJECTED` | CHEF_DEPT_STAGE | `{"target":"REJECTED","comment":"motif obligatoire"}` |
| `COMPANY_PENDING` | CHEF_DEPT_STAGE | `{"target":"COMPANY_PENDING"}` |
| `ACCEPTED` | ENTREPRISE | `{"target":"ACCEPTED","supervisorId":1,"supervisorName":"Youssef Gharbi"}` |
| `REFUSED` | ENTREPRISE | `{"target":"REFUSED","comment":"motif obligatoire"}` |
| `IN_PROGRESS` | CHEF_DEPT_STAGE · ENTREPRISE | `{"target":"IN_PROGRESS"}` |
| `COMPLETED` | ENCADRANT · CHEF_DEPT_STAGE | `{"target":"COMPLETED"}` |

La réponse contient `availableActions` : les transitions que **le rôle courant**
peut déclencher depuis l'état actuel. Le frontend s'en sert pour afficher les
bons boutons sans dupliquer la règle métier.

### Demandes de convention et lettre d'affectation

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/internships/{id}/requests` | ETUDIANT |
| GET | `/api/internships/{id}/requests` | authentifié |
| GET | `/api/internships/requests/pending` | CHEF_DEPT_STAGE · ADMIN |
| PATCH | `/api/internships/requests/{requestId}` | CHEF_DEPT_STAGE · ADMIN |

```json
// POST  : { "type": "CONVENTION" }  ou  { "type": "LETTRE_AFFECTATION" }
// PATCH : { "status": "ISSUED" }    ou  { "status": "REJECTED", "reason": "motif" }
```

Le dossier doit être approuvé, et une seule demande du même type peut être en
attente.

---

## 4 — evaluation-service · `/api/evaluations` ⭐ Module 2

### Journal de stage

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/evaluations/internships/{id}/tasks` | ETUDIANT |
| GET | `/api/evaluations/internships/{id}/tasks?status=` | authentifié |
| GET | `/api/evaluations/internships/{id}/tasks/summary` | authentifié |
| PUT | `/api/evaluations/tasks/{taskId}` | ETUDIANT |
| DELETE | `/api/evaluations/tasks/{taskId}` | ETUDIANT |
| PATCH | `/api/evaluations/tasks/{taskId}/decision` | ENCADRANT · ADMIN |

```json
// POST tasks
{ "taskDate": "2026-08-19", "title": "Mise en place du pipeline CI",
  "description": "GitHub Actions, build Maven et tests", "hours": 7 }

// PATCH decision
{ "status": "VALIDATED" }
{ "status": "REJECTED", "reason": "motif obligatoire" }
```

Contraintes : stage `IN_PROGRESS`, durée entre 0,5 et 12 h, date non future et
postérieure au début du stage. `status` accepte `PENDING` `VALIDATED` `REJECTED`.

### Grille et note

| Méthode | Chemin | Rôle |
|---|---|---|
| PUT | `/api/evaluations/internships/{id}` | ENCADRANT · ADMIN |
| POST | `/api/evaluations/internships/{id}/submit` | ENCADRANT · ADMIN |
| GET | `/api/evaluations/internships/{id}` | parties prenantes |
| GET | `/api/evaluations/statistics` | chefs · ADMIN |

```json
{ "technicalScore": 16, "qualityScore": 15, "autonomyScore": 16,
  "communicationScore": 15, "punctualityScore": 16,
  "globalComment": "Appreciation globale, obligatoire pour valider",
  "remarks": "Remarques libres" }
```

Barème : technique 30 % · qualité 20 % · autonomie 20 % · communication 15 % ·
assiduité 15 %. Note arrondie à 0,25, **jamais saisie, toujours recalculée**.
L'étudiant ne voit sa note qu'après `submit`.

### Exports

| Méthode | Chemin | Rôle | Produit |
|---|---|---|---|
| GET | `/api/evaluations/export/xlsx` | chefs · ADMIN | `notes-stages-AAAA-MM-JJ.xlsx` |
| GET | `/api/evaluations/internships/{id}/journal/pdf` | parties prenantes | `journal-stage-{id}.pdf` |

### Réclamations avec bouclage

| Méthode | Chemin | Rôle |
|---|---|---|
| POST | `/api/evaluations/claims` | ETUDIANT |
| POST | `/api/evaluations/claims/{id}/messages` | ETUDIANT · chefs |
| POST | `/api/evaluations/claims/{id}/take` | CHEF_DEPT_PEDAGO · ADMIN |
| POST | `/api/evaluations/claims/{id}/close` | CHEF_DEPT_PEDAGO · ADMIN |
| GET | `/api/evaluations/claims/{id}` | parties prenantes |
| GET | `/api/evaluations/claims/mine` | ETUDIANT |
| GET | `/api/evaluations/claims?status=` | chefs · ADMIN |

```json
// POST claims
{ "internshipId": 3, "type": "NOTE", "subject": "Contestation de la note",
  "message": "Corps du premier message" }

// POST messages  et  POST close
{ "content": "Reponse ou relance" }
```

```
OPEN -> IN_REVIEW -> RESPONDED <-> REOPENED -> CLOSED
```

Type : `NOTE` `TACHE` `AUTRE`. Une réponse du département passe en `RESPONDED`,
une relance de l'étudiant en `REOPENED` et incrémente `reopenCount`. Seul le chef
pédagogique clôture ; une réclamation close n'accepte plus de message.

---

## Interfaces techniques

| URL | Contenu |
|---|---|
| `http://localhost:8761` | Dashboard Eureka |
| `http://localhost:8888/{service}/default` | Configuration servie par le config-server |
| `http://localhost:8090/actuator/health` | Santé du gateway |
| `http://localhost:15672` | RabbitMQ (guest / guest) |
| `localhost:3306` | MySQL (root / root) |
