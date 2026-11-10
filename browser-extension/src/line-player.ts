/** What the line player needs of YouTube's video element. */
export interface Media {
  currentTime: number;
  paused: boolean;
  play(): unknown;
  pause(): void;
}

export interface TimedLine {
  beginMs: number;
  endMs: number;
}

/** A jump lands this long before the line, so its first sound isn't cut. */
export const LEAD_MS = 125;
/** Auto-pause stops this long before the line ends, before the next one's first sound. */
export const PAUSE_EARLY_MS = 30;

/** The index of the line begun last at [ms] (shown or just over), or -1 before the first. */
export function lineIndexAt(lines: TimedLine[], ms: number): number {
  let low = 0;
  let high = lines.length - 1;
  let found = -1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (lines[mid]!.beginMs <= ms) {
      found = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  return found;
}

/**
 * Moves through a video line by line, as a learner does: back to the previous line, the same
 * line again, on to the next, and, with auto-pause on, a stop at the end of every line.
 */
export class LinePlayer<L extends TimedLine = TimedLine> {
  lines: L[] = [];
  autoPause = false;
  /** The line auto-pause stopped after, so it stops there once, and plays on when asked. */
  private pausedAfter = -1;

  constructor(private readonly media: () => Media | null) {}

  private now(): number {
    return (this.media()?.currentTime ?? 0) * 1000;
  }

  /** The line playing, or the one just over; -1 before the first. Seeks land [LEAD_MS] early, so a line counts from then. */
  current(): number {
    return lineIndexAt(this.lines, this.now() + LEAD_MS);
  }

  /** Called every frame: stops at the end of the line when auto-pause is on. */
  tick() {
    const media = this.media();
    if (!this.autoPause || !media || media.paused) return;
    const ms = media.currentTime * 1000;
    const index = lineIndexAt(this.lines, ms);
    const line = this.lines[index];
    if (!line || index === this.pausedAfter) return;
    if (ms >= line.endMs - PAUSE_EARLY_MS) {
      this.pausedAfter = index;
      media.pause();
    }
  }

  previous() {
    const index = this.current();
    // Well into a line, "back" starts it again, as a player's previous-track button does.
    const line = this.lines[index];
    const target = line && this.now() - line.beginMs > RESTART_WITHIN_MS ? index : index - 1;
    this.jump(Math.max(0, target));
  }

  replay() {
    this.jump(Math.max(0, this.current()));
  }

  next() {
    this.jump(Math.min(this.lines.length - 1, this.current() + 1));
  }

  /** Plays line [index] from its start. */
  jump(index: number) {
    const media = this.media();
    const line = this.lines[index];
    if (!media || !line) return;
    media.currentTime = Math.max(0, line.beginMs - LEAD_MS) / 1000;
    this.pausedAfter = index - 1;
    void media.play();
  }

  /** Plays on after an auto-pause, or pauses. */
  toggle() {
    const media = this.media();
    if (!media) return;
    if (media.paused) void media.play();
    else media.pause();
  }
}

/** How far into a line "previous" restarts it rather than going to the line before. */
export const RESTART_WITHIN_MS = 1500;
