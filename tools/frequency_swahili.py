"""
The Swahili frequency list (build_frequency_list.py --language sw).

Swahili writes a verb with its subject, tense, relative and object markers in front (a-li-ni-ona "he saw me",
wa-na-o-soma "who are reading", ha-tu-ku-ja "we did not come") and a noun's agreement prefixes on its adjectives (mtu
mzuri, vitabu vizuri), so a corpus has hundreds of forms of a common verb, most of which no pack lists. Each text word is
read here as the pack's form (dictionaries/sw-en.sqlite, from the English Wiktionary), an adjective with its class prefix,
a verb with its markers (verb_lemmas), or a noun with the locative -ni (nyumbani, sokoni); a word of the lexicon keeps its
own count, and a form of several words is shared by how common each is by the forms of no other.

Counted from three kinds of text: film subtitles and talks (OPUS OpenSubtitles, TED2020), news (Leipzig's news and community
corpora and Global Voices) and Leipzig's Wikipedia sentences, each the share of the blend WEIGHTS gives. A capitalised word
inside a sentence is a name and not counted.
"""
import functools
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

LEIPZIG_NEWS = ["swa_news_2020_30K", "swa_newscrawl_2011_10K", "swa_community_2017"]
LEIPZIG_WIKIPEDIA = ["swa_wikipedia_2021_100K"]
OPUS = [("subtitles", "OpenSubtitles", "v2024"), ("subtitles", "TED2020", "v1"), ("news", "GlobalVoices", "v2018q4")]
# Subtitles bring the everyday words (nakupenda, njoo, asante), news the written language's, Wikipedia its rarer words; it
# is kept small, as much of it is bots' pages on villages and species (kata, wilaya, spishi).
WEIGHTS = {"subtitles": 0.45, "news": 0.35, "wikipedia": 0.2}
LICENCES = {
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "OpenSubtitles": "terms at https://opus.nlpl.eu/OpenSubtitles/corpus/version/OpenSubtitles",
    "GlobalVoices": "CC BY 3.0",
    "TED2020": "CC BY-NC-ND 4.0",
}

# As the reader's wordCharacters (a-zA-Z'-, with ´ ` ’ ‘ read as '), cut at hyphens: ng'ombe is one word.
APOSTROPHES = str.maketrans("´`’‘", "''''")
WORD = re.compile(r"[A-Za-z]+(?:'[A-Za-z]+)*")
SENTENCE_START = re.compile(r"(?:^|[.!?:;…\"“”«»()\[\]♪-]\s*)$")
# A word must be counted this often (all its forms, all sources) to be listed.
LEAST_COUNT = 4
FORMS_LISTED = 400

# Parts of speech that are no words of a text.
NOT_WORDS = {"name", "prefix", "suffix", "infix", "interfix", "symbol", "character", "phrase", "proverb", "prep_phrase"}
# The English Wiktionary's verbs made from others, read as their verb unless FreeDict has them as words of their own
# (ambia "tell", of amba; eleza "explain", of elea).
DERIVED = {"passive", "causative", "applicative", "stative", "reciprocal", "conversive"}

# Verb markers. Subject: the persons and the noun classes; before a vowel some are written short (tw-ende, mw-ende).
SUBJECTS = ["ni", "u", "a", "tu", "m", "mu", "wa", "i", "li", "ya", "ki", "vi", "zi", "ku", "pa"]
SUBJECTS_BEFORE_VOWEL = ["n", "w", "tw", "mw", "y", "l", "ch", "vy", "z", "kw"]
NEGATIVE_SUBJECTS = ["si", "hu", "ha", "hatu", "ham", "hamu", "hawa", "hau", "hai", "hali", "haya", "haki", "havi", "hazi",
                     "haku", "hapa", "hayu"]
