"""
Urdu for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

The reader (UrduParser) matches a word in Urdu letters without marks: ك as ک, ي and ى as ی, the Arabic ه, ە and ۀ as ہ,
ۂ as ہ, ۓ as ئے, without its vowel signs, tatweel and non-joiner. ھ (do-chashmi he, which writes aspiration: بھی, تھا), ہ, ے,
ں, ٹ, ڈ and ڑ are letters of their own and stay. So do these: the pack is written so, and the list counts words so.
Keep urdu_fold and UrduParser.fold the same.
"""
import gzip
import json
import os
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
# And the stems and forms of گزرنا and کٹنا that the pack has as rare words: گزر "carrot", کٹی "waist"; چاہنا's stem
# (چاہ رہا), the pack's "wish", and جینا's imperative (جیو اور جینے دو), the pack's "living", which the news writes mostly
# for جیو نیوز.
OVERRIDES["گزر"] = ["گزرنا"]
OVERRIDES["کٹی"] = ["کٹنا"]
OVERRIDES["چاہ"] = ["چاہنا"]
OVERRIDES["جیو"] = ["جینا"]
# And کرانا's infinitive (جمع کرانی ہے), the pack's "clerk".
OVERRIDES["کرانی"] = ["کرانا"]
# Names (the news's people and places), the Latin letters' names, the Taliban, and ان کا, ان کی, ان کے written as one word
# and ان کو (which the pack takes for the rare verb انکنا's forms).
EXCLUDE = {"خان", "علی", "نواز", "سندھ", "پنجاب", "سعودی", "طالبان", "روس", "مودی", "نثار", "جاوید", "سنگھ",
           "ڈی", "ایس", "بی", "ٹی", "ایم", "این", "ایف", "ایل", "آر", "وی", "یو", "ای", "کیو", "ایچ", "انکا", "انکی", "انکے", "انکو"}

# Words of the pack the list leaves out, whose counts read_usage cannot tell from another word's: halves of pairs written
# apart (بھگ of لگ بھگ, سام of سام سنگ, دیشی of بنگلہ دیشی, روی of سست روی, پو of پو لیس, ال of the Arabic article,
# Punjabi وچ, بیر of بڈھ بیر), brands (میزان بینک, ٹیلی نار), and the news's people the names beside them did not show
# (رحمن ملک, رنگنا ہیراتھ, مبشر لقمان, یاسمین راشد, قادر پٹیل, راج ناتھ, سجنا; جلال, طیب, نسیم, اصغر, فراز, شفیق, گلزار, صابر,
# کاشف, ہمایوں, نجیب, تبسم, ثروت, شبنم, نیر, اشرف سوہنا).
NOT_WORDS = {"بھگ", "سام", "دیشی", "روی", "پو", "ال", "وچ", "بیر", "میزان", "نار", "رحمن", "رنگنا", "لقمان", "یاسمین",
             "پٹیل", "ناتھ", "سجنا", "جلال", "طیب", "نسیم", "اصغر", "فراز", "شفیق", "گلزار", "صابر", "کاشف", "ہمایوں", "نجیب",
             "تبسم", "ثروت", "شبنم", "نیر", "سوہنا",
             # Past rank 4300: ہاتھا of ہاتھا پائی, ہکا of ہکا بکا, گنت of ان گنت, پیل of دھکم پیل, the places مالا کنڈ
             # and جھار کھنڈ, the pieces of words a space split that the rules miss (جار ی, مہان کوش, بر ائے, آلا ت,
             # اصلا ح, ملا وٹ, کلا س, گر فتا ر, انو ں), English and brands (ارتھ, ویکی, پورن), and people (سنی دیول, جان ابراہام, ایشوریہ رائے, جنرل ٹکا خان, شکتی کپور,
             # قاسم سلیمانی, مینا کماری, دیپکا پڈوکون, جوہی چاولہ, مدھو بالا, پرتھوی راج, مہیلا جے وردھنے; سمیر, سیما,
             # تابش, اجیت, ستیہ, پریا, جیوتی, ساہی, ریکھا, کلیان, سیمل, ابتسام, مانکی, کھڑک) and ساہتیہ of the academies.
             "ہاتھا", "ہکا", "کنڈ", "دیول", "ابراہام", "ایشوریہ", "ٹکا", "سمیر", "سیما", "تابش", "اجیت", "گنت", "جار", "کوش",
             "ائے", "آلا", "اصلا", "وٹ", "کلا", "فتا", "انو", "اونا", "ارتھ", "ویکی", "شکتی", "سلیمانی", "کماری", "دیپکا",
             "جوہی", "مدھو", "پرتھوی", "مہیلا", "ستیہ", "پریا", "جیوتی", "ساہی", "ریکھا", "کلیان", "سیمل", "ابتسام", "مانکی",
             "کھڑک", "ساہتیہ", "کھنڈ", "پیل", "پورن",
             # Past rank 4700: the suffix گان written apart (نادہندہ گان), the people, films, songs and brands the
             # sentences write as no word (دیپک کمار, ماڈل مسکان, یوگی آدتیہ ناتھ, امرت راج, اظہر رنگیلا, فلم عاشقی ٹو,
             # فائر فاکس, وہیل چیئرز, تائی کوانڈو, ناگن چورنگی, میر چاکر, سائی بابا, قلی قطب شاہ, سجن یونین, گبر سنگھ;
             # رمزی, بالو, عنبر, مانک),
             # the places کٹک, کورال (کو رال) and the Chola dynasty (چول), pieces of words split by a space the rules miss
             # (باو لنگ, آ سانی, آ رمی; رمی جمرات aside), the misspelt کوی (کوئی), and words the sentences hardly use but
             # in Hindi spelled in Urdu letters (ناسنا, ڈاسنا, کھبا, گنگنا, سڑا); and from 5101, more people (سیپ
             # بلاٹر, راکھی ساونت, سیرل نن, کالو خان, امل, نزہت, عرشی, گلفام), pieces (دہرہ دون, دی ادر, کر نسی of نسنا,
             # لو دھی, متعارف کریا, کار آمد's کارا, انڈ سٹری), and زنگی, a slur as حبشی is.
             "گان", "دیپک", "مسکان", "یوگی", "امرت", "رنگیلا", "عاشقی", "فاکس", "چیئرز", "تائی", "ناگن", "چاکر", "سائی",
             "قلی", "سجن", "گبر", "کٹک", "رال", "چول", "لنگ", "سانی", "رمی", "کوی", "ناسنا", "ڈاسنا", "کھبا", "گنگنا",
             "سڑا", "رمزی", "بالو", "عنبر", "مانک", "سیپ", "راکھی", "نن", "کالو", "امل", "دون", "ادر", "نسنا", "دھی",
             "کریا", "زنگی", "نزہت", "عرشی", "گلفام", "کارا", "انڈ"}
