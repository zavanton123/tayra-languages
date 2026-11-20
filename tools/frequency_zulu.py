"""
The Zulu frequency list (build_frequency_list.py --language zu).

Zulu is written conjunctively: a verb carries its subject, tense, negative and object concords in front and its
extensions and ending behind (ngiyambona "I see him", abangazange bahambe "who never went", kuthiwa "it is said"), a noun
takes its prepositions and copulas on (nomuntu "with a person", ngesikhathi "at the time", ngumuntu "it is a person",
ekhaya "at home"), and a name keeps its capital behind such a prefix (uThemba, kaThemba, eGoli, ngoMsombuluko). A corpus
so has hundreds of forms of a common word, most of which no pack lists. Each text word is read here as the pack's word or
form (dictionaries/zu-en.sqlite, from the English Wiktionary, whose inflection tables are generated and long), a noun or
pronoun with a prefix (nominal_lemmas), or a verb by its prefix and ending as the pack's tables show them, with an object
concord or an extension (verb_lemmas); a word of the lexicon keeps its own count, and a form of several words is shared
by how common each is by the forms of no other.

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
FORMS_LISTED = 400
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
    **{p + "khona": "khona" for p in ("e", "i", "be", "li", "se", "yi", "si", "lu", "bu", "zi", "ba", "ngi", "u", "ni")},
    # "Your" (wakho, yakho, ezakho), wena's, not khona's "of it".
    **{c + "akho": "wena" for c in ("w", "b", "y", "l", "s", "z", "lw", "bw", "kw", "", "ow", "ab", "ey", "el", "es", "ez", "olw",
                                    "obw", "okw", "aw", "k", "kub", "kuy", "kul", "kus", "kuz", "kulw", "kukw", "nob", "noy", "nez")},
    # "With it" (class 5 and 11), not ingalo "arm".
    "ngalo": "lona",
    "muntu": "umuntu",
    # The class 6 "these" written in full.
    "lawa": "la",
    # "You say", "it says", "who says", far more than uthi "stick".
    "uthi": "thi",
    "kuthi": "thi",
    "othi": "thi",
}


def strip_tones(word):
    import unicodedata
    return unicodedata.normalize("NFC", "".join(c for c in unicodedata.normalize("NFD", word) if not unicodedata.combining(c)))


# The object concords a verb may take before its stem, and the shapes they take before a stem in a vowel (ngiyakwazi,
# uyamazi, ngiyabazi).
OBJECTS = ["ngi", "ku", "m", "mu", "si", "ni", "ba", "wu", "yi", "li", "wa", "zi", "lu", "bu"]
OBJECTS_BEFORE_VOWEL = ["ngi", "kw", "m", "mw", "s", "n", "b", "w", "y", "l", "z", "lw", "bw"]
# The extensions a verb's stem takes (applicative, causative, reciprocal, passive, neuter and their joins): bonwa, hambela,
# hambisa, thandana, thandiwe, enzeka, bonakala, hambisisa, thandelana.
EXTENSIONS = ["el", "is", "an", "w", "iw", "ek", "akal", "elw", "isw", "isis", "elel", "elan", "isan", "an", "ekel",
              "eliw", "isiw", "aniw", "ulw", "ul"]
# The enclitics written onto a word: -ni "what", -phi "where", -ke "then", -nje "just".
ENCLITICS = ["ni", "phi", "ke", "nje"]
# Prefixes ending in a that join a noun's first vowel (a + i is e, a + u is o): na "and, with", nga "by, at", njenga "like",
# kuna "than", nanga "and by".
JOINING = ["na", "nga", "njenga", "kuna", "nanga", "nanjenga", "njengoba"]
# Prefixes that take a noun in u- or i- without its vowel (kubaba, kaThemba, ngumuntu, yinto) and the possessive concords
# with ka (likaNkulunkulu).
DROPPING = {p: "u" for p in ("ku", "ka", "ngu", "wu", "kuka", "lika", "sika", "zika", "yaka", "waka", "baka", "kwaka",
                              "lwaka", "buka", "aka", "eka", "naka", "ngaka")}
DROPPING.update({"yi": "i", "ngi": "i"})
# The concords of the quantitative -nke "all" (wonke, bonke, zonke) and of -dwa "alone" (yedwa, bodwa, ngedwa).
NKE = {c + "nke" for c in ("o", "wo", "bo", "zo", "yo", "lo", "so", "ko", "lwo", "bwo", "mo")}
DWA = {c + "dwa" for c in ("nge", "we", "ye", "so", "bo", "lo", "zo", "yo", "wo", "lwo", "bwo", "ni", "si", "eyo", "owo", "elilo",
                           "esiso", "olulo", "obubo", "okuko", "ababo", "ezizo", "omo", "abo")}
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
            if lemma in self.kinds and form != lemma and re.fullmatch(r"[a-z]+", form):
                self.forms[form].add(lemma)
                self.table[lemma].add(form)
        for word in stems_only:
            if word not in self.kinds:
                self.forms[word].add(self.combining[word])
        self.verbs = {w for w, ks in self.kinds.items() if "verb" in ks and len(w) > 1}
        self.pronouns = {w for w, ks in self.kinds.items() if "pron" in ks and len(w) > 3}
        self.nominals = {w for w, ks in self.kinds.items() if ks & {"noun", "pron"}}
        self._learn_verb_patterns()

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
                self.whole[verb].add(verb)
            for ext in (["iw"] if len(root) < 3 else EXTENSIONS):
                if root + ext + "a" not in self.kinds:
                    self.stems[root + ext].add(verb)
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

    @staticmethod
    def kind_of(stem):
        return "V" if stem[0] in VOWELS else "C"

    def _prefix_fits(self, prefix, ending, kind):
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
        return False

    @functools.lru_cache(maxsize=None)
    def verb_lemmas(self, token):
        """The verbs [token] is a form of by the tables' prefixes and endings, with an object concord or an extension; those
        of the shortest prefix only, so the longest stem: bonakala is bona's (bon-akal-a), not kala's."""
        for i in range(0, min(len(token) - 1, self.longest_prefix) + 1):
            prefix = token[:i]
            out = set()
            for j in range(i + 1, len(token) + 1):
                stem, ending = token[i:j], token[j:]
                kind = self.kind_of(stem)
                if stem in self.stems and self._prefix_fits(prefix, ending, kind):
                    out |= self.stems[stem]
                if not ending and stem in self.whole and self._prefix_fits(prefix, "a", kind):
                    out |= self.whole[stem]
            if out:
                return frozenset(out)
        return frozenset()

    def _word_or_form(self, base, kinds=None):
        """[base] as a word of the lexicon or the words it is a form of, of the parts of speech [kinds] when given."""
        if base in self.kinds and (kinds is None or self.kinds[base] & kinds):
            return {base}
        found = {w for w in self.forms.get(base, ()) if kinds is None or self.kinds[w] & kinds}
        return found

    @functools.lru_cache(maxsize=None)
    def nominal_lemmas(self, token):
        """The words [token] is a noun's, pronoun's or adverb's form of with a prefix: nomuntu, ngesikhathi, nabantu (umuntu,
        isikhathi), kubaba, ngunkulunkulu, likaNkulunkulu, nami, ngayo (mina, yona), wonke, yedwa, ngaphansi, kancane."""
        if token in NKE:
            return frozenset({"nke"} & set(self.kinds))
        if token in DWA:
            return frozenset({"dwa"} & set(self.kinds))
        nouns = {"noun", "pron"}
        for prefix in JOINING:
            stem = prefix[:-1]
            if not token.startswith(stem) or len(token) < len(stem) + 3:
                continue
            vowel, rest = token[len(stem)], token[len(stem) + 1:]
            bases = {"a": ["a"], "e": ["i", "e"], "o": ["u", "o"]}.get(vowel, [])
            found = set()
            for v in bases:
                found |= self._word_or_form(v + rest, nouns)
            if found:
                return frozenset(found)
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
        if token in FRAGMENTS:
            return frozenset()
        if token in self.kinds:
            return frozenset({token})
        if token in self.special:
            return frozenset({self.special[token]})
        if token in self.forms:
            # A form a verb's table shares with a noun's, pronoun's or adjective's is theirs (wakhe "his", not akha's "he
            # built"; abanye "others", not nya's), and so is one the noun rules read (kuyona "it is it", not ona's).
            found = self.forms[token]
            others = {w for w in found if "verb" not in self.kinds[w]}
            # A noun's form without its first vowel (sithole, zithole of isithole "heifer") is a verb's first when it reads
            # as one (sithole "we found", thola's).
            vowelless = all(any(v + token == w or v + token in self.table[w] for v in VOWELS) for w in others)
            if others and vowelless and self.verb_lemmas(token):
                return self.verb_lemmas(token)
            if others:
                return frozenset(others)
            nominal = self.nominal_lemmas(token)
            if nominal:
                return nominal
            # A short verb's table shares many forms with a longer verb's (ungene "you should not rain", na's, is "you should
            # enter", ngena's): the longer verb's, when the verb rules read it so.
            weak = lambda w: len(w) < 4 and w not in SHORT_VERBS
            if not all(weak(w) for w in found):
                found = {w for w in found if not weak(w)}
            if all(len(w) < 4 for w in found):
                longer = {w for w in self.verb_lemmas(token) if not weak(w) and (len(w) >= 4 or all(weak(f) for f in found))}
                if longer:
                    return frozenset(longer)
            return frozenset(found)
        found = self.nominal_lemmas(token)
        if found:
            return found
        found = self.verb_lemmas(token)
        if found:
            return found
        for clitic in ENCLITICS:
            if token.endswith(clitic) and len(token) >= len(clitic) + 3:
                bare = token[:-len(clitic)]
                found = self.verb_lemmas(bare)
                if found:
                    return found
        return frozenset()


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
