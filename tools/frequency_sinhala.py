"""
Sinhala for tools/build_dictionary.py, tools/build_frequency_list.py (--language si) and tools/check_course.py.

Spelling. A word is the Sinhala block's letters, vowel signs and al-lakuna, in NFC (ො as ෙ + ා composed), as the reader reads
it (its wordCharacters: the block and the joiner). The zero-width joiner after al-lakuna writes a conjunct (ශ්‍රී, ප්‍රශ්නය,
අවශ්‍ය, ක්‍ෂ) and belongs to the word; a joiner anywhere else (ශ‍්‍රී, ‍මේ) is a typing slip, and a corpus form with one is no
word. The non-joiner, which some sites write after al-lakuna (දක්‌වා, ක්‌), changes nothing to how a word looks: the reader
drops it (its characterSubstitutions), and so does everything here.

Words. The English Wiktionary has about 1,200 Sinhala entries, nearly without forms, so the pack adds the Sinhala words of
the Sinhala-English parallel word dictionary (kasunw22/sinhala-para-dict, MIT), each glossed with the English words it is
paired with that are English words (the English list's or the en-ru pack's).

The list. Sinhala writes a noun's cases, its indefinite and plural, and the clitics -ත් "also", -ද (question), -ම, -යි on
it (පොතට, පොතක්, පොත්වලින්, ළමයාගේ, ළමයෙක්, ගෙදරත්), and a verb's tenses (බලනවා, බැලුවා, බලන්න, බලමින්). A corpus form
is counted for the words it is a form of: by hand (SINHALA_FORMS: pronouns, the common verbs' irregular forms, the
literary language's auxiliaries), by the rules below, or as itself. A Wiktionary word or one of SINHALA_WORDS is a word
of its own and keeps its count; a word only the parallel dictionary has (which pairs forms too: නිවසට "house") is a word
only when no rule makes it a form of another.
"""
import json
import os
import re
import sqlite3
import unicodedata
from collections import defaultdict

ZWJ = "‍"
ZWNJ = "‌"
AL_LAKUNA = "්"
# The block's signs, vowels, consonants, al-lakuna and vowel signs, without the Lith digits and the kunddaliya; and the joiner.
LETTERS = re.compile("[ඁ-෥ෲෳ‍]+")
STRAY_JOINER = re.compile("(?<!්)‍|‍$")
VOWEL_SIGNS = set("ාැෑිීුූෘෙේෛොෝෞෟෲෳ")
VOWELS = {chr(c) for c in range(0x0d85, 0x0d97)}
CONSONANTS = {chr(c) for c in range(0x0d9a, 0x0dc7)}


def clean(word):
    """A Sinhala spelling as the reader has it: NFC, without the non-joiner."""
    return unicodedata.normalize("NFC", word).replace(ZWNJ, "").strip()


def is_word(word):
    """Whether [word] is Sinhala letters only, with no stray joiner."""
    return bool(LETTERS.fullmatch(word)) and not STRAY_JOINER.search(word)


PARA_DICT = "sinhala-para-dict-En-Si-dict-filtered-V2.tsv"
# English words of two letters or one that are glosses; others that short are the parallel dictionary's fragments.
SHORT_GLOSSES = {"a", "i", "am", "an", "as", "at", "be", "by", "do", "go", "he", "if", "in", "is", "it", "me", "my", "no",
                 "of", "oh", "ok", "on", "or", "ox", "so", "to", "up", "us", "we"}
GLOSSES_KEPT = 6


