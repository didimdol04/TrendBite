/**
 * API 클라이언트.
 *
 * MOCK_MODE=false  → 실제 백엔드와 통신
 * MOCK_FALLBACK=true → 네트워크/CORS 오류 시 mock 데이터로 자동 대체 (개발 편의)
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
  KNOWN_KW,
} from './mockData';
import { toast } from '@/components/Toast';

export const MOCK_MODE = false;
export const MOCK_FALLBACK = false;
const API_BASE = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// ──────────────────────────────────────────────────────────────
// POST /api/v1/analysis/run
// ──────────────────────────────────────────────────────────────
export async function runAnalysis(req: AnalysisRequest): Promise<AnalysisResponse> {
  const mockFn = async () => {
    await sleep(2000);
    const kw = req.keyword.trim();
    if (kw.includes('탕후루')) return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['보류'], req);
    if (kw.includes('마라탕')) return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['신중 검토'], req);
    if (kw.includes('약과'))   return cloneMockWithKeyword(MOCK_ANALYSIS_BY_VERDICT['도입 권장'], req);
    return cloneMockWithKeyword(MOCK_ANALYSIS_RESPONSE, req);
  };
  if (MOCK_MODE) return mockFn();
  return withFallback(
    () => fetchJson<AnalysisResponse>('/api/v1/analysis/run', { method: 'POST', body: JSON.stringify(req) }),
    mockFn,
  );
}

function cloneMockWithKeyword(src: AnalysisResponse, req: AnalysisRequest): AnalysisResponse {
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
  const mockFn = async () => { await sleep(200); return MOCK_POPULAR_KEYWORDS.slice(0, limit); };
  if (MOCK_MODE) return mockFn();
  return withFallback(() => fetchJson<TrendKeywordResponse[]>(`/api/v1/trend/popular?limit=${limit}`), mockFn);
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/trend/search-keywords?q=<query>
// ──────────────────────────────────────────────────────────────
export async function searchMenuKeywords(q: string): Promise<string[]> {
  const mockFn = async () => {
    const query = q.trim().toLowerCase();
    if (!query) return [];
    return KNOWN_KW.filter(k => k.toLowerCase().includes(query));
  };
  if (MOCK_MODE) return mockFn();
  return withFallback(() => fetchJson<string[]>(`/api/v1/trend/search-keywords?q=${encodeURIComponent(q)}`), mockFn);
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/trend/lifecycles
// ──────────────────────────────────────────────────────────────
export async function getLifecycles(): Promise<LifecycleResponse[]> {
  const mockFn = async () => { await sleep(50); return MOCK_LIFECYCLES; };
  if (MOCK_MODE) return mockFn();
  return withFallback(() => fetchJson<LifecycleResponse[]>('/api/v1/trend/lifecycles'), mockFn);
}

// ──────────────────────────────────────────────────────────────
// POST /api/v1/sales/record
// ──────────────────────────────────────────────────────────────
export async function recordSales(req: SalesRecordRequest): Promise<SalesRecordResponse> {
  const mockFn = async (): Promise<SalesRecordResponse> => {
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
  };
  if (MOCK_MODE) return mockFn();
  return withFallback(() => fetchJson<SalesRecordResponse>('/api/v1/sales/record', { method: 'POST', body: JSON.stringify(req) }), mockFn);
}

// ──────────────────────────────────────────────────────────────
// GET /api/v1/sales/correction/{menuId}
// ──────────────────────────────────────────────────────────────
export async function getSalesCorrection(menuId: number, recordsCount = 0, originalExpected = 35): Promise<SalesCorrectionResponse> {
  const mockFn = async (): Promise<SalesCorrectionResponse> => {
    await sleep(200);
    if (recordsCount < 3) {
      throw new ApiClientError('UNPROCESSABLE_ENTITY', `데이터가 부족합니다. 최소 3일 이상 기록 후 조회해주세요. (현재 ${recordsCount}일)`);
    }
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
  };
  if (MOCK_MODE) return mockFn();
  return withFallback(() => fetchJson<SalesCorrectionResponse>(`/api/v1/sales/correction/${menuId}`), mockFn);
}

// ──────────────────────────────────────────────────────────────
// 공통 유틸
// ──────────────────────────────────────────────────────────────

export class ApiClientError extends Error {
  constructor(public code: string, message: string, public fieldErrors?: Record<string, string>) {
    super(message);
  }
}

/**
 * API 오류 시 mockFn으로 자동 대체.
 * MOCK_FALLBACK=true이면 네트워크 오류·HTTP 에러 모두 mock으로 대체.
 * MOCK_FALLBACK=false이면 re-throw.
 */
async function withFallback<T>(realFn: () => Promise<T>, mockFn: () => Promise<T>): Promise<T> {
  try {
    return await realFn();
  } catch (e) {
    if (MOCK_FALLBACK && e instanceof ApiClientError) {
      toast.info(`백엔드 오류(${e.code}) — mock 데이터로 대체합니다`);
      return mockFn();
    }
    throw e;
  }
}

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    });
  } catch (e) {
    // 네트워크 오류 (서버 다운, CORS preflight 실패 등)
    const msg = e instanceof Error ? e.message : String(e);
    toast.error(`서버에 연결할 수 없습니다 (${API_BASE}) — ${msg}`);
    throw new ApiClientError('NETWORK_ERROR', msg);
  }

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
