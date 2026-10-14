#!/usr/bin/env python3
"""
Builds a word frequency list for the app: the most common words of a language, as dictionary
words ("dizer"), each with the forms counted for it ("diz", "disse", ...).

Word counts come from the first source that has the language:

  1. wordfreq (pip install wordfreq), a blend of subtitles, Wikipedia, books, news and web text
  2. FrequencyWords (hermitdave/FrequencyWords), counts from OpenSubtitles 2018
  3. the Leipzig Corpora Collection, counts from a news or Wikipedia corpus
  4. the language's Wikipedia, counted from its articles dump (for languages none of the others has)
  5. for Ancient Greek, the New Testament's word forms as MorphGNT parses the SBLGNT

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
import html
import io
import re
import os
import sqlite3
import sys
import subprocess
import tarfile
import unicodedata
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_dictionary import to_serbian_cyrillic  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")
CACHE = os.path.join(DICTIONARIES, "frequency-sources")
OUT_DIR = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency")

# The languages the app teaches, by the codes it files them under.
LANGUAGES = [
    "be", "bg", "ca", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "gl", "hr", "hu", "is",
    "it", "la", "lt", "lv", "mk", "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sr", "sv", "tr",
    "uk", "ga", "cy", "gv", "mt", "tl", "hy", "ka", "sq", "eo", "eu", "gd", "kw", "haw", "bs",
    "id", "mn", "vi", "br", "tok", "sga", "grc", "ar", "arz",
]

# wordfreq's code where it differs from the app's. wordfreq also answers for a language it lacks
# with its nearest one (Russian for Belarusian, Serbo-Croatian for Croatian), which is not wanted.
WORDFREQ_CODES = {"no": "nb", "tl": "fil"}

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
    "cy": {"leipzig": ["cym_wikipedia_2021_100K"]},
    "br": {"leipzig": ["bre_wikipedia_2021_100K"]},
    "yi": {"leipzig": ["yid_wikipedia_2021_30K"]},
    "arz": {"leipzig": ["arz_wikipedia_2021_100K"]},
    "kn": {"leipzig": ["kan_wikipedia_2021_300K"]},
    "th": {"leipzig": ["tha_news_2020_30K", "tha_wikipedia_2021_10K"]},
    "ga": {"leipzig": ["gle_wikipedia_2021_30K"]},
    "gv": {"leipzig": ["glv_wikipedia_2021_10K"]},
    "mt": {"leipzig": ["mlt_wikipedia_2021_30K", "mlt_news_2020_30K"]},
    "mn": {"leipzig": ["mon_wikipedia_2021_100K", "mon_news_2020_100K"]},
    "tl": {"leipzig": ["tgl_wikipedia_2021_100K", "tgl_news_2020_30K"]},
    "hy": {"leipzig": ["hye_wikipedia_2021_1M", "hye_news_2020_30K"]},
    "ka": {"leipzig": ["kat_wikipedia_2021_300K", "kat_news_2020_100K"]},
    "sq": {"leipzig": ["sqi_news_2020_1M", "sqi_wikipedia_2021_300K"]},
    "eu": {"leipzig": ["eus_wikipedia_2021_300K", "eus_news_2020_30K"]},
    "eo": {"leipzig": ["epo_newscrawl_2017_1M", "epo_wikipedia_2021_300K"]},
}

# Languages written without spaces: a corpus's runs of letters are cut into words as the app's
# parser cuts them, the longest the dictionary knows first; wordfreq's own lists are cut already.
SEGMENTED = {"zh", "yue", "ja", "th", "lo", "km"}
# Of these, the ones whose letters no word covers make one word together, as in the app.
JOIN_UNKNOWN = {"th", "lo", "km"}

# The gloss language of the pack whose forms group a language's words, where English's is small.
DICTIONARY_TARGETS = {"br": "fr"}

# Wikipedia editions by the app's code where they differ.
WIKIPEDIAS = {"yue": "zh_yue"}

# A first source with fewer word forms than this (wordfreq keeps only its short list for some
# languages) is blended with every other source that has the language.
ENOUGH_FORMS = 100_000

SOURCE_LICENCES = {
    "morphgnt": "CC BY-SA 3.0; text SBLGNT",
    "wikipedia": "CC BY-SA 4.0",
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


def is_wordlike(text):
    """Letters, hyphens and apostrophes; and the vowel signs and other marks of the scripts that write them apart."""
    return all(c.isalpha() or unicodedata.category(c)[0] == "M" or c in "-'ʻ\u200c" for c in text)


def english_share(form):
    """How common a form is in English, as a share of all English words, or 0 without wordfreq."""
    try:
        import wordfreq
    except ImportError:
        return 0.0
    return wordfreq.word_frequency(form, "en")


def normalise(language, form):
    """A form as the reader writes it: Romanian text has ş, ţ turned into ș, ț."""
    if language == "ro":
        return form.replace("ş", "ș").replace("ţ", "ț")
    return form


def eszett_spellings(form):
    """The spellings of a form with ss written as ß: every ss at once, and each one alone."""
    spots = [i for i in range(len(form) - 1) if form[i:i + 2] == "ss"]
    if not spots:
        return []
    variants = {form.replace("ss", "ß")}
    for i in spots:
        variants.add(form[:i] + "ß" + form[i + 2:])
    return sorted(variants)


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
    # The list's own frequencies: word_frequency would tokenize each word again, which needs MeCab
    # for Japanese and Korean.
    frequencies = wordfreq.get_frequency_dict(language, wordlist="best")
    words = sorted(frequencies, key=lambda w: -frequencies[w])[:limit]
    from importlib.metadata import version as installed
    version = installed("wordfreq")
    counts = {}
    for w in words:
        # wordfreq folds Greek final ς to σ ("τησ"); the dictionary and texts write ς.
        key = w[:-1] + "ς" if language == "el" and w.endswith("σ") else w
        counts[key] = counts.get(key, 0) + frequencies[w]
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


# Wikipedia markup that is not running text: templates, tables, references, files and comments.
WIKI_NOISE = [
    re.compile(r"<!--.*?-->", re.S),
    re.compile(r"<ref[^>/]*/>|<ref[^>]*>.*?</ref>", re.S),
    re.compile(r"\{\|.*?\|\}", re.S),
    re.compile(r"\[\[(?:[^\]|:]+):[^\]]*\]\]"),
    re.compile(r"<[^>]+>"),
    re.compile(r"https?://\S+"),
]
WIKI_LINK = re.compile(r"\[\[(?:[^\]|]*\|)?([^\]|]*)\]\]")
# A letter, or a combining mark of the scripts whose vowel signs Python does not count as letters.
WIKI_LETTER = r"[^\W\d_]|[\u0300-\u036f\u0591-\u05c7\u064b-\u065f\u0670\u0730-\u074a\u0900-\u0dff\u0e31\u0e34-\u0e3a\u0e47-\u0e4e\u0eb1\u0eb4-\u0ebc\u0ec8-\u0ecd\u17b4-\u17d3]"
WIKI_WORD = re.compile(rf"(?:{WIKI_LETTER})+(?:['’ʻ\u200c-](?:{WIKI_LETTER})+)*")


def strip_templates(text):
    """The text without {{templates}}, which nest."""
    out, depth, i = [], 0, 0
    while i < len(text):
        if text.startswith("{{", i):
            depth += 1
            i += 2
        elif text.startswith("}}", i) and depth:
            depth -= 1
            i += 2
        else:
            if not depth:
                out.append(text[i])
            i += 1
    return "".join(out)


def wikipedia_counts(language, limit):
    wiki = WIKIPEDIAS.get(language, language)
    name = f"wikipedia-{wiki}.xml.bz2"
    path = download(f"https://dumps.wikimedia.org/{wiki}wiki/latest/{wiki}wiki-latest-pages-articles.xml.bz2", name)
    if path is None:
        return None
    import bz2
    counts = defaultdict(int)
    page = []
    in_text = False
    with bz2.open(path, "rt", encoding="utf-8") as f:
        for line in f:
            if "<text" in line:
                in_text = True
                line = line[line.index(">", line.index("<text")) + 1:]
            if in_text:
                end = line.find("</text>")
                page.append(line if end < 0 else line[:end])
                if end >= 0:
                    in_text = False
                    text = "".join(page)
                    page = []
                    if text.lstrip().lower().startswith("#redirect"):
                        continue
                    text = html.unescape(strip_templates(text))
                    for noise in WIKI_NOISE:
                        text = noise.sub(" ", text)
                    text = WIKI_LINK.sub(r"\1", text)
                    for word in WIKI_WORD.findall(text):
                        counts[lower(language, word)] += 1
    top = sorted(counts.items(), key=lambda kv: -kv[1])[:limit]
    return dict(top), f"{wiki}.wikipedia.org articles"


def morphgnt_counts(language, limit):
    """Ancient Greek, which no other source has: the word forms of the New Testament as MorphGNT parses the SBLGNT."""
    if language != "grc":
        return None
    path = download("https://github.com/morphgnt/sblgnt/archive/refs/heads/master.zip", "morphgnt.zip")
    if path is None:
        return None
    import zipfile
    counts = defaultdict(int)
    with zipfile.ZipFile(path) as z:
        for name in z.namelist():
            if name.endswith("-morphgnt.txt"):
                for line in z.read(name).decode("utf-8").splitlines():
                    parts = line.split()
                    if len(parts) == 7:
                        counts[lower(language, parts[4])] += 1
    top = sorted(counts.items(), key=lambda kv: -kv[1])[:limit]
    return dict(top), "MorphGNT parses of the SBLGNT"


SOURCES = {"wordfreq": wordfreq_counts, "morphgnt": morphgnt_counts, "frequencywords": frequencywords_counts, "leipzig": leipzig_counts, "wikipedia": wikipedia_counts}


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
    path = os.path.join(DICTIONARIES, f"{language}-{DICTIONARY_TARGETS.get(language, 'en')}.sqlite")
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
    # Spellings the 1996 German reform replaced ("daß" for "dass"), whose other links are noise ("daß" to "das").
    outdated = set()
    for form_lc, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        if "deprecated in the spelling reform" in lemma:
            outdated.add(form_lc)
        lemma = lower(language, lemma)
        if is_wordlike(lemma):
            lemmas[form_lc].append(lemma)
    for form_lc, of in lemmas.items():
        if len(set(of)) > MAX_LEMMAS_PER_FORM:
            lemmas[form_lc] = []
    return pos, lemmas, common, outdated


def segmenter(language):
    """Cuts a run of letters into the longest words of the language's dictionary, as SegmentingParser does."""
    db = sqlite3.connect(os.path.join(DICTIONARIES, f"{language}-{DICTIONARY_TARGETS.get(language, 'en')}.sqlite"))
    words = {w for (w,) in db.execute("SELECT word_lc FROM entries UNION SELECT form_lc FROM forms") if w and " " not in w}
    longest = min(16, max((len(w) for w in words), default=1))
    join = language in JOIN_UNKNOWN

    def segment(run):
        out, unknown, i = [], "", 0
        while i < len(run):
            end = next((j for j in range(min(len(run), i + longest), i, -1) if run[i:j] in words), None)
            if end is None:
                if join:
                    unknown += run[i]
                else:
                    out.append(run[i])
                i += 1
                continue
            if unknown:
                out.append(unknown)
                unknown = ""
            out.append(run[i:end])
            i = end
        if unknown:
            out.append(unknown)
        return out

    return segment


