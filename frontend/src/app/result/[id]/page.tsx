'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Box, Btn, Badge, Chip, PendingMarker, fmt } from '@/components/Primitives';
import type { AnalysisRequest } from '@/lib/types';
import { SketchLine, SketchDonut, SketchGauge, SketchWaterfall } from '@/components/Charts';
import { KakaoMap } from '@/components/KakaoMap';
import { useUser } from '@/lib/userContext';
import { MOCK_ANALYSIS_RESPONSE, MOCK_LIFECYCLES } from '@/lib/mockData';
import type { AnalysisResponse, SimilarPattern, LifecycleResponse } from '@/lib/types';
import { toast } from '@/components/Toast';
import Link from 'next/link';

type LoadedResult = {
  id: string;
  menuName: string;
  category: string;
  address?: string;
  createdAt: string;
  data: AnalysisResponse['data'];
  menuId?: number;
  noTrend?: boolean;
  originalRequest?: AnalysisRequest;
};

export default function ResultPage() {
  const params = useParams();
  const router = useRouter();
  const { getAnalysis } = useUser();
  const id = (params.id as string) || 'sample';

  const [result, setResult] = useState<LoadedResult | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id === 'sample') {
      setResult({
        id: 'sample',
        menuName: '마라탕',
        category: 'dining',
        address: '서울특별시 강남구 학동로',
        createdAt: new Date().toISOString(),
        data: MOCK_ANALYSIS_RESPONSE.data,
      });
    } else if (id === 'guest') {
      try {
        const raw = sessionStorage.getItem('trendbite_guest_result');
        if (raw) {
          const r = JSON.parse(raw);
          setResult({ id: 'guest', ...r });
        }
      } catch {}
    } else {
      const saved = getAnalysis(id);
      if (saved) {
        setResult({
          id: saved.id,
          menuName: saved.menuName,
          category: saved.category,
          address: saved.address,
          createdAt: saved.createdAt,
          data: saved.response,
          menuId: saved.menuId,
          noTrend: saved.noTrend,
          originalRequest: saved.originalRequest,
        });
      }
    }
    setLoading(false);
  }, [id, getAnalysis]);

  if (loading) {
    return <div style={{ padding: 40, textAlign: 'center' }}>로딩 중...</div>;
  }
  if (!result) {
    return (
      <div style={{ padding: 80, textAlign: 'center' }}>
        <div className="wf-h2" style={{ marginBottom: 8 }}>분석 결과를 찾을 수 없어요</div>
        <p className="wf-muted" style={{ marginBottom: 20 }}>닉네임 로그아웃 상태이거나 잘못된 ID입니다.</p>
        <Btn primary onClick={() => router.push('/')}>홈으로</Btn>
      </div>
    );
  }

  return (
    <div style={{ background: '#fff' }}>
      <VerdictHero result={result} />
      <ScoreBreakdownSection data={result.data} noTrend={result.noTrend} />
      <TrendSection data={result.data} noTrend={result.noTrend} />
      <LocationSection data={result.data} />
      <FinanceSection data={result.data} />
      <AIReportSection data={result.data} result={result} />
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Verdict Hero (와이어프레임 A안 - 풀-블리드 네이비)
// ────────────────────────────────────────────────────────
function VerdictHero({ result }: { result: LoadedResult }) {
  const router = useRouter();
  const { verdict, scores } = result.data.report;
  const verdictBadge = VERDICT_BADGE[verdict];
  const headline = VERDICT_HEADLINE[verdict];

  const handleRetry = () => {
    if (result.originalRequest) {
      try { sessionStorage.setItem('trendbite_wizard_draft', JSON.stringify(result.originalRequest)); } catch {}
    }
    router.push('/analyze');
  };

  return (
    <div style={{
      background: 'var(--tb-navy)', color: '#fff',
      padding: 'clamp(40px, 6vw, 64px) clamp(20px, 6vw, 80px) clamp(36px, 5vw, 56px)',
      position: 'relative', overflow: 'hidden',
    }}>
      <div className="wf-eyebrow" style={{ color: 'rgba(255,255,255,0.6)', marginBottom: 16 }}>
        VERDICT · 분석 완료
      </div>
      <div className="hero-grid">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 18, flexWrap: 'wrap' }}>
            <span style={{
              background: verdictBadge.bg, color: verdictBadge.fg,
              padding: '8px 16px', borderRadius: 999,
              fontWeight: 800, fontSize: 13, letterSpacing: '0.04em',
            }}>{verdictBadge.icon} {verdict}</span>
            <span className="wf-mono" style={{ color: 'rgba(255,255,255,0.5)' }}>
              {result.menuName} · {result.address || '주소 없음'} · {result.createdAt.slice(0, 10)}
            </span>
          </div>
          <div style={{
            fontSize: 'clamp(28px, 4vw, 44px)',
            fontWeight: 700, letterSpacing: '-0.02em',
            lineHeight: 1.15, marginBottom: 18,
          }}>
            {headline.title(result.menuName)}<br />
            <span style={{ color: 'var(--tb-amber)' }}>{headline.highlight}</span>
          </div>
          <p style={{ color: 'rgba(255,255,255,0.75)', fontSize: 15, maxWidth: 640, margin: 0 }}>
            {result.data.report.llmReport.split('\n')[0].slice(0, 200)}
          </p>
        </div>
        <div style={{
          background: 'rgba(255,255,255,0.08)', borderRadius: 16, padding: 20,
          textAlign: 'center', border: '1px solid rgba(255,255,255,0.15)',
        }}>
          {result.noTrend ? (
            <svg width={160} height={160}>
              <circle cx={80} cy={80} r={68} stroke="rgba(255,255,255,0.15)" strokeWidth="10" fill="none" />
              <text x={80} y={76} textAnchor="middle" fontSize="28" fontWeight={700}
                fill="rgba(255,255,255,0.35)" fontFamily="var(--tb-font)">N/A</text>
              <text x={80} y={100} textAnchor="middle" fontSize="11"
                fill="rgba(255,255,255,0.3)" fontFamily="var(--tb-font-mono)">종합 점수</text>
              <text x={80} y={116} textAnchor="middle" fontSize="10"
                fill="rgba(255,255,255,0.25)" fontFamily="var(--tb-font-mono)">트렌드 제외</text>
            </svg>
          ) : (
            <SketchDonut size={160} value={scores.total} label="종합 점수" color="var(--tb-amber)" />
          )}
        </div>
      </div>

      {/* floating action bar */}
      <div className="no-print" style={{
        position: 'absolute', top: 16, right: 24,
        display: 'flex', gap: 6, padding: 6,
        background: 'rgba(255,255,255,0.08)',
        border: '1px solid rgba(255,255,255,0.15)', borderRadius: 8,
      }}>
        {[
          { icon: '↻', label: '다시', onClick: handleRetry },
          { icon: '⤓', label: 'PDF', onClick: () => window.print() },
          { icon: '↗', label: '공유', onClick: () => alert('🔶 공유 기능 추후 구현') },
        ].map((a, i) => (
          <button
            key={i}
            onClick={a.onClick}
            style={{
              background: 'transparent', border: 0,
              padding: '6px 10px', fontSize: 11.5, color: '#fff', cursor: 'pointer',
            }}>{a.icon} {a.label}</button>
        ))}
      </div>

      <style jsx>{`
        .hero-grid {
          display: grid;
          grid-template-columns: 1fr 220px;
          gap: 48px;
          align-items: center;
        }
        @media (max-width: 900px) {
          .hero-grid { grid-template-columns: 1fr; gap: 24px; }
        }
      `}</style>
    </div>
  );
}

