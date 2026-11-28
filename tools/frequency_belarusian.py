"""
Belarusian for tools/build_frequency_list.py.

The English Wiktionary has few Belarusian entries (5,500 words: no музей, фестываль, сцэна, тэрыторыя), and only those
have forms tables, so a third of the corpus's words went to no word and the list stopped at 3,840 words. The Russian and
Polish Wiktionaries' Belarusian entries (the be-ru and be-pl packs, 23,000 and 20,000 words) name most of the words it
lacks, without forms; the corpus's forms of them are found by the changes of ending the English pack's tables show. The
news write taraškievica too (пасьля, музэй, прэзыдэнт), whose spellings go to their official ones. What no pack has is
induced from the corpus (транспарт, квіток, званне), and WORDS adds the rest by hand (mend).
"""
import io
import json
import os
import re
import sqlite3
import tarfile
from collections import Counter, defaultdict

# The kinds of word that inflect, whose forms the endings find, and the other kinds the packs' words may be.
INFLECTED = ("noun", "adj", "verb")
UNINFLECTED = ("adv", "num", "pron", "prep", "conj", "particle", "intj")
LETTERS = re.compile("[абвгдеёжзійклмнопрстуўфхцчшыьэюя']+")
# How far a form and its word may differ at the end, in letters of the word and of the form.
WORD_TAIL, FORM_TAIL = 4, 6
# How often a change of ending must turn a word into its form in the English pack's tables to be used.
LEAST_SUPPORT = 3
# The endings of a noun's, an adjective's and a verb's forms. The pack's tables also list the words made from a word
# (гарадок and гарадскі of горад, заяўка of заява, the imperfective выказваць of выказаць), which a form must not be taken for.
NOUN_ENDINGS = ("а", "я", "у", "ю", "е", "э", "і", "ы", "о", "ё", "ом", "ём", "ем", "эм", "ам", "ям", "ой", "ою", "ёй", "ёю",
                "ей", "ею", "эй", "эю", "ай", "аю", "яй", "яю", "ах", "ях", "амі", "ямі", "мі", "аў", "яў", "оў", "ёў")
ADJECTIVE_ENDINGS = ("ы", "і", "ая", "яя", "ае", "яе", "ое", "ую", "юю", "ага", "яга", "ога", "аму", "яму", "ому", "ым", "ім",
                     "ай", "яй", "ой", "аю", "яю", "ою", "ых", "іх", "ымі", "імі", "ыя", "ія")
VERB_ENDINGS = ("у", "ю", "еш", "эш", "ёш", "іш", "ыш", "е", "э", "ем", "эм", "ём", "ім", "ым", "еце", "эце", "ёце", "яце", "іце",
                "ыце", "юць", "уць", "аць", "яць", "іць", "ыць", "ў", "ла", "ло", "лі", "ўшы", "шы", "учы", "ючы", "ачы", "ячы",
                "й", "йце", "ь", "ьце", "і") + tuple(c + e for c in "нт" for e in ADJECTIVE_ENDINGS)
VOWELS = "аяуюеэіыоё"
# The letters a noun's or adjective's dictionary form ends in that are its ending.
ENDING_LETTERS = "аяоеёьйыіэу"
# The consonants a stem's last one may turn into before an ending (рука, руцэ; горад, горадзе; калектыў, калектыва).
ALTERNATING = {"к": ["к", "ц", "ч"], "г": ["г", "з", "ж"], "х": ["х", "с", "ш"], "т": ["т", "ц"], "д": ["д", "дз"], "ц": ["ц", "ч"],
               "ў": ["ў", "в"], "в": ["в", "ў"], "з": ["з", "ж"], "с": ["с", "ш"]}
# The soft sign taraškievica writes before a consonant (сьнег, пасьля, пытаньне, зьвярнуцца) and the official spelling does
# not: the corpus's news write both. A form so spelled is the word of its official spelling.
TARASKIEVICA_SOFT = re.compile("(?<=[сзцн])ь(?=[бвгджзйклмнпрстфхцчш])")


def normalise(form):
    """A form as the list writes it: the apostrophe straight (з'ява, not з’ява or зʼява), and a first ў as у, which the
    list adds back to each form (ўжо of ужо, after a vowel)."""
    form = form.replace("’", "'").replace("ʼ", "'")
    return "у" + form[1:] if form.startswith("ў") else form


def official(form):
    """The official spelling of a form written in taraškievica's soft signs (and зь before a iotated vowel for з': зьявіцца),
    and the preposition зь (з)."""
    if form == "зь":
        return "з"
    return TARASKIEVICA_SOFT.sub("", re.sub("^(а?)зь(?=[еёюяі])", r"\1з'", form))


# Taraškievica's other spellings of loanwords, each tried throughout a word that no pack explains as written: зы and сы
# for зі and сі (прэзыдэнт, сытуацыя), э for е (музэй, дакумэнт, эўрапейскі), зт and зк for ст and ск (грамадзтва,
# грамадзкі), ля, лю, лё for ла, лу, ло (заля, клюб, калёнія), and -іяў for -ій (пазыцыяў).
TARASKIEVICA_LOANS = (("зы", "зі"), ("сы", "сі"), ("э", "е"), ("зт", "ст"), ("зк", "ск"), ("ля", "ла"), ("лю", "лу"), ("лё", "ло"))


def official_spellings(form):
    """The spellings a form written in taraškievica may have officially, fewest changes first."""
    form = official(form)
    found = [form]
    for old, new in TARASKIEVICA_LOANS:
        if old in form:
            found += [f.replace(old, new) for f in found]
    if form.endswith("іяў"):
        found += [f[:-3] + "ій" for f in found]
    return found[1:]


def lexicon(dictionaries, pos, lemmas):
    """
    The be-ru and be-pl packs' lowercase words, with their kinds, that the English pack has neither as a word nor as a form
    (годзе "enough", which is год's; стаў "pond", which is стаць's), nor written in taraškievica (пытаньне for пытанне).
    """
    words = defaultdict(set)
    for target in ("ru", "pl"):
        path = os.path.join(dictionaries, f"be-{target}.sqlite")
        if not os.path.exists(path):
            continue
        db = sqlite3.connect(path)
        for word, word_lc, kind in db.execute("SELECT word, word_lc, pos FROM entries"):
            if word == word_lc and kind in INFLECTED + UNINFLECTED and LETTERS.fullmatch(word_lc) and len(word_lc) > 1 \
                    and word_lc not in pos and not lemmas.get(word_lc):
                words[word_lc].add(kind)
        db.close()
    return {w: kinds for w, kinds in words.items() if official(w) == w or not (official(w) in words or official(w) in pos
                                                                             or lemmas.get(official(w)))}


