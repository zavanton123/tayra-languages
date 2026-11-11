import type { BridgeLine } from './bridge-types';
import { currentStatus } from './lines';
import { lineIndexAt } from './line-player';
import type { SavedWord } from './saved';
import type { ExtensionSettings, PanelTab } from './settings';
import { statusClass } from './statuses';
import { fillWord } from './ruby';
import { listenToWords, type WordGestures } from './word-gestures';
import { SPEAK_ICON } from './word-popup';
import { WORD_GROUPS, groupCounts, groupOf, knownShare, ordered, suggested, videoWords, type VideoWord, type WordGroup } from './words';

const STYLE = `
  :host { display: block; margin: 0 0 12px; font-family: Roboto, system-ui, -apple-system, 'Segoe UI', sans-serif; }
  .panel { display: flex; flex-direction: column; height: var(--height, 480px); overflow: hidden; background: #1d2125; color: #e6e9ec; border: 1px solid #2d3238; border-radius: 12px; font-size: 14px; line-height: 1.4; }
  .panel.folded { height: auto; }
  .panel.folded > :not(header) { display: none; }
  header { display: flex; align-items: center; gap: 2px; padding: 6px 6px 0; border-bottom: 1px solid #2d3238; }
  header .tab { padding: 8px 12px; border: 0; border-bottom: 2px solid transparent; background: none; color: #a3acb5; font-family: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
  header .tab:hover { color: #e6e9ec; }
  header .tab.on { color: #e6e9ec; border-bottom-color: #8fc3ec; }
  header .brand { padding: 0 8px; color: #8fc3ec; font-weight: 700; font-size: 13px; }
  header .open { margin-left: auto; padding: 4px 9px; border: 1px solid #3a4148; border-radius: 999px; background: none; color: #e6e9ec; font-family: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
  header .open:hover { border-color: #8fc3ec; }
  header .open[hidden] { display: none; }
  header .open:not([hidden]) + .fold { margin-left: 0; }
  header .fold { margin-left: auto; width: 30px; height: 30px; border: 0; border-radius: 8px; background: none; color: #a3acb5; font-size: 16px; cursor: pointer; }
  header .fold:hover { background: #262c32; color: #e6e9ec; }
  .toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px; padding: 8px 10px; border-bottom: 1px solid #2d3238; color: #a3acb5; font-size: 12px; }
  .toolbar:empty { display: none; }
  .toolbar label { display: flex; align-items: center; gap: 6px; cursor: pointer; }
  .toolbar select { padding: 3px 6px; border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: inherit; }
  .toolbar .push { margin-left: auto; }
  button.pill { padding: 4px 10px; border: 1px solid #3a4148; border-radius: 999px; background: #23282d; color: #e6e9ec; font-family: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
  button.pill:hover { border-color: #8fc3ec; }
  button.pill.on { background: #8fc3ec; border-color: #8fc3ec; color: #0b2a40; }
  button.pill[hidden] { display: none; }
  .body { position: relative; flex: 1; overflow: auto; overscroll-behavior: contain; }
  .row { display: grid; grid-template-columns: auto minmax(0, 1fr) 28px; gap: 6px; padding: 6px 8px 6px 6px; border-left: 3px solid transparent; cursor: pointer; }
  .row:hover { background: #23282d; }
  .row.current { background: #22303c; border-left-color: #8fc3ec; }
  .time { align-self: start; padding: 2px 0 0; border: 0; background: none; min-width: 38px; color: #8a949e; font: 12px ui-monospace, Menlo, monospace; text-align: left; cursor: pointer; }
  .row.current .time { color: #8fc3ec; }
  .tr { display: none; margin-top: 2px; color: #a3acb5; font-size: 13px; }
  .row:hover .tr, .translations .tr { display: block; }
  .tr:empty { display: none !important; }
  .actions { display: flex; flex-direction: column; gap: 2px; align-self: start; visibility: hidden; }
  .row:hover .actions { visibility: visible; }
  .actions button { width: 26px; height: 26px; padding: 0; border: 0; border-radius: 6px; background: none; color: #a3acb5; cursor: pointer; }
  .actions button:hover { background: #2d3238; color: #e6e9ec; }
  .actions svg { width: 15px; height: 15px; fill: currentColor; }
  .word { border-radius: 0.2em; padding: 0 0.06em; cursor: pointer; }
  .word:hover { outline: 2px solid rgba(239, 241, 242, 0.6); }
  .word.s0 { background: #34476b; }
  .word.s1 { background: #7c6735; }
  .word.s2 { background: #6e5e37; }
  .word.s3 { background: #605438; }
  .word.s4 { background: #524b3a; }
  .word.open { outline: 2px solid #8fc3ec; }
  rt { font-size: 0.55em; color: #a3acb5; }
  .section { padding: 10px 12px 4px; color: #a3acb5; font-size: 11px; font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase; }
  .share { padding: 10px 12px 4px; }
  .share .bar { height: 6px; margin-top: 6px; border-radius: 3px; background: #34476b; overflow: hidden; }
  .share .bar div { height: 100%; background: #8fc3ec; }
  .item { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 2px 10px; padding: 6px 12px; }
  .item:hover { background: #23282d; }
  .item .meta { color: #8a949e; font-size: 12px; font-variant-numeric: tabular-nums; }
  .item .example { grid-column: 1 / -1; overflow: hidden; color: #a3acb5; font-size: 12px; white-space: nowrap; text-overflow: ellipsis; cursor: pointer; }
  .item .example:hover { color: #e6e9ec; }
  .item .forget { border: 0; background: none; color: #8a949e; font-size: 16px; line-height: 1; cursor: pointer; }
  .item .forget:hover { color: #e6e9ec; }
  .empty { padding: 24px 16px; color: #a3acb5; text-align: center; }
  .footer { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 8px 10px; border-top: 1px solid #2d3238; color: #a3acb5; font-size: 12px; }
  .footer:empty { display: none; }
`;

