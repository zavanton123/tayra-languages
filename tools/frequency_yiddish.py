"""
The Yiddish forms the yi-en pack's forms table lacks, for build_frequency_list. The pack keeps only the forms its word
pattern accepts, which drops every pointed one (קאָנטינענטן, אויפֿגעהערט): a noun's plural, a verb's participles and most
of an adjective's endings went to no word. They come from wty's inflection tables (the dump the pack is made from), and
where it has none, from the regular paradigms: a noun's plural in -ן, -ען, -ס, -עס, -ים, -ות or -לעך, a verb's present
(not its bare stem), participles and zu-infinitive (with a separable prefix: אַרויסגעגאַנגען, אָנצוקומען), an adjective's
endings.

A form goes to its word only when the counts have it, pointed or not (קאָנטינענטן or קאנטינענטן), and nothing else claims it:
no entry of the pack, no form its table or wty gives another word, no other word's paradigm. Forms are written in YIVO
spelling, as the list writes words (points after their letters, ay as ײַ, ey as two yods).
"""
import json
import os
import re
import zipfile
from collections import defaultdict

# A form of wty's tables written in Hebrew letters (not its romanisations), the maqaf and gereshes allowed.
LETTERS = re.compile("[\u05b0-\u05f4\u05be]+")
PRONOUNS = {"איך", "דו", "ער", "זי", "עס", "מיר", "איר", "זיי", "מען"}
# The tags of a word's own inflections; spellings, dialect forms and derived words are other words' (an alternative
# spelling, a Daytshmerish one, a diminutive, the female equivalent).
INFLECTION = re.compile(r"plural|participle|present|imperative|person|nominative|accusative|dative|masculine|feminine|neuter"
                        r"|comparative|superlative|predicative|inflected", re.I)
OTHER_WORD = re.compile(r"alternative|alt-of|dialect|daytshmerish|northeastern|litvish|poylish|standard|diminutive|female"
                        r"|abbreviation|endearing|uncommon|humorous|obsolete|archaic|clitic|contract|morpheme|independent"
                        r"|internet|pejorative|rare", re.I)

# Spellings of a form that are mostly another word: the preposition אויסער "besides", not אויס's; the nouns שאָדן "damage",
# עסקן "activist", משפּט "trial", דרוק "print", לעכער "holes", not a plural or a verb's stem; the Beatles.
APART = {"אויסער", "שאָדן", "עסקן", "משפּט", "דרוק", "לעכער", "ביטלס", "קליפּס", "קינאס"}

FINAL = {"כ": "ך", "מ": "ם", "נ": "ן", "צ": "ץ"}
NONFINAL = {v: k for k, v in FINAL.items()}
VOWEL_END = ("ע", "י", "ו", "\u05b7", "\u05b8", "\u05b4")
# The separable prefixes, which take גע and צו after them (אַרויסגעגאַנגען, אָנצוקומען), as they are spelled inside a word.
SEPARABLE = ("אַוועק", "אַהיים", "אַהער", "אַהין", "אַדורכ", "אַרויס", "אַרויפֿ", "אַרײַנ", "אַראָפּ", "אַריבער", "אַרום",
             "אַנטקעגנ", "פֿונאַנדער", "פֿאַרבײַ", "צוזאַמענ", "צונויפֿ", "צוריק", "אויס", "אויפֿ", "אונטער", "איבער", "אײַנ",
             "אָפּ", "אָנ", "בײַ", "דורכ", "מיט", "נאָכ", "פֿאָר", "צו")
# The inseparable ones, which take no גע (באַשלאָסן, פֿאַרשטאַנען).
INSEPARABLE = ("באַ", "גע", "דער", "אַנט", "פֿאַר", "צע")


def at_end(stem):
    """[stem] as a word ends: final letters (קום, מאַך, לויף)."""
    if stem.endswith("פֿ"):
        return stem[:-2] + "ף"
    if stem and stem[-1] in FINAL:
        return stem[:-1] + FINAL[stem[-1]]
    return stem


def inside(word):
    """[word] with an ending after it: its final letter in its plain shape (ך to כ, ף to פֿ)."""
    if word.endswith("ף"):
        return word[:-1] + "פֿ"
    if word and word[-1] in NONFINAL:
        return word[:-1] + NONFINAL[word[-1]]
    return word


def verb_stem(infinitive):
    """The stem endings are joined to (קומ of קומען, מאַכ of מאַכן, גיי of גיין), or None."""
    if infinitive.endswith("ען"):
        stem = infinitive[:-2]
        if stem.endswith(("מ", "נ", "נג", "נק")) or stem.endswith(VOWEL_END):
            return stem
    if infinitive.endswith("ן") and len(infinitive) > 2:
        return infinitive[:-1]
    return None


