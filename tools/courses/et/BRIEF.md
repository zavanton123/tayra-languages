# Writing an Estonian frequency course

Each course teaches one band of 100 words from the Estonian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `et-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Kati tahab Siimu raamatut. Siim ei anna seda. Ta annab Katile teise
  raamatu...* Everyday life in Estonia: Tallinn, Tartu, Pärnu, Narva, Viljandi, Rakvere,
  Kuressaare, Haapsalu, Võru, Paide, Otepää, Jõhvi; the islands Saaremaa, Hiiumaa, Muhu and Kihnu;
  Lake Peipus (*Peipsi järv*), the bogs, the forests and the coast; and now and then Helsinki,
  Riga (*Riia*) or Stockholm (*Stockholm*), where Estonians live and Estonian is spoken at home:
  home, family, food, shopping, work, school, the town, health, free time, travel, the seasons,
  the summer house in the country (*maakodu*, *suvila*: outside words in the low bands) and the sauna.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ta* is whenever it could be unclear
  (Estonian *tema*/*ta* is "he" and "she" alike, so name the person more often than English would).
  Give each story its own names: the learner reads the course in one go, and two Katis in two
  lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order.
  Estonian puts the verb second in a main clause: *Kati ostab leiba*, but *Täna ostab Kati leiba*,
  *Siis läheb Siim koju*. A sentence that says where something is or that someone has something
  starts with the place or the owner (*Laual on raamat*, *Katil on auto*, *Majas on kaks tuba*). The
  pronoun stays in the first and second person (*Ma lähen koju*, *Kas sa tuled?*); the short forms
  *ma*, *sa*, *ta*, *me*, *te*, *nad*, *mu*, *su* are standard and are the usual ones; the long
  forms (*mina*, *sina*, *tema*, *meie*, *teie*, *nemad*, *minu*, *sinu*) are for contrast or
  emphasis (*Mina ei tule*, *See on minu raamat, mitte sinu*). Little dialogue; when there is some,
  short and simple, with *sina* among family, friends and children, and the polite *teie* (+ the
  plural verb: *Kas te saaksite aidata?*) with a stranger, an older person, in a shop, an office or
  a doctor's room; Estonians say *teie* more than Finns do. In a letter or an e-mail the polite
  pronoun is capitalised (*Teie*, *Teile*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense, affirmative and negative (*läheb*, *ei lähe*, *ma ei tea*;
    the negative is *ei* + the stem in every person); *olema* with the local cases for where
    someone is (*on kodus*, *on Tartus*, *on tööl*) and with the adessive for having (*Katil on
    auto*, *Mul on aega*); the genitive of owners and of names (*Kati auto*, *minu raamat*, *mu
    ema*); the partitive after numbers, in negation and for "some" (*kaks last*, *pole aega*, *ostab
    leiba*, *palju inimesi*); the six local cases in their plain senses (*majas*, *majast*,
    *majja*/*majasse*; *laual*, *laualt*, *lauale*) and the comitative *-ga* (*emaga*, *autoga*);
    yes/no questions with *kas* (*Kas sa tuled?*, *Kas Kati on kodus?*; Estonian has no question
    suffix) and the question words (*mis*, *kes*, *kus*, *kuhu*, *kust*, *miks*, *kuidas*,
    *millal*, *kui palju*); the simple past (*läks*, *tuli*, *oli*, *ütles*, *tegi*, *sõi*) from the
    first courses, in short sentences; numbers as digits;
  - ranks 301–600: the imperative (*tule*, *mine*, *võta*, *istu*; *ära mine*, *tulge*, *ärge
    muretsege*); the total and partial object (*Kati ostab raamatu* / *Kati loeb raamatut*; the
    object of a negated verb is always partitive); the plural cases (*majades*, *poodidesse*,
    *lastele*); comparatives (*suurem kui*, *parem*, *rohkem*); the common postpositions (*juures*,
    *juurde*, *juurest*, *pärast*, *ees*, *taga*, *kõrval*, *peale*, *kaudu*, *vastu*); the two
    infinitives: *-ma* after verbs of going, starting and must (*lähen sööma*, *hakkab sadama*,
    *pean minema*) and its cases (*olen söömas*, *tulen söömast*), *-da* after wanting, being able
    and liking (*tahan minna*, *võin aidata*, *oskab ujuda*, *meeldib lugeda*); *meeldima* with the
    adessive (*Katile meeldib kohv*);
  - ranks 601–1000: the perfect and pluperfect (*on teinud*, *oli teinud*); the conditional
    (*tahaksin*, *kas sa võiksid*, *oleks hea*); the impersonal for "people do, one does" (*Siin
    räägitakse eesti keelt*, *Suvel minnakse maale*); the relative pronouns *mis* and *kes* in
    their cases (*maja, kus me elame*; *mees, keda ma nägin*; *raamat, mida ma loen*); the essive and
    translative (*töötab õpetajana*, *saab õpetajaks*, *läheb külmaks*); the terminative, abessive
    and comitative (*õhtuni*, *rahata*, *sõbraga*); the clitic *-gi*/*-ki* (*minagi*, *keegi*,
    *mitte kunagi*, *seegi*); reported speech with *et* (*Kati ütleb, et ta tuleb*);
  - ranks 1001–1300: superlatives (*kõige suurem*, *suurim*, *parim*, *kõige rohkem*); the
    impersonal past (*maja ehitati 1950. aastal*); the *-des* form for "while, by doing" (*kodus
    olles*, *tulles*, *naerdes*); time clauses (*kui*, *enne kui*, *pärast seda kui*, *kuni*,
    *niipea kui*);
  - ranks 1301–1600: participles as adjectives (*lugev tüdruk*, *loetud raamat*, *tulev suvi*,
    *väsinud mees*) and the *-mata* form (*ütlemata*, *maksmata arve*); the indefinites with
    negation (*keegi ei tulnud*, *mitte midagi*, *mitte kunagi*, *mitte kuskil*);
  - ranks 1601–2000: the participle constructions for *et* clauses (*nägin teda tulemas*, *ta
    ütles end tulevat* only where natural); the jussive (*tulgu ta homme*); the conditional perfect
    (*oleksin tulnud*); indirect questions (*ma ei tea, kas ta tuleb*; *ta küsis, kus jaam on*);
  - ranks 2001–2500: the rarer cases in their set uses (abessive *rahata*, *põhjuseta*; essive
    *noorena*, *esimesena*; terminative *tänaseni*), the impersonal of all tenses, longer sentences
    joined with *kuigi*, *et* ("so that"), *sest*, *kuna*, *kui ainult*, *selle asemel et*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *siiski*, *seega*,
    *seetõttu*, *teisisõnu*, *seevastu*, *lisaks*, *pealegi*; the quotative mood for reported
    facts where it is idiomatic (*ta olevat haige*, *rong pidavat hilinema*); common idioms explained
    by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*kolumn*, a light reflection on everyday life),
    dialogue that marks *sina* and *teie*, and less common connectors (*kuigi*, *nii et*, *sellest
    hoolimata*, *kokkuvõttes*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style (*-mine*, *-us* nouns) used
    with care; long sentences that stay easy to parse; tone and register chosen to suit the text
    type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*sest*, *kui*, *kuigi*, *et*, *aga*, *nii et*),
  dialogue can carry more of the story, and a story may be told by a first-person narrator or as
  an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short informative or
  opinion text (a column, a letter to a newspaper, a short report), as long as it is everyday, calm
  and easy to follow. Still one clear idea at a time: a learner at the level should follow every
  sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Siim tuleb alati esimesena. Siis tuleb Kati ja siis tulevad lapsed.*).
- **Questions at the end**: after the story, a paragraph starting `Küsimused:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *miks* and *kuidas*, with fuller answers):

  ```
  Küsimused:
  Kus Kati ja Siim elavad?
  Nad elavad Tartus.
  Kas maja on suur?
  Ei, maja ei ole suur, aga see on hea.
  ```

  The header `Küsimused:` is layout: the checker does not count it as a word.
