"""
Mongolian (Khalkha, Cyrillic) for the frequency list.

The English pack has few Mongolian words (about 4,700) and fewer forms: a verb has only some of its participles, and many
nouns have none. Counted only through it, half of the news and Wikipedia text was no word of the list, verbs were ranked by
their dictionary form alone (хэлэх 528), and the pack's entries glossed as another word's form (байна, болсон, сарын)
were words of their own. So:

- the Russian and Chinese packs add the words the English one lacks (ажиллах, илүү, эсвэл, шиг, санаа), and a few little
  words no pack has are added by hand (гэвч, гэхдээ, тэгвэл);
- an entry glossed only as another word's form is that word's form (байна of байх, сарын of сар, таны of та), except the
  few that are words in use (болон "and", хүртэл "until", харин "but", удаа "time"); a verb glossed as another's voice
  (ойлгох, харуулах, нэмэгдэх) is a word of its own, and so are the verbs the pack files as other verbs' forms;
- every noun, adjective, number and verb gets its regular forms (paradigms): the cases with their vowel harmony and the
  fleeting vowel (аймаг, аймгийн; ажил, ажлын), the plural and its cases, the reflexive possessive (гэртээ, ажлаа), and a
  verb's tenses, participles and converbs (явсан, явж, яваад, явна, явав, явлаа, явжээ, явдаг, явбал, явахад, яваагүй) and its
  agent noun's cases unless that noun is a word itself (хаагчид of хаах; уншигч keeps its own);
  a form that is a word itself is left to that word;
- the harmonic variants of the little words are one word (уу, үү, юү; вэ, бэ; даа, дээ, доо, дөө; руу, рүү, лүү), юу
  "what" is a word of its own, not уу's variant, and a number's attributive form is the number's (гурван of гурав);
- names are no words: those the texts capitalise inside a sentence (Стокгольм, Гачуурт) and those the pack glosses only as
  a place, people, god, title or book (хурмаст, ринбүчи, данжуур);
- an adjective or adverb spelled as a verb's participle or converb is its form (хуурсан of хуурах, бололгүй of болох).
"""
import io
import json
import os
import re
import sqlite3
import tarfile
from collections import defaultdict

LETTERS = re.compile(r"[а-яёөү]+")
# The entries' parts of speech that are words of the list (as WORD_POS, with postpositions, which are prepositions here).
_KINDS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "intj", "particle", "postp"}
# A gloss naming another word's form: "past participle in -сан (-san) of авах", "genitive singular of жил", "front vowel
# harmonic form of уу", "dative-locative of гар".
_FORM_GLOSS = re.compile(
    r"(tense|participle|converb|cvb|supine|indicative|case|plural|singular|genitive|accusative|dative|ablative|instrumental|comitative|"
    r"directive|possessive|harmonic form|conditioned form|mood|imperative|terminative|concessive|precative|optative)\b"
    r".*?\bof ([а-яёөү]+)", re.I)
