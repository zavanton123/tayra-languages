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
A_TAILS = ["ක්", "ක", "කට", "කින්", "කුත්", "කි", "ක්වත්", "කදී", "ට", "දී", "ේ", "ේදී", "ෙන්", "ෙහි", "ින්", "්", "්වල", "්වලට", "්වලින්",
           "්වලදී", "්වලත්", "වල", "වලට", "වලින්", "ව", "ගේ", "ගෙන්"]
# A learned noun's stem takes its definite -ය and the cases on it (ප්‍රදේශ: ප්‍රදේශය, ප්‍රදේශයේ, ප්‍රදේශයක්), and a noun in -ය or
# -ව has its stem as its plural (ප්‍රශ්නය: ප්‍රශ්න, ප්‍රශ්නවලට; කතාව: කතා, කතාවලින්), a noun in -ව an animate plural in -න්
# (කාන්තාව: කාන්තාවන්).
YA_TAILS = ["ය", "යක්", "යක", "යකට", "යකින්", "යකි", "යකදී", "යට", "යේ", "යේදී", "යෙන්", "යෙහි"]
VA_TAILS = ["න්", "න්ට", "න්ගේ", "න්ව", "න්ගෙන්", "ෝ"]
VOWEL_TAILS = ["ට", "ගේ", "ව", "ගෙන්", "යක්", "යක", "යකට", "යකින්", "යකදී", "යේ", "යේදී", "යෙන්", "යෙහි", "යින්", "යට", "වල", "වලට",
               "වලින්", "ක්", "කට", "කින්", "කදී", "දී", "හි"]
CONSONANT_TAILS = ["වල", "වලට", "වලින්", "ට", "දී", "ගේ"]
# An animate noun in -ා (ළමයා: ළමයාට, ළමයාගේ, ළමයාව; ළමයෙක්, ළමයෙකුට; ළමයි, ළමයින්ට; අම්මලා; බල්ලෝ).
ANIMATE_TAILS = ["ට", "ගේ", "ව", "ගෙන්", "ගේදී"]
ANIMATE_STEM_TAILS = ["ෙක්", "ෙකු", "ෙකුට", "ෙකුගේ", "ෙකුව", "ෙකුගෙන්", "කු", "කුට", "කුගේ", "කුව", "ි", "ින්", "ින්ට", "ින්ගේ",
                      "ින්ව", "ින්ගෙන්", "ලා", "ලාට", "ලාගේ", "ලාව", "ෝ", "ෝව"]
# A noun in -යා or -වා also takes -න් in the plural (දරුවන්, නායකයන්).
N_PLURAL_TAILS = ["න්", "න්ට", "න්ගේ", "න්ව", "න්ගෙන්"]
# An animate noun in -ා may take -ුන් in the plural (පුතා: පුතුන්).
U_PLURAL_TAILS = ["ුන්", "ුන්ට", "ුන්ගේ", "ුන්ව", "ුන්ගෙන්"]
# A noun in -වරයා or -කරුවා takes -ු in the plural (ගුරුවරු, ගුරුවරුන්ට); one in -ියා -ී (නිලධාරීන්).
YA_PLURAL_TAILS = ["ු", "ුන්", "ුන්ට", "ුන්ගේ", "ුන්ව", "ුන්ගෙන්", "ුහු"]
IYA_PLURAL_TAILS = ["ීන්", "ීන්ට", "ීන්ගේ", "ීන්ව", "ීන්ගෙන්", "ීහු"]
# (Its plural is often spelled with a short ි: නිලධාරින්.)
IYA_SHORT_PLURAL_TAILS = ["ින්", "ින්ට", "ින්ගේ", "ින්ව", "ින්ගෙන්"]
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
PAST_TAILS = ["ුවා", "ුව", "ුවේ", "ුවෙ", "ූ", "ුවේය", "ුවාය", "ුවොත්", "ුවාම", "ීම", "ීමට", "ීමේ", "ීමෙන්", "ීමක්", "ීම්",
              "ීමේදී", "ූහ"]
