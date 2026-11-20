"""
The Somali frequency list (build_frequency_list.py --language so).

Somali puts its articles, demonstratives and possessives on the end of a noun (nin "man", ninka "the man", ninkii "the man
(mentioned)", ninkan "this man", ninkayga "my man"; magaalo "town", magaalada "the town"; bil "month", bisha "the month"), its
persons and tenses on the end of a verb (qor "write", qoraa, qortay, qornay, qorayaa, qoraysaa), and runs its focus and
verbal particles together with the subject pronoun that follows them (waa + aan "waan", waxa + uu "wuxuu", la + u "loo",
ma + uu "muu"). The English Wiktionary has about 1,100 Somali words and few of their forms, so each text word is read here
as the pack's word or form, a noun with its determiner (nouns_of), a plural (plurals_of), a verb with its ending (verbs_of),
an adjective with its copula (adjectives_of), or one of the particle clusters (SOMALI_FORMS); a word of the lexicon keeps
its own count, and a form of several words is shared by how common each is by the forms of no other.

The pack lacks most of the little words a sentence is made of (ayaa, ma, in, la, aan, uu, ay, kale, badan): SOMALI_WORDS
lists those and other everyday words it lacks, by hand, with their part of speech and an English gloss for the writers.

Counted from Leipzig's news (2011 and 2020) and community corpora, OPUS's CCAligned web text and TED talks and Leipzig's
Wikipedia sentences, each the share of the blend WEIGHTS gives. A capitalised word inside a sentence is a name and not
counted.
"""
import functools
import gzip
import io
import json
import os
import re
import sqlite3
import tarfile
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICTIONARIES = os.path.join(ROOT, "dictionaries")

LEIPZIG = [("news", "som_newscrawl_2011_100K"), ("news", "som_news_2020_10K"), ("community", "som_community_2017"),
           ("wikipedia", "som_wikipedia_2021_10K")]
OPUS = [("web", "CCAligned", "v1"), ("talks", "TED2020", "v1")]
# News and the community corpus (forums, blogs) are most of the text; the web crawl is a third as much, as much of it is
# boilerplate (online, hotel, software); TED's talks are a small sample of speech, Wikipedia a small one of its rarer words.
WEIGHTS = {"news": 0.4, "community": 0.35, "web": 0.12, "talks": 0.05, "wikipedia": 0.08}
LICENCES = {
    "leipzig": "terms at https://wortschatz.uni-leipzig.de/en/download",
    "CCAligned": "terms at https://opus.nlpl.eu/CCAligned/corpus/version/CCAligned",
    "TED2020": "CC BY-NC-ND 4.0",
}

# As the reader's wordCharacters (a-zA-Z'), with ´ ` ’ ‘ read as ': su'aal and la'aan are one word, and so are lo' "cattle"
# and da' "age", with the glottal stop at their end.
APOSTROPHES = str.maketrans("´`’‘", "''''")
WORD = re.compile(r"[A-Za-z]+(?:'[A-Za-z]+)*'?")
SENTENCE_START = re.compile(r"(?:^|[.!?:;…\"“”«»()\[\]♪-]\s*)$")
# A word must be counted this often (all its forms, all sources) to be listed.
LEAST_COUNT = 4
FORMS_LISTED = 300

# Parts of speech that are no words of a text.
NOT_WORDS = {"name", "prefix", "suffix", "infix", "interfix", "symbol", "character", "phrase", "proverb", "prep_phrase"}

