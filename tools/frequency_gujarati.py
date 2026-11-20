"""
The Gujarati frequency list (build_frequency_list.py --language gu).

Gujarati writes a noun with its case postposition joined on (ઘરમાં, રામનો, છોકરાને, શાળાએ), after the noun's oblique form
(છોકરો, છોકરાને; છોકરું, છોકરાંને), and a verb's endings on its stem (કરું, કરે, કર્યું, કરશે, કરતો, કરવામાં). The gu-en pack
has tables for some words, many of them wrong for irregular verbs (જવું's જ, જય), and simplemma knows no Gujarati. So each word
of the pack (and GUJARATI_WORDS, everyday words it lacks) is declined or conjugated here by its ending, and a corpus form is
counted for the words it is a form of: by hand (GUJARATI_FORMS, IRREGULAR), by the pack's tables, or by these rules. A form
that is a word of the pack itself keeps its count (જો "if", not જોવું's "look!"), unless the word it is also a form of is more
common than the form. A form of several words is shared between them by how common each is by its forms of no other word.

The words are counted from Leipzig's Gujarati news and Wikipedia sentences, as the reader cuts them (GUJARATI_WORD). Most of
the 2011 news crawl is Gujarat Samachar's pages, whose legacy-font conversion wrote some letters wrongly: રૂ as રૃ (શરૃ,
જરૃર), ધ before a vowel sign or virama as ઘ (વઘુ, ઘ્યાન), a doubled ા (વિજ્ઞાાન), ુ before ્ર (ગુ્રપ), and a consonant + ્ય as
consonant + ય (ઘટયા). A word of those pages is counted as its repaired spelling (repaired) when the other sources write that
spelling more often than the one the page has.
"""
import io
import json
import os
import re
import sqlite3
import tarfile
import unicodedata
from collections import defaultdict
from functools import lru_cache

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

# As the reader's Gujarati wordCharacters (the Gujarati block), without the Gujarati digits: numbers are no words. The
# reader reads no zero-width joiner as part of a word; the corpora's few (in a half form, અન્‍ય) are dropped before counting.
GUJARATI_LETTERS = "઀-૥૰-૿"
GUJARATI_WORD = re.compile(f"[{GUJARATI_LETTERS}]+")
JOINERS = str.maketrans("", "", "‌‍")
CONSONANTS = {chr(c) for c in range(0x0A95, 0x0ABA)}
NUKTA = "઼"
VIRAMA = "્"
VOWEL_SIGNS = {chr(c) for c in range(0x0ABE, 0x0AC9)} | {"ૉ", "ો", "ૌ", "ે", "ૈ"}
VOWEL_LETTERS = {chr(c) for c in range(0x0A85, 0x0A95)}
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep", "phrase"}
NOMINAL_KINDS = {"noun", "adj", "num", "det"}
FORMS_LISTED = 400
# A word must be counted this often (all its forms, in all the corpora) to be listed: below it the corpora's typos.
LEAST_COUNT = 4

LEIPZIG_NEWS = ["guj_newscrawl_2011_300K", "guj_news_2020_30K"]
LEIPZIG_WIKIPEDIA = ["guj_wikipedia_2021_100K"]
# Each kind of text's share of the blend: Gujarat Samachar's pages (two thirds of the news words), the other news pages,
# and Wikipedia, whose articles add their own words (તાલુકો, વસ્તી) and are a quarter.
WEIGHTS = {"samachar": 0.5, "news": 0.25, "wikipedia": 0.25}

# The postpositions a noun takes written on (ઘરમાં, ઘરનો, ઘરથી, ઘરમાંથી), after its oblique form.
CASE_TAILS = ["ને", "નો", "ની", "નું", "ના", "નાં", "થી", "માં", "માંથી", "માંનો", "માંની", "માંનું", "માંના", "નાથી", "નામાં", "નાં"]


ADVERB_TAILS = ["નો", "ની", "નું", "ના", "નાં", "થી", "માં"]


def consonant_final(word):
    last = word[-2] if word.endswith(NUKTA) and len(word) > 1 else word[-1]
    return last in CONSONANTS


def long_i(form):
    """[form] with ઇ after a letter written ઈ, the dictionary's spelling (થઇ as થઈ, કોઇ as કોઈ)."""
    return form[:1] + form[1:].replace("ઇ", "ઈ")


def with_short_i(forms):
    """
    The forms with ઈ after a letter also spelled with ઇ (થઈ, થઇ; કોઈ, કોઇ), as half the texts write them, and a past in
    -યું also without its nasal (કર્યુ, કહ્યુ), as Gujarat Samachar's pages often write it.
    """
    out = set(forms) | {f[:1] + f[1:].replace("ઈ", "ઇ") for f in forms if "ઈ" in f[1:]}
    return out | {f[:-1] for f in out if f.endswith("યું")}



def nominal_forms(word):
    """A noun's (or adjective's used as one) forms: its oblique and plural, each with the postpositions written on."""
    if word.endswith("ો") and len(word) > 2:
        oblique = word[:-1] + "ા"
        bases = [oblique, oblique + "ઓ"]
        forms = {word, word[:-1] + "ે", oblique + "એ", oblique + "ઓએ"}
    elif word.endswith("ું") and len(word) > 2:
        stem = word[:-2]
        bases = [stem + "ા", stem + "ાં", stem + "ાંઓ"]
        forms = {word, stem + "ે", stem + "ાએ", stem + "ાંએ", stem + "ાંઓએ"}
    else:
        plural = word + ("ો" if consonant_final(word) else "ઓ")
        bases = [word, plural]
        forms = {word + ("ે" if consonant_final(word) else "એ"), plural + "એ"}
    return forms | set(bases) | {b + t for b in bases for t in CASE_TAILS}


