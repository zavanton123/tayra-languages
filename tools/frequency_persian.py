"""
Persian for tools/build_frequency_list.py and tools/check_course.py.

The reader keeps the zero-width non-joiner inside a word (می‌روم, کتاب‌ها are one word each) and matches words with ك, ي
and ى as ک and ی, without marks (PersianParser); so do these. The pack's forms table is written without the non-joiner
(میروم, کتابها), so a form is looked up without it, and a form the pack lacks is its word by its endings, never by simplemma,
which files می‌شود under کرد. Without vowels the table also names look-alikes (کوه of که, سیاه of سه, پیر of پدر): a word
with a sense of its own keeps its count, unless PERSIAN_OVERRIDES sends it to a verb it is mostly a form of (کند of کردن).
"""
import json
import os
import re
import sqlite3
from collections import defaultdict

ZWNJ = "‌"
_MARKS = re.compile("[ً-ٰٟـ]")
_LETTERS = str.maketrans({"ك": "ک", "ي": "ی", "ى": "ی", "ۀ": "ه", "ھ": "ه", "ە": "ه"})
_COMPOSED = (("آ", "آ"), ("أ", "أ"), ("إ", "إ"), ("ؤ", "ؤ"), ("یٔ", "ئ"), ("ئ", "ئ"), ("ىٔ", "ئ"))
SCRIPT = re.compile(f"[ء-غف-يپچژکگی{ZWNJ}]+")

# The endings written onto a noun or adjective, longest first, in the spelling without the non-joiner.
_PRONOUNS = ("مان", "تان", "شان", "مون", "تون", "شون", "م", "ت", "ش")
_AFTER_VOWEL_PRONOUNS = ("یمان", "یتان", "یشان", "یمون", "یتون", "یشون", "ام", "ات", "اش", "یم", "یت", "یش")
_NOMINAL_ENDINGS = tuple(sorted(
    {p for p in _PRONOUNS} | {p for p in _AFTER_VOWEL_PRONOUNS} | {"ی", "ای", "یی", "ها", "های", "هایی", "ترین", "تر", "ان", "و", "ه", "ئی"}
    | {"ها" + p for p in _PRONOUNS} | {"های" + p for p in _PRONOUNS} | {"هایم", "هایت", "هایش"},
    key=len, reverse=True))

# A gloss that makes its word another's form names the relation and "of": "Spoken form of آتش", "broken plural of",
# "past participle of", "third-person plural present of", "comparative degree of"; not "person" or "letter" alone.
_FORM_GLOSS = re.compile(r"\b(?:form|plural|singular|inflection|stem|participle|imperative|spelling|contraction|comparative|superlative|"
                         r"degree|misspelling|romanization|transliteration|abbreviation|letter|present|past|indicative|subjunctive|"
                         r"possessive) of\b", re.I)
_OF_WORD = re.compile(r"\bof ([ء-ۿ‌]+)")
_NOT_MODERN = {"archaic", "obsolete", "dated", "rare", "historical", "Classical-Persian", "Dari", "Tajik", "dialectal", "poetic", "Early-New-Persian", "Internet", "uncommon"}
_OWN_KINDS = {"noun", "adj", "verb", "adv", "pron", "prep", "num", "conj", "det", "intj", "particle"}

# The other packs, in the order their words are taken; and how they gloss a plural ("Pluriel de فیلم", "زن 的複數").
_OTHER_PACKS = ("fa-ru.sqlite", "fa-fr.sqlite", "fa-zh.sqlite", "fa-pl.sqlite")
_PLURAL_GLOSS = re.compile(r"pluriel|複數|复数|множ|liczba mnoga|forme|форма", re.I)

OWN = set()
FORM_OF = defaultdict(list)
ROWS = defaultdict(list)
SPELLED = defaultdict(list)
MASS = {}


def persian_fold(word):
    """A word as the reader matches it: Persian letters, no marks, the non-joiner kept."""
    for spread, composed in _COMPOSED:
        word = word.replace(spread, composed)
    return _MARKS.sub("", word.translate(_LETTERS)).strip(ZWNJ)


def bare(word):
    return word.replace(ZWNJ, "")


def _skeleton(word):
    return re.sub("[اآویهئأإؤء]", "", bare(word))


def _related(form, lemma):
    """Whether a noun's pack row is one: its word with endings (کتابها), or the same consonants (کتب, اخبار, مواد)."""
    plain, base = bare(form), bare(lemma)
    if plain.startswith(base) or (base.endswith("ه") and plain.startswith(base[:-1]) and len(base) > 2):
        return True
    return len(_skeleton(lemma)) >= 2 and _skeleton(form) == _skeleton(lemma)


