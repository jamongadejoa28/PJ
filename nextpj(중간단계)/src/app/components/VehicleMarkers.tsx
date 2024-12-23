"use client";

import React, { useEffect, useRef } from "react";
import type { VehicleMarkersProps } from "../types/types";

// 차량 타입별 이미지 매핑 함수
const getImageType = (type: string): string => {
  const normalizedType = type.toLowerCase().trim().split('_')[0];
  
  const typeMap: { [key: string]: string } = {
    'passenger': 'passenger',
    'private': 'passenger',
    'truck': 'truck',
    'trailer': 'truck',
    'bus': 'bus',
    'coach': 'bus',
    'motorcycle': 'motorcycle',
    'moped': 'motorcycle',
    'bicycle': 'bicycle',
    'bike': 'bicycle',
    'pedestrian': 'pedestrian',
    'person': 'pedestrian',
    'tram': 'tram',
    'rail_urban': 'urban_train',
    'rail': 'train',
    'rail_electric': 'train',
    'ship': 'ship',
    'emergency': 'passenger',
    'authority': 'passenger',
    'army': 'truck',
    'vip': 'passenger'
  };

  const mappedType = typeMap[normalizedType];
  if (!mappedType) {
    console.log(`Unknown vehicle type: ${type}, using default 'passenger'`);
  }
  return mappedType || 'passenger';
};

// 차량 타입별 색상 필터 매핑
const getVehicleColorFilter = (type: string): string => {
  // 기본 차량 타입에 따른 색상 필터 설정
  const filterMap: { [key: string]: string } = {
    'passenger': 'brightness(0) saturate(100%) invert(16%) sepia(96%) saturate(7404%) hue-rotate(359deg) brightness(97%) contrast(113%)', // 빨간색
    'truck': 'brightness(0) saturate(100%) invert(72%) sepia(88%) saturate(1099%) hue-rotate(180deg) brightness(102%) contrast(108%)',    // 노란색
    'bus': 'brightness(0) saturate(100%) invert(21%) sepia(96%) saturate(4146%) hue-rotate(238deg) brightness(97%) contrast(108%)'       // 파란색
  };

  // 차량 타입을 기본 타입으로 변환
  const baseType = getImageType(type);
  return filterMap[baseType] || filterMap['passenger']; // 기본값은 passenger(빨간색)
};

// 차량 정보 그룹화 함수
const groupVehiclesByType = (vehicles: any[]) => {
  const groupedVehicles: { [key: string]: number } = {};
  vehicles.forEach(vehicle => {
    const type = getImageType(vehicle.type);
    groupedVehicles[type] = (groupedVehicles[type] || 0) + 1;
  });
  return groupedVehicles;
};

let GroundOverlay: any = null;

