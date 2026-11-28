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


# How each word went to another in fold, for the list's pinning: word -> (the word it went to, why).
MERGED = {}


def fold(scores, forms, pos, lemmas, dictionaries, corpora):
    """
    After the counting: a form's capitalised uses inside a sentence are a name's and count for no word, and a hyphenated
    word, which the reader reads as its pieces, is left out.
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
