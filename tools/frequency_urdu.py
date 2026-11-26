"""
Urdu for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

The reader (UrduParser) matches a word in Urdu letters without marks: ك as ک, ي and ى as ی, the Arabic ه, ە and ۀ as ہ,
ۂ as ہ, ۓ as ئے, without its vowel signs, tatweel and non-joiner. ھ (do-chashmi he, which writes aspiration: بھی, تھا), ہ, ے,
ں, ٹ, ڈ and ڑ are letters of their own and stay. So do these: the pack is written so, and the list counts words so.
Keep urdu_fold and UrduParser.fold the same.
"""
import json
import re
import tarfile
from collections import defaultdict

ZWNJ = "‌"
# Long vowels and hamza seats written as a letter and a combining sign, and the letters with hamza Urdu writes otherwise.
_COMPOSED = (
    ("آ", "آ"), ("أ", "أ"), ("إ", "إ"), ("ؤ", "ؤ"),
    ("یٔ", "ئ"), ("ئ", "ئ"), ("ىٔ", "ئ"),
    ("ۓ", "ئے"), ("ۓ", "ئے"),
    ("ۂ", "ہ"), ("ۂ", "ہ"), ("ۀ", "ہ"),
)
_LETTERS = str.maketrans({"ك": "ک", "ي": "ی", "ى": "ی", "ه": "ہ", "ە": "ہ"})
# The vowel signs, shadda, sukun, hamza and noon ghunna marks, superscript alef, tatweel and the non-joiner.
_MARKS = re.compile("[ً-ٰٟـ‌]")
# The letters of an Urdu word as the reader reads it (UrduParser's word characters, folded).
LETTERS = re.compile("[ء-غف-يٹ-ۓ]+")


def urdu_fold(word):
    """A word as the reader matches it (UrduParser.fold)."""
    for spread, composed in _COMPOSED:
        word = word.replace(spread, composed)
    return _MARKS.sub("", word.translate(_LETTERS))


# The pronouns a verb's table names beside its forms, untagged or tagged with their person (میں of ہونا).
TABLE_PRONOUNS = {"میں", "تو", "وہ", "یہ", "ہم", "تم", "آپ"}


# The verbs whose perfective, subjunctive and imperatives no rule makes (کیا of کرنا, گیا of جانا, دیجیے of دینا), in place
# of the regular ones.
_IRREGULAR_VERBS = {
    "کرنا": "کیا کیے کئے کی کیں کروں کرے کریں کرو کیجیے کیجئے",
    "جانا": "گیا گئے گئی گئیں جاؤں جائے جائیں جاؤ جائیے",
    "دینا": "دیا دیے دئے دی دیں دوں دے دو دیجیے دیجئے",
    "لینا": "لیا لیے لئے لی لیں لوں لے لو لیجیے لیجئے",
    "ہونا": "ہوا ہوئے ہوئی ہوئیں ہوں ہو",
}


def verb_forms(verb):
    """
    A verb's forms by the regular rules, as texts write them apart from their auxiliaries: the stem (کھا), infinitive (کھانے,
    کھانی), habitual participle (کھاتا, کھاتے, کھاتی, کھاتیں), perfective (کھایا, کھائے, کھائی, کھائیں; بیٹھا, بیٹھے,
    بیٹھی, بیٹھیں), subjunctive and imperatives (کھاؤں, کھائیں, کھاؤ, کھائیے; بیٹھوں, بیٹھو, بیٹھیے, بیٹھئے).
    """
    if not verb.endswith("نا") or " " in verb or len(verb) < 3:
        return []
    s = verb[:-2]
    out = [s, s + "نا", s + "نے", s + "نی", s + "تا", s + "تے", s + "تی", s + "تیں"]
    if verb in _IRREGULAR_VERBS:
        out += _IRREGULAR_VERBS[verb].split()
    elif s[-1] in "او":
        out += [s + "یا", s + "ئے", s + "ئی", s + "ئیں", s + "ؤں", s + "ؤ", s + "ئیے", s + "ئیو"]
    elif s[-1] == "ی":
        out += [s + "ا", s + "ے", s[:-1] + "ئے", s + "ں", s + "وں", s + "و", s + "جیے", s[:-1] + "ئیے"]
    else:
        out += [s + "ا", s + "ے", s + "ی", s + "یں", s + "وں", s + "و", s + "یے", s + "ئے", s + "ئیے"]
    return list(dict.fromkeys(f for f in out if f != verb))


