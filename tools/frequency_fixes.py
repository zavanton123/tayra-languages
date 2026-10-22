"""
Per-language corrections for tools/build_frequency_list.py, where the dictionary's forms table or
the lemmatizer gets a common word wrong. Each language's top 600 words were read and fixed by
hand (2026-10-06); the tail is left to the general rules.

OVERRIDES[language][form] = [word, ...] sends the form's count to those words (shared by how
common they are); [form] keeps the form as a word of its own. EXCLUDE[language] = {word, ...}
leaves words out entirely: names, foreign words, abbreviations. Everything is lowercase.
"""

# Portuguese, tuned first.
PORTUGUESE_OVERRIDES = {
    "pt": {
        "a": ["a"], "ela": ["ela"], "elas": ["ela"], "eles": ["eles"],
        "ao": ["a"], "aos": ["a"], "à": ["a"], "às": ["a"],
        "os": ["o"], "as": ["a"],
        "na": ["em"], "nas": ["em"], "nos": ["em", "nós"],
        "pela": ["por"], "pelas": ["por"], "pelos": ["por"],
        "numa": ["em"], "nuns": ["em"], "numas": ["em"],
        "dela": ["de"], "deles": ["de"], "delas": ["de"],
        "neste": ["em"], "nesta": ["em"], "nesse": ["em"], "nessa": ["em"], "nisso": ["em"], "nisto": ["em"],
        "disso": ["de"], "disto": ["de"], "daquele": ["de"], "daquela": ["de"], "naquele": ["em"], "naquela": ["em"],
        "daqui": ["de"], "dali": ["de"], "daí": ["de"],
        "deu": ["dar"],
        # Words the dictionary also lists as forms of another word, which a learner meets on their own.
        "isso": ["isso"], "isto": ["isto"], "aquilo": ["aquilo"], "fora": ["fora"], "mal": ["mal"],
        "melhor": ["melhor"], "pior": ["pior"], "maior": ["maior"], "menor": ["menor"], "ótimo": ["ótimo"], "péssimo": ["péssimo"],
        "tais": ["tal"],
    },
}

PORTUGUESE_EXCLUDE = {
    "pt": {"in", "brasil", "joão", "silva", "jesus", "lula", "paulo", "maria", "pedro", "josé", "francisco", "bahia", "paraná", "salvador"},
}

# ---- Reviewed group A ----
_OVERRIDES_A = {
    # German: personal pronouns are listed as forms of each other, and their case forms (mir, uns) were lost;
    # articles go to "der"; possessives to their own word; verb forms the dictionary sends to a noun of the same
    # spelling (fragen to Frage, glaube to Glaube) or to another verb (führen to fahren) go back to their verb;
    # wordfreq spells ß as ss, which the dictionary does not tie to the word (gross, strasse).
    "de": {
        "ich": ["ich"], "mir": ["ich"], "mich": ["ich"],
        "du": ["du"], "dir": ["du"], "dich": ["du"],
        "er": ["er"], "ihn": ["er"], "ihm": ["er"],
        "sie": ["sie"], "ihnen": ["sie"], "es": ["es"],
        "wir": ["wir"], "uns": ["wir"], "ihr": ["ihr"], "euch": ["ihr"],
        "der": ["der"], "die": ["der"], "das": ["der"], "den": ["der"], "dem": ["der"], "des": ["der"],
        "denen": ["der"], "deren": ["der"], "dessen": ["der"], "derer": ["der"],
        "ihre": ["ihr"], "ihren": ["ihr"], "ihrem": ["ihr"], "ihres": ["ihr"], "ihrer": ["ihr"],
        "sein": ["sein"], "seine": ["sein"], "seinen": ["sein"], "seinem": ["sein"], "seiner": ["sein"], "seines": ["sein"],
        "mein": ["mein"], "meine": ["mein"], "meinem": ["mein"], "meines": ["mein"], "meiner": ["mein"],
        "meinen": ["mein", "meinen"], "meint": ["meinen"], "meinte": ["meinen"], "meinst": ["meinen"],
        "meinten": ["meinen"], "gemeint": ["meinen"],
        "dein": ["dein"], "deine": ["dein"], "deinen": ["dein"], "deinem": ["dein"], "deiner": ["dein"], "deines": ["dein"],
        "unser": ["unser"], "unsere": ["unser"], "unseren": ["unser"], "unserem": ["unser"], "unserer": ["unser"], "unseres": ["unser"],
        "euer": ["euer"], "eure": ["euer"], "euren": ["euer"], "eurem": ["euer"], "eurer": ["euer"],
        "was": ["was"], "wat": ["was"], "wessen": ["wer"], "mehr": ["mehr"], "einst": ["einst"],
        "muss": ["müssen"], "soll": ["sollen"], "solls": ["sollen"],
        "gross": ["groß"], "grosse": ["groß"], "grossen": ["groß"], "grosser": ["groß"], "grosses": ["groß"], "grossem": ["groß"],
        "grösser": ["groß"], "grössere": ["groß"], "grösseren": ["groß"], "grösste": ["groß"], "grössten": ["groß"],
        "strasse": ["straße"], "strassen": ["straße"], "spass": ["spaß"], "scheisse": ["scheiße"],
        "anderen": ["anderer"], "andere": ["anderer"], "anderes": ["anderer"], "anderem": ["anderer"], "anderer": ["anderer"],
        "andern": ["anderer"], "andre": ["anderer"],
        "einige": ["einige"], "einigen": ["einige"], "einiges": ["einige"], "einiger": ["einige"],
        "stunde": ["stunde"], "stunden": ["stunde"],
        "denke": ["denken"], "denken": ["denken"], "glaube": ["glauben"], "glauben": ["glauben"],
        "fragen": ["frage", "fragen"], "spielen": ["spielen"], "arbeiten": ["arbeiten"],
        "fallen": ["fallen"], "falls": ["falls"], "gefällt": ["gefallen"], "halten": ["halten"],
        "folgen": ["folgen"], "teilen": ["teilen"], "teils": ["teils"], "landen": ["landen"], "bitten": ["bitten"],
        "jung": ["jung"], "junge": ["junge", "jung"], "jungen": ["jung", "junge"], "junger": ["jung"],
        "jünger": ["jung"], "jüngere": ["jung"], "jüngeren": ["jung"], "jüngsten": ["jung"],
        "enden": ["enden"], "na": ["na"], "lieben": ["lieben"], "lieb": ["lieb"], "lieber": ["gern", "lieb"],
        "gründen": ["gründen"], "bar": ["bar"], "nahmen": ["nehmen"],
        "alte": ["alt"], "alten": ["alt"], "alter": ["alter", "alt"],
        "kleine": ["klein"], "kleinen": ["klein"], "kleiner": ["klein"], "kleinem": ["klein"],
        "stimmt": ["stimmen"], "stimmen": ["stimmen"], "stimmte": ["stimmen"], "beginnen": ["beginnen"],
        "reicht": ["reichen"], "reichen": ["reichen"], "zahl": ["zahl"], "handel": ["handel"], "lauten": ["lauten"],
        "schach": ["schach"], "drücken": ["drücken"], "planet": ["planet"], "planeten": ["planet"], "planen": ["planen"],
        "kämpfen": ["kämpfen"], "berichten": ["berichten"], "rufen": ["rufen"], "besitzen": ["besitzen"],
        "schützen": ["schützen"], "dauern": ["dauern"], "lehren": ["lehren"], "leere": ["leer"],
        "reisen": ["reisen"], "riesen": ["riese"], "reis": ["reis"], "vergleichen": ["vergleichen"],
        "erfolgen": ["erfolgen"], "kriegen": ["kriegen"], "bauen": ["bauen"], "besuchen": ["besuchen"],
        "weisen": ["weisen"], "wiese": ["wiese"], "achten": ["achten"],
        "grün": ["grün"], "grüne": ["grün"], "grünen": ["grün"], "grüner": ["grün"], "grünes": ["grün"],
        "link": ["link"], "linke": ["link"], "linken": ["link"], "linker": ["link"], "links": ["links"],
        "freunde": ["freund"], "daten": ["datum"],
        "führen": ["führen"], "führt": ["führen"], "führte": ["führen"], "geführt": ["führen"],
        "stellen": ["stellen"], "stellt": ["stellen"], "stellte": ["stellen"], "gestellt": ["stellen"], "stellung": ["stellung"],
        "legen": ["legen"], "legt": ["legen"], "legte": ["legen"], "gelegt": ["legen"],
        "malen": ["malen"], "gemalt": ["malen"], "malt": ["malen"], "malte": ["malen"],
        "wegen": ["wegen"], "rechts": ["rechts"], "danke": ["danke"], "versuch": ["versuch"],
        "groß": ["groß"], "spaß": ["spaß"], "scheiße": ["scheiße"], "riese": ["riese"], "fällt": ["fallen"],
        "meister": ["meister"], "unsre": ["unser"], "eins": ["eins"], "halte": ["halten"], "bilder": ["bild"],
        "leiden": ["leiden"], "grenze": ["grenze"], "grenzen": ["grenze", "grenzen"], "geboten": ["bieten"], "gebot": ["gebot"],
        "passiert": ["passieren"], "passierte": ["passieren"], "erklärt": ["erklären"], "erklärte": ["erklären"],
        "verloren": ["verlieren"],
        "dies": ["dieser"], "heißen": ["heißen"], "weiß": ["weiß"], "weiss": ["wissen"], "fuß": ["fuß"], "heisst": ["heißen"], "heissen": ["heißen"], "hiess": ["heißen"], "liess": ["lassen"],
        "weisst": ["wissen"], "weissen": ["weiß"], "weisse": ["weiß"], "massnahme": ["maßnahme"], "massnahmen": ["maßnahme"],
        "fuss": ["fuß"], "füsse": ["fuß"], "nen": ["ein"], "gibt's": ["geben"], "gibts": ["geben"], "geht's": ["gehen"], "wär": ["sein"],
        # Pronominal adverbs are words of their own, not forms of their preposition.
        "damit": ["damit"], "dabei": ["dabei"], "dazu": ["dazu"], "davon": ["davon"], "dafür": ["dafür"], "darauf": ["darauf"],
        "drauf": ["darauf"], "daran": ["daran"], "dran": ["daran"], "darum": ["darum"], "drum": ["darum"], "dadurch": ["dadurch"],
        "dagegen": ["dagegen"], "danach": ["danach"], "daraus": ["daraus"], "draus": ["daraus"], "darüber": ["darüber"],
        "drüber": ["darüber"], "darunter": ["darunter"], "drunter": ["darunter"], "darin": ["darin"], "dazwischen": ["dazwischen"],
        "daneben": ["daneben"], "dahinter": ["dahinter"], "davor": ["davor"], "wobei": ["wobei"], "wozu": ["wozu"], "womit": ["womit"],
        "wofür": ["wofür"], "worauf": ["worauf"], "woran": ["woran"], "worum": ["worum"], "wovon": ["wovon"], "woraus": ["woraus"],
        "worüber": ["worüber"],
    },
    # Dutch: "een", "er", "dat", "daar" were merged into "de"/"het"/"die"; the pronoun tables cross-link every pronoun,
    # so "ze", "we", "onze" were lost; verb forms that are also nouns (heeft, kan, zit, weet, wil, staat) and other
    # look-alikes (vier into vuur, moeder into vader, kennen into kunnen) go back to their own word.
    "nl": {
        "de": ["de"], "den": ["de"], "der": ["de"], "een": ["een"], "één": ["een"], "ene": ["een"], "eens": ["eens"], "is": ["zijn"], "er": ["er"],
        "dat": ["dat"], "da's": ["dat"], "daar": ["daar"], "ze": ["ze"], "we": ["we"], "onze": ["ons"],
        "m'n": ["mijn"], "z'n": ["zijn"],
        "heeft": ["hebben"], "kan": ["kunnen"], "kennen": ["kennen"], "kent": ["kennen"], "ken": ["kennen"], "kende": ["kennen"],
        "zit": ["zitten"], "zitten": ["zitten"], "weet": ["weten"], "weten": ["weten"], "wil": ["willen"],
        "kijk": ["kijken"], "kijken": ["kijken"], "staat": ["staan"], "zoek": ["zoeken"], "hoef": ["hoeven"], "hoeft": ["hoeven"],
        "hoeven": ["hoeven"], "doe": ["doen"], "doet": ["doen"], "reed": ["rijden"],
        "gebruiken": ["gebruiken"], "gebruikt": ["gebruiken"], "gebruikte": ["gebruiken"], "gebruikten": ["gebruiken"],
        "geloven": ["geloven"], "gelooft": ["geloven"], "geloofde": ["geloven"], "hopen": ["hopen"], "hoopt": ["hopen"],
        "hoopte": ["hopen"], "passen": ["passen"], "past": ["passen"], "plaatsen": ["plaatsen"], "geplaatst": ["plaatsen"],
        "delen": ["delen"], "werken": ["werken"], "werkt": ["werken"], "werkte": ["werken"], "gewerkt": ["werken"],
        "drukken": ["drukken"],
        "waar": ["waar"], "ware": ["waar"], "waren": ["zijn"], "waarde": ["waarde"], "waarden": ["waarde"],
        "vroeg": ["vragen", "vroeg"], "vroeger": ["vroeger"], "vroege": ["vroeg"], "vroegere": ["vroeg"],
        "laat": ["laten", "laat"], "late": ["laat"], "later": ["later"], "laatste": ["laatst"], "laatst": ["laatst"],
        "echter": ["echter"], "ergens": ["ergens"], "nergens": ["nergens"], "overal": ["overal"],
        "alle": ["alle"], "allen": ["alle"], "gegevens": ["gegevens"],
        "leggen": ["leggen"], "legt": ["leggen"], "leg": ["leggen"], "legde": ["leggen"], "gelegd": ["leggen"], "lage": ["laag"],
        "langs": ["langs"], "ouders": ["ouders"], "moeder": ["moeder"], "moeders": ["moeder"],
        "jong": ["jong"], "jonge": ["jong"], "jonger": ["jong"], "jongere": ["jong"], "jongste": ["jong"], "jongeren": ["jong"],
        "rechts": ["rechts"], "gericht": ["richten"], "gelden": ["gelden"], "geldt": ["gelden"],
        "vier": ["vier"], "vieren": ["vieren"], "vierde": ["vierde"], "dier": ["dier"], "dieren": ["dier"],
        "dingen": ["ding"], "ding": ["ding"], "rij": ["rij"], "rijen": ["rij"], "haan": ["haan"], "hanen": ["haan"],
        "ogen": ["oog"], "oog": ["oog"], "zaken": ["zaak"], "regel": ["regel"], "regels": ["regel"], "hert": ["hert"],
        "zwarte": ["zwart"], "genoemd": ["noemen"], "genoemde": ["noemen"], "geniet": ["genieten"], "genoten": ["genieten"],
        # Words of their own that the dictionary lists under a shorter word.
        "na": ["na"], "naast": ["naast"], "nader": ["nader"], "mee": ["mee"], "toe": ["toe"],
        "waarom": ["waarom"], "daarom": ["daarom"], "daarna": ["daarna"], "daarnaast": ["daarnaast"], "waarna": ["waarna"],
        "daarmee": ["daarmee"], "waarmee": ["waarmee"], "ermee": ["ermee"], "daarbij": ["daarbij"], "waarbij": ["waarbij"],
        "erbij": ["erbij"], "daardoor": ["daardoor"], "waardoor": ["waardoor"], "daarvoor": ["daarvoor"], "waarvoor": ["waarvoor"],
        "ervoor": ["ervoor"], "daarin": ["daarin"], "waarin": ["waarin"], "erin": ["erin"], "daarop": ["daarop"],
        "waarop": ["waarop"], "erop": ["erop"], "daarvan": ["daarvan"], "waarvan": ["waarvan"], "ervan": ["ervan"],
        "daaraan": ["daaraan"], "eraan": ["eraan"], "waarover": ["waarover"], "daarover": ["daarover"], "erover": ["erover"],
        "eruit": ["eruit"], "ertoe": ["ertoe"], "daartoe": ["daartoe"], "waaronder": ["waaronder"], "daaronder": ["daaronder"],
    },
    # Danish: "i" (in) and "er" (is) were lost; den/de/dem and the possessives min/din/hans/hendes/vores/deres were merged
    # into det/jeg/du/han/hun/vi; present tenses in -er that are also agent nouns (spiller, kender, elsker) go back to
    # their verb, as do skal, var, går; look-alikes (tal into tale, form into flere, lægge into læge, ende into fly).
    "da": {
        "i": ["i"], "er": ["være"], "var": ["være"], "skal": ["skulle"], "går": ["gå"],
        "det": ["det"], "dét": ["det"], "den": ["den"], "de": ["de"], "dem": ["de"], "deres": ["deres"],
        "min": ["min"], "mit": ["min"], "mine": ["min"], "din": ["din"], "dit": ["din"], "dine": ["din"],
        "hans": ["hans"], "hendes": ["hendes"], "vores": ["vores"], "vor": ["vor"], "vore": ["vor"], "vort": ["vor"],
        "jer": ["jer"], "hvis": ["hvis"], "mange": ["mange"], "næsten": ["næsten"],
        "kunde": ["kunde"], "kunder": ["kunde"], "kunden": ["kunde"], "kunderne": ["kunde"],
        "holde": ["holde"], "holder": ["holde"], "holdt": ["holde"], "holdes": ["holde"],
        "lad": ["lade"], "lade": ["lade"], "lader": ["lade"], "lod": ["lade"], "ladet": ["lade"],
        "lav": ["lav", "lave"], "lavere": ["lav"], "lavt": ["lav"], "laveste": ["lav"],
        "tal": ["tal"], "tallet": ["tal"], "tallene": ["tal"],
        "føre": ["føre"], "førte": ["føre"], "ført": ["føre"], "førende": ["føre"],
        "form": ["form"], "formen": ["form"], "former": ["form"],
        "lægge": ["lægge"], "lægger": ["lægge"], "læg": ["lægge"], "lagt": ["lægge"], "lagde": ["lægge"],
        "ende": ["ende"], "ender": ["ende"], "endte": ["ende"], "enden": ["ende"], "endt": ["ende"],
        "båd": ["båd"], "båden": ["båd"], "bådene": ["båd"], "vind": ["vind"], "vinden": ["vind"],
        "hændte": ["hænde"], "hændt": ["hænde"], "hænde": ["hænde"], "fyrre": ["fyrre"], "dårligt": ["dårlig"],
        "giver": ["give"], "gi'r": ["give"], "elsker": ["elske"], "kender": ["kende"], "spiller": ["spille"],
        "tænker": ["tænke"], "kører": ["køre"], "viser": ["vise"], "handler": ["handle"], "passer": ["passe"],
        "føler": ["føle"], "begynder": ["begynde"], "vinder": ["vinde"], "hader": ["hade"], "sender": ["sende"],
        "bor": ["bo"], "timer": ["time"], "øjne": ["øje"], "mangler": ["mangle"], "livet": ["liv"],
        "ryg": ["ryg"], "ryggen": ["ryg"], "varm": ["varm"], "varmt": ["varm"], "vågner": ["vågne"], "vågne": ["vågne"], "vågnede": ["vågne"],
        "drukne": ["drukne"], "drukner": ["drukne"], "druknede": ["drukne"], "druknet": ["drukne"],
        "slette": ["slette"], "slettet": ["slette"], "ledere": ["leder"], "ledet": ["lede"],
        "forene": ["forene"], "forenede": ["forene"], "forenet": ["forene"],
        "fokus": ["fokus"], "program": ["program"], "model": ["model"], "situation": ["situation"], "version": ["version"],
    },
    # Norwegian: "i" (in) and "tv" were lost; si (say) and sette (set) were merged into sin and se, når into nå;
    # kan/skal/går/var and present tenses in -er that are also agent nouns (spiller, elsker, lever) go back to their
    # verb; object pronouns go under the subject (as Danish has them); hans/hennes/deres are words of their own.
    "no": {
        "i": ["i"], "tv": ["tv"], "når": ["når"],
        "kan": ["kunne"], "skal": ["skulle"], "går": ["gå"], "var": ["være"], "skala": ["skala"],
        "meg": ["jeg"], "deg": ["du"], "ham": ["han"], "henne": ["hun"], "oss": ["vi"], "dem": ["de"],
        "hans": ["hans"], "hennes": ["hennes"], "deres": ["deres"], "hammer": ["hammer"],
        "si": ["si"], "sier": ["si"], "sa": ["si"], "sagt": ["si"], "sies": ["si"],
        "sette": ["sette"], "setter": ["sette"], "satte": ["sette"], "satt": ["sitte", "sette"],
        "spiller": ["spille"], "elsker": ["elske"], "begynner": ["begynne"], "skriver": ["skrive"], "søker": ["søke"],
        "viser": ["vise"], "bor": ["bo"], "lever": ["leve"],
        "levere": ["levere"], "leverer": ["levere"], "levert": ["levere"], "leverte": ["levere"],
        "undre": ["undre"], "undres": ["undre"], "undret": ["undre"], "takk": ["takk"], "helst": ["gjerne"],
        "forene": ["forene"], "forente": ["forene"], "forent": ["forene"],
        "brød": ["brød"], "brødet": ["brød"], "legende": ["legende"], "legenden": ["legende"], "legender": ["legende"],
        "fart": ["fart"], "farten": ["fart"],
    },
    # Swedish: "i" (in) and sitt/dina/vårt were lost to the cross-linked pronoun tables; och went into att, ja into jag,
    # tv into du, ren into den, katt into fan, ben into be, modern into mor; får/går/vill/kör/bor/lever go to their verb;
    # possessive forms go to min/din/sin/vår and object pronouns under the subject (as Danish has them).
    "sv": {
        "i": ["i"], "och": ["och"], "ja": ["ja"], "tv": ["tv"], "tvn": ["tv"],
        "mig": ["jag"], "dig": ["du"], "honom": ["han"], "henne": ["hon"], "oss": ["vi"], "dem": ["de"], "er": ["ni"],
        "mina": ["min"], "mitt": ["min"], "dina": ["din"], "ditt": ["din"], "sina": ["sin"], "sitt": ["sin"],
        "våra": ["vår"], "vårt": ["vår"],
        "får": ["få"], "går": ["gå"], "vill": ["vilja"], "kör": ["köra"], "körs": ["köra"], "bor": ["bo"], "lever": ["leva"],
        "hittar": ["hitta"], "borde": ["böra"], "satte": ["sätta"], "finns": ["finnas"], "fanns": ["finnas"],
        "annat": ["annan"], "andra": ["annan"], "annan": ["annan"], "andras": ["annan"],
        "nåt": ["någon"], "nån": ["någon"], "några": ["någon"], "sån": ["sådan"], "sånt": ["sådan"], "såna": ["sådan"],
        "ren": ["ren"], "rena": ["ren"], "renare": ["ren"],
        "skola": ["skola"], "skolan": ["skola"], "skolor": ["skola"], "skolorna": ["skola"], "skolans": ["skola"],
        "sen": ["sen"], "senare": ["sen"], "senaste": ["sen"], "sent": ["sen"], "vidare": ["vidare"],
        "katt": ["katt"], "katten": ["katt"], "katter": ["katt"], "katterna": ["katt"], "bubbla": ["bubbla"],
        "stället": ["ställe"], "ställe": ["ställe"], "ställen": ["ställe"],
        "ända": ["ända"], "ände": ["ände"], "änden": ["ände"],
        "ben": ["ben"], "benen": ["ben"], "benet": ["ben"], "rest": ["rest", "resa"], "resten": ["rest"], "rester": ["rest"],
        "lyckas": ["lyckas"], "lyckades": ["lyckas"], "lyckats": ["lyckas"],
        "arbeten": ["arbete"],
        "modern": ["modern"], "moderna": ["modern"], "modernt": ["modern"], "modernare": ["modern"], "modernaste": ["modern"],
        "låten": ["låt"], "skiten": ["skit"],
    },
    # Icelandic: the cross-linked pronoun tables lost mér, honum, hans, okkur, þess, þeir...; they go under the
    # nominative; er/ert/erum/var/voru were words of their own instead of forms of vera; tíma was split from tími,
    # verður from verða; sá (that) was merged into sjá, enn into en, reyndar into reyna.
    "is": {
        "er": ["vera"], "ert": ["vera"], "erum": ["vera"], "ertu": ["vera"], "var": ["vera"], "voru": ["vera"],
        "verið": ["vera"], "væru": ["vera"], "værir": ["vera"], "værum": ["vera"],
        "mig": ["ég"], "mér": ["ég"], "þig": ["þú"], "þér": ["þú"], "honum": ["hann"], "hans": ["hann"],
        "hana": ["hún"], "henni": ["hún"], "hennar": ["hún"], "þess": ["það"], "okkur": ["við"], "okkar": ["við"],
        "ykkur": ["þið"], "ykkar": ["þið"], "þeir": ["þeir"], "mín": ["minn"], "þín": ["þinn"],
        "vor": ["vor"], "vorið": ["vor"],
        "verður": ["verða"], "verða": ["verða"], "átti": ["eiga"], "áttu": ["eiga"], "átta": ["átta"], "má": ["mega"],
        "gengur": ["ganga"], "gengu": ["ganga"],
        "sá": ["sá", "sjá"], "sú": ["sá"], "þann": ["sá"], "þeirri": ["sá"],
        "enn": ["enn"], "tíma": ["tími"], "veistu": ["vita"], "vissi": ["vita"], "vissu": ["vita"],
        "ber": ["bera"], "bera": ["bera"], "berja": ["berja"],
        "loksins": ["loksins"], "loks": ["loks"],
        "veldur": ["valda"], "olli": ["valda"], "valda": ["valda"], "hitt": ["hinn"],
        "hét": ["heita"], "heita": ["heita"], "heitir": ["heita"],
        "ný": ["nýr"], "nýja": ["nýr"], "nýjum": ["nýr"], "nýi": ["nýr"],
        "sæt": ["sætur"], "sætur": ["sætur"], "sætu": ["sætur"], "reyndar": ["reyndar"],
        "byggir": ["byggja"], "byggist": ["byggja"], "eignaðist": ["eignast"], "eignast": ["eignast"],
        "virðist": ["virðast"], "gerist": ["gerast"], "gerðist": ["gerast"], "telst": ["teljast"], "komnir": ["koma"],
        "fyrri": ["fyrri"], "fyrra": ["fyrri"],
    },
    # English: the forms table ties words to acronyms they spell or appear in (fast to time, price to rest, form to
    # family, dream to development, offer to off, member to remember), merges they/them/their into it and year/yeah
    # into here; would/could/might/able were forms of will/can/may; is/was stood apart from be. Each such word gets
    # its own forms back; object pronouns go under the subject, possessives and contractions with "n't" stay apart.
    "en": {
        "a": ["a"], "an": ["a"],
        "they": ["they"], "them": ["they"], "you're": ["you"], "themselves": ["themselves"], "they're": ["they"], "they've": ["they"], "they'll": ["they"], "they'd": ["they"],
        "their": ["their"], "theirs": ["their"],
        "its": ["its"],
        "it": ["it"], "it's": ["it"],
        "his": ["his"],
        "he": ["he"], "him": ["he"], "he's": ["he"],
        "i": ["i"], "me": ["i"], "i'm": ["i"], "i've": ["i"], "i'll": ["i"], "i'd": ["i"],
        "my": ["my"],
        "your": ["your"], "yours": ["your"],
        "we": ["we"], "us": ["we"],
        "our": ["our"], "ours": ["our"],
        "there": ["there"], "there's": ["there"],
        "then": ["then"],
        "though": ["though"], "tho": ["though"],
        "thoughts": ["thought"],
        "year": ["year"], "years": ["year"], "year's": ["year"],
        "yeah": ["yeah"], "yea": ["yeah"],
        "more": ["more"],
        "most": ["most"],
        "less": ["less"],
        "least": ["least"],
        "be": ["be"], "is": ["be"], "are": ["be"], "was": ["be"], "were": ["be"], "been": ["be"], "am": ["be"], "being": ["be"],
        "isn't": ["isn't"],
        "wasn't": ["wasn't"],
        "weren't": ["weren't"],
        "would": ["would"], "would've": ["would"],
        "could": ["could"], "could've": ["could"],
        "might": ["might"],
        "able": ["able"],
        "air": ["air"], "aired": ["air"],
        "art": ["art"], "arts": ["art"],
        "hey": ["hey"], "does": ["do"], "smart": ["smart"],
        "ass": ["ass"], "asses": ["ass"],
        "news": ["news"],
        "say": ["say"], "said": ["say"],
        "thing": ["thing"], "things": ["thing"],
        "hits": ["hits"],
        "fast": ["fast"], "fastest": ["fast"], "faster": ["fast"],
        "free": ["free"], "freed": ["free"], "frees": ["free"], "freeing": ["free"],
        "tree": ["tree"], "trees": ["tree"],
        "well": ["well"],
        "good": ["good"],
        "offer": ["offer"], "offered": ["offer"], "offers": ["offer"], "offering": ["offer"], "offerings": ["offer"],
        "evening": ["evening"], "evenings": ["evening"],
        "mom": ["mom"], "moms": ["mom"],
        "mark": ["mark"], "marked": ["mark"], "marks": ["mark"], "marking": ["mark"],
        "form": ["form"], "forms": ["form"], "formed": ["form"], "forming": ["form"],
        "round": ["round"], "rounds": ["round"], "rounded": ["round"],
        "member": ["member"], "members": ["member"],
        "act": ["act"], "acting": ["act"], "acts": ["act"], "acted": ["act"],
        "tank": ["tank"], "tanks": ["tank"],
        "freak": ["freak"], "freaking": ["freak"],
        "fork": ["fork"],
        "love": ["love"], "loving": ["love"],
        "further": ["further"],
        "furthermore": ["furthermore"],
        "base": ["base"], "based": ["base"], "bases": ["base"], "basing": ["base"],
        "price": ["price"], "prices": ["price"], "pricing": ["price"], "priced": ["price"],
        "prize": ["prize"], "prizes": ["prize"],
        "shore": ["shore"], "shores": ["shore"],
        "arrow": ["arrow"], "arrows": ["arrow"],
        "lose": ["lose"], "lost": ["lose"], "loses": ["lose"],
        "loss": ["loss"], "losses": ["loss"],
        "fit": ["fit"], "fits": ["fit"], "fitted": ["fit"], "fitting": ["fit"],
        "wash": ["wash"], "washing": ["wash"], "washed": ["wash"], "washes": ["wash"],
        "brake": ["brake"], "brakes": ["brake"],
        "broker": ["broker"],
        "circle": ["circle"], "circles": ["circle"], "circling": ["circle"], "circled": ["circle"],
        "grill": ["grill"], "grilled": ["grill"], "grilling": ["grill"],
        "choose": ["choose"], "chose": ["choose"],
        "interesting": ["interesting"],
        "interested": ["interested"],
        "ship": ["ship"], "ships": ["ship"], "shipping": ["ship"], "shipped": ["ship"],
        "hole": ["hole"], "holes": ["hole"],
        "sheet": ["sheet"], "sheets": ["sheet"],
        "cent": ["cent"], "cents": ["cent"],
        "dream": ["dream"], "dreams": ["dream"], "dreaming": ["dream"], "dreamed": ["dream"], "dreamt": ["dream"],
        "skull": ["skull"], "skulls": ["skull"],
        "ham": ["ham"],
        "liver": ["liver"],
        "seed": ["seed"], "seeds": ["seed"],
        "sum": ["sum"], "sums": ["sum"],
        "toll": ["toll"],
        "thou": ["thou"],
        "thy": ["thy"],
        "hat": ["hat"], "hats": ["hat"],
        "bite": ["bite"], "bites": ["bite"], "biting": ["bite"],
        "bag": ["bag"], "bags": ["bag"],
        "punch": ["punch"], "punched": ["punch"],
        "kiss": ["kiss"], "kissing": ["kiss"], "kissed": ["kiss"], "kisses": ["kiss"],
        "steam": ["steam"],
        "stem": ["stem"], "stems": ["stem"],
        "sauce": ["sauce"],
        "loose": ["loose"],
        "net": ["net"], "nets": ["net"],
        "neat": ["neat"],
        "pace": ["pace"],
        "shut": ["shut"], "shutting": ["shut"],
        "bye": ["bye"],
        "ray": ["ray"], "rays": ["ray"],
        "okay": ["okay"], "ok": ["okay"],
        "testament": ["testament"], "testaments": ["testament"],
        "building": ["building", "build"], "better": ["good"], "best": ["good"], "thought": ["think", "thought"],
    },
}

