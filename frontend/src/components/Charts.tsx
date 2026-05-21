'use client';
import React from 'react';

/* 와이어프레임의 jitterPath 그대로 — 정갈한 박스 + sketchy 차트 룩 */
function jitterPath(points: [number, number][], jitter = 1.0): string {
  return points.map((p, i) => {
    const dx = Math.sin(i * 12.9) * jitter;
    const dy = Math.cos(i * 7.1) * jitter;
    return (i === 0 ? 'M' : 'L') + (p[0] + dx).toFixed(1) + ' ' + (p[1] + dy).toFixed(1);
  }).join(' ');
}

export const SketchLine = ({
  width = 220, height = 90, data, label, marker, fill = false, color,
}: {
  width?: number; height?: number; data: number[];
  label?: string; marker?: number; fill?: boolean; color?: string;
}) => {
  const pad = 8;
  const w = width - pad * 2;
  const h = height - pad * 2;
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;
  const pts: [number, number][] = data.map((v, i) => [
    pad + (i / (data.length - 1)) * w,
    pad + h - ((v - min) / range) * h,
  ]);
  const d = jitterPath(pts, 1.0);
  const dArea = d + ` L ${pad + w} ${pad + h} L ${pad} ${pad + h} Z`;
  const markerPt = marker != null ? pts[marker] : null;
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      <line x1={pad} y1={pad + h} x2={pad + w} y2={pad + h} className="sketch-axis" />
      {fill && <path d={dArea} className="sketch-fill" />}
      <path d={d} className="sketch-line" style={color ? { stroke: color } : undefined} />
      {markerPt && <circle cx={markerPt[0]} cy={markerPt[1]} r="5" className="sketch-marker" />}
      {label && (
        <text x={pad + 4} y={pad + 12} className="sketch" fontSize="13" fill="var(--tb-ink-3)">{label}</text>
      )}
    </svg>
  );
};

export const SketchDonut = ({
  size = 140, value = 72, label = '종합', color = 'var(--tb-navy)',
}: { size?: number; value: number; label?: string; color?: string }) => {
  const r = size / 2 - 12;
  const c = size / 2;
  const circ = 2 * Math.PI * r;
  const dash = (Math.max(0, Math.min(100, value)) / 100) * circ;
  return (
    <svg width={size} height={size}>
      <circle cx={c} cy={c} r={r} stroke="var(--tb-line-2)" strokeWidth="10" fill="none" />
      <circle
        cx={c} cy={c} r={r}
        stroke={color} strokeWidth="10" fill="none" strokeLinecap="round"
        strokeDasharray={`${dash} ${circ}`}
        transform={`rotate(-90 ${c} ${c})`}
      />
      <text x={c} y={c} textAnchor="middle" dy="0.1em"
        fontSize={size * 0.28} fontWeight={700}
        fill={color} fontFamily="var(--tb-font)">{value}</text>
      <text x={c} y={c + size * 0.22} textAnchor="middle" fontSize="11"
        fill="var(--tb-ink-3)" fontFamily="var(--tb-font-mono)">{label}/100</text>
    </svg>
  );
};

export const SketchGauge = ({
  value, label, sub,
}: { value: number; label: string; sub?: string }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
      <span className="wf-h3">{label}</span>
      <span style={{ fontSize: 18, fontWeight: 700, color: 'var(--tb-navy)' }}>{value}</span>
    </div>
    <svg width="100%" height="14" viewBox="0 0 200 14" preserveAspectRatio="none">
      <rect x="1" y="4" width="198" height="6" rx="3" fill="var(--tb-line-2)" />
      <rect x="1" y="4" width={Math.max(0, Math.min(100, value)) * 1.98} height="6" rx="3" fill="var(--tb-navy)" />
    </svg>
    {sub && <span className="wf-mono">{sub}</span>}
  </div>
);

export const SketchBars = ({
  width = 220, height = 90, data, labels, color,
}: {
  width?: number; height?: number; data: number[]; labels?: string[]; color?: string;
}) => {
  const pad = 10;
  const w = width - pad * 2;
  const h = height - pad * 2 - 14;
  const max = Math.max(...data);
  const bw = w / data.length - 4;
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      <line x1={pad} y1={pad + h} x2={pad + w} y2={pad + h} className="sketch-axis" />
      {data.map((v, i) => {
        const bh = (v / max) * h;
        const x = pad + i * (bw + 4);
        const y = pad + h - bh;
        return (
          <rect key={i} x={x} y={y} width={bw} height={bh}
            fill={color || 'var(--tb-navy)'} />
        );
      })}
      {labels && labels.map((l, i) => (
        <text key={i} x={pad + i * (bw + 4) + bw / 2} y={pad + h + 12}
          textAnchor="middle" className="sketch" fontSize="11" fill="var(--tb-ink-3)">
          {l}
        </text>
      ))}
    </svg>
  );
};

