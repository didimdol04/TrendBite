'use client';
import { Box } from './Primitives';

const STEPS = [
  { n: 1, t: '입지 분석 중...',      sub: '서울 상권 데이터 매칭'         },
  { n: 2, t: '재무 계산 중...',      sub: '단위 경제성 · BEP 계산'        },
  { n: 3, t: '트렌드 분석 중...',     sub: '네이버 데이터랩 + DTW 매칭'    },
  { n: 4, t: 'AI 리포트 작성 중...', sub: 'LangGraph LLM 호출'            },
];

/**
 * P3 로딩 — 와이어프레임 디자인 그대로.
 * 회의록의 분석 4단계 (입지/재무/트렌드/AI) 진행 표시.
 */
export function LoadingOverlay({ menuName, activeStep }: { menuName: string; activeStep: number }) {
  const totalProgress = ((activeStep + 1) / STEPS.length) * 100;
  const remainingSec = Math.max(1, Math.round((STEPS.length - activeStep) * 0.5));

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 200,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(180deg, var(--tb-card) 0%, #ECEFF7 100%)',
      padding: 20,
    }}>
      <Box style={{ width: '100%', maxWidth: 560, padding: '48px 48px 36px', textAlign: 'center', background: '#fff' }}>
        {/* big donut */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 20 }}>
          <svg width="180" height="180" viewBox="0 0 180 180">
            <circle cx="90" cy="90" r="72" stroke="var(--tb-line-2)" strokeWidth="10" fill="none" />
            <circle
              cx="90" cy="90" r="72"
              stroke="var(--tb-navy)" strokeWidth="10" fill="none" strokeLinecap="round"
              strokeDasharray={`${(totalProgress / 100) * 2 * Math.PI * 72} ${2 * Math.PI * 72}`}
              transform="rotate(-90 90 90)"
              style={{ transition: 'stroke-dasharray 0.3s' }}
            />
            <text x="90" y="86" textAnchor="middle" fontSize="38" fontWeight={700} fill="var(--tb-navy)" fontFamily="var(--tb-font)">
              {Math.round(totalProgress)}%
            </text>
            <text x="90" y="108" textAnchor="middle" fontSize="11" fill="var(--tb-ink-3)" fontFamily="var(--tb-font-mono)">
              남은 시간 약 {remainingSec}초
            </text>
          </svg>
        </div>

        <div className="wf-h2" style={{ marginBottom: 8 }}>{menuName} 분석 중이에요</div>
        <p className="wf-muted" style={{ fontSize: 12.5, marginBottom: 28 }}>
          약 30초 정도 걸려요. 잠시만 기다려주세요.
        </p>

        <div style={{ textAlign: 'left', display: 'grid', gap: 10, marginBottom: 12 }}>
          {STEPS.map((s, i) => {
            const state =
              i < activeStep  ? 'done' :
              i === activeStep ? 'active' :
              'pending';
            return (
              <div key={s.n} style={{
                display: 'grid', gridTemplateColumns: '24px 1fr 60px', gap: 10, alignItems: 'center',
                padding: '10px 14px',
                border: '1px solid ' + (state === 'active' ? 'var(--tb-navy)' : 'var(--tb-line-2)'),
                borderRadius: 8,
                background: state === 'active' ? 'var(--tb-navy-soft)' : '#fff',
                transition: 'all 0.2s',
              }}>
                <span style={{
                  width: 22, height: 22, borderRadius: 999,
                  background: state === 'done' ? 'var(--tb-navy)' : '#fff',
                  border: '1.5px solid ' + (state === 'pending' ? 'var(--tb-line)' : 'var(--tb-navy)'),
                  color: state === 'done' ? '#fff' : 'var(--tb-navy)',
                  fontSize: 11, fontWeight: 700,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>{state === 'done' ? '✓' : s.n}</span>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>{s.t}</div>
                  <div className="wf-mono">{s.sub}</div>
                </div>
                <span className="wf-mono" style={{
                  textAlign: 'right',
                  color: state === 'active' ? 'var(--tb-navy)' : state === 'done' ? 'var(--tb-ink-3)' : 'var(--tb-ink-4)'
                }}>
                  {state === 'done' ? '완료' : state === 'active' ? '진행중' : '대기'}
                </span>
              </div>
            );
          })}
        </div>
      </Box>
    </div>
  );
}