# And the verbs' forms that unlink_lookalikes cannot tell from another word's: بسر of بسر کرنا "spend", no form of بسرنا
# "forget".
NOT_VERB_FORMS = {"بسر": "بسرنا"}

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
    # برتنا "treat, show" (غفلت برت رہی ہے), whose stem the pack has as a Hindu fast.
    "برتنا": ("verb", ""),
}
# Spellings the rules below would make another word's form that are no form of it: گوئی (پیشن گوئی), فنی "technical", مینے
# and مینوں (میں نے, Punjabi "to me"), اتے, کرانے (of the verb کرانا), بقائے (بقا with the izafat), رندوں "drunkards", ہیے,
# افسوں "spell", مایوں, بریں, آئندے; the suffixes' سیٹھی, پرستوں, کاریوں; names (شروتی, شیدی, بقائی, ہیری "Harry"); and the
# forms of adjectives in -ا that do not inflect (پیدا, جدا, سوا, گویا, ماتا, مہا, ناروا, صفرا, ددا, خدرا, خیلا).
NOT_FORMS = {"گوئی", "فنی", "مینے", "مینوں", "اتے", "کرانے", "بقائے", "رندوں", "ہیے", "افسوں", "مایوں", "بریں", "آئندے",
             "سیٹھی", "پرستوں", "کاریوں", "شروتی", "شیدی", "بقائی", "ہیری", "پیدی", "جدی", "سوی", "سوے", "گوئے", "ماتے",
             "مہے", "مہی", "ناروی", "صفری", "صفرے", "ددی", "خدری", "خیلی"}


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
    send_verb_uses(pos, lemmas)
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
    file_oblique_plurals(pos, lemmas)