def adjective_forms(word):
    """An adjective's gender and number forms (સારો, સારી, સારું, સારા, સારાં, સારે) and theirs used as nouns (નાનાને, મોટીઓ)."""
    if word.endswith("ું") and len(word) > 2:
        stem = word[:-2]
        return nominal_forms(word) | nominal_forms(stem + "ો") | nominal_forms(stem + "ી")
    return nominal_forms(word)


# A verb's endings on its stem (કર of કરવું), and on a stem ending in a vowel (ખા of ખાવું, જો of જોવું).
CONSONANT_STEM_TAILS = ["ું", "ે", "ીએ", "ો", "ીશ", "શે", "ીશું", "શો", "શું", "્યો", "્યું", "્યા", "્યાં", "ી", "ીને", "તો", "તી",
                        "તું", "તા", "તાં", "ેલો", "ેલી", "ેલું", "ેલા", "ેલાં", "ેલ", "જે", "જો", "નાર", "નારા", "નારી", "નારો",
                        "નારું", "તાંની"]
VOWEL_STEM_TAILS = ["ઉં", "ઈએ", "ઓ", "ઈશ", "શે", "ઈશું", "શો", "શું", "યો", "યું", "યા", "યાં", "ઈ", "ઈને", "તો", "તી", "તું",
                    "તા", "તાં", "યેલો", "યેલી", "યેલું", "યેલા", "યેલાં", "જે", "જો", "નાર", "નારા", "નારી"]
# The infinitive's (કરવ- of કરવું) forms: its genders, its oblique with the postpositions (કરવામાં, કરવાનું), the passive.
INFINITIVE_TAILS = ["ું", "ો", "ી", "ા", "ાં", "ાનું", "ાનો", "ાની", "ાના", "ાનાં", "ામાં", "ાથી", "ાને", "ાય", "ાયો", "ાયું", "ાયા",
                    "ાઈ", "ાશે", "ાતું", "ાતો", "ાતી", "ાતા", "ાતાં"]
PASSIVE_TAILS = ["ાય", "ાયો", "ાયું", "ાયા", "ાયાં", "ાઈ", "ાશે", "ાતું", "ાતો", "ાતી", "ાતા", "ાતાં", "ાયેલું", "ાયેલા", "ાયેલી", "ાયેલો"]

# The verbs whose forms no rule makes, written out; their infinitive's forms (જવામાં, લેવાનું) are added by rule.
IRREGULAR = {
    "હોવું": "છે છું છો છીએ હતો હતી હતું હતા હતાં હતુ હશે હોઈશ હોઈશું હશો હોય હોઉં હોઈએ હો હોત હોઈ હોઈને નથી નહોતો નહોતી "
             "નહોતું નહોતા નહોતાં હોતો હોતી હોતું હોતા હોતાં",
    "જવું": "જાઉં જાય જઈએ જાઓ જાવ જા જજે જજો જઈશ જશે જઈશું જશો ગયો ગઈ ગયું ગયા ગયાં ગયેલો ગયેલી ગયેલું ગયેલા ગયેલાં ગયેલ જતો "
            "જતી જતું જતા જતાં જઈ જઈને જનાર જનારા",
    "થવું": "થાઉં થાય થઈએ થાઓ થા થજે થજો થઈશ થશે થઈશું થશો થયો થઈ થયું થયા થયાં થયેલો થયેલી થયેલું થયેલા થયેલાં થયેલ "
            "થતો થતી થતું થતા થતાં થઈને થાત થનાર થનારા",
    "લેવું": "લઉં લે લઈએ લો લેજે લેજો લઈશ લેશે લઈશું લેશો લીધો લીધી લીધું લીધા લીધાં લીધેલો લીધેલી લીધેલું લીધેલા લીધેલાં "
             "લેતો લેતી લેતું લેતા લેતાં લઈ લઈને લેત લેનાર લેનારા",
    "દેવું": "દઉં દે દઈએ દો દેજે દેજો દઈશ દેશે દઈશું દેશો દીધો દીધી દીધું દીધા દીધાં દીધેલો દીધેલી દીધેલું દીધેલા દેતો દેતી "
             "દેતું દેતા દેતાં દઈ દઈને દેત દેનાર",
    "ખાવું": "ખાઉં ખાય ખાઈએ ખાઓ ખા ખાજે ખાજો ખાઈશ ખાશે ખાઈશું ખાશો ખાધો ખાધી ખાધું ખાધા ખાધાં ખાધેલો ખાધેલી ખાધેલું ખાધેલા "
             "ખાતો ખાતી ખાતા ખાતાં ખાઈ ખાઈને ખાત ખાનાર ખવાય ખવાશે ખવાયું",
    "પીવું": "પીઉં પીએ પીઓ પી પીજે પીજો પીશ પીઈશ પીશે પીશું પીશો પીધો પીધી પીધું પીધા પીધાં પીધેલો પીધેલું પીતો પીતી પીતું "
             "પીતા પીતાં પીને પીત પીનાર",
    "જોવું": "જોઉં જુએ જુઓ જોજે જોજો જોઈશ જોશે જોઈશું જોશો જોયો જોઈ જોયું જોયા જોયાં જોયેલો જોયેલું જોયેલી જોયેલા જોતો જોતી "
             "જોતું જોતા જોતાં જોઈને જોત જોનાર જોનારા",
    "બેસવું": "બેસું બેસે બેસીએ બેસો બેસ બેસજે બેસજો બેસીશ બેસશે બેસીશું બેસશો બેઠો બેઠી બેઠું બેઠા બેઠાં બેઠેલો બેઠેલી બેઠેલું "
              "બેઠેલા બેઠેલાં બેસતો બેસતી બેસતું બેસતા બેસતાં બેસી બેસીને બેસત બેસનાર",
    "સૂવું": "સૂઉં સૂએ સૂઈએ સૂઓ સૂ સૂજે સૂજો સૂઈશ સૂશે સૂઈશું સૂશો સૂતો સૂતી સૂતું સૂતા સૂતાં સૂઈ સૂઈને સૂનાર",
    "ધોવું": "ધોઉં ધુએ ધોઈએ ધુઓ ધોજે ધોઈશ ધોશે ધોયો ધોઈ ધોયું ધોયા ધોયાં ધોતો ધોતી ધોતું ધોતા ધોઈને",
    "રોવું": "રોઉં રુએ રોઈએ રુઓ રોજે રોઈશ રોશે રોયો રોઈ રોયું રોયા રોતો રોતી રોતું રોતા રોતાં રોઈને",
    "મરવું": "મરું મરે મરીએ મરો મર મરીશ મરશે મર્યો મરી મર્યું મર્યા મર્યાં મરેલો મરેલી મરેલું મરેલા મરતો મરતી મરતું મરતા "
             "મરતાં મરીને મૂઓ મૂઈ મૂઉં",
}