# By hand: everyday words the pack lacks, with their part of speech and gloss.
SOMALI_WORDS = {
    # Focus and verbal particles, pronouns and their clitics.
    "ayaa": ("particle", "focus marker (after a noun phrase)"),
    "ma": ("particle", "not; question marker"),
    "aan": ("pron", "I, we (subject clitic); not (in subordinate clauses)"),
    "aad": ("pron", "you (subject clitic); very (adverb)"),
    "uu": ("pron", "he (subject clitic)"),
    "ay": ("pron", "she, they (subject clitic)"),
    "aynu": ("pron", "we (inclusive, subject clitic)"),
    "aannu": ("pron", "we (exclusive, subject clitic)"),
    "la": ("pron", "one, someone (impersonal subject); with"),
    "i": ("pron", "me (object clitic)"),
    "na": ("pron", "us (exclusive, object clitic); and (enclitic)"),
    "ina": ("pron", "us (inclusive, object clitic)"),
    "idin": ("pron", "you (plural, object clitic)"),
    "is": ("pron", "oneself, each other (reflexive)"),
    "soo": ("particle", "towards the speaker (directional)"),
    "sii": ("particle", "away, further (directional); give"),
    "kala": ("particle", "apart, separately"),
    "wada": ("particle", "together, all"),
    "ugu": ("particle", "most (superlative); in, for"),
    "ee": ("conj", "and; which (linking a noun to what describes it)"),
    "in": ("conj", "that (introducing a clause)"),
    "ha": ("particle", "let (optative); don't (with a negative imperative)"),
    "yaa": ("pron", "who"),
    "maxay": ("pron", "what, why"),
    "sidee": ("adv", "how"),
    "xaggee": ("adv", "where"),
    "halkee": ("adv", "where"),
    "goorma": ("adv", "when"),
    "immisa": ("pron", "how many, how much"),
    "kee": ("det", "which"),
    "kuwa": ("pron", "those, the ones"),
    "kaas": ("det", "that (masculine)"),
    "taas": ("det", "that (feminine)"),
    "kuwan": ("det", "these"),
    "kuwaas": ("det", "those"),
    "kii": ("det", "the one (mentioned)"),
    "tii": ("det", "the one (feminine, mentioned)"),
    "kale": ("adj", "other, another"),
    "kasta": ("det", "every, each"),
    "walba": ("det", "every, each"),
    "dhammaan": ("adj", "all"),
    "qaar": ("noun", "some, part"),
    "badan": ("adj", "many, much"),
    "si": ("noun", "way, manner"),
    "laakiin": ("conj", "but"),
    "balse": ("conj", "but"),
    "ama": ("conj", "or"),
    "mise": ("conj", "or (in questions)"),
    "hase": ("conj", "however (hase yeeshee)"),
    "waayo": ("conj", "because"),
    "maxaa": ("pron", "what"),
    "sabab": ("noun", "reason"),
    "ilaa": ("prep", "until, up to"),
    "kadib": ("adv", "afterwards"),
    "dabadeed": ("adv", "afterwards"),
    "hore": ("adj", "front, former, before"),
    "dambe": ("adj", "later, next, back"),
    "hadda": ("adv", "now"),
    "haatan": ("adv", "now"),
    "weli": ("adv", "still, yet"),
    "mar": ("noun", "time, occasion; once"),
    "markii": ("conj", "when (in the past)"),
    "marka": ("conj", "when, so"),
    "markaas": ("adv", "then"),
    "inta": ("conj", "while, as long as; the amount"),
    "intii": ("conj", "while, during"),
    "halkan": ("adv", "here"),
    "halkaas": ("adv", "there"),
    "halka": ("noun", "the place where"),
    "meel": ("noun", "place"),
    "dhan": ("noun", "side, direction; all"),
    "dhinac": ("noun", "side"),
    "hoos": ("noun", "under, below"),
    "kor": ("noun", "top, above"),
    "gudaha": ("noun", "inside"),
    "dibad": ("noun", "outside, abroad"),
    "dhex": ("noun", "middle, between"),
    "agagaar": ("noun", "surroundings, around"),
    "shalay": ("adv", "yesterday"),
    "berri": ("adv", "tomorrow"),
    "maanta": ("adv", "today"),
    "xalay": ("adv", "last night"),
    "caawa": ("adv", "tonight"),
    "saaka": ("adv", "this morning"),
    "dhawaan": ("adv", "recently, soon"),
    "had": ("adv", "always (had iyo jeer)"),
    "jeer": ("noun", "time (had iyo jeer)"),
    "kaliya": ("adv", "only"),
    "sidoo": ("adv", "likewise (sidoo kale: also)"),
    "sidaas": ("adv", "so, like that"),
    "sidan": ("adv", "like this"),
    "haa": ("intj", "yes"),
    "maya": ("intj", "no"),
    "mahadsanid": ("intj", "thank you"),
    "haye": ("intj", "okay"),
    "waaw": ("intj", "wow"),
    "salaan": ("noun", "greeting"),
    "nabad": ("noun", "peace"),
    # Verbs.
    "tag": ("verb", "go"),
    "kar": ("verb", "be able, can"),
    "rab": ("verb", "want"),
    "noqo": ("verb", "become"),
    "odhan": ("verb", "say"),
    "ool": ("verb", "be (in a place)"),
    "aqoon": ("verb", "know"),
    "og": ("adj", "aware, knowing"),
    "dhac": ("verb", "happen, fall"),
    "bax": ("verb", "go out, leave"),
    "gaar": ("verb", "reach, arrive"),
    "dhig": ("verb", "put, set"),
    "dhis": ("verb", "build"),
    "dil": ("verb", "kill"),
    "dir": ("verb", "send"),
    "qab": ("verb", "have, hold"),
    "baahan": ("adj", "in need of"),
    "fahan": ("verb", "understand"),
    "hel": ("verb", "find, get"),
    "bixi": ("verb", "pay, take out"),
    "tus": ("verb", "show"),
    "weydii": ("verb", "ask"),
    "seexo": ("verb", "sleep"),
    "toos": ("verb", "wake up"),
    "xir": ("verb", "close, tie"),
    "iibi": ("verb", "sell"),
    "iibso": ("verb", "buy"),
    "shaqee": ("verb", "work"),
    "akhri": ("verb", "read"),
    "dhagayso": ("verb", "listen"),
    "sug": ("verb", "wait"),
    "isticmaal": ("verb", "use"),
    "joogso": ("verb", "stop (oneself)"),
    "raac": ("verb", "follow, go with"),
    "celi": ("verb", "return, give back"),
    "muuqo": ("verb", "appear, be seen"),
    "dhammee": ("verb", "finish"),
    "dhammaad": ("noun", "end"),
    "bilaab": ("verb", "begin"),
    "qaybso": ("verb", "share"),
    "bari": ("noun", "east"),
    "galbeed": ("noun", "west"),
    "waqooyi": ("noun", "north"),
    "koonfur": ("noun", "south"),
    # Everyday nouns, adjectives and numbers.
    "dal": ("noun", "country"),
    "dowlad": ("noun", "government, state"),
    "gobol": ("noun", "region"),
    "degmo": ("noun", "district"),
    "sano": ("noun", "year"),
    "sannad": ("noun", "year"),
    "bil": ("noun", "month; moon"),
    "toddobaad": ("noun", "week"),
    "saacad": ("noun", "hour; watch"),
    "daqiiqad": ("noun", "minute"),
    "waqti": ("noun", "time"),
    "lacag": ("noun", "money"),
    "ciidan": ("noun", "army, troops"),
    "shacab": ("noun", "the public, people"),
    "bulsho": ("noun", "society, community"),
    "qoys": ("noun", "family"),
    "carruur": ("noun", "children"),
    "wiil": ("noun", "boy, son"),
    "gabadh": ("noun", "girl, daughter"),
    "haween": ("noun", "women"),
    "naag": ("noun", "woman"),
    "nolol": ("noun", "life"),
    "caafimaad": ("noun", "health"),
    "waxbarasho": ("noun", "education"),
    "iskuul": ("noun", "school"),
    "jaamacad": ("noun", "university"),
    "macallin": ("noun", "teacher"),
    "arday": ("noun", "student"),
    "af": ("noun", "language; mouth"),
    "eray": ("noun", "word"),
    "magac": ("noun", "name"),
    "su'aal": ("noun", "question"),
    "jawaab": ("noun", "answer; to answer"),
    "war": ("noun", "news"),
    "arrin": ("noun", "matter, issue"),
    "sheeko": ("noun", "story"),
    "qoraal": ("noun", "text, writing"),
    "maqaal": ("noun", "article"),
    "telefoon": ("noun", "telephone"),
    "gaari": ("noun", "car"),
    "baabuur": ("noun", "vehicle, lorry"),
    "diyaarad": ("noun", "aeroplane"),
    "waddo": ("noun", "road"),
    "jid": ("noun", "road, way"),
    "dhul": ("noun", "land, ground"),
    "xaafad": ("noun", "neighbourhood"),
    "tuulo": ("noun", "village"),
    "xarun": ("noun", "centre, headquarters"),
    "xafiis": ("noun", "office"),
    "dukaan": ("noun", "shop"),
    "isbitaal": ("noun", "hospital"),
    "masjid": ("noun", "mosque"),
    "kaniisad": ("noun", "church"),
    "albaab": ("noun", "door"),
    "qol": ("noun", "room"),
    "miis": ("noun", "table"),
    "kursi": ("noun", "chair"),
    "sariir": ("noun", "bed"),
    "dhar": ("noun", "clothes"),
    "kab": ("noun", "shoe"),
    "quraac": ("noun", "breakfast"),
    "qado": ("noun", "lunch"),
    "casho": ("noun", "dinner"),
    "kalluun": ("noun", "fish"),
    "khudaar": ("noun", "vegetables"),
    "miro": ("noun", "fruit"),
    "sonkor": ("noun", "sugar"),
    "milix": ("noun", "salt"),
    "kafee": ("noun", "coffee"),
    "bun": ("noun", "coffee"),
    "roodhi": ("noun", "bread"),
    "baasto": ("noun", "pasta"),
    "ukun": ("noun", "egg"),
    "subag": ("noun", "butter, ghee"),
    "geel": ("noun", "camels"),
    "lo'": ("noun", "cattle"),
    "ari": ("noun", "sheep and goats"),
    "ido": ("noun", "sheep (flock)"),
    "riyo": ("noun", "goats"),
    "eey": ("noun", "dog"),
    "cir": ("noun", "sky"),
    "roob": ("noun", "rain"),
    "dabayl": ("noun", "wind"),
    "kulayl": ("noun", "heat"),
    "qabow": ("adj", "cold"),
    "kulul": ("adj", "hot"),
    "jiilaal": ("noun", "dry season (winter)"),
    "gu'": ("noun", "spring rainy season; year"),
    "xagaa": ("noun", "summer (dry season)"),
    "dayr": ("noun", "autumn rainy season"),
    "subax": ("noun", "morning"),
    "galab": ("noun", "afternoon"),
    "fiid": ("noun", "evening"),
    "duhur": ("noun", "noon"),
    "sideed": ("num", "eight"),
    "waji": ("noun", "face"),
    "qoor": ("noun", "neck"),
    "nidaam": ("noun", "system, order"),
    "dhowr": ("num", "several, a few"),
    "dib": ("noun", "back (dib u: again, back)"),
    "guud": ("adj", "general, overall"),
    "muddo": ("noun", "period, while"),
    "dhexe": ("adj", "middle, central"),
    "saabsan": ("adj", "concerning, about"),
    "cid": ("noun", "anyone, party, side"),
    "tusaale": ("noun", "example"),
    "dhibaato": ("noun", "problem, trouble"),
    "go'aan": ("noun", "decision"),
    "fursad": ("noun", "opportunity, chance"),
    "guul": ("noun", "success, victory"),
    "heer": ("noun", "level"),
    "qaab": ("noun", "form, way"),
    "hab": ("noun", "method, way"),
    "talo": ("noun", "advice, plan"),
    "cabsi": ("noun", "fear"),
    "run": ("noun", "truth"),
    "taariikh": ("noun", "history; date"),
    "caan": ("adj", "famous"),
    "rasmi": ("adj", "official"),
    "dhab": ("adj", "real, true"),
    "bal": ("particle", "just, well (softening a request)"),
    "xal": ("noun", "solution"),
    "hawl": ("noun", "task, work"),
    "shir": ("noun", "meeting, conference"),
    "kulan": ("noun", "meeting"),
    "awood": ("noun", "power, ability"),
    "cod": ("noun", "voice; vote"),
    "amar": ("noun", "order, command"),
    "xil": ("noun", "post, duty"),
    "xilli": ("noun", "time, season"),
    "mudan": ("adj", "deserving"),
    "gargaar": ("noun", "help, aid"),
    "hub": ("noun", "weapon"),
    "ganacsi": ("noun", "trade, business"),
    "heshiis": ("noun", "agreement"),
    "bog": ("noun", "page"),
    "tayo": ("noun", "quality"),
    "halis": ("noun", "danger"),
    "horumar": ("noun", "progress, development"),
    "dumar": ("noun", "women"),
    "xitaa": ("adv", "even"),
    "iminka": ("adv", "now"),
    "waliba": ("adv", "especially, moreover"),
    "goor": ("noun", "time, when"),
    "maaha": ("particle", "is not"),
    "la'aan": ("noun", "lack; without"),
    "maadaama": ("conj", "since, as"),
    "darteed": ("adv", "because of it"),
    "inkastoo": ("conj", "although"),
    "ruux": ("noun", "person, soul"),
    "cilmi": ("noun", "knowledge, science"),
    "dheeraad": ("noun", "extra, more"),
    "safar": ("noun", "journey, travel"),
    "qalab": ("noun", "equipment, tools"),
    "labaatan": ("num", "twenty"),
    "soddon": ("num", "thirty"),
    "afartan": ("num", "forty"),
    "konton": ("num", "fifty"),
    "lixdan": ("num", "sixty"),
    "toddobaatan": ("num", "seventy"),
    "siddeetan": ("num", "eighty"),
    "sagaashan": ("num", "ninety"),
    "boqol": ("num", "hundred"),
    "kun": ("num", "thousand"),
    "malaayiin": ("num", "millions"),
    "hal": ("num", "one (before a noun)"),
    "mid": ("num", "one"),
    "kowaad": ("num", "first"),
    "labaad": ("num", "second"),
    "saddexaad": ("num", "third"),
    "afaraad": ("num", "fourth"),
    "shanaad": ("num", "fifth"),
    "wanaag": ("noun", "goodness"),
    "fiican": ("adj", "good, fine"),
    "xun": ("adj", "bad"),
    "cusub": ("adj", "new"),
    "duq": ("adj", "old (person)"),
    "da'": ("noun", "age"),
    "gaaban": ("adj", "short"),
    "dheer": ("adj", "long, tall"),
    "yar": ("adj", "small, little"),
    "dhow": ("adj", "near"),
    "fog": ("adj", "far"),
    "adag": ("adj", "hard, strong"),
    "fudud": ("adj", "easy, light"),
    "qurux": ("noun", "beauty"),
    "qurxoon": ("adj", "beautiful"),
    "faraxsan": ("adj", "happy"),
    "murugaysan": ("adj", "sad"),
    "daallan": ("adj", "tired"),
    "gaajaysan": ("adj", "hungry"),
    "harraaddan": ("adj", "thirsty"),
    "jaban": ("adj", "cheap; broken"),
    "qaali": ("adj", "expensive, dear"),
    "nadiif": ("adj", "clean"),
    "wasakh": ("adj", "dirty"),
    "muhiim": ("adj", "important"),
    "diyaar": ("adj", "ready"),
    "sax": ("adj", "correct"),
    "khalad": ("noun", "mistake"),
    "xor": ("adj", "free"),
    "caddaan": ("adj", "white"),
    "madow": ("adj", "black"),
    "cagaar": ("adj", "green"),
    "jaalle": ("adj", "yellow"),
    "guduud": ("adj", "red"),
}
# Forms whose word the rules would miss or get wrong: the particle clusters, the irregular verbs, spellings of a word.
SOMALI_FORMS = {}
# The focus particles and the subject pronoun that follows them: waan "(focus) I", wuxuu "(what) he", ayay "(focus) she".
for _focus, _clusters in {
    "waa": ["waan", "waad", "wuu", "way", "waannu", "waynu", "weynu", "waanu", "wey", "waana", "weeye", "waaye"],
    "waxa": ["waxaa", "waxaan", "waxaad", "wuxuu", "waxay", "waxaannu", "waxaynu", "waxeynu", "waxaanu", "waxey", "waxuu", "waxan",
             "waxad"],
    "ayaa": ["ayaan", "ayaad", "ayuu", "ayay", "ayey", "ayaannu", "ayaynu", "ayeynu", "ayaanu"],
    "baa": ["baan", "baad", "buu", "bay", "baannu", "baynu"],
    "maxaa": ["maxaan", "maxaad", "muxuu", "maxaynu", "maxaannu"],
    "ma": ["maan", "maad", "muu", "may", "mey", "maannu", "maynu", "miyaa", "miyaad", "miyuu", "miyay", "miyey", "miyaan",
           "miyaannu", "miyaynu", "majiro", "majirto"],
    "in": ["inuu", "inay", "iney", "inaan", "inaad", "inaynu", "inaannu", "inaanu", "inuusan", "inaysan", "inaanay"],
    "haddii": ["hadduu", "hadday", "haddaan", "haddaad", "haddaynu", "haddaannu", "hadii", "haddey", "hadduusan"],
    "mar": ["markuu", "markay", "markaan", "markaad", "markaynu", "markaannu", "markey", "markaa", "markasta"],
    "inta": ["intuu", "intay", "intaan", "intaad", "intey"],
    "sida": ["siduu", "siday", "sidaan", "sidaad", "sidey", "sidii"],
    "ha": ["hay", "haan", "haad", "hayna"],
    "yaa": ["yuu", "yaan", "yay"],
    # The pronoun with the negative aan: uusan "he not", aysan "she, they not", aanan "I not".
    "uu": ["uusan", "uusu", "uun"],
    "ay": ["aysan", "aysu", "ayan", "aanay", "aysay"],
    "aad": ["aadan", "aadna"],
    "aan": ["aanan", "aanu", "aannan", "aanad"],
}.items():
    for _cluster in _clusters:
        SOMALI_FORMS.setdefault(_cluster, _focus)
