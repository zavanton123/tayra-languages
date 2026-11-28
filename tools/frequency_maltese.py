"""
Maltese words and forms the English pack lacks, for the frequency list.

The English pack (10,700 entries) has few of the loanwords Maltese text is full of (student, grupp, kumpanija, sindku,
trasport, mowbajl), no plural for a fifth of its nouns (persuna has no persuni), no table for the verbs it gives only as
another's form (staqsa of saqsa: no staqsiet), and none of the forms with a pronoun or the negative written onto them
(qalilhom, staqsietu, għadhom, hawnx). A fifth of the counted text was no word or form of it. So:

- the other packs' words come in where the English pack has no word or form spelled so (OTHER_PACKS), with their forms;
- a noun or adjective of the packs with no forms of its own gets the regular ones of a loanword (persuna: persuni; membru:
  membri; post: postijiet; privat: privata, privati), and a verb in -a, or a perfect in -a the pack files as another verb's
  form (staqsa), the regular ones of its kind (staqsiet, staqsew, jistaqsi);
- a form that is a word's or form's with a pronoun or the negative written onto it (qal + ilhom, għad + hom, hawn + x) is
  that word's;
- and the words no pack has are found in the counts themselves (induce): verbs by their imperfect (jinkludi, tinkludu:
  inkluda), and nouns written often enough, with their regular forms; a word written in capitals where no sentence starts
  is a name (Malta, Muscat, Ġesù), and one counted mostly in sentences the pack knows few words of is English (the, of).

A made form goes to its word only where no pack spells a word or form so, and only if the counts have it (SEEN). Each one
written with an i before two consonants (istudenti of studenti, after l-) goes with it, as maltese_generated_forms has it.
"""
import functools
import heapq
import json
import os
import re
import sqlite3
from collections import defaultdict

# The forms the counts have, by their share of the text, set by the list builder before the pack is read.
SEEN = {}
# How often each piece of the counted text is written (SEEN_IN), in a sentence most of whose words the pack has
# (AMONG_MALTESE), and where no sentence starts (WRITTEN), there in lowercase (LOWER): maltese_counts tallies them.
SEEN_IN = defaultdict(int)
AMONG_MALTESE = defaultdict(int)
WRITTEN = defaultdict(int)
LOWER = defaultdict(int)
# And how often it is written before a hyphen that is no article's (kontro-eżami, Kon-Katidral).
HYPHENED = defaultdict(int)
# How often a capitalised piece is written after another and a space (George Bush: AFTER["george", "bush"]), and how often
# next to text left damaged (gÄ§andha's andha, after the mending).
AFTER = defaultdict(int)
DAMAGED = defaultdict(int)

LETTERS = re.compile("[a-zàèìòùċġħż]+")
VOWELS = set("aeiouàèìòù")
OTHER_PACKS = ["mt-zh", "mt-pl", "mt-ru"]
# A word is a name when it is written in lowercase less than this share of the times no sentence starts with it (malta 0.00,
# president 0.21, segretarju 0.16, imperu 0.19 as titles), and English when fewer than this share of its uses are in
# sentences the pack knows half the words of (the 0.44, of 0.42, film 0.85).
LOWER_SHARE = 0.1
MALTESE_SHARE = 0.75
# And English when English text has it this many times as often as these (time 44, team 17, website 5.6; not test 1.4,
# student 1.8, online 4.3, which Maltese writes as English does), or it is spelled with letters Maltese has not (c, y) or ends
# in -ing (upgrading).
ENGLISH_RATIO = 5
NOT_MALTESE = re.compile("[cy]|ing$")
# A word found in the counts is written bare at least this share of the text.
INDUCED_SHARE = 2e-6
# And a verb found by its forms has at least this many of them, imperfects and others.
INDUCED_VERB_FORMS = 3
# A word found in the counts is written bare at least 1/HEAD_SHARE as often as its commonest form.
HEAD_SHARE = 5

# The pronouns written onto a word, and the negative: a possessive after a noun (rasu, darhom, sħabi), an object or indirect
# object after a verb (qatlu, qalilhom, jgħidli, staqsieh), either after another word (għadhom, minnhom, fuqha).
POSSESSIVE = ("i", "ek", "ok", "k", "u", "h", "ha", "na", "kom", "hom")
OBJECT = ("ni", "ek", "ok", "k", "u", "h", "ha", "na", "kom", "hom", "li", "lek", "lok", "lu", "lha", "lna", "lkom", "lhom",
          "ili", "ilek", "ilu", "ilha", "ilna", "ilkom", "ilhom", "ih")
SUFFIXES = sorted(set(POSSESSIVE) | set(OBJECT), key=lambda s: (-len(s), s))

