# Plateforme de gestion des stages

Application de gestion et de traitement des stages **PFE** et **stages d'été** :
demande de stage, workflow de validation multi-acteurs, journal de stage en ligne,
grille d'évaluation avec calcul automatique de la note, gestion documentaire,
réclamations, notifications, monitoring et prédiction du risque par apprentissage automatique.

**Projet Intégré** — Spring Boot · Angular · Docker · CI/CD · Prometheus/Grafana · MLA

---

## Modules approfondis

| Module | Service | Périmètre |
|---|---|---|
| ⭐ **1 — Demandes & workflow** | `internship-service` | Demande PFE / Été, entreprise d'accueil, encadrant, convention, lettre d'affectation, approbation service stage, acceptation entreprise |
| ⭐ **2 — Suivi & évaluation** | `evaluation-service` | Journal de stage, validation des tâches, grille d'évaluation, note automatique, export XLSX, réclamation avec bouclage, journal PDF |

---

## Architecture

```
                         Angular  (4200)
                             │
                             ▼
                      API Gateway  (8090)
                             │
        ┌────────────┬───────┼───────┬────────────┬────────────┐
        ▼            ▼       ▼       ▼            ▼            ▼
      auth         user  internship evaluation document  notification
      8081         8082     8083       8084       8085        8086
        └────────────┴───────┴───────┴────────────┴────────────┘
                             │
              Eureka (8761)  ·  Config Server (8888)
                             │
              MySQL · RabbitMQ · Prometheus · Grafana
                             │
                        ml-service (8087)
```

Chaque microservice est un **projet autonome** : son propre `pom.xml`, son propre
`Dockerfile`, sa propre base de données. Aucun POM parent, aucune bibliothèque
partagée. Les services ne se connaissent qu'à l'exécution, via **Eureka** et le
**gateway**.

---

## Ports

> Le gateway écoute sur **8090** et non 8080 : le port 8080 est occupé par le listener HTTP d'Oracle XDB sur la machine de développement.

| Composant | Port | Exposé | Base |
|---|---|---|---|
| Angular | 4200 | oui | — |
| API Gateway | 8090 | oui | — |
| auth-service | 8081 | non | `auth_db` |
| user-service | 8082 | non | `user_db` |
| internship-service ⭐ | 8083 | non | `internship_db` |
| evaluation-service ⭐ | 8084 | non | `evaluation_db` |
| document-service | 8085 | non | `document_db` |
| notification-service | 8086 | non | `notification_db` |
| ml-service | 8087 | non | — |
| Eureka | 8761 | oui | — |
| Config Server | 8888 | oui | — |
| MySQL | 3306 | oui | 6 schémas |
| RabbitMQ / Management | 5672 / 15672 | oui | — |
| Prometheus | 9090 | oui | — |
| Grafana | 3000 | oui | — |
| SonarQube | 9000 | oui | PostgreSQL dédié |

Les services métier ne publient aucun port vers l'hôte : ils ne sont joignables
qu'à travers le gateway, sur le réseau interne Docker.

---

## Rôles

`ETUDIANT` · `ENTREPRISE` · `ENCADRANT` · `CHEF_DEPARTEMENT_STAGE` · `CHEF_DEPARTEMENT_PEDAGOGIQUE` · `ADMIN`

---

## Workflow d'un stage

```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → COMPANY_PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
                         │                          │
                         └→ REJECTED                └→ REFUSED
```

Les deux sorties latérales exigent un motif obligatoire. Chaque transition est
réservée à un rôle précis, contrôlé côté backend.

### Réclamation (bouclage)

```
OPEN → IN_REVIEW → RESPONDED ⇄ REOPENED → CLOSED
```

---

## Grille d'évaluation

Note de stage calculée automatiquement à partir de la grille remplie par
l'encadrant entreprise. Chaque critère est noté **sur 20**.

| Critère | Poids |
|---|---|
| Compétences techniques | 30 % |
| Qualité du travail rendu | 20 % |
| Autonomie & initiative | 20 % |
| Communication & intégration | 15 % |
| Assiduité & ponctualité | 15 % |

**Note finale** = somme pondérée, **arrondie à 0,25 près**.

L'appréciation globale et les remarques sont un champ libre obligatoire,
distinct de la note.

---

## Organisation Git

Un dépôt unique, **une branche par microservice**.

```
main                    intégration : tous les dossiers y coexistent
├── discovery-server
├── config-server
├── api-gateway
├── auth-service
├── user-service
├── internship-service      ⭐
├── evaluation-service      ⭐
├── document-service
├── notification-service
├── ml-service
├── stage-platform
└── devops
```

Chaque branche est créée **depuis `main`** et fusionnée dans `main` dès que son
point de contrôle est validé.

```bash
git checkout main && git pull
git checkout -b auth-service
# ... développement ...
git add auth-service/ && git commit -m "feat(auth): login JWT"
git push -u origin auth-service
```

---

## Démarrage

```bash
# Infrastructure seule (développement)
docker compose up -d mysql rabbitmq

# Plateforme complète
docker compose up --build

# Avec SonarQube
docker compose --profile quality up -d
```

En développement, les services tournent dans IntelliJ (un projet par fenêtre) et
se connectent à l'infrastructure conteneurisée. Seul le service en cours de
développement est lancé depuis l'IDE ; les autres tournent en conteneur.

---

## Stack

**Backend** Spring Boot 3 · Spring Cloud (Eureka, Config, Gateway) · Spring Security + JWT · Spring Data JPA · RabbitMQ · Apache POI · openhtmltopdf
**Frontend** Angular · TypeScript
**ML** Python · FastAPI · scikit-learn
**Données** MySQL 8
**DevOps** Docker · Docker Compose · GitHub Actions · SonarQube · JaCoCo · Testcontainers
**Monitoring** Spring Actuator · Micrometer · Prometheus · Grafana
