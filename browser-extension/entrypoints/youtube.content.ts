import { cueAt, parseJson3, type Cue } from '../src/json3';
import { CHANNEL, isPageMessage, type CaptionTrack, type ExtensionMessage, type TrackResult, type VideoDetails } from '../src/messages';

/** Beside YouTube's page: picks the video's subtitle track, fetches it and its translation through the page, and shows them over the video. */
export default defineContentScript({
  matches: ['https://www.youtube.com/*'],
  runAt: 'document_idle',
  main(ctx) {
    const NATIVE = 'en';
    let nextId = 1;
    const waiting = new Map<number, (result: TrackResult) => void>();
    let original: Cue[] = [];
    let translation: Cue[] = [];
    let current = '';

    function ask(baseUrl: string, translateTo: string | null): Promise<TrackResult> {
      const id = nextId++;
      const message: ExtensionMessage = { channel: CHANNEL, type: 'track', request: { id, baseUrl, translateTo } };
      return new Promise((resolve) => {
        waiting.set(id, resolve);
        window.postMessage(message, location.origin);
      });
    }

    // Spike only: what was got, shown on the player.
    const status: string[] = [];
    function report(entry: Record<string, unknown>) {
      console.info('[tayra]', JSON.stringify(entry));
      if (entry.event === 'video') status.length = 0;
      status.push(
        entry.event === 'video'
          ? `tracks: ${(entry.tracks as string[]).join(', ') || 'none'}`
          : `${entry.event}${entry.to ? ` → ${String(entry.to)}` : ''}: HTTP ${String(entry.status)}, ${String(entry.cues)} lines (${String(entry.token)})`,
      );
      if (mount() && diagnostics) diagnostics.textContent = `Tayra test · ${status.join(' · ')}`;
      const all = JSON.parse(document.documentElement.dataset.tayraReport ?? '[]') as unknown[];
      all.push(entry);
      document.documentElement.dataset.tayraReport = JSON.stringify(all);
    }

    function pick(tracks: CaptionTrack[]): CaptionTrack | null {
      return tracks.find((t) => t.kind !== 'asr') ?? tracks[0] ?? null;
    }

    async function load(details: VideoDetails) {
      current = details.videoId;
      original = [];
      translation = [];
      render(null, null);
      const track = pick(details.tracks);
      report({ event: 'video', videoId: details.videoId, title: details.title, tracks: details.tracks.map((t) => `${t.languageCode}${t.kind ? `:${t.kind}` : ''}`), picked: track?.languageCode ?? null });
      if (!track) return;
      const first = await ask(track.baseUrl, null);
      if (current !== details.videoId) return;
      original = first.body ? parseJson3(first.body) : [];
      report({ event: 'track', videoId: details.videoId, status: first.status, token: first.token, bytes: first.body?.length ?? 0, cues: original.length });
      const target = track.languageCode.startsWith(NATIVE) ? 'pt' : NATIVE;
      const second = await ask(track.baseUrl, target);
      if (current !== details.videoId) return;
      translation = second.body ? parseJson3(second.body) : [];
      report({ event: 'translation', videoId: details.videoId, to: target, status: second.status, token: second.token, bytes: second.body?.length ?? 0, cues: translation.length });
    }

    window.addEventListener('message', (event) => {
      if (event.source !== window || !isPageMessage(event.data)) return;
      const message = event.data;
      if (message.type === 'video') void load(message.details);
      else {
        waiting.get(message.result.id)?.(message.result);
        waiting.delete(message.result.id);
      }
    });

    // The overlay: inside the player, so it shows in full screen too; in a shadow root, so YouTube's styles leave it alone.
    let host: HTMLElement | null = null;
    let line: HTMLElement | null = null;
    let translated: HTMLElement | null = null;
    let diagnostics: HTMLElement | null = null;
    const hideOwn = document.createElement('style');
    hideOwn.textContent = '.ytp-caption-window-container { display: none !important; }';

    function mount(): boolean {
      const p = document.getElementById('movie_player');
      if (!p) return false;
      if (host && host.parentElement === p) return true;
      host = document.createElement('div');
      host.id = 'tayra-subtitles';
      const root = host.attachShadow({ mode: 'open' });
      root.innerHTML = `
        <style>
          :host { position: absolute; inset: 0; z-index: 60; pointer-events: none; }
          .box { position: absolute; left: 10%; right: 10%; bottom: 12%; text-align: center; font-family: system-ui, sans-serif; }
          .line, .translation { display: inline-block; padding: 0.15em 0.5em; border-radius: 0.3em; background: rgba(8, 8, 8, 0.78); color: #fff; }
          .line { font-size: clamp(16px, 2.6vw, 34px); }
          .translation { margin-top: 0.3em; font-size: clamp(13px, 1.8vw, 24px); color: #d9e4ff; }
          .line:empty, .translation:empty { display: none; }
          .status { position: absolute; top: 8px; left: 8px; padding: 4px 8px; border-radius: 4px; background: #1d4ed8; color: #fff; font: 12px system-ui, sans-serif; }
          .status:empty { display: none; }
        </style>
        <div class="status"></div>
        <div class="box"><div><span class="line"></span></div><div><span class="translation"></span></div></div>`;
      line = root.querySelector('.line');
      translated = root.querySelector('.translation');
      diagnostics = root.querySelector('.status');
      p.appendChild(host);
      return true;
    }

    function render(text: string | null, translationText: string | null) {
      if (line && line.textContent !== (text ?? '')) line.textContent = text ?? '';
      if (translated && translated.textContent !== (translationText ?? '')) translated.textContent = translationText ?? '';
    }

    function tick() {
      const video = document.querySelector<HTMLVideoElement>('#movie_player video');
      const advert = document.getElementById('movie_player')?.classList.contains('ad-showing') ?? false;
      if (original.length && mount() && video && !advert) {
        if (!hideOwn.isConnected) document.head.appendChild(hideOwn);
        const ms = video.currentTime * 1000;
        render(cueAt(original, ms)?.text ?? null, cueAt(translation, ms)?.text ?? null);
      } else {
        render(null, null);
      }
      if (ctx.isValid) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  },
});
