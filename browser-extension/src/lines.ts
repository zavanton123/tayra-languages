import type { BridgeLine, BridgeSegment } from './bridge-types';
import { cueAt, type Cue } from './json3';

/** Lines from YouTube's own tracks, for when the app isn't there to clean and cut them: each with the translated line showing at its middle. */
export function localLines(original: Cue[], translation: Cue[]): BridgeLine[] {
  return original.map((cue) => ({
    beginMs: cue.beginMs,
    endMs: cue.endMs,
    text: cue.text,
    translation: cueAt(translation, (cue.beginMs + cue.endMs) / 2)?.text ?? null,
    segments: [],
  }));
}

/** A segment's status as it is now: changed since the line came, or as it came. */
export function currentStatus(segment: BridgeSegment, changed: ReadonlyMap<string, number>): number | null {
  if (!segment.lc) return null;
  return changed.get(segment.lc) ?? segment.status ?? null;
}
