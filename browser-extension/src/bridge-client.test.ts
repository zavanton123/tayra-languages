import { describe, expect, it } from 'vitest';
import { BridgeClient, PORTS, type TokenStore } from './bridge-client';

class MemoryTokens implements TokenStore {
  constructor(public token: string | null = null) {}
  async get() {
    return this.token;
  }
  async set(token: string | null) {
    this.token = token;
  }
}

/** A fake app on [port]: answers hello, and status for token "t". */
function app(port: number | null, calls: string[] = []) {
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input));
    calls.push(`${init?.method} ${url.port}${url.pathname}`);
    if (Number(url.port) !== port) throw new TypeError('connection refused');
    const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });
    if (url.pathname === '/v1/hello') return json(200, { app: 'idiomiko', api: 1 });
    const auth = (init?.headers as Record<string, string>).Authorization;
    if (auth !== 'Bearer t') return json(401, { error: 'Only the Idiomiko extension may call the app' });
    if (url.pathname === '/v1/speech') return url.searchParams.get('text') === 'oi' ? new Response(new Uint8Array([1, 2, 3]), { status: 200 }) : new Response(null, { status: 204 });
    return json(200, { language: null, native: 'en', version: null });
  }) as typeof fetch;
}

describe('BridgeClient', () => {
  it('finds the app on any of its ports', async () => {
    const client = new BridgeClient(new MemoryTokens(), app(PORTS[2]!));
    expect(await client.find()).toBe(PORTS[2]);
  });

  it('says the app is offline when nothing answers', async () => {
    const client = new BridgeClient(new MemoryTokens('t'), app(null));
    expect(await client.call('GET', '/v1/status')).toEqual({ ok: false, error: 'offline' });
  });

  it('gets its token from the app\'s helper', async () => {
    const tokens = new MemoryTokens();
    let asked = 0;
    const client = new BridgeClient(tokens, app(PORTS[0]!), 4000, async () => {
      asked++;
      return 't';
    });
    expect(await client.call('GET', '/v1/status')).toEqual({ ok: true, data: { language: null, native: 'en', version: null } });
    expect(tokens.token).toBe('t');
    expect((await client.call('GET', '/v1/status')).ok).toBe(true);
    expect(asked).toBe(1);
  });

  it('is unconnected when the app is open but the helper gives no token', async () => {
    const client = new BridgeClient(new MemoryTokens(), app(PORTS[0]!), 4000, async () => null);
    expect(await client.call('GET', '/v1/status')).toEqual({ ok: false, error: 'unconnected' });
  });

  it('is unconnected when the helper can\'t be started', async () => {
    const client = new BridgeClient(new MemoryTokens(), app(PORTS[0]!), 4000, async () => {
      throw new Error('Specified native messaging host not found.');
    });
    expect(await client.call('GET', '/v1/status')).toEqual({ ok: false, error: 'unconnected' });
  });

  it('is offline, not unconnected, when the app isn\'t open', async () => {
    let asked = 0;
    const client = new BridgeClient(new MemoryTokens(), app(null), 4000, async () => {
      asked++;
      return 't';
    });
    expect(await client.call('GET', '/v1/status')).toEqual({ ok: false, error: 'offline' });
    expect(asked).toBe(0);
  });

  it('forgets a token the app no longer knows, and asks the helper again', async () => {
    const tokens = new MemoryTokens('old');
    expect(await new BridgeClient(tokens, app(PORTS[0]!)).call('GET', '/v1/status')).toEqual({ ok: false, error: 'unconnected' });
    expect(tokens.token).toBeNull();
    tokens.token = 'old';
    const client = new BridgeClient(tokens, app(PORTS[0]!), 4000, async () => 't');
    expect((await client.call('GET', '/v1/status')).ok).toBe(true);
    expect(tokens.token).toBe('t');
  });

  it('finds the app again after it moved to another port', async () => {
    let port = PORTS[0]!;
    const calls: string[] = [];
    const moving = ((input: RequestInfo | URL, init?: RequestInit) => app(port, calls)(input, init)) as typeof fetch;
    const client = new BridgeClient(new MemoryTokens('t'), moving);
    expect((await client.call('GET', '/v1/status')).ok).toBe(true);
    port = PORTS[1]!;
    expect((await client.call('GET', '/v1/status')).ok).toBe(true);
  });

  it('gets audio as bytes, and nothing when the app leaves it to the browser', async () => {
    const client = new BridgeClient(new MemoryTokens('t'), app(PORTS[0]!));
    const audio = await client.bytes('/v1/speech?text=oi');
    expect(audio.ok && new Uint8Array(audio.data!)).toEqual(new Uint8Array([1, 2, 3]));
    expect(await client.bytes('/v1/speech?text=tchau')).toEqual({ ok: true, data: null });
  });
});
