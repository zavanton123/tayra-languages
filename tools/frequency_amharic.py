"""
The Amharic frequency list (build_frequency_list.py --language am).

Amharic writes a word with its prepositions and conjunctions in front (በቤት "in a house", የልጁ "the boy's", ከእኔ, ስለሚሄድ) and
its article, possessive, object and clitic endings behind (ቤቱ, ቤታችን, ልጆቹን, ቤቱም), and a verb with its person, tense,
relative and negative markers on both sides (ይሰብራል, የሚሰብሩት, አልሰበረም, እንዲመጣ). The Ethiopic script writes a consonant
and the vowel after it as one letter, so an ending fuses with the letter before it (ቤት + -u is ቤቱ, ቤት + -oč is ቤቶች). Each
text word is therefore read here as consonants and vowels (syllables), its affixes taken off as sound, not as letters.

A text word is credited to the words of the am-en pack (dictionaries/am-en.sqlite: the English Wiktionary's Amharic entries
and the Amharic words of its translation tables) it is a form of: itself when it is one, else a form the pack lists, else a
word with its prepositions and endings taken off (nouns, adjectives, pronouns, numbers), else a verb whose root consonants are
left when the verb's markers are taken off (verb_lemmas). Spellings that sound alike count as one (ሀ ሐ ኀ, ሰ ሠ, አ ዐ, ጸ ፀ,
ሀ ሃ, አ ኣ), as Amharic writes them either way. A word of the pack keeps its own count; a form of several words is shared by
how common each is by the forms of no other.

Counted from web text (CC-100's Amharic, and OPUS's CCMatrix), Leipzig's Wikipedia sentences, and the subtitles, talks and
news OPUS has (OpenSubtitles, TED2020, Global Voices), each the share of the blend WEIGHTS gives. Lines in Tigrinya, which the
web corpora mix in, are left out.
"""
import functools
import gzip
import io
import json
import lzma
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

# The reader's Amharic wordCharacters (AddedLanguages.kt): the Ethiopic letters, gemination marks and digits. Its
# exceptionsSplitSentences come first, as in the reader: an abbreviation such as ዓ.ም. or one letter before a full stop (ወ.)
# is one word. A word with a digit is a number, and no word of the list.
LETTER_RANGES = "\\u1200-\\u135a\\u135d-\\u135f\\u1369-\\u137c\\u1380-\\u138f\\u2d80-\\u2dde\\uab01-\\uab2e"
ABBREVIATIONS = ["[A-Z].", "[\\u1200-\\u1359\\u1369-\\u137C].", "ዓ.ም.", "እ.ኤ.አ.", "ዓክልበ.", "እ.አ.አ.", "ኪ.ሜ.", "ክ.በ.", "እ.ኤ.ኣ.",
                 "ዓ.ዓ.", "ኤ.አ.", "አ.አ.", "ወዘተ.", "አ.ም.", "ምዕ."]
WORD = re.compile("(" + "|".join(a.replace(".", "\\.") for a in ABBREVIATIONS) + "|[" + LETTER_RANGES + "]+)", re.IGNORECASE)
DIGIT = re.compile("[\\u1369-\\u137c]")
MARKS = re.compile("[\\u135d-\\u135f]")

# ----- Letters as sounds -----

# Each consonant row's sound, by its Unicode name; rows that sound alike in Amharic share one (ሀ ሐ ኀ ኸ, ሰ ሠ, አ ዐ, ጸ ፀ).
CONSONANTS = {
    "H": "h", "HH": "h", "X": "h", "KX": "h", "L": "l", "M": "m", "MY": "m", "SZ": "s", "S": "s", "R": "r", "RY": "r",
    "SH": "x", "Q": "q", "QH": "q", "B": "b", "V": "v", "T": "t", "C": "c", "N": "n", "NY": "N", "GLOTTAL": "'",
    "PHARYNGEAL": "'", "K": "k", "W": "w", "Z": "z", "ZH": "Z", "Y": "y", "D": "d", "DD": "d", "J": "j", "G": "g", "GG": "g",
    "TH": "T", "CH": "C", "PH": "P", "TS": "S", "TZ": "S", "F": "f", "FY": "f", "P": "p",
}
# The vowel of each order, as its name ends: ä (A), u, i, a, e, none (the sixth order), o, and the labialised ones (W).
_ORDERS = [("WAA", "Wa"), ("WEE", "We"), ("WA", None), ("WI", "Wi"), ("WE", "W"), ("OA", "o"), ("AA", "a"), ("EE", "e"),
           ("A", "A"), ("U", "u"), ("I", "i"), ("E", ""), ("O", "o")]
VOWEL_SOUNDS = set("AuiaeoW")


def _letters():
    out = {}
    for cp in list(range(0x1200, 0x135b)) + list(range(0x1380, 0x1390)) + list(range(0x2d80, 0x2ddf)) + list(range(0xab01, 0xab2f)):
        try:
            name = unicodedata.name(chr(cp))
        except ValueError:
            continue
        if "SYLLABLE " not in name:
            continue
        tail = name.split("SYLLABLE ")[1]
        for suffix, vowel in _ORDERS:
            if tail.endswith(suffix) and len(tail) > len(suffix):
                row = tail[: -len(suffix)].strip()
                if vowel is None:
                    vowel = "Wa" if cp < 0x1380 and (cp - 0x1200) % 8 == 7 else "WA"
                consonant = CONSONANTS.get(row, "?")
                if row == "GLOTTAL" and vowel == "Wa":
                    vowel = "A"  # ኧ, an ä
                # h and the glottal stop sound the same before ä and a (ሀ ሃ, አ ኣ).
                if consonant in "h'" and vowel == "A":
                    vowel = "a"
                out[chr(cp)] = consonant + vowel
                break
    return out


LETTERS = _letters()


def sounds(word):
    """[word] as consonants and vowels: ቤቱ is "betu", ቤቶች "betoc", ሰበረ "sAbArA", አንተ "'antA"."""
    return "".join(LETTERS.get(c, "") for c in MARKS.sub("", word))


def is_amharic(token):
    return bool(token) and not DIGIT.search(token) and all(c in LETTERS or MARKS.match(c) for c in token)


def reader_words(text):
    """[text]'s words as the reader cuts them, abbreviations and numbers among them."""
    return [m.group(0) for m in WORD.finditer(text) if m.group(0)]


def tokens(line):
    """A line's words that may be words of the list: Ethiopic letters only."""
    for word in reader_words(line):
        if is_amharic(word):
            yield word


def skeleton(s):
    """The consonants of [s] but the glottal stop, which comes and goes with the vowels (አደረገ, ያደርጋል, ማድረግ: drg)."""
    return "".join(c for c in s if c not in "AuiaeoW'")


