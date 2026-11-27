"""
Punjabi for tools/build_frequency_list.py (Gurmukhi).

The pa-en pack is small, and texts spell many of its words otherwise than it does: without the nukta (ਫਿਲਮ for ਫ਼ਿਲਮ,
ਖਾਸ for ਖ਼ਾਸ), without the addak (ਉਥੇ for ਉੱਥੇ, ਵਿਚੋਂ for ਵਿੱਚੋਂ, ਤਕ for ਤੱਕ), with tippi for bindi or the other way
round; and they write an oblique pronoun with its postposition joined on (ਉਸਨੇ, ਇਸਦਾ, ਜਿਸਨੂੰ). So a form the pack
lacks is (relink):

- the pronoun's, when it is an oblique pronoun with a postposition joined on;
- else the word of the pack's word or form spelled as it is but for nukta (ਲ਼ as ਲ: ਕਾਲੇ of ਕਾਲ਼ਾ), addak, tippi and bindi,
  a subjoined ਹ (ਥੋੜੀ for ਥੋੜ੍ਹੀ), and in a longer word a sihari before the last letter or ਣ for a last ਨ (ਸ਼ਾਮਿਲ for
  ਸ਼ਾਮਲ), the commonest (loose); or, failing that, but for a last bindi (ਨਹੀ for ਨਹੀਂ).

Most of the pack's words have no forms table, and the tables spell a -ਨਾ verb's infinitive with ਣ (ਪੜ੍ਹਣ), where texts
write ਨ as well (ਪੜ੍ਹਨ): so each word's regular forms are made by its kind, gender and ending (regular_forms), and filed
under it where the pack has no such word or form, and so are the verbs' irregular forms (IRREGULAR).

And the pack lacks thousands of everyday words (ਜਿੱਥੇ, ਹੋਟਲ, ਹਿੱਸਾ, ਚਲਾਉਣਾ): they are found in the corpus itself, as
the Kannada list finds them (induce), from the forms no word of the pack explains; names and English among the commoner
ones are left out by hand (NOT_WORDS), and WORDS are added by hand.

And the pack has many words twice, spelled with and without the nukta or addak (ਜ਼ਮੀਨ and ਜਮੀਨ, ਖ਼ੁਸ਼ੀ and ਖੁਸ਼ੀ, ਪੱਗ
and ਪਗ): two words spelled so that share a word of their glosses are one, under the spelling texts use most (spellings).
Two that share none stay apart (ਸਾਲ "year", ਸ਼ਾਲ "shawl"; ਪਤਾ "address", ਪੱਤਾ "leaf").

Once counted (fold), a form counts only its uses as a word, spread over the corpus's pages: not what one article repeats,
nor its uses as a piece of a name or of a word a space split (read_usage); a noun mostly so used is a name and left out.
A verb's forms that tell no verb count for it only beside its telling ones (_unpieced).
"""
import heapq
import io
import json
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import Counter

NUKTA = "਼"
ADDAK = "ੱ"
TIPPI = "ੰ"
BINDI = "ਂ"
SUBJOINED_HA = "੍ਹ"
LONGER = 5
VOWEL_SIGNS = "ਾਿੀੁੂੇੈੋੌ"
VOWELS = "ਅਆਇਈਉਊਏਐਓਔ"
# A verb's stem after which the infinitive is -ਨਾ (ਕਰਨਾ, ਪੜ੍ਹਨਾ, ਸੁਣਨਾ), which texts also write -ਣਾ; else it is -ਣਾ only.
N_STEMS = ("ਰ", "ੜ", "ਣ", "ਨ", "ਲ਼", "ੜ੍ਹ", "ਰ੍ਹ")
NOMINAL_KINDS = {"noun", "adj", "det", "num"}
CONSONANT = re.compile("[\u0a15-\u0a39\u0a59-\u0a5e]")


def folded(word):
    """A Gurmukhi spelling without nukta and addak, with bindi for tippi."""
    return word.replace(NUKTA, "").replace(ADDAK, "").replace(TIPPI, BINDI)


def marks(word):
    """How many nuktas and addaks [word] is written with."""
    return word.count(NUKTA) + word.count(ADDAK)


def loose(word):
    """
    [folded], without a subjoined ਹ (ਥੋੜ੍ਹੀ as ਥੋੜੀ, ਪੜ੍ਹਨ as ਪੜਨ), and in a longer word without a sihari before the last
    letter and with ਨ for a last ਣ, as texts write such words either way (ਸ਼ਾਮਿਲ and ਸ਼ਾਮਲ, ਪ੍ਰਕਾਸ਼ਿਤ and ਪ੍ਰਕਾਸ਼ਤ,
    ਮਹੱਤਵਪੂਰਣ and ਮਹੱਤਵਪੂਰਨ).
    """
    word = folded(word).replace(SUBJOINED_HA, "")
    if len(word) >= LONGER:
        if word[-2] == "ਿ" and word[-1] not in VOWEL_SIGNS + BINDI:
            word = word[:-2] + word[-1]
        if word[-1] == "ਣ":
            word = word[:-1] + "ਨ"
    return word




# The oblique pronouns texts write their postpositions onto, and their words.
OBLIQUE_PRONOUNS = {
    "ਉਸ": "ਉਹ", "ਇਸ": "ਇਹ", "ਉਹ": "ਉਹ", "ਇਹ": "ਇਹ", "ਜਿਸ": "ਜੋ", "ਕਿਸ": "ਕੌਣ",
    "ਉਨ੍ਹਾਂ": "ਉਹ", "ਉਹਨਾਂ": "ਉਹ", "ਇਨ੍ਹਾਂ": "ਇਹ", "ਇਹਨਾਂ": "ਇਹ", "ਜਿਨ੍ਹਾਂ": "ਜੋ", "ਜਿਹਨਾਂ": "ਜੋ", "ਕਿਨ੍ਹਾਂ": "ਕੌਣ",
}
JOINED_POSTPOSITIONS = ("ਨੇ", "ਨੂੰ", "ਦਾ", "ਦੀ", "ਦੇ", "ਦੀਆਂ", "ਤੋਂ", "ਵਿੱਚ", "ਵਿਚ", "ਨਾਲ", "ਲਈ", "ਕੋਲ", "ਤੇ", "ਚ")

