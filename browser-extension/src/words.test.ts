import { describe, expect, it } from 'vitest';
import type { BridgeLine } from './bridge-types';
import { groupCounts, knownShare, linesWith, ordered, suggested, videoWords } from './words';

const line = (...words: [string, number][]): BridgeLine => ({
  beginMs: 0,
  endMs: 1000,
  text: words.map(([w]) => w).join(' '),
  segments: words.flatMap(([w, status]) => [{ text: w, term: w, lc: w.toLowerCase(), status }, { text: ' ' }]),
});

const lines = [line(['O', 99], ['gato', 0], ['dorme', 1]), line(['o', 99], ['cão', 0]), line(['gato', 0], ['e', 98])];

describe('videoWords', () => {
  it('counts each word once, in the order the video says them, with its rank and first line', () => {
    const words = videoWords(lines, new Map(), { gato: 900, o: 1 });
    expect(words.map((w) => [w.lc, w.count, w.rank, w.line])).toEqual([
      ['o', 2, 1, 0],
      ['gato', 2, 900, 0],
      ['dorme', 1, null, 0],
      ['cão', 1, null, 1],
      ['e', 1, null, 2],
    ]);
  });

  it('takes a status changed since the lines came', () => {
    const words = videoWords(lines, new Map([['gato', 99]]), {});
    expect(words.find((w) => w.lc === 'gato')!.status).toBe(99);
  });
});

describe('order and groups', () => {
  const words = videoWords(lines, new Map(), { gato: 900, cão: 400, o: 1 });

  it('puts the most common first, and words the list lacks last', () => {
    expect(ordered(words, 'rank').map((w) => w.lc)).toEqual(['o', 'cão', 'gato', 'dorme', 'e']);
    expect(ordered(words, 'count').map((w) => w.lc).slice(0, 2)).toEqual(['o', 'gato']);
  });

  it('suggests only new words, most common first', () => {
    expect(suggested(words, 5).map((w) => w.lc)).toEqual(['cão', 'gato']);
  });

  it('counts the groups, and the share of what is said that is known or set aside', () => {
    expect(groupCounts(words)).toEqual({ new: 2, learning: 1, known: 1, ignored: 1 });
    // o twice and e once of the seven words said.
    expect(knownShare(words)).toBeCloseTo(3 / 7);
  });
});

describe('linesWith', () => {
  it('finds the other lines that say the word', () => {
    expect(linesWith(lines, 'gato', 0, 5)).toEqual([2]);
    expect(linesWith(lines, 'o', -1, 1)).toEqual([0]);
  });
});
