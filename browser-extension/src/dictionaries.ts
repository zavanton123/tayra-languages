/** A website to look a word up on: [url] has {word} where the word goes. */
export interface DictionaryLink {
  name: string;
  url: string;
}

/** What a dictionary's address is filled in with. */
export interface LinkWords {
  word: string;
  /** The code of the language being learned, such as pt. */
  from: string;
  /** The code of the language to translate into. */
  to: string;
  /** The English name of the language being learned, as Wiktionary heads its sections. */
  language: string;
}

export const DEFAULT_DICTIONARIES: DictionaryLink[] = [
  { name: 'Wiktionary', url: 'https://en.wiktionary.org/wiki/{word}#{language}' },
  { name: 'Google Translate', url: 'https://translate.google.com/?sl={from}&tl={to}&text={word}&op=translate' },
  { name: 'Glosbe', url: 'https://glosbe.com/{from}/{to}/{word}' },
  { name: 'Forvo', url: 'https://forvo.com/word/{word}/#{from}' },
];

const MAX_NAME = 40;
const MAX_DICTIONARIES = 20;

/** Why [link] can't be a dictionary, or null when it can. */
export function dictionaryProblem(link: DictionaryLink, others: DictionaryLink[] = []): string | null {
  const name = link.name.trim();
  if (!name) return 'Give it a name.';
  if (name.length > MAX_NAME) return 'Give it a shorter name.';
  if (others.some((other) => other.name.trim().toLowerCase() === name.toLowerCase())) return 'There is one with that name already.';
  if (!/^https?:\/\/[^\s/]+/i.test(link.url.trim())) return 'The address must start with https://';
  if (!link.url.includes('{word}')) return 'Write {word} where the word goes.';
  if (others.length >= MAX_DICTIONARIES) return `Keep ${MAX_DICTIONARIES} at most.`;
  return null;
}

/** The dictionaries of saved settings that can be used, in their order. */
export function validDictionaries(saved: unknown): DictionaryLink[] {
  if (!Array.isArray(saved)) return DEFAULT_DICTIONARIES;
  const kept: DictionaryLink[] = [];
  for (const item of saved) {
    if (typeof item !== 'object' || !item) continue;
    const { name, url } = item as Partial<DictionaryLink>;
    if (typeof name !== 'string' || typeof url !== 'string') continue;
    const link = { name: name.trim(), url: url.trim() };
    if (!dictionaryProblem(link, kept)) kept.push(link);
  }
  return kept;
}

/** [link]'s address for [words], each part encoded for an address. */
export function dictionaryUrl(link: DictionaryLink, words: LinkWords): string {
  const values: Record<string, string> = {
    word: words.word.replace(/​/g, ''),
    from: words.from,
    to: words.to,
    language: words.language.replace(/ /g, '_'),
  };
  return link.url.replace(/\{(word|from|to|language)\}/g, (_, key: string) => encodeURIComponent(values[key]!));
}

/** The dictionary called [name], or the first when there is none by that name. */
export function dictionaryNamed(links: DictionaryLink[], name: string | null): DictionaryLink | null {
  return links.find((link) => link.name === name) ?? links[0] ?? null;
}
