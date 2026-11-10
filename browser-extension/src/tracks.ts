import type { CaptionTrack } from './messages';

const base = (code: string) => code.split('-')[0]!.toLowerCase();

/**
 * The subtitles to learn from: ones a person made in the language being learned, else YouTube's
 * automatic ones in it, else (when the app doesn't say the language) the first a person made.
 */
export function pickTrack(tracks: CaptionTrack[], learning: string | null): CaptionTrack | null {
  if (learning) {
    const inLanguage = tracks.filter((t) => base(t.languageCode) === base(learning));
    return inLanguage.find((t) => t.kind !== 'asr') ?? inLanguage[0] ?? null;
  }
  return tracks.find((t) => t.kind !== 'asr') ?? tracks[0] ?? null;
}

/** The language to have YouTube translate [track] into, or null when it is already in it. */
export function translationFor(track: CaptionTrack, native: string): string | null {
  return base(track.languageCode) === base(native) ? null : native;
}
