'use client';
import Link from 'next/link';
import { useUser } from '@/lib/userContext';
import { usePathname } from 'next/navigation';

export default function Header() {
  const { state, logout } = useUser();
  const pathname = usePathname();

  return (
    <header style={{
      height: 56, borderBottom: '1px solid var(--tb-line)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', background: '#fff',
      position: 'sticky', top: 0, zIndex: 50,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
        <Link href="/" style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
          <span style={{ fontWeight: 700, fontSize: 16, color: 'var(--tb-navy)', letterSpacing: '-0.01em' }}>
            TrendBite
          </span>
          <span style={{ color: 'var(--tb-ink-3)', fontWeight: 500, fontSize: 12 }}>두쫀쿠</span>
        </Link>
        <nav className="hide-mobile" style={{ display: 'flex', gap: 16, fontSize: 12.5, color: 'var(--tb-ink-2)' }}>
          <Link href="/" style={navStyle(pathname === '/')}>홈</Link>
          {state.kind === 'identified' && (
            <Link href="/history" style={navStyle(pathname?.startsWith('/history'))}>이력</Link>
          )}
        </nav>
      </div>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        {state.kind === 'identified' ? (
          <>
            <span className="wf-mono hide-mobile" style={{ fontSize: 11 }}>
              👤 {state.user.nickname}
            </span>
            <button
              type="button"
              onClick={() => { if (confirm('현재 닉네임을 해제하고 게스트로 전환할까요? (저장된 분석/실판매 기록은 같은 닉네임으로 다시 로그인 시 다시 보입니다)')) logout(); }}
              className="wf-btn"
              style={{ height: 30, padding: '0 10px', fontSize: 11 }}
            >
              로그아웃
            </button>
          </>
        ) : (
          <span className="wf-mono hide-mobile" style={{ fontSize: 11 }}>게스트 모드</span>
        )}
        <Link href="/analyze">
          <button className="wf-btn wf-btn-primary" style={{ height: 32, fontSize: 12 }}>
            분석 시작
          </button>
        </Link>
      </div>
    </header>
  );
}

function navStyle(active: boolean | undefined): React.CSSProperties {
  return {
    color: active ? 'var(--tb-navy)' : 'var(--tb-ink-2)',
    fontWeight: active ? 700 : 500,
    borderBottom: active ? '2px solid var(--tb-navy)' : '2px solid transparent',
    paddingBottom: 2,
  };
}
