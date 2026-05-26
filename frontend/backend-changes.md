# Backend Changes Required

프론트엔드 기능 구현 및 실서버 연동 시 확인된 백엔드 수정 항목입니다.

---

## 도메인별 추가 작업 항목

---

### 1. [Trend 도메인] GET /api/v1/trend/search-keywords 엔드포인트 구현

**에러**: 메뉴명 입력 시 "백엔드 오류(INTERNAL_SERVER_ERROR) — mock 데이터로 대체합니다"
**원인**: 해당 엔드포인트가 백엔드에 존재하지 않아 GlobalExceptionHandler의 범용 예외 핸들러(500)로 떨어짐

**수정 파일 3곳:**

#### `trend/repository/TrendKeywordRepository.java`
```java
// 아래 JPQL 쿼리 메서드 추가
@Query("SELECT t.keyword FROM TrendKeyword t WHERE LOWER(t.keyword) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY t.searchCount DESC")
List<String> findKeywordsBySubstring(@Param("q") String q, Pageable pageable);
```

#### `trend/service/TrendService.java`
```java
// 아래 메서드 추가
public List<String> searchKeywords(String q) {
    if (q == null || q.isBlank()) return Collections.emptyList();
    return keywordRepository.findKeywordsBySubstring(q.trim(), PageRequest.of(0, 20));
}
```

#### `trend/controller/TrendController.java`
```java
// 아래 엔드포인트 추가
/** GET /api/v1/trend/search-keywords?q=<query> — 키워드 포함 매칭 목록 */
@GetMapping("/search-keywords")
public ResponseEntity<List<String>> searchKeywords(
        @RequestParam(defaultValue = "") String q) {
    return ResponseEntity.ok(trendService.searchKeywords(q));
}
```

---

### 2. [Global 도메인] FastApiCallException 전용 예외 핸들러 추가

**에러**: 분석 시작 시 "백엔드 오류(INTERNAL_SERVER_ERROR) — mock 데이터로 대체합니다"
**원인**: `FastApiCallException`(Python FastAPI 서버 연결 실패)이 발생하면 처리 핸들러가 없어 범용 500 핸들러로 처리됨.
`FastApiCallException.java` Javadoc에는 "503으로 매핑한다"고 명시되어 있으나 구현 누락 상태.

**수정 파일 1곳:**

#### `global/exception/GlobalExceptionHandler.java`
```java
// import 추가
import com.trendbite.menuprofit.trend.exception.FastApiCallException;
import org.springframework.http.HttpStatus;

// 아래 핸들러 추가 (handleUnexpected 위에 삽입)
/** 503 - FastAPI(트렌드/리포트 서버) 호출 실패 */
@ExceptionHandler(FastApiCallException.class)
public ResponseEntity<Map<String, Object>> handleFastApiCall(FastApiCallException e) {
    log.error("[GlobalExceptionHandler] FastAPI 호출 실패: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(buildBody("SERVICE_UNAVAILABLE", e.getMessage()));
}
```

---

### 3. [Analysis 도메인] FastAPI 실패 시 부분 분석 폴백 + Menu 엔티티 저장 + menuId 응답 반환

**에러 2**: 분석 전체가 실패해 mock 데이터 리포트만 표시됨
**에러 3**: 실판매 기록 페이지에서 "백엔드 오류(BAD_REQUEST)" — SalesService가 Menu 엔티티를 DB에서 찾지 못함

**원인 (에러 2)**: `AnalysisOrchestrationService.run()`에서 FastAPI(trend/report) 호출 실패 시 예외가 전파되어 분석 전체가 중단됨.
입지·재무 분석은 성공해도 트렌드/리포트 실패 하나로 전체 응답이 500이 됨.

**원인 (에러 3)**: 프론트엔드가 분석 저장 시 클라이언트 생성 시퀀스 ID(1, 2, 3...)를 `menuId`로 사용하는데,
백엔드 `SalesService.record()`는 이 ID로 `Menu` 엔티티를 DB 조회 → 없으면 400 BAD_REQUEST 반환.
분석 실행 시 Menu 엔티티를 DB에 저장하고, 해당 ID를 응답으로 내려주면 해결됨.