_EXCLUDE_A = {
    # German: English words, abbreviations, names and stray letters.
    "de": {"wa", "new", "is", "de", "on", "usa", "bayern", "bzw", "omg", "pages", "hero", "title", "data", "date",
           "dates", "st", "en", "et", "est", "aw", "nh"},
    # Dutch: names and brands.
    "nl": {"peter", "facebook", "twitter"},
    # Danish: English words and a stray letter.
    "da": {"the", "self", "a", "sir"},
    # Norwegian: English words and abbreviations (Ap, AS, ASA).
    "no": {"in", "and", "ap", "as", "asa"},
    # Swedish: English words, stray letters and a name.
    "sv": {"the", "a", "m", "and", "hate", "hates", "fabian", "hälsingland"},
    # English: initialisms, names and stray letters the forms table ties to common words.
    "en": {"e", "mr", "mars", "marc", "lgbtq", "lgbtqi", "bruce", "elmo", "ioe", "iot", "poi", "pmi", "aida", "pois",
           "mattie", "leeds", "brooke", "moore", "ted", "mg", "cia", "es", "zee"},
}

# ---- Reviewed group B ----
_OVERRIDES_B = {
    # French: wordfreq splits elisions (l' d' j' qu' c' n' s' m' t') into bare letters, which were dropped;
    # de/le/du/des/au had been swallowed by "un"/"il" through pronoun and article tables, and the
    # possessives and personal pronouns by "je"/"vous"/"il"/"elle"; "est" went to the noun "east".
    "fr": {
        "de": ["de"], "d": ["de"], "du": ["de"], "des": ["de", "un"],
        "au": ["à"], "aux": ["à"],
        "le": ["le"], "la": ["le"], "les": ["le"], "l": ["le"],
        "c": ["ce"], "cet": ["ce"], "j": ["je"], "n": ["ne"], "s": ["se"], "m": ["me"], "t": ["te"], "qu": ["que"],
        "je": ["je"], "vous": ["vous"], "eux": ["eux"],
        "y": ["y"], "nous": ["nous"], "moi": ["moi"], "tu": ["tu"], "toi": ["toi"], "lui": ["lui"],
        "leur": ["leur"], "leurs": ["leur"],
        "mon": ["mon"], "ma": ["mon"], "mes": ["mon"], "ton": ["ton"], "ta": ["ton"], "tes": ["ton"],
        "son": ["son"], "sa": ["son"], "ses": ["son"], "notre": ["notre"], "nos": ["notre"], "votre": ["votre"], "vos": ["votre"],
        "est": ["être"], "suis": ["être"], "ayant": ["avoir"], "pu": ["pouvoir"], "né": ["naître"],
        "fait": ["faire"], "faites": ["faire"], "dit": ["dire"], "dites": ["dire"], "dû": ["devoir"],
        "perdu": ["perdre"], "perdue": ["perdre"], "perdus": ["perdre"], "perdues": ["perdre"],
        "connu": ["connaître"], "connue": ["connaître"], "connues": ["connaître"],
        "ouvert": ["ouvrir"], "ouverte": ["ouvrir"], "ouverts": ["ouvrir"], "ouvertes": ["ouvrir"],
        "passe": ["passer"], "passes": ["passer"], "laisse": ["laisser"], "laisses": ["laisser"],
        "présente": ["présenter", "présent"], "présentes": ["présenter", "présent"],
        "moins": ["moins"], "mieux": ["mieux"], "meilleur": ["meilleur"], "meilleure": ["meilleur"],
        "meilleurs": ["meilleur"], "meilleures": ["meilleur"], "pire": ["pire"], "pires": ["pire"], "moindre": ["moindre"],
        "belle": ["beau"], "belles": ["beau"], "nationale": ["national"], "nationales": ["national"],
        "affaires": ["affaire"], "parties": ["partie"],
        "reine": ["reine"], "reines": ["reine"], "déesse": ["déesse"], "déesses": ["déesse"],
        "verset": ["verset"], "versets": ["verset"],
        "ca": ["ça"], "fous": ["foutre"], "tue": ["tuer"],
    },
    # Spanish: "el" was folded into "la" and "lo"/"los"/"las" with it; "muy" into "mucho", "hay" into "ahí",
    # "padre" into "madre", creer's forms into "crear"; clitics (me, nos), él/ellos, eso/aquello/ese,
    # mejor/peor/mayor/menor and tanto had been absorbed by other words; del/al are contractions of de/a.
    "es": {
        "el": ["el"], "la": ["el"], "los": ["el"], "las": ["el"], "lo": ["lo"],
        "del": ["de"], "al": ["a"], "e": ["e"], "u": ["u"],
        "me": ["me"], "nos": ["nos"], "él": ["él"], "ellos": ["él"], "ella": ["ella"], "ellas": ["ella"], "ello": ["ello"],
        "tu": ["tu"], "tus": ["tu"],
        "vuestro": ["vuestro"], "vuestra": ["vuestro"], "vuestros": ["vuestro"], "vuestras": ["vuestro"],
        "eso": ["eso"], "aquello": ["aquello"], "ese": ["ese"], "esa": ["ese"], "esos": ["ese"], "esas": ["ese"],
        "ése": ["ese"], "ésa": ["ese"], "ésos": ["ese"], "ésas": ["ese"],
        "aquel": ["aquel"], "aquella": ["aquel"], "aquellos": ["aquel"], "aquellas": ["aquel"], "aquél": ["aquel"], "aquélla": ["aquel"], "aquéllos": ["aquel"],
        "este": ["este"], "esta": ["este"], "estos": ["este"], "estas": ["este"], "éste": ["este"], "ésta": ["este"], "éstos": ["este"], "éstas": ["este"],
        "muy": ["muy"], "tanto": ["tanto"], "tanta": ["tanto"], "tantos": ["tanto"], "tantas": ["tanto"],
        "mejor": ["mejor"], "mejores": ["mejor"], "peor": ["peor"], "peores": ["peor"],
        "mayor": ["mayor"], "mayores": ["mayor"], "menor": ["menor"], "menores": ["menor"],
        "malo": ["malo"], "mala": ["malo"], "malos": ["malo"], "malas": ["malo"],
        "primer": ["primero"], "primera": ["primero"], "primeras": ["primero"],
        "hay": ["haber"], "había": ["haber"], "habia": ["haber"], "he": ["haber"],
        "creo": ["creer"], "cree": ["creer"], "creen": ["creer"], "crees": ["creer"], "creemos": ["creer"],
        "hecha": ["hacer"], "hechas": ["hacer"], "duele": ["doler"], "duelen": ["doler"], "duelan": ["doler"],
        "pasa": ["pasar"], "pasas": ["pasar"], "queda": ["quedar"], "quedas": ["quedar"], "trata": ["tratar"], "tratas": ["tratar"],
        "mira": ["mirar"], "miras": ["mirar"], "llama": ["llamar"], "llamas": ["llamar"],
        "padre": ["padre"], "padres": ["padre"], "madres": ["madre"], "papá": ["papá"], "papás": ["papá"], "pa": ["para"],
        "reina": ["reina"], "reinas": ["reina"], "salida": ["salida"], "vuelta": ["vuelta"], "vueltas": ["vuelta"],
        "entrada": ["entrada"], "entradas": ["entrada"], "peso": ["peso"], "pesos": ["peso"], "bonos": ["bono"],
        "comidas": ["comida"], "partidos": ["partido"], "partes": ["parte"], "resultados": ["resultado"],
        "formas": ["forma"], "funciones": ["función"], "logros": ["logro"], "correos": ["correo"], "pagos": ["pago"],
        "ola": ["ola"], "olas": ["ola"], "logo": ["logo"],
        "chileno": ["chileno"], "chilena": ["chileno"], "chilenos": ["chileno"], "chilenas": ["chileno"],
        "chino": ["chino"], "china": ["chino"], "chinos": ["chino"], "chinas": ["chino"],
    },
    # Italian: the articles were spread over "la"/"il"/"lo" and "i" and the elided l' c' d' po' dell' all'
    # were dropped; articulated prepositions went to rare homographs (nei to "neo", agli to "aglio", ai to
    # "aio", pelle to "per"); "più" sat in "molto", "sono" in "suono", "so" in "suo", "se"/"ti"/"ne" in "si"/"in".
    "it": {
        "il": ["il"], "lo": ["il"], "la": ["il"], "i": ["il"], "gli": ["il"], "le": ["il"], "l": ["il"], "glie": ["il"],
        "li": ["li"], "ne": ["ne"], "se": ["se"], "ti": ["ti"], "te": ["te"], "mi": ["mi"], "me": ["me"], "cui": ["cui"],
        "loro": ["loro"], "lor": ["loro"], "lui": ["lui"], "lei": ["lei"],
        "mio": ["mio"], "mia": ["mio"], "miei": ["mio"], "mie": ["mio"],
        "c": ["ci"], "d": ["di"], "po": ["poco"], "pò": ["poco"], "quest": ["questo"],
        "del": ["di"], "dello": ["di"], "della": ["di"], "dei": ["di"], "degli": ["di"], "delle": ["di"], "dell": ["di"],
        "al": ["a"], "allo": ["a"], "alla": ["a"], "ai": ["a"], "agli": ["a"], "alle": ["a"], "all": ["a"],
        "dal": ["da"], "dallo": ["da"], "dalla": ["da"], "dai": ["da"], "dagli": ["da"], "dalle": ["da"], "dall": ["da"],
        "nel": ["in"], "nello": ["in"], "nella": ["in"], "nei": ["in"], "negli": ["in"], "nelle": ["in"], "nell": ["in"],
        "sul": ["su"], "sullo": ["su"], "sulla": ["su"], "sui": ["su"], "sugli": ["su"], "sulle": ["su"], "sull": ["su"],
        "col": ["con"], "coi": ["con"], "pel": ["per"], "pelle": ["pelle"], "pelli": ["pelle"],
        "più": ["più"], "meglio": ["meglio"], "peggio": ["peggio"],
        "sono": ["essere"], "so": ["sapere"], "ha": ["avere"], "possa": ["potere"], "possiamo": ["potere"],
        "fatto": ["fare"], "fatta": ["fare"], "fatte": ["fare"], "farlo": ["fare"],
        "visto": ["vedere"], "visti": ["vedere"], "viste": ["vedere"], "messo": ["mettere"], "messi": ["mettere"],
        "credo": ["credere"], "credi": ["credere"], "tratta": ["trattare"], "pare": ["parere"], "usa": ["usare"], "dia": ["dare"],
        "cosi": ["così"], "fine": ["fine"], "fini": ["fine"], "danni": ["danno"],
        "parole": ["parola"], "bella": ["bello"], "belle": ["bello"], "giovani": ["giovane"],
        "ragazzo": ["ragazzo"], "ragazzi": ["ragazzo"], "santo": ["santo"], "santa": ["santo"], "san": ["santo"],
        "minore": ["minore"], "minori": ["minore"], "minimo": ["minimo"], "minima": ["minimo"],
        "tipico": ["tipico"], "tipica": ["tipico"], "tipici": ["tipico"], "tipiche": ["tipico"],
        "veramente": ["veramente"], "principalmente": ["principalmente"], "solitamente": ["solitamente"],
        "decisamente": ["decisamente"], "propriamente": ["propriamente"], "brevemente": ["brevemente"], "puramente": ["puramente"],
        "conte": ["conte"], "contessa": ["contessa"], "conta": ["contare"], "regina": ["regina"], "regine": ["regina"],
        "dea": ["dea"], "pulizia": ["pulizia"], "pulizie": ["pulizia"], "parete": ["parete"], "pareti": ["parete"],
        "marino": ["marino"], "marina": ["marino"], "marini": ["marino"], "marine": ["marino"],
    },
    # Catalan: the articles and weak pronouns (el la els les en es hi ho li em et ens us) were credited to one
    # another through the pronoun tables, and l' d' s' m' n' t' (split into bare letters) were dropped;
    # del/al/als are contractions; "va" went to the adjective "va", "és"/"són" were split with "ésser",
    # "dones" (women) and "dades" (data) went to "donar" and the Spanish "dar".
    "ca": {
        "el": ["el"], "la": ["el"], "els": ["el"], "les": ["el"], "l": ["el"], "lo": ["el"], "los": ["el"],
        "del": ["de"], "dels": ["de"], "d": ["de"], "al": ["a"], "als": ["a"], "pel": ["per"], "pels": ["per"],
        "en": ["en"], "n": ["en"], "es": ["es"], "se": ["es"], "s": ["es"], "si": ["si"], "hi": ["hi"], "ho": ["ho"],
        "li": ["li"], "em": ["em"], "me": ["em"], "m": ["em"], "et": ["et"], "te": ["et"], "t": ["et"],
        "ens": ["ens"], "nos": ["ens"], "us": ["us"], "vos": ["us"],
        "jo": ["jo"], "mi": ["jo"], "tu": ["tu"], "ell": ["ell"], "ells": ["ell"], "ella": ["ella"], "elles": ["ella"],
        "vostè": ["vostè"], "vostès": ["vostè"],
        "seu": ["seu"], "seva": ["seu"], "seus": ["seu"], "seves": ["seu"],
        "nostre": ["nostre"], "nostra": ["nostre"], "nostres": ["nostre"],
        "va": ["anar"], "pot": ["poder"], "pots": ["poder"], "vol": ["voler"], "vols": ["voler"],
        "he": ["haver"], "haig": ["haver"], "cal": ["caldre"],
        "és": ["ser"], "són": ["ser"], "ésser": ["ser"], "sigui": ["ser"], "serà": ["ser"], "eren": ["ser"], "sóc": ["ser"], "era": ["ser"],
        "dones": ["dona"], "dona": ["dona", "donar"], "dóna": ["donar"], "sis": ["sis"], "dades": ["dada"], "cases": ["casa"],
        "ric": ["ric"], "rica": ["ric"], "rics": ["ric"], "riques": ["ric"], "rius": ["riu"],
        "oferta": ["oferta"], "ofertes": ["oferta"], "partits": ["partit"], "reina": ["reina"], "reines": ["reina"],
        "noi": ["noi"], "nois": ["noi"], "segona": ["segon"], "segones": ["segon"], "quina": ["quin"], "quines": ["quin"],
        "bon": ["bo"], "bons": ["bo"], "millor": ["millor"], "millors": ["millor"],
    },
    # Galician (subtitles): the articles o/a/os/as were all under "as"; contractions (do da no na ao co pola
    # nun cun del nel) are sent to their preposition as for Portuguese; the clitics me/te/che/lle/se, the
    # neuters isto/iso/aquilo and "moi" had been swallowed; vir's forms went to "ver"; possessives and
    # este/ese/outro forms stood as separate words.
    "gl": {
        "o": ["o"], "os": ["o"], "lo": ["o"], "los": ["o"], "a": ["a"], "as": ["a"], "la": ["a"], "las": ["a"],
        "do": ["de"], "da": ["de"], "dos": ["de"], "das": ["de"], "del": ["de"], "dela": ["de"], "deles": ["de"], "delas": ["de"],
        "deste": ["de", "dar"], "dese": ["de"], "destes": ["de"], "deses": ["de"],
        "no": ["en"], "na": ["en"], "nos": ["en", "nós"], "nas": ["en"], "nun": ["en"], "nunha": ["en"], "nel": ["en"], "nela": ["en"],
        "ao": ["a"], "aos": ["a"], "ás": ["a"], "ós": ["a"],
        "co": ["con"], "coa": ["con"], "cos": ["con"], "coas": ["con"], "cun": ["con"], "cunha": ["con"],
        "polo": ["por"], "pola": ["por"], "polos": ["por"], "polas": ["por"],
        "se": ["se"], "me": ["me"], "te": ["te"], "che": ["che"], "cho": ["che"], "chos": ["che"], "lle": ["lle"], "lles": ["lle"],
        "isto": ["isto"], "iso": ["iso"], "aquilo": ["aquilo"], "moi": ["moi"],
        "este": ["este"], "ese": ["ese"], "aquel": ["aquel"], "outro": ["outro"], "algún": ["algún"],
        "doutro": ["de"], "doutra": ["de"], "doutros": ["de"], "doutras": ["de"], "díxome": ["dicir"],
        "esta": ["este"], "estes": ["este"], "estas": ["este"], "esa": ["ese"], "eses": ["ese"], "esas": ["ese"],
        "aquela": ["aquel"], "aqueles": ["aquel"], "aquelas": ["aquel"],
        "outra": ["outro"], "outros": ["outro"], "outras": ["outro"], "algunha": ["algún"], "algúns": ["algún"], "algunhas": ["algún"],
        "meu": ["meu"], "miña": ["meu"], "meus": ["meu"], "miñas": ["meu"], "teu": ["teu"], "túa": ["teu"], "teus": ["teu"], "túas": ["teu"],
        "seu": ["seu"], "súa": ["seu"], "seus": ["seu"], "súas": ["seu"], "noso": ["noso"], "nosa": ["noso"], "nosos": ["noso"], "nosas": ["noso"],
        "voso": ["voso"], "vosa": ["voso"], "vosos": ["voso"], "vosas": ["voso"],
        "son": ["ser"], "ten": ["ter"], "teño": ["ter"], "temos": ["ter"], "vai": ["ir"], "quere": ["querer"], "síntoo": ["sentir"],
        "vir": ["vir"], "ven": ["vir", "ver"], "vén": ["vir"], "veña": ["vir"], "vin": ["ver", "vir"],
        "creo": ["crer"], "crees": ["crer"], "canta": ["cantar"], "cantos": ["canto"],
        "mellor": ["mellor"], "mellores": ["mellor"], "peor": ["peor"], "peores": ["peor"], "má": ["malo"], "más": ["malo"],
        "morto": ["morto"], "morta": ["morto"], "mortos": ["morto"], "mortas": ["morto"],
        "maldito": ["maldito"], "maldita": ["maldito"], "malditos": ["maldito"], "malditas": ["maldito"],
        "caso": ["caso"], "casos": ["caso"], "casas": ["casa"], "praza": ["praza"], "prazas": ["praza"], "prazo": ["prazo"], "prazos": ["prazo"],
        "arriba": ["arriba"], "listo": ["listo"], "listos": ["listo"], "serio": ["serio"], "serios": ["serio"],
        "negocio": ["negocio"], "negocios": ["negocio"], "voto": ["voto"], "votos": ["voto"], "resultado": ["resultado"], "resultados": ["resultado"],
        "cargo": ["cargo"], "cargos": ["cargo"], "interese": ["interese"], "intereses": ["interese"], "peso": ["peso"],
        "totalmente": ["totalmente"], "colleita": ["colleita"], "paredes": ["parede"], "comida": ["comida"], "saída": ["saída"],
        "entrada": ["entrada"], "grao": ["grao"], "graos": ["grao"],
    },
    # Romanian: "noi"/"ne" sat in the adjective "nou", "da"/"dat"/"dată" in "dar", "oameni"/"om" in "vrea";
    # clitic pronouns (se, îi, le, mă, îmi, te, îți, vă) and possessives were folded into el/ea/eu/tu;
    # am/au/e/fost and past participles stood apart from their verbs; feminine nouns in their -a
    # (articled) form went to rare homograph verbs (echipa, baza, cauza, lumina...); text without
    # diacritics (si, in, ii, imi) is sent to the spelled word.
    "ro": {
        "noi": ["noi"], "ne": ["ne"], "nostru": ["nostru"], "noastră": ["nostru"], "noastre": ["nostru"], "noștri": ["nostru"],
        "vă": ["vă"], "voi": ["voi"], "vostru": ["vostru"], "voastră": ["vostru"], "voastre": ["vostru"], "voștri": ["vostru"],
        "voiam": ["vrea"], "voia": ["vrea"], "voiai": ["vrea"],
        "se": ["se"], "își": ["își"], "sine": ["sine"], "îi": ["îi"], "ii": ["îi"], "le": ["le"], "lor": ["lor"], "lui": ["lui"],
        "mă": ["mă"], "îmi": ["îmi"], "imi": ["îmi"], "te": ["te"], "îți": ["îți"], "îţi": ["îți"],
        "meu": ["meu"], "mea": ["meu"], "mei": ["meu"], "mele": ["meu"], "tău": ["tău"], "ta": ["tău"], "tăi": ["tău"], "tale": ["tău"],
        "său": ["său"], "sa": ["său"], "săi": ["său"], "sale": ["său"],
        "om": ["om"], "oameni": ["om"], "oamenii": ["om"],
        "da": ["da"], "dat": ["da"], "dau": ["da"], "dai": ["da"], "dă": ["da"], "dată": ["dată"], "data": ["dată"], "date": ["dată"],
        "am": ["avea"], "au": ["avea"], "ai": ["avea", "al"], "e": ["fi"], "fost": ["fi"], "pot": ["putea"],
        "ia": ["lua"], "iau": ["lua"], "iei": ["lua"], "pare": ["părea"], "pari": ["părea"], "pară": ["părea"],
        "spus": ["spune"], "văzut": ["vedea"], "luat": ["lua"], "găsit": ["găsi"], "ajuns": ["ajunge"], "auzit": ["auzi"],
        "pierdut": ["pierde"], "folosit": ["folosi"], "plecat": ["pleca"], "adus": ["aduce"], "terminat": ["termina"],
        "gândit": ["gândi"], "lăsat": ["lăsa"], "realizat": ["realiza"], "vorbit": ["vorbi"], "uitat": ["uita"],
        "prins": ["prinde"], "intrat": ["intra"], "trimis": ["trimite"], "încercat": ["încerca"], "ucis": ["ucide"],
        "prezintă": ["prezenta"], "prezint": ["prezenta"], "prezinte": ["prezenta"],
        "si": ["și"], "in": ["în"], "într": ["în"], "ntr": ["în"], "oare": ["oare"], "oh": ["oh"], "stele": ["stea"],
        "casă": ["casă"], "echipă": ["echipă"], "mână": ["mână"], "bază": ["bază"], "cauză": ["cauză"], "armă": ["armă"],
        "lumină": ["lumină"], "listă": ["listă"], "clasă": ["clasă"], "linie": ["linie"], "republică": ["republică"], "comandă": ["comandă"],
        "casa": ["casă"], "echipa": ["echipă"], "mâna": ["mână"], "baza": ["bază"], "cauza": ["cauză"], "arma": ["armă"],
        "seara": ["seară"], "lumina": ["lumină"], "zona": ["zonă"], "lista": ["listă"], "clasa": ["clasă"], "linia": ["linie"],
        "republica": ["republică"], "comanda": ["comandă"], "prima": ["prim"], "primă": ["prim"],
        "moarte": ["moarte"], "moartea": ["moarte"], "necunoscut": ["necunoscut"], "necunoscută": ["necunoscut"], "necunoscute": ["necunoscut"],
        "românii": ["român"], "români": ["român"], "românesc": ["românesc"], "românești": ["românesc"], "românească": ["românesc"], "românește": ["românesc"],
        "plăcile": ["placă"], "plăcilor": ["placă"],
    },
    # Latin (Wikipedia): the pronoun "is" was filed under "eo" (I go) and "est"/"sit" partly under "edo"/"sinus";
    # many adjectives stood under a feminine/neuter or ablative form or a homograph (magna under maximus,
    # bonum, nova, multa, publica, medio, anno), fero's forms under "ferus", reflexive sibi/sese under "suus";
    # participles factus/dictus/natus/positus are sent to their verbs.
    "la": {
        "is": ["is"], "eum": ["is"], "eius": ["is"], "ei": ["is"], "id": ["is"], "eorum": ["is"], "earum": ["is"], "eam": ["is"], "ii": ["is"], "eis": ["is"], "iis": ["is"], "ea": ["is"], "eae": ["is"], "eos": ["is"], "eas": ["is"], "eo": ["is"],
        "est": ["sum"], "esset": ["sum"], "sit": ["sum"], "esse": ["sum"], "fore": ["sum"], "foret": ["sum"],
        "posse": ["possum"],
        "qua": ["qui"],
        "at": ["at"],
        "anno": ["annus"],
        "primus": ["primus"], "primo": ["primus"],
        "magnus": ["magnus"], "magna": ["magnus"], "magni": ["magnus"], "magnum": ["magnus"], "magno": ["magnus"], "magnam": ["magnus"], "magnae": ["magnus"], "magnis": ["magnus"], "magnos": ["magnus"], "magnas": ["magnus"],
        "sibi": ["se"], "sese": ["se"],
        "sui": ["suus", "se"],
        "suus": ["suus"], "suo": ["suus"], "suam": ["suus"], "suis": ["suus"], "suas": ["suus"],
        "tuus": ["tuus"], "tua": ["tuus"], "tuum": ["tuus"], "tuo": ["tuus"], "tuae": ["tuus"], "tuos": ["tuus"], "tuis": ["tuus"], "tuas": ["tuus"],
        "nos": ["nos"],
        "factus": ["facio"], "facta": ["facio"], "facto": ["facio"], "factae": ["facio"], "facti": ["facio"], "factis": ["facio"], "factam": ["facio"], "factos": ["facio"], "factas": ["facio"],
        "dictus": ["dico"], "dicta": ["dico"], "dicti": ["dico"], "dicto": ["dico"], "dictis": ["dico"], "dictam": ["dico"], "dictae": ["dico"],
        "dictator": ["dictator"], "dictatorem": ["dictator"], "dictatoris": ["dictator"],
        "natus": ["nascor"], "nata": ["nascor"], "natum": ["nascor"], "nati": ["nascor"], "natae": ["nascor"], "natos": ["nascor"],
        "positus": ["pono"], "posita": ["pono"], "positum": ["pono"], "positae": ["pono"], "positis": ["pono"], "positam": ["pono"], "positi": ["pono"], "posito": ["pono"],
        "habere": ["habeo"], "cor": ["cor"], "cordis": ["cor"], "cordi": ["cor"], "certa": ["certus"], "certo": ["certus"],
        "parvum": ["parvus"], "parva": ["parvus"], "parvis": ["parvus"], "parvi": ["parvus"],
        "fero": ["fero"], "condo": ["condo"], "monstro": ["monstro"], "habito": ["habito"], "servo": ["servo"], "tracto": ["tracto"],
        "disco": ["disco"], "colo": ["colo"], "formo": ["formo"], "comitor": ["comitor"], "administro": ["administro"], "vario": ["vario"], "pario": ["pario"],
        "fert": ["fero"], "tulit": ["fero"], "fertur": ["fero"], "ferunt": ["fero"], "ferre": ["fero"], "tulerunt": ["fero"], "ferebat": ["fero"], "feruntur": ["fero"],
        "multa": ["multus"], "multis": ["multus"], "multae": ["multus"], "multas": ["multus"], "multarum": ["multus"], "multam": ["multus"],
        "nova": ["novus"], "novae": ["novus"], "novam": ["novus"], "novis": ["novus"], "novas": ["novus"], "novarum": ["novus"],
        "secunda": ["secundus"], "secundam": ["secundus"], "secundae": ["secundus"], "secundas": ["secundus"], "secundis": ["secundus"], "secundarum": ["secundus"], "secundorum": ["secundus"],
        "tertio": ["tertius"], "tertia": ["tertius"], "tertias": ["tertius"],
        "publica": ["publicus"], "publicae": ["publicus"], "publicam": ["publicus"], "publicis": ["publicus"], "publicarum": ["publicus"], "publicas": ["publicus"],
        "omnia": ["omnis"],
        "varius": ["varius"], "variis": ["varius"], "varia": ["varius"], "variae": ["varius"], "varias": ["varius"], "varios": ["varius"], "varii": ["varius"],
        "variat": ["vario"], "variant": ["vario"],
        "vera": ["verus"], "veras": ["verus"],
        "nulla": ["nullus"], "nullo": ["nullus"], "nullam": ["nullus"], "nulli": ["nullus"], "nullius": ["nullus"],
        "nonnulli": ["nonnullus"], "nonnullis": ["nonnullus"], "nonnullos": ["nonnullus"], "nonnullorum": ["nonnullus"],
        "diversus": ["diversus"], "diversis": ["diversus"], "diversa": ["diversus"], "diversae": ["diversus"], "diversas": ["diversus"], "diversi": ["diversus"], "diversarum": ["diversus"], "diversos": ["diversus"], "diversorum": ["diversus"],
        "bonus": ["bonus"], "bonum": ["bonus"], "bona": ["bonus"], "boni": ["bonus"], "bono": ["bonus"], "bonorum": ["bonus"], "bonam": ["bonus"], "bonis": ["bonus"], "bonae": ["bonus"], "bonos": ["bonus"], "bonas": ["bonus"],
        "malum": ["malus"], "mala": ["malus"], "mali": ["malus"], "malis": ["malus"], "malorum": ["malus"],
        "proprius": ["proprius"], "propria": ["proprius"], "proprium": ["proprius"], "propriis": ["proprius"], "proprio": ["proprius"], "proprias": ["proprius"], "proprii": ["proprius"], "propriorum": ["proprius"],
        "medio": ["medius"], "media": ["medius"], "medias": ["medius"], "mediae": ["medius"], "mediam": ["medius"], "medii": ["medius"], "mediis": ["medius"], "mediorum": ["medius"],
        "civile": ["civilis"], "civilis": ["civilis"], "civili": ["civilis"], "civilem": ["civilis"], "civilibus": ["civilis"], "civiles": ["civilis"], "civilium": ["civilis"], "civilia": ["civilis"],
        "antiqua": ["antiquus"], "antiquo": ["antiquus"], "antiquas": ["antiquus"],
        "latinus": ["latinus"], "latina": ["latinus"], "latino": ["latinus"], "latinas": ["latinus"], "latinae": ["latinus"], "latinum": ["latinus"], "latini": ["latinus"],
        "populo": ["populus"],
        "popularis": ["popularis"], "populares": ["popularis"], "populari": ["popularis"], "popularem": ["popularis"], "popularium": ["popularis"], "populare": ["popularis"], "popularia": ["popularis"],
        "loco": ["locus"], "loca": ["locus"],
        "titulo": ["titulus"],
        "initio": ["initium"], "initia": ["initium"],
        "principio": ["principium"], "principium": ["principium"], "principia": ["principium"],
        "anima": ["anima"], "animam": ["anima"], "animae": ["anima"], "animarum": ["anima"], "animas": ["anima"],
        "animo": ["animus"],
        "par": ["par"], "pares": ["par"], "paris": ["par"], "parium": ["par"], "parem": ["par"], "pari": ["par"],
        "peperit": ["pario"],
        "minus": ["minus"],
        "minor": ["minor"], "minores": ["minor"], "minoris": ["minor"], "minorem": ["minor"], "minore": ["minor"],
        "summus": ["summus"],
        "proximus": ["proximus"], "proximis": ["proximus"], "proximos": ["proximus"], "proximi": ["proximus"], "proximae": ["proximus"],
        "manet": ["maneo"], "manent": ["maneo"], "manere": ["maneo"],
        "servantur": ["servo"], "servatur": ["servo"], "servare": ["servo"], "servavit": ["servo"], "servat": ["servo"], "servant": ["servo"], "servari": ["servo"],
        "habitant": ["habito"], "habitabant": ["habito"], "habitabat": ["habito"], "habitat": ["habito"], "habitare": ["habito"],
        "tractat": ["tracto"], "tractant": ["tracto"], "tractantur": ["tracto"], "tractare": ["tracto"], "tractatur": ["tracto"], "tractavit": ["tracto"],
        "monstrat": ["monstro"], "monstrant": ["monstro"], "monstratur": ["monstro"], "monstravit": ["monstro"], "monstrantur": ["monstro"], "monstrare": ["monstro"], "monstrans": ["monstro"],
        "condidit": ["condo"], "conditum": ["condo"], "condiderunt": ["condo"], "conditus": ["condo"], "condere": ["condo"], "condiderat": ["condo"], "condidisse": ["condo"], "conderet": ["condo"],
        "didicit": ["disco"], "discere": ["disco"], "disci": ["disco"], "disco": ["disco"], "discit": ["disco"], "didicerunt": ["disco"],
        "formant": ["formo"], "format": ["formo"], "formatur": ["formo"], "formantur": ["formo"], "formans": ["formo"], "formare": ["formo"], "formatum": ["formo"], "formavit": ["formo"],
        "colitur": ["colo"], "coluntur": ["colo"], "colebat": ["colo"], "colere": ["colo"], "colebant": ["colo"], "colunt": ["colo"], "coluit": ["colo"], "colebatur": ["colo"],
        "comitatur": ["comitor"], "comitantur": ["comitor"], "comitari": ["comitor"], "comitat": ["comitor"], "comitabantur": ["comitor"], "comitabatur": ["comitor"], "comitaretur": ["comitor"],
        "administravit": ["administro"], "administrabat": ["administro"], "administratur": ["administro"], "administrat": ["administro"], "administrabatur": ["administro"], "administrandi": ["administro"],
        "lusit": ["ludo"],
        "vitia": ["vitium"], "vitiis": ["vitium"],
        "tribuum": ["tribus"], "tribubus": ["tribus"], "tribu": ["tribus"], "tribum": ["tribus"],
        "leges": ["lex"],
        "uni": ["unus"],
    },
}