# The written language's past in -ිය, -ීය on the fronted stem (ගෙවිය, තැබිය, බැලීය), often spelled as a noun (බිරිය, දිවිය).
LITERARY_PAST_TAILS = ["ිය", "ීය"]
E_PAST_TAILS = ["ුණා", "ුණ", "ුණේ", "ුණු", "ුණි", "ුණොත්", "ුණාම", "ුණාය", "ුණේය", "ී", "ිලා", "ිල", "ීම", "ීමට", "ීමේ", "ීමෙන්",
                "ීමක්", "ීමේදී", "ිණ", "ිණි", "ීම්"]
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
    "එයා": "එයාට එයාගේ එයාව එයාගෙන් එයාත් එයාද එයාලා එයාලට එයාලාට එයාලගේ එයාලාගේ එයාගෙ",
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
             "යන්නෙ යනව ගියෙ යයිද යන්නද යාවි යැම යන්නයි යැමට යැමේ යාමෙන් ගියාට යනවට",
    "එනවා": "එන එන්න එන්නේ එයි එති එමු එමින් එද්දී එනකොට ආවා ආව ආවේ ආවේය ආවාය ආවත් ආවොත් ආවාම ඇවිත් ඇවිල්ලා ඒම ඒමට "
            "ඊමට ඒමේ ආ ආවාද එනවද එනවාද එන්නම් එයිද එනව ආවෙ එන්නෙ ආවම ආවහම ආවට ආවද ආපු",
    "කරනවා": "කරන කරන්න කරන්නේ කරයි කරති කරමු කරමින් කරද්දී කරනකොට කළා කළ කළේ කළේය කළාය කළත් කළොත් කළාම කරලා කරල කර "
             "කොට කිරීම කිරීමට කිරීමේ කිරීමෙන් කිරීමක් කරනු කරන්නට කරපු කළහ කරගෙන කරන්නම් කරනවද කරනවාද කරයිද කරන්නද කරන්නත් "
             "කෙරුවා කෙරුව කෙරුවේ කරනව කළෙ කරන්නෙ කිරීමත් කිරීමද කරති කරන්නා කරන්නන් කරමි කිරීම් කිරීමේදී කරනවට කිරිමට කෙළේ "
             "කළහොත් කළාට කරන්නැයි කිරීමය කරන්ඩ කිරිම්",
    "වෙනවා": "වෙන වෙන්න වෙන්නේ වෙයි වේ වෙති වෙමු වෙමින් වෙද්දී වුණා වුණ වුණේ වුණු වුණත් වුණොත් වුණාම වී වෙලා වෙල විය වූ "
             "වූයේ වූයේය වූහ වීම වීමට වීමේ වීමෙන් වන වන්නේ වන්නට වනු වේද වෙයිද වෙනවද වෙනවාද වෙනකොට වුවත් වුවද වුවහොත් "
             "වේවා වුණාද වෙනව වෙන්නෙ වුණෙ වූවා වූවාය වන්නා වීමක් වනවා වුයේ වුව වීම් වන්නේය වූවත් වූවද වීමේදී වීය විණි වූවන් "
             "වෙන්ඩ උනහම වුණහම",
    "දෙනවා": "දෙන දෙන්න දෙන්නේ දෙයි දෙති දෙමු දෙමින් දෙද්දී දුන්නා දුන්න දුන්නේ දුන්නේය දුන් දුන්නත් දුන්නොත් දීලා දීල දීම "
             "දීමට දීමේ දීමෙන් දෙනු දෙන්නම් දෙනවද දෙනවාද දෙන්නට දුන්නාද දෙයිද දෙනව දුන්නෙ දීමක් දුනි දීම්",
    "ගන්නවා": "ගන්න ගන්නේ ගනී ගනියි ගනිති ගනිමු ගනිමින් ගත්තා ගත්ත ගත්තේ ගත්තේය ගත් ගත්තත් ගත්තොත් ගෙන ගෙනත් අරන් අරගෙන "
              "ගැනීම ගැනීමට ගැනීමේ ගැනීමෙන් ගනු ගන්නට ගන්නම් ගන්නවද ගන්නවාද ගත්තාද ගන්නා ගන්නව ගත්තෙ ගැනීමක් ගනිද්දී ගත්හ ගැනිණ",
    "කියනවා": "කියන කියන්න කියන්නේ කියයි කියති කියමු කියමින් කියද්දී කිව්වා කිව්ව කිව්වේ කිව්වත් කිව්වොත් කීවා කී කීවේය "
              "කියූ කියූයේ කියා කියලා කියල කීම කීමට කියනු කියන්නට කියනවද කියනවාද කිව්වාද කියාද කියනව කිව්වෙ කියන්නෙ කීවේ කිව කියාය "
              "කීහ කීවාය කියූහ",
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
    "ලබනවා": "ලද ලදී ලදුව ලැබුවා ලැබූ ලැබුවේය ලබා ලබන ලබන්නේ ලබයි ලබති ලැබීම ලැබීමට ලබමින් ලබන්න ලබන්නට ලබනු ලද්දේ ලදි ලද්දා ලැබ ලැබිය ලැබු ලත්",
    "දානවා": "දාන දාන්න දාන්නේ දායි දාමු දාලා දාල දාපු දැම්මා දැම්ම දැම්මේ දැම්මෙ දාන්නම් දානව දාන්නෙ දැම්මාම",
    "ලැබෙනවා": "ලැබෙන ලැබේ ලැබෙයි ලැබුණා ලැබුණ ලැබුණේ ලැබුණු ලැබී ලැබිණ ලැබිණි ලැබීමට ලැබෙන්නේ ලැබෙන්න",
    "පවසනවා": "පවසයි පවසති පැවසීය පැවසූ පැවසුවා පවසා පවසමින් පැවසීම පවසන පවසන්නේ පැවසුවේය පැවසේ පැවසූහ",
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
                "පැවැත්වේ පැවැත්විණ පැවැත්විණි පැවැත්වුණා පැවැත්වුණු පැවැත්වෙයි පවත්වමින් පැවැත්වුවා පැවැත්විය",
    "හඳුන්වනවා": "හඳුන්වන හඳුන්වා හැඳින්වූ හැඳින්වීම හැඳින්වේ හඳුන්වයි හඳුන්වන්නේ හැඳින්වෙන හඳුන්වමින් හඳුන්වාදීම "
                 "හැඳින්වෙන්නේ",
    "ලබාදෙනවා": "ලබාදී ලබාදීම ලබාදීමට ලබාදීමේ ලබාදෙන ලබාදෙයි ලබාදුන් ලබාදුන්නා ලබාදෙන්න ලබාදෙන්නේ ලබාදුන්නේ ලබාදෙමින්",
    "ලබාගන්නවා": "ලබාගත ලබාගත් ලබාගැනීම ලබාගැනීමට ලබාගැනීමේ ලබාගන්න ලබාගන්නා ලබාගෙන ලබාගත්තා ලබාගන්නට ලබාගනී",
    "කරගන්නවා": "කරගත කරගත් කරගත්තා කරගන්න කරගැනීම කරගැනීමට කරගන්නට කරගන්නා කරගනී කරගත්තේ කරගනිමින්",
    "වහිනවා": "වහින වහියි වහිද්දී වැස්සා වැස්සේ වැහැලා වැහැල වැහැපු වහින්න",
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
# And a verb's past and relative in -ා and -ේ with the written language's -ය (කීවා: කීවාය, ආවේ: ආවේය).
for _lemma, _text in _BY_HAND.items():
    for _form in _text.split():
        if _lemma.endswith("නවා") and _form.endswith(("ා", "ේ")) and sum(1 for c in _form if "ක" <= c <= "ෆ" or "අ" <= c <= "ඖ") >= 2:
            SINHALA_FORMS.setdefault(_form + "ය", _lemma)
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
    "පැහැදිලි": "adj", "නියමිත": "adj",
    # A noun the pack has only in its forms (වතාවට, වතාවක් "times").
    "වතාව": "noun",
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
    "කෝච්චිය": ("noun", "train"), "ඉර": ("noun", "sun"),
    "ඉස්පිරිතාලය": ("noun", "hospital"), "සතුට": ("noun", "happiness, joy"),
    # Common words of the corpus the pack lacks (its parallel dictionary pairs them with English it does not read).
    "බිත්තිය": ("noun", "wall"), "ලියුම": ("noun", "letter"), "ඩ්‍රෝන්": ("noun", "drone"), "කිසිවෙක්": ("pron", "anyone; (with a negative) no one"),
    "ඇතුළ": ("noun", "inside, interior"), "වහිනවා": ("verb", "to rain"), "බ්‍රිතාන්‍ය": ("adj", "British"),
    "ඉන්දියානු": ("adj", "Indian"), "ත්‍රස්තවාදය": ("noun", "terrorism"), "ඩෙංගු": ("noun", "dengue"),
    "වර්ෂය": ("noun", "year"), "ආස": ("adj", "fond of, liking (spoken)"), "කිලෝ": ("noun", "kilo"),
    "ද්‍රවිඩ": ("adj", "Dravidian"), "පෙරහර": ("noun", "procession, perahera"), "ඔලිම්පික්": ("adj", "Olympic"),
    "විහාරාධිපති": ("noun", "chief incumbent (of a temple)"), "බෙහෙවින්": ("adv", "greatly, largely"),
    "පුද": ("noun", "offering, worship"), "බෝධි": ("noun", "Bodhi (the sacred fig tree)"),
    "ක්‍රියාමාර්ගය": ("noun", "measure, course of action"), "එමගින්": ("adv", "thereby, by that"),
    "මෙමගින්": ("adv", "hereby, by this"), "මැති": ("noun", "member of parliament"), "හෙළ": ("adj", "Sinhalese (of old), Hela"),
    "උඩරට": ("noun", "the up-country, the Kandyan highlands"), "මෙකී": ("det", "this (aforesaid)"),
    "ඉමහත්": ("adj", "immense, great"), "පණ්ඩිත": ("noun", "scholar, pundit"), "අප්‍රිකානු": ("adj", "African"),
    "කොරියානු": ("adj", "Korean"), "යුරෝපීය": ("adj", "European"), "ජපන්": ("adj", "Japanese"),
    "ටෙලිනාට්‍ය": ("noun", "teledrama, TV serial"), "ත්‍රිරෝද": ("adj", "three-wheel"), "ග්‍රෑම්": ("noun", "gram"),
    "ජෛව": ("adj", "biological, bio-"), "මැතිතුමා": ("noun", "the honourable member"), "සිල්": ("noun", "the (Buddhist) precepts"),
    "මහලේකම්": ("noun", "general secretary"), "පාලි": ("noun", "Pali"), "පූජ්‍ය": ("adj", "venerable"),
    "හොත්": ("conj", "if; or"), "කතාබහ": ("noun", "chat, conversation"), "දර": ("noun", "firewood"),
    "දොස්තර": ("noun", "doctor (spoken)"), "සසර": ("noun", "samsara, the round of rebirth"), "අවනත": ("adj", "obedient, submissive"),
    "තැනැත්තා": ("noun", "person, the one who"), "ශ්‍රේෂ්ඨ": ("adj", "great, eminent"), "හින්දා": ("postp", "because of (spoken)"),
    "බුදුදහම": ("noun", "Buddhism"), "තුමා": ("noun", "he, the gentleman (honorific)"),
    "ආයුර්වේද": ("noun", "Ayurveda"), "එකිනෙක": ("pron", "each other, one another"), "ආර්ය": ("adj", "noble; Aryan"),
    "සමරු": ("adj", "commemorative"), "එදිනෙදා": ("adj", "daily, everyday"), "නොහොත්": ("conj", "or, otherwise"),
    "කිසිසේත්": ("adv", "at all, in any way"), "කොපමණ": ("adv", "how much, how many"), "සංඝනායක": ("noun", "chief prelate"),
    "මහානායක": ("noun", "chief prelate (mahanayaka)"), "බඳු": ("postp", "like, such as"), "හින්දි": ("noun", "Hindi"),
    "නැඟුම": ("noun", "rise, uplift"), "රිමාන්ඩ්": ("noun", "remand"), "ක්‍රිස්තියානි": ("adj", "Christian"),
    "සෑහෙන": ("adj", "considerable, sufficient"), "අනුශාසනාව": ("noun", "advice, admonition"), "යනාදී": ("det", "and so on, etc."),
    "බුලත්": ("noun", "betel"), "තෙරුන්": ("noun", "monks, theras"), "සම්බුද්ධ": ("adj", "fully enlightened"),
    "අවතැන්": ("adj", "displaced"), "අධ්‍යාපනික": ("adj", "educational"), "මහඟු": ("adj", "precious"),
    "ඥාතියා": ("noun", "relative, kinsman"), "නත්තල්": ("noun", "Christmas"), "නෙට්බෝල්": ("noun", "netball"),
    "ඉස්ලාම්": ("noun", "Islam"), "කඨින": ("noun", "Kathina (the robe offering)"), "ඊනියා": ("adj", "so-called"),
    "නාශක": ("noun", "pesticide, -cide"), "ජාතක": ("noun", "Jataka (a birth story of the Buddha)"), "පව": ("noun", "sin; pity"),
    "මත්කුඩු": ("noun", "drugs, narcotics"), "ඉෂ්ට": ("adj", "fulfilled"), "අනුස්මරණ": ("adj", "commemorative, memorial"),
    "විවිධාකාර": ("adj", "various, of many kinds"), "අඟහරුවාදා": ("noun", "Tuesday"), "මැතිනිය": ("noun", "madam, Mrs. (honorific)"),
    "යටි": ("adj", "lower, under-"), "තරගාවලිය": ("noun", "tournament"), "ශාසනික": ("adj", "of the Buddhist order"),
    "මදි": ("adj", "not enough"), "එහෙනම්": ("conj", "then, in that case"), "ඔහොම": ("adv", "like that (spoken)"),
    "ජනිත": ("adj", "born, generated"), "මෙතැන": ("adv", "here, this place"), "සංඝයා": ("noun", "the Sangha, the monks"),
    "චෛත්‍යය": ("noun", "stupa, chaitya"), "අතිවිශාල": ("adj", "huge, enormous"), "වෙස්": ("noun", "disguise"),
    "මුල්‍ය": ("adj", "financial"), "ඉතුරු": ("adj", "remaining, left over"), "කොයිතරම්": ("adv", "how much"),
    "අතිගරු": ("adj", "His Excellency (honorific)"), "සංස්කෘත": ("noun", "Sanskrit"), "බැඩ්මින්ටන්": ("noun", "badminton"),
    "බස්රථය": ("noun", "bus"), "සාමාන්‍යාධිකාරී": ("noun", "general manager"), "පින්කම": ("noun", "meritorious deed"),
    "අනුන්": ("pron", "others, other people"), "අන්දම": ("noun", "manner, way"),
    "ගණය": ("noun", "category, group"), "නිදසුන": ("noun", "example"), "රහත්": ("noun", "arahant (an enlightened one)"),
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
             "ලිපි": "ලිපිය", "නීති": "නීතිය", "පරීක්ෂණ": "පරීක්ෂණය", "හොඳයි": "හොඳ", "ආච්චී": "ආච්චි",
             "ආච්චීගේ": "ආච්චි", "කවද": "කවදා", "කව්ද": "කවුද", "වාගේ": "වගේ", "ඕනවට": "ඕනෑ", "ඕනෑවට": "ඕනෑ",
             "හැක්කේ": "හැකි", "විසිනි": "විසින්", "වශයෙනි": "වශයෙන්", "සඳහාය": "සඳහා", "පසුවය": "පසුව", "වගෙ": "වගේ",
             "ඕනෙ": "ඕනෑ", "ඕනැ": "ඕනෑ", "සියළු": "සියලු", "සියලූ": "සියලු", "සියළුම": "සියලු", "පිලිබඳ": "පිළිබඳ", "කිසිඳු": "කිසිදු",
             "මුලික": "මූලික", "ඇතුලු": "ඇතුළු", "ආදි": "ආදී", "පරන": "පරණ", "කටහඩ": "කටහඬ", "පුලුවන්": "පුළුවන්",
             "පුළුවන": "පුළුවන්", "අම්මගෙ": "අම්මා", "මෙමඟින්": "මෙමගින්", "එමඟින්": "එමගින්", "යනාදිය": "යනාදී",
             "ඉගන": "ඉගෙන", "සම්පුර්ණ": "සම්පූර්ණ", "අළුත්": "අලුත්", "කාගෙ": "කවුද", "පවුලෙ": "පවුල", "විද්යුත්": "විද්‍යුත්"}
