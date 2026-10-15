#!/usr/bin/env python3
"""
Builds an offline dictionary pack for the app from a Yomitan dictionary made by
wiktionary-to-yomitan (wty), the dictionaries Yomitan offers for Wiktionary languages.

A wty dictionary is a zip of term banks: JSON rows of [term, reading, tags, rules, score,
definitions, sequence, term tags], where a definition is either structured content (an HTML-like
tree holding the glosses, their tags and examples) or a [lemma, [inflection]] pair saying the term
is a form of the lemma. The pack keeps what the app shows: the glosses with their tags, the part
of speech, a transcription from wty's IPA dictionaries (the gloss language's edition, else the
English one), and the forms table, to which the forms of the
same language's English-gloss dictionary (the app's own when dictionaries/<source>-en.sqlite is
there) are added for the lemmas the pack has: the English Wiktionary lists far more inflections
than the others. The layout is the one
tools/build_dictionary.py writes.

Downloads are cached in dictionaries/wty/. The script prints the pack's size and entry count;
a pair whose pack would hold fewer than --min-entries entries is not written.

--glossaries builds word finders, "<native>-<learning>-gloss" packs: the words of a native language
with their translations into the learning one, from the translation tables of the native
language's Wiktionary (wty's glossary dictionaries), each meaning's label kept as its tag.

Built packs are listed in dictionaries/wty/built.json; --write-catalog writes that list into the
app's catalog (DictionaryPackSizes.kt) as the packs of the dictionaries-v2 release, and the word
finders as those of the glossaries-v1 release, which is where the files are published.

Usage: python3 tools/build_yomitan_dictionary.py --source ka --target en [--wty-source CODE] [--out FILE]
       python3 tools/build_yomitan_dictionary.py --pairs ka-en,ka-ru,...
       python3 tools/build_yomitan_dictionary.py --glossaries ru-en,ru-de,...
       python3 tools/build_yomitan_dictionary.py --write-catalog [--include '.*-pl']
"""
import argparse
import json
import os
import re
import sqlite3
import subprocess
import sys
import unicodedata
import zipfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import CLEANERS, finish_output, is_clean_form, lowercase, open_output, serbian_spellings  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "dictionaries", "wty")
BASE_URL = "https://huggingface.co/datasets/daxida/wty-release/resolve/main/latest/dict"
BUILT = os.path.join(CACHE, "built.json")
CATALOG = os.path.join(ROOT, "core/domain/src/commonMain/kotlin/com/tayra/languages/core/domain/dictionary/DictionaryPackSizes.kt")
RELEASE = "dictionaries-v2"
# The word finders' release: the glossary packs, named "<native>-<learning>-gloss".
GLOSSARY_RELEASE = "glossaries-v1"

# The app's code where wty files the language under another: Serbo-Croatian covers Serbian,
# Croatian and Bosnian, and Norwegian is the Bokmål dictionary.
WTY_SOURCES = {"sr": "sh", "hr": "sh", "bs": "sh", "no": "nb"}
# Languages whose pack joins more than one wty dictionary, the first one's entries first: Cantonese
# words of their own are few, and Wiktionary files most words Cantonese writes under Chinese.
WTY_MERGED = {"yue": ["yue", "zh"]}

# wty's part-of-speech tags, as the app's packs name them (the names kaikki.org uses).
POS = {
    "n": "noun", "v": "verb", "vt": "verb", "vi": "verb", "vr": "verb", "vdt": "verb", "adj": "adj", "adv": "adv",
    "name": "name", "surn": "name", "prop-n": "name", "pron": "pron", "num": "num", "prep": "prep", "postp": "postp",
    "conj": "conj", "intj": "intj", "ptcl": "particle", "artic": "article", "det": "det", "suf": "suffix",
    "pref": "prefix", "phrase": "phrase", "prov": "proverb", "prep-phrase": "prep_phrase", "char": "character",
    "contr": "contraction", "counter": "counter", "adn": "adnominal", "ptcpl": "participle", "adj_noun": "adj",
    "r": "root", "pos-r": "root", "col": "phrase", "abbrev": "abbrev", "punct": "punct", "sym": "symbol",
    "affix": "affix", "infix": "infix", "interfix": "interfix", "circumfix": "circumfix", "classifier": "classifier",
}

