"""
Writes the app's Uzbek list (files/frequency/uz.tsv) from the pinned one (tools/courses/uz/wordlist.tsv): the same words in
the same order, without most of the core forms no text has (frequency_uzbek.app_list). Run it after the pinned list changes
and before a merge that adds Uzbek courses, so the forms they use stay listed.

    python3 tools/trim_uzbek_list.py

It counts the corpora as build_frequency_list does (a few minutes); UZ_COUNTS names a pickle of those counts to use instead.
"""
import glob
import json
import os
import sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
import build_frequency_list as b  # noqa: E402
import frequency_uzbek as u  # noqa: E402

COURSES = os.path.join(b.ROOT, "tools", "courses", "uz")


def counts():
    """The counts the list's ranks are made from (frequency_uzbek.SEEN), as build_frequency_list makes them."""
    cached = os.environ.get("UZ_COUNTS")
    if cached:
        import pickle
        got = pickle.load(open(cached, "rb"))
        for source in b.SOURCE_ORDER["uz"]:
            b.SOURCES[source] = (lambda r: (lambda language, limit: r))(got[source])
    b.build("uz", None, 10_600)
    return u.SEEN


def course_words():
    """Every word the Uzbek courses write, and the forms extra_forms.tsv adds."""
    used = set()

    def walk(value):
        if isinstance(value, str):
            used.update(w.lower() for w in u.words(value))
        elif isinstance(value, list):
            for item in value:
                walk(item)
        elif isinstance(value, dict):
            for item in value.values():
                walk(item)
    for path in sorted(glob.glob(os.path.join(COURSES, "*.json"))):
        walk(json.load(open(path, encoding="utf-8")))
    for line in open(os.path.join(COURSES, "extra_forms.tsv"), encoding="utf-8"):
        if not line.startswith("#"):
            used.update(f.lower() for f in line.split())
    return used


def main():
    lines = open(os.path.join(COURSES, "wordlist.tsv"), encoding="utf-8").read().splitlines()
    out = u.app_list(lines, counts(), course_words())
    path = os.path.join(b.OUT_DIR, "uz.tsv")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")
    before = sum(len(line.split("\t")[1].split()) for line in lines if not line.startswith("#") and "\t" in line)
    after = sum(len(line.split("\t")[1].split()) for line in out if not line.startswith("#") and "\t" in line)
    print(f"wrote {path}: {after} forms of {before}, {os.path.getsize(path):,} bytes")


if __name__ == "__main__":
    main()
