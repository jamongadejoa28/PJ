// src/app/layout.tsx
import { ReactNode } from 'react';
import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'VWorld 3D Map',
  description: 'VWorld 3D Map with Next.js',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <head>
        {/* VWorld 맵 필수 스크립트들을 순서대로 로드 */}
        <script src="https://map.vworld.kr/js/webglMapInit.js.do?version=3.0&apiKey=6E9CE663-6911-306B-9982-F19C3EA3224C" async={false} />
      </head>
      <body>{children}</body>
    </html>
  );
}