# Structured-content blocks that are not the gloss itself.
SKIPPED_BLOCKS = {"tags", "details-entry-examples", "extra-info", "example-sentence", "backlink", "synonyms", "antonyms", "related-words", "preamble"}

# A sense whose tags are all one of these says nothing about the meaning.
NOISE_TAGS = {"form-of", "alt-of", "no-gloss", "morpheme", "romanization"}

# Glosses that only say a capitalised headword is a name. The Greek Wiktionary has a bot-made
# entry for every surname it met in each language ("επώνυμο (ανδρικό ή γυναικείο)"), which would
# outnumber the words; such entries are left out.
NAME_GLOSSES = {"el": re.compile(r"επώνυμο|όνομα")}


def download(name, url):
    """The cached file for a wty download, fetched with curl when missing; None when the release lacks it."""
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    missing = path + ".missing"
    if os.path.exists(path):
        return path
    if os.path.exists(missing):
        return None
    status = subprocess.run(["curl", "-sSL", "-f", "-o", path + ".part", url], capture_output=True)
    if status.returncode != 0:
        open(missing, "w").close()
        return None
    os.replace(path + ".part", path)
    return path


def main_dictionary(source, target):
    return download(f"wty-{source}-{target}.zip", f"{BASE_URL}/{source}/{target}/wty-{source}-{target}.zip")


def ipa_dictionary(source, target):
    return download(f"wty-{source}-{target}-ipa.zip", f"{BASE_URL}/{source}/{target}/wty-{source}-{target}-ipa.zip")


def banks(path, prefix):
    with zipfile.ZipFile(path) as z:
        tags = {}
        for name in z.namelist():
            if name.startswith("tag_bank"):
                for row in json.loads(z.read(name)):
                    tags[row[0]] = (row[1], row[3] or row[0])
        rows = []
        for name in sorted(z.namelist()):
            if name.startswith(prefix):
                rows.extend(json.loads(z.read(name)))
    return rows, tags


def text_of(node):
    """The visible text of a structured-content node, leaving out tags, examples and nested lists."""
    if isinstance(node, str):
        return node
    if isinstance(node, list):
        return "".join(text_of(x) for x in node)
    if not isinstance(node, dict):
        return ""
    if node.get("tag") in ("ol", "ul", "details", "summary", "a", "img", "rt", "rp"):
        return ""
    if node.get("tag") == "br":
        return " "
    if (node.get("data") or {}).get("content") in SKIPPED_BLOCKS:
        return ""
    return text_of(node.get("content"))


def tags_of(node):
    """The titles of the tag spans in a gloss line ("feminine", "soccer")."""
    out = []

    def walk(n):
        if isinstance(n, list):
            for x in n:
                walk(x)
        elif isinstance(n, dict):
            if n.get("tag") in ("ol", "ul", "details"):
                return
            if (n.get("data") or {}).get("content") == "tag":
                title = n.get("title") or text_of(n.get("content"))
                if title:
                    out.append(title.strip())
                return
            walk(n.get("content"))

    walk(node)
    return out


# Example sentences kept per sense; Wiktionary has dozens for some common words.
MAX_EXAMPLES = 3


def examples_of(node):
    """The example sentences of a gloss line, with their translations: [{"text", "translation"}]."""
    out = []

    def walk(n):
        if isinstance(n, list):
            for x in n:
                walk(x)
        elif isinstance(n, dict):
            if n.get("tag") in ("ol", "ul"):
                return
            if (n.get("data") or {}).get("content") == "example-sentence":
                parts = {}
                for child in n.get("content") if isinstance(n.get("content"), list) else [n.get("content")]:
                    kind = (child.get("data") or {}).get("content") if isinstance(child, dict) else None
                    if kind in ("example-sentence-a", "example-sentence-b"):
                        parts[kind] = " ".join(text_of(child.get("content")).split())
                if parts.get("example-sentence-a"):
                    example = {"text": parts["example-sentence-a"]}
                    if parts.get("example-sentence-b"):
                        example["translation"] = parts["example-sentence-b"]
                    out.append(example)
                return
            walk(n.get("content"))

    walk(node)
    return out[:MAX_EXAMPLES]


