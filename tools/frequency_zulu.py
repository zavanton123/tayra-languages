"""
The Zulu frequency list (build_frequency_list.py --language zu).

Zulu is written conjunctively: a verb carries its subject, tense, negative and object concords in front and its
extensions and ending behind (ngiyambona "I see him", abangazange bahambe "who never went", kuthiwa "it is said"), a noun
takes its prepositions and copulas on (nomuntu "with a person", ngesikhathi "at the time", ngumuntu "it is a person",
ekhaya "at home"), and a name keeps its capital behind such a prefix (uThemba, kaThemba, eGoli, ngoMsombuluko). A corpus
so has hundreds of forms of a common word, most of which no pack lists. Each text word is read here as the pack's word or
form (dictionaries/zu-en.sqlite, from the English Wiktionary, whose inflection tables are generated and long), a noun or
pronoun with a prefix (nominal_lemmas), or a verb by its prefix and ending as the pack's tables show them, with an object
concord, -sa- "still" or an extension (verb_lemmas), or in a compound tense (wayehlala); failing those, as a regular form
the tables lack that one pack word alone makes (regular_lemmas: locatives, copulas, a noun without its first vowel,
concords on adjectives and adverbs, hortatives, the perfect in -ele). A word of the lexicon keeps its own count, and a
form of several words is shared by how common each is by the forms of no other; a noun's form a verb's is written alike
(ekhala "of the nose", "crying") is the one word's or the other's by the same counts (NOUN_SHARE).

Counted from Leipzig's community and web sentences, OPUS's CCAligned web text and its Wikimedia text, each kind the share
WEIGHTS gives. A word written with a capital inside a sentence is a name (Durban, uThemba, kwaZulu) and not counted, unless
the pack has it as a word or the noun rules make it one (iSonto "Sunday", isiZulu, ngoMsombuluko "on Monday").
"""
import functools
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

LEIPZIG = [("community", "zul_community_2017"), ("web", "zul_web_2013_100K")]
OPUS = [("web", "CCAligned", "v1"), ("wiki", "wikimedia", "v20230407")]
# Leipzig's community sentences are much of the written language (notices, letters, sermons, news); the web text adds the
# everyday words of shops and forums, and is much product pages (usayizi, iphrinta), so it gets less than its size.
WEIGHTS = {"community": 0.5, "web": 0.35, "wiki": 0.15}
LICENCES = {
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "CCAligned": "terms at https://opus.nlpl.eu/CCAligned/corpus/version/CCAligned",
    "wikimedia": "CC BY-SA 4.0",
}

# As the reader's wordCharacters for Zulu (a-zA-Z): an apostrophe or a hyphen cuts a word (ngakho-ke is ngakho + ke).
WORD = re.compile(r"[A-Za-z]+")
SENTENCE_START = re.compile(r"(?:^|[.!?:;…\"“”«»()\[\]♪]\s*|\s-\s*)$")
# A word must be counted this often (all its forms, all sources) to be listed.
LEAST_COUNT = 4
# A common verb has hundreds of forms in the texts; with the regular forms read too, 600 keeps every form 400 did.
FORMS_LISTED = 600
VOWELS = "aeiou"

# Parts of speech that are no words of a text.
NOT_WORDS = {"name", "prefix", "suffix", "infix", "interfix", "symbol", "character", "phrase", "proverb", "prep_phrase"}
COMBINING_STEM = re.compile(r"^Combining stem of (\w+)")

