"use client"

import React, { useState } from 'react';

const MapTypeControl = ({ map }) => {
  const [mapType, setMapType] = useState('roadmap');

  const handleMapTypeChange = (type) => {
    if (type === 'roadmap') {
      map.setMapTypeId(kakao.maps.MapTypeId.ROADMAP);
      setMapType('roadmap');
    } else {
      map.setMapTypeId(kakao.maps.MapTypeId.HYBRID);
      setMapType('hybrid');
    }
  };

  return (
    <div className="absolute top-4 left-4 z-50 flex rounded-lg overflow-hidden shadow-md">
      <button
        className={`px-4 py-2 text-sm font-medium ${
          mapType === 'roadmap'
            ? 'bg-blue-600 text-white'
            : 'bg-white text-gray-700 hover:bg-gray-50'
        }`}
        onClick={() => handleMapTypeChange('roadmap')}
      >
        지도
      </button>
      <button
        className={`px-4 py-2 text-sm font-medium border-l border-gray-200 ${
          mapType === 'hybrid'
            ? 'bg-blue-600 text-white'
            : 'bg-white text-gray-700 hover:bg-gray-50'
        }`}
        onClick={() => handleMapTypeChange('hybrid')}
      >
        위성지도
      </button>
    </div>
  );
};

export default MapTypeControl;