def adjective_forms(adjective):
    """An adjective in -ا's forms (اچھے, اچھی; نئے, نئی of نیا)."""
    if not adjective.endswith("ا") or " " in adjective or len(adjective) < 2:
        return []
    s = adjective[:-1]
    if s.endswith("ی") and len(s) > 1:
        return [s[:-1] + "ئے", s[:-1] + "ئی"]
    return [s + "ے", s + "ی"]


def noun_forms(noun, gender):
    """
    A noun's plural and oblique forms by its gender: masculine in -ا or -ہ (لڑکے, لڑکوں; علاقے, علاقوں), in -و (آلوؤں) and
    others (گھروں), feminine in -ی (لڑکیاں, لڑکیوں), in -یا (گڑیاں, گڑیوں), in -ا or -و (دعائیں, دعاؤں) and others
    (باتیں, باتوں); of a noun whose gender is unknown, only the oblique plural of one in neither -ا nor -ہ.
    """
    if " " in noun or len(noun) < 2:
        return []
    last = noun[-1]
    if gender == "feminine":
        if last == "ی":
            return [noun + "اں", noun + "وں"]
        if noun.endswith("یا"):
            return [noun + "ں", noun[:-1] + "وں"]
        if last in "او":
            return [noun + "ئیں", noun + "ؤں"]
        return [noun + "یں", noun + "وں"]
    if gender == "masculine" and last in "اہ":
        return [noun[:-1] + "ے", noun[:-1] + "وں"]
    if last in "اہ":
        return []
    if last == "و":
        return [noun + "ؤں"]
    return [noun + "وں"]


