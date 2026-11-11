import type { BridgeLine, LookupResponse } from './bridge-types';
import { DEFAULT_DICTIONARIES, dictionaryProblem } from './dictionaries';
import type { CaptionTrack } from './messages';
import { currentStatus } from './lines';
import { FONT_SCALES, POSITION_RANGE, hidden, type ExtensionSettings, type HideMode } from './settings';
import { clock } from './side-panel';
import { STATUS_BUTTONS, statusClass } from './statuses';
import { listenToWords, type WordGestures } from './word-gestures';
import { SPEAK_ICON } from './word-popup';

const STYLE = `
  :host { position: absolute; inset: 0; z-index: 60; pointer-events: none; font-family: system-ui, -apple-system, 'Segoe UI', sans-serif; --scale: 1; --position: 11%; --background: rgba(8, 8, 8, 0.7); --background-hover: rgba(8, 8, 8, 0.85); }
  :host([data-font='sans']) .text { font-family: Arial, Helvetica, sans-serif; }
  :host([data-font='serif']) .text { font-family: 'Times New Roman', Times, serif; }
  .subtitles { position: absolute; left: 3%; right: 3%; bottom: var(--position); display: flex; align-items: flex-end; justify-content: center; gap: 10px; }
  .text { pointer-events: auto; display: flex; flex-direction: column; align-items: center; gap: 0.3em; max-width: 84%; text-align: center; }
  .line, .translation { max-width: 100%; padding: 0.12em 0.45em; border-radius: 0.3em; background: var(--background); color: #fff; transition: filter 120ms; }
  .text:hover .line, .text:hover .translation { background: var(--background-hover); }
  .line { font-size: calc(clamp(16px, 2.5vw, 34px) * var(--scale)); line-height: 1.35; }
  .translation { font-size: calc(clamp(13px, 1.7vw, 23px) * var(--scale)); color: #dbe6ff; }
  .translation.ht { color: #ffe3b3; }
  .line:empty, .translation:empty { display: none; }
  .hidden { filter: blur(0.25em); cursor: pointer; }
  .hidden:hover { filter: none; }
  .controls { pointer-events: auto; display: flex; flex-direction: column; gap: 6px; transition: opacity 150ms; }
  .controls[hidden] { display: none; }
  .controls button { display: grid; place-items: center; width: 32px; height: 32px; padding: 0; border: 0; border-radius: 8px; background: var(--background); color: #eff1f2; font: 600 13px system-ui, sans-serif; cursor: pointer; }
  .controls button:hover { background: rgba(31, 95, 139, 0.95); }
  .controls button.on { background: #8fc3ec; color: #0b2a40; }
  .controls .handle { cursor: ns-resize; }
  .controls svg { width: 18px; height: 18px; fill: currentColor; }
  .pause-mark { position: absolute; right: 3%; bottom: calc(var(--position) + 5.5em); padding: 3px 8px; border-radius: 6px; background: #8fc3ec; color: #0b2a40; font: 600 12px system-ui, sans-serif; animation: pulse 1.4s ease-in-out infinite; }
  .pause-mark[hidden] { display: none; }
  @keyframes pulse { 50% { opacity: 0.45; } }
  @media (prefers-reduced-motion: reduce) { .pause-mark { animation: none; } }
  .word { cursor: pointer; border-radius: 0.2em; padding: 0 0.06em; color: #eff1f2; }
  .word:hover { outline: 2px solid rgba(239, 241, 242, 0.7); }
  /* Tayra's dark-theme statuses: they read on a video, and dark-mode extensions leave them be. */
  .word.s0 { background: #34476b; }
  .word.s1 { background: #7c6735; }
  .word.s2 { background: #6e5e37; }
  .word.s3 { background: #605438; }
  .word.s4 { background: #524b3a; }
  .word.open { outline: 2px solid #8fc3ec; }
  .notice { position: absolute; top: 12px; left: 12px; padding: 6px 10px; border-radius: 6px; background: #163e5c; color: #eff1f2; font-size: 13px; }
  .notice:empty { display: none; }
  .panel { pointer-events: auto; position: absolute; z-index: 2; right: 12px; top: 12px; bottom: 60px; width: min(340px, 70%); overflow: auto; padding: 4px 16px 16px;
    background: #1d2125; color: #e6e9ec; border: 1px solid #2d3238; border-radius: 12px; box-shadow: 0 12px 40px rgba(0, 0, 0, 0.55); font-size: 13px; }
  .panel[hidden] { display: none; }
  .panel header { position: sticky; top: 0; display: flex; align-items: center; padding: 10px 0; background: #1d2125; }
  .panel h2 { margin: 0; font-size: 15px; }
  .panel .close { margin-left: auto; border: 0; background: none; color: #a3acb5; font-size: 20px; cursor: pointer; }
  .panel h3 { margin: 14px 0 6px; color: #a3acb5; font-size: 11px; font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase; }
  .panel label { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 5px 0; }
  .panel select, .panel input[type=range] { max-width: 60%; }
  .panel select { padding: 4px 6px; border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: inherit; }
  .panel .keys { display: grid; grid-template-columns: auto 1fr; gap: 3px 10px; color: #a3acb5; }
  .panel kbd { padding: 0 5px; border: 1px solid #3a4148; border-radius: 4px; color: #e6e9ec; font: 12px ui-monospace, Menlo, monospace; }
  .card { pointer-events: auto; position: absolute; z-index: 2; left: 50%; bottom: calc(var(--position) + 7.5em); transform: translateX(-50%); width: min(440px, 86%); max-height: 52%; overflow: auto;
    background: #1d2125; color: #e6e9ec; border: 1px solid #2d3238; border-radius: 12px; box-shadow: 0 12px 40px rgba(0, 0, 0, 0.55); font-size: 14px; line-height: 1.45; text-align: left; }
  .card[hidden] { display: none; }
  .card header { position: sticky; top: 0; display: flex; align-items: baseline; gap: 10px; padding: 12px 14px 8px; background: #1d2125; border-bottom: 1px solid #2d3238; }
  .card h2 { margin: 0; font-size: 20px; font-weight: 650; }
  .card .reading { color: #a3acb5; }
  .card .close { margin-left: auto; border: 0; background: none; color: #a3acb5; font-size: 20px; line-height: 1; cursor: pointer; }
  .card .statuses { display: flex; flex-wrap: wrap; gap: 6px; padding: 10px 14px; }
  .card .statuses button { min-width: 34px; padding: 5px 8px; border: 1px solid #3a4148; border-radius: 8px; background: #23282d; color: #e6e9ec; font: 600 13px system-ui, sans-serif; cursor: pointer; }
  .card .statuses button:hover { border-color: #8fc3ec; }
  .card .statuses button.current { background: #8fc3ec; border-color: #8fc3ec; color: #0b2a40; }
  .card .body { padding: 0 14px 14px; display: grid; gap: 10px; }
  .card .mine { padding: 8px 10px; border-radius: 8px; background: #262c32; }
  .card .entry .head { font-weight: 600; }
  .card .entry .pos { margin-left: 6px; color: #a3acb5; font-style: italic; font-weight: 400; }
  .card .entry ol { margin: 4px 0 0; padding-left: 20px; }
  .card .muted { color: #a3acb5; }
  .card .rank { color: #8a949e; font-size: 13px; font-variant-numeric: tabular-nums; }
  .card .say { width: 28px; height: 28px; padding: 0; border: 0; border-radius: 6px; background: none; color: #a3acb5; cursor: pointer; align-self: center; }
  .card .say:hover { background: #2d3238; color: #e6e9ec; }
  .card .say svg { width: 18px; height: 18px; fill: currentColor; }
  .card h3 { margin: 4px 0 0; color: #a3acb5; font-size: 11px; font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase; }
  .card .example { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 8px; padding: 4px 6px; border-radius: 6px; cursor: pointer; }
  .card .example:hover { background: #262c32; }
  .card .example .at { color: #8a949e; font: 12px ui-monospace, Menlo, monospace; padding-top: 2px; }
  .card .example b { color: #8fc3ec; font-weight: 600; }
  .card .links { display: flex; flex-wrap: wrap; gap: 6px; }
  .card .links a { padding: 3px 9px; border: 1px solid #3a4148; border-radius: 999px; color: #e6e9ec; font-size: 12px; text-decoration: none; }
  .card .links a:hover { border-color: #8fc3ec; }
  .card .links a.last { border-color: #8fc3ec; }
  .panel .dictionary { display: flex; align-items: center; gap: 8px; padding: 3px 0; }
  .panel .dictionary span { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .panel .dictionary small { color: #8a949e; }
  .panel .dictionary button, .panel .add button { border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: inherit; cursor: pointer; }
  .panel .dictionary button { border: 0; background: none; color: #a3acb5; font-size: 16px; }
  .panel .add { display: grid; gap: 6px; margin-top: 6px; }
  .panel .add input { padding: 5px 7px; border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: inherit; }
  .panel .add button, .panel .reset { justify-self: start; padding: 4px 10px; }
  .panel .reset { margin-top: 6px; border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: inherit; cursor: pointer; }
  .panel .note { color: #8a949e; font-size: 12px; }
  .panel .problem { color: #ffb4ab; font-size: 12px; }
  .card .error { color: #ffb4ab; }
`;

