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
- some common words no pack has are added by hand (WORDS: mīnus, papildus, labprāt, citur, prasība, transports, izmērs);
- and so are some verbs no pack conjugates, or conjugates as another (VERBS: sēsties, vērst, tiekties), whose prefixed
  verbs these are too (apsēsties, pievērst, attiekties).

A made form goes to its word only where the English pack spells no word or form so, and only if the counts have it (SEEN).
A verb no pack has is made only where the counts have forms of its own, and enough of them beside those it would share with
another word (sēdēties, made of sēdēt, has none beside sēsties's sēžos, sēdies); a form two made verbs take goes to the
commoner (pievēršu of pievērst, not of pievērt).
"""
import json
import os
import re
import sqlite3
from collections import defaultdict

# The forms the counts have, and how common each is, set by the list builder before the pack is read.
SEEN = set()
COUNTS = {}

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
    # Nouns whose cases the packs give to a verb or to another noun: izmēra and pārskatu to izmērīt and pārskatīt, jubileju to
    # jubilejs, uzkodas to uzkosties, apmaksai to apmaksāt, piekļuvi to piekļūt.
    "izmērs": "noun", "pārskats": "noun", "jubileja": "noun", "uzkoda": "noun", "atliekas": "noun", "izpēte": "noun",
    "ieskats": "noun", "nopelns": "noun", "atlase": "noun", "apskate": "noun", "piekļuve": "noun", "pārstrāde": "noun",
    "iemaksa": "noun", "ieskaite": "noun", "piesaiste": "noun", "atmaksa": "noun", "ieguve": "noun", "apsargs": "noun",
    "uzskaite": "noun", "pieraksts": "noun", "saskare": "noun", "atsauce": "noun", "apmaksa": "noun", "nostāsts": "noun",
    "padeve": "noun", "apkope": "noun", "iesaiste": "noun", "apgāde": "noun", "sasaiste": "noun", "atskats": "noun",
    "atruna": "noun", "piemaksa": "noun", "skauts": "noun", "apmāns": "noun", "pakausis": "noun", "izdiena": "noun",
    "apskats": "noun",
}
# The forms of a noun of WORDS that its ending does not give (atliekas, a plural).
NOUN_FORMS = {"atliekas": ["atlieku", "atliekām", "atliekās"]}
# Verbs no pack conjugates, or conjugates as another (vērst as vērt), whose prefixed verbs are common (apsēsties, pievērst,
# attiekties, apgulties, nomākt). Without them their forms went to a verb made from a look-alike (apsēdēties of sēdēt,
# pievērt, attikties of tikt, apgulēties of gulēt, nomācīt of mācīt). Their tables are theirs, the pack's too (vērst's vēršu
# and vērs, which it gives vērt alone); mākt's forms that are mācīt's or mācīties's (mācu, māca, mācies) are left out.
VERBS = {
    "sēsties": "sēžos sēdies sēžas sēžamies sēžaties sēdos sēdās sēdāmies sēdāties sēdīšos sēdīsies sēdīsimies sēdīsieties "
               "sēdieties sēstos sēžoties sēdīšoties sēdusies sēdušies sēdušās sēzdamies jāsēžas sēšanās",
    "vērst": "vēršu vērs vērš vēršam vēršat vērsu vērsa vērsām vērsāt vērsīšu vērsīsi vērsīs vērsīsim vērsīsiet vērsiet "
             "vērstu vēršot vērsusi vērsuši vērsušas vērsts vērsta vērsti vērstas vērsto vērstā vēršams vēršama vērsdams "
             "jāvērš vēršana vēršanas vēršanu",
    "vērsties": "vēršos vērsies vēršas vēršamies vēršaties vērsos vērsās vērsāmies vērsāties vērsīšos vērsīsies vērsīsimies "
                "vērsīsieties vērsieties vērstos vēršoties vērsusies vērsušies vērsušās vērsdamies jāvēršas vēršanās",
    "tiekties": "tiecos tiecies tiecas tiecamies tiecaties tiecās tiecāmies tiecāties tiekšos tieksies tieksimies tieksieties "
                "tiecieties tiektos tiecoties tiekusies tiekušies tiekušās tiekdamies jātiecas tiekšanās",
    "gulties": "guļos gulies guļas guļamies guļaties gūlos gūlās gūlāmies gūlāties gulšos gulsies gulsimies gulsieties "
               "gulieties gultos guļoties gulšoties gulusies gulušies gulušās guldamies jāguļas gulšanās",
    "mākt": "māc mācam mācat mākšu māksi māks māksim māksiet māciet māktu mākusi mākuši mākts mākta māktas mākti "
            "mācošs mācošā mācošais jāmāc mākšana",
    "mākties": "mācas mācamies mācaties mācās mākšos māksies māksimies māktos mākusies mākušies "
               "mākdamies jāmācas mākšanās",
}
# Forms a pack's table lacks, which a verb made from a look-alike took: bīties's participles (nobijusies of nobūties).
MORE_FORMS = {"bīties": "bijusies bijušies bijušās nebijusies nebijušies"}
# A verb whose present is another's (teikt's saku, saka are sacīt's): its prefixed and reflexive verbs have it too
# (piesakās of pieteikties, izsakās of izteikties), which went to piesacīties and izsacīties.
PRESENT_OF = {"teikt": "sacīt"}
# How common a made verb's own forms (no other word's) must be beside the rest it would take: sēdēties has almost none of
# sēdējās, sēdēsies beside sēsties's sēžos and sēdies, nobūties none beside nobīties's nobijies, nobijusies.
EVIDENCE = 0.05
# How common, beside the commonest, a made verb must be to share a form with another made verb (pievērt does not share
# pievēršu with pievērst, nor novelt novēlu with novēlēt).
SHARE = 0.5


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


def reflexive(f, own=frozenset()):
    """A plain verb's form made reflexive (mācu: mācos; māca: mācās; ņem: ņemas; runā: runājas; mācīšana: mācīšanās)."""
    if f.startswith("jā") and len(f) > 4:
        return {"jā" + r for r in reflexive(f[2:], {g[2:] for g in own if g.startswith("jā")})}
    if DECLINED.search(f) and not f.endswith("tu") and not re.search(r"(dams|usi|uši|ušas)$", f):
        return set()
    if f.endswith(("šana", "šanas", "šanu", "šanā")):
        return {f[:f.rfind("šan")] + "šanās"}
    for a, b in REFLEXIVE_ENDINGS:
        if f.endswith(a):
            return {f[:-len(a)] + b}
    if f.endswith("a"):
        # māca: mācās, koda: kodās; kodas, dzijas and uzkodas are nouns.
        return {f[:-1] + "ās"}
    if f.endswith(("ā", "ē", "o", "ī")):
        return {f + "jas", f + "jies"}
    if f[-1:] not in "aeiouāēīū":
        # The second person where the third ends otherwise (tiec of tiek: tiecies, not tiecas, which is tiekties's).
        if f.endswith("c") and f[:-1] + "k" in own or f.endswith("dz") and f[:-2] + "g" in own:
            return {f + "ies"}
        if f.endswith("k") and f[:-1] + "c" in own or f.endswith("g") and f[:-1] + "dz" in own:
            return {f + "as"}
        return {f + "as", f + "ies"}
    return set()


def reflexive_forms(verb, forms):
    """A reflexive verb's forms from its plain verb's (mācīt: mācos, nemācās, jāmācās)."""
    own = forms | {verb}
    out = set()
    for f in own:
        if f.startswith("ne") and not verb.startswith("ne") and f[2:] in own or len(f) <= 2:
            continue
        for r in reflexive(f, own):
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


def infinitive(word):
    """How common a verb's infinitive is, negated too: what a made verb's rivals for a form are weighed by."""
    return COUNTS.get(word, 0.0) + COUNTS.get("ne" + word, 0.0)


def evidenced(word, forms, takers, elsewhere):
    """
    Whether the counts have a made verb's own forms (no other word's, nor a commoner made verb's, no verbal noun nor
    conditional in -tos, which is a participle's locative too), and enough of them beside the rest it would take.
    """
    mine = infinitive(word)
    own = rest = 0.0
    for f in forms:
        if f not in takers:
            continue
        count = COUNTS.get(f, 0.0)
        if elsewhere(f) or any(infinitive(r) >= mine for r in takers[f] if r != word) or "šan" in f or f.endswith("tos"):
            rest += count
        else:
            own += count
    # One the counts lack the infinitive of shares no form (piekust, of piekusis: uzdrošināt's uzdrošinās is uzdrošināties's).
    return own > 0 and own >= EVIDENCE * rest and (mine > 0 or not rest)


def load(db, pos, lemmas, common, dictionaries, word_pos):
    """Adds the reflexive and prefixed verbs, the other packs' words and the regular forms to the pack's (pos, lemmas, common)."""
    known = set(pos) | {form for form, of in lemmas.items() if of}
    pack_forms = forms_by_word(lemmas)

    def table(word):
        # A word's forms, and the forms of those of them the pack has as words of their own, folded into it (ņemts, neņemt);
        # spelled as they are, not as another verb the pack files there (braukt of nesākt, dzelt of nepirkt).
        # Nor a word the pack's table has as an ending (iet and is of svērt, which made saiet a form of sasvērt), nor the reflexive
        # verb it has as a plain verb's form (doties of dot, which made nodoties a form of nodot).
        direct = {f for f in pack_forms.get(word, set()) if (f[0] == word[0] or f.startswith("ne") or f not in pos)
                  and not (f.endswith("ties") and not word.endswith("ties"))}
        return direct | {g for f in direct if f not in common for g in pack_forms.get(f, ())
                         if g[:2] in (f[:2], word[:2]) or g[2:4] == word[:2]}

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

    for word, forms in VERBS.items():
        if not pos.get(word, set()) & word_pos - {"verb"}:
            pos[word].add("verb")
            common[word] = word
            for f in forms.split():
                if f in SEEN and word not in lemmas[f]:
                    lemmas[f].append(word)
    for word, forms in MORE_FORMS.items():
        for f in forms.split():
            add(f, word)
    # The nouns' and adjectives' regular forms, given them once the verbs are made; a made verb shares them (izmēra of izmērs).
    declensions = defaultdict(set)
    for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos = 'noun'"):
        for s in json.loads(senses):
            declensions[word].update(int(m.group(1)) for t in s.get("tags", []) for m in [DECLENSION.fullmatch(t)] if m)
    declined = []
    for word, kinds in sorted(pos.items()):
        if common.get(word) != word:
            continue
        if "noun" in kinds:
            tagged = declensions.get(word, set())
            declined += [(f, word) for f in NOUN_FORMS.get(word) or noun_forms(word, next(iter(tagged)) if len(tagged) == 1 else None)]
        if "adj" in kinds:
            declined += [(f, word) for f in adjective_forms(word)]

    verbs = {}
    for word, kinds in sorted(pos.items()):
        if "verb" not in kinds or common.get(word) != word or not LETTERS.fullmatch(word) or len(word) < 3 or not word.endswith(
                ("t", "ties")):
            continue
        verbs[word] = set(VERBS[word].split()) if word in VERBS else table(word) | set(MORE_FORMS.get(word, "").split())
        # A verb with no table to speak of (zvanīt, labot, pasūtīt of the other packs; priecāties) gets its class's regular
        # forms.
        if len(verbs[word]) < 5:
            plain = word[:-4] + "t" if word.endswith("ties") else word
            regular = verb_forms(plain)
            regular = reflexive_forms(plain, set(regular)) if plain != word else set(regular)
            for f in sorted(regular):
                add(f, word)
            verbs[word] |= regular
    for word, other in PRESENT_OF.items():
        verbs[word] |= {f for f in verbs.get(other, ()) if not f.startswith((other[:-2], "ne" + other[:-2]))}
    made_verbs = {}

    def make(word, forms):
        """Takes the forms for the verb word, made a word of the list below where the pack has no such spelling."""
        # A spelling the pack has as another kind of word, or as a form, is left to it.
        if word in known and not own(word, "verb"):
            return False
        made_verbs.setdefault(word, set()).update(forms)
        return True

    # A reflexive verb gets its plain verb's forms made reflexive (justies: jūtos, jūties, jūtas of just), and then any verb,
    # plain or reflexive, its prefixed verbs' (pieteikties, nomierināties).
    for verb, forms in sorted(verbs.items()):
        if len(forms) >= 5 and verb.endswith("t") and verb[:-1] + "ties" not in VERBS:
            word = verb[:-1] + "ties"
            made = reflexive_forms(verb, forms)
            if make(word, made):
                verbs[word] = verbs.get(word, set()) | made
                # The plain verb's future is spelled so too (mēģinās, sazinās): the two take it as rivals.
                future = {verb[:-1] + "s", "ne" + verb[:-1] + "s"} & made
                if future:
                    make(verb, future)
    for verb, forms in sorted(verbs.items()):
        if len(forms) < 5:
            continue
        for prefix in PREFIXES:
            make(prefix + verb, prefixed_forms(prefix, verb, forms))

    # A verb the packs lack is made only where the counts have forms of it that no other word, nor a commoner made verb, takes
    # (apsēdēties has none: apsēdies is apsēsties's), enough beside the forms it would share.
    takers = defaultdict(set)
    for word, forms in made_verbs.items():
        for f in forms:
            if f in SEEN and f not in known:
                takers[f].add(word)
    # Taken by a word or the forms given so far (VERBS, WORDS, the other packs', the short tables' regular ones).
    elsewhere = {f for f in takers if f in pos or lemmas.get(f)} | {f for f, _ in declined}
    # The other packs' verbs (iesākt, uzkost) are words whatever their forms.
    kept = {word: forms for word, forms in made_verbs.items()
            if word in known or pos.get(word) or evidenced(word, forms, takers, elsewhere.__contains__)}
    # A form more than one of them takes is left to those at least half as common as the commonest, by their infinitive and
    # the forms they alone take (novēlu of novēlēt, not of novelt; pievēršu of pievērst, not of pievērt; mēģinās of mēģināt).
    alone = defaultdict(float)
    for f, rivals in takers.items():
        if len(rivals & kept.keys()) == 1 and f not in elsewhere and "šan" not in f and not f.endswith("tos"):
            alone[next(iter(rivals & kept.keys()))] += COUNTS.get(f, 0.0)
    strength = {w: infinitive(w) + alone[w] for w in kept}
    for f in list(takers):
        rivals = takers[f] & kept.keys()
        best = max((strength[w] for w in rivals), default=0.0)
        takers[f] = {w for w in rivals if strength[w] >= SHARE * best}
    for word, forms in sorted(kept.items()):
        made = [f for f in sorted(forms) if (f not in takers or word in takers[f]) and add(f, word)]
        if word not in known and (made or word in SEEN):
            pos[word].add("verb")
            common[word] = word

    for f, word in declined:
        add(f, word)
