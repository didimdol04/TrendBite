'use client';
import { Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Box, Btn, Badge, Chip, fmt } from '@/components/Primitives';
import { SketchLineMulti } from '@/components/Charts';
import { KakaoMapMulti } from '@/components/KakaoMap';
import { NicknameGate } from '@/components/NicknameGate';
import { useUser } from '@/lib/userContext';
import { buildCompareSummary, getMaxIndex, getMinIndex, getDeltaLabel } from '@/lib/compare';
import type { SavedAnalysis } from '@/lib/types';

// ─── 결과 페이지와 공유하는 상수
const VERDICT_TONE: Record<string, 'navy' | 'amber' | 'grey' | 'dark'> = {
  '도입 권장': 'navy', '신중 검토': 'amber', '불확실': 'grey', '보류': 'dark',
};

function hasTrendChart(it: SavedAnalysis): boolean {
  const series = it.response.trend?.interest_series;
  return !it.noTrend && Array.isArray(series) && series.length >= 2;
}

// 시리즈별 색상 (Charts.tsx의 MULTI_COLORS와 동일 순서)
const SERIES_COLORS = ['var(--tb-navy)', 'var(--tb-amber)', 'var(--tb-success)'];
const SERIES_COLOR_FALLBACKS = ['#1A2A6C', '#E0A800', '#16A085'];

// ─── 최고/최저 인덱스 계산 유틸
function bestIdx(nums: (number | null)[], lowerIsBetter = false): number {
  let best = -1;
  let bestVal = lowerIsBetter ? Infinity : -Infinity;
  for (let i = 0; i < nums.length; i++) {
    const v = nums[i];
    if (v == null) continue;
    if (lowerIsBetter ? v < bestVal : v > bestVal) { bestVal = v; best = i; }
  }
  return best;
}

// ─── 비교 표 셀
function CCell({
  value, numValue, bestValue, isHighlight, lowerIsBetter = false,
}: {
  value: string;
  numValue?: number | null;
  bestValue?: number | null;
  isHighlight: boolean;
  lowerIsBetter?: boolean;
}) {
  const delta = (numValue != null && bestValue != null && !isHighlight)
    ? getDeltaLabel(numValue, bestValue)
    : '';
  return (
    <td style={{
      padding: '10px 14px', textAlign: 'center', fontSize: 13.5, fontWeight: isHighlight ? 700 : 400,
      color: isHighlight ? 'var(--tb-navy)' : 'var(--tb-ink)',
      background: isHighlight ? 'var(--tb-navy-soft)' : 'transparent',
      borderBottom: '1px solid var(--tb-line-2)',
      transition: 'background 0.15s',
    }}>
      {value}
      {delta && (
        <span style={{ fontSize: 10, marginLeft: 4, color: delta.startsWith('▼') ? 'var(--tb-danger)' : 'var(--tb-success)', fontWeight: 600 }}>
          {delta}
        </span>
      )}
    </td>
  );
}

// ─── 비교 표 행
function CRow({
  label, values, numValues, lowerIsBetter = false, count,
}: {
  label: string;
  values: string[];
  numValues?: (number | null)[];
  lowerIsBetter?: boolean;
  count: number;
}) {
  const best = numValues ? bestIdx(numValues, lowerIsBetter) : -1;
  const bestVal = best >= 0 && numValues ? numValues[best] : null;
  return (
    <tr>
      <td style={{
        padding: '10px 14px', fontSize: 12, fontWeight: 600,
        color: 'var(--tb-ink-2)', borderBottom: '1px solid var(--tb-line-2)',
        whiteSpace: 'nowrap',
      }}>
        {label}
      </td>
      {Array.from({ length: count }).map((_, i) => (
        <CCell
          key={i}
          value={values[i] ?? '—'}
          numValue={numValues?.[i]}
          bestValue={bestVal}
          isHighlight={best === i}
          lowerIsBetter={lowerIsBetter}
        />
      ))}
    </tr>
  );
}