OVERRIDES = {
    # The plural obliques texts write with ਨ੍ਹ, which the pack has under the spelling with ਹ.
    "ਉਨ੍ਹਾਂ": ["ਉਹ"], "ਇਨ੍ਹਾਂ": ["ਇਹ"], "ਜਿਨ੍ਹਾਂ": ["ਜੋ"], "ਜਿਹਨਾਂ": ["ਜੋ"], "ਕਿਨ੍ਹਾਂ": ["ਕੌਣ"],
    "ਕਿਸੇ": ["ਕੋਈ"],
    # ਮਾਂ "mother", not the dialect dative of ਮੈਂ; ਧੀ "daughter".
    "ਮਾਂ": ["ਮਾਂ"], "ਮਾਵਾਂ": ["ਮਾਂ"], "ਧੀ": ["ਧੀ"], "ਧੀਆਂ": ["ਧੀ"],
    # The possessives, which the pack gives as forms of their pronouns.
    **{f: ["ਮੈਂ"] for f in ("ਮੇਰਾ", "ਮੇਰੀ", "ਮੇਰੇ", "ਮੇਰੀਆਂ")},
    **{f: ["ਤੂੰ"] for f in ("ਤੇਰਾ", "ਤੇਰੀ", "ਤੇਰੇ", "ਤੇਰੀਆਂ")},
    **{f: ["ਅਸੀਂ"] for f in ("ਸਾਡਾ", "ਸਾਡੀ", "ਸਾਡੇ", "ਸਾਡੀਆਂ")},
    **{f: ["ਤੁਸੀਂ"] for f in ("ਤੁਹਾਡਾ", "ਤੁਹਾਡੀ", "ਤੁਹਾਡੇ", "ਤੁਹਾਡੀਆਂ")},
    # The present participle of ਦੇਣਾ as texts write it (the pack has ਦੇਂਦਾ).
    **{f: ["ਦੇਣਾ"] for f in ("ਦਿੰਦਾ", "ਦਿੰਦੇ", "ਦਿੰਦੀ", "ਦਿੰਦੀਆਂ")},
    # The stem, not ਵਣ "forest" spelled with ਬ.
    "ਬਣ": ["ਬਣਨਾ"],
    # The oblique of ਸਮਾਂ "time" (the pack files it under a spelling ਸਮੈ it has no entry for).
    "ਸਮੇਂ": ["ਸਮਾਂ"],
    # ਵਿੱਚ and ਵਿੱਚੋਂ cut short ('ਚ, 'ਚੋਂ), which the reader reads without the apostrophe.
    "ਚ": ["ਵਿੱਚ"], "ਚੋਂ": ["ਵਿੱਚੋਂ"],
    # ਵਾਲਾ "the one who, -er" (the pack's ਵਾਲ਼ਾ) is no form of ਵਾਲ "hair" (the pack's vocative).
    **{f: ["ਵਾਲ਼ਾ"] for f in ("ਵਾਲਾ", "ਵਾਲੇ", "ਵਾਲੀ", "ਵਾਲੀਆਂ", "ਵਾਲਿਆਂ")},
    # Forms of ਮੰਨਣਾ and ਰੱਖਣਾ far more than ਮੰਨੀ "a thick cake" and ਰੱਖ "protection".
    "ਮੰਨੀ": ["ਮੰਨਣਾ"], "ਰੱਖ": ["ਰੱਖਣਾ"], "ਰਖ": ["ਰੱਖਣਾ"],
    # Words the pack lacks (WORDS), not the others it spells alike: ਕੇਸ "case" (ਕੇਸ਼ "hair"), ਪਦ "verse, post" (ਪੱਦ "fart"),
    # ਗੋਲੀ "bullet, pill" (ਗੋਲਾ "slave"), ਕੱਪ "cup" (ਕੱਪਣਾ "to cut").
    "ਕੇਸ": ["ਕੇਸ"], "ਕੇਸਾਂ": ["ਕੇਸ"], "ਪਦ": ["ਪਦ"], "ਗੋਲੀ": ["ਗੋਲੀ"], "ਗੋਲੀਆਂ": ["ਗੋਲੀ"], "ਕੱਪ": ["ਕੱਪ"], "ਕੱਪਾਂ": ["ਕੱਪ"],
    # ਕਰਨ is ਕਰਨਾ's oblique infinitive far more than the noun "doing".
    "ਕਰਨ": ["ਕਰਨਾ"],
    # The feminine of ਦਾ, which the pack has as an entry of its own; ਪੂਰੀ is ਪੂਰਾ's far more than the bread.
    "ਦੀ": ["ਦਾ"], "ਪੂਰੀ": ["ਪੂਰਾ"],
    # The past of ਰੱਖਣਾ far more than ਰੱਖਿਆ "defence".
    "ਰੱਖਿਆ": ["ਰੱਖਣਾ"], "ਰਖਿਆ": ["ਰੱਖਣਾ"],
    # ਬਾਰਾਂ "twelve", not the plural of ਬਾਰ (ਵਾਰ "time").
    "ਬਾਰਾਂ": ["ਬਾਰਾਂ"],
    # The oblique infinitive of ਜਾਣਾ and ਜਾਣਨਾ far more than ਜਾਣ "knowledge"; ਜਾਣੇ, ਜਾਣੋ are ਜਾਣਨਾ's.
    "ਜਾਣ": ["ਜਾਣਾ", "ਜਾਣਨਾ"], "ਜਾਣੇ": ["ਜਾਣਨਾ"], "ਜਾਣੋ": ["ਜਾਣਨਾ"],
    # ਦੁਨੀਆ "world" and ਨੂੰ as texts write them most.
    # ਜਨ "people", not ਜਨਵਰੀ cut short; ਨਾਲੋਂ "than", ਨਾਲ's, not ਨਾਲ੍ਹ "horseshoe"'s; ਸੰਨ "year (of an era)", not ਸੰਨ੍ਹ
    # "breach"; ਜਮ੍ਹਾਂ "deposited", not ਜਮ's; ਪੜਾਵਾਂ the plural of ਪੜਾਅ "stage", not of ਪੜ੍ਹਾਉਣਾ.
    "ਜਨ": ["ਜਨ"], "ਨਾਲੋਂ": ["ਨਾਲ"], "ਨਾਲੋ": ["ਨਾਲ"], "ਨਾਲ਼ੋਂ": ["ਨਾਲ"], "ਸੰਨ": ["ਸੰਨ"], "ਜਮ੍ਹਾਂ": ["ਜਮ੍ਹਾਂ"], "ਪੜਾਵਾਂ": ["ਪੜਾਅ"],
    "ਦੁਨੀਆਂ": ["ਦੁਨੀਆ"], "ਨੁੰ": ["ਨੂੰ"], "ਅਪਰੈਲ": ["ਅਪ੍ਰੈਲ"], "ਕਾਰਣ": ["ਕਾਰਨ"], "ਕਾਰਣਾਂ": ["ਕਾਰਨ"],
    # ਆਪਣਾ and ਇਨ੍ਹਾਂ spelled otherwise.
    **{f: ["ਆਪ"] for f in ("ਅਪਣੀ", "ਅਪਣੇ", "ਅਪਣੀਆਂ")}, "ਇੰਨ੍ਹਾਂ": ["ਇਹ"],
}