/** Space between the card and the subtitles under it, and above it, in px. */
const CARD_GAP = 12;
const CARD_MIN_HEIGHT = 120;

const ICONS = {
  previous: '<svg viewBox="0 0 24 24"><path d="M6 6h2v12H6zm3.5 6 8.5 6V6z"/></svg>',
  replay: '<svg viewBox="0 0 24 24"><path d="M12 5V1L7 6l5 5V7c3.3 0 6 2.7 6 6s-2.7 6-6 6-6-2.7-6-6H4c0 4.4 3.6 8 8 8s8-3.6 8-8-3.6-8-8-8z"/></svg>',
  next: '<svg viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>',
  settings: '<svg viewBox="0 0 24 24"><path d="M19.4 13a7.5 7.5 0 0 0 0-2l2.1-1.6-2-3.5-2.5 1a7.6 7.6 0 0 0-1.7-1L15 3.3h-4l-.4 2.6a7.6 7.6 0 0 0-1.7 1l-2.5-1-2 3.5L6.6 11a7.5 7.5 0 0 0 0 2l-2.1 1.6 2 3.5 2.5-1c.5.4 1.1.7 1.7 1l.4 2.6h4l.4-2.6c.6-.3 1.2-.6 1.7-1l2.5 1 2-3.5zM13 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z" transform="translate(-1 0)"/></svg>',
  move: '<svg viewBox="0 0 24 24"><path d="M12 2 8 6h3v5H6V8l-4 4 4 4v-3h5v5H8l4 4 4-4h-3v-5h5v3l4-4-4-4v3h-5V6h3z"/></svg>',
};

