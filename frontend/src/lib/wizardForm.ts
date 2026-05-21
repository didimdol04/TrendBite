'use client';
/**
 * 위저드 폼 상태 관리.
 *
 * 회의록: "자동 저장" (와이어프레임 P2 사이드바에 표시됨)
 *  - 입력값은 sessionStorage에 즉시 저장 → 새로고침해도 유지
 *  - 분석 완료 후 명시적으로 clear
 *
 * 백엔드 AnalysisRequest 스키마 1:1 매핑.
 */
import { useEffect, useState, useCallback } from 'react';
import type { AnalysisRequest } from './types';

const KEY = 'trendbite_wizard_draft';

export const DEFAULT_WIZARD: AnalysisRequest = {
  keyword: '',
  category: 'dining',
  location: {
    address: '',
    lat: 0,
    lng: 0,
  },
  finance: {
    menuInfo: {
      menuName: '',
      category: 'dining',
      sellingPrice: 0,
      packagingCostPerUnit: 0,
    },
    directMaterialCost: {
      ingredientCostPerUnit: 0,
    },
    directLaborCost: {
      bohTimeMinutes: 0,
      fohTimeMinutes: 0,
      employees: [
        { name: '직원 1', mode: 'simple', monthlyTotal: 0 },
      ],
    },
    variableOverheadCostPerUnit: 0,
    fixedCost: {
      monthlyRent: 0,
      managementFee: 0,
      marketingCost: 0,
      miscCost: 0,
    },
    salesAssumption: {
      expectedDailySales: 0,
      workingDaysPerWeek: 6,
      trendDurationWeeks: 24,
      dailyWorkHours: 10,
    },
    baselineProfit: null,
  },
};

export const SAMPLE_WIZARD: AnalysisRequest = {
  keyword: '마라탕',
  category: 'dining',
  location: {
    address: '서울특별시 강남구 학동로',
    lat: 37.5172,
    lng: 127.0473,
  },
  finance: {
    menuInfo: {
      menuName: '마라탕',
      category: 'dining',
      sellingPrice: 12000,
      packagingCostPerUnit: 300,
    },
    directMaterialCost: { ingredientCostPerUnit: 4000 },
    directLaborCost: {
      bohTimeMinutes: 5,
      fohTimeMinutes: 3,
      employees: [
        { name: '사장',       mode: 'simple', monthlyTotal: 2500000 },
        { name: '주방 알바',  mode: 'detail', hourlyWage: 10030, weeklyHours: 30, hasNightShift: false },
      ],
    },
    variableOverheadCostPerUnit: 500,
    fixedCost: {
      monthlyRent: 1500000,
      managementFee: 200000,
      marketingCost: 100000,
      miscCost: 100000,
    },
    salesAssumption: {
      expectedDailySales: 80,
      workingDaysPerWeek: 6,
      trendDurationWeeks: 12,
      dailyWorkHours: 10,
    },
    baselineProfit: null,
  },
};

export function useWizardForm() {
  const [form, setForm] = useState<AnalysisRequest>(DEFAULT_WIZARD);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(KEY);
      if (raw) setForm(JSON.parse(raw));
    } catch {}
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      sessionStorage.setItem(KEY, JSON.stringify(form));
    } catch {}
  }, [form, hydrated]);

  const reset = useCallback(() => {
    sessionStorage.removeItem(KEY);
    setForm(DEFAULT_WIZARD);
  }, []);

  const loadSample = useCallback(() => {
    setForm(SAMPLE_WIZARD);
  }, []);

  return { form, setForm, reset, loadSample, hydrated };
}