const VERDICT_BADGE: Record<string, { icon: string; bg: string; fg: string }> = {
  '도입 권장': { icon: '✓', bg: 'var(--tb-amber)', fg: '#1A1100' },
  '신중 검토': { icon: '⚠', bg: 'var(--tb-amber)', fg: '#1A1100' },
  '불확실':   { icon: '?', bg: '#EEF1F5',         fg: 'var(--tb-ink-2)' },
  '보류':     { icon: '✕', bg: '#2A2F3A',         fg: '#fff' },
};
const VERDICT_HEADLINE: Record<string, { title: (m: string) => string; highlight: string }> = {
  '도입 권장': { title: (m) => `${m}은(는) 지금 도입하기에`, highlight: '좋은 타이밍입니다' },
  '신중 검토': { title: (m) => `${m}은(는) 도입 전`,         highlight: '신중하게 검토해보세요' },
  '불확실':   { title: (m) => `${m}은(는) 판단을`,           highlight: '단정하기 어려운 상태입니다' },
  '보류':     { title: (m) => `${m}은(는) 지금`,             highlight: '도입을 보류하는 게 좋아요' },
};

// ────────────────────────────────────────────────────────
// Score Breakdown
// ────────────────────────────────────────────────────────
function ScoreBreakdownSection({ data, noTrend }: { data: AnalysisResponse['data']; noTrend?: boolean }) {
  const { scores } = data.report;
  const nonTrendItems = [
    { t: '수익성',  v: scores.profitability,  sub: '이익률 ' + fmt.pct(data.finance.data.monthlyResult.profitMargin) },
    { t: '지속성',  v: scores.sustainability, sub: '예상 24주' },
    { t: '종합',    v: scores.total,          sub: 'verdict: ' + data.report.verdict },
  ];
  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: '#fff' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 16, flexWrap: 'wrap', gap: 8 }}>
        <div className="wf-h2">점수 분해</div>
        <span className="wf-mono">
          <PendingMarker>점수 산출 공식 합의 필요 (FastAPI LangGraph)</PendingMarker>
        </span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
        {/* 트렌드 점수 — noTrend 시 회색 N/A */}
        <Box style={{ padding: 18 }}>
          {noTrend ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span className="wf-h3">트렌드</span>
                <span style={{ fontSize: 18, fontWeight: 700, color: 'var(--tb-ink-4)' }}>N/A</span>
              </div>
              <div style={{ height: 6, background: 'var(--tb-line-2)', borderRadius: 999 }} />
              <span className="wf-mono" style={{ color: 'var(--tb-ink-4)' }}>트렌드 데이터 없음</span>
            </div>
          ) : (
            <SketchGauge value={scores.trend} label="트렌드" sub={data.trend.plc_stage + ' · ' + data.trend.overall_trend} />
          )}
        </Box>
        {nonTrendItems.map((c, i) => (
          <Box key={i} style={{ padding: 18 }}>
            <SketchGauge value={c.v} label={c.t} sub={c.sub} />
          </Box>
        ))}
      </div>
    </section>
  );
}

