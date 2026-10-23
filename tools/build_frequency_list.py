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

--source forces one of them. The counts are of word forms (in Arabic, of the words left by cutting off the
letters written onto their front, as the reader cuts them); each form is credited to the dictionary
words it is a form of, using the forms table of the language's offline dictionary pack
(dictionaries/<code>-en.sqlite, made by tools/build_dictionary.py; for Arabic, the tagged forms of
the wty dictionary the pack is made from), or for a form the pack does not have, the simplemma lemmatizer (pip install simplemma), whose names are capitalised and left out. A form that is also a word in
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
# Romanian is counted from web text and subtitles alone, as its list was first: Wikipedia's adds the words of its
# own pages ("licențiere", "demografie") to the first thousand.
SOURCE_ORDER["ro"] = ["wordfreq", "frequencywords"]
# So is Slovak, whose courses were written against that list.
SOURCE_ORDER["sk"] = ["wordfreq", "frequencywords"]
# Languages whose sources are all blended, however many forms the first has.
BLEND = {"ko"}
# Galician is counted from subtitles alone, as its list was first built (wordfreq has no Galician, and the Wikipedia
# dump blended in after FrequencyWords would add encyclopaedic words).
SOURCE_ORDER["gl"] = ["frequencywords"]

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
from frequency_fixes import ARABIC_CUT, ARABIC_WHOLE, DISPLAY, EXCLUDE, KOREAN_APART, KOREAN_CUT, KOREAN_STEMS, KOREAN_WHOLE, OVERRIDES, SLOVAK_NEGATIVE_VERBS  # noqa: E402

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
# (시간당, 개당; 부당 "unjust" is no 부 + 당), and so are 쯤 "about", 씩 "each", 째 and 짜리 (세 시쯤에, 두 개씩,
# 사흘째, 천 원짜리), and 에다 "onto" (벽에다가). 엔, 에선, 에겐 and 한텐 are 에, 에서, 에게 and 한테 with 는 (이번엔, 집에선).
KOREAN_PARTICLES = [
    ("들", "", 0),
    ("에", "", 1), ("에서", "", 1), ("에게", "", 1), ("에게서", "", 1), ("한테", "", 1), ("한테서", "", 1),
    ("에다", "", 1), ("에다가", "", 1), ("께", "", 1), ("께서", "", 1), ("로", "R", 1), ("으로", "K", 1), ("로서", "R", 1), ("으로서", "K", 1),
    ("로써", "R", 1), ("으로써", "K", 1), ("와", "V", 1), ("과", "C", 1), ("하고", "", 1), ("랑", "V", 1),
    ("이랑", "C", 1), ("처럼", "", 1), ("보다", "", 1), ("까지", "", 1), ("부터", "", 1), ("마다", "", 1),
    ("만큼", "", 1), ("대로", "", 1), ("만", "", 1), ("밖에", "", 1), ("뿐", "", 1), ("조차", "", 1), ("마저", "", 1),
    ("당", "", 1), ("쯤", "", 1), ("씩", "", 1), ("째", "", 1), ("짜리", "", 1),
    ("엔", "", 2), ("에선", "", 2), ("에겐", "", 2), ("한텐", "", 2), ("도", "", 2), ("은", "C", 2), ("는", "V", 2), ("이나", "C", 2), ("나", "V", 2), ("이라도", "C", 2), ("라도", "V", 2),
    ("이든", "C", 2), ("든", "V", 2), ("이든지", "C", 2), ("든지", "V", 2),
    ("이", "C", 3), ("가", "V", 3), ("을", "C", 3), ("를", "V", 3), ("의", "", 3),
]
# The copula 이다 after a noun (학생이에요, 의사예요), last in a stack and after no particle but those of
# place and manner (때문이에요, 뿐이에요). After a vowel its 이 may drop (의사였어요, 정도면, 고양이다), not in the
# form that would read as another word (고); 면 "if it is" only where the word whole is no other (KOREAN_COPULA_IF).
KOREAN_COPULA = [(form, "") for form in (
    "이다", "입니다", "입니까", "이었다", "이었어", "이었어요", "이었습니다", "이었던", "이고", "이지만", "이라고", "이라서",
    "이라면", "이라는", "이란", "인데", "인데요", "인지", "인가", "인가요", "일까", "일까요", "이죠", "이지", "이지요", "이니까",
    "이네요", "이며", "인", "이었기", "이었는지", "이었는데", "이었고", "이었지만", "이었을", "이구나", "이군요", "이던데", "이던데요",
    "이시라고", "이세요", "이라", "이어서", "이었다고", "이었다면", "이었으면", "이자", "일", "이기", "이라며", "이라니", "이냐",
    "이냐고", "이냐며", "이잖아", "이잖아요", "이네", "이면서", "이시다", "이십니다", "이셨다", "이셨어요", "이신", "이래", "이래요",
)] + [("이에요", "C"), ("이야", "C"), ("이면", "C")] + [(form, "V") for form in (
    "예요", "야", "였다", "였어", "였어요", "였습니다", "였던", "지만", "라고", "라서", "라면", "라는", "란", "죠", "니까", "네요", "면",
    "였기", "였는지", "였는데", "였고", "였지만", "였을", "구나", "군요", "다", "여서", "였다고", "였다면", "였으면", "라며", "라니",
    "냐", "냐고", "냐며", "잖아", "잖아요", "면서", "래", "래요", "시라고", "십니다", "셨다", "셨어요",
)]
# Forms after a vowel that a verb's stem takes too (가냐, 하잖아, 보면서): after a stem that makes a verb with 다 or 시다,
# ends a verb's ending (가려다) or is honorific (아시잖아요), they are the verb's.
KOREAN_COPULA_VOWEL_VERBAL = {
    "다", "여서", "였다고", "였다면", "였으면", "라며", "라니", "냐", "냐고", "냐며", "잖아", "잖아요", "면서", "래", "래요",
    "시라고", "십니다", "셨다", "셨어요",
}
KOREAN_HONORIFIC_STEM = "시"
# Forms a verb in 이다 takes too (보이자, 먹일, 보이기): after a stem that makes such a verb, they are its.
KOREAN_COPULA_VERBAL_I = {
    "이라", "이어서", "이었다고", "이었다면", "이었으면", "이자", "일", "이기", "이라며", "이라니", "이냐", "이냐고", "이냐며",
    "이잖아", "이잖아요", "이네", "이면서", "이시다", "이십니다", "이셨다", "이셨어요", "이신", "이래", "이래요",
}
# Forms of the copula that are words of their own too (다 "all", 일 "work", 이자 "interest"): they stay whole as a word, and
# follow no word that comes before a noun (무슨일 is 무슨 일) or the particle 에 (벽에다 is 벽 + 에다), and those of
# KOREAN_COPULA_LONG a word of two syllables or more only (한일, 위다 are no copula's); cut off a noun, they count for 이다
# without being listed under it (KOREAN_COPULA_FORMS).
KOREAN_COPULA_WORDS = {
    "다", "일", "이자", "이기", "래", "이래", "이래요", "여서", "이어서", "십니다", "이십니다", "셨다", "이셨다", "셨어요",
    "이셨어요", "이신",
}
KOREAN_COPULA_LONG = {"다", "일", "이자", "래", "이래", "이래요"}
KOREAN_DETERMINERS = {"아무", "무슨", "그런", "이런", "저런", "어떤", "다른", "모든"}
# Forms a verb ends in too (가세요, 크구나, 보이던데요): after a stem that makes a verb with 다 or 이다, they are its.
KOREAN_COPULA_VERBAL = {"이구나", "이군요", "이던데", "이던데요", "이시라고", "이세요", "구나", "군요"}
KOREAN_COPULA_IF = {"이면", "면"}
KOREAN_COPULA_VOWEL_IF = "면"
KOREAN_NOT_BEFORE_IF = set("다려라자")
# The polite 요, after a particle only (학교에서요): after a verb it is part of the verb (먹었어요). Nor does
# it follow 와 or 과, which are of writing: 도와요 and 나와요 are forms of 돕다 and 나오다.
KOREAN_POLITE = "요"
KOREAN_NOT_BEFORE_POLITE = {"와", "과"}
KOREAN_PER = "당"
# The copula's past and 여서 after a vowel (였어요) are no copula after a stem that makes a verb in 이다 (모이다):
# 모였어요 and 조여서 are 모이다's and 조이다's.
KOREAN_COPULA_PAST = ("였", "여")
KOREAN_STACK = 4
# 가 does not follow these: before it they are 내, 제 (내가), so 나가 is a form of 나가다.
KOREAN_NOT_BEFORE_GA = {"나", "저"}
# Nor does the copula follow 아니, being 아니다's own (아니라고 is a form of 아니다), or 할, 하다's (할인 is no 할 + 인).
KOREAN_NOT_BEFORE_COPULA = {"아니", "할"}
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


def korean_stacks(rest, stem, known=None, rank=None):
    """
    Every way [rest] is a stack of particles after [stem], each a list of pieces; [known] is the lexicon's test and
    [rank] its frequency ranks.
    """
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
                # 면 "if it is" (정도면, 학생이면) only where the word whole is no other (먹이면 of 먹이다), nor after a
                # verb's stem (받아들이면) or ending (한다면, 가려면, 아니라면, 하자면), which take 면 too.
                before = stem + rest[:len(rest) - len(form)]
                if form in KOREAN_COPULA_IF and known is not None and rest.endswith(form) and (
                        known(stem + rest) or known(before + "다") or before[-1] in KOREAN_NOT_BEFORE_IF and known(before[:-1] + "다")):
                    continue
                if form in KOREAN_COPULA_VERBAL and known is not None and rest.endswith(form) and (
                        known(before + "다") or form[0] == "이" and known(before + "이다")):
                    continue
                if known is not None and rest.endswith(form) and korean_copula_is_verbs(form, sound, before, stem + rest, known):
                    continue
                if form in KOREAN_COPULA_WORDS and pieces and pieces[-1] == "에":
                    continue
                # A verb's command quoted (익히라고, 보내라는) where the verb is commoner than the word its stem spells.
                if form[0] == "라" and sound == "V" and rank is not None and rest.endswith(form) and rank(before + "다") and (
                        not rank(before) or rank(before + "다") < rank(before)):
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


