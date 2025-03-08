// next.config.js
/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/vworld-proxy',
        destination: 'https://api.vworld.kr/req/search'
      }
    ];
  }
};

module.exports = nextConfig;