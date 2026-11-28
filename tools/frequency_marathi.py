"""
The Marathi frequency list (build_frequency_list.py --language mr).

Marathi writes a noun's case endings and postpositions on its oblique stem (घर: घरात, घराला, घराचा, घरासाठी; शाळा: शाळेत;
पाणी: पाण्यात) and the clitics -च "just" and -ही "also" after them (घरीच, मीही). The pack's tables give most nouns' cases
and some verbs' tenses (केला, करतो, करावे), but not the postpositions, the pronouns' forms, the adjectives', nor most verbs'
future and habitual tenses. So each word of the pack is declined and conjugated here by its ending and its table, and a
corpus form is counted for the words it is a form of: by hand (MARATHI_FORMS: the pronouns, irregular verbs), by the pack's
tables, or by these rules. A form that is a word of the pack itself keeps its count (कारण "reason", not कर's), unless the
word it is also a form of is more common than the form (केले goes to करणे). A form of several words is shared between them
by how common each is by its forms of no other word.

The pack lacks thousands of everyday words (लक्ष, विभाग, पीक, पिढी, कारवाई, बांधकाम): they are found in the corpus itself
(induce), from the forms no word of the pack explains, as the Bengali and Punjabi lists find them. A form spelled with the
other i or u than an explained one (करुन, सुरु) is a slip of it. Names are left out: those the sentences show before the
honorific यांनी or a word of place (corpus_names), and by hand (INDUCED_NOT_WORDS), with pieces of words cut at a number and
abbreviations (FRAGMENTS).

The news corpus of 2016 was converted from an old font: it writes the eyelash ra as ऱ with no virama (दुसऱया) and the
conjunct -्य as य़ (मोठय़ा); such spellings are read as the ones texts and the reader have (marathi_fold).
"""
import heapq
import io
import json
import math
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import Counter, defaultdict

# As the reader's Marathi wordCharacters: letters and vowel signs, no digits or danda.
LETTERS = re.compile("[\u0900-\u0963\u0971-\u097f\u200c\u200d]+")
CONSONANTS = "\u0915-\u0939\u0958-\u095f\u0979-\u097f"
VOWEL_SIGNS = set("ािीुूृॅेैॉोौंँः")
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep"}
FORMS_LISTED = 400
# Each source's weight: the news (three Leipzig corpora, 1.4 million sentences) and the Wikipedia's sentences, which bring
# in the words of nature, places and things the news lacks.
SOURCE_WEIGHTS = {"leipzig": 0.8, "wikipedia_sentences": 0.2}


def marathi_fold(form):
    """A Marathi spelling as the reader and pack have it (NFC, eyelash ra as ऱ्), the old-font news spellings mended."""
    form = re.sub("(?<!\u094d)\u0930\u094d\u200d", "\u0931\u094d", unicodedata.normalize("NFC", form))
    form = form.replace("\u094d\u200d", "\u094d").replace("\u094d\u0931\u094d", "\u094d\u0930\u094d")
    # ऱ with no virama before a consonant (दुसऱया), and य़ after a consonant (मोठय़ा), are the old font's ऱ्य and ्य.
    form = re.sub(f"\u0931(?=[{CONSONANTS}])", "\u0931\u094d", form)
    form = re.sub(f"(?<=[{CONSONANTS}])\u092f\u093c", "\u094d\u092f", form).replace("\u094d\u092f\u093c", "\u094d\u092f")
    # A nukta after a vowel sign or at the start is a slip (आहे़).
    form = re.sub(f"(?<![{CONSONANTS}])\u093c", "", form)
    return form


# A joiner left after folding (two after the व् of रिझर्व्ह, as one site types it) is a typing slip.
JOINER = re.compile("[\u200c\u200d]")

# The case endings a noun's oblique stem takes (घरा-ला, घरा-ने, घरा-त, घरा-चा), and the postpositions written on it
# (घरा-साठी, शाळे-मध्ये, पाण्या-वर).
CASES = ["ला", "ना", "ने", "नं", "नी", "शी", "त", "ं", "चा", "ची", "चे", "च्या", "चं", "स", "तून", "तील", "ंत"]
POSTPOSITIONS = ["साठी", "मध्ये", "मधून", "मधील", "मधे", "वर", "वरून", "वरील", "वरती", "कडे", "कडून", "कडील", "पासून", "पर्यंत",
                 "बरोबर", "सोबत", "सह", "शिवाय", "विना", "जवळ", "समोर", "मागे", "पुढे", "खाली", "नंतर", "पूर्वी", "आधी", "पेक्षा",
                 "प्रमाणे", "सारखा", "सारखी", "सारखे", "सारख्या", "सारखं", "मुळे", "विषयी", "बद्दल", "करिता", "ऐवजी", "द्वारे",
                 "पाशी", "भोवती", "विरुद्ध", "विरोधात", "पलीकडे", "बाबत", "संदर्भात", "पैकी", "तर्फे", "अंतर्गत", "दरम्यान",
                 "निमित्त", "निमित्ताने", "आत", "बाहेर", "मार्फत", "पोटी", "लाही", "लाच", "चाच", "चीच", "चेच", "च्याच", "हून",
                 "नुसार", "संबंधी"]
# The genitives of the locative and of postpositions (गावातला, गल्लीतल्या, धाब्यावरच्या, घरामधला, आईबद्दलचे), on the oblique.
POSTPOSITIONS += [p + t for p in ("त", "वर", "मध", "बद्दल", "साठी", "पर्यंत", "पासून", "कड", "जवळ", "समोर", "नंतर", "पूर्वी", "बाबत",
                                  "संबंधी")
                  for t in (("ला", "ली", "ले", "ल्या", "लं") if p in ("त", "मध") else ("चा", "ची", "चे", "च्या", "चं"))]
OBLIQUE_TAILS = CASES + POSTPOSITIONS
CLITICS = ["च", "ही", "सुद्धा"]
# Many nouns take the cases on their direct form too (सरकारने, रेल्वेत, सकाळचा, बाहेरच्या, प्रकारचे), and adverbs of time
# take the genitive and पर्यंत, पासून (सध्याचा, आतापर्यंत). Not -ं, -ंत, -स, -ना, -नी, -शी there: so many other words end
# in them (सावंत, मानस, परवाना, जोशी); after a consonant not the locative -त, -तून, -तील either (रावत, कामत), which goes on
# the oblique (हॉटेलात).
DIRECT_TAILS = [t for t in OBLIQUE_TAILS if t not in ("ं", "ंत", "स", "ना", "नी", "शी")]
CONSONANT_DIRECT_TAILS = [t for t in DIRECT_TAILS if t not in ("त", "तून", "तील", "तला", "तली", "तले", "तल्या", "तलं")]
ADVERB_TAILS = ["चा", "ची", "चे", "च्या", "चं", "पर्यंत", "पासून"]
# The tails that also end a verb's past or present (बसला, नसतील): not on a word that is a verb's stem too (बस, नस).
VERB_LIKE_TAILS = ("ला", "लाच", "लाही", "त", "तील")

# A verb's present stem's endings (करतो, करते, करतात, करत, करताना), its future's (करेन, करशील, करेल, करू, कराल, करतील), and
# the endings of the stem before -ाय (करायचा, करायला) and the infinitive's oblique (करण्यासाठी).
PRESENT_TAILS = ["त", "तो", "ते", "ता", "तात", "तोस", "तेस", "तं", "तोय", "तेय", "ताय", "तायत", "ताहेत", "ताना", "तील", "तेत",
                 "तच", "तोच", "तेच", "ताच", "त्या", "ती"]
FUTURE_TAILS = ["ेन", "ीन", "शील", "ेल", "ू", "ाल", "तील", "ेल", "ूया", "ूयात", "ा", "ावा", "ावी", "ावे", "ाव्या", "ावं", "ावेत",
                "ावीत", "ावेसे", "ावेसा", "ावेसं", "ावेसेच", "ायचा", "ायची", "ायचे", "ायच्या", "ायचं", "ायचो", "ायचास", "ायला",
                "ायलाच", "ायलाही", "ावयाचे", "ावयाचा", "ावयाची", "ावयाच्या", "ून", "ूनही", "ूनच", "ूनसुद्धा", "ू नका", "ेपर्यंत",
                "ेना"]
VOWEL_FUTURE_TAILS = ["ईन", "शील", "ईल", "ऊ", "ल", "तील", "ऊया", "वा", "वी", "वे", "व्या", "वं", "वेत", "वीत", "यचा", "यची", "यचे",
                      "यच्या", "यचं", "यचो", "यला", "यलाच", "यलाही", "ऊन", "ऊनही", "ऊनच", "ईपर्यंत", "ईना"]
INFINITIVE_TAILS = ["णे", "णं", "णार", "णारा", "णारी", "णारे", "णाऱ्या", "णाऱ्यां", "णाऱ्यांना", "णाऱ्यांनी", "णाऱ्यांचा", "णाऱ्यांची",
                    "णाऱ्यांचे", "णाऱ्यांच्या", "णाऱ्याला", "णाऱ्याचा", "णाऱ्याची", "णाऱ्याचे", "णाऱ्याच्या", "णारं", "णारच", "णारही",
                    "णेच", "णेही", "णंच"]
