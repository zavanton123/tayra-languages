"""
Hausa for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

Hausa is written in Boko, the Latin alphabet with the hooked letters ɓ ɗ ƙ ƴ, which much text writes with an apostrophe:
'b, 'd, k' and 'y ('yan, k'asa, 'dan). The reader (HausaParser) matches a word with the hooked letters whichever way it is
written; an apostrophe inside a word is its glottal stop (sa'a, jama'a) and stays. Keep hausa_fold and HausaParser.fold the
same. Ajami, Hausa in Arabic letters, is no part of the pack or the list.
"""
import bz2
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import defaultdict

APOSTROPHES = str.maketrans("’‘ʼ´`", "'''''")
_K_HOOK = re.compile(r"([kK])'(?=[^\W\d_])")
_HOOK_FIRST = re.compile(r"'([bBdDyY])")
_HOOKED = {"b": "ɓ", "B": "Ɓ", "d": "ɗ", "D": "Ɗ", "y": "ƴ", "Y": "Ƴ"}
# A Hausa word as the reader cuts it: letters with apostrophes inside, and one in front where it writes a hooked letter.
WORD = re.compile(r"(?:'(?=[bdyBDY]))?[^\W\d_]+(?:'[^\W\d_]+)*")
# Hausa in Arabic letters (Ajami), with the letters it adds to them.
AJAMI = re.compile("[؀-ۿݐ-ݿࢠ-ࣿﭐ-﷿ﹰ-﻿]")


def hausa_fold(word):
    """A word with its hooked letters for the apostrophe spellings (k'asa as ƙasa, 'yan as ƴan), as HausaParser.fold has it."""
    word = word.translate(APOSTROPHES)
    if "'" not in word:
        return word
    word = _K_HOOK.sub(lambda m: "ƙ" if m.group(1) == "k" else "Ƙ", word)
    return _HOOK_FIRST.sub(lambda m: _HOOKED[m.group(1)], word)


def unmarked(word):
    """A word without the tone and length marks the dictionary writes (gidā̀jē, kar̃àntā), as text writes it."""
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", word) if not unicodedata.combining(c)))


def hausa_clean(word):
    """A dictionary spelling as text writes it and the reader matches it: unmarked, with its hooked letters."""
    return hausa_fold(unmarked(word.strip()))


# ---------------------------------------------------------------------------------------------------------------------------
# The frequency list (build_frequency_list.py --language ha).
#
# Hausa has no wordfreq or subtitle list. It is counted from three kinds of text, each the share of the blend WEIGHTS gives:
# everyday sentences (Tatoeba's and Common Voice's), news (Leipzig's community corpus: VOA Hausa, Leadership Hausa, CRI) and
# Wikipedia (OPUS's wikimedia, the Hausa Wikipedia's translated articles). A capitalised word inside a sentence is a name and
# not counted, nor is a sentence that is mostly English (a reference or a title).
#
# Most news and web text writes ɓ ɗ ƙ as plain b d k (dan, kasa, daya for ɗan, ƙasa, ɗaya); 'y it keeps. A plain spelling in
# a sentence that writes no hooked letter is shared among the words it may stand for (kasa: ƙasa "land" and kasa "fail") by how
# often each is written in the sentences that do write them (resolve_hooks).
#
# Words are grouped as the pack groups them (dictionaries/ha-en.sqlite: plurals, possessed forms) and by rule: a noun's or
# adjective's linked form (gidan, ƙasar) and possessives (gidansa, ƙasarmu, gidana, motata), a verb's verbal noun linker (ganin,
# samun). The pack (Wiktionary and Wikidata) lacks many common words (na, ne, kuma, akwai, cewa): a word it lacks is listed for
# itself when everyday sentences have it, or news and Wikipedia both do, and it is no English word.
# ---------------------------------------------------------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")
ENGLISH_LIST = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency", "en.tsv")

LEIPZIG_NEWS = ["hau_community_2017"]
TATOEBA = ("hau", "https://downloads.tatoeba.org/exports/per_language/hau/hau_sentences.tsv.bz2")
COMMONVOICE = ("ha", "70dde553e48d7d0da452ba4ba070c001f65db88c", ["sentence-collector"])
OPUS = [("wikipedia", "wikimedia", "v20260327")]
WEIGHTS = {"everyday": 0.35, "news": 0.45, "wikipedia": 0.2}
LICENCES = {
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "tatoeba": "CC BY 2.0 FR",
    "commonvoice": "CC0 1.0",
    "wikimedia": "CC BY-SA 4.0",
}

SENTENCE_START = re.compile(r"(?:^|[.!?:;…\"“”«»()\[\]♪-]\s*)$")
DEHOOK = str.maketrans("ɓɗƙƴ", "bdky")
# How much a word of the pack counts as written in hooked text before any is: a plain spelling no hooked text has is shared
# among the words it may stand for alike.
PACK_PRIOR = 1.0
# Below this share of the text that writes the hook and has a plain spelling or its hooked one, the plain one is a dropped
# hook, if the pack has no word so spelled.
MISSPELT_SHARE = 0.3
# Below this share, a plain spelling the pack lacks beside a hooked word of the pack is that word's.
PACK_TWIN_SHARE = 0.9
# At or above this share, a hooked spelling the pack lacks is the word a plain one the pack lacks stands for.
HOOKED_SHARE = 0.3
# English words that mark a sentence as English (a reference, a title).
ENGLISH_MARKERS = {"the", "of", "and", "for", "with", "from", "by", "is", "was", "are", "were", "this", "that", "which", "their"}
# Letters no Hausa word has: q, v and x are foreign; p is in loanwords the pack has.
FOREIGN_LETTERS = re.compile("[qvxpçéèêëàâîïôöüûñß]")
# A word must be counted this often (all its forms, all sources) to be listed.
LEAST_COUNT = 4
FORMS_LISTED = 200

NOT_WORDS = {"name", "character", "prefix", "suffix", "phrase", "proverb", "symbol"}
NOMINAL = {"noun", "adj", "num"}
POSSESSIVES = ("sa", "ta", "ka", "ki", "mu", "ku", "su")

