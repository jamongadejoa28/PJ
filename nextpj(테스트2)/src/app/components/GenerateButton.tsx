"use client";

import { useState } from "react";

interface GenerateButtonProps {
  coordinates: [number, number, number, number]; // [west, south, east, north]
  vehicles: any;
  options: any;
  duration: number;
  roadTypes: any;
}

const GenerateButton: React.FC<GenerateButtonProps> = ({
  coordinates,
  vehicles,
  options,
  duration,
  roadTypes,
}) => {
  const [status, setStatus] = useState<string>("");
  const [progress, setProgress] = useState<number>(0);
  const [isGenerating, setIsGenerating] = useState<boolean>(false);

  const handleGenerate = async () => {
    setIsGenerating(true);
    setStatus("Downloading OSM data...");
    setProgress(20);

    try {
      // osmWebWizard.py의 모든 옵션들을 포함하는 요청 데이터 구성
      const requestData = {
        // 선택된 지역의 좌표 (west, south, east, north)
        coordinates: coordinates,

        // 차량 설정
        // vehicles 배열의 각 항목을 객체로 변환하여 설정값 전달
        vehicles: vehicles.reduce(
          (acc, vehicle) => ({
            ...acc,
            [vehicle.internal]: {
              enabled: vehicle.enabled, // 해당 차량 타입 활성화 여부
              count: vehicle.count, // 차량 수 (insertion-rate)
              fringeFactor: vehicle.fringeFactor, // 외곽 지역 가중치
            },
          }),
          {}
        ),

        // 시뮬레이션 지속 시간 (초 단위)
        duration: duration,

        // 네트워크 및 시뮬레이션 옵션들
        options: {
          // 네트워크 생성 관련 옵션
          publicTransport: options.publicTransport, // 대중교통 포함 여부
          leftHand: options.leftHand, // 좌측 통행 여부
          polygons: options.polygons, // 다각형(건물 등) 포함 여부
          carOnlyNetwork: options.carOnlyNetwork, // 자동차 전용 네트워크
          decal: options.decal, // 배경 이미지 사용 여부

          // osmBuild.py 옵션들
          removeGeometry: true, // --geometry.remove
          guessRoundabouts: true, // --roundabouts.guess
          guessRamps: true, // --ramps.guess
          joinJunctions: true, // --junctions.join
          guessSignals: true, // --tls.guess-signals
          discardSimple: true, // --tls.discard-simple
          joinTLS: true, // --tls.join
          cornerDetail: 5, // --junctions.corner-detail

          // randomTrips.py 관련 옵션
          minDistance: options.minDistance, // 최소 통행 거리
          maxDistance: options.maxDistance, // 최대 통행 거리
          departPos: options.randomDepartPos, // 무작위 출발 위치
          arrivalPos: options.randomArrivalPos, // 무작위 도착 위치
          fringe: options.allowFringe, // 외곽 지역 통행 허용
          validate: true, // 경로 유효성 검사
        },

        // 도로 타입 설정
        // osmWebWizard.py에서 사용하는 도로 타입 카테고리와
        // 각 카테고리에 포함된 세부 타입들
        roadTypes: {
          Highway: roadTypes.Highway, // 고속도로, 간선도로 등
          Pedestrians: roadTypes.Pedestrians, // 보행자도로, 인도 등
          Railway: roadTypes.Railway, // 철도, 지하철 등
          Aeroway: roadTypes.Aeroway, // 공항 관련 도로
          Waterway: roadTypes.Waterway, // 수로
          Aerialway: roadTypes.Aerialway, // 케이블카 등
          Route: roadTypes.Route, // 페리 등
        },
      };

      // API 호출
      const response = await fetch("/api/scenario/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // 진행 상태 표시 및 업데이트
      setStatus("Generating network...");
      setProgress(40);

      const data = await response.json();

      setStatus("Generating routes...");
      setProgress(80);

      setStatus(data.message);
      setProgress(100);
    } catch (error) {
      console.error("Error:", error);
      setStatus("Error generating scenario");
    } finally {
      setTimeout(() => {
        setIsGenerating(false);
        setProgress(0);
      }, 2000);
    }
  };

  return (
    <div className="fixed top-4 right-4">
      <button
        onClick={handleGenerate}
        disabled={isGenerating}
        className={`bg-green-500 text-white px-4 py-2 rounded ${
          isGenerating ? "opacity-50 cursor-not-allowed" : "hover:bg-green-600"
        }`}
      >
        Generate Scenario
      </button>
      {isGenerating && (
        <div className="mt-2 w-64 bg-gray-200 rounded">
          <div
            className="bg-blue-500 text-xs leading-none py-1 text-center text-white rounded"
            style={{ width: `${progress}%` }}
          >
            {status}
          </div>
        </div>
      )}
      {!isGenerating && status && (
        <div className="mt-2 w-64 bg-gray-200 rounded p-2">{status}</div>
      )}
    </div>
  );
};

export default GenerateButton;