"""
Kazakh (Cyrillic) for the frequency list and the course checker.

The English pack has about 13,000 Kazakh entries; its nouns have their cases (кітап, кітаптың, кітаптарға) but no possessive
forms, and only about 170 of its 720 verbs have any forms at all. Counted only through it, a Kazakh text, whose words carry
one ending after another (кітаптарымызда "in our books", айтылмаған "not said"), is mostly no word of the list. So, as
Mongolian's is, the list is grouped by the regular forms (paradigms) of every word of the pack:

- a noun's, adjective's or number's cases, plural, possessives, the possessives' cases, the locative-attributive -дағы, the
  similative -дай and the predicative personal endings (баламын "I am a child"), each ending's first letter as the word's last
  sound has it (кітапқа, үйге, қалаға; кітаптың, үйдің, қаланың) and its vowel by the word's last vowel (back а, ы; front е, і),
  with к, қ, п voiced before a vowel (кітабы, жүрегі, аяғы) and a short last vowel dropped where it may be (халқы, аузы);
- a verb's (cited in -у, as the dictionary has it: айту, оқу, сөйлеу, шығу) tenses with their personal endings, participles
  with their cases and possessives (айтқаны, айтқанын, баратынын), converbs, conditional, imperatives, the verbal noun's cases
  (айтуға, айтудың) and all of these negated (айтпады, айтпайтын), and its passive, reflexive, reciprocal and causative
  verbs (айтылды, жиналды, көрсетті) where the pack has no entry for them;
- the harmonic variants of the little words are one word (ма, ме, ба, бе, па, пе; да, де, та, те; мен, бен, пен);
- the personal and demonstrative pronouns get their irregular cases (маған, оған, бұған, мұны, соны).

A form that is a word of the pack itself is left to that word (бар "there is", not бару's imperative; оқушы "pupil"). The
corpora write the letter і as the Latin i in many words (едi, кейiн, бiрақ), and some other letters as their Latin look-alikes;
normalise writes them as the Cyrillic letters.

kazakh_base reads a word the list does not have as a regular form of a list word, for the course checker.
"""
import json
import os
import re
from collections import defaultdict

LETTERS = re.compile(r"[а-яёәғқңөұүһі]+")
# Latin letters written for the Cyrillic ones they look like, inside Cyrillic words.
_LOOKALIKES = str.maketrans({"i": "і", "a": "а", "e": "е", "o": "о", "p": "р", "c": "с", "x": "х", "y": "у", "ə": "ә",
                             "ɵ": "ө", "ө": "ө", "h": "һ", "k": "к", "m": "м", "t": "т", "b": "в", "ї": "і", "ı": "і"})
_CYRILLIC = re.compile(r"[а-яёәғқңөұүһі]")


def normalise(form):
    """A form with the Latin look-alikes of Kazakh letters (едi, кейiн) written as the Cyrillic letters, if it has Cyrillic."""
    if _CYRILLIC.search(form) and not LETTERS.fullmatch(form):
        folded = form.translate(_LOOKALIKES)
        if LETTERS.fullmatch(folded):
            return folded
    return form


_TRUE_VOWELS = set("аәеоөұүыіэяё")
_BACK = set("аоұыя")
# ё is only in loanwords, which take front endings (актёрлер).
_FRONT = set("әеөүіэё")


def harmonies(word):
    """
    The vowels a word's endings take, ("а", "ы") or ("е", "і"), by its last vowel; both where that is и, у or ю (ми, миы; би,
    биі; алфавит, алфавиттер) or a back word ends in к or г, as loanwords do (банк, банкке).
    """
    for c in reversed(word):
        if c in _BACK:
            return [("а", "ы"), ("е", "і")] if word[-1] in "кгь" else [("а", "ы")]
        if c in _FRONT:
            return [("е", "і")]
        if c in "иую":
            break
    return [("а", "ы"), ("е", "і")]


def _last(word):
    return word.rstrip("ьъ")[-1:] or word[-1:]


def sound(word):
    """
    The class of a word's last sound that picks the first letter of an ending: V a vowel, R р й у и, L л, N м н ң, Z з ж, T a
    voiceless consonant (and б в г д ғ of loanwords, which end voiceless: клубқа, заводқа).
    """
    c = _last(word)
    if c in _TRUE_VOWELS:
        return "V"
    if c in "рйуию":
        return "R"
    if c == "л":
        return "L"
    if c in "мнң":
        return "N"
    if c in "зж":
        return "Z"
    return "T"