def endings(pos, lemmas):
    """How often each change of ending turns a word of a kind into its form, by the English pack's tables, keyed by
    (kind, the word's ending, the form's ending, the letters before them)."""
    rules = defaultdict(int)
    for form, of in lemmas.items():
        for lemma in of:
            kinds = pos.get(lemma, set()) & set(INFLECTED)
            if len(kinds) != 1 or form == lemma:
                continue
            kind = next(iter(kinds))
            stem = os.path.commonprefix([form, lemma])
            # A noun's or adjective's ending is the change, not the letters after it (сцэна, сцэнах: а to ах, not to х).
            if stem == lemma and kind != "verb" and lemma[-1] in ENDING_LETTERS:
                stem = stem[:-1]
            if len(stem) < 2 or len(lemma) - len(stem) > WORD_TAIL or len(form) - len(stem) > FORM_TAIL:
                continue
            for context in (stem[-2:], stem[-1:], ""):
                rules[(kind, lemma[len(stem):], form[len(stem):], context)] += 1
    return {key: n for key, n in rules.items() if rules[key[:3] + ("",)] >= LEAST_SUPPORT and inflects(*key[:3])}


def inflects(kind, word_tail, form_tail):
    """Whether a change of ending makes a form of a word of the kind, not another word."""
    if kind == "verb":
        # A reflexive verb is a word of its own (складацца, not складаць's).
        if form_tail.endswith(("ся", "цца")) != word_tail.endswith(("ся", "цца")):
            return False
        bare = form_tail[:-2] if form_tail.endswith("ся") else form_tail[:-3] if form_tail.endswith("цца") else form_tail
        # Nor is an imperfective in -ваць or a verb in -нуць made from it (выказваць of выказаць, крыкнуць of крычаць).
        if bare.endswith(("ваць", "нуць")) and not word_tail.endswith(bare[-4:]):
            return False
        # A past without ending is a verb's in -чы, -ці or -нуць (мог, вёз, прывык); a bare stem of another is no form of it (дар).
        if not bare or bare[-1:] not in VOWELS and not bare.endswith(VERB_ENDINGS):
            return word_tail.endswith(("чы", "ці", "нуць"))
        return bare.endswith(VERB_ENDINGS)
    ends = NOUN_ENDINGS if kind == "noun" else ADJECTIVE_ENDINGS
    ending = max([e for e in ends if form_tail.endswith(e)] + [""], key=len)
    rest, base = form_tail[:len(form_tail) - len(ending)], word_tail[:-1] if word_tail[-1:] in ENDING_LETTERS else word_tail
    # The stem's end as the word has it, its last consonant changed, a vowel before it dropped (сродак, сродку) or, with
    # no ending, put in (падтрымка, падтрымак); the -ін of a people's singular goes (гараджанін, гараджане).
    return any(rest == variant for variant in stems(base)) or kind == "noun" and (
        not ending and base and rest in (v + base for v in "аеэоё") or base.endswith("ін") and rest == base[:-2]) or (
        kind == "noun" and not word_tail and form_tail in ("зе", "зі"))


def stems(base):
    """The ways a stem's end may be written before an ending."""
    dropped = [base] + [base[:i] + base[i + 1:] for i in range(len(base) - 1) if base[i] in VOWELS]
    return {v[:-1] + c for v in dropped for c in ALTERNATING.get(v[-1:], [v[-1:]])} if base else {""}


def candidates(form, rules, tails):
    """The words of each kind whose form [form] may be by [rules], with how well the change of ending is attested."""
    found = defaultdict(int)
    for cut in range(max(2, len(form) - FORM_TAIL), len(form) + 1):
        stem, tail = form[:cut], form[cut:]
        for word_tail in tails.get(tail, ()):
            word = stem + word_tail
            if word == form or len(word) < 3:
                continue
            for kind in INFLECTED:
                # The change must be seen after the stem's last letter (стане of стаць, not мяне of мяць).
                if not rules.get((kind, word_tail, tail, stem[-1:])):
                    continue
                score = sum(weight * rules.get((kind, word_tail, tail, context), 0)
                            for weight, context in ((100, stem[-2:]), (10, stem[-1:]), (1, "")))
                found[(word, kind)] = max(found[(word, kind)], score)
    return found


def word_of(form, rules, tails, kinds_of):
    """The likeliest word of [kinds_of] whose form [form] is by [rules], and its kind; or None."""
    known = [(score, word, kind) for (word, kind), score in candidates(form, rules, tails).items() if kind in kinds_of(word)]
    return max(known)[1:] if known else None


def dictionary_kinds(word):
    """The kinds of word a word's spelling may be the dictionary form of."""
    kinds = set()
    if word.endswith(("ць", "ці", "чы", "цца", "ціся", "чыся")):
        kinds.add("verb")
    elif word.endswith(("ы", "і")):
        kinds.add("adj")
    elif not word.endswith(("у", "ю", "э", "ы", "і")):
        kinds.add("noun")
    return kinds


def capitals(dictionaries, corpora):
    """The share of each word's uses in the Leipzig corpora that are capitalised, as a name's are."""
    upper, total = defaultdict(int), defaultdict(int)
    for corpus in corpora:
        path = os.path.join(dictionaries, "frequency-sources", f"leipzig-{corpus}.tar.gz")
        if not os.path.exists(path):
            continue
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[2].isdigit() and " " not in parts[1]:
                    word = normalise(parts[1].lower())
                    total[word] += int(parts[2])
                    if parts[1][:1].isupper():
                        upper[word] += int(parts[2])
    return {w: upper[w] / n for w, n in total.items() if n}


# Everyday words no pack has: particles, adverbs and prepositions, which have no forms to find them by (жа, ва as у is
# written before some consonants, нядаўна, неяк), and многі, whose dictionary form texts never write (многія, многіх).
WORDS = {
    "жа": "particle", "ва": "prep", "аж": "particle", "ажно": "particle", "ані": "particle", "многі": "adj",
    "нядаўна": "adv", "неяк": "adv", "лёгка": "adv", "невядома": "adv", "некалі": "adv", "ніяк": "adv", "нямала": "adv",
    "недалёка": "adv", "непадалёк": "adv", "падобна": "adv",
}
# A form no pack word explains is a word of its own (транспарт, квіток, званне, назваць) when the corpus has its dictionary
# form too, it has at least INDUCED_FORMS forms, and no more than INDUCED_CAPITALISED of their uses are capitalised (a name's:
# Пазьняк, Кавалькова).
INDUCED_FORMS = 3
INDUCED_CAPITALISED = 0.4
# The share of a noun's uses its feminine instrumental (сферай, рамкай) must have for it to be feminine.
FEMININE_SHARE = 0.01


