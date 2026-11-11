import { toBase64 } from '../src/base64';
import { BridgeClient } from '../src/bridge-client';
import type { BridgeResult } from '../src/bridge-types';
import type { RuntimeRequest } from '../src/runtime-messages';

/** The only part of the extension that talks to the Tayra app; the YouTube tab and the popup ask it. */
export default defineBackground(() => {
  const tokens = {
    async get() {
      const saved = await browser.storage.local.get('token');
      return typeof saved.token === 'string' ? saved.token : null;
    },
    async set(token: string | null) {
      if (token) await browser.storage.local.set({ token });
      else await browser.storage.local.remove('token');
    },
  };
  /** The helper the Tayra app registers with the browser; the browser lets only this extension start it. */
  async function helperToken(): Promise<string | null> {
    try {
      const answer = (await browser.runtime.sendNativeMessage('app.tayra.bridge', { type: 'token' })) as { token?: unknown } | undefined;
      return typeof answer?.token === 'string' && answer.token.length > 0 ? answer.token : null;
    } catch {
      // No helper: Tayra isn't installed here, or the browser can't start helpers (a sandboxed one).
      return null;
    }
  }
  const client = new BridgeClient(tokens, undefined, undefined, helperToken);

  function browserName(): string {
    const agent = navigator.userAgent;
    if (agent.includes('Firefox/')) return 'Firefox';
    if (agent.includes('Edg/')) return 'Edge';
    return 'Chrome';
  }

  async function handle(request: RuntimeRequest): Promise<BridgeResult<unknown>> {
    switch (request.type) {
      case 'status':
        return client.call('GET', '/v1/status');
      case 'pair':
        return client.pair(request.code, browserName());
      case 'unpair':
        await tokens.set(null);
        return { ok: true, data: null };
      case 'transcript':
        return client.call('POST', '/v1/transcript', request.request);
      case 'lookup':
        return client.call('GET', `/v1/lookup?term=${encodeURIComponent(request.term)}`);
      case 'save':
        return client.call('POST', '/v1/terms', request.request);
      case 'changes':
        return client.call('GET', `/v1/changes?since=${request.since}`);
      case 'addVideo':
        return client.call('POST', '/v1/videos', request.request);
      case 'bookmark':
        return client.call('POST', '/v1/bookmarks', request.request);
      case 'open':
        return client.call('POST', '/v1/open', request.request);
      case 'printTranscript': {
        // The page takes it from session storage, which only the extension's own pages can read.
        const id = crypto.randomUUID();
        await browser.storage.session.set({ [`print:${id}`]: request.transcript });
        await browser.tabs.create({ url: `${browser.runtime.getURL('/transcript.html')}?id=${id}` });
        return { ok: true, data: null };
      }
      case 'speech': {
        const answer = await client.bytes(`/v1/speech?text=${encodeURIComponent(request.text)}`);
        return answer.ok ? { ok: true, data: { audio: answer.data ? toBase64(answer.data) : null } } : answer;
      }
    }
  }

  browser.runtime.onMessage.addListener((message: unknown) => handle(message as RuntimeRequest));
});
