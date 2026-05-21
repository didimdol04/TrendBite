'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Box, Btn, Input, Field, Badge, PendingMarker, fmt } from '@/components/Primitives';
import { SketchLine } from '@/components/Charts';
import { useUser } from '@/lib/userContext';
import { recordSales, getSalesCorrection } from '@/lib/api';
import type { SavedAnalysis, SavedSalesRecord, SalesCorrectionResponse } from '@/lib/types';

/**
 * 🆕 실판매 데이터 기록 페이지 (회의록 명시 신규 페이지).
 *
 * - 닉네임 로그인된 유저만 접근 가능
 * - 분석 결과에 연결된 menuId를 기반으로 일일 실판매를 기록
 * - 3일 이상 기록 누적 시 예측 보정값 제공 (백엔드 GET /api/v1/sales/correction/{menuId})
 * - 동일 날짜 중복 금지 (백엔드 unique constraint와 일치)
 */
export default function SalesRecordPage() {
  const params = useParams();
  const router = useRouter();
  const { state, getAnalyses, saveSalesRecord, getSalesRecords } = useUser();

  const menuId = Number(params.menuId);
  const [analysis, setAnalysis] = useState<SavedAnalysis | null>(null);
  const [records, setRecords] = useState<SavedSalesRecord[]>([]);
  const [correction, setCorrection] = useState<SalesCorrectionResponse | null>(null);
  const [correctionError, setCorrectionError] = useState<string | null>(null);

  // 입력 폼
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [quantity, setQuantity] = useState('');
  const [revenue, setRevenue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // 메뉴 단가 자동 채움
  useEffect(() => {
    if (state.kind !== 'identified') return;
    const found = getAnalyses().find(a => a.menuId === menuId);
    if (found) {
      setAnalysis(found);
    }
    setRecords(getSalesRecords(menuId));
  }, [state, menuId, getAnalyses, getSalesRecords]);

  // 3일 이상 기록 시 보정값 조회
  useEffect(() => {
    if (records.length >= 3 && analysis) {
      getSalesCorrection(menuId, records.length, analysis.response.finance.data.unitEconomics.sellingPrice)
        .then(res => { setCorrection(res); setCorrectionError(null); })
        .catch(e => setCorrectionError(e.message || '보정 데이터 조회 실패'));
    } else {
      setCorrection(null);
      setCorrectionError(null);
    }
  }, [records, menuId, analysis]);

  if (state.kind !== 'identified') {
    return (
      <div style={{ padding: 80, textAlign: 'center' }}>
        <div className="wf-h2" style={{ marginBottom: 8 }}>실판매 기록은 닉네임 로그인이 필요해요</div>
        <Link href="/"><Btn primary>홈으로</Btn></Link>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div style={{ padding: 80, textAlign: 'center' }}>
        <div className="wf-h2" style={{ marginBottom: 8 }}>해당 분석을 찾을 수 없어요</div>
        <p className="wf-muted" style={{ marginBottom: 20 }}>
          분석 이력에서 항목을 선택해 진입해주세요.
        </p>
        <Link href="/history"><Btn primary>이력 보기</Btn></Link>
      </div>
    );
  }

  const sellingPrice = analysis.response.finance.data.unitEconomics.sellingPrice;
  const expectedDaily = 35; // 데모: 위저드에서 입력한 expectedDailySales가 분석 응답에 없으므로 default

  const onAddRecord = async () => {
    setError(null);
    const q = Number(quantity);
    const r = Number(revenue);
    if (!date) return setError('날짜를 선택해주세요');
    if (!q || q < 0) return setError('판매량을 올바르게 입력해주세요');
    if (!r || r < 0) return setError('매출을 올바르게 입력해주세요');

    setSaving(true);
    try {
      // 백엔드 호출 (mock)
      await recordSales({ menuId, salesDate: date, actualQuantity: q, actualRevenue: r });
      // 로컬 저장
      saveSalesRecord({ menuId, salesDate: date, actualQuantity: q, actualRevenue: r });
      setRecords(getSalesRecords(menuId));
      setQuantity('');
      setRevenue('');
      // 다음 날짜로 자동 진행
      const d = new Date(date); d.setDate(d.getDate() + 1);
      setDate(d.toISOString().slice(0, 10));
    } catch (e: any) {
      setError(e.message || '저장 중 오류가 발생했습니다');
    } finally {
      setSaving(false);
    }
  };

  // 추천 매출 = quantity × sellingPrice
  const suggestedRevenue = quantity ? Number(quantity) * sellingPrice : 0;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'clamp(24px, 5vw, 40px) clamp(16px, 5vw, 56px)' }}>
      {/* 상단 메타 */}
      <div style={{ marginBottom: 24 }}>
        <div className="wf-eyebrow" style={{ marginBottom: 8 }}>실판매 데이터 기록</div>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <h1 className="wf-h1" style={{ fontSize: 26 }}>
            <span style={{ color: 'var(--tb-navy)' }}>{analysis.menuName}</span>의 실제 판매를 기록해주세요
          </h1>
          <Link href={`/result/${analysis.id}`}>
            <Btn>← 분석 결과로</Btn>
          </Link>
        </div>
        <p className="wf-muted" style={{ fontSize: 12.5, marginTop: 8, marginBottom: 0 }}>
          매일 기록하시면, 3일 누적 이후부터 <b>실제 판매를 반영한 예측 보정값</b>을 제공해드려요.
        </p>
      </div>

      <div className="record-grid">
        {/* 좌측: 입력 폼 */}
        <Box style={{ padding: 24 }}>
          <div className="wf-h3" style={{ marginBottom: 12 }}>오늘의 실판매</div>

          <Field label="판매 날짜" required style={{ marginBottom: 12 }}>
            <Input type="date" value={date} onChange={e => setDate(e.target.value)} max={new Date().toISOString().slice(0, 10)} />
          </Field>
          <Field label="실제 판매 수량 (개)" required style={{ marginBottom: 12 }}>
            <Input
              type="number" min={0}
              placeholder="예: 28"
              value={quantity}
              onChange={e => setQuantity(e.target.value)}
            />
          </Field>
          <Field label="실제 매출 (원)" required
            hint={quantity ? `메뉴 단가 기준 추천값: ${fmt.won(suggestedRevenue)} (할인/세트 등 차이 가능)` : '메뉴 단가 × 판매량 기본값'}
            style={{ marginBottom: 16 }}
          >
            <Input
              type="number" min={0}
              placeholder={fmt.num(suggestedRevenue)}
              value={revenue}
              onChange={e => setRevenue(e.target.value)}
            />
          </Field>

          {error && (
            <div style={{ marginBottom: 12, padding: '8px 12px', background: '#FCE7E7', color: 'var(--tb-danger)', borderRadius: 6, fontSize: 12 }}>
              {error}
            </div>
          )}

          <Btn primary lg onClick={onAddRecord} disabled={saving} style={{ width: '100%' }}>
            {saving ? '저장 중...' : '＋ 기록 추가'}
          </Btn>

          <div style={{ marginTop: 16, padding: '10px 12px', background: 'var(--tb-canvas)', borderRadius: 6, fontSize: 11.5, color: 'var(--tb-ink-3)' }}>
            <div style={{ marginBottom: 4 }}>📅 <b>매일 1회 기록 권장</b> · 동일 날짜 중복 불가</div>
            <div>📦 <b>CSV 일괄 업로드</b>는 추후 지원 예정 — <PendingMarker>업로드 형식 합의 필요</PendingMarker></div>
          </div>
        </Box>

        {/* 우측: 기록 + 보정 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* 보정 결과 */}
          <Box style={{ padding: 24, background: correction ? 'var(--tb-navy-soft)' : '#fff', borderColor: correction ? 'transparent' : 'var(--tb-line)' }}>
            <div className="wf-h3" style={{ marginBottom: 8, color: correction ? 'var(--tb-navy)' : 'var(--tb-ink)' }}>
              📊 예측 보정 결과
            </div>
            {correction ? (
              <>
                <p className="wf-mono" style={{ marginBottom: 16, color: 'var(--tb-navy)' }}>
                  최근 {correction.data.basedOnDays}일 평균을 기반으로 보정됨
                </p>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, flexWrap: 'wrap' }}>
                  <div>
                    <div className="wf-mono">기존 예측</div>
                    <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--tb-ink-3)', textDecoration: 'line-through' }}>
                      {correction.data.originalExpectedDailySales}개/일
                    </div>
                  </div>
                  <div style={{ fontSize: 20, color: 'var(--tb-ink-3)' }}>→</div>
                  <div>
                    <div className="wf-mono">보정된 예측</div>
                    <div style={{ fontSize: 28, fontWeight: 800, color: 'var(--tb-navy)' }}>
                      {correction.data.correctedExpectedDailySales}개/일
                    </div>
                  </div>
                  <Badge tone={correction.data.correctionRate >= 1 ? 'success' : 'amber'}>
                    {correction.data.correctionRate >= 1 ? '+' : ''}
                    {((correction.data.correctionRate - 1) * 100).toFixed(1)}%
                  </Badge>
                </div>
                <div style={{ marginTop: 12, fontSize: 11.5 }}>
                  <PendingMarker>보정값을 결과 페이지 재무 시뮬레이션에 자동 반영하는 동선 합의 필요</PendingMarker>
                </div>
              </>
            ) : correctionError ? (
              <div style={{ fontSize: 12, color: 'var(--tb-ink-3)' }}>
                ℹ️ {correctionError}
              </div>
            ) : (
              <div style={{ fontSize: 12, color: 'var(--tb-ink-3)' }}>
                <div style={{ marginBottom: 6 }}>아직 기록이 <b>{records.length}일</b>치 쌓였어요.</div>
                <div>최소 <b>3일</b> 누적 시 자동으로 보정 결과가 표시됩니다.</div>
              </div>
            )}
          </Box>

          {/* 기록 목록 */}
          <Box style={{ padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 12 }}>
              <div className="wf-h3">기록된 판매 ({records.length}일)</div>
              <span className="wf-mono">최근 {Math.min(records.length, 14)}일</span>
            </div>

            {records.length === 0 ? (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--tb-ink-3)', fontSize: 13 }}>
                아직 기록이 없습니다. 첫 데이터를 입력해보세요.
              </div>
            ) : (
              <>
                {/* 시계열 차트 */}
                <div style={{ marginBottom: 16, padding: 12, background: 'var(--tb-canvas)', borderRadius: 6 }}>
                  <SketchLine
                    width={500} height={80}
                    data={records.slice(-14).map(r => r.actualQuantity)}
                    fill
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
                    <span className="wf-mono">{records[Math.max(0, records.length - 14)]?.salesDate}</span>
                    <span className="wf-mono">{records[records.length - 1]?.salesDate}</span>
                  </div>
                </div>

                {/* 목록 */}
                <div style={{ maxHeight: 300, overflowY: 'auto', border: '1px solid var(--tb-line-2)', borderRadius: 6 }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12.5 }}>
                    <thead style={{ background: 'var(--tb-canvas)', position: 'sticky', top: 0 }}>
                      <tr>
                        <th style={th}>날짜</th>
                        <th style={th}>판매량</th>
                        <th style={th}>매출</th>
                        <th style={th}>객단가</th>
                      </tr>
                    </thead>
                    <tbody>
                      {[...records].reverse().map((r) => {
                        const avg = r.actualQuantity ? r.actualRevenue / r.actualQuantity : 0;
                        const deviation = ((avg - sellingPrice) / sellingPrice) * 100;
                        return (
                          <tr key={r.id} style={{ borderTop: '1px solid var(--tb-line-2)' }}>
                            <td style={td}>{r.salesDate}</td>
                            <td style={td}>{r.actualQuantity}개</td>
                            <td style={td}>{fmt.won(r.actualRevenue)}</td>
                            <td style={td}>
                              {fmt.won(Math.round(avg))}
                              {Math.abs(deviation) > 0.5 && (
                                <span style={{ marginLeft: 6, color: 'var(--tb-ink-4)', fontSize: 10.5 }}>
                                  ({deviation > 0 ? '+' : ''}{deviation.toFixed(0)}%)
                                </span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </Box>
        </div>
      </div>

      <style jsx>{`
        .record-grid {
          display: grid;
          grid-template-columns: minmax(320px, 1fr) minmax(320px, 1.6fr);
          gap: 20px;
        }
        @media (max-width: 768px) {
          .record-grid { grid-template-columns: 1fr; }
        }
      `}</style>
    </div>
  );
}

const th: React.CSSProperties = {
  padding: '10px 12px', textAlign: 'left',
  fontSize: 11, fontWeight: 600, color: 'var(--tb-ink-2)',
};
const td: React.CSSProperties = {
  padding: '10px 12px', color: 'var(--tb-ink)',
};
