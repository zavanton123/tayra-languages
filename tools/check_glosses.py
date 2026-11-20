#!/usr/bin/env python3
"""
Checks a supplement file of hand-written glosses (tools/glosses/<xx>-<target>.tsv, or --file): every
row is word, part of speech and gloss separated by tabs; the word is in the language's frequency
list, given once, and not already explained by the pack; the part of speech is one of POS; the
gloss's senses (separated by " ; ") are non-empty and at most MAX_SENSE characters.

Prints each problem with its line number and exits with status 1 when there is one.

Usage: python3 tools/check_glosses.py <xx> [--target en] [--file FILE] [--list FILE]
"""
import argparse
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import frequency_list, lowercase, read_supplement, supplement_path  # noqa: E402
from missing_glosses import explained, pack_file  # noqa: E402

POS = {"noun", "verb", "adj", "adv", "pron", "prep", "postp", "conj", "num", "particle", "intj", "phrase"}
MAX_SENSE = 80


def problems(source, target, path, list_path=None):
    listed = {word for word, _ in frequency_list(source, list_path)}
    if not listed:
        yield 0, f"no {source} frequency list"
    pack = pack_file(source, target)
    known = explained(pack, source) if pack else set()
    if pack and pack.startswith(tempfile.gettempdir()):
        os.remove(pack)
    seen = {}
    for number, word, pos, senses, fields in read_supplement(path):
        if fields != 3:
            yield number, f"{fields} tab-separated fields, want 3 (word, pos, gloss)"
            continue
        if not word:
            yield number, "empty word"
        elif listed and word not in listed:
            yield number, f"{word}: not in the {source} frequency list"
        if lowercase(word, source) in seen:
            yield number, f"{word}: already on line {seen[lowercase(word, source)]}"
        seen.setdefault(lowercase(word, source), number)
        if lowercase(word, source) in known:
            yield number, f"{word}: the pack already explains it (an entry or a form of one), leave it out"
        if pos not in POS:
            yield number, f"{word}: part of speech {pos!r} is not one of {', '.join(sorted(POS))}"
        for sense in senses:
            if not sense:
                yield number, f"{word}: empty sense (stray ' ; ' or empty gloss)"
            elif len(sense) > MAX_SENSE:
                yield number, f"{word}: sense longer than {MAX_SENSE} characters: {sense[:40]}..."
            elif ";" in sense:
                yield number, f"{word}: ';' inside a sense, separate senses with ' ; '"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", help="language code")
    parser.add_argument("--target", default="en", help="gloss language (default en)")
    parser.add_argument("--file", help="supplement file to check instead of tools/glosses/<xx>-<target>.tsv")
    parser.add_argument("--list", help="frequency list to read instead of the app's <xx>.tsv")
    args = parser.parse_args()
    path = args.file or supplement_path(args.source, args.target)
    if not os.path.exists(path):
        parser.error(f"no file {path}")
    found = list(problems(args.source, args.target, path, args.list))
    for number, message in found:
        print(f"{path}:{number}: {message}")
    rows = len(read_supplement(path))
    print(f"{rows} rows, {len(found)} problems", file=sys.stderr)
    sys.exit(1 if found else 0)


if __name__ == "__main__":
    main()