export const SketchWaterfall = ({
  width = 420, height = 170, steps,
}: {
  width?: number; height?: number;
  steps: { label: string; value: number; type: 'start' | 'pos' | 'neg' | 'end'; note?: string }[];
}) => {
  const pad = 18;
  const w = width - pad * 2;
  const h = height - pad - 28;
  const totals: { from: number; to: number }[] = [];
  let cum = 0;
  steps.forEach((s) => {
    if (s.type === 'start')      { totals.push({ from: 0,   to: s.value }); cum = s.value; }
    else if (s.type === 'end')   { totals.push({ from: 0,   to: s.value }); cum = s.value; }
    else if (s.type === 'pos')   { totals.push({ from: cum, to: cum + s.value }); cum += s.value; }
    else                         { totals.push({ from: cum, to: cum - s.value }); cum -= s.value; }
  });
  const max = Math.max(...totals.flatMap(t => [t.from, t.to])) || 1;
  const bw = w / steps.length - 6;
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      {steps.map((s, i) => {
        const t = totals[i];
        const top = pad + h - (Math.max(t.from, t.to) / max) * h;
        const bot = pad + h - (Math.min(t.from, t.to) / max) * h;
        const x = pad + i * (bw + 6);
        const fill =
          s.type === 'start' || s.type === 'end' ? 'var(--tb-navy)' :
          s.type === 'pos' ? 'var(--tb-navy-soft)' : '#FCE7E7';
        const stroke = s.type === 'neg' ? '#C44545' : 'var(--tb-navy)';
        return (
          <g key={i}>
            <rect x={x} y={top} width={bw} height={Math.max(2, bot - top)}
              fill={fill} stroke={stroke} strokeWidth="1.2" />
            <text x={x + bw / 2} y={pad + h + 14} textAnchor="middle"
              fontSize="10.5" fill="var(--tb-ink-3)">{s.label}</text>
            <text x={x + bw / 2} y={top - 4} textAnchor="middle" fontSize="10"
              className="sketch" fill="var(--tb-ink-2)">{s.note || ''}</text>
          </g>
        );
      })}
    </svg>
  );
};

/** 카카오 지도 placeholder — 데모에선 SVG로 대체. 실제 연동시 KakaoMap SDK 사용 */
export const MapPh = ({
  width = '100%', height = 220, address,
}: { width?: string | number; height?: number; address?: string }) => (
  <div style={{
    width, height, position: 'relative', borderRadius: 'var(--tb-radius-sm)',
    overflow: 'hidden', background: '#EEF2F7',
  }}>
    <svg width="100%" height="100%" viewBox="0 0 320 220" preserveAspectRatio="xMidYMid slice">
      <g stroke="#D5DCE6" strokeWidth="1.5" fill="none">
        <path d="M0 60 Q 80 50 160 70 T 320 80" />
        <path d="M0 130 Q 100 120 200 140 T 320 150" />
        <path d="M40 0 L 80 220" />
        <path d="M180 0 L 200 220" />
        <path d="M260 0 L 250 220" />
      </g>
      <ellipse cx="170" cy="110" rx="80" ry="50"
        fill="rgba(26,42,108,0.08)" stroke="rgba(26,42,108,0.3)"
        strokeWidth="1.5" strokeDasharray="4 3" />
      <g transform="translate(170 105)">
        <circle r="14" fill="rgba(26,42,108,0.15)" />
        <path d="M0 -16 C 8 -16 12 -10 12 -4 C 12 4 0 14 0 14 C 0 14 -12 4 -12 -4 C -12 -10 -8 -16 0 -16 Z"
          fill="var(--tb-navy)" />
        <circle r="3" cy="-4" fill="#fff" />
      </g>
    </svg>
    <div style={{
      position: 'absolute', top: 8, left: 8, background: '#fff',
      border: '1px solid var(--tb-line)', padding: '3px 8px', borderRadius: 4,
      fontSize: 10.5, fontFamily: 'var(--tb-font-mono)', color: 'var(--tb-ink-3)',
    }}>
      kakao.maps <span style={{ color: '#E0A800', marginLeft: 4 }}>🔶 SDK 연동 필요</span>
    </div>
    {address && (
      <div style={{
        position: 'absolute', bottom: 8, left: 8, right: 8,
        background: '#fff', border: '1px solid var(--tb-line)',
        padding: '6px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600,
      }}>
        📍 {address}
      </div>
    )}
  </div>
);
