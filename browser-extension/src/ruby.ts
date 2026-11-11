import type { BridgeSegment } from './bridge-types';

/** What shows above Japanese and Chinese words: their readings (furigana, pinyin), romaji, or nothing. */
export type ReadingMode = 'readings' | 'romaji' | 'off';

/** A piece of a word as shown, with the text above it, if any. */
export interface ShownPiece {
  text: string;
  above: string | null;
}

/** [segment]'s text in pieces with what [mode] puts above them; the text as it is when there is nothing to show. */
export function shownPieces(segment: BridgeSegment, mode: ReadingMode): ShownPiece[] {
  if (mode === 'romaji' && segment.romaji) return [{ text: segment.text, above: segment.romaji }];
  const ruby = segment.ruby;
  if (mode !== 'readings' || !ruby?.some((piece) => piece.reading)) return [{ text: segment.text, above: null }];
  // The pieces spell the word as looked up; when it is written otherwise here, the readings go over all of it.
  if (ruby.map((piece) => piece.text).join('') !== segment.text) return [{ text: segment.text, above: ruby.map((piece) => piece.reading || piece.text).join('') }];
  return ruby.map((piece) => ({ text: piece.text, above: piece.reading || null }));
}

/** Fills [element] with [segment]'s text, its readings above it as [mode] says. */
export function fillWord(element: HTMLElement, segment: BridgeSegment, mode: ReadingMode) {
  const pieces = shownPieces(segment, mode);
  if (pieces.length === 1 && pieces[0]!.above === null) {
    element.textContent = segment.text;
    return;
  }
  element.replaceChildren();
  for (const piece of pieces) {
    if (piece.above === null) {
      element.append(piece.text);
      continue;
    }
    const ruby = document.createElement('ruby');
    const rt = document.createElement('rt');
    rt.textContent = piece.above;
    ruby.append(piece.text, rt);
    element.append(ruby);
  }
}
