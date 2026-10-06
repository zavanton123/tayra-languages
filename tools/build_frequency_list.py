#!/usr/bin/env python3
"""
Builds a word frequency list for the app: the most common words of a language, as dictionary
words ("dizer"), each with the forms counted for it ("diz", "disse", ...).

Word counts come from the first source that has the language:

  1. wordfreq (pip install wordfreq), a blend of subtitles, Wikipedia, books, news and web text
  2. FrequencyWords (hermitdave/FrequencyWords), counts from OpenSubtitles 2018
  3. the Leipzig Corpora Collection, counts from a news or Wikipedia corpus

--source forces one of them. The counts are of word forms; each form is credited to the dictionary
words it is a form of, using the forms table of the language's offline dictionary pack
(dictionaries/<code>-en.sqlite, made by tools/build_dictionary.py), or for a form the pack does
not have, the simplemma lemmatizer (pip install simplemma), whose names are capitalised and left out. A form that is also a word in
its own right ("casa", the noun, and a form of "casar") keeps its count unless the other word is
more common than the form itself ("era" goes to "ser"); a form of several words ("foi", of "ir"
and "ser") is shared between them by how common they are. Forms that are not in the dictionary
(names, foreign words, numbers) and words that are only names, letters or affixes are left out.

The output is a text file with one word per line, most common first:

  # source: wordfreq 3.1 (CC BY-SA 4.0)
  dizer<TAB>diz disse dizer dizendo ...

The word is written as the dictionary writes it ("Haus" in German); its forms are lowercase.

Usage: python3 tools/build_frequency_list.py --language pt|all [--source wordfreq|frequencywords|leipzig]
       [--size 10000] [--out FILE]
"""
import argparse
import io
import os
import sqlite3
import sys
import subprocess
import tarfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import to_serbian_cyrillic  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")
CACHE = os.path.join(DICTIONARIES, "frequency-sources")
OUT_DIR = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency")

# The languages the app teaches, by the codes it files them under.
LANGUAGES = [
    "be", "bg", "ca", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "gl", "hr", "hu", "is", "it",
    "la", "lt", "lv", "mk", "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sr", "sv", "tr", "uk",
]

# wordfreq's code where it differs from the app's. wordfreq also answers for a language it lacks
# with its nearest one (Russian for Belarusian, Serbo-Croatian for Croatian), which is not wanted.
WORDFREQ_CODES = {"no": "nb"}

# Per language: the FrequencyWords folder and the Leipzig corpora to fall back on. Croatian and
# Serbian, which wordfreq only has together, Estonian and Galician come from FrequencyWords;
# Belarusian and Latin, which neither of the others has, from Leipzig.
FALLBACKS = {
    "pt": {"frequencywords": "pt_br", "leipzig": ["por_news_2020_100K"]},
    "be": {"leipzig": ["bel_news_2020_100K", "bel_newscrawl_2017_300K", "bel_wikipedia_2021_300K"]},
    "et": {"leipzig": ["est_news_2020_300K"]},
    "la": {"leipzig": ["lat_wikipedia_2021_100K"]},
    "lt": {"leipzig": ["lit_news_2020_300K"]},
    "lv": {"leipzig": ["lav_news_2020_300K"]},
}

# A first source with fewer word forms than this (wordfreq keeps only its short list for some
# languages) is blended with every other source that has the language.
ENOUGH_FORMS = 100_000

SOURCE_LICENCES = {
    "wordfreq": "CC BY-SA 4.0",
    "frequencywords": "CC BY-SA 4.0",
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
}

# Languages that capitalise their nouns, so a capitalised entry is not taken for a name.
CAPITALISED_NOUNS = {"de"}

# simplemma's code where it differs from the app's; it has no Belarusian.
SIMPLEMMA_CODES = {"hr": "hbs", "sr": "hbs", "no": "nb"}

# Parts of speech a frequency list shows; names, letters, affixes and set phrases are left out.
WORD_POS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "article", "intj", "contraction", "particle"}

# Per-language corrections, reviewed by hand.
from frequency_fixes import DISPLAY, EXCLUDE, OVERRIDES  # noqa: E402

# How common another word must be, against the form itself, to take a form that is also a word:
# a more common word ("era" to "ser"), or a less common one of a kind that inflects for
# gender and number ("todos" to "todo", "nova" to "novo").
TAKEOVER = 1.0
INFLECTED_KINDS = {"adj", "pron", "det", "article", "num"}
INFLECTED_TAKEOVER = 0.2
# A form the dictionary ties to more words than this is a helper word copied into their
# inflection tables (German "haben" in every verb's perfect, "ein" in every separable verb's)
# rather than a form of any of them, and is counted as itself.
MAX_LEMMAS_PER_FORM = 6
# Forms considered per word, most common first; the app matches saved words against them.
FORMS_PER_WORD = 40


def english_share(form):
    """How common a form is in English, as a share of all English words, or 0 without wordfreq."""
    try:
        import wordfreq
    except ImportError:
        return 0.0
    return wordfreq.word_frequency(form, "en")