def mend(freq, pos, lemmas, common, dictionaries, overrides, excluded, corpora):
    """
    Adds the other packs' words and WORDS to the list builder's [pos] and [common], and the reflexive verbs the corpus has
    of the packs' verbs; sends each corpus form that no pack has to the word its ending makes it a form of, a form in
    taraškievica spelling to its official spelling's word, and the forms still left to the words they induce.
    """
    rules = endings(pos, lemmas)
    tails = {}
    for kind, word_tail, form_tail, _ in rules:
        tails.setdefault(form_tail, set()).add(word_tail)
    for word, kinds in list(lexicon(dictionaries, pos, lemmas).items()) + [(w, {k}) for w, k in WORDS.items()]:
        pos[word].update(kinds)
        common[word] = word
    # A reflexive verb the corpus has in its infinitive (складацца, праводзіцца) whose verb is a pack's.
    for verb in [w for w in common if "verb" in pos.get(w, ())]:
        reflexive = verb[:-2] + "цца" if verb.endswith("ць") else verb + "ся" if verb.endswith(("ці", "чы")) else None
        if reflexive and freq.get(reflexive) and reflexive not in pos and not lemmas.get(reflexive):
            pos[reflexive].add("verb")
            common[reflexive] = reflexive

    def kinds_of(word):
        return pos.get(word, set()) if word in common else set()

    def open_form(form):
        return form not in pos and not lemmas.get(form) and form not in overrides and form not in excluded \
            and LETTERS.fullmatch(form) and len(form) >= 3

    for form in sorted(freq):
        if open_form(form):
            found = word_of(form, rules, tails, kinds_of)
            if found:
                lemmas[form] = [found[0]]
    # A taraškievica spelling goes where its official one does (сёньня as сёння, пасьля as пасля, зь as з).
    for form in sorted(freq):
        spelled = official(form)
        if spelled != form and form not in common and form not in overrides and (spelled in common or lemmas.get(spelled)):
            lemmas[form] = [spelled] if spelled in common else list(lemmas[spelled])
    # A loanword in taraškievica spelling goes where its official spelling does (музэй, прэзыдэнта, грамадзкіх); a short
    # participle (створана, размешчана) to its participle.
    for form in sorted(freq):
        if not open_form(form):
            continue
        for spelled in official_spellings(form) + ([form[:-1] + "ы"] if form.endswith(("на", "та")) else []):
            found = [spelled] if spelled in common else lemmas.get(spelled) or (lambda f: f and [f[0]])(
                word_of(spelled, rules, tails, kinds_of))
            if found:
                lemmas[form] = list(found)
                break
    # The words the forms still left induce, each the dictionary form of the forms that may be its: a form counts for
    # each reading by how well its change of ending is attested against its best one's (рэжымаў is far likelier рэжым's
    # than a рэжыма's), its own spelling as a dictionary form counting fully.
    capitalised = capitals(dictionaries, corpora)
    left = [f for f in sorted(freq) if open_form(f) and official(f) == f]
    readings = {}
    for form in left:
        found = dict(candidates(form, rules, tails))
        best = max(found.values(), default=1)
        found = {key: score / best for key, score in found.items()}
        for kind in dictionary_kinds(form):
            found[(form, kind)] = 1.0
        readings[form] = found
    groups, shares = defaultdict(set), defaultdict(float)
    for form, found in readings.items():
        for word_kind, weight in found.items():
            groups[word_kind].add(form)
            shares[word_kind] += freq[form] * weight
    induced = {}
    for (word, kind), forms in groups.items():
        share = sum(freq[f] for f in forms)
        if word in forms and kind in dictionary_kinds(word) and len(forms) >= INDUCED_FORMS and word not in pos \
                and not lemmas.get(word) and sum(freq[f] * capitalised.get(f, 0) for f in forms) <= INDUCED_CAPITALISED * share:
            induced[(word, kind)] = shares[(word, kind)]
    # A noun in -а whose forms are a noun's without it (рэжыма, рэжым; участка, участак; квітка, квіток) is that noun, unless
    # the corpus has the form only a feminine noun has, its instrumental (сферай: сфера, not сфер), or the other is its
    # genitive plural (рамак of рамка) and there is no masculine one (рамкаў). Taraškievica gives feminine nouns a genitive
    # plural in -аў too (рэформаў), which so says nothing of the gender.
    nouns_of = defaultdict(set)
    for word, kind in induced:
        if kind == "noun":
            for form in groups[(word, kind)]:
                nouns_of[form].add(word)
    for word in sorted(w for w, kind in induced if kind == "noun" and w[-1:] in "ая"):
        stem = word[:-1]
        feminine = sum(freq.get(stem + e, 0) for e in ("ай", "аю", "яй", "яю", "ой", "ою", "ёй", "ёю")) > FEMININE_SHARE * sum(
            freq[f] for f in groups[(word, "noun")])
        shared = defaultdict(int)
        for form in groups[(word, "noun")]:
            for other in nouns_of[form]:
                shared[other] += 1
        for other in sorted(o for o, n in shared.items() if n >= 2 and o[-1:] not in VOWELS and (o, "noun") in induced):
            plural = other in [stem] + [stem[:-1] + v + stem[-1:] for v in "аое"] and not any(
                freq.get(stem + e) for e in ("аў", "оў"))
            if (word, "noun") in induced:
                induced.pop((other, "noun") if feminine or plural else (word, "noun"))
    # A genitive plural in -ій or -ый (рэпрэсій, намінацый) is a feminine noun's in -я, which texts may seldom write, where
    # the corpus has its instrumental or, without a masculine one's (каментарыем), it ends as the feminine loans in -цыя,
    # -сія, -зія, -гія, -фія do.
    for word in sorted(w for w, kind in induced if kind == "noun" and w.endswith(("ій", "ый")) and len(w) > 4):
        feminine, forms = word[:-1] + "я", groups[(word, "noun")]
        share = sum(freq[f] for f in forms)
        if feminine not in pos and not lemmas.get(feminine) and (
                sum(freq.get(word[:-1] + e, 0) for e in ("яй", "яю")) > FEMININE_SHARE * share
                or freq.get(word[:-1] + "ем", 0) <= FEMININE_SHARE * share and word.endswith(("цый", "сій", "зій", "гій", "фій"))):
            induced[(feminine, "noun")] = induced.pop((word, "noun"))
            for form in forms:
                readings[form][(feminine, "noun")] = readings[form].pop((word, "noun"))
    # A noun in -а with no feminine instrumental and a masculine genitive plural (удара, ударамі, удараў) is the genitive of
    # a masculine noun whose nominative the endings did not reach: the stem itself (удар), or with ў for в (масіў, заліў)
    # or a vowel put back before ц or к (спартовец, сродак), as the corpus spells it.
    for word in sorted(w for w, kind in induced if kind == "noun" and w[-1:] == "а"):
        stem, forms = word[:-1], groups[(word, "noun")]
        if any(freq.get(stem + e, 0) for e in ("ай", "аю", "ой", "ою")) or not any(freq.get(stem + e, 0) for e in ("аў", "оў")):
            continue
        nominatives = [stem, stem[:-1] + "ў" if stem.endswith("в") else None, stem[:-2] + "вец" if stem.endswith("ўц") else None,
                       stem[:-1] + "ец" if stem.endswith("ц") else None, stem[:-1] + "ак" if stem.endswith("к") else None]
        nominative = max((n for n in nominatives if n and freq.get(n) and open_form(n) and (n, "noun") not in induced),
                         key=lambda n: freq[n], default=None)
        if nominative:
            induced[(nominative, "noun")] = induced.pop((word, "noun"))
            for form in forms | {nominative}:
                found = readings.setdefault(form, {})
                found[(nominative, "noun")] = found.pop((word, "noun"), 1.0)
    made = defaultdict(list)
    for form, found in readings.items():
        options = [(induced[wk], weight, wk) for wk, weight in found.items() if wk in induced]
        if options:
            made[max(options)[2][0]].append((form, max(options)[2][1]))
    # And one in taraškievica spelling is its official spelling's word (прэзыдэнцкі, прэзідэнцкі; зьявіцца, з'явіцца).
    for word in sorted(made):
        spelled = next((v for v in official_spellings(word) + ([word[:-1] + "ы"] if word.endswith(("на", "та")) else [])
                        if v in common or v in made), None)
        for form, kind in made[word]:
            if spelled:
                lemmas[form] = [spelled]
            else:
                pos[word].add(kind)
                common[word] = word
                if form != word:
                    lemmas[form] = [word]