TENSES = ["na", "li", "ta", "me", "mesha", "ki", "ka", "nge", "ngali", "ngeli", "singe", "singali"]
NEGATIVE_TENSES = ["ku", "ta", "ja", "nge", "ngali"]
# The relative markers, after na, li, taka (the future's ta before them) and si.
RELATIVES = ["ye", "o", "yo", "lo", "cho", "vyo", "zo", "ko", "po", "mo"]
RELATIVE_TENSES = ["na", "li", "taka", "si"]
OBJECTS = ["", "ni", "ku", "m", "mu", "mw", "tu", "wa", "ki", "vi", "li", "ya", "i", "zi", "u", "pa", "ji"]
# The gnomic tense, its a merged into the subject (nasoma, twasoma, chafaa).
GNOMIC = ["na", "wa", "a", "twa", "mwa", "cha", "vya", "ya", "za", "la", "kwa", "pa"]

# By hand: words the packs lack, and forms whose word the rules would get wrong.
SWAHILI_WORDS = {}
SWAHILI_FORMS = {
    # The infinitive of wa "be", far more than the conjunction "that" (kwamba is that).
    "kuwa": "wa",
    # "Is not", the negative of ni, a word of its own as ni is.
    "si": "si",
    # "I have", "he has", "it has", far more than nina "mother", ana (of ana kwa ana) and ina "truly".
    "nina": "wa na",
    "ana": "wa na",
    "ina": "wa na",
    # "While you are" far more than ukiwa "loneliness"; siwe "I am not", wa's as its other persons are (usiwe, asiwe).
    "ukiwa": "wa",
    "siwe": "wa",
    # The pack has it as lewa's ("not be drunk"); it is toa's passive ("be given out, released").
    "kutolewa": "toa",
    # "Due to" (kutokana na), the infinitive of tokana, toka's; not kana's with the negative kuto.
    "kutokana": "toka",
}
# The relative of the place copulas wapo, wako, wamo, when no word of its own (kilimo is "farming"): alipo "where he is",
# tulikomo "where we are inside".
RELATIVE_COPULAS = {s + "li" + r: "wa" + r for s in SUBJECTS for r in ("po", "ko", "mo")}
# An entry that is only a verb with an object marker: kupa "to give to you", kuita "to call you".
CONCORD_FORM = re.compile(r"object concord\) with \w+ of -([a-z]+)")
# Words that are no words of an ordinary text: Wikipedia's, letters, abbreviations.
SWAHILI_EXCLUDED = set()


def _verb_prefixes():
    """Every run of markers a verb may have in front, by the ending it takes: a (indicative), e (subjunctive), i (negative
    present), r (a general relative: asomaye) and n (the plural imperative: someni, nisaidieni)."""
    out = defaultdict(set)
    ku = ["", "ku", "kw"]  # one-syllable verbs keep their infinitive's ku (anakula, alikuja), enda and isha its kw (alikwenda)
    for o in OBJECTS:
        for k in ku:
            for start in ("ku", "kw", "kuto", "hu", ""):
                if start == "kw" and (o or k) or start == "" and (o or k):
                    continue
                out[start + o + (k if start not in ("ku", "kw", "kuto") else "")].add("a")
            for s in SUBJECTS:
                for t in TENSES:
                    out[s + t + o + k].add("a")
                for t in RELATIVE_TENSES:
                    for r in RELATIVES:
                        out[s + t + r + o + k].add("a")
            for s in NEGATIVE_SUBJECTS:
                for t in NEGATIVE_TENSES:
                    out[s + t + o + k].add("a")
        for g in GNOMIC:
            out[g + o].add("a")
        for s in SUBJECTS + SUBJECTS_BEFORE_VOWEL:
            for neg in ("", "si"):
                out[s + neg + o].add("e")
                out[s + neg + o].add("n")
            out[s + o].add("r")
        out[o].add("e")
        out[o].add("n")
        for s in NEGATIVE_SUBJECTS:
            out[s + o].add("i")
    return out


VERB_PREFIXES = _verb_prefixes()
LONGEST_PREFIX = max(map(len, VERB_PREFIXES))


def passives(stem):
    """[stem]'s passives (pendwa, funguliwa, tolewa, pewa, liwa, fikiriwa)."""
    vowels = "aeiou"
    if not stem.endswith("a"):
        return {stem + "wa", stem[:-1] + "iwa"} if len(stem) > 2 else set()
    if sum(c in vowels for c in stem) == 1:
        return {stem[:-1] + "ewa", stem[:-1] + "iwa"}
    if stem[-2] in vowels:
        return {stem[:-1] + "liwa", stem[:-1] + "lewa", stem + "wa"}
    return {stem[:-1] + "wa"}


