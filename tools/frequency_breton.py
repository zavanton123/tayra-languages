"""
Breton for tools/build_frequency_list.py: the regular forms the dictionary pack lacks.

The br-fr pack (Wiktionary) has a conjugation table for some seventy verbs only; for the other thirteen hundred it has the
verbal noun and at most a few forms, and for most nouns no plural. So the Breton Wikipedia's divizet, meneg, krogas,
embannas, mennozhioù were no word's and fell out of the list. made_forms gives each pack verb that has no table of its own
the regular forms of its stem (the verbal noun less -añ, -iñ, -out, -at, -al, -el, -et, -ek, -in; BRETON_STEMS for the
verbs whose stem changes its vowel: kregiñ, krog-), each noun its plurals in -où and -ioù, each adjective its comparative.
The list builder files such a form under its word only when the corpus has it, mostly in lower case, the pack has it as no
word and no word's form, and the pack's links and mutations give it no other word (build_list, breton_paradigms); and
tools/courses/br/extra_forms.tsv is made from the same forms.

The br-fr pack also lacks whole words, among the commonest of the Breton Wikipedia: strollad, abalamour, lodenn, memes,
implij, pennañ, bepred, kudenn, peurrest. So once the pack's own words have their forms (breton_relink, breton_paradigms),
more_words adds those of the br-en pack (the English Wiktionary), with their kinds and forms, and FreeDict's (br-fr-freedict,
Geriadur Tomaz), which has no kinds: a verb where it ends as a verbal noun and is glossed with a French infinitive, else a
noun. Neither takes a spelling the pack's words have (vezo stays bezañ's, not mezo's) nor is a regular form of them (dimezet,
reoù: FreeDict lists participles as words). corpus_verbs adds the verbs no pack has whose participle the corpus writes
(implijout of implij, with implijet), and own_words the words the corpus writes often, in lower case and in no gloss of the
packs (sistem, relijiel). The list builder then files their regular and mutated forms as it does the pack's (breton_more).

The pack files forms of bezañ under its French gloss (vije under "être"): they go to bezañ (french_lemmas). WORDS gives the
forms and spellings no pack files under their word (ennañ, hiriv, laret).
"""
import gzip
import json
import os
import re
import shutil
import sqlite3
import tempfile

# A verb with this many forms in the pack has its table; the forms it lacks are irregular ones the rules would get wrong.
FULL_TABLE = 20

# The verbal noun's endings, the longest first, and the stem left without them (diviz of divizout, deskriv of deskrivañ).
VERBAL_NOUN_ENDINGS = ("añ", "iñ", "out", "at", "al", "el", "et", "ek", "in")

# The stems of the verbs whose vowel changes from the verbal noun (kregiñ: krog, kroget, krogas).
BRETON_STEMS = {
    "kregiñ": "krog", "stekiñ": "stok", "leskiñ": "losk", "lemel": "lam", "dilemel": "dilam", "desevel": "desav",
    "adsevel": "adsav", "envel": "anv", "adenvel": "adanv", "lesenvel": "lesanv", "herzel": "harz", "tevel": "tav",
    "genel": "gan", "gervel": "galv", "derc'hel": "dalc'h", "kendelc'her": "kendalc'h", "kleñvel": "klañv", "skeiñ": "sko",
    "teiñ": "to", "goleiñ": "golo", "gounit": "gounez", "dizoleiñ": "dizolo", "distreiñ": "distro", "emdreiñ": "emdro",
}
# Verbs on ober's irregular stems (adober: adgraet, adreas), whose forms the rules would get wrong.
IRREGULAR = {"adober", "dizober"}

# The personal endings: the present (diviz, divizan, divizez, divizomp, divizit, divizont), the imperfect (divize), the
# preterite (divizas, divizjont), the future (divizo), the two conditionals (divizfe, divizje), the imperative (divizit) and
# the past participle (divizet). Not the impersonal present and imperfect: -er and -ed are mostly nouns (embanner
# "publisher", sened "senate").
VERB_ENDINGS = ("", "an", "ez", "omp", "it", "ont",
                "en", "es", "e", "emp", "ec'h", "ent",
                "is", "jout", "as", "jomp", "joc'h", "jont", "jod",
                "in", "i", "o", "imp", "ot", "int", "or",
                "fen", "fes", "fe", "femp", "fec'h", "fent", "fed",
                "jen", "jes", "je", "jemp", "jec'h", "jent", "jed",
                "et")
