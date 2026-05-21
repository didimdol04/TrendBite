/**
 * Mock-first API 클라이언트.
 *
 * 5/20 데모 단계에서는 MOCK_MODE=true → mock 응답 즉시 반환.
 * 5/31 백엔드 연동 시점에 MOCK_MODE=false 또는 process.env.NEXT_PUBLIC_API_BASE 설정으로 전환.
 *
 * 함수 시그니처는 백엔드 endpoint와 1:1 매핑.
 */
import type {
  AnalysisRequest,
  AnalysisResponse,
  SalesRecordRequest,
  SalesRecordResponse,
  SalesCorrectionResponse,
  TrendKeywordResponse,
  LifecycleResponse,
  ApiResponse,
} from './types';
import {
  MOCK_ANALYSIS_RESPONSE,
  MOCK_ANALYSIS_BY_VERDICT,
  MOCK_POPULAR_KEYWORDS,
  MOCK_LIFECYCLES,
  hasExactKeywordData,
} from './mockData';

export const MOCK_MODE = true;
const API_BASE = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

/** 일정 시간 지연 — 로딩 UI 데모를 위해 */
const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// ──────────────────────────────────────────────────────────────
// POST /api/v1/analysis/run
// ──────────────────────────────────────────────────────────────
export async function runAnalysis(req: AnalysisRequest): Promise<AnalysisResponse> {
  if (MOCK_MODE) {
    await sleep(2000); // 로딩 UX 데모용
    // 키워드에 따라 verdict 분기 (데모 효과)
    const kw = req.keyword.trim();
    if (kw.includes('탕후루'))     return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['보류'], req);
    if (kw.includes('마라탕'))     return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['신중 검토'], req);
    if (kw.includes('약과'))       return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['도입 권장'], req);
    return cloneMockWithKeyword(MOCK_ANALYSIS_RESPONSE, req);
  }
  return fetchJson<AnalysisResponse>('/api/v1/analysis/run', { method: 'POST', body: JSON.stringify(req) });
}

function cloneMockWithKeyword(src: AnalysisResponse, req: AnalysisRequest): AnalysisResponse {
  // 요청에서 메뉴명/주소/판매가 등을 반영해 결과가 사용자 입력과 매칭되도록
  const clone: AnalysisResponse = JSON.parse(JSON.stringify(src));
  clone.data.location.data.locationInfo.address = req.location.address || clone.data.location.data.locationInfo.address;
  clone.data.location.data.locationInfo.lat = req.location.lat;
  clone.data.location.data.locationInfo.lng = req.location.lng;
  clone.data.finance.data.unitEconomics.sellingPrice = req.finance.menuInfo.sellingPrice;
  return clone;
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/trend/popular?limit=N
// ──────────────────────────────────────────────────────────────
export async function getPopularKeywords(limit = 9): Promise<TrendKeywordResponse[]> {
  if (MOCK_MODE) {
    await sleep(200);
    return MOCK_POPULAR_KEYWORDS.slice(0, limit);
  }
  return fetchJson<TrendKeywordResponse[]>(`/api/v1/trend/popular?limit=${limit}`);
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/trend/lifecycles
// ──────────────────────────────────────────────────────────────
export async function getLifecycles(): Promise<LifecycleResponse[]> {
  if (MOCK_MODE) {
    await sleep(50);
    return MOCK_LIFECYCLES;
  }
  return fetchJson<LifecycleResponse[]>('/api/v1/trend/lifecycles');
}

// ──────────────────────────────────────────────────────────────
// POST /api/v1/sales/record
// ──────────────────────────────────────────────────────────────
export async function recordSales(req: SalesRecordRequest): Promise<SalesRecordResponse> {
  if (MOCK_MODE) {
    await sleep(300);
    return {
      success: true,
      data: {
        id: Math.floor(Math.random() * 1000),
        menuId: req.menuId,
        salesDate: req.salesDate,
        actualQuantity: req.actualQuantity,
        actualRevenue: req.actualRevenue,
      },
    };
  }
  return fetchJson<SalesRecordResponse>('/api/v1/sales/record', { method: 'POST', body: JSON.stringify(req) });
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/sales/correction/{menuId}
// ──────────────────────────────────────────────────────────────
export async function getSalesCorrection(menuId: number, recordsCount = 0, originalExpected = 35): Promise<SalesCorrectionResponse> {
  if (MOCK_MODE) {
    await sleep(200);
    if (recordsCount < 3) {
      throw new ApiClientError('UNPROCESSABLE_ENTITY', `데이터가 부족합니다. 최소 3일 이상 기록 후 조회해주세요. (현재 ${recordsCount}일)`);
    }
    // 데모: 실판매 평균이 예측의 80% 정도라고 가정
    const corrected = Math.round(originalExpected * 0.8);
    return {
      success: true,
      data: {
        menuId,
        originalExpectedDailySales: originalExpected,
        correctedExpectedDailySales: corrected,
        correctionRate: 0.8,
        basedOnDays: recordsCount,
        lastSalesDate: new Date().toISOString().slice(0, 10),
      },
    };
  }
  return fetchJson<SalesCorrectionResponse>(`/api/v1/sales/correction/${menuId}`);
}

// ──────────────────────────────────────────────────────────────
// fetch helper
// ──────────────────────────────────────────────────────────────
export class ApiClientError extends Error {
  constructor(public code: string, message: string, public fieldErrors?: Record<string, string>) {
    super(message);
  }
}

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = json as ApiResponse<unknown>;
    if (err && err.success === false) {
      throw new ApiClientError(err.error || 'UNKNOWN', err.message || res.statusText, err.fieldErrors);
    }
    throw new ApiClientError('UNKNOWN', res.statusText);
  }
  return json as T;
}
