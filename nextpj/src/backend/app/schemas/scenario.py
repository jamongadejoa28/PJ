# src/backend/app/schemas/scenario.py
from pydantic import BaseModel
from typing import Dict, List, Optional, Union

class VehicleSettings(BaseModel):
    count: float
    fringeFactor: float
    enabled: bool

class SelectedArea(BaseModel):
    rect: List[float]
    center: List[float]

class ScenarioRequest(BaseModel):
    coordinates: List[float]  # [west, south, east, north]
    duration: int
    vehicles: Dict[str, VehicleSettings]
    options: Dict[str, bool]
    roadTypes: Dict[str, List[str]]
    selectedArea: Optional[SelectedArea] = None

class ScenarioResponse(BaseModel):
    status: str
    message: str
    progress: Optional[int] = None
    files: Optional[List[str]] = None