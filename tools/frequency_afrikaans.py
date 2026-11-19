"""
Afrikaans corrections for tools/build_frequency_list.py.

The indefinite article is written 'n, which the reader reads as one word with its apostrophe. Leipzig's corpora cut the
straight apostrophe off it (' + n) and FrequencyWords drops it (n), while the curly ’n stays whole: all of them are counted
as 'n, since a bare n is almost never anything else in Afrikaans text. The reader writes ’ ‘ ´ ` as the apostrophe.

The English Wiktionary has few Afrikaans words (about 6,000) and lacks many of the commonest (om, na, weer, begin, dus,
neem, terug, gebeur, nodig, mekaar). The FreeDict packs (af-en, af-de) add the words it lacks, with the regular forms
Afrikaans makes of them (forms): the plural in -s, -e or -'s (with a short vowel's consonant doubled: kop, koppe), the
diminutive in -(t)jie, -ie, -pie or -etjie and its plural, an adjective's attributive -e and its -er and -ste, a verb's ge-
participle and -ende. A made form is the word's only where no pack word or form is spelled so.

The pack's tables and its "alternative form of" senses also file words under others: the personal pronouns under each
other (hy and sy under hom, my under ek), moet "must" under met "with", ver "far" under vir "for", dag "day" under dink.
OVERRIDES puts them back; EXCLUDE leaves out names and foreign words the counts bring in.

Even with them, the packs lack hundreds of the commonest words (af, stil, stel, ooit, heel, gelede, daarvan, sulke) and
forms (gewone, gestel, gebiede). Those are in frequency_afrikaans.tsv (form, tab, word; a word of its own is listed as
itself): the counts' forms no pack has down to rank 4,500, read by hand, names, English, Dutch and abbreviations left out.
Its words take the made forms as the packs' do. The days, the months and the language names are written capitalised
(Maandag, Januarie, Engels), as the pack has them: they are words, not names.
"""
import gzip
import os
import re
import shutil
import sqlite3
import tempfile

APOSTROPHES = str.maketrans("’‘´`", "''''")
LETTERS = re.compile(r"[a-zà-öø-ÿ'ŉ]+")
VOWELS = set("aeiouyáéíóúàèìòùâêîôûäëïöü")
PACKS = ("af-en-freedict", "af-de-freedict")
# Prefixes after which a verb takes no ge- (begin, verstaan, ontmoet, herhaal, erken, gebruik).
UNPREFIXED = ("be", "ver", "ont", "her", "er", "ge")


def normalised(form):
    """A form as the reader reads it."""
    form = form.translate(APOSTROPHES)
    return "'n" if form == "n" else form


def _short_stem(word):
    """Whether a word ends in one short vowel and one consonant, which doubles before an ending (kop, koppe; man, manne)."""
    return (len(word) >= 3 and word[-1] not in VOWELS and word[-1] not in "wy" and word[-2] in "aeiou"
            and word[-3] not in VOWELS)


def forms(word):
    """The regular forms of a word, whatever kind it is; text decides which of them occur."""
    made = {word + "s", word + "e", word + "'s", word + "er", word + "ste", word + "ers"}
    if _short_stem(word):
        double = word + word[-1]
        made |= {double + "e", double + "er", double + "ers", double + "ie", double + "ies", double + "etjie", double + "etjies"}
    last = word[-1]
    if last in "dt":
        tail = "jie"
    elif last in "gk":
        tail = "ie"
    elif last == "m":
        tail = "pie"
    else:
        tail = "tjie"
    made |= {word + tail, word + tail + "s"}
    if word.endswith("heid"):
        made.add(word[:-4] + "hede")
    if not word.startswith(UNPREFIXED) and len(word) > 2:
        made.add("ge" + word)
    made |= {word + "ende", word + "de"}
    made.discard(word)
    return made


def _open_pack(dictionaries, name):
    """A FreeDict pack, from its .sqlite or by unpacking its .sqlite.gzip; (None, None) when there is neither."""
    path = os.path.join(dictionaries, f"{name}.sqlite")
    if os.path.exists(path):
        return sqlite3.connect(path), None
    packed = path + ".gzip"
    if not os.path.exists(packed):
        return None, None
    handle, temp = tempfile.mkstemp(suffix=".sqlite")
    with os.fdopen(handle, "wb") as out, gzip.open(packed, "rb") as source:
        shutil.copyfileobj(source, out)
    return sqlite3.connect(temp), temp


def load(pos, lemmas, common, dictionaries):
    """Adds the FreeDict packs' words the English one lacks, with their made forms, and the words given by hand."""
    known = set(pos) | set(lemmas)
    added = set()
    for name in PACKS:
        db, temp = _open_pack(dictionaries, name)
        if db is None:
            continue
        for word, word_lc in db.execute("SELECT word, word_lc FROM entries"):
            word_lc = word_lc.translate(APOSTROPHES)
            if word != word_lc or word_lc in known or len(word_lc) < 2 or not LETTERS.fullmatch(word_lc):
                continue
            pos[word_lc].add("noun")
            common.setdefault(word_lc, word_lc)
            added.add(word_lc)
        db.close()
        if temp:
            os.remove(temp)
    known |= added
    for word in sorted(added):
        for form in forms(word):
            if form not in known:
                lemmas[form].append(word)
    hand = set()
    for word in WORDS:
        if word not in pos:
            pos[word].add("noun")
            common.setdefault(word, word)
            hand.add(word)
    known |= hand
    for word in sorted(hand):
        for form in forms(word):
            if form not in known:
                lemmas[form].append(word)
    for word, written in CAPITALISED.items():
        pos[word].add("noun")
        common[word] = written