const VehicleMarkers: React.FC<VehicleMarkersProps & { map: any }> = ({
  map,
  simulationActive,
  duration,
  onError,
  onComplete,
}) => {
  const vehiclesRef = useRef<Map<string, any>>(new Map());
  const socketRef = useRef<WebSocket | null>(null);
  const durationRef = useRef<number>(duration);
  const isManualStart = useRef<boolean>(false);

  // GroundOverlay 클래스 초기화
  useEffect(() => {
    if (!window.kakao?.maps || !map) return;

    class CustomGroundOverlay extends window.kakao.maps.AbstractOverlay {
      private bounds: any;
      private node: HTMLDivElement;
      private img: HTMLImageElement;
      private type: string;

      constructor(bounds: any, imgSrc: string, vehicleType: string) {
        super();
        this.bounds = bounds;
        this.type = vehicleType;
        this.node = document.createElement('div');
        this.node.style.position = 'absolute';
        this.node.style.zIndex = '1';
        
        this.img = document.createElement('img');
        this.img.style.position = 'absolute';
        this.img.style.width = '100%';
        this.img.style.height = '100%';
        this.img.src = imgSrc;
        // 차량 타입에 따른 색상 필터 적용
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
        
        this.node.style.left = sw.x + 'px';
        this.node.style.top = ne.y + 'px';
        this.node.style.width = width + 'px';
        this.node.style.height = height + 'px';
      }

      onRemove() {
        this.node.parentNode?.removeChild(this.node);
      }

      setPosition(position: any) {
        const lat = position.getLat();
        const lng = position.getLng();
        const latDelta = 0.0001;
        const lngDelta = 0.0001;
        
        const sw = new window.kakao.maps.LatLng(lat - latDelta, lng - lngDelta);
        const ne = new window.kakao.maps.LatLng(lat + latDelta, lng + lngDelta);
        this.bounds = new window.kakao.maps.LatLngBounds(sw, ne);
        
        this.draw();
      }
    }

    GroundOverlay = CustomGroundOverlay;
  }, [map]);

  useEffect(() => {
    if (!window.kakao?.maps || !map || !simulationActive || !GroundOverlay) return;

    if (socketRef.current?.readyState === WebSocket.OPEN) {
      console.log("WebSocket 연결이 이미 존재합니다");
      return;
    }

    const ws = new WebSocket("ws://localhost:8000/api/scenario/ws/simulation");

    ws.onopen = () => {
      console.log("WebSocket 연결이 열렸습니다");
      ws.send(JSON.stringify({ duration: durationRef.current }));
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        if (message.type === "vehicle_positions" && Array.isArray(message.data)) {
          const incomingVehicles = message.data;
          const existingVehicles = vehiclesRef.current;

          // 차량 타입별 그룹화된 정보 로깅
          const groupedVehicles = groupVehiclesByType(incomingVehicles);
          console.log("현재 차량 현황:", {
            총_차량_수: incomingVehicles.length,
            타입별_차량_수: groupedVehicles,
            차량_상세: incomingVehicles.map(v => ({
              id: v.id,
              type: getImageType(v.type),
              위치: `(${v.position.lat.toFixed(6)}, ${v.position.lng.toFixed(6)})`
            }))
          });

          // 제거될 차량 처리
          const incomingIds = new Set(incomingVehicles.map((v: any) => v.id));
          existingVehicles.forEach((overlay, id) => {
            if (!incomingIds.has(id)) {
              overlay.setMap(null);
              existingVehicles.delete(id);
            }
          });

          // 차량 위치 업데이트 또는 새로운 차량 추가
          incomingVehicles.forEach((vehicle: any) => {
            const { id, position, type } = vehicle;
            const vehiclePosition = new window.kakao.maps.LatLng(position.lat, position.lng);

            if (existingVehicles.has(id)) {
              existingVehicles.get(id).setPosition(vehiclePosition);
            } else {
              const latDelta = 0.0001;
              const lngDelta = 0.0001;
              const sw = new window.kakao.maps.LatLng(
                position.lat - latDelta,
                position.lng - lngDelta
              );
              const ne = new window.kakao.maps.LatLng(
                position.lat + latDelta,
                position.lng + lngDelta
              );
              const bounds = new window.kakao.maps.LatLngBounds(sw, ne);

              const vehicleType = getImageType(type);
              const overlay = new GroundOverlay(bounds, `/images/${vehicleType}.png`, type);
              overlay.setMap(map);
              existingVehicles.set(id, overlay);
            }
          });
        } else if (message.type === "simulation_complete") {
          console.log("시뮬레이션이 완료되었습니다");
          cleanupSimulation(ws);
          if (onComplete) onComplete();
        }
      } catch (error) {
        console.error("메시지 처리 중 오류:", error);
        cleanupSimulation(ws);
        if (onError) onError();
      }
    };

    ws.onerror = (error) => {
      console.error("WebSocket 오류:", error);
      cleanupSimulation(ws);
      if (onError) onError();
    };

    ws.onclose = (event) => {
      console.log(`WebSocket 연결이 종료되었습니다. 코드: ${event.code}`);
      socketRef.current = null;
      if (event.code !== 1000 && onError) onError();
    };

    socketRef.current = ws;

    return () => {
      cleanupSimulation(ws);
    };
  }, [simulationActive, onError, onComplete, map]);

  const cleanupSimulation = (ws: WebSocket) => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: "disconnect" }));
      ws.close(1000);
    }
    socketRef.current = null;
    vehiclesRef.current.forEach((overlay) => overlay.setMap(null));
    vehiclesRef.current.clear();
    isManualStart.current = false;
  };

  return null;
};

export default VehicleMarkers;
