"""
The Tamil frequency list (build_frequency_list.py --language ta).

Tamil writes a word with its case endings, plural, clitics and verb endings joined on (வீட்டில், மரங்களை, செய்கிறேன்,
செய்துவிட்டார்), with sounds between them (பள்ளி + ஐ is பள்ளியை, அம்மா + உம் is அம்மாவும், மரம் + ஐ is மரத்தை), and a
k, c, t or p doubled onto a word before the next (பள்ளிக்குப் போனான்). The ta-en pack's tables give most nouns' cases and
most verbs' persons and tenses (from the English Wiktionary's ta-decl and ta-conj templates), but none of the forms built on
a verb's stems (செய்த, செய்து, செய்தால், செய்வது, செய்யப்பட்டது, செய்திருக்கிறார்), the formal written endings
(செய்கின்றனர், செய்தனர்) or the clitics, nor does simplemma know Tamil. So those are made here from each verb's table, as
the Kannada list makes its forms by rule (frequency_kannada), and a corpus form is counted for the words it is a form of.
A form that is a word of the pack itself keeps its count (ஆனால் "but", not ஆ's "if it becomes"), unless TAMIL_FORMS files it.
"""
import functools
import json
import os
import re
import sqlite3
import unicodedata
from collections import defaultdict

# As the reader's Tamil wordCharacters, without the Tamil digits and number signs (numbers are no words).
TAMIL_LETTERS = re.compile("[஀-௥‌‍]+")
CONSONANTS = {chr(c) for c in range(0x0B95, 0x0BBA)}
VIRAMA = "்"
# The vowels and the signs they are written with after a consonant (அ is the consonant's own vowel).
SIGNS = {"அ": "", "ஆ": "ா", "இ": "ி", "ஈ": "ீ", "உ": "ு", "ஊ": "ூ", "எ": "ெ", "ஏ": "ே", "ஐ": "ை", "ஒ": "ொ", "ஓ": "ோ", "ஔ": "ௌ"}
VOWELS = set(SIGNS)
# After these a vowel joins with the glide ய (பள்ளியை, தலையில்); after the others with வ (அம்மாவை, பூவும்).
FRONT = set("ிீெேை") | set("இஈஎஏஐ")
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp"}
NOMINAL = {"noun", "num", "pron"}
FORMS_LISTED = 400


def join(stem, tail):
    """[stem] with [tail] joined on as Tamil writes it: a vowel replaces a final virama or short u (மரத்த் + இல் is
    மரத்தில், எனக்கு + உம் is எனக்கும்), and joins a vowel with a glide (பள்ளி + ஐ is பள்ளியை, அம்மா + ஐ is அம்மாவை)."""
    if not stem or not tail or tail[0] not in VOWELS:
        return stem + tail
    sign, rest = SIGNS[tail[0]], tail[1:]
    last = stem[-1]
    if last == VIRAMA:
        return stem[:-1] + sign + rest
    if last == "ு" and len(stem) > 2 and stem[-2] in CONSONANTS:
        return stem[:-1] + sign + rest
    if last in FRONT:
        return stem + "ய" + sign + rest
    return stem + "வ" + sign + rest


def vowel_final(form):
    return bool(form) and form[-1] != VIRAMA


# A noun's endings past what its table gives: the formal dative -இற்கு, the locative's and ablative's clitics, the
# adverbial and adjectival -ஆக and -ஆன ("as", "being": அழகாக, அழகான), the ablative's spoken -இல் இருந்து written whole,
# and "about, with, without" written on.
OBLIQUE_TAILS = ["இற்கு", "இற்காக", "இன்", "இல்", "இலும்", "இலே", "இலேயே", "இடம்", "இடமும்", "இடமிருந்து", "ஓடு", "உடன்",
                 "உடைய", "ஆல்", "இலிருந்து", "இலிருந்தும்", "ஐ", "ஐயும்", "ஐயே", "உக்கு", "உக்கும்", "உக்கே", "உக்காக", "இனால்",
                 "இனுடைய", "இனை", "இல்லை", "இலா", "ஐப்போல", "இனும்", "இனுள்", "இனூடாக"]
BARE_TAILS = ["ஆக", "ஆன", "ஆகும்", "ஆகவும்", "ஆனது", "ஆனவர்", "ஆனவை", "ஆகவே", "ஆகத்", "ஐ", "உம்", "ஏ", "ஆ", "ஓ", "இல்லாத",
              "இல்லாமல்", "அற்ற"]