_EXCLUDE_B = {
    # French: names (Paris, Jean, Marie, Pierre) and stray tokens.
    "fr": {"paris", "jean", "marie", "maria", "pierre", "ku", "cu", "sit"},
    # Spanish: the country Chile, a name and stray tokens.
    "es": {"chile", "logan", "er", "eme", "emes"},
    # Italian: city names, English "the" and stray tokens.
    "it": {"roma", "napoli", "the", "gennaro", "die"},
    # Catalan: the country name and Spanish "dar" with its forms.
    "ca": {"espanya", "dar", "di", "da", "dem", "den", "dan", "das", "maria"},
    # Galician: English "lord" from subtitles.
    "gl": {"lord"},
    # Romanian: the country name, the split-off "-ul" of site-ul and stray tokens.
    "ro": {"românia", "româniei", "ul", "ima", "ime", "imai", "great", "grease"},
    # Latin: personal names and German/French tokens in the Wikipedia corpus.
    "la": {"anna", "paulus", "pauli", "paula", "paulae", "marcus", "marci", "marco", "marcum", "der", "haut", "e.g."},
}

# ---- Reviewed group C ----
# Frequency-list fixes for ru, uk, be, bg, mk, sr, hr (reviewer C), to merge into
# tools/build_frequency_list.py OVERRIDES and EXCLUDE.

