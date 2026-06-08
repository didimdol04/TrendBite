'use client';
import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Box, Btn, Input, Field, Progress, Badge, Chip, PendingMarker, PendingBox, NumberInput } from '@/components/Primitives';
import { SketchBars } from '@/components/Charts';
import { AddressInput } from '@/components/AddressInput';
import { LoadingOverlay } from '@/components/LoadingOverlay';
import { useWizardForm } from '@/lib/wizardForm';
import { useUser } from '@/lib/userContext';
import { runAnalysis, searchMenuKeywords, getPopularKeywords, isTrendWeak } from '@/lib/api';
import { toast } from '@/components/Toast';
import { CATEGORY_OPTIONS } from '@/lib/mockData';
import type { EmployeeDto, AnalysisRequest } from '@/lib/types';

const STEPS = [
  { n: 1, t: '메뉴 정보', sub: '메뉴명 · 카테고리 · 가격 · 원가' },
  { n: 2, t: '입지',       sub: '주소 · 좌표' },
  { n: 3, t: '인건비',     sub: '제조 시간 · 직원 카드' },
  { n: 4, t: '고정비',     sub: '임대료 · 관리 · 마케팅' },
  { n: 5, t: '판매 가정',  sub: '일판매량 · 영업일 · 기존 매장' },
];

export default function AnalyzePage() {
  return (
    <Suspense fallback={<div style={{ padding: 40 }}>로딩 중...</div>}>
      <AnalyzeInner />
    </Suspense>
  );
}

