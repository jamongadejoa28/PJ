// next.config.ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
 reactStrictMode: true,
 scripts: [
   {
     src: `//dapi.kakao.com/v2/maps/sdk.js?appkey=6204e9e81582afd77a86a98ecd921498&libraries=services,clusterer,drawing`,
     strategy: "beforeInteractive" 
   }
 ]
};

export default nextConfig;