# Names and surnames the pack has as words (ਸਿੰਘ "lion" is the Sikh surname in text), and letters.
EXCLUDE = {"ਸਿੰਘ", "ਕੌਰ", "ਕੁਮਾਰ", "ਮੋਹਨ", "ਸੁਰਜੀਤ", "ਗਿੱਲ", "ਕਪੂਰ", "ਬੀ", "ਆਲ", "ਕਾ"}


GLOSS_WORD = re.compile(r"[a-z]{3,}")
GLOSS_STOP = {"the", "and", "for", "with", "from", "that", "this", "form", "used", "one", "something", "someone", "person",
              "thing", "act", "state", "being", "having", "which", "who", "make", "made", "into", "out", "not", "any", "all",
              "punjabi", "spelling", "alternative", "standard", "india", "pakistan", "also", "very", "etc", "kind", "type",
              "masculine", "feminine", "plural", "singular", "oblique", "direct"}


# A gloss that only names the spelling with the nukta ("nuqtaless form of ਜ਼ਮੀਨ (zamīn)").
NUQTALESS = re.compile(r"^nuqtaless form of (\S+)")


def gloss_words(dictionaries):
    """Each word of the pa-en pack: the content words of its glosses (but those that call it a nuqtaless form), and the
    words those call it the nuqtaless form of."""
    words, nuqtaless = {}, {}
    with sqlite3.connect(os.path.join(dictionaries, "pa-en.sqlite")) as db:
        for word, senses in db.execute("SELECT word_lc, senses FROM entries"):
            glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])]
            own = " ".join(g for g in glosses if not NUQTALESS.match(g)).lower()
            words.setdefault(word, set()).update(set(GLOSS_WORD.findall(own)) - GLOSS_STOP)
            nuqtaless.setdefault(word, set()).update(m.group(1) for m in map(NUQTALESS.match, glosses) if m)
    return words, nuqtaless


def spellings(dictionaries, is_word, count):
    """
    The words of the pack that are another's spelling (see the module's note): word -> the spelling it is filed under. A
    word glossed as another's nuqtaless form is filed under that word.
    """
    glossed, nuqtaless = gloss_words(dictionaries)
    moved = {}
    for w, targets in nuqtaless.items():
        targets = [t for t in targets if t != w and is_word(t) and not nuqtaless.get(t)]
        # Only a word that is nothing else (ਜੰਗ is "war" as well as the nuqtaless ਜ਼ੰਗ "rust").
        if targets and is_word(w) and not glossed[w]:
            moved[w] = max(targets, key=lambda t: (count(t), t))
    groups = {}
    for w in glossed:
        if is_word(w) and w not in moved:
            groups.setdefault(folded(w), []).append(w)
    for group in groups.values():
        group = sorted(group, key=lambda w: (-count(w), w))
        for i, w in enumerate(group[1:], 1):
            head = next((h for h in group[:i] if h not in moved and glossed[h] & glossed[w]), None)
            if head:
                moved[w] = head
    return moved


def pack_genders(dictionaries):
    """Each noun of the pa-en pack: "m", "f" or "" as its senses tag it."""
    genders = {}
    with sqlite3.connect(os.path.join(dictionaries, "pa-en.sqlite")) as db:
        for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos = 'noun'"):
            tags = {t for sense in json.loads(senses) for t in sense.get("tags", [])}
            genders[word] = genders.get(word, "") + ("m" if "masculine" in tags else "") + ("f" if "feminine" in tags else "")
    return genders


def nominal_forms(word, kinds, gender):
    """A noun's or adjective's regular forms: of one in -ਾ its oblique and plural (ਮਾਮਲੇ, ਮਾਮਲਿਆਂ), feminine (ਕਾਲੀ,
    ਕਾਲੀਆਂ) or plural in -ਵਾਂ (ਭਾਸ਼ਾਵਾਂ) by its kind and gender; of one in another vowel its plural in -ਆਂ (ਵਿਦਿਆਰਥੀਆਂ);
    of one in a consonant its plural in -ਾਂ (ਪਿੰਡਾਂ). Not the ablative in -ੋਂ, as ਵਰਤੋਂ "use" is no ਵਰਤ "fast"'s."""
    adjective = bool(kinds & {"adj", "det", "num"})
    if word.endswith("ਾ"):
        stem = word[:-1]
        forms = [stem + "ੇ", stem + "ਿਆਂ", stem + "ਿਆ"] if adjective or "f" not in gender or "m" in gender else []
        if adjective:
            forms += [stem + "ੀ", stem + "ੀਆਂ"]
        if "noun" in kinds and "m" not in gender:
            forms.append(word + "ਵਾਂ")
        return forms
    if word[-1] in VOWEL_SIGNS or word[-1] in VOWELS:
        return [] if adjective and "noun" not in kinds else [word + "ਆਂ"]
    if word[-1] in (BINDI, TIPPI) or adjective and "noun" not in kinds:
        return []
    return [word + "ਾਂ"]


def verb_stem(verb):
    """A verb's stem and the letters of its infinitive (ਣ, or ਨ and ਣ), or None."""
    if len(verb) < 3 or not verb.endswith(("ਣਾ", "ਨਾ")):
        return None
    stem = verb[:-2]
    return stem, ("ਨ", "ਣ") if stem.endswith(N_STEMS) else ("ਣ",)


