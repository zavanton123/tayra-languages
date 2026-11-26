"""
Cornish for tools/build_frequency_list.py.

Cornish has no corpus but its Wikipedia, written in Kernewek Kemmyn (KK), the Standard Written Form and Late Cornish
spellings side by side, and the dictionary pack (Wiktionary) lacks many of its commonest words: the past of bos (o, veu,
esa), the pronoun i "they", lies, nebes, hemma, kyns, ytho, the contractions (dhe'n, ha'y), the months and days (names to
the pack). So, besides the overrides below:

- only the articles count (namespace 0), without their headings: a quarter of the dump's words are its Lua modules,
  templates and talk pages, in English (is, id, the, error, with), and every article ends "Devynnow" (sources);
- the curly apostrophe is the straight one (y’n is y'n);
- a form the pack lacks is the pack's form it mutates or respells (unmutated, or the KK spelling of an SWF or Late one:
  dhiworth is dhyworth, radn is rann, dus is tus, gemeras is kemeras), the commonest where several;
- and else a word of its own, when the articles write it in lower case at least OWN_LOWER of the time and OWN_COUNT
  times or more, and English hardly writes it: names are capitalised.
"""

import json
import os
import re
import sqlite3

# A form the pack lacks becomes a word of its own from this many articles' uses, and this share of them in lower case, if
# it is at least OWN_LETTERS long and English writes it less than OWN_ENGLISH times as often as these articles do (the
# articles quote English titles and captions: links, novel, screenwriter).
OWN_COUNT = 8
OWN_LOWER = 0.6
OWN_LETTERS = 3
OWN_ENGLISH = 0.05

# The initial mutations as (written, radical): soft, aspirate, hard and mixed, as Kernewek Kemmyn writes them.
MUTATIONS = (
    ("dh", "d"), ("v", "b"), ("v", "m"), ("j", "ch"), ("w", "gw"), ("w", "g"), ("g", "k"), ("b", "p"), ("d", "t"),
    ("h", "k"), ("f", "p"), ("th", "t"),
    ("p", "b"), ("t", "d"), ("kw", "gw"), ("k", "g"),
    ("f", "b"), ("f", "m"), ("hw", "gw"), ("h", "g"),
)
VOWELS = "aeiouy"
# The other spellings' ways of writing Kernewek Kemmyn's (pedn for penn, kebmyn for kemmyn, whedhel for hwedhel, cath for kath).
RESPELLINGS = (("dn", "nn"), ("bm", "mm"), ("wh", "hw"), ("ck", "k"), ("c", "k"), ("oe", "oo"), ("ue", "eu"), ("i", "y"), ("y", "i"), ("y", "e"),
               ("e", "y"))


def unmutated(form):
    """The radical words a lowercased Cornish form may be, by the mutations."""
    out = [radical + form[len(written):] for written, radical in MUTATIONS if form.startswith(written) and len(form) > len(written) + 1]
    # Soft mutation drops a g before a vowel, l or r (an gath, y'n lyver; lowen of glowen is no word, but wolok of golok is w).
    if form[:1] in VOWELS or form[:1] in ("l", "r"):
        out.append("g" + form)
    return out


def respelled(form):
    """The Kernewek Kemmyn spellings a form written in another spelling may be."""
    out = []
    for written, kk in RESPELLINGS:
        if written in form:
            out.append(form.replace(written, kk))
            if form.count(written) > 1:
                at = form.find(written)
                while at >= 0:
                    out.append(form[:at] + kk + form[at + len(written):])
                    at = form.find(written, at + 1)
    return out


def candidates(form):
    """The pack spellings a form it lacks may stand for: unmutated, respelled, or both."""
    found = unmutated(form) + respelled(form)
    found += [r for u in unmutated(form) for r in respelled(u)]
    return list(dict.fromkeys(c for c in found if c != form))


