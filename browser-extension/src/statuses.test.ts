import { describe, expect, it } from 'vitest';
import { IGNORED, KNOWN, UNKNOWN, nextStatus, statusName } from './statuses';

describe('nextStatus', () => {
  it('steps a word up, and from known back to learning 1', () => {
    expect([UNKNOWN, 1, 2, 3, 4, KNOWN, IGNORED, null].map((s) => nextStatus(s))).toEqual([1, 2, 3, 4, KNOWN, 1, 1, 1]);
  });

  it('sets a word aside whatever it was', () => {
    expect(nextStatus(3, true)).toBe(IGNORED);
  });

  it('names the statuses', () => {
    expect([UNKNOWN, 2, KNOWN, IGNORED].map(statusName)).toEqual(['New', 'Learning 2', 'Known', 'Ignored']);
  });
});
