/** How long a video plays before it goes into Tayra's Videos: long enough to be watched, not just looked at. */
export const WATCHED_TO_ADD_MS = 30_000;

/** Pauses longer than this between two looks are not counted: the tab was hidden or the computer slept. */
const MAX_STEP_MS = 1000;

/** How long a video has been playing, counted from the moments it is looked at. */
export class WatchedTime {
  private total = 0;
  private last: number | null = null;

  /** The time played so far, after a look at [now] (ms) while it [playing] or not. */
  tick(now: number, playing: boolean): number {
    if (playing && this.last !== null) {
      const step = now - this.last;
      if (step > 0 && step <= MAX_STEP_MS) this.total += step;
    }
    this.last = playing ? now : null;
    return this.total;
  }

  reset() {
    this.total = 0;
    this.last = null;
  }
}