def _syllables(s):
    """[s] as (consonant, vowel) pairs, the vowel "" for the sixth order and W-led for the labialised ones."""
    out = []
    for c in s:
        if c in "AuiaeoW":
            if out:
                out[-1][1] += c
        else:
            out.append([c, ""])
    return out


# ----- Affixes, as sounds -----

# Prepositions and the like written onto a word's front: የ "of", በ "in, by", ለ "for", ከ "from", ስለ "about", እንደ "like",
# ወደ "to", እስከ "until", ከነ "with", በየ "every", እነ (a plural of names), and two of them together (እንደየ, ከየ).
NOMINAL_PREFIXES = ["yA", "bA", "lA", "kA", "slA", "'ndA", "wAdA", "'skA", "kAnA", "bAyA", "'nA", "'ndAyA", "kAyA", "lAyA",
                    "yAyA", "'skAyA", "bAkA", "kAbA", "yAbA", "lAbA", "yAkA"]
# A noun's endings, the outermost first: a clitic (-ም "also", -ስ "and what of", -ና "and", -ማ, -ኮ), the object -ን, the article
# or a possessive (ቤቱ, ቤቷ, ቤቴ, ቤትህ, ቤትሽ, ቤታችን, ቤታችሁ, ቤታቸው, ቤትዎ; after a vowel ቡናው, ቡናዬ, ቡናችን), and the plural
# (ቤቶች, ቡናዎች, the Ge'ez -at of ቀናት).
NOMINAL_LAYERS = [
    ["m", "s", "na", "ma", "ko", "mma"],
    ["n", "nn", "nu", "nnu"],
    ["u", "e", "Wa", "acn", "achu", "acAw", "h", "x", "wo", "itu", "ytu", "Witu",
     "w", "ye", "wa", "cn", "chu", "cAw", "Wacn", "Wachu", "WacAw", "Wa"],
    ["oc", "woc", "at", "an", "yat"],
]

# A verb's front: the preposition or conjunction (as on a noun, and እየ "while"), then the marker of its form: the
# imperfective's person (ይ, ት, እ, እን, ል), its relative (የሚ, የምት, የም, የምን), when (ሲ, ስት, ስ, ስን), if (ቢ, ብት, ብ, ብን),
# about to (ሊ), so that (እንዲ, እንድት, እንድ, እንድን), the negatives (አይ, አት, አል, አን; የማይ, ሳይ, ባይ, እንዳይ), the perfective's
# negative (አል, ያል, ካል, እንዳል, ስላል), and the infinitive's መ, ማ (ለመ, በመ, ከመ, የመ, አለመ, ባለመ).
_PERSON = ["y", "t", "'", "'n", "l", "n"]
_MARKED = (["mi", "mt", "m", "mn", "si", "st", "s", "sn", "bi", "bt", "b", "bn", "li", "lt", "l", "ln",
            "'ndi", "'ndt", "'nd", "'ndn", "'ay", "'at", "'al", "'an", "may", "mat", "mal", "man", "say", "sat", "sal", "san",
            "bay", "bat", "bal", "ban", "'nday", "'ndat", "'ndal", "'ndan", "yal", "kal", "slal", "yamay", "yAmay",
            "mA", "ma", "'alAmA", "'alAma", "balAmA", "kalAmA", "yalAmA", "lAlAmA", "'yA"] + _PERSON)
VERB_PREFIXES = sorted({p + m for p in [""] + NOMINAL_PREFIXES for m in [""] + _MARKED}, key=len)
# What each front may come before (FREE, PERSON, INFINITIVE below): a preposition and the perfective's markers before a
# perfective or gerund, the infinitive's መ before an infinitive, the rest before an imperfective or jussive; አል is the
# perfective's negative and the first person's (አልሰበረም, አልሰብርም).
_FREE_MARKS = {"", "'al", "yal", "kal", "slal", "'ndal", "'yA"}
PREFIX_KINDS = {}
for _p in [""] + NOMINAL_PREFIXES:
    for _m in [""] + _MARKED:
        _kinds = 0
        if _m in _FREE_MARKS:
            _kinds |= 1
        if _m.endswith(("mA", "ma")):
            _kinds |= 4
        elif _m not in _FREE_MARKS or _m == "'al":
            _kinds |= 2
        PREFIX_KINDS[_p + _m] = PREFIX_KINDS.get(_p + _m, 0) | _kinds
# A verb's back, the outermost first: a clitic, the auxiliary of the present (-ኣል: ይሰብራል, ሰብሯል), an object (ሰበረው,
# ሰበራት, ሰበረኝ, ሰበረለት, ሰበረበት), then its person: the perfective's (ሰበረ, ሰበረች, ሰበሩ, ሰበርኩ, ሰበርክ, ሰበርሽ, ሰበርን,
# ሰበራችሁ; በላሁ), the imperfective's (-u, -i), the gerund's (ሰብሮ, ሰብራ, ሰብሬ, ሰብረው, ሰብረህ, ሰብረሽ, ሰብረን, ሰብራችሁ; በልቶ),
# the infinitive's -t of a verb whose last root letter is weak (መብላት), and the article and endings an infinitive takes.
_OBJECTS = ["w", "t", "at", "N", "n", "h", "x", "cAw", "acAw", "achu", "chu", "Aw", "AN", "An", "Ah", "Ax", "wo", "Wo",
            "bAt", "bat", "bN", "bn", "bh", "bx", "bachu", "bacAw", "bwo", "lAt", "lat", "lN", "ln", "lh", "lx", "lachu",
            "lacAw", "lwo", "Wat", "WacAw", "Wachu"]
_PERSONS = ["A", "Ac", "u", "ku", "k", "x", "n", "achu", "hu", "h", "a", "ac", "i", "e", "o", "Aw", "Ah", "Ax", "An",
            "to", "ta", "te", "tAw", "tAh", "tAx", "tAn", "tachu", "ti", "tu", "t", "at", "At", "it", "ot", "Wa", "W",
            "Wacn", "Wachu", "WacAw", "acn", "Wan", "un", "um", "uns", "acAwn"]
_AUXILIARIES = ["al", "alu", "alAc", "alAhu", "alAh", "alAx", "alAn", "alachu", "yalAx", "iyalAx", "Wal", "Awal", "yalAhu",
                "'alAhu", "Ahal", "Axal", "Anal", "achuWal", "Wall", "alAcAw"]
VERB_LAYERS = [["m", "s", "na", "ma", "mm", "n", "nm", "u", "un", "w", "wn", "wm"], _AUXILIARIES, _OBJECTS, _PERSONS]


class Affixes:
    """A set of affixes looked up by length, for taking them off a word's front or back."""

    def __init__(self, affixes):
        self.set = {a for a in affixes if a}
        self.lengths = sorted({len(a) for a in self.set})

    def fronts(self, word):
        return [a for n in self.lengths if n < len(word) and (a := word[:n]) in self.set]

    def backs(self, word):
        return [a for n in self.lengths if n < len(word) and (a := word[-n:]) in self.set]