# The list's corrections, as frequency_fixes.OVERRIDES and EXCLUDE have them, after Hindi's (frequency_fixes): the copula ہے
# with ہیں and ہوں, as تھا with تھی, تھے, تھیں; کی and کے of کا; every possessive (میرا, ہمارا) a word of its own with its
# forms; لیے "for" (کے لیے) a word of its own; verb forms the pack also has as a rare word spelled alike (کر "tax", دیا
# "lamp", آیا "nanny", ہوا "air", گیا "Gaya", مل "mill") sent to their verb; the future's گا, گی, گے, written apart, a word
# of their own; کس, کن, کسے of کون; the gender pair's other word (پوری of پورا, نئی of نیا).
_VERB_FORMS = {
    "کرنا": "کر کرو کروں", "جانا": "جا گیا گئے", "دینا": "دیا", "کہنا": "کہا کہہ", "رہنا": "رہا", "آنا": "آیا",
    "ملنا": "مل ملا ملی", "بننا": "بن", "ہونا": "ہوا ہوتا ہوگا ہوگی ہونگے", "چلنا": "چلو", "ڈالنا": "ڈال", "پہنچنا": "پہنچ پہنچا",
    "چکنا": "چکا چکی", "سکنا": "سکتے سکے سکا", "نکلنا": "نکل", "روکنا": "روک", "پکڑنا": "پکڑ", "رکھنا": "رکھ", "پڑنا": "پڑا پڑے پڑتی",
    "لینا": "لو", "لگنا": "لگ", "بتانا": "بتا", "گرنا": "گر", "توڑنا": "توڑ توڑا", "سننا": "سنا", "بھیجنا": "بھیجا",
    "لانا": "لا لائی", "بدلنا": "بدل", "پانا": "پا پائی", "بنانا": "بنائی", "پوچھنا": "پوچھ", "بولنا": "بول", "کھولنا": "کھول",
    "کھیلنا": "کھیلا", "بھاگنا": "بھاگ", "لگانا": "لگائی", "جاگنا": "جاگ", "ماننا": "مان", "باندھنا": "باندھ", "ٹوٹنا": "ٹوٹا",
    "پھٹنا": "پھٹ", "ہنسنا": "ہنس", "سیکھنا": "سیکھ", "گھسنا": "گھس",
}
_PRONOUN_FORMS = {"وہ": "انہوں انھوں", "کون": "کس کن کسے", "یہی": "اسی انہی", "وہی": "اسی انہی"}
_POSSESSIVES = "میرا تیرا ہمارا تمہارا"
OVERRIDES = {
    "ہے": ["ہے"], "ہیں": ["ہے"], "ہوں": ["ہے"],
    "تھا": ["تھا"], "تھی": ["تھا"], "تھے": ["تھا"], "تھیں": ["تھا"],
    "کی": ["کا", "کرنا"], "کے": ["کا"],
    "لیے": ["لیے"], "لئے": ["لیے"],
    "گا": ["گا"], "گی": ["گا"], "گے": ["گا"],
    "کیا": ["کیا", "کرنا"], "بنا": ["بننا", "بنانا"],
    "دونوں": ["دونوں"], "سی": ["سا"], "سے": ["سے"],
    "پوری": ["پورا"], "نئی": ["نیا"], "ایسی": ["ایسا"], "ایسے": ["ایسا"], "جیسی": ["جیسا"], "جیسے": ["جیسا"],
    "اخبار": ["اخبار"], "اخبارات": ["اخبار"],
    **{form: [word] for word, forms in {**_VERB_FORMS, **_PRONOUN_FORMS}.items() for form in forms.split()},
    **{possessive[:-1] + ending: [possessive] for possessive in _POSSESSIVES.split() for ending in "اےی"},
}
OVERRIDES["اسی"] = ["یہی", "وہی"]
OVERRIDES["انہی"] = ["یہی", "وہی"]
# The perfective of one verb and the stem of its causative (اٹھا of اٹھنا and اٹھانا), and the rupee's oblique, which the
# pack glosses as a sense of its own.
OVERRIDES["اٹھا"] = ["اٹھنا", "اٹھانا"]
OVERRIDES["چھپا"] = ["چھپنا", "چھپانا"]
OVERRIDES["روپے"] = ["روپیہ"]
# The spellings the pack has of two words of WORDS: پولس (of पुलिस) and اسپتال, which Urdu text writes پولیس and ہسپتال.
OVERRIDES["پولس"] = ["پولیس"]
OVERRIDES["اسپتال"] = ["ہسپتال"]
# And the stems and forms of گزرنا and کٹنا that the pack has as rare words: گزر "carrot", کٹی "waist".
OVERRIDES["گزر"] = ["گزرنا"]
OVERRIDES["کٹی"] = ["کٹنا"]
# Names (the news's people and places), the Latin letters' names, the Taliban, and ان کا, ان کی, ان کے written as one word
# and ان کو (which the pack takes for the rare verb انکنا's forms).
EXCLUDE = {"خان", "علی", "نواز", "سندھ", "پنجاب", "سعودی", "طالبان", "روس", "مودی", "نثار", "جاوید", "سنگھ",
           "ڈی", "ایس", "بی", "ٹی", "ایم", "این", "ایف", "ایل", "آر", "وی", "یو", "ای", "کیو", "ایچ", "انکا", "انکی", "انکے", "انکو"}

# Everyday words the pack has no entry for, as their kind and forms (a verb's are verb_forms'): the English Wiktionary has no
# Urdu entry for them, and its Hindi entries name no Urdu spelling (नीचे, वहीं, कटना) or another (पुलिस's پولس, गुज़ारना's
# گذارنا, सदस्य's سدسیہ). والا, written apart (کرنے والا), is the pack's suffix; بارے (کے بارے میں), بارہ's oblique, is a word
# of its own, as Hindi's बारे is.
WORDS = {
    "بارے": ("noun", ""), "والا": ("noun", "والی والے والوں والیاں"), "نیچے": ("adv", ""), "وہیں": ("adv", ""),
    "جبکہ": ("conj", ""), "رکن": ("noun", "رکنوں ارکان اراکین"), "پولیس": ("noun", ""), "ہسپتال": ("noun", "ہسپتالوں"),
    "معلومات": ("noun", ""), "تبدیلی": ("noun", "تبدیلیاں تبدیلیوں"), "تقریب": ("noun", "تقریبات تقریبوں"), "تعاون": ("noun", ""),
    "سرکاری": ("adj", ""), "عالمی": ("adj", ""), "حقیقی": ("adj", ""), "طویل": ("adj", ""),
    "گزارنا": ("verb", ""), "گزرنا": ("verb", ""), "کٹنا": ("verb", ""),
}
# Spellings the rules below would make another word's form that are no form of it: گوئی (پیشن گوئی), فنی "technical", مینے
# and مینوں (میں نے, Punjabi "to me"), اتے, کرانے (of the verb کرانا), بقائے (بقا with the izafat), رندوں "drunkards", ہیے,
# افسوں "spell", مایوں, بریں, آئندے; the suffixes' سیٹھی, پرستوں, کاریوں; names (شروتی, شیدی, بقائی); and the forms of
# adjectives in -ا that do not inflect (پیدا, جدا, سوا, گویا, ماتا, مہا, ناروا, صفرا, ددا, خدرا, خیلا).
NOT_FORMS = {"گوئی", "فنی", "مینے", "مینوں", "اتے", "کرانے", "بقائے", "رندوں", "ہیے", "افسوں", "مایوں", "بریں", "آئندے",
             "سیٹھی", "پرستوں", "کاریوں", "شروتی", "شیدی", "بقائی", "پیدی", "جدی", "سوی", "سوے", "گوئے", "ماتے", "مہے", "مہی",
             "ناروی", "صفری", "صفرے", "ددی", "خدری", "خیلی"}


