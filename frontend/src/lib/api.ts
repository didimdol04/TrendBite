import type {
  AnalysisInput,
  AnalysisResponse,
  LifecycleMeta,
  TrendKeyword,
} from './types';

const BASE = ''; // next.config rewrites 로 /api/* 가 백엔드로 프록시됨

async function jsonFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${body}`);
  }
  return res.json();
}

export const api = {
  popular: (limit = 8) =>
    jsonFetch<TrendKeyword[]>(`/api/v1/trend/popular?limit=${limit}`),
  lifecycles: () => jsonFetch<LifecycleMeta[]>(`/api/v1/trend/lifecycles`),
  run: (input: AnalysisInput) =>
    jsonFetch<AnalysisResponse>('/api/v1/analysis/run', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
};
