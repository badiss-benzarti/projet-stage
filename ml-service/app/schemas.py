"""Contrats d'entree et de sortie de l'API de prediction."""

from enum import Enum
from typing import Dict, List

from pydantic import BaseModel, Field


class RiskLevel(str, Enum):
    """Les trois niveaux renvoyes au tableau de bord."""

    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class PredictionRequest(BaseModel):
    """
    Indicateurs extraits du journal de stage par evaluation-service.

    Aucune donnee nominative n'est transmise : le modele ne voit que des
    compteurs. C'est volontaire, et defendable devant un jury.
    """

    tasks_total: int = Field(..., ge=0, description="Nombre de taches saisies")
    tasks_pending: int = Field(..., ge=0, description="Taches en attente de validation")
    tasks_rejected: int = Field(..., ge=0, description="Taches refusees par l'encadrant")
    hours_ratio: float = Field(
        ..., ge=0, le=3,
        description="Heures validees rapportees aux heures attendues a ce stade",
    )
    days_since_last_entry: int = Field(
        ..., ge=0, description="Jours ecoules depuis la derniere saisie"
    )
    progress_ratio: float = Field(
        ..., ge=0, le=1, description="Avancement du stage, de 0 a 1"
    )

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "tasks_total": 12,
                    "tasks_pending": 3,
                    "tasks_rejected": 2,
                    "hours_ratio": 0.72,
                    "days_since_last_entry": 6,
                    "progress_ratio": 0.55,
                }
            ]
        }
    }


class PredictionResponse(BaseModel):
    risk: RiskLevel
    probability: float = Field(..., description="Confiance du modele sur la classe retenue")
    probabilities: Dict[str, float] = Field(..., description="Distribution complete")
    drivers: List[str] = Field(
        ..., description="Indicateurs qui pesent le plus dans cette prediction"
    )


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_version: str
    trained_at: str
