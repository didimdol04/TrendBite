'use client';
import { useEffect, useRef } from 'react';
import { ensureKakaoMaps } from '@/lib/kakaoSdk';

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