**수정 파일 2곳:**

#### `trend/dto/response/AnalysisResponse.java`
```java
// DataDto에 menuId 필드 추가
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public static class DataDto {
    private Long menuId;                     // 실판매 기록 연동용 Menu 엔티티 ID
    private LocationAnalysisResponse location;
    private FinanceResponseDto       finance;
    private TrendResponse            trend;
    private SimulationResponseDto    simulation;
    private ReportResponse           report;
}
```

#### `trend/service/AnalysisOrchestrationService.java`
```java
// 1. import 추가
import com.trendbite.menuprofit.entity.Menu;
import com.trendbite.menuprofit.repository.MenuRepository;
import com.trendbite.menuprofit.trend.exception.FastApiCallException;
import java.util.Collections;
import java.util.List;

// 2. MenuRepository 필드 추가 (@RequiredArgsConstructor가 생성자 주입)
private final MenuRepository menuRepository;

// 3. run() 메서드 전체 교체
public AnalysisResponse run(AnalysisRequest request) {
    // 1. 입지
    LocationAnalysisResponse location = locationAnalysisService.analyze(request.getLocation());
    Double multiplier = location.getData().getFinalResult().getLocationMultiplier();

    // 2. 재무
    FinanceResponseDto finance = financeService.calculate(request.getFinance());

    // 3. Menu 엔티티 저장 — 실판매 기록(SalesService)에서 조회할 DB ID 생성
    Long menuId = saveMenuEntity(request);

    // 4. 트렌드 — FastAPI 실패 시 빈 응답으로 부분 분석 계속
    TrendResponse trend;
    try {
        trend = trendService.analyze(TrendRequest.builder()
                .keyword(request.getKeyword())
                .category(request.getCategory())
                .build());
    } catch (FastApiCallException e) {
        log.warn("[AnalysisOrchestration] FastAPI trend 실패 — 부분 분석 모드: {}", e.getMessage());
        trend = TrendResponse.builder().similarPatterns(Collections.emptyList()).build();
    }

    // 5. 시뮬레이션 (trendScore null이면 DEFAULT_TREND_SCORE_NORMALIZED=0.5 적용)
    SimulationResponseDto simulation = simulationService.simulate(
            buildSimulationRequest(request, trend, finance, multiplier));

    // 6. 리포트 — FastAPI 실패 시 플레이스홀더
    ReportResponse report;
    try {
        report = fastApiReportClient.generate(
                buildReportRequest(request.getKeyword(), trend, finance, multiplier));
    } catch (FastApiCallException e) {
        log.warn("[AnalysisOrchestration] FastAPI report 실패 — 플레이스홀더 사용: {}", e.getMessage());
        report = ReportResponse.builder()
                .verdict("불확실")
                .scores(ReportResponse.ScoreBreakdown.builder()
                        .trend(0).profitability(0).sustainability(0).total(0).build())
                .riskFactors(List.of("AI 리포트 서비스에 연결할 수 없습니다."))
                .llmReport("AI 리포트 서비스 연결에 실패했습니다. 입지·재무 수치를 바탕으로 검토해주세요.")
                .build();
    }

    return AnalysisResponse.builder()
            .success(true)
            .data(AnalysisResponse.DataDto.builder()
                    .menuId(menuId)
                    .location(location)
                    .finance(finance)
                    .trend(trend)
                    .simulation(simulation)
                    .report(report)
                    .build())
            .build();
}

// 4. 신규 private 헬퍼 — Menu 엔티티 생성·저장 후 ID 반환
private Long saveMenuEntity(AnalysisRequest req) {
    try {
        Menu menu = new Menu();
        menu.setMenuName(req.getFinance().getMenuInfo().getMenuName());
        menu.setCategory(req.getFinance().getMenuInfo().getCategory());
        menu.setSellingPrice(req.getFinance().getMenuInfo().getSellingPrice());
        menu.setIngredientCost(req.getFinance().getDirectMaterialCost().getIngredientCostPerUnit());
        menu.setExpectedDailySales(req.getFinance().getSalesAssumption().getExpectedDailySales());
        menu.setWorkingDaysPerWeek(req.getFinance().getSalesAssumption().getWorkingDaysPerWeek());
        menu.setTrendDurationWeeks(req.getFinance().getSalesAssumption().getTrendDurationWeeks());
        menu.setBohTimeMinutes(req.getFinance().getDirectLaborCost().getBohTimeMinutes());
        menu.setFohTimeMinutes(req.getFinance().getDirectLaborCost().getFohTimeMinutes());
        menu.setVariableOverheadCostPerUnit(req.getFinance().getVariableOverheadCostPerUnit());
        menu.setPackagingCostPerUnit(req.getFinance().getMenuInfo().getPackagingCostPerUnit());
        menu.setMonthlyRent(req.getFinance().getFixedCost().getMonthlyRent());
        menu.setMonthlyUtility(req.getFinance().getFixedCost().getManagementFee());
        menu.setMonthlyMisc(req.getFinance().getFixedCost().getMiscCost());
        if (req.getFinance().getBaselineProfit() != null) {
            menu.setBaselineMonthlyRevenue(
                    req.getFinance().getBaselineProfit().getCurrentMonthlyRevenue());
            menu.setBaselineMonthlyOperatingProfit(
                    req.getFinance().getBaselineProfit().getCurrentMonthlyOperatingProfit());
        }
        return menuRepository.save(menu).getId();
    } catch (Exception e) {
        log.warn("[AnalysisOrchestration] Menu 저장 실패 — menuId 없이 계속: {}", e.getMessage());
        return null;
    }
}
```

