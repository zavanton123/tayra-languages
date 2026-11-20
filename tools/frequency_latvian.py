"""
Latvian words and forms the English pack lacks, for the frequency list.

The English pack (17,000 entries, half of them forms of others) has no entry for prefixed verbs as common as iekāpt,
apturēt, noņemt, ielikt, satikt and nopirkt, though it conjugates their verbs in full (kāpt, turēt, ņemt, likt, tikt,
pirkt); nor for kilometrs, traktors and vagons, which the packs glossed in other languages have; and the tables of many
of its nouns stop at two or three cases (pakalpojums has pakalpojumi and pakalpojumus, not pakalpojumu). Counted only
through it, such words were missing from the list or ranked far below their use. So:

- a reflexive verb gets its plain verb's forms made reflexive (justies: jūtos, jūties of just), whether the pack has it with
  a short table or not at all (priecāties, veikties);
- a prefixed verb gets its verb's forms with the prefix (no + ņēma, ne + no + ņem, jā + no + ņem), whether the pack has
  the prefixed verb with a short table (paņemt: paņem, paņēmu) or not at all (noņemt);
- the other packs' words come in where the English pack has no word or form spelled so (OTHER_PACKS), with their forms;
- a noun or adjective gets the regular forms of its declension (the pack's declension-N tag, else its ending);
- and some common words no pack has are added by hand (WORDS: mīnus, papildus, labprāt, citur, prasība, transports).

A made form goes to its word only where the English pack spells no word or form so, and only if the counts have it (SEEN).
"""
import json
import os
import re
import sqlite3
from collections import defaultdict

# The forms the counts have, set by the list builder before the pack is read.
SEEN = set()

LETTERS = re.compile(r"[a-zāčēģīķļņšūž]+")
PREFIXES = ("aiz", "ap", "at", "ie", "iz", "no", "pa", "pār", "pie", "sa", "uz")
# The Latvian packs glossed in other languages, which have words the English one lacks (kilometrs, traktors, vagons).
OTHER_PACKS = ["lv-ru", "lv-pl", "lv-zh", "lv-fr", "lv-de", "lv-pt"]
DECLENSION = re.compile(r"declension-([1-6])")
# Common words no pack has, by hand, with their kind (nouns get their declension's forms as the packs' do).
WORDS = {
    "mīnus": "adv", "plus": "adv", "papildus": "adv", "papildu": "adj", "pretī": "adv", "aizvien": "adv", "tepat": "adv",
    "turpat": "adv", "labprāt": "adv", "toreiz": "adv", "tolaik": "adv", "citur": "adv", "vienreiz": "adv", "divreiz": "adv",
    "nākamreiz": "adv", "pirmkārt": "adv", "otrkārt": "adv", "galvenokārt": "adv", "tikmēr": "adv", "līdzās": "adv",
    "teju": "adv", "nupat": "adv", "savādāk": "adv", "pamazām": "adv", "otrādi": "adv", "nudien": "adv", "labdien": "intj",
    "hallo": "intj", "oho": "intj", "foto": "noun", "kino": "noun", "prasība": "noun", "tuvums": "noun", "periods": "noun",
    "sacensība": "noun", "transports": "noun",
}


def palatal(stem):
    """The stem with its last consonant softened, as in the genitive plural (brāļu, upju, siržu)."""
    for a, b in (("sn", "šņ"), ("zn", "žņ"), ("sl", "šļ"), ("zl", "žļ"), ("ln", "ļņ"), ("ll", "ļļ"), ("dz", "dž"),
                 ("st", "š"), ("t", "š"), ("d", "ž"), ("s", "š"), ("z", "ž"), ("c", "č"), ("n", "ņ"), ("l", "ļ"),
                 ("b", "bj"), ("p", "pj"), ("m", "mj"), ("v", "vj"), ("f", "fj")):
        if stem.endswith(a):
            return stem[:-len(a)] + b
    return stem


