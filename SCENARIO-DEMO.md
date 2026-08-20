# Scénario de démonstration

**Durée : 12 à 15 minutes.** Un seul récit continu, du dépôt de la demande
jusqu'à la clôture de la réclamation.

Base vidée le 20 août 2026 : aucun stage, aucune tâche, aucune note.
Les six comptes et les trois profils (étudiant, entreprise, encadrant)
sont conservés.

---

## Avant de commencer

Ouvrez **http://localhost:4200** et vérifiez que ces quatre onglets répondent :

| URL | Attendu |
|---|---|
| `localhost:4200` | Page de connexion |
| `localhost:8761` | Eureka — 9 services `UP` |
| `localhost:8090/actuator/health` | `{"status":"UP"}` |
| `localhost:15672` | RabbitMQ (guest / guest) |

**Faites une passe à blanc** sur chaque écran avant la vraie démonstration :
le premier appel à chaque service prend 5 à 10 secondes de mise en chauffe,
les suivants 20 à 70 ms.

Pour changer de compte : avatar en haut à droite, puis **Se déconnecter**.
Les boutons de comptes de démonstration sur la page de connexion
pré-remplissent le formulaire d'un clic.

---

## Acte 1 — L'étudiant dépose sa demande

**Connexion :** `ahmed.bensalah@esprit.tn` / `Etudiant@2026`

1. Le tableau de bord affiche **Aucun dossier de stage**.
   Cliquez sur **Déposer une demande**.

2. Remplissez le formulaire :

   | Champ | Valeur |
   |---|---|
   | Type | **Projet de fin d'études** |
   | Intitulé | `Plateforme de gestion des stages` |
   | Description | `Architecture microservices Spring Boot, frontend Angular, CI/CD et apprentissage automatique` |
   | Année | `2026-2027` |
   | Entreprise | **SocieteTech Partner** |
   | Début | **01/07/2026** |
   | Fin | **31/12/2026** |

   > La date de début doit être **dans le passé** : le journal refuse toute
   > tâche antérieure au début du stage. Avec un début en septembre, vous ne
   > pourriez rien saisir aujourd'hui.

3. **Créer le brouillon**, puis retour au **Tableau de bord**.
   La frise affiche l'étape 1, état **Brouillon**, et un seul bouton :
   **Soumettre la demande**.

4. Cliquez dessus. La frise avance, état **Soumise**, plus aucun bouton :
   la balle est dans le camp du service des stages.

**À souligner :** les boutons ne sont pas codés en dur. Ils viennent du
champ `availableActions` calculé par le backend selon l'état et le rôle.

---

## Acte 2 — Le service des stages instruit

**Connexion :** `chef.stages@esprit.tn` / `ChefStage@2026`

5. Le tableau de bord annonce **1 demande à instruire** en ambre.
   Cliquez la carte, puis **Instruire** sur la ligne d'Ahmed.
   Le panneau latéral s'ouvre : état, entreprise, période, sujet.

6. **Prendre en charge** → état **En cours d'examen**.

7. Deux boutons apparaissent. **Montrez d'abord le refus** :
   cliquez **Refuser la demande**, un champ motif s'ouvre.
   Cliquez **Annuler**.

8. **Approuver la demande** → état **Approuvée**.

9. **Transmettre à l'entreprise** → **En attente entreprise**.

**À souligner :** le motif est obligatoire pour les deux refus du workflow,
côté interface **et** côté serveur.

---

## Acte 3 — L'entreprise accepte

**Connexion :** `soc.tech@partner.tn` / `Entreprise@2026`

10. Le tableau de bord annonce **1 demande en attente**.
    Allez dans **Demandes reçues**.

11. **Accepter le stagiaire** : un sélecteur d'encadrant apparaît,
    **Youssef Gharbi — Ingénieur DevOps**.

    > Sans encadrant déclaré, l'acceptation est impossible : c'est lui qui
    > validera le journal et remplira la grille.

12. **Confirmer l'acceptation**. Puis **Mes stagiaires** → **Détail** :
    l'historique montre chaque transition et son auteur.

