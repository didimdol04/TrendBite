'use client';
/**
 * 회의록 결정사항:
 *  - 로그인/회원가입은 만들지 않되, 닉네임 or 가게명을 필수 입력 받아 유저 식별
 *  - 닉네임이 있어야 분석 결과 저장 + 실판매 기록 가능
 *  - 닉네임 없는 "게스트" 모드는 1회성 분석만 가능
 *
 * 데모에서는 백엔드 User 엔티티 부재 상태를 시뮬레이션:
 *  - localStorage에 nickname 저장 (개인 디바이스 단위 식별)
 *  - 같은 디바이스에서 같은 닉네임으로 진입 시 기존 유저로 인식
 *  - "🔶 합의 필요"로 표시 — 실서비스에선 백엔드 User 엔티티 + 인증 방식 필요
 */
import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { UserProfile, SavedAnalysis, SavedSalesRecord, AnalysisResponse } from './types';

const STORAGE_KEY = 'trendbite_user';
const ANALYSES_KEY = 'trendbite_analyses';
const SALES_KEY = 'trendbite_sales';
const KNOWN_NICKNAMES_KEY = 'trendbite_known_nicknames';

export type UserState =
  | { kind: 'guest' }
  | { kind: 'identified'; user: UserProfile };

interface Ctx {
  state: UserState;
  login: (nickname: string) => UserProfile;
  logout: () => void;
  isReturning: (nickname: string) => boolean;
  saveAnalysis: (input: { menuName: string; category: string; address?: string; data: AnalysisResponse['data']; noTrend?: boolean; menuId?: number; originalRequest?: SavedAnalysis['originalRequest'] }) => SavedAnalysis;
  getAnalyses: () => SavedAnalysis[];
  getAnalysis: (id: string) => SavedAnalysis | undefined;
  deleteAnalysis: (id: string) => void;
  saveSalesRecord: (input: Omit<SavedSalesRecord, 'id' | 'userId' | 'createdAt'>) => SavedSalesRecord;
  getSalesRecords: (menuId: number) => SavedSalesRecord[];
}

const UserContext = createContext<Ctx | null>(null);

function uuid() {
  // 데모용 짧은 ID
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36);
}

