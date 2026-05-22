'use client';
import { useEffect, useRef, useState } from 'react';
import { Btn } from './Primitives';
import { toast } from './Toast';
import { ensureKakaoMaps } from '@/lib/kakaoSdk';

export interface AddressResult {
  address: string;
  lat: number;
  lng: number;
}

interface Props {
  value?: Partial<AddressResult>;
  onChange: (result: AddressResult) => void;
}

declare global {
  interface Window { daum: any; }
}

const SEOUL = { lat: 37.5665, lng: 126.9780 };

let _postcodePromise: Promise<boolean> | null = null;

function ensurePostcode(): Promise<boolean> {
  if (window.daum?.Postcode) return Promise.resolve(true);
  if (_postcodePromise) return _postcodePromise;

  _postcodePromise = new Promise<boolean>((resolve) => {
    const s = document.createElement('script');
    s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
    s.onload = () => resolve(true);
    s.onerror = () => {
      toast.error('우편번호 서비스 로드 실패');
      _postcodePromise = null;
      resolve(false);
    };
    document.head.appendChild(s);
  });
  return _postcodePromise;
}

export function AddressInput({ value, onChange }: Props) {
  const [tab, setTab] = useState<'search' | 'map'>('search');
  const [address, setAddress] = useState(value?.address || '');
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    if (tab !== 'map') return;
    let cancelled = false;

    (async () => {
      const ok = await ensureKakaoMaps();
      if (!ok || cancelled || !mapContainerRef.current) return;

      const lat = value?.lat || SEOUL.lat;
      const lng = value?.lng || SEOUL.lng;
      const center = new window.kakao.maps.LatLng(lat, lng);

      const map = new window.kakao.maps.Map(mapContainerRef.current, {
        center,
        level: 3,
      });
      mapRef.current = map;

      const marker = new window.kakao.maps.Marker({
        position: center,
        draggable: true,
        map,
      });
      markerRef.current = marker;

      const handlePosition = (latlng: any) => {
        const newLat: number = latlng.getLat();
        const newLng: number = latlng.getLng();
        const geocoder = new window.kakao.maps.services.Geocoder();
        geocoder.coord2Address(newLng, newLat, (result: any, status: any) => {
          if (cancelled) return;
          if (status === window.kakao.maps.services.Status.OK) {
            const addr: string =
              result[0]?.road_address?.address_name ||
              result[0]?.address?.address_name ||
              '';
            setAddress(addr);
            onChangeRef.current({ address: addr, lat: newLat, lng: newLng });
          }
        });
      };

      window.kakao.maps.event.addListener(map, 'click', (e: any) => {
        if (cancelled) return;
        marker.setPosition(e.latLng);
        handlePosition(e.latLng);
      });

      window.kakao.maps.event.addListener(marker, 'dragend', () => {
        if (cancelled) return;
        handlePosition(marker.getPosition());
      });
    })();

    return () => {
      cancelled = true;
      mapRef.current = null;
      markerRef.current = null;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const openPostcode = async () => {
    const [postcodeOk, mapsOk] = await Promise.all([ensurePostcode(), ensureKakaoMaps()]);
    if (!postcodeOk || !mapsOk) return;

    new window.daum.Postcode({
      oncomplete: (data: any) => {
        const addr: string = data.roadAddress || data.jibunAddress || data.address || '';
        const geocoder = new window.kakao.maps.services.Geocoder();
        geocoder.addressSearch(addr, (result: any, status: any) => {
          if (status === window.kakao.maps.services.Status.OK) {
            const lat = Number(result[0].y);
            const lng = Number(result[0].x);
            setAddress(addr);
            onChangeRef.current({ address: addr, lat, lng });
          } else {
            toast.error('좌표 변환에 실패했습니다');
            setAddress(addr);
            onChangeRef.current({ address: addr, lat: 0, lng: 0 });
          }
        });
      },
    }).open();
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 4, marginBottom: 12 }}>
        {(['search', 'map'] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            style={{
              padding: '6px 16px',
              fontSize: 12.5,
              fontWeight: 600,
              borderRadius: 6,
              border: '1px solid',
              cursor: 'pointer',
              background: tab === t ? 'var(--tb-navy)' : 'transparent',
              color: tab === t ? '#fff' : 'var(--tb-ink-2)',
              borderColor: tab === t ? 'var(--tb-navy)' : 'var(--tb-line)',
              transition: 'all 0.15s',
            }}
          >
            {t === 'search' ? '주소 검색' : '지도에서 선택'}
          </button>
        ))}
      </div>

      {tab === 'search' && (
        <div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <div
              style={{
                flex: 1,
                padding: '10px 12px',
                border: '1px solid var(--tb-line)',
                borderRadius: 6,
                fontSize: 13,
                background: address ? '#fff' : 'var(--tb-card)',
                color: address ? 'var(--tb-ink)' : 'var(--tb-ink-4)',
                minHeight: 40,
                lineHeight: 1.4,
              }}
            >
              {address || '주소 검색 버튼을 눌러 선택하세요'}
            </div>
            <Btn lg onClick={openPostcode} style={{ flexShrink: 0 }}>
              주소 검색
            </Btn>
          </div>
          {address && (
            <div style={{ marginTop: 8, fontSize: 11.5, color: 'var(--tb-ink-3)' }}>
              선택된 주소로 좌표가 자동 설정되었습니다.
            </div>
          )}
        </div>
      )}

      {tab === 'map' && (
        <div>
          <div
            ref={mapContainerRef}
            style={{
              width: '100%',
              height: 280,
              borderRadius: 8,
              overflow: 'hidden',
              border: '1px solid var(--tb-line)',
            }}
          />
          {address && (
            <div
              style={{
                marginTop: 8,
                padding: '8px 12px',
                background: 'var(--tb-navy-soft)',
                borderRadius: 6,
                fontSize: 12,
                color: 'var(--tb-navy)',
                fontWeight: 600,
              }}
            >
              {address}
            </div>
          )}
          <div style={{ marginTop: 6, fontSize: 11.5, color: 'var(--tb-ink-4)' }}>
            지도를 클릭하거나 마커를 드래그해 위치를 선택하세요.
          </div>
        </div>
      )}
    </div>
  );
}