const COPY_ICON = '<svg viewBox="0 0 24 24"><path d="M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z"/></svg>';

const GROUP_NAMES: Record<WordGroup, string> = { new: 'New', learning: 'Learning', known: 'Known', ignored: 'Ignored' };

/** How many words the Words tab suggests learning first. */
const SUGGESTED = 8;
/** The tallest the panel gets when it sits under the video rather than beside it. */
const UNDER_MAX_HEIGHT = 640;
const MIN_HEIGHT = 360;

export interface SidePanelEvents extends WordGestures {
  /** Go to line [index] of the video and play it. */
  onSeek(index: number): void;
  onSettings(change: Partial<ExtensionSettings>): void;
  onMarkKnown(words: VideoWord[]): void;
  /** Take a word off the video's saved list; Tayra keeps it. */
  onForget(lc: string): void;
  onCopy(text: string): void;
  /** Say line [text]. */
  onSpeak(text: string): void;
  onOpenInTayra(): void;
  /** Open the transcript to print or save. */
  onPrint(): void;
}

/**
 * The panel YouTube's right column gets above its suggestions, as Language Reactor has: the
 * video's lines to read along and jump to, its words with how well they are known, and the words
 * saved from it.
 */
export class SidePanel {
  private host: HTMLElement | null = null;
  private root!: ShadowRoot;
  private panel!: HTMLElement;
  private toolbar!: HTMLElement;
  private body!: HTMLElement;
  private footer!: HTMLElement;
  private settings: ExtensionSettings | null = null;
  private lines: BridgeLine[] = [];
  private ranks: Record<string, number> = {};
  private words = true;
  private saved: SavedWord[] = [];
  private changed: ReadonlyMap<string, number> = new Map();
  private version = -1;
  private current = -1;
  private following = true;
  /** The pointer is on the lines: they hold still under it, and catch up with the video when it leaves. */
  private pointerInside = false;
  private autoScrollAt = 0;
  private confirming = false;
  private progress: string | null = null;
  private rendered = '';

  constructor(private readonly events: SidePanelEvents) {}