// ─── 비교 표 컨테이너
function CTable({
  items, rows, lowerIsBetterMap = {},
}: {
  items: SavedAnalysis[];
  rows: { label: string; values: string[]; numValues?: (number | null)[]; lowerIsBetter?: boolean }[];
  lowerIsBetterMap?: Record<string, boolean>;
}) {
  const count = items.length;
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: count === 3 ? 560 : 420 }}>
        <colgroup>
          <col style={{ width: 130 }} />
          {items.map((_, i) => <col key={i} />)}
        </colgroup>
        <thead>
          <tr>
            <th style={{ padding: '8px 14px', background: 'var(--tb-card)', borderBottom: '2px solid var(--tb-line)', textAlign: 'left', fontSize: 11 }} />
            {items.map((it, i) => (
              <th key={i} style={{
                padding: '8px 14px', background: 'var(--tb-card)',
                borderBottom: '2px solid var(--tb-line)',
                textAlign: 'center', fontSize: 11.5, fontWeight: 700,
                color: 'var(--tb-ink-2)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
                  <span style={{
                    display: 'inline-block', width: 8, height: 8, borderRadius: 2,
                    background: SERIES_COLOR_FALLBACKS[i % SERIES_COLOR_FALLBACKS.length],
                  }} />
                  항목 {String.fromCharCode(65 + i)}
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, ri) => (
            <CRow
              key={ri}
              label={row.label}
              values={row.values}
              numValues={row.numValues}
              lowerIsBetter={row.lowerIsBetter ?? lowerIsBetterMap[row.label] ?? false}
              count={count}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── 섹션 헤더
function SectionHead({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <div className="wf-eyebrow" style={{ marginBottom: 4 }}>{eyebrow}</div>
      <div className="wf-h2">{title}</div>
    </div>
  );
}

// ─── 실제 비교 페이지 내용
function CompareContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { state, getAnalysis } = useUser();
  const [showGate, setShowGate] = useState(false);
  const [items, setItems] = useState<SavedAnalysis[]>([]);
  const [loaded, setLoaded] = useState(false);

  const idsParam = searchParams.get('ids') || '';
  const ids = idsParam.split(',').filter(Boolean);

  useEffect(() => {
    if (state.kind !== 'identified') { setShowGate(true); setLoaded(true); return; }
    const loaded = ids.map(id => getAnalysis(id)).filter((a): a is SavedAnalysis => !!a);
    setItems(loaded);
    setLoaded(true);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state, idsParam]);

  // ── 게스트
  if (state.kind !== 'identified') {
    return (
      <>
        <div style={{ padding: 80, textAlign: 'center', maxWidth: 480, margin: '0 auto' }}>
          <div className="wf-h1" style={{ fontSize: 22, marginBottom: 8 }}>닉네임이 필요해요</div>
          <Btn primary lg onClick={() => setShowGate(true)}>닉네임 입력하기</Btn>
        </div>
        <NicknameGate open={showGate} onClose={() => setShowGate(false)} />
      </>
    );
  }

  if (!loaded) {
    return <div style={{ padding: 80, textAlign: 'center' }}>로딩 중...</div>;
  }

  // ── ids 없거나 1개 이하
  if (items.length < 2) {
    return (
      <div style={{ padding: 80, textAlign: 'center', maxWidth: 480, margin: '0 auto' }}>
        <div className="wf-h2" style={{ marginBottom: 12 }}>비교하려면 2개 이상 선택하세요</div>
        <p className="wf-muted" style={{ marginBottom: 24 }}>
          이력 페이지에서 분석 결과를 2~3개 선택한 뒤 비교하기를 눌러주세요.
        </p>
        <Link href="/history"><Btn primary lg>← 이력으로 돌아가기</Btn></Link>
      </div>
    );
  }

  const count = items.length;
  const scores = items.map(i => i.response.report.scores.total);
  const bestScoreIdx = getMaxIndex(scores);
  const categories = new Set(items.map(i => i.category));
  const hasCategoryMismatch = categories.size > 1;
  const summary = buildCompareSummary(items);

  return (
    <div style={{ background: '#fff', minHeight: '100vh' }}>
      {/* 상단 네비 */}
      <div style={{
        padding: '14px clamp(20px, 5vw, 80px)',
        borderBottom: '1px solid var(--tb-line-2)',
        display: 'flex', alignItems: 'center', gap: 12,
        position: 'sticky', top: 0, background: '#fff', zIndex: 50,
      }}>
        <Link href="/history">
          <Btn style={{ height: 32, fontSize: 12 }}>← 이력으로</Btn>
        </Link>
        <span className="wf-eyebrow">분석 결과 비교</span>
        <span className="wf-mono" style={{ marginLeft: 4 }}>{count}개 항목</span>
      </div>

      {/* ── 1. 헤더 카드 (컬럼 정렬) ── */}
      <section style={{ padding: 'clamp(24px, 4vw, 40px) clamp(20px, 5vw, 80px)', background: 'var(--tb-navy)' }}>
        <div className="wf-eyebrow" style={{ color: 'rgba(255,255,255,0.55)', marginBottom: 20 }}>
          COMPARE · 비교 개요
        </div>
        {/* 컬럼 헤더: 각 항목별 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: `140px repeat(${count}, 1fr)`,
          gap: 12,
        }}>
          {/* 빈 레이블 칸 */}
          <div />
          {items.map((it, i) => {
            const isBest = i === bestScoreIdx;
            return (
              <div key={i} style={{
                background: isBest ? 'rgba(224,168,0,0.18)' : 'rgba(255,255,255,0.07)',
                border: isBest ? '1.5px solid rgba(224,168,0,0.5)' : '1px solid rgba(255,255,255,0.12)',
                borderRadius: 12, padding: '16px 18px',
                color: '#fff',
                position: 'relative',
              }}>
                {isBest && (
                  <div style={{
                    position: 'absolute', top: -10, left: '50%', transform: 'translateX(-50%)',
                    background: 'var(--tb-amber)', color: '#1A1100',
                    padding: '2px 10px', borderRadius: 999,
                    fontSize: 11, fontWeight: 700, whiteSpace: 'nowrap',
                  }}>
                    👑 최적
                  </div>
                )}
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                  <span style={{
                    display: 'inline-block', width: 10, height: 10, borderRadius: 2,
                    background: SERIES_COLOR_FALLBACKS[i % SERIES_COLOR_FALLBACKS.length],
                    flexShrink: 0,
                  }} />
                  <span style={{ fontSize: 11.5, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>
                    항목 {String.fromCharCode(65 + i)}
                  </span>
                </div>
                <div style={{ fontSize: 17, fontWeight: 700, marginBottom: 4, lineHeight: 1.3 }}>
                  {it.menuName}
                </div>
                <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.55)', marginBottom: 10, lineHeight: 1.5 }}>
                  {it.address || '주소 없음'}<br />
                  {it.createdAt.slice(0, 10)}
                </div>
                <div style={{ marginBottom: 10 }}>
                  <Badge tone={VERDICT_TONE[it.response.report.verdict] || 'grey'}>
                    {it.response.report.verdict}
                  </Badge>
                </div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                  <span style={{ fontSize: 36, fontWeight: 800, color: isBest ? 'var(--tb-amber)' : '#fff', lineHeight: 1 }}>
                    {it.response.report.scores.total}
                  </span>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>/100</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* 행 레이블 + 값 (모바일용 추가 정보) */}
        <div style={{ marginTop: 16, display: 'grid', gridTemplateColumns: `140px repeat(${count}, 1fr)`, gap: 4 }}>
          {[
            { label: '메뉴명', values: items.map(i => i.menuName) },
            { label: '주소', values: items.map(i => (i.address || '주소 없음').slice(0, 20)) },
            { label: '분석일', values: items.map(i => i.createdAt.slice(0, 10)) },
          ].flatMap(row => {
            const allSame = row.values.every(v => v === row.values[0]);
            return [
              <div key={`lbl-${row.label}`} style={{ color: 'rgba(255,255,255,0.4)', fontSize: 11, padding: '4px 0', alignSelf: 'center' }}>
                {row.label}
              </div>,
              ...row.values.map((v, i) => (
                <div key={`val-${row.label}-${i}`} style={{
                  fontSize: 11, padding: '4px 8px',
                  color: allSame ? 'rgba(255,255,255,0.35)' : 'rgba(255,255,255,0.75)',
                  fontFamily: row.label === '분석일' ? 'var(--tb-font-mono)' : undefined,
                  borderRadius: 4,
                  background: !allSame && i === (row.label === '주소' ? -1 : bestScoreIdx) ? 'rgba(255,255,255,0.05)' : 'transparent',
                }}>
                  {v}
                </div>
              )),
            ];
          })}
        </div>
      </section>

      {/* ── 2. 비교 요약 ── */}
      {summary && (
        <section style={{ padding: '0 clamp(20px, 5vw, 80px)', marginTop: -1 }}>
          <div style={{
            background: 'var(--tb-amber-soft)',
            border: '1px solid rgba(224,168,0,0.4)',
            borderTop: 'none',
            padding: '16px 22px',
            fontSize: 14, color: '#5A3D00', lineHeight: 1.65,
            fontWeight: 500,
          }}>
            <span style={{ fontWeight: 800, color: '#806100', marginRight: 8 }}>⚡ 핵심 차이</span>
            {summary}
          </div>
        </section>
      )}

      {/* ── 3. 점수 비교 ── */}
      <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 5vw, 80px)', background: '#fff' }}>
        <SectionHead eyebrow="SECTION A" title="점수 비교" />
        {hasCategoryMismatch && (
          <div style={{ marginBottom: 12 }}>
            <Chip tone="amber">⚠ 카테고리가 달라 해석에 주의하세요</Chip>
          </div>
        )}
        <Box style={{ overflow: 'hidden', padding: 0 }}>
          <CTable
            items={items}
            rows={[
              {
                label: '트렌드 점수',
                values: items.map(i => i.noTrend ? 'N/A' : String(i.response.report.scores.trend)),
                numValues: items.map(i => i.noTrend ? null : i.response.report.scores.trend),
              },
              {
                label: '수익성 점수',
                values: items.map(i => String(i.response.report.scores.profitability)),
                numValues: items.map(i => i.response.report.scores.profitability),
              },
              {
                label: '지속성 점수',
                values: items.map(i => String(i.response.report.scores.sustainability)),
                numValues: items.map(i => i.response.report.scores.sustainability),
              },
              {
                label: '종합 점수',
                values: items.map(i => String(i.response.report.scores.total)),
                numValues: items.map(i => i.response.report.scores.total),
              },
            ]}
          />
        </Box>

        {/* 2개일 때: 총점 차이 강조 */}
        {count === 2 && (
          <div style={{
            marginTop: 12, padding: '10px 16px',
            background: 'var(--tb-navy-soft)', borderRadius: 'var(--tb-radius-sm)',
            fontSize: 12.5, color: 'var(--tb-navy)', fontWeight: 600,
          }}>
            종합 점수 차이: {Math.abs(scores[0] - scores[1])}점
            {' '}({scores[0] > scores[1] ? '항목 A 우위' : '항목 B 우위'})
          </div>
        )}
      </section>

      {/* ── 4. 입지 비교 ── */}
      <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 5vw, 80px)', background: 'var(--tb-card)' }}>
        <SectionHead eyebrow="SECTION B" title="입지 비교" />

        {/* 지도 (핀 여러 개) */}
        <Box style={{ padding: 12, marginBottom: 16 }}>
          <KakaoMapMulti
            pins={items.map((it, i) => ({
              lat: it.response.location.data.locationInfo.lat,
              lng: it.response.location.data.locationInfo.lng,
              label: `항목 ${String.fromCharCode(65 + i)}`,
            }))}
            height={240}
          />
        </Box>

        <Box style={{ overflow: 'hidden', padding: 0 }}>
          <CTable
            items={items}
            rows={[
              {
                label: '입지계수',
                values: items.map(i => `×${i.response.location.data.finalResult.locationMultiplier.toFixed(2)}`),
                numValues: items.map(i => i.response.location.data.finalResult.locationMultiplier),
              },
              {
                label: '등급 (1~10)',
                values: items.map(i => `${i.response.location.data.finalResult.grade}/10`),
                numValues: items.map(i => i.response.location.data.finalResult.grade),
              },
              {
                label: '유동인구',
                values: items.map(i => fmt.num(i.response.location.data.rawMetrics.foottraffic)),
                numValues: items.map(i => i.response.location.data.rawMetrics.foottraffic),
              },
              {
                label: '월평균매출',
                values: items.map(i => fmt.won(i.response.location.data.rawMetrics.avgMonthlySales)),
                numValues: items.map(i => i.response.location.data.rawMetrics.avgMonthlySales),
              },
              {
                label: '경쟁점포수',
                values: items.map(i => `${i.response.location.data.rawMetrics.competitionCount}개`),
                numValues: items.map(i => i.response.location.data.rawMetrics.competitionCount),
                lowerIsBetter: true,
              },
            ]}
          />
        </Box>
      </section>

      {/* ── 5. 트렌드 비교 ── */}
      <TrendCompareSection items={items} count={count} />

      {/* ── 6. 재무 비교 ── */}
      <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 5vw, 80px)', background: 'var(--tb-card)' }}>
        <SectionHead eyebrow="SECTION D" title="재무 비교" />
        <Box style={{ overflow: 'hidden', padding: 0 }}>
          <CTable
            items={items}
            rows={[
              {
                label: '객단가',
                values: items.map(i => fmt.won(i.response.finance.data.unitEconomics.sellingPrice)),
                numValues: items.map(i => i.response.finance.data.unitEconomics.sellingPrice),
              },
              {
                label: '공헌이익/개',
                values: items.map(i => fmt.won(i.response.finance.data.unitEconomics.contributionMarginPerUnit)),
                numValues: items.map(i => i.response.finance.data.unitEconomics.contributionMarginPerUnit),
              },
              {
                label: '월 영업이익',
                values: items.map(i => fmt.won(i.response.finance.data.monthlyResult.operatingProfit)),
                numValues: items.map(i => i.response.finance.data.monthlyResult.operatingProfit),
              },
              {
                label: '이익률',
                values: items.map(i => fmt.pct(i.response.finance.data.monthlyResult.profitMargin)),
                numValues: items.map(i => i.response.finance.data.monthlyResult.profitMargin),
              },
              {
                label: 'BEP 일판매량',
                values: items.map(i => `${i.response.finance.data.breakEven.breakEvenQuantityPerDay}개`),
                numValues: items.map(i => i.response.finance.data.breakEven.breakEvenQuantityPerDay),
                lowerIsBetter: true,
              },
            ]}
          />
        </Box>
      </section>

      {/* 하단 액션 */}
      <div style={{ padding: 'clamp(24px, 4vw, 40px) clamp(20px, 5vw, 80px)', display: 'flex', gap: 12, flexWrap: 'wrap', justifyContent: 'center' }}>
        <Link href="/history"><Btn lg>← 이력으로 돌아가기</Btn></Link>
        <Link href="/analyze"><Btn lg primary>＋ 새 분석</Btn></Link>
      </div>
    </div>
  );
}

// ─── 트렌드 비교 섹션 (별도 컴포넌트 — noTrend 혼합 처리)
function TrendCompareSection({ items, count }: { items: SavedAnalysis[]; count: number }) {
  const withTrend = items.filter(hasTrendChart);
  const allNoTrend = withTrend.length === 0;

  return (
    <section style={{ padding: 'clamp(24px, 4vw, 32px) clamp(20px, 5vw, 80px)', background: '#fff' }}>
      <SectionHead eyebrow="SECTION C" title="트렌드 비교" />

      {allNoTrend ? (
        <Box style={{ padding: '32px 24px', textAlign: 'center' }}>
          <div className="wf-h3" style={{ marginBottom: 6 }}>비교 항목 모두 트렌드 데이터가 없습니다</div>
          <p className="wf-muted" style={{ fontSize: 12.5 }}>등록된 키워드 메뉴만 관심도 곡선이 표시됩니다.</p>
        </Box>
      ) : (
        <>
          {/* 일부 noTrend 혼합 안내 */}
          {withTrend.length < count && (
            <div style={{ marginBottom: 12 }}>
              <Chip tone="amber">⚠ 일부 항목은 트렌드 데이터가 없어 차트에서 제외됩니다</Chip>
            </div>
          )}

          {/* 다중 시리즈 라인 차트 */}
          <Box style={{ padding: 18, marginBottom: 16 }}>
            <div className="wf-h3" style={{ marginBottom: 12 }}>
              월별 관심도 비교 <span className="wf-mono">네이버 데이터랩</span>
            </div>
            {/* 범례 */}
            <div style={{ display: 'flex', gap: 16, marginBottom: 10, flexWrap: 'wrap' }}>
              {items.map((it, i) => {
                const hasChart = hasTrendChart(it);
                return (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6, opacity: hasChart ? 1 : 0.35 }}>
                    <svg width={24} height={8}>
                      <line
                        x1={0} y1={4} x2={24} y2={4}
                        stroke={SERIES_COLOR_FALLBACKS[i % SERIES_COLOR_FALLBACKS.length]}
                        strokeWidth={2}
                        strokeDasharray={i === 0 ? undefined : i === 1 ? '6 3' : '3 3'}
                      />
                    </svg>
                    <span style={{ fontSize: 11.5, color: 'var(--tb-ink-2)', fontWeight: 600 }}>
                      항목 {String.fromCharCode(65 + i)} · {it.menuName}
                      {!hasChart && ' (데이터 없음)'}
                    </span>
                  </div>
                );
              })}
            </div>
            <div style={{ width: '100%', overflowX: 'auto' }}>
              <SketchLineMulti
                width={Math.max(380, withTrend[0]?.response.trend.interest_series?.length ?? 12) * 12}
                height={160}
                series={items.map((it, i) => ({
                  data: hasTrendChart(it) ? (it.response.trend.interest_series ?? []) : [],
                  name: `항목 ${String.fromCharCode(65 + i)}`,
                  color: SERIES_COLORS[i % SERIES_COLORS.length],
                }))}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
              <span className="wf-mono">과거</span>
              <span className="wf-mono">현재</span>
            </div>
          </Box>

          {/* 트렌드 지표 표 */}
          <Box style={{ overflow: 'hidden', padding: 0 }}>
            <CTable
              items={items}
              rows={[
                {
                  label: 'PLC 단계',
                  values: items.map(i => i.noTrend ? '트렌드 미포함' : (i.response.trend.plc_stage || 'N/A')),
                },
                {
                  label: '트렌드 점수',
                  values: items.map(i => i.noTrend ? '—' : String(i.response.trend.trend_score)),
                  numValues: items.map(i => i.noTrend ? null : i.response.trend.trend_score),
                },
                {
                  label: '성장률',
                  values: items.map(i => i.noTrend ? '—' : i.response.trend.growth_rate.toFixed(2)),
                  numValues: items.map(i => i.noTrend ? null : i.response.trend.growth_rate),
                },
                {
                  label: '최신 관심도',
                  values: items.map(i => i.noTrend ? '—' : i.response.trend.latest_interest.toFixed(1)),
                  numValues: items.map(i => i.noTrend ? null : i.response.trend.latest_interest),
                },
                {
                  label: '전체 추세',
                  values: items.map(i => i.noTrend ? '—' : i.response.trend.overall_trend),
                },
              ]}
            />
          </Box>
        </>
      )}
    </section>
  );
}

// ─── 기본 내보내기 — Suspense 래퍼 (useSearchParams 요건)
export default function ComparePage() {
  return (
    <Suspense fallback={
      <div style={{ padding: 80, textAlign: 'center' }}>비교 데이터 로딩 중...</div>
    }>
      <CompareContent />
    </Suspense>
  );
}
