# État du projet — point de reprise

**Dernière mise à jour : 20 août 2026** · dépôt `github.com/badiss-benzarti/projet-stage`
Soutenance : semaine du **7 septembre 2026**.

---

## En une phrase

Backend et frontend sont **terminés et fonctionnels** ; les 18 fonctionnalités
du cahier des charges sont couvertes de bout en bout. **Il reste le volet
DevOps, entièrement à faire.**

---

## Ce qui est fait

### Backend — 10 services

| Service | Port | Contenu |
|---|---|---|
| `discovery-server` | 8761 | Eureka |
| `config-server` | 8888 | Mode natif, lit `config-repo/` |
| `api-gateway` | 8090 | WebFlux, 7 routes, CORS |
| `auth-service` | 8081 | JWT HS384, BCrypt, 6 rôles, seeder ADMIN |
| `user-service` | 8082 | Étudiants, entreprises, encadrants |
| `internship-service` ⭐ | 8083 | Machine à états 10 statuts, demandes de documents, RabbitMQ |
| `evaluation-service` ⭐ | 8084 | Journal, grille, note auto, XLSX, PDF, réclamations, risque |
| `document-service` | 8085 | Dépôt, validation, attestation PDF |
| `notification-service` | 8086 | Consommateur RabbitMQ |
| `ml-service` | 8087 | FastAPI, RandomForest, conteneur `gs-ml` |

64 endpoints REST · 44 tests unitaires dans 7 classes · Dockerfile pour chacun.

### Frontend — Angular 21, 21 écrans

Standalone, OnPush, signals, `@if`/`@for`, `inject()`, Tailwind 4, zéro `any`,
zéro avertissement de build, tout en chargement paresseux.

Espaces : étudiant (7 écrans), entreprise (5), encadrant (3),
département stages (4), département pédagogique (4), plus connexion,
inscription et notifications.

### Le cahier des charges : 18/18

Toutes les fonctionnalités ont un backend **et** une interface.

---

## Ce qui reste — le DevOps

| Tâche | État |
|---|---|
| `docker-compose` complet (17 conteneurs) | ❌ ne contient que MySQL, RabbitMQ, SonarQube |
| Dockerfile Angular + `nginx.conf` | ❌ |
| GitHub Actions (`ci.yml`, `docker.yml`) | ❌ `.github/workflows/` est vide |
| JaCoCo | ❌ couverture SonarQube à 0 % sans lui |
| SonarQube branché | ❌ conteneur déclaré, jamais lancé |
| Prometheus + `prometheus.yml` | ❌ dossier vide |
| Grafana + dashboard | ❌ dossier vide |
| Tests contrôleurs et Testcontainers | ❌ seulement des tests unitaires |

C'est la seule exigence explicite de l'énoncé encore à zéro.

---

## Comment tout relancer

### 1. Infrastructure
```powershell
cd C:\Users\GIGABYTE\gestion-stages
docker compose up -d          # MySQL + RabbitMQ
docker start gs-ml            # service de prédiction
```

### 2. Les services Spring, dans cet ordre
```
discovery-server  8761   ← toujours en premier
config-server     8888
api-gateway       8090
auth-service      8081
user-service      8082
internship-service 8083
evaluation-service 8084
document-service  8085
notification-service 8086
```
Depuis IntelliJ : une fenêtre par dossier, `-Xmx256m` en VM options.
En ligne de commande : `java -Xmx320m -jar <service>/target/*.jar`

### 3. Frontend
```powershell
cd stage-platform
npx ng serve        # http://localhost:4200
```

> **Ne changez jamais de branche Git pendant qu'`ng serve` tourne** : le
> watcher perd l'arborescence et sert un bundle périmé sans le signaler.

---

## Comptes

| Rôle | Email | Mot de passe |
|---|---|---|
| ADMIN | `admin@esprit.tn` | `Admin@2026` |
| ETUDIANT | `ahmed.bensalah@esprit.tn` | `Etudiant@2026` |
| ETUDIANT | `sarra.jelassi@esprit.tn` | `Etudiant@2026` |
| ENTREPRISE | `soc.tech@partner.tn` | `Entreprise@2026` |
| ENCADRANT | `encadrant@partner.tn` | `Encadrant@2026` |
| CHEF DÉPT STAGE | `chef.stages@esprit.tn` | `ChefStage@2026` |
| CHEF DÉPT PÉDAGO | `chef.pedago@esprit.tn` | `ChefPedago@2026` |

---

## Décisions structurantes à ne pas remettre en cause

1. **Un seul endpoint de workflow** `POST /api/internships/{id}/transition`.
   La table de transitions porte toute la règle ; le frontend n'affiche que
   les `availableActions` renvoyées par le serveur.

2. **La note n'est jamais saisie.** `ScoringService` la recalcule à chaque
   enregistrement depuis les coefficients du `config-repo`. Si les poids ne
   totalisent pas 100, le service refuse de démarrer.

3. **Aucune bibliothèque partagée.** Le filtre JWT est dupliqué dans chaque
   service, et le contrat d'événement RabbitMQ aussi. C'est délibéré : une
   `common-lib` imposerait un `mvn install` manuel et casserait les
   Dockerfiles indépendants.

4. **`prefer-ip-address` et Eureka.** Les instances s'enregistrent sur
   `192.168.56.1` (adaptateur VirtualBox). Sans conséquence en local,
   à surveiller en Docker.

5. **L'entreprise d'accueil n'a pas besoin de compte.** Le chef de
   département peut répondre à sa place, et l'encadrant peut n'être qu'un
   nom. C'est ce qui rend le point 3 du cahier réalisable.

6. **Spring Boot 4.1.0 / Spring Cloud 2025.1.2**, aligné sur le projet
   `webapp` précédent. Attention : `starter-web` s'appelle désormais
   `starter-webmvc`, et le gateway est `gateway-server-webflux`.

---

## Pièges rencontrés, à ne pas repayer

- `generated` est un **mot réservé MySQL 8** → colonne `is_generated`.
- Le publieur RabbitMQ estampille ses messages avec sa propre classe :
  le consommateur doit forcer `TypePrecedence.INFERRED`.
- Spring Security renvoie **403 quand le jeton manque** : un
  `AuthenticationEntryPoint` rétablit le 401, ce dont dépend l'intercepteur
  Angular.
- Les téléchargements passent par un **blob** : un `<a href>` ne porte pas
  l'en-tête `Authorization`.
- Les listes paginées Spring **ne trient pas** par défaut : `mine()` renvoyait
  le dossier le plus ancien.
- **Mise en chauffe** : le premier appel à chaque service prend 5 à 10 s.
  Faire une passe à blanc avant toute démonstration.

---

## Documents du dépôt

| Fichier | Contenu |
|---|---|
| `API.md` | Toutes les routes, rôles, corps JSON, codes d'erreur |
| `api-tests.http` | 76 requêtes exécutables dans IntelliJ |
| `SCENARIO-DEMO.md` | Démonstration en 38 étapes |
| `docker/reset-demo.sql` | Vide les données métier, garde les comptes |
| `C:\Users\GIGABYTE\PLAN-GESTION-STAGES.md` | Le plan d'ensemble (hors dépôt) |

---

## Prochaine étape

**Le DevOps**, dans cet ordre :
1. Dockerfile Angular + `nginx.conf`
2. `docker-compose` complet, healthchecks et ordre de démarrage
3. GitHub Actions : matrice sur les 9 services Spring
4. JaCoCo puis SonarQube
5. Prometheus et un dashboard Grafana