# The ones the list files a verb's form by: the third persons, the past participle and the imperative's -it. Wikipedia hardly
# writes the first and second persons, and the spellings it has with those endings are mostly other words (trees, kampi).
LIST_VERB_ENDINGS = ("", "e", "ent", "as", "jont", "o", "ont", "fe", "fent", "je", "jent", "it", "et")
NOUN_ENDINGS = ("où", "ioù")
ADJECTIVE_ENDINGS = ("oc'h",)

# A verb's stem alone is its form when the corpus has the verb's other spellings at least this share as often.
BARE_SHARE = 0.5
# A form of a verb the pack files as a spelling of another verb filed under it in turn (embann, embanniñ) is the verb's only
# if the verb is this many times as common.
SPELLING_SHARE = 3
# Spellings the rules make that are other words: English (trees of treañ) and Latin ones.
NOT_FORMS = {"trees", "treis", "manifesto", "regis", "reges"}

# The packs whose words br-fr lacks, the first first: br-en has kinds and forms, FreeDict words alone.
PACKS = ("br-en", "br-fr-freedict")
# The pack's forms filed under a French gloss instead of the Breton verb.
FRENCH_LEMMAS = {"être": "bezañ"}
# FreeDict's words as the list has them: letters, the apostrophe of c'h and a hyphen between parts.
LETTERS = re.compile(r"[^\W\d_]+(?:['-][^\W\d_]+)*")
# A French infinitive (filmer, perdre, s'affoler), FreeDict's gloss of a verb.
FRENCH_INFINITIVE = re.compile(r"(?:se |s')?[a-zàâçéèêëîïôûùüœ-]+(?:er|ir|re|oir)(?: \(.*\))?")

# A word no pack has is a word of its own (sistem, relijiel, marevezh) when the corpus writes it this many times or more,
# OWN_LOWER of the time in lower case (not a name), with OWN_LETTERS letters or more and a vowel (not km, mm), no hyphen (mare-se
# is mare + se), and no word of the packs' French or English glosses (the, and, des, tout: titles and quotes).
OWN_COUNT = 20
OWN_LOWER = 0.6
OWN_LETTERS = 3
VOWELS = set("aeiouyâêîôûàèéìòùü")
# The words of the packs' glosses, French and English.
GLOSS_WORDS = set()
GLOSS_WORD = re.compile(r"[^\W\d_]+")
# Hand words and spellings no pack has: a form -> its word, a word of its own listed as itself. The personal forms of e
# "in" (ennañ, enni), the dialect spellings hiriv (hiziv), laret (lavaret), lakat (lakaat), braz (bras), araok (a-raok),
# delc'her (derc'hel) and deut (dont), kaout's ac'h, dalv, talvezout's "is worth", and brevet "broken", breviñ's
# participle, which the pack has as an adjective.
WORDS = {"ennon": "e", "ennout": "e", "ennañ": "e", "enni": "e", "ennomp": "e", "ennoc'h": "e", "enno": "e", "ennoñ": "e",
         "hiriv": "hiziv", "laret": "lavaret", "lakat": "lakaat", "braz": "bras", "araok": "a-raok", "delc'her": "derc'hel",
         "deut": "dont", "ac'h": "kaout", "dalv": "talvezout", "brevet": "breviñ"}
# Words no pack has that texts write with a capital, so own_words leaves them out: Bretoned, the Britons.
CAPITALISED = {"bretoned": "noun"}
# Foreign words and abbreviations the corpus writes in lower case: von (German names), della, ent, niv (niverenn).
NOT_WORDS = {"von", "della", "ent", "niv"}

# A made form counts only if the corpus writes it in lower case at least this share of the time (not Jezuz, Sened).
LOWER_SHARE = 0.5


def verb_stem(verb):
    """The stem a regular verb's forms are made on: diviz of divizout, muzuli of muzuliañ, degas of degas."""
    if verb in BRETON_STEMS:
        return BRETON_STEMS[verb]
    for ending in VERBAL_NOUN_ENDINGS:
        if verb.endswith(ending) and len(verb) - len(ending) >= 2:
            return verb[:-len(ending)]
    return verb