# An ending's first letter by the last sound (sound): plural, genitive, accusative, dative, locative, ablative, instrumental.
_PLURAL = {"V": "л", "R": "л", "L": "д", "N": "д", "Z": "д", "T": "т"}
_GENITIVE = {"V": "н", "N": "н", "R": "д", "L": "д", "Z": "д", "T": "т"}
_ACCUSATIVE = {"V": "н", "R": "д", "L": "д", "N": "д", "Z": "д", "T": "т"}
_VOICED = {"V": True, "R": True, "L": True, "N": True, "Z": True, "T": False}
_ABLATIVE = {"V": "д", "R": "д", "L": "д", "Z": "д", "N": "н", "T": "т"}
_INSTRUMENTAL = {"V": "м", "R": "м", "L": "м", "N": "м", "Z": "б", "T": "п"}
# The predicative "I am" and "we are" (баламын, қазақпын, жалғызбын; барғанмын, барғанбыз), and a verb's negative -ма.
_FIRST = {"V": "мб", "R": "м", "L": "мб", "N": "мб", "Z": "б", "T": "п"}
_NEGATIVE = {"V": "м", "R": "м", "L": "м", "N": "б", "Z": "б", "T": "п"}
_VOICING = {"п": "б", "к": "г", "қ": "ғ"}
_UNVOICING = {"б": "п", "г": "к", "ғ": "қ"}


def _g(word, a):
    """The dative's and participle's ғ/г, or қ/к after a voiceless sound."""
    back = a == "а"
    if _VOICED[sound(word)]:
        return "ғ" if back else "г"
    return "қ" if back else "к"


def _q(a):
    return "қ" if a == "а" else "к"


def cases(x, a, y, after=None):
    """
    The case forms of a stem [x] in harmony (a, y): genitive, accusative, dative, locative, ablative, instrumental, the
    locative-attributive and the similative. [after] is "3" after a third-person possessive (кітабының, кітабын, кітабына,
    кітабында, кітабынан), "12" after a first- or second-person singular one (кітабыма, кітабыңа).
    """
    s = sound(x)
    g = "ғ" if a == "а" else "г"
    if after == "3":
        # With "according to" too (айтуынша, пікірінше).
        out = [x + "н" + y + "ң", x + "н", x + "н" + a, x + "нд" + a, x + "н" + a + "н", x + "мен", x + "нд" + a + g + y,
               x + "нш" + a]
        return out
    loc = ("д" if _VOICED[s] else "т") + a
    out = [x + _GENITIVE[s] + y + "ң", x + _ACCUSATIVE[s] + y, x + loc, x + _ABLATIVE[s] + a + "н",
           x + _INSTRUMENTAL[s] + "ен", x + ("мен" if s == "Z" else _INSTRUMENTAL[s] + "ен"), x + loc + (g if _VOICED[s] or True else g) + y, x + ("д" if _VOICED[s] else "т") + a + "й"]
    # After у and и the pack's tables have н too (баруның, емуны, вискинің).
    if x.endswith(("у", "и")):
        out += [x + "н" + y + "ң", x + "н" + y]
    if after == "12":
        # And "in my (your) opinion" (ойымша, менімше).
        out += [x + a, x + "мен", x + "ш" + a]
    else:
        out.append(x + _g(x, a) + a)
    return out


def _vowel_stems(word):
    """The stems a vowel ending is joined to: the word, with к қ п voiced (кітаб, жүрег), and short of its last vowel (халқ)."""
    stems = [word]
    if word[-1] in _VOICING:
        stems.append(word[:-1] + _VOICING[word[-1]])
    # A short last vowel between consonants may drop (халық, халқы; ауыз, аузы; орын, орны; ерік, еркі).
    if len(word) >= 4 and word[-2] in "ыіұү" and word[-1] not in _TRUE_VOWELS and word[-3] in "лрйуз" and \
            any(c in _TRUE_VOWELS for c in word[:-3]):
        stems.append(word[:-2] + word[-1])
    return stems


def possessives(word, a, y):
    """A noun's possessive forms, each with whether it is first or second singular ("12"), third ("3") or other (None)."""
    out = []
    if sound(word) == "V":
        out += [(word + "м", "12"), (word + "ң", "12"), (word + "ң" + y + "з", None), (word + "с" + y, "3"),
                (word + "м" + y + "з", None), (word + "ңд" + a + "р", None), (word + "ң" + y + "зд" + a + "р", None)]
    else:
        for v in _vowel_stems(word):
            out += [(v + y + "м", "12"), (v + y + "ң", "12"), (v + y + "ң" + y + "з", None), (v + y, "3"),
                    (v + y + "м" + y + "з", None), (v + y + "ңд" + a + "р", None), (v + y + "ң" + y + "зд" + a + "р", None)]
    return out


def predicatives(x, a, y):
    """A word with the personal endings of "to be" (баламын, баласың, баласыз, баламыз; студентпін)."""
    s = sound(x)
    out = [x + "с" + y + "ң", x + "с" + y + "з", x + "с" + y + "ңд" + a + "р", x + "с" + y + "зд" + a + "р"]
    for m in _FIRST[s]:
        out += [x + m + y + "н", x + m + y + "з"]
    if s in ("L", "N"):
        out.append(x + "б" + y + "з")
    return out


