'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Box, Btn, Chip } from '@/components/Primitives';
import { SketchLine } from '@/components/Charts';
import { NicknameGate } from '@/components/NicknameGate';
import { useUser } from '@/lib/userContext';
import { getPopularKeywords } from '@/lib/api';
import { LANDING_LIFECYCLE_SAMPLES } from '@/lib/mockData';
import type { TrendKeywordResponse } from '@/lib/types';

export default function LandingPage() {
  const router = useRouter();
  const { state } = useUser();
  const [popular, setPopular] = useState<TrendKeywordResponse[]>([]);
  const [showGate, setShowGate] = useState(false);

  useEffect(() => {
    getPopularKeywords(9).then(setPopular).catch(() => {});
  }, []);

  const startAnalysis = () => {
    if (state.kind === 'identified') {
      router.push('/analyze');
    } else {
      setShowGate(true);
    }
  };

  const enterAsGuest = () => {
    router.push('/analyze?guest=1');
  };

  return (
    <>
      <div style={{
        maxWidth: 1280, margin: '0 auto',
        padding: 'clamp(40px, 6vw, 56px) clamp(16px, 5vw, 80px) 48px',
        background: '#fff',
      }}>
        {/* hero */}
        <div className="hero-grid">
          <div>
            <div className="wf-eyebrow" style={{ marginBottom: 14 }}>
              외식업 신메뉴 수익성 예측 시뮬레이터
            </div>
            <h1 className="wf-h1" style={{ fontSize: 'clamp(28px, 4vw, 40px)', marginBottom: 16 }}>
              신메뉴 도입,<br />
              감이 아닌 <span style={{ color: 'var(--tb-navy)' }}>데이터로</span> 결정하세요
            </h1>
            <p className="wf-muted" style={{ fontSize: 14, marginBottom: 24, maxWidth: 460 }}>
              트렌드 분석, 입지 평가, 재무 시뮬레이션을 한 번에.
            </p>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <Btn primary lg onClick={startAnalysis}>내 메뉴 분석하기 →</Btn>
              <Btn lg onClick={() => router.push('/result/sample')}>샘플 리포트 보기</Btn>
            </div>
          </div>
          {/* hero illustration: lifecycle preview */}
          <Box style={{ padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 12 }}>
              <span className="wf-h3">메뉴 라이프사이클 비교</span>
              <span className="wf-mono">시드 패턴 3종</span>
            </div>
            <div style={{ display: 'grid', gap: 10 }}>
              {LANDING_LIFECYCLE_SAMPLES.map((d, i) => (
                <div key={i} style={{
                  display: 'grid', gridTemplateColumns: '70px 1fr 80px',
                  alignItems: 'center', gap: 10, padding: '8px 4px',
                  borderTop: i ? '1px solid var(--tb-line-2)' : 'none',
                }}>
                  <span className="wf-h3" style={{ fontSize: 13 }}>{d.name}</span>
                  <SketchLine width={220} height={48} data={d.data} fill />
                  <Chip>{d.label}</Chip>
                </div>
              ))}
            </div>
            <div style={{ marginTop: 12, fontSize: 12, color: 'var(--tb-navy)', fontWeight: 600 }}>
              당신의 메뉴는 어디쯤일까요?
            </div>
          </Box>
        </div>

        {/* 3 value cards */}
        <div className="value-cards" style={{ marginTop: 56, marginBottom: 48 }}>
          {[
            { num: '01', t: '트렌드 분석', d: '네이버 데이터랩과 DTW 유사사례 매칭으로 라이프사이클 단계를 진단합니다.' },
            { num: '02', t: '입지 평가', d: '서울 상권 데이터 기반 유동인구·매출·집객·경쟁 4축 분석.' },
            { num: '03', t: '재무 시뮬레이션', d: '원가, 인건비, 고정비를 반영한 영업이익 / 손익분기점 자동 계산.' },
          ].map((c, i) => (
            <Box key={i} style={{ padding: 22 }}>
              <div className="wf-mono" style={{ marginBottom: 16, color: 'var(--tb-navy)' }}>{c.num}</div>
              <div className="wf-h2" style={{ marginBottom: 8 }}>{c.t}</div>
              <p className="wf-muted" style={{ fontSize: 12.5, margin: 0 }}>{c.d}</p>
            </Box>
          ))}
        </div>

        {/* trending keywords */}
        <div style={{ marginBottom: 40 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
            <div className="wf-h2">지금 사장님들이 많이 보는 메뉴</div>
            <span className="wf-mono">최근 7일 · 검색 카운트 기준</span>
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {popular.map((k, i) => (
              <Chip
                key={k.keyword}
                tone={i < 3 ? 'navy' : null}
                onClick={() => router.push(`/analyze?keyword=${encodeURIComponent(k.keyword)}&category=${k.category}`)}
              >
                {k.keyword}
                <span className="wf-mono" style={{ marginLeft: 4, opacity: 0.7 }}>{k.searchCount}</span>
              </Chip>
            ))}
          </div>
        </div>

        {/* big CTA */}
        <Box style={{
          padding: '32px 36px', display: 'flex',
          justifyContent: 'space-between', alignItems: 'center',
          background: 'var(--tb-navy)', borderColor: 'var(--tb-navy)',
          flexWrap: 'wrap', gap: 16,
        }}>
          <div>
            <div style={{ color: '#fff', fontSize: 20, fontWeight: 700, marginBottom: 6 }}>
              지금 바로, 내 메뉴를 검증해보세요
            </div>
            <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: 13 }}>
              평균 3분 · 닉네임/가게명 한 줄이면 결과 저장도 가능
            </div>
          </div>
          <Btn
            lg
            onClick={startAnalysis}
            style={{ background: '#fff', color: 'var(--tb-navy)', border: 0, fontWeight: 700 }}
          >
            내 메뉴 분석하기 →
          </Btn>
        </Box>
      </div>

      <style jsx>{`
        .hero-grid {
          display: grid;
          grid-template-columns: 1.2fr 1fr;
          gap: 48px;
          align-items: center;
        }
        .value-cards {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 16px;
        }
        @media (max-width: 900px) {
          .hero-grid { grid-template-columns: 1fr; gap: 32px; }
          .value-cards { grid-template-columns: 1fr; }
        }
      `}</style>

      <NicknameGate
        open={showGate}
        onClose={() => { setShowGate(false); router.push('/analyze'); }}
        onProceedAsGuest={enterAsGuest}
        title="시작하기 전에"
      />
    </>
  );
}