- **Standard written Estonian** (*kirjakeel*), as a careful native writer writes it today, in
  narration and in dialogue alike: no spoken or dialect forms. The short pronouns (*ma*, *sa*,
  *ta*, *me*, *te*, *nad*, *mu*, *su*, *mul*, *sul*, *tal*, *mulle*, *sulle*, *talle*) are standard
  and welcome. Never *okei*, *tsau*, *tšau*, *mõnus* as "cool", *lahe* as "cool", *äge* as "cool",
  *kutt*, *tšikk*, *issi*, *emme*, *jah* spelled *jaa*, *nüüd* spelled *nyyd*, *vä* for *või*, *ei
  tea* shortened, and no Võro, Seto, Mulgi or island dialect forms (*ma olõ*, *miä*). No slang and
  no vulgar words (the list, counted from film subtitles, has many: skip them). Every *õ*, *ä*, *ö*
  and *ü* is written, and every double letter: the checker compares letters exactly, and they tell
  words apart (*kool* "school" / *kõõl*; *linna* "town's" / *lina* "linen"; *kala* / *kalla*; *sada*
  "hundred" / *saada* "to get"; *tuli* "came, fire" / *tulli*; *jää* "ice, stay!" / *ja*; *kõik* /
  *koik*). Write *š* and *ž* in loanwords as Estonian does (*šokolaad*, *garaaž*). Days, months,
  languages and nationalities are written in lower case (*esmaspäev*, *mai*, *eesti keel*,
  *eestlane*); names of places and people with a capital (*Tartu*, *Kati*, *Eesti*).