def lower(language, text):
    """Lowercase as the language does: Turkish dotted and dotless i keep apart."""
    if language == "tr":
        text = text.replace("I", "ı").replace("İ", "i")
    return text.lower()


def wordfreq_counts(language, limit):
    import wordfreq
    language = WORDFREQ_CODES.get(language, language)
    if language not in wordfreq.available_languages("best"):
        return None
    words = wordfreq.top_n_list(language, limit, wordlist="best")
    from importlib.metadata import version as installed
    version = installed("wordfreq")
    counts = {}
    for w in words:
        # wordfreq folds Greek final ς to σ ("τησ"); the dictionary and texts write ς.
        key = w[:-1] + "ς" if language == "el" and w.endswith("σ") else w
        counts[key] = counts.get(key, 0) + wordfreq.word_frequency(w, language, wordlist="best")
    return counts, f"wordfreq {version}".strip()


def download(url, name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print(f"Downloading {url}", file=sys.stderr)
        # curl uses the system's certificates, which a python.org Python on macOS does not.
        result = subprocess.run(["curl", "-fsSL", "-o", path + ".part", "-w", "%{http_code}", url], capture_output=True, text=True)
        if result.stdout.strip() == "404":
            return None
        if result.returncode != 0:
            sys.exit(f"Could not download {url}: {result.stderr.strip()}")
        os.rename(path + ".part", path)
    return path


def frequencywords_counts(language, limit):
    folder = FALLBACKS.get(language, {}).get("frequencywords", language)
    path = download(f"https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/{folder}/{folder}_full.txt", f"frequencywords-{folder}.txt")
    if path is None:
        return None
    counts = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) == 2:
                word = lower(language, parts[0])
                counts[word] = counts.get(word, 0) + int(parts[1])
            if len(counts) >= limit:
                break
    return counts, f"FrequencyWords {folder} (OpenSubtitles 2018)"


def leipzig_counts(language, limit):
    corpora = FALLBACKS.get(language, {}).get("leipzig", [])
    counts = {}
    used = []
    for corpus in corpora:
        path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
        if path is None:
            continue
        used.append(corpus)
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[2].isdigit():
                    word = lower(language, parts[1])
                    counts[word] = counts.get(word, 0) + int(parts[2])
    if not used:
        return None
    top = sorted(counts.items(), key=lambda kv: -kv[1])[:limit]
    return dict(top), f"Leipzig Corpora Collection {', '.join(used)}"


SOURCES = {"wordfreq": wordfreq_counts, "frequencywords": frequencywords_counts, "leipzig": leipzig_counts}


class Lemmatizer:
    def __init__(self, code):
        import simplemma
        self.simplemma = simplemma
        self.code = code

    def is_known(self, form):
        return self.simplemma.is_known(form, lang=self.code)

    def lemmatize(self, form):
        return self.simplemma.lemmatize(form, lang=self.code)


def simplemma_for(language):
    """simplemma for the language, or None when it is not installed or lacks the language."""
    try:
        lemmatizer = Lemmatizer(SIMPLEMMA_CODES.get(language, language))
        lemmatizer.is_known("a")
        return lemmatizer
    except (ImportError, ValueError):
        return None


def load_dictionary(language):
    path = os.path.join(DICTIONARIES, f"{language}-en.sqlite")
    if not os.path.exists(path):
        sys.exit(f"{path} is missing; build it with: python3 tools/build_dictionary.py --source {language} --target en")
    db = sqlite3.connect(path)
    pos = defaultdict(set)
    # Words whose common-word entries are all capitalised ("China", the noun) are names too,
    # except the nouns of a language that capitalises them; those keep their capital ("Haus").
    common = {}
    for word, word_lc, p in db.execute("SELECT word, word_lc, pos FROM entries"):
        pos[word_lc].add(p)
        if p not in WORD_POS:
            continue
        if word == word_lc:
            common[word_lc] = word_lc
        elif language in CAPITALISED_NOUNS and p == "noun" and lower(language, word) == word_lc:
            common.setdefault(word_lc, word)
    # A noun the dictionary also lists as a rare lowercase word ("zeit") keeps its capital.
    common.update(DISPLAY.get(language, {}))
    lemmas = defaultdict(list)
    for form_lc, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemma = lower(language, lemma)
        if all(c.isalpha() or c in "-'" for c in lemma):
            lemmas[form_lc].append(lemma)
    for form_lc, of in lemmas.items():
        if len(set(of)) > MAX_LEMMAS_PER_FORM:
            lemmas[form_lc] = []
    return pos, lemmas, common


