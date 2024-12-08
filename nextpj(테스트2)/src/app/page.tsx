// src/app/page.tsx
"use client";

import { useEffect, useState } from "react";
import { Map, MapMarker } from "react-kakao-maps-sdk";
import VehicleSettings from "./components/VehicleSettings";
import RoadTypes from "./components/RoadTypes";
import Copyright from "./components/Copyright";
import type { VehicleType, VehicleData } from "./components/VehicleSettings";
import type {
  RoadCategory,
  RoadType,
  HighwayType,
  PedestrianType,
  RailwayType,
  AerowayType,
  WaterwayType,
  AerialwayType,
  RouteType,
} from "./components/RoadTypes";

declare global {
  interface Window {
    kakao: any;
  }
}

interface RoadTypeState {
  allTypes: RoadType[]; // 해당 카테고리의 모든 타입
  enabledTypes: RoadType[]; // 현재 활성화된 타입들
}

type RoadTypeStateMap = {
  [K in RoadCategory]: RoadTypeState;
};

type MouseState = {
  x: number;
  y: number;
  area: string | null;
};

type Options = {
  polygons: boolean;
  publicTransport: boolean;
  carOnlyNetwork: boolean;
  decal: boolean;
  leftHand: boolean;
};

type VehicleClass = {
  display: string;
  internal: VehicleType;
  fringeFactor: number;
  count: number;
  enabled: boolean;
};

type RoadCategoryMap = {
  [K in RoadCategory]: RoadType[];
};

