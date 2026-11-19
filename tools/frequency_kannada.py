"""
The Kannada frequency list (build_frequency_list.py --language kn).

Kannada writes a word with its case endings, plural, clitics and verb endings joined on (ಮನೆಯಲ್ಲಿ, ಮಾಡುತ್ತಾರೆ), with sounds
between them (ಮನೆ + ಅನ್ನು is ಮನೆಯನ್ನು, ಮರ + ಅನ್ನು is ಮರವನ್ನು), and the packs have no forms tables worth the name, nor does
simplemma know Kannada. So each word of the lexicon (the kn-en and kn-ru packs' words, the Kannada words the en-kn and ru-kn
gloss packs give for English and Russian ones, and KANNADA_WORDS) is declined or conjugated here by its ending, and a corpus
form is counted for the words it is a form of. A form that is a word of the lexicon itself keeps its count (ಆದರೆ "but",
not ಆಗು's "if it becomes"), unless KANNADA_FORMS files it.
"""
import bisect
import functools
import heapq
import json
import os
import re
import sqlite3
from collections import defaultdict

# As the reader's Kannada wordCharacters, without the Kannada digits (numbers are no words).
KANNADA_LETTERS = re.compile(r"[ಀ-೥೰-೿‌‍]+")
# A joiner that follows no virama is a typing slip (ಮ‌ೂಲಕ).
STRAY_JOINER = re.compile("(?<!್)[\u200c\u200d]")
CONSONANTS = {chr(c) for c in range(0x0C95, 0x0CBA)}
VIRAMA = "್"
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep"}
# The kinds of the gloss packs' English and Russian words, as the Kannada word's.
GLOSS_KINDS = {"noun": "noun", "verb": "verb", "adj": "adj", "adv": "adv", "num": "num"}
# A word must be counted this often (all its forms) to be listed: below it the corpus's typos and one-off spellings.
LEAST_COUNT = 5
FORMS_LISTED = 400
# A masculine (ಮಗನು) or human plural (ನಟರು) declension is taken for a noun in -a only when the corpus has this many of its
# telling forms: ಜನನ ("birth") is no ಜನ + ನ.
TELLING_FORMS = 2

# Tails joined to a stem that ends in a consonant with its inherent a (ಮಗನ, ನೀರ of ನೀರು, ಮರವ of ಮರ + glide).
NOMINAL_TAILS = ["ು", "ನ್ನು", "ೂ", "ೇ", "ಾ", "ನ್ನೂ", "ನ್ನೇ", "ೊಂದಿಗೆ", "ಾಗಿ", "ಾದ", "ಾಗಿದೆ", "ಾಗಿದ್ದ", "ಾಗಿದ್ದರು", "ಾಗಿದ್ದಾರೆ",
                 "ಾಗಿತ್ತು", "ಾಯಿತು", "ಾಗುತ್ತದೆ", "ಾಗಿರುವ", "ಾಗುವ", "ಾಗಲು", "ಾಗಿಯೂ", "ಾಗಿಯೇ", "ಿಲ್ಲ", "ಿಲ್ಲದ", "ಿಲ್ಲದೆ", "ಿದೆ",
                 "ಿವೆ", "ಿತ್ತು", "ಿದ್ದ", "ಿದ್ದರು", "ಿದ್ದಾರೆ", "ಿರುವ", "ಾಗಿದ್ದನು", "ಾಗಿದ್ದಳು", "ಾದರು", "ಾದನು", "ಾದಳು"]
# And those joined to a stem that takes the dative -ಇಗೆ and genitive -ಇನ (ನೀರಿಗೆ, ನೀರಿನ; ಮಗನಿಗೆ).
OBLIQUE_TAILS = ["", "ಿಗೆ", "ಲ್ಲಿ", "ಿಂದ", "ಿಗೂ", "ಿಗೇ", "ಲ್ಲೂ", "ಲ್ಲೇ", "ಿಗಾಗಿ", "ಂತೆ", "ಿಂದಲೂ", "ಿಂದಲೇ", "ಲ್ಲಿಯೂ", "ಲ್ಲಿಯೇ", "ಲ್ಲಿದೆ",
                 "ಲ್ಲಿದ್ದ", "ಲ್ಲಿರುವ", "ಿಗಿಂತ"]
# The plural's oblique ಗಳ takes both (ಗಳನ್ನು, ಗಳಿಗೆ, ಗಳ, ಗಳಲ್ಲಿ).
PLURAL_TAILS = NOMINAL_TAILS + OBLIQUE_TAILS
# A noun in -u's genitive and its cases (ನೀರಿನ, ನೀರಿನಲ್ಲಿ, ಮಗುವಿನ).
INA_TAILS = ["ಿನ", "ಿನಲ್ಲಿ", "ಿನಿಂದ", "ಿನೊಂದಿಗೆ", "ಿನಲ್ಲೂ", "ಿನಂತೆ", "ಿನಲ್ಲೇ", "ಿನವರು", "ಿನಲ್ಲಿಯೂ", "ಿನಲ್ಲಿದೆ", "ಿನಿಂದಲೂ"]
# A noun in -a's genitive -ದ and dative -ಕ್ಕೆ (ಮರದ, ಮರದಲ್ಲಿ, ಮರಕ್ಕೆ).
DA_TAILS = ["ದ", "ದಲ್ಲಿ", "ದಿಂದ", "ದೊಂದಿಗೆ", "ದಂತೆ", "ದಲ್ಲೂ", "ದಲ್ಲೇ", "ದಿಂದಲೂ", "ದಿಂದಲೇ", "ದವರು", "ದಲ್ಲಿಯೂ", "ದಲ್ಲಿದೆ", "ದಲ್ಲಿದ್ದ",
           "ದಲ್ಲಿರುವ", "ಕ್ಕೆ", "ಕ್ಕೂ", "ಕ್ಕೇ", "ಕ್ಕಾಗಿ", "ಕ್ಕಿಂತ", "ದಿಂದಾಗಿ"]
# A noun in -i or -e (ಮನೆಯ, ಮನೆಗೆ): after the glide ಯ, the genitive is the stem itself.
YA_TAILS = NOMINAL_TAILS + ["", "ಲ್ಲಿ", "ಿಂದ", "ಲ್ಲೂ", "ಲ್ಲೇ", "ಂತೆ", "ಿಂದಲೂ", "ಲ್ಲಿಯೂ", "ಲ್ಲಿದೆ", "ಲ್ಲಿದ್ದ", "ಲ್ಲಿರುವ", "ವರು"]
DATIVE_TAILS = ["ಗೆ", "ಗೂ", "ಗೇ", "ಗಾಗಿ", "ಗಿಂತ"]
# An adjective as a noun (ದೊಡ್ಡದು, ದೊಡ್ಡವರು, ದೊಡ್ಡದಾಗಿ).
ADJECTIVE_TAILS = ["ದು", "ದನ್ನು", "ದಕ್ಕೆ", "ದರ", "ದಾಗಿ", "ದಾದ", "ದೇ", "ದೂ", "ದಾಗಿದೆ", "ದಾಗಿತ್ತು", "ವರು", "ವನು", "ವಳು", "ವರ", "ವರನ್ನು",
                   "ವರಿಗೆ", "ದರಲ್ಲಿ", "ದಿದೆ"]
ORDINAL_TAILS = ["ನೇ", "ನೆಯ", "ನೆ", "ನೆಯದು", "ನೆಯದಾಗಿ"]

# The verb's endings, by the stem they join: the present stem (ಮಾಡು, ಕುಡಿಯು), the stem of the infinitive (ಮಾಡ, ಕುಡಿಯ), and
# the past relative stem (ಮಾಡಿದ, ಕುಡಿದ, ಹೋದ).
PRESENT_TAILS = ["ತ್ತೇನೆ", "ತ್ತೀಯ", "ತ್ತೀಯೆ", "ತ್ತೀಯಾ", "ತ್ತಾನೆ", "ತ್ತಾಳೆ", "ತ್ತದೆ", "ತ್ತೇವೆ", "ತ್ತೀರಿ", "ತ್ತೀರಾ", "ತ್ತಾರೆ", "ತ್ತವೆ",
                 "ತ್ತಾ", "ತ್ತ", "ತ್ತಲೇ", "ತ್ತಾರೆಯೇ", "ತ್ತದೆಯೇ", "ತ್ತದೆಯಾ", "ತ್ತೇನೆಂದು", "ತ್ತಾನೆಂದು", "ತ್ತಾರೆಂದು", "ತ್ತದೆಂದು",
                 "ವ", "ವುದು", "ವುದನ್ನು", "ವುದಕ್ಕೆ", "ವುದರ", "ವುದರಿಂದ", "ವುದರಲ್ಲಿ", "ವುದಿಲ್ಲ", "ವುದೂ", "ವುದೇ", "ವಾಗ", "ವಾಗಲೂ",
                 "ವವರು", "ವವರ", "ವವರಿಗೆ", "ವವರನ್ನು", "ವವನು", "ವವಳು", "ವಂತೆ", "ವಷ್ಟು", "ವಂತಹ", "ವುದಾಗಿ", "ವಲ್ಲಿ", "ವುದಕ್ಕಾಗಿ",
                 "ವವರೆಗೆ", "ವುದರೊಂದಿಗೆ", "ವಂಥ", "ವುದಿಲ್ಲವೇ", "ವುದಾದರೆ", "ವುದು", "ವನು", "ವಳು", "ವರು"]
INFINITIVE_TAILS = ["ಲು", "ಲಿ", "ಲಿಕ್ಕೆ", "ಲೇ", "ೋಣ", "ಬೇಕು", "ಬೇಕಾದ", "ಬೇಕಾಗಿದೆ", "ಬೇಕಾಗಿತ್ತು", "ಬೇಕಾಯಿತು", "ಬೇಕಾಗುತ್ತದೆ",
                    "ಬೇಕೆಂದು", "ಬೇಕಿತ್ತು", "ಬೇಕಾಗಿ", "ಬೇಕಾದರೆ", "ಬೇಕೇ", "ಬೇಡ", "ಬೇಡಿ", "ಬಹುದು", "ಬಹುದಾದ", "ಬಹುದೇ", "ಬಾರದು",
                    "ಬಾರದ", "ಬಲ್ಲ", "ಬಲ್ಲರು", "ದೆ", "ದ", "ದಿರಲು", "ದಿದ್ದರೆ", "ದಂತೆ", "ದವರು", "ಲಿಲ್ಲ", "ಲ್ಲ", "ಲಾರ", "ಲಾರೆ",
                    "ಲಾರರು", "ಲಾಗದ", "ಲಾರದು", "ಲೆಂದು", "ಲೇಬೇಕು", "ಲಿದೆ", "ಲಿದ್ದಾರೆ", "ಲಿರುವ", "ಲಿದ್ದ", "ಲಿಲ್ಲವೇ", "ಲೂ"]
PAST_TAILS = ["", "ೆ", "ೆನು", "ನು", "ಳು", "ರು", "ೆವು", "ಿರಿ", "ವು", "ಾಗ", "ರೆ", "ರೂ", "ವರು", "ವರ", "ವರನ್ನು", "ವರಿಗೆ", "ವನು", "ವಳು",
              "ುದು", "ಂತೆ", "ುದರಿಂದ", "ರೆಂದು", "ನೆಂದು", "ಳೆಂದು", "ವು", "ಾಗಲೂ", "ರಾದರೂ", "ಾಯಿತು", "ನಂತರ", "ಷ್ಟು", "ರೇ", "ನೇ",
              "ಳೇ", "ವನ್ನು", "ಾಗಿನಿಂದ", "ುದನ್ನು"]

