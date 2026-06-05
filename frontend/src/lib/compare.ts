import type { SavedAnalysis } from './types';

export interface MenuGroup {
  menuName: string;
  items: SavedAnalysis[];
}

/** 같은 menuName이면서 address가 서로 다른 항목이 2건 이상인 묶음을 반환 */
export function groupByMenu(items: SavedAnalysis[]): MenuGroup[] {
  const map = new Map<string, SavedAnalysis[]>();
  for (const item of items) {
    const key = item.menuName.trim().toLowerCase();
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(item);
  }
  const result: MenuGroup[] = [];
  for (const [, group] of map) {
    const uniqueAddresses = new Set(group.map(i => (i.address || '').trim()));
    if (group.length >= 2 && uniqueAddresses.size >= 2) {
      result.push({ menuName: group[0].menuName, items: group });
    }
  }
  return result;
}

/** 비교 대상들에서 차이가 큰 축을 룰베이스로 한 문장 생성 (LLM 호출 없음) */
export function buildCompareSummary(items: SavedAnalysis[]): string {
  if (items.length < 2) return '';

  const scores = items.map(i => i.response.report.scores.total);
  const multipliers = items.map(i => i.response.location.data.finalResult.locationMultiplier);
  const competitions = items.map(i => i.response.location.data.rawMetrics.competitionCount);

  const maxScoreIdx = getMaxIndex(scores);
  const minScoreIdx = getMinIndex(scores);
  const maxMulIdx = getMaxIndex(multipliers);

  const menuName = items[0].menuName;
  const shortAddr = (idx: number) =>
    items[idx].address?.split(' ').slice(1, 3).join(' ') || `항목 ${idx + 1}`;

  const parts: string[] = [];

  const scoreDiff = scores[maxScoreIdx] - scores[minScoreIdx];
  if (scoreDiff >= 5) {
    parts.push(
      `같은 ${menuName}이라도 ${shortAddr(maxScoreIdx)}(${scores[maxScoreIdx]}점)이 ${shortAddr(minScoreIdx)}(${scores[minScoreIdx]}점)보다 ${scoreDiff}점 높음`
    );
  }

  const maxMul = Math.max(...multipliers);
  const minMul = Math.min(...multipliers);
  if (maxMul - minMul >= 0.04) {
    parts.push(`입지계수 ${shortAddr(maxMulIdx)} ${maxMul.toFixed(2)} vs 최저 ${minMul.toFixed(2)}`);
  }

  const maxComp = Math.max(...competitions);
  const minComp = Math.min(...competitions);
  const maxCompIdx = getMaxIndex(competitions);
  if (minComp > 0 && maxComp >= minComp * 1.5) {
    const ratio = (maxComp / minComp).toFixed(1);
    parts.push(`경쟁점포는 ${shortAddr(maxCompIdx)}이 약 ${ratio}배`);
  } else if (maxComp > 0 && minComp === 0) {
    parts.push(`경쟁점포 수 ${maxComp}개 vs 0개`);
  }

  if (parts.length === 0) {
    return `${menuName} ${items.length}건 비교 — 입지·점수 차이가 적어 조건이 유사합니다.`;
  }

  return parts.join(', ') + '.';
}

/** 최댓값 인덱스 반환 */
export function getMaxIndex(values: number[]): number {
  let idx = 0;
  for (let i = 1; i < values.length; i++) {
    if (values[i] > values[idx]) idx = i;
  }
  return idx;
}

/** 최솟값 인덱스 반환 */
export function getMinIndex(values: number[]): number {
  let idx = 0;
  for (let i = 1; i < values.length; i++) {
    if (values[i] < values[idx]) idx = i;
  }
  return idx;
}

/**
 * 기준값 대비 증감 레이블 반환
 * @example getDeltaLabel(82, 67) → "▲22%"
 */
export function getDeltaLabel(value: number, baseValue: number): string {
  if (baseValue === 0) return '';
  const pct = ((value - baseValue) / Math.abs(baseValue)) * 100;
  if (Math.abs(pct) < 0.5) return '';
  const sign = pct >= 0 ? '▲' : '▼';
  return `${sign}${Math.abs(pct).toFixed(0)}%`;
}
