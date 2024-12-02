# src/backend/app/services/osm_generator.py

import os
import sys
import subprocess
import shutil
from typing import List, Dict, Any, Optional
import gzip
from schemas.scenario import ScenarioRequest
from math import radians, cos, sin, asin, sqrt

class OSMScenarioGenerator:
    def __init__(self, output_dir: str):
        """
        Initializes the OSM scenario generator
        output_dir: Directory path where output files will be stored
        """
        # Basic configuration
        self.output_dir = output_dir
        self.prefix = "osm"
        self.files = {}
        self.files_relative = {}
        self.route_files = []

        # Verify and initialize SUMO environment
        self.sumo_home = os.environ.get("SUMO_HOME")
        if not self.sumo_home:
            raise EnvironmentError("SUMO_HOME environment variable is not set")

        # Add SUMO tools to Python path
        tools_dir = os.path.join(self.sumo_home, "tools")
        if tools_dir not in sys.path:
            sys.path.append(tools_dir)

        # Set up typemap paths
        self.typemapdir = os.path.join(self.sumo_home, "data", "typemap")
        self.typemaps = {
            "net": os.path.join(self.typemapdir, "osmNetconvert.typ.xml"),
            "poly": os.path.join(self.typemapdir, "osmPolyconvert.typ.xml"),
        }

        # Import required SUMO tools
        try:
            import osmGet
            import osmBuild
            import randomTrips
            self.osmGet = osmGet
            self.osmBuild = osmBuild
            self.randomTrips = randomTrips
        except ImportError as e:
            raise ImportError(f"Could not import required module: {str(e)}")

    def filename(self, use: str, name: str, usePrefix: bool = True) -> str:
        """
        Generates file names and paths consistently throughout the system
        """
        prefix = self.prefix if usePrefix else ''
        filename = f"{prefix}{name}"
        if use in ['net', 'poly', 'trips']:
            filename += '.gz'
        self.files_relative[use] = filename
        self.files[use] = os.path.join(self.output_dir, filename)
        return self.files[use]

    def calculate_selected_area_bounds(self, center: List[float], canvas_rect: List[float]) -> List[float]:
        """
        Calculates geographical bounds based on selected area
        """
        lat, lon = center
        d_lat = canvas_rect[3] - canvas_rect[1]
        d_lon = canvas_rect[2] - canvas_rect[0]

        lat_km = 111.0
        lon_km = cos(radians(lat)) * 111.0

        west = lon - (d_lon / 2) / lon_km
        east = lon + (d_lon / 2) / lon_km
        south = lat - (d_lat / 2) / lat_km
        north = lat + (d_lat / 2) / lat_km

        return [west, south, east, north]

    async def generate(self, request: ScenarioRequest, selected_area = None) -> List[str]:
        """
        Main process for scenario generation
        """
        try:
            selected_area = request.selectedArea if request.selectedArea else None
            # Create network configuration file
            self.filename("netccfg", ".netccfg")
            
            # Download OSM data
            self.filename("osm", "_bbox.osm.xml.gz")
            await self._download_osm_data(request.coordinates, selected_area)
            
            # Generate network
            self.filename("net", ".net.xml")
            await self._build_network(
                self.files['osm'],
                request.vehicles,
                request.options,
                request.roadTypes
            )

            # Generate routes for vehicles
            route_files = await self._generate_routes(request.vehicles, request.duration)
            self.route_files = route_files

            # Create SUMO configuration
            config_file = await self._create_sumo_config()

            return [self.files["net"], config_file] + route_files

        except Exception as e:
            print(f"Error in generate: {str(e)}")
            raise

    async def _download_osm_data(self, coordinates: List[float], selected_area = None) -> None:
        """
        OpenStreetMap 데이터 다운로드
        coordinates: 전체 지도의 좌표 범위
        selected_area: 사용자가 선택한 영역 정보 (있는 경우)
        """
        try:
            west, south, east, north = coordinates
            
            # 선택 영역이 있는 경우, 해당 영역으로 범위 조정
            if selected_area:
                rect = selected_area['rect']
                center = selected_area['center']
                
                # 선택 영역의 좌표를 실제 지리 좌표로 변환
                adjusted_bounds = self.calculate_selected_area_bounds(center, rect)
                west, south, east, north = adjusted_bounds

            osmArgs = [
                "-b", f"{west},{south},{east},{north}",
                "-p", self.prefix,
                "-d", self.output_dir,
                "-z"
            ]


            # 현재 디렉토리 저장
            original_dir = os.getcwd()
            try:
                # 작업 디렉토리 변경
                os.chdir(self.output_dir)
                
                print(f"Downloading OSM data with arguments: {osmArgs}")  # 디버깅용 출력
                self.osmGet.get(osmArgs)
                
                # 파일 생성 확인
                if not os.path.exists(self.files['osm']):
                    raise FileNotFoundError(f"OSM data file was not created: {self.files['osm']}")
                    
            finally:
                os.chdir(original_dir)

        except Exception as e:
            error_msg = f"Failed to download OSM data: {str(e)}"
            print(error_msg)  # 오류 정보 출력
            raise Exception(error_msg)

    async def _build_network(self, osm_file: str, vehicles: dict, options: dict, road_types: dict) -> str:
        try:
            # 도로 타입 필터링 문자열 생성
            edge_removal = []
            if road_types and road_types.get("Highway"):
                edge_types = []
                for road_type in road_types["Highway"]:
                    # 기본 도로 타입과 연결로 추가
                    edge_types.append(f"highway.{road_type}")
                    edge_types.append(f"highway.{road_type}_link")
                edge_str = ",".join(edge_types)
                if edge_str:
                    edge_removal.append(f'        <keep-edges.by-type value="{edge_str}"/>')
            
            # 차량 클래스 필터링 추가
            if options.get("carOnlyNetwork"):
                edge_removal.append('        <keep-edges.by-vclass value="passenger,bus,delivery,truck"/>')

            # netconvert 설정 파일 생성
            netccfg_content = f"""<?xml version="1.0" encoding="UTF-8"?>
    <configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/netconvertConfiguration.xsd">
        <input>
            <type-files value="{self.typemaps['net']}"/>
            <osm-files value="{osm_file}"/>
        </input>
        <output>
            <output-file value="{self.files['net']}"/>
            <output.street-names value="true"/>
            <output.original-names value="true"/>
        </output>
        <processing>
            <geometry.remove value="true"/>
            <roundabouts.guess value="true"/>
            <ramps.guess value="true"/>
            <junctions.join value="true"/>
            <geometry.max-grade.fix value="true"/>
            <lefthand value="false"/>
        </processing>
        <tls_building>
            <tls.discard-simple value="true"/>
            <tls.join value="true"/>
            <tls.guess-signals value="true"/>
            <tls.default-type value="actuated"/>
        </tls_building>
        {f'    <edge_removal>\n{chr(10).join(edge_removal)}\n    </edge_removal>' if edge_removal else ''}
        <junctions>
            <junctions.corner-detail value="5"/>
            <junctions.limit-turn-speed value="5.5"/>
            <rectangular-lane-cut value="true"/>
        </junctions>
        <pedestrian>
            <sidewalks.guess value="true"/>
            <crossings.guess value="true"/>
            <walkingareas value="true"/>
        </pedestrian>
    </configuration>"""

            # 설정 파일 저장
            with open(self.files["netccfg"], "w") as f:
                f.write(netccfg_content)

            print(f"Created netconvert config file: {self.files['netccfg']}")
            print(f"Config content:\n{netccfg_content}")

            # netconvert 실행
            cmd = ["netconvert", "-c", self.files["netccfg"]]
            process = subprocess.run(cmd, capture_output=True, text=True, check=True)

            if process.stderr:
                print("netconvert warnings:", process.stderr)

            return self.files["net"]

        except subprocess.CalledProcessError as e:
            print(f"netconvert stderr: {e.stderr}")
            raise Exception(f"Failed to build network: {str(e)}")
        except Exception as e:
            print(f"Unexpected error in _build_network: {str(e)}")
            raise

    async def _generate_routes(self, vehicles: dict, duration: int) -> List[str]:
        route_files = []
        
        for vtype, settings in vehicles.items():
            if not settings.enabled:
                continue

            trips_file = self.filename(f"{vtype}_trips", f".{vtype}.trips.xml")
            route_file = self.filename(f"{vtype}_routes", f".{vtype}.rou.xml")

            # ID 관리를 위한 prefix 사용
            trip_options = [
                "-n", self.files["net"],
                "-o", trips_file,
                "-r", route_file,
                "-b", "0",
                "-e", str(duration),
                "--fringe-factor", str(settings.fringeFactor),
                "--vehicle-class", vtype,
                "--validate",
                "--insertion-density", str(settings.count),
                "--prefix", vtype  # 차량 타입을 prefix로 사용
            ]

            try:
                print(f"Generating trips and routes for {vtype}...")
                self.randomTrips.main(self.randomTrips.get_options(trip_options))
                
                if os.path.exists(trips_file) and os.path.exists(route_file):
                    print(f"Successfully generated files for {vtype}")
                    route_files.append(route_file)
                else:
                    print(f"Warning: Failed to generate files for {vtype}")
                    
            except Exception as e:
                print(f"Error generating routes for {vtype}: {str(e)}")
                continue

        return route_files

    async def _create_sumo_config(self) -> str:
        """
        Creates SUMO configuration file
        """
        self.filename("config", ".sumocfg")

        config_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/sumoConfiguration.xsd">
    <input>
        <net-file value="{os.path.basename(self.files['net'])}"/>
        <route-files value="{','.join(os.path.basename(f) for f in self.route_files)}"/>
    </input>
    <time>
        <begin value="0"/>
    </time>
    <processing>
        <ignore-route-errors value="true"/>
    </processing>
    <routing>
        <device.rerouting.adaptation-interval value="10"/>
        <device.rerouting.adaptation-steps value="18"/>
    </routing>
    <report>
        <verbose value="true"/>
        <duration-log.statistics value="true"/>
        <no-step-log value="true"/>
    </report>
</configuration>"""

        with open(self.files["config"], "w") as f:
            f.write(config_content)

        return self.files["config"]