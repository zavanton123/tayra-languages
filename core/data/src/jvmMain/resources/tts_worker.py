#!/usr/bin/env python3
"""Speech worker for Tayra Languages: Piper and Kokoro behind one JSON-line protocol.

One request per line on stdin, one reply per line on stdout:
  {"id": 1, "cmd": "status", "engine": "piper"}  -> {"id": 1, "version": "1.8.0", "python": "3.12.14"}
  {"id": 2, "cmd": "synthesize", "engine": "piper", "model": "/path/voice.onnx", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
  {"id": 3, "cmd": "synthesize", "engine": "kokoro", "model": "/path/kokoro.onnx", "voices": "/path/voices.bin",
   "voice": "pf_dora", "lang": "pt-br", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
Kokoro's status also lists the languages whose readers fail to load: {"id": 4, ..., "missing": ["zh", "ja"]}.
Any failure replies {"id": ..., "error": "..."}. Loaded models are kept for the next request.
"""
import importlib.util
import json
import sys
import types
import wave

PIPER = {}
KOKORO = {}
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
    raise RuntimeError(f"unknown engine {engine!r}")


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
