"""
Reads JMdict (the Electronic Dictionary Research and Development Group's Japanese dictionary,
CC BY-SA 4.0, https://www.edrdg.org/edrdg/licence.html) into the entries of the app's packs, for
tools/build_yomitan_dictionary.py to add to its Japanese packs the words their Wiktionary lacks.

JMdict has glosses in English and, for many of its entries, in German, Russian, Spanish, French and
other languages. Each entry becomes one pack entry per headword and reading, as the converted wty
packs have them: a kanji spelling with each reading allowed for it (re_restr), or the reading alone
for a word written in kana. Other kanji spellings become forms of the first one. Senses keep their
glosses in the target language, their usage labels (archaic, colloquial, honorific...) and fields,
and an entry with JMdict's priority marks (news1, ichi1, spec1, gai1) counts as common.

The entity names JMdict's XML uses for parts of speech and labels are read as they are written
(&v5r;), not expanded, so the file is parsed line by line rather than with an XML parser.
"""
import gzip
import os
import re
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PATH = os.path.join(ROOT, "dictionaries", "japanese", "JMdict.gz")
URL = "http://ftp.edrdg.org/pub/Nihongo/JMdict.gz"

# JMdict's gloss languages, by the app's codes.
LANGUAGES = {"en": "eng", "de": "ger", "ru": "rus", "es": "spa", "fr": "fre", "nl": "dut", "hu": "hun", "sv": "swe", "sl": "slv"}

# Spellings and readings JMdict keeps only to be found (search-only), or marks as irregular,
# outdated or rarely used: they are not entries of their own (すっごーい for 凄い).
SKIPPED_KANJI = {"sK", "iK", "oK", "rK"}
SKIPPED_READINGS = {"sk", "ik", "ok", "rk"}

# Priority marks that make a word common, as JMdict's own "common" flag counts them.
COMMON = {"news1", "ichi1", "spec1", "spec2", "gai1"}

# JMdict's parts of speech as the app's packs name them; the first that maps names the entry.
def part_of_speech(code):
    if code.startswith("v") and code not in ("vulg", "vidg"):
        return "verb" if code not in ("vi", "vt") else None
    return {
        "n": "noun", "n-adv": "noun", "n-t": "noun", "n-pref": "prefix", "n-suf": "suffix", "n-pr": "name",
        "adj-i": "adj", "adj-ix": "adj", "adj-na": "adj", "adj-no": "adj", "adj-pn": "adnominal", "adj-t": "adj",
        "adj-f": "adj", "adj-ku": "adj", "adj-shiku": "adj", "adj-nari": "adj", "adj-kari": "adj",
        "adv": "adv", "adv-to": "adv", "aux": "verb", "aux-v": "verb", "aux-adj": "adj", "conj": "conj",
        "cop": "verb", "ctr": "counter", "exp": "phrase", "int": "intj", "num": "num", "pn": "pron",
        "pref": "prefix", "prt": "particle", "suf": "suffix", "unc": None,
    }.get(code)

# The labels kept on a sense, as Yomitan's JMdict dictionaries word them.
LABELS = {
    "abbr": "abbreviation", "arch": "archaic", "col": "colloquial", "derog": "derogatory", "fam": "familiar",
    "fem": "female term", "hon": "honorific", "hum": "humble", "id": "idiomatic", "male": "male term",
    "obs": "obsolete", "on-mim": "onomatopoeia", "poet": "poetical", "pol": "polite", "rare": "rare",
    "sens": "sensitive", "sl": "slang", "uk": "usually kana", "vulg": "vulgar", "yoji": "yojijukugo",
    "joc": "jocular", "form": "formal", "dated": "dated", "hist": "historical", "litf": "literary",
    "proverb": "proverb", "work": "work", "char": "character", "net-sl": "internet slang", "chn": "children's language",
}
FIELDS = {"med": "medicine", "law": "law", "math": "mathematics", "comp": "computing", "ling": "linguistics", "biol": "biology",
          "bot": "botany", "zool": "zoology", "chem": "chemistry", "food": "food", "sports": "sports", "Buddh": "Buddhism",
          "physics": "physics", "econ": "economics", "music": "music", "mil": "military", "geol": "geology"}


def download():
    if not os.path.exists(PATH):
        os.makedirs(os.path.dirname(PATH), exist_ok=True)
        subprocess.run(["curl", "-fsSL", "-o", PATH, URL], check=True)
    return PATH


def entities(text):
    return re.findall(r"&([^;]+);", text)


