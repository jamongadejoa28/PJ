// src/app/page.tsx

"use client";

import { useEffect, useState, useCallback } from "react";
import { Map } from "react-kakao-maps-sdk";
import {
  RoadCategory,
  VehicleType,
  VehicleSettings as VehicleSettingsType,
  ScenarioOptions,
} from "@/types/simulation";
import VehicleSettings from "./components/VehicleSettings";
import RoadTypes from "./components/RoadTypes";
import Copyright from "./components/Copyright";
import VehicleMarkers from "./components/VehicleMarkers";

// 전역 타입 선언
declare global {
  interface Window {
    kakao: any;
  }
}

// 차량 클래스 타입 정의
type VehicleClass = {
  display: string;
  internal: VehicleType;
  fringeFactor: number;
  count: number;
  enabled: boolean;
};

// 도로 타입 상태 정의
type RoadTypeState = {
  allTypes: string[];
  enabledTypes: string[];
};

type RoadTypeStateMap = Record<RoadCategory, RoadTypeState>;

// 차량 클래스 초기 데이터
const vehicleClasses: VehicleClass[] = [
  {
    display: "Cars",
    internal: VehicleType.Passenger,
    fringeFactor: 5,
    count: 12,
    enabled: true,
  },
  {
    display: "Trucks",
    internal: VehicleType.Truck,
    fringeFactor: 5,
    count: 8,
    enabled: false,
  },
  {
    display: "Bus",
    internal: VehicleType.Bus,
    fringeFactor: 5,
    count: 4,
    enabled: false,
  },
  {
    display: "Motorcycles",
    internal: VehicleType.Motorcycle,
    fringeFactor: 2,
    count: 4,
    enabled: false,
  },
  {
    display: "Bicycles",
    internal: VehicleType.Bicycle,
    fringeFactor: 2,
    count: 6,
    enabled: false,
  },
  {
    display: "Pedestrians",
    internal: VehicleType.Pedestrian,
    fringeFactor: 1,
    count: 10,
    enabled: false,
  },
  {
    display: "Trams",
    internal: VehicleType.Tram,
    fringeFactor: 20,
    count: 2,
    enabled: false,
  },
  {
    display: "Urban trains",
    internal: VehicleType.RailUrban,
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
  {
    display: "Trains",
    internal: VehicleType.Rail,
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
  {
    display: "Ships",
    internal: VehicleType.Ship,
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
];

// 도로 카테고리 정의
const roadCategories: Record<RoadCategory, string[]> = {
  [RoadCategory.Highway]: [
    "motorway",
    "trunk",
    "primary",
    "secondary",
    "tertiary",
    "unclassified",
    "residential",
    "living_street",
    "unsurfaced",
    "service",
    "raceway",
    "bus_guideway",
  ],
  [RoadCategory.Pedestrians]: [
    "track",
    "footway",
    "pedestrian",
    "path",
    "bridleway",
    "cycleway",
    "step",
    "steps",
    "stairs",
  ],
  [RoadCategory.Railway]: [
    "preserved",
    "tram",
    "subway",
    "light_rail",
    "rail",
    "highspeed",
    "monorail",
  ],
  [RoadCategory.Aeroway]: [
    "stopway",
    "parking_position",
    "taxiway",
    "taxilane",
    "runway",
    "highway_strip",
  ],
  [RoadCategory.Waterway]: ["river", "canal"],
  [RoadCategory.Aerialway]: ["cable_car", "gondola"],
  [RoadCategory.Route]: ["ferry"],
};

export default function MainPage() {
  // 상태 관리
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const [mapInstance, setMapInstance] = useState<any>(null);
  const [duration, setDuration] = useState(3600);
  const [radius, setRadius] = useState(1000);
  const [mapType, setMapType] = useState<any>(null);
  const [options, setOptions] = useState<ScenarioOptions>({
    polygons: true,
    publicTransport: false,
    carOnlyNetwork: false,
    decal: false,
    leftHand: false,
  });
  const [coordinates, setCoordinates] = useState({
    lat: 37.5665,
    lng: 126.978,
  });
  const [address, setAddress] = useState("서울시청");
  const [vehicles, setVehicles] = useState<VehicleClass[]>(vehicleClasses);
  const [status, setStatus] = useState({
    visible: false,
    text: "",
    progress: 0,
  });
  const [simulationActive, setSimulationActive] = useState(false);
  const [simulationError, setSimulationError] = useState(false);

  useEffect(() => {
    if (window.kakao?.maps) {
      setMapType(window.kakao.maps.MapTypeId.HYBRID);
    }
  }, []);

  // 도로 타입 상태 초기화
  const initialRoadTypeState: RoadTypeStateMap = Object.entries(
    roadCategories
  ).reduce(
    (acc, [category, types]) => ({
      ...acc,
      [category]: {
        allTypes: [...types],
        enabledTypes: [],
      },
    }),
    {} as RoadTypeStateMap
  );

  const [roadTypeStates, setRoadTypeStates] =
    useState<RoadTypeStateMap>(initialRoadTypeState);

  // 활성화된 도로 타입 가져오기
  const enabledRoadTypes = Object.fromEntries(
    Object.entries(roadTypeStates).map(([category, state]) => [
      category,
      state.enabledTypes,
    ])
  ) as Record<RoadCategory, string[]>;

  // 지도 생성 핸들러
  const handleMapCreate = useCallback((map: any) => {
    setMapInstance(map);

    // 지도 클릭 이벤트 리스너 등록
    window.kakao.maps.event.addListener(map, "click", (mouseEvent: any) => {
      const latlng = mouseEvent.latLng;
      setCoordinates({
        lat: latlng.getLat(),
        lng: latlng.getLng(),
      });

      // 클릭한 위치의 주소 정보 가져오기
      const geocoder = new window.kakao.maps.services.Geocoder();
      geocoder.coord2Address(
        latlng.getLng(),
        latlng.getLat(),
        (result: any, status: any) => {
          if (status === window.kakao.maps.services.Status.OK && result[0]) {
            setAddress(result[0].address.address_name);
          }
        }
      );
    });
  }, []);

  // 주소 검색 핸들러
  const handleSearch = useCallback(() => {
    if (!mapInstance) return;

    const geocoder = new window.kakao.maps.services.Geocoder();
    geocoder.addressSearch(address, (result: any, status: any) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const coords = {
          lat: parseFloat(result[0].y),
          lng: parseFloat(result[0].x),
        };
        setCoordinates(coords);
        mapInstance.setCenter(
          new window.kakao.maps.LatLng(coords.lat, coords.lng)
        );
      } else {
        alert("주소 검색에 실패했습니다.");
      }
    });
  }, [address, mapInstance]);

  // 차량 설정 변경 핸들러
  const handleVehicleChange = useCallback(
    (internal: VehicleType, data: VehicleSettingsType) => {
      setVehicles((prev) =>
        prev.map((vehicle) =>
          vehicle.internal === internal ? { ...vehicle, ...data } : vehicle
        )
      );
    },
    []
  );

  // 도로 타입 변경 핸들러
  const handleRoadTypeChange = useCallback(
    (category: RoadCategory, enabledTypes: string[]) => {
      setRoadTypeStates((prev) => ({
        ...prev,
        [category]: {
          ...prev[category],
          enabledTypes,
        },
      }));
    },
    []
  );

  // 시나리오 생성 핸들러
  const handleGenerateScenario = async () => {
    setStatus({
      visible: true,
      text: "시나리오 생성을 시작합니다...",
      progress: 0,
    });

    try {
      const response = await fetch(
        "http://localhost:8000/api/scenario/generate",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            coordinates,
            radius,
            duration,
            vehicles: vehicles.reduce(
              (acc, v) => ({
                ...acc,
                [v.internal]: {
                  count: v.count,
                  fringeFactor: v.fringeFactor,
                  enabled: v.enabled,
                },
              }),
              {}
            ),
            roadTypes: enabledRoadTypes,
            options,
          }),
        }
      );

      const reader = response.body?.getReader();
      if (!reader) throw new Error("응답을 읽을 수 없습니다");

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const decodedValue = new TextDecoder().decode(value);
        const data = JSON.parse(decodedValue);

        setStatus({
          visible: true,
          text: data.message,
          progress: data.progress,
        });
      }
    } catch (error) {
      setStatus({
        visible: true,
        text: "오류가 발생했습니다",
        progress: 0,
      });
    }

    setTimeout(() => {
      setStatus({ visible: false, text: "", progress: 0 });
    }, 2000);
  };

  // 시뮬레이션 제어 핸들러
  const handleStartSimulation = useCallback(() => {
    setSimulationError(false);
    setSimulationActive(true);
  }, []);

  const handleSimulationError = useCallback(() => {
    setSimulationError(true);
    setSimulationActive(false);
    alert("시뮬레이션 실행 중 오류가 발생했습니다.");
  }, []);

  const handleSimulationComplete = useCallback(() => {
    setSimulationActive(false);
    alert("시뮬레이션이 성공적으로 완료되었습니다.");
  }, []);

  return (
    <main className="w-full h-screen relative">
      {/* 지도 컴포넌트 */}
      <Map
        center={{ lat: coordinates.lat, lng: coordinates.lng }}
        style={{ width: "100%", height: "100%" }}
        level={3}
        onCreate={handleMapCreate}
        mapTypeId={mapType} // 상태값으로 맵 타입 설정
      >
        {mapInstance && (
          <VehicleMarkers
            map={mapInstance}
            simulationActive={simulationActive}
            duration={duration}
            onError={handleSimulationError}
            onComplete={handleSimulationComplete}
          />
        )}
      </Map>

      {/* 사이드 패널 */}
      <div className={`side ${activeTab !== null ? "open" : ""}`}>
        <div className="flex flex-col">
          {[
            { title: "Options", icon: "generate" },
            { title: "Vehicles", icon: "passenger" },
            { title: "Road-Types", icon: "road" },
            { title: "Copyright", icon: null },
          ].map((item, i) => (
            <div
              key={i}
              className={`tab ${activeTab === i ? "open" : ""}`}
              onClick={() => setActiveTab(activeTab === i ? null : i)}
              title={item.title}
            >
              {!item.icon ? (
                "©"
              ) : (
                <img
                  src={`/images/${item.icon}.png`}
                  alt={item.title}
                  className="w-8 m-1"
                />
              )}
            </div>
          ))}
        </div>

        {/* 옵션 컨트롤 패널 */}
        <div className={`controls ${activeTab === 0 ? "open" : ""}`}>
          <div className="container">
            <h4 className="section-title mb-2">Position</h4>
            <div className="space-y-2">
              <div className="flex items-center gap-1">
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  onKeyPress={(e) => {
                    if (e.key === "Enter") {
                      handleSearch();
                    }
                  }}
                  className="input-field flex-1"
                />
                <button onClick={handleSearch} className="button-secondary">
                  Search
                </button>
              </div>
              <input
                type="text"
                value={`${coordinates.lat.toFixed(
                  6
                )}, ${coordinates.lng.toFixed(6)}`}
                readOnly
                className="input-field w-full"
              />
            </div>
          </div>

          <div className="container mt-5">
            <h4 className="section-title mb-2">Options</h4>
            <div className="space-y-2">
              <label className="option-label">
                Duration (s)
                <input
                  type="number"
                  value={duration}
                  onChange={(e) => setDuration(parseInt(e.target.value))}
                  min={0}
                  className="number-input"
                />
              </label>
              <label className="option-label">
                Radius (m)
                <input
                  type="number"
                  value={radius}
                  onChange={(e) =>
                    setRadius(
                      Math.min(3000, Math.max(0, parseInt(e.target.value)))
                    )
                  }
                  min={0}
                  max={3000}
                  className="number-input"
                />
              </label>
              {/* 추가 옵션들 체크박스로 표시 */}
              {Object.entries(options).map(([key, value]) => (
                <label key={key} className="option-label">
                  {key
                    .replace(/([A-Z])/g, " $1")
                    .replace(/^./, (str) => str.toUpperCase())}
                  <input
                    type="checkbox"
                    checked={value}
                    onChange={(e) =>
                      setOptions({ ...options, [key]: e.target.checked })
                    }
                    className="checkbox"
                  />
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* 차량 설정 패널 */}
        <div className={`controls ${activeTab === 1 ? "open" : ""}`}>
          {vehicles.map((vehicle) => (
            <VehicleSettings
              key={vehicle.internal}
              display={vehicle.display}
              internal={vehicle.internal}
              defaultFringeFactor={vehicle.fringeFactor}
              defaultCount={vehicle.count}
              defaultEnabled={vehicle.enabled}
              onChange={(data) => handleVehicleChange(vehicle.internal, data)}
            />
          ))}
        </div>

        {/* 도로 타입 설정 패널 */}
        <div className={`controls ${activeTab === 2 ? "open" : ""}`}>
          <RoadTypes
            initialEnabled={enabledRoadTypes}
            onChange={handleRoadTypeChange}
          />
        </div>

        {/* 저작권 정보 패널 */}
        <div className={`controls ${activeTab === 3 ? "open" : ""}`}>
          <Copyright />
        </div>

        {/* 시나리오 생성 및 시뮬레이션 제어 버튼 */}
        <div className="absolute top-[10px] right-[10px] w-[calc(100%-20px)]">
          {!status.visible ? (
            <div className="space-y-1">
              <button
                onClick={handleGenerateScenario}
                className="scenario-button"
              >
                Generate Scenario
              </button>
              <button
                onClick={handleStartSimulation}
                disabled={simulationActive}
                className={`scenario-button ${
                  simulationActive ? "opacity-50 cursor-not-allowed" : ""
                }`}
              >
                {simulationError
                  ? "Error"
                  : simulationActive
                  ? "Simulation in Progress..."
                  : "Start Simulation"}
              </button>
            </div>
          ) : (
            <div className="status-bar">
              <span>{status.text}</span>
              <div
                className="status-progress"
                style={{ width: `${status.progress}%` }}
              />
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