---

### 4. [Trend 도메인] ReportResponse JSON 직렬화 키 불일치 수정

**에러**: 프론트가 `riskFactors` / `llmReport`(camelCase)를 기대하지만 백엔드가 `risk_factors` / `llm_report`(snake_case)를 반환해 결과 페이지에서 `undefined` 처리됨.

**원인**: `ReportResponse.java`에 `@JsonProperty("risk_factors")`, `@JsonProperty("llm_report")`가 선언되어 Spring 직렬화 시에도 snake_case로 출력됨.
FastAPI로부터 역직렬화할 때만 snake_case 매핑이 필요하므로 `@JsonAlias`로 교체해야 함.

**수정 파일 1곳:**

#### `trend/dto/response/ReportResponse.java`
```java
// import 변경
// 기존: import com.fasterxml.jackson.annotation.JsonProperty;  ← 해당 필드에서 제거
// 추가: import com.fasterxml.jackson.annotation.JsonAlias;

// 기존 코드
@JsonProperty("risk_factors")
private List<String> riskFactors;

@JsonProperty("llm_report")
private String llmReport;

// 변경 후
@JsonAlias("risk_factors")   // FastAPI 역직렬화용. Spring 직렬화는 camelCase(riskFactors)로 출력
private List<String> riskFactors;

@JsonAlias("llm_report")     // FastAPI 역직렬화용. Spring 직렬화는 camelCase(llmReport)로 출력
private String llmReport;
```

**주의**: `TrendResponse.java` / `SimilarPattern.java`의 `@JsonProperty`는 프론트 타입(`types.ts`)이 snake_case를 그대로 사용하므로 건드리지 않음.

---

### 5. [트렌드 도메인] FastAPI /analyze — 기준 키워드 동반 조회 및 low_volume 플래그 반환

**배경**: 마라탕처럼 검색량이 많은 기준 키워드와 분석 대상을 같은 네이버 데이터랩 keywordGroups 호출에 넣으면,
동일 정규화 스케일(max=100) 안에서 상대 비율로 저검색 여부를 정확히 판별할 수 있다.
현재는 키워드 그룹마다 개별 정규화되어 절대 비교가 불가능하므로 신메뉴 판별이 부정확하다.

**요청 항목**:
- FastAPI `/analyze` 트렌드 조회 시 동일 keywordGroups에 기준 키워드(마라탕 또는 설정 가능한 seed 키워드)를 함께 포함
- 분석 대상 키워드의 상대 비율 또는 `low_volume: bool` 플래그를 TrendResponse에 추가 반환

