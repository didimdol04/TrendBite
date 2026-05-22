'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import { api } from '@/lib/api';
import type { AnalysisInput, EmployeeInput } from '@/lib/types';

const CATEGORIES = [
  'dining', 'cafe', 'bakery', 'snack', 'dessert', 'fastfood', 'chicken', 'bar',
];

const STEPS = ['메뉴', '입지', '인건비', '고정비', '판매 가정'];

const DEFAULT_INPUT: AnalysisInput = {
  keyword: '',
  category: 'dining',
  location: { address: '서울특별시 강남구 학동로', lat: 37.5172, lng: 127.0473 },
  finance: {
    menuInfo: { menuName: '', category: 'dining', sellingPrice: 12000, packagingCostPerUnit: 300 },
    directMaterialCost: { ingredientCostPerUnit: 4000 },
    directLaborCost: {
      bohTimeMinutes: 5, fohTimeMinutes: 3,
      employees: [{ name: '사장', mode: 'simple', monthlyTotal: 2500000 }],
    },
    variableOverheadCostPerUnit: 500,
    fixedCost: { monthlyRent: 1500000, managementFee: 200000, marketingCost: 100000, miscCost: 100000 },
    salesAssumption: { expectedDailySales: 80, workingDaysPerWeek: 6, trendDurationWeeks: 12, dailyWorkHours: 10 },
  },
};

export default function AnalyzePage() {
  return (
    <Suspense fallback={<main className="min-h-screen flex items-center justify-center text-gray-500">로딩…</main>}>
      <Analyze />
    </Suspense>
  );
}

function Analyze() {
  const router = useRouter();
  const params = useSearchParams();
  const [step, setStep] = useState(0);
  const [input, setInput] = useState<AnalysisInput>(DEFAULT_INPUT);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadingStage, setLoadingStage] = useState(0);

  // 인기 키워드 칩에서 들어온 경우 사전 채움
  useEffect(() => {
    const k = params.get('keyword');
    const c = params.get('category');
    if (k || c) {
      setInput(prev => ({
        ...prev,
        keyword: k ?? prev.keyword,
        category: c ?? prev.category,
        finance: { ...prev.finance, menuInfo: { ...prev.finance.menuInfo, menuName: k ?? prev.finance.menuInfo.menuName, category: c ?? prev.finance.menuInfo.category } },
      }));
    }
  }, [params]);

  // 로딩 시 가짜 단계 진행 (실제 백엔드는 단일 호출이지만 UX 용)
  useEffect(() => {
    if (!loading) return;
    setLoadingStage(0);
    const timers = [
      setTimeout(() => setLoadingStage(1), 1500),
      setTimeout(() => setLoadingStage(2), 3500),
      setTimeout(() => setLoadingStage(3), 6000),
    ];
    return () => timers.forEach(clearTimeout);
  }, [loading]);

  const update = (patch: Partial<AnalysisInput>) =>
    setInput(prev => ({ ...prev, ...patch }));

  const updateFinance = (patch: Partial<AnalysisInput['finance']>) =>
    setInput(prev => ({ ...prev, finance: { ...prev.finance, ...patch } }));

  const submit = async () => {
    setLoading(true); setError(null);
    try {
      const resp = await api.run(input);
      sessionStorage.setItem('trendbite:input', JSON.stringify(input));
      sessionStorage.setItem('trendbite:result', JSON.stringify(resp));
      router.push('/result');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
      setLoading(false);
    }
  };

  const canNext = (() => {
    if (step === 0) return input.keyword.trim() !== '' && input.category.trim() !== '' && input.finance.menuInfo.sellingPrice > 0;
    return true;
  })();

  return (
    <main className="min-h-screen bg-[#F5F7FA]">
      <header className="bg-navy text-white px-8 py-4 flex justify-between items-center">
        <Link href="/" className="font-bold">TrendBite</Link>
        <div className="text-sm text-white/70">{STEPS[step]} ({step + 1}/{STEPS.length})</div>
      </header>

      <div className="max-w-3xl mx-auto p-6">
        <div className="flex gap-2 mb-6">
          {STEPS.map((_, i) => (
            <div key={i} className={`h-2 flex-1 rounded ${i <= step ? 'bg-navy' : 'bg-gray-200'}`} />
          ))}
        </div>

        <div className="bg-white rounded-xl shadow-sm p-8 mb-6">
          {step === 0 && <Step1 input={input} update={update} updateFinance={updateFinance} />}
          {step === 1 && <Step2 input={input} update={update} />}
          {step === 2 && <Step3 input={input} updateFinance={updateFinance} />}
          {step === 3 && <Step4 input={input} updateFinance={updateFinance} />}
          {step === 4 && <Step5 input={input} updateFinance={updateFinance} />}
        </div>

        <div className="flex justify-between">
          <button onClick={() => setStep(s => Math.max(0, s - 1))} disabled={step === 0}
            className="px-6 py-3 rounded-lg border border-gray-300 disabled:opacity-40">이전</button>
          {step < STEPS.length - 1 ? (
            <button onClick={() => setStep(s => s + 1)} disabled={!canNext}
              className="px-6 py-3 rounded-lg bg-navy text-white font-semibold disabled:opacity-40">다음 →</button>
          ) : (
            <button onClick={submit} disabled={loading}
              className="px-8 py-3 rounded-lg bg-gold text-navy font-bold disabled:opacity-40">
              {loading ? '분석 중…' : '분석 시작'}
            </button>
          )}
        </div>

        {error && <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded text-red-700 text-sm">{error}</div>}
      </div>

      {loading && <LoadingOverlay stage={loadingStage} />}
    </main>
  );
}