# By hand: a form's word where the rules would take another (form: word, or "" for no word of the list). A word of its own
# that a rule makes from another: kaɗan "a little" (kaɗa "beat" + n), kusan "almost", fuskanta "face" (fuska + n + ta),
# faranta "please", mayar "return"; Faransa is France.
HAUSA_FORMS = {
    "kaɗan": "kaɗan", "kusan": "kusan", "fuskanta": "fuskanta", "faranta": "faranta", "faransa": "", "mayar": "mayar",
    "mutumin": "mutum", "man": "mai", "kwatanta": "kwatanta", "jagoranta": "jagoranta", "karyata": "karyata",
    "kyautata": "kyautata", "tabbata": "tabbata",
    # Spellings without the hook that Wikidata has as words of their own.
    "matukar": "matuƙa", "bukata": "buƙata", "bukatar": "buƙata", "sako": "saƙo", "sakon": "saƙo", "zabe": "zaɓe",
    "zaben": "zaɓe", "alkawari": "alƙawari", "alkawarin": "alƙawari",
    # Spellings without the hook that hook-writing text drops often enough to pass for words.
    "yaki": "yaƙi", "yakin": "yaƙi", "daki": "ɗaki", "dakin": "ɗaki", "rika": "riƙa", "kadan": "kaɗan",
    # farar "white" (feminine) is fari's, not fara's.
    "farar": "fari",
    # Plurals the pack lacks, and their linked forms.
    "lokuta": "lokaci", "lokutan": "lokaci", "yankuna": "yanki", "yankunan": "yanki", "shugabanni": "shugaba",
    "shugabannin": "shugaba", "kayayyaki": "kaya", "kayayyakin": "kaya", "hanyoyi": "hanya", "hanyoyin": "hanya",
    "ƙungiyoyi": "ƙungiya", "ƙungiyoyin": "ƙungiya", "jirage": "jirgi", "jiragen": "jirgi", "kamfanoni": "kamfani",
    "kamfanonin": "kamfani", "ɓangarori": "ɓangare", "ɓangarorin": "ɓangare", "hotuna": "hoto", "hotunan": "hoto",
    "halaye": "hali", "halayen": "hali", "manufofi": "manufa", "manufofin": "manufa", "harkoki": "harka",
    "harkokin": "harka", "dabaru": "dabara", "dabarun": "dabara", "rahotanni": "rahoto", "rahotannin": "rahoto",
    "idanu": "ido", "idanun": "ido", "iyalai": "iyali", "iyalan": "iyali", "waɗansu": "waɗansu", "kullun": "kullum",
    # A hook written where there is none (yake).
    "yaƙe": "",
    # Feminines and spellings the pack does not link: waccan "that (fem.)", tasu "theirs (fem.)", siya for saya "buy", tayar
    # for tada "raise", wanna for wannan; ɗin's possessives (motar ɗina, fim ɗinta; not ɗinka "sew", ɗinki "sewing").
    "waccan": "wancan", "tasu": "nasu", "siya": "saya", "siyan": "saya", "siyo": "saya", "siyar": "sayar", "tayar": "tada",
    "wanna": "wannan", "ɗina": "ɗin", "ɗinsa": "ɗin", "ɗinta": "ɗin", "ɗinsu": "ɗin", "ɗinmu": "ɗin",
    "ɗinku": "ɗin", "bukukuwa": "biki", "bukukuwan": "biki", "dawakai": "doki", "dawakan": "doki", "jahar": "jiha",
    "jaha": "jiha", "maras": "marasa", "marar": "marasa", "yuwu": "yiwu", "yiyu": "yiwu", "hayaki": "hayaƙi",
    "hayakin": "hayaƙi", "natsuwa": "nutsuwa", "natsuwar": "nutsuwa", "buki": "biki", "gagaruma": "gagarumi",
    "gagarumar": "gagarumi", "yanda": "yadda", "tamu": "namu", "kanki": "kai", "magudanar": "magudana",
    "magudanan": "magudana", "ƙaragar": "ƙaraga",
    # Words of their own the rules would make forms: liman "imam" (no lima + n), wucin of wucin gadi "temporary", kokawa
    # "wrestling" (no koka "complain" + wa).
    "liman": "liman", "wucin": "wucin", "kokawa": "kokawa",
    # Loanwords in -n and -r, no linked forms (radar, beyar "bear", kushin "cushion", laftanan "lieutenant").
    "radar": "radar", "beyar": "beyar", "kushin": "kushin", "golan": "golan", "bahar": "bahar", "sansan": "sansan",
    "malin": "malin", "laftanan": "laftanan", "lasin": "lasin", "mesin": "mesin",
}
# Words the pack lacks that are spelled as a hooked word is without its hook, and are words of their own: ki "you" (ƙi "refuse"),
# yaya "how" (ƴaƴa "children", written 'ya'ya), haka "so" (haƙa "dig"), kala "kind", kadara "property".
HAUSA_PLAIN_WORDS = {"ki", "yaya", "haka", "kala", "kadara"}
# Wikidata's words written without their hooks, which are no words of their own (bukata for buƙata).
HAUSA_NOT_WORDS = {"bukata", "zabe", "sako", "matukar", "alkawari", "wasika", "kwaya", "karuwa"}
# Forms the pack gives a word that are words of their own (doki "horse" of doka "law", kashe "kill" of kasa, san "know" of sa
# and sau, sanya "put on" of sa, rage "reduce" of raga "net", iyaye "parents" of uwa) or no words (i, im of iya).
HAUSA_NOT_FORMS = {("doki", "doka"), ("kashe", "kasa"), ("san", "sa"), ("san", "sau"), ("sanya", "sa"), ("rage", "raga"),
                   ("ragi", "raga"), ("iyaye", "uwa"), ("i", "iya"), ("im", "iya"), ("iye", "iya"), ("guje", "guda"),
                   ("guji", "guda"), ("fara", "fari"), ("damina", "dama"), ("biye", "miya"), ("buda", "wanka"),
                   ("bina", "wanka")}
# Two words written as one, as much text does (yace for ya ce, shine for shi ne): each is counted as its words.
HAUSA_JOINED = {
    "yace": "ya ce", "tace": "ta ce", "sukace": "suka ce", "yayi": "ya yi", "tayi": "ta yi", "sukayi": "suka yi",
    "zasu": "za su", "zaka": "za ka", "zamu": "za mu", "zaki": "za ki", "zaku": "za ku", "za'a": "za a",
    "bane": "ba ne", "bace": "ba ce", "basu": "ba su", "bamu": "ba mu", "baza": "ba za",
    "dake": "da ke", "abinda": "abin da", "shine": "shi ne", "itace": "ita ce", "sune": "su ne", "anan": "a nan",
    "yasa": "ya sa", "bada": "ba da", "yiwa": "yi wa", "menene": "me ne ne", "nayi": "na yi", "kayi": "ka yi",
    "akayi": "aka yi", "ankai": "an kai", "bazan": "ba zan", "yafi": "ya fi", "dasu": "da su", "dashi": "da shi",
    "yakamata": "ya kamata", "meyasa": "me ya sa", "koda": "ko da", "kodayake": "ko da yake", "akai": "a kai",
    "mene": "me ne", "haryanzu": "har yanzu", "aciki": "a ciki", "acikin": "a cikin", "akansa": "a kansa", "kuyi": "ku yi",
    "barsu": "bar su", "harma": "har ma", "shike": "shi ke", "wanan": "wannan", "nima": "ni ma", "itama": "ita ma",
    "kaima": "kai ma", "kema": "ke ma",
}
# Words that are no Hausa words of ordinary text: names written small, English, abbreviations, fragments (basuwa, written for
# ba su), the linker n written apart.
HAUSA_EXCLUDED = {"allah", "adam", "s", "of", "habibu", "on", "n", "basuwa", "ƙwa"}

