#!/usr/bin/env python3
"""Speech worker for Tayra Languages: Piper and Kokoro behind one JSON-line protocol.

One request per line on stdin, one reply per line on stdout:
  {"id": 1, "cmd": "status", "engine": "piper"}  -> {"id": 1, "version": "1.8.0", "python": "3.12.14"}
  {"id": 2, "cmd": "synthesize", "engine": "piper", "model": "/path/voice.onnx", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
  {"id": 3, "cmd": "synthesize", "engine": "kokoro", "model": "/path/kokoro.onnx", "voices": "/path/voices.bin",
   "voice": "pf_dora", "lang": "pt-br", "text": "...", "speed": "1.0", "out": "/tmp/x.wav"}
Any failure replies {"id": ..., "error": "..."}. Loaded models are kept for the next request.
"""
import json
import sys
import wave

PIPER = {}
KOKORO = {}


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
        return {"version": version("kokoro-onnx"), "python": sys.version.split()[0]}
    raise RuntimeError(f"unknown engine {engine!r}")


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
        samples, rate = model.create(text, voice=req["voice"], speed=speed, lang=req["lang"])
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
