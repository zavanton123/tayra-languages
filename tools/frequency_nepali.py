"""
The Nepali frequency list (build_frequency_list.py --language ne).

Nepali writes a noun's postpositions joined to it (घरमा "in the house", रामको "Ram's", केटाहरूलाई "to the boys", घरबाटै
"right from home"), and a verb's helpers after its conjunctive or infinitive (गरिरहेको "doing", गर्नुपर्छ "must do",
गरेपछि "after doing"). The pack (dictionaries/ne-en.sqlite, from the English Wiktionary) has tables for most verbs and some
nouns, but most nouns have none, and its pronoun tables list every pronoun on each pronoun's page. So the pronouns are filed
by hand (PRONOUNS), and a corpus form is read as a pack word, a table's form, or a word with endings joined on by the rules
below (nominal_forms; JOINED: what follows a verb's participle, infinitive or conjunctive). A form that is a word of the
pack itself keeps its count (भने "if", not भन्नु's "said"), unless the word it is also a verb form of is more common than the
form; it is never taken for a noun with an ending (आमा "mother", not आ + मा). A form of several words is shared between them by
how common each is by its forms of no other word.

Counted from news and web text (Leipzig's news of 2020 and its crawl of Nepali web pages of 2015), film subtitles (OPUS
OpenSubtitles) and Leipzig's Wikipedia sentences, each the share of the blend WEIGHTS gives.
"""
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

LEIPZIG = {"news": ["nep_news_2020_300K", "nep-np_web_2015_1M"], "wikipedia": ["nep_wikipedia_2021_100K"]}
SUBTITLES = "https://object.pouta.csc.fi/OPUS-OpenSubtitles/v2024/mono/ne.txt.gz"
# Subtitles bring the words of talk (मलाई, तिमी, किन), news and web pages the written language's, Wikipedia its rarer ones; it
# is kept small, as much of it is pages on villages and districts (गाविस, जिल्ला).
WEIGHTS = {"news": 0.5, "subtitles": 0.4, "wikipedia": 0.1}
LICENCES = {
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "OpenSubtitles": "terms at https://opus.nlpl.eu/OpenSubtitles/corpus/version/OpenSubtitles",
}

# As the reader's Nepali wordCharacters, without the Latin letters (no Nepali words) and the digits (no word characters).
LETTERS = re.compile("[\u0900-\u0963\u0971-\u097f\u200c\u200d]+")
# A joiner belongs only after a virama (गर्‍यो); elsewhere it is a typing slip.
STRAY_JOINER = re.compile("(?<!\u094d)[\u200c\u200d]")
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep", "classifier",
              "counter"}
# A word must be counted this often (all its forms, all sources) to be listed: below it the corpus's typos.
LEAST_COUNT = 5
FORMS_LISTED = 400

PLURALS = ["", "हरू", "हरु"]
# The postpositions written joined to a noun (घरमा, घरबाट, घरभन्दा, घरजस्तो), and those that take one more after them
# (घरभित्रको, घरनजिकै, घरतिरबाट is rare and left out).
CASES = ["ले", "लाई", "को", "का", "की", "मा", "बाट", "देखि", "सँग", "संग", "सम्म", "तिर", "भन्दा", "जस्तो", "जस्ता", "जस्ती",
         "भित्र", "माथि", "मुनि", "पछि", "अघि", "बिना", "विना", "सित", "तर्फ", "पट्टि", "भरि", "द्वारा", "प्रति", "अनुसार",
         "सहित", "बीच", "बाहेक", "निम्ति", "लागि", "नजिक", "अगाडि", "पछाडि", "मार्फत", "वाला", "वाली", "बारे", "भर"]
STACKED = ["भित्र", "माथि", "मुनि", "पछि", "अघि", "बीच", "तिर", "सम्म", "देखि", "नजिक", "अगाडि", "पछाडि", "तर्फ", "पट्टि",
           "सँग", "संग", "बाट", "बाहिर", "भन्दा", "बारे"]
SECOND = ["को", "का", "की", "मा", "ले", "लाई"]
# The emphatic नै, also written as -ै on the genitive and locative (उसकै, घरमै), and पनि "also" when written on (मपनि).
CLITICS = ["नै", "पनि", "चाहिँ", "चाहिं"]
# The classifiers written on a number (दुईवटा, तीनजना) and an ordinal's ending.
NUMBER_TAILS = ["वटा", "ओटा", "टा", "जना", "थरी", "चोटि", "पटक", "औं", "औँ", "ौं", "ौँ"]

# What follows a verb's forms, by the form's ending: a participle's postpositions (गरेकोले, गर्नेहरू), a past conditional's
# (गरेपछि, भएपनि), an infinitive's (गर्नुपर्छ is पर्नु's forms on it), a conjunctive's helper verbs (गरिरहेको, गरिदियो,
# गरिसक्यो), a progressive's copula (गर्दैछ).
HELPERS = ["रहनु", "दिनु", "सक्नु", "हाल्नु", "पठाउनु", "बस्नु", "लिनु", "छाड्नु", "छोड्नु"]
AFTER_E = ["पछि", "पनि", "तापनि", "मा", "देखि", "सँगै", "संगै", "अनुसार", "जस्तो", "जस्तै", "नै"]
# The honorific's auxiliary (गर्नुहुन्छ, गर्नुभयो) where a table lacks it.
AFTER_NU = ["को", "का", "की", "मा", "अघि", "अगाडि", "पूर्व", "भन्दा", "नै", "लाई", "ले", "सम्म", "पर्ने", "पर्छ", "हुन्छ", "हुन्न",
            "हुन्थ्यो", "हुँदैन", "हुन्छन्", "भयो", "भएको", "भएका", "भएकी", "होस्", "हुने", "हुँदा", "भए", "हुनेछ"]
# And a verbal noun's -ाले "because of" (हुनाले, गर्नाले).
AFTER_N = ["का", "को", "लाई", "कै", "सम्म", "नै", "लागि", "ाले"]
AFTER_DAI = ["छ", "छन्", "छु", "छौं", "छौँ", "छौ", "छिन्", "थियो", "थिए", "थिइन्", "गर्दा"]
AFTER_DA = ["खेरि", "खेरी", "सम्म", "मा", "नै"]

