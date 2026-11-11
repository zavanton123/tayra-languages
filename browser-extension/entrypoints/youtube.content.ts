import type { BridgeLine, BridgeStatus } from '../src/bridge-types';
import { cueAt, parseJson3, type Cue } from '../src/json3';
import { localLines } from '../src/lines';
import { LinePlayer, PAUSE_EARLY_MS } from '../src/line-player';
import { CHANNEL, isPageMessage, type CaptionTrack, type ExtensionMessage, type TrackResult, type VideoDetails } from '../src/messages';
import { Overlay, trackKey } from '../src/overlay';
import { ask } from '../src/runtime-messages';
import { loadSaved, storeSaved, withSaved, type SavedWord } from '../src/saved';
import { DEFAULT_SETTINGS, loadSettings, normalize, saveSettings, type ExtensionSettings } from '../src/settings';
import { SidePanel } from '../src/side-panel';
import { KNOWN } from '../src/statuses';
import { pickTrack, translationFor } from '../src/tracks';
import type { VideoWord } from '../src/words';

/** How often the subtitles ask the app for words whose status changed elsewhere. */
const CHANGES_EVERY_MS = 4000;

const RETRY_AFTER_MS = 3000;

/** Settings whose change needs the subtitles fetched again. */
const RELOADING: (keyof ExtensionSettings)[] = ['translationLanguage', 'machineTranslation', 'humanTranslation', 'enabled'];

/**
 * Beside YouTube's page: gets the video's subtitles through the page, has the Tayra app clean them
 * and cut them into words with their statuses, and shows them over the video with the controls a
 * learner needs. Without the app it shows YouTube's subtitles and their translation as they are.
 */
