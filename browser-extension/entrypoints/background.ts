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
  const client = new BridgeClient(tokens);

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
    }
  }

  browser.runtime.onMessage.addListener((message: unknown) => handle(message as RuntimeRequest));
});
