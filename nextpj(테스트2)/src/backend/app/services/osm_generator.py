# src/backend/app/services/osm_generator.py

import os
import sys
import subprocess
import osmnx as ox
from typing import Dict, Any, AsyncGenerator
import logging
from pathlib import Path

class OSMGenerator:
    def __init__(self):
        """
        OSMGenerator 초기화 - SUMO 환경 설정 및 로깅 설정
        """
        if 'SUMO_HOME' not in os.environ:
            raise EnvironmentError("SUMO_HOME 환경 변수가 설정되지 않았습니다.")

        self.sumo_home = os.environ['SUMO_HOME']
        self.tools_dir = os.path.join(self.sumo_home, 'tools')

        # Set data_dir to an absolute path relative to this file
        self.data_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'data'))
        os.makedirs(self.data_dir, exist_ok=True)  # 디렉토리가 없으면 생성

        # 로깅 설정
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(os.path.join(self.data_dir, 'osm_generator.log')),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)

        # OSMnx 기본 설정
        ox.settings.use_cache = True
        ox.settings.log_console = True
        ox.settings.all_oneway = True

        self.logger.info(f"Data directory set to: {self.data_dir}")

    def _create_custom_filter(self, road_types: Dict[str, Any]) -> str:
        """
        도로 타입 설정을 OSM 필터 문자열로 변환

        Args:
            road_types (Dict[str, Any]): 도로 타입 설정 딕셔너리

        Returns:
            str: OSM 필터 문자열
        """
        filters = []
        for category, types in road_types.items():
            if types:
                # 열거형 값에서 실제 도로 타입만 추출
                type_values = [str(t).split('.')[-1].lower() for t in types]
                # category와 type을 결합하여 Overpass QL filter 생성
                types_str = "|".join(type_values)
                filters.append(f'["{category.lower()}"~"{types_str}"]')

        filter_str = ''.join(filters) if filters else ''
        if filter_str:
            self.logger.info(f"도로 타입 필터링 적용: {filter_str}")
        else:
            self.logger.info("도로 타입 필터링 없음.")
        return filter_str

    async def generate(self, data: Dict[str, Any]) -> AsyncGenerator[Dict[str, Any], None]:
        """
        OSM 데이터를 생성하고 SUMO 네트워크로 변환하는 비동기 제너레이터
        """
        try:
            yield {"progress": 10, "message": "OSM 데이터 다운로드 준비 중..."}

            # 네트워크 다운로드 매개변수 설정
            center_point = (data["coordinates"]["lat"], data["coordinates"]["lng"])
            distance = data["radius"]

            # 도로 타입에 따른 custom_filter 생성
            custom_filter = self._create_custom_filter(data.get("roadTypes", {}))

            try:
                # OSM 데이터 다운로드
                G = ox.graph_from_point(
                    center_point=center_point,
                    dist=distance,
                    dist_type='bbox',  # bbox 타입으로 고정하여 일관된 결과 보장
                    network_type='drive',  # 기본 네트워크 타입은 drive
                    simplify=False,  # 토폴로지 단순화하지 않음
                    retain_all=True,  # 연결되지 않은 부분도 유지
                    truncate_by_edge=True,  # 경계에서 엣지 잘라내기
                    custom_filter=custom_filter if custom_filter else None
                )

                node_count = len(G.nodes)
                edge_count = len(G.edges)
                self.logger.info(f"OSM 데이터 다운로드 성공. 노드 수: {node_count}, 엣지 수: {edge_count}")
                yield {"progress": 30, "message": "OSM 데이터 다운로드 완료"}

                if node_count == 0:
                    raise ValueError("다운로드된 OSM 데이터에 노드가 없습니다. 필터 설정을 확인하세요.")

            except Exception as e:
                self.logger.error(f"OSM 데이터 다운로드 실패: {str(e)}")
                yield {"progress": 0, "message": f"OSM 데이터 다운로드 중 오류가 발생했습니다: {str(e)}"}
                return

            # OSM 파일 저장
            osm_file = os.path.join(self.data_dir, "test.osm")
            net_file = os.path.join(self.data_dir, "test.net.xml")

            try:
                # OSM XML 형식으로 저장
                ox.save_graph_xml(G, filepath=osm_file)
                self.logger.info(f"OSM 파일 저장 성공: {osm_file}")
                yield {"progress": 50, "message": "OSM 데이터 저장 완료"}
            except Exception as e:
                self.logger.error(f"OSM 파일 저장 실패: {str(e)}")
                yield {"progress": 0, "message": f"OSM 파일 저장 중 오류가 발생했습니다: {str(e)}"}
                return

            # SUMO 네트워크 생성
            yield {"progress": 60, "message": "SUMO 네트워크 생성 중..."}

            # netconvert 옵션 설정
            # Use the absolute path to netconvert
            netconvert_path = os.path.join(self.sumo_home, 'bin', 'netconvert')
            if not os.path.isfile(netconvert_path):
                self.logger.error(f"netconvert 실행 파일을 찾을 수 없습니다: {netconvert_path}")
                yield {"progress": 0, "message": f"netconvert 실행 파일을 찾을 수 없습니다: {netconvert_path}"}
                return

            netconvert_options = [
                netconvert_path,  # 절대 경로 사용
                "--osm-files", osm_file,  # OSM 파일 입력 옵션 수정
                "--output-file", net_file,  # 출력 파일 옵션 수정
                "--geometry.remove",
                "--roundabouts.guess",
                "--ramps.guess",
                "--junctions.join",
                "--tls.guess-signals",
                "--tls.discard-simple",
                "--tls.join",
                "--tls.default-type", "actuated",
                "--junctions.corner-detail", "5",
                "--output.street-names",
                "--output.original-names",
                "--precision.geo", "2",
                "--geometry.avoid-overlap",
                "--geometry.min-dist", "1",
                "--remove-edges.isolated"
            ]

            self.logger.info(f"netconvert 명령어: {' '.join(netconvert_options)}")

            try:
                result = subprocess.run(netconvert_options, check=True, capture_output=True, text=True)
                self.logger.info("SUMO 네트워크 생성 성공")
                self.logger.debug(f"netconvert 출력: {result.stdout}")
                yield {"progress": 70, "message": "SUMO 네트워크 생성 완료"}
            except subprocess.CalledProcessError as e:
                self.logger.error(f"netconvert 실패: {e.stderr}")
                yield {"progress": 0, "message": f"SUMO 네트워크 생성 중 오류가 발생했습니다: {e.stderr}"}
                return

            # 경로 생성
            yield {"progress": 80, "message": "경로 생성 중..."}
            try:
                await self._generate_routes(data["vehicles"], data["duration"])
                self.logger.info("경로 생성 성공")
                yield {"progress": 90, "message": "경로 생성 완료"}
            except Exception as e:
                self.logger.error(f"경로 생성 실패: {str(e)}")
                yield {"progress": 0, "message": "경로 생성 중 오류가 발생했습니다"}
                return

            # SUMO 설정 파일 생성
            yield {"progress": 95, "message": "SUMO 설정 파일 생성 중..."}
            try:
                await self._create_sumo_config(data["duration"])
                self.logger.info("SUMO 설정 파일 생성 성공")
                yield {"progress": 100, "message": "완료!"}
            except Exception as e:
                self.logger.error(f"SUMO 설정 파일 생성 실패: {str(e)}")
                yield {"progress": 0, "message": "SUMO 설정 파일 생성 중 오류가 발생했습니다"}
                return

        except Exception as e:
            self.logger.error(f"예상치 못한 오류 발생: {str(e)}")
            yield {"progress": 0, "message": f"예상치 못한 오류가 발생했습니다: {str(e)}"}

    async def _generate_routes(self, vehicles: Dict[str, Any], duration: int):
        """
        차량별 경로 생성

        Args:
            vehicles (Dict[str, Any]): 차량 설정
            duration (int): 시뮬레이션 지속 시간 (초)
        """
        sys.path.append(self.tools_dir)
        try:
            import randomTrips
        except ImportError as e:
            self.logger.error(f"randomTrips 모듈을 불러올 수 없습니다: {str(e)}")
            raise

        for vehicle_type, settings in vehicles.items():
            if not settings.get("enabled", False):
                continue

            net_file = os.path.join(self.data_dir, "test.net.xml")
            trips_file = os.path.join(self.data_dir, f"test.{vehicle_type}.trips.xml")

            options = [
                "-n", net_file,
                "-o", trips_file,
                "-b", "0",
                "-e", str(duration),
                "--fringe-factor", str(settings.get("fringeFactor", 1.0)),
                "-p", str(3600 / float(settings.get("count", 100))),  # 시간당 차량 수를 주기로 변환
                "--vehicle-class", vehicle_type,
                "--prefix", f"{vehicle_type}_",  # 고유한 프리픽스 추가
                "--validate"
            ]

            try:
                self.logger.info(f"Running randomTrips with options: {' '.join(options)}")
                randomTrips.main(randomTrips.get_options(options))
                self.logger.info(f"{vehicle_type} 차량 경로 생성 완료: {trips_file}")
            except Exception as e:
                self.logger.error(f"{vehicle_type} 차량 경로 생성 실패: {str(e)}")
                raise

    async def _create_sumo_config(self, duration: int):
        """
        SUMO 설정 파일 생성

        Args:
            duration (int): 시뮬레이션 지속 시간 (초)
        """
        # Find all trip files
        trip_files = list(Path(self.data_dir).glob("test.*.trips.xml"))
        if not trip_files:
            self.logger.error("No trip files found to include in SUMO config.")
            raise FileNotFoundError("No trip files found to include in SUMO config.")

        # Create a comma-separated list of trip file names relative to sumocfg
        trip_files_str = ",".join([f.name for f in trip_files])

        # Use relative path for net-file
        net_file_str = "test.net.xml"

        config_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <input>
        <net-file value="{net_file_str}"/>
        <route-files value="{trip_files_str}"/>
    </input>
    <time>
        <begin value="0"/>
        <end value="{duration}"/>
    </time>
    <processing>
        <time-to-teleport value="-1"/>
        <ignore-route-errors value="true"/>
    </processing>
    <report>
        <verbose value="true"/>
        <no-step-log value="true"/>
    </report>
</configuration>"""

        config_file = os.path.join(self.data_dir, "test.sumocfg")
        try:
            with open(config_file, "w") as f:
                f.write(config_content)
            self.logger.info(f"SUMO 설정 파일 생성 완료: {config_file}")
        except Exception as e:
            self.logger.error(f"SUMO 설정 파일 작성 실패: {str(e)}")
            raise