def load(db, pos, lemmas, common):
    """Folds the pack's words, and notes which have a sense of their own, which are glossed as another's form, and its rows."""
    folded_pos = defaultdict(set)
    for word, kinds in pos.items():
        folded_pos[persian_fold(word)] |= kinds
    pos.clear()
    pos.update(folded_pos)
    for word in list(common):
        common.setdefault(persian_fold(word), persian_fold(word))
    verbs, modern_verbs = set(), set()
    for word_lc, p, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        word = persian_fold(word_lc)
        if p not in _OWN_KINDS:
            continue
        for sense in json.loads(senses):
            for gloss in sense.get("glosses", []):
                if _FORM_GLOSS.search(gloss):
                    of = _OF_WORD.search(gloss)
                    if of and persian_fold(of.group(1)) != word and persian_fold(of.group(1)) not in FORM_OF[word]:
                        FORM_OF[word].append(persian_fold(of.group(1)))
                elif not _NOT_MODERN & set(sense.get("tags", [])):
                    OWN.add(word)
                    if p == "verb":
                        modern_verbs.add(word)
        if p == "verb":
            verbs.add(word)
    # An obsolete or dialectal verb (یازیدن, هاسیدن, رفدن) is no word of the list: its table names common words (نیازی, هاست).
    for word in verbs - modern_verbs:
        pos[word].discard("verb")
    forms = defaultdict(set)
    for form, of in lemmas.items():
        forms[bare(persian_fold(form))].update(persian_fold(lemma) for lemma in of)
    for word, kind in _other_words(db, pos, forms).items():
        pos[word].add(kind)
        common.setdefault(word, word)
        OWN.add(word)
    for word in pos:
        if ZWNJ in word:
            SPELLED[bare(word)].append(word)
    for form, of in lemmas.items():
        if not SCRIPT.fullmatch(persian_fold(form)):
            continue
        key = bare(persian_fold(form))
        for lemma in of:
            lemma = persian_fold(lemma)
            if lemma != key and lemma not in ROWS[key]:
                ROWS[key].append(lemma)
    lemmas.clear()


def _other_words(db, pos, forms):
    """
    Words the English pack lacks: PERSIAN_WORDS, and the words the other Persian packs on disk have with a kind of their own
    that the English pack has as no word, no form and no other spelling (پیام, سهام); a phrase, a plural glossed as such or a
    verb's form is none.
    """
    found = dict(PERSIAN_WORDS)
    written = {bare(word) for word in pos}
    path = db.execute("PRAGMA database_list").fetchone()[2]
    for other in _OTHER_PACKS:
        other = os.path.join(os.path.dirname(path), other)
        if not os.path.exists(other):
            continue
        for word_lc, p, senses in sqlite3.connect(other).execute("SELECT word_lc, pos, senses FROM entries ORDER BY id"):
            word = persian_fold(word_lc)
            if p not in _OWN_KINDS - {"verb"} or not SCRIPT.fullmatch(word) or word in pos or bare(word) in forms or word in found:
                continue
            if bare(word) in written or _verb_form(bare(word), forms, pos):
                continue
            if any(_PLURAL_GLOSS.search(g) for sense in json.loads(senses) for g in sense.get("glosses", [])):
                continue
            found[word] = p
    return found


def _verb_form(plain, forms, pos):
    """Whether a word is a verb's form with می or ن before it that the pack's table lacks (میتوان, نداری, می‌بینمت)."""
    for prefix in ("نمی", "می", "ن"):
        rest = plain[len(prefix):]
        if plain.startswith(prefix) and len(rest) > 1 and any("verb" in pos.get(l, ()) for l in forms.get(rest, set()) | forms.get(rest[:-1], set())):
            return True
    return False


def _rows(form, pos):
    """The words the pack rightly names for a form written without the non-joiner: verbs, and nouns it is related to."""
    return [l for l in ROWS.get(form, ()) if "verb" in pos.get(l, ()) or _related(form, l)]


def _mass(word, freq, pos):
    """How common a word is with its forms that are no other word's."""
    if not MASS:
        for form, share in freq.items():
            of = _rows(bare(form), pos)
            MASS[form] = MASS.get(form, 0) + share
            if len(of) == 1 and form not in OWN:
                MASS[of[0]] = MASS.get(of[0], 0) + share
    return MASS.get(word, 0)


def _verbal(plain, pos):
    """The verbs of a verb's form with the pack's table: negated (نمیکنم, نکرد) or with a pronoun after it (میبینمش)."""
    if plain.startswith("نمی") and (of := _rows(plain[1:], pos)):
        return of
    if plain.startswith("ن") and len(plain) > 3 and plain[1:] not in OWN and (of := [l for l in _rows(plain[1:], pos) if "verb" in pos.get(l, ())]):
        return of
    return []


