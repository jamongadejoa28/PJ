# src/backend/app/services/osm_generator.py

import os
import sys
import subprocess
import osmnx as ox
from typing import Dict, Any, AsyncGenerator
import logging
from pathlib import Path
import gzip
from datetime import datetime

class OSMGenerator:
    def __init__(self):
        # SUMO 환경 확인 및 설정
        if 'SUMO_HOME' not in os.environ:
            raise EnvironmentError("SUMO_HOME 환경 변수가 설정되지 않았습니다.")

        self.sumo_home = os.environ['SUMO_HOME']
        self.tools_dir = os.path.join(self.sumo_home, 'tools')
        
        # backend/data 폴더 경로 설정
        base_dir = os.getcwd() # backend/app/service
        self.data_dir = os.path.join(os.path.dirname(base_dir),'data')

        if not os.path.exists(self.data_dir):
            os.makedirs(self.data_dir)

        # 파일 이름 접두사 설정
        self.prefix = "osm"

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
        """도로 타입 설정을 OSM 필터 문자열로 변환"""
        filters = []
        for category, types in road_types.items():
            if types:
                type_values = [str(t).split('.')[-1].lower() for t in types]
                types_str = "|".join(type_values)
                filters.append(f'["{category.lower()}"~"{types_str}"]')

        filter_str = ''.join(filters) if filters else ''
        if filter_str:
            self.logger.info(f"도로 타입 필터링 적용: {filter_str}")
        else:
            self.logger.info("도로 타입 필터링 없음.")
        return filter_str

    async def _generate_routes(self, vehicles: Dict[str, Any], duration: int):
        """차량별 경로 생성"""
        sys.path.append(self.tools_dir)
        try:
            import randomTrips
            duarouter = os.path.join(self.sumo_home, 'bin', 'duarouter')
        except ImportError as e:
            self.logger.error(f"필수 모듈 가져오기 실패: {str(e)}")
            raise

        # 현재 작업 디렉토리 저장
        original_cwd = os.getcwd()
        os.chdir(self.data_dir)

        try:
            for vehicle_type, settings in vehicles.items():
                if not settings.get("enabled", False):
                    continue

                # 파일 경로 설정 (상대 경로 사용)
                net_file = f"{self.prefix}.net.xml.gz"
                trips_file = f"{self.prefix}.{vehicle_type}.trips.xml"
                routes_file = f"{self.prefix}.{vehicle_type}.rou.xml"

                # RandomTrips 매개변수
                trip_options = [
                    "-n", net_file,
                    "-o", trips_file,
                    "-b", "0",
                    "-e", str(duration),
                    "--fringe-factor", str(settings.get("fringeFactor", 1.0)),
                    "--insertion-density", str(settings.get("count", 100)),
                    "--vehicle-class", vehicle_type,
                    "--prefix", vehicle_type,
                    "--validate"
                ]

                # 경로 생성
                self.logger.info(f"{vehicle_type} 차량 경로 생성 중...")
                randomTrips.main(randomTrips.get_options(trip_options))

                # duarouter로 경로 변환
                duarouter_options = [
                    duarouter,
                    "-n", net_file,
                    "-r", trips_file,
                    "-o", routes_file,
                    "--ignore-errors",
                    "--no-warnings",
                    "--begin", "0",
                    "--end", str(duration),
                    "--no-step-log"
                ]

                result = subprocess.run(duarouter_options, capture_output=True, text=True)
                if result.returncode != 0:
                    raise Exception(f"duarouter 실패: {result.stderr}")

                # 결과 파일 압축
                if os.path.exists(routes_file):
                    with open(routes_file, 'rb') as f_in:
                        with gzip.open(f"{routes_file}.gz", 'wb') as f_out:
                            f_out.write(f_in.read())
                    os.remove(routes_file)  # 압축 후 원본 삭제
            
                # trips 파일 삭제
                if os.path.exists(trips_file):
                    os.remove(trips_file)

                self.logger.info(f"{vehicle_type} 차량 경로 생성 완료")

        finally:
            os.chdir(original_cwd)

    async def _create_sumo_config(self, duration: int):
        """SUMO 설정 파일 생성"""
        try:
            # 라우트 파일 목록 가져오기
            route_files = [f for f in os.listdir(self.data_dir) if f.endswith('.rou.xml.gz')]
            route_files_str = ",".join(route_files)

            config_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- generated on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} by SUMO -->
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/sumoConfiguration.xsd">
    <input>
        <net-file value="{self.prefix}.net.xml.gz"/>
        <route-files value="{route_files_str}"/>
    </input>
    <processing>
        <ignore-route-errors value="true"/>
        <tls.actuated.jam-threshold value="30"/>
    </processing>
    <routing>
        <device.rerouting.adaptation-steps value="18"/>
        <device.rerouting.adaptation-interval value="10"/>
    </routing>
    <report>
        <verbose value="true"/>
        <duration-log.statistics value="true"/>
        <no-step-log value="true"/>
    </report>