_LETTER_NAME = re.compile(r"name of the Cyrillic script letter|letter of the Mongolian alphabet")
# A gloss naming a voice ("causative voice in -уулах of цуглах"): the verb is a word of its own.
_VOICE_GLOSS = re.compile(r"\bvoice\b", re.I)
# Entries glossed as another word's form that are words in use, kept as words.
OWN = {
    "болон", "хүртэл", "дахин", "харин", "удаа", "оноо", "хулгана", "шалтаг", "мохоо", "яаж", "эд", "ерөнхийлөгч",
    "баярлалаа", "өршөөгөөрэй", "ард", "юу",
    # Genitives that are words of their own: "most", "fine", "the given", "last, recent", "first".
    "хамгийн", "нарийн", "тухайн", "сүүлийн", "эхний",
}
# Words the pack gives a sense of their own that are another word's form in texts: the quotative гэж "that" and гэдэг
# "called" are гэх's, ирсэн "inbound" ирэх's.
FOLDED = {"гэж": "гэх", "гэдэг": "гэх", "гэсэн": "гэх", "ирсэн": "ирэх"}
# A noun's genitive endings.
_GENITIVES = ("ын", "ийн", "ны", "ний", "айн", "ойн", "уйн", "үйн", "эйн")
# The independent genitive's collective form is a word of its own ("family", "classmates"): гэрийнхэн, ангийнхан.
_COLLECTIVE = ("нхан", "нхэн", "нхон", "нхөн")
# Common words no pack has, with their kinds: the little words, and the words of the news and Wikipedia text that are
# most common without one (зөвхөн, хэрэв, ялангуяа, систем, хөгжих).
ADDED = {
    "гэвч": "conj", "гэхдээ": "conj", "гэтэл": "conj", "тэгвэл": "conj", "тэгэхээр": "conj", "тэгээд": "conj",
    "дагуу": "prep", "руу": "prep", "биз": "particle", "дэх": "prep", "дахь": "prep", "тань": "particle",
    # Adjectives spelled as another word's form: амьд "alive" (not амь's dative), явган "on foot".
    "амьд": "adj", "явган": "adj",
}
_ADDED_KINDS = {
    "det": "ийм энэхүү тэрхүү иймэрхүү",
    "pron": "тэдгээр",
    "conj": "боловч хэрэв хэрвээ хэдийгээр түүнчлэн төдийгүй байтугай юмуу",
    "prep": "улмаас турш даяар тутам хэрээр гадуур",
    "particle": "эсэх тусам билээ ажээ бололтой",
    "adv": "зөвхөн гаруй хэрхэн ингээд улмаар ийнхүү хэдий тухайлбал ихэвчлэн цаашид ялангуяа ихээхэн мэдээж иймд иймээс "
           "хэт хожим байнга жишээлбэл тун харьцангуй удалгүй хэр урьд цааш зайлшгүй ойролцоогоор нэгэнт ердөө тасралтгүй "
           "голдуу ганцхан сайтар нилээд нэлээн нэлээд дөнгөж түрүүнд төдий чухам хамтдаа даруй түлхүү хичнээн ядаж урагш "
           "хэтэрхий хэдхэн доош урьдчилан ингэж цаашлаад дараалан заавал тиймээс шууд",
    "adj": "ижил давуу сөрөг шуурхай онцгой нэмэлт тусгаар шударга тод ялгаатай чөлөөт идэвхтэй хангалттай ховор яруу "
           "өвөрмөц түгээмэл эерэг болзошгүй яаралтай харамсалтай төгөлдөр хүчирхэг онц ухаалаг нийтлэг гавьяат "
           "тохиромжтой ноцтой хамааралтай анхан жирийн найдвартай ердийн оновчтой хурц төгс хараат уугуул бүхэл "
           "шилдэг нэрэмжит эрчимтэй ойлгомжтой бүтээлч агуу цорын бодитой онлайн анхдагч эмзэг дотно цайвар төвөгтэй ширүүн "
           "дөрвөлжин уян дижитал тэнцүү таатай туйлын хавтгай хуурамч огцом хүндтэй органик хязгаарлагдмал үнэнч "
           "нэгдмэл эгэл хөндлөн өсвөр мундаг чадварлаг хүчингүй ойролцоо дараах түр үүдэлтэй удаан эхний дэд энгийн",
    "noun": "хөрөнгө хүчин ихэнх систем салбар зээл хяналт шинжилгээ нэгж онцлог эрчим төхөөрөмж техник цол бизнес "
            "хамтлаг дэмжлэг коронавирус суурин эрсдэл зөрчил медаль хөгжил зэрэгцээ эхэн стандарт хохирол өрх дүгнэлт "
            "хангамж судлаач хандлага зарчим чиглэл бүтээгдэхүүн процесс хүсэл материал үзүүлэлт гомдол осол дүн "
            "арилжаа доктор бүрэлдэхүүн сервер дипломат дийлэнх цуврал элс вэб ачаалал магадлал протокол шийдэл "
            "стратеги өсөлт сонголт багц үүсвэр үнэлгээ даралт заалт тооцоо сууц температур тэжээл нэвтрүүлэг логик "
            "төлөв ялгаа харьцаа хорио түрээс урам заавар үзлэг гамшиг "
            "профессор харьяат гоол бэрхшээл агуулга зүтгэлтэн мэдэл олонх суваг тээвэр оролдлого томуу эрхлэгч биет "
            "өмч ариутгал ажиглалт тархалт холбогдол төгсгөл өвчлөл эрэлт эргэлт зориулалт бүртгэл дэглэм хамгаалалт "
            "элемент тулгуур хувьцаа тайлан төрөлхтөн тэмдэглэл график дархлаа ертөнц хандив мастер тулаан мэдрэмж "
            "нийтлэл хэлэлцүүлэг цар чуулган тоног оруулалт авалт тандалт зохицуулалт хороолол зөрүү",
    "verb": "холбогдох хангах хэрэгжих гүйцэтгэх хандах дамжуулах боловсруулах буурах хөгжих сайжрах илрэх байрлах "
            "үүсэх үүсгэх шийдвэрлэх харьцуулах суурилах тусгаарлах тодруулах үргэлжлэх төлөвлөх шийдэх тодорхойлох "
            "зохицуулах нөлөөлөх хамаарах бүрдэх зориулах нэвтрэх шалгаруулах хянах багтах зөрчих чөлөөлөх шилжих "
            "тооцох анхаарах хорих хориглох биелэх эзэмших өрнөх нийцэх онцлох тараах зүтгэх хадгалах эмчлэх хамрах "
            "эдгэрэх буцаах авчрах үнэлэх ажиглах эрхлэх бүртгэх батлах ахлах анагаах зохих төгсөх ингэх шалтгаалах "
            "зохиох шалгарах төвлөрөх тэргүүлэх тогтох нүүх",
}
for _kind, _words in _ADDED_KINDS.items():
    ADDED.update({w: _kind for w in _words.split()})
# The harmonic variants of the little words, each the first word's.
VARIANTS = {"дугаар": ("дүгээр",), "уу": ("үү", "юү"), "вэ": ("бэ",), "даа": ("дээ", "доо", "дөө"), "руу": ("рүү", "лүү"), "дэх": ("дахь", "дэхь"),
            "эсвэл": ("эсхүл",)}