def verb_tails(stem):
    """The ends of [stem]'s forms, by the ending each takes after its markers."""
    out = []
    if stem.endswith("a"):
        out += [(stem, "a"), (stem[:-1] + "e", "e"), (stem[:-1] + "i", "i"), (stem[:-1] + "eni", "n")]
        out += [(stem + r, "r") for r in RELATIVES]
    else:
        # A verb from Arabic keeps its last vowel (anafikiri, afikiri, hafikiri, rudini).
        out += [(stem, "a"), (stem, "e"), (stem, "i"), (stem + "ni", "n")]
    return out


def adjective_forms(stem):
    """An adjective's or number's forms with each class's prefix (mzuri, wazuri, vizuri, nzuri; mwema, wema, chema, njema)."""
    if not stem:
        return set()
    out = {stem}
    if stem[0] in "aeiou":
        for p in ("mw", "w", "wa", "m", "my", "mi", "j", "ji", "ma", "ch", "ki", "k", "vy", "vi", "v", "ny", "nj", "p", "pa", "kw",
                  "ku"):
            out.add(p + stem)
        if stem[0] == "i":
            out |= {p + stem[1:] for p in ("we", "me", "pe")}
    else:
        out |= {p + stem for p in ("m", "wa", "mi", "ji", "ma", "ki", "vi", "pa", "ku", "mu")}
        if stem[0] in "dgjz":
            out.add("n" + stem)
        elif stem[0] in "bvp":
            out.add("m" + stem)
        elif stem[0] in "rl":
            out.add("nd" + stem[1:])
        elif stem[0] == "w":
            out.add("mb" + stem[1:])
    return out


