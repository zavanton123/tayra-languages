#!/usr/bin/env python3
"""
Builds the Toki Pona dictionary packs and word frequency list from Linku (lipu Linku, linku.la,
CC BY-SA 4.0), the community dictionary of Toki Pona: the English Wiktionary does not cover the
language, so neither kaikki.org nor wiktionary-to-yomitan has it.

Each word's definition in a gloss language becomes its senses (split at semicolons), and the
frequency list ranks the words by their latest usage share in Linku's community survey. The packs
have the layout tools/build_dictionary.py writes and are recorded in dictionaries/wty/built.json
with the converted ones, since they are published in the same release.

Usage: python3 tools/build_toki_pona.py
"""
import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import finish_output, open_output  # noqa: E402
from build_yomitan_dictionary import record  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "dictionaries", "linku-words.json")
FREQUENCY = os.path.join(ROOT, "feature/frequency/src/commonMain/composeResources/files/frequency/tok.tsv")
# The app's gloss languages, by Linku's codes for them.
TARGETS = {"en": "en", "ru": "ru", "de": "de", "fr": "fr", "es": "es", "pt": "pt", "pl": "pl", "el": "el", "zh": "zh_hans"}


def words():
    if not os.path.exists(CACHE):
        subprocess.run(["curl", "-fsSL", "-o", CACHE, "https://api.linku.la/v1/words?lang=*"], check=True)
    with open(CACHE, encoding="utf-8") as f:
        return json.load(f)


def latest_usage(word):
    usage = word.get("usage") or {}
    return usage[max(usage)] if usage else 0


def main():
    data = {k: v for k, v in words().items() if not v.get("deprecated")}
    for target, linku in TARGETS.items():
        out = os.path.join(ROOT, "dictionaries", f"tok-{target}.sqlite.gzip")
        db, path = open_output(out)
        rows = []
        for word in sorted(data):
            definition = (data[word].get("translations", {}).get(linku) or {}).get("definition", "").strip()
            if not definition:
                continue
            senses = [{"glosses": [part.strip()]} for part in definition.split(";") if part.strip()]
            rows.append((word, json.dumps(senses, ensure_ascii=False, separators=(",", ":"))))
        db.executemany(
            "INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES (?, ?, ?, 'unknown', NULL, ?)",
            ((i + 1, w, w.lower(), senses) for i, (w, senses) in enumerate(rows)),
        )
        finish_output(db, path, out, "tok", target, len(rows), 0, "lipu Linku (linku.la)")
        os.remove(path)
        size = os.path.getsize(out)
        record(f"tok-{target}", size)
        print(f"tok-{target}: {len(rows)} entries, {size:,} bytes", file=sys.stderr)
    ranked = sorted(data, key=lambda w: (-latest_usage(data[w]), w))
    with open(FREQUENCY, "w", encoding="utf-8") as f:
        f.write("# source: lipu Linku usage survey (CC BY-SA 4.0)\n")
        f.write(f"# words: {len(ranked)}\n")
        for word in ranked:
            f.write(f"{word}\t{word.lower()}\n")
    print(f"{FREQUENCY}: {len(ranked)} words", file=sys.stderr)


if __name__ == "__main__":
    main()
