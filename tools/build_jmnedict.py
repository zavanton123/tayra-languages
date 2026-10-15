#!/usr/bin/env python3
"""
Builds the Japanese names pack, ja-names, from JMnedict (EDRDG, CC BY-SA 4.0), the names companion
of JMdict that 10ten Japanese Reader looks names up in: people's family and given names, places,
stations, companies and works, about 740,000 of them, each with its reading and romanised name.

An entry per spelling: a name is read in many ways (小池: こいけ, おいけ, こいや...), so each
reading's romanised names are a sense, the reading after them, with the kind of name (surname,
place...) as tags; a spelling read one way only has that reading as its entry's. Every reading is
a form of its spellings, so a name written in kana finds them. The pack is recorded in dictionaries/wty/built.json with the others, for
--write-catalog to list it in the dictionaries-v2 release.

Usage: python3 tools/build_jmnedict.py
"""
import gzip
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import finish_output, open_output  # noqa: E402
from build_yomitan_dictionary import record  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "dictionaries", "japanese", "JMnedict.xml.gz")
OUT = os.path.join(ROOT, "dictionaries", "ja-names.sqlite.gzip")

KINDS = {
    "surname": "surname", "place": "place", "unclass": "name", "fem": "female given name", "given": "given name",
    "person": "person", "masc": "male given name", "station": "station", "organization": "organisation",
    "company": "company", "work": "work", "product": "product", "char": "character", "serv": "service",
    "fict": "fictional", "group": "group", "ev": "event", "obj": "object", "relig": "religion", "creat": "creature",
    "dei": "deity", "doc": "document", "leg": "legend", "myth": "mythology", "oth": "other", "ship": "ship",
}


def text_of(line):
    return re.sub(r"<[^>]+>", "", line).strip()


def entries():
    with gzip.open(SOURCE, "rt", encoding="utf-8") as f:
        entry = None
        for raw in f:
            line = raw.strip()
            if line == "<entry>":
                entry = {"k": [], "r": [], "trans": []}
            elif line == "</entry>":
                yield entry
                entry = None
            elif entry is None:
                continue
            elif line.startswith("<keb>"):
                entry["k"].append(text_of(line))
            elif line.startswith("<reb>"):
                entry["r"].append({"text": text_of(line), "restr": []})
            elif line.startswith("<re_restr>"):
                entry["r"][-1]["restr"].append(text_of(line))
            elif line == "<trans>":
                entry["trans"].append({"names": [], "kinds": []})
            elif line.startswith("<name_type>"):
                entry["trans"][-1]["kinds"] += [KINDS.get(c, c) for c in re.findall(r"&([^;]+);", line)]
            elif line.startswith("<trans_det"):
                # Translations other than the romanised name are rare; the English ones are kept.
                if 'xml:lang' not in line or 'xml:lang="eng"' in line:
                    entry["trans"][-1]["names"].append(text_of(line))


def main():
    db, path = open_output(OUT)
    by_word, forms = {}, set()
    for entry in entries():
        translations = [t for t in entry["trans"] if t["names"]]
        if not translations:
            continue
        pairs = [(k, r["text"]) for r in entry["r"] for k in entry["k"] if not r["restr"] or k in r["restr"]] if entry["k"] else [(r["text"], None) for r in entry["r"]]
        for word, reading in dict.fromkeys(pairs):
            readings = by_word.setdefault(word, {})
            readings.setdefault(reading, []).extend(t for t in translations if t not in readings.get(reading, []))
            if reading:
                forms.add((reading, word))
    rows = []
    for word in sorted(by_word):
        readings = by_word[word]
        senses = []
        for reading, translations in readings.items():
            for t in translations:
                gloss = "; ".join(t["names"]) + (f" ({reading})" if reading and len(readings) > 1 else "")
                senses.append({"glosses": [gloss]} | ({"tags": list(dict.fromkeys(t["kinds"]))} if t["kinds"] else {}))
        rows.append((word, next(iter(readings)) if len(readings) == 1 else None, senses, list(readings)))
    db.executemany(
        "INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES (?, ?, ?, 'name', NULL, ?)",
        ((i + 1, word, word.lower(), json.dumps(senses, ensure_ascii=False, separators=(",", ":"))) for i, (word, _, senses, _) in enumerate(rows)),
    )
    db.executemany("INSERT INTO forms(form_lc, lemma, generated) VALUES (?, ?, 0)", sorted({(f.lower(), lemma) for f, lemma in forms}))
    db.executescript(
        "CREATE TABLE readings (word TEXT NOT NULL, reading TEXT NOT NULL); CREATE INDEX readings_word ON readings(word);"
        "CREATE TABLE entry_readings (entry_id INTEGER NOT NULL PRIMARY KEY, reading TEXT NOT NULL);"
    )
    db.executemany("INSERT INTO readings VALUES (?, ?)", sorted({(word, r) for word, _, _, all_readings in rows for r in all_readings if r}))
    db.executemany("INSERT INTO entry_readings VALUES (?, ?)", [(i + 1, reading) for i, (_, reading, _, _) in enumerate(rows) if reading])
    finish_output(db, path, OUT, "ja", "names", len(rows), len(forms), "JMnedict (EDRDG, CC BY-SA 4.0)")
    os.remove(path)
    size = os.path.getsize(OUT)
    record("ja-names", size)
    print(f"ja-names: {len(rows):,} entries, {len(forms):,} forms, {size:,} bytes", file=sys.stderr)


if __name__ == "__main__":
    main()