def spelled(form):
    """A form as it is written: й before а or у is я or ю (қой + ады is қояды, қой + у is қою)."""
    return form.replace("йа", "я").replace("йу", "ю") if "й" in form else form


def attributive(word, a, y):
    """A word with -ғы, of a time or place (жылғы, бүгінгі, кешегі, қысқы, кейінгі)."""
    return word + _g(word, a) + y


def comparatives(word, a, y):
    """An adjective's or adverb's comparative in -рақ (жақсырақ, тезірек, көбірек)."""
    if sound(word) == "V":
        return [word + "р" + a + _q(a)]
    return [v + y + "р" + a + _q(a) for v in _vowel_stems(word)[:2]]


def noun_forms(word, plural=True, predicative=True):
    """A noun's (adjective's, number's) regular forms: cases, plural, possessives and their cases, predicative endings."""
    out = set()
    for a, y in harmonies(word):
        out.update(cases(word, a, y))
        if predicative:
            out.update(predicatives(word, a, y))
        for p, kind in possessives(word, a, y):
            out.add(p)
            out.update(cases(p, a, y, kind))
        if plural:
            pl = word + _PLURAL[sound(word)] + a + "р"
            out.add(pl)
            out.update(cases(pl, a, y))
            if predicative:
                out.update(predicatives(pl, a, y))
            for p, kind in possessives(pl, a, y):
                out.add(p)
                out.update(cases(p, a, y, kind))
        # The -нікі of whose (баланікі, Асқардікі).
        out.add(word + _GENITIVE[sound(word)] + "ікі")
        # Without (баласыз, ақшасыз), and of a time (жылғы, кешегі, қысқы).
        out.add(word + "с" + y + "з")
        out.add(attributive(word, a, y))
    out = {spelled(f) for f in out}
    out.discard(word)
    return out


def verb_stems(verb):
    """
    The stems of a verb cited in -у (or -ю): before a consonant and before a vowel, and whether it ends in a vowel. A stem in
    к қ п is written voiced before a vowel (шығу: шықты, шығады); оқу may be оқы- (оқиды) as well as оқ-, and the stem of a
    cluster may have a vowel inside before a consonant (қорқу: қорықты, қорқады): each is tried.
    """
    if verb.endswith("ю"):
        if verb[-2:-1] == "и":
            return [(verb[:-1], verb[:-1], False)]
        # қою is қой-; баю is байы- (байиды, байыды).
        base = verb[:-1] + "й"
        return [(base, base, False)] + [(base + y, base + y, True) for _, y in harmonies(base)]
    if not verb.endswith("у") or len(verb) < 2:
        return []
    base = verb[:-1]
    if base[-1] in "аеәоө":
        return [(base, base, True)]
    if base[-1] in "ыіұү":
        # жуу, туу, сүю: the stem ends in у.
        return [(base, base, False)]
    stems = []
    consonant = _UNVOICING.get(base[-1])
    plain = base[:-1] + consonant if consonant else base
    stems.append((plain, base, False))
    for a, y in harmonies(base):
        stems.append((base + y, base + y, True))
        if len(base) > 2 and base[-2] not in _TRUE_VOWELS and base[-2] not in "йу" and base[-3] in _TRUE_VOWELS:
            stems.append((base[:-1] + y + (consonant or base[-1]), base, False))
    return stems


def _joined(stem, ending):
    """A vowel stem in ы/і with an ending that starts with й or у: оқы + й = оқи, оқы + у = оқу."""
    if stem[-1] in "ыі" and ending[:1] == "й":
        return stem[:-1] + "и" + ending[1:]
    if stem[-1] in "ыі" and ending[:1] == "у":
        return stem[:-1] + ending
    return stem + ending