// ── Steps ────────────────────────────────────────────────────────────

function Field({ label, children, hint }: { label: string; children: React.ReactNode; hint?: string }) {
  return (
    <label className="block mb-4">
      <div className="text-sm font-semibold mb-1">{label}</div>
      {children}
      {hint && <div className="text-xs text-gray-500 mt-1">{hint}</div>}
    </label>
  );
}

const inputCls = 'w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-navy';

function Step1({ input, update, updateFinance }: {
  input: AnalysisInput;
  update: (p: Partial<AnalysisInput>) => void;
  updateFinance: (p: Partial<AnalysisInput['finance']>) => void;
}) {
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">① 메뉴 정보</h2>
      <Field label="메뉴명">
        <input className={inputCls} value={input.keyword}
          onChange={e => { const v = e.target.value; update({ keyword: v }); updateFinance({ menuInfo: { ...input.finance.menuInfo, menuName: v } }); }}
          placeholder="예: 마라탕" />
      </Field>
      <Field label="카테고리">
        <select className={inputCls} value={input.category}
          onChange={e => { const v = e.target.value; update({ category: v }); updateFinance({ menuInfo: { ...input.finance.menuInfo, category: v } }); }}>
          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
      </Field>
      <div className="grid grid-cols-2 gap-4">
        <Field label="판매가 (원)">
          <input type="number" className={inputCls} value={input.finance.menuInfo.sellingPrice}
            onChange={e => updateFinance({ menuInfo: { ...input.finance.menuInfo, sellingPrice: +e.target.value } })} />
        </Field>
        <Field label="포장재비 (원/개)">
          <input type="number" className={inputCls} value={input.finance.menuInfo.packagingCostPerUnit}
            onChange={e => updateFinance({ menuInfo: { ...input.finance.menuInfo, packagingCostPerUnit: +e.target.value } })} />
        </Field>
        <Field label="식재료비 (원/개)">
          <input type="number" className={inputCls} value={input.finance.directMaterialCost.ingredientCostPerUnit}
            onChange={e => updateFinance({ directMaterialCost: { ingredientCostPerUnit: +e.target.value } })} />
        </Field>
        <Field label="단위당 변동제조간접원가 (원)">
          <input type="number" className={inputCls} value={input.finance.variableOverheadCostPerUnit}
            onChange={e => updateFinance({ variableOverheadCostPerUnit: +e.target.value })} />
        </Field>
      </div>
    </div>
  );
}

function Step2({ input, update }: { input: AnalysisInput; update: (p: Partial<AnalysisInput>) => void }) {
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">② 입지</h2>
      <Field label="주소" hint="서울 외 지역은 보정계수 1.0(중립)이 적용됩니다.">
        <input className={inputCls} value={input.location.address}
          onChange={e => update({ location: { ...input.location, address: e.target.value } })}
          placeholder="서울특별시 강남구 ..." />
      </Field>
      <div className="grid grid-cols-2 gap-4">
        <Field label="위도 (lat)" hint="WGS84">
          <input type="number" step="0.0001" className={inputCls} value={input.location.lat}
            onChange={e => update({ location: { ...input.location, lat: +e.target.value } })} />
        </Field>
        <Field label="경도 (lng)" hint="WGS84">
          <input type="number" step="0.0001" className={inputCls} value={input.location.lng}
            onChange={e => update({ location: { ...input.location, lng: +e.target.value } })} />
        </Field>
      </div>
      <div className="text-xs text-gray-500 mt-2">
        프로토타입이라 좌표 직접 입력. 운영 시 카카오 우편번호 SDK 로 자동 채움 예정.
      </div>
    </div>
  );
}

