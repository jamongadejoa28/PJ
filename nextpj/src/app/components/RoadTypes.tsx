// src/app/components/RoadTypes.tsx
"use client";

import { useState, useEffect } from "react";

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

interface RoadTypeProps {
  category: RoadCategory;
  typeList: RoadType[]; // 모든 가능한 타입
  enabledTypes: RoadType[]; // 현재 활성화된 타입
  onChange?: (category: RoadCategory, enabledTypes: RoadType[]) => void;
}

export default function RoadTypes({
  category,
  typeList,
  enabledTypes,
  onChange,
}: RoadTypeProps) {
  const handleCategoryChange = (checked: boolean) => {
    if (onChange) {
      onChange(category, checked ? typeList : []);
    }
  };

  const handleTypeChange = (type: RoadType, checked: boolean) => {
    if (onChange) {
      const newEnabledTypes = checked
        ? [...enabledTypes, type]
        : enabledTypes.filter((t) => t !== type);
      onChange(category, newEnabledTypes);
    }
  };

  const isAllChecked = enabledTypes.length === typeList.length;

  return (
    <div className="container">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-lg font-medium">{category}</h4>
        <input
          type="checkbox"
          checked={isAllChecked}
          className="checkAll ml-2"
          id={category.toLowerCase()}
          onChange={(e) => handleCategoryChange(e.target.checked)}
        />
      </div>

      <div className={`roadTypes ${category.toLowerCase()} space-y-1`}>
        {typeList.map((type) => (
          <label key={type} className="road-type-label flex items-center">
            <input
              type="checkbox"
              checked={enabledTypes.includes(type)}
              id={`${category}_${type}`}
              className="mr-2"
              onChange={(e) => handleTypeChange(type, e.target.checked)}
            />
            {type}
          </label>
        ))}
      </div>
    </div>
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
