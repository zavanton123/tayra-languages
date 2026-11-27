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
# A word whose uses are at least this share in one fixed pair or beside one name, and that has at least PHRASE_LEAST uses, is
# no word of the list (Neighbours), nor one written as a name more than NAME_RATIO times as often as not.
PHRASE_SHARE = 0.6
PHRASE_LEAST = 10
NAME_RATIO = 5
# The words a verb's phrase may have that leave it a word of its own (ladabtar da, cim ma, yi wa).
PHRASE_PARTICLES = {"da", "wa", "ma"}
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
    # Grades and verbal nouns of irregular verbs or of verbs the pack spells or files otherwise (_fold_heads takes the regular
    # ones): biye "following" (bi), karye (karya "break"), taho (tafi), gaishe and gaisa (gaisuwa), shayar "give a drink" (sha),
    # yaƙar "fight against" (yaƙi), nutsu (nutsuwa), girme (girma), kasu "be divided" (kasa), tsaida (tsayar), marawa of
    # mara wa baya "support", yiyuwa (yiwuwa), jiyo (ji), dau (ɗauka), dena (daina), addabi and addabe (addaba), and the
    # possessives kowannensu and yinsa.
    "biye": "bi", "karye": "karya", "taho": "tafi", "gaishe": "gaisuwa", "shayar": "sha", "yaƙar": "yaƙi", "nutsu": "nutsuwa",
    # Spellings: kwai without its hook, ƙalla of a ƙalla (aƙalla), haki for haƙƙi, toh, ameen, waƴanda, memba (mamba), kome
    # (komai), mahimman (muhimmi), jar (ja), yinwa (yunwa), sa'annan (sannan), and the plural mafiya.
    "kwai": "ƙwai", "ƙalla": "aƙalla", "haki": "haƙƙi", "daddadare": "daddare", "halasta": "halatta", "toh": "to",
    "ameen": "amin", "waƴanda": "wanda", "memba": "mamba", "mafiya": "mafi", "kome": "komai", "mahimman": "muhimmi",
    "jar": "ja", "tsaida": "tsayar", "marawa": "mara", "gaisa": "gaisuwa", "girme": "girma", "kasu": "kasa",
    "yiyuwa": "yiwuwa", "jiyo": "ji", "dau": "ɗauka", "dena": "daina", "addabi": "addaba", "addabe": "addaba",
    "kowannensu": "kowanne", "yinsa": "yin", "yinta": "yin", "yinwa": "yunwa", "sa'annan": "sannan",
    # Spellings the writers reported: kanwa for ƙanwa "younger sister" (not kanwa "potash", which text seldom has), diba and
    # ɗibar for ɗiba "scoop", dumi for ɗumi "warmth" (no ɗuma "beat"), mutuƙar for matuƙar, ɗumbin for ɗimbin, waƙanda for
    # waɗanda; the plurals ƙananu (ƙarami) and fukafukai (fiffike), and dahu, the grade 7 of dafa "cook".
    "kanwa": "ƙanwa", "diba": "ɗiba", "ɗiba": "ɗiba", "ɗibar": "ɗiba", "dumi": "ɗumi", "ɗumi": "ɗumi", "mutuƙar": "matuƙa",
    "mutukar": "matuƙa", "ɗumbin": "ɗimbin", "waƙanda": "wanda", "ƙananu": "ƙarami", "kananu": "ƙarami",
    "fukafukai": "fiffike", "fikafikai": "fiffike", "fikafikan": "fiffike", "dahu": "dafa",
    # Plurals and linked forms of words the rules do not reach: zawarawa (bazawara "divorcee"), muggan (mugu), shahararen
    # (shahararre).
    "zawarawa": "bazawara", "zawarawan": "bazawara", "muggan": "mugu", "shahararen": "shahararre",
    # Feminines the rules do not make (musulma "Muslim woman", makauniya "blind woman") and spellings (murmurshi, ainahi).
    "musulma": "musulmi", "musulmar": "musulmi", "makauniya": "makaho", "makauniyar": "makaho", "murmurshi": "murmushi",
    "ainahi": "ainihi", "ainahin": "ainihi", "yanyin": "yanayi", "hukumci": "hukunci", "hukumcin": "hukunci",
    # Plurals whose glosses the rules do not match to their singular's: akidu "beliefs" (aƙida "ideology"), rukuna (rukuni).
    "akidu": "aƙida", "aƙidu": "aƙida", "rukuna": "rukuni", "rukunan": "rukuni",
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
    "kaima": "kai ma", "kema": "ke ma", "farinciki": "farin ciki", "kodayaushe": "ko da yaushe", "sukai": "suka yi",
    "za'ayi": "za a yi",
    "kune": "ku ne", "mune": "mu ne", "acan": "a can", "kauda": "kau da", "yai": "ya yi", "naaji": "na ji",
    "ɗansanda": "ɗan sanda", "ɗansandan": "ɗan sandan", "dansanda": "dan sanda", "dansandan": "dan sandan",
    "yanzunnan": "yanzu nan", "ɗinnan": "ɗin nan", "dinnan": "din nan", "kodai": "ko dai",
    "gareni": "gare ni", "gareta": "gare ta", "garesu": "gare su", "gareshi": "gare shi", "garemu": "gare mu",
    "amatsayin": "a matsayin", "aƙasar": "a ƙasar", "akasar": "a kasar", "yakara": "ya ƙara", "aci": "a ci",
    "baza'a": "ba za a", "ba'ayi": "ba a yi", "watarana": "wata rana",
}
# Words that are no Hausa words of ordinary text: names written small, English, abbreviations, fragments (basuwa, written for
# ba su), the linker n written apart.
HAUSA_EXCLUDED = {"allah", "adam", "s", "of", "habibu", "on", "n", "basuwa", "ƙwa", "by", "zoonotic", "sabin", "cen", "yaƙ", "um",
                  "em", "er", "huh", "ugh", "aha", "zu", "yakamat", "serology"}