_OVERRIDES_C = {
    # Russian: pronoun cases were dropped (each is tied to all eight pronouns, over MAX_LEMMAS_PER_FORM)
    # or kept as words of their own (его, её, их); было/есть/стал sat apart from быть/стать; the
    # dictionary ties aspect partners (говорить to сказать) and derived words (жизненный to жизнь,
    # ученик to учить, дорога to дорогой) together, so those are kept as words of their own.
    "ru": {
        "мне": ["я"], "меня": ["я"], "мной": ["я"], "мною": ["я"], "тебя": ["ты"], "тебе": ["ты"],
        "тобой": ["ты"], "тобою": ["ты"], "его": ["он"], "ему": ["он"], "него": ["он"], "нему": ["он"],
        "нём": ["он"], "её": ["она"], "ее": ["она"], "ей": ["она"], "неё": ["она"], "нее": ["она"],
        "ней": ["она"], "ею": ["она"], "нею": ["она"], "их": ["они"], "них": ["они"], "ими": ["они"],
        "ними": ["они"], "им": ["он", "они"], "ним": ["он", "они"], "нас": ["мы"], "нам": ["мы"],
        "нами": ["мы"], "вас": ["вы"], "вам": ["вы"], "вами": ["вы"], "себе": ["себя"], "собой": ["себя"],
        "собою": ["себя"], "все": ["весь"], "всех": ["весь"], "всем": ["весь"], "всеми": ["весь"],
        "было": ["быть"], "есть": ["быть"], "стал": ["стать"], "та": ["тот"], "свое": ["свой"],
        "своем": ["свой"], "мое": ["мой"], "твое": ["твой"], "идет": ["идти"], "остается": ["остаться"],
        "дает": ["дать"], "начал": ["начать"], "начале": ["начало"], "началом": ["начало"],
        "началу": ["начало"], "начала": ["начать", "начало"], "лучшее": ["лучший"], "главное": ["главный"],
        "настоящее": ["настоящий"], "воздуха": ["воздух"], "подам": ["подать"], "передам": ["передать"],
        "во": ["во"], "говорить": ["говорить"], "делать": ["делать"], "смотреть": ["смотреть"],
        "понимать": ["понимать"], "брать": ["брать"], "давать": ["давать"], "узнать": ["узнать"],
        "увидеть": ["увидеть"], "пойти": ["пойти"], "написать": ["написать"], "решать": ["решать"],
        "принимать": ["принимать"], "находить": ["находить"], "приходить": ["приходить"],
        "выходить": ["выходить"], "проходить": ["проходить"], "начинать": ["начинать"],
        "становиться": ["становиться"], "называться": ["называться"], "называть": ["называть"],
        "помогать": ["помогать"], "отвечать": ["отвечать"], "слышать": ["слышать"],
        "создавать": ["создавать"], "ставить": ["ставить"], "покупать": ["покупать"],
        "получать": ["получать"], "показывать": ["показывать"], "проводить": ["проводить"],
        "оставаться": ["оставаться"], "открывать": ["открывать"], "собирать": ["собирать"],
        "собираться": ["собираться"], "возвращаться": ["возвращаться"], "рассказывать": ["рассказывать"],
        "подумать": ["подумать"], "сыграть": ["сыграть"], "поехать": ["поехать"], "прочитать": ["прочитать"],
        "приготовить": ["приготовить"], "понравиться": ["понравиться"], "попытаться": ["попытаться"],
        "появиться": ["появиться"], "входить": ["входить"], "умирать": ["умирать"], "строить": ["строить"],
        "терять": ["терять"], "забывать": ["забывать"], "выбирать": ["выбирать"], "снимать": ["снимать"],
        "спрашивать": ["спрашивать"], "предлагать": ["предлагать"], "занимать": ["занимать"],
        "убивать": ["убивать"], "сесть": ["сесть"], "съесть": ["съесть"], "поесть": ["поесть"],
        "жизненный": ["жизненный"], "мировой": ["мировой"], "городской": ["городской"],
        "военный": ["военный"], "немецкий": ["немецкий"], "народный": ["народный"], "рабочий": ["рабочий"],
        "центральный": ["центральный"], "денежный": ["денежный"], "мужской": ["мужской"],
        "женский": ["женский"], "семейный": ["семейный"], "судебный": ["судебный"], "сельский": ["сельский"],
        "село": ["село"], "государственный": ["государственный"], "общественный": ["общественный"],
        "писатель": ["писатель"], "показатель": ["показатель"], "определение": ["определение"],
        "данный": ["данный"], "большинство": ["большинство"], "подруга": ["подруга"],
        "маленький": ["маленький"], "дорога": ["дорога"], "ученик": ["ученик"], "учение": ["учение"],
        "училище": ["училище"], "чтение": ["чтение"], "пиво": ["пиво"], "напиток": ["напиток"],
        "площадка": ["площадка"], "королева": ["королева"], "красота": ["красота"],
        "полицейский": ["полицейский"], "лучший": ["лучший"], "ток": ["ток"], "ход": ["ход"],
        "кома": ["кома"], "начало": ["начало"], "воздух": ["воздух"], "понятие": ["понятие"],
        "домашний": ["домашний"], "областной": ["областной"], "ночной": ["ночной"], "любимый": ["любимый"],
        "морской": ["морской"], "спортивный": ["спортивный"], "музыкальный": ["музыкальный"],
        "друг": ["друг"], "мало": ["мало"], "меньше": ["меньше"],
    },
    # Ukrainian: same pronoun problem as Russian; є was dropped; київ/львів were counted for кий/лев;
    # діти, the plural of дитина, took діло; aspect partners and derived words split off as in Russian.
    "uk": {
        "мене": ["я"], "мені": ["я"], "мною": ["я"], "тебе": ["ти"], "тобі": ["ти"], "тобою": ["ти"],
        "його": ["він"], "йому": ["він"], "нього": ["він"], "ньому": ["він"], "нім": ["він"], "її": ["вона"],
        "їй": ["вона"], "неї": ["вона"], "ній": ["вона"], "нею": ["вона"], "їх": ["вони"], "них": ["вони"],
        "ними": ["вони"], "їм": ["вони"], "ним": ["він", "вони"], "нас": ["ми"], "нам": ["ми"],
        "нами": ["ми"], "вас": ["ви"], "вам": ["ви"], "вами": ["ви"], "собі": ["себе"], "собою": ["себе"],
        "все": ["весь"], "усе": ["весь"], "усі": ["весь"], "усіх": ["весь"], "усім": ["весь"],
        "усіма": ["весь"], "є": ["бути"], "став": ["стати"], "стала": ["стати"], "стане": ["стати"],
        "яка": ["який"], "яку": ["який"], "шо": ["що"], "ж": ["же"], "діти": ["дитина"], "дітей": ["дитина"],
        "дітям": ["дитина"], "дітьми": ["дитина"], "дітях": ["дитина"], "люди": ["людина"],
        "людей": ["людина"], "людям": ["людина"], "людьми": ["людина"], "людях": ["людина"],
        "тис": ["тисяча"], "грн": ["гривня"], "зв'язку": ["зв'язок"], "чому": ["чому"], "чомусь": ["чомусь"],
        "діло": ["діло"], "головний": ["головний"], "казати": ["казати"], "робити": ["робити"],
        "піти": ["піти"], "бачити": ["бачити"], "брати": ["брати"], "знаходитися": ["знаходитися"],
        "починати": ["починати"], "розуміти": ["розуміти"], "написати": ["написати"],
        "дякувати": ["дякувати"], "почути": ["почути"], "український": ["український"],
        "російський": ["російський"], "міський": ["міський"], "місцевий": ["місцевий"],
        "державний": ["державний"], "грошовий": ["грошовий"], "західний": ["західний"],
        "сільський": ["сільський"], "передній": ["передній"], "економічний": ["економічний"],
        "політичний": ["політичний"], "сторінка": ["сторінка"], "маленький": ["маленький"],
        "хлопчик": ["хлопчик"], "поліцейський": ["поліцейський"], "святий": ["святий"],
        "бойовий": ["бойовий"], "східний": ["східний"], "вчений": ["вчений"], "вивчати": ["вивчати"],
        "вивчити": ["вивчити"], "пані": ["пані"], "дивний": ["дивний"], "дивно": ["дивно"],
        "королева": ["королева"], "вино": ["вино"], "ставка": ["ставка"], "дитячий": ["дитячий"],
        "людський": ["людський"],
    },
    # Belarusian: pronoun cases were dropped; forms written with ў after a vowel (ўжо, ўсе, ўлады) are
    # not in the dictionary and were dropped too; ў itself is у; some common words the small
    # dictionary lacks are kept as words of their own.
    "be": {
        "ў": ["у"], "мяне": ["я"], "мне": ["я"], "мной": ["я"], "мною": ["я"], "цябе": ["ты"],
        "табе": ["ты"], "табой": ["ты"], "табою": ["ты"], "яго": ["ён"], "яму": ["ён"], "яе": ["яна"],
        "ёй": ["яна"], "ёю": ["яна"], "іх": ["яны"], "імі": ["яны"], "ім": ["ён", "яны"], "нас": ["мы"],
        "нам": ["мы"], "намі": ["мы"], "вас": ["вы"], "вам": ["вы"], "вамі": ["вы"], "сабе": ["сябе"],
        "сабой": ["сябе"], "сабою": ["сябе"], "ўсё": ["увесь"], "усё": ["увесь"], "ўсе": ["увесь"],
        "усе": ["увесь"], "ўсіх": ["увесь"], "усіх": ["увесь"], "ўсім": ["увесь"], "ўсяго": ["увесь"],
        "ўсёй": ["увесь"], "ўвесь": ["увесь"], "ўсю": ["увесь"], "ўся": ["увесь"], "усімі": ["увесь"],
        "ўжо": ["ужо"], "ўлады": ["улада"], "ўлада": ["улада"], "ўладу": ["улада"], "ўдзел": ["удзел"],
        "ўпершыню": ["упершыню"], "ўзровень": ["узровень"], "ўзроўню": ["узровень"], "ўвагу": ["увага"],
        "ўмовы": ["умова"], "ўмовах": ["умова"], "ўрад": ["урад"], "ўрада": ["урад"], "ўвогуле": ["увогуле"],
        "ўзяць": ["узяць"], "якія": ["які"], "які": ["які"], "якая": ["які"], "якіх": ["які"],
        "якой": ["які"], "якую": ["які"], "якога": ["які"], "якім": ["які"], "якое": ["які"],
        "стала": ["стаць"], "партыі": ["партыя"], "за": ["за"], "ні": ["ні"], "бы": ["бы"],
        "далей": ["далей"], "нягледзячы": ["нягледзячы"], "значна": ["значна"], "даволі": ["даволі"],
        "спачатку": ["спачатку"], "шэраг": ["шэраг"], "паколькі": ["паколькі"], "згодна": ["згодна"],
        "прычым": ["прычым"], "прыкладна": ["прыкладна"], "хаця": ["хаця"], "практычна": ["практычна"],
        "рабіць": ["рабіць"], "казаць": ["казаць"], "пайсці": ["пайсці"], "напісаць": ["напісаць"],
        "узяць": ["узяць"], "прыйсці": ["прыйсці"], "разумець": ["разумець"], "спрабаваць": ["спрабаваць"],
        "сельскі": ["сельскі"], "пярэдні": ["пярэдні"], "жаночы": ["жаночы"], "каралева": ["каралева"],
        "сястра": ["сястра"], "гаварыць": ["гаварыць"],
    },
    # Bulgarian: е (is) was dropped as a stray letter; pronoun clitics and long forms sat apart
    # from аз/ти/той/тя/ние/вие/те; дете was counted for де, пет for път, всички for всеки;
    # aspect partners and derived adjectives split off.
    "bg": {
        "е": ["съм"], "бих": ["съм"], "мен": ["аз"], "мене": ["аз"], "ме": ["аз"], "ми": ["аз"],
        "теб": ["ти"], "тебе": ["ти"], "него": ["той"], "го": ["той"], "му": ["той"], "нея": ["тя"],
        "я": ["тя"], "ѝ": ["тя"], "й": ["тя"], "нас": ["ние"], "ни": ["ние"], "вас": ["вие"], "ви": ["вие"],
        "тях": ["те"], "ги": ["те"], "им": ["те"], "става": ["ставам"], "часа": ["час"], "края": ["край"],
        "първи": ["пръв"], "нашите": ["наш"], "души": ["душа"], "г-жа": ["госпожа"], "д-р": ["доктор"],
        "малка": ["малък"], "малки": ["малък"], "малката": ["малък"], "малкия": ["малък"],
        "малкият": ["малък"], "пет": ["пет"], "дете": ["дете"], "цел": ["цел"], "всички": ["всички"],
        "всичко": ["всичко"], "малък": ["малък"], "ставам": ["ставам"], "казвам": ["казвам"],
        "виждам": ["виждам"], "разбера": ["разбера"], "идвам": ["идвам"], "намирам": ["намирам"],
        "отивам": ["отивам"], "започвам": ["започвам"], "оставам": ["оставам"], "убивам": ["убивам"],
        "получавам": ["получавам"], "местен": ["местен"], "женски": ["женски"], "странен": ["странен"],
        "световен": ["световен"], "приятелка": ["приятелка"], "госпожа": ["госпожа"], "минал": ["минал"],
        "сигурност": ["сигурност"], "значение": ["значение"], "държавен": ["държавен"], "военен": ["военен"],
        "следващ": ["следващ"], "готовност": ["готовност"], "сложен": ["сложен"], "създаване": ["създаване"],
        "основен": ["основен"], "любовен": ["любовен"], "съпруга": ["съпруга"], "страшен": ["страшен"],
        "учен": ["учен"], "кралица": ["кралица"], "лудост": ["лудост"], "труден": ["труден"],
        "дължина": ["дължина"], "зле": ["зле"],
    },
    # Macedonian: е (is) was dropped; тие/таа/тоа and the possessives were all counted for тој/јас/ти;
    # clitics sat with the wrong pronoun; aspect partners and derived words split off.
    "mk": {
        "е": ["сум"], "него": ["тој"], "нему": ["тој"], "го": ["тој"], "му": ["тој"], "неа": ["таа"],
        "нејзе": ["таа"], "ја": ["таа"], "ѝ": ["таа"], "нив": ["тие"], "ним": ["тие"], "ги": ["тие"],
        "им": ["тие"], "нè": ["ние"], "мојот": ["мој"], "мојата": ["мој"], "моите": ["мој"],
        "моето": ["мој"], "моја": ["мој"], "твојот": ["твој"], "твојата": ["твој"], "твоите": ["твој"],
        "твоето": ["твој"], "твоја": ["твој"], "својата": ["свој"], "својот": ["свој"], "нашиот": ["наш"],
        "нашата": ["наш"], "нашите": ["наш"], "вашиот": ["ваш"], "вашата": ["ваш"], "вашите": ["ваш"],
        "мажи": ["маж"], "мажите": ["маж"], "мали": ["мал"], "малите": ["мал"], "часа": ["час"],
        "милиони": ["милион"], "ке": ["ќе"], "сè": ["сѐ"], "водат": ["води"], "тие": ["тие"], "таа": ["таа"],
        "тоа": ["тоа"], "мој": ["мој"], "твој": ["твој"], "наш": ["наш"], "ваш": ["ваш"],
        "бидејќи": ["бидејќи"], "кај": ["кај"], "сѐ": ["сѐ"], "случи": ["случи"], "случува": ["случува"],
        "прави": ["прави"], "гледа": ["гледа"], "рече": ["рече"], "доаѓа": ["доаѓа"], "отиде": ["отиде"],
        "најде": ["најде"], "донесе": ["донесе"], "дава": ["дава"], "мислење": ["мислење"],
        "значење": ["значење"], "различен": ["различен"], "следен": ["следен"], "татко": ["татко"],
        "ќерка": ["ќерка"], "сестра": ["сестра"], "силен": ["силен"], "личен": ["личен"],
        "потребен": ["потребен"], "централен": ["централен"], "среќен": ["среќен"], "јавност": ["јавност"],
        "важен": ["важен"], "учи": ["учи"], "губи": ["губи"], "рана": ["рана"], "кралица": ["кралица"],
        "доведе": ["доведе"], "одговара": ["одговара"], "негов": ["негов"], "нејзин": ["нејзин"],
        "нивен": ["нивен"], "милион": ["милион"],
    },
    # Serbian (Latin): forms of biti (je, sam, si, bilo) and imati, trebati, morati, raditi sat apart
    # as homographs; pronoun cases sat apart; tvoj/svoj were counted for moj; ijekavian duplicates of
    # ekavian words (htjeti, vidjeti, ovdje) split their counts; i (and) was dropped as a letter;
    # Cyrillic tokens in the counts stood apart from their Latin spelling.
    "sr": {
        "je": ["biti"], "sam": ["biti"], "si": ["biti"], "bilo": ["biti"], "bila": ["biti"],
        "bili": ["biti"], "bile": ["biti"], "jesu": ["biti"], "jesi": ["biti"], "nisi": ["biti"],
        "biste": ["biti"], "budeš": ["biti"], "је": ["biti"], "bit": ["biti"], "biću": ["biti"],
        "me": ["ja"], "mene": ["ja"], "meni": ["ja"], "mnom": ["ja"], "mnome": ["ja"], "ја": ["ja"],
        "te": ["ti"], "tebe": ["ti"], "tebi": ["ti"], "tobom": ["ti"], "ти": ["ti"], "ga": ["on"],
        "njega": ["on"], "mu": ["on"], "njemu": ["on"], "njim": ["on"], "njime": ["on"], "joj": ["ona"],
        "nju": ["ona"], "ju": ["ona"], "njoj": ["ona"], "njom": ["ona"], "njome": ["ona"], "nje": ["ona"],
        "nas": ["mi"], "nama": ["mi"], "ми": ["mi"], "vas": ["vi"], "vama": ["vi"], "ih": ["oni"],
        "njih": ["oni"], "im": ["oni"], "njima": ["oni"], "one": ["oni"], "sebi": ["sebe"],
        "sobom": ["sebe"], "svi": ["sav"], "sve": ["sav"], "ima": ["imati"], "imam": ["imati"],
        "treba": ["trebati"], "nema": ["nemati"], "mora": ["morati"], "radi": ["raditi"],
        "radio": ["raditi"], "radim": ["raditi"], "rade": ["raditi"], "govori": ["govoriti"],
        "govore": ["govoriti"], "čini": ["činiti"], "čine": ["činiti"], "traže": ["tražiti"],
        "izgleda": ["izgledati"], "žele": ["želeti"], "željeti": ["želeti"], "došla": ["doći"],
        "stane": ["stati"], "duše": ["duša"], "toga": ["taj"], "koga": ["ko"], "tko": ["ko"],
        "čega": ["šta"], "sta": ["šta"], "шта": ["šta"], "што": ["što"], "veće": ["velik"],
        "veća": ["velik"], "veću": ["velik"], "većem": ["velik"], "vezi": ["veza"], "veze": ["veza"],
        "vezu": ["veza"], "nova": ["nov"], "mal": ["mali"], "ljudi": ["čovek"], "ljude": ["čovek"],
        "ljudima": ["čovek"], "čovjek": ["čovek"], "htjeti": ["hteti"], "ce": ["hteti"],
        "vidjeti": ["videti"], "voljeti": ["voleti"], "živjeti": ["živeti"], "vjerovati": ["verovati"],
        "razumjeti": ["razumeti"], "ovdje": ["ovde"], "gdje": ["gde"], "vrijeme": ["vreme"],
        "uvijek": ["uvek"], "prije": ["pre"], "mjesto": ["mesto"], "nitko": ["niko"], "umrijeti": ["umreti"],
        "osjećati": ["osećati"], "djeca": ["deca"], "svijet": ["svet"], "djevojka": ["devojka"],
        "donijeti": ["doneti"], "lijepo": ["lepo"], "mjesec": ["mesec"], "dijete": ["dete"],
        "uspjeti": ["uspeti"], "pobjeći": ["pobeći"], "lijep": ["lep"], "riječ": ["reč"], "tijelo": ["telo"],
        "poslije": ["posle"], "provjeriti": ["proveriti"], "dolje": ["dole"], "promijeniti": ["promeniti"],
        "riješiti": ["rešiti"], "sljedeći": ["sledeći"], "posljednji": ["poslednji"], "osjećaj": ["osećaj"],
        "negdje": ["negde"], "pobijediti": ["pobediti"], "predsjednik": ["predsednik"],
        "smiješno": ["smešno"], "pjesma": ["pesma"], "sjetiti": ["setiti"], "dječak": ["dečak"],
        "povrijediti": ["povrediti"], "naprijed": ["napred"], "vijesti": ["vesti"], "svjetlo": ["svetlo"],
        "zvijezda": ["zvezda"], "osjetiti": ["osetiti"], "djelovati": ["delovati"],
        "natjerati": ["naterati"], "vjerovatno": ["verovatno"], "primijetiti": ["primetiti"],
        "nedjelja": ["nedelja"], "sretan": ["srećan"], "у": ["u"], "и": ["i"], "а": ["a"], "о": ["o"],
        "с": ["s"], "сада": ["sad"], "ко": ["ko"], "kome": ["ko"], "он": ["on"], "га": ["on"], "му": ["on"],
        "она": ["ona"], "они": ["oni"], "их": ["oni"], "ме": ["ja"], "мене": ["ja"], "те": ["ti"],
        "тебе": ["ti"], "нас": ["mi"], "нам": ["mi"], "вас": ["vi"], "ви": ["vi"], "вам": ["vi"],
        "cio": ["ceo"], "ja": ["ja"], "ti": ["ti"], "on": ["on"], "ona": ["ona"], "ono": ["ono"],
        "mi": ["mi"], "vi": ["vi"], "oni": ["oni"], "se": ["se"], "sebe": ["sebe"], "moj": ["moj"],
        "tvoj": ["tvoj"], "svoj": ["svoj"], "naš": ["naš"], "vaš": ["vaš"], "njegov": ["njegov"],
        "njen": ["njen"], "njihov": ["njihov"], "i": ["i"], "samo": ["samo"], "posle": ["posle"],
        "dole": ["dole"], "značenje": ["značenje"], "mali": ["mali"], "čovek": ["čovek"], "smeti": ["smeti"],
        "super": ["super"], "more": ["more"],
    },
    # Croatian: as Serbian, with the ijekavian words kept and their ekavian duplicates merged in.
    "hr": {
        "je": ["biti"], "sam": ["biti"], "si": ["biti"], "bilo": ["biti"], "bila": ["biti"],
        "bili": ["biti"], "bile": ["biti"], "jesu": ["biti"], "jesi": ["biti"], "nisi": ["biti"],
        "biste": ["biti"], "budeš": ["biti"], "bit": ["biti"], "biću": ["biti"], "me": ["ja"],
        "mene": ["ja"], "meni": ["ja"], "mnom": ["ja"], "mnome": ["ja"], "te": ["ti"], "tebe": ["ti"],
        "tebi": ["ti"], "tobom": ["ti"], "ga": ["on"], "njega": ["on"], "mu": ["on"], "njemu": ["on"],
        "njim": ["on"], "njime": ["on"], "joj": ["ona"], "nju": ["ona"], "ju": ["ona"], "njoj": ["ona"],
        "njom": ["ona"], "njome": ["ona"], "nje": ["ona"], "nas": ["mi"], "nama": ["mi"], "vas": ["vi"],
        "vama": ["vi"], "ih": ["oni"], "njih": ["oni"], "im": ["oni"], "njima": ["oni"], "one": ["oni"],
        "sebi": ["sebe"], "sobom": ["sebe"], "svi": ["sav"], "sve": ["sav"], "ima": ["imati"],
        "imam": ["imati"], "treba": ["trebati"], "nema": ["nemati"], "mora": ["morati"], "radi": ["raditi"],
        "radio": ["raditi"], "radim": ["raditi"], "toga": ["taj"], "ko": ["tko"], "koga": ["tko"],
        "šta": ["što"], "sta": ["što"], "vidjela": ["vidjeti"], "vidjelo": ["vidjeti"], "veze": ["veza"],
        "vezi": ["veza"], "vezu": ["veza"], "izgleda": ["izgledati"], "veće": ["velik"], "veća": ["velik"],
        "veću": ["velik"], "većem": ["velik"], "nova": ["nov"], "mal": ["mali"], "ljudi": ["čovjek"],
        "ljude": ["čovjek"], "ljudima": ["čovjek"], "čovek": ["čovjek"], "hteti": ["htjeti"],
        "ce": ["htjeti"], "cu": ["htjeti"], "videti": ["vidjeti"], "želeti": ["željeti"],
        "voleti": ["voljeti"], "ovde": ["ovdje"], "gde": ["gdje"], "vreme": ["vrijeme"],
        "verovati": ["vjerovati"], "uvek": ["uvijek"], "mesto": ["mjesto"], "razumeti": ["razumjeti"],
        "osećati": ["osjećati"], "svet": ["svijet"], "deca": ["djeca"], "živeti": ["živjeti"],
        "devojka": ["djevojka"], "umreti": ["umrijeti"], "lepo": ["lijepo"], "dete": ["dijete"],
        "mesec": ["mjesec"], "uspeti": ["uspjeti"], "pobeći": ["pobjeći"], "reč": ["riječ"],
        "lep": ["lijep"], "doneti": ["donijeti"], "telo": ["tijelo"], "dole": ["dolje"],
        "proveriti": ["provjeriti"], "rešiti": ["riješiti"], "sledeći": ["sljedeći"],
        "promeniti": ["promijeniti"], "poslednji": ["posljednji"], "osećaj": ["osjećaj"],
        "negde": ["negdje"], "dečak": ["dječak"], "pobediti": ["pobijediti"], "predsednik": ["predsjednik"],
        "smešno": ["smiješno"], "pesma": ["pjesma"], "setiti": ["sjetiti"], "povrediti": ["povrijediti"],
        "napred": ["naprijed"], "svetlo": ["svjetlo"], "lek": ["lijek"], "vesti": ["vijesti"],
        "zvezda": ["zvijezda"], "umesto": ["umjesto"], "pre": ["prije"], "ja": ["ja"], "ti": ["ti"],
        "on": ["on"], "ona": ["ona"], "ono": ["ono"], "mi": ["mi"], "vi": ["vi"], "oni": ["oni"],
        "se": ["se"], "sebe": ["sebe"], "moj": ["moj"], "tvoj": ["tvoj"], "svoj": ["svoj"], "naš": ["naš"],
        "vaš": ["vaš"], "njegov": ["njegov"], "njen": ["njen"], "njihov": ["njihov"], "i": ["i"],
        "samo": ["samo"], "značenje": ["značenje"], "mali": ["mali"], "čovjek": ["čovjek"],
        "netko": ["netko"], "super": ["super"], "more": ["more"], "dovraga": ["dovraga"],
    },
}

