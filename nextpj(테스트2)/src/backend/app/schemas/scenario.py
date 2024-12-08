# src/backend/app/schemas/scenario.py
from pydantic import BaseModel, Field, field_validator
from typing import Dict, List, Optional
from enum import Enum

class HighwayType(str, Enum):
    motorway = "motorway"
    trunk = "trunk"
    primary = "primary"
    secondary = "secondary"
    tertiary = "tertiary"

class RailwayType(str, Enum):
    rail = "rail"
    tram = "tram"

class RoadCategory(str, Enum):
    Highway = "Highway"
    Railway = "Railway"

class VehicleSettings(BaseModel):
    count: float = Field(..., ge=0, description="0 이상의 차량 수")
    fringeFactor: float = Field(..., ge=0, description="프린지 팩터 (0 이상)")
    enabled: bool = Field(..., description="차량 유형 활성화 여부")

class Options(BaseModel):
    polygons: bool = Field(False, description="폴리곤 사용 여부")
    publicTransport: bool = Field(False, description="대중교통 사용 여부")
    carOnlyNetwork: bool = Field(False, description="자동차 전용 네트워크 사용 여부")
    decal: bool = Field(False, description="데칼 사용 여부")
    leftHand: bool = Field(False, description="좌측 통행 여부")

class Coordinates(BaseModel):
    lat: float = Field(..., ge=-90, le=90, description="위도 (-90 ~ 90)")
    lng: float = Field(..., ge=-180, le=180, description="경도 (-180 ~ 180)")

class RoadTypes(BaseModel):
    Highway: Optional[List[HighwayType]] = Field(None, description="고속도로 타입 리스트")
    Railway: Optional[List[RailwayType]] = Field(None, description="철도 타입 리스트")

    @field_validator('Highway', 'Railway', mode='before')
    def validate_road_types(cls, v):
        if v is not None:
            return [item.value if isinstance(item, Enum) else item for item in v]
        return v

class ScenarioRequest(BaseModel):
    coordinates: Coordinates = Field(..., description="시나리오 중심 좌표")
    radius: int = Field(..., ge=0, le=3000, description="시나리오 반경 (0-3000m)")
    duration: int = Field(..., ge=0, description="시나리오 지속 시간 (초)")
    vehicles: Dict[str, VehicleSettings] = Field(..., description="차량 설정")
    roadTypes: RoadTypes = Field(..., description="도로 타입 설정")
    options: Options = Field(..., description="시나리오 옵션")

    class Config:
        json_schema_extra = {
            "example": {
                "coordinates": {"lat": 37.5665, "lng": 126.978},
                "radius": 1000,
                "duration": 3600,
                "vehicles": {
                    "passenger": {
                        "count": 12,
                        "fringeFactor": 5,
                        "enabled": True
                    }
                },
                "roadTypes": {
                    "Highway": ["motorway", "trunk", "primary"],
                    "Railway": ["rail", "tram"]
                },
                "options": {
                    "polygons": True,
                    "publicTransport": False,
                    "carOnlyNetwork": False,
                    "decal": False,
                    "leftHand": False
                }
            }
        }
