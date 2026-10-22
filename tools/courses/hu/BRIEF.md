# Writing a Hungarian frequency course

Each course teaches one band of 100 words from the Hungarian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `hu-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Anna egy macskát akar. Elmegy egy menhelyre. Ott három macskát lát...*
  Everyday life in Hungary: Budapest and its districts, Debrecen, Szeged, Pécs, Győr, Miskolc,
  Eger, Kecskemét, Sopron, Veszprém, Lake Balaton and its villages, the Danube Bend, the Tisza, the
  Great Plain (*Alföld*) and its farmsteads (*tanya*), the Mátra and Bükk hills, villages, weekend
  houses and gardens (*hétvégi ház*, *kert*); and now and then Vienna, London or Berlin, where
  Hungarian is spoken at home: home, family, food, shopping, work, school, the city, health, free
  time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Hungarian *ő* is "he" and "she", so say
  who *ő* is whenever it could be unclear. Give each story its own names: the learner reads the
  course in one go, and two Annas in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (the focus before the verb: *Anna ma megy a boltba. Kenyeret vesz.*). Present tense up to rank
  500, with the past of single finished events where Hungarian needs it (*Tegnap esett.*) and *fog*
  + infinitive for the future; from rank 600 the story may be told in the past (*Anna elment a
  boltba. Esett az eső.*). Hungarian drops subject pronouns: write *én*, *te*, *ő*, *ők* only for
  contrast, emphasis or a new subject. Little dialogue; when there is some, short and simple, with
  *te* (tegezés) among family and friends and *ön* or *maga* with the third person (magázás) for
  strangers, older people and formal settings (*Segíthetek?*, *Kovács úr*, *Mari néni*,
  *Józsi bácsi*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*mert*, *ezért*, *amikor*, *mielőtt*, *miután*,
  *bár*, *pedig*, *hanem*, *viszont*), dialogue can carry more of the story, and a story may be told
  by a first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the
  bands, each band using what came before:
  - ranks 1–300: vowel harmony in every ending (*házban*, *kertben*, *ötben*); the present tense
    with the indefinite and the definite conjugation (*látok egy házat*, *látom a házat*; *kér*,
    *kéri*), the -ik verbs (*dolgozik*, *eszik*); *van*, *nincs*, *nincsenek*, and having with *van*
    (*Annának van egy kutyája*); the plural *-k* (*házak*, *almák*); the accusative *-t* (*kenyeret*,
    *almát*); the commonest cases in plain phrases: *-ban/-ben*, *-ba/-be*, *-ból/-ből*,
    *-n/-on/-en/-ön*, *-ra/-re*, *-nál/-nél*, *-hoz/-hez/-höz*, *-nak/-nek*, *-val/-vel* (*Péterrel*,
    *kerttel*); the possessive endings (*házam*, *házad*, *háza*, *kutyája*); question words (*mi*,
    *ki*, *mit*, *kit*, *miért*, *mikor*, *milyen*) and yes/no questions by intonation; *nem* and
    *ne*; *akar*, *kell*, *tud* + infinitive; *fog* + infinitive; the past of the commonest verbs
    (*volt*, *ment*, *mondta*, *látta*); the verbal prefixes written apart where Hungarian puts
    them after the verb: in commands (*Gyere be! Tedd le!*), with *nem* and *ne* (*Nem megy el.
    Ne menj ki!*) and with *is* (*Be is jön.*);
  - ranks 301–600: the past tense for events, indefinite and definite (*vettem*, *vettem egy
    kenyeret*, *megvettem a kenyeret*); prefixed verbs and the prefix moving (*Megnézi a filmet.
    Nem nézi meg. Meg akarja nézni.*); the imperative and its everyday requests (*Gyere! Add ide!
    Üljünk le!*); *-hat/-het* (*Bejöhetek? Itt lehet parkolni.*); comparatives (*nagyobb, mint*,
    *a legnagyobb*); postpositions (*előtt*, *után*, *mellett*, *alatt*, *mögött*, *között*); the
    remaining cases *-tól/-től*, *-ig*, *-ért*; *-lak/-lek* (*szeretlek*, *látlak*); numbers and the
    time (*hétkor*, *fél nyolckor*, *negyed kilenc*);
  - ranks 601–1000: the conditional (*szeretnék*, *jó lenne*, *ha lenne időm, eljönnék*); the
    possessive with plural possessions (*barátaim*, *könyvei*); *-é* and *-ék* (*Annáé*, *Annáék*);
    relative clauses with *aki*, *ami*, *amely*, *ahol*, *amikor*; *hogy*-clauses; the present
    participle in *-ó/-ő* (*az alvó gyerek*) and *-va/-ve* (*ülve*, *nevetve*); the personal
    infinitive (*mennem kell*, *nem kell sietned*); *-vá/-vé*, *-ként*, *-kor*;
  - ranks 1001–1300: the past conditional (*ha tudtam volna, eljöttem volna*); the subjunctive in
    *hogy*-clauses (*azt akarja, hogy jöjjön*, *mondd meg neki, hogy hívjon fel*); the past
    participle (*a megírt levél*, *a tegnap vett kenyér*); verbal nouns in *-ás/-és* (*olvasás*,
    *főzés*); reported speech (*azt mondta, hogy fáradt*); the causative (*megcsináltatja*,
    *megjavíttatja*);
  - ranks 1301–1600: the indefinite pronouns (*valaki*, *bárki*, *senki*, *semmi* with double
    negation: *senki nem jött*, *semmit sem tud*), postpositions with the possessive (*előttem*,
    *mögötte*, *miatta*), *-e* in indirect questions (*Nem tudom, jön-e.*), *mintha*, *akár...
    akár*, *sem... sem*, *nemcsak... hanem... is*;
  - ranks 1601–2000: the participle in *-andó/-endő* (*a megoldandó feladat*), the verbs in
    *-ódik/-ődik* and *-ul/-ül* that say what happens by itself (*kinyílik az ajtó*, *elromlott az
    autó*), *van* + *-va/-ve* (*Az ajtó be van zárva.*), *ahhoz, hogy*, *annak ellenére, hogy*;
  - ranks 2001–2500: longer participial phrases, the past conditional in full, verbal nouns as
    subjects, connectors such as *ezért*, *emiatt*, *ugyanakkor*, *ráadásul*, *ennek ellenére*;
    reported speech with the original tense kept, as Hungarian does;
  - ranks 2501–3000: a richer, more formal register: connectors such as *azonban*, *ugyanis*,
    *tehát*, *továbbá*, *mivel*, *amennyiben*, *-ra vonatkozóan*; long participial phrases; common
    idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *tárca* (a light column on everyday life, as in Hungarian newspapers),
    dialogue that marks tegezés and magázás, less common connectors (*holott*, *noha*, *jóllehet*,
    *ámbár*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone and
    register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, regional and learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a *tárca*, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Itt van? Nincs itt. Ott van? Nincs ott sem.*).
- **Questions at the end**: after the story, a paragraph starting `Kérdések:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *miért* and *hogyan*, with fuller answers):

  ```
  Kérdések:
  Mit akar Anna?
  Anna egy macskát akar.
  Hova megy Anna?
  Anna egy menhelyre megy.
  ```