def made_forms(pos, table, verb_endings=VERB_ENDINGS):
    """
    The regular forms of the pack's words, as (made, bare, verbal): made is form -> the words it may be, bare the forms
    that are a verb's stem alone (meneg, diviz), which are often a noun the pack lacks (kamp of kampiñ, sakr of sakrañ),
    verbal the forms of verbs. [pos] gives each word its kinds, [table] each word the forms the pack files under it,
    [verb_endings] the endings a verb's forms are made with. A verb with a table of its own (bezañ, ober, gwelet: FULL_TABLE
    forms) gets none, nor one on a stem of two letters (suiñ, goiñ: sujed and gofes are nouns).
    """
    made, bare, verbal = {}, set(), set()

    def add(form, word):
        if form != word and form not in NOT_FORMS:
            made.setdefault(form, set()).add(word)
            return form
        return None

    for word, kinds in pos.items():
        if not word.replace("'", "").isalpha():
            continue
        stem = verb_stem(word)
        if "verb" in kinds and len(stem) > 2 and len(table.get(word, ())) < FULL_TABLE and word not in IRREGULAR:
            verbal.update(add(stem + ending, word) for ending in verb_endings)
            if stem != word:
                bare.add(stem)
        if "noun" in kinds:
            for ending in NOUN_ENDINGS:
                add(word + ending, word)
        if "adj" in kinds:
            for ending in ADJECTIVE_ENDINGS:
                add(word + ending, word)
    return made, bare & set(made), verbal - {None}


def corpus_counts(path):
    """
    How often Leipzig's corpus at [path] writes each lowercased word, in all its spellings and as a share of them in lower
    case: (counts, lower-case shares). Read once per path.
    """
    if path not in _CORPUS:
        import io
        import tarfile
        lower, every = {}, {}
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[2].isdigit():
                    word = parts[1].replace("ʼ", "'").replace("’", "'")
                    key = word.lower()
                    every[key] = every.get(key, 0) + int(parts[2])
                    if word == key:
                        lower[key] = lower.get(key, 0) + int(parts[2])
        _CORPUS[path] = every, {w: lower.get(w, 0) / n for w, n in every.items() if n}
    return _CORPUS[path]


_CORPUS = {}


def lower_shares(path):
    """How often Leipzig's corpus at [path] writes each lowercased word in lower case, as a share of all its spellings."""
    return corpus_counts(path)[1]


def apostrophe(text):
    return text.replace("\u02bc", "'").replace("\u2019", "'")


def _open_pack(dictionaries, name):
    """A pack, from its .sqlite or by unpacking its .sqlite.gzip; (None, None) when there is neither."""
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


# The soft mutation as (written, radical), the commonest: of two spellings each the other's mutation (kêriadenn,
# gêriadenn), the one is the other's soft mutation (ur gêriadenn).
SOFT = (("g", "k"), ("d", "t"), ("b", "p"), ("c'h", "g"), ("w", "gw"), ("v", "b"), ("v", "m"), ("z", "d"))


def _soft_radicals(form):
    return {radical + form[len(written):] for written, radical in SOFT if form.startswith(written) and len(form) > len(written)}


def _mutated_of(form, others, mutations):
    """Whether [form] is a mutated spelling of one of [others], where either may be the other's: the soft mutation decides."""
    return any(r in others and (r in _soft_radicals(form) or form not in _soft_radicals(r))
               for r in _unmutated(form, mutations))


def _unmutated(form, mutations):
    return {radical + form[len(written):] for written, radical in mutations if form.startswith(written) and len(form) > len(written)}


def french_lemmas(pos, lemmas):
    """Gives the forms the pack files under a French gloss (vije of "être") the Breton verb, unless they are words."""
    for form, of in lemmas.items():
        if form not in pos and any(l in FRENCH_LEMMAS for l in of):
            lemmas[form] = [FRENCH_LEMMAS.get(l, l) for l in of]


