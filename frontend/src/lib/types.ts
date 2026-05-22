// 백엔드 응답의 핵심 타입들. snake_case 그대로 (백엔드가 그대로 직렬화).

export type LifecycleMeta = { name: string; description: string; color: string };

export type SimilarPattern = {
  name: string;
  dtw_similarity: number;
  dtw_dist: number;
  lifecycle: string;
  peak_month?: string | null;
};

export type TrendResponse = {
  plc_stage: string;
  overall_trend: string;
  recent_trend: string;
  latest_interest: number;
  average_interest: number;
  trend_speed: string;
  peak_ratio: number;
  growth_rate: number;
  trend_score: number;
  recommendation: string;
  similar_patterns: SimilarPattern[];
  /** 14개월 raw 관심도 시계열. FastAPI 옛 버전에서는 undefined. */
  interest_series?: number[];
};

export type LocationResponse = {
  success: boolean;
  data: {
    locationInfo: {
      address: string;
      lat: number;
      lng: number;
      areaCode?: string;
      areaName?: string;
      dongCode?: string;
      dongName?: string;
    };
    rawMetrics: {
      foottraffic: number;
      avgMonthlySales: number;
      facilityCount: number;
      competitionCount: number;
    };
    processedScores: {
      trafficScore: number;
      salesScore: number;
      competitionScore: number;
      rawScore: number;
    };
    finalResult: { locationMultiplier: number; grade: number };
    message?: string | null;
  };
};

export type FinanceResponse = {
  success: boolean;
  data: {
    unitEconomics: {
      sellingPrice: number;
      directMaterialCostPerUnit: number;
      directLaborCostPerUnit: number;
      variableOverheadPerUnit: number;
      totalVariableCostPerUnit: number;
      contributionMarginPerUnit: number;
    };
    monthlyResult: {
      totalRevenue: number;
      totalVariableCost: number;
      fixedCostAllocation: number;
      operatingProfit: number;
      profitMargin: number;
      foodCostRate: number;
    };
    breakEven: { breakEvenQuantityPerDay: number; breakEvenRevenuePerMonth: number };
    incremental?: {
      additionalRevenue: number;
      additionalOperatingProfit: number;
      profitGrowthRate: number;
    } | null;
  };
};

export type SimulationResponse = {
  success: boolean;
  data: {
    baseline?: { currentOperatingProfit: number } | null;
    simulation: {
      adjustedRevenue: number;
      totalOperatingProfit: number;
      profitMargin: number;
      breakEvenQuantity: number;
      warningLevel: 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED';
      operationBurdenIndex: number;
    };
    incremental?: { additionalOperatingProfit: number; profitGrowthRate: number } | null;
  };
};

export type ReportResponse = {
  verdict: string;
  scores: { trend: number; profitability: number; sustainability: number; total: number };
  risk_factors: string[];
  llm_report: string;
};

export type AnalysisResponse = {
  success: boolean;
  data: {
    location: LocationResponse;
    finance: FinanceResponse;
    trend: TrendResponse;
    simulation: SimulationResponse;
    report: ReportResponse;
  };
};

// ── 입력 폼 (FinanceRequestDto + LocationAnalysisRequest + 트렌드 키워드)
export type EmployeeInput =
  | { name?: string; mode: 'simple'; monthlyTotal: number }
  | {
      name?: string;
      mode: 'detail';
      hourlyWage: number;
      weeklyHours: number;
      hasNightShift: boolean;
      nightShiftHoursPerWeek?: number;
    };

export type AnalysisInput = {
  keyword: string;
  category: string;
  location: { address: string; lat: number; lng: number };
  finance: {
    menuInfo: {
      menuName: string;
      category: string;
      sellingPrice: number;
      packagingCostPerUnit: number;
    };
    directMaterialCost: { ingredientCostPerUnit: number };
    directLaborCost: {
      bohTimeMinutes: number;
      fohTimeMinutes: number;
      employees: EmployeeInput[];
    };
    variableOverheadCostPerUnit: number;
    fixedCost: {
      monthlyRent: number;
      managementFee: number;
      marketingCost: number;
      miscCost: number;
    };
    salesAssumption: {
      expectedDailySales: number;
      workingDaysPerWeek: number;
      trendDurationWeeks: number;
      dailyWorkHours: number;
    };
  };
};

export type TrendKeyword = {
  keyword: string;
  category: string;
  searchCount: number;
  lastSearchedAt: string;
};