# After the counting (fold): a word's count is only its uses as that word, and a word the writers of the courses take for
# another's form, partner or spelling is that word's.
#
# A capitalised use inside a sentence (Цыганкоў, МАЗ, Бор) is a name's, which counts for no word of the list; a word seen
# fewer than NAME_LEAST times keeps its count, as too few uses tell no name.
CYRILLIC = re.compile("[А-ЯЁІЎа-яёіў'’ʼ]+")
NAME_LEAST = 5
# Colloquial words and abbreviations the courses do not teach.
NOT_WORDS = {"ейны", "англ"}
# The pack's glosses that name another word a word is a spelling of, anywhere ("symbols (classical spelling of сімволіка)"),
# or a form of, in its first sense ("ring (form of кальцо)", "statues (form of статуя)", "(diminutive of вока) little eye").
SPELLING_OF = re.compile(r"\((?:[a-z]+ )*(?:spelling|variant|form) of ([а-яёіў']+)\)")
SPELLING_KINDS = ("spelling of", "variant of", "classical form of", "nonstandard form of")
FORM_OF = re.compile(r"\((?:[a-z.]+ )*(?:form|plural|diminutive|fem\.) of ([а-яёіў']+)")
# Other spellings the glosses do not name: taraškievica's (актор, the standard акцёр; мажлівасць, магчымасць), variants
# (складвацца, наведнік, атрымваць, кошка), a misread one (каласка, made of каляскі by official_spellings).
VARIANTS = {"актор": "акцёр", "складвацца": "складацца", "наведнік": "наведвальнік", "кошка": "котка", "каласка": "каляска",
            "атрымваць": "атрымліваць", "мажлівасць": "магчымасць"}
# The letters taraškievica writes for the official ones, each tried at one place of a word at a time (элемэнт, элемент;
# рэжысэр, рэжысёр; энэргія, энергія), as official_spellings tries them throughout.
TARASKIEVICA_LETTERS = (("э", "е"), ("э", "ё"), ("зы", "зі"), ("сы", "сі"), ("ля", "ла"), ("лю", "лу"), ("лё", "ло"),
                        ("зт", "ст"), ("зк", "ск"))
# English words of no weight when glosses are compared.
GLOSS_STOP = {"to", "the", "a", "an", "of", "one", "oneself", "someone", "something", "somebody", "be", "is", "in", "on", "at",
              "by", "for", "with", "from", "up", "out", "off", "into", "onto", "over", "as", "and", "or", "its", "it", "about",
              "down", "away", "back", "get", "become", "each", "other"}
ENGLISH = re.compile("[a-z]+")
# The endings an English word's stem is found without (formation, form; division, divide; filled, fill).
ENGLISH_ENDINGS = ("ations", "ation", "itions", "ition", "tions", "tion", "sions", "sion", "ments", "ment", "ings", "ing",
                   "ances", "ance", "ences", "ence", "ities", "ity", "ers", "er", "ed", "es", "al", "ure", "ive", "ly", "s", "e")
# The first glosses of a participle (reinforced, done), a comparative (more important, later, largest) and a diminutive or
# feminine (little eye, Roma woman).
PARTICIPLES = set("""
done made written taken given seen known held built born found sold told left kept brought bought thought caught taught sent
spent set put cut shut shown drawn grown thrown broken chosen frozen spoken stolen forgotten hidden beaten eaten fallen driven
risen bound laid paid said read led fed met lost won hung struck stuck dug sworn torn worn borne lit shot sunk begun run
come gone felt dealt meant burnt learnt rebuilt overcome forbidden withdrawn upheld undertaken mistaken shaken sought fought
dead
""".split())
COMPARATIVE = {"more", "most", "less", "least", "later", "better", "best", "worse", "worst", "latest", "earlier"}
DIMINUTIVE = {"little", "small", "diminutive", "affectionate", "tiny", "woman", "female", "girl"}
# The endings of English nouns that name an action whatever the verb's glosses (awarding, construction, payment); others
# do so when the verb's glosses have their stem (arrival of arrive, closure of close, issue of issue).
ACTION_NOUNS = ("ing", "tion", "sion", "ment")
VERB_TAILS = ("ць", "ці", "чы", "цца", "ціся", "чыся")
# The consonants a stem's last one turns into before an ending (абвясціць, абвешчаны; аднавіць, адноўлены, аднаўляць),
# and the vowels a stress shift turns (узмацніць, узмоцнены; горад, гарадок).
STEM_CHANGES = (("сц", "шч"), ("ст", "шч"), ("с", "ш"), ("з", "ж"), ("т", "ч"), ("ц", "ч"), ("дз", "дж"), ("д", "дж"),
                ("здз", "ждж"), ("б", "бл"), ("п", "пл"), ("в", "ўл"), ("м", "мл"), ("ф", "фл"), ("сц", "ск"))