def _stems(plain, written):
    """
    What a word may be with an ending off it, the endings that fit: جایم of جا, خانه‌اش of خانه, not کتابات of کتاب. An
    ending in ا after a vowel (ام, اش, ای) is written after the non-joiner (خانه‌ام, کتابی‌ام): پیام is no پی with ام.
    """
    for ending in _NOMINAL_ENDINGS:
        if not plain.endswith(ending) or len(plain) - len(ending) < 2:
            continue
        stem = plain[:-len(ending)]
        if ending in _AFTER_VOWEL_PRONOUNS and stem[-1] not in "اوهی":
            continue
        if (ending == "ای" or ending in _AFTER_VOWEL_PRONOUNS and ending[0] == "ا") and not written.endswith(ZWNJ + ending):
            continue
        yield ending, stem


def _by_endings(plain, written, is_word, count):
    """The word a form is by the endings on it (کتابش, خانه‌اش, جایم, کتاب‌هایش, بزرگ‌تر), the commonest such word."""
    found = []
    for ending, stem in _stems(plain, written):
        for base in [stem] + SPELLED.get(stem, []) + ([stem + "ه"] if ending in ("ها", "های", "گان") else []):
            if base in OWN and is_word(base) and count(base) >= count(plain):
                found.append(base)
    return max(found, key=count) if found else None


def shares(form, is_word, count, freq, pos):
    """The words a form's count goes to."""
    if not SCRIPT.fullmatch(form):
        return {}
    if form in OWN and is_word(form):
        return {form: 1.0}
    plain = bare(form)
    # Another spelling of a word the pack writes with the non-joiner (هیچکس of هیچ‌کس), or without it (کتابخانه of کتاب‌خانه).
    for spelled in SPELLED.get(plain, []) + [plain]:
        if spelled != form and spelled in OWN and is_word(spelled):
            return {spelled: 1.0}
    targets = [t for t in dict.fromkeys(_rows(plain, pos) + FORM_OF.get(form, []) + _verbal(plain, pos)) if t != form and is_word(t)]
    if not targets:
        if is_word(form):
            return {form: 1.0}
        base = _by_endings(plain, form, is_word, count)
        if base:
            return {base: 1.0}
        for ending in _PRONOUNS:
            if plain.endswith(ending) and len(plain) > len(ending) + 2:
                stem = plain[:-len(ending)]
                verbs = [t for t in _rows(stem, pos) + _verbal(stem, pos) if "verb" in pos.get(t, ()) and is_word(t)]
                if verbs:
                    targets = verbs
                    break
        if not targets:
            return {}
    weights = {t: _mass(t, freq, pos) or 1e-12 for t in targets}
    whole = sum(weights.values())
    return {t: w / whole for t, w in weights.items()}


_checker = {}


def checker_word(word, ranks, dictionary):
    """
    The listed form or word a course's word counts as when the list lacks it as written: the same word with or without the
    non-joiner, a verb's form by the pack's table, or a word with endings on it (کتابش, خانه‌اش, کتاب‌هایش, دیدمش). None if
    it is no such word.
    """
    if not _checker:
        import sqlite3
        db = sqlite3.connect(dictionary)
        pos = defaultdict(set)
        for word_lc, p in db.execute("SELECT word_lc, pos FROM entries"):
            pos[word_lc].add(p)
        lemmas = defaultdict(list)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            lemmas[form].append(lemma)
        load(db, pos, lemmas, {})
        _checker["pos"] = pos
        written = {}
        for key, rank in ranks.items():
            plain = bare(key)
            if plain not in written or rank < ranks[written[plain]]:
                written[plain] = key
        _checker["written"] = written
    pos, written = _checker["pos"], _checker["written"]

    def listed(plain):
        """The listed spelling of a word without the non-joiner, or else the best-ranked word the pack makes it a form of."""
        if plain in written:
            return written[plain]
        of = [l for l in _rows(plain, pos) + _verbal(plain, pos) if l in ranks]
        return min(of, key=ranks.get) if of else None

    plain = bare(word)
    found = listed(plain)
    if found:
        return found
    stems = [found for _, stem in _stems(plain, word) if (found := listed(stem))]
    return min(stems, key=ranks.get) if stems else None


# Read by hand in the top 1,000 (2026-11-07).
# Words no pack on disk has as a word: the pack has بله and پایین only as other spellings of بلی and پائین, and پیام as a name.
PERSIAN_WORDS = {"بله": "intj", "پایین": "adv", "پیام": "noun", "سهام": "noun", "فروش": "noun"}


def _to(word, *forms):
    return {form: [word] for form in forms}


