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
(dictionaries/<code>-en.sqlite, made by tools/build_dictionary.py). A form that is also a word in
its own right ("casa", the noun, and a form of "casar") keeps its count unless the other word is
more common than the form itself ("era" goes to "ser"); a form of several words ("foi", of "ir"
and "ser") is shared between them by how common they are. Forms that are not in the dictionary
(names, foreign words, numbers) and words that are only names, letters or affixes are left out.

The output is a text file with one word per line, most common first:

  # source: wordfreq 3.1 (CC BY-SA 4.0)
  dizer<TAB>diz disse dizer dizendo ...

Usage: python3 tools/build_frequency_list.py --language pt [--source wordfreq|frequencywords|leipzig]
       [--size 10000] [--out FILE]
"""
import argparse
import io
import os
import sqlite3
import sys
import tarfile
import urllib.request
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")
CACHE = os.path.join(DICTIONARIES, "frequency-sources")
OUT_DIR = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency")

# Per language: the FrequencyWords folder and the Leipzig corpus to fall back on.
FALLBACKS = {
    "pt": {"frequencywords": "pt_br", "leipzig": "por_news_2020_100K"},
}

SOURCE_LICENCES = {
    "wordfreq": "CC BY-SA 4.0",
    "frequencywords": "CC BY-SA 4.0",
    "leipzig": "see https://wortschatz.uni-leipzig.de/en/download",
}

# Parts of speech a frequency list shows; names, letters, affixes and set phrases are left out.
WORD_POS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "article", "intj", "contraction", "particle"}

# Forms the dictionary does not tie to the word they belong to, per language.
OVERRIDES = {
    "pt": {
        "a": ["a"], "ela": ["ela"], "elas": ["ela"], "eles": ["eles"],
        "ao": ["a"], "aos": ["a"], "à": ["a"], "às": ["a"],
        "os": ["o"], "as": ["a"],
        "na": ["em"], "nas": ["em"], "nos": ["em", "nós"],
        "pela": ["por"], "pelas": ["por"], "pelos": ["por"],
        "numa": ["em"], "nuns": ["em"], "numas": ["em"],
        "dela": ["de"], "deles": ["de"], "delas": ["de"],
        "neste": ["em"], "nesta": ["em"], "nesse": ["em"], "nessa": ["em"], "nisso": ["em"], "nisto": ["em"],
        "disso": ["de"], "disto": ["de"], "daquele": ["de"], "daquela": ["de"], "naquele": ["em"], "naquela": ["em"],
        "daqui": ["de"], "dali": ["de"], "daí": ["de"],
        "deu": ["dar"],
        # Words the dictionary also lists as forms of another word, which a learner meets on their own.
        "isso": ["isso"], "isto": ["isto"], "aquilo": ["aquilo"], "fora": ["fora"], "mal": ["mal"],
        "melhor": ["melhor"], "pior": ["pior"], "maior": ["maior"], "menor": ["menor"], "ótimo": ["ótimo"], "péssimo": ["péssimo"],
        "tais": ["tal"],
    },
}

# Words left out although the dictionary has them: names used far more than the common word.
EXCLUDE = {
    "pt": {"in", "brasil", "joão", "silva", "jesus", "lula", "paulo", "maria", "pedro", "josé", "francisco", "bahia", "paraná", "salvador"},
}

# How common another word must be, against the form itself, to take a form that is also a word:
# a more common word ("era" to "ser"), or a less common one of a kind that inflects for
# gender and number ("todos" to "todo", "nova" to "novo").
TAKEOVER = 1.0
INFLECTED_KINDS = {"adj", "pron", "det", "article", "num"}
INFLECTED_TAKEOVER = 0.2
# Forms considered per word, most common first; the app matches saved words against them.
FORMS_PER_WORD = 24


def wordfreq_counts(language, limit):
    import wordfreq
    if language not in wordfreq.available_languages("best"):
        return None
    words = wordfreq.top_n_list(language, limit, wordlist="best")
    from importlib.metadata import version as installed
    version = installed("wordfreq")
    return {w: wordfreq.word_frequency(w, language, wordlist="best") for w in words}, f"wordfreq {version}".strip()


def download(url, name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print(f"Downloading {url}", file=sys.stderr)
        try:
            urllib.request.urlretrieve(url, path + ".part")
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            raise
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
                counts[parts[0].lower()] = counts.get(parts[0].lower(), 0) + int(parts[1])
            if len(counts) >= limit:
                break
    return counts, f"FrequencyWords {folder} (OpenSubtitles 2018)"


def leipzig_counts(language, limit):
    corpus = FALLBACKS.get(language, {}).get("leipzig")
    if corpus is None:
        return None
    path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
    if path is None:
        return None
    counts = {}
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 3 and parts[2].isdigit():
                word = parts[1].lower()
                counts[word] = counts.get(word, 0) + int(parts[2])
    top = sorted(counts.items(), key=lambda kv: -kv[1])[:limit]
    return dict(top), f"Leipzig Corpora Collection {corpus}"


SOURCES = {"wordfreq": wordfreq_counts, "frequencywords": frequencywords_counts, "leipzig": leipzig_counts}


def load_dictionary(language):
    path = os.path.join(DICTIONARIES, f"{language}-en.sqlite")
    if not os.path.exists(path):
        sys.exit(f"{path} is missing; build it with: python3 tools/build_dictionary.py --source {language} --target en")
    db = sqlite3.connect(path)
    pos = defaultdict(set)
    # Words whose common-word entries are all capitalised ("China", the noun) are names too.
    lowercase = set()
    for word, word_lc, p in db.execute("SELECT word, word_lc, pos FROM entries"):
        pos[word_lc].add(p)
        if p in WORD_POS and word == word_lc:
            lowercase.add(word_lc)
    lemmas = defaultdict(list)
    for form_lc, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemma = lemma.lower()
        if all(c.isalpha() or c in "-'" for c in lemma):
            lemmas[form_lc].append(lemma)
    return pos, lemmas, lowercase


def build(language, source, size):
    order = [source] if source else ["wordfreq", "frequencywords", "leipzig"]
    for name in order:
        found = SOURCES[name](language, 200_000)
        if found:
            counts, label = found
            break
    else:
        sys.exit(f"No frequency source has {language}")
    pos, lemmas, lowercase = load_dictionary(language)
    overrides = OVERRIDES.get(language, {})
    excluded = EXCLUDE.get(language, set())
    total = sum(counts.values())
    freq = {w: c / total for w, c in counts.items()}

    def is_word(w):
        kinds = pos.get(w, set()) & WORD_POS
        if not kinds or w not in lowercase or w in excluded:
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
    for form, share in freq.items():
        if form in excluded:
            continue
        targets = targets_of(form)
        for lemma, part in targets.items():
            scores[lemma] += share * part
            forms[lemma][form] = forms[lemma].get(form, 0) + share * part

    ranked = sorted(scores, key=lambda w: -scores[w])[:size]
    lines = [f"# source: {label} ({SOURCE_LICENCES[name]})", f"# words: {len(ranked)}"]
    for word in ranked:
        counted = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_PER_WORD]
        lines.append(word + "\t" + " ".join(counted))
    return "\n".join(lines) + "\n", label


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--language", required=True)
    parser.add_argument("--source", choices=sorted(SOURCES))
    parser.add_argument("--size", type=int, default=10_000)
    parser.add_argument("--out")
    args = parser.parse_args()
    text, label = build(args.language, args.source, args.size)
    out = args.out or os.path.join(OUT_DIR, f"{args.language}.tsv")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"{out}: {text.count(chr(10)) - 2} words from {label}", file=sys.stderr)


if __name__ == "__main__":
    main()
