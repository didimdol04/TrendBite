'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Box, Btn, Badge, Input, fmt } from '@/components/Primitives';
import { SketchLine } from '@/components/Charts';
import { NicknameGate } from '@/components/NicknameGate';
import { useUser } from '@/lib/userContext';
import type { SavedAnalysis } from '@/lib/types';

const VERDICT_TONE: Record<string, 'navy' | 'amber' | 'grey' | 'dark'> = {
  '도입 권장': 'navy',
  '신중 검토': 'amber',
  '불확실': 'grey',
  '보류': 'dark',
};

export default function HistoryPage() {
  const router = useRouter();
  const { state, getAnalyses, getSalesRecords } = useUser();
  const [items, setItems] = useState<SavedAnalysis[]>([]);
  const [search, setSearch] = useState('');
  const [showGate, setShowGate] = useState(false);

  useEffect(() => {
    if (state.kind === 'identified') {
      setItems(getAnalyses());
    } else {
      // 게스트는 이력 페이지 접근 불가 → 닉네임 게이트
      setShowGate(true);
    }
  }, [state, getAnalyses]);

  const filtered = items.filter(i =>
    !search || i.menuName.toLowerCase().includes(search.toLowerCase())
  );

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
    <div style={{ maxWidth: 1280, margin: '0 auto', padding: 'clamp(24px, 5vw, 40px) clamp(16px, 5vw, 80px)' }}>
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
        <div style={{ display: 'flex', gap: 8 }}>
          <Input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="🔍 메뉴명 검색"
            style={{ width: 220 }}
          />
          <Link href="/analyze">
            <Btn primary>＋ 새 분석</Btn>
          </Link>
        </div>
      </div>

      {filtered.length === 0 ? (
        <Box dashed style={{
          padding: 60, textAlign: 'center',
          background: '#fff',
        }}>
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
            return (
              <Box key={it.id} style={{ padding: 20, position: 'relative' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Link href={`/result/${it.id}`}>
                      <div className="wf-h2" style={{ marginBottom: 4, color: 'var(--tb-ink)', cursor: 'pointer' }}>
                        {it.menuName}
                      </div>
                    </Link>
                    <div className="wf-mono">
                      {it.createdAt.slice(0, 10)} · {it.address?.slice(0, 18) || '주소 없음'}
                    </div>
                  </div>
                  <Badge tone={VERDICT_TONE[verdict] || 'grey'}>{verdict}</Badge>
                </div>
                <div style={{ height: 56, marginBottom: 12 }}>
                  <SketchLine
                    width={260} height={56}
                    data={[20, 35, 45, 50, 60, 70, 65, 72]}
                    fill
                  />
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
                <div style={{ display: 'flex', gap: 6 }}>
                  <Link href={`/result/${it.id}`} style={{ flex: 1 }}>
                    <Btn style={{ width: '100%', height: 32, fontSize: 11 }}>📄 결과 보기</Btn>
                  </Link>
                  <Link href={`/sales-record/${it.menuId}`} style={{ flex: 1 }}>
                    <Btn style={{ width: '100%', height: 32, fontSize: 11 }}>
                      📊 실판매 ({salesCount})
                    </Btn>
                  </Link>
                </div>
              </Box>
            );
          })}
        </div>
      )}
    </div>
  );
}