_NOMINAL_PREFIXES = Affixes(NOMINAL_PREFIXES)
# The same with the a of a word's first letter fused with the prefix's ä (ባንተ, ላንቺ, ካገር, ያገሩ).
_FUSED_PREFIXES = Affixes(p[:-1] + "a" for p in NOMINAL_PREFIXES)
_VERB_PREFIXES = Affixes(VERB_PREFIXES)
_FUSED_VERB_PREFIXES = Affixes([p[:-1] + "a" for p in NOMINAL_PREFIXES + ["'yA"]] + [p + "'ya" for p in NOMINAL_PREFIXES])
_NOMINAL_LAYERS = [Affixes(layer) for layer in NOMINAL_LAYERS]
_VERB_LAYERS = [Affixes(layer) for layer in VERB_LAYERS]


def strip_layers(word, layers):
    """Every (rest, taken) left when some of [layers]' endings are taken off [word]'s end, each layer at most once, outermost
    first; taken is how many sounds went."""
    out = [(word, 0)]
    for layer in layers:
        out += [(rest[: -len(e)], taken + len(e)) for rest, taken in out for e in layer.backs(rest)]
    return out


def nominal_fronts(word):
    """Every (rest, taken) left when a preposition (or none) is taken off [word]'s front, the rest as it was before the
    prefix met it: ባንተ is በ + አንተ (its a fused with the prefix's), የኔ is የ + እኔ (its እ dropped, which counts half a
    sound more, so that በሺዎች is ሺ's before እሺ's)."""
    out = [(word, 0)]
    for prefix in _NOMINAL_PREFIXES.fronts(word):
        rest = word[len(prefix):]
        out.append((rest, len(prefix)))
        if rest[0] not in "AuiaeoW'":
            out.append(("'" + rest, len(prefix) + 0.5))
    for prefix in _FUSED_PREFIXES.fronts(word):
        out.append(("'" + word[len(prefix) - 1:], len(prefix)))
    return out


# ----- The words -----

# Parts of speech that are no words of a text.
NOT_WORDS = {"name", "prefix", "suffix", "infix", "interfix", "symbol", "character", "phrase", "proverb", "prep_phrase"}