# By hand: everyday words the pack lacks (build_dictionary.py adds them to the zu-en pack with these glosses).
ZULU_WORDS = {
    "kodwa": ("conj", "but"),
    "kepha": ("conj", "but"),
    "ukuthi": ("conj", "that (introducing a clause)"),
    "nokho": ("conj", "however, nevertheless"),
    "hhayi": ("adv", "no, not"),
    "mayelana": ("prep", "about, concerning"),
    "ngakho": ("conj", "therefore, so"),
    "kanjalo": ("adv", "thus, in that way, likewise"),
    "kanje": ("adv", "like this, in this way"),
    "kumele": ("verb", "it is necessary, must"),
    "ngisho": ("adv", "even"),
    "angeke": ("adv", "never, will not, cannot"),
    "nakuba": ("conj", "although"),
    "ngempela": ("adv", "really, truly"),
    "namuhla": ("adv", "today"),
    "ngaphambi": ("adv", "before, in front"),
    "lutho": ("pron", "nothing, anything"),
    "ngani": ("adv", "why; with what"),
    "nini": ("adv", "when"),
    "sengathi": ("conj", "as if, it seems that"),
    "njengoba": ("conj", "as, since, because"),
    "eduze": ("adv", "near, nearby"),
    "ndawonye": ("adv", "together"),
    "ikakhulukazi": ("adv", "especially"),
    "kuphela": ("adv", "only"),
    "ngabe": ("adv", "perhaps; would that; introduces a question"),
    "bese": ("conj", "and then"),
    "yize": ("conj", "although; in vain"),
    "zange": ("verb", "never (did): the auxiliary of the negative past, as in akazange afike \"he never came\""),
    "ngapha": ("adv", "on this side, over here; towards"),
}
# Entries that are names a text writes as words: KwaZulu the province, and a capitalised noun the pack glosses as a
# country, city, province or continent, a person's name or a god's (iGoli, iNdiya, uJabulani, uJesu); its languages,
# peoples, days, months and holidays are words (isiNgisi, umZulu, uLwesibili, uMashi, uKhisimusi).
ZULU_EXCLUDED = {"kwazulu", "isinye", "ikhona"}
# Nouns the pack lacks that the verb rules would read (isineke "patience" as sina's i-sinek-e), with their prefixes: no
# words.
UNLISTED_NOUNS = re.compile(r"^(?:i|e|ne|nge|yi|we|se|ye|le|ze|lwe|bwe|kwe|ke|o|a)?sineke$")
# A class prefix written apart before a foreign word (ama channel, ngama degrees): no word.
FRAGMENTS = {"ama", "nama", "ngama", "kwama", "lama", "yama", "wama", "sama", "ezama", "okwama"}
# And the copula "which are" before a number (abangu 20, ezingu 5).
FRAGMENTS |= {c + "ngu" for c in ("e", "ezi", "zi", "a", "aba", "ba", "esi", "eyi", "eli", "olu", "obu", "oku", "emi", "ayi", "ama")}
NAME_GLOSS = re.compile(r"\((?:a|the) (?:\w+ )*(?:country|city|province|continent|planet)\b|\bgiven name|\bsurname|\bclan name|^(?:Jesus|Jehovah|Zululand)$")
# Forms whose word the rules would get wrong, or that are words of their own.
ZULU_FORMS = {
    # "That" and its forms with a prefix: the conjunction, not thi's infinitive.
    **{p + "kuthi": "ukuthi" for p in ("ngo", "no", "yo", "ko", "so", "lo", "wo", "wu", "yi", "e", "ku", "njengo", "ba", "o")},
    # "But" (ko + dwa would be class 15's "alone").
    "kodwa": "kodwa",
    # "Therefore" (nga + kho), and "with it" its class 15 pronoun, khona.
    "ngakho": "ngakho",
    "nakho": "khona",
    # "There is", "there is not", the copula of place.
    "kukhona": "khona",
    "akukho": "khona",
    "akekho": "khona",
    "ayikho": "khona",
    "abekho": "khona",
    "akukhona": "khona",
    "kwakukhona": "khona",
    "kusekhona": "khona",
    # "Even", far more than "I say".
    "ngisho": "ngisho",
    # "Perhaps", the question's ngabe.
    "ingabe": "ngabe",
    # "Even if, whether": noma with ku, na, nga.
    "kunoma": "noma",
    "nanoma": "noma",
    "nganoma": "noma",
    # "Another, other" of class 7 and 10 (isinye, izinye); the pack's isinye "bladder" is left out.
    "isinye": "nye",
    "esinye": "nye",
    "izinye": "nye",
    # "Who is present", "it is there": khona's copulas, not ikhona "corner" (left out).
    **{"a" + p + "kho": "khona" for p in ("zi", "si", "li", "lu", "bu", "ngi", "wu", "ni", "ba")},
    **{p + "khona": "khona" for p in ("e", "i", "be", "li", "se", "yi", "si", "lu", "bu", "zi", "ba", "ngi", "u", "ni")},
    # "Your" (wakho, yakho, ezakho), wena's, not khona's "of it".
    **{c + "akho": "wena" for c in ("w", "b", "y", "l", "s", "z", "lw", "bw", "kw", "", "ow", "ab", "ey", "el", "es", "ez", "olw",
                                    "obw", "okw", "aw", "k", "kub", "kuy", "kul", "kus", "kuz", "kulw", "kukw", "nob", "noy", "nez")},
    # "With it" (class 5 and 11), not ingalo "arm".
    "ngalo": "lona",
    "muntu": "umuntu",
    # "Each" (ngamunye, ngalinye, ngasinye): nye's.
    **{"nga" + c + "nye": "nye" for c in ("mu", "li", "si", "yi", "lu", "bu", "ku", "zi", "wu", "ba")},
    "luthi": "thi",
    # "Let me, just" (ake ngibone), ke's.
    "ake": "ke",
    # "This" (class 9) written in full.
    "lena": "le",
    # "Every day" (nsuku zonke), izinsuku without its prefix.
    "nsuku": "usuku",
    # The class 6 "these" written in full.
    "lawa": "la",
    # "You say", "it says", "who says", far more than uthi "stick".
    "uthi": "thi",
    "kuthi": "thi",
    "othi": "thi",
    # "Sitting, staying", hlala's perfect; "they are not there" of class 6.
    "hlezi": "hlala",
    "awekho": "khona",
    # "Come!", za's.
    "woza": "za",
    "wozani": "za",
    # "He is not there", as akekho.
    "akakho": "khona",
    # "Badly", bi's; "after, behind" spelled without u; "on the day of"; "in that (it is)".
    "kabi": "bi",
    "emva": "emuva",
    "ngemva": "emuva",
    "ngomhla": "mhla",
    "ngomhlaka": "mhla",
    "ekutheni": "ukuthi",
    # "Coming, next" (izinyanga ezayo), za's relative, far more than yona's "those of it".
    "ezayo": "za",
    # "Darling" (sithandwa sami), the vocative.
    "sithandwa": "isithandwa",
}
# Forms the pack's generated tables give a noun wrongly: umbhali "writer" has umhlali's plural (abahlali "residents", not
# ababhali), and uhambo inkambo's (izinkambo "customs"; uhambo's is izinhambo).
TABLE_ERRORS = {"umbhali": re.compile(r"hlali"), "uhambo": re.compile(r"^izinkambo$")}
# A noun's forms that are the noun itself in its other number (abafana, amakhala, izithombe, obaba; not abamabele "those
# of the breasts"): the noun's, whatever a verb reads them as.
PLURAL_PREFIXES = ("aba", "abe", "ama", "imi", "izi")
# A noun's form that a verb's form is written alike (ekhala "of the nose", "crying"; ngibhala "I am a wheelbarrow", "I
# write") is the verb's when the noun's other forms are less than this share of the verb's (ikhala, ibhala, ihlezi), and
# the noun's otherwise (esizwe "of the nation", ilanga's elanga).
NOUN_SHARE = 1 / 3
# The past of a compound tense, the auxiliary ba's with its subject concord, and the participial concord the verb then
# takes: wayehlala "he was staying" (waye- + ehlala), babehlala, kwakuhlala, bengihlala, ubehlala.
COMPOUND_PAST = {"waye": "e", "babe": "be", "aye": "e", "wawu": "u", "yayi": "i", "lali": "li", "sasi": "si", "zazi": "zi",
                 "lwalu": "lu", "babu": "bu", "kwaku": "ku", "ngangi": "ngi", "nani": "ni", "bengi": "ngi", "ube": "e",
                 "bebe": "be", "besi": "si", "beni": "ni", "bewu": "u", "beyi": "i", "beli": "li", "bezi": "zi", "belu": "lu",
                 "bebu": "bu", "beku": "ku"}


def strip_tones(word):
    import unicodedata
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", word) if not unicodedata.combining(c)))


# The object concords a verb may take before its stem, and the shapes they take before a stem in a vowel (ngiyakwazi,
# uyamazi, ngiyabazi).
OBJECTS = ["ngi", "ku", "m", "mu", "si", "ni", "ba", "wu", "yi", "li", "wa", "zi", "lu", "bu"]
OBJECTS_BEFORE_VOWEL = ["ngi", "kw", "m", "mw", "s", "n", "b", "w", "y", "l", "z", "lw", "bw"]
# The object concords that are no subject concords: an imperative's prefix, and none before a noun's stem.
BARE_OBJECTS = {"", "m", "mu", "mw", "kw", "w", "y", "yi", "s", "n", "b", "l", "z", "lw", "bw"}
# The extensions a verb's stem takes (applicative, causative, reciprocal, passive, neuter and their joins): bonwa, hambela,
# hambisa, thandana, thandiwe, enzeka, bonakala, hambisisa, thandelana.
EXTENSIONS = ["el", "is", "an", "w", "iw", "ek", "akal", "elw", "isw", "isis", "elel", "elan", "isan", "an", "ekel",
              "eliw", "isiw", "aniw", "ulw", "ul"]
# Verbs the pack lacks whose forms the rules would read as a shorter verb's with an extension (thulula "pour out" as thula
# "be quiet" + -ul-): no words.
UNLISTED_ROOTS = ("thulul",)
# The enclitics written onto a word: -ni "what", -phi "where", -ke "then", -nje "just".
ENCLITICS = ["ni", "phi", "ke", "nje"]
# Prefixes ending in a that join a noun's first vowel (a + i is e, a + u is o): na "and, with", nga "by, at", njenga "like",
# kuna "than", nanga "and by".
JOINING = ["na", "nga", "njenga", "kuna", "nanga", "nanjenga", "njengoba"]
# The possessive concords, which join a noun's first vowel as na does (wesikhathi, sokuhlala, lwezingane); the pack's tables
# have most of them, not those of an infinitive.
POSSESSIVE_STEMS = ["w", "b", "y", "l", "s", "z", "lw", "bw", "kw", "ow", "ab", "ey", "el", "es", "ez", "olw", "obw", "okw"]
# Prefixes that take a noun in u- or i- without its vowel (kubaba, kaThemba, ngumuntu, yinto) and the possessive concords
# with ka (likaNkulunkulu).
DROPPING = {p: "u" for p in ("ku", "ka", "ngu", "wu", "kuka", "lika", "sika", "zika", "yaka", "waka", "baka", "kwaka",
                              "lwaka", "buka", "aka", "eka", "naka", "ngaka")}
