import { ask } from '../../src/runtime-messages';

const content = document.getElementById('content')!;

function paragraph(text: string, className = ''): HTMLParagraphElement {
  const p = document.createElement('p');
  p.textContent = text;
  if (className) p.className = className;
  return p;
}

async function render() {
  const status = await ask({ type: 'status' });
  content.replaceChildren();
  if (status.ok) {
    const language = status.data.language?.name;
    content.append(
      paragraph('Connected to Tayra', 'ok'),
      paragraph(language ? `Open a YouTube video in ${language} to watch it with your words.` : 'Choose a language to learn in Tayra.', 'muted'),
    );
    const forget = document.createElement('button');
    forget.className = 'quiet';
    forget.textContent = 'Unpair this browser';
    forget.onclick = async () => {
      await ask({ type: 'unpair' });
      await render();
    };
    content.append(forget);
    return;
  }
  if (status.error === 'offline') {
    content.append(
      paragraph('The Tayra app is not open.'),
      paragraph('Start Tayra on this computer to look words up and colour subtitles. Without it, the extension still shows subtitles and their translation.', 'muted'),
    );
    return;
  }
  if (status.error === 'unpaired') {
    content.append(paragraph('In Tayra, open Settings → Browser extension, press “Pair a browser”, and type the code here.', 'muted'));
    const form = document.createElement('form');
    const input = document.createElement('input');
    input.inputMode = 'numeric';
    input.autocomplete = 'off';
    input.placeholder = '000 000';
    input.maxLength = 7;
    const button = document.createElement('button');
    button.type = 'submit';
    button.textContent = 'Pair';
    form.append(input, button);
    const problem = paragraph('', 'error');
    form.onsubmit = async (event) => {
      event.preventDefault();
      button.disabled = true;
      const paired = await ask({ type: 'pair', code: input.value });
      button.disabled = false;
      if (paired.ok) await render();
      else problem.textContent = paired.error === 'offline' ? 'The Tayra app is not open.' : 'That code is wrong or has expired. Ask Tayra for a new one.';
    };
    content.append(form, problem);
    input.focus();
    return;
  }
  content.append(paragraph(status.message ?? 'Tayra could not answer.', 'error'));
}

void render();