# The numbers and their attributive forms (гурван ном "three books").
NUMBERS = {
    "нэг": "нэгэн", "гурав": "гурван", "дөрөв": "дөрвөн", "тав": "таван", "зургаа": "зургаан", "долоо": "долоон",
    "найм": "найман", "ес": "есөн", "арав": "арван", "хорь": "хорин", "гуч": "гучин", "дөч": "дөчин", "тавь": "тавин",
    "жар": "жаран", "дал": "далан", "ная": "наян", "ер": "ерэн", "зуу": "зуун", "мянга": "мянган", "түм": "түмэн",
}
# Forms sent by hand: words a commoner word's form is spelled as (эмээ "grandmother", not эм's; өвөө "grandfather"), and
# the pack's wrong links (амаар is ам's, not унших's).
MONGOLIAN_OVERRIDES = {"эмээ": ["эмээ"], "өвөө": ["өвөө"], "амаар": ["ам"], "сайтай": ["сайн"], "дуун": ["дуу"], "хаанаас": ["хаана"]}
# The pack's entries of a form: бологчид "plural of бологч", хурууд "dried curds" (хуруу's plural), урид (урьд).
MONGOLIAN_OVERRIDES.update({"бологчид": ["болох"], "хурууд": ["хуруу"], "урид": ["урьд"]})
# Forms spelled as a rarer word's (авсан of авс "coffin", эрхийн of эрхий "thumb", монголчууд of монголч "Mongolist"): the common
# word's alone.
for _word, _forms in {
    "авах": "авсан", "давах": "давсан", "бодох": "бодсон", "гарах": "гарлаа", "эрх": "эрхийг эрхийн", "байгаль": "байгалийн",
    "ам": "амны", "дунд": "дундаа", "монгол": "монголчууд монголчуудын монголчуудыг", "будда": "буддын", "тахал": "тахлын",
    "мэдээ": "мэдээг", "засаг": "засгаа", "илүү": "илүүд", "хорио": "хориог", "тулах": "тулаад", "зураг": "зурган",
}.items():
    MONGOLIAN_OVERRIDES.update({_form: [_word] for _form in _forms.split()})
# And the irregular forms of pronouns and words without paradigms here (тэрээр "he", танд "to you", хоорондоо).
for _word, _forms in {
    "та": "танд таныг танаас тантай", "энэ": "үүнийгээ", "бид": "манайд манайхан",
    "өөрсдөө": "өөрсдийн өөрсдийг өөрсдөд өөрсдөөс", "бусад": "бусдад бусдын бусдыг бусдаас", "залуу": "залуус",
    "нөхөр": "нөхөд", "хооронд": "хоорондоо хоорондын хоорондох", "хамт": "хамтын", "гадна": "гадны гаднаас",
    "байнга": "байнгын", "холбоо": "холбоотойгоор", "мэт": "мэтээр мэтийн", "буй": "буйг буйгаа буйгаар буйд", "юу": "юуны юуг юунаас юугаар",
    "нэг": "нэгийг нэгийн нэгд нэгт нэгээс нэгээр нэгэнд нэгэндээ нэгнээ нэгнийг нэгний нэгнээсээ", "хэн": "хэнд хэнийг хэний хэнээс хэнтэй",
    "тэр": "тэрээр тэрбээр түүнийгээ тэрийг тэрэнд", "дараах": "дараахь дараахи", "ихэнх": "ихэнхи", "дийлэнх": "дийлэнхи",
    "тэдгээр": "тэдгээрийн тэдгээрийг тэдгээрт тэдгээрээс", "дор": "доорх доорхи", "ерөнхий": "ерөнхийдөө", "мод": "модон", "цас": "цасан", "элс": "элсэн",
    "дээр": "дээрх дээрхи", "тал": "талаарх талаархи", "дотор": "доторх", "хүртэл": "хүртэлх", "төлөө": "төлөөх", "руу": "руугаа рүүгээ",
    # The pronouns and particles that take cases (нийтийн "public", бүрийн "of each", багш нарын "the teachers'").
    "нийт": "нийтийн нийтэд нийтээр нийтдээ нийтийг нийтээрээ нийтээс", "бүр": "бүрийн бүрт бүрийг бүрд бүрээр бүрээс",
    "бүгд": "бүгдийг бүгдээрээ бүгдэд бүгдийн бүгдээр бүгдээс бүгдээ", "нар": "нарын нарт нараас нартай нарыг",
    "эсэх": "эсэхийг эсэхэд эсэхийн эсэхээс эсэхээр",
    # хувийн "private, personal" is хувь's, not хув "amber"'s.
    "хувь": "хувийн хувийг хувийнхаа хувийнх",
}.items():
    MONGOLIAN_OVERRIDES.update({_form: [_word] for _form in _forms.split()})
# A name's genitive the Russian pack has as an adjective ("European").
MONGOLIAN_EXCLUDE = {"европын"}
# And the rare words and loans past the courses' first 3,800 (the Russian pack's words of other packs' forms, Russian
# words in names) whose counts are other words' (ониг "joke" of онгон's онгууд, анга of анги's ангийнх, ури of урид, даж
# of дайн дажин, лис of лист, щит).
MONGOLIAN_EXCLUDE |= {"ониг", "анга", "ури", "даж", "лис", "щит", "түргэ"}
# And names the rules below cannot tell, seen too seldom: рах (the demon Rahu), хуйхуй "Hui people", цэдэнбализм.
MONGOLIAN_EXCLUDE |= {"рах", "хуйхуй", "цэдэнбализм"}
# Nouns in use spelled as a verb's stem or form, the verb's in the pack (ор "bed", ор "enter!"); the rarer ones (ав "hunt",
# ир "edge", алх "hammer") are left to the verb, as their forms are mostly its (аваас, ирээ).
SHARED_WITH_VERBS = {"ор", "хэл", "яриа", "идээ", "уншигч", "торго"}