function Step3({ input, updateFinance }: { input: AnalysisInput; updateFinance: (p: Partial<AnalysisInput['finance']>) => void }) {
  const labor = input.finance.directLaborCost;
  const setEmployees = (employees: EmployeeInput[]) =>
    updateFinance({ directLaborCost: { ...labor, employees } });

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">③ 인건비</h2>
      <div className="grid grid-cols-2 gap-4 mb-6">
        <Field label="BOH 시간 (분)" hint="메뉴 1개당 주방 작업 시간">
          <input type="number" step="0.1" className={inputCls} value={labor.bohTimeMinutes}
            onChange={e => updateFinance({ directLaborCost: { ...labor, bohTimeMinutes: +e.target.value } })} />
        </Field>
        <Field label="FOH 시간 (분)" hint="홀 서빙 시간">
          <input type="number" step="0.1" className={inputCls} value={labor.fohTimeMinutes}
            onChange={e => updateFinance({ directLaborCost: { ...labor, fohTimeMinutes: +e.target.value } })} />
        </Field>
      </div>

      <div className="text-sm font-semibold mb-2">직원 목록</div>
      {labor.employees.map((emp, idx) => (
        <EmployeeCard key={idx} emp={emp}
          onChange={(next: EmployeeInput) => setEmployees(labor.employees.map((e, i) => i === idx ? next : e))}
          onDelete={() => setEmployees(labor.employees.filter((_, i) => i !== idx))} />
      ))}
      <button onClick={() => setEmployees([...labor.employees, { name: '', mode: 'simple', monthlyTotal: 2000000 }])}
        className="w-full py-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-navy hover:text-navy transition">
        + 직원 추가
      </button>
    </div>
  );
}

function EmployeeCard({ emp, onChange, onDelete }: { emp: EmployeeInput; onChange: (e: EmployeeInput) => void; onDelete: () => void }) {
  return (
    <div className="border border-gray-200 rounded-lg p-4 mb-3">
      <div className="flex justify-between items-center mb-3">
        <input className="flex-1 mr-3 px-3 py-1 border border-gray-300 rounded text-sm" placeholder="직원 구분 (예: 사장, 알바)"
          value={emp.name ?? ''} onChange={e => onChange({ ...emp, name: e.target.value } as EmployeeInput)} />
        <button onClick={onDelete} className="text-red-500 text-sm">삭제</button>
      </div>

      <div className="flex gap-1 mb-3 bg-gray-100 rounded-lg p-1 w-fit">
        <button onClick={() => onChange({ name: emp.name, mode: 'simple', monthlyTotal: 2000000 })}
          className={`px-4 py-1 rounded ${emp.mode === 'simple' ? 'bg-white shadow font-semibold' : 'text-gray-500'}`}>간편 입력</button>
        <button onClick={() => onChange({ name: emp.name, mode: 'detail', hourlyWage: 10030, weeklyHours: 40, hasNightShift: false })}
          className={`px-4 py-1 rounded ${emp.mode === 'detail' ? 'bg-white shadow font-semibold' : 'text-gray-500'}`}>상세 입력</button>
      </div>

      {emp.mode === 'simple' ? (
        <Field label="월 인건비 총액 (원)">
          <input type="number" className={inputCls} value={emp.monthlyTotal}
            onChange={e => onChange({ ...emp, monthlyTotal: +e.target.value })} />
        </Field>
      ) : (
        <div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="시급 (원)">
              <input type="number" className={inputCls} value={emp.hourlyWage}
                onChange={e => onChange({ ...emp, hourlyWage: +e.target.value })} />
            </Field>
            <Field label="주 소정근로시간">
              <input type="number" step="0.1" className={inputCls} value={emp.weeklyHours}
                onChange={e => onChange({ ...emp, weeklyHours: +e.target.value })} />
            </Field>
          </div>
          <label className="flex items-center gap-2 text-sm mb-2">
            <input type="checkbox" checked={emp.hasNightShift}
              onChange={e => onChange({ ...emp, hasNightShift: e.target.checked, nightShiftHoursPerWeek: e.target.checked ? (emp.nightShiftHoursPerWeek ?? 5) : undefined })} />
            야간 근무 있음
          </label>
          {emp.hasNightShift && (
            <Field label="주당 야간 시간">
              <input type="number" step="0.5" className={inputCls} value={emp.nightShiftHoursPerWeek ?? 0}
                onChange={e => onChange({ ...emp, nightShiftHoursPerWeek: +e.target.value })} />
            </Field>
          )}
        </div>
      )}
    </div>
  );
}