*(Facultatif, écran **Encadrants** : « Déclarer un encadrant » crée le
compte ET rattache le profil, en deux appels. Utilisez un email neuf.)*

---

## Acte 4 — Démarrage et journal

13. **Repassez en `chef.stages@esprit.tn`** → *Demandes* → filtre
    **Acceptée** → **Instruire** → **Démarrer le stage**.
    État : **En cours**.

**Connexion :** `ahmed.bensalah@esprit.tn`

14. **Journal de stage** — la saisie est ouverte. Ajoutez quatre tâches :

    | Date | Tâche | Heures |
    |---|---|---|
    | 13/08/2026 | `Analyse du besoin et cadrage` | 7 |
    | 14/08/2026 | `Modélisation des bases de données` | 6.5 |
    | 17/08/2026 | `Mise en place du pipeline CI` | 7 |
    | 19/08/2026 | `Premiers écrans du prototype` | 5 |

    Les compteurs passent à **4 saisies · 4 en attente**.

15. Testez les garde-fous : une tâche **datée demain** est refusée,
    une tâche de **20 heures** aussi, avec le message du serveur.

---

## Acte 5 — L'encadrant valide et note

**Connexion :** `encadrant@partner.tn` / `Encadrant@2026`

16. Le tableau de bord montre Ahmed et, après quelques secondes, une
    **jauge de risque** avec les indicateurs qui l'expliquent.

17. **Journaux à valider** : validez les trois premières tâches.
    Sur la quatrième, **Refuser** avec le motif
    `Précisez les outils de maquettage utilisés et joignez les captures d'écran`.

18. **Grille d'évaluation**. Placez les curseurs :

    | Critère | Note |
    |---|---|
    | Compétences techniques | **16** |
    | Qualité du travail rendu | **15** |
    | Autonomie et initiative | **16** |
    | Communication et intégration | **15** |
    | Assiduité et ponctualité | **16** |

    La note se recalcule **en direct** : **15,75 / 20**.

    > (16×30 + 15×20 + 16×20 + 15×15 + 16×15) / 100 = 15,65,
    > arrondi au quart de point → **15,75**.

19. Appréciation globale, obligatoire :
    `Stagiaire autonome et rigoureux. Bonne maîtrise des outils DevOps, intégration rapide dans l'équipe.`

20. **Enregistrer le brouillon**, puis **Valider l'évaluation** :
    note définitive, formulaire figé.

---

## Acte 6 — L'étudiant conteste

**Connexion :** `ahmed.bensalah@esprit.tn`

21. **Journal de stage** : la tâche refusée affiche son motif et un bouton
    **Corriger et renvoyer**. Cliquez, complétez la description, renvoyez.
    La tâche repasse **En attente** : c'est la boucle de correction.

22. **Ma note** : le détail du calcul ligne par ligne, la contribution de
    chaque critère, et l'appréciation de l'encadrant.

23. Retour au **Journal de stage** → **Télécharger en PDF**.

24. **Réclamations** → **Nouvelle réclamation** :
    - Objet : **Contestation de note**
    - Sujet : `Contestation de la note d'assiduité`
    - Message : `Je conteste la note d'assiduité : mes retards étaient justifiés par un certificat médical.`

---

## Acte 7 — Le bouclage de la réclamation

**Connexion :** `chef.pedago@esprit.tn` / `ChefPedago@2026`

25. Le tableau de bord montre **1 note validée**, moyenne **15,75**, et
    **1 réclamation en cours**.

26. **Réclamations** : le fil s'ouvre et passe automatiquement
    **En cours d'examen**. Répondez :
    `Après vérification auprès de l'encadrant, deux retards ne sont pas justifiés.`

27. **Repassez en étudiant** → *Réclamations* → relancez :
    `Je joins le certificat médical couvrant ces deux journées, pouvez-vous réexaminer ?`

    → Statut **Relancée**, compteur **1 relance**.
    **C'est le bouclage exigé par le cahier des charges.**

28. **Repassez en `chef.pedago@esprit.tn`** → répondez, puis
    **Clôturer la réclamation**. Le fil se ferme, plus aucune réponse possible.