def oblique(word):
    """The stems a noun's cases are joined to, by its ending: மரம் மரத்த், வீடு வீட்ட் (and வீட்), கண் கண்ண் (and கண்)."""
    if word.endswith("ம்") and len(word) > 2:
        return {word[:-2] + "த்த்"}
    if word.endswith(("டு", "று")) and len(word) > 2:
        doubled = word[:-2] + word[-2] + VIRAMA + word[-2] + VIRAMA
        return {doubled, word}
    if word[-1] == VIRAMA and len(word) == 2 or (len(word) == 3 and word[-1] == VIRAMA and word[0] in CONSONANTS and word[1] in "ிுெொ"):
        # A short syllable's consonant doubles before a vowel (கண்ணை, பல்லை, பெண்ணை).
        return {word + word[-2] + VIRAMA, word}
    return {word}


def plural(word):
    if word.endswith("ம்") and len(word) > 2:
        return word[:-2] + "ங்கள்"
    return word + "கள்"


@functools.lru_cache(maxsize=None)
def nominal_forms(word):
    """A noun's (or number's, pronoun's) cases, plural and clitics by rule."""
    forms = set()
    for stem in oblique(word) | {plural(word)}:
        forms |= {join(stem, t) for t in OBLIQUE_TAILS}
    # A noun in -அம் has a short oblique before -இன் and -இல் too (உலகின், உலகில்), but for one in -யம் (நிலையில் is நிலை's).
    if word.endswith("ம்") and len(word) > 4 and word[-3] not in "யவ":
        forms |= {join(word[:-2] + VIRAMA, t) for t in ("இன்", "இல்", "இலும்", "இலே", "இலிருந்து")}
    forms.add(plural(word))
    forms |= {join(word, t) for t in BARE_TAILS}
    forms |= {join(plural(word), t) for t in ("உம்", "ஏ", "ஆ", "ஆக", "ஆன", "ஆகவும்")}
    # A noun in -அம் stands bare before another (உலக நாடுகள், மாவட்ட ஆட்சியர்).
    if word.endswith("ம்") and len(word) > 4:
        forms.add(word[:-2])
    # The dative after a front vowel and -ம் nouns' short dative (பள்ளிக்கு, மரத்துக்கு is made above).
    if word[-1] in FRONT:
        forms |= {word + "க்கு", word + "க்கும்", word + "க்காக", word + "க்குப்"}
    return forms


# The person endings of a verb's tenses, as its stems take them (செய்த + ஏன், செய்கிற + ஆர்கள்).
PERSONS = ["ஏன்", "ஆய்", "ஆன்", "ஆள்", "ஆர்", "அது", "ஓம்", "ஈர்கள்", "ஆர்கள்", "அன", "ஈர்", "ஆய", "அனர்"]
# Endings on the past and future stems that make nouns, conditionals and participles: செய்தது, செய்ததை, செய்தால்,
# செய்தவர்; செய்வது, செய்வதற்கு.
NOUN_TAILS = ["து", "தில்லை", "தை", "தில்", "தற்கு", "தற்காக", "தால்", "தும்", "தோடு", "துடன்", "தன்", "தனால்", "தாக", "தான", "துதான்",
              "தையும்", "தே", "தா", "தோ"]
PAST_TAILS = ["ால்", "ாலும்", "ாலே", "வர்", "வர்கள்", "வருக்கு", "வரை", "வரின்", "வர்களுக்கு", "வர்களை", "வர்களின்", "வள்",
              "வன்", "வை", "போது", "பின்", "பிறகு", "ும்"]
FUTURE_TAILS = ["ர்", "ர்கள்", "ோம்", "ேன்", "ாய்", "ான்", "ாள்", "ீர்கள்", "ன"]
# On the infinitive (செய்ய): the negative stem's forms (செய்யாமல், செய்யாத), the permissive, the optative, obligation.
INFINITIVE_TAILS = ["லாம்", "லாமா", "லாமே", "ட்டும்", "ட்டுமா", "வில்லை", "வேண்டும்", "வேண்டாம்", "வேண்டிய", "வேண்டி",
                    "க்கூடாது", "க்கூடிய", "க்கூடும்", "வும்", "வே", "ாமல்", "ாத", "ாதது", "ாதவர்", "ாதவர்கள்", "ாதே",
                    "ாதீர்கள்", "ாது", "ாவிட்டால்", "ாவிட்டாலும்", "ாதா", "ாததால்", "ாததை", "ாதபோது", "மாட்டேன்", "மாட்டாய்",
                    "மாட்டான்", "மாட்டாள்", "மாட்டார்", "மாட்டோம்", "மாட்டீர்கள்", "மாட்டார்கள்", "முடியும்", "முடியாது",
                    "முடியவில்லை", "த்", "ச்", "ப்", "க்"]
