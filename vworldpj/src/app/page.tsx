// src/app/page.tsx
'use client';

import dynamic from 'next/dynamic';
import SearchBar from '@/components/map/SearchBar';

const VWorldMap = dynamic(() => import('@/components/map/VWorldMap'), {
  ssr: false,
  loading: () => (
    <div className="w-full h-screen flex items-center justify-center bg-gray-100">
      <p className="text-lg text-gray-600">지도를 불러오는 중...</p>
    </div>
  ),
});

export default function Home() {
  return (
    <VWorldMap>
      <SearchBar />
    </VWorldMap>
  );
}