# The object pronouns and the impersonal la run together with the prepositions that follow them (la + u "loo", i + ku
# "igu", na + ka "naga", u + ka "uga"); a cluster is filed under its first word.
for _first, _clusters in {
    "la": ["loo", "lagu", "laga", "lala", "looga", "loogu", "loola", "lagula", "lagaga", "lagama", "lagu", "lama", "laguma",
           "looma", "lagaa", "lasoo", "laysku", "laysu", "layska"],
    "i": ["ii", "igu", "iga", "ila", "iigu", "iiga", "igula", "igaga"],
    "ku": ["kuu", "kugu", "kaa", "kula", "kuugu", "kaaga", "kugula", "kaga", "kuma", "kusoo", "kuwada"],
    "na": ["noo", "nagu", "naga", "nala", "noogu", "nooga", "nagula"],
    "ina": ["inoo", "inagu", "inaga", "inala", "inoogu"],
    "idin": ["idiin", "idinku", "idinka", "idinla", "idiinku", "idiinka"],
    "u": ["uga", "ula", "uma", "ugala", "ugama"],
    "ka": ["kama", "kasoo", "kaso"],
    "is": ["isku", "iska", "isu", "isla", "isugu", "iskaga", "isaga", "iskula", "isagu"],
}.items():
    for _cluster in _clusters:
        SOMALI_FORMS.setdefault(_cluster, _first)
