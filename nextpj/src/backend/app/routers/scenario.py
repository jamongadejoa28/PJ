# src/backend/app/routers/scenario.py
from fastapi import APIRouter, HTTPException
import os
import time  # time 모듈 추가
from schemas.scenario import ScenarioRequest, ScenarioResponse
from services.osm_generator import OSMScenarioGenerator

router = APIRouter()

@router.post("/scenario/generate")
async def generate_scenario(request: ScenarioRequest):
    try:
        # 절대 경로 사용
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))  # src/backend
        data_dir = os.path.join(base_dir, "data")
        scenario_id = f"scenario_{int(time.time())}"
        scenario_dir = os.path.join(data_dir, scenario_id)
        
        # 디버깅을 위한 로그 추가
        print(f"Creating scenario directory at: {scenario_dir}")
        os.makedirs(scenario_dir, exist_ok=True)
        print("Directory created successfully")

        generator = OSMScenarioGenerator(scenario_dir)
        result = await generator.generate(request)

        return ScenarioResponse(
            status="success",
            message="Scenario files generated successfully",
            files=result
        )
    except Exception as e:
        print(f"Error in generate_scenario: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))