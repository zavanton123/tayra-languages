"""
Malay words and forms for the frequency list.

The ms-en pack (11,000 entries) lists some affixed verbs as words of their own (mengajar, menjelaskan, selamatkan,
bertanya) and has no entry or form for many others (menggunakan, dibeli, diambil, memeriksa, mengucapkan), so the list
filed a verb's voices under different words or dropped them. Nor has it many everyday words (penting, petani, sila,
terakhir, tersenyum). So (relink):

- a word of the Indonesian packs is a Malay word where the ms pack has no word or form spelled so and Malaysian web
  text uses it (OTHER_PACKS, MALAYSIAN_SHARE): Indonesian-only words (uang, mobil, karena, pikir) are written in
  subtitles, not on .my pages;
- a root's verb forms go to the root: meN-, di-, ber-, memper-/diper- with or without -kan/-i, and -kan/-i alone
  (ajar: mengajar, diajar, mengajarkan, mengajari; guna: menggunakan, digunakan, gunakan; tanya: bertanya, ditanya,
  tanyakan), and so do they with a clitic (menggunakannya). A pack word so spelled goes there too when it is a verb
  alone, unless it is a root itself, whose own forms text writes (menang: memenangi; didik: mendidik);
- a word with -nya the packs have as an adverb (biasanya, akhirnya) is its word's, as the reader reads it;
- se- with a time or measure word (sehari, seminggu, setahun, seorang) is that word's.

Derived nouns and adjectives (pelajar, pendidikan, makanan, terakhir, tertentu) and the other ter- words stay words of
their own.
"""
import os
import re
import sqlite3

LETTERS = re.compile(r"[a-z]+")
# The Indonesian packs, whose words Malay mostly shares.
OTHER_PACKS = ["id-en", "id-fr", "id-pl", "id-pt", "id-ru", "id-zh"]
# An Indonesian pack's word counts as Malay when Malaysian web text (Leipzig's .my crawl) has at least this share of
# its count in the blend: penting 1.0, sila 0.9, tersenyum 0.6, but uang 0.01, karena 0.05, banget 0.03.
MALAYSIAN_SHARE = 0.25
# What the reader takes off a word (check_course's indonesian_bare): the particles, then the possessives.
PARTICLES = ("lah", "kah", "pun", "tah")
POSSESSIVES = ("nya", "ku", "mu")
# The words se- ("one") is put before: sehari, seminggu, seorang, sebuah.
MEASURES = {
    "hari", "minggu", "bulan", "tahun", "jam", "minit", "saat", "malam", "pagi", "petang", "abad", "dekad", "orang",
    "buah", "ekor", "biji", "helai", "keping", "batang", "pasang", "cawan", "gelas", "botol", "pinggan", "mangkuk",
    "kilo", "kilometer", "meter", "liter", "jenis", "kumpulan",
}
# An Indonesian pack's word needs at least this share of the counts (one in a million): rarer ones are mostly Indonesian
# spellings (keuangan for kewangan, sorga for syurga).
LEAST = 1e-6
# The kinds a pack word so spelled may have to be a root's verb form (bertanya, mengajar, berada "to be at" and adj).
VERBAL = {"verb", "adj", "noun"}
# How much a verb root counts above another root that makes the same form (root_of).
VERB_WEIGHT = 10
# A word is a root itself, not another's form, when its own forms make this share of its count (memenangi of menang).
ROOT_SHARE = 0.02
# Indonesian packs' words that are English, abbreviations, names or misspellings in Malaysian text.
NOT_WORDS = {
    "dr", "en", "km", "sri", "go", "de", "am", "web", "set", "super", "kim", "tok", "wan", "ali", "dato", "pak", "mara",
    "not", "are", "top", "real", "order", "per", "pro", "sun", "port", "los", "non", "lin", "hong", "wong", "malik", "amir",
    "hafiz", "zahid", "gombak", "ampang", "besut", "metro", "fam", "nas", "ram", "android", "email", "area", "didalam",
    "kedalam", "menit", "mikir", "bel", "kop", "meng", "aja",
}
# The kinds of word that make verbs.
CONTENT = {"noun", "verb", "adj", "adv", "num"}
# Words spelled as a root's form that are words of their own (berani "brave", not rani's; gulai "curry", not gula's).
OWN = {"berani", "capai", "gulai", "daki", "kasi", "maki", "berak", "tarikan", "bercukur", "mengeong", "mengelek", "mengaku", "berhala"}
# Forms whose root is not the commonest that makes them.
ROOTS = {
    **{f: ["tahu"] for f in ("ketahui", "mengetahui", "diketahui", "ketahuilah", "mengetahuinya", "diketahuinya")},
    "menumpukan": ["tumpu"], "ditumpukan": ["tumpu"],
    # mengaku "to admit" is no form of aku "I".
    **{f: ["mengaku"] for f in ("mengakui", "diakui", "akui", "mengakuinya", "diakuinya", "akuilah")},
    # Forms VERB_WEIGHT gives to the wrong verb (beramal of amal, not ramal; mengeratkan of erat, not kerat).
    **{f: ["amal"] for f in ("beramal", "beramallah")},
    **{f: ["perangkap"] for f in ("memerangkap", "memerangkapkan", "diperangkap")},
    **{f: ["erat"] for f in ("mengerat", "mengeratkan", "dieratkan", "eratkan")},
    **{f: ["suai"] for f in ("menyuai", "disuai", "menyuaikan", "disuaikan")},
    **{f: ["tapai"] for f in ("menapai", "ditapai")},
    "menyepi": ["sepi"],
}