def _verb_forms(stem, voiced, vowel, a, y, negative=True, nominal=True):
    out = set()
    q = _q(a)
    s = sound(stem)
    personal = ["м", "ң", "ң" + y + "з", "", q, "ңд" + a + "р", "ң" + y + "зд" + a + "р"]
    # The present-future and its converb (айта, оқи, сөйлей): айтамын, айтады; оқимын, оқиды.
    present = _joined(stem, "й") if vowel else voiced + a
    out.add(present)
    out.update(present + e for e in ("м" + y + "н", "с" + y + "ң", "с" + y + "з", "д" + y, "м" + y + "з",
                                     "с" + y + "ңд" + a + "р", "с" + y + "зд" + a + "р"))
    # The past: айттым, айтты, айттық.
    past = stem + ("д" if _VOICED[s] else "т") + y
    out.update(past + e for e in personal)
    # The conditional: айтсам, айтса.
    out.update(stem + "с" + a + e for e in personal)
    out.add(stem + "с" + a + "ш" + y)
    # The imperatives: айт, айтыңыз, айтыңдар, айтайын, айтайық, айтсын, айтшы, айтыңызшы.
    out.add(stem)
    join = stem if vowel else voiced + y
    out.update({join + "ң" + y + "з", join + "ңд" + a + "р", join + "ң" + y + "зд" + a + "р", join + "ң" + y + "зш" + y,
                stem + "с" + y + "н", stem + "ш" + y})
    first = _joined(stem, "й") if vowel else voiced + a + "й"
    out.update({first + y + "н", first + y + q})
    # The converb in -ып and the evidential past: айтып, айтыпты, айтыппын; and of a stem in п, тауып (табу).
    converbs = [stem + "п" if vowel else voiced + y + "п"]
    if not vowel and stem.endswith("п"):
        converbs.append(stem[:-1] + "у" + y + "п")
    for converb in converbs:
        out.update({converb, converb + "т" + y})
        out.update(predicatives(converb, a, y))
    # The aorist: айтар, айтармын.
    aorist = stem + "р" if vowel else voiced + a + "р"
    out.add(aorist)
    out.update(predicatives(aorist, a, y))
    # The intention: айтпақ, айтпақпын, айтпақшы.
    neg = _NEGATIVE[s]
    intention = stem + neg + a + q
    out.update({intention, intention + "ш" + y})
    out.update(predicatives(intention, a, y))
    out.update(predicatives(intention + "ш" + y, a, y))
    # The wish: айтқым, айтқысы (келеді).
    g = _g(stem, a)
    wish = stem + g + y
    out.update(wish + e for e in ("м", "ң", "с" + y, "м" + y + "з", "ң" + y + "з", "с" + y + "н", "с" + y + "н" + a))
    # The converbs -ғалы, -ғанша, -ғасын.
    out.update({stem + g + a + "л" + y, stem + g + a + "нш" + a, stem + g + a + "с" + y + "н"})
    # The past participle and the future participle, with their cases, possessives and personal endings: айтқан, айтқаны,
    # айтқанын, айтқанда, айтқанмын; айтатын, айтатыны.
    participles = [stem + g + a + "н", _joined(stem, "йт" + y + "н") if vowel else voiced + a + "т" + y + "н"]
    # The verbal noun: айту, айтуға, айтуы, айтуым.
    noun = _joined(stem, "у") if vowel else voiced + "у"
    out.add(noun)
    for p in participles + [noun]:
        out.add(p)
        if nominal:
            out.update(noun_forms(p, plural=True, predicative=True))
    # The progressive in -уда, and the -ғандықтан "because".
    out.add(participles[0] + "д" + y + q + "т" + a + "н")
    if negative:
        # Negated, the stem is a vowel stem: айтпа-йды, айтпа-ды, айтпа-ған, айтпа-й; and the negative aorist айтпас.
        for n in {stem + neg + a}:
            out.update(_verb_forms(n, n, True, a, y, negative=False, nominal=nominal))
            out.add(n + "с")
            out.update(predicatives(n + "с", a, y))
    return out


def verb_forms(verb, nominal=True):
    """A verb's regular forms, negated too, for each stem it may have."""
    out = set()
    for stem, voiced, vowel in verb_stems(verb):
        for a, y in harmonies(stem):
            out.update(_verb_forms(stem, voiced, vowel, a, y, nominal=nominal))
    out = {spelled(f) for f in out}
    out.discard(verb)
    return {f for f in out if LETTERS.fullmatch(f)}


def derived_verbs(verb):
    """The passive, reflexive, reciprocal and causative verbs regularly made from a verb (айтылу, жиналу, көрісу, жазғызу)."""
    out = set()
    for stem, voiced, vowel in verb_stems(verb):
        for a, y in harmonies(stem):
            g = _g(stem, a)
            causatives = [stem + g + y + "з", stem + ("д" if _VOICED[sound(stem)] else "т") + y + "р"]
            if vowel:
                # With the passive of a vowel stem doubled too (делінген).
                made = [stem + "л", stem + "н", stem + "с", stem + "л" + y + "н"]
                causatives.append(stem + "т")
            else:
                made = [voiced + y + ("н" if stem.endswith("л") else "л"), voiced + y + "н", voiced + y + "с"]
            # A causative's passive (енгізілген, жүргізілді, орнатылған).
            made += causatives + [c + y + "л" for c in causatives]
            for m in made:
                out.add(m + "у")
    out.discard(verb)
    return out


# The little words' harmonic variants, each the first word's.
VARIANTS = {"ма": ("ме", "ба", "бе", "па", "пе"), "да": ("де", "та", "те"), "мен": ("бен", "пен"), "ғой": ("қой",),
            "ғана": ("қана",)}