# ---------------------------------------------------------------------------------------------------------------------------
# Entries of the pack that are forms (Lexicon._link_entries).
#
# Wikidata's lexemes have many plurals, linked forms and feminines as entries of their own (makonni "weeks", kiɗan "music
# (of)", cikakkiyar "complete (fem.)"), spellings (banbanci "difference (variant of bambanci)", hakki beside haƙƙi), grades of
# a verb (gayyace beside gayyata "to invite"), joined spellings ("nasan: (na san) I know") and the halves of a hyphenated word
# ("canje: (canje-canje) changes"). Each is counted for its word: a gloss "variant, plural or feminine of X" names it; a
# linked form is its word without the -n or -r; a plural is the word beginning alike that is glossed as its gloss in the
# singular; a feminine or plural adjective has the masculine of its ending (ADJECTIVE_ENDINGS). Spellings and grades the pack
# glosses alike are one word, listed as the commonest. A verbal noun in -wa is its verb's (dubawa: duba), and a linked form
# whose word the pack lacks is that word's when text writes it (kolin: koli; Lexicon.link_bare). A joined spelling is counted
# as its words, and a word mostly written as half of a hyphenated one is not counted (HYPHEN_SHARE).
# ---------------------------------------------------------------------------------------------------------------------------
GLOSS_PARENS = re.compile(r"\(([^()]*)\)")
GLOSS_OF = re.compile(r"\b(?:variant(?: spelling)?|plural|feminine|fem\.?) of ([^()]+?)\s*(?:\)|$)")
# The feminine and plural endings of the adjectives in -aCCe (cikakke, cikakkiya, cikakku; ingantacce, ingantacciya,
# ingantattu; ƙwararre; nakasasshe), and the masculine ending each is of.
ADJECTIVE_ENDINGS = [("cciya", "cce"), ("ttu", "cce"), ("kkiya", "kke"), ("kku", "kke"), ("rriya", "rre"), ("rru", "rre"),
                     ("ssiya", "sshe"), ("ssu", "sshe"), ("nniya", "nne"), ("nnu", "nne"), ("bbiya", "bbe"), ("bbu", "bbe"),
                     ("ddiya", "dde"), ("ddu", "dde"), ("lliya", "lle"), ("llu", "lle")]
# A verb's stem before a grade's last vowel, with the palatal consonants the -e and -i grades have for t, s, z (gayyace:
# gayyata).
GRADE_STEM = re.compile("(?:c|sh|j)$")
PALATAL = {"c": "t", "sh": "s", "j": "z"}
# A word joined to the verb after it that the pack lacks (muyi, naji, kace): the person-aspect words and the commonest verbs.
JOINED_PERSONS = sorted({"na", "ya", "ta", "mu", "ku", "su", "ka", "ki", "a", "an", "sun", "mun", "kun", "ke", "ake", "zai",
                         "zan", "za", "bai", "ban"}, key=lambda p: (-len(p), p))
JOINED_VERBS = {"yi", "ce", "ji", "ga", "je", "zo", "san"}
# At or above this share of its count written as half of a hyphenated word (fina-finai, ire-iren), a spelling is not counted.
HYPHEN_SHARE = 0.5
ENGLISH_IRREGULAR = {"people": "person", "children": "child", "men": "man", "women": "woman", "feet": "foot", "teeth": "tooth",
                     "mice": "mouse", "geese": "goose", "wives": "wife", "lives": "life", "knives": "knife", "leaves": "leaf",
                     "thieves": "thief", "halves": "half", "wolves": "wolf", "loaves": "loaf", "calves": "calf"}


def gloss_keys(glosses):
    """A pack entry's glosses as compared: lowercase, without what is in brackets, each part apart, without to, a, the."""
    keys = set()
    for gloss in glosses:
        for part in re.split(r"[;,/]", GLOSS_PARENS.sub(" ", gloss).lower()):
            part = re.sub(r"^(?:to|a|an|the) ", "", " ".join(part.split()).strip(" .!?"))
            part = re.sub(r"our(s?)\b", r"or\1", part)
            if part:
                keys.add(part)
    return keys


def english_singular(phrase):
    """An English plural gloss in the singular (weeks: week, political parties: political party), or None."""
    *head, last = phrase.split() or [""]
    if last in ENGLISH_IRREGULAR:
        single = ENGLISH_IRREGULAR[last]
    elif last.endswith("ies") and len(last) > 4:
        single = last[:-3] + "y"
    elif last.endswith(("ches", "shes", "sses", "xes", "zes")):
        single = last[:-2]
    elif last.endswith("s") and not last.endswith(("ss", "us", "is")) and len(last) > 3:
        single = last[:-1]
    else:
        return None
    return " ".join(head + [single])


def verbal_heads(keys):
    """The first words of glosses, an -ing one also as its verb (looking: look, preparing: prepare, running: run)."""
    out = set()
    for key in keys:
        word = key.split()[0]
        out.add(word)
        if word.endswith("ing") and len(word) > 5:
            stem = word[:-3]
            out |= {stem, stem + "e"} | ({stem[:-1]} if stem[-1] == stem[-2] else set())
    return out


def heads_alike(a, b):
    """Whether two sets of gloss words share one, or one's first five letters (existence, exist; continuation, continue)."""
    return bool(a & b) or any(len(x) >= 5 and len(y) >= 5 and x[:5] == y[:5] for x in a for y in b)


def skeleton(word):
    """A spelling without its hooks, doubled letters and the n/m written for the one before b, f, m (banbanci, bambanci; hakki,
    haƙƙi; al'uma, al'umma; mutun, mutum)."""
    word = re.sub("m$", "n", re.sub("n(?=[bfm])", "m", word.translate(DEHOOK)))
    return re.sub(r"(.)\1", r"\1", word)