def verb_forms(stem, letters):
    """
    A verb's regular forms from its stem, each with whether it is a telling one, which no noun has (a participle, the
    future, the infinitive in -ਾ): its infinitive in each spelling (ਪੜ੍ਹਨ and ਪੜ੍ਹਣ, ਪੜ੍ਹਨੀ, ਪੜ੍ਹਨਗੇ), present and past
    participles, subjunctive, future and imperative, as a stem in -ਉ (ਚਲਾਉਣਾ: ਚਲਾਉਂਦਾ, ਚਲਾਇਆ, ਚਲਾਵੇ), another vowel
    (ਪੀਣਾ: ਪੀਂਦਾ; ਪੀਤਾ is no rule's) or a consonant (ਭੇਜਣਾ: ਭੇਜਦਾ, ਭੇਜਿਆ, ਭੇਜੇਗਾ) makes them.
    """
    forms = {}

    def add(base, tails, telling):
        for tail in tails:
            forms.setdefault(base + tail, telling)

    for n in letters:
        add(stem + n, ("ਾ", "ਗੇ", "ਗੀਆਂ"), True)
        add(stem + n, ("", "ੀ", "ੇ", "ੀਆਂ", "ੋਂ"), False)
    participles = ("ਦਾ", "ਦੀ", "ਦੇ", "ਦੀਆਂ", "ਦਿਆਂ")
    if stem[-1] in VOWEL_SIGNS or stem[-1] in VOWELS:
        base = stem[:-1] if stem.endswith("ਉ") and len(stem) > 1 else stem
        add(stem + BINDI, participles, True)
        add(base, ("ਇਆ", "ਵੇਗਾ", "ਵੇਗੀ", "ਵਾਂਗਾ", "ਵਾਂਗੀ", "ਵਾਂਗੇ", "ਵੋਗੇ") + (("ਏਗਾ", "ਏਗੀ") if base != stem else ()), True)
        add(base, ("", "ਈ", "ਏ", "ਈਆਂ", "ਇਆਂ", "ਓ", "ਵਾਂ", "ਵੇ", "ਵੋ", "ਈਏ"), False)
    else:
        add(stem, participles + ("ਿਆ", "ੇਗਾ", "ੇਗੀ", "ਾਂਗਾ", "ਾਂਗੀ", "ਾਂਗੇ", "ੋਗੇ"), True)
        add(stem, ("", "ੀ", "ੇ", "ੀਆਂ", "ਿਆਂ", "ੋ", "ਾਂ", "ਿਓ", "ੀਏ"), False)
    return forms


def regular_forms(word, kinds, gender):
    """A word's regular forms by its kinds (nominal_forms, verb_forms)."""
    forms = nominal_forms(word, kinds, gender) if kinds & NOMINAL_KINDS else []
    if "verb" in kinds and verb_stem(word):
        forms += list(verb_forms(*verb_stem(word)))
    return [f for f in forms if f != word]


# Verbs' forms that no rule makes and the tables lack (ਖਾਧਾ, ਕੀਤਾ; ਲੱਗਾ, which is not ਲਗਾ, ਲਗਾਉਣਾ's stem), filed under the
# verb where the pack has no such word or form.
IRREGULAR = {
    "ਹੋਣਾ": "ਹਾਂ ਹੈਂ ਹੋ ਹੈ ਹਨ ਸੀ ਸਾਂ ਸੀਂ ਸਨ ਸੀਗਾ ਹੋਵੇ ਹੋਵੇਗਾ ਹੋਵੇਗੀ ਹੋਣਗੇ ਹੋਵਾਂਗੇ ਹੋਇਆ ਹੋਈ ਹੋਏ ਹੋਈਆਂ ਹੁੰਦਾ ਹੁੰਦੀ ਹੁੰਦੇ ਹੁੰਦੀਆਂ",
    "ਜਾਣਾ": "ਗਿਆ ਗਈ ਗਏ ਗਈਆਂ ਜਾਂਦਾ ਜਾਂਦੀ ਜਾਂਦੇ ਜਾਂਦੀਆਂ ਜਾਵਾਂ ਜਾਵੇ ਜਾਵੋ ਜਾਓ ਜਾਣਗੇ ਜਾਵੇਗਾ ਜਾਵੇਗੀ ਜਾਵਾਂਗਾ ਜਾਵਾਂਗੀ ਜਾਵਾਂਗੇ ਜਾਏਗਾ ਜਾਏਗੀ",
    "ਕਰਨਾ": "ਕੀਤਾ ਕੀਤੀ ਕੀਤੇ ਕੀਤੀਆਂ ਕਰੋ ਕਰਿਓ ਕਰੀਏ",
    "ਦੇਣਾ": "ਦਿੱਤਾ ਦਿੱਤੀ ਦਿੱਤੇ ਦਿੱਤੀਆਂ ਦਿੰਦਾ ਦਿੰਦੀ ਦਿੰਦੇ ਦਿੰਦੀਆਂ ਦਿਓ ਦੇਵੋ ਦੇਵਾਂ ਦੇਵੇ ਦੇਵੇਗਾ ਦੇਵੇਗੀ ਦੇਣਗੇ",
    "ਲੈਣਾ": "ਲਿਆ ਲਈ ਲਏ ਲਈਆਂ ਲੈਂਦਾ ਲੈਂਦੀ ਲੈਂਦੇ ਲੈਂਦੀਆਂ ਲਓ ਲਵੋ ਲਵਾਂ ਲਵੇ ਲਵੇਗਾ ਲਵੇਗੀ ਲੈਣਗੇ",
    "ਪੀਣਾ": "ਪੀਤਾ ਪੀਤੀ ਪੀਤੇ ਪੀਂਦਾ ਪੀਂਦੀ ਪੀਂਦੇ ਪੀਓ ਪੀਵੇ",
    "ਖਾਣਾ": "ਖਾਧਾ ਖਾਧੀ ਖਾਧੇ ਖਾਧੀਆਂ ਖਾਂਦਾ ਖਾਂਦੀ ਖਾਂਦੇ ਖਾਓ ਖਾਵੇ ਖਾਵਾਂ",
    "ਆਉਣਾ": "ਆਇਆ ਆਈ ਆਏ ਆਈਆਂ ਆਉਂਦਾ ਆਉਂਦੀ ਆਉਂਦੇ ਆਉਂਦੀਆਂ ਆਓ ਆਵੇ ਆਵਾਂ ਆਵੇਗਾ ਆਵੇਗੀ ਆਉਣਗੇ",
    "ਕਹਿਣਾ": "ਕਿਹਾ ਕਹੀ ਕਹੇ ਕਹਿੰਦਾ ਕਹਿੰਦੀ ਕਹਿੰਦੇ ਕਹਿੰਦੀਆਂ ਕਹੋ",
    "ਰਹਿਣਾ": "ਰਿਹਾ ਰਹੀ ਰਹੇ ਰਹੀਆਂ ਰਹਿੰਦਾ ਰਹਿੰਦੀ ਰਹਿੰਦੇ ਰਹਿੰਦੀਆਂ ਰਹੋ",
    "ਸੌਣਾ": "ਸੁੱਤਾ ਸੁੱਤੀ ਸੁੱਤੇ ਸੌਂਦਾ ਸੌਂਦੀ ਸੌਂਦੇ",
    "ਲੱਗਣਾ": "ਲੱਗਾ ਲੱਗੀ ਲੱਗੇ ਲੱਗੀਆਂ",
    "ਚੁੱਕਣਾ": "ਚੁੱਕਾ ਚੁੱਕੀ ਚੁੱਕੇ ਚੁੱਕੀਆਂ",
    "ਖੜ੍ਹਨਾ": "ਖੜ੍ਹਾ ਖੜ੍ਹੀ ਖੜ੍ਹੇ ਖੜ੍ਹੀਆਂ ਖੜਾ ਖੜੀ ਖੜੇ",
}