VOWEL_CHANGES = (("а", "о"), ("о", "а"), ("я", "е"), ("е", "я"), ("я", "ё"), ("ё", "я"), ("е", "ё"), ("а", "э"), ("э", "а"))
# The infinitives' ending pairs of an aspect pair whose stems are alike (змясціць, змяшчаць; улічыць, улічваць;
# звярнуць, звяртаць; набраць, набіраць; унесці, уносіць), the perfective's first, a stem's last consonant changed by
# STEM_CHANGES and up to two vowels by VOWEL_CHANGES (усвядоміць, усведамляць).
ASPECT_TAILS = (
    ("іць", "аць"), ("іць", "яць"), ("ыць", "аць"), ("іць", "ляць"), ("іць", "ваць"), ("ыць", "ваць"), ("аць", "ваць"),
    ("іць", "ьваць"), ("аць", "ьваць"), ("ыць", "ываць"), ("іць", "іваць"), ("ець", "аць"), ("ець", "ваць"), ("эць", "аць"),
    ("аваць", "оўваць"), ("яваць", "ёўваць"), ("іць", "йваць"), ("уць", "уваць"), ("іць", "оўваць"),
    ("нуць", "таць"), ("нуць", "каць"), ("нуць", "гаць"), ("нуць", "аць"), ("нуць", "ваць"), ("нуць", "даць"),
    ("браць", "біраць"), ("абраць", "біраць"), ("зваць", "зываць"), ("слаць", "сылаць"), ("няць", "німаць"),
    ("няць", "ымаць"), ("няць", "маць"), ("няць", "ймаць"), ("несці", "носіць"), ("весці", "водзіць"), ("везці", "возіць"),
    ("йсці", "ходзіць"), ("ысці", "ыходзіць"), ("ехаць", "язджаць"), ("даць", "даваць"), ("стаць", "ставаць"),
    ("бегчы", "бягаць"), ("мерці", "міраць"), ("перці", "піраць"), ("церці", "ціраць"), ("легчы", "лягаць"),
    ("класці", "кладаць"), ("чаць", "чынаць"), ("цягнуць", "цягваць"), ("помніць", "памінаць"), ("ліць", "льняць"),
    ("сесці", "садзіцца"), ("бачыць", "бачваць"), ("знаць", "знаваць"),
)
# The prefixes a perfective adds to its imperfective without changing its sense (рабіць, зрабіць; пісаць, напісаць), and
# those that do so to a loanword's verb (зафіксаваць, адрэагаваць); a verb they make is the other's partner only when its
# glosses add nothing to the other's first sense (to the letter for LOAN_PREFIXES).
EMPTY_PREFIXES = ("з", "с", "зь", "па", "на", "пра")
LOAN_PREFIXES = ("за", "ад")
# The English words that tell a prefixed verb's sense from its verb's (збегчы "to run away", бегчы "to run").
PARTICLES = {"up", "out", "off", "away", "down", "back", "over", "into"}
# The imperfectives made with a suffix (надаваць, выпісваць), which no prefix makes a perfective of.
SECONDARY = ("даваць", "ставаць", "знаваць", "ываць", "іваць", "оўваць", "ёўваць")


def name_shares(dictionaries, corpora):
    """The share of each word's uses in the Leipzig sentences that are capitalised inside a sentence, a name's."""
    named, total = Counter(), Counter()
    for corpus in corpora:
        path = os.path.join(dictionaries, "frequency-sources", f"leipzig-{corpus}.tar.gz")
        if not os.path.exists(path):
            continue
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                words = CYRILLIC.findall(line.partition("\t")[2])
                for i, word in enumerate(words):
                    key = normalise(word.lower())
                    total[key] += 1
                    if i and word[:1].isupper():
                        named[key] += 1
    return {w: n / total[w] for w, n in named.items() if total[w] >= NAME_LEAST}


def glosses(dictionaries):
    """The be-en pack's glosses of each word, and of its first sense."""
    every, first = defaultdict(list), defaultdict(list)
    with sqlite3.connect(os.path.join(dictionaries, "be-en.sqlite")) as db:
        for word, senses in db.execute("SELECT word_lc, senses FROM entries ORDER BY id"):
            senses = json.loads(senses)
            every[word].extend(g for s in senses for g in s.get("glosses", []))
            if senses and word not in first:
                first[word] = list(senses[0].get("glosses", []))
    return every, first


def english_stem(word):
    for ending in ENGLISH_ENDINGS:
        if word.endswith(ending) and len(word) - len(ending) >= 3:
            return word[:-len(ending)][:6]
    return word[:6]


def same_stem(a, b):
    """Whether two English stems are one word's (explan, explai; divi, divid)."""
    short, long = sorted((a, b), key=len)
    return a == b or len(short) >= 5 and a[:5] == b[:5] or len(short) >= 4 and long.startswith(short)


def overlap(a, b):
    return any(same_stem(x, y) for x in a for y in b)


def covered(a, b):
    """Whether every stem of [a] is one of [b]'s."""
    return all(any(same_stem(x, y) for y in b) for x in a)


def gloss_keys(texts, particles=False):
    """The stems of the English words of glosses that carry their sense ([particles]: with PARTICLES, which tell a prefixed
    verb's sense)."""
    words = ENGLISH.findall(re.sub(r"\([^)]*\)", " ", " ".join(texts).lower()))
    return {english_stem(w) for w in words if (w not in GLOSS_STOP or particles and w in PARTICLES) and len(w) > 1}


def gloss_heads(texts):
    """The first English word of each of the glosses' readings ("passing, going through": passing, going)."""
    heads = []
    for text in texts:
        for part in re.split("[,;]", re.sub(r"\([^)]*\)", " ", text.lower())):
            words = [w for w in ENGLISH.findall(part) if w not in GLOSS_STOP]
            if words:
                heads.append(words[0])
    return heads


def spelling_variants(word):
    """The official spellings a word written in taraškievica may have: official_spellings's, and one letter of TARASKIEVICA_LETTERS
    changed at a time (элемэнт, элемент; рэжысэр, рэжысёр)."""
    found = [official(word)] + official_spellings(word)
    for old, new in TARASKIEVICA_LETTERS:
        start = word.find(old)
        while start >= 0:
            found.append(word[:start] + new + word[start + len(old):])
            start = word.find(old, start + 1)
    return [f for f in dict.fromkeys(found) if f != word]


def reflexive(verb):
    """A verb's reflexive (пачаць, пачацца; несці, несціся), or None."""
    if verb.endswith("ць"):
        return verb[:-2] + "цца"
    if verb.endswith(("ці", "чы")):
        return verb + "ся"
    return None


def unreflexive(verb):
    """A reflexive verb's verb (пачацца, пачаць), or None."""
    if verb.endswith("цца"):
        return verb[:-3] + "ць"
    if verb.endswith(("ціся", "чыся")):
        return verb[:-2]
    return None