INFINITIVE_OBLIQUE_TAILS = ["ला", "त", "ची", "चा", "चे", "च्या", "चं", "स", "ने", "साठी", "पूर्वी", "आधी", "बद्दल", "मुळे", "पेक्षा",
                            "ऐवजी", "विषयी", "करिता", "पासून", "वर", "सारखे", "सारखी", "सारखा", "सारखं", "ऐवढे", "इतके", "ची", "तच", "तही",
                            "लाच", "साठीच", "पुरते"]
# A past's endings by gender, number and person (केला, केली, केले, केल्या, केलं, केलो, केलास, केलात, केलेत, केलीस) and its
# participle's (केलेला, केलेल्या) and oblique's (केल्याने, केल्यावर, केल्यास, केल्यानंतर).
PAST_TAILS = ["ा", "ी", "े", "्या", "ं", "ो", "ास", "ात", "ेत", "ीत", "ीस", "्यात", "ेला", "ेली", "ेले", "ेल्या", "ेलं", "ेलो",
              "ेल्यां", "ेल्यांना", "ेल्यांनी", "ेल्यांचा", "ेल्यांची", "ेल्यांचे", "ेल्यांच्या", "ेल्याला", "ेल्याचा", "ेल्याची",
              "ेल्याचे", "ेल्याच्या", "्याने", "्यानं", "्यावर", "्यास", "्यानंतर", "्यामुळे", "्याशिवाय", "्याबद्दल", "्याप्रमाणे",
              "्याचा", "्याची", "्याचे", "्याच्या", "्याचं", "्यावरही", "्यासारखे", "्यासारखा", "्यासारखी", "्यापासून", "ाच", "ीच",
              "ेच", "्याच", "ाही", "ीही", "ेही", "्याही", "्यानेच", "्यावरच", "ेलेच", "ेलेही", "ेलेल्याच"]