DROPPING.update({"yi": "i", "ngi": "i"})
# The subject concords, with the relative and the negative ones, that a noun's or pronoun's form with na "with, have" or
# the copula takes (unemali "he has money", abanemali "who have money", akanawo "he does not have it").
SUBJECT_CONCORDS = ["ngi", "u", "si", "ni", "ba", "i", "li", "zi", "ku", "lu", "bu", "a", "be", "se", "e", "o", "aba", "esi",
                    "eli", "ezi", "olu", "obu", "oku", "eni", "engi", "angi", "awu", "aka", "asi", "ani", "aba", "awa", "ayi",
                    "ali", "azi", "alu", "abu", "aku"]
# The concords of the quantitative -nke "all" (wonke, bonke, zonke) and of -dwa "alone" (yedwa, bodwa, ngedwa).
NKE = {c + "nke" for c in ("o", "wo", "bo", "zo", "yo", "lo", "so", "ko", "lwo", "bwo", "mo", "no")}
DWA = {c + "dwa" for c in ("nge", "we", "ye", "so", "bo", "lo", "zo", "yo", "wo", "lwo", "bwo", "ni", "si", "eyo", "owo", "elilo",
                           "esiso", "olulo", "obubo", "okuko", "ababo", "ezizo", "omo", "abo",
                           "oye", "noye")}
# Prefixes an adverb takes: ngaphansi, naphakathi, kuqala; the possessive concords, okwamanje "for now", samanje "modern".
ADVERB_PREFIXES = ["nga", "na", "ku", "wa", "ba", "ya", "la", "sa", "za", "lwa", "bwa", "kwa", "owa", "aba", "eya", "ela", "esa",
                   "eza", "olwa", "obwa", "okwa", "ezase", "okwase"]
# The copulas and their negatives on an absolute pronoun: kuyona "it is it", akuyona "it is not", nguye "it is he".
COPULAS = ["ku", "aku", "i", "ayi", "li", "ali", "si", "asi", "zi", "azi", "lu", "alu", "bu", "abu", "wu", "awu", "ngu",
           "akungu", "yi", "akuyi", "kuyi", "kungu", "kwakuyi", "kungaba"]
# The verbs of one consonant or two that the verb rules read beyond their tables: their passive (phiwa, dliwa) and object
# concords (ngikunika is nika's; uyangipha, wamupha). The rest of the short verbs (ma "stand", na "rain", sa "dawn", nya,
# wa "fall") have only their tables' forms: by rule they would take abanye, bese, kuwe, lawa.
SHORT_VERBS = {"ya", "za", "ba", "dla", "pha", "fa", "thi", "sho"}
# And eqa "jump", whose tables lack its object concords (ungaweqi, akaweqanga).
SHORT_VERBS |= {"eqa"}
# Verbs a text also writes with e- or a- (ukwedlula, ezedlule "past", wezwa "he heard", ezahlukene "different"), and as
# a frequentative (ezahlukahlukene "various"), which the pack has otherwise.
VARIANTS = {"edlula": "dlula", "ezwa": "zwa", "ahlukana": "ehlukana", "ahluka": "ehluka", "ahlukahlukana": "ehlukana",
            "ehlukahlukana": "ehlukana", "hlukahlukana": "ehlukana"}

# The regular forms the pack's tables lack, read only where nothing above reads a word and one pack word alone makes it
# (Lexicon.regular_lemmas). The concords a noun's copula, an adjective, a relative stem or an adverb takes in front:
# subject (iphezulu, ulapha), relative (eliphezulu, eyishumi, okuyinto), negative (ayiphezulu, akude) and past (yayinkulu,
# ibimnyama).
CONCORDS = ["ngi", "u", "si", "ni", "ba", "i", "li", "zi", "lu", "bu", "ku", "a", "wu",
            "e", "o", "aba", "esi", "eli", "ezi", "olu", "obu", "oku", "engi", "eni", "ama",
            "angi", "awu", "aka", "asi", "ani", "ali", "azi", "alu", "abu", "aku", "awa", "ayi",
            "bengi", "ube", "besi", "beni", "bebe", "ibi", "ibe", "beli", "bezi", "belu", "bebu", "beku", "be", "abe",
            "ngangi", "wawu", "sasi", "nani", "babe", "yayi", "lali", "zazi", "lwalu", "babu", "kwaku", "waye", "aye",
            "kube", "kungu", "kuyi", "kwakuyi", "kwakungu", "kunga", "kuzoba", "kungaba"]
# The prefixes of a locative: itself (emgwaqweni), "at" with a concord (lisesifundazweni, abasemkhunjini, usekhaya),
# "and, by, of" (nasemkhunjini, ngasemgwaqweni, wasemzini).
LOCATIVE_PREFIXES = {"", "s"} | {c + "s" for c in CONCORDS + ["na", "nga", "njenga", "kwa", "wa", "ba", "ya", "la", "sa",
                                                                "za", "lwa", "bwa", "owa", "eya", "ela", "esa", "eza",
                                                                "olwa", "obwa", "okwa", "kuna", "ngoku"]}
# A noun without its first vowel after ku "to" (kusisebenzi), kuna "than", or na "have" in the negative (azinasici).
BARE_PREFIXES = {"", "ku", "kuna", "nga"} | {n + "na" for n in ("angi", "awu", "aka", "asi", "ani", "aba", "ayi", "ali",
                                                                  "azi", "alu", "abu", "aku", "awa", "a", "enga",
                                                                  "esinga", "ezinga", "abanga", "onga", "okunga")}
# "Both, all three" (bobabili, zozimbili) before a number's forms.
QUANTITATIVE = ["bo", "zo", "yo", "wo", "lo", "so", "ko", "lwo", "bwo", "mo", "no"]
# The negative copula before a pronoun's combining stem: akusikho "it is not there", akusiyo "it is not it".
NEGATIVE_COPULAS = ["akusi", "akuyi", "asi", "ayisi", "akasi", "alisi", "azisi", "alusi", "abusi", "abasi", "awusi", "awasi"]
# Prefixes before an infinitive without its first vowel and with an object concord: ngokungipha, kokuqhathanisa,
# kunakuqala, njengokumbona.
INFINITIVE_PREFIXES = ["u", "o", "no", "ngo", "njengo", "kuno", "nango", "ko", "wo", "bo", "yo", "lo", "so", "zo", "lwo", "bwo",
                       "owo", "abo", "eyo", "elo", "eso", "ezo", "olwo", "obo", "oko", "kuna", "nga", "na", "e"]
# Which prefixes each kind of regular form takes (Lexicon._make_regular_forms).
REGULAR_PREFIXES = {
    "locative": LOCATIVE_PREFIXES,
    "copula": {""} | set(CONCORDS),
    "bare": BARE_PREFIXES,
    "stem": {""} | set(CONCORDS) | {"yi", "ngu", "wu", "njenga"} | set(QUANTITATIVE),
    "pronoun": set(NEGATIVE_COPULAS),
    "khona": {""} | set(CONCORDS) | {"ngoku", "noku", "nga", "ka"},
    "o-noun": {"k"},
}
LONGEST_REGULAR_PREFIX = max(len(p) for ps in REGULAR_PREFIXES.values() for p in ps)
# A labial before the locative's -w- becomes a palatal (umkhumbi emkhunjini, umlomo emlonyeni, ingubo engutsheni).
PALATALS = [("mb", "nj"), ("mp", "ntsh"), ("bh", "j"), ("ph", "sh"), ("p", "tsh"), ("b", "tsh"), ("m", "ny")]