# The pronouns, filed by hand (the pack's tables list every pronoun on each one's page): word -> its forms. The polite and
# honorific pronouns are written with ं, ँ or without the nasal (तपाईं, तपाईँ, तपाई).
PRONOUNS = {
    "म": "मैले मलाई मेरो मेरा मेरी ममा मबाट मसँग मसंग मजस्तो मेरै मलाईनै मैलेनै",
    "हामी": "हामीले हामीलाई हाम्रो हाम्रा हाम्री हामीमा हामीबाट हामीसँग हामीसंग हाम्रै हामीहरू हामीहरु हामीहरूले हामीहरुले "
            "हामीहरूलाई हामीहरुलाई हामीहरूको हामीहरुको हामीहरूमा हामीहरूसँग",
    "तँ": "तैँले तैंले तैले तँलाई तेरो तेरा तेरी तँसँग तेरै",
    "तिमी": "तिमीले तिमीलाई तिम्रो तिम्रा तिम्री तिमीमा तिमीबाट तिमीसँग तिमीसंग तिम्रै तिमीहरू तिमीहरु तिमीहरूले तिमीहरुले "
            "तिमीहरूलाई तिमीहरुलाई तिमीहरूको तिमीहरुको तिमीहरूमा तिमीहरूसँग",
    "तपाईं": "तपाईँ तपाई तपाईंले तपाईँले तपाईले तपाईंलाई तपाईँलाई तपाईलाई तपाईंको तपाईँको तपाईको तपाईंका तपाईँका तपाईका "
             "तपाईंकी तपाईकी तपाईंमा तपाईमा तपाईंसँग तपाईसँग तपाईंसंग तपाईसंग तपाईंबाट तपाईबाट तपाईंकै तपाईकै "
             "तपाईंहरू तपाईँहरू तपाईहरू तपाईंहरु तपाईहरु तपाईंहरूले तपाईहरूले तपाईंहरूलाई तपाईहरूलाई तपाईंहरूको तपाईहरूको",
    "ऊ": "उस उसले उसलाई उसको उसका उसकी उसमा उसबाट उससँग उससंग उसकै उसैले उसैलाई",
    "उनी": "उनि उनले उनलाई उनको उनका उनकी उनमा उनबाट उनीसँग उनीसंग उनसँग उनकै उनैले उनीहरू उनीहरु उनीहरूले उनीहरुले "
           "उनीहरूलाई उनीहरुलाई उनीहरूको उनीहरुको उनीहरूका उनीहरुका उनीहरूमा उनीहरुमा उनीहरूसँग उनीहरूबाट",
    "उहाँ": "उहाँले उहाँलाई उहाँको उहाँका उहाँकी उहाँमा उहाँसँग उहाँसंग उहाँबाट उहाँकै उहाँहरू उहाँहरु उहाँहरूले "
            "उहाँहरूलाई उहाँहरूको उहा उहाले उहालाई उहाको",
    "यो": "यस यसले यसलाई यसको यसका यसकी यसमा यसबाट यससँग यसरी यसकारण यसकै यसैले यसैमा यसैको",
    "यी": "यिनले यिनलाई यिनको यिनका यिनमा यिनी यिनीहरू यिनीहरु यिनीहरूले यिनीहरूलाई यिनीहरूको",
    "त्यो": "त्यस त्यसले त्यसलाई त्यसको त्यसका त्यसकी त्यसमा त्यसबाट त्यससँग त्यसरी त्यसकारण त्यसकै त्यसैले त्यसैमा त्यसैको",
    "ती": "तिनले तिनलाई तिनको तिनका तिनमा तिनी तिनीहरू तिनीहरु तिनीहरूले तिनीहरुले तिनीहरूलाई तिनीहरुलाई तिनीहरूको तिनीहरुको",
    "के": "केले केलाई केको केका केमा केबाट",
    "को": "कसले कसलाई कसको कसका कसकी कसमा कससँग कसबाट",
    "जो": "जस जसले जसलाई जसको जसका जसकी जसमा जससँग जसबाट जसरी",
    "कोही": "कसैले कसैलाई कसैको कसैका कसैमा कसैसँग",
    "आफू": "आफूले आफूलाई आफूमा आफूसँग आफूबाट आफ्नो आफ्ना आफ्नी आफ्नै आफैं आफैँ आफै आफैले आफैंले आफैलाई",
    "सबै": "सबैले सबैलाई सबैको सबैका सबैमा सबैसँग सबैभन्दा सबैजना",
}
# The copulas, filed apart from हुनु as the pack has them (छ, हो and their negatives are its entries): छ "is (there, so)", हो
# "is (it, he)", in each person.
COPULAS = {
    "छ": "छन छन् छु छौं छौँ छौ छिन् छे छस् छेस् छ्यौ",
    "छैन": "छैनन् छैनौं छैनौँ छैनौ छैनस् छैनस छैनँ छिनँ",
    "हो": "हुन् हुँ हौं हौँ हौ",
    "होइन": "होइनन् होइनौं होइनौँ होइनौ होइनस् होइनँ हैन हैनन् होईन",
}
NEPALI_FORMS = {form: word for table in (PRONOUNS, COPULAS) for word, forms in table.items() for form in [word] + forms.split()}
# The genitive's agreeing forms written apart (after a number or a foreign word: २०७७ का, COVID को) are को's.
NEPALI_FORMS.update({"का": "को", "की": "को"})
# A pronoun's other spellings (आफु, ति, यि), its oblique with the emphatic (तिनै, यिनै), and आआफ्नो "each their own".
NEPALI_FORMS.update({"आफु": "आफू", "आआफ्नो": "आफू", "आआफ्ना": "आफू", "आआफ्नै": "आफू", "ति": "ती", "तिन": "ती", "तिनै": "ती",
                     "यि": "यी", "यिन": "यी", "यिनै": "यी"})
