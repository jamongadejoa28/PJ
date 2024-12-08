// src/app/contexts/MapContext.tsx
"use client";

import { createContext, useContext } from 'react';

interface MapContextType {
  setView: (lon: number, lat: number) => void;
}

const MapContext = createContext<MapContextType | undefined>(undefined);

export function useMap() {
  const context = useContext(MapContext);
  if (!context) {
    throw new Error('useMap must be used within a MapProvider');
  }
  return context;
}

interface MapProviderProps {
  children: React.ReactNode;
  value: MapContextType;
}

export function MapProvider({ children, value }: MapProviderProps) {
  return (
    <MapContext.Provider value={value}>
      {children}
    </MapContext.Provider>
  );
}