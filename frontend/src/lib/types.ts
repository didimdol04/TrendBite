/**
 * 백엔드 API DTO를 TypeScript로 미러링.
 *
 * 출처:
 *  - TrendBite-feature-integration: AnalysisRequest/Response, TrendResponse, FinanceResponseDto,
 *    SimulationResponseDto, SalesRecordRequestDto, SalesCorrectionResponseDto
 *  - TrendBite-feature-location-quality-pass-2: LocationAnalysisRequest/Response (v2 추가 필드)
 *
 * 백엔드 DTO 필드명 그대로 사용 — 직렬화 시 변환 없이 그대로 보내고 받는다.
 */

// ──────────────────────────────────────────────────────────────
// 공통 응답 envelope
// ──────────────────────────────────────────────────────────────
export interface ApiSuccess<T> {
  success: true;
  data: T;
}
export interface ApiError {
  success: false;
  error: string; // VALIDATION_FAILED | BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR
  message?: string;
  fieldErrors?: Record<string, string>;
}
export type ApiResponse<T> = ApiSuccess<T> | ApiError;

// ──────────────────────────────────────────────────────────────
// AnalysisRequest (POST /api/v1/analysis/run)
// ──────────────────────────────────────────────────────────────
export interface AnalysisRequest {
  keyword: string;
  category: string;
  location: LocationAnalysisRequest;
  finance: FinanceRequestDto;
}

// ──── Location request
export interface LocationAnalysisRequest {
  address?: string;
  lat: number;  // -90 ~ 90
  lng: number;  // -180 ~ 180
}

// ──── Finance request
export interface FinanceRequestDto {
  menuInfo: MenuInfoDto;
  directMaterialCost: DirectMaterialCostDto;
  directLaborCost: DirectLaborCostDto;
  variableOverheadCostPerUnit: number;
  fixedCost: FixedCostDto;
  salesAssumption: SalesAssumptionDto;
  baselineProfit?: BaselineProfitDto | null;
}
export interface MenuInfoDto {
  menuName: string;
  category: string;
  sellingPrice: number;
  packagingCostPerUnit: number;
}
export interface DirectMaterialCostDto {
  ingredientCostPerUnit: number;
}
export interface DirectLaborCostDto {
  bohTimeMinutes: number;
  fohTimeMinutes: number;
  employees: EmployeeDto[];
}
export interface EmployeeDto {
  name: string;
  mode: 'simple' | 'detail';
  monthlyTotal?: number;
  hourlyWage?: number;
  weeklyHours?: number;
  hasNightShift?: boolean;
  nightShiftHoursPerWeek?: number;
}
export interface FixedCostDto {
  monthlyRent: number;
  managementFee: number;
  marketingCost: number;
  miscCost: number;
}
export interface SalesAssumptionDto {
  expectedDailySales: number;
  workingDaysPerWeek: number;
  trendDurationWeeks: number;
  dailyWorkHours?: number;
}
export interface BaselineProfitDto {
  currentMonthlyRevenue: number;
  currentMonthlyOperatingProfit: number;
}

// ──────────────────────────────────────────────────────────────
// AnalysisResponse
// ──────────────────────────────────────────────────────────────
export interface AnalysisResponse {
  success: boolean;
  data: {
    location: LocationAnalysisResponse;
    finance: FinanceResponseDto;
    trend: TrendResponse;
    simulation: SimulationResponseDto;
    report: ReportResponse;
  };
}

// ──── Location response (pass-2 신규 필드 포함)
export interface LocationAnalysisResponse {
  success: boolean;
  data: {
    locationInfo: LocationInfoDto;
    rawMetrics: RawMetricsDto;
    processedScores: ProcessedScoresDto;
    finalResult: FinalResultDto;
    message?: string | null;
  };
}
export interface LocationInfoDto {
  address: string;
  lat: number;
  lng: number;
  areaCode: string;
  areaName: string;
  dongCode: string;
  dongName: string;
  nearestFallback?: boolean | null;       // pass-2 추가
  nearestDistanceMeters?: number | null;  // pass-2 추가
}
export interface RawMetricsDto {
  foottraffic: number;
  avgMonthlySales: number;
  facilityCount: number;
  competitionCount: number;
  dataSourceStatus?: DataSourceStatus | null; // pass-2 추가
}
export interface DataSourceStatus {
  trafficFromApi: boolean | null;
  salesFromApi: boolean | null;
  facilityFromApi: boolean | null;
  competitionFromApi: boolean | null;
}
export interface ProcessedScoresDto {
  trafficScore: number;
  salesScore: number;
  competitionScore: number;
  rawScore: number;
  normalizedScore?: number | null; // pass-2 추가 (0~1)
  displayScore?: number | null;    // pass-2 추가 (0~100, UI 표시 권장)
}
export interface FinalResultDto {
  locationMultiplier: number; // 0.8 ~ 1.2
  grade: number;              // 1 ~ 10
}

