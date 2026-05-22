# TrendBite Frontend
> 졸업 프로젝트 · 소자본 외식업 사장님을 위한 신메뉴 수익성 예측 시뮬레이터

## 실행 방법

```bash
npm install
npm run dev    # http://localhost:3000
```

빌드 검증:
```bash
npm run build
npx tsc --noEmit   # 타입 오류만 검사 (빌드 없이)
```

## 동작 모드

| 설정 | 값 | 설명 |
|------|-----|------|
| `MOCK_MODE` | `false` | 실 백엔드와 통신 (`src/lib/api.ts`) |
| `MOCK_FALLBACK` | `true` | 백엔드 오류 시 mock 데이터로 자동 폴백 + toast 안내 |
| `NEXT_PUBLIC_API_BASE` | `.env.local` | `http://localhost:8080` (기본값) |
| `NEXT_PUBLIC_KAKAO_APP_KEY` | `.env.local` | Kakao JS SDK 앱 키 (localhost:3000 등록 필요) |

백엔드 없이 데모 실행만 원할 경우 `MOCK_MODE = true` 로 변경.  
mock 시나리오: 마라탕(신중 검토), 약과(도입 권장), 탕후루(보류)

## 페이지 구조

| 경로 | 설명 | 비고 |
|------|------|------|
| `/` | P1 랜딩 | 인기 키워드 9개 + 라이프사이클 미리보기 |
| `/analyze` | P2 위저드 (5스텝) | 자동저장 · 샘플 데이터 채우기 · sessionStorage 임시 저장 |
| `/result/[id]` | P4 결과 리포트 | `sample` / `guest` / 저장 ID 분기 |
| `/history` | P5 이력 | 닉네임 식별 유저만 접근 가능 |
| `/sales-record/[menuId]` | 실판매 기록 | 일별 기록 + 3일 누적 시 예측 보정 활성화 |
| `/terms`, `/privacy` | 법적 placeholder | 본문 작성 필요 |

P3 로딩은 별도 라우트 대신 위저드 submit 시 **풀스크린 오버레이**로 표시 (`LoadingOverlay.tsx`).

## 유저 식별 분기

`src/lib/userContext.tsx`에서 3가지 유저 유형 처리:

1. **게스트** (닉네임 미입력) — 1회성 분석만, `/result/guest`로 sessionStorage 결과 표시
2. **신규 유저** (처음 닉네임 입력) — 분석/저장/실판매 기록 가능
3. **기존 유저** (localStorage 닉네임 매칭) — 위 + 과거 이력 조회 + 등록 메뉴 목록

> 현재는 localStorage + 닉네임 매칭으로 유저 식별.  
> 실서비스 시 백엔드 User 엔티티 + 인증 방식 필요 (`backend-changes.md` 참고).

## 백엔드 API 매핑

`src/lib/types.ts`에 TypeScript로 모든 DTO 미러링.

| 엔드포인트 | 호출 함수 | 상태 |
|---|---|---|
| `POST /api/v1/analysis/run` | `runAnalysis()` | 연동 |
| `GET  /api/v1/trend/popular` | `getPopularKeywords()` | 연동 |
| `GET  /api/v1/trend/lifecycles` | `getLifecycles()` | 연동 |
| `GET  /api/v1/trend/search-keywords?q=` | `searchMenuKeywords()` | ❌ 백엔드 미구현 (mock fallback 중) |
| `POST /api/v1/sales/record` | `recordSales()` | 연동 |
| `GET  /api/v1/sales/correction/{menuId}` | `getSalesCorrection()` | 연동 |

백엔드 미구현 항목 상세: `backend-changes.md` 참고

## 디자인 시스템

