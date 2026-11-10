/** Messages between the script in YouTube's own page and the extension's script beside it, sent with window.postMessage. */

export const CHANNEL = 'tayra-youtube';

export interface CaptionTrack {
  languageCode: string;
  /** "asr" for subtitles YouTube made by speech recognition, "" for ones a person wrote. */
  kind: string;
  name: string;
  baseUrl: string;
}

export interface VideoDetails {
  videoId: string;
  title: string;
  author: string;
  lengthSeconds: number;
  tracks: CaptionTrack[];
}

export interface TrackRequest {
  id: number;
  baseUrl: string;
  /** The language YouTube should translate the track into, or null for the track as it is. */
  translateTo: string | null;
}

export interface TrackResult {
  id: number;
  status: number;
  body: string | null;
  /** How the track was got: shown by the player itself, or fetched with a token from the track's address, the player's request, or none. */
  token: 'url' | 'player' | 'none';
}

export type PageMessage =
  | { channel: typeof CHANNEL; type: 'video'; details: VideoDetails }
  | { channel: typeof CHANNEL; type: 'track-result'; result: TrackResult };

export type ExtensionMessage = { channel: typeof CHANNEL; type: 'track'; request: TrackRequest };

export function isPageMessage(data: unknown): data is PageMessage {
  return typeof data === 'object' && data !== null && (data as { channel?: unknown }).channel === CHANNEL && (data as { type?: unknown }).type !== 'track';
}

export function isExtensionMessage(data: unknown): data is ExtensionMessage {
  return typeof data === 'object' && data !== null && (data as { channel?: unknown }).channel === CHANNEL && (data as { type?: unknown }).type === 'track';
}