# Everyday words the rules leave out, by their kind: parti "part", which the pack files under parta "to depart" (its
# overrides send parti and partijiet here); tali "such" and nomina "appointment", which would be ta + li and nomi + na.
WORDS = {"parti": "noun", "tali": "adj", "nomina": "noun"}
# Forms the rules miss, by their word: x'inhu "what is", m'hu (mhux) of huwa; ismu "his name" of isem; tlett, which the
# pack files under tila; ijiem "days" after l-; għaddej "passing", a participle the pack lacks.
FORMS = {
    "inhu": "huwa", "inhi": "hija", "inhuma": "huma", "mhu": "huwa", "mhix": "hija", "mhumiex": "huma",
    **{f: "isem" for f in ("ismu", "ismek", "ismi", "isimha", "isimhom", "isimkom", "isimna")},
    **{f: "ħajja" for f in ("ħajtu", "ħajti", "ħajtek", "ħajjitha", "ħajjithom", "ħajjitna", "ħajjitkom")},
    "tlett": "tlieta", "ijiem": "jum", "mhemmx": "hemm", "mhemm": "hemm", "mhi": "hija", "qegħda": "qiegħed",
    "qiegħdin": "qiegħed", **{f: "għadda" for f in ("għaddej", "għaddejja", "għaddejjin")},
    # ħa "took" and ilu "it has been" before a pronoun (ħadha, eħodha "take it", ilni "I have been").
    **{f: "ħa" for f in ("ħadha", "ħadhom", "ħadni", "ħadna", "ħadek", "ħadkom", "eħodha", "eħodhom", "eħodni")},
    **{f: "ilu" for f in ("ilni", "ilek", "ilha", "ilna", "ilkom", "ilhom")},
    # The imperfect of kell "to have" with its pronouns (jkolli, ikollok), which the pack has as ikoll alone.
    **{p + "koll" + e + x: "kell" for p in "jtni" for e in ("i", "ok", "u", "ha", "na", "kom", "hom") for x in ("", "x")},
}
# Names and English and Italian the counts give as words (Times of Malta, the Labour Party's Union, club, per, dei).
NOT_WORDS = {
    "times", "union", "unions", "air", "club", "per", "dei", "staff", "mass", "von", "dal", "don", "ben", "van", "dil", "pied",
    "inter", "pre", "anti", "super", "mobile", "station", "television", "metropolitan", "speaker", "tour", "leg", "legs",
    "football", "stadium", "worker", "workers", "penalties", "bars", "euro", "www", "gonzipn", "fknk", "nru", "dro", "dis",
    "mel", "hom", "ijiet", "fini", "joni", "nifisha", "nfisha",
    # And spellings without a letter (andu of għandu, afna of ħafna, kas of każ).
    "andu", "andho", "afna", "kas", "enerali",
}
# The letters typed for Maltese's marked ones in text written without them (ghandu, hafna, zgur), which make look-alikes.
MARKS = {"h": "ħ", "z": "ż", "c": "ċ", "g": "ġ"}
ACCENTED = {"a": "à", "e": "è", "i": "ì", "o": "ò", "u": "ù"}
UNMARK = str.maketrans({"ħ": "h", "ż": "z", "ċ": "c", "ġ": "g", "à": "a", "è": "e", "ì": "i", "ò": "o", "ù": "u"})
# The endings a word with a pronoun written onto it takes no more pronouns after (kellhom, not kellhomhom).
TAKEN = ("hom", "kom", "ha", "na", "ni", "li", "lha", "lna", "lek", "hiex")
# Little words that take no pronoun (sa "until", se "will", ma "not").
BARE = {"sa", "se", "ma", "da", "xi", "u", "jew", "li", "ta'"}


def counted(form):
    return SEEN.get(form, 0.0)


@functools.lru_cache(maxsize=None)
def english(form):
    """How common [form] is in English, as a share of its words, or 0 without wordfreq."""
    try:
        import wordfreq
    except ImportError:
        return 0.0
    return wordfreq.word_frequency(form, "en")


@functools.lru_cache(maxsize=None)
def italian(form):
    """How common [form] is in Italian, as a share of its words, or 0 without wordfreq."""
    try:
        import wordfreq
    except ImportError:
        return 0.0
    return wordfreq.word_frequency(form, "it")


def name_or_english(form):
    """Whether the counts write [form] as a name, or it is English (see LOWER_SHARE)."""
    if WRITTEN.get(form, 0) and LOWER.get(form, 0) < LOWER_SHARE * WRITTEN[form]:
        return True
    if SEEN_IN.get(form, 0) and AMONG_MALTESE.get(form, 0) < MALTESE_SHARE * SEEN_IN[form]:
        return True
    return bool(NOT_MALTESE.search(form)) or english(form) >= ENGLISH_RATIO * counted(form)


def unprothetic(form):
    """The spellings [form] is with an i or u before two consonants (iżvilupp: żvilupp; ikollu: jkollu; uħud: wħud)."""
    if len(form) < 4 or form[0] not in "iu":
        return []
    return [f for f in (form[1:], "j" + form[1:], "w" + form[1:]) if prothetic(f) == form]


def prothetic(form):
    """[form] with the i texts write before its two first consonants (istudenti, ikollu of jkollu, uħud of wħud), or None."""
    letters = form.replace("għ", "\x01")
    if len(letters) >= 2 and letters[0] not in VOWELS | {"\x01"} and letters[0].isalpha() and (
            letters[1] == "\x01" or letters[1].isalpha() and letters[1] not in VOWELS):
        return "i" + form[1:] if form[0] == "j" else "u" + form[1:] if form[0] == "w" else "i" + form
    return None


def noun_forms(word):
    """A loanword noun's regular forms: plurals in -i, -ijiet, -jiet and -s (studenti, postijiet, partijiet, plejers)."""
    if not LETTERS.fullmatch(word) or len(word) < 3:
        return []
    last = word[-1]
    if last in "au":
        return [word[:-1] + "i", word[:-1] + "ijiet"]
    if last == "i":
        return [word + "jiet"]
    if last in "àèòù":
        return [word[:-1] + {"à": "a", "è": "e", "ò": "o", "ù": "u"}[last] + "jiet"]
    if last in VOWELS:
        return []
    return [word + "i", word + "ijiet", word + "s"]


def adjective_forms(word):
    """A loanword adjective's regular forms: feminine in -a, plural in -i (privat: privata, privati; storiku: storika)."""
    if not LETTERS.fullmatch(word) or len(word) < 3:
        return []
    if word.endswith("u"):
        return [word[:-1] + "a", word[:-1] + "i"]
    if word[-1] in VOWELS:
        return []
    return [word + "a", word + "i", word + "in"]


def verb_forms(verb):
    """
    A verb in -a's regular forms, of the weak verbs (staqsa: staqsiet, staqsew, staqsejt, jistaqsi, jistaqsu) and the
    loanwords (inkluda: inkludiet, jinkludi; ikkonferma: ikkonfermat, jikkonferma), each with its negative; or none.
    """
    if not verb.endswith("a") or len(verb) < 4 or not LETTERS.fullmatch(verb):
        return []
    s = verb[:-1]
    perfect = [s + e for e in ("iet", "ew", "ejt", "ejna", "ejtu", "at", "aw", "ajt", "ajna", "ajtu")]
    # The imperfect's prefix takes an i before two consonants (jistaqsi), and stands alone before a vowel (jinkludi).
    stems = [s] if s[0] in VOWELS else [s, "i" + s]
    imperfect = [p + t + e for p in "jtn" for t in stems for e in ("i", "u", "a", "aw")]
    made = perfect + imperfect
    return made + [f + "x" for f in made + [verb]]