def noun_forms(word, declension=None):
    """A noun's regular case forms by its declension, or by its ending where the pack gives none."""
    if not LETTERS.fullmatch(word):
        return []
    if declension is None:
        declension = 2 if word.endswith("is") else 3 if word.endswith("us") else 1 if word.endswith(("s", "š")) else (
            4 if word.endswith("a") else 5 if word.endswith("e") else None)
    ending = {1: ("s", "š"), 2: ("is", "s"), 3: ("us",), 4: ("a",), 5: ("e",), 6: ("s",)}.get(declension, ())
    ending = next((e for e in ending if word.endswith(e)), None)
    s = word[:-len(ending)] if ending else ""
    # A stem of two letters makes others' spellings (ali of alus, saa of sals).
    if len(s) < 3:
        return []
    p = palatal(s)
    if declension == 1:
        return [s + e for e in ("a", "am", "u", "ā", "i", "iem", "us", "os")]
    if declension == 2 and ending == "is":
        # Some keep the plain stem (tēta, kaķa); the counts settle which.
        return [s + "im", s + "i", s + "ī"] + [x + e for x in dict.fromkeys((p, s))
                                               for e in ("a", "i", "u", "iem", "us", "os")]
    if declension == 2:
        return [s + "im", s + "i", s + "ī"] + [p + e for e in ("i", "u", "iem", "us", "os")]
    if declension == 3:
        return [s + e for e in ("um", "u", "ū", "i", "iem", "os")]
    if declension == 4:
        return [s + e for e in ("as", "ai", "u", "ā", "ām", "ās")]
    if declension == 5:
        return [s + e for e in ("es", "ei", "i", "ē", "ēm", "ēs")] + [p + "u"]
    return [s + e for e in ("ij", "i", "ī", "is", "īm", "īs")] + [p + "u"]


def adjective_forms(word):
    """An adjective's regular forms: indefinite, definite, comparative, superlative, and its adverbs (labi, labāk)."""
    if len(word) < 4 or not word.endswith(("s", "š")) or word.endswith(("is", "us")) or not LETTERS.fullmatch(word):
        return []
    s = word[:-1]
    indefinite = ("s", "a", "am", "u", "ā", "i", "iem", "us", "os", "as", "ai", "ām", "ās")
    definite = ("ais", "ā", "ajam", "o", "ajā", "ie", "ajiem", "os", "ajos", "ās", "ajai", "ajām", "ajās")
    out = [s + e for e in indefinite + definite]
    out += [s + "āk" + e for e in indefinite + definite] + ["vis" + s + "āk" + e for e in definite]
    return out + [s + "āk", "vis" + s + "āk"]


def verb_forms(verb):
    """A verb's regular forms by its class (2nd: veidot, runāt, vērtēt; 3rd: mainīt, mēģināt), negated too; or none."""
    if len(verb) < 5 or not verb.endswith(("ot", "āt", "ēt", "īt")) or not LETTERS.fullmatch(verb):
        return []
    long = verb[:-1]
    made = []

    def tenses(present, imperative, debitive):
        past = ("ju", "ji", "ja", "jām", "jāt", "šu", "si", "s", "sim", "siet", "sit", "tu", "dams", "jis", "jusi", "juši",
                "jušas", "jušo", "ts", "ta", "ti", "tas", "to", "tā", "tie", "tais", "šana", "šanas", "šanu", "šanā")
        return present + [long + e for e in past] + imperative + ["jā" + debitive]

    # The verbs in -ēt the English pack lacks are of the 2nd conjugation (garantēt, komentēt, ārstēt); its 3rd (redzēt,
    # gribēt, turēt) are few and in the pack.
    if verb.endswith(("ot", "āt", "ēt")) and not verb.endswith("ināt"):
        # 2nd conjugation: veidoju, veido, veidojam; runāju, runā.
        made += tenses([long + e for e in ("ju", "", "jam", "jat", "jot", "jošs", "jams")], [long + "jiet"], long)
    if verb.endswith(("ināt", "īt")):
        # 3rd conjugation: mainu, maina, mainām; mēģinu, mēģina.
        root = verb[:-2]
        made += tenses([root + e for e in ("u", "i", "a", "ām", "āt", "ot", "āms")], [root + "iet"], root + "a")
    return made + ["ne" + f for f in made if not f.startswith("jā")]


