'use client';
import React, { useState } from 'react';

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
 * 미결정 항목 마커. 데모에서 "🔶 합의 필요" 영역을 시각적으로 표시.
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

/**
 * 천 단위 콤마 입력 필드.
 * - 포커스 중: 숫자만 입력 (콤마 자동 제거)
 * - 블러 시: toLocaleString으로 콤마 포맷
 */
export function NumberInput({
  value, onChange, lg, style, ...rest
}: {
  value: number;
  onChange: (n: number) => void;
  lg?: boolean;
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type' | 'onFocus' | 'onBlur'>) {
  const [focused, setFocused] = useState(false);
  const [raw, setRaw] = useState('');

  return (
    <input
      className={'wf-input' + (lg ? ' wf-input-lg' : '')}
      type="text"
      inputMode="numeric"
      style={style}
      value={focused ? raw : (value ? value.toLocaleString('ko-KR') : '')}
      onFocus={() => { setFocused(true); setRaw(value ? String(value) : ''); }}
      onBlur={() => { setFocused(false); onChange(Number(raw) || 0); }}
      onChange={(e) => {
        const digits = e.target.value.replace(/[^0-9]/g, '');
        setRaw(digits);
        onChange(Number(digits) || 0);
      }}
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