# हुन्न "is not, will not be", which the pack has as a particle, is हुनु's negative present.
NEPALI_FORMS.update({"हुन्न": "हुनु", "हुन्नन्": "हुनु", "हुन्नँ": "हुनु"})
# Words a rule or a table would send to another word: केही "some" is no form of के.
OWN = {"केही"}
# Everyday words the pack lacks, with their kind as the rules decline them: function words, and verbs, whose forms are made by
# rule (regular_verb_forms).
NEPALI_WORDS = {
    "हे": "intj", "अझै": "adv", "साँच्चै": "adv", "मात्र": "adv", "ल्याउनु": "verb", "पठाउनु": "verb", "थाल्नु": "verb",
    "हाल्नु": "verb", "लैजानु": "verb",
    "जस्तो": "postp", "बढी": "adj", "न": "particle", "पटक": "noun", "साथै": "adv", "बमोजिम": "postp", "अँ": "intj",
    "त्यस्तो": "adj", "यस्तो": "adj", "सञ्चालन": "noun", "दर्ता": "noun", "निकै": "adv", "रकम": "noun", "जे": "pron", "गते": "noun",
    "है": "particle", "कहिल्यै": "adv", "यति": "adv", "अन्तिम": "adj", "दोस्रो": "adj", "माथि": "postp", "त्यसो": "adv",
    "ओह": "intj", "चिन्ता": "noun", "महसुस": "noun", "सोही": "pron", "फिर्ता": "noun", "भनाइ": "noun", "हक": "noun",
    "दुवै": "pron", "बारे": "postp", "रुपैयाँ": "noun", "वास्तवमा": "adv", "मद्दत": "noun", "सम्पन्न": "adj",
    "सम्बन्धित": "adj", "दाबी": "noun", "त्यति": "adv", "पछिल्लो": "adj", "तयारी": "noun", "खारेज": "adj", "छलफल": "noun",
    "पक्कै": "adv", "जवाफ": "noun", "सम्बन्ध": "noun", "अन्तर्गत": "postp", "एकै": "adj", "व्यवस्थापन": "noun",
    "केन्द्रीय": "adj", "पक्राउ": "noun", "अनुसन्धान": "noun", "कानूनी": "adj", "मध्ये": "postp", "नम्बर": "noun",
    "सम्पूर्ण": "adj", "बजेट": "noun", "नाइँ": "intj", "गम्भीर": "adj", "सधैं": "adv", "सम्भावना": "noun", "सायद": "adv",
    "अलिक": "adv", "एक्लै": "adv", "केन्द्र": "noun", "सम्पत्ति": "noun", "सजाय": "noun", "हँ": "intj", "सम्झौता": "noun",
    "अझ": "adv", "जति": "adv", "नियन्त्रण": "noun", "कतै": "adv", "थोरै": "adj", "बलियो": "adj", "अर्थात्": "conj",
    "कतिपय": "adj", "बाहेक": "postp", "मार्फत": "postp", "ज्यान": "noun", "जिम्मेवारी": "noun", "अनुहार": "noun",
    "कर्जा": "noun", "अघिल्लो": "adj", "उपर": "postp", "लगायत": "postp", "आन्तरिक": "adj", "जरुरी": "adj", "म्याद": "noun",
    "कमजोर": "adj", "तरिका": "noun", "खेलाडी": "noun", "फेला": "adv", "ख्याल": "noun", "भर्खरै": "adv", "हालसम्म": "adv",
    "जोड": "noun", "अन्त्य": "noun", "चाँडै": "adv", "माफी": "noun", "सवारी": "noun", "भर्ना": "noun", "सम्भव": "adj",
    "प्रतिवेदन": "noun", "प्राविधिक": "adj", "कैद": "noun", "मन्दिर": "noun", "छिट्टै": "adv", "भुक्तानी": "noun",
    "मूर्ख": "adj", "पछाडि": "postp", "गाडी": "noun", "सम्झना": "noun", "राजीनामा": "noun", "झगडा": "noun", "तसर्थ": "conj",
    "आम्दानी": "noun", "भन्सार": "noun", "उडान": "noun", "बरु": "adv", "थुप्रै": "adj", "सिर्जना": "noun", "अचम्म": "noun",
    "खाल": "noun", "गोप्य": "adj", "सिद्धान्त": "noun", "फोटो": "noun", "तर्फ": "postp", "बर्बाद": "adj", "खडा": "adj",
    "सम्बोधन": "noun", "चिनियाँ": "adj", "निरन्तर": "adv", "कलेज": "noun", "सङ्कलन": "noun", "रोजगारी": "noun",
    "पो": "particle", "किसिम": "noun", "ठहर": "noun", "मिटर": "noun", "पुनरावेदन": "noun", "दफा": "noun", "शेयर": "noun",
    "चिन्तित": "adj", "मन्त्रालय": "noun", "निवेदक": "noun", "प्लिज": "intj",
    "बढ्नु": "verb", "पार्नु": "verb", "जनाउनु": "verb", "घट्नु": "verb", "चलाउनु": "verb", "फर्कनु": "verb", "तोक्नु": "verb",
    "झण्डै": "adv", "बिहे": "noun", "भर्खर": "adv", "सल्लाह": "noun", "छुट्टै": "adv", "फाइदा": "noun", "नगद": "noun",
    "झन्": "adv", "ढिला": "adj", "चौथो": "adj", "सहभागिता": "noun", "सामेल": "adj", "घरेलु": "adj", "छनोट": "noun",
    "पूँजी": "noun", "खै": "intj", "तालिम": "noun", "कम्तीमा": "adv", "चासो": "noun", "सञ्चार": "noun", "पीडित": "adj",
    "ठ्याक्कै": "adv", "कता": "adv", "अत्यन्त": "adv", "सुरुवात": "noun", "उता": "adv", "वरिपरि": "adv", "हेरचाह": "noun",
    "कपडा": "noun", "ग्यास": "noun", "कोष": "noun", "जागिर": "noun", "बसोबास": "noun", "प्रारम्भिक": "adj",
    "किलोमिटर": "noun", "जरिवाना": "noun", "अन्तरिम": "adj", "प्रमाणपत्र": "noun", "मापदण्ड": "noun", "आन्दोलन": "noun",
    "निरन्तरता": "noun", "टिम": "noun", "मिलियन": "num", "विगत": "adj", "जिम्मा": "noun", "प्रष्ट": "adj", "निकाय": "noun",
    "सन्दर्भ": "noun", "साउन": "noun", "भदौ": "noun", "असोज": "noun", "कात्तिक": "noun", "मंसिर": "noun", "पुस": "noun",
    "चैत": "noun", "सुरु": "noun", "त्यसोभए": "adv", "त्यसैगरी": "adv", "यसो": "adv", "अवस्थित": "adj", "हरे": "intj",
    "आनन्द": "noun", "गोली": "noun",
}
# And those the bands past 2300 lacked: words the corpus writes often that neither pack has (बेच्नु, घुम्नु, चामल, माटो,
# गाउँपालिका; दल "party", which the pack has only as a form of दल्नु). आज and अब take postpositions (आजको, अबदेखि).
NEPALI_WORDS.update({word: "verb" for word in (
    "जोड्नु पुर्याउनु बोक्नु छाड्नु हटाउनु मनाउनु घटाउनु समेट्नु अपनाउनु सार्नु बेच्नु आइपुग्नु फैलिनु फैलाउनु भर्नु झर्नु "
    "जन्मनु बचाउनु भित्र्याउनु भित्रिनु दिलाउनु जोगाउनु सर्नु ठान्नु घुम्नु बिताउनु भोग्नु जुट्नु बित्नु सघाउनु जुटाउनु भाग्नु "
    "बच्नु बेहोर्नु फर्काउनु ओगट्नु सम्हाल्नु हट्नु छुट्याउनु खिच्नु रोज्नु छाप्नु खुलाउनु छिर्नु धान्नु उक्लनु रोप्नु "
    "स्वीकार्नु बदल्नु तोड्नु डुब्नु बगाउनु तुल्याउनु रुचाउनु निम्त्याउनु गुम्नु रमाउनु बाल्नु पछ्याउनु फुट्नु खुल्नु "
    "बाहिरिनु खटिनु खटाउनु लानु").split()})
NEPALI_WORDS.update({word: "noun" for word in (
    "कामदार बालबालिका उपत्यका यात्रु पहिरो खानेपानी उद्धार दबाब विज्ञ मुनाफा बासिन्दा परम्परा चाहना भेला जात भाउ नतिजा "
    "उजुरी बाढी ढुवानी माटो फोहोर सन्तान आफन्त कुराकानी कमजोरी बानी जोडी सम्पदा उकालो ओरालो भीड टोल पोखरी छोराछोरी "
    "किनार छेउ सिमाना घरपरिवार भोलिपल्ट जात्रा चाड चाडपर्व गहुँ उज्यालो हतार घाँटी ज्याला सुत्केरी मिर्गौला अगुवा "
    "चामल बेरोजगारी कठिनाइ नोक्सानी सरसफाइ सिँचाइ जडीबुटी आगलागी भेटघाट कागजात धितो खानी समझदारी ठेक्का सहुलियत "
    "हाराहारी सरोकार पुस्ता मजदुर स्वास्थ्यकर्मी इन्धन सन्धि आशङ्का साक्षरता अपाङ्गता थालनी ढिलाइ किनबेच सन्तुलन "
    "सम्पादक मह कब्जा विपद् सोचाइ सिफारिस भान्सा कमैया दल गाउँपालिका महासंघ लगानीकर्ता अर्थतन्त्र "
    "गुणस्तर सहकार्य परिषद् जनशक्ति महोत्सव सञ्चालक जलविद्युत् प्रवर्द्धन आज अब").split()})