def load(dictionaries):
    """
    Reads the other packs: br-en's words with their kinds and the forms it files under its words, FreeDict's words with the
    kind their glosses give, and the words of br-fr's and br-en's glosses (GLOSS_WORDS). [dictionaries] is their folder.
    """
    words, forms = {}, {}
    db, temp = _open_pack(dictionaries, "br-fr")
    if db is not None:
        _read_glosses(db)
        db.close()
    db, temp = _open_pack(dictionaries, PACKS[0])
    if db is not None:
        _read_glosses(db)
        for word, word_lc, kind in sorted(db.execute("SELECT word, word_lc, pos FROM entries")):
            word, word_lc = apostrophe(word), apostrophe(word_lc)
            if word == word_lc and _listable(word_lc):
                words.setdefault(word_lc, set()).add(kind)
        for form, lemma in sorted(db.execute("SELECT form_lc, lemma FROM forms")):
            form, lemma = apostrophe(form), apostrophe(lemma).lower()
            if _listable(form) and form != lemma:
                forms.setdefault(form, []).append(lemma)
        db.close()
        if temp:
            os.remove(temp)
    db, temp = _open_pack(dictionaries, PACKS[1])
    if db is not None:
        glosses = {}
        for word, word_lc, senses in sorted(db.execute("SELECT word, word_lc, senses FROM entries")):
            word, word_lc = apostrophe(word), apostrophe(word_lc)
            if word == word_lc and _listable(word_lc) and word_lc not in words:
                glosses.setdefault(word_lc, []).extend(g for s in json.loads(senses) for g in s.get("glosses", [])[:1])
        db.close()
        if temp:
            os.remove(temp)
        for word, of in sorted(glosses.items()):
            verb = word.endswith(VERBAL_NOUN_ENDINGS) and any(FRENCH_INFINITIVE.fullmatch(g) for g in of)
            words[word] = {"verb" if verb else "noun"}
    for word, kind in CAPITALISED.items():
        words.setdefault(word, {kind})
    MORE_WORDS.clear()
    MORE_WORDS.update(words)
    MORE_FORMS.clear()
    MORE_FORMS.update(forms)


# The other packs' words (word -> its kinds) and br-en's forms (form -> its words), as load reads them.
MORE_WORDS = {}
MORE_FORMS = {}


def _read_glosses(db):
    for (senses,) in db.execute("SELECT senses FROM entries"):
        for sense in json.loads(senses):
            for gloss in sense.get("glosses", []):
                GLOSS_WORDS.update(GLOSS_WORD.findall(gloss.lower()))


def _listable(word):
    """Whether a word may be a word of the list: two letters or more, and no short word written onto the next (d'am)."""
    return len(word) > 1 and bool(LETTERS.fullmatch(word)) and "'" not in word.replace("c'h", "")


def more_words(pos, lemmas, common, word_pos, placed, made, mutations):
    """
    Adds the other packs' words (MORE_WORDS) to (pos, common), with br-en's forms of them: those no spelling of the list
    has yet ([placed]: it is a word, or its count goes to one), that are no regular form or bare stem of a word ([made],
    regular's) or of another of them, and no mutated spelling of a word. Returns them.
    """
    added = {}
    for word, kinds in sorted(MORE_WORDS.items()):
        kinds = kinds & word_pos
        if not kinds or word in pos or placed(word) or word in made or word in NOT_WORDS:
            continue
        if any(placed(r) for r in _unmutated(word, mutations)):
            continue
        added[word] = kinds
    # Nor a regular form of another of them (diazezet of diazezañ).
    theirs, bare, _ = made_forms(added, {})
    added = {w: k for w, k in added.items() if w not in set(theirs) - bare}
    for word, kinds in sorted(added.items()):
        pos[word].update(kinds)
        common[word] = word
    for form, of in sorted(MORE_FORMS.items()):
        if form not in pos and not placed(form):
            words_of = [w for w in of if w in added]
            if words_of:
                lemmas[form] = words_of
    return set(added)


def corpus_verbs(freq, counts, lowercase, pos, lemmas, common, is_word, placed, excluded, mutations):
    """
    Adds the verbs no pack has whose participle the corpus writes and whose stem is a word (implijout of implij, with
    implijet; staliañ of stal, with staliet) or whose participle is common and no word's (kondaoniñ, with kondaonet), both
    mostly in lower case, the commonest of a stem's verbal nouns: [freq] the counts the list is made of, [counts] and
    [lowercase] corpus_counts'. Returns them.
    """
    best = {}
    for form in sorted(freq):
        if form in pos or lemmas.get(form) or form in excluded or form in NOT_WORDS or not form.endswith(VERBAL_NOUN_ENDINGS) or (
                lowercase.get(form, 0) < LOWER_SHARE):
            continue
        stem = verb_stem(form)
        participle = stem + "et"
        # A participle (implijet, kroget) is spelled as a verbal noun in -et.
        if stem == form or participle == form or lowercase.get(participle, 0) < LOWER_SHARE:
            continue
        # The stem of a verb in -iañ keeps its i (staliañ, staliet), the word it is made on not (stal).
        if is_word(stem) or stem.endswith("i") and is_word(stem[:-1]) or (
                counts.get(participle, 0) >= OWN_COUNT and not placed(participle)):
            if stem not in best or (freq[form], form) > (freq[best[stem]], best[stem]):
                best[stem] = form
    # Not a verb mutated (gejañ of kejañ): breton_mutated files it under the other.
    verbs = set(best.values())
    verbs = {v for v in verbs if not any(placed(r) for r in _unmutated(v, mutations)) and not _mutated_of(v, verbs, mutations)}
    for form in sorted(verbs):
        pos[form].add("verb")
        common[form] = form
    return verbs