**프론트 영향**:
- `src/lib/types.ts`의 `TrendResponse`에 `low_volume?: boolean | null` 필드 예약 완료
- `src/lib/api.ts`의 `isTrendWeak()` 헬퍼는 "백엔드 플래그 우선, 없으면 trend_score/latest_interest 임계값 프록시" 순서로 구현되어 있음 — 백엔드가 `low_volume`을 추가하면 프론트 수정 없이 자동 적용됨

---

## 구현 완료 항목 (프론트)

- `frontend/src/lib/types.ts` — `AnalysisResponse.data.menuId?: number` 추가
- `frontend/src/lib/userContext.tsx` — `saveAnalysis()` 에 `menuId?: number` 파라미터 추가; 백엔드 menuId 우선 사용, 없으면 클라이언트 시퀀스 폴백
- `frontend/src/app/analyze/page.tsx` — `saveAnalysis()` 호출 시 `res.data.menuId` 전달

---

## 도메인별 미구현 항목 요약 테이블

> 아래 표는 백엔드에서 **아직 구현되지 않은** 항목만 기재.  
> 이미 구현된 항목(CORS 설정, Location nearestFallback 등)은 제거.  
> 상세 코드는 위 "도메인별 추가 작업 항목" 절의 해당 번호를 참고.

---

### C-0: Global 도메인

| ID | 항목 | 상태 | 대상 파일 | 상세 |
|----|------|------|-----------|------|
| G1 | `FastApiCallException` 전용 예외 핸들러 추가 (HTTP 503) | ❌ 미구현 | `global/exception/GlobalExceptionHandler.java` | → 항목 2 참고 |

> ~~CORS allowedOrigins 설정~~ → ✅ `CorsConfig.java` 이미 구현(`http://localhost:3000` 허용)

---

### C-1: Trend 도메인

| ID | 항목 | 상태 | 대상 파일 | 상세 |
|----|------|------|-----------|------|
| T1 | `GET /api/v1/trend/search-keywords?q=` 엔드포인트 신규 구현 | ❌ 미구현 | `TrendKeywordRepository` / `TrendService` / `TrendController` | → 항목 1 참고 |
| T2 | `ReportResponse.java` `@JsonProperty` → `@JsonAlias` 교체 (Spring 직렬화 키 불일치 수정) | ❌ 미구현 | `trend/dto/response/ReportResponse.java` | → 항목 4 참고 |
| T3 | FastAPI `/analyze` — 기준 키워드(마라탕) 동반 조회 + `low_volume` 플래그 반환 | ❌ 미구현 | FastAPI trend 분석 로직 | → 항목 5 참고 |

---

### C-2: Location 도메인

미구현 항목 없음.

> ~~L1: nearestFallback 로직~~ → ✅ 이미 동작 확인

---

### C-3: Analysis 도메인

| ID | 항목 | 상태 | 대상 파일 | 상세 |
|----|------|------|-----------|------|
| F1 | `AnalysisOrchestrationService.run()` 리팩토링: FastAPI 실패 시 부분 분석 폴백 + `Menu` 엔티티 저장 + `menuId` 응답 포함 | ❌ 미구현 | `trend/service/AnalysisOrchestrationService.java` / `trend/dto/response/AnalysisResponse.java` | → 항목 3 참고 |

---

### C-4: Sales 도메인

| ID | 항목 | 상태 | 대상 파일 | 상세 |
|----|------|------|-----------|------|
| S1 | 분석 응답(`AnalysisResponse.DataDto`)에 `menuId` 필드 포함 — `SalesService`의 Menu 조회 연동 | ❌ 미구현 | `trend/dto/response/AnalysisResponse.java` | → 항목 3 참고 (F1과 동일 수정 세트) |

---

## 우선순위 순서 (권장)

1. **G1** — FastApiCallException 핸들러: 분석 전체 500 방지, 범위 좁음
2. **F1 + S1** — AnalysisOrchestrationService 리팩토링 + menuId 반환: 실판매 기록 연동 전제 조건
3. **T1** — search-keywords 엔드포인트: 메뉴명 자동완성 기능
4. **T2** — ReportResponse @JsonAlias: AI 리포트 필드 undefined 수정
5. **T3** — FastAPI 기준 키워드 동반 조회 + low_volume 플래그: 신메뉴 트렌드 건너뜀 정확도 향상 (프론트는 플래그 준비 완료)
