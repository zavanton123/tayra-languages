"""
Burmese for tools/build_frequency_list.py (build_burmese) and tools/check_course.py: how the reader reads Burmese text
(BurmeseParser), and the app's own Burmese word list (files/lexicon/my.txt), made from ICU's.

Burmese is written without spaces between words (spaces part phrases), so a run of its letters is cut into its likeliest
words by how common they are, as Lao and Khmer are, but never inside a syllable (my_break): a word does not end before a
vowel sign, medial, tone mark, asat or virama (U+102B-U+103F and the other Myanmar signs), nor after a virama (the consonant
it stacks belongs to the syllable before), nor before a consonant that the asat or virama after it closes (တင် is no တ +
င်). ၊ ။ and the Myanmar digits are no words; ၌ ၍ ၎ ၏ are letters.

Text is read Unicode-normalised as the reader does (my_normalise): dot below before asat (U+1037 U+103A, as NFC orders
them), ဥ + ီ as ဦ, and ၀ (zero) between letters as ဝ (wa), the common mistyping (၀င် is ဝင် "enter"). Zawgyi, the older
font encoding that stores the same letters on other code points and in visual order, is no Unicode Burmese: the builder
leaves out every line of a source that Google's myanmartools takes for Zawgyi (zawgyi_lines).

Usage: python3 tools/frequency_burmese.py --lexicon   writes files/lexicon/my.txt from ICU's burmesedict.txt
"""
import os
import re
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")
CACHE = os.path.join(DICTIONARIES, "frequency-sources")
LEXICON = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "lexicon", "my.txt")
ICU_VERSION = "78.3"
ICU_DICT = f"https://raw.githubusercontent.com/unicode-org/icu/release-{ICU_VERSION}/icu4c/source/data/brkitr/dictionaries/burmesedict.txt"

# As BurmeseParser has them: the reader's Burmese letters (its word characters, U+1000-U+103F and U+104C-U+109F: not the
# digits U+1040-U+1049 nor ၊ ။), the signs written on a letter, the asat and the virama.
MY_RUN = re.compile("[\u1000-\u103f\u104c-\u109f]+")
MY_SIGNS = frozenset(
    [chr(c) for c in range(0x102B, 0x1040)] + [chr(c) for c in range(0x1056, 0x105A)] + [chr(c) for c in range(0x105E, 0x1061)]
    + [chr(c) for c in range(0x1062, 0x1065)] + [chr(c) for c in range(0x1067, 0x106E)] + [chr(c) for c in range(0x1071, 0x1075)]
    + [chr(c) for c in range(0x1082, 0x108E)] + ["\u108f"] + [chr(c) for c in range(0x109A, 0x109E)]
)
ASAT, VIRAMA, DOT_BELOW = "\u103a", "\u1039", "\u1037"
DIGITS = frozenset(chr(c) for c in range(0x1040, 0x104A))


def my_break(run, at):
    """Whether a Burmese word may end before [at] in [run] (BurmeseParser.canBreak): between syllables only."""
    if at <= 0 or at >= len(run):
        return True
    if run[at] in MY_SIGNS or run[at - 1] == VIRAMA:
        return False
    after = at + 1
    if after < len(run) and run[after] == DOT_BELOW:
        after += 1
    return not (after < len(run) and run[after] in (ASAT, VIRAMA))


def my_normalise(text):
    """BurmeseParser.normalise: asat + dot below as dot below + asat, ဥ + ီ as ဦ, ၀ next to a letter and no digit as ဝ."""
    if not any("\u1000" <= c <= "\u109f" for c in text):
        return text
    text = text.replace("\u103a\u1037", "\u1037\u103a").replace("\u1025\u102e", "\u1026")
    if "\u1040" not in text:
        return text
    out = list(text)
    for i, c in enumerate(text):
        if c != "\u1040":
            continue
        before = text[i - 1] if i > 0 else ""
        after = text[i + 1] if i + 1 < len(text) else ""
        if before in DIGITS or after in DIGITS:
            continue
        if "\u1000" <= before <= "\u103f" or "\u1000" <= after <= "\u103f":
            out[i] = "\u101d"
    return "".join(out)