# A spelling no Hausa word has: two consonants at the start but the digraphs and the w and y after a consonant (ƙsar, gyra),
# or the same consonant twice at the end (saww).
UNHAUSA = re.compile(r"^(?!sh|ts|[kgƙ][wy]|'[bdyƴ])[^aeiou'][^aeiouwy]|^[kgƙ][wy][^aeiou]|([^aeiou])\1$")

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
# The relative continuous person-aspect words, which are joined to a verb as the others are (sukeyi: suke yi).
JOINED_RELATIVE = {"yake", "take", "suke", "nake", "muke", "kuke"}
# Object pronouns text joins to the verb before them (kasheta, haɗasu, sameshi: kashe ta, haɗa su, same shi), and the least
# count of a verb (all its spellings) that is so split.
JOINED_OBJECTS = ("shi", "ta", "su", "mu", "ku", "ni")
JOINED_OBJECT_VERB = 20
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


# Words of a gloss that say nothing of its sense.
GLOSS_STOP = {"to", "be", "a", "an", "the", "of", "up", "out", "over", "on", "in", "into", "from", "for", "with", "something",
              "someone", "somebody", "oneself", "become", "get", "getting", "make", "do", "one", "by", "verb", "as", "at", "or",
              "and", "off", "down", "away", "about", "very", "much", "many", "being", "thing", "things", "kind", "type"}


def glossed_alike(a, b):
    """Whether two lists of glosses share a word of four letters or more, or the first five letters of one (spoil, spoiled;
    tired, tiredness), what is in brackets aside."""
    def words(glosses):
        found = {w for g in glosses for w in re.findall(r"[a-z]+", GLOSS_PARENS.sub(" ", g).lower())
                 if len(w) >= 4 and w not in GLOSS_STOP}
        return found | {w[:-1] for w in found if w.endswith("s") and not w.endswith("ss")}
    return any(x[:5] == y[:5] for x in words(a) for y in words(b))


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


def tokens(line, names=None, split=None, hyphened=None, marks=False):
    """A line's words as the reader cuts and matches them, lowercase, without the capitalised ones inside a sentence (names),
    which are counted in [names]. [split] gives the words a joined spelling stands for (Lexicon.split); the words that are
    half of a hyphenated one are counted in [hyphened]. With [marks], a name is given as NAME_MARK and its spelling, and an
    abbreviation or number as NAME_MARK alone, where a word would be."""
    line = line.translate(APOSTROPHES)
    end = 0
    for match in WORD.finditer(line):
        word = match.group(0)
        first = word.lstrip("'")
        if marks and any(c.isdigit() for c in line[end:match.start()]):
            yield NAME_MARK
        end = match.end()
        # A word in capitals is an abbreviation (NGO, FCDA) or a heading's, and no evidence of how a word is written.
        if len(first) > 1 and first.isupper():
            if marks:
                yield NAME_MARK
            continue
        if first[:1].isupper() and not SENTENCE_START.search(line[:match.start()]):
            if names is not None:
                names[hausa_fold(word).lower()] += 1
            if marks:
                yield NAME_MARK + hausa_fold(word).lower()
            continue
        word = hausa_fold(word).lower()
        if hyphened is not None and _hyphened(line, match.start(), match.end()):
            hyphened[word] += 1
        words = HAUSA_JOINED.get(word, word).split()
        if split is not None and len(words) == 1:
            words = split(word) or words
        yield from words