def file_oblique_plurals(pos, lemmas):
    """
    Gives the oblique plural of a masculine noun in -ہ that the pack gave to a word spelled as its stem (پیمانوں of پیمانہ,
    not پیمان "vow"; دانوں of دانہ, not دان) to that noun, when the sentences have the noun and its oblique (پیمانہ,
    پیمانے) more often than the other word.
    """
    seen = USAGE["seen"]
    for form in sorted(lemmas):
        stem = form[:-2]
        if not form.endswith("وں") or stem not in lemmas[form] or form in OVERRIDES:
            continue
        # Or in -ا (انڈوں of انڈا, not انڈ "castor").
        for noun in (stem + "ہ", stem + "ا"):
            if "noun" in pos.get(noun, ()) and seen.get(noun, 0) + seen.get(stem + "ے", 0) > seen.get(stem, 0):
                lemmas[form] = list(dict.fromkeys(noun if lemma == stem else lemma for lemma in lemmas[form]))
                break


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
# What follows a verb's stem (بھون لیں, کھود کر, سونپ دیا, لٹ گئے, جھوم اٹھے), and a perfective (تلی ہوئی, سونپی گئی).
# A noun takes کر, دیا and لیا too (کام کر, حکم دیا, حصہ لیا): a stem is one the sentences also follow with the others.
_STEM_HELPERS = set("""لیا لی لیے لئے لیں لے لو لیجیے لیجئے دیا دی دیے دئے دیئے دئیے دیں دے دو دیجیے دیجئے رہا رہی رہے رہیں
سکا سکی سکے سکتا سکتی سکتے سکیں چکا چکی چکے چکیں گیا گئی گئے گئیں جاتا جاتی جاتے جائے جائیں جانا جانے ڈالا ڈالی ڈالے ڈالیں
بیٹھا بیٹھی بیٹھے پایا پائے پائی پاتا پاتے اٹھا اٹھی اٹھے اٹھیں""".split())
_PERFECT_HELPERS = set("ہوا ہوئی ہوئے ہوئیں گیا گئی گئے گئیں جاتا جاتی جاتے جائے جائیں جانا جانے".split())
_HELPING = _VERBAL | _LITTLE | _AFTER_NOUN | _BEFORE_NOUN | TABLE_PRONOUNS | _STEM_HELPERS | _PERFECT_HELPERS
# A word the sentences write beside one word PAIR_SHARE of the time, or beside a name NAME_PAIR_SHARE of the time, is that
# often a piece of a pair (read_usage); not one they have more than PAIRED_MOST times.
PAIR_SHARE = 0.75
NAME_PAIR_SHARE = 0.4
PAIRED_MOST = 5000
# A word no word of the pack that the sentences write beside names NAME_PARTNER_SHARE of the time is a name too (یاور
# زمان, قاسم سلیمانی): the partner of a pair as NAME_PAIR_SHARE has it.
NAME_PARTNER_SHARE = 0.25
# The titles written before a person's name, and the commonest parts of the news's names, given names and family names
# (not the pack's names, which are places as often: آزاد کشمیر, سعودی عرب): a word mostly written beside them is a name
# (نسیم اشرف, ڈاکٹر نسیم; رجب طیب, قیصر شیخ). Not ملک "country", پیر "Monday", میاں "husband", خواجہ "eunuch" (خواجہ
# سرا), اللہ (انشاء اللہ), شریف (قرآن شریف), which stand beside words as often.
_TITLES = set("""ڈاکٹر مولانا جسٹس جنرل بیگم صاحبزادہ صاحبزادی علامہ حافظ مفتی قاری حاجی سردار نواب مخدوم مسٹر مسز سینیٹر
کرنل میجر کیپٹن بریگیڈیئر انسپکٹر سید چوہدری چودھری رانا راجہ شیخ شہزادہ شہزادی اداکار اداکارہ گلوکار گلوکارہ
کرکٹر""".split())
_NAME_PARTS = set("""محمد احمد علی حسین شاہ الدین الرحمن الرحمان الحق اقبال چوہدری چودھری سید شیخ بٹ قریشی رضوی نقوی زیدی جعفری
گیلانی بخاری عباسی بلوچ صدیقی فاروقی ہاشمی انصاری اعوان مزاری بگٹی خٹک آفریدی درانی بیگ مرزا اختر اشرف اسلم اکرم ارشد اعجاز
افضل امجد انور ایاز بشیر جاوید جمیل حامد حفیظ حمید خالد رشید رضا ریاض زاہد سعید سلیم شاہد شفیق شوکت صدیق طارق عارف عاصم
عامر عباس عبداللہ عثمان عرفان عمران غلام فاروق فیصل قادر کامران کریم ماجد مجید محمود مسعود ممتاز منیر ناصر نعیم وسیم وقار
یوسف یونس راشد سنگھ""".split())
# And the families the news names most that are words of the pack too (کپور "camphor"): beside them a word is a name in
# a pair (رشی کپور, امیتابھ بچن, سونیا گاندھی, اشوک کمار), not elsewhere.
_FAMILIES = set("کپور بچن گاندھی کمار".split())
# Words written between two names that are no names (اسلم ولد اکرم, پاکستان بمقابلہ بھارت, X عرف Y).
_BETWEEN_NAMES = {"ولد", "بنت", "زوجہ", "عرف", "بن", "بمقابلہ"}
# The words written before a work's title (فلم عاشقی, ڈرامہ سیریل), which count as a title's for a word after them.
_WORKS = set("فلم فلموں ڈرامہ ڈرامے سیریل ناول".split())
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