- **Compounds are one word** in Estonian (*raamatupood*, *bussipeatus*, *hommikusöök*,
  *suvekodu*). A compound counts as itself: at its own rank when the list has it as an entry, and
  as an outside word when it does not, even if both its parts are list words (the checker never
  splits a compound, nor a word with a hyphen). So use compounds the list has (`rank.py` tells),
  and otherwise write the idea with two list words where Estonian allows it (*Kati auto*, not
  *pereauto*), or keep the compound as one of the lesson's few outside words.
- **Estonian typography**: direct speech in the Estonian quotation marks „…“ (low opening, high
  closing), each speaker's line a paragraph of its own; the narrator's comma goes after the closing
  mark, a question or exclamation mark of the quoted sentence inside it:

  ```
  „Maja on hea,“ ütleb Kati.
  „Mis see on?“ küsib Siim. „Kas ma saan selle?“
  ```

  (The comma of a quoted statement goes inside the marks, before „ütleb Kati“; the verb comes
  before the speaker after a quote.) The questions and answers of the final `Küsimused:` block
  are plain lines, as above. A comma goes before every subordinate clause (*et*, *kui*, *sest*,
  *kes*, *mis*, *kus*, *kuigi*). Inside a sentence a dash is the en dash with spaces ( – ). Write
  numbers as digits (*3 last*, *kell 7*, *kell 7.30*, *1992. aastal*), ordinals with a full stop
  (*1. mai*, *5. klass*), and an ending joined to a number or an abbreviation with a hyphen (*5-le*,
  *1990-ndatel*, *SMS-iga*, *EL-is*: the checker reads these as the number or the abbreviation),
  except a number word that is in your band: spell that one out to teach it (*kaks last*).