def korean_copula_is_verbs(form, sound, before, word, known):
    """
    Whether the copula's [form] after [before] is a verb's instead, by the lexicon's test [known]: one a verb in 이다
    takes too after a stem that makes one (보이자, 먹일); one in 라 after a vowel where 라 ends a verb's stem (모자라서,
    자라고); one verbs end in after a vowel (KOREAN_COPULA_VOWEL_VERBAL) after a stem that makes a verb with 다 or 시다,
    ends a verb's ending or is honorific (가냐, 가려다, 아시잖아요); and a form that is a word itself (KOREAN_COPULA_WORDS)
    in a [word] the lexicon has (개다, 기념일), after a word that comes before a noun (무슨일), or, for KOREAN_COPULA_LONG,
    after a word of one syllable (한일).
    """
    if form in KOREAN_COPULA_VERBAL_I and known(before + "이다"):
        return True
    if sound == "V" and form[0] == "라" and known(before + "라다"):
        return True
    if form in KOREAN_COPULA_VOWEL_VERBAL and (known(before + "다") or known(before + "시다") or before[-1] == KOREAN_HONORIFIC_STEM
                                               or before[-1] in KOREAN_NOT_BEFORE_IF and known(before[:-1] + "다")):
        return True
    return form in KOREAN_COPULA_WORDS and (known(word) or before in KOREAN_DETERMINERS or form in KOREAN_COPULA_LONG and len(before) < 2)


# The final consonants a verb's stem may lose or change (none, ㄷ, ㄹ, ㅂ, ㅅ, ㅎ), as Hangul numbers them.
KOREAN_SOFT_FINALS = {0, 7, 8, 17, 19, 27}


def korean_stem_of(lemma, word, strict=True):
    """
    Whether [lemma], a dictionary form, can be what [word] inflects: its stem begins the word, the
    stem's last syllable in its first letter only (갔어요 of 가다, 몰라 of 모르다, 해 of 하다). The rules
    also give words no reading would (다 of 학교에서). Only [strict]ly, the stem's last syllable changes as a
    verb's does (땄다 is no 뜨다's, 끄라고 no 끌다's), and no particle follows the stem (비키의).
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
    final = (ord(stem[-1]) - 0xAC00) % 28
    if final == 8 and 0 <= after < 11172 and after // 28 == 11 * 21 + 18:
        return False
    # Only a stem in ㄷ, ㄹ, ㅂ, ㅅ or ㅎ loses or changes its last consonant (들어 of 듣다, 사는 of 살다, 도와 of
    # 돕다); the rules, which read the letters apart, also take 부터요 for 붙어요.
    if not (final in KOREAN_SOFT_FINALS or word[len(stem) - 1] == stem[-1]):
        return False
    if not strict:
        return True
    # Nor does a verb's form end in a particle after its stem (비키의, 졸리와 are names').
    rest = word[len(stem):]
    if word.startswith(stem) and (rest in KOREAN_NO_VERB_ENDINGS or rest == "을" and not final):
        return False
    return korean_last_syllable_fits(stem, word[len(stem) - 1], word[len(stem):len(stem) + 1])


# A verb's stem's last vowel as its 아/어 ending changes it, by Hangul's numbering of the vowels: ㅗ to ㅘ (봐), ㅜ to ㅝ
# (줘), ㅣ to ㅕ (마셔), ㅚ to ㅙ (돼), ㅡ to ㅓ (써), or to ㅏ after ㅏ or ㅗ (바빠, 몰라).
KOREAN_VOWEL_CHANGES = {8: {9}, 13: {14}, 20: {6}, 11: {10}, 18: {4}}
KOREAN_BRIGHT_VOWELS = {0, 2, 8, 9, 12}
# Particles no verb's form ends in after its stem.
KOREAN_NO_VERB_ENDINGS = {"의", "를", "와", "과", "가", "에", "에서", "에게", "로", "처럼", "까지", "부터", "만", "랑"}
# The ㄹ of a stem drops before ㄴ, ㅂ and ㅅ (사는, 사세요 of 살다) or turns into ㄴ or ㅂ (산다, 삽니다), keeping it
# before 라 (살라고): 끄라고 is 끄다's, not 끌다's.
KOREAN_L_DROPS_BEFORE = {2, 7, 9}
KOREAN_L_BECOMES = {4, 17, 10}


def korean_last_syllable_fits(stem, got, after):
    """Whether [got] may be the last syllable of a verb's [stem] in a form, [after] the syllable after it, if any."""
    last = stem[-1]
    if got == last:
        return True
    x, y = ord(last) - 0xAC00, ord(got) - 0xAC00
    vowel, now, final, end = x // 28 % 21, y // 28 % 21, x % 28, y % 28
    if vowel != now:
        changes = set(KOREAN_VOWEL_CHANGES.get(vowel, ()))
        if vowel == 18 and len(stem) > 1 and (ord(stem[-2]) - 0xAC00) // 28 % 21 in KOREAN_BRIGHT_VOWELS:
            changes.add(0)
        # 하다 and a stem in ㅎ (해, 그래, 빨개, 하얘), and 그러다, 이러다 (그래, 이래).
        if last == "하" or final == 27 or last in ("러", "쩌"):
            changes |= {1, 3, 7}
        if now not in changes:
            return False
    if final == 8 and end != 8:
        if end in KOREAN_L_BECOMES:
            return True
        code = ord(after) - 0xAC00 if after else -1
        return end == 0 and 0 <= code < 11172 and code // 588 in KOREAN_L_DROPS_BEFORE
    return True


def korean_reu_doubling(lemmas):
    """
    The verbs in 르 of the pack, each with whether its 아/어 forms double the ㄹ (자르다: 잘라, 잘랐다) or not (따르다:
    따라), by the most of the forms the pack files under it; [lemmas] maps a form to the words it is a form of.
    """
    votes = defaultdict(int)
    for form, of in lemmas.items():
        for lemma in of:
            doubled = korean_reu_doubled(lemma, form)
            if doubled is not None:
                votes[lemma] += 1 if doubled else -1
    return {lemma: n > 0 for lemma, n in votes.items() if n}


def korean_reu_doubled(lemma, word):
    """For a verb in 르 and its form in 아/어 (잘라 or 자라 of 자르다), whether the form doubles the ㄹ; None for any other."""
    stem = lemma[:-1]
    if len(stem) < 2 or not stem.endswith("르") or len(word) < len(stem) or not lemma.endswith("다"):
        return None
    at, before = ord(word[len(stem) - 1]) - 0xAC00, ord(word[len(stem) - 2]) - 0xAC00
    if not (0 <= at < 11172 and 0 <= before < 11172) or at // 588 != 5 or at // 28 % 21 not in (0, 4):
        return None
    return before % 28 == 8


def korean_reu_fits(lemma, word, doubling):
    """Whether [word] may be a form of [lemma] as its 르 takes 아/어 ([doubling], of korean_reu_doubling): 자랐고 is no 자르다's."""
    doubled = korean_reu_doubled(lemma, word)
    return doubled is None or lemma not in doubling or doubled == doubling[lemma]


# A Korean form goes to a verb at least as common as it with its other forms, the pack and the rules tying
# many forms to rare verbs that only look alike (미니, a name, to 미다); failing that, to one the rules make it
# of that is KOREAN_LOOKALIKE times less common, as a verb whose commonest form is this one may be (그래요, 긴).
KOREAN_LOOKALIKE = 10.0
# The endings of a polite or formal sentence (부탁해요, 감사합니다), which make no word but the verb's form.
KOREAN_SENTENCE_ENDINGS = ("요", "니다")
KOREAN_NOMINAL = {"noun", "pron", "num"}
# A form shared by verbs is listed only under those it goes to at least a KOREAN_LISTED-th as much as the most.
KOREAN_LISTED = 3.0
# The syllables 하다's stem takes (확보해요, 확보했다, 확보합니다, 확보할, 확보한).
KOREAN_HADA = set("하해했합할한함")
# Verbs a noun makes a verb with (운동하다, 출시되다, 피난시키다, 기만당하다), each with the beginnings its forms take after
# the noun: a reader who knows the noun reads them.
KOREAN_LIGHT_VERBS = {
    "하다": tuple(KOREAN_HADA),
    "되다": tuple("되돼됐됩될된됨"),
    "시키다": ("시키", "시켜", "시켰", "시킨", "시킬", "시킵", "시킴"),
    "당하다": ("당하", "당해", "당했", "당할", "당한", "당합", "당함"),
}
# Of those, the ones whose verbs the pack has are counted for their noun where it is the commoner.
KOREAN_MERGED_LIGHT_VERBS = ("하다", "되다")


def korean_light_noun(lemma, form):
    """The noun of a verb of a noun and a light verb (출시 of 출시되다) that [form] spells as such, or None."""
    for light, beginnings in KOREAN_LIGHT_VERBS.items():
        noun = lemma[:-len(light)]
        if lemma.endswith(light) and len(noun) > 1 and form.startswith(noun) and form.startswith(beginnings, len(noun)):
            return noun
    return None


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


# The humble 드리다 after a noun that makes a verb with 하다 (부탁드립니다 of 부탁하다), which the pack lacks.
KOREAN_HUMBLE = ("드리", "드려", "드렸", "드립", "드릴", "드린", "드림")
# The vowels of a verb's 아/어 form (쉬워, 빨라, 해, 돼), before 지다 "become" (쉬워지다 of 쉽다).
KOREAN_INFINITIVE_VOWELS = {0, 1, 4, 6, 9, 10, 14}


def korean_humble_verb(form, is_verb):
    """The verb in 하다 a form in 드리다 is humble for (부탁드립니다 of 부탁하다), by [is_verb]; None for any other."""
    for at in range(len(form) - 2, 1, -1):
        if form.startswith(KOREAN_HUMBLE, at) and is_verb(form[:at] + "하다"):
            return form[:at] + "하다"
    return None


def korean_become_stem(lemma):
    """The 아/어 form a verb in 지다 "become" is made of (쉬워 of 쉬워지다, 빨라 of 빨라지다), or None."""
    if not lemma.endswith("지다") or len(lemma) < 4:
        return None
    code = ord(lemma[-3]) - 0xAC00
    return lemma[:-2] if 0 <= code < 11172 and not code % 28 and code // 28 % 21 in KOREAN_INFINITIVE_VOWELS else None


def korean_become_verbs(verb):
    """The verbs in 지다 "become" a verb makes, as dictionaries spell them (이루어지다 of 이루다, 만들어지다 of 만들다)."""
    return [verb[:-1] + "어지다", verb[:-1] + "아지다"]


