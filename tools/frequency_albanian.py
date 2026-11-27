"""
Albanian corrections for tools/build_frequency_list.py.

The sq-en pack (Wiktionary) is small and its declension tables file real words under look-alikes: nga under ka
("ox", whose plural is qe), në under ne, liri under li, farë under fare, qesh under ka. Hand OVERRIDES put the
commonest of those back; ALBANIAN_KINDS and the own-forms rule in build_list keep the rest apart.

The pack also lacks many common words (i, pra, vërtet, problem) and forms (doli, vdiq, quhet, mira). Those are in
frequency_albanian.tsv (form, tab, word): the subtitles' forms the pack lacks down to rank 4,500, read by hand, and
below that the ones whose ending the pack's own tables give for a word of the pack. Forms written without ë or ç
(mire, cfare) are left out, as are names and English. simplemma is not used for Albanian: the table replaces it,
so the list is the same with or without it installed. The news blended in counts only forms the pack or the table
has: its others, guessed by their endings, were mostly names (Ramës, Kurtit) or words the pack lacks (kryeministri).

After the counting, fold leaves out what the list's far end filled up with: names and pieces of names (the news write them
capitalised), Gheg and archaic words, words counted for a form the table guessed, noise and vulgar words; and it folds
plurals and other forms counted as words into their word.
"""
import os
import re
from collections import defaultdict

_HERE = os.path.dirname(os.path.abspath(__file__))

# form -> words, from the table: a word of its own is listed as itself. Its rows are in the order of the subtitles' counts:
# the first TABLE_READ were read by hand (to rank 4,500), the others guessed by their ending (GUESSED).
TABLE, GUESSED = {}, set()
TABLE_READ = 974
with open(os.path.join(_HERE, "frequency_albanian.tsv"), encoding="utf-8") as _f:
    for _n, _line in enumerate(_f, 1):
        _form, _, _word = _line.rstrip("\n").partition("\t")
        if _form and _word:
            TABLE[_form] = [_word]
            if _n > TABLE_READ:
                GUESSED.add(_form)

