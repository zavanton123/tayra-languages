#!/usr/bin/env python3
"""
Builds a word frequency list for the app: the most common words of a language, as dictionary
words ("dizer"), each with the forms counted for it ("diz", "disse", ...).

Word counts come from the first source that has the language (for Korean, the second and third
blended, counted as whole words that are cut into a word and its particles as the reader cuts them):

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
import json
import math
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
    "id", "mn", "vi", "br", "tok", "sga", "grc", "ar", "arz", "he", "fa", "yi", "aii", "hi", "kn",
    "th", "lo", "km", "zh", "yue", "ja", "ko",
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
    "ko": {"leipzig": ["kor_news_2019_1M"]},
}

# Languages written without spaces: a corpus's runs of letters are cut into words as the app's
# parser cuts them, the longest the dictionary knows first; wordfreq's own lists are cut already.
SEGMENTED = {"zh", "yue", "ja", "th", "lo", "km"}
# Of these, the ones whose letters no word covers make one word together, as in the app.
JOIN_UNKNOWN = {"th", "lo", "km"}

# Korean is counted from whole space-separated words, cut into a word and its particles as the reader
# cuts them (korean_pieces); wordfreq's Korean is counted in morpheme pieces, which no reader shows.
SOURCE_ORDER = {"ko": ["frequencywords", "leipzig"]}
# Languages whose sources are all blended, however many forms the first has.
BLEND = {"ko"}

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
WORD_POS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "article", "intj", "contraction", "particle", "adnominal"}

# Per-language corrections, reviewed by hand.
from frequency_fixes import DISPLAY, EXCLUDE, KOREAN_CUT, KOREAN_STEMS, KOREAN_WHOLE, OVERRIDES  # noqa: E402

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


# Languages whose words are written in Chinese characters or kana.
CJK = {"zh", "yue", "ja"}

# OpenCC's tables, as the app bundles them.
OPENCC = os.path.join(ROOT, "core", "ui", "src", "commonMain", "composeResources", "files", "opencc")
_opencc = {}


def _opencc_table(name):
    if name not in _opencc:
        table = {}
        with open(os.path.join(OPENCC, name), encoding="utf-8") as f:
            for line in f:
                key, _, values = line.rstrip("\n").partition("\t")
                if key and values:
                    table[key] = values.split(" ")[0]
        _opencc[name] = (table, max(map(len, table)))
    return _opencc[name]


def to_traditional(text):
    """[text] in traditional characters as Taiwan writes them (OpenCC's s2tw), each character's usual spelling first."""
    for stage in (("STPhrases.txt", "STCharacters.txt"), ("TWVariantsPhrases.txt", "TWVariants.txt")):
        tables = [_opencc_table(name) for name in stage]
        out, i = [], 0
        while i < len(text):
            for table, longest in tables:
                match = next((text[i:i + n] for n in range(min(longest, len(text) - i), 0, -1) if text[i:i + n] in table), None)
                if match:
                    out.append(table[match])
                    i += len(match)
                    break
            else:
                out.append(text[i])
                i += 1
        text = "".join(out)
    return text


def is_cjk(c):
    return "\u3040" <= c <= "\u30ff" or "\u3400" <= c <= "\u9fff" or "\uf900" <= c <= "\ufaff" or ord(c) >= 0x20000


# The godan endings by the stem's last kana: masu stems in the i row (出し → 出す), and the te and
# ta stems (知っ → 知る, 読ん → 読む, 書い → 書く).
GODAN = {"い": "う", "き": "く", "ぎ": "ぐ", "し": "す", "ち": "つ", "に": "ぬ", "び": "ぶ", "み": "む", "り": "る"}
TE_STEMS = {"っ": "うつる", "ん": "むぶぬ", "い": "くぐ"}
# How common a verb must be, against its stem, to take the stem's count: else a rare verb that only
# looks like the stem (同じる for 同じ) would.
JA_STEM_SHARE = 0.02
# The readings the dictionary's entries give each Japanese word, and the spellings left with no word
# by the related-kanji rule; filled by load_dictionary.
JA_READINGS = {}
JA_ORPHANS = set()


def ja_stem_words(form):
    """The verbs and adjectives a stem the dictionary lacks may be of: 帰っ → 帰る, 飲ん → 飲む, 良かっ → 良い."""
    words = [form + "る"]
    if form.endswith("かっ"):
        words.append(form[:-2] + "い")
    words += [form[:-1] + end for end in TE_STEMS.get(form[-1:], "") + GODAN.get(form[-1:], "")]
    if form.endswith("行っ"):
        words.append(form[:-1] + "く")
    return words


def verb_stem_of(form, verb):
    """Whether [form] is a stem of [verb]: 食べ of 食べる, 出し of 出す, 知っ of 知る."""
    if verb == form + "る":
        return True
    if len(form) < 2 or verb[:-1] != form[:-1]:
        return False
    return verb[-1:] == GODAN.get(form[-1]) or verb[-1:] in TE_STEMS.get(form[-1], "")


def is_kana(word):
    """Whether [word] is written in kana alone."""
    return bool(word) and all("\u3040" <= c <= "\u30ff" for c in word)


def kanji_stem(word):
    """The word without the hiragana that end it (食べる → 食, 出し → 出), or empty for a word of kana alone."""
    stem = word.rstrip("".join(chr(c) for c in range(0x3041, 0x3097)))
    return stem if any(is_cjk(c) and not "\u3040" <= c <= "\u30ff" for c in stem) else ""


# Korean particles, as the reader cuts them off a word (KoreanParser.kt in core/domain/.../parse; keep
# the two the same): each with the sound it follows, "C" a final consonant, "V" a vowel, "R" a vowel or
# ㄹ, "K" a consonant but ㄹ, "" any, and its place in a stack (집에도 is 집 + 에 + 도): the plural 들
# first, then particles of place, manner and company (two may stack: 에서부터), then 도, 은/는 and their
# kind, then the case endings 이/가, 을/를, 의. 당 "per" is among the second, right after a counter only
# (시간당, 개당; 부당 "unjust" is no 부 + 당).
KOREAN_PARTICLES = [
    ("들", "", 0),
    ("에", "", 1), ("에서", "", 1), ("에게", "", 1), ("에게서", "", 1), ("한테", "", 1), ("한테서", "", 1),
    ("께", "", 1), ("께서", "", 1), ("로", "R", 1), ("으로", "K", 1), ("로서", "R", 1), ("으로서", "K", 1),
    ("로써", "R", 1), ("으로써", "K", 1), ("와", "V", 1), ("과", "C", 1), ("하고", "", 1), ("랑", "V", 1),
    ("이랑", "C", 1), ("처럼", "", 1), ("보다", "", 1), ("까지", "", 1), ("부터", "", 1), ("마다", "", 1),
    ("만큼", "", 1), ("대로", "", 1), ("만", "", 1), ("밖에", "", 1), ("뿐", "", 1), ("조차", "", 1), ("마저", "", 1),
    ("당", "", 1),
    ("도", "", 2), ("은", "C", 2), ("는", "V", 2), ("이나", "C", 2), ("나", "V", 2), ("이라도", "C", 2), ("라도", "V", 2),
    ("이든", "C", 2), ("든", "V", 2), ("이든지", "C", 2), ("든지", "V", 2),
    ("이", "C", 3), ("가", "V", 3), ("을", "C", 3), ("를", "V", 3), ("의", "", 3),
]
# The copula 이다 after a noun (학생이에요, 의사예요), last in a stack and after no particle but those of
# place and manner (때문이에요, 뿐이에요). After a vowel its 이 may drop (의사였어요), not in the forms
# that would read as another word (다, 고, 면).
KOREAN_COPULA = [(form, "") for form in (
    "이다", "입니다", "입니까", "이었다", "이었어", "이었어요", "이었습니다", "이었던", "이고", "이지만", "이라고", "이라서",
    "이라면", "이라는", "이란", "인데", "인데요", "인지", "인가", "인가요", "일까", "일까요", "이죠", "이지", "이지요", "이니까",
    "이네요", "이며", "인",
)] + [("이에요", "C"), ("이야", "C")] + [(form, "V") for form in (
    "예요", "야", "였다", "였어", "였어요", "였습니다", "였던", "지만", "라고", "라서", "라면", "라는", "란", "죠", "니까", "네요",
)]
# The polite 요, after a particle only (학교에서요): after a verb it is part of the verb (먹었어요). Nor does
# it follow 와 or 과, which are of writing: 도와요 and 나와요 are forms of 돕다 and 나오다.
KOREAN_POLITE = "요"
KOREAN_NOT_BEFORE_POLITE = {"와", "과"}
KOREAN_PER = "당"
# The copula's past after a vowel (였어요) is no copula after a stem that makes a verb in 이다 (모이다):
# 모였어요 is 모이다's.
KOREAN_COPULA_PAST = "였"
KOREAN_STACK = 4
# 가 does not follow these: before it they are 내, 제 (내가), so 나가 is a form of 나가다.
KOREAN_NOT_BEFORE_GA = {"나", "저"}
# Nor does the copula follow 아니, being 아니다's own: 아니라고 is a form of 아니다.
KOREAN_NOT_BEFORE_COPULA = {"아니"}
# As SegmentingParser weighs Chinese words: a piece costs the log of its frequency rank plus this; a
# word the lexicon has without a rank counts as rank KOREAN_UNRANKED.
KOREAN_WORD_COST = 4.0
KOREAN_UNRANKED = 50_000


def korean_follows(previous, sound):
    """Whether a particle that follows [sound] may come after the syllable [previous]."""
    code = ord(previous) - 0xAC00
    if not 0 <= code < 11172:
        return False
    final = code % 28
    return sound == "" or {"C": final != 0, "V": final == 0, "R": final in (0, 8), "K": final not in (0, 8)}[sound]


def korean_stacks(rest, stem, known=None):
    """Every way [rest] is a stack of particles after [stem], each a list of pieces; [known] is the lexicon's test."""
    out = []
    verb_in_i = known is not None and known(stem + "이다")

    def go(at, last, slot, pieces, place):
        if at == len(rest):
            out.append(pieces)
            return
        if len(pieces) == KOREAN_STACK:
            return
        if pieces and pieces[-1] not in KOREAN_NOT_BEFORE_POLITE and rest.endswith(KOREAN_POLITE) and at + len(KOREAN_POLITE) == len(rest):
            out.append(pieces + [KOREAN_POLITE])
        if slot <= 1 and (pieces or stem not in KOREAN_NOT_BEFORE_COPULA):
            for form, sound in KOREAN_COPULA:
                if not pieces and verb_in_i and form.startswith(KOREAN_COPULA_PAST):
                    continue
                if rest.endswith(form) and at + len(form) == len(rest) and korean_follows(last, sound):
                    out.append(pieces + [form])
        for form, sound, next_slot in KOREAN_PARTICLES:
            if not rest.startswith(form, at) or not korean_follows(last, sound):
                continue
            if form == "가" and not pieces and stem in KOREAN_NOT_BEFORE_GA:
                continue
            if form == KOREAN_PER and (pieces or stem not in KOREAN_COUNTERS):
                continue
            # Two particles of place and manner may stack (에서부터, 까지만); otherwise each comes later than the last.
            if next_slot > slot or (next_slot == slot == 1 and place == 1):
                go(at + len(form), form[-1], next_slot, pieces + [form], place + (next_slot == 1))

    go(0, stem[-1], -1, [], 0)
    return out


def korean_stem_of(lemma, word):
    """
    Whether [lemma], a dictionary form, can be what [word] inflects: its stem begins the word, the
    stem's last syllable in its first letter only (갔어요 of 가다, 몰라 of 모르다, 해 of 하다). The rules
    also give words no reading would (다 of 학교에서).
    """
    stem = lemma[:-1]
    if not lemma.endswith("다") or not stem or len(stem) > len(word):
        return False
    for i, (a, b) in enumerate(zip(stem, word)):
        x, y = ord(a) - 0xAC00, ord(b) - 0xAC00
        if not (0 <= x < 11172 and 0 <= y < 11172):
            return False
        if (x // 588 != y // 588) if i == len(stem) - 1 else (x // 28 != y // 28):
            return False
    # A stem in ㄹ takes no ending in 으 (길다: 길면, 긴, 길), which the rules allow: 길을 is 길 + 을.
    after = ord(word[len(stem)]) - 0xAC00 if len(word) > len(stem) else -1
    return not ((ord(stem[-1]) - 0xAC00) % 28 == 8 and 0 <= after < 11172 and after // 28 == 11 * 21 + 18)


# Endings joined to a verb's stem as they are (하자, 보지, 알리고, 가면), and those after which a stem ending
# in a vowel swallows the 아/어 of the ending (가도, 서서, 내야; and the stem alone: 지내).
KOREAN_PLAIN_ENDINGS = ("지", "자", "고", "면", "게")
KOREAN_VOWEL_ENDINGS = ("", "도", "서", "야")


def korean_verb_readings(form, verbs):
    """The [verbs] whose stem with one of the commonest endings spells [form]: 하자 of 하다, 써 of 쓰다, 봐도 of 보다."""
    out = set()
    for ending in KOREAN_PLAIN_ENDINGS:
        if form.endswith(ending) and len(form) > len(ending):
            out.add(form[:-len(ending)] + "다")
    for ending in KOREAN_VOWEL_ENDINGS:
        if not form.endswith(ending) or len(form) == len(ending):
            continue
        base = form[:len(form) - len(ending)]
        code = ord(base[-1]) - 0xAC00
        if not 0 <= code < 11172 or code % 28:
            continue
        lead, vowel, head = code // 588, code // 28 % 21, base[:-1]
        # 가 of 가다, 내 of 내다; 써 of 쓰다, but not 라 of a stem in 르 (오르다 is 올라); 봐 of 보다, 줘 of
        # 주다, 모여 of 모이다; 해 of 하다.
        stems = [base] if vowel in (0, 1, 4, 5) and base[-1] != "하" else []
        if vowel in (0, 4) and lead != 5:
            stems.append(head + chr(0xAC00 + lead * 588 + 18 * 28))
        stems += [head + chr(0xAC00 + lead * 588 + to * 28) for of, to in ((9, 8), (14, 13), (6, 20)) if vowel == of]
        if base[-1] == "해":
            stems.append(head + "하")
        out.update(stem + "다" for stem in stems)
    return out & verbs


# Endings of spoken Korean that Yomitan's rules do not undo, longest first.
KOREAN_SPOKEN_ENDINGS = ["잖습니까", "잖아요", "잖니", "잖아"]

# How the dictionary glosses a word written with its particle.
KOREAN_FUSED = re.compile(r"(topic-marked form of|allomorph of .* before -가|plural of)")


def korean_fused(word, senses):
    """
    Whether a pack entry is a word with its particle (나는, "topic-marked form of 나"; 내가) or with the
    plural 들 (아이들, "plural of 아이"), which the reader cuts.
    """
    return word[-1:] in ("는", "은", "가", "들") and all(
        KOREAN_FUSED.match(gloss) for sense in json.loads(senses) for gloss in sense.get("glosses", [""]))

# The particles the list counts as words of their own, the dictionary having them only as affixes
# ("-에서"); the copula's forms are counted for 이다. 하고 "with" is a form of 하다 too, which it goes to.
KOREAN_OWN = {form for form, _, _ in KOREAN_PARTICLES} - {"하고"} | {KOREAN_POLITE}
KOREAN_COPULA_FORMS = {form for form, _ in KOREAN_COPULA}

_korean_inflections = {}


def korean_dictionary_forms(word, bare=True):
    """
    The dictionary forms Yomitan's rules give for [word], nearest first, with how many rules each took;
    without [bare], not those that only take the word for a bare stem (사과 of 사과다).
    """
    if word not in _korean_inflections:
        from korean_inflection import dictionary_forms
        _korean_inflections[word] = [(lemma, len(trace), trace != ("어간",)) for lemma, trace in dictionary_forms(word)]
    return [(lemma, steps) for lemma, steps, more in _korean_inflections[word] if bare or more]


def korean_pieces(word, known, rank):
    """
    A Korean word cut as the reader cuts it: the word and the particles after it (친구가 is 친구 + 가), when
    the word before them is one the lexicon [known] knows. Of the cuts and the word left whole, the one
    whose pieces are together the most common wins, by the costs SegmentingParser gives Chinese words
    ([rank] is a word's frequency rank): a word the frequency list has (나가, a form of 나가다; 사과) stays
    whole; one only the dictionary has never does against a cut after a word of the list (집은 is 집 + 은,
    though the dictionary has it as a form of 집다).
    """
    if KOREAN_NATIVE_NUMBER.fullmatch(word):
        return [word]
    best, cut = None, [word]
    after_listed = False
    for end in range(len(word) - 1, 0, -1):
        stem = word[:end]
        if not known(stem) and not (stem.endswith(KOREAN_HONORIFIC) and end > 1 and known(stem[:-1])):
            continue
        for stack in korean_stacks(word[end:], stem, known):
            cost = sum(math.log(rank(piece) or KOREAN_UNRANKED) + KOREAN_WORD_COST for piece in [stem] + stack)
            if rank(stem):
                after_listed = True
            if best is None or cost < best:
                best, cut = cost, [stem] + stack
    if best is None:
        return [word]
    whole = rank(word) or (KOREAN_UNRANKED if known(word) and not after_listed else None)
    return [word] if whole is not None and math.log(whole) + KOREAN_WORD_COST <= best else cut


def korean_counter_cut(word, known, rank, previous):
    """
    A Korean word after [previous], the word before it, cut as a counter and its particles when [previous] is
    a number (한 대가 is 대 + 가, not 대가 "price"; 열한 시야 is 시 + 야) or a word of time before one (다음 주로
    is 주 + 로, not 주로 "mainly"); None otherwise, or when the word is a counter itself.
    """
    if previous is None:
        return None
    counters = KOREAN_COUNTERS if korean_number(previous) or previous == "몇" else KOREAN_TIME_COUNTERS if previous in KOREAN_TIME_WORDS else ()
    best, cut = None, None
    for counter in sorted((c for c in counters if word.startswith(c)), key=len, reverse=True):
        if counter == word:
            return None
        # A counter's stack may hold the copula's past: 다섯 개였어요 is 개 + 였어요, though 개이다 is a verb.
        for stack in korean_stacks(word[len(counter):], counter):
            cost = sum(math.log(rank(piece) or KOREAN_UNRANKED) for piece in stack)
            if best is None or cost < best:
                best, cut = cost, [counter] + stack
        if cut:
            return cut
    return None


def korean_number(word):
    """Whether [word] is a number: in digits, or written out, native (세, 열한, 여든) or Sino-Korean (오, 삼십, 이천이십)."""
    return word.isdigit() or bool(KOREAN_NATIVE_NUMBER.fullmatch(word) or KOREAN_SINO_NUMBER.fullmatch(word))


# Number words before a counter (세 명, 두 시, 열두 개, 여든 살), which stay whole: 여든 is no 여 + 든.
KOREAN_NATIVE_NUMBER = re.compile("(?:열|스물|스무|서른|마흔|쉰|예순|일흔|여든|아흔)(?:한|두|세|석|네|넉|다섯|여섯|일곱|여덟|아홉)?|한|두|세|석|네|넉|다섯|여섯|일곱|여덟|아홉")
# Sino-Korean numbers (오 분, 만 원, 삼십 분), digits between their tens, hundreds, thousands and ten thousands;
# but not 이 "this", 사 or 구 alone, which are more often words.
KOREAN_SINO_NUMBER = re.compile("(?:[이삼사오육칠팔구]?[십백천만])+[일이삼사오육칠팔구]?|[일삼오육칠팔]")
# The honorific 님 after a word (사장님께 is 사장님 + 께 though the lexicon lacks 사장님).
KOREAN_HONORIFIC = "님"
KOREAN_COUNTERS = {
    "개", "명", "분", "시", "시간", "살", "번", "마리", "잔", "병", "권", "장", "대", "층", "원", "년", "월", "일", "주",
    "주일", "주년", "개월", "달", "가지", "그릇", "벌", "켤레", "송이", "번째", "째", "쪽", "초", "세", "인분", "칸", "줄",
    "통", "곡", "편", "채", "군데", "학년", "반", "호", "점", "퍼센트", "도", "배", "박", "박스", "봉지", "상자", "조각",
    "컵", "접시", "바퀴", "정거장", "킬로", "킬로미터", "미터", "킬로그램", "그램", "리터", "센티미터", "호선", "등", "사람",
    "인", "건", "회",
}
# Words of time before a unit of time (다음 주, 지난 달, 이번 해), and those units.
KOREAN_TIME_WORDS = {"다음", "이번", "지난", "저번", "매"}
KOREAN_TIME_COUNTERS = {"주", "달", "해", "번"}


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
    for word, word_lc, p, senses in db.execute("SELECT word, word_lc, pos, senses FROM entries"):
        if language == "ko" and korean_fused(word_lc, senses):
            continue
        pos[word_lc].add(p)
        if p not in WORD_POS and not (language == "ko" and p == "counter"):
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
    if language == "ja":
        # The pack files a kanji word's kana reading as a form of it, so the reader finds the word written
        # in kana ("ところ" for 所). A reading that is a word of its own is counted for itself: else "ます",
        # the polite ending, would go to 増す "to increase".
        try:
            aliases = {(reading, lower(language, word)) for reading, word in db.execute("SELECT reading, word FROM readings")}
        except sqlite3.OperationalError:
            aliases = set()
        for form_lc, of in lemmas.items():
            if form_lc in pos:
                lemmas[form_lc] = [lemma for lemma in of if (form_lc, lemma) not in aliases]
        # The pack also files a kanji word under words it can be read as (秋 under 時, 彼 under あれ, 美しい
        # under 厳しい): a kanji form belongs to a word sharing a kanji with it, or to a reading its entries give.
        readings = defaultdict(set)
        for word, reading in db.execute("SELECT e.word_lc, r.reading FROM entries e JOIN entry_readings r ON r.entry_id = e.id"):
            readings[word].add(reading)
        kun = {c: k.split() for c, k in db.execute("SELECT character, kunyomi FROM kanji")}
        # A kana form belongs to a kanji word it is a reading of, or to a verb or adjective it may be a form of
        # (おり of 降りる, not of 俺).
        for form_lc, of in lemmas.items():
            kanji = {c for c in form_lc if is_cjk(c) and not is_kana(c)}
            if not kanji:
                lemmas[form_lc] = [l for l in of if is_kana(l) or form_lc in readings[l] or pos.get(l, set()) & {"verb", "adj"}]
                continue
            # A verb or adjective of one kanji is read as its kun reading with the same kana (居る as いる).
            tail = form_lc[1:] if len(kanji) == 1 and form_lc[0] in kanji and is_kana(form_lc[1:]) else None
            own = readings[form_lc] | ({r.replace(".", "") for r in kun.get(form_lc[0], []) if r.partition(".")[2] == tail} if tail else set())
            kept = [l for l in of if kanji & set(l) or (is_kana(l) and l in own)]
            # A spelling whose only links were to words read alike (超える, filed under 越える) is a word itself.
            if of and not kept:
                JA_ORPHANS.add(form_lc)
            lemmas[form_lc] = kept
        JA_READINGS.update(readings)
    if language in ("zh", "yue"):
        # A simplified spelling is a form of every traditional word written with it, variant and dialect
        # spellings among them (我们 of 我們 and of 姆們); its count goes to the main word: the most common
        # one, as wordfreq counts a traditional spelling under its simplified one, or else the one with the most senses.
        senses = dict(db.execute("SELECT word_lc, SUM(json_array_length(senses)) FROM entries GROUP BY word_lc"))
        try:
            from wordfreq import word_frequency
            weight = lambda lemma: (word_frequency(lemma, "zh"), senses.get(lemma, 0))  # noqa: E731
        except ImportError:
            weight = lambda lemma: (0, senses.get(lemma, 0))  # noqa: E731
        for form_lc, of in lemmas.items():
            # A character goes to its usual traditional spelling, as OpenCC writes it, or stays itself when it is
            # one (了, not 瞭; 只, not 隻): wordfreq counts every spelling of a character alike.
            if len(form_lc) == 1 and "character" in pos.get(form_lc, set()):
                usual = to_traditional(form_lc)
                lemmas[form_lc] = [] if usual == form_lc else [usual] if usual in of else of
                continue
            if len(set(of)) > 1:
                best = max(weight(lemma) for lemma in of)
                lemmas[form_lc] = [lemma for lemma in of if weight(lemma) == best]
    for form_lc, of in lemmas.items():
        if len(set(of)) > MAX_LEMMAS_PER_FORM:
            # A kana spelling of many kanji words read alike (かかる of 掛かる, 係る, 罹る ...) stays with them.
            ja_reading = language == "ja" and is_kana(form_lc) and len(form_lc) > 2 and not set(form_lc) & set("ゃゅょ") and any(form_lc in JA_READINGS.get(l, ()) for l in of)
            lemmas[form_lc] = of if ja_reading else []
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
    order = [source] if source else SOURCE_ORDER.get(language, ["wordfreq", "frequencywords", "leipzig", "wikipedia", "morphgnt"])
    found = []
    for name in order:
        result = SOURCES[name](language, 200_000)
        if result and language in SEGMENTED and name != "wordfreq":
            result = (resegment(language, result[0]), result[1])
        if result:
            found.append((name, *result))
            if len(found) == 1 and len(result[0]) >= ENOUGH_FORMS and language not in BLEND:
                break
    if not found:
        sys.exit(f"No frequency source has {language}")
    if language == "ko":
        return build_korean(found, size)
    return build_list(language, found, size)


# The reader cuts a Korean word by how common its pieces are, as the list it ships with ranks them, so
# the Korean list is built again from cuts made with the last one's ranks until they no longer change
# it, at most KOREAN_ROUNDS times.
KOREAN_ROUNDS = 6
KOREAN_LETTERS = re.compile("[\uac00-\ud7a3\u1100-\u11ff\u3130-\u318f]+")


def list_ranks(text):
    """The words of a built list, with their forms, and each one's rank, as LexiconService gives the reader them."""
    words, ranks = set(), {}
    rank = 0
    for line in text.splitlines():
        if line.startswith("#") or not line.strip():
            continue
        rank += 1
        word, _, forms = line.partition("\t")
        forms = [f for f in forms.split(" ") if f]
        key = word.lower()
        words.update(forms + [key])
        for form in forms + [key]:
            ranks.setdefault(form, rank)
    return words, ranks


# How much commoner a word must be than its beginning with any other particle to be read as a verb's form
# that ends like a particle: 가는 (가다) is far commoner than 가에, 가를; 나는 is not than 나한테. A word
# the dictionary has needs only be as common.
KOREAN_VERB_MARGIN = 3.0
KOREAN_WORD_MARGIN = 1.0
# A noun spelled as a verb's form is the verb's when it is KOREAN_CASELESS times as common alone as with
# all of KOREAN_CASES together, and the verb KOREAN_VERB_SHARE times as common as it.
KOREAN_CASES = ["이", "가", "을", "를", "의", "에", "에서", "에게", "로", "으로", "와", "과"]
KOREAN_CASELESS = 10.0
KOREAN_VERB_SHARE = 2.0


def build_korean(found, size):
    db = sqlite3.connect(os.path.join(DICTIONARIES, "ko-en.sqlite"))
    # As the reader's lexicon has them with the pack installed (Dictionary.sq's selectWords).
    pack = {w for (w,) in db.execute("SELECT word_lc FROM entries UNION SELECT form_lc FROM forms") if w and " " not in w}
    verbs = {w for (w,) in db.execute("SELECT word_lc FROM entries WHERE pos IN ('verb', 'adj')")} - {"이다"}
    lemmas = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        if lemma in verbs:
            lemmas[form].add(lemma)
    share = defaultdict(float)
    for _, counts, _ in found:
        total = sum(counts.values())
        for token, count in counts.items():
            for run in KOREAN_LETTERS.findall(token):
                share[run] += count / total / len(found)

    def first_cut(run):
        """The cut with the longest word the dictionary knows, before the list's ranks can choose among them."""
        for end in range(len(run) - 1, 0, -1):
            if run[:end] in pack:
                stacks = korean_stacks(run[end:], run[:end], pack.__contains__)
                if stacks:
                    return run[:end], stacks[0]
        return None

    # A word that may be a verb's form or a noun with a particle (가는: 가다, or 가 + 는; 나는: 나다, or 나 + 는)
    # is the verb's when it is KOREAN_VERB_MARGIN times as common as the noun with any other particle (one
    # that makes no verb's form), as a noun takes many. These are kept whole when the first list is made;
    # the list then has them as forms, by which the reader keeps them whole too. KOREAN_WHOLE and
    # KOREAN_CUT correct it word by word.
    entries = {w for (w,) in db.execute("SELECT word_lc FROM entries")}
    cuttable = {run: cut for run in share if (cut := first_cut(run))}
    with_particle = defaultdict(list)
    particles = {form for form, _, _ in KOREAN_PARTICLES}
    for run, (stem, stack) in cuttable.items():
        if len(stack) == 1 and stack[0] in particles and run not in entries:
            with_particle[stem].append((share[run], run))

    def verbal(run):
        """Whether [run] may be a verb's form: by the pack, or by Yomitan's rules past the bare stem, which any word is."""
        return bool(lemmas.get(run)) or any(
            lemma in verbs and steps and korean_stem_of(lemma, run) for lemma, steps in korean_dictionary_forms(run, bare=False))

    def reads_as_verb(run, stem):
        # 하고 after a noun that makes a verb with 하다 is that verb's (공부하고), not "with" (친구하고).
        if run == stem + "하고" and stem + "하다" in verbs:
            return True
        if not verbal(run):
            return False
        rival = max((x for x, other in with_particle[stem] if other != run and not verbal(other)), default=0.0)
        return share[run] >= KOREAN_VERB_MARGIN * rival

    # The reader leaves a word the dictionary has whole only when the list has it, so the first cut keeps
    # whole the dictionary's words that are as common as their beginning with any other particle (바로, not
    # 바 + 로; 개인, not 개 + 인), and cuts its look-alikes (길이, "length", is mostly 길 + 이).
    def reads_as_word(run, stem):
        if run not in entries or not pos_of.get(run, set()) & WORD_POS:
            return False
        rival = max((x for x, other in with_particle[stem] if other != run and not verbal(other)), default=0.0)
        return share[run] >= KOREAN_WORD_MARGIN * rival

    pos_of = defaultdict(set)
    for word, p, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if not korean_fused(word, senses):
            pos_of[word].add(p)
    seeds = {run for run, (stem, _) in cuttable.items() if reads_as_verb(run, stem) or reads_as_word(run, stem)}
    # A noun of the pack spelled as a common verb's form (하자 of 하다, 보지 of 보다, 가도 of 가다, 알리고 of
    # 알리다) is the verb's when it seldom takes a case particle, which a noun takes and a verb's form never
    # does, and the verb's other forms are KOREAN_VERB_SHARE times as common as it.
    mass = defaultdict(float)
    for run, x in share.items():
        for lemma in lemmas.get(run, ()):
            mass[lemma] += x
    verbal_nouns = {}
    for run, x in share.items():
        kinds = pos_of.get(run, set()) & WORD_POS
        if not kinds or kinds - {"noun"}:
            continue
        readings = [v for v in korean_verb_readings(run, verbs) if mass[v] - (x if v in lemmas.get(run, ()) else 0) >= KOREAN_VERB_SHARE * x]
        cased = sum(share.get(run + p, 0.0) for p in KOREAN_CASES if korean_stacks(p, run))
        if readings and x >= KOREAN_CASELESS * cased:
            verbal_nouns[run] = max(readings, key=mass.get)
    seeds = (seeds | KOREAN_WHOLE) - KOREAN_CUT
    # The first cut is by wordfreq's ranks, whose Korean is counted in pieces: particles and nouns, few verbs.
    import wordfreq
    frequencies = wordfreq.get_frequency_dict("ko", wordlist="best")
    ranks = {w: i + 1 for i, w in enumerate(sorted(frequencies, key=lambda w: -frequencies[w]))}
    # Only the seeds are left whole by rank: wordfreq has some words with a particle as one piece (나야).
    for run in cuttable:
        ranks.pop(run, None)
    ranks.update({run: 1 for run in seeds})
    # Nor does the first cut leave whole the others the dictionary has (나야, a form of 나다 to it).
    lexicon = pack - (cuttable.keys() - seeds)
    cuts = {}
    text = label = None
    for round_ in range(KOREAN_ROUNDS + 1):
        known, rank = lexicon.__contains__, ranks.get
        changed = 0.0
        pieces = []
        for name, counts, source_label in found:
            out = defaultdict(int)
            whole = sum(counts.values())
            for token, count in counts.items():
                for run in KOREAN_LETTERS.findall(token):
                    cut = korean_pieces(run, known, rank)
                    if cuts.get(run, cut) != cut:
                        changed += count / whole
                    cuts[run] = cut
                    for piece in cut:
                        out[piece] += count
            pieces.append((name, dict(out), source_label))
        print(f"ko round {round_}: {changed / len(found):.2%} of words cut otherwise", file=sys.stderr)
        if round_ and changed / len(found) < 0.0005 or round_ == KOREAN_ROUNDS:
            break
        # A word left whole that the reader could cut must be in the list for the reader to leave it whole.
        whole_words = {run for run in cuttable if cuts.get(run) == [run]} - KOREAN_CUT
        text, label = build_list("ko", pieces, size, required=whole_words, verbal_nouns=verbal_nouns)
        words, ranks = list_ranks(text)
        # The words cut by hand stay out of the list (build_list leaves them out), so the reader cuts them.
        lexicon = pack | words
    return text, label


def build_list(language, found, size, required=frozenset(), verbal_nouns=None):
    """The list from the sources' counts of the words as the reader cuts them."""
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
    verbal_nouns = verbal_nouns or {}
    if language == "ko":
        # Listed, a word cut by hand would be left whole by the reader.
        excluded = excluded | KOREAN_CUT

    # The words simplemma found for forms the dictionary lacks, as they are written.
    guessed = {}
    lemmatizer = simplemma_for(language)

    # How common a Korean word is with its forms that are no other word's: a verb's dictionary form is
    # rare in text, so a form of several verbs (가는, of 가다 and 갈다) is shared by this.
    mass = {}

    def weight(word):
        if language != "ko":
            return count(word)
        if not mass:
            for form, share in freq.items():
                mass[form] = mass.get(form, 0) + share
                of = set(lemmas.get(form, [])) - {form}
                if len(of) == 1 and not is_word(form):
                    lemma = of.pop()
                    mass[lemma] = mass.get(lemma, 0) + share
        return mass.get(word, 0)

    def guess(form):
        """simplemma's word for a form the dictionary does not have, or None for a name or an unknown form."""
        if language == "ko":
            return korean_guess(form)
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

    def korean_guess(form):
        """The verb or adjective a Korean form the dictionary lacks inflects, by Yomitan's rules: the nearest, then the commonest."""
        found = [(steps, -weight(lemma), lemma) for lemma, steps in korean_dictionary_forms(form)
                 if korean_stem_of(lemma, form) and pos.get(lemma, set()) & {"verb", "adj"} and lemma in common and lemma not in excluded
                 and weight(lemma) >= count(form)]
        if not found:
            # Spoken endings the rules lack: 있잖아 "you know, there is" of 있다.
            stem = next((form[:-len(ending)] for ending in KOREAN_SPOKEN_ENDINGS if form.endswith(ending) and len(form) > len(ending)), None)
            if stem and stem + "다" in common and pos.get(stem + "다", set()) & {"verb", "adj"}:
                return stem + "다"
            # The polite 요 after an ending the rules know without it (같네요, 있나요, 할게요).
            if form.endswith(KOREAN_POLITE) and len(form) > 2:
                base = form[:-1]
                verbs = [l for l in lemmas.get(base, []) if pos.get(l, set()) & {"verb", "adj"} and l in common]
                return max(verbs, key=weight) if verbs else korean_guess(base)
        return min(found)[2] if found else None

    def is_word(w):
        # Chinese and Japanese text quotes Latin words, and Wikipedia's leaves markup; the dictionaries have some.
        if language in CJK and not any(is_cjk(c) for c in w):
            return False
        if w in guessed and w not in pos:
            return True
        # The dictionary files a Chinese character under "character", not as a word; one that wordfreq
        # counts on its own, as jieba cuts text, is a word (的, 是, 吃).
        if language in ("zh", "yue") and len(w) == 1 and "character" in pos.get(w, set()):
            return w not in excluded
        # Korean counts with counters, words of their own (세 마리, three animals).
        kinds = pos.get(w, set()) & (WORD_POS | ({"counter"} if language == "ko" else set()))
        if not kinds or w not in common or w in excluded:
            return False
        # Korean has words written with Chinese characters too (眞짜 for 진짜), which texts write in Hangul.
        if language == "ko" and not KOREAN_LETTERS.fullmatch(w):
            return False
        # Single letters are kept only as the little words they can be ("a", "e", "o").
        return len(w) > 1 or language in ("ja", "ko") or bool(kinds & {"prep", "conj", "article", "pron", "det"})

    try:
        from wordfreq import word_frequency
    except ImportError:
        word_frequency = None

    def count(word):
        """
        How common a word's own spelling is; wordfreq has German ß words under ss ("heissen"), and
        Chinese under its simplified spelling, which it finds for a traditional one (我們 as 我们).
        """
        if language in ("zh", "yue") and word not in freq and word_frequency:
            return word_frequency(word, "zh")
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
        elif language == "ko" and form in KOREAN_OWN:
            return {form: 1.0}
        elif language == "ko" and form in verbal_nouns and is_word(verbal_nouns[form]):
            return {verbal_nouns[form]: 1.0}
        elif language == "ko" and any(form.startswith(KOREAN_STEMS[verb]) and verb in lemmas.get(form, []) for verb in KOREAN_STEMS):
            return {next(verb for verb in KOREAN_STEMS if form.startswith(KOREAN_STEMS[verb])): 1.0}
        elif language == "ko" and form in KOREAN_COPULA_FORMS:
            return {"이다": 1.0}
        elif language == "ko" and form.endswith("다") and is_word(form) and pos[form] & {"verb", "adj"} and not any(
                steps and korean_stem_of(lemma, form) and lemma in lemmas.get(form, []) for lemma, steps in korean_dictionary_forms(form)):
            # A dictionary form is no form of another word, as the pack has some (들다 of 먹다, 다르다 of 다른),
            # unless it inflects it (없었다 of 없다).
            return {form: 1.0}
        else:
            # A simplified character (说) is no word in the dictionary but a form of its traditional one (說).
            if len(form) == 1 and not is_word(form) and language not in ("zh", "yue", "ko"):
                return {}
            known = [s for s in spellings(form) if s in lemmas or s in pos]
            if not known and language == "ja" and len(form) > 1:
                verbs = [v for v in ja_stem_words(form) if pos.get(v, set()) & {"verb", "adj"} and is_word(v)]
                if verbs:
                    weights = {v: count(v) or 1e-12 for v in verbs}
                    return {v: w / sum(weights.values()) for v, w in weights.items()}
            if not known:
                word = guess(form)
                return {word: 1.0} if word else {}
            candidates = [l for s in known for l in ([s] if s != form else []) + lemmas.get(s, [])]
            # A Chinese or Japanese name ("日本", "美国") is filed with its poetic synonyms (神国, 金山) as their form.
            # A word filed under a name too (円 under the surname 円谷) keeps its count; only the name is left out.
            if language in CJK:
                is_name = lambda l: bool(pos.get(l)) and pos[l] <= {"name"}  # noqa: E731
                if is_name(form) or (candidates and all(is_name(l) for l in candidates)):
                    return {}
                candidates = [l for l in candidates if not is_name(l)]
            others = list(dict.fromkeys(l for l in candidates if l != form and is_word(l)))
            # A simplified word whose traditional entries are all variant spellings (哪里 of 哪裏) goes to the
            # spelling Taiwan writes (哪裡), when that is a word.
            if language in ("zh", "yue") and not others and not is_word(form):
                usual = to_traditional(form)
                if usual != form and is_word(usual):
                    others = [usual]
        # A Japanese verb or adjective stem (食べ, 出し, 見) goes to its word (食べる, 出す, 見る), the same
        # kanji with other kana after them, as the reader looks the stem up, however common the stem is.
        # A noun made from a verb (考え "idea", 終わり "end") stays a word of its own, sharing its count below.
        if language == "ja" and is_kana(form):
            # A kana piece (しよ of しよう) gives its count only to a kanji word written with it that is common itself.
            others = [l for l in others if is_kana(l) or count(l) >= JA_STEM_SHARE * count(form) or (len(form) > 2 and not set(form) & set("ゃゅょ") and form in JA_READINGS.get(l, ()))]
            # A kana spelling of several kanji words read alike counts for the commonest (かかる for 掛かる);
            # two kana are more often a piece of a longer word (しさ of らしさ) than a word written in kana.
            if not is_word(form) and len(others) > 1 and not any(is_kana(l) for l in others):
                # まつ is 待つ, not 末: a kana spelling ending as a verb ends goes to a verb.
                verbs = [l for l in others if form[-1] in "うくぐすつぬぶむる" and "verb" in pos.get(l, set())]
                others = [max(verbs or others, key=count)]
        stem_of = [] if language != "ja" or form in overrides or pos.get(form, set()) & {"adj"} or (len(form) > 1 and "noun" in pos.get(form, set())) else [
            l for l in others
            if "verb" in pos.get(l, set()) and kanji_stem(l) and verb_stem_of(form, l) and count(l) >= JA_STEM_SHARE * count(form)
        ]
        # A noun that is also a verb's stem (教え "teaching" and the 教え of 教えて) is mostly the verb in
        # wordfreq's pieces, which the reader joins into the verb: the two share its count.
        if language == "ja" and not stem_of and form not in overrides and len(form) > 1 and "noun" in pos.get(form, set()) and is_word(form):
            verbs = [
                l for l in others
                if "verb" in pos.get(l, set()) and kanji_stem(l) and verb_stem_of(form, l) and count(l) >= JA_STEM_SHARE * count(form)
            ]
            if verbs:
                return {form: 0.5, **{verb: 0.5 / len(verbs) for verb in verbs}}
        if stem_of:
            others = stem_of
        # A form whose only entry is an interjection ("foi", "nossa") is counted for the word it is a form of.
        # Japanese files a set phrase under its variants (ありがとう under ありがと), so there it counts for itself.
        elif is_word(form) and form not in overrides and not (others and pos[form] & WORD_POS <= {"intj"} and language != "ja"):
            # A form that is a word itself keeps its count ("casa", not "casar") unless [TAKEOVER] says otherwise.
            # Korean's forms tables are full of nouns spelled as some verb's form (사실 of 사다, 우려 of 우리다),
            # so there a word keeps its count, and the few that are a verb's form are overrides (해 of 하다).
            if language == "ko" and not pos[form] & {"verb", "adj"}:
                return {form: 1.0}
            inflected = bool(pos[form] & INFLECTED_KINDS)
            mine = weight(form)
            others = [
                l for l in others
                if weight(l) >= TAKEOVER * mine or (inflected and pos[l] & INFLECTED_KINDS and weight(l) >= INFLECTED_TAKEOVER * mine)
            ]
            if not others:
                return {form: 1.0}
        if language == "ko" and form not in overrides:
            # A Korean form goes only to words at least as common as it with their forms: the pack and the
            # rules also tie forms to rare verbs that only look alike (미니, a name, to 미다).
            others = [l for l in others if weight(l) >= count(form)]
        if not others:
            return {form: 1.0} if form in JA_ORPHANS else {}
        weights = {l: weight(l) or 1e-12 for l in others}
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
        counted = sorted(forms[word], key=lambda f: -forms[word][f])
        counted = counted[:FORMS_PER_WORD] + [f for f in counted[FORMS_PER_WORD:] if f in required]
        if language == "ja" and any(is_cjk(c) and not is_kana(c) for c in word):
            # A kanji spelling reached through a kana word read alike (枯れる through かれる to 狩る, 湧く through
            # わく to 分ける) is another word: a kanji word's forms share a kanji with it.
            counted = [f for f in counted if not any(is_cjk(c) and not is_kana(c) for c in f) or set(f) & set(word)]
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
