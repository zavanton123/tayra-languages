# Writing a Romanian frequency course

Each course teaches one band of 100 words from the Romanian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ro-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ana nu găsește telefonul. Merge în bucătărie. Nu e acolo. Merge în
  cameră...* Everyday life where Romanian is spoken: București, Cluj, Iași, Brașov, Sibiu,
  Timișoara, Craiova, Oradea, Suceava, Sinaia and the Prahova valley, the Black Sea coast and
  Mamaia, the Danube Delta, the Carpathians, the villages of Maramureș and Bucovina; Chișinău; and
  now and then London, Madrid or Rome, where Romanian is spoken at home: home, family, food,
  shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *el*/*ea* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Andreis
  in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Romanian drops subject pronouns: write *el*, *ea*, *eu*, *tu* only where
  Romanian would (contrast, emphasis, a new subject). Little dialogue; when there is some, short
  and simple, with *tu* among family and friends and *dumneavoastră* (+ the second person plural:
  *Dumneavoastră locuiți aici?*) with strangers, older people and in shops or offices; *domnul
  Popescu*, *doamna Ionescu*.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense of every kind of verb (*vorbește*, *merge*, *vine*, *face*,
    *lucrează*, *citește*), *a fi* and *a avea* (*e acasă*, *are un câine*), the definite article
    joined to the noun (*casa*, *omul*, *copilul*, *copiii*) and the genitive-dative in its plain
    uses (*casa Anei*, *mama lui Andrei*, *îi dă mamei*); *a vrea* / *a putea* / *trebuie* + *să* and
    the subjunctive (*vrea să meargă*, *poate să vină*, *trebuie să plece*, *să fie*, *să aibă*); the
    **perfect compus** for what happened, from the first course (*a mers*, *am văzut*, *au venit*,
    *a fost*), as Romanian tells every story in it; the future with *o să* (*o să plouă*) and with
    *va* (*va veni*); the weak pronouns in their places, joined by a hyphen where Romanian joins them
    (*îl vede*, *o caută*, *îi spune*, *mi-a dat*, *s-a dus*, *l-a găsit*, *nu-i*, *n-are*); the
    reflexive (*se numește*, *se uită*, *se duce*); *e* beside *este*; simple questions (*ce*,
    *cine*, *unde*, *cum*, *când*, *de ce*, *cât*);
  - ranks 301–600: the imperative (*vino*, *spune*, *uită-te*, *du-te*, *dă-mi*, *spune-mi*; *nu
    pleca*, *nu te uita*), the possessives (*casa mea*, *prietenul meu*, *al meu*, *a ei*), the
    vocative of names and family words (*Mamă!*, *Andrei!*), comparatives (*mai mare decât*, *cel
    mai bun*), *pentru că*, *când*, *dacă*, the time (*la ora cinci*, *la 7.30*);
  - ranks 601–1000: the **imperfect** for background and habits (*era frig*, *când era mic,
    locuia la Sibiu*) beside the perfect compus for events; *aș vrea*, *ar fi bine* as polite set
    phrases; the relatives *care*, *pe care*, *unde*; the gerund (*mergând*, *uitându-se*);
    reported speech with *că* (*spune că vine*); *a-i plăcea* (*îmi place*, *le plac merele*);
  - ranks 1001–1300: the **conditional** (*ar merge*, *ar trebui*, *ar fi mai bine*), *dacă* +
    present for real conditions (*dacă plouă, rămânem acasă*), superlatives (*cel mai înalt*, *cea
    mai bună*), the double weak pronouns (*mi-l dă*, *i-am spus*, *ți-o aduc*, *le-am arătat*), *al
    cui*, *căruia*, *căreia*;
  - ranks 1301–1600: the **subjunctive** after its usual triggers (*vreau să vii*, *înainte să*,
    *fără să*, *ca să*, *până să*, *e bine să*), the indefinites with double negation (*nu vine
    nimeni*, *nu e nimic*, *niciun*), *deși*, *în loc să*, *în afară de*;
  - ranks 1601–2000: the **mai-mult-ca-perfect** (*plecase*, *ajunsese*), the past conditional in
    *dacă* sentences (*dacă aș fi știut, aș fi venit*), the passive with *a fi* in plain cases
    (*podul a fost construit în 1920*) and the passive *se* (*aici se vând flori*, *se spune că*),
    indirect questions (*nu știe dacă vine*, *întreabă unde e*), *ca și cum*;
  - ranks 2001–2500: the past subjunctive (*trebuia să fi plecat*), reported speech with the
    tenses in their places (*a spus că o să vină*), *în timp ce*, *din cauza*, *datorită*,
    *potrivit*, *în ciuda*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *totuși*, *însă*, *prin
    urmare*, *așadar*, *cu toate acestea*, *în ceea ce privește*, *de altfel*; gerund clauses; the
    **perfect simplu** of written narration (*merse*, *zise*, *făcu*) for recognition in
    third-person written narration only, as some books use it; common idioms made clear by their
    context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (a light reflection on everyday life), dialogue that
    marks *tu* and *dumneavoastră*, and less common connectors (*ba chiar*, *de vreme ce*, *astfel
    încât*, *cu cât… cu atât*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style used with care; long
    sentences that stay easy to parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*pentru că*, *când*, *în timp ce*, *deși*,
  *deci*, *dar*, *așa că*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Merge în bucătărie. Nu e acolo. Merge în cameră. Nu e nici acolo.*).
- **Questions at the end**: after the story, a paragraph starting `Întrebări:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *de ce* and *cum*, with fuller answers):

  ```
  Întrebări:
  Ce nu găsește Ana?
  Ana nu găsește telefonul.
  Unde e telefonul?
  Telefonul e în mașină.
  ```

  The header `Întrebări:` is layout: the checker does not count it as a word.
- **Standard Romanian**: the narration is the standard language as the Romanian Academy spells
  it today (the 1993 rules, DOOM): *â* inside a word (*când*, *România*, *mâine*, *sunt*, *suntem*),
  *î* at the start and end of a word and of each part of a compound (*început*, *a urî*,
  *neîncredere*); never *sînt*, *cînd*, *mîine*. Always write the Romanian letters *ă â î ș ț*,
  and *ș* and *ț* with the **comma below** (U+0219, U+021B), never the cedilla *ş ţ* (the checker
  reads both, the app shows what you write). Neutral, correct and natural Romanian as written
  today, also in stories set in Moldova: no regional forms (*o fost*, *îs*, *amu*, *ghini*), no
  archaic spellings, no slang and no vulgar words (the list has some: skip them). Dialogue may be
  lightly colloquial only in what any careful speaker says (*hai*, *bine*, *gata*, *păi*, *mersi*).
  Hyphens as Romanian writes them: *s-a*, *mi-a*, *l-am*, *n-am*, *într-o*, *dintr-un*, *printr-o*,
  *dă-mi*, *uită-te*, *du-te*, *spune-mi*, *nu-i*, *ce-i*.
- **Romanian typography**: dialogue starts each line with a dash (*linia de dialog*, the em dash
  —, followed by a space); the narrator's words follow after a comma or with the speaker's verb
  after the question or exclamation mark; each speaker's line is a paragraph of its own:

  ```
  — Bună, Ana! spune Mihai.
  — Bună! Vrei o cafea? întreabă Ana.
  ```

  A word or title quoted inside a sentence goes in Romanian quotation marks „…” (low, then high).
  Never straight or curly single quotes around words (the reader counts an apostrophe as part of a
  word). Inside a sentence a dash is the en dash with spaces ( – ); hyphens inside words stay
  hyphens. Write numbers as digits (*3 frați*, *la ora 7*, *la 7.30*, *în 1992*), ordinals as
  words when the band has them (*a doua zi*, *al treilea*) or with digits as Romanian writes them
  (*al 2-lea*), centuries in Roman numerals (*secolul al XIX-lea*: counted as numbers), except a
  number word that is in your band: spell that one out to teach it (*doi frați*).
- **Letters**: use only the letters of the Romanian alphabet in a word (*a–z*, *ă â î ș ț*). The
  app's reader cuts a word at any other letter (*café*, *Zoë*, *Müller*): write *cafenea*, or a
  name without such letters.
- **Names are names**: place names, adjectives made from place names (*român*, *românesc*,
  *european*, *american*, *german*, *francez*, *moldovenesc*) and words for peoples or languages
  count as names: skip them as band words even when the list has them as entries; a text may
  still use one (*vorbește românește*, *limba română*), counted at its rank. Never use a list word
  as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and monastery, priests, monks, icons, prayer,
  fasting, heaven and hell, angels and devils, and every custom or holiday of religious origin,
  even as a date, a school break or a gift season: Christmas (*Crăciun*, *Moș Crăciun*, carols,
  *colinde*, the tree), Easter (*Paște*, painted eggs, *cozonac* as the Easter cake), *Bobotează*,
  saints' days and name days (*Sfântul Andrei*, *Sânziene*, *Sfântul Ilie*), *Halloween*, the
  church wedding and christening; a village *hram* (a patron saint's feast) is avoided too. Folk
  customs with no church holiday or saint named are fine: the *Mărțișor* on 1 March, a folk dance
  (*hora*), a fair or market (*târg*, *piață*), a birthday, New Year's Eve (*Revelion*) as a family
  evening, the start of the school year, the summer holidays, a civil wedding at the town hall.
  Place names such as *Sfântu Gheorghe* are names, but prefer towns without a saint in their name,
  never visit a church or a monastery, use *cer* only as the sky, *a ruga* only as "to ask" and in
  *te rog*, *vă rog* ("please"), and *post* only as a post or a station;
- **politics**: the state, the government, parliament, elections, parties, ministers,
  presidents, kings and rulers, nationalism, borders as disputes, revolutions, the communist
  regime and the *Securitate*; everyday civic life (a library, a school, the town hall's counter,
  a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die; no accidents;
- **vulgar or sexual words**, and words whose main sense is sexual or vulgar (*naiba*, *dracu*);
- **alcohol** in every form (wine, beer, *țuică*, *pălincă*, a toast, a wine cellar, a bar visit
  for a drink), **gambling** (lotteries, betting, casinos), drugs and smoking;
- **deliberate misspellings**: no spellings without diacritics, no *sînt*, no regional or
  colloquial spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Romanian first names are fine as characters.

The themes keep to this: the history courses (*Istoria de lângă noi*, *Arheologie și trecut*,
*Exploratori și călători*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, rulers or politics; *Târguri și festivaluri populare* and *Artă și obiceiuri
populare* are folk and family celebrations with no church holiday or saint named; *Servicii
publice* is the library, the clinic and the town hall's counters, never politics; *Limba română*
and *Regiuni și graiuri* are about words, regional speech and learning, never about language
politics or peoples; *Basme și legende* tells folk tales without saints, devils or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *biserică* 292, *sfânt* 365, *suflet* 597, *păcat* 726 (also in *ce păcat*), *preot*
  911, *creștin* 924, *ortodox* 959, *zeu* 972; *ruga* 134 only as "to ask" and in *te rog*, *vă
  rog*; *post* 580 only as a job, a post or a station; *cer* 494 only as the sky;
- politics: *național* 206, *stat* 230 (the state; *am stat* counts for *sta*), *putere* 311
  (except strength: *nu mai are putere*), *președinte* 373, *guvern* 413, *lege* 424, *rege* 427,
  *politic* 489, *consiliu* 508 (except "advice": *un sfat* is the everyday word), *politică* 536,
  *partid* 578, *ministru* 586, *republică* 609, *primar* 746, *uniune* 740, *autoritate* 777,
  *popor* 802, *securitate* 836, *regină* 868, *lider* 881, *campanie* 916, *teritoriu* 930,
  *apărare* 933, *parlament* 943, *minister* 947; *libertate* 993 only of a person or an animal;
- war, crime, police, courts: *ucide* 265, *armă* 317, *poliție* 369, *luptă* 370, *război* 378,
  *omorî* 414, *avocat* 438, *proces* 464, *crimă* 556, *militar* 570, *agent* 592, *armată* 606,
  *polițist* 616, *atac* 634, *ordin* 656, *lupta* 659, *victimă* 675, *închisoare* 706, *fura* 748,
  *soldat* 800, *căpitan* 842 (except of a ship or a team), *ataca* 855, *ofițer* 873; *lovi* 526
  only of a ball or a knock (*a lovit mingea*, *s-a lovit la genunchi*);
- death: *muri* 207, *moarte* 290, *mort* 367, *sânge* 485, *accident* 670;
- vulgar: *naiba* 244, *sex* 530, *rahat* 720, *nenorocit* 816, *fund* 824 (also "bottom": write
  around it);
- alcohol: *vin* 411 ("wine"; *vin* "I come" counts for *veni*), *bea* 418 only of water, tea, milk
  or juice;
- names, place adjectives, peoples and languages: *român* 194, *european* 334, *românesc* 436,
  *american* 449, *maria* 751, *rus* 774, *german* 792, *ion* 819 (holds *Ioana*), *francez* 848,
  *tom* 969;
- list noise and forms of other words: *as* 483 (*aș* written without its comma), *pană* 703
  (ranked by *pana*, *până* without its breve), *geac* 771, *cat* 814, *par* 899 (ranked by *par*,
  "I seem"), *pasă* 696 (its count is *îmi pasă*, of *a păsa*; the noun is a pass in football),
  *atâta* 591 (a form of *atât* 125), *prietenă* 770 (the feminine of *prieten* 186), and of
  *dimineața* 718 and *dimineață* 732 teach one only (*dimineața*, "in the morning", through
  *dimineața* and *dimineață* alike).

Further on, the same categories hold; for example *drog* 1001, *vot* 1034, *crăciuni* 1070 (holds
*Crăciun*), *judecător* 1195, *bere* 1211, *evreu* 1285, *mănăstire* 1315, *bombă* 1340, *cruce*
1391, *iad* 1397, *dracului* 1412, *pariu* 1461, *înger* 1562, *sabie* 1667, *mormânt* 1715,
*pistol* 1721, *catolic* 1770, *diavol* 1793, *tribunal* 1836, *hoț* 1839, *fuma* 1887, *glonț*
1930, *ucigaș* 1936, *beat* 1956, *papă* 1968, *țigară* 2045, *rugăciune* 2086, *curvă* 2214, *rai*
2273, *cimitir* 2411, *furt* 2421, *înmormântare* 2509, *icoană* 2634, *pușcă* 2683, *sinucidere*
2868, *călugăr* 3031, *drac* 3069, *cazinou* 3309, *paște* 3673, *whisky* 4129, *tutun* 4630,
*loterie* 5977.

## The file

Write `tools/courses/ro/<id>.json`:

```json
{
  "id": "ro-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Romanian, e.g. Mâncare și cumpărături",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Romanian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ro/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *merg* or *a mers* for *merge*, *bună* for *bun*, *casa* for
  *casă*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  the infinitive of a verb without *a* (*merge*, *lucra*, *vedea*), the singular of a noun without
  its article (*casă*, *om*), the masculine singular of an adjective (*bun*, *nou*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after. Each piece of a hyphenated word counts
  (*s-a* is two words).
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, a word of the content rules); the checker's last line
  lists the band words not taught yet.

Things to know about the Romanian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ro/wordlist.tsv`
  (a copy of the app's `ro.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it.
- **How the checker cuts words.** As the app's reader does, it splits a word at every hyphen and
  reads *ş ţ* as *ș ț*: *s-a* is *s* + *a*, *mi-a* is *mi* + *a*, *într-o* is *într* + *o*,
  *dintr-un* is *dintr* + *un*, *n-am* is *n* + *am*, *dă-mi* is *dă* + *mi*, *uitându-se* is
  *uitându* + *se*. The cut pieces count as their words: *s* as *se*, *n* as *nu*, *l* as *el*
  (*l-am văzut*), *m* as *mă*, *i* as *îi*, *v* as *vă*, *ți* as *tu*, *într* as *în*, *dintr* as
  *din*, *printr* as *prin*; *mi* 46, *o* 9 and *a* 5 are entries of their own. A name of several
  words or with a hyphen (*Cluj-Napoca*, *Satu Mare*, *Baia Mare*, *Piatra Neamț*) counts as one
  known word wherever it is written whole.
- **The verb entries**: *fi* 2 holds *e*, *este*, *sunt*, *era*, *fost*, *fie*, *fii*; *avea* 7
  holds *am*, *ai*, *are*, *au*, *aș*, *ați* (so *am mers* counts for *avea* and *merge*), but *a*
  5 is its own entry (the preposition, the infinitive's *a* and the *a* of *a mers*); *vrea* 24
  holds the future's *voi*, *vei*, *vom*, *veți*, *vor*, while *va* 42, *o* 9 (*o să*) and *ar* 44
  (*ar fi*) are entries of their own; *poate* 58 ("maybe") is not *putea* 31 (*poate* "can" counts
  for it too); *da* 29 is both "yes" and "to give" (*dă*, *dau*, *dat*); *prim* 91 is taught through
  *primul*, *prima*; *două* 73 holds *doi*; *noi* 74 holds *nouă* ("to us" and "nine": the number
  nine counts at 74). Since the list's builder fix, a verb's past participle counts for the verb
  (*am lucrat* for *lucra*, *a căutat* for *căuta*); some participles are also entries of their own
  with a noun or adjective sense (*trecut* 291 "past", *început* 423 "beginning", *răspuns* 638
  "answer", *venit* 646 "income", *iubit* 486 "boyfriend", *cunoscut* 625 "well-known", *fericit*
  436, *minunat* 395, *potrivit* 574, *greșit* 728, *pregătit* 817, *scris* 1445): teach such an
  entry in its own sense, as a noun or an adjective, never through *am trecut*, *a răspuns*.
- **Pronouns and little words are entries of their own**: *eu* 35 (*mine*, *mie*), *tu* 27
  (*tine*, *ție*, *ți*), *el* 23 (*îl*, *-l*), *ea* 80 (*ele*), *ei* 67, *noi* 74, *voi* 88 (*vouă*),
  *dumneavoastră* 281; *mă* 32, *te* 28, *îi* 30, *îmi* 83, *îți* 130, *își* 268, *mi* 46, *ne* 49,
  *vă* 61, *le* 65, *se* 18, *lui* 48, *lor* 158; *ăsta* 26 (*asta*, *astea*), *acest* 50,
  *acesta* 77 (*aceasta*), *ăla* 172 (*aia*), *acel* 185, *acela* 205, *cel* 38, *al* 19 (*a*, *ai*,
  *ale*), *unul* 81 (*una*), *alt* 103 and *altul* 264.
- **Homographs**: some entries hold two words (*dar* "but" and "gift"; *da* "yes" and "to give";
  *luni* "Monday" and "months"; *mie* 678 "thousand", holding *mii*, and *mie* "to me" of *eu*;
  *fiu* 253 "son" and *să fiu*; *stea* 743 "star" and *să stea*; *cer* 494 "sky" and *(eu) cer*;
  *pui* 1246 "chick, chicken" and *(tu) pui*; *vină* 329 "fault" and *să vină*; *joacă* 1066 "play,
  game" and *se joacă*; *urmă* 184 "trace", *în urmă*, beside the verb *urma* 304; *bază* 332 beside
  the verb *baza* 716; *formă* 361 beside *forma* 520; *scuză* 383 beside *scuza* 626). The checker
  counts a spelling at the lowest rank of the entries that list it, so the verb form looks known
  early (*vin* "I come" counts for *veni* 79, *să stea* for *sta* 96, *se joacă* for *juca* 354,
  *pui* for *pune* 106), and the noun's own forms (*vinul*, *steaua*, *vina*, *puiul*) show which
  word a text means. Use a new word in its own sense, never through its homograph, and count a new
  word's uses in forms that are its own.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms); every
  headword in it must be a headword of the list (the checker warns otherwise). If a true form is
  missing, write around it and name the word and form in your report.
- **Names** are not in the list: people, places (*Cluj*, *Iași*, *Brașov*, *Sibiu*, *București*),
  shops and brands go in `names`. Keep only true names in `names`, never common words, and never a
  name that is also a list word: as characters avoid *Elena* (a form of *elen*), *Ioana* and *Ion*
  (*ion*), *Maria*, *Bogdan*, *Matei* (*mat*), *Sorin*, *Marin*, *Florin*, *Mara*, *Silvia*,
  *Cezar*, *Tom*; as places *Constanța*, *Pitești*, *Alba Iulia* (*alb*) and the *Delta* are list
  words too: they count at their own rank, so leave them out of `names` and use them only as
  outside words, or pick another town. Safe: *Ana*, *Andrei*, *Mihai*, *George*, *Alexandru*,
  *Dan*, *Radu*, *Vlad*, *Cristina*, *Laura*, *Mircea*, *Irina*, *Corina*, *Bianca*, *Daria*,
  *Sofia*, *Luca*, *Ilinca*, *Teodora*, *Ștefan*, *Victor*, *Paul*, *Petru*, *Ionuț*, *Adina*,
  *Raluca*, *Oana*, *Cosmin*, *Tudor*, *Iulia*, *Diana*, *Roxana*, *Lucian*, *Emil*; *București*,
  *Cluj*, *Iași*, *Brașov*, *Sibiu*, *Timișoara*, *Craiova*, *Oradea*, *Suceava*, *Sinaia*,
  *Arad*, *Galați*, *Bacău*, *Ploiești*, *Tulcea*, *Mamaia*, *Chișinău*. Check a name's rank with
  `rank.py` before you use it. Day and month names are in the list (*luni* 362, *vineri* 1863,
  *sâmbătă* 1961, *duminică* 1775, *marți* 3042, *joi* 2636, *miercuri* 2694; *noiembrie* 534,
  *octombrie* 553, *decembrie* 557, *septembrie* 562, *martie* 569, *iunie* 590, *iulie* 668,
  *aprilie* 689, *august* 700, *ianuarie* 741, *februarie* 858; *mai* is the word "more" 15; *vară*
  697, *iarnă* 1177, *toamnă* 1750, *primăvară* 1869).
- Everyday words rank later than you would think: *copil* 108, *nou* 110, *tată* 126, *mamă* 137,
  *casă* 142, *mulțumi* 146 (*mulțumesc*), *fată* 153, *mic* 155, *familie* 169, *prieten* 186,
  *frumos* 193, *oraș* 200, *apă* 220, *mașină* 235, *acasă* 237, *carte* 245, *băiat* 247, *seară*
  250, *cameră* 251, *frate* 263, *școală* 302, *azi* 306, *masă* 410, *bea* 418, *soră* 457, *ușă*
  489, *câine* 745, *salut* 767, *cafea* 809, *revedere* 1027, *bunică* 1294, *bunic* 1339, *pisică*
  1486, *pâine* 1829; the dialogue tag *spune* (37) is in the first band, *zice* 140, *întreba* 262
  and *răspunde* 467 come later.
- The list's form lists are noisy (old spellings with *î* and without diacritics, look-alikes such
  as *mere* "apples" under *merge*, *lea* under *la*); a new word must appear as itself or a true
  inflection, never through such a form. Some entries are names, letters or noise (*as*, *geac*,
  *cat*): skip them. Loanwords Romanian uses as its own (*site*, *video*, *film*, *meci*, *fotbal*,
  *internet*) are taught like any word; interjections that are entries (*hai*, *haide*, *hei*,
  *păi*, *mersi*, *alo*, *pa*) are taught like words, in dialogue.
