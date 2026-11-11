/** What the Tayra desktop app answers, as its bridge sends it (core/domain bridge/BridgeMessages.kt). */

export interface BridgeLanguage {
  id: number;
  name: string;
  code: string | null;
}

export interface BridgeStatus {
  language: BridgeLanguage | null;
  native: string;
  version: string | null;
}

export interface BridgeSegment {
  text: string;
  /** For a word or saved phrase: the text to look it up and save it by. */
  term?: string | null;
  /** What its status is kept under. */
  lc?: string | null;
  status?: number | null;
  /** Japanese and Chinese words in pieces, each with its reading above (empty for kana). */
  ruby?: { text: string; reading: string }[] | null;
  /** Japanese words in romaji. */
  romaji?: string | null;
}

export interface BridgeLine {
  beginMs: number;
  endMs: number;
  text: string;
  translation?: string | null;
  segments: BridgeSegment[];
}

export interface TranscriptRequest {
  videoId: string;
  trackLanguage: string;
  automatic: boolean;
  original: string;
  translation?: string | null;
}

export interface TranscriptResponse {
  languageMatches: boolean;
  language: BridgeLanguage | null;
  lines: BridgeLine[];
  /** Each word's place in the language's frequency list, by its lc; older apps send none. */
  ranks?: Record<string, number>;
}

export interface BridgeEntry {
  word: string;
  pos: string;
  ipa?: string | null;
  reading?: string | null;
  senses: string[][];
  dictionary?: string | null;
}

export interface LookupResponse {
  term: string;
  lc: string;
  status: number;
  translation: string;
  parents: string[];
  entries: BridgeEntry[];
}

export interface SaveTermRequest {
  term: string;
  status: number;
  translation?: string | null;
  sentence?: string | null;
}

export interface SaveTermResponse {
  lc: string;
  status: number;
}

export interface ChangesResponse {
  seq: number;
  statuses: Record<string, number>;
  reset: boolean;
}

/** Why a call didn't get an answer: the app isn't open, this browser isn't paired, or the app refused. */
export type BridgeFailure = 'offline' | 'unpaired' | 'failed';

export type BridgeResult<T> = { ok: true; data: T } | { ok: false; error: BridgeFailure; message?: string };

export interface AddVideoRequest {
  videoId: string;
  title: string;
  channel: string | null;
  durationMs: number;
  trackLanguage: string;
  automatic: boolean;
  original: string;
  translation: string | null;
}

export interface AddVideoResponse {
  bookId: number;
  added: boolean;
}

export interface BookmarkRequest {
  bookId: number;
  atMs: number;
}

export interface BookmarkResponse {
  id: number;
  title: string;
  added: boolean;
}

export interface OpenVideoRequest {
  bookId: number;
  positionMs: number;
}
