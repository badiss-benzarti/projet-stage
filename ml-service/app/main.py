"""
Service de prediction du risque de difficulte en stage.

Appele par evaluation-service en REST. Volontairement sans etat et sans
base de donnees : il ne fait que transformer des indicateurs en score.
"""

import logging

from fastapi import FastAPI, HTTPException, status

from app.model import modele
from app.schemas import HealthResponse, PredictionRequest, PredictionResponse

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("ml-service")

app = FastAPI(
    title="Service de prediction du risque de stage",
    description=(
        "Estime le risque qu'un stage rencontre des difficultes, a partir des "
        "indicateurs du journal de stage. Modele entraine sur un jeu de donnees "
        "SYNTHETIQUE : la plateforme est neuve et ne dispose d'aucun historique reel."
    ),
    version="1.0.0",
)


@app.get("/health", response_model=HealthResponse, tags=["technique"])
def health():
    """Sonde utilisee par Docker et Prometheus."""
    return HealthResponse(
        status="UP" if modele.loaded else "DOWN",
        model_loaded=modele.loaded,
        model_version=app.version,
        trained_at=modele.trained_at,
    )


@app.get("/metrics-model", tags=["technique"])
def metrics_model():
    """
    Qualite du modele : exactitude, F1 par classe, matrice de confusion,
    importance des variables. A montrer pendant la soutenance.
    """
    if not modele.metrics:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Aucune metrique disponible")
    return modele.metrics


@app.post("/predict", response_model=PredictionResponse, tags=["prediction"])
def predict(requete: PredictionRequest):
    """Renvoie le niveau de risque et les indicateurs qui l'expliquent."""
    if not modele.loaded:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "Modele indisponible : l'entrainement n'a pas ete execute",
        )

    risque, probabilite, distribution, raisons = modele.predict(requete.model_dump())
    log.info("Prediction %s (%.2f) - %s", risque, probabilite, ", ".join(raisons))

    return PredictionResponse(
        risk=risque,
        probability=probabilite,
        probabilities=distribution,
        drivers=raisons,
    )
