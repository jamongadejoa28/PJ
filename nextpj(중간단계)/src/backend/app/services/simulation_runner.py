# src/backend/app/services/simulation_runner.py

import os
import asyncio
import glob
from typing import Optional
import logging

logger = logging.getLogger(__name__)

class SimulationRunner:
    def __init__(self, data_dir: str):
        # PROJECT_ROOT를 정의하여 절대 경로 생성
        self.PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        self.data_dir = os.path.join(self.PROJECT_ROOT, "data")
        if not os.path.exists(self.data_dir):
            raise FileNotFoundError(f"Data directory not found: {self.data_dir}")
        self.current_process: Optional[asyncio.subprocess.Process] = None

    async def find_sumocfg_file(self) -> str:
        # 절대 경로 사용
        config_files = glob.glob(os.path.join(self.data_dir, "*.sumocfg"))
        if not config_files:
            raise FileNotFoundError(f"No .sumocfg file found in directory: {self.data_dir}")
        return config_files[0]
    
    async def _handle_stream(self, stream: asyncio.StreamReader, prefix: str):
        while True:
            line = await stream.readline()
            if not line:
                break
            try:
                decoded_line = line.decode('utf-8').strip()
            except UnicodeDecodeError:
                decoded_line = line.decode('cp949', errors='replace').strip()
            
            # 로그 출력
            logger.info(f"{prefix}: {decoded_line}")

    async def run_simulation(self, duration: int, max_teleport: Optional[int] = -1) -> bool:
        try:
            config_file = await self.find_sumocfg_file()
            
            # SUMO 명령어 구성
            cmd = [
                "sumo",
                "-c", config_file,
                "--waiting-time-memory", "100",
                "--time-to-teleport", str(max_teleport),
                "--end", str(duration),
                "--max-depart-delay", str(duration),
                "--quit-on-end", "true",
                "--device.rerouting.mode", "8",
                "--default.carfollowmodel", "EIDM",
                "--device.rerouting.probability", "1",
                "--ignore-route-errors", "true",
                "--fcd-output", os.path.join(self.data_dir, "fcd_output.xml")
            ]

            # 시뮬레이션 프로세스 실행
            self.current_process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )

            # stdout과 stderr 스트림 처리를 위한 태스크 생성
            stdout_task = asyncio.create_task(self._handle_stream(self.current_process.stdout, "STDOUT"))
            stderr_task = asyncio.create_task(self._handle_stream(self.current_process.stderr, "STDERR"))

            # 프로세스 완료 대기
            return_code = await self.current_process.wait()

            # 출력 스트림 태스크 완료 대기
            await stdout_task
            await stderr_task

            return return_code

        except Exception as e:
            logger.error(f"Simulation execution error: {str(e)}")
            raise
