#!/usr/bin/env python3
"""
Checks a frequency-list course (tools/courses/<code>/<id>.json) against the language's word
frequency list, lesson by lesson, so generated lessons stay readable at their level:

- coverage: the share of a lesson's words ranked within the course's rankUpTo (names in the
  course's "names", and numbers, count as known); words outside are listed with their ranks;
- new words: each is a word of the course's band (ranks rankUpTo-99 .. rankUpTo), new in the
  course, and used at least twice in its lesson, counting its forms;
- length: words per lesson within the course's range.

Usage: python3 tools/check_course.py tools/courses/pt/pt-mini-0100.json [--brief]
Exit status 0 when every lesson passes.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LISTS = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency")
WORD = re.compile(r"[^\W\d_]+(?:['’][^\W\d_]+)*", re.UNICODE)
BAND = 100


def rules(rank_up_to):
    """Coverage and length a lesson needs: the first courses have few words to work with."""
    if rank_up_to <= 100:
        return 0.90, 80, 180
    if rank_up_to <= 300:
        return 0.93, 120, 240
    if rank_up_to <= 600:
        return 0.95, 180, 320
    if rank_up_to <= 1000:
        return 0.95, 220, 380
    if rank_up_to <= 2000:
        return 0.95, 260, 450
    if rank_up_to <= 3000:
        return 0.95, 300, 520
    if rank_up_to <= 4000:
        return 0.95, 320, 550
    return 0.95, 350, 600


def fold(code, word):
    # The German list folds ß to ss in most of its forms, so both spellings are compared folded.
    if code == "de":
        return word.replace("ß", "ss")
    # The French list writes the straight apostrophe; texts may use the curly one.
    if code == "fr":
        return word.replace("’", "'")
    return word


def load_list(code):
    ranks, words = {}, {}
    # A language's courses may pin the list they were written against, so a rebuilt app list
    # (which can reorder ranks) does not move their bands.
    pinned = os.path.join(ROOT, "tools", "courses", code, "wordlist.tsv")
    path = pinned if os.path.exists(pinned) else os.path.join(LISTS, f"{code}.tsv")
    with open(path, encoding="utf-8") as f:
        rank = 0
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            rank += 1
            word, _, forms = line.rstrip("\n").partition("\t")
            key = fold(code, word.lower())
            forms = [fold(code, form) for form in forms.split()]
            words[key] = (rank, word, forms)
            for form in [key] + forms:
                ranks[form] = min(ranks.get(form, rank), rank)
    # Forms the list misses, kept per language next to its courses (tools/courses/<code>/extra_forms.tsv).
    extra = os.path.join(ROOT, "tools", "courses", code, "extra_forms.tsv")
    if os.path.exists(extra):
        for line in open(extra, encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            word, _, forms = line.rstrip("\n").partition("\t")
            key = fold(code, word.lower())
            rank, headword, known = words[key]
            added = [fold(code, form) for form in forms.split()]
            words[key] = (rank, headword, known + [f for f in added if f not in known])
            for form in added:
                ranks[form] = min(ranks.get(form, rank), rank)
    return ranks, words


def tokens(text, code=None, ranks=None):
    if code == "fr":
        return french_tokens(text, ranks)
    out = []
    for match in WORD.finditer(text):
        out.extend(part for part in re.split(r"-", match.group(0)) if part)
    return out


FRENCH_WORD = re.compile(r"[^\W\d_]+(?:['’-][^\W\d_]+)*", re.UNICODE)


def french_tokens(text, ranks):
    """
    French words as the reader counts them, split at hyphens and apostrophes (l'homme is l +
    homme, est-ce is est + ce), except the words the list has whole (aujourd'hui, jusqu'à,
    quelqu'un, week-end), which a learner knows as one word.
    """
    out = []
    for match in FRENCH_WORD.finditer(text):
        word = match.group(0).replace("’", "'")
        if word.lower() in ranks:
            out.append(word)
            continue
        for part in word.split("-"):
            if part.lower() in ranks:
                out.append(part)
            else:
                out.extend(piece for piece in part.split("'") if piece)
    return out


def check(path, brief=False):
    course = json.load(open(path, encoding="utf-8"))
    code = course["id"].split("-")[0]
    ranks, words = load_list(code)
    up_to = course["rankUpTo"]
    band_start = up_to - BAND + 1
    need, shortest, longest = rules(up_to)
    names = {fold(code, n.lower()) for n in course.get("names", [])}
    seen_new = set()
    ok = True
    report = []
    for i, lesson in enumerate(course["lessons"], start=1):
        toks = tokens(lesson["text"], code, ranks)
        lowered = [fold(code, t.lower()) for t in toks]
        outside = {}
        known = 0
        for t in lowered:
            # An English possessive ("emma's") counts as its word: the list has few of them.
            if t.endswith(("'s", "’s")) and t not in ranks and len(t) > 2:
                t = t[:-2]
            if t in names:
                known += 1
                continue
            rank = ranks.get(t)
            if rank is not None and rank <= up_to:
                known += 1
            else:
                outside[t] = rank
        coverage = known / max(1, len(lowered))
        problems = []
        if coverage < need:
            problems.append(f"coverage {coverage:.1%} < {need:.0%}")
        if not shortest <= len(lowered) <= longest:
            problems.append(f"{len(lowered)} words, needs {shortest}-{longest}")
        for new in lesson.get("newWords", []):
            key = fold(code, new.lower())
            entry = words.get(key)
            if entry is None:
                problems.append(f"new word '{new}' is not in the list")
                continue
            rank, _, forms = entry
            if not band_start <= rank <= up_to:
                problems.append(f"new word '{new}' is rank {rank}, outside {band_start}-{up_to}")
            if key in seen_new:
                problems.append(f"new word '{new}' was introduced in an earlier lesson")
            uses = sum(1 for t in lowered if t == key or t in forms)
            if uses < 2:
                problems.append(f"new word '{new}' used {uses}x, needs 2+")
            seen_new.add(key)
        status = "PASS" if not problems else "FAIL"
        ok = ok and not problems
        line = f"lesson {i:2} {status}  {len(lowered)} words, coverage {coverage:.1%}, {len(lesson.get('newWords', []))} new"
        report.append(line)
        for p in problems:
            report.append("    - " + p)
        if outside and (problems or not brief):
            listed = sorted(outside.items(), key=lambda kv: (kv[1] is None, kv[1] or 0))
            report.append("    outside: " + ", ".join(f"{w}({r if r else 'not in list'})" for w, r in listed))
    band = [w for w, (r, _, _) in words.items() if band_start <= r <= up_to]
    missing = [words[w][1] for w in band if w not in seen_new]
    report.append(f"band {band_start}-{up_to}: {len(seen_new)} new words taught; not taught ({len(missing)}): {', '.join(missing)}")
    print("\n".join(report))
    return ok


if __name__ == "__main__":
    sys.exit(0 if check(sys.argv[1], brief="--brief" in sys.argv) else 1)