def load(pos, lemmas, common, dictionaries, word_pos, overrides, excluded):
    """Adds the other packs' words, the regular and suffixed forms and the words the counts show to (pos, lemmas, common)."""
    def known(form):
        # The article and the prepositions joined to it are counted without their hyphen too (mill, tal).
        return form in pos or bool(lemmas.get(form)) or form in overrides or form + "-" in overrides or form in excluded

    def unmarked(form):
        """Whether [form] is a word or form of the pack typed without its marks (ghal, bhal, kaz, zona)."""
        if "gh" in form:
            return True
        slots = [i for i, c in enumerate(form) if c in MARKS]
        if not slots or len(slots) > 6:
            return False
        for marked in range(1, 1 << len(slots)):
            letters = list(form)
            for bit, i in enumerate(slots):
                if marked >> bit & 1:
                    letters[i] = MARKS[letters[i]]
            if known("".join(letters)):
                return True
        return False

    def free(form):
        """Whether [form] is counted, no pack's word or form, and no name, English or look-alike."""
        return counted(form) > 0 and not known(form) and not ({form} | set(unprothetic(form))) & NOT_WORDS and not (
            name_or_english(form)) and not unmarked(form)

    def add(form, word):
        """Files [form] under [word] where nothing spells it, and its spellings with and without an i before two consonants."""
        for f in [form, prothetic(form)] + unprothetic(form):
            if f and free(f):
                lemmas[f].append(word)

    def is_word(w):
        return bool(pos.get(w, set()) & word_pos) and common.get(w) == w

    for word, kind in sorted(WORDS.items()):
        pos[word].add(kind)
        common[word] = word
    for form, word in sorted(FORMS.items()):
        add(form, word)

    # The other packs' words, with their forms; not one spelled as another's without its marks (kaz of każ).
    others, their_forms = {}, []
    for name in OTHER_PACKS:
        path = os.path.join(dictionaries, f"{name}.sqlite")
        if not os.path.exists(path):
            continue
        other = sqlite3.connect(path)
        for word, word_lc, p in other.execute("SELECT word, word_lc, pos FROM entries ORDER BY id"):
            # The Chinese pack's glossary gives no kind: a word in -a whose imperfects the counts have more than its plurals is a
            # verb (inkluda: jinkludi), any other a noun.
            kind = p if p != "unknown" else "verb" if word.endswith("a") and sum(
                counted(f) for f in verb_forms(word) if f[0] in "jtn") > sum(counted(f) for f in noun_forms(word)) else "noun"
            if kind not in word_pos or word != word_lc or not LETTERS.fullmatch(word) or len(word) < 3 or known(word) or (
                    word in others) or word in NOT_WORDS or name_or_english(word) or unmarked(word):
                continue
            others[word] = kind
        their_forms += other.execute("SELECT form_lc, lemma FROM forms ORDER BY rowid").fetchall()
        other.close()
    for word, kind in sorted(others.items()):
        pos[word].add(kind)
        common[word] = word
    for word in sorted(w for w in others if unmarked(w)):
        del others[word], common[word], pos[word]
    for form, lemma in their_forms:
        if lemma in others:
            add(form, lemma)

    # The regular forms of the nouns, adjectives and verbs with none in the pack.
    tabled = defaultdict(int)
    for of in lemmas.values():
        for lemma in set(of):
            tabled[lemma] += 1
    for word in sorted(pos):
        # Not a word sent elsewhere by hand (kont "account", whose konti would go to kien with it).
        if not is_word(word) or tabled.get(word, 0) > 1 or overrides.get(word, [word]) != [word]:
            continue
        regular = []
        if "noun" in pos[word]:
            regular += noun_forms(word)
        if "adj" in pos[word]:
            regular += adjective_forms(word)
        if "verb" in pos[word]:
            regular += verb_forms(word)
        for form in regular:
            add(form, word)
    # A perfect in -a the pack files as a verb's form (staqsa of saqsa) has that verb's regular forms, and a participle in m-
    # its feminine and plural (misruq: misruqa, misruqin; mistenni: mistennija).
    for form in sorted(lemmas):
        verbs = [l for l in dict.fromkeys(lemmas[form]) if "verb" in pos.get(l, ()) and l != form]
        if len(verbs) != 1 or is_word(form):
            continue
        if form.endswith("a") and not form.startswith(("j", "t", "n", "m")):
            for f in verb_forms(form):
                add(f, verbs[0])
        if form.startswith("m") and len(form) >= 4 and LETTERS.fullmatch(form):
            endings = ("ja", "jin") if form[-1] == "i" else ("a", "in") if form[-1] not in VOWELS else ()
            for ending in endings:
                add(form + ending, verbs[0])
        # And an active participle in -ej (ġej: ġejja, ġejjin).
        if form.endswith("ej") and len(form) >= 3:
            for f in (form + "ja", form + "jin"):
                add(f, verbs[0])

    def words_of(form):
        """The words a written form is of: itself when it is a word, else those it is filed under; or those of its spelling
        with or without an i before two consonants (jkollu as ikollu)."""
        for f in [form, prothetic(form)] + unprothetic(form):
            # A form sent elsewhere by hand goes there (kont "I was" to kien, not the noun "account").
            of = overrides.get(f) if f in overrides else [f] if is_word(f) else dict.fromkeys(lemmas.get(f, ()) if f else ())
            of = [w for w in of if is_word(w)]
            if of:
                return of
        return []

    def bases(stem):
        """What a stem before a pronoun is written alone: itself, -a for -ie (staqsieh), -e- for -i- (għamilha, jagħmilha)."""
        out = [stem]
        if stem.endswith("ie"):
            out.append(stem[:-2] + "a")
        # A dual drops its n (idejh of idejn), a preposition in -a takes a j (warajh of wara), a noun in -a its a for -it
        # (ħajjithom of ħajja).
        if stem.endswith(("ej", "aj")):
            out.append(stem + "n")
        if stem.endswith("aj"):
            out.append(stem[:-1])
        if stem.endswith("it") and len(stem) > 4:
            out.append(stem[:-2] + "a")
        # A perfect's -et and a noun's -a are written -t (qaltlu of qalet, ħiltu of ħila), a verb's -a -i (rnexxilu).
        if stem.endswith("t") and len(stem) >= 4 and stem[-2] not in VOWELS:
            out += [stem[:-1] + "et", stem[:-1] + "a"]
        if stem.endswith("i") and len(stem) >= 4:
            out.append(stem[:-1] + "a")
        last = stem.rfind("i")
        if 0 < last and len(stem) - last <= 3 and all(c not in VOWELS for c in stem[last + 1:]):
            out.append(stem[:last] + "e" + stem[last + 1:])
        return out

    def takes(word, suffix):
        """Whether [word] takes [suffix]: an object or indirect object after a verb (qatlu, qalli), a possessive after a noun
        (rasu, sħabi), a preposition or adverb (fuqha, għadhom, warajh), but my after a noun only."""
        kinds = pos[word] & word_pos
        if suffix == "i":
            return kinds == {"noun"}
        if suffix not in POSSESSIVE and suffix != "ih":
            return "verb" in kinds
        return bool(kinds & {"verb", "noun", "prep", "adv"})

    def suffixed(form):
        """The words [form] is with a pronoun or the negative written onto it (qalilhom: qal; hawnx: hawn), or []."""
        tries = []
        if form.endswith("iex") and len(form) > 5:
            tries.append((form[:-3] + "a", None))
        if form.endswith("x") and len(form) >= 3:
            # The negative shortens ie (kienu: kinux).
            tries += [(b, None) for b in (form[:-1], form[:-1][::-1].replace("i", "ei", 1)[::-1])]
        for suffix in SUFFIXES:
            stem = form[:-len(suffix)]
            if not form.endswith(suffix) or len(stem) < 2 or stem.endswith(TAKEN):
                continue
            # An adjective in -ali is no verb in -a with li (lokali, tali, ambjentali).
            if suffix.endswith("li") and form.endswith("ali"):
                continue
            # k and h come after a vowel (jagħtik, tah; h after ej and aj too: idejh, warajh), u and ek after a consonant
            # (qatlu, rajtek).
            if suffix == "k" and stem[-1] not in VOWELS or suffix == "h" and stem[-1] not in VOWELS | {"j"} or (
                    suffix in ("u", "ek") and stem[-1] in VOWELS):
                continue
            tries += [(b, suffix) for b in bases(stem) if suffix != "i" or len(b) >= 3]
        # The commonest of the spellings it may be made of (għamel + ha, not għam + ilha).
        found = []
        for base, suffix in tries:
            of = [w for w in words_of(base) if suffix is None or takes(w, suffix)]
            if of and base not in BARE and (counted(base) > 0 or base in pos):
                found.append((counted(base), -len(found), of))
        return max(found)[2] if found else []

    def mend():
        for form in sorted(SEEN, key=lambda f: (-SEEN[f], f)):
            if not free(form) or not LETTERS.fullmatch(form):
                continue
            # A form spelled with or without the i before two consonants (wħud, nkluż), or a word without its last vowel's
            # accent (diġa, pero, attivita) or apostrophe (erba), is the word's.
            of = words_of(form) or (form[-1] in ACCENTED and words_of(form[:-1] + ACCENTED[form[-1]])) or words_of(
                form + "'") or suffixed(form)
            if of:
                lemmas[form].extend(of)

    mend()
    induce(pos, lemmas, common, free, suffixed)
    # And the words found have their pronouns too (posthom of post).
    mend()