# isaga "he" is a pronoun before it is is + ka.
SOMALI_FORMS.pop("isaga", None)
SOMALI_FORMS.pop("isagu", None)
SOMALI_FORMS.update({
    # ah "be": its persons and tenses.
    **{f: "ah" for f in ["ahay", "tahay", "yahay", "nahay", "tihiin", "yihiin", "ahaa", "ahayd", "ahaayeen", "ahaan", "ahayn",
                         "ahi", "ahow", "ahaada", "ahaado", "ahaato", "ahaadaan", "ahaanno", "ahaaday", "ahaatay", "ahaaneyn",
                         "ahaanayn", "ahaanaya", "ahaanayaa", "ahaanaysaa", "ahaa", "aheyd", "ihi", "tihiin", "tihid", "aheyn",
                         "ahaayeen", "ahaanayo", "ahaanayso"]},
    # leh "having": lahaa "had", leeyahay "has".
    **{f: "leh" for f in ["lahaa", "lahayd", "lahaayeen", "lahayn", "leeyahay", "leedahay", "leenahay", "leedihiin", "leeyihiin",
                          "lahaan", "lahaado", "lahaato", "lahaadaan", "lahaaday", "lahaatay", "lahaanayaa", "lahaanaya", "leeyay",
                          "lahaanayso", "leeyihay", "laheyd", "laheyn", "lahaanno", "leeyahay"]},
    # jecel "fond of": jeclahay "I like".
    **{f: "jecel" for f in ["jeclahay", "jeceshahay", "jecelnahay", "jeceshihiin", "jecelyihiin", "jeclaa", "jeclayd", "jecleyd",
                            "jeclaayeen", "jeclayn", "jecleyn", "jeclaan", "jeclaado", "jeclaaday", "jeclaatay", "jeclaanayaa"]},
    # og "aware": ogahay "I know", ogtahay "you know".
    **{f: "og" for f in ["ogahay", "ogtahay", "ognahay", "ogtihiin", "ogyihiin", "ogaa", "ogayd", "ogeyd", "ogaayeen", "ogayn",
                         "ogeyn", "ogsoon", "ogsooni"]},
    # iman "come": yimid "he came", timaaddaa "she comes", kaalay "come!".
    **{f: "iman" for f in ["yimi", "yimid", "timi", "timid", "nimi", "nimid", "yimaaddaa", "timaaddaa", "nimaadnaa", "timaaddaan",
                           "yimaaddaan", "yimaadeen", "timaadeen", "nimaadnay", "yimaado", "timaado", "nimaadno", "yimaadaan",
                           "yimaadda", "timaadda", "imanaya", "imanayaa", "imanaysaa", "imanaynaa", "imanayaan", "imanayay",
                           "imanaysay", "imanayeen", "imaanaya", "imaanayaa", "imaanaysaa", "imaanayso", "imaanayo", "imaan",
                           "imaada", "imow", "kaalay", "kaalaya", "imaatay", "imaaday", "imaadeen", "imanayn", "imaanin",
                           "iman", "yimaadeen", "timaadday", "yimaadday", "imaaddo", "yimaaddo"]},
    # odhan "say": yidhi/yiri "he said", tidhaahdaa "she says", dheh "say!".
    **{f: "odhan" for f in ["yidhi", "yiri", "tidhi", "tiri", "nidhi", "niri", "idhi", "iri", "yidhaahdaa", "yiraahdaa",
                            "tidhaahdaa", "tiraahdaa", "nidhaahnaa", "niraahnaa", "yidhaahdaan", "yiraahdaan", "tidhaahdaan",
                            "tiraahdaan", "yidhaahdeen", "yiraahdeen", "tidhaahdeen", "tiraahdeen", "nidhaahnay", "niraahnay",
                            "yidhaahdo", "yiraahdo", "tidhaahdo", "tiraahdo", "nidhaahno", "niraahno", "idhaahdo", "iraahdo",
                            "odhan", "oran", "dheh", "dhaha", "dhahay", "odhanaya", "oranaya", "odhanayaa", "oranayaa",
                            "odhanaysaa", "oranaysaa", "odhanayaan", "oranayaan", "odhanayay", "oranayay", "odhanayeen",
                            "oranayeen", "odhanin", "oranin", "yidhaahda", "yiraahda", "tidhaahda", "tiraahda", "yidhaahdaa",
                            "idhaahdaa", "iraahdaa", "idhi", "yiraa", "yidhaa", "lyiri"]},
    # ool "be (in a place)": yaal "it lies", taal, yaalla, yiil "it lay".
    **{f: "ool" for f in ["yaal", "taal", "naal", "yaalla", "taalla", "naalla", "yaallaan", "taallaan", "yiil", "tiil", "niil",
                          "yiilleen", "tiilleen", "yaala", "taala", "yaalaan", "yaalo", "taalo", "oolli", "oolaa", "oolay",
                          "oolaya", "oolin"]},
    # aqoon "know": yaqaan "he knows", aqaan "I know", yiqiin "he knew".
    **{f: "aqoon" for f in ["aqaan", "taqaan", "yaqaan", "naqaan", "taqaanniin", "yaqaanniin", "taqaaniin", "yaqaaniin",
                            "aqiin", "tiqiin", "yiqiin", "niqiin", "yiqiinneen", "tiqiinneen", "aqaanno", "yaqaanno", "taqaanno",
                            "naqaanno", "aqoonin", "yaqaan", "garan", "aqoonaya"]},
    # "The month", which the pack has as a word of its own ("date").
    "bisha": "bil",
    # The place words: halkii "the place (said)", halkaa "there".
    "kuwo": "kuwa", "kuwii": "kuwa", "kuwaasoo": "kuwaas", "kuwaasi": "kuwaas", "taasoo": "taas", "taasi": "taas", "tani": "tan",
    "kaasoo": "kaas", "kaasi": "kaas", "wixii": "wax", "sidaa": "sidaas", "intaa": "inta", "intaas": "inta", "horay": "hore",
    "horey": "hore", "ahaatee": "ah", "ahaadeen": "ah",
    "kaloo": "kale", "wuxu": "waxa", "aha": "ah", "usoo": "u",
    # arag "see": its stem is ark- before a vowel (arkay "he saw").
    **{f: "arag" for f in ["arkay", "arkey", "arko", "arki", "arkin", "arkaa", "arkeen", "arkaan", "arkaya", "arkayaa", "arkaysaa",
                           "arkayaan", "arkayay", "arkayo", "arka", "arkeyn", "arkayn"]},
    "halkii": "halka", "halkay": "halka", "halkaa": "halkaas", "halkaasi": "halkaas", "halkani": "halkan",
    # Spellings of one word.
    "waxa": "waxa",
    "sanadood": "sannad", "sanadii": "sannad", "sannadii": "sannad", "kastaa": "kasta", "kastaaba": "kasta", "halkuu": "halka",
})
# Spellings of a word, and pack entries that are a word's form: a text word so spelled, and its forms, are the word's.
SOMALI_VARIANTS = {"labo": "laba", "dawlad": "dowlad", "wakhti": "waqti", "sanad": "sannad", "keliya": "kaliya", "xidh": "xir",
                   "gabar": "gabadh", "erey": "eray", "taleefan": "telefoon", "magaalad": "magaalo", "xarunta": "xarun",
                   "qalabka": "qalab", "socda": "soco", "illaa": "ilaa", "hadii": "haddii", "siddeed": "sideed", "hada": "hadda",
                   "dhamaan": "dhammaan", "laakin": "laakiin", "wali": "weli", "xataa": "xitaa", "koowaad": "kowaad", "sadex": "saddex",
                   "danbe": "dambe", "dhawr": "dhowr", "howl": "hawl", "xili": "xilli", "qabo": "qab", "dheh": "odhan",
                   "jirka": "jidh", "jidka": "jid", "nidaamka": "nidaam", "dhaqdhaqaaqa": "dhaqdhaqaaq", "tago": "tag",
                   "yimaad": "iman", "imaad": "iman", "samay": "samee", "hey": "hay", "cayaar": "ciyaar", "dagal": "dagaal",
                   "dhamee": "dhammee", "dhamey": "dhammee", "shaqey": "shaqee", "ahay": "ah", "woqooyi": "waqooyi",
                   "asaga": "isaga", "ayaga": "iyaga", "anaga": "annaga", "jaceyl": "jacayl", "cunno": "cunto",
                   "dimuqraadi": "dimoqraadi", "shinbir": "shimbir", "jaale": "jaalle", "calanka": "calan", "wajiga": "waji",
                   "dabada": "dabo", "heesta": "hees", "timaha": "timo", "qoorta": "qoor", "bangiga": "banki", "bangi": "banki",
                   "musqusha": "musqul", "qorsheynta": "qorsheyn", "isticmaalaha": "isticmaal", "qumbaha": "qumbe",
                   "faa'idooyin": "faa'ido", "sugto": "sug", "maxa": "maxaa", "ey": "eey"}