def vowels(word):
    """How many syllables [word] has: its vowels, ai, au and oi as one (luar has two, cat and bom one)."""
    return len(re.findall(r"ai|au|oi|[aeiou]", word))


def men(root):
    """The spellings of meN- with [root]: mengajar, menggunakan, mengira, membeli, memilih, mencari, menulis, menyimpan."""
    c, out = root[0], []
    if c in "aeiou" or c in "gh":
        out.append("meng" + root)
    elif c == "k":
        # And kept before some (mengkaji).
        out += ["meng" + root[1:], "mengk" + root[1:]]
    elif c in "bfv":
        out.append("mem" + root)
    elif c == "p":
        # p drops (memilih), and stays in loans and punya (memproses, mempunyai).
        out += ["mem" + root[1:], "mem" + root]
    elif root.startswith("sy") or c in "cdjz":
        out.append("men" + root)
    elif c == "s":
        out.append("meny" + root[1:])
    elif c == "t":
        # t drops (menulis), and stays in some (mentadbir).
        out += ["men" + root[1:], "men" + root]
    elif c in "lmnrwy":
        out.append("me" + root)
    if vowels(root) == 1:
        # A root of one syllable takes menge- (mengecat, mengebom).
        out.append("menge" + root)
    return out


def ber(root):
    """ber- with [root]: bertanya, bermain; be- before r and a first syllable in -er (berehat, bekerja)."""
    if root[0] == "r":
        return ["be" + root, "ber" + root]
    if re.match(r"[^aeiou]er", root):
        return ["be" + root, "ber" + root]
    return ["ber" + root]


def paradigm(root):
    """A root's verb forms but itself: meN-, di-, ber-, memper-/diper- with or without -kan/-i, and -kan/-i alone."""
    out = set()
    for suffix in ("", "kan", "i"):
        if suffix == "i" and root.endswith("i"):
            continue
        if suffix:
            out.add(root + suffix)
            out.add("per" + root + suffix)
        out.update(m + suffix for m in men(root))
        out.update(("di" + root + suffix, "memper" + root + suffix, "diper" + root + suffix))
        if suffix != "i":
            out.update(b + suffix for b in ber(root))
    # And ber- verbs' meN- and di- with -kan (memberhentikan, diberhentikan of berhenti).
    out.update(p + b + "kan" for b in ber(root) for p in ("mem", "di"))
    out.discard(root)
    return out


def bare(form):
    """[form] without the clitics the reader takes off (menggunakannya, ambillah, diambilnya), longest first."""
    out = []
    for particle in ("",) + PARTICLES:
        if particle and not (form.endswith(particle) and len(form) - len(particle) >= 3):
            continue
        stem = form[:len(form) - len(particle)]
        if particle:
            out.append(stem)
        for possessive in POSSESSIVES:
            if stem.endswith(possessive) and len(stem) - len(possessive) >= 3:
                out.append(stem[:-len(possessive)])
    return out


