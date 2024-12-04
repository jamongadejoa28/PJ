// src/app/context/MapContext.tsx
"use client";

import React, { createContext, useContext, useRef, ReactNode } from "react";
import MapComponent, { MapRef } from "../components/MapComponent";

type MapContextType = {
  setView: (lon: number, lat: number) => void;
};

const MapContext = createContext<MapContextType | undefined>(undefined);

export const MapProvider = ({ children }: { children: ReactNode }) => {
  const mapRef = useRef<MapRef>(null);

  const setView = (lon: number, lat: number) => {
    if (mapRef.current) {
      mapRef.current.setView(lon, lat);
    }
  };

  return (
    <MapContext.Provider value={{ setView }}>
      <MapComponent ref={mapRef} />
      {children}
    </MapContext.Provider>
  );
};

export const useMap = () => {
  const context = useContext(MapContext);
  if (!context) {
    throw new Error("useMap must be used within a MapProvider");
  }
  return context;
};