def distance(a, b):
    """The edit distance between two spellings."""
    row = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        prev, row[0] = row[0], i
        for j, y in enumerate(b, 1):
            prev, row[j] = row[j], min(row[j] + 1, row[j - 1] + 1, prev + (x != y))
    return row[-1]


# The hooked letters a sentence writes (ɓ, ɗ, ƙ, or their apostrophe spellings), by the plain letter each stands for. 'y is
# kept by text that writes no other hook, so a plain y is never taken as written on purpose.
HOOKS = {"b": re.compile("[ɓƁ]|'[bB]"), "d": re.compile("[ɗƊ]|'[dD]"), "k": re.compile("[ƙƘ]|[kK]'[^\\W\\d_]")}
HOOKED_LETTERS = {"ɓ": "b", "ɗ": "d", "ƙ": "k", "ƴ": "y"}
# Common words whose plain spelling is no word but a dropped hook: a sentence with one drops that hook, whatever else it writes.
DROPPED_HOOKS = {"dan": "d", "daya": "d", "kasar": "k", "kasashe": "k", "kasashen": "k", "kungiya": "k", "kungiyar": "k",
                 "kungiyoyi": "k", "kungiyoyin": "k", "karshe": "k", "karshen": "k", "karkashin": "k", "bangare": "b",
                 "bangaren": "b", "dalibai": "d", "daliban": "d", "kudi": "d", "kudin": "d", "kananan": "k", "wadanda": "d",
                 "wadannan": "d", "daukar": "d", "dauki": "d", "kokari": "k", "kokarin": "k", "kauye": "k", "kauyen": "k",
                 "kwarai": "k", "bukatar": "k", "zaben": "b"}


def hooks_written(sentence, words=()):
    """The plain letters whose hooked letter a sentence writes and drops nowhere (b, d, k)."""
    written = {letter for letter, pattern in HOOKS.items() if pattern.search(sentence)}
    return frozenset(written - {DROPPED_HOOKS[w] for w in words if w in DROPPED_HOOKS})


def dropped(spelling, hooked):
    """The plain letters [spelling] has where [hooked] has a hooked one (dan, ɗan: d)."""
    return frozenset(HOOKED_LETTERS[h] for p, h in zip(spelling, hooked) if p != h)


def _hyphened(line, start, end):
    """Whether the word at line[start:end] is half of a hyphenated one (fina-finai, canje-canje)."""
    before = start >= 2 and line[start - 1] == "-" and line[start - 2].isalpha()
    return before or line[end:end + 1] == "-" and line[end + 1:end + 2].isalpha()


def tokens(line, names=None, split=None, hyphened=None):
    """A line's words as the reader cuts and matches them, lowercase, without the capitalised ones inside a sentence (names),
    which are counted in [names]. [split] gives the words a joined spelling stands for (Lexicon.split); the words that are
    half of a hyphenated one are counted in [hyphened]."""
    line = line.translate(APOSTROPHES)
    for match in WORD.finditer(line):
        word = match.group(0)
        first = word.lstrip("'")
        # A word in capitals is an abbreviation (NGO, FCDA) or a heading's, and no evidence of how a word is written.
        if len(first) > 1 and first.isupper():
            continue
        if first[:1].isupper() and not SENTENCE_START.search(line[:match.start()]):
            if names is not None:
                names[hausa_fold(word).lower()] += 1
            continue
        word = hausa_fold(word).lower()
        if hyphened is not None and _hyphened(line, match.start(), match.end()):
            hyphened[word] += 1
        words = HAUSA_JOINED.get(word, word).split()
        if split is not None and len(words) == 1:
            words = split(word) or words
        yield from words


def english(words):
    return sum(1 for w in words if w in ENGLISH_MARKERS) >= 2