# The perfect with உள் (செய்துள்ளார்), the formal written one.
UL_FORMS = ["உள்ளது", "உள்ளார்", "உள்ளனர்", "உள்ளன", "உள்ள", "உள்ளேன்", "உள்ளோம்", "உள்ளாய்", "உள்ளீர்கள்", "உள்ளார்கள்",
            "உள்ளான்", "உள்ளாள்", "உள்ளதாக", "உள்ளதால்", "உள்ளதை", "உள்ளவர்", "உள்ளவர்கள்", "உள்ளதும்", "உள்ளனர்"]


def table_stems(word, table):
    """A verb's stems as its table shows them, as (present, past, future, infinitive) tuples, one for each a table of
    several verbs of one spelling has (படு "suffer" பட்ட, "lie down" படுத்த). The present is the relative participle's
    (செய்கிற), the past the past stem (செய்த, எழுதின, போன), the future's (செய்வ, படிப்ப)."""
    found = {"present": [], "past": [], "future": [], "infinitive": []}
    for form in sorted(table):
        if form.endswith("கிறேன்"):
            found["present"].append(form[:-3])
        elif form.endswith("ேன்") and not form.endswith(("மாட்டேன்", "கின்றேன்")):
            stem = form[:-3]
            kind = "future" if stem.endswith(("வ", "ப")) and not stem.endswith(("ந்த", "ட்ட", "ற்ற", "த்த")) else "past"
            found[kind].append(stem)
        elif form.endswith("வில்லை"):
            found["infinitive"].append(form[:-len("வில்லை")])
    rows = max(len(v) for v in found.values())
    return [tuple(found[k][i] if i < len(found[k]) else None for k in ("present", "past", "future", "infinitive"))
            for i in range(rows)]


def ruled_stems(word):
    """The stems of a verb in -u the pack has no table for, conjugated as எழுது (எழுதுகிற, எழுதின, எழுதுவ, எழுத)."""
    if word.endswith("ு") and len(word) > 2 and word[-2] in CONSONANTS:
        return word + "கிற", word[:-1] + "ின", word + "வ", word[:-1]
    return None, None, None, None


def all_stems(word, table):
    """Every set of stems [word] is conjugated with: its table's, and for a verb in -i, -e or -ai the other class's too, as
    Wiktionary files the weak and the strong verb under one headword (அமைந்த "formed" and அமைத்த "made")."""
    found = table_stems(word, table)
    if not any(s[0] or s[1] for s in found):
        found = [s for s in [ruled_stems(word)] if s[0]]
    if word[-1] in "ிைெ" and len(word) > 1:
        weak = (word + "கிற", word + "ந்த", word + "வ", word + "ய")
        strong = (word + "க்கிற", word + "த்த", word + "ப்ப", word + "க்க")
        pasts = {s[1] for s in found}
        found += [s for s in (weak, strong) if s[1] not in pasts]
    return found


def gerund(past):
    """The verbal participle by the past stem: செய்த செய்து, எழுதின எழுதி, போன போய்."""
    if past.endswith("ின"):
        return [past[:-1]]
    if past.endswith("ன") and len(past) > 1 and past[-2] != VIRAMA:
        return [past[:-1] + "ய்"]
    return [past + "ு"]


@functools.lru_cache(maxsize=None)
def simple_verb_forms(word, table):
    """A verb's forms on its stems, without the compounds with another verb."""
    forms = set(table)
    # The future relative with போது "when" written on (வரும்போது).
    forms |= {f + "போது" for f in table if f.endswith("ும்")}
    for stems in all_stems(word, table):
        forms |= _stem_forms(word, *stems)
    forms.discard("")
    return frozenset(forms)


def _stem_forms(word, present, past, future, infinitive):
    forms = set()
    if present:
        forms |= {present, present + "வர்", present + "வர்கள்", present + "து", present + "போது"}
        formal = present[:-1] + "ன்ற"
        forms |= {formal, formal + "னர்", formal + "ன", formal + "து", formal + "வர்"}
        forms |= {present + SIGNS[t[0]] + t[1:] for t in PERSONS}
        forms |= {formal + SIGNS[t[0]] + t[1:] for t in PERSONS}
    if past:
        forms |= {past} | {past + t for t in PAST_TAILS} | {past + t for t in NOUN_TAILS}
        forms |= {past + SIGNS[t[0]] + t[1:] for t in PERSONS}
        forms.add(past + "ர்" if past.endswith("ின") else past + "னர்")
        if past.endswith("ின"):
            forms |= {past[:-1] + "ய", past[:-1] + "யது", past[:-1] + "யதை", past[:-1] + "யவர்", past[:-1] + "யவர்கள்"}
        forms |= set(gerund(past))
        forms |= {g + "ம்" for g in gerund(past) if g.endswith("ு")}
    if future:
        forms |= {future + t for t in NOUN_TAILS} | {future + t for t in FUTURE_TAILS}
        # The verbal noun in -தல் (செய்தல், படித்தல்).
        forms.add(word + ("த்தல்" if future.endswith("ப்ப") else "தல்"))
    if infinitive:
        forms |= {infinitive} | {infinitive + t for t in INFINITIVE_TAILS}
    return forms


