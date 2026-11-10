import { describe, expect, it } from 'vitest';
import { LEAD_MS, LinePlayer, lineIndexAt, type Media } from './line-player';

class FakeMedia implements Media {
  currentTime = 0;
  paused = true;
  play() {
    this.paused = false;
  }
  pause() {
    this.paused = true;
  }
}

const lines = [
  { beginMs: 1000, endMs: 3000 },
  { beginMs: 4000, endMs: 6000 },
  { beginMs: 6000, endMs: 9000 },
];

function player() {
  const media = new FakeMedia();
  const lp = new LinePlayer(() => media);
  lp.lines = lines;
  return { media, lp };
}

describe('lineIndexAt', () => {
  it('gives the line begun last, shown or just over', () => {
    expect([0, 1000, 3500, 6000, 99999].map((ms) => lineIndexAt(lines, ms))).toEqual([-1, 0, 0, 2, 2]);
  });
});

describe('LinePlayer', () => {
  it('jumps to the next line, a little before it begins, and plays', () => {
    const { media, lp } = player();
    media.currentTime = 1.5;
    lp.next();
    expect(media.currentTime).toBeCloseTo((4000 - LEAD_MS) / 1000);
    expect(media.paused).toBe(false);
    lp.next();
    expect(media.currentTime).toBeCloseTo((6000 - LEAD_MS) / 1000);
    lp.next();
    expect(media.currentTime).toBeCloseTo((6000 - LEAD_MS) / 1000);
  });

  it('goes back a line, or restarts the line when well into it', () => {
    const { media, lp } = player();
    media.currentTime = 4.3;
    lp.previous();
    expect(media.currentTime).toBeCloseTo((1000 - LEAD_MS) / 1000);
    media.currentTime = 5.8;
    lp.previous();
    expect(media.currentTime).toBeCloseTo((4000 - LEAD_MS) / 1000);
  });

  it('replays the line playing or just over', () => {
    const { media, lp } = player();
    media.currentTime = 3.6;
    lp.replay();
    expect(media.currentTime).toBeCloseTo((1000 - LEAD_MS) / 1000);
  });

  it('pauses once at the end of each line when auto-pause is on', () => {
    const { media, lp } = player();
    lp.autoPause = true;
    media.play();
    media.currentTime = 2.5;
    lp.tick();
    expect(media.paused).toBe(false);
    media.currentTime = 2.98;
    lp.tick();
    expect(media.paused).toBe(true);
    media.play();
    media.currentTime = 3.2;
    lp.tick();
    expect(media.paused).toBe(false);
    lp.replay();
    media.currentTime = 2.99;
    lp.tick();
    expect(media.paused).toBe(true);
  });

  it('leaves the video alone with auto-pause off', () => {
    const { media, lp } = player();
    media.play();
    media.currentTime = 2.99;
    lp.tick();
    expect(media.paused).toBe(false);
  });
});
