'use client';
import { Box } from './Primitives';

const STEPS = [
  { n: 1, t: '입지 분석 중...',      sub: '서울 상권 데이터 매칭'         },
  { n: 2, t: '재무 계산 중...',      sub: '단위 경제성 · BEP 계산'        },
  { n: 3, t: '트렌드 분석 중...',     sub: '네이버 데이터랩 + DTW 매칭'    },
  { n: 4, t: 'AI 리포트 작성 중...', sub: 'LangGraph LLM 호출'            },
];

function StepIndicator({ state, n }: { state: 'done' | 'active' | 'pending'; n: number }) {
  if (state === 'done') {
    return (
      <span style={{
        width: 22, height: 22, borderRadius: 999, flexShrink: 0,
        background: 'var(--tb-navy)', border: '1.5px solid var(--tb-navy)',
        color: '#fff', fontSize: 11, fontWeight: 700,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>✓</span>
    );
  }
  if (state === 'active') {
    return (
      <svg width="22" height="22" viewBox="0 0 22 22" style={{ flexShrink: 0, display: 'block' }}>
        <circle cx="11" cy="11" r="9" stroke="var(--tb-line-2)" strokeWidth="2" fill="none" />
        <circle
          cx="11" cy="11" r="9"
          stroke="var(--tb-navy)" strokeWidth="2.5" fill="none"
          strokeDasharray="18 38" strokeLinecap="round"
          style={{ transformOrigin: '11px 11px', animation: 'tb-spin 0.85s linear infinite' }}
        />
      </svg>
    );
  }
  return (
    <span style={{
      width: 22, height: 22, borderRadius: 999, flexShrink: 0,
      background: '#fff', border: '1.5px solid var(--tb-line)',
      color: 'var(--tb-ink-3)', fontSize: 11, fontWeight: 700,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{n}</span>
  );
}

export function LoadingOverlay({
  menuName,
  activeStep,
  isCompleting = false,
}: {
  menuName: string;
  activeStep: number;
  isCompleting?: boolean;
}) {
  // 25 / 50 / 75 / 75(대기) → API 응답 후 isCompleting=true 시 100%
  const totalProgress = isCompleting ? 100 : Math.min(75, (activeStep + 1) * 25);
  const circumference = 2 * Math.PI * 72;
  const centerLabel = isCompleting ? '완료!' : '분석 중...';

  return (
    <>
      <style>{`@keyframes tb-spin { to { transform: rotate(360deg); } }`}</style>
      <div style={{
        position: 'fixed', inset: 0, zIndex: 200,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: 'linear-gradient(180deg, var(--tb-card) 0%, #ECEFF7 100%)',
        padding: 20,
      }}>
        <Box style={{ width: '100%', maxWidth: 560, padding: '48px 48px 36px', textAlign: 'center', background: '#fff' }}>
          {/* 도넛 */}
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 20 }}>
            <svg width="180" height="180" viewBox="0 0 180 180">
              <circle cx="90" cy="90" r="72" stroke="var(--tb-line-2)" strokeWidth="10" fill="none" />
              <circle
                cx="90" cy="90" r="72"
                stroke="var(--tb-navy)" strokeWidth="10" fill="none" strokeLinecap="round"
                strokeDasharray={`${(totalProgress / 100) * circumference} ${circumference}`}
                transform="rotate(-90 90 90)"
                style={{ transition: 'stroke-dasharray 0.4s ease' }}
              />
              <text x="90" y="86" textAnchor="middle" fontSize="38" fontWeight={700} fill="var(--tb-navy)" fontFamily="var(--tb-font)">
                {Math.round(totalProgress)}%
              </text>
              <text x="90" y="108" textAnchor="middle" fontSize="11" fill="var(--tb-ink-3)" fontFamily="var(--tb-font-mono)">
                {centerLabel}
              </text>
            </svg>
          </div>

          <div className="wf-h2" style={{ marginBottom: 8 }}>{menuName} 분석 중이에요</div>
          <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 28 }}>
            약 30초 정도 걸려요. 잠시만 기다려주세요.
          </p>

          <div style={{ textAlign: 'left', display: 'grid', gap: 10, marginBottom: 12 }}>
            {STEPS.map((s, i) => {
              const state: 'done' | 'active' | 'pending' =
                i < activeStep   ? 'done' :
                i === activeStep ? 'active' :
                'pending';
              return (
                <div key={s.n} style={{
                  display: 'grid', gridTemplateColumns: '24px 1fr 24px', gap: 10, alignItems: 'center',
                  padding: '10px 14px',
                  border: '1px solid ' + (state === 'active' ? 'var(--tb-navy)' : 'var(--tb-line-2)'),
                  borderRadius: 8,
                  background: state === 'active' ? 'var(--tb-navy-soft)' : '#fff',
                  transition: 'all 0.2s',
                }}>
                  <StepIndicator state={state} n={s.n} />
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 600 }}>{s.t}</div>
                    <div className="wf-mono">{s.sub}</div>
                  </div>
                  {/* 우측: 완료는 체크 텍스트, 나머지는 공백 */}
                  <span className="wf-mono" style={{
                    textAlign: 'right', fontSize: 10,
                    color: 'var(--tb-ink-3)',
                  }}>
                    {state === 'done' ? '완료' : ''}
                  </span>
                </div>
              );
            })}
          </div>
        </Box>
      </div>
    </>
  );
}
