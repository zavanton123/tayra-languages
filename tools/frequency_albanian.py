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
"""
import os

_HERE = os.path.dirname(os.path.abspath(__file__))

# form -> words, from the table: a word of its own is listed as itself.
TABLE = {}
with open(os.path.join(_HERE, "frequency_albanian.tsv"), encoding="utf-8") as _f:
    for _line in _f:
        _form, _, _word = _line.rstrip("\n").partition("\t")
        if _form and _word:
            TABLE[_form] = [_word]

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
    # Days of the week (e mërkurë, të dielën), which the pack lacks.
    "mërkurë": ["mërkurë"], "mërkurën": ["mërkurë"], "mërkura": ["mërkurë"], "diel": ["diel"], "dielën": ["diel"],
    "diela": ["diel"],
}

OVERRIDES = {**TABLE, **HAND}

# Misspellings, dialect spellings, pieces and foreign words the pack has.
EXCLUDE = {
    "eshtë", "eshte", "eshta", "degjôj", "marre", "tun", "tuk", "tungë", "tye", "albanian", "forms", "nime", "nani",
    "ëm", "mo", "ngë", "nukë", "nëk", "çar", "ktu", "kah", "atu", "ay", "eti", "syresh", "shpi", "hide", "hidja", "tend",
    "ŋa", "umb", "um", "pro", "pron", "proj", "ska", "silli", "gjera", "gjerat", "gjerave", "gjerash", "boj", "çë",
    # Names and English (Dan, Dave, John, he), and be, mostly the news' BE (the EU).
    "dan", "dave", "dani", "dante", "xhon", "he", "be", "kame",
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