# Words no pack entry has, with their kinds. The English pack lacks many of the commonest (ወደ, ግን, ሌላ, መጣ, ሄደ, አገኘ):
# these are the ones the corpora use most that are no form of a word of the pack, and everyday words besides.
AMHARIC_WORDS = {}
for _kind, _words in {
    "prep": "ወደ ስለ እስከ እንደ ከነ ያለ",
    "postp": "ድረስ መካከል በኩል ዘንድ ያህል በቀር ረገድ አኳያ ሳቢያ አጠገብ ታች ባሻገር",
    "conj": "ይልቅ ግን እንጂ እንዲሁም ወይ ታዲያ ስለዚህም እንግዲህ ቢሆንም ሆኖም",
    "adv": """ጭራሽ ልክ ብቅ የምር ትዝ ምነው ዘወትር አስቀድሞ አስመልክቶ ድጋሚ ከፍ እኩል ድንገት ዘወትር እጅግ በተለይ ወዲህ ይበልጥ ቢያንስ ምናልባት ሰሞኑን ኋላ ሁሌም ሁልጊዜ ገደማ ዘንድሮ ቀስ ያኔ ዳግም እዚያ እንዲሁ እንዲያውም እንዲህ
        ቶሎ አብሮ ዝም ደስ ይፋ ጭምር ዝቅ ቁጭ ጣልቃ ትናንትና ትላንት በፍጥነት ብዙውን ወዴት እንደገና አሁንም እስካሁን""",
    "particle": "አዎ እነሆ ሆይ እንኳን እንኳ እኮ እስቲ እስኪ አይደል እንዴ",
    "pron": "እርስ ምናምን ስንት ይኸው ማንም ሁላችን ሁላችሁ እያንዳንዱ",
    "det": "ማንኛውም አንዳንድ ሌላ አብዛኛው መላ",
    "adj": """በርካታ ተጨማሪ ተመሳሳይ ሰላማዊ ጠንካራ ተግባራዊ ተገቢ ፖለቲካዊ መንፈሳዊ አጠቃላይ ታሪካዊ ማህበራዊ ህዝባዊ ምርጥ ትክክለኛ ወታደራዊ
        አዝናኝ አስቂኝ ሕያው በጎ በቂ ሰራሽ ቅርብ ክቡር አልባ ቅድስት ተደጋጋሚ ፈቃደኛ ጋዜጣዊ ውብ አሉታዊ ቀጥተኛ ውድቅ ስኬታማ ሀገራዊ አሳሳቢ ዓመታዊ አስደናቂ
        ተከታታይ መደበኛ አነስተኛ ወቅታዊ ታዋቂ ውጤታማ ታማኝ ማዕከላዊ ህጋዊ መንግስታዊ ተጠያቂ ምቹ ቀጣይ ተንቀሳቃሽ ተጨባጭ ሰብዓዊ ክልላዊ
        አገራዊ አስቸኳይ አስተማማኝ አሳዛኝ ቀዳሚ አስገራሚ ኢኮኖሚያዊ ዴሞክራሲያዊ ብሔራዊ ባህላዊ ቋሚ ሞቃት እውን ይቅር ዘመናዊ ዓለም አቀፍ
        አቀፍ ውድ ርካሽ ደስተኛ ቀጭን ወፍራም ንጹሕ ቆሻሻ ሙቅ ቀዝቃዛ ጣፋጭ ሰነፍ ጎበዝ ብልህ ደግ ክፉ ሀብታም ድሃ ጤነኛ ታማሚ ባዶ""",
    "num": "ሚሊዮን ቢሊዮን ሃምሳ ስልሳ ሰባ ሰማንያ ዘጠና",
    "noun": """ሁኔታ መብት አገልግሎት ቡድን ዓይነት መልክ አስተዳደር መግለጫ ልማት ትግል እንቅስቃሴ ተግባር ተቋም ውይይት እርምጃ ትኩረት
        ሂደት ስብሰባ ትውልድ ነጥብ በዓል መድረክ አቅም ሚኒስቴር ስልጣን ንግግር አመራር አዋጅ ግንባታ ንብረት ዕለት ቢሮ ዞን መስመር
        ሰልፍ ሀብት አቋም አማራጭ ሚና መልዕክት ምክትል ማዕከል አስተሳሰብ አጋጣሚ ቅድሚያ አንቀጽ አቅጣጫ ነዋሪ መጨረሻ ግለሰብ ኃላፊ
        ኃላፊነት መፍትሄ አመለካከት ስህተት ልምድ መነሻ ባለሙያ ተጠቃሚ ማንነት አገዛዝ ዘመቻ ርዕስ ወጪ ተወካይ ኮሚሽን ቁጥጥር ስብከት
        ስጋት ቆይታ ሚዲያ ፕሮጀክት ሰነድ በጀት መከላከያ ትክክል ኢኮኖሚ ምርምር ትዕዛዝ ዳይሬክተር ሙከራ ጋዜጠኛ ስልጠና እቅድ አለቃ
        ንቅናቄ ሜዳ ቀውስ ምርት ቦርድ ሙስና ተሳትፎ ምሁር ርቀት መኖሪያ አዳራሽ ሠራተኛ አካሄድ ኢንዱስትሪ አግባብ ጉብኝት ፎቶ
        ስብስብ ክለብ ተጫዋች አባባል ችሎታ ምግባር ቀጠሮ አስኪያጅ መምሪያ ተፅዕኖ ሰንደቅ ትብብር ማብራሪያ እስረኛ አስተዋጽኦ ባለስልጣን
        ማስታወቂያ ማሳያ አስፈጻሚ ካርድ ብድር ኮሌጅ ማረጋገጫ አቅርቦት ባህሪ እውነታ ሙያ መዋቅር ብዙሃን ምስክር አሰልጣኝ ንድፍ
        አርቲስት ቪዲዮ መምህር አርበኛ ራዕይ ሰበብ መለያ ቅሬታ ምእመን ጣቢያ ዲሞክራሲ እይታ ነዳጅ ትርፍ ምንዛሪ አጠቃቀም ማስታወሻ
        ጠዋት ኩሽና ምሳ እራት አትክልት ብርድ ጥቅምት ጳጉሜ ጉዳት መሐንዲስ ሹፌር ታክሲ ባቡር አውሮፕላን ጣራ ግድግዳ ደረጃ ሻንጣ
        እውቅና ትግበራ አርማ በረከት መድን ስንቅ ዕንቁ ጋሽ ለጋሽ ጥሰት ቀጠና ስልት ስሌት እስላም መብራት ጎን መዝገብ ምዝገባ መቻቻል አስርት መከራ ቅኝ ቅናሽ ቀኖና ዳንስ ጫና ሥላሴ በቀል እትም ምድብ
        ስድብ ምደባ ዝማኔ እስር እጥረት እልቂት ርስት እጥፍ መጠን ቅርስ እገዛ ብቃት ሰሞን መርህ ስፍራ ፍሬ ምትክ ምቾት መቀመጫ ቀኝ ግራ መሃል ስኬት ሴራ ምልክት አንፃር ማህበር መገናኛ በላይነት አማካኝነት ምርመራ ሪፖርተር ተሞክሮ አምራች አፈጻጸም ሰንበት
        መጠይቅ ጨረታ አስተዳዳሪ ትስስር ሬዲዮ ህልውና ደንበኛ ዕቃ ማስጠንቀቂያ ቅርንጫፍ ጎል ሽያጭ ልቦና አቀባበል ማሻሻያ ስያሜ ጽሕፈት
        አጋማሽ ምሕረት አቤቱታ ተከታይ ፖለቲከኛ መገለጫ መደብ ኤጀንሲ መግባባት ተቀማጭ ሰይጣን ተቀባይነት ሊቀመንበር መሥሪያ
        ቦርሳ ሳጥን ወረቀት እስክሪብቶ እርሳስ ሰሌዳ ፊደል ቁጥር ስዕል ፊልም ሙዚቃ ዘፈን ጭፈራ ጨዋታ በዓል ሠርግ ልደት ስጦታ ግብዣ""",
    "verb": """መጣ ሄደ ተኛ ተነሳ ተቀመጠ ሸጠ ተጫወተ መለሰ አስታወሰ ሰጠ ተቀበለ ቆየ ተወለደ አገባ ተራበ አገኘ ተገኘ ናፈቀ ተሳፈረ
        ተመለሰ አበላ አጠጣ ለበሰ አወለቀ ነቃ ቀሰቀሰ አወቀ አደረገ ተደረገ ተባለ ተከተለ መሰለ አስፈለገ ታወሰ ተመለከተ ተለየ ተጠበቀ
        አስታወቀ ታየ አሳየ ቻለ ተቻለ ተጀመረ ተከላከለ አቀረበ ተካሄደ ተሰጠ ተወሰነ አስረዳ ጠቆመ ተገለጸ ቆመ ተቀመጠ አለቀሰ አጠበ
        ታጠበ ተዘጋጀ አዘጋጀ ተሰማ አሰማ ተመረቀ ተጓዘ ደወለ ተጠራ አዳመጠ ተሳተፈ አሳተፈ ተረከበ አስረከበ ተፈጠረ ተጣላ ታረቀ ጸለየ
        አጣራ አዳነ አሳመነ አስቆመ በቀለ አነሰ ተወ ቀረ ከፋ ተባበረ አስቀደመ ተያያዘ ለያየ ተለያየ አስወገደ አሳለፈ አሸነፈ አሳደገ አስተማረ አወረደ አስቻለ ተግባባ
        አለፈ በለጠ ተሾመ አከበረ ተከበረ ዋኘ ሮጠ ዘለለ ወደቀ ተራመደ ጠፋ አጠፋ ተሻለ አሻሻለ ጨረሰ ተጠናቀቀ አጠናቀቀ ሞከረ ተሞከረ ጠበቀ ተጠቀመ""",
    "intj": "እሺ አቤት ወይኔ ኧረ",
}.items():
    for _word in _words.split():
        AMHARIC_WORDS.setdefault(_word, _kind)