NEPALI_WORDS.update({word: "adj" for word in "पहाडी प्रशस्त गहिरो चाँडो ढुक्क बेरोजगार बेपत्ता दोब्बर".split()})
NEPALI_WORDS.update({word: "adv" for word in (
    "हालै धेरैजसो तुरुन्त अन्यत्र वरपर त्यसबेला त्यतिबेला यसपटक यतिबेला यसपालि पटकपटक उत्तिकै बारम्बार एकैपटक").split()})
NEPALI_WORDS.update({"डेढ": "num", "एकअर्का": "pron", "त्यहीँ": "adv"})
# A verb's other spellings (फर्किनु, the stem in -इ the news write): their forms by rule are the verb's.
VERB_SPELLINGS = {"फर्किनु": "फर्कनु", "निस्किनु": "निस्कनु", "उक्लिनु": "उक्लनु", "जन्मिनु": "जन्मनु"}
# Forms the pack's tables lack or make wrongly: जानु's past is on the stem ग- (गयो, गएर), and हुनु's optative is होस्.
EXTRA_FORMS = {
    "जानु": "जाउँ जाउ जाऊँ गए गएर गई गइ गयो गएँ गएं गयौं गयौँ गयौ गइन् गइनन् गएन गएनन् गएछ गएछन् गइरहेको गएका गएकी गएको",
    "हुनु": "होस् होस हुँदा हुँदै हुँदैन हुँदैनन् हुँदो हुँदिन हुन्न भो भै हुनुहुन्थ्यो हुनाले",
    "भन्नु": "भन्या",
    "आउनु": "आउ आउँ",
    "दिनु": "देउ दिउँ",
    "जस्तो": "जस्तै जस्ता जस्ती",
    "रहनु": "रहन्छ रहन्छन् रह्यो रहँदैन रहँदा रहँदै रहेन",
    "चाहनु": "चाहेको चाहेका चाहेकी चाहे चाहेर चाहँदैन चाह्यो चाहँदा चाहिँदैन",
    # मन पर्नु "to like", written joined (मनपर्छ).
    "पर्नु": "मनपर्छ मनपर्ने मनपर्यो मनपर्‍यो मनपर्दैन मनपरेको मनपर्छन् मनपर्दो",
    # लानु "take away" makes its past on the stem लग- (लगेर, लग्यो).
    "लानु": "लान लाने लान्छ लान्छन् लान्छु लाँदै लाँदा लगेर लगे लग्यो लगेको लगेका लगेकी लगिन् लगें लगेँ लग्यौं लग्दै लग्दा "
            "लगिदियो लगिदिए लगिदिन्छ लगिन्छ लगियो लगिएको नलगेको नलान",
}
# A word's other spellings, folded into the spelling the list shows (the pack has some as words of their own): ठिक as ठीक,
# केहि as केही, the reformed spellings the news write (महिना, वर्ष) and the older ones the pack keeps.
SPELLINGS = {
    "ठिक": "ठीक", "केहि": "केही", "महीना": "महिना", "बर्ष": "वर्ष", "तिमि": "तिमी", "सहि": "सही", "राती": "राति", "रुप": "रूप",
    "बिभिन्न": "विभिन्न", "राष्ट्रीय": "राष्ट्रिय", "वहाँ": "उहाँ", "जमीन": "जमिन", "धन्यबाद": "धन्यवाद", "उ": "ऊ", "करीब": "करिब",
    "खरीद": "खरिद", "कानुन": "कानून", "दश": "दस", "नैं": "नै", "चाहिं": "चाहिँ", "अगाडी": "अगाडि", "त्यहि": "त्यही", "अरु": "अरू",
    "कारवाही": "कारबाही", "शुरु": "सुरु", "शुरू": "सुरु", "बुवा": "बुबा", "हिंड्नु": "हिँड्नु", "सङ्ख्या": "संख्या",
    "जनसङ्ख्या": "जनसंख्या", "बैङ्क": "बैंक", "सङ्गीत": "संगीत", "पुरै": "पूरै", "बिक्री": "विक्री", "महत्त्वपूर्ण": "महत्वपूर्ण",
    "दावी": "दाबी", "मुर्ख": "मूर्ख", "गल्ति": "गल्ती", "किनकी": "किनकि", "कोहि": "कोही", "साच्चै": "साँच्चै", "ठुलो": "ठूलो",
    "सधै": "सधैं", "सधैँ": "सधैं", "दुइ": "दुई", "संग": "सँग", "पछाडी": "पछाडि", "यहि": "यही", "भनाई": "भनाइ",
    "कानुनी": "कानूनी", "संचालन": "सञ्चालन", "संधै": "सधैं", "वमोजिम": "बमोजिम", "झन्डै": "झण्डै", "एवम्": "एवं",
    "स्कूल": "स्कुल", "शायद": "सायद", "पुरा": "पूरा", "संकलन": "सङ्कलन", "फेरी": "फेरि",
    # The Hindi pack's spellings of words the list has in their Nepali spelling.
    "भाई": "भाइ", "तरीका": "तरिका", "जरूरी": "जरुरी", "मीटर": "मिटर", "शुक्रवार": "शुक्रबार", "बुधवार": "बुधबार",
    "शनिवार": "शनिबार", "बिहिबार": "बिहीबार", "आईतबार": "आइतबार", "तालीम": "तालिम", "खेलकूद": "खेलकुद", "औषधी": "औषधि",
    "टीम": "टिम", "महसूस": "महसुस", "रमाईलो": "रमाइलो", "विहान": "बिहान", "बस्तु": "वस्तु", "यहां": "यहाँ", "नया": "नयाँ",
    "कहा": "कहाँ", "सग": "सँग", "साचो": "साँचो", "चिनिया": "चिनियाँ", "चीनियाँ": "चिनियाँ", "लीग": "लिग", "परिबार": "परिवार",
    "बिबाह": "विवाह", "विमा": "बीमा", "बैसाख": "वैशाख", "अर्थात": "अर्थात्", "सन": "सन्", "विना": "बिना",
    "किलोमीटर": "किलोमिटर", "चिज": "चीज", "घरेलू": "घरेलु", "दुध": "दूध", "विदा": "बिदा", "तस्वीर": "तस्बिर", "रूख": "रुख",
    "नून": "नुन", "चिठि": "चिठी", "निलो": "नीलो", "सीपाही": "सिपाही", "हिरा": "हीरा", "भालू": "भालु", "पाटि": "पाटी",
    "बिधवा": "विधवा", "अल्छि": "अल्छी", "एक्काईस": "एक्काइस",
    # Past rank 2300: the other spellings the news write (दोश्रो, वेला, सवै, अबस्था), नक्शा and राजनैतिक, the dialect छोरो,
    # and ओटा, the classifier written apart from its number (५ ओटा).
    "दोश्रो": "दोस्रो", "तेश्रो": "तेस्रो", "आइतवार": "आइतबार", "बिहीवार": "बिहीबार", "मङ्गलवार": "मङ्गलबार",
    "मंगलवार": "मङ्गलबार", "मंगलबार": "मङ्गलबार", "बैशाख": "वैशाख", "जोखीम": "जोखिम", "वेला": "बेला", "दुबै": "दुवै", "सवै": "सबै",
    "त्यत्ति": "त्यति", "सुरू": "सुरु", "मंसीर": "मंसिर", "बिषय": "विषय", "ब्यक्ति": "व्यक्ति", "विरामी": "बिरामी",
    "मुल्य": "मूल्य", "अबस्था": "अवस्था", "स्थानिय": "स्थानीय", "केन्द्रिय": "केन्द्रीय", "सम्बन्धि": "सम्बन्धी",
    "जागीर": "जागिर", "छनौट": "छनोट", "सिफारिश": "सिफारिस", "त्यहीं": "त्यहीँ", "मजदूर": "मजदुर",
    "जलविद्युत": "जलविद्युत्", "प्रवद्र्धन": "प्रवर्द्धन", "ओटा": "वटा", "नक्शा": "नक्सा", "राजनैतिक": "राजनीतिक",
    "छोरो": "छोरा", "पोसाक": "पोशाक", "शब्दकोष": "शब्दकोश", "नतीजा": "नतिजा", "तारीख": "तारिख", "पोखरि": "पोखरी",
    "रूसी": "रुसी", "शुरूआत": "शुरुआत", "सशक्तिकरण": "सशक्तीकरण", "सहरी": "शहरी", "नास": "नाश", "बीर": "वीर",
    # The spellings the list's first 2300 have, which those past it fold into (मिठो, not मीठो; गरीब, not गरिब).
    "मीठो": "मिठो", "नमुना": "नमूना", "गरिब": "गरीब", "गरिबी": "गरीबी", "सिट": "सीट",
}
# Links of the pack's tables that are wrong: खाली "empty" is no form of खानु.
WRONG_LINKS = {("खाली", "खानु"), ("फेरी", "फेर्नु"), ("गोली", "गोलो"), ("चालू", "चाल्नु")}
# The tables' causatives that are no verbs of their own (lexicon): misspellings (लागाउनु of लाग्नु, पुगाउनु of पुग्नु), those
# whose forms are the verb's nouns (सोचाइ, हेराइ) and those Nepali does not use (मराउनु, चोराउनु).
NOT_CAUSATIVES = {"लागाउनु", "पुगाउनु", "सोचाउनु", "हेराउनु", "मराउनु", "चोराउनु", "टोकाउनु", "खनाउनु", "हेपाउनु"}
# Corpus spellings that are no words: letters and endings written apart, abbreviations.
NOT_WORDS = {"हरू", "हरु", "नं", "रु", "रू", "डा", "स", "दु"}
# Words the Hindi pack confirms (HINDI_CONFIRMED) that are names in Nepali text, or Hindi's alone.
NOT_CONFIRMED = {"थापा", "श्रेष्ठ", "शाह", "पोखरा", "किम", "सिंह", "राई", "गुरुङ", "पौडेल", "शर्मा", "यादव", "चितवन"}
# A word the pack lacks that a Nepali text uses this often, and the hi-en pack has as a word (not a name), is a word too:
# Nepali shares most of its learned vocabulary with Hindi in the same spelling (कारण, अवस्था, निर्णय, जानकारी), which the
# ne-en pack, from 2,300 Wiktionary entries, lacks. Its kind is the Hindi entry's.
HINDI_CONFIRMED = 30
HINDI_KINDS = {"noun", "adj", "adv", "num", "conj", "det", "intj", "particle", "postp", "prep", "pron"}


