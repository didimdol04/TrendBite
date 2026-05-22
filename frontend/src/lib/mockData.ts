import type {
  AnalysisResponse,
  TrendKeywordResponse,
  LifecycleResponse,
  SimilarPattern,
} from './types';

/**
 * 백엔드 응답 mock.
 * 출처: api-tests.http ⑦ 마라탕 통합 분석 예시 + 결과 페이지 캡처값.
 */
export const MOCK_ANALYSIS_RESPONSE: AnalysisResponse = {
  success: true,
  data: {
    location: {
      success: true,
      data: {
        locationInfo: {
          address: '서울특별시 강남구 학동로',
          lat: 37.5172,
          lng: 127.0473,
          areaCode: 'A0103',
          areaName: '강남구청(청담역,8번,강남세무서) - 삼성2동',
          dongCode: '11680510',
          dongName: '삼성동',
          nearestFallback: false,
          nearestDistanceMeters: null,
        },
        rawMetrics: {
          foottraffic: 18200,
          avgMonthlySales: 412000000,
          facilityCount: 24,
          competitionCount: 7,
          dataSourceStatus: {
            trafficFromApi: true,
            salesFromApi: true,
            facilityFromApi: true,
            competitionFromApi: true,
          },
        },
        processedScores: {
          trafficScore: 0.82,
          salesScore: 0.76,
          competitionScore: 0.41,
          rawScore: 0.66,
          normalizedScore: 0.80,
          displayScore: 80,
        },
        finalResult: {
          locationMultiplier: 1.14,
          grade: 8,
        },
        message: null,
      },
    },
    finance: {
      success: true,
      data: {
        unitEconomics: {
          sellingPrice: 12000,
          directMaterialCostPerUnit: 4000,
          directLaborCostPerUnit: 1100,
          variableOverheadPerUnit: 500,
          totalVariableCostPerUnit: 6079,
          contributionMarginPerUnit: 5921,
        },
        monthlyResult: {
          totalRevenue: 24960000,
          totalVariableCost: 12644320,
          fixedCostAllocation: 1900000,
          operatingProfit: 10415680,
          profitMargin: 0.42,
          foodCostRate: 0.33,
        },
        breakEven: {
          breakEvenQuantityPerDay: 13,
          breakEvenRevenuePerMonth: 4056000,
        },
        incremental: null,
      },
    },
    trend: {
      plc_stage: '성숙기',
      overall_trend: '하락',
      recent_trend: '최근 유지',
      latest_interest: 44.6,
      average_interest: 83.6,
      trend_speed: 'stable',
      peak_ratio: 0.45,
      growth_rate: -0.87,
      trend_score: 24,
      recommendation: '신중 검토',
      similar_patterns: [
        {
          name: '키토김밥',
          dtw_similarity: 0.68,
          dtw_dist: 0.32,
          lifecycle: '안정정착',
          peak_month: '2024-08',
        },
        {
          name: '달고나커피',
          dtw_similarity: 0.64,
          dtw_dist: 0.36,
          lifecycle: '유행후정체',
          peak_month: '2020-04',
        },
        {
          name: '붕어빵',
          dtw_similarity: 0.63,
          dtw_dist: 0.37,
          lifecycle: '계절성',
          peak_month: '2024-12',
        },
      ],
    },
    simulation: {
      success: true,
      data: {
        baseline: null,
        simulation: {
          adjustedRevenue: 19968000,
          totalOperatingProfit: 7952544,
          profitMargin: 0.40,
          breakEvenQuantity: 13,
          warningLevel: 'YELLOW',
          operationBurdenIndex: 1.76,
        },
        incremental: null,
      },
    },
    report: {
      verdict: '신중 검토',
      scores: {
        trend: 24,
        profitability: 100,
        sustainability: 60,
        total: 59,
      },
      riskFactors: [
        '주요 리스크 요인',
        '입지 경쟁도 높음',
      ],
      llmReport:
        '현재 마라탕 메뉴는 성숙기에 접어들며 안정적인 관심도를 유지하고 있지만, 최근 소비자 흥미가 감소하여 관심도가 44.55로 평균 83.62에 미치지 못하고 있습니다. 이는 시장 잠재력이 제한적임을 나타냅니다. 영업이익은 10,415,680원으로 이익률은 42%로 양호하나, 손익분기점인 13개 판매량을 달성해야 합니다.\n\n경쟁이 치열한 지역에서는 마라탕 창업이 어려울 수 있습니다. 따라서 창업을 고려할 때는 입지와 경쟁 상황을 신중히 분석해야 합니다. 결론적으로, 마라탕 창업은 시장 변화와 소비자 트렌드를 반영하여 전략적으로 접근해야 할 것입니다.',
    },
  },
};

