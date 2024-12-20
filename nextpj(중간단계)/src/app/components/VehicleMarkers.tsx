// src/app/components/VehicleMarkers.tsx

import React, { useEffect, useState, useRef } from "react";
import { MapMarker } from "react-kakao-maps-sdk";
import type { Vehicle, VehicleMarkersProps } from "../types/types";

const getImageType = (type: string): string => {
  const typeMap: { [key: string]: string } = {
    passenger: "passenger",
    truck: "truck",
    bus: "bus",
    motorcycle: "motorcycle",
    bicycle: "bicycle",
    pedestrian: "pedestrian",
    tram: "tram",
    rail_urban: "urban_train",
    rail: "train",
    ship: "ship",
  };
  return typeMap[type] || "passenger";
};

const VehicleMarkers: React.FC<VehicleMarkersProps> = ({
  simulationActive,
  duration,
  onError,    // 에러 콜백
  onComplete, // 완료 콜백
}) => {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const socketRef = useRef<WebSocket | null>(null);
  const durationRef = useRef<number>(duration); // duration을 ref로 관리
  const isManualStart = useRef<boolean>(false);

  // duration이 변경될 때마다 ref를 업데이트
  useEffect(() => {
    durationRef.current = duration;
  }, [duration]);

  useEffect(() => {
    if (simulationActive) {
      isManualStart.current = true;
      // 이미 WebSocket 연결이 존재하면 새로 만들지 않음
      if (socketRef.current) {
        console.warn("WebSocket 연결이 이미 존재합니다.");
        return;
      }

      const ws = new WebSocket("ws://localhost:8000/api/scenario/ws/simulation");

      ws.onopen = () => {
        console.log("WebSocket 연결이 열렸습니다.");
        ws.send(JSON.stringify({ duration: durationRef.current }));
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);

          if (message.type === "vehicle_positions" && Array.isArray(message.data)) {
            setVehicles(message.data);
            const carLen = message.data.length;
            const carId = message.data[0]?.id;
            const carLat = message.data[0]?.position.lat;
            const carLng = message.data[0]?.position.lng;
            console.log(
              `차량 수: ${carLen}, 차량id: ${carId}, 차량위치: (${carLat}, ${carLng})`
            );
          } else if (message.type === "simulation_complete") {
            console.log("시뮬레이션이 완료되었습니다.");
            if(ws.readyState === WebSocket.OPEN){ws.close(1000);}
            setVehicles([]);
            isManualStart.current = false;
            if (onComplete) onComplete(); // 완료 콜백 호출
          } else if (message.type === "error") {
            console.error("시뮬레이션 오류:", message.message);
            if(ws.readyState === WebSocket.OPEN){ws.close(1011);}
            setVehicles([]);
            isManualStart.current = false;
            if (onError) onError(); // 에러 콜백 호출
          }
        } catch (error) {
          console.error("메시지 처리 중 오류:", error);
          setVehicles([]);
          isManualStart.current = false;
          if (onError) onError(); // 에러 콜백 호출
        }
      };

      ws.onerror = (error) => {
        console.error("WebSocket 오류:", error);
        setVehicles([]);
        isManualStart.current = false;
        if (onError) onError(); // 에러 콜백 호출
      };

      ws.onclose = (event) => {
        console.log(`WebSocket 연결이 종료되었습니다. 코드: ${event.code}`);
        socketRef.current = null;
        setVehicles([]);
        isManualStart.current = false;
        // 정상 종료가 아닐 경우 에러 콜백 호출
        if (event.code !== 1000 && onError) onError();
      };

      // WebSocket 참조 저장
      socketRef.current = ws;

      // 클린업 함수: 컴포넌트 언마운트 시 WebSocket 종료
      return () => {
        if (socketRef.current?.readyState === WebSocket.OPEN) {
          socketRef.current.send(JSON.stringify({ type: "disconnect" }));
          socketRef.current.close(1000);
        }
        socketRef.current = null;
        isManualStart.current =
      };
    } else {
      // simulationActive가 false일 때 WebSocket 연결이 존재하면 종료
      if (socketRef.current) {
        socketRef.current.send(JSON.stringify({ type: "disconnect" }));
        socketRef.current.close(1000);
        socketRef.current = null;
        setVehicles([]);
      }
    }
  }, [simulationActive, onError, onComplete]); // duration은 의존성 배열에서 제거

  return (
    <>
      {vehicles.map((vehicle) => (
        <MapMarker
          key={vehicle.id}
          position={{ lat: vehicle.position.lat, lng: vehicle.position.lng }}
          image={{
            src: `/images/${getImageType(vehicle.type)}.png`,
            size: {
              width: 24,
              height: 24,
            },
          }}
          zIndex={1}
        />
      ))}
    </>
  );
};

export default VehicleMarkers;
