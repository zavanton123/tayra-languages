#!/usr/bin/env python3
"""Speech worker for Tayra Languages: Piper and Kokoro, and faster-whisper to hear speech, behind one JSON-line protocol.

One request per line on stdin, one reply per line on stdout:
  {"id": 1, "cmd": "status", "engine": "piper"}  -> {"id": 1, "version": "1.8.0", "python": "3.12.14"}
  {"id": 2, "cmd": "synthesize", "engine": "piper", "model": "/path/voice.onnx", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
  {"id": 3, "cmd": "synthesize", "engine": "kokoro", "model": "/path/kokoro.onnx", "voices": "/path/voices.bin",
   "voice": "pf_dora", "lang": "pt-br", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
Kokoro's status also lists the languages whose readers fail to load: {"id": 4, ..., "missing": ["zh", "ja"]}.
  {"id": 5, "cmd": "prepare", "engine": "whisper", "model": "small", "models": "/path/whisper"}  -> {"model": "small"}
  {"id": 6, "cmd": "transcribe", "engine": "whisper", "model": "small", "models": "/path/whisper", "lang": "pt",
   "audio": "/tmp/x.wav", "prompt": "the sentence expected"}  -> {"text": "..."}
  {"id": 7, "cmd": "transcribe_media", "engine": "whisper", "model": "small", "models": "/path/whisper", "lang": "pt",
   "media": "https://.../video.mp4", "progress": "/tmp/p"}  -> {"segments": [{"start": 0, "end": 1500, "text": "...",
   "words": [[0, 400, " O"], ...]}, ...]} (milliseconds). While it works, the file "progress" holds "audio 0.42" or
   "speech 0.42"; a file named like it with ".cancel" added stops it.
Any failure replies {"id": ..., "error": "..."}. Loaded models are kept for the next request.
"""
import importlib.util
import json
import os
import sys
import types
import wave

PIPER = {}
KOKORO = {}
WHISPER = {}
READERS = {}


def version(package):
    try:
        from importlib.metadata import version as v
        return v(package)
    except Exception:  # noqa: BLE001
        return "unknown"


def status(engine):
    if engine == "piper":
        import piper  # noqa: F401
        return {"version": version("piper-tts"), "python": sys.version.split()[0]}
    if engine == "kokoro":
        import kokoro_onnx  # noqa: F401
        import soundfile  # noqa: F401
        missing = []
        for lang in MISAKI_LANGS:
            try:
                reader(lang)
            except Exception:  # noqa: BLE001
                missing.append(lang)
        return {"version": version("kokoro-onnx"), "python": sys.version.split()[0], "missing": missing}
    if engine == "whisper":
        import faster_whisper  # noqa: F401
        return {"version": version("faster-whisper"), "python": sys.version.split()[0]}
    raise RuntimeError(f"unknown engine {engine!r}")


def whisper(req):
    """The faster-whisper model named in the request, downloaded into its folder the first time."""
    name, folder = req["model"], req["models"]
    if (name, folder) not in WHISPER:
        from faster_whisper import WhisperModel
        # Eight threads: the large model takes a third less time than with the default four, and more slow the small one down.
        WHISPER[(name, folder)] = WhisperModel(name, device="cpu", compute_type="int8", download_root=folder, cpu_threads=min(8, os.cpu_count() or 4))
    return WHISPER[(name, folder)]


def transcribe(req):
    # The app records 16 kHz mono 16-bit WAV, read here as is: faster-whisper's own decoder breaks with newer PyAV.
    import numpy
    with wave.open(req["audio"], "rb") as w:
        if (w.getframerate(), w.getnchannels(), w.getsampwidth()) != (16000, 1, 2):
            raise RuntimeError("expected 16 kHz mono 16-bit audio")
        audio = numpy.frombuffer(w.readframes(w.getnframes()), dtype=numpy.int16).astype(numpy.float32) / 32768.0
    # One short answer: no carrying over between segments, which only makes up text after a pause. The sentence
    # expected, as the text before it, leans the model towards its words, which an accent otherwise hides.
    prompt = req.get("prompt") or None
    text = heard(whisper(req), audio, req["lang"], prompt)
    # Whisper takes a prompt for words already written down: speech that goes on past it can lose the words
    # it matches. Short of most of them, it is heard again without one, and the closer of the two is kept.
    if prompt and share(text, prompt) < PROMPT_SHARE:
        plain = heard(whisper(req), audio, req["lang"], None)
        if share(plain, prompt) > share(text, prompt):
            text = plain
    return {"text": text}


PROMPT_SHARE = 0.8


class Cancelled(Exception):
    pass


def transcribe_media(req):
    progress = req["progress"]

    def report(stage, fraction):
        partial = progress + ".part"
        with open(partial, "w") as f:
            f.write(f"{stage} {max(0.0, min(1.0, fraction)):.3f}")
        os.replace(partial, progress)

    def check():
        if os.path.exists(progress + ".cancel"):
            raise Cancelled("cancelled")

    audio = media_audio(req["media"], report, check)
    if len(audio) == 0:
        return {"segments": []}
    seconds = len(audio) / 16000.0
    report("speech", 0.0)
    # The voice detector skips music and silence, where Whisper otherwise makes up words; nothing carries over
    # between segments, which can set it repeating a line for minutes.
    segments, _ = whisper(req).transcribe(audio, language=req["lang"], beam_size=5, vad_filter=True, word_timestamps=True,
                                          condition_on_previous_text=False)
    heard = []
    for segment in segments:
        check()
        report("speech", segment.end / seconds)
        heard.append({
            "start": round(segment.start * 1000), "end": round(segment.end * 1000), "text": segment.text.strip(),
            "words": [[round(w.start * 1000), round(w.end * 1000), w.word] for w in (segment.words or [])],
        })
    return {"segments": heard}