# Endings of spoken Korean that Yomitan's rules do not undo, longest first.
KOREAN_SPOKEN_ENDINGS = ["잖습니까", "잖아요", "잖니", "잖아"]

# 거 "thing" with the copula, written as one (거라고요, 거냐고, 거니깐).
KOREAN_THING_COPULA = ("거라", "거냐", "거니까", "거니깐")

# How the dictionary glosses a word written with its particle.
KOREAN_FUSED = re.compile(r"(topic-marked form of|allomorph of .* before -가|plural of)")
# How it glosses a form it has as a word of its own: 화려한 "attributive form of 화려하다", 먹기 "verbal noun of 먹다",
# 아빤 "topic-marked form of 아빠", 해요 "informal polite present exclamatory form of 하다"; and a verb's noun in -기 glossed
# as the English one in -ing (만나기 "meeting", 걷기 "walking").
KOREAN_INFLECTION_GLOSS = re.compile(
    r"(?:an? )?(?:(?:informal|formal|polite|plain|present|past|future|declarative|interrogative|imperative|exclamatory|"
    r"realis|irrealis|attributive|adnominal|conjunctive|sequential|infinitive|nominal|verbal|topic-marked)\s+)*"
    r"(?:form|noun|adnominal|determiner|infinitive|topic-marked)\s+of\s+([가-힣]+)", re.I)
KOREAN_VERBAL_NOUN_GLOSS = re.compile(r"(?:verbal noun|action noun|[a-z]+ing\b(?! [a-z]))", re.I)
# The forms the pack has as words, each with the word it is the form of where there is one; filled by load_dictionary.
KOREAN_GLOSSED = {}


def korean_glossed(rows, verbs):
    """
    The pack's entries ([rows] of word, part of speech and senses) that are another word's forms (KOREAN_INFLECTION_GLOSS),
    each with that word where all name one, else None; and nouns in -기 of [verbs] glossed as a verb's noun, with the verb.
    Words with their particle (korean_fused) are not among them.
    """
    glosses, first = defaultdict(list), {}
    for word, p, senses in rows:
        if p not in WORD_POS or korean_fused(word, senses):
            continue
        found = [g for sense in json.loads(senses) for g in sense.get("glosses", [""])]
        glosses[word] += found
        if p == "noun" and found:
            first.setdefault(word, found[0])
    out = {}
    for word, found in glosses.items():
        matches = [KOREAN_INFLECTION_GLOSS.match(g) for g in found]
        if found and all(matches):
            of = {m.group(1) for m in matches}
            out[word] = of.pop() if len(of) == 1 else None
        elif word.endswith("기") and len(word) > 1 and word[:-1] + "다" in verbs and KOREAN_VERBAL_NOUN_GLOSS.match(first.get(word, "")):
            out[word] = word[:-1] + "다"
    return out


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
KOREAN_COPULA_FORMS = {form for form, _ in KOREAN_COPULA} - {KOREAN_COPULA_VOWEL_IF} - KOREAN_COPULA_WORDS

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
    if KOREAN_NATIVE_NUMBER.fullmatch(word) or korean_whole_number(word):
        return [word]
    best, cut = None, [word]
    after_listed = False
    for end in range(len(word), 0, -1):
        stem = word[:end]
        head = [stem]
        if not known(stem):
            # A word with the honorific 님 the lexicon lacks is the word and 님 (사장님 is 사장 + 님, 사장님께 사장 + 님 + 께).
            if not (stem.endswith(KOREAN_HONORIFIC) and end > 1 and known(stem[:-1])):
                continue
            head = [stem[:-1], KOREAN_HONORIFIC]
        elif end == len(word):
            continue
        for stack in korean_stacks(word[end:], stem, known, rank):
            cost = sum(math.log(rank(piece) or KOREAN_UNRANKED) + KOREAN_WORD_COST for piece in head + stack)
            if rank(stem):
                after_listed = True
            if best is None or cost < best:
                best, cut = cost, head + stack
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


def korean_whole_number(word):
    """
    Whether [word] is a Sino-Korean number that stays whole (이십만, 십만, 삼십오): of three syllables or more, or two
    from 십, 백 or 천, and not ending in 이, which is as often the particle (백이, 만이); 이만 "this much" is cut.
    """
    return bool(KOREAN_SINO_NUMBER.fullmatch(word)) and (len(word) > 2 or len(word) == 2 and word[0] in "십백천") and word[-1] != "이"


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


# Arabic: the letters written onto the front of a word, as the reader cuts them off (ArabicParser.kt in
# core/domain/.../parse; keep the two the same): the conjunction و or ف first, then a preposition ب, ك or ل, the
# article ال, the two together (بال, كال; ل with ال is لل, its alif dropped, and stays one piece), or the future's
# س before a verb's imperfect (سيكتب). What is left is a word of two letters or more.
ARABIC_CONJUNCTIONS = ("و", "ف")
ARABIC_PREPOSITIONS = ("ب", "ك", "ل")
ARABIC_ARTICLE = "ال"
ARABIC_FOR_THE = "لل"
ARABIC_FUTURE = "س"
ARABIC_IMPERFECT = "يتنأ"
# The letters the future's س comes before: the imperfect's, and آ, the first person's أ on a verb in أ (سآخذ, سآتي).
ARABIC_FUTURE_BEFORE = ARABIC_IMPERFECT + "آ"
ARABIC_CLITICS = {*ARABIC_CONJUNCTIONS, *ARABIC_PREPOSITIONS, ARABIC_ARTICLE, ARABIC_FOR_THE, ARABIC_FUTURE}
# The pronouns joined to the end of a word (كتابه, عليها, كتبوه), longest first, but كن, seldom one and the end of many
# a word (يمكن, أماكن, لنكن is ل + نكن). Before one, a word may write its ة as
# ت (مدرستي), its ى as ي or ا (عليه, بناها) and its وا as و (كتبوه); ب and ل take them as well (به, لك).
ARABIC_SUFFIXES = ("هما", "كما", "هم", "هن", "كم", "ها", "نا", "ني", "ه", "ك", "ي")
ARABIC_SUFFIXED_PREPOSITIONS = ("ب", "ل")
# The vowel signs, shadda, sukun, the dagger alif and the tatweel, which are not written in the words they are in.
ARABIC_MARKS = re.compile("[\u064b-\u065f\u0670\u0640]")
ARABIC_WORD_COST = 4.0
ARABIC_UNRANKED = 50_000


def arabic_analyses(word):
    """Every way [word] is the letters in front and a word of two letters or more: والكتاب is و + ال + كتاب, للبيت لل + بيت."""
    out = []
    for conjunction in ("",) + ARABIC_CONJUNCTIONS:
        if not word.startswith(conjunction):
            continue
        rest = word[len(conjunction):]
        head = [conjunction] if conjunction else []
        options = [([], rest)] if conjunction else []
        if rest.startswith(ARABIC_FOR_THE):
            options.append(([ARABIC_FOR_THE], rest[2:]))
        for preposition in ARABIC_PREPOSITIONS:
            if rest.startswith(preposition):
                options.append(([preposition], rest[1:]))
                if preposition != "ل" and rest.startswith(ARABIC_ARTICLE, 1):
                    options.append(([preposition, ARABIC_ARTICLE], rest[3:]))
        if rest.startswith(ARABIC_ARTICLE):
            options.append(([ARABIC_ARTICLE], rest[2:]))
        if rest.startswith(ARABIC_FUTURE) and rest[1:2] and rest[1] in ARABIC_FUTURE_BEFORE:
            options.append(([ARABIC_FUTURE], rest[1:]))
        out.extend((head + clitics, stem) for clitics, stem in options if len(stem) >= 2)
    return out


def arabic_bases(word):
    """The words [word] may be with a pronoun after it, as they are written before one: كتاب of كتابه, مدرسة of مدرستي, على of عليه."""
    out = []
    for suffix in ARABIC_SUFFIXES:
        base = word[:-len(suffix)]
        if not word.endswith(suffix) or not base or len(base) == 1 and base not in ARABIC_SUFFIXED_PREPOSITIONS:
            continue
        out.append(base)
        if base.endswith("ت"):
            out.append(base[:-1] + "ة")
        elif base.endswith(("ي", "ا")):
            out.append(base[:-1] + "ى")
        elif base.endswith("و"):
            out.append(base + "ا")
    return out


# Two words the frequency list has whole that are as often the conjunction ف and a word, told apart by the word after
# them (ArabicParser's contextCut; keep the two the same). فقد "lost" takes a noun, the ف + قد "so ... has" of the news a
# verb: before a word with the article, or with none after it, فقد is whole, else ف + قد. فهم "understanding,
# understood" is ف + هم "so they" only before what only follows a pronoun: a negation, قد, في, or a verb's plural in
# -ون (فهم لا يعرفون, فهم يعملون).
# The list counts each as whole for the share of it the reader leaves whole, as the forms of the verb suggest (فقدت,
# يفقد: some tenth of فقد is "lost"; يفهم, الفهم: over half of فهم is the word), and the rest for ف and the other word.
ARABIC_CONTEXT_WORDS = {"فقد": ("قد", 0.1), "فهم": ("هم", 0.65)}
ARABIC_THEY_BEFORE = {"لا", "لم", "لن", "قد", "في", "ليسوا", "كانوا", "أيضا", "جميعا"}
ARABIC_THEY_VERB = re.compile("^[يتس].{2,}ون$")


def arabic_context_cut(word, following):
    """فقد and فهم cut by the word after them ([following], None when none follows in the sentence), or None for any other word."""
    if word not in ARABIC_CONTEXT_WORDS:
        return None
    stem = ARABIC_CONTEXT_WORDS[word][0]
    if word == "فقد":
        whole = following is None or following.startswith(ARABIC_ARTICLE)
    else:
        whole = following is None or not (following in ARABIC_THEY_BEFORE or ARABIC_THEY_VERB.match(following))
    return [word] if whole else ["ف", stem]


