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


def tokens(text, code=None, ranks=None, names=frozenset(), verbs=None):
    if code == "fr":
        return french_tokens(text, ranks)
    if code == "es":
        return spanish_tokens(text, ranks, names, verbs)
    out = []
    for match in WORD.finditer(text):
        out.extend(part for part in re.split(r"-", match.group(0)) if part)
    return out


ORDINAL_ENDINGS = {"e", "er", "re", "ère", "es", "ers", "res", "ème", "èmes", "nd", "nde", "nds", "ndes"}
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
        # The ending of an ordinal (20e, 1er, 2nde) belongs to its number, which is not counted.
        if match.start() > 0 and text[match.start() - 1].isdigit() and word.lower() in ORDINAL_ENDINGS:
            continue
        if word.lower() in ranks:
            out.append(word)
            continue
        for part in word.split("-"):
            # Elided words come off the front one at a time, so d'aujourd'hui is d + aujourd'hui.
            while part and part.lower() not in ranks and "'" in part:
                elided, _, part = part.partition("'")
                if elided:
                    out.append(elided)
            if part:
                out.extend([part] if part.lower() in ranks else [piece for piece in part.split("'") if piece])
    return out


# Object pronouns a Spanish verb can carry at its end, longest first so "les" goes before "le".
SPANISH_CLITICS = ("los", "las", "les", "nos", "os", "lo", "la", "le", "me", "te", "se")
UNACCENTED = str.maketrans("áéíóú", "aeiou")


def spanish_verb_forms(words):
    """Every form of the list's verbs, the words whose headword is an infinitive (arrepentirse too)."""
    return {form for key, (_, _, forms) in words.items() if key.endswith(("ar", "er", "ir", "ír", "arse", "erse", "irse", "írse")) for form in [key] + forms}


def spanish_imperative_infinitives(form):
    """
    The infinitives an imperative (or a usted/ustedes form) may come from: pregunta, pregunte and
    pregunten from preguntar, vive from vivir, and with the stem change undone, cuenta from contar,
    piensa from pensar, pide from pedir.
    """
    bare = form[:-1] if form.endswith("n") else form
    stems = []
    if bare.endswith("a"):
        stems += [bare[:-1] + "ar", bare[:-1] + "er", bare[:-1] + "ir"]
    if bare.endswith("e"):
        stems += [bare[:-1] + "er", bare[:-1] + "ir", bare[:-1] + "ar"]
    out = set(stems)
    for infinitive in stems:
        root, ending = infinitive[:-2], infinitive[-2:]
        for changed, plain in (("ue", "o"), ("ie", "e"), ("i", "e")):
            at = root.rfind(changed)
            if at >= 0:
                out.add(root[:at] + plain + root[at + len(changed):] + ending)
    return out


def spanish_clitics(word, ranks, verbs=None):
    """
    A verb carrying pronouns split into the verb and them, as a learner reads it: levantarse is
    levantar + se, dándoselo is dando + se + lo, dímelo is di + me + lo. The written accent such a
    form needs comes off the verb. None when the word does not split that way.
    """
    lower = word.lower()
    for first in SPANISH_CLITICS:
        if not lower.endswith(first) or len(lower) <= len(first) + 1:
            continue
        rest = lower[: -len(first)]
        for second in ("",) + SPANISH_CLITICS:
            if second and (not rest.endswith(second) or len(rest) <= len(second) + 1):
                continue
            stem = rest[: len(rest) - len(second)] if second else rest
            for candidate in (stem, stem.translate(UNACCENTED)):
                # Only forms of verbs carry pronouns (an infinitive, gerund or imperative), so chiles
                # is not chi + les; without the list's verbs, the ending decides.
                is_verb = candidate in verbs if verbs is not None else candidate.endswith(("ar", "er", "ir", "ndo", "a", "e", "i", "n", "z", "d"))
                # The list may file a verb's imperative as a noun (pregunta), yet with pronouns and
                # its written accent (pregúntales) it can only be the verb's.
                if verbs is not None and not is_verb and any(c in "áéíóú" for c in lower):
                    is_verb = any(infinitive in verbs for infinitive in spanish_imperative_infinitives(candidate))
                # Pronouns after any form but an infinitive call for a written accent (tómate,
                # dímelo), save one pronoun after a one-syllable form (ponte, dime); so an unaccented
                # tomate is the noun, not toma + te.
                # The vosotros imperative takes them unaccented, and drops its d before os: habladlo,
                # sentaos.
                if verbs is not None and not is_verb and first == "os" and not second and candidate + "d" in verbs:
                    is_verb = True
                elif is_verb and not candidate.endswith(("ar", "er", "ir", "ír", "d")) and not any(c in "áéíóú" for c in lower):
                    is_verb = not second and len(re.findall("[aeiouáéíóúü]+", candidate)) == 1
                if candidate in ranks and is_verb:
                    return [candidate] + ([second] if second else []) + [first]
    return None


def spanish_tokens(text, ranks, names=frozenset(), verbs=None):
    """Spanish words split at hyphens, and a verb's attached pronouns counted as words of their own."""
    out = []
    for match in WORD.finditer(text):
        for part in re.split(r"-", match.group(0)):
            if not part:
                continue
            # A name is never a verb with pronouns, though Chile ends like one.
            split = None if part.lower() in ranks or part.lower() in names else spanish_clitics(part, ranks, verbs)
            out.extend(split or [part])
    return out


def check(path, brief=False):
    course = json.load(open(path, encoding="utf-8"))
    code = course["id"].split("-")[0]
    ranks, words = load_list(code)
    verbs = spanish_verb_forms(words) if code == "es" else None
    up_to = course["rankUpTo"]
    band_start = up_to - BAND + 1
    need, shortest, longest = rules(up_to)
    names = {fold(code, n.lower()) for n in course.get("names", [])}
    # In Spanish a name of several words (San José, La Habana) counts as one known word wherever it
    # is written whole; the earlier languages count such names word by word, as they were written to.
    long_names = sorted((n for n in course.get("names", []) if " " in n), key=len, reverse=True) if code == "es" else []
    seen_new = set()
    ok = True
    report = []
    for i, lesson in enumerate(course["lessons"], start=1):
        text = lesson["text"]
        whole_names = 0
        for name in long_names:
            whole_names += text.count(name)
            text = text.replace(name, " ")
        toks = tokens(text, code, ranks, names, verbs)
        lowered = [fold(code, t.lower()) for t in toks] + ["\u0000name"] * whole_names
        names_here = names | {"\u0000name"}
        outside = {}
        known = 0
        for t in lowered:
            # An English possessive ("emma's") counts as its word: the list has few of them.
            if t.endswith(("'s", "’s")) and t not in ranks and len(t) > 2:
                t = t[:-2]
            if t in names_here:
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