# The words the pack glosses as a given name or surname (دیپک "Deepak", پریتم), with those it says come from Sanskrit; the
# words all of whose senses name a place, people, person or faith (ترکستان "Turkestan", زرتشت "Zoroaster", ٹوری "a
# Tory"); and the words the Urdu and Hindi entries say are borrowed from Sanskrit (سنکیت, پرہار, وکاس, ایشور), which the
# Urdu news and Wikipedia write seldom: read_usage fills them.
KINDS = {"given": set(), "hindu_given": set(), "proper": set(), "sanskrit": set()}
_GIVEN = re.compile(r"\b(given name|surname)\b")
# A gloss's first words, up to its first comma, colon, bracket or dash.
_HEAD_GLOSS = re.compile(r"[^,;:(–—]+")
# The capitalised words a gloss starts with that make no name (A Tory, The Netherlands; To ...).
_GLOSS_STARTS = {"A", "An", "The", "To", "Any", "One", "Of", "In", "Used", "Short", "Alternative", "Synonym", "Name",
                 "Abbreviation", "Plural", "Feminine", "Masculine", "Obsolete", "See"}
_CONNECTORS = {"of", "the", "and", "de", "al", "el", "ibn", "bin"}
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# The Wiktionary dumps the pack is built from (tools/build_dictionary.py), whose etymologies tell the Sanskrit borrowings.
DUMPS = (os.path.join(ROOT, "dictionaries", "kaikki.org-dictionary-Urdu.jsonl.gz"),
         os.path.join(ROOT, "dictionaries", "kaikki.org-dictionary-Hindi.jsonl.gz"))
# The first line of an etymology that says where the word comes from (after its tree or its verb set).
_ORIGIN = re.compile(r"(Learned borrowing|Semi-learned borrowing|Borrowed|Inherited|From|Derived|Ultimately|Compound|"
                     r"Equivalent|Clipping|Calque|Coined|Back-formation|Onomatopoeic|Uncertain|Unknown|Of |Cognate|Related|"
                     r"Doublet|Possibly|Probably|Perhaps)")
_SANSKRIT = ("Learned borrowing from Sanskrit", "Borrowed from Sanskrit")


def _proper_gloss(gloss, lower):
    """Whether a gloss names a place, people or person (a Tory): its first words all capitalised, and none of them a word
    the pack's glosses write in lower case elsewhere (Wine, Baseness, Spanish jasmine)."""
    head = _HEAD_GLOSS.match(re.sub(r"^(a|an|the) ", "", gloss.strip()))
    words = head.group(0).split() if head else []
    if not words or words[0] in _GLOSS_STARTS or not words[0][:1].isupper():
        return False
    return all(w in _CONNECTORS or w[:1].isupper() and w.lower().strip(".'’") not in lower for w in words)


def _origin(text):
    for line in text.split("\n"):
        line = line.strip()
        if _ORIGIN.match(line):
            return line
    return ""