SINHALA_FORMS.update(SPELLINGS)
# Corpus spellings that are no words: letters and endings written apart.
NOT_WORDS = {"ක්", "ට", "ගේ", "ය", "ව", "ක", "ද", "ම", "යි", "ත්", "වල", "වලට", "වලින්", "වලදී", "වලත්", "ගෙන්",
             # English letters spelled out (D, T, K, C) and people's names the parallel dictionary pairs with a word.
             "ඩී", "ටී", "කේ", "සී", "බී", "පී", "ජේ", "එස්", "එල්", "ආර්", "එච්", "ඩබ්ලිව්", "ජී", "එම්", "සිල්වා", "තිස්ස",
             "රනිල්", "වික්‍රමසිංහ", "පෙරේරා", "රාජපක්ෂ", "මහින්ද", "සරත්", "කුමාරය",
             # Pieces of words: ටෙලි of ටෙලි නාට්‍ය, යුත් of යුත්තේ, and a typo.
             "ටෙලි", "යුත්", "ප්‍රමාණයකය",
             # Endings written off a number (5කදී, 10ක්ද).
             "කදී", "ක්ද",
             # A parallel dictionary word that is සටන's indefinite with -ය.
             "සටනකය",
             # කාර of කාර්යය and -කාර compounds, the letter N, and -වරයා written apart.
             "කාර", "එන්", "වරයා"}
# People's and places' names the parallel dictionary pairs with an English name, as names are no words of the list.
NOT_WORDS |= set("""
පීටර් ලයනල් ජෝ වික්ටර් ජෝශප් ජෝසප් ජෝෂප් සාලිය ටෝනි පෝල් නීල් පිලිප් පීතර එරික් පර්සි බේත් රෝයි ඇලෙක්සැන්ඩර් බර්නාඩ්
හෙක්ටර් ස්ටීවන් මලික් බ්ලේක් ඩැනියෙල් ඩැනියල් කොස්තා මෙල් වින්සන්ට් රොන් කේට් ඇඩම්ස් ශීලා ෂාරුක් බ්‍රයන් වෝනර් පාම ස්ටීව්
රොඩ්නි ඇඩම් ආදම් ඇනා ආනා අන්නා ජේන් කෙලී බ්‍රැඩ් බෙනඩික්ට් ඇන්ඩි හෝම්ස් බෙන්ජමින් ජොනී ඩාවින් රෙජී තෙරේසා කේන් ග්‍රැහැම්
රෙක්ස් සිද් හෙලන් ලෝරා නොයෙල් ක්ලින්ට් බැරැක් ඩේවි අයිරින් ඩේව් නික් ඕමාර් එමා ජිමී පටේල් ජෙෆ් ටෙඩී බේලි ෆ්‍රෑන්ක්ලින්
කාරා මාර්ටින් ලෙස්ලි ජේම්ස් මයිකල් තෝමස් ඩේවිඩ් ජෝර්ජ් විලියම් චාල්ස් ජැක්සන් එඩ්වඩ් හිට්ලර් ගාන්ධි ඩයස් සිරිල් මර්වින්
ජිනීවා ජිනීවාහි නිව්යෝර්ක් නිව්යෝක් යෝර්ක් හොංකොං සිම්බාබ්වේ වොෂින්ටනය ඕලන්ද සුඩානය ටිබෙටය ජෝර්දානය රියෝ මැන්චෙස්ටර්
ඝානාව මොරොක්කෝව ජැමෙයිකාව ජැමෙයිකාවේ ජැමෙයිකා බොලිවියාව අයිස්ලන්තය ලන්ඩන් ලන්ඩනයේ ඩලස් මුම්බායි චෙන්නායි
කන්තලේ නිල්මිණි මඩකළපු මූන් රාජ් ශිව පරණවිතාන බුෂ් කෙහෙළිය බස්නායක
""".split())
# A verbal noun's endings, which take a noun's cases (හැඟීම: හැඟීමක්, රැස්වීම: රැස්වීමේදී).
VERBAL_NOUN_ENDINGS = ("ීම", "ෑම", "ීම්", "ුම")
# Endings only a verb's forms have (a verb's -ා, -ල, -න, -මු forms are often nouns: පුරා, අසල, ඔතන, පළමු).
VERBAL_TAILS = ("මින්", "න්න", "න්නේ", "න්නට", "ද්දී", "නකොට", "ූ", "ුවා", "ුවේ", "ුවේය", "ීම", "ීමට", "ීමේ", "ීමෙන්", "ුණා",
                "ුණු", "ුණේ", "ිලා")