# The personal and other pronouns' forms (the pack files the possessives મારું, તારું, તમારું under હું, તું, તમે): word -> forms.
PRONOUNS = {
    "હું": "મને મેં મારું મારો મારી મારા મારાં મારે મારાથી મારામાં મારૂં",
    "અમે": "અમને અમારું અમારો અમારી અમારા અમારાં અમારે અમારાથી અમારામાં",
    "આપણે": "આપણને આપણું આપણો આપણી આપણા આપણાં આપણાથી આપણામાં",
    "તું": "તને તેં તારું તારો તારી તારા તારાં તારે તારાથી તારામાં",
    "તમે": "તમને તમારું તમારો તમારી તમારા તમારાં તમારે તમારાથી તમારામાં",
    "આપ": "આપને આપનું આપનો આપની આપના આપનાં આપશ્રી",
    "તે": "તેમાંથી તેને તેણે તેનું તેનો તેની તેના તેનાં તેમાં તેનાથી તેનામાં",
    "તેઓ": "તેમને તેમણે તેમનું તેમનો તેમની તેમના તેમનાં તેઓએ તેમનામાં તેમનાથી તેઓને તેઓનું તેઓની તેઓના",
    "એ": "એમાંથી એને એણે એનું એનો એની એના એનાં એમાં એનાથી એનામાં એમને એમણે એમનું એમનો એમની એમના એમનાં એમનામાં "
         "એમનાથી એઓ",
    "આ": "આમાંથી આને આણે આનું આનો આની આના આનાં આમાં આનાથી આનામાં આમને આમણે આમનું આમનો આમની આમના આમનાં આમનામાં",
    "જે": "જેમાંથી જેને જેણે જેનું જેનો જેની જેના જેનાં જેમાં જેનાથી જેનામાં જેમને જેમણે જેમનું જેમનો જેમની જેમના જેમનાં જેઓ",
    "કોણ": "કોને કોણે કોનું કોનો કોની કોના કોનાં",
    "કોઈ": "કોઈને કોઈનું કોઈનો કોઈની કોઈના કોઈએ કોઈમાં કોઈથી",
    "પોતે": "પોતાને પોતાનું પોતાનો પોતાની પોતાના પોતાનાં પોતાથી પોતામાં પોતાનામાં",
    "શું": "શો શી શા શાં શાનું શાની શાનો શાના શાથી શામાં",
    "કશું": "કશો કશી કશા કશાનું કશાની કશાનો કશાના કશામાં",
}
GUJARATI_FORMS = {form: word for word, forms in PRONOUNS.items() for form in [word] + forms.split()}
GUJARATI_FORMS.update({
    # કેટલુંક "some" changes inside (કેટલાક, કેટલીક, કેટલાંક).
    "કેટલાક": "કેટલુંક", "કેટલીક": "કેટલુંક", "કેટલાંક": "કેટલુંક", "કેટલાકે": "કેટલુંક", "કેટલાકને": "કેટલુંક",
    # લોકો "people" is લોક's plural.
    # બધા "all (people)" is બધું's plural, which the pack has as a phrase of its own.
    "બધા": "બધું", "બધાને": "બધું", "બધાએ": "બધું", "બધાની": "બધું", "બધાના": "બધું", "બધાનો": "બધું", "બધાનું": "બધું",
    "લોકો": "લોક", "લોકોને": "લોક", "લોકોનો": "લોક", "લોકોની": "લોક", "લોકોનું": "લોક", "લોકોના": "લોક", "લોકોએ": "લોક",
    "લોકોમાં": "લોક", "લોકોથી": "લોક",
})
# Spellings the pack has the other way, or lacks: each is declined as the word it is a spelling of, and its forms counted
# for it.
SPELLINGS = {
    "બન્ને": "બંને", "દરમ્યાન": "દરમિયાન", "હજી": "હજુ", "કાંઈ": "કંઈ", "નહિ": "નહીં", "નહી": "નહીં", "છતા": "છતાં",
    "ઉભું": "ઊભું", "ઉંચું": "ઊંચું", "દુર": "દૂર", "જુનું": "જૂનું", "રાજય": "રાજ્ય", "સામાજીક": "સામાજિક", "મંજુરી": "મંજૂરી",
    "નુકશાન": "નુકસાન", "વિરૂદ્ધ": "વિરુદ્ધ", "વિરુધ્ધ": "વિરુદ્ધ", "ગુરૂ": "ગુરુ", "રજુઆત": "રજૂઆત", "રજુ": "રજૂ", "શરુ": "શરૂ",
    "ખુબ": "ખૂબ", "દ્રારા": "દ્વારા", "જયારે": "જ્યારે", "મહત્વ": "મહત્ત્વ", "બેન્ક": "બેંક", "ઈચ્છા": "ઇચ્છા",
    "નાંખવું": "નાખવું", "મુકવું": "મૂકવું", "રેલ્વે": "રેલવે", "વસતી": "વસ્તી", "હંમેશા": "હંમેશાં", "મહત્વપૂર્ણ": "મહત્ત્વપૂર્ણ",
    "મંજુર": "મંજૂર", "ઈજા": "ઇજા", "દ્રશ્ય": "દૃશ્ય", "ઉતરવું": "ઊતરવું",
}
# Everyday words the pack lacks, by the kind the rules decline them as: Leipzig's commonest words it has no entry for, names,
# English words and abbreviations aside.
GUJARATI_WORDS = {word: kind for kind, words in {
    "pron": "તેઓ કંઈ કંઈક એકબીજા",
    "particle": "ન",
    "det": "એવું તેવું જેટલું આટલું સૌ કેટલાય એકાદ",
    "postp": "વડે બદલ સાથે દ્વારા અંગે દરમિયાન ઉપરાંત મુજબ હેઠળ અનુસાર સહિત પ્રમાણે બદલે તરફથી વિના ઉપર ખાતે પૈકી મારફતે થકી દીઠ "
             "નિમિત્તે અંતર્ગત પ્રતિ",
    "conj": "છતાં તેમજ જેથી જેમ કેમકે કારણકે",
    "adv": "આથી એથી સૌથી ફરી ત્યારબાદ ત્યાર અત્યાર ફક્ત સાથોસાથ છેક ક્યાંય ક્યારેક ક્યારેય હંમેશાં ઘણીવાર એકવાર વાસ્તવમાં "
           "મોટાભાગે પુનઃ અત્રે અનુક્રમે ભાગરૂપે કારણોસર ઘેર પરત સાવ વાગ્યે રાત્રે આવતીકાલે ગઈકાલે અતિ અલબત્ત ઠેર માંડ "
           "સરળતાથી",
    "noun": "ઉપયોગ ટકા વિરોધ આયોજન કાર્યવાહી ચર્ચા વિકાસ અભ્યાસ જરૂર સ્થિતિ સારવાર કામગીરી કેન્દ્ર રોડ શાળા રમત ભરતી ઝડપ "
            "વધારો ઘટાડો દર જાહેરાત ખર્ચ સંખ્યા સેવા આધાર ઘટના વ્યવસ્થા જાણ લાભ ઉત્પાદન બેઠક યોજના રચના ભૂમિકા મેચ આવક "
            "પ્રયાસ નેતા સફળતા અધિકારી રાહત તક સ્થળ આદેશ વિજય તંત્ર ભગવાન સત્તા મંજૂરી વેચાણ સમસ્યા કાર માગણી નિર્માણ "
            "સામનો સજા અંત પસંદગી પ્રદેશ કોર્ટ મુખ્યમંત્રી માનવ ટન પ્રયત્ન વડો ભોગ કાર્યક્રમ પક્ષ વિભાગ યુનિવર્સિટી નાણું "
            "માંગ રજૂઆત રોકાણ મંત્રી ફેરફાર પાત્ર દેખાવ વિવાદ સંસ્થા ફોર્મ પ્રતિબંધ ક્ષમતા સંશોધન કાયદો દર્શન કૃષિ પાર રાહ "
            "સ્વરૂપ પ્રજા આરોપી ડિગ્રી મધ્ય કાર્ડ સંપર્ક રેકોર્ડ અમલ સરેરાશ સપાટી જોખમ બનાવ કપ લાગણી આક્ષેપ સુવિધા ગ્રામ "
            "પ્રારંભ વાહન મીટર શાસન અભિનય મોબાઈલ એવોર્ડ નાશ સોસાયટી સલામતી પદ્ધતિ સંભાવના ખાતરી ઉજવણી મુદ્દો પ્રોજેક્ટ "
            "રક્ષણ અબજ બજેટ નીતિ સૂચના ગેસ કામકાજ કર્મચારી માન્યતા અધ્યક્ષ ઉદ્યોગ શેર હેતુ અવસાન પ્રદર્શન સમર્થન દ્રષ્ટિ "
            "નિવેદન વલણ ટીકા પાલન કેપ્ટન સંચાલન નેટવર્ક જનતા સુનાવણી શંકા અપીલ ઓફિસ સિસ્ટમ નોંધ નિયમ માસ તેજી ઓપરેશન "
            "આતંકવાદી વિનંતી મથક આંદોલન કોર્પોરેશન નાગરિક જથ્થો કિસ્સો શ્રેણી આરોપ રિપોર્ટ કૌભાંડ સેન્ટર ભાર વેપારી ઉકેલ "
            "એરપોર્ટ ટોચ ઈંચ ફૂટ ભય આદર્શ ટેક્સ સાથ વિકલ્પ કારકિર્દી સંગ્રહ કેન્સર કવિતા પાર્ક વર્ણન ટ્રાફિક ટેકનોલોજી "
            "પ્રચાર કિલો કથા સ્પષ્ટતા કિલોમીટર સંબંધી લોન ફેશન કમિશનર જરૂરિયાત દરખાસ્ત સંરક્ષણ સ્પર્ધા સ્ટાફ સૂત્ર સંકેત "
            "સત્ય સચિવ ચેતવણી અપેક્ષા ગ્રુપ પ્રયોગ જોડાણ જામીન સહકાર પોસ્ટ પુરસ્કાર પર્યાવરણ કન્યા અંતર ભૂલ નફો પદ જીત "
            "ધારાસભ્ય જૂથ માર્કેટ વિચારણા ઇજા હાલત પાલિકા વિતરણ આકર્ષણ ભલામણ પરાજય માલ ધમકી માહોલ વર્તન પ્રવૃત્તિ "
            "કોમ્પ્યુટર માલિક દિગ્દર્શક સર્વે સુધારો આશ્ચર્ય નોટિસ સંતાન કાળ દૃશ્ય ક્રમ દાન છૂટ અકસ્માત લાઇન કમાણી ધારણા "
            "કોચ લક્ષણ અભાવ વૃદ્ધિ નિકાલ પ્રેરણા દિવાળી તત્ત્વ પ્રદાન યુવક સંઘ પ્રોફેસર સાંસદ પ્રશંસા ચેપ માર ગાળો પત્રકાર "
            "ડેટા મીડિયા પ્રતિનિધિ રોષ એજન્સી નિર્માતા ઊર્જા આહાર કાળજી સામગ્રી આગમન વોર્ડ સંપત્તિ ગોલ વસ્ત્ર મેડલ અભિપ્રાય "
            "વચન યોગદાન જોગવાઈ સમયગાળો આમંત્રણ મુક્તિ માત્રા ભંડોળ સૂચન જજ શાસક સ્કોર આક્રમણ ખુલાસો કદ ગૌરવ કબજો સૌંદર્ય "
            "આઝાદી હિંમત વિલંબ પાયો દાગીનો ભીડ રાજકારણ મેનેજર આચાર્ય લક્ષ્ય પંચાયત ટ્રક પરિવહન ઓર્ડર કાર્યકર વિસ્ફોટ વસવાટ "
            "મતલબ પડદો આત્મહત્યા મંત્રાલય પ્રસારણ ઘાટ અભિયાન સભા અભિગમ ક્રિયા સત્ર લૂંટ મૂલ્ય કટોકટી ચેનલ રાજકારણી મંડળ "
            "ગુણવત્તા વિદાય સરખામણી મોટાભાગ તત્વ બોર્ડ સંદર્ભ ધારણ અનામત ભંગ પ્રભાવ સર્જન બિલ હવામાન દલીલ બચાવ "
            "સન્માન કલ્યાણ પ્રોત્સાહન સૈન્ય બોમ્બ વિધાનસભા રૂમ સરળતા રાત્રિ તપાસ સમજ શોધ સહાય ટેકો નાયક "
            "અભિનેત્રી",
    "adj": "મુખ્ય અનેક ખાસ કુલ સમગ્ર ચાલુ આગામી પ્રાપ્ત ગંભીર આર્થિક રાજકીય સરકારી સ્થાનિક હાજર વ્યક્ત સ્પષ્ટ રાષ્ટ્રીય ચોક્કસ ગત "
           "સમાન આધારિત સંપૂર્ણ પૂર્ણ ઉચ્ચ ઉલ્લેખનીય વિશેષ પ્રાથમિક વિદેશી વિશાળ સામાજિક શ્રેષ્ઠ માનસિક જવાબદાર મજબૂત શક્ય "
           "ભૂતપૂર્વ સાબિત નોંધપાત્ર મુક્ત સંયુક્ત ખાલી વૈશ્વિક સક્રિય નિષ્ફળ લાગુ કુદરતી અંગત કેન્દ્રીય શારીરિક સંબંધિત "
           "વિરોધી રદ નિયમિત ગેરકાયદે સત્તાવાર મહાન પરંપરાગત આંતરિક જારી ઉત્પન્ન પ્રગટ ઉપયોગી કાર્યરત ઉપસ્થિત વ્યસ્ત આખરી "
           "લશ્કરી અસરકારક મર્યાદિત શૈક્ષણિક નાણાકીય સ્થાપિત એકમાત્ર મહત્ત્વપૂર્ણ ઉત્તમ સ્થિર શુભ શહેરી વિશિષ્ટ તબીબી "
           "સાંસ્કૃતિક નિશ્ચિત ઉગ્ર તાત્કાલિક વાસ્તવિક ભવ્ય વહીવટી કાયમી વરિષ્ઠ આકર્ષક સ્થાયી ઝેરી મહત્તમ મંજૂર પ્રભાવિત કાનૂની "
           "જંગી સક્ષમ ગ્રામ્ય અનુભવી સલામત પ્રચલિત સ્વાભાવિક મૂળભૂત નિવૃત્ત પ્રારંભિક નારાજ સઘન જીવંત ગુપ્ત જોખમી પશ્ચિમી "
           "નિવાસી હળવું બાહ્ય વિપરીત માધ્યમિક જાતીય સહકારી આક્રમક માન્ય દૈનિક ફરજિયાત સંભવિત પરેશાન શુદ્ધ કૃત્રિમ ભાવિ સર્વ "
           "જાગૃત સંવેદનશીલ સ્વસ્થ રસપ્રદ અધિક કેન્દ્રિત ભૌતિક શક્તિશાળી શંકાસ્પદ ઘાયલ ગાઢ નકારાત્મક જપ્ત ખાદ્ય તોફાની દેશી "
           "યથાવત આકસ્મિક વિસ્તૃત કિંમતી તેજ યુવાન યુવા ઉપરોક્ત તરફી લગતું પાસ જમા વીજ અમેરિકન મહત્ત્વનું અંતિમ "
           "ઝડપી મેડિકલ વ્યાપક પુરવાર સાજું હિન્દુ સંખ્યાબંધ હાંસલ રવાના નિર્દોષ વરસાદી એકત્ર",
    "verb": "ધરાવવું મેળવવું પાડવું મૂકવું કરાવવું ગુમાવવું ભજવવું અટકાવવું વધારવું નોંધવું ઉઠાવવું દર્શાવવું ઉમેરવું તોડવું ઊતરવું "
            "લગાવવું યોજવું ફટકારવું રોકવું ચલાવવું સૂચવવું ગણાવવું ફગાવવું ચૂકવું ટકવું ફાટવું ભાગવું નાસવું ધસવું મચવું "
            "અનુભવવું નોંધાવવું સંકળાવું માગવું",
}.items() for word in words.split()}
# Links of the pack's tables that are wrong: its template conjugates the irregular verbs as regular ones (જ "indeed" and જય
# "victory" as જવું's, સ as હોવું's).
WRONG_LINKS = {("જ", "જવું"), ("જય", "જવું"), ("જયું", "જવું"), ("જઓ", "જવું"), ("જઉં", "જવું"), ("જત", "જવું"), ("સ", "હોવું"),
               ("ખાયું", "ખાવું"), ("ખાવાય", "ખાવું"), ("લેઉં", "લેવું"), ("દેઉં", "દેવું"),
               ("ભરત", "ભરવું"), ("રવિ", "રવિવાર"), ("નાયક", "નાયિકા"), ("અભિનેત્રી", "અભિનેતા")}