# The corpora the list is counted from, as the builder caches them: how they write a word tells a name (Стокгольм,
# Женова, Гачуурт), which the texts capitalise inside a sentence with all its forms, from a word, which they mostly
# write in lower case (монгол is "Mongolian" as often as Монгол Улс).
CORPORA = ("mon_wikipedia_2021_100K", "mon_news_2020_100K")
NAME_SHARE = 0.9
_CAPITALISED = re.compile(r"[А-ЯЁӨҮа-яёөү]+")
# A word after these (or first in a sentence) is capitalised as a sentence's start, a quote's or a heading's.
_STARTS = ".!?:;«»\"“”„'‘’()—–-"
# The English pack's names of peoples, places, gods, titles and books, glossed only capitalised: "Khormusta (king of the
# gods, Indra)", "Rinpoche (title of a lama)", "Tengyur (Buddhist canon)", "Jangar (epic)", "Kidan (people or person)".
_NAME_GLOSS = re.compile(r"^[A-Z][^(]*\((?:[^)]*\W)?(?:city|town|river|lake|mountains?|monastery|epic|canon|title|"
                         r"gods?|deity|people|tribe|ethnic group|khaganate)\b")

# A verb's past participles and its converb "without", which the packs gloss as adjectives and adverbs too ("roasted",
# "not placed", бололгүй "without becoming").
_PARTICIPLES = ("сан", "сэн", "сон", "сөн", "аагүй", "ээгүй", "оогүй", "өөгүй", "лгүй")

_VOWELS = "аоуэөүияеёюы"
_LONG = ("аа", "оо", "ээ", "өө", "уу", "үү", "ай", "ой", "уй", "үй", "эй", "ий", "ии", "иа", "яа", "ёо", "еэ", "юу", "юү")


def harmony(word):
    """
    The vowel a word's suffixes take: а or э by its first vowel other than и, rounded (о, ө) where all its vowels but и
    are о or ө (болох, болсон; but оруулах, оруулсан; өгүүлэх, өгүүлсэн).
    """
    vowels = [c for c in word if c in "аоуэөүяёеюы"]
    if not vowels:
        return "э"
    if all(c in "оё" for c in vowels):
        return "о"
    if all(c == "ө" for c in vowels):
        return "ө"
    return "а" if vowels[0] in "аоуяёыю" else "э"


def _endings(a):
    """The harmonic pieces for the vowel [a]: the vowel, its long form, the matching у or ү, the comitative's vowel."""
    u = "у" if a in "ао" else "ү"
    tai = {"а": "тай", "о": "той", "э": "тэй", "ө": "тэй"}[a]
    return a, a + a, u, tai


def _is_long(word):
    return word.endswith(_LONG)


def _fleeting(word):
    """The stem with its last short vowel dropped before a vowel ending (аймаг → аймг, ажил → ажл), or None."""
    if len(word) < 4 or word[-1] in _VOWELS or word[-2] not in "аоэөиуү" or word[-3] in _VOWELS:
        return None
    if not any(c in _VOWELS for c in word[:-3]):
        return None
    return word[:-2] + word[-1]