# Literary verbs the pack lacks, and the spoken verb they are (ගසනවා: ගහනවා).
LITERARY_VERBS = {"ගසනවා": "ගහනවා", "අසනවා": "අහනවා", "සාදනවා": "හදනවා"}
# One word's spellings and forms the rules keep apart, by hand: spelling or form -> the word.
FOLDS = {
    # A noun's definite, plural, indefinite or case form, an adjective in -ු beside its noun, glossed apart from it.
    "සාමාන්‍යය": "සාමාන්‍ය", "ආරම්භය": "ආරම්භ", "උත්සාහය": "උත්සාහ", "අරමුණු": "අරමුණ", "ටිකක්": "ටික", "දකුණ": "දකුණු",
    "සතුටු": "සතුට", "දස": "දහය", "වහන්සේලා": "වහන්සේ", "නිසයි": "නිසා", "මුහුද": "මුහුදු", "සති": "සතිය",
    "වැරදි": "වැරැද්ද", "වැරැදි": "වැරැද්ද", "කටයුත්ත": "කටයුතු", "බුදුන්": "බුදු", "අයකු": "අය", "නියමිතය": "නියමිත",
    # Verb forms: the literary past, the participle before හැකි and යුතු, the absolutive.
    "යෙදී": "යෙදෙනවා", "සැලකිය": "සලකනවා", "ලැබීය": "ලබනවා", "රැක": "රකිනවා", "පැවැසීය": "පවසනවා", "තියා": "තබනවා",
    "දැකිය": "දකිනවා", "නොවී": "වෙනවා", "වනුයේ": "වෙනවා", "තිබු": "තිබෙනවා", "මියගොස්": "මිය",
    # Spellings and spoken forms.
    "බිරිඳ": "බිරිය", "කාරණාව": "කාරණය", "රහ": "රස", "බෑ": "බැහැ", "කාලෙ": "කාලය", "නෙමෙයි": "නෙවෙයි",
    "නගනවා": "නඟනවා", "ජෙනරාල්": "ජනරාල්", "නිමා": "නිම", "නැතුව": "නැති", "නැතැයි": "නැත", "බැව්": "බව",
    "කොහොමද": "කොහොම", "මාධ්‍යය": "මාධ්‍ය", "රාත්‍රිය": "රාත්‍රී", "භාරය": "භාර", "සම්බන්ධය": "සම්බන්ධ", "කඳු": "කන්ද",
    "සමහරු": "සමහර", "අධිකාරි": "අධිකාරිය", "රජතුමා": "රජ", "තාත්ත": "තාත්තා", "අයිය": "අයියා", "තරහා": "තරහ",
    "හඩ": "හඬ", "මමම": "මම", "අයෙක්": "අය", "ගෙවල්": "ගෙදර", "දේශනය": "දේශනාව", "ගායනය": "ගායනා", "සංචාරකයා": "සංචාරක",
    "මහජනතාව": "මහජන", "දැනටමත්": "දැනට",
    # Spellings the pack glosses apart (ධූරය "office", ධුරය "position"), and spoken ones (අනික, අනිත් of අනෙක).
    "ධූරය": "ධුරය", "අනික": "අනෙක", "අනික්": "අනෙක", "අනිත්": "අනෙක", "නැමැති": "නමැති", "ස්වාභාවික": "ස්වභාවික",
    # Forms the rules leave apart: a compound verb's literary absolutive, a clitic, an indefinite.
    "ලක්ව": "ලක්වූ", "ලස්සනයි": "ලස්සන", "බලපෑමක්": "බලපෑම්", "අවදානමක්": "අවදානම", "පුදුමයක්": "පුදුම",
    "අනිවාර්ය": "අනිවාර්යය", "පණිවිඩය": "පණිවුඩය", "සුවිශේෂ": "සුවිශේෂී", "පේනවා": "පෙනෙනවා", "මුණ": "මුහුණ",
    "මූණ": "මුහුණ", "හඳුනාගත": "හඳුනා", "ඉතිං": "ඉතින්", "වැඩිහිටි": "වැඩිහිටියන්", "තාම": "තව", "විකිණීම": "විකුණනවා",
    "හෙළිවී": "හෙළි", "සඳහන්ය": "සඳහන්", "පාර්ශවය": "පාර්ශ්වය", "අයිති": "අයිතිය", "පහරදී": "පහර", "කැලය": "කැලෑව",
    "අංගය": "අංග", "පිරිසුදු": "පිරිසිදු", "වෙනස්වීම්": "වෙනස", "ඉගෙනුම": "ඉගෙන", "පළමුවැනි": "පළමු",
    # Spoken forms and spellings the writers met as words of their own (නං of නම්, මවු of මව, පාසැල of පාසල).
    "නං": "නම්", "මවු": "මව", "දවල්": "දහවල්", "දවල": "දහවල්", "පාසැල": "පාසල", "සිනහ": "සිනා", "ගෙට": "ගෙය",
    "නිවෙස": "නිවස", "හුඟාක්": "හුඟ", "ගොඩාක්": "ගොඩ", "රගර්": "රග්බි", "ලාංකේය": "ලාංකික", "ලාංකීය": "ලාංකික",
    "ඇමෙරිකානු": "ඇමරිකානු", "අමෙරිකානු": "ඇමරිකානු", "ඇමරිකන්": "ඇමරිකානු", "යුරෝපා": "යුරෝපීය", "ළමය": "ළමයා",
    "දිනෙක": "දිනය", "අයෙකු": "අය", "කිසිවෙකු": "කිසිවෙක්", "කිසිවකු": "කිසිවෙක්", "ශක්ති": "ශක්තිය", "මීටර": "මීටර්",
    "යුධ": "යුද", "තොරතුර": "තොරතුරු", "පාරවල්": "පාර", "ජුලි": "ජූලි", "ප්‍රධානතම": "ප්‍රධාන", "විශිෂ්ටතම": "විශිෂ්ට",
    "ඇතැමුන්": "ඇතැම්", "දමිළ": "දෙමළ", "ආසියාතික": "ආසියානු", "නෙමේ": "නෙවෙයි", "සම්පත": "සම්පත්", "දරුවො": "දරුවා",
    "නිසාවෙන්": "නිසා", "පුරාවට": "පුරා", "ගෙනා": "ගෙනෙනවා", "ගෙනැවිත්": "ගෙනෙනවා", "ඇතුළත්ව": "ඇතුළත්",
    "බැවිනි": "බැවින්", "ඥාති": "ඥාතියා", "ඥාතීන්": "ඥාතියා", "කෘති": "කෘතිය", "ආශා": "ආශාව",
    "රහතන්": "රහත්", "තුර": "තුරු", "මණ්ඩලයීය": "මණ්ඩලීය", "වැහි": "වැසි", "අත්‍යාවශ්‍ය": "අත්‍යවශ්‍ය", "බත": "බත්",
    "වෙළඳාම": "වෙළෙඳාම", "මන": "මනස", "බලවතුන්": "බලවත්", "දෙමවුපියන්": "දෙමව්පියන්", "පිවිස": "පිවිසි",
    "චිත්‍රපටිය": "චිත්‍රපටය", "රැස්ව": "රැස්", "රෑට": "රෑ", "හඳුනාගැනීම්": "හඳුනා", "උයන්": "උයන", "රැජන": "රැජින",
    "හමාරක": "හමාරය", "අත්අඩංගුවට": "අත්අඩංගුව", "සීමාවන": "සීමාව", "තොටුපොළ": "තොටුපළ", "පිං": "පින",
    "ලියවුණු": "ලියැවුණු", "දෙනකුගෙන්": "දෙනා", "යටත්ව": "යටත්", "යුතුමය": "යුතු", "බේර": "බේරා", "ඇතු": "ඇතා",
    "ඉරියව්ව": "ඉරියව්", "අකැමැති": "අකමැති", "ගුරුවරියක්": "ගුරුවරිය", "ප්‍රමුඛතාවය": "ප්‍රමුඛත්වය",
    "රැස්වීමක": "රැස්වෙනවා", "ද්වේෂය": "ද්වේශය", "එකඟත්වය": "එකඟතාවය", "හැඳින්වීම්": "හඳුන්වනවා", "වැඩිමල්": "වැඩිමහල්",
    "සූදානම්ව": "සූදානම", "කොමිසන්": "කොමිෂන්", "නරඹන්නන්": "නරඹනවා", "මිහිර": "මිහිරි",
    "ඉපදුන": "ඉපදෙන", "නිදසුන්": "නිදසුන", "නිදසුනක්": "නිදසුන", "පිළිගැනේ": "පිළිගන්නවා", "ඉදිරිපත්වී": "ඉදිරිපත්",
    "පාක්ෂිකයන්": "පාක්ෂික", "නිකං": "නිකම්", "කුරුලු": "කුරුල්ලා", "දෙව්": "දෙවියා", "හිමිවරුන්": "හිමියා", "ගාණක්": "ගණන",
    "හුදකලාව": "හුදෙකලා", "තණ්හාය": "තණ්හාව", "ගණයට": "ගණය", "දැමිය": "දමනවා", "ඉංගිරිසි": "ඉංග්‍රීසි",
    "හාමුදුරුවො": "හාමුදුරුවෝ", "මවුවරුන්": "මව", "පමා": "ප්‍රමාද", "ගං": "ගඟ", "පැත්තෙ": "පැත්ත", "අවියක්": "අවි",
    "දෙපාර්ශවය": "දෙපාර්ශ්වය", "පරදා": "පරදවා",
}
# Words a rule takes for another word's form that are words of their own, glossed apart: form -> the words it is no form of
# (පතල "mine" of පතනවා, කොටුව "box" of කොටනවා, හරක් "cattle" of හරය, වැඩේ "the work" of වැඩෙනවා).
NOT_FORMS = {"පතල": {"පතනවා"}, "කොටුව": {"කොටනවා"}, "හරක්": {"හරය", "හර"}, "ගවුම": {"ගවයා"}, "මාරු": {"මාරයා"},
             "තල්": {"තලය", "තල"}, "බසින්": {"බසින"}, "ළඟින්": {"ළඟා"}, "වැඩේ": {"වැඩෙනවා"},
             "රහත්": {"රස", "රහ"}, "යටත්": {"යට"}, "දහවල්": {"දහ", "දහය"}}
# A case's endings, which the written language's -ය may follow (හමුවකදීය, ඇත්තේය).
CASE_ENDINGS = ("දී", "ේ", "ින්", "ෙන්")
# A word another makes only loosely is a word of its own when its case forms are this common beside it.
CASED_SHARE = 0.3
# The endings that tell such a word: a noun's cases and plural, which no clitic, verb form or bare stem takes.
CASE_TEST_TAILS = ["ක්", "කට", "කින්", "කදී", "ට", "ේ", "ේදී", "ෙන්", "යක්", "යට", "යේ", "යෙන්", "වල", "වලට", "වලින්", "්"]
# A verb's spoken forms that are often a noun of their own (මහල "floor" of මහනවා).
SPOKEN_VERB_TAILS = ("ල", "ලා", "පු")
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