# Forms the rules make wrongly or not at all, by hand: form -> word. The pronouns' forms are their obliques with the cases
# and postpositions (OBLIQUE_TAILS) and clitics; MARATHI_PRONOUNS gives each pronoun's own ones and its oblique stems.
MARATHI_PRONOUNS = {
    "मी": ("मला मलाच मलाही माझा माझी माझे माझ्या माझं माझेच माझाच माझीच माझ्याच मीच मीही मीसुद्धा मज मजला", ["माझ्या"]),
    "तू": ("तुला तुलाच तुलाही तुझा तुझी तुझे तुझ्या तुझं तुझेच तुझाच तुझीच तूच तूही तुज", ["तुझ्या"]),
    "आम्ही": ("आम्हाला आम्हांला आम्हीच आम्हीही आमचा आमची आमचे आमच्या आमचं आमचेच आम्हां आम्हास", ["आमच्या", "आम्हां"]),
    "तुम्ही": ("तुम्हाला तुम्हांला तुम्हीच तुम्हीही तुमचा तुमची तुमचे तुमच्या तुमचं तुमचेच तुम्हां तुम्हास", ["तुमच्या", "तुम्हां"]),
    "आपण": ("आपल्याला आपला आपली आपले आपल्या आपलं आपलेच आपलाच आपलीच आपणच आपणही आपणास आपणांस", ["आपल्या"]),
    "तो": ("त्या त्याला त्याने त्यानं त्याचा त्याची त्याचे त्याच्या त्याचं त्याच त्यास तोच तोही त्याचाच त्याचीच त्याचेच", ["त्या", "त्याच्या"]),
    "ती": ("तिला तिने तिनं तिचा तिची तिचे तिच्या तिचं तीच तीही तिलाच तिचाच तिचीच तिचेच", ["तिच्या"]),
    "ते": ("त्यांना त्यांनी त्यांचा त्यांची त्यांचे त्यांच्या त्यांचं तेच तेही त्यांनाच त्यांचाच त्यांचीच त्यांचेच त्यांच्याच त्यांस",
           ["त्यां", "त्यांच्या"]),
    "हा": ("या ह्या याला याने यानं याचा याची याचे याच्या याचं हाच हाही ह्याला ह्याने ह्याचा ह्याची ह्याचे ह्याच्या ह्याचं यास", ["या", "ह्या", "याच्या"]),
    "ही": ("हिला हिने हिचा हिची हिचे हिच्या हिचं हीच", []),
    "हे": ("यांना यांनी यांचा यांची यांचे यांच्या यांचं हेच हेही ह्यांना ह्यांनी ह्यांचा ह्यांची ह्यांचे ह्यांच्या", ["यां", "ह्यां", "यांच्या"]),
    "कोण": ("कोणाला कोणी कोणाचा कोणाची कोणाचे कोणाच्या कोणाचं कोणाशी कोणीही कोणाही कोणालाही कुणाला कुणाचा कुणाची कुणाचे कुणाच्या "
            "कोणच कोणता कोणती कोणते कोणत्या कोणतं कोणतीही कोणताही कोणतेही कोणत्याही", ["कोणा", "कुणा", "कोणत्या", "कोणाच्या"]),
    "काय": ("कशाला कशाचा कशाची कशाचे कशाच्या कशात कशानं कशाने कशामुळे कशासाठी कशावर कशाबद्दल कायच कायही", ["कशा"]),
    "जो": ("ज्या ज्याला ज्याने ज्याचा ज्याची ज्याचे ज्याच्या ज्याचं जी जे ज्यांना ज्यांनी ज्यांचा ज्यांची ज्यांचे ज्यांच्या जोच", ["ज्या", "ज्यां", "ज्याच्या", "ज्यांच्या"]),
    "स्वतः": ("स्वतःला स्वतःचा स्वतःची स्वतःचे स्वतःच्या स्वतःचं स्वतःच स्वतःशी स्वतःहून स्वत", ["स्वतःच्या"]),
    "कोणी": ("कोणीतरी कुणीतरी कोणालातरी कुणालातरी", []),
    "एकमेक": ("एकमेकांना एकमेकांनी एकमेकांचा एकमेकांची एकमेकांचे एकमेकांच्या एकमेकांचं एकमेकांनाच एकमेकांचेच", ["एकमेकां"]),
}
MARATHI_FORMS = {
    # असणे's present and past (आहे, होता), and its negative नसणे's (नाही is the particle "no, not").
    "आहे": "असणे", "आहेत": "असणे", "आहेस": "असणे", "आहोत": "असणे", "आहात": "असणे", "आहेच": "असणे", "आहेतच": "असणे",
    "आहेही": "असणे", "आहेना": "असणे", "आहेय": "असणे",
    "होता": "असणे", "होती": "असणे", "होते": "असणे", "होतो": "असणे", "होतास": "असणे", "होतात": "असणे", "होत्या": "असणे",
    "होतीस": "असणे", "होतं": "असणे", "होतेच": "असणे", "होताच": "असणे", "होतीच": "असणे",
    "नव्हता": "नसणे", "नव्हती": "नसणे", "नव्हते": "नसणे", "नव्हतो": "नसणे", "नव्हत्या": "नसणे", "नव्हतं": "नसणे", "नव्हतास": "नसणे",
    "नाहीत": "नाही", "नाहीस": "नाही", "नाहीये": "नाही", "नाहीयेत": "नाही", "नाहीच": "नाही", "नाहीतच": "नाही", "नाहीय": "नाही",
    # होणे's past (झाला) and its forms on व्हा- (व्हावे, व्हायला).
    "झाला": "होणे", "झाली": "होणे", "झाले": "होणे", "झाल्या": "होणे", "झालं": "होणे", "झालो": "होणे", "झालास": "होणे",
    "झालात": "होणे", "झालेत": "होणे", "झालीत": "होणे", "झालीस": "होणे", "झाल्यात": "होणे", "होईल": "होणे", "होईन": "होणे",
    "होशील": "होणे", "होऊ": "होणे", "व्हाल": "होणे", "होतील": "होणे", "व्हा": "होणे", "होवो": "होणे",
    # जाणे's past (गेला), येणे's (आला) and its forms on या- (यायला, यावे), देणे's (दिला) and घेणे's (घेतला).
    "गेला": "जाणे", "गेली": "जाणे", "गेले": "जाणे", "गेल्या": "जाणे", "गेलं": "जाणे", "गेलो": "जाणे", "गेलास": "जाणे", "गेलात": "जाणे",
    "गेलेत": "जाणे", "गेलीत": "जाणे",
    "आला": "येणे", "आली": "येणे", "आले": "येणे", "आल्या": "येणे", "आलं": "येणे", "आलो": "येणे", "आलास": "येणे", "आलात": "येणे",
    "आलेत": "येणे", "आलीत": "येणे", "ये": "येणे", "येईल": "येणे", "येईन": "येणे", "येशील": "येणे", "येऊ": "येणे",
    "याल": "येणे", "येतील": "येणे",
    "दिला": "देणे", "दिली": "देणे", "दिले": "देणे", "दिल्या": "देणे", "दिलं": "देणे", "दिलो": "देणे", "दिलास": "देणे",
    "दिलात": "देणे", "दिलेत": "देणे", "दिलीत": "देणे", "द्या": "देणे", "दे": "देणे", "देईल": "देणे", "देईन": "देणे",
    "द्याल": "देणे", "देतील": "देणे", "द्यावे": "देणे", "द्यावा": "देणे", "द्यावी": "देणे", "द्यायला": "देणे", "द्यायचे": "देणे",
    "द्यायचा": "देणे", "द्यायची": "देणे",
    "न्यायला": "नेणे", "न्यायचा": "नेणे", "न्यायची": "नेणे", "न्यायचे": "नेणे", "न्यायच्या": "नेणे", "न्यायचं": "नेणे",
    "न्यावे": "नेणे", "न्यावा": "नेणे", "न्यावी": "नेणे", "असेपर्यंत": "असणे", "असेपर्यंतच": "असणे",
    "घ्या": "घेणे", "घे": "घेणे", "घेईल": "घेणे", "घेईन": "घेणे", "घ्याल": "घेणे", "घेतील": "घेणे", "घ्यावे": "घेणे",
    "घ्यावा": "घेणे", "घ्यावी": "घेणे", "घ्यायला": "घेणे", "घ्यायचे": "घेणे", "घ्यायचा": "घेणे", "घ्यायची": "घेणे",
    # करणे's past on के- (केला: the table's), खाणे's (खाल्ला), पिणे's (प्यायला), पाहणे's (पाहिला), सांगणे's (सांगितला).
    "कर": "करणे", "करा": "करणे", "करीत": "करणे", "करीन": "करणे",
    "प्या": "पिणे", "पी": "पिणे", "प्यायला": "पिणे", "प्यायले": "पिणे", "प्यायली": "पिणे", "प्यायचा": "पिणे", "प्यायचे": "पिणे",
    "प्यावे": "पिणे", "पितो": "पिणे", "पिते": "पिणे", "पितात": "पिणे", "पित": "पिणे",
    # शकणे "can" and पाहिजे "must" (a word of its own).
    "शकतो": "शकणे", "शकते": "शकणे", "शकतात": "शकणे", "शकत": "शकणे", "शकेल": "शकणे", "शकतील": "शकणे", "शकाल": "शकणे",
    "शकलो": "शकणे", "शकला": "शकणे", "शकली": "शकणे", "शकले": "शकणे", "शकल्या": "शकणे", "शकतोस": "शकणे", "शकतेस": "शकणे",
    "शकू": "शकणे", "शकेन": "शकणे", "शकशील": "शकणे", "शकतं": "शकणे",
    "पाहिजेत": "पाहिजे", "पाहिजेच": "पाहिजे", "हवी": "हवा", "हवे": "हवा", "हव्या": "हवा", "हवं": "हवा", "हवेत": "हवा",
    "हवीत": "हवा", "हवाच": "हवा", "हवेच": "हवा", "नकोत": "नको", "नकोच": "नको", "नकोस": "नको", "नका": "नको", "नये": "नको",
    "नयेत": "नको",
    "नेते": "नेता", "जागी": "जागा", "जागीच": "जागा",
    # Time and place words with their cases.
    "आजचा": "आज", "आजची": "आज", "आजचे": "आज", "आजच्या": "आज", "आजचं": "आज", "आजच": "आज", "आजही": "आज",
    "उद्याचा": "उद्या", "उद्याची": "उद्या", "उद्याचे": "उद्या", "उद्याच्या": "उद्या", "कालचा": "काल", "कालची": "काल",
    "कालचे": "काल", "कालच्या": "काल", "आताचा": "आता", "आताची": "आता", "आताचे": "आता", "आताच्या": "आता", "आताच": "आता",
    "आत्ता": "आता", "आत्ताच": "आता", "इथला": "इथे", "इथली": "इथे", "इथले": "इथे", "इथल्या": "इथे", "तिथला": "तिथे",
    "तिथली": "तिथे", "तिथले": "तिथे", "तिथल्या": "तिथे", "इथेच": "इथे", "तिथेच": "तिथे", "कुठला": "कुठे", "कुठली": "कुठे",
    "कुठले": "कुठे", "कुठल्या": "कुठे", "कुठेही": "कुठे", "कुठेतरी": "कुठे", "कधीही": "कधी", "कधीच": "कधी", "कधीतरी": "कधी",
    "इथून": "इथे", "येथून": "इथे", "तिथून": "तिथे", "तेथून": "तिथे", "कुठून": "कुठे", "कोठून": "कुठे", "दिवशी": "दिवस",
    "दिवशीच": "दिवस", "दिवशीही": "दिवस", "वेळाने": "वेळ", "वेळानं": "वेळ",
    "पहिल्यांदा": "पहिले", "पहिल्यांदाच": "पहिले",
    # The genitives of adverbs of place (पुढचा "next", मागचा, खालचा, वरचा, आतला).
    **{stem + t: word for stem, word, tails in (("पुढ", "पुढे", "च"), ("माग", "मागे", "च"), ("खाल", "खाली", "च"), ("वर", "वर", "च"),
                                                ("आत", "आत", "ल"))
       for t in (tails + e + c for e in ("ा", "ी", "े", "्या", "ं") for c in ("", "च", "ही"))},
    # स्त्री's plural स्त्रिया, on स्त्रियां- in the cases (स्त्रियांना).
    **{f: "स्त्री" for f in ["स्त्रिया"] + [c + t for c in ("स्त्रियां",) for t in CASES + POSTPOSITIONS]},
    # Spellings of conjunctives with व for ऊ, or ऊ for वू (घेवून for घेऊन, ठेऊन for ठेवून).
    "घेवून": "घेणे", "देवून": "देणे", "येवून": "येणे", "होवून": "होणे", "ठेऊन": "ठेवणे", "ठेऊ": "ठेवणे", "जावून": "जाणे",
    "होवू": "होणे", "पहा": "पाहणे", "रहा": "राहणे",
    # घर's genitive on its direct form (घरचा "of the house, home-made"; घरच्यांना "to the family").
    **{"घर" + t + c: "घर" for t in ("चा", "ची", "चे", "च्या", "चं", "च्यां", "च्यांना", "च्यांनी", "च्यांचा", "च्यांची", "च्यांचे",
                                    "च्यांच्या") for c in ("", "ही")}, "दुसरीकडे": "दुसरे", "दुसरीकडून": "दुसरे", "दुसरीकडेच": "दुसरे",
    "काहीतरी": "काही", "संध्याकाळी": "संध्याकाळ", "संध्याकाळीच": "संध्याकाळ", "दुपारी": "दुपार", "दुपारीच": "दुपार",
    # असे "so", तसे, कसे, जसे agree as adjectives, with स as श before -ी and -्या (अशी, अशा).
    **{c + t: c + "से" for c in ("अ", "त", "क", "ज") for t in ("सा", "शी", "शा", "सं", "सेच", "साच", "शीच", "शाच", "शाप्रकारे", "सेही")},
}
# Words of their own that are spelled as another's form, which keep their count however common that word is: कारण
# "reason" (not कर's), नेता "leader" (not नेणे's), चालू "running".
KEEP = {"कारण", "मात्र", "सारखा", "आज", "काल", "म्हणजे", "म्हणून", "बाजार", "दरम्यान", "पण", "तर", "जर", "वर", "कमी", "आता", "भर",
        "नेता", "स्वीकार", "जपून", "देखावा", "कसला", "चालू", "म्हणे", "खाते", "देवी"}
# Spellings the pack has the other way, or lacks: each is declined as the word it is a spelling of.
SPELLINGS = {
    "येथे": "इथे", "तेथे": "तिथे", "जेथे": "जिथे", "कोठे": "कुठे", "पोलिस": "पोलीस", "आणी": "आणि", "सुध्दा": "सुद्धा",
    "पाहीजे": "पाहिजे", "बध्दल": "बद्दल", "महत्व": "महत्त्व", "कसा": "कसे", "संभाळणे": "सांभाळणे",
}
# Everyday words the pack lacks, with their kind as the rules decline them.
MARATHI_WORDS = {
    "येथील": "adj", "तेथील": "adj", "त्यामुळे": "conj", "यामुळे": "conj", "यावेळी": "adv", "त्यावेळी": "adv", "त्यानंतर": "adv",
    "यानंतर": "adv", "तसेच": "conj", "मात्र": "conj", "सर्व": "det", "प्रत्येक": "det", "इतर": "adj", "अनेक": "adj",
    "म्हणणे": "verb", "सांगणे": "verb", "पाहणे": "verb", "बघणे": "verb", "मिळणे": "verb", "घेणे": "verb", "खाणे": "verb",
    "ठेवणे": "verb", "विचारणे": "verb", "शिकणे": "verb", "आवडणे": "verb", "वाटणे": "verb", "पिणे": "verb", "झोपणे": "verb",
    "उठणे": "verb", "धावणे": "verb", "थांबणे": "verb", "विसरणे": "verb", "पोहोचणे": "verb",
    "जिथे": "adv", "इथे": "adv", "पाहिजे": "verb", "सुरू": "adv", "जसे": "adv", "एवढा": "adj", "इतका": "adj", "तितका": "adj",
    "नेता": "noun", "अधिकारी": "noun", "प्रमाणे": "postp", "आरोपी": "noun", "व्यक्ती": "noun", "पूर्वी": "adv", "जरी": "conj",
    "प्रवासी": "noun", "संस्कृती": "noun", "सारे": "adj", "कसला": "adj", "पुरावा": "noun", "पान": "noun",
    "किती": "adv", "बाबा": "noun", "संध्याकाळ": "noun", "दुपार": "noun", "डॉक्टर": "noun", "थंडी": "noun", "उशीर": "noun",
    "पटकन": "adv", "शिजवणे": "verb", "टेबल": "noun", "कृपया": "adv", "तिकीट": "noun", "हॉटेल": "noun", "नाटक": "noun",
    "फोन": "noun", "संगणक": "noun", "वरण": "noun", "मोदक": "noun", "पूजा": "noun", "सूर्य": "noun", "चंद्र": "noun",
    "हस्तकला": "noun", "आसन": "noun", "न": "particle", "सांभाळणे": "verb",
    "खोका": "noun", "मजुरी": "noun", "अलीकडे": "adv", "लढत": "noun", "शेती": "noun",
}
# The masculine and neuter nouns with a locative in -ी (घरी "at home", गावी, वर्षी "in the year").
LOCATIVE_I = {"घर", "गाव", "वर्ष", "साल", "ठिकाण", "प्रकरण", "मार्ग", "स्थान", "हात", "शेत", "देव", "जाग", "दार", "पाठ", "मन", "काम",
              "सोमवार", "मंगळवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार", "रविवार"}
