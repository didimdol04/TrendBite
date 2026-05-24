'use client';
import { useState } from 'react';
import { useUser } from '@/lib/userContext';
import { Box, Btn, Input, Field } from './Primitives';
import type { UserProfile } from '@/lib/types';

const KNOWN_NAMES_KEY = 'trendbite_known_nicknames';

function readKnown(): Record<string, UserProfile> {
  if (typeof window === 'undefined') return {};
  try {
    return JSON.parse(localStorage.getItem(KNOWN_NAMES_KEY) || '{}') as Record<string, UserProfile>;
  } catch {
    return {};
  }
}

function rand4(): string {
  return Math.random().toString(36).slice(2, 6).toUpperCase();
}

/**
 * 가게명 입력 모달.
 * - 신규: 가게명 + 랜덤 4자리 식별자(#XXXX)로 등록
 * - 기존: 베이스 가게명으로 조회 → 저장된 전체 식별자 복원
 * - localStorage: { [베이스가게명]: UserProfile } 구조 유지
 */
export function NicknameGate({
  open, onClose, onProceedAsGuest, title = '시작하기 전에', description,
}: {
  open: boolean;
  onClose: () => void;
  onProceedAsGuest?: () => void;
  title?: string;
  description?: string;
}) {
  const { login } = useUser();
  const [storeName, setStoreName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [suffix] = useState(rand4); // 마운트 시 1회 생성, 신규 가입에만 사용

  if (!open) return null;

  const trimmed = storeName.trim();
  const existing = trimmed ? readKnown()[trimmed] : undefined;
  const returning = !!existing;

  // 표시용 전체 가게명: 기존은 저장된 nickname(suffix 포함), 신규는 입력값 + 생성된 suffix
  const displayName = returning
    ? existing!.nickname
    : trimmed
      ? `${trimmed} #${suffix}`
      : '';

  const submit = () => {
    setError(null);
    if (!trimmed) { setError('가게명을 입력해주세요'); return; }
    if (trimmed.length > 30) { setError('30자 이하로 입력해주세요'); return; }
    try {
      login(displayName);
      onClose();
    } catch (e: any) {
      setError(e?.message || '오류가 발생했습니다');
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed', inset: 0, background: 'rgba(15, 19, 32, 0.55)',
        zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: 16,
      }}
      onClick={onClose}
    >
      <Box
        onClick={(e) => e.stopPropagation()}
        style={{ width: '100%', maxWidth: 460, padding: 32, background: '#fff' }}
      >
        <div className="wf-eyebrow" style={{ marginBottom: 10 }}>USER · 가게명</div>
        <h2 className="wf-h1" style={{ fontSize: 22, marginBottom: 8 }}>{title}</h2>
        <p className="wf-muted" style={{ fontSize: 12.5, margin: '0 0 20px' }}>
          {description || '가게명을 입력하시면 분석 결과를 저장하고 실판매를 기록할 수 있어요. 같은 가게명으로 다시 들어오시면 기존 분석 이력을 다시 보실 수 있어요.'}
        </p>

        <Field label="가게명" required>
          <Input
            lg
            placeholder="예: 맛나분식"
            value={storeName}
            onChange={(e) => setStoreName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') submit(); }}
            maxLength={30}
            autoFocus
          />
        </Field>

        {/* 기존 가게 — 저장된 식별자 표시 */}
        {returning && (
          <div style={{
            marginTop: 10, padding: '8px 12px',
            background: 'var(--tb-navy-soft)', borderRadius: 6,
            fontSize: 12, color: 'var(--tb-navy)',
          }}>
            <b>{displayName}</b> 님, 다시 오셨네요! 기존 분석 이력을 함께 보실 수 있어요.
          </div>
        )}

        {/* 신규 가게 — 등록될 식별자 미리보기 */}
        {!returning && trimmed && (
          <div style={{
            marginTop: 10, padding: '8px 12px',
            background: 'var(--tb-card)', borderRadius: 6,
            fontSize: 12, color: 'var(--tb-ink-2)',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
          }}>
            <span style={{ flexShrink: 0 }}>등록될 가게 식별자</span>
            <span style={{
              fontWeight: 700, color: 'var(--tb-navy)',
              fontFamily: 'var(--tb-font-mono)', fontSize: 11.5,
              textAlign: 'right',
            }}>
              {displayName}
            </span>
          </div>
        )}

        {error && (
          <div style={{ marginTop: 10, fontSize: 12, color: 'var(--tb-danger)' }}>
            {error}
          </div>
        )}

        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <Btn primary lg onClick={submit} style={{ width: '100%' }} disabled={!trimmed}>
            {returning
              ? `${displayName}(으)로 계속하기 →`
              : displayName
                ? `${displayName}로 시작하기 →`
                : '시작하기 →'}
          </Btn>
          {onProceedAsGuest && (
            <button
              type="button"
              onClick={() => { onProceedAsGuest(); onClose(); }}
              style={{
                background: 'transparent', border: 0,
                color: 'var(--tb-ink-3)', fontSize: 12,
                padding: '8px 0', cursor: 'pointer',
              }}
            >
              가게명 없이 게스트로 진행하기
              <span className="wf-mono" style={{ marginLeft: 8 }}>&middot; 1회성 &middot; 저장 안 됨</span>
            </button>
          )}
        </div>

        <div style={{
          marginTop: 18, padding: '10px 12px',
          background: '#FFF4D4', border: '1px dashed #E0A800', borderRadius: 6,
          fontSize: 11, color: '#806100',
        }}>
          🔶 <b>실서비스 합의 필요</b> · 현재 데모는 같은 디바이스 + 같은 가게명 매칭만으로 유저를 식별합니다. 실서비스에선 백엔드 User 엔티티 추가 + 비밀번호 또는 SMS/이메일 인증 필요.
        </div>
      </Box>
    </div>
  );
}