def action_glossed(dictionaries):
    """The pack's words glossed as an action: an English -ing form or "to ..." (කරවීම "making"; not බලපෑම "impact")."""
    out = set()
    db = sqlite3.connect(os.path.join(dictionaries, "si-en.sqlite"))
    for word, senses in db.execute("SELECT word_lc, senses FROM entries"):
        for sense in json.loads(senses):
            for gloss in sense.get("glosses", []):
                if gloss.lower().startswith("to ") or any(t.endswith("ing") for t in re.findall("[a-z]+", gloss.lower())):
                    out.add(word)
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
                forms |= {stem[:-2] + t for t in IYA_PLURAL_TAILS + IYA_SHORT_PLURAL_TAILS}
    elif last in CONSONANTS:
        forms = {word + t for t in A_TAILS}
        # (A stem in a conjunct ්ය or ්ව takes the -ය too: විශ්ව: විශ්වය.)
        conjunct = word[-2:-1] == AL_LAKUNA or word[-3:-1] == AL_LAKUNA + ZWJ
        if (not word.endswith(("ය", "ව")) or conjunct) and word + "ය" not in known:
            forms |= {word + t for t in YA_TAILS}
    elif last == AL_LAKUNA:
        forms = {word + t for t in CONSONANT_TAILS}
    else:
        forms = {word + t for t in VOWEL_TAILS}
    if word.endswith("ව"):
        forms |= {word + t for t in VA_TAILS}
    # A learned noun in -ය has a plural in -යන් too (තත්ත්වය: තත්ත්වයන්).
    if word.endswith("ය") and letters(word) >= 3:
        forms |= {word + t for t in ["න්", "න්ට", "න්හි", "න්ගේ"]}
    # A noun in -ුව has its plural in -ූන් (භික්ෂුව: භික්ෂූන්, භික්ෂූන්ට).
    if word.endswith("ුව") and letters(word) >= 3:
        forms |= {word[:-2] + "ූ" + t for t in N_PLURAL_TAILS + ["හු"]}
    # An inanimate noun in -ිය has its plural in -ීන් and -ිවල (ජාතිය: ජාතීන්, ජාතිවල); its bare -ි is plural_stems'.
    if word.endswith("ිය") and letters(word) >= 3:
        forms |= {word[:-2] + t for t in IYA_PLURAL_TAILS} | {word[:-1] + t for t in ["වල", "වලට", "වලින්"]}
    # An animate noun in -ු takes -වන් in the plural (සැකකරු: සැකකරුවන්), and a noun in -ු its definite -ුව's cases (පෙරවරු:
    # පෙරවරුවේ).
    if word.endswith("ු"):
        forms |= {word + "ව" + t for t in N_PLURAL_TAILS + ["ේ", "ේදී", "ට", "ෙන්", "ක්", "කට", "කින්"]}
    return with_clitics(forms | {word}) - {word}


def full_stem_forms(word):
    """
    A noun in -න්, -ස් or -ල්'s cases on the full a (ඉක්මන්: ඉක්මනට, ඉක්මනින්; වෙනස්: වෙනසට), and a noun's spoken plural in -වල්
    (දොර: දොරවල්), with their clitics.
    """
    stem = word[:-1]
    if word[-1:] in CONSONANTS and letters(word) >= 2:
        return with_clitics({word + "වල්"})
    if not word.endswith(AL_LAKUNA) or stem[-1:] not in CONSONANTS or letters(stem) < 2:
        return set()
    return with_clitics({stem + t for t in ["ට", "ින්", "ේ", "ෙන්", "ේදී"]})


def literary_pasts(word):
    """A verb's written past in -ිය, -ීය on its fronted a-stem (ගෙවනවා: ගෙවිය; තබනවා: තැබිය), with the clitics."""
    if not word.endswith("නවා") or " " in word:
        return set()
    stem = word[:-3]
    if not stem or stem[-1] not in CONSONANTS or letters(stem) < 2:
        return set()
    return with_clitics({front(stem) + t for t in LITERARY_PAST_TAILS})


def stem_of(word):
    """The stem a noun in -ය or -ව has as its plural (ප්‍රශ්න of ප්‍රශ්නය, කතා of කතාව), or None."""
    # (An adjective's adverb in -ව keeps its adjective: සරලව is සරල's.)
    before = word[-2:-1]
    if word.endswith("ය") and word[:-1].endswith(CASE_ENDINGS):
        return None
    if letters(word) >= 3 and (word[-1] == "ය" and (before in CONSONANTS or before in VOWEL_SIGNS - {"ි"})
                               or word[-1] == "ව" and before in VOWEL_SIGNS - {"ි"}):
        return word[:-1]
    return None


def front(stem, rounded=True):
    """
    The stem with its first vowel fronted, as a past takes it (බල: බැල, අහ: ඇහ, කාප: කෑප; පුද: පිද, උසුල: ඉසිල, කොට: කෙට, තෝර:
    තේර), a u fronting the u's after it too; only a and ā when not [rounded].
    """
    first = stem[0]
    if first == "අ":
        return "ඇ" + stem[1:]
    if first == "ආ":
        return "ඈ" + stem[1:]
    if first == "උ" and rounded:
        return "ඉ" + stem[1:].replace("ු", "ි")
    if first in ("ඔ", "ඕ") and rounded:
        return {"ඔ": "එ", "ඕ": "ඒ"}[first] + stem[1:]
    if first in CONSONANTS:
        nxt = stem[1:2]
        if nxt == "ා":
            return first + "ෑ" + stem[2:]
        if nxt == "ු" and rounded:
            return first + "ි" + stem[2:].replace("ු", "ි")
        if nxt in ("ො", "ෝ") and rounded:
            return first + {"ො": "ෙ", "ෝ": "ේ"}[nxt] + stem[2:]
        if nxt and nxt not in VOWEL_SIGNS and nxt != AL_LAKUNA and nxt != ZWJ:
            return first + "ැ" + stem[1:]
    return stem


# Verbs whose forms a compound made with them takes (සිදුවෙනවා: සිදුවූ, සිදුවීම; ලබාගන්නවා: ලබාගත්).
COMPOUNDED = ("වෙනවා", "කරනවා", "ගන්නවා", "දෙනවා")


# A compound verb's forms written as one word with a word before them (පත්කළා, ඉදිකිරීම, හඳුනාගෙන, පහරදුන්): -වන, -වේ, -විය,
# -වෙන are rather an ordinal, a locative or a noun's (පළමුවන, පොදුවේ, අඩවිය, පිරිවෙන), -වනවා a causative's (කරවනවා).
JOINED_FORMS = {base: [f for f in [base] + _BY_HAND[base].split() if f not in ("වන", "වේ", "විය", "වෙන", "වනු", "වේද", "වනවා")]
                for base in COMPOUNDED}
# Words spelled as a word and a compound verb's form that are words of their own.
NOT_JOINED = {"හිතකර", "හානිකර", "අහිතකර", "තනිකර", "කරුණාකර", "වැදගත්", "ඇඟවීම", "දැන්වීම"}


def joined_parts(word):
    """[word] as (the word before, the compound verb's form), the longest form first, or None."""
    if word in NOT_JOINED:
        return None
    for base, forms in JOINED_FORMS.items():
        for form in sorted(forms, key=len, reverse=True):
            if word.endswith(form) and letters(word[:-len(form)]) >= 2:
                return word[:-len(form)], base
    return None


def verb_forms(word):
    """
    A verb's forms by rule from its -නවා form: the present's, and the past's of an a-stem or e-stem; a compound's are
    its last verb's (සිදුවෙනවා as වෙනවා; not යෙදෙනවා, an e-stem, as දෙනවා).
    """
    if not word.endswith("නවා") or " " in word:
        return set()
    for base in COMPOUNDED:
        if word.endswith(base) and letters(word[:-len(base)]) >= 2 and word not in _BY_HAND:
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
        # A causative's past relative on its stem as it is (කරවනවා: කරවූ).
        if last == "ව" and letters(stem) >= 3:
            forms.add(stem + "ූ")
    elif last == "ා" and letters(stem) >= 2:
        # An ā-stem's absolutive and past (බලපානවා: බලපාලා, බලපාපු, බලපෑ, බලපෑවා).
        forms |= {stem + t for t in A_PRESENT_TAILS} | {stem[:-1] + "ෑ" + t for t in AA_PAST_TAILS}
    elif last == "ි":
        forms.add(stem[:-1] + "ී")
        # An i-stem's past and verbal noun on its fronted root (රකිනවා: රැක්කා, රැකීම); its absolutive (රැක) is often a word
        # (නැවත "again", මැද "middle").
        root = front(stem[:-1])
        if letters(root) >= 2 and root[-1] in CONSONANTS:
            forms |= {root + AL_LAKUNA + root[-1] + "ා", root + AL_LAKUNA + root[-1] + "ේ", root + "ීම", root + "ීමට", root + "ීමේ"}
    return with_clitics(forms) - {word} - verbal_nouns(word)