def arabic_pieces(word, known, rank, following=None, head=None):
    """
    An Arabic word cut as the reader cuts it: the letters in front of it and the word they are on (والعالم is و + ال +
    عالم), when that word is one the lexicon [known] knows, or such a word with a pronoun after it (وكتابهم). A word the
    frequency list has ([rank] is a word's rank in it) stays whole (ولد, كان, لكن), and so may a headword of the list
    ([head] is its rank as one) with a pronoun after it (كلنا of كل, بابي of باب). Of the cuts and that, the one whose
    word is the most common wins, by the costs SegmentingParser gives Chinese words, the letters in front costing the
    log of their rank alone. After the conjunction, a word of the list is left as the reader leaves it alone (ولأن is
    و + لأن, as لأن is whole), and after لل a word of the list is the article's (للبنك is لل + بنك) unless ل is on a word
    of the list (للجنة is ل + لجنة, by the costs). A word only a
    dictionary has stays whole unless a cut leaves a word of the list (لبنان is no ل + بنان; the dictionary's forms with
    ال, العالم, are cut). فقد and فهم go by the word after them ([following]; arabic_context_cut).
    """
    context = arabic_context_cut(word, following)
    if context:
        return context
    if rank(word):
        return [word]
    best, cut, after_listed = None, [word], False
    # A headword, not another's form (فعليك is ف + عليك, no فعلي + ك of فعل + ي + ك), nor a word with the article, which is
    # cut off (الفهم is ال + فهم, no الف + هم).
    owned = min((head(b) for b in arabic_bases(word) if head and head(b) and not b.startswith(ARABIC_ARTICLE)), default=None)
    if owned:
        best, after_listed = math.log(owned) + ARABIC_WORD_COST, True
    for clitics, stem in arabic_analyses(word):
        if len(clitics) > 1 and clitics[0] in ARABIC_CONJUNCTIONS and rank(word[1:]):
            continue
        # After لل, a word of the list is the article's (للبنك is لل + بنك, not ل + لبنك "for your milk"), unless what
        # follows ل is a word of the list itself, when the commoner reading wins (للجنة is ل + لجنة, not لل + جنة).
        if clitics[-1:] == ["ل"] and stem.startswith("ل") and rank(stem[1:]) and not rank(stem):
            continue
        bases = [stem] + arabic_bases(stem)
        ranked = next((rank(b) for b in bases if rank(b)), None)
        if ranked is None and not any(known(b) for b in bases):
            continue
        if ranked:
            after_listed = True
        cost = sum(math.log(rank(c) or ARABIC_UNRANKED) for c in clitics) + math.log(ranked or ARABIC_UNRANKED) + ARABIC_WORD_COST
        if best is None or cost < best:
            best, cut = cost, clitics + [stem]
    if best is None or known(word) and not after_listed:
        return [word]
    return cut


# The pack's forms table loses wty's tags, and without its vowels a verbal noun, a participle or a derived verb is
# spelled as some word's form (تعليم and عالم under علم, أمن under من): the Arabic list's forms come from wty's own
# dictionary instead, a form whose tags are all of these (case, state, number, gender, person, tense, mood, voice).
ARABIC_INFLECTION_TAGS = set("""
    nominative genitive accusative genitive/accusative nominative/genitive nominative/genitive/accusative accusative/genitive
    informal genitive/accusative/informal oblique definite indefinite construct long-construct definite/indefinite singular
    dual plural paucal collective singulative masculine feminine masculine/feminine common female equivalent triptote diptote
    invariable sound-form broken-form first-person second-person third-person second/third-person past non-past perfective
    imperfective indicative subjunctive jussive imperative active passive active/passive
""".split())
ARABIC_VERB_TAGS = {"first-person", "second-person", "third-person", "second/third-person", "past", "non-past", "perfective",
                    "imperfective", "indicative", "subjunctive", "jussive", "imperative"}
ARABIC_NUMBER_TAGS = {"plural", "dual", "paucal", "collective", "singulative"}
ARABIC_DERIVED_TAGS = {"verbal", "noun-from-verb", "participle", "elative"}
# How a form is its word's, for the forms the Arabic list counts (filled by build_arabic): a verb's form (True), a noun's
# or adjective's (False), or another spelling of it (None: الى of إلى, ان of أن, فى of في).
ARABIC_FORMS = {}
# The verbal nouns, participles and elatives wty has as forms only, words of their own unless a commoner word's plural
# too (قيام, but مدارس is مدرسة's), and the dictionary's words that are a plural, each sense of them so tagged (أيام);
# filled by build_arabic.
ARABIC_LEXEMES = set()
ARABIC_PLURALS = set()
# How common each verb is by its forms that are no other word's (filled by build_arabic): a form several verbs share is
# shared by this (يصل is وصل's more than صلى's, يبني بنى's, not بن's).
ARABIC_VERB_MASS = {}
# The words wty tags each form a plural of (طلاب of طالب, مرات of مرة), and the feminine singular of (صغيرة of صغير), but
# as no verb's form; filled by build_arabic.
ARABIC_PLURAL_OF = defaultdict(set)
ARABIC_FEMININE_OF = defaultdict(set)
# Every form wty has as a verbal noun, a participle or an elative (مساعدة, مشاركة), the dictionary's words among them.
ARABIC_DERIVED = set()
# The forms read as two words, a word and another with a pronoun (يومي "daily" and "my day"), listed under both.
ARABIC_SHARED = set()
# How sure arabic_moved must be: "my X" must be at least this share of the count; the feminine at most so many times as
# common as the adjective; the singular at least this share of the plural; a verb's own forms at most this share of the
# other verb's; that verb's forms as common as this share of the form (the more for an elative).
ARABIC_MINE_SHARE = 0.15
ARABIC_FEMININE_SHARE = 3.0
ARABIC_PLURAL_SHARE = 0.3
ARABIC_VERB_OWN = 0.1
ARABIC_VERB_TAKES = 0.5
ARABIC_ELATIVE_VERB = 0.3
# The words a misspelt form (انا, الى) is, each as common as its spellings that are that word.
ARABIC_SPELLED = {}
# The little words: a verb spelled as one has its forms counted for nothing (تمني is no form of من); and a word spelled
# as a pronoun or particle with letters in front is read whole only when much commoner than the two (وهم is و + هم,
# not "illusion").
ARABIC_FUNCTION = {"pron", "prep", "conj", "particle", "det", "article", "adv"}
ARABIC_LITTLE = {"pron", "conj", "particle", "det", "article"}
# The words that take a pronoun after them: nouns, adjectives, verbs and prepositions, and إن and its kind (أنه, لكنها).
ARABIC_SUFFIXED_KINDS = {"noun", "adj", "verb", "prep", "num"}
ARABIC_SUFFIXED_PARTICLES = {"أن", "إن", "لكن", "كأن", "لعل", "ليت", "لأن"}
# The determiners, which take a pronoun even where wty spells the two as another word's form (كلنا, not كال's).
ARABIC_DETERMINERS = {"كل", "بعض", "جميع", "نفس", "غير"}


def arabic_tagged_forms():
    """
    Each form of wty's Arabic dictionary without its vowels, with the words it is an inflection of (ARABIC_FORMS' kinds:
    True for a verb's, "number" for a plural, dual or singulative, False for another noun's or adjective's); the forms
    that are a verbal noun, a participle or an elative, which are words of their own (قيام, عالم, أكبر); and each form's
    tags for each of its words.
    """
    path = os.path.join(DICTIONARIES, "wty", "wty-ar-en.zip")
    if not os.path.exists(path):
        sys.exit(f"{path} is missing; fetch it with: python3 tools/build_yomitan_dictionary.py --source ar --target en")
    import zipfile
    forms, lexemes, tagged = defaultdict(dict), set(), defaultdict(set)
    with zipfile.ZipFile(path) as z:
        for name in z.namelist():
            if not name.startswith("term_bank"):
                continue
            for row in json.loads(z.read(name)):
                term, _, entry, _, _, glosses = row[:6]
                if entry != "non-lemma" or " " in term:
                    continue
                term = ARABIC_MARKS.sub("", term)
                for gloss in glosses:
                    if not (isinstance(gloss, list) and len(gloss) == 2 and isinstance(gloss[1], list)):
                        continue
                    lemma = ARABIC_MARKS.sub("", gloss[0])
                    tags = " ".join(gloss[1]).split()
                    if " " in lemma:
                        continue
                    if any(t in ARABIC_DERIVED_TAGS for t in tags):
                        lexemes.add(term)
                    if lemma == term or not tags or not all(t in ARABIC_INFLECTION_TAGS or t.startswith("ar-infl-") for t in tags):
                        continue
                    # The article is cut off by the reader, as it is here (العالم is ال + عالم).
                    if term.startswith(ARABIC_ARTICLE) and not lemma.startswith(ARABIC_ARTICLE):
                        continue
                    kind = True if any(t in ARABIC_VERB_TAGS for t in tags) else "number" if any(t in ARABIC_NUMBER_TAGS for t in tags) else False
                    tagged[term, lemma].update(tags)
                    # A form that is a noun's as well as a verb's is the noun's.
                    earlier = forms[term].get(lemma, True)
                    forms[term][lemma] = kind if earlier is True else "number" if "number" in (earlier, kind) else False
    return forms, lexemes, tagged


def arabic_spellings(word, hamza_only=False):
    """
    [word] written with the hamza on or under an alif it lacks (ان: أن, إن, آن; الان: الآن), and unless [hamza_only], ى
    for its last ي and ة for its last ه (الى, مدرسه).
    """
    out = []
    for i, c in enumerate(word):
        if c == "ا":
            out.extend(word[:i] + h + word[i + 1:] for h in "أإآ")
    if hamza_only:
        return out
    if word.endswith("ي"):
        out.append(word[:-1] + "ى")
    if word.endswith("ه") and len(word) > 2:
        out.append(word[:-1] + "ة")
    return out


# How much commoner than the letters in front and the word after them, by how often those letters come before words
# (ال before nearly half), a word the dictionary has that can be read as them must be to be read whole: after ال, a
# word is all but always the noun it is on (الشمس, الموت, not words of their own); a word after which a pronoun or
# particle is spelled (وهم "and they", فلم "so not") is much more often the two than a dictionary's look-alike.
ARABIC_WHOLE_MARGIN = 3.0
ARABIC_FUNCTION_MARGIN = 15.0
ARABIC_ARTICLE_MARGIN = 50.0
ARABIC_DEFINITE_SHARE = 0.3
ARABIC_DEFINITE_LEAST = 2e-6
ARABIC_ROUNDS = 4