</configuration>"""

            # 설정 파일 저장
            config_file = os.path.join(self.data_dir, f"{self.prefix}.sumocfg")
            with open(config_file, "w", encoding='utf-8') as f:
                f.write(config_content)

            self.logger.info("SUMO 설정 파일 생성 완료")
        except Exception as e:
            self.logger.error(f"SUMO 설정 파일 생성 실패: {str(e)}")
            raise

    async def generate(self, data: Dict[str, Any]) -> AsyncGenerator[Dict[str, Any], None]:
        """OSM 데이터를 생성하고 SUMO 네트워크로 변환하는 비동기 제너레이터"""
        try:
            yield {"progress": 10, "message": "OSM 데이터 다운로드 준비 중..."}

            # 네트워크 다운로드 매개변수 설정
            center_point = (data["coordinates"]["lat"], data["coordinates"]["lng"])
            distance = data["radius"]
            custom_filter = self._create_custom_filter(data.get("roadTypes", {}))

            try:
                # OSM 데이터 다운로드
                G = ox.graph_from_point(
                    center_point=center_point,
                    dist=distance,
                    dist_type='bbox',
                    network_type='drive',
                    simplify=False,
                    retain_all=True,
                    truncate_by_edge=True,
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
            osm_file = os.path.join(self.data_dir, f"{self.prefix}.osm")
            net_file = os.path.join(self.data_dir, f"{self.prefix}.net.xml")

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

            netconvert_path = os.path.join(self.sumo_home, 'bin', 'netconvert')
            if not os.path.isfile(netconvert_path):
                self.logger.error(f"netconvert 실행 파일을 찾을 수 없습니다: {netconvert_path}")
                yield {"progress": 0, "message": f"netconvert 실행 파일을 찾을 수 없습니다: {netconvert_path}"}
                return

            netconvert_options = [
                netconvert_path,
                "--osm-files", osm_file,
                "--output-file", net_file,
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

            try:
                result = subprocess.run(netconvert_options, check=True, capture_output=True, text=True)
                self.logger.info("SUMO 네트워크 생성 성공")
                self.logger.debug(f"netconvert 출력: {result.stdout}")

                # 네트워크 파일 압축
                with open(net_file, 'rb') as f_in:
                    with gzip.open(f"{net_file}.gz", 'wb') as f_out:
                        f_out.write(f_in.read())
                os.remove(net_file)  # 원본 파일 삭제
                self.logger.info("네트워크 파일 압축 완료")
                yield {"progress": 70, "message": "SUMO 네트워크 생성 완료"}
            except Exception as e:
                self.logger.error(f"SUMO 네트워크 생성 실패: {str(e)}")
                yield {"progress": 0, "message": f"SUMO 네트워크 생성 중 오류가 발생했습니다: {str(e)}"}
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
                os.remove(os.path.join(self.data_dir,"routes.rou.xml"))
                return
            except Exception as e:
                self.logger.error(f"SUMO 설정 파일 생성 실패: {str(e)}")
                yield {"progress": -1, "message": "SUMO 설정 파일 생성 중 오류가 발생했습니다"}
                return

        except Exception as e:
            self.logger.error(f"예상치 못한 오류 발생: {str(e)}")
            yield {"progress": 0, "message": f"예상치 못한 오류가 발생했습니다: {str(e)}"}