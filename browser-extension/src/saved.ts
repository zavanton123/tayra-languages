/** A word saved to Tayra from a video's word card, with the line it was met in. */
export interface SavedWord {
  term: string;
  lc: string;
  line: string;
  beginMs: number;
  savedAt: number;
}

/** How many videos keep their saved words; the oldest go first. */
export const SAVED_VIDEOS = 200;

/** [list] with [word] first, in place of the same word saved before. */
export function withSaved(list: SavedWord[], word: SavedWord): SavedWord[] {
  return [word, ...list.filter((w) => w.lc !== word.lc)];
}

const key = (videoId: string) => `saved:${videoId}`;

export async function loadSaved(videoId: string): Promise<SavedWord[]> {
  const stored = await browser.storage.local.get(key(videoId));
  const list = stored[key(videoId)];
  return Array.isArray(list) ? (list as SavedWord[]) : [];
}

export async function storeSaved(videoId: string, list: SavedWord[]): Promise<void> {
  await browser.storage.local.set({ [key(videoId)]: list });
  const order = ((await browser.storage.local.get('savedVideos')).savedVideos as string[] | undefined) ?? [];
  const videos = [videoId, ...order.filter((v) => v !== videoId)];
  const dropped = videos.splice(SAVED_VIDEOS);
  await browser.storage.local.set({ savedVideos: videos });
  if (dropped.length) await browser.storage.local.remove(dropped.map(key));
}