def plain(form):
    """[form] without its last vowel's accent (età: eta)."""
    return form[:-1] + {v: k for k, v in ACCENTED.items()}[form[-1]] if form[-1:] in ACCENTED.values() else form


def induce(pos, lemmas, common, free, suffixed):
    """The words no pack has (see the module's note), added with their forms; not one that is another found before it with a
    pronoun (posthom of post), which mend files as its form."""
    # Not a spelling of another without its marks (zona of żona, kaz of każ), its accent (eta) or its apostrophe (waqqa).
    found = sorted(f for f in SEEN if free(f) and LETTERS.fullmatch(f) and len(f) >= 3)
    by_unmarked = defaultdict(list)
    for f in found:
        by_unmarked[f.translate(UNMARK)].append(f)
    candidates = [f for f in found if counted(f + "'") < counted(f) and not any(
        g != f and counted(g) > counted(f) and all(a == b or UNMARK.get(ord(b)) == a for a, b in zip(f, g))
        for g in by_unmarked[f.translate(UNMARK)])]
    imperfect = re.compile("[jtn](.{3,}?)(?:i|u|a|aw)x?")
    paradigms = {}
    for form in candidates:
        match = imperfect.fullmatch(form)
        # Not an imperfect with a pronoun (jeħodha, jismagħna), which no verb in -ha or a consonant and -na is (ikkundanna is).
        stem = match.group(1) if match else ""
        if match and stem + "a" not in pos and not stem.endswith("h") and not (
                stem.endswith("n") and stem[-2] not in VOWELS | {"n"}):
            paradigms[stem + "a", "verb"] = set()
    for form in candidates:
        if counted(form) >= INDUCED_SHARE:
            paradigms[form, "noun"] = set()

    def mass(forms):
        return sum(counted(f) for f in forms)

    def verbal(forms):
        # Its imperfects of two persons (jinkludi, tinkludi), or one and a perfect; one of them in j- or t-, as n- is also the
        # passive's in- (inkarigu, inċertezza).
        told = {f for f in forms if f[0] in "jtn"}
        return len(forms) >= INDUCED_VERB_FORMS and any(f[0] in "jt" for f in told) and (
            len({f[0] for f in told}) > 1 or forms - told)

    for word, kind in list(paradigms):
        regular = verb_forms(word) if kind == "verb" else noun_forms(word) + adjective_forms(word)
        forms = {f for r in regular + [word] for f in [r, plain(r), prothetic(r)] + unprothetic(r) if f and free(f)}
        if kind == "verb" and not verbal(forms):
            del paradigms[word, kind]
            continue
        paradigms[word, kind] = forms
    claimed = set()
    heap = [(-mass(forms), word, kind) for (word, kind), forms in paradigms.items()]
    heapq.heapify(heap)
    while heap:
        negative, word, kind = heapq.heappop(heap)
        forms = paradigms[word, kind] - claimed
        if mass(forms) < -negative:
            paradigms[word, kind] = forms
            if forms:
                heapq.heappush(heap, (-mass(forms), word, kind))
            continue
        # A noun is written bare as often as a fifth of its commonest form (not prinċipal, of prinċipali).
        if kind == "noun" and (word in claimed or counted(word) < INDUCED_SHARE or
                               HEAD_SHARE * counted(word) < max(counted(f) for f in forms)):
            continue
        if kind == "verb" and not verbal(forms):
            continue
        if kind == "noun" and suffixed(word):
            continue
        # A noun written with the i of an article before it (iżvilupp of l-iżvilupp) is shown as it is written alone, if
        # texts write it so often enough (not nkluż of inkluż).
        alone = [f for f in unprothetic(word) if HEAD_SHARE * counted(f) >= counted(word)] if kind == "noun" else []
        shown = max(alone, key=lambda f: (counted(f), f)) if alone else word
        pos[shown].add(kind)
        common[shown] = shown
        for f in sorted(forms - {shown}):
            lemmas[f].append(shown)
        claimed |= forms | {word, shown}


