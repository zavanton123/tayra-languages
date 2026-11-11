import { fromBase64 } from './base64';

/** How many words and lines keep their audio while the page is open. */
const KEPT = 60;

/**
 * Says words and lines: with the voice chosen in Tayra (Piper or Kokoro), whose audio the app makes,
 * or with the browser's own voice when Tayra uses the system's, or isn't open.
 */
export class Speaker {
  private context: AudioContext | null = null;
  private playing: AudioBufferSourceNode | null = null;
  private readonly kept = new Map<string, Uint8Array | null>();
  private asked = 0;

  constructor(private readonly fetchAudio: (text: string) => Promise<{ audio: string | null } | null>) {}

  async say(text: string, languageCode: string | null) {
    const spoken = text.replace(/​/g, '').trim();
    if (!spoken) return;
    this.stop();
    const asked = ++this.asked;
    let audio = this.kept.get(spoken);
    if (audio === undefined) {
      const answer = await this.fetchAudio(spoken).catch(() => null);
      audio = answer?.audio ? fromBase64(answer.audio) : null;
      // Without the app the browser speaks, and the app is asked again next time.
      if (answer) this.keep(spoken, audio);
    }
    if (asked !== this.asked) return;
    if (audio) await this.play(audio);
    else this.browserVoice(spoken, languageCode);
  }

  stop() {
    this.playing?.stop();
    this.playing = null;
    speechSynthesis.cancel();
  }

  private keep(text: string, audio: Uint8Array | null) {
    this.kept.delete(text);
    this.kept.set(text, audio);
    if (this.kept.size > KEPT) this.kept.delete(this.kept.keys().next().value!);
  }

  private async play(audio: Uint8Array) {
    this.context ??= new AudioContext();
    if (this.context.state === 'suspended') await this.context.resume();
    // Decoding takes the buffer over, so it gets a copy and the kept audio stays.
    const buffer = await this.context.decodeAudioData(audio.slice().buffer);
    const source = this.context.createBufferSource();
    source.buffer = buffer;
    source.connect(this.context.destination);
    source.start();
    this.playing = source;
  }

  private browserVoice(text: string, languageCode: string | null) {
    const utterance = new SpeechSynthesisUtterance(text);
    if (languageCode) utterance.lang = languageCode;
    speechSynthesis.speak(utterance);
  }
}