# Forms whose word the rules would miss or get wrong (written as text has them): the demonstratives after a preposition
# (በዚህ, ከዚያ), other spellings of a word (ዉስጥ, ውጪ), the bound forms of a noun before another (ቤተ ክርስቲያን, ሕገ
# መንግሥት), irregular plurals (መጻሕፍት), and the forms of አለ "say; there is" that have none of its letters but l (ብሎ, ይላል,
# የለም, የሌለ), which AMHARIC_STEMS reads.
AMHARIC_FORMS = {
    "ዚህ": "ይህ", "ዚያ": "ያ", "ዚሁ": "ይህ", "ዛ": "ያ", "ዚች": "ይህች", "ዚያች": "ያች", "ዚችኛው": "ይህች", "እዛ": "እዚያ",
    "ዉስጥ": "ውስጥ", "ውጪ": "ውጭ", "ነዉ": "ነው", "ቤተ": "ቤት", "ሕገ": "ሕግ", "ህገ": "ሕግ", "ቃለ": "ቃል", "ክፍለ": "ክፍል",
    "መጻሕፍት": "መጽሐፍ", "መጻህፍት": "መጽሐፍ", "ታላላቅ": "ታላቅ", "ትልልቅ": "ትልቅ", "ትንንሽ": "ትንሽ", "ብዬ": "አለ",
    "ነኝ": "ነው", "ነህ": "ነው", "ነሽ": "ነው", "ናት": "ነው", "ነን": "ነው", "ናችሁ": "ነው", "ናቸው": "ነው", "ነዎት": "ነው",
    "ነበር": "ነበረ", "ጋ": "ጋር", "እንጅ": "እንጂ", "በኃላ": "በኋላ", "በሁዋላ": "በኋላ", "ማነው": "ማን", "ምንድን": "ምን",
    "ምንድነው": "ምን", "ያለምንም": "ምንም", "ግዜ": "ጊዜ", "መች": "መቼ", "መተው": "ተወ", "ሂድ": "ሄደ", "ሂጂ": "ሄደ",
    "ሂዱ": "ሄደ", "ቁም": "ቆመ", "ቁሙ": "ቆመ", "ና": "መጣ", "ነይ": "መጣ", "ኑ": "መጣ", "ይላል": "አለ", "ይላሉ": "አለ",
    "ትላለች": "አለ", "ብሎ": "አለ", "ብለው": "አለ", "ብላ": "አለ", "ብለህ": "አለ", "ብለሽ": "አለ", "ብለን": "አለ",
    "ብላችሁ": "አለ", "ብለዋል": "አለ", "ብሏል": "አለ", "ብላለች": "አለ", "ብያለሁ": "አለ", "ብለናል": "አለ", "እንበል": "አለ",
    "ልበል": "አለ", "የኔ": "እኔ", "አገራት": "አገር", "ቤተሰቦች": "ቤተሰብ", "ታላቁ": "ታላቅ", "የሴት": "ሴት", "አዩ": "አየ", "አዬ": "አየ", "አብረን": "አብሮ", "አብረው": "አብሮ", "አብረኝ": "አብሮ", "መቸም": "መቼ",
    "ወይንም": "ወይም", "ወይንስ": "ወይም", "ያክል": "ያህል", "ብሎም": "አለ", "ባይ": "አለ", "ባዮች": "አለ", "ያዩ": "አየ", "እርሶ": "እርስዎ", "እላለሁ": "አለ", "ትላለህ": "አለ", "ትያለሽ": "አለ", "እንላለን": "አለ", "ትላላችሁ": "አለ", "ሌላኛው": "ሌላ", "አብዛኞቹ": "አብዛኛው", "ፍቃድ": "ፈቃድ", "አንጻር": "አንፃር", "አማካይነት": "አማካኝነት",
}
# The stems of አለ ("say; there is, have") that the rules can't make from its letters: ይ-ል-ኣል, ሲ-ል, የሚ-ል, ብ-ሎ, በ-ል, ማ-ለት,
# የ-ለ-ም, የ-ሌለ.
AMHARIC_STEMS = {
    "al": {"አለ": 3}, "alA": {"አለ": 1}, "l": {"አለ": 2}, "bl": {"አለ": 1}, "bAl": {"አለ": 1}, "lAt": {"አለ": 4}, "yAl": {"አለ": 1}, "lel": {"አለ": 1},
    "lelA": {"አለ": 1}, "yAlA": {"አለ": 1},
}
# Words that are no words of an ordinary text: letters, abbreviations, bound forms that are words only with the next.
AMHARIC_EXCLUDED = {"ዘሪሁን", "ገብረ", "ጥሩነሽ", "አንዳርጋቸው", "ስመ", "አርሰናል", "አርሲ", "ማርሻል", "ጋና", "ካሳሁን", "ቻይና", "ግርማ", "ደሴ",
                    "ዳኒ", "ጥላሁን", "ንጥረ", "ለንደን", "ናንሲ", "ወያኔ", "እስመ", "ዐ", "ሥነ", "ስነ", "ድረ", "ፀረ", "ርዕሰ", "ኃይለ",
                    "ደብረ", "ሊቀ", "ቅድመ", "ግብረ", "ሀገረ", "ደቀ", "አብያተ", "ሊቃነ"}
# Pack entries that are other words' forms or the words of a rare verb whose stems take far commoner words' forms (ናደ:
# እንዳሉ, ላለ: ይላል): no words of the list, their forms read by the rules.
AMHARIC_UNLISTED = {"የተለያዩ", "የሥራ", "የውጭ", "የትግራይ", "የንግድ", "የፖለቲካ", "ማንኛው", "አገራት", "ቤተሰቦች", "ታላቁ", "የሴት", "የኔ",
                    "ናደ", "ጎ", "አር", "በዳ", "ላለ", "ላላ", "ና"}


# What may come before a stem: nothing or a preposition (a perfective, a gerund, an imperative: ሰበረ, የሰበረ, ሰብሮ, ስበር),
# a person's marker (an imperfective or jussive: ይሰብራል, የሚሰብር, እንዲሰብር), or the infinitive's መ (መስበር, ለመስበር).
FREE, PERSON, INFINITIVE = 1, 2, 4
ANY = FREE | PERSON | INFINITIVE


def _base_stems(letters):
    """
    The stems a verb's forms are made on, by the pattern of its perfective's letters (each a consonant and its vowel), with
    what may come before each: a triradical (ሰበረ: perfective ሰበር-, imperfective and gerund -ሰብር-, jussive and infinitive
    -ስበር-), one with a after its first letter (ማረከ: -ማርክ-), a biradical whose last root letter is weak (በላ: -በላ-, -ብላ-,
    በል-ቶ, መ-ብል-ኣት; ሰጠ: ይ-ሰጥ-ኣል, ሰጥ-ቶ, መ-ስጥ-ኧት), one with o, e or a between its two letters (ሞተ: ሙት; ሄደ: ሂድ; ሳመ:
    ይ-ስም-ኣል), and a quadriliteral (መሰከረ: -መሰክር-, መስክር-ኦ). Empty for a pattern none of these is.
    """
    cs = [c for c, _ in letters]
    vs = [v for _, v in letters]
    if len(letters) == 3 and vs in (["A", "A", "A"], ["a", "A", "A"]):
        c1, c2, c3 = cs
        v1 = vs[0]
        # The perfective's stem is also a passive's imperfective (ይሰበራል, ይናገራል, ይታወቃል), and a B verb's infinitive (መፈለግ).
        out = {c1 + v1 + c2 + "A" + c3 + "A": FREE, c1 + v1 + c2 + "A" + c3: ANY, c1 + v1 + c2 + c3: FREE | PERSON}
        if v1 == "A":
            out[c1 + c2 + "A" + c3] = ANY
        return out
    if len(letters) == 3 and vs == ["A", "a", "A"]:
        # A repeated or mutual verb (ለያየ, ደጋገመ, ተያያዘ): ይለያያል, ለያይቶ, መለያየት.
        c1, c2, c3 = cs
        return {c1 + "A" + c2 + "a" + c3 + "A": FREE, c1 + "A" + c2 + "a" + c3: ANY}
    if len(letters) == 2 and vs == ["A", "a"]:
        c1, c2 = cs
        return {c1 + "A" + c2 + "a": FREE | PERSON, c1 + "A" + c2: FREE | PERSON, c1 + c2 + "a": FREE | PERSON,
                c1 + c2: PERSON | INFINITIVE}
    if len(letters) == 2 and vs == ["A", "A"]:
        c1, c2 = cs
        return {c1 + "A" + c2 + "A": FREE, c1 + "A" + c2: FREE | PERSON, c1 + c2: ANY, c1 + c2 + "A": FREE | PERSON}
    if len(letters) == 2 and vs[1] == "A" and vs[0] in ("o", "e", "a"):
        c1, c2 = cs
        inner = {"o": "u", "e": "i", "a": ""}[vs[0]]
        return {c1 + vs[0] + c2 + "A": FREE, c1 + vs[0] + c2: ANY, c1 + inner + c2: FREE | PERSON}
    if len(letters) == 4 and vs[3] == "A" and all(v in ("A", "a") for v in vs[:3]):
        c1, c2, c3, c4 = cs
        v1, v2, v3 = vs[:3]
        return {c1 + v1 + c2 + v2 + c3 + v3 + c4 + "A": FREE, c1 + v1 + c2 + v2 + c3 + v3 + c4: FREE | INFINITIVE,
                c1 + v1 + c2 + v2 + c3 + c4: FREE | PERSON, c1 + v1 + c2 + c3 + c4: FREE | PERSON,
                c1 + v1 + c2 + c3 + v3 + c4: INFINITIVE | PERSON}
    return {}