def _read_kinds(db):
    """Fills KINDS from the pack's senses and the dumps' etymologies."""
    for kind in KINDS.values():
        kind.clear()
    entries = defaultdict(list)
    lower = set()
    for word, kind, senses in db.execute("SELECT word_lc, pos, senses FROM entries ORDER BY id"):
        glosses = [g for s in json.loads(senses) for g in s.get("glosses", [])[:1]]
        entries[urdu_fold(word)].append((kind, glosses))
        lower.update(w for g in glosses for w in re.findall(r"\b[a-z][a-z'’]+", g))
    for word, kinds in sorted(entries.items()):
        glosses = [g for _, gs in kinds for g in gs]
        given = [g for g in glosses if _GIVEN.search(g)]
        if given:
            KINDS["given"].add(word)
            if any("Sanskrit" in g for g in given):
                KINDS["hindu_given"].add(word)
        common = [g for kind, gs in kinds if kind != "name" for g in gs]
        if common and all(kind not in ("adj", "verb") for kind, _ in kinds) and all(_proper_gloss(g, lower) for g in common):
            KINDS["proper"].add(word)
    origins = defaultdict(list)
    for path in DUMPS:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                d = json.loads(line)
                text = (d.get("etymology_text") or "").strip()
                if path == DUMPS[0]:
                    spellings = [d.get("word", "")]
                else:
                    spellings = [x["form"].strip() for x in d.get("forms", []) if "Urdu" in (x.get("tags") or [])]
                for spelling in dict.fromkeys(urdu_fold(s) for s in spellings if s):
                    if text:
                        origins[spelling].append(_origin(text))
    KINDS["sanskrit"].update(w for w, found in origins.items() if found and all(o.startswith(_SANSKRIT) for o in found))


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
    before_name = _NAME_PARTS | _TITLES | _WORKS
    GLOSSES.clear()
    GLOSSES.update(glosses)
    _read_kinds(db)
    kinds = ("seen", "verbal", "nominal", "named", "broken", "foreign", "stem_helped", "kar", "perfect_helped", "form_helped",
             "split", "paired")
    usage = {k: {} for k in kinds}
    seen, verbal, nominal, named, broken, alien = (usage[k] for k in kinds[:6])
    stem_helped, kar, perfect_helped, form_helped = (usage[k] for k in ("stem_helped", "kar", "perfect_helped", "form_helped"))
    joined = {}
    for sentence in _sentences():
        words = LETTERS.findall(urdu_fold(sentence))
        last = len(words) - 1
        for i, word in enumerate(words):
            seen[word] = seen.get(word, 0) + 1
            before = words[i - 1] if i else ""
            after = words[i + 1] if i < last else ""
            if after in _VERBAL:
                verbal[word] = verbal.get(word, 0) + 1
                if after != "کر":
                    form_helped[word] = form_helped.get(word, 0) + 1
            if after in _STEM_HELPERS:
                stem_helped[word] = stem_helped.get(word, 0) + 1
            elif after == "کر":
                kar[word] = kar.get(word, 0) + 1
            if after in _PERFECT_HELPERS:
                perfect_helped[word] = perfect_helped.get(word, 0) + 1
            nominal[word] = nominal.get(word, 0) + (after in _AFTER_NOUN) + (before in _BEFORE_NOUN)
            if before in before_name or after in _NAME_PARTS:
                named[word] = named.get(word, 0) + 1
            if (len(after) == 1 and after != "و" and word + after in known) or (
                    len(before) == 1 and before not in "وک" and before + word in known):
                broken[word] = broken.get(word, 0) + 1
            if len(word) > 1 and len(after) > 1 and (word not in _HELPING or after not in _HELPING) and word + after in known:
                joined[(word, after)] = joined.get((word, after), 0) + 1
            if word in foreign.get((word, after), ()) or word in foreign.get((before, word), ()):
                alien[word] = alien.get(word, 0) + 1
    # Two pieces of a word the sentences write whole at least as often as split (زبر دستی, دو ران, مز ید, یو نین, حوا لے); a
    # little word or helper is no piece (کر دی).
    split = usage["split"]
    for (a, b), count in sorted(joined.items()):
        if seen.get(a + b, 0) >= count:
            for piece in (a, b):
                if piece not in _HELPING:
                    split[piece] = split.get(piece, 0) + count
    _read_loan_pieces(usage, known)
    _read_pairs(usage, known, foreign, {w for w, kinds in pos.items() if "num" in kinds})
    return usage


# A word the pack lacks that the sentences have whole LOAN_WHOLE times and more often than a piece of it (English loans:
# آرمی, باولنگ) makes that piece written apart a piece of a word too (آ رمی, باو لنگ), as a pack word's do: a single
# letter, or a piece beside a piece no word of the pack (not a compound's halves: سفارت خانہ, رد عمل).
LOAN_WHOLE = 50


def _read_loan_pieces(usage, known):
    """Counts in usage["broken"] and usage["split"] the uses as pieces of the words LOAN_WHOLE has."""
    seen = usage["seen"]

    def whole(word, piece):
        return word not in known and seen.get(word, 0) >= max(LOAN_WHOLE, seen.get(piece, 0))

    broken, joined = {}, {}
    for sentence in _sentences():
        words = LETTERS.findall(urdu_fold(sentence))
        last = len(words) - 1
        for i, word in enumerate(words):
            if word in _HELPING or word in known and len(word) == 1:
                continue
            before = words[i - 1] if i else ""
            after = words[i + 1] if i < last else ""
            if len(after) == 1 and after != "و" and whole(word + after, word):
                broken[word] = broken.get(word, 0) + 1
            elif len(before) == 1 and before not in "وک" and whole(before + word, word):
                broken[word] = broken.get(word, 0) + 1
            elif len(word) > 1 and len(after) > 1 and after not in known and whole(word + after, word):
                joined[word] = joined.get(word, 0) + 1
            elif len(word) > 1 and len(before) > 1 and before not in known and whole(before + word, word):
                joined[word] = joined.get(word, 0) + 1
    for kind, counts in (("broken", broken), ("split", joined)):
        for word, count in sorted(counts.items()):
            usage[kind][word] = usage[kind].get(word, 0) + count