# A noun's plural endings and a verb's commonest endings, with what the word may end in instead (tylleryow of tyller, trenow
# of tren; dhemedhas of demedhi, studhyas of studhya, dhehwelas of dehweles).
NOUN_ENDINGS = (("yow", ""), ("ow", ""), ("oryon", "or"), ("oryon", "er"), ("yon", ""), ("ys", ""))
VERB_ENDINGS = (("sons", ""), ("ons", ""), ("yas", ""), ("as", ""), ("is", ""), ("ys", ""), ("yes", ""), ("ens", ""), ("yn", ""))
VERB_NOUN_ENDINGS = ("a", "i", "ya", "el", "es", "os", "he", "")


def inflected(form):
    """
    The words a form the pack lacks may be a plural or a verb's form of, as (word, "noun" or "verb", 0 as written or 1
    unmutated or respelled).
    """
    out = []
    for tier, spelled in [(0, form)] + [(1, c) for c in candidates(form)]:
        for ending, base in NOUN_ENDINGS:
            if spelled.endswith(ending) and len(spelled) - len(ending) >= 3:
                out.append((spelled[:-len(ending)] + base, "noun", tier))
        for ending, _ in VERB_ENDINGS:
            if spelled.endswith(ending) and len(spelled) - len(ending) >= 3:
                out += [(spelled[:-len(ending)] + end, "verb", tier) for end in VERB_NOUN_ENDINGS]
    return [c for c in dict.fromkeys(out) if c[0] != form]


def _to(word, *forms):
    return {form: [word] for form in forms}


