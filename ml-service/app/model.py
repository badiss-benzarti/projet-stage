"""Chargement du modele et prediction."""

import json
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import pandas as pd
from joblib import load

DOSSIER_MODELE = Path(__file__).resolve().parent.parent / "model"
CHEMIN_MODELE = DOSSIER_MODELE / "risk_model.pkl"
CHEMIN_METRIQUES = DOSSIER_MODELE / "metrics.json"

# Libelles lisibles pour expliquer une prediction a l'utilisateur.
LIBELLES = {
    "tasks_total": "volume de taches saisies",
    "tasks_pending": "taches en attente de validation",
    "tasks_rejected": "taches refusees",
    "hours_ratio": "heures validees par rapport aux attendues",
    "days_since_last_entry": "jours sans saisie",
    "progress_ratio": "avancement du stage",
}


class RiskModel:
    """
    Enveloppe du modele entraine.

    Charge une seule fois au demarrage : deserialiser un RandomForest a
    chaque requete couterait plus cher que la prediction elle-meme.
    """

    def __init__(self) -> None:
        self._modele = None
        self._features: List[str] = []
        self._metriques: Dict = {}
        self._charger()

    def _charger(self) -> None:
        if CHEMIN_MODELE.exists():
            paquet = load(CHEMIN_MODELE)
            self._modele = paquet["model"]
            self._features = paquet["features"]
        if CHEMIN_METRIQUES.exists():
            self._metriques = json.loads(CHEMIN_METRIQUES.read_text(encoding="utf-8"))

    @property
    def loaded(self) -> bool:
        return self._modele is not None

    @property
    def trained_at(self) -> str:
        return self._metriques.get("trained_at", "inconnu")

    @property
    def metrics(self) -> Dict:
        return self._metriques

    def predict(self, donnees: Dict[str, float]) -> Tuple[str, float, Dict[str, float], List[str]]:
        if not self.loaded:
            raise RuntimeError("Modele non charge")

        X = pd.DataFrame([[donnees[f] for f in self._features]], columns=self._features)

        probabilites = self._modele.predict_proba(X)[0]
        classes = list(self._modele.classes_)
        index = int(np.argmax(probabilites))

        distribution = {c: round(float(p), 4) for c, p in zip(classes, probabilites)}
        return classes[index], round(float(probabilites[index]), 4), distribution, self._drivers(donnees)

    def _drivers(self, d: Dict[str, float]) -> List[str]:
        """
        Explique la prediction en langage clair.

        Une jauge "risque eleve" sans justification est inexploitable
        pour un responsable : il doit savoir sur quoi agir.
        """
        raisons: List[str] = []

        if d["days_since_last_entry"] >= 10:
            raisons.append(f"aucune saisie depuis {int(d['days_since_last_entry'])} jours")
        if d["tasks_total"] > 0 and d["tasks_rejected"] / d["tasks_total"] >= 0.25:
            raisons.append(f"{int(d['tasks_rejected'])} taches refusees sur {int(d['tasks_total'])}")
        if d["hours_ratio"] < 0.6:
            raisons.append(f"heures validees a {int(d['hours_ratio'] * 100)} % de l'attendu")
        if d["tasks_total"] < 3 and d["progress_ratio"] > 0.4:
            raisons.append("journal quasi vide alors que le stage est bien avance")
        if d["tasks_total"] > 0 and d["tasks_pending"] / d["tasks_total"] >= 0.5:
            raisons.append("la majorite des taches attend une validation de l'encadrant")

        return raisons or ["aucun signal preoccupant"]


modele = RiskModel()