def _read_pairs(usage, known, foreign, numbers):
    """
    Counts in usage["paired"], for every word of the pack the sentences have SEEN_ENOUGH to PAIRED_MOST times, its uses
    beside the word it stands beside most (not a little word, a helper or itself): when that word is a name, or no word of
    the pack and mostly written beside it (سشما سوراج, تمل ناڈو, گوپی ناتھ, کمل ہاسن), and has it NAME_PAIR_SHARE of the
    time, or any word PAIR_SHARE of the time (رہن سہن, ہاتھا پائی); not a pair the pack has with the word's own sense (سنگ مرمر, شناختی کارڈ), nor a
    number (ایک سو اکیانوے).
    """
    seen = usage["seen"]
    candidates = {w for w, n in seen.items() if SEEN_ENOUGH <= n <= PAIRED_MOST and w in known and w not in _HELPING
                  and w not in numbers}
    near = {}
    for sentence in _sentences():
        words = LETTERS.findall(urdu_fold(sentence))
        for i, word in enumerate(words):
            if word not in candidates:
                continue
            partners = near.setdefault(word, {})
            if i and words[i - 1] not in _HELPING and words[i - 1] != word:
                key = (words[i - 1], word)
                partners[key] = partners.get(key, 0) + 1
            if i + 1 < len(words) and words[i + 1] not in _HELPING and words[i + 1] != word:
                key = (word, words[i + 1])
                partners[key] = partners.get(key, 0) + 1
    names = _NAME_PARTS | _TITLES | EXCLUDE | NOT_WORDS | _FAMILIES
    paired = usage["paired"]
    for word in sorted(near):
        if not near[word]:
            continue
        pair, count = max(near[word].items(), key=lambda item: (item[1], item[0]))
        other = pair[0] if pair[1] == word else pair[1]
        if pair in foreign and word not in foreign[pair]:
            continue
        share = count / seen[word]
        # A word no other word of the pack is beside is a name when the pair is most of its own uses too (سشما سوراج).
        name = other in names or _share_of(usage, "named", other) >= NAMED_SHARE or other not in known and (
                count >= NAME_PAIR_SHARE * seen.get(other, 0) or _share_of(usage, "named", other) >= NAME_PARTNER_SHARE)
        if share >= PAIR_SHARE or name and share >= NAME_PAIR_SHARE:
            paired[word] = count


def _share_of(usage, kind, word):
    seen = usage["seen"].get(word, 0)
    return usage[kind].get(word, 0) / seen if seen >= SEEN_ENOUGH else 0.0


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
    """
    Takes the verbs' ambiguous forms that the sentences use as other words off those verbs (see VERBAL_SHARE), and
    NOT_VERB_FORMS.
    """
    for form, verb in sorted(NOT_VERB_FORMS.items()):
        lemmas[form] = [l for l in lemmas.get(form, []) if l != verb]
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
    # And a verb whose spellings shared with other words (پٹے, پٹا of پٹنا; ترے, ترا "your" of ترنا; پٹائی, چھلنی, بیانی) the
    # sentences together seldom follow with a helper (VERBAL_SHARE) and whose own forms they have under PLAIN_SHARE times as
    # often: of those spellings it keeps only the ones they follow with a helper as often. A spelling only of that verb in a
    # mood no helper follows (بھونیں, تلیں) is its own.
    for verb in sorted(verbs):
        own = _verb_own_forms(verb) - {verb[:-2] + "نی", verb[:-2] + "نیں"}
        shared = [f for f in sorted(set(verbs[verb])) if f not in own and f != verb and f not in OVERRIDES
                  and verb in lemmas.get(f, ()) and not _mood_only(f, verb, pos, lemmas)]
        total = sum(seen.get(f, 0) for f in shared)
        if total < SEEN_ENOUGH or sum(seen.get(f, 0) for f in own) >= PLAIN_SHARE * total:
            continue
        helped = {f: _stem_uses(f) if f == verb[:-2] else USAGE["form_helped"].get(f, 0) for f in shared}
        if sum(helped.values()) >= VERBAL_SHARE * total:
            continue
        for form in shared:
            if seen.get(form, 0) < SEEN_ENOUGH or helped[form] < VERBAL_SHARE * seen[form]:
                lemmas[form] = [l for l in lemmas[form] if l != verb]
    # And a form two verbs share (پکڑا of پکڑنا and پکڑانا, نبھا of نبھنا and نبھانا) is the one's whose own forms the
    # sentences have; not the other's, whose own forms they have under OTHER_VERB_SHARE times as often.
    plain = {verb: sum(seen.get(f, 0) for f in _verb_own_forms(verb)) for verb in verbs}
    for form in sorted(lemmas):
        of = [l for l in lemmas[form] if l in plain and form not in _verb_own_forms(l)]
        if len(of) < 2 or form in OVERRIDES:
            continue
        most = max(plain[v] for v in of)
        lemmas[form] = [l for l in lemmas[form] if l not in of or plain[l] >= OTHER_VERB_SHARE * most]
    # And a verb whose own forms the sentences have under VERB_OWN_FEWEST times is no verb of the spellings it shares with
    # other words of the pack (پوتی "granddaughter" of پوتنا, مسل of مسلنا), nor of those no helper but کر follows
    # (جوتی "shoe" of جوتنا, چالیں of چالنا, اگاہ کیا of اگاہنا); ڈٹ کر, ڈٹے رہے stay ڈٹنا's.
    for verb in sorted(verbs):
        if plain[verb] >= VERB_OWN_FEWEST:
            continue
        for form in sorted(set(verbs[verb]) - _verb_own_forms(verb) - {verb}):
            if form in OVERRIDES or verb not in lemmas.get(form, ()):
                continue
            helped = USAGE["form_helped"].get(form, 0)
            if len(lemmas[form]) > 1 or pos.get(form, set()) - {"verb"} or helped < VERBAL_SHARE * seen.get(form, 0) \
                    or not helped:
                lemmas[form] = [l for l in lemmas[form] if l != verb]


