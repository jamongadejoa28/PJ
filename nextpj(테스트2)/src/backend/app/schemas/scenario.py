# src/backend/app/schemas/scenario.py

from pydantic import BaseModel
from typing import Dict, List, Optional

class VehicleSettings(BaseModel):
   internal: str
   enabled: bool
   count: float
   fringeFactor: float

class RoadTypeSettings(BaseModel):
   enabled: bool
   types: List[str]

class Options(BaseModel):
   polygons: bool
   publicTransport: bool 
   carOnlyNetwork: bool
   leftHand: bool

class ScenarioRequest(BaseModel):
   coordinates: Dict[str, float]  # {"lat": float, "lng": float}
   radius: int
   duration: int
   vehicles: Dict[str, VehicleSettings]
   roadTypes: Dict[str, RoadTypeSettings]
   options: Options