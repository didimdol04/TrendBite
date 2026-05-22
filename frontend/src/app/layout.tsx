import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'TrendBite — 신메뉴 트렌드 분석',
  description: '소자본 외식업 사장님을 위한 신메뉴 도입 의사결정 시뮬레이터',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body className="font-sans antialiased min-h-screen">{children}</body>
    </html>
  );
}