# What tokens(marks=True) gives for a name (NAME_MARK and its spelling), an abbreviation or a number (NAME_MARK alone).
NAME_MARK = "\0"


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
                # "(fused bai da) has no", and the first words of a longer phrase ("(ba tare da) without": batare).
                parts = [hausa_clean(p).lower() for p in text.split()]
                parts = parts[1:] if parts[:1] == ["fused"] else parts
                for k in range(len(parts), 1, -1):
                    if "".join(parts[:k]) == word and all(WORD.fullmatch(p) for p in parts[:k]):
                        self.joined[word] = parts[:k]
                        break
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
        for person in JOINED_PERSONS + sorted(JOINED_RELATIVE):
            rest = word[len(person):]
            if word.startswith(person) and rest in JOINED_VERBS:
                return [person, rest]
            # The future with its person and verb (zasuyi: za su yi).
            if person == "za" and word.startswith(person) and rest:
                more = self.split(rest)
                if more and more[0] in JOINED_PERSONS:
                    return [person] + more
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


# The person-aspect words a verb follows (ya tafi, sun komo, za su kashe, suka karɓe), not those that are words of other
# kinds too (ta "of", na "of", a "at", ka, ki).
SUBJECTS = {"ya", "sun", "mun", "kun", "an", "zai", "zan", "za", "su", "mu", "ku", "suka", "aka", "muka", "kuka", "yakan",
            "takan", "sukan", "mukan", "kukan", "nakan"}


class Neighbours:
    """The word most often before and after each word, if one is more than half the time: a majority vote (Boyer and Moore)
    over the words around it, names (NAME_MARK) among them, so a word of a fixed phrase or a name (kwanton ɓauna, zirin Gaza)
    can be told."""

    def __init__(self):
        self.votes = ({}, {})
        self.total = defaultdict(int)

    def add(self, sequence):
        for i, word in enumerate(sequence):
            if word.startswith(NAME_MARK):
                continue
            self.total[word] += 1
            around = (sequence[i - 1] if i else "", sequence[i + 1] if i + 1 < len(sequence) else "")
            for votes, other in zip(self.votes, around):
                candidate, n = votes.get(word, ("", 0))
                if other == candidate:
                    votes[word] = (candidate, n + 1)
                elif n:
                    votes[word] = (candidate, n - 1)
                else:
                    votes[word] = (other, 1)

    def fixed(self, word, total):
        """The word [word] is a fixed pair with, or the name it is written beside, and at least the share of its [total] uses
        so written (a share is at least (votes left + uses) / 2 uses), or None: a name, or a word whose own majority on the
        other side is [word] (kwanton, ɓauna)."""
        best = None
        for side, votes in enumerate(self.votes):
            other, n = votes.get(word, ("", 0))
            if not other or not total or other == NAME_MARK:
                continue
            if not other.startswith(NAME_MARK):
                back, m = self.votes[1 - side].get(other, ("", 0))
                if back.translate(DEHOOK) != word.translate(DEHOOK) or (m + self.total.get(other, 0)) < PHRASE_SHARE * 2 * self.total.get(other, 0):
                    continue
            part = (n + total) / (2 * total)
            if not best or part > best[1]:
                best = (other, part)
        return best


def _counts(download, lexicon):
    """Each kind's word counts, by the plain letters whose hooked letter the sentence writes (hooks_written); the names, how
    often each word is half of a hyphenated one, how often it follows a person-aspect word (SUBJECTS) as a verb does, and the
    words around it (Neighbours)."""
    counts = {kind: defaultdict(lambda: defaultdict(int)) for kind in WEIGHTS}
    names = defaultdict(int)
    hyphened = defaultdict(int)
    after_subject = defaultdict(int)
    neighbours = Neighbours()
    for kind, text in _sentences(download):
        text = text.translate(APOSTROPHES)
        halves = defaultdict(int)
        sequence = list(tokens(text, names, lexicon.split, halves, marks=True))
        words = [w for w in sequence if not w.startswith(NAME_MARK)]
        if not words or english(words) or AJAMI.search(text):
            continue
        written = hooks_written(text, words)
        for i, word in enumerate(words):
            counts[kind][word][written & set(word.translate(DEHOOK))] += 1
            if i and words[i - 1] in SUBJECTS:
                after_subject[word] += 1
        for word, n in halves.items():
            hyphened[word] += n
        neighbours.add(sequence)
    return counts, names, hyphened, after_subject, neighbours