HAND = {
    # Little words the pack ties to another word's table.
    "i": ["i"], "të": ["të"], "te": ["te"], "ti": ["ti"], "ke": ["kam"], "ka": ["kam"], "kem": ["kam"],
    "ne": ["ne"], "na": ["ne"], "neve": ["ne"], "nesh": ["ne"], "në": ["në"],
    "nga": ["nga"], "sa": ["sa"], "kush": ["kush"], "disa": ["disa"], "ca": ["ca"], "prej": ["prej"],
    "dhe": ["dhe"], "edhe": ["edhe"], "ende": ["ende"], "ja": ["ja"], "drejt": ["drejt"], "tani": ["tani"],
    "ime": ["im"], "që": ["që"], "qe": ["jam"], "qenë": ["jam"], "le": ["le"],
    # jetë is "life" and the subjunctive of jam (të jetë); its other forms are life's.
    "jetë": ["jam", "jetë"], "jeta": ["jetë"], "jetën": ["jetë"], "jetës": ["jetë"], "jete": ["jetë"], "jetët": ["jetë"],
    "jetëve": ["jetë"],
    # atë is "him, that" (ai's) and "father", whose other forms are at's.
    "atë": ["ai"], "at": ["at"], "ati": ["at"], "atit": ["at"], "atin": ["at"], "etër": ["at"], "etërit": ["at"],
    "etërve": ["at"], "etërish": ["at"],
    # Words of their own filed under a look-alike's table.
    "qesh": ["qesh"], "qetë": ["qetë"], "pi": ["pi"], "gji": ["gji"], "ide": ["ide"], "liri": ["liri"], "farë": ["farë"],
    "hënë": ["hënë"], "hëna": ["hënë"], "hënën": ["hënë"], "hënës": ["hënë"],
    "gju": ["gju"], "gjunjë": ["gju"], "gjunjët": ["gju"], "gjunjëve": ["gju"], "gjuri": ["gju"], "gjurin": ["gju"],
    "gjurit": ["gju"],
    "bletë": ["bletë"], "qepë": ["qepë"], "valë": ["valë"], "arë": ["arë"], "enë": ["enë"], "pallat": ["pallat"],
    "dhëmb": ["dhëmb"], "dhëmbë": ["dhëmb"], "dhëmbët": ["dhëmb"], "dhëmbi": ["dhëmb"], "dhëmbin": ["dhëmb"],
    "dhëmbëve": ["dhëmb"],
    "ves": ["ves"], "not": ["not"], "shaj": ["shaj"], "milje": ["milje"], "maj": ["maj"], "majit": ["maj"],
    "mori": ["marr"], "desh": ["dua"], "pe": ["shoh"], "vete": ["vete"], "veten": ["vete"], "vetja": ["vete"],
    "vajta": ["vete"], "vajti": ["vete"], "vajtur": ["vete"],
    # A form the list had as a word of its own.
    "do": ["dua"], "jep": ["jap"], "marrë": ["marr"], "din": ["di"], "del": ["dal"], "dalë": ["dal"], "ra": ["bie"],
    "vdekur": ["vdes"], "ec": ["eci"], "duk": ["dukem"], "mbes": ["mbet"], "yti": ["yt"], "jona": ["ynë"],
    "juaji": ["juaj"], "kalojë": ["kaloj"], "lerë": ["lë"], "pakë": ["pak"], "blerë": ["blej"], "humbas": ["humb"],
    "intereson": ["interesoj"], "interesoj": ["interesoj"],
    # Case and gender forms the pack gives entries of their own.
    "mua": ["unë"], "meje": ["unë"], "mu": ["unë"], "cila": ["cili"], "cilën": ["cili"], "cilat": ["cili"],
    "cilave": ["cili"], "dikë": ["dikush"], "dike": ["dikush"], "dika": ["dikush"], "dikën": ["dikush"],
    "zezë": ["zi"], "zeza": ["zi"], "zeze": ["zi"], "zezën": ["zi"], "zezat": ["zi"], "shokë": ["shok"],
    "thanë": ["them"], "pirë": ["pi"], "vënd": ["vend"], "vëndi": ["vend"], "vëndin": ["vend"], "vënde": ["vend"],
    "prova": ["provë"], "prove": ["provë"], "sill": ["sjell"], "morre": ["marr"],
    "kali": ["kalë"], "bote": ["botë"], "rradhë": ["radhë"], "rradha": ["radhë"], "rradhën": ["radhë"], "dhanë": ["jap"],
    "zëmër": ["zemër"], "zëmra": ["zemër"], "zëmrën": ["zemër"], "nenë": ["nënë"], "nena": ["nënë"], "macë": ["mace"],
    "ëngjëll": ["engjëll"], "dihet": ["di"], "dihej": ["di"], "përshëndetje": ["përshëndetje"], "moj": ["moj"],
    # Common words the pack has only as names.
    "goxha": ["goxha"], "guxim": ["guxim"], "guximi": ["guxim"], "guximin": ["guxim"], "mirëmbrëma": ["mirëmbrëma"],
    "lumturi": ["lumturi"], "krenari": ["krenari"], "qëndrim": ["qëndrim"], "qëndrimi": ["qëndrim"],
    "qëndrimin": ["qëndrim"], "lirim": ["lirim"], "lirimin": ["lirim"], "shkëlqim": ["shkëlqim"], "përparim": ["përparim"],
    "fatlum": ["fatlum"], "guximtar": ["guximtar"], "unik": ["unik"], "kristal": ["kristal"], "patriot": ["patriot"],
    "dashurie": ["dashuri"], "premto": ["premtoj"], "premton": ["premtoj"], "gjata": ["gjatë"],
    # The reflexive and passive u (u kthye) and the pronoun pairs ta and ma (ta jap, ma jep), not forms of unë and ai.
    "u": ["u"], "ta": ["ta"], "ma": ["ma"],
    # mbrëmje "evening" is a word of its own, not a form of mbrëmë "last night".
    "mbrëmje": ["mbrëmje"], "mbrëmja": ["mbrëmje"], "mbrëmjet": ["mbrëmje"],
    # datë "date", not daj "to divide"; lum "happy" (të lumtë), not lumë "river".
    "datë": ["datë"], "datën": ["datë"], "datës": ["datë"], "datat": ["datë"], "datave": ["datë"],
    "lum": ["lum"], "lumtë": ["lum"], "lumi": ["lumë"], "lumin": ["lumë"], "lume": ["lumë"], "bija": ["bijë"],
    # Spellings of other words' forms: jan of janë, morra and morri of mora and mori, kurr of kurrë, të gjesh of gjej.
    "jan": ["jam"], "morra": ["marr"], "morri": ["marr"], "morrën": ["marr"], "morrëm": ["marr"], "morrët": ["marr"],
    "kurr": ["kurrë"], "gjesh": ["gjej"],
    # The elided words, which the reader and the counts cut off at the apostrophe (s'kam, t'i, ç'ke, m'u).
    "s": ["nuk"], "t": ["të"], "ç": ["çfarë"], "m": ["më"],
    # Forms of words with entries of their own, filed under a look-alike.
    "qan": ["qaj"], "pika": ["pikë"], "lidhja": ["lidhje"], "enët": ["enë"], "tanë": ["ynë"], "lindja": ["lindje"],
    # Homographs whose commoner word the pack's table misses: të mi "my" (not mi "mouse"), të tu "your", e re "new"
    # (not re "cloud"), luan "plays" (not luan "lion"), të vijë "may come" (not vijë "line").
    "mi": ["im", "mi"], "tu": ["yt"], "re": ["ri"], "reja": ["ri"], "luan": ["luaj"], "vijë": ["vij"],
    "vijën": ["vijë"], "vijës": ["vijë"], "vijat": ["vijë"], "vijave": ["vijë"], "vijash": ["vijë"],
    "retë": ["re"], "reve": ["re"], "luanit": ["luan"], "luanët": ["luan"], "luanë": ["luan"], "luanëve": ["luan"],
    "luana": ["luan"], "luanësh": ["luan"], "luanve": ["luan"], "luani": ["luan", "luaj"], "luanin": ["luan", "luaj"],
    # Words of their own the pack files as another's form: gjithnjë beside gjithmonë, përse beside pse, meqenëse beside meqë.
    "gjithnjë": ["gjithnjë"], "përse": ["përse"], "meqenëse": ["meqenëse"],
    # Days of the week (e mërkurë, të dielën), which the pack lacks or files under a look-alike (enjtja under enjt "Jupiter").
    "mërkurë": ["mërkurë"], "mërkurën": ["mërkurë"], "mërkura": ["mërkurë"], "diel": ["diel"], "dielën": ["diel"],
    "diela": ["diel"], "enjtja": ["enjte"], "enjtën": ["enjte"],
    # Misspellings and forms filed under a look-alike: ëndër of ëndërr, arësye of arsye, thëni "say" of them, not thëri "nit".
    "ëndër": ["ëndërr"], "arësye": ["arsye"], "thëni": ["them"], "rrëndësi": ["rëndësi"], "krahën": ["krah"],
    "kembë": ["këmbë"],
}