AA_PAST_TAILS = ["", "වා", "ව", "වේ", "වෙ", "වාම", "වොත්"]
NOUN_TAILS = ["ම", "මට", "මේ", "මෙන්", "මක්", "ම්"]


def verbal_nouns(word):
    """
    The verbal nouns of an ā-stem (බලපානවා: බලපෑම, බලපෑමට; යාම) and of a causative on its stem as it is (කරවනවා: කරවීම), which
    the pack often glosses as a noun of their own (බලපෑම "impact"): analyse takes them for the verb's only when glossed as the
    action.
    """
    if not word.endswith("නවා") or " " in word:
        return set()
    stem = word[:-3]
    if stem.endswith("ා") and letters(stem) >= 2:
        forms = {stem[:-1] + "ෑ" + t for t in NOUN_TAILS} | {stem + t for t in NOUN_TAILS}
    elif stem[-1:] == "ව" and letters(stem) >= 3:
        forms = {stem + "ී" + t for t in NOUN_TAILS}
    else:
        return set()
    return with_clitics(forms)


def absolutives(word):
    """A verb's absolutive on its fronted stem (ලබනවා: ලැබ; බසිනවා: බැස), as often a word of its own (නැවත, මැද)."""
    if not word.endswith("නවා") or " " in word:
        return set()
    stem = word[:-3]
    if stem.endswith("ි"):
        stem = stem[:-1]
    if not stem or stem[-1] not in CONSONANTS or letters(stem) < 2:
        return set()
    fronted = front(stem, rounded=False)
    return {fronted} if fronted != stem else set()


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
    # (A name the pack has as a noun is a name: රනිල්.)
    for word in sorted(NOT_WORDS & set(own)):
        del own[word]
    return own, weak - set(own) - set(NOT_WORDS), names - set(own)


def word_forms(word, kinds, known=frozenset()):
    forms = with_clitics({word}) - {word}
    if kinds & NOMINAL_KINDS and letters(word) >= 2:
        forms |= nominal_forms(word, known)
        stem = stem_of(word)
        if stem:
            # And its stem's forms (දිනය: දින, දිනක, දිනට, දිනවල).
            forms |= {stem} | nominal_forms(stem, known) | with_clitics({stem + t for t in ["වල", "වලට", "වලින්", "වලදී"]})
    if "verb" in kinds or word.endswith("නවා"):
        forms |= verb_forms(word)
    return forms


PLURAL_TAILS = ["", "වල", "වලට", "වලින්"]


def plural_stems(word):
    """
    A noun's plurals and adjectives that may be words of their own: ජාති of ජාතිය; on the single consonant of a doubled one,
    -ු or -ි (කටයුතු of කටයුත්ත, ගොඩනැගිලි of ගොඩනැගිල්ල, වැසි of වැස්ස); and -ු on a final consonant (තනතුරු of තනතුර,
    සතුටු of සතුට).
    """
    out = set()
    if word[-1] in CONSONANTS and letters(word) >= 3:
        out.add(word + "ු")
    if word.endswith("ිය") and letters(word) >= 3:
        out.add(word[:-1])
    # A noun in -ය has its plural in -ා too (යානය: යානා, රචනය: රචනා).
    if word.endswith("ය") and letters(word) >= 3 and word[-2] in CONSONANTS:
        out.add(word[:-1] + "ා")
    if len(word) > 3 and word[-1] in CONSONANTS and word[-2] == AL_LAKUNA and word[-3] == word[-1] and letters(word) >= 3:
        out |= {word[:-2] + "ු", word[:-2] + "ි"}
    return out


# Honorific endings of a person's noun (අධ්‍යක්ෂවරයා of අධ්‍යක්ෂ, රජතුමා of රජ).
PERSON_ENDINGS = ("වරයා", "තුමා")


def animate_stems(word):
    """
    An animate noun in -යා or -ුවා's bare stems (ගොවි and ගොවිය of ගොවියා, නිලධාරී of නිලධාරියා, සිසු of සිසුවා), and the noun before an honorific ending
    (සභාපති of සභාපතිවරයා). Not a stem before -ා alone, as a verb's absolutive ends so too (වෙහෙසා).
    """
    if not word.endswith("ා") or not word[-2:-1] in CONSONANTS or letters(word) < 3:
        return set()
    stem = word[:-1]
    out = set()
    if stem.endswith("ය") and letters(stem) >= 3 and stem[-2] != AL_LAKUNA:
        out |= {stem, stem[:-1]}
        if stem.endswith("ිය"):
            out.add(stem[:-2] + "ී")
    for ending in PERSON_ENDINGS:
        if word.endswith(ending) and letters(word[:-len(ending)]) >= 2:
            out.add(word[:-len(ending)])
    # A noun in -ුවා has its stem in -ු (සිසු of සිසුවා, නළු of නළුවා).
    if word.endswith("ුවා") and letters(word) >= 3:
        out.add(word[:-2])
    return out


# Letters written either way in one word: the second the spelling key has.
KEY_LETTERS = str.maketrans({"ඟ": "ග", "ඬ": "ඩ", "ඳ": "ද", "ඹ": "බ", "ළ": "ල", "ණ": "න", "ෑ": "ැ", "භ": "බ", "ථ": "ත"})


def spelling_key(word):
    """
    What [word]'s spellings share: ක්‍ෂ as ක්ෂ, ත්ත්ව as ත්ව, ාංශ as ංශ, ං as න්, ඟ as ග, ළ as ල, ණ as න, ථ as ත, ී ූ as ි ු, an
    ordinal's -වැනි and -වෙනි as -වන, and ැ before a consonant and ැ once.
    """
    key = word.replace("ක්" + ZWJ + "ෂ", "ක්ෂ").replace("ත්ත්ව", "ත්ව").replace("ාංශ", "ංශ").replace("ං", "න්").translate(KEY_LETTERS)
    key = re.sub("([ැඇ])([ක-ෆ])ැ", "\\1\\2", key)
    # An ordinal's -වැනි, -වෙනි and -වන (තුන්වැනි, තුන්වන).
    key = re.sub("(වැනි|වෙනි)$", "වන", key)
    # (Not -ීය, an adjective's, beside a noun's -ිය: වෘත්තීය "professional", වෘත්තිය "profession".)
    ending = "ීය" if key.endswith("ීය") else ""
    return key[:len(key) - len(ending)].replace("ී", "ි").replace("ූ", "ු") + ending


def same_letters(a, b):
    """Whether [a] and [b] are one spelling but for the joiner (රක්‍ෂිත, රක්ෂිත), which the pack may gloss apart."""
    return a != b and a.replace(ZWJ, "") == b.replace(ZWJ, "")


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
    scores, forms = analyse(blended(found), dictionaries)
    return _rank(scores, forms, size, label)