# wty's parts of speech, as the pack names them.
KINDS = {"n": "noun", "v": "verb", "adj": "adj"}
ALTERNATIVE = re.compile(r"^(alternative|alt-of)( (common|uncommon|spelling|form))?$")


class Table:
    """wty's inflection tables: each one-word form with the words it is an inflection of, the kinds of word each table is
    for, and the plain alternative spellings of words (ראַטעוון of ראַטעווען)."""

    def __init__(self, path, normalise):
        self.forms, self.kinds, self.alternatives = defaultdict(set), defaultdict(set), defaultdict(set)
        if not os.path.exists(path):
            return
        with zipfile.ZipFile(path) as z:
            rows = [row for name in sorted(z.namelist()) if name.startswith("term_bank") for row in json.loads(z.read(name))]
        for term, _reading, tags, rules, _score, definitions, *_ in rows:
            if tags != "non-lemma":
                continue
            for definition in definitions:
                if not isinstance(definition, list) or len(definition) < 2 or not LETTERS.fullmatch(definition[0]):
                    continue
                lemma, kinds = normalise(definition[0]), " ".join(definition[1])
                words = term.replace("(", " ").replace(")", " ").split()
                if ALTERNATIVE.match(kinds) and len(words) == 1 and LETTERS.fullmatch(words[0]):
                    self.alternatives[normalise(words[0])].add(lemma)
                if not INFLECTION.search(kinds) or OTHER_WORD.search(kinds):
                    continue
                # A present with its pronoun (איך קום), an imperative with it after (קום (דו)); not a separable verb's
                # present (איך הער אויף), whose pieces are other words' to the reader.
                if len(words) == 2 and (words[0] in PRONOUNS) != (words[1] in PRONOUNS):
                    words = [w for w in words if w not in PRONOUNS]
                if len(words) != 1 or not LETTERS.fullmatch(words[0]):
                    continue
                self.forms[normalise(words[0])].add(lemma)
                if rules in KINDS:
                    self.kinds[lemma].add(KINDS[rules])


def participles(verb, participle_of):
    """
    The past participle a verb may have, by its prefix: a separable one before the participle of the verb after it
    (אַרויס + געגאַנגען), an inseparable one in place of its גע (באַ + שלאָסן); else the regular one (געמאַכט, שטודירט).
    """
    for prefix in SEPARABLE:
        rest = verb[len(prefix):]
        if verb.startswith(prefix) and len(rest) > 2:
            if rest in participle_of:
                return [prefix + p for p in participle_of[rest]]
            stem = verb_stem(rest)
            return [prefix + "גע" + stem + ("" if stem.endswith("ט") else "ט")] if stem else []
    for prefix in INSEPARABLE:
        rest = verb[len(prefix):]
        if verb.startswith(prefix) and len(rest) > 2:
            strong = [prefix + p[2:] for p in participle_of.get(rest, ()) if p.startswith("גע")]
            if strong:
                return strong
            stem = verb_stem(verb)
            return [stem + ("" if stem.endswith("ט") else "ט")] if stem else []
    stem = verb_stem(verb)
    if not stem:
        return []
    if verb.endswith("ירן"):
        return [stem + "ט"]
    return ["גע" + stem + ("" if stem.endswith("ט") else "ט")]


def verb_forms(verb, participle_of):
    stem = verb_stem(verb)
    if not stem:
        return []
    forms = participles(verb, participle_of) + [inside(verb) + "דיק"]
    separable = next((p for p in SEPARABLE if verb.startswith(p) and len(verb) - len(p) > 2), None)
    if separable:
        # Its present is written apart (ער הערט אויף), but its zu-infinitive whole (אויפֿצוהערן).
        return forms + [separable + "צו" + verb[len(separable):]]
    # Not its bare stem (גלייך of גלייכן), which is mostly some other word.
    return forms + [stem + ("ט" if stem.endswith(("ס", "ז", "צ")) else "סט"), stem if stem.endswith("ט") else stem + "ט"]


def noun_forms(noun):
    stem = inside(noun)
    forms = [stem + "ן", stem + "ס", stem + "עס", stem + "ים"]
    if not stem.endswith("ע"):
        forms.append(stem + "ען")
    if noun.endswith("ה"):
        forms.append(noun[:-1] + "ות")
    if noun.endswith("ל"):
        forms.append(noun + "עך")
    return forms