`src/app/globals.css`의 CSS 변수 — 와이어프레임 초안과 1:1 일치. 핵심 5색 이내:
- `--tb-navy`  Primary (#1A2A6C)
- `--tb-amber` Accent  (#E0A800)
- `--tb-canvas`, `--tb-line`, `--tb-ink-*` (회색 단계)

폰트: Pretendard Variable / Caveat (sketch) / JetBrains Mono (mono)

## 모바일 반응형

| 위치 | 상태 | 비고 |
|------|------|------|
| 위저드 사이드바 (`wizard-layout`) | ✅ | 768px 이하에서 상단 바로 전환 |
| 결과 Verdict Hero (`hero-grid`) | ✅ | 900px 이하에서 1컬럼 전환 |
| 입력 2컬럼 그리드 (`grid-2col`) | ✅ | 768px 이하에서 1컬럼 전환 |
| 결과 섹션 카드 그리드 | ✅ | `auto-fit minmax(...)` 자동 축소 |
| 헤더/버튼 패딩 | ✅ | `clamp()` + media query 조정 |
| `SketchWaterfall` (재무 섹션) | ⚠️ | 고정 `width={420}` — 375px 이하 기기에서 가로 스크롤 발생 가능 |
| `SketchLine` (트렌드 섹션) | ⚠️ | 고정 `width={380}` — 좁은 화면에서 오버플로 가능 |
| 결과 페이지 플로팅 액션 바 | ⚠️ | `position: absolute` 배치 — 모바일에서 제목 텍스트와 겹칠 수 있음 |

## 구현 완료 항목

| 항목 | 비고 |
|------|------|
| 카카오 우편번호 + 지도 SDK 연동 | AddressInput (Tab A: 주소검색, Tab B: 지도 클릭·드래그) |
| 결과 페이지 카카오 지도 표시 | KakaoMap 컴포넌트, 입지 섹션에 핀 표시 |
| 결과 페이지 '다시' 버튼 | originalRequest를 sessionStorage에 저장 후 위저드 복원 |
| 결과 페이지 PDF 저장 | `window.print()` + `@media print` CSS 최적화 |
| 숫자 입력 천 단위 콤마 | NumberInput 컴포넌트 (blur 시 포맷, focus 시 raw 입력) |
| 분석 이력 페이지 | `/history` — 카드 그리드, 메뉴명 검색, 이력 클릭 시 결과 페이지 이동 |
| 실판매 기록 페이지 | `/sales-record/[menuId]` — 일별 입력, 3일 누적 시 보정 결과 카드 |
| 부분 분석 모드 | 트렌드 데이터 없는 메뉴 입력 시 안내 + 트렌드 섹션 N/A 표시 |
| 백엔드 연동 + mock fallback | MOCK_MODE=false, 백엔드 오류 시 toast + mock 폴백 |
| '다시 분석' 수정 후 새 이력 생성 | 덮어쓰기 없이 별도 ID로 저장 (uuid + 날짜로 구분) |

## 🔶 합의 필요 항목 (구현하는 경우에)

| # | 항목 | 상태 |
|---|------|------|
| 1 | **백엔드 User 엔티티 추가** (Menu/ActualSales에 user_id FK) | 백엔드 작업 필요 |
| 2 | **유사 메뉴 추천 임계값 / 데이터 소스** (키워드 사전 기준 미확정) | 합의 필요 |
| 3 | **점수 도넛 total 산출 공식** (FastAPI LangGraph 반환값 그대로 표시 중) | 합의 필요 |
| 4 | **결과 페이지 공유 기능** (URL 복사 / SNS 공유 동선) | 미구현 |
| 5 | **CSV 일괄 업로드** 형식 (실판매 기록 대량 입력) | 미구현 |
| 6 | **약관 / 개인정보처리방침 본문** | 미작성 |
| 7 | **닉네임 충돌 처리 정책** (실서비스 인증 방식) | 실서비스 시 결정 |
| 8 | **실판매 기록을 통한 보정값을 결과 페이지에 자동 반영하는 동선** | 미구현 |

## 추가 권장 작업 (프론트 완성도를 위해서)

| 항목 | 우선순위 | 설명 |
|------|----------|------|
| 차트 모바일 대응 | 상 | `SketchWaterfall` / `SketchLine` SVG에 `viewBox` + `width="100%"` 추가하여 반응형 전환 |
| 입력 필드 유효성 검사 | 상 | 위저드 "다음" 버튼 클릭 시 필수 필드 미입력 경고 (현재 검증 없음) |
| 결과 공유 기능 | 중 | URL 클립보드 복사 (`navigator.clipboard.writeText`) |
| 플로팅 액션 바 모바일 개선 | 중 | 모바일에서 `position: fixed` 하단 바로 전환 |
| 위저드 드래프트 복원 UX | 중 | `sessionStorage`에 draft 있을 때 진입 시 "이전 입력 불러오기" 토스트 |
| 이력 페이지 정렬/필터 | 저 | 날짜 · verdict · 메뉴명 정렬 옵션 |
| 약관/개인정보처리방침 본문 | 저 | 현재 placeholder만 존재 |

## 디렉토리 구조

```
src/
├── app/                          # Next.js 14 App Router
│   ├── layout.tsx
│   ├── globals.css               # 디자인 토큰 + wireframe primitives + print CSS
│   ├── page.tsx                  # P1 랜딩
│   ├── analyze/page.tsx          # P2 위저드 (5스텝)
│   ├── result/[id]/page.tsx      # P4 결과 리포트
│   ├── history/page.tsx          # P5 분석 이력
│   ├── sales-record/[menuId]/page.tsx  # 실판매 기록 + 보정 결과
│   ├── terms/page.tsx
│   └── privacy/page.tsx
├── components/
│   ├── Primitives.tsx            # Box, Btn, Input, Field, Badge, Chip, NumberInput, PendingMarker
│   ├── Charts.tsx                # SketchLine/Donut/Gauge/Waterfall/Bars
│   ├── AddressInput.tsx          # 주소 입력 (Tab A: 카카오 우편번호, Tab B: 지도 클릭)
│   ├── KakaoMap.tsx              # 결과 페이지 지도 (read-only, 핀 표시)
│   ├── Header.tsx
│   ├── Footer.tsx
│   ├── Toast.tsx                 # 전역 toast (success / error / info)
│   ├── NicknameGate.tsx          # 닉네임 입력 모달
│   └── LoadingOverlay.tsx        # P3 로딩 (4단계 진행 표시)
└── lib/
    ├── types.ts                  # 백엔드 DTO 미러
    ├── mockData.ts               # api-tests.http 기반 mock 응답
    ├── api.ts                    # MOCK_MODE / MOCK_FALLBACK flag, fetch helper
    ├── kakaoSdk.ts               # Kakao Maps SDK 싱글톤 로더 (autoload=false)
    ├── userContext.tsx           # 닉네임 기반 유저 식별 (localStorage)
    └── wizardForm.ts             # 위저드 상태 + 자동 저장 (sessionStorage)
```

## 시연 시나리오 제안

1. `/` 진입 → "샘플 리포트 보기"로 결과 페이지 미리보기 (mock 마라탕)
2. 헤더 "분석 시작" → 닉네임 게이트 → 닉네임 입력 후 위저드 진입
3. 좌측 사이드바 "샘플 데이터 채우기" → 5스텝 자동 채움
4. Step 1에서 "마라타엉" 오타 입력 → 유사 메뉴 추천 드롭다운 시연
5. Step 1에서 "두바이초콜릿" 입력 → "트렌드 데이터 없음" 부분 분석 안내
6. Step 2에서 주소 검색 탭 → 카카오 우편번호 팝업 → 좌표 자동 입력
7. 마지막 단계 "분석 시작" → 4단계 로딩 오버레이 → 결과 페이지
8. 결과 상단 "↻ 다시" → 위저드 복원 후 수정 → 새 이력으로 저장
9. 결과 상단 "⤓ PDF" → 브라우저 인쇄 미리보기 (헤더·플로팅 바 숨김)
10. 결과 하단 "실판매 데이터 기록" → 일별 입력 → 3일 이상 입력 시 보정 카드 활성화
11. `/history` → 이력 카드 그리드 + 메뉴명 검색 시연