# Past stems that are not the regular -ಇದ (-ದ after -i, -e): gerund, past relative stem, third person neuter past, and the
# stem the perfect is made on (ಹೋಗಿ + ದ್ದಾನೆ, ಬಂದಿ + ದೆ).
IRREGULAR = {
    "ಹೋಗು": ("ಹೋಗಿ", "ಹೋದ", ["ಹೋಯಿತು", "ಹೋಯ್ತು"], "ಹೋಗಿ"),
    "ಬರು": ("ಬಂದು", "ಬಂದ", ["ಬಂದಿತು", "ಬಂತು"], "ಬಂದಿ"),
    "ಆಗು": ("ಆಗಿ", "ಆದ", ["ಆಯಿತು", "ಆಯ್ತು"], "ಆಗಿ"),
    "ಇರು": ("ಇದ್ದು", "ಇದ್ದ", ["ಇತ್ತು"], "ಇದ್ದಿ"),
    "ಕೊಡು": ("ಕೊಟ್ಟು", "ಕೊಟ್ಟ", ["ಕೊಟ್ಟಿತು", "ಕೊಟ್ಟಿತ್ತು"], "ಕೊಟ್ಟಿ"),
    "ಬಿಡು": ("ಬಿಟ್ಟು", "ಬಿಟ್ಟ", ["ಬಿಟ್ಟಿತು"], "ಬಿಟ್ಟಿ"),
    "ಇಡು": ("ಇಟ್ಟು", "ಇಟ್ಟ", ["ಇಟ್ಟಿತು"], "ಇಟ್ಟಿ"),
    "ಪಡು": ("ಪಟ್ಟು", "ಪಟ್ಟ", ["ಪಟ್ಟಿತು"], "ಪಟ್ಟಿ"),
    "ಸುಡು": ("ಸುಟ್ಟು", "ಸುಟ್ಟ", ["ಸುಟ್ಟಿತು"], "ಸುಟ್ಟಿ"),
    "ತೊಡು": ("ತೊಟ್ಟು", "ತೊಟ್ಟ", ["ತೊಟ್ಟಿತು"], "ತೊಟ್ಟಿ"),
    "ನೆಡು": ("ನೆಟ್ಟು", "ನೆಟ್ಟ", ["ನೆಟ್ಟಿತು"], "ನೆಟ್ಟಿ"),
    "ತಿನ್ನು": ("ತಿಂದು", "ತಿಂದ", ["ತಿಂದಿತು"], "ತಿಂದಿ"),
    "ಕೊಳ್ಳು": ("ಕೊಂಡು", "ಕೊಂಡ", ["ಕೊಂಡಿತು"], "ಕೊಂಡಿ"),
    # ಕೊಳ್ಳು after a vowel in a compound (ಒಳಗೊಳ್ಳು, ಆರಂಭಗೊಳ್ಳು).
    "ಗೊಳ್ಳು": ("ಗೊಂಡು", "ಗೊಂಡ", ["ಗೊಂಡಿತು"], "ಗೊಂಡಿ"),
    "ಕೊಲ್ಲು": ("ಕೊಂದು", "ಕೊಂದ", ["ಕೊಂದಿತು"], "ಕೊಂದಿ"),
    "ಎನ್ನು": ("ಎಂದು", "ಎಂದ", ["ಎಂದಿತು"], "ಎಂದಿ"),
    "ಕಾಣು": ("ಕಂಡು", "ಕಂಡ", ["ಕಂಡಿತು"], "ಕಂಡಿ"),
    "ಸಾಯು": ("ಸತ್ತು", "ಸತ್ತ", ["ಸತ್ತಿತು"], "ಸತ್ತಿ"),
    "ಕಾಯು": ("ಕಾದು", "ಕಾದ", ["ಕಾಯಿತು"], "ಕಾದಿ"),
    "ಕೂರು": ("ಕೂತು", "ಕೂತ", ["ಕೂತಿತು"], "ಕೂತಿ"),
    "ನಗು": ("ನಕ್ಕು", "ನಕ್ಕ", ["ನಕ್ಕಿತು"], "ನಕ್ಕಿ"),
    "ಅಳು": ("ಅತ್ತು", "ಅತ್ತ", ["ಅತ್ತಿತು"], "ಅತ್ತಿ"),
    "ಉಣ್ಣು": ("ಉಂಡು", "ಉಂಡ", ["ಉಂಡಿತು"], "ಉಂಡಿ"),
    "ಬೀಳು": ("ಬಿದ್ದು", "ಬಿದ್ದ", ["ಬಿತ್ತು", "ಬಿದ್ದಿತು"], "ಬಿದ್ದಿ"),
    "ತರು": ("ತಂದು", "ತಂದ", ["ತಂದಿತು", "ತಂತು"], "ತಂದಿ"),
    "ನಿಲ್ಲು": ("ನಿಂತು", "ನಿಂತ", ["ನಿಂತಿತು"], "ನಿಂತಿ"),
    "ಹೊರಡು": ("ಹೊರಟು", "ಹೊರಟ", ["ಹೊರಟಿತು"], "ಹೊರಟಿ"),
    "ಹೊಗು": ("ಹೊಕ್ಕು", "ಹೊಕ್ಕ", ["ಹೊಕ್ಕಿತು"], "ಹೊಕ್ಕಿ"),
    "ಮಲಗು": ("ಮಲಗಿ", "ಮಲಗಿದ", ["ಮಲಗಿತು"], "ಮಲಗಿ"),
    "ಬೇಯು": ("ಬೆಂದು", "ಬೆಂದ", ["ಬೆಂದಿತು"], "ಬೆಂದಿ"),
    "ನೋಯು": ("ನೊಂದು", "ನೊಂದ", ["ನೊಂದಿತು"], "ನೊಂದಿ"),
    "ಈಯು": ("ಈದು", "ಈದ", ["ಈಯಿತು"], "ಈದಿ"),
    "ಮೀಯು": ("ಮಿಂದು", "ಮಿಂದ", ["ಮಿಂದಿತು"], "ಮಿಂದಿ"),
    "ಸಿಗು": ("ಸಿಕ್ಕಿ", "ಸಿಕ್ಕ", ["ಸಿಕ್ಕಿತು"], "ಸಿಕ್ಕಿ"),
    "ದೊರೆ": ("ದೊರೆತು", "ದೊರೆತ", ["ದೊರೆಯಿತು", "ದೊರೆತಿತು"], "ದೊರೆತಿ"),
    "ಕಲಿ": ("ಕಲಿತು", "ಕಲಿತ", ["ಕಲಿಯಿತು", "ಕಲಿತಿತು"], "ಕಲಿತಿ"),
    "ಅರಿ": ("ಅರಿತು", "ಅರಿತ", ["ಅರಿಯಿತು"], "ಅರಿತಿ"),
    "ಮರೆ": ("ಮರೆತು", "ಮರೆತ", ["ಮರೆಯಿತು", "ಮರೆತಿತು"], "ಮರೆತಿ"),
    "ಬೆರೆ": ("ಬೆರೆತು", "ಬೆರೆತ", ["ಬೆರೆಯಿತು"], "ಬೆರೆತಿ"),
    "ಸೋಲು": ("ಸೋತು", "ಸೋತ", ["ಸೋತಿತು"], "ಸೋತಿ"),
}
# Forms of these verbs no rule makes: ಇರು's present (ಇದೆ, ಇದ್ದಾನೆ), the imperatives of ಬರು and ಕೊಳ್ಳು.
VERB_EXTRA = {
    "ಇರು": ["ಇದ್ದೇನೆ", "ಇದ್ದೀಯ", "ಇದ್ದೀಯಾ", "ಇದ್ದಾನೆ", "ಇದ್ದಾಳೆ", "ಇದೆ", "ಇದ್ದೇವೆ", "ಇದ್ದೀರಿ", "ಇದ್ದೀರಾ", "ಇದ್ದಾರೆ", "ಇವೆ", "ಇದೆಯೇ",
            "ಇದೆಯಾ", "ಇದ್ದಾರೆಯೇ", "ಇದ್ದರೆ", "ಇದ್ದರೂ", "ಇದ್ದರಿಂದ", "ಇದ್ದಾಗ", "ಇದ್ದವರು", "ಇದ್ದುದು", "ಇದ್ದದ್ದು", "ಇದೆಂದು", "ಇದ್ದಾರೆಂದು",
            "ಇವೆಯೇ", "ಇದ್ದೆ", "ಇದ್ದೆವು", "ಇದ್ದಳು", "ಇದ್ದನು", "ಇದ್ದರು", "ಇದ್ದವು", "ಇದ್ದಿರಿ", "ಇದ್ದೂ", "ಇದ್ದೇ"],
    "ಬರು": ["ಬಾ", "ಬನ್ನಿ", "ಬಾರೋ", "ಬಾರೇ", "ಬಾರದು", "ಬಾರದ"],
    "ಕೊಳ್ಳು": ["ಕೊ", "ಕೊಳ್ಳಿ"],
    "ತರು": ["ತಾ", "ತನ್ನಿ"],
    "ಆಗು": ["ಆಗಲಿ", "ಆಗಿಲ್ಲ", "ಆಗದ", "ಆಗದೆ"],
    "ಸಿಗು": ["ಸಿಕ್ಕಿದ", "ಸಿಕ್ಕಿದರು", "ಸಿಕ್ಕಿದರೆ", "ಸಿಕ್ಕಿದಾಗ"],
}