# Words that are no words of an ordinary text: abbreviations, English, the corpora's markup.
SOMALI_EXCLUDED = {"nnkh", "ooyin", "al", "of", "the", "and", "by", "on", "for", "com", "www", "mr", "dr", "gmt", "df", "sh", "mm", "kmg", "oct",
                   "start", "home", "online", "software", "hotel", "our", "mai", "chiang", "dur", "deg"}

# A noun's determiners: their consonant (k, t and the shapes they take after the noun's last sound: g, h, nothing; d, sh,
# dh) and what follows it: the article (-ka, -kii), the demonstratives (-kan, -kaas, -keer), the question (-kee), the subject
# case (-ku, -kani, -kaasi) and the possessives (-kayga "my", -kaaga "your", -kiisa "his", -keeda "her", -keenna "our",
# -kooda "their"), with their subject (-kaygu) and remote (-kaygii) forms, and the short possessives of kin (hooyaday).
DETERMINER_CONSONANTS = ["k", "g", "h", "", "t", "d", "sh"]
_GUTTURALS = set("qxch'")


def _takes(consonant, stem):
    """Whether a noun ending as [stem] does takes the determiner's consonant in that shape: k after most consonants (ninka),
    g after a vowel, g, w or y (guriga, buugga, odayga), none after q, x, c, h or ' (xaqa, shaaha) and feminine dh (gabadha);
    t after most consonants (beerta), d after a vowel, d or a guttural (bisadda, nabadda), and none after d as texts often
    write it (dowlada)."""
    last = stem[-1]
    if consonant == "k":
        return last not in "aeiouwyg" and last not in _GUTTURALS
    if consonant == "g":
        return last in "aeiouwyg"
    if consonant == "t":
        return last not in "aeiould" and last not in _GUTTURALS and not stem.endswith("dh")
    if consonant == "d":
        return last in "aeioud" or last in _GUTTURALS
    return last in _GUTTURALS or stem.endswith("dh") or last == "d"


