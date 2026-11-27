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


# The particles a verb or a sentence ends in and the helper verbs a verb takes after it, which ICU's list writes onto the
# words before them (ရပါတယ်, လူများ, ပြန်လာ, လုပ်ပေး, သွားချင်): an entry the pack lacks that is words and one of these, cut
# between syllables, is read as those words.
ENDINGS = ("တယ်", "ပါ", "ဘူး", "လား", "လဲ", "မယ်", "ပြီ", "သည်", "မည်", "ခဲ့", "ရဲ့", "ကို", "နဲ့", "တွေ", "များ", "က", "မှာ",
           "လည်း", "ပဲ", "တော့", "တာ", "၏", "ပြီး", "နှင့်", "မှ", "သို့", "၌", "၍", "လာ", "သွား", "ပေး", "ထား", "လိုက်", "နေ",
           "ကြည့်", "ချင်", "နိုင်", "ကြ")
# Words of the pack the texts write mostly as other words: ကတော့ "funnel" is nearly always the particles က + တော့; အ
# "dumb" is the prefix အ- the cut leaves (အ + ကြာ) and the subtitles' "uh".
EXCLUDE = {"ကတော့", "အ"}

# Syllables of foreign words and names that are no word alone in ordinary Burmese, or whose own word (ယာ "upland field", ကေ
# "hair") is rare beside them: the Latin letters (ဘီ, ဂျီ, ပီ, အေ), the vowels' names, and the syllables the texts spell
# foreign names and words with (ဟယ်ရီ "Harry", အင်တာနက် "internet", ဒီမိုကရေစီ, မော်ဒယ်). The reader cuts them so, as the
# pack and ICU's list have them; the list counts neither them nor a word next to one, as it counts no word next to letters no
# word covers.
FRAGMENTS = frozenset("""
ကေ တီ ဘီ ဂျီ ပီ အေ ဂျေ ဗွီ အာရ် အီ အာ ယာ ရီ ရိ လီ ယို မို ယား ဂျင် အင် မက် အက် အိုင် အန် မော် တိ ချာ ဘု ဘတ် ဖာ
ဂျက် ကွန် လန် မင် ကတ် ဇာ ပ န ဘ ဖ ဇ နို
""".split())

# Loanwords neither the pack nor ICU's list has, which the reader would cut into syllables that are words (အို "old" + ကေ
# "hair"): the app's own list has them, and the spellings below.
EXTRA_WORDS = ("အိုကေ", "တီဗီ", "တီဗွီ")

# Spellings the pack lacks, each filed under the word of the pack it spells, as the list's forms: အကို for အစ်ကို "elder
# brother"; တစ်လျောက် and တလျှောက် for တစ်လျှောက် "all along" (whose piece လျောက် the cut left alone); မွတ်ဆလင် and
# မွတ်စလင်မ် for မွတ်စလင် "Muslim" (whose မွတ် it left); the creaky တကယ့် of တကယ် "really" (တကယ့်ကို), which no rule
# makes, as the adverb takes no creaky tone; and အကျော်ကြား of ကျော်ကြား "famous" (အကျော်ကြားဆုံး "the most famous"),
# which the cut read as အကျော် + ကြား. The app's own list has them, so the reader reads each whole.
SPELLINGS = {
    "အကို": "အစ်ကို", "တစ်လျောက်": "တစ်လျှောက်", "တလျှောက်": "တစ်လျှောက်", "တလျောက်": "တစ်လျှောက်",
    "မွတ်ဆလင်": "မွတ်စလင်", "မွတ်စလင်မ်": "မွတ်စလင်", "တကယ့်": "တကယ်", "အကျော်ကြား": "ကျော်ကြား", "တဝိုက်": "တစ်ဝိုက်",
}

# Words the pack lacks that the texts use most, each with its kind: ICU's list has them, and the reader reads them as words
# of their own, but with no entry in the pack the list had none of them. The connectives and the quotative of the written
# style (ဟု, ထို့ကြောင့်, သဖြင့်), its paired verbs (ရေးသား "to write", တွေ့ရှိ "to find"), everyday nouns (သစ်သား "wood",
# ခန်းမ "hall", ငှက်ပျောသီး "banana"), and the countries whose names are their languages too (ဂျပန် "Japan; Japanese",
# ပြင်သစ် "France; French"), which the pack has as names only, and ဥရောပ "Europe".
WORDS = {
    "ဟု": "particle", "ထို့ကြောင့်": "conj", "ထို့နောက်": "conj", "သဖြင့်": "conj", "ထိုအခါ": "conj", "သစ်သား": "noun",
    "တစ်နေ့": "noun", "ခန်းမ": "noun", "ငှက်ပျောသီး": "noun", "ဂျပန်": "noun", "ပြင်သစ်": "noun", "ဥရောပ": "noun",
    "အဓိပ္ပာယ်": "noun", "တန်ဖိုး": "noun", "တပ်ဖွဲ့": "noun", "စွမ်းအား": "noun", "ခန္ဓာကိုယ်": "noun", "ကျေးရွာ": "noun",
    "အကြောင်းအရာ": "noun", "စီမံကိန်း": "noun", "သက်တမ်း": "noun", "အပူချိန်": "noun", "ဆုံးဖြတ်ချက်": "noun",
    "စည်းမျဉ်း": "noun", "လှုပ်ရှားမှု": "noun", "တစ်ဝိုက်": "noun", "လျှပ်လက်": "noun", "အချင်းချင်း": "pron",
    "အပါအဝင်": "prep", "ပိုမို": "adv", "ဆက်လက်": "adv", "အနည်းငယ်": "adv", "ထပ်မံ": "adv", "တရားဝင်": "adj",
    "ကောင်းမွန်": "adj", "ခက်ခဲ": "adj", "လုံလောက်": "adj", "လွယ်ကူ": "adj", "မြင့်မား": "adj", "ဆိုးရွား": "adj",
    "များပြား": "adj", "တူညီ": "adj", "ရေးသား": "verb", "ဖြစ်ပေါ်": "verb", "တွေ့ရှိ": "verb", "စကားပြော": "verb",
    "ဆိုလို": "verb", "သေဆုံး": "verb", "ဖြစ်ပွား": "verb", "စီမံ": "verb", "ထိန်းချုပ်": "verb", "ထမ်းဆောင်": "verb",
    "လုပ်ကိုင်": "verb", "ကျေးဇူးပြု": "verb", "ဖွံ့ဖြိုး": "verb", "စိုက်ပျိုး": "verb", "ထည့်သွင်း": "verb",
    "ပေါ်ပေါက်": "verb", "ဖွင့်လှစ်": "verb", "ခန့်အပ်": "verb", "ဆင်းသက်": "verb", "စိုးရိမ်": "verb", "ပေါင်းစပ်": "verb",
    "တက်ရောက်": "verb", "သွားရောက်": "verb", "ထုတ်ပြန်": "verb", "သက်ရောက်": "verb", "ပို့ဆောင်": "verb",
    "ချီးမြှင့်": "verb", "လှူဒါန်း": "verb", "ရိုက်ကူး": "verb", "ပေးအပ်": "verb", "ရောင်းချ": "verb", "ကြိုးပမ်း": "verb",
    "နှစ်သက်": "verb", "ဂုဏ်ယူ": "verb", "မူတည်": "verb", "မေးမြန်း": "verb", "ရည်ညွှန်း": "verb", "ရပ်တန့်": "verb",
    "ဝယ်ယူ": "verb", "ဖြတ်သန်း": "verb", "ဖော်ထုတ်": "verb", "အသုံးချ": "verb",
}

