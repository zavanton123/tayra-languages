"""
Urdu for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

The reader (UrduParser) matches a word in Urdu letters without marks: ك as ک, ي and ى as ی, the Arabic ه, ە and ۀ as ہ,
ۂ as ہ, ۓ as ئے, without its vowel signs, tatweel and non-joiner. ھ (do-chashmi he, which writes aspiration: بھی, تھا), ہ, ے,
ں, ٹ, ڈ and ڑ are letters of their own and stay. So do these: the pack is written so, and the list counts words so.
Keep urdu_fold and UrduParser.fold the same.
"""
import re

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
# Names (the news's people and places), the Latin letters' names, the Taliban, and ان کا, ان کی, ان کے written as one word
# and ان کو (which the pack takes for the rare verb انکنا's forms).
EXCLUDE = {"خان", "علی", "نواز", "سندھ", "پنجاب", "سعودی", "طالبان", "روس", "مودی", "نثار", "جاوید", "سنگھ",
           "ڈی", "ایس", "بی", "ٹی", "ایم", "این", "ایف", "ایل", "آر", "وی", "یو", "ای", "کیو", "ایچ", "انکا", "انکی", "انکے", "انکو"}