def mend(db, pos, lemmas, common):
    """
    Adds WORDS, and the forms the pack misses of its nouns and adjectives without a table of their own, to the list
    builder's [pos], [lemmas] and [common]. The pack makes those forms by the rules only for a gender its entry's head names
    (g=), which the Hindi entries name otherwise, and only for an adjective its Hindi entry shows inflected: دورے, عرصے of
    دورہ, عرصہ (masculine on their senses), پابندیاں of پابندی, تھوڑی and تھوڑے of تھوڑا. Such a form goes to its word when
    the pack has it neither as a word nor as a form.
    """
    for word, (kind, forms) in WORDS.items():
        pos[word].add(kind)
        common[word] = word
        for form in forms.split() or (verb_forms(word) if kind == "verb" else []):
            if form not in WORDS and word not in lemmas.get(form, ()):
                lemmas[form].append(word)
    USAGE.clear()
    USAGE.update(read_usage(db, pos, lemmas))
    unlink_lookalikes(pos, lemmas)
    # The oblique plural of an adjective in -ا the pack's table inflects, which texts use as a noun (بڑوں "elders",
    # چھوٹوں), and which the tables lack, so that the pack gave it to a rarer word (بڑ) or verb (چھوٹنا) alone; not one
    # spelled as a noun in -ہ's (دانوں of دانہ, not دانا "wise").
    for word, kinds in sorted(pos.items()):
        if "adj" in kinds and word.endswith("ا") and word in lemmas.get(word[:-1] + "ے", ()):
            form = word[:-1] + "وں"
            if form not in pos and word[:-1] + "ہ" not in pos and form not in OVERRIDES and form not in NOT_FORMS \
                    and word not in lemmas.get(form, ()):
                lemmas[form].append(word)
    tabled = {lemma for (lemma,) in db.execute("SELECT DISTINCT lemma FROM forms WHERE generated = 0")}
    genders = {}
    for word, kind, senses in db.execute("SELECT word_lc, pos, senses FROM entries WHERE pos IN ('noun', 'adj')"):
        # Not a rare word spelled as another's form, whose forms would go with it (پوری "puri" of پورا, کہنی "elbow" of کہنا).
        if word in tabled or word in EXCLUDE or word in OVERRIDES or lemmas.get(word) or " " in word:
            continue
        tags = {t for s in json.loads(senses) for t in s.get("tags", [])} & {"masculine", "feminine"}
        genders.setdefault((word, kind), set()).update(tags)
    made = {}
    for (word, kind), tags in genders.items():
        gender = next(iter(tags)) if len(tags) == 1 else None
        forms = adjective_forms(word) if kind == "adj" else noun_forms(word, gender)
        # And an adjective's oblique plural, which texts use as a noun (پیاروں), unless spelled as a noun in -ہ's (دانوں).
        if kind == "adj" and len(forms) == 2 and not forms[0].endswith("ئے") and word[:-1] + "ہ" not in pos:
            forms = forms + [word[:-1] + "وں"]
        # A masculine noun in -اہ ends in the consonant (ماہ, گواہ): گواہوں, not گواوں.
        if kind == "noun" and gender == "masculine" and word.endswith("اہ"):
            forms = [word + "وں"]
        for form in forms:
            if form not in pos and not lemmas.get(form) and form not in NOT_FORMS and form not in OVERRIDES and form not in EXCLUDE:
                made.setdefault(form, []).append(word)
    for form, words in made.items():
        lemmas[form] = list(dict.fromkeys(words))


