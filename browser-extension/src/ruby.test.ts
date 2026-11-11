import { expect, it } from 'vitest';
import { shownPieces } from './ruby';

const taberu = { text: '食べる', term: '食べる', lc: '食べる', ruby: [{ text: '食', reading: 'た' }, { text: 'べる', reading: '' }], romaji: 'taberu' };

it('puts the readings over the pieces that need them, or romaji over the word', () => {
  expect(shownPieces(taberu, 'readings')).toEqual([{ text: '食', above: 'た' }, { text: 'べる', above: null }]);
  expect(shownPieces(taberu, 'romaji')).toEqual([{ text: '食べる', above: 'taberu' }]);
  expect(shownPieces(taberu, 'off')).toEqual([{ text: '食べる', above: null }]);
});

it('reads a word written otherwise than looked up as a whole, and leaves words without readings alone', () => {
  expect(shownPieces({ ...taberu, text: 'タベル' }, 'readings')).toEqual([{ text: 'タベル', above: 'たべる' }]);
  expect(shownPieces({ text: 'gato', term: 'gato', lc: 'gato' }, 'readings')).toEqual([{ text: 'gato', above: null }]);
});