  /** Puts the panel at the top of YouTube's right column, again when YouTube replaced it; false while there is none. */
  mount(): boolean {
    const column = document.querySelector<HTMLElement>('ytd-watch-flexy:not([hidden]) #secondary-inner') ?? document.querySelector<HTMLElement>('#secondary-inner');
    if (!column) return false;
    if (this.host?.parentElement === column) return true;
    this.host?.remove();
    const host = document.createElement('div');
    host.id = 'tayra-side-panel';
    const root = host.attachShadow({ mode: 'open' });
    root.innerHTML = `<style>${STYLE}</style>
      <div class="panel">
        <header>
          <span class="brand">Tayra</span>
          <button class="tab" data-tab="subtitles">Subtitles</button>
          <button class="tab" data-tab="words">Words</button>
          <button class="tab" data-tab="saved">Saved</button>
          <button class="open" title="Open this video in the Tayra app, where you are in it" hidden>Open in Tayra</button>
          <button class="fold" title="Fold the panel"></button>
        </header>
        <div class="toolbar"></div>
        <div class="body"></div>
        <div class="footer"></div>
      </div>`;
    this.root = root;
    this.panel = root.querySelector('.panel')!;
    this.toolbar = root.querySelector('.toolbar')!;
    this.body = root.querySelector('.body')!;
    this.footer = root.querySelector('.footer')!;
    root.querySelector('header')!.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      const tab = target.closest<HTMLElement>('[data-tab]')?.dataset.tab as PanelTab | undefined;
      if (tab) this.events.onSettings({ panelTab: tab, panelOpen: true });
      else if (target.closest('.open')) this.events.onOpenInTayra();
      else if (target.closest('.fold')) this.events.onSettings({ panelOpen: !(this.settings?.panelOpen ?? true) });
    });
    listenToWords(this.body, (element) => {
      const index = Number(element.dataset.line ?? element.closest<HTMLElement>('.row')?.dataset.index ?? -1);
      const line = this.lines[index];
      if (!line || !element.dataset.term || !element.dataset.lc) return null;
      return { term: element.dataset.term, lc: element.dataset.lc, line, element };
    }, this.events);
    this.body.addEventListener('click', (event) => this.clicked(event));
    this.body.addEventListener('mouseenter', () => (this.pointerInside = true));
    this.body.addEventListener('mouseleave', () => {
      this.pointerInside = false;
      const row = this.body.querySelector<HTMLElement>('.row.current');
      if (row && this.following && this.settings?.panelTab === 'subtitles') this.scrollTo(row);
    });
    this.body.addEventListener('scroll', () => {
      if (this.settings?.panelTab !== 'subtitles' || performance.now() - this.autoScrollAt < 200 || !this.following) return;
      this.following = false;
      this.renderToolbar();
    });
    column.prepend(host);
    this.host = host;
    this.rendered = '';
    this.render();
    return true;
  }

  apply(settings: ExtensionSettings) {
    this.settings = settings;
    this.render();
  }

  /** A new video's lines; [words] is false when the app did not cut them into words. */
  setVideo(lines: BridgeLine[], ranks: Record<string, number>, words: boolean, saved: SavedWord[]) {
    this.lines = lines;
    this.ranks = ranks;
    this.words = words;
    this.saved = saved;
    this.current = -1;
    this.following = true;
    this.confirming = false;
    this.progress = null;
    this.rendered = '';
    this.render();
  }

  setSaved(saved: SavedWord[]) {
    this.saved = saved;
    if (this.settings?.panelTab === 'saved') this.render(true);
  }

  /** Recolours the words when statuses changed. */
  refresh(changed: ReadonlyMap<string, number>, version: number) {
    this.changed = changed;
    if (version === this.version) return;
    this.version = version;
    if (!this.host) return;
    if (this.settings?.panelTab === 'subtitles') {
      this.body.querySelectorAll<HTMLElement>('.word[data-lc]').forEach((span) => {
        const status = currentStatus({ text: '', lc: span.dataset.lc, status: Number(span.dataset.status) }, changed);
        span.className = `word ${statusClass(status)}${span.classList.contains('open') ? ' open' : ''}`;
      });
    } else {
      this.render(true);
    }
  }

  /** Marks the line playing, and keeps it in view unless the reader scrolled away. */
  setCurrent(index: number) {
    if (!this.host || index === this.current) return;
    this.current = index;
    if (this.settings?.panelTab !== 'subtitles') return;
    this.body.querySelector('.row.current')?.classList.remove('current');
    const row = this.body.querySelector<HTMLElement>(`.row[data-index="${index}"]`);
    row?.classList.add('current');
    if (row && this.following && !this.pointerInside) this.scrollTo(row);
  }

  setProgress(text: string | null) {
    this.progress = text;
    if (this.settings?.panelTab === 'words') this.renderFooter();
  }

  /** Fits the panel to the player beside it, or to the window when it sits under the video. */
  fit() {
    if (!this.host) return;
    const player = document.getElementById('movie_player')?.getBoundingClientRect();
    const own = this.host.getBoundingClientRect();
    if (!player) return;
    const beside = own.top < player.bottom - 40;
    const height = Math.round(beside ? Math.max(MIN_HEIGHT, player.bottom - own.top) : Math.min(UNDER_MAX_HEIGHT, Math.max(MIN_HEIGHT, innerHeight * 0.7)));
    const value = `${height}px`;
    if (this.host.style.getPropertyValue('--height') !== value) this.host.style.setProperty('--height', value);
  }

  show(visible: boolean) {
    if (this.host) this.host.hidden = !visible;
  }

  private scrollTo(row: HTMLElement) {
    this.autoScrollAt = performance.now();
    this.body.scrollTop = Math.max(0, row.offsetTop - this.body.clientHeight / 3);
  }

  private render(keepScroll = false) {
    const settings = this.settings;
    if (!this.host || !settings) return;
    this.panel.classList.toggle('folded', !settings.panelOpen);
    const fold = this.root.querySelector<HTMLElement>('.fold')!;
    fold.textContent = settings.panelOpen ? '▴' : '▾';
    fold.title = settings.panelOpen ? 'Fold the panel' : 'Open the panel';
    this.root.querySelector<HTMLElement>('.open')!.hidden = !this.words;
    this.root.querySelectorAll<HTMLElement>('[data-tab]').forEach((tab) => {
      tab.classList.toggle('on', tab.dataset.tab === settings.panelTab);
      if (tab.dataset.tab === 'saved') tab.textContent = this.saved.length ? `Saved ${this.saved.length}` : 'Saved';
    });
    const key = `${settings.panelTab}|${settings.panelTranslations}|${settings.wordsOrder}|${settings.wordsShown.join()}|${settings.readings}`;
    if (key === this.rendered && !keepScroll) return;
    const scroll = this.body.scrollTop;
    const sameTab = this.rendered.split('|')[0] === settings.panelTab;
    this.rendered = key;
    this.body.replaceChildren();
    this.body.classList.toggle('translations', settings.panelTranslations);
    if (settings.panelTab === 'subtitles') this.renderLines();
    else if (settings.panelTab === 'words') this.renderWords();
    else this.renderSaved();
    this.renderToolbar();
    this.renderFooter();
    if (settings.panelTab === 'subtitles' && this.following) {
      const row = this.body.querySelector<HTMLElement>(`.row[data-index="${this.current}"]`);
      if (row) this.scrollTo(row);
    } else if (sameTab) {
      this.body.scrollTop = scroll;
    }
  }

  private renderToolbar() {
    const settings = this.settings!;
    this.toolbar.replaceChildren();
    if (settings.panelTab === 'subtitles') {
      this.toolbar.append(this.checkbox('Translations always', settings.panelTranslations, (on) => this.events.onSettings({ panelTranslations: on })));
      const sync = pill('Follow the video', false);
      sync.className += ' push';
      sync.hidden = this.following;
      sync.title = 'Scroll back to the line playing and keep it in view';
      sync.addEventListener('click', () => {
        this.following = true;
        this.renderToolbar();
        const row = this.body.querySelector<HTMLElement>('.row.current');
        if (row) this.scrollTo(row);
      });
      this.toolbar.append(sync);
      const print = pill('Print', false);
      if (this.following) print.className += ' push';
      print.title = 'The transcript with its translations and your words marked, to print or save as PDF';
      print.addEventListener('click', () => this.events.onPrint());
      this.toolbar.append(print);
    } else if (settings.panelTab === 'words' && this.words) {
      const counts = groupCounts(videoWords(this.lines, this.changed, this.ranks));
      for (const group of WORD_GROUPS) {
        const chip = pill(`${GROUP_NAMES[group]} ${counts[group]}`, settings.wordsShown.includes(group));
        chip.addEventListener('click', () => {
          const shown = settings.wordsShown.includes(group) ? settings.wordsShown.filter((g) => g !== group) : [...settings.wordsShown, group];
          this.events.onSettings({ wordsShown: shown });
        });
        this.toolbar.append(chip);
      }
      const order = document.createElement('select');
      order.className = 'push';
      order.title = 'Order';
      for (const [value, text] of [['rank', 'Most common first'], ['count', 'Most said in the video'], ['video', 'As the video says them']]) {
        order.append(new Option(text, value, false, value === settings.wordsOrder));
      }
      order.addEventListener('change', () => this.events.onSettings({ wordsOrder: order.value as ExtensionSettings['wordsOrder'] }));
      this.toolbar.append(order);
    }
  }

  private renderFooter() {
    this.footer.replaceChildren();
    if (this.settings?.panelTab !== 'words' || !this.words) return;
    if (this.progress) {
      this.footer.append(this.progress);
      return;
    }
    const fresh = this.listedWords().filter((w) => groupOf(w.status) === 'new');
    if (!fresh.length) return;
    if (!this.confirming) {
      const mark = pill(`Mark the ${fresh.length} new words listed as known`, false);
      mark.addEventListener('click', () => {
        this.confirming = true;
        this.renderFooter();
      });
      this.footer.append(mark);
      return;
    }
    this.footer.append(`Mark ${fresh.length} words as known in Tayra?`);
    const yes = pill('Mark as known', true);
    yes.addEventListener('click', () => {
      this.confirming = false;
      this.events.onMarkKnown(fresh);
    });
    const no = pill('Cancel', false);
    no.addEventListener('click', () => {
      this.confirming = false;
      this.renderFooter();
    });
    this.footer.append(yes, no);
  }

  private renderLines() {
    if (!this.lines.length) return this.body.append(empty('No subtitles yet.'));
    const fragment = document.createDocumentFragment();
    this.lines.forEach((line, index) => {
      const row = document.createElement('div');
      row.className = index === this.current ? 'row current' : 'row';
      row.dataset.index = String(index);
      const time = document.createElement('button');
      time.className = 'time';
      time.textContent = clock(line.beginMs);
      const content = document.createElement('div');
      const text = document.createElement('div');
      text.className = 'text';
      this.appendWords(text, line);
      const tr = document.createElement('div');
      tr.className = 'tr';
      tr.textContent = line.translation ?? '';
      content.append(text, tr);
      const actions = document.createElement('div');
      actions.className = 'actions';
      actions.innerHTML = `<button class="speak" title="Say the line">${SPEAK_ICON}</button><button class="copy" title="Copy the line">${COPY_ICON}</button>`;
      row.append(time, content, actions);
      fragment.append(row);
    });
    this.body.append(fragment);
  }

  private appendWords(into: HTMLElement, line: BridgeLine) {
    if (!line.segments.length) {
      into.textContent = line.text;
      return;
    }
    for (const segment of line.segments) {
      if (segment.term && segment.lc) {
        const span = wordSpan(segment.text, segment.term, segment.lc, currentStatus(segment, this.changed));
        span.dataset.status = String(segment.status ?? 0);
        fillWord(span, segment, this.settings?.readings ?? 'readings');
        into.append(span);
      } else {
        into.append(segment.text);
      }
    }
  }

  private listedWords(): VideoWord[] {
    const settings = this.settings!;
    const words = videoWords(this.lines, this.changed, this.ranks).filter((w) => settings.wordsShown.includes(groupOf(w.status)));
    return ordered(words, settings.wordsOrder);
  }

  private renderWords() {
    if (!this.words) return this.body.append(empty('Open the Tayra app, with this video’s language as the one you learn, to see its words.'));
    const all = videoWords(this.lines, this.changed, this.ranks);
    if (!all.length) return this.body.append(empty('No words yet.'));
    const share = Math.round(knownShare(all) * 100);
    const summary = document.createElement('div');
    summary.className = 'share';
    summary.innerHTML = `<div></div><div class="bar"><div></div></div>`;
    summary.firstElementChild!.textContent = `You know ${share}% of the words this video says.`;
    (summary.querySelector('.bar > div') as HTMLElement).style.width = `${share}%`;
    this.body.append(summary);
    const worth = suggested(all, SUGGESTED);
    if (worth.length && this.settings!.wordsShown.includes('new')) {
      this.body.append(section('Worth learning first'));
      for (const word of worth) this.body.append(this.wordItem(word));
    }
    const listed = this.listedWords();
    this.body.append(section(`${listed.length} words`));
    if (!listed.length) this.body.append(empty('No words in the groups chosen above.'));
    const fragment = document.createDocumentFragment();
    for (const word of listed) fragment.append(this.wordItem(word));
    this.body.append(fragment);
  }

  private wordItem(word: VideoWord): HTMLElement {
    const item = document.createElement('div');
    item.className = 'item';
    const head = document.createElement('div');
    head.append(wordSpan(word.text, word.term, word.lc, word.status, word.line));
    const meta = document.createElement('span');
    meta.className = 'meta';
    meta.textContent = [word.rank ? `#${word.rank}` : '', `×${word.count}`].filter(Boolean).join(' · ');
    meta.title = 'Its place among the language’s most common words, and how many times the video says it';
    item.append(head, meta, example(this.lines[word.line]?.text ?? '', word.line));
    return item;
  }

  private renderSaved() {
    if (!this.saved.length) return this.body.append(empty('Words you save from this video’s word card are listed here.'));
    const statuses = new Map(videoWords(this.lines, this.changed, this.ranks).map((w) => [w.lc, w.status]));
    for (const saved of this.saved) {
      const index = Math.max(0, lineIndexAt(this.lines, saved.beginMs));
      const item = document.createElement('div');
      item.className = 'item';
      const head = document.createElement('div');
      head.append(wordSpan(saved.term, saved.term, saved.lc, statuses.get(saved.lc) ?? this.changed.get(saved.lc) ?? null, index));
      const forget = document.createElement('button');
      forget.className = 'forget';
      forget.title = 'Take it off this list (Tayra keeps the word)';
      forget.dataset.forget = saved.lc;
      forget.textContent = '×';
      item.append(head, forget, example(`${clock(saved.beginMs)}  ${saved.line}`, index));
      this.body.append(item);
    }
  }

  private clicked(event: Event) {
    const target = event.target as HTMLElement;
    // Words have their own gestures.
    if (target.closest('.word')) return;
    const row = target.closest<HTMLElement>('.row');
    const forget = target.closest<HTMLElement>('[data-forget]')?.dataset.forget;
    if (forget) return this.events.onForget(forget);
    const seek = target.closest<HTMLElement>('[data-seek]')?.dataset.seek;
    if (seek !== undefined) return this.events.onSeek(Number(seek));
    if (!row) return;
    const index = Number(row.dataset.index);
    if (target.closest('.copy')) return this.events.onCopy(this.lines[index]?.text ?? '');
    if (target.closest('.speak')) return this.events.onSpeak(this.lines[index]?.text ?? '');
    this.following = true;
    this.renderToolbar();
    this.events.onSeek(index);
  }

  private checkbox(label: string, value: boolean, change: (value: boolean) => void): HTMLElement {
    const row = document.createElement('label');
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = value;
    input.addEventListener('change', () => change(input.checked));
    row.append(input, label);
    return row;
  }
}