# A word of the pack that another word's form is spelled as keeps its count (ફરી "again", not ફરવું's "having turned"; રમત
# "game", ગાય "cow", બદલે "instead"), but for these, which texts use mostly as the other word's form: પડી "fell" (not
# "packet"), છો "you are", પૂરી "complete" (not the bread), નાની "small".
FORM_FIRST = {"પડી", "છો", "છોડી", "મરી", "ખસી", "છૂટી", "જોતી", "આપો", "પૂરી", "નાની", "નાનો", "કાળી"}
# Corpus spellings that are no words: letters, English words in Gujarati letters.
NOT_WORDS = {"ઓફ", "એન્ડ", "ધ", "ઃ", "ઇન", "ટુ", "ફોર", "ઓન", "બી", "સી", "ડી", "તા", "ડૉ", "શ્રી", "મિ",
             # The abbreviation of rupees (રૂ. 500), not રૂ "cotton"; surnames and places the pack has as nouns.
             "રૂ", "પટેલ", "ગાંધી", "મોદી", "શાહ", "ખાન", "લંડન", "કાશ્મીર", "કચ્છ"}


# A passive shortens the stem's first long vowel (માન: મનાય, જાણ: જણાય, મૂક: મુકાય, આપ: અપાય, બાંધ: બંધાય).
SHORTER = {"ા": "", "ી": "િ", "ૂ": "ુ", "આ": "અ", "ઈ": "ઇ", "ઊ": "ઉ"}


