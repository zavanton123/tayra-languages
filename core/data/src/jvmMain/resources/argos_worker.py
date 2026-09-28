#!/usr/bin/env python3
"""Argos Translate worker for Tayra Languages.

Reads one JSON request per line on stdin and writes one JSON reply per line on stdout:
  {"id": 1, "cmd": "status"}                          -> {"id": 1, "version": "...", "pairs": ["pt-en", ...]}
  {"id": 2, "cmd": "install", "from": "pt", "to": "en"} -> {"id": 2, "installed": ["pt-en"]}
  {"id": 3, "cmd": "translate", "from": "pt", "to": "en", "q": "..."} -> {"id": 3, "t": "..."}
Any failure replies {"id": ..., "error": "..."}.
"""
import json
import os
import sys

# python.org builds of Python ship without a CA bundle, which makes the package index
# download fail deep inside argostranslate; certifi (an argostranslate dependency) has one.
if not os.environ.get("SSL_CERT_FILE"):
    try:
        import certifi
        os.environ["SSL_CERT_FILE"] = certifi.where()
    except Exception:  # noqa: BLE001
        pass

try:
    import argostranslate.package
    import argostranslate.translate
    IMPORT_ERROR = None
except Exception as e:  # noqa: BLE001
    IMPORT_ERROR = f"argostranslate is not installed for this Python ({e}). Install it with: pip install argostranslate"


def version():
    try:
        from importlib.metadata import version as v
        return v("argostranslate")
    except Exception:  # noqa: BLE001
        return "unknown"


def installed_pairs():
    pairs = []
    for lang in argostranslate.translate.get_installed_languages():
        for target in argostranslate.translate.get_installed_languages():
            if lang.code != target.code and lang.get_translation(target) is not None:
                pairs.append(f"{lang.code}-{target.code}")
    return sorted(set(pairs))


def direct_pairs():
    pairs = set()
    for lang in argostranslate.translate.get_installed_languages():
        for tr in getattr(lang, "translations_from", []):
            pairs.add(f"{tr.from_lang.code}-{tr.to_lang.code}")
    return pairs


def install(src, dst):
    argostranslate.package.update_package_index()
    available = argostranslate.package.get_available_packages()
    have = direct_pairs()
    wanted = [(src, dst)]
    if not any(p.from_code == src and p.to_code == dst for p in available):
        # No direct model: go through English, as Argos does when translating.
        wanted = [(src, "en"), ("en", dst)]
    installed = []
    for a, b in wanted:
        if a == b or f"{a}-{b}" in have:
            continue
        pkg = next((p for p in available if p.from_code == a and p.to_code == b), None)
        if pkg is None:
            raise RuntimeError(f"no Argos package for {a}-{b}")
        argostranslate.package.install_from_path(pkg.download())
        installed.append(f"{a}-{b}")
    return installed


def handle(req):
    cmd = req.get("cmd")
    if IMPORT_ERROR:
        raise RuntimeError(IMPORT_ERROR)
    if cmd == "status":
        return {"version": version(), "pairs": installed_pairs()}
    if cmd == "install":
        return {"installed": install(req["from"], req["to"])}
    if cmd == "translate":
        src, dst, text = req["from"], req["to"], req["q"]
        if f"{src}-{dst}" not in installed_pairs():
            raise RuntimeError(f"no Argos package installed for {src}-{dst}")
        return {"t": argostranslate.translate.translate(text, src, dst)}
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
            reply = {"id": req_id, "error": str(e)}
        sys.stdout.write(json.dumps(reply, ensure_ascii=False) + "\n")
        sys.stdout.flush()


if __name__ == "__main__":
    main()
