// src/app/components/Options.tsx
"use client";

import { useState } from "react";
import axios from "axios";
import { useMap } from "../context/MapContext";

type OptionsProps = {};

const Options: React.FC<OptionsProps> = () => {
  const [address, setAddress] = useState<string>("Berlin");
  const [latLon, setLatLon] = useState<string>("52.52 13.4");
  const [duration, setDuration] = useState<number>(3600);
  const [addPolygons, setAddPolygons] = useState<boolean>(true);
  const [importPublicTransport, setImportPublicTransport] = useState<boolean>(false);
  const [carOnlyNetwork, setCarOnlyNetwork] = useState<boolean>(false);
  const [decal, setDecal] = useState<boolean>(false);
  const [leftHand, setLeftHand] = useState<boolean>(false);

  const [canvasToggle, setCanvasToggle] = useState<boolean>(false);
  const [canvasActive, setCanvasActive] = useState<boolean>(false);

  const toggleCanvas = () => {
    setCanvasToggle(!canvasToggle);
    setCanvasActive(!canvasActive);
  };

  const { setView } = useMap();

  const handleSearch = async () => {
    try {
      const response = await axios.get(
        `https://nominatim.openstreetmap.org/search`,
        {
          params: {
            q: address,
            format: "json",
            limit: 1,
          },
        }
      );
      if (response.data && response.data.length > 0) {
        const { lat, lon } = response.data[0];
        setView(parseFloat(lon), parseFloat(lat));
      } else {
        alert("주소를 찾을 수 없습니다.");
      }
    } catch (error) {
      console.error(error);
      alert("주소 검색 중 오류가 발생했습니다.");
    }
  };

  const handleGoTo = () => {
    const [lat, lon] = latLon.split(" ").map(Number);
    if (isNaN(lat) || isNaN(lon)) {
      alert("유효한 좌표를 입력하세요.");
      return;
    }
    setView(lon, lat);
  };

  return (
    <div className="p-4">
      <h4 className="text-lg font-semibold mb-2">Position</h4>
      <div className="space-y-2">
        {/* 주소 검색 */}
        <div>
          <input
            type="text"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            size={18}
            className="border p-1 rounded w-full"
            placeholder="Enter address"
          />
          <button
            onClick={handleSearch}
            className="mt-1 bg-blue-500 text-white px-2 py-1 rounded w-full hover:bg-blue-600"
          >
            Search
          </button>
        </div>
        {/* 좌표 검색 */}
        <div>
          <input
            type="text"
            value={latLon}
            onChange={(e) => setLatLon(e.target.value)}
            size={18}
            className="border p-1 rounded w-full"
            placeholder="Enter latitude and longitude (e.g., 52.52 13.4)"
          />
          <button
            onClick={handleGoTo}
            className="mt-1 bg-blue-500 text-white px-2 py-1 rounded w-full hover:bg-blue-600"
          >
            Go to
          </button>
        </div>
        {/* 현재 위치 사용 */}
        <div>
          <button className="bg-blue-500 text-white px-2 py-1 rounded w-full hover:bg-blue-600">
            Use current location
          </button>
        </div>
      </div>

      <h4 className="text-lg font-semibold mt-4 mb-2">Options</h4>
      <div className="space-y-2">
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={canvasToggle}
            onChange={toggleCanvas}
            className="mr-2"
          />
          Select Area
        </label>
        <label className="flex items-center">
          <span className="mr-2">Duration</span>
          <input
            type="number"
            value={duration}
            onChange={(e) => setDuration(parseInt(e.target.value))}
            min="0"
            step="1"
            className="border p-1 rounded w-20"
          />
        </label>
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={addPolygons}
            onChange={(e) => setAddPolygons(e.target.checked)}
            className="mr-2"
          />
          Add Polygons
        </label>
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={importPublicTransport}
            onChange={(e) => setImportPublicTransport(e.target.checked)}
            className="mr-2"
          />
          Import Public Transport
        </label>
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={carOnlyNetwork}
            onChange={(e) => setCarOnlyNetwork(e.target.checked)}
            className="mr-2"
          />
          Car-only Network
        </label>
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={decal}
            onChange={(e) => setDecal(e.target.checked)}
            className="mr-2"
          />
          Satellite background
        </label>
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={leftHand}
            onChange={(e) => setLeftHand(e.target.checked)}
            className="mr-2"
          />
          Left Hand Traffic
        </label>
      </div>
    </div>
  );
};

export default Options;