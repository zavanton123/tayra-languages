"""
Uzbek (Latin script) for the dictionary pack, the frequency list and the course checker.

Uzbek writes oʻ and gʻ with a turned comma (U+02BB) and the tutuq belgisi (ʼ, U+02BC) between letters (maʼno, sanʼat), but
texts type either as ' ` ´ ‘ or ’ as often as not. normalise writes each such mark as the letter it stands for, as the
reader does (UzbekParser.normalized): after o or g and before a letter, the turned comma; after a g ending a word, but for the
ng of keling, the turned comma too (bogʻ); between two other letters, the tutuq belgisi. Any other mark is a quotation mark.
"""
import re

TURNED_COMMA = "ʻ"
TUTUQ = "ʼ"
MARKS = "'`´‘’" + TURNED_COMMA + TUTUQ
# A word as the reader reads it: its word characters (a-z, the Latin-1 and Latin Extended letters up to ȳ, and the two marks).
WORD = re.compile("[a-zA-ZÀ-ÖØ-öø-ȳ" + TURNED_COMMA + TUTUQ + "]+")


def normalise(text):
    """[text] with each apostrophe-like mark of oʻ, gʻ and the tutuq belgisi written as that letter; the length is kept."""
    if not any(c in MARKS for c in text):
        return text
    out = list(text)
    for i in range(1, len(text)):
        if text[i] not in MARKS:
            continue
        before = text[i - 1]
        after_letter = i + 1 < len(text) and text[i + 1].isalpha()
        two_before = text[i - 2] if i >= 2 else None
        if before in "oOgG" and after_letter:
            out[i] = TURNED_COMMA
        elif before in "gG" and two_before is not None and two_before.isalpha() and two_before not in "nN":
            out[i] = TURNED_COMMA
        elif before.isalpha() and after_letter and before not in "oOgG":
            out[i] = TUTUQ
    return "".join(out)


def words(text):
    """The words of [text] as the reader cuts them, normalised."""
    return WORD.findall(normalise(text))


# The corpora counted, as build_frequency_list downloads them: Leipzig's news (2011 and 2020) and Wikipedia sentences, and
# CC-100's web text (Common Crawl pages of 2018), each line once.
LEIPZIG_NEWS = ["uzb_newscrawl_2011_100K", "uzb_news_2020_30K"]
LEIPZIG_WIKIPEDIA = ["uzb_wikipedia_2021_100K"]
CC100 = "https://data.statmt.org/cc-100/uz.txt.xz"
# The CC-100 lines read: its first two million different ones (of about 7 million, some 25 million words).
CC100_LINES = 2_000_000
_CYRILLIC = re.compile("[Ѐ-ӿ]")


def count_lines(lines):
    """
    The words of [lines] as the reader reads them, lowercased, with their counts. A line written in Cyrillic (Uzbek's other
    alphabet) is left out; so is a word written only with a capital inside a line (Toshkent, Karimov), which is a name.
    """
    words, capital, plain = {}, set(), set()
    for line in lines:
        if _CYRILLIC.search(line):
            continue
        for i, word in enumerate(WORD.findall(normalise(line))):
            key = word.lower()
            words[key] = words.get(key, 0) + 1
            if word[0] != key[0]:
                if i:
                    capital.add(key)
            else:
                plain.add(key)
    return {w: n for w, n in words.items() if not (w in capital and w not in plain)}


def leipzig_sentences(path):
    """A Leipzig corpus's sentences, each once."""
    import io
    import tarfile
    seen = set()
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            sentence = line.rstrip("\n").partition("\t")[2]
            if sentence not in seen:
                seen.add(sentence)
                yield sentence


