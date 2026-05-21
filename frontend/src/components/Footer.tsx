import Link from 'next/link';

export default function Footer() {
  return (
    <footer style={{
      height: 48, borderTop: '1px solid var(--tb-line)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', background: '#fff', fontSize: 11.5, color: 'var(--tb-ink-3)',
    }}>
      <span>© 2026 TrendBite (졸업프로젝트 — 두쫀쿠 팀)</span>
      <span style={{ display: 'flex', gap: 14 }}>
        <Link href="/terms" style={{ color: 'var(--tb-ink-3)' }}>이용약관</Link>
        <Link href="/privacy" style={{ color: 'var(--tb-ink-3)' }}>개인정보처리방침</Link>
      </span>
    </footer>
  );
}