# Words no pack entry has, with their kinds. The English pack lacks many of the commonest (жету, қайту, бұрын, мұндай, жиі,
# тірі): these are the ones the corpora use most that are no form of a word of the pack, and everyday words besides.
ADDED = {"да": "particle", "тағы": "adv", "арқылы": "prep", "қандай": "pron"}
for _kind, _words in {
    "verb": """жету қайту қолдау шығару жинау кездесу дайындау қолдану орындау ауыстыру әкелу әкету сүру жоғалу жуу тазалау
        пісіру көмектесу мәлімдеу арнау келтіру қайтару асыру өзгерту өзгеру тарау ұйымдастыру атқару ауыру айналысу көріну
        өтіну қарастыру қадағалау қудалау қамту мойындау иелену күшею үлгеру көздеу алдау босау көшіру шалу шегу тию кешу ілу
        жаю сынау айдау төну күресу арту жұмсау сағыну оқыту қыдыру ашулану кеңею қысқару азаю жалғастыру ескеру тоқтату
        жалғасу жөнелту құттықтау шақырту тыйылу ұрлау ұру қуу сөгу мақтану таңдану қызығу шешіну жуыну тамақтандыру
        демалдыру бастыру іздестіру тексерту сұрастыру әзірлеу түсіндіру шомылу айығу жазылу жөтелу түшкіру есінеу
        сыйлау тігу тоқу үтіктеу сыпыру ұйықтату ұнату жасыру жасырыну тығылу күту тапсыру тастау""",
    "noun": """бой басшы журналист депутат судья ая тұрғы тұс аст арт кесір жай дау орган бизнес процесс қаражат бюджет валюта
        кандидат гектар кен проблема қатысушы нұқсан тосқауыл үкім соққы видео жылу отставка интернет шеру науқан қастандық
        күдік жақтас режиссер қарт алғыс бәсеке астар тиек айыпталушы жәбірленуші төлем ереуіл бағам тізгін гол жұртшылық шу
        алаңдаушылық виза тапшылық дүркін килограмм жүлдегер тонна олимпиада чемпион адвокат совет түрткі жұмысшы наразылық
        аралық қамау тыйым жарты такси вокзал кафе фото тарелка печенье аяқкиім кешкі мейрам демалыс саяхат сапар қонақүй
        пойыз ұшқыш жүргізуші сатушы аспаз дәрі дәріхана шаштараз ойыншық қуыршақ доп велосипед жаңбыр боран тұман аяз
        жұмыртқа көже палау бауырсақ қымыз шұбат ірімшік сүзбе айран қаймақ шырын кәмпит торт сабын сүлгі тіс шаш
        арқа іш бел мойын иық тізе саусақ тырнақ ерін тіл құлақ мұрын маңдай бет шеке ана әже немере іні сіңлі әпке жиен
        ағай келіншек нағашы құда жезде жеңге отбасы туыс көрші""",
    "adj": """мұндай осындай ешқандай мынадай мынандай сондай бұндай белгілі түрлі бөлек тірі ерте наразы қарулы жария ондаған
        мыңдаған жүздеген миллиондаған тиесілі жеткілікті айқын басқаша сапалы қолайлы жекеменшік көршілес білікті құзырлы
        қатерлі жазбаша санаулы өрескел кәдімгі ащы өткір сұмдық елеулі талантты тік тығыз шамалы бақытты қуанышты сабырлы
        зиянды ұялы баяғы тәуелді сыбайлас коммерциялық коммуналдық террорлық экстремистік кедендік авторитарлық қызметтік
        кеңестік заңдық аталмыш бірлескен қаншама тыныш баяу шулы жайлы ыңғайлы қымбат дәмсіз ашулы ренішті шаршаңқы ұқыпты
        тәртіпті сыпайы мейірімді ақкөңіл жомарт сараң""",
    "adv": """бұрын жиі әрі солай олай бұлай алдымен алғаш талай сосын онша әзірше біршама бірер көбінесе неліктен
        біртіндеп небәрі дерлік барынша керісінше ұзамай күрт мейлінше айтарлықтай бүгінде қайтадан қаншалықты соншалықты
        бірден әлдебір әлгі бәрібір қалайша барша бірқатар әрбір кей сонау осынау әрқайсысы ешқайсысы жалғыз әрең әдейі
        кешке түнде таңертең бірге бірақ дейін шейін әбден мүлде мүлдем тым әсте""",
    "intj": "жарайды жарайсың әй ой әттең пай уа қап",
    "conj": "һәм я яки әйтпесе",
}.items():
    for _word in _words.split():
        ADDED.setdefault(_word, _kind)
