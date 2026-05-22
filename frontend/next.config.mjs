const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080';

/** @type {import('next').NextConfig} */
const nextConfig = {
  // /api/v1/analysis/run 은 Seoul API + LangGraph 까지 합쳐 60~90초 가능.
  // dev 서버 proxy 의 기본 idle timeout 이 짧아 socket hang up 이 자주 나서
  // 180초로 늘림.
  experimental: {
    proxyTimeout: 180_000,
  },
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${BACKEND}/api/:path*` }];
  },
};

export default nextConfig;