def normalised(form):
    return unicodedata.normalize("NFC", form)


def nasal_variants(form):
    """
    The form with ं written ँ and the other way round (गरौं, गरौँ), without it (हुँदैन, हुदैन; गर्दिनँ, गर्दिन), and the eyelash
    र written either way (गर्यो, गर्‍यो).
    """
    out = {form, form.replace("ं", "ँ"), form.replace("ँ", "ं"), form.replace("ँ", "")}
    out |= {f.replace("र्य", "र्\u200dय") for f in out} | {f.replace("र्\u200dय", "र्य") for f in out}
    return out


def nominal_tails():
    tails = set()
    for plural in PLURALS:
        for case in [""] + CASES:
            tails.add(plural + case)
        for case in STACKED:
            for second in SECOND:
                tails.add(plural + case + second)
    with_clitics = set(tails)
    for tail in tails:
        for clitic in CLITICS:
            with_clitics.add(tail + clitic)
        # को + नै is कै, मा + नै is मै (उसकै, घरमै), and the stacked ones too (भित्रकै).
        if tail.endswith("को"):
            with_clitics.add(tail[:-2] + "कै")
        if tail.endswith("मा"):
            with_clitics.add(tail[:-2] + "मै")
        # The emphatic on an ending without one, or on the stem (घरबाटै, घरभित्रै, मात्रै, सँगै).
        if (not tail or tail[-1] not in "ािीुूेैोौंँ्") and tail != "ले":
            with_clitics.add(tail + "ै")
    return with_clitics


NOMINAL_TAILS = nominal_tails()
# Endings a pack table's own forms take (उसको is no form to put endings on twice).
PARTICIPLE_TAILS = {t for t in NOMINAL_TAILS if t}


def takes_postpositions(word, kinds):
    """Whether a word takes the nominal endings: a noun, adjective, number, and an adverb or postposition of more than a
    syllable that is no conjunction or particle too (अहिलेसम्म, भित्रको; not पनि, मा)."""
    if len(word) < 2:
        return False
    if kinds & {"noun", "adj", "num", "det", "pron", "classifier", "counter"}:
        return True
    return bool(kinds & {"adv", "postp"}) and not kinds & {"conj", "particle"} and len(word) > 2


def nominal_stems(word, kinds):
    """The stems a nominal's endings go on: the word, and an adjective in -ो's agreeing -ा and -ी (राम्रा, राम्री)."""
    stems = {word}
    if kinds & {"adj", "det", "pron", "postp"} and word.endswith("ो") and len(word) > 2:
        stems |= {word[:-1] + "ा", word[:-1] + "ी", word[:-1] + "ै"}
    return stems


def nominal_forms(word, kinds=frozenset({"noun"})):
    """A noun's (or adjective's, number's) forms with its plural, postpositions and clitics joined on."""
    forms = set()
    for stem in nominal_stems(word, kinds):
        forms |= {stem + tail for tail in NOMINAL_TAILS}
    if "num" in kinds:
        forms |= {word + tail for tail in NUMBER_TAILS}
    return forms


# The forms the pack itself gets for a noun or adjective without a table: its plural and main postpositions (घरहरू, घरमा).
PACK_TAILS = sorted({p + c + k for p in PLURALS for c in ["", "ले", "लाई", "को", "का", "की", "मा", "बाट", "देखि", "सँग", "सम्म",
                                                              "भन्दा", "तिर"] for k in ("", "नै")} - {""})


def pack_forms(word, kinds):
    """The forms the dictionary pack lists for a word with no table of its own (build_dictionary.py --source ne)."""
    return sorted({stem + tail for stem in nominal_stems(word, kinds) for tail in PACK_TAILS} | (nominal_stems(word, kinds) - {word}))


def joined_tails(form, verb):
    """What may be written after [verb]'s [form], by what the form is (JOINED)."""
    if form == verb:
        return "nu"
    if form == verb[:-1]:
        return "n"
    if form.endswith(("को", "का", "की", "ने")):
        return "participle"
    if form.endswith(("ि", "ी", "ई", "इ")):
        return "conjunctive"
    if form.endswith("दै"):
        return "dai"
    if form.endswith("दा"):
        return "da"
    if form.endswith(("े", "ए")):
        return "e"
    return None