# Words the packs lack, or have only as a name or a form, with forms no rule makes. "kind" is the word's kind as the
# rules decline it ("noun", "adj", "verb"; anything else takes no endings).
KANNADA_WORDS = {
    # Pronouns, with their cases.
    "ನಾನು": ("pron", "ನನ್ನ ನನ್ನನ್ನು ನನಗೆ ನನ್ನಲ್ಲಿ ನನ್ನಿಂದ ನನ್ನದು ನಾನೂ ನಾನೇ ನನಗೂ ನನ್ನನ್ನೂ ನನ್ನೊಂದಿಗೆ ನನ್ನಂತೆ ನನಗಾಗಿ ನನ್ನದೇ ನನ್ನವರು ನನ್ನನ್ನೇ"),
    "ನೀನು": ("pron", "ನಿನ್ನ ನಿನ್ನನ್ನು ನಿನಗೆ ನಿನ್ನಲ್ಲಿ ನಿನ್ನಿಂದ ನಿನ್ನದು ನೀನೂ ನೀನೇ ನಿನಗೂ ನಿನ್ನೊಂದಿಗೆ ನಿನಗಾಗಿ ನಿನ್ನನ್ನೂ ನಿನ್ನಂತೆ"),
    "ನಾವು": ("pron", "ನಮ್ಮ ನಮ್ಮನ್ನು ನಮಗೆ ನಮ್ಮಲ್ಲಿ ನಮ್ಮಿಂದ ನಮ್ಮದು ನಾವೂ ನಾವೇ ನಮಗೂ ನಮ್ಮೊಂದಿಗೆ ನಮಗಾಗಿ ನಮ್ಮವರು ನಮ್ಮನ್ನೂ ನಮ್ಮಂತೆ ನಮ್ಮದೇ"),
    "ನೀವು": ("pron", "ನಿಮ್ಮ ನಿಮ್ಮನ್ನು ನಿಮಗೆ ನಿಮ್ಮಲ್ಲಿ ನಿಮ್ಮಿಂದ ನಿಮ್ಮದು ನೀವೂ ನೀವೇ ನಿಮಗೂ ನಿಮ್ಮೊಂದಿಗೆ ನಿಮಗಾಗಿ ನಿಮ್ಮನ್ನೂ ನಿಮ್ಮಂತೆ"),
    "ಅವನು": ("pron", "ಅವನ ಅವನನ್ನು ಅವನಿಗೆ ಅವನಲ್ಲಿ ಅವನಿಂದ ಅವನೂ ಅವನೇ ಅವನಿಗೂ ಅವನೊಂದಿಗೆ ಅವನದು ಅವನನ್ನೂ ಅವ"),
    "ಅವಳು": ("pron", "ಅವಳ ಅವಳನ್ನು ಅವಳಿಗೆ ಅವಳಲ್ಲಿ ಅವಳಿಂದ ಅವಳೂ ಅವಳೇ ಅವಳಿಗೂ ಅವಳೊಂದಿಗೆ ಅವಳದು ಅವಳನ್ನೂ"),
    "ಅದು": ("pron", "ಅದರ ಅದನ್ನು ಅದಕ್ಕೆ ಅದರಲ್ಲಿ ಅದರಿಂದ ಅದೂ ಅದೇ ಅದಕ್ಕೂ ಅದರಲ್ಲೂ ಅದನ್ನೇ ಅದರೊಂದಿಗೆ ಅದಕ್ಕಾಗಿ ಅದರಂತೆ ಅದನ್ನೂ ಅದರಲ್ಲೇ "
                    "ಅದಕ್ಕಿಂತ ಅದರಿಂದಾಗಿ ಅದಾಗಿ ಅದಾದ ಅದಾಗಿದೆ"),
    "ಇದು": ("pron", "ಇದರ ಇದನ್ನು ಇದಕ್ಕೆ ಇದರಲ್ಲಿ ಇದರಿಂದ ಇದೂ ಇದೇ ಇದಕ್ಕೂ ಇದರಲ್ಲೂ ಇದನ್ನೇ ಇದರೊಂದಿಗೆ ಇದಕ್ಕಾಗಿ ಇದರಂತೆ ಇದನ್ನೂ ಇದರಲ್ಲೇ "
                    "ಇದಕ್ಕಿಂತ ಇದರಿಂದಾಗಿ ಇದಾಗಿ ಇದಾದ ಇದಾಗಿದೆ ಇದೊಂದು"),
    "ಅವರು": ("pron", "ಅವರ ಅವರನ್ನು ಅವರಿಗೆ ಅವರಲ್ಲಿ ಅವರಿಂದ ಅವರೂ ಅವರೇ ಅವರಿಗೂ ಅವರೊಂದಿಗೆ ಅವರದು ಅವರನ್ನೂ ಅವರಂತೆ ಅವರದೇ ಅವರಲ್ಲೂ"),
    "ಇವರು": ("pron", "ಇವರ ಇವರನ್ನು ಇವರಿಗೆ ಇವರಲ್ಲಿ ಇವರಿಂದ ಇವರೂ ಇವರೇ ಇವರಿಗೂ ಇವರೊಂದಿಗೆ ಇವರದು"),
    "ಇವನು": ("pron", "ಇವನ ಇವನನ್ನು ಇವನಿಗೆ ಇವನಲ್ಲಿ ಇವನಿಂದ ಇವನೂ ಇವನೇ ಇವ"),
    "ಇವಳು": ("pron", "ಇವಳ ಇವಳನ್ನು ಇವಳಿಗೆ ಇವಳಲ್ಲಿ ಇವಳಿಂದ ಇವಳೂ ಇವಳೇ"),
    "ಅವು": ("pron", "ಅವನ್ನು ಅವನ್ನೂ ಅವನ್ನೇ ಅವುಗಳು ಅವುಗಳ ಅವುಗಳನ್ನು ಅವುಗಳಿಗೆ ಅವುಗಳಲ್ಲಿ ಅವುಗಳಿಂದ ಅವೂ ಅವೇ ಅವುಗಳೂ ಅವುಗಳೇ ಅವುಗಳಲ್ಲೂ"),
    "ಇವು": ("pron", "ಇವನ್ನು ಇವನ್ನೂ ಇವನ್ನೇ ಇವುಗಳು ಇವುಗಳ ಇವುಗಳನ್ನು ಇವುಗಳಿಗೆ ಇವುಗಳಲ್ಲಿ ಇವುಗಳಿಂದ ಇವೂ ಇವೇ ಇವುಗಳೂ ಇವುಗಳೇ ಇವುಗಳಲ್ಲೂ"),
    "ತಾನು": ("pron", "ತನ್ನನ್ನು ತನಗೆ ತನ್ನಲ್ಲಿ ತನ್ನಿಂದ ತಾನೂ ತಾನೇ ತನಗೂ ತನ್ನದು ತನ್ನದೇ ತನ್ನೊಂದಿಗೆ ತನ್ನನ್ನೇ"),
    "ತಾವು": ("pron", "ತಮ್ಮ ತಮ್ಮನ್ನು ತಮಗೆ ತಮ್ಮಲ್ಲಿ ತಮ್ಮಿಂದ ತಾವೂ ತಾವೇ ತಮಗೂ ತಮ್ಮದು ತಮ್ಮದೇ ತಮ್ಮೊಂದಿಗೆ ತಮ್ಮನ್ನೇ"),
    "ಆತ": ("pron", "ಆತನು ಆತನ ಆತನನ್ನು ಆತನಿಗೆ ಆತನಲ್ಲಿ ಆತನಿಂದ ಆತನೂ ಆತನೇ ಆತನೊಂದಿಗೆ"),
    "ಆಕೆ": ("pron", "ಆಕೆಯು ಆಕೆಯ ಆಕೆಯನ್ನು ಆಕೆಗೆ ಆಕೆಯಲ್ಲಿ ಆಕೆಯಿಂದ ಆಕೆಯೂ ಆಕೆಯೇ ಆಕೆಯೊಂದಿಗೆ"),
    "ಈತ": ("pron", "ಈತನು ಈತನ ಈತನನ್ನು ಈತನಿಗೆ ಈತನಿಂದ ಈತನೇ"),
    "ಈಕೆ": ("pron", "ಈಕೆಯ ಈಕೆಯನ್ನು ಈಕೆಗೆ ಈಕೆಯೇ"),
    "ಯಾರು": ("pron", "ಯಾರ ಯಾರನ್ನು ಯಾರಿಗೆ ಯಾರಲ್ಲಿ ಯಾರಿಂದ ಯಾರೂ ಯಾರೋ ಯಾರಾದರೂ ಯಾರೇ ಯಾರದು ಯಾರಿಗೂ ಯಾರನ್ನೂ"),
    "ಏನು": ("pron", "ಏನನ್ನು ಏನೂ ಏನೋ ಏನಾದರೂ ಏನೇ ಏನನ್ನೂ ಏನಿದೆ ಏನಿಲ್ಲ ಏನಾಯಿತು ಏನಂದರೆ"),
    "ಯಾವ": ("det", "ಯಾವುದು ಯಾವುದೇ ಯಾವುದನ್ನು ಯಾವುದರ ಯಾವುದಕ್ಕೆ ಯಾವುದೂ ಯಾವುದೋ ಯಾವುದರಲ್ಲಿ ಯಾವುದನ್ನೂ ಯಾವುದಾದರೂ ಯಾವುವು ಯಾವುದಕ್ಕೂ ಯಾವುದರಿಂದ"),
    "ಎಲ್ಲ": ("det", "ಎಲ್ಲಾ ಎಲ್ಲರೂ ಎಲ್ಲರ ಎಲ್ಲರಿಗೂ ಎಲ್ಲವೂ ಎಲ್ಲವನ್ನೂ ಎಲ್ಲದರ ಎಲ್ಲದರಲ್ಲೂ ಎಲ್ಲರನ್ನೂ ಎಲ್ಲರು ಎಲ್ಲವನ್ನು ಎಲ್ಲರಲ್ಲೂ ಎಲ್ಲದಕ್ಕೂ ಎಲ್ಲವು ಎಲ್ಲರಿಗೆ ಎಲ್ಲರನ್ನು"),
    "ಒಬ್ಬ": ("num", "ಒಬ್ಬನು ಒಬ್ಬಳು ಒಬ್ಬರು ಒಬ್ಬನೇ ಒಬ್ಬಳೇ ಒಬ್ಬರೇ ಒಬ್ಬರ ಒಬ್ಬರನ್ನು ಒಬ್ಬರಿಗೆ ಒಬ್ಬನ ಒಬ್ಬಳ ಒಬ್ಬನನ್ನು ಒಬ್ಬಳನ್ನು ಒಬ್ಬರಾದ ಒಬ್ಬರಾಗಿ "
                    "ಒಬ್ಬರಾಗಿದ್ದರು ಒಬ್ಬರೂ ಒಬ್ಬನಿಗೆ ಒಬ್ಬಳಿಗೆ ಒಬ್ಬರಲ್ಲಿ"),
    "ಇಬ್ಬರು": ("num", "ಇಬ್ಬರ ಇಬ್ಬರನ್ನು ಇಬ್ಬರಿಗೆ ಇಬ್ಬರೂ ಇಬ್ಬರಲ್ಲಿ"),
    "ಮೂವರು": ("num", "ಮೂವರ ಮೂವರನ್ನು ಮೂವರಿಗೆ ಮೂವರೂ"),
    "ಒಂದು": ("num", "ಒಂದೇ ಒಂದರ ಒಂದನ್ನು ಒಂದಕ್ಕೆ ಒಂದರಲ್ಲಿ ಒಂದೂ ಒಂದರಿಂದ ಒಂದಾಗಿ ಒಂದಾದ ಒಂದಾಗಿದೆ ಒಂದನೇ ಒಂದನೆಯ"),
    "ಮೊದಲು": ("adv", "ಮೊದಲ ಮೊದಲನೆಯ ಮೊದಲನೇ ಮೊದಲಿಗೆ ಮೊದಲೇ ಮೊದಲಿನ ಮೊದಲಿಂದ ಮೊದಲನೆಯದಾಗಿ ಮೊದಲಿಗರು"),
    "ಕೊನೆ": ("noun", "ಕೊನೆಯ ಕೊನೆಗೆ ಕೊನೆಯಲ್ಲಿ ಕೊನೆಯದಾಗಿ ಕೊನೆಯವರೆಗೂ ಕೊನೆಗೂ ಕೊನೆಯಾಯಿತು ಕೊನೆಯದು"),
    # Words of not being, wanting and knowing, with their forms.
    "ಇಲ್ಲ": ("adv", "ಇಲ್ಲದ ಇಲ್ಲದೆ ಇಲ್ಲವೇ ಇಲ್ಲದಿದ್ದರೆ ಇಲ್ಲವಾದ ಇಲ್ಲದಿರುವ ಇಲ್ಲವಾಗಿ ಇಲ್ಲದಿದ್ದರೂ ಇಲ್ಲದೇ ಇಲ್ಲವೆಂದು ಇಲ್ಲದವರು ಇಲ್ಲವಾಯಿತು ಇಲ್ಲದಂತೆ "
                    "ಇಲ್ಲದಿದ್ದಾಗ ಇಲ್ಲದ್ದರಿಂದ ಇಲ್ಲದಿದ್ದ ಇಲ್ಲವಾಗಿದೆ"),
    "ಅಲ್ಲ": ("adv", "ಅಲ್ಲದ ಅಲ್ಲವೇ ಅಲ್ಲವೆ ಅಲ್ಲವೆಂದು ಅಲ್ಲದವರು ಅಲ್ಲದಿದ್ದರೆ ಅಲ್ಲವಾದ ಅಲ್ಲವಾದರೂ"),
    "ಬೇಕು": ("adv", "ಬೇಕಾದ ಬೇಕಾಗಿದೆ ಬೇಕಾಯಿತು ಬೇಕಿತ್ತು ಬೇಕೇ ಬೇಕಾಗುತ್ತದೆ ಬೇಕಾಗಿತ್ತು ಬೇಕಾದರೆ ಬೇಕಿಲ್ಲ ಬೇಕೆಂದು ಬೇಕಾಗಿ ಬೇಕಾಗುವ ಬೇಕಾದಷ್ಟು ಬೇಕಾದವರು ಬೇಕಾಗಿರುವ"),
    "ಬೇಡ": ("adv", "ಬೇಡವೇ ಬೇಡವಾದ ಬೇಡವೆಂದು ಬೇಡವಾಗಿದೆ"),
    "ಗೊತ್ತು": ("noun", "ಗೊತ್ತಿಲ್ಲ ಗೊತ್ತಾಯಿತು ಗೊತ್ತಾಗಿ ಗೊತ್ತಿತ್ತು ಗೊತ್ತಾಗುತ್ತದೆ ಗೊತ್ತಿರುವ ಗೊತ್ತಾದ ಗೊತ್ತೇ ಗೊತ್ತಾ ಗೊತ್ತಿದೆ ಗೊತ್ತಾಗಲಿಲ್ಲ ಗೊತ್ತಿಲ್ಲದ ಗೊತ್ತಾಗದೆ"),
    "ಇಷ್ಟ": ("noun", ""),
    "ಸಾಧ್ಯ": ("noun", "ಸಾಧ್ಯವಿಲ್ಲ ಸಾಧ್ಯವಾಗಲಿಲ್ಲ ಸಾಧ್ಯವಿದೆ ಸಾಧ್ಯವೇ ಸಾಧ್ಯವಾದಷ್ಟು ಸಾಧ್ಯವಾಗದ ಸಾಧ್ಯವಿಲ್ಲದ"),
    # Question words and words of place and time.
    "ಯಾಕೆ": ("adv", "ಯಾಕೋ ಯಾಕೆಂದರೆ"),
    "ಏಕೆ": ("adv", "ಏಕೋ ಏಕೆಂದರೆ"),
    "ಎಲ್ಲಿ": ("adv", "ಎಲ್ಲಿಯ ಎಲ್ಲಿಂದ ಎಲ್ಲಿಗೆ ಎಲ್ಲೂ ಎಲ್ಲೋ ಎಲ್ಲಿಯೂ ಎಲ್ಲಿಯಾದರೂ ಎಲ್ಲಿದೆ ಎಲ್ಲಿದ್ದಾನೆ ಎಲ್ಲಿದ್ದೀಯ ಎಲ್ಲಿದ್ದಾರೆ ಎಲ್ಲಿಯವರೆಗೆ"),
    "ಇಲ್ಲಿ": ("adv", "ಇಲ್ಲಿಯ ಇಲ್ಲಿಂದ ಇಲ್ಲಿಗೆ ಇಲ್ಲೂ ಇಲ್ಲೇ ಇಲ್ಲಿಯೂ ಇಲ್ಲಿಯೇ ಇಲ್ಲಿದೆ ಇಲ್ಲಿದ್ದಾರೆ ಇಲ್ಲಿಯವರೆಗೆ ಇಲ್ಲಿನ ಇಲ್ಲಿದ್ದ ಇಲ್ಲಿರುವ"),
    "ಅಲ್ಲಿ": ("adv", "ಅಲ್ಲಿಯ ಅಲ್ಲಿಂದ ಅಲ್ಲಿಗೆ ಅಲ್ಲೂ ಅಲ್ಲೇ ಅಲ್ಲಿಯೂ ಅಲ್ಲಿಯೇ ಅಲ್ಲಿದೆ ಅಲ್ಲಿದ್ದ ಅಲ್ಲಿದ್ದಾರೆ ಅಲ್ಲಿಯವರೆಗೆ ಅಲ್ಲಿನ ಅಲ್ಲಿರುವ ಅಲ್ಲಿದ್ದರು"),
    "ಯಾವಾಗ": ("adv", "ಯಾವಾಗಲೂ ಯಾವಾಗಲೋ ಯಾವಾಗಲಾದರೂ"),
    "ಹೇಗೆ": ("adv", "ಹೇಗೋ ಹೇಗಾದರೂ ಹೇಗಿದೆ ಹೇಗಿದ್ದೀಯ ಹೇಗಿದ್ದೀರಿ ಹೇಗಿತ್ತು"),
    "ಎಷ್ಟು": ("adv", "ಎಷ್ಟೋ ಎಷ್ಟೇ ಎಷ್ಟಿದೆ ಎಷ್ಟರ ಎಷ್ಟರಲ್ಲಿ"),
    "ಅಷ್ಟು": ("adv", "ಅಷ್ಟೇ ಅಷ್ಟರಲ್ಲಿ ಅಷ್ಟರ ಅಷ್ಟೂ ಅಷ್ಟಾಗಿ"),
    "ಇಷ್ಟು": ("adv", "ಇಷ್ಟೇ ಇಷ್ಟರಲ್ಲಿ ಇಷ್ಟರ ಇಷ್ಟೂ"),
    "ಆಗ": ("adv", "ಆಗಲೇ ಆಗಲೂ ಆಗಿನ"),
    "ಈಗ": ("adv", "ಈಗಲೇ ಈಗಲೂ ಈಗಿನ ಈಗಾಗಲೇ"),
    "ಇಂದು": ("adv", "ಇಂದಿನ ಇಂದಿಗೂ ಇಂದೇ"),
    "ನಾಳೆ": ("adv", "ನಾಳೆಯ ನಾಳೆಯೇ ನಾಳೆಗೆ"),
    "ನಿನ್ನೆ": ("adv", "ನಿನ್ನೆಯ ನಿನ್ನೆಯೇ"),
    "ಇವತ್ತು": ("adv", "ಇವತ್ತಿನ ಇವತ್ತೇ"),
    "ಬೆಳಿಗ್ಗೆ": ("adv", "ಬೆಳಗ್ಗೆ ಬೆಳಿಗ್ಗೆಯೇ ಬೆಳಗಿನ ಬೆಳಗ್ಗಿನ"),
    "ಆಮೇಲೆ": ("adv", ""),
    "ಮತ್ತೆ": ("adv", ""),
    "ಇನ್ನೂ": ("adv", ""),
    "ಇನ್ನು": ("adv", ""),
    "ಹಾಗೆ": ("adv", "ಹಾಗೇ ಹಾಗಾಗಿ ಹಾಗಿದ್ದರೆ"),
    "ಹೀಗೆ": ("adv", "ಹೀಗೇ ಹೀಗಾಗಿ ಹೀಗಿದೆ ಹೀಗಿತ್ತು"),
    "ಅಂತಹ": ("det", "ಅಂಥ ಅಂತಹದು ಅಂತಹವರು"),
    "ಇಂತಹ": ("det", "ಇಂಥ ಇಂತಹದು ಇಂತಹವರು"),
    "ತುಂಬಾ": ("adv", "ತುಂಬ"),
    "ಕೂಡ": ("adv", "ಕೂಡಾ"),
    "ಜೊತೆ": ("postp", "ಜೊತೆಗೆ ಜೊತೆಯಲ್ಲಿ ಜೊತೆಗೂ ಜೊತೆಯೇ"),
    "ಹತ್ತಿರ": ("postp", "ಹತ್ತಿರದ ಹತ್ತಿರದಲ್ಲಿ ಹತ್ತಿರಕ್ಕೆ ಹತ್ತಿರವೇ"),
    "ಒಳಗೆ": ("postp", "ಒಳಗಿನ ಒಳಗಿಂದ ಒಳಗೂ ಒಳಗೇ"),
    "ಹೊರಗೆ": ("postp", "ಹೊರಗಿನ ಹೊರಗಿಂದ ಹೊರಗೂ"),
    "ಕೆಳಗೆ": ("postp", "ಕೆಳಗಿನ ಕೆಳಗಿಂದ ಕೆಳಗೂ"),
    "ಮೇಲೆ": ("postp", "ಮೇಲಿನ ಮೇಲಿಂದ ಮೇಲೂ ಮೇಲೇ ಮೇಲಿರುವ ಮೇಲಿನಿಂದ ಮೇಲಿದೆ ಮೇಲಿದ್ದ ಮೇಲಿತ್ತು"),
    "ಮುಂದೆ": ("postp", "ಮುಂದೂ"),
    "ಹಿಂದೆ": ("postp", "ಹಿಂದಿನ ಹಿಂದಿನಿಂದ"),
    "ನಂತರ": ("postp", "ನಂತರದ ನಂತರವೂ ನಂತರವೇ ನಂತರದಲ್ಲಿ"),
    "ಮೂಲಕ": ("postp", ""),
    "ಸಲುವಾಗಿ": ("postp", ""),
    "ಪರವಾಗಿ": ("postp", ""),
    "ಅನುಗುಣವಾಗಿ": ("postp", "ಅನುಗುಣವಾದ"),
    "ಮಾತ್ರ": ("adv", "ಮಾತ್ರವೇ ಮಾತ್ರವಲ್ಲ ಮಾತ್ರವಲ್ಲದೆ ಮಾತ್ರವಲ್ಲದೇ ಮಾತ್ರವು ಮಾತ್ರಾ"),
    "ಕೆಲವು": ("adj", "ಕೆಲವರು ಕೆಲವರ ಕೆಲವರಿಗೆ ಕೆಲವರನ್ನು ಕೆಲವರಲ್ಲಿ ಕೆಲವರೂ"),
    "ಬಗ್ಗೆ": ("postp", "ಬಗೆಗಿನ"),
    "ಕುರಿತು": ("postp", "ಕುರಿತ ಕುರಿತಾದ"),
    "ನಡುವೆ": ("postp", "ನಡುವಿನ ನಡುವೆಯೂ"),
    "ವರೆಗೆ": ("postp", "ವರೆಗೂ ವರೆಗಿನ"),
    "ತನಕ": ("postp", ""),
    "ಬದಲು": ("postp", "ಬದಲಿಗೆ ಬದಲಾಗಿ"),
    "ಪ್ರಕಾರ": ("postp", ""),
    "ವಿರುದ್ಧ": ("postp", "ವಿರುದ್ಧದ ವಿರುದ್ಧವಾಗಿ"),
    "ಹೊರತು": ("postp", "ಹೊರತಾಗಿ ಹೊರತುಪಡಿಸಿ"),
    "ಕಡೆಗೆ": ("postp", ""),
    "ಸುತ್ತ": ("postp", "ಸುತ್ತಲೂ ಸುತ್ತಮುತ್ತ ಸುತ್ತಮುತ್ತಲಿನ"),
    "ಮುಂಚೆ": ("adv", "ಮುಂಚಿನ"),
    "ಬಳಿ": ("postp", "ಬಳಿಯ ಬಳಿಗೆ ಬಳಿಯಲ್ಲಿ"),
    # Conjunctions and particles.
    "ಏಕೆಂದರೆ": ("conj", ""),
    "ಆದ್ದರಿಂದ": ("conj", ""),
    "ಆದರೂ": ("conj", ""),
    "ಅಲ್ಲದೆ": ("conj", "ಅಲ್ಲದೇ"),
    "ಅಂದರೆ": ("conj", ""),
    "ಎಂದರೆ": ("conj", ""),
    "ಅಂತ": ("particle", "ಅಂತಾ"),
    "ಎಂಬ": ("particle", "ಎಂಬುದು ಎಂಬುದನ್ನು ಎಂಬುದರ ಎಂಬುದಕ್ಕೆ ಎಂಬವರು ಎಂಬಲ್ಲಿ ಎಂಬುವ ಎಂಬುದೇ ಎಂಬವನು ಎಂಬಾತ ಎಂಬಾಕೆ ಎಂಬುದಾಗಿ ಎಂಬುದಾಗಿದೆ"),
    "ಹೌದು": ("intj", ""),
    "ಸರಿ": ("adj", ""),
    "ದಯವಿಟ್ಟು": ("adv", ""),
    "ಧನ್ಯವಾದ": ("noun", "ಧನ್ಯವಾದಗಳು"),
    "ನಮಸ್ಕಾರ": ("intj", "ನಮಸ್ತೆ"),
    # Everyday words the packs lack.
    "ಒಳ್ಳೆಯ": ("adj", "ಒಳ್ಳೆಯದು ಒಳ್ಳೆಯದನ್ನು ಒಳ್ಳೆಯವರು ಒಳ್ಳೆಯವನು ಒಳ್ಳೆಯವಳು ಒಳ್ಳೆಯದಾಗಿ ಒಳ್ಳೆಯದೇ"),
    "ಬಿಸಿ": ("adj", "ಬಿಸಿಯಾದ ಬಿಸಿಯಾಗಿ ಬಿಸಿಯಾಗಿದೆ"),
    "ತಣ್ಣಗೆ": ("adv", "ತಣ್ಣನೆಯ ತಣ್ಣಗಿನ"),
    "ಖುಷಿ": ("noun", ""),
    "ಹಸಿವು": ("noun", "ಹಸಿವಾಗಿದೆ ಹಸಿವಾಯಿತು"),
    "ಬಾಯಾರಿಕೆ": ("noun", ""),
    "ತಡ": ("noun", "ತಡವಾಗಿ ತಡವಾಯಿತು ತಡವಾಗಿದೆ"),
    "ನಿಧಾನ": ("adj", "ನಿಧಾನವಾಗಿ ನಿಧಾನವಾದ"),
    "ಬೇಗ": ("adv", "ಬೇಗನೆ ಬೇಗನೇ"),
    "ಬಸ್": ("noun", "ಬಸ್ಸು ಬಸ್ಸಿನ ಬಸ್ಸಿನಲ್ಲಿ ಬಸ್ಸಿಗೆ ಬಸ್ಸನ್ನು ಬಸ್‌ನಲ್ಲಿ ಬಸ್‌ಗೆ ಬಸ್‌ನ ಬಸ್ಸುಗಳು"),
    "ಮಾರುಕಟ್ಟೆ": ("noun", ""),
    "ವೈದ್ಯ": ("noun", ""),
    "ಡಾಕ್ಟರ್": ("noun", "ಡಾಕ್ಟರ ಡಾಕ್ಟರನ್ನು ಡಾಕ್ಟರಿಗೆ ಡಾಕ್ಟರ್‌ಗೆ"),
    "ಚಳಿಗಾಲ": ("noun", ""),
    "ದೇವರು": ("noun", "ದೇವರ ದೇವರನ್ನು ದೇವರಿಗೆ ದೇವರೇ ದೇವರೂ ದೇವರಲ್ಲಿ"),
    "ಕನ್ನಡ": ("noun", "ಕನ್ನಡದ ಕನ್ನಡದಲ್ಲಿ ಕನ್ನಡವನ್ನು ಕನ್ನಡಕ್ಕೆ ಕನ್ನಡವು ಕನ್ನಡವೇ ಕನ್ನಡದಲ್ಲೇ ಕನ್ನಡದಿಂದ ಕನ್ನಡಿಗ ಕನ್ನಡಿಗರು"),
    "ಇಂಗ್ಲಿಷ್": ("noun", "ಇಂಗ್ಲಿಷ್‌ನಲ್ಲಿ ಇಂಗ್ಲಿಷಿನಲ್ಲಿ ಇಂಗ್ಲಿಷಿನ ಇಂಗ್ಲೀಷ್ ಇಂಗ್ಲೀಷಿನಲ್ಲಿ"),
    "ತಮಿಳು": ("noun", ""),
    "ತೆಲುಗು": ("noun", ""),
    "ಮಲಯಾಳಂ": ("noun", "ಮಲಯಾಳ"),
    "ಸಂಸ್ಕೃತ": ("noun", ""),
    "ಮರಾಠಿ": ("noun", ""),
    "ಉರ್ದು": ("noun", ""),
    "ಊರು": ("noun", ""),
    "ಅಪ್ಪ": ("noun", ""),
    "ಅಮ್ಮ": ("noun", ""),
    "ಹುಡುಗ": ("noun", ""),
    "ಹುಡುಗಿ": ("noun", ""),
    "ಗೆಳತಿ": ("noun", ""),
    "ತಾತ": ("noun", ""),
    "ಮಾವ": ("noun", ""),
    "ಅತ್ತೆ": ("noun", ""),
    "ಚಿಕ್ಕಪ್ಪ": ("noun", ""),
    "ದೊಡ್ಡಪ್ಪ": ("noun", ""),
    "ಮೊಮ್ಮಗ": ("noun", ""),
    "ಮೊಮ್ಮಗಳು": ("noun", ""),
    "ಕೋಣೆ": ("noun", ""),
    "ಅಡುಗೆ": ("noun", ""),
    "ಅಡುಗೆಮನೆ": ("noun", ""),
    "ತಿಂಡಿ": ("noun", ""),
    "ಸಾರು": ("noun", ""),
    "ದೋಸೆ": ("noun", ""),
    "ಇಡ್ಲಿ": ("noun", ""),
    "ರೊಟ್ಟಿ": ("noun", ""),
    "ತರಕಾರಿ": ("noun", ""),
    "ಬಾಳೆಹಣ್ಣು": ("noun", ""),
    "ಮೇಜು": ("noun", ""),
    "ಕುರ್ಚಿ": ("noun", ""),
    "ಚೀಲ": ("noun", ""),
    "ಆಟಿಕೆ": ("noun", ""),
    "ಉದ್ಯಾನ": ("noun", "ಉದ್ಯಾನವನ"),
    "ಹೋಟೆಲ್": ("noun", "ಹೋಟೆಲಿನ ಹೋಟೆಲಿನಲ್ಲಿ ಹೋಟೆಲ್‌ನಲ್ಲಿ ಹೋಟೆಲಿಗೆ ಹೋಟೆಲ್‌ಗೆ"),
    "ಕೆಲಸ": ("noun", ""),
    "ಫೋನ್": ("noun", "ಫೋನ್‌ನಲ್ಲಿ ಫೋನಿನಲ್ಲಿ ಫೋನಿನ ಫೋನ್‌ನ ಫೋನನ್ನು ಫೋನ್‌ಗೆ"),
    "ಟಿಕೆಟ್": ("noun", "ಟಿಕೆಟ್ಟು ಟಿಕೆಟ್‌ಗಳು ಟಿಕೆಟನ್ನು"),
    "ಕಿಟಕಿ": ("noun", ""),
    "ಮಹಡಿ": ("noun", ""),
    "ವಾರಾಂತ್ಯ": ("noun", ""),
    "ರಜೆ": ("noun", ""),
    "ಪ್ರವಾಸ": ("noun", ""),
    "ಸುಸ್ತು": ("noun", "ಸುಸ್ತಾಗಿದೆ ಸುಸ್ತಾಯಿತು ಸುಸ್ತಾಗಿ"),
    "ಜ್ವರ": ("noun", ""),
    "ಪಕ್ಕ": ("noun", "ಪಕ್ಕದ ಪಕ್ಕದಲ್ಲಿ ಪಕ್ಕಕ್ಕೆ"),
    # Verbs the packs lack.
    "ಹಾಕು": ("verb", ""),
    "ಇಡು": ("verb", ""),
    "ತೆಗೆ": ("verb", ""),
    "ತೆಗೆದುಕೊಳ್ಳು": ("verb", "ತೆಗೆದುಕೊ"),
    "ಕೊಡು": ("verb", ""),
    "ಕಾಯು": ("verb", ""),
    "ತಲುಪು": ("verb", ""),
    "ಮುಗಿ": ("verb", ""),
    "ಮುಗಿಸು": ("verb", ""),
    "ಮಾತಾಡು": ("verb", ""),
    "ಮಾತನಾಡು": ("verb", ""),
    "ಕಲಿಸು": ("verb", ""),
    "ಕಲಿ": ("verb", ""),
    "ಇರು": ("verb", ""),
    "ಮಲಗು": ("verb", ""),
    "ನಿಲ್ಲು": ("verb", ""),
    "ಸಿಗು": ("verb", ""),
    "ಹೊರಡು": ("verb", ""),
    "ಬಯಸು": ("verb", ""),
    "ಕುಳಿತುಕೊಳ್ಳು": ("verb", "ಕುಳಿತುಕೊ ಕುಳಿತ ಕುಳಿತು ಕುಳಿತಿದ್ದ ಕುಳಿತಿದ್ದಾನೆ ಕುಳಿತಿದ್ದಾಳೆ ಕುಳಿತಿದ್ದಾರೆ ಕುಳಿತಿದೆ ಕುಳಿತರು ಕುಳಿತನು ಕುಳಿತಳು ಕುಳಿತೆ"),
    "ಎದ್ದೇಳು": ("verb", "ಎದ್ದ ಎದ್ದು ಎದ್ದನು ಎದ್ದಳು ಎದ್ದರು ಎದ್ದೆ ಎದ್ದೇಳುತ್ತಾನೆ ಎದ್ದೇಳುತ್ತಾಳೆ ಎದ್ದೇಳುತ್ತಾರೆ ಎದ್ದೇಳುತ್ತೇನೆ"),
    "ತಿಳಿ": ("verb", "ತಿಳಿದಿದೆ ತಿಳಿದಿಲ್ಲ ತಿಳಿಯದ"),
    "ತಿಳಿಸು": ("verb", ""),
    "ಕರೆ": ("verb", ""),
    "ಕೊಳ್ಳು": ("verb", ""),
    "ಕೊಂಡುಕೊಳ್ಳು": ("verb", ""),
    "ಖರೀದಿಸು": ("verb", ""),
    "ಓಡು": ("verb", ""),
    "ನಡೆ": ("verb", ""),
    "ತೊಳೆ": ("verb", ""),
    "ಆಡು": ("verb", ""),
    "ಹುಡುಕು": ("verb", ""),
    "ಹಾಡು": ("noun", ""),
    "ದೊರೆ": ("verb", ""),
    "ಬದಲಾಗು": ("verb", ""),
    "ನೆನಪಿಸು": ("verb", ""),
    "ಮರೆ": ("verb", ""),
    "ಒಪ್ಪು": ("verb", ""),
    "ಸಹಾಯಮಾಡು": ("verb", ""),
    "ಪ್ರಾರಂಭಿಸು": ("verb", ""),
    "ಆರಂಭಿಸು": ("verb", ""),
    "ಬದಲಾಯಿಸು": ("verb", ""),
    "ಕೇಳು": ("verb", ""),
    "ಹೇಳು": ("verb", ""),
    "ನೋಡು": ("verb", ""),
    "ಬರು": ("verb", ""),
    "ಹೋಗು": ("verb", ""),
    "ಆಗು": ("verb", ""),
    "ಮಾಡು": ("verb", ""),
    "ತಿನ್ನು": ("verb", ""),
    "ಕುಡಿ": ("verb", ""),
    "ಓದು": ("verb", ""),
    "ಬರೆ": ("verb", ""),
    "ತರು": ("verb", ""),
    "ಕಾಣು": ("verb", ""),
    "ಬಿಡು": ("verb", ""),
    "ಸೇರು": ("verb", ""),
    "ಉಳಿ": ("verb", ""),
    "ಗೆಲ್ಲು": ("verb", "ಗೆದ್ದ ಗೆದ್ದು ಗೆದ್ದರು ಗೆದ್ದನು ಗೆದ್ದಳು ಗೆದ್ದಿತು ಗೆದ್ದಿದ್ದಾರೆ ಗೆದ್ದಿದೆ ಗೆದ್ದಿದ್ದ ಗೆದ್ದಿತ್ತು"),
    "ಸೋಲು": ("verb", ""),
    "ಬೆಳೆ": ("verb", ""),
    "ಹುಟ್ಟು": ("verb", ""),
    "ಸಾಯು": ("verb", ""),
    "ಪಡೆ": ("verb", ""),
    "ಹೊಂದು": ("verb", ""),
    "ನೀಡು": ("verb", ""),
    "ಬಳಸು": ("verb", ""),
    "ತಿರುಗು": ("verb", ""),
    "ಕಟ್ಟು": ("verb", ""),
    "ಮುಚ್ಚು": ("verb", ""),
    "ತೆರೆ": ("verb", ""),
    "ಎತ್ತು": ("verb", ""),
    "ಇಳಿ": ("verb", ""),
    "ಹತ್ತು": ("verb", ""),
    "ಬೀಳು": ("verb", ""),
    "ನಗು": ("verb", ""),
    "ಅಳು": ("verb", ""),
    "ಕಳಿಸು": ("verb", "ಕಳಿಸಲು ಕಳಿಸಿದ ಕಳಿಸಿದರು"),
    "ಕಳುಹಿಸು": ("verb", ""),
    "ಪ್ರೀತಿಸು": ("verb", ""),
    "ಯೋಚಿಸು": ("verb", ""),
    "ನಂಬು": ("verb", ""),
    "ಅನಿಸು": ("verb", "ಅನಿಸಿತು ಅನಿಸುತ್ತದೆ ಅನಿಸಿದೆ ಅನ್ನಿಸಿತು ಅನ್ನಿಸುತ್ತದೆ"),
    "ಕಾಣಿಸು": ("verb", ""),
    "ಕೇಳಿಸು": ("verb", ""),
    "ಮಾರು": ("verb", ""),
    "ಕೊಲ್ಲು": ("verb", ""),
}