def relink(freq, pos, lemmas, is_word, overrides, excluded, dictionaries, common, found, word_pos):
    """
    Adds the Indonesian packs' Malay words to [pos] and [common], files the roots' verb forms the pack lacks in [lemmas],
    and gives the overrides that send the pack's affixed verbs and -nya adverbs to their root.
    """
    web = next((counts for name, counts, _ in found if name == "leipzig"), {})
    total = sum(web.values()) or 1
    malaysian = {}
    for w, c in web.items():
        for piece in w.lower().split("-"):
            if piece:
                malaysian[piece] = malaysian.get(piece, 0) + c / total
    own = {w for w in pos if is_word(w)}

    def free(form):
        return form not in pos and not lemmas.get(form) and form not in overrides and form not in excluded

    # The Indonesian packs' words.
    candidates = {}
    for name in OTHER_PACKS:
        path = os.path.join(dictionaries, f"{name}.sqlite")
        if not os.path.exists(path):
            continue
        db = sqlite3.connect(path)
        for word, word_lc, p in db.execute("SELECT word, word_lc, pos FROM entries"):
            if p in word_pos and word == word_lc and LETTERS.fullmatch(word_lc) and len(word_lc) > 2:
                candidates.setdefault(word_lc, set()).add(p)
        db.close()
    candidates = {w: kinds for w, kinds in candidates.items() if free(w) and w not in NOT_WORDS and freq.get(w)
                  # A clitic form of a word is that word's (katanya, akhirnya).
                  and not any(b in own for b in bare(w))}

    pool = own | set(candidates)
    made = {}
    for root in sorted(pool):
        # A pronoun, conjunction or the like makes no verbs (mengaku is no form of aku, mengedan none of dan).
        if not LETTERS.fullmatch(root) or len(root) < 3 or not (pos.get(root) or candidates.get(root, set())) & CONTENT:
            continue
        for form in paradigm(root):
            made.setdefault(form, set()).add(root)

    def family(word):
        """[word] and the forms the counts have that would be its: rampas with merampas, letup with meletup."""
        out = [word]
        for form in paradigm(word):
            # Its forms the pack has as verbs count too (membantu and dibantu, bantu's).
            if form in freq and (free(form) or form in candidates or lemmas.get(form) or "verb" in pos.get(form, set())):
                of = (made.get(form, set()) & pool) - {form}
                if of and max(sorted(of), key=lambda r: freq.get(r, 0)) == word:
                    out.append(form)
        return out

    # A word of theirs is Malay when it and its forms are common enough, and common in Malaysian text.
    added = {}
    for w, kinds in candidates.items():
        forms = family(w)
        count = sum(freq.get(f, 0) for f in forms)
        if count >= LEAST and sum(malaysian.get(f, 0) for f in forms) >= MALAYSIAN_SHARE * count:
            added[w] = kinds
    roots = own | set(added)

    # And a word's other spelling makes its forms (memahami of paham, faham's).
    for spelling, of in sorted(lemmas.items()):
        if len(of) == 1 and of[0] in own and pos[of[0]] & CONTENT and spelling not in own and LETTERS.fullmatch(spelling) \
                and len(spelling) > 2:
            for form in paradigm(spelling):
                made.setdefault(form, set()).add(of[0])

    def is_root(word):
        """
        A word whose own verb forms text writes (memenangi, mendidik, dimengerti) is a root, not another's form; forms its
        roots make too (dijelaskan of jelas, not only of jelaskan) do not count.
        """
        of = made.get(word, set())
        return sum(freq.get(f, 0) for f in paradigm(word) if not made.get(f, set()) & of) >= ROOT_SHARE * freq.get(word, 0)

    def measured(word):
        return word.startswith("se") and word[2:] in MEASURES and word[2:] in roots

    for word, kinds in added.items():
        if measured(word) or word in ROOTS or word not in OWN and word in made and "verb" in kinds and not is_root(word):
            continue
        pos[word].update(kinds)
        common[word] = word
    roots = {w for w in roots if is_word(w)}
    # A word of the pack is no form of an added word (pen, not penting's).
    for word in own:
        if lemmas.get(word) and set(lemmas[word]) & set(added):
            lemmas[word] = [l for l in lemmas[word] if l not in added]

    def root_of(form):
        """
        The commonest root that makes [form], a verb before others VERB_WEIGHT times commoner (mengawal of kawal, not awal;
        berikan of beri, not ikan; but memalukan of malu, not palu).
        """
        of = (made.get(form, set()) & roots) - {form}
        weight = lambda r: freq.get(r, 0) * (VERB_WEIGHT if "verb" in pos.get(r, set()) else 1)  # noqa: E731
        return [max(sorted(of), key=weight)] if of else []

    def verbal(form, root):
        """
        Whether a pack word spelled as [root]'s form is a verb, as the pack has it or, where it calls it a noun, adjective or
        adverb, as its root is (berenang, berjaya, dilarang; but beras "rice" is no form of ras, nor bahaya's berbahaya).
        """
        kinds = pos[form] & word_pos
        return "verb" in kinds and kinds <= VERBAL or kinds <= VERBAL | {"adv"} and "verb" in pos.get(root, set()) and (
            form.startswith(("me", "di", "be")) and not form.startswith("per"))

    result = {}
    for form in made:
        if form not in freq or form in OWN:
            continue
        of = ROOTS.get(form) or root_of(form)
        if not of:
            continue
        if free(form):
            lemmas[form] = of
        elif form in own and form not in overrides and (form in ROOTS or verbal(form, of[0]) and not is_root(form)):
            result[form] = of
    for form, of in ROOTS.items():
        if form in freq and form not in made:
            if free(form):
                lemmas[form] = of
            elif form in own:
                result[form] = of
    for word in sorted("se" + m for m in MEASURES):
        if measured(word) and word in freq and free(word):
            lemmas[word] = [word[2:]]
    for word in OWN:
        if word in freq:
            result[word] = [word]

    def head(word):
        """The word [word] is counted for: itself, or the root it was sent to."""
        if word in overrides:
            return [w for w in overrides[word] if w == word or is_word(w)]
        if word in result:
            return result[word]
        if word in roots:
            return [word]
        return lemmas.get(word) if word in made or measured(word) else []

    # A form with a clitic is its word's (menggunakannya of guna, pentingnya, berani of berani).
    for form in list(freq):
        if not free(form):
            continue
        for b in bare(form):
            of = head(b)
            if of:
                lemmas[form] = of
                break
    # And an adverb in -nya is its word's (biasanya, akhirnya).
    for word in sorted(own):
        if word.endswith("nya") and word[:-3] in roots and pos[word] & word_pos <= {"adv", "adj"} and word not in overrides \
                and word not in OWN:
            result[word] = head(word[:-3]) or [word[:-3]]
    return result