# Pack words of rare senses whose spelling is a commoner word's form: वेळा "large cardamoms" (वेळ's plural), ज्या "bowstring"
# (जो's oblique), शीस "fruit bud" (its oblique शिसा- is शिसे "lead"'s).
DROPPED = {"वेळा", "ज्या", "शीस"}
# Pasts of verbs the rules make wrongly: the past stem (for PAST_TAILS) by verb.
# The stem the future's and desiderative's endings in -व-, -य- go on of verbs whose stem changes (यावे, घ्यायला, न्यायचे).
VOWEL_STEMS = {"येणे": "या", "घेणे": "घ्या", "देणे": "द्या", "नेणे": "न्या", "पिणे": "प्या", "होणे": "व्हा"}
DESIDERATIVE_TAILS = ["वे", "वी", "वा", "वेत", "वीत", "वं", "व्या", "यला", "यलाच", "यलाही", "यचा", "यची", "यचे", "यच्या", "यचं",
                      "यचो", "वयाचा", "वयाची", "वयाचे", "वयाच्या", "वेसे", "वेसा", "वेसं"]
PAST_STEMS = {"करणे": ["केल"], "जाणे": ["गेल"], "येणे": ["आल"], "देणे": ["दिल"], "घेणे": ["घेतल"], "होणे": ["झाल"], "असणे": ["असल"],
              "खाणे": ["खाल्ल"], "पिणे": ["प्यायल", "पील"], "पाहणे": ["पाहिल"], "सांगणे": ["सांगितल"], "बघणे": ["बघितल"],
              "लिहिणे": ["लिहिल"], "राहणे": ["राहिल"], "म्हणणे": ["म्हटल", "म्हणाल"], "मारणे": ["मारल"], "धुणे": ["धुतल"],
              "नसणे": ["नसल"], "शिकणे": ["शिकल"], "बसणे": ["बसल"], "वाहणे": ["वाहिल"], "सोडणे": ["सोडल"], "शोधणे": ["शोधल"],
              "हसणे": ["हसल"], "रडणे": ["रडल"], "ऐकणे": ["ऐकल"], "सापडणे": ["सापडल"], "भेटणे": ["भेटल"], "आणणे": ["आणल"],
              "नेणे": ["नेल"], "झोपणे": ["झोपल"], "पडणे": ["पडल"], "धरणे": ["धरल"], "मिळणे": ["मिळाल"], "वाटणे": ["वाटल"],
              "लागणे": ["लागल"], "दिसणे": ["दिसल"], "कळणे": ["कळल"], "समजणे": ["समजल"], "आवडणे": ["आवडल"]}
# Links of the pack's tables that are wrong: मल "dirt" has no form मला.
WRONG_LINKS = {("मला", "मल")}
# Corpus spellings that are no words: letters, abbreviations, English words in Devanagari.
NOT_WORDS = {"डॉ", "श्री", "सौ", "कु", "प्रा", "ना", "रु", "क्र", "स", "क", "द", "ए", "बी", "सी", "डी", "जी", "ऑफ", "एस", "आर", "टी"}
# Pack words the news has as names far more than as words (पाटील "village headman", शरद "autumn", अनिल "wind").
NOT_WORDS |= {"पाटील", "शरद", "देसाई", "अनिल", "शहा", "प्रशांत", "कृष्ण", "कृष्णा"}
# Names, parties, English words of names (ऑफ, इंडिया) and abbreviations the corpus has that the pack does not and the
# sentences do not show as names (corpus_names): no words for induce. Their forms (मुंबईत, भाजपने) are theirs.
INDUCED_NOT_WORDS = set("""
भाजप भाजपा काँग्रेस कॉंग्रेस काँगेस शिवसेना मनसे मनसा आप तृणमूल मगो युवासेना राष्ट्रवादी अमेरिका लोकसभा कर्नाटक गोवा बिहार
पंजाब बंगाल फेसबुक श्रीलंका न्यूझीलंड इटली जम्मू हरियाणा हरयाणा आंध्र कार्तिक दाऊद न्यूयॉर्क सीरिया नागपुर कोल्हापुर तामिळनाडू
तमिळनाडू हिमालय उत्तराखंड नायर शिव वाराणसी मेक्सिको श्रीनगर मद्रास बेल्जियम बांदा बार्सिलोना अरुणाचल कोलंबिया सिडनी स्वीडन उमर
विकिपीडिया वेल्स कालिदास यूट्यूब बीजिंग आगरा मालदीव गया बर्लिन सिंध त्रिपुरा बांग्लादेश सिद्धिविनायक क्रोएशिया मौर्य
ऑस्ट्रिया हिंदुस्तान हिंद सातवाहन मुहम्मद मोहम्मद सौराष्ट्र बाहुबली चालुक्य उज्जैन उस्मान चंद्रभागा कन्याकुमारी लेह नॉर्वे
कंबोडिया चाणक्य नेपोलियन इंद्राणी अमेठी पनामा बाल्कन आर्यन बनारस जॉर्जिया मध्यप्रदेश उत्तरप्रदेश तेलंगणा ओडिसा ओरिसा
विराट धोनी कोहली टाटा बजाज महिंद्रा रिलायन्स एअरटेल जिओ गुगल सिडको म्हाडा बीसीसीआय आयसीसी सीबीआय आयपीएल आरबीआय बीएसई
एलबीटी विंडीज इंडीज छत्रपती शिवराय लोकमान्य टिळक शाहू स्वामी पंडित भारती मारुती समर्थ दत्त गौतम गांधीजी श्रीकृष्ण श्रीराम
सह्याद्री सिंधू कंगना अश्विन अजिंक्य सायना दीपिका द्रविड धवन जॉर्ज डेव्हिड जेम्स रिओ मसूद सय्यद हार्दिक महालक्ष्मी संता
अली निपाणी डोंबिवली पनवेल वसई इंडियन नॅशनल इंटरनॅशनल युनायटेड न्यू वेस्ट स्टेट बेस्ट वर्ल्ड सेंट्रल ग्लोबल रॉयल इंडस्ट्रीज
मोटर्स फाऊंडेशन फेडरेशन लिमिटेड ग्रीन हिरो बिग स्पेशल सुपर रेड
""".split())
# Pieces of words cut at a number (१०वी, २०१६च्या), abbreviations, letters and Hindi or English words the corpus has: no words
# for induce, nor are their forms.
FRAGMENTS = set("""
चं चे ची चा च्या वू वी वा वे व्या ला ले अल रू के मा एम ता आय पा दि है इन एन पं स्व को से खा डा एल अन पर मेरे और आदमी बहुत
रा सा ग्रा उदा श्रीमती ऍड ऍप अँड फॉर टू ऑफ द यू जिहे कै शा हू जी बी सी डी आर टी एस ओ जे यांच्याहस्ते
""".split())