# Words of the packs that are forms of another (ಕಲ್ಲಿನ, ಕಂಡ), or two spellings of one word (ಹಾಗು of ಹಾಗೂ): counted and listed
# under the word.
KANNADA_FORMS = {
    "ಕಲ್ಲಿನ": "ಕಲ್ಲು", "ಆರೋಗ್ಯದ": "ಆರೋಗ್ಯ", "ಕೆಲಸಗಾರರು": "ಕೆಲಸಗಾರ", "ಸಂಗತಿಗಳು": "ಸಂಗತಿ", "ಮಡಿದ": "ಮಡಿ", "ಕಂಡ": "ಕಾಣು",
    "ಕೊಂಡ": "ಕೊಳ್ಳು", "ಕಳಿಸಲು": "ಕಳಿಸು", "ಅವುಗಳು": "ಅವು", "ಇವುಗಳು": "ಇವು", "ಹಾಗು": "ಹಾಗೂ", "ಕತೆ": "ಕಥೆ",
    "ಮಕ್ಕಳು": "ಮಗು", "ಮಕ್ಕಳ": "ಮಗು", "ಮಕ್ಕಳನ್ನು": "ಮಗು", "ಮಕ್ಕಳಿಗೆ": "ಮಗು", "ಮಕ್ಕಳಲ್ಲಿ": "ಮಗು", "ಮಕ್ಕಳಿಂದ": "ಮಗು",
    "ಮಕ್ಕಳೂ": "ಮಗು", "ಮಕ್ಕಳೊಂದಿಗೆ": "ಮಗು", "ಮಕ್ಕಳಾದ": "ಮಗು", "ಮಕ್ಕಳಿದ್ದಾರೆ": "ಮಗು", "ಮಕ್ಕಳಿಗೂ": "ಮಗು",
    "ಜನರು": "ಜನ", "ಜನರ": "ಜನ", "ಜನರನ್ನು": "ಜನ", "ಜನರಿಗೆ": "ಜನ", "ಜನರಲ್ಲಿ": "ಜನ", "ಜನರಿಂದ": "ಜನ", "ಜನರೂ": "ಜನ",
    "ಜನರೇ": "ಜನ", "ಜನರಿಗೂ": "ಜನ", "ಜನರೊಂದಿಗೆ": "ಜನ",
    "ಹೆಂಗಸರು": "ಹೆಂಗಸು", "ಗಂಡಸರು": "ಗಂಡಸು",
    "ಎರಡನೆಯ": "ಎರಡು", "ಮೂರನೆಯ": "ಮೂರು", "ತಿಂಗಳ": "ತಿಂಗಳು",
    # ಕಲೆ "art" in compounds (ಕಲಾವಿದ, ಕಲಾ ಪ್ರದರ್ಶನ), not ಕಲಿ "learn" asked a question.
    "ಕಲಾ": "ಕಲೆ",
}
# Two spellings of one word: the second's count and forms go to the first (ಸರಕಾರ to ಸರ್ಕಾರ, ಕಾಲೇಜ್ to ಕಾಲೇಜು).
KANNADA_SPELLINGS = {"ಮುಂದುವರೆ": "ಮುಂದುವರಿ", "ಕಂಪೆನಿ": "ಕಂಪನಿ", "ಸರಕಾರ": "ಸರ್ಕಾರ", "ಜತೆ": "ಜೊತೆ", "ಇತರೆ": "ಇತರ", "ಕಾಲೇಜ್": "ಕಾಲೇಜು",
                     "ವಿಶ್ವವಿದ್ಯಾನಿಲಯ": "ವಿಶ್ವವಿದ್ಯಾಲಯ", "ಅಂತಾರಾಷ್ಟ್ರೀಯ": "ಅಂತರರಾಷ್ಟ್ರೀಯ", "ಇಂಡಿಯ": "ಇಂಡಿಯಾ", "ಅಮೇರಿಕ": "ಅಮೇರಿಕಾ",
                     # Loanwords written with and without a closing u (ಮೀಟರ್, ಮೀಟರು): the commoner spelling keeps the word.
                     "ಚರ್ಚು": "ಚರ್ಚ್", "ಮೀಟರು": "ಮೀಟರ್", "ಕಿಲೋಮೀಟರು": "ಕಿಲೋಮೀಟರ್", "ಹೋಟೆಲು": "ಹೋಟೆಲ್", "ಸೆಕೆಂಡು": "ಸೆಕೆಂಡ್",
                     "ಸಿಮೆಂಟು": "ಸಿಮೆಂಟ್", "ಹಾರ್ಮೋನು": "ಹಾರ್ಮೋನ್", "ಬಜಾರು": "ಬಜಾರ್", "ಶೇರ್": "ಶೇರು", "ಕಾರ್": "ಕಾರು",
                     "ವಿದ್ಯುತ್ತು": "ವಿದ್ಯುತ್", "ಸಂಸತ್": "ಸಂಸತ್ತು", "ಪರಿಷತ್": "ಪರಿಷತ್ತು",
                     # Plurals the packs or the corpus give as words of their own, with their case forms.
                     "ಬ್ಯಾಂಕುಗಳು": "ಬ್ಯಾಂಕ್", "ಪಡೆಗಳು": "ಪಡೆ", "ಪ್ರಕಾರಗಳು": "ಪ್ರಕಾರ", "ಬೆಳೆಗಳು": "ಬೆಳೆ", "ಕಾಲೇಜುಗಳು": "ಕಾಲೇಜು",
                     "ದೊರೆಗಳು": "ದೊರೆ", "ಪ್ರೋಟೀನುಗಳು": "ಪ್ರೋಟೀನ್", "ಕತೆಗಳು": "ಕಥೆ", "ಅವರುಗಳು": "ಅವರು", "ಇವರುಗಳು": "ಇವರು",
                     "ಪ್ರವಾಸಿಗರು": "ಪ್ರವಾಸಿ", "ಪೊಲೀಸರು": "ಪೊಲೀಸ್", "ಪೋಲಿಸರು": "ಪೋಲಿಸ್", "ಸಂಯೋಜಕರು": "ಸಂಯೋಜಕ", "ಸ್ಥಾಪಕರು": "ಸ್ಥಾಪಕ",
                     "ಪ್ರಮುಖರು": "ಪ್ರಮುಖ", "ಬೌದ್ಧರು": "ಬೌದ್ಧ", "ಮುಸ್ಲಿಮರು": "ಮುಸ್ಲಿಮ್", "ಬ್ರಿಟಿಷರು": "ಬ್ರಿಟಿಷ್", "ಬ್ರಿಟೀಷರು": "ಬ್ರಿಟೀಷ್",
                     "ಇಂಗ್ಲಿಷರು": "ಇಂಗ್ಲಿಷ್", "ಫ್ರೆಂಚರು": "ಫ್ರೆಂಚ್", "ಜರ್ಮನರು": "ಜರ್ಮನ್", "ಗ್ರೀಕರು": "ಗ್ರೀಕ್", "ಪೋರ್ಚುಗೀಸರು": "ಪೋರ್ಚುಗೀಸ್"}
