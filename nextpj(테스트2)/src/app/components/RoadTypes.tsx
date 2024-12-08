// src/app/components/RoadTypes.tsx
"use client";

import { useState } from "react";

type RoadCategory =
  | "Highway"
  | "Pedestrians"
  | "Railway"
  | "Aeroway"
  | "Waterway"
  | "Aerialway"
  | "Route";

type HighwayType =
  | "motorway"
  | "trunk"
  | "primary"
  | "secondary"
  | "tertiary"
  | "unclassified"
  | "residential"
  | "living_street"
  | "unsurfaced"
  | "service"
  | "raceway"
  | "bus_guideway";

type PedestrianType =
  | "track"
  | "footway"
  | "pedestrian"
  | "path"
  | "bridleway"
  | "cycleway"
  | "step"
  | "steps"
  | "stairs";

type RailwayType =
  | "preserved"
  | "tram"
  | "subway"
  | "light_rail"
  | "rail"
  | "highspeed"
  | "monorail";

type AerowayType =
  | "stopway"
  | "parking_position"
  | "taxiway"
  | "taxilane"
  | "runway"
  | "highway_strip";

type WaterwayType = "river" | "canal";
type AerialwayType = "cable_car" | "gondola";
type RouteType = "ferry";

type RoadType =
  | HighwayType
  | PedestrianType
  | RailwayType
  | AerowayType
  | WaterwayType
  | AerialwayType
  | RouteType;

const roadTypes: Record<RoadCategory, RoadType[]> = {
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
  ],
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
  ],
  Railway: [
    "preserved",
    "tram",
    "subway",
    "light_rail",
    "rail",
    "highspeed",
    "monorail",
  ],
  Aeroway: [
    "stopway",
    "parking_position",
    "taxiway",
    "taxilane",
    "runway",
    "highway_strip",
  ],
  Waterway: ["river", "canal"],
  Aerialway: ["cable_car", "gondola"],
  Route: ["ferry"],
};

interface RoadTypesProps {
  initialEnabled?: Record<RoadCategory, RoadType[]>;
  onChange?: (category: RoadCategory, enabledTypes: RoadType[]) => void;
}

export default function RoadTypes({
  initialEnabled,
  onChange,
}: RoadTypesProps) {
  const [enabledTypes, setEnabledTypes] = useState<
    Record<RoadCategory, RoadType[]>
  >(
    initialEnabled ||
      Object.keys(roadTypes).reduce((acc, category) => {
        acc[category as RoadCategory] = [];
        return acc;
      }, {} as Record<RoadCategory, RoadType[]>)
  );

  const handleCategoryChange = (category: RoadCategory, checked: boolean) => {
    const newEnabledTypes = {
      ...enabledTypes,
      [category]: checked ? [...roadTypes[category]] : [],
    };
    setEnabledTypes(newEnabledTypes);
    onChange?.(category, newEnabledTypes[category]);
  };

  const handleTypeChange = (
    category: RoadCategory,
    type: RoadType,
    checked: boolean
  ) => {
    const currentTypes = enabledTypes[category] || [];
    const newEnabledTypes = {
      ...enabledTypes,
      [category]: checked
        ? [...currentTypes, type]
        : currentTypes.filter((t) => t !== type),
    };
    setEnabledTypes(newEnabledTypes);
    onChange?.(category, newEnabledTypes[category]);
  };

  return (
    <>
      {(Object.keys(roadTypes) as RoadCategory[]).map((category) => {
        const isAllChecked =
          (enabledTypes[category]?.length || 0) === roadTypes[category].length;

        return (
          <div key={category} className="container">
            <div className="flex items-center justify-between mb-2">
              <h4 className="text-lg font-medium">{category}</h4>
              <input
                type="checkbox"
                checked={isAllChecked}
                className="checkAll"
                id={category.toLowerCase()}
                onChange={(e) =>
                  handleCategoryChange(category, e.target.checked)
                }
              />
            </div>
            <div className={`roadTypes ${category.toLowerCase()} space-y-1`}>
              {roadTypes[category].map((type) => (
                <label key={type} className="flex items-center">
                  <input
                    type="checkbox"
                    checked={enabledTypes[category]?.includes(type) || false}
                    id={`${category}_${type}`}
                    className="mr-2"
                    onChange={(e) =>
                      handleTypeChange(category, type, e.target.checked)
                    }
                  />
                  {type}
                </label>
              ))}
            </div>
          </div>
        );
      })}
    </>
  );
}

export type {
  RoadCategory,
  RoadType,
  HighwayType,
  PedestrianType,
  RailwayType,
  AerowayType,
  WaterwayType,
  AerialwayType,
  RouteType,
};