# Everyday words the pack lacks, or has only as a name, with their kinds: ਰੱਬ "God", ਬ੍ਰਹਿਮੰਡ "universe", ਸੁਆਹ "ash",
# languages, months and days; ਵਜੇ "o'clock" (the pack's ਵੱਜੇ, a form of ਵੱਜਣਾ, otherwise); ਕੇਸ "case", ਪਦ "verse, post",
# ਗੋਲੀ "bullet, pill" and ਕੱਪ "cup", which it has only spelled as other words (ਕੇਸ਼ "hair", ਪੱਦ "fart", ਗੋਲਾ "slave",
# ਕੱਪਣਾ "to cut").
WORDS = {"ਰੱਬ": "noun", "ਬ੍ਰਹਿਮੰਡ": "noun", "ਸੁਆਹ": "noun", "ਅੰਗਰੇਜ਼ੀ": "noun", "ਸੰਸਕ੍ਰਿਤ": "noun", "ਜਨਵਰੀ": "noun",
         "ਦਸੰਬਰ": "noun", "ਐਤਵਾਰ": "noun", "ਵਜੇ": "noun", "ਕੇਸ": "noun", "ਪਦ": "noun", "ਗੋਲੀ": "noun", "ਕੱਪ": "noun",
         "ਸੰਨ": "noun", "ਜਮ੍ਹਾਂ": "adj"}
# Forms of the corpus that are no words for induce, among the first 3,000 it would find: names of people, places, parties
# and firms (as the pack's names are none), English words that come only in English names (ਆਫ, ਐਂਡ, ਨੈਸ਼ਨਲ), and
# abbreviations and ordinal endings written apart (ਸ੍ਰ, ਪ੍ਰੋ, ਕਿਮੀ, ਵੀਂ).
NOT_WORDS = set("""
ਨਾਨਕ ਲਾਹੌਰ ਬਾਦਲ ਖਾਨ ਭਾਜਪਾ ਕਾਂਗਰਸ ਬਠਿੰਡਾ ਮੋਦੀ ਯੂਰਪ ਏਸ਼ੀਆ ਸ਼ਰਮਾ ਰਣਜੀਤ ਹੁਸ਼ਿਆਰਪੁਰ ਫਰਾਂਸ ਨਿਊਯਾਰਕ ਆਸਟਰੇਲੀਆ
ਅਫਰੀਕਾ ਨਰਿੰਦਰ ਚੰਦਰ ਜਾਪਾਨ ਮਹਾਰਾਸ਼ਟਰ ਸੋਨ ਫਰੀਦਕੋਟ ਰਾਓ ਗੂਗਲ ਕਲਕੱਤਾ ਸਪੇਨ ਡੇਵਿਡ ਤਲਵਾੜਾ ਕੇਜਰੀਵਾਲ ਕੈਲੀਫੋਰਨੀਆ ਵਿਲੀਅਮ
ਜਾਰਜ ਸੁਰਿੰਦਰ ਅਕਬਰ ਜੰਮੂ ਕਬੀਰ ਟੈਗੋਰ ਫੇਸਬੁੱਕ ਰਾਹੁਲ ਸੇਨ ਫਿਲਮਫੇਅਰ ਫ਼ਰੀਦ ਟਾਈਮਜ਼ ਮੈਰੀ ਆਂਧਰਾ ਹੈਰੀ ਇਰਾਕ ਇਕਬਾਲ ਮਨਮੋਹਨ
ਅਰਜਨ ਗੁਰਦਾਸਪੁਰ ਜੋਸ਼ੀ ਕੋਲਕਾਤਾ ਯਾਰਕ ਜੌਨ ਦੱਤ ਗੁਪਤਾ ਸ਼੍ਰੀਲੰਕਾ ਅਬਦੁਲ ਜੈਕ ਮੀਰ ਕਿਸ਼ੋਰ ਜੱਸਾ ਮਾਲਵੇ ਗੁਰਸ਼ਰਨ ਅਰਜੁਨ ਨਾਭਾ
ਸੁਖਬੀਰ ਯਾਦਵ ਗੋਆ ਜੇਮਜ਼ ਮਾਈਕਲ ਚਾਰਲਸ ਅਮਰਜੀਤ ਮਦਰਾਸ ਹਰਮਨ ਪ੍ਰੀਤਮ ਸੁਖਦੇਵ ਵਿਜੇ ਸੱਯਦ ਲਖਨਊ ਮੋਗਾ ਕੇਰਲ ਅਜਮੇਰ ਹਿਮਾਚਲ
ਸ਼ੰਕਰ ਬੰਗਲੌਰ ਪ੍ਰਤਾਪ ਮੰਮਟ ਭਰਤ ਹੈਨਰੀ ਲੰਦਨ ਸਿਕੰਦਰ ਬਰਨਾਲਾ ਕਾਰਲ ਜਸਵੰਤ ਅਰਸਤੂ ਪੀਟਰ ਹਰਭਜਨ ਅਜੀਤ ਫਿਰੋਜ਼ਪੁਰ ਵਾਸ਼ਿੰਗਟਨ
ਮਲਿਕ ਰਾਬਰਟ ਤਾਮਿਲਨਾਡੂ ਗੁਰਬਖਸ਼ ਹੁਸੈਨ ਅਹਿਮਦ ਬੇਦੀ ਵਰਮਾ ਯੂਕੇ ਇੰਡੀਆ ਕਪੂਰਥਲਾ ਕਿਸ਼ਨ ਕੋਲੰਬੀਆ ਜੈਪੁਰ ਟਾਟਾ ਠਾਕੁਰ ਤਲਵੰਡੀ
ਪਟੇਲ ਬਲਦੇਵ ਮਹਿਤਾ ਮਹਿੰਦਰ ਮੁਕਤਸਰ ਰਾਧਾ ਲੈਨਿਨ ਵਿਸ਼ਨੂੰ ਸਮਿਥ ਸ਼ਿਕਾਗੋ ਸਿੰਗਾਪੁਰ ਸੰਨੀ ਹਸਨ ਹਿਮਾਲਾ ਲਾਲਾ
ਬ੍ਰਾਜ਼ੀਲ ਮਾਨਸਾ ਰਾਜੀਵ ਬ੍ਰਾਜ਼ੀਲ ਮਾਨਸਾ ਰਾਜੀਵ ਕਿੰਗਡਮ ਸਾਊਥ ਸੈਂਟਰਲ ਵਾਟਰ
ਆਫ ਆਫ਼ ਐਂਡ ਦ ਫਾਰ ਇੰਨ ਓਵਰ ਅੰਡਰ ਈਸਟ ਵੈਸਟ ਵ੍ਹਾਈਟ ਰੈੱਡ ਬਲੈਕ ਬਿੱਗ ਬੈਸਟ ਗੋਲਡਨ ਮੈਨ ਨੈਸ਼ਨਲ ਇੰਟਰਨੈਸ਼ਨਲ ਵਰਲਡ ਸਟੇਟ
ਏਅਰ ਸੇਂਟ ਯੂਨਾਈਟਿਡ ਰਾਇਲ ਐਕਸਪ੍ਰੈਸ ਕਿੰਗ ਹਾਈ ਨਿਊ ਆਰਟਸ ਸਪੋਰਟਸ ਓਪਨ ਡਬਲਯੂ ਲਵ ਵਨ ਹਮ
ਸ੍ਰ ਪ੍ਰੋ ਕਿਮੀ ਵੀਂ ਵੇਂ
""".split())
# A form no word of the pack explains is a word of its own when written bare INDUCED_BARE times, or INDUCED_LONE times when
# the corpus has it in no other form (most such are names and English); a verb when INDUCED_FORMS of its telling forms
# (verb_forms), an infinitive and a participle among them, are written INDUCED_VERB times in all, and it takes its other
# forms (its subjunctive, ਭੇਜੇ) only when they are at most INDUCED_LOOSE times those (not ਵਿਖੇ "at" of ਵਿਖਣਾ, nor ਚੋਣ
# "election" of ਚੋਣਾ), its stem (ਭੇਜ, written apart before ਦਿੱਤਾ, ਕੇ) INDUCED_STEM times. A word in -ਾ takes its feminine forms (ਪਿਛਲੀ of ਪਿਛਲਾ) only when they are between
# INDUCED_FEMININE times its others and 1/INDUCED_FEMININE of them (not ਦੇਵੀ of ਦੇਵਾ, nor ਹਿੱਸੀਆਂ of ਹਿੱਸਾ).
INDUCED_BARE = 20
INDUCED_LONE = 100
INDUCED_FORMS = 3
INDUCED_VERB = 30
INDUCED_LOOSE = 3
INDUCED_STEM = 10
INDUCED_FEMININE = 3
INFINITIVE_TAILS = ("ਣਾ", "ਨਾ", "ਣ", "ਨ", "ਣੀ", "ਨੀ", "ਣੇ", "ਨੇ")
PARTICIPLE_TAILS = ("ਦਾ", "ਦੀ", "ਦੇ", "ਦੀਆਂ", "ਿਆ", "ਇਆ")
# The endings a verb's form is told by, longest first: the stem before them is the verb's (ਭੇਜ of ਭੇਜਦੇ, ਚਲਾਉ of ਚਲਾਉਂਦਾ).
VERB_TAILS = sorted({"ਣਾ", "ਨਾ", "ਣ", "ਨ", "ਣੀ", "ਨੀ", "ਣੇ", "ਨੇ", "ਦਾ", "ਦੀ", "ਦੇ", "ਦੀਆਂ", "ਦਿਆਂ", "ਿਆ", "ੇਗਾ", "ੇਗੀ",
                     "ਣਗੇ", "ਨਗੇ"}, key=len, reverse=True)