def senses_of(node):
    """Each item of the glosses lists, nested ones as senses of their own: (gloss, tags, examples)."""
    out = []

    def items(n):
        if isinstance(n, list):
            for x in n:
                items(x)
        elif isinstance(n, dict):
            if n.get("tag") == "li" and (n.get("data") or {}).get("content") not in ("synonym-item", "antonym-item"):
                gloss = " ".join(text_of(n.get("content")).split())
                if gloss:
                    out.append((gloss, tags_of(n.get("content")), examples_of(n.get("content"))))
                items(nested_lists(n.get("content")))
                return
            if (n.get("data") or {}).get("content") in SKIPPED_BLOCKS:
                return
            items(n.get("content"))

    for block in find_blocks(node, "glosses"):
        items(block.get("content"))
    return out


def nested_lists(node):
    """The ol and ul elements inside a gloss line, where its sub-senses are."""
    found = []

    def walk(n):
        if isinstance(n, list):
            for x in n:
                walk(x)
        elif isinstance(n, dict):
            if n.get("tag") in ("ol", "ul") and (n.get("data") or {}).get("content") not in ("synonyms-list", "antonyms-list"):
                found.append(n)
                return
            if (n.get("data") or {}).get("content") in SKIPPED_BLOCKS:
                return
            walk(n.get("content"))

    walk(node)
    return found


def find_blocks(node, kind):
    found = []

    def walk(n):
        if isinstance(n, list):
            for x in n:
                walk(x)
        elif isinstance(n, dict):
            if (n.get("data") or {}).get("content") == kind:
                found.append(n)
                return
            walk(n.get("content"))

    walk(node)
    return found


def spellings_for(source):
    """How a word is stored: cleaned of stress and length marks, and for Serbian in both scripts."""
    clean = CLEANERS.get(source, lambda word: word)
    if source == "sr":
        return serbian_spellings
    if source in ("hr", "bs"):
        return lambda word: [] if any("Ѐ" <= c <= "ӿ" for c in word) else [clean(word)]
    return lambda word: [clean(word)]


def read_dictionary(path, source, target=None):
    """Returns (entries keyed by (word, pos), forms) from a wty main dictionary."""
    rows, tag_bank = banks(path, "term_bank")
    spellings = spellings_for(source)
    entries = {}
    forms = set()
    for term, reading, def_tags, _rules, _score, definitions, *_ in rows:
        words = spellings(term.strip())
        if not words:
            continue
        tags = def_tags.split() if def_tags else []
        pos_tag = next((t for t in tags if tag_bank.get(t, ("",))[0] == "partOfSpeech" or t in POS), None)
        pos = POS.get(pos_tag, "unknown") if pos_tag else "unknown"
        entry_tags = [tag_bank.get(t, ("", t))[1] for t in tags if t != pos_tag and t != "non-lemma" and tag_bank.get(t, ("",))[0] not in ("partOfSpeech", "")]
        senses = []
        for d in definitions:
            if isinstance(d, list):
                if d and isinstance(d[0], str):
                    for lemma in spellings(d[0].strip()):
                        for word in words:
                            if is_clean_form(word, lemma):
                                forms.add((word, lemma))
                continue
            if isinstance(d, str):
                senses.append({"glosses": [d.strip()]} | ({"tags": entry_tags} if entry_tags else {}))
                continue
            if isinstance(d, dict) and d.get("type") == "structured-content":
                for gloss, gloss_tags, examples in senses_of(d.get("content")):
                    kept = [t for t in entry_tags + gloss_tags if t not in NOISE_TAGS]
                    sense = {"glosses": [gloss]}
                    if kept:
                        sense["tags"] = list(dict.fromkeys(kept))
                    if examples:
                        sense["examples"] = examples
                    senses.append(sense)
            elif isinstance(d, dict) and d.get("type") == "text" and d.get("text"):
                senses.append({"glosses": [d["text"].strip()]})
        if not senses:
            continue
        names = NAME_GLOSSES.get(target)
        if names and term[:1].isupper() and all(names.search(g) for sense in senses for g in sense["glosses"]):
            continue
        for word in words:
            # Japanese and Chinese words have an entry per reading (打つ read うつ and ぶつ), as Yomitan shows them.
            key = (word, pos, reading.strip()) if source in READING_LANGUAGES else (word, pos)
            if key in entries:
                entries[key]["senses"].extend(s for s in senses if s not in entries[key]["senses"])
                entries[key]["reading"] = entries[key]["reading"] or reading.strip() or None
            else:
                entries[key] = {"word": word, "pos": pos, "senses": list(senses), "reading": reading.strip() or None, "order": len(entries)}
            if reading and reading.strip() and reading.strip() != word:
                forms.add((reading.strip(), word))
                if source == "zh":
                    # Pinyin is looked up as Yomitan normalises it, with and without its tone marks.
                    pinyin = clean_reading(source, reading)
                    forms.add((normalize_pinyin(pinyin), word))
                    forms.add((toneless_pinyin(pinyin), word))
    return entries, forms


