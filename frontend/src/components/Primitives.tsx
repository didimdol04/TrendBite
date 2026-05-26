'use client';
import React, { useState, useRef, useEffect } from 'react';

export const Box = ({
  children, style, className = '', dashed = false, ...rest
}: React.HTMLAttributes<HTMLDivElement> & { dashed?: boolean }) => (
  <div
    className={(dashed ? 'wf-box-dashed ' : 'wf-box ') + className}
    style={style}
    {...rest}
  >
    {children}
  </div>
);

export const Chip = ({
  children, tone, style, onClick,
}: { children: React.ReactNode; tone?: 'navy' | 'amber' | null; style?: React.CSSProperties; onClick?: () => void }) => {
  const cls =
    tone === 'navy' ? 'wf-chip wf-chip-navy' :
    tone === 'amber' ? 'wf-chip wf-chip-amber' :
    'wf-chip';
  return (
    <span className={cls} style={{ ...(onClick && { cursor: 'pointer' }), ...style }} onClick={onClick}>
      {children}
    </span>
  );
};

export const Badge = ({
  children, tone = 'navy', style,
}: { children: React.ReactNode; tone?: 'navy' | 'amber' | 'grey' | 'dark' | 'danger' | 'success'; style?: React.CSSProperties }) => {
  const map: Record<string, string> = {
    navy: '', amber: ' wf-badge-amber', grey: ' wf-badge-grey',
    dark: ' wf-badge-dark', danger: ' wf-badge-danger', success: ' wf-badge-success',
  };
  return <span className={'wf-badge' + (map[tone] || '')} style={style}>{children}</span>;
};

export const Btn = ({
  children, primary, lg, style, ...rest
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { primary?: boolean; lg?: boolean }) => (
  <button
    type={rest.type || 'button'}
    className={'wf-btn' + (primary ? ' wf-btn-primary' : '') + (lg ? ' wf-btn-lg' : '')}
    style={style}
    {...rest}
  >
    {children}
  </button>
);

export const Input = ({
  lg, style, ...rest
}: React.InputHTMLAttributes<HTMLInputElement> & { lg?: boolean }) => (
  <input className={'wf-input' + (lg ? ' wf-input-lg' : '')} style={style} {...rest} />
);

export const Field = ({
  label, hint, required, children, style,
}: {
  label: string;
  hint?: string;
  required?: boolean;
  children: React.ReactNode;
  style?: React.CSSProperties;
}) => (
  <div style={style}>
    <label className="wf-field-label">
      {label}{required && <span style={{ color: 'var(--tb-danger)', marginLeft: 2 }}>*</span>}
    </label>
    {children}
    {hint && <div className="wf-mono" style={{ marginTop: 4 }}>{hint}</div>}
  </div>
);

export const Progress = ({ pct, style }: { pct: number; style?: React.CSSProperties }) => (
  <div className="wf-progress-track" style={style}>
    <div className="wf-progress-fill" style={{ width: Math.max(0, Math.min(100, pct)) + '%' }} />
  </div>
);

/**
 * 미결정 항목 마커. "🔶 합의 필요" 영역을 시각적으로 표시.
 */
export const PendingMarker = ({ children }: { children: React.ReactNode }) => (
  <span className="pending-marker">🔶 {children}</span>
);

export const PendingBox = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="pending-box">
    <div style={{ fontWeight: 700, marginBottom: 4 }}>🔶 {title}</div>
    <div>{children}</div>
  </div>
);

// 천 단위 콤마 입력 필드 — 타이핑 중에도 실시간으로 콤마가 표시됨
export function NumberInput({
  value, onChange, lg, style, ...rest
}: {
  value: number;
  onChange: (n: number) => void;
  lg?: boolean;
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type' | 'onFocus' | 'onBlur'>) {
  const inputRef = useRef<HTMLInputElement>(null);
  const isFocused = useRef(false);
  const [display, setDisplay] = useState(() => value ? value.toLocaleString('ko-KR') : '');

  useEffect(() => {
    if (!isFocused.current) {
      setDisplay(value ? value.toLocaleString('ko-KR') : '');
    }
  }, [value]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    const selStart = e.target.selectionStart ?? raw.length;
    const digitsBeforeCursor = raw.slice(0, selStart).replace(/\D/g, '').length;

    const digits = raw.replace(/\D/g, '');
    const num = Number(digits) || 0;
    const formatted = digits ? num.toLocaleString('ko-KR') : '';

    setDisplay(formatted);
    onChange(num);

    // 콤마 삽입으로 인한 커서 밀림 보정
    requestAnimationFrame(() => {
      const el = inputRef.current;
      if (!el) return;
      let count = 0;
      let pos = formatted.length;
      for (let i = 0; i < formatted.length; i++) {
        if (/\d/.test(formatted[i])) count++;
        if (count === digitsBeforeCursor) { pos = i + 1; break; }
      }
      el.setSelectionRange(pos, pos);
    });
  };

  return (
    <input
      ref={inputRef}
      className={'wf-input' + (lg ? ' wf-input-lg' : '')}
      type="text"
      inputMode="numeric"
      style={style}
      value={display}
      onFocus={() => { isFocused.current = true; }}
      onBlur={() => {
        isFocused.current = false;
        onChange(Number(display.replace(/\D/g, '')) || 0);
      }}
      onChange={handleChange}
      {...rest}
    />
  );
}

/* 숫자 포맷 헬퍼 */
export const fmt = {
  won: (n: number | null | undefined) => n == null ? '-' : n.toLocaleString('ko-KR') + '원',
  num: (n: number | null | undefined) => n == null ? '-' : n.toLocaleString('ko-KR'),
  pct: (n: number | null | undefined, digits = 1) => n == null ? '-' : (n * 100).toFixed(digits) + '%',
};