function wordSpan(text: string, term: string, lc: string, status: number | null, line?: number): HTMLElement {
  const span = document.createElement('span');
  span.className = `word ${statusClass(status)}`;
  span.dataset.term = term;
  span.dataset.lc = lc;
  span.dataset.status = String(status ?? 0);
  if (line !== undefined) span.dataset.line = String(line);
  span.textContent = text;
  return span;
}

function example(text: string, line: number): HTMLElement {
  const element = document.createElement('div');
  element.className = 'example';
  element.dataset.seek = String(line);
  element.title = 'Play this line';
  element.textContent = text;
  return element;
}

function pill(text: string, on: boolean): HTMLButtonElement {
  const button = document.createElement('button');
  button.className = on ? 'pill on' : 'pill';
  button.textContent = text;
  return button;
}

function section(text: string): HTMLElement {
  const element = document.createElement('div');
  element.className = 'section';
  element.textContent = text;
  return element;
}

function empty(text: string): HTMLElement {
  const element = document.createElement('div');
  element.className = 'empty';
  element.textContent = text;
  return element;
}

/** [ms] as YouTube writes times: 1:05, or 1:02:05 past an hour. */
export function clock(ms: number): string {
  const total = Math.floor(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = String(total % 60).padStart(2, '0');
  return h ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`;
}