def resegment(language, counts):
    segment = segmenter(language)
    out = defaultdict(int)
    for text, count in counts.items():
        for word in segment(text):
            out[word] += count
    return dict(out)


def build(language, source, size):
    order = [source] if source else ["wordfreq", "frequencywords", "leipzig", "wikipedia", "morphgnt"]
    found = []
    for name in order:
        result = SOURCES[name](language, 200_000)
        if result and language in SEGMENTED and name != "wordfreq":
            result = (resegment(language, result[0]), result[1])
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
            freq[normalise(language, w)] += c / total / len(found)
    label = " + ".join(f"{label} ({SOURCE_LICENCES[name]})" for name, _, label in found)
    pos, lemmas, common, outdated = load_dictionary(language)
    overrides = OVERRIDES.get(language, {})
    excluded = EXCLUDE.get(language, set())

    # The words simplemma found for forms the dictionary lacks, as they are written.
    guessed = {}
    lemmatizer = simplemma_for(language)

    def guess(form):
        """simplemma's word for a form the dictionary does not have, or None for a name or an unknown form."""
        if lemmatizer is None or len(form) < 2 or not is_wordlike(form) or not lemmatizer.is_known(form):
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

    def count(word):
        """How common a word's own spelling is; wordfreq has German ß words under ss ("heissen")."""
        return freq.get(word, 0) or (freq.get(word.replace("ß", "ss"), 0) if language == "de" else 0)

    def spellings(form):
        """The form and, in German, its current spellings with ß: wordfreq writes "aß" and "Ass" alike as "ass"."""
        if language != "de":
            return [form]
        # A ß spelling the dictionary files as another spelling of the form ("Obergeschoß") adds nothing.
        return [form] + [v for v in eszett_spellings(form) if v not in outdated and form not in lemmas.get(v, [])]

    def direct(form):
        """The words a form's count goes to, with their shares, before following words that are forms themselves."""
        if form in overrides:
            others = overrides[form]
            if others == [form]:
                return {form: 1.0}
        else:
            if len(form) == 1 and not is_word(form):
                return {}
            known = [s for s in spellings(form) if s in lemmas or s in pos]
            if not known:
                word = guess(form)
                return {word: 1.0} if word else {}
            candidates = [l for s in known for l in ([s] if s != form else []) + lemmas.get(s, [])]
            others = list(dict.fromkeys(l for l in candidates if l != form and is_word(l)))
        # A form whose only entry is an interjection ("foi", "nossa") is counted for the word it is a form of.
        if is_word(form) and form not in overrides and not (others and pos[form] & WORD_POS <= {"intj"}):
            # A form that is a word itself keeps its count ("casa", not "casar") unless [TAKEOVER] says otherwise.
            inflected = bool(pos[form] & INFLECTED_KINDS)
            mine = count(form)
            others = [
                l for l in others
                if count(l) >= TAKEOVER * mine or (inflected and pos[l] & INFLECTED_KINDS and count(l) >= INFLECTED_TAKEOVER * mine)
            ]
            if not others:
                return {form: 1.0}
        if not others:
            return {}
        weights = {l: count(l) or 1e-12 for l in others}
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
        if language == "de":
            # wordfreq writes ß as ss ("weisst"); texts write "weißt", which the dictionary knows.
            counted = list(dict.fromkeys(counted + [v for f in counted for v in eszett_spellings(f) if v in lemmas or v in pos]))
        if language in ("ru", "be"):
            # Most texts write ё as е ("идет" for "идёт").
            counted = list(dict.fromkeys(counted + [f.replace("ё", "е") for f in counted if "ё" in f]))
        if language == "be":
            # A word starts with ў after a vowel ("і ўсе") and with у elsewhere.
            counted = list(dict.fromkeys(counted + [{"у": "ў", "ў": "у"}[f[0]] + f[1:] for f in counted if f[:1] in ("у", "ў")]))
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