# fold: the words of the counts a listed word's form is filed under, and those dropped as names. A word is listed when it is
# one of the first LISTED; a name is written in lowercase less than NAME_SHARE of the times no sentence starts with it, and
# written so at least NAME_LEAST and fewer than NAME_MOST times (Qawra, Grixti, Munxar; not the months, Għid or Xlokk); a
# compound's piece is written before a hyphen at least PIECE_SHARE of its uses (kontro-eżami, Kon-Katidral, neo-, eko-).
LISTED = 10_000
NAME_SHARE = 0.1
NAME_LEAST = 20
NAME_MOST = 400
PIECE_SHARE = 0.5
# A name's later piece is written capitalised after a name at least AFTER_NAME_SHARE of its uses, and in lowercase less than
# AFTER_NAME_LOWER of the times no sentence starts with it (Bush of George Bush, Rangers, Arts; not ministru, kurunell).
AFTER_NAME_SHARE = 0.3
AFTER_NAME_LOWER = 0.35
AFTER_NAME_LEAST = 10
# A word no pack has is English when the English dictionary (en-en) has it, or its singular, and English text has it at least
# ENGLISH_SHARE times as often as Maltese text (goalkeeper, meetings, stakeholders, roundabout); a word's form, when English
# has it ENGLISH_RATIO times as often (smart, filed under smar). A piece written next to damaged text DAMAGED_SHARE of its
# uses is none.
ENGLISH_SHARE = 0.05
# And Italian when spelled with none of Maltese's own letters (ċ ġ ħ ż j k x) and Italian text has it ITALIAN_RATIO times as
# often (punto, piazza, sorelle, giugno); not a verb, as many a loanword verb is spelled as its Italian (illumina, invita).
ITALIAN_RATIO = 4
ITALIAN_LETTERS = re.compile("[a-zàèìòù]+")
DAMAGED_SHARE = 0.5
# A word no pack has that is another's spelling (sistemà, assoċċjazzjoni, qegħdha) is one SPELLING_RATIO times rarer.
SPELLING_RATIO = 5
# Words the courses teach as another's form, which the pack has as words of their own: qarsa, "pinch", as the feminine of
# qares "sour".
TAUGHT_AS = {"qarsa": "qares"}
# A gloss naming the word a form is of: "grants (plural of għotja)", "(it) is staged ; is raised (form of ttella')", "past
# participle of għaraf: known", "observed (fem. of mħares)"; not a verbal noun (tħaddim of ħaddem), a word of its own. Or one
# made of a form and a pronoun: "they gave him (taw + -h)".
NAMES_BASE = re.compile(r"(?<!verbal noun )(?<!noun )\b(?:passive|mediopassive|plural|paucal|dual|fem\.|feminine|form|participle|"
                        r"spelling|misspelling|variant|imperfect|imperative|elative|comparative|singulative) of ([a-zàèìòùċġħż'-]+)")
# A later sense naming the word another's spelling (mbgħad: "afterwards (variant of imbagħad)"; xiehda: "witnesses (variant
# of xhieda)"), read where the two have the same consonants (not qattus "cat", "euphemistic form of qaddis").
VARIANT_BASE = re.compile(r"\b(?:variant|misspelling|spelling|alternative form) of ([a-zàèìòùċġħż'-]+)")
LEMMA_FIRST = re.compile(r"^\(([a-zàèìòùċġħż']+)\) ")
JOINED_BASE = re.compile(r"\(([a-zàèìòùċġħż']+) \+ -[a-zàèìòùċġħż']+\)")
# A gloss marking an adjective's feminine or plural (solved (fem.), academic (pl.)), and one reading as a passive.
MARKED = re.compile(r"\((?:[^()]*\b)?(?:fem|pl)\.\)|\bfem\.|\bfeminine\b|\bplural\b")
PASSIVE = re.compile(r"\b(?:be|is|are|was|were|been|being|passive|mediopassive)\b")
# A gloss reading as a participle (financed, given, paid, entering), and a loanword's participle (ffinanzjat, solvut, oppost,
# elett, diviż).
PARTICIPLE = re.compile(r"\b[a-z]+(?:ed|en|ing)\b|\b(?:made|done|built|paid|sold|set|put|held|kept|lost|sent|spent|bought|brought|"
                        r"taught|caught|found|left|met|told|known|shown|seen|given|taken|written|chosen|born|cut|hit|led|fed|"
                        r"read|run|won|worn|torn|sworn|struck|stuck|hung|dug|shut|split|spread|burnt|felt|heard|meant)\b")
LOAN_PARTICIPLE = re.compile(".{3,}(?:at|ut|it|st|ss|tt|ż)")
# The ends of a feminine or plural (eterna, siewja, xellugija, etniċi, mħallsin), and of a verb's persons (nħarġet, nħarġu).
ADJECTIVE_ENDS = ("ija", "ja", "a", "i", "in", "jin")
PERSON_ENDS = ("et", "u", "ew", "aw", "x")
# The other spellings a word is in, by letter: a vowel for another (konflitt, kunflitt; personaġġ, persunaġġ), s for ż
# (leġislazzjoni, leġiżlazzjoni).
SWAPS = {"o": "u", "u": "o", "e": "i", "i": "e", "s": "ż", "ż": "s", "z": "ż"}


def consonant(letter):
    return letter not in VOWELS and letter != "'"


def unprothetic_i(word):
    """[word], and without the i texts write before two consonants (inqabad, nqabad)."""
    two = len(word) > 3 and word[0] == "i" and consonant(word[1]) and consonant(word[2])
    return [word, word[1:]] if two else [word]


def revoweled(stem):
    """A stem whose last vowel a feminine, plural or person dropped (mħallsa, nħarġu), with each vowel put back (mħallas,
    nħareġ, nħoloq); għ is one consonant."""
    end = 2 if stem.endswith("għ") else 1
    head = stem[:-end]
    if len(stem) < 3 or not consonant(stem[-1]) or not head or not consonant(head[-1]):
        return []
    return [head + vowel + stem[-end:] for vowel in "aeio"]


