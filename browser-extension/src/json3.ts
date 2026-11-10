/** One subtitle line: its text and when it shows, in milliseconds from the video's start. */
export interface Cue {
  beginMs: number;
  endMs: number;
  text: string;
}

interface Json3Event {
  tStartMs?: number;
  dDurationMs?: number;
  aAppend?: number;
  segs?: { utf8?: string }[];
}

/** The lines of a YouTube subtitle track in its json3 form, in order, empty ones left out. */
export function parseJson3(body: string): Cue[] {
  const root = JSON.parse(body) as { events?: Json3Event[] };
  const cues: Cue[] = [];
  for (const event of root.events ?? []) {
    if (!event.segs || event.aAppend) continue;
    const text = event.segs.map((s) => s.utf8 ?? '').join('').replace(/\s+/g, ' ').trim();
    if (!text) continue;
    const beginMs = event.tStartMs ?? 0;
    cues.push({ beginMs, endMs: beginMs + (event.dDurationMs ?? 0), text });
  }
  return cues.sort((a, b) => a.beginMs - b.beginMs);
}

/** The line showing at [ms], or null between lines: the latest one begun that hasn't ended. */
export function cueAt<T extends { beginMs: number; endMs: number }>(cues: T[], ms: number): T | null {
  let low = 0;
  let high = cues.length - 1;
  let found = -1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (cues[mid]!.beginMs <= ms) {
      found = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  const cue = cues[found];
  return cue && ms < cue.endMs ? cue : null;
}
