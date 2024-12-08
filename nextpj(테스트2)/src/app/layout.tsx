// src/app/layout.tsx
import "./globals.css";
import type { Metadata } from "next";
import Script from "next/script";
export const metadata: Metadata = {
  title: "OSM Web Wizard",
  description: "Generate traffic scenarios using OpenStreetMap data",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <head>
        <link rel="icon" href="/images/favicon.ico" />
      </head>
      <body>
        <div className="h-full">{children}</div>
        <Script
          src={`//dapi.kakao.com/v2/maps/sdk.js?appkey=6204e9e81582afd77a86a98ecd921498&libraries=services&autoload=false`}
          strategy="beforeInteractive"
        />
      </body>
    </html>
  );
}
