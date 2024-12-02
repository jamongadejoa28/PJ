// src/app/wizard/page.tsx
"use client";

import { useEffect, useRef, useState } from "react";
import Map from "ol/Map";
import View from "ol/View";
import TileLayer from "ol/layer/Tile";
import OSM from "ol/source/OSM";
import { fromLonLat, transform } from "ol/proj";
import { Coordinate } from "ol/coordinate";
import VehicleSettings from "../components/VehicleSettings";
import RoadTypes from "../components/RoadTypes";
import Copyright from "../components/Copyright";
import type { VehicleType, VehicleData } from "../components/VehicleSettings";
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
} from "../components/RoadTypes";

interface RoadTypeState {
  allTypes: RoadType[]; // 해당 카테고리의 모든 타입
  enabledTypes: RoadType[]; // 현재 활성화된 타입들
}

type RoadTypeStateMap = {
  [K in RoadCategory]: RoadTypeState;
};

// Types
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

const initialRoadTypeState: RoadTypeStateMap = Object.entries(
  roadCategories
).reduce((acc, [category, types]) => {
  acc[category as RoadCategory] = {
    allTypes: [...types],
    enabledTypes: [...types],
  };
  return acc;
}, {} as RoadTypeStateMap);

export default function WizardPage() {
  // States
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const [canvasActive, setCanvasActive] = useState(false);
  const [map, setMap] = useState<Map | null>(null);
  const [canvasRect, setCanvasRect] = useState([0.1, 0.1, 0.75, 0.9]);
  const [mouse, setMouse] = useState<MouseState>({ x: 0, y: 0, area: null });
  const [duration, setDuration] = useState(3600);
  const [options, setOptions] = useState<Options>({
    polygons: true,
    publicTransport: false,
    carOnlyNetwork: false,
    decal: false,
    leftHand: false,
  });
  const [coordinates, setCoordinates] = useState<Coordinate>([13.4, 52.52]);
  const [address, setAddress] = useState("Berlin");
  const [vehicles, setVehicles] = useState<VehicleClass[]>(vehicleClasses);
  const [enabledRoadTypes, setEnabledRoadTypes] =
    useState<RoadCategoryMap>(roadCategories);
  const [status, setStatus] = useState({
    visible: false,
    text: "",
    progress: 0,
  });

  // Refs
  const mapRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // Utility functions
  const lequal = (a: number, b: number, c: number): boolean => {
    return a <= b && b <= c;
  };

  // Map initialization
  useEffect(() => {
    if (!mapRef.current) return;

    const initialMap = new Map({
      target: mapRef.current,
      layers: [
        new TileLayer({
          source: new OSM({
            url: "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            attributions: ["© OpenStreetMap contributors"],
          }),
        }),
      ],
      view: new View({
        center: fromLonLat(coordinates),
        zoom: 16,
      }),
      controls: [], // We'll add custom controls later if needed
    });

    // Map change handler
    initialMap.on("moveend", () => {
      const center = initialMap.getView().getCenter();
      if (center) {
        const lonLat = transform(center, "EPSG:3857", "EPSG:4326");
        setCoordinates(lonLat);
      }
    });

    setMap(initialMap);

    return () => {
      initialMap.setTarget(undefined);
    };
  }, []);

  // Location handlers
  const handleSearch = async () => {
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
          address
        )}`
      );
      const data = await response.json();
      if (data && data.length > 0) {
        const { lat, lon } = data[0];
        const newCoords: Coordinate = [parseFloat(lon), parseFloat(lat)];
        setCoordinates(newCoords);
        if (map) {
          map.getView().animate({
            center: fromLonLat(newCoords),
            zoom: 16,
            duration: 1000,
          });
        }
      }
    } catch (error) {
      console.error("Error searching location:", error);
    }
  };

  const handleCurrentLocation = () => {
    if (!navigator.geolocation) {
      console.error("Geolocation is not supported by your browser");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const newCoords: Coordinate = [
          position.coords.longitude,
          position.coords.latitude,
        ];
        setCoordinates(newCoords);
        if (map) {
          map.getView().animate({
            center: fromLonLat(newCoords),
            zoom: 16,
            duration: 1000,
          });
        }
      },
      (error) => {
        console.error("Error getting current location:", error);
      }
    );
  };

  // Canvas handlers
  useEffect(() => {
    if (!canvasRef.current || !canvasActive) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const draw = () => {
      const width = canvas.width;
      const height = canvas.height;

      ctx.clearRect(0, 0, width, height);
      ctx.fillStyle = "#808080";
      ctx.globalAlpha = 0.5;

      ctx.fillRect(0, 0, width, height);

      const x0 = width * canvasRect[0];
      const y0 = height * canvasRect[1];
      const x1 = width * canvasRect[2];
      const y1 = height * canvasRect[3];

      ctx.clearRect(x0, y0, x1 - x0, y1 - y0);
    };

    const resizeCanvas = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      draw();
    };

    window.addEventListener("resize", resizeCanvas);
    resizeCanvas();

    return () => {
      window.removeEventListener("resize", resizeCanvas);
    };
  }, [canvasActive, canvasRect]);

  // Vehicle handlers
  const handleVehicleChange = (internal: VehicleType, data: VehicleData) => {
    setVehicles((prev) =>
      prev.map((vehicle) =>
        vehicle.internal === internal ? { ...vehicle, ...data } : vehicle
      )
    );
  };

  const [roadTypeStates, setRoadTypeStates] =
    useState<RoadTypeStateMap>(initialRoadTypeState);

  // Road type handlers
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

  // Generate scenario handler
  const handleGenerateScenario = async () => {
    setStatus({ visible: true, text: "Generating scenario...", progress: 0 });

    try {
      // 바운딩 박스 계산
      const bbox = canvasActive
        ? [
            coordinates[0] - (canvasRect[2] - canvasRect[0]) * 0.1, // west
            coordinates[1] - (canvasRect[3] - canvasRect[1]) * 0.1, // south
            coordinates[0] + (canvasRect[2] - canvasRect[0]) * 0.1, // east
            coordinates[1] + (canvasRect[3] - canvasRect[1]) * 0.1, // north
          ]
        : [
            coordinates[0] - 0.01, // west
            coordinates[1] - 0.01, // south
            coordinates[0] + 0.01, // east
            coordinates[1] + 0.01, // north
          ];

      // 차량 설정 변환
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

      const requestData = {
        coordinates: bbox,
        duration: parseInt(duration.toString()),
        options: {
          polygons: options.polygons,
          publicTransport: options.publicTransport,
          carOnlyNetwork: options.carOnlyNetwork,
          decal: options.decal,
          leftHand: options.leftHand,
        },
        vehicles: vehicleSettings,
        roadTypes: Object.fromEntries(
          Object.entries(roadTypeStates).map(([category, state]) => [
            category,
            state.enabledTypes,
          ])
        ),
      };

      console.log(
        "Sending request with data:",
        JSON.stringify(requestData, null, 2)
      );

      const response = await fetch(
        "http://localhost:8000/api/scenario/generate",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
          },
          body: JSON.stringify(requestData),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(`Server error: ${data.detail}`);
      }

      setStatus({ visible: true, text: data.message, progress: 100 });
    } catch (error) {
      console.error("Error:", error);
      setStatus({
        visible: true,
        text: `Error: ${error.message}`,
        progress: 0,
      });
    }
  };

  return (
    <main className="w-full h-screen relative">
      <div ref={mapRef} className="map" />

      <canvas
        ref={canvasRef}
        className={`canvas-overlay`}
        style={{
          display: canvasActive ? "block" : "none",
          cursor: (() => {
            if (!canvasRef.current) return "default";
            const canvas = canvasRef.current;
            const rect = canvas.getBoundingClientRect();
            const x = mouse.x;
            const y = mouse.y;
            const width = canvas.width;
            const height = canvas.height;
            const x0 = width * canvasRect[0];
            const y0 = height * canvasRect[1];
            const x1 = width * canvasRect[2];
            const y1 = height * canvasRect[3];

            const tolerance = 20;

            // 가장자리 근처인지 확인
            const isNearTop = Math.abs(y - y0) < tolerance;
            const isNearBottom = Math.abs(y - y1) < tolerance;
            const isNearLeft = Math.abs(x - x0) < tolerance;
            const isNearRight = Math.abs(x - x1) < tolerance;

            // 선택 영역 내부나 가장자리에 있는지 확인
            const isInBounds =
              lequal(x0 - tolerance, x, x1 + tolerance) &&
              lequal(y0 - tolerance, y, y1 + tolerance);

            if (!isInBounds) return "default";

            // 모서리 커서
            if (isNearTop && isNearLeft) return "nw-resize";
            if (isNearTop && isNearRight) return "ne-resize";
            if (isNearBottom && isNearLeft) return "sw-resize";
            if (isNearBottom && isNearRight) return "se-resize";

            // 가장자리 커서
            if (isNearTop) return "ns-resize";
            if (isNearBottom) return "ns-resize";
            if (isNearLeft) return "ew-resize";
            if (isNearRight) return "ew-resize";

            // 내부 커서
            if (lequal(x0, x, x1) && lequal(y0, y, y1)) {
              return mouse.area === "move" ? "move" : "pointer";
            }

            return "default";
          })(),
        }}
        onMouseMove={(e) => {
          if (!canvasRef.current) return;
          const canvas = canvasRef.current;
          const rect = canvas.getBoundingClientRect();
          const x = e.clientX - rect.left;
          const y = e.clientY - rect.top;

          if (mouse.area) {
            const dx = (x - mouse.x) / canvas.width;
            const dy = (y - mouse.y) / canvas.height;
            const newRect = [...canvasRect];

            switch (mouse.area) {
              case "move":
                newRect[0] = Math.max(
                  0,
                  Math.min(1 - (newRect[2] - newRect[0]), newRect[0] + dx)
                );
                newRect[1] = Math.max(
                  0,
                  Math.min(1 - (newRect[3] - newRect[1]), newRect[1] + dy)
                );
                newRect[2] = Math.min(1, newRect[2] + dx);
                newRect[3] = Math.min(1, newRect[3] + dy);
                break;
              case "nw":
                newRect[0] = Math.min(newRect[2], Math.max(0, newRect[0] + dx));
                newRect[1] = Math.min(newRect[3], Math.max(0, newRect[1] + dy));
                break;
              case "ne":
                newRect[2] = Math.max(newRect[0], Math.min(1, newRect[2] + dx));
                newRect[1] = Math.min(newRect[3], Math.max(0, newRect[1] + dy));
                break;
              case "sw":
                newRect[0] = Math.min(newRect[2], Math.max(0, newRect[0] + dx));
                newRect[3] = Math.max(newRect[1], Math.min(1, newRect[3] + dy));
                break;
              case "se":
                newRect[2] = Math.max(newRect[0], Math.min(1, newRect[2] + dx));
                newRect[3] = Math.max(newRect[1], Math.min(1, newRect[3] + dy));
                break;
              case "n":
                newRect[1] = Math.min(newRect[3], Math.max(0, newRect[1] + dy));
                break;
              case "s":
                newRect[3] = Math.max(newRect[1], Math.min(1, newRect[3] + dy));
                break;
              case "w":
                newRect[0] = Math.min(newRect[2], Math.max(0, newRect[0] + dx));
                break;
              case "e":
                newRect[2] = Math.max(newRect[0], Math.min(1, newRect[2] + dx));
                break;
            }
            setCanvasRect(newRect);
          }
          setMouse({ x, y, area: mouse.area });
        }}
        onMouseDown={(e) => {
          if (!canvasRef.current) return;
          const canvas = canvasRef.current;
          const rect = canvas.getBoundingClientRect();
          const x = e.clientX - rect.left;
          const y = e.clientY - rect.top;

          const width = canvas.width;
          const height = canvas.height;
          const x0 = width * canvasRect[0];
          const y0 = height * canvasRect[1];
          const x1 = width * canvasRect[2];
          const y1 = height * canvasRect[3];

          const tolerance = 20;
          const isNearTop = Math.abs(y - y0) < tolerance;
          const isNearBottom = Math.abs(y - y1) < tolerance;
          const isNearLeft = Math.abs(x - x0) < tolerance;
          const isNearRight = Math.abs(x - x1) < tolerance;

          // 선택 영역 내부나 가장자리에 있는지 확인
          const isInBounds =
            lequal(x0 - tolerance, x, x1 + tolerance) &&
            lequal(y0 - tolerance, y, y1 + tolerance);

          if (isInBounds) {
            let area = "";
            if (isNearTop && isNearLeft) area = "nw";
            else if (isNearTop && isNearRight) area = "ne";
            else if (isNearBottom && isNearLeft) area = "sw";
            else if (isNearBottom && isNearRight) area = "se";
            else if (isNearTop) area = "n";
            else if (isNearBottom) area = "s";
            else if (isNearLeft) area = "w";
            else if (isNearRight) area = "e";
            else if (lequal(x0, x, x1) && lequal(y0, y, y1)) area = "move";

            setMouse({ x, y, area });
          }
        }}
        onMouseUp={() => setMouse((prev) => ({ ...prev, area: null }))}
      />

      <div className={`side ${activeTab !== null ? "open" : ""}`}>
        {/* Tabs */}
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

        {/* Options Tab Content */}
        <div className={`controls ${activeTab === 0 ? "open" : ""}`}>
          {/* Position Section */}
          <div className="container">
            <h4 className="section-title mb-2">Position</h4>
            <div className="space-y-2">
              <div className="flex items-center gap-1">
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                  className="input-field flex-1"
                  placeholder="Enter location..."
                />
                <button onClick={handleSearch} className="button-secondary">
                  Search
                </button>
              </div>
              <div className="flex items-center gap-1">
                <input
                  type="text"
                  value={`${coordinates[1].toFixed(6)} ${coordinates[0].toFixed(
                    6
                  )}`}
                  className="input-field flex-1"
                  readOnly
                />
                <button
                  onClick={() => {
                    if (map) {
                      map.getView().animate({
                        center: fromLonLat(coordinates),
                        duration: 1000,
                      });
                    }
                  }}
                  className="button-secondary"
                >
                  Go to
                </button>
              </div>
              <button
                onClick={handleCurrentLocation}
                className="button-secondary w-full"
              >
                Use current location
              </button>
            </div>
          </div>

          {/* Select Area Section */}
          <div className="container">
            <div className="options open">
              <label className="option-label">
                Select Area
                <input
                  type="checkbox"
                  checked={canvasActive}
                  onChange={(e) => setCanvasActive(e.target.checked)}
                  className="checkbox"
                />
              </label>
            </div>
          </div>

          {/* Options Section */}
          <div className="container mt-5">
            <h4 className="section-title mb-2">Options</h4>
            <div className="space-y-2">
              <label className="option-label">
                Duration
                <input
                  type="number"
                  value={duration}
                  onChange={(e) => setDuration(parseInt(e.target.value))}
                  min={0}
                  step={1}
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

        {/* Vehicles Tab Content */}
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

        {/* Road Types Tab Content */}
        <div className={`controls ${activeTab === 2 ? "open" : ""}`}>
          {Object.entries(roadTypeStates).map(([category, state]) => (
            <RoadTypes
              key={category}
              category={category as RoadCategory}
              typeList={state.allTypes}
              enabledTypes={state.enabledTypes}
              onChange={handleRoadTypeChange}
            />
          ))}
        </div>

        {/* Copyright Tab Content */}
        <div className={`controls ${activeTab === 3 ? "open" : ""}`}>
          <Copyright />
        </div>

        {/* Fixed Bottom Section */}
        <div className="absolute top-0 left-0 w-full px-2.5">
          <button className="export-button" onClick={handleGenerateScenario}>
            Generate Scenario
          </button>

          {status.visible && (
            <div className="status-bar">
              <span className="block mb-1">{status.text}</span>
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