CORNISH_OVERRIDES = {
    # The pack has no entry for these common forms, or files them under a look-alike.
    **_to("bos", "o", "veu", "feu", "esa", "esens", "esen", "eses", "bydh", "vydh", "bedhons", "vedhons", "bia", "via", "ew", "vo", "jeves", "jevo", "jevydh",
          "esov", "esos", "eson", "esowgh", "esons"),
    **_to("gul", "wra", "wre", "gwra", "hwra", "wrussa", "wrella", "wrello", "wrug", "hwrug", "gwrug", "gwrussa", "wrons",
          "wruss", "gwrons"),
    **_to("godhvos", "wodhya", "godhya", "wodhyen", "wodhyens"),
    **_to("dos", "dheuth", "deuth", "teuth", "dueth", "tos", "deuthons"),
    **_to("merwel", "verwis", "merwis", "verwas"),
    **_to("mos", "ethen", "ethons"),
    **_to("gallos", "ell", "yll", "hyll", "hyllir", "gyll", "gallsa", "galla"),
    **_to("kavos", "gawas", "kawas", "kavas", "gavas"),
    **_to("gweles", "welas", "gwelas"),
    **_to("kemeres", "gemeras", "kemeras"),
    **_to("leverel", "lever"),
    **_to("komprehendya", "gomprehend", "komprehend"),
    **_to("ynwedh", "ewedh"),
    # Pronouns and pointing words.
    **_to("i", "i", "anjei", "enjei", "jei"),
    **_to("ty", "ty"),
    **_to("hemma", "hemma", "hemm", "hebma"),
    **_to("homma", "homma", "hobma", "homm"),
    **_to("henna", "hedna"),
    **_to("honna", "honna", "hodna"),
    **_to("a", "anedha", "anodho", "anedhi"),
    # Little words.
    **{w: [w] for w in ("lies", "nebes", "kyns", "hwath", "mysk", "myns", "ytho", "may", "na", "martesen", "warbarth",
                        "nagonan", "kyn", "hepken", "yth", "kettel", "ottomma", "ottena", "ott", "otta", "nevra", "byth",
                        "kemmys", "aral")},
    **_to("ytho", "etho"),
    **_to("may", "mayth"),
    **_to("na", "nag"),
    **_to("kyns", "kens"),
    **_to("aral", "arall", "erell", "erel"),
    **_to("meur", "moyha", "voyha"),
    **_to("ynter", "yntra"),
    # Contractions, words of their own as y'n, a'n and ha'n are; e'n and i'n are y'n in other spellings.
    **{w: [w] for w in ("dhe'n", "dre'n", "ha'y", "a'y", "dhe'y", "dh'y", "y'ga", "ha'ga", "a'ga", "dh'aga", "a's", "y'm",
                        "a'm", "ha'm", "dhe'm", "y'th", "a'th", "ha'th", "dhe'th", "y'gan", "ha'gan", "a'gan")},
    **_to("y'n", "e'n", "i'n", "y'n"),
    # Months and days, words of the language (names to the pack), and their other spellings.
    **{w: [w] for w in ("genver", "hwevrer", "meurth", "ebrel", "metheven", "gortheren", "gwynngala", "hedra", "kevardhu",
                        "sul", "lun", "mergher", "yow", "gwener", "sadorn")},
    **_to("hwevrer", "whevrer", "hwevrel"),
    **_to("ebrel", "ebryl"),
    **_to("gwynngala", "gwyngala", "gwydngala"),
    # Headwords the pack's mutation entries put first, and forms filed under a look-alike.
    **_to("bras", "bras", "vras", "brassa", "vrassa", "fras", "broas", "vroas", "brâs"),
    **_to("bri", "bri", "vri", "fri"),
    **_to("enys", "enys", "ynys", "enesow", "ynysow"),
    **_to("chi", "chy", "chiow", "chei", "jy", "ji"),
    **_to("tre", "treven", "trevow"),
    **_to("te", "te"),
    **_to("tesen", "tesen", "tesennow", "desen"),
    **_to("tro", "dro", "troyow"),
    **_to("drolla", "drolla", "drollys"),
    **_to("tren", "tren", "trenow"),
    **_to("genas", "genas"),
    **_to("tren", "drenow"),
    **_to("gallos", "hyllons", "yllons", "allons", "gyllons"),
    **_to("styrya", "styrys", "styryas"),
    **_to("styr", "styryow"),
    **{w: [w] for w in ("marow", "gwerin", "offis", "gorow", "styr")},
    **_to("kavos", "kevys", "kevis", "hevis"),
    **_to("tas", "tas", "das", "thas", "tasow"),
    **_to("teg", "teg", "tekka"),
    **_to("tevi", "teves", "tevys", "tevis", "tevas"),
    # Forms the pack's look-alikes take: dha "your" is not da or dhe, keffrys "also" not kevri, mowes "girl" not mos.
    **_to("dha", "dha"),
    **_to("keffrys", "keffrys", "keffres"),
    **_to("kever", "kever", "hever"),
    **_to("gwerin", "werin"),
    **_to("kara", "keras", "kerens", "herens", "kerys", "keryes", "keryn", "heryas"),
    **_to("mowes", "mowes", "vowes", "mowesi", "mowysi", "vowysi"),
    **_to("fos", "fos", "fosow"),
    **_to("gallos", "ylli", "hylli", "hyllsons", "hyllyn", "kyllons"),
    **_to("gul", "gwruthyl", "wruthyl", "kwruthyl", "hwre", "wrussons", "wren", "hwrussons", "wrens", "kwrellys", "hwrella",
          "wrewgh", "gwren", "gwrewgh", "gwre", "kwrella", "hwrussys", "hwren", "wrussyn", "hwrussens"),
    **_to("gwrys", "gwres"),
    **_to("dri", "dros", "dhros"),
    **_to("troos", "tros"),
    **_to("tren", "dren"),
    **_to("garr", "diwarr", "dhiwarr"),
    **_to("gwari", "waria"),
    **_to("henna", "henn"),
    **_to("hwarvos", "hwer", "wher"),
    **{w: [] for w in ("di", "dia", "tes", "ten", "tenow", "tolla", "tollys", "tosa", "or", "be", "area", "arrow")},
    # One word written several ways, or a plural, comparative or participle the pack lacks, under its word.
    **_to("cita", "sita"),
    **_to("sodhogel", "soedhogel"),
    **_to("kansblydhen", "kansblydhen", "kansbledhen", "kansvledhen", "cansvledhen"),
    **_to("ughel", "uhel", "uhella"),
    **_to("ughelder", "uhelder"),
    **_to("kepar", "pekara", "pecara"),
    **_to("arbennik", "arbennek", "arbednek"),
    **_to("tardhans", "tardhansow"),
    **_to("tiredh", "tiredhow"),
    **_to("tybyans", "tybyansow"),
    **_to("triger", "drigoryon", "trigoryon"),
    **_to("soudor", "soudoryon"),
    **_to("servis", "servisyow"),
    **_to("studhya", "studhyas"),
    **_to("displegya", "dhisplegyas"),
    **_to("drehevel", "derevys"),
    **_to("kawsya", "kawsys"),
    **_to("posek", "posekka"),
    **_to("henwel", "henwys"),
    **_to("treweythyow", "terweythyow"),
    **_to("kesrosweyth", "kesroesweyth"),
    **_to("gnas", "gnasow"),
    **_to("oberer", "oberoryon"),
    **_to("eghen", "ehen"),
    **_to("poynt", "boynt"),
    **_to("gwaynya", "waynyas"),
    **_to("stoff", "stoffow"),
    **_to("a-varr", "a-varra"),
    **_to("poblans", "poblans", "boblans"),
    **_to("romans", "romans", "romansow"),
    **_to("tevi", "dyv", "tyv"),
    **_to("kewsel", "kews", "gews"),
    **_to("usya", "usys"),
    **_to("devnydhya", "devnydhys", "dhevnydhys", "tevnydhys"),
    **_to("gelwel", "gelwys", "elwis", "helwys", "kelwys", "welwis"),
    **_to("passya", "passyes"),
    **_to("rekordya", "rekordys"),
    **_to("sewya", "sew"),
    **_to("warbarth", "war-barth"),
    **_to("dydh", "jydh", "jedh"),
    **_to("dos", "deu", "dheu", "teu"),
    # The language's own name, written with a capital as a name is, is a word as Sowsnek is.
    **_to("kernewek", "kernewek"),
    # Forms the pack glosses as their own words: "had" (y'n jeva, a's tevo), "is he", "is done", "gets".
    **_to("bos", "jeva", "tevo", "ywa"),
    **_to("gul", "hwrer", "gwrythul"),
    **_to("kavos", "gyv"),
    **_to("gallos", "halja"),
}

