import { expect, it } from 'vitest';
import { fromBase64, toBase64 } from './base64';

it('carries audio through base64 unchanged, however long', () => {
  const bytes = new Uint8Array(100_000).map((_, i) => (i * 7) % 256);
  expect(fromBase64(toBase64(bytes.buffer))).toEqual(bytes);
  expect(toBase64(new Uint8Array([82, 73, 70, 70]).buffer)).toBe('UklGRg==');
});
