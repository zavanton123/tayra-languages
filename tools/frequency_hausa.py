"""
Hausa for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

Hausa is written in Boko, the Latin alphabet with the hooked letters ɓ ɗ ƙ ƴ, which much text writes with an apostrophe:
'b, 'd, k' and 'y ('yan, k'asa, 'dan). The reader (HausaParser) matches a word with the hooked letters whichever way it is
written; an apostrophe inside a word is its glottal stop (sa'a, jama'a) and stays. Keep hausa_fold and HausaParser.fold the
same. Ajami, Hausa in Arabic letters, is no part of the pack or the list.
"""
import re
import unicodedata

APOSTROPHES = str.maketrans("’‘ʼ´`", "'''''")
_K_HOOK = re.compile(r"([kK])'(?=[^\W\d_])")
_HOOK_FIRST = re.compile(r"'([bBdDyY])")
_HOOKED = {"b": "ɓ", "B": "Ɓ", "d": "ɗ", "D": "Ɗ", "y": "ƴ", "Y": "Ƴ"}
# A Hausa word as the reader cuts it: letters with apostrophes inside, and one in front where it writes a hooked letter.
WORD = re.compile(r"(?:'(?=[bdyBDY]))?[^\W\d_]+(?:'[^\W\d_]+)*")
# Hausa in Arabic letters (Ajami), with the letters it adds to them.
AJAMI = re.compile("[؀-ۿݐ-ݿࢠ-ࣿﭐ-﷿ﹰ-﻿]")


def hausa_fold(word):
    """A word with its hooked letters for the apostrophe spellings (k'asa as ƙasa, 'yan as ƴan), as HausaParser.fold has it."""
    word = word.translate(APOSTROPHES)
    if "'" not in word:
        return word
    word = _K_HOOK.sub(lambda m: "ƙ" if m.group(1) == "k" else "Ƙ", word)
    return _HOOK_FIRST.sub(lambda m: _HOOKED[m.group(1)], word)


def unmarked(word):
    """A word without the tone and length marks the dictionary writes (gidā̀jē, kar̃àntā), as text writes it."""
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", word) if not unicodedata.combining(c)))


def hausa_clean(word):
    """A dictionary spelling as text writes it and the reader matches it: unmarked, with its hooked letters."""
    return hausa_fold(unmarked(word.strip()))