// ────────────────────────────────────────────────────────
// Trend Section
// ────────────────────────────────────────────────────────
function TrendSection({ data, noTrend }: { data: AnalysisResponse['data']; noTrend?: boolean }) {
  if (noTrend) {
    return (
      <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: 'var(--tb-card)' }}>
        <div style={{ marginBottom: 16 }}>
          <div className="wf-eyebrow" style={{ marginBottom: 4 }}>SECTION C</div>
          <div className="wf-h2">트렌드 분석</div>
        </div>
        <Box style={{ padding: '36px 24px', textAlign: 'center' }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 56, height: 56, borderRadius: 999,
            background: 'var(--tb-line-2)', marginBottom: 16,
          }}>
            <span style={{ fontSize: 22, color: 'var(--tb-ink-4)', fontWeight: 700 }}>—</span>
          </div>
          <div className="wf-h3" style={{ marginBottom: 8 }}>이 메뉴는 트렌드 데이터가 없습니다</div>
          <p className="wf-muted" style={{ fontSize: 13, maxWidth: 400, margin: '0 auto', lineHeight: 1.6 }}>
            TrendBite에 등록된 키워드가 아닌 메뉴입니다.<br />
            PLC 분석 · 관심도 곡선 · DTW 유사 패턴은 표시되지 않습니다.
          </p>
        </Box>
      </section>
    );
  }

  const t = data.trend;
  const plcStage = t.plc_stage;

  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: 'var(--tb-card)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div className="wf-eyebrow" style={{ marginBottom: 4 }}>SECTION C</div>
          <div className="wf-h2">트렌드 분석</div>
        </div>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          <Chip tone="navy">PLC: {plcStage}</Chip>
          <Chip>전체 {t.overall_trend}</Chip>
          <Chip>최근 {t.recent_trend}</Chip>
          <Chip>속도 {t.trend_speed}</Chip>
        </div>
      </div>

      {/* 4 트렌드 지표 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12, marginBottom: 16 }}>
        {[
          { t: '최신 관심도',   v: t.latest_interest.toFixed(1) },
          { t: '평균 관심도',   v: t.average_interest.toFixed(1) },
          { t: '정점 대비',     v: (t.peak_ratio * 100).toFixed(0) + '%' },
          { t: '추세 기울기',   v: t.growth_rate.toFixed(2) },
        ].map((k, i) => (
          <Box key={i} style={{ padding: 16 }}>
            <div className="wf-mono" style={{ marginBottom: 6 }}>{k.t}</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--tb-navy)' }}>{k.v}</div>
          </Box>
        ))}
      </div>

      {/* PLC + 월별 관심도 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16, marginBottom: 16 }}>
        <Box style={{ padding: 18 }}>
          <div className="wf-h3" style={{ marginBottom: 8 }}>PLC 라이프사이클 곡선</div>
          <PlcCurve current={plcStage} />
        </Box>
        <Box style={{ padding: 18 }}>
          <div className="wf-h3" style={{ marginBottom: 8 }}>
            월별 관심도 <span className="wf-mono">네이버 데이터랩</span>
          </div>
          {/* 데모: trend_score를 단순 시계열로 가시화 (실제 시계열은 백엔드 응답에 없음) */}
          <SketchLine
            width={380} height={150}
            data={fakeMonthlySeriesFromScore(t.trend_score, t.peak_ratio)}
            fill
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
            <span className="wf-mono">12개월 전</span>
            <span className="wf-mono">현재</span>
          </div>
        </Box>
      </div>

      {/* DTW similar patterns */}
      <div className="wf-h3" style={{ marginBottom: 8 }}>
        유사 과거 패턴 (DTW Top {t.similar_patterns.length})
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 12 }}>
        {t.similar_patterns.map((p, i) => (
          <SimilarPatternCard key={i} p={p} />
        ))}
      </div>
    </section>
  );
}