PERSIAN_OVERRIDES = {
    # A verb's past or participle the pack also has as a noun (بود "being", کرد "Kurd", داد "justice", رسید "receipt"),
    # and the spoken forms of a word that have a sense of their own (باشه "OK", کنه "tick", بره "lamb"): the word's.
    **_to("بودن", "بود", "باشه", "هست", "هستن", "نیست", "نبود", "هستی", "نیستی"),
    **_to("کردن", "کرد", "کند", "کنه", "کردی"),
    **_to("دادن", "داد", "داده"),
    **_to("گرفتن", "گرفت"),
    **_to("رسیدن", "رسید", "رسیده"),
    **_to("ماندن", "ماند", "موندن"),
    **_to("توانستن", "توان"),
    **_to("آمدن", "بیا"),
    **_to("رفتن", "برم", "بری", "بره"),
    **_to("دیدن", "دیده"),
    **_to("زدن", "زده"),
    **_to("بردن", "برده"),
    **_to("گفتن", "می‌گم", "میگم"),
    # Spoken spellings and a word with its ending, which the pack has as words of their own.
    **_to("من", "منو", "منه", "مرا", "منی"),
    **_to("شدن", "میشی"),
    **_to("زدن", "میزد"),
    **_to("خوردن", "خوردی"),
    **_to("کجا", "کجایی"),
    **_to("آمدن", "اومدن"),
    **_to("دانستن", "دونستن"),
    **_to("خوردن", "بخور"),
    **_to("سرد", "سرده"),
    **_to("دختر", "دختری"),
    **_to("فیلم", "فیلمی"),
    **_to("پا", "پای"),
    **_to("بو", "بوی"),
    **_to("زن", "زنی"),
    **_to("بزرگ", "بزرگ‌تر"),
    **_to("انتها", "انتهاء", "انتها"),
    **_to("امضا", "امضاء", "امضا"),
    **_to("طلایی", "طلائی", "طلایی"),
    **_to("از", "ازت"),
    **_to("تو", "تویی", "توش"),
    **_to("دوست", "دوس"),
    **_to("افتادن", "افتاده"),
    **_to("خبر", "خبری"),
    **_to("داشتن", "نداری"),
    **_to("توانستن", "میتوان"),
    **_to("برای", "واسه"),
    **_to("به", "بهت", "بهم"),
    **_to("این", "اینه", "اینا"),
    **_to("عزیز", "عزیزم"),
    **_to("دل", "دلم"),
    **_to("همه", "همش"),
    **_to("ممنون", "ممنونم"),
    **_to("خدا", "خدای"),
    **_to("حال", "حالی"),
    **_to("مرد", "مردی"),
    **_to("آره", "اره"),
    **_to("باران", "بارون"),
    **_to("زبان", "زبون"),
    **_to("نان", "نون"),
    **_to("رویا", "رؤیا", "رویا"),
    **_to("باز", "بازم"),
    **_to("بخش", "بخشی"),
    **_to("میلیون", "ملیون"),
    **_to("کار", "کاری", "کارم", "کاره", "کاریش", "کاریم", "کاریت", "کارین"),
    **_to("نشان", "نشون", "نشونه", "نشونت", "نشونم", "نشونش", "نشونتون", "نشونمون", "نشونشون", "نشونی", "نشونیش", "نشونیم", "نشونن"),
    # The standard spelling as the word: پایین, بله.
    **_to("پایین", "پائین", "پایین"),
    **_to("بله", "بلی", "بله"),
    **_to("ابتدا", "ابتداء", "ابتدا"),
    **_to("مگر", "مگه"),
    **_to("خانه", "خونه"),
    **_to("کار", "کارا"),
    # Words of their own that the pack also has as another's form.
    **_to("کو", "کو"),
    **_to("سهام", "سهام"),
    **_to("فروش", "فروش"),
    **_to("بهتر", "بهتر"),
}

# Wikipedia's own words, letters, a clipped مگه, and people's names.
PERSIAN_EXCLUDE = {"ویکی‌پدیا", "ویکی", "ویل", "اس", "جی", "ان", "تی", "اف", "مک", "لی", "لا", "ال", "ا", "هات", "مگ", "علی", "حسن", "رضا", "احمد", "محمد", "حسین", "مهدی", "کیوان", "کارن",
                   # Literary verbs whose tables name common words (بدنی, هلند, سرش, بیارید, بدرد, تکی, نمکی).
                   "دنیدن", "هشتن", "سرشتن", "یارستن", "دریدن", "پالیدن", "رستن", "تنیدن", "رندیدن", "تکیدن", "چمیدن", "ژولیدن",
                   "پوییدن", "سریدن", "مکیدن"}