def with_vowel_changed(stem, anywhere=False):
    """[stem], and it with its last vowel (or [anywhere] one vowel) turned as a stress shift turns it (узмацніць, узмоцнены;
    аднавіць, адноўлены; горад, гарадок)."""
    found = [stem]
    for i in range(len(stem) - 1, -1, -1):
        if stem[i] in VOWELS:
            found += [stem[:i] + new + stem[i + 1:] for old, new in VOWEL_CHANGES if stem[i] == old]
            if not anywhere:
                break
    return found


def unchanged_stems(stem):
    """The stems a stem whose last consonant may have changed before an ending comes from (абвешч of абвясц, адноўл of аднав)."""
    found = [stem]
    for old, new in STEM_CHANGES:
        if stem.endswith(new) and len(stem) > len(new) + 1:
            found.append(stem[:-len(new)] + old)
    return [v for s in found for v in with_vowel_changed(s)]


def verbs_of_noun(noun):
    """The verbs a verbal noun may name the action of (чаканне of чакаць, тлумачэнне of тлумачыць, узвядзенне of узвесці,
    прыбыццё of прыбыць, выдача of выдаць, ваганне of вагацца), as their infinitives and reflexives' infinitives."""
    found = []
    if noun.endswith(("ццё", "цце")):
        found += [noun[:-3] + "ць", noun[:-3] + "ці"]
    elif noun.endswith("дача"):
        found.append(noun[:-2] + "ць")
    elif noun.endswith("нне") and len(noun) > 5:
        base = noun[:-3]
        if base[-1:] in "ая":
            found.append(base + "ць")
        if base[-1:] in "еэаяё":
            for stem in unchanged_stems(base[:-1]):
                found += [stem + t for t in ("іць", "ыць", "ець", "эць", "аць", "яць")]
                if stem.endswith("н"):
                    found.append(stem + "уць")
                if stem.endswith(("с", "з")):
                    found.append(stem + "ці")
            found += carried(base[:-1])
    return found + [reflexive(v) for v in found if reflexive(v)]


def carried(stem):
    """The verbs in -сці a stem in -д or -дз is of (узвядз of узвесці, знойдз of знайсці, перакладз of перакласці)."""
    if stem.endswith(("д", "дз")):
        return [v + "ці" for v in with_vowel_changed(stem[:-2 if stem.endswith("дз") else -1] + "с")]
    return []


def verbs_of_participle(word):
    """The verbs an adjective in -ны, -ты or -лы may be the participle of (сказаны, узмоцнены, перанесены, знойдзены,
    адкрыты, загінулы, памерлы)."""
    stem, found = word[:-1], []
    if stem.endswith(("ан", "ян")):
        found.append(stem[:-1] + "ць")
    if stem.endswith(("ан", "ян", "ен", "эн", "ён")):
        for base in unchanged_stems(stem[:-2]):
            found += [base + t for t in ("іць", "ыць", "ець", "эць")]
            if base.endswith(("с", "з")):
                found.append(base + "ці")
        found += carried(stem[:-2])
    elif stem.endswith(("т", "л")):
        found += [stem[:-1] + "ць", stem[:-1] + "ці"]
    return found


def bases_of_comparative(word):
    """The adjectives or adverbs a comparative or superlative may be of (важнейшы, пазнейшы, найноўшы, лягчэй)."""
    bare = word[3:] if word.startswith("най") and len(word) > 6 else word
    stems = []
    for tail in ("ейшы", "эйшы", "ейшая"):
        if bare.endswith(tail):
            stems.append(bare[:-len(tail)])
    if bare != word and bare.endswith("шы"):
        stems.append(bare[:-2].replace("ў", "в"))
    if word.endswith(("ей", "эй")):
        stems.append(word[:-2])
    found = []
    for stem in stems:
        for base in unchanged_stems(stem) + ([stem[:-1] + "к"] if stem.endswith("ч") else []):
            for variant in with_vowel_changed(base):
                found += [variant + t for t in ("ы", "і", "а", "кі", "ка")]
    return found


def bases_of_diminutive(word):
    """The nouns a noun may be a diminutive or feminine of (куток of кут, кніжка of кніга, гарадок of горад, паэтка of паэт,
    славянка of славянін, настаўніца of настаўнік)."""
    found = []
    if word.endswith(("ок", "ак", "ык", "ік", "ёк")) and len(word) > 4 and word[-3] not in VOWELS:
        found += [v for s in unchanged_stems(word[:-2]) for v in with_vowel_changed(s, True) if v[-1:] not in VOWELS]
    if word.endswith("ка") and not word.endswith(("іка", "ыка")) and len(word) > 4:
        stems = [word[:-2]] + [word[:-3] + o for n, o in (("ж", "г"), ("ж", "з"), ("ч", "к"), ("ч", "ц"), ("ш", "х"), ("ш", "с"))
                               if word[-3] == n]
        found += [v + t for s in stems for v in with_vowel_changed(s, True) for t in ("", "а", "я", "о", "е", "ін", "ец")]
    if word.endswith("ніца"):
        found.append(word[:-4] + "нік")
    if word.endswith(("іца", "ыца")):
        found.append(word[:-3])
    return found


def verb_aspects(dictionaries):
    """The aspects the be-pl pack tags each verb with."""
    found = defaultdict(set)
    path = os.path.join(dictionaries, "be-pl.sqlite")
    if os.path.exists(path):
        with sqlite3.connect(path) as db:
            for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos = 'verb' ORDER BY id"):
                for sense in json.loads(senses):
                    found[word].update(t for t in sense.get("tags", []) if t in ("perfective", "imperfective"))
    return found


def changed_stems(stem):
    """The stems an imperfective may make of a perfective's stem (змясц, змяшч; аднав, аднаўл; ацан, ацэн; усвядом,
    усведамл)."""
    found = [stem]
    for old, new in STEM_CHANGES:
        if stem.endswith(old):
            found.append(stem[:-len(old)] + new)
    once = [v for s in found for v in with_vowel_changed(s, True)]
    return list(dict.fromkeys(once + [w for v in once for w in with_vowel_changed(v, True)]))


def aspect_partners(verb, verbs):
    """The verbs of [verbs] that may be [verb]'s imperfective by ASPECT_TAILS, the reflexive's of a reflexive."""
    bare = unreflexive(verb) or verb
    found = set()
    for perfective, imperfective in ASPECT_TAILS:
        if not bare.endswith(perfective) or len(bare) - len(perfective) < (1 if len(perfective) > 4 else 2):
            continue
        for stem in changed_stems(bare[:-len(perfective)]):
            partner = stem + imperfective
            if bare != verb:
                partner = reflexive(partner)
            if partner and partner != verb and partner in verbs:
                found.add(partner)
    return found