OTHER_VERB_SHARE = 0.1
VERB_OWN_FEWEST = 3


# The endings of a verb's subjunctive and imperatives, which no helper follows (بھونیں, تلیں, ابالیں).
_MOODS = ("یں", "ئیں", "و", "ؤ", "یے", "ئیے", "ئیو", "وں", "ؤں", "جیے", "جئے")


def _mood_only(form, verb, pos, lemmas):
    return form != verb[:-2] and form.endswith(_MOODS) and form not in pos and lemmas.get(form) == [verb]


# A pack word spelled as a verb's stem or perfective that the sentences follow with that verb's helpers VERB_USE_SHARE of
# the time (بھون لیں, کھود کر, سونپ دیا, گھیر لیا, ابال لیں, لٹ گئے; تلی ہوئی), a stem with others than کر STEM_HELPED_SHARE
# of the time, is that verb's form.
VERB_USE_SHARE = 0.4
STEM_HELPED_SHARE = 0.15


def _stem_uses(form):
    return USAGE["stem_helped"].get(form, 0) + USAGE["kar"].get(form, 0)


# The ones mend made so last time, which a new build makes afresh.
_MADE = set()


def send_verb_uses(pos, lemmas):
    """Sends the pack words that the sentences use as a verb's stem or perfective to that verb (VERB_USE_SHARE)."""
    for form in sorted(set(OVERRIDES) & _MADE):
        del OVERRIDES[form]
    _MADE.clear()
    seen = USAGE["seen"]
    of = defaultdict(list)
    for verb in sorted(w for w, kinds in pos.items() if "verb" in kinds):
        for form in set(verb_forms(verb)) - _verb_own_forms(verb):
            of[form].append(verb)
    for form, verbs in sorted(of.items()):
        if form in OVERRIDES or form in NOT_VERB_FORMS or form in EXCLUDE or form in NOT_WORDS \
                or not pos.get(form, set()) & {"noun", "adj"} or seen.get(form, 0) < SEEN_ENOUGH:
            continue
        # The stem of one verb that is the perfective of another goes to the one the helpers after it show.
        stems = [v for v in verbs if form == v[:-2]]
        if len(stems) == 1 and _stem_uses(form) >= VERB_USE_SHARE * seen[form] \
                and USAGE["stem_helped"].get(form, 0) >= STEM_HELPED_SHARE * seen[form]:
            verb = stems[0]
        elif not stems and len(verbs) == 1 and USAGE["perfect_helped"].get(form, 0) >= VERB_USE_SHARE * seen[form]:
            verb = verbs[0]
        else:
            continue
        OVERRIDES[form] = [verb]
        _MADE.add(form)
        if verb not in lemmas[form]:
            lemmas[form].append(verb)


