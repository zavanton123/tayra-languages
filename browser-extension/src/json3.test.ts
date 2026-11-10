import { describe, expect, it } from 'vitest';
import { cueAt, parseJson3 } from './json3';

const track = JSON.stringify({
  events: [
    { tStartMs: 0, dDurationMs: 5000, id: 1, wpWinPosId: 1 },
    { tStartMs: 1200, dDurationMs: 2000, segs: [{ utf8: 'Olá,' }, { utf8: ' tudo\nbem?' }] },
    { tStartMs: 3300, dDurationMs: 1500, aAppend: 1, segs: [{ utf8: '\n' }] },
    { tStartMs: 3400, dDurationMs: 1000, segs: [{ utf8: '  ' }] },
    { tStartMs: 5000, dDurationMs: 2500, segs: [{ utf8: 'Tudo ótimo.' }] },
  ],
});

describe('parseJson3', () => {
  it('keeps the lines with text, joined into one line each', () => {
    expect(parseJson3(track)).toEqual([
      { beginMs: 1200, endMs: 3200, text: 'Olá, tudo bem?' },
      { beginMs: 5000, endMs: 7500, text: 'Tudo ótimo.' },
    ]);
  });
});

describe('cueAt', () => {
  const cues = parseJson3(track);
  it('finds the line showing', () => {
    expect(cueAt(cues, 1200)?.text).toBe('Olá, tudo bem?');
    expect(cueAt(cues, 6000)?.text).toBe('Tudo ótimo.');
  });
  it('finds nothing before, between and after the lines', () => {
    expect(cueAt(cues, 100)).toBeNull();
    expect(cueAt(cues, 4000)).toBeNull();
    expect(cueAt(cues, 9000)).toBeNull();
  });
});