# Words of a kind above that are verbs too (қамау "arrest", "to arrest"; жолдау "message", "to send").
ADDED_VERBS = {"қамау", "жолдау"}
for _kind, _words in {
    "verb": """қылу ерту кесу өтеу қозғау аралау белгілеу бағыттау құрау таныстыру жаралау нашарлау жақсарту жақындау азайту
        күшейту қаржыландыру төмендеу өсіру ұзарту жоғалту мәжбүрлеу нығайту қымбаттау насихаттау толтыру байланыстыру
        туындау қоршау тізу жұқтыру ұласу шабу жұлу арқалау шоғырлану ушығу жаулау қаптау алаңдау үміттену қауіптену
        шағымдану сіңіру""",
    "noun": """алд радио әкімдік төрайым телеарна рубль формат сепаратист бизнесмен митинг комментарий филиал мигрант атлет
        сенатор актер банкир тергеуші сотталушы үлескер шеруші хиджаб үрдіс бұзушылық келісімшарт премьер километр колония
        милиция өндіруші еңбекақы металл медаль триллион тума мэр тірлік күнкөріс балл тер сыйақы араздық иелік бөлімше
        құран әркім қайсы әлек""",
    "adj": """қосалқы әлгіндей озық құқылы сыңайлы көрнекті тиімсіз жарамсыз жіті аяусыз атышулы шешуші ұтымды сорақы
        қисынсыз жалпыұлттық постсоветтік маркалы дәрежелі зәру түпкілікті толыққанды осыншама осынша сонша небір душар""",
    "adv": """еш дөп түгелдей екіталай анағұрлым неғұрлым сірә ылғи әзер былайша бұлайша қысқаша аздап қашанда ешуақытта
        біраз төтеп""",
    "pron": "қай бірдеңе",
    "particle": "міне",
    "intj": "қайдам о әу ә қайран",
}.items():
    for _word in _words.split():
        ADDED.setdefault(_word, _kind)
for _kind, _words in {
    "verb": """жолдау бірлесу асырау шалдығу ұйымдасу мамандандыру жылжу мақұлдау қиындау үйлену жақтау жиілеу жығу жағу қадау
        құлдырау жию қысу қыру сілтеу жүгіну кеңейту орау ысыру үйлестіру айрылу күзету баптау жетілдіру үнемдеу тойлау
        бейімдеу заңдастыру теру тыну жолығу қию озу ілігу толықтыру өндіру ауысу танысу жетісу""",
    "noun": """шығын қарсылас егін лақап мораторий диссидент блогер сұқбат қарқын допинг бүлік медиа атлетика коммуникация
        девальвация позиция квота элита миллиардер киік лидер кандидатура өсім қуғын изолятор фестиваль айырма интеграция
        репрессия табалдырық теракт зейнеткерлік борышкер жем цензура құқыққорғаушы колледж мониторинг геноцид менеджер мұң
        желеу өкілет өкілеттік шолу қарсаң төңірек өкімет доктор коррупция курс факт тендер фирма камера жырау естелік
        сыбыс мердігер құрылтайшы көшірме қарымта тұрақсыздық қымбатшылық жеңілдік сайыс үзінді астық""",
    "adj": """коммунистік террористік туристік журналистік бөлмелі балалы көпбалалы орыстілді қазақтілді ресейшіл кішігірім
        ақшалай қатарлы бұлыңғыр асығыс бейім мәжбүрлі тәуір байырғы сайлауалды ойлы адами кассациялық күлкілі жалдамалы
        шекаралас қатыгез дамушы оңтайлы үстем бағынышты жалпақ аграрлық интеграциялық дөрекі шикі бастамашыл қитұрқы жемқор
        іскер айтулы қыруар француз латын армян православ тұрғылықты көпқабатты бірегей шалғай іспетті ауызша біртүрлі
        оншақты жалпыхалықтық сай үздік""",
    "adv": """аулақ көптеп өздігінен бірауыздан тіптен біресе ұдайы ілгері мықтап амалсыздан ертеректе әрмен кілең күштеп
        белсене бірлесе онан аяқасты әшкере жүзеге""",
}.items():
    for _word in _words.split():
        ADDED.setdefault(_word, _kind)
# Spellings that are mostly a common word's form, though the pack has them as a rarer word of their own (алып "giant",
# тұрып "radish", жатыр "womb", десе "dessiatina"), or as a word that is a form (күні, отыр): the common word's.
KAZAKH_OVERRIDES = {}
for _word, _forms in {
    "алу": "алып алды алыңыз алыңдар", "қарау": "қараған қарады", "жазу": "жазған", "тұру": "тұрып", "деу": "десе",
    "айту": "айт", "келу": "келі", "беру": "берет", "жату": "жатыр", "отыру": "отыр", "күн": "күні", "болу": "болатын боп болары боларын",
    "жыл": "жылы", "әкелу": "әкеп", "құқық": "құқы құқын құқына құқымыз құқымызды", "бәрі": "бәріміз бәрімізге", "көп": "көбісі", "алд": "алда алдағы",
}.items():
    KAZAKH_OVERRIDES.update({_form: [_word] for _form in _forms.split()})