// 시연을 위해 다양한 결과를 보여줄 수 있도록 추가 시나리오 — verdict별 1개씩
export const MOCK_ANALYSIS_BY_VERDICT: Record<string, AnalysisResponse> = {
  '도입 권장': {
    ...MOCK_ANALYSIS_RESPONSE,
    data: {
      ...MOCK_ANALYSIS_RESPONSE.data,
      report: {
        ...MOCK_ANALYSIS_RESPONSE.data.report,
        verdict: '도입 권장',
        scores: { trend: 78, profitability: 84, sustainability: 71, total: 82 },
      },
      simulation: {
        ...MOCK_ANALYSIS_RESPONSE.data.simulation,
        data: {
          ...MOCK_ANALYSIS_RESPONSE.data.simulation.data,
          simulation: {
            ...MOCK_ANALYSIS_RESPONSE.data.simulation.data.simulation,
            warningLevel: 'GREEN',
            operationBurdenIndex: 0.92,
          },
        },
      },
    },
  },
  '신중 검토': MOCK_ANALYSIS_RESPONSE,
  '불확실': {
    ...MOCK_ANALYSIS_RESPONSE,
    data: {
      ...MOCK_ANALYSIS_RESPONSE.data,
      report: {
        ...MOCK_ANALYSIS_RESPONSE.data.report,
        verdict: '불확실',
        scores: { trend: 51, profitability: 55, sustainability: 48, total: 51 },
      },
    },
  },
  '보류': {
    ...MOCK_ANALYSIS_RESPONSE,
    data: {
      ...MOCK_ANALYSIS_RESPONSE.data,
      report: {
        ...MOCK_ANALYSIS_RESPONSE.data.report,
        verdict: '보류',
        scores: { trend: 18, profitability: 30, sustainability: 25, total: 24 },
      },
      simulation: {
        ...MOCK_ANALYSIS_RESPONSE.data.simulation,
        data: {
          ...MOCK_ANALYSIS_RESPONSE.data.simulation.data,
          simulation: {
            ...MOCK_ANALYSIS_RESPONSE.data.simulation.data.simulation,
            warningLevel: 'RED',
            operationBurdenIndex: 2.4,
          },
        },
      },
    },
  },
};

/** 백엔드 GET /api/v1/trend/popular?limit=9 mock */
export const MOCK_POPULAR_KEYWORDS: TrendKeywordResponse[] = [
  { keyword: '두바이초콜릿', category: 'snack',  searchCount: 1284, lastSearchedAt: '2026-05-18T10:00:00' },
  { keyword: '요아정',       category: 'cafe',   searchCount:  982, lastSearchedAt: '2026-05-18T11:00:00' },
  { keyword: '탕후루',       category: 'snack',  searchCount:  871, lastSearchedAt: '2026-05-17T09:00:00' },
  { keyword: '마라탕후루',   category: 'dining', searchCount:  705, lastSearchedAt: '2026-05-17T16:00:00' },
  { keyword: '크로플',       category: 'cafe',   searchCount:  612, lastSearchedAt: '2026-05-16T12:00:00' },
  { keyword: '흑임자라떼',   category: 'cafe',   searchCount:  488, lastSearchedAt: '2026-05-16T14:00:00' },
  { keyword: '약과',         category: 'snack',  searchCount:  411, lastSearchedAt: '2026-05-15T10:00:00' },
  { keyword: '붕어빵',       category: 'snack',  searchCount:  388, lastSearchedAt: '2026-05-15T08:00:00' },
  { keyword: '로제떡볶이',   category: 'dining', searchCount:  322, lastSearchedAt: '2026-05-14T13:00:00' },
];

/** 백엔드 GET /api/v1/trend/lifecycles mock (Lifecycle enum 6종) */
export const MOCK_LIFECYCLES: LifecycleResponse[] = [
  { name: '단기급락',   description: '정점 후 빠르게 식는 유행',     color: '#E74C3C' },
  { name: '유행후정체', description: '정점 후 안정 없이 점진 감소',  color: '#7F8C8D' },
  { name: '계절성',     description: '매년 동일 시기 반복',          color: '#3498DB' },
  { name: '안정정착',   description: '정점 후 일부 매장에서 안정',   color: '#16A085' },
  { name: '지속성장',   description: '장기 정착 가능성',             color: '#27AE60' },
  { name: '재유행',     description: '주기적 재부상 패턴',           color: '#9B59B6' },
];

/** TrendBite 등록 키워드 목록 — searchMenuKeywords mock 소스 */
export const KNOWN_KW = [
  '마라탕', '마라꼬치', '마라샹궈', '마라전골', '마라탕후루',
  '탕후루', '흑당버블티',
  '두바이초콜릿', '요아정', '크로플',
  '흑임자라떼', '약과', '붕어빵', '로제떡볶이',
  '키토김밥', '달고나커피',
];

export function hasExactKeywordData(input: string): boolean {
  return KNOWN_KW.includes(input.trim());
}

/** category 한글 ↔ 영문 매핑 — 백엔드는 자유 문자열이지만 일관성을 위해 dropdown 사용 */
export const CATEGORY_OPTIONS: { value: string; label: string }[] = [
  { value: 'dining', label: '식사' },
  { value: 'cafe',   label: '카페·디저트' },
  { value: 'snack',  label: '간식·분식' },
  { value: 'pub',    label: '주점' },
  { value: 'other',  label: '기타' },
];

/** 와이어프레임 P1 랜딩의 라이프사이클 미리보기 데이터 (시드 패턴 3종) */
export const LANDING_LIFECYCLE_SAMPLES = [
  { name: '탕후루',     label: '단기급락',   data: [5, 10, 40, 100, 80, 40, 20, 10, 5, 2, 1, 1] },
  { name: '흑당버블티', label: '유행후정체', data: [10, 30, 80, 100, 90, 70, 50, 40, 30, 25, 20, 20] },
  { name: '마라탕',     label: '지속성장',   data: [10, 15, 25, 40, 55, 70, 85, 95, 100, 98, 97, 99] },
];
