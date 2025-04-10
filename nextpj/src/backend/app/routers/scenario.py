# src/backend/app/routers/scenario.py

from fastapi import APIRouter, HTTPException, Depends, BackgroundTasks, WebSocket
from fastapi.responses import StreamingResponse, JSONResponse
import json
from pydantic import BaseModel
from typing import AsyncGenerator
from services.osm_generator import OSMGenerator
from services.simulation_runner import SimulationRunner
from schemas.scenario import ScenarioRequest
import logging, asyncio
from starlette.websockets import WebSocketDisconnect

# 라우터 초기화
router = APIRouter()

# 시뮬레이션 요청을 위한 모델 정의
class SimulationRequest(BaseModel):
    duration: int

    class Config:
        json_schema_extra = {"example": {"duration": 3600}}

# 서비스 인스턴스 생성을 위한 의존성 함수들
def get_osm_generator() -> OSMGenerator:
    return OSMGenerator()

def get_simulation_runner() -> SimulationRunner:
    return SimulationRunner(data_dir="data")

@router.post("/generate")
async def generate_scenario(
    request: ScenarioRequest, 
    generator: OSMGenerator = Depends(get_osm_generator)
) -> StreamingResponse:
    """시나리오 생성을 위한 엔드포인트"""
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
    """메인 시뮬레이션 실행을 위한 WebSocket 엔드포인트"""
    await websocket.accept()
    logging.info("메인 시뮬레이션 WebSocket 연결이 열렸습니다")

    try:
        # 시뮬레이션 설정 수신
        data = await websocket.receive_text()
        config = json.loads(data)
        logging.info(f"시뮬레이션 설정 수신: {config}")

        # 연결 종료 요청 처리
        if config.get("type") == "disconnect":
            await websocket.close(code=1000)
            return

        # 시뮬레이션 실행
        success = await simulation_runner.run_simulation(
            config.get("duration", 3600),  # 기본값 1시간
            websocket
        )

        if not success:
            logging.error("시뮬레이션 실행 실패")
            await websocket.send_json({
                "type": "error",
                "message": "시뮬레이션 실행에 실패했습니다."
            })

    except WebSocketDisconnect:
        logging.warning("WebSocket 연결이 클라이언트에 의해 종료되었습니다")
    except json.JSONDecodeError as e:
        logging.error(f"잘못된 JSON 형식: {str(e)}")
        await websocket.send_json({
            "type": "error",
            "message": "잘못된 설정 형식입니다."
        })
    except Exception as e:
        logging.error(f"WebSocket 핸들링 중 오류 발생: {str(e)}")
        try:
            await websocket.send_json({
                "type": "error",
                "message": "시뮬레이션 중 오류가 발생했습니다."
            })
        except Exception as send_error:
            logging.error(f"오류 메시지 전송 실패: {str(send_error)}")
    finally:
        if not websocket.client_state.DISCONNECTED:
            await websocket.close(code=1000)