_EXCLUDE_C = {
    # Russian: abbreviations, an English word, and Germany, a name.
    "ru": {
        "the", "word", "германией", "германии", "германию", "германия", "го", "др", "см",
    },
    # Ukrainian: names (Ukraine, Russia, Moscow, Kyiv, Lviv), abbreviations and a stray letter.
    "uk": {
        "к", "києва", "києвом", "києві", "київ", "львова", "львові", "львів", "москва", "москви", "москву",
        "москві", "росію", "росія", "росією", "росії", "ст", "україна", "україни", "україною", "україну",
        "україні",
    },
    # Belarusian: nothing beyond what the dictionary already leaves out.
    "be": set(),
    # Bulgarian: names (Bulgaria, Russia, John), abbreviations.
    "bg": {
        "бг", "българия", "джон", "русия", "сащ",
    },
    # Macedonian: nothing found in the top ranks.
    "mk": set(),
    # Serbian: English words.
    "sr": {
        "the", "тхе",
    },
    # Croatian: an English word and an abbreviation.
    "hr": {
        "g.", "the",
    },
}

# ---- Reviewed group D ----
# Fixes for pl, cs, sk, sl, lt, lv, et frequency lists, to paste into OVERRIDES and EXCLUDE in
# tools/build_frequency_list.py.

_OVERRIDES_D = {
    # Polish: the dictionary files aspect partners as forms of each other, so every imperfective verb was
    # counted for its perfective (mówić -> powiedzieć, robić -> zrobić) or the reverse; they keep themselves.
    # on/ona/oni/one/my were chained into one word; ta/tą/tę/tej went to 'tam'; 'jest', 'miał', 'musi', 'wiesz'
    # stood alone; 'i' and 'we' were dropped; comparatives with no entry ('lepsze') were dropped.
    "pl": {
        "brać": ["brać"], "bywać": ["bywać"], "dawać": ["dawać"], "decydować": ["decydować"],
        "dochodzić": ["dochodzić"], "dodawać": ["dodawać"], "dojść": ["dojść"], "dostawać": ["dostawać"],
        "dowiadywać": ["dowiadywać"], "dzielić": ["dzielić"], "dziękować": ["dziękować"], "jadać": ["jadać"],
        "kończyć": ["kończyć"], "kupować": ["kupować"], "mówić": ["mówić"], "napisać": ["napisać"],
        "nazywać": ["nazywać"], "niszczyć": ["niszczyć"], "obejrzeć": ["obejrzeć"], "odbywać": ["odbywać"],
        "oddawać": ["oddawać"], "odpowiadać": ["odpowiadać"], "okazywać": ["okazywać"],
        "organizować": ["organizować"], "osiągać": ["osiągać"], "otrzymywać": ["otrzymywać"],
        "otwierać": ["otwierać"], "oznaczyć": ["oznaczyć"], "poczuć": ["poczuć"], "podawać": ["podawać"],
        "podejmować": ["podejmować"], "pojawiać": ["pojawiać"], "pojechać": ["pojechać"], "pokazywać": ["pokazywać"],
        "policzyć": ["policzyć"], "pomagać": ["pomagać"], "pomyśleć": ["pomyśleć"], "poprosić": ["poprosić"],
        "porozmawiać": ["porozmawiać"], "postarać": ["postarać"], "posłuchać": ["posłuchać"],
        "powodować": ["powodować"], "poznawać": ["poznawać"], "pozostawać": ["pozostawać"], "pozwalać": ["pozwalać"],
        "przechodzić": ["przechodzić"], "przedstawiać": ["przedstawiać"], "przekonywać": ["przekonywać"],
        "przeprowadzać": ["przeprowadzać"], "przepraszać": ["przepraszać"], "przestawać": ["przestawać"],
        "przetłumaczyć": ["przetłumaczyć"], "przychodzić": ["przychodzić"], "przygotowywać": ["przygotowywać"],
        "przyjmować": ["przyjmować"], "przypominać": ["przypominać"], "przyznawać": ["przyznawać"],
        "próbować": ["próbować"], "pójść": ["pójść"], "płacić": ["płacić"], "robić": ["robić"],
        "rozpoczynać": ["rozpoczynać"], "rozumieć": ["rozumieć"], "skorzystać": ["skorzystać"],
        "składać": ["składać"], "spotykać": ["spotykać"], "sprawdzać": ["sprawdzać"], "sprawiać": ["sprawiać"],
        "stanąć": ["stanąć"], "stawać": ["stawać"], "stawiać": ["stawiać"], "stawić": ["stawić"],
        "stwarzać": ["stwarzać"], "słyszeć": ["słyszeć"], "tracić": ["tracić"], "trafiać": ["trafiać"],
        "tworzyć": ["tworzyć"], "tłumaczyć": ["tłumaczyć"], "udać": ["udać"], "utracić": ["utracić"],
        "utworzyć": ["utworzyć"], "uwierzyć": ["uwierzyć"], "uznawać": ["uznawać"], "użyć": ["użyć"],
        "wchodzić": ["wchodzić"], "widywać": ["widywać"], "widzieć": ["widzieć"], "wprowadzać": ["wprowadzać"],
        "wracać": ["wracać"], "wrzeć": ["wrzeć"], "wspominać": ["wspominać"], "wybierać": ["wybierać"],
        "wybudować": ["wybudować"], "wychodzić": ["wychodzić"], "wydać": ["wydać"], "wygrywać": ["wygrywać"],
        "wyjrzeć": ["wyjrzeć"], "wykonywać": ["wykonywać"], "wykorzystywać": ["wykorzystywać"],
        "wystarczać": ["wystarczać"], "występować": ["występować"], "wysyłać": ["wysyłać"], "zabijać": ["zabijać"],
        "zaczynać": ["zaczynać"], "zagrać": ["zagrać"], "zajmować": ["zajmować"], "zakładać": ["zakładać"],
        "zamykać": ["zamykać"], "zapominać": ["zapominać"], "zastanawiać": ["zastanawiać"],
        "zastosować": ["zastosować"], "zawrzeć": ["zawrzeć"], "zbierać": ["zbierać"], "zbudować": ["zbudować"],
        "zdobywać": ["zdobywać"], "zgadzać": ["zgadzać"], "zjadać": ["zjadać"], "zjeść": ["zjeść"],
        "zmieniać": ["zmieniać"], "znajdować": ["znajdować"], "zostawać": ["zostawać"], "łączyć": ["łączyć"],
        "żyć": ["żyć"], "on": ["on"], "ona": ["ona"], "oni": ["oni"], "one": ["one"], "my": ["my"], "jego": ["on"],
        "go": ["on"], "mu": ["on"], "jemu": ["on"], "niego": ["on"], "niemu": ["on"], "nim": ["on", "oni"],
        "jej": ["ona"], "ją": ["ona"], "nią": ["ona"], "niej": ["ona"], "ich": ["oni"], "im": ["oni"],
        "nich": ["oni"], "nimi": ["oni"], "nas": ["my"], "nam": ["my"], "nami": ["my"], "mi": ["ja"], "ta": ["ten"],
        "tą": ["ten"], "tę": ["ten"], "tej": ["ten"], "tam": ["tam"], "jest": ["być"], "sa": ["być"],
        "miał": ["mieć"], "miałem": ["mieć"], "miały": ["mieć"], "ma": ["mieć"], "mam": ["mieć"], "mamy": ["mieć"],
        "musi": ["musieć"], "wiesz": ["wiedzieć"], "należy": ["należeć"], "znaczy": ["znaczyć"], "dzieje": ["dziać"],
        "działa": ["działać"], "stanie": ["stać"], "dnia": ["dzień"], "stanowi": ["stanowić"], "podoba": ["podobać"],
        "dużej": ["duży"], "leć": ["lecieć"], "sie": ["się"], "cos": ["coś"], "dane": ["dane"], "danych": ["dane"],
        "udało": ["udać"], "uda": ["udać"], "udał": ["udać"], "udaje": ["udać", "udawać"], "droga": ["droga"],
        "drogi": ["droga", "drogi"], "druga": ["drugi"], "drugiej": ["drugi"], "wino": ["wino"], "rana": ["rana"],
        "ran": ["rana"], "ranny": ["ranny"], "lis": ["lis"], "pora": ["pora"], "pory": ["pora"], "pani": ["pani"],
        "panią": ["pani"], "i": ["i"], "we": ["w"], "lepsze": ["dobry"], "lepsza": ["dobry"], "lepszego": ["dobry"],
        "lepszym": ["dobry"], "najlepsza": ["dobry"], "najlepszego": ["dobry"], "najlepszą": ["dobry"],
        "większe": ["duży"], "większą": ["duży"], "większej": ["duży"], "większa": ["duży"], "większych": ["duży"],
        "większego": ["duży"], "większym": ["duży"], "największą": ["duży"], "mniejsze": ["mały"], "gorsze": ["zły"],
        "dobrze": ["dobrze"], "lepiej": ["dobrze"], "najlepiej": ["dobrze"], "mało": ["mało"], "mniej": ["mało"],
        "najmniej": ["mało"], "bardziej": ["bardzo"], "najbardziej": ["bardzo"], "razem": ["razem"],
        "kiedyś": ["kiedyś"], "prawie": ["prawie"], "czasami": ["czasami"],
    },
    # Czech: the same aspect-partner rows (říkat -> říct, chodit -> jít) are undone; 'je' (is) was counted
    # for ona/oni/ono; the plural pronoun cases for 'onen'; 'může'... for the noun 'moc'; 'let' (years) for
    # 'léto'; negated forms (není, nemá, nechci) were dropped because 'nebýt'/'nemít' have no entry. 'ty' and
    # its cases were chained into 'ten'; zlý, nos, volat, věda, dav, králík, topit and brána sat in look-alike verbs
    # and nouns (zlo, nosit, vole, vědět, dát, král, utopit, brát).
    "cs": {
        "brát": ["brát"], "chodit": ["chodit"], "chápat": ["chápat"], "dojit": ["dojit"], "dávat": ["dávat"],
        "dívat": ["dívat"], "děkovat": ["děkovat"], "hrávat": ["hrávat"], "jezdit": ["jezdit"], "končit": ["končit"],
        "kupovat": ["kupovat"], "měnit": ["měnit"], "nabízet": ["nabízet"], "nacházet": ["nacházet"],
        "napsat": ["napsat"], "odcházet": ["odcházet"], "odpovídat": ["odpovídat"], "otevírat": ["otevírat"],
        "otvírat": ["otvírat"], "pobavit": ["pobavit"], "podporovat": ["podporovat"], "podržet": ["podržet"],
        "pokročit": ["pokročit"], "pomáhat": ["pomáhat"], "posílat": ["posílat"], "používat": ["používat"],
        "poznávat": ["poznávat"], "počkat": ["počkat"], "proběhnout": ["proběhnout"], "procházet": ["procházet"],
        "prodávat": ["prodávat"], "prosit": ["prosit"], "ptát": ["ptát"], "představovat": ["představovat"],
        "přečíst": ["přečíst"], "přicházet": ["přicházet"], "připravovat": ["připravovat"], "slýchat": ["slýchat"],
        "sníst": ["sníst"], "spojovat": ["spojovat"], "stavět": ["stavět"], "stvořit": ["stvořit"],
        "stávat": ["stávat"], "udělat": ["udělat"], "ukazovat": ["ukazovat"], "uslyšet": ["uslyšet"],
        "uvidět": ["uvidět"], "uvěřit": ["uvěřit"], "učit": ["učit"], "vracet": ["vracet"], "vybírat": ["vybírat"],
        "vydávat": ["vydávat"], "vyhledat": ["vyhledat"], "vyhledávat": ["vyhledávat"], "vyměnit": ["vyměnit"],
        "vypadnout": ["vypadnout"], "vyrobit": ["vyrobit"], "vytvářet": ["vytvářet"], "vyřešit": ["vyřešit"],
        "vídat": ["vídat"], "zabíjet": ["zabíjet"], "zajmout": ["zajmout"], "zamilovat": ["zamilovat"],
        "zaplatit": ["zaplatit"], "zkoušet": ["zkoušet"], "ztrácet": ["ztrácet"], "zvolit": ["zvolit"],
        "čítat": ["čítat"], "říkat": ["říkat"], "on": ["on"], "ona": ["ona"], "ono": ["ono"], "oni": ["oni"],
        "ony": ["ony"], "je": ["být"], "jí": ["ona"], "ho": ["on"], "mu": ["on"], "něj": ["on"], "jej": ["on"],
        "něm": ["on"], "ním": ["on"], "jim": ["oni"], "nich": ["oni"], "jich": ["oni"], "nimi": ["oni"],
        "ně": ["oni"], "nim": ["oni"], "jimi": ["oni"], "není": ["být"], "nebude": ["být"], "nejsou": ["být"],
        "nebyl": ["být"], "nebylo": ["být"], "nejsem": ["být"], "nebyla": ["být"], "nejsi": ["být"],
        "nejsme": ["být"], "nejste": ["být"], "má": ["mít"], "mám": ["mít"], "nemá": ["mít"], "nemám": ["mít"],
        "neměl": ["mít"], "neměla": ["mít"], "neměli": ["mít"], "nemáš": ["mít"], "nemělo": ["mít"],
        "nechci": ["chtít"], "moct": ["moct"], "moci": ["moct"], "pomoci": ["pomoct", "pomoc"],
        "nepomůže": ["pomoct"], "nepomohl": ["pomoct"], "nepomohlo": ["pomoct"], "nepomohla": ["pomoct"],
        "nepomohou": ["pomoct"], "pán": ["pán"], "pan": ["pán"], "pana": ["pán"], "panu": ["pán"], "let": ["rok"],
        "letech": ["rok"], "lety": ["rok"], "stav": ["stav"], "stavu": ["stav"], "brány": ["brána"],
        "branky": ["branka"], "boje": ["boj"], "kolo": ["kolo"], "kola": ["kolo"], "pěna": ["pěna"], "pěny": ["pěna"],
        "rána": ["rána"], "ránu": ["rána"], "rány": ["rána"], "hodin": ["hodina"], "hodinách": ["hodina"],
        "hodinami": ["hodina"], "zlatý": ["zlatý"], "zbytečný": ["zbytečný"], "hlavně": ["hlavně"],
        "vlastně": ["vlastně"], "jednou": ["jednou"], "příště": ["příště"], "i": ["i"], "li": ["li"],
        "hele": ["hele"], "ty": ["ty", "ten"], "ti": ["ty", "ten"], "tebe": ["ty"], "tě": ["ty"], "tobě": ["ty"],
        "tebou": ["ty"], "tu": ["tu", "ten"], "vám": ["vy"], "vámi": ["vy"], "zavolat": ["zavolat"],
        **{f: ["zlý"] for f in "zle zlou zlá zlé zlého zlém zlému zlí zlý zlých zlým zlými".split()},
        **{f: ["nos"] for f in "nos nose nosem nosu nosy nosík nosů".split()},
        **{f: ["volat"] for f in "volají volající volal volala volali volalo volaly volaný volat volej volejte "
           "volá volám voláme voláno voláte voláš".split()},
        **{f: ["věda"] for f in "věd věda vědou vědu vědy vědách vědě".split()},
        **{f: ["vědecký"] for f in "vědeckou vědecky vědecká vědecké vědeckého vědeckém vědecký vědeckých "
           "vědeckým".split()},
        **{f: ["dav"] for f in "dav davem davu davy".split()},
        **{f: ["králík"] for f in "králičí králíci králík králíka králíkem králíky králíků králíček".split()},
        **{f: ["topit"] for f in "topil topila topili topilo topit topí topící topím topíme topíte".split()},
        **{f: ["brána"] for f in "bran branami branou brána bránou bránu bráně".split()},
        **{f: ["branka"] for f in "brance branek branka brankami brankou branku".split()},
    },
    # Slovak: simplemma's negated lemmas (nechcieť, nemať, nemôcť...) go to the positive verb; 'ona' and
    # the plural were counted for 'on'; 'je' for 'jesť'; aspect partners split as in Polish; common
    # invariable words unknown to both dictionary and simplemma (v, k, nikdy, vždy, sem...) were dropped.
    "sk": {
        "urobiť": ["urobiť"], "brať": ["brať"], "písať": ["písať"], "meniť": ["meniť"], "predávať": ["predávať"],
        "vracať": ["vracať"], "predstavovať": ["predstavovať"], "dávať": ["dávať"], "bývať": ["bývať"],
        "zameniť": ["zameniť"], "utvoriť": ["utvoriť"], "sčítať": ["sčítať"], "pozrieť": ["pozrieť"],
        "naučiť": ["naučiť"], "poďakovať": ["poďakovať"], "ona": ["ona"], "oni": ["oni"], "jej": ["ona"],
        "ju": ["ona"], "ňu": ["ona"], "nej": ["ona"], "ňou": ["ona"], "ich": ["oni"], "im": ["oni"], "nich": ["oni"],
        "je": ["byť"], "mám": ["mať"], "nemá": ["mať"], "nechcieť": ["chcieť"], "nemať": ["mať"], "nemôcť": ["môcť"],
        "nemusieť": ["musieť"], "nebyť": ["byť"], "nevedieť": ["vedieť"], "nemyslieť": ["myslieť"], "nedať": ["dať"],
        "nehovoriť": ["hovoriť"], "nedokázať": ["dokázať"], "nebáť": ["báť"], "nevidieť": ["vidieť"],
        "nepovedať": ["povedať"], "nepotrebovať": ["potrebovať"], "neveriť": ["veriť"], "nedostať": ["dostať"],
        "nesmieť": ["smieť"], "nepoznať": ["poznať"], "neprísť": ["prísť"], "nenájsť": ["nájsť"],
        "neexistovať": ["existovať"], "nenechať": ["nechať"], "nechápať": ["chápať"], "nevadiť": ["vadiť"],
        "nepočuť": ["počuť"], "netušiť": ["tušiť"], "nepáčiť": ["páčiť"], "neznamenať": ["znamenať"],
        "necítiť": ["cítiť"], "nechodiť": ["chodiť"], "nejsť": ["ísť"], "nerob": ["robiť"],
        "nerozumiem": ["rozumieť"], "aby": ["aby"], "pani": ["pani"], "deje": ["diať"], "stane": ["stať"],
        "nos": ["nos"], "nosa": ["nos"], "nosom": ["nos"], "inak": ["inak"], "ináč": ["inak"], "so": ["s"],
        "možné": ["možný"], "pozrite": ["pozrieť"], "vyššie": ["vysoký"], "nižšie": ["nízky"], "v": ["v"], "k": ["k"],
        "i": ["i"], "u": ["u"], "no": ["no"], "nikdy": ["nikdy"], "vždy": ["vždy"], "sem": ["sem"],
        "takto": ["takto"], "niekedy": ["niekedy"], "predtým": ["predtým"], "kam": ["kam"], "niekde": ["niekde"],
        "najprv": ["najprv"], "odtiaľto": ["odtiaľto"], "odkiaľ": ["odkiaľ"], "odtiaľ": ["odtiaľ"],
        "nejako": ["nejako"], "kedysi": ["kedysi"], "prvýkrát": ["prvýkrát"],
    },
    # Slovene: personal pronouns were tangled (ti/vas/vam under 'mi', je/jih/jim under 'ona'); 'je', 'sem'
    # and 'ni'/'nisem' are 'biti'; forms whose dictionary lemma has stress marks ('bíti') were dropped;
    # frequent adverbs unknown to simplemma (samo, treba, naprej, najprej...) were dropped.
    "sl": {
        "jaz": ["jaz"], "ti": ["ti"], "on": ["on"], "ona": ["ona"], "oni": ["oni"], "vi": ["vi"], "me": ["jaz"],
        "mene": ["jaz"], "meni": ["jaz"], "mano": ["jaz"], "zame": ["jaz"], "name": ["jaz"], "mi": ["mi", "jaz"],
        "tebe": ["ti"], "tebi": ["ti"], "tabo": ["ti"], "zate": ["ti"], "nate": ["ti"], "te": ["ti", "ta"],
        "nas": ["mi"], "nam": ["mi"], "nami": ["mi"], "vas": ["vi"], "vam": ["vi"], "vami": ["vi"], "ga": ["on"],
        "mu": ["on"], "njega": ["on"], "njemu": ["on"], "zanj": ["on"], "nanj": ["on"], "njim": ["on", "oni"],
        "jo": ["ona"], "ji": ["ona"], "njo": ["ona"], "njej": ["ona"], "zanjo": ["ona"], "nanjo": ["ona"],
        "jih": ["oni"], "jim": ["oni"], "njih": ["oni"], "ju": ["oni"], "zanje": ["oni"], "nanje": ["oni"],
        "je": ["biti"], "sem": ["biti"], "ni": ["biti"], "nisem": ["biti"], "niso": ["biti"], "nisi": ["biti"],
        "nismo": ["biti"], "niste": ["biti"], "nista": ["biti"], "nisva": ["biti"], "bodi": ["biti"],
        "bodite": ["biti"], "si": ["biti", "se"], "ve": ["vedeti"], "vedel": ["vedeti"], "vedla": ["vedeti"],
        "vedli": ["vedeti"], "hotel": ["hoteti"], "hotela": ["hoteti"], "nočem": ["hoteti"], "ko": ["ko"],
        "prav": ["prav"], "prej": ["prej"], "niti": ["niti"], "pravi": ["praviti", "pravi"], "pravim": ["praviti"],
        "dela": ["delati", "delo"], "let": ["leto"], "prosim": ["prositi"], "oprostite": ["oprostiti"],
        "roka": ["roka"], "dve": ["dva"], "ena": ["en"], "eden": ["en"], "tri": ["trije"], "tisto": ["tisti"],
        "sprejeti": ["sprejeti"], "sprejet": ["sprejeti"], "zadeva": ["zadeva"], "zadeve": ["zadeva"],
        "zadevo": ["zadeva"], "zadevi": ["zadeva"], "zadevah": ["zadeva"], "zadev": ["zadeva"], "več": ["veliko"],
        "največ": ["veliko"], "pridi": ["priti"], "more": ["moči"], "živi": ["živeti"], "znam": ["znati"],
        "bolje": ["dobro"], "najbolje": ["dobro"], "boljše": ["dober"], "najboljše": ["dober"], "večji": ["velik"],
        "največji": ["velik"], "manjši": ["majhen"], "starejši": ["star"], "mlajši": ["mlad"], "višji": ["visok"],
        "pozneje": ["pozno"], "hitreje": ["hitro"], "sama": ["sam"], "sami": ["sam"], "same": ["sam"],
        "padel": ["pasti"], "padla": ["pasti"], "dobi": ["dobiti"], "samo": ["samo"], "k": ["k"], "no": ["no"],
        "treba": ["treba"], "hej": ["hej"], "naprej": ["naprej"], "najprej": ["najprej"], "narobe": ["narobe"],
        "drugače": ["drugače"], "prvič": ["prvič"], "medtem": ["medtem"], "precej": ["precej"], "tja": ["tja"],
        "dolgo": ["dolgo"], "malce": ["malce"], "tokrat": ["tokrat"], "nekoč": ["nekoč"], "zagotovo": ["zagotovo"],
        "zares": ["zares"], "povsem": ["povsem"], "znova": ["znova"], "večinoma": ["večinoma"],
    },
    # Lithuanian: 'tas' and its forms were counted for 'šis'; 'dukra' for 'sūnus'; superlatives for the rare
    # 'didus'; forms whose dictionary lemma has stress marks ('výras', 'kẽlias') were dropped, as were negated
    # verb forms simplemma does not know (nėra, neturiu, nežinau) and a few particles.
    "lt": {
        "tas": ["tas"], "tą": ["tas"], "tų": ["tas"], "to": ["tas"], "tam": ["tas"], "tuo": ["tas"], "tie": ["tas"],
        "tuos": ["tas"], "tame": ["tas"], "tiems": ["tas"], "jų": ["jie"], "didus": ["didelis"], "labiau": ["labai"],
        "labiausiai": ["labai"], "žaidimas": ["žaidimas"], "žaidimų": ["žaidimas"], "žaidimo": ["žaidimas"],
        "žaidimą": ["žaidimas"], "dukra": ["dukra"], "dukros": ["dukra"], "dukrą": ["dukra"], "vakarė": ["vakaras"],
        "narė": ["narys"], "ilgus": ["ilgas"], "kartus": ["kartas"], "kartų": ["kartas"], "veikus": ["veikti"],
        "muzikas": ["muzika"], "kovos": ["kova"], "tarsi": ["tarsi"], "prisimisti": ["prisiminti"], "salė": ["salė"],
        "kuriame": ["kuris"], "dėmesio": ["dėmesys"], "rūpintis": ["rūpintis"], "rūpinasi": ["rūpintis"],
        "rūpinosi": ["rūpintis"], "nenorti": ["norėti"], "norti": ["norėti"], "nenorėjau": ["norėti"],
        "nenorėjo": ["norėti"], "nėra": ["būti"], "nebūtų": ["būti"], "nesu": ["būti"], "nebuvau": ["būti"],
        "nebūna": ["būti"], "nebūčiau": ["būti"], "neturiu": ["turėti"], "neturėjo": ["turėti"],
        "neturėtų": ["turėti"], "neturime": ["turėti"], "neturėjau": ["turėti"], "neturite": ["turėti"],
        "nesuprantu": ["suprasti"], "nesupratau": ["suprasti"], "nesupranti": ["suprasti"],
        "nesupranta": ["suprasti"], "nežino": ["žinoti"], "nežinojau": ["žinoti"], "nežinai": ["žinoti"],
        "nežinome": ["žinoti"], "nereikėtų": ["reikėti"], "nereikėjo": ["reikėti"], "nereikės": ["reikėti"],
        "nemačiau": ["matyti"], "nematau": ["matyti"], "matyt": ["matyti"], "nemanau": ["manyti"],
        "nepatinka": ["patikti"], "nereiškia": ["reikšti"], "nerūpi": ["rūpėti"], "nesakyk": ["sakyti"],
        "nedaryk": ["daryti"], "netikiu": ["tikėti"], "netinka": ["tikti"], "metus": ["metai"], "metais": ["metai"],
        "metu": ["metas"], "laiko": ["laikas"], "darbo": ["darbas"], "miesto": ["miestas"], "mieste": ["miestas"],
        "geriau": ["gerai"], "geriausiai": ["gerai"], "aišku": ["aiškus"], "vandens": ["vanduo"],
        "vandenį": ["vanduo"], "vandeniu": ["vanduo"], "vandenyje": ["vanduo"], "kokia": ["koks"],
        "sporto": ["sportas"], "centro": ["centras"], "centre": ["centras"], "gera": ["geras"], "kelio": ["kelias"],
        "kelią": ["kelias"], "kelyje": ["kelias"], "karo": ["karas"], "karą": ["karas"], "duris": ["durys"],
        "durų": ["durys"], "meno": ["menas"], "vyrai": ["vyras"], "vyrų": ["vyras"], "vyro": ["vyras"],
        "vyrą": ["vyras"], "vyru": ["vyras"], "vyrui": ["vyras"], "vyrams": ["vyras"], "energijos": ["energija"],
        "energiją": ["energija"], "formos": ["forma"], "formų": ["forma"], "formą": ["forma"], "tautos": ["tauta"],
        "tautų": ["tauta"], "ryto": ["rytas"], "moters": ["moteris"], "vakaro": ["vakaras"], "vakarus": ["vakaras"],
        "dingo": ["dingti"], "laimės": ["laimė"], "motinos": ["motina"], "laikosi": ["laikytis"], "bijo": ["bijoti"],
        "šunį": ["šuo"], "šunys": ["šuo"], "šuns": ["šuo"], "šunų": ["šuo"], "narius": ["narys"], "eime": ["eiti"],
        "niekad": ["niekada"], "trūksta": ["trūkti"], "na": ["na"], "ei": ["ei"], "tad": ["tad"], "šiek": ["šiek"],
        "gaila": ["gaila"], "iškart": ["iškart"], "visgi": ["visgi"], "kuomet": ["kuomet"], "nedaug": ["nedaug"],
        "nagi": ["nagi"],
    },
    # Latvian: 'ir', 'bija', 'būs', 'esmu', 'nav'... have entries of their own and kept their counts apart
    # from 'būt'; 'šis' and its forms were counted for 'tas'; man/tev stood apart from es/tu; frequent
    # adverbs unknown to simplemma (savukārt, iepriekš, vispār...) were dropped.
    "lv": {
        "šis": ["šis"], "ir": ["būt"], "bija": ["būt"], "būs": ["būt"], "esmu": ["būt"], "esi": ["būt"],
        "nav": ["būt"], "nebija": ["būt"], "neesmu": ["būt"], "nebūs": ["būt"], "neesi": ["būt"], "man": ["es"],
        "tev": ["tu"], "sev": ["sevi"], "var": ["varēt"], "varam": ["varēt"], "domāju": ["domāt"], "domā": ["domāt"],
        "runā": ["runāt"], "vēlas": ["vēlēties"], "vēlos": ["vēlēties"], "nevēlos": ["vēlēties"],
        "nenotiks": ["notikt"], "neinteresē": ["interesēt"], "laikā": ["laiks"], "vietā": ["vieta"],
        "dzīvi": ["dzīve"], "lietas": ["lieta"], "skolotāja": ["skolotājs"], "pirmā": ["pirmais"], "rīts": ["rīts"],
        "rīta": ["rīts"], "rītu": ["rīts"], "rītam": ["rīts"], "nākamais": ["nākamais"], "nākamā": ["nākamais"],
        "nākamo": ["nākamais"], "nākamajā": ["nākamais"], "kundze": ["kundze"], "kundzi": ["kundze"],
        "kundzes": ["kundze"], "savukārt": ["savukārt"], "iepriekš": ["iepriekš"], "vispār": ["vispār"],
        "šoreiz": ["šoreiz"], "uzreiz": ["uzreiz"], "vispirms": ["vispirms"], "tādējādi": ["tādējādi"],
        "pietiekami": ["pietiekami"], "apkārt": ["apkārt"], "tostarp": ["tostarp"], "nesen": ["nesen"],
        "nereti": ["nereti"], "netālu": ["netālu"],
    },
    # Estonian: pronoun case forms (mu, mind, mul, sind, teda, neid...) are tied to more than 6 words in
    # the dictionary and were dropped; ma/sa/ta/me/te/nad were separate from mina/sina/tema/meie/teie/nemad;
    # verb forms went to noun homographs (tuli, sai, jää, või); postpositions ('postp') were not counted.
    "et": {
        "ma": ["mina"], "mina": ["mina"], "mu": ["mina"], "minu": ["mina"], "mind": ["mina"], "mul": ["mina"],
        "minul": ["mina"], "mulle": ["mina"], "minule": ["mina"], "minult": ["mina"], "minust": ["mina"],
        "minuga": ["mina"], "minus": ["mina"], "sa": ["sina"], "sina": ["sina"], "su": ["sina"], "sinu": ["sina"],
        "sind": ["sina"], "sul": ["sina"], "sinul": ["sina"], "sulle": ["sina"], "sinule": ["sina"],
        "sinult": ["sina"], "sinust": ["sina"], "sinuga": ["sina"], "sinus": ["sina"], "ta": ["tema"],
        "tema": ["tema"], "teda": ["tema"], "tal": ["tema"], "temal": ["tema"], "talle": ["tema"], "temale": ["tema"],
        "temalt": ["tema"], "temast": ["tema"], "temaga": ["tema"], "me": ["meie"], "meie": ["meie"],
        "meid": ["meie"], "meil": ["meie"], "meile": ["meie"], "meilt": ["meie"], "meist": ["meie"],
        "meiega": ["meie"], "te": ["teie"], "teie": ["teie"], "teid": ["teie"], "teil": ["teie"], "teile": ["teie"],
        "teilt": ["teie"], "teiega": ["teie"], "nad": ["nemad"], "nemad": ["nemad"], "neid": ["nemad"],
        "neil": ["nemad"], "neile": ["nemad"], "neilt": ["nemad"], "neist": ["nemad"], "nende": ["nemad"],
        "nendega": ["nemad"], "nendest": ["nemad"], "teist": ["teine", "teie"], "enda": ["ise"], "enese": ["ise"],
        "pole": ["olema"], "polnud": ["olema"], "võid": ["võima"], "võiks": ["võima"], "võis": ["võima"],
        "võisid": ["võima"], "võita": ["võitma"], "peaks": ["pidama"], "pead": ["pidama", "pea"], "tule": ["tulema"],
        "tuli": ["tulema"], "tuleks": ["tulema"], "tuled": ["tulema"], "sai": ["saama"], "saada": ["saama"],
        "taha": ["tahtma"], "jää": ["jääma"], "jääd": ["jääma"], "jäid": ["jääma"], "jääks": ["jääma"],
        "muuta": ["muutma"], "loota": ["lootma"], "loed": ["lugema"], "kuule": ["kuulma"], "kuulis": ["kuulma"],
        "usu": ["uskuma"], "lõi": ["lööma"], "tegemist": ["tegema"], "kaotama": ["kaotama"],
        "liigutama": ["liigutama"], "kasvatama": ["kasvatama"], "põletama": ["põletama"], "tekitama": ["tekitama"],
        "soovitama": ["soovitama"], "teatama": ["teatama"], "kuulutama": ["kuulutama"], "all": ["all"],
        "ees": ["ees"], "peal": ["peal"], "sisse": ["sisse"], "jaoks": ["jaoks"], "jooksul": ["jooksul"],
        "tõttu": ["tõttu"], "kohta": ["kohta"], "puhul": ["puhul"], "no": ["no"], "öösel": ["öösel"],
        "pärit": ["pärit"],
    },
}