def build_arabic(found, size):
    db = sqlite3.connect(os.path.join(DICTIONARIES, "ar-en.sqlite"))
    # As the reader's lexicon has them with the pack installed (Dictionary.sq's selectWords).
    pack = {w for (w,) in db.execute("SELECT word_lc FROM entries UNION SELECT form_lc FROM forms") if w and " " not in w}
    pos = defaultdict(set)
    plural, glosses = defaultdict(list), defaultdict(list)
    for word, p, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        pos[word].add(p)
        if p in WORD_POS:
            plural[word] += ["plural" in sense.get("tags", []) for sense in json.loads(senses)]
            glosses[word] += [g for sense in json.loads(senses) for g in sense.get("glosses", [""])]
    ARABIC_PLURALS.clear()
    ARABIC_PLURALS.update(w for w, tagged in plural.items() if tagged and all(tagged))
    bound = {w for w, found in glosses.items() if found and all(g.startswith("only used in") for g in found)}
    forms, lexemes, tagged = arabic_tagged_forms()
    ARABIC_PLURAL_OF.clear()
    ARABIC_FEMININE_OF.clear()
    for (form, lemma), tags in tagged.items():
        if tags & ARABIC_VERB_TAGS:
            continue
        if "plural" in tags:
            ARABIC_PLURAL_OF[form].add(lemma)
        elif {"feminine", "singular"} <= tags and "dual" not in tags:
            ARABIC_FEMININE_OF[form].add(lemma)
    ARABIC_DERIVED.clear()
    ARABIC_DERIVED.update(lexemes)
    ARABIC_SHARED.clear()
    ARABIC_SPELLED.clear()
    ARABIC_LEXEMES.clear()
    ARABIC_LEXEMES.update(lexemes - pos.keys())
    for word in ARABIC_LEXEMES:
        pos[word].add("noun")
    words = {w for w, kinds in pos.items() if kinds & WORD_POS} - EXCLUDE.get("ar", set()) - ARABIC_CUT - bound
    function = {w for w in words if pos[w] & ARABIC_FUNCTION}
    # A verb spelled as a little word (أن "to moan", من "to bestow", عن "to appear") has its forms counted for nothing:
    # تمني and أنت are no forms of من and أن.
    forms = {f: {l: kind for l, kind in of.items() if not (kind is True and l in function)} for f, of in forms.items()}
    share = defaultdict(float)
    for _, counts, _ in found:
        total = sum(counts.values())
        for token, count in counts.items():
            token = ARABIC_MARKS.sub("", token)
            if is_wordlike(token):
                share[token] += count / total / len(found)
    # The feminine of an adjective or a participle the dictionary lacks (مؤكدة, شاملة) is its form.
    for t in share:
        base = t[:-1]
        if t.endswith("ة") and t not in pack and not forms.get(t) and base in words and (
                "adj" in pos[base] or base.startswith("م") and base in lexemes):
            forms[t] = {base: False}

    def spelled(t, hamza_only=False):
        """
        The words [t] is another spelling of, as ARABIC_FORMS has them, each as common as the spellings that are it (انا is
        أنا, as أنا is commoner than إنا): words of the dictionary (انا of أنا) and the words of forms (الى of إلى).
        """
        out = defaultdict(float)
        for v in arabic_spellings(t, hamza_only):
            of = [v] if v in words else [l for l in forms.get(v, ()) if l in words]
            # A spelling that is several words' form is shared by how common they are (أمور is أمر's, not مار's).
            whole = sum(share.get(l, 0.0) for l in of) or 1.0
            for l in of:
                out[l] += share.get(v, 0.0) * (share.get(l, 0.0) / whole if len(of) > 1 else 1.0)
        ARABIC_SPELLED[t] = dict(out)
        return {l: None for l in out}

    def takes_pronouns(word):
        return bool(pos[word] & ARABIC_SUFFIXED_KINDS) or word in ARABIC_SUFFIXED_PARTICLES

    def suffixed(t, little=False):
        """
        The words [t] is a form of with a pronoun after it, as ARABIC_FORMS has them: of its commonest base that is a word or
        a word's form, written as it is or with a hamza (لانه of لأن), and that takes a pronoun (not هو, أو, لا); a
        preposition's own forms with one take none (بنا is ب's, so بناها is بنى's). With [little], only a preposition's or
        إن's and its kind's (عليه, أنه).
        """
        best, out = -1.0, {}
        bases = arabic_bases(t)
        # A word's ى is written ا before a pronoun (معناه of معنى, not of معنا "with us").
        bases = [b for b in bases if not (b.endswith("ا") and b[:-1] + "ى" in words)]
        for base in bases:
            for b in [base] + arabic_spellings(base):
                of = {b: "suffix"} if b in words and takes_pronouns(b) else {
                    l: kind for l, kind in forms.get(b, {}).items() if l in words and takes_pronouns(l) and l not in function}
                if little:
                    of = {l: kind for l, kind in of.items() if "prep" in pos[l] or l in ARABIC_SUFFIXED_PARTICLES}
                if of and share.get(b, 0.0) > best:
                    best, out = share.get(b, 0.0), of
        return out

    readings = {}
    # The determiners that take a pronoun (كلنا "all of us", not كال's verb form).
    determiners = {w for w in ARABIC_DETERMINERS if w in words}

    def reading(t):
        """
        The words [t] read whole is: itself, the words it is a form of, another spelling of, or one of those with a pronoun.
        A form is a little word's with a pronoun too (به is ب's, أنه أن's), and a word's only where it is no form itself.
        """
        if t not in readings:
            own = {l: kind for l, kind in forms.get(t, {}).items() if l in words}
            if t in ARABIC_CUT:
                readings[t] = {}
            elif t in words or t in ARABIC_WHOLE:
                # A word spelled as a little word with a pronoun is that (لها "to her", not "to amuse oneself"); but لكن is
                # no ل + كن.
                readings[t] = {t: False, **own, **({} if t in function else suffixed(t, little=True))}
            elif own:
                hamza = {l: kind for l, kind in spelled(t, hamza_only=True).items() if l in arabic_spellings(t, hamza_only=True)}
                # A determiner's with a pronoun too (كلنا, not only كال's).
                little = {**suffixed(t, little=True), **{l: kind for l, kind in suffixed(t).items() if l in determiners}}
                readings[t] = {**own, **little, **hamza}
            else:
                readings[t] = {**spelled(t), **suffixed(t)}
        return readings[t]

    def dictionary_word(t):
        return bool(reading(t))

    # The words left out, and those only written with ال (صين, only in الصين), are no words the letters in front are on:
    # الصين stays whole.
    unwritten = EXCLUDE.get("ar", set()) | bound

    def stem_known(stem):
        return stem not in unwritten and (stem in pack or dictionary_word(stem) or any(b in pack for b in arabic_bases(stem)))

    analyses = {t: [(c, s) for c, s in arabic_analyses(t) if stem_known(s)] for t in share}
    # How often each run of letters in front comes before the dictionary's words, as the counts of the words written
    # with them against those of the words alone.
    joined, alone = defaultdict(float), defaultdict(float)
    for t, found_cuts in analyses.items():
        if found_cuts and not dictionary_word(t):
            clitics, stem = max(found_cuts, key=lambda cut: share.get(cut[1], 0.0))
            joined["+".join(clitics)] += share[t]
            alone["+".join(clitics)] += share.get(stem, 0.0)
    rate = {k: joined[k] / alone[k] for k in joined if alone[k]}

    def function_word(stem):
        return any(pos.get(b, set()) & ARABIC_LITTLE for b in [stem] + arabic_spellings(stem))

    def kept_whole(t):
        """Whether the builder reads [t] whole: a dictionary's word that is more often itself than what it can be cut into."""
        if t in ARABIC_WHOLE or not analyses[t] or any(b in ARABIC_WHOLE for b in arabic_bases(t)):
            return True
        if not dictionary_word(t):
            return False
        # A preposition with its pronoun (بها, لهم) is one word, as a noun with its pronoun is.
        if t[0] in ARABIC_SUFFIXED_PREPOSITIONS and t[1:] in ARABIC_SUFFIXES:
            return True
        # A noun often written with the article is that noun (البرد, اللبن): برد is "cold", لبن "milk", no ب + رد or ل + بن.
        definite = share.get(ARABIC_ARTICLE + t, 0.0)
        if pos[t] & {"noun", "adj"} and definite >= ARABIC_DEFINITE_SHARE * share[t] and definite >= ARABIC_DEFINITE_LEAST:
            return True
        for clitics, stem in analyses[t]:
            margin = ARABIC_ARTICLE_MARGIN if ARABIC_ARTICLE in clitics or ARABIC_FOR_THE in clitics else (
                ARABIC_FUNCTION_MARGIN if function_word(stem) else ARABIC_WHOLE_MARGIN)
            if share[t] < margin * rate.get("+".join(clitics), 0.0) * share.get(stem, 0.0):
                return False
        return True

    whole = {t for t in share if kept_whole(t)}
    lemmas = {t: {l: kind for l, kind in reading(t).items() if l != t} for t in share}
    for form, of in forms.items():
        lemmas.setdefault(form, {l: kind for l, kind in of.items() if l in words})
    # A word of the dictionary that is another's spelling too (الان of الآن) is it where that is commoner.
    for t in share:
        if t in words:
            lemmas[t].update({v: None for v in arabic_spellings(t, hamza_only=True) if v in words})
    lemmas = {t: of for t, of in lemmas.items() if of}
    ARABIC_FORMS.clear()
    ARABIC_FORMS.update(lemmas)
    # A verb is as common as its forms that are no other word's: بنى's (بنيت, يبني), not بن's spelling.
    ARABIC_VERB_MASS.clear()
    for t, of in lemmas.items():
        if len(of) == 1 and t not in words and next(iter(of.values())) is True:
            lemma = next(iter(of))
            ARABIC_VERB_MASS[lemma] = ARABIC_VERB_MASS.get(lemma, 0.0) + share.get(t, 0.0)

    # The reader leaves whole the words the list has (rank), and cuts the rest as arabic_pieces does: the first cut
    # is by wordfreq's ranks, of the words kept whole only, the letters in front taken as the commonest.
    ordered = sorted(share, key=lambda w: -share[w])
    ranks = {w: i + 1 for i, w in enumerate(ordered) if w in whole}
    ranks.update({c: 1 for c in ARABIC_CLITICS})
    # The words of the dictionary among them are the headwords to be.
    heads = {w: r for w, r in ranks.items() if w in words}
    lexicon = pack | whole
    text = label = None
    cuts = {}
    for round_ in range(ARABIC_ROUNDS + 1):
        known, rank = lexicon.__contains__, ranks.get
        changed = 0.0
        pieces = []
        for name, counts, source_label in found:
            out = defaultdict(int)
            total = sum(counts.values())
            for token, count in counts.items():
                token = ARABIC_MARKS.sub("", token)
                if not is_wordlike(token):
                    continue
                cut = [token] if token in whole else arabic_pieces(token, known, rank, head=heads.get)
                if cuts.get(token, cut) != cut:
                    changed += count / total
                cuts[token] = cut
                if token in ARABIC_CONTEXT_WORDS:
                    # The reader cuts فقد and فهم by the word after them; counted whole for the share it leaves whole.
                    stem, kept = ARABIC_CONTEXT_WORDS[token]
                    out[token] += count * kept
                    out["ف"] += count * (1 - kept)
                    out[stem] += count * (1 - kept)
                    continue
                for piece in cut:
                    out[piece] += count
            pieces.append((name, dict(out), source_label))
        print(f"ar round {round_}: {changed / len(found):.2%} of words cut otherwise", file=sys.stderr)
        if round_ and changed / len(found) < 0.0005 or round_ == ARABIC_ROUNDS:
            break
        # A word kept whole that the reader could cut must be in the list for the reader to leave it whole.
        required = {t for t in whole if analyses.get(t)}
        text, label = build_list("ar", pieces, size, required=required, cut={t for t, c in cuts.items() if c != [t]} | bound)
        listed, ranks = list_ranks(text)
        heads = list_heads(text)
        lexicon = pack | listed
    return text, label


