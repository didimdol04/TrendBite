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

/**
 * 한글 조사 선택 헬퍼.
 * 단어의 마지막 한글 음절을 찾아 받침 유무로 조사를 결정.
 * (charCode - 0xAC00) % 28 !== 0 이면 받침 있음.
 */
export function josa(
  word: string,
  type: '을를' | '은는' | '이가' | '과와' | '이라도'
): string {
  let lastKorean = '';
  for (let i = word.length - 1; i >= 0; i--) {
    const code = word.charCodeAt(i);
    if (code >= 0xAC00 && code <= 0xD7A3) { lastKorean = word[i]; break; }
  }
  const hasFinal = lastKorean
    ? (lastKorean.charCodeAt(0) - 0xAC00) % 28 !== 0
    : false; // 한글 없으면 받침 없는 형태
  const table: Record<string, [string, string]> = {
    '을를':   ['을', '를'],
    '은는':   ['은', '는'],
    '이가':   ['이', '가'],
    '과와':   ['과', '와'],
    '이라도': ['이라도', '라도'],
  };
  return table[type][hasFinal ? 0 : 1];
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
  const minCompIdx = getMinIndex(competitions);

  const shortAddr = (idx: number) => {
    const addr = items[idx].address;
    if (!addr) return `항목 ${String.fromCharCode(65 + idx)}`;
    const parts = addr.split(' ').filter(Boolean);
    return parts.slice(1, 3).join(' ') || `항목 ${String.fromCharCode(65 + idx)}`;
  };

  const allSameMenu = items.every(
    i => i.menuName.trim().toLowerCase() === items[0].menuName.trim().toLowerCase()
  );

  const scoreDiff = scores[maxScoreIdx] - scores[minScoreIdx];
  const maxMul = Math.max(...multipliers);
  const minMul = Math.min(...multipliers);
  const maxComp = Math.max(...competitions);
  const minComp = Math.min(...competitions);

  const parts: string[] = [];

  if (allSameMenu) {
    const menu = items[0].menuName;

    if (scoreDiff === 0) {
      parts.push(`같은 ${menu}${josa(menu, '이라도')} ${items.length}곳 모두 동일 점수(${scores[0]}점)`);
    } else if (scoreDiff >= 3) {
      const addrBest = shortAddr(maxScoreIdx);
      const addrWorst = shortAddr(minScoreIdx);
      parts.push(
        `같은 ${menu}${josa(menu, '이라도')} ` +
        `${addrBest}(${scores[maxScoreIdx]}점)${josa(addrBest, '이가')} ` +
        `${addrWorst}(${scores[minScoreIdx]}점)보다 ${scoreDiff}점 높음`
      );
    } else {
      parts.push(`같은 ${menu}${josa(menu, '이라도')} 점수 차이 ${scoreDiff}점`);
    }

    if (maxMul - minMul >= 0.04) {
      parts.push(
        `입지계수 ${shortAddr(maxMulIdx)} ${maxMul.toFixed(2)} vs 최저 ${minMul.toFixed(2)}`
      );
    }

    if (maxComp !== minComp && maxComp > 0) {
      const maxCompIdx = getMaxIndex(competitions);
      const addrHigh = shortAddr(maxCompIdx);
      const ratio = minComp > 0
        ? `약 ${(maxComp / minComp).toFixed(1)}배`
        : `${maxComp}개 vs 0개`;
      parts.push(`경쟁점포 ${addrHigh}${josa(addrHigh, '이가')} ${ratio} 많음`);
    }

    if (parts.length === 0) {
      return `${menu} ${items.length}건 비교 — 입지·점수 차이가 적어 조건이 유사합니다.`;
    }
  } else {
    // 다른 메뉴 혼합 — 인사이트형 문장
    const bestMenu = items[maxScoreIdx].menuName;
    const bestAddr = shortAddr(maxScoreIdx);

    if (scoreDiff === 0) {
      parts.push(`${items.length}개 항목 점수 동일(${scores[0]}점) — 입지·재무 지표로 비교 권장`);
    } else {
      const worstMenu = items[minScoreIdx].menuName;
      const worstAddr = shortAddr(minScoreIdx);
      parts.push(
        `${bestMenu}${josa(bestMenu, '이가')} ${bestAddr}(${scores[maxScoreIdx]}점)에서 가장 유리 — ` +
        `${worstMenu}(${worstAddr}, ${scores[minScoreIdx]}점) 대비 ${scoreDiff}점 높음`
      );
    }

    if (maxMul - minMul >= 0.04) {
      const bestMulMenu = items[maxMulIdx].menuName;
      parts.push(
        `입지계수 ${bestMulMenu}(${maxMul.toFixed(2)}) 최고, 최저 ${minMul.toFixed(2)}와 차이 있음`
      );
    }

    const lowComp = competitions[minCompIdx];
    if (maxComp > 0 && maxComp !== lowComp) {
      const lowCompMenu = items[minCompIdx].menuName;
      parts.push(
        `경쟁강도 ${lowCompMenu}(${lowComp}개)${josa(lowCompMenu, '이가')} 가장 낮아 진입 여건 유리`
      );
    } else if (maxComp === 0) {
      parts.push('경쟁점포 없어 초기 진입 유리');
    }

    if (parts.length === 0) {
      return `메뉴 ${items.length}건 비교 — 입지·점수 차이가 적어 조건이 유사합니다.`;
    }
  }

  return parts.join('. ') + '.';
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

/**
 * 여러 시리즈의 앞쪽 평탄/0 구간을 트리밍.
 * 전체 최댓값의 thresholdRatio 이상인 값이 처음 나타나는 인덱스 직전부터 반환.
 */
export function trimLeadingFlat(
  series: number[][],
  thresholdRatio = 0.1
): { trimmedSeries: number[][]; startIndex: number } {
  if (series.length === 0) return { trimmedSeries: [], startIndex: 0 };

  const allVals = series.flat().filter(isFinite);
  if (allVals.length === 0) return { trimmedSeries: series, startIndex: 0 };

  const overallMax = Math.max(...allVals);
  const threshold = overallMax * thresholdRatio;
  const maxLen = Math.max(...series.map(s => s.length));

  let firstMeaningful = maxLen - 1;
  for (let i = 0; i < maxLen; i++) {
    if (series.some(s => (s[i] ?? 0) >= threshold)) {
      firstMeaningful = i;
      break;
    }
  }

  const startIndex = Math.max(0, firstMeaningful - 1);
  return {
    trimmedSeries: series.map(s => s.slice(startIndex)),
    startIndex,
  };
}

/**
 * 상대 x축 눈금 라벨 생성 (실제 날짜 없이 상대적 개월 수로 표기)
 */
export function buildRelativeAxisTicks(
  length: number
): { index: number; label: string }[] {
  if (length <= 0) return [];
  if (length === 1) return [{ index: 0, label: '최근' }];

  const ticks: { index: number; label: string }[] = [];
  ticks.push({ index: length - 1, label: '최근' });

  if (length >= 6) {
    const midIdx = Math.floor(length / 2);
    const monthsAgo = length - 1 - midIdx;
    ticks.push({ index: midIdx, label: `−${monthsAgo}개월` });
  }

  if (length >= 12) {
    ticks.push({ index: 0, label: `−${length - 1}개월` });
  }

  return ticks.sort((a, b) => a.index - b.index);
}