def forms_by_word(lemmas):
    """Each word's forms in the pack, with those of its forms that the pack has as words folded into it (ņemts of ņemt)."""
    out = defaultdict(set)
    for form, of in lemmas.items():
        for lemma in of:
            if lemma != form:
                out[lemma].add(form)
    return out


# A plain verb's endings and the reflexive verb's (mācu, mācos; mācīsim, mācīsimies), the longest first.
REFLEXIVE_ENDINGS = [("iet", "ieties"), ("sim", "simies"), ("siet", "sieties"), ("sit", "sities"), ("dams", "damies"),
                     ("ušas", "ušās"), ("uši", "ušies"), ("usi", "usies"), ("šu", "šos"), ("tu", "tos"), ("si", "sies"),
                     ("am", "amies"), ("ām", "āmies"), ("at", "aties"), ("āt", "āties"), ("ot", "oties"), ("is", "ies"),
                     ("u", "os"), ("i", "ies"), ("t", "ties"), ("s", "sies")]
# A declined participle (mācīts, mācītajam, gājušo), which the reflexive verb has none of in this shape; the conditional in
# -tu (mācītu) is none.
DECLINED = re.compile(r"(?:t|oš|am|dam|uš)"
                      r"(?:ais|ajam|ajā|ajiem|ajos|ajai|ajām|ajās|ie|iem|ai|am|ām|ās|as|os|us|o|u|ā|a|i|s)$")


def reflexive(f):
    """A plain verb's form made reflexive (mācu: mācos; māca: mācās; ņem: ņemas; runā: runājas; mācīšana: mācīšanās)."""
    if f.startswith("jā") and len(f) > 4:
        return {"jā" + r for r in reflexive(f[2:])}
    if DECLINED.search(f) and not f.endswith("tu") and not re.search(r"(dams|usi|uši|ušas)$", f):
        return set()
    if f.endswith(("šana", "šanas", "šanu", "šanā")):
        return {f[:f.rfind("šan")] + "šanās"}
    for a, b in REFLEXIVE_ENDINGS:
        if f.endswith(a):
            return {f[:-len(a)] + b}
    if f.endswith("a"):
        return {f + "s", f[:-1] + "ās"}
    if f.endswith(("ā", "ē", "o", "ī")):
        return {f + "jas", f + "jies"}
    if f[-1:] not in "aeiouāēīū":
        return {f + "as", f + "ies"}
    return set()


def reflexive_forms(verb, forms):
    """A reflexive verb's forms from its plain verb's (mācīt: mācos, nemācās, jāmācās)."""
    own = forms | {verb}
    out = set()
    for f in own:
        if f.startswith("ne") and not verb.startswith("ne") and f[2:] in own or len(f) <= 2:
            continue
        for r in reflexive(f):
            out.update((r,) if r.startswith("jā") else (r, "ne" + r))
    return out


def prefixed_forms(prefix, verb, forms):
    """A prefixed verb's forms from its verb's (no + ņēma, ne + no + ņem, jā + no + ņem)."""
    own = forms | {verb}
    out = set()
    for f in own:
        # The pack's table of a verb has some endings as forms (u, i, a of svērt); ej of iet and ēd of ēst are forms.
        if len(f) <= 2 and f[0] != verb[0] and verb[0] not in "aeiouāēīū":
            continue
        if f.startswith("jā") and not verb.startswith("jā"):
            out.add("jā" + prefix + f[2:])
        elif f.startswith("ne") and not verb.startswith("ne") and f[2:] in own:
            continue
        else:
            out.update((prefix + f, "ne" + prefix + f))
    return out