HELPERS = ("இரு", "விடு", "கொள்", "படு", "போ")


def verb_forms(word, table, helpers):
    """A verb's forms with its compounds: the perfect and progressive with இரு and உள் (செய்திருக்கிறார், செய்துள்ளார்,
    செய்துகொண்டிருக்கிறார்), the completive விடு (செய்துவிட்டார்), the reflexive கொள் (செய்துகொண்டார்) and the passive
    with படு (செய்யப்பட்டது)."""
    forms = set(simple_verb_forms(word, table))
    for present, past, future, infinitive in all_stems(word, table):
        forms |= _compound_forms(word, past, infinitive, helpers)
    return forms


def _compound_forms(word, past, infinitive, helpers):
    forms = set()
    if past:
        for g in gerund(past):
            forms |= {join(g, f) for f in helpers["இரு"] | set(UL_FORMS)}
            for helper in ("விடு", "கொள்"):
                forms |= {g + f for f in helpers[helper]}
                forms |= {g + f[0] + VIRAMA + f for f in helpers[helper] if f[0] in "கசதப"}
            forms |= {g + "க்கொண்டு", g + "கொண்டு", g + "க்கொண்டிருக்கிறது", g + "கொண்டிருக்கிறது",
                      g + "க்கொண்டிருந்தது", g + "கொண்டிருந்தது", g + "க்கொண்டிருந்தார்", g + "கொண்டிருந்தார்",
                      g + "க்கொண்டிருக்கிறார்", g + "கொண்டிருக்கிறார்", g + "க்கொண்டிருந்தான்", g + "கொண்டிருந்தான்",
                      g + "க்கொண்டிருந்தாள்", g + "கொண்டிருந்தாள்", g + "க்கொண்டிருக்கிறேன்", g + "கொண்டிருக்கிறேன்"}
    if infinitive and word != "படு":
        forms |= {infinitive + "ப்" + f for f in helpers["படு"]}
    return forms


def unclitic(form):
    """The forms [form] may be with a clitic joined on: -உம் "also, and", -ஏ (emphasis), -ஆ (a question), -ஓ (doubt) and
    -தான் "only, indeed": வீட்டிலும் of வீட்டில், எனக்கும் of எனக்கு, அம்மாவும் of அம்மா, அவனே of அவன்."""
    bases = set()
    if form.endswith("தான்") and len(form) > 5:
        bases.add(form[:-4])
    if form.endswith("ும்") and len(form) > 4:
        lead = form[:-3]
    elif form[-1] in "ோோ" and len(form) > 2:
        lead = form[:-1]
    else:
        return bases
    if lead[-1] in CONSONANTS:
        bases |= {lead + VIRAMA, lead + "ு"}
        if lead[-1] == "ய" and len(lead) > 2 and lead[-2] in FRONT:
            bases.add(lead[:-1])
        if lead[-1] == "வ" and len(lead) > 2 and lead[-2] not in FRONT and lead[-2] != VIRAMA:
            bases.add(lead[:-1])
    return bases


def unsandhi(form):
    """The form without the consonant doubled onto it before the next word (பள்ளிக்குப், வீட்டைப், செய்யச்)."""
    if len(form) > 3 and form[-1] == VIRAMA and form[-2] in "கசதப" and form[-3] != VIRAMA:
        return {form[:-2]}
    return set()


def bases(form):
    """What a corpus form may be without its sandhi consonant and its clitic."""
    out = set()
    for plain in unsandhi(form) | {form}:
        if plain != form:
            out.add(plain)
        for base in unclitic(plain):
            out.add(base)
            out |= unclitic(base)
    return out


