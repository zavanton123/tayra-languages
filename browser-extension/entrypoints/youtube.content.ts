import type { BridgeLine, BridgeStatus } from '../src/bridge-types';
import { cueAt, parseJson3 } from '../src/json3';
import { localLines } from '../src/lines';
import { CHANNEL, isPageMessage, type ExtensionMessage, type TrackResult, type VideoDetails } from '../src/messages';
import { Overlay } from '../src/overlay';
import { ask } from '../src/runtime-messages';
import { pickTrack, translationFor } from '../src/tracks';

/** How often the subtitles ask the app for words whose status changed elsewhere. */
const CHANGES_EVERY_MS = 4000;

/**
 * Beside YouTube's page: gets the video's subtitles through the page, has the Tayra app clean them
 * and cut them into words with their statuses, and shows them over the video. Without the app it
 * shows YouTube's subtitles and their translation as they are.
 */
export default defineContentScript({
  matches: ['https://www.youtube.com/*'],
  runAt: 'document_idle',
  main(ctx) {
    let nextId = 1;
    const waiting = new Map<number, (result: TrackResult) => void>();
    let video: VideoDetails | null = null;
    let lines: BridgeLine[] = [];
    let fromApp = false;
    const changed = new Map<string, number>();
    let version = 0;
    let seq = 0;
    let open: { lc: string; term: string; line: BridgeLine; resume: boolean } | null = null;

    const overlay = new Overlay({
      onWord: (term, lc, line, element) => void lookUp(term, lc, line, element),
      onStatus: (status) => void setStatus(status),
      onCloseCard: () => closeCard(),
    });
    const hideOwn = document.createElement('style');
    hideOwn.textContent = '.ytp-caption-window-container { display: none !important; }';

    function track(baseUrl: string, translateTo: string | null): Promise<TrackResult> {
      const id = nextId++;
      const message: ExtensionMessage = { channel: CHANNEL, type: 'track', request: { id, baseUrl, translateTo } };
      return new Promise((resolve) => {
        waiting.set(id, resolve);
        window.postMessage(message, location.origin);
      });
    }

    async function load(details: VideoDetails) {
      video = details;
      lines = [];
      fromApp = false;
      changed.clear();
      version++;
      closeCard();
      const status = await ask({ type: 'status' }).catch(() => null);
      const app: BridgeStatus | null = status?.ok ? status.data : null;
      const chosen = pickTrack(details.tracks, app?.language?.code ?? null);
      if (!chosen) {
        if (app?.language) overlay.mount() && overlay.say(`No ${app.language.name} subtitles for this video`);
        return;
      }
      const native = app?.native ?? navigator.language.split('-')[0] ?? 'en';
      const translateTo = translationFor(chosen, native);
      const original = await track(chosen.baseUrl, null);
      const translated = translateTo ? await track(chosen.baseUrl, translateTo) : null;
      if (video !== details) return;
      if (!original.body) {
        console.debug('[tayra] no subtitles', original.status);
        return;
      }
      if (app) {
        const answer = await ask({
          type: 'transcript',
          request: { videoId: details.videoId, trackLanguage: chosen.languageCode, automatic: chosen.kind === 'asr', original: original.body, translation: translated?.body ?? null },
        }).catch(() => null);
        if (video !== details) return;
        if (answer?.ok) {
          lines = answer.data.lines;
          fromApp = answer.data.languageMatches;
          const start = await ask({ type: 'changes', since: 0 }).catch(() => null);
          if (start?.ok) seq = start.data.seq;
          return;
        }
      }
      lines = localLines(parseJson3(original.body), translated?.body ? parseJson3(translated.body) : []);
      if (status && !status.ok && overlay.mount()) {
        overlay.say(status.error === 'unpaired' ? 'Tayra: pair this browser in the app’s Settings to look words up' : 'Tayra: open the app to look words up');
      }
    }

    async function lookUp(term: string, lc: string, line: BridgeLine, element: HTMLElement) {
      const player = document.querySelector<HTMLVideoElement>('#movie_player video');
      const resume = open ? open.resume : !!player && !player.paused;
      player?.pause();
      overlay.closeCard();
      element.classList.add('open');
      open = { lc, term, line, resume };
      overlay.showCardLoading(term);
      const answer = await ask({ type: 'lookup', term }).catch(() => null);
      if (open?.term !== term) return;
      if (answer?.ok) overlay.showCard(answer.data);
      else overlay.showCardError(term, answer?.error === 'offline' ? 'The Tayra app is not open.' : 'Tayra could not look the word up.');
    }

    async function setStatus(status: number) {
      const word = open;
      if (!word) return;
      const answer = await ask({ type: 'save', request: { term: word.term, status, sentence: word.line.text } }).catch(() => null);
      if (!answer?.ok) {
        overlay.showCardError(word.term, 'Tayra could not save the word.');
        return;
      }
      changed.set(answer.data.lc, answer.data.status);
      if (answer.data.lc !== word.lc) changed.set(word.lc, answer.data.status);
      version++;
      closeCard();
    }

    function closeCard() {
      const word = open;
      open = null;
      overlay.closeCard();
      if (word?.resume) void document.querySelector<HTMLVideoElement>('#movie_player video')?.play();
    }

    async function pollChanges() {
      if (!fromApp || document.hidden || !video) return;
      const answer = await ask({ type: 'changes', since: seq }).catch(() => null);
      if (!answer?.ok) return;
      if (answer.data.reset) {
        seq = answer.data.seq;
        if (video) void load(video);
        return;
      }
      seq = answer.data.seq;
      const statuses = Object.entries(answer.data.statuses);
      if (!statuses.length) return;
      for (const [lc, status] of statuses) changed.set(lc, status);
      version++;
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
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && open) {
        event.stopPropagation();
        closeCard();
      }
    }, true);
    ctx.setInterval(() => void pollChanges(), CHANGES_EVERY_MS);

    function tick() {
      const player = document.getElementById('movie_player');
      const media = player?.querySelector<HTMLVideoElement>('video');
      const advert = player?.classList.contains('ad-showing') ?? false;
      const here = new URL(location.href).searchParams.get('v');
      if (lines.length && video?.videoId === here && media && !advert && overlay.mount()) {
        if (!hideOwn.isConnected) document.head.appendChild(hideOwn);
        // While a word's card is open, its line stays.
        overlay.show(open ? open.line : cueAt(lines, media.currentTime * 1000), changed, version);
      } else {
        overlay.hide();
        hideOwn.remove();
      }
      if (ctx.isValid) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  },
});