# Forms induce keeps as words of their own though another word's paradigm makes them: postpositions in -ੇ (ਸਾਹਮਣੇ "in
# front of", not ਸਾਹਮਣਾ "encounter"; ਵਾਸਤੇ "for", not ਵਾਸਤਾ "connection").
INDUCED_OWN = {"ਸਾਹਮਣੇ", "ਵਾਸਤੇ", "ਸਿਟੀ", "ਵਿਧੀ"}


def induce(unexplained):
    """
    The words the corpus's forms [unexplained] (form: count) are of (see INDUCED_BARE): word -> (kind, its forms). The
    commonest by their forms' counts first, so a form goes to one of them only; spellings alike but for loose are one word,
    under the commonest.
    """
    alike = {}
    for form in unexplained:
        alike.setdefault(loose(form), []).append(form)

    def variants(forms):
        return {v for f in forms for v in alike.get(loose(f), ())}

    def mass(forms):
        return sum(unexplained[f] for f in forms)

    def verbal(forms):
        return len(forms) >= INDUCED_FORMS and mass(forms) >= INDUCED_VERB and any(
            f.endswith(PARTICIPLE_TAILS) for f in forms)

    own = variants(INDUCED_OWN)
    candidates = set()
    for group in alike.values():
        head = max(group, key=lambda f: (unexplained[f], f))
        # Not a Latin letter's name (ਬੀ, ਡੀ, ਐਸ), but a word of two consonants (ਦਰ "rate", ਦਲ "party").
        if unexplained[head] >= INDUCED_BARE and (len(head) > 2 or all(CONSONANT.fullmatch(c) for c in head)):
            candidates.add((head, "noun"))
    for form in unexplained:
        for tail in VERB_TAILS:
            stem = form[:-len(tail)]
            if not form.endswith(tail) or len(stem) < 2 or stem.endswith("੍"):
                continue
            # A vowel stem takes a bindi before the participle (ਚਲਾਉਂਦਾ, ਪੀਂਦਾ).
            if tail.startswith("ਦ") and stem.endswith(BINDI) and (stem[-2] in VOWEL_SIGNS or stem[-2] in VOWELS):
                stem = stem[:-1]
            letters = ("ਨ", "ਣ") if stem.endswith(N_STEMS) else ("ਣ",)
            candidates.add((max((stem + n + "ਾ" for n in letters), key=lambda v: unexplained.get(v, 0)), "verb"))
    paradigms = {}
    for word, kind in candidates:
        made = verb_forms(*verb_stem(word)) if kind == "verb" else dict.fromkeys(nominal_forms(word, {"noun", "adj"}, ""), False)
        forms = variants(list(made) + [word]) - (own - {word})
        telling = set()
        if kind == "verb":
            telling = variants(f for f, tells in made.items() if tells) & forms
            if not verbal(telling) or not any(f.endswith(INFINITIVE_TAILS) for f in forms):
                continue
            stems = variants([verb_stem(word)[0], verb_stem(word)[0][:-1]] if word.endswith("ਉਣਾ") else [verb_stem(word)[0]])
            forms = telling | {f for f in forms - telling
                               if unexplained[f] <= (INDUCED_STEM if f in stems else INDUCED_LOOSE) * mass(telling)}
        elif word.endswith("ਾ"):
            # An adjective's feminine (ਪਿਛਲੀ), not a noun in -ੀ made from one in -ਾ (ਗਾਇਕੀ "singing" of ਗਾਇਕਾ "singer"):
            # an adjective's oblique in -ੇ is common too.
            feminine = variants([word[:-1] + "ੀ", word[:-1] + "ੀਆਂ"]) & forms
            others = forms - feminine
            if not (mass(others) <= INDUCED_FEMININE * mass(feminine) <= INDUCED_FEMININE ** 2 * mass(others)
                    and INDUCED_FEMININE * mass(variants([word[:-1] + "ੇ"]) & forms) >= mass(variants([word]) & forms)):
                forms -= feminine
        paradigms[word, kind] = (forms, telling)
    claimed, chosen = set(), {}
    heap = [(-mass(forms), word, kind) for (word, kind), (forms, _) in paradigms.items()]
    heapq.heapify(heap)
    while heap:
        negative, word, kind = heapq.heappop(heap)
        forms, telling = paradigms[word, kind]
        forms, telling = forms - claimed, telling - claimed
        if mass(forms) < -negative:
            paradigms[word, kind] = (forms, telling)
            if forms:
                heapq.heappush(heap, (-mass(forms), word, kind))
            continue
        if kind == "noun":
            bare = variants([word]) & forms
            if word in claimed or mass(bare) < INDUCED_BARE or forms <= bare and mass(bare) < INDUCED_LONE:
                continue
        elif not verbal(telling):
            continue
        # The word as texts write it most.
        shown = max(variants([word]) & forms or {word}, key=lambda f: (unexplained.get(f, 0), f == word))
        chosen[shown] = (kind, forms | {shown})
        claimed |= forms | {word}
    return chosen