# The kinds of word (noun, verb...) each Romanian word of the dictionary is with senses of its own, not only as another
# word's form ("am", first-person form of avea; "fu", simple perfect of fi).
ROMANIAN_KINDS = {}
ROMANIAN_FORM_GLOSS = re.compile(r"\bof ([^\W\d_]+)")
# A noun or verb the other kind's form is spelled as (fiu "son" and să fiu; cost and the verb costa) keeps its count when
# text has its own forms (fiul, costul) at least this share of it.
ROMANIAN_OWN_FORMS = 0.05
# A verb spelled as a noun's articled form (urma, the trace and to follow; lupta) gives the noun this share of it.
ROMANIAN_ARTICLED_SHARE = 0.8


def romanian_kinds(db, lemmas):
    """Each Romanian word's kinds whose senses are not all glossed as a form of a word its forms rows name."""
    senses = defaultdict(list)
    for word, p, text in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if p in WORD_POS:
            senses[word, p] += [" ".join(s.get("glosses", [])) for s in json.loads(text)]
    kinds = defaultdict(set)
    for (word, p), glosses in senses.items():
        kinds.setdefault(word, set())
        if not all(set(ROMANIAN_FORM_GLOSS.findall(g)) & set(lemmas.get(word, ())) for g in glosses):
            kinds[word].add(p)
    return kinds


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
    if language == "ko":
        verbs = {w for (w,) in db.execute("SELECT word_lc FROM entries WHERE pos IN ('verb', 'adj')")}
        KOREAN_GLOSSED.update(korean_glossed(db.execute("SELECT word_lc, pos, senses FROM entries"), verbs))
    for word, word_lc, p, senses in db.execute("SELECT word, word_lc, pos, senses FROM entries"):
        if language == "ko" and (korean_fused(word_lc, senses) or word_lc in KOREAN_GLOSSED):
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
    # A Korean verb's form the pack files under other verbs too (가는 of 가다, 갈다 and 가늘다) goes as its forms table says.
    for form_lc, of in KOREAN_GLOSSED.items() if language == "ko" else ():
        if of and set(lemmas.get(form_lc, [of])) - {of} and pos.get(of, set()) & {"verb", "adj"}:
            KOREAN_GLOSSED[form_lc] = None
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
    if language == "ro":
        ROMANIAN_KINDS.update(romanian_kinds(db, lemmas))
    if language == "ar" and ARABIC_FORMS:
        lemmas = defaultdict(list, {form: list(of) for form, of in ARABIC_FORMS.items()})
        for word in ARABIC_LEXEMES - pos.keys():
            pos[word].add("noun")
            common[word] = word
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
    if language == "ar":
        return build_arabic(found, size)
    return build_list(language, found, size)


# The reader cuts a Korean word by how common its pieces are, as the list it ships with ranks them, so
# the Korean list is built again from cuts made with the last one's ranks until they no longer change
# it, at most KOREAN_ROUNDS times.
KOREAN_ROUNDS = 6
KOREAN_LETTERS = re.compile("[\uac00-\ud7a3\u1100-\u11ff\u3130-\u318f]+")


def list_heads(text):
    """The headwords of a built list, each with its rank."""
    heads, rank = {}, 0
    for line in text.splitlines():
        if line.startswith("#") or not line.strip():
            continue
        rank += 1
        heads.setdefault(line.partition("\t")[0].lower(), rank)
    return heads


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
            lemma in verbs and steps and korean_stem_of(lemma, run, strict=False) for lemma, steps in korean_dictionary_forms(run, bare=False))

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
    left_out = EXCLUDE.get("ko", set()) | KOREAN_CUT
    mass = defaultdict(float)
    for run, x in share.items():
        for lemma in lemmas.get(run, ()) if run not in left_out else ():
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
                    # The copula's 면 after a vowel (정도면) counts as 이면, for 이다; a 면 alone is the noun. Its forms that
                    # are words too (고양이 + 다, 결과 + 일) count for 이다.
                    if len(cut) > 1 and cut[-1] == KOREAN_COPULA_VOWEL_IF:
                        out[KOREAN_COPULA_VOWEL_IF] -= count
                        out["이" + KOREAN_COPULA_VOWEL_IF] += count
                    elif len(cut) > 1 and cut[-1] in KOREAN_COPULA_WORDS:
                        out[cut[-1]] -= count
                        out["이다"] += count
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