def noun_forms(word, noun=True):
    """
    A noun's (or adjective's, number's, adverb's, postposition's) regular case forms and reflexive possessive, and a
    noun's plural, as a map from each form to whether it is made from the whole word with a plain ending (гэрт,
    гэрийн) rather than from a changed stem or with a rarer ending (ажлын, сангийн, зүйлс, хананд).
    """
    a, aa, u, tai = _endings(harmony(word))
    out = {}

    def add(plain, *forms, reflexive=False):
        for f in forms:
            for g in (f, f + aa) if reflexive else (f,):
                out[g] = out.get(g, False) or plain

    if word.endswith("е"):
        # үе, бие: үеийн, үед, үеэс, үеэр.
        e = "э" if a in "эө" else a
        add(True, word + "ийн", word + "ийг", word + "ийгээ", word + "ийнхээ", word + e, word + "гүй", word + tai,
            word + tai + "г" + aa, word + tai + "г" + aa + "р")
        add(True, word + "д", word + e + "с", word + e + "р", reflexive=True)
        plurals = {word + u + u + "д": True}
    elif word.endswith(("и", "ь")):
        stem = word[:-1] + "и"
        plain = word.endswith("и")
        add(plain, stem + "йн", stem + "йг", stem + "йг" + aa, stem + "йнх" + aa, stem + a)
        add(plain, stem + "д", stem + a + "с", stem + a + "р", reflexive=True)
        add(True, word + "д", reflexive=True)
        add(True, word + tai, word + tai + "г" + aa, word + tai + "г" + aa + "р", word + "гүй")
        plurals = {stem + u + "д": plain}
    elif _is_long(word):
        add(True, word + "гийн", word + "г", word + "г" + aa, word + "н", word + tai, word + tai + "г" + aa,
            word + tai + "г" + aa + "р", word + "гүй")
        add(True, word + "д", word + "г" + aa + "с", word + "г" + aa + "р", reflexive=True)
        add(False, word + "ны", word + "ний")
        add(False, word + "нд", word + "н" + aa + "с", reflexive=True)
        plurals = {word + "н" + u + u + "д": True, word + "д": False}
    else:
        if word[-1] in _VOWELS:
            # аяга: аягын, аягыг, аягаас; аяганы, аяганд.
            stems, plain = [word[:-1]], False
        else:
            stems, plain = [word], True
            if _fleeting(word):
                stems.append(_fleeting(word))
        for i, s in enumerate(stems):
            whole = plain and i == 0
            add(whole, s + "ын", s + "ийн", s + "ыг", s + "ийг", s + "ыг" + aa, s + "ийг" + aa, s + "ынх" + aa,
                s + "ийнх" + aa, s + aa)
            # The independent genitive (өөрийнх "one's own").
            add(False, s + "ынх", s + "ийнх")
            add(whole, s + aa + "с", s + aa + "р", reflexive=True)
            if i:
                add(False, s + a + "д", reflexive=True)
        add(True, word + tai, word + tai + "г" + aa, word + tai + "г" + aa + "р", word + "гүй")
        add(True, word + "д", word + "т", reflexive=True)
        if word.endswith("н"):
            # A word in н takes the genitive's bare vowel (машины, гишүүний), on the stem without its fleeting vowel too
            # (өвчний, орчны, олны).
            gen = "ы" if a in "ао" else "ий"
            add(True, word + gen)
            if _fleeting(word):
                add(False, _fleeting(word) + gen)
            # And the hidden н's accusative with the reflexive (цалингаа).
            if noun:
                add(False, word + "г" + aa)
        if _fleeting(word):
            # The hidden н after the stem without its fleeting vowel (үндэс, үндсэнд, үндсэндээ); not its bare form, which is
            # mostly a verb's past (орос, орсон of орох).
            fs = _fleeting(word)
            add(False, fs + a + "н" + aa + "с")
            add(False, fs + a + "нд", reflexive=True)
        if word[-1] in "жчш":
            # After ж, ч and ш the dative's vowel is и (туршид).
            add(True, word + "ид", reflexive=True)
        if word[-1] in "жчшк":
            # And the hidden н's (ээлжинд, гудамжинд, банкинд).
            add(False, word + "инд", reflexive=True)
        # The hidden н (ус, усны, усанд) and, after a consonant, the dative's vowel (хот, хотод).
        add(False, word + "ны", word + "ний", word + "ныг", word + "нийг")
        add(False, word + "нд", word + "н" + aa + "с", reflexive=True)
        if word[-1] not in _VOWELS:
            # After a cluster or a dental or sibilant the dative's vowel is the rule (улсад, хотод, ажилд beside).
            add(word[-1] in "тдсзжчш" or word[-2] not in _VOWELS, word + a + "д", reflexive=True)
            add(False, word + a + "нд", reflexive=True)
        if noun and word.endswith("н"):
            # сан, сангийн, санг; байшин, байшингийн.
            add(False, word + "гийн", word + "г", word + "гийг")
            add(False, word + "г" + aa + "с", word + "г" + aa + "р", reflexive=True)
        plurals = {s + u + u + "д": i == 0 and plain for i, s in enumerate(stems)}
        plurals[word + "н" + u + u + "д"] = False
        # гишүүн, гишүүд; эрдэмтэн, эрдэмтэд; хэрэглэгч, хэрэглэгчид, хэрэглэгчдийн; зүйлс, үгс, нэрс.
        if word.endswith("н") and len(word) > 3:
            plurals[word[:-1] + "д"] = False
        if word.endswith("ч"):
            add(False, word + "ид")
            plurals[word + "д"] = False
        if word[-1] in "лнрм":
            plurals[word + "с"] = False
    # A plural in -д drops its last vowel before a vowel ending (эрдэмтэд, эрдэмтдийн; ажилчид, ажилчдын).
    for p in list(plurals) if noun else ():
        if p.endswith("д") and _fleeting(p) and _fleeting(p) not in plurals:
            add(False, *(_fleeting(p) + g for g in ("ын", "ийн", "ыг", "ийг", a + "д", aa + "с", aa + "р")))
    for p, plain in plurals.items() if noun else ():
        # The plural's endings take its own vowel (онууд, онуудад; хотууд, хотуудаас).
        pa, paa, _, ptai = _endings(harmony(p))
        add(plain, p, p + "ын", p + "ийн", p + "ыг", p + "ийг", p + "ынх" + paa, p + "ийнх" + paa, p + ptai,
            p + ptai + "г" + paa, p + paa)
        add(plain, p + pa + "д", p + paa + "с", p + paa + "р", reflexive=True)
        if p.endswith("д"):
            # иргэддээ, хүүхдүүддээ.
            add(False, p + "д" + paa)
    out.pop(word, None)
    return out


def verb_stems(verb):
    """
    A verb's stems: before a consonant ending, and the stem a vowel ending is joined to with the verb's vowel (яв, ява;
    ир, ирэ), or for a stem ending in a long vowel or diphthong the stem itself (бай, хий, суу), and the vowel.
    """
    base = verb[:-1]
    a = harmony(base)
    if _is_long(base) or base == "гэ":
        return [base], base, a
    # A stem ending in я, е, ё or ю ends in its vowel's й (уях: уясан, уяж).
    if base[-1] not in _VOWELS or base[-1] in "яеёю":
        return [base], base, a
    stem = base[:-1]
    # A stem may keep its vowel (унасан, ажилласан, ашиглаж).
    consonant = [stem, base]
    if base[-1] == "и" and stem[-1] not in "жчшй":
        # ярих: ярьсан, ярьж, яриад, never ярсан (барих: барьсан, not барах's барсан); but очих: очсон, очиж.
        consonant = [stem + "ь", base]
    if len(stem) > 2 and stem[-1] not in _VOWELS and stem[-2] not in _VOWELS:
        # A cluster may take a vowel inside it (эхэлсэн of эхлэх, өөрчилж of өөрчлөх, авчирсан of авчрах).
        consonant += [stem[:-1] + a + stem[-1], stem[:-1] + "и" + stem[-1]]
    return consonant, base, a