// ──── Finance response
export interface FinanceResponseDto {
  success: boolean;
  data: {
    unitEconomics: UnitEconomicsDto;
    monthlyResult: MonthlyResultDto;
    breakEven: BreakEvenDto;
    incremental: IncrementalFinanceDto | null;
  };
}
export interface UnitEconomicsDto {
  sellingPrice: number;
  directMaterialCostPerUnit: number;
  directLaborCostPerUnit: number;
  variableOverheadPerUnit: number;
  totalVariableCostPerUnit: number;
  contributionMarginPerUnit: number;
}
export interface MonthlyResultDto {
  totalRevenue: number;
  totalVariableCost: number;
  fixedCostAllocation: number;
  operatingProfit: number;
  profitMargin: number;
  foodCostRate: number;
}
export interface BreakEvenDto {
  breakEvenQuantityPerDay: number;
  breakEvenRevenuePerMonth: number;
}
export interface IncrementalFinanceDto {
  additionalRevenue: number;
  additionalOperatingProfit: number;
  profitGrowthRate: number;
}

// ──── Trend response
export interface TrendResponse {
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
}
export interface SimilarPattern {
  name: string;
  dtw_similarity: number;
  dtw_dist: number;
  lifecycle: string;
  peak_month?: string | null;
}

// ──── Simulation response
export interface SimulationResponseDto {
  success: boolean;
  data: {
    baseline: { currentOperatingProfit: number } | null;
    simulation: {
      adjustedRevenue: number;
      totalOperatingProfit: number;
      profitMargin: number;
      breakEvenQuantity: number;
      warningLevel: 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED';
      operationBurdenIndex: number;
    };
    incremental: {
      additionalOperatingProfit: number;
      profitGrowthRate: number;
    } | null;
  };
}

// ──── Report response
export interface ReportResponse {
  verdict: '도입 권장' | '신중 검토' | '불확실' | '보류';
  scores: ScoreBreakdownDto;
  riskFactors?: string[];
  llmReport: string;
}
export interface ScoreBreakdownDto {
  trend: number;
  profitability: number;
  sustainability: number;
  total: number;
}

// ──────────────────────────────────────────────────────────────
// Sales (실판매)
// ──────────────────────────────────────────────────────────────
export interface SalesRecordRequest {
  menuId: number;
  salesDate: string; // YYYY-MM-DD
  actualQuantity: number;
  actualRevenue: number;
}
export interface SalesRecordResponse {
  success: boolean;
  data: {
    id: number;
    menuId: number;
    salesDate: string;
    actualQuantity: number;
    actualRevenue: number;
  };
}
export interface SalesCorrectionResponse {
  success: boolean;
  data: {
    menuId: number;
    originalExpectedDailySales: number;
    correctedExpectedDailySales: number;
    correctionRate: number;
    basedOnDays: number;
    lastSalesDate: string;
  };
}

// ──────────────────────────────────────────────────────────────
// Trend popular / lifecycles
// ──────────────────────────────────────────────────────────────
export interface TrendKeywordResponse {
  keyword: string;
  category: string;
  searchCount: number;
  lastSearchedAt: string;
}
export interface LifecycleResponse {
  name: string;        // 한글 라벨 (FastAPI 응답값과 일치)
  description: string;
  color: string;       // HEX
}

// ──────────────────────────────────────────────────────────────
// 프론트 전용 — 유저 식별
// ──────────────────────────────────────────────────────────────
export interface UserProfile {
  id: string;          // 클라이언트 생성 UUID
  nickname: string;    // 닉네임 or 가게명
  createdAt: string;
}

export interface SavedAnalysis {
  id: string;
  userId: string;
  menuId: number;     // 데모: 클라이언트 sequence
  menuName: string;
  category: string;
  address?: string;
  createdAt: string;
  response: AnalysisResponse['data'];
}

export interface SavedSalesRecord {
  id: string;
  userId: string;
  menuId: number;
  salesDate: string;
  actualQuantity: number;
  actualRevenue: number;
  createdAt: string;
}