# Nouns of two letters that take determiners (afka, afkiisa); the rules read no other word that short.
SHORT_NOUNS = {"af"}
_POSSESSIVES = ["ayg", "aag", "iis", "eed", "eenn", "een", "ayag", "iinn", "iin", "ood"]
DETERMINER_ENDINGS = sorted(
    {"a", "u", "ii", "an", "aas", "ani", "aasi", "een", "eer", "ee", "uu", "aa", "ay", "iis", "eed", "ood"}
    | {p + v for p in _POSSESSIVES for v in ("a", "u", "ii")}
    | {p.replace("ayg", "ayd").replace("ayag", "ayad") + v for p in ("ayg", "ayag") for v in ("a", "u", "ii")}
    | {p[:-1] + "d" + v for p in ("aag",) for v in ("a", "u", "ii")},
    key=lambda e: -len(e))
# The enclitics that may close any word: -na "and", -se "but", -ba "every, at all".
ENCLITICS = ["na", "se", "ba"]

# Verb endings. A first-conjugation verb (qor "write", keen "bring", sii "give") takes them on its stem, with t as t, d, s or
# sh by the stem's last sound (qortay, kacday, siisay, gashay) and n (qornay, keennay); its progressive is the stem with -ay
# (qorayaa, qoraysaa). A second-conjugation verb (samee "make", jooji "stop") takes y, s and n (sameeyay, sameysay, sameynay,
# samayn); a third-conjugation verb (iibso "buy", fadhiiso "sit", qaado "take") takes -ad, -at and -an (iibsaday, iibsatay,
# iibsannay, iibsan) or, after a consonant, t (bartay, qaatay).
_PROGRESSIVE = ["aa", "saa", "naa", "aan", "saan", "ay", "say", "nay", "een", "seen", "o", "so", "no", "a", "sa", "na", "n",
                "ey", "sey", "ney", "oo", "in"]
FIRST_ENDINGS = (["a", "aa", "aan", "ay", "ey", "een", "eyn", "o", "i", "in", "an", "id", "ayn", "aya", "aysa", "ayna", "ow"]
                 + [t + e for t in ("t", "d", "s") for e in ("aa", "aan", "ay", "ey", "een", "o", "id", "a", "in")]
                 + ["n" + e for e in ("aa", "ay", "ey", "o", "a", "een", "in")]
                 + [p + e for p in ("ay", "ey") for e in _PROGRESSIVE])
