# TrendBite Frontend Demo

> 졸업 프로젝트 · 소자본 외식업 사장님을 위한 신메뉴 수익성 예측 시뮬레이터
> **5/20 회의용 프론트엔드 데모**

## 실행 방법

```bash
npm install
npm run dev    # http://localhost:3000
```

빌드 검증:
```bash
npm run build
```

## 데모 모드

- 기본값: `MOCK_MODE = true` (`src/lib/api.ts`)
- 백엔드 없이도 모든 페이지 동작 — 마라탕 mock 응답을 기반으로 결과 페이지 렌더
- 메뉴명에 따라 verdict 분기 시연:
  - **마라탕**(또는 일반): `신중 검토` (캡처와 동일)
  - **약과**: `도입 권장` (GREEN)
  - **탕후루**: `보류` (RED)

백엔드 연동 시:
1. `src/lib/api.ts`의 `MOCK_MODE = false`로 변경
2. `.env.local`에 `NEXT_PUBLIC_API_BASE=http://localhost:8080` 설정

## 페이지 구조

| 경로 | 와이어프레임 | 비고 |
|---|---|---|
| `/` | P1 랜딩 | 인기 키워드 9개 + 라이프사이클 미리보기 |
| `/analyze` | P2 위저드 (5스텝) | 자동저장 · 샘플 데이터 채우기 버튼 포함 |
| `/result/[id]` | P4 결과 리포트 | `sample` / `guest` / 저장 ID 분기 |
| `/history` | P5 이력 | 닉네임 식별 유저만 |
| 🆕 `/sales-record/[menuId]` | **신규** (회의록 명시) | 일별 실판매 기록 + 3일 누적 시 예측 보정 |
| `/terms`, `/privacy` | 법적 placeholder | 본문 작성 필요 |

P3 로딩은 별도 라우트 대신 위저드 submit 시 **풀스크린 오버레이**로 표시 (`src/components/LoadingOverlay.tsx`).

## 유저 식별 분기 (회의록 결정사항)

`src/lib/userContext.tsx`에서 3가지 유저 유형 처리:

1. **게스트** (닉네임 미입력) — 1회성 분석만, `/result/guest`로 sessionStorage 결과 표시
2. **신규 유저** (DB에 처음) — 분석/저장/실판매 기록 가능
3. **기존 유저** (DB 매칭) — 위 + 과거 이력 조회 + 등록 메뉴 목록

⚠️ 현재 데모는 localStorage + 닉네임 매칭으로 유저 식별. **실서비스 시 백엔드 User 엔티티 추가 + 인증 방식 합의 필요**.

## 백엔드 API 매핑

`src/lib/types.ts`에 TypeScript로 모든 DTO 미러링. 출처:
- `TrendBite-feature-integration` 브랜치 — Trend / Finance / Simulation / Sales
- `TrendBite-feature-location-quality-pass-2` 브랜치 — Location (v2 신규 필드 반영)
  - `nearestFallback` + `nearestDistanceMeters` → 결과 페이지에 fallback 안내 표시
  - `dataSourceStatus` → 외부 API 미수신 시 raw 지표에 경고 표시
  - `displayScore` (0~100) → 사용자 표시값으로 사용

| 엔드포인트 | 호출 함수 |
|---|---|
| `POST /api/v1/analysis/run` | `runAnalysis()` |
| `GET  /api/v1/trend/popular` | `getPopularKeywords()` |
| `GET  /api/v1/trend/lifecycles` | `getLifecycles()` |
| `POST /api/v1/sales/record` | `recordSales()` |
| `GET  /api/v1/sales/correction/{menuId}` | `getSalesCorrection()` |

## 디자인 시스템

`src/app/globals.css`의 CSS 변수 — 와이어프레임 초안과 1:1 일치. 핵심 5색 이내:
- `--tb-navy`  Primary (#1A2A6C)
- `--tb-amber` Accent  (#E0A800)
- `--tb-canvas`, `--tb-line`, `--tb-ink-*` (회색 단계)

폰트: Pretendard Variable / Caveat (sketch) / JetBrains Mono (mono)

## 🔶 합의 필요 항목 (데모에 노란 마커로 표시)

데모 곳곳에 `<PendingMarker>` 및 `<PendingBox>`로 표시되어 있음 — 5/20 회의에서 일괄 논의:

1. **백엔드 User 엔티티 추가** (Menu/ActualSales에 user_id FK)
2. **유사 메뉴 추천 임계값 / 데이터 소스** (현재는 키워드 사전 + 부분 매칭)
3. **카카오 우편번호·지도 SDK 연동** (현재 수기 입력 + SVG placeholder)
4. **점수 도넛 total 산출 공식** (현재 FastAPI 응답값 그대로 표시)
5. **결과 페이지 액션** — PDF / 공유 / 다시 분석 동선
6. **감가상각·배달 수수료** 입력 필드 추가 (백엔드 `FixedCostDto` TODO)
7. **CSV 일괄 업로드** 형식 (실판매 기록)
8. **약관 / 개인정보처리방침 본문**
9. **닉네임 충돌 처리 정책** (실서비스 인증)
10. **보정값을 결과 페이지에 자동 반영하는 동선**

## 디렉토리 구조

```
src/
├── app/                       # Next.js 14 App Router
│   ├── layout.tsx
│   ├── globals.css            # 디자인 토큰 + wireframe primitives
│   ├── page.tsx               # P1 랜딩
│   ├── analyze/page.tsx       # P2 위저드
│   ├── result/[id]/page.tsx   # P4 결과
│   ├── history/page.tsx       # P5 이력
│   ├── sales-record/[menuId]/page.tsx  # 🆕 실판매 기록
│   ├── terms/page.tsx
│   └── privacy/page.tsx
├── components/
│   ├── Primitives.tsx         # Box, Btn, Input, Field, Badge, Chip, PendingMarker
│   ├── Charts.tsx             # SketchLine/Donut/Gauge/Waterfall, MapPh
│   ├── Header.tsx
│   ├── Footer.tsx
│   ├── NicknameGate.tsx       # 닉네임 모달
│   └── LoadingOverlay.tsx     # P3 로딩
└── lib/
    ├── types.ts               # 백엔드 DTO 미러
    ├── mockData.ts            # api-tests.http 기반 mock
    ├── api.ts                 # MOCK_MODE flag, fetch helper
    ├── userContext.tsx        # 닉네임 기반 유저 식별 (localStorage)
    └── wizardForm.ts          # 위저드 상태 + 자동 저장 (sessionStorage)
```

## 시연 시나리오 제안

1. `/` 진입 → "샘플 리포트 보기"로 P4 결과 미리보기 (mock 마라탕)
2. 헤더 "분석 시작" → 닉네임 게이트 → 닉네임 입력 후 위저드 진입
3. 좌측 사이드바 "샘플 데이터 채우기" → 5스텝 모두 자동 채움
4. Step 1에서 "마라타엉" 같은 오타 입력 → 유사 메뉴 추천 시연
5. Step 1에서 "두바이초콜릿" 등 신규 메뉴 입력 → "트렌드 데이터 없음" 안내 박스 시연
6. 마지막 단계에서 "분석 시작" → 로딩 4단계 진행 표시 → 결과 페이지
7. 결과 페이지 하단 "실판매 기록" → 실판매 기록 페이지
8. 3일 이상 입력 → 예측 보정 결과 카드 활성화
9. `/history` → 카드 그리드에서 분석 이력 조회