def locatives(noun):
    """The locatives of [noun] (or its plural) as texts write them: e- for its first vowel, -ini/-eni for its last, -o and
    -u with a w (emgwaqweni, ezulwini) or a labial made palatal (emkhunjini), umu- of a short stem as m- (emzini); a u- noun
    of class 11 with o- (osebeni)."""
    stem = noun[1:]
    if len(stem) < 3 or stem[-1] not in VOWELS or noun[0] not in "aiu":
        return set()
    heads = {"e" + stem[:-1]}
    if noun[0] == "u":
        heads.add("o" + stem[:-1])
    if noun.startswith("umu") and len(noun) <= 6:
        heads.add("em" + noun[3:-1])
    last = stem[-1]
    out = set()
    for head in heads:
        out |= {head + end for end in {"a": ["eni"], "e": ["eni"], "i": ["ini"], "o": ["weni", "eni"], "u": ["wini", "ini"]}[last]}
        for labial, palatal in PALATALS:
            if head.endswith(labial):
                out.add(head[:-len(labial)] + palatal + ("ini" if last in "iu" else "eni"))
                break
    return out


def noun_copulas(noun):
    """"It is [noun]": ngumuntu, wumuntu, yishumi, yizinto, ngamanzi, ngobaba."""
    stem = noun[1:]
    if noun[0] == "u":
        return {"ngu" + stem, "wu" + stem}
    if noun[0] == "i":
        return {"yi" + stem}
    if noun[0] in "ao":
        return {"ng" + noun}
    return set()


