"""
Urdu for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

The reader (UrduParser) matches a word in Urdu letters without marks: ك as ک, ي and ى as ی, the Arabic ه, ە and ۀ as ہ,
ۂ as ہ, ۓ as ئے, without its vowel signs, tatweel and non-joiner. ھ (do-chashmi he, which writes aspiration: بھی, تھا), ہ, ے,
ں, ٹ, ڈ and ڑ are letters of their own and stay. So do these: the pack is written so, and the list counts words so.
Keep urdu_fold and UrduParser.fold the same.
"""
import re

ZWNJ = "‌"
# Long vowels and hamza seats written as a letter and a combining sign, and the letters with hamza Urdu writes otherwise.
_COMPOSED = (
    ("آ", "آ"), ("أ", "أ"), ("إ", "إ"), ("ؤ", "ؤ"),
    ("یٔ", "ئ"), ("ئ", "ئ"), ("ىٔ", "ئ"),
    ("ۓ", "ئے"), ("ۓ", "ئے"),
    ("ۂ", "ہ"), ("ۂ", "ہ"), ("ۀ", "ہ"),
)
_LETTERS = str.maketrans({"ك": "ک", "ي": "ی", "ى": "ی", "ه": "ہ", "ە": "ہ"})
# The vowel signs, shadda, sukun, hamza and noon ghunna marks, superscript alef, tatweel and the non-joiner.
_MARKS = re.compile("[ً-ٰٟـ‌]")
# The letters of an Urdu word as the reader reads it (UrduParser's word characters, folded).
LETTERS = re.compile("[ء-غف-يٹ-ۓ]+")


def urdu_fold(word):
    """A word as the reader matches it (UrduParser.fold)."""
    for spread, composed in _COMPOSED:
        word = word.replace(spread, composed)
    return _MARKS.sub("", word.translate(_LETTERS))


# The pronouns a verb's table names beside its forms, untagged or tagged with their person (میں of ہونا).
TABLE_PRONOUNS = {"میں", "تو", "وہ", "یہ", "ہم", "تم", "آپ"}