/** What the word card shows besides the dictionary. */
export interface CardExtras {
  rank: number | null;
  /** Other lines of the video that say the word: their index, time and line. */
  examples: { index: number; line: BridgeLine }[];
  /** The card's dictionaries, with the word filled in. */
  links: { name: string; url: string }[];
  lastDictionary: string | null;
}

export interface OverlayEvents extends WordGestures {
  onStatus(status: number): void;
  /** Say the open card's word. */
  onSpeak(): void;
  /** Go to line [index] and play it. */
  onExample(index: number): void;
  /** A dictionary of the card was opened. */
  onDictionaryUsed(name: string): void;
  onOpenInTayra(): void;
  /** Open the transcript to print or save. */
  onPrint(): void;
  onCloseCard(): void;
  onAction(action: 'previous' | 'replay' | 'next' | 'autopause'): void;
  onSettings(change: Partial<ExtensionSettings>): void;
  onTrack(key: string): void;
  /** The pointer came onto the subtitles, or left them. */
  onHover(inside: boolean): void;
}

/** What the settings panel offers for this video. */
export interface PanelChoices {
  tracks: CaptionTrack[];
  track: string | null;
  translationLanguages: { languageCode: string; name: string }[];
  native: string;
}

export function trackKey(track: CaptionTrack): string {
  return `${track.languageCode}|${track.kind}`;
}

/**
 * What the extension draws over YouTube's player: the current line with its words coloured by
 * status and its translations, the buttons beside them, the card of a word clicked and the
 * settings. Inside the player, so it shows in full screen; in a shadow root, so YouTube's styles
 * leave it alone.
 */
export class Overlay {
  private host: HTMLElement | null = null;
  private root!: ShadowRoot;
  private line!: HTMLElement;
  private machine!: HTMLElement;
  private human!: HTMLElement;
  private notice!: HTMLElement;
  private card!: HTMLElement;
  private panel!: HTMLElement;
  private pauseMark!: HTMLElement;
  private shown: BridgeLine | null = null;
  private shownKey = '';
  private noticeTimer: ReturnType<typeof setTimeout> | undefined;
  private settings: ExtensionSettings | null = null;
  private choices: PanelChoices | null = null;