def lexicon(dictionaries):
    """The pack's words by kind, its forms tables (form -> words), and the words that are only names."""
    db = sqlite3.connect(os.path.join(dictionaries, "mr-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if " " in word or "-" in word:
            continue
        # A language's name is an everyday word (मराठी); other names are left out.
        if pos == "name" and any("language" in g for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS:
            kinds[word].add(pos)
    for word, kind in MARATHI_WORDS.items():
        kinds[word].add(kind)
    folded = {variant: word for variant, word in SPELLINGS.items() if variant in kinds and word in kinds and variant != word}
    for word in DROPPED:
        kinds.pop(word, None)
    # The pack has some words in two spellings: a neuter's spoken -ं beside its -े (सोनं, सोने), an adjective in its masculine
    # beside the neuter it is cited in (छोटा, छोटे). Each is folded into the -े.
    for word in list(kinds):
        other = word[:-1] + "े"
        if word[-1] == "ं" and word[:-1] in kinds and not vowel_final(word[:-1]):
            folded[word] = word[:-1]
        elif word[-1] == "े" and word[:-1] in kinds and "noun" in kinds[word] & kinds[word[:-1]] and not vowel_final(word[:-1]):
            folded[word] = word[:-1]
        elif word not in folded and other in kinds and (word[-1] == "ं" or word[-1] == "ा" and "adj" in kinds[word] | kinds[other]
                                                       and not kinds[word] & {"noun", "verb"}):
            folded[word] = other
        # So is an adjective in -ा the pack has as a noun (काळा, हिरवा, अर्धा) beside its -े; जागा "place" is no जागे "awake".
        elif (word[-1] == "ा" and other in kinds and "adj" in kinds[other] and kinds[word] <= {"adj", "noun", "adv"}
              and word not in ("जागा", "विसावा")):
            folded[word] = other
    for variant in folded:
        del kinds[variant]
    table = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemma = folded.get(lemma, lemma)
        if (" " in lemma or "-" in lemma or lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS
                or lemma in MARATHI_PRONOUNS or form in MARATHI_FORMS or form in PRONOUN_FORMS):
            continue
        table[form].add(lemma)
    ALSO_NAMES.clear()
    ALSO_NAMES.update(names & set(kinds))
    return kinds, table, names - set(kinds)


def pronoun_forms():
    """Each pronoun's forms: its own, and its oblique stems with the cases and postpositions, with the clitics."""
    out = {}
    for word, (own, obliques) in MARATHI_PRONOUNS.items():
        forms = set(own.split())
        for stem in obliques:
            forms |= {stem + t for t in OBLIQUE_TAILS if not t.startswith(("ला", "ना", "ने", "नं", "नी", "चा", "ची", "चे", "च्या", "चं"))}
        for form in with_clitics(forms):
            out.setdefault(form, word)
    for word in list(MARATHI_PRONOUNS) + list(MARATHI_WORDS):
        out.pop(word, None)
    return out


def vowel_final(word):
    return word[-1] in VOWEL_SIGNS or word[-1] in "अआइईउऊएऐओऔ"


SHORT = {"ी": "ि", "ू": "ु"}


def noun_classes(word):
    """
    A noun's declensions it may follow, by its last letter: each its oblique stems (singular, plural) and direct plurals.
    A consonant-final noun is masculine or neuter (घर: घरा-, घरां-, घरे) or one of the two feminine kinds (वेळ: वेळे-, वेळा;
    गोष्ट: गोष्टी-, गोष्टी); one in -ा masculine (रस्ता: रस्त्या-, रस्ते) or feminine (शाळा: शाळे-, शाळां-); one in -ी feminine
    or masculine (नदी: नदी-, नद्या) or neuter (पाणी: पाण्या-); one in -े neuter (केळे: केळ्या-, केळी).
    """
    last, base = word[-1], word[:-1]
    # A noun in -सा or a masculine in -सी has its स as श in the oblique (आरसा: आरशात, आरश्यात; प्रवासी: प्रवाशांना).
    hushed = base[:-1] + "श" if base.endswith("स") else None
    if last == "ा":
        return [((base + "्या", base + "्यां"), {base + "े"}), ((base + "े", word + "ं"), set()), ((word, word + "ं"), {base + "े"})] + (
            [((hushed + "ा", hushed + "ां"), {base + "े"}), ((hushed + "्या", hushed + "्यां"), {base + "े"})] if hushed else [])
    if last == "ी":
        return [((word, word + "ं"), {base + "्या"}), ((base + "्या", base + "्यां"), set())] + (
            [((hushed + "ा", hushed + "ां"), set())] if hushed else [])
    if last in "ें":
        return [((base + "्या", base + "्यां"), {base + "ी"})]
    # A feminine in -ाई has its plural in -ाया (कारवाई: कारवाया, कारवायांना).
    if word.endswith("ाई"):
        return [((word, word + "ं"), {word[:-1] + "या"}), ((word[:-1] + "या", word[:-1] + "यां"), set())]
    # A neuter in -ू has its oblique in -ा (पिल्लू: पिल्लाला, पिल्ले; लिंबू: लिंबाचा); a masculine keeps the -ू (खेळाडूला).
    if last == "ू":
        return [((word, word + "ं"), set()), ((base + "ा", base + "ां"), {base + "े"})]
    if vowel_final(word):
        return [((word, word + "ं"), set())]
    # The locative in -ी with the first feminine kind (वेळी); a masculine's or neuter's is LOCATIVE_I. A long ी or ू before the
    # last consonant may be short in the oblique (तिकीट: तिकिटा-; जमीन: जमिनी-; पूल: पुला-).
    stems = [word] + ([word[:-2] + SHORT[word[-2]] + word[-1]] if len(word) > 2 and word[-2] in SHORT else [])
    # A feminine in -स has its स as श in the oblique (शिफारस: शिफारशी).
    hushed = [((word[:-1] + "शी", word[:-1] + "शीं"), {word[:-1] + "शी"})] if word[-1] == "स" else []
    return [cls for w in stems for cls in [((w + "ा", w + "ां"), {w + "े", w + "ं"}), ((w + "े", w + "ां"), {w + "ा", w + "ी"}),
                                           ((w + "ी", w + "ीं"), {w + "ी"})]] + hushed


def oblique_count(stem):
    return sum(COUNTS.get(s + t, 0.0) for s in eyelash({stem}) for t in CASES)


def free_count(stem, tails):
    """How often the corpus has [stem] with [tails], but for the spellings that are words of their own or a pronoun's forms."""
    return sum(COUNTS.get(s + t, 0.0) for s in eyelash({stem}) for t in tails
               if s + t not in KINDS and s + t not in MARATHI_FORMS and s + t not in PRONOUN_FORMS)


def stem_of_its_own(stem, word):
    """
    Whether a stem is another word's: a word of its own (आरोपी beside आरोप), another's form (दिले), another noun's oblique
    (किल्ल्या- of किल्ला beside किल्ली, तारे- of तार beside तारा) or a verb's infinitive's (करण्या- of करणे beside करणी).
    """
    if stem != word and (stem in KINDS or stem in MARATHI_FORMS or stem in PRONOUN_FORMS) or STEM_NOUNS.get(stem, {word}) - {word}:
        return True
    infinitive = stem[:stem.rfind("ण्या")] + "णे" if "ण्या" in stem else ""
    return "verb" in KINDS.get(infinitive, ())


def is_verb(word):
    return "verb" in KINDS.get(word, ()) or word in VERBS_BY_HAND


def table_obliques(table):
    """
    A table's oblique stems: its datives' (मुलाला, मुलांना: मुला-, मुलां-) when it has their other cases too (मुलाने), so not
    an oblique that is itself in -ला (गोला of गोल).
    """
    return {f[:-2] for f in table if f.endswith(("ला", "ंना")) and len(f) > 2 and any(f[:-2] + t in table for t in ("ने", "नी", "चा", "त"))}


def nominal_forms(word, table, adjective=False, numeral=False):
    """
    A noun's forms: its table's, its obliques (from its table's datives: घराला is घरा + ला; else of the declensions whose
    case forms the corpus has at least a quarter as often as the commonest's) with the cases and postpositions, and its plurals.
    """
    obliques, forms = set(), set(table)
    obliques |= table_obliques(table)
    if not obliques:
        # An adjective is a noun in the plural (भारतीयांना, श्रीमंतांचे): व्यक्तींना is व्यक्ती's, not व्यक्त's.
        classes = [((word + "ां",), set())] if adjective and not vowel_final(word) else noun_classes(word)
        # A stem that is another noun's own oblique is that noun's: राज्या- is राज्य's, not राजा's; पायां- पाय's, not पाया's.
        classes = [(tuple(stem for stem in stems if PRIMARY_OBLIQUES.get(stem, word) == word), plurals) for stems, plurals in classes]
        # A number declines as it is (दहा: दहाही, दहांना), not as a noun in -ा (दह्या is दही's).
        if numeral and vowel_final(word):
            classes = [((word, word + "ं"), set())]
        scores = [sum(oblique_count(stem) for stem in stems) for stems, _ in classes]
        best = max(scores)
        for (stems, plurals), score in zip(classes, scores):
            if score >= best / 4 and (score > 0 or not best and (stems, plurals) == classes[0]):
                obliques |= set(stems)
                forms |= plurals
    if word in LOCATIVE_I:
        forms.add(word + "ी")
    forms |= obliques
    for stem in obliques:
        forms |= {stem + t for t in OBLIQUE_TAILS}
    return forms


def keeps_table_kind(word, stem, obliques):
    """
    Whether a declension (by its first oblique) may stand for a table's: of a consonant-final noun, a feminine table's no
    masculine (गप्पा- for गप्पी-) and only an -ी table's an -ी one (जखमी "wounded" is no oblique of जखम); not the direct
    oblique of a noun in -ा whose table has -्या when the word without its -ा is a word (गजरा: गजरात is गजर's; दर्जा:
    दर्जाचे is its), nor -्या of a noun in -ी whose plural is -्यां (वाटी).
    """
    if not vowel_final(word):
        feminine = any(o.endswith(("े", "ी", "ीं")) for o in obliques)
        if stem.endswith("ी"):
            return any(o.endswith(("ी", "ीं")) for o in obliques)
        return not (stem.endswith("ा") and feminine)
    if word[-1] == "ा" and stem == word and word[:-1] in KINDS:
        return not any(o.endswith(("्या", "्यां")) for o in obliques)
    if word[-1] == "ी" and stem.endswith("्या"):
        return not any(o.endswith("्यां") for o in obliques)
    return True


def retabled_forms(word, ks, table):
    """
    The forms of a noun whose table has obliques the corpus hardly has (दूध: दधा- for दुधा-; खेळाडू: खेळाडूवा-) by the
    declensions the corpus has four times as often, on no stem that is another word's.
    """
    obliques = table_obliques(table)
    if "noun" not in ks or not obliques:
        return set()
    classes = [(stems, plurals) for stems, plurals in noun_classes(word) if keeps_table_kind(word, stems[0], obliques)
               and not (stems[0] == word and word in ALSO_NAMES)
               and not any(PRIMARY_OBLIQUES.get(stem, word) != word or stem_of_its_own(stem, word) for stem in stems)
               and not any(p != word and p in KINDS for p in plurals)] or [((), set())]
    scores = [sum(free_count(stem, CASES) for stem in stems) for stems, _ in classes]
    best = max(scores)
    forms = set()
    if best > 4 * sum(free_count(stem, CASES) for stem in obliques):
        for (stems, plurals), score in zip(classes, scores):
            if score >= best / 4:
                forms |= set(stems) | plurals | {stem + t for stem in stems for t in OBLIQUE_TAILS}
        return forms
    # Or only its plural's (व्यक्ती: व्यक्त्यां- for व्यक्तीं-).
    plural = [free_count(stems[1], CASES) if len(stems) > 1 else 0.0 for stems, _ in classes]
    top = max(plural)
    if top > 4 * sum(free_count(stem, CASES) for stem in obliques if stem.endswith("ं")):
        for (stems, _), score in zip(classes, plural):
            if score > 0 and score >= top / 4:
                forms |= {stems[1]} | {stems[1] + t for t in OBLIQUE_TAILS}
    return forms


def direct_forms(word, ks, table):
    """
    A noun's cases and postpositions on its direct form (सरकारने, रेल्वेत, सकाळचा, बाहेरच्या), when the corpus has them at
    least a quarter as often as on its obliques (not घरने beside घराने), and an adverb's genitive (सध्याचा, आतापर्यंत).
    """
    if word in ALSO_NAMES:
        return set()
    if ks & NOMINAL_KINDS and (not vowel_final(word) or word[-1] == "े"):
        tails = DIRECT_TAILS if vowel_final(word) else CONSONANT_DIRECT_TAILS
        # A verb's stem, or one the corpus has with a past's other endings too (मेला, मेली of मरणे; मधला, मधले "middle").
        if is_verb(word + "णे") or word + "ली" in COUNTS or word + "ले" in COUNTS:
            tails = [t for t in tails if t not in VERB_LIKE_TAILS]
        rivals = [sum(oblique_count(stem) for stem in stems) for stems, _ in noun_classes(word)]
        rivals.append(sum(oblique_count(stem) for stem in table_obliques(table)))
        score = free_count(word, [t for t in tails if t in CASES])
        tails = tails if score > 0 and score >= max(rivals) / 4 else []
    elif "adv" in ks:
        tails = ADVERB_TAILS
    else:
        return set()
    # Not a word said twice (जवळजवळ "almost").
    return {word + t for t in tails if t != word}


def adjective_forms(word):
    """An adjective in -ा or -े agrees (चांगला, चांगली, चांगले, चांगल्या, चांगलं) and takes the cases on its oblique."""
    if word[-1] not in "ाे":
        return set()
    base = word[:-1]
    forms = {base + "ा", base + "ी", base + "े", base + "्या", base + "ं", base + "्यां"}
    stems = [base + "्या", base + "्यां"]
    # One in -सा has its स as श before -ी and -ा (पुरेसा: पुरेशी, पुरेशा; फारसा: फारशा).
    if base.endswith("स"):
        forms |= {base[:-1] + "शी", base[:-1] + "शा"}
        stems.append(base[:-1] + "शा")
    for stem in stems:
        forms |= {stem + t for t in OBLIQUE_TAILS}
    return forms


def verb_forms(word, table):
    """A verb's forms: its table's, and by rule its present, future, habitual, infinitive's and past's from its stem."""
    forms = set(table)
    if not word.endswith("णे"):
        return forms
    stem = word[:-2]
    forms |= {stem + t for t in PRESENT_TAILS}
    if vowel_final(stem):
        forms |= {stem + t for t in VOWEL_FUTURE_TAILS}
    else:
        forms |= {stem + t for t in FUTURE_TAILS}
    if word in VOWEL_STEMS:
        forms |= {VOWEL_STEMS[word] + t for t in DESIDERATIVE_TAILS}
    # The -ाय stem the table's desiderative shows (व्हायला: व्हा-; यायला: या-).
    for form in table:
        if form.endswith("ायला"):
            forms |= {form[:-3] + t for t in ("यचा", "यची", "यचे", "यच्या", "यचं", "वा", "वी", "वे", "व्या", "वं", "वेत", "वीत")}
    infinitive = stem + "ण"
    forms |= {infinitive + t[1:] if t.startswith("ण") else stem + t for t in INFINITIVE_TAILS}
    forms |= {infinitive + "्या" + t for t in INFINITIVE_OBLIQUE_TAILS + OBLIQUE_TAILS} | {infinitive + "्या"}
    pasts = set(PAST_STEMS.get(word, []))
    # The table's masculine past (केला, लागला) gives the stem; without one, the stem + ल (बसला), + िल (राहिला) or + ाल (उडाला).
    pasts |= {f[:-1] for f in table if f.endswith("ला") and f[:-1] + "ी" in table}
    if not pasts:
        pasts = {stem + "ल"} if vowel_final(stem) else {stem + "ल", stem + "िल", stem + "ितल", stem + "ाल"}
    for p in pasts:
        forms |= {p + t for t in PAST_TAILS}
    return forms


NOMINAL_KINDS = {"noun", "num", "det", "postp"}


def word_forms(word, ks, table_of):
    table = table_of.get(word, set())
    forms = set(table)
    if ks & NOMINAL_KINDS or "adj" in ks and not vowel_final(word):
        forms |= nominal_forms(word, table, adjective=not ks & NOMINAL_KINDS, numeral="num" in ks and not ks & {"noun"})
    if ks & {"adj", "pron", "adv", "num"} and len(word) > 2:
        forms |= adjective_forms(word)
    if "verb" in ks:
        forms |= verb_forms(word, table)
    return eyelash(forms)


EYELASH = re.compile("(?<!\u094d)\u0930\u094d(?=[\u092f\u0939])")


def eyelash(forms):
    """The forms with र before य or ह also written as the eyelash ra, as Marathi has it (दुसऱ्या, करणाऱ्या), and the other way
    round, as texts have it too (करणार्या)."""
    return set(forms) | {EYELASH.sub("\u0931\u094d", f) for f in forms} | {f.replace("\u0931\u094d", "\u0930\u094d") for f in forms}


def with_clitics(forms):
    """The forms with the clitics -च, -ही and -सुद्धा."""
    return set(forms) | {f + c for f in forms for c in CLITICS}


def unclitic(form):
    """The form without the clitic it may end in (घरीच, मीही)."""
    for c in CLITICS:
        if form.endswith(c) and len(form) > len(c) + 1:
            return {form[:-len(c)]}
    return set()


def tail_cost(word, form):
    """How far [form] is from [word]: none, a case or ending (one), or a postposition on it (two)."""
    if form == word:
        return 0
    if not form.startswith(word[:-1] if vowel_final(word) else word):
        return 1
    tail = form[len(word):]
    return 2 if any(tail.endswith(p) for p in POSTPOSITIONS if len(p) > 2) else 1


PARTICIPLE = re.compile("(?:लेल|णार|णाऱ)(?:ा|ी|े|्या|ं)$")
PRONOUN_FORMS = {}
COUNTS = {}
KINDS = {}
# The pack's words that it has as names too (अक्षय, सागर): a name takes the cases on its direct form (अक्षयने), so those
# forms are the name's, not the word's.
ALSO_NAMES = set()
# The verbs MARATHI_FORMS gives forms of, the pack has or not (नसणे).
VERBS_BY_HAND = {word for word in MARATHI_FORMS.values() if word.endswith("णे")}
# Each oblique stem a noun's table or rules give: stem -> nouns.
STEM_NOUNS = {}
PRIMARY_OBLIQUES = {}


def primary_obliques(kinds, table_of):
    """Each noun's own oblique stems, the ones its table or its ending leave no doubt about: stem -> noun."""
    out = {}
    for word, ks in kinds.items():
        if "noun" not in ks:
            continue
        stems = table_obliques(table_of.get(word, ()))
        if not stems and not vowel_final(word):
            stems = {word + "ा", word + "ां"}
        for stem in stems:
            out[stem] = word if stem not in out else None
    return {stem: word for stem, word in out.items() if word}


# A word the corpus has and the pack lacks (induce): a noun (or adjective) when its forms are written INDUCED_BARE times (of
# the corpus's words) and itself INDUCED_SEEN times, or INDUCED_LONE times when the corpus has it in no other form; a verb
# when INDUCED_FORMS of its telling forms by rule (VERB_TELLING: an infinitive's, a participle's, a conjunctive's, which no
# noun has) are written INDUCED_VERB times in all.
INDUCED_BARE = 1e-6
INDUCED_SEEN = 2e-7
INDUCED_LONE = 4e-6
INDUCED_FORMS = 3
INDUCED_VERB = 2e-6
VERB_TELLING = ("णे", "णं", "णार", "णारा", "णारी", "णारे", "णाऱ्या", "णाऱ्यांना", "ण्यात", "ण्याची", "ण्याचा", "ण्याचे", "ण्याच्या",
                "ण्यासाठी", "ण्यास", "ण्याने", "ण्याला", "ण्यापूर्वी", "ायला", "ायचे", "ायचा", "ायची", "ायच्या", "ावे", "ावा", "ावी",
                "ावेत", "ून", "ूनही", "ताना", "तात", "लेल्या", "लेले", "लेला", "लेली", "ल्याने", "ल्यानंतर", "ल्यावर", "ल्यास",
                "ल्याचे", "ल्याची", "ल्याचा", "ल्यामुळे")
# A form ending so is a verb's, no noun's.
NOT_NOUN = tuple(t for t in VERB_TELLING if t.startswith(("ण", "ायला", "ल्या", "लेल", "ताना")) and len(t) > 2)
# A word of the corpus that the sentences have before the honorific यांनी, यांच्या (पाटील यांनी) NAME_HONOURED of the times,
# before such a name NAME_FIRST of the times (नरेंद्र मोदी), or before a word of place (PLACE_AFTER: सातारा येथे, सांगली
# जिल्ह्यातील) NAME_PLACE of the times, is a name; of the words written NAME_SEEN times.
NAME_SEEN = 30
NAME_HONOURED = 0.2
NAME_FIRST = 0.25
NAME_PLACE = 0.2
HONORIFIC = ("यां", "ह्यां")
PLACE_AFTER = {"येथे", "येथील", "येथून", "इथे", "इथल्या", "जिल्ह्यातील", "जिल्ह्यात", "जिल्ह्याच्या", "जिल्हा", "तालुक्यातील",
               "तालुक्यात", "तालुका", "शहरात", "शहरातील", "शहर", "गावात", "गावातील", "राज्यात", "राज्यातील", "राज्याच्या", "देशात"}
# Words the corpus shows that are others' spellings: induce files their forms under these.
INDUCED_SPELLINGS = {"पहणे": "पाहणे", "पहाणे": "पाहणे", "रहणे": "राहणे", "रहाणे": "राहणे", "लिहणे": "लिहिणे"}


def sentences(corpora):
    """The words of each sentence of the Leipzig archives [corpora], folded (marathi_fold)."""
    folded = {}
    for path in corpora:
        if not path:
            continue
        with tarfile.open(path) as tar:
            member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
            for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
                words = []
                for w in LETTERS.findall(unicodedata.normalize("NFC", line.partition("\t")[2])):
                    f = folded.get(w)
                    if f is None:
                        f = folded[w] = marathi_fold(w)
                    words.append(f)
                yield words


def corpus_names(corpora, wanted):
    """Of the words [wanted], those the sentences show as names (NAME_HONOURED, NAME_FIRST)."""
    seen, honoured, before = Counter(), Counter(), defaultdict(Counter)
    for words in sentences(corpora):
        seen.update(words)
        for i in range(len(words) - 1):
            word, after = words[i], words[i + 1]
            if after.startswith(HONORIFIC):
                honoured[word] += 1
            if word in wanted:
                before[word][after] += 1
    honoured_names = {w for w, n in honoured.items() if seen[w] >= NAME_SEEN and n >= NAME_HONOURED * seen[w]}
    out = set()
    for word in wanted:
        if seen[word] < NAME_SEEN:
            continue
        if (word in honoured_names or sum(n for w, n in before[word].items() if w in honoured_names) >= NAME_FIRST * seen[word]
                or sum(n for w, n in before[word].items() if w in PLACE_AFTER) >= NAME_PLACE * seen[word]):
            out.add(word)
    return out


def name_forms(name):
    """A name's forms: its stems (itself, its oblique: पुण्यात of पुणे, and its honorific plural: रामभाऊंनी, रावांच्या) with
    the cases, postpositions and clitics."""
    stems = {name, name + ("ं" if vowel_final(name) else "ां")}
    if name[-1] == "ा":
        stems |= {name[:-1] + "े", name[:-1] + "्या"}
    elif name[-1] == "े":
        stems.add(name[:-1] + "्या")
    elif not vowel_final(name):
        stems.add(name + "ा")
    return with_clitics(stems | {stem + t for stem in stems for t in OBLIQUE_TAILS})


def slipped(form):
    """[form] with its long and short i and u alike (करुन, करून; सुरु, सुरू), a य after a consonant with or without the virama
    (मोठया, मोठ्या) and the old ध्द as द्ध (पध्दत, पद्धत)."""
    return form.replace("ी", "ि").replace("ू", "ु").replace("\u094dय", "य").replace("ध्द", "द्ध")


def telling_tail(tail):
    """Whether a verb's ending, after its past's -ा or -ि (उडाल्याने, पाठविलेल्या), tells a verb (VERB_TELLING)."""
    return tail.startswith(VERB_TELLING) or tail[:1] in ("ा", "ि") and tail[1:].startswith(VERB_TELLING)


def induce(unexplained, claimers):
    """
    The words the corpus's forms no word of the pack explains ([unexplained]: form -> count) are of: word -> its forms. A
    name's forms ([claimers]) are its, and no word's. Each form goes to one word, the one with the most forms' count of those
    left: a noun's forms are its nominal_forms and direct_forms with the clitics, a verb's its verb_forms.
    """
    claimed = (FRAGMENTS | NOT_WORDS) & set(unexplained)
    # A name's forms, but for those the corpus has more often than the name (आढावा "review", not आढाव's).
    for name in claimers - NOT_WORDS:
        claimed |= {f for f in name_forms(name) if f in unexplained and (f == name or unexplained[f] <= COUNTS.get(name, 0))}
    left = {f: n for f, n in unexplained.items() if f not in claimed}
    # The forms spelled alike but for slips (slipped), as one, under the spelling written most often (मुस्लीम, मुस्लिम).
    alike = defaultdict(set)
    for form in left:
        alike[slipped(form)].add(form)

    def spelled(forms):
        return {f for form in forms for f in alike.get(slipped(form), ())}

    def commonest(form):
        return max(alike[slipped(form)], key=lambda f: (left[f], f)) == form

    paradigms, verbs, tried = {}, set(), set()
    for form, n in left.items():
        for tail in VERB_TELLING:
            stem = form[:-len(tail)]
            # The verbs the pack lacks have a consonant stem; a past on -ाल is the stem's (उडाल्याने of उडणे).
            if tail.startswith("ल") and stem.endswith("ा") and len(stem) > 2 and not vowel_final(stem[:-1]):
                stem = stem[:-1]
            if not form.endswith(tail) or len(stem) < 2 or vowel_final(stem) or stem[-1] == "्" or stem + "णे" in tried:
                continue
            word = stem + "णे"
            tried.add(word)
            # Not its bare stem + -ा, -ू, -ं (खोका "box" is no imperative of खोकणे).
            made = {f for f in verb_forms(word, set()) if f[len(stem):] not in ("ा", "ू", "ं")}
            made = spelled(f for f in with_clitics(eyelash(made)) if f in left)
            telling = {f for f in made if telling_tail(f[len(stem):])}
            if len(telling) >= INDUCED_FORMS and math.fsum(left[f] for f in telling) >= INDUCED_VERB:
                paradigms[word] = made
                verbs.add(word)
    for form, n in left.items():
        if n < INDUCED_SEEN or len(form) < 2 or form.endswith(NOT_NOUN) or form in paradigms or not commonest(form):
            continue
        # A consonant-final word the corpus has less often than the same in -ा, or a third as often as in -ी, is that one's
        # (मालिक, मालिका; विक्र, विक्री).
        if not vowel_final(form) and (left.get(form + "ा", 0) > n or left.get(form + "ी", 0) > 3 * n):
            continue
        # A word in -े is the plural or the neuter of one in -ा the corpus has too (रस्ते, मोठे): that one stands for it.
        if form[-1] == "े" and left.get(form[:-1] + "ा", 0) >= INDUCED_BARE:
            continue
        ks = {"noun", "adj"} if form[-1] in "ाे" else {"noun"}
        made = word_forms(form, ks, {}) | {form}
        tails = direct_forms(form, ks, set())
        if form + "णे" in verbs:
            tails = {f for f in tails if not f[len(form):].startswith(VERB_LIKE_TAILS)}
        paradigms[form] = spelled(f for f in with_clitics(made | eyelash(tails)) if f in left)
    # Lazy greedy: a word's count only falls as others take its forms, so one popped with its count unchanged is the best left.
    chosen = {}
    heap = [(-math.fsum(left[f] for f in forms), word) for word, forms in paradigms.items()]
    heapq.heapify(heap)
    while heap:
        negative, word = heapq.heappop(heap)
        forms = paradigms[word] - claimed
        mass = math.fsum(left[f] for f in forms)
        if mass < -negative:
            paradigms[word] = forms
            if mass > 0:
                heapq.heappush(heap, (-mass, word))
            continue
        if word in verbs:
            stem = word[:-2]
            telling = {f for f in forms if telling_tail(f[len(stem):])}
            if len(telling) < INDUCED_FORMS or math.fsum(left[f] for f in telling) < INDUCED_VERB:
                continue
        elif word in claimed or mass < INDUCED_BARE or forms <= {word} and left[word] < INDUCED_LONE:
            continue
        claimed |= forms | {word}
        # An adjective in -ा is cited in its -े, as the pack has them (मोठे): one whose -ी and -े the corpus has a fourth as often
        # as its -ा, and its -े's cases (a feminine's oblique: शाळेत) less often than its -ी.
        base = word[:-1]
        if (word[-1] == "ा" and word not in verbs and left.get(base + "ी", 0) >= left.get(word, 0) / 4
                and left.get(base + "े", 0) >= left.get(word, 0) / 4
                and math.fsum(left.get(base + "े" + t, 0) for t in CASES) < left.get(base + "ी", 0)):
            word = base + "े"
        # One the corpus has in -ी three times as often as in -ा and -े is an adjective in -ी (नागरी, पाकिस्तानी).
        if word[-1] in "ाे" and word not in verbs and left.get(base + "ी", 0) > 3 * (left.get(base + "ा", 0) + left.get(base + "े", 0)):
            word = base + "ी"
        # A verb in -विणे is its -वणे's (पाठविणे, पाठवणे), and a few are others' spellings (INDUCED_SPELLINGS).
        if word in verbs and word.endswith("विणे"):
            word = word[:-4] + "वणे"
        word = INDUCED_SPELLINGS.get(word, word)
        # A noun's plural the pack's table lacks is the noun's (लढे of लढा, नद्या of नदी).
        if word not in verbs:
            word = next((w for w in (word[:-1] + "ा" if word[-1] == "े" else "", word[:-3] + "ी" if word.endswith("्या") else "")
                         if "noun" in KINDS.get(w, ())), word)
        chosen.setdefault(word, set()).update(forms)
    return chosen


def build(found, size, label, corpora=()):
    """The list: the pack's words and those the corpus shows (induce), by their forms' counts in [found]."""
    dictionaries = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
    PRONOUN_FORMS.clear()
    PRONOUN_FORMS.update(pronoun_forms())
    kinds, table, names = lexicon(dictionaries)
    counts = defaultdict(float)
    weights = {name: SOURCE_WEIGHTS.get(name, 1.0) for name, _, _ in found}
    whole_weight = sum(weights.values())
    for name, source, _ in found:
        total = sum(source.values())
        for form, n in source.items():
            form = marathi_fold(form)
            if LETTERS.fullmatch(form) and not JOINER.search(form):
                counts[form] += n / total * weights[name] / whole_weight

    COUNTS.clear()
    COUNTS.update(counts)
    KINDS.clear()
    KINDS.update(kinds)
    table_of = defaultdict(set)
    for form, lemmas in table.items():
        for lemma in lemmas:
            table_of[lemma].add(form)
    PRIMARY_OBLIQUES.clear()
    PRIMARY_OBLIQUES.update(primary_obliques(kinds, table_of))
    STEM_NOUNS.clear()
    for word, ks in kinds.items():
        if "noun" in ks:
            for stem in table_obliques(table_of.get(word, ())) | {s for stems, _ in noun_classes(word) for s in stems}:
                STEM_NOUNS.setdefault(stem, set()).add(word)
    # A verb's forms (its table's and the rules') and a noun's, adjective's or pronoun's, apart: form -> words.
    verbal, nominal = defaultdict(set), defaultdict(set)
    # A noun's forms on its direct form (direct_forms) and on the obliques its table lacks (retabled_forms): its only when no
    # other word has the form.
    direct = defaultdict(set)
    # Of a verb's forms, the ones its table has and its participles (बिघडलेले, धावणारा): the only ones a word of its own
    # spelled the same may be (केलं "banana" is करणे's केलं; खाते "account" is no form of खाणे, nor देवी "goddess" of देणे).
    attested = defaultdict(set)

    def add(word, as_word, ks):
        made = word_forms(word, ks, table_of if word == as_word else {})
        verb_made = eyelash(verb_forms(word, table_of.get(word, set()) if word == as_word else set())) if "verb" in ks else set()
        for form in with_clitics(made | {word}):
            if form in counts and form != as_word:
                if form in verb_made or (form[:-1] in verb_made and form.endswith(("च", "ी"))):
                    verbal[form].add(as_word)
                    if form in table_of.get(as_word, ()) or PARTICIPLE.search(form):
                        attested[form].add(as_word)
                else:
                    nominal[form].add(as_word)
        made = with_clitics(made | {word})
        own_table = table_of.get(word, set()) if word == as_word else set()
        for form in with_clitics(eyelash(direct_forms(word, ks, own_table) | retabled_forms(word, ks, own_table))):
            if form in counts and form != as_word and form not in made:
                direct[form].add(as_word)

    for word, ks in kinds.items():
        add(word, word, ks)
    for variant, word in SPELLINGS.items():
        if word in kinds and variant != word:
            add(variant, word, kinds[word])
            if variant in counts:
                nominal[variant].add(word)

    def candidates(form):
        """(the word the form is itself, or None; the other words it is a form of)."""
        if form in MARATHI_FORMS:
            return None, {MARATHI_FORMS[form]}
        if form in PRONOUN_FORMS:
            return None, {PRONOUN_FORMS[form]}
        if form in NOT_WORDS or form in names and not table.get(form):
            return None, set()
        own = form if form in kinds else None
        if own in KEEP:
            return own, set()
        for base in unclitic(form) if not own else ():
            if base in MARATHI_FORMS:
                return None, {MARATHI_FORMS[base]}
        verbs = verbal.get(form, set()) - {form}
        if own:
            return own, attested.get(form, set()) - {form}
        nouns = nominal.get(form, set()) - {form}
        if nouns:
            fewest = min(tail_cost(w, form) for w in nouns)
            nouns = {w for w in nouns if tail_cost(w, form) == fewest}
            # Of those, the longest (मुलीला is मुलगी's, not मुल's).
            longest = max(len(w) for w in nouns)
            nouns = {w for w in nouns if len(w) == longest}
            # A verb's form is no short noun with a postposition on it.
            if verbs and fewest == 2:
                nouns = set()
        others = verbs | nouns
        if not others:
            for base in unclitic(form):
                if base in kinds:
                    return None, {base}
            others = direct.get(form, set())
            if others:
                longest = max(len(w) for w in others)
                others = {w for w in others if len(w) == longest}
        return own, others

    found_heads = {form: candidates(form) for form in counts}
    # A form no word explains is a slip of the form spelled with the other i or u (करुन of करून, सुरु of सुरू) that is
    # written at least as often, and goes where that goes.
    spelled = defaultdict(list)
    for form, (own, others) in found_heads.items():
        if own or others:
            spelled[slipped(form)].append(form)
    for form, (own, others) in list(found_heads.items()):
        if not own and not others and form not in NOT_WORDS and form not in names:
            right = [f for f in spelled.get(slipped(form), ()) if counts[f] >= counts[form]]
            heads = {h for f in right for h in found_heads[f][1] or {found_heads[f][0]}}
            if heads:
                found_heads[form] = (None, heads)
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, others) in found_heads.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]
    scores = defaultdict(float)
    forms = defaultdict(dict)
    unexplained = {}
    for form, share in counts.items():
        own, others = found_heads[form]
        if own and others and max(alone.get(o, 0.0) for o in others) <= share:
            heads = {own}
        else:
            heads = others or ({own} if own else set())
        if not heads:
            unexplained[form] = share
            continue
        weights_here = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights_here.values())
        for head in heads:
            part = weights_here[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    wanted = {f for f, n in unexplained.items() if n >= INDUCED_BARE}
    claimers = names | NOT_WORDS | INDUCED_NOT_WORDS | corpus_names(corpora, wanted)
    for word, made in induce(unexplained, claimers).items():
        for form in sorted(made):
            scores[word] += counts[form]
            forms[word][form] = forms[word].get(form, 0.0) + counts[form]

    ranked = sorted(scores, key=lambda w: (-scores[w], w))[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
