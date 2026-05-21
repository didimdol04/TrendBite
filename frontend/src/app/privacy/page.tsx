import { PendingMarker } from '@/components/Primitives';

export default function PrivacyPage() {
  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '40px 24px', background: '#fff', minHeight: 'calc(100vh - 104px)' }}>
      <div className="wf-eyebrow" style={{ marginBottom: 8 }}>LEGAL · 개인정보처리방침</div>
      <h1 className="wf-h1" style={{ marginBottom: 8 }}>개인정보처리방침</h1>
      <p className="wf-muted" style={{ marginBottom: 24, fontSize: 12.5 }}>
        최종 수정일: 2026-05-19 (데모용 placeholder)
      </p>

      <div style={{ marginBottom: 24 }}>
        <PendingMarker>본문 작성 필요 — 5/20 회의 논의 항목</PendingMarker>
      </div>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>1. 수집하는 개인정보 항목</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          본 서비스는 다음의 정보를 수집합니다.
        </p>
        <ul style={{ fontSize: 13, lineHeight: 1.8, color: 'var(--tb-ink-2)', paddingLeft: 20 }}>
          <li><b>필수</b>: 닉네임 또는 가게명 (분석 이력 식별 용도)</li>
          <li><b>분석 입력값</b>: 메뉴명, 카테고리, 매장 주소·좌표, 가격·원가·고정비 등 시뮬레이션 입력 정보</li>
          <li><b>실판매 기록</b>: 일자별 판매량·매출 (사용자가 직접 입력 시)</li>
          <li><b>자동 수집</b>: 접속 IP, 브라우저 종류 (보안·통계 목적)</li>
        </ul>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>2. 개인정보의 이용 목적</h2>
        <ul style={{ fontSize: 13, lineHeight: 1.8, color: 'var(--tb-ink-2)', paddingLeft: 20 }}>
          <li>분석 결과 저장 및 동일 닉네임 재진입 시 이력 조회</li>
          <li>실판매 데이터를 기반으로 한 예측 보정 모델 운영</li>
          <li>서비스 개선을 위한 통계 분석 (개인 식별 정보 제외)</li>
        </ul>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>3. 외부 API로 전송되는 정보</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          입력하신 정보 중 일부는 분석을 위해 다음 외부 서비스로 전송될 수 있습니다.
        </p>
        <ul style={{ fontSize: 13, lineHeight: 1.8, color: 'var(--tb-ink-2)', paddingLeft: 20 }}>
          <li>네이버 데이터랩: 메뉴명·카테고리 (트렌드 검색)</li>
          <li>서울 열린데이터광장 / 소상공인시장진흥공단: 매장 좌표 (상권 매칭)</li>
          <li>OpenAI/Anthropic 등 LLM 제공사: 분석 결과 (AI 리포트 생성)</li>
        </ul>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)', marginTop: 8 }}>
          닉네임 또는 개인 식별 정보는 외부 API로 전송되지 않습니다.
        </p>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>4. 개인정보의 보유 및 파기</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          현재 데모 버전은 모든 데이터를 사용자 디바이스 브라우저 저장소(localStorage)에 보관합니다. 사용자가 브라우저 데이터를 삭제하면 모든 분석 이력과 실판매 기록이 함께 삭제됩니다.
        </p>
        <div style={{ marginTop: 8 }}>
          <PendingMarker>실서비스 출시 시 백엔드 DB 보유 정책 합의 필요</PendingMarker>
        </div>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>5. 이용자의 권리</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          이용자는 언제든지 헤더의 "로그아웃" 버튼을 통해 닉네임 연결을 해제할 수 있으며, 브라우저 저장소 정리를 통해 모든 데이터를 삭제할 수 있습니다.
        </p>
      </section>
    </div>
  );
}
