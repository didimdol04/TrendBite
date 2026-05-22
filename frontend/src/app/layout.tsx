import type { Metadata } from 'next';
import './globals.css';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import { UserProvider } from '@/lib/userContext';
import { TbToaster } from '@/components/Toast';

export const metadata: Metadata = {
  title: 'TrendBite — 신메뉴 수익성 시뮬레이터',
  description:
    '소자본 외식업 사장님을 위한 신메뉴 도입 의사결정 시뮬레이터. 트렌드 + 입지 + 재무를 30초 안에.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>
        <UserProvider>
          <div className="tb-page">
            <Header />
            <main className="tb-main">{children}</main>
            <Footer />
          </div>
          <TbToaster />
        </UserProvider>
      </body>
    </html>
  );
}
