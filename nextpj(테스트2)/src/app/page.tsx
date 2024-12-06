"use client";

import { useState } from "react";
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

interface RoadTypeState {
  allTypes: RoadType[];
  enabledTypes: RoadType[];
}

type RoadTypeStateMap = {
  [K in RoadCategory]: RoadTypeState;
};

type Options = {
  polygons: boolean;
  publicTransport: boolean;
  carOnlyNetwork: boolean;
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

export default function Home() {
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const [coordinates, setCoordinates] = useState({ lat: 37.5665, lng: 126.9780 });
  const [radius, setRadius] = useState(1000);
  const [duration, setDuration] = useState(3600);
  const [vehicles, setVehicles] = useState<VehicleClass[]>(vehicleClasses);
  const [roadTypeStates, setRoadTypeStates] = useState(initialRoadTypeState);
  const [options, setOptions] = useState<Options>({
    polygons: true,
    publicTransport: false,
    carOnlyNetwork: false,
    leftHand: false,
  });
  const [status, setStatus] = useState({
    isGenerating: false,
    progress: 0,
    message: "",
  });

  const handleMapClick = (mouseEvent: kakao.maps.MouseEvent) => {
    setCoordinates({
      lat: mouseEvent.latLng.getLat(),
      lng: mouseEvent.latLng.getLng(),
    });
  };

  const handleVehicleChange = (internal: VehicleType, data: VehicleData) => {
    setVehicles((prev) =>
      prev.map((vehicle) =>
        vehicle.internal === internal ? { ...vehicle, ...data } : vehicle
      )
    );
  };

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

  const handleGenerateScenario = async () => {
    setStatus({ isGenerating: true, progress: 0, message: "Starting..." });
    
    try {
      const response = await fetch("/api/scenario/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          coordinates,
          radius,
          duration,
          options,
          vehicles: vehicles.filter(v => v.enabled),
          roadTypes: Object.fromEntries(
            Object.entries(roadTypeStates).map(([category, state]) => [
              category,
              state.enabledTypes,
            ])
          ),
        }),
      });

      const reader = response.body?.getReader();
      if (!reader) throw new Error("No reader available");

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const text = new TextDecoder().decode(value);
        const data = JSON.parse(text);
        
        setStatus({
          isGenerating: true,
          progress: data.progress,
          message: data.message,
        });
      }

      setTimeout(() => {
        setStatus({ isGenerating: false, progress: 0, message: "" });
      }, 2000);

    } catch (error) {
      setStatus({
        isGenerating: false,
        progress: 0,
        message: "Generation failed",
      });
    }
  };

  const handleSimulationStart = async () => {
    try {
      await fetch("/api/scenario/simulate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ duration }),
      });
    } catch (error) {
      console.error("Simulation failed");
    }
  };

  return (
    <main className="w-full h-screen relative">
      <Map
        center={{ lat: coordinates.lat, lng: coordinates.lng }}
        style={{ width: "100%", height: "100%" }}
        level={3}
        onClick={handleMapClick}
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

        <div className={`controls ${activeTab === 0 ? "open" : ""}`}>
          <div className="container">
            <h4 className="section-title mb-2">Network Settings</h4>
            <div className="space-y-2">
              <label className="option-label">
                Duration
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
                    setRadius(Math.min(3000, Math.max(0, parseInt(e.target.value))))
                  }
                  min={0}
                  max={3000}
                  step={100}
                  className="number-input"
                />
              </label>

              {Object.entries(options).map(([key, value]) => (
                <label key={key} className="option-label">
                  {key.replace(/([A-Z])/g, " $1").replace(/^./, str => str.toUpperCase())}
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

        <div className={`controls ${activeTab === 3 ? "open" : ""}`}>
          <Copyright />
        </div>

        <div className="fixed-controls">
          {status.isGenerating ? (
            <div className="status-bar">
              <span>{status.message}</span>
              <div
                className="progress-bar"
                style={{ width: `${status.progress}%` }}
              />
            </div>
          ) : (
            <>
              <button onClick={handleGenerateScenario} className="export-button">
                Generate Scenario
              </button>
              <button onClick={handleSimulationStart} className="export-button mt-2">
                Start Simulation
              </button>
            </>
          )}
        </div>
      </div>
    </main>
  );
}