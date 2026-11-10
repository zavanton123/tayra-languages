import { CHANNEL, isExtensionMessage, type CaptionTrack, type PageMessage, type TrackRequest, type TrackResult, type VideoDetails } from '../src/messages';

/**
 * Runs in YouTube's own page, where the player is. It tells the extension which video plays and its
 * subtitle tracks, and fetches a track when asked. YouTube answers a track's address with an empty
 * page unless it carries the player's token, so the token is kept from the player's own requests.
 */
export default defineContentScript({
  matches: ['https://www.youtube.com/*'],
  world: 'MAIN',
  runAt: 'document_start',
  main() {
    interface Player extends HTMLElement {
      getPlayerResponse?: () => PlayerResponse | null;
      loadModule?: (name: string) => void;
      getOption?: (module: string, option: string) => unknown;
      setOption?: (module: string, option: string, value: unknown) => void;
    }
    interface PlayerResponse {
      videoDetails?: { videoId?: string; title?: string; author?: string; lengthSeconds?: string };
      captions?: { playerCaptionsTracklistRenderer?: { captionTracks?: RawTrack[]; translationLanguages?: RawLanguage[] } };
    }
    interface RawLanguage {
      languageCode: string;
      languageName?: { simpleText?: string; runs?: { text: string }[] };
    }
    interface RawTrack {
      baseUrl: string;
      languageCode: string;
      kind?: string;
      name?: { simpleText?: string; runs?: { text: string }[] };
    }

    /** The player's own latest subtitle request per video: its token and the parameters YouTube wants with it. */
    const requests = new Map<string, URL>();
    /** What the player itself was sent for each track, by [trackKey]. */
    const bodies = new Map<string, string>();
    const fetch0 = window.fetch.bind(window);

    function post(message: PageMessage extends infer M ? (M extends PageMessage ? Omit<M, 'channel'> : never) : never) {
      window.postMessage({ channel: CHANNEL, ...message }, location.origin);
    }

    function trackKey(address: URL): string {
      const p = address.searchParams;
      return [p.get('v'), p.get('lang'), p.get('kind') ?? '', p.get('tlang') ?? ''].join('|');
    }

    function parse(url: string): URL | null {
      try {
        return new URL(url, location.href);
      } catch {
        return null;
      }
    }

    // Kept only per video it names: an advert before the video asks for its own.
    function seen(url: string, body: string | null) {
      const address = parse(url);
      const video = address?.searchParams.get('v');
      if (!address || !video) return;
      if (address.searchParams.get('pot')) requests.set(video, address);
      if (body) bodies.set(trackKey(address), body);
    }

    const open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (this: XMLHttpRequest, method: string, url: string | URL, ...rest: unknown[]) {
      const address = String(url);
      if (address.includes('/api/timedtext')) {
        seen(address, null);
        this.addEventListener('load', () => {
          if (this.status === 200 && (this.responseType === '' || this.responseType === 'text')) seen(address, this.responseText);
        });
      }
      return (open as (...args: unknown[]) => void).call(this, method, url, ...rest);
    } as typeof XMLHttpRequest.prototype.open;
    window.fetch = function (input: RequestInfo | URL, init?: RequestInit) {
      const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
      const answer = fetch0(input, init);
      if (url.includes('/api/timedtext')) {
        seen(url, null);
        void answer.then((response) => (response.ok ? response.clone().text() : null)).then((body) => seen(url, body), () => undefined);
      }
      return answer;
    };
    try {
      new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) if (entry.name.includes('/api/timedtext')) seen(entry.name, null);
      }).observe({ type: 'resource', buffered: true });
    } catch {
      // No observer: the hooks above still see the player's requests.
    }

    const player = () => document.getElementById('movie_player') as Player | null;
    const ANNOUNCE_EVERY_MS = 500;
    /** Ten minutes of looking, enough for the longest run of adverts. */
    const ANNOUNCE_TRIES = 1200;
    const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

    function details(): VideoDetails | null {
      const response = player()?.getPlayerResponse?.();
      const video = response?.videoDetails;
      if (!video?.videoId) return null;
      const tracks: CaptionTrack[] = (response?.captions?.playerCaptionsTracklistRenderer?.captionTracks ?? []).map((t) => ({
        languageCode: t.languageCode,
        kind: t.kind ?? '',
        name: t.name?.simpleText ?? (t.name?.runs ?? []).map((r) => r.text).join(''),
        baseUrl: t.baseUrl,
      }));
      const translationLanguages = (response?.captions?.playerCaptionsTracklistRenderer?.translationLanguages ?? []).map((l) => ({
        languageCode: l.languageCode,
        name: l.languageName?.simpleText ?? (l.languageName?.runs ?? []).map((r) => r.text).join('') ?? l.languageCode,
      }));
      return { videoId: video.videoId, title: video.title ?? '', author: video.author ?? '', lengthSeconds: Number(video.lengthSeconds ?? 0), tracks, translationLanguages };
    }

    let announced = '';
    let announcing = 0;
    /**
     * Tells the extension which video plays, once the player has its details. An advert first can
     * keep them back for minutes, so it keeps looking until the page moves to another video.
     */
    async function announce() {
      const round = ++announcing;
      const wanted = new URL(location.href).searchParams.get('v');
      for (let i = 0; i < ANNOUNCE_TRIES && round === announcing; i++) {
        if (new URL(location.href).searchParams.get('v') !== wanted) return;
        const found = details();
        if (found && found.videoId === wanted) {
          if (found.videoId !== announced) {
            announced = found.videoId;
            post({ type: 'video', details: found });
          }
          return;
        }
        await sleep(ANNOUNCE_EVERY_MS);
      }
    }

    interface PlayerTrack {
      languageCode: string;
      kind?: string;
      translationLanguage?: { languageCode: string };
    }

    /**
     * Has the player show [wanted], so it asks for that track itself with everything YouTube wants
     * with the request, then puts the viewer's own choice back. Returns once the player was sent it.
     */
    async function viaPlayer(wanted: URL): Promise<string | null> {
      const p = player();
      const key = trackKey(wanted);
      if (bodies.has(key) || !p?.setOption) return bodies.get(key) ?? null;
      const lang = wanted.searchParams.get('lang') ?? '';
      const kind = wanted.searchParams.get('kind') ?? '';
      const tlang = wanted.searchParams.get('tlang');
      let before: unknown = null;
      try {
        p.loadModule?.('captions');
        before = p.getOption?.('captions', 'track') ?? null;
      } catch {
        // The module isn't there yet.
      }
      for (let i = 0; i < 40 && !bodies.has(key); i++) {
        try {
          // Off between tries: the player asks again only when the track changes.
          if (i % 8 === 0) p.setOption('captions', 'track', {});
          if (i % 8 === 1) {
            const list = (p.getOption?.('captions', 'tracklist') ?? []) as PlayerTrack[];
            const own = list.find((t) => t.languageCode === lang && (t.kind ?? '') === kind) ?? { languageCode: lang, kind };
            p.setOption('captions', 'track', tlang ? { ...own, translationLanguage: { languageCode: tlang } } : own);
          }
        } catch {
          // Tried again below.
        }
        await sleep(250);
      }
      try {
        p.setOption('captions', 'track', before && Object.keys(before).length ? before : {});
      } catch {
        // Left as it is.
      }
      return bodies.get(key) ?? null;
    }

    /** The track as the player's own last request had it, with this track's language and translation instead. */
    function likeThePlayer(wanted: URL): URL {
      const own = requests.get(wanted.searchParams.get('v') ?? '');
      if (!own) return wanted;
      const address = new URL(own.href);
      for (const name of ['lang', 'kind', 'name', 'tlang']) {
        const value = wanted.searchParams.get(name);
        if (value) address.searchParams.set(name, value);
        else address.searchParams.delete(name);
      }
      return address;
    }

    async function fetchTrack(request: TrackRequest): Promise<TrackResult> {
      const wanted = new URL(request.baseUrl, location.href);
      wanted.searchParams.set('fmt', 'json3');
      wanted.searchParams.delete('tlang');
      if (request.translateTo) wanted.searchParams.set('tlang', request.translateTo);
      const shown = await viaPlayer(wanted);
      if (shown) return { id: request.id, status: 200, body: shown, token: 'player' };
      const address = likeThePlayer(wanted);
      const token: TrackResult['token'] = address.searchParams.get('pot') ? (address === wanted ? 'url' : 'player') : 'none';
      try {
        const response = await fetch0(address.href, { credentials: 'include' });
        const body = response.ok ? await response.text() : null;
        return { id: request.id, status: response.status, body: body || null, token };
      } catch {
        return { id: request.id, status: 0, body: null, token };
      }
    }

    window.addEventListener('message', (event) => {
      if (event.source !== window || !isExtensionMessage(event.data)) return;
      void fetchTrack(event.data.request).then((result) => post({ type: 'track-result', result }));
    });
    document.addEventListener('yt-navigate-finish', () => void announce());
    document.addEventListener('yt-player-updated', () => void announce());
  },
});