def regular(pos, common, word_pos, tables):
    """
    The regular forms of the words in (pos, common), made_forms' on [tables] (the pack's forms of each word), but not a
    verb's stem alone: one breton_paradigms left free is mostly a noun of its own (film of filmañ).
    """
    words = {w: k for w, k in pos.items() if w in common and k & word_pos}
    made, bare, _ = made_forms(words, tables)
    return set(made) - bare


def own_words(counts, lowercase, pos, lemmas, common, placed, made, excluded, overrides, mutations):
    """
    Adds the words no pack has that the corpus writes as words of their own (OWN_COUNT, OWN_LOWER), and files under each
    its plurals in -où and -ioù and, after -er, -our and -ad, -ien. Not a regular form of a word ([made]: produet), nor one
    of them or of a spelling the list has mutated (gêriadenn of kêriadenn) or in the plural (studierien of studier,
    darempredoù of darempred, doueed of doue), which go to it. [counts] and
    [lowercase] are corpus_counts', [placed] tells whether a spelling's count goes to a word already. Returns them.
    """
    def free(form):
        return (form not in pos and not placed(form) and form not in excluded and form not in overrides
                and form not in GLOSS_WORDS and form not in made and lowercase.get(form, 0) >= OWN_LOWER)

    def endings(word):
        return NOUN_ENDINGS + (("ien",) if word.endswith(("er", "our", "ad")) else ())

    def singular(word):
        """The spelling a plural is of that the list has or finds (darempredoù of darempred, doueed of doue, dileuridi of
        dileuriad), or None."""
        for ending in ("où", "ioù", "ien", "ed", "idi"):
            if not word.endswith(ending):
                continue
            bases = [word[:-len(ending)] + tail for tail in (("iad", "ad") if ending == "idi" else ("",))]
            for base in bases:
                if (base in found or placed(base)) and (ending in endings(base) or ending in ("ed", "idi") and (
                        base in pos or base in found)):
                    return base
        return None

    found = {w for w, n in counts.items() if n >= OWN_COUNT and free(w) and len(w) >= OWN_LETTERS and VOWELS & set(w)
             and LETTERS.fullmatch(w) and "-" not in w and "'" not in w.replace("c'h", "") and w not in NOT_WORDS}
    # A plural of a spelling the list has already goes where that spelling does.
    plural = {w: singular(w) for w in sorted(found) if singular(w)}
    # A spelling of one of those mutated (gatoliked of katoliked, of katolik) is breton_mutated's to file.
    def mutated(word):
        return any(placed(r) or singular(r) for r in _unmutated(word, mutations)) or _mutated_of(word, found, mutations)

    words = {w for w in found - set(plural) if not mutated(w)}
    # A plural whose singular is no word after all (a mutated spelling) is one itself.
    for form, base in sorted(plural.items()):
        if base not in words and not placed(base):
            del plural[form]
            if not mutated(form):
                words.add(form)
    for word in sorted(words):
        pos[word].add("noun")
        common[word] = word
    for form, base in sorted(plural.items()):
        lemmas[form] = [base] if base in words or base in pos else list(lemmas[base])
    for word in sorted(words):
        for ending in endings(word):
            form = word + ending
            if form in counts and free(form):
                lemmas[form] = [word]
    return words


def regular_of_added(pos, common, added):
    """
    Takes out of (pos, common) the [added] words that are another added word's regular form, a verb's stem alone aside
    (staliet of staliañ, sevenet of seveniñ: FreeDict has both), for breton_paradigms to file them under it. Returns them.
    """
    made, bare, _ = made_forms({w: pos[w] for w in added}, {})
    out = {w for w in added if w in made and w not in bare and made[w] - {w}}
    for word in out:
        del pos[word]
        common.pop(word, None)
    return out
