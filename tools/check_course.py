#!/usr/bin/env python3
"""
Checks a frequency-list course (tools/courses/<code>/<id>.json) against the language's word
frequency list, lesson by lesson, so generated lessons stay readable at their level:

- coverage: the share of a lesson's words ranked within the course's rankUpTo (names in the
  course's "names", and numbers, count as known); words outside are listed with their ranks;
- new words: each is a word of the course's band (ranks rankUpTo-99 .. rankUpTo), new in the
  course, and used at least twice in its lesson, counting its forms;
- length: words per lesson within the course's range.

Usage: python3 tools/check_course.py tools/courses/pt/pt-mini-0100.json [--brief]
Exit status 0 when every lesson passes.
"""
import json
import math
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LISTS = os.path.join(ROOT, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency")
WORD = re.compile(r"[^\W\d_]+(?:['’][^\W\d_]+)*", re.UNICODE)
BAND = 100


def rules(rank_up_to):
    """Coverage and length a lesson needs: the first courses have few words to work with."""
    if rank_up_to <= 100:
        return 0.90, 80, 180
    if rank_up_to <= 300:
        return 0.93, 120, 240
    if rank_up_to <= 600:
        return 0.95, 180, 320
    if rank_up_to <= 1000:
        return 0.95, 220, 380
    if rank_up_to <= 2000:
        return 0.95, 260, 450
    if rank_up_to <= 3000:
        return 0.95, 300, 520
    if rank_up_to <= 4000:
        return 0.95, 320, 550
    return 0.95, 350, 600


GREEK_ACCENTED = {"ά": "α", "έ": "ε", "ή": "η", "ί": "ι", "ό": "ο", "ύ": "υ", "ώ": "ω", "ΐ": "ϊ", "ΰ": "ϋ"}
TURKISH_CIRCUMFLEX = str.maketrans("âîû", "aiu")


# OpenCC's tables, as the app bundles them, for reading Chinese in simplified characters.
OPENCC = os.path.join(ROOT, "core", "ui", "src", "commonMain", "composeResources", "files", "opencc")
_to_simplified = []


def to_simplified(text):
    """[text] in simplified characters, as the app's OpenCC tables convert it (TSPhrases, then TSCharacters)."""
    if not _to_simplified:
        for name in ("TSPhrases.txt", "TSCharacters.txt"):
            table = {}
            for line in open(os.path.join(OPENCC, name), encoding="utf-8"):
                key, _, values = line.rstrip("\n").partition("\t")
                if key and values:
                    table[key] = values.split(" ")[0]
            _to_simplified.append((table, max(map(len, table))))
    out, i = [], 0
    while i < len(text):
        for table, longest in _to_simplified:
            match = next((text[i:i + n] for n in range(min(longest, len(text) - i), 0, -1) if text[i:i + n] in table), None)
            if match:
                out.append(table[match])
                i += len(match)
                break
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def _is_kanji(c):
    return "\u3400" <= c <= "\u9fff" or "\uf900" <= c <= "\ufaff"


def fold(code, word):
    # Chinese courses are written in simplified characters; the list's words are traditional, with their simplified forms.
    if code == "zh":
        return to_simplified(word)
    # The German list folds ß to ss in most of its forms, so both spellings are compared folded.
    if code == "de":
        return word.replace("ß", "ss")
    # The French, Italian and Dutch lists write the straight apostrophe; texts may use the curly one.
    if code in ("fr", "it", "nl"):
        return word.replace("’", "'")
    # Russian texts for learners write ё where it belongs, while the list holds both spellings of most words.
    if code == "ru":
        return word.replace("ё", "е")
    # The Ukrainian list writes the apostrophe straight (м'ясо); texts may use the curly one or the modifier letter (м’ясо, мʼясо).
    if code == "uk":
        return word.replace("’", "'").replace("ʼ", "'")
    # Turkish texts may write â, î, û (hâlâ, dükkân, millî) where the list mostly has a, i, u; and a
    # capital İ lowercased by Python keeps a combining dot.
    if code == "tr":
        return word.replace("̇", "").translate(TURKISH_CIRCUMFLEX)
    # A Greek word stressed on the third syllable from the end takes a second accent before an enclitic
    # pronoun (όνομά μου, τηλέφωνό της): the word is the one with the first accent only.
    # Each part of a hyphenated pair keeps its own accent (άνω-κάτω, σιγά-σιγά).
    if code == "el" and "-" in word:
        return "-".join(fold(code, part) for part in word.split("-"))
    if code == "el" and sum(1 for c in word if c in GREEK_ACCENTED) > 1:
        last = max(i for i, c in enumerate(word) if c in GREEK_ACCENTED)
        return word[:last] + GREEK_ACCENTED[word[last]] + word[last + 1:]
    return word


def load_list(code):
    ranks, words = {}, {}
    # A language's courses may pin the list they were written against, so a rebuilt app list
    # (which can reorder ranks) does not move their bands.
    pinned = os.path.join(ROOT, "tools", "courses", code, "wordlist.tsv")
    path = pinned if os.path.exists(pinned) else os.path.join(LISTS, f"{code}.tsv")
    with open(path, encoding="utf-8") as f:
        rank = 0
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            rank += 1
            word, _, forms = line.rstrip("\n").partition("\t")
            key = fold(code, word.lower())
            forms = [fold(code, form) for form in forms.split()]
            # A Japanese kanji form that shares no kanji with its word is another word read alike (秋 of 時).
            if code == "ja" and any(_is_kanji(c) for c in key):
                forms = [f for f in forms if not any(_is_kanji(c) for c in f) or any(c in key for c in f if _is_kanji(c))]
            if key in words:
                # The same word listed twice once ё and е are one letter (ученый, учёный): the commoner entry stays.
                first_rank, first_word, first_forms = words[key]
                words[key] = (first_rank, first_word, first_forms + [f for f in forms if f not in first_forms])
            else:
                words[key] = (rank, word, forms)
            for form in [key] + forms:
                ranks[form] = min(ranks.get(form, rank), rank)
    # A Japanese word that is an entry of its own is that entry, not a form of a commoner one (できる, not する).
    if code == "ja":
        for key, (rank, _, _) in words.items():
            ranks[key] = rank
    # Forms the list misses, kept per language next to its courses (tools/courses/<code>/extra_forms.tsv).
    extra = os.path.join(ROOT, "tools", "courses", code, "extra_forms.tsv")
    if os.path.exists(extra):
        for line in open(extra, encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            word, _, forms = line.rstrip("\n").partition("\t")
            key = fold(code, word.lower())
            if key not in words:
                print(f"warning: {extra}: '{word}' is not a headword of the list; its forms are ignored", file=sys.stderr)
                continue
            rank, headword, known = words[key]
            added = [fold(code, form) for form in forms.split()]
            words[key] = (rank, headword, known + [f for f in added if f not in known])
            for form in added:
                ranks[form] = min(ranks.get(form, rank), rank)
    return ranks, words


# As SegmentingParser has them: the numbers and demonstratives a measure word follows, and the measure words.
COUNTING = "一二两三四五六七八九十百千万几这那哪每"
MEASURES = "个個本只隻位次天名家件张張条條种種句双雙块塊杯碗岁歲点點辆輛台層层门門份节節场場篇首遍"
# A number in characters, with a time character or a measure word the list joins to it (八点, 十年, 一道, 第二).
CHINESE_MEASURES = "点半号岁年月日天周个次道本只位张条件名家种句双块杯碗辆台层门份节场篇首遍口群片封部棵支根套座间所头箱盒包瓶袋盘串排把顿笔扇枚粒颗幅"
CHINESE_NUMBER = re.compile(r"第?[零〇一二三四五六七八九十百千万亿两]+[" + CHINESE_MEASURES + "]?")
CJK_RUN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]+")


def chinese_tokens(text, ranks):
    """
    Chinese words as the app's reader cuts them (SegmentingParser): each run of characters into its
    likeliest words, those of the frequency list; a character no word covers is a word of its own.
    Letters outside the characters are words as usual.
    """
    longest = min(16, max((len(w) for w in ranks), default=1))
    out = []
    for match in re.finditer(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]+|[^\W\d_]+", text):
        run = match.group(0)
        if not CJK_RUN.fullmatch(run):
            out.append(run)
            continue
        # The likeliest cut, as SegmentingParser.likeliest makes it: each word costs the log of its rank plus 4,
        # a character no word covers is a word of its own at rank 100,000.
        best = [0.0] + [math.inf] * len(run)
        start = [0] * (len(run) + 1)
        for end in range(1, len(run) + 1):
            for i in range(max(0, end - longest), end):
                # After a number or a demonstrative, a measure word goes with it (两个人 is 两 + 个 + 人).
                if end - i > 1 and i > 0 and run[i - 1] in COUNTING and run[i] in MEASURES:
                    continue
                rank = ranks.get(fold("zh", run[i:end]))
                if rank is None:
                    if end - i > 1:
                        continue
                    rank = 100_000
                cost = best[i] + math.log(rank) + 4.0
                if cost < best[end]:
                    best[end], start[end] = cost, i
        words, end = [], len(run)
        while end > 0:
            words.append(run[start[end]:end])
            end = start[end]
        out.extend(reversed(words))
    return out


# A word the checker counts as known whatever its rank: a name, a number.
KNOWN = "\u0000name"
_mecab = []


def _tagger():
    """MeCab with IPADIC, the dictionary of the Kuromoji the desktop reader cuts Japanese with."""
    if not _mecab:
        try:
            import MeCab
            import ipadic
        except ImportError:
            sys.exit("Japanese courses need MeCab: run the checker with a Python that has mecab-python3 and ipadic installed")
        _mecab.append(MeCab.Tagger(ipadic.MECAB_ARGS))
    return _mecab[0]


# What a name the checker has matched is left as, so the text around it is cut as it would be around the name.
JA_NAME = "鈴木"
# A number written as one word with its counter (一つ, 三時, 十二月).
JA_NUMBER = re.compile(r"[一二三四五六七八九十百千万〇零]+(?:つ|時|月|日|年|人|歳|回|個|本|枚|匹|分|円|階|週間|か月|ヶ月|番|度|台|冊|杯)?")
# The particles that join a verb to the one helping it (住んでいる, 食べてしまう), and -たり, as KuromojiBoundaries has them.
JA_JOINING = {"て", "で", "ちゃ", "じゃ", "たり", "だり"}


def ja_continues(pos, detail, surface, previous):
    """KuromojiBoundaries.continues: whether a piece belongs to the word before it."""
    if previous is None or previous == "記号":
        return False
    after_inflecting = previous in ("動詞", "形容詞", "助動詞")
    if pos == "助動詞":
        return after_inflecting
    if pos == "動詞" and detail in ("非自立", "接尾"):
        return after_inflecting or (detail == "接尾" and previous == "名詞")
    if pos == "形容詞" and detail in ("非自立", "接尾"):
        return after_inflecting
    if pos == "名詞" and detail == "接尾":
        return previous == "名詞"
    if pos == "助詞" and detail in ("接続助詞", "並立助詞") and surface in JA_JOINING:
        return after_inflecting
    return False


def japanese_tokens(text, ranks):
    """
    Japanese words as the desktop reader cuts them: MeCab's pieces joined into words as
    KuromojiBoundaries joins them (食べ・まし・た is 食べました), and within a word the longest run of
    pieces the list knows, a verb or adjective with what follows it read as its dictionary form, as the
    reader looks 食べました up as 食べる. Each word is given as the form the list ranks: 食べる for
    食べました. A number in kanji with its counter (三人, 五つ) is [KNOWN], as digits are not words.
    """
    out = []
    node = _tagger().parseToNode(text)
    pieces = []
    while node:
        if node.surface:
            f = node.feature.split(",")
            pieces.append((node.surface, f[0], f[1], f[2], f[6] if len(f) > 6 and f[6] != "*" else node.surface))
        node = node.next
    position = {id(piece): n for n, piece in enumerate(pieces)}
    # Words: runs of pieces, each joined to the one before as the reader joins them.
    words, previous = [], None
    for piece in pieces:
        surface, pos, detail = piece[0], piece[1], piece[2]
        if words and ja_continues(pos, detail, surface, previous):
            words[-1].append(piece)
        else:
            words.append([piece])
        previous = "動詞" if words[-1][-1] is piece and len(words[-1]) > 1 and pos == "助詞" else pos
    for word in words:
        i = 0
        while i < len(word):
            head = word[i]
            if head[1] == "記号" or not re.search(r"[^\W\d_]", head[0]):
                i += 1
                continue
            # さん, ちゃん, 様 and 君 right after a name go with the name (not 君 after 「マキさん！」).
            before = pieces[position[id(head)] - 1] if position[id(head)] else None
            if head[2] == "接尾" and head[3] == "人名" and before and (before[0] == JA_NAME or before[3] == "人名"):
                out.append(KNOWN)
                i += 1
                continue
            if head[0] == JA_NAME:
                i += 1
                continue
            if head[2] == "数" and head[0] != "何":
                # A number and the counters after it.
                j = i + 1
                while j < len(word) and (word[j][2] == "数" or word[j][3] == "助数詞"):
                    j += 1
                out.append(KNOWN)
                i = j
                continue
            end, lemma = i + 1, head[4]
            whole = "".join(p[0] for p in word[i:])
            if head[1] in ("動詞", "形容詞", "助動詞") and (head[4] in ranks or whole in ranks):
                end = len(word)
                # The commoner of the dictionary form and the word as written (ください is 下さい, not 下さる).
                lemma = min((x for x in (head[4], whole) if x in ranks), key=ranks.get)
            else:
                for j in range(len(word), i, -1):
                    if "".join(p[0] for p in word[i:j]) in ranks:
                        end, lemma = j, "".join(p[0] for p in word[i:j])
                        break
            written = "".join(p[0] for p in word[i:end])
            # Grammar the list lacks (によって, という, らしい, the ん of のだ) is grammar, never an outside word.
            unlisted = lemma not in ranks and head[0] not in ranks
            if JA_NUMBER.fullmatch(written) or unlisted and (head[1] in ("助詞", "助動詞") or head[0] == "ん" and head[2] == "非自立"):
                out.append(KNOWN)
            else:
                out.append(lemma if lemma in ranks or head[0] not in ranks else head[0])
            i = end
    return out


def tokens(text, code=None, ranks=None, names=frozenset(), verbs=None, heads=None):
    if code == "ja":
        return japanese_tokens(text, ranks)
    if code == "zh":
        return chinese_tokens(text, ranks)
    if code == "fr":
        return french_tokens(text, ranks)
    if code == "es":
        return spanish_tokens(text, ranks, names, verbs)
    if code in ("ru", "uk"):
        return russian_tokens(text)
    if code == "it":
        return italian_tokens(text, ranks, names, verbs, heads)
    if code == "nl":
        return dutch_tokens(text)
    if code in ("da", "no", "sv", "el"):
        return danish_tokens(text, ranks)
    if code == "tr":
        return turkish_tokens(text)
    if code == "ko":
        return korean_tokens(text, ranks, names, heads)
    out = []
    for match in WORD.finditer(text):
        out.extend(part for part in re.split(r"-", match.group(0)) if part)
    return out


# Dutch writes some short words with an apostrophe in front: 's ochtends (des), 't (het), 'n (een),
# 'k (ik), 'm (hem). They count as the words they stand for; an apostrophe inside a word (auto's,
# zo'n, m'n) keeps the word whole, as the list has it.
DUTCH_WORD = re.compile(r"(?<![^\W\d_])['’](?:s|t|n|k|m)(?![^\W\d_])|[^\W\d_]+(?:['’][^\W\d_]+)*", re.UNICODE)
DUTCH_CLITICS = {"'s": "des", "'t": "het", "'n": "een", "'k": "ik", "'m": "hem"}


def dutch_tokens(text):
    out = []
    for match in DUTCH_WORD.finditer(text):
        word = match.group(0).replace("’", "'")
        clitic = DUTCH_CLITICS.get(word.lower())
        if clitic:
            out.append(clitic)
            continue
        out.extend(part for part in word.split("-") if part)
    return out


# A Danish, Norwegian or Swedish word joined by a hyphen stays whole when the list has it whole (e-mail, id-kort, p-plads);
# otherwise its parts count apart.
DANISH_WORD = re.compile(r"[^\W\d_]+(?:['’-][^\W\d_]+)*", re.UNICODE)


def danish_tokens(text, ranks):
    out = []
    for match in DANISH_WORD.finditer(text):
        word = match.group(0)
        if "-" in word and word.lower() not in ranks:
            out.extend(part for part in word.split("-") if part)
        else:
            out.append(word)
    return out


GREEK_ELIDED = {"σ": "σε", "τ": "το", "μ": "με", "απ": "από", "γι": "για", "ν": "να", "θ": "θα"}
GREEK_ELISION = re.compile(r"(?<![^\W\d_])(σ|τ|μ|απ|γι|ν|θ)['’](?=\s*[^\W\d_])", re.IGNORECASE)


ORDINAL_ENDINGS = {"e", "er", "re", "ère", "es", "ers", "res", "ème", "èmes", "nd", "nde", "nds", "ndes"}
FRENCH_WORD = re.compile(r"[^\W\d_]+(?:['’-][^\W\d_]+)*", re.UNICODE)


def french_tokens(text, ranks):
    """
    French words as the reader counts them, split at hyphens and apostrophes (l'homme is l +
    homme, est-ce is est + ce), except the words the list has whole (aujourd'hui, jusqu'à,
    quelqu'un, week-end), which a learner knows as one word.
    """
    out = []
    for match in FRENCH_WORD.finditer(text):
        word = match.group(0).replace("’", "'")
        # The ending of an ordinal (20e, 1er, 2nde) belongs to its number, which is not counted.
        if match.start() > 0 and text[match.start() - 1].isdigit() and word.lower() in ORDINAL_ENDINGS:
            continue
        if word.lower() in ranks:
            out.append(word)
            continue
        for part in word.split("-"):
            # Elided words come off the front one at a time, so d'aujourd'hui is d + aujourd'hui.
            while part and part.lower() not in ranks and "'" in part:
                elided, _, part = part.partition("'")
                if elided:
                    out.append(elided)
            if part:
                out.extend([part] if part.lower() in ranks else [piece for piece in part.split("'") if piece])
    return out


ROMAN = re.compile(r"(?=[IVXLC])M{0,3}(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})")

def russian_tokens(text):
    """Russian words, split at hyphens; the ending of an ordinal glued to its digits (5-го, 20-м) is not a word."""
    out = []
    for match in WORD.finditer(text):
        start = match.start()
        if start >= 2 and text[start - 1] == "-" and text[start - 2].isdigit():
            continue
        out.append(match.group(0))
    return out


def russian_name_forms(names):
    """
    The case forms of Russian names, from their regular declension: Анна, Анны, Анне, Анну, Анной;
    Иван, Ивана, Ивану, Иваном, Иване; Игорь, Игоря, Игорю, Игорем, Игоре; Мария, Марии, Марию,
    Марией. A name of several words or with a hyphen (Нижний Новгород, Санкт-Петербург) gives
    the forms of each part. A name whose stem changes (Лев, Пётр, Любовь) is listed by its stems.
    """
    forms = set()
    for name in names:
        for part in re.split(r"[\s-]+", name.lower().replace("ё", "е")):
            if not part:
                continue
            forms.add(part)
            stem, last = part[:-1], part[-1]
            if last == "а":
                forms.update(stem + e for e in ("ы", "и", "е", "у", "ой", "ою"))
                if stem[-1:] in "жшщч":
                    forms.update(stem + e for e in ("ей", "ею"))
            elif last == "я":
                if part.endswith("ия"):
                    forms.update(stem + e for e in ("и", "ю", "ей"))
                else:
                    forms.update(stem + e for e in ("и", "е", "ю", "ей", "ею"))
            elif last in "ьй":
                forms.update(stem + e for e in ("я", "ю", "ем", "е", "и", "ью"))
            elif last not in "оеиыуюэ":
                forms.update(part + e for e in ("а", "у", "ом", "е", "ем"))
    return forms


UKRAINIAN_SOFTENED = {"г": "з", "к": "ц", "х": "с"}


def ukrainian_name_forms(names):
    """
    The case forms of Ukrainian names, the vocative too, from their regular declension: Оксана, Оксани,
    Оксані, Оксану, Оксаною, Оксано (Ольга, Ользі; Саша, Сашею); Марія, Марії, Марією, Маріє; Таня, Тані,
    Танею, Таню; Іван, Івана, Іванові, Іваном, Іване; Ігор, Ігоря, Ігореві, Ігорем, Ігорю; Андрій, Андрія,
    Андрієві, Андрієм, Андрію; Петро, Петра, Петрові, Петре; Карпати, Карпат, Карпатах; Біла Церква, Білої
    Церкви; Кривий, Кривого. A name in -ів or -їв (Львів, Харків, Київ) also gives its stem in -ов-/-єв-
    (Львова, Києва). A name of several words or with a hyphen gives the forms of each part. A name whose
    stem changes otherwise (Ірпінь, Ірпеня; Ріг, Рогу) is listed by its stems.
    """
    forms = set()
    for name in names:
        for part in re.split(r"[\s-]+", fold("uk", name.lower())):
            if not part:
                continue
            forms.add(part)
            stem, last = part[:-1], part[-1]
            if part.endswith("ий"):
                forms.update(part[:-2] + e for e in ("ого", "ому", "им", "ім"))
            elif last == "а":
                forms.update(stem + e for e in ("и", "і", "у", "ою", "о", "ої", "ій"))
                if stem[-1:] and stem[-1] in "жчшщ":
                    forms.add(stem + "ею")
                if stem[-1:] in UKRAINIAN_SOFTENED:
                    forms.add(stem[:-1] + UKRAINIAN_SOFTENED[stem[-1]] + "і")
            elif last == "я":
                if stem[-1:] and stem[-1] in "аеєиіїоуюя'":
                    forms.update(stem + e for e in ("ї", "ю", "єю", "є"))
                else:
                    forms.update(stem + e for e in ("і", "ю", "ею", "е"))
            elif last == "й":
                forms.update(stem + e for e in ("я", "ю", "єві", "єм", "ї"))
            elif last == "ь":
                forms.update(stem + e for e in ("я", "ю", "еві", "ем", "і"))
            elif last == "о":
                forms.update(stem + e for e in ("а", "у", "ові", "ом", "е", "і"))
            elif last == "и":
                forms.update(stem + e for e in ("", "ах", "ам", "ами"))
            elif last not in "еєіїую'":
                forms.update(part + e for e in ("а", "у", "ові", "ом", "і", "е", "я", "ю", "еві", "ем"))
                if part.endswith(("ів", "їв")):
                    short = part[:-2] + ("о" if part[-2] == "і" else "є") + "в"
                    forms.update(short + e for e in ("а", "у", "ові", "ом", "і", "е"))
    forms.discard("")
    return forms


def serbian_name_forms(names):
    """
    The case forms of Serbian names, from their regular declension: Ana, Ane, Ani, Anu, Anom; Marko,
    Marka, Marku, Markom; Jovan, Jovana, Jovanu, Jovanom, Jovane; Petrović, Petrovića, Petroviću;
    Niš, Niša, Nišu; Novi Sad, Novog Sada, Novom Sadu. A name whose vowel drops (Petar, Kragujevac, Čačak) also
    gives the short stem (Petra, Kragujevca, Čačka). A name of several words or with a hyphen gives the
    forms of each part.
    """
    forms = set()
    for name in names:
        for part in re.split(r"[\s-]+", name.lower()):
            if not part:
                continue
            forms.add(part)
            stem, last = part[:-1], part[-1]
            if last == "a":
                # Ana, Ane, Ani, Anu, Anom, and the adjective-like Fruška gora: Fruškoj gori; Ana's: Anin, Anina.
                forms.update(stem + e for e in ("e", "i", "u", "o", "om", "oj", "im", "in", "ina", "ino", "ine", "inu", "inim", "inoj", "ini"))
            elif last == "o":
                forms.update(stem + e for e in ("a", "u", "om", "o"))
            elif last == "e":
                forms.update(stem + e for e in ("a", "u", "em"))
            elif last == "i":
                forms.update(stem + e for e in ("og", "om", "oj", "im", "ih", "a", "e", "u", "i"))
            elif last not in "uy":
                forms.update(part + e for e in ("a", "u", "om", "em", "e", "i", "ov", "ova", "ovo", "ove", "ovu", "ovom", "ovoj", "ovi", "ovim"))
                if len(part) > 4 and part.endswith(("ac", "ak", "ar", "ec", "ek")):
                    short = part[:-2] + part[-1]
                    forms.update(short + e for e in ("a", "u", "om", "em", "e", "i"))
    return forms


# Object pronouns a Spanish verb can carry at its end, longest first so "les" goes before "le".
SPANISH_CLITICS = ("los", "las", "les", "nos", "os", "lo", "la", "le", "me", "te", "se")
UNACCENTED = str.maketrans("áéíóú", "aeiou")


def spanish_verb_forms(words):
    """Every form of the list's verbs, the words whose headword is an infinitive (arrepentirse too)."""
    return {form for key, (_, _, forms) in words.items() if key.endswith(("ar", "er", "ir", "ír", "arse", "erse", "irse", "írse")) for form in [key] + forms}


def spanish_imperative_infinitives(form):
    """
    The infinitives an imperative (or a usted/ustedes form) may come from: pregunta, pregunte and
    pregunten from preguntar, vive from vivir, and with the stem change undone, cuenta from contar,
    piensa from pensar, pide from pedir.
    """
    bare = form[:-1] if form.endswith("n") else form
    stems = []
    if bare.endswith("a"):
        stems += [bare[:-1] + "ar", bare[:-1] + "er", bare[:-1] + "ir"]
    if bare.endswith("e"):
        stems += [bare[:-1] + "er", bare[:-1] + "ir", bare[:-1] + "ar"]
    out = set(stems)
    for infinitive in stems:
        root, ending = infinitive[:-2], infinitive[-2:]
        for changed, plain in (("ue", "o"), ("ie", "e"), ("i", "e")):
            at = root.rfind(changed)
            if at >= 0:
                out.add(root[:at] + plain + root[at + len(changed):] + ending)
    return out


def spanish_clitics(word, ranks, verbs=None):
    """
    A verb carrying pronouns split into the verb and them, as a learner reads it: levantarse is
    levantar + se, dándoselo is dando + se + lo, dímelo is di + me + lo. The written accent such a
    form needs comes off the verb. None when the word does not split that way.
    """
    lower = word.lower()
    for first in SPANISH_CLITICS:
        if not lower.endswith(first) or len(lower) <= len(first) + 1:
            continue
        rest = lower[: -len(first)]
        for second in ("",) + SPANISH_CLITICS:
            if second and (not rest.endswith(second) or len(rest) <= len(second) + 1):
                continue
            stem = rest[: len(rest) - len(second)] if second else rest
            for candidate in (stem, stem.translate(UNACCENTED)):
                # Only forms of verbs carry pronouns (an infinitive, gerund or imperative), so chiles
                # is not chi + les; without the list's verbs, the ending decides.
                is_verb = candidate in verbs if verbs is not None else candidate.endswith(("ar", "er", "ir", "ndo", "a", "e", "i", "n", "z", "d"))
                # The list may file a verb's imperative as a noun (pregunta), yet with pronouns and
                # its written accent (pregúntales) it can only be the verb's.
                if verbs is not None and not is_verb and any(c in "áéíóú" for c in lower):
                    is_verb = any(infinitive in verbs for infinitive in spanish_imperative_infinitives(candidate))
                # Pronouns after any form but an infinitive call for a written accent (tómate,
                # dímelo), save one pronoun after a one-syllable form (ponte, dime); so an unaccented
                # tomate is the noun, not toma + te.
                # The vosotros imperative takes them unaccented, and drops its d before os: habladlo,
                # sentaos.
                if verbs is not None and not is_verb and first == "os" and not second and candidate + "d" in verbs:
                    is_verb = True
                elif is_verb and not candidate.endswith(("ar", "er", "ir", "ír", "d")) and not any(c in "áéíóú" for c in lower):
                    is_verb = not second and len(re.findall("[aeiouáéíóúü]+", candidate)) == 1
                if candidate in ranks and is_verb:
                    return [candidate] + ([second] if second else []) + [first]
    return None


def spanish_tokens(text, ranks, names=frozenset(), verbs=None):
    """Spanish words split at hyphens, and a verb's attached pronouns counted as words of their own."""
    out = []
    for match in WORD.finditer(text):
        for part in re.split(r"-", match.group(0)):
            if not part:
                continue
            # A name is never a verb with pronouns, though Chile ends like one.
            split = None if part.lower() in ranks or part.lower() in names else spanish_clitics(part, ranks, verbs)
            out.extend(split or [part])
    return out


# Pronouns an Italian verb can carry at its end: the combined ones (glielo, melo, ce ne) first, then the single ones.
ITALIAN_PRONOUN_PAIRS = {
    first + second: (first_word, second)
    for first, first_word in (("glie", "gli"), ("me", "me"), ("te", "te"), ("se", "se"), ("ce", "ce"), ("ve", "ve"))
    for second in ("lo", "la", "li", "le", "ne")
}
ITALIAN_PRONOUNS = ("mi", "ti", "ci", "vi", "si", "lo", "la", "li", "le", "gli", "ne")
# The short imperatives (da', di', fa', sta', va') double the first letter of the pronoun: dammi, dimmi, fallo, vattene.
ITALIAN_SHORT_IMPERATIVES = ("da", "di", "fa", "sta", "va")


def italian_verb_forms(words):
    """Every form of the list's verbs, the words whose headword is an infinitive (alzarsi too)."""
    return {form for key, (_, _, forms) in words.items() if key.endswith(("are", "ere", "ire", "rre", "arsi", "ersi", "irsi", "rsi")) for form in [key] + forms}


# Pronoun compounds the list files as words of their own beside the real words they resemble (dimmi, fammi, eccolo).
ITALIAN_LISTED_COMPOUNDS = {"dimmi", "fammi", "fallo", "falla", "eccomi", "eccolo", "ditemi", "fatemi", "vattene", "dammi"}


def italian_clitics(word, ranks, verbs=None, listed=False, heads=None):
    """
    A verb carrying pronouns split into the verb and them, as a learner reads it: mangiarlo is
    mangiare + lo, dimmelo is di + me + lo, guardandolo is guardando + lo, eccomi is ecco + mi. The
    infinitive loses its final e (mangiar-lo, por-lo for porre). None when the word does not split
    that way.
    """
    lower = word.lower()
    if listed and lower in ITALIAN_LISTED_COMPOUNDS:
        listed = False
    elif listed and heads is not None and lower not in heads:
        # A form the list files under a verb (parla, under parlare) is that verb's, not pare + la.
        return None
    elif listed:
        # A word the list holds is only taken for verb and pronouns when it is an apocopated infinitive with them
        # (farmi, dirlo, esserci, alzarsi): parola, bene and portale are words.
        pass
    options = [(suffix, pair) for suffix, pair in sorted(ITALIAN_PRONOUN_PAIRS.items(), key=lambda kv: -len(kv[0]))]
    options += [(pronoun, (pronoun,)) for pronoun in sorted(ITALIAN_PRONOUNS, key=lambda p: -len(p))]
    for suffix, pair in options:
        if not lower.endswith(suffix) or len(lower) <= len(suffix) + 1:
            continue
        stem = lower[: -len(suffix)]
        if listed and not (stem.endswith("r") and (stem + "e" in verbs or stem + "re" in verbs)):
            continue
        candidates = [stem, stem + "e", stem + "re"]
        # The short imperatives double the pronoun's first consonant: da + mmi.
        if stem[-1] == suffix[0] and stem[:-1] in ITALIAN_SHORT_IMPERATIVES:
            candidates.append(stem[:-1])
        for candidate in candidates:
            if candidate in ITALIAN_SHORT_IMPERATIVES and candidate != stem:
                return [candidate] + list(pair)
            if candidate in ranks and (candidate in verbs if verbs is not None else candidate.endswith(("are", "ere", "ire", "rre", "ndo", "a", "i", "e", "o"))):
                return [candidate] + list(pair)
            if candidate == "ecco":
                return [candidate] + list(pair)
    return None


def italian_tokens(text, ranks, names=frozenset(), verbs=None, heads=None):
    """
    Italian words as the reader counts them: elisions come off the front (l'amico is l + amico,
    dell'acqua is dell + acqua, c'è is c + è) except the words the list has whole, and a verb's
    attached pronouns are counted as words of their own. The ª and º of an ordinal (1º) are no words.
    """
    out = []
    for token in french_tokens(re.sub(r"(?<=\d)[ºª°]", "", text), ranks):
        lower = token.lower()
        # An elided pair the list holds as a word of its own at a higher rank than its parts (com'è) counts as the parts.
        if "'" in token and lower in ranks:
            parts = [part for part in token.split("'") if part]
            if len(parts) > 1 and all(part.lower() in ranks for part in parts) and ranks[lower] > max(ranks[part.lower()] for part in parts):
                out.extend(parts)
                continue
        split = None if lower in names else italian_clitics(token, ranks, verbs, listed=lower in ranks, heads=heads)
        out.extend(split or [token])
    return out


def turkish_lower(text):
    """Lowercased as Turkish does: I is ı, İ is i."""
    return text.replace("I", "ı").replace("İ", "i").lower()


def turkish_tokens(text):
    """
    Turkish words, lowercased the Turkish way. A suffix set off by an apostrophe (Ali'nin, Ankara'ya,
    TDK'ya) goes with the word before it, which is counted alone; after digits (3'te, 1990'larda) it
    belongs to the number, which is not counted.
    """
    text = re.sub(r"(?<=\d)['’][^\W\d_]+", " ", text)
    return [turkish_lower(re.split(r"['’]", match.group(0))[0]) for match in WORD.finditer(text)]


# Regular Turkish endings the list's forms run out on: the copula and personal endings on any word
# (evdeydi, masaydı, yıldır, telefondaydım), and a verb's -me/-dik/-ecek nouns and converbs with their
# possessive and case (görmesini, yaşadığını, olacağını, çalıştıktan, görmeden).
TR_COPULA = re.compile(r"(?:y?ken|y?[dt][ıiuü](?:m|n|k|nız|niz|nuz|nüz|lar|ler)?|y?m[ıiuü]ş(?:[ıiuü]m|s[ıiuü]n|[ıiuü]z|l[ae]r)?|[dt][ıiuü]r|y?s[ae]|y?[ıiuü]m|y?[ıiuü]z|s[ıiuü]n(?:[ıiuü]z)?)$")
TR_VERBAL = re.compile(
    # -me nouns with plural, possessive and case (görmesini, yapmalarını, harcamamız)
    r"(?:m[ae](?:l[ae]r)?(?:s?[ıiuü]|[ıiuü]m|[ıiuü]n|[ıiuü]m[ıiuü]z|[ıiuü]n[ıiuü]z|l[ae]r[ıiuü])?(?:n?[ıiuü]|n?[ae]|n?d[ae]|n?d[ae]n|yl[ae]|y[ıiuü]|y[ae])?"
    # -dik and -ecek participles with plural, possessive and case (yaşadığını, istediklerini, gittiğimde, edeceklerine)
    r"|(?:[dt][ıiuü][kğ]|y?[ae]c[ae][kğ])(?:l[ae]r)?(?:[ıiuü](?:m|n|m[ıiuü]z|n[ıiuü]z)?|l[ae]r[ıiuü])?(?:n?[ıiuü]|n?[ae]|n?d[ae]|n?d[ae]n)?"
    # -en participles (çalışanın, çalışanlarını) and converbs (çalıştıktan, görmeden, -erek, -ince, -ip)
    r"|y?[ae]n(?:l[ae]r)?(?:[ıiuü]|l[ae]r[ıiuü])?(?:n?[ıiuü]|n?[ae]|n?d[ae]|n?d[ae]n|[ıiuü]n)?"
    r"|[dt][ıiuü]k[dt][ae]n|m[ae]d[ae]n|y?[ae]r[ae]k|y?[ıiuü]nc[ae]|y?[ıiuü]p"
    # conditional -se and aorist -erse (yıkarsak, temizlerlerse)
    r"|(?:[ae]r|[ıiuü]r)?(?:l[ae]r)?s[ae](?:k|m|n|n[ıiuü]z)?)$")
# -ebil- (yapabileceğini, yatabiliyordu) and the passive -il-/-n- (yapılıyor) before any of those, or a tense.
TR_ABLE = re.compile(r"y?[ae]bil")
TR_HARD = {"d": "t", "ğ": "k", "b": "p", "c": "ç"}
# A noun's plural, possessive and case (kartınızla, komşularınızla, telefonundaki, dükkânlardaki).
TR_NOMINAL = re.compile(r"(?:l[ae]r)?(?:[ıiuü]?m|[ıiuü]?n|s?[ıiuü]|[ıiuü]?m[ıiuü]z|[ıiuü]?n[ıiuü]z|l[ae]r[ıiuü])?"
                        r"(?:n?[ıiuü]|n?[ae]|y[ıiuü]|y[ae]|n?[dt][ae]|n?[dt][ae]n|n?[dt][ae]ki|yl[ae]|l[ae]|n[ıiuü]n|[ıiuü]n)?$")
TR_PASSIVE = re.compile(r"(?:[ıiuü]l|n)$")
TR_TENSE = re.compile(r"(?:m?[ıiuü]?yor|[ae]r|[ıiuü]r|[dt][ıiuü]|m[ıiuü]ş|y?[ae]c[ae][kğ]|m[ae]l[ıi]|m[ae]z|m[ae])\w*$")


def turkish_base(word, ranks):
    """The list word a regularly suffixed Turkish word is a form of, or None."""
    match = TR_COPULA.search(word)
    if match and match.start() >= 3:
        base = word[:match.start()]
        if base in ranks:
            return base
        word = base
    # The longest list noun the rest of the word is a plural, possessive and case ending of (omuzdaki is
    # omuz + daki, not o + muzdaki); a two-letter noun (el, ev, su) only with an ending of three letters or more.
    for end in range(len(word) - 1, 1, -1):
        if end == 2 and len(word) < 5:
            break
        if not TR_NOMINAL.fullmatch(word, end):
            continue
        base = word[:end]
        for noun in (base, base[:-1] + TR_HARD[base[-1]] if base[-1:] in TR_HARD else None):
            if noun in ranks:
                return noun
    # A verb stem may be two letters (ol-, öl-): the verb itself must be a list word.
    stems = []
    match = TR_VERBAL.search(word)
    if match and match.start() >= 2:
        stems.append(word[:match.start()])
    match = TR_ABLE.search(word, 2)
    if match:
        stems.append(word[:match.start()])
    match = TR_TENSE.search(word, 2)
    if match:
        stems.append(word[:match.start()])
    for stem in list(stems):
        passive = TR_PASSIVE.search(stem)
        if passive and passive.start() >= 2:
            stems.append(stem[:passive.start()])
    # A stem softened before a vowel (gid- of gitmek, ed- of etmek).
    stems += [stem[:-1] + TR_HARD[stem[-1]] for stem in stems if stem[-1:] in TR_HARD]
    for stem in stems:
        for verb in (stem + "mak", stem + "mek"):
            if verb in ranks:
                return verb
    return None


# Korean: runs of Hangul are the words, each cut as the reader cuts it (KoreanParser, ported as
# build_frequency_list.korean_pieces) by the ranks of the list the app ships, not the courses' pinned one.
KOREAN_RUN = re.compile("[가-힣ᄀ-ᇿ㄰-㆏]+")
# Number words (build_frequency_list.korean_number) before a counter (세 명, 두 시, 열두 개, 오 분): the number
# and the counter count as known.
_korean = {}


def korean_reader():
    """The bundled list's words and ranks, as LexiconService gives them to the reader, and the ko-en pack's words."""
    if not _korean:
        import sqlite3
        import build_frequency_list as fl
        words, bundled = fl.list_ranks(open(os.path.join(LISTS, "ko.tsv"), encoding="utf-8").read())
        path = os.path.join(ROOT, "dictionaries", "ko-en.sqlite")
        if not os.path.exists(path):
            sys.exit(f"Korean courses need the ko-en pack at {path}")
        db = sqlite3.connect(path)
        # An entry that is a word with its particle or plural 들 (아이들, "plural of 아이") is cut, as the list counts it.
        fused = {w for w, senses in db.execute("SELECT word_lc, senses FROM entries") if fl.korean_fused(w, senses)}
        pack = {w for (w,) in db.execute("SELECT word_lc FROM entries UNION SELECT form_lc FROM forms") if w} - fused
        # The copula's forms are listed under 이다; its stem 이 would otherwise begin any word in ㅇ (있었구나).
        verbs = {w for (w,) in db.execute("SELECT word_lc FROM entries WHERE pos IN ('verb', 'adj')")} - {"이다"}
        lemmas = {}
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            lemmas.setdefault(form, set()).add(lemma)
        _korean.update(fl=fl, known=lambda w: w in words or w in pack, rank=bundled.get, verbs=verbs, lemmas=lemmas)
    return _korean


def korean_guess(form, ranks, heads, depth=0):
    """
    The list's verb or adjective a whole Korean form the list lacks inflects, as build_frequency_list's korean_guess
    finds it: by Yomitan's rules, the nearest, then the commonest; then the spoken endings in 잖아, then without a
    polite 요 (같네요, 할게요).
    """
    k = korean_reader()
    fl = k["fl"]
    # The pack's own verb for the form (나으세요 of 낫다, not 나다), as the list builder takes it.
    listed = [lemma for lemma in k["lemmas"].get(form, ()) if lemma in heads and lemma in k["verbs"]]
    if listed:
        return min(listed, key=ranks.get)
    # The polite 요 after a word of the list (이거요), before the rules take it for a verb's (익다).
    if form.endswith(fl.KOREAN_POLITE) and form[:-1] in ranks:
        return form[:-1]
    found = [(steps, ranks[lemma], lemma) for lemma, steps in fl.korean_dictionary_forms(form)
             if lemma in heads and lemma in k["verbs"] and fl.korean_stem_of(lemma, form)]
    if found:
        return min(found)[2]
    stem = next((form[:-len(e)] for e in fl.KOREAN_SPOKEN_ENDINGS if form.endswith(e) and len(form) > len(e)), None)
    if stem and stem + "다" in heads and stem + "다" in k["verbs"]:
        return stem + "다"
    if form.endswith(fl.KOREAN_POLITE) and len(form) > 2 and depth < 2:
        base = form[:-1]
        listed = [lemma for lemma in k["lemmas"].get(base, ()) if lemma in heads and lemma in k["verbs"]]
        return min(listed, key=ranks.get) if listed else korean_guess(base, ranks, heads, depth + 1)
    return None


def korean_name(run, names):
    """A run that is a name with particles after it (민수가, 서울에서, 민준이는) or called by it (민수야, 민준아): None, or the particles."""
    k = korean_reader()
    for name in names:
        if not run.startswith(name):
            continue
        rest = run[len(name):]
        if not rest:
            return []
        final = (ord(name[-1]) - 0xAC00) % 28 if "가" <= name[-1] <= "힣" else -1
        # The vocative, and the 이 a given name ending in a consonant takes before particles (민준이가).
        if rest == ("아" if final > 0 else "야"):
            return []
        stacks = k["fl"].korean_stacks(rest, name)
        if not stacks and final > 0 and rest.startswith("이"):
            if rest == "이":
                return []
            stacks = k["fl"].korean_stacks(rest[1:], name + "이")
        if stacks:
            return min(stacks, key=lambda stack: sum(math.log(k["rank"](p) or k["fl"].KOREAN_UNRANKED) for p in stack))
    return None


def korean_tokens(text, ranks, names, heads):
    """
    Korean words as the reader cuts them: each run of Hangul into a word and the particles after it, by the
    bundled list's ranks and the ko-en pack's words. Each piece is the list's word as written, or, for a whole
    verb form the list lacks, the verb it inflects. A name, and a number's counter, is [KNOWN]; the
    particles after it are words.
    """
    k = korean_reader()
    names = sorted({part for name in names for part in name.split() if part}, key=len, reverse=True)
    fl = k["fl"]
    out = []
    previous_number, previous_end, previous_run = False, 0, None
    for match in KOREAN_RUN.finditer(text):
        run = match.group(0)
        start = match.start()
        # The word before, as the reader gives it: the run before after spaces, or the digits just before.
        gap = text[previous_end:start]
        after_digits = start > 0 and text[start - 1].isdigit()
        before = re.search(r"\d+$", gap).group(0) if after_digits else previous_run if gap and gap.isspace() else None
        previous_run = run
        particles = korean_name(run, names)
        if particles is not None:
            out.append(KNOWN)
            out.extend(particles)
            previous_number, previous_end = False, match.end()
            continue
        # After a number, a counter and its particles (한 대가 is 대 + 가, not 대가 "price").
        pieces = fl.korean_counter_cut(run, k["known"], k["rank"], before) or fl.korean_pieces(run, k["known"], k["rank"])
        # A counter after digits (3개, 2024년에) or after a number word (세 명, 오 분) belongs to the number.
        spaced = previous_number and bool(gap) and gap.isspace()
        if after_digits or spaced and pieces[0] in fl.KOREAN_COUNTERS:
            if spaced:
                out[-1] = KNOWN
            out.append(KNOWN)
            pieces = pieces[1:]
        previous_end = match.end()
        previous_number = len(pieces) == 1 and pieces[0] == run and fl.korean_number(run)
        for piece in pieces:
            out.append(piece if piece in ranks else korean_guess(piece, ranks, heads) or piece)
    return out


def check(path, brief=False):
    course = json.load(open(path, encoding="utf-8"))
    code = course["id"].split("-")[0]
    ranks, words = load_list(code)
    verbs = spanish_verb_forms(words) if code == "es" else italian_verb_forms(words) if code == "it" else None
    up_to = course["rankUpTo"]
    band_start = up_to - BAND + 1
    need, shortest, longest = rules(up_to)
    # Chinese cuts into more, shorter words than the languages the lengths were set for.
    if code in ("zh", "ja", "ko") and up_to > 4000:
        longest = 700
    names = {fold(code, n.lower()) for n in course.get("names", [])}
    # In Spanish a name of several words or joined by a hyphen (San José, al-Ándalus) counts as one known word wherever it
    # is written whole; the earlier languages count such names word by word, as they were written to.
    long_names = sorted((n for n in course.get("names", []) if " " in n or "-" in n or (code == "it" and "'" in n)), key=len, reverse=True) if code in ("es", "it", "sr", "nl", "da", "no", "sv", "el") else []
    # Chinese writes no spaces, so every name is matched whole, the longest first.
    if code in ("zh", "ja"):
        long_names = sorted(course.get("names", []), key=len, reverse=True)
    if code == "ru":
        names = names | russian_name_forms(course.get("names", []))
    if code == "uk":
        names = names | ukrainian_name_forms(course.get("names", []))
    if code == "sr":
        names = names | serbian_name_forms(course.get("names", []))
    # A Polish name of several words or with a hyphen (Zielona Góra, Bielsko-Biała) counts as one known word wherever it
    # is written whole; each case form the text uses is listed whole (Zielonej Górze).
    if code == "pl":
        long_names = sorted((n for n in course.get("names", []) if " " in n or "-" in n), key=len, reverse=True)
    # Turkish names are lowercased the Turkish way (Işıl, İzmir); one of several words (Kız Kulesi) counts as one
    # known word wherever it is written whole, its suffix too (Kız Kulesi'ne).
    if code == "tr":
        names = {fold(code, turkish_lower(n)) for n in course.get("names", [])}
        long_names = sorted((n for n in course.get("names", []) if " " in n or "-" in n), key=len, reverse=True)
    seen_new = set()
    ok = True
    report = []
    # Where each new word is taught, by its forms: a Chinese lesson must not use a word a later lesson teaches.
    taught_in = {}
    if code in ("zh", "ja", "ko"):
        for j, later in enumerate(course["lessons"], start=1):
            for new in later.get("newWords", []):
                key = fold(code, new.lower())
                entry = words.get(key) or next((e for w, e in words.items() if key in e[2]), None)
                for form in [key] + (entry[2] if entry else []):
                    taught_in.setdefault(form, (j, new))
    for i, lesson in enumerate(course["lessons"], start=1):
        text = lesson["text"]
        whole_names = 0
        for name in long_names:
            if code in ("da", "nl", "no", "sv"):
                # The genitive of a name of several words goes with it (Maja Holms, Jan de Vries').
                pattern = re.compile(re.escape(name) + r"(?:['’]s|s|['’])?(?![^\W\d_])")
                whole_names += len(pattern.findall(text))
                text = pattern.sub(" ", text)
                continue
            if code == "tr":
                pattern = re.compile(r"(?<![^\W\d_])" + re.escape(name) + r"(?:['’][^\W\d_]+)?(?![^\W\d_])")
                whole_names += len(pattern.findall(text))
                text = pattern.sub(" ", text)
                continue
            # A katakana name is not matched inside a longer katakana word (リン in オリンピック).
            if code == "ja" and re.fullmatch(r"[ァ-ヺー]+", name):
                pattern = re.compile(r"(?<![ァ-ヺー])" + re.escape(name) + r"(?![ァ-ヺー])")
                whole_names += len(pattern.findall(text))
                text = pattern.sub(JA_NAME, text)
                continue
            whole_names += text.count(name)
            text = text.replace(name, JA_NAME if code == "ja" else " ")
        # A decade or number ending joined to the digits is a number (Danish 1960'erne, Norwegian 1960-tallet, Swedish 1960-talet).
        if code in ("da", "no"):
            text = re.sub(r"\d+['’][^\W\d_]+", " ", text)
        if code == "no":
            text = re.sub(r"\d+-tall(?:et|ets)?\b", " ", text)
        if code == "sv":
            text = re.sub(r"\d+-tal(?:et|ets|s)?\b", " ", text)
            # Swedish joins an ending to an abbreviation with a colon (tv:n, cd:n): it counts as the abbreviation.
            text = re.sub(r"(?<=[^\W\d_]):[^\W\d_]+", "", text)
        # Greek elides a short word before a vowel (σ' αγαπώ, τ' όνομα, απ' το): it counts as the word it stands for.
        if code == "el":
            text = GREEK_ELISION.sub(lambda m: GREEK_ELIDED[m.group(1).lower()] + " ", text)
            # ό,τι ("whatever") is one word written with a comma.
            text = re.sub(r"([Όό]),τι", r"\1τι", text)
            # An ordinal written with digits (19ος, 19ου, 3η) is a number.
            text = re.sub(r"(?<=\d)(?:ος|ός|ης|ής|ου|ού|ων|ών|ους|ούς|οι|οί|ες|ές|ο|ό|η|ή|α|ά)(?![^\W\d_])", "", text)
        # The header of the questions is the lesson's layout, not one of its words.
        if code == "ja":
            text = text.replace("質問：", "")
        if code == "uk":
            text = re.sub(r"(?m)^Запитання:", "", text)
        if code == "ko":
            text = re.sub(r"(?m)^질문:", "", text)
        toks = tokens(text, code, ranks, names, verbs, set(words) if code in ("it", "ko") else None)
        # A century or a king's number in capital Roman numerals (siglo XVI, Carlos V) is a number.
        if code in ("es", "ru", "it", "sr", "uk"):
            numerals = sum(1 for t in toks if ROMAN.fullmatch(t))
            toks = [t for t in toks if not ROMAN.fullmatch(t)]
            whole_names += numerals
        # So is a Polish one (XIX wiek, Jan III Sobieski); a capital I alone is the word i.
        if code == "pl":
            numerals = sum(1 for t in toks if t != "I" and ROMAN.fullmatch(t))
            toks = [t for t in toks if t == "I" or not ROMAN.fullmatch(t)]
            whole_names += numerals
        raw_words = [fold(code, t.lower()) for t in re.findall(r"[^\W\d_]+(?:['’][^\W\d_]+)*", text)]
        lowered = [fold(code, t.lower()) for t in toks] + ["\u0000name"] * whole_names
        if code == "tr":
            lowered = [w if w in ranks or w in names else turkish_base(w, ranks) or w for w in lowered]
        # Ukrainian writes у/в and і/й by the sound before (учинив, вчинив): a word the list has only one way counts.
        if code == "uk":
            swap = {"у": "в", "в": "у", "і": "й", "й": "і"}
            lowered = [w if w in ranks or w in names or w[:1] not in swap or swap[w[0]] + w[1:] not in ranks else swap[w[0]] + w[1:] for w in lowered]
        names_here = names | {"\u0000name"}
        outside = {}
        known = 0
        after_number = False
        for t in lowered:
            # An English possessive ("emma's") counts as its word: the list has few of them.
            if t.endswith(("'s", "’s")) and t not in ranks and len(t) > 2:
                t = t[:-2]
            if t in names_here:
                known += 1
                continue
            # A Chinese number written in characters (三, 二十五, 两) is a number, as digits are, and so is a
            # measure word right after one (两 + 遍).
            if code == "zh":
                measure = len(t) == 1 and t in CHINESE_MEASURES and after_number
                after_number = bool(re.fullmatch(r"第?[零〇一二三四五六七八九十百千万亿两几]+", t))
                if measure or CHINESE_NUMBER.fullmatch(t):
                    known += 1
                    continue
            rank = ranks.get(t)
            if rank is not None and rank <= up_to:
                known += 1
            else:
                outside[t] = rank
        coverage = known / max(1, len(lowered))
        problems = []
        if coverage < need:
            problems.append(f"coverage {coverage:.1%} < {need:.0%}")
        if not shortest <= len(lowered) <= longest:
            problems.append(f"{len(lowered)} words, needs {shortest}-{longest}")
        for new in lesson.get("newWords", []):
            key = fold(code, new.lower())
            entry = words.get(key)
            # A Chinese word may be written in a simplified spelling the list keeps as a form (着 of 著).
            if entry is None and code == "zh":
                heads = [w for w, (r, _, forms) in words.items() if key in forms and band_start <= r <= up_to]
                if len(heads) == 1:
                    key, entry = heads[0], words[heads[0]]
            if entry is None:
                problems.append(f"new word '{new}' is not in the list")
                continue
            rank, _, forms = entry
            if not band_start <= rank <= up_to:
                problems.append(f"new word '{new}' is rank {rank}, outside {band_start}-{up_to}")
            if key in seen_new:
                problems.append(f"new word '{new}' was introduced in an earlier lesson")
            uses = sum(1 for t in lowered if t == key or t in forms)
            # A word the checker splits (Italian farci is fare + ci) still counts when the list teaches it whole.
            if code == "it":
                uses = max(uses, sum(1 for t in raw_words if t == key))
            if uses < 2:
                problems.append(f"new word '{new}' used {uses}x, needs 2+")
            seen_new.add(key)
        early = sorted({taught_in[t] for t in lowered if t in taught_in and taught_in[t][0] > i})
        for j, new in early:
            problems.append(f"uses '{new}' before lesson {j} teaches it")
        status = "PASS" if not problems else "FAIL"
        ok = ok and not problems
        line = f"lesson {i:2} {status}  {len(lowered)} words, coverage {coverage:.1%}, {len(lesson.get('newWords', []))} new"
        report.append(line)
        for p in problems:
            report.append("    - " + p)
        if outside and (problems or not brief):
            listed = sorted(outside.items(), key=lambda kv: (kv[1] is None, kv[1] or 0))
            report.append("    outside: " + ", ".join(f"{w}({r if r else 'not in list'})" for w, r in listed))
    band = [w for w, (r, _, _) in words.items() if band_start <= r <= up_to]
    # Chinese band words are shown as the courses write them, in simplified characters.
    missing = [w if code == "zh" else words[w][1] for w in band if w not in seen_new]
    report.append(f"band {band_start}-{up_to}: {len(seen_new)} new words taught; not taught ({len(missing)}): {', '.join(missing)}")
    print("\n".join(report))
    return ok


if __name__ == "__main__":
    sys.exit(0 if check(sys.argv[1], brief="--brief" in sys.argv) else 1)
