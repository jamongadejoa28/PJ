# src/backend/app/services/simulation_runner.py

import os
import subprocess
import logging
from typing import Optional

class SimulationRunner:
   def __init__(self, data_dir: str):
       self.data_dir = data_dir
       logging.basicConfig(level=logging.INFO)

   def run(self, duration: int, max_teleport: Optional[int] = -1) -> bool:
       try:
           config_file = os.path.join(self.data_dir, "test.sumocfg")
           if not os.path.exists(config_file):
               raise FileNotFoundError("SUMO config file not found")

           cmd = [
               "sumo",
               "-c", config_file,
               "--quit-on-end",
               "--waiting-time-memory", "100",
               "--time-to-teleport", str(max_teleport),
               "--end", str(duration),
               "--max-depart-delay", str(duration),
               "--quit-on-end", "true",
               "--verbose", "true"
           ]

           process = subprocess.run(cmd, check=True, capture_output=True, text=True)
           
           if process.returncode == 0:
               logging.info("Simulation completed successfully")
               return True
           else:
               logging.error(f"Simulation failed: {process.stderr}")
               return False

       except Exception as e:
           logging.error(f"Error running simulation: {str(e)}")
           return False