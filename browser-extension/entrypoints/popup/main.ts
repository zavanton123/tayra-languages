import { ask } from '../../src/runtime-messages';
import { loadSettings, saveSettings } from '../../src/settings';

const content = document.getElementById('content')!;

function paragraph(text: string, className = ''): HTMLParagraphElement {
  const p = document.createElement('p');
  p.textContent = text;
  if (className) p.className = className;
  return p;
}

/** The sites the extension works on. Firefox asks the user for them after installing; Chrome grants them at once. */
const ORIGINS = ['https://www.youtube.com/*', 'http://127.0.0.1/*'];

async function render() {
  const granted = await browser.permissions.contains({ origins: ORIGINS }).catch(() => true);
  if (!granted) {
    content.replaceChildren(paragraph('Allow Idiomiko to work on YouTube and to reach the Idiomiko app on this computer.', 'muted'));
    const allow = document.createElement('button');
    allow.textContent = 'Allow';
    // A permission is asked for from a click; the popup closes while the browser asks.
    allow.onclick = () => void browser.permissions.request({ origins: ORIGINS }).then(() => render());
    content.append(allow);
    return;
  }
  const status = await ask({ type: 'status' });
  content.replaceChildren(await enabledSwitch());
  if (status.ok) {
    const language = status.data.language?.name;
    content.append(
      paragraph('Connected to Idiomiko', 'ok'),
      paragraph(language ? `Open a YouTube video in ${language} to watch it with your words.` : 'Choose a language to learn in Idiomiko.', 'muted'),
    );
    return;
  }
  if (status.error === 'offline') {
    content.append(
      paragraph('Open Idiomiko on this computer to use the extension.'),
      paragraph('While the app is open, the extension colours subtitles by the words you know and looks words up. Without it, the extension still shows subtitles and their translation.', 'muted'),
      tryAgain(),
    );
    return;
  }
  if (status.error === 'unconnected') {
    // The app is open, but its helper gave this browser no token.
    if (typeof browser.runtime.sendNativeMessage !== 'function') {
      content.append(paragraph(`This browser isn’t supported yet — use ${BROWSERS}.`));
      return;
    }
    content.append(
      paragraph('Idiomiko is open, but this browser could not connect to it.'),
      paragraph(`Quit and reopen Idiomiko so it can set up this browser, then try again. If it still can’t connect, this browser isn’t supported yet — use ${BROWSERS}.`, 'muted'),
      tryAgain(),
    );
    return;
  }
  content.append(paragraph(status.message ?? 'Idiomiko could not answer.', 'error'));
}

/** The browsers the app sets up for the extension (NativeHostInstaller). */
const BROWSERS = 'Chrome, Edge, Brave, Vivaldi, Arc or Firefox';

function tryAgain(): HTMLButtonElement {
  const button = document.createElement('button');
  button.className = 'quiet';
  button.textContent = 'Try again';
  button.onclick = () => void render();
  return button;
}

/** Turns the Idiomiko subtitles on YouTube on or off. */
async function enabledSwitch(): Promise<HTMLElement> {
  const settings = await loadSettings();
  const row = document.createElement('label');
  row.className = 'switch';
  const box = document.createElement('input');
  box.type = 'checkbox';
  box.checked = settings.enabled;
  box.onchange = async () => saveSettings({ ...(await loadSettings()), enabled: box.checked });
  row.append(box, 'Idiomiko subtitles on YouTube');
  return row;
}

void render();
