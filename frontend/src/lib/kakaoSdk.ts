/**
 * 카카오 Maps SDK 싱글턴 로더.
 * AddressInput, KakaoMap 등 여러 컴포넌트가 같은 Promise를 공유해
 * 중복 로드·race condition을 방지합니다.
 */
import { toast } from '@/components/Toast';

declare global {
  interface Window { kakao: any; }
}

const KAKAO_KEY = process.env.NEXT_PUBLIC_KAKAO_APP_KEY;
let _promise: Promise<boolean> | null = null;

export function ensureKakaoMaps(): Promise<boolean> {
  if (typeof window === 'undefined') return Promise.resolve(false);
  if (!KAKAO_KEY) {
    toast.error('NEXT_PUBLIC_KAKAO_APP_KEY가 설정되지 않았습니다');
    return Promise.resolve(false);
  }
  // 이미 완전히 초기화된 경우 (LatLng가 생성자로 사용 가능한지 확인)
  if (typeof window.kakao?.maps?.LatLng === 'function') return Promise.resolve(true);
  if (_promise) return _promise;

  _promise = new Promise<boolean>((resolve) => {
    const s = document.createElement('script');
    // autoload=false: onload 시점에 window.kakao.maps.load(cb) 로 초기화
    // cb 안에서야 LatLng 등 Maps API 생성자 사용 가능
    s.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&libraries=services&autoload=false`;
    s.onload = () => {
      try {
        window.kakao.maps.load(() => resolve(true));
      } catch (e) {
        toast.error('카카오 지도 SDK 초기화 실패 (앱 키 · 도메인 등록 확인)');
        _promise = null;
        resolve(false);
      }
    };
    s.onerror = () => {
      toast.error('카카오 지도 SDK 로드 실패');
      _promise = null;
      resolve(false);
    };
    document.head.appendChild(s);
  });
  return _promise;
}