export default defineContentScript({
  matches: ['https://www.youtube.com/*'],
  runAt: 'document_idle',
  async main(ctx) {
    let settings: ExtensionSettings = DEFAULT_SETTINGS;
    let nextId = 1;
    const waiting = new Map<number, (result: TrackResult) => void>();
    let video: VideoDetails | null = null;
    let chosenTrack: string | null = null;
    let lines: BridgeLine[] = [];
    let human: (string | null)[] = [];
    let fromApp = false;
    let native = 'en';
    const changed = new Map<string, number>();
    let version = 0;
    let seq = 0;
    let ranks: Record<string, number> = {};
    let saved: SavedWord[] = [];
    let marking = false;
    let open: { lc: string; term: string; line: BridgeLine; resume: boolean; element: HTMLElement } | null = null;
    let revealed = 0;
    let revealedFor = -1;
    let hovering = false;
    let holdAfter = -1;
    let heldByHover = false;
    let autoPaused = false;
    let peek: { code: string; resume: boolean } | null = null;

    const media = () => document.querySelector<HTMLVideoElement>('#movie_player video');
    const player = new LinePlayer<BridgeLine>(media);

    const overlay = new Overlay({
      onWord: (term, lc, line, element) => void lookUp(term, lc, line, element),
      onStatus: (status) => void setStatus(status),
      onCloseCard: () => closeCard(),
      onAction: (action) => {
        if (action === 'autopause') void change({ autoPause: !settings.autoPause });
        else player[action]();
      },
      onSettings: (patch) => void change(patch),
      onTrack: (key) => {
        chosenTrack = key;
        if (video) void load(video);
      },
      onHover: (inside) => hover(inside),
    });
    const side = new SidePanel({
      onWord: (term, lc, line, element) => void lookUp(term, lc, line, element),
      onSeek: (index) => {
        if (open) closeCard();
        player.jump(index, true);
      },
      onSettings: (patch) => void change(patch),
      onMarkKnown: (words) => void markKnown(words),
      onForget: (lc) => {
        if (!video) return;
        saved = saved.filter((w) => w.lc !== lc);
        side.setSaved(saved);
        void storeSaved(video.videoId, saved);
      },
      onCopy: (text) => void navigator.clipboard.writeText(text).then(() => overlay.say('Line copied', 1500), () => undefined),
    });
    const hideOwn = document.createElement('style');
    hideOwn.textContent = '.ytp-caption-window-container { display: none !important; }';

    async function change(patch: Partial<ExtensionSettings>) {
      const next = normalize({ ...settings, ...patch });
      await saveSettings(next);
      if (patch.autoPause !== undefined) overlay.say(next.autoPause ? 'Auto-pause on' : 'Auto-pause off', 1500);
    }

    function applySettings(next: ExtensionSettings) {
      const reload = RELOADING.some((key) => next[key] !== settings[key]);
      settings = next;
      player.autoPause = next.autoPause;
      overlay.apply(next);
      side.apply(next);
      if (reload && video) void load(video);
    }

    function track(baseUrl: string, translateTo: string | null): Promise<TrackResult> {
      const id = nextId++;
      const message: ExtensionMessage = { channel: CHANNEL, type: 'track', request: { id, baseUrl, translateTo } };
      return new Promise((resolve) => {
        waiting.set(id, resolve);
        window.postMessage(message, location.origin);
      });
    }

    /** A track a person made in [language], other than the one learned from. */
    function humanTrack(tracks: CaptionTrack[], language: string, besides: CaptionTrack): CaptionTrack | null {
      const base = (code: string) => code.split('-')[0]!.toLowerCase();
      return tracks.find((t) => t !== besides && t.kind !== 'asr' && base(t.languageCode) === base(language)) ?? null;
    }

    async function load(details: VideoDetails) {
      if (video?.videoId !== details.videoId) chosenTrack = null;
      video = details;
      lines = [];
      human = [];
      player.lines = [];
      fromApp = false;
      ranks = {};
      saved = [];
      changed.clear();
      version++;
      closeCard();
      side.setVideo([], {}, false, []);
      if (!settings.enabled) return;
      const status = await ask({ type: 'status' }).catch(() => null);
      const app: BridgeStatus | null = status?.ok ? status.data : null;
      native = app?.native ?? navigator.language.split('-')[0] ?? 'en';
      const chosen = details.tracks.find((t) => trackKey(t) === chosenTrack) ?? pickTrack(details.tracks, app?.language?.code ?? null);
      overlay.mount();
      overlay.setChoices({ tracks: details.tracks, track: chosen ? trackKey(chosen) : null, translationLanguages: details.translationLanguages, native });
      if (!chosen) {
        if (app?.language) overlay.say(`No ${app.language.name} subtitles for this video. Pick other subtitles in the ⚙ settings.`);
        return;
      }
      const language = settings.translationLanguage ?? native;
      const translateTo = settings.machineTranslation ? translationFor(chosen, language) : null;
      let original = await track(chosen.baseUrl, null);
      // YouTube sometimes sends the player's first request nothing; asked again a moment later, it sends the lines.
      if (!original.body) {
        await new Promise((resolve) => setTimeout(resolve, RETRY_AFTER_MS));
        if (video !== details) return;
        original = await track(chosen.baseUrl, null);
      }
      const translated = translateTo ? await track(chosen.baseUrl, translateTo) : null;
      const humanSource = settings.humanTranslation ? humanTrack(details.tracks, language, chosen) : null;
      const humanBody = humanSource ? (await track(humanSource.baseUrl, null)).body : null;
      if (video !== details) return;
      if (!original.body) {
        overlay.say('YouTube did not give the subtitles. Reload the page to try again.');
        return;
      }
      let got: BridgeLine[] | null = null;
      if (app) {
        const answer = await ask({
          type: 'transcript',
          request: { videoId: details.videoId, trackLanguage: chosen.languageCode, automatic: chosen.kind === 'asr', original: original.body, translation: translated?.body ?? null },
        }).catch(() => null);
        if (video !== details) return;
        if (answer?.ok) {
          got = answer.data.lines;
          fromApp = answer.data.languageMatches;
          ranks = answer.data.ranks ?? {};
          const start = await ask({ type: 'changes', since: 0 }).catch(() => null);
          if (start?.ok) seq = start.data.seq;
        }
      }
      lines = got ?? localLines(parseJson3(original.body), translated?.body ? parseJson3(translated.body) : []);
      const humanCues: Cue[] = humanBody ? parseJson3(humanBody) : [];
      human = lines.map((line) => cueAt(humanCues, (line.beginMs + line.endMs) / 2)?.text ?? null);
      player.lines = lines;
      saved = await loadSaved(details.videoId).catch(() => []);
      if (video !== details) return;
      side.setVideo(lines, ranks, fromApp, saved);
      if (!got && status && !status.ok) {
        overlay.say(status.error === 'unpaired' ? 'Tayra: pair this browser in the app’s Settings to look words up' : 'Tayra: open the app to look words up');
      }
    }

    async function lookUp(term: string, lc: string, line: BridgeLine, element: HTMLElement) {
      const playing = media();
      const resume = open ? open.resume : heldByHover || (!!playing && !playing.paused);
      playing?.pause();
      closeCard(false);
      element.classList.add('open');
      open = { lc, term, line, resume, element };
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
      if (status >= 1 && status <= 4 && video) {
        saved = withSaved(saved, { term: word.term, lc: word.lc, line: word.line.text, beginMs: word.line.beginMs, savedAt: Date.now() });
        side.setSaved(saved);
        void storeSaved(video.videoId, saved);
      }
      closeCard();
    }

    /** Saves [words] as known, one after another, saying how far it got. */
    async function markKnown(words: VideoWord[]) {
      if (marking) return;
      marking = true;
      const forVideo = video;
      try {
        for (const [i, word] of words.entries()) {
          if (video !== forVideo) return;
          side.setProgress(`Marking ${i + 1} of ${words.length} as known…`);
          const answer = await ask({ type: 'save', request: { term: word.term, status: KNOWN, sentence: lines[word.line]?.text ?? null } }).catch(() => null);
          if (!answer?.ok) {
            side.setProgress(`Tayra could not save every word: ${i} of ${words.length} were marked.`);
            return;
          }
          changed.set(answer.data.lc, answer.data.status);
          changed.set(word.lc, answer.data.status);
          version++;
        }
        side.setProgress(null);
      } finally {
        marking = false;
      }
    }

    /** Closes the word card, and plays on as it was before unless [resume] is false. */
    function closeCard(resume = true) {
      const word = open;
      open = null;
      overlay.closeCard();
      word?.element.classList.remove('open');
      if (resume && word?.resume && !hovering) {
        heldByHover = false;
        void media()?.play();
      }
    }

    /** With the pointer on the subtitles, the video stops at the end of the line, and plays on when it leaves. */
    function hover(inside: boolean) {
      hovering = inside;
      const playing = media();
      if (inside) {
        holdAfter = settings.hoverHold && playing && !playing.paused ? player.current() : -1;
        return;
      }
      holdAfter = -1;
      if (heldByHover && !open) {
        heldByHover = false;
        void playing?.play();
      }
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

    function typing(event: KeyboardEvent): boolean {
      const target = event.composedPath()[0];
      if (!(target instanceof HTMLElement)) return false;
      return target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName);
    }

    function active(): boolean {
      return settings.enabled && lines.length > 0 && new URL(location.href).pathname === '/watch';
    }

    function onKeyDown(event: KeyboardEvent) {
      if (!active() || typing(event)) return;
      if (event.key === 'Escape' && open) return done(event, closeCard);
      const copy = event.code === 'KeyC' && (event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey;
      if (copy) {
        if (window.getSelection()?.toString()) return;
        const text = overlay.lineText();
        if (text) return done(event, () => void navigator.clipboard.writeText(text).then(() => overlay.say('Line copied', 1500), () => undefined));
        return;
      }
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      const arrows = settings.arrowKeys;
      const code = event.code;
      const is = (letter: string, arrow: string) => code === letter || (arrows && code === arrow);
      if (is('KeyA', 'ArrowLeft') || is('KeyD', 'ArrowRight')) {
        return done(event, () => {
          if (event.repeat) return;
          const playing = media();
          peek = { code, resume: !!playing && !playing.paused };
          if (is('KeyA', 'ArrowLeft')) player.previous(false);
          else player.next(false);
        });
      }
      if (is('KeyS', 'ArrowDown')) return done(event, () => !event.repeat && player.replay());
      if (is('KeyW', 'ArrowUp')) return done(event, () => !event.repeat && player.toggle());
      if (code === 'KeyQ') return done(event, () => !event.repeat && void change({ autoPause: !settings.autoPause }));
      if (code === 'KeyE') return done(event, () => !event.repeat && revealed++);
    }

    function onKeyUp(event: KeyboardEvent) {
      if (peek && event.code === peek.code) {
        const resume = peek.resume;
        peek = null;
        event.preventDefault();
        event.stopPropagation();
        // Held while the key is down; it plays on from the line once let go, as it did before.
        if (resume) void media()?.play();
      }
    }

    function done(event: KeyboardEvent, action: () => unknown) {
      event.preventDefault();
      event.stopPropagation();
      action();
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
    document.addEventListener('keydown', onKeyDown, true);
    document.addEventListener('keyup', onKeyUp, true);
    ctx.setInterval(() => void pollChanges(), CHANGES_EVERY_MS);
    browser.storage.onChanged.addListener((changes, area) => {
      if (area === 'local' && changes.settings) applySettings(normalize(changes.settings.newValue));
    });
    applySettings(await loadSettings());

    function tick() {
      const box = document.getElementById('movie_player');
      const playing = media();
      const advert = box?.classList.contains('ad-showing') ?? false;
      const here = new URL(location.href).searchParams.get('v');
      const watching = settings.enabled && lines.length > 0 && video?.videoId === here;
      if (watching && settings.sidePanel && side.mount()) {
        side.show(true);
        side.fit();
        side.refresh(changed, version);
        if (playing) side.setCurrent(open ? lines.indexOf(open.line) : player.current());
      } else {
        side.show(false);
      }
      if (settings.enabled && lines.length && video?.videoId === here && playing && !advert && overlay.mount()) {
        if (!hideOwn.isConnected) document.head.appendChild(hideOwn);
        const wasPlaying = !playing.paused;
        player.tick();
        if (wasPlaying && playing.paused && !peek) autoPaused = true;
        const holdLine = lines[holdAfter];
        if (holdLine && !playing.paused && playing.currentTime * 1000 >= holdLine.endMs - PAUSE_EARLY_MS) {
          playing.pause();
          heldByHover = true;
          holdAfter = -1;
        }
        if (!playing.paused) {
          autoPaused = false;
          heldByHover = false;
          if (hovering && holdAfter < 0 && settings.hoverHold) holdAfter = player.current();
        }
        overlay.autoPaused(autoPaused && playing.paused);
        // While a word's card is open, its line stays.
        const index = open ? lines.indexOf(open.line) : player.current();
        const line = open ? open.line : cueAt(lines, playing.currentTime * 1000) ?? (playing.paused ? (lines[index] ?? null) : null);
        if (index !== revealedFor) {
          revealedFor = index;
          revealed = 0;
        }
        overlay.show(line, line ? (human[lines.indexOf(line)] ?? null) : null, changed, version, settings.hideMode, revealed);
      } else {
        overlay.hide();
        hideOwn.remove();
      }
      if (ctx.isValid) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  },
});
