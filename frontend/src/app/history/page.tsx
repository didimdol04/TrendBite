'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Box, Btn, Badge, Chip, Input } from '@/components/Primitives';
import { SketchLine } from '@/components/Charts';
import { NicknameGate } from '@/components/NicknameGate';
import { useUser } from '@/lib/userContext';
import { deleteAnalysisApi } from '@/lib/api';
import { groupByMenu } from '@/lib/compare';
import { toast } from '@/components/Toast';
import type { SavedAnalysis } from '@/lib/types';

function hasTrendChart(it: SavedAnalysis): boolean {
  const series = it.response.trend?.interest_series;
  return !it.noTrend && Array.isArray(series) && series.length >= 2;
}

const VERDICT_TONE: Record<string, 'navy' | 'amber' | 'grey' | 'dark'> = {
  '도입 권장': 'navy',
  '신중 검토': 'amber',
  '불확실': 'grey',
  '보류': 'dark',
};

export default function HistoryPage() {
  const router = useRouter();
  const { state, getAnalyses, getSalesRecords, deleteAnalysis } = useUser();
  const [items, setItems] = useState<SavedAnalysis[]>([]);
  const [search, setSearch] = useState('');
  const [showGate, setShowGate] = useState(false);
  const [confirmId, setConfirmId] = useState<string | null>(null);

  // 비교 모드
  const [compareMode, setCompareMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  useEffect(() => {
    if (state.kind === 'identified') {
      setItems(getAnalyses());
    } else {
      setShowGate(true);
    }
  }, [state, getAnalyses]);

  // 비교 모드 OFF 시 선택 초기화
  useEffect(() => {
    if (!compareMode) setSelectedIds([]);
  }, [compareMode]);

  const handleDelete = async (it: SavedAnalysis) => {
    deleteAnalysis(it.id);
    setItems(getAnalyses());
    setConfirmId(null);
    if (it.menuId) {
      deleteAnalysisApi(it.menuId).catch(() => {});
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      if (prev.includes(id)) return prev.filter(x => x !== id);
      if (prev.length >= 3) {
        toast.info('최대 3개까지 선택할 수 있어요.');
        return prev;
      }
      return [...prev, id];
    });
  };

  const handleCompare = () => {
    if (selectedIds.length < 2) {
      toast.info('2개 이상 선택해야 비교할 수 있어요.');
      return;
    }
    router.push(`/history/compare?ids=${selectedIds.join(',')}`);
  };

  const handleAutoGroup = (ids: string[]) => {
    router.push(`/history/compare?ids=${ids.join(',')}`);
  };

  const filtered = items.filter(i =>
    !search || i.menuName.toLowerCase().includes(search.toLowerCase())
  );

  const recommendedGroups = groupByMenu(items);

  const selectedItems = items.filter(i => selectedIds.includes(i.id));
  const categories = new Set(selectedItems.map(i => i.category));
  const hasCategoryMismatch = categories.size > 1;

  if (state.kind !== 'identified') {
    return (
      <>
        <div style={{
          padding: 80, textAlign: 'center', maxWidth: 480, margin: '0 auto',
        }}>
          <div className="wf-h1" style={{ fontSize: 22, marginBottom: 8 }}>닉네임이 필요해요</div>
          <p className="wf-muted" style={{ marginBottom: 20 }}>
            이력 페이지는 닉네임/가게명으로 식별된 유저만 볼 수 있어요.
          </p>
          <Btn primary lg onClick={() => setShowGate(true)}>닉네임 입력하기</Btn>
        </div>
        <NicknameGate open={showGate} onClose={() => setShowGate(false)} />
      </>
    );
  }

  return (
    <div style={{ maxWidth: 1280, margin: '0 auto', padding: 'clamp(24px, 5vw, 40px) clamp(16px, 5vw, 80px)', paddingBottom: compareMode && selectedIds.length > 0 ? 100 : undefined }}>
      {/* 헤더 */}
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end',
        marginBottom: 24, flexWrap: 'wrap', gap: 16,
      }}>
        <div>
          <div className="wf-eyebrow" style={{ marginBottom: 8 }}>분석 이력</div>
          <h1 className="wf-h1">
            <span style={{ color: 'var(--tb-navy)' }}>{state.user.nickname}</span>님이 분석한 메뉴 {items.length}개
          </h1>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <Input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="🔍 메뉴명 검색"
            style={{ width: 220 }}
          />
          {items.length >= 2 && (
            <Btn
              onClick={() => setCompareMode(m => !m)}
              style={compareMode ? {
                background: 'var(--tb-navy)', color: '#fff', borderColor: 'var(--tb-navy)',
              } : undefined}
            >
              {compareMode ? '✕ 비교 모드 끄기' : '⚖ 비교 모드'}
            </Btn>
          )}
          <Link href="/analyze">
            <Btn primary>＋ 새 분석</Btn>
          </Link>
        </div>
      </div>

      {/* 추천 비교 배너 */}
      {recommendedGroups.length > 0 && !compareMode && (
        <div style={{
          marginBottom: 20,
          padding: '12px 18px',
          background: 'var(--tb-navy-soft)',
          border: '1px solid var(--tb-line)',
          borderRadius: 'var(--tb-radius)',
          display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap',
        }}>
          <span style={{ fontSize: 13, color: 'var(--tb-navy)', fontWeight: 600 }}>
            "{recommendedGroups[0].menuName}"을 {recommendedGroups[0].items.length}곳에서 분석했어요 — 한눈에 비교해볼까요?
          </span>
          <Btn
            style={{ height: 30, fontSize: 11.5, padding: '0 12px' }}
            onClick={() => handleAutoGroup(recommendedGroups[0].items.map(i => i.id))}
          >
            → 비교하기
          </Btn>
        </div>
      )}

      {/* 비교 모드 안내 */}
      {compareMode && (
        <div style={{
          marginBottom: 16, padding: '10px 16px',
          background: 'var(--tb-amber-soft)', border: '1px solid var(--tb-amber)',
          borderRadius: 'var(--tb-radius-sm)', fontSize: 12.5, color: '#806100',
        }}>
          비교할 분석 결과를 최대 3개 선택하세요. ({selectedIds.length}/3 선택됨)
        </div>
      )}

      {filtered.length === 0 ? (
        <Box dashed style={{ padding: 60, textAlign: 'center', background: '#fff' }}>
          <div className="wf-h3" style={{ marginBottom: 8 }}>
            {items.length === 0 ? '아직 분석한 메뉴가 없어요' : '검색 결과가 없어요'}
          </div>
          <p className="wf-muted" style={{ marginBottom: 16, fontSize: 12.5 }}>
            {items.length === 0
              ? '첫 메뉴를 분석해서 이력을 시작해보세요.'
              : '다른 검색어로 시도해보세요.'}
          </p>
          {items.length === 0 && (
            <Link href="/analyze"><Btn primary>지금 분석하기 →</Btn></Link>
          )}
        </Box>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
          {filtered.map((it) => {
            const score = it.response.report.scores.total;
            const verdict = it.response.report.verdict;
            const salesCount = getSalesRecords(it.menuId).length;
            const isSelected = selectedIds.includes(it.id);
            return (
              <Box
                key={it.id}
                style={{
                  padding: 20, position: 'relative',
                  ...(compareMode && isSelected ? {
                    border: '2px solid var(--tb-navy)',
                    boxShadow: '0 0 0 3px rgba(26,42,108,0.12)',
                  } : {}),
                }}
              >
                {/* 비교 모드 체크박스 */}
                {compareMode && (
                  <div
                    onClick={() => toggleSelect(it.id)}
                    style={{
                      position: 'absolute', top: 12, left: 12,
                      width: 20, height: 20,
                      border: isSelected ? '2px solid var(--tb-navy)' : '1.5px solid var(--tb-line)',
                      borderRadius: 4,
                      background: isSelected ? 'var(--tb-navy)' : '#fff',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      cursor: 'pointer', zIndex: 2,
                      transition: 'all 0.15s',
                    }}
                  >
                    {isSelected && <span style={{ color: '#fff', fontSize: 12, fontWeight: 700, lineHeight: 1 }}>✓</span>}
                  </div>
                )}

                <div style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
                  marginBottom: 14,
                  ...(compareMode ? { paddingLeft: 28 } : {}),
                }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Link href={compareMode ? '#' : `/result/${it.id}`}>
                      <div
                        className="wf-h2"
                        style={{ marginBottom: 4, color: 'var(--tb-ink)', cursor: compareMode ? 'default' : 'pointer' }}
                        onClick={compareMode ? () => toggleSelect(it.id) : undefined}
                      >
                        {it.menuName}
                      </div>
                    </Link>
                    <div className="wf-mono">
                      {it.createdAt.slice(0, 10)} · {it.address?.slice(0, 18) || '주소 없음'}
                    </div>
                  </div>
                  <Badge tone={VERDICT_TONE[verdict] || 'grey'}>{verdict}</Badge>
                </div>
                <div
                  style={{ height: 56, marginBottom: 12, cursor: compareMode ? 'pointer' : 'default' }}
                  onClick={compareMode ? () => toggleSelect(it.id) : undefined}
                >
                  {hasTrendChart(it) ? (
                    <SketchLine
                      width={260} height={56}
                      data={it.response.trend.interest_series!}
                      fill
                    />
                  ) : (
                    <div style={{
                      height: '100%', display: 'flex', alignItems: 'center',
                      justifyContent: 'center', gap: 6,
                      border: '1px dashed var(--tb-line-2)', borderRadius: 4,
                      color: 'var(--tb-ink-3)', fontSize: 11,
                    }}>
                      <span>📉</span>
                      <span>{it.noTrend ? '트렌드 분석 미포함' : '관심도 데이터 없음'}</span>
                    </div>
                  )}
                </div>
                <div style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
                  borderTop: '1px solid var(--tb-line-2)', paddingTop: 12, marginBottom: 12,
                }}>
                  <span className="wf-mono">종합 점수</span>
                  <span style={{ fontSize: 22, fontWeight: 700, color: 'var(--tb-navy)' }}>
                    {score}
                    <span style={{ fontSize: 12, color: 'var(--tb-ink-3)', fontWeight: 500 }}>/100</span>
                  </span>
                </div>

                {/* 비교 모드에서는 삭제 UI 대신 선택 안내 */}
                {compareMode ? (
                  <Btn
                    style={{
                      width: '100%', height: 32, fontSize: 11,
                      ...(isSelected ? { background: 'var(--tb-navy)', color: '#fff', borderColor: 'var(--tb-navy)' } : {}),
                    }}
                    onClick={() => toggleSelect(it.id)}
                  >
                    {isSelected ? '✓ 선택됨' : '선택하기'}
                  </Btn>
                ) : confirmId === it.id ? (
                  <div style={{
                    display: 'flex', gap: 6, alignItems: 'center',
                    padding: '8px 10px', borderRadius: 6,
                    background: '#fff5f5', border: '1px solid #fca5a5',
                  }}>
                    <span style={{ fontSize: 11, color: '#b91c1c', flex: 1 }}>정말 삭제할까요?</span>
                    <Btn
                      onClick={() => handleDelete(it)}
                      style={{ height: 28, fontSize: 11, background: '#ef4444', color: '#fff', border: 'none' }}
                    >삭제</Btn>
                    <Btn
                      onClick={() => setConfirmId(null)}
                      style={{ height: 28, fontSize: 11 }}
                    >취소</Btn>
                  </div>
                ) : (
                  <div style={{ display: 'flex', gap: 6 }}>
                    <Link href={`/result/${it.id}`} style={{ flex: 1 }}>
                      <Btn style={{ width: '100%', height: 32, fontSize: 11 }}>📄 분석 결과 보기</Btn>
                    </Link>
                    <Link href={`/sales-record/${it.menuId}`} style={{ flex: 1 }}>
                      <Btn style={{ width: '100%', height: 32, fontSize: 11 }}>
                        📊 실판매 기록 ({salesCount})
                      </Btn>
                    </Link>
                    <Btn
                      onClick={() => setConfirmId(it.id)}
                      style={{ height: 32, fontSize: 11, padding: '0 10px', color: '#ef4444' }}
                    >삭제</Btn>
                  </div>
                )}
              </Box>
            );
          })}
        </div>
      )}

      {/* 하단 sticky 액션바 */}
      {compareMode && selectedIds.length > 0 && (
        <div style={{
          position: 'fixed', bottom: 0, left: 0, right: 0,
          background: 'var(--tb-navy)', color: '#fff',
          padding: '14px 24px',
          display: 'flex', alignItems: 'center', gap: 12,
          zIndex: 100, boxShadow: '0 -4px 16px rgba(0,0,0,0.15)',
          flexWrap: 'wrap',
        }}>
          <span style={{ fontSize: 13.5, fontWeight: 600 }}>
            {selectedIds.length}개 선택됨
          </span>
          {hasCategoryMismatch && (
            <Chip style={{
              background: 'rgba(224,168,0,0.2)', color: '#E0A800',
              borderColor: 'rgba(224,168,0,0.4)', fontSize: 11.5,
            }}>
              ⚠ 카테고리 불일치
            </Chip>
          )}
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
            <Btn
              style={{ height: 36, fontSize: 12, color: 'rgba(255,255,255,0.7)', background: 'transparent', border: '1px solid rgba(255,255,255,0.2)' }}
              onClick={() => setSelectedIds([])}
            >
              선택 초기화
            </Btn>
            <Btn
              style={{ height: 36, fontSize: 12.5, background: 'var(--tb-amber)', color: '#1A1100', border: 'none', fontWeight: 700 }}
              onClick={handleCompare}
            >
              비교하기 →
            </Btn>
          </div>
        </div>
      )}
    </div>
  );
}