# Words of the packs the lists leave out: letters, the case endings the corpus writes apart after a number or a Latin word
# (1857ರಲ್ಲಿ, ಐಐಟಿ ನ), and words that are only names.
KANNADA_EXCLUDED = {"ಅ", "ನ", "ರ", "ದ", "ಗೆ", "ರಲ್ಲಿ", "ನಲ್ಲಿ", "ಅನ್ನು", "ರಿಂದ", "ರಂದು", "ಯ", "ಲ್ಲಿ", "ಕ್ಕೆ", "ವ", "ಇ", "ಎ", "ಓಂ", "ಶ್ರೀ",
                    "ನಿ", "ನೇ", "ಮೀ", "ಕಿ", "ಸೆಂ", "ಕಿಮೀ", "ಡಾ", "ಮಿ", "ಲೀ", "ಗ್ರಾಂ"}
# Words of the corpus that are names, English or letters, which induce would take for words.
KANNADA_NOT_WORDS = {"ಗಳು", "ಆಫ್", "ದಿ", "ಅಂಡ್", "ಇನ್", "ಟು", "ಫಾರ್", "ಆನ್", "ಶೇ", "ರೂ", "ಕ್ರಿ", "ಕಿ", "ಎ", "ಬಿ", "ಸಿ", "ಡಿ", "ಕೆ",
                     "ಎಸ್", "ಎಂ", "ಆರ್", "ಜಿ", "ಟಿ", "ಪಿ", "ವಿ", "ಯು", "ಜೆ", "ಎನ್", "ಎಲ್", "ಎಚ್", "ಎಫ್", "ಡಬ್ಲ್ಯೂ", "ಒ", "ಐ",
                     "ಜಾನ್", "ಸಿಂಗ್", "ನ್ಯೂ", "ಯುನೈಟೆಡ್", "ಮೈಸೂರ್", "ಪಾಲ್", "ಬದು",
                     "ಜಾರ್ಜ್", "ನ್ಯಾಷನಲ್", "ಉದಾ", "ಹಾಲ್", "ನೆಯ", "ಆಯು", "ಮಾರುತು"}
