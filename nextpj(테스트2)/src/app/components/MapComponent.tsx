// src/app/components/MapComponent.tsx
"use client";

import React, { useEffect, useRef, useImperativeHandle, forwardRef } from "react";
import Script from "next/script";

declare global {
  interface Window {
    kakao: any;
  }
}

interface MapComponentProps {
  onCoordinateSelect?: (lat: number, lng: number) => void;
  defaultCenter?: {
    lat: number;
    lng: number;
  };
}

export interface MapHandle {
  setCenter: (lat: number, lng: number) => void;
}

const MapComponent = forwardRef<MapHandle, MapComponentProps>(
  ({ onCoordinateSelect, defaultCenter = { lat: 37.5665, lng: 126.9780 } }, ref) => {
    const mapContainer = useRef<HTMLDivElement>(null);
    const mapRef = useRef<any>(null);
    const markerRef = useRef<any>(null);

    // 지도 초기화 함수
    const initializeMap = () => {
      if (!mapContainer.current || !window.kakao) return;

      const options = {
        center: new window.kakao.maps.LatLng(defaultCenter.lat, defaultCenter.lng),
        level: 3,
        draggable: true,
        zoomable: true,
      };

      const mapInstance = new window.kakao.maps.Map(mapContainer.current, options);
      mapRef.current = mapInstance;

      // 초기 마커 설정
      const initialMarker = new window.kakao.maps.Marker({
        position: new window.kakao.maps.LatLng(defaultCenter.lat, defaultCenter.lng),
        map: mapInstance,
      });
      markerRef.current = initialMarker;

      // 지도 클릭 이벤트 설정
      window.kakao.maps.event.addListener(mapInstance, "click", (mouseEvent: any) => {
        const latlng = mouseEvent.latLng;

        // 마커 위치 업데이트
        markerRef.current.setPosition(latlng);

        const lat = latlng.getLat();
        const lng = latlng.getLng();
        if (onCoordinateSelect) {
          onCoordinateSelect(lat, lng);
        }
      });
    };

    // 부모 컴포넌트가 호출할 수 있는 메서드 정의
    useImperativeHandle(ref, () => ({
      setCenter: (lat: number, lng: number) => {
        if (mapRef.current) {
          const moveLatLng = new window.kakao.maps.LatLng(lat, lng);
          mapRef.current.setCenter(moveLatLng);
        }
      },
    }));

    // 클린업 함수
    const cleanup = () => {
      if (markerRef.current) {
        markerRef.current.setMap(null);
        markerRef.current = null;
      }
      if (mapRef.current) {
        window.kakao.maps.event.removeAllListeners(mapRef.current);
        mapRef.current = null;
      }
    };

    return (
      <>
        <Script
          strategy="beforeInteractive"
          src={`//dapi.kakao.com/v2/maps/sdk.js?appkey=6204e9e81582afd77a86a98ecd921498&autoload=false`}
          onLoad={() => {
            window.kakao.maps.load(initializeMap);
          }}
        />
        <div
          ref={mapContainer}
          style={{ width: "100%", height: "100%", backgroundColor: "#f8f9fa" }}
        />
      </>
    );
  }
);

MapComponent.displayName = "MapComponent";

export default MapComponent;
