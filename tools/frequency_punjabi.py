"""
Punjabi for tools/build_frequency_list.py (Gurmukhi).

The pa-en pack is small, and texts spell many of its words otherwise than it does: without the nukta (ਫਿਲਮ for ਫ਼ਿਲਮ,
ਖਾਸ for ਖ਼ਾਸ), without the addak (ਉਥੇ for ਉੱਥੇ, ਵਿਚੋਂ for ਵਿੱਚੋਂ, ਤਕ for ਤੱਕ), with tippi for bindi or the other way
round; and they write an oblique pronoun with its postposition joined on (ਉਸਨੇ, ਇਸਦਾ, ਜਿਸਨੂੰ). So a form the pack
lacks is (relink):

- the pronoun's, when it is an oblique pronoun with a postposition joined on;
- else the pack's word or form spelled as it is but for nukta, addak, tippi and bindi (folded), the commonest.

And the pack has many words twice, spelled with and without the nukta or addak (ਜ਼ਮੀਨ and ਜਮੀਨ, ਖ਼ੁਸ਼ੀ and ਖੁਸ਼ੀ, ਪੱਗ
and ਪਗ): two words spelled so that share a word of their glosses are one, under the spelling texts use most (spellings).
Two that share none stay apart (ਸਾਲ "year", ਸ਼ਾਲ "shawl"; ਪਤਾ "address", ਪੱਤਾ "leaf").
"""
import json
import os
import re
import sqlite3

NUKTA = "਼"
ADDAK = "ੱ"
TIPPI = "ੰ"
BINDI = "ਂ"


def folded(word):
    """A Gurmukhi spelling without nukta and addak, with bindi for tippi."""
    return word.replace(NUKTA, "").replace(ADDAK, "").replace(TIPPI, BINDI)


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
    # Words the pack lacks, filed under others it spells alike: ਕੇਸ "case" (ਕੇਸ਼ "hair"), ਪਦ "verse, post" (ਪੱਦ "fart"),
    # ਗੋਲੀ "bullet, pill" (ਗੋਲਾ "slave"), ਕੱਪ "cup" (ਕੱਪਣਾ "to cut").
    **{f: [] for f in ("ਕੇਸ", "ਕੇਸਾਂ", "ਪਦ", "ਗੋਲੀ", "ਗੋਲੀਆਂ", "ਕੱਪ", "ਕੱਪਾਂ")},
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


def relink(freq, pos, lemmas, is_word, count, overrides, excluded, dictionaries):
    """
    Files the forms the pack lacks (see the module's note) in [lemmas], and a word spelled as another (spellings) under it;
    gives the overrides that sends such a word's spelling to the other.
    """
    moved = spellings(dictionaries, is_word, count)
    for form, of in lemmas.items():
        if any(l in moved for l in of):
            lemmas[form] = list(dict.fromkeys(moved.get(l, l) for l in of))
    for w, head in moved.items():
        lemmas[w] = [head]
    known = {}
    for w in set(pos) | set(lemmas):
        if is_word(w) or lemmas.get(w):
            known.setdefault(folded(w), []).append(w)
    for form in list(freq):
        if form in pos or lemmas.get(form) or form in overrides or form in excluded:
            continue
        joined = next((OBLIQUE_PRONOUNS[form[:-len(p)]] for p in JOINED_POSTPOSITIONS
                       if form.endswith(p) and form[:-len(p)] in OBLIQUE_PRONOUNS), None)
        if joined and is_word(joined):
            lemmas[form] = [joined]
            continue
        spelled = [w for w in known.get(folded(form), ()) if w != form]
        if spelled:
            lemmas[form] = [moved.get(w, w) for w in [max(spelled, key=lambda w: (count(w), w))]]
    return {w: [head] for w, head in moved.items() if w not in overrides}