function SimilarPatternCard({ p }: { p: SimilarPattern }) {
  const lifecycle = MOCK_LIFECYCLES.find(l => l.name === p.lifecycle);
  // 데모: lifecycle 이름에 따라 다른 곡선 모양
  const series = LIFECYCLE_SAMPLE_SERIES[p.lifecycle] || [20, 40, 60, 70, 60, 50, 45, 40, 35];
  return (
    <Box style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 6 }}>
        <span className="wf-h3">{p.name}</span>
        <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--tb-navy)' }}>
          {(p.dtw_similarity * 100).toFixed(0)}%
        </span>
      </div>
      <SketchLine width={260} height={70} data={series} fill color={lifecycle?.color} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
        <Chip style={lifecycle ? { background: lifecycle.color + '22', color: lifecycle.color, borderColor: 'transparent', fontWeight: 600 } : undefined}>
          {p.lifecycle}
        </Chip>
        <span className="wf-mono">유사도</span>
      </div>
      {lifecycle && (
        <div className="wf-mono" style={{ marginTop: 6, fontSize: 10.5 }}>
          {lifecycle.description}
        </div>
      )}
    </Box>
  );
}

const LIFECYCLE_SAMPLE_SERIES: Record<string, number[]> = {
  '단기급락':   [10, 25, 60, 100, 90, 50, 25, 15, 10],
  '유행후정체': [15, 40, 80, 100, 80, 60, 50, 45, 42],
  '계절성':     [20, 40, 60, 80, 60, 30, 20, 40, 70, 80, 60, 30],
  '안정정착':   [10, 30, 60, 85, 80, 75, 72, 70, 70],
  '지속성장':   [10, 20, 35, 50, 65, 80, 92, 96, 100],
  '재유행':     [30, 50, 70, 50, 30, 40, 60, 75, 65],
};