class Lexicon:
    """The words of the ha-en pack, their forms, and the forms the rules make."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "ha-en.sqlite"))
        self.wiktionary = set()
        with gzip.open(os.path.join(dictionaries, "kaikki.org-dictionary-Hausa.jsonl.gz"), "rt", encoding="utf-8") as f:
            for line in f:
                entry = json.loads(line)
                self.wiktionary.add(hausa_clean(entry["word"]).lower())
                self.wiktionary.update(hausa_clean(x["form"]).lower() for x in entry.get("forms", []) if x.get("form"))
        self.kinds = defaultdict(set)
        for word, pos in db.execute("SELECT word_lc, pos FROM entries"):
            if pos in NOT_WORDS or not WORD.fullmatch(word) or word in HAUSA_NOT_WORDS:
                continue
            self.kinds[word].add(pos)
        self.forms = defaultdict(set)
        own = defaultdict(set)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            lemma = lemma.lower()
            if lemma in self.kinds and form != lemma and len(form) > 1 and WORD.fullmatch(form) and (form, lemma) not in HAUSA_NOT_FORMS:
                self.forms[form].add(lemma)
                own[lemma].add(form)
        self.generated = defaultdict(set)
        # A possessive the rules make, by its linked form (gidanta: gidan), for telling a verb in -nta apart (kwatanta).
        self.possessive_of = {}
        for word, kinds in self.kinds.items():
            for form, linked in self._rule_forms(word, kinds, own[word]):
                if form != word:
                    self.generated[form].add(word)
                    if linked:
                        self.possessive_of[form] = linked
        self.senses = defaultdict(list)
        for word, senses in db.execute("SELECT word_lc, senses FROM entries"):
            if WORD.fullmatch(word):
                self.senses[word] += [s["glosses"][0] for s in json.loads(senses) if s.get("glosses")]
        self._link_entries()

    def _link_entries(self):
        """
        The pack's entries that are another entry's forms, read from their glosses and spellings (see "Entries of the pack
        that are forms" above): [links] (form: word), [same] (groups of spellings or grades of one word, the commonest of which
        build() keeps) and [joined] (a joined spelling: its words).
        """
        self.links, self.joined, self.same = {}, {}, []
        keys = {w: gloss_keys(g) for w, g in self.senses.items()}
        by_key = defaultdict(set)
        for word in self.kinds:
            for key in keys.get(word, ()):
                by_key[key].add(word)
        for word, glosses in self.senses.items():
            for text in [t for g in glosses for t in GLOSS_PARENS.findall(g) + GLOSS_OF.findall(g)]:
                parts = [hausa_clean(p).lower() for p in text.split()]
                if len(parts) > 1 and "".join(parts) == word and all(WORD.fullmatch(p) for p in parts):
                    self.joined[word] = parts
        for word in self.kinds:
            if word in HAUSA_FORMS:
                if HAUSA_FORMS[word] and "," not in HAUSA_FORMS[word] and HAUSA_FORMS[word] != word:
                    self.links[word] = HAUSA_FORMS[word]
                continue
            if word in self.joined:
                continue
            target = self._form_of(word, keys, by_key)
            if target and target != word:
                self.links[word] = target
        # Grades of a verb (gayyata, gayyace) and spellings of a word (bambanci, banbanci; hakki, haƙƙi) the pack glosses alike.
        groups = defaultdict(set)
        for word in self.kinds:
            if len(word) >= 4 and word[-1] in "aeiou" and self._verb(word):
                groups["grade", GRADE_STEM.sub(lambda m: PALATAL[m.group(0)], word[:-1])].add(word)
            groups["spelling", skeleton(word)].add(word)
            if keys.get(word):
                groups["glosses", frozenset(keys[word])].add(word)
        for (kind, _), words in groups.items():
            if len(words) < 2 or len(words) > 12:
                continue
            words = sorted(words)
            for i, a in enumerate(words):
                for b in words[i + 1:]:
                    # Glossed alike; two spelled apart by their hooks alone in at least half the senses of the one with fewer
                    # (kafa "leg; to erect; to establish" is no spelling of ƙafa "foot, leg").
                    alike = keys.get(a, set()) & keys.get(b, set())
                    if not alike or a.translate(DEHOOK) == b.translate(DEHOOK) and 2 * len(alike) < min(len(keys[a]), len(keys[b])):
                        continue
                    if kind == "grade" and "verb" not in self.kinds[a] | self.kinds[b]:
                        continue
                    # Spellings apart by more than hooks and doubled letters are not across parts of speech (sumba "kiss" and
                    # sumbace "to kiss"), nor person-aspect words, which the pack glosses alike (mun, muka, mukan "we").
                    if kind == "glosses" and (not self.kinds[a] & self.kinds[b] or (self.kinds[a] | self.kinds[b]) & {"pron", "particle"}):
                        continue
                    if kind == "glosses" and (a[0] != b[0] or len(a) < 3 or len(b) < 3 or distance(skeleton(a), skeleton(b)) > 2):
                        continue
                    self.same.append({a, b})

    def _form_of(self, word, keys, by_key):
        """The word the pack's entry [word] is a form of (a plural, linked form, feminine or spelling), or None."""
        glosses = self.senses.get(word, [])
        named = [hausa_clean(t).lower() for g in glosses for t in GLOSS_OF.findall(g)]
        named = [t for t in named if WORD.fullmatch(t) and t != word]
        if named:
            return named[0]
        base = word
        # A linked form the pack has as an entry (kiɗan "music (of)", ƙimar "value (of)", hayar beside haya "rent").
        if word[-1] in "nr" and len(word) >= 4:
            stems = [word[:-1]] + ([word[:-2] + "ai"] if word.endswith("an") else [])
            known = [s for s in stems if s in self.kinds]
            # Or a verb's verbal noun so linked (shafar "affecting": shafa "to affect").
            if known and (any("(of)" in g for g in glosses)
                          or not self.kinds[word] & {"verb", "pron", "particle"} and keys.get(word, set()) & keys.get(known[0], set())
                          or self.kinds[word] <= NOMINAL and "verb" in self.kinds[known[0]]
                          and verbal_heads(keys.get(word, set())) & verbal_heads(keys.get(known[0], set()))):
                return known[0]
            if any("(of)" in g for g in glosses):
                base = word[:-1]
        found = self._masculine(base) or self._singular(base, word, keys, by_key)
        if found:
            return found
        return base if base != word else None

    def _masculine(self, word, glosses=None):
        """The masculine or singular adjective a feminine or plural one is of (cikakkiya, ingantattu: cikakke, ingantacce)."""
        for ending, masculine in ADJECTIVE_ENDINGS:
            if word.endswith(ending) and len(word) >= len(ending) + 3 and word[:-len(ending)] + masculine in self.kinds:
                return word[:-len(ending)] + masculine
        # A feminine noun in -iya the pack glosses so (abokiya, mataimakiya: aboki, mataimaki).
        if word.endswith("iya") and word[:-2] in self.kinds and any(
                "fem" in g or "female" in g for g in (glosses if glosses is not None else self.senses.get(word, []))):
            return word[:-2]
        return None

    def _singular(self, word, entry, keys, by_key):
        """The singular of a plural the pack has as an entry: a word beginning alike glossed as its gloss in the singular
        (makonni "weeks": mako "week", ɗaruruwa "hundreds": ɗari "hundred")."""
        if not self.kinds.get(entry, set()) & NOMINAL:
            return None
        # Its first gloss a plural (makwanci "sleeping place; cemeteries" is no plural).
        first = gloss_keys(self.senses.get(entry, [])[:1])
        if not first or not all(english_singular(k) for k in first):
            return None
        found = set()
        for key in keys.get(entry, ()):
            single = english_singular(key)
            if not single:
                continue
            for s in by_key.get(single, ()):
                alike = len(os.path.commonprefix([s.translate(DEHOOK), word.translate(DEHOOK)]))
                # Two letters alike will do for the first gloss (kurakurai "mistakes": kuskure) but for the ma- of the doers
                # and places (mahukunta "authorities" is no plural of malami "official").
                if s != word and s != entry and self.kinds[s] & NOMINAL and len(s) <= len(word) + 1 \
                        and (alike >= 3 and not (word.startswith("ma") and alike < 4)
                             or alike == 2 and key in first and not word.startswith("ma")):
                    found.add(s)
        if not found:
            return None
        return max(found, key=lambda s: (len(os.path.commonprefix([s, word])), -len(s), s))

    def link_bare(self, written):
        """
        Links the pack's linked forms whose word it lacks to that word when text writes it (kolin: koli, ƙirar: ƙira,
        gurɓataccen: gurɓatacce; mataimakiyar: mataimakiya, so mataimaki), and the verbal nouns in -wa the pack has to their
        verbs when it glosses them alike (dubawa "looking": duba "to look", shiryawa: shirya; not kokawa "wrestling" of koka
        "complain").
        """
        for word, kinds in self.kinds.items():
            if word in self.links or word in HAUSA_FORMS or word in self.joined:
                continue
            base = word[:-1]
            # The word written a twentieth as often as its linked form, or at all for an adjective (gagarumi beside gagarumin);
            # no form of another word (kansar "cancer" is no kansa "himself"), nor a loanword in -n or -r (kicin, radar).
            if kinds <= NOMINAL and len(word) >= 5 and base not in self.kinds and base not in self.forms and base not in self.generated \
                    and (word[-1] == "n" and base[-1] in "aeiou" or word[-1] == "r" and base[-1] == "a") \
                    and written.get(base, 0) >= (1 if kinds == {"adj"} else max(1, 0.05 * written.get(word, 0))):
                self.links[word] = self._masculine(base, self.senses.get(word, [])) or base
                continue
            # A verbal noun glossed as one (buɗewa "opening", dubawa "looking"), or glossed as its verb is (wanzuwa "existence":
            # wanzu "to exist").
            verb = word[:-2]
            if not word.endswith("wa") or len(verb) < 3 or not kinds <= {"noun", "verb"}:
                continue
            verbs = [verb] if "verb" in self.kinds.get(verb, ()) else sorted(v for v in self.forms.get(verb, ()) if "verb" in self.kinds[v])
            first = gloss_keys(self.senses.get(word, [])[:1])
            if verbs and (any(k.split()[0].endswith("ing") for k in first)
                          or heads_alike(verbal_heads(gloss_keys(self.senses.get(word, []))),
                                         verbal_heads(gloss_keys(self.senses.get(verbs[0], []))))):
                self.links[word] = verbs[0]

    def _verb(self, word):
        """Whether an entry is a verb: the pack's, or a noun it glosses as one too (gayyata "invitation; (verb) to invite")."""
        kinds = self.kinds[word]
        return "verb" in kinds or kinds <= {"noun"} and any(g.startswith("(verb)") for g in self.senses.get(word, []))

    def split(self, word):
        """The words a joined spelling stands for (nasan: na san, muyi: mu yi), or None: the pack's, and a person-aspect word
        joined to one of the commonest verbs when the pack has no word so spelled."""
        if word in self.joined:
            return self.joined[word]
        if word in self.kinds or word in self.forms or word in self.generated or word in HAUSA_FORMS or word in self.wiktionary:
            return None
        for person in JOINED_PERSONS:
            rest = word[len(person):]
            if word.startswith(person) and rest in JOINED_VERBS:
                return [person, rest]
        return None

    @staticmethod
    def _rule_forms(word, kinds, pack_forms):
        """(form, the linked form it is a possessive of, or None) for the forms the rules make of [word]."""
        out = set()
        # A word of one syllable is made on by its pack forms alone: ya "elder sister" + n is no yan, sa "bull" no san, but
        # ƴaƴa (of ɗa) has ƴaƴan.
        if len(word) < 3:
            word = None
        if kinds & NOMINAL:
            bases = {word} - {None} | {f for f in pack_forms if f[-1] in "aeiou" and len(f) >= 3}
            linked = {f for f in pack_forms if f[-1] in "nr"}
            for base in bases:
                if base[-1] not in "aeiou":
                    continue
                # A plural in -ai is linked in -an (ɗalibai: ɗaliban, abokai: abokan).
                linked.add(base[:-1] + "n" if base.endswith("ai") else base + "n")
                if base.endswith("a"):
                    linked.add(base + "r")
                out.add(base + "na")
                if base.endswith("a"):
                    out.add(base + "ta")
            out = {(f, None) for f in out | linked}
            out |= {(x + p, x) for x in linked for p in POSSESSIVES}
        if word and "verb" in kinds and word[-1] in "aeiou":
            out.add((word + "n", None))
        # A verb's verbal noun in -wa and its linked form (duba: dubawa, dubawar; aiwatar: aiwatarwa).
        if word and "verb" in kinds and (word[-1] in "aeiou" or word.endswith("ar")):
            out |= {(word + "wa", None), (word + "war", None)}
        return out

    def lemmas(self, token):
        """The words [token] is a form of: itself when it is a word, else the pack's words, else those of the rules; each an
        entry of the pack that is another's form (links) counted as that one."""
        if token in HAUSA_FORMS:
            return frozenset(w for w in HAUSA_FORMS[token].split(",") if w)
        if token in self.kinds:
            found = {token}
        elif token in self.forms:
            found = self.forms[token]
        elif token in self.generated:
            found = self.generated[token]
        else:
            return frozenset()
        return frozenset(self.word(w) for w in found)

    def word(self, entry):
        """The word an entry is counted as, following its links."""
        seen = {entry}
        while entry in self.links and self.links[entry] not in seen:
            entry = self.links[entry]
            seen.add(entry)
        return entry


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            yield line.rstrip("\n").partition("\t")[2]