def read_ipa(path, source):
    """The first transcription of each word in a wty IPA dictionary."""
    if path is None:
        return {}
    rows, _ = banks(path, "term_meta_bank")
    spellings = spellings_for(source)
    out = {}
    for row in rows:
        if len(row) < 3 or row[1] != "ipa":
            continue
        transcriptions = (row[2] or {}).get("transcriptions") or []
        ipa = next((t.get("ipa") for t in transcriptions if t.get("ipa")), None)
        if ipa:
            for word in spellings(row[0].strip()):
                out.setdefault(word, ipa)
    return out


# Languages whose words get a reading shown above them (furigana, pinyin): the reading wty gives
# each headword. Cantonese's pack borrows the Chinese dictionary, whose readings are Mandarin.
READING_LANGUAGES = {"ja", "zh"}
JAPANESE = os.path.join(ROOT, "dictionaries", "japanese")
# KANJIDIC2 has meanings in these languages; the other packs give its English ones.
KANJI_MEANING_LANGUAGES = {"en", "fr", "es", "pt"}


def normalize_pinyin(text):
    """Yomitan's normalizePinyin: composed, lowercase, without spaces or syllable marks."""
    return re.sub(r"[\s・:'’-]|//", "", unicodedata.normalize("NFC", text).lower())


def toneless_pinyin(text):
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", normalize_pinyin(text)) if not "\u0300" <= c <= "\u036f"))


def clean_reading(source, reading):
    """The reading as shown above a word: Chinese gives pinyin first and other romanisations after it in brackets."""
    if not reading:
        return None
    if source == "zh":
        reading = reading.split(" (")[0].strip()
    return reading or None


def kanji_rows(target):
    """
    One row per kanji of KANJIDIC2 (EDRDG, CC BY-SA 4.0): the character, its meanings in [target]
    (or English), on and kun readings, stroke count, school grade, JLPT level and frequency rank.
    """
    import gzip
    import xml.etree.ElementTree as ET
    path = os.path.join(JAPANESE, "kanjidic2.xml.gz")
    if not os.path.exists(path):
        subprocess.run(["curl", "-fsSL", "-o", path, "http://www.edrdg.org/kanjidic/kanjidic2.xml.gz"], check=True)
    language = target if target in KANJI_MEANING_LANGUAGES else "en"
    rows = []
    with gzip.open(path) as f:
        for _, element in ET.iterparse(f):
            if element.tag != "character":
                continue
            literal = element.findtext("literal")
            misc = element.find("misc")
            meanings, onyomi, kunyomi = [], [], []
            for group in element.iter("rmgroup"):
                for r in group.findall("reading"):
                    if r.get("r_type") == "ja_on":
                        onyomi.append(r.text)
                    elif r.get("r_type") == "ja_kun":
                        kunyomi.append(r.text)
                for m in group.findall("meaning"):
                    if (m.get("m_lang") or "en") == language:
                        meanings.append(m.text)
            def number(tag):
                value = misc.findtext(tag) if misc is not None else None
                return int(value) if value and value.isdigit() else None
            if meanings or onyomi or kunyomi:
                rows.append((literal, json.dumps(meanings, ensure_ascii=False), " ".join(onyomi), " ".join(kunyomi),
                             number("stroke_count"), number("grade"), number("jlpt"), number("freq")))
            element.clear()
    return rows, language


