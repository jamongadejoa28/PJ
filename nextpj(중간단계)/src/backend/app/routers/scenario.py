# src/backend/app/routers/scenario.py

from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import StreamingResponse, JSONResponse
import json
from typing import Dict, Any, AsyncGenerator
from app.services.osm_generator import OSMGenerator
from app.services.simulation_runner import SimulationRunner
from app.schemas.scenario import ScenarioRequest
import logging

router = APIRouter()

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
    duration: int,
    simulation_runner: SimulationRunner = Depends(get_simulation_runner)
) -> JSONResponse:
    try:
        # 시뮬레이션을 비동기적으로 실행
        success = await simulation_runner.run_async(duration=duration)
        if not success:
            raise HTTPException(status_code=500, detail="Simulation failed")
        return {"status": "success"}
    except HTTPException as he:
        raise he
    except Exception as e:
        logging.error(f"Error in start_simulation: {str(e)}")
        raise HTTPException(status_code=500, detail="An error occurred while running the simulation.")