_EXCLUDE_D = {
    # English words, abbreviations, and the names Warszawa and Ukraina.
    "pl": {"and", "best", "de", "just", "la", "lo", "np", "ok", "ukraina", "warszawa"},
    # 'ok' (sent to 'oko'), and Praha, whose forms were counted for 'práh'.
    "cs": {"ok", "praha", "prahou", "prahu", "prahy", "praze"},
    # Abbreviations
    "sk": {"km", "sr"},
    # 'ok' (sent to 'oko'); village forms of 'vas' that would follow it to 'vi'.
    "sl": {"ok", "vaseh", "vasi", "vasjo", "vasmi"},
    # Abbreviations
    "lt": {"mln", "proc", "tūkst"},
    "lv": set(),
    "et": set(),
}

# ---- Reviewed group E ----
_OVERRIDES_E = {
    # Finnish: personal pronouns kept apart (me/te/he were merged into minä/sinä/hän, minä into mikä);
    # lexicalised adverbs/conjunctions/postpositions that the dictionary lists as case forms of a more
    # common word (niin, näin, hyvin, vaan, joten, jälkeen, takaisin, ...) kept as words, and
    # postpositions that were dropped (takia, asti, varten, saakka, muassa); verb infinitives swallowed
    # by a homographic noun (toimia, uskoa, pystyä, koskea, keksiä); verb forms sent to a noun
    # homograph (voi -> voida, tuli -> tulla) and nouns sent to an unrelated word (kulta, kettu, satama).
    "fi": {
        "minä": ["minä"], "minun": ["minä"], "minua": ["minä"], "minulle": ["minä"], "minulla": ["minä"],
        "minusta": ["minä"], "minut": ["minä"], "minulta": ["minä"], "minuun": ["minä"], "minussa": ["minä"],
        "minäkin": ["minä"],
        "me": ["me"], "meidän": ["me"], "meillä": ["me"], "meille": ["me"], "meitä": ["me"], "meistä": ["me"],
        "meihin": ["me"], "meissä": ["me"], "meiltä": ["me"], "meidät": ["me"],
        "sinä": ["sinä"], "sinun": ["sinä"], "sinua": ["sinä"], "sinulle": ["sinä"], "sinulla": ["sinä"],
        "sinusta": ["sinä"], "sinut": ["sinä"], "sinulta": ["sinä"], "sinuun": ["sinä"], "sinussa": ["sinä"],
        "te": ["te"], "teidän": ["te"], "teillä": ["te"], "teille": ["te"], "teitä": ["te"], "teistä": ["te"],
        "teihin": ["te"], "teissä": ["te"], "teiltä": ["te"], "teidät": ["te"],
        "hän": ["hän"], "hänen": ["hän"], "hänet": ["hän"], "häntä": ["hän"], "hänelle": ["hän"], "hänellä": ["hän"],
        "hänestä": ["hän"], "häneen": ["hän"], "hänessä": ["hän"], "häneltä": ["hän"],
        "he": ["he"], "heidän": ["he"], "heitä": ["he"], "heille": ["he"], "heillä": ["he"], "heistä": ["he"],
        "heihin": ["he"], "heissä": ["he"], "heiltä": ["he"], "heidät": ["he"],
        "mitä": ["mikä"],
        "niin": ["niin"], "siis": ["siis"], "siksi": ["siksi"], "näin": ["näin"], "hyvin": ["hyvin"],
        "vaan": ["vaan"], "joten": ["joten"], "jotta": ["jotta"], "joskus": ["joskus"], "yhä": ["yhä"],
        "erittäin": ["erittäin"], "miksi": ["miksi"], "milloin": ["milloin"], "jälkeen": ["jälkeen"],
        "takaisin": ["takaisin"], "takana": ["takana"], "taakse": ["taakse"], "takaa": ["takaa"],
        "edessä": ["edessä"], "eteen": ["eteen"], "edellä": ["edellä"], "esiin": ["esiin"], "esille": ["esille"],
        "esillä": ["esillä"], "etenkin": ["etenkin"], "poikki": ["poikki"], "läsnä": ["läsnä"],
        "takia": ["takia"], "asti": ["asti"], "varten": ["varten"], "saakka": ["saakka"], "muassa": ["muassa"],
        "kuitenkaan": ["kuitenkin"], "verran": ["verran"], "verta": ["veri"], "vertaa": ["verrata"],
        "elämä": ["elämä"], "elämän": ["elämä"], "elämää": ["elämä"], "elämään": ["elämä"], "elämässä": ["elämä"],
        "elämästä": ["elämä"], "elämäni": ["elämä"], "elämänsä": ["elämä"],
        "kansa": ["kansa"], "kansan": ["kansa"], "kansaa": ["kansa"],
        "voima": ["voima"], "voimaan": ["voima"], "voimassa": ["voima"],
        "toimia": ["toimia"], "toimi": ["toimia"], "uskoa": ["uskoa"], "uskon": ["uskoa"], "uskot": ["uskoa"],
        "uskoin": ["uskoa"], "uskomme": ["uskoa"], "pystyä": ["pystyä"], "pysty": ["pystyä"],
        "koskea": ["koskea"], "keksiä": ["keksiä"],
        "voi": ["voida"], "voin": ["voida"], "voit": ["voida"], "voimme": ["voida"], "voiko": ["voida"],
        "voisi": ["voida"], "voihan": ["voida"], "voivat": ["voida"],
        "tuli": ["tulla"], "tulin": ["tulla"], "tulen": ["tulla"], "tulet": ["tulla"],
        "halusi": ["haluta"], "maan": ["maa"], "kertaa": ["kerta"], "tuota": ["tuo"], "totta": ["tosi"],
        "päässä": ["pää"], "päähän": ["pää"],
        "kulta": ["kulta"], "kultaa": ["kulta"], "kettu": ["kettu"], "ketun": ["kettu"],
        "satama": ["satama"], "satamassa": ["satama"], "satamaan": ["satama"],
        "esim": ["esimerkiksi"],
    },
    # Hungarian: vagy ("or") was swallowed by van; postpositions were dropped (szerint, után, között,
    # ... - see the WORD_POS note) or headed by a personal form (szerinte); case-suffix forms of personal
    # pronouns (nekem, veled, tőlünk, ...) go to the pronoun; possessed nouns listed as their own words
    # (vége, része, neve, ideje, apja, ...) go to the noun; verb forms that were own lines (tett, tette,
    # elment, kérem, hiszem) go to the verb; unrelated words merged by homography split off
    # (hét/hé, kék+kő/kell, állat/áll, párt/pár, hív/hű, megért/megér, folytat/folyik, adat/ad).
    "hu": {
        "vagy": ["vagy"],
        "szerint": ["szerint"], "szerinte": ["szerint"], "után": ["után"], "között": ["között"], "alatt": ["alatt"],
        "miatt": ["miatt"], "által": ["által"], "előtt": ["előtt"], "mellett": ["mellett"], "nélkül": ["nélkül"],
        "során": ["során"], "alapján": ["alapján"], "óta": ["óta"], "közül": ["közül"], "keresztül": ["keresztül"],
        "helyett": ["helyett"], "körül": ["körül"], "követően": ["követően"], "mögött": ["mögött"], "felett": ["felett"],
        "múlva": ["múlva"],
        "őt": ["ő"],
        "nekem": ["én"], "velem": ["én"], "tőlem": ["én"], "rólam": ["én"], "bennem": ["én"], "belőlem": ["én"],
        "nálam": ["én"], "rajtam": ["én"], "rám": ["én"], "hozzám": ["én"], "belém": ["én"],
        "neked": ["te"], "veled": ["te"], "tőled": ["te"], "rólad": ["te"], "benned": ["te"], "belőled": ["te"],
        "nálad": ["te"], "rajtad": ["te"], "rád": ["te"], "hozzád": ["te"], "beléd": ["te"],
        "neki": ["ő"], "vele": ["ő"], "tőle": ["ő"], "róla": ["ő"], "benne": ["ő"], "belőle": ["ő"], "nála": ["ő"],
        "nekünk": ["mi"], "velünk": ["mi"], "tőlünk": ["mi"], "rólunk": ["mi"], "bennünk": ["mi"], "belőlünk": ["mi"],
        "nálunk": ["mi"], "rajtunk": ["mi"], "ránk": ["mi"], "hozzánk": ["mi"], "belénk": ["mi"],
        "nektek": ["ti"], "veletek": ["ti"], "tőletek": ["ti"], "rólatok": ["ti"], "bennetek": ["ti"],
        "belőletek": ["ti"], "nálatok": ["ti"], "rajtatok": ["ti"], "rátok": ["ti"], "hozzátok": ["ti"], "belétek": ["ti"],
        "nekik": ["ők"], "velük": ["ők"], "tőlük": ["ők"], "róluk": ["ők"], "bennük": ["ők"], "belőlük": ["ők"],
        "náluk": ["ők"], "rajtuk": ["ők"], "rájuk": ["ők"], "hozzájuk": ["ők"], "beléjük": ["ők"],
        "vége": ["vég"], "része": ["rész"], "részt": ["rész"], "neve": ["név"], "ideje": ["idő"], "alapja": ["alap"],
        "sora": ["sor"], "száma": ["szám"], "élete": ["élet"], "életem": ["élet"], "éve": ["év"], "napja": ["nap"],
        "helye": ["hely"], "keze": ["kéz"], "feje": ["fej"], "felesége": ["feleség"], "apja": ["apa"], "apám": ["apa"],
        "anyám": ["anya"], "fia": ["fiú"], "fiam": ["fiú"], "barátom": ["barát"], "uram": ["úr"],
        "tett": ["tesz"], "tettem": ["tesz"], "tette": ["tesz"], "tettek": ["tesz"], "elment": ["elmegy"],
        "készült": ["készül"], "kérem": ["kér"], "kérlek": ["kér"], "hiszem": ["hisz"],
        "jobban": ["jól"], "legjobban": ["jól"],
        "hét": ["hét"], "kék": ["kék"], "kő": ["kő"], "állat": ["állat"], "adatok": ["adat"], "párt": ["párt"],
        "pártok": ["párt"], "pártot": ["párt"], "gyerek": ["gyerek"], "hív": ["hív"],
        "megért": ["megért"], "megértem": ["megért"], "megérteni": ["megért"], "megérti": ["megért"],
        "megértette": ["megért"], "megértettem": ["megért"], "megértetted": ["megért"],
        "folytat": ["folytat"], "folytatja": ["folytat"],
    },
    # Turkish: biz/siz/onlar were merged into ben/sen/on, and o's oblique forms (onu, ona, onun) into
    # on "ten"; postpositions dropped or sent to verbs/nouns (için, kadar, göre, üzere, hakkında, ...);
    # bare stems that simplemma guessed for verb forms (al, et, bırak, başla, çek, bitir) go to the
    # -mek/-mak infinitive, and demek forms guessed as the clitic de; unrelated words merged by homography
    # split off (bile/ile, neden/ne, ekip/ekmek, oyun/oy, adam/ada, kara/kere, karı/kar, yazı/yaz,
    # beyin/bey, acı+açı/aç, ana/an, hukuk/hak, gez-/kez, dene/tane).
    "tr": {
        "biz": ["biz"], "bizim": ["biz"], "bize": ["biz"], "bizi": ["biz"], "bizimle": ["biz"], "bizden": ["biz"],
        "bizde": ["biz"], "bizler": ["biz"], "bizleri": ["biz"], "bizlere": ["biz"],
        "siz": ["siz"], "sizin": ["siz"], "size": ["siz"], "sizi": ["siz"], "sizinle": ["siz"], "sizden": ["siz"],
        "sizde": ["siz"], "sizler": ["siz"], "sizleri": ["siz"], "sizlere": ["siz"],
        "onu": ["o"], "ona": ["o"], "onun": ["o"], "onunla": ["o"], "ondan": ["o"], "onda": ["o"], "onsuz": ["o"],
        "onla": ["o"], "onunda": ["o"],
        "onlar": ["onlar"], "onları": ["onlar"], "onlara": ["onlar"], "onların": ["onlar"], "onlardan": ["onlar"],
        "onlarda": ["onlar"], "onlarla": ["onlar"],
        "için": ["için"], "kadar": ["kadar"], "göre": ["göre"], "üzere": ["üzere"], "ait": ["ait"],
        "dolayı": ["dolayı"], "dair": ["dair"], "hakkında": ["hakkında"],
        "bile": ["bile"], "neden": ["neden"], "nedeni": ["neden"], "nedeniyle": ["neden"], "işte": ["işte"],
        "al": ["almak"], "et": ["etmek"], "bırak": ["bırakmak"], "başla": ["başlamak"], "çek": ["çekmek"],
        "bitir": ["bitirmek"], "olur": ["olmak"], "gelen": ["gelmek"], "çıktı": ["çıkmak"], "yapılan": ["yapmak"],
        "demektir": ["demek"], "denir": ["demek"], "denen": ["demek"], "dediğini": ["demek"], "dediğimi": ["demek"],
        "dediğine": ["demek"], "dediklerini": ["demek"], "demeyi": ["demek"], "diyeceğimi": ["demek"],
        "dediğimde": ["demek"], "demeden": ["demek"], "dediğinde": ["demek"], "denebilir": ["demek"],
        "demiştir": ["demek"], "diyenler": ["demek"], "demesi": ["demek"], "demişken": ["demek"],
        "diyeceğini": ["demek"], "dediklerimi": ["demek"], "dediklerine": ["demek"], "demekti": ["demek"],
        "geçmeden": ["geçmek"], "geçtiğini": ["geçmek"], "geçmesi": ["geçmek"], "geçmesini": ["geçmek"],
        "geçtikten": ["geçmek"], "geçmiştir": ["geçmek"],
        "burada": ["bura"], "buradan": ["bura"], "burası": ["bura"], "nerede": ["nere"], "nereden": ["nere"],
        "hala": ["hâlâ"], "tabi": ["tabii"], "tabii": ["tabii"],
        "ekip": ["ekip"], "oyunu": ["oyun"], "oyuna": ["oyun"], "oyunun": ["oyun"], "oyunda": ["oyun"], "oyundan": ["oyun"],
        "adamı": ["adam"], "adamın": ["adam"], "adama": ["adam"], "adayı": ["aday"],
        "ölümü": ["ölüm"], "ölüme": ["ölüm"], "ölümden": ["ölüm"], "ölümün": ["ölüm"], "ölümde": ["ölüm"],
        "kara": ["kara"], "karı": ["karı"], "karım": ["karı"], "karımı": ["karı"], "karımın": ["karı"],
        "karımla": ["karı"], "karıma": ["karı"], "yazı": ["yazı"], "beyin": ["beyin"], "kurum": ["kurum"],
        "acı": ["acı"], "açı": ["açı"], "açısından": ["açı"], "açıdan": ["açı"], "açısı": ["açı"],
        "ana": ["ana"], "hukuk": ["hukuk"], "hukuku": ["hukuk"], "hukukun": ["hukuk"], "hukuka": ["hukuk"],
        "gez": ["gezmek"], "gezmeyi": ["gezmek"], "gezmesi": ["gezmek"], "dene": ["denemek"],
    },
    # Greek: the dictionary ties the articles and personal pronouns to "τα"/"του", so εγώ, εσύ, αυτός,
    # μου, σου, με, σε and the articles all collapsed into one "του" line; articles now go to ο, clitic and
    # strong pronoun forms to their nominative, με/σε stay prepositions and στο/στην/... go to σε. The
    # σ-final keys (τησ, τουσ, μασ, ...) are wordfreq's spelling of final ς (see the note). Also: είναι
    # was its own word, gender pairs merged into the wrong word (πατέρας/μητέρα, παππούς/γιαγιά,
    # βασιλιάς/βασίλισσα), γεια under υγεία, verb forms with their own entries (συμβαίνει, περάσει, ...),
    # neuter/plural forms listed as separate words (παιδιά, όλα, καλό, άλλο, νέο, πολλοί, χρόνια).
    "el": {
        "ο": ["ο"], "η": ["ο"], "το": ["ο"], "του": ["ο"], "της": ["ο"], "τησ": ["ο"], "τον": ["ο"], "την": ["ο"],
        "τη": ["ο"], "των": ["ο"], "τα": ["ο"], "τους": ["ο"], "τουσ": ["ο"], "τις": ["ο"], "τισ": ["ο"], "οι": ["ο"],
        "στο": ["σε"], "στην": ["σε"], "στη": ["σε"], "στον": ["σε"], "στα": ["σε"], "στους": ["σε"], "στουσ": ["σε"],
        "στις": ["σε"], "στισ": ["σε"], "στης": ["σε"], "στησ": ["σε"], "στων": ["σε"],
        "με": ["με"], "σε": ["σε"], "απ": ["από"], "ασ": ["ας"],
        "εγώ": ["εγώ"], "μου": ["εγώ"], "μένα": ["εγώ"], "εμένα": ["εγώ"],
        "εσύ": ["εσύ"], "σου": ["εσύ"], "σένα": ["εσύ"], "εσένα": ["εσύ"],
        "εμείς": ["εμείς"], "εμείσ": ["εμείς"], "μας": ["εμείς"], "μασ": ["εμείς"], "εμάς": ["εμείς"], "εμάσ": ["εμείς"],
        "εσείς": ["εσείς"], "εσείσ": ["εσείς"], "σας": ["εσείς"], "σασ": ["εσείς"], "εσάς": ["εσείς"], "εσάσ": ["εσείς"],
        "αυτός": ["αυτός"], "αυτόσ": ["αυτός"], "αυτή": ["αυτός"], "αυτό": ["αυτός"], "αυτού": ["αυτός"],
        "αυτής": ["αυτός"], "αυτήσ": ["αυτός"], "αυτόν": ["αυτός"], "αυτήν": ["αυτός"], "αυτοί": ["αυτός"],
        "αυτές": ["αυτός"], "αυτέσ": ["αυτός"], "αυτά": ["αυτός"], "αυτών": ["αυτός"], "αυτούς": ["αυτός"],
        "αυτούσ": ["αυτός"],
        "είναι": ["είμαι"], "παιδιά": ["παιδί"], "όλα": ["όλος"], "όλο": ["όλος"], "καλό": ["καλός"],
        "άλλο": ["άλλος"], "νέο": ["νέος"], "πολλοί": ["πολύς"], "χρόνια": ["χρόνος"], "μισό": ["μισός"],
        "χρήματα": ["χρήμα"], "ετών": ["έτος"], "λεπτά": ["λεπτό"],
        "άγιος": ["άγιος"], "άγιο": ["άγιος"],
        "πατέρας": ["πατέρας"], "πατέρα": ["πατέρας"], "πατέρων": ["πατέρας"], "πατεράδες": ["πατέρας"],
        "παππούς": ["παππούς"], "παππού": ["παππούς"], "παππούδες": ["παππούς"],
        "βασιλιάς": ["βασιλιάς"], "βασιλιά": ["βασιλιάς"], "βασιλιάδες": ["βασιλιάς"],
        "αδελφός": ["αδελφός"], "αδερφός": ["αδελφός"],
        "γεια": ["γεια"], "γειά": ["γεια"], "μεν": ["μεν"],
        "συμβαίνει": ["συμβαίνω"], "χρειάζεται": ["χρειάζομαι"], "αφορά": ["αφορώ"], "αξίζει": ["αξίζω"],
        "πειράζει": ["πειράζω"], "περάσει": ["περνάω"], "βρει": ["βρίσκω"], "δώσει": ["δίνω"], "μείνει": ["μένω"],
        "βοηθήσει": ["βοηθάω"],
    },
    # Portuguese (additions): "é" was dropped by the single-letter rule; quer/preciso/olha/acha/amo were
    # own lines instead of their verbs; words merged into an unrelated homograph or neighbour
    # (mar/mal, preto/perto, dona/seu, partido/partir, olho/olhar, comida/comer, câmera/câmara,
    # animar/animal, pressa/para); stray plurals/feminines (melhores, última, milhões).
    "pt": {
        "é": ["ser"], "quis": ["querer"], "quer": ["querer"], "preciso": ["precisar"], "precisa": ["precisar"],
        "olha": ["olhar"], "acha": ["achar"], "amo": ["amar"], "ama": ["amar"], "num": ["em"],
        "mar": ["mar"], "mares": ["mar"], "preto": ["preto"], "preta": ["preto"], "pretos": ["preto"], "pretas": ["preto"],
        "dona": ["dona"], "donas": ["dona"], "partido": ["partido"], "partidos": ["partido"], "partida": ["partida"],
        "partes": ["parte"], "olho": ["olho"], "olhos": ["olho"], "comida": ["comida"], "câmera": ["câmera"],
        "animar": ["animar"], "animais": ["animal"], "pressa": ["pressa"], "dados": ["dados"], "bateria": ["bateria"],
        "melhores": ["melhor"], "última": ["último"], "últimas": ["último"], "milhões": ["milhão"],
    },
}

