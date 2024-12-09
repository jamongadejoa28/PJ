/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    domains: ['dapi.kakao.com'], // 카카오맵 이미지를 위한 도메인 설정
  },
  webpack: (config) => {
    config.module.rules.push({
      test: /\.css$/,
      use: ['style-loader', 'css-loader'],
    });
    return config;
  },
}

module.exports = nextConfig