# Entries of ICU's list the pack lacks that the reader reads as the pack's words (make_lexicon), besides those it finds.
CUT = {"ခြင်ဆေး": ["ခြင်", "ဆေး"], "အဖျားတက်": ["အဖျား", "တက်"], "လျှပ်ပြက်": ["လျှပ်", "ပြက်"]}


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


def pack_cut(word, pack):
    """[word] cut between syllables into the fewest words of [pack] (the first such cut), or None if it cannot be."""
    best = [None] * (len(word) + 1)
    best[0] = []
    for i in range(len(word)):
        if best[i] is None:
            continue
        for j in range(i + 1, len(word) + 1):
            if (j - i < len(word) and my_break(word, j) and word[i:j] in pack
                    and (best[j] is None or len(best[i]) + 1 < len(best[j]))):
                best[j] = best[i] + [word[i:j]]
    return best[len(word)]


def icu_words(icu_path, pack):
    """
    ICU's Burmese word list (LeRoy Benjamin Sharon's): its Burmese entries normalised, a verb or adjective without its
    citation ending (CITATION_ENDINGS), none the pack lacks that is words and one of ENDINGS (ပြန်လာ is ပြန် + လာ, ရပါတယ်
    ရ + ပါ + တယ်).
    """
    words = set()
    for line in open(icu_path, encoding="utf-8-sig"):
        word = line.strip().lstrip("\ufeff")
        if not word or word.startswith("#") or not all("\u1000" <= c <= "\u109f" for c in word) or MY_RUN.fullmatch(word) is None:
            continue
        word = my_normalise(word)
        for ending in CITATION_ENDINGS:
            if word.endswith(ending) and len(word) > len(ending) and my_break(word, len(word) - len(ending)):
                word = word[:-len(ending)]
                break
        words.add(word)
    known = words | pack
    return {w for w in words if w in pack or not any(len(w) > len(e) and w.endswith(e) and my_break(w, len(w) - len(e))
                                                     and cuts_into(w, 0, len(w) - len(e), known) for e in ENDINGS)}


def make_lexicon(icu_path):
    """
    The app's Burmese word list from ICU's (icu_words), without the entries the pack lacks that are two of its nouns, cut
    between syllables (ခရုခွံ "snail shell" is ခရု "snail" + ခွံ "shell", လေယာဉ်ကွင်း လေယာဉ် + ကွင်း), nor those of CUT:
    the reader reads them as the pack's words, which it can look up, as it reads Lao's phrases (LaoParser). Not two words
    of another kind, which are often one word, a pair of verbs that mean one thing (လုံလောက် "enough", သန်မာ "strong"), nor
    a word twice (ဖေဖေ "daddy"). The pack's words they are cut into are on the list too, for a reader without the pack; and
    so are EXTRA_WORDS, the spellings of SPELLINGS and the words the frequency list adds by hand (WORDS).
    """
    pack = pack_words()
    db = sqlite3.connect(os.path.join(DICTIONARIES, "my-en.sqlite"))
    kinds = {}
    for word, pos in db.execute("SELECT word_lc, pos FROM entries"):
        kinds.setdefault(my_normalise(word), set()).add(pos)
    nouns = {w for w, k in kinds.items() if k <= {"noun", "classifier"}}
    words = icu_words(icu_path, pack)
    own = set(EXTRA_WORDS) | set(SPELLINGS) | set(WORDS)
    kept, pieces = set(), set()
    for word in sorted(words):
        cut = None if word in pack or word in own else CUT.get(word) or pack_cut(word, pack)
        if word not in CUT and (cut is None or len(cut) != 2 or cut[0] == cut[1] or not all(p in nouns for p in cut)):
            kept.add(word)
        else:
            pieces.update(cut)
    return sorted(kept | pieces | own)


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
