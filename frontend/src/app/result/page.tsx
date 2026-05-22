'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '@/lib/api';
import type { AnalysisResponse, LifecycleMeta } from '@/lib/types';

const VERDICT_COLOR: Record<string, string> = {
  '도입 권장': 'bg-emerald-600 text-white',
  '신중 검토': 'bg-amber-500 text-white',
  '불확실': 'bg-gray-500 text-white',
  '보류': 'bg-red-600 text-white',
};

const WARNING_COLOR: Record<string, { bg: string; label: string }> = {
  GREEN: { bg: 'bg-emerald-500', label: '안정' },
  YELLOW: { bg: 'bg-yellow-400', label: '주의' },
  ORANGE: { bg: 'bg-orange-500', label: '경계' },
  RED: { bg: 'bg-red-600', label: '위험' },
};

export default function Result() {
  const router = useRouter();
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [lifecycles, setLifecycles] = useState<LifecycleMeta[]>([]);

  useEffect(() => {
    const raw = sessionStorage.getItem('trendbite:result');
    if (!raw) { router.replace('/analyze'); return; }
    setResult(JSON.parse(raw));
    api.lifecycles().then(setLifecycles).catch(() => { /* lifecycle 메타는 실패해도 화면은 떠야 함 */ });
  }, [router]);

  const lifecycleMap = useMemo(() =>
    Object.fromEntries(lifecycles.map(l => [l.name, l])), [lifecycles]);

  if (!result) return <main className="min-h-screen flex items-center justify-center">로딩…</main>;

  const d = result.data;
  const trend = d.trend;
  const fin = d.finance.data;
  const loc = d.location.data;
  const sim = d.simulation.data.simulation;
  const rep = d.report;
  const verdictCls = VERDICT_COLOR[rep.verdict] ?? 'bg-navy text-white';
  const warn = WARNING_COLOR[sim.warningLevel] ?? WARNING_COLOR.GREEN;

  return (
    <main className="min-h-screen bg-[#F5F7FA] pb-16">
      <header className="bg-navy text-white px-8 py-4 flex justify-between items-center">
        <Link href="/" className="font-bold">TrendBite</Link>
        <div className="flex gap-2">
          <Link href="/analyze" className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded text-sm">다른 메뉴 분석</Link>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-6 pt-8">
        {/* Verdict Hero */}
        <div className="bg-white rounded-2xl shadow-sm p-8 mb-6 flex items-center gap-8">
          <div className="flex-1">
            <div className={`inline-block px-5 py-2 rounded-full text-lg font-bold mb-3 ${verdictCls}`}>{rep.verdict}</div>
            <h1 className="text-3xl font-extrabold text-navy mb-2">{result.data.trend.recommendation}</h1>
            <p className="text-gray-600">메뉴 도입 의사결정을 위한 데이터 기반 분석 결과입니다.</p>
          </div>
          <div className="text-center">
            <ScoreDonut value={rep.scores.total} />
            <div className="text-xs text-gray-500 mt-1">종합 점수</div>
          </div>
        </div>

        {/* Score breakdown */}
        <div className="grid grid-cols-4 gap-4 mb-6">
          {[
            { label: '트렌드',     v: rep.scores.trend },
            { label: '수익성',     v: rep.scores.profitability },
            { label: '지속성',     v: rep.scores.sustainability },
            { label: '종합',       v: rep.scores.total, accent: true },
          ].map(({ label, v, accent }) => (
            <div key={label} className={`rounded-xl p-4 ${accent ? 'bg-navy text-white' : 'bg-white'}`}>
              <div className={`text-xs mb-1 ${accent ? 'text-white/60' : 'text-gray-500'}`}>{label}</div>
              <div className="text-3xl font-bold mb-2">{v}</div>
              <div className={`h-1.5 rounded-full ${accent ? 'bg-white/20' : 'bg-gray-100'}`}>
                <div className={`h-full rounded-full ${accent ? 'bg-gold' : 'bg-navy'}`} style={{ width: `${Math.min(100, v)}%` }} />
              </div>
            </div>
          ))}
        </div>

        {/* Trend section */}
        <Section title="트렌드 분석">
          <div className="flex flex-wrap gap-2 mb-4">
            <Chip>PLC 단계: <b>{trend.plc_stage}</b></Chip>
            <Chip>전체 추세: {trend.overall_trend}</Chip>
            <Chip>최근: {trend.recent_trend}</Chip>
            <Chip>속도: {trend.trend_speed}</Chip>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
            <ChartCard title="14개월 관심도 추이">
              <InterestSeriesChart series={trend.interest_series} />
            </ChartCard>
            <ChartCard title="제품 수명 주기 (PLC)">
              <PlcCurve currentStage={trend.plc_stage} />
            </ChartCard>
          </div>
          <div className="grid grid-cols-4 gap-3 mb-5">
            <Kpi label="최신 관심도"   value={trend.latest_interest.toFixed(1)} />
            <Kpi label="평균 관심도"   value={trend.average_interest.toFixed(1)} />
            <Kpi label="정점 대비"     value={`${Math.round(trend.peak_ratio * 100)}%`} />
            <Kpi label="추세 기울기"   value={trend.growth_rate.toFixed(2)} />
          </div>
          <div className="text-sm font-semibold mb-2">유사한 과거 사례 Top 3</div>
          <div className="grid grid-cols-3 gap-3">
            {trend.similar_patterns.slice(0, 3).map((sp, i) => {
              const meta = lifecycleMap[sp.lifecycle];
              return (
                <div key={i} className="border border-gray-200 rounded-lg p-4">
                  <div className="font-semibold mb-2">{sp.name}</div>
                  <div className="inline-block px-2 py-0.5 rounded text-xs text-white mb-2" style={{ background: meta?.color ?? '#7F8C8D' }}>
                    {sp.lifecycle}
                  </div>
                  <div className="text-2xl font-bold text-navy">{Math.round(sp.dtw_similarity * 100)}%</div>
                  <div className="text-xs text-gray-500">유사도</div>
                  {meta?.description && <div className="text-xs text-gray-600 mt-2">{meta.description}</div>}
                </div>
              );
            })}
          </div>
        </Section>

        {/* Location section */}
        <Section title="입지 분석">
          <div className="flex items-end gap-8 mb-5">
            <div>
              <div className="text-xs text-gray-500">보정계수</div>
              <div className="text-4xl font-extrabold text-navy">{loc.finalResult.locationMultiplier.toFixed(2)}</div>
            </div>
            <div>
              <div className="text-xs text-gray-500">등급</div>
              <div className="text-4xl font-extrabold text-navy">{loc.finalResult.grade}<span className="text-lg text-gray-400">/10</span></div>
            </div>
            <div className="text-sm text-gray-600">
              <div>{loc.locationInfo.address}</div>
              {loc.locationInfo.areaName && <div className="text-gray-500">{loc.locationInfo.areaName} · {loc.locationInfo.dongName}</div>}
            </div>
          </div>
          <div className="grid grid-cols-4 gap-3">
            <Kpi label="유동인구"     value={loc.rawMetrics.foottraffic.toLocaleString()} />
            <Kpi label="주변 매출"     value={loc.rawMetrics.avgMonthlySales.toLocaleString()} />
            <Kpi label="집객시설"     value={loc.rawMetrics.facilityCount.toLocaleString()} />
            <Kpi label="경쟁 점포"     value={loc.rawMetrics.competitionCount.toLocaleString()} />
          </div>
          {loc.message && <div className="mt-3 text-xs text-gray-500">{loc.message}</div>}
        </Section>

        {/* Finance section */}
        <Section title="재무 시뮬레이션">
          <div className="grid grid-cols-4 gap-3 mb-5">
            <Kpi label="월 영업이익"   value={`${fin.monthlyResult.operatingProfit.toLocaleString()}원`} />
            <Kpi label="이익률"         value={`${Math.round(fin.monthlyResult.profitMargin * 100)}%`} />
            <Kpi label="BEP (일)"      value={`${fin.breakEven.breakEvenQuantityPerDay}개`} />
            <Kpi label="식재료비율"   value={`${Math.round(fin.monthlyResult.foodCostRate * 100)}%`} />
          </div>
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="text-sm font-semibold mb-2">단위 경제성</div>
            <div className="grid grid-cols-3 gap-3 text-sm">
              <div><span className="text-gray-500">판매가</span> {fin.unitEconomics.sellingPrice.toLocaleString()}원</div>
              <div><span className="text-gray-500">변동원가</span> {fin.unitEconomics.totalVariableCostPerUnit.toLocaleString()}원</div>
              <div className="font-semibold text-emerald-700"><span className="text-gray-500 font-normal">공헌이익</span> {fin.unitEconomics.contributionMarginPerUnit.toLocaleString()}원</div>
            </div>
          </div>
        </Section>

        {/* Simulation 신호등 */}
        <Section title="입지·트렌드 반영 시뮬레이션">
          <div className="flex items-center gap-6 mb-4">
            <div className={`w-20 h-20 rounded-full ${warn.bg} flex items-center justify-center text-white font-bold text-lg`}>
              {warn.label}
            </div>
            <div>
              <div className="text-sm text-gray-500">경고 레벨</div>
              <div className="text-2xl font-bold">{sim.warningLevel}</div>
              <div className="text-sm text-gray-600">운영 부담 지수 {sim.operationBurdenIndex}</div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Kpi label="보정 월매출"        value={`${sim.adjustedRevenue.toLocaleString()}원`} />
            <Kpi label="최종 영업이익"      value={`${sim.totalOperatingProfit.toLocaleString()}원`} />
            <Kpi label="이익률"              value={`${Math.round(sim.profitMargin * 100)}%`} />
          </div>
        </Section>

        {/* LLM Report */}
        <Section title="AI 종합 리포트">
          <div className="border-l-4 border-navy bg-gray-50 p-5 rounded mb-5 leading-relaxed whitespace-pre-line">
            {rep.llm_report}
          </div>
          <div className="text-sm font-semibold mb-2">⚠️ 주요 리스크 요인</div>
          <ul className="space-y-1">
            {rep.risk_factors.map((r, i) => (
              <li key={i} className="text-sm flex gap-2"><span className="text-amber-600">●</span> {r}</li>
            ))}
          </ul>
        </Section>

        {/* CTA */}
        <div className="text-center mt-8">
          <Link href="/analyze" className="inline-block bg-navy text-white px-8 py-3 rounded-lg font-semibold hover:bg-navy-dark transition">
            다른 메뉴 분석하기
          </Link>
        </div>
      </div>
    </main>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="bg-white rounded-xl shadow-sm p-6 mb-5">
      <h2 className="text-lg font-bold mb-4 text-navy">{title}</h2>
      {children}
    </section>
  );
}