class Lexicon:
    """The words of the zu-en pack, their forms, the pronouns' combining stems, and the verbs' prefixes and endings."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "zu-en.sqlite"))
        self.kinds = defaultdict(set)
        # The words the pack writes with a capital after their prefix, which a text writes so too: iSonto "Sunday",
        # uMsombuluko "Monday", isiZulu, uNkulunkulu.
        self.capitalised = set()
        self.combining = {}
        stems_only = set()
        for written, word, pos, senses in db.execute("SELECT word, word_lc, pos, senses FROM entries"):
            if pos in NOT_WORDS or not re.fullmatch(r"[a-z]+", word) or word in ZULU_EXCLUDED:
                continue
            glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])]
            if written != word:
                if any(NAME_GLOSS.search(g) for g in glosses):
                    continue
                self.capitalised.add(word)
            stems = {m.group(1).lower() for g in glosses if (m := COMBINING_STEM.match(g))}
            if stems and len(stems) == len(glosses):
                self.combining.setdefault(word, next(iter(stems)))
                stems_only.add(word)
                continue
            self.kinds[word].add(pos)
        for word, (pos, _) in ZULU_WORDS.items():
            self.kinds[word].add(pos)
        # "With it" of class 15 and 17 (nakho, ngakho), not wena's possessive kho.
        self.combining["kho"] = "khona"
        self.forms = defaultdict(set)
        self.table = defaultdict(set)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            lemma = lemma.lower()
            if lemma in TABLE_ERRORS and TABLE_ERRORS[lemma].search(form):
                continue
            if lemma in self.kinds and form != lemma and re.fullmatch(r"[a-z]+", form):
                self.forms[form].add(lemma)
                self.table[lemma].add(form)
        for word in stems_only:
            if word not in self.kinds:
                self.forms[word].add(self.combining[word])
        self.verbs = {w for w, ks in self.kinds.items() if "verb" in ks and len(w) > 1}
        self.pronouns = {w for w, ks in self.kinds.items() if "pron" in ks and len(w) > 3}
        self.nominals = {w for w, ks in self.kinds.items() if ks & {"noun", "pron"}}
        self.bases = {w: {w} | {f for f in self.table[w] if f.startswith(PLURAL_PREFIXES) and len(f) <= len(w) + 3
                                or w[0] == "u" and f == "o" + w[1:]} for w, ks in self.kinds.items() if "noun" in ks}
        # The forms a noun shares with a verb, which the build gives the one or the other by their counts
        # (NOUN_SHARE): the noun's words, the others'.
        self.collisions = {}
        self._learn_verb_patterns()
        self._make_regular_forms()

    def _learn_verb_patterns(self):
        """The prefixes and endings of the pack's verb tables, by whether the stem starts with a consonant (C) or a vowel
        (V): ngiya-hamb-a, aba-hamb-anga, uzo-hamb-a; ngiy-enz-a, w-enz-a. A pair counts when three verbs show it."""
        seen = defaultdict(set)
        for verb in self.verbs:
            if not verb.endswith("a") or len(verb) < 4 or len(self.table[verb]) < 50:
                continue
            root = verb[:-1]
            kind = "V" if root[0] in VOWELS else "C"
            for form in self.table[verb]:
                at = form.rfind(root)
                if at < 0:
                    continue
                seen[kind, form[:at], form[at + len(root):]].add(verb)
        self.endings = {"C": defaultdict(set), "V": defaultdict(set)}
        for (kind, prefix, ending), verbs in seen.items():
            if len(verbs) >= (3 if kind == "C" else 2):
                self.endings[kind][ending].add(prefix)
        # The imperative is the stem itself (hamba, hambisa, enza), which the tables do not list as a form.
        for kind in self.endings:
            self.endings[kind]["a"].add("")
        self.longest_prefix = max(len(p) for k in self.endings.values() for ps in k.values() for p in ps) + 3
        # The stems a verb's forms are made on: its root and its extended roots that are no verbs of their own; a verb in
        # another vowel (thi, sho, azi) is a stem as a whole, taking the prefixes of the forms in -a.
        self.stems = defaultdict(set)
        self.whole = defaultdict(set)
        for verb in self.verbs:
            if verb.endswith("a"):
                root = verb[:-1]
                short = len(root) < 3
                if short and verb not in SHORT_VERBS:
                    continue
                self.stems[root].add(verb)
            else:
                root = verb
                if len(verb) < 3 and verb not in SHORT_VERBS:
                    continue
                self.whole[verb].add(verb)
            for ext in (["iw"] if len(root) < 3 else EXTENSIONS):
                stem = root + ext
                if stem + "a" in self.kinds:
                    continue
                # The -iw- of a verb whose passive is a verb of its own is that verb's: vuthiwe is vuthwa's perfect.
                if ext == "iw" and len(root) > 2 and root + "wa" in self.verbs:
                    self.stems[stem].add(root + "wa")
                    continue
                # A stem on a longer verb's root is that verb's: balekela is baleka's (balek-el-a), not bala's.
                if any(stem[:k] + "a" in self.verbs for k in range(len(root) + 1, len(stem))):
                    continue
                self.stems[stem].add(verb)
        for stem in [s for s in self.stems if s.startswith(UNLISTED_ROOTS)]:
            del self.stems[stem]
        # thi "say" and sho in their perfect: uthe "he said", okuthile "something", ushilo; ba's past and future auxiliary
        # (wabe, kwakube, uzobe); zange "never" with its negative subject (akazange, angizange).
        self.special = {}
        for prefix in self.endings["C"].get("a", ()):
            if prefix:
                self.special.setdefault(prefix + "be", "ba")
        for prefix in self.endings["C"].get("i", ()):
            self.special.setdefault(prefix + "zange", "zange")
        for prefix in self.endings["C"].get("e", ()):
            self.special.setdefault(prefix + "the", "thi")
        for prefix in self.endings["C"].get("ile", ()):
            self.special.setdefault(prefix + "thile", "thi")
            self.special.setdefault(prefix + "shilo", "sho")

    def _make_regular_forms(self):
        """The forms of the pack's words its tables lack, each with the prefixes it takes (REGULAR_PREFIXES): a noun's
        locatives as texts write them (emgwaqweni, emkhunjini, emzini; the tables have emgwaqeni), its copulas (yishumi,
        ngumuntu), the noun without its first vowel (filimu, khasi); an adjective's, a relative stem's and an adverb's
        forms (nkulu, mnyama, phezulu, seduze); a pronoun's combining stem; and the verb stems of the perfect in -ele, -ene
        (tholakele, sele, jwayele) and of the e- verbs without their e (jwayela)."""
        self.regular = {kind: defaultdict(set) for kind in REGULAR_PREFIXES}
        for noun in [w for w, ks in self.kinds.items() if "noun" in ks and len(w) > 3]:
            table = self.table[noun]
            for base in {noun} | {f for f in table if f[0] in "aiu" and len(f) > 3 and f[1:] in table}:
                for form in locatives(base):
                    self.regular["locative"][form].add(noun)
                for form in noun_copulas(base):
                    self.regular["copula"][form].add(noun)
                if len(base) > 4:
                    self.regular["bare"][base[1:]].add(noun)
                # Ku "to" before a noun in o- (kowesifazane, kobaba).
                if base[0] == "o":
                    self.regular["o-noun"][base].add(noun)
        stems = defaultdict(set)
        for word, kinds in self.kinds.items():
            if len(word) < 3:
                continue
            if kinds & {"adj", "num"} or "conj" in kinds and self.table[word]:
                for form in {word} | self.table[word]:
                    stems[form].add(word)
                    if form[0] in VOWELS and len(word) > 3:
                        stems[form[1:]].add(word)
            if "adv" in kinds:
                stems[word].add(word)
                if word[0] in VOWELS:
                    stems["s" + word].add(word)
        for form, words in stems.items():
            if len(form) > 3:
                self.regular["stem"][form] |= words
        # "Is not there", "who is no longer there" (kungekho, engekho, akusekho, ongasekho): khona's.
        for form in ("ngekho", "sekho", "ngasekho"):
            self.regular["khona"][form].add("khona")
        for stem, word in self.combining.items():
            if word in self.kinds:
                self.regular["pronoun"][stem].add(word)
        self.perfect = defaultdict(set)
        self.dropped = defaultdict(set)
        for verb in self.verbs:
            if not verb.endswith("a") or len(verb) < 4:
                continue
            root = verb[:-1]
            roots = [root]
            if root[0] == "e" and len(root) > 4 and root[1] not in VOWELS:
                roots.append(root[1:])
                self.dropped[root[1:]].add(verb)
            for r in roots:
                if r[-2:] in ("al", "an", "el", "en") and len(r) > 2:
                    self.perfect[r[:-2] + "e" + r[-1]].add(verb)
        for variant, verb in VARIANTS.items():
            if verb in self.verbs:
                root = variant[:-1]
                self.perfect[root].add(verb)
                if root[-2:] in ("al", "an", "el", "en"):
                    self.perfect[root[:-2] + "e" + root[-1]].add(verb)
        self.variants = {v[:-1]: {verb} for v, verb in VARIANTS.items() if verb in self.verbs}
        # The stems the regular verb forms are read on: no short verb's (waye is no form of ya), but an infinitive's may be
        # (kokuba, ngokungipha).
        self.regular_stems = {stem: set(verbs) for stem, verbs in self.stems.items() if len(stem) > 2}
        for stem, verbs in list(self.variants.items()) + list(self.dropped.items()):
            self.regular_stems.setdefault(stem, set()).update(verbs)
        self.infinitive_stems = {**self.regular_stems, **{s: v for s, v in self.stems.items() if len(s) <= 2}}

    def _parse_verb(self, token, stems, endings, fits):
        """As verb_lemmas, with [stems], [endings] and the test [fits] of a prefix given."""
        for i in range(0, min(len(token) - 1, self.longest_prefix) + 1):
            prefix = token[:i]
            out = set()
            for j in range(i + 1, len(token)):
                stem, ending = token[i:j], token[j:]
                if ending in endings and stem in stems and fits(prefix, ending, self.kind_of(stem)):
                    out |= stems[stem]
            if out:
                return out
        return set()

    def _regular_verbs(self, token):
        """The verbs [token] is a regular form of that the tables lack: the hortative (masibone, masibuye), the imperative
        with an object concord (yidonse, msize), the perfect in -ele, -ene (itholakele, esisele, ujwayele), an e- verb
        without its e (uzojwayela), a verb in another vowel with the relative -yo (amaziyo, engikwaziyo), and an infinitive
        with an object concord or a prefix (ngokungipha, kokuqhathanisa, kunakuqala)."""
        out = set()
        stems = self.regular_stems
        fits = self._prefix_fits
        if token.startswith("ma") and len(token) > 5:
            out |= self._parse_verb(token[2:], stems, {"e"}, lambda p, e, k: fits(p, "e", k))
        objects = lambda p, k: p in (OBJECTS if k == "C" else OBJECTS_BEFORE_VOWEL)
        out |= self._parse_verb(token, stems, {"e", "eni"}, lambda p, e, k: objects(p, k))
        out |= self._parse_verb(token, self.perfect, {"e", "eyo"}, lambda p, e, k: fits(p, "ile", k) or fits(p, "e", k) and p == "")
        out |= self._parse_verb(token, self.dropped, set(self.endings["C"]), fits)
        out |= self._parse_verb(token, self.variants, set(self.endings["V"]), fits)
        # An adverb of a verb's form with oku (ngokuvamile "usually", ngokuphelele "fully").
        if token.startswith(("ngoku", "ngokw")) and len(token) > 7:
            out |= self.verb_lemmas(token[2:]) or self._regular_verbs(token[2:])
        if token.endswith("yo"):
            for i in range(0, min(len(token), self.longest_prefix + 1)):
                if token[i:-2] in self.whole and fits(token[:i], "ayo", self.kind_of(token[i:-2])):
                    out |= self.whole[token[i:-2]]
        for prefix in INFINITIVE_PREFIXES:
            for ku in ("ku", "kw"):
                if token.startswith(prefix + ku) and len(token) >= len(prefix) + 4:
                    rest = token[len(prefix) + 2:]
                    kind = "C" if ku == "ku" else "V"
                    # An infinitive that is a noun of its own is the noun's (kunakuqala: ukuqala "the beginning").
                    if "u" + ku + rest in self.kinds:
                        out.add("u" + ku + rest)
                        continue
                    found = self._parse_verb(rest, self.infinitive_stems, {"a"},
                                             lambda p, e, k: k == kind and (p == "" or objects(p, k)))
                    for i in range(0, 4):
                        if rest[i:] in self.whole and (i == 0 or objects(rest[:i], self.kind_of(rest[i:]))):
                            found |= self.whole[rest[i:]]
                    out |= found
        return out

    @functools.lru_cache(maxsize=None)
    def regular_lemmas(self, token):
        """The word [token] is a regular form of that the tables lack (_make_regular_forms, _regular_verbs), when one word
        alone makes it."""
        found = set()
        for i in range(0, min(len(token) - 2, LONGEST_REGULAR_PREFIX) + 1):
            prefix, rest = token[:i], token[i:]
            for kind, prefixes in REGULAR_PREFIXES.items():
                if prefix in prefixes and rest in self.regular[kind]:
                    found |= self.regular[kind][rest]
        found |= self._regular_verbs(token)
        return frozenset(found) if len(found) == 1 else frozenset()

    @staticmethod
    def kind_of(stem):
        return "V" if stem[0] in VOWELS else "C"

    @functools.lru_cache(maxsize=None)
    def _prefix_fits(self, prefix, ending, kind, persistive=True):
        prefixes = self.endings[kind].get(ending)
        if prefixes and prefix in prefixes:
            return True
        # An object concord after the prefix: ngiya-m-bona, ngiya-kw-azi.
        plain = self.endings["C"].get(ending)
        if not plain:
            return False
        for oc in (OBJECTS if kind == "C" else OBJECTS_BEFORE_VOWEL):
            if prefix.endswith(oc) and prefix[:-len(oc)] in plain:
                return True
        # The persistive -sa- "still" after the subject concord, an object concord after it too: akasafiki, sisakhumbula,
        # lisalindile, akasamkhuzi; not on a short verb (isayithi "site" is no "it still says it") or an infinitive.
        for i in range(1, len(prefix) - 1) if persistive else ():
            if (prefix[i:i + 2] == "sa" and (kind == "C" or i + 2 < len(prefix)) and not prefix[:i].endswith(("uku", "eku"))
                    and self._prefix_fits(prefix[:i] + prefix[i + 2:], ending, kind)):
                return True
        return False

    def _plain_fits(self, prefix, ending, kind, persistive=False):
        """Whether [prefix] is one the tables show before a verb with [ending], without an object concord."""
        return prefix in self.endings[kind].get(ending, ())

    def _verbs(self, token, fits):
        for i in range(0, min(len(token) - 1, self.longest_prefix) + 1):
            prefix = token[:i]
            out = set()
            for j in range(i + 1, len(token) + 1):
                stem, ending = token[i:j], token[j:]
                kind = self.kind_of(stem)
                if stem in self.stems and fits(prefix, ending, kind, len(stem) > 2):
                    out |= self.stems[stem]
                if not ending and stem in self.whole and fits(prefix, "a", kind, False):
                    out |= self.whole[stem]
            if out:
                return frozenset(out)
        return frozenset()

    def _subject_verbs(self, token):
        """The verbs [token] is a form of by verb_lemmas, unless its prefix is an object concord alone (mfana, msolwa: no
        "resemble him!", "suspect him!")."""
        for i in range(0, min(len(token) - 1, self.longest_prefix) + 1):
            if self._verbs(token[i:], lambda p, e, k, s: p == "" and self._prefix_fits(token[:i], e, k, s)):
                return frozenset() if token[:i] in BARE_OBJECTS else self._verbs(token, self._prefix_fits)
        return frozenset()

    @functools.lru_cache(maxsize=None)
    def verb_lemmas(self, token):
        """The verbs [token] is a form of by the tables' prefixes and endings, with an object concord or an extension; those
        of the shortest prefix only, so the longest stem: bonakala is bona's (bon-akal-a), not kala's."""
        return self._verbs(token, self._prefix_fits)

    def _compound(self, token):
        """The words [token] is a form of in a compound tense (COMPOUND_PAST): wayehlala, babehlala, wayengahlali, and an
        adjective's or a noun's (laliluhlaza "it was green", wayengumuntu)."""
        for aux, concord in COMPOUND_PAST.items():
            if token.startswith(aux) and len(token) > len(aux) + 2:
                # A participial the pack has, else a verb's form (wayesefa is fa's, no "of the inheritance").
                rest = concord + token[len(aux):]
                found = (rest in self.forms and self.lemmas(rest)) or self.verb_lemmas(rest) or self.lemmas(rest)
                if found:
                    if rest in self.collisions:
                        self.collisions[token] = self.collisions[rest]
                    return found
        return frozenset()

    def _strong(self, verbs):
        return {w for w in verbs if len(w) >= 4 or w in SHORT_VERBS}

    def _collide(self, token, nominal, others):
        """[nominal] and the verbs [others] that [token] is a form of alike, kept for the build (NOUN_SHARE)."""
        self.collisions[token] = (frozenset(nominal), frozenset(others))
        return frozenset(nominal) | frozenset(others)

    def _other_readings(self, token):
        """The verbs [token] is a form of by their tables or by the tables' prefixes without an object concord (ekhala,
        behlezi)."""
        tables = {w for w in self.forms.get(token, ()) if "verb" in self.kinds[w]}
        return self._strong(tables | self._verbs(token, self._plain_fits))

    def _word_or_form(self, base, kinds=None):
        """[base] as a word of the lexicon or the words it is a form of, of the parts of speech [kinds] when given."""
        if base in self.kinds and (kinds is None or self.kinds[base] & kinds):
            return {base}
        found = {w for w in self.forms.get(base, ()) if kinds is None or self.kinds[w] & kinds}
        return found

    def _joined(self, token, prefixes):
        """The noun, pronoun or adjective [token] is with one of [prefixes] joined to its first vowel (a + i is e, a + u is
        o): nomuntu, ngesikhathi, nabanye, wesikhathi, ngowethu; an infinitive too (nokuhlala, sokufunda)."""
        for prefix in prefixes:
            stem = prefix[:-1] if prefix.endswith("a") else prefix
            if not token.startswith(stem) or len(token) < len(stem) + 3:
                continue
            vowel, rest = token[len(stem)], token[len(stem) + 1:]
            found = set()
            for v in {"a": ["a"], "e": ["i", "e"], "o": ["u", "o"]}.get(vowel, []):
                found |= self._word_or_form(v + rest, None if (v + rest).startswith(("uku", "ukw")) else {"noun", "pron", "adj"})
            if found:
                return frozenset(found)
        return frozenset()

    def _locative(self, token):
        """The noun [token] is the locative of when the pack's table lacks it: emsebenzini, esikoleni, endleleni (umsebenzi,
        isikole, indlela)."""
        if not (token.startswith("e") and token.endswith(("ini", "eni")) and len(token) > 6):
            return frozenset()
        middle = token[1:-3]
        return frozenset(v + middle + end for v in ("u", "i", "a") for end in VOWELS if "noun" in self.kinds.get(v + middle + end, ()))

    def _copula_nouns(self, token):
        """The nouns [token] is with the copula of an i- noun (ngibhala, yibhala) or with a subject concord on it (ayibhala,
        ngiyingane), the readings nominal_lemmas has first among its own."""
        for prefix in ("ngi", "yi"):
            rest = token[len(prefix):]
            if token.startswith(prefix) and len(rest) > 2 and rest[0] not in VOWELS:
                found = self._word_or_form("i" + rest, {"noun"})
                if found:
                    return found
        for prefix in ("ngi", "u", "si", "ni", "ba", "i", "li", "zi", "ku", "lu", "bu", "wu", "a"):
            rest = token[len(prefix):]
            if token.startswith(prefix) and rest[:2] in ("ng", "yi", "wu") and rest in self.forms:
                found = {w for w in self.forms[rest] if "noun" in self.kinds[w]}
                if found:
                    return found
        return set()

    @functools.lru_cache(maxsize=None)
    def nominal_lemmas(self, token):
        """The words [token] is a noun's, pronoun's or adverb's form of with a prefix: nomuntu, ngesikhathi, nabantu (umuntu,
        isikhathi), kubaba, ngunkulunkulu, likaNkulunkulu, nami, ngayo (mina, yona), wonke, yedwa, ngaphansi, kancane."""
        if token in NKE:
            return frozenset({"nke"} & set(self.kinds))
        if token in DWA:
            return frozenset({"dwa"} & set(self.kinds))
        nouns = {"noun", "pron"}
        found = self._joined(token, JOINING + POSSESSIVE_STEMS)
        if found:
            return found
        for prefix in sorted(DROPPING, key=len, reverse=True):
            rest = token[len(prefix):]
            if token.startswith(prefix) and len(rest) > 2 and rest[0] not in VOWELS:
                found = self._word_or_form(DROPPING[prefix] + rest, nouns)
                if found:
                    return frozenset(found)
        for prefix in ("na", "nga", "ku", "kuna", "njenga"):
            rest = token[len(prefix):]
            if token.startswith(prefix) and rest in self.combining:
                return frozenset({self.combining[rest]} & set(self.kinds))
        for prefix in COPULAS:
            rest = token[len(prefix):]
            if token.startswith(prefix) and rest in self.pronouns:
                return frozenset({rest})
        # A subject concord on na "with", "have" or the copula nga: unemali "he has money", banezingane, ungowethu "it is
        # ours".
        for prefix in SUBJECT_CONCORDS:
            rest = token[len(prefix):]
            if token.startswith(prefix) and rest[:1] == "n" and len(rest) > 3:
                found = self._joined(rest, ["na", "nga"])
                if found:
                    return found
                # A pronoun's: unayo "he has it", akanawo "he does not have it".
                if rest.startswith("na") and rest[2:] in self.combining:
                    return frozenset({self.combining[rest[2:]]} & set(self.kinds))
        found = self._locative(token)
        if found:
            return found
        # A locative with -se- "at" and a prefix: nasemsebenzini "and at work", kwasekhaya, basemakhaya.
        for prefix in ("nase", "ngase", "kwase", "sase", "wase", "base", "yase", "lase", "zase", "lwase", "bwase", "ase", "ese",
                       "se", "nakwa"):
            rest = token[len(prefix) - 1:]
            if token.startswith(prefix) and len(rest) > 4:
                found = self._word_or_form(rest, nouns) or self._locative(rest)
                if found:
                    return frozenset(found)
        # A subject concord on a noun's copula: ngingumuntu "I am a person", uyingane "you are a child", singabantu.
        for prefix in ("ngi", "u", "si", "ni", "ba", "i", "li", "zi", "ku", "lu", "bu", "wu", "a"):
            rest = token[len(prefix):]
            if token.startswith(prefix) and rest[:2] in ("ng", "yi", "wu") and rest in self.forms:
                found = {w for w in self.forms[rest] if "noun" in self.kinds[w]}
                if found:
                    return frozenset(found)
        for prefix in ADVERB_PREFIXES:
            rest = token[len(prefix):]
            if token.startswith(prefix) and len(rest) > 3 and "adv" in self.kinds.get(rest, ()):
                return frozenset({rest})
        if token.startswith("ka") and len(token) > 4 and self.kinds.get(token[2:], set()) & {"adj", "conj", "num"}:
            return frozenset({token[2:]})
        return frozenset()

    def takes_over(self, token):
        """Whether the words the pack has [token] as a form of may take its count: only when it is a verb and nothing else,
        never when it is a noun, pronoun or particle as well."""
        return token not in ZULU_FORMS and self.kinds.get(token) == {"verb"} and bool(self.forms.get(token))

    @functools.lru_cache(maxsize=None)
    def lemmas(self, token, name_like=False):
        """The words [token] is a form of: itself when it is a word, else the pack's words, those of the noun rules, or, unless
        it is written like a name (with a capital inside), those of the verb rules, without an enclitic too."""
        if name_like:
            # Only a word the pack writes with its capital, with a prefix or as it stands (ngoMsombuluko, iSonto); eGoli is
            # Johannesburg, not the pack's igoli "goal".
            found = {token} if token in self.kinds else set(self.forms.get(token, ())) | self.nominal_lemmas(token)
            return frozenset(w for w in found if w in self.capitalised)
        if token in ZULU_FORMS:
            return frozenset(ZULU_FORMS[token].split(","))
        if token in FRAGMENTS or UNLISTED_NOUNS.match(token):
            return frozenset()
        if token in self.kinds:
            return frozenset({token})
        if token in self.special:
            return frozenset({self.special[token]})
        weak = lambda w: len(w) < 4 and w not in SHORT_VERBS
        nounish = lambda words: all(self.kinds[w] & {"noun"} and not self.kinds[w] & {"pron", "adj", "num"} for w in words)
        if token in self.forms:
            # A form a verb's table shares with a pronoun's or adjective's is theirs (wakhe "his", not akha's "he built";
            # abanye "others", not nya's), and so is one the noun rules read (kuyona "it is it", not ona's).
            found = self.forms[token]
            others = {w for w in found if "verb" not in self.kinds[w]}
            # A noun's form without its first vowel (sithole, zithole of isithole "heifer") is a verb's first when it reads
            # as one with a subject concord (sithole "we found", thola's; mfana is umfana's, no "resemble him!").
            vowelless = all("noun" in self.kinds[w] and any(token == b[1:] for b in self.bases[w]) for w in others)
            subject = self._subject_verbs(token)
            if others and vowelless and subject:
                return subject
            # A noun's other forms a verb writes alike (ekhala "of the nose", "crying") are shared, as the build decides
            # (NOUN_SHARE); its own forms in either number (abafana, amakhala) are its.
            if others and nounish(others) and not any(token in self.bases[w] for w in others):
                readings = self._other_readings(token)
                if readings:
                    return self._collide(token, others, readings)
            if others:
                return frozenset(others)
            # So is a form the noun rules read (ngibhala "I am a wheelbarrow", "I write"), and one they read as no noun
            # is the verb's (angene, ngena's, no "it is by four").
            strong = {w for w in found if not weak(w)}
            nominal = self.nominal_lemmas(token)
            if nominal and strong and nounish(nominal) and self._copula_nouns(token):
                return self._collide(token, nominal, strong)
            if nominal and strong and all(self.kinds[w] <= {"adj", "num"} for w in nominal):
                return frozenset(strong)
            if nominal:
                return nominal
            # A short verb's table shares many forms with a longer verb's (ungene "you should not rain", na's, is "you should
            # enter", ngena's): the longer verb's, when the verb rules read it so; and a noun's regular form's (sana, usana's
            # without its vowel, no "it should rain").
            if not strong:
                regular = self.regular_lemmas(token)
                if regular and not any(weak(w) for w in regular):
                    return regular
            if not all(weak(w) for w in found):
                found = {w for w in found if not weak(w)}
            if all(len(w) < 4 for w in found):
                longer = {w for w in self.verb_lemmas(token) if not weak(w) and (len(w) >= 4 or all(weak(f) for f in found))}
                if longer:
                    return frozenset(longer)
            return frozenset(found)
        found = self.nominal_lemmas(token)
        if found:
            # A noun the rules read with a copula (ayibhala "it is no wheelbarrow") that a verb's form writes alike (ayibhala
            # "he writes it") is shared, as the build decides (NOUN_SHARE); not one with na, nga or a possessive (nabafana,
            # singabakhiqizi).
            verbs = self._other_readings(token)
            if verbs and nounish(found) and self._copula_nouns(token):
                return self._collide(token, found, verbs)
            return found
        found = self.verb_lemmas(token) or self._compound(token)
        if found:
            return found
        for clitic in ENCLITICS:
            # No -ni "what" on a locative but an infinitive's (ekugcineni) or on a plural imperative (enzeni): esangweni,
            # isango's, is no "what is kissed?".
            if (clitic == "ni" and token[0] in "eo" and token.endswith(("eni", "ini")) and not token.startswith(("eku", "ekw"))
                    and not self._verbs(token[:-2], lambda p, e, k, s: p == "" and self._prefix_fits(p, e, k, False))):
                continue
            if token.endswith(clitic) and len(token) >= len(clitic) + 3:
                bare = token[:-len(clitic)]
                found = self.verb_lemmas(bare)
                if found:
                    return found
        return self.regular_lemmas(token)


def _tokens(line):
    """A line's words, lowercase, each with how it is written: "prefixed" with a capital after its first letter (uThemba,
    eGoli, isiZulu), "name" with a capital first inside a sentence, "initial" with one at a sentence's start, else "word"
    (an all-capitals word too). A prefix cut off by a hyphen (ngo-2010, e-Afrika, i-ofisi, ama-website, njenge-ANC) is no
    word."""
    line = line.translate(APOSTROPHES)
    for match in WORD.finditer(line):
        word = match.group(0)
        after = line[match.end():match.end() + 2]
        if after[:1] == "-" and len(after) == 2 and after[1].isalnum() and (len(word) <= 4 or after[1].isdigit() or after[1].isupper()
                                                                           or after[1] in VOWELS):
            continue
        if word.isupper() and len(word) > 1:
            yield word.lower(), "word"
        elif any(c.isupper() for c in word[1:]):
            yield word.lower(), "prefixed"
        elif word[0].isupper():
            yield word.lower(), "initial" if SENTENCE_START.search(line[:match.start()]) else "name"
        else:
            yield word.lower(), "word"


APOSTROPHES = str.maketrans("´`’‘", "''''")


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            yield line.rstrip("\n").partition("\t")[2]


def _counts(download):
    """Each kind of text's word counts, the words written with a capital after a prefix apart, and the names of the corpora
    counted. A word with a capital at a sentence's start counts unless the texts write it with its capital inside sentences
    more often than without (Mandela, Durban)."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    initial = {kind: defaultdict(int) for kind in WEIGHTS}
    capitalised = defaultdict(int)
    named = defaultdict(int)
    used = defaultdict(list)
    seen = set()

    def count(kind, line):
        # The web text repeats its menus and notices on every page: a sentence is counted once.
        if kind == "web":
            if line in seen:
                return
            seen.add(line)
        for word, written in _tokens(line):
            if written == "prefixed":
                named[word] += 1
            elif written == "name":
                capitalised[word] += 1
            elif written == "initial":
                initial[kind][word] += 1
            else:
                counts[kind][word] += 1

    for kind, corpus in LEIPZIG:
        path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
        if path is None:
            continue
        used["leipzig"].append(corpus)
        for sentence in _leipzig_sentences(path):
            count(kind, sentence)
    for kind, corpus, version in OPUS:
        name = f"opus-{corpus.lower()}-{version}-zu.txt.gz"
        path = download(f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/zu.txt.gz", name)
        if path is None:
            continue
        used[corpus].append(version)
        with gzip.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                count(kind, line.strip())
    lowercase = defaultdict(int)
    for kind_counts in counts.values():
        for word, n in kind_counts.items():
            lowercase[word] += n
    for kind, kind_counts in initial.items():
        for word, n in kind_counts.items():
            if lowercase[word] >= capitalised[word]:
                counts[kind][word] += n
    return counts, named, used


def label_of(used):
    parts = []
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    for corpus in ("CCAligned", "wikimedia"):
        if used.get(corpus):
            parts.append(f"OPUS {corpus} {', '.join(used[corpus])} zu ({LICENCES[corpus]})")
    return " + ".join(parts)


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the lexicon's words by their forms' counts, each kind of text weighted by WEIGHTS."""
    lexicon = Lexicon(dictionaries)
    counts, named, used = _counts(download)
    label = label_of(used)
    share = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        if not total:
            continue
        for form, n in kind_counts.items():
            share[form] += WEIGHTS[kind] * n / total
            raw[form] += n
    # A word written like a name counts where the pack or the noun rules make it a word (iSonto, ngoMsombuluko), at the
    # weight of the whole text.
    total = sum(sum(c.values()) for c in counts.values())
    named_heads = {}
    for form, n in named.items():
        heads = lexicon.lemmas(form, name_like=True)
        if heads:
            named_heads[form] = heads
            share[form] += n / total
            raw[form] += n

    heads = {form: named_heads.get(form) or lexicon.lemmas(form) for form in share}
    own = defaultdict(float)
    for form, value in share.items():
        if len(heads[form]) == 1 and not lexicon.takes_over(form):
            own[next(iter(heads[form]))] += value
    # A verb that is also a form of a commoner word than itself is that word's, unless the text uses it as a verb with forms
    # of its own as well.
    for form in share:
        if lexicon.takes_over(form):
            others = lexicon.forms[form] - {form}
            if others and max(own.get(w, 0.0) for w in others) > share[form] and own.get(form, 0.0) < share[form]:
                heads[form] = frozenset(others)
    # A noun's form a verb writes alike is the noun's unless its other forms are much rarer (NOUN_SHARE).
    for form in share:
        if form in lexicon.collisions and heads[form] == lexicon.lemmas(form):
            nominal, others = lexicon.collisions[form]
            rare = max(own.get(w, 0.0) for w in nominal) < NOUN_SHARE * max(own.get(w, 0.0) for w in others)
            heads[form] = others if rare else nominal
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    for form, value in share.items():
        words = heads[form]
        if not words:
            continue
        weights = {w: own.get(w, 0.0) for w in words}
        whole = sum(weights.values())
        for word in words:
            part = weights[word] / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            scores[word] += value * part
            totals[word] += raw[form]
            forms[word][form] = forms[word].get(form, 0.0) + value * part

    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


_CHECKER = {}


def checker_lexicon():
    if "lexicon" not in _CHECKER:
        _CHECKER["lexicon"] = Lexicon()
    return _CHECKER["lexicon"]


def checker_word(token, ranks, name_like=False):
    """
    The list's word [token] is a form of by the rules the list was built with, the commonest when it may be several, or None:
    a verb form the list does not have (ngiyambona: bona), a noun with a prefix (nomngane: umngane), a pronoun's (nabo: bona).
    """
    found = [(ranks[w], w) for w in checker_lexicon().lemmas(token, name_like) if w in ranks]
    return min(found)[1] if found else None
