'use client';
import { useState } from 'react';
import { useUser } from '@/lib/userContext';
import { Box, Btn, Input, Field, Badge } from './Primitives';

/**
 * 닉네임/가게명 입력 모달.
 *
 * 회의록 결정사항:
 *  - 로그인/회원가입 X, 닉네임 or 가게명을 필수 입력 받아 유저 식별
 *  - 신규 유저(DB에 처음) / 기존 유저(매칭) 분기
 *  - 게스트 모드: 1회성 분석만 가능, 저장 X
 */
export function NicknameGate({
  open, onClose, onProceedAsGuest, title = '시작하기 전에',
  description,
}: {
  open: boolean;
  onClose: () => void;
  onProceedAsGuest?: () => void;
  title?: string;
  description?: string;
}) {
  const { login, isReturning } = useUser();
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState<string | null>(null);
  const returning = nickname.trim() && isReturning(nickname);

  if (!open) return null;

  const submit = () => {
    setError(null);
    const v = nickname.trim();
    if (!v) {
      setError('닉네임 또는 가게명을 입력해주세요');
      return;
    }
    if (v.length > 30) {
      setError('30자 이하로 입력해주세요');
      return;
    }
    try {
      login(v);
      onClose();
    } catch (e: any) {
      setError(e.message || '오류가 발생했습니다');
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
        <div className="wf-eyebrow" style={{ marginBottom: 10 }}>USER · 닉네임 / 가게명</div>
        <h2 className="wf-h1" style={{ fontSize: 22, marginBottom: 8 }}>{title}</h2>
        <p className="wf-muted" style={{ fontSize: 12.5, margin: '0 0 20px' }}>
          {description || '닉네임 또는 가게명을 입력하시면 분석 결과를 저장하고 실판매를 기록할 수 있어요. 같은 이름으로 다시 들어오시면 기존 분석 이력을 다시 보실 수 있어요.'}
        </p>

        <Field label="닉네임 또는 가게명" required>
          <Input
            lg
            placeholder="예: 두쫀쿠분식 / 마라탕맛집사장"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') submit(); }}
            maxLength={30}
            autoFocus
          />
        </Field>

        {returning && (
          <div style={{
            marginTop: 10, padding: '8px 12px',
            background: 'var(--tb-navy-soft)', borderRadius: 6,
            fontSize: 12, color: 'var(--tb-navy)',
          }}>
            ✓ <b>{nickname.trim()}</b> 님, 다시 오셨네요! 기존 분석 이력을 함께 보실 수 있어요.
          </div>
        )}
        {error && (
          <div style={{ marginTop: 10, fontSize: 12, color: 'var(--tb-danger)' }}>
            {error}
          </div>
        )}

        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <Btn primary lg onClick={submit} style={{ width: '100%' }}>
            {returning ? `${nickname.trim()}(으)로 계속하기 →` : '시작하기 →'}
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
              닉네임 없이 게스트로 진행하기
              <span className="wf-mono" style={{ marginLeft: 8 }}>· 1회성 · 저장 안 됨</span>
            </button>
          )}
        </div>

        <div style={{
          marginTop: 18, padding: '10px 12px',
          background: '#FFF4D4', border: '1px dashed #E0A800', borderRadius: 6,
          fontSize: 11, color: '#806100',
        }}>
          🔶 <b>실서비스 합의 필요</b> · 현재 데모는 같은 디바이스 + 같은 닉네임 매칭만으로 유저를 식별합니다. 실서비스에선 백엔드 User 엔티티 추가 + 비밀번호 또는 SMS/이메일 인증 필요.
        </div>
      </Box>
    </div>
  );
}