function PlcCurve({ current }: { current: string }) {
  // 도입기 / 성장기 / 성숙기 / 쇠퇴기 영역
  const stages = ['도입기', '성장기', '성숙기', '쇠퇴기'];
  // 한글 매핑 (백엔드값)
  const normalizedCurrent =
    current.includes('도입') ? '도입기' :
    current.includes('성장') ? '성장기' :
    current.includes('성숙') ? '성숙기' :
    current.includes('쇠퇴') ? '쇠퇴기' : '성숙기';
  const idx = stages.indexOf(normalizedCurrent);
  const xCenters = [65, 155, 245, 335];
  const yCenters = [150, 60, 50, 130];
  return (
    <svg viewBox="0 0 380 200" width="100%" height="180" style={{ display: 'block' }}>
      <line x1="20" y1="160" x2="370" y2="160" className="sketch-axis" />
      <path d="M 20 150 Q 90 130, 130 80 T 230 40 Q 290 30, 330 80 T 370 150" className="sketch-line" />
      {[110, 200, 290].map((x, i) => (
        <line key={i} x1={x} y1="20" x2={x} y2="160" className="sketch-grid" strokeDasharray="3 3" />
      ))}
      {stages.map((s, i) => (
        <text key={s} x={xCenters[i]} y="178" textAnchor="middle" fontSize="11"
          fill={i === idx ? 'var(--tb-navy)' : 'var(--tb-ink-3)'}
          fontWeight={i === idx ? 700 : 400}
          fontFamily="var(--tb-font-mono)">
          {s}{i === idx ? ' ●' : ''}
        </text>
      ))}
      {idx >= 0 && (
        <>
          <circle cx={xCenters[idx]} cy={yCenters[idx]} r="7" className="sketch-marker" />
          <text x={xCenters[idx]} y={yCenters[idx] - 14} textAnchor="middle"
            className="sketch" fontSize="14" fill="var(--tb-amber)">현재 위치</text>
        </>
      )}
    </svg>
  );
}

function fakeMonthlySeriesFromScore(score: number, peakRatio: number): number[] {
  // 데모용: trend_score + peak_ratio로 단순 곡선 생성
  const peak = Math.max(score, peakRatio * 100);
  return Array.from({ length: 12 }, (_, i) => {
    const t = i / 11;
    return Math.round(score * 0.6 + Math.sin(t * Math.PI) * peak * 0.4);
  });
}

