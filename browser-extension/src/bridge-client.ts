import type { BridgeResult } from './bridge-types';

const readJson = <T>(response: Response): Promise<T> => response.json().catch(() => null) as Promise<T>;

/** The ports the desktop app listens on, in the order it tries them (DesktopBrowserBridge.DEFAULT_PORTS). */
export const PORTS = [47831, 47832, 47833, 47834, 47835];

export interface TokenStore {
  get(): Promise<string | null>;
  set(token: string | null): Promise<void>;
}

/**
 * Talks to the Tayra desktop app on this computer: finds the port it listens on, and sends the
 * pairing token with every call. Forgets the token when the app no longer knows it.
 */
export class BridgeClient {
  private port: number | null = null;

  constructor(
    private readonly tokens: TokenStore,
    private readonly fetchImpl: typeof fetch = (input, init) => fetch(input, init),
    private readonly timeoutMs = 4000,
    /** The token the app's helper gives, with no pairing code; null where the browser can't start it. */
    private readonly automatic: () => Promise<string | null> = async () => null,
  ) {}

  async call<T>(method: 'GET' | 'POST', path: string, body?: unknown, authorized = true, read: (response: Response) => Promise<T> = readJson): Promise<BridgeResult<T>> {
    const first = await this.callOnce<T>(method, path, body, authorized, read);
    if (first.ok || first.error !== 'unpaired') return first;
    // No token yet, or one the app no longer knows: the helper may give one.
    const token = await this.automatic().catch(() => null);
    if (!token) return first;
    await this.tokens.set(token);
    return this.callOnce<T>(method, path, body, authorized, read);
  }

  /** What the app sends at [path] as bytes, or null when it sends nothing (204). */
  bytes(path: string): Promise<BridgeResult<ArrayBuffer | null>> {
    return this.call('GET', path, undefined, true, async (response) => (response.status === 204 ? null : response.arrayBuffer()));
  }

  private async callOnce<T>(method: 'GET' | 'POST', path: string, body: unknown, authorized: boolean, read: (response: Response) => Promise<T>): Promise<BridgeResult<T>> {
    const token = authorized ? await this.tokens.get() : null;
    if (authorized && !token) return (await this.find()) ? { ok: false, error: 'unpaired' } : { ok: false, error: 'offline' };
    for (let attempt = 0; attempt < 2; attempt++) {
      const port = this.port ?? (await this.find());
      if (port === null) return { ok: false, error: 'offline' };
      try {
        const response = await this.send(port, method, path, body, token);
        if (response.status === 401) {
          await this.tokens.set(null);
          return { ok: false, error: 'unpaired' };
        }
        if (!response.ok) {
          const failure = (await response.json().catch(() => null)) as { error?: string } | null;
          return { ok: false, error: 'failed', message: failure?.error ?? `HTTP ${response.status}` };
        }
        return { ok: true, data: await read(response) };
      } catch {
        // The app quit, or moved to another port when it started again.
        this.port = null;
      }
    }
    return { ok: false, error: 'offline' };
  }

  async pair(code: string, browserName: string): Promise<BridgeResult<null>> {
    const answer = await this.call<{ token: string }>('POST', '/v1/pair', { code: code.replace(/\s+/g, ''), browser: browserName }, false);
    if (!answer.ok) return answer;
    await this.tokens.set(answer.data.token);
    return { ok: true, data: null };
  }

  /** The port Tayra answers on, or null when the app isn't open. */
  async find(): Promise<number | null> {
    for (const port of PORTS) {
      try {
        const response = await this.send(port, 'GET', '/v1/hello', undefined, null);
        const hello = (await response.json()) as { app?: string };
        if (response.ok && hello.app === 'tayra') {
          this.port = port;
          return port;
        }
      } catch {
        // Nothing there, or something else.
      }
    }
    this.port = null;
    return null;
  }

  private async send(port: number, method: string, path: string, body: unknown, token: string | null): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const headers: Record<string, string> = {};
      if (body !== undefined) headers['Content-Type'] = 'application/json';
      if (token) headers.Authorization = `Bearer ${token}`;
      return await this.fetchImpl(`http://127.0.0.1:${port}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: controller.signal,
      });
    } finally {
      clearTimeout(timer);
    }
  }
}