def participle_verbs(word):
    """
    The verbs a participle may be of: a loanword's in -at, -ut, -it, its verb in -a or -ixxa with the doubled first consonant
    and the i the verbs are written with (ffinanzjat: ffinanzja; preparat: ipprepara; rappurtat: rrapporta; solvut: solva;
    projbit: pprojbixxa); and an m- participle's (mħallas: ħallas; mniżżel: niżżel).
    """
    out = []
    bare = unprothetic_i(word)[-1]
    loan = re.fullmatch(r"(.{3,}?)(?:at|ut|it)", bare)
    if loan:
        stem = loan.group(1)
        last = stem.rfind("u")
        for s in [stem] + ([stem[:last] + "o" + stem[last + 1:]] if last > 0 else []):
            for verb in (s + "a", s + "ixxa"):
                out += [verb, s[0] + verb, "i" + s[0] + verb, "i" + verb]
    if bare.startswith("m") and len(bare) >= 5 and consonant(bare[1]):
        out.append(bare[1:])
    return out


def masculines(word):
    """The masculines and singulars a feminine or plural adjective may be of (eterna: etern; komda: komdu; siewja: siewi;
    etniċi: etniku; mħallsa: mħallas)."""
    out = []
    for end in ADJECTIVE_ENDS:
        if word.endswith(end) and len(word) - len(end) >= 3:
            stem = word[:-len(end)]
            out += [stem, stem + "u", stem + "i"] + revoweled(stem)
    if word.endswith(("ċi", "ka")):
        out.append(word[:-2] + "ku")
    return out


def perfects(word):
    """
    The perfects a verb form may be of: its imperfect's without the person (jitwaqqaf: twaqqaf; tlaħħaq: laħħaq; jinħarġu:
    nħarġu), its person's without the ending, the dropped vowel put back (nħarġet, nħarġu: nħareġ; ttellgħet: ttella'), each
    with and without the i before two consonants (inqabad).
    """
    out = unprothetic_i(word)
    for form in list(out):
        if form[:1] in "jtn" and len(form) > 4:
            if form[1] == "i" and consonant(form[2]) and consonant(form[3]):
                out.append(form[2:])
            elif consonant(form[1]):
                out.append(form[1:])
    for form in list(out):
        for end in PERSON_ENDS:
            if form.endswith(end) and len(form) - len(end) >= 3:
                stem = form[:-len(end)]
                out += [stem] + revoweled(stem) + ([stem[:-2] + "a'"] if stem.endswith("għ") else [])
    # A weak verb's imperfect ends in -i (jiskadi: skada; jinkludi: inkluda), and a perfect starting with two consonants is
    # written with an i (jikkonferma: ikkonferma).
    out += [f[:-1] + "a" for f in out[1:] if f.endswith("i") and len(f) > 3]
    out += ["i" + f for f in out[1:] if len(f) > 3 and consonant(f[0]) and consonant(f[1])]
    return list(dict.fromkeys(out))


def imperfect_shaped(word):
    """Whether [word] reads as an imperfect, which the pack may gloss as a dictionary form (jitħallat "to be mixed",
    tintalab "to be asked for"): j and a consonant or i, or ti and two consonants."""
    return len(word) > 4 and (word[0] == "j" and (consonant(word[1]) or word[1] == "i") or (
        word[:2] == "ti" and consonant(word[2]) and consonant(word[3])))


def singulars(word):
    """The singulars a plural in -i, -jiet or -ijiet may be of (migranti: migrant; prostituti: prostituta; anfibji: anfibju;
    kanoniċi: kanoniku)."""
    out = []
    for end in ("ijiet", "jiet", "i"):
        if word.endswith(end) and len(word) - len(end) >= 3:
            stem = word[:-len(end)]
            out += [stem, stem + "a", stem + "u"] + ([stem + "i"] if end != "i" else []) + (
                [stem[:-1] + "ku"] if stem.endswith("ċ") else [])
            break
    return out


def plural_gloss(gloss, of):
    """Whether English [gloss] is the plural of [of] (migrants of migrant, amphibians of amphibian, tactics of tactic)."""
    if not gloss or not of:
        return False
    head, last = gloss.rsplit(" ", 1) if " " in gloss else ("", gloss)
    of_head, of_last = of.rsplit(" ", 1) if " " in of else ("", of)
    return head == of_head and last in (of_last + "s", of_last + "es", of_last[:-1] + "ies") + (
        (of_last[:-2] + "en",) if of_last.endswith("man") else ())


def variants(word):
    """The spellings [word] may be a slip for: spellings' (assoċċjazzjoni, pusses), its last vowel without or with its accent
    (sistemà), ħ for h and back (fiħ), without a silent h (qegħdha, kellhu), e for ie (ken)."""
    out = spellings(word) | {plain(word)}
    if word[-1:] in ACCENTED:
        out.add(word[:-1] + ACCENTED[word[-1]])
    for i, letter in enumerate(word):
        if letter in "hħ":
            out.add(word[:i] + ("ħ" if letter == "h" else "h") + word[i + 1:])
        if letter == "h" and i:
            out.add(word[:i] + word[i + 1:])
        if letter == "e" and word[i - 1:i] != "i":
            out.add(word[:i] + "ie" + word[i + 1:])
    return out - {word}


def undoubled(word):
    """[word] with its doubled letters written once."""
    return re.sub(r"(.)\1", r"\1", word)


def skeleton(word):
    """[word]'s consonants (xiehda and xhieda share theirs)."""
    return {c for c in word if consonant(c) and c != "-"}


def actives(perfect):
    """The actives a passive may be of: nkiser of kiser, ntalab of talab, ntlagħab of lagħab, ntesa of nesa, nstema' of sema',
    twaqqaf of waqqaf, ssaħħaħ of saħħaħ."""
    out = []
    if perfect.startswith("nt") and len(perfect) > 4:
        out.append(perfect[2:])
    if perfect.startswith("n") and len(perfect) > 3:
        out.append(perfect[1:])
        if len(perfect) > 4 and perfect[2] == "t":
            out.append(perfect[1] + perfect[3:])
    if len(perfect) > 3 and perfect[1] == "t" and consonant(perfect[0]):
        out.append(perfect[0] + perfect[2:])
    if len(perfect) > 4 and consonant(perfect[1]) and (perfect[0] == "t" or perfect[0] == perfect[1]):
        out.append(perfect[1:])
    return out


def unsuffixed(word):
    """What a verb form with a pronoun written onto it is without it (tawh: taw; narawh: naraw; staqsieh: staqsa)."""
    out = []
    for suffix in SUFFIXES + ["x"]:
        stem = word[:-len(suffix)]
        if word.endswith(suffix) and len(stem) >= 2:
            out += [stem] + ([stem[:-2] + "a"] if stem.endswith("ie") else [])
    return out