def _add(out, stems, allowed):
    for stem, kinds in stems.items():
        stem = stem.lstrip("'")
        if len(skeleton(stem)) >= 2 and kinds & allowed:
            out[stem] = out.get(stem, 0) | (kinds & allowed)


def verb_stems(key):
    """
    The stems of the verb [key] (its perfective, or an infinitive the pack has instead) with what may come before each, as
    stem -> FREE | PERSON | INFINITIVE: those of its base (_base_stems), and for a ተ- verb those of its base without the ተ
    after a person's marker or መ (ይሰበራል, መሰበር) and with it otherwise (ተሰበረ, ተሰብሮ), for an አ- verb with its a (ያደርጋል,
    አድርጎ) and without it after ማ (ማድረግ), for an አስ- verb with as (ያስፈልጋል) and s after ማ (ማስፈለግ). A perfective whose
    pattern is none of them has its own stems (the perfective and its stem) only.
    """
    if key[-1] not in "AuiaeoW" and key[:2] in ("mA", "ma"):
        return _infinitive_stems(key)
    letters = _letters_of(key)
    out = {}
    bare = key.lstrip("'")
    _add(out, {bare: FREE, (bare[:-1] if bare[-1] in "Aa" else bare): FREE}, ANY)
    _add(out, _base_stems(letters), ANY)
    if key.startswith("tA") and len(letters) >= 3:
        base = _base_stems(letters[1:])
        # Its imperfective and infinitive are made on its base's perfective stem (ይሰበራል, ይቻላል, መሰበር).
        stem = key[2:-1] if key[-1] in "Aa" else key[2:]
        if base:
            _add(out, {stem: PERSON | INFINITIVE}, ANY)
        _add(out, {"tA" + s: k for s, k in base.items()}, FREE)
    if letters[0] in (("'", "a"), ("'", "A")) and len(letters) >= 3:
        if letters[1] == ("s", "") and len(letters) >= 4:
            base = _base_stems(letters[2:])
            _add(out, {"as" + s: k for s, k in base.items()}, FREE | PERSON)
            _add(out, {"as" + skeleton(s)[0] + s[len(skeleton(s)[0]):].lstrip("A"): k for s, k in base.items()}, FREE | PERSON)
            _add(out, {"s" + s: k for s, k in base.items()}, INFINITIVE)
        else:
            base = _base_stems(letters[1:])
            _add(out, {"a" + s: k for s, k in base.items()}, FREE | PERSON)
            # The jussive and gerund lose the base's first vowel (ያድርግ, አድርጎ, አምጥቶ).
            _add(out, {"a" + s[0] + s[1:].lstrip("A"): k for s, k in base.items()}, FREE | PERSON)
            _add(out, base, INFINITIVE)
    return out


def _letters_of(key):
    """[key]'s letters as (consonant, vowel), a labialised consonant written with its W (gWA is gW + A)."""
    out = []
    for c, v in _syllables(key):
        if v.startswith("W"):
            c, v = c + "W", v[1:]
        out.append((c, v))
    return out


def _infinitive_stems(key):
    """The stems of a verb the pack has as its infinitive only (መቁጠር, መብላት, ማድረግ), from the infinitive's pattern."""
    rest = key[2:]
    letters = _letters_of(rest)
    vs = [v for _, v in letters]
    cs = [c for c, _ in letters]
    out = {}
    _add(out, {rest: INFINITIVE}, ANY)
    if key.startswith("ma"):
        # A causative: ማድረግ is m + a + its stem (ያድርግ, አድርጎ, ያደርጋል).
        if len(cs) == 3 and vs == ["", "A", ""]:
            _add(out, {"a" + cs[0] + "A" + cs[1] + "A" + cs[2] + "A": FREE, "a" + cs[0] + "A" + cs[1] + "A" + cs[2]: FREE,
                       "a" + cs[0] + "A" + cs[1] + cs[2]: PERSON, "a" + cs[0] + cs[1] + cs[2]: FREE | PERSON}, ANY)
        else:
            _add(out, {"a" + rest: FREE | PERSON, "a" + skeleton(rest): FREE | PERSON}, ANY)
        return out
    if len(letters) == 3 and vs in (["", "A", ""], ["A", "A", ""]):
        base = [(cs[0], "A"), (cs[1], "A"), (cs[2], "A")]
    elif len(letters) == 3 and vs == ["", "a", ""] and cs[2] == "t":
        base = [(cs[0], "A"), (cs[1], "a")]
    elif len(letters) == 3 and vs == ["", "A", ""] and cs[2] == "t":
        base = [(cs[0], "A"), (cs[1], "A")]
    elif len(letters) == 2 and vs[0] in ("o", "e", "a") and vs[1] == "":
        base = [(cs[0], vs[0]), (cs[1], "A")]
    elif len(letters) == 4 and vs[3] == "":
        base = [(cs[0], "A"), (cs[1], "A"), (cs[2], "A"), (cs[3], "A")]
    else:
        base = None
    if base:
        _add(out, _base_stems(base), ANY)
    return out