function AnalyzeInner() {
  const router = useRouter();
  const params = useSearchParams();
  const { form, setForm, reset, loadSample, hydrated } = useWizardForm();
  const { state, saveAnalysis } = useUser();
  const isGuest = params.get('guest') === '1' || state.kind !== 'identified';

  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [loadingStep, setLoadingStep] = useState(0); // 0~3: 입지/재무/트렌드/AI
  const [isCompleting, setIsCompleting] = useState(false); // API 응답 후 100% 전환 신호
  // 사용자가 Step1에서 명시적으로 트렌드를 건너뛰기로 선택했을 때만 true — 타이핑 결과로 자동 설정 안 함
  const [userForceSkipTrend, setUserForceSkipTrend] = useState(false);

  // 인기 키워드에서 진입 시 자동 채움
  useEffect(() => {
    const kw = params.get('keyword');
    const cat = params.get('category');
    if (kw && hydrated) {
      setForm(f => ({
        ...f,
        keyword: kw,
        category: cat || f.category,
        finance: { ...f.finance, menuInfo: { ...f.finance.menuInfo, menuName: kw, category: cat || f.finance.menuInfo.category } },
      }));
    }
  }, [hydrated, params, setForm]);

  if (!hydrated) {
    return <div style={{ padding: 40 }}>로딩 중...</div>;
  }

  const goNext = () => setStep(s => Math.min(4, s + 1));
  const goPrev = () => setStep(s => Math.max(0, s - 1));

  const onSubmit = async () => {
    setSubmitting(true);
    setLoadingStep(0);
    setIsCompleting(false);
    // 4단계 progress 시뮬레이션 (총 ~2초) — step 3(AI) 도달 후 API 응답까지 75%에서 대기
    const stepInterval = setInterval(() => {
      setLoadingStep(s => Math.min(3, s + 1));
    }, 500);
    try {
      const res = await runAnalysis(form);
      clearInterval(stepInterval);
      setLoadingStep(3);      // 4단계 모두 체크
      setIsCompleting(true);  // 도넛 100% 점프
      await new Promise(r => setTimeout(r, 600));

      // 분석 결과 기반 트렌드 건너뜀 최종 판정
      const backendTrendWeak = isTrendWeak(res.data.trend);
      const finalNoTrend = userForceSkipTrend || backendTrendWeak;
      if (backendTrendWeak && !userForceSkipTrend) {
        toast.info('네이버 데이터랩 검색량이 기준치보다 낮아 트렌드 분석을 건너뜁니다. 입지·재무 중심으로 평가했어요.');
      }

      if (state.kind === 'identified') {
        const saved = saveAnalysis({
          menuName: form.finance.menuInfo.menuName || form.keyword,
          category: form.category,
          address: form.location.address,
          data: res.data,
          noTrend: finalNoTrend,
          menuId: res.data.menuId,
          originalRequest: form,
        });
        reset();
        router.push(`/result/${saved.id}`);
      } else {
        // 게스트: sessionStorage에 결과만 임시 저장
        sessionStorage.setItem('trendbite_guest_result', JSON.stringify({
          menuName: form.finance.menuInfo.menuName || form.keyword,
          category: form.category,
          address: form.location.address,
          data: res.data,
          createdAt: new Date().toISOString(),
          noTrend: finalNoTrend,
        }));
        reset();
        router.push('/result/guest');
      }
    } catch (e: any) {
      clearInterval(stepInterval);
      alert('분석 중 오류: ' + (e?.message || e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: 1280, margin: '0 auto', background: '#fff', minHeight: 'calc(100vh - 104px)' }}>
      <div className="wizard-layout">
        <aside className="wizard-nav">
          <div className="wf-eyebrow" style={{ marginBottom: 12 }}>입력 위저드</div>
          <div style={{ marginBottom: 16, fontWeight: 700, fontSize: 16 }}>
            {step + 1}/5 단계
          </div>
          <Progress pct={((step + 1) / 5) * 100} style={{ marginBottom: 24 }} />
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 4 }}>
            {STEPS.map((s, i) => {
              const active = i === step;
              const done = i < step;
              return (
                <li key={i} onClick={() => setStep(i)} style={{
                  padding: '10px 12px', borderRadius: 8, cursor: 'pointer',
                  background: active ? 'var(--tb-navy-soft)' : 'transparent',
                  border: active ? '1px solid rgba(26,42,108,0.15)' : '1px solid transparent',
                  display: 'grid', gridTemplateColumns: '20px 1fr', gap: 10, alignItems: 'flex-start',
                }}>
                  <span style={{
                    width: 18, height: 18, borderRadius: 999,
                    background: done ? 'var(--tb-navy)' : '#fff',
                    border: '1.5px solid ' + (active || done ? 'var(--tb-navy)' : 'var(--tb-line)'),
                    color: done ? '#fff' : 'var(--tb-navy)',
                    fontSize: 10, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 2,
                  }}>{done ? '✓' : s.n}</span>
                  <div>
                    <div style={{ fontSize: 12.5, fontWeight: active ? 700 : 600, color: active ? 'var(--tb-navy)' : 'var(--tb-ink)' }}>{s.t}</div>
                    <div className="wf-mono" style={{ marginTop: 2 }}>{s.sub}</div>
                  </div>
                </li>
              );
            })}
          </ul>
          <div style={{ marginTop: 24, padding: '12px 0', borderTop: '1px solid var(--tb-line-2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="wf-mono">자동 저장</span>
              <span className="wf-mono" style={{ color: 'var(--tb-navy)' }}>● 임시 저장됨</span>
            </div>
            <button onClick={loadSample} style={{ background: 'transparent', border: 0, color: 'var(--tb-navy)', fontSize: 11.5, cursor: 'pointer', padding: 0 }}>
              📋 샘플 데이터 채우기 (마라탕)
            </button>
            <br/>
            <button onClick={() => { if (confirm('입력 내용을 모두 지울까요?')) reset(); }}
              style={{ background: 'transparent', border: 0, color: 'var(--tb-ink-3)', fontSize: 11.5, cursor: 'pointer', padding: 0, marginTop: 6 }}>
              🗑️ 초기화
            </button>
          </div>

          {isGuest && (
            <div style={{ marginTop: 16, padding: '10px 12px', background: 'var(--tb-amber-soft)', borderRadius: 6, fontSize: 11, color: '#806100' }}>
              👤 <b>게스트 모드</b><br />
              결과는 저장되지 않습니다. 닉네임 입력 시 이력 조회 가능.
            </div>
          )}
        </aside>

        <main className="wizard-body">
          {step === 0 && <Step1 form={form} setForm={setForm} onUserForceSkipTrend={setUserForceSkipTrend} userForceSkipTrend={userForceSkipTrend} />}
          {step === 1 && <Step2 form={form} setForm={setForm} />}
          {step === 2 && <Step3 form={form} setForm={setForm} />}
          {step === 3 && <Step4 form={form} setForm={setForm} />}
          {step === 4 && <Step5 form={form} setForm={setForm} />}

          <FormBar
            step={step}
            onPrev={goPrev}
            onNext={goNext}
            onSubmit={onSubmit}
            submitting={submitting}
          />
        </main>
      </div>

      <style jsx>{`
        .wizard-layout {
          display: grid;
          grid-template-columns: 240px 1fr;
          min-height: calc(100vh - 104px);
        }
        .wizard-nav {
          padding: 32px 20px 24px;
          border-right: 1px solid var(--tb-line);
          background: #fff;
        }
        .wizard-body {
          padding: 40px clamp(20px, 5vw, 56px);
        }
        @media (max-width: 768px) {
          .wizard-layout { grid-template-columns: 1fr; }
          .wizard-nav { border-right: 0; border-bottom: 1px solid var(--tb-line); padding: 16px; }
        }
      `}</style>

      {submitting && <LoadingOverlay menuName={form.finance.menuInfo.menuName || form.keyword || '신메뉴'} activeStep={loadingStep} isCompleting={isCompleting} />}
    </div>
  );
}

function FormBar({
  step, onPrev, onNext, onSubmit, submitting,
}: {
  step: number; onPrev: () => void; onNext: () => void;
  onSubmit: () => void; submitting: boolean;
}) {
  const isLast = step === 4;
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      borderTop: '1px solid var(--tb-line-2)', paddingTop: 16, marginTop: 32,
      flexWrap: 'wrap', gap: 12,
    }}>
      <Btn onClick={onPrev} disabled={step === 0}>← 이전</Btn>
      {isLast ? (
        <Btn primary lg onClick={onSubmit} disabled={submitting} style={{ padding: '0 36px' }}>
          {submitting ? '분석 중...' : '분석 시작 →'}
        </Btn>
      ) : (
        <Btn primary onClick={onNext}>다음 →</Btn>
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Step 1: 메뉴 정보 + 키워드 매칭 후보 드롭다운
// ────────────────────────────────────────────────────────
function Step1({
  form, setForm, onUserForceSkipTrend, userForceSkipTrend,
}: {
  form: AnalysisRequest;
  setForm: React.Dispatch<React.SetStateAction<AnalysisRequest>>;
  onUserForceSkipTrend: (v: boolean) => void;
  userForceSkipTrend: boolean;
}) {
  const [candidates, setCandidates] = useState<string[]>([]);
  const [noDataMode, setNoDataMode] = useState(false);
  const [popularKeywords, setPopularKeywords] = useState<string[]>([]);

  useEffect(() => {
    getPopularKeywords(8)
      .then(res => {
        // 백엔드 중복 항목 방어적 제거
        const seen = new Set<string>();
        const unique = res.filter(k => !seen.has(k.keyword) && seen.add(k.keyword));
        setPopularKeywords(unique.map(k => k.keyword));
      })
      .catch(() => {});
  }, []);

  const setMenu = (patch: Partial<typeof form.finance.menuInfo>) => {
    setForm(f => ({
      ...f,
      finance: { ...f.finance, menuInfo: { ...f.finance.menuInfo, ...patch } },
      keyword: patch.menuName ?? f.keyword,
      category: patch.category ?? f.category,
    }));
  };

  const onMenuNameChange = async (name: string) => {
    setMenu({ menuName: name });
    setForm(f => ({ ...f, keyword: name }));
    if (!name.trim()) {
      setCandidates([]);
      setNoDataMode(false);
      onUserForceSkipTrend(false);
      return;
    }
    try {
      // searchMenuKeywords는 자동완성 제안 용도만 — 결과 유무로 트렌드 분석을 막지 않음
      const results = await searchMenuKeywords(name.trim());
      setCandidates(results);
      setNoDataMode(results.length === 0);
    } catch {
      setCandidates([]);
      setNoDataMode(true);
    }
  };

  const inputName = form.finance.menuInfo.menuName.trim();
  const isExactMatch = inputName.length > 0 && candidates.includes(inputName);
  const displayCandidates = candidates.filter(c => c !== inputName);
  const chipsToShow = inputName === '' ? popularKeywords : displayCandidates;
  const chipsLabel  = inputName === '' ? '🔥 지금 뜨는 메뉴' : '연관 메뉴';

  return (
    <div>
      <div className="wf-h1" style={{ fontSize: 24, marginBottom: 6 }}>메뉴 정보를 입력해주세요</div>
      <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 24 }}>
        분석할 메뉴의 기본 정보입니다.
      </p>

      <div className="grid-2col">
        <Field label="메뉴명" required style={{ gridColumn: '1 / -1' }}>
          <Input
            lg
            placeholder="예: 마라탕"
            value={form.finance.menuInfo.menuName}
            onChange={(e) => onMenuNameChange(e.target.value)}
          />
          {/* 인기 트렌드 키워드(입력 없음) / 검색 매칭 키워드(타이핑 중) */}
          {chipsToShow.length > 0 && !isExactMatch && (
            <div style={{ marginTop: 8 }}>
              <div style={{ fontSize: 11, color: 'var(--tb-ink-3)', marginBottom: 6 }}>
                {chipsLabel}
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {chipsToShow.map(kw => (
                  <Chip key={kw} onClick={() => onMenuNameChange(kw)}>
                    {kw}
                  </Chip>
                ))}
              </div>
            </div>
          )}
          {/* 아직 분석된 적 없는 메뉴 — 부드러운 안내 (트렌드는 분석 후 결과 기반으로 판단) */}
          {noDataMode && !isExactMatch && (
            <div style={{
              marginTop: 8, padding: '14px 16px',
              background: 'var(--tb-card)',
              border: '1px solid var(--tb-line)',
              borderRadius: 'var(--tb-radius-sm)',
              fontSize: 12.5,
            }}>
              <div style={{ fontWeight: 700, marginBottom: 4, color: 'var(--tb-ink)' }}>
                이 메뉴는 아직 분석된 적 없어요
              </div>
              <div style={{ color: 'var(--tb-ink-2)', marginBottom: 10, lineHeight: 1.55 }}>
                그대로 분석을 진행하면 네이버 데이터랩에서 트렌드를 새로 조회합니다.
                검색량이 충분하지 않으면 입지·재무만 표시돼요.
              </div>
              <button
                onClick={() => onUserForceSkipTrend(!userForceSkipTrend)}
                style={{
                  background: userForceSkipTrend ? 'var(--tb-navy)' : 'transparent',
                  border: '1px solid var(--tb-navy)',
                  color: userForceSkipTrend ? '#fff' : 'var(--tb-navy)',
                  borderRadius: 6,
                  padding: '5px 12px',
                  fontSize: 11.5,
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                {userForceSkipTrend ? '✓ 트렌드 건너뛰기 선택됨' : '트렌드 건너뛰고 입지·재무만 분석'}
              </button>
            </div>
          )}
          {/* 정확히 매칭됨 */}
          {isExactMatch && (
            <div style={{ marginTop: 6, fontSize: 11.5, color: 'var(--tb-success)' }}>
              ✓ 트렌드 데이터 매칭됨
            </div>
          )}
        </Field>

        <Field label="카테고리" required>
          <select
            className="wf-input wf-input-lg"
            style={{ height: 44 }}
            value={form.category}
            onChange={(e) => setMenu({ category: e.target.value })}
          >
            {CATEGORY_OPTIONS.map(o => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </Field>

        <Field label="판매가 (원)" required>
          <NumberInput lg placeholder="9,800"
            value={form.finance.menuInfo.sellingPrice}
            onChange={n => setMenu({ sellingPrice: n })}
          />
        </Field>

        <Field label="식재료비 (원/단위)" required>
          <NumberInput lg placeholder="2,400"
            value={form.finance.directMaterialCost.ingredientCostPerUnit}
            onChange={n => setForm(f => ({ ...f, finance: { ...f.finance, directMaterialCost: { ingredientCostPerUnit: n } } }))}
          />
        </Field>

        <Field label="포장재비 (원/단위)">
          <NumberInput lg placeholder="320"
            value={form.finance.menuInfo.packagingCostPerUnit}
            onChange={n => setMenu({ packagingCostPerUnit: n })}
          />
        </Field>

        <Field label="변동제조간접원가 (원/단위)" hint="가스·전기·소모품 등 메뉴 1개 당 발생하는 변동 간접비">
          <NumberInput lg placeholder="500"
            value={form.finance.variableOverheadCostPerUnit}
            onChange={n => setForm(f => ({ ...f, finance: { ...f.finance, variableOverheadCostPerUnit: n } }))}
          />
        </Field>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Step 2: 입지
// ────────────────────────────────────────────────────────
function Step2({
  form, setForm,
}: { form: AnalysisRequest; setForm: React.Dispatch<React.SetStateAction<AnalysisRequest>> }) {
  return (
    <div>
      <div className="wf-h1" style={{ fontSize: 24, marginBottom: 6 }}>매장 입지를 알려주세요</div>
      <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 24 }}>
        주소 검색 또는 지도 클릭으로 위치를 선택하세요.
      </p>

      <Field label="매장 주소" required style={{ marginBottom: 16 }}>
        <AddressInput
          value={{
            address: form.location.address,
            lat: form.location.lat,
            lng: form.location.lng,
          }}
          onChange={({ address, lat, lng }) =>
            setForm(f => ({ ...f, location: { ...f.location, address, lat, lng } }))
          }
        />
      </Field>

      <Box style={{
        marginTop: 4, padding: '10px 14px',
        display: 'flex', gap: 8, alignItems: 'center',
        background: 'var(--tb-amber-soft)', borderColor: 'transparent',
      }}>
        <span style={{ fontSize: 14 }}>ⓘ</span>
        <span style={{ fontSize: 12 }}>서울 외 지역은 보정계수 1.0 (중립)이 적용됩니다.</span>
      </Box>
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Step 3: 인건비 - simple/detail 토글
// ────────────────────────────────────────────────────────
function Step3({
  form, setForm,
}: { form: AnalysisRequest; setForm: React.Dispatch<React.SetStateAction<AnalysisRequest>> }) {
  const labor = form.finance.directLaborCost;

  const updateEmployees = (employees: EmployeeDto[]) => {
    setForm(f => ({
      ...f,
      finance: { ...f.finance, directLaborCost: { ...f.finance.directLaborCost, employees } }
    }));
  };
  const updateEmployee = (i: number, patch: Partial<EmployeeDto>) => {
    const next = [...labor.employees];
    next[i] = { ...next[i], ...patch };
    updateEmployees(next);
  };
  const addEmployee = () => {
    updateEmployees([
      ...labor.employees,
      { name: `직원 ${labor.employees.length + 1}`, mode: 'simple', monthlyTotal: 0 }
    ]);
  };
  const removeEmployee = (i: number) => {
    updateEmployees(labor.employees.filter((_, idx) => idx !== i));
  };

  return (
    <div>
      <div className="wf-h1" style={{ fontSize: 24, marginBottom: 6 }}>인건비를 입력해주세요</div>
      <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 24 }}>
        직원별로 간편/상세 모드를 자유롭게 선택할 수 있어요.
      </p>

      <div className="wf-h3" style={{ marginBottom: 10 }}>A. 메뉴 1개 제조 시간</div>
      <div className="grid-2col" style={{ marginBottom: 28 }}>
        <Field label="BOH (주방, 분)">
          <Input type="number" min={0} step={0.1}
            value={labor.bohTimeMinutes || ''}
            onChange={e => setForm(f => ({
              ...f,
              finance: { ...f.finance, directLaborCost: { ...labor, bohTimeMinutes: Number(e.target.value) || 0 } }
            }))} />
        </Field>
        <Field label="FOH (홀, 분)">
          <Input type="number" min={0} step={0.1}
            value={labor.fohTimeMinutes || ''}
            onChange={e => setForm(f => ({
              ...f,
              finance: { ...f.finance, directLaborCost: { ...labor, fohTimeMinutes: Number(e.target.value) || 0 } }
            }))} />
        </Field>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
        <div className="wf-h3">B. 직원 카드 (총 {labor.employees.length}명)</div>
        <Btn onClick={addEmployee}>＋ 직원 추가</Btn>
      </div>

      {labor.employees.map((emp, i) => (
        <Box key={i} style={{ padding: 16, marginBottom: 12, borderColor: emp.mode === 'detail' ? 'var(--tb-navy)' : 'var(--tb-line)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Input
                style={{ width: 200, height: 30, fontSize: 13, fontWeight: 600 }}
                value={emp.name}
                onChange={e => updateEmployee(i, { name: e.target.value })}
              />
              <Badge tone={emp.mode === 'simple' ? 'navy' : 'amber'}>
                {emp.mode === 'simple' ? '간편' : '상세'}
              </Badge>
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <SegToggle
                active={emp.mode}
                onChange={(m) => updateEmployee(i, { mode: m })}
              />
              <button className="wf-btn" style={{ height: 28, padding: '0 8px', fontSize: 11 }}
                onClick={() => removeEmployee(i)}
                disabled={labor.employees.length === 1}>
                삭제
              </button>
            </div>
          </div>

          {emp.mode === 'simple' ? (
            <Field label="월 인건비 총액 (원)">
              <NumberInput placeholder="3,200,000"
                value={emp.monthlyTotal || 0}
                onChange={n => updateEmployee(i, { monthlyTotal: n })}
              />
            </Field>
          ) : (
            <>
              <div className="grid-2col" style={{ marginBottom: 10 }}>
                <Field label="시급 (원)">
                  <NumberInput placeholder="11,000"
                    value={emp.hourlyWage || 0}
                    onChange={n => updateEmployee(i, { hourlyWage: n })}
                  />
                </Field>
                <Field label="주 소정근로시간">
                  <Input type="number" min={0} step={0.1}
                    placeholder="20"
                    value={emp.weeklyHours || ''}
                    onChange={e => updateEmployee(i, { weeklyHours: Number(e.target.value) || 0 })}
                  />
                </Field>
              </div>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12.5, padding: '8px 0', cursor: 'pointer' }}>
                <input type="checkbox"
                  checked={!!emp.hasNightShift}
                  onChange={e => updateEmployee(i, { hasNightShift: e.target.checked })}
                />
                야간 근무 있음
              </label>
              {emp.hasNightShift && (
                <Field label="주당 야간 근무시간">
                  <Input type="number" min={0} step={0.1}
                    placeholder="6"
                    value={emp.nightShiftHoursPerWeek || ''}
                    onChange={e => updateEmployee(i, { nightShiftHoursPerWeek: Number(e.target.value) || 0 })}
                  />
                </Field>
              )}
            </>
          )}
        </Box>
      ))}
    </div>
  );
}

function SegToggle({
  active, onChange,
}: { active: 'simple' | 'detail'; onChange: (m: 'simple' | 'detail') => void }) {
  return (
    <div style={{ display: 'inline-flex', border: '1px solid var(--tb-line)', borderRadius: 6, padding: 2, background: 'var(--tb-card)' }}>
      {(['simple', 'detail'] as const).map(k => (
        <button
          key={k}
          onClick={() => onChange(k)}
          style={{
            border: 0, padding: '4px 12px', fontSize: 11.5, fontWeight: 600,
            background: active === k ? '#fff' : 'transparent',
            color: active === k ? 'var(--tb-navy)' : 'var(--tb-ink-3)',
            borderRadius: 4, cursor: 'pointer',
            boxShadow: active === k ? '0 1px 2px rgba(0,0,0,.06)' : 'none',
          }}>
          {k === 'simple' ? '간편' : '상세'}
        </button>
      ))}
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Step 4: 고정비
// ────────────────────────────────────────────────────────
function Step4({
  form, setForm,
}: { form: AnalysisRequest; setForm: React.Dispatch<React.SetStateAction<AnalysisRequest>> }) {
  const fc = form.finance.fixedCost;
  const total = fc.monthlyRent + fc.managementFee + fc.marketingCost + fc.miscCost;
  const setFc = (patch: Partial<typeof fc>) => setForm(f => ({
    ...f, finance: { ...f.finance, fixedCost: { ...f.finance.fixedCost, ...patch } }
  }));

  return (
    <div>
      <div className="wf-h1" style={{ fontSize: 24, marginBottom: 6 }}>월 고정비를 입력해주세요</div>
      <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 24 }}>
        매출과 무관하게 매달 나가는 비용입니다.
      </p>

      <div className="grid-2col">
        <Field label="월 임대료 (원)">
          <NumberInput lg placeholder="2,800,000" value={fc.monthlyRent} onChange={n => setFc({ monthlyRent: n })} />
        </Field>
        <Field label="월 관리비·공과금 (원)">
          <NumberInput lg placeholder="350,000" value={fc.managementFee} onChange={n => setFc({ managementFee: n })} />
        </Field>
        <Field label="월 마케팅비 (원)">
          <NumberInput lg placeholder="500,000" value={fc.marketingCost} onChange={n => setFc({ marketingCost: n })} />
        </Field>
        <Field label="월 기타 비용 (원)">
          <NumberInput lg placeholder="200,000" value={fc.miscCost} onChange={n => setFc({ miscCost: n })} />
        </Field>
      </div>

      <Box style={{
        marginTop: 20, padding: '14px 18px',
        background: 'var(--tb-navy-soft)', borderColor: 'transparent',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        flexWrap: 'wrap', gap: 12,
      }}>
        <div>
          <div className="wf-mono" style={{ color: 'var(--tb-navy)' }}>월 고정비 합계 (실시간)</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--tb-navy)' }}>
            {total.toLocaleString('ko-KR')}<span style={{ fontSize: 13, fontWeight: 500, marginLeft: 4 }}>원/월</span>
          </div>
        </div>
        {total > 0 && (
          <SketchBars width={200} height={56}
            data={[fc.monthlyRent, fc.managementFee, fc.marketingCost, fc.miscCost].map(v => v || 0.0001)}
            labels={['임대', '관리', '마케팅', '기타']}
          />
        )}
      </Box>
    </div>
  );
}

// ────────────────────────────────────────────────────────
// Step 5: 판매 가정 + 기존 매장
// ────────────────────────────────────────────────────────
function Step5({
  form, setForm,
}: { form: AnalysisRequest; setForm: React.Dispatch<React.SetStateAction<AnalysisRequest>> }) {
  const sa = form.finance.salesAssumption;
  const baseline = form.finance.baselineProfit;
  const setSa = (patch: Partial<typeof sa>) => setForm(f => ({
    ...f, finance: { ...f.finance, salesAssumption: { ...f.finance.salesAssumption, ...patch } }
  }));

  return (
    <div>
      <div className="wf-h1" style={{ fontSize: 24, marginBottom: 6 }}>판매 가정과 기존 매장 정보</div>
      <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 24 }}>
        마지막 단계예요. 가정값으로 시뮬레이션이 진행됩니다.
      </p>

      <div className="grid-2col">
        <Field label="예상 일 판매량 (개)" required>
          <Input lg type="number" min={0} placeholder="35"
            value={sa.expectedDailySales || ''}
            onChange={e => setSa({ expectedDailySales: Number(e.target.value) || 0 })} />
        </Field>
        <Field label="주당 영업일" required>
          <Input lg type="number" min={1} max={7} placeholder="6"
            value={sa.workingDaysPerWeek || ''}
            onChange={e => setSa({ workingDaysPerWeek: Number(e.target.value) || 0 })} />
        </Field>
        <Field label={`일 영업시간: ${sa.dailyWorkHours || 10}시간`} hint="기본 10시간">
          <input type="range" min={4} max={16} value={sa.dailyWorkHours || 10}
            onChange={e => setSa({ dailyWorkHours: Number(e.target.value) })}
            style={{ width: '100%', accentColor: 'var(--tb-navy)' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span className="wf-mono">4h</span>
            <span className="wf-mono">16h</span>
          </div>
        </Field>
        <Field label={`트렌드 지속 기간: ${sa.trendDurationWeeks}주`} hint="시뮬레이션 호라이즌">
          <input type="range" min={1} max={52} value={sa.trendDurationWeeks}
            onChange={e => setSa({ trendDurationWeeks: Number(e.target.value) })}
            style={{ width: '100%', accentColor: 'var(--tb-navy)' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span className="wf-mono">1주</span>
            <span className="wf-mono">52주</span>
          </div>
        </Field>
      </div>

      <Box style={{ padding: 16, marginTop: 24 }}>
        <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}>
          <div>
            <div className="wf-h3">기존 매장이 있으세요?</div>
            <div className="wf-mono" style={{ marginTop: 2 }}>증분 영업이익 계산 (선택)</div>
          </div>
          <button
            type="button"
            onClick={() => setForm(f => ({
              ...f,
              finance: {
                ...f.finance,
                baselineProfit: baseline ? null : { currentMonthlyRevenue: 0, currentMonthlyOperatingProfit: 0 }
              }
            }))}
            style={{
              width: 40, height: 22,
              background: baseline ? 'var(--tb-navy)' : 'var(--tb-line)',
              borderRadius: 999, position: 'relative', flexShrink: 0,
              border: 0, cursor: 'pointer', padding: 0,
            }}
          >
            <span style={{
              position: 'absolute',
              [baseline ? 'right' : 'left']: 2,
              top: 2, width: 18, height: 18, background: '#fff', borderRadius: 999,
              transition: 'all 0.2s',
            }} />
          </button>
        </label>
        {baseline && (
          <>
            <hr style={{ height: 1, background: 'var(--tb-line)', border: 0, margin: '14px 0' }} />
            <div className="grid-2col" style={{ gap: 12 }}>
              <Field label="현재 월매출 (원)">
                <NumberInput placeholder="18,000,000"
                  value={baseline.currentMonthlyRevenue}
                  onChange={n => setForm(f => ({ ...f, finance: { ...f.finance, baselineProfit: { ...f.finance.baselineProfit!, currentMonthlyRevenue: n } } }))}
                />
              </Field>
              <Field label="현재 월 영업이익 (원)">
                <NumberInput placeholder="4,200,000"
                  value={baseline.currentMonthlyOperatingProfit}
                  onChange={n => setForm(f => ({ ...f, finance: { ...f.finance, baselineProfit: { ...f.finance.baselineProfit!, currentMonthlyOperatingProfit: n } } }))}
                />
              </Field>
            </div>
          </>
        )}
      </Box>

      <div style={{ marginTop: 24, padding: '12px 14px', background: 'var(--tb-card)', borderRadius: 8, fontSize: 12 }}>
        <input type="checkbox" defaultChecked id="agree" style={{ marginRight: 8 }} />
        <label htmlFor="agree">
          <a href="/terms" target="_blank" style={{ color: 'var(--tb-navy)', textDecoration: 'underline' }}>이용약관</a> 및&nbsp;
          <a href="/privacy" target="_blank" style={{ color: 'var(--tb-navy)', textDecoration: 'underline' }}>개인정보처리방침</a>에 동의합니다.
        </label>
        <div style={{ marginTop: 6 }}>
          <PendingMarker>약관 본문 작성 필요</PendingMarker>
        </div>
      </div>
    </div>
  );
}
