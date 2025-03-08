// src/app/api/vworld-search/route.ts
import { NextResponse } from 'next/server';

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const query = searchParams.get('query');
    const apiKey = '6E9CE663-6911-306B-9982-F19C3EA3224C';  // API 키를 직접 설정

    if (!query) {
      return NextResponse.json(
        { error: '검색어가 필요합니다.' },
        { status: 400 }
      );
    }

    // VWorld API 직접 호출
    const vworldUrl = `http://api.vworld.kr/req/search?service=search&request=search&version=2.0&crs=EPSG:4326&size=10&page=1&query=${encodeURIComponent(query)}&type=address&category=road,parcel&format=json&errorformat=json&key=${apiKey}`;

    const response = await fetch(vworldUrl);
    const data = await response.json();

    // 응답 로깅
    console.log('VWorld API Response:', data);

    if (data.response?.status === 'OK') {
      return NextResponse.json(data);
    } else {
      return NextResponse.json(
        { error: '검색 결과가 없습니다.', details: data.response },
        { status: 404 }
      );
    }
  } catch (error) {
    console.error('VWorld API 요청 실패:', error);
    return NextResponse.json(
      { error: '검색 중 오류가 발생했습니다.' },
      { status: 500 }
    );
  }
}