---

## Acte 8 — Les documents

**Connexion :** `ahmed.bensalah@esprit.tn`

29. **Documents** → type **Convention de stage** → déposez un PDF
    (le journal téléchargé à l'étape 23 fait l'affaire).

30. Testez le filtre : déposez un fichier **.txt**, il est refusé avec son
    type MIME.

**Connexion :** `chef.stages@esprit.tn`

31. **Documents à valider** : **Consulter** télécharge le fichier.
    **Refuser** avec le motif `Le cachet de l'entreprise est absent en page 2`.

32. **Repassez en étudiant** : le document affiche son motif. Redéposez-le.

33. **Repassez en `chef.stages@esprit.tn`** → **Accepter**.

---

## Acte 9 — Le volet apprentissage automatique

**Connexion :** `chef.pedago@esprit.tn`

34. **Suivi du risque** : la jauge d'Ahmed, avec le niveau, la confiance du
    modèle, et **les indicateurs qui expliquent la prédiction**.

    > Le modèle ne reçoit que six compteurs, aucune donnée nominative.
    > L'étudiant ne voit pas son propre score : le lui afficher serait
    > contre-productif, et discutable éthiquement.

35. Montrez la qualité du modèle : **http://localhost:8087/docs**
    (Swagger de FastAPI), puis `GET /metrics-model` — exactitude, F1 par
    classe, matrice de confusion, importance des variables.

---

## Acte 10 — Exports et infrastructure

36. **Notes et évaluations** → **Exporter en XLSX**. Ouvrez le fichier :
    en-têtes, notes par critère, note finale, et la **moyenne en formule Excel**.

37. **Notifications** dans la barre latérale : les événements du workflow
    remontés par RabbitMQ, compteur non lu, marquage individuel et global.

38. Terminez sur l'infrastructure :

    | URL | À montrer |
    |---|---|
    | `localhost:8761` | Les 9 services enregistrés dans Eureka |
    | `localhost:8888/api-gateway/default` | La configuration servie à distance |
    | `localhost:15672` | La file `notifications.queue`, 0 message en souffrance |
    | GitHub Actions | Le pipeline vert |
    | SonarQube `:9000` | La Quality Gate |
    | Grafana `:3000` | Le tableau de bord de supervision |

---

## Les cinq phrases qui comptent

**Sur le workflow** — un seul endpoint `POST /{id}/transition` gouverne les
dix états. La table de transitions déclare l'état source, l'état cible, les
rôles habilités et l'obligation de motif. Ajouter un état ne demande aucune
route ni aucune ligne d'Angular : le frontend affiche les
`availableActions` que le serveur lui renvoie.

**Sur la note** — elle n'est jamais saisie. Le serveur la recalcule à chaque
enregistrement à partir des coefficients du `config-repo`. Un encadrant ne
peut pas imposer une note incohérente avec sa propre grille. Et si les poids
ne totalisent pas 100, le service **refuse de démarrer**.

**Sur l'asynchrone** — `internship-service` ne connaît pas l'existence de
`notification-service`. Il publie sur un échange topic, point. Une panne du
bus ne fait jamais échouer une transition métier.

**Sur le modèle** — le jeu de données est **synthétique**, 3 000 lignes, et
c'est assumé : la plateforme est neuve, il n'existe aucun historique de
stages réels. La classe « risque élevé » est sur-pondérée, parce que manquer
un étudiant en difficulté coûte plus cher qu'une fausse alerte.

**Sur la sécurité** — le frontend masque des boutons, il ne protège rien.
Chaque service applique `@PreAuthorize` en plus de la validation du jeton au
gateway, et les services métier ne publient aucun port vers l'hôte.

---

## Remettre la base à zéro

Fichier `docker/reset-demo.sql`, à rejouer entre deux répétitions :

```powershell
docker exec -i gs-mysql mysql -uroot -proot < docker/reset-demo.sql
```

Les comptes et les profils sont conservés : l'interface n'a pas encore
d'écran d'inscription ni de création de profil étudiant.