# Words the pack lacks, or has only as a form, with their kinds and forms by hand ("other" takes no endings).
TAMIL_WORDS = {
    "ஒரு": ("det", ""),
    # Everyday words the pack lacks.
    "தற்போது": ("adv", "தற்போதைய"), "ஏனெனில்": ("conj", ""), "ஏதாவது": ("pron", ""), "ஏதேனும்": ("pron", ""),
    "யாராவது": ("pron", "யாரையாவது யாருக்காவது"), "இன்னொரு": ("det", ""), "மற்றொரு": ("det", ""), "சிறிது": ("adv", ""),
    "இவ்வளவு": ("adv", ""), "அவ்வளவு": ("adv", ""), "எவ்வளவு": ("adv", ""), "இதுவரை": ("adv", ""), "ஒருவேளை": ("adv", ""),
    "ஒருபோதும்": ("adv", ""), "ஒருமுறை": ("adv", ""), "அனைவர்": ("pron", "அனைவரும் அனைவருக்கும் அனைவரையும் அனைவரின்"),
    "எல்லோர்": ("pron", "எல்லோரும் எல்லோருக்கும் எல்லோரையும் எல்லோரின்"), "இருவர்": ("pron", "இருவரும் இருவருக்கும் இருவரையும்"),
    "மற்றவர்": ("noun", ""), "வயது": ("noun", ""), "மனைவி": ("noun", ""),
    "அடிப்படை": ("noun", ""), "பட்டியல்": ("noun", ""), "ஆதரவு": ("noun", ""), "புகார்": ("noun", ""), "வீரர்": ("noun", ""),
    "சகோதரர்": ("noun", ""), "எழுத்தாளர்": ("noun", ""), "இளைஞர்": ("noun", ""), "தொழிலாளர்": ("noun", ""),
    "செயலாளர்": ("noun", ""),
    "ஆ": ("verb", "ஆகிறது ஆகிறார் ஆகிறேன் ஆகின்றன ஆகின்றது ஆனது ஆனார் ஆனேன் ஆனான் ஆனாள் ஆனோம் ஆனார்கள் ஆனன ஆயிற்று ஆகும் ஆவார் "
                  "ஆவேன் ஆவான் ஆவாள் ஆவோம் ஆவது ஆவதற்கு ஆவதால் ஆவதை ஆகாது ஆகவில்லை ஆகமாட்டேன் ஆகமாட்டார் ஆகாமல் ஆகாத ஆக "
                  "ஆகக்கூடாது ஆகலாம் ஆகட்டும் ஆகி ஆகிய ஆகியது ஆகியோர் ஆகியவை ஆகியவற்றை ஆகிவிட்டது ஆகிவிட்டார் ஆகிவிட்டேன் "
                  "ஆகியுள்ளது ஆகியுள்ளார் ஆகியிருக்கிறது ஆகியிருந்தது ஆன ஆனவர் ஆனதால் ஆனதும் ஆனதை ஆனபோது ஆகிக்கொண்டிருக்கிறது "
                  "ஆய் ஆயின ஆயினர் ஆனர் ஆகுக ஆகிறோம் ஆகின்றனர் ஆவதில்லை ஆகாதது ஆகவே"),
}
# Forms the pack files under a word they are no form of, or the rules would, and two spellings of one word, by hand:
# form -> word.
TAMIL_FORMS = {
    "ஆம்": "ஆம்", "ஆகஸ்ட்": "ஆகஸ்டு", "இல்ல": "இல்லை", "நாட்டு": "நாடு", "கோவில்": "கோயில்",
    # The pronouns' formal genitives in -அது and accusatives in -அனை.
    "தனது": "தான்", "தமது": "தாம்", "எமது": "யாம்", "எம்": "யாம்", "எம்மை": "யாம்", "எமக்கு": "யாம்", "அவரது": "அவர்",
    "அவனது": "அவன்", "அவளது": "அவள்", "அவர்களது": "அவர்கள்", "இவரது": "இவர்", "இவனது": "இவன்", "இவளது": "இவள்",
    "இவர்களது": "இவர்கள்", "இதனை": "இது", "அதனை": "அது", "எதனை": "எது", "இதனையும்": "இது", "அதனையும்": "அது",
}
for _word, _forms in {
    "உள்": "உள்ளது உள்ள உள்ளனர் உள்ளன உள்ளார் உள்ளேன் உள்ளோம் உள்ளாய் உள்ளீர்கள் உள்ளார்கள் உள்ளான் உள்ளாள் உள்ளதாக உள்ளதால் உள்ளதை "
           "உள்ளவர் உள்ளவர்கள் உள்ளதும் உள்ளவை உள்ளவற்றை",
}.items():
    TAMIL_FORMS.update({_form: _word for _form in _forms.split()})
