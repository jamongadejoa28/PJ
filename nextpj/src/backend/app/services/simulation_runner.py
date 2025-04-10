# src/app/backend/service/simulation_runner.py

from pyproj import Transformer
from fastapi import WebSocket
from typing import Optional
import os, sys, traceback, logging, glob, traci, json, asyncio, gzip
import xml.etree.ElementTree as ET
from starlette.websockets import WebSocketDisconnect

class SimulationRunner:
    def __init__(self, data_dir: str):
        # 기본 디렉토리 및 설정 초기화
        self.data_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', data_dir))
        config_files = glob.glob(os.path.join(self.data_dir, "*.sumocfg"))
        self.config_file = config_files[0] if config_files else None
        
        # 도로 제어 관련 상태 변수
        self.motorway_links = []  # 고속도로 진입로 목록
        self.block_motorway = False  # 도로 차단 상태
        self.carType = ["passenger", "truck", "bus", "motorcycle", "bicycle"]
        self.control_status = {
            "block_applied": False
        }

        # 로깅 설정
        log_file = os.path.join(self.data_dir, 'simulation_runner.log')
        if os.path.exists(log_file):
            os.remove(log_file)
            
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file),
                logging.StreamHandler(sys.stdout)
            ]
        )
        self.logger = logging.getLogger(__name__)
        
        # 좌표 변환기 설정
        self.transformer = Transformer.from_crs(
            "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
            "EPSG:4326",
            always_xy=True
        )
        
        # 네트워크 파일에서 offset 값 로드
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
                    
                offset_x, offset_y = map(float, net_offset.split(','))
                self.center_x = -offset_x
                self.center_y = -offset_y
                
                # 고속도로 진입로 정보 로드
                for edge in root.findall('.//edge'):
                    edge_type = edge.get('type')
                    if edge_type == 'highway.motorway_link':
                        self.motorway_links.append(edge.get('id'))
                self.logger.info(f'고속도로 진입로 {len(self.motorway_links)}개를 찾았습니다')
                
        except Exception as e:
            self.logger.error(f"center 값 설정 중 오류가 발생했습니다: {str(e)}")
            self.logger.error(traceback.format_exc())
            raise

        self.traci_started = False
        self.simulation_step = 0
        self.lock = asyncio.Lock()

    async def handle_simulation_control(self, websocket: WebSocket) -> None:
        """WebSocket을 통해 받은 제어 명령을 처리하는 함수"""
        try:
            msg = await asyncio.wait_for(websocket.receive_json(), timeout=0.1)
            self.logger.info(f"제어 메시지 수신: {msg}")
            
            if 'blockMotorwayLinks' in msg:
                new_block_state = bool(msg['blockMotorwayLinks'])
                if new_block_state != self.block_motorway:
                    self.block_motorway = new_block_state
                    try:
                        for edge_id in self.motorway_links:
                            if new_block_state:
                                traci.edge.setDisallowed(edge_id, self.carType)
                            else:
                                traci.edge.setAllowed(edge_id, self.carType)
                        self.control_status["block_applied"] = True
                        self.logger.info(f"도로 차단 상태 변경 적용됨: {self.block_motorway}")
                    except traci.exceptions.TraCIException as e:
                        self.logger.error(f"도로 차단 상태 변경 실패: {str(e)}")
                        self.control_status["block_applied"] = False
                        
        except asyncio.TimeoutError:
            pass
        except Exception as e:
            self.logger.error(f"제어 메시지 처리 중 오류: {str(e)}")

    async def initialize_simulation(self, duration: int, websocket: Optional[WebSocket] = None) -> bool:
        """시뮬레이션 초기화 함수"""
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
                "--time-to-teleport", "1",
                "--end", str(duration),
                "--max-depart-delay", str(duration),
                "--quit-on-end", "true",
                "--device.rerouting.mode", "8",
                "--default.carfollowmodel", "EIDM",
                "--device.rerouting.probability", "1",
                "--ignore-route-errors", "true",
                "--fcd-output", os.path.join(self.data_dir, "fcd_output.xml")
            ]

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
        """메인 시뮬레이션 실행 함수"""
        async with self.lock:
            try:
                if not await self.initialize_simulation(duration, websocket):
                    return False

                total_steps = duration
                last_vehicle_count = 0
    
                while traci.simulation.getMinExpectedNumber() > 0:
                    # 제어 메시지 처리
                    if websocket:
                        await self.handle_simulation_control(websocket)

                    # 시뮬레이션 스텝 실행
                    traci.simulationStep()
                    
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
                                speed = traci.vehicle.getSpeed(vehicle_id)
                                adjusted_x = x + self.center_x
                                adjusted_y = y + self.center_y
                                longitude, latitude = self.transformer.transform(adjusted_x, adjusted_y)
                                vehicle_positions.append({
                                    "id": vehicle_id,
                                    "position": {"lat": latitude, "lng": longitude},
                                    "type": traci.vehicle.getTypeID(vehicle_id),
                                    "angle": traci.vehicle.getAngle(vehicle_id),
                                    "speed": speed * 3.6
                                })
                            except traci.exceptions.TraCIException as e:
                                self.logger.error(f"차량 {vehicle_id} 처리 중 오류: {str(e)}")
                                continue
                        average_speed = (sum(v["speed"] for v in vehicle_positions) / 
                                        len(vehicle_positions)) if vehicle_positions else 0
                                    
                        # 차량 위치와 제어 상태 전송
                        await websocket.send_json({
                            "type": "vehicle_positions",
                            "data": vehicle_positions,
                            "progress": ((self.simulation_step * 100) // total_steps),
                            "vehicleCount": current_vehicle_count,
                            "controlStatus": self.control_status,
                            "averageSpeed" : round(average_speed, 2)
                        })

                    self.simulation_step += 1
                    if self.simulation_step >= duration:
                        break

                if websocket:
                    await websocket.send_text(json.dumps({"type": "simulation_complete"}))

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
        """시뮬레이션 종료 시 정리 작업을 수행하는 함수"""
        try:
            if self.traci_started:
                self.logger.info("TraCI 연결을 종료합니다")
                traci.close()
                self.traci_started = False
                self.simulation_step = 0
                self.logger.info("TraCI 연결이 안전하게 종료되었습니다")
        except Exception as e:
            self.logger.error(f"TraCI 종료 중 오류 발생: {str(e)}")