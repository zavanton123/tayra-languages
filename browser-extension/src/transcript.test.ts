import { expect, it } from 'vitest';
import type { BridgeLine } from './bridge-types';
import { printable } from './transcript';

const lines: BridgeLine[] = [
  { beginMs: 1500, endMs: 3000, text: 'O gato dorme.', translation: 'The cat sleeps.', segments: [
    { text: 'O', term: 'O', lc: 'o', status: 99 }, { text: ' ' }, { text: 'gato', term: 'gato', lc: 'gato', status: 0 }, { text: ' ' },
    { text: 'dorme', term: 'dorme', lc: 'dorme', status: 2 }, { text: '.' },
  ] },
  { beginMs: 65_000, endMs: 66_000, text: 'Bom dia.', segments: [] },
];

it('marks the words saved from the video, those being learned and the new ones, with times and translations', () => {
  const printed = printable({
    videoId: 'abc', title: 'Gatos', channel: 'Canal', language: 'Portuguese', lines, human: [null, 'Good morning.'],
    changed: new Map([['gato', 1]]),
    saved: [{ term: 'dorme', lc: 'dorme', line: 'O gato dorme.', beginMs: 1500, savedAt: 0 }],
    madeAt: 0,
  });
  expect(printed.lines[0]!.pieces.map((p) => [p.text, p.mark])).toEqual([['O', null], [' ', null], ['gato', 'learning'], [' ', null], ['dorme', 'saved'], ['.', null]]);
  expect(printed.lines.map((l) => [l.time, l.seconds, l.translation, l.human])).toEqual([['0:01', 1, 'The cat sleeps.', null], ['1:05', 65, null, 'Good morning.']]);
  expect(printed.lines[1]!.pieces).toEqual([{ text: 'Bom dia.', mark: null }]);
  expect(printed.saved).toEqual([{ term: 'dorme', time: '0:01', seconds: 1, line: 'O gato dorme.' }]);
});
