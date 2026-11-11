import type { BridgeLine } from './bridge-types';

/** A word on the page: its text to look up, what its status is kept under, the line it is in, and its element. */
export interface WordTarget {
  term: string;
  lc: string;
  line: BridgeLine;
  element: HTMLElement;
}

/** What can be done to a word with the pointer, wherever it is drawn. */
export interface WordGestures {
  /** Click: its card. */
  onWord(word: WordTarget): void;
  /** Shift+click: the dictionary used last. */
  onDictionary(word: WordTarget): void;
  /** Right-click: the next status, or ignored with Shift. */
  onCycle(word: WordTarget, ignore: boolean): void;
  /** The pointer came onto a word, or left it (null). */
  onWordHover(word: WordTarget | null): void;
}

/** Listens for [gestures] on the words (`.word` elements) inside [container]; [resolve] says which word an element is, or null for none. */
export function listenToWords(container: HTMLElement, resolve: (element: HTMLElement) => WordTarget | null, gestures: WordGestures) {
  const find = (event: Event): WordTarget | null => {
    const element = (event.target as HTMLElement | null)?.closest?.<HTMLElement>('.word');
    return element && container.contains(element) ? resolve(element) : null;
  };
  let hovered: HTMLElement | null = null;
  container.addEventListener('click', (event) => {
    const word = find(event);
    if (!word) return;
    event.preventDefault();
    if ((event as MouseEvent).shiftKey) gestures.onDictionary(word);
    else gestures.onWord(word);
  });
  container.addEventListener('contextmenu', (event) => {
    const word = find(event);
    if (!word) return;
    event.preventDefault();
    gestures.onCycle(word, (event as MouseEvent).shiftKey);
  });
  container.addEventListener('mouseover', (event) => {
    const word = find(event);
    if (word?.element === hovered) return;
    hovered = word?.element ?? null;
    gestures.onWordHover(word);
  });
  container.addEventListener('mouseleave', () => {
    if (!hovered) return;
    hovered = null;
    gestures.onWordHover(null);
  });
}