def verb_forms(verb, agents=True):
    """
    A verb's regular forms: tenses, participles and their cases, converbs, negatives, the voluntative and requests, and
    unless [agents] is false the agent noun's (хаагч, хаагчид).
    """
    consonant, joined, a = verb_stems(verb)
    _, aa, u, tai = _endings(a)
    out = set()
    for s in consonant:
        san = s + "с" + a + "н"
        out.update({san, san + ("ы" if a in "ао" else "ий"), san + "гүй", san + "д", san + tai, san + "д" + aa, s + "сн" + aa, s + "сн" + "ы", s + "сн" + "ий", s + "сн" + "ыг",
                    s + "сн" + "ийг", s + "сн" + aa + "с", s + "сн" + aa + "р", s + "сн" + aa + "с" + aa})
        out.update({s + "ж", s + "ч", s + "жээ", s + "чээ", s + "н" + a, s + "д" + a + "г", s + "д" + a + "ггүй",
                    s + "дг" + aa + "с", s + "дг" + aa + "р", s + "дг" + aa, s + "дгийг", s + "дгийн", s + "дг" + "ыг",
                    s + "дг" + "ын", s + "д" + a + "гт", s + "д" + a + "г" + tai,
                    s + "л" + aa, s + "в" + a + "л", s + "б" + a + "л", s + "т" + a + "л", s + "м" + aa + "р",
                    s + "с" + aa + "р", s + "ъя", s + "ъё", s + "ье"})
    # The endings in the verb's vowel: after a long stem as they are (байх, байгаад), else on the vowel (явах, яваад).
    if _is_long(joined) or joined == "гэ":
        x, g = joined, (joined + "г" if _is_long(joined) else joined[:-1] if joined == "гэ" else joined)
        long_part = [g + aa]
        out.update({x + "в", x + "я", x + "е", x + "ё", x + "н"})
    elif joined[-1] in "яеёю":
        # хаях: хаяв, хаяад, хаяагүй, хаягч.
        x = joined
        long_part = [joined + a]
        out.update({x + "в", x + "н"})
    else:
        stem = joined[:-1] if joined[-1] in _VOWELS else joined
        x = stem + (joined[-1] if joined[-1] == "и" else a)
        # ярих: яриад; but очих, унших: очоод, уншаад.
        long_part = [x + a] if joined[-1] == "и" and stem[-1] not in "жчшй" else [stem + aa]
        out.update({x + "в", x + "н"})
    for v in long_part:
        # The polite request (өгөөч, яваач).
        out.add(v + "ч")
        out.update({v + "д", v + "гүй", v, v + "р" + a + "й", v + "нгуут", v + "нгүүт", v + "г", v + "г" + aa,
                    v + "гаар" if a in "ао" else v + "гээр", v + "тай" if a == "а" else v + tai})
    out.update({x + "х", x + "х" + a + "д", x + "х" + aa + "р", x + "хгүй", x + "хыг", x + "хийг", x + "хын", x + "хийн",
                x + "х" + aa + "с", x + "х" + tai, x + "х" + a + "д" + aa, x + "х" + aa + "с" + aa,
                x + "хд" + aa, x + "х" + aa + "р" + aa})
    # The converb "without" (бололгүй, явалгүй, хийлгүй).
    out.add(x + "лгүй")
    # A stem in a sibilant keeps its vowel before -ж (үзэж, хүсэж).
    out.update({joined + "ж", joined + "жээ"})
    # The agent noun in -гч and its cases and plural (хаагч, хаагчид, хаагчдын).
    g = x + "гч"
    out.update(set() if not agents else {g, g + "ийн", g + "ийг", g + "ид", g + aa + "р", g + aa + "с", g + tai, g + "дын", g + "дийн", g + "дыг", g + "дийг",
                g + "д" + a + "д", g + "д" + aa + "с", g + "д" + aa + "р", g + "ид" + aa})
    out.discard(verb)
    return {f for f in out if LETTERS.fullmatch(f)}


def derived_verbs(verb):
    """The causative and passive verbs regularly made from a verb (оруулах of орох, байгуулагдах of байгуулах)."""
    consonant, joined, a = verb_stems(verb)
    stem = consonant[0]
    if joined[-1] in _VOWELS and _is_long(joined):
        stem = joined
    u, after = ("уу", "а") if a in "ао" else ("үү", "э")
    out = {stem + u + "л" + after + "х", stem + "лг" + a + "х", stem + "г" + a + "х", joined + "гд" + a + "х", stem + "гд" + a + "х"}
    # And the spoken completive in -чих, whose stem keeps its х (болчих, болчихсон, болчихоод; гаргачихсан, суучихсан).
    out.update(s + "чих" + a + "х" for s in set(consonant) | {joined})
    out.discard(verb)
    return out


def _other_packs(dictionaries):
    """The Russian and Chinese packs' words, with their kinds, that are single Cyrillic words."""
    found = defaultdict(set)
    for name in ("mn-ru", "mn-zh"):
        path = os.path.join(dictionaries, f"{name}.sqlite")
        if not os.path.exists(path):
            continue
        db = sqlite3.connect(path)
        for word_lc, p in db.execute("SELECT word_lc, pos FROM entries"):
            if p in _KINDS and LETTERS.fullmatch(word_lc):
                found[word_lc].add("prep" if p == "postp" else p)
        db.close()
    return found


