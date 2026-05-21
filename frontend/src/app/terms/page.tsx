import { PendingMarker } from '@/components/Primitives';

export default function TermsPage() {
  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: '40px 24px', background: '#fff', minHeight: 'calc(100vh - 104px)' }}>
      <div className="wf-eyebrow" style={{ marginBottom: 8 }}>LEGAL · 이용약관</div>
      <h1 className="wf-h1" style={{ marginBottom: 8 }}>이용약관</h1>
      <p className="wf-muted" style={{ marginBottom: 24, fontSize: 12.5 }}>
        최종 수정일: 2026-05-19 (데모용 placeholder)
      </p>

      <div style={{ marginBottom: 24 }}>
        <PendingMarker>약관 본문 작성 필요 — 5/20 회의 논의 항목</PendingMarker>
      </div>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>제1조 (목적)</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          본 약관은 TrendBite("이하 서비스")가 제공하는 신메뉴 수익성 예측 시뮬레이션 서비스의 이용 조건 및 절차, 이용자와 서비스의 권리, 의무, 책임사항을 규정함을 목적으로 합니다. 본 서비스는 졸업 프로젝트의 일환으로 제공되며, 분석 결과는 참고용 정보입니다.
        </p>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>제2조 (서비스의 내용)</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          서비스는 다음의 기능을 제공합니다.
        </p>
        <ul style={{ fontSize: 13, lineHeight: 1.8, color: 'var(--tb-ink-2)', paddingLeft: 20 }}>
          <li>네이버 데이터랩 등 공공 데이터 기반 메뉴 트렌드 분석</li>
          <li>서울 상권 데이터 기반 입지 분석 및 보정계수 산정</li>
          <li>입력값 기반 영업이익·손익분기점 재무 시뮬레이션</li>
          <li>LangGraph 기반 AI 종합 리포트 및 위험 요인 도출</li>
          <li>실판매 데이터 기록을 통한 예측 보정</li>
        </ul>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>제3조 (결과의 면책)</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          본 서비스가 제공하는 모든 분석 결과는 통계적 추정에 기반한 참고 자료이며, 실제 매출 및 수익을 보장하지 않습니다. 외식업 창업·확장에 관한 최종 의사결정은 이용자 본인의 판단과 책임 하에 이루어져야 합니다.
        </p>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>제4조 (사용자 식별)</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          본 서비스는 별도의 회원가입 없이 이용 가능하나, 분석 결과 저장 및 실판매 기록 기능을 사용하기 위해 닉네임 또는 가게명을 등록하실 수 있습니다. 등록된 닉네임은 동일 디바이스에서 본인의 분석 이력을 다시 조회하는 용도로만 사용됩니다.
        </p>
      </section>

      <section style={{ marginBottom: 24 }}>
        <h2 className="wf-h2" style={{ marginBottom: 8 }}>제5조 (데이터 출처)</h2>
        <p style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--tb-ink-2)' }}>
          본 서비스는 다음 공공 데이터 소스를 활용합니다.
        </p>
        <ul style={{ fontSize: 13, lineHeight: 1.8, color: 'var(--tb-ink-2)', paddingLeft: 20 }}>
          <li>네이버 데이터랩 검색 트렌드 API</li>
          <li>서울 열린데이터광장 — 상권 매출·집객·유동인구 데이터</li>
          <li>소상공인시장진흥공단 상가(상권)정보 데이터</li>
          <li>카카오 우편번호·지도 API</li>
        </ul>
      </section>
    </div>
  );
}