def relink(freq, pos, lemmas, is_word, count, overrides, excluded, dictionaries, common):
    """
    Files the forms the pack lacks (see the module's note) in [lemmas], and a word spelled as another (spellings) under it,
    and adds the words the corpus shows the pack lacks (induce) to [pos] and [common]; gives the overrides that sends such a
    word's spelling to the other.
    """
    moved = spellings(dictionaries, is_word, count)
    for form, of in lemmas.items():
        if any(l in moved for l in of):
            lemmas[form] = list(dict.fromkeys(moved.get(l, l) for l in of))
    for w, head in moved.items():
        lemmas[w] = [head]
    for word, kind in WORDS.items():
        pos[word].add(kind)
        common[word] = word

    def free(form):
        return form not in pos and not lemmas.get(form) and form not in overrides and form not in excluded

    genders = pack_genders(dictionaries)
    made = {}
    for word in [w for w in pos if is_word(w) and w not in moved]:
        for form in regular_forms(word, pos[word], genders.get(word, "")):
            if free(form):
                made.setdefault(form, []).append(word)
    for verb, forms in IRREGULAR.items():
        for form in forms.split():
            if free(form):
                made[form] = [verb]
    for form, words in made.items():
        lemmas[form] = words
    known = {}
    for w in set(pos) | set(lemmas):
        if is_word(w) or lemmas.get(w):
            known.setdefault(loose(w), []).append(w)
    for form in list(freq):
        if not free(form):
            continue
        joined = next((OBLIQUE_PRONOUNS[form[:-len(p)]] for p in JOINED_POSTPOSITIONS
                       if form.endswith(p) and form[:-len(p)] in OBLIQUE_PRONOUNS), None)
        if joined and is_word(joined):
            lemmas[form] = [joined]
            continue
        spelled = [w for w in known.get(loose(form), ()) if w != form]
        # Or as it is but for a last bindi the text forgot or added (ਨਹੀ for ਨਹੀਂ, ਵਿੱਚੋ for ਵਿੱਚੋਂ), the commoner spelling.
        if not spelled:
            other = form[:-1] if form[-1] in (BINDI, TIPPI) else form + BINDI
            spelled = [w for w in known.get(loose(other), ()) if count(w) > count(form)]
        # Texts leave out nukta and addak, seldom add them: a form with both more than a spelling is another word (ਸ਼ੱਕੀ
        # "suspicious", not ਸਕੀ of ਸਕਣਾ).
        spelled = [w for w in spelled if marks(form) - marks(w) < 2]
        if spelled:
            like = max(spelled, key=lambda w: (count(w), w))
            lemmas[form] = list(dict.fromkeys(moved.get(w, w) for w in ([like] if is_word(like) else lemmas[like])))
    unit = min(freq.values())
    # A name left out leaves out its forms too (ਬਠਿੰਡੇ of ਬਠਿੰਡਾ), however spelled (ਫ਼ਰਾਂਸ).
    unwanted = {loose(f) for word in NOT_WORDS for f in [word] + nominal_forms(word, {"noun"}, "")}
    unexplained = {f: round(n / unit) for f, n in freq.items() if free(f) and len(f) > 1 and loose(f) not in unwanted}
    for word, (kind, forms) in induce(unexplained).items():
        pos[word].add(kind)
        common[word] = word
        for form in forms - {word}:
            lemmas[form] = [word]
    return {w: [head] for w, head in moved.items() if w not in overrides}