# How the news and Wikipedia sentences use the words (Leipzig's, the build's third source): the builder sets SENTENCES to
# their archives, and mend reads them once into USAGE.
SENTENCES = []
USAGE = {}
# The content words of the pack's glosses of each word, as read_usage finds them.
GLOSSES = {}
# What follows a verb's stem or participle (کر, گیا, دیا, لیا, رہا, سکا, چکا, ہوئے, ہے, گا) and a noun's or adjective's
# form (کا, کی, کو, میں, سے, پر, نے; والا), and what comes before a noun (کا, ایک, اس, اپنا, ہر).
_VERBAL = set("""کر گیا گئی گئے گئیں جاتا جاتی جاتے جائے جائیں جانا جانے جاؤ جاتیں دیا دی دیے دئیے دئے دیں دینا دینے دیتا دیتی
دیتے دے دو لیا لی لیے لئے لیں لینا لینے لیتا لیتی لیتے لے لو رہا رہی رہے رہیں رہنا رہنے رہتا رہتی رہتے سکا سکی سکے سکتا سکتی
سکتے سکیں چکا چکی چکے چکیں ہوا ہوئی ہوئے ہوئیں پڑا پڑی پڑے پڑتا پڑتی پڑتے ڈالا ڈالی ڈالے بیٹھا بیٹھی بیٹھے اٹھا اٹھی اٹھے
گا گی گے ہے ہیں تھا تھی تھے تھیں""".split())
_LITTLE = set("کا کی کے کو میں سے پر نے تک اور یا بھی ہی تو نہ نہیں".split())
_AFTER_NOUN = set("کا کی کے کو میں سے پر نے تک والا والی والے والوں".split())
_BEFORE_NOUN = set("کا کی کے ایک اس ان یہ وہ اپنا اپنی اپنے فی ہر کوئی کسی کئی سب تمام".split())
# The titles written before a person's name, and the commonest parts of the news's names, given names and family names
# (not the pack's names, which are places as often: آزاد کشمیر, سعودی عرب): a word mostly written beside them is a name
# (نسیم اشرف, ڈاکٹر نسیم; رجب طیب, قیصر شیخ). Not ملک "country", پیر "Monday", میاں "husband", خواجہ "eunuch" (خواجہ
# سرا), اللہ (انشاء اللہ), شریف (قرآن شریف), which stand beside words as often.
_TITLES = set("""ڈاکٹر مولانا جسٹس جنرل بیگم صاحبزادہ صاحبزادی علامہ حافظ مفتی قاری حاجی سردار نواب مخدوم مسٹر مسز سینیٹر
کرنل میجر کیپٹن بریگیڈیئر انسپکٹر سید چوہدری چودھری رانا راجہ شیخ شہزادہ شہزادی""".split())
_NAME_PARTS = set("""محمد احمد علی حسین شاہ الدین الرحمن الرحمان الحق اقبال چوہدری چودھری سید شیخ بٹ قریشی رضوی نقوی زیدی جعفری
گیلانی بخاری عباسی بلوچ صدیقی فاروقی ہاشمی انصاری اعوان مزاری بگٹی خٹک آفریدی درانی بیگ مرزا اختر اشرف اسلم اکرم ارشد اعجاز
افضل امجد انور ایاز بشیر جاوید جمیل حامد حفیظ حمید خالد رشید رضا ریاض زاہد سعید سلیم شاہد شفیق شوکت صدیق طارق عارف عاصم
عامر عباس عبداللہ عثمان عرفان عمران غلام فاروق فیصل قادر کامران کریم ماجد مجید محمود مسعود ممتاز منیر ناصر نعیم وسیم وقار
یوسف یونس راشد سنگھ""".split())
# Words written between two names that are no names (اسلم ولد اکرم, پاکستان بمقابلہ بھارت, X عرف Y).
_BETWEEN_NAMES = {"ولد", "بنت", "زوجہ", "عرف", "بن", "بمقابلہ"}
# A word mostly written beside names (NAMED_SHARE of the times the sentences have it) is a name: the news's people.
NAMED_SHARE = 0.5
# The fewest times the sentences must have a word for its uses there to settle anything.
SEEN_ENOUGH = 20
# The words of the glosses that tell no sense from another.
_STOP_GLOSS = set("""the and for with from that this one any some being used person thing which who what into onto off out
over upon very more most also not other another given name female male kind sort type form act state quality etc something
someone especially usually often""".split())