function Chip({ children }: { children: React.ReactNode }) {
  return <span className="inline-block px-3 py-1 bg-gray-100 rounded-full text-xs">{children}</span>;
}

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-gray-50 rounded-lg p-3">
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      <div className="text-lg font-bold text-navy">{value}</div>
    </div>
  );
}

function ScoreDonut({ value }: { value: number }) {
  const r = 36, c = 2 * Math.PI * r;
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <svg viewBox="0 0 100 100" className="w-24 h-24">
      <circle cx="50" cy="50" r={r} fill="none" stroke="#e5e7eb" strokeWidth="10" />
      <circle cx="50" cy="50" r={r} fill="none" stroke="#1A2A6C" strokeWidth="10"
        strokeDasharray={c} strokeDashoffset={c * (1 - clamped / 100)}
        transform="rotate(-90 50 50)" strokeLinecap="round" />
      <text x="50" y="56" textAnchor="middle" fontSize="22" fontWeight="800" fill="#1A2A6C">{clamped}</text>
    </svg>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="border border-gray-200 rounded-lg p-4">
      <div className="text-xs font-semibold text-gray-500 mb-2">{title}</div>
      {children}
    </div>
  );
}

function InterestSeriesChart({ series }: { series?: number[] }) {
  if (!series || series.length === 0) {
    return (
      <div className="h-[160px] flex items-center justify-center text-xs text-gray-400">
        시계열 데이터 없음
        <div className="text-[10px] mt-1 text-gray-300">FastAPI interest_series 필드 필요</div>
      </div>
    );
  }

  const n = series.length;
  // 가장 마지막이 "최근", 그 이전은 -N 개월 라벨
  const data = series.map((value, i) => ({
    label: i === n - 1 ? '최근' : `-${n - 1 - i}m`,
    value: Number(value.toFixed(1)),
  }));

  return (
    <ResponsiveContainer width="100%" height={160}>
      <LineChart data={data} margin={{ top: 5, right: 10, bottom: 0, left: -20 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis dataKey="label" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
        <YAxis tick={{ fontSize: 10 }} domain={[0, 100]} />
        <Tooltip
          contentStyle={{ fontSize: 12, borderRadius: 8 }}
          formatter={(v: number) => [v, '관심도']}
        />
        <Line type="monotone" dataKey="value" stroke="#1A2A6C" strokeWidth={2.5}
          dot={{ r: 2.5, fill: '#1A2A6C' }} activeDot={{ r: 5, fill: '#E0A800' }} />
      </LineChart>
    </ResponsiveContainer>
  );
}

// PLC 단계별 현재 위치 좌표 (viewBox 400×220 기준)
// y 좌표는 SVG path "M 20 180 Q 90 180, 110 130 T 200 50 T 290 50 T 380 180"
// 의 베지어 경로 위 정확한 점을 계산해 박아둠 — 곡선 위에 마커가 얹히도록.
const PLC_MARKERS: Record<string, { x: number; y: number }> = {
  '도입기': { x: 70,  y: 171 },
  '성장기': { x: 150, y: 79  },
  '성숙기': { x: 250, y: 35  },
  '쇠퇴기': { x: 350, y: 137 },
};

function PlcCurve({ currentStage }: { currentStage: string }) {
  const marker = PLC_MARKERS[currentStage];

  return (
    <svg viewBox="0 0 400 220" className="w-full h-[160px]">
      {/* baseline */}
      <line x1="20" y1="180" x2="380" y2="180" stroke="#e5e7eb" strokeWidth="1" />

      {/* PLC curve — bell-ish lifecycle */}
      <path
        d="M 20 180 Q 90 180, 110 130 T 200 50 T 290 50 T 380 180"
        fill="none" stroke="#1A2A6C" strokeWidth="2.5"
      />

      {/* 단계 구분선 (점선) */}
      {[100, 200, 300].map(x => (
        <line key={x} x1={x} y1="20" x2={x} y2="180"
          stroke="#e5e7eb" strokeWidth="1" strokeDasharray="3 3" />
      ))}

      {/* 단계 라벨 */}
      <text x="50"  y="205" textAnchor="middle" fontSize="11" fill="#6b7280">도입기</text>
      <text x="150" y="205" textAnchor="middle" fontSize="11" fill="#6b7280">성장기</text>
      <text x="250" y="205" textAnchor="middle" fontSize="11" fill="#6b7280">성숙기</text>
      <text x="350" y="205" textAnchor="middle" fontSize="11" fill="#6b7280">쇠퇴기</text>

      {/* 현재 단계 마커 */}
      {marker && (
        <g>
          <circle cx={marker.x} cy={marker.y} r="9" fill="#E0A800" stroke="white" strokeWidth="3" />
          <text x={marker.x} y={marker.y - 16} textAnchor="middle" fontSize="11"
            fontWeight="700" fill="#1A2A6C">현재</text>
        </g>
      )}
    </svg>
  );
}