def analyse(share, dictionaries):
    """Each word's score and its forms' shares: (word -> score, word -> form -> share)."""
    own, weak, names = lexicon(dictionaries)
    glosses = gloss_lemmas(dictionaries, os.path.dirname(dictionaries))
    actions = action_glossed(dictionaries)

    def noun_glossed(form):
        """Whether the pack glosses a verbal noun as a noun of its own, not as the action (බලපෑම "impact")."""
        return bool(glosses.get(form)) and form not in actions and not form.endswith(("මට", "මේ"))

    def unrelated(word, maker):
        """
        Whether [word] is no form of the short noun [maker] it looks like a form of: it is far commoner than the noun, and the
        pack glosses the two with no English word in common (නායක "leader" of නාය "landslide", කෙසේ "how" of කෙස "hair").
        A longer noun's forms are glossed apart often enough (කොටසක් "part", කොටස "section").
        """
        return (letters(maker) <= 2 and not maker.endswith("නවා")
                and share.get(word, 0.0) > UNRELATED_SHARE * share.get(maker, 0.0) and bool(glosses.get(word)) and bool(glosses.get(maker))
                and not glosses[word] & glosses[maker] and word not in verbal)

    def alike(a, b):
        """Whether the pack glosses [a] and [b] with an English word in common, or one the start of another (build, building)."""
        first, second = glosses.get(a, set()), glosses.get(b, set())
        return bool(first & second) or any(len(x) >= 5 and len(y) > len(x) and y.startswith(x) or len(y) >= 5 and len(x) > len(y)
                                           and x.startswith(y) for x in first for y in second)

    kinds = {w: ks for w, ks in own.items()}
    # Every word's forms: form -> the words that make it.
    made = defaultdict(set)
    verbal = defaultdict(set)
    # A word of its own keeps its -ය noun apart (රජ "king", රජය "government"), and so does a stem the pack glosses apart from
    # it (මත "on", මතය "opinion"; දහස "thousand", දහසය "sixteen"); other stems take it (වාහන: වාහනය).
    known = set(own) | {w for w in weak if w.endswith("ය") and glosses.get(w) and glosses.get(w[:-1]) and not alike(w, w[:-1])}
    for word in list(own) + sorted(weak):
        ks = kinds.get(word, {"unknown"})
        for form in word_forms(word, ks, known):
            if form in share or form in weak:
                made[form].add(word)
        for form in verb_forms(word):
            verbal[form].add(word)
        # (These two only when glossed alike or not at all, as they are often words of their own: අනේ of අන්, බිරිය of බුරනවා.)
        for form in (full_stem_forms(word) if ks & NOMINAL_KINDS and word[:-1] not in known else set()) | literary_pasts(word):
            if (form in share or form in weak) and form not in own and (not glosses.get(form) or alike(form, word)):
                made[form].add(word)
                if word.endswith("නවා"):
                    verbal[form].add(word)
        # An animate noun's bare stem, as compounds and speech have it (ගොවි of ගොවියා, ශිෂ්‍ය of ශිෂ්‍යයා, තාත්ත of තාත්තා), when the
        # pack glosses the two alike (not වැසි "rain" of වැසියා "resident").
        for stem in animate_stems(word):
            if (stem in share or stem in weak) and alike(stem, word):
                made[stem].add(word)
        # Another animate noun's plural in -න්, glossed alike (මිතුරන් of මිතුරා; not ඉක්මන් "quick" of ඉක්මා).
        # (And its plural in -ුන්: පුතුන් of පුතා.)
        if word.endswith("ා") and word[-2:-1] in CONSONANTS and letters(word) >= 3:
            for form in with_clitics({word[:-1] + t for t in N_PLURAL_TAILS + U_PLURAL_TAILS}):
                if (form in share or form in weak) and form not in own and alike(form, word):
                    made[form].add(word)
        for form in verbal_nouns(word):
            if (form in share or form in weak) and not noun_glossed(form):
                made[form].add(word)
                verbal[form].add(word)
        # A learned noun's person noun in -යා (විශේෂඥයා, විශේෂඥයෙක් of විශේෂඥ), glossed alike or not at all.
        # (When the pack has the person noun, it is the word, and this one its stem: animate_stems.)
        person = word + "යා"
        if word[-1] in CONSONANTS and not word.endswith(("ය", "ව")) and letters(word) >= 3 and person not in own and person not in weak:
            for form in {f for f in {person} | nominal_forms(person) if f.startswith(person[:-1])}:
                if (form in share or form in weak) and form not in own and (not glosses.get(form) or alike(form, word)):
                    made[form].add(word)
    # A hand fold's forms are its word's (බලපෑමක් of බලපෑම්), and NOT_FORMS are no forms of the words named.
    for variant, word in FOLDS.items():
        for form in word_forms(variant, kinds.get(variant, kinds.get(word, {"unknown"})), known):
            if ((form in share or form in weak) and form not in SINHALA_FORMS and form not in own
                    and (not glosses.get(form) or alike(form, word) or alike(form, variant))):
                made[form].add(word)
    for form, makers in NOT_FORMS.items():
        made[form] -= makers
        verbal[form] -= makers
    # A case form with the written language's -ය on it is that form's (හමුවකදීය "it was at a meeting": හමුවකදී, හමුව's).
    for word in sorted(weak):
        if word.endswith("ය") and word[:-1].endswith(CASE_ENDINGS) and made.get(word[:-1]):
            made[word] |= made[word[:-1]] - {word}

    # A spelling's forms (පක්‍ෂයේ of පක්‍ෂය, පක්ෂය's), and a form by hand with a clitic on it (වීමත් of වීම, වෙනවා's).
    spelled = defaultdict(set)
    for form, word in SINHALA_FORMS.items():
        if form == word:
            continue
        made_forms = with_clitics({form}) - {form}
        if form in SPELLINGS:
            made_forms |= word_forms(form, kinds.get(word, {"unknown"}), known)
        for made_form in made_forms:
            if (made_form in share or made_form in weak) and made_form not in SINHALA_FORMS:
                made[made_form].add(word)
                if form in SPELLINGS:
                    spelled[made_form].add(word)
                if word.endswith("නවා"):
                    verbal[made_form].add(word)
    def cases(word, makers):
        """The share of [word]'s own case forms in the blend (දඬුවම: දඬුවමක්, දඬුවමට, දඬුවමෙන්) that [makers] do not make."""
        tests = {word + t for t in CASE_TEST_TAILS} | full_stem_forms(word)
        return sum(share.get(f, 0.0) for f in tests if f not in own and not made.get(f, set()) & makers)

    def bare(word):
        """[word]'s forms by rule without the clitics."""
        global CLITICS
        clitics, CLITICS = CLITICS, []
        try:
            return word_forms(word, kinds.get(word, {"unknown"}), known) | verb_forms(word)
        finally:
            CLITICS = clitics

    # A word another makes only by a clitic (දඬු + ම), a verb's spoken -ල, -ලා or -පු (මහනවා: මහල) or an animate noun's bare stem
    # (වර්ගයා: වර්ගය) is a word of its own when it takes the cases itself, as no such form does (දඬුවමක්, මහලේ, වර්ගයේ).
    for word in sorted(f for f in made if f in weak and f not in own):
        loose = set()
        for maker in made[word]:
            if maker == word:
                continue
            if (any(word.endswith(c) and (word[:-len(c)] == maker or maker in made.get(word[:-len(c)], ())
                                          or SINHALA_FORMS.get(word[:-len(c)]) == maker) for c in CLITICS)
                    and word not in bare(maker) and word not in SINHALA_FORMS):
                loose.add(maker)
            elif maker.endswith("නවා") and word.endswith(SPOKEN_VERB_TAILS) and word in verb_forms(maker):
                loose.add(maker)
            elif word in animate_stems(maker):
                loose.add(maker)
        if loose and cases(word, loose) >= CASED_SHARE * share.get(word, 0.0):
            made[word] -= loose
            verbal[word] -= loose
    # A pack name's case forms are the name's, which the list leaves out (ගාල්ලේ, ගාල්ලට of ගාල්ල), when the pack glosses them
    # alike or not at all and no word makes them.
    name_forms = set()
    for name in sorted(names):
        for form in nominal_forms(name, known):
            if form in weak and form not in own and not made.get(form) and (not glosses.get(form) or alike(form, name)):
                name_forms.add(form)
    # A compound verb's form written as one word, when the list has no such verb (ඉදිකළා, පත්කළා, සිදුකරන): the first word's,
    # or the verb's whose form that is (තෝරාගෙන of තෝරනවා, හොයාගෙන of සොයනවා).
    # (And such a form the pack lacks: සිදුකර, ඉදිකළේ.)
    orphans = {f for f in share if f not in own and f not in weak and not made.get(f) and f not in SINHALA_FORMS}
    for word in sorted(weak | orphans):
        # (ගත of ගන්නවා after a verb's absolutive: දැකගත of දකිනවා; after another word it is a word's: අන්තර්ගත.)
        if word.endswith("ගත") and SINHALA_FORMS.get(word[:-2], "").endswith("නවා"):
            made[word].add(SINHALA_FORMS[word[:-2]])
            verbal[word].add(SINHALA_FORMS[word[:-2]])
            continue
        parts = joined_parts(word)
        if not parts or parts[0] + parts[1] in own or parts[0] + parts[1] in weak:
            continue
        first, base = parts
        # A verb's absolutive before ගන්නවා is that verb's (තෝරාගෙන); before වෙනවා only a word of its own, as a causative's
        # stem is no word (ඉගැන්වීම, හැකිවීම).
        verbs = set()
        if base == "ගන්නවා":
            verbs = {SINHALA_FORMS[first]} if SINHALA_FORMS.get(first, "").endswith("නවා") else set(verbal.get(first, ()))
        if verbs:
            for verb in verbs:
                made[word].add(verb)
                verbal[word].add(verb)
        elif first in own or first in weak and base != "වෙනවා" and first not in verbal:
            made[word].add(first)
    # A word only the parallel dictionary has is a word when no shorter word makes it, and when it is no stem of a noun in -ය
    # or -ව that is common enough beside it (ප්‍රශ්න of ප්‍රශ්නය; not ජාතික "national" of the rare ජාතිකය).
    stems = {}
    for word in set(own) | weak:
        stem = stem_of(word)
        if stem in weak and share.get(word, 0.0) >= STEM_SHARE * share.get(stem, 0.0):
            stems[stem] = word
    # A verb first, as its forms are shorter than it (හදන of හදනවා).
    words = set(own) | {w for w in weak if w.endswith("නවා") and not any(m in own for m in made.get(w, ()))}
    def definite_of(form, word):
        """Whether [form] is a case of [word]'s definite form, glossed alike (පාර්ලිමේන්තුවේ of පාර්ලිමේන්තුව of පාර්ලිමේන්තු)."""
        return (not word.endswith("නවා") and alike(form, word)
                and any(w in (word + "ය", word + "ව") and w != form for w in made.get(form, ())))

    def chained(form, word):
        """
        Whether [form] is a case of a form of [word] that is no word, glossed alike or not at all (හැඟීමක් of හැඟීම of හැඟෙනවා,
        ක්‍රියාවට of ක්‍රියාව of ක්‍රියා): of a verb only its verbal noun takes cases.
        """
        return any(w != form and w not in words and word in made.get(w, ()) and (not glosses.get(form) or alike(form, w))
                   and (not glosses.get(w) or not glosses.get(word) or alike(w, word))
                   and (not word.endswith("නවා") or w.endswith(VERBAL_NOUN_ENDINGS)) for w in made.get(form, ()))

    def made_by_word(word):
        """Whether a word makes [word], or makes the definite form or the form it is a case of."""
        return (any(w in words and not unrelated(word, w) for w in made.get(word, ()) if w != word)
                or any(definite_of(word, g) or chained(word, g)
                       for w in made.get(word, ()) if w not in words for g in made.get(w, ()) if g in words))

    for word in sorted(weak, key=lambda w: (len(w), w)):
        # (A spelling's form is its word's even when that word is longer and not yet taken: අමාත්‍යංශ of අමාත්‍යාංශය.)
        if (word not in stems and word not in name_forms and not made_by_word(word)
                and not any(w != word and (w in own or w in weak) for w in spelled.get(word, ()))):
            words.add(word)
    # A plural shorter than its noun, or as long (නිලධාරීන්, නිලධාරින් of නිලධාරියා, ළමයි of ළමයා), is the noun's.
    for word in sorted(words & weak, key=lambda w: (len(w), w)):
        if any(w in words and w.endswith("ා") and len(w) >= len(word) and w != word and not unrelated(word, w) for w in made.get(word, ())):
            words.discard(word)
    # One word in two spellings (ක්‍ෂ and ක්ෂ, ඟ and ග, ළ and ල, කැමැති and කැමති): the rarer is the commoner's when the pack glosses
    # them alike; FOLDS by hand first.
    variant_of = {v: w for v, w in FOLDS.items() if w in words}
    # A compound verb in -කරනවා or -වෙනවා written as one word the pack lacks is its first word's, glossed alike (පළකරනවා,
    # පළවෙනවා: පළ; not සැඟවෙනවා, an e-stem), and
    # one in -ගන්නවා on a verb's absolutive that verb's (දැකගන්නවා: දකිනවා).
    for verb in sorted(words & weak):
        for base in ("කරනවා", "වෙනවා", "ගන්නවා"):
            first = verb[:-len(base)]
            if not verb.endswith(base) or letters(first) < 2 or verb in variant_of:
                continue
            if base == "ගන්නවා":
                if SINHALA_FORMS.get(first, "").endswith("නවා") and SINHALA_FORMS[first] in words:
                    variant_of[verb] = SINHALA_FORMS[first]
            elif first in words and alike(first, verb):
                variant_of[verb] = first
    by_key = defaultdict(list)
    for word in words:
        by_key[spelling_key(word)].append(word)
    for group in by_key.values():
        group.sort(key=lambda w: (-share.get(w, 0.0), w))
        for word in group[1:]:
            head = next((w for w in group if w != word and w not in variant_of and (alike(w, word) or same_letters(w, word))), None)
            if head and word not in variant_of and share.get(head, 0.0) >= share.get(word, 0.0):
                variant_of[word] = head
    # A spelling of a word folded by hand is that word's too (අධිකාරී as අධිකාරි, අධිකාරිය's).
    folded_keys = {spelling_key(v): w for v, w in FOLDS.items() if v in variant_of}
    for word in sorted(words):
        head = folded_keys.get(spelling_key(word))
        if head and word not in variant_of and word != head and head in words:
            variant_of[word] = head
    # A noun and its shorter plural, glossed alike, are one word, the commoner (කටයුතු: කටයුත්ත; ජාතිය: ජාති).
    for noun in sorted(words):
        # (Any spelling of the plural, but -ු itself: ලක්වූ is a past of ලක්ව.)
        for plural in sorted(w for p in plural_stems(noun) for w in [p] + (by_key.get(spelling_key(p), []) if p != noun + "ු" else [])):
            if plural in words and plural not in variant_of and noun not in variant_of and alike(plural, noun):
                rarer, commoner = sorted((plural, noun), key=lambda w: (share.get(w, 0.0), w))
                variant_of[rarer] = commoner
    # A verb the pack lacks (දරනවා, කැඳවනවා) has its forms the pack has under its commonest one (දරන: දරා, දැරූ, දැරීම), when the
    # pack glosses them alike, and its other forms too.
    # A literary verb the pack lacks is its spoken one's (ගසනවා: ගහනවා, ගසා, ගැසූ).
    for verb in sorted({w for w in share if w.endswith("නවා")} | set(LITERARY_VERBS)):
        if verb in own or verb in weak or verb.startswith(NEGATIVE) or letters(verb) < 4:
            continue
        rule_forms = (verb_forms(verb) | {verb} | absolutives(verb) | literary_pasts(verb)
                      | {f for f in verbal_nouns(verb) if not noun_glossed(f)})
        # (A hand fold stays in its verb's group, which follows it: ලැබිය.)
        group = sorted((w for w in rule_forms if w in words and w in weak and (w not in variant_of or w in FOLDS)),
                       key=lambda w: (-share.get(w, 0.0), w))
        spoken = LITERARY_VERBS.get(verb) or SINHALA_FORMS.get(verb, "")
        if not spoken:
            # Or the verb two of its forms are listed under by hand (පැවැත්වේ, පැවැත්විණ: පවත්වනවා).
            listed = sorted(SINHALA_FORMS[w] for w in verb_forms(verb) if SINHALA_FORMS.get(w, "").endswith("නවා"))
            spoken = next((v for v in listed if listed.count(v) >= 2), "")
        if spoken.endswith("නවා") and spoken in words:
            group = [spoken] + group
        if len(group) < 2:
            continue
        # The commonest that is surely a form of it (not පුරා "throughout" of පුරනවා).
        head = next((w for w in group if w == spoken or w.endswith(VERBAL_TAILS) or any(alike(w, o) for o in group if o != w)), None)
        if not head:
            continue
        group = [head] + [w for w in group if w != head]
        # (The absolutive in -ා too when the verb is known by hand: ගසා of ගසනවා, ගහනවා's.)
        tails = VERBAL_TAILS + ("ා",) if head == spoken else VERBAL_TAILS
        joined = [w for w in group[1:] if alike(w, head) or w.endswith(tails)]
        if not joined:
            continue
        for w in joined:
            variant_of.setdefault(w, head)
        for form in rule_forms:
            if form in share and form not in words:
                made[form].add(head)
                verbal[form].add(head)
    # A misspelling of a word's form, commoner and glossed alike, is that word's (හදුන්වා of හඳුන්වා, ලැබු of ලැබූ).
    form_keys = defaultdict(list)
    for form in made:
        if form not in words and form in share and glosses.get(form):
            form_keys[spelling_key(form)].append(form)
    for word in sorted(words & weak):
        if word in variant_of:
            continue
        for form in sorted(form_keys.get(spelling_key(word), ())):
            makers = sorted(m for m in made[form] if m in words and m != word)
            if form != word and len(makers) == 1 and share[form] >= share.get(word, 0.0) and (alike(word, form) or same_letters(word, form)):
                variant_of[word] = makers[0]
                break
    words -= set(variant_of)

    def resolved(word):
        for _ in range(3):
            word = variant_of.get(word, word)
        return word

    def heads_of(form):
        """(the word the form is itself, or None; the other words it is a form of)."""
        if form in SINHALA_FORMS:
            return None, {SINHALA_FORMS[form]}
        if form in variant_of:
            return None, {resolved(form)}
        if form in NOT_WORDS or (form in names or form in name_forms) and form not in made:
            return None, set()
        if form.startswith(NEGATIVE) and len(form) > 3 and form not in words:
            rest = form[len(NEGATIVE):]
            if rest in SINHALA_FORMS and SINHALA_FORMS[rest].endswith("නවා"):
                return None, {SINHALA_FORMS[rest]}
            verbs = {w for w in verbal.get(rest, ()) if w in words}
            if verbs:
                return None, verbs
        own_word = form if form in words else None
        makers = {resolved(w) for w in made.get(form, ())} - {form}
        verbs = {resolved(w) for w in verbal.get(form, ())} & makers & words
        if not own_word and not makers & words:
            # A case of a definite form that is no word (පාර්ලිමේන්තුවේ of පාර්ලිමේන්තුව, itself පාර්ලිමේන්තු's).
            makers = {resolved(g) for w in makers for g in made.get(w, ()) if resolved(g) in words and definite_of(form, g)} - {form}
            if not makers:
                # Or a case of a form that is no word (හැඟීමක් of හැඟීම, a form of හැඟෙනවා).
                makers = {resolved(g) for w in made.get(form, ()) for g in made.get(w, ()) if resolved(g) in words and chained(form, g)} - {form}
        makers &= words
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
    return scores, forms


def _rank(scores, forms, size, label):
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