def _variant_key(word):
    """A word as its other spellings write it too: final ہ as ا (مہینہ, مہینا), ؤ as و, ئ as ی, ھ as ہ, آ as ا."""
    if word.endswith("ہ"):
        word = word[:-1] + "ا"
    return word.replace("ؤ", "و").replace("ئ", "ی").replace("ھ", "ہ").replace("آ", "ا")


# A word the sentences mostly (PIECE_SHARE) write as a piece of a word or pair is none (زبر, ید, رت, سہن, جیگوار, ناڈو);
# so is one they have too seldom to tell shares by (SEEN_ENOUGH) but at least PIECE_FEWEST times, mostly as a piece of a
# word a space split (میدا ن, ہد ایت, کپا س, جھو ٹ).
PIECE_SHARE = 0.5
PIECE_FEWEST = 5


def _rare_piece(word):
    seen = USAGE["seen"].get(word, 0)
    pieces = USAGE["broken"].get(word, 0) + USAGE["split"].get(word, 0)
    return seen < SEEN_ENOUGH and pieces >= PIECE_FEWEST and pieces >= PIECE_SHARE * seen


def _piece(form):
    """The share of a form's uses as a piece of a word a space split, of a two-word word of another sense or of a pair."""
    return min(1.0, _share("broken", form) + _share("split", form) + max(_share("foreign", form), _share("paired", form)))


# A word the sentences have (with its forms) fewer than RARE_SEEN times is no word of the list: when all its senses name a
# place, people or person (KINDS), when the pack glosses it as a Sanskrit given name (the news's Indian people: دیپک,
# پریتم, ابھشیک, whose word senses Urdu hardly uses) or as a given name the sentences write beside names GIVEN_NAMED_SHARE of
# the time, and when it is borrowed from Sanskrit (a Hindi word: سنکیت, پرہار, وکاس, ایشور).
RARE_SEEN = 100
GIVEN_NAMED_SHARE = 0.15


def _no_word(word, counted):
    """A name, place or Hindi word (KINDS) that the list leaves out."""
    seen = USAGE["seen"]
    if sum(seen.get(f, 0) for f in set(counted) | {word}) >= RARE_SEEN:
        return False
    return word in KINDS["proper"] or word in KINDS["hindu_given"] or word in KINDS["sanskrit"] \
        or word in KINDS["given"] and _share("named", word) >= GIVEN_NAMED_SHARE


def _merge(forms, into, word):
    for form, share in sorted(forms[word].items()):
        forms[into][form] = forms[into].get(form, 0) + share


def fold(scores, forms, pos):
    """
    After the counting: a word or form counts only its uses that are that word (read_usage), not those as a piece of a word
    a space split (بڑ, خلا, زبر, ران) or of a two-word word of another sense (مون, صلاح) or of a pair or name (سہن, ناڈو,
    سوراج), and a word mostly written so (PIECE_SHARE) is none; a word written mostly beside names (نسیم, طیب, قیصر) is a
    name, and so is a rare word of a name's, place's or Hindi kind (_no_word); two spellings of one word (مہینا, مہینہ: the
    same key, glosses in common; ڈھونڈھنا, ڈھونڈنا) are the commoner one; and NOT_WORDS are left out.
    """
    for word in sorted(scores):
        counted = forms[word]
        for form in sorted(counted):
            piece = _piece(form)
            if piece:
                counted[form] *= 1 - piece
    for word in sorted(scores):
        named = word not in _TITLES and word not in _BETWEEN_NAMES and _share("named", word) >= NAMED_SHARE
        if word in NOT_WORDS or named or _piece(word) >= PIECE_SHARE or _rare_piece(word) or _no_word(word, forms[word]):
            del scores[word]
    keyed = {}
    for word in sorted(scores, key=lambda w: (-sum(forms[w].values()), w)):
        key = _variant_key(word)
        first = keyed.setdefault(key, word)
        if first != word and GLOSSES.get(first, set()) & GLOSSES.get(word, set()):
            _merge(forms, first, word)
            del scores[word]
    # And a word written with a second ھ (ڈھونڈھنا) is its spelling with one, when that is commoner and of a like sense.
    for word in sorted(scores, key=lambda w: (sum(forms[w].values()), w)):
        if word.count("ھ") < 2:
            continue
        spellings = [word[:i] + word[i + 1:] for i, letter in enumerate(word) if letter == "ھ"]
        for other in spellings:
            if other in scores and sum(forms[other].values()) > sum(forms[word].values()) \
                    and GLOSSES.get(other, set()) & GLOSSES.get(word, set()):
                _merge(forms, other, word)
                del scores[word]
                break
    for word in scores:
        scores[word] = sum(forms[word].values())
