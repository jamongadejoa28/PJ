// src/app/components/MapComponent.tsx
"use client";

import { useEffect, useRef, useImperativeHandle, forwardRef } from "react";

declare global {
  interface Window {
    kakao: any;
  }
}

interface MapProps {
  onCoordinateSelect?: (lat: number, lng: number, address: string) => void;
  initialCenter?: { lat: number; lng: number };
  initialZoom?: number;
}

interface MapHandle {
  setView: (lon: number, lat: number) => void;
  getMap: () => any;
}

const MapComponent = forwardRef<MapHandle, MapProps>(({
  onCoordinateSelect,
  initialCenter = { lat: 37.5665, lng: 126.9780 },
  initialZoom = 3,
}, ref) => {
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);

  useEffect(() => {
    const script = document.createElement("script");
    script.src = "//dapi.kakao.com/v2/maps/sdk.js?appkey=&libraries=services&autoload=false";
    document.head.appendChild(script);

    script.onload = () => {
      window.kakao.maps.load(() => {
        const container = document.getElementById("map");
        const options = {
          center: new window.kakao.maps.LatLng(initialCenter.lat, initialCenter.lng),
          level: initialZoom
        };

        const map = new window.kakao.maps.Map(container, options);
        mapRef.current = map;

        const geocoder = new window.kakao.maps.services.Geocoder();

        window.kakao.maps.event.addListener(map, 'click', (mouseEvent: any) => {
          const latlng = mouseEvent.latLng;

          if (markerRef.current) {
            markerRef.current.setMap(null);
          }

          const marker = new window.kakao.maps.Marker({
            position: latlng
          });
          marker.setMap(map);
          markerRef.current = marker;

          geocoder.coord2Address(latlng.getLng(), latlng.getLat(), (result: any, status: any) => {
            if (status === window.kakao.maps.services.Status.OK && onCoordinateSelect) {
              onCoordinateSelect(
                latlng.getLat(),
                latlng.getLng(),
                result[0].address.address_name
              );
            }
          });
        });
      });
    };

    return () => {
      document.head.removeChild(script);
    };
  }, [initialCenter, initialZoom, onCoordinateSelect]);

  useImperativeHandle(ref, () => ({
    setView: (lon: number, lat: number) => {
      if (mapRef.current) {
        const moveLatLon = new window.kakao.maps.LatLng(lat, lon);
        mapRef.current.setCenter(moveLatLon);
      }
    },
    getMap: () => mapRef.current
  }));

  return <div id="map" className="w-full h-full" />;
});

MapComponent.displayName = 'MapComponent';

export default MapComponent;