def text_of(line):
    return re.sub(r"<[^>]+>", "", line).strip()


def read(target):
    """
    The entries of JMdict with senses in [target]: dicts of word, pos, reading, senses, common,
    and the forms (other kanji spelling, the first spelling).
    """
    language = LANGUAGES[target]
    entries, forms = [], set()
    with gzip.open(download(), "rt", encoding="utf-8") as f:
        entry = None
        for raw in f:
            line = raw.strip()
            if line == "<entry>":
                entry = {"k": [], "r": [], "senses": [], "pos": None}
                k = r = s = None
            elif line == "</entry>":
                entries.extend(_rows(entry))
                # Other spellings, and the readings left out as entries, find the first spelling.
                main = next((k["text"] for k in entry["k"] if k["text"] and not k.get("skipped")), None)
                if main:
                    forms.update((k["text"], main) for k in entry["k"] if k["text"] and k["text"] != main)
                    forms.update((r["text"], main) for r in entry["r"] if r.get("skipped") and r["text"])
                entry = None
            elif entry is None:
                continue
            elif line == "<k_ele>":
                k = {"text": None, "common": False}
                entry["k"].append(k)
            elif line.startswith("<keb>"):
                k["text"] = text_of(line)
            elif line.startswith("<ke_inf>") and k is not None and set(entities(line)) & SKIPPED_KANJI:
                k["skipped"] = True
            elif line.startswith("<ke_pri>") and text_of(line) in COMMON and k is not None:
                k["common"] = True
            elif line == "<r_ele>":
                k = None
                r = {"text": None, "common": False, "restr": [], "nokanji": False}
                entry["r"].append(r)
            elif line.startswith("<reb>"):
                r["text"] = text_of(line)
            elif line.startswith("<re_inf>") and set(entities(line)) & SKIPPED_READINGS:
                r["skipped"] = True
            elif line.startswith("<re_nokanji"):
                r["nokanji"] = True
            elif line.startswith("<re_restr>"):
                r["restr"].append(text_of(line))
            elif line.startswith("<re_pri>") and text_of(line) in COMMON:
                r["common"] = True
            elif line == "<sense>":
                s = {"glosses": [], "pos": [], "labels": [], "stagk": [], "stagr": []}
                entry["senses"].append(s)
            elif line.startswith("<pos>"):
                s["pos"] += [p for p in (part_of_speech(c) for c in entities(line)) if p]
            elif line.startswith("<misc>"):
                s["labels"] += [LABELS[c] for c in entities(line) if c in LABELS]
            elif line.startswith("<field>"):
                s["labels"] += [FIELDS[c] for c in entities(line) if c in FIELDS]
            elif line.startswith("<stagk>"):
                s["stagk"].append(text_of(line))
            elif line.startswith("<stagr>"):
                s["stagr"].append(text_of(line))
            elif line.startswith("<gloss"):
                lang = re.search(r'xml:lang="(\w+)"', line)
                if (lang.group(1) if lang else "eng") == language:
                    s["glosses"].append(text_of(line))
    return entries, forms


def _rows(entry):
    # A sense's part of speech carries on to the senses after it until another is given; JMdict
    # gives them on its English senses only, so a language's senses take the English ones'.
    pos = None
    for sense in entry["senses"]:
        pos = sense["pos"] or pos
        sense["pos"] = pos or []
    kanji = [k for k in entry["k"] if k["text"] and not k.get("skipped")]
    rows = []
    pairs = []
    for r in entry["r"]:
        if r.get("skipped"):
            continue
        allowed = [] if r["nokanji"] else [k for k in kanji if not r["restr"] or k["text"] in r["restr"]]
        if allowed:
            pairs.append((allowed[0], r))
        if not kanji or r["nokanji"]:
            pairs.append((None, r))
    for k, r in pairs:
        word = k["text"] if k else r["text"]
        senses = [s for s in entry["senses"] if s["glosses"]
                  and (not s["stagk"] or (k and k["text"] in s["stagk"]))
                  and (not s["stagr"] or r["text"] in s["stagr"])]
        if not senses:
            continue
        by_pos = {}
        for s in senses:
            by_pos.setdefault((s["pos"] or ["unknown"])[0], []).append(s)
        for pos, group in by_pos.items():
            rows.append({
                "word": word,
                "pos": pos,
                "reading": r["text"] if k else None,
                "senses": [{"glosses": ["; ".join(s["glosses"])]} | ({"tags": s["labels"]} if s["labels"] else {}) for s in group],
                "common": (k["common"] if k else False) or r["common"],
            })
    return rows