def english_words(root):
    """The English words a gloss may be: the English list's words and forms, and the en-ru pack's forms of its words."""
    words = set()
    with open(os.path.join(root, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency", "en.tsv"),
              encoding="utf-8") as f:
        for line in f:
            if not line.startswith("#"):
                words.add(line.split("\t", 1)[0].strip().lower())
    db = sqlite3.connect(os.path.join(root, "dictionaries", "en-ru.sqlite"))
    forms = {form for form, lemma in db.execute("SELECT form_lc, lemma FROM forms") if lemma.lower() in words}
    return {w for w in words | forms if re.fullmatch("[a-z]+", w) and (len(w) > 2 or w in SHORT_GLOSSES)}


def para_dict_glosses(root):
    """The parallel dictionary's Sinhala words, each with its English glosses, the most often paired first: word -> glosses."""
    english = english_words(root)
    paired = defaultdict(lambda: defaultdict(int))
    order = {}
    with open(os.path.join(root, "dictionaries", PARA_DICT), encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) != 2:
                continue
            word = clean(parts[1])
            gloss = parts[0].strip().lower()
            if not is_word(word) or gloss not in english:
                continue
            # Its English side has each word as typed (People, PEOPLE, people): the spellings a pair has count for it.
            paired[word][gloss] += 1
            order.setdefault((word, gloss), len(order))
    return {word: sorted(glosses, key=lambda g: (-glosses[g], order[(word, g)]))[:GLOSSES_KEPT] for word, glosses in paired.items()}


# The sources, each a share of the blend: the written language (news, books and the web, counted by the parallel
# dictionary's authors from the STM and SinMin corpora, 250 million words), the spoken one (subtitles), and Wikipedia.
COUNTS_URL = "https://raw.githubusercontent.com/kasunw22/sinhala-para-dict/main/word-frequencies/all_counts.txt"
SUBTITLES_URL = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/si/si_full.txt"
WIKIPEDIA = "sin_wikipedia_2021_100K"
WEIGHTS = {"written": 0.7, "subtitles": 0.2, "wikipedia": 0.1}
LABELS = {
    "written": "Sinhala-English parallel word dictionary word counts (STM and SinMin corpora; kasunw22/sinhala-para-dict, MIT)",
    "subtitles": "FrequencyWords si (OpenSubtitles 2018; CC BY-SA 4.0)",
    "wikipedia": f"Leipzig Corpora Collection {WIKIPEDIA} (terms at https://wortschatz.uni-leipzig.de/en/download)",
}
FORMS_LISTED = 300
# A form used less than this share of the blend (some 5 times in 250 million words) is a typo or a one-off, and is not listed.
LEAST_FORM_SHARE = 2e-8
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep", "phrase"}
NOMINAL_KINDS = {"noun", "adj", "num", "pron", "det", "unknown", "adv", "postp"}

# A noun's endings. After a final consonant with its inherent a (පොත: පොතක්, පොතට, පොතේ, පොතෙන්, පොත්, පොත්වලට), and
# after a final vowel (ගෙදර is the first kind; කෝපි: කෝපියක්, කෝපියට, කෝපියේ).
A_TAILS = ["ක්", "ක", "කට", "කින්", "කුත්", "කි", "ක්වත්", "ට", "ේ", "ේදී", "ෙන්", "ෙහි", "ින්", "්", "්වල", "්වලට", "්වලින්",
           "්වලදී", "්වලත්", "වල", "වලට", "වලින්", "ව", "ගේ", "ගෙන්"]
# A learned noun's stem takes its definite -ය and the cases on it (ප්‍රදේශ: ප්‍රදේශය, ප්‍රදේශයේ, ප්‍රදේශයක්), and a noun in -ය or
# -ව has its stem as its plural (ප්‍රශ්නය: ප්‍රශ්න, ප්‍රශ්නවලට; කතාව: කතා, කතාවලින්), a noun in -ව an animate plural in -න්
# (කාන්තාව: කාන්තාවන්).
YA_TAILS = ["ය", "යක්", "යක", "යකට", "යකින්", "යකි", "යට", "යේ", "යේදී", "යෙන්", "යෙහි"]
VA_TAILS = ["න්", "න්ට", "න්ගේ", "න්ව", "න්ගෙන්", "ෝ"]
VOWEL_TAILS = ["ට", "ගේ", "ව", "ගෙන්", "යක්", "යක", "යකට", "යකින්", "යේ", "යේදී", "යෙන්", "යෙහි", "යින්", "යට", "වල", "වලට",
               "වලින්", "ක්", "කට", "කින්", "දී", "හි"]
CONSONANT_TAILS = ["වල", "වලට", "වලින්", "ට", "දී", "ගේ"]
# An animate noun in -ා (ළමයා: ළමයාට, ළමයාගේ, ළමයාව; ළමයෙක්, ළමයෙකුට; ළමයි, ළමයින්ට; අම්මලා; බල්ලෝ).
ANIMATE_TAILS = ["ට", "ගේ", "ව", "ගෙන්", "ගේදී"]
ANIMATE_STEM_TAILS = ["ෙක්", "ෙකු", "ෙකුට", "ෙකුගේ", "ෙකුව", "ෙකුගෙන්", "කු", "කුට", "කුගේ", "කුව", "ි", "ින්", "ින්ට", "ින්ගේ",
                      "ින්ව", "ින්ගෙන්", "ලා", "ලාට", "ලාගේ", "ලාව", "ෝ", "ෝව"]
# A noun in -යා or -වා also takes -න් in the plural (දරුවන්, නායකයන්).
N_PLURAL_TAILS = ["න්", "න්ට", "න්ගේ", "න්ව", "න්ගෙන්"]
# A noun in -වරයා or -කරුවා takes -ු in the plural (ගුරුවරු, ගුරුවරුන්ට); one in -ියා -ී (නිලධාරීන්).
YA_PLURAL_TAILS = ["ු", "ුන්", "ුන්ට", "ුන්ගේ", "ුන්ව", "ුන්ගෙන්", "ුහු"]
IYA_PLURAL_TAILS = ["ීන්", "ීන්ට", "ීන්ගේ", "ීන්ව", "ීන්ගෙන්", "ීහු"]
# The clitics -ත් "also, even", -ද (question), -ම "just, very", -යි (emphasis), on any form.
CLITICS = ["ත්", "ද", "ම", "යි"]
# After the instrumental and ablative (පොතෙන්, ගමින්) only -ම (හොඳින්ම, මුලින්ම).
ABLATIVES = ("ෙන්", "ින්")

# A verb's forms by its present stem (බල of බලනවා), and its past by the fronted stem (බැල: බැලුවා, බැලූ, බැලීම).
PRESENT_TAILS = ["නවා", "නව", "නවලු", "න", "න්නේ", "න්නෙ", "න්න", "න්නට", "න්නම්", "න්නා", "යි", "ති", "මි", "මු", "මින්", "ද්දී",
                 "ද්දි", "නකොට", "නකම්", "නු", "න්නේය"]
# The colloquial participle and past relative of an a-stem of two syllables or more (බලලා, බලපු; not ක of කනවා).
A_PRESENT_TAILS = ["ලා", "ල", "පු"]
NASAL_PRESENT_TAILS = ["නවා", "නව", "නවලු", "න", "නේ", "නෙ", "නට", "නම්", "නා", "නු", "නකොට", "නකම්"]
PAST_TAILS = ["ුවා", "ුව", "ුවේ", "ුවෙ", "ූ", "ුවේය", "ුවාය", "ුවොත්", "ුවාම", "ීම", "ීමට", "ීමේ", "ීමෙන්", "ීමක්"]
E_PAST_TAILS = ["ුණා", "ුණ", "ුණේ", "ුණු", "ුණි", "ුණොත්", "ුණාම", "ී", "ිලා", "ිල", "ීම", "ීමට", "ීමේ", "ීමෙන්", "ිණ", "ිණි"]
# The negative prefix (නොකළ, නොවේ, නොදන්නා): a verb's form with it is the verb's.
NEGATIVE = "නො"


def _forms(lemma, text):
    return {lemma: text.split()}


# Forms no rule makes or makes wrongly, by hand: word -> its forms. The pronouns, the commonest verbs' irregular pasts and
# participles, and the literary language's verbs and auxiliaries.
_BY_HAND = {
    "මම": "මට මගේ මාව මගෙන් මා මාගේ මං මටත් මගේම මමත් මාත් මමද මටම මගෙ",
    "අපි": "අපට අපිට අපේ අපව අපිව අපෙන් අපිත් අප අපගේ අපටත් අපේම අපිම අපිද අපගෙන් අපිටත් අපෙ",
    "ඔයා": "ඔයාට ඔයාගේ ඔයාව ඔයාගෙන් ඔයාත් ඔයාද ඔයාටත් ඔයාගෙ",
    "ඔයාලා": "ඔයාලට ඔයාලාට ඔයාලගේ ඔයාලාගේ ඔයාලව ඔයාලාව ඔයාල",
    "ඔබ": "ඔබට ඔබේ ඔබගේ ඔබව ඔබෙන් ඔබත් ඔබද ඔබටත් ඔබලා ඔබලාට ඔබලාගේ",
    "ඔහු": "ඔහුට ඔහුගේ ඔහුව ඔහුගෙන් ඔහුත් ඔහුද ඔහුටත් ඔහුම",
    "ඇය": "ඇයට ඇයගේ ඇයව ඇයගෙන් ඇයත් ඇයද ඇයටත් ඈ ඇගේ ඇයම",
    "එයා": "එයාට එයාගේ එයාව එයාගෙන් එයාත් එයාද එයාලා එයාලට එයාලාට එයාලගේ එයාලාගේ",
    "ඔවුන්": "ඔවුන්ට ඔවුන්ගේ ඔවුන්ව ඔවුන්ගෙන් ඔවුහු ඔවුනට ඔවුන්ද ඔවුන්ටත් ඔවුනොත්",
    "උඹ": "උඹට උඹේ උඹව උඹලා උඹලට උඹලාට උඹත් උබ උබට උබව උබේ",
    "ඌ": "ඌව ඌට ඌගේ",
    "තමන්": "තමන්ට තමන්ගේ තමන්ව තමන්ගෙන් තමා තමාට තමාගේ තමාව තමාම තමන්ම තමන්ටම",
    "එය": "එයට එහි එයින් එයත් එයද එයම",
    "ඒක": "ඒකට ඒකේ ඒකෙන් ඒකත් ඒකද ඒකම ඒකෙ",
    "මේක": "මේකට මේකේ මේකෙන් මේකත් මේකද මේකම මේකෙ",
    "ඒවා": "ඒවාට ඒවායේ ඒවායින් ඒවාත් ඒවාද ඒවට",
    "ඕක": "ඕකට ඕකේ ඕකෙන් ඕකත් ඕකද",
    "උන්": "උන උන්ට උන්ගේ උන්ව",
    "මහතා": "මහතාගේ මහතාට මහතාව මහතාත් මහත්වරු මහත්වරුන් මහත්වරුනි",
    "මේවා": "මේවාට මේවායේ මේවායින් මේවාත් මේවාද",
    "කවුද": "කවුරු කාට කාගේ කාව කවුරුත් කවුරුහරි කවුරුවත් කවුරුන් කවුරුන්ට",
    "මොකක්": "මොකක්ද මොකද මොකටද මොකට මොකුත් මොකක්හරි",
    "මොනවා": "මොනවද මොනවාද මොනවත් මොනවාත්",
    "කෙනා": "කෙනෙක් කෙනෙකු කෙනෙකුට කෙනෙකුගේ කෙනකු කෙනකුට කෙනකුගේ කෙනෙක්ද කෙනෙක්වත් කෙනෙකුත් කෙනාට කෙනාගේ",
    "යනවා": "යන යන්න යන්නේ යනවා යයි යති යමු යමින් යද්දී යනකොට යනකම් ගියා ගිය ගියේ ගියේය ගියාය ගියත් ගියොත් ගියාම ගිහින් "
             "ගිහිල්ලා ගිහිං ගොස් ගොසින් යාම යෑම යාමට යෑමට යාමේ යෑමේ යන්නට යා ගියෝය ගියහ යන්නම් ගියාද යනවද යනවාද යනවලු "
             "යන්නෙ යනව ගියෙ යයිද යන්නද යාවි යැම යන්නයි",
    "එනවා": "එන එන්න එන්නේ එයි එති එමු එමින් එද්දී එනකොට ආවා ආව ආවේ ආවේය ආවාය ආවත් ආවොත් ආවාම ඇවිත් ඇවිල්ලා ඒම ඒමට "
            "ඊමට ඒමේ ආ ආවාද එනවද එනවාද එන්නම් එයිද එනව ආවෙ එන්නෙ ආවම ආවහම ආවට ආවද",
    "කරනවා": "කරන කරන්න කරන්නේ කරයි කරති කරමු කරමින් කරද්දී කරනකොට කළා කළ කළේ කළේය කළාය කළත් කළොත් කළාම කරලා කරල කර "
             "කොට කිරීම කිරීමට කිරීමේ කිරීමෙන් කිරීමක් කරනු කරන්නට කරපු කළහ කරගෙන කරන්නම් කරනවද කරනවාද කරයිද කරන්නද කරන්නත් "
             "කෙරුවා කෙරුව කෙරුවේ කරනව කළෙ කරන්නෙ කිරීමත් කිරීමද කරති කරන්නා කරන්නන් කරමි කිරීම් කිරීමේදී කරනවට කිරිමට කෙළේ",
    "වෙනවා": "වෙන වෙන්න වෙන්නේ වෙයි වේ වෙති වෙමු වෙමින් වෙද්දී වුණා වුණ වුණේ වුණු වුණත් වුණොත් වුණාම වී වෙලා වෙල විය වූ "
             "වූයේ වූයේය වූහ වීම වීමට වීමේ වීමෙන් වන වන්නේ වන්නට වනු වේද වෙයිද වෙනවද වෙනවාද වෙනකොට වුවත් වුවද වුවහොත් "
             "වේවා වුණාද වෙනව වෙන්නෙ වුණෙ වූවා වූවාය වන්නා වීමක් වනවා වුයේ",
    "දෙනවා": "දෙන දෙන්න දෙන්නේ දෙයි දෙති දෙමු දෙමින් දෙද්දී දුන්නා දුන්න දුන්නේ දුන්නේය දුන් දුන්නත් දුන්නොත් දීලා දීල දීම "
             "දීමට දීමේ දීමෙන් දෙනු දෙන්නම් දෙනවද දෙනවාද දෙන්නට දුන්නාද දෙයිද දෙනව දුන්නෙ දීමක් දුනි",
    "ගන්නවා": "ගන්න ගන්නේ ගනී ගනියි ගනිති ගනිමු ගනිමින් ගත්තා ගත්ත ගත්තේ ගත්තේය ගත් ගත්තත් ගත්තොත් ගෙන ගෙනත් අරන් අරගෙන "
              "ගැනීම ගැනීමට ගැනීමේ ගැනීමෙන් ගනු ගන්නට ගන්නම් ගන්නවද ගන්නවාද ගත්තාද ගන්නා ගන්නව ගත්තෙ ගැනීමක් ගනිද්දී ගත්හ",
    "කියනවා": "කියන කියන්න කියන්නේ කියයි කියති කියමු කියමින් කියද්දී කිව්වා කිව්ව කිව්වේ කිව්වත් කිව්වොත් කීවා කී කීවේය "
              "කියූ කියූයේ කියා කියලා කියල කීම කීමට කියනු කියන්නට කියනවද කියනවාද කිව්වාද කියාද කියනව කිව්වෙ කියන්නෙ කීවේ",
    "දන්නවා": "දන්න දන්නේ දනී දනියි දනිති දැන දන්නවද දන්නවාද දන්නව දන්නෙ දන්නා දැනීම",
    "දකිනවා": "දකින දකින්න දකින්නේ දකී දකියි දකිති දකිමු දකිමින් දැක්කා දැක්ක දැක්කේ දැක්කත් දැක්කොත් දුටු දුටුවේ දුටුවා දැක "
              "දැකලා දැකල දැකීම දැකීමට දකිනු දැක්කාද දකිනවද දකිනවාද දකින්නට දක්නට",
    "ඉන්නවා": "ඉන්න ඉන්නේ ඉන්නවද ඉන්නවාද හිටියා හිටිය හිටියේ හිටපු ඉඳලා ඉඳල ඉඳන් ඉද්දී ඉන්නකොට හිටියත් හිටියොත් ඉන්නම් "
              "ඉන්නත් ඉන්නව ඉන්නෙ හිටියෙ ඉඳිද්දී ඉදල ඉඳලම",
    "සිටිනවා": "සිටින සිටින්නේ සිටියි සිටී සිටිති සිටිමු සිටිමින් සිටියා සිටිය සිටියේ සිටියේය සිටියාය සිටි සිටීම සිටීමට "
               "සිටිද්දී සිටියහ සිටියෝය සිටින්න සිටින්නට සිටියදී",
    "තිබෙනවා": "තිබෙන තිබෙන්නේ තිබේ තිබෙයි තිබෙති තිබූ තිබූයේ තිබුණි තිබිණ තිබිණි තිබී තිබීම තිබීමට තිබීමේ තිබෙනු තිබේද "
               "තිබුණා තිබුණ තිබුණේ තිබුණු තිබුණත් තිබුණොත් තිබිලා තියෙනවා තියෙන තියෙන්න තියෙන්නේ තියෙනවද තියෙනවාද තියෙනව "
               "තියනවා තියන තියෙන්නෙ තිබ්බා තිබුණෙ තිබෙන්න තිබෙන්නට තිබිය තිබියදී",
    "කනවා": "කන කන්න කන්නේ කයි කති කමු කමින් කද්දී කෑවා කෑව කෑවේ කෑවත් කාලා කාල කා කෑ කනු කන්නට කනවද කනවාද කෑවාද කනව",
    "බොනවා": "බොන බොන්න බොන්නේ බොයි බොති බොමු බොමින් බිව්වා බීවා බීව බීවේ බීලා බීල බී බොනවද බොනවාද බොනව බොන්නට",
    "ලියනවා": "ලියන ලියන්න ලියන්නේ ලියයි ලියති ලියමු ලියමින් ලිව්වා ලිව්ව ලිව්වේ ලියූ ලියා ලියලා ලියල ලිවීම ලිවීමට ලියනු "
              "ලිව්වත් ලියන්නට ලියනවද",
    "කියවනවා": "කියවන කියවන්න කියවන්නේ කියවයි කියෙව්වා කියෙව්ව කියෙව්වේ කියවූ කියවා කියවලා කියවීම කියවීමට කියවන්නට",
    "ලබනවා": "ලද ලදී ලදුව ලැබුවා ලැබූ ලැබුවේය ලබා ලබන ලබන්නේ ලබයි ලබති ලැබීම ලැබීමට ලබමින් ලබන්න ලබන්නට ලබනු ලද්දේ ලදි ලද්දා",
    "ලැබෙනවා": "ලැබෙන ලැබේ ලැබෙයි ලැබුණා ලැබුණ ලැබුණේ ලැබුණු ලැබී ලැබිණ ලැබිණි ලැබීමට ලැබෙන්නේ ලැබෙන්න",
    "පවසනවා": "පවසයි පවසති පැවසීය පැවසූ පැවසුවා පවසා පවසමින් පැවසීම පවසන පවසන්නේ පැවසුවේය",
    "පැමිණෙනවා": "පැමිණ පැමිණි පැමිණියේ පැමිණියා පැමිණෙන පැමිණේ පැමිණීම පැමිණීමට පැමිණෙයි පැමිණිය පැමිණෙන්නේ පැමිණියහ",
    "සිතනවා": "සිතන සිතන්න සිතන්නේ සිතයි සිතුවා සිතූ සිතා සිතීම සිතමින් සිතුවේය",
    "හිතනවා": "හිතන හිතන්න හිතන්නේ හිතයි හිතුවා හිතුව හිතුවේ හිතලා හිතල හිතනවද හිතනවාද හිතන්නෙ",
    "ඉගෙන ගන්නවා": "",
    "බලනවා": "බලන බලන්න බලන්නේ බලයි බලමු බලමින් බැලුවා බැලුව බැලුවේ බැලූ බලා බලලා බලල බැලීම බැලීමට බලනවද බලනවාද බලන්නට",
    "අහනවා": "අහන අහන්න අහන්නේ අහයි ඇහුවා ඇහුව ඇහුවේ අහලා අහල ඇසූ අසා අසයි අසන ඇසීම ඇසීමට අහනවද",
    "යවනවා": "යවන යවන්න යැව්වා යැව්ව යැව්වේ යැවූ යවා යවලා යැවීම යැවීමට යවයි",
    "පවතිනවා": "පවතින පවතී පවතියි පවතිති පැවති පැවැති පැවතියේ පැවැතියේ පැවතුණා පැවතුණු පැවතීම පවතින්නේ පවතින්නට "
               "පැවතිණ පැවතිණි පවත්නා",
    "පවත්වනවා": "පවත්වන පවත්වා පවත්වාගෙන පැවැත්වූ පැවැත්වීම පැවැත්වීමට පැවැත්වීමේ පවත්වයි පවත්වන්න පවත්වන්නේ පැවැත්වෙන "
                "පැවැත්වේ පැවැත්විණ පැවැත්විණි පැවැත්වුණා පැවැත්වුණු පැවැත්වෙයි පවත්වමින් පැවැත්වුවා",
    "හඳුන්වනවා": "හඳුන්වන හඳුන්වා හැඳින්වූ හැඳින්වීම හැඳින්වේ හඳුන්වයි හඳුන්වන්නේ හැඳින්වෙන හඳුන්වමින් හඳුන්වාදීම",
    "ලබාදෙනවා": "ලබාදී ලබාදීම ලබාදීමට ලබාදීමේ ලබාදෙන ලබාදෙයි ලබාදුන් ලබාදුන්නා ලබාදෙන්න ලබාදෙන්නේ ලබාදුන්නේ ලබාදෙමින්",
    "ලබාගන්නවා": "ලබාගත ලබාගත් ලබාගැනීම ලබාගැනීමට ලබාගැනීමේ ලබාගන්න ලබාගන්නා ලබාගෙන ලබාගත්තා ලබාගන්නට ලබාගනී",
    "කරගන්නවා": "කරගත කරගත් කරගත්තා කරගන්න කරගැනීම කරගැනීමට කරගන්නට කරගන්නා කරගනී කරගත්තේ",
    "අවුරුද්ද": "අවුරුදු අවුරුද්දක් අවුරුද්දට අවුරුද්දේ අවුරුද්දෙන් අවුරුද්දකට අවුරුදුවල අවුරුදුවලට අවුරුදුවලින් අවුරුද්දකින්",
    "මිනිසා": "මිනිසුන් මිනිස්සු මිනිසුන්ට මිනිසුන්ගේ මිනිස්සුන් මිනිස්සුන්ට මිනිසෙක් මිනිසෙකු මිනිහා මිනිහෙක් මිනිහට මිනිහගේ "
              "මිනිහාගේ මිනිහාට",
    "සිසුවා": "සිසුන් සිසුන්ට සිසුන්ගේ සිසුවෙක් සිසුවකු සිසුවාගේ සිසුවාට",
    "සතා": "සතුන් සත්තු සතුන්ට සතුන්ගේ සත්තුන්",
    "සොයනවා": "සොයන සොයන්න සෙව්වා සෙව්ව සෙව්වේ සෙවූ සොයා සොයලා සෙවීම සෙවීමට සොයයි සොයමින් හොයනවා හොයන හොයන්න හෙව්වා "
              "හොයලා",
    "පෙන්වනවා": "පෙන්වන පෙන්වන්න පෙන්නුවා පෙන්වූ පෙන්වා පෙන්වලා පෙන්වීම පෙන්වීමට පෙන්වයි පෙන්වමින් පෙන්නන්න පෙන්නලා",
    "පිහිටනවා": "පිහිටි පිහිටියේ පිහිටා පිහිටන පිහිටීම",
    "හම්බවෙනවා": "හම්බවුණා හම්බවෙන හම්බවෙන්න හම්බවුණේ",
}
SINHALA_FORMS = {}
for _lemma, _text in _BY_HAND.items():
    for _form in _text.split():
        SINHALA_FORMS.setdefault(_form, _lemma)
del _lemma, _text, _form
# Everyday words the pack lacks or has under another kind, with the kind the rules decline them by.
SINHALA_WORDS = {
    "යනවා": "verb", "ඉන්නවා": "verb", "තිබෙනවා": "verb", "ලැබෙනවා": "verb", "පැමිණෙනවා": "verb", "කියවනවා": "verb",
    "යවනවා": "verb", "හම්බවෙනවා": "verb", "ඔවුන්": "pron", "තමන්": "pron", "ඒක": "pron", "මේක": "pron", "ඒවා": "pron",
    "මේවා": "pron", "මොකක්": "pron", "එය": "pron", "එයා": "pron",
    # Words a rule would take for another's form: නම් "if" (නම "name"), සිට "from" (සිටිනවා), ඇති, ඇත "there is".
    "නම්": "conj", "සිට": "postp", "ඇති": "adj", "ඇත": "verb", "නැහැ": "adv", "නැත": "adv", "නැති": "adj",
    # Postpositions and conjunctions made from a noun's case (විසින් "by", මගින් "by means of", නමුත් "but", වගේ "like"),
    # and words of their own spelled as another's form (හැකි "able", සමත් "successful", පත් "appointed", යම් "some").
    "විසින්": "postp", "මගින්": "postp", "මඟින්": "postp", "නමුත්": "conj", "එහෙත්": "conj", "එහෙම": "adv",
    "එහෙයින්": "conj", "වගේ": "postp", "ඉතින්": "conj", "හැකි": "adj", "සමත්": "adj", "වහන්සේ": "noun", "පත්": "adj",
    "යම්": "det", "තරම්": "postp", "ඕනෑම": "det", "මෙන්ම": "conj", "ඕනෑ": "adj",
    # Postpositions and adverbs a noun in -ය or -ව would take as its stem or its form (මත "on", not මතය "opinion"'s).
    "මත": "postp", "පසු": "postp", "පසුව": "adv", "යුතු": "adj", "පිළිබඳ": "postp", "පිළිබඳව": "postp", "අනුව": "postp",
    "අනතුරුව": "adv", "සිදු": "adj", "සඳහන්": "adj", "නව": "adj", "සම්බන්ධ": "adj", "සම්බන්ධයෙන්": "postp", "අදාළ": "adj",
    "ඇතුළු": "adj", "අමතර": "adj", "කලින්": "adv", "කල්": "noun", "නිකුත්": "adj", "නොහැකි": "adj", "තබනවා": "verb",
    "යොදනවා": "verb", "සොයනවා": "verb", "ඉල්ලනවා": "verb", "පෙන්වනවා": "verb", "පවතිනවා": "verb", "පිහිටනවා": "verb",
    # Words a verb's rule form is spelled as: වඩා "than", වහාම "at once", ගහන "population", වහල "roof", දක්වා "until".
    "වඩා": "adv", "වහාම": "adv", "ගහන": "noun", "වහල": "noun", "දක්වා": "postp", "ගණන": "noun", "යැයි": "particle",
}
# Everyday words the pack lacks, with their kind and an English gloss: they go into the pack too (build_dictionary).
HAND_ENTRIES = {
    "ඒ": ("det", "that, those; it"), "මහතා": ("noun", "Mr.; gentleman"), "මහත්මිය": ("noun", "Mrs.; lady"),
    "මහත්තයා": ("noun", "sir, gentleman"), "ඕක": ("pron", "that (near you)"), "එදා": ("adv", "that day; then"),
    "අතන": ("adv", "over there"), "ඔතන": ("adv", "there (near you)"), "කෙරෙහි": ("postp", "towards, regarding"),
    "මෙවර": ("adv", "this time"), "ආදී": ("det", "and so on, etc."), "ආදිය": ("noun", "and so on, etc."),
    "වනවිට": ("conj", "by the time"), "මිය": ("adj", "dead (මිය යනවා: to die)"), "බෞද්ධ": ("adj", "Buddhist"),
    "උදෙසා": ("postp", "for, for the sake of"), "කවදා": ("adv", "when?"), "විතර": ("postp", "about; only"),
    "හැබැයි": ("conj", "but, however"), "බැගින්": ("postp", "each, apiece"), "නෙමෙයි": ("adv", "not (it is not)"),
    "පස්සේ": ("postp", "after; behind"), "දැයි": ("particle", "whether"), "යැයි": ("particle", "that (closing a quote)"),
    "මොවුන්": ("pron", "these people, they"), "මොහු": ("pron", "this man, he"), "නොමැත": ("adv", "there is not"),
    "නැතහොත්": ("conj", "or else, otherwise"), "එනිසා": ("conj", "therefore"), "හැමදාම": ("adv", "every day, always"),
    "වැඩිපුර": ("adv", "more, too much"), "එකල": ("adv", "at that time"), "එවකට": ("adv", "at that time"),
    "එදින": ("adv", "that day"), "එවන්": ("det", "such"), "ලාංකික": ("adj", "Sri Lankan"), "ඉංග්‍රීසි": ("noun", "English"),
    "චීන": ("adj", "Chinese"), "ඉන්දීය": ("adj", "Indian"), "ආසියානු": ("adj", "Asian"), "ඇමරිකානු": ("adj", "American"),
    "මුස්ලිම්": ("adj", "Muslim"), "උන්": ("pron", "they (colloquial)"), "තිබෙනවා": ("verb", "to be, to exist, to have (of things)"),
    "පැමිණෙනවා": ("verb", "to arrive, to come"), "තබනවා": ("verb", "to put, to place, to keep"),
    "මේවා": ("pron", "these, these things"), "හම්බවෙනවා": ("verb", "to meet, to be found"),
    "ලබාදෙනවා": ("verb", "to provide, to give"), "පවත්වනවා": ("verb", "to hold, to conduct, to maintain"),
    "හඳුන්වනවා": ("verb", "to introduce, to call (by a name)"), "ලබාගන්නවා": ("verb", "to obtain, to get"),
    "කරගන්නවා": ("verb", "to get done, to do for oneself"), "මිනිසා": ("noun", "man, human being"),
}
SINHALA_WORDS.update({word: kind for word, (kind, _) in HAND_ENTRIES.items()})
# Spellings of one word, the second the one listed: නෑ is නැහැ, said.
SPELLINGS = {"නෑ": "නැහැ", "නැද්ද": "නැහැ", "නැත්තේ": "නැත", "නැතත්": "නැත", "ඇත්තේ": "ඇත", "ඇතැයි": "ඇත", "ඇතත්": "ඇත",
             "නැතිව": "නැති", "ඇතිව": "ඇති", "ඕන": "ඕනෑ", "ඕනේ": "ඕනෑ", "ඕනි": "ඕනෑ", "හැක": "හැකි", "ලඟ": "ළඟ",
             "ලඟට": "ළඟ", "යුතුය": "යුතු", "යුතුයි": "යුතු", "යුත්තේ": "යුතු", "ඒව": "ඒවා", "හැකිය": "හැකි", "හැකියි": "හැකි",
             "නොහැක": "නොහැකි", "නොහැකිය": "නොහැකි", "වඩාත්": "වඩා",
             # Two entries of the pack for one word.
             "මරණ": "මරණය", "වර්ග": "වර්ගය", "බල": "බලය", "මතකය": "මතක",
             # Common misspellings (ල for ළ, න for ණ, ද for ඳ, ග for ඟ) and older spellings (ක්‍ෂ, කථා).
             "ලග": "ළඟ", "ලගට": "ළඟ", "ළග": "ළඟ", "ළගට": "ළඟ", "ලඟම": "ළඟ", "ඕනද": "ඕනෑ", "ඔන": "ඕනෑ", "ඕනම": "ඕනෑම",
             "පහල": "පහළ", "පහලට": "පහළ", "ඉහල": "ඉහළ", "ඉහලට": "ඉහළ", "ඊලග": "ඊළඟ", "ඊලඟ": "ඊළඟ", "ඊලඟට": "ඊළඟ",
             "ඊලගට": "ඊළඟ", "ඊළගට": "ඊළඟ", "ඊළග": "ඊළඟ", "පමන": "පමණ", "පිළිබද": "පිළිබඳ", "සදහන්": "සඳහන්",
             "සදහා": "සඳහා", "තුල": "තුළ", "කවදද": "කවදා", "විතරයි": "විතර", "පස්සෙ": "පස්සේ", "ඇගට": "ඇඟ", "ඇග": "ඇඟ",
             "ඔතනම": "ඔතන", "යෑයි": "යැයි", "තත්වය": "තත්ත්වය", "ලක්‍ෂ": "ලක්ෂය", "බරපතළ": "බරපතල", "පොලීසිය": "පොලිසිය",
             "පක්‍ෂය": "පක්ෂය", "පරීක්‍ෂණ": "පරීක්ෂණය", "අමාත්‍යංශය": "අමාත්‍යාංශය", "වේලාව": "වෙලාව", "සහභාගී": "සහභාගි",
             "තරඟය": "තරගය", "කථාව": "කතාව", "කථා": "කතාව", "සහාය": "සහය", "කාලේ": "කාලය", "ඇත්නම්": "ඇත",
             "යුතුව": "යුතු", "අම්ම": "අම්මා", "තරඟ": "තරගය", "පක්‍ෂ": "පක්ෂය",
             "ලිපි": "ලිපිය", "නීති": "නීතිය", "පරීක්ෂණ": "පරීක්ෂණය"}
SINHALA_FORMS.update(SPELLINGS)
# Corpus spellings that are no words: letters and endings written apart.
NOT_WORDS = {"ක්", "ට", "ගේ", "ය", "ව", "ක", "ද", "ම", "යි", "ත්", "වල", "වලට", "වලින්", "වලදී", "වලත්", "ගෙන්",
             # English letters spelled out (D, T, K, C) and people's names the parallel dictionary pairs with a word.
             "ඩී", "ටී", "කේ", "සී", "බී", "පී", "ජේ", "එස්", "එල්", "ආර්", "එච්", "ඩබ්ලිව්", "ජී", "එම්", "සිල්වා", "තිස්ස",
             "රනිල්", "වික්‍රමසිංහ", "පෙරේරා", "රාජපක්ෂ", "මහින්ද", "සරත්", "කුමාරය",
             # Pieces of words: ටෙලි of ටෙලි නාට්‍ය, යුත් of යුත්තේ, and a typo.
             "ටෙලි", "යුත්", "ප්‍රමාණයකය"}
# A stem is its -ය or -ව noun's when that noun is at least this common beside it.
STEM_SHARE = 0.2
# A word glossed apart from the noun it looks like a form of is a word of its own when it is this many times commoner.
UNRELATED_SHARE = 3


# English words too common in glosses to tell two Sinhala words apart.
GLOSS_STOPS = {"the", "a", "an", "to", "of", "be", "in", "on", "for", "and", "or", "with", "by", "at", "from", "as", "that",
               "this", "it", "which", "who", "not", "no", "so", "up", "out", "into", "than", "then", "also"}


def gloss_lemmas(dictionaries, root):
    """Each pack word's glosses as English lemmas (the English list's words its forms belong to), without GLOSS_STOPS."""
    lemma_of = {}
    with open(os.path.join(root, "feature", "frequency", "src", "commonMain", "composeResources", "files", "frequency", "en.tsv"),
              encoding="utf-8") as f:
        for line in f:
            if line.startswith("#"):
                continue
            head, _, forms = line.rstrip("\n").partition("\t")
            for form in [head] + forms.split():
                lemma_of.setdefault(form.lower(), head.lower())
    out = defaultdict(set)
    db = sqlite3.connect(os.path.join(dictionaries, "si-en.sqlite"))
    for word, senses in db.execute("SELECT word_lc, senses FROM entries"):
        for sense in json.loads(senses):
            for gloss in sense.get("glosses", []):
                for token in re.findall("[a-z]+", gloss.lower()):
                    lemma = lemma_of.get(token, token)
                    if lemma not in GLOSS_STOPS and token not in GLOSS_STOPS:
                        out[word].add(lemma)
    return out


def letters(word):
    return sum(1 for c in word if c in CONSONANTS or c in VOWELS)


def with_clitics(forms):
    out = set(forms)
    for form in forms:
        for clitic in CLITICS:
            if form.endswith(ABLATIVES) and clitic != "ම":
                continue
            if form.endswith(AL_LAKUNA) and clitic == "ත්":
                out.add(form[:-1] + "ුත්")
            elif form.endswith("ක්") and clitic == "යි":
                out.add(form[:-1] + "ි")
            else:
                out.add(form + clitic)
    return out


def nominal_forms(word, known=frozenset()):
    """
    A noun's (adjective's, number's) case, indefinite and plural forms, with the clitics on them. A stem whose -ය noun is
    a word of its own ([known]: රජ "king", රජය "government"; මත "on", මතය "opinion") does not take the -ය.
    """
    last = word[-1]
    if last == "ා":
        stem = word[:-1]
        forms = {word + t for t in ANIMATE_TAILS} | {stem + t for t in ANIMATE_STEM_TAILS}
        if stem.endswith(("ය", "ව")):
            forms |= {stem + t for t in N_PLURAL_TAILS}
        if stem.endswith("ය") and len(stem) > 2:
            forms |= {stem[:-1] + t for t in YA_PLURAL_TAILS}
            if stem.endswith("ිය"):
                forms |= {stem[:-2] + t for t in IYA_PLURAL_TAILS}
    elif last in CONSONANTS:
        forms = {word + t for t in A_TAILS}
        if not word.endswith(("ය", "ව")) and word + "ය" not in known:
            forms |= {word + t for t in YA_TAILS}
    elif last == AL_LAKUNA:
        forms = {word + t for t in CONSONANT_TAILS}
    else:
        forms = {word + t for t in VOWEL_TAILS}
    if word.endswith("ව"):
        forms |= {word + t for t in VA_TAILS}
    # An inanimate noun in -ිය has its plural in -ි and -ීන් (ජාතිය: ජාති, ජාතීන්).
    if word.endswith("ිය") and letters(word) >= 3:
        forms |= {word[:-2] + t for t in IYA_PLURAL_TAILS} | {word[:-1] + t for t in ["වල", "වලට", "වලින්"]}
    # An animate noun in -ු takes -වන් in the plural (සැකකරු: සැකකරුවන්).
    if word.endswith("ු"):
        forms |= {word + "ව" + t for t in N_PLURAL_TAILS}
    return with_clitics(forms | {word}) - {word}


def stem_of(word):
    """The stem a noun in -ය or -ව has as its plural (ප්‍රශ්න of ප්‍රශ්නය, කතා of කතාව), or None."""
    # (An adjective's adverb in -ව keeps its adjective: සරලව is සරල's.)
    before = word[-2:-1]
    if letters(word) >= 3 and (word[-1] == "ය" and (before in CONSONANTS or before in VOWEL_SIGNS - {"ි"})
                               or word[-1] == "ව" and before in VOWEL_SIGNS - {"ි"}):
        return word[:-1]
    return None


def front(stem):
    """The stem with its first vowel fronted, as a past takes it (බල: බැල, අහ: ඇහ, කාප: කෑප)."""
    first = stem[0]
    if first == "අ":
        return "ඇ" + stem[1:]
    if first == "ආ":
        return "ඈ" + stem[1:]
    if first in CONSONANTS:
        nxt = stem[1:2]
        if nxt == "ා":
            return first + "ෑ" + stem[2:]
        if nxt and nxt not in VOWEL_SIGNS and nxt != AL_LAKUNA and nxt != ZWJ:
            return first + "ැ" + stem[1:]
    return stem


# Verbs whose forms a compound made with them takes (සිදුවෙනවා: සිදුවූ, සිදුවීම; ලබාගන්නවා: ලබාගත්).
COMPOUNDED = ("වෙනවා", "කරනවා", "ගන්නවා", "දෙනවා")


def verb_forms(word):
    """
    A verb's forms by rule from its -නවා form: the present's, and the past's of an a-stem or e-stem; a compound's are
    its last verb's (සිදුවෙනවා as වෙනවා).
    """
    if not word.endswith("නවා") or " " in word:
        return set()
    for base in COMPOUNDED:
        if word.endswith(base) and len(word) > len(base) and word not in _BY_HAND:
            head = word[:-len(base)]
            return with_clitics({head + form for form in _BY_HAND[base].split()} | {head + base}) - {word}
    stem = word[:-3]
    if not stem:
        return set()
    if stem.endswith(AL_LAKUNA):
        forms = {stem + t for t in NASAL_PRESENT_TAILS}
    else:
        forms = {stem + t for t in PRESENT_TAILS}
        if stem[-1] in CONSONANTS and letters(stem) >= 2:
            forms |= {stem + t for t in A_PRESENT_TAILS}
    last = stem[-1]
    if last == "ෙ" and letters(stem) >= 2:
        forms |= {stem[:-1] + t for t in E_PAST_TAILS} | {stem[:-1] + "ේ"}
    elif last in CONSONANTS and letters(stem) >= 2:
        forms |= {front(stem) + t for t in PAST_TAILS} | {stem + "ා"}
    elif last == "ි":
        forms.add(stem[:-1] + "ී")
    return with_clitics(forms) - {word}


def lexicon(dictionaries):
    """(own words by kind, the parallel dictionary's words, names): own words are the Wiktionary's and SINHALA_WORDS."""
    db = sqlite3.connect(os.path.join(dictionaries, "si-en.sqlite"))
    own, weak, names = defaultdict(set), set(), set()
    for word, pos in db.execute("SELECT word_lc, pos FROM entries"):
        if " " in word or "-" in word or not is_word(word):
            continue
        if pos == "name":
            names.add(word)
        elif pos == "unknown":
            weak.add(word)
        elif pos in WORD_KINDS:
            own[word].add(pos)
    for word, kind in SINHALA_WORDS.items():
        own[word].add(kind)
    for form, word in SINHALA_FORMS.items():
        if form != word:
            own.pop(form, None)
            weak.discard(form)
    return own, weak - set(own) - set(NOT_WORDS), names - set(own)


def word_forms(word, kinds, known=frozenset()):
    forms = with_clitics({word}) - {word}
    if kinds & NOMINAL_KINDS and letters(word) >= 2:
        forms |= nominal_forms(word, known)
        stem = stem_of(word)
        if stem:
            # And its stem's forms (දිනය: දින, දිනක, දිනට, දිනවල).
            forms |= {stem} | nominal_forms(stem, known)
    if "verb" in kinds or word.endswith("නවා"):
        forms |= verb_forms(word)
    return forms


def tail_cost(word, form):
    """How many endings [form] adds to [word]: one, or two with a clitic."""
    tail = form[len(word):] if form.startswith(word) else form
    return 2 if any(tail.endswith(c) and tail != c for c in CLITICS) or tail.endswith("ුත්") else 1


def _source_counts(download):
    found = {}
    path = download(COUNTS_URL, "sinhala-para-dict-all_counts.txt")
    if path:
        counts = defaultdict(int)
        with open(path, encoding="utf-8") as f:
            for line in f:
                word, _, n = line.rstrip("\n").rpartition(",")
                if n.isdigit():
                    counts[word] += int(n)
        found["written"] = counts
    path = download(SUBTITLES_URL, "frequencywords-si.txt")
    if path:
        counts = defaultdict(int)
        with open(path, encoding="utf-8") as f:
            for line in f:
                parts = line.split()
                if len(parts) == 2 and parts[1].isdigit():
                    counts[parts[0]] += int(parts[1])
        found["subtitles"] = counts
    path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{WIKIPEDIA}.tar.gz", f"leipzig-{WIKIPEDIA}.tar.gz")
    if path:
        import io
        import tarfile
        counts = defaultdict(int)
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-words.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[2].isdigit():
                    counts[parts[1]] += int(parts[2])
        found["wikipedia"] = counts
    return found


def blended(found):
    """form -> its share of the blend, the forms as the reader reads them (NFC, no non-joiner, no stray joiner)."""
    share = defaultdict(float)
    for kind, counts in found.items():
        total = sum(counts.values())
        for form, n in counts.items():
            form = clean(form)
            if is_word(form):
                share[form] += WEIGHTS[kind] * n / total
    return share


def build(size, download, dictionaries=None):
    dictionaries = dictionaries or os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
    found = _source_counts(download)
    label = " + ".join(LABELS[k] for k in found)
    share = blended(found)
    own, weak, names = lexicon(dictionaries)
    glosses = gloss_lemmas(dictionaries, os.path.dirname(dictionaries))

    def unrelated(word, maker):
        """
        Whether [word] is no form of the short noun [maker] it looks like a form of: it is far commoner than the noun, and the
        pack glosses the two with no English word in common (නායක "leader" of නාය "landslide", කෙසේ "how" of කෙස "hair").
        A longer noun's forms are glossed apart often enough (කොටසක් "part", කොටස "section").
        """
        return (letters(maker) <= 2 and not maker.endswith("නවා")
                and share.get(word, 0.0) > UNRELATED_SHARE * share.get(maker, 0.0) and bool(glosses.get(word)) and bool(glosses.get(maker))
                and not glosses[word] & glosses[maker] and word not in verbal)

    kinds = {w: ks for w, ks in own.items()}
    # Every word's forms: form -> the words that make it.
    made = defaultdict(set)
    verbal = defaultdict(set)
    known = set(own) | weak
    for word in list(own) + sorted(weak):
        ks = kinds.get(word, {"unknown"})
        for form in word_forms(word, ks, known):
            if form in share or form in weak:
                made[form].add(word)
        for form in verb_forms(word):
            verbal[form].add(word)
    # A word only the parallel dictionary has is a word when no shorter word makes it, and when it is no stem of a noun in -ය
    # or -ව that is common enough beside it (ප්‍රශ්න of ප්‍රශ්නය; not ජාතික "national" of the rare ජාතිකය).
    stems = {}
    for word in set(own) | weak:
        stem = stem_of(word)
        if stem in weak and share.get(word, 0.0) >= STEM_SHARE * share.get(stem, 0.0):
            stems[stem] = word
    # A verb first, as its forms are shorter than it (හදන of හදනවා).
    words = set(own) | {w for w in weak if w.endswith("නවා") and not any(m in own for m in made.get(w, ()))}
    for word in sorted(weak, key=len):
        if word not in stems and not any(w in words and not unrelated(word, w) for w in made.get(word, ()) if w != word):
            words.add(word)
    # A plural shorter than its noun (නිලධාරීන් of නිලධාරියා, ළමයි of ළමයා) is the noun's.
    for word in sorted(words & weak, key=len):
        if any(w in words and w.endswith("ා") and len(w) > len(word) and not unrelated(word, w) for w in made.get(word, ())):
            words.discard(word)

    def heads_of(form):
        """(the word the form is itself, or None; the other words it is a form of)."""
        if form in SINHALA_FORMS:
            return None, {SINHALA_FORMS[form]}
        if form in NOT_WORDS or form in names and form not in made:
            return None, set()
        if form.startswith(NEGATIVE) and len(form) > 3 and form not in words:
            rest = form[len(NEGATIVE):]
            if rest in SINHALA_FORMS and SINHALA_FORMS[rest].endswith("නවා"):
                return None, {SINHALA_FORMS[rest]}
            verbs = {w for w in verbal.get(rest, ()) if w in words}
            if verbs:
                return None, verbs
        own_word = form if form in words else None
        makers = {w for w in made.get(form, ()) if w in words and w != form}
        verbs = {w for w in makers if form in verbal and w in verbal[form]}
        if own_word:
            return own_word, verbs
        nouns = makers - verbs
        if nouns:
            fewest = min(tail_cost(w, form) for w in nouns)
            nouns = {w for w in nouns if tail_cost(w, form) == fewest}
            # Of those, the ones that share the most of it (මහලට is මහල + ට, not මහා's stem මහ + ලට).
            longest = max(len(os.path.commonprefix([w, form])) for w in nouns)
            nouns = {w for w in nouns if len(os.path.commonprefix([w, form])) == longest}
        return None, verbs | nouns

    heads = {form: heads_of(form) for form in share}
    alone = defaultdict(float)
    for form, (own_word, others) in heads.items():
        every = ({own_word} if own_word else set()) | others
        if len(every) == 1:
            alone[next(iter(every))] += share[form]
    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, value in share.items():
        own_word, others = heads[form]
        if own_word and others and max(alone.get(o, 0.0) for o in others) <= value:
            chosen = {own_word}
        else:
            chosen = others or ({own_word} if own_word else set())
        if not chosen:
            continue
        weights = {h: alone.get(h, 0.0) for h in chosen}
        whole = sum(weights.values())
        for head in chosen:
            part = weights[head] / whole if whole else 1 / len(chosen)
            if part <= 0:
                continue
            scores[head] += value * part
            forms[head][form] = forms[head].get(form, 0.0) + value * part
    ranked = sorted(scores, key=lambda w: (-scores[w], w))[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted((f for f in forms[word] if forms[word][f] >= LEAST_FORM_SHARE), key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


def checker_word(word, ranks):
    """
    The listed form [word] is with a clitic written on that the list lacks with it (ගෙදරත් of ගෙදර, පොතෙනුත් of පොතෙන්,
    පොතකි of පොතක්), or None (check_course).
    """
    candidates = []
    if word.endswith("ුත්"):
        candidates.append(word[:-3] + AL_LAKUNA)
    if word.endswith("ත්"):
        candidates.append(word[:-2])
    if word.endswith(("ද", "ම")):
        candidates.append(word[:-1])
    if word.endswith("යි"):
        candidates.append(word[:-2])
    if word.endswith("කි"):
        candidates.append(word[:-1] + AL_LAKUNA)
    return next((c for c in candidates if letters(c) >= 2 and c in ranks), None)