# The case endings the corpus writes apart after a number, an abbreviation or a Latin word (2010ರಲ್ಲಿ, ಐಐಟಿ ಯನ್ನು, X ಗಳನ್ನು),
# with the glide or stem consonant before them: no words, and no stems for induce (ದಲ್ಲಿ is no verb ದಲ್ಲು).
CASE_FRAGMENTS = {lead + tail for lead in ("", "ಯ", "ವ", "ದ", "ನ", "ರ", "ಗಳ", "ರವ", "ರವರ")
                  for tail in NOMINAL_TAILS + OBLIQUE_TAILS + DATIVE_TAILS + ["ಷ್ಟು", "ರು", "ನ್ನು", "ನ್ನೂ"] if lead + tail} - {"ನಂತರ", "ವಾದ", "ನಗೆ"}
# Spellings in the packs' words that are no words: the glosses keep a non-joiner or a dot.
GLOSS_WORD = re.compile(r"[ಀ-೥೰-೿]+")


def kind_of(kinds):
    """The kind the rules decline a word by: a verb is conjugated, a noun or an adjective declined, any other left as it is."""
    if "verb" in kinds:
        return "verb"
    if kinds & {"noun", "num"}:
        return "noun"
    if "adj" in kinds:
        return "adj"
    return "other"