def media_audio(url, report, check):
    """The sound of the media at [url] (a web address or a file) as 16 kHz mono samples, read by PyAV as it comes."""
    import av
    import numpy
    chunks = []
    try:
        container = av.open(url, timeout=30)
    except Exception as e:  # noqa: BLE001
        raise RuntimeError(f"the video could not be opened: {e}") from e
    with container:
        stream = next((s for s in container.streams if s.type == "audio"), None)
        if stream is None:
            raise RuntimeError("the video has no sound")
        stream.thread_type = "AUTO"
        seconds = container.duration / 1_000_000 if container.duration else 0
        resampler = av.AudioResampler(format="s16", layout="mono", rate=16000)
        for i, frame in enumerate(container.decode(stream)):
            for out in resampler.resample(frame):
                chunks.append(out.to_ndarray().reshape(-1).copy())
            if i % 100 == 0:
                check()
                if seconds and frame.time is not None:
                    report("audio", frame.time / seconds)
        for out in resampler.resample(None):
            chunks.append(out.to_ndarray().reshape(-1).copy())
    if not chunks:
        return numpy.zeros(0, dtype=numpy.float32)
    return numpy.concatenate(chunks).astype(numpy.float32) / 32768.0


def share(text, expected):
    """How many of the words of [expected] are in [text], as a share."""
    def words(t):
        return [w for w in "".join(c.lower() if c.isalnum() or c in "'’" else " " for c in t).split()]
    wanted = words(expected)
    given = set(words(text))
    return sum(1 for w in wanted if w in given) / len(wanted) if wanted else 1.0


def heard(model, audio, lang, prompt):
    # One pass, at most a long sentence's tokens: Whisper's retries at higher temperatures, set off when
    # a prompt makes its first try look repetitive, took half a minute over a single sentence.
    segments, _ = model.transcribe(audio, language=lang, beam_size=5, condition_on_previous_text=False, initial_prompt=prompt,
                                   temperature=0.0, max_new_tokens=128, without_timestamps=True)
    return " ".join(s.text.strip() for s in segments).strip()


# The voices of these languages were trained on misaki's readers, not on eSpeak's, which
# reads Chinese poorly and Japanese kanji not at all; the others go through kokoro-onnx's eSpeak.
MISAKI_LANGS = ("zh", "ja")


def reader(lang):
    """misaki's reader for the language, set up as Kokoro's own pipeline does for the v1.0 model."""
    found = READERS.get(lang)
    if found is None:
        if lang == "zh":
            from misaki import zh
            found = zh.ZHG2P()
        else:
            # misaki.ja imports pyopenjtalk up front, yet only its "pyopenjtalk" version uses it; the default
            # reader runs on fugashi and UniDic. pyopenjtalk has no wheels, so a stand-in spares a compiler.
            if importlib.util.find_spec("pyopenjtalk") is None:
                sys.modules["pyopenjtalk"] = types.ModuleType("pyopenjtalk")
            from misaki import ja
            found = ja.JAG2P()
        READERS[lang] = found
    return found


KOKORO_PADDING_SECONDS = 0.2


def synthesize(req):
    engine, text, out = req["engine"], req["text"], req["out"]
    speed = max(0.3, min(3.0, float(req.get("speed") or 1.0)))
    if engine == "piper":
        from piper import PiperVoice
        voice = PIPER.get(req["model"])
        if voice is None:
            voice = PIPER[req["model"]] = PiperVoice.load(req["model"])
        with wave.open(out, "wb") as wav:
            try:
                from piper import SynthesisConfig
                voice.synthesize_wav(text, wav, syn_config=SynthesisConfig(length_scale=1.0 / speed))
            except ImportError:
                voice.synthesize(text, wav, length_scale=1.0 / speed)
        return {"out": out}
    if engine == "kokoro":
        from kokoro_onnx import Kokoro
        import soundfile
        key = (req["model"], req["voices"])
        model = KOKORO.get(key)
        if model is None:
            model = KOKORO[key] = Kokoro(req["model"], req["voices"])
        lang = req["lang"]
        if lang in MISAKI_LANGS:
            phonemes = reader(lang)(text)[0]
            samples, rate = model.create(phonemes, voice=req["voice"], speed=speed, lang=lang, is_phonemes=True)
        else:
            samples, rate = model.create(text, voice=req["voice"], speed=speed, lang=lang)
        # Kokoro trims the silence off both ends, leaving a few hundredths of a second, which
        # sounds cut off once the player's own start-up eats into it: give each end some quiet.
        import numpy
        quiet = numpy.zeros(int(rate * KOKORO_PADDING_SECONDS), dtype=samples.dtype)
        samples = numpy.concatenate([quiet, samples, quiet])
        soundfile.write(out, samples, rate, subtype="PCM_16")
        return {"out": out}
    raise RuntimeError(f"unknown engine {engine!r}")


def handle(req):
    cmd = req.get("cmd")
    if cmd == "status":
        return status(req.get("engine"))
    if cmd == "synthesize":
        return synthesize(req)
    if cmd == "prepare":
        whisper(req)
        return {"model": req["model"]}
    if cmd == "transcribe":
        return transcribe(req)
    if cmd == "transcribe_media":
        return transcribe_media(req)
    raise RuntimeError(f"unknown command {cmd!r}")


def main():
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        req_id = None
        try:
            req = json.loads(line)
            req_id = req.get("id")
            reply = handle(req)
            reply["id"] = req_id
        except Exception as e:  # noqa: BLE001
            reply = {"id": req_id, "error": str(e) or e.__class__.__name__}
        sys.stdout.write(json.dumps(reply, ensure_ascii=False) + "\n")
        sys.stdout.flush()


if __name__ == "__main__":
    main()