- **Standard Hungarian**: neutral, correct and natural Hungarian as written today (the spelling
  of the Akadémia, *AkH. 12*), with vowel harmony, the definite and indefinite conjugation, the
  long and short vowels (*kör*/*kőr*, *tör*/*tőr*, *irt*/*írt*), the double consonants
  (*hall*/*hal*, *megy*/*meggy*) and the assimilations (*kerttel*, *Péterrel*, *látja*, *olvassa*,
  *nézzük*) right in every word. Spoken forms (*nem tom*, *asszem*, *mit csinálsz?* is fine,
  *mit csinász* is not), slang (*csaj*, *gáz*) and dialect are not used, not even in dialogue;
  dialogue may be lightly colloquial only in what any careful speaker says (*szia*, *helló*,
  *köszi*, *oké*, *jaj*). Always write the accents: the checker compares letters exactly, and
  *kerek* (round), *kérek* (I ask) and *kerék* (wheel) are three words.
- **Hungarian typography**: direct speech is set out as Hungarian books do it, each speaker's line
  a paragraph of its own beginning with an en dash and a space, the speaker's tag after another
  dash:

  ```
  – Szia, Anna! – mondja Péter.
  – Szia! – feleli Anna. – Hogy vagy?
  ```

  The questions and answers of the final `Kérdések:` block are plain lines, as above; a quoted
  sentence inside an answer goes in Hungarian quotation marks („…”). Write numbers as digits
  (*3 gyerek*, *7-kor*, *7.30-kor*, *2010-ben*), ordinals and dates with a full stop (*5.
  osztály*, *május 3.*, *a 19. században*), the ending after a hyphen as Hungarian does; spell out
  a number word that is in your band to teach it (*hét nap*). *Kérdések:* is the lesson's layout,
  not one of its words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and other houses of worship, prayer, priests,
  heaven and hell, and the holidays and customs of religious origin, even as a date, a meal or a
  gift season: Christmas (*karácsony*, *szenteste*, *Mikulás*, *advent*), Easter (*húsvét*,
  *locsolkodás*), Pentecost, All Saints' Day, Halloween, name days (*névnap*) and saints' days;
  and the religious phrases of everyday speech (*Istenem!*, *Hála Istennek*, *Isten hozott*,
  *Isten éltessen*): greet with *szia*, *helló*, *jó napot*, *üdvözlöm*; wish *boldog
  születésnapot*. Birthdays, New Year's Eve (*szilveszter*) and New Year, the grape harvest and
  other folk customs with no church holiday named (*szüreti mulatság*, a village fair, a folk
  dance, painted eggs at a spring fair) are fine;
- **politics**, the state, elections, parties, ministers, presidents, kings and queens (fairy
  tales included), borders, national days and nationalism, and every country's disputes and
  history of conflict; everyday civic life (a library, a school board, a residents' meeting, the
  town hall's opening hours) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  traffic warden is fine; thefts, arrests, prisons, lawyers and trials are not);
- **death**, killing, funerals, mourning, blood and injury beyond a scraped knee;
- **vulgar or sexual words**, insults, and words whose main sense is sexual;
- **alcohol** (wine, beer, *pálinka*, bars, wine cellars, a toast), **gambling** (betting,
  lotteries, casinos), drugs and smoking;
- **person names taught as words**: a band word that is a name, a surname, a place name or a word
  made from a place or a people (*budapesti*, *amerikai*, *európai*) is skipped. Language names
  are words of their own and are taught as languages (*magyar* through *magyarul*, *a magyar
  nyelv*; *angol*, *német*, *francia* through *angolul*, *németül*, *franciául*), never as
  nationalities.

Skip band words that only fit these topics, and say so in your report. Those of the first 1000
ranks:

- religion: *isten* 195, *istenem* 314, *szent* 465, *templom* 793, *zsidó* 942, *pokol* 948;
  *imád* 438 only as "to adore" (*imádja a csokit*);
- death, violence, crime: *meghal* 221, *megöl* 257, *öl* 357, *halál* 426, *halott* 446,
  *fegyver* 422, *rendőrség* 464, *lő* 487, *háború* 491, *gyilkos* 620, *rendőr* 641, *áldozat*
  653, *halála* 662, *katona* 663, *börtön* 678, *vér* 703, *harcol* 794, *támadás* 824, *bizonyíték*
  836, *üt* 838, *harc* 840, *támad* 858, *bíró* 875, *gyilkosság* 896, *bánt* 927, *nyomozó* 929,
  *lop* 932, *bíróság* 940, *ügyvéd* 975, *zsaru* 978; *hal* 295 only as "fish" (*halat*, *halak*),
  never "to die" (*halt*, *halni*);
- politics: *király* 311, *nemzeti* 419, *kormány* 474, *állam* 510, *elnök* 524, *törvény* 547,
  *állami* 830, *jog* 864, *politikai* 950, *választás* 960 only as "choice";
- vulgar, insulting, sexual: *fene* 384, *hülye* 398, *szar* 460, *kurva* 463, *franc* 477,
  *csaj* 691, *fasz* 886, *mell* 912, *szex* 913, *rohadt* 922, *kibaszott* 934, *idióta* 941;
  *szemét* 966 only as "rubbish";
- alcohol and gambling: *bár* 249 only as "although" (never *bárban*); *fogad* 413 only as "to
  receive, to welcome", never "to bet"; *nyer* 287 in sport and games, never in betting;
- names and words from places and peoples: *amerikai* 347, *budapesti* 604, *európai* 436, *orosz*
  552 and *kínai* 765, *olasz* 808, *japán* 816, *frank* 951 except as languages (*oroszul*,
  *kínaiul*, *olaszul*, *japánul*); *magyar* 78, *német* 374, *angol* 408, *francia* 628 as languages.

## The file

Write `tools/courses/hu/<id>.json`:

```json
{
  "id": "hu-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Hungarian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places and brands used in the texts, without endings"],
  "lessons": [
    {
      "title": "Story title in Hungarian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/hu/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *mondta* for *mond*, *házban* for *ház*, *nem megy el* for
  *el*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: a
  verb in the third person singular present, as Hungarian dictionaries give it (*mond*, *megy*,
  *dolgozik*, *eszik*), a noun or adjective without endings.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, an abbreviation, a list error such as a name, a
  foreign word or a form of a lower word); the checker's last line lists the band words not taught
  yet.

Things to know about the Hungarian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/hu/wordlist.tsv`
  (a copy of the app's `hu.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built partly from film subtitles, so it is rich in spoken words (*oké*, *szia*,
  *hé*, *persze*, *srác*), and some everyday words rank later than you might expect (*apa* 137,
  *anya* 173, *lány* 156, *gyerek* 362, *kutya* 507, *kávé* 748, *asztal* 957, *bolt* 1096, *busz*
  1131, *testvér* 1161, *macska* 1567, *kenyér* 2300, *bátyja* 2474, *nagymama* 3510). The
  question words *hol* (113), *hogyan* (176), *hova* (303), *mennyi* (320) and the verb *kérdez*
  (310) are outside the first bands: ask with *mi*, *ki*, *mikor*, *miért*, *milyen* and tag
  dialogue with *mondja*. Check a word's rank rather than guess it.
- The list files a word's endings under its headword (the cases, plural and possessives of
  nouns, the conjugation of verbs) and keeps at most 40 of them; `extra_forms.tsv` adds the
  declension and conjugation tables and the possessive forms the English Wiktionary gives for the
  list's words, and the prefix on every form of a prefixed verb. On top of both, the checker reads
  the regular endings itself: a noun's case after its plural or possessive (*táskámban*,
  *kulcsomat*, *barátaival*, *kerttel*), a verb's personal endings (*olvastunk*, *dolgozom*,
  *segítenék*, *olvashatunk*, *nézzük*, *láss*), the personal infinitive (*mennem*, *tudnunk*). A
  word counts at the rank of the headword it is a form of; a form neither file has and the checker
  cannot read counts as an outside word.
- **Verbal prefixes.** The reader cuts words at spaces and hyphens, so a prefix written apart is
  a word of its own: *meg* (12), *el* (24), *ki* (23, also "who"), *be* (61), *fel* (62, *föl*
  is its form), *le* (71), *át*, *vissza*, *haza*, *össze*. In *Nem nézi meg* the learner reads
  *nézi* (*néz*) and *meg*. Written on, the prefix makes another word, and almost every common
  prefixed verb is a list entry of its own, often ranked far later than its verb (*elmegy* 194,
  *meghal* 221, *megnéz* 400, *meglát* 505, *bejön* 903, *kimegy* 1885, *feláll* 2255, *leül*
  2493, *letesz* 2639): *bejött* counts as *bejön* (903), not as *jön*. So in the early bands write
  the prefix apart where Hungarian does (*Gyere be! Tedd le! Nem megy ki. Ki akar menni. Be is
  jön.*) and avoid the attached forms the band does not reach. A prefixed verb is taught through
  its attached forms (*elment*, *elmegyek*), its verb through the bare ones (*ment*, *megyek*), and
  the prefix through its separate uses: never teach *megy* through *elment*. A prefixed verb that
  is not in the list counts as its verb (*megfőzte* as *főz*, *visszajövök* as *jön*).
- **Words filed as forms of others.** Some everyday words are filed only as a form of the word
  they come from: *eső* "rain" under *esik*, *folyó* "river" under *folyik*, *jövő* "next, future"
  under *jön*, *író* "writer" and *írás* under *ír*, *szeretet* under *szeret*, *mondat* under
  *mond*, *fizetés* under *fizet*, *ülés* under *ül*, *vevő* under *vesz*, *megálló* under
  *megáll*, *ének* "song" under *én*, *érték* under *ér*, *adat* and *adó* under *ad*. They count as
  known at that rank, but they cannot be taught, and a headword is never taught through them
  (*esik* is not taught by *eső*).
- **Homographs.** Many short headwords hold two words: *hét* "seven" and "week", *hat* "six" and
  "to have an effect", *nő* "woman" and "to grow", *fél* "half" and "to fear", *ér* "to reach",
  "to be worth" and "vein", *vár* "to wait" and "castle", *ég* "sky" and "to burn", *tér* "square"
  and "to turn", *hal* "fish" and "to die", *sír* "to cry" and "grave", *fog* "will", "to hold"
  and "tooth", *kor* "age", *ad* "to give", *bár* "although" and "bar". A form shared by two
  words counts for both. Teach a homograph in the sense its forms in the band file show and the
  content rules allow, and count its uses in that sense only. *vagy* is "or" (19), and also "you
  are", a form of *van*: teach *vagy* through "or". *mikor* is filed under *amikor*: teach
  *amikor* through *amikor*. *mindenki* (106), *utána* (342), *magad* (208), *magam* (154),
  *éves* (217), *kicsi* (393) and *kicsit* (125) are entries of their own, not forms of *minden*,
  *után*, *maga*, *év* or *kis*.
- **Inflected forms as entries.** About one entry in ten is only an inflected form of a lower
  entry: possessives (*lánya* 705, *háza* 801, *apád* 752, *barátja* 756, *munkája* 761, *nevem*
  844) and verb forms (*látta* 627, *szeretne* 759, *szeretlek* 416, *döntött* 766). Never teach
  them: they are the word already taught. `extra_forms.tsv` files the possessive ones under their
  word, so *háza* counts from *ház* (219); the verb forms count from their own rank (*látta* 627),
  so before that rank write *látja* or *látott*. Participles and verbal nouns with a meaning of
  their own are words (*ismert* "known", *nyitott* "open", *lakás* "flat", *vonat* "train",
  *repülő* "aeroplane", *bemutató* "show", *tanuló* "pupil").
- **Names** are not in the list: people, places (*Budapest*, *Balaton*, *Duna*, *Debrecen*), shops
  and brands go in `names`, once, without endings. Hungarian writes the endings on a name (*Annának*,
  *Péterrel*, *Budapestre*, *Annáék*, *Veránál*) and the checker reads them; a name of several
  words counts word by word, each part a name (*Kovács Anna*). Keep only true names in `names`,
  never common words, and never a name that is also a list word: *Dóra*, *Nóra*, *Levente*, *Ági*,
  *Virág*, *Rózsa*, *Flóra* and the surnames *Nagy*, *Kis*, *Kovács*, *Szabó*, *Varga*, *Molnár*,
  *Farkas*, *Fekete*, *Fehér*, *Török*, *Katona*, *Bíró* are list words: call the character
  something else (*Anna*, *Kati*, *Eszter*, *Judit*, *Réka*, *Zsófi*, *Lili*, *Juli*, *Vera*,
  *Péter*, *Gábor*, *Bence*, *Tamás*, *Máté*, *Ádám*, *Csaba*, *Dani*, *Marci*, *Zoltán*; *Tóth*,
  *Horváth*, *Kiss*, *Balogh*, *Papp*, *Simon*, *Lukács*). *néni* (1971) and *bácsi* (1555) after a
  name are words. Day and month names are in the list (*szeptember* 421, *március* 629); check
  their ranks.
- The checker compares whole words, lowercased, letter for letter. An ending after digits and a
  hyphen (*7-kor*, *2010-ben*) belongs to the number, which is not counted. A word joined by a
  hyphen counts as its parts (*van-e* is *van* + *e*). `Kérdések:` is not counted.
- The list's form lists are noisy (spellings without accents, unrelated words of the same
  spelling); a new word must appear as itself or a true inflection, never through such a form.
  Some entries are names, abbreviations or English words (*tv*, *oh*, *ok*, *s*, *e*): skip them.
  Loanwords that Hungarian uses as its own (*telefon*, *film*, *videó*, *busz*, *oké*) are taught
  like any word.
