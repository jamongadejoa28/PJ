# src/backend/app/routers/scenario.py

from fastapi import APIRouter, HTTPException, Depends, BackgroundTasks, WebSocket
from fastapi.responses import StreamingResponse, JSONResponse
import json
from pydantic import BaseModel
from typing import AsyncGenerator
from services.osm_generator import OSMGenerator
from services.simulation_runner import SimulationRunner
from schemas.scenario import ScenarioRequest
import logging
from starlette.websockets import WebSocketDisconnect

router = APIRouter()

class SimulationRequest(BaseModel):
    duration: int

    class Config:
        json_schema_extra = {"example": {"duration": 3600}}

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
            yield json.dumps({
                "progress": 0,
                "message": "Error occurred during scenario generation."
            }) + "\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive"}
    )

@router.websocket("/ws/simulation")
async def websocket_simulation(
    websocket: WebSocket,
    simulation_runner: SimulationRunner = Depends(get_simulation_runner)
):
    await websocket.accept()
    logging.info("WebSocket 연결이 열렸습니다")

    try:
        data = await websocket.receive_text()
        config = json.loads(data)

        if config.get("type") == "disconnect":
            await websocket.close(code=1000)
            return

        logging.info(f"Received config: {config}")
        success = await simulation_runner.run_simulation(
            config["duration"], 
            websocket
        )

        if not success:
            logging.error("시뮬레이션 실행 실패")

    except WebSocketDisconnect:
        logging.warning("WebSocket 연결이 클라이언트에 의해 종료되었습니다")
    except Exception as e:
        logging.error(f"WebSocket 핸들링 중 오류 발생: {str(e)}")
        try:
            await websocket.send_text(json.dumps({
                "type": "error",
                "message": "시뮬레이션 중 오류가 발생했습니다."
            }))
        except Exception as send_error:
            logging.error(f"오류 메시지 전송 실패: {str(send_error)}")
    finally:
        if not websocket.client_state.DISCONNECTED:
            await websocket.close(code=1000)