def shortened(stem):
    """[stem] with its first long vowel short, as a passive has it."""
    for i, c in enumerate(stem[:-1]):
        if c in SHORTER and (i == 0 or c not in "આઈઊ"):
            return stem[:i] + SHORTER[c] + stem[i + 1:]
    return stem


def verb_forms(word, table):
    """A verb's (-વું) forms: its table's, IRREGULAR's or the rules', and its infinitive's."""
    if not word.endswith("વું") or len(word) < 4:
        return set(table)
    base = word[:-2]
    forms = {base + t for t in INFINITIVE_TAILS}
    if word in IRREGULAR:
        return forms | set(IRREGULAR[word].split())
    stem = word[:-3]
    if stem.endswith("હે"):
        # રહેવું, કહેવું: the consonant stem's endings (રહું, રહ્યું, રહીશ, રહીને) and the -e stem's (રહેશે, રહેતો, રહેલું).
        short = stem[:-1]
        forms |= {short + t for t in CONSONANT_STEM_TAILS if not t.startswith(("શ", "ત", "ે", "જ", "ન"))} - {short}
        forms |= {stem, stem + "શે", stem + "શો", stem + "જે", stem + "જો", stem + "નાર", stem + "નારા"}
        forms |= {stem + t for t in ("તો", "તી", "તું", "તા", "તાં", "લો", "લી", "લું", "લા", "લાં", "લ")}
        forms |= {short + t for t in PASSIVE_TAILS}
    elif consonant_final(stem):
        forms |= {stem + t for t in CONSONANT_STEM_TAILS}
        forms |= {stem + t for t in PASSIVE_TAILS} | {shortened(stem) + t for t in PASSIVE_TAILS}
    else:
        forms |= {stem + t for t in VOWEL_STEM_TAILS}
        forms.add(stem + ("ય" if stem.endswith("ા") else "એ"))
    return forms | set(table)