_EXCLUDE_E = {
    # Finnish: English words, abbreviations and names.
    "fi": {"in", "to", "mm", "jan", "miettinen", "turku", "turun", "turussa", "turkuun", "turusta", "turkua", "tku"},
    # Hungarian: abbreviations, an English word and a bare suffix.
    "hu": {"km", "dr.", "dr", "main", "től"},
    # Turkish: English "in", suffixes split off at apostrophes, ABD and a city name.
    "tr": {"in", "un", "unu", "una", "unda", "unun", "undan", "la", "den", "dan", "nin", "nın", "abd", "adana"},
    # Greek: word fragments that were grouped under "αισώπειος", and an abbreviation.
    "el": {"ου", "ες", "ος", "ων", "κ."},
    # Portuguese (additions): English "my", stray letters.
    "pt": {"my", "x", "xx", "p"},
}


OVERRIDES = {}
EXCLUDE = {}
for _overrides, _exclude in [(_OVERRIDES_A, _EXCLUDE_A), (_OVERRIDES_B, _EXCLUDE_B), (_OVERRIDES_C, _EXCLUDE_C), (_OVERRIDES_D, _EXCLUDE_D), (_OVERRIDES_E, _EXCLUDE_E)]:
    for _language, _fixes in _overrides.items():
        OVERRIDES.setdefault(_language, {}).update(_fixes)
    for _language, _words in _exclude.items():
        EXCLUDE.setdefault(_language, set()).update(_words)
# How a word is written where the dictionary has it in lowercase too: German nouns whose lowercase
# twin is rare or a stray entry ("zeit", "mensch").
DISPLAY = {
    "de": {word.lower(): word for word in (
        "Zeit",
        "Mensch",
        "Frau",
        "Raum",
        "Kraft",
        "Sonne",
        "Feind",
        "Sturm",
        "Wert",
        "Schuld",
        "Rahmen",
        "Spitze",
        "Mittel",
        "Plan",
        "Klasse",
        "Licht",
        "Leinwand",
        "Uni",
        "Wurst",
        "Kapital",
        "Schmuck",
        "Patent",
        "Terminal",
        "Creme",
        "Stier",
        "Gutachten",
        "Abendessen",
        "Mittagessen",
        "Vergnügen",
        "Verbrechen",
        "Unternehmen",
    )},
}

# Found after the Greek final-sigma fix brought in wordfreq's counts.
OVERRIDES["el"]["κάνεις"] = ["κάνω"]
# German ss forms whose ß spelling is another word, which the shared wordfreq count let take them over,
# and past tenses spelled like a noun ("riss", "Riss"), which go to both.
OVERRIDES["de"].update({
    "russe": ["russe"], "russen": ["russe"], "stutzen": ["stutzen"], "weisste": ["wissen"],
    "riss": ["reißen", "riss"], "biss": ["beißen", "biss"], "schloss": ["schließen", "schloss"],
})
# Romanian nouns in -ă the dictionary also lists as verbs in -a ("a pagina"), whose articled form
# ("pagina") is far more common than the bare noun and so headed the line.
for _articled in ("mașina", "pagina", "oferta", "suma", "grupa", "litera", "structura", "cifra", "masca", "taxa", "factura", "norma", "gara", "sămânța", "cota", "axa", "flota", "ancheta"):
    OVERRIDES["ro"][_articled] = [_articled[:-1] + "ă"]
    OVERRIDES["ro"].setdefault(_articled[:-1] + "ă", [_articled[:-1] + "ă"])

# "vir" is also a form of "ver" ("quando eu vir"), which took it and every form of "vir" with it.
OVERRIDES["pt"].update({"vir": ["vir"], "veio": ["vir"], "vinha": ["vir"], "vindo": ["vir"], "vinham": ["vir"]})

# Portuguese's later additions apply on top of its first set.
OVERRIDES["pt"] = {**PORTUGUESE_OVERRIDES["pt"], **OVERRIDES.get("pt", {})}
EXCLUDE["pt"] = PORTUGUESE_EXCLUDE["pt"] | EXCLUDE.get("pt", set())

# Japanese: the polite ending ます is a suffix to the dictionary, whose kana forms it shares with
# 申す and 増す; its forms are its own.
OVERRIDES.setdefault("ja", {}).update({"ます": ["ます"], "まし": ["ます"], "ませ": ["ます"], "ましょ": ["ます"]})
# The tokenizer cuts "知ってる" into 知っ and てる, the spoken いる, whose reading 照る shares.
OVERRIDES["ja"]["てる"] = ["いる"]
# Little words the dictionary also files as the kana of other words: はい "yes" as 這う's stem,
# おい "hey" as 追う's, でし as 弟子 rather than です, ところ as 野老 rather than 所.
OVERRIDES["ja"].update({"はい": ["はい"], "おい": ["おい"], "しか": ["しか"], "よく": ["よく"], "でし": ["です"], "ところ": ["所"]})
OVERRIDES["ja"].update({"うち": ["内"], "こういう": ["こういう"], "ほんと": ["本当"]})
# 九 "nine", whose reading ここの the dictionary also gives この.
OVERRIDES["ja"]["九"] = ["九"]
# Words the dictionary has only as names, which texts use as common words (日本 in 日本人 and 日本語).
OVERRIDES["ja"].update({w: [w] for w in ("日本", "地球", "クリスマス", "オリンピック", "ハロウィン", "欧米")})
# Kanji nouns whose rare readings are commoner kana words: 理由 "reason" (わけ), 友 "friend" (the particle とも).
OVERRIDES["ja"].update({"理由": ["理由"], "友": ["友"]})
# あれ "that", which the dictionary also files as a form of ある; おり, the stem of おる in ております.
OVERRIDES["ja"].update({"あれ": ["あれ"], "おり": ["おる"]})
# Kana spellings and て stems that would go to the wrong word: 行っ is 行く far more than 行う.
OVERRIDES["ja"].update({
    "行っ": ["行く"], "いっ": ["言う", "行く"], "いっぱい": ["一杯"], "かける": ["掛ける"], "かけ": ["掛ける"],
    "かかり": ["掛かる"], "はじめ": ["始め"], "いれる": ["入れる"], "いたる": ["至る"], "しり": ["知る"],
})
# Transitive verbs the dictionary files under their intransitive partner (入れる "put in" under 入る "enter").
OVERRIDES["ja"].update({w: [w] for w in ("入れる", "開ける", "届ける")})
OVERRIDES["ja"].update({"入れ": ["入れる"], "開け": ["開ける"], "届け": ["届ける"], "あけ": ["開ける"], "あける": ["開ける"]})

# Korean: 거 ("thing", 것 as spoken) is in the pack only as a Chinese character's reading, and its
# contractions with a particle (게 of 거 + 이, 건, 걸) are written as one; 해, 볼, 갈 and 알 are verb forms
# the pack files as nouns ("the sun", "cheek"); 여기서 and 내게 are 여기 and 나 contracted with a particle.
OVERRIDES["ko"] = {
    "거": ["거"], "게": ["거"], "걸": ["거"], "건": ["거"], "거야": ["거"], "겁니다": ["거"],
    "해": ["하다"], "하지": ["하다"], "하지만": ["하지만"], "그렇지": ["그렇다"], "아니라": ["아니다"],
    "아니": ["아니"], "아냐": ["아니다"], "아뇨": ["아뇨"], "말야": ["말"],
    "마": ["말다"], "마요": ["말다"], "마세요": ["말다"], "마라": ["말다"], "맘": ["마음"],
    "여기서": ["여기"], "여긴": ["여기"], "여길": ["여기"], "여기요": ["여기"], "거기서": ["거기"], "저기서": ["저기"], "어디서": ["어디"],
    "내게": ["나"], "네게": ["너"], "볼": ["보다"], "갈": ["가다"], "알": ["알다"], "살": ["살다", "살"],
    "다시는": ["다시"], "다신": ["다시"],
    # 뭐 "what" (무엇 as spoken) is not in the pack; contractions of a pronoun with 는 or 를 (넌, 그걸) go to it.
    "뭐": ["뭐"], "뭔가": ["뭔가"], "뭔데": ["뭐"], "뭔지": ["뭐"], "누군지": ["누구"],
    "넌": ["너"], "그건": ["그것"], "그걸": ["그것"], "이건": ["이것"], "이걸": ["이것"], "저건": ["저것"], "저걸": ["저것"],
    "자넨": ["자네"], "얘길": ["이야기"], "제게": ["저"], "거요": ["거"], "거고": ["거"], "거잖아": ["거"],
    "할": ["하다"], "큰": ["크다"], "보고": ["보다"], "본": ["보다"], "가지고": ["가지다"], "좋아요": ["좋다"], "널": ["너"],
    "여사": ["여사"], "건데": ["거"], "건가": ["거"], "건가요": ["거"], "건지": ["거"], "거라": ["거"], "겁니까": ["거"], "거다": ["거"],
    "해도": ["하다"], "해서": ["하다"], "작은": ["작다"], "가요": ["가다"], "그치": ["그렇다"], "왜요": ["왜"],
    "니가": ["너"], "우릴": ["우리"], "우리요": ["우리"], "우리네": ["우리"], "데는": ["데"], "데서": ["데"],
    "마시고": ["마시다"], "마담": ["마담"], "비리": ["비리"], "외신": ["외신"], "외세": ["외세"],
    "아녜요": ["아니다"], "그럼요": ["그럼"], "저기요": ["저기"], "제발요": ["제발"], "갖고": ["가지다"], "텐데": ["터"], "테니": ["터"],
    # The copula after a quotation, which news text writes apart from it ("…"이라며).
    "며": ["이다"], "라며": ["이다"], "이라며": ["이다"], "면서": ["이다"], "이면서": ["이다"],
}

