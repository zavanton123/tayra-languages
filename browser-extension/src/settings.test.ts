import { describe, expect, it } from 'vitest';
import { DEFAULT_SETTINGS, hidden, normalize } from './settings';

describe('normalize', () => {
  it('fills in what was never saved and brings back what is out of range', () => {
    expect(normalize(undefined)).toEqual(DEFAULT_SETTINGS);
    const s = normalize({ fontSize: 9, backgroundOpacity: -5, position: 200, hideMode: 'nonsense', autoPause: true });
    expect([s.fontSize, s.backgroundOpacity, s.position, s.hideMode, s.autoPause]).toEqual([4, 0, 80, 'off', true]);
  });
  it('keeps only the word groups and panel choices it knows', () => {
    const s = normalize({ wordsShown: ['known', 'bogus', 'new'], panelTab: 'nope', wordsOrder: 'count' });
    expect([s.wordsShown, s.panelTab, s.wordsOrder]).toEqual([['new', 'known'], 'subtitles', 'count']);
  });
  it('keeps the dictionaries chosen, even none, and the one used last', () => {
    const s = normalize({ dictionaries: [{ name: 'Mine', url: 'https://a.example/{word}' }], lastDictionary: 'Mine' });
    expect([s.dictionaries.map((d) => d.name), s.lastDictionary]).toEqual([['Mine'], 'Mine']);
    expect(normalize({ dictionaries: [] }).dictionaries).toEqual([]);
  });
});

describe('hidden', () => {
  it('reveals the translation first, then the line', () => {
    expect([0, 1, 2].map((n) => hidden('translation-then-subtitle', n))).toEqual([
      { line: true, translation: true },
      { line: true, translation: false },
      { line: false, translation: false },
    ]);
  });
  it('hides one of them until revealed once', () => {
    expect(hidden('subtitle', 0)).toEqual({ line: true, translation: false });
    expect(hidden('translation', 1)).toEqual({ line: false, translation: false });
    expect(hidden('off', 0)).toEqual({ line: false, translation: false });
  });
});