def lexicon(dictionaries=DICTIONARIES):
    """The pack's words by kind, its forms tables (form -> words), and the words that are only names."""
    db = sqlite3.connect(os.path.join(dictionaries, "gu-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if " " in word or "-" in word or not GUJARATI_WORD.fullmatch(word):
            continue
        # A language's name is an everyday word (ગુજરાતી); other names are left out.
        if pos == "name" and any("language" in g for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS:
            kinds[word].add(pos)
    for word, kind in GUJARATI_WORDS.items():
        kinds[word].add(kind)
    folded = {variant: word for variant, word in SPELLINGS.items() if variant in kinds and word in kinds and variant != word}
    for variant in folded:
        del kinds[variant]
    table = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemma = folded.get(lemma, lemma)
        if (" " in lemma or "-" in lemma or lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS
                or lemma in PRONOUNS or form in GUJARATI_FORMS or not GUJARATI_WORD.fullmatch(form)):
            continue
        table[form].add(lemma)
    return kinds, table, names - set(kinds)


def word_forms(word, ks, table_of):
    """[word]'s forms by its kinds [ks]: a verb's, an adjective's, a noun's (numbers' and determiners' as nouns')."""
    forms = set()
    if word in PRONOUNS:
        return set(PRONOUNS[word].split())
    if "verb" in ks:
        forms |= verb_forms(word, table_of.get(word, ()))
    if "adj" in ks or "det" in ks or "pron" in ks and word.endswith("ું"):
        forms |= adjective_forms(word)
    elif ks & NOMINAL_KINDS:
        forms |= nominal_forms(word)
    # A postposition or adverb takes the genitive, થી and માં after it too (માટેની, પરથી, પાસેથી, ત્યાંથી, સુધીમાં).
    if ks & {"postp", "adv", "prep"}:
        forms |= {word + t for t in ADVERB_TAILS}
    return forms | table_of.get(word, set())


# Gujarat Samachar's misspellings (see the module's doc), as the spellings they may stand for: what is always wrong, and
# what is wrong where the other sources write the repaired spelling more often.
ALWAYS = [("ાા", "ા"), ("ુ્ર", "્રુ"), ("ૂ્ર", "્રૂ")]
SAMACHAR_REPAIRS = [(re.compile("રૃ"), ("રૂ", "રુ")), (re.compile(f"ઘ(?=[ુૂ{VIRAMA}])"), ("ધ",)),
                    (re.compile(f"(?<=[ક-હ])ય(?=[ાુેો])"), (VIRAMA + "ય",))]


def repaired(word, clean):
    """[word] of a Gujarat Samachar page as its spelling: the repair the other sources' counts [clean] have most, if more."""
    for wrong, right in ALWAYS:
        word = word.replace(wrong, right)
    best, best_count = word, clean.get(word, 0)
    candidates = {word}
    for pattern, rights in SAMACHAR_REPAIRS:
        candidates |= {pattern.sub(right, c) for c in candidates for right in rights if pattern.search(c)}
    for c in sorted(candidates):
        if clean.get(c, 0) > best_count:
            best, best_count = c, clean[c]
    return best


def _leipzig_sentences(path):
    """A Leipzig corpus's sentences, each with the site it is from (its sources' host, when the corpus has them)."""
    with tarfile.open(path) as tar:
        members = {m.name.rsplit("-", 1)[-1]: m for m in tar.getmembers() if m.isfile()}
        hosts = {}
        if "sources.txt" in members and "inv_so.txt" in members:
            source_host = {}
            for line in io.TextIOWrapper(tar.extractfile(members["sources.txt"]), encoding="utf-8"):
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 2:
                    source_host[parts[0]] = re.sub(r"^https?://(www\.)?", "", parts[1]).split("/")[0]
            for line in io.TextIOWrapper(tar.extractfile(members["inv_so.txt"]), encoding="utf-8"):
                parts = line.split()
                if len(parts) == 2:
                    hosts[parts[1]] = source_host.get(parts[0], "")
        for line in io.TextIOWrapper(tar.extractfile(members["sentences.txt"]), encoding="utf-8"):
            number, _, sentence = line.rstrip("\n").partition("\t")
            yield sentence, hosts.get(number, "")


def tokens(text):
    """Gujarati words as the reader cuts them: in NFC, without joiners, runs of the block's letters and signs."""
    return GUJARATI_WORD.findall(unicodedata.normalize("NFC", text).translate(JOINERS))


def corpus_counts(download):
    """Each kind of text's word counts (Gujarat Samachar's repaired), and the corpora counted."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    used = []
    for kind, corpora in (("news", LEIPZIG_NEWS), ("wikipedia", LEIPZIG_WIKIPEDIA)):
        for corpus in corpora:
            path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
            if path is None:
                continue
            used.append(corpus)
            seen = set()
            for sentence, host in _leipzig_sentences(path):
                # Wikipedia's village and town pages repeat their sentences with other numbers.
                if kind == "wikipedia":
                    key = re.sub(r"[\d૦-૯]+", "0", sentence)
                    if key in seen:
                        continue
                    seen.add(key)
                into = counts["samachar" if host.endswith("gujaratsamachar.com") else kind]
                for word in tokens(sentence):
                    into[word] += 1
    clean = defaultdict(int)
    for kind in ("news", "wikipedia"):
        for word, n in counts[kind].items():
            clean[word] += n
    samachar = defaultdict(int)
    for word, n in counts["samachar"].items():
        samachar[repaired(word, clean)] += n
    counts["samachar"] = samachar
    return counts, used


def build(size, download, licence, dictionaries=DICTIONARIES):
    """The list: the lexicon's words by their forms' counts, each kind of text weighted by WEIGHTS."""
    kinds, table, names = lexicon(dictionaries)
    by_kind, used = corpus_counts(download)
    label = f"Leipzig Corpora Collection {', '.join(used)} ({licence})"
    counts = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in by_kind.items():
        total = sum(kind_counts.values())
        for form, n in kind_counts.items():
            counts[form] += WEIGHTS[kind] * n / total
            raw[form] += n

    table_of = defaultdict(set)
    for form, lemmas in table.items():
        for lemma in lemmas:
            table_of[lemma].add(form)
    # A verb's forms (its table's and the rules') and a noun's, adjective's or pronoun's, apart: form -> words.
    verbal, nominal = defaultdict(set), defaultdict(set)

    def add(word, as_word, ks):
        made = with_short_i(word_forms(word, ks, table_of) | {word})
        verb_made = with_short_i(verb_forms(word, table_of.get(word, ()))) if "verb" in ks else set()
        for form in made:
            if form in counts and form != as_word:
                (verbal if form in verb_made else nominal)[form].add(as_word)

    for word, ks in kinds.items():
        add(word, word, ks)
    for variant, word in SPELLINGS.items():
        if word in kinds and variant != word:
            add(variant, word, kinds[word])
    for form, word in GUJARATI_FORMS.items():
        if form != word:
            for f in with_short_i({form}):
                if f in counts:
                    nominal[f] = {word}

    def candidates(form):
        """(the word the form is itself, or None; the other words it is a form of)."""
        if form in GUJARATI_FORMS and GUJARATI_FORMS[form] != form:
            return None, {GUJARATI_FORMS[form]}
        if form in NOT_WORDS or form in names and not table.get(form):
            return None, set()
        own = form if form in kinds else long_i(form) if long_i(form) in kinds else None
        if own and own not in FORM_FIRST:
            return own, set()
        verbs = verbal.get(form, set()) - {form}
        nouns = nominal.get(form, set()) - {form}
        # A verb's form is no one-syllable word with an ending (છે is હોવું's, not છ "six" + ે).
        if verbs:
            nouns = {w for w in nouns if len(w) > 2}
        # A noun's form goes to the longest words it is a form of (રાતે is રાત + ે, not રા + તે), a letter's difference aside
        # (છોકરાને is છોકરો's and છોકરું's alike).
        if nouns:
            longest = max(len(w) for w in nouns)
            nouns = {w for w in nouns if len(w) >= longest - 1}
        return None, verbs | nouns

    found_heads = {form: candidates(form) for form in counts}
    alone = defaultdict(float)
    for form, (own, others) in found_heads.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    for form, share in counts.items():
        own, others = found_heads[form]
        if own and others and max(alone.get(o, 0.0) for o in others) <= share:
            heads = {own}
        else:
            heads = others or ({own} if own else set())
        if not heads:
            continue
        weights = {h: alone.get(h, 0.0) for h in sorted(heads)}
        whole = sum(weights.values())
        for head in weights:
            part = weights[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            totals[head] += raw[form]
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    ranked = sorted((w for w in scores if totals[w] >= LEAST_COUNT), key=lambda w: (-scores[w], w))[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


# The checker's reading of a word the list lacks: the rules' forms of the list's words, built once per list.
_CHECKER = {}


@lru_cache(maxsize=1)
def _kinds():
    return lexicon()[0]


def name_forms(name):
    """A name's forms with the postpositions written on (રામનો, મીનાને, અમદાવાદમાં, સુરતથી), as a noun's."""
    forms = {name + t for t in CASE_TAILS} | {name + ("ે" if consonant_final(name) else "એ")}
    return with_short_i(forms)


def checker_word(token, words, ranks, names=frozenset()):
    """
    The list's word [token] is a form of by the rules, as a key of [words] (the checker's list: key -> (rank, word, forms)),
    the commonest when it may be several; "\\0name" when it is a case form of one of the course's [names]; or None.
    """
    for name in names:
        if not name.startswith("\u0000") and token.startswith(name) and token in name_forms(name):
            return "\u0000name"
    index = _CHECKER.get(id(words))
    if index is None:
        kinds = _kinds()
        index = {}
        for key, (rank, _, _) in words.items():
            for form in with_short_i(word_forms(key, kinds.get(key, {"noun"}), {}) | {key}):
                if form not in index or rank < index[form][0]:
                    index[form] = (rank, key)
        _CHECKER.clear()
        _CHECKER[id(words)] = index
    hit = index.get(token)
    return hit[1] if hit else None
