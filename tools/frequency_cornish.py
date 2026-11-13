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
    **_to("homma", "homma", "hobma"),
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
}