class Lexicon:
    """The words of the sw-en pack (and FreeDict's sw-en), their forms, verbs and declinable adjectives."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "sw-en.sqlite"))
        self.kinds = defaultdict(set)
        declinable, invariable = set(), set()
        concord_forms = defaultdict(set)
        for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
            if pos in NOT_WORDS or not re.fullmatch(r"[a-z]+(?:'[a-z]+)*(?: na)?", word):
                continue
            glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])]
            bases = {m.group(1) for g in glosses if (m := CONCORD_FORM.search(g))}
            if glosses and len(bases) == 1 and all(CONCORD_FORM.search(g) for g in glosses):
                concord_forms[word] |= bases
                continue
            self.kinds[word].add(pos)
            tags = {t for s in json.loads(senses) for t in s.get("tags", [])}
            if pos in ("adj", "num") and "declinable" in tags:
                (invariable if "invariable" in tags else declinable).add(word)
        self.forms = defaultdict(set)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            if lemma in self.kinds and form != lemma and re.fullmatch(r"[a-z]+(?:'[a-z]+)*", form):
                self.forms[form].add(lemma)
        for word, bases in concord_forms.items():
            if word not in self.kinds:
                self.forms[word] |= {b for b in bases if b in self.kinds}
        derived = self._derived(dictionaries)
        freedict = self._freedict(dictionaries)
        for word, pos in freedict.items():
            if word in self.kinds:
                continue
            if word in derived or word not in self.forms:
                self.kinds[word].add(pos)
                self.forms.pop(word, None)
        for word, kind in SWAHILI_WORDS.items():
            self.kinds[word].add(kind)
        # ni and si, the copula and its negative, take no markers; siwe's forms are wa's.
        self.verbs = {w for w, ks in self.kinds.items() if "verb" in ks and " " not in w and w not in ("ni", "si", "siwe")}
        # The stems a verb's forms are made on: its own, and those of the verbs made from it that are no words of their own.
        self.stems = defaultdict(set)
        for verb in self.verbs:
            self.stems[verb].add(verb)
        for word, base in derived.items():
            if word not in self.kinds:
                while base in derived and base not in self.kinds:
                    base = derived[base]
                if base in self.verbs:
                    self.stems[word].add(base)
        # A passive no word of its own is its verb's (kulimwa: lima).
        for verb in self.verbs:
            for passive in passives(verb):
                if passive not in self.kinds and passive not in self.stems:
                    self.stems[passive].add(verb)
        self.tails = defaultdict(set)
        for stem, verbs in self.stems.items():
            for tail, ending in verb_tails(stem):
                for verb in verbs:
                    self.tails[tail].add((verb, ending))
        self.adjective_forms = defaultdict(set)
        for word in declinable - invariable:
            for form in adjective_forms(word):
                if form != word:
                    self.adjective_forms[form].add(word)
        self.nouns = {w for w, ks in self.kinds.items() if "noun" in ks}

    @staticmethod
    def _derived(dictionaries):
        """The English Wiktionary's derived verbs, as their verb (pigwa: piga)."""
        out = {}
        with gzip.open(os.path.join(dictionaries, "kaikki.org-dictionary-Swahili.jsonl.gz"), "rt", encoding="utf-8") as f:
            for line in f:
                if '"verb"' not in line:
                    continue
                entry = json.loads(line)
                if entry.get("pos") != "verb":
                    continue
                for sense in entry.get("senses", []):
                    if DERIVED & set(sense.get("tags", [])) and sense.get("form_of"):
                        out[entry["word"].lower()] = sense["form_of"][0]["word"].lstrip("-").lower()
        return out

    @staticmethod
    def _freedict(dictionaries):
        path = os.path.join(dictionaries, "sw-en-freedict.sqlite")
        if not os.path.exists(path):
            with gzip.open(path + ".gzip") as f, open(path + ".part", "wb") as out:
                out.write(f.read())
            os.rename(path + ".part", path)
        kinds = {"noun": "noun", "verb": "verb", "adjective": "adj", "adverb": "adv", "pronoun": "pron", "conjunction": "conj",
                 "preposition": "prep", "interjection": "intj", "numeral": "num"}
        out = {}
        for word, senses in sqlite3.connect(path).execute("SELECT word_lc, senses FROM entries"):
            if not re.fullmatch(r"[a-z]+(?:'[a-z]+)*", word):
                continue
            glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])[:1]]
            kind = next((kinds[g.split(",")[0].strip()] for g in glosses if g.split(",")[0].strip() in kinds), None)
            if kind:
                out[word] = kind
        return out

    def is_word(self, token):
        return token in self.kinds

    @functools.lru_cache(maxsize=None)
    def verb_lemmas(self, token):
        """The verbs [token] is a form of by its markers (alinipenda: penda; hawaji: ja; kitakachofanyika: fanya), those of the
        longest stem only: alichomwa is choma's (a-li-chomwa), not wa's (a-li-cho-m-wa)."""
        out = set()
        for i in range(0, min(len(token), LONGEST_PREFIX) + 1):
            endings = VERB_PREFIXES.get(token[:i])
            if not endings:
                continue
            for verb, ending in self.tails.get(token[i:], ()):
                if ending in endings:
                    out.add(verb)
            if out:
                break
        return frozenset(out)

    def takes_over(self, token):
        """Whether the words the pack has [token] as a form of may take its count: only when it is a verb and nothing else
        (acha, kuna), never when it is a noun, particle or adverb as well (hali, katika, jana, lazima)."""
        return token not in SWAHILI_FORMS and self.kinds.get(token) == {"verb"} and bool(self.forms.get(token))

    @functools.lru_cache(maxsize=None)
    def lemmas(self, token):
        """The words [token] is a form of: itself when it is a word, else the pack's words, a noun's with the locative -ni, or
        those of the adjective and verb rules."""
        if token in SWAHILI_FORMS:
            return frozenset(SWAHILI_FORMS[token].split(","))
        if token in SWAHILI_EXCLUDED:
            return frozenset()
        if token in self.kinds:
            return frozenset({token})
        if token in self.forms:
            return frozenset(self.forms[token])
        if token in RELATIVE_COPULAS:
            return frozenset({RELATIVE_COPULAS[token]})
        if token.endswith("ni") and len(token) > 4:
            base = token[:-2]
            found = {base} if base in self.nouns else {w for w in self.forms.get(base, ()) if w in self.nouns}
            if found:
                return frozenset(found)
        return frozenset(self.adjective_forms.get(token, set()) | self.verb_lemmas(token))


def _tokens(line):
    """A line's words, lowercase, without the capitalised ones inside a sentence (names); an all-capitals word is a word."""
    line = line.translate(APOSTROPHES)
    for match in WORD.finditer(line):
        word = match.group(0)
        if word[0].isupper() and not word.isupper() and not SENTENCE_START.search(line[:match.start()]):
            continue
        yield word.lower()


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            yield line.rstrip("\n").partition("\t")[2]


# Wikipedia sentences that differ only in their names and numbers ("X ni kata ya Wilaya ya Y katika Mkoa wa Z") are counted once.
SKELETON_NAME = re.compile(r"[A-Za-z']+")


def _counts(download):
    """Each kind of text's word counts, and the names of the corpora counted."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    used = defaultdict(list)
    for kind, corpora in (("news", LEIPZIG_NEWS), ("wikipedia", LEIPZIG_WIKIPEDIA)):
        for corpus in corpora:
            path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
            if path is None:
                continue
            used["leipzig"].append(corpus)
            seen = set()
            for sentence in _leipzig_sentences(path):
                if kind == "wikipedia":
                    key = re.sub(r"\d+", "0", SKELETON_NAME.sub(lambda m: m.group() if m.group().islower() else "X", sentence))
                    if key in seen:
                        continue
                    seen.add(key)
                for word in _tokens(sentence):
                    counts[kind][word] += 1
    for kind, corpus, version in OPUS:
        name = f"opus-{corpus.lower()}-{version}-sw.txt.gz"
        path = download(f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/sw.txt.gz", name)
        if path is None:
            continue
        used[corpus].append(version)
        with gzip.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                for word in _tokens(line.strip().lstrip("-").strip()):
                    counts[kind][word] += 1
    return counts, used


def label_of(used):
    parts = []
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    for corpus in ("OpenSubtitles", "TED2020", "GlobalVoices"):
        if used.get(corpus):
            parts.append(f"OPUS {corpus} {', '.join(used[corpus])} sw ({LICENCES[corpus]})")
    return " + ".join(parts)


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the lexicon's words by their forms' counts, each kind of text weighted by WEIGHTS."""
    lexicon = Lexicon(dictionaries)
    counts, used = _counts(download)
    label = label_of(used)
    share = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        if not total:
            continue
        for form, n in kind_counts.items():
            share[form] += WEIGHTS[kind] * n / total
            raw[form] += n

    heads = {form: lexicon.lemmas(form) for form in share}
    own = defaultdict(float)
    for form, value in share.items():
        if len(heads[form]) == 1 and not lexicon.takes_over(form):
            own[next(iter(heads[form]))] += value
    # A verb that is also a form of a commoner word than itself is that word's (kuna of wa na), unless the text uses it as a
    # verb with forms of its own as well (acha: kuacha, aliacha; lala: kulala).
    for form in share:
        if lexicon.takes_over(form):
            others = lexicon.forms[form] - {form}
            if max(own.get(w, 0.0) for w in others) > share[form] and own.get(form, 0.0) < share[form]:
                heads[form] = frozenset(others)
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    for form, value in share.items():
        words = heads[form]
        if not words:
            continue
        weights = {w: own.get(w, 0.0) for w in words}
        whole = sum(weights.values())
        for word in words:
            part = weights[word] / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            scores[word] += value * part
            totals[word] += raw[form]
            forms[word][form] = forms[word].get(form, 0.0) + value * part

    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed and " " not in word:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


_CHECKER = {}


def checker_lexicon():
    if "lexicon" not in _CHECKER:
        _CHECKER["lexicon"] = Lexicon()
    return _CHECKER["lexicon"]


def checker_word(token, ranks):
    """
    The list's word [token] is a form of by the rules the list was built with, the commonest when it may be several, or None:
    a verb form the list does not have (tutakapokutana: kutana), a class form of an adjective, a locative of a noun.
    """
    found = [(ranks[w], w) for w in checker_lexicon().lemmas(token) if w in ranks]
    return min(found)[1] if found else None
