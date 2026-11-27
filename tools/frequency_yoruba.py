"""
Yoruba spellings, and the Yoruba frequency list (build_frequency_list.py --language yo).

Yoruba marks its tones on the vowels (and the syllabic n, m): the grave is the low tone (ọ̀rọ̀ "word"), the acute the high
(ọjọ́ "day"), and the mid tone is left unmarked (ilé "house": i mid, é high). The dot below makes ẹ, ọ and ṣ. Tone tells
words apart (ọkọ "husband", ọkọ̀ "vehicle", ọkọ́ "hoe", oko "farm"), so the reader, this list, the dictionary pack and
tools/check_course.py all keep the marks, in NFC: ẹ́ is ẹ (U+1EB9) and a combining acute (U+0301), as there is no single
letter for it. Older typing puts the vertical line below (U+0329) for the dot; it is read as the dot (yoruba_standard).

The English Wiktionary files its Yoruba entries under a toneless title (ọkọ) and gives the toned word as the canonical form;
the pack (build_dictionary.py) files each entry under its toned word, and lists the toneless and dotless spellings as
generated forms of it, so a lookup in text typed without the marks still finds its words.

Much online Yoruba leaves the tones or the dots out, or both (machine translations always do). The list is counted only
from sentences written with the marks (well_marked): enough of their words toned and known, and none of the common words
written without its tones (ati, ninu) or a word without its dots (àwon); a spelling without its tones is never guessed at,
as several words share it. The list's forms are the standard spellings only.

The marks let some other text through, which is left out (foreign): Chinese pinyin, Vietnamese, Gbe (Fon, Gun), mojibake.
Past the commonest words the counts are small and careless typing adds its own: a rare word that a common one is mistyped
as (awọ́n "tongue" for àwọn, kíì "key" for kìí, ìre "blessings" for ire) is counted for the common word (CARELESS_RATIO), a
dialect's spelling for the standard word (lénìí for lónìí), and a rare word written capitalised far more often than not
(Dàda, Ifọ̀) is a name and left out.
"""
import functools
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

GRAVE, ACUTE, MACRON, DOT_BELOW, LINE_BELOW = "̀", "́", "̄", "̣", "̩"
TONE_MARKS = {GRAVE, ACUTE, MACRON}

# A word: letters with their marks, as the reader's wordCharacters (tools/languages/Yoruba.yaml) read it. No apostrophe or
# hyphen: Yoruba writes an elision as one word (lọ́jà, nílé).
LETTERS = "A-Za-zÀ-ÖØ-öø-ȳḀ-ỿ̀-ͯ"
WORD = re.compile(f"[{LETTERS}]+")
WORD_ONLY = re.compile(f"^[{LETTERS}]+$")


def yoruba_standard(text):
    """[text] in NFC, the line below read as the dot below."""
    return unicodedata.normalize("NFC", text.replace(LINE_BELOW, DOT_BELOW))


def toneless(word):
    """[word] without its tone marks, the dots kept (ọkọ̀: ọkọ)."""
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", word) if c not in TONE_MARKS))


def dotless(word):
    """[word] without its dots, the tones kept (àwọn: àwon)."""
    return unicodedata.normalize("NFC", unicodedata.normalize("NFD", word).replace(DOT_BELOW, ""))


def bare(word):
    """[word] without tones or dots (ọkọ̀: oko)."""
    return dotless(toneless(word))


def has_tone(word):
    return any(c in TONE_MARKS for c in unicodedata.normalize("NFD", word))