# The words a sentence or a phrase ends in, which the ICU list writes onto the word before them (ရပါတယ်, ဒီမှာ, လူများ): an
# entry of the list that is words of the lexicon and one of these, cut between syllables, is left out of the app's list.
PARTICLES = ("တယ်", "ပါ", "ဘူး", "လား", "လဲ", "မယ်", "ပြီ", "သည်", "မည်", "ခဲ့", "ရဲ့", "ကို", "နဲ့", "တွေ", "များ", "က",
             "မှာ", "လည်း", "ပဲ", "တော့", "တာ", "၏", "ပြီး", "နှင့်", "မှ", "သို့", "၌", "၍")
# Words of the pack the texts write mostly as other words: ကတော့ "funnel" is nearly always the particles က + တော့.
EXCLUDE = {"ကတော့"}

# Syllables of foreign words and names that are no word alone in ordinary Burmese, or whose own word (ယာ "upland field", ကေ
# "hair") is rare beside them: the Latin letters (ဘီ, ဂျီ, ပီ, အေ), the vowels' names, and the syllables the texts spell
# foreign names and words with (ဟယ်ရီ "Harry", အင်တာနက် "internet", ဒီမိုကရေစီ, မော်ဒယ်). The reader cuts them so, as the
# pack and ICU's list have them; the list counts neither them nor a word next to one, as it counts no word next to letters no
# word covers.
FRAGMENTS = frozenset("""
ကေ တီ ဘီ ဂျီ ပီ အေ ဂျေ ဗွီ အာရ် အီ အာ ယာ ရီ ရိ လီ ယို မို ယား ဂျင် အင် မက် အက် အိုင် အန် မော် တိ ချာ ဘု ဘတ် ဖာ
ဂျက် ကွန် လန် မင် ကတ် ဇာ
""".split())

# Loanwords neither the pack nor ICU's list has, which the reader would cut into syllables that are words (အို "old" + ကေ
# "hair"): the app's own list has them.
EXTRA_WORDS = ("အိုကေ", "တီဗီ", "တီဗွီ")


# The letters before a final asat whose syllable can take the creaky tone: the nasals, ယ, and the ာ of ော်.
CREAKY_BEFORE_ASAT = frozenset("\u1004\u1009\u100a\u1014\u1019\u101a\u102c")


def creaky(word):
    """
    [word] in the induced creaky tone a noun or pronoun takes before the noun it qualifies (ဘုရင့် "the king's", ကမ္ဘာ့
    "the world's", ခင်ဗျာ့ "your"): ့ on a final syllable with a nasal (င် ဉ် ည် န် မ်), ယ် or ော် (before the asat, as NFC
    orders them), or ending in ာ, ါ, ူ or ေ with no tone mark, or in place of the း of ား, ါး. None for other words: a
    syllable that ends in a stop (က် စ် တ် ပ်) has no tone to change.
    """
    if word.endswith(ASAT) and len(word) > 2 and word[-2] in CREAKY_BEFORE_ASAT:
        return word[:-1] + DOT_BELOW + ASAT
    if word[-1:] in ("\u102c", "\u102b", "\u1030", "\u1031") and len(word) > 1:
        return word + DOT_BELOW
    if word.endswith(("\u102c\u1038", "\u102b\u1038")):
        return word[:-1] + DOT_BELOW
    return None


# ICU's verbs and adjectives are cited with the sentence ending သည် or the relative သော (စားသည် "to eat", ကောင်းသော "good"),
# which the text writes apart: the entry is the word before it.
CITATION_ENDINGS = ("သည်", "သော")


def pack_words(path=None):
    """The headwords and forms of the my-en pack, as the reader has them."""
    db = sqlite3.connect(path or os.path.join(DICTIONARIES, "my-en.sqlite"))
    return {my_normalise(w) for (w,) in db.execute("SELECT word_lc FROM entries UNION SELECT form_lc FROM forms") if w and " " not in w}