# Korean words that are a verb's form, or a word with a particle, against what the builder's counts say
# (build_korean): the list then has the first as forms, by which the reader keeps them whole. The pack
# and Yomitan's rules read some nouns with 을 as a verb's (짓을 of 짓다, whose form is 지을).
KOREAN_WHOLE = set()
KOREAN_CUT = {
    "짓을", "짓이", "짓은", "짓도", "남의", "적이", "적을", "이를", "길을", "거라고", "거라", "거라면", "거란", "거니까",
    "나랑", "애가", "아이가", "삶을", "삶은", "숨을", "약을", "불을", "불이", "신의", "신이", "신은", "신도", "집은", "물은",
    "차는", "배는", "개는", "양이", "양은", "목이", "골을", "빚을", "이거야", "미소를", "측은", "사의",
    "도시는", "비는", "달은", "길은", "역사가", "정치가", "주제가", "지구의", "문의",
    # Read in subtitles as 수 + 도 (할 수도 있다) and 길 + 이, more than as "capital" and "length".
    "수도", "길이",
}
# Rare verbs whose forms in the pack spell names and loanwords (미니, 메리, 조니), and 긷다, whose forms are
# 길다's: their forms go nowhere, or to the word they are.
EXCLUDE.setdefault("ko", set()).update({
    "인하대", "닉다", "시읏", "순천", "미다", "베다", "매다", "메다", "헤다", "에다", "재다", "긷다", "디다", "갉다", "모드다", "여의다",
})
OVERRIDES["ko"].update({
    "길어": ["길다"], "길어요": ["길다"], "길었던": ["길다"], "길어야": ["길다"], "길어서": ["길다"],
    "길었다": ["길다"], "길었지": ["길다"], "더는": ["더"], "버니": [], "버락": [], "번가": [], "불신": ["불신"],
    "부실": ["부실"], "분담": ["분담"], "감자": ["감자"], "감면": ["감면"], "때면": ["때"], "땐": ["때"], "땜": ["때문"], "배려": ["배려"], "조니": [], "조이": [],
})
# 거 with the copula, which the pack files under 걸다; 다니 under 달다; 새 "new" and 잔 "cup" under
# 사이 and 자다; spellings of dialect or of a slip that are no word to learn.
OVERRIDES["ko"].update({form: ["거"] for form in (
    "거라고요", "거니까요", "거래요", "걸까요", "거냐", "거면", "거니", "거네", "걸요", "건데요", "거란다", "거라구요", "거라구",
    "거냐고", "거라네", "거랍니다", "거라니까", "거라며", "거라니", "거면서",
)})
OVERRIDES["ko"].update({
    "다니": ["다니다"], "마시오": ["말다"], "줄리": [], "새": ["새"], "잔": ["잔"], "한국": ["한국"],
    "기냥": [], "오데": [], "어데": [], "긍게": [], "한나": [], "아참": [], "다함": [],
})
# Verbs whose forms the pack files under a shorter verb they begin like (마셔 under 마다 and 말다, 지워 under
# 집다, 가셨어 under 갈다): a form beginning so goes to the verb (KOREAN_STEMS[verb] = beginnings).
KOREAN_STEMS = {
    "마시다": ("마시", "마셔", "마셨", "마신", "마실", "마십니"),
    "지우다": ("지우", "지워", "지웠", "지운", "지울"),
    "가시다": ("가시", "가셔", "가셨", "가신", "가실", "가십"),
    # The pack files 달아, 달았다 under 닫다 too, whose forms are 닫아, 닫았다.
    "달다": ("달아", "달았"),
}
# Person names, surnames and brands that rank by their use in news; Korean's cities stay words.
EXCLUDE["ko"].update({
    "트럼프", "김", "박", "황", "윤", "최", "허", "노", "임", "추", "삼성", "카카오", "애플", "한겨레", "부시", "잭", "샘", "피트", "론", "맥", "존",
})
KOREAN_CUT.update({"일이", "때문에", "명의", "박은", "박이", "박을", "숨이", "숨도", "숨은", "차라도", "배라도", "데라도"})
# 거지 is 거 + 지 (것이지) far more than "beggar"; 꺼 is 거 after a possessor (내 꺼). 아뇨 is 아니요 as spoken.
# 써 and 타, verb forms the pack files as an adverb and a determiner. 차라, a rare form of 차다 that would
# take 차라도 from 차 + 라도; 차도 is mostly 차 + 도.
OVERRIDES["ko"].update({
    "거지": ["거"], "거지요": ["거"], "꺼": ["거"], "아뇨": ["아니요"], "아니요": ["아니요"], "써": ["쓰다"], "타": ["타다"], "차라": [],
})
KOREAN_CUT.update({"차도"})
# Sino-Korean nouns spelled as a verb's form with -자 or -지 that the builder would give the verb (일다, 감다, 굴다).
OVERRIDES["ko"].update({w: [w] for w in ("일자", "감지", "굴지")})
# Verb forms that the builder's counts would cut as a noun with a particle (자 + 는, 아 + 나 + 요), and 누가요,
# 누가 with the polite 요. 니 is 너 as spoken (니가, 니 엄마); 해야하다 is 해야 하다 written without its space.
KOREAN_WHOLE.update({"자는", "자를", "아나요", "누가요"})
OVERRIDES["ko"].update({"니": ["너"], "누가요": ["누가"], "해야하다": ["하다"]})
# Adverbs in -게 that are words of their own, not their adjective's form (build_list): the pro-forms, and 되게 "very".
OVERRIDES["ko"].update({w: [w] for w in ("어떻게", "이렇게", "그렇게", "저렇게", "되게")})
# A noun and a name the pack files as rare verbs' forms (고다, 썰다), and 잘 자 "good night" written as one.
OVERRIDES["ko"].update({"고심": [], "써니": [], "잘자": ["자다"]})
# 너냐 "is it you", which the pack files under 넣다; 안나, the name Anna, or 안 나 written as one.
OVERRIDES["ko"].update({"너냐": ["너"], "안나": []})
# 서 (서 있어요, 거기 서) and 기어 (기어 가다) alone are 서다's and 기다's far more than "west" and "marlin";
# the builder's test for nouns spelled as a verb's form misses them (서로 and 기어이 look like 서, 기어 with a particle).
OVERRIDES["ko"].update({"서": ["서다"], "기어": ["기다"]})
# 앞에서 and 중에서 take particles of their own (앞에서는, 중에서도): as forms of 앞 and 중 the reader keeps them whole.
# 혼자서, 멀리서, 둘이서 and 셋이서 are 혼자, 멀리, 둘 and 셋 with 에서 contracted, as 여기서 is.
OVERRIDES["ko"].update({"앞에서": ["앞"], "중에서": ["중"], "혼자서": ["혼자"], "멀리서": ["멀리"], "둘이서": ["둘"], "셋이서": ["셋"]})
# 화가 is mostly 화 + 가 (화가 나다), 적도 적 + 도 (본 적도 없다), more than "painter" and "equator"; 오와 and 오도,
# which the pack files under 오다, are 오 + 와, 오 + 도.
KOREAN_CUT.update({"화가", "적도", "오와", "오도"})
# Spoken 바래 is 바라다 "hope" misspelt, far more than 바래다 "fade"; 데리다 "take along", a verb of few forms,
# has none in the pack (데려 is filed under 데다), nor has it 이것저것 "this and that"; 일대 "area" and 노래다
# "it is a song" are no forms of 일다 and 노리다, nor 거리다 and 거시기 of 걸다, nor 걸세 (거 + ㄹ세); 일어서 is
# 일어서다's; 칠 is 치다's (칠 수 있다) more than "seven".
OVERRIDES["ko"].update({form: ["바라다"] for form in (
    "바래", "바래요", "바랬어", "바래야지", "바랬어요", "바랬는데", "바랬던", "바랬다", "바랬을", "바랬지", "바래야죠", "바랬지만",
    "바랬죠", "바래야", "바랬고", "바랬거든", "바랬는지", "바랬나", "바랬겠지", "바랬기", "바랬거든요", "바래왔던", "바래서야", "바래서",
)})
OVERRIDES["ko"].update({form: ["데리다"] for form in (
    "데리고", "데리러", "데리구", "데려", "데려다", "데려다가", "데리고는", "데리고도", "데려서",
)})
OVERRIDES["ko"].update({
    "일대": ["일대"], "이것저것": ["이것저것"], "노래다": ["노래"], "거리다": [], "거시기": ["거시기"], "일어서": ["일어서다"], "칠": ["치다"], "걸세": ["거"],
})
# Forms of two words alike, each listed under both: 꺼 is 거 (내 꺼) and 끄다 (불 꺼); 끈다 끄다 and 끌다; 사지 and 사면
# the nouns and 사다 (사지 마, 사면 돼요); 실은 "in fact" and 싣다 (짐을 실은 차).
OVERRIDES["ko"].update({
    "꺼": ["거", "끄다"], "끈다": ["끄다", "끌다"], "사지": ["사다", "사지"], "사면": ["사다", "사면"], "실은": ["실은", "싣다"],
})
# Verbs in 하다 kept apart from their noun, which means another thing (이상하다 "strange", 이상 "above"; 시장하다
# "hungry", 시장 "market"; 불구하고 "despite").
KOREAN_APART = {
    "이상하다", "불구하다", "시장하다", "사고하다", "부인하다", "일정하다", "부상하다", "정당하다", "요원하다", "교사하다", "자세하다",
    "유리하다", "가정하다", "운명하다", "매장하다", "고대하다", "비상하다", "주체하다", "기구하다", "소원하다", "불길하다", "재고하다",
    "무력하다", "가세하다", "광대하다", "사양하다", "행사하다", "방한하다", "치열하다", "시사하다", "시인하다", "유도하다", "유치하다",
    "동경하다", "대두하다", "동사하다",
}
# Words written without their space (우리집 for 우리 집, 신경쓰다 for 신경 쓰다), counted for the words they join; 일년 and
# 석달 are 일 년 and 석 달, whose number counts as known.
OVERRIDES["ko"].update({
    "우리집": ["우리", "집"], "어느정도": ["어느", "정도"], "신경쓰다": ["신경", "쓰다"], "일년": ["년"], "석달": ["달"],
    "생일파티": ["생일", "파티"], "소리지르다": ["소리", "지르다"], "잘자요": ["자다"], "손대지마": ["손대다", "말다"],
    "손대지마요": ["손대다", "말다"], "안와": ["안", "오다"], "안와요": ["안", "오다"],
})
OVERRIDES["ko"].update({form: ["차다", "있다"] for form in ("차있어", "차있고", "차있죠", "차있지만", "차있어요", "차있지", "차있었죠", "차있단다")})
# A word contracted with 는 (그땐, 저흰, 아버진) or with the copula (접니다, 무대서), counted for the word.
OVERRIDES["ko"].update({
    "하난": ["하나"], "계신데": ["계시다"], "그땐": ["그때"], "거긴": ["거기"], "저긴": ["저기"], "저흰": ["저희"], "너흰": ["너희"],
    "아버진": ["아버지"], "어머닌": ["어머니"], "여잔": ["여자"], "자긴": ["자기"], "따윈": ["따위"], "나머진": ["나머지"],
    "소린": ["소리"], "아깐": ["아까"], "아저씬": ["아저씨"], "필욘": ["필요"], "우린": ["우리"], "우리지": ["우리"],
    "우리라니": ["우리"], "우리잖아": ["우리"], "우리게": ["우리"], "무대서": ["무대"], "무대라": ["무대"], "쫌": ["좀"],
    "거구요": ["거"], "접니다": ["저"], "접니까": ["저"], "전데요": ["저"], "저니까요": ["저"], "온대요": ["오다"],
    "뒤면": ["뒤"], "뒤라": ["뒤"], "뒤로군": ["뒤"], "뒤던가": ["뒤"], "오지도": ["오다"], "오진": ["오다"], "오질": ["오다"],
    "오지는": ["오다"],
})
OVERRIDES["ko"].update({form: ["씨"] for form in ("씨네", "씨라고", "씨세요", "씨군요", "씨란다", "씨라면", "씨라", "씨오")})
OVERRIDES["ko"].update({form: ["개"] for form in (
    "개라", "개잖아", "개네", "개라니", "개니", "개잖아요", "개라고요", "개라구요", "개로군", "개거든요",
)})
# Verbs' forms the pack or the rules give a look-alike: 끄다's alike with 끌다's (끄는, 끄세요), 자다's under 잘다, 골다's
# under 고다, 열다's under 연달다; 모시다, 드시다, 쑤시다 and 갈리다 are verbs of their own, not 몰다's, 들다's, 쑤다's or
# 갈다's honorific or passive; 하시다 and 하시 are 하다 with the honorific.
OVERRIDES["ko"].update({form: ["끄다", "끌다"] for form in ("끄는", "끄세요", "끄시고", "끕니다", "끄는데", "끄냐")})
OVERRIDES["ko"].update({
    "자냐": ["자다"], "자세요": ["자다"], "잘래요": ["자다"], "고는": ["골다"], "곤다": ["골다"], "연다는": ["열다"],
    "연다니까요": ["열다"], "다가서": ["다가서다"], "길러진": ["기르다"], "춥니다": ["춥다"], "쉽네": ["쉽다"],
    "데요": ["데"], "데서요": ["데"], "데려고": ["데리다"], "델꼬": ["데리다"], "하시다": ["하다"], "하시": ["하다"],
    "모시다": ["모시다"], "쑤시다": ["쑤시다"], "갈리다": ["갈리다"], "이셨어요": ["이다"], "가다가": ["가다"],
})
KOREAN_STEMS.update({
    "모시다": ("모시", "모셔", "모셨", "모신", "모실", "모십"),
    "드시다": ("드시", "드셔", "드셨", "드신", "드실", "드십", "드세"),
    "쑤시다": ("쑤시", "쑤셔", "쑤셨", "쑤신", "쑤실"),
    "갈리다": ("갈리", "갈려", "갈렸", "갈린", "갈릴"),
})
# Verbs' forms the pack files as nouns, more often the verb's (불 켜, 길 건너, 잘 그릴 수 있다, 비어 있다), and adjectives'
# forms it has as words (이상한); 잘생기다, which it has only as 잘생겼다.
OVERRIDES["ko"].update({
    "켜": ["켜다"], "건너": ["건너다"], "섰다": ["서다"], "접어": ["접다"], "그릴": ["그리다"], "비어": ["비다"],
    "이상한": ["이상하다"],
})
OVERRIDES["ko"].update({form: ["잘생기다"] for form in ("잘생기다", "잘생겼다", "잘생긴", "잘생기고", "잘생겼어", "잘생겨서", "잘생겼네")})
# Names, loanwords and fragments the rules make some rare verb's forms (미니, 밀러 of 밀다; 세단 of 세다; 소니 of 솔다).
OVERRIDES["ko"].update({form: [] for form in (
    "미니", "밀러", "밀고자", "미시", "밀매", "밀란", "미신고", "세단", "세라", "세실", "세이", "세요", "세시네", "모네", "다다",
    "시군", "십니다", "시군요", "셨나요", "신가요", "시냐", "시네", "셔", "셨어요", "셨다", "시오", "는데", "는다", "기시", "기시다",
    "길리", "지아", "쏘리", "출세", "추심", "추대", "추신", "갈리아", "갈릴레", "갈마", "저신다", "젖소", "소니", "소냐", "솔라",
    "던", "던데", "던가", "고고", "고마", "고자질", "고건", "고자", "고은", "고게", "고서", "고아라", "옥자", "옥시", "옥시는",
    "옥실", "식자재", "식대", "휠", "휠라", "휘이", "배신감", "배기", "배뇨", "배라", "배라구요", "신디", "신고자", "신고서",
    "신우", "신라면", "신아", "신야", "건지도", "노라", "눈가", "누락", "쪼리", "쫄지", "쫄", "졸나", "조네", "조신", "개소",
    "개시다", "업신", "하야시", "하야와", "가르", "과부하", "가리", "아구", "아구창", "크", "기단", "기라", "기디", "델레", "데라",
    "덴바", "추이",
)})
# Nouns with a particle or the copula that the pack and the rules read as a verb's form (젖이 of 젖다, 속도다 of 속다), and
# 위도 "latitude", mostly 위 + 도.
KOREAN_CUT.update({"젖이", "녹이", "굴을", "노를", "속도다", "도시다", "위도"})
# 고소하다 "savoury" is no 고소 "lawsuit".
KOREAN_APART.add("고소하다")
# Countries, places, holidays and the like the pack has as names only, as Korean's cities are words (서울, 부산), and
# everyday words it lacks.
OVERRIDES["ko"].update({w: [w] for w in (
    "미국", "일본", "중국", "북한", "영국", "러시아", "독일", "프랑스", "뉴욕", "한반도", "대한민국", "우리나라", "유럽", "베트남",
    "크리스마스", "아시아", "인천", "울산", "제주", "광주", "올림픽", "도쿄", "멕시코", "이탈리아", "평양", "스페인", "캐나다",
    "월드컵", "추석", "태국", "제주도", "베이징", "아프리카", "인도네시아", "강원도", "싱가포르", "브라질", "대만", "필리핀",
    "이집트", "스위스", "태평양", "경기도", "오사카", "성탄절", "한강", "유엔", "남한", "몽골", "일본어", "중국어",
    "여부", "고속도로", "신용카드", "퇴원", "재활용", "팀장", "숙모", "관광지", "중고차", "공연장", "영화제", "페스티벌", "다이어리",
    "주민센터", "상영",
)})
# 나와 is as often 나 + 와 "and I" as 나오다's form: the reader cuts it, as courses read it.
KOREAN_CUT.add("나와")
# 누구세요 and 어디세요 "who / where is it", 누구 and 어디 with the honorific copula.
OVERRIDES["ko"].update({"누구세요": ["누구"], "어디세요": ["어디"]})

# Arabic words read whole, or cut into the letters in front and the word after them, against what the builder's
# counts say (build_arabic): the list has the first, by which the reader keeps them whole, and leaves out the second.
ARABIC_WHOLE = {
    "الله", "اللهم", "لله", "اليوم", "الآن", "الان", "الليلة", "اللي", "لقد", "لأن", "لان", "لكي", "لذا", "لذلك", "كذلك",
    "لما", "بدون", "كلا", "لماذا", "لدى", "كأن",
}
ARABIC_CUT = {
    "ولكن",
}
# Names, the stems of names written with ال (صين of الصين, which then stays whole), foreign and dialect words.
EXCLUDE.setdefault("ar", set()).update({
    "محمد", "أحمد", "خالد", "حمد", "جون", "سي", "صين", "عراق", "أردن", "فرنس", "قاهرة", "كويت", "دي", "ده", "مكن", "اوروبا",
    "يمن", "هند", "بنان", "ندن", "اريس", "ندا", "شار", "يرو", "ينيا", "اضل", "سودان", "أربعاء", "جزائر", "سوريا", "تركيا",
    "روسيا", "بريطانيا", "ألمانيا", "المانيا", "إيطاليا", "ايطاليا", "إسبانيا", "اسبانيا", "أستراليا", "استراليا", "عمان",
    "ليبيا", "كوريا", "أوروبا", "أوربة", "سليمان", "سارة", "يل", "دبي", "مش",
})
OVERRIDES.setdefault("ar", {}).update({
    "لدي": ["لدى"], "يدي": ["يد"], "تمت": ["تم"], "يجب": ["وجب"], "تجب": ["وجب"], "علي": ["على"], "خطر": ["خطر"],
    "كبيرة": ["كبير"], "ألمان": ["ألماني"], "أسود": ["أسود"], "يوما": ["يوم"], "عدل": ["عدل"], "قيم": ["قيمة"], "لذا": ["لذا"], "أمي": ["أم"], "امي": ["أم"],
})
# Arabic feminines and plurals that are words of their own, not the adjective's or singular's (جنسية "nationality",
# نظرية "theory", ضربة "a blow"); and forms the rules miss (طفلة, which the dictionary lacks; قصص and أنفس, which it
# has as words of their own).
OVERRIDES["ar"].update({w: [w] for w in (
    "جنسية", "نظرية", "ديمقراطية", "بلدية", "خلفية", "ملكية", "شرعية", "بشرية", "أهلية", "دنيا", "نظرة", "ضربة",
)})
OVERRIDES["ar"].update({"طفلة": ["طفل"], "قصص": ["قصة"], "أنفس": ["نفس"]})
# Verbs of their own that wty also tags as another verb's present (أعاد "to repeat", أعد "to prepare", توفي "to die").
OVERRIDES["ar"].update({w: [w] for w in ("أعاد", "أعد", "توفي")})
# Forms the rules miss: wty has تصل as a noun too, بقي's forms under its other spelling بقى, أمه as ماه's only, and
# أود "I would like" shared with ودى.
OVERRIDES["ar"].update({"تبقى": ["بقي"], "تصل": ["وصل"]})
OVERRIDES["ar"].update({"طيبة": ["طيب"], "سيئة": ["سيئ"], "كلاب": ["كلب"], "عروض": ["عرض"], "شخصا": ["شخص"], "أبا": ["أب"], "أباه": ["أب"],
                       "أمه": ["أم"], "أود": ["ود"]})
# Names spelled as forms of a word the rules now list: لبنان, the dual of لبن "milk", counts for nothing, ألبانيا for
# ألباني; and أنتي is أنت misspelt, not أنة's.
OVERRIDES["ar"].update({"لبنان": [], "ألبانيا": ["ألباني"], "الباني": ["ألباني"], "أنتي": ["أنت"], "انتي": ["أنت"]})