# Kinds the pack gives a word that its forms would be wrongly made by: நீ "you" as a verb (நீக்க is நீக்கு's).
DROPPED_KINDS = {("நீ", "verb"), ("உள்", "verb")}
# Pack links that are wrong (ஒரு is its own word, not ஒன்று's form), as (form, lemma).
WRONG_LINKS = {("ஒரு", "ஒன்று"), ("ஆம்", "ஆ"), ("ஆ", "ஆம்"), ("பல்", "பல"), ("மா", "அம்மா")}
# Words whose tables are another's again (the literary யான் "I" declines as நான்): their forms are that word's.
TABLELESS = {"யான்"}
SINGULARS = {"அவன்": "அவர்கள்", "அவள்": "அவர்கள்", "அவர்": "அவர்கள்", "இவன்": "இவர்கள்", "இவள்": "இவர்கள்", "இவர்": "இவர்கள்"}
# Words of their own that are also forms of another, which keep their count however common that one is: the quotative and
# the postpositions and conjunctions made of verb forms (என்று "that", கொண்டு "with", பற்றி "about", விட "than"), and the
# adverbs of place.
KEEP = {"என்று", "என", "என்ற", "என்பது", "எனும்", "என்னும்", "என்றால்", "ஆனால்", "கொண்டு", "பற்றி", "தொடர்ந்து", "குறித்து", "விட",
        "கூட", "போது", "மூலம்", "வரை", "பிறகு", "பின்னர்", "மீது", "உடன்", "அதனால்", "இல்லாமல்", "கூடாது", "வேண்டும்",
        "வேண்டாம்", "உண்டு", "சென்ற", "அருகில்", "முதலில்", "சுற்றி", "நோக்கி", "தவிர", "உள்ளே", "வெளியே", "கீழே", "மேலே",
        "முன்பு", "பின்பு", "இல்லை", "போல", "போன்ற", "மட்டும்", "எனவே", "எனினும்", "மேலும்", "அதாவது", "திரும்ப", "இனி",
        "ஓர்",
        # The pronouns, which the pack's tables give as one another's forms (நாம் of நான், அவர்கள் of அவர்).
        "நான்", "நாம்", "நாங்கள்", "நீ", "நீங்கள்", "அவன்", "அவள்", "அவர்", "அவர்கள்", "இவன்", "இவள்", "இவர்", "இவர்கள்",
        "அது", "இது", "அவை", "இவை", "தான்", "தாம்", "தாங்கள்", "யார்", "எது", "எவை"}
# Pack words the list leaves out: letters, case endings written apart after a number or a Latin word (2020-இல்), and
# abbreviations.
NOT_WORDS = {"பா", "கோ", "ரா", "கோவை", "கள்", "ஆல்", "இன்", "உம்", "எல்", "அல்", "ஆன்", "அம்", "மு", "ரூ", "அ", "இ", "உ", "எ", "ஒ", "க", "ச", "த", "ப", "ம", "ந", "வ", "இல்", "ஐ", "ஆம்ப", "கி", "மீ", "செ", "டி", "எஸ்", "பி", "ஜி",
             "எம்", "கே", "சி", "ஏ", "ஆர்"}


EVERYDAY_NAME = re.compile(r"\blanguage\b|month of the|day of the week|^(January|February|March|April|May|June|July|August|"
                           r"September|October|November|December) \(")