SECOND_ENDINGS = (["y" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a", "in")]
                  + ["s" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")]
                  + ["n" + e for e in ("aa", "ay", "ey", "o", "a", "in", "")]
                  + ["n" + p + e for p in ("ay", "ey") for e in _PROGRESSIVE])
THIRD_ENDINGS = (["ad" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")]
                 + ["at" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")]
                 + ["an" + e for e in ("", "naa", "nay", "no", "in", "na")]
                 + ["an" + p + e for p in ("ay", "ey") for e in _PROGRESSIVE] + ["o", "ada", "da"])
CONTRACTED_ENDINGS = ["t" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")]
# The middle (third-conjugation) verbs the pack lacks, whose forms are read as their plain verb's: qaado "take for oneself"
# (qaatay "he took", qaadatay, qaadan) as qaad's.
MIDDLE_VERBS = {"qaad": "qaado", "bar": "baro", "dhig": "dhigo", "hel": "helo", "gal": "galo", "keen": "keeno", "sheeg": "sheego",
                "rab": "rabo", "dhis": "dhiso", "xir": "xirso", "isticmaal": "isticmaalo"}
# An adjective as a predicate (weynaa "was big", wanaagsanayd, fiicnayn) and its plural by doubling (yaryar, waaweyn).
ADJECTIVE_ENDINGS = ["aa", "ayd", "eyd", "ayn", "eyn", "aayeen", "i", "aan", "aato", "aado", "aaday", "aatay", "aaday", "aatay",
                     "aanaya", "aanayaa", "aanaysaa", "aada", "aanayso", "aanayo", "aaday", "eyay"]


def _vowel(c):
    return c in "aeiou"


class Lexicon:
    """The words of the so-en pack and SOMALI_WORDS, their forms, and the rules that read a text word as one of them."""

    def __init__(self, dictionaries=DICTIONARIES):
        db = sqlite3.connect(os.path.join(dictionaries, "so-en.sqlite"))
        self.kinds = defaultdict(set)
        for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
            if pos in NOT_WORDS or not re.fullmatch(r"[a-z]+(?:'[a-z]+)*", word):
                continue
            # The letters' names (xa, ba) are no words of a text.
            if all(g.startswith("The name of the Latin script letter") for x in json.loads(senses) for g in x.get("glosses", [])):
                continue
            self.kinds[word].add(pos)
        for word, (pos, _) in SOMALI_WORDS.items():
            if " " not in word:
                self.kinds[word].add(pos)
        self.forms = defaultdict(set)
        for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
            if lemma in self.kinds and form != lemma and re.fullmatch(r"[a-z]+(?:'[a-z]+)*", form):
                self.forms[form].add(lemma)
        for variant, word in SOMALI_VARIANTS.items():
            self.kinds.pop(variant, None)
            self.forms[variant] = {word}
        # A verb of four letters or more is its own noun as well (hadal "speech", dagaal "fight", safar "journey").
        self.nouns = {w for w, ks in self.kinds.items() if ks & {"noun", "adj", "num"} or "verb" in ks and len(w) > 3}
        self.verbs = {w for w, ks in self.kinds.items() if "verb" in ks and len(w) > 2}
        self.adjectives = {w for w, ks in self.kinds.items() if "adj" in ks and len(w) > 2}
        self.tails = defaultdict(set)
        for verb in self.verbs:
            for form in self.verb_forms(verb):
                if form != verb:
                    self.tails[form].add(verb)
        for variant, word in SOMALI_VARIANTS.items():
            if word in self.verbs:
                for form in self.verb_forms(variant):
                    if form not in self.kinds:
                        self.tails[form].add(word)
        for verb, middle in MIDDLE_VERBS.items():
            for form in self.verb_forms(middle) | {middle}:
                if form not in self.kinds:
                    self.tails[form].add(verb)
        self.adjective_forms = defaultdict(set)
        for word in self.adjectives:
            for form in self.adjective_forms_of(word):
                if form != word:
                    self.adjective_forms[form].add(word)

    @staticmethod
    def verb_forms(verb):
        """[verb]'s regular forms by its conjugation."""
        out = set()
        if verb.endswith("ee"):
            for base in (verb, verb[:-2] + "ey", verb[:-2] + "ay"):
                out |= {base + e for e in SECOND_ENDINGS}
        elif verb.endswith("i") and len(verb) > 3 and not verb.endswith("ii"):
            out |= {verb + e for e in SECOND_ENDINGS}
        elif verb.endswith("o") and len(verb) > 3:
            base = verb[:-1]
            out |= {base + e for e in THIRD_ENDINGS}
            out |= {base + "d" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")}
            out |= {base + "ot" + e for e in ("aa", "aan", "ay", "ey", "een", "o", "a")}
            out |= {base + "on" + e for e in ("", "naa", "nay", "no", "in")} | {base + "on" + p + e for p in ("ay", "ey") for e in _PROGRESSIVE}
            if not base.endswith("s") and not _vowel(base[-1]):
                out |= {base + e for e in CONTRACTED_ENDINGS}
                if base.endswith("d"):
                    out |= {base[:-1] + e for e in CONTRACTED_ENDINGS}
                if base.endswith("m"):
                    out |= {base[:-1] + "n" + e for e in CONTRACTED_ENDINGS}
        else:
            stem = verb
            for e in FIRST_ENDINGS:
                out.add(stem + e)
                if _vowel(stem[-1]) and e[0] in "aeio":
                    out.add(stem + "y" + e)
            if stem.endswith("l"):
                out |= {stem[:-1] + "sh" + e[1:] for e in FIRST_ENDINGS if e.startswith("t")}
            # A stem of two like syllables loses its second vowel before a vowel: arag arkay, hadal hadlay, maqal maqlay.
            m = re.fullmatch(r"([^aeiou]*)([aeiou])([^aeiou]+)\2([^aeiou]+)", stem)
            if m:
                short = m.group(1) + m.group(2) + m.group(3) + m.group(4)
                out |= {short + e for e in ("a", "aa", "aan", "ay", "ey", "een", "o", "i", "in", "id")}
                out |= {short + p + e for p in ("ay", "ey") for e in _PROGRESSIVE}
            # One with a takes i or e in its infinitive: kac kici, dhac dhici, tag tegi, gal geli.
            if re.fullmatch(r"[^aeiou]+a[^aeiou]+", stem):
                for v in ("i", "e"):
                    out |= {stem.replace("a", v) + e for e in ("i", "in")}
            if stem.endswith("m"):
                out |= {stem[:-1] + "n" + e for e in FIRST_ENDINGS if e.startswith("t")}
        return out

    @staticmethod
    def adjective_forms_of(word):
        out = {word + e for e in ADJECTIVE_ENDINGS}
        # A short adjective doubles in the plural: yar yaryar, weyn waaweyn, dheer dhaadheer.
        if len(word) <= 5:
            out.add(word + word)
            onset = re.match(r"[^aeiou]*", word).group(0)
            if onset:
                out.add(onset + "aa" + word)
                out.add(onset + "a" + word)
        return out

    def is_word(self, token):
        return token in self.kinds

    def singulars(self, stem):
        """The nouns [stem] may be the plural of: dameero dameer, magaalooyin magaalo, aabbayaal aabbe, buugag buug, baabuurro baabuur."""
        out = set()
        if stem in self.nouns:
            out.add(stem)
        for ending in ("ooyin", "oyin", "yaal", "ood", "o", "yo"):
            if stem.endswith(ending) and len(stem) >= len(ending) + 3:
                base = stem[:-len(ending)]
                if ending == "yaal":
                    cands = (base[:-1] + "e", base[:-1] + "o") if base.endswith("a") else ()
                elif ending in ("ooyin", "oyin", "ood"):
                    cands = (base, base + "o")
                else:
                    cands = (base, base[:-1] if len(base) > 2 and base[-1] == base[-2] else None)
                for cand in cands:
                    if cand and cand in self.nouns:
                        out.add(cand)
        # buugag of buug, dabab of dab: the last consonant again after a.
        m = re.fullmatch(r"(.*[aeiou])([^aeiou]+)(aa?)\2", stem)
        if m and m.group(1) + m.group(2) in self.nouns and (m.group(3) == "a" or re.search(r"([aeiou])\1$", m.group(1))):
            out.add(m.group(1) + m.group(2))
        return out

    @functools.lru_cache(maxsize=None)
    def nouns_of(self, token):
        """The nouns, adjectives or numbers [token] is with a determiner: ninka nin, magaalada magaalo, bisha bil, buugga buug,
        aabbaha aabbe, ninkayga nin, xaqa xaq, gabadha gabadh; or, when it is no such form of a word, the plural of one with
        or without one: dameeraha dameer (dameero), magaalooyinka magaalo; and a pronoun's subject and -oo forms (iyadoo iyada)."""
        direct, plural = set(), set()
        for ending in DETERMINER_ENDINGS:
            if not token.endswith(ending):
                continue
            pre = token[:-len(ending)]
            for consonant in DETERMINER_CONSONANTS:
                if consonant and not pre.endswith(consonant):
                    continue
                stem = pre[:-len(consonant)] if consonant else pre
                if len(stem) < 2:
                    continue
                cands = set()
                if consonant == "sh":
                    cands.add(stem + "l")
                elif consonant == "h" and stem.endswith("a"):
                    cands |= {stem, stem[:-1] + "e", stem[:-1] + "o"}
                elif consonant == "d" and stem.endswith("a"):
                    cands |= {stem[:-1] + "o", stem[:-1] + "e", stem}
                elif _takes(consonant, stem):
                    cands.add(stem)
                for cand in cands:
                    cand = SOMALI_VARIANTS.get(cand, cand)
                    # A verb's infinitive is its noun (ilaalinta "the protection", samaynta "the making").
                    if self._noun(cand):
                        pass
                    elif cand.endswith("n") and cand[:-1] in self.verbs:
                        direct.add(cand[:-1])
                    elif re.search("[ae]yn$", cand) and cand[:-3] + "ee" in self.verbs:
                        direct.add(cand[:-3] + "ee")
                    if self._noun(cand):
                        direct.add(cand)
                    else:
                        plural |= {w for w in self.singulars(cand) if self._noun(w)}
            if direct:
                break
        out = direct or plural or {w for w in self.singulars(token) if w != token and self._noun(w)}
        # A pronoun's subject and -oo forms: isagu, isagoo of isaga; iyadoo of iyada.
        for ending in ("u", "oo"):
            base = token[:-len(ending)] + "a"
            if token.endswith(ending) and "pron" in self.kinds.get(base, ()):
                out = out | {base}
        return frozenset(out)

    def _noun(self, word):
        return word in self.nouns and (len(word) > 2 or word in SHORT_NOUNS)

    @functools.lru_cache(maxsize=None)
    def lemmas(self, token, enclitic=True):
        """The words [token] is a form of: itself when it is a word, else the pack's words, those of the noun, verb and
        adjective rules, or of the word before an enclitic -na, -se or -ba (waxaana, isaguna, waxba)."""
        if token in SOMALI_FORMS:
            return frozenset(SOMALI_FORMS[token].split(","))
        if token in SOMALI_EXCLUDED:
            return frozenset()
        if token in self.kinds:
            return frozenset({token})
        if token in self.forms:
            return frozenset(self.forms[token])
        # A closing quote after a word is no glottal stop.
        if token.endswith("'"):
            return self.lemmas(token[:-1], enclitic)
        found = set(self.nouns_of(token)) | self.tails.get(token, set()) | self.adjective_forms.get(token, set())
        if found:
            return frozenset(found)
        if enclitic:
            for e in ENCLITICS:
                if token.endswith(e) and (len(token) - len(e) >= 2 or token[:-len(e)] in self.kinds):
                    inner = self.lemmas(token[:-len(e)], False)
                    if inner:
                        return inner
        return frozenset()


def _tokens(line):
    """A line's words, lowercase, without the capitalised ones inside a sentence (names); an all-capitals word is a word."""
    line = line.translate(APOSTROPHES)
    for match in WORD.finditer(line):
        word = match.group(0)
        if word[0].isupper() and not word.isupper() and not SENTENCE_START.search(line[:match.start()]):
            continue
        yield word.lower()


def _leipzig_sentences(path):
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            yield line.rstrip("\n").partition("\t")[2]


def _counts(download):
    """Each kind of text's word counts, and the names of the corpora counted."""
    counts = {kind: defaultdict(int) for kind in WEIGHTS}
    used = defaultdict(list)
    for kind, corpus in LEIPZIG:
        path = download(f"https://downloads.wortschatz-leipzig.de/corpora/{corpus}.tar.gz", f"leipzig-{corpus}.tar.gz")
        if path is None:
            continue
        used["leipzig"].append(corpus)
        for sentence in _leipzig_sentences(path):
            for word in _tokens(sentence):
                counts[kind][word] += 1
    for kind, corpus, version in OPUS:
        name = f"opus-{corpus.lower()}-{version}-so.txt.gz"
        path = download(f"https://object.pouta.csc.fi/OPUS-{corpus}/{version}/mono/so.txt.gz", name)
        if path is None:
            continue
        used[corpus].append(version)
        seen = set()
        with gzip.open(path, "rt", encoding="utf-8") as f:
            for line in f:
                line = line.strip().lstrip("-").strip()
                # The web crawl repeats its pages' menus and footers: each line is counted once.
                if kind == "web":
                    if line in seen:
                        continue
                    seen.add(line)
                for word in _tokens(line):
                    counts[kind][word] += 1
    return counts, used


def label_of(used):
    parts = []
    if used.get("leipzig"):
        parts.append(f"Leipzig Corpora Collection {', '.join(used['leipzig'])} ({LICENCES['leipzig']})")
    for corpus in ("CCAligned", "TED2020"):
        if used.get(corpus):
            parts.append(f"OPUS {corpus} {', '.join(used[corpus])} so ({LICENCES[corpus]})")
    return " + ".join(parts)


def build(size, download, dictionaries=DICTIONARIES):
    """The list: the lexicon's words by their forms' counts, each kind of text weighted by WEIGHTS."""
    lexicon = Lexicon(dictionaries)
    counts, used = _counts(download)
    label = label_of(used)
    share = defaultdict(float)
    raw = defaultdict(int)
    for kind, kind_counts in counts.items():
        total = sum(kind_counts.values())
        if not total:
            continue
        for form, n in kind_counts.items():
            share[form] += WEIGHTS[kind] * n / total
            raw[form] += n

    heads = {form: lexicon.lemmas(form) for form in share}
    own = defaultdict(float)
    for form, value in share.items():
        if len(heads[form]) == 1:
            own[next(iter(heads[form]))] += value
    scores = defaultdict(float)
    totals = defaultdict(int)
    forms = defaultdict(dict)
    for form, value in share.items():
        words = heads[form]
        if not words:
            continue
        weights = {w: own.get(w, 0.0) for w in words}
        whole = sum(weights.values())
        for word in words:
            part = weights[word] / whole if whole else 1 / len(words)
            if part <= 0:
                continue
            scores[word] += value * part
            totals[word] += raw[form]
            forms[word][form] = forms[word].get(form, 0.0) + value * part

    ranked = [w for w in sorted(scores, key=lambda w: (-scores[w], w)) if totals[w] >= LEAST_COUNT][:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: (-forms[word][f], f))[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label


_CHECKER = {}


def checker_lexicon():
    if "lexicon" not in _CHECKER:
        _CHECKER["lexicon"] = Lexicon()
    return _CHECKER["lexicon"]


def checker_word(token, ranks):
    """
    The list's word [token] is a form of by the rules the list was built with, the commonest when it may be several, or None:
    a noun with a determiner the list does not have (gurigayaga: guri), a verb form, a particle cluster.
    """
    found = [(ranks[w], w) for w in checker_lexicon().lemmas(token) if w in ranks]
    return min(found)[1] if found else None
