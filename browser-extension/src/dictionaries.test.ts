import { describe, expect, it } from 'vitest';
import { DEFAULT_DICTIONARIES, dictionaryNamed, dictionaryProblem, dictionaryUrl, validDictionaries } from './dictionaries';

const words = { word: 'pão de queijo', from: 'pt', to: 'ru', language: 'Portuguese' };

describe('dictionaryUrl', () => {
  it('fills the word and the languages in, encoded', () => {
    expect(dictionaryUrl(DEFAULT_DICTIONARIES[1]!, words)).toBe('https://translate.google.com/?sl=pt&tl=ru&text=p%C3%A3o%20de%20queijo&op=translate');
    expect(dictionaryUrl(DEFAULT_DICTIONARIES[0]!, { ...words, word: 'pão', language: 'Old English' })).toBe('https://en.wiktionary.org/wiki/p%C3%A3o#Old_English');
  });
});

describe('dictionaries kept in the settings', () => {
  it('turns down what could not be opened', () => {
    expect(dictionaryProblem({ name: '', url: 'https://a.example/{word}' })).toBe('Give it a name.');
    expect(dictionaryProblem({ name: 'A', url: 'javascript:alert(1)//{word}' })).toBe('The address must start with https://');
    expect(dictionaryProblem({ name: 'A', url: 'https://a.example/' })).toBe('Write {word} where the word goes.');
    expect(dictionaryProblem({ name: 'Glosbe', url: 'https://a.example/{word}' }, DEFAULT_DICTIONARIES)).toBe('There is one with that name already.');
    expect(dictionaryProblem({ name: 'Mine', url: 'https://a.example/{word}' }, DEFAULT_DICTIONARIES)).toBeNull();
  });

  it('keeps the good ones of what was saved, and the usual ones when nothing was', () => {
    expect(validDictionaries(undefined)).toEqual(DEFAULT_DICTIONARIES);
    expect(validDictionaries([{ name: ' Mine ', url: 'https://a.example/{word}' }, { name: 'Bad', url: 'ftp://x/{word}' }, 7])).toEqual([{ name: 'Mine', url: 'https://a.example/{word}' }]);
    expect(validDictionaries([])).toEqual([]);
  });

  it('opens the one used last, or the first', () => {
    expect(dictionaryNamed(DEFAULT_DICTIONARIES, 'Glosbe')?.name).toBe('Glosbe');
    expect(dictionaryNamed(DEFAULT_DICTIONARIES, 'Gone')?.name).toBe('Wiktionary');
    expect(dictionaryNamed([], null)).toBeNull();
  });
});
