'use client';

import { createContext, useContext, ReactNode } from 'react';

interface MapContextType {
  mapObj: { map: vw.Map } | null;
  setMapObj: (map: { map: vw.Map } | null) => void;
}

const MapContext = createContext<MapContextType>({
  mapObj: null,
  setMapObj: () => {},
});

export const useMapContext = () => useContext(MapContext);
export default MapContext;