'use client';

import { useState, KeyboardEvent } from 'react';
import { useMapContext } from '@/contexts/MapContext';

// 검색 결과의 타입 정의
interface SearchResult {
  address: {
    road?: string;    // 도로명 주소
    parcel?: string;  // 지번 주소
    bldnm?: string;   // 건물명
  };
  point: {
    x: string;  // 경도
    y: string;  // 위도
  };
}

export default function SearchBar() {
  // MapContext에서 지도 객체 가져오기
  const { mapObj } = useMapContext();
  
  // 상태 관리
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // JSONP 방식으로 주소 검색을 수행하는 함수
  const performSearch = (query: string) => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    setIsLoading(true);

    // 이전에 생성된 검색 스크립트가 있다면 제거
    const oldScript = document.getElementById('searchScript');
    if (oldScript) {
      oldScript.remove();
    }

    // VWorld API의 응답을 처리할 전역 콜백 함수
    (window as any).handleSearchResult = (data: any) => {
      setIsLoading(false);
      
      if (data.response.status === 'OK' && data.response.result?.items) {
        setSearchResults(data.response.result.items);
      } else {
        setSearchResults([]);
      }
    };

    // JSONP 요청을 위한 스크립트 태그 생성
    const script = document.createElement('script');
    script.id = 'searchScript';
    const apiKey = '6E9CE663-6911-306B-9982-F19C3EA3224C';
    
    // VWorld 검색 API URL 구성
    // category=road로 설정하여 도로명 주소 검색에 초점
    script.src = `https://api.vworld.kr/req/search?service=search&request=search&version=2.0&crs=EPSG:4326&size=10&page=1&query=${encodeURIComponent(query)}&type=address&category=road&format=json&errorformat=json&key=${apiKey}&callback=handleSearchResult`;
    
    // 생성된 스크립트를 문서에 추가하여 API 요청 실행
    document.body.appendChild(script);
  };

  // 검색 입력란에서 Enter 키 입력 처리
  const handleKeyPress = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      performSearch(searchValue);
    }
  };

  // 검색 결과 위치로 지도 이동
  const moveToLocation = (lon: number, lat: number, locationType: string) => {
    if (!mapObj?.map) return;

    // 위치 유형에 따른 고도 설정
    // 도로는 더 자세히 보기 위해 낮은 고도 사용
    const z = locationType === 'road' ? 300 : 500;
    
    // VWorld 지도 이동을 위한 좌표 및 카메라 위치 설정
    const movePo = new vw.CoordZ(lon, lat, z);
    // 수직으로 내려다보는 시야 설정 (-90도)
    const mPosi = new vw.CameraPosition(movePo, new vw.Direction(0, -90, 0));
    
    // 부드러운 애니메이션으로 이동 (1초)
    mapObj.map.moveTo(mPosi, 1);
  };

  return (
    <div className="absolute top-5 left-5 z-10">
      <div className="bg-white rounded-lg shadow-lg p-4 w-[400px]">
        {/* 검색 입력 영역 */}
        <div className="flex gap-2">
          <input
            type="text"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="주소를 입력하세요"
            className="flex-1 px-4 py-2 text-gray-700 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={() => performSearch(searchValue)}
            disabled={isLoading}
            className={`px-4 py-2 rounded-lg ${
              isLoading 
                ? 'bg-gray-400' 
                : 'bg-blue-500 hover:bg-blue-600'
            } text-white focus:outline-none focus:ring-2 focus:ring-blue-500`}
          >
            {isLoading ? (
              <div className="w-5 h-5 border-t-2 border-white rounded-full animate-spin" />
            ) : (
              '검색'
            )}
          </button>
        </div>

        {/* 검색 결과 목록 */}
        {searchResults.length > 0 && (
          <div className="mt-3 max-h-[300px] overflow-y-auto">
            {searchResults.map((item, index) => (
              <div
                key={index}
                onClick={() => {
                  const lon = parseFloat(item.point.x);
                  const lat = parseFloat(item.point.y);
                  const locationType = item.address.road ? 'road' : 'parcel';
                  moveToLocation(lon, lat, locationType);
                }}
                className="p-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100 last:border-b-0"
              >
                <p className="text-gray-800">
                  {item.address.road || item.address.parcel}
                </p>
                {item.address.bldnm && (
                  <p className="text-sm text-gray-500 mt-1">
                    {item.address.bldnm}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}