  constructor(private readonly events: OverlayEvents) {}

  /** Puts the overlay into the player, again when YouTube replaced it; false while there is no player. */
  mount(): boolean {
    const player = document.getElementById('movie_player');
    if (!player) return false;
    if (this.host?.parentElement === player) return true;
    this.host?.remove();
    const host = document.createElement('div');
    host.id = 'tayra-subtitles';
    const root = host.attachShadow({ mode: 'open' });
    root.innerHTML = `<style>${STYLE}</style>
      <div class="notice"></div>
      <div class="pause-mark" hidden>Auto-paused</div>
      <div class="card" hidden></div>
      <div class="panel" hidden></div>
      <div class="subtitles">
        <div class="controls left">
          <button data-action="next" title="Next line (D)">${ICONS.next}</button>
          <button data-action="replay" title="Replay the line (S)">${ICONS.replay}</button>
          <button data-action="previous" title="Previous line (A)">${ICONS.previous}</button>
        </div>
        <div class="text"><div class="line"></div><div class="translation mt"></div><div class="translation ht"></div></div>
        <div class="controls right">
          <button data-action="autopause" title="Auto-pause at the end of each line (Q)">AP</button>
          <button data-action="settings" title="Tayra settings">${ICONS.settings}</button>
          <button data-action="move" class="handle" title="Drag to move the subtitles; click to show or hide these buttons">${ICONS.move}</button>
        </div>
      </div>`;
    this.root = root;
    this.line = root.querySelector('.line')!;
    this.machine = root.querySelector('.mt')!;
    this.human = root.querySelector('.ht')!;
    this.notice = root.querySelector('.notice')!;
    this.card = root.querySelector('.card')!;
    this.panel = root.querySelector('.panel')!;
    this.pauseMark = root.querySelector('.pause-mark')!;
    // Clicks here are the overlay's: the player under it would play or pause.
    for (const type of ['click', 'mousedown', 'mouseup', 'dblclick', 'pointerdown', 'pointerup', 'contextmenu', 'wheel']) {
      root.addEventListener(type, (event) => event.stopPropagation());
    }
    // Typing in the settings must not reach YouTube's keys.
    for (const type of ['keydown', 'keyup', 'keypress']) {
      root.addEventListener(type, (event) => {
        if ((event.target as HTMLElement).matches?.('input, select, textarea')) event.stopPropagation();
      });
    }
    listenToWords(this.line, (element) => {
      if (this.line.classList.contains('hidden') || !this.shown || !element.dataset.term || !element.dataset.lc) return null;
      return { term: element.dataset.term, lc: element.dataset.lc, line: this.shown, element };
    }, this.events);
    const text = root.querySelector<HTMLElement>('.text')!;
    text.addEventListener('mouseenter', () => this.events.onHover(true));
    text.addEventListener('mouseleave', () => this.events.onHover(false));
    root.querySelectorAll<HTMLElement>('.controls button').forEach((button) => {
      const action = button.dataset.action!;
      if (action === 'move') return this.draggable(button, player);
      button.addEventListener('click', () => {
        if (action === 'settings') this.togglePanel();
        else this.events.onAction(action as 'previous' | 'replay' | 'next' | 'autopause');
      });
    });
    this.card.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      const status = target.closest<HTMLElement>('[data-status]')?.dataset.status;
      const example = target.closest<HTMLElement>('[data-example]')?.dataset.example;
      const link = target.closest<HTMLElement>('a[data-dictionary]')?.dataset.dictionary;
      if (status !== undefined) this.events.onStatus(Number(status));
      else if (example !== undefined) this.events.onExample(Number(example));
      else if (link) this.events.onDictionaryUsed(link);
      else if (target.closest('.say')) this.events.onSpeak();
      else if (target.closest('.close')) this.events.onCloseCard();
    });
    player.appendChild(host);
    this.host = host;
    this.shown = null;
    this.shownKey = '';
    if (this.settings) this.apply(this.settings);
    return true;
  }

  /** Dragging the handle moves the subtitles up and down; a click without a drag shows or hides the buttons. */
  private draggable(handle: HTMLElement, player: HTMLElement) {
    handle.addEventListener('pointerdown', (down) => {
      handle.setPointerCapture(down.pointerId);
      const box = player.getBoundingClientRect();
      let moved = false;
      let position = this.settings?.position ?? 11;
      const move = (event: PointerEvent) => {
        if (Math.abs(event.clientY - down.clientY) > 3) moved = true;
        if (!moved) return;
        position = Math.round(((box.bottom - event.clientY) / box.height) * 100);
        position = Math.min(POSITION_RANGE.max, Math.max(POSITION_RANGE.min, position));
        this.host?.style.setProperty('--position', `${position}%`);
      };
      const up = () => {
        handle.removeEventListener('pointermove', move);
        handle.removeEventListener('pointerup', up);
        if (moved) this.events.onSettings({ position });
        else this.events.onSettings({ showControls: !(this.settings?.showControls ?? true) });
      };
      handle.addEventListener('pointermove', move);
      handle.addEventListener('pointerup', up);
    });
  }

  apply(settings: ExtensionSettings) {
    this.settings = settings;
    if (!this.host) return;
    const style = this.host.style;
    style.setProperty('--scale', String(FONT_SCALES[settings.fontSize] ?? 1));
    style.setProperty('--position', `${settings.position}%`);
    const alpha = settings.backgroundOpacity / 100;
    style.setProperty('--background', `rgba(8, 8, 8, ${alpha})`);
    style.setProperty('--background-hover', `rgba(8, 8, 8, ${Math.min(1, alpha + 0.15)})`);
    this.host.dataset.font = settings.font;
    this.root.querySelector<HTMLElement>('.controls.left')!.hidden = !settings.showControls;
    this.root.querySelectorAll<HTMLElement>('.controls.right button:not(.handle)').forEach((b) => (b.hidden = !settings.showControls));
    this.root.querySelector('[data-action="autopause"]')!.classList.toggle('on', settings.autoPause);
    this.shownKey = '';
    if (!this.panel.hidden) this.renderPanel();
  }

  /**
   * Shows [line] with its words coloured by [changed] over the statuses they came with, its
   * translations, and as much of it as the hide mode lets show after [revealed] reveals.
   */
  show(line: BridgeLine | null, human: string | null, changed: ReadonlyMap<string, number>, version: number, hideMode: HideMode, revealed: number) {
    if (!this.host) return;
    this.placeCard();
    const key = `${version}|${hideMode}|${revealed}|${human ?? ''}`;
    if (line === this.shown && key === this.shownKey) return;
    this.shown = line;
    this.shownKey = key;
    this.line.replaceChildren();
    if (line) {
      if (line.segments.length) {
        for (const segment of line.segments) {
          if (segment.term && segment.lc) {
            const span = document.createElement('span');
            span.className = `word ${statusClass(currentStatus(segment, changed))}`;
            span.dataset.term = segment.term;
            span.dataset.lc = segment.lc;
            span.dataset.status = String(segment.status ?? 0);
            span.textContent = segment.text;
            this.line.append(span);
          } else {
            this.line.append(segment.text);
          }
        }
      } else {
        this.line.textContent = line.text;
      }
    }
    this.machine.textContent = line?.translation ?? '';
    this.human.textContent = line ? (human ?? '') : '';
    const hide = hidden(hideMode, revealed);
    this.line.classList.toggle('hidden', hide.line);
    this.machine.classList.toggle('hidden', hide.translation);
    this.human.classList.toggle('hidden', hide.translation);
  }

  /** Marks that auto-pause stopped the video, until it plays again. */
  autoPaused(on: boolean) {
    if (this.host) this.pauseMark.hidden = !on;
  }

  /** The line shown, as text. */
  lineText(): string | null {
    return this.shown?.text ?? null;
  }

  say(text: string, forMs = 6000) {
    if (!this.host) return;
    this.notice.textContent = text;
    clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => (this.notice.textContent = ''), forMs);
  }

  setChoices(choices: PanelChoices) {
    this.choices = choices;
    if (this.host && !this.panel.hidden) this.renderPanel();
  }

  togglePanel() {
    if (!this.host) return;
    this.panel.hidden = !this.panel.hidden;
    if (!this.panel.hidden) this.renderPanel();
  }

  private renderPanel() {
    const settings = this.settings;
    if (!settings) return;
    const choices = this.choices;
    const panel = this.panel;
    panel.replaceChildren();
    const header = document.createElement('header');
    header.innerHTML = '<h2>Tayra</h2><button class="close" title="Close">×</button>';
    header.querySelector('.close')!.addEventListener('click', () => (panel.hidden = true));
    panel.append(header);

    const section = (title: string) => {
      const h = document.createElement('h3');
      h.textContent = title;
      panel.append(h);
    };
    const select = (label: string, value: string, options: [string, string][], change: (value: string) => void) => {
      const row = document.createElement('label');
      row.append(label);
      const element = document.createElement('select');
      for (const [v, text] of options) element.append(new Option(text, v, false, v === value));
      element.addEventListener('change', () => change(element.value));
      row.append(element);
      panel.append(row);
    };
    const toggle = (label: string, value: boolean, change: (value: boolean) => void) => {
      const row = document.createElement('label');
      row.append(label);
      const element = document.createElement('input');
      element.type = 'checkbox';
      element.checked = value;
      element.addEventListener('change', () => change(element.checked));
      row.append(element);
      panel.append(row);
    };

    section('Subtitles');
    if (choices?.tracks.length) {
      select('Subtitles', choices.track ?? '', choices.tracks.map((t) => [trackKey(t), `${t.name || t.languageCode}${t.kind === 'asr' ? ' (auto)' : ''}`]), (v) => this.events.onTrack(v));
    }
    const native = choices?.native ?? 'en';
    const languages: [string, string][] = [['', `As in Tayra (${native})`], ...(choices?.translationLanguages ?? []).map((l): [string, string] => [l.languageCode, l.name])];
    select('Translation into', settings.translationLanguage ?? '', languages, (v) => this.events.onSettings({ translationLanguage: v || null }));
    toggle('Machine translation', settings.machineTranslation, (v) => this.events.onSettings({ machineTranslation: v }));
    toggle('Human translation, when there is one', settings.humanTranslation, (v) => this.events.onSettings({ humanTranslation: v }));
    select('Hide until revealed', settings.hideMode, [
      ['off', 'Nothing'],
      ['translation', 'The translation'],
      ['subtitle', 'The subtitle'],
      ['translation-then-subtitle', 'Both: translation first'],
      ['subtitle-then-translation', 'Both: subtitle first'],
    ], (v) => this.events.onSettings({ hideMode: v as HideMode }));

    section('Playback');
    toggle('Pause at the end of each line', settings.autoPause, (v) => this.events.onSettings({ autoPause: v }));
    toggle('Pause at the line’s end while the pointer is on it', settings.hoverHold, (v) => this.events.onSettings({ hoverHold: v }));
    toggle('Arrow keys move between lines', settings.arrowKeys, (v) => this.events.onSettings({ arrowKeys: v }));

    section('Look');
    select('Text size', String(settings.fontSize), [['0', 'Extra small'], ['1', 'Small'], ['2', 'Normal'], ['3', 'Large'], ['4', 'Extra large']], (v) => this.events.onSettings({ fontSize: Number(v) }));
    select('Font', settings.font, [['default', 'Default'], ['sans', 'Arial'], ['serif', 'Times']], (v) => this.events.onSettings({ font: v as ExtensionSettings['font'] }));
    const opacity = document.createElement('label');
    opacity.append('Background');
    const range = document.createElement('input');
    range.type = 'range';
    range.min = '0';
    range.max = '100';
    range.value = String(settings.backgroundOpacity);
    range.addEventListener('change', () => this.events.onSettings({ backgroundOpacity: Number(range.value) }));
    opacity.append(range);
    panel.append(opacity);
    toggle('Buttons beside the subtitles', settings.showControls, (v) => this.events.onSettings({ showControls: v }));
    toggle('Panel of subtitles and words beside the video', settings.sidePanel, (v) => this.events.onSettings({ sidePanel: v }));

    section('Tayra’s Videos');
    toggle('Add the videos I watch for half a minute', settings.addToLibrary, (v) => this.events.onSettings({ addToLibrary: v }));
    const open = document.createElement('button');
    open.className = 'reset';
    open.textContent = 'Open this video in Tayra';
    open.addEventListener('click', () => this.events.onOpenInTayra());
    panel.append(open);
    const print = document.createElement('button');
    print.className = 'reset';
    print.textContent = 'Print or save the transcript';
    print.addEventListener('click', () => this.events.onPrint());
    panel.append(print);

    section('Words');
    toggle('Show a word’s meaning when the pointer rests on it', settings.hoverLookup, (v) => this.events.onSettings({ hoverLookup: v }));
    toggle('Say a word when it is clicked', settings.speakOnClick, (v) => this.events.onSettings({ speakOnClick: v }));
    this.renderDictionaries(panel, settings);

    section('Keys');
    const keys = document.createElement('div');
    keys.className = 'keys';
    const rows: [string, string][] = [
      ['A', 'Previous line (hold to stay)'],
      ['S', 'Replay the line'],
      ['D', 'Next line (hold to stay)'],
      ['W', 'Play or pause'],
      ['Q', 'Auto-pause on or off'],
      ['E', 'Reveal what is hidden'],
      ['⌘/Ctrl C', 'Copy the line'],
      ['R', 'Bookmark the line in Tayra'],
      ['Esc', 'Close the word card'],
      ['Right-click', 'A word’s next status'],
      ['⇧ Right-click', 'Ignore a word'],
      ['⇧ Click', 'Look a word up in the dictionary used last'],
    ];
    for (const [key, what] of rows) {
      const k = document.createElement('kbd');
      k.textContent = key;
      const w = document.createElement('span');
      w.textContent = what;
      keys.append(k, w);
    }
    panel.append(keys);
  }

  /** The card's websites: each can be taken off, others added with {word} where the word goes. */
  private renderDictionaries(panel: HTMLElement, settings: ExtensionSettings) {
    const title = document.createElement('div');
    title.className = 'note';
    title.textContent = 'Dictionaries on the word card';
    panel.append(title);
    for (const link of settings.dictionaries) {
      const row = document.createElement('div');
      row.className = 'dictionary';
      const name = document.createElement('span');
      name.textContent = link.name;
      name.title = link.url;
      const remove = document.createElement('button');
      remove.title = `Take ${link.name} off the card`;
      remove.textContent = '×';
      remove.addEventListener('click', () => this.events.onSettings({ dictionaries: settings.dictionaries.filter((d) => d !== link) }));
      row.append(name, remove);
      panel.append(row);
    }
    const add = document.createElement('form');
    add.className = 'add';
    const name = document.createElement('input');
    name.placeholder = 'Name';
    const url = document.createElement('input');
    url.placeholder = 'https://…{word}…';
    const problem = document.createElement('div');
    problem.className = 'problem';
    const button = document.createElement('button');
    button.textContent = 'Add';
    const note = document.createElement('div');
    note.className = 'note';
    note.textContent = '{word} is the word; {from} and {to} are language codes, such as pt and en; {language} is the language’s English name.';
    add.append(name, url, problem, button, note);
    add.addEventListener('submit', (event) => {
      event.preventDefault();
      const link = { name: name.value.trim(), url: url.value.trim() };
      const wrong = dictionaryProblem(link, settings.dictionaries);
      if (wrong) problem.textContent = wrong;
      else this.events.onSettings({ dictionaries: [...settings.dictionaries, link] });
    });
    panel.append(add);
    const usual = JSON.stringify(settings.dictionaries) === JSON.stringify(DEFAULT_DICTIONARIES);
    if (!usual) {
      const reset = document.createElement('button');
      reset.className = 'reset';
      reset.textContent = 'Bring back the usual dictionaries';
      reset.addEventListener('click', () => this.events.onSettings({ dictionaries: DEFAULT_DICTIONARIES }));
      panel.append(reset);
    }
  }

  cardOpen(): boolean {
    return !!this.host && !this.card.hidden;
  }

  /** Keeps the card just above the subtitles, however tall they are, in the room left above them. */
  private placeCard() {
    if (!this.host || this.card.hidden) return;
    const player = this.host.getBoundingClientRect();
    const subtitles = this.root.querySelector('.text')!.getBoundingClientRect();
    if (!subtitles.height) return;
    this.card.style.bottom = `${player.bottom - subtitles.top + CARD_GAP}px`;
    this.card.style.maxHeight = `${Math.max(CARD_MIN_HEIGHT, subtitles.top - player.top - 2 * CARD_GAP)}px`;
  }

  showCardLoading(term: string) {
    this.card.hidden = false;
    this.card.replaceChildren(header(term, null), paragraph('Looking it up…', 'body muted'));
    this.placeCard();
  }

  showCardError(term: string, text: string) {
    this.card.hidden = false;
    this.card.replaceChildren(header(term, null), paragraph(text, 'body error'));
    this.placeCard();
  }

  showCard(word: LookupResponse, extras: CardExtras) {
    this.card.hidden = false;
    const statuses = document.createElement('div');
    statuses.className = 'statuses';
    for (const button of STATUS_BUTTONS) {
      const element = document.createElement('button');
      element.textContent = button.label;
      element.title = button.title;
      element.dataset.status = String(button.value);
      if (button.value === word.status) element.className = 'current';
      statuses.append(element);
    }
    const body = document.createElement('div');
    body.className = 'body';
    if (word.translation) body.append(paragraph(word.translation, 'mine'));
    if (word.parents.length) body.append(paragraph(`Form of ${word.parents.join(', ')}`, 'muted'));
    for (const entry of word.entries) {
      const block = document.createElement('div');
      block.className = 'entry';
      const head = document.createElement('div');
      head.className = 'head';
      head.textContent = entry.word + (entry.reading ? ` 【${entry.reading}】` : '');
      if (entry.pos) {
        const pos = document.createElement('span');
        pos.className = 'pos';
        pos.textContent = entry.pos;
        head.append(pos);
      }
      const senses = document.createElement('ol');
      for (const glosses of entry.senses) {
        const item = document.createElement('li');
        item.textContent = glosses.join('; ');
        senses.append(item);
      }
      block.append(head, senses);
      if (entry.dictionary) block.append(paragraph(entry.dictionary, 'muted'));
      body.append(block);
    }
    if (!word.entries.length && !word.translation) body.append(paragraph('No dictionary entry. Install a dictionary in Tayra’s Downloads.', 'muted'));
    if (extras.examples.length) {
      body.append(heading('Also in this video'));
      for (const { index, line } of extras.examples) body.append(exampleRow(index, line, word.lc));
    }
    if (extras.links.length) {
      body.append(heading('Look it up'));
      const links = document.createElement('div');
      links.className = 'links';
      for (const link of extras.links) {
        const a = document.createElement('a');
        a.href = link.url;
        a.target = '_blank';
        a.rel = 'noopener noreferrer';
        a.dataset.dictionary = link.name;
        a.textContent = link.name;
        if (link.name === extras.lastDictionary) {
          a.className = 'last';
          a.title = 'Shift+click a word to open this one';
        }
        links.append(a);
      }
      body.append(links);
    }
    const top = header(word.term, word.entries[0]?.ipa ?? null);
    const say = document.createElement('button');
    say.className = 'say';
    say.title = 'Say it';
    say.innerHTML = SPEAK_ICON;
    top.insertBefore(say, top.querySelector('.close'));
    if (extras.rank) {
      const rank = paragraph(`#${extras.rank}`, 'rank');
      rank.title = 'Its place among the language’s most common words';
      top.insertBefore(rank, say);
    }
    this.card.replaceChildren(top, statuses, body);
    this.placeCard();
  }

  closeCard() {
    if (!this.host) return;
    this.card.hidden = true;
    this.line.querySelector('.open')?.classList.remove('open');
  }

  hide() {
    this.show(null, null, new Map(), -1, 'off', 0);
    this.closeCard();
    this.autoPaused(false);
    this.setVisible(false);
  }

  /** Hidden whole, buttons too, while there is nothing to show over the video: during adverts, or away from a video. */
  setVisible(visible: boolean) {
    if (this.host) this.host.style.display = visible ? '' : 'none';
  }
}