export function UserProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<UserState>({ kind: 'guest' });

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setState({ kind: 'identified', user: JSON.parse(raw) });
    } catch {}
  }, []);

  const login = useCallback((nameInput: string): UserProfile => {
    const trimmed = nameInput.trim();
    if (!trimmed) throw new Error('가게명을 입력해주세요');

    let known: Record<string, UserProfile> = {};
    try {
      known = JSON.parse(localStorage.getItem(KNOWN_NICKNAMES_KEY) || '{}');
    } catch {}

    // 베이스 가게명 추출: 'XX #ABCD' 패턴에서 'XX' 분리
    const suffixMatch = trimmed.match(/^(.+)\s#[A-Z0-9]{4}$/);
    const baseName = suffixMatch ? suffixMatch[1] : trimmed;

    // 기존 가게 → 저장된 프로필 그대로 복원
    const existing = known[baseName];
    if (existing) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(existing));
      setState({ kind: 'identified', user: existing });
      return existing;
    }

    // 신규 가게 → trimmed(suffix 포함)을 nickname으로, baseName을 키로 저장
    const user: UserProfile = {
      id: uuid(),
      nickname: trimmed,
      createdAt: new Date().toISOString(),
    };
    known[baseName] = user;
    localStorage.setItem(KNOWN_NICKNAMES_KEY, JSON.stringify(known));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    setState({ kind: 'identified', user });
    return user;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setState({ kind: 'guest' });
  }, []);

  const isReturning = useCallback((nameInput: string): boolean => {
    try {
      const known: Record<string, UserProfile> = JSON.parse(
        localStorage.getItem(KNOWN_NICKNAMES_KEY) || '{}'
      );
      const trimmed = nameInput.trim();
      const suffixMatch = trimmed.match(/^(.+)\s#[A-Z0-9]{4}$/);
      const baseName = suffixMatch ? suffixMatch[1] : trimmed;
      return !!known[baseName];
    } catch {
      return false;
    }
  }, []);

  const saveAnalysis: Ctx['saveAnalysis'] = useCallback((input) => {
    if (state.kind !== 'identified') {
      throw new Error('게스트는 분석을 저장할 수 없습니다.');
    }
    const all = readJson<SavedAnalysis[]>(ANALYSES_KEY, []);
    // 백엔드가 menuId를 반환하면 우선 사용, 없으면 클라이언트 시퀀스로 폴백
    const menuId = input.menuId ?? (all.length ? Math.max(...all.map(a => a.menuId)) + 1 : 1);
    const saved: SavedAnalysis = {
      id: uuid(),
      userId: state.user.id,
      menuId,
      menuName: input.menuName,
      category: input.category,
      address: input.address,
      createdAt: new Date().toISOString(),
      response: input.data,
      noTrend: input.noTrend,
      originalRequest: input.originalRequest,
    };
    all.push(saved);
    writeJson(ANALYSES_KEY, all);
    return saved;
  }, [state]);

  const getAnalyses = useCallback(() => {
    if (state.kind !== 'identified') return [];
    return readJson<SavedAnalysis[]>(ANALYSES_KEY, [])
      .filter(a => a.userId === state.user.id)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }, [state]);

  const getAnalysis = useCallback((id: string) => {
    const all = readJson<SavedAnalysis[]>(ANALYSES_KEY, []);
    return all.find(a => a.id === id);
  }, []);

  const deleteAnalysis = useCallback((id: string) => {
    if (state.kind !== 'identified') return;
    const all = readJson<SavedAnalysis[]>(ANALYSES_KEY, []);
    const target = all.find(a => a.id === id && a.userId === state.user.id);
    if (!target) return;
    writeJson(ANALYSES_KEY, all.filter(a => a.id !== id));
    const sales = readJson<SavedSalesRecord[]>(SALES_KEY, []);
    writeJson(SALES_KEY, sales.filter(s => s.menuId !== target.menuId));
  }, [state]);

  const saveSalesRecord: Ctx['saveSalesRecord'] = useCallback((input) => {
    if (state.kind !== 'identified') {
      throw new Error('게스트는 실판매를 기록할 수 없습니다.');
    }
    const all = readJson<SavedSalesRecord[]>(SALES_KEY, []);
    // 동일 menuId + 동일 날짜 중복 방지 (백엔드 unique constraint와 일치)
    const dup = all.find(r =>
      r.userId === state.user.id &&
      r.menuId === input.menuId &&
      r.salesDate === input.salesDate
    );
    if (dup) {
      throw new Error('해당 날짜에 이미 판매 데이터가 존재합니다.');
    }
    const saved: SavedSalesRecord = {
      ...input,
      id: uuid(),
      userId: state.user.id,
      createdAt: new Date().toISOString(),
    };
    all.push(saved);
    writeJson(SALES_KEY, all);
    return saved;
  }, [state]);

  const getSalesRecords = useCallback((menuId: number) => {
    if (state.kind !== 'identified') return [];
    return readJson<SavedSalesRecord[]>(SALES_KEY, [])
      .filter(r => r.userId === state.user.id && r.menuId === menuId)
      .sort((a, b) => a.salesDate.localeCompare(b.salesDate));
  }, [state]);

  return (
    <UserContext.Provider value={{
      state, login, logout, isReturning,
      saveAnalysis, getAnalyses, getAnalysis, deleteAnalysis,
      saveSalesRecord, getSalesRecords,
    }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser(): Ctx {
  const ctx = useContext(UserContext);
  if (!ctx) throw new Error('UserProvider 안에서만 사용 가능합니다.');
  return ctx;
}

// helpers
function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}
function writeJson<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value));
}
