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
        # Every core: a sentence takes about a third less time than with the default four.
        WHISPER[(name, folder)] = WhisperModel(name, device="cpu", compute_type="int8", download_root=folder, cpu_threads=os.cpu_count() or 0)
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
    # A prompt the speech matches word for word can leave Whisper thinking it is already written down,
    # and it gives back nothing: heard again without one.
    if prompt and not any(c.isalnum() for c in text):
        text = heard(whisper(req), audio, req["lang"], None)
    return {"text": text}


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