def cc100_lines(path, limit=CC100_LINES):
    """CC-100's Uzbek lines, each once, up to [limit] of them."""
    import lzma
    seen = set()
    with lzma.open(path, "rt", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line in seen:
                continue
            seen.add(line)
            yield line
            if len(seen) >= limit:
                break


# The regular forms (paradigms). The English pack's Uzbek tables are made by a template that knows neither the voicing of a
# last k or q before a vowel (yurak, yuragi; qishloq, qishlogʻi), nor the vowel a few nouns drop (oʻgʻil, oʻgʻli; burun,
# burni), nor any form of a verb but its tenses; and of its 4,000 words only 200 are verbs. So, as the Kazakh list is, the
# Uzbek one is grouped by the regular forms of every word of the pack and of Apertium's Uzbek lexicon, which has 33,000 words
# with their kinds (APERTIUM): Uzbek has no vowel harmony, and each ending has one shape, or two after a vowel.

_VOWELS = set("aeiou")


def ends_in_vowel(word):
    return word[-1:] in _VOWELS or word.endswith("o" + TURNED_COMMA)


def _syllables(word):
    return sum(1 for i, c in enumerate(word) if c in _VOWELS)


def dative(stem, ending="ga"):
    """An ending in g (-ga, -gacha, -gan, -guncha) after a stem: k after k, q after q (yurakka, qishloqqa, ekkan, chiqqan)."""
    if stem.endswith("k"):
        return stem + "k" + ending[1:]
    if stem.endswith("q"):
        return stem + "q" + ending[1:]
    return stem + ending


def vowel_stems(word):
    """
    The stems a vowel ending joins: the word, with a last k or q voiced in a word of more than one syllable (yuragi,
    qishlogʻi), and short of the vowel of its last syllable where a few nouns drop it (oʻgʻli, burni, shahri, singlisi).
    """
    stems = [word]
    if _syllables(word) > 1 and word[-1] in "kq":
        stems.append(word[:-1] + ("g" if word[-1] == "k" else "g" + TURNED_COMMA))
    if word in ELIDING:
        stems.append(word[:-2] + word[-1])
    return stems


# The nouns that drop the vowel of their last syllable before a vowel ending (oʻgʻil, oʻgʻli; oʻrin, oʻrni).
ELIDING = {"oʻgʻil", "burun", "ogʻiz", "qorin", "boʻyin", "koʻngil", "singil", "shahar", "oʻrin", "bagʻir", "egin", "qayin"}


CASES = ("ning", "ni", "ga", "da", "dan", "dek", "day", "dagi", "gacha", "niki")
PREDICATIVES = ("man", "san", "miz", "siz", "dir", "lar")


def cases(stem):
    return [dative(stem, c) if c[0] == "g" else stem + c for c in CASES]


def possessives(word):
    """A noun's possessive forms (otam, otasi; kitobim, kitobi; yuragim, oʻgʻlim), without the plural's."""
    if ends_in_vowel(word):
        return [word + e for e in ("m", "ng", "si", "miz", "ngiz")]
    return [v + e for v in vowel_stems(word) for e in ("im", "ing", "i", "imiz", "ingiz")]


def noun_forms(word, plural=True):
    """A noun's (adjective's, number's) regular forms: plural, possessives, their cases, and the predicative endings."""
    out = set()
    bases = [word] + ([word + "lar"] if plural else [])
    for base in bases:
        out.add(base)
        out.update(cases(base))
        out.update(base + e for e in PREDICATIVES)
        out.add(base + "da" + "gi")
        for p in possessives(base) + [base + "lari"] if base == word else [base + e for e in ("im", "ing", "i", "imiz", "ingiz")]:
            out.add(p)
            out.update(cases(p))
            out.update(p + e for e in ("man", "san", "miz", "siz", "dir"))
    # Its -lik (yaxshilik), -li (suvli), -siz (suvsiz) and -cha (oʻzbekcha) words, where the lexicon has none of their own.
    for made in (word + "lik", word + "li", word + "siz", word + "cha", word + "chilik"):
        out.add(made)
    lik = word + "lik"
    out.update(cases(lik) + [lik + "lar", word + "ligi", word + "ligini", word + "ligiga", word + "ligida", word + "ligidan",
                             word + "ligining", word + "liklari"])
    out.discard(word)
    return out


def verb_forms(verb):
    """A verb's regular forms (cited in -moq: bormoq, oʻqimoq, ishlamoq), negated too, with its participles' and verbal noun's."""
    if not verb.endswith("moq") or len(verb) < 5:
        return set()
    stem = verb[:-3]
    out = set()
    out.update(_verb_forms(stem, ends_in_vowel(stem)))
    out.update(_verb_forms(stem + "ma", True, negative=True))
    out.discard(verb)
    return out


def _verb_forms(s, vowel, negative=False):
    out = set()
    persons = ("man", "san", "di", "miz", "siz", "dilar")
    # The infinitive with -chi (intention) and -da (now: bormoqda), and their predicative endings.
    for inf in (s + "moqchi", s + "moqda"):
        out.add(inf)
        out.update(inf + e for e in PREDICATIVES)
        out.update(inf + e for e in ("ydi", "ligi", "ligini"))
    # The present-future: boraman, boradi; oʻqiyman; bormayman. Its stem is the converb (bora, oʻqiy, bormay).
    present = s + ("y" if vowel else "a")
    out.add(present)
    out.update(present + e for e in persons)
    # The past: bordim, bording, bordi, bordik, bordingiz, bordilar.
    out.update(s + "di" + e for e in ("m", "ng", "", "k", "ngiz", "lar", "ngizmi", "mi"))
    # The progressive: boryapman, boryapti; oʻqiyapti; and the present participle: borayotgan, oʻqiyotgan.
    out.update(s + "yap" + e for e in ("man", "san", "ti", "miz", "siz", "tilar", "timi", "sizmi"))
    ongoing = present + "yotgan" if not vowel else s + "yotgan"
    for p in (ongoing,):
        out.add(p)
        out.update(noun_forms(p))
    out.update((present if not vowel else s) + "yot" + e for e in ("ib", "ir", "ibdi", "irman", "ganda"))
    # The past participle (borgan, chiqqan, ekkan, oʻqigan, bormagan) with its cases, possessives and endings, and -gancha.
    past = dative(s, "gan")
    out.add(past)
    out.update(noun_forms(past))
    out.update({past + "cha", past + "day", past + "dek"})
    # The future participle: boradigan, oʻqiydigan, bormaydigan.
    future = present + "digan"
    out.add(future)
    out.update(noun_forms(future))
    # The aorist: borar, oʻqir; negated bormas; with the endings (borarman, bormasman).
    aorist = s + "mas" if negative else s + ("r" if vowel else "ar")
    out.add(aorist)
    out.update(aorist + e for e in PREDICATIVES + ("di", "kan", "di"))
    if negative:
        out.update({s + "sdan", s + "slik", s + "sligi", s + "sligini", s + "sligiga", s + "slikka", s + "slikni", s + "y", s + "ydi"})
    # The converb in -ib and the evidential past: borib, oʻqib, boribdi, boribman; and of a negated verb, bormabdi.
    converb = s + ("b" if vowel else "ib")
    out.add(converb)
    out.update(converb + e for e in persons + ("dir", "di", "man", "san", "miz", "siz", "dilar"))
    # The converbs -guncha, -gach, -gani (until, after, in order to), -gudek; and the agent -guvchi.
    for e in ("guncha", "gach", "gani", "gudek", "guday", "guvchi", "gulik", "gi", "gim", "ging", "gisi", "gimiz", "gingiz"):
        out.add(dative(s, e))
    # The conditional: borsam, borsa; and with -chi (borsachi).
    out.update(s + "sa" + e for e in ("m", "ng", "", "k", "ngiz", "lar", "chi", "ydi", "ydim", "m-u"))
    # The imperatives: bor, boring, boringiz, borgin, borsin, boray, boraylik; oʻqing, oʻqisin, oʻqiy, oʻqiylik; and with -chi.
    out.add(s)
    polite = s + ("ng" if vowel else "ing")
    out.update({polite, polite + "iz", polite + "lar", polite + "chi", polite + "izchi", s + "chi", dative(s, "gin"), s + "sin",
                s + "sinlar", present + ("" if vowel else "y"), present + ("lik" if vowel else "ylik"),
                present + ("in" if vowel else "yin")})
    # The verbal noun: borish, oʻqish, with its cases and possessives (borishga, borishi, borishini); negated, bormaslik.
    if not negative:
        noun = s + ("sh" if vowel else "ish")
        out.add(noun)
        out.update(noun_forms(noun))
        # The agent in -uvchi (boruvchi; oʻqi + uvchi is oʻquvchi, ishla + vchi ishlovchi), with its forms.
        agent = s[:-1] + "uvchi" if s.endswith("i") else s[:-1] + "ovchi" if s.endswith("a") else s + ("vchi" if vowel else "uvchi")
        out.add(agent)
        out.update(noun_forms(agent))
    return out


def derived_verbs(verb):
    """The passive, reflexive, reciprocal and causative verbs regularly made from a verb (oʻqilmoq, yozilmoq, koʻrishmoq, oʻqitmoq)."""
    if not verb.endswith("moq") or len(verb) < 5:
        return set()
    s = verb[:-3]
    if ends_in_vowel(s):
        made = [s + "l", s + "n", s + "sh", s + "t", s + "tir", s + "ldi"[:-2] + "in"]
    else:
        made = [s + ("in" if s.endswith("l") else "il"), s + "in", s + "ish", s + "tir", s + "dir", s + "giz", s + "ir", s + "iz"]
    made += [m + "il" for m in made if m.endswith(("tir", "dir", "t", "giz", "iz", "ir"))]
    return {m + "moq" for m in made if m + "moq" != verb}


# The copula's forms, of its stem e- (emoq): was, were; it is said.
COPULA = "edi edim eding edik edingiz edilar ediyu ediku edimi emish emishlar emishman emishsan".split()


def passive_of(verb, pos):
    """The verb [verb] (cited in -moq) is the passive or reflexive of, made with -il, -in, -l or -n, if a verb of [pos], else None."""
    s = verb[:-3]
    for endings, base, after_vowel in ((("il", "in"), s[:-2], False), (("l", "n"), s[:-1], True), (("yil", "yin"), s[:-3], True)):
        if s.endswith(endings) and len(base) > 1 and "verb" in pos.get(base + "moq", ()) and ends_in_vowel(base) == after_vowel:
            return base + "moq"
    return None


# Spelling and the little words.
CLITICS = ("mi", "chi", "ku", "gina", "da", "ham")


def with_clitics(form):
    """A form with the question -mi, -chi, -ku, -gina (only) and -u/-yu (and) written onto it; none onto a letter alone (u)."""
    if len(form) < 2:
        return set()
    out = {form + c for c in CLITICS[:4]}
    out.add(form + ("yu" if ends_in_vowel(form) else "u"))
    return out


# The counter ta (beshta, 5 ta) and its cases, which take no other endings.
PRONOUNS_TA = "tasi tadan taga tani tasida tasini tasiga tasidan tasining tacha tagina"
# The personal and demonstrative pronouns' cases, which the pack's tables make as a noun's (menning, uga).
PRONOUNS = {
    "men": "mening menga meni menda mendan mendek meniki menimcha menman",
    "sen": "sening senga seni senda sendan sendek seniki sencha",
    "u": "uning unga uni unda undan undek uniki",
    "biz": "bizning bizga bizni bizda bizdan bizdek bizniki bizlar bizlarning bizlarga bizlarni bizlarda bizlardan bizningcha",
    "siz": "sizning sizga sizni sizda sizdan sizdek sizniki sizlar sizlarning sizlarga sizlarni sizlarda sizlardan sizningcha",
    "ular": "ularning ularga ularni ularda ulardan ulardek",
    "bu": "buning bunga buni bunda bundan bular bularning bularga bularni bularda bulardan bundek",
    "shu": "shuning shunga shuni shunda shundan shular shularning shularga shularni shularda shulardan shundek",
    "oʻsha": "oʻshaning oʻshanga oʻshani oʻshanda oʻshandan oʻshalar oʻshalarning oʻshalarga oʻshalarni",
    "kim": "kimning kimga kimni kimda kimdan kimlar kimlarning kimlarga kimlarni kimsan kimman kimsiz",
    "nima": "nimaning nimaga nimani nimada nimadan nimalar nimalarni nimasi nimasini nimang nimam",
    "ta": PRONOUNS_TA,
    "oʻz": "oʻzim oʻzing oʻzi oʻzimiz oʻzingiz oʻzlari oʻzimni oʻzingni oʻzini oʻzimizni oʻzingizni oʻzlarini oʻzimga "
           "oʻzingga oʻziga oʻzimizga oʻzingizga oʻzlariga oʻzimda oʻzida oʻzimizda oʻzingizda oʻzlarida oʻzimdan oʻzingdan "
           "oʻzidan oʻzimizdan oʻzingizdan oʻzlaridan oʻzimning oʻzingning oʻzining oʻzimizning oʻzingizning oʻzlarining "
           "oʻzlar oʻzicha oʻzingizni",
}


# Apertium's Uzbek lexicon (GPL-3.0), at a fixed commit: its words, each with the kind its continuation class gives. The English
# pack has about 3,700 Uzbek words; many of the commonest (ham, ushbu, orqali, asosiy, qarshi) are not among them.
APERTIUM = "https://raw.githubusercontent.com/apertium/apertium-uzb/1bdee4a38c357226176d516ae533c43f8ecb045b/apertium-uzb.uzb.lexc"
APERTIUM_KINDS = [
    (re.compile(r"^(?:V|VERB)-"), "verb"), (re.compile(r"^N1(?:-NAT|-PL)?$"), "noun"), (re.compile(r"^A[0-9]"), "adj"),
    (re.compile(r"^ADV"), "adv"), (re.compile(r"^PAREN$"), "adv"), (re.compile(r"^IJ$"), "intj"), (re.compile(r"^C[ACS]"), "conj"),
    (re.compile(r"^POST"), "prep"), (re.compile(r"^PRON"), "pron"), (re.compile(r"^DET"), "det"), (re.compile(r"^NUM$"), "num"),
    (re.compile(r"^(?:QST|MOD)"), "particle"),
]
_ENTRY = re.compile(r"^([^\s:!%<>]+):(\S+)\s+(\S+)\s*;")


def apertium_words(path):
    """
    The lexicon's words in its Common lexicon as the list writes them (a verb with -moq), each with its kinds. Its verbal nouns
    (oshirish, foydalanish: N1-VN) are left to their verbs, as the pack's are. Most of its entries were filed under a class by
    a classifier, whose confidence their comment gives; a verb only so filed, of a stem that is another kind of word too
    (uchun-, til-, et- "meat"), is often a made-up one (DOUBTFUL_VERBS), which load keeps only where texts use its forms (etdi,
    etgan: etmoq "to do"); a verb glossed by hand (qozon- "to win") is kept.
    """
    words, guessed_verbs, hand_verbs, others, mistakes = {}, set(), set(), set(), set()
    with open(path, encoding="utf-8") as f:
        common = False
        for line in f:
            if line.startswith("LEXICON "):
                common = line.split()[1] == "Common"
                continue
            match = common and _ENTRY.match(line.strip())
            if not match:
                continue
            lemma, cls = normalise(match.group(1)), match.group(3)
            # A misspelling or a piece of a word (mas, lari: MIST) is no word, under whatever other class it is filed too.
            if cls == "MIST":
                mistakes.add(lemma)
                continue
            kind = next((k for pattern, k in APERTIUM_KINDS if pattern.search(cls)), None)
            if kind is None or lemma != lemma.lower() or not WORD.fullmatch(lemma) or len(lemma) < 2:
                continue
            words.setdefault(lemma + "moq" if kind == "verb" else lemma, set()).add(kind)
            if kind == "verb":
                (guessed_verbs if _GUESSED.search(line) else hand_verbs).add(lemma)
            else:
                others.add(lemma)
    for stem in guessed_verbs - hand_verbs:
        GUESSED_VERBS.add(stem + "moq")
        if stem in others:
            DOUBTFUL_VERBS.add(stem + "moq")
    for word in MISFILED | mistakes:
        words.pop(word, None)
    return words


# The lexicon's verbs glossed by hand that are misspelled (organ- "to learn" for oʻrgan-, togʻ- "to give birth" for tugʻ-) or
# no verb (kerak- "to require" for kerak).
MISFILED = {"organmoq", "togʻmoq", "kerakmoq"}


# The verbs only the lexicon's classifier filed, and those of them of a stem that is another kind of word too.
GUESSED_VERBS = set()
DOUBTFUL_VERBS = set()
# How common the plain forms of such a verb must be together (its past, perfect and present third persons, its converb in -ib)
# for it to be kept: about 50 uses in the 47 million words counted.
DOUBTFUL_SHARE = 1e-6


def finite_forms(verb):
    """A verb's commonest forms that no noun has: bordi, borgan, boradi, borib, bormoqda, boryapti (of bormoq)."""
    s = verb[:-3]
    vowel = ends_in_vowel(s)
    return {s + "di", dative(s, "gan"), s + ("ydi" if vowel else "adi"), s + ("b" if vowel else "ib"), s + "moqda", s + "yapti"}


# A classifier's confidence, as the comment of the entries it filed gives it.
_GUESSED = re.compile(r";\s*!\s*[01]\.\d+\s*$")


# The words of two letters the lexicon has that are words: the counter ta (beshta, 5 ta) and yo "or".
SHORT = {"ta", "yo"}
# Seen forms: when set (the builder sets it to the corpus's forms before load), only these of the generated forms are kept.
SEEN = None
# The words the Apertium lexicon adds, which the pack has no entry for.
LEXICON_ONLY = set()
_NOMINAL = {"noun", "adj", "num"}
# Words the pack has as another kind that take the noun endings too (hamma, hammasi; koʻp, koʻpi; bor, bori).
NOMINAL_ALSO = {"hamma", "barcha", "koʻp", "bor", "yoʻq", "kerak", "har", "ayrim", "baʼzi", "boshqa", "biri", "bari"}
# Spellings that are a form of a common word, though the pack or the lexicon has them as a rarer word of their own.
OVERRIDES = {"qil": ["qilmoq"]}
# bora "regard" (borasida "as regards"), no form of bormoq for being spelled as its converb.
OVERRIDES.update({form: ["bora"] for form in "bora borasida borada borasidagi boradagi".split()})
OVERRIDES.update({form: ["bormoq"] for form in "boraman borasan boramiz borasiz boramanmi borasanmi boramizmi borasizmi".split()})
# Forms of a pronoun spelled as words of their own, which keep them (unda "then", shunda "then", bunda "here").
OWN = set()


def regular_forms(word, kinds):
    out = set()
    if "verb" in kinds and word.endswith("moq"):
        out |= verb_forms(word)
    # A little word the lexicon also has as a noun (ham "also") takes no noun endings.
    if kinds & _NOMINAL and not kinds & {"conj", "prep", "particle"} or word in NOMINAL_ALSO:
        out |= noun_forms(word)
    if kinds & {"adj", "adv"}:
        out.update({word + "roq", word + "roqdir", word + "roqqa", word + "gina"})
    if kinds & {"adv", "prep"}:
        out.add(word + "gi")
    return out


def _mass(forms):
    seen = SEEN if isinstance(SEEN, dict) else {}
    return sum(seen.get(f, 0) for f in forms)


_OWN_KINDS = _NOMINAL | {"verb", "adv", "pron", "prep", "conj", "det", "intj", "particle"}


# How common a word must be, against a word only the lexicon has that is its regular form, to take that word as its form: a
# fifth as common takes instituti (institut), oʻgʻli (oʻgʻil), oshirish (oshirmoq), but not dollar (dol "gate"), kurash
# "struggle" (kuramoq "to shovel"), yoʻnalish "direction" (yoʻnalmoq).
FOLD_SHARE = 0.2


def derived_words(word, kinds):
    """The words made from [word] that are no inflection of it: its -lik, -li, -siz, -cha words, comparatives and -gi words,
    and of a verb its stem (the imperative), its converb, aorist and agent (bor, borma, bora, borar, boruvchi)."""
    out = {word + e for e in ("lik", "li", "siz", "cha", "chilik", "roq", "gi", "gina")}
    out |= {f for f in noun_forms(word + "lik")}
    if "verb" in kinds and word.endswith("moq"):
        s = word[:-3]
        out |= {s, s + "ma", s + "a", s + "y", s + "may", s + "ar", s + "r", s + "mas", s + "chi"}
        agent = s[:-1] + "uvchi" if s.endswith("i") else s[:-1] + "ovchi" if s.endswith("a") else s + ("vchi" if ends_in_vowel(s) else "uvchi")
        out |= {agent} | noun_forms(agent)
    return out


def load(db, pos, lemmas, common, lexicon_path):
    """Adds the lexicon's words and the regular forms above to the English pack's [pos], [lemmas] and [common], as
    load_dictionary has them. [SEEN], the counts, picks the generated forms kept and settles which words are folded."""
    seen = SEEN
    for word, kinds in apertium_words(lexicon_path).items():
        if word in common or word in pos and pos[word] & _OWN_KINDS:
            continue
        # A word of two letters only the lexicon has (to, si, ol "front") is mostly noise, whose made-up forms would take real
        # words' (tong, olim); so is a verb of a stem of one or two letters only its classifier filed (yo-), but not one
        # glossed by hand (et- "to do").
        if len(word) < 3 and word not in SHORT or word in GUESSED_VERBS and len(word) < 6:
            continue
        if word in DOUBTFUL_VERBS and _mass(finite_forms(word)) < DOUBTFUL_SHARE:
            kinds = kinds - {"verb"}
            if not kinds:
                continue
        LEXICON_ONLY.add(word)
        pos[word].update(kinds)
        common.setdefault(word, word)
    # A passive or reflexive verb only the lexicon has (etilmoq, berilmoq, sotilmoq, deyilmoq) is its verb's (etmoq), as the
    # pack's Kazakh ones are: its forms are counted for that verb, and it is no word of the list. One commoner than its verb
    # (tugʻilmoq "to be born", of tugʻmoq; aylanmoq "to turn", of aylamoq) is a word of its own.
    folded = {}
    for word in sorted(w for w in LEXICON_ONLY if "verb" in pos[w] and w.endswith("moq")):
        base = passive_of(word, pos)
        if base and _mass(verb_forms(word) | {word}) < _mass(verb_forms(base) | {base}):
            folded[word] = base
    for word in folded:
        LEXICON_ONLY.discard(word)
        common.pop(word, None)
        pos.pop(word, None)
    fixed = set()
    # The copula's forms (edi, edim, emish): the others the pack's table makes are other words' (er "husband", esa, emas) or
    # none (ey, eyapman).
    lemmas.pop("emoq", None)
    for form in [f for f, of in lemmas.items() if "emoq" in of]:
        lemmas[form] = [l for l in lemmas[form] if l != "emoq"]
    for form in COPULA:
        lemmas[form] = ["emoq"]
        fixed.add(form)
    # The pack's tables for the pronouns are a noun's (menning, uga): their own cases instead.
    for word, forms in PRONOUNS.items():
        for form in [f for f, of in lemmas.items() if word in of]:
            lemmas[form] = [l for l in lemmas[form] if l != word]
        for form in forms.split():
            if form not in OWN:
                lemmas[form] = sorted(set(lemmas.get(form, [])) | {word})
                fixed.add(form)
    # A verb of the pack is a word of its own, though the pack has it as another's form too.
    for word in [w for w in lemmas if "verb" in pos.get(w, set()) and w.endswith("moq")]:
        del lemmas[word]

    generated = {}
    # The words each form is an inflection of, as no derived word is (yaxshilik, suvli; the verb's stem bor, borma, bora).
    inflected = {}

    def add(form, word, derived=()):
        if (seen is None or form in seen) and form != word:
            generated.setdefault(form, set()).add(word)
            if form not in derived:
                inflected.setdefault(form, set()).add(word)

    words = [w for w in common if WORD.fullmatch(w) and len(w) > 1 and w not in PRONOUNS and w != "emoq"]
    for word in words:
        kinds = pos.get(word, set())
        forms = regular_forms(word, kinds)
        # The -lik, -li, -siz, -cha words of their own (birlik, suvli) keep their forms.
        for made in (word + "lik", word + "li", word + "siz", word + "cha", word + "chilik"):
            if made in common:
                forms -= {made} | regular_forms(made, pos.get(made, set()))
        derived = derived_words(word, kinds)
        for form in forms | {word}:
            add(form, word, derived)
            # A clitic goes onto a form the counts have (uyimi on uyi).
            if seen is None or form in seen:
                for c in with_clitics(form):
                    add(c, word)
        if "verb" in kinds and word.endswith("moq"):
            # Its passive, causative and other verbs no source has are counted for it, but make no word its form (kurash
            # "struggle" is no form of kuramoq "to shovel" for being spelled as kura-sh-'s stem).
            for derived in derived_verbs(word):
                if derived in pos:
                    continue
                made = verb_forms(derived) | {derived}
                for form in made:
                    add(form, word, made)
    for word, base in folded.items():
        for form in verb_forms(word) | {word}:
            add(form, base)
            for c in with_clitics(form):
                add(c, base)
    for word in PRONOUNS:
        for form in PRONOUNS[word].split() + [word]:
            for c in with_clitics(form):
                add(c, word)
    # A noun or adjective only the lexicon has that is another word's regular form (instituti, shahri, oʻgʻli, oʻtgan,
    # oshirish: a possessive, a participle, a verbal noun) is that form, and no word of the list; but for the doers in -uvchi
    # (oʻqituvchi "teacher", oʻquvchi "pupil"), which are words of their own.
    def bare(word, other):
        """How common [word] is but for [other] and its forms."""
        if not (word.endswith("moq") and "verb" in pos.get(word, ())):
            return _mass([word])
        return _mass(verb_forms(word) - {other} - regular_forms(other, pos.get(other, set())))

    # One whose word is rare beside it (dollar, not dol's plural) is a word of its own.
    as_form = {w for w in LEXICON_ONLY if pos[w] <= _NOMINAL | {"verb"} and not w.endswith("vchi")
               and any(bare(g, w) >= FOLD_SHARE * _mass([w]) for g in inflected.get(w, ()) if g != w)}
    for word in as_form:
        LEXICON_ONLY.discard(word)
        common.pop(word, None)
    for form in list(generated):
        generated[form] -= as_form
        # A form of a word that is another's form too is the word's alone (kurashda of kurash "struggle", not of kuramoq,
        # whose verbal noun is spelled kurash).
        of = generated[form]
        if len(of) > 1:
            generated[form] = {g for g in of if not any(h != g and h in common and g in inflected.get(h, ()) for h in of)} or of
        if not generated[form]:
            del generated[form]
    for form, of in generated.items():
        if form in fixed or form in common and pos.get(form, set()) & _OWN_KINDS:
            continue
        have = lemmas.get(form, [])
        lemmas[form] = have + sorted(of - set(have))


def uzbek_base(word, ranks, cache={}):
    """The list word a regularly inflected Uzbek word is a form of (kitoblarimizda of kitob, borganimda of bormoq), or None."""
    for end in range(len(word) - 1, 0, -1):
        stem = word[:end]
        candidates = [(stem, "noun")]
        if stem.endswith("g"):
            candidates.append((stem[:-1] + "k", "noun"))
        if stem.endswith("g" + TURNED_COMMA):
            candidates.append((stem[:-2] + "q", "noun"))
        candidates += [(stem + "moq", "verb")]
        if stem.endswith("ma"):
            candidates.append((stem[:-2] + "moq", "verb"))
        for candidate, kind in candidates:
            if candidate not in ranks or candidate == word:
                continue
            key = (candidate, kind)
            if key not in cache:
                cache[key] = set().union(*(with_clitics(f) | {f} for f in regular_forms(candidate, {kind})))
            if word in cache[key]:
                return candidate
    return None


# Abbreviations the lexicon has as words, the accusative ending written apart (5 ni) and the .uz of web addresses.
EXCLUDE = set("km kg sm mm mln mlrd ni uz".split())
# The letter oʻ (its name, to the pack), a piece of -dagi, and the misspellings halq (xalq) and etkazmoq (yetkazmoq).
EXCLUDE |= {"oʻ", "dag", "halq", "etkazmoq"}
