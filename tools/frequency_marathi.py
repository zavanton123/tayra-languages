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

The news corpus of 2016 was converted from an old font: it writes the eyelash ra as ऱ with no virama (दुसऱया) and the
conjunct -्य as य़ (मोठय़ा); such spellings are read as the ones texts and the reader have (marathi_fold).
"""
import json
import os
import re
import sqlite3
import unicodedata
from collections import defaultdict

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
    form = re.sub(f"(?<=[{CONSONANTS}])\u092f\u093c", "\u094d\u092f", form)
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
                 "निमित्त", "निमित्ताने", "आत", "बाहेर", "मार्फत", "पोटी", "लाही", "लाच", "चाच", "चीच", "चेच", "च्याच"]
OBLIQUE_TAILS = CASES + POSTPOSITIONS
CLITICS = ["च", "ही", "सुद्धा"]

# A verb's present stem's endings (करतो, करते, करतात, करत, करताना), its future's (करेन, करशील, करेल, करू, कराल, करतील), and
# the endings of the stem before -ाय (करायचा, करायला) and the infinitive's oblique (करण्यासाठी).
PRESENT_TAILS = ["त", "तो", "ते", "ता", "तात", "तोस", "तेस", "तं", "तोय", "तेय", "ताय", "तायत", "ताहेत", "ताना", "तील", "तेत",
                 "तच", "तोच", "तेच", "ताच", "त्या", "ती"]
FUTURE_TAILS = ["ेन", "ीन", "शील", "ेल", "ू", "ाल", "तील", "ेल", "ूया", "ूयात", "ा", "ावा", "ावी", "ावे", "ाव्या", "ावं", "ावेत",
                "ावीत", "ावेसे", "ावेसा", "ावेसं", "ावेसेच", "ायचा", "ायची", "ायचे", "ायच्या", "ायचं", "ायचो", "ायचास", "ायला",
                "ायलाच", "ायलाही", "ावयाचे", "ावयाचा", "ावयाची", "ावयाच्या", "ून", "ूनही", "ूनच", "ूनसुद्धा", "ू नका"]
VOWEL_FUTURE_TAILS = ["ईन", "शील", "ईल", "ऊ", "ल", "तील", "ऊया", "वा", "वी", "वे", "व्या", "वं", "वेत", "वीत", "यचा", "यची", "यचे",
                      "यच्या", "यचं", "यचो", "यला", "यलाच", "यलाही", "ऊन", "ऊनही", "ऊनच"]
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
    "पाहीजे": "पाहिजे", "बध्दल": "बद्दल", "महत्व": "महत्त्व", "कसा": "कसे",
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
}
# The masculine and neuter nouns with a locative in -ी (घरी "at home", गावी, वर्षी "in the year").
LOCATIVE_I = {"घर", "गाव", "वर्ष", "साल", "ठिकाण", "प्रकरण", "मार्ग", "स्थान", "हात", "शेत", "देव", "जाग", "दार", "पाठ", "मन", "काम"}
# Pack words of rare senses whose spelling is a commoner word's form: वेळा "large cardamoms" (वेळ's plural), ज्या "bowstring"
# (जो's oblique).
DROPPED = {"वेळा", "ज्या"}
# Pasts of verbs the rules make wrongly: the past stem (for PAST_TAILS) by verb.
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
NOT_WORDS = {"डॉ", "श्री", "सौ", "कु", "प्रा", "ना", "रु", "क्र", "स", "न", "क", "द", "ए", "बी", "सी", "डी", "जी", "ऑफ", "एस", "आर", "टी"}
# Pack words the news has as names far more than as words (पाटील "village headman", शरद "autumn", अनिल "wind").
NOT_WORDS |= {"पाटील", "शरद", "देसाई", "अनिल", "शहा", "प्रशांत", "कृष्ण", "कृष्णा"}


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
    if last == "ा":
        return [((base + "्या", base + "्यां"), {base + "े"}), ((base + "े", word + "ं"), set()), ((word, word + "ं"), {base + "े"})]
    if last == "ी":
        return [((word, word + "ं"), {base + "्या"}), ((base + "्या", base + "्यां"), set())]
    if last in "ें":
        return [((base + "्या", base + "्यां"), {base + "ी"})]
    if vowel_final(word):
        return [((word, word + "ं"), set())]
    # The locative in -ी with the first feminine kind (वेळी); a masculine's or neuter's is LOCATIVE_I. A long ी or ू before the
    # last consonant may be short in the oblique (तिकीट: तिकिटा-; जमीन: जमिनी-; पूल: पुला-).
    stems = [word] + ([word[:-2] + SHORT[word[-2]] + word[-1]] if len(word) > 2 and word[-2] in SHORT else [])
    return [cls for w in stems for cls in [((w + "ा", w + "ां"), {w + "े", w + "ं"}), ((w + "े", w + "ां"), {w + "ा", w + "ी"}),
                                           ((w + "ी", w + "ीं"), {w + "ी"})]]


def oblique_count(stem):
    return sum(COUNTS.get(stem + t, 0.0) for t in CASES)


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


def adjective_forms(word):
    """An adjective in -ा or -े agrees (चांगला, चांगली, चांगले, चांगल्या, चांगलं) and takes the cases on its oblique."""
    if word[-1] not in "ाे":
        return set()
    base = word[:-1]
    forms = {base + "ा", base + "ी", base + "े", base + "्या", base + "ं", base + "्यां"}
    for stem in (base + "्या", base + "्यां"):
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
    # The -ाय stem the table's desiderative shows (व्हायला: व्हा-; यायला: या-).
    for form in table:
        if form.endswith("ायला"):
            forms |= {form[:-3] + t for t in ("यचा", "यची", "यचे", "यच्या", "यचं", "वा", "वी", "वे", "व्या", "वं", "वेत", "वीत")}
    infinitive = stem + "ण"
    forms |= {infinitive + t[1:] if t.startswith("ण") else stem + t for t in INFINITIVE_TAILS}
    forms |= {infinitive + "्या" + t for t in INFINITIVE_OBLIQUE_TAILS} | {infinitive + "्या"}
    pasts = set(PAST_STEMS.get(word, []))
    # The table's masculine past (केला, लागला) gives the stem; without one, the stem + ल (बसला) or + िल (राहिला).
    pasts |= {f[:-1] for f in table if f.endswith("ला") and f[:-1] + "ी" in table}
    if not pasts:
        pasts = {stem + "ल"} if vowel_final(stem) else {stem + "ल", stem + "िल", stem + "ितल"}
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
    """The forms with र before य or ह also written as the eyelash ra, as Marathi has it (दुसऱ्या, करणाऱ्या)."""
    return set(forms) | {EYELASH.sub("\u0931\u094d", f) for f in forms}


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


def build(found, size, label):
    """The list: the pack's words, by their forms' counts in [found]."""
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
    table_of = defaultdict(set)
    for form, lemmas in table.items():
        for lemma in lemmas:
            table_of[lemma].add(form)
    PRIMARY_OBLIQUES.clear()
    PRIMARY_OBLIQUES.update(primary_obliques(kinds, table_of))
    # A verb's forms (its table's and the rules') and a noun's, adjective's or pronoun's, apart: form -> words.
    verbal, nominal = defaultdict(set), defaultdict(set)
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
        return own, others

    found_heads = {form: candidates(form) for form in counts}
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, others) in found_heads.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]
    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, share in counts.items():
        own, others = found_heads[form]
        if own and others and max(alone.get(o, 0.0) for o in others) <= share:
            heads = {own}
        else:
            heads = others or ({own} if own else set())
        if not heads:
            continue
        weights_here = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights_here.values())
        for head in heads:
            part = weights_here[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    ranked = sorted(scores, key=lambda w: (-scores[w], w))[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