# How the sentences use a word (read_usage), once it is counted: a page (an article, a web page) counts it PAGE_MOST times at
# most, so that a word one article or blog repeats (ਰਾਮਾਨੁਜਨ, ਸਪਿੱਨੌਰ, ਜਨਮਸਾਖੀ) is not a common one; and its uses as a name's
# piece count for nothing: after a title (ਡਾ ਰਵਿੰਦਰ, ਸ੍ਰੀ ਲੰਕਾ), before a Sikh or Muslim name's second part or a surname
# (ਕੁਲਦੀਪ ਸਿੰਘ, ਸਲਮਾਨ ਖ਼ਾਨ, ਸੰਜੇ ਦੱਤ), or beside a word no word of the list explains that the sentences have fewer than
# RARE_SEEN times (the news's and Wikipedia's other names and English: ਰਿਚਰਡ ਨਿਕਸਨ, ਮਾਰਟਿਨ ਲੂਥਰ, ਹੋਮ ਗਾਰਡ).
PAGE_MOST = 2
RARE_SEEN = 40
TITLES = set("""
ਡਾ ਸ ਸ੍ਰ ਸ੍ਰੀ ਸ਼੍ਰੀ ਸ੍ਰੀਮਤੀ ਸ਼੍ਰੀਮਤੀ ਸਰ ਸੇਂਟ ਕਿੰਗ ਲਾਰਡ ਪੰਡਤ ਪੰਡਿਤ ਮਿਸਟਰ ਮਿਸ ਮੌਲਾਨਾ ਬਾਬਾ ਭਾਈ ਸੰਤ ਪ੍ਰੋ ਪ੍ਰੋਫੈਸਰ
ਪ੍ਰੋਫ਼ੈਸਰ ਮਹਾਰਾਜਾ ਰਾਜਾ ਰਾਣੀ ਬੀਬੀ ਸ਼ੇਖ ਮੀਆਂ ਸਰਦਾਰ ਗਿਆਨੀ ਜਥੇਦਾਰ ਕਾਮਰੇਡ ਹਜ਼ਰਤ ਬਾਬੂ ਕੈਪਟਨ ਕਰਨਲ ਮੇਜਰ ਸਵਾਮੀ ਮੁਹੰਮਦ
""".split())
NAME_PARTS = set("""
ਸਿੰਘ ਕੌਰ ਖ਼ਾਨ ਖਾਨ ਅਲੀ ਮੁਹੰਮਦ ਕੁਮਾਰ ਪ੍ਰਸਾਦ ਨਾਥ ਰਾਓ ਸ਼ਰਮਾ ਗੁਪਤਾ ਵਰਮਾ ਗਾਂਧੀ ਦੱਤ ਬਾਜਵਾ ਸੰਧੂ ਗਿੱਲ ਢਿੱਲੋਂ ਸਿੱਧੂ ਗਰੇਵਾਲ
ਬਰਾੜ ਚੀਮਾ ਬੇਦੀ ਕਪੂਰ ਜੋਸ਼ੀ ਯਾਦਵ ਪਟੇਲ ਮਹਿਤਾ ਮਲਿਕ ਅਹਿਮਦ ਹੁਸੈਨ
""".split())
# A noun the sentences use as a name's piece (above) NAMED_SHARE of the times they have it is a name, and left out.
NAMED_SHARE = 0.45
LETTERS = re.compile("[\u0a01-\u0a65\u0a70-\u0a75]+")


def _sentences(corpora):
    """Each sentence of the Leipzig archives [corpora] as (its page, its words)."""
    for path in corpora:
        if not path:
            continue
        name = os.path.basename(path)
        with tarfile.open(path) as tar:
            members = {m.name.rsplit("-", 1)[-1]: m for m in tar.getmembers() if m.isfile()}
            page_of = {}
            for line in io.TextIOWrapper(tar.extractfile(members["inv_so.txt"]), encoding="utf-8"):
                source, sentence = line.rstrip("\n").split("\t")[:2]
                page_of[sentence] = source
            for line in io.TextIOWrapper(tar.extractfile(members["sentences.txt"]), encoding="utf-8"):
                number, _, text = line.rstrip("\n").partition("\t")
                yield name + "/" + page_of.get(number, ""), LETTERS.findall(unicodedata.normalize("NFC", text))


def read_usage(corpora, known):
    """
    How often the sentences have each word (seen), counting each page PAGE_MOST times at most (spread), and how often as a
    name's piece or as a piece of a word a space split (lost; see PAGE_MOST).
    """
    sentences = sorted(_sentences(corpora), key=lambda s: s[0])
    seen = Counter()
    for _, words in sentences:
        seen.update(words)

    def rare(word):
        return word and word not in known and seen[word] < RARE_SEEN

    spread, lost, pairs = Counter(), Counter(), Counter()
    page, on_page = None, Counter()
    for here, words in sentences + [(None, [])]:
        if here != page:
            spread.update({w: min(n, PAGE_MOST) for w, n in on_page.items()})
            page, on_page = here, Counter()
        on_page.update(words)
        last = len(words) - 1
        for i, word in enumerate(words):
            before = words[i - 1] if i else ""
            after = words[i + 1] if i < last else ""
            if before in TITLES or after in NAME_PARTS or rare(before) or rare(after):
                lost[word] += 1
            if after and word + after in known:
                pairs[word, after] += 1
    # Two pieces of a word the sentences write whole at least as often as apart (ਬਹੁ ਗਿਣਤੀ, ਨਵ ਉਦਾਰਵਾਦੀ, ਡਾਕ ਖ਼ਾਨਾ).
    for (a, b), n in sorted(pairs.items()):
        if seen[a + b] >= n:
            lost[a] += n
            lost[b] += n
    return seen, spread, {w: min(n, seen[w]) for w, n in lost.items()}


def _unpieced(scores, forms, pos):
    """
    A verb's forms that tell no verb (its stem, subjunctive, -ੀ, -ੇ: ਚੌਂਕ, ਚੌਂਕੀ of ਚੌਂਕਣਾ, ਜਰੀਏ of ਜਰਨਾ, ਸੀਤਾ of ਸਿਊਣਾ)
    count for it only when at most INDUCED_LOOSE times its telling forms (verb_forms, IRREGULAR), its stem INDUCED_STEM
    times, as induce has it: else they are another word's or a name.
    """
    for verb in sorted(scores):
        if "verb" not in pos.get(verb, ()) or not verb_stem(verb):
            continue
        stem, letters = verb_stem(verb)
        made = verb_forms(stem, letters)
        tells = {loose(f) for f, telling in made.items() if telling} | {loose(f) for f in IRREGULAR.get(verb, "").split()}
        stems = {loose(stem), loose(stem[:-1])} if verb.endswith("ਉਣਾ") else {loose(stem)}
        counted = forms[verb]
        telling = sum(c for f, c in counted.items() if loose(f) in tells)
        for form in sorted(counted):
            if loose(form) not in tells and counted[form] > (INDUCED_STEM if loose(form) in stems else INDUCED_LOOSE) * telling:
                del counted[form]
        scores[verb] = sum(counted.values())


def fold(scores, forms, pos, lemmas, dictionaries, corpora):
    """
    After the counting: a form counts only its uses as a word, spread over the pages (read_usage); a verb's forms that tell
    none count only beside its telling ones (_unpieced); a noun mostly a name's piece (NAMED_SHARE) is left out.
    """
    known = set(pos) | {f for f, of in lemmas.items() if of}
    seen, spread, lost = read_usage(corpora, known)
    for word in sorted(scores):
        counted = forms[word]
        for form in sorted(counted):
            if seen[form]:
                counted[form] *= spread[form] / seen[form] * (1 - lost.get(form, 0) / seen[form])
        scores[word] = sum(counted.values())
    _unpieced(scores, forms, pos)
    for word in sorted(scores):
        uses = sum(seen[f] for f in forms[word])
        if scores[word] <= 0 or uses and not pos.get(word, set()) - {"noun", "name"} and sum(
                lost.get(f, 0) for f in forms[word]) >= NAMED_SHARE * uses:
            del scores[word]