# Forms of the auxiliaries еді and екен, which no paradigm makes.
AUXILIARIES = {
    "еді": "едім едің едіңіз едік едіңдер едіңіздер",
    "екен": "екенмін екенсің екенсіз екенбіз екенміз екенсіңдер екенін екені екеніне екенінде екендігі екендігін екендігіне "
            "екенін екенімді екеніңді екенімізді екеніңізді екендігімен",
}
# Words the pack has as adjectives too that take no noun endings (ал "but": алда, алдағы are алд's).
NOT_NOMINAL = {"ал"}
# The adverbs with a comparative (тезірек, жиірек, ертерек).
COMPARED = {"тез", "жиі", "ерте", "кеш", "жылдам", "баяу", "ақырын", "әрі", "аз", "көп"}
# Words that take the noun endings though the pack has them as another kind (өзі, өзінің; жоққа, жоғы; анасы).
NOMINAL_ALSO = {"өз", "жоқ", "ана", "бар", "емес", "кейбір", "қайсы", "біраз", "әркім", "бүгін", "кейін", "қазір", "ертең", "кеше",
                "бұрын", "ешкім", "ештеңе"}
# The verbs whose stems stand alone as the present progressive, with its personal endings (отырмын, жүрміз, жатырсың).
PROGRESSIVES = {"отыру": "отыр", "жүру": "жүр", "тұру": "тұр", "жату": "жатыр"}
# The pronouns' cases, which no paradigm makes (маған, оған, мұны).
PRONOUNS = {
    "мен": "менің маған мені менде менен менімен менсіз менікі",
    "сен": "сенің саған сені сенде сенен сенімен сенсіз сенікі",
    "ол": "оның оған оны онда одан онымен оларсыз онікі",
    "біз": "біздің бізге бізді бізде бізден бізбен бізсіз біздікі біздегі біздер",
    "сіз": "сіздің сізге сізді сізде сізден сізбен сізсіз сіздікі сіздегі",
    "олар": "олардың оларға оларды оларда олардан олармен",
    "сендер": "сендердің сендерге сендерді сендерде сендерден сендермен",
    "сіздер": "сіздердің сіздерге сіздерді сіздерде сіздерден сіздермен",
    "бұл": "бұның мұның бұған мұған бұны мұны бұнда мұнда бұдан мұнан мұдан бұнымен мұнымен бұлар бұлардың бұларға бұларды "
           "бұларда бұлардан бұлармен",
    "осы": "осының осыған осыны осында осыдан осымен осылар осылардың осыларға осыларды",
    "сол": "соның соған соны сонда содан сонымен солар солардың соларға соларды соларда солардан солармен",
    "мына": "мынаның мынаған мынаны мынада мында мынадан мынамен мыналар мыналардың мыналарды",
    "ана": "ананың анаған ананы анада анадан анамен аналар",
    "кім": "кімнің кімге кімді кімде кімнен кіммен кімдер кімдердің кімдерге кімдерді кімсің кіммін кімсіз",
    "не": "ненің неге нені неде неден немен нелер нелерді несі несін",
}
# Words a form of another is spelled as, which keep it (онда "there", not ол's locative alone; мұнда "here").
OWN = {"онда", "мұнда", "сонда", "осында", "одан", "содан", "неге", "мында"}


def _kinds_of(db):
    kinds = defaultdict(set)
    for word_lc, p in db.execute("SELECT word_lc, pos FROM entries"):
        kinds[word_lc].add(p)
    return kinds


_NOMINAL = {"noun", "adj", "num"}
# Kinds whose words take the noun endings when they stand for a noun (бәрі, бірі, көбі): adverbs and postpositions do not.
_KINDS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "intj", "particle", "postp"}


def regular_forms(word, kinds, nominal_verbs=True):
    """A word's regular forms, by its kinds."""
    out = set()
    if "verb" in kinds and word.endswith(("у", "ю")):
        out |= verb_forms(word, nominal=nominal_verbs)
    if (kinds & _NOMINAL and word not in VARIANTS and word not in PRONOUNS and word not in NOT_NOMINAL) or word in NOMINAL_ALSO:
        out |= noun_forms(word)
    for a, y in harmonies(word):
        if "adj" in kinds or word in COMPARED:
            out.update(comparatives(word, a, y))
        if kinds & {"adv", "prep", "postp"}:
            out.add(attributive(word, a, y))
    # A loanword's double or -ст end is written single before an ending (процесі, процестер; журналистер).
    if out and len(word) > 3 and word[-1] not in _TRUE_VOWELS and (word[-1] == word[-2] or word.endswith("ст")):
        out |= noun_forms(word[:-1])
    out.discard(word)
    return out