function header(term: string, reading: string | null): HTMLElement {
  const element = document.createElement('header');
  const title = document.createElement('h2');
  title.textContent = term.replace(/​/g, '');
  element.append(title);
  if (reading) element.append(paragraph(reading, 'reading'));
  const close = document.createElement('button');
  close.className = 'close';
  close.title = 'Close';
  close.textContent = '×';
  element.append(close);
  return element;
}

function heading(text: string): HTMLElement {
  const element = document.createElement('h3');
  element.textContent = text;
  return element;
}

/** Line [index] of the video, with the word [lc] marked, to play from the card. */
function exampleRow(index: number, line: BridgeLine, lc: string): HTMLElement {
  const row = document.createElement('div');
  row.className = 'example';
  row.dataset.example = String(index);
  row.title = 'Play this line';
  const at = document.createElement('span');
  at.className = 'at';
  at.textContent = clock(line.beginMs);
  const text = document.createElement('span');
  for (const segment of line.segments) {
    if (segment.lc === lc) {
      const b = document.createElement('b');
      b.textContent = segment.text;
      text.append(b);
    } else {
      text.append(segment.text);
    }
  }
  row.append(at, text);
  return row;
}

function paragraph(text: string, className: string): HTMLElement {
  const element = document.createElement('div');
  element.className = className;
  element.textContent = text;
  return element;
}