def _split_objects(counts, after_subject, lexicon):
    """Counts a spelling the pack lacks that is a verb and its object pronoun (haifeshi, kasheta, sameshi: haife shi, kashe
    ta, same shi) as its words, in place: a verb by text (GRADE_VERB) counted at least JOINED_OBJECT_VERB times, no other word
    of the pack's without its hooks (kasata is ƙasata, not kasa ta)."""
    written = defaultdict(int)
    for kind_counts in counts.values():
        for word, by_hooks in kind_counts.items():
            written[word] += sum(by_hooks.values())
    known = set(lexicon.kinds) | set(lexicon.forms) | set(lexicon.generated) | lexicon.wiktionary | set(HAUSA_FORMS)
    plain = {w.translate(DEHOOK) for w in known}
    for word in sorted(written):
        if word in known or word.translate(DEHOOK) in plain:
            continue
        for pronoun in JOINED_OBJECTS:
            verb = word[:-len(pronoun)]
            if not word.endswith(pronoun) or len(verb) < 4 or verb[-1] not in "aeiou":
                continue
            if written[verb] >= JOINED_OBJECT_VERB and after_subject.get(verb, 0) >= GRADE_VERB * written[verb]:
                for kind_counts in counts.values():
                    for hooks, n in kind_counts.pop(word, {}).items():
                        kind_counts[verb][hooks] += n
                        kind_counts[pronoun][frozenset()] += n
                break


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


# ---------------------------------------------------------------------------------------------------------------------------
# Words that are forms of other words of the list (_fold_heads).
#
# The pack lacks many verbs or has them as nouns (koma "fishing net", taru, shaida "witness"), so their grades, verbal nouns
# and possessives were listed apart. Text shows a verb by what goes before it: a verb follows a person-aspect word (ya koma,
# sun komo) much more often than a noun does (SUBJECTS). A word is folded under the word the list has, which is commoner:
# - a word whose own spelling the pack makes another's form (rushewa, naɗi, jihohi: rushe, naɗa, jiha);
# - a grade, the verb's last vowel changed to -e, -o, -u or -i (komo, tono, karɓe, rarrabu: koma, tona, karɓa, rarraba),
#   when both are verbs by text; to -a's first, then -i's and -e's;
# - a verbal noun in -wa (kashewa, iyawa, gamsuwa, taruwa: kashe, iya, gamsu, taru) or a grade 7 one in -uwa (faɗuwa,
#   tsayuwa: faɗa, tsaya) a verb has, glossed as one or not in the pack, written less than VERBAL_NOUN_SHARE as often as its
#   verb (rayuwa "life", kasuwa "market", mutuwa "death" are words of their own);
# - a feminine in -uwa of a word in -o or in -iya of one in -i (tsohuwa, tauraruwa, jaririya: tsoho, tauraro, jariri),
#   glossed so or not in the pack;
# - a possessive in -na or -ta on the word's last vowel (ganina, wajena, kyamarata: gani, waje, kyamara), no verb by text
#   and not in the pack unless glossed so ("my daughter").
# Grades that are words of their own stay (OWN_GRADES), and so do the grade 5 verbs in -ar (warkar, karantar).
# ---------------------------------------------------------------------------------------------------------------------------
# Of the uses of a word, the share after a person-aspect word that makes it a verb for a grade, for the verb a grade is of,
# for a verb a verbal noun is of, and below which a possessive is no verb.
GRADE_VERB = 0.3
BASE_VERB = 0.15
NOUN_VERB = 0.05
POSSESSIVE_VERB = 0.15
VERBAL_NOUN_SHARE = 0.7
# Grades and spellings alike that are words of their own: faru "happen" (fara "begin"), saki "divorce, release" (saka, sake),
# kori "chase away" (kore "green"), biyo "follow hither" (biya "pay"), zame "slip" (zama), zaɓi "choice" (zaɓe "election"),
# tuntuɓe "stumbling" (tuntuɓi "contact"), aike "errand" (aika), fito "come out" (fita), lamunci "tolerate" (lamunce
# "guarantee"), rame "grow thin" (rama), ɗago "lift" (ɗage "postpone"), kunno "emerge" (kunna "switch on").
OWN_GRADES = {"faru", "saki", "kori", "biyo", "zame", "zaɓi", "tuntuɓe", "aike", "fito", "lamunci", "rame", "ɗago", "kunno",
              "damu", "zare", "huce", "kullu", "gabaci", "ƙare", "daɗe", "bushe", "shanye", "farke", "tsare", "ɗage", "rayu"}