def _gloss_words(senses):
    """The content words of an entry's English glosses."""
    return {w for s in senses for g in s.get("glosses", []) for w in re.findall("[a-z]+", g.lower())
            if len(w) > 2 and w not in _STOP_GLOSS}


def _sentences():
    for path in SENTENCES:
        if not path:
            continue
        with tarfile.open(path) as archive:
            members = sorted((m for m in archive.getmembers() if m.name.endswith("-sentences.txt")), key=lambda m: m.name)
            for member in members:
                for line in archive.extractfile(member):
                    yield line.decode("utf-8", "replace").rstrip("\n").partition("\t")[2]


def read_usage(db, pos, lemmas):
    """
    Counts, for every word of the sentences (as the reader cuts and folds them): how often they have it (seen), and how
    often it stands before a verb's helper (verbal: ڈٹ کر, سہمے ہوئے), as a noun stands (nominal: کی سمری, ہیرے کی, both
    counted), beside a name or after a title (named), as a piece of a word that a space split (broken: بڑ ی, خلا ف, اجلا س,
    the letter after it or before it making a word or form of the pack), or as a piece of the pack's two-word words whose
    sense is none of its own, their glosses sharing no word's first five letters (foreign: مون of مون سون "monsoon", صلاح
    of the name صلاح الدین; not مرچ of کالی مرچ "black pepper", شناختی "identifying" of شناختی کارڈ "identity card").
    """
    known = set(pos) | {f for f, of in lemmas.items() if of}
    glosses = {}
    pairs = {}
    for word, senses in db.execute("SELECT word_lc, senses FROM entries ORDER BY id"):
        glosses.setdefault(urdu_fold(word), set()).update(_gloss_words(json.loads(senses)))
    for word, senses in db.execute("SELECT word_lc, senses FROM entries WHERE word_lc LIKE '% %' ORDER BY id"):
        parts = tuple(urdu_fold(word).split())
        if len(parts) == 2:
            pairs.setdefault(parts, set()).update(_gloss_words(json.loads(senses)))
    foreign = {}
    for (a, b), words in pairs.items():
        # Not a postposition's or conjunction's pair (کے ساتھ, سب سے, آپس میں), nor a word written twice (آہستہ آہستہ).
        if a == b or a in _LITTLE or b in _LITTLE:
            continue
        stems = {w[:5] for w in words}
        foreign[(a, b)] = {p for p in (a, b) if p in glosses and not {w[:5] for w in glosses[p]} & stems}
    before_name = _NAME_PARTS | _TITLES
    GLOSSES.clear()
    GLOSSES.update(glosses)
    kinds = ("seen", "verbal", "nominal", "named", "broken", "foreign")
    usage = {k: {} for k in kinds}
    seen, verbal, nominal, named, broken, alien = (usage[k] for k in kinds)
    for sentence in _sentences():
        words = LETTERS.findall(urdu_fold(sentence))
        last = len(words) - 1
        for i, word in enumerate(words):
            seen[word] = seen.get(word, 0) + 1
            before = words[i - 1] if i else ""
            after = words[i + 1] if i < last else ""
            if after in _VERBAL:
                verbal[word] = verbal.get(word, 0) + 1
            nominal[word] = nominal.get(word, 0) + (after in _AFTER_NOUN) + (before in _BEFORE_NOUN)
            if before in before_name or after in _NAME_PARTS:
                named[word] = named.get(word, 0) + 1
            if (len(after) == 1 and after != "و" and word + after in known) or (
                    len(before) == 1 and before not in "وک" and before + word in known):
                broken[word] = broken.get(word, 0) + 1
            if word in foreign.get((word, after), ()) or word in foreign.get((before, word), ()):
                alien[word] = alien.get(word, 0) + 1
    return usage