def spellings(word):
    """The other spellings of [word]: a consonant doubled or not (dettal, dettall), a letter for another (SWAPS), a verb's
    first consonant doubled, with or without its i (ittrasferixxa, ttrasferixxa, trasferixxa)."""
    out = {word[2:]} if len(word) > 4 and word[0] == "i" and word[1] == word[2] and consonant(word[1]) else set()
    out |= {word[1:]} if len(word) > 3 and word[0] == word[1] and consonant(word[0]) else set()
    out |= {word[0] + word, "i" + word[0] + word} if consonant(word[0]) and word[0] != word[1:2] else set()
    for i in range(1, len(word)):
        letter = word[i]
        if consonant(letter):
            out.add(word[:i] + word[i + 1:] if letter == word[i - 1] else word[:i] + letter + word[i:])
        if letter in SWAPS:
            out.add(word[:i] + SWAPS[letter] + word[i + 1:])
    return out - {word}


def fold(scores, forms, pos, pack, supplement):
    """
    Moves each word of the counts that is a listed word's form into that word, with its forms: a participle (ffinanzjata,
    mħallsa: ffinanzja, ħallas), a feminine or plural (eterna: etern), a verb's person (jqajjem: qajjem; nħarġet: nħareġ), a
    passive whose active is listed (ntalab: talab; jitħallas: ħallas), one with a pronoun (tawh: ta), a plural or form its gloss
    names (kisbiet: kisba), another spelling glossed alike (dettal: dettall). Drops names (Qawra), their later pieces (Bush)
    and a compound's first pieces (kontro, kon), and the English, Italian and damaged text no pack has (goalkeeper, punto);
    the [pack] glosses (its hand-written ones, [supplement], name the word anywhere), and the English dictionary beside it
    tells English. Returns what went where (None: dropped).
    """
    here = os.path.dirname(pack)
    packed = set()
    for name in ["mt-en"] + OTHER_PACKS:
        path = os.path.join(here, f"{name}.sqlite")
        if os.path.exists(path):
            db = sqlite3.connect(path)
            packed |= {w for (w,) in db.execute("SELECT word_lc FROM entries")}
            db.close()
    path = os.path.join(here, "en-en.sqlite")
    english_db = sqlite3.connect(path) if os.path.exists(path) else None

    def in_english(word):
        """Whether the English dictionary has [word] or its singular (meetings, referees, studios)."""
        if english_db is None or not re.fullmatch("[a-z]+", word):
            return False
        tries = [word] + [word[:-len(e)] + r for e, r in (("s", ""), ("es", ""), ("ies", "y")) if word.endswith(e)]
        return any(english_db.execute("SELECT 1 FROM entries WHERE word_lc = ? LIMIT 1", (t,)).fetchone() for t in tries)

    def is_english(word, share):
        return word not in packed and english(word) > 0 and english(word) >= share * counted(word) and in_english(word)

    def is_italian(word):
        return word not in packed and counted(word) > 0 and bool(ITALIAN_LETTERS.fullmatch(word)) and not re.search(
            "[jkx]", word) and "verb" not in pos.get(word, ()) and italian(word) >= ITALIAN_RATIO * counted(word)

    def is_name(word):
        written = WRITTEN.get(word, 0)
        return written >= NAME_LEAST and LOWER.get(word, 0) < NAME_SHARE * written

    # A word's forms that are names or English count for nothing (Bidnija of bidni, smart of smar); not a capitalised word's
    # (Maltin of Malti).
    for word in sorted(scores):
        foreign = [f for f in forms.get(word, {}) if f != word and (
            is_name(f) and not is_name(word) or is_english(f, ENGLISH_RATIO))]
        for form in foreign:
            scores[word] -= forms[word].pop(form)
    ranked = sorted(scores, key=lambda w: (-scores[w], w))
    listed = set(ranked[:LISTED])
    owner = {}
    for word in ranked[:LISTED]:
        for form, share in forms.get(word, {}).items():
            if form != word and (form not in owner or share > forms[owner[form]][form]):
                owner[form] = word
    glosses, firsts = defaultdict(list), defaultdict(list)
    db = sqlite3.connect(pack)
    for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos != 'name' ORDER BY id"):
        current = [g for s in json.loads(senses) if not {"obsolete", "archaic"} & set(s.get("tags", [])) for g in s.get("glosses", [])]
        glosses[word] += current
        firsts[word] += current[:1]
    db.close()
    written = {}
    for line in open(supplement, encoding="utf-8"):
        word, _, rest = line.rstrip("\n").partition("\t")
        written[word] = rest.partition("\t")[2]

    def kinds(word):
        return pos.get(word, set()) - {"name"}

    def listed_as(base):
        """The listed word [base] is, or is listed under."""
        return base if base in listed else owner.get(base)

    def fits(word, to):
        """Whether [word] may be [to]'s form by their kinds: a noun of a noun or adjective, a verb of a verb, an adjective of
        an adjective or a verb (a participle)."""
        k, of = kinds(word), kinds(to)
        return not k or not of or bool(k & of) or "adj" in k and "verb" in of

    def named(word):
        """The listed word a gloss names [word] a form of: a hand-written one anywhere (jqajjem: "raises (an issue) ; stirs up
        (form of qajjem)"), Wiktionary's in the first sense of each entry (not qattus "cat", whose second sense is a
        "euphemistic form of qaddis")."""
        if word in written:
            bases = [b for p in re.findall(r"\(([^()]*)\)", written[word]) for b in NAMES_BASE.findall(p) + JOINED_BASE.findall(f"({p})")]
            # Or one starting with the word it is a spelling or form of, of its consonants (kabbra "(kabbar) to enlarge"; not
            # grawnd "(sport) ground").
            bases += [b for b in LEMMA_FIRST.findall(written[word]) if {c for c in b if consonant(c)} <= set(word)]
        else:
            found = [NAMES_BASE.findall(g.partition(":")[0]) for g in firsts.get(word, [])]
            bases = [b[0] for b in found] if found and all(found) else []
        spelled = [b for g in glosses.get(word, ()) for b in VARIANT_BASE.findall(g) if skeleton(b) == skeleton(word)]
        for base in bases + spelled:
            # A spelling of a word written with the article (bilmod of bil-mod, talanqas of tal-anqas) is that word's.
            to = listed_as(base) or listed_as(base.rstrip("'")) or base.endswith("a'") and listed_as(base[:-2] + "għa") or (
                "-" in base and listed_as(base.rpartition("-")[2]))
            if to and to != word and (fits(word, to) or base in spelled or "-" in base):
                return to
        return None

    def verb(word):
        return "verb" in kinds(word)

    def dictionary_form(word):
        """Whether the pack glosses a verb as a dictionary form (to install), not an inflected one ((they) are issued)."""
        return any(g.startswith("to ") for g in glosses.get(word, ()))

    def first(candidates, word, test):
        for base in candidates:
            to = listed_as(base)
            if to and to != word and test(to, base):
                return to
        return None

    def target(word):
        k = kinds(word)
        found = named(word)
        if found:
            return found
        if "adj" in k and ("noun" not in k or any(MARKED.search(g) for g in glosses.get(word, ()))):
            # A verb's participle is glossed as one (financed, paid): not immedjat "immediate" (of immedja "to mediate"), nor
            # favorit "favourite".
            participial = not glosses.get(word) or any(PARTICIPLE.search(g) for g in glosses[word])
            marked = any(MARKED.search(g) for g in glosses.get(word, ()))
            found = participial and first(participle_verbs(word), word, lambda to, base: verb(to)) or first(
                masculines(word), word, lambda to, base: "adj" in kinds(to) or marked and base == to or participial and (
                    base != to and verb(to) and bool(LOAN_PARTICIPLE.fullmatch(base) or base.startswith(("m", "im")))))
            for base in masculines(word) if participial and not found else ():
                found = first(participle_verbs(base), word, lambda to, b: verb(to))
                if found:
                    break
            if found:
                return found
        # A plural the pack glosses as one (migranti "migrants", anfibji "amphibians") is its singular's.
        if "noun" in k and word.endswith("i"):
            gloss = [re.sub(r"\([^)]*\)", "", g).strip().lower() for g in firsts.get(word, [])[:1]]
            found = gloss and first(singulars(word), word, lambda to, base: base == to and bool(kinds(to) & {"noun", "adj"}) and any(
                plural_gloss(gloss[0], re.sub(r"\([^)]*\)", "", g).strip().lower()) for g in firsts.get(to, [])[:1]))
            if found:
                return found
        # A verb found by its imperfects is made in -a (idawwlu: dawwla), which may be a listed verb's with its last vowel
        # (dawwal).
        if word not in packed and "verb" in k and word.endswith("a"):
            found = first(revoweled(word[:-1]), word, lambda to, base: verb(to))
            if found:
                return found
        # A verb's form; or one no pack has (nġibdu, of nġibed: ġibed).
        if k <= {"verb"} or word not in packed:
            whole = dictionary_form(word) and not imperfect_shaped(word)
            found = first([] if whole else perfects(word)[1:], word, lambda to, base: verb(to))
            if not found and (not glosses.get(word) or any(PASSIVE.search(g) for g in glosses[word])):
                for perfect in unprothetic_i(word) if whole else perfects(word):
                    found = first(actives(perfect), word, lambda to, base: verb(to))
                    if found:
                        break
            if not found and not whole:
                found = first(unsuffixed(word), word, lambda to, base: verb(to))
            if found:
                return found
        alike = {re.sub(r"\([^)]*\)", "", g).strip().lower() for g in glosses.get(word, ())} - {""}
        found = first(sorted(spellings(word)), word, lambda to, base: k & kinds(to) and scores[to] > scores[word] and alike & {
            re.sub(r"\([^)]*\)", "", g).strip().lower() for g in glosses.get(to, ())})
        # A word no pack has that is a slip for one written SPELLING_RATIO times as often (sistemà, qegħdha, kellhu) is that
        # word's; with a consonant doubled or not, only for a word's own spelling (assoċċjazzjoni), not a form's (gwerrer
        # "warrior", gwerer "wars").
        return found or word not in packed and first(sorted(variants(word)), word, lambda to, base: counted(
            base) >= SPELLING_RATIO * counted(word) and bool(k & kinds(to)) and (base == to or undoubled(base) != undoubled(
                word)))

    # How often a word is written as a name's later piece: capitalised after a name (George Bush).
    after = defaultdict(int)
    for (before, word), times in AFTER.items():
        if before != word and WRITTEN.get(before, 0) >= 3 and LOWER.get(before, 0) < NAME_SHARE * WRITTEN[before]:
            after[word] += times

    moves = {}
    for word in ranked:
        if not LETTERS.fullmatch(word.rstrip("'")):
            continue
        # English (goalkeeper, meetings), Italian (punto) and pieces of damaged text no pack has are no words.
        damaged = 0 < SEEN_IN.get(word, 0) * DAMAGED_SHARE <= DAMAGED.get(word, 0)
        if word not in packed and (is_english(word, ENGLISH_SHARE) or is_italian(word) or damaged):
            moves[word] = None
            continue
        found = TAUGHT_AS.get(word) or target(word)
        if found:
            moves[word] = found
        elif SEEN_IN.get(word) and HYPHENED.get(word, 0) >= PIECE_SHARE * SEEN_IN[word]:
            moves[word] = None
        elif NAME_LEAST <= WRITTEN.get(word, 0) < NAME_MOST and LOWER.get(word, 0) < NAME_SHARE * WRITTEN[word]:
            moves[word] = None
        elif SEEN_IN.get(word, 0) >= AFTER_NAME_LEAST and after[word] >= AFTER_NAME_SHARE * SEEN_IN[word] and LOWER.get(
                word, 0) < AFTER_NAME_LOWER * WRITTEN.get(word, 0):
            moves[word] = None

    def final(word):
        """Where [word] goes, following words that go elsewhere themselves (jinħarġu, nħarġu, nħareġ); itself where that
        is a name or piece dropped, or comes back round."""
        seen = [word]
        while moves.get(seen[-1]) and seen[-1] not in seen[:-1]:
            seen.append(moves[seen[-1]])
        end = seen[-1]
        return word if end in seen[:-1] or end in moves else end

    for word in ranked:
        if word not in moves:
            continue
        to = final(word) if moves[word] else None
        if to == word:
            continue
        share, its = scores.pop(word), forms.pop(word, {})
        if to is None:
            continue
        scores[to] += share
        for form, part in its.items():
            forms.setdefault(to, {})[form] = forms.get(to, {}).get(form, 0) + part
    return moves