_HERE = os.path.dirname(os.path.abspath(__file__))

# form -> word, from the table; a word of its own is listed as itself.
TABLE = {}
with open(os.path.join(_HERE, "frequency_afrikaans.tsv"), encoding="utf-8") as _f:
    for _line in _f:
        _form, _, _word = _line.rstrip("\n").partition("\t")
        if _form and _word:
            TABLE[_form] = _word

# Words no pack has, which text needs.
WORDS = set(TABLE.values())

# The days, months and languages, which Afrikaans capitalises.
CAPITALISED = {word.lower(): word for word in (
    "Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrydag", "Saterdag", "Sondag", "Januarie", "Februarie", "Maart",
    "April", "Mei", "Junie", "Julie", "Augustus", "September", "Oktober", "November", "Desember", "Afrikaans", "Engels",
    "Kersfees", "Frans", "Duits", "Brits", "Amerikaans", "Europees", "Nederlands", "Russies", "Grieks", "Romeins",
    "Italiaans", "Chinees", "Spaans", "Christelik", "Christen", "Afrikaner",
)}
# Their forms, as the packs lack them.
CAPITALISED_FORMS = {
    "franse": "frans", "duitse": "duits", "britse": "brits", "amerikaanse": "amerikaans", "europese": "europees",
    "nederlandse": "nederlands", "russiese": "russies", "griekse": "grieks", "romeinse": "romeins",
    "italiaanse": "italiaans", "chinese": "chinees", "spaanse": "spaans", "christelike": "christelik",
    "christene": "christen", "afrikaners": "afrikaner", "sondae": "sondag", "maandae": "maandag",
}

HAND = {
    # The personal pronouns, each a word as in Dutch (my, jou, hy, haar are not ek's, jy's, hom's and sy's).
    "ek": ["ek"], "ek's": ["ek"], "my": ["my"], "jy": ["jy"], "jy's": ["jy"], "jy't": ["jy"], "djy": ["jy"], "jou": ["jou"],
    "djou": ["jou"], "hy": ["hy"], "hy's": ["hy"], "hy't": ["hy"], "sy": ["sy"], "sy's": ["sy"],
    "sy't": ["sy"], "syn": ["sy"], "haar": ["haar"], "hare": ["haar"],
    # The present and past of wees, as Dutch files is and was under zijn; ware is waar's.
    "is": ["wees"], "issie": ["wees"], "was": ["wees"], "ware": ["waar"], "gewas": ["gewas"],
    # Words of their own filed under a look-alike: moet "must" under met, ver "far" under vir, toe under tot, dag "day"
    # under dink, staat "state" under staan, ome "uncles" under om.
    "moet": ["moet"], "moes": ["moet"], "moete": ["moet"], "moets": ["moet"], "moenie": ["moenie"], "mee": ["mee"],
    "ver": ["ver"], "verre": ["ver"], "verste": ["ver"], "verder": ["verder"], "toe": ["toe"], "toe't": ["toe"],
    "dag": ["dag"], "dae": ["dag"], "daggie": ["dag"], "dog": ["dink"], "gedag": ["dink"], "staat": ["staat"], "state": ["staat"], "ome": ["oom"],
    "later": ["later"], "laaste": ["laaste"], "ouers": ["ouers"], "geval": ["geval"], "gebied": ["gebied"],
    "sake": ["saak"], "selfde": ["selfde"], "helde": ["held"], "mate": ["maat"], "kwaai": ["kwaai"], "gerus": ["gerus"],
    "geveg": ["geveg"], "gevegte": ["geveg"], "koningin": ["koningin"], "koninginne": ["koningin"],
    "partytjie": ["partytjie"], "partytjies": ["partytjie"], "liedjie": ["liedjie"], "liedjies": ["liedjie"],
    "bakkie": ["bakkie"], "bakkies": ["bakkie"], "koppie": ["koppie"], "koppies": ["koppie"], "hys": ["hys"],
    "gehys": ["hys"], "grawe": ["grawe"], "gegrawe": ["grawe"], "pouse": ["pouse"], "magie": ["magie"],
    "gevoel": ["voel", "gevoel"], "gevoelens": ["gevoel"], "nader": ["naby", "nader"], "regter": ["regter"],
}

OVERRIDES = {**{form: [word] for form, word in TABLE.items()}, **{form: [word] for form, word in CAPITALISED_FORMS.items()},
             **HAND}

# Names, English and pieces the made forms or the packs bring in.
EXCLUDE = {"anna", "la", "russe", "peter", "kin", "agte", "perre", "mese", "omer", "omers", "oms"}
