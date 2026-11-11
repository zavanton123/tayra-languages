import type { PrintTranscript } from '../../src/transcript';

type Option = 'times' | 'translations' | 'saved' | 'learning' | 'new';

const DEFAULTS: Record<Option, boolean> = { times: true, translations: true, saved: true, learning: false, new: false };
const OPTIONS_KEY = 'printOptions';

/** The transcript the YouTube tab handed over, by the id in the address; it is taken, so a reload asks again. */
async function load(): Promise<PrintTranscript | null> {
  const id = new URLSearchParams(location.search).get('id');
  if (!id) return null;
  const key = `print:${id}`;
  const stored = await browser.storage.session.get(key);
  const transcript = (stored[key] as PrintTranscript | undefined) ?? null;
  // Kept for this tab under its own name, so reloading it still shows the transcript.
  if (transcript) sessionStorage.setItem(key, JSON.stringify(transcript));
  await browser.storage.session.remove(key);
  return transcript ?? JSON.parse(sessionStorage.getItem(key) ?? 'null');
}

function options(): Record<Option, boolean> {
  try {
    return { ...DEFAULTS, ...JSON.parse(localStorage.getItem(OPTIONS_KEY) ?? '{}') };
  } catch {
    return { ...DEFAULTS };
  }
}

function apply(chosen: Record<Option, boolean>) {
  const page = document.getElementById('page')!;
  page.classList.toggle('no-times', !chosen.times);
  page.classList.toggle('no-translations', !chosen.translations);
  page.classList.toggle('show-saved', chosen.saved);
  page.classList.toggle('show-learning', chosen.learning);
  page.classList.toggle('show-new', chosen.new);
  page.querySelectorAll<HTMLElement>('.legend [data-option]').forEach((item) => (item.hidden = !chosen[item.dataset.option as Option]));
}

function element<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string, text?: string): HTMLElementTagNameMap[K] {
  const made = document.createElement(tag);
  if (className) made.className = className;
  if (text !== undefined) made.textContent = text;
  return made;
}

function watchLink(transcript: PrintTranscript, seconds: number): string {
  return `https://www.youtube.com/watch?v=${encodeURIComponent(transcript.videoId)}&t=${seconds}s`;
}

function render(transcript: PrintTranscript) {
  document.title = `${transcript.title} · Transcript`;
  const page = document.getElementById('page')!;
  page.replaceChildren();
  page.append(element('h1', undefined, transcript.title));
  const meta = element('p', 'meta');
  const link = element('a', undefined, 'youtube.com');
  link.href = watchLink(transcript, 0);
  const made = new Date(transcript.madeAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
  meta.append([transcript.channel, transcript.language].filter(Boolean).join(' · ') + ' · ', link, ` · ${made}`);
  page.append(meta);

  const legend = element('div', 'legend');
  for (const [option, text, swatch] of [['saved', 'Saved from this video', 'var(--saved)'], ['learning', 'Being learned', 'var(--learning)'], ['new', 'New', 'var(--new)']] as const) {
    const item = element('span', undefined, text);
    item.dataset.option = option;
    item.style.setProperty('--swatch', swatch);
    legend.append(item);
  }
  if (transcript.language) page.append(legend);

  for (const line of transcript.lines) {
    const row = element('div', 'line');
    const time = element('a', 'time', line.time);
    time.href = watchLink(transcript, line.seconds);
    const text = element('div', 'text');
    for (const piece of line.pieces) text.append(piece.mark ? element('mark', piece.mark, piece.text) : piece.text);
    row.append(time, text);
    if (line.translation) row.append(element('div', 'translation', line.translation));
    if (line.human && line.human !== line.translation) row.append(element('div', 'translation human', line.human));
    page.append(row);
  }

  if (transcript.saved.length) {
    page.append(element('h2', undefined, `Words saved from this video (${transcript.saved.length})`));
    const table = element('table');
    for (const word of transcript.saved) {
      const row = element('tr');
      row.append(element('td', 'term', word.term), element('td', 'at', word.time), element('td', undefined, word.line));
      table.append(row);
    }
    page.append(table);
  }
  page.append(element('footer', undefined, 'Made with Tayra for YouTube'));
}

async function main() {
  const transcript = await load().catch(() => null);
  const page = document.getElementById('page')!;
  if (!transcript) {
    page.replaceChildren(element('p', 'muted', 'This transcript is no longer here. Print it again from the video’s Tayra panel.'));
    return;
  }
  render(transcript);
  const chosen = options();
  const controls = document.getElementById('controls')!;
  controls.hidden = false;
  controls.querySelectorAll<HTMLInputElement>('input[data-option]').forEach((input) => {
    const option = input.dataset.option as Option;
    input.checked = chosen[option];
    if (!transcript.language && option !== 'times' && option !== 'translations') input.closest('label')!.hidden = true;
    input.addEventListener('change', () => {
      chosen[option] = input.checked;
      try {
        localStorage.setItem(OPTIONS_KEY, JSON.stringify(chosen));
      } catch {
        // Not kept; the page still shows the choice.
      }
      apply(chosen);
    });
  });
  document.getElementById('print')!.addEventListener('click', () => window.print());
  apply(chosen);
}

void main();