# A regular verb's endings on its stem, for a verb the pack has no table for (regular_verb_forms): on a consonant stem (गर्),
# and on a vowel stem (ल्याउ, खा).
CONSONANT_STEM = ["नु", "न", "ने", "छ", "छन्", "छु", "छौं", "छौ", "छिन्", "छे", "दै", "दा", "दैन", "दैनन्", "दिन", "दिनँ", "थ्यो",
                  "थे", "थिन्", "यो", "ला", "लान्", "नुहोस्", "नुहुन्छ", "नुभयो", "नुभएको", "नेछ", "नेछन्", "दछ", "दछन्"]
CONSONANT_PAST = ["े", "ेको", "ेका", "ेकी", "ेर", "ें", "ेँ", "ेन", "ेनन्", "ी", "ि", "ौं", "ौँ", "ोस्", "िन्", "ेछ", "ेछन्"]
VOWEL_STEM = ["नु", "न", "ने", "ँछ", "ँछन्", "ँछु", "ँछौं", "ँछौ", "ँछिन्", "ँदै", "ँदा", "ँदैन", "ँदैनन्", "ँदिन", "ँथ्यो", "ँथे",
              "ला", "लान्", "नुहोस्", "नुहुन्छ", "नुभयो", "नुभएको", "नेछ", "नेछन्"]
VOWEL_PAST = ["ए", "एको", "एका", "एकी", "एर", "एँ", "एं", "एन", "एनन्", "ई", "इ", "यो", "औं", "औँ", "ओस्", "इन्", "एछ", "एछन्"]
# A passive's endings on the conjunctive (गरि + न्छ, एको, यो: गरिन्छ, गरिएको, गरियो), for a verb whose passive is no word of
# the pack's.
PASSIVE = ["न्छ", "न्छन्", "न्न", "एको", "एका", "एकी", "यो", "ए", "ने", "न", "नु", "ँदैन", "ँदै", "ँदा", "एर", "एन", "एनन्", "एछ",
           "नेछ", "नेछन्"]


def regular_verb_forms(verb):
    """A regular verb's forms by rule (गर्नु, ल्याउनु), with the negative न- before them, for a verb the pack has no table for."""
    stem = verb[:-2]
    if not verb.endswith("नु") or not stem:
        return set()
    if stem.endswith("्"):
        base = stem[:-1]
        forms = {stem + t for t in CONSONANT_STEM} | {base + t for t in CONSONANT_PAST} | {base}
    elif "\u0915" <= stem[-1] <= "\u0939":
        # A stem in a consonant with its vowel (फर्क of फर्कनु, रह of रहनु): फर्कन्छ, फर्केको, फर्क्यो.
        forms = {stem + t for t in ("नु", "न", "ने", "न्छ", "न्छन्", "न्छु", "न्दै", "न्दा", "न्न", "न्थ्यो", "नुहोस्", "नेछ")}
        forms |= {stem + t for t in CONSONANT_PAST} | {stem, stem + "्यो"}
    elif stem.endswith("ि"):
        # A stem in -इ (फैलि of फैलिनु): a passive's endings (फैलिन्छ, फैलिएको, फैलियो).
        forms = {stem + t for t in PASSIVE} | {stem + t for t in ("नुहोस्", "नुहुन्छ", "नुभयो", "नुभएको", "न्थ्यो", "न्छु")}
    elif stem.endswith("ा"):
        # A stem in -आ (खा, जा): खायो, खाएको, खाई.
        forms = {stem + t for t in VOWEL_STEM} | {stem + t for t in VOWEL_PAST} | {stem}
    else:
        # A stem in -आउ, -उ (ल्याउ, धु): ल्यायो, ल्याएको, ल्याई on the stem without its -उ.
        base = stem[:-1] if stem.endswith("ु") or stem.endswith("उ") else stem
        forms = {stem + t for t in VOWEL_STEM} | {base + t for t in VOWEL_PAST} | {stem}
    return forms | {"न" + f for f in forms}


