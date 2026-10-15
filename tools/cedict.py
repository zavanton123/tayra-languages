"""
Reads CC-CEDICT (MDBG, CC BY-SA 4.0, https://cc-cedict.org/wiki/), the Chinese-English dictionary
Zhongwen and many readers use, into the entries of the app's packs, for
tools/build_yomitan_dictionary.py to add to the Chinese packs the words their Wiktionary lacks.

A line is "Traditional Simplified [pin1 yin1] /gloss/gloss/". Each becomes an entry under its
traditional spelling, as the wty packs file Chinese words, with its pinyin in tone marks as the
reading; the simplified spelling is a form of it. Each gloss is a sense; "CL:個|个[ge4]" glosses
name the word's measure words, kept as a tag of its first sense.
"""
import gzip
import os
import re
import subprocess
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PATH = os.path.join(ROOT, "dictionaries", "chinese", "cedict_1_0_ts_utf-8_mdbg.txt.gz")
URL = "https://www.mdbg.net/chinese/export/cedict/cedict_1_0_ts_utf-8_mdbg.txt.gz"

LINE = re.compile(r"^(\S+) (\S+) \[([^\]]*)\] /(.*)/\s*$")
MARKS = {"1": "̄", "2": "́", "3": "̌", "4": "̀"}


def download():
    if not os.path.exists(PATH):
        os.makedirs(os.path.dirname(PATH), exist_ok=True)
        subprocess.run(["curl", "-fsSL", "-o", PATH, URL], check=True)
    return PATH


def syllable(numbered):
    """One pinyin syllable from tone numbers to tone marks ("lu:4" to "lǜ"), by the usual rule: a or e, else the o of ou, else the last vowel."""
    if not numbered or not numbered[-1].isdigit():
        return numbered.replace("u:", "ü").replace("U:", "Ü")
    tone, text = numbered[-1], numbered[:-1].replace("u:", "ü").replace("U:", "Ü").replace("v", "ü")
    mark = MARKS.get(tone)
    if mark is None:
        return text
    lower = text.lower()
    if "a" in lower:
        at = lower.index("a")
    elif "e" in lower:
        at = lower.index("e")
    elif "ou" in lower:
        at = lower.index("o")
    else:
        vowels = [i for i, c in enumerate(lower) if c in "aeiouü"]
        if not vowels:
            return text
        at = vowels[-1]
    return unicodedata.normalize("NFC", text[: at + 1] + mark + text[at + 1:])


def pinyin(numbered):
    return " ".join(syllable(s) for s in numbered.split())


def read():
    """The entries of CC-CEDICT: dicts of word, pos, reading, senses; and the forms (simplified, traditional)."""
    rows, forms = [], set()
    with gzip.open(download(), "rt", encoding="utf-8") as f:
        for line in f:
            if line.startswith("#"):
                continue
            match = LINE.match(line)
            if not match:
                continue
            traditional, simplified, reading, glosses = match.groups()
            measure = []
            senses = []
            for gloss in glosses.split("/"):
                gloss = gloss.strip()
                if not gloss:
                    continue
                if gloss.startswith("CL:"):
                    # 個|个[ge4],隻|只[zhi1] → 個 (gè), 隻 (zhī)
                    measure += [f"{m.group(1)} ({pinyin(m.group(2))})" for m in re.finditer(r"([^,|\[]+)(?:\|[^,\[]+)?\[([^\]]+)\]", gloss[3:])]
                    continue
                # A word cited with its pinyin ("see 道行[dao4 heng2]") gets it in tone marks.
                senses.append({"glosses": [re.sub(r"\[([a-zA-Z: 0-9]+)\]", lambda m: f" ({pinyin(m.group(1))})", gloss)]})
            if not senses:
                continue
            if measure:
                senses[0]["tags"] = ["measure word: " + ", ".join(measure)]
            rows.append({"word": traditional, "pos": "unknown", "reading": pinyin(reading), "senses": senses})
            if simplified != traditional:
                forms.add((simplified, traditional))
    return rows, forms