def _russian_glosses(dictionaries):
    """The Russian pack's glosses of its words' entries, by word."""
    found = defaultdict(list)
    path = os.path.join(dictionaries, "mn-ru.sqlite")
    if os.path.exists(path):
        db = sqlite3.connect(path)
        for word_lc, p, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
            if p in _KINDS:
                found[word_lc] += [g for s in json.loads(senses) for g in s.get("glosses", [])]
        db.close()
    return found


def agent_noun(verb):
    """A verb's agent noun in -гч (хаагч of хаах, уншигч of унших)."""
    return {f for f in verb_forms(verb) if f.endswith("гч")}


def _regular_forms(word, kinds, agents=True):
    """A word's regular forms, each with whether it is made from the whole word with a plain ending."""
    made = {}
    if "verb" in kinds and word.endswith("х"):
        made.update({f: False for f in verb_forms(word, agents)})
    if kinds & {"noun", "adj", "num", "adv", "prep"} and not kinds & {"particle", "conj", "pron", "det"} and (
            word not in VARIANTS):
        for f, plain in noun_forms(word, "noun" in kinds).items():
            made[f] = made.get(f, False) or plain
    return made


def capitals(dictionaries):
    """
    How often the corpora write each word inside a sentence, and how often capitalised there (not first, nor after a
    full stop, colon, quote, bracket or dash), by its lower case.
    """
    seen, capitalised = defaultdict(int), defaultdict(int)
    for corpus in CORPORA:
        path = os.path.join(dictionaries, "frequency-sources", f"leipzig-{corpus}.tar.gz")
        if not os.path.exists(path):
            continue
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                sentence = line.rstrip("\n").partition("\t")[2]
                for match in _CAPITALISED.finditer(sentence):
                    before = sentence[:match.start()].rstrip()
                    if not before or before[-1] in _STARTS:
                        continue
                    word = match.group()
                    seen[word.lower()] += 1
                    if word[0].isupper():
                        capitalised[word.lower()] += 1
    return seen, capitalised