CORNISH_EXCLUDE = {
    # English and markup the articles carry (titles, captions, file names), filed under Cornish look-alikes or words of
    # their own.
    "the", "thy", "is", "his", "id", "hid", "error", "errors", "for", "with", "by", "tag", "has", "add", "but", "err", "check",
    "was", "arg", "both", "dates", "how", "go", "why", "four", "ford", "he", "of", "and", "in", "at", "from",
    "wew", "gew", "frynk", "brynk", "gemm",
    # Markup and abbreviations of the image, table and timeline syntax (skeusennik is the image option "thumb").
    "jpg", "jpeg", "png", "svg", "gif", "km", "align", "colspan", "rowspan", "nbsp", "www", "org", "com", "item", "thumb",
    "uk", "geograph", "label", "text", "wdt", "mm", "kg", "cm", "till", "lower", "skeusennik", "http", "https", "html",
    "px", "upright", "center", "enter", "to", "do", "al", "hell", "coor", "wikibase", "klass", "ndash", "sparql", "bgcolor", "gje", "uji", "née", "département",
    "americanus", "arctos", "thibetanus", "nervus", "genus", "cropped",
    # Latin names of species, English, French, German, Estonian and Lithuanian words, a colour code and file extensions the
    # articles carry.
    "melanoleuca", "ursinus", "maakond", "ogv", "anjeii", "maritimus", "malayanus", "tabacicolor", "pratense", "glauca",
    "phylum", "cumulus", "essayist", "theropods", "bretonne", "d'outre-mer", "immer", "peahoone", "apskritis", "panoramio",
    "efefef", "webm", "tif",
}