def adjective_forms(word):
    """A noun's adjectives in -лық (аудандық, оппозициялық), each with its commonest forms."""
    out = {}
    for a, y in harmonies(word):
        made = word + {"V": "л", "R": "л", "L": "д", "N": "д", "Z": "д", "T": "т"}[sound(word)] + y + _q(a)
        voiced = made[:-1] + _VOICING[made[-1]]
        out[made] = {made, voiced + y, voiced + y + "н", voiced + y + "н" + a, voiced + y + "нд" + a}
    return out


# Seen forms: when set (the builder sets it to the corpus's forms before load), only these of the generated forms are kept.
SEEN = None


def load(db, pos, lemmas, common, dictionaries):
    """Adds the words and forms above to the English pack's [pos], [lemmas] and [common], as load_dictionary has them."""
    seen = SEEN
    for word, kind in ADDED.items():
        pos[word].add(kind)
        common[word] = word
    for word in ADDED_VERBS:
        pos[word].add("verb")
    fixed = set()
    for word, variants in VARIANTS.items():
        common.setdefault(word, word)
        fixed.add(word)
        for variant in variants:
            common.pop(variant, None)
            lemmas[variant] = [word]
            fixed.add(variant)
    # A verb of the pack is a word of its own, though the pack has it as another's form too (түсіну of түсу, оралу of орау).
    for word in [w for w in lemmas if "verb" in pos.get(w, set()) and w in common and w.endswith(("у", "ю"))]:
        if word in lemmas and word not in KAZAKH_OVERRIDES:
            del lemmas[word]
    progressive = {v: " ".join(f for a, y in harmonies(b) for f in predicatives(b, a, y)) for v, b in PROGRESSIVES.items()}
    for word, forms in list(PRONOUNS.items()) + list(AUXILIARIES.items()) + list(progressive.items()):
        for form in forms.split():
            if form not in OWN:
                lemmas[form] = sorted(set(lemmas.get(form, [])) | {word})
                fixed.add(form)

    def own_word(w):
        return w in common and bool(pos.get(w, set()) & _KINDS) and w not in fixed

    generated = defaultdict(set)
    verbs = [w for w in common if "verb" in pos.get(w, set()) and LETTERS.fullmatch(w) and w.endswith(("у", "ю"))]
    for word in [w for w in common if LETTERS.fullmatch(w) and len(w) > 1]:
        for form in regular_forms(word, pos.get(word, set())):
            if seen is None or form in seen:
                generated[form].add(word)
    # A noun's adjective in -лық is the noun's where the pack has no such word (аудандық; not барлық "all", бар's).
    for word in [w for w in common if LETTERS.fullmatch(w) and len(w) > 1 and ("noun" in pos.get(w, set()) or w in NOMINAL_ALSO)]:
        for made, forms in adjective_forms(word).items():
            if made in common:
                continue
            for form in forms:
                if seen is None or form in seen:
                    generated[form].add(word)
    for verb in verbs:
        for derived in derived_verbs(verb):
            if "verb" in pos.get(derived, set()):
                continue
            for form in verb_forms(derived) | {derived}:
                if seen is None or form in seen:
                    generated[form].add(verb)
    for form, words in generated.items():
        if own_word(form) or form in fixed:
            continue
        have = lemmas.get(form, [])
        lemmas[form] = have + sorted(words - set(have))


def kazakh_base(word, ranks, cache={}):
    """The list word a regularly inflected Kazakh word is a form of (кітаптарымызда of кітап), or None."""
    for end in range(len(word) - 1, 0, -1):
        stem = word[:end]
        candidates = [stem]
        if stem[-1] in _UNVOICING:
            candidates.append(stem[:-1] + _UNVOICING[stem[-1]])
        # A dropped vowel (халқы of халық, аузы of ауыз).
        if len(stem) > 2 and stem[-2] not in _TRUE_VOWELS and stem[-1] not in _TRUE_VOWELS:
            candidates += [stem[:-1] + v + stem[-1] for v in "ыіұү"]
        # A verb (айтты of айту, оқиды of оқу, шықты of шығу).
        verbs = [stem + "у", (stem[:-1] + "у") if stem[-1:] in "ыі" else None, stem + "ю",
                 (stem[:-1] + _VOICING[stem[-1]] + "у") if stem[-1:] in _VOICING else None]
        for candidate, kinds in [(c, {"noun"}) for c in candidates] + [(v, {"verb"}) for v in verbs if v]:
            if candidate not in ranks or candidate == word:
                continue
            key = (candidate, frozenset(kinds))
            if key not in cache:
                cache[key] = regular_forms(candidate, kinds) | (
                    {f for d in derived_verbs(candidate) for f in verb_forms(d) | {d}} if "verb" in kinds else set())
            if word in cache[key]:
                return candidate
    return None