def adjective_forms(adjective):
    stem = inside(adjective)
    plain_n = stem.endswith(("מ", "נ", "נג", "נק")) or stem.endswith(VOWEL_END)
    forms = [stem + "ע", stem + "ער", stem + ("עם" if plain_n else "ן"), stem + "עם", stem + "ס"]
    most = stem + ("ט" if stem.endswith(("ס", "ז", "צ", "ש")) else "סט")
    return forms + [most, most + "ע", most + "ער", most + "ן"]


def forms(freq, pos, lemmas, is_word, overrides, excluded, unpointed, wty_path, normalise):
    """
    The forms the counts have that the pack's table lacks ({form: word}), for lemmas, and the unpointed spellings of
    them that no pointed word of the pack has ({spelling: form}).
    """
    table = Table(wty_path, normalise)
    participle_of = defaultdict(list)
    for form, of in table.forms.items():
        for lemma in of:
            if form.startswith("גע") and lemma.endswith("ן") and form != lemma and not form.endswith("דיק"):
                participle_of[lemma].append(form)

    def word_of(lemma, depth=0):
        """The list's word for one of wty's: itself, or the word it is a form or plain spelling of (בעסט of גוט)."""
        if is_word(lemma) or depth > 2:
            return lemma
        onward = {word_of(l, depth + 1) for l in table.forms.get(lemma, set()) | table.alternatives.get(lemma, set())}
        onward = {l for l in onward if is_word(l)}
        return onward.pop() if len(onward) == 1 else lemma

    claims = defaultdict(set)
    for form, of in table.forms.items():
        # Not as the form of a word the pack has no entry for (געגאַנגען of געגאַן, besides גיין).
        claims[form].update(word for word in map(word_of, of) if pos.get(word))

    def claim(form, word):
        # A word the pack writes as the list does not (ייַ for ײַ) claims in the list's spelling, as that word where the pack
        # has it too.
        form, written = normalise(form), normalise(word)
        if form != word:
            claims[form].add(written if pos.get(written) and is_word(written) else word)

    # The unpointed spellings the pack has for some pointed word or form are that one's (yiddish_spellings).
    taken = {unpointed(w) for w in list(pos) + list(lemmas) if pos.get(w) or lemmas.get(w)}
    # A kind of word wty has a table for has its forms from it; the others, the regular ones, but not a word the pack also
    # has pointed (מאכן of מאַכן), whose forms are the pointed one's.
    variants = {unpointed(w) for w in pos if pos.get(w) and unpointed(w) != w}
    for word in [w for w in pos if pos[w] and w not in variants and is_word(w)]:
        kinds = pos[word] - table.kinds.get(word, set())
        made = []
        if "verb" in kinds and word.endswith("ן") and " " not in word:
            made += verb_forms(word, participle_of)
        if "noun" in kinds:
            made += noun_forms(word)
        if "adj" in kinds and not word.endswith(("ע", "ן")):
            made += adjective_forms(word)
        for form in made:
            claim(form, word)
    # wty's tables lack a separable verb's zu-infinitive (אָנצוקומען, אויפֿצוהערן), and some give a prefixed verb a weak
    # participle its verb does not have (אָנגערופֿט for אָנגערופֿן, as רופֿן's is גערופֿן).
    for word in [w for w in pos if "verb" in table.kinds.get(w, ()) and "verb" in pos[w] and is_word(w)]:
        separable = next((p for p in SEPARABLE if word.startswith(p) and len(word) - len(p) > 2), None)
        if separable:
            claim(separable + "צו" + word[len(separable):], word)
        prefix = separable or next((p for p in INSEPARABLE if word.startswith(p) and len(word) - len(p) > 2), None)
        if prefix and participle_of.get(word[len(prefix):]):
            for form in participles(word, participle_of):
                claim(form, word)

    def seen(form):
        return freq.get(form, 0) > 0 or (unpointed(form) != form and unpointed(form) not in taken and freq.get(unpointed(form), 0) > 0)

    added = {}
    for form, of in claims.items():
        if len(of) != 1 or form in APART or pos.get(form) or lemmas.get(form) or form in taken or form in overrides or form in excluded:
            continue
        (word,) = of
        if is_word(word) and word not in excluded and seen(form):
            added[form] = word
    spelled = defaultdict(set)
    for form in added:
        if unpointed(form) != form:
            spelled[unpointed(form)].add(form)
    spelled = {u: next(iter(of)) for u, of in spelled.items()
               if len(of) == 1 and u not in taken and u not in claims and u not in overrides and u not in excluded}
    return added, spelled