# Words of the counts that are another word's forms, by the kw-en pack's glosses and the regular endings (fold): a mutated
# word (dhownder, gawsya), a plural (diredhow, kolovennow), a past, passive or participle (recevas, skrifir, profys), a
# comparative (uskissa) or another spelling (tommder, scol), counted for that word.
# The initial mutations Kernewek Kemmyn writes most, soft (an gath, y'n dre), preferred where a pair reads both ways.
SOFT = {("g", "k"), ("d", "t"), ("b", "p"), ("v", "b"), ("v", "m"), ("dh", "d"), ("w", "gw"), ("w", "g"), ("j", "ch")}
# The other spellings' ways of writing Kernewek Kemmyn's, besides RESPELLINGS: a word written either way goes to the one
# written more.
VARIANTS = (("oe", "o"), ("nn", "n"), ("n", "nn"), ("ll", "l"), ("l", "ll"), ("mm", "m"), ("m", "mm"), ("ss", "s"), ("s", "ss"),
            ("tt", "t"), ("rr", "r"), ("r", "rr"), ("j", "s"), ("gh", "h"), ("h", "gh"), ("dh", "th"), ("th", "dh"))
KK = (("dn", "nn"), ("bm", "mm"), ("wh", "hw"), ("ck", "k"), ("c", "k"), ("oe", "oo"), ("ue", "eu"))
PLURALS = ("yow", "ow", "yon", "ys", "yes", "es")
AGENT_PLURALS = (("oryon", "or"), ("oryon", "er"), ("yoryon", "yer"), ("oryon", "yer"), ("yon", "yer"), ("ydhyon", "ydh"))
VERB_FORMS = ("sons", "yons", "ons", "yas", "as", "yes", "ys", "is", "ir", "yr", "ens", "yn")
# A comparative doubles or hardens the last consonant (downna, uskissa, tekka).
COMPARED = (("nn", "n"), ("ss", "s"), ("kk", "g"), ("kk", "k"), ("tt", "d"), ("tt", "t"), ("pp", "b"), ("rr", "r"), ("ll", "l"),
            ("mm", "m"), ("", ""))
# Letters of no Cornish spelling: Croatian, Estonian, Swedish and other words the articles quote (županija, mõisa, län).
CORNISH_LETTERS = set("abcdefghijklmnopqrstuvwxyz'-âêîôûŷ")
# Pairs the rules read as forms that are words apart, or forms the glosses do not show (tavellow "probes" of tavell "craft").
FOLD_APART = {"kowas", "hil", "fol", "erbys", "hwenn", "honna", "hesken", "bothen", "kowles", "bynk", "fesont",
              "kelyn", "maglen", "faster", "arvedhen", "kegis", "homma", "otta", "blous"}
FOLD_TO = {"tavellow": "tavell", "pras": "bras", "dhifres": "difres", "bennfenten": "pennfenten", "brovi": "provia",
           "kesstrivya": "kesstrifa", "ervys": "arva", "gwersyon": "versyon", "godhav": "godhevel", "gryj": "krysi",
           "kerghyn": "kyrghynn", "studhyer": "studhyor", "istorier": "istorior", "termys": "termow"}


def _glosses(dictionaries):
    path = os.path.join(dictionaries, "kw-en.sqlite")
    glosses, kinds = {}, {}
    if not os.path.exists(path):
        return glosses, kinds
    with sqlite3.connect(path) as db:
        for word, kind, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
            kinds.setdefault(word, set()).add(kind)
            glosses.setdefault(word, []).extend(g for sense in json.loads(senses) for g in sense.get("glosses", []))
    return glosses, kinds