def load(db, pos, lemmas, common, dictionaries):
    """Adds the words and forms above to the English pack's [pos], [lemmas] and [common], as load_dictionary has them."""
    # Entries glossed as another word's form are that word's.
    forms_of = defaultdict(set)
    voiced = set()
    glossed = defaultdict(list)
    for word_lc, p, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if not LETTERS.fullmatch(word_lc):
            continue
        glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])]
        if p in _KINDS:
            glossed[word_lc] += glosses
        if p == "verb" and any(_VOICE_GLOSS.search(g) for g in glosses):
            voiced.add(word_lc)
            continue
        for gloss in glosses:
            of = _FORM_GLOSS.search(gloss)
            if of and of.group(2) != word_lc:
                forms_of[word_lc].add(of.group(2))
    # The letters' names (ша, дэ, эн) are no words.
    letters = set()
    for word_lc, senses in db.execute("SELECT word_lc, senses FROM entries WHERE pos = 'noun'"):
        if all(_LETTER_NAME.search(g) for s in json.loads(senses) for g in s.get("glosses", [])):
            letters.add(word_lc)
            pos[word_lc].discard("noun")
            if not pos[word_lc] & _KINDS:
                common.pop(word_lc, None)
    for word, target in FOLDED.items():
        forms_of[word].add(target)
    others = _other_packs(dictionaries)
    for word, targets in forms_of.items():
        if word in OWN or word.endswith(_COLLECTIVE) or word in voiced or not any(pos.get(t) or t in others for t in targets):
            continue
        common.pop(word, None)
        lemmas[word] = sorted(set(lemmas.get(word, [])) | targets)
    for word, kind in ADDED.items():
        pos[word].add(kind)
        common[word] = word
    fixed = set()
    for number, attributive in NUMBERS.items():
        pos[number].add("num")
        common.setdefault(number, number)
        common.pop(attributive, None)
        lemmas[attributive] = [number]
        fixed.add(attributive)
    for word, variants in VARIANTS.items():
        common.setdefault(word, word)
        lemmas[word] = []
        fixed.add(word)
        for variant in variants:
            common.pop(variant, None)
            lemmas[variant] = [word]
            fixed.add(variant)
    for word in OWN:
        lemmas[word] = []

    def own_word(w):
        return w in common and bool(pos.get(w, set()) & _KINDS)

    def unfold():
        """
        A verb is no other verb's form (олгох and олдох of олох, үзүүлэх of үзэх, хэмээх of гэх), and a noun no other
        noun's (гэрээ "contract", not гэр's; эмээ "grandmother", not эм's).
        """
        for form in list(lemmas):
            if not own_word(form) or not lemmas[form]:
                continue
            kinds = pos[form]
            lemmas[form] = [l for l in lemmas[form] if not (
                "verb" in kinds and form.endswith("х") and "verb" in pos.get(l, set())
                or kinds & {"noun", "adj"} and not pos.get(l, set()) & {"verb", "pron", "num"})]
            # A noun spelled as a verb's stem or form (ор "bed" and ор "enter!", хэл, яриа) shares its spelling with the verb
            # and keeps its own forms (орны, оронд are ор's and орон's, not орох's): by hand, as the general rule gives the
            # spelling, and with it the noun's forms, to the commoner verb.
            if form in SHARED_WITH_VERBS and form not in MONGOLIAN_OVERRIDES:
                MONGOLIAN_OVERRIDES[form] = [form] + [l for l in lemmas[form] if l != form]

    unfold()
    # Names are no words, nor their forms any word's: those the pack glosses as a people, place, god, title or book
    # alone, and those the texts capitalise inside a sentence with their forms (as their stems are: Стокгольмд,
    # Женовагийн).
    seen, capitalised = capitals(dictionaries)
    russian = _russian_glosses(dictionaries)
    kept = OWN | set(ADDED) | set(NUMBERS) | set(VARIANTS)

    def is_name(word, kinds):
        if word in kept:
            return False
        glosses = glossed.get(word, [])
        if glosses and all(g[:1].isupper() for g in glosses) and any(_NAME_GLOSS.search(g) for g in glosses):
            return True
        forms = {word} | set(_regular_forms(word, kinds))
        caps = sum(capitalised.get(f, 0) for f in forms)
        inside = sum(seen.get(f, 0) for f in forms)
        if caps >= 2 and caps >= NAME_SHARE * inside:
            return True
        # And one the texts have too seldom to tell, which both packs gloss only capitalised (Ереван, Мальмё, Дворжак)
        # and the texts never write in lower case inside a sentence.
        every = glosses + russian.get(word, [])
        return bool(every) and all(g[:1].isupper() for g in every) and caps == inside
    for word in [w for w in common if LETTERS.fullmatch(w) and is_name(w, pos.get(w, set()))]:
        common.pop(word)
    # Every word's regular forms that are no word themselves; and a causative or passive verb no pack has is its verb's
    # (оруулсан, байгуулагдсан).
    generated = defaultdict(set)
    plain = defaultdict(set)

    def agents(verb):
        """Whether a verb's agent noun is its (хаагч of хаах), not a word itself (уншигч, тоглогч) that keeps its forms."""
        return "verb" not in pos.get(verb, set()) or not any(own_word(g) or "noun" in others.get(g, ()) for g in agent_noun(verb))

    def derive(verb):
        for derived in derived_verbs(verb):
            if derived not in pos and derived not in others:
                generated[derived].add(verb)
                for form in verb_forms(derived):
                    generated[form].add(verb)
                if derived[:-2].endswith("чих"):
                    generated[derived[:-2]].add(verb)
    for word in [w for w in common if LETTERS.fullmatch(w) and len(w) > 1]:
        for form, whole in _regular_forms(word, pos.get(word, set()), agents(word)).items():
            generated[form].add(word)
            if whole:
                plain[form].add(word)
        if "verb" in pos[word] and word.endswith("х"):
            derive(word)
    # But an adjective spelled as a noun's genitive is that genitive (сарын "monthly" of сар, төрийн of төр, дэлхийн).
    for word in [w for w in common if w in generated and w.endswith(_GENITIVES) and w not in OWN]:
        if pos[word] & _KINDS <= {"noun", "adj"} and any(pos.get(n, set()) & {"noun"} for n in generated[word]):
            common.pop(word)
            lemmas[word] = sorted(n for n in generated[word] if "noun" in pos.get(n, set()))
    # And an adjective or adverb spelled as a verb's past participle or converb "without" is that form (хуурсан
    # "roasted" of хуурах, тавигдаагүй, бололгүй), not one spelled as another form (түргэн "quick", бүтэн "whole"); and
    # a noun spelled as a noun's case form and glossed as that noun is, that case form (араатны "wild beast; predator"
    # of араатан "beast; predator").
    for word in [w for w in common if w in generated and w not in kept]:
        kinds = pos[word] & _KINDS
        if kinds in ({"adj"}, {"adv"}) and word.endswith(_PARTICIPLES):
            of = sorted(v for v in generated[word] if "verb" in pos.get(v, set()) and v in common)
        elif kinds == {"noun"}:
            of = sorted(n for n in generated[word] if n != word and "noun" in pos.get(n, set()) and n in common
                        and word in noun_forms(n) and set(glossed.get(n, ())) & set(glossed.get(word, ())))
        else:
            continue
        if of:
            common.pop(word)
            lemmas[word] = of
    # The other packs' words the English one lacks, but not their verb forms (the Russian pack's хийж "делая", байжээ) and
    # adjectives that are a word's case form (улсын "государственный").
    added = []
    # A noun only they have that is spelled as a verb's stem, its imperative (орш "profit" and орш "dwell!", бүрх, зод),
    # is left to the verb, whose forms its own would take (оршийн, бүрхэд).
    stems = {verb_stems(v)[0][0] for v in common if "verb" in pos.get(v, set()) and v.endswith("х")}
    for word, kinds in others.items():
        if pos.get(word, set()) & _KINDS or word in OWN or word in letters:
            continue
        if "verb" in kinds and not word.endswith("х"):
            kinds = kinds - {"verb"}
        if not kinds or word in generated and not kinds & {"noun"} or "verb" not in kinds and word in stems or (
                is_name(word, kinds)):
            continue
        pos[word] |= kinds
        common[word] = word
        added.append(word)
    # The other packs' verbs the English pack files as a verb's forms (хүргэх of хүрэх) are words too.
    unfold()
    for word in added:
        for form, whole in _regular_forms(word, pos[word], agents(word)).items():
            generated[form].add(word)
            if whole:
                plain[form].add(word)
        if "verb" in pos[word] and word.endswith("х"):
            derive(word)
    for form, words in generated.items():
        if own_word(form) or form in fixed:
            continue
        # A form made from a whole word with a plain ending (баг + ийн) is that word's, not one made from a changed stem or
        # with a rarer ending (бага, баг-ийн; хан, хан-анд beside хана-нд).
        have = lemmas.get(form, [])
        lemmas[form] = have + sorted((plain.get(form) or words) - set(have))