- **Names are names**: place names, adjectives made from place names (*tallinlane*, *tartlane*,
  *eestlane*, *soomlane*) and words for peoples or languages (*eesti*, *soome*, *vene*, *saksa*,
  *inglise*) count as names: skip them as band words even when the list has them as entries; a text
  may still use one (*räägib eesti keelt*), counted at its rank. Never use a list word as a
  character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*jumal*, *issand*), church and chapel, priests, prayer, heaven
  and hell (*põrgu*), angels and devils (*kurat*, *pagan*), souls and spirits (*hing*, *vaim*),
  witches (*nõid*), miracles (*ime*), curses (*needma*), and every custom or holiday of religious
  origin, even as a date, a school break or a gift season: Christmas (*jõulud*, *jõuluvana*),
  Easter (*lihavõtted*), Midsummer (*jaanipäev*, *jaaniõhtu*, St John's day, with its bonfires),
  Shrovetide (*vastlapäev*), All Souls' (*hingedepäev*), St Martin's and St Catherine's days
  (*mardipäev*, *kadripäev*), confirmation (*leer*) and name days. Folk customs with no church
  holiday named are fine: a birthday, New Year's Eve as a family evening, a summer evening at the
  summer house, the sauna, berry and mushroom picking, the first day of school (*1. september*,
  *teadmistepäev*), a market (*turg*, *laat*), a song or a food festival. Never visit a church, and
  use *uskuma* only as believing a person or a fact;
- **politics**: the state (*riik*), the government (*valitsus*), parliament (*Riigikogu*), laws
  (*seadus*), elections, parties, ministers, presidents, kings and queens (*kuningas*,
  *kuninganna*), power (*võim*), freedom as a political word (*vabadus*), nationalism,
  independence and Independence Day (*iseseisvuspäev*, *vabariigi aastapäev*), the Song Festival as
  a national event (*laulupidu* as a plain singing festival is fine), the Soviet period, flags,
  language politics, and borders as disputes; everyday civic life (a library, a school, the town
  council's office, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts**, military service (*armee*,
  *ajateenistus*) and ranks (*kapten*, *kolonel*, *kindral*, *leitnant*, *seersant*, *sõdur*)
  included, and hunting (a lost-property office or a crossing guard at a school is fine; theft,
  arrests, prisons and trials are not); the lake and the sea stay mild (a storm keeps the boats
  ashore; nobody falls through the ice or is lost);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual; insults (*loll*, *idioot*,
  *rumal* about a person); kissing, divorce and romance plots;
- **alcohol** in every form (beer *õlu*, wine *vein*, vodka *viin*, *hõõgvein*, a sauna beer, a bar
  visit for a drink), **gambling** (lotteries, betting, casinos), drugs and smoking (*suits*,
  *sigaret*);
- **deliberate misspellings**: no spoken forms, no spellings without *õ*, *ä*, *ö* and *ü*, not even
  in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Estonian first names are fine as characters.

The themes keep to this: the history courses (*Käegakatsutav ajalugu*, *Arheoloogia ja minevik*,
*Maadeavastajad ja rändurid*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, rulers, occupations or politics; *Rahvapeod ja laadad* and *Rahvakunst ja
kombed* are folk and family celebrations with no church holiday or saint named (no *jaanipäev*, no
*mardipäev*); *Avalikud teenused* is the library, the health centre and the town's service desks,
never politics; *Eesti keel* and *Sõnad ja nende päritolu* are about words, dialects and learning,
never about language politics; *Muinasjutud ja legendid* tells folk tales (clever animals, giants
like *Suur Tõll* or *Kalevipoeg* only as gentle strongmen, the forest's creatures) without gods,
devils, spirits or magic.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *jumal* 102, *issand* 392, *põrgu* 459, *püha* 493 (holy; also "holiday", mostly the
  church ones), *pagan* 523, *hing* 537, *isand* 642, *vanduma* 646, *ime* 805, *vaim* 871, *kirik*
  886, *nõid* 999, *needma* 468, *kurat* 95, *koletis* 829 (monster); *taevas* 596 only as the sky;
- politics: *riik* 545, *rahvas* 481, *kuningas* 470, *kuninganna* 880, *võim* 695, *valitsus*
  750, *seadus* 759, *vabadus* 973, *kohus* 618 (court; also "duty": skip it), *advokaat* 797;
- war, crime, police, courts: *tapma* 89, *relv* 256, *lööma* 258, *päästma* 283, *politsei* 356,
  *tulistama* 375, *kapten* 413, *võitlema* 416, *varastama* 448, *sõda* 457, *ründama* 511,
  *agent* 527, *vang* 591, *röövima* 593, *mõrv* 617, *vangla* 631, *vaenlane* 632, *hävitama* 626,
  *kolonel* 666, *jälitama* 672, *sõdur* 679, *kindral* 688, *süüdistama* 698, *ohver* 709, *rünnak*
  794, *süütu* 801, *süü* 371, *lõks* 812, *pomm* 814, *peksma* 828, *politseinik* 866, *tõend* 872,
  *petma* 878, *tapmine* 895, *leitnant* 904, *võitlus* 930, *uurija* 938, *missioon* 943, *armee*
  956, *sihtmärk* 970, *seersant* 980, *võmm* 986;
- death: *surema* 162, *surnud* 181, *surm* 269, *veri* 425, *laip* 700, *matma* 955;
- vulgar, sexual, insults, romance: *perse* 262, *loll* 478, *pask* 629, *idioot* 668, *rumal* 659,
  *sitt* 691, *suudlema* 733, *tagumik* 843, *seks* 897, *kihlama* 913, *jobu* 922, *keppima* 974,
  *värdjas* 967;
- alcohol: *viin* 725, *õlu* 803, *baar* 991;
- names, place adjectives, peoples and languages: none below 1000 (*inglise* 1046, *vene* 1648,
  *saksa* 1662, *soome* 4231, *eesti* 5600 come later); *Emma* is a form of *ema* (88) and *Toomas*
  of *tooma* (139): never use them as names;
- spoken forms, list noise and forms of other words: *jah* 23 is fine, but *jaa* 622, *okei* 310,
  *no* 326 (a filler; fine in dialogue, not worth a lesson), *noh* 231, *oh* 122, *hei* 134, *ah*
  388, *oi* 706, *oo* 763, *uh* 964, *vau* 990, *halloo* 498, *hallo* 995, *kutt* 280, *söör* 300
  (subtitle "sir"), *kullake* 320, *jama* 391, *lahe* 432 (spoken "cool"), *semu* 496, *daam* 541,
  *preili* 557, *värk* 602, *issi* 612, *boss* 658, *emme* 689, *kraam* 987; *mõlema* 319 is the
  headword of *mõlemad* (teach it as that word), *aim* 524 lives in *pole aimugi*, *ase* 597 in
  *asemel* "instead of", *tasa* 894 in *tasa* "quietly, even"; *tuli* 810 "fire", *jää* "ice" and
  *sai* "bread roll" are words of their own whose spelling is mostly *tulema*'s, *jääma*'s and
  *saama*'s (see below).

Further on, the same categories hold; for example *jõulud*, *jaanipäev*, *lihavõtted*, *kirik*,
*preester*, *palve*, *ingel*, *vein*, *suits*, *sigaret*, *vangla*, *varas*, *mõrvar*,
*matused*, *haud*, *lipp*, *valimised*, *partei*, *president*, *jahimees*.

## The file

Write `tools/courses/et/<id>.json`:

```json
{
  "id": "et-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Estonian, e.g. Toit ja poeskäik",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Estonian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/et/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *läheb* or *läksime* for *minema*, *last* or *lastele* for
  *laps*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  the *-ma* infinitive of a verb (*minema*, *tegema*), the nominative singular of a noun and an
  adjective (*laps*, *uus*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, a spoken form, a word of the content rules); the
  checker's last line lists the band words not taught yet.

Things to know about the Estonian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/et/wordlist.tsv`
  (a copy of the app's `et.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  film subtitles (FrequencyWords, OpenSubtitles 2018), so talk words rank high and some everyday
  written words rank low; war, crime, police and swearing words rank high and are skipped.
- **How the checker reads an Estonian word.** A word the list (with `extra_forms.tsv`) has is read
  as it is. A word it lacks is read without its clitic *-gi*/*-ki* (*minagi* is *mina*, *sealgi*
  is *seal*, *kellelgi* is *kellel*); Estonian has no possessive suffixes. A name in `names`
  counts in all its case forms (*Kati*, *Katile*, *Katiga*, *Katit*; *Tallinn*, *Tallinna*,
  *Tallinnas*, *Tallinnasse*; *Saaremaa*, *Saaremaal*; *Peipus*, *Peipsi*, *Peipsil*), with the
  clitic too, even with up to two letters of its stem changed. A name the checker still misses (a
  long stem change) can go into `names` in that form too. A name of several words or with a hyphen
  (*Peipsi järv*, *Ida-Virumaa*) counts as one word wherever it is written whole; its case forms
  count by its parts. *Eesti* in `names` counts for the lowercase *eesti* of *eesti keel* too.
- `extra_forms.tsv` holds the standard forms of the list's words that the list lacks (the list
  keeps at most 40 forms per word): the 14 cases in both numbers, the verb forms, and the
  declension of the participles, verbal nouns and comparatives the list files under their word
  (*tehtud*, *tegev* of *tegema*; *parema* of *hea*), from the English Wiktionary's Estonian data
  (`dictionaries/kaikki.org-dictionary-Estonian.jsonl.gz`), without the forms Wiktionary marks as
  dialectal, colloquial, rare or archaic. It is made by
  `/Users/zavanton/TabbyProjects/tayra-writers/et-writers/make_extra_forms.py`. Wiktionary's
  Estonian is thinner than its Finnish: if a true form is missing, write around it and name the
  word and form in your report.
- **Pronouns and little words are entries of their own**: *mina* 2, *sina* 4, *tema* 6, *meie*
  10, *teie* 13, *nemad* 15 hold their short forms and cases (*ma*, *mu*, *mul*, *mulle*, *sa*,
  *sul*, *ta*, *tal*, *talle*, *me*, *meil*, *te*, *nad*, *neil*); *see* 3 holds *need* and their
  cases (*seda*, *selle*, *sellest*, *neid*); *mis* 9 holds *mida*, *mille*, *millest*; *kes* 30
  holds *keda*, *kelle*; *miski* 31 holds *midagi*; *keegi* 56 holds *kedagi*; *ise* 36 holds the
  reflexive *end*, *enda*, *ennast*, *endale*, *enese*; *ei* 5 holds the negative imperative *ära*,
  *ärge*; *olema* 1 holds *on*, *pole*, *oli*, *polnud*, *oleks*. Adverbs and postpositions are
  entries apart from the noun they come from: *kodu* 133 holds *kodus*, *koju*, *kodust*; *kokku*
  242 ("together") is apart from *kogu* 144; *ajal* is *aeg*'s (46); *välja* 47, *tagasi* 57,
  *sisse* 142, *üles* 138, *alla* 177, *ette* 203, *juurde* 180, *juures* 313, *peale* 92, *pärast*
  80, *eest* 81, *kohta* 171, *jaoks* 157, *kaasa* 221, *ees* 351, *taga* 355, *peal* 358, *väljas*
  377, *ümber* 389, *sees* 433, *kõrval* 775, *all* 285 are entries of their own.
- **Words filed under a commoner word spelled the same**, which the checker counts at that word's
  rank, so they look known early; they are never band words at that rank, and below rank 1000 use
  them only where the story makes their meaning plain: *tuli* "fire" (its spelling is *tulema*'s
  past; *tuld*, *tules*, *tulega* are *tuli* 810's), *jää* "ice" (*jääma*'s imperative; *jääl*,
  *jääs* are *jää*'s), *sai* "bread roll" (*saama*'s past; *saia* is *sai*'s), *meil* "e-mail"
  (*meie*'s adessive; *meili*, *meilid* are *meil*'s), *tee* "road" and "tea" (one entry, 58; *tee*
  is also *tegema*'s imperative), *kuul* "bullet, ball" (also *kuu*'s adessive), *maal* "painting"
  (also *maa*'s adessive "in the country"), *looma* "to create" (also *loom*'s genitive), *ala*,
  *alus*, *vara*, *varas*, *kana*, *kanal*. A verb's participles, verbal noun and *-matu* adjective
  are filed under it (*tehtud*, *tegemine*, *elav*, *ootamatu*), and so are the comparatives and
  superlatives (*parem*, *parim*, *suurem*); they are no band words of their own. The exceptions
  are entries of their own: *vanem* 394 ("parent", *vanemad* "parents"; the spelling *vanem* "older" also counts as *vana*'s, 215), *enam* 115 ("anymore"),
  the ordinals (*kolmas* 687, *neljas*, *viies*...), and the *-nud*/*-tud* adjectives the list has
  as entries (*surnud*, *väsinud* 796, *ülejäänud* 690).
- **Homographs and shared spellings**: some entries hold two words (*tee* "road" and "tea", *kord*
  "time, once" and "order", *pea* "head" and "must" (*pead* is also *pidama*'s), *iga* "every" and
  "age", *kuu* "month" and "moon", *liiga* "too" and "league"), and a spelling shared by two
  entries counts for both (*looma* is *loom*'s genitive and "to create"; *kutsu* is *kutsuma*'s
  imperative; *kutsu* "puppy" is a rare entry). Use a new word in its own sense, never through its
  homograph, and count a new word's uses in forms that are its own (*looma* "create" through
  *loob*, *lõi*, *loodud*; *tuli* "fire" through *tuld*, *tules*).
- **Names** are not in the list: people, places (*Tartu*, *Pärnu*, *Narva*), shops and brands go
  in `names`. Keep only true names in `names`, never common words, and never a name that is also a
  list word: as characters avoid *Emma* (*ema*), *Toomas* (*tooma*), *Mari* (berry), *Marju*,
  *Jaan*, *Malle*, *Pille*, *Tuuli* (*tuul*), *Hele*, *Heli* (*heli* "sound"), *Mati*, *Kaarel*,
  *Kalju*... check every name with `rank.py` before you use it. Safe in the pilot: *Kati*,
  *Siim*, *Liis*, *Kadri*, *Mart*, *Tõnu*, *Peeter*, *Andres*, *Kristjan*, *Martin*, *Rasmus*,
  *Karl*, *Mihkel*, *Triin*, *Kertu*, *Kaisa*, *Maarja*, *Helen*, *Reet*, *Ülle*, *Tiiu*, *Priit*,
  *Margus*, *Indrek*, *Ott*, *Marko*, *Hendrik*, *Anu*, *Laura*, *Grete*, *Liina*, *Kärt*, *Urmas*,
  *Tiit*, *Katrin*, *Ants*, *Lauri*, *Madis*, *Joonas*, *Oskar*, *Mia*, *Sofia*, *Hugo*, *Robin*,
  *Elsa*; *Tallinn*, *Tartu*, *Pärnu*, *Narva*, *Viljandi*, *Saaremaa*, *Hiiumaa*, *Kuressaare*,
  *Haapsalu*, *Rakvere*, *Võru*, *Paide*, *Otepää*, *Muhu*, *Kihnu*, *Lahemaa*, *Soomaa*,
  *Emajõgi*, *Peipus*. Day and month names are in the list but rank late (*reede* 1437, *laupäev*
  1528, *esmaspäev* 1701, *pühapäev* 1749, *teisipäev* 2129, *neljapäev* 2194, *kolmapäev* 2818;
  *mai* 3171, *juuni* 3277, *jaanuar* 3734; *august* also counts as *auk*'s elative); the seasons
  too (*suvi* 1170, *talv* 1941, *kevad* 2476, *sügis* 3620).
- Everyday words rank later than you would think: *maja* 205, *kodu* 133, *ema* 88, *isa* 79,
  *laps* 113, *sõber* 120, *koer* 431, *kass* 936, *kool* 374, *toit* 542, *vesi* 316, *kohv* 664,
  *leib* 1997, *piim* 1628, *pood* 734, *raamat* 440, *auto* 164, *buss* 977, *rong* 901, *suur* 86,
  *väike* 141, *ilus* 281, *aitäh* 193, *tere* 82, *vabandust* (*vabandus* 147), *tuba* 372, *laud*
  578, *aken* 740, *uks* 293, *köök* 1135, *meri* 966, *mets* 767, *järv* 1592; the dialogue tag
  *ütlema* 26 is in the first band, *küsima* 176 and *vastama* 338 come later.
- The list's form lists are noisy (look-alikes, subtitle spellings); a new word must appear as
  itself or a true inflection, never through such a form. Some entries are names, English words or
  interjections: skip them. Loanwords Estonian uses as its own (*video*, *internet*, *film*,
  *telefon*, *arvuti*) are taught like any word.