// Constants
const vehicleClasses: VehicleClass[] = [
  {
    display: "Cars",
    internal: "passenger",
    fringeFactor: 5,
    count: 12,
    enabled: true,
  },
  {
    display: "Trucks",
    internal: "truck",
    fringeFactor: 5,
    count: 8,
    enabled: false,
  },
  {
    display: "Bus",
    internal: "bus",
    fringeFactor: 5,
    count: 4,
    enabled: false,
  },
  {
    display: "Motorcycles",
    internal: "motorcycle",
    fringeFactor: 2,
    count: 4,
    enabled: false,
  },
  {
    display: "Bicycles",
    internal: "bicycle",
    fringeFactor: 2,
    count: 6,
    enabled: false,
  },
  {
    display: "Pedestrians",
    internal: "pedestrian",
    fringeFactor: 1,
    count: 10,
    enabled: false,
  },
  {
    display: "Trams",
    internal: "tram",
    fringeFactor: 20,
    count: 2,
    enabled: false,
  },
  {
    display: "Urban trains",
    internal: "rail_urban",
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
  {
    display: "Trains",
    internal: "rail",
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
  {
    display: "Ships",
    internal: "ship",
    fringeFactor: 40,
    count: 2,
    enabled: false,
  },
];

const roadCategories: RoadCategoryMap = {
  Highway: [
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
  ] as HighwayType[],
  Pedestrians: [
    "track",
    "footway",
    "pedestrian",
    "path",
    "bridleway",
    "cycleway",
    "step",
    "steps",
    "stairs",
  ] as PedestrianType[],
  Railway: [
    "preserved",
    "tram",
    "subway",
    "light_rail",
    "rail",
    "highspeed",
    "monorail",
  ] as RailwayType[],
  Aeroway: [
    "stopway",
    "parking_position",
    "taxiway",
    "taxilane",
    "runway",
    "highway_strip",
  ] as AerowayType[],
  Waterway: ["river", "canal"] as WaterwayType[],
  Aerialway: ["cable_car", "gondola"] as AerialwayType[],
  Route: ["ferry"] as RouteType[],
};

export default function MainPage() {
  // States
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const [map, setMap] = useState<any>(null);
  const [marker, setMarker] = useState<any>(null);
  const [duration, setDuration] = useState(3600);
  const [radius, setRadius] = useState(1000);
  const [simulationStatus, setSimulationStatus] = useState({
    isRunning: false,
    error: false,
  });
  const [options, setOptions] = useState<Options>({
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

  // 초기 RoadTypeStateMap 설정
  const initialRoadTypeState: RoadTypeStateMap = Object.entries(
    roadCategories
  ).reduce((acc, [category, types]) => {
    acc[category as RoadCategory] = {
      allTypes: [...types],
      enabledTypes: [], // 초기화 시 빈 배열
    };
    return acc;
  }, {} as RoadTypeStateMap);

  // RoadTypeStateMap 상태 관리
  const [roadTypeStates, setRoadTypeStates] =
    useState<RoadTypeStateMap>(initialRoadTypeState);

  // enabledRoadTypes를 roadTypeStates에서 추출
  const enabledRoadTypes: Record<RoadCategory, RoadType[]> = Object.fromEntries(
    Object.entries(roadTypeStates).map(([category, state]) => [
      category,
      state.enabledTypes,
    ])
  ) as Record<RoadCategory, RoadType[]>;

  // Map 초기화
  useEffect(() => {
    if (document.getElementById("map")) {
      const script = document.createElement("script");
      script.src =
        "//dapi.kakao.com/v2/maps/sdk.js?appkey=YOUR_APP_KEY&libraries=services&autoload=false";
      script.async = true;
      document.head.appendChild(script);
      script.onload = () => {
        window.kakao.maps.load(() => {
          const container = document.getElementById("map");
          const optionsMap = {
            center: new window.kakao.maps.LatLng(
              coordinates.lat,
              coordinates.lng
            ),
            level: 3,
          };
          const mapInstance = new window.kakao.maps.Map(container, optionsMap);
          setMap(mapInstance);

          window.kakao.maps.event.addListener(
            mapInstance,
            "click",
            (mouseEvent: any) => {
              const latlng = mouseEvent.latLng;

              // 마커 처리
              if (marker) {
                marker.setMap(null);
              }
              const newMarker = new window.kakao.maps.Marker({
                position: latlng,
              });
              newMarker.setMap(mapInstance);
              setMarker(newMarker);

              // 좌표 업데이트
              setCoordinates({
                lat: latlng.getLat(),
                lng: latlng.getLng(),
              });

              // 클릭한 위치의 주소 가져오기
              const geocoder = new window.kakao.maps.services.Geocoder();
              geocoder.coord2Address(
                latlng.getLng(),
                latlng.getLat(),
                (result: any, status: any) => {
                  if (status === window.kakao.maps.services.Status.OK) {
                    setAddress(result[0].address.address_name);
                  }
                }
              );
            }
          );
        });
      };

      return () => {
        if (script.parentNode) {
          script.parentNode.removeChild(script);
        }
      };
    }
  }, [marker, coordinates.lat, coordinates.lng]);

  // 주소 검색 핸들러
  const handleSearch = () => {
    const geocoder = new kakao.maps.services.Geocoder();

    geocoder.addressSearch(address, (result, status) => {
      if (status === kakao.maps.services.Status.OK) {
        const coords = {
          lat: parseFloat(result[0].y),
          lng: parseFloat(result[0].x),
        };

        setCoordinates(coords);
        if (map) {
          map.setCenter(new window.kakao.maps.LatLng(coords.lat, coords.lng));
        }
      } else {
        alert("주소 검색에 실패했습니다.");
      }
    });
  };

  // 차량 설정 변경 핸들러
  const handleVehicleChange = (internal: VehicleType, data: VehicleData) => {
    setVehicles((prev) =>
      prev.map((vehicle) =>
        vehicle.internal === internal ? { ...vehicle, ...data } : vehicle
      )
    );
  };

  // 도로 타입 변경 핸들러
  const handleRoadTypeChange = (
    category: RoadCategory,
    enabledTypes: RoadType[]
  ) => {
    setRoadTypeStates((prev) => ({
      ...prev,
      [category]: {
        ...prev[category],
        enabledTypes: enabledTypes,
      },
    }));
  };

  // 시나리오 생성 핸들러
  const handleGenerateScenario = async () => {
    setStatus({
      visible: true,
      text: "Starting scenario generation...",
      progress: 0,
    });

    try {
      const response = await fetch(
        "http://localhost:8000/api/scenario/generate",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            coordinates: coordinates,
            radius: radius,
            duration: duration,
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
            roadTypes: Object.fromEntries(
              Object.entries(roadTypeStates).map(([category, state]) => [
                category,
                state.enabledTypes,
              ])
            ),
            options: options,
          }),
        }
      );

      const reader = response.body?.getReader();
      if (!reader) throw new Error("Failed to read response");

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
    } catch (error: any) {
      setStatus({
        visible: true,
        text: "Error",
        progress: 0,
      });
    }

    // 2초 후 상태 초기화
    setTimeout(() => {
      setStatus({ visible: false, text: "", progress: 0 });
    }, 2000);
  };

  // 시뮬레이션 시작 핸들러
  const handleStartSimulation = async () => {
    setSimulationStatus({ isRunning: true, error: false });

    try {
      const response = await fetch(
        "http://localhost:8000/api/scenario/simulate",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ duration: duration }),
        }
      );

      if (!response.ok) {
        throw new Error("Simulation failed");
      }

      const data = await response.json();
      if (data.status !== "success") {
        throw new Error("Simulation failed");
      }
    } catch (error) {
      setSimulationStatus({ isRunning: false, error: true });
      setTimeout(() => {
        setSimulationStatus({ isRunning: false, error: false });
      }, 2000);
    }
  };

  // 차량 설정을 객체로 변환
  const vehicleSettings = Object.fromEntries(
    vehicles
      .filter((v) => v.enabled)
      .map((v) => [
        v.internal,
        {
          count: parseFloat(v.count.toString()),
          fringeFactor: parseFloat(v.fringeFactor.toString()),
          enabled: true,
        },
      ])
  );

  return (
    <main className="w-full h-screen relative">
      <Map
        center={{ lat: coordinates.lat, lng: coordinates.lng }}
        style={{ width: "100%", height: "100%" }}
        level={3}
        onClick={(_, mouseEvent) => {
          const latlng = mouseEvent.latLng;

          // 좌표 업데이트
          setCoordinates({
            lat: latlng.getLat(),
            lng: latlng.getLng(),
          });

          // 클릭한 위치의 주소 가져오기
          const geocoder = new kakao.maps.services.Geocoder();
          geocoder.coord2Address(
            latlng.getLng(),
            latlng.getLat(),
            (result, status) => {
              if (status === kakao.maps.services.Status.OK && result[0]) {
                setAddress(result[0].address.address_name);
              }
            }
          );
        }}
      >
        <MapMarker position={coordinates} />
      </Map>
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

        {/* 탭 내용 */}
        <div className={`controls ${activeTab === 0 ? "open" : ""}`}>
          {/* Position 섹션 */}
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

          {/* Options 섹션 */}
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

        {/* Vehicles 탭 내용 */}
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

        {/* Road Types 탭 내용 */}
        <div className={`controls ${activeTab === 2 ? "open" : ""}`}>
          {/* RoadTypes를 한 번만 렌더링 */}
          <RoadTypes
            initialEnabled={enabledRoadTypes}
            onChange={handleRoadTypeChange}
          />
        </div>

        {/* Copyright 탭 내용 */}
        <div className={`controls ${activeTab === 3 ? "open" : ""}`}>
          <Copyright />
        </div>

        {/* 고정 하단 섹션 */}
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
                disabled={simulationStatus.isRunning}
                className={`scenario-button ${
                  simulationStatus.isRunning
                    ? "opacity-50 cursor-not-allowed"
                    : ""
                }`}
              >
                {simulationStatus.error
                  ? "Error"
                  : simulationStatus.isRunning
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
