// src/app/page.tsx
"use client";

export default function Home() {
  // 새 탭을 여는 함수를 정의합니다
  const handleOpenWizard = () => {
    // window.open()을 사용하여 새 탭에서 wizard 페이지를 엽니다
    window.open('/wizard', '_blank', 'noopener,noreferrer');
  };
  
  return (
    <main className="h-screen flex flex-col items-center justify-center bg-gradient-to-b from-indigo-100 to-white">
      <h1 className="text-4xl font-bold text-gray-800 mb-8">
        SUMO Traffic Simulation
      </h1>
      <button 
        onClick={handleOpenWizard}  // 클릭 핸들러를 새 함수로 변경합니다
        className="px-12 py-6 text-xl font-semibold text-white 
                  bg-gradient-to-r from-indigo-600 to-indigo-800
                  rounded-lg shadow-lg hover:shadow-xl
                  transform hover:scale-105 transition-all duration-200
                  hover:from-indigo-700 hover:to-indigo-900"
      >
        OSM Web Wizard 실행
      </button>
      <p className="mt-4 text-gray-600 text-center max-w-md">
        OpenStreetMap 데이터를 사용하여 교통 시뮬레이션을 
        쉽게 생성하고 관리하세요.
      </p>
    </main>
  );
}