def lexicon(dictionaries):
    """
    The Kannada words and their kinds: the packs' entries, the gloss packs' Kannada words, and KANNADA_WORDS; and those of
    them only the gloss packs have, which may be a form (ಮಾಡಲು, ದೇಶದ) given for a word. Names are left out, but for the
    languages' (ಕನ್ನಡ, ತುಳು).
    """
    kinds = defaultdict(set)
    names = set()
    for pack in ("kn-en", "kn-ru"):
        path = os.path.join(dictionaries, f"{pack}.sqlite")
        if not os.path.exists(path):
            continue
        for word, pos, senses in sqlite3.connect(path).execute("SELECT word_lc, pos, senses FROM entries"):
            # A language's name is an everyday word (ಕನ್ನಡ, ತುಳು); other names are left out.
            if pos == "name" and any("language" in g for s in json.loads(senses) for g in s.get("glosses", [])):
                pos = "noun"
            if pos == "name":
                names.add(word)
            # The Russian pack names a verb by its infinitive (ಮಾಡಲು): the word is the verb (ಮಾಡು, ಕುಡಿ of ಕುಡಿಯಲು).
            if pos == "verb" and word.endswith("ಲು") and len(word) > 3:
                word = word[:-3] if word[-3] == "ಯ" and word[-4] in "ಿೆ" else word[:-2] + "ು"
            if pos in WORD_KINDS and GLOSS_WORD.fullmatch(word):
                kinds[word].add(pos)
    packed = set(kinds) | set(KANNADA_WORDS)
    for pack in ("en-kn-gloss", "ru-kn-gloss"):
        path = os.path.join(dictionaries, f"{pack}.sqlite")
        if not os.path.exists(path):
            continue
        for pos, senses in sqlite3.connect(path).execute("SELECT pos, senses FROM entries"):
            for sense in json.loads(senses):
                for gloss in sense.get("glosses", []):
                    for part in re.split(r"[,;/]", gloss):
                        part = part.strip()
                        if not GLOSS_WORD.fullmatch(part) or len(part) < 2:
                            continue
                        if pos == "name" and not any("language" in t for t in sense.get("tags", [])):
                            names.add(part)
                        elif pos in GLOSS_KINDS:
                            kinds[part].add(GLOSS_KINDS[pos])
    for word, (kind, _) in KANNADA_WORDS.items():
        kinds[word].add(kind)
    for word in KANNADA_EXCLUDED | set(KANNADA_FORMS) | (names - packed):
        kinds.pop(word, None)
    return kinds, set(kinds) - packed, names - set(kinds)


def nominal_forms(word):
    """The case, plural and predicate forms of a noun or adjective, by its ending, with its masculine and human plural
    declensions apart (they are taken only where the corpus has them)."""
    last = word[-1]
    plain, masculine, human = set(), set(), set()
    plural = word + ("‌ಗಳ" if last == VIRAMA else "ಗಳ")
    plain |= {plural + t for t in PLURAL_TAILS}
    if last in CONSONANTS:
        glide = word + "ವ"
        plain |= {glide + t for t in NOMINAL_TAILS} | {word + t for t in DA_TAILS}
        masculine |= {word + "ನ" + t for t in NOMINAL_TAILS + OBLIQUE_TAILS}
        human |= {word + "ರ" + t for t in NOMINAL_TAILS + OBLIQUE_TAILS}
    elif last == "ು":
        stem = word[:-1]
        plain |= {stem + t for t in NOMINAL_TAILS + OBLIQUE_TAILS[1:] + INA_TAILS}
        # The Sanskrit nouns in -u and ಮಗು take the glide (ಗುರುವಿನ, ವಸ್ತುವನ್ನು, ಮಗುವಿಗೆ).
        glide = word + "ವ"
        plain |= {glide + t for t in NOMINAL_TAILS + ["ಿಗೆ", "ಿಗೂ", "ಿಗಾಗಿ"] + INA_TAILS}
    elif last in "ಿೀೆೇೈ" or last in "ಇಈಎಏಐ":
        glide = word + "ಯ"
        plain |= {glide + t for t in YA_TAILS} | {word + t for t in DATIVE_TAILS}
        human |= {glide + "ರ" + t for t in NOMINAL_TAILS + OBLIQUE_TAILS}
    elif last in "ಾೂೋೌ":
        glide = word + "ವ"
        plain |= {glide + t for t in NOMINAL_TAILS + ["ಿಗೆ"] + INA_TAILS} | {word + t for t in DATIVE_TAILS}
    elif last == VIRAMA:
        # A loanword ending in a consonant: ಬಸ್‌ನಲ್ಲಿ, ಕ್ರಿಕೆಟ್‌ನ, or with the vowel the case brings (ಕ್ರಿಕೆಟಿನ, ಕ್ರಿಕೆಟನ್ನು).
        for joined in (word + "‌", word):
            plain |= {joined + t for t in ["ನ", "ನಲ್ಲಿ", "ನಿಂದ", "ಗೆ", "ನ್ನು", "ನೊಂದಿಗೆ", "ನಂತೆ", "ಗಳು", "ಗಾಗಿ"]}
        stem = word[:-1]
        plain |= {stem + t for t in ["ಿನ", "ಿನಲ್ಲಿ", "ಿನಿಂದ", "ಿಗೆ", "ನ್ನು", "ು", "ೂ", "ೇ", "ಾಗಿ", "ಾದ"]}
    return plain, masculine, human


def adjective_forms(word):
    return {word + t for t in ADJECTIVE_TAILS} if word[-1] in CONSONANTS else set()


def verb_stems(word):
    """A verb's present stem, infinitive stem, gerund, past relative stem, neuter pasts and perfect stem, or None."""
    # A verb made with an irregular one is conjugated as it is (ಒಳಗೊಳ್ಳು as ಕೊಳ್ಳು, ಕಂಡುಬರು as ಬರು).
    helper = next((h for h in IRREGULAR if word.endswith(h) and len(word) > len(h)), None)
    if helper:
        lead = word[:-len(helper)]
        return tuple(lead + x if isinstance(x, str) else [lead + y for y in x] for x in verb_stems(helper))
    if word in IRREGULAR:
        gerund, past, neuter, perfect = IRREGULAR[word]
        if word.endswith("ು"):
            return word, word[:-1], gerund, past, neuter, perfect
        return word + "ಯು", word + "ಯ", gerund, past, neuter, perfect
    if word.endswith("ು") and len(word) > 2 and word[-2] in CONSONANTS:
        stem = word[:-1]
        return word, stem, stem + "ಿ", stem + "ಿದ", [stem + "ಿತು"], stem + "ಿ"
    if word[-1] in "ಿೆ" and len(word) > 1 and word[-2] in CONSONANTS:
        return word + "ಯು", word + "ಯ", word + "ದು", word + "ದ", [word + "ಯಿತು"], word + "ದಿ"
    return None


@functools.lru_cache(maxsize=None)
def simple_verb_forms(word):
    """A verb's own forms, without the passive and the compounds made with another verb."""
    stems = verb_stems(word)
    if stems is None:
        return frozenset(VERB_EXTRA.get(word, ()))
    present, infinitive, gerund, past, neuter, perfect = stems
    forms = {word, gerund, *neuter}
    forms |= {present + t for t in PRESENT_TAILS}
    forms |= {infinitive + t for t in INFINITIVE_TAILS}
    forms |= {past + t for t in PAST_TAILS}
    # The polite imperative: ಮಾಡಿ, ಮಾಡಿರಿ; ಕುಡಿಯಿರಿ.
    forms |= {gerund + "ರಿ"} if word.endswith("ು") else {word + "ಯಿರಿ", word + "ಯಿ"}
    # The progressive with ಇರು (ಮಾಡುತ್ತಿದ್ದಾನೆ) and the perfect (ಮಾಡಿದ್ದಾನೆ, ಮಾಡಿದೆ), from ಇರು's forms.
    if word != "ಇರು":
        for be in sorted(simple_verb_forms("ಇರು")):
            if be.startswith("ಇ") and len(be) > 1:
                forms.add(present + "ತ್ತಿ" + be[1:])
                forms.add(perfect + be[1:])
    forms |= set(VERB_EXTRA.get(word, ()))
    # The neuter pasts and the negative with ಇಲ್ಲ: ಮಾಡಿಲ್ಲ, ಮಾಡಿಲ್ಲವೇ.
    forms |= {perfect + "ಲ್ಲ", perfect + "ಲ್ಲವೇ", perfect + "ಲ್ಲದ"}
    return frozenset(forms)


def verb_forms(word, helpers):
    """A verb's forms with its passive (ಮಾಡಲಾಯಿತು, ಮಾಡಲ್ಪಟ್ಟ) and compounds with ಕೊಳ್ಳು and ಬಿಡು (ಮಾಡಿಕೊಂಡ, ಮಾಡಿಬಿಟ್ಟ)."""
    forms = set(simple_verb_forms(word))
    stems = verb_stems(word)
    if stems is None:
        return forms
    _, infinitive, gerund, _, _, _ = stems
    forms |= {infinitive + "ಲಾ" + f[1:] for f in helpers["ಆಗು"] if f.startswith("ಆ") and len(f) > 1}
    forms |= {infinitive + "ಲ್" + f for f in helpers["ಪಡು"]}
    for helper in ("ಕೊಳ್ಳು", "ಬಿಡು"):
        forms |= {gerund + f for f in helpers[helper]}
    return forms


def unclitic(form):
    """The forms [form] may be with a clitic ಊ "also, even", ಏ "the very" or ಆ (a question) joined on: ಮನೆಗೂ of ಮನೆಗೆ,
    ಅವನೇ of ಅವನು, ಇದೆಯೇ of ಇದೆ, ಬರುತ್ತೀಯಾ of ಬರುತ್ತೀಯ."""
    bases = set()
    if form.endswith(("ಯೂ", "ಯೇ", "ಯಾ")) and len(form) > 3:
        bases.add(form[:-2])
    if form[-1] in "ೂೇಾ" and len(form) > 2:
        bases |= {form[:-1] + "ು", form[:-1] + "ಿ", form[:-1] + "ೆ", form[:-1]}
    return bases


def word_forms(word, ks, counts, helpers):
    """The forms the rules give [word] of kinds [ks]: a masculine or human plural declension only where [counts] has
    TELLING_FORMS of its telling forms."""
    kind = kind_of(ks)
    forms = set()
    # A number that is a verb too (ಏಳು "seven", "to rise"; ಹತ್ತು "ten", "to climb") is the number alone.
    if kind == "verb" and "num" not in ks:
        forms |= verb_forms(word, helpers)
    if kind in ("noun", "adj") or ks & {"noun", "adj", "num"}:
        plain, masculine, human = nominal_forms(word)
        forms |= plain
        glide = word + "ಯ" if word[-1] in "ಿೀೆೇೈ" else word
        for extra, telling in ((masculine, ("ನನ್ನು", "ನಿಗೆ", "ನು")), (human, ("ರನ್ನು", "ರಿಗೆ", "ರು"))):
            if sum(1 for t in telling if counts.get(glide + t, 0) > 0) >= TELLING_FORMS:
                forms |= extra
    if "adj" in ks:
        forms |= adjective_forms(word)
    if "num" in ks and word.endswith("ು"):
        forms |= {word[:-1] + t for t in ORDINAL_TAILS}
    forms.discard(word)
    return forms


# Words the packs lack are found in the corpus itself: a common form no word of the lexicon explains is a noun or
# adjective of its own when it is written bare INDUCED_BARE times (ಅನೇಕ, ಪ್ರದೇಶ, ಸಾಮಾನ್ಯ), and a verb when INDUCED_FORMS of
# its forms are written INDUCED_VERB times in all (ಜನಿಸು of ಜನಿಸಿದರು, ಜನಿಸಿದ, ಜನಿಸುತ್ತಾರೆ); the commonest by their forms first,
# so a form goes to one of them only.
INDUCED_BARE = 20
# A word the corpus has in no form but the bare one must be written this often: below it, most are English words, names and
# case forms of names (ಏಷ್ಯಾದ, ಲಾರ್ಡ್, ಪೀಟರ್), which a noun of the language would not be without its cases.
INDUCED_LONE = 100
INDUCED_FORMS = 3
INDUCED_VERB = 30
# The endings a verb's form is told by, which no noun has (ಮಾಡಿದರು, ಮಾಡುತ್ತದೆ): the stem before them is the verb's.
VERB_ENDINGS_U = ["ಿದ", "ಿದರು", "ಿದನು", "ಿದಳು", "ಿತು", "ುತ್ತದೆ", "ುತ್ತಾರೆ", "ುತ್ತವೆ", "ುತ್ತಾನೆ", "ುವ", "ಲು", "ಿದ್ದಾರೆ", "ಿರುವ", "ಿದೆ",
                  "ಲಾಗುತ್ತದೆ", "ಲಾಯಿತು", "ಲಾಗಿದೆ", "ುವುದು", "ಿದ್ದರು", "ಿ"]
