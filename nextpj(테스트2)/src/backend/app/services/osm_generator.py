import os
import sys
import subprocess
import osmnx as ox
from typing import Dict, Any, Tuple
import logging

class OSMGenerator:
    def __init__(self):
        if 'SUMO_HOME' not in os.environ:
            raise EnvironmentError("SUMO_HOME not set")
        
        self.sumo_home = os.environ['SUMO_HOME']
        self.tools_dir = os.path.join(self.sumo_home, 'tools')
        self.data_dir = os.path.join('data')
        os.makedirs(self.data_dir, exist_ok=True)

    async def generate(self, data: Dict[str, Any], progress_callback) -> Tuple[bool, str]:
        try:
            await progress_callback(10, "Configuring OSM extraction...")
            ox.config(use_cache=True, log_console=True)
            
            # OSM 데이터 추출
            G = ox.graph_from_point(
                (data["coordinates"]["lat"], data["coordinates"]["lng"]),
                dist=data["radius"],
                network_type="drive",
                retain_all=True,
                simplify=False
            )
            
            await progress_callback(30, "Saving OSM data...")
            osm_file = os.path.join(self.data_dir, "test.osm")
            net_file = os.path.join(self.data_dir, "test.net.xml")
            ox.save_graph_xml(G, osm_file)

            await progress_callback(50, "Generating network...")
            netconvert_options = [
                "netconvert",
                "-i", osm_file,
                "-o", net_file,
                "--geometry.remove",
                "--roundabouts.guess",
                "--ramps.guess",
                "--junctions.join",
                "--tls.guess-signals",
                "--tls.discard-simple",
                "--tls.join",
                "--tls.default-type", "actuated",
                "--junctions.corner-detail", "5",
                "--rectangular-lane-cut"
            ]

            # 도로타입 필터링
            if data.get("roadTypes"):
                filters = []
                for category, types in data["roadTypes"].items():
                    filters.extend([f"highway.{t}" for t in types])
                if filters:
                    netconvert_options.extend(["--keep-edges.by-type", ",".join(filters)])

            subprocess.run(netconvert_options, check=True)
            
            await progress_callback(70, "Generating routes...")
            await self._generate_routes(data["vehicles"], data["duration"])

            await progress_callback(90, "Creating SUMO configuration...")
            await self._create_sumo_config()

            return True, "Network generation complete"

        except Exception as e:
            logging.error(f"Generation failed: {str(e)}")
            return False, str(e)

    async def _generate_routes(self, vehicles: Dict, duration: int):
        sys.path.append(self.tools_dir)
        import randomTrips
        
        for vehicle in vehicles:
            if not vehicle["enabled"]:
                continue
            
            net_file = os.path.join(self.data_dir, "test.net.xml")
            trips_file = os.path.join(self.data_dir, f"test.{vehicle['internal']}.trips.xml")
            
            options = [
                "-n", net_file,
                "-o", trips_file,
                "-b", "0",
                "-e", str(duration),
                "--fringe-factor", str(vehicle["fringeFactor"]),
                "--vehicle-class", vehicle["internal"],
                "--validate"
            ]
            
            randomTrips.main(randomTrips.get_options(options))

    async def _create_sumo_config(self):
        config_content = """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <input>
        <net-file value="test.net.xml"/>
        <route-files value="test.*.trips.xml"/>
    </input>
    <time>
        <begin value="0"/>
        <end value="3600"/>
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
        with open(config_file, "w") as f:
            f.write(config_content)