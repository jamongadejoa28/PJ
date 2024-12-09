# src/backend/app/services/simulation_runner.py

import os
import subprocess
import logging
from typing import Optional
import asyncio

class SimulationRunner:
    def __init__(self, data_dir: str):
        """
        SimulationRunner 초기화

        Args:
            data_dir (str): SUMO 데이터 파일들이 위치한 디렉토리 경로
        """
        self.data_dir = data_dir
        self.config_file = os.path.join(self.data_dir, "test.sumocfg")
        
        # 로깅 설정
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(os.path.join(self.data_dir, 'simulation_runner.log')),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)

    async def run_async(self, duration: int, max_teleport: Optional[int] = -1) -> bool:
        """
        비동기적으로 시뮬레이션을 실행합니다.

        Args:
            duration (int): 시뮬레이션 종료 시간 (초 단위)
            max_teleport (Optional[int], optional): 차량의 텔레포트 최대 시간. 기본값은 -1 (무제한).

        Returns:
            bool: 시뮬레이션 실행 성공 여부
        """
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, self.run, duration, max_teleport)

    def run(self, duration: int, max_teleport: Optional[int] = -1) -> bool:
        """
        SUMO 시뮬레이션을 실행합니다.

        Args:
            duration (int): 시뮬레이션 종료 시간 (초 단위)
            max_teleport (Optional[int], optional): 차량의 텔레포트 최대 시간. 기본값은 -1 (무제한).

        Returns:
            bool: 시뮬레이션 실행 성공 여부
        """
        try:
            # 설정 파일 존재 여부 확인
            if not os.path.exists(self.config_file):
                raise FileNotFoundError(f"SUMO config file not found at: {self.config_file}")

            # SUMO 실행 명령어 구성
            cmd = [
                "sumo",  # CLI 모드로 실행
                "-c", self.config_file,
                "--waiting-time-memory", "100",
                "--time-to-teleport", str(max_teleport),
                "--end", str(duration),
                "--max-depart-delay", str(duration),
                "--quit-on-end", "true",
                "--verbose", "true"  # 디버깅을 위한 상세 로그 출력
            ]

            self.logger.info(f"Executing command: {' '.join(cmd)}")

            # SUMO 프로세스 실행
            process = subprocess.run(cmd, check=True, capture_output=True, text=True)

            # 실행 결과 확인
            if process.returncode == 0:
                self.logger.info("Simulation completed successfully.")
                self.logger.debug(f"Simulation output: {process.stdout}")
                return True
            else:
                self.logger.error(f"Simulation failed with return code {process.returncode}: {process.stderr}")
                return False

        except FileNotFoundError as fnf_error:
            self.logger.error(fnf_error)
            return False
        except subprocess.CalledProcessError as cpe:
            self.logger.error(f"Simulation process failed: {cpe.stderr}")
            return False
        except Exception as e:
            self.logger.error(f"Error running simulation: {str(e)}")
            return False