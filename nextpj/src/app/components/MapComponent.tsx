// src/app/components/MapComponent.tsx
"use client";

import { useEffect, useRef, useImperativeHandle, forwardRef } from "react";
import "ol/ol.css";
import { Map, View } from "ol";
import { OSM } from "ol/source";
import { Tile as TileLayer } from "ol/layer";
import { fromLonLat } from "ol/proj";

const MapComponent = forwardRef((props, ref) => {
  const mapElement = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);

  useEffect(() => {
    if (!mapElement.current) return;

    const initialMap = new Map({
      target: mapElement.current,
      layers: [
        new TileLayer({
          source: new OSM(),
        }),
      ],
      view: new View({
        center: fromLonLat([13.4, 52.52]),
        zoom: 16,
      }),
    });

    mapRef.current = initialMap;

    return () => initialMap.setTarget(undefined);
  }, []);

  // 지도 관련 모든 필요한 메서드들을 노출시킵니다
  useImperativeHandle(ref, () => ({
    setView: (lon: number, lat: number) => {
      if (mapRef.current) {
        const view = mapRef.current.getView();
        view.setCenter(fromLonLat([lon, lat]));
        view.setZoom(16);
      }
    },
    // 선택 영역 변환에 필요한 메서드들 추가
    getSize: () => mapRef.current?.getSize(),
    getCoordinateFromPixel: (pixel: [number, number]) =>
      mapRef.current?.getCoordinateFromPixel(pixel),
    // 지도 객체 자체도 제공
    getMap: () => mapRef.current,
  }));

  return <div id="map" ref={mapElement} className="w-full h-full"></div>;
});

export default MapComponent;
