'use client';
import { useEffect, useRef } from 'react';
import { ensureKakaoMaps } from '@/lib/kakaoSdk';

export interface MapPin {
  lat: number;
  lng: number;
  label?: string;
}

interface Props {
  lat: number;
  lng: number;
  height?: number;
}

export function KakaoMap({ lat, lng, height = 260 }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!lat || !lng) return;
    let cancelled = false;

    (async () => {
      const ok = await ensureKakaoMaps();
      if (!ok || cancelled || !containerRef.current) return;

      const center = new window.kakao.maps.LatLng(lat, lng);
      const map = new window.kakao.maps.Map(containerRef.current, { center, level: 3 });
      new window.kakao.maps.Marker({ position: center, map });
    })();

    return () => { cancelled = true; };
  }, [lat, lng]);

  if (!lat || !lng) {
    return (
      <div style={{
        width: '100%', height,
        background: 'var(--tb-card)', borderRadius: 8,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 12, color: 'var(--tb-ink-4)',
      }}>
        위치 정보 없음
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height, borderRadius: 8, overflow: 'hidden' }}
    />
  );
}

/** 여러 핀을 표시하는 카카오 지도. 핀이 2개 이상이면 모두 보이도록 자동 줌 */
export function KakaoMapMulti({ pins, height = 260 }: { pins: MapPin[]; height?: number }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const validPins = pins.filter(p => p.lat && p.lng);

  useEffect(() => {
    if (validPins.length === 0) return;
    let cancelled = false;

    (async () => {
      const ok = await ensureKakaoMaps();
      if (!ok || cancelled || !containerRef.current) return;

      const first = validPins[0];
      const center = new window.kakao.maps.LatLng(first.lat, first.lng);
      const map = new window.kakao.maps.Map(containerRef.current, { center, level: 5 });

      const bounds = new window.kakao.maps.LatLngBounds();
      for (const pin of validPins) {
        const pos = new window.kakao.maps.LatLng(pin.lat, pin.lng);
        new window.kakao.maps.Marker({ position: pos, map });
        bounds.extend(pos);
      }

      if (validPins.length > 1) {
        map.setBounds(bounds, 60);
      }
    })();

    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pins.map(p => `${p.lat},${p.lng}`).join('|')]);

  if (validPins.length === 0) {
    return (
      <div style={{
        width: '100%', height,
        background: 'var(--tb-card)', borderRadius: 8,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 12, color: 'var(--tb-ink-4)',
      }}>
        위치 정보 없음
      </div>
    );
  }

  return (
    <div ref={containerRef} style={{ width: '100%', height, borderRadius: 8, overflow: 'hidden' }} />
  );
}
