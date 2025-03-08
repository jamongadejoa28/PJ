// src/components/map/VWorldMap.tsx
'use client';

import { useState, useEffect, ReactNode } from 'react';
import MapContext from '@/contexts/MapContext';

interface VWorldMapProps {
  children: ReactNode;
}

export default function VWorldMap({ children }: VWorldMapProps) {
  const [mapObj, setMapObj] = useState<{ map: vw.Map } | null>(null);

  useEffect(() => {
    // 지도 초기화 함수
    const initializeMap = () => {
      try {
        // vw 객체가 존재하는지 확인
        if (typeof vw === 'undefined') {
          console.log('VWorld map object not loaded yet, retrying...');
          return;
        }

        console.log('Initializing VWorld map...');
        
        // 지도 옵션 설정
        const options = {
          mapId: "vmap",
          initPosition: new vw.CameraPosition(
            new vw.CoordZ(127.425, 38.196, 1548700),
            new vw.Direction(0, -90, 0)
          ),
          logo: true,
          navigation: true,
          renderMode: 'auto'
        };

        // 지도 객체 생성 및 초기화
        const vworldMap = new vw.Map();
        vworldMap.setOption(options);
        vworldMap.start();
        
        setMapObj({ map: vworldMap });
        console.log('VWorld map initialized successfully');
      } catch (error) {
        console.error('Failed to initialize VWorld map:', error);
      }
    };

    // 일정 간격으로 초기화 시도
    const initInterval = setInterval(() => {
      if (typeof vw !== 'undefined') {
        initializeMap();
        clearInterval(initInterval);
      }
    }, 500);

    return () => {
      clearInterval(initInterval);
      if (mapObj?.map) {
        console.log('Cleaning up VWorld map');
        mapObj.map.destroy();
      }
    };
  }, []);

  return (
    <MapContext.Provider value={{ mapObj, setMapObj }}>
      <div className="relative w-full h-screen">
        <div 
          id="vmap" 
          className="absolute inset-0 w-full h-full bg-gray-100"
        />
        {children}
      </div>
    </MapContext.Provider>
  );
}