# The corpora, each a kind of text. Stories: novels, folk-tale and language pages, proverbs and recorded speech from the
# yoruba-text collection (Niger-Volta-LTI), written with every mark; news: its Global Voices articles and Leipzig's community
# corpus; web: OPUS NLLB (much of it scripture and machine translation, which the marks sift out); Wikipedia: Leipzig's
# sentences and OPUS wikimedia. The collection's blog and news pages and its two scanned books, and CCAligned, leave a
# mid tone unmarked as often as not (ki for kí, ko for kò) and are not counted.
YORUBA_TEXT_COMMIT = "1e5ee7da7fd808ab0e8cee3e5f9abd693a013640"
YORUBA_TEXT = {
    "stories": ["Alabi_YorubaTwi_Embedding/alakowe.txt", "Alabi_YorubaTwi_Embedding/edeyorubarewa.txt",
                "Alabi_YorubaTwi_Embedding/igbo_olodumare.txt", "Alabi_YorubaTwi_Embedding/irinkerindo_ninu_igbo.txt",
                "Alabi_YorubaTwi_Embedding/oroyoruba.txt", "Owe/owe.txt", "LagosNWU/all_transcripts.txt",
                "SLR86/line_index_female.tsv", "SLR86/line_index_male.tsv", "Universal_Declaration_Human_Rights/unhr.yo.txt"],
    "news": ["Iroyin/yoglobalvoices.txt"],
}
LEIPZIG = {"news": ["yor_community_2017"], "wikipedia": ["yor_wikipedia_2016_10K", "yor_wikipedia_2021_10K"]}
OPUS = [("web", "NLLB", "v1"), ("news", "GlobalVoices", "v2018q4"), ("wikipedia", "wikimedia", "v20260327")]
# Stories bring the everyday words (ẹ̀rù, ìyàwó, ilé), news the written language's, the web's scripture and Wikipedia's
# pages their own; the stories, the smallest, weigh the most.
WEIGHTS = {"stories": 0.4, "news": 0.15, "web": 0.3, "wikipedia": 0.15}
LICENCES = {
    "yoruba-text": "GPL-3.0, https://github.com/Niger-Volta-LTI/yoruba-text",
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "NLLB": "terms at https://opus.nlpl.eu/NLLB/corpus/version/NLLB",
    "GlobalVoices": "CC BY 3.0",
    "wikimedia": "CC BY-SA 4.0",
}

# A sentence is counted when at least this share of its words carries a tone mark (well-marked Yoruba has about half; text
# typed without tones, and every machine translation, has none), and this share of its lowercase words are words of the
# pack (mojibake and other languages' accented letters have tones but no words).
LEAST_TONED = 0.25
LEAST_KNOWN = 0.6
LEAST_WORDS = 4
# A sentence that writes one of these common words without its tones (ati, lati, ninu, awọn) or a word without its dots
# (àwon) is written carelessly, and its other words may be too: a mid-tone word there (ki, ko, bi) is as often a high or
# low one left unmarked. Such sentences are left out.
TELL_WORDS = ["àti", "láti", "nínú", "àwọn", "náà", "yìí", "fún", "pẹ̀lú", "ṣùgbọ́n", "ènìyàn", "tàbí", "nígbà", "nítorí",
              "lórí", "kò", "wọ́n", "jẹ́", "pé", "sí", "ọmọ", "ọdún", "èyí", "ìlú", "tún", "máa", "lọ́wọ́", "gẹ́gẹ́", "bẹ́ẹ̀",
              "púpọ̀", "kí", "bí", "wọ̀nyí", "ṣé", "rẹ̀", "wípé", "kúrò", "padà", "jáde", "ọ̀rọ̀", "ìgbà", "fẹ́"]
# A word must be counted this often (all its spellings, all sources) to be listed.
LEAST_COUNT = 4
SENTENCE_START = re.compile(r"(?:^|[.!?:;…\"“”«»()\[\]‘’'*-]\s*)$")

# Text in other languages that well_marked lets through, its words having Yoruba's marks and spellings. A word in small
# letters with marks (an English or French word in a Yoruba sentence has none, or a capital) must have Yoruba's letters and
# marks only: no c, q, v, x or z and no wh or sh (Gbe and pinyin have them), none of Vietnamese's marks (ư, ă, ẽ, ỏ) or
# other letters (đ, ɛ, ø); mojibake for the dotted letters (àwvn, æjô, ÿe) fails too. Beside the grave, acute, macron (on n)
# and dot below, the caron and circumflex write a rising and a falling tone (márǔn, mẹ́sǎn), and older typing writes the dot
# as a dot above (ṅ), a cedilla (ş) or a plus below. Pinyin's caron (nǐ, wǒmen) and macron on a vowel (duō) are told from
# the rising tone by a sentence without any of Yoruba's dotted letters. Chinese characters, kana and hangul are foreign.
YORUBA_LETTERS = set("abdefghijklmnoprstuwy")
YORUBA_MARKS = {GRAVE, ACUTE, MACRON, DOT_BELOW, "̌", "̂", "̇", "̧", "̟"}
FOREIGN_SCRIPT = re.compile("[⺀-鿿가-힯぀-ヿ]")
PINYIN_MARK = re.compile("̌|[aeiou]̄")
DOTTED = re.compile("[ẹọṣ]")
# Gbe (Gun) writes ẹ and ọ as Yoruba does; two of its commonest words in one sentence tell it.
GBE_WORDS = {yoruba_standard(w) for w in ["dọ", "wẹ", "nọ", "ẹn", "lẹ", "mẹ", "enẹ", "ehe", "gbẹtọ", "tọn"]}
# A word or spelling counted fewer times than this is rare. A rare word that shares its letters (bare) with a spelling of a
# word scoring CARELESS_RATIO times more is that word typed carelessly; a rare word written capitalised inside a sentence
# twice as often as in small letters (and at least twice) is a name.
RARE = 50
CARELESS_RATIO = 10
# "Ondo form of dúró", "Lagos form of lónìí", "(transitive) Ikalẹ form of sè": a dialect's word, counted for the standard one.
DIALECT_FORM = re.compile(r"^(?:\([^)]*\) )?[A-ZÀ-ỹ][^,;(]* form of (\S+) \(“")

# Parts of speech that are no words of a text.
NOT_WORDS = {"name", "character", "prefix", "suffix", "infix", "interfix", "symbol", "phrase", "proverb", "prep_phrase"}
LETTER_NAME = "name of the Latin script letter"
# By hand: words a spelling is counted for instead of the pack's.
YORUBA_FORMS = {}
# A low-tone verb of one syllable is said and written with the mid tone before its object (rà "buy": mo ra ìwé; mọ̀ "know":
# mo mọ ọ̀nà; gbà "take": gba owó; kà "read": ka ìwé): such a spelling is the verb's, unless it is a word itself; mọ "mould"
# is far rarer than mọ̀ before an object.
LOW_TONE_SYLLABLE = re.compile(r"^[^aeiouẹọ]*[aeiouẹọ]n?$")
MID_BEFORE_OBJECT = {"mọ"}
# Spellings that are, in the counted text, mostly a common word with its tone left out (ko for kò, to for tó, dun for dùn,
# pọ for pọ̀, verbs that take no object).
NOT_BEFORE_OBJECT = {"ko", "to", "dun", "pọ"}


def low_tone(word):
    """[word], one syllable, with the grave on its vowel (gba: gbà)."""
    d = unicodedata.normalize("NFD", word)
    i = max(i for i, c in enumerate(d) if c in "aeiou")
    j = i + 1
    while j < len(d) and unicodedata.combining(d[j]):
        j += 1
    return unicodedata.normalize("NFC", d[:j] + GRAVE + d[j:])


# Words whose only senses are rare ones (ki "load", yi "be tough", re the mid-tone syllable): what the text writes so is kí,
# yí or yìí and rẹ̀ with their tones left out.
YORUBA_EXCLUDED = {"ki", "yi", "re"}


