import { expect, it } from 'vitest';
import { WatchedTime } from './library';

it('counts only the time the video plays, and not a hidden tab’s gaps', () => {
  const watched = new WatchedTime();
  watched.tick(0, true);
  watched.tick(500, true);
  expect(watched.tick(1000, true)).toBe(1000);
  watched.tick(1500, false);
  watched.tick(9000, false);
  watched.tick(9500, true);
  expect(watched.tick(10_000, true)).toBe(1500);
  expect(watched.tick(70_000, true)).toBe(1500);
  watched.reset();
  expect(watched.tick(80_000, true)).toBe(0);
});
