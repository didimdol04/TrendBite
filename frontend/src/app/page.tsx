'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import type { TrendKeyword } from '@/lib/types';

export default function Landing() {
  const [popular, setPopular] = useState<TrendKeyword[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.popular(8).then(setPopular).catch(e => setError(String(e)));
  }, []);

  return (
    <main className="bg-navy text-white min-h-screen">
      <header className="px-8 py-6 flex justify-between items-center">
        <div className="text-xl font-bold">
          TrendBite <span className="text-gold">두쫀쿠</span>
        </div>
        <Link
          href="/analyze"
          className="bg-gold text-navy px-4 py-2 rounded-lg font-semibold hover:bg-gold-dark transition"
        >
          분석 시작
        </Link>
      </header>

      <section className="px-8 py-16 max-w-5xl mx-auto">
        <h1 className="text-5xl font-extrabold leading-tight mb-4">
          신메뉴 도입,
          <br />
          <span className="text-gold">감이 아닌 데이터로</span> 결정하세요
        </h1>
        <p className="text-lg text-white/80 mb-12">
          트렌드 + 입지 + 재무를 한 번에. 30초 안에 AI 리포트까지.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-16">
          {[
            { t: '트렌드 분석', d: '네이버 검색량 + DTW 유사 사례 매칭' },
            { t: '입지 평가', d: '서울 상권 데이터 + 외식업 경쟁도' },
            { t: '재무 시뮬레이션', d: '원가/이익/BEP + 신호등 경고' },
          ].map(({ t, d }) => (
            <div
              key={t}
              className="bg-white/10 backdrop-blur rounded-xl p-6"
            >
              <div className="text-xl font-bold mb-2">{t}</div>
              <div className="text-white/70 text-sm">{d}</div>
            </div>
          ))}
        </div>

        <div className="mb-8">
          <div className="text-sm text-white/60 mb-3">
            지금 사장님들이 많이 보는 메뉴
          </div>
          {error && (
            <div className="text-red-300 text-sm">
              백엔드 연결 실패: {error}
            </div>
          )}
          <div className="flex flex-wrap gap-2">
            {popular.map(k => (
              <Link
                key={`${k.keyword}-${k.category}`}
                href={`/analyze?keyword=${encodeURIComponent(
                  k.keyword,
                )}&category=${encodeURIComponent(k.category)}`}
                className="bg-white/10 hover:bg-white/20 px-4 py-2 rounded-full text-sm transition"
              >
                {k.keyword}{' '}
                <span className="text-white/50">({k.searchCount})</span>
              </Link>
            ))}
            {!error && popular.length === 0 && (
              <span className="text-white/40 text-sm">로딩 중…</span>
            )}
          </div>
        </div>

        <Link
          href="/analyze"
          className="inline-block bg-gold text-navy text-lg font-bold px-8 py-4 rounded-xl hover:bg-gold-dark transition"
        >
          내 메뉴 분석하기 →
        </Link>
      </section>
    </main>
  );
}