OVERRIDES = {**TABLE, **HAND}

# Misspellings, dialect spellings, pieces and foreign words the pack has.
EXCLUDE = {
    "eshtë", "eshte", "eshta", "degjôj", "marre", "tun", "tuk", "tungë", "tye", "albanian", "forms", "nime", "nani",
    "ëm", "mo", "ngë", "nukë", "nëk", "çar", "ktu", "kah", "atu", "ay", "eti", "syresh", "shpi", "hide", "hidja", "tend",
    "ŋa", "umb", "um", "pro", "pron", "proj", "ska", "silli", "gjera", "gjerat", "gjerave", "gjerash", "boj", "çë",
    # Names and English (Dan, Dave, John, he), and be, mostly the news' BE (the EU).
    "dan", "dave", "dani", "dante", "xhon", "he", "be", "kame", "jani",
    # The days written with their article, which are hënë, premte, shtunë and mërkurë again.
    "e hënë", "e premte", "e shtunë", "e mërkurë",
}

# Words the pack files under their mediopassive or feminine (bashkoj under bashkohem, gjerman under gjermane), shown as
# the word itself.
DISPLAY = {"bashkohem": "bashkoj", "furnizohem": "furnizoj", "gjermane": "gjerman"}

# Forms sent to a word by hand above that stay with it, though the word's own spelling goes to another (retë stays with
# re "cloud", whose spelling re is mostly ri's).
STAY = {"vijën", "vijës", "vijat", "vijave", "vijash", "retë", "reve", "luanit", "luanët", "luanë", "luanëve", "luana",
        "luanësh", "luanve", "luani", "luanin"}


