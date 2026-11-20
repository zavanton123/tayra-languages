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
"""

# A verb with this many forms in the pack has its table; the forms it lacks are irregular ones the rules would get wrong.
FULL_TABLE = 20

# The verbal noun's endings, the longest first, and the stem left without them (diviz of divizout, deskriv of deskrivañ).
VERBAL_NOUN_ENDINGS = ("añ", "iñ", "out", "at", "al", "el", "et", "ek", "in")

# The stems of the verbs whose vowel changes from the verbal noun (kregiñ: krog, kroget, krogas).
BRETON_STEMS = {
    "kregiñ": "krog", "stekiñ": "stok", "leskiñ": "losk", "lemel": "lam", "dilemel": "dilam", "desevel": "desav",
    "adsevel": "adsav", "envel": "anv", "adenvel": "adanv", "lesenvel": "lesanv", "herzel": "harz", "tevel": "tav",
    "genel": "gan", "gervel": "galv", "derc'hel": "dalc'h", "kendelc'her": "kendalc'h", "kleñvel": "klañv", "skeiñ": "sko",
    "teiñ": "to", "goleiñ": "golo", "dizoleiñ": "dizolo", "distreiñ": "distro", "emdreiñ": "emdro",
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


def lower_shares(path):
    """How often Leipzig's corpus at [path] writes each lowercased word in lower case, as a share of all its spellings."""
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
    return {w: lower.get(w, 0) / n for w, n in every.items() if n}
