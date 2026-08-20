"""
Generation du jeu de donnees et entrainement du modele de risque.

IMPORTANT, a dire dans le rapport et a la soutenance : la plateforme est
neuve, il n'existe aucun historique de stages reels. Le jeu de donnees
est donc SYNTHETIQUE, produit par une regle metier a laquelle on ajoute
du bruit. C'est la pratique academique normale ; ce qui serait fautif,
c'est de le presenter comme des donnees reelles.

Regle de generation (avant bruit) : un stage est a risque quand
l'etudiant ne saisit plus son journal, accumule les refus, ou prend du
retard sur le volume d'heures attendu.

Lance automatiquement pendant le build de l'image Docker, ce qui rend
l'entrainement reproductible et evite de versionner un binaire .pkl.
"""

import json
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import pandas as pd
from joblib import dump
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

GRAINE = 42
TAILLE = 3000
DOSSIER_MODELE = Path(__file__).resolve().parent.parent / "model"

FEATURES = [
    "tasks_total",
    "tasks_pending",
    "tasks_rejected",
    "hours_ratio",
    "days_since_last_entry",
    "progress_ratio",
]


def generer_dataset(n: int = TAILLE, graine: int = GRAINE) -> pd.DataFrame:
    """Produit n stages fictifs et leur niveau de risque."""
    rng = np.random.default_rng(graine)

    progress = rng.uniform(0.05, 1.0, n)

    # Un stage avance compte logiquement plus de taches.
    tasks_total = np.clip(
        (progress * rng.normal(28, 9, n)).round(), 0, None
    ).astype(int)

    tasks_pending = np.clip(
        (tasks_total * rng.beta(1.6, 6, n)).round(), 0, None
    ).astype(int)

    tasks_rejected = np.clip(
        (tasks_total * rng.beta(1.2, 12, n)).round(), 0, None
    ).astype(int)

    hours_ratio = np.clip(rng.normal(0.85, 0.28, n), 0, 2.0)
    days_since_last_entry = np.clip(rng.exponential(4.5, n).round(), 0, 60).astype(int)

    df = pd.DataFrame(
        {
            "tasks_total": tasks_total,
            "tasks_pending": tasks_pending,
            "tasks_rejected": tasks_rejected,
            "hours_ratio": hours_ratio,
            "days_since_last_entry": days_since_last_entry,
            "progress_ratio": progress,
        }
    )

    # ---- Score de risque : la regle metier ----
    taux_refus = np.where(df.tasks_total > 0, df.tasks_rejected / df.tasks_total, 0)
    taux_attente = np.where(df.tasks_total > 0, df.tasks_pending / df.tasks_total, 0)

    score = (
        2.4 * np.clip(df.days_since_last_entry / 21, 0, 1)   # silence prolonge
        + 2.0 * np.clip(taux_refus / 0.35, 0, 1)              # refus repetes
        + 1.8 * np.clip((1 - df.hours_ratio) / 0.6, 0, 1)     # retard sur les heures
        + 0.9 * np.clip(taux_attente / 0.5, 0, 1)             # encadrant peu reactif
        + 1.2 * ((df.tasks_total < 3) & (df.progress_ratio > 0.4))  # journal quasi vide
    )

    # Le bruit represente tout ce que le modele ne peut pas voir :
    # contexte personnel, qualite de l'encadrement, difficulte du sujet.
    score = score + rng.normal(0, 0.45, n)

    df["risk"] = pd.cut(
        score, bins=[-np.inf, 1.6, 3.0, np.inf], labels=["LOW", "MEDIUM", "HIGH"]
    ).astype(str)

    return df


def entrainer() -> dict:
    df = generer_dataset()
    X, y = df[FEATURES], df["risk"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.25, random_state=GRAINE, stratify=y
    )

    modele = RandomForestClassifier(
        n_estimators=300,
        max_depth=10,
        min_samples_leaf=4,
        # Detecteur de risque : manquer un etudiant en difficulte coute plus
        # cher qu'une fausse alerte. On sur-pondere donc la classe HIGH,
        # ce qui privilegie le rappel sur la precision.
        class_weight={"LOW": 1.0, "MEDIUM": 1.0, "HIGH": 2.2},
        random_state=GRAINE,
        n_jobs=-1,
    )
    modele.fit(X_train, y_train)

    y_pred = modele.predict(X_test)
    rapport = classification_report(y_test, y_pred, output_dict=True, zero_division=0)

    DOSSIER_MODELE.mkdir(parents=True, exist_ok=True)
    dump({"model": modele, "features": FEATURES}, DOSSIER_MODELE / "risk_model.pkl")

    metriques = {
        "trained_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "dataset": {"size": len(df), "synthetic": True, "seed": GRAINE},
        "distribution": df["risk"].value_counts().to_dict(),
        "accuracy": round(rapport["accuracy"], 4),
        "macro_f1": round(rapport["macro avg"]["f1-score"], 4),
        "per_class": {
            c: {k: round(v, 4) for k, v in rapport[c].items()}
            for c in ("LOW", "MEDIUM", "HIGH")
            if c in rapport
        },
        "confusion_matrix": confusion_matrix(
            y_test, y_pred, labels=["LOW", "MEDIUM", "HIGH"]
        ).tolist(),
        "feature_importance": {
            f: round(float(i), 4)
            for f, i in sorted(
                zip(FEATURES, modele.feature_importances_),
                key=lambda t: t[1],
                reverse=True,
            )
        },
    }

    (DOSSIER_MODELE / "metrics.json").write_text(
        json.dumps(metriques, indent=2), encoding="utf-8"
    )
    return metriques


if __name__ == "__main__":
    m = entrainer()
    print(f"Jeu de donnees synthetique : {m['dataset']['size']} lignes")
    print(f"Repartition               : {m['distribution']}")
    print(f"Exactitude                : {m['accuracy']}")
    print(f"F1 macro                  : {m['macro_f1']}")
    print("Importance des variables  :")
    for f, i in m["feature_importance"].items():
        print(f"  {f:<24} {i}")