def distinct_pair(perfective, imperfective):
    """Whether two verbs' endings make an aspect pair that only aspect partners show (not іць, аць alone: сказіць, сказаць)."""
    a, b = unreflexive(perfective) or perfective, unreflexive(imperfective) or imperfective
    stem = os.path.commonprefix([a, b])
    return len(b) - len(stem) >= 5 or a[len(stem):] not in ("іць", "ыць", "ець", "эць", "аць", "яць")


# How each word went to another in fold, for the list's pinning: word -> (the word it went to, why).
MERGED = {}


def fold(scores, forms, pos, lemmas, dictionaries, corpora):
    """
    After the counting: a form's capitalised uses inside a sentence are a name's and count for no word, and a hyphenated
    word, which the reader reads as its pieces, is left out.
    A word the pack glosses as another's spelling or form, or written in taraškievica, is that word's.
    A participle, verbal noun, comparative or diminutive goes to its word.
    Of aspect, reflexive and gender partners, the one counted less goes to the other.
    """
    MERGED.clear()
    shares = name_shares(dictionaries, corpora)
    for word in sorted(scores):
        counted = forms[word]
        for form in sorted(counted):
            counted[form] *= 1 - shares.get(form, 0.0)
        scores[word] = sum(counted.values())
    for word in sorted(w for w in scores if "-" in w or scores[w] <= 0 or w in NOT_WORDS):
        del scores[word]
    every, first = glosses(dictionaries)
    keys = {w: gloss_keys(g) for w, g in every.items()}
    order = {w: i for i, w in enumerate(sorted(scores, key=lambda w: (-scores[w], w)))}
    nouns = {w for w in scores if "noun" in pos.get(w, ())}

    def alike(a, b):
        """Whether two words' glosses share a word, or either has none."""
        return not keys.get(a) or not keys.get(b) or overlap(keys[a], keys[b])

    def glossed_alike(a, b):
        """Whether two words have glosses that share a word."""
        return overlap(keys.get(a, ()), keys.get(b, ()))

    def heads_of(word):
        return gloss_heads(first.get(word, ()))

    parent, kind = {}, {}

    def derive(word, base, why):
        if word not in parent and base != word and base in scores:
            parent[word], kind[word] = base, why

    # Spellings and forms the glosses name, by hand, and in taraškievica.
    for word in sorted(scores):
        for text in every.get(word, ()):
            for match in SPELLING_OF.finditer(text):
                if any(k in match.group(0) for k in SPELLING_KINDS):
                    derive(word, match.group(1), "spelling")
        for match in FORM_OF.finditer(" ".join(first.get(word, ()))):
            target = match.group(1)
            if target in scores:
                derive(word, target, "form")
            elif word not in parent and not lemmas.get(target) and target not in pos:
                # A word the list lacks is the headword of its forms (кольца, кальцо).
                parent[word], kind[word] = target, "rename"
        if word in VARIANTS:
            if VARIANTS[word] in scores:
                derive(word, VARIANTS[word], "spelling")
            elif word not in parent:
                parent[word], kind[word] = VARIANTS[word], "rename"
        # A verbal noun's genitive (напісання) is the noun's.
        if word.endswith("ння") and "noun" in pos.get(word, ()) and word not in parent and (
                word not in every or word[:-1] + "е" in " ".join(every[word])):
            if word[:-1] + "е" in scores:
                derive(word, word[:-1] + "е", "form")
            elif not lemmas.get(word[:-1] + "е"):
                parent[word], kind[word] = word[:-1] + "е", "rename"
        for spelled in spelling_variants(word):
            if spelled in scores and (glossed_alike(word, spelled) or word not in every) and \
                    pos.get(word, set()) & pos.get(spelled, set()):
                derive(word, spelled, "spelling")
                break

    verbs = {w for w in scores if "verb" in pos.get(w, ()) and w.endswith(VERB_TAILS)}
    adjectives = {w for w in scores if pos.get(w, set()) & {"adj", "adv"}}

    def as_verbs(found):
        """The verbs of the list that [found]'s infinitives are, or are the forms of (апрануць of апранаць)."""
        return [w for f in found for w in ([f] if f in verbs else [l for l in lemmas.get(f, ()) if l in verbs])]

    def best(found, word, need):
        """The commonest word of [found] that is in [need]."""
        found = [f for f in dict.fromkeys(found) if f in need and f != word]
        return min(found, key=order.get) if found else None

    def stem_in(head, word):
        return overlap({english_stem(head)}, keys.get(word, ()))

    def plain_action(noun, verb):
        """Whether a noun's glosses name only the verb's action (a noun without glosses is taken to), and with a verb
        without glosses, whether they are all action nouns."""
        heads = heads_of(noun)
        if not keys.get(noun):
            return True
        if not keys.get(verb):
            return all(h.endswith(ACTION_NOUNS) for h in heads)
        return glossed_alike(noun, verb) and all(h.endswith(ACTION_NOUNS) or stem_in(h, verb) for h in heads)

    def own_first(found):
        """Verbs before reflexives (далучэнне of далучыць, not of далучыцца)."""
        return [f for f in found if not unreflexive(f)] + [f for f in found if unreflexive(f)]

    for word in sorted(scores):
        genitive = word.endswith("ння") and kind.get(word) == "rename"
        if word in parent and not genitive:
            continue
        if word in nouns and word.endswith(("нне", "ццё", "цце", "дача")) or genitive:
            # A verbal noun's genitive filed as a word (напісання) is the noun's, and so its verb's.
            verb = best(as_verbs(own_first(verbs_of_noun(parent[word] if genitive else word))), word, verbs)
            if verb and plain_action(word, verb):
                parent[word], kind[word] = verb, "verbal noun"
            continue
        if word in adjectives and word.endswith(("ны", "ты", "лы")) and word not in verbs:
            found = own_first(verbs_of_participle(word))
            # A participle without glosses goes to the commonest verb it may be of (сустрэты, аточаны, прапушчаны).
            verb = best(as_verbs(f for f in found if glossed_alike(word, f) or not keys.get(word)), word, verbs)
            heads = heads_of(word)
            # Its first reading is a participle's (узмоцнены "reinforced", запоўнены "filled, full").
            if verb and (heads[0].endswith("ed") or heads[0] in PARTICIPLES or stem_in(heads[0], verb) if heads
                         else word.endswith(("аны", "яны", "ены", "ёны", "эны", "ыты", "уты", "яты"))):
                derive(word, verb, "participle")
        if word in adjectives and (word.endswith(("ейшы", "эйшы", "шы")) and word.startswith("най") or word.endswith(
                ("ейшы", "эйшы", "ей", "эй"))):
            found = bases_of_comparative(word)
            same = {w for w in adjectives if pos[w] & pos[word] & {"adj", "adv"}}
            base = best(found, word, same) or best(found, word, adjectives)
            heads = heads_of(word)
            # One without glosses, when it is one in -ейшы (навейшы, слабейшы, карацейшы).
            if base and (any(h in COMPARATIVE or h.endswith(("er", "est")) for h in heads) if heads else
                         word.endswith(("ейшы", "эйшы"))):
                derive(word, base, "comparative")

    # A noun without glosses that the corpus made of a form (кішэчніка, правіянта, ільгот, жылак, відэакамер, палаткі) is
    # its nominative's: a masculine's genitive whose stem is among its forms, at least half as common, with no feminine's
    # instrumental; a genitive plural with a feminine's instrumental among its forms; a plural of a listed noun.
    def feminine(stem, counted):
        return any(stem + e in counted for e in ("ай", "аю", "яй", "яю", "ой", "ою"))

    for word in sorted(nouns):
        if word in parent or word in every or pos.get(word) != {"noun"}:
            continue
        counted = forms.get(word, {})
        stem = word[:-1]
        target = None
        if word[-1:] in "ая" and counted.get(stem, 0) >= 0.5 * counted.get(word, 0) > 0 and not feminine(stem, counted):
            target = stem
        elif word.endswith("ца") and counted.get(word[:-2] + "ец", 0) >= 0.5 * counted.get(word, 0) > 0 and not feminine(
                stem, counted):
            # A noun in -ец (мудраца, мудрэц; галандца, галандзец).
            target = word[:-2] + "ец"
        elif word[-1:] not in VOWELS and word[-2:-1] in "аое" and word[-3:-2] in "кц" and feminine(word[:-2] + "к", counted):
            target = word[:-2] + "ка"
        elif word[-1:] not in VOWELS and feminine(word, counted):
            target = word + "а"
        elif word[-1:] in "ыі" and stem + "а" in scores:
            target = stem + "а"
        if not target or target == word:
            continue
        lemma = next((l for l in lemmas.get(target, ()) if l in scores and l != word), None)
        if target in scores:
            derive(word, target, "form")
        elif lemma:
            derive(word, lemma, "form")
        else:
            parent[word], kind[word] = target, "rename"

    # The partners: of two, the one counted less goes to the other.
    aspects = verb_aspects(dictionaries)
    first_keys = {w: gloss_keys(g) for w, g in first.items()}
    partners = defaultdict(set)

    def pair(a, b, why):
        if a != b and a in scores and b in scores:
            partners[a].add((b, why))
            partners[b].add((a, why))

    for verb in sorted(verbs):
        for partner in sorted(aspect_partners(verb, verbs)):
            # Without glosses, only by a change of ending no other verb pair shares (змясціць, змяшчаць).
            if glossed_alike(verb, partner) or (not keys.get(verb) or not keys.get(partner)) and distinct_pair(verb, partner):
                pair(verb, partner, "aspect")
        for prefix in EMPTY_PREFIXES + LOAN_PREFIXES:
            bare = verb[len(prefix):]
            if not verb.startswith(prefix) or bare not in verbs or not first_keys.get(verb) or not keys.get(bare):
                continue
            if (unreflexive(bare) or bare).endswith(SECONDARY + ("нуць",)) or aspects.get(bare) == {"perfective"} or \
                    aspects.get(verb) == {"imperfective"}:
                continue
            # Its glosses add nothing to the verb's first sense (зрабіць, рабіць "to do, to make"; not пахаваць "to bury",
            # хаваць "to hide; to bury").
            mine, its = gloss_keys(first.get(verb, ()), True), gloss_keys(first.get(bare, ()), True)
            if covered(mine, its) and (prefix not in LOAN_PREFIXES or covered(its, mine)):
                pair(verb, bare, "prefix")
        # A verb in -іраваць beside one in -аваць is its other spelling (фарміраваць, фармаваць).
        bare = unreflexive(verb) or verb
        if bare.endswith("іраваць") and len(bare) > 9:
            spelled = bare[:-7] + "аваць" if bare == verb else reflexive(bare[:-7] + "аваць")
            if spelled in verbs and alike(verb, spelled):
                pair(verb, spelled, "variant")
        # A reflexive is its verb's partner when its first sense is a passive or shares a word with the verb's glosses
        # (пачацца, пачаць "to begin"), and a word of its own otherwise (займацца "to study", займаць "to occupy").
        base = unreflexive(verb)
        if base and as_verbs([base]):
            text = " ".join(first.get(verb, ()))
            if verb not in first or not keys.get(base) or text.startswith("to be ") or overlap(first_keys[verb], keys[base]):
                pair(verb, as_verbs([base])[0], "reflexive")
    for word in sorted(nouns):
        if word in parent:
            continue
        base = best(bases_of_diminutive(word), word, nouns)
        if not base:
            continue
        heads = [h for h in heads_of(word) if h not in DIMINUTIVE]
        # The first reading names what the other word does (куток "corner", кут "corner"; not мужык "peasant", муж).
        if not heads or not stem_in(heads[0], base):
            continue
        heads = set(heads_of(word))
        if heads & {"woman", "female", "girl"} or any(h.endswith("ess") for h in heads):
            pair(word, base, "feminine")
        else:
            derive(word, base, "diminutive")
    # Partners of partners are one word too (падняць, падымаць, падняцца, падымацца), the commonest of them its headword.
    group = {}

    def leader(word):
        while group.get(word, word) != word:
            word = group[word]
        return word

    for word in sorted(partners):
        for other, _ in sorted(partners[word]):
            if word not in parent and other not in parent:
                a, b = sorted((leader(word), leader(other)), key=order.get)
                if a != b:
                    group[b] = a
    for word in sorted(partners):
        top = leader(word)
        if word not in parent and top != word:
            parent[word] = top
            kind[word] = min((order[b], why) for b, why in partners[word])[1]

    def root(word):
        seen = [word]
        while seen[-1] in parent and parent[seen[-1]] not in seen:
            seen.append(parent[seen[-1]])
        return seen[-1]

    for word in sorted(parent, key=lambda w: (-order.get(w, 0), w)):
        if word not in scores:
            continue
        into = root(word)
        if into == word:
            continue
        if into not in scores:
            pos[into] = pos.get(into, set()) | pos.get(word, set())
            forms[into] = {}
        for form, credit in sorted(forms.pop(word).items()):
            forms[into][form] = forms[into].get(form, 0) + credit
        scores[into] = sum(forms[into].values())
        del scores[word]
        MERGED[word] = (into, kind[word])