def _meanings(glosses):
    """A word's glosses as plain English words, without brackets, "to" or articles."""
    out = set()
    for gloss in glosses:
        for part in re.split(r"[,;]", re.sub(r"\([^)]*\)", "", gloss.lower())):
            part = re.sub(r"^(to|a|an|the|is) ", "", part.strip()).strip(" .")
            if part:
                out.add(part)
    return out


def _english_words(meanings):
    return {t for m in meanings for t in re.findall(r"[a-z]{3,}", m) if t not in ("the", "and", "one", "for", "with")}


def _related(first, second):
    """Whether two glosses share an English stem: four letters, or a three-letter word (received, receive; is used, use)."""
    return any(x[:min(4, len(x), len(y))] == y[:min(4, len(x), len(y))] for x in _english_words(first) for y in _english_words(second))


def _plural(meaning):
    irregular = {"child": "children", "foot": "feet", "tooth": "teeth", "mouse": "mice", "person": "people", "woman": "women",
                 "man": "men", "goose": "geese", "leaf": "leaves", "life": "lives", "wife": "wives"}
    out = {meaning + "s", meaning + "es", irregular.get(meaning, "")}
    if meaning.endswith("y"):
        out.add(meaning[:-1] + "ies")
    if meaning.endswith("man"):
        out.add(meaning[:-3] + "men")
    return out - {""}


def _variants(word):
    out = set(respelled(word))
    for written, other in VARIANTS + KK:
        at = word.find(written)
        while at >= 0:
            out.add(word[:at] + other + word[at + len(written):])
            at = word.find(written, at + 1)
    if word.endswith("k"):
        out.add(word[:-1] + "g")
    # An agent noun in -er or -or (souder, soudor; kampyer, kampyor), a participle in -ys or -yes (grondys, grondyes).
    for written, other in (("er", "or"), ("or", "er"), ("ys", "yes"), ("yes", "ys")):
        if word.endswith(written) and len(word) > len(written) + 2:
            out.add(word[:-len(written)] + other)
    return out - {word}


def _soft(form, radical):
    if form[:1] in VOWELS + "lr" and radical == "g" + form:
        return True
    return any(form.startswith(w) and radical == r + form[len(w):] for w, r in SOFT)