def build_list(language, found, size, required=frozenset(), verbal_nouns=None, cut=frozenset()):
    """The list from the sources' counts of the words as the reader cuts them."""
    # Each source's share of its own total, averaged; a form a source does not list counts as 0 there.
    freq = defaultdict(float)
    for _, counts, _ in found:
        total = sum(counts.values())
        for w, c in counts.items():
            freq[normalise(language, w)] += c / total / len(found)
    if language == "ro":
        # The reader cuts Romanian at hyphens (s-a is s + a, dintr-o is dintr + o), and so is each count.
        for w in [w for w in freq if "-" in w]:
            share = freq.pop(w)
            for piece in filter(None, w.split("-")):
                freq[piece] += share
    label = " + ".join(f"{label} ({SOURCE_LICENCES[name]})" for name, _, label in found)
    pos, lemmas, common, outdated = load_dictionary(language)
    overrides = OVERRIDES.get(language, {})
    excluded = EXCLUDE.get(language, set())
    verbal_nouns = verbal_nouns or {}
    if language == "ko":
        # Listed, a word cut by hand would be left whole by the reader.
        excluded = excluded | KOREAN_CUT
    # So would the words the counts were cut from ([cut]: والعالم, and وفي, a word of the dictionary too).
    excluded = excluded | cut

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
                # A word left out (the name 김) is no verb's form, and a verb left out (긷다) shares no form; nor is
                # a form sent elsewhere by hand or as a particle (겁니다 of 거, not 걸다; 든 of 뭐든, not 들다).
                of = set(overrides[form] if form in overrides else [] if language == "ko" and form in KOREAN_OWN | KOREAN_COPULA_FORMS
                         else lemmas.get(form, [])) - {form} - excluded
                if len(of) == 1 and not is_word(form) and form not in excluded:
                    lemma = of.pop()
                    mass[lemma] = mass.get(lemma, 0) + share
        return mass.get(word, 0)

    def guess(form):
        """simplemma's word for a form the dictionary does not have, or None for a name or an unknown form."""
        if language == "ko":
            return korean_guess(form)
        # simplemma takes Arabic words for others of their root (انت for آن); the Arabic list knows its forms itself.
        if language == "ar":
            return None
        if lemmatizer is None or len(form) < 2 or not is_wordlike(form) or not lemmatizer.is_known(form):
            return None
        # Web text in every language is full of English, and simplemma knows English loanwords.
        if language != "en" and english_share(form) >= freq.get(form, 0):
            return None
        lemma = lemmatizer.lemmatize(form)
        key = lower(language, lemma)
        if (lemma != key and language not in CAPITALISED_NOUNS) or (key in pos and key not in common) or key in excluded:
            return None
        # simplemma gives a negated Slovak verb as a word of its own (neurobil of neurobiť).
        if language == "sk" and slovak_positive(key):
            return slovak_positive(key)
        guessed.setdefault(key, lemma)
        return key

    def slovak_positive(word):
        """
        The verb a negated Slovak verb (neurobiť, netrápiť) is the form of, as the dictionary or simplemma knows the verb,
        or None: for other words, and for the verbs that mean more than "not" (nenávidieť, neznášať).
        """
        positive = word[2:]
        if not word.startswith("ne") or not word.endswith("ť") or word in SLOVAK_NEGATIVE_VERBS or positive in excluded:
            return None
        if "verb" in pos.get(positive, set()) and positive in common:
            return positive
        if not pos.get(positive) and lemmatizer and lemmatizer.is_known(positive) and lemmatizer.lemmatize(positive) == positive:
            guessed.setdefault(positive, positive)
            return positive
        return None

    reu = {}

    def korean_guess(form):
        """The verb or adjective a Korean form the dictionary lacks inflects, by Yomitan's rules: the nearest, then the commonest."""
        if not reu:
            reu.update(korean_reu_doubling(lemmas))
        # The polite 요 after a word (사람요, 다시요), before the rules take it for a verb's (살다, 달다); but 되요, 돼요
        # misspelt, is of 되다, a verb commoner than the word 되 that its stem spells.
        base = form[:-1]
        if form.endswith(KOREAN_POLITE) and is_word(base) and not (
                is_word(base + "다") and pos[base + "다"] & {"verb", "adj"} and weight(base + "다") >= weight(base)):
            return base
        humble = korean_humble_verb(form, lambda verb: is_word(verb) and pos[verb] & {"verb", "adj"})
        if humble:
            return humble
        # A verb whose forms begin like a shorter verb's (모셨는데 of 모시다, not 몰다; 달았고 of 달다, not 닫다).
        stems = [verb for verb, beginnings in KOREAN_STEMS.items() if form.startswith(beginnings) and is_word(verb)]
        if stems:
            return stems[0]
        found = [(steps, -weight(lemma), lemma) for lemma, steps in korean_dictionary_forms(form)
                 if korean_stem_of(lemma, form) and korean_reu_fits(lemma, form, reu) and pos.get(lemma, set()) & {"verb", "adj"}
                 and lemma in common and lemma not in excluded]
        # Failing that, a verb a tenth as common whose stem the form is with a plain ending (길게 of 길다, 쉽지 of 쉽다).
        # A verb in 하다 is no look-alike when the form spells its 하 out (확보했습니다 of 확보하다).
        found = [f for f in found if -f[1] >= count(form)] or [
            f for f in found if -KOREAN_LOOKALIKE * f[1] >= count(form) and any(form == f[2][:-1] + e for e in KOREAN_PLAIN_ENDINGS)
            or f[2].endswith("하다") and form[len(f[2]) - 2:len(f[2]) - 1] in KOREAN_HADA]
        if not found:
            # A verb of a noun and 하다, 되다, 시키다 or 당하다 the pack lacks is the noun's (출시했어요, 출시됐어요 of 출시), as
            # one it has may be (운동하다).
            for lemma, _ in [(form, 0)] + korean_dictionary_forms(form):
                noun = korean_light_noun(lemma, form)
                if noun and lemma not in pos and lemma not in KOREAN_APART and is_word(noun) and "noun" in pos[noun]:
                    return noun
            # A verb in 지다 "become" the pack lacks is its verb's (자연스러워졌어요 of 자연스럽다, 빨라졌고 of 빠르다), or
            # that verb's own in 지다 as the pack spells it (이뤄졌다 of 이루어지다).
            for lemma, _ in korean_dictionary_forms(form):
                stem = korean_become_stem(lemma)
                verb = stem and not is_word(lemma) and korean_guess(stem)
                if verb:
                    return next((v for v in korean_become_verbs(verb) if is_word(v)), verb)
            # Spoken endings the rules lack: 있잖아 "you know, there is" of 있다.
            stem = next((form[:-len(ending)] for ending in KOREAN_SPOKEN_ENDINGS if form.endswith(ending) and len(form) > len(ending)), None)
            if stem and stem + "다" in common and pos.get(stem + "다", set()) & {"verb", "adj"}:
                return stem + "다"
            # After a verb's form in 다 without it (했잖아요 of 했다, 하다's).
            if stem and len(stem) > 1:
                verb = korean_guess(stem + "다")
                if verb:
                    return verb
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
        # The Arabic clitics are words of the reader's, and so are the words read whole by hand (الله, a name to the dictionary).
        if language == "ar" and (w in ARABIC_CLITICS or w in ARABIC_WHOLE and w in pos and w not in excluded):
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

    # The words arabic_moved sent to another, whose other spellings (تقرا, اعرف) follow them.
    moved = set()

    def arabic_moved(form, others):
        """
        The words an Arabic word of the dictionary is a form of after all, with their shares, or None: as the reader reads
        it, it is that word inflected, however common (ARABIC_MINE_SHARE and the rest say how sure each rule must be).
        """
        own = count(form)
        if not own:
            return None
        # "My X" spelled as the adjective of X (يومي "daily" and "my day", هاتفي): the two share its count, X for as much
        # as X is written with "your" and "his" (يومك, يومه), and it is listed under both.
        if form.endswith("ي") and "adj" in pos[form]:
            stem = form[:-1]
            noun = next((b for b in [stem] + ([stem[:-1] + "ة"] if stem.endswith("ت") else []) if is_word(b) and "noun" in pos[b]), None)
            if noun:
                mine = min(count(stem + "ك"), count(stem + "ه"))
                part = min(1.0, mine / own)
                if part >= ARABIC_MINE_SHARE:
                    ARABIC_SHARED.add(form)
                    if part == 1.0:
                        moved.add(form)
                        return {noun: 1.0}
                    return {form: 1.0 - part, noun: part}
        # The feminine of an adjective (صغيرة of صغير, عالمية of عالمي) that is no adverb (خاصة "especially") nor verbal noun
        # (مساعدة "help"), and not much commoner than it (جامعة "university", طائرة "aeroplane", which are nouns).
        feminine = [l for l in ARABIC_FEMININE_OF.get(form, ()) if is_word(l) and "adj" in pos[l] and ARABIC_FEMININE_SHARE * count(l) >= own]
        if feminine and "adv" not in pos[form] and form not in ARABIC_DERIVED:
            moved.add(form)
            return {feminine[0]: 1.0}
        # A plural that is no verb's form (طلاب of طالب, مرات of مرة): a sound one, or one the dictionary has only as that,
        # and not much commoner than its singular (قيام "rising" is more than قائم's).
        if "verb" not in pos[form]:
            plurals = [l for l in ARABIC_PLURAL_OF.get(form, ()) if is_word(l) and ARABIC_PLURAL_SHARE * own <= count(l) and (
                form in (l + "ات", l + "ون", l + "ين") or l.endswith("ة") and form == l[:-1] + "ات"
                or form in ARABIC_PLURALS or form in ARABIC_LEXEMES)]
            if plurals:
                moved.add(form)
                weights = {l: count(l) or 1e-12 for l in plurals}
                return {l: w / sum(weights.values()) for l, w in weights.items()}
        # A verb's present that the dictionary has as a verb or an interjection of its own (ترى, تقرأ, تعرف), or a first
        # person spelled as an elative (أعرف, أفعل, but not أكثر, أكبر): the verb's, when its other persons are as common
        # (يعرف, تعرف) and its own forms are few.
        if form[:1] in ARABIC_IMPERFECT and len(form) > 2:
            elative = "adj" in pos[form] and form[0] == "أ"
            if elative and pos[form] & WORD_POS <= {"adj", "verb"} or pos[form] & WORD_POS <= {"verb", "intj"}:
                persons = max(count(p + form[1:]) for p in "يت" if p != form[0])
                verbs = [l for l in others if ARABIC_FORMS.get(form, {}).get(l) is True
                         and ARABIC_VERB_MASS.get(form, 0.0) <= ARABIC_VERB_OWN * ARABIC_VERB_MASS.get(l, 0.0)
                         and ARABIC_VERB_MASS.get(l, 0.0) >= (ARABIC_ELATIVE_VERB if elative else ARABIC_VERB_TAKES) * own]
                if verbs and (not elative or persons >= own / 2):
                    moved.add(form)
                    weights = {l: ARABIC_VERB_MASS[l] for l in verbs}
                    return {l: w / sum(weights.values()) for l, w in weights.items()}
        return None

    def direct(form):
        """The words a form's count goes to, with their shares, before following words that are forms themselves."""
        compared = False
        if form in overrides:
            others = overrides[form]
            if others == [form]:
                return {form: 1.0}
        elif language == "ko" and form in KOREAN_OWN:
            return {form: 1.0}
        elif language == "ar" and form in ARABIC_CLITICS:
            return {form: 1.0}
        elif language == "sk" and "verb" in pos.get(form, set()) and slovak_positive(form):
            return {slovak_positive(form): 1.0}
        elif language == "ko" and KOREAN_GLOSSED.get(form) and is_word(KOREAN_GLOSSED[form]):
            # A form the pack has as a word (화려한, 먹기, 아빤) is the word it is the form of.
            return {KOREAN_GLOSSED[form]: 1.0}
        elif language == "ko" and form.startswith(KOREAN_THING_COPULA) and form not in pos:
            # 거 with the copula (거라고요, 거냐고, 거니깐), which the rules make 걸다's.
            return {"거": 1.0}
        elif language == "ko" and form in verbal_nouns and is_word(verbal_nouns[form]):
            return {verbal_nouns[form]: 1.0}
        elif language == "ko" and any(form.startswith(KOREAN_STEMS[verb]) and verb in lemmas.get(form, []) for verb in KOREAN_STEMS):
            return {next(verb for verb in KOREAN_STEMS if form.startswith(KOREAN_STEMS[verb])): 1.0}
        elif language == "ko" and form in KOREAN_COPULA_FORMS:
            return {"이다": 1.0}
        elif language == "ko" and form in pos and form.endswith(KOREAN_SENTENCE_ENDINGS) and not pos[form] & KOREAN_NOMINAL and (
                verbs := [l for l in dict.fromkeys(lemmas.get(form, [])) if l != form and is_word(l) and pos[l] & {"verb", "adj"} and korean_stem_of(l, form)]):
            # A verb's polite or formal form that the pack has as a word too (부탁해요 "please", 감사합니다 "thank you") is the verb's.
            weights = {l: weight(l) or 1e-12 for l in verbs}
            return {l: w / sum(weights.values()) for l, w in weights.items()}
        elif language == "ko" and form.endswith("게") and "adv" in pos.get(form, set()) and is_word(form[:-1] + "다") and pos[form[:-1] + "다"] & {"verb", "adj"}:
            # An adverb in -게 is its adjective's form (짧게 of 짧다, 크게 of 크다); the pro-forms (어떻게, 그렇게) are overrides.
            return {form[:-1] + "다": 1.0}
        elif language == "ko" and (noun := korean_hada_noun(form)):
            # A verb or adjective of a noun and 하다 or 되다 (운동하다, 요리하다, 발표되다) is the noun's where the noun is the
            # commoner: a reader who knows 운동 reads 운동해요. KOREAN_APART keeps those whose noun means another thing (이상하다).
            merged[noun].append(form)
            merged_verbs.add(form)
            return {noun: 1.0}
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
        elif is_word(form) and form not in overrides and not (others and pos[form] & WORD_POS <= {"intj"} and language not in ("ja", "ar")):
            # A form that is a word itself keeps its count ("casa", not "casar") unless [TAKEOVER] says otherwise.
            # Korean's forms tables are full of nouns spelled as some verb's form (사실 of 사다, 우려 of 우리다),
            # so there a word keeps its count, and the few that are a verb's form are overrides (해 of 하다).
            if language == "ko" and not pos[form] & {"verb", "adj"}:
                return {form: 1.0}
            # A Romanian noun spelled as a commoner verb's form keeps its count and forms (fiu "son", not fi's; stea
            # "star", not sta's), and a verb spelled as a noun's form shares it (urma, lupta).
            if language == "ro":
                shares = romanian_shares(form, others)
                if shares:
                    return shares
            # Without its vowels, an Arabic word is often spelled as another's verb form too (أنت as a form of أن "to
            # moan"): it keeps its count.
            if language == "ar":
                shares = arabic_moved(form, others)
                if shares:
                    return shares
                # Nor is it a noun's form because it is spelled so (أمة "nation", not عبد's feminine; حوالي, not حال's): only
                # another spelling (علي of على), a little word's with a pronoun (لها of ل), a verb's form that is only a verb
                # itself (أرى of رأى), a plural the dictionary has as such (أيام of يوم), or a form of a word that inflects as it
                # does (هذه of هذا).
                def arabic_form_of(l):
                    kind = ARABIC_FORMS.get(form, {}).get(l, False)
                    return kind in (None, "suffix") or kind is True and pos[form] & WORD_POS == {"verb"} or (
                        kind == "number" and (form in ARABIC_PLURALS or form in ARABIC_LEXEMES)) or (
                        kind is not True and pos[form] & INFLECTED_KINDS and pos.get(l, set()) & INFLECTED_KINDS)
                others = [l for l in others if arabic_form_of(l)]
            inflected = bool(pos[form] & INFLECTED_KINDS)
            mine = weight(form)
            others = [
                l for l in others
                if weight(l) >= TAKEOVER * mine or (inflected and pos[l] & INFLECTED_KINDS and weight(l) >= INFLECTED_TAKEOVER * mine)
            ]
            if not others:
                return {form: 1.0}
            compared = True
        if language == "ko" and form not in overrides and not compared:
            # A form that is a word itself went to its verb above by [TAKEOVER]; others go by KOREAN_LOOKALIKE.
            near = [l for l in others if weight(l) >= count(form)]
            if not near:
                rules = {lemma for lemma, steps in korean_dictionary_forms(form) if steps and korean_stem_of(lemma, form)}
                near = [l for l in others if l in rules and KOREAN_LOOKALIKE * weight(l) >= count(form)]
            others = near
        if not others:
            return {form: 1.0} if form in JA_ORPHANS else {}
        if language == "ar" and len(others) > 1:
            kinds = ARABIC_FORMS.get(form, {})
            weights = {l: (ARABIC_VERB_MASS.get(l, 0.0) if kinds.get(l) is True else ARABIC_SPELLED.get(form, {}).get(l, 0.0) if kinds.get(l, 0) is None
                           else weight(l)) or 1e-12 for l in others}
        else:
            weights = {l: weight(l) or 1e-12 for l in others}
        whole = sum(weights.values())
        return {l: w / whole for l, w in weights.items()}

    romanian_forms = defaultdict(set)

    def romanian_own(word):
        """How common a Romanian word's forms are that are no other word's nor a noun themselves (fiul of fiu)."""
        if not romanian_forms:
            for form, of in lemmas.items():
                if len(set(of)) == 1 and "noun" not in ROMANIAN_KINDS.get(form, set()):
                    romanian_forms[of[0]].add(form)
        return sum(count(f) for f in romanian_forms.get(word, ()))

    def romanian_shares(form, others):
        """
        The words a Romanian word goes to by its kind, or None for the general rule. A word the dictionary has only as
        another's form is that word's (uită of uita). A noun spelled as a verb's form keeps its count when text has its
        own forms (fiul of fiu), or its articled form is the verb (cină and cina), but not the verb's participle (dus of
        duce). A verb spelled as a noun's or adjective's form shares that spelling with it (lupta with luptă), or gives
        it to it when text has few forms of the verb (griji, worries, not "to take care").
        """
        kinds = ROMANIAN_KINDS.get(form, set())
        if form in ROMANIAN_KINDS and not kinds and others:
            weights = {l: weight(l) or 1e-12 for l in others}
            return {l: w / sum(weights.values()) for l, w in weights.items()}
        nominal = lambda word: bool(ROMANIAN_KINDS.get(word, set()) & {"noun", "adj"})  # noqa: E731
        verbal = lambda word: "verb" in ROMANIAN_KINDS.get(word, set()) <= {"verb", "intj"}  # noqa: E731
        common_forms = lambda word: romanian_own(word) >= ROMANIAN_OWN_FORMS * count(word)  # noqa: E731
        if verbal(form):
            nouns = [l for l in others if nominal(l)]
            if not nouns:
                return None
            if not common_forms(form):
                return {l: 1.0 / len(nouns) for l in nouns}
            # The articled noun is the commoner reading; an adjective takes its article seldom (continua, to continue).
            articled = [l for l in nouns if "noun" in ROMANIAN_KINDS[l] and form in (l[:-1] + "a", l + "a")]
            if articled:
                return {form: 1.0 - ROMANIAN_ARTICLED_SHARE, **{l: ROMANIAN_ARTICLED_SHARE / len(articled) for l in articled}}
            # A noun's plural (munci, works and to work) is shared alike; an adjective's form (prezenta) by how common
            # each word's own forms are.
            weights = {l: romanian_own(form) if "noun" in ROMANIAN_KINDS[l] else romanian_own(l) for l in nouns}
            weights[form] = romanian_own(form)
            whole = sum(weights.values())
            return {l: w / whole for l, w in weights.items()} if whole else None
        if kinds & {"noun", "adj"} and "verb" not in kinds:
            verbs = [l for l in others if verbal(l)]
            articled = any(l in (form[:-1] + "a", form + "a") and form in lemmas.get(l, ()) for l in verbs)
            participle = any(form in (l[:-1] + "at", l[:-2] + "ut", l + "t") or l.endswith("e") and form[-1] in "ts" and form[0] == l[0] for l in verbs)
            if verbs and (articled or common_forms(form) and not participle):
                return {form: 1.0}
        return None

    merged = defaultdict(list)
    merged_verbs = set()
    # The forms that reached their word through a verb merged into it (생각되지 through 생각되다 to 생각).
    via = {}

    def korean_hada_noun(form):
        light = next((v for v in KOREAN_MERGED_LIGHT_VERBS if form.endswith(v)), None)
        noun = form[:-len(light)] if light else ""
        if len(noun) < 2 or form in KOREAN_APART or not is_word(form) or not pos[form] & {"verb", "adj"}:
            return None
        # The noun must keep its count, not be sent to a verb whose form it spells (보고 of 보다, 작고 of 작다).
        if not is_word(noun) or "noun" not in pos[noun] or overrides.get(noun, [noun]) != [noun] or noun in verbal_nouns:
            return None
        return noun if weight(noun) >= weight(form) else None

    resolved = {}

    def targets_of(form, depth=0):
        """[direct], following a word that is itself a form of another ("essas" to "essa" to "esse")."""
        if form in resolved:
            return resolved[form]
        result = defaultdict(float)
        for word, part in direct(form).items():
            if word != form and depth < 3:
                onward = targets_of(word, depth + 1)
                # An Arabic word spelled as another's verb form keeps its own forms (يتكون of تكون, not of كان), and a verb
                # spelled as another word keeps its own (لن of لان "to soften", not of لأن); but not one sent to its word by
                # rule or by hand (تبقى to بقي), whose spellings go with it.
                if language == "ar" and word not in moved and word not in overrides and (
                        any(ARABIC_FORMS.get(word, {}).get(t) is True for t in onward) or ARABIC_FORMS.get(form, {}).get(word) is True):
                    onward = None
                if onward and word not in onward:
                    if word in merged_verbs:
                        via[form] = word
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
    listed, listed_as = set(ranked), set()
    # A Korean form shared by verbs (산다 of 살다 and 사다, 준다 of 주다 and 줄다) is listed under one it goes to
    # KOREAN_LISTED times less than to another only for its share of the count: a course counts 산다 as no use of 사다.
    main = {}
    if language == "ko":
        for word in ranked:
            for form, share in forms[word].items():
                if form not in main or share > forms[main[form]][form]:
                    main[form] = word
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        # A form sent by hand to several words is listed under each (끈다 of 끄다 and 끌다).
        counted = sorted((f for f in forms[word] if f not in main or KOREAN_LISTED * forms[word][f] >= forms[main[f]][f]
                          or word in overrides.get(f, ())), key=lambda f: -forms[word][f])
        # A Korean form of a word sent to another by hand (해야할 of 해야하다, 해야 하다 written as one) is listed
        # however many forms come before it: the reader finds no word of the list for it otherwise.
        everything = counted
        counted = counted[:FORMS_PER_WORD] + [f for f in counted[FORMS_PER_WORD:] if f in required or (language == "ar" and f in ARABIC_SHARED) or (
            language == "ko" and lemmas.get(f) and all(overrides.get(l, [l]) != [l] for l in lemmas[f]))]
        if language == "ja" and any(is_cjk(c) and not is_kana(c) for c in word):
            # A kanji spelling reached through a kana word read alike (枯れる through かれる to 狩る, 湧く through
            # わく to 分ける) is another word: a kanji word's forms share a kanji with it.
            counted = [f for f in counted if not any(is_cjk(c) and not is_kana(c) for c in f) or set(f) & set(word)]
        # The verbs in 하다 and 되다 sent to their noun, which texts seldom write in their dictionary form, are listed for the
        # reader to find, with as many of each one's forms as it would have had listed under it (생각되지 of 생각되다, under 생각).
        for verb in merged.get(word, []):
            own = [f for f in everything if via.get(f) == verb][:FORMS_PER_WORD]
            counted = list(dict.fromkeys(counted + [verb] + own))
        if word == "이다" and language == "ko":
            # The reader reads the copula's 면 (정도 + 면) as 이다's, which the list counts as 이면, and cuts every
            # form of the copula it knows, however seldom (사람이었기), as a word of the list.
            counted += [f for f, _ in KOREAN_COPULA if f not in counted and f not in KOREAN_COPULA_WORDS]
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
        shown = common.get(word) or guessed.get(word) or word
        if language == "ar":
            # The commonest spelling heads the word (أن, not ان), of those mostly counted for it and no word of the list.
            spelled = [f for f in counted if word in arabic_spellings(f) and count(f) > count(word) and forms[word][f] >= count(f) / 2
                       and f not in listed and f not in listed_as]
            shown = max(spelled, key=count) if spelled else shown
            listed_as.add(shown)
        lines.append(shown + "\t" + " ".join(counted))
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