def lexicon_words():
    """The app's own Burmese word list (files/lexicon/my.txt)."""
    return {l.strip() for l in open(LEXICON, encoding="utf-8") if l.strip() and not l.startswith("#")}


def cuts_into(word, start, end, words):
    """Whether [word] from [start] to [end] can be cut between syllables into words of [words]."""
    reached = [False] * (end + 1)
    reached[start] = True
    for i in range(start, end):
        if not reached[i]:
            continue
        for j in range(i + 1, end + 1):
            if my_break(word, j) and word[i:j] in words:
                reached[j] = True
    return reached[end]


def make_lexicon(icu_path):
    """
    The app's Burmese word list from ICU's (LeRoy Benjamin Sharon's word list): its Burmese entries normalised, a verb or
    adjective without its citation ending (CITATION_ENDINGS), and no entry that is words and a particle (PARTICLES) unless the
    pack has it as a word of its own (ဟိုတယ် "hotel", ဒီမှာ "here").
    """
    pack = pack_words()
    words = set()
    for line in open(icu_path, encoding="utf-8-sig"):
        word = line.strip().lstrip("﻿")
        if not word or word.startswith("#") or not all("\u1000" <= c <= "\u109f" for c in word) or MY_RUN.fullmatch(word) is None:
            continue
        word = my_normalise(word)
        for ending in CITATION_ENDINGS:
            if word.endswith(ending) and len(word) > len(ending) and my_break(word, len(word) - len(ending)):
                word = word[:-len(ending)]
                break
        words.add(word)
    words.update(EXTRA_WORDS)
    known = words | pack
    kept = set()
    for word in words:
        if word not in pack and any(len(word) > len(p) and word.endswith(p) and my_break(word, len(word) - len(p))
                                    and cuts_into(word, 0, len(word) - len(p), known) for p in PARTICLES):
            continue
        kept.add(word)
    return sorted(kept)


def zawgyi_lines(lines):
    """
    The lines of [lines] that are Unicode Burmese, not Zawgyi: Google's myanmartools (pip install myanmartools) gives the
    likelihood of Zawgyi, and a line it gives 0.5 or more is left out. A line both encodings would write the same (လာပါ) is
    near 0.5 either way; it is kept below that, and so are lines with no Burmese letter (-inf).
    """
    try:
        from myanmartools import ZawgyiDetector
    except ImportError:
        sys.exit("Burmese needs myanmartools to leave out Zawgyi text: pip install myanmartools")
    detector = ZawgyiDetector()
    for line in lines:
        if detector.get_zawgyi_probability(line) < 0.5:
            yield line


def main():
    if sys.argv[1:] != ["--lexicon"]:
        sys.exit(__doc__.strip().splitlines()[-1])
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from build_frequency_list import download  # noqa: E402
    path = download(ICU_DICT, f"icu-{ICU_VERSION}-burmesedict.txt")
    words = make_lexicon(path)
    with open(LEXICON, "w", encoding="utf-8") as f:
        f.write(f"# source: ICU {ICU_VERSION} burmesedict (Unicode License v3; Copyright (C) 2016 and later: Unicode, Inc. and others;"
                " word list (c) 2013 LeRoy Benjamin Sharon)\n")
        f.write("# Burmese words the reader cuts text into, besides the frequency list's and the installed packs'; made by"
                " tools/frequency_burmese.py --lexicon\n")
        # The word list's licence asks for its notice with every copy.
        for line in open(path, encoding="utf-8-sig"):
            line = line.strip().lstrip("\ufeff")
            if line.startswith("#") and "--------" not in line:
                f.write("# " + line.lstrip("#").strip() + "\n")
        for word in words:
            f.write(word + "\n")
    print(f"Wrote {LEXICON}: {len(words):,} words", file=sys.stderr)


if __name__ == "__main__":
    main()