def fold(scores, forms, pos, dictionaries):
    """Moves each word of the counts that is another's form into that word (with its forms), and drops foreign ones."""
    glosses, kinds = _glosses(dictionaries)
    meaning = {w: _meanings(g) for w, g in glosses.items()}
    own = {w for w, to in CORNISH_OVERRIDES.items() if to == [w]}
    for word in [w for w in scores if not set(w) <= CORNISH_LETTERS]:
        del scores[word]
        forms.pop(word, None)

    def alike(a, b):
        return bool(meaning.get(a, set()) & meaning.get(b, set()))

    def related(a, b):
        """Whether two words' glosses share an English stem (received, receive; is used, use), or the first has none."""
        return a not in glosses or _related(meaning.get(a, ()), meaning.get(b, ()))

    def plural_of(a, b):
        return any(m in _plural(n) for m in meaning.get(a, ()) for n in meaning.get(b, ()))

    def kind(word):
        return kinds.get(word, set()) | pos.get(word, set())

    def listed(word):
        return word in scores and scores[word] > 0

    def mutated(word):
        """The radical a mutated word is, where the pack names it or glosses both alike, or the word has no entry."""
        for gloss in glosses.get(word, ()):
            for named in re.findall(r"\(([^,()]+), mutated\)|mutation of (\S+)", gloss):
                named = (named[0] or named[1]).strip().lower()
                named = named if listed(named) else owner.get(named)
                # Not a word with senses of its own besides (ker "dear", also "hard mutation of ger").
                if named and named != word and all(
                        "mutat" in g or _related(_meanings([g]), meaning.get(named, ())) for g in glosses[word]):
                    return named
        found = []
        for radical in unmutated(word):
            if radical == word or word in unmutated(radical) and not _soft(word, radical) and (
                    _soft(radical, word) or scores.get(radical, 0) <= scores[word]):
                continue
            # Or the radical in another spelling (gevrenn, kevren), or a verb's stem (dhrehev, drehevel).
            for spelled in [radical] + sorted(_variants(radical)) + [radical + end for end in VERB_NOUN_ENDINGS + ("ya",) if end]:
                if spelled == word or not listed(spelled):
                    continue
                if alike(word, spelled) or spelled == radical and word not in glosses and spelled in glosses:
                    found.append((spelled == radical, _soft(word, radical), scores[spelled], spelled))
        return max(found)[-1] if found else None

    def kk_spelling(word):
        """The Kernewek Kemmyn spelling of a word written in another (tobmder, tommder; scol, skol), glossed alike."""
        spelled = {word[:i] + k + word[i + len(w):] for w, k in KK for i in range(len(word)) if word.startswith(w, i)}
        spelled |= {word.replace(w, k) for w, k in KK if w in word}
        found = [o for o in spelled if o != word and listed(o) and (alike(word, o) or word not in glosses)]
        return max(found, key=lambda o: (scores[o], o)) if found else None

    def variant(word):
        """The spelling of the word written more, where the pack glosses both alike (tomder, tommder; askell, askel)."""
        found = [o for o in _variants(word) if listed(o) and alike(word, o) and not any(w in o and k in word for w, k in KK)
                 and (scores[o], o) > (scores[word], word)]
        return max(found, key=lambda o: (scores[o], o)) if found else None

    def plural(word):
        """The singular of a plural (diredhow, kolovennow, skoloryon), by the endings and the pack's glosses."""
        if word.endswith("es") and any("female" in g or "woman" in g or "(woman)" in g for g in glosses.get(word, ())):
            return None
        bases = []
        for spelled in [word] + unmutated(word):
            for ending, base in [(e, "") for e in PLURALS] + list(AGENT_PLURALS):
                if spelled.endswith(ending) and len(spelled) - len(ending) >= 2:
                    stem = spelled[:-len(ending)]
                    bases += [stem + base, stem + "a"]
                    if not base and len(stem) > 2 and stem[-1] == stem[-2]:
                        bases.append(stem[:-1])
                    # A singular in -el, -er before the plural's -low, -row (kopel, koplow; kilometer, kilometrow; titel, titlys).
                    if not base and len(stem) > 3 and stem[-1] in "lr" and stem[-2] not in VOWELS:
                        bases.append(stem[:-1] + "e" + stem[-1])
        bases += [v for b in bases for v in sorted(_variants(b))]
        found = []
        for base in dict.fromkeys(bases):
            if base == word or not listed(base) or "verb" in kind(base) and "noun" not in kind(base):
                continue
            if word not in glosses and (base in glosses or base in forms) or plural_of(word, base) or (
                    alike(word, base) and word.endswith(("ow", "yow", "yon"))):
                found.append(base)
        # An English-looking plural in -s (maters), or one in -i (pennseri; lytherennegi of lytherennek), only where the pack
        # glosses it as the plural.
        for base in ([word[:-1]] if word.endswith(("s", "i")) else []) + ([word[:-2] + "k"] if word.endswith("gi") else []):
            if not found and listed(base) and plural_of(word, base):
                found.append(base)
        return max(found, key=lambda b: scores[b]) if found else None

    def singular(word):
        """The plural a mutated singular the pack lacks goes to, where its radical is no word of the counts (grib, kribow)."""
        if word in glosses:
            return None
        for radical in unmutated(word):
            if listed(radical):
                return None
            for p in (radical + "ow", radical + "yow", radical + radical[-1] + "ow"):
                if listed(p) and p in glosses:
                    return p
        return None

    def verb_form(word):
        """The verb a past, passive or participle is (recevas, skrifir, profys, guntellas), by the endings."""
        if word in glosses and not kinds.get(word, set()) <= {"verb", "adj"}:
            return None
        found = []
        for ending in VERB_FORMS:
            if not word.endswith(ending) or len(word) - len(ending) < 3:
                continue
            stem = word[:-len(ending)]
            for spelled in [stem] + unmutated(stem) + list(_variants(stem)) + [v for u in unmutated(stem) for v in _variants(u)]:
                for end in VERB_NOUN_ENDINGS + ("ya", "he", "y"):
                    verb = spelled + end
                    if verb != word and listed(verb) and "verb" in kind(verb) and related(word, verb):
                        # A verb-noun before a noun the pack also glosses as a verb's form (nivera, not niver).
                        found.append((bool(end), scores[verb], verb))
        return max(found)[-1] if found else None

    def compared(word):
        """The adjective a comparative or superlative in -a is (uskissa, kalessa, ledanna)."""
        if not word.endswith("a") or len(word) < 5 or word in glosses and "adj" not in kinds.get(word, set()):
            return None
        stem = word[:-1]
        for spelled in [stem] + unmutated(stem):
            for doubled, base in COMPARED:
                if spelled.endswith(doubled):
                    adjective = spelled[:len(spelled) - len(doubled)] + base
                    graded = word not in glosses or any(m.endswith(("er", "est")) or m.startswith(("more", "most")) for m in meaning[word])
                    if adjective != word and listed(adjective) and "adj" in kind(adjective) and related(word, adjective) and graded:
                        return adjective
        return None

    def verb_spelling(word):
        """Another verb-noun of the same verb, glossed alike (teudha, teudhi; ametha, amethi)."""
        if "verb" not in kinds.get(word, set()):
            return None
        stems = {word[:-len(e)] for e in VERB_NOUN_ENDINGS + ("ya", "he") if e and word.endswith(e)}
        found = []
        for stem in stems:
            for spelled in [stem] + unmutated(stem):
                for end in [e for e in VERB_NOUN_ENDINGS + ("ya", "he") if e]:
                    other = spelled + end
                    if other != word and listed(other) and "verb" in kinds.get(other, set()) and alike(word, other) and (
                            scores[other] > scores[word]):
                        found.append(other)
        return max(found, key=lambda o: scores[o]) if found else None

    def merge(rules):
        """Folds each word one of the rules finds another word for into it, following chains."""
        parent = {}
        for word in sorted(scores, key=lambda w: (-scores[w], w)):
            if word in own or word in FOLD_APART or "-" in word or "'" in word:
                continue
            to = next((t for t in (rule(word) for rule in rules) if t and t != word), None)
            if to:
                parent[word] = to

        def root(word):
            seen = [word]
            while word in parent and len(seen) < 8:
                word = parent[word]
                if word in seen:
                    # Two words each the other's form: the commoner is the word.
                    return max(seen[seen.index(word):], key=lambda w: (scores.get(w, 0), w))
                seen.append(word)
            return word

        roots = {word: root(word) for word in parent}
        for word, to in roots.items():
            if to != word and word in scores:
                scores[to] += scores.pop(word)
                for form, share in forms.pop(word, {}).items():
                    forms[to][form] = forms[to].get(form, 0) + share
        for word, to in roots.items():
            for form in forms.get(to, ()):
                owner[form] = to
        return roots

    # The commonest word each form is listed under.
    owner = {}
    for word in sorted(scores, key=lambda w: scores[w]):
        for form in forms.get(word, ()):
            owner[form] = word
    # Mutations and Kernewek Kemmyn's spellings first, so that a word written several ways has its whole count before the
    # other spellings and the endings are weighed.
    folded = merge([FOLD_TO.get, mutated, kk_spelling])
    folded.update(merge([variant]))
    folded.update(merge([plural, singular, verb_form, compared, verb_spelling]))
    return folded