// ────────────────────────────────────────────────────────
// Location Section
// ────────────────────────────────────────────────────────
function LocationSection({ data }: { data: AnalysisResponse['data'] }) {
  const loc = data.location.data;
  const ds = loc.rawMetrics.dataSourceStatus;
  const competitionWarn = loc.rawMetrics.competitionCount > 5;

  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: '#fff' }}>
      <div style={{ marginBottom: 16 }}>
        <div className="wf-eyebrow" style={{ marginBottom: 4 }}>SECTION D</div>
        <div className="wf-h2">입지 분석</div>
      </div>

      {/* nearestFallback 안내 (pass-2 추가 필드) */}
      {loc.locationInfo.nearestFallback && (
        <Box style={{
          padding: 12, marginBottom: 16,
          background: 'var(--tb-amber-soft)', borderColor: 'transparent',
        }}>
          <span style={{ fontSize: 12, color: '#806100' }}>
            ⓘ 정확한 상권 폴리곤 매칭이 안 되어 약 <b>{Math.round(loc.locationInfo.nearestDistanceMeters || 0)}m</b> 떨어진 인근 상권 데이터를 사용했습니다.
          </span>
        </Box>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16, marginBottom: 16 }}>
        <Box style={{ padding: 12 }}>
          <KakaoMap lat={loc.locationInfo.lat} lng={loc.locationInfo.lng} height={260} />
        </Box>
        <Box style={{ padding: 24, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div className="wf-mono" style={{ marginBottom: 6 }}>입지 등급 (1~10)</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 18 }}>
              <span style={{ fontSize: 56, fontWeight: 800, color: 'var(--tb-navy)', lineHeight: 1 }}>
                {loc.finalResult.grade}
              </span>
              <span style={{ fontSize: 18, color: 'var(--tb-ink-3)' }}>/10</span>
              <Badge tone={loc.finalResult.grade >= 7 ? 'navy' : loc.finalResult.grade >= 4 ? 'amber' : 'grey'} style={{ marginLeft: 8 }}>
                {loc.finalResult.grade >= 7 ? '상위권' : loc.finalResult.grade >= 4 ? '중위권' : '하위권'}
              </Badge>
            </div>
            <div className="wf-mono" style={{ marginBottom: 6 }}>보정계수</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
              <span style={{ fontSize: 36, fontWeight: 700, color: 'var(--tb-navy)', lineHeight: 1 }}>
                ×{loc.finalResult.locationMultiplier.toFixed(2)}
              </span>
              <span className="wf-mono">
                매출 {loc.finalResult.locationMultiplier >= 1 ? '+' : ''}
                {((loc.finalResult.locationMultiplier - 1) * 100).toFixed(0)}% 보정
              </span>
            </div>
          </div>
          <div style={{ marginTop: 16, fontSize: 11.5, color: 'var(--tb-ink-3)' }}>
            ● {loc.locationInfo.areaName || '서울 상권 데이터 적용'}
          </div>
        </Box>
      </div>

      {/* 4 raw metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
        {[
          { t: '유동인구',     raw: fmt.num(loc.rawMetrics.foottraffic),         score: loc.processedScores.displayScore ?? Math.round(loc.processedScores.trafficScore * 100),    from: ds?.trafficFromApi    },
          { t: '주변 매출',    raw: fmt.won(loc.rawMetrics.avgMonthlySales),     score: loc.processedScores.displayScore ?? Math.round(loc.processedScores.salesScore * 100),      from: ds?.salesFromApi       },
          { t: '집객시설',     raw: fmt.num(loc.rawMetrics.facilityCount) + '개', score: loc.processedScores.displayScore ?? 80,                                                    from: ds?.facilityFromApi    },
          { t: '경쟁 업소',    raw: fmt.num(loc.rawMetrics.competitionCount) + '개', score: Math.round(loc.processedScores.competitionScore * 100),                                from: ds?.competitionFromApi },
        ].map((c, i) => (
          <Box key={i} style={{ padding: 16 }}>
            <SketchGauge value={c.score} label={c.t} sub={c.raw} />
            {c.t === '경쟁 업소' && competitionWarn && (
              <div style={{ marginTop: 6, fontSize: 11, color: '#806100' }}>⚠ 다소 포화</div>
            )}
            {c.from === null && (
              <div style={{ marginTop: 6, fontSize: 10.5, color: 'var(--tb-ink-4)', fontStyle: 'italic' }}>
                ⚠️ 외부 API 데이터 미수신 (휴리스틱 추정)
              </div>
            )}
          </Box>
        ))}
      </div>
    </section>
  );
}

// ────────────────────────────────────────────────────────
// Finance Section
// ────────────────────────────────────────────────────────
function FinanceSection({ data }: { data: AnalysisResponse['data'] }) {
  const fin = data.finance.data;
  const sim = data.simulation.data.simulation;
  const ue = fin.unitEconomics;
  const mr = fin.monthlyResult;
  const incremental = data.simulation.data.incremental;

  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: 'var(--tb-card)' }}>
      <div style={{ marginBottom: 16 }}>
        <div className="wf-eyebrow" style={{ marginBottom: 4 }}>SECTION E</div>
        <div className="wf-h2">재무 시뮬레이션</div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: 16, marginBottom: 16 }}>
        <Box style={{ padding: 18 }}>
          <div className="wf-h3" style={{ marginBottom: 6 }}>단위 경제성 (1개당)</div>
          <SketchWaterfall width={420} height={170} steps={[
            { label: '판매가',     value: ue.sellingPrice,            type: 'start', note: fmt.num(ue.sellingPrice) },
            { label: '-식재료',     value: ue.directMaterialCostPerUnit, type: 'neg',   note: '-' + fmt.num(ue.directMaterialCostPerUnit) },
            { label: '-인건비',     value: ue.directLaborCostPerUnit,    type: 'neg',   note: '-' + fmt.num(ue.directLaborCostPerUnit) },
            { label: '-변동간접',   value: ue.variableOverheadPerUnit,    type: 'neg',   note: '-' + fmt.num(ue.variableOverheadPerUnit) },
            { label: '공헌이익',    value: ue.contributionMarginPerUnit,  type: 'end',   note: fmt.num(ue.contributionMarginPerUnit) },
          ]} />
        </Box>
        <Box style={{ padding: 18 }}>
          <div className="wf-h3" style={{ marginBottom: 6 }}>월간 결과</div>
          <SketchWaterfall width={420} height={170} steps={[
            { label: '총매출',     value: Math.round(mr.totalRevenue / 10000),         type: 'start', note: Math.round(mr.totalRevenue / 10000) + '만' },
            { label: '-변동원가',   value: Math.round(mr.totalVariableCost / 10000),     type: 'neg',   note: '-' + Math.round(mr.totalVariableCost / 10000) + '만' },
            { label: '-고정비',     value: Math.round(mr.fixedCostAllocation / 10000),   type: 'neg',   note: '-' + Math.round(mr.fixedCostAllocation / 10000) + '만' },
            { label: '영업이익',    value: Math.round(mr.operatingProfit / 10000),       type: 'end',   note: Math.round(mr.operatingProfit / 10000) + '만' },
          ]} />
        </Box>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, marginBottom: 12 }}>
        {[
          { t: '월 영업이익',  v: fmt.won(mr.operatingProfit),                  sub: '가정값 기준' },
          { t: '이익률',       v: fmt.pct(mr.profitMargin),                     sub: '업계 평균 대비' },
          { t: '식재료 비율',   v: fmt.pct(mr.foodCostRate),                     sub: '권장 30% 이내' },
          { t: 'BEP 일판매량', v: fin.breakEven.breakEvenQuantityPerDay + '개', sub: '월 매출 ' + fmt.won(fin.breakEven.breakEvenRevenuePerMonth) },
        ].map((k, i) => (
          <Box key={i} style={{ padding: 16 }}>
            <div className="wf-mono" style={{ marginBottom: 6 }}>{k.t}</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--tb-navy)' }}>{k.v}</div>
            <div className="wf-mono" style={{ marginTop: 4 }}>{k.sub}</div>
          </Box>
        ))}
      </div>

      {/* 입지·트렌드 반영 시뮬레이션 */}
      <Box style={{ padding: 18, marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
          <div>
            <div className="wf-h3">입지·트렌드 반영 시뮬레이션</div>
            <div className="wf-mono" style={{ marginTop: 4 }}>최종 영업이익 — 위 재무 결과에 입지 보정 × 트렌드 부담 반영</div>
          </div>
          <WarningLevelBadge level={sim.warningLevel} burden={sim.operationBurdenIndex} />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
          {[
            { t: '보정 월매출',     v: fmt.won(sim.adjustedRevenue) },
            { t: '최종 영업이익',   v: fmt.won(sim.totalOperatingProfit) },
            { t: '이익률',          v: fmt.pct(sim.profitMargin) },
            { t: '운영 부담 지수',  v: sim.operationBurdenIndex.toFixed(2) },
          ].map((k, i) => (
            <div key={i} style={{ padding: '12px 16px', background: 'var(--tb-card)', borderRadius: 6 }}>
              <div className="wf-mono" style={{ marginBottom: 4 }}>{k.t}</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--tb-navy)' }}>{k.v}</div>
            </div>
          ))}
        </div>
      </Box>

      {/* incremental (선택 입력) */}
      {incremental && (
        <Box style={{ padding: 16, marginTop: 12, background: 'var(--tb-navy-soft)', borderColor: 'transparent' }}>
          <div className="wf-mono" style={{ marginBottom: 6, color: 'var(--tb-navy)' }}>
            기존 매장 대비 증분 (선택 입력값 기반)
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16 }}>
            {[
              ['증분 영업이익', fmt.won(incremental.additionalOperatingProfit) + '/월'],
              ['증감률',         fmt.pct(incremental.profitGrowthRate)],
            ].map(([t, v], i) => (
              <div key={i}>
                <div className="wf-mono">{t}</div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--tb-navy)' }}>{v}</div>
              </div>
            ))}
          </div>
        </Box>
      )}
    </section>
  );
}