def build(language, source, size):
    order = [source] if source else ["wordfreq", "frequencywords", "leipzig"]
    found = []
    for name in order:
        result = SOURCES[name](language, 200_000)
        if result:
            found.append((name, *result))
            if len(found) == 1 and len(result[0]) >= ENOUGH_FORMS:
                break
    if not found:
        sys.exit(f"No frequency source has {language}")
    # Each source's share of its own total, averaged; a form a source does not list counts as 0 there.
    freq = defaultdict(float)
    for _, counts, _ in found:
        total = sum(counts.values())
        for w, c in counts.items():
            freq[w] += c / total / len(found)
    label = " + ".join(f"{label} ({SOURCE_LICENCES[name]})" for name, _, label in found)
    pos, lemmas, common = load_dictionary(language)
    overrides = OVERRIDES.get(language, {})
    excluded = EXCLUDE.get(language, set())

    # The words simplemma found for forms the dictionary lacks, as they are written.
    guessed = {}
    lemmatizer = simplemma_for(language)

    def guess(form):
        """simplemma's word for a form the dictionary does not have, or None for a name or an unknown form."""
        if lemmatizer is None or len(form) < 2 or not all(c.isalpha() or c in "-'" for c in form) or not lemmatizer.is_known(form):
            return None
        # Web text in every language is full of English, and simplemma knows English loanwords.
        if language != "en" and english_share(form) >= freq.get(form, 0):
            return None
        lemma = lemmatizer.lemmatize(form)
        key = lower(language, lemma)
        if (lemma != key and language not in CAPITALISED_NOUNS) or (key in pos and key not in common) or key in excluded:
            return None
        guessed.setdefault(key, lemma)
        return key

    def is_word(w):
        if w in guessed and w not in pos:
            return True
        kinds = pos.get(w, set()) & WORD_POS
        if not kinds or w not in common or w in excluded:
            return False
        # Single letters are kept only as the little words they can be ("a", "e", "o").
        return len(w) > 1 or bool(kinds & {"prep", "conj", "article", "pron", "det"})

    def direct(form):
        """The words a form's count goes to, with their shares, before following words that are forms themselves."""
        if form in overrides:
            others = overrides[form]
            if others == [form]:
                return {form: 1.0}
        else:
            if len(form) == 1 and not is_word(form):
                return {}
            if form not in lemmas and form not in pos:
                word = guess(form)
                return {word: 1.0} if word else {}
            others = list(dict.fromkeys(l for l in lemmas.get(form, []) if l != form and is_word(l)))
        # A form whose only entry is an interjection ("foi", "nossa") is counted for the word it is a form of.
        if is_word(form) and form not in overrides and not (others and pos[form] & WORD_POS <= {"intj"}):
            # A form that is a word itself keeps its count ("casa", not "casar") unless [TAKEOVER] says otherwise.
            inflected = bool(pos[form] & INFLECTED_KINDS)
            mine = freq.get(form, 0)
            others = [
                l for l in others
                if freq.get(l, 0) >= TAKEOVER * mine or (inflected and pos[l] & INFLECTED_KINDS and freq.get(l, 0) >= INFLECTED_TAKEOVER * mine)
            ]
            if not others:
                return {form: 1.0}
        if not others:
            return {}
        weights = {l: freq.get(l, 0) or 1e-12 for l in others}
        whole = sum(weights.values())
        return {l: w / whole for l, w in weights.items()}

    resolved = {}

    def targets_of(form, depth=0):
        """[direct], following a word that is itself a form of another ("essas" to "essa" to "esse")."""
        if form in resolved:
            return resolved[form]
        result = defaultdict(float)
        for word, part in direct(form).items():
            if word != form and depth < 3:
                onward = targets_of(word, depth + 1)
                if onward and word not in onward:
                    for final, share in onward.items():
                        result[final] += part * share
                    continue
            result[word] += part
        resolved[form] = dict(result)
        return resolved[form]

    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, share in list(freq.items()):
        if form in excluded:
            continue
        targets = targets_of(form)
        for lemma, part in targets.items():
            scores[lemma] += share * part
            forms[lemma][form] = forms[lemma].get(form, 0) + share * part

    ranked = sorted(scores, key=lambda w: -scores[w])[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        counted = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_PER_WORD]
        if language == "sr":
            # The counts are of Latin-script text; Serbian is read in Cyrillic too.
            counted = list(dict.fromkeys(counted + [to_serbian_cyrillic(f) for f in counted]))
        lines.append((common.get(word) or guessed.get(word) or word) + "\t" + " ".join(counted))
    return "\n".join(lines) + "\n", label


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--language", required=True, help="a language code, or all")
    parser.add_argument("--source", choices=sorted(SOURCES))
    parser.add_argument("--size", type=int, default=10_000)
    parser.add_argument("--out", help="the output file, or folder with --language all")
    args = parser.parse_args()
    for language in LANGUAGES if args.language == "all" else [args.language]:
        text, label = build(language, args.source, args.size)
        out = os.path.join(args.out, f"{language}.tsv") if args.out and args.language == "all" else args.out or os.path.join(OUT_DIR, f"{language}.tsv")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, "w", encoding="utf-8") as f:
            f.write(text)
        print(f"{out}: {text.count(chr(10)) - 2} words from {label}", file=sys.stderr)


if __name__ == "__main__":
    main()
