#!/usr/bin/env python3
"""
Lists the frequency-list words of a language that its dictionary pack cannot explain: no entry of
the dumps under the word, and no form row pointing it at a lemma that has one. Words the supplement
file (tools/glosses/<source>-<target>.tsv) already glosses are left out, so gloss writers can work
through the list in rank batches without rebuilding the pack in between.

Prints rank, word and a few of the word's corpus forms, tab-separated, in rank order.

Usage: python3 tools/missing_glosses.py <xx> [--target en] [--limit N] [--from-rank R] [--pack FILE] [--list FILE]
       python3 tools/missing_glosses.py --counts [xx,yy,...]
"""
import argparse
import gzip
import os
import shutil
import sqlite3
import sys
import tempfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import MAX_LEMMAS_PER_FORM, frequency_list, lowercase, read_supplement, supplement_path  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# The languages whose packs the 2026-11 gloss work covers.
GLOSS_LANGUAGES = "ne,kk,af,kn,ta,te,pa,ur,bn,sw,az,yi,mt,gv,kw,haw,br,lo,km,mn,eu,be,sq,lv,vi,grc"
SHOWN_FORMS = 5


def pack_file(source, target, path=None):
    """The pack's SQLite file: [path], else dictionaries/<source>-<target>.sqlite (unpacked from .gzip when needed); None when absent."""
    path = path or os.path.join(ROOT, "dictionaries", f"{source}-{target}.sqlite")
    if path.endswith(".gzip") or not os.path.exists(path):
        packed = path if path.endswith(".gzip") else path + ".gzip"
        if not os.path.exists(packed):
            return None
        out = tempfile.NamedTemporaryFile(suffix=".sqlite", delete=False)
        with gzip.open(packed, "rb") as f, out:
            shutil.copyfileobj(f, out)
        return out.name
    return path


def explained(path, source):
    """The lowercase words the pack's own entries explain, directly or as a form of a lemma with an entry, supplement entries left out."""
    db = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    supplement = int((db.execute("SELECT value FROM meta WHERE key = 'supplement_entries'").fetchone() or ["0"])[0])
    last = (db.execute("SELECT max(id) FROM entries").fetchone()[0] or 0) - supplement
    words, word_lcs = set(), set()
    for word, word_lc in db.execute("SELECT word, word_lc FROM entries WHERE id <= ?", (last,)):
        words.add(word)
        word_lcs.add(word_lc)
    lemmas = defaultdict(set)
    for form_lc, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemmas[form_lc].add(lemma)
    db.close()
    # The app ignores a form that too many lemmas share (DictionaryService.MAX_LEMMAS_PER_FORM).
    return word_lcs | {form for form, found in lemmas.items() if len(found) <= MAX_LEMMAS_PER_FORM and found & words}


def missing(source, target="en", pack=None, list_path=None):
    """(rank, word, forms) of the list's words the pack does not explain and the supplement does not gloss; None without a pack or list."""
    path = pack_file(source, target, pack)
    ranked = frequency_list(source, list_path)
    if path is None or not ranked:
        return None
    known = explained(path, source)
    if path.startswith(tempfile.gettempdir()):
        os.remove(path)
    glossed = supplement_path(source, target)
    if os.path.exists(glossed):
        known |= {lowercase(word, source) for _, word, _, _, _ in read_supplement(glossed)}
    return [(rank, word, [f for f in forms if f != word][:SHOWN_FORMS])
            for rank, (word, forms) in enumerate(ranked, 1) if lowercase(word, source) not in known]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", nargs="?", help="language code of the frequency list and pack")
    parser.add_argument("--target", default="en", help="gloss language of the pack (default en)")
    parser.add_argument("--limit", type=int, help="print at most this many words")
    parser.add_argument("--from-rank", type=int, default=1, help="start at this rank of the list")
    parser.add_argument("--pack", help="pack file to check instead of dictionaries/<xx>-<target>.sqlite")
    parser.add_argument("--list", help="frequency list to read instead of the app's <xx>.tsv")
    parser.add_argument("--counts", nargs="?", const=GLOSS_LANGUAGES, help="print how many words are missing per language")
    args = parser.parse_args()
    if args.counts:
        print("language\tlist words\tmissing")
        for source in args.counts.split(","):
            found = missing(source, args.target)
            if found is None:
                print(f"{source}\t-\tno pack or list", file=sys.stderr)
                continue
            print(f"{source}\t{len(frequency_list(source))}\t{len(found)}")
        return
    if not args.source:
        parser.error("give a language code, or --counts")
    found = missing(args.source, args.target, args.pack, args.list)
    if found is None:
        parser.error(f"no {args.source}-{args.target} pack or no {args.source} frequency list")
    rows = [row for row in found if row[0] >= args.from_rank][: args.limit]
    for rank, word, forms in rows:
        print(f"{rank}\t{word}\t{' '.join(forms)}")
    print(f"{len(rows)} shown, {len(found)} missing in all", file=sys.stderr)


if __name__ == "__main__":
    main()
