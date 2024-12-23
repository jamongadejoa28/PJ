# src/backend/service/simluation_runner.py

from pyproj import Proj, Transformer
from fastapi import WebSocket
from typing import Optional
import os, sys, traceback, logging, glob, traci, json, asyncio, gzip, xml.etree.ElementTree as ET
from starlette.websockets import WebSocketDisconnect

class SimulationRunner:
    def __init__(self, data_dir: str):
        # 기본 디렉토리 설정 및 로거 초기화
        self.data_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', data_dir))
        config_files = glob.glob(os.path.join(self.data_dir, "*.sumocfg"))
        self.config_file = config_files[0] if config_files else None
        
        # 로깅 설정
        logging.basicConfig(
            level=logging.DEBUG,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(os.path.join(self.data_dir, 'simulation_runner.log')),
                logging.StreamHandler(sys.stdout)
            ]
        )
        self.logger = logging.getLogger(__name__)
        
        # 좌표 변환 설정
        self.transformer = Transformer.from_crs(
            "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",  # UTM Zone 52N
            "EPSG:4326",  # WGS84
            always_xy=True  # x,y (경도,위도) 순서 사용
        )
        
         # netOffset 값을 XML 파일에서 읽어서 center 값으로 설정
        net_file_path = os.path.join(self.data_dir, "osm.net.xml.gz")
        if not os.path.exists(net_file_path):
            raise FileNotFoundError(f"필수 네트워크 파일을 찾을 수 없습니다: {net_file_path}")
            
        try:
            with gzip.open(net_file_path, 'rb') as gz_file:
                tree = ET.parse(gz_file)
                root = tree.getroot()
                location = root.find('location')
                
                if location is None:
                    raise ValueError("XML 파일에서 location 요소를 찾을 수 없습니다")
                    
                net_offset = location.get('netOffset')
                if not net_offset:
                    raise ValueError("location 요소에서 netOffset 속성을 찾을 수 없습니다")
                    
                # netOffset 값의 부호를 반전하여 center 값으로 설정
                offset_x, offset_y = map(float, net_offset.split(','))
                self.center_x = -offset_x  # netOffset 값의 부호를 반전
                self.center_y = -offset_y  # netOffset 값의 부호를 반전
                
                self.logger.info(f"XML 파일에서 center 값을 설정했습니다: x={self.center_x}, y={self.center_y}")
                
        except Exception as e:
            self.logger.error(f"center 값 설정 중 오류가 발생했습니다: {str(e)}")
            self.logger.error(traceback.format_exc())
            raise  # 오류를 상위로 전파하여 초기화가 실패하도록 함

        self.traci_started = False
        self.simulation_step = 0
        self.lock = asyncio.Lock()

    async def _handle_stream(self, stream: asyncio.StreamReader, prefix: str):
        while True:
            line = await stream.readline()
            if not line:
                break
            try:
                decoded_line = line.decode('utf-8').strip()
            except UnicodeDecodeError:
                decoded_line = line.decode('cp949', errors='replace').strip()
            self.logger.info(f"{prefix}: {decoded_line}")

    async def initialize_simulation(self, duration: int, websocket: Optional[WebSocket] = None) -> bool:
        try:
            if not self.config_file or not os.path.exists(self.config_file):
                error_msg = f"시나리오 파일을 찾을 수 없습니다. 경로: {self.config_file}"
                self.logger.error(error_msg)
                if websocket:
                    await websocket.send_text(json.dumps({"type": "error", "message": error_msg}))
                raise FileNotFoundError(error_msg)

            cmd = [
                "sumo",
                "-c", self.config_file,
                "--waiting-time-memory", "100",
                "--time-to-teleport", "-1",
                "--end", str(duration),
                "--max-depart-delay", str(duration),
                "--quit-on-end", "true",
                "--device.rerouting.mode", "8",
                "--default.carfollowmodel", "EIDM",
                "--device.rerouting.probability", "1",
                "--ignore-route-errors", "true",
                "--fcd-output", os.path.join(self.data_dir, "fcd_output.xml")
            ]

            self.logger.info("-" * 50)
            self.logger.info("시뮬레이션 초기화를 시작합니다")
            self.logger.info(f"SUMO 설정 파일: {self.config_file}")

            if not self.traci_started:
                traci.start(cmd)
                self.traci_started = True
                self.logger.info("TraCI 연결이 성공적으로 설정되었습니다")
            
            return True

        except Exception as e:
            self.logger.error(f"시뮬레이션 초기화 중 오류 발생: {str(e)}")
            self.logger.error(traceback.format_exc())
            if websocket:
                await websocket.send_text(json.dumps({"type": "error", "message": str(e)}))
            return False

    async def run_simulation(self, duration: int, websocket: Optional[WebSocket] = None) -> bool:
        """통합된 시뮬레이션 실행 함수"""
        async with self.lock:
            try:
                if not await self.initialize_simulation(duration, websocket):
                    return False

                total_steps = duration
                last_progress = 0
                last_vehicle_count = 0

                while traci.simulation.getMinExpectedNumber() > 0:
                    traci.simulationStep()
                    self.simulation_step += 1

                    # 진행률 계산 및 로깅
                    current_progress = (self.simulation_step * 100) // total_steps
                    if current_progress >= last_progress + 10:
                        self.logger.info(f"시뮬레이션 진행률: {current_progress}%")
                        last_progress = current_progress

                    # 차량 정보 수집 및 전송
                    current_vehicles = traci.vehicle.getIDList()
                    current_vehicle_count = len(current_vehicles)

                    if current_vehicle_count != last_vehicle_count:
                        self.logger.info(f"현재 차량 수: {current_vehicle_count}")
                        last_vehicle_count = current_vehicle_count

                    if websocket:
                        vehicle_positions = []
                        for vehicle_id in current_vehicles:
                            try:
                                x, y = traci.vehicle.getPosition(vehicle_id)
                                adjusted_x = x + self.center_x
                                adjusted_y = y + self.center_y
                                longitude, latitude = self.transformer.transform(adjusted_x, adjusted_y)
                            
                                vehicle_positions.append({
                                    "id": vehicle_id,
                                    "position": {"lat": latitude, "lng": longitude},
                                    "type": traci.vehicle.getTypeID(vehicle_id)
                                })
                            except traci.exceptions.TraCIException as e:
                                self.logger.error(f"차량 {vehicle_id} 처리 중 오류: {str(e)}")
                                continue

                        try:
                            await websocket.send_text(json.dumps({
                                "type": "vehicle_positions",
                                "data": vehicle_positions,
                                "progress": current_progress,
                                "vehicleCount": current_vehicle_count
                            }))
                        except WebSocketDisconnect:
                            self.logger.warning("WebSocket 연결이 끊어졌습니다")
                            return False

                    if self.simulation_step >= duration:
                        break

                # 시뮬레이션 완료 처리
                if websocket:
                    try:
                        await websocket.send_text(json.dumps({"type": "simulation_complete"}))
                    except Exception as e:
                        self.logger.error(f"완료 메시지 전송 중 오류: {str(e)}")

                return True

            except WebSocketDisconnect:
                self.logger.warning("WebSocket 연결이 끊어졌습니다")
                return False
            except Exception as e:
                self.logger.error(f"시뮬레이션 실행 중 오류 발생: {str(e)}")
                self.logger.error(traceback.format_exc())
                if websocket:
                    try:
                        await websocket.send_text(json.dumps({
                            "type": "error",
                            "message": "시뮬레이션 중 오류가 발생했습니다."
                        }))
                    except Exception as send_error:
                        self.logger.error(f"오류 메시지 전송 실패: {str(send_error)}")
                return False
            finally:
                await self.cleanup()

    async def cleanup(self):
        """TraCI 연결 정리"""
        try:
            if self.traci_started:
                self.logger.info("TraCI 연결을 종료합니다")
                traci.close()
                self.traci_started = False
                self.simulation_step = 0
                self.logger.info("TraCI 연결이 안전하게 종료되었습니다")
        except Exception as e:
            self.logger.error(f"TraCI 종료 중 오류 발생: {str(e)}")
