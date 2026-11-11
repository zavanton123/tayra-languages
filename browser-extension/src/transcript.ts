import type { BridgeLine } from './bridge-types';
import { currentStatus } from './lines';
import type { SavedWord } from './saved';
import { clock } from './side-panel';

/** How a word of the printed transcript is marked: saved from this video, being learned, new, or not at all. */
export type PrintMark = 'saved' | 'learning' | 'new' | null;

export interface PrintPiece {
  text: string;
  mark: PrintMark;
}

export interface PrintLine {
  time: string;
  /** Where the line starts, in whole seconds, to link to it. */
  seconds: number;
  pieces: PrintPiece[];
  translation: string | null;
  /** The translation a person made, when the video has one. */
  human: string | null;
}

/** A video's transcript as the extension prints it. */
export interface PrintTranscript {
  videoId: string;
  title: string;
  channel: string;
  /** The language learned, when the app cut the lines into words. */
  language: string | null;
  lines: PrintLine[];
  saved: { term: string; time: string; seconds: number; line: string }[];
  /** When it was made, in epoch ms. */
  madeAt: number;
}

export interface PrintInput {
  videoId: string;
  title: string;
  channel: string;
  language: string | null;
  lines: BridgeLine[];
  human: (string | null)[];
  changed: ReadonlyMap<string, number>;
  saved: SavedWord[];
  madeAt: number;
}

export function printable(input: PrintInput): PrintTranscript {
  const savedLcs = new Set(input.saved.map((w) => w.lc));
  const lines = input.lines.map((line, index): PrintLine => ({
    time: clock(line.beginMs),
    seconds: Math.floor(line.beginMs / 1000),
    pieces: line.segments.length ? line.segments.map((segment) => ({ text: segment.text, mark: markOf(segment.lc, currentStatus(segment, input.changed), savedLcs) })) : [{ text: line.text, mark: null }],
    translation: line.translation ?? null,
    human: input.human[index] ?? null,
  }));
  const saved = [...input.saved]
    .sort((a, b) => a.beginMs - b.beginMs)
    .map((w) => ({ term: w.term.replace(/​/g, ''), time: clock(w.beginMs), seconds: Math.floor(w.beginMs / 1000), line: w.line }));
  return { videoId: input.videoId, title: input.title, channel: input.channel, language: input.language, lines, saved, madeAt: input.madeAt };
}

function markOf(lc: string | null | undefined, status: number | null, saved: ReadonlySet<string>): PrintMark {
  if (!lc) return null;
  if (saved.has(lc)) return 'saved';
  if (status !== null && status >= 1 && status <= 4) return 'learning';
  if (status === 0) return 'new';
  return null;
}