def lexicon(dictionaries=DICTIONARIES, counts=None):
    """
    The pack's words by kind and its tables (form -> words), the pronoun tables aside, and the words that are only names. With
    [counts] (form -> how often texts have it), the words the Hindi pack confirms are added (HINDI_CONFIRMED).
    """
    db = sqlite3.connect(os.path.join(dictionaries, "ne-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        word = normalised(word)
        if not LETTERS.fullmatch(word) or STRAY_JOINER.search(word):
            continue
        # A language's name is an everyday word (नेपाली); other names are left out.
        if pos == "name" and any("language" in g for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS:
            kinds[word].add(pos)
    for word, kind in NEPALI_WORDS.items():
        kinds[word].add(kind)
    # A spelling the pack has as a word of its own is folded into the word it spells.
    for variant, word in SPELLINGS.items():
        if variant in kinds:
            kinds[word] |= kinds.pop(variant)
    for word in list(PRONOUNS) + list(COPULAS):
        kinds[word].add("pron" if word in PRONOUNS else "verb")
    # A causative the tables list as a form of its verb (सुनाउनु of सुन्नु, बोलाउनु of बोल्नु) is a verb of its own
    # (NOT_CAUSATIVES aside).
    causatives = set()
    for form, lemma, generated in db.execute("SELECT form_lc, lemma, generated FROM forms"):
        form, lemma = normalised(form), normalised(lemma)
        if not generated and form.endswith("ाउनु") and not lemma.endswith("ाउनु") and lemma in kinds \
                and LETTERS.fullmatch(form) and form not in NOT_CAUSATIVES:
            causatives.add(form)
    for verb in causatives:
        kinds[verb].add("verb")
    table = defaultdict(set)
    for form, lemma, generated in db.execute("SELECT form_lc, lemma, generated FROM forms"):
        form, lemma = normalised(form), normalised(lemma)
        if form in causatives and lemma != form:
            continue
        if generated or lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS or lemma in PRONOUNS \
                or form in NEPALI_FORMS or not LETTERS.fullmatch(form):
            continue
        lemma = SPELLINGS.get(lemma, lemma)
        for variant in nasal_variants(form) | vowel_stem_nasals(form, lemma):
            table[variant].add(lemma)
    for lemma, forms in EXTRA_FORMS.items():
        for form in forms.split():
            table[form].add(lemma)
    tabled = {lemma for lemmas in table.values() for lemma in lemmas}
    for verb in [w for w, ks in kinds.items() if "verb" in ks and w.endswith("नु")]:
        if verb not in tabled:
            for form in regular_verb_forms(verb):
                for variant in nasal_variants(form):
                    table[variant].add(verb)
        stem = verb[:-2]
        # The passive on the conjunctive: गर् + इ (गरिन्छ), बनाउ + इ (बनाइन्छ), दि + इ (दिइन्छ).
        passive = stem[:-1] + "ि" if stem.endswith("्") else (stem[:-1] if stem.endswith(("ु", "उ")) else stem) + "इ"
        if passive + "नु" not in kinds and not stem.endswith("ि"):
            # And its negative (नगरिने, नसकिएको).
            for form in {passive + t for t in PASSIVE}:
                table[form].add(verb)
                table["न" + form].add(verb)
        # The negative infinitive (नगर्नु) and, on a consonant stem, the formal present (गर्दछ, पर्दछ).
        table["न" + verb].add(verb)
        if stem.endswith("्"):
            for tail in ("दछ", "दछन्", "दछु", "दछौं", "दछिन्", "दैछ", "दैछन्"):
                table[stem + tail].add(verb)
    for variant, verb in VERB_SPELLINGS.items():
        for form in regular_verb_forms(variant):
            for spelling in nasal_variants(form):
                table[spelling].add(verb)
    if counts:
        confirmed = hindi_confirmed(dictionaries, kinds, table, counts)
        for word, ks in confirmed.items():
            kinds[word] |= ks
    return kinds, table, names - set(kinds)


def vowel_stem_nasals(form, verb):
    """A vowel-stem verb's forms in -दा, -दै, -दो written with the ँ the tables leave out (हुदा, जादै: हुँदा, जाँदै)."""
    stem = verb[:-2] if verb.endswith("नु") else None
    if not stem or stem.endswith("्"):
        return set()
    for negative in ("", "न"):
        if form.startswith(negative + stem + "द"):
            return {negative + stem + "ँ" + form[len(negative + stem):]}
    return set()


def hindi_confirmed(dictionaries, kinds, table, counts):
    """The words the Hindi pack confirms: word -> kinds (HINDI_CONFIRMED)."""
    path = os.path.join(dictionaries, "hi-en.sqlite")
    if not os.path.exists(path):
        return {}
    hindi = defaultdict(set)
    for word, pos in sqlite3.connect(path).execute("SELECT word_lc, pos FROM entries"):
        hindi[normalised(word)].add(pos)
    analyser = Analyser(kinds, table)
    confirmed = {}
    for form, n in counts.items():
        if n < HINDI_CONFIRMED or form in kinds or form in NOT_WORDS or form in NOT_CONFIRMED or form in SPELLINGS:
            continue
        ks = hindi.get(form, set()) & HINDI_KINDS
        if not ks or len(form) < 2:
            continue
        own, verbs, nouns = analyser.candidates(form)
        if verbs or nouns:
            continue
        confirmed[form] = ks
    return confirmed


class Analyser:
    """Reads a form as the words it may be a form of, by the pack's tables and the rules."""

    def __init__(self, kinds, table):
        self.kinds = kinds
        self.table = table
        self.table_of = defaultdict(set)
        for form, lemmas in table.items():
            for lemma in lemmas:
                self.table_of[lemma].add(form)
        # Stems the nominal endings go on: stem -> words.
        self.nominal = defaultdict(set)
        for word, ks in kinds.items():
            if takes_postpositions(word, ks) and word not in PRONOUNS:
                for stem in nominal_stems(word, ks):
                    self.nominal[stem].add(word)
        for variant, word in SPELLINGS.items():
            if takes_postpositions(word, kinds.get(word, set())):
                for stem in nominal_stems(variant, kinds[word]):
                    self.nominal[stem].add(word)
        # A pronoun's forms take the endings too (उनीहरूसँगै is उनी's), and so do a table's participles (गरेकोले) and the
        # forms a verb's helpers follow (गरि + रहेको).
        self.joined = defaultdict(set)
        for form, word in NEPALI_FORMS.items():
            if word in PRONOUNS:
                self.joined[(form, "pronoun")].add(word)
        for lemma, ks in kinds.items():
            if "verb" in ks and lemma.endswith("नु"):
                for form in self.table_of.get(lemma, set()) | {lemma}:
                    kind = joined_tails(form, lemma)
                    if kind:
                        self.joined[(form, kind)].add(lemma)
        helper_forms = set()
        for helper in HELPERS:
            if helper in kinds:
                helper_forms |= {helper} | self.table_of.get(helper, set())
        parna = {"पर्नु"} | self.table_of.get("पर्नु", set()) if "पर्नु" in kinds else set()
        sakna = self.table_of.get("सक्नु", set())
        self.after = {
            "participle": PARTICIPLE_TAILS | {"कै"},
            "pronoun": PARTICIPLE_TAILS,
            "nu": set(AFTER_NU) | parna,
            "n": set(AFTER_N) | sakna,
            "conjunctive": helper_forms,
            "dai": set(AFTER_DAI),
            "da": set(AFTER_DA),
            "e": set(AFTER_E),
        }
        self.joined_prefixes = defaultdict(list)
        for (form, kind), words in self.joined.items():
            self.joined_prefixes[form].append((kind, words))

    def by_rule(self, form):
        """(words the form is a noun's form of, by the fewest letters of ending; words it is a verb's or pronoun's form of)."""
        nouns, verbs = {}, set()
        for cut in range(len(form), 0, -1):
            stem, tail = form[:cut], form[cut:]
            if stem in self.nominal and tail in NOMINAL_TAILS:
                for word in self.nominal[stem]:
                    nouns.setdefault(word, len(tail))
            # A number with its classifier written on (दुईवटा, तीनजना).
            if tail in NUMBER_TAILS and "num" in self.kinds.get(stem, ()):
                nouns.setdefault(stem, len(tail))
            for kind, words in self.joined_prefixes.get(stem, ()):
                if tail in self.after[kind]:
                    verbs |= words
        # The emphatic -ै on a word that ends in a consonant (एकदमै, ठिकै, मात्रै, आजै).
        if form.endswith("ै") and len(form) > 2 and form[-2] not in "ािीुूेैोौंँ्" \
                and not self.kinds.get(form[:-1], {"verb"}) <= {"verb"}:
            nouns.setdefault(form[:-1], 1)
        # The contracted emphatic on a participle (गरेकै): को + नै.
        if form.endswith("कै") and form[:-1] + "ो" in self.table:
            verbs |= {w for w in self.table[form[:-1] + "ो"] if "verb" in self.kinds.get(w, ())}
        if nouns:
            fewest = min(nouns.values())
            nouns = {w for w, n in nouns.items() if n == fewest}
        return set(nouns), verbs

    def folded(self, form):
        """
        The words a word of its own is also a form of by rule, which it is filed under when they are commoner: a number with its
        classifier or ordinal ending (चारवटा, सातौं), a word with the emphatic -ै (ठीकै), an adjective's agreeing -ा, -ी or -ै
        (बुढा of बुढो), a noun with भर, भरि "all through" (दिनभर).
        """
        out = set()
        for tail in NUMBER_TAILS:
            if form.endswith(tail) and "num" in self.kinds.get(form[:-len(tail)], ()):
                out.add(form[:-len(tail)])
        if form.endswith("ै") and form[-2] not in "ािीुूेैोौंँ्" and not self.kinds.get(form[:-1], {"verb"}) <= {"verb"}:
            out.add(form[:-1])
        if form[-1] in "ाीै" and "adj" in self.kinds.get(form[:-1] + "ो", ()):
            out.add(form[:-1] + "ो")
        for tail in ("भर", "भरि"):
            if form.endswith(tail) and "noun" in self.kinds.get(form[:-len(tail)], ()):
                out.add(form[:-len(tail)])
        return out - {form}

    def candidates(self, form):
        """(the word the form is itself, or None; the words it is a table's or a verb's form of; those it is a noun's of)."""
        if form in NEPALI_FORMS:
            return None, {NEPALI_FORMS[form]}, set()
        if form in SPELLINGS and form not in self.table:
            return None, {SPELLINGS[form]}, set()
        own = form if form in self.kinds else None
        if own in OWN:
            return own, set(), set()
        tabled = self.table.get(form, set()) - {form}
        nouns, verbs = self.by_rule(form)
        return own, (tabled | verbs) - {form}, nouns - {form}


def _tokens(line):
    """A line's words as the reader cuts them (LETTERS), in NFC, without the ones with a stray joiner."""
    for word in LETTERS.findall(normalised(line)):
        if not STRAY_JOINER.search(word):
            yield word


# The web crawl's court judgments (a quarter of its sentences) put their words in the first 300 (वादी, प्रतिवादी, निवेदक,
# पुनरावेदन): the sentences from those sites are left out.
EXCLUDED_SITES = {"supremecourt.gov.np", "nkp.gov.np"}
SITE = re.compile(r"^[a-z]+://(?:www\.)?([^/:]+)")


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        members = {m.name.rsplit("-", 1)[-1]: m for m in tar.getmembers() if m.isfile()}
        excluded = set()
        if "sources.txt" in members and "inv_so.txt" in members:
            sites = {}
            for line in io.TextIOWrapper(tar.extractfile(members["sources.txt"]), encoding="utf-8"):
                number, _, url = line.rstrip("\n").partition("\t")
                site = SITE.match(url.split("\t")[0])
                if site and site.group(1) in EXCLUDED_SITES:
                    sites[number] = site.group(1)
            for line in io.TextIOWrapper(tar.extractfile(members["inv_so.txt"]), encoding="utf-8"):
                source, sentence = line.rstrip("\n").split("\t")[:2]
                if source in sites:
                    excluded.add(sentence)
        for line in io.TextIOWrapper(tar.extractfile(members["sentences.txt"]), encoding="utf-8"):
            number, _, sentence = line.rstrip("\n").partition("\t")
            if number not in excluded:
                yield sentence


# Wikipedia sentences that differ only in their numbers ("X गाउँ विकास समितिको जनसंख्या N छ") are counted once.
DIGITS = re.compile(r"[0-9०-९]+")


# A word's spellings that differ only in a nasal written as ं or as the nasal of the next letter's row (सम्भावना, संभावना;
# रङ्ग, रंग), as ं or ँ before a consonant (भैंसी, भैँसी), त्त्व written त्व (महत्त्व, महत्व), a visarga before a consonant (दुःखी, दुखी), a nukta, the eyelash र (पुर्‍याउनु) or a final virama (संसद्, संसद):
# one word, under its commonest spelling.
_NASAL_OF = {c: n for n, row in (("ङ", "कखगघ"), ("ञ", "चछजझ"), ("ण", "टठडढ"), ("न", "तथदध"), ("म", "पफबभ")) for c in row}
_HALF_NASAL = re.compile("([ङञणनम])\u094d([क-ह])")


def spelling_key(word):
    word = word.replace("\u093c", "").replace("र्\u200dय", "र्य").replace("त्त्व", "त्व")
    word = re.sub("\u0903(?=[क-ह])", "", word).removesuffix("\u094d")
    word = re.sub("\u0901(?=[क-ह])", "\u0902", word)
    return _HALF_NASAL.sub(lambda m: "ं" + m.group(2) if _NASAL_OF.get(m.group(2)) == m.group(1) else m.group(0), word)


def fold_spellings(scores, totals, forms):
    """Folds each word into its commonest spelling (spelling_key), in place."""
    spellings = defaultdict(list)
    for word in scores:
        spellings[spelling_key(word)].append(word)
    for words in spellings.values():
        if len(words) < 2:
            continue
        main = min(words, key=lambda w: (-scores[w], w))
        for word in words:
            if word != main:
                merge(word, main, scores, totals, forms)


def merge(word, into, scores, totals, forms):
    """Files [word] and its forms under [into], in place."""
    scores[into] += scores.pop(word)
    totals[into] += totals.pop(word, 0)
    for form, credit in forms.pop(word, {}).items():
        forms[into][form] = forms[into].get(form, 0.0) + credit


def _counts(download):
    """Each kind of text's word counts, and the corpora counted."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    used = defaultdict(list)
    for kind, corpora in LEIPZIG.items():
        for corpus in corpora:
            path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
            if path is None:
                continue
            used["leipzig"].append(corpus)
            seen = set()
            for sentence in _leipzig_sentences(path):
                key = DIGITS.sub("0", sentence)
                if key in seen:
                    continue
                seen.add(key)
                for word in _tokens(sentence):
                    counts[kind][word] += 1
    path = download(SUBTITLES, "opus-opensubtitles-v2024-ne.txt.gz")
    if path is not None:
        used["OpenSubtitles"].append("v2024")
        with gzip.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                for word in _tokens(line):
                    counts["subtitles"][word] += 1
    return counts, used


def label_of(used):
    parts = []
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    if used.get("OpenSubtitles"):
        parts.append(f"OPUS OpenSubtitles {', '.join(used['OpenSubtitles'])} ne ({LICENCES['OpenSubtitles']})")
    return " + ".join(parts)


def rank(download, dictionaries=DICTIONARIES):
    """
    The words by score, best first, with their scores, their forms' credit (word -> form -> share), their counts in all, and
    the label of the sources: the pack's words by their forms' counts, each kind of text weighted by WEIGHTS, spellings folded
    (fold_spellings).
    """
    counts, used = _counts(download)
    label = label_of(used) + " + English Wiktionary Hindi entries (CC BY-SA 4.0) for the words the ne-en pack lacks"
    share = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        for form, n in kind_counts.items():
            if total:
                share[form] += WEIGHTS[kind] * n / total
                raw[form] += n
    kinds, table, names = lexicon(dictionaries, raw)
    analyser = Analyser(kinds, table)

    found = {}
    for form in share:
        if form in NOT_WORDS or form in names and form not in table:
            continue
        found[form] = analyser.candidates(form)
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, verbs, nouns) in found.items():
        heads = {own} if own else verbs or nouns
        if len(heads) == 1:
            alone[next(iter(heads))] += share[form]
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    folded_into = {}
    for form, (own, verbs, nouns) in found.items():
        value = share[form]
        # A word of its own that the pack has only as a verb's form (भयो, थियो) is that verb's, when the verb is more common
        # than the form; a noun, postposition or verb of its own keeps its count (ले "by", not लिनु's; बनाउनु, not बन्नु's).
        if own:
            taken = verbs and kinds[own] <= {"verb"} and not own.endswith("नु")
            heads = verbs if taken and max(alone.get(v, 0.0) for v in verbs) > value else {own}
            folded = analyser.folded(own) if heads == {own} else set()
            if folded and max(alone.get(w, 0.0) for w in folded) > value:
                heads = folded
                if len(folded) == 1:
                    folded_into[own] = next(iter(folded))
        else:
            heads = verbs | nouns
        if not heads:
            continue
        weights = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights.values())
        for head in heads:
            part = weights[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += value * part
            totals[head] += raw[form]
            forms[head][form] = forms[head].get(form, 0.0) + value * part
    # A word filed under another (चारवटा under चार) takes its own forms with it (चारवटाको).
    for word, into in sorted(folded_into.items()):
        if word in scores and into in scores and into not in folded_into:
            merge(word, into, scores, totals, forms)
    fold_spellings(scores, totals, forms)
    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT
              and NEPALI_FORMS.get(w, w) == w]
    return ranked, scores, forms, totals, label


def build(size, download, dictionaries=DICTIONARIES):
    """The list (rank), its words' forms by their credit."""
    ranked, _, forms, _, label = rank(download, dictionaries)
    ranked = ranked[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