def load(db, pos, lemmas, common, dictionaries, word_pos):
    """Adds the reflexive and prefixed verbs, the other packs' words and the regular forms to the pack's (pos, lemmas, common)."""
    known = set(pos) | {form for form, of in lemmas.items() if of}
    pack_forms = forms_by_word(lemmas)

    def table(word):
        # A word's forms, and the forms of those of them the pack has as words of their own, folded into it (ņemts, neņemt).
        direct = pack_forms.get(word, set())
        return direct | {g for f in direct if f not in common for g in pack_forms.get(f, ())}

    def add(form, word):
        if form in SEEN and form not in known and word not in lemmas[form]:
            lemmas[form].append(word)
            return True
        return False

    def own(word, kind):
        return kind in pos.get(word, ()) and common.get(word) == word

    for word, kind in WORDS.items():
        # The pack has labdien only as a phrase, which is no word of the list.
        if not pos.get(word, set()) & word_pos and not lemmas.get(word):
            pos[word].add(kind)
            common[word] = word
    for name in OTHER_PACKS:
        path = os.path.join(dictionaries, f"{name}.sqlite")
        if not os.path.exists(path):
            continue
        other = sqlite3.connect(path)
        added = set()
        for word, word_lc, p in other.execute("SELECT word, word_lc, pos FROM entries"):
            if p not in word_pos or word != word_lc or not LETTERS.fullmatch(word_lc) or len(word_lc) < 2 or word_lc in known or (
                    word_lc in pos and p in pos[word_lc]):
                continue
            pos[word_lc].add(p)
            common.setdefault(word_lc, word_lc)
            added.add(word_lc)
        for form, lemma in other.execute("SELECT form_lc, lemma FROM forms"):
            if lemma in added:
                add(form, lemma)
        other.close()

    verbs = {}
    for word, kinds in sorted(pos.items()):
        if "verb" not in kinds or common.get(word) != word or not LETTERS.fullmatch(word) or len(word) < 3 or not word.endswith(
                ("t", "ties")):
            continue
        verbs[word] = table(word)
        # A verb with no table to speak of (zvanīt, labot, pasūtīt of the other packs; priecāties) gets its class's regular
        # forms.
        if len(verbs[word]) < 5:
            plain = word[:-4] + "t" if word.endswith("ties") else word
            regular = verb_forms(plain)
            regular = reflexive_forms(plain, set(regular)) if plain != word else set(regular)
            for f in sorted(regular):
                add(f, word)
            verbs[word] |= regular
    made_verbs = set()

    def make(word, forms):
        """Gives the forms to the verb word, made a word of the list where the pack has no such spelling."""
        # A spelling the pack has as another kind of word, or as a form, is left to it.
        if word in known and not own(word, "verb"):
            return False
        made = [f for f in sorted(forms) if add(f, word)]
        if made:
            made_verbs.add(word)
        if word not in known and (made or word in SEEN):
            pos[word].add("verb")
            common[word] = word
        return True

    # A reflexive verb gets its plain verb's forms made reflexive (justies: jūtos, jūties, jūtas of just), and then any verb,
    # plain or reflexive, its prefixed verbs' (pieteikties, nomierināties).
    for verb, forms in sorted(verbs.items()):
        if len(forms) >= 5 and verb.endswith("t"):
            word = verb[:-1] + "ties"
            made = reflexive_forms(verb, forms)
            if make(word, made):
                verbs[word] = verbs.get(word, set()) | made
    for verb, forms in sorted(verbs.items()):
        if len(forms) < 5:
            continue
        for prefix in PREFIXES:
            make(prefix + verb, prefixed_forms(prefix, verb, forms))

    declensions = defaultdict(set)
    for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos = 'noun'"):
        for s in json.loads(senses):
            declensions[word].update(int(m.group(1)) for t in s.get("tags", []) for m in [DECLENSION.fullmatch(t)] if m)
    for word, kinds in sorted(pos.items()):
        if common.get(word) != word:
            continue
        if "noun" in kinds:
            tagged = declensions.get(word, set())
            for f in noun_forms(word, next(iter(tagged)) if len(tagged) == 1 else None):
                add(f, word)
        if "adj" in kinds:
            for f in adjective_forms(word):
                add(f, word)