def lexicon(dictionaries):
    """The pack's words by kind, its tables (form -> words) and the words that are only names."""
    db = sqlite3.connect(os.path.join(dictionaries, "ta-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        word = unicodedata.normalize("NFC", word)
        if not TAMIL_LETTERS.fullmatch(word):
            continue
        # A language's, a month's or a weekday's name is an everyday word (தமிழ், மார்ச், திங்கள்); other names are left out.
        if pos == "name" and any(EVERYDAY_NAME.search(g) for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS and (word, pos) not in DROPPED_KINDS:
            kinds[word].add(pos)
    for word, (kind, _) in TAMIL_WORDS.items():
        # A polite noun in -அர் is the pack's -அன் noun's form (word_forms), when the pack has that.
        if not (word.endswith("ர்") and "noun" in kinds.get(word[:-2] + "ன்", ())):
            kinds[word].add(kind)
    table = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        form, lemma = unicodedata.normalize("NFC", form), unicodedata.normalize("NFC", lemma)
        if lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS or not TAMIL_LETTERS.fullmatch(form):
            continue
        # A singular pronoun's table gives the plural's forms too (அவர்களை under அவன்): they are the plural's.
        if lemma in SINGULARS and form.startswith(SINGULARS[lemma][:-1]):
            continue
        # உள்'s table is made as a verb's (உண்டேன் is உண்'s): its forms are TAMIL_FORMS'.
        if lemma == "உள்" or lemma in TABLELESS:
            continue
        table[form].add(lemma)
    polite_known.clear()
    polite_known.update(kinds)
    return kinds, table, names - set(kinds)


def helper_forms(table_of):
    """The helpers' own forms, with the perfect on their participle (பட்டுள்ளது, விட்டிருக்கிறார்)."""
    helpers = {}
    for h in HELPERS:
        table = frozenset(table_of.get(h, ()))
        forms = set(simple_verb_forms(h, table)) | {h}
        for _, past, _, _ in all_stems(h, table):
            for g in gerund(past) if past else ():
                forms |= {join(g, f) for f in UL_FORMS}
        helpers[h] = frozenset(forms)
    simple_ru = simple_verb_forms("இரு", frozenset(table_of.get("இரு", ())))
    for h in HELPERS:
        if h != "இரு":
            for _, past, _, _ in all_stems(h, frozenset(table_of.get(h, ()))):
                for g in gerund(past) if past else ():
                    helpers[h] = helpers[h] | {join(g, f) for f in simple_ru}
    return helpers


# The pack's words, which word_forms makes no polite form of a noun into (அமைச்சர் is a word of its own beside அமைச்சன்).
polite_known = set()


def word_forms(word, ks, table_of, helpers):
    """Every form of [word] of kinds [ks]: its table's, and those the rules make."""
    forms = set(table_of.get(word, ()))
    if word in TAMIL_WORDS:
        forms |= set(TAMIL_WORDS[word][1].split())
    if ks & NOMINAL or "adj" in ks and word[-1] == VIRAMA:
        forms |= nominal_forms(word)
        # A noun of a person in -அன் has its polite form in -அர் (மாணவன் மாணவர், தலைவன் தலைவர்).
        polite = word[:-2] + "ர்"
        if "noun" in ks and word.endswith("ன்") and len(word) > 3 and word[-3] in CONSONANTS and polite not in polite_known:
            forms |= {polite} | nominal_forms(polite)
    elif ks & {"adj", "adv"}:
        forms |= {join(word, t) for t in ("ஆக", "ஆன", "ஆகவும்", "ஆகவே", "உம்", "ஏ")}
    # Ordinals (இரண்டாவது, இரண்டாம்).
    if "num" in ks:
        forms |= {join(word, t) for t in ("ஆவது", "ஆம்", "ஆவதாக", "ஆவதை")}
    if "verb" in ks:
        forms |= verb_forms(word, frozenset(table_of.get(word, ())), helpers)
    forms.discard(word)
    return forms


def build(found, size, label, weights=None):
    """The list: the pack's words, by their forms' counts in [found], each source weighted by [weights] (by name)."""
    dictionaries = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
    kinds, table, names = lexicon(dictionaries)
    counts = defaultdict(float)
    for name, source, _ in found:
        total = sum(source.values())
        weight = (weights or {}).get(name, 1 / len(found))
        for form, n in source.items():
            form = unicodedata.normalize("NFC", form)
            if TAMIL_LETTERS.fullmatch(form):
                counts[form] += n / total * weight

    table_of = defaultdict(set)
    for form, lemmas in table.items():
        for lemma in lemmas:
            table_of[lemma].add(form)
    helpers = helper_forms(table_of)
    needed = set(counts)
    for form in counts:
        needed |= bases(form)
    # The forms the rules and tables make, and of them those of verbs and pronouns, which a word of its own may be
    # (வந்து of வா, என்னை of நான்); a noun's accusative may not (காலை "morning" is no கால் + ஐ, நாளை "tomorrow" none of நாள்).
    made, verbal = defaultdict(set), defaultdict(set)
    for word, ks in kinds.items():
        for form in word_forms(word, ks, table_of, helpers):
            if form in needed:
                made[form].add(word)
        if ks & {"verb", "pron"}:
            for form in (verb_forms(word, frozenset(table_of.get(word, ())), helpers) if "verb" in ks else set()) | table_of.get(word, set()):
                if form in needed:
                    verbal[form].add(word)
    # A name's spellings and case forms are no words (இலங்கை, மதுரையில்), unless a table has them as a word's form.
    name_forms = set()
    for name in names:
        name_forms |= {name} | (nominal_forms(name) & needed)

    def plain_heads(form):
        """(the word the form is itself, or None; the words it is a form of)."""
        if form in TAMIL_FORMS:
            return None, {TAMIL_FORMS[form]}
        if form in NOT_WORDS:
            return None, set()
        own = form if form in kinds else None
        if own in KEEP or own and "num" in kinds[own]:
            return own, set()
        if own and form.endswith("ை"):
            return own, verbal.get(form, set()) - {form}
        return own, made.get(form, set()) - {form}

    def nameless(form):
        return form in NOT_WORDS or form in name_forms and form not in kinds and form not in table

    direct = {form: (None, set()) if nameless(form) else plain_heads(form) for form in counts}
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, others) in direct.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]

    @functools.lru_cache(maxsize=None)
    def decide(form):
        """The words [form] is counted for. A word of the pack that is also a form of another (வந்து of வா, என்னை of நான்)
        is that word's form when the word is the commoner by its own forms; otherwise it keeps its count. A form with a
        clitic or a doubled consonant the rules do not make (வந்துதான், என்னைப்) goes where the form without it goes."""
        own, others = direct[form] if form in direct else plain_heads(form)
        share = counts.get(form, 0.0)
        # A noun of its own whose plural and cases the corpus has too keeps its count (பாடல் "song", not பாடு's "singing";
        # கட்டிடம் "building", not கட்டு's).
        if own and others and (max(alone.get(o, 0.0) for o in others) <= share
                               or "noun" in kinds[own] and alone.get(own, 0.0) >= share / 10):
            return frozenset({own})
        if own or others:
            return frozenset(others or {own})
        heads = set()
        for base in sorted(bases(form)):
            if not nameless(base):
                heads |= decide(base)
        return frozenset(heads)

    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, share in counts.items():
        heads = set() if nameless(form) else decide(form)
        if not heads:
            continue
        weights_of = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights_of.values())
        for head in heads:
            part = weights_of[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    ranked = sorted(scores, key=lambda w: -scores[w])[:size]
    # A noun of a person whose polite form in -அர் is written more than its own (அமைச்சர், தலைவர்) is listed under that form.
    for i, word in enumerate(ranked):
        polite = word[:-2] + "ர்" if word.endswith("ன்") and len(word) > 3 else None
        if polite and polite not in scores and forms[word].get(polite, 0.0) > forms[word].get(word, 0.0):
            ranked[i] = polite
            forms[polite] = forms.pop(word)
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


# The checker (check_course.py) reads a word the pinned list does not have by these rules: செய்திருக்கிறேன் is செய்'s,
# பள்ளிக்குப் பள்ளி's and ராணியிடம் the name ராணி's.
_CHECKER = {}


def _checker_lexicon():
    if not _CHECKER:
        dictionaries = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
        kinds, table, _ = lexicon(dictionaries)
        table_of = defaultdict(set)
        for form, lemmas in table.items():
            for lemma in lemmas:
                table_of[lemma].add(form)
        _CHECKER.update(kinds=kinds, table_of=table_of, helpers=helper_forms(table_of), index={})
    return _CHECKER


@functools.lru_cache(maxsize=None)
def _rule_forms(word):
    lex = _checker_lexicon()
    ks = lex["kinds"].get(word) or {"noun"}
    return frozenset(word_forms(word, ks, lex["table_of"], lex["helpers"])) | {word}


@functools.lru_cache(maxsize=None)
def _name_forms(name):
    return frozenset(nominal_forms(name)) | {name}


def checker_word(token, words, ranks, names=frozenset()):
    """
    The list's word [token] is a form of by the rules, as a key of [words] (the checker's list: key -> (rank, word, forms)),
    the commonest when it may be several; "\\0name" when it is a case form of one of the course's [names]; or None.
    A form with a clitic or a doubled consonant after it (வீட்டிலும், பள்ளிக்குப்) is read as the form without it.
    """
    candidates = {token} | bases(token)
    for base in sorted(candidates - {token}):
        if base in ranks:
            return base
    for name in names:
        if name.startswith("\u0000"):
            continue
        if any(c.startswith(name[:2]) for c in candidates) and candidates & _name_forms(name):
            return "\u0000name"
    lex = _checker_lexicon()
    index = lex["index"].get(id(words))
    if index is None:
        index = defaultdict(set)
        for key in words:
            for start in {key} | lex["table_of"].get(key, set()) | set(TAMIL_WORDS.get(key, ("", ""))[1].split()):
                index[start[:2]].add(key)
        lex["index"] = {id(words): index}
    found = []
    for candidate in candidates:
        for key in index.get(candidate[:2], ()):
            if candidate in _rule_forms(key):
                found.append((words[key][0], key))
    return min(found)[1] if found else None