function WarningLevelBadge({ level, burden }: { level: string; burden: number }) {
  const map: Record<string, { tone: 'success' | 'amber' | 'danger'; label: string }> = {
    GREEN:  { tone: 'success', label: '안정' },
    YELLOW: { tone: 'amber',   label: '주의' },
    ORANGE: { tone: 'amber',   label: '경계' },
    RED:    { tone: 'danger',  label: '위험' },
  };
  const m = map[level] || { tone: 'amber' as const, label: level };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <Badge tone={m.tone}>{level} · {m.label}</Badge>
      <span className="wf-mono">운영 부담 {burden.toFixed(2)}</span>
    </div>
  );
}

// ────────────────────────────────────────────────────────
// AI Report Section
// ────────────────────────────────────────────────────────
function AIReportSection({ data, result }: { data: AnalysisResponse['data']; result: LoadedResult }) {
  const router = useRouter();
  const { state } = useUser();
  const noTrend = result.noTrend;

  useEffect(() => {
    if (noTrend) {
      toast.info('실서비스 시 입지/재무만 기반 LLM 프롬프트 분기 필요');
    }
  // 마운트 1회만 실행
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const reportText = noTrend
    ? [
        result.menuName + ' 메뉴는 TrendBite 등록 키워드가 아니어서 트렌드 데이터 없이 분석되었습니다.',
        '',
        '입지 등급 ' + data.location.data.finalResult.grade + '점 · 보정계수 x' +
          data.location.data.finalResult.locationMultiplier.toFixed(2) +
          ' 기반으로 수익성을 평가했습니다. 이익률 ' +
          fmt.pct(data.finance.data.monthlyResult.profitMargin) +
          ' · 일 손익분기 ' + data.finance.data.breakEven.breakEvenQuantityPerDay + '개 달성이 핵심 조건입니다.',
        '',
        '트렌드 리스크를 배제한 입지 · 재무 중심 의사결정을 권장합니다.',
      ].join('\n')
    : data.report.llmReport;

  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 6vw, 80px)', background: '#fff' }}>
      <div style={{ marginBottom: 16 }}>
        <div className="wf-eyebrow" style={{ marginBottom: 4 }}>SECTION F</div>
        <div className="wf-h2">AI 리포트 + 리스크</div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
        <Box style={{ padding: '28px 32px', borderLeft: '4px solid ' + (noTrend ? 'var(--tb-line)' : 'var(--tb-navy)'), borderRadius: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <span style={{ fontSize: 20, color: noTrend ? 'var(--tb-ink-4)' : 'var(--tb-navy)' }}>"</span>
            <span className="wf-mono" style={{ color: noTrend ? 'var(--tb-ink-4)' : 'var(--tb-navy)' }}>
              TRENDBITE AI · LangGraph{noTrend ? ' (입지/재무 기반)' : ''}
            </span>
          </div>
          <p style={{ fontSize: 14.5, lineHeight: 1.75, color: 'var(--tb-ink)', margin: 0, whiteSpace: 'pre-line' }}>
            {reportText}
          </p>
        </Box>
        <Box style={{ padding: 20 }}>
          <div className="wf-h3" style={{ marginBottom: 12 }}>주요 리스크 요인</div>
          <div style={{ display: 'grid', gap: 10 }}>
            {(data.report.riskFactors || []).map((r, i) => (
              <div key={i} style={{
                display: 'grid', gridTemplateColumns: '20px 1fr', gap: 8,
                fontSize: 12.5, lineHeight: 1.5,
              }}>
                <span style={{ color: 'var(--tb-amber)', fontSize: 14 }}>⚠</span>
                <span>{r}</span>
              </div>
            ))}
            {(data.report.riskFactors || []).length === 0 && (
              <span className="wf-mono">감지된 리스크가 없습니다</span>
            )}
          </div>
        </Box>
      </div>

      {/* bottom actions */}
      <div style={{
        display: 'flex', gap: 12, marginTop: 32, justifyContent: 'center',
        flexWrap: 'wrap',
      }}>
        <Link href="/analyze"><Btn lg primary>＋ 다른 메뉴 분석</Btn></Link>
        {state.kind === 'identified' && (
          <Link href="/history"><Btn lg>≡ 이력 보기</Btn></Link>
        )}
        {result.id !== 'sample' && result.id !== 'guest' && result.menuId && (
          <Link href={`/sales-record/${result.menuId}`}>
            <Btn lg>📊 실판매 데이터 기록</Btn>
          </Link>
        )}
        {result.id === 'guest' && (
          <Btn lg onClick={() => alert('🔶 닉네임 입력 후 저장하기 → 추후 데모 모달로 연결')}>
            💾 닉네임 등록하고 저장
          </Btn>
        )}
      </div>
    </section>
  );
}