def pitch_rows():
    """Kanjium's pitch accents (CC BY-SA 4.0): word, reading and the positions of the downstep, "0" for none."""
    path = os.path.join(JAPANESE, "accents.txt")
    if not os.path.exists(path):
        subprocess.run(["curl", "-fsSL", "-o", path, "https://raw.githubusercontent.com/mifunetoshiro/kanjium/master/data/source_files/raw/accents.txt"], check=True)
    rows = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) == 3 and parts[2]:
                rows.append((parts[0], parts[1] or parts[0], parts[2]))
    return rows


def write_extras(db, source, target, rows):
    """The tables only some packs have: readings, and for Japanese kanji and pitch accents."""
    if source in READING_LANGUAGES:
        readings = sorted({(e["word"], clean_reading(source, e.get("reading"))) for e in rows if clean_reading(source, e.get("reading")) not in (None, e["word"])})
        db.executescript("CREATE TABLE readings (word TEXT NOT NULL, reading TEXT NOT NULL); CREATE INDEX readings_word ON readings(word);")
        db.executemany("INSERT INTO readings VALUES (?, ?)", readings)
        # Each entry's own reading, by its id: the rows are inserted in this order from 1.
        db.execute("CREATE TABLE entry_readings (entry_id INTEGER NOT NULL PRIMARY KEY, reading TEXT NOT NULL)")
        db.executemany("INSERT INTO entry_readings VALUES (?, ?)", [
            (i + 1, clean_reading(source, e.get("reading"))) for i, e in enumerate(rows) if clean_reading(source, e.get("reading")) not in (None, e["word"])
        ])
    if source == "ja":
        kanji, language = kanji_rows(target)
        db.executescript(
            "CREATE TABLE kanji (character TEXT NOT NULL PRIMARY KEY, meanings TEXT NOT NULL, onyomi TEXT NOT NULL, "
            "kunyomi TEXT NOT NULL, strokes INTEGER, grade INTEGER, jlpt INTEGER, frequency INTEGER);"
        )
        db.executemany("INSERT OR IGNORE INTO kanji VALUES (?, ?, ?, ?, ?, ?, ?, ?)", kanji)
        db.executescript("CREATE TABLE pitch (word TEXT NOT NULL, reading TEXT NOT NULL, positions TEXT NOT NULL); CREATE INDEX pitch_word ON pitch(word);")
        db.executemany("INSERT INTO pitch VALUES (?, ?, ?)", pitch_rows())
        db.executemany("INSERT INTO meta(key, value) VALUES (?, ?)", [
            ("kanji_meanings_language", language),
            ("kanji_source", "KANJIDIC2 (EDRDG, CC BY-SA 4.0)"),
            ("pitch_source", "The pitch accent notation was provided by Uros O. through his free database Kanjium (CC BY-SA 4.0)"),
        ])


def english_forms(source, wty):
    """
    The forms of the language's English-gloss dictionary: the app's own pack when it is in
    dictionaries/ (built from the English Wiktionary's full tables), else wty's.
    """
    local = os.path.join(ROOT, "dictionaries", f"{source}-en.sqlite")
    if os.path.exists(local):
        db = sqlite3.connect(local)
        try:
            return set(db.execute("SELECT form_lc, lemma FROM forms WHERE generated = 0"))
        finally:
            db.close()
    english = main_dictionary(wty, "en")
    return read_dictionary(english, source)[1] if english else set()


