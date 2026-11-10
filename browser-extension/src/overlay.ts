import type { BridgeLine, LookupResponse } from './bridge-types';
import { currentStatus } from './lines';
import { STATUS_BUTTONS, statusClass } from './statuses';

const STYLE = `
  :host { position: absolute; inset: 0; z-index: 60; pointer-events: none; font-family: system-ui, -apple-system, 'Segoe UI', sans-serif; }
  .subtitles { position: absolute; left: 8%; right: 8%; bottom: 11%; display: flex; flex-direction: column; align-items: center; gap: 0.35em; text-align: center; }
  .line, .translation { pointer-events: auto; max-width: 100%; padding: 0.12em 0.45em; border-radius: 0.3em; background: rgba(8, 8, 8, 0.8); color: #fff; }
  .line { font-size: clamp(16px, 2.5vw, 34px); line-height: 1.35; }
  .translation { font-size: clamp(13px, 1.7vw, 23px); color: #dbe6ff; }
  .line:empty, .translation:empty { display: none; }
  .word { cursor: pointer; border-radius: 0.2em; padding: 0 0.04em; }
  .word:hover { outline: 2px solid rgba(255, 255, 255, 0.75); }
  .word.s0 { background: #cde0fb; color: #12213a; }
  .word.s1 { background: #fae39e; color: #2b2208; }
  .word.s2 { background: #f8e7b4; color: #2b2208; }
  .word.s3 { background: #f6ebc9; color: #2b2208; }
  .word.s4 { background: #f4efdf; color: #2b2208; }
  .word.open { outline: 2px solid #8fc3ec; }
  .notice { position: absolute; top: 12px; left: 12px; padding: 6px 10px; border-radius: 6px; background: rgba(31, 95, 139, 0.95); color: #fff; font-size: 13px; }
  .notice:empty { display: none; }
  .card { pointer-events: auto; position: absolute; left: 50%; bottom: calc(11% + 7.5em); transform: translateX(-50%); width: min(440px, 86%); max-height: 52%; overflow: auto;
    background: #fff; color: #1b1f24; border-radius: 12px; box-shadow: 0 12px 40px rgba(0, 0, 0, 0.45); font-size: 14px; line-height: 1.45; text-align: left; }
  .card[hidden] { display: none; }
  .card header { position: sticky; top: 0; display: flex; align-items: baseline; gap: 10px; padding: 12px 14px 8px; background: #fff; border-bottom: 1px solid #e3e8ee; }
  .card h2 { margin: 0; font-size: 20px; font-weight: 650; }
  .card .reading { color: #5c6670; }
  .card .close { margin-left: auto; border: 0; background: none; color: #5c6670; font-size: 20px; line-height: 1; cursor: pointer; }
  .card .statuses { display: flex; flex-wrap: wrap; gap: 6px; padding: 10px 14px; }
  .card .statuses button { min-width: 34px; padding: 5px 8px; border: 1px solid #cfd6de; border-radius: 8px; background: #fff; color: #1b1f24; font: 600 13px system-ui, sans-serif; cursor: pointer; }
  .card .statuses button.current { background: #1f5f8b; border-color: #1f5f8b; color: #fff; }
  .card .body { padding: 0 14px 14px; display: grid; gap: 10px; }
  .card .mine { padding: 8px 10px; border-radius: 8px; background: #f1f5f9; }
  .card .entry .head { font-weight: 600; }
  .card .entry .pos { margin-left: 6px; color: #5c6670; font-style: italic; font-weight: 400; }
  .card .entry ol { margin: 4px 0 0; padding-left: 20px; }
  .card .muted { color: #5c6670; }
  .card .error { color: #b3261e; }
`;

export interface OverlayEvents {
  /** A word of [line] was clicked. */
  onWord(term: string, lc: string, line: BridgeLine, element: HTMLElement): void;
  onStatus(status: number): void;
  onCloseCard(): void;
}

/**
 * What the extension draws over YouTube's player: the current line with its words coloured by
 * status, its translation, and the card of a word clicked. Inside the player, so it shows in full
 * screen; in a shadow root, so YouTube's styles leave it alone.
 */
export class Overlay {
  private host: HTMLElement | null = null;
  private line!: HTMLElement;
  private translation!: HTMLElement;
  private notice!: HTMLElement;
  private card!: HTMLElement;
  private shown: BridgeLine | null = null;
  private shownVersion = -1;
  private noticeTimer: ReturnType<typeof setTimeout> | undefined;

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
      <div class="card" hidden></div>
      <div class="subtitles"><div class="line"></div><div class="translation"></div></div>`;
    this.line = root.querySelector('.line')!;
    this.translation = root.querySelector('.translation')!;
    this.notice = root.querySelector('.notice')!;
    this.card = root.querySelector('.card')!;
    // Clicks here are the overlay's: the player under it would play or pause.
    for (const type of ['click', 'mousedown', 'mouseup', 'dblclick', 'pointerdown', 'pointerup']) {
      root.addEventListener(type, (event) => event.stopPropagation());
    }
    this.line.addEventListener('click', (event) => {
      const word = (event.target as HTMLElement).closest<HTMLElement>('.word');
      if (word?.dataset.term && word.dataset.lc && this.shown) this.events.onWord(word.dataset.term, word.dataset.lc, this.shown, word);
    });
    this.card.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      const status = target.closest<HTMLElement>('[data-status]')?.dataset.status;
      if (status !== undefined) this.events.onStatus(Number(status));
      else if (target.closest('.close')) this.events.onCloseCard();
    });
    player.appendChild(host);
    this.host = host;
    this.shown = null;
    return true;
  }

  /** Shows [line], its words coloured by [changed] over the statuses they came with; [version] says when those changed. */
  show(line: BridgeLine | null, changed: ReadonlyMap<string, number>, version: number) {
    if (!this.host) return;
    if (line === this.shown && version === this.shownVersion) return;
    this.shown = line;
    this.shownVersion = version;
    this.line.replaceChildren();
    if (line) {
      if (line.segments.length) {
        for (const segment of line.segments) {
          if (segment.term && segment.lc) {
            const span = document.createElement('span');
            span.className = `word ${statusClass(currentStatus(segment, changed))}`;
            span.dataset.term = segment.term;
            span.dataset.lc = segment.lc;
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
    this.translation.textContent = line?.translation ?? '';
  }

  say(text: string, forMs = 6000) {
    if (!this.host) return;
    this.notice.textContent = text;
    clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => (this.notice.textContent = ''), forMs);
  }

  cardOpen(): boolean {
    return !!this.host && !this.card.hidden;
  }

  showCardLoading(term: string) {
    this.card.hidden = false;
    this.card.replaceChildren(header(term, null), paragraph('Looking it up…', 'body muted'));
  }

  showCardError(term: string, text: string) {
    this.card.hidden = false;
    this.card.replaceChildren(header(term, null), paragraph(text, 'body error'));
  }

  showCard(word: LookupResponse) {
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
    this.card.replaceChildren(header(word.term, word.entries[0]?.ipa ?? null), statuses, body);
  }

  closeCard() {
    if (!this.host) return;
    this.card.hidden = true;
    this.line.querySelector('.open')?.classList.remove('open');
  }

  hide() {
    this.show(null, new Map(), -1);
    this.closeCard();
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

function paragraph(text: string, className: string): HTMLElement {
  const element = document.createElement('div');
  element.className = className;
  element.textContent = text;
  return element;
}