SKELETON_NAME = re.compile(r"[^\W\d_']+")


def _sentences(download):
    """(kind, sentence) for every sentence counted: each everyday sentence once, each Wikipedia one once its names and numbers
    aside ("X ƙauye ne a ƙaramar hukumar Y")."""
    path = download(TATOEBA[1], os.path.join(f"tatoeba-{TATOEBA[0]}", f"{TATOEBA[0]}_sentences.tsv.bz2"))
    seen = set()
    if path:
        with bz2.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                text = line.rstrip("\n").split("\t")[-1].strip()
                if text and text not in seen:
                    seen.add(text)
                    yield "everyday", text
    locale, commit, files = COMMONVOICE
    for name in files:
        path = download(f"https://raw.githubusercontent.com/common-voice/common-voice/{commit}/server/data/{locale}/{name}.txt",
                        os.path.join(f"commonvoice-{locale}", f"{name}.txt"))
        if path:
            with open(path, encoding="utf-8") as f:
                for line in f:
                    text = line.strip()
                    if text and text not in seen:
                        seen.add(text)
                        yield "everyday", text
    for corpus in LEIPZIG_NEWS:
        path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
        if path:
            for text in _leipzig_sentences(path):
                yield "news", text
    for kind, corpus, version in OPUS:
        path = download(f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/ha.txt.gz", f"opus-{corpus}-{version}-ha.txt.gz")
        if path:
            skeletons = set()
            with gzip.open(path, "rt", encoding="utf-8") as f:
                for line in f:
                    text = line.strip()
                    key = re.sub(r"\d+", "0", SKELETON_NAME.sub(lambda m: m.group() if m.group().islower() else "X", text))
                    if not text or key in skeletons:
                        continue
                    skeletons.add(key)
                    yield kind, text


def _counts(download, lexicon):
    """Each kind's word counts, by the plain letters whose hooked letter the sentence writes (hooks_written); the names, and
    how often each word is half of a hyphenated one."""
    counts = {kind: defaultdict(lambda: defaultdict(int)) for kind in WEIGHTS}
    names = defaultdict(int)
    hyphened = defaultdict(int)
    for kind, text in _sentences(download):
        text = text.translate(APOSTROPHES)
        halves = defaultdict(int)
        words = list(tokens(text, names, lexicon.split, halves))
        if not words or english(words) or AJAMI.search(text):
            continue
        written = hooks_written(text, words)
        for word in words:
            counts[kind][word][written & set(word.translate(DEHOOK))] += 1
        for word, n in halves.items():
            hyphened[word] += n
    return counts, names, hyphened


def _join_same(lexicon, share):
    """Links each group of spellings or grades of one word (Lexicon.same) to its commonest member, by its forms' counts."""
    counted = defaultdict(float)
    for form, value in share.items():
        found = lexicon.lemmas(form)
        if len(found) == 1:
            counted[next(iter(found))] += value
    parent = {}

    def root(w):
        while parent.get(w, w) != w:
            w = parent[w]
        return w

    for group in lexicon.same:
        a, b = (root(lexicon.word(w)) for w in group)
        if a != b:
            parent[max(a, b)] = min(a, b)
    members = defaultdict(set)
    for w in parent:
        members[root(w)].add(w)
    for top, group in members.items():
        group = {lexicon.word(w) for w in group | {top}}
        keep = max(group, key=lambda w: (counted.get(w, 0.0), w))
        # The list writes the hooks: a commoner spelling without them is the hooked one's (roka, roƙa "beg"), but for the
        # words of their own so spelled (kadara "property").
        if keep not in HAUSA_PLAIN_WORDS:
            keep = next((w for w in sorted(group) if w != keep and w.translate(DEHOOK) == keep), keep)
        for w in group:
            if w != keep:
                lexicon.links[w] = keep


def resolve_hooks(counts, lexicon):
    """
    Each kind's counts with every plain spelling shared among the hooked words it may stand for (see the module notes).

    A plain spelling in a sentence that writes the hooked letter it lacks is taken as written (kasa "fail" beside ƙasa). Else it
    is shared among itself and its hooked spellings by how often the sentences that write those hooks have each. A plain
    spelling the pack lacks that such sentences have seldom beside the hooked one (MISSPELT_SHARE) is a dropped hook alone.
    """
    # The count of each spelling in the sentences that write each hook.
    evidence = defaultdict(lambda: defaultdict(int))
    for kind_counts in counts.values():
        for word, by_hooks in kind_counts.items():
            for written, n in by_hooks.items():
                for letter in written:
                    evidence[word][letter] += n
                if "ƴ" in word or "y" in word:
                    evidence[word]["y"] += n
    known = set(lexicon.kinds) | set(lexicon.forms) | set(lexicon.generated) | HAUSA_PLAIN_WORDS
    # Spellings taken as words whatever hooked text writes: the Wiktionary's, and their forms. Wikidata has some words without
    # their hooks (bukata, zabe, sako) and some with hooks they lack.
    trusted = lexicon.wiktionary | HAUSA_PLAIN_WORDS
    variants = defaultdict(set)
    for spelling in set(evidence) | known:
        bare = spelling.translate(DEHOOK)
        if bare != spelling:
            variants[bare].add(spelling)

    def shares(word):
        """({spelling: share}, whether [word] is a dropped hook) for a plain [word] whose hooks are not written. A hooked
        spelling no Wiktionary word has that is seldom written beside [word] is a hook written where there is none, and no
        word it may stand for (ɗomin beside domin)."""
        found = []
        # In order, so a tie in max() below falls the same way whatever the hash seed.
        for h in sorted(variants[word]):
            letters = dropped(word, h)
            own = sum(evidence[word][x] for x in letters) / len(letters)
            other = sum(evidence[h][x] for x in letters) / len(letters)
            if h in trusted or other >= MISSPELT_SHARE * (own + other) or h in known and word not in known:
                found.append((h, own, other))
        if not found:
            return {word: 1.0}, False
        weights = {h: other + (PACK_PRIOR if h in known else 0.0) for h, _, other in found}
        # A spelling the pack lacks is a dropped hook of the pack's word (kwakwalwa: ƙwaƙwalwa, kirji: ƙirji) unless text
        # that writes the hook all but never writes that word with it (gaba, not gaɓa "beach"): news and Wikipedia drop ƙ in
        # most words even in sentences that write it in others.
        # Two spellings the pack lacks are one word when the hooked one is written at least as often as HOOKED_SHARE: no one
        # writes hooks a word has not (haƙiƙa, roƙo, gargaɗi beside hakika, roko, gargadi).
        misspelt = word not in trusted and any(own < MISSPELT_SHARE * (own + other)
                                               or word not in known and h in known and own < PACK_TWIN_SHARE * (own + other)
                                               or word not in known and other >= HOOKED_SHARE * (own + other)
                                               for h, own, other in found if other > 0)
        if not misspelt:
            _, own, _ = max(found, key=lambda x: x[2])
            weights[word] = own + (PACK_PRIOR if word in known else 0.0)
        total = sum(weights.values())
        return ({x: w / total for x, w in weights.items()} if total else {h: 1 / len(found) for h, _, _ in found}), misspelt

    cache = {}
    out = {}
    for kind, kind_counts in counts.items():
        resolved = defaultdict(float)
        for word, by_hooks in kind_counts.items():
            hooked = variants.get(word)
            # A hook written where there is none (ɗomin, ɓabu, ƴanzu): a hooked spelling no Wiktionary word has, that the
            # text writing that hook has seldom beside the plain one, is the plain word.
            bare = word.translate(DEHOOK)
            if bare != word and word not in trusted and bare in evidence:
                letters = dropped(bare, word)
                own = sum(evidence[word][x] for x in letters if x != "y") + (evidence[word]["y"] if "y" in letters else 0)
                plain_n = sum(evidence[bare][x] for x in letters if x != "y") + (evidence[bare]["y"] if "y" in letters else 0)
                if own < MISSPELT_SHARE * (own + plain_n):
                    resolved[bare] += sum(by_hooks.values())
                    continue
            if hooked and word not in cache:
                cache[word] = shares(word)
            for written, n in by_hooks.items():
                # Written as it is in text that writes its hooks, unless it is a dropped hook even there (din for ɗin).
                if not hooked or all(dropped(word, h) <= written for h in hooked) and not cache[word][1]:
                    resolved[word] += n
                    continue
                for spelling, part in cache[word][0].items():
                    resolved[spelling] += n * part
        out[kind] = resolved
    return out


def _english_words():
    words = set()
    if os.path.exists(ENGLISH_LIST):
        with open(ENGLISH_LIST, encoding="utf-8") as f:
            for i, line in enumerate(f):
                if line.startswith("#"):
                    continue
                if i > 20000:
                    break
                word, _, forms = line.rstrip("\n").partition("\t")
                words.add(word.lower())
                words.update(forms.split())
    return words


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the pack's words and the everyday words it lacks, by their forms' counts, each kind weighted by WEIGHTS. A
    word the pack lacks is no name (written capitalised more often than not) and no word of one letter."""
    lexicon = Lexicon(dictionaries)
    counts, names, hyphened = _counts(download, lexicon)
    written = defaultdict(int)
    for kind_counts in counts.values():
        for word, by_hooks in kind_counts.items():
            written[word] += sum(by_hooks.values())

    def halved(form):
        """Whether a spelling is mostly written as half of a hyphenated word (canje, ire, fina), with or without its hooks."""
        spellings = {form, form.translate(DEHOOK)}
        total = sum(written.get(f, 0) for f in spellings)
        return total > 0 and sum(hyphened.get(f, 0) for f in spellings) >= HYPHEN_SHARE * total

    counts = resolve_hooks(counts, lexicon)
    english_words = _english_words()
    label = (f"Tatoeba hau ({LICENCES['tatoeba']}) + Common Voice ha sentences ({LICENCES['commonvoice']}) + Leipzig Corpora "
             f"Collection {', '.join(LEIPZIG_NEWS)} ({LICENCES['leipzig']}) + OPUS wikimedia {OPUS[0][2]} ha ({LICENCES['wikimedia']})")
    share = defaultdict(float)
    raw = defaultdict(float)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        if not total:
            continue
        for form, n in kind_counts.items():
            share[form] += WEIGHTS[kind] * n / total
            raw[form] += n

    def listable_alone(form):
        """Whether a word the pack lacks is listed for itself: everyday text has it, or news and Wikipedia both do; it has
        no foreign letter and is no English word that Hausa text seldom has."""
        if form in HAUSA_EXCLUDED or FOREIGN_LETTERS.search(form) or len(form) < 2:
            return False
        everyday = counts["everyday"].get(form, 0.0)
        news = counts["news"].get(form, 0.0)
        wiki = counts["wikipedia"].get(form, 0.0)
        # A name: written capitalised inside sentences more often than small anywhere (Ibrahim, Babangida).
        if names.get(form, 0) > everyday + news + wiki:
            return False
        if form in english_words and everyday < 3:
            return False
        return everyday >= 2 or (news >= 3 and wiki >= 3)

    lexicon.link_bare(written)
    _join_same(lexicon, share)
    heads = {}
    for form in share:
        found = lexicon.lemmas(form)
        if not found and form not in HAUSA_FORMS and listable_alone(form):
            found = frozenset({form})
        heads[form] = found
    # A possessive the rules make that text has more than the linked form it is made on is a word of its own, a verb in -nta
    # or -rta (kwatanta "compare" is no kwatan + ta).
    for form, linked in lexicon.possessive_of.items():
        if form.endswith("anta") and form in share and form not in lexicon.kinds and form not in lexicon.forms and form not in HAUSA_FORMS \
                and share[form] > share.get(linked, 0.0) and listable_alone(form):
            heads[form] = frozenset({form})
    # A noun the pack lacks has its linked form too (daɗi: daɗin, ƙarfi: ƙarfin, ƙungiya: ƙungiyar), when that is no word of
    # the pack's itself. Not after a short word in -a: the little words have words of their own in -an and -ar (kamar "like"
    # is no form of kama "catch", nor daban "different" of daba, sukan of suka, akan of aka).
    for form in share:
        stem = form[:-1]
        if form[-1:] in ("n", "r") and len(stem) >= 4 and stem[-1] in "aeiou" and form not in lexicon.kinds \
                and form not in HAUSA_FORMS and stem not in lexicon.kinds and heads.get(stem) == frozenset({stem}) \
                and (stem[-1] in "eiou" and form[-1] == "n" or stem[-1] == "a" and len(stem) >= 5):
            heads[form] = heads[stem]
    # And its possessives (ƙarfinsa, shirinsa, manufarta): a linked form with a possessive ending.
    for form in share:
        stem = form[:-2]
        if form[-2:] in POSSESSIVES and stem[-1:] in ("n", "r") and len(stem) >= 4 and form not in lexicon.kinds \
                and form not in lexicon.forms and form not in lexicon.generated and form not in HAUSA_FORMS \
                and heads.get(stem) and heads.get(form) in (frozenset(), frozenset({form})):
            heads[form] = heads[stem]
    for form in heads:
        if halved(form):
            heads[form] = frozenset()
    own = defaultdict(float)
    for form, value in share.items():
        if len(heads[form]) == 1:
            own[next(iter(heads[form]))] += value
    scores = defaultdict(float)
    totals = defaultdict(float)
    forms = defaultdict(dict)
    for form, value in share.items():
        words = heads[form]
        if not words:
            continue
        weights = {w: own.get(w, 0.0) for w in sorted(words)}
        whole = sum(weights.values())
        for word in words:
            part = weights[word] / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            scores[word] += value * part
            totals[word] += raw[form] * part
            forms[word][form] = forms[word].get(form, 0.0) + value * part

    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT and w not in HAUSA_EXCLUDED][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        # A spelling without the hook (yaki of yaƙi) is counted for its word but not listed: the list writes the hooks.
        hooked = set(forms[word])
        plain = {f for f in hooked if f.translate(DEHOOK) == f and (f in HAUSA_FORMS or f not in lexicon.kinds and f not in lexicon.forms)
                 and any(g != f and g.translate(DEHOOK) == f for g in hooked)}
        listed = sorted((f for f in forms[word] if f not in plain), key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


def checker_word(token, ranks):
    """
    The list's form [token] is made on by the possessive and linker rules, the commonest when it may be several, or None: a
    possessive the list does not have (gidanmu: gidan, motarsa: motar, ɗalibansu: ɗaliban), a linked form (gonar: gona).
    """
    stems = [token[:-len(p)] for p in POSSESSIVES + ("na",) if token.endswith(p) and len(token) > len(p) + 2]
    found = []
    for stem in stems + [token]:
        if stem in ranks and stem != token:
            found.append((ranks[stem], stem))
        if stem[-1:] in ("n", "r") and len(stem) > 3 and stem[:-1] in ranks:
            found.append((ranks[stem[:-1]], stem[:-1]))
    return min(found)[1] if found else None

