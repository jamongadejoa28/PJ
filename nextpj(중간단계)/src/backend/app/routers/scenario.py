# src/backend/app/routers/scenario.py

from fastapi import APIRouter, HTTPException, Depends, BackgroundTasks
from fastapi.responses import StreamingResponse
import json
from pydantic import BaseModel
from typing import AsyncGenerator
from services.osm_generator import OSMGenerator
from services.simulation_runner import SimulationRunner
from schemas.scenario import ScenarioRequest
import logging

router = APIRouter()

class SimulationRequest(BaseModel):
    duration: int

# 의존성 주입을 위한 함수
def get_osm_generator() -> OSMGenerator:
    return OSMGenerator()

def get_simulation_runner() -> SimulationRunner:
    return SimulationRunner(data_dir="data")

@router.post("/generate")
async def generate_scenario(
    request: ScenarioRequest,
    generator: OSMGenerator = Depends(get_osm_generator)
) -> StreamingResponse:
    async def event_generator() -> AsyncGenerator[str, None]:
        try:
            async for event in generator.generate(request.dict()):
                yield json.dumps(event) + "\n\n"
        except Exception as e:
            logging.error(f"Error in generate_scenario: {str(e)}")
            yield json.dumps({"progress": 0, "message": "Error occurred during scenario generation."}) + "\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive"
        }
    )

   
@router.post("/simulate")
async def start_simulation(
    request: SimulationRequest,
    background_tasks: BackgroundTasks,
    executor: SimulationRunner = Depends(get_simulation_runner)
):
    try:
        background_tasks.add_task(
            executor.run_simulation,
            duration = request.duration
        )
        
        return {"message": "시뮬레이션이 시작되었습니다."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
