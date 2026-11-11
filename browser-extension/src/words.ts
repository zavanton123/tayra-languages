import type { BridgeLine } from './bridge-types';
import { currentStatus } from './lines';
import { IGNORED, KNOWN, UNKNOWN } from './statuses';

/** A word of the video, counted over all its lines. */
export interface VideoWord {
  lc: string;
  /** The text to look it up and save it by. */
  term: string;
  /** As it was first written in the video. */
  text: string;
  status: number;
  /** How many times the video says it. */
  count: number;
  /** Its place in the language's frequency list, or null when the list lacks it. */
  rank: number | null;
  /** The first line that has it. */
  line: number;
}

export type WordGroup = 'new' | 'learning' | 'known' | 'ignored';
export const WORD_GROUPS: WordGroup[] = ['new', 'learning', 'known', 'ignored'];

/** In the video's order, by the frequency list, or by how often the video says it. */
export type WordOrder = 'video' | 'rank' | 'count';

export function groupOf(status: number): WordGroup {
  if (status === KNOWN) return 'known';
  if (status === IGNORED) return 'ignored';
  if (status >= 1 && status <= 4) return 'learning';
  return 'new';
}

/** Each word of [lines] once, with its status as it is now, in the order the video says them. */
export function videoWords(lines: BridgeLine[], changed: ReadonlyMap<string, number>, ranks: Readonly<Record<string, number>>): VideoWord[] {
  const words = new Map<string, VideoWord>();
  lines.forEach((line, index) => {
    for (const segment of line.segments) {
      if (!segment.lc || !segment.term) continue;
      const word = words.get(segment.lc);
      if (word) word.count++;
      else {
        words.set(segment.lc, {
          lc: segment.lc,
          term: segment.term,
          text: segment.text,
          status: currentStatus(segment, changed) ?? UNKNOWN,
          count: 1,
          rank: ranks[segment.lc] ?? null,
          line: index,
        });
      }
    }
  });
  return [...words.values()];
}

export function ordered(words: VideoWord[], order: WordOrder): VideoWord[] {
  const byRank = (a: VideoWord, b: VideoWord) => (a.rank ?? Infinity) - (b.rank ?? Infinity);
  switch (order) {
    case 'video':
      return [...words];
    case 'rank':
      return [...words].sort((a, b) => byRank(a, b) || b.count - a.count);
    case 'count':
      return [...words].sort((a, b) => b.count - a.count || byRank(a, b));
  }
}

/** The new words most worth learning first: the most common in the language, then the most said in the video. */
export function suggested(words: VideoWord[], limit: number): VideoWord[] {
  return ordered(words.filter((w) => groupOf(w.status) === 'new'), 'rank').slice(0, limit);
}

export function groupCounts(words: VideoWord[]): Record<WordGroup, number> {
  const counts: Record<WordGroup, number> = { new: 0, learning: 0, known: 0, ignored: 0 };
  for (const word of words) counts[groupOf(word.status)]++;
  return counts;
}

/** The share of the words the video says, every time it says them, that are known or set aside, 0 to 1. */
export function knownShare(words: VideoWord[]): number {
  let all = 0;
  let known = 0;
  for (const word of words) {
    all += word.count;
    const group = groupOf(word.status);
    if (group === 'known' || group === 'ignored') known += word.count;
  }
  return all ? known / all : 0;
}