def _share(kind, word):
    seen = USAGE["seen"].get(word, 0)
    return USAGE[kind].get(word, 0) / seen if seen >= SEEN_ENOUGH else 0.0


# A verb's ambiguous form (its stem, perfective, subjunctive or imperative: سمری, بوری, ہیری, کلپ, پچ, بہرے, بچوں) whose
# sentences seldom put a helper after it (VERBAL_SHARE) and mostly use it as a noun, and whose verb's own forms (the
# infinitive and habitual participle: سمرنے, سمرتا) are rarer than PLAIN_SHARE times it, is no form of that verb.
VERBAL_SHARE = 0.1
PLAIN_SHARE = 0.2


def _verb_own_forms(verb):
    stem = verb[:-2]
    return {stem + e for e in ("نا", "نے", "نی", "نیں", "تا", "تے", "تی", "تیں")}


# Or, of a form spelled as a noun's or adjective's regular form (بہرے of بہرا "deaf", ہیرے of ہیرا), the sentences put a
# helper after it less than OTHER_VERBAL_SHARE of the time.
OTHER_VERBAL_SHARE = 0.25


def unlink_lookalikes(pos, lemmas):
    """Takes the verbs' ambiguous forms that the sentences use as other words off those verbs (see VERBAL_SHARE)."""
    seen = USAGE["seen"]
    verbs = defaultdict(list)
    for form, of in lemmas.items():
        for lemma in of:
            if lemma.endswith("نا") and "verb" in pos.get(lemma, ()):
                verbs[lemma].append(form)
    regular = set()
    for word, kinds in pos.items():
        if " " not in word and kinds & {"noun", "adj"}:
            regular.update(adjective_forms(word) if "adj" in kinds else [])
            regular.update(noun_forms(word, "masculine") if word[-1:] in "اہ" and "noun" in kinds else [])
    for verb in sorted(verbs):
        own = _verb_own_forms(verb)
        plain = sum(seen.get(f, 0) for f in own)
        for form in sorted(set(verbs[verb]) - own - {verb}):
            count = seen.get(form, 0)
            if form in OVERRIDES or plain >= PLAIN_SHARE * count:
                continue
            helped, used = _share("verbal", form), _share("nominal", form)
            # A noun or adjective of the pack (بہرا "deaf") is no form of a verb the sentences hardly have, however seldom
            # they have it; the mend below then makes its forms.
            spelled = pos.get(form, set()) & {"noun", "adj"}
            other = helped < VERBAL_SHARE and used >= helped or form in regular and helped < OTHER_VERBAL_SHARE
            if other if count >= SEEN_ENOUGH else spelled:
                lemmas[form] = [l for l in lemmas[form] if l != verb]


def _variant_key(word):
    """A word as its other spellings write it too: final ہ as ا (مہینہ, مہینا), ؤ as و, ئ as ی, ھ as ہ, آ as ا."""
    if word.endswith("ہ"):
        word = word[:-1] + "ا"
    return word.replace("ؤ", "و").replace("ئ", "ی").replace("ھ", "ہ").replace("آ", "ا")


def fold(scores, forms, pos):
    """
    After the counting: a word or form counts only its uses that are that word (read_usage), not those as a piece of a word
    a space split (بڑ, خلا) or of a two-word word of another sense (مون, صلاح); a word written mostly beside names (نسیم,
    طیب, قیصر) is a name; and two spellings of one word (مہینا, مہینہ: the same key, glosses in common) are the commoner
    one.
    """
    for word in sorted(scores):
        counted = forms[word]
        for form in sorted(counted):
            piece = min(1.0, _share("broken", form) + _share("foreign", form))
            if piece:
                counted[form] *= 1 - piece
    for word in sorted(scores):
        named = word not in _TITLES and word not in _BETWEEN_NAMES and _share("named", word) >= NAMED_SHARE
        if named:
            del scores[word]
    keyed = {}
    for word in sorted(scores, key=lambda w: (-sum(forms[w].values()), w)):
        key = _variant_key(word)
        first = keyed.setdefault(key, word)
        if first != word and GLOSSES.get(first, set()) & GLOSSES.get(word, set()):
            for form, share in sorted(forms[word].items()):
                forms[first][form] = forms[first].get(form, 0) + share
            del scores[word]
    for word in scores:
        scores[word] = sum(forms[word].values())
