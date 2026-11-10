import type { BridgeResult, BridgeStatus, ChangesResponse, LookupResponse, SaveTermRequest, SaveTermResponse, TranscriptRequest, TranscriptResponse } from './bridge-types';

/** What the extension's pages and content scripts ask its background, which alone talks to the app. */
export type RuntimeRequest =
  | { type: 'status' }
  | { type: 'pair'; code: string }
  | { type: 'unpair' }
  | { type: 'transcript'; request: TranscriptRequest }
  | { type: 'lookup'; term: string }
  | { type: 'save'; request: SaveTermRequest }
  | { type: 'changes'; since: number };

export interface RuntimeAnswers {
  status: BridgeResult<BridgeStatus>;
  pair: BridgeResult<null>;
  unpair: BridgeResult<null>;
  transcript: BridgeResult<TranscriptResponse>;
  lookup: BridgeResult<LookupResponse>;
  save: BridgeResult<SaveTermResponse>;
  changes: BridgeResult<ChangesResponse>;
}

export function ask<R extends RuntimeRequest>(request: R): Promise<RuntimeAnswers[R['type']]> {
  return browser.runtime.sendMessage(request) as Promise<RuntimeAnswers[R['type']]>;
}
