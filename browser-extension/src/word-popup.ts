import { STATUS_BUTTONS } from './statuses';

const STYLE = `
  :host { all: initial; }
  .popup { position: fixed; z-index: 2147483000; width: max-content; max-width: 300px; padding: 10px 12px; background: #1d2125; color: #e6e9ec; border: 1px solid #2d3238;
    border-radius: 10px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.55); font: 14px/1.4 system-ui, -apple-system, 'Segoe UI', sans-serif; }
  .popup[hidden] { display: none; }
  .head { display: flex; align-items: center; gap: 8px; }
  .term { font-weight: 650; font-size: 15px; }
  .rank { color: #8a949e; font-size: 12px; font-variant-numeric: tabular-nums; }
  .say { margin-left: auto; width: 26px; height: 26px; padding: 0; border: 0; border-radius: 6px; background: none; color: #a3acb5; cursor: pointer; }
  .say:hover { background: #2d3238; color: #e6e9ec; }
  .say svg { width: 16px; height: 16px; fill: currentColor; }
  .meaning { margin-top: 4px; color: #dbe6ff; }
  .meaning.muted { color: #a3acb5; }
  .statuses { display: flex; gap: 4px; margin-top: 8px; }
  .statuses button { min-width: 28px; padding: 3px 6px; border: 1px solid #3a4148; border-radius: 6px; background: #23282d; color: #e6e9ec; font: 600 12px system-ui, sans-serif; cursor: pointer; }
  .statuses button:hover { border-color: #8fc3ec; }
  .statuses button.current { background: #8fc3ec; border-color: #8fc3ec; color: #0b2a40; }
  .hint { margin-top: 6px; color: #8a949e; font-size: 11px; }
`;

export const SPEAK_ICON = '<svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 8v8a4.5 4.5 0 0 0 2.5-4zM14 3.2v2.1a7 7 0 0 1 0 13.4v2.1a9 9 0 0 0 0-17.6z"/></svg>';

/** Space between the word and the popup, and the window's edges, in px. */
const GAP = 8;

export interface PopupWord {
  term: string;
  status: number;
  rank: number | null;
  /** What it means; null while it is being looked up. */
  meaning: string | null;
  /** The meaning is a remark, such as there being none. */
  muted?: boolean;
}

export interface WordPopupEvents {
  onStatus(status: number): void;
  onSpeak(): void;
  /** The pointer came onto the popup, or left it. */
  onInside(inside: boolean): void;
}

/**
 * The small card a word gets while the pointer rests on it: its meaning, how common it is, and
 * buttons to say it and set its status. Over everything, in full screen too.
 */
export class WordPopup {
  private host: HTMLElement | null = null;
  private popup!: HTMLElement;
  private anchor: HTMLElement | null = null;

  constructor(private readonly events: WordPopupEvents) {}

  private mount() {
    const parent = document.fullscreenElement ?? document.body;
    if (this.host?.parentElement === parent) return;
    if (!this.host) {
      this.host = document.createElement('div');
      this.host.id = 'tayra-word-popup';
      const root = this.host.attachShadow({ mode: 'open' });
      root.innerHTML = `<style>${STYLE}</style><div class="popup" hidden></div>`;
      this.popup = root.querySelector('.popup')!;
      for (const type of ['click', 'mousedown', 'mouseup', 'dblclick', 'pointerdown', 'pointerup', 'contextmenu', 'wheel']) {
        root.addEventListener(type, (event) => event.stopPropagation());
      }
      this.popup.addEventListener('mouseenter', () => this.events.onInside(true));
      this.popup.addEventListener('mouseleave', () => this.events.onInside(false));
      this.popup.addEventListener('click', (event) => {
        const target = event.target as HTMLElement;
        const status = target.closest<HTMLElement>('[data-status]')?.dataset.status;
        if (status !== undefined) this.events.onStatus(Number(status));
        else if (target.closest('.say')) this.events.onSpeak();
      });
    }
    parent.append(this.host);
  }

  show(anchor: HTMLElement, word: PopupWord) {
    this.mount();
    this.anchor = anchor;
    this.render(word);
  }

  /** Shows [word] anew where it is, when it is still the word shown. */
  update(anchor: HTMLElement, word: PopupWord) {
    if (this.anchor === anchor && !this.popup.hidden) this.render(word);
  }

  hide() {
    this.anchor = null;
    if (this.host) this.popup.hidden = true;
  }

  isShown(): boolean {
    return !!this.host && !this.popup.hidden;
  }

  private render(word: PopupWord) {
    const head = document.createElement('div');
    head.className = 'head';
    const term = document.createElement('span');
    term.className = 'term';
    term.textContent = word.term.replace(/​/g, '');
    head.append(term);
    if (word.rank) {
      const rank = document.createElement('span');
      rank.className = 'rank';
      rank.textContent = `#${word.rank}`;
      rank.title = 'Its place among the language’s most common words';
      head.append(rank);
    }
    const say = document.createElement('button');
    say.className = 'say';
    say.title = 'Say it';
    say.innerHTML = SPEAK_ICON;
    head.append(say);
    const meaning = document.createElement('div');
    meaning.className = word.meaning === null || word.muted ? 'meaning muted' : 'meaning';
    meaning.textContent = word.meaning ?? 'Looking it up…';
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
    const hint = document.createElement('div');
    hint.className = 'hint';
    hint.textContent = 'Click the word for its card · right-click for its next status';
    this.popup.replaceChildren(head, meaning, statuses, hint);
    this.popup.hidden = false;
    this.place();
  }

  /** Above the word when there is room, else under it, within the window. */
  private place() {
    if (!this.anchor) return;
    const word = this.anchor.getBoundingClientRect();
    const width = this.popup.offsetWidth;
    const height = this.popup.offsetHeight;
    const left = Math.min(Math.max(GAP, word.left + word.width / 2 - width / 2), window.innerWidth - width - GAP);
    const top = word.top - height - GAP >= GAP ? word.top - height - GAP : Math.min(word.bottom + GAP, window.innerHeight - height - GAP);
    this.popup.style.left = `${Math.max(GAP, left)}px`;
    this.popup.style.top = `${Math.max(GAP, top)}px`;
  }
}