function Step4({ input, updateFinance }: { input: AnalysisInput; updateFinance: (p: Partial<AnalysisInput['finance']>) => void }) {
  const fc = input.finance.fixedCost;
  const total = fc.monthlyRent + fc.managementFee + fc.marketingCost + fc.miscCost;
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">④ 고정비 (월)</h2>
      <div className="grid grid-cols-2 gap-4">
        <Field label="임대료"><input type="number" className={inputCls} value={fc.monthlyRent} onChange={e => updateFinance({ fixedCost: { ...fc, monthlyRent: +e.target.value } })} /></Field>
        <Field label="관리비"><input type="number" className={inputCls} value={fc.managementFee} onChange={e => updateFinance({ fixedCost: { ...fc, managementFee: +e.target.value } })} /></Field>
        <Field label="마케팅비"><input type="number" className={inputCls} value={fc.marketingCost} onChange={e => updateFinance({ fixedCost: { ...fc, marketingCost: +e.target.value } })} /></Field>
        <Field label="기타비"><input type="number" className={inputCls} value={fc.miscCost} onChange={e => updateFinance({ fixedCost: { ...fc, miscCost: +e.target.value } })} /></Field>
      </div>
      <div className="mt-4 p-4 bg-gray-50 rounded-lg flex justify-between items-center">
        <span className="text-sm text-gray-600">월 고정비 합계</span>
        <span className="text-xl font-bold text-navy">{total.toLocaleString()}원</span>
      </div>
    </div>
  );
}

function Step5({ input, updateFinance }: { input: AnalysisInput; updateFinance: (p: Partial<AnalysisInput['finance']>) => void }) {
  const sa = input.finance.salesAssumption;
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">⑤ 판매 가정</h2>
      <div className="grid grid-cols-2 gap-4">
        <Field label="예상 일 판매량 (개)"><input type="number" className={inputCls} value={sa.expectedDailySales} onChange={e => updateFinance({ salesAssumption: { ...sa, expectedDailySales: +e.target.value } })} /></Field>
        <Field label="주당 영업일"><input type="number" className={inputCls} value={sa.workingDaysPerWeek} onChange={e => updateFinance({ salesAssumption: { ...sa, workingDaysPerWeek: +e.target.value } })} /></Field>
        <Field label="일 영업시간"><input type="number" step="0.5" className={inputCls} value={sa.dailyWorkHours} onChange={e => updateFinance({ salesAssumption: { ...sa, dailyWorkHours: +e.target.value } })} /></Field>
        <Field label="트렌드 지속 기간 (주)" hint={`${sa.trendDurationWeeks}주`}>
          <input type="range" min={1} max={52} className="w-full" value={sa.trendDurationWeeks}
            onChange={e => updateFinance({ salesAssumption: { ...sa, trendDurationWeeks: +e.target.value } })} />
        </Field>
      </div>
    </div>
  );
}

// ── Loading Overlay ──────────────────────────────────────────────────

function LoadingOverlay({ stage }: { stage: number }) {
  const stages = ['입지 분석 중', '재무 계산 중', '트렌드 분석 중', 'AI 리포트 작성 중'];
  return (
    <div className="fixed inset-0 bg-navy/95 z-50 flex items-center justify-center">
      <div className="text-white max-w-md text-center px-6">
        <div className="w-16 h-16 border-4 border-white/20 border-t-gold rounded-full animate-spin mx-auto mb-8" />
        <div className="text-xl font-bold mb-6">분석 중입니다</div>
        <div className="text-sm text-white/60 mb-8">약 30초 정도 걸려요. 잠시만 기다려주세요.</div>
        <div className="space-y-3 text-left">
          {stages.map((label, i) => (
            <div key={label} className="flex items-center gap-3">
              <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs ${i < stage ? 'bg-gold text-navy' : i === stage ? 'bg-white text-navy animate-pulse' : 'bg-white/20'}`}>
                {i < stage ? '✓' : i + 1}
              </div>
              <span className={i <= stage ? 'text-white' : 'text-white/40'}>{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