UNPALATAL = {"c": "t", "j": "d", "x": "s", "Z": "z", "N": "n", "C": "T"}


class Lexicon:
    """The words of the am-en pack and AMHARIC_WORDS by their sounds, the pack's forms, and the verbs by their roots."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "am-en.sqlite"))
        self.kinds = defaultdict(set)
        self.spellings = defaultdict(set)
        for word, pos in db.execute("SELECT word_lc, pos FROM entries"):
            if pos in NOT_WORDS or not is_amharic(word):
                continue
            key = sounds(word)
            if len(key) < 2 or key in AMHARIC_EXCLUDED_KEYS or word in AMHARIC_UNLISTED:
                continue
            self.kinds[key].add(pos)
            self.spellings[key].add(word)
        for word, kind in AMHARIC_WORDS.items():
            key = sounds(word)
            self.kinds[key].add(kind)
            self.spellings[key].add(word)
        self.forms = defaultdict(set)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            if not is_amharic(form) or not is_amharic(lemma):
                continue
            key, head = sounds(form), sounds(lemma)
            if head in self.kinds and key != head and key not in self.kinds:
                self.forms[key].add(head)
        for form, lemma in AMHARIC_FORMS.items():
            self.forms[sounds(form)] = {sounds(w) for w in lemma.split(",")}
            # A pack entry that is another word's form (አብረው of አብሮ) is that word's.
            self.kinds.pop(sounds(form), None)
        # The verbs by their stems; an infinitive entry is its perfective's form when the pack has both (መቁጠር of ቆጠረ).
        self.stems = defaultdict(dict)
        verbs = [k for k, kinds in self.kinds.items() if "verb" in kinds]
        finite = {k for k in verbs if k[-1] in "AuiaeoW"}
        for key in sorted(verbs):
            for stem, kinds in verb_stems(key).items():
                self.stems[stem][key] = self.stems[stem].get(key, 0) | kinds
        for key in sorted(set(verbs) - finite):
            rest = key[2:]
            cores = [rest] + ([rest[:-2]] if rest.endswith(("at", "At")) else [])
            heads = {v for c in cores for v, k in self.stems.get(c, {}).items() if v in finite and k & INFINITIVE}
            # መ- is a plain verb's infinitive (መግባት: ገባ, not አገባ), ማ- a causative's (ማግባት: አገባ).
            causative = {v for v in heads if v.startswith(("'a", "a"))}
            plain = {v for v in heads - causative if not v.startswith("tA")} or heads - causative
            heads = causative if key.startswith("ma") and causative else plain if key.startswith("mA") and plain else heads
            if len(heads) == 1 and "verb" in self.kinds[key] and self.kinds[key] <= {"verb", "noun"}:
                self.forms[key] = heads
                del self.kinds[key]
                for stem in verb_stems(key):
                    self.stems[stem].pop(key, None)
        for stem, kinds in AMHARIC_STEMS.items():
            for verb in kinds:
                self.stems[stem][sounds(verb)] = self.stems[stem].get(sounds(verb), 0) | kinds[verb]

    def is_word(self, key):
        return key in self.kinds

    def headword(self, key, spelled):
        """How the list writes the word [key]: its spelling of the pack (or AMHARIC_WORDS) the texts use most."""
        spellings = sorted(self.spellings[key], key=lambda w: (-spelled.get(w, 0), w))
        return spellings[0]

    def _known(self, key):
        if key in self.kinds:
            return {key}
        return self.forms.get(key, set())

    @functools.lru_cache(maxsize=None)
    def nominal(self, key):
        """(cost, words): the words [key] is with its prepositions and noun endings taken off (የቤታችን: ቤት), the fewest sounds
        taken first."""
        best, found = None, set()
        for rest, front in nominal_fronts(key):
            for core, back in strip_layers(rest, _NOMINAL_LAYERS):
                if front + back == 0 or len(core) < 2:
                    continue
                words = self._known(core)
                if not words and core[-1] not in "AuiaeoW" and rest[len(core):].startswith("oc"):
                    # ሠራተኛ, ከተማ: an a-final noun's plural is made on its consonant (ሠራተኞች, ከተሞች).
                    words = {w for w in self._known(core + "a") if self.kinds.get(w, set()) & {"noun", "adj", "det", "pron", "num"}}
                if not words:
                    continue
                cost = front + back
                if best is None or cost < best:
                    best, found = cost, set(words)
                elif cost == best:
                    found |= words
        return best, frozenset(found)

    @functools.lru_cache(maxsize=None)
    def verbal(self, key):
        """(cost, verbs): the verbs [key] is a form of when its verb markers are taken off and a verb's stem is left
        (የሚሰብሩት: yAmi + sAbr + u + t), the fewest sounds taken first."""
        best, found = None, set()
        fronts = [(p, key[len(p):].lstrip("'"), PREFIX_KINDS[p]) for p in [""] + _VERB_PREFIXES.fronts(key)]
        # A preposition, ያ- or እያ- fused with a verb's a (ያለ, ካሉ, ባለፈው, እያለ, ያደረገው).
        fronts += [(p, "a" + key[len(p):], FREE) for p in _FUSED_VERB_PREFIXES.fronts(key)]
        # After የሚ, ሲ, ቢ, ሊ, እንዲ a verb's a comes with a y (የሚያደርግ, ሲያውቅ).
        fronts += [(p, r[1:], k) for p, r, k in fronts if p.endswith("i") and r.startswith("ya")]
        for prefix, rest, allowed in fronts:
            for core, back in strip_layers(rest, _VERB_LAYERS):
                verbs = {v for v, kinds in self.stems.get(core, {}).items() if kinds & allowed}
                if not verbs and core and core[-1] in UNPALATAL:
                    # Before -e and -i a last t, d, s, z, n, l, T is written palatal (ሰብሬ but ሄጄ, ሆኜ, ትቀመጪ).
                    back += 1
                    verbs = {v for v, kinds in self.stems.get(core[:-1] + UNPALATAL[core[-1]], {}).items() if kinds & allowed}
                # A stem with nothing taken off is an imperative only when it has three letters or ends in a vowel (ስበር, ስማ):
                # እስር is no ሥራ, ምት no መታ.
                if not prefix and not back and len(skeleton(core)) < 3 and core and core[-1] not in "AuiaeoW":
                    continue
                if not verbs:
                    continue
                cost = len(prefix) + back
                if best is None or cost < best:
                    best, found = cost, set(verbs)
                elif cost == best:
                    found |= verbs
        return best, frozenset(found)

    @functools.lru_cache(maxsize=None)
    def lemmas(self, key):
        """The words [key] is a form of: itself when it is a word, else the pack's or AMHARIC_FORMS' words, else those of the
        noun rules, else those of the verb rules; both for a word in -u whose verb reading takes no more sounds off than
        its noun reading (ወጡ: ወጣ "they went out" or ወጥ "the stew"), for the counts to share by how common each is."""
        if key in AMHARIC_EXCLUDED_KEYS:
            return frozenset()
        if key in self.forms and key not in self.kinds:
            return frozenset(self.forms[key])
        if key in self.kinds:
            return frozenset({key})
        nominal_cost, nominal = self.nominal(key)
        verbal_cost, verbal = self.verbal(key)
        if nominal and verbal and verbal_cost <= nominal_cost and key.endswith("u"):
            return nominal | verbal
        return nominal or verbal


AMHARIC_EXCLUDED_KEYS = {sounds(w) for w in AMHARIC_EXCLUDED}


# ----- The counts -----

LEIPZIG = [("wikipedia", "amh_wikipedia_2021_30K")]
# (kind, OPUS corpus, version): its monolingual Amharic text.
OPUS = [("web", "CCMatrix", "v1"), ("subtitles", "OpenSubtitles", "v2024"), ("subtitles", "TED2020", "v1"),
        ("news", "GlobalVoices", "v2018q4")]
CC100 = ("web", "https://data.statmt.org/cc-100/am.txt.xz", "cc100-am.txt.xz")
# Web text (news, blogs and forums, 50 million words) leads; Wikipedia brings its rarer words, and the subtitles and talks
# (25,000 words) the words of talk (አመሰግናለሁ, እባክህ, ደህና), weighed far above their size.
WEIGHTS = {"web": 0.78, "wikipedia": 0.1, "subtitles": 0.08, "news": 0.04}
LICENCES = {
    "cc100": "CC-100 (Common Crawl terms)",
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "CCMatrix": "terms at https://opus.nlpl.eu/CCMatrix",
    "OpenSubtitles": "terms at https://opus.nlpl.eu/OpenSubtitles/corpus/version/OpenSubtitles",
    "TED2020": "CC BY-NC-ND 4.0",
    "GlobalVoices": "CC BY 3.0",
}
# Lines with any of these are Tigrinya (ኣብ "in", እዩ "is", ናብ "to", ናይ "of"), which the web corpora mix in.
TIGRINYA = {"ኣብ", "እዩ", "ናብ", "እቲ", "ብዛዕባ", "እዚ", "ኣሎ", "ኣለዉ", "እያ", "እዮም", "ዝኾነ", "ምስ", "ከም", "ኢዩ", "ናይ", "ነቲ", "ኣይ",
            "ዝበለ"}
# A word must be counted this often (all its forms, all sources) to be listed: below it the corpora's typos.
LEAST_COUNT = 5
FORMS_LISTED = 400


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            yield line.rstrip("\n").partition("\t")[2]


def _lines(path):
    if path.endswith(".xz"):
        with lzma.open(path, "rt", encoding="utf-8") as f:
            yield from f
    else:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            yield from f


def _count(lines, counts):
    """Each line's words once a line, a line seen before or in Tigrinya not at all."""
    seen = set()
    for line in lines:
        line = line.strip()
        if not line or line in seen:
            continue
        seen.add(line)
        words = list(tokens(line))
        if any(w in TIGRINYA for w in words):
            continue
        for word in words:
            counts[word] += 1


