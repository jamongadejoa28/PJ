// src/app/components/VehicleMakers.tsx

"use client";

import React, { useEffect, useRef, useState } from "react";
import type { VehicleMarkersProps } from "../types/types";

// 지구 관련 상수 정의
const METERS_PER_DEGREE = 111319.5; // 적도에서의 1도당 거리 (미터)

// 차량 타입별 이미지 매핑 함수
const getImageType = (type: string): string => {
  const normalizedType = type.toLowerCase().trim().split("_")[0];
  const typeMap: { [key: string]: string } = {
    passenger: "passenger",
    private: "passenger",
    truck: "truck",
    trailer: "truck",
    bus: "bus",
    coach: "bus",
    motorcycle: "motorcycle",
    moped: "motorcycle",
    bicycle: "bicycle",
    bike: "bicycle",
    pedestrian: "pedestrian",
    person: "pedestrian",
    tram: "tram",
    rail_urban: "urban_train",
    rail: "train",
    rail_electric: "train",
    ship: "ship",
    emergency: "passenger",
    authority: "passenger",
    army: "truck",
    vip: "passenger",
  };
  return typeMap[normalizedType] || "passenger";
};

// 차량 타입별 실제 크기 정의 (미터 단위)
const getVehicleDimensions = (
  type: string
): { width: number; length: number } => {
  const normalizedType = type.toLowerCase().trim().split("_")[0];
  const dimensions = {
    passenger: { width: 1.8, length: 4.5 },
    truck: { width: 2.5, length: 8.0 },
    bus: { width: 2.5, length: 12.0 },
    motorcycle: { width: 0.8, length: 2.0 },
    bicycle: { width: 0.6, length: 1.8 },
    pedestrian: { width: 0.5, length: 0.5 },
    tram: { width: 2.5, length: 15.0 },
    rail_urban: { width: 3.0, length: 20.0 },
    rail: { width: 3.2, length: 25.0 },
    ship: { width: 5.0, length: 15.0 },
  };
  return dimensions[normalizedType] || dimensions["passenger"];
};

// 차량 타입별 색상 필터 매핑
const getVehicleColorFilter = (type: string): string => {
  const filterMap: { [key: string]: string } = {
    passenger:
      "brightness(0) saturate(100%) invert(16%) sepia(96%) saturate(7404%) hue-rotate(359deg) brightness(97%) contrast(113%)",
    truck:
      "brightness(0) saturate(100%) invert(72%) sepia(88%) saturate(1099%) hue-rotate(359deg) brightness(102%) contrast(108%)",
    bus: "brightness(0) saturate(100%) invert(21%) sepia(96%) saturate(4146%) hue-rotate(238deg) brightness(97%) contrast(108%)",
  };
  const baseType = getImageType(type);
  return filterMap[baseType] || filterMap["passenger"];
};

// 현재 줌 레벨과 위도에서의 미터당 픽셀 비율 계산
const getMetersPerPixel = (lat: number, level: number): number => {
  const metersPerDegree = METERS_PER_DEGREE * Math.cos((lat * Math.PI) / 180);
  const degreesPerPixel = 360 / (256 * Math.pow(2, 13 - level));
  return metersPerDegree * degreesPerPixel;
};

// CustomGroundOverlay 클래스를 저장할 변수
let CustomGroundOverlay: any = null;