# After the counting (fold): a word's count is only its own uses.
# Words the news write capitalised (Blerim, Sokol, Shahini, Struga): names, or pieces of them, when the forms counted for
# the word are in lower case less than NAME_LOWER of at least NAME_NEWS times, or, for a word the pack also has as a name,
# less than NAMED_LOWER of at least NAMED_NEWS times. Not interjections, which start sentences (Hej, Alo).
NAME_LOWER, NAME_NEWS = 0.1, 20
NAMED_LOWER, NAMED_NEWS = 0.4, 5
# Words written capitalised that are words all the same (AIDS, Ramazani).
CAPITALISED_WORDS = {"aids", "sida", "ramazan", "dervish"}
# Names and pieces of names the subtitles write in lower case, so the news cannot tell them.
NAMES = {"blerim", "sokol", "mej", "lilë", "hut", "demet", "kurt", "lajlë", "lajle", "melek", "meleq"}
# The pack's senses tagged so (and not also Tosk or standard) are no standard Albanian: a word with no other sense is left
# out (kerr, ndih, gjem) unless the news write it at least DIALECT_NEWS times (zbraz, gatim: the tags are on the wrong
# senses).
DIALECT_TAGS = {"Gheg", "archaic", "obsolete", "dialectal", "regional", "Arbëresh", "Arvanitika"}
STANDARD_TAGS = {"Tosk", "standard"}
DIALECT_NEWS = 50
DIALECT_GLOSS = re.compile(r"\((?:Gheg|archaic|dialectal|obsolete|regional)\)|^(?:Gheg|dialectal|archaic|obsolete) (?:form|spelling)")
# A word counted for one form at least this share of its count, a form the pack has nowhere and the table guessed from its
# ending, is no word (harbor for harboj, vader for vade, përkundër for përkund, prani for praj), unless the form is the
# word with a case or gender ending (plaçkat of plaçkë, femërore of femëror).
GUESSED_SHARE = 0.8
ENDINGS = {"a", "e", "ë", "i", "u", "n", "s", "t", "in", "it", "un", "ut", "at", "et", "ët", "ve", "ën", "ës", "në", "së", "ja",
           "je", "ia", "të", "sh", "ash", "esh", "ësh", "ra", "ri", "rat", "rit"}
# Gheg, archaic and regional words the pack does not tag so (axhë, tesha, kerr, shakë), noise (KOK, rra, crank for
# carangth, shogun for shog, hangari for angari, asher for ashe) and vulgar words.
NOT_WORDS = {
    "axhë", "tesha", "dhez", "ase", "carangth", "kok", "rra", "ver", "putanë", "shog", "tamë", "rig", "pip", "angari",
    "kerr", "apë", "celik", "shpor", "ashe", "nes", "kith", "qek", "hum", "nëmë", "garth", "fell", "kath", "naze", "shakë",
    "tallë", "loke", "joshë", "cen", "ëndë", "thëri", "op", "gop", "bun", "timër", "tus", "kuak", "glasë", "dilet", "serë",
    "cagë", "bis", "zdryp", "grah", "nalt", "gore", "jes", "gjër", "kurrgjë", "tis", "pitas", "vol", "kaps", "byk", "curran",
}
# Plurals, other spellings and forms counted as words of their own, with the word they are (vathë "earrings", hallet
# "troubles", të hajmë "that we eat").
FOLD = {"vathë": "vath", "hallet": "hall", "ankoj": "ankohem", "hajmë": "ha", "djegje": "djegie", "kthjell": "kthjelloj"}
# Forms filed under a look-alike, with the word they are: palloi "the peacock", not palloj's.
MISFILED = {"palloi": "pallua"}


