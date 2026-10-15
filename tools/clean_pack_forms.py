#!/usr/bin/env python3
"""
Cleans the forms of the published packs the way tools/build_dictionary.py builds them now, without
building them again: the forms that only a table's auxiliary or the loose word of a compound tense
gave (the "auxiliary" forms of the English Wiktionary dumps, "haben" for every German verb, and the
single words of their "multiword-construction" ones, "ayant"), and the forms too many lemmas share (MAX_LEMMAS_PER_FORM: the pronouns of a
Latvian conjugation, a Vietnamese classifier, a case label).

Each pack of the catalog whose file in dictionaries/ is the published one (its size the catalog's)
is rewritten in place when it changes, with its new size written into the catalog and
dictionaries/wty/built.json; the changed packs are listed for uploading to their releases.

Usage: python3 tools/clean_pack_forms.py [--packs de-en,it-fr] [--dry-run]
"""
import argparse
import gzip
import json
import os
import re
import shutil
import sqlite3
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import (  # noqa: E402
    CLEANERS, SKIP_FORM_TAGS, SKIP_FORM_TAGS_BY_SOURCE, SOURCES, drop_shared_forms, is_clean_form, lowercase,
    open_text, serbian_spellings, skipped_form,
)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "core/domain/src/commonMain/kotlin/com/tayra/languages/core/domain/dictionary/DictionaryPackSizes.kt")
BUILT = os.path.join(ROOT, "dictionaries", "wty", "built.json")
# The skip tags the packs were built without; compound tenses' torn words are left out besides.
NEW_TAGS = {"auxiliary"}


def catalog():
    """The published packs: name to (release, size)."""
    text = open(CATALOG).read()
    packs = {}
    for release, block in re.findall(r'    "([\w.-]+)" to mapOf\(\n(.*?)\n    \),', text, re.S):
        for name, size in re.findall(r'"([a-z]+-[a-z]+(?:-gloss)?)" to ([\d_]+)L', block):
            packs[name] = (release, int(size.replace("_", "")))
    return packs


def tagged_forms(source):
    """(form_lc, lemma) pairs that only forms with the new skip tags gave the source's packs."""
    path = os.path.join(ROOT, SOURCES[source][2]) if source in SOURCES else None
    if not path or not os.path.exists(path):
        return set()
    cleaner = CLEANERS.get(source, lambda word: word)
    spellings = serbian_spellings if source == "sr" else (lambda word: [cleaner(word)])
    skipped_before = (SKIP_FORM_TAGS - NEW_TAGS) | SKIP_FORM_TAGS_BY_SOURCE.get(source, set())
    tagged, kept = set(), set()
    with open_text(path) as f:
        for line in f:
            d = json.loads(line)
            raw_word = d.get("word", "")
            for word in spellings(raw_word):
                for x in d.get("forms", []):
                    if set(x.get("tags", [])) & skipped_before:
                        continue
                    left_out = skipped_form(x, skipped_before | NEW_TAGS)
                    for form in spellings(x.get("form", "").strip()):
                        if is_clean_form(form, word):
                            (tagged if left_out else kept).add((lowercase(form, source), word))
            for s in d.get("senses", []):
                for x in s.get("form_of", []):
                    for lemma in spellings(x.get("word", "")):
                        for word in spellings(raw_word):
                            kept.add((lowercase(word, source), lemma))
    return tagged - kept


def clean(name, noise_of, dry_run):
    """Cleans one pack; returns its new size when it changed, else None."""
    packed = os.path.join(ROOT, "dictionaries", f"{name}.sqlite.gzip")
    work = packed + ".cleaning"
    with gzip.open(packed, "rb") as src, open(work, "wb") as dst:
        shutil.copyfileobj(src, dst)
    db = sqlite3.connect(work)
    try:
        source = db.execute("SELECT value FROM meta WHERE key = 'source_language'").fetchone()[0]
        noise = noise_of(source)
        tagged = 0
        if noise:
            pairs = [row for row in db.execute("SELECT form_lc, lemma FROM forms") if row in noise]
            db.executemany("DELETE FROM forms WHERE form_lc = ? AND lemma = ?", pairs)
            tagged = len(pairs)
        shared = drop_shared_forms(db, source)
        if tagged + shared == 0:
            return None
        count = db.execute("SELECT count(*) FROM forms").fetchone()[0]
        print(f"{name}: {tagged:,} auxiliary or compound-tense forms, {shared:,} shared forms removed; {count:,} left", file=sys.stderr)
        if dry_run:
            return None
        db.execute("UPDATE meta SET value = ? WHERE key = 'form_count'", (str(count),))
        db.commit()
        db.execute("VACUUM")
    finally:
        db.close()
    plain = os.path.join(ROOT, "dictionaries", f"{name}.sqlite")
    with open(work, "rb") as src, gzip.open(packed + ".new", "wb", compresslevel=9) as dst:
        shutil.copyfileobj(src, dst)
    os.replace(packed + ".new", packed)
    if os.path.exists(plain):
        os.replace(work, plain)
    return os.path.getsize(packed)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--packs", help="comma-separated pack names; default: every published pack")
    parser.add_argument("--dry-run", action="store_true", help="report what would be removed, change nothing")
    args = parser.parse_args()
    published = catalog()
    names = args.packs.split(",") if args.packs else sorted(published)
    noise_cache = {}

    def noise_of(source):
        if source not in noise_cache:
            noise_cache[source] = tagged_forms(source)
        return noise_cache[source]

    changed = {}
    for name in names:
        release, size = published[name]
        packed = os.path.join(ROOT, "dictionaries", f"{name}.sqlite.gzip")
        if not os.path.exists(packed) or os.path.getsize(packed) != size:
            print(f"{name}: skipped, the file here is not the published one", file=sys.stderr)
            continue
        try:
            new_size = clean(name, noise_of, args.dry_run)
        finally:
            if os.path.exists(packed + ".cleaning"):
                os.remove(packed + ".cleaning")
        if new_size is not None:
            changed[name] = (release, new_size)
    if not changed:
        return
    text = open(CATALOG).read()
    for name, (_, new_size) in changed.items():
        text = re.sub(rf'"{re.escape(name)}" to [\d_]+L', f'"{name}" to {new_size:_}L', text)
    open(CATALOG, "w").write(text)
    built = json.load(open(BUILT))
    built.update({name: new_size for name, (_, new_size) in changed.items() if name in built})
    with open(BUILT, "w") as f:
        json.dump(built, f, indent=1, sort_keys=True)
    for release in sorted({release for release, _ in changed.values()}):
        print(f"{release}: " + " ".join(f"dictionaries/{name}.sqlite.gzip" for name, (r, _) in sorted(changed.items()) if r == release))


if __name__ == "__main__":
    main()