# The words HAUSA_FORMS files forms under, which are words whatever their spelling (karya "break" beside ƙarya "lie").
HAND_WORDS = {w for words in HAUSA_FORMS.values() for w in words.split(",") if w}
FEMININE_GLOSS = re.compile(r"\b(?:fem|female|woman|girl|heroine)\b")
# A gloss that names an action or state, as a verbal noun's does (killing, satisfaction, ability, failure).
VERBAL_GLOSS = re.compile(r"^\S*(?:ing|ion|ment|ance|ence|ity|ness|ure|al)$")
# A gloss of a possessed word or of a verbal noun with its object ("their habitats", "(bi shi) following him").
POSSESSIVE_GLOSS = re.compile(r"\b(?:my|his|her|its|our|your|their|him|them|us|me)\b")


def _fold_heads(heads, share, written, after_subject, lexicon):
    """Folds the words of [heads] (form: its words) that are grades, verbal nouns, feminines or possessives of others (see
    the notes above), in place."""
    score = defaultdict(float)
    for form, words in heads.items():
        if len(words) == 1:
            score[next(iter(words))] += share[form]

    def head(w):
        return score.get(w, 0.0) > 0 and heads.get(w, frozenset()) in (frozenset(), frozenset({w}))

    unhooked = defaultdict(set)
    plainly = defaultdict(set)
    for w in score:
        if head(w):
            unhooked[w.translate(DEHOOK)].add(w)
            plainly[w.translate(DEHOOK).replace("'", "")].add(w)

    def listed(w):
        """The word of the list [w] is counted as: itself, the one word its spelling is a form of (fice: fita; not a grade 5
        verb in -ar, which the pack has as a linked form: bayar "give" is no baya "back"), or its hooked spelling (dauka:
        ɗauka), or None."""
        if head(w):
            return w
        if len(heads.get(w, ())) == 1 and head(next(iter(heads[w]))) and not w.endswith("r"):
            return next(iter(heads[w]))
        found = unhooked.get(w.translate(DEHOOK), set())
        return next(iter(found)) if len(found) == 1 else None

    def verbness(w):
        spellings = {w, w.translate(DEHOOK)}
        n = sum(written.get(x, 0) for x in spellings)
        return sum(after_subject.get(x, 0) for x in spellings) / n if n else 0.0

    def verbal(x, verb):
        """Whether the pack has [x] as a verbal noun of [verb], or not at all."""
        if not lexicon.senses.get(x):
            return True
        keys = gloss_keys(lexicon.senses.get(x, [])[:3])
        return "verb" in lexicon.kinds.get(x, ()) or any(VERBAL_GLOSS.match(k.split()[0]) for k in keys) \
            or heads_alike(verbal_heads(set(keys)), verbal_heads(gloss_keys(lexicon.senses.get(verb, []))))

    def pack_verb(w):
        return "verb" in lexicon.kinds.get(w, ())

    def spelt(w):
        """[w] as a word of the list, or its hooked spelling (akida: aƙida); not a form of another word."""
        if head(w):
            return w
        found = unhooked.get(w.translate(DEHOOK), set())
        return next(iter(found)) if len(found) == 1 else None

    def singulars(x):
        """The listed words a plural's shape makes its singular, the commonest first (see singular)."""
        if len(x) < 5 or verbness(x) >= NOUN_VERB:
            return []
        stems = []
        if re.fullmatch(r".+o[^aeiou']i", x) or x.endswith("una"):
            stems.append(x[:-3])
        if x.endswith("aye") or re.fullmatch(r".*([^aeiou'])a\1e", x):
            stems.append(x[:-3])
        if x.endswith(("ai", "u")):
            stems.append(x[:-2] if x.endswith("ai") else x[:-1])
        # The doers in ma-...-ci have plurals in ma-...-ta (makusanci, makusanta; mabuƙaci, mabuƙata).
        if x.startswith("ma") and x.endswith("ta"):
            stems = [x[:-2] + "c"]
        found = {w for w in (spelt(st + v) for st in stems for v in ("i" if x.endswith("ta") and x.startswith("ma") else "aeiou"))
                 if w and w != x}
        return sorted(found, key=lambda w: (-score.get(w, 0.0), w))

    def singular(x, plain=False):
        """The listed singular of a plural (rundunoni, rokoki, muhallai, akidu, wajaje, makusanta: runduna, roka, muhalli,
        akida, waje, makusanci): a plural the pack glosses as one, or one the pack lacks that is rarer than its singular;
        with [plain], any form so made (takwarori of takwarorinsu)."""
        found = singulars(x)
        if not found:
            return None
        word = found[0]
        if plain:
            return word
        glosses = lexicon.senses.get(x, [])
        first = gloss_keys(glosses[:1])
        if glosses:
            single = [g if not english_singular(g) else english_singular(g) for g in glosses]
            if (first and all(english_singular(k) for k in first) or x.startswith("ma")) \
                    and (not lexicon.senses.get(word) or glossed_alike(single, lexicon.senses.get(word, []))):
                return word
            return None
        return word if score.get(word, 0.0) > score.get(x, 0.0) and not lexicon.kinds.get(x) else None

    def masculine(x):
        """The listed masculine singular of a feminine or plural adjective (zaɓaɓɓu, haifaffiya, gurɓatattu: zaɓaɓɓe,
        haifaffe, gurɓatacce), or of its linked form (gurɓatacciyar, fasassun)."""
        if verbness(x) >= NOUN_VERB:
            return None
        if x[-1:] in ("n", "r") and x[:-1].endswith(tuple(e for e, _ in ADJECTIVE_ENDINGS) + ("iya", "u")):
            return masculine(x[:-1])
        for ending, male in ADJECTIVE_ENDINGS:
            if x.endswith(ending) and len(x) >= len(ending) + 3 and listed(x[:-len(ending)] + male):
                return listed(x[:-len(ending)] + male)
        m = re.fullmatch(r"(.+)(.)\2(?:iya|u)", x)
        return listed(m.group(1) + 2 * m.group(2) + "e") if m and m.group(2) not in "aeiou'" else None

    def linked_word(stem):
        """The word a linked form is of, when the list has it (bin, takwarorin, muhallan: bi's bin, takwara, muhalli)."""
        word = listed(stem)
        if word:
            return word
        if stem[-1:] not in ("n", "r") or len(stem) < 4:
            return None
        bare = stem[:-1]
        for form in [bare] + ([stem[:-2] + "ai"] if stem.endswith("an") else []):
            word = listed(form) or singular(form, plain=True)
            if word:
                return word
        return None

    def possessed(x):
        """The word of a possessive or a verbal noun with its object the pack has as an entry (muhallansu "their habitats",
        hayyacinta "her senses", binsa "following him"): its linked form's word, or that form without its linker when text
        has it."""
        glosses = lexicon.senses.get(x, [])
        ending = next((p for p in POSSESSIVES if x.endswith(p)), None)
        if not ending or not any(POSSESSIVE_GLOSS.search(g) for g in glosses[:2]):
            return None
        stem = x[:-2]
        if stem[-1:] not in ("n", "r") or len(stem) < 3 or verbness(x) >= POSSESSIVE_VERB:
            return None
        return linked_word(stem) or (stem[:-1] if len(stem) >= 5 and written.get(stem[:-1], 0) + written.get(stem, 0) else None)

    def linked(x):
        """The word of a linked form the pack has as an entry, glossed as its word is or not at all, and no verb by text
        (mamatan "the dead", muguntar "wickedness", safofin "socks": mamaci, mugu, safa)."""
        if x[-1:] not in ("n", "r") or len(x) < 6 or pack_verb(x) or verbness(x) >= NOUN_VERB:
            return None
        bare = x[:-1]
        senses = lexicon.senses.get(x, [])
        for word in singulars(bare) + [listed(bare), x.endswith("an") and listed(x[:-2] + "ai")]:
            if word and word != x and score[word] > score.get(x, 0.0) and (
                    not senses or glossed_alike(senses, lexicon.senses.get(word, []) + lexicon.senses.get(bare, []))):
                return word
        return None

    def grade_base(x):
        """The verb a grade in -e, -o, -u or -i is of (komo, karɓe, rarrabe, ruɗe, mirgine, juye: koma, karɓa, rarraba, ruɗa,
        mirgina, juya; haƙiƙance: haƙiƙanta), the -a verb first: one the text has as a verb as it does the grade, or one the
        pack has as a verb as it does the grade, whichever is commoner."""
        if len(x) < 4 or x[-1] not in "eoui":
            return None
        m = GRADE_STEM.search(x[:-1])
        for stem in [x[:-1]] + ([x[:-1][:m.start()] + PALATAL[m.group(0)]] if m else []):
            for v in "aie":
                b = stem + v
                if b == x or not head(b):
                    continue
                if stem == x[:-1] and verbness(x) >= GRADE_VERB and score[b] > score.get(x, 0.0) and verbness(b) >= BASE_VERB:
                    return b
                # Its main senses alike (damu "worry" is no grade of dama "chance").
                if pack_verb(x) and pack_verb(b) and v == "a" \
                        and glossed_alike(lexicon.senses.get(x, [])[:3], lexicon.senses.get(b, [])[:2]):
                    return b
                # Both verbs of the pack and the grade one by text, however glossed (ruɗe "confuse": ruɗa "deceive").
                if stem == x[:-1] and v == "a" and pack_verb(x) and pack_verb(b) and verbness(x) >= GRADE_VERB:
                    return b
        # A grade 6 in -wo of a verb in a vowel (kirawo: kira).
        if x.endswith("wo") and len(x) >= 6 and listed(x[:-2]) and (pack_verb(x) or verbness(x) >= GRADE_VERB) \
                and (pack_verb(x[:-2]) or verbness(x[:-2]) >= BASE_VERB):
            return listed(x[:-2])
        return None

    def base(x):
        # A form by hand whose word is listed (ƙananu: ƙarami; dumi, counted through the pack's dumin: ɗumi).
        hand = HAUSA_FORMS.get(x)
        if hand and "," not in hand and hand != x and listed(hand):
            return listed(hand)
        if x in OWN_GRADES or x in HAUSA_FORMS or x in HAND_WORDS:
            return None
        # A word whose own spelling is counted as another's form (rushewa: rushe, naɗi: naɗa).
        if len(heads.get(x, ())) == 1 and x not in heads[x] and head(next(iter(heads[x]))):
            return next(iter(heads[x]))
        # A spelling without the hooks or the apostrophe of a commoner word the list has (dumi, alumma, jamaa, d'aya: ɗumi,
        # al'umma, jama'a, ɗaya), that the pack lacks or glosses as that word (not kasa "fail" beside ƙasa "land").
        if x not in HAUSA_PLAIN_WORDS:
            spelled = (unhooked.get(x, set()) if x == x.translate(DEHOOK) else set()) \
                | (plainly.get(x.translate(DEHOOK).replace("'", ""), set()) if "'" not in x or "'" in x[1:] else set())
            spelled = {w for w in spelled - {x} if w.translate(DEHOOK) != x.translate(DEHOOK) or x == x.translate(DEHOOK)}
            if len(spelled) == 1:
                word = next(iter(spelled))
                if x not in lexicon.kinds and score[word] > score[x] or score[word] > 2 * score[x] \
                        and glossed_alike(lexicon.senses.get(x, []), lexicon.senses.get(word, [])):
                    return word
        # A spelling the pack names in brackets ("ɗinbin: (ɗimbin) a great many").
        for gloss in lexicon.senses.get(x, [])[:2]:
            for text in GLOSS_PARENS.findall(gloss):
                spelling = hausa_clean(text).lower()
                if WORD.fullmatch(spelling) and spelling != x and listed(spelling) \
                        and distance(skeleton(spelling), skeleton(x)) <= 1:
                    return listed(spelling)
        b = grade_base(x)
        if b:
            return b
        if x.endswith("wa") and len(x) >= 4:
            stem = x[:-2]
            verb = listed(stem) or grade_base(stem)
            # The pack's verbal noun glossed as its word is (gajiyawa "getting tired": gajiya "tiredness"; rikicewa
            # "confusion": rikice "confused") needs no verb by text.
            glossed = verb and glossed_alike(lexicon.senses.get(x, []), lexicon.senses.get(verb, []))
            if verb and (len(x) >= 5 or pack_verb(verb)) and (verbness(stem) >= NOUN_VERB or glossed) \
                    and score.get(x, 0.0) <= VERBAL_NOUN_SHARE * score.get(verb, 0.0) and verbal(x, verb):
                return verb
        if x.endswith("uwa") and len(x) >= 6:
            for stem in (x[:-3] + "a", x[:-3] + "e"):
                verb = listed(stem)
                if verb and verbness(stem) >= BASE_VERB and score[x] <= VERBAL_NOUN_SHARE * score[verb] and verbal(x, verb):
                    return verb
        # A feminine: tsohuwa, tauraruwa (tsoho, tauraro); matashiya, jaririya (matashi, jariri).
        if x.endswith("uwa") and len(x) >= 6 or x.endswith("iya") and len(x) >= 6:
            word = (listed(x[:-3] + "o") or listed(x[:-3] + "u")) if x.endswith("uwa") else listed(x[:-2])
            if word and word.endswith(("o", "u", "i")) and score[word] > score[x] and (
                    any(FEMININE_GLOSS.search(g) for g in lexicon.senses.get(x, []))
                    or not lexicon.senses.get(x) and lexicon.kinds.get(word, set()) & NOMINAL):
                return word
        for ending in ("na", "ta"):
            word = listed(x[:-2]) if x.endswith(ending) and x[-3:-2] in ("a" if ending == "ta" else "aeiou") else None
            if word and score[word] > score[x] and verbness(x) < POSSESSIVE_VERB and (
                    not lexicon.senses.get(x) and len(x) >= 5
                    or any(re.search(r"\bmy\b", g) for g in lexicon.senses.get(x, []))):
                return word
        # An adjective in -arwa of a grade 5 verb (ƙayatarwa, nishaɗantarwa: ƙayatar, nishaɗantar); not a noun (sadarwa
        # "communication").
        if x.endswith("arwa") and len(x) >= 7 and listed(x[:-2]) and "noun" not in lexicon.kinds.get(x, ()):
            return listed(x[:-2])
        # A feminine in -iya of a noun in -a, glossed so or not in the pack (jakadiya: jakada).
        if x.endswith("iya") and len(x) >= 7 and listed(x[:-3] + "a") and score[listed(x[:-3] + "a")] > score[x] \
                and (any(FEMININE_GLOSS.search(g) for g in lexicon.senses.get(x, [])) or not lexicon.senses.get(x)) \
                and lexicon.kinds.get(listed(x[:-3] + "a"), set()) & NOMINAL:
            return listed(x[:-3] + "a")
        return possessed(x) or masculine(x) or singular(x) or linked(x)

    target = {}
    for x in sorted(score):
        if head(x) or heads.get(x) and x not in heads[x]:
            b = base(x)
            if b and b != x:
                target[x] = b

    def final(w):
        seen = {w}
        while w in target and target[w] not in seen:
            w = target[w]
            seen.add(w)
        return w

    for form, words in heads.items():
        if words & target.keys():
            heads[form] = frozenset(final(w) for w in words)
    # A folded word text has that was counted only through its forms (rundunoni, through rundunonin) is its word's form too.
    for x in sorted(target):
        if not heads.get(x) and share.get(x, 0.0) > 0:
            heads[x] = frozenset({final(x)})
    return target


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the pack's words and the everyday words it lacks, by their forms' counts, each kind weighted by WEIGHTS. A
    word the pack lacks is no name (written capitalised more often than not) and no word of one letter."""
    lexicon = Lexicon(dictionaries)
    counts, names, hyphened, after_subject, neighbours = _counts(download, lexicon)
    _split_objects(counts, after_subject, lexicon)
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
        if form in HAUSA_EXCLUDED or FOREIGN_LETTERS.search(form) or len(form) < 2 or UNHAUSA.search(form):
            return False
        everyday = counts["everyday"].get(form, 0.0)
        news = counts["news"].get(form, 0.0)
        wiki = counts["wikipedia"].get(form, 0.0)
        # A name: written capitalised inside sentences more often than small anywhere (Ibrahim, Babangida).
        if names.get(form, 0) > everyday + news + wiki:
            return False
        if form in english_words and (everyday < 3 or form not in lexicon.wiktionary) or "ʻ" in form:
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
    _fold_heads(heads, share, written, after_subject, lexicon)
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

    # A word mostly written in one fixed pair or beside one name (kwanton ɓauna, zirin Gaza) is no word of the list, nor one far
    # more often a name (Sin "China" beside sin, the Arabic letter) unless it names a language.
    for word in sorted(scores):
        uses = sum(neighbours.total.get(f, 0) for f in forms[word])
        bound = 0.0
        for form in forms[word]:
            pair = neighbours.fixed(form, neighbours.total.get(form, 0))
            if pair:
                bound += pair[1] * neighbours.total.get(form, 0)
        if uses >= PHRASE_LEAST and bound >= PHRASE_SHARE * uses:
            scores[word] = 0.0
        if names.get(word, 0) > NAME_RATIO * max(1, uses) and not any("language" in g for g in lexicon.senses.get(word, [])):
            scores[word] = 0.0
        # A word the pack has only in a phrase it names first ("dubatar: (dubun dubatar) tens of thousands", "katutu: (yi
        # katutu) to be rampant"), but for the verbs with da, wa or ma ("(ladabtar da) to discipline", "(cim ma)").
        first = lexicon.senses.get(word, [""])[0]
        phrase = re.match(r"\(([^()]*)\)", first)
        if phrase:
            parts = [hausa_clean(p).lower() for p in re.split(r"[\s/-]+", phrase.group(1)) if p]
            if word in parts and len(parts) > 1 and any(p not in PHRASE_PARTICLES and p != word for p in parts):
                scores[word] = 0.0
        if "ʻ" in word:
            scores[word] = 0.0
    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w))
              if totals[w] >= LEAST_COUNT and scores[w] > 0 and w not in HAUSA_EXCLUDED][:size]
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