def _news_case(cache, corpora):
    """{form: (lower-case count, count)} in the cached Leipzig news, which keep the case text writes."""
    import io
    import tarfile
    case = {}
    for corpus in corpora:
        path = os.path.join(cache, f"leipzig-{corpus}.tar.gz")
        if not os.path.exists(path):
            continue
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) < 3 or not parts[2].isdigit():
                    continue
                word, n = parts[1], int(parts[2])
                low, total = case.get(word.lower(), (0, 0))
                case[word.lower()] = (low + (n if word == word.lower() else 0), total + n)
    return case


def _pack(dictionaries):
    """The pack's words with the tags of each sense (names apart), the words it has as names, and every spelling it has."""
    import json
    import sqlite3
    senses, named, spelled = defaultdict(list), set(), set()
    path = os.path.join(dictionaries, "sq-en.sqlite")
    if not os.path.exists(path):
        return senses, named, spelled
    db = sqlite3.connect(path)
    for word, kind, data in db.execute("SELECT word_lc, pos, senses FROM entries ORDER BY id"):
        spelled.add(word)
        if kind == "name":
            named.add(word)
            continue
        senses[word].extend(dict(s, kind=kind) for s in json.loads(data))
    spelled.update(f for (f,) in db.execute("SELECT form_lc FROM forms"))
    db.close()
    return senses, named, spelled


def _dialect(senses):
    def tagged(sense):
        tags = set(sense.get("tags", []))
        return tags & DIALECT_TAGS and not tags & STANDARD_TAGS or any(DIALECT_GLOSS.search(g) for g in sense.get("glosses", []))
    return bool(senses) and all(tagged(s) for s in senses)


def _declined(word, form):
    stems = [word] + ([word[:-1]] if word[-1:] in ("e", "ë") else [])
    return any(form.startswith(stem) and form[len(stem):] in ENDINGS for stem in stems)


def _move(forms, form, frm, to):
    share = forms[frm].pop(form, 0)
    if share:
        forms[to][form] = forms[to].get(form, 0) + share


def fold(scores, forms, dictionaries, cache, corpora):
    """
    After the counting: names, Gheg and archaic words, words the table guessed from a form and NOT_WORDS are left out; FOLD
    words go to the word they are a form of; a non-active verb (adresohem) keeps only its own forms and the participle both
    share (adresuar), the others going to its active verb (adreson of adresoj); MISFILED forms go to their word.
    """
    case = _news_case(cache, corpora)
    senses, named, spelled = _pack(dictionaries)
    for word, to in sorted(FOLD.items()):
        if word in scores and to in scores:
            for form in sorted(forms[word]):
                _move(forms, form, word, to)
            del scores[word]
    for word in sorted(scores):
        if word.endswith("ohem") and word[:-4] + "oj" in scores:
            active = word[:-4] + "oj"
            for form in sorted(forms[word]):
                if not form.startswith(word[:-2]) and not form.endswith("uar") and form in forms[active]:
                    _move(forms, form, word, active)
    for form, to in sorted(MISFILED.items()):
        if to in scores:
            for word in sorted(scores):
                if word != to and form in forms[word]:
                    _move(forms, form, word, to)
    for word in sorted(scores):
        counted = forms[word]
        whole = sum(counted.values())
        low = sum(case.get(f, (0, 0))[0] for f in counted)
        news = sum(case.get(f, (0, 0))[1] for f in counted)
        said = any(s["kind"] in ("intj", "particle") for s in senses.get(word, []))
        name = word not in CAPITALISED_WORDS and not said and (
            news >= NAME_NEWS and low < NAME_LOWER * news or word in named and news >= NAMED_NEWS and low < NAMED_LOWER * news)
        top = max(counted, key=lambda f: (counted[f], f)) if counted else word
        guessed = top != word and top in GUESSED and top not in HAND and top not in spelled and not _declined(word, top) and \
            whole and counted[top] >= GUESSED_SHARE * whole
        dialect = news < DIALECT_NEWS and _dialect(senses.get(word, []))
        if word in NOT_WORDS or word in NAMES or name or guessed or dialect:
            del scores[word]
    for word in scores:
        scores[word] = sum(forms[word].values())