def _counts(download):
    """Each kind of text's word counts, and the corpora counted."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    used = defaultdict(list)
    kind, url, name = CC100
    path = download(url, name)
    if path is not None:
        used["cc100"].append("am")
        _count(_lines(path), counts[kind])
    for kind, corpus in LEIPZIG:
        path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
        if path is not None:
            used["leipzig"].append(corpus)
            _count(_leipzig_sentences(path), counts[kind])
    for kind, corpus, version in OPUS:
        path = download(f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/am.txt.gz",
                        f"opus-{corpus.lower()}-{version}-am.txt.gz")
        if path is not None:
            used[corpus].append(version)
            _count(_lines(path), counts[kind])
    return counts, used


def label_of(used):
    parts = []
    if used.get("cc100"):
        parts.append(f"CC-100 am ({LICENCES['cc100']})")
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    for corpus in ("CCMatrix", "OpenSubtitles", "TED2020", "GlobalVoices"):
        if used.get(corpus):
            parts.append(f"OPUS {corpus} {', '.join(used[corpus])} am ({LICENCES[corpus]})")
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
    # A form seen once is no form worth reading for: the corpora's typos and run-together words.
    forms_read = sorted(f for f in share if raw[f] >= 2)
    heads = {form: lexicon.lemmas(sounds(form)) for form in forms_read}
    own = defaultdict(float)
    for form in forms_read:
        if len(heads[form]) == 1:
            own[next(iter(heads[form]))] += share[form]
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    spelled = defaultdict(lambda: defaultdict(int))
    for form in forms_read:
        words = heads[form]
        if not words:
            continue
        key = sounds(form)
        weights = {w: own.get(w, 0.0) for w in words}
        whole = sum(weights.values())
        for word in words:
            part = weights[word] / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            value = share[form] * part
            scores[word] += value
            totals[word] += raw[form]
            forms[word][form] = forms[word].get(form, 0.0) + value
            if key == word:
                spelled[word][form] += raw[form]
    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        headword = lexicon.headword(word, spelled[word])
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if headword not in listed:
            listed = [headword] + listed[:FORMS_LISTED - 1]
        lines.append(headword + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


_CHECKER = {}


def checker_lexicon():
    if "lexicon" not in _CHECKER:
        _CHECKER["lexicon"] = Lexicon()
    return _CHECKER["lexicon"]


def checker_word(token, ranks):
    """
    The list's word [token] is a form of by the rules the list was built with, the commonest when it may be several, or None:
    a form the list does not have (የሚሰብሩት: ሰበረ; ለልጆቻችን: ልጅ), or another spelling of a listed word (ሠላም: ሰላም).
    """
    lexicon = checker_lexicon()
    if "keys" not in _CHECKER:
        _CHECKER["keys"] = {}
    keys = _CHECKER["keys"]
    if id(ranks) not in keys:
        keys.clear()
        by_key = {}
        for word in ranks:
            by_key.setdefault(sounds(word), word)
        keys[id(ranks)] = by_key
    by_key = keys[id(ranks)]
    found = [(ranks[by_key[k]], by_key[k]) for k in lexicon.lemmas(sounds(token)) if k in by_key]
    return min(found)[1] if found else None


def checker_name(token, names):
    """Whether [token] is one of the course's [names] with a preposition or ending on it (የሳራ, ለአበበ, ሳራም, ከሳራና)."""
    if id(names) not in _CHECKER.setdefault("names", {}):
        _CHECKER["names"] = {id(names): {sounds(n) for n in names}}
    keys = _CHECKER["names"][id(names)]
    return any(core in keys for rest, _ in nominal_fronts(sounds(token)) for core, _ in strip_layers(rest, _NOMINAL_LAYERS))