def build(source, target, out_path, wty_source=None, min_entries=1000):
    wty = wty_source or WTY_SOURCES.get(source, source)
    entries, forms = {}, set()
    for part in WTY_MERGED.get(source, [wty]):
        path = main_dictionary(part, target)
        if path is None:
            continue
        part_entries, part_forms = read_dictionary(path, source, target)
        for key, entry in part_entries.items():
            if key in entries:
                entries[key]["senses"].extend(s for s in entry["senses"] if s not in entries[key]["senses"])
            else:
                entries[key] = entry
        forms |= part_forms
    if not entries:
        print(f"{source}-{target}: wty has no {wty}-{target} dictionary", file=sys.stderr)
        return None
    words = sum(1 for e in entries.values() if e["pos"] != "name")
    if words < min_entries:
        print(f"{source}-{target}: only {words:,} entries besides names, not written", file=sys.stderr)
        return None
    lemmas = {e["word"] for e in entries.values()}
    if target != "en":
        forms |= {(form, lemma) for form, lemma in english_forms(source, wty) if lemma in lemmas}
    forms = {(form, lemma) for form, lemma in forms if lemma in lemmas}
    # The target edition's transcriptions first, then the English edition's, which has the most.
    ipa = read_ipa(ipa_dictionary(wty, "en"), source) if target != "en" else {}
    ipa.update(read_ipa(ipa_dictionary(wty, target), source))

    db, sqlite_path = open_output(out_path)
    # A word's readings keep wty's order, the main one first (日本: にほん, にっぽん, じっぽん).
    rows = sorted(entries.values(), key=lambda e: (e["word"].lower(), e["word"], e["pos"], e.get("order", 0)))
    db.executemany(
        "INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES (?, ?, ?, ?, ?, ?)",
        (
            (i + 1, e["word"], lowercase(e["word"], source), e["pos"], ipa.get(e["word"]) or (None if source in READING_LANGUAGES else e.get("reading")),
             json.dumps(e["senses"], ensure_ascii=False, separators=(",", ":")))
            for i, e in enumerate(rows)
        ),
    )
    form_rows = sorted({(lowercase(form, source), lemma) for form, lemma in forms})
    db.executemany("INSERT INTO forms(form_lc, lemma, generated) VALUES (?, ?, 0)", form_rows)
    write_extras(db, source, target, rows)
    finish_output(
        db, sqlite_path, out_path, source, target, len(rows), len(form_rows),
        f"wiktionary-to-yomitan wty-{wty}-{target} (Wiktionary via kaikki.org)",
    )
    size = os.path.getsize(out_path)
    print(f"{source}-{target}: {len(rows):,} entries, {len(form_rows):,} forms, {size:,} bytes", file=sys.stderr)
    return size


def glossary_dictionary(native, learning):
    return download(f"wty-{native}-{learning}-gloss.zip", f"{BASE_URL}/{native}/{learning}/wty-{native}-{learning}-gloss.zip")


def read_glossary(path, native, learning):
    """
    The entries of a wty glossary dictionary: words of [native] with the [learning] words its
    Wiktionary's translation tables give for each of their meanings, the meaning's label as a tag.
    """
    rows, tag_bank = banks(path, "term_bank")
    clean = CLEANERS.get(learning, lambda word: word)
    entries = {}
    for term, _reading, def_tags, _rules, _score, definitions, *_ in rows:
        word = term.strip()
        tags = def_tags.split() if def_tags else []
        pos_tag = next((t for t in tags if tag_bank.get(t, ("",))[0] == "partOfSpeech" or t in POS), None)
        pos = POS.get(pos_tag, "unknown") if pos_tag else "unknown"
        senses = []
        for d in definitions:
            if isinstance(d, str) and d.strip():
                senses.append({"glosses": [clean(d.strip())]})
            elif isinstance(d, dict) and d.get("type") == "structured-content":
                label = " ".join(text_of(find_blocks(d.get("content"), "sense-label")).split())
                for block in find_blocks(d.get("content"), "glossary"):
                    words = [clean(" ".join(text_of(li.get("content")).split())) for li in block.get("content") or [] if isinstance(li, dict) and li.get("tag") == "li"]
                    words = list(dict.fromkeys(w for w in words if w))
                    if words:
                        senses.append({"glosses": words} | ({"tags": [label]} if label else {}))
        if not word or not senses:
            continue
        key = (word, pos)
        if key in entries:
            entries[key]["senses"].extend(s for s in senses if s not in entries[key]["senses"])
        else:
            entries[key] = {"word": word, "pos": pos, "senses": senses}
    return entries


def build_glossary(native, learning, out_path, min_entries=100):
    """A word finder: [native] words with their [learning] translations, from wty's glossary dictionary."""
    wty = WTY_SOURCES.get(learning, learning)
    path = glossary_dictionary(native, wty)
    if path is None:
        print(f"{native}-{learning}-gloss: wty has no {native}-{wty} glossary", file=sys.stderr)
        return None
    entries = read_glossary(path, native, learning)
    if len(entries) < min_entries:
        print(f"{native}-{learning}-gloss: only {len(entries):,} entries, not written", file=sys.stderr)
        return None
    db, sqlite_path = open_output(out_path)
    rows = sorted(entries.values(), key=lambda e: (e["word"].lower(), e["word"], e["pos"]))
    db.executemany(
        "INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES (?, ?, ?, ?, NULL, ?)",
        ((i + 1, e["word"], lowercase(e["word"], native), e["pos"], json.dumps(e["senses"], ensure_ascii=False, separators=(",", ":"))) for i, e in enumerate(rows)),
    )
    finish_output(db, sqlite_path, out_path, native, learning, len(rows), 0, f"wiktionary-to-yomitan wty-{native}-{wty}-gloss (Wiktionary via kaikki.org)")
    size = os.path.getsize(out_path)
    print(f"{native}-{learning}-gloss: {len(rows):,} entries, {size:,} bytes", file=sys.stderr)
    return size


