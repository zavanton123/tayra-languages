import { describe, expect, it } from 'vitest';
import { currentStatus, localLines } from './lines';

describe('localLines', () => {
  it('gives each line the translation showing at its middle', () => {
    const lines = localLines(
      [{ beginMs: 0, endMs: 2000, text: 'Olá' }, { beginMs: 2000, endMs: 4000, text: 'Tchau' }],
      [{ beginMs: 0, endMs: 1900, text: 'Hello' }],
    );
    expect(lines.map((l) => [l.text, l.translation])).toEqual([['Olá', 'Hello'], ['Tchau', null]]);
  });
});

describe('currentStatus', () => {
  it('takes a change over the status the line came with', () => {
    const segment = { text: 'gato', term: 'gato', lc: 'gato', status: 0 };
    expect(currentStatus(segment, new Map())).toBe(0);
    expect(currentStatus(segment, new Map([['gato', 2]]))).toBe(2);
    expect(currentStatus({ text: '.' }, new Map([['gato', 2]]))).toBeNull();
  });
});
