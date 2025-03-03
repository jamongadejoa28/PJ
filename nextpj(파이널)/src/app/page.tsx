// src/app/page.tsx

"use client";

import { useEffect, useState } from "react";
import { Map, MapMarker } from "react-kakao-maps-sdk";
import MapTypeControl from "./components/MapTypeControl";
import SpeedChart from "./components/SpeedChart";
import VehicleSettings from "./components/VehicleSettings";
import {
  RoadCategory,
  VehicleType,
  ScenarioOptions
} from "@/types/simulation";
import VehicleMarkers from "./components/VehicleMarkers";
import RoadTypes from "./components/RoadTypes";
import Copyright from "./components/Copyright";

declare global {
  interface Window {
    kakao: any;
  }
}

type VehicleClass = {
  display: string;
  internal: VehicleType;
  fringeFactor: number;
  count: number;
  enabled: boolean;
};

type RoadTypeState = {
  allTypes: string[];
  enabledTypes: string[];
};

type RoadTypeStateMap = Record<RoadCategory, RoadTypeState>;

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
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const [map, setMap] = useState<any>(null);
  const [mapInstance, setMapInstance] = useState<any>(null);
  const [marker, setMarker] = useState<any>(null);
  const [duration, setDuration] = useState(3600);
  const [radius, setRadius] = useState(1000);
  const [simulationStatus, setSimulationStatus] = useState({
    isRunning: false,
    error: false,
  });
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

  const enabledRoadTypes = Object.fromEntries(
    Object.entries(roadTypeStates).map(([category, state]) => [
      category,
      state.enabledTypes,
    ])
  ) as Record<RoadCategory, string[]>;

  // Map initialization
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

              if (marker) {
                marker.setMap(null);
              }
              const newMarker = new window.kakao.maps.Marker({
                position: latlng,
              });
              newMarker.setMap(mapInstance);
              setMarker(newMarker);

              setCoordinates({
                lat: latlng.getLat(),
                lng: latlng.getLng(),
              });

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

  const handleSearch = () => {
    const geocoder = new window.kakao.maps.services.Geocoder();

    geocoder.addressSearch(address, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK) {
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

  const handleVehicleChange = (
    internal: VehicleType,
    data: VehicleSettings
  ) => {
    setVehicles((prev) =>
      prev.map((vehicle) =>
        vehicle.internal === internal ? { ...vehicle, ...data } : vehicle
      )
    );
  };

  const handleRoadTypeChange = (
    category: RoadCategory,
    enabledTypes: string[]
  ) => {
    setRoadTypeStates((prev) => ({
      ...prev,
      [category]: {
        ...prev[category],
        enabledTypes,
      },
    }));
  };

  const handleGenerateScenario = async () => {
    // 초기 상태 설정 - 사용자에게 시작을 알립니다
    setStatus({
      visible: true,
      text: "Starting scenario generation...",
      progress: 0,
    });

    try {
      // API 요청 데이터 준비 및 전송
      const response = await fetch(
        "http://localhost:8000/api/scenario/generate",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            // 지도 관련 설정
            coordinates,
            radius,
            duration,
            // 차량 설정을 객체로 변환
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
            // 도로 타입 설정을 객체로 변환
            roadTypes: Object.fromEntries(
              Object.entries(roadTypeStates).map(([category, state]) => [
                category,
                state.enabledTypes,
              ])
            ),
            options,
          }),
        }
      );

      // 응답 상태 확인
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // 스트림 리더 및 디코더 설정
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      // 불완전한 메시지를 저장할 버퍼
      let buffer = "";

      if (!reader) {
        throw new Error("Failed to read response");
      }

      // 스트림 데이터 처리 루프
      while (true) {
        // 청크 단위로 데이터 읽기
        const { done, value } = await reader.read();

        // 스트림이 끝나면 루프 종료
        if (done) {
          break;
        }

        // 바이너리 데이터를 텍스트로 변환하여 버퍼에 추가
        buffer += decoder.decode(value, { stream: true });

        // 버퍼의 내용을 줄 단위로 분리
        const lines = buffer.split("\n");

        // 마지막 줄은 다음 청크와 연결될 수 있으므로 버퍼에 보관
        buffer = lines.pop() || "";

        // 완성된 각 메시지 처리
        for (const line of lines) {
          // SSE 프로토콜의 구분자인 빈 줄은 무시
          if (line.trim() === "") continue;

          try {
            // JSON 메시지 파싱 및 상태 업데이트
            const data = JSON.parse(line);
            setStatus({
              visible: true,
              text: data.message,
              progress: data.progress,
            });
          } catch (e) {
            // JSON 파싱 실패 시 경고 로그
            console.warn("Failed to parse SSE message:", line);
          }
        }
      }
    } catch (error) {
      // 오류 발생 시 처리
      console.error("Scenario generation error:", error);
      setStatus({
        visible: true,
        text: "Error generating scenario",
        progress: 0,
      });
    } finally {
      // 작업 완료 후 상태 표시 정리
      // 2초 후에 상태 표시를 숨김
      setTimeout(() => {
        setStatus({
          visible: false,
          text: "",
          progress: 0,
        });
      }, 2000);
    }
  };

  const handleStartSimulation = () => {
    // 시뮬레이션 시작 시 상태 초기화
    setSimulationError(false);
    setSimulationActive(true);
  };

  const handleSimulationError = () => {
    setSimulationError(true);
    setSimulationActive(false);
    alert("시뮬레이션 실행 중 오류가 발생했습니다.");
  };

  const handleSimulationComplete = () => {
    setSimulationActive(false);
    alert("시뮬레이션이 성공적으로 완료되었습니다.");
  };

  return (
    <main className="w-full h-screen relative">
      <Map
        center={{ lat: coordinates.lat, lng: coordinates.lng }}
        style={{ width: "100%", height: "100%" }}
        level={3}
        onCreate={setMapInstance}
        onClick={(_, mouseEvent) => {
          const latlng = mouseEvent.latLng;
          setCoordinates({
            lat: latlng.getLat(),
            lng: latlng.getLng(),
          });

          const geocoder = new window.kakao.maps.services.Geocoder();
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
        {mapInstance && (
          <>
            <MapTypeControl map={mapInstance} />
            <VehicleMarkers
              map={mapInstance}
              simulationActive={simulationActive}
              duration={duration}
              onError={handleSimulationError}
              onComplete={handleSimulationComplete}
            />
          </>
        )}
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
          <SpeedChart simulationActive={simulationActive} />
        </div>

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

        <div className={`controls ${activeTab === 2 ? "open" : ""}`}>
          <RoadTypes
            initialEnabled={enabledRoadTypes}
            onChange={handleRoadTypeChange}
          />
        </div>

        <div className={`controls ${activeTab === 3 ? "open" : ""}`}>
          <Copyright />
        </div>

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