def record(name, size):
    built = json.load(open(BUILT)) if os.path.exists(BUILT) else {}
    if size:
        built[name] = size
    else:
        built.pop(name, None)
    with open(BUILT, "w") as f:
        json.dump(dict(sorted(built.items())), f, indent=1)


def write_catalog(include=None):
    """
    Writes the packs built so far into the app's catalog, the word dictionaries into the
    dictionaries-v2 block and the glossaries into the glossaries-v1 one: those whose name matches
    [include], when given, besides the ones the blocks already list.
    """
    built = json.load(open(BUILT)) if os.path.exists(BUILT) else {}
    text = open(CATALOG).read()
    for release, glossaries in ((RELEASE, False), (GLOSSARY_RELEASE, True)):
        block = re.compile(r'(    "' + release + r'" to mapOf\(\n)(.*?)(\n    \),)', re.S)
        found = block.search(text)
        if not found:
            sys.exit(f"no {release} block in {CATALOG}")
        listed = set(re.findall(r'"([a-z]+-[a-z]+(?:-gloss)?)" to', found.group(2)))
        sizes = {name: size for name, size in built.items() if name.endswith("-gloss") == glossaries
                 and (name in listed or include is None or re.fullmatch(include, name))}
        items = [f'"{name}" to {size:_}L' for name, size in sorted(sizes.items())]
        lines = ["        " + ", ".join(items[i:i + 5]) + "," for i in range(0, len(items), 5)]
        text = block.sub(lambda m: m.group(1) + "\n".join(lines) + m.group(3), text)
        print(f"wrote {len(items)} packs into the {release} block", file=sys.stderr)
    with open(CATALOG, "w") as f:
        f.write(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", help="language the pack explains, by the app's code")
    parser.add_argument("--target", help="language the glosses are in")
    parser.add_argument("--pairs", help="comma-separated source-target pairs to build in one go")
    parser.add_argument("--wty-source", help="wty's code for the source language when it differs")
    parser.add_argument("--out", help="output file (default dictionaries/<source>-<target>.sqlite.gzip)")
    parser.add_argument("--min-entries", type=int, default=1000)
    parser.add_argument("--glossaries", help="comma-separated native-learning pairs to build word finders for, from wty's glossaries")
    parser.add_argument("--write-catalog", action="store_true", help="write the built packs into the app's catalog")
    parser.add_argument("--include", help="with --write-catalog, add only the built packs whose name matches this regex")
    args = parser.parse_args()
    if args.write_catalog:
        write_catalog(args.include)
        return
    if args.glossaries:
        for native, learning in (p.split("-", 1) for p in args.glossaries.split(",")):
            name = f"{native}-{learning}-gloss"
            record(name, build_glossary(native, learning, os.path.join(ROOT, "dictionaries", f"{name}.sqlite.gzip")))
        return
    pairs = [p.split("-", 1) for p in args.pairs.split(",")] if args.pairs else [(args.source, args.target)]
    if not all(s and t for s, t in pairs):
        parser.error("give --source and --target, or --pairs")
    sizes = defaultdict(int)
    for source, target in pairs:
        out = args.out if args.out and len(pairs) == 1 else os.path.join(ROOT, "dictionaries", f"{source}-{target}.sqlite.gzip")
        size = build(source, target, out, args.wty_source, args.min_entries)
        if not args.out:
            record(f"{source}-{target}", size)
        if size:
            sizes[f"{source}-{target}"] = size
    for name, size in sizes.items():
        print(f'"{name}" to {size:_}L,')


if __name__ == "__main__":
    main()