const VehicleMarkers: React.FC<VehicleMarkersProps & { map: any }> = ({
  map,
  simulationActive,
  duration,
  onError,
  onComplete,
}) => {
  // 상태 및 참조 관리
  const vehiclesRef = useRef<Map<string, any>>(new Map());
  const socketRef = useRef<WebSocket | null>(null);
  const durationRef = useRef<number>(duration);
  const [simulationSpeed, setSimulationSpeed] = useState(1.0);
  const [isMotorwayBlocked, setIsMotorwayBlocked] = useState(false);
  const [controlStatus, setControlStatus] = useState({
    speed_applied: false,
    block_applied: false,
  });
  const [vehicleStats, setVehicleStats] = useState({
    totalCount: 0,
    typeDistribution: {} as Record<string, number>,
  });

  // CustomGroundOverlay 클래스 초기화
  useEffect(() => {
    if (!window.kakao?.maps || !map) return;

    class GroundOverlay extends window.kakao.maps.AbstractOverlay {
      private bounds: any;
      private node: HTMLDivElement;
      private img: HTMLImageElement;
      private type: string;

      constructor(bounds: any, imgSrc: string, vehicleType: string) {
        super();
        this.bounds = bounds;
        this.type = vehicleType;

        this.node = document.createElement("div");
        this.node.style.position = "absolute";
        this.node.style.zIndex = "1";

        this.img = document.createElement("img");
        this.img.style.position = "absolute";
        this.img.style.width = "100%";
        this.img.style.height = "100%";
        this.img.src = imgSrc;
        this.img.style.filter = getVehicleColorFilter(vehicleType);

        this.node.appendChild(this.img);
      }

      onAdd() {
        const panel = this.getPanels().overlayLayer;
        panel.appendChild(this.node);
      }

      draw() {
        const projection = this.getProjection();
        if (!projection) return;

        const ne = projection.pointFromCoords(this.bounds.getNorthEast());
        const sw = projection.pointFromCoords(this.bounds.getSouthWest());

        const width = Math.abs(ne.x - sw.x);
        const height = Math.abs(sw.y - ne.y);

        this.node.style.left = sw.x + "px";
        this.node.style.top = ne.y + "px";
        this.node.style.width = width + "px";
        this.node.style.height = height + "px";
      }

      onRemove() {
        this.node.parentNode?.removeChild(this.node);
      }

      setPosition(position: any) {
        const lat = position.getLat();
        const lng = position.getLng();
        const dimensions = getVehicleDimensions(this.type);

        const latDelta = dimensions.length / 2 / METERS_PER_DEGREE;
        const lngDelta =
          dimensions.width /
          2 /
          (METERS_PER_DEGREE * Math.cos((lat * Math.PI) / 180));

        const sw = new window.kakao.maps.LatLng(lat - latDelta, lng - lngDelta);
        const ne = new window.kakao.maps.LatLng(lat + latDelta, lng + lngDelta);
        this.bounds = new window.kakao.maps.LatLngBounds(sw, ne);

        this.draw();
      }
    }

    CustomGroundOverlay = GroundOverlay;
  }, [map]);

  // 제어 명령 전송 함수
  const sendControlCommand = (command: any) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      try {
        socketRef.current.send(JSON.stringify(command));
        console.log("제어 명령 전송:", command);
      } catch (error) {
        console.error("제어 명령 전송 실패:", error);
      }
    }
  };

  // 속도 제어 핸들러
  const handleSpeedChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newSpeed = parseFloat(e.target.value);
    setSimulationSpeed(newSpeed);
    sendControlCommand({ simulationSpeed: newSpeed });
  };

  // 도로 차단 제어 핸들러
  const handleMotorwayToggle = () => {
    const newBlockState = !isMotorwayBlocked;
    setIsMotorwayBlocked(newBlockState);
    sendControlCommand({ blockMotorwayLinks: newBlockState });
  };

  // 차량 오버레이 생성 함수
  const createNewVehicleOverlay = (
    id: string,
    position: any,
    type: string,
    dimensions: any
  ) => {
    if (!CustomGroundOverlay) return;

    const lat = position.getLat();
    const lng = position.getLng();

    const latDelta = dimensions.length / 2 / METERS_PER_DEGREE;
    const lngDelta =
      dimensions.width /
      2 /
      (METERS_PER_DEGREE * Math.cos((lat * Math.PI) / 180));

    const sw = new window.kakao.maps.LatLng(lat - latDelta, lng - lngDelta);
    const ne = new window.kakao.maps.LatLng(lat + latDelta, lng + lngDelta);
    const bounds = new window.kakao.maps.LatLngBounds(sw, ne);

    const overlay = new CustomGroundOverlay(
      bounds,
      `/images/${type}.png`,
      type
    );
    overlay.setMap(map);
    vehiclesRef.current.set(id, overlay);
  };

  // 차량 위치 업데이트 함수
  const updateVehiclePositions = (vehicles: any[]) => {
    const incomingIds = new Set(vehicles.map((v) => v.id));
    const existingVehicles = vehiclesRef.current;

    // 제거될 차량 처리
    existingVehicles.forEach((overlay, id) => {
      if (!incomingIds.has(id)) {
        overlay.setMap(null);
        existingVehicles.delete(id);
      }
    });

    // 차량 통계 업데이트
    const typeCount: Record<string, number> = {};
    vehicles.forEach((vehicle) => {
      const vehicleType = getImageType(vehicle.type);
      typeCount[vehicleType] = (typeCount[vehicleType] || 0) + 1;
    });

    setVehicleStats({
      totalCount: vehicles.length,
      typeDistribution: typeCount,
    });

    // 차량 위치 업데이트 또는 새로운 차량 추가
    vehicles.forEach((vehicle) => {
      const { id, position, type } = vehicle;
      const vehiclePosition = new window.kakao.maps.LatLng(
        position.lat,
        position.lng
      );

      if (existingVehicles.has(id)) {
        existingVehicles.get(id).setPosition(vehiclePosition);
      } else {
        const vehicleType = getImageType(type);
        const dimensions = getVehicleDimensions(type);
        createNewVehicleOverlay(id, vehiclePosition, vehicleType, dimensions);
      }
    });
  };

  // WebSocket 연결 및 메시지 처리
  useEffect(() => {
    if (
      !window.kakao?.maps ||
      !map ||
      !simulationActive ||
      !CustomGroundOverlay
    )
      return;

    const ws = new WebSocket("ws://localhost:8000/api/scenario/ws/simulation");

    ws.onopen = () => {
      console.log("WebSocket 연결 성공");
      sendControlCommand({
        duration: durationRef.current,
        simulationSpeed,
        blockMotorwayLinks: isMotorwayBlocked,
      });
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        if (
          message.type === "vehicle_positions" &&
          Array.isArray(message.data)
        ) {
          updateVehiclePositions(message.data);

          if (message.controlStatus) {
            setControlStatus(message.controlStatus);
          }
        } else if (message.type === "simulation_complete") {
          console.log("시뮬레이션 완료");
          if (onComplete) onComplete();
        }
      } catch (error) {
        console.error("메시지 처리 오류:", error);
        if (onError) onError();
      }
    };

    ws.onerror = (error) => {
      console.error("WebSocket 오류:", error);
      if (onError) onError();
    };

    ws.onclose = (event) => {
      console.log(`WebSocket 연결 종료. 코드: ${event.code}`);
      if (event.code !== 1000 && onError) onError();
    };

    socketRef.current = ws;

    return () => cleanupSimulation(ws);
  }, [simulationActive, map]);

  // 시뮬레이션 정리 함수
  const cleanupSimulation = (ws: WebSocket) => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.close(1000);
    }
    socketRef.current = null;
    vehiclesRef.current.forEach((overlay) => overlay.setMap(null));
    vehiclesRef.current.clear();
  };

  return (
    <div className="absolute top-24 left-4 bg-white rounded-lg shadow-md p-4 z-10 space-y-4">
      <div className="control-section">
        <div className="speed-control">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            시뮬레이션 속도 (x{simulationSpeed.toFixed(1)})
            {controlStatus.speed_applied && (
              <span className="text-green-500 ml-2" title="적용됨">
                ✓
              </span>
            )}
          </label>
          <input
            type="range"
            min="0.1"
            max="10"
            step="0.1"
            value={simulationSpeed}
            onChange={handleSpeedChange}
            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
          />
          <div className="flex justify-between text-xs text-gray-500 mt-1">
            <span>0.1x</span>
            <span>10x</span>
          </div>
        </div>
      </div>

      <div className="road-control">
        <button
          onClick={handleMotorwayToggle}
          className={`w-full px-4 py-2 text-sm font-medium rounded-md ${
            isMotorwayBlocked
              ? "bg-red-600 text-white hover:bg-red-700"
              : "bg-blue-600 text-white hover:bg-blue-700"
          } transition-colors duration-200`}
        >
          {isMotorwayBlocked ? "도로 차단 해제" : "고속도로 진입로 차단"}
          {controlStatus.block_applied && (
            <span className="ml-2" title="적용됨">
              ✓
            </span>
          )}
        </button>
      </div>

      <div className="statistics-section">
        <div className="text-sm font-medium text-gray-700">
          총 차량 수: {vehicleStats.totalCount}
        </div>
        <div className="text-xs text-gray-500 mt-1">
          {Object.entries(vehicleStats.typeDistribution).map(
            ([type, count]) => (
              <div key={type} className="flex justify-between">
                <span>{type}:</span>
                <span>{count}대</span>
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
};

export default VehicleMarkers;
