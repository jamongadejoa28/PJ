"use client";

import React, { createContext, useContext, useRef, useState, ReactNode } from "react";
import MapComponent, { MapHandle } from "../components/MapComponent";

interface Coordinates {
  lat: number;
  lng: number;
}

interface MapContextType {
  coordinates: Coordinates;
  setCoordinates: (lat: number, lng: number) => void;
  setView: (lat: number, lng: number) => void;
}

const MapContext = createContext<MapContextType | undefined>(undefined);

export const MapProvider = ({ children }: { children: ReactNode }) => {
  const [coordinates, setCoords] = useState<Coordinates>({ lat: 37.5665, lng: 126.9780 });
  const mapRef = useRef<MapHandle>(null);

  const setCoordinates = (lat: number, lng: number) => {
    setCoords({ lat, lng });
  };

  const setView = (lat: number, lng: number) => {
    if (mapRef.current) {
      mapRef.current.setCenter(lat, lng);
      setCoordinates(lat, lng);
    }
  };

  return (
    <MapContext.Provider value={{ coordinates, setCoordinates, setView }}>
      <MapComponent 
        ref={mapRef}
        defaultCenter={coordinates} 
        onCoordinateSelect={setCoordinates}
      />
      {children}
    </MapContext.Provider>
  );
};

export const useMap = () => {
  const context = useContext(MapContext);
  if (!context) {
    throw new Error("useMap must be used within MapProvider");
  }
  return context;
};