VERB_ENDINGS_IE = ["ದ", "ದರು", "ದನು", "ಯುತ್ತದೆ", "ಯುತ್ತಾರೆ", "ಯುವ", "ಯಲು", "ಯಿತು", "ದಿದೆ", "ಯಲಾಗುತ್ತದೆ"]
NOT_NOUN = ("ುತ್ತದೆ", "ುತ್ತಾರೆ", "ುತ್ತವೆ", "ುತ್ತಾನೆ", "ುತ್ತಾಳೆ", "ಿದರು", "ಿದನು", "ಿದಳು", "ಲಾಯಿತು", "ಿದ್ದಾರೆ", "ುವುದು", "ಿದ್ದರು",
            "ಲಾಗುತ್ತದೆ", "ಲಾಗಿದೆ", "ದರು", "ದನು", "ದಳು", "ಿತ್ತು", "ಿದ್ದವು", "ಿವೆ", "ಿದೆ", "ಿತು", "ಯಿತು", "ದ್ದಾರೆ")


def induce(unexplained, counts, helpers):
    """The words the corpus's forms [unexplained] (form: count) are of, with their kinds."""
    candidates = {}
    for form, n in unexplained.items():
        if n >= INDUCED_BARE and len(form) > 2 and not form.endswith(NOT_NOUN) and form[-1] != "\u200c":
            candidates[form] = {"noun"}
        for endings, back in ((VERB_ENDINGS_U, "ು"), (VERB_ENDINGS_IE, "")):
            for ending in endings:
                if form.endswith(ending) and len(form) > len(ending) + 1:
                    stem = form[:-len(ending)]
                    if back and stem[-1] not in CONSONANTS or not back and stem[-1] not in "ಿೆ":
                        continue
                    verb = stem + back
                    # A verb in -i or -e takes the glide before -ಇತು and -ಉತ್ತದೆ (ದೊರೆಯಿತು of ದೊರೆ, not ದೊರೆಯು).
                    if back and stem[-1] == "ಯ" and len(stem) > 2 and stem[-2] in "ಿೆ":
                        verb = stem[:-1]
                    if verb not in candidates:
                        candidates[verb] = {"verb"}
    # A verb is worth conjugating only when INDUCED_FORMS forms begin as its stem does and come to INDUCED_VERB.
    ordered = sorted(unexplained)
    starts = [0]
    for form in ordered:
        starts.append(starts[-1] + unexplained[form])

    def begun(prefix):
        low, high = bisect.bisect_left(ordered, prefix), bisect.bisect_left(ordered, prefix + "\uffff")
        return high - low, starts[high] - starts[low]

    # The forms each written form may be without its clitic.
    under = defaultdict(set)
    for form in unexplained:
        for key in unclitic(form) | {form}:
            under[key].add(form)
    # A verb made with an irregular one (ಒಳಗೊಂಡಿದೆ of ಒಳಗೊಳ್ಳು, ಕಂಡುಬರುತ್ತದೆ of ಕಂಡುಬರು).
    for form, n in unexplained.items():
        for helper, (gerund, past, _, _) in IRREGULAR.items():
            for stem in (past, helper[:-1] if helper.endswith("ು") else helper):
                at = form.find(stem, 2)
                if at > 0:
                    candidates.setdefault(form[:at] + helper, {"verb"})
    paradigms = {}
    for word, ks in candidates.items():
        if "verb" in ks:
            number, mass = begun(word[:-1] if word.endswith("ು") else word)
            if number < INDUCED_FORMS or mass < INDUCED_VERB:
                continue
        forms = set()
        for form in word_forms(word, ks, counts, helpers) | {word}:
            forms |= under.get(form, set())
        if "verb" in ks and (len(forms) < INDUCED_FORMS or sum(unexplained[f] for f in forms) < INDUCED_VERB):
            continue
        paradigms[word] = forms
    # Lazy greedy: a word's mass only falls as others claim its forms, so one popped with its mass unchanged is the best left.
    claimed, chosen = set(), {}
    heap = [(-sum(unexplained[f] for f in forms), word) for word, forms in paradigms.items()]
    heapq.heapify(heap)
    while heap:
        negative, word = heapq.heappop(heap)
        forms = paradigms[word] - claimed
        mass = sum(unexplained[f] for f in forms)
        if mass < -negative:
            paradigms[word] = forms
            if mass > 0:
                heapq.heappush(heap, (-mass, word))
            continue
        if "noun" in candidates[word] and (word in claimed or unexplained.get(word, 0) < INDUCED_BARE):
            continue
        if "noun" in candidates[word] and forms <= {word} and unexplained.get(word, 0) < INDUCED_LONE:
            continue
        if "verb" in candidates[word] and (len(forms) < INDUCED_FORMS or mass < INDUCED_VERB):
            continue
        chosen[word] = candidates[word]
        claimed |= forms | {word}
    return chosen


def build(found, size, label):
    """The list: the words of the lexicon and those the corpus shows (induce), by their forms' counts in [found]."""
    dictionaries = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
    kinds, glossed, names = lexicon(dictionaries)
    counts = defaultdict(float)
    raw = defaultdict(int)
    for _, source, _ in found:
        total = sum(source.values())
        for form, n in source.items():
            if KANNADA_LETTERS.fullmatch(form) and not STRAY_JOINER.search(form):
                counts[form] += n / total / len(found)
                raw[form] += n

    helpers = {h: simple_verb_forms(h) for h in ("ಆಗು", "ಪಡು", "ಕೊಳ್ಳು", "ಬಿಡು")}
    by_hand = defaultdict(set)
    for word, (_, forms) in KANNADA_WORDS.items():
        for form in forms.split():
            by_hand[form].add(word)
    for form, word in KANNADA_FORMS.items():
        by_hand[form].add(word)

    # The forms the rules are asked for: those written, and those they may be without a clitic.
    needed = set(counts)
    for form in counts:
        needed |= unclitic(form)

    def rule(words):
        ruled = defaultdict(set)
        for word, ks in words.items():
            for form in word_forms(word, ks, counts, helpers):
                if form in needed:
                    ruled[form].add(word)
        return ruled

    def plain_targets(form):
        if form in by_hand:
            return by_hand[form]
        if form in kinds and not (form in glossed and ruled.get(form, set()) - {form}):
            return {form}
        return ruled.get(form, set())

    def targets(form):
        if form in KANNADA_EXCLUDED or form in KANNADA_NOT_WORDS or form in CASE_FRAGMENTS:
            return set()
        heads = plain_targets(form)
        if not heads:
            for base in unclitic(form):
                heads = heads | plain_targets(base)
        return heads

    ruled = rule(kinds)
    unexplained = {f: n for f, n in raw.items()
                   if not targets(f) and f not in KANNADA_EXCLUDED and f not in KANNADA_NOT_WORDS and f not in CASE_FRAGMENTS}
    # A word induce finds is none that is a form of a word already known (ಇದ್ದು of ಇರು, from ಇದ್ದಿತು).
    induced = {w: ks for w, ks in induce(unexplained, counts, helpers).items()
               if w not in KANNADA_NOT_WORDS and w not in CASE_FRAGMENTS and w not in names and not targets(w)
               # nor a locative with ಇದೆ or ಇರುವ joined on (ರೀತಿಯಲ್ಲಿದೆ is no verb ರೀತಿಯಲ್ಲು).
               and not (w.endswith("ಲ್ಲು") and targets(w[:-1] + "ಿ"))}
    for word, ks in induced.items():
        kinds[word] |= ks
    for form, heads in rule(induced).items():
        ruled[form] |= heads

    # A form of several words is shared by how common each is by its forms of no other word.
    own = defaultdict(float)
    found_targets = {}
    for form, share in counts.items():
        heads = {KANNADA_SPELLINGS.get(h, h) for h in targets(form)}
        found_targets[form] = heads
        if len(heads) == 1:
            own[next(iter(heads))] += share
    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, share in counts.items():
        heads = found_targets[form]
        if not heads:
            continue
        weights = {h: own.get(h, 0.0) for h in heads}
        whole = sum(weights.values())
        for head in heads:
            part = weights[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    least = LEAST_COUNT * len(found) / sum(sum(source.values()) for _, source, _ in found)
    ranked = [w for w in sorted(scores, key=lambda w: -scores[w]) if scores[w] >= least][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


# The checker (check_course.py) reads a word the pinned list does not have by these rules: ಮಾಡುತ್ತೇನೆ is ಮಾಡು's, ಹುಡುಗನಿಗೆ
# ಹುಡುಗ's and ರಾಮನಿಗೆ the name ರಾಮ's, though a Wikipedia corpus has few of them. Every declension is taken here (ALL_TELLING).
class _AllTelling(dict):
    def get(self, key, default=None):
        return 1


ALL_TELLING = _AllTelling()
_HELPERS = {}
_CHECKER_INDEX = {}


def _helpers():
    if not _HELPERS:
        _HELPERS.update({h: simple_verb_forms(h) for h in ("ಆಗು", "ಪಡು", "ಕೊಳ್ಳು", "ಬಿಡು")})
    return _HELPERS


def _checker_kinds(word, listed):
    """A listed word's kinds as the rules decline it: a verb when its listed forms are a verb's, a noun or adjective too."""
    ks = {"noun", "adj"}
    hand = KANNADA_WORDS.get(word)
    if hand and hand[0] not in ("noun", "adj", "verb"):
        return {hand[0]}
    if verb_stems(word) is not None and (hand and hand[0] == "verb" or set(listed) & (simple_verb_forms(word) - {word})):
        ks.add("verb")
    return ks


@functools.lru_cache(maxsize=None)
def _rule_forms(word, kinds):
    return frozenset(word_forms(word, set(kinds), ALL_TELLING, _helpers())) | {word}


@functools.lru_cache(maxsize=None)
def _name_forms(name):
    """A name's case forms: a noun's, and a woman's name in -ā with ಳ (ಸೀತಾಳ, ಸೀತಾಳಿಗೆ)."""
    forms = _rule_forms(name, frozenset({"noun"}))
    if name.endswith("ಾ"):
        forms |= {name + "ಳ" + t for t in NOMINAL_TAILS + OBLIQUE_TAILS}
    return forms


def _stems(word, kinds):
    """The beginnings a form of [word] starts with, to look its forms up by."""
    starts = {word, word[:-1]} if len(word) > 1 else {word}
    if "verb" in kinds:
        for stem in verb_stems(word) or ():
            starts |= set(stem) if isinstance(stem, list) else {stem}
    return {s for s in starts if s}


def checker_word(token, words, ranks, names=frozenset()):
    """
    The list's word [token] is a form of by the rules, as a key of [words] (the checker's list: key -> (rank, word, forms)),
    the commonest when it may be several; "\\0name" when it is a case form of one of the course's [names]; or None.
    A form with a clitic (ಮನೆಗೂ, ಅವನೇ) is read as the form without it.
    """
    for base in sorted(unclitic(token)):
        if base in ranks:
            return base
    index = _CHECKER_INDEX.get(id(words))
    if index is None:
        index = defaultdict(set)
        for key, (_, _, listed) in words.items():
            kinds = frozenset(_checker_kinds(key, listed))
            for stem in _stems(key, kinds):
                index[stem].add((key, kinds))
        _CHECKER_INDEX.clear()
        _CHECKER_INDEX[id(words)] = index
    candidates = {token} | unclitic(token)
    for name in names:
        if name.startswith("\u0000"):
            continue
        if any(c.startswith(name[:-1] if len(name) > 1 else name) for c in candidates) and candidates & _name_forms(name):
            return "\u0000name"
    found = []
    for end in range(len(token), 0, -1):
        for key, kinds in index.get(token[:end], ()):
            if candidates & _rule_forms(key, kinds):
                found.append((words[key][0], key))
    return min(found)[1] if found else None