def _hand_words():
    """
    frequency_yoruba.tsv (spelling, tab, word): the English Wiktionary lacks hundreds of the commonest Yoruba words (bá,
    tún, nígbà, tàbí, gẹ́gẹ́, rere) and the contracted ones (lára, lọ́wọ́, sílẹ̀, nílé), so the counts' standard spellings no
    pack has, down to rank 4,000 or so, were read by hand: a word of its own is listed as itself, a variant spelling as the
    word it is (gbọ́dọ̀: gbọdọ̀). Spellings without their tones, names, English and fragments were left out.
    """
    out = {}
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "frequency_yoruba.tsv"), encoding="utf-8") as f:
        for line in f:
            if line.strip() and not line.startswith("#"):
                spelling, word = (yoruba_standard(x.strip()) for x in line.rstrip("\n").split("\t"))
                out[spelling.lower()] = word
    return out


HAND_WORDS = _hand_words()
TELLS = {bare(w) for w in TELL_WORDS}


class Lexicon:
    """The words of the yo-en pack, their spellings (second canonical forms, alternative forms) and dotless spellings."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "yo-en.sqlite"))
        self.kinds = defaultdict(set)
        self.written = defaultdict(set)
        dialect = {}
        for word, pos, senses in db.execute("SELECT word, pos, senses FROM entries"):
            if pos in NOT_WORDS or " " in word or not WORD_ONLY.match(word):
                continue
            senses = json.loads(senses)
            glosses = [g for s in senses for g in s.get("glosses", [])]
            if glosses and all(LETTER_NAME in g for g in glosses):
                continue
            # A word of the Ekiti dialect only (ko "that") is no word of the standard language.
            if senses and all("Ekiti" in s.get("tags", []) for s in senses):
                continue
            standard = {m.group(1) for m in map(DIALECT_FORM.match, glosses) if m}
            if glosses and len(standard) == 1 and all(map(DIALECT_FORM.match, glosses)):
                dialect[word.lower()] = yoruba_standard(standard.pop()).lower()
                continue
            self.kinds[word.lower()].add(pos)
            self.written[word.lower()].add(word)
        for spelling, word in HAND_WORDS.items():
            if spelling == word.lower():
                self.kinds[spelling].add("hand")
                self.written[spelling].add(word)
        self.forms = defaultdict(set)
        for spelling, word in HAND_WORDS.items():
            if spelling != word.lower():
                self.forms[spelling].add(word.lower())
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms WHERE generated = 0"):
            if lemma.lower() in self.kinds and form not in self.kinds and WORD_ONLY.match(form):
                self.forms[form].add(lemma.lower())
        self.dotless = defaultdict(set)
        for word in self.kinds:
            self.dotless[dotless(word)].add(word)
        self.dialect = {word: standard for word, standard in dialect.items()
                        if standard in self.kinds and word not in self.kinds and word not in self.forms}

    def head(self, word):
        """[word] as the pack writes it: lowercase, or capitalised when every entry is (Mùsùlùmí)."""
        written = self.written[word]
        return word if word in written else min(written)

    def low_tone_verb(self, token):
        """The low-tone verb [token] is the spelling of before an object (ra: rà), or None."""
        if has_tone(token) or token in NOT_BEFORE_OBJECT or not LOW_TONE_SYLLABLE.match(token):
            return None
        verb = low_tone(token)
        return verb if self.kinds.get(verb, set()) & {"verb", "hand"} else None

    def is_known(self, token):
        return token in self.kinds or token in self.forms or token in self.dialect or self.low_tone_verb(token) is not None

    def is_careless(self, token):
        """Whether [token], no word itself, is a common word without its tones (TELL_WORDS) or a word without its dots."""
        if self.is_known(token):
            return False
        return bare(token) in TELLS if not has_tone(token) else token in self.dotless

    @functools.lru_cache(maxsize=None)
    def lemmas(self, token):
        """The words [token] is a spelling of, and whether it is a standard one (listed as a form)."""
        if token in YORUBA_FORMS:
            return frozenset(YORUBA_FORMS[token].split(",")), True
        if token in YORUBA_EXCLUDED:
            return frozenset(), False
        verb = self.low_tone_verb(token)
        if verb and (token not in self.kinds or token in MID_BEFORE_OBJECT):
            return frozenset({verb}), True
        if token in self.kinds:
            return frozenset({token}), True
        if token in self.forms:
            return frozenset(self.forms[token]), True
        if token in self.dialect:
            return frozenset({self.dialect[token]}), False
        return frozenset(), False


def _lines(path):
    if path.endswith(".tar.gz"):
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                yield line.rstrip("\n").partition("\t")[2]
    elif path.endswith(".gz"):
        with gzip.open(path, "rt", encoding="utf-8", errors="replace") as f:
            for line in f:
                yield line.rstrip("\n")
    else:
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                yield line.rstrip("\n").rpartition("\t")[2] if path.endswith(".tsv") else line.rstrip("\n")


def well_marked(words, lexicon):
    """Whether a sentence's words are written with Yoruba's marks (LEAST_TONED, LEAST_KNOWN)."""
    lowercase = [w.lower() for w in words if not w[0].isupper()]
    if len(words) < LEAST_WORDS or len(lowercase) < LEAST_WORDS - 1:
        return False
    toned = sum(has_tone(w) for w in words) / len(words)
    known = sum(lexicon.is_known(w) for w in lowercase) / len(lowercase)
    return toned >= LEAST_TONED and known >= LEAST_KNOWN and not any(lexicon.is_careless(w) for w in lowercase)


def foreign(line):
    """Whether a well-marked line is in another language or mojibake (YORUBA_LETTERS, YORUBA_MARKS, GBE_WORDS)."""
    if FOREIGN_SCRIPT.search(line):
        return True
    small = line.lower()
    if PINYIN_MARK.search(unicodedata.normalize("NFD", small)) and not DOTTED.search(small):
        return True
    for word in WORD.findall(line):
        if not word[0].islower():
            continue
        letters = unicodedata.normalize("NFD", word.lower())
        if not any(unicodedata.combining(c) for c in letters):
            continue
        if "wh" in letters or "sh" in letters or any(c not in YORUBA_LETTERS and c not in YORUBA_MARKS for c in letters):
            return True
    return len(set(WORD.findall(small)) & GBE_WORDS) >= 2


def tokens(line, lexicon, cases=None):
    """
    A line's words, lowercase, without the capitalised ones inside a sentence (names) unless the pack writes the word so, and
    without acronyms (WHO, PU). [cases] counts how often each word is written inside a sentence capitalised and in small letters.
    """
    for match in WORD.finditer(line):
        word = match.group(0)
        if word.isupper() and len(word) > 1 and word.isascii():
            continue
        if word[0].isupper() and not word.isupper() and not SENTENCE_START.search(line[:match.start()]):
            if word not in lexicon.written.get(word.lower(), ()):
                if cases is not None:
                    cases[word.lower()][0] += 1
                continue
        elif cases is not None and word[0].islower():
            cases[word][1] += 1
        yield word.lower()


def _counts(download, lexicon):
    """
    Each kind of text's word counts, the names of the corpora counted, how many sentences were kept of how many, and how often
    each word is written capitalised and in small letters inside a sentence.
    """
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    cases = defaultdict(lambda: [0, 0])
    used = defaultdict(list)
    kept = defaultdict(lambda: [0, 0])
    seen = set()
    sources = []
    for kind, files in YORUBA_TEXT.items():
        for name in files:
            url = f"https://raw.githubusercontent.com/Niger-Volta-LTI/yoruba-text/{YORUBA_TEXT_COMMIT}/{name}"
            sources.append((kind, "yoruba-text", name, url, "yoruba-text-" + name.replace("/", "_")))
    for kind, corpora in LEIPZIG.items():
        for corpus in corpora:
            sources.append((kind, "leipzig", corpus, f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz"))
    for kind, corpus, version in OPUS:
        sources.append((kind, corpus, version, f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/yo.txt.gz",
                        f"opus-{corpus.lower()}-{version}-yo.txt.gz"))
    for kind, source, name, url, file in sources:
        path = download(url, file)
        if path is None:
            continue
        used[source].append(name)
        for line in _lines(path):
            line = yoruba_standard(line.strip().lstrip("-").strip())
            if not line:
                continue
            kept[source][1] += 1
            words = WORD.findall(line)
            if not well_marked(words, lexicon) or foreign(line):
                continue
            key = hash(line)
            if key in seen:
                continue
            seen.add(key)
            kept[source][0] += 1
            for token in tokens(line, lexicon, cases):
                counts[kind][token] += 1
    return counts, used, kept, cases


def label_of(used):
    parts = []
    if used.get("yoruba-text"):
        parts.append(f"yoruba-text {YORUBA_TEXT_COMMIT[:7]}, {len(used['yoruba-text'])} files ({LICENCES['yoruba-text']})")
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    for corpus in ("NLLB", "GlobalVoices", "wikimedia"):
        if used.get(corpus):
            parts.append(f"OPUS {corpus} {', '.join(used[corpus])} yo ({LICENCES[corpus]})")
    return " + ".join(parts) + "; well-marked sentences only"


def _scores(heads, share, raw):
    """
    Each word's score (its spellings' shares; a spelling of several words split by their own spellings' shares), count, and
    its standard spellings' shares.
    """
    own = defaultdict(float)
    for token, value in share.items():
        words, _ = heads[token]
        if len(words) == 1:
            own[next(iter(words))] += value
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    for token, value in share.items():
        words, standard = heads[token]
        if not words:
            continue
        weights = {w: own.get(w, 0.0) for w in sorted(words)}
        whole = sum(weights.values())
        for word, weight in weights.items():
            part = weight / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            scores[word] += value * part
            totals[word] += raw[token]
            if standard:
                forms[word][token] = forms[word].get(token, 0.0) + value * part
    return scores, totals, forms


def _fold_careless(heads, share, raw):
    """
    Counts each rare word that shares its letters with a standard spelling of a word scoring CARELESS_RATIO times more for that
    word (awọ́n for àwọn, ìrú for irú, ẹdá for ẹ̀dá), and does not list it as that word's spelling.
    """
    scores = _scores(heads, share, raw)[0]
    alike = defaultdict(set)
    for token, (words, standard) in heads.items():
        if standard:
            alike[bare(token)].update(words)
    for token, (words, _) in list(heads.items()):
        if words != {token} or raw[token] >= RARE:
            continue
        better = frozenset(w for w in alike[bare(token)] if w != token and scores[w] >= CARELESS_RATIO * scores[token])
        if better:
            heads[token] = (better, False)


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the pack's words by their spellings' counts, each kind of text weighted by WEIGHTS."""
    lexicon = Lexicon(dictionaries)
    counts, used, kept, cases = _counts(download, lexicon)
    for source, (k, n) in sorted(kept.items()):
        print(f"yo {source}: {k:,} of {n:,} lines well marked", file=__import__("sys").stderr)
    label = label_of(used)
    share = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        if not total:
            continue
        for token, n in kind_counts.items():
            share[token] += WEIGHTS[kind] * n / total
            raw[token] += n

    heads = {token: lexicon.lemmas(token) for token in share}
    for token in share:
        capitalised, small = cases.get(token, (0, 0))
        if raw[token] < RARE and capitalised >= max(2, 2 * small):
            heads[token] = (frozenset(), False)
    _fold_careless(heads, share, raw)
    scores, totals, forms = _scores(heads, share, raw)

    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))
        if word not in listed:
            listed = [word] + listed
        lines.append(lexicon.head(word) + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
