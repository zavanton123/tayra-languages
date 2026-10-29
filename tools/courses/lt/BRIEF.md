# Writing a Lithuanian frequency course

Each course teaches one band of 100 words from the Lithuanian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `lt-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Daiva nežino, kur yra Kajus. Ji eina į namą. Ten Kajaus nėra. Ji eina į
  kitą namą...* Everyday life where Lithuanian is spoken: Vilnius, Kaunas, Klaipėda, Šiauliai,
  Panevėžys, Alytus, Marijampolė, Utena, Telšiai, Palanga, Nida, Druskininkai, Birštonas, Anykščiai,
  Kėdainiai, Biržai, Zarasai; the Nemunas and the Neris, the Curonian Spit and the lagoon, the
  lakes of Aukštaitija and Dzūkija's pine forests, the Baltic coast; and now and then Dublin,
  London, Oslo, Chicago, Riga or the Lithuanian villages around Punskas and Seinai, where
  Lithuanians live and Lithuanian is spoken at home: home, family, food, shopping, work, school,
  the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *jis*/*ji* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Tomas in
  two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; Lithuanian word order is free, so keep to the neutral one). Lithuanian
  can drop the subject pronoun after the first mention, and in speech the present of *būti* is
  often left out (*Mes iš Kauno.* *Jam 8 metai.*); both are fine. Little dialogue; when there is
  some, short and simple, with *tu* among family and friends and *jūs* (with the second person
  plural) with strangers, older people and in shops or offices (*Ar galite pasakyti...?*, *ponas
  Petras*, *ponia Kazlauskienė*).
- **Address uses the vocative**: *Tomai, kur tu?*, *Urte, čia tavo vieta.*, *Pauliau!*, *Kajau!*,
  *ponia Ona*, *mama*, *tėti*. Never the nominative in direct address for names that have a
  vocative of their own (*Tomas, eik čia!* is wrong).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense (*yra*, *turi*, *eina*, *nori*, *gali*, *žino*, *sako*) and the
    simple past (*buvo*, *turėjo*, *ėjo*, *matė*, *padarė*, *pasakė*) from the first courses;
    negation with *ne-* joined to the verb (*nežino*, *nėra*, *nebuvo*) and the genitive it takes
    (*neturi laiko*, *nemato Kajaus*, *ten nieko nėra*); the nominative, the accusative of the object
    (*mato jūrą*), the genitive of possession and after *iš*, *nuo*, *iki*, *be*, *prie*, *po*, *dėl*
    (*Ievos namas*, *iš Kauno*, *po darbo*), the locative of place (*Kaune*, *namuose*, *darbe*), the
    instrumental after *su* (*su Pauliumi*), the dative of age and need (*Lukui 8 metai*, *man
    reikia*); *galėti*, *norėti*, *reikėti* + infinitive; simple questions (*ar*, *kas*, *ką*,
    *kur*, *kodėl*, *kaip*, *koks*, *kiek*);
  - ranks 301–600: the future (*bus*, *eisiu*, *padarysime*), the imperative (*eik*, *sakyk*,
    *palauk*, *eikite*, *žiūrėk*), the reflexive verbs and their particle (*mokosi*, *jaučiasi*,
    *vadinasi*; after a prefix or *ne-* it moves inside: *susitinka*, *nesimoko*, *atsiprašau*),
    the plural of every case, *patikti* with the dative (*man patinka*), the comparative (*geresnis*,
    *daugiau*, *labiau*, *nei*/*negu*), *kad*, *kai*, *nes*, *jei* clauses with the present or the
    future (*jei lis, liksime namuose*);
  - ranks 601–1000: the frequentative past for habits (*būdavo*, *eidavo*, *žaisdavome*), *kuris*
    in every case (*namas, kuriame gyvena*), reported speech with *kad* (*sako, kad ateis*), numbers
    with their cases (*du vaikai*, *penki vaikai*, *dešimt vaikų*, *dvidešimt vienas vaikas*), the
    collective numerals with plural-only nouns (*dveji metai*, *treji metai*, *dvejos durys*), the
    instrumental of means and time (*važiuoja autobusu*, *vakarais*);
  - ranks 1001–1300: the conditional (*būtų*, *norėčiau*, *galėtum*, *jei turėčiau laiko,
    važiuočiau*), *kad* + conditional for wishes and purpose (*noriu, kad ateitum*), the
    superlative (*geriausias*, *daugiausia*), the definite adjectives (*naujasis*, *geroji*,
    *Senasis miestas*), the indefinite pronouns and double negation (*kažkas*, *kas nors*, *niekas
    nieko nežino*, *niekur*);
  - ranks 1301–1600: the participles as adjectives and in plain clauses (*dirbantis žmogus*,
    *praėjusią savaitę*, *parašytas laiškas*), the passive (*namas pastatytas 1930 metais*), the
    half-participle *-damas* for a second action of the subject (*eidamas namo, jis galvojo*), the
    gerund (*grįžus namo*, *lyjant*), *nors*, *vis dėlto*, *užuot*;
  - ranks 1601–2000: the impersonal passive (*sakoma*, *manoma*, *čia nerūkoma*), the permissive
    (*tegul ateina*), the necessity forms (*būtina*, *reikėtų*), indirect questions with *ar*, *kur*,
    *kada* (*nežino, ar ateis*), the pluperfect-like *buvo atėjęs* only where it is plain, *tarsi*,
    *lyg*;
  - ranks 2001–2500: the participles in reported speech (*sakė esąs pavargęs*) for recognition,
    the oblique mood of news (*esą*, *neva*) explained by the context, verbal nouns
    (*skaitymas*, *keitimas*), *nepaisant*, *dėka*, *pagal*, *remiantis*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *tačiau*, *vis dėlto*,
    *taigi*, *be to*, *kita vertus*, *atsižvelgiant į*, *kalbant apie*; long participial phrases;
    common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*skiltis*, a light reflection on everyday life), dialogue
    that marks *tu* and *jūs*, and less common connectors (*nors ir*, *juolab*, *kadangi*, *užtat*);
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
  After rank 1000 sentences get longer and joined (*nes*, *kai*, *kol*, *nors*, *todėl*, *tačiau*,
  *bet*, *tad*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter to a newspaper, a short report), as long as it
  is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the level
  should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Daiva eina ten. Ten nieko nėra. Ji eina į kitą namą.*).
- **Questions at the end**: after the story, a paragraph starting `Klausimai:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kodėl* and *kaip*, with fuller answers):

  ```
  Klausimai:
  Ko nežino Daiva?
  Daiva nežino, kur yra Kajus.
  Kur yra Kajus?
  Kajus yra prie namų su Nojumi.
  ```

  The checker counts the header `Klausimai:` as the word *klausimas* (rank 186): in the first
  course it is one outside word per lesson, after that a known one.
- **Standard Lithuanian**: the narration is *bendrinė kalba*, the standard language as the
  Valstybinė lietuvių kalbos komisija and the *Dabartinės lietuvių kalbos žodynas* give it: neutral,
  correct and natural Lithuanian as written today. No dialect forms (Žemaitian, Dzūkian), no
  barbarisms the standard rejects (*biškį*, *nu*, *tipo*, *čiut*, *davai*, *blyn*, *šūdas*,
  *viskas okei*, *sorry*), no Russian or Polish loans of speech (*vsio*, *koroče*). Dialogue may be
  lightly colloquial only in what any careful speaker says (*na*, *gerai*, *aišku*, *ačiū*,
  *prašom*, *labas*, *sveiki*, *iki*). Always write every letter: *ą*, *č*, *ę*, *ė*, *į*, *š*, *ų*,
  *ū*, *ž* are letters of their own and tell words apart (*į* "into" / *i-*; *jį* "him" / *ji*
  "she"; *ją* "her" / *ja* "with her"; *ė* in *ėjo*, *nėra*; *ų* in the genitive plural *vaikų* /
  *vaiku*; *ū* in *būti*, *sūnus*). The checker compares them exactly.
- **Lithuanian typography**: direct speech in the low and high quotation marks „…“, each
  speaker's line a paragraph of its own; after the closing mark comes a comma and a dash before
  the narrator's words (*„Labas“, – sako Tomas.*); a question or exclamation mark stays inside and
  takes no comma (*„Kur tu?“ – sako Daiva.*); narration before the quote ends in a colon
  (*Martynas sako: „Aš noriu eiti!“*). The questions and answers of the final `Klausimai:` block are
  plain lines, as above. Inside a sentence a dash is the en dash with spaces ( – ). Write numbers
  as digits (*8 metai*, *nuo 8 iki 6*, *20 metų*, *1930 metais*), except a number word that is in
  your band: spell that one out to teach it (*du vaikai*, *trys dienos*). Do not join an ending to
  digits (*3-ioji*, *2020-aisiais*, *XX a.*): the checker reads such an ending as a word of its
  own; write the ordinal as a word (*trečioji*) or rephrase (*2020 metais*). Write no
  abbreviations (*m.*, *val.*, *g.*, *pvz.*, *kt.*): write *metais*, *valandą*, *gatvė*,
  *pavyzdžiui*, *ir kiti*.
- **Names are names**: place names, adjectives made from place names (*lietuviškas*, *vokiškas*,
  *kauniškis*) and words for peoples or languages (*lietuvis*, *vokietis*, *rusas*, *lenkas*)
  count as names: skip them as band words even when the list has them as entries; a text may still
  use one (*kalba lietuviškai*, *lietuvių kalba*), counted at its rank. Never use a list word as a
  character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, priests, monks, prayer, heaven and
  hell, angels and devils, souls and spirits, crosses and wayside shrines, pilgrims and
  pilgrimages, and every custom or holiday of religious origin, even as a date, a school break or
  a gift season: Christmas (*Kalėdos*, *Kūčios*, the twelve dishes), Easter (*Velykos*, painted
  eggs), *Užgavėnės* (Shrovetide, with its *blynai* as a carnival dish), *Joninės* or *Rasos*
  (midsummer, St John's night, with its bonfires and the fern flower), *Vėlinės* (All Souls),
  *Kaziuko mugė* (St Casimir's fair) and every *atlaidai*, the Hill of Crosses, Vilnius
  Cathedral and the Gate of Dawn, and name days (*vardadienis*, which come from the calendar of
  saints); a town's patron festival is avoided too. Folk customs with no church holiday or saint
  named are fine: a folk song (*sutartinė*, *daina*), the *kanklės*, a craft fair (*mugė*,
  *turgus*) with no saint in its name, a harvest festival, the Song Festival (*Dainų šventė*) as a
  music festival, a birthday, New Year's Eve (*Naujųjų metų naktis*) as a family evening, the
  start of the school year (*Rugsėjo pirmoji*), the summer holidays, mushroom and berry picking.
  Place names such as *Šventoji* are names, but prefer towns without a saint in their name; use
  *dangus* only as the sky and *tikėti* only as believing a person or a fact;
- **politics**: the state, the government, the *Seimas*, elections, parties, ministers,
  presidents, kings, dukes and rulers (Mindaugas, Gediminas, Vytautas as rulers), independence and
  its days (16 February, 11 March), the Soviet years, deportations and partisans, flags, language
  politics, and borders as disputes; everyday civic life (a library, a school, the *seniūnija*'s
  counter, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not); castles and
  hill forts (*piliakalniai*) only as places to visit, never with battles; the sea and the lagoon
  stay mild (a storm keeps the boats in port; nobody is lost);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (beer, *alus*, *midus*, *trauktinė*, *degtinė*, wine, a toast, a bar
  visit for a drink), **gambling** (lotteries, betting, casinos), drugs and smoking;
- **deliberate misspellings**: no spellings without the Lithuanian letters, no colloquial or
  dialect spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Lithuanian first names are fine as
  characters.

The themes keep to this: the history courses (*Istorija iš arti*, *Archeologija ir praeitis*,
*Tyrinėtojai ir keliautojai*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, rulers or politics; *Liaudies šventės ir mugės* and *Liaudies menas ir
papročiai* are folk and family celebrations with no church holiday or saint named; *Viešosios
paslaugos* is the library, the health centre and the *seniūnija*'s counters, never politics;
*Lietuvių kalba* and *Žodžiai ir jų kilmė* are about words, dialects and learning, never about
language politics; *Pasakos ir sakmės* tells folk tales (clever animals, giants, the grass snake
queen *Eglė žalčių karalienė* only as a tale with no death in it, *Neringa* the giantess) without
gods, devils or spirits, so not *Perkūnas*, *velnias* or *laumės*.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *dievas* 127, *velnias* 260, *bažnyčia* 409, *šventas* 820; *dangus* 919 only as the
  sky; *šventė* 470 only as a party or festival with no church holiday;
- politics: *valstybė* 134, *savivaldybė* 162 (the town hall's counter is fine if a story needs
  it, but prefer *seniūnija*), *teisė* 186 (rights, law), *prezidentas* 248, *atstovas* 293,
  *vyriausybė* 294, *seimas* 300, *ministras* 330, *taryba* 339, *politika* 341, *karalius* 347,
  *sąjunga* 353, *nacionalinis* 369 (except *nacionalinis parkas*), *įstatymas* 389, *valdžia* 398,
  *partija* 397, *ministerija* 399, *rinkimas* 596, *valstybinis* 469, *pirmininkas* 485, *laisvė*
  492, *politinis* 512, *respublika* 518, *komisija* 588, *tauta* 579, *valdyti* 698 (except a
  machine: *valdyti automobilį* is better *vairuoti*), *lyderis* 768, *komitetas* 786, *meras* 669,
  *vadas* 787 (leader, commander), *pilietis* 795, *administracija* 890;
- war, crime, police, courts: *policija* 252, *teismas* 273, *karas* 249, *ginklas* 357,
  *pareigūnas* 358, *kraujas* 516, *nužudyti* 526, *byla* 536, *kova* 362, *kapitonas* 644,
  *kovoti* 681, *operacija* 758, *karys* 641 (soldier), *pergalė* 776 (victory), *karinis* 838,
  *auka* 861 (victim), *teisėjas* 966, *nusikaltimas* 911, *kalėjimas* 914, *armija* 954, *kariuomenė* 976; *apsauga* 242 and *saugumas*
  493 only as safety and protection in everyday life (*aplinkos apsauga*, *saugumas kelyje*);
- death: *mirti* 165, *mirtis* 312, *žūti* 887;
- vulgar and insults: *šūdas* 435, *kvailas* 849;
- alcohol and smoking: none in the first 1000, but *gerti* 781 only with water, tea, coffee, milk
  and juice (and *alus* 1067, *vynas* 1312 just after);
- names, place adjectives, peoples and languages: *lietuvis* 224, *lietuviškas* 864, *vokietis*
  950, *jonas* 715 (a name), *žydas* 901; *kinas* 508 only as the cinema (it also holds *kinai*, "the Chinese");
- topics too close to illness news: *karantinas* 402, *infekcija* 815;
- list noise: the interjection *nagi* 226 and the particle *gi* 441 (teach *na* 119 and *ei* 219
  only in dialogue, if at all);
- negated verbs kept as entries: *neleisti* 844, *nepavykti* 925 (forms of *leisti*, *pavykti*).

Further on, the same categories hold; for example *siela* 1068, *alus* 1067, *vynas* 1312,
*nepriklausomybė* 1188, *kandidatas* 1007, *kryžius* 1598, *kunigas* 1168, *amerikietis* 1059,
*smurtas* 1645, *angelas* 2034, *pragaras* 2154, *rusas* 2129, *rūkyti* 2265, *dvasia* 1377, *malda*
2105, *rojus* 3237, *degtinė* 3323, *loterija* 4548, the negated verb entries (below), and the
vulgar and slang words.

## The file

Write `tools/courses/lt/<id>.json`:

```json
{
  "id": "lt-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Lithuanian, e.g. Maistas ir pirkiniai",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
  "lessons": [
    {
      "title": "Story title in Lithuanian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/lt/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ėjo* or *eina* for *eiti*, *gera* or *geriau* for *geras*,
  *nežinau* for *žinoti*, *vaikų* for *vaikas*). Reuse earlier lessons' new words too. `newWords`
  holds the word as the list writes it: the infinitive of a verb (*eiti*, *mokytis*), the
  nominative singular of a noun (but *metai*, *durys*, *pietūs*, *studijos*, *varžybos*,
  *rungtynės*, *vestuvės*, *lėšos*, which the list keeps in the plural), the masculine singular of
  an adjective (*geras*, *gražus*, *didelis*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a made-up headword, a
  negated verb, a form of another word, a place adjective, a word of the content rules); the
  checker's last line lists the band words not taught yet.

Things to know about the Lithuanian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/lt/wordlist.tsv`
  (a copy of the app's `lt.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  subtitles, the web and news (wordfreq, OpenSubtitles and the Leipzig news corpus), so talk and
  news both rank high: politics and court words are thick in the first thousand and are skipped.
- **The checker has no Lithuanian rules of its own**: it cuts words at spaces, punctuation and
  hyphens, and looks each one up as written (lowercased). So a form counts only if the list or
  `extra_forms.tsv` has it under the headword; `rank.py` shows where a form is filed.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns (*vaikas*: *vaiko*, *vaikui*, *vaiką*, *vaiku*, *vaike*, *vaikai*, *vaikų*...), the
  comparative and superlative of adjectives and adverbs (*geresnis*, *geriausias* under *geras*;
  *didesnis*, *didžiausias* under *didelis*), the conjugation, the participles and the negated
  forms of verbs. The list kept at most 40 forms a word, so `extra_forms.tsv` adds the rest from
  the dictionary pack and the regular paradigms: every negated form (*nežinau*, *nesimoko*,
  *neatėjo*, *nebegaliu*), the forms of prefixed verbs (*pamatėme*, *sužinojau*), the rarer cases.
  *aš* holds *man*, *mane*, *manęs*, *manimi*; *tu* holds *tau*, *tave*, *tavęs*; *jis* holds *jo*,
  *jam*, *jį*, *juo*; *ji* holds *jos*, *jai*, *ją*; *mes*, *jūs*, *jie* their cases (*mūsų*,
  *jums*, *jų*, *juos*); *kas* holds *ką*, *ko*, *kam*, *kuo*; *savęs* (114) holds *sau*, *save*;
  *savo* (29), *mano* (46), *tavo* (79) are entries of their own; *visas* holds *visi*, *visa*;
  *viskas* (53) holds *viską*, *visko*; *gerai* (35) and *labai* (36) are entries of their own.
- **Negated verbs**: *ne-* is written joined to the verb (*nežino*, *nėra*, *nebuvo*, *nesu*), and
  every negated form counts for its verb. In an unprefixed reflexive verb the particle moves after
  *ne-* (*mokosi* → *nesimoko*, *jaučiasi* → *nesijaučia*); in a prefixed one it stays (*susitiko* →
  *nesusitiko*, *atsiprašau* → *neatsiprašau*). *eiti* negates as *neina*, *nėjo*, *neis*.
  *nebe-* "no longer" counts the same way (*nebėra*, *nebegaliu*). The list also keeps a few
  negated verbs as entries of their own (*neleisti* 844, *nepavykti* 925, *nekalbėti* 1635, *nemėgti*
  1941, *nepastebėti* 2036, *nesutikti* 2210, *neužtekti* 2636, *neskubėti* 2788, *nedirbti* 2918,
  *nebebūti* 2399, *nebenorėti* 3358, *nemiegoti* 4371): they are forms of their verbs; never teach
  them, and skip them in your band. *netekti* 912 "to lose" is a verb of its own (*neteko darbo*),
  not a negated *tekti* 261 "to have to, to happen" (*teko laukti*, *man teko*): teach each in its
  sense.
- **Reflexive verbs are words of their own**: *mokytis* 586 "to learn" and *mokyti* 1336 "to
  teach", *jaustis* 375 and *jausti*, *vadintis* 678 and *vadinti* 569, *baigtis* 464 and *baigti*
  232, *laikytis* 380 and *laikyti* 274, *imtis* 741 and *imti* 282, *naudotis* 947 and *naudoti*
  143, *tikėtis* 279 "to hope" and *tikėti* 482 "to believe", and *džiaugtis*, *stengtis*,
  *elgtis*, *rūpintis*, *kreiptis*, *ruoštis*, *sektis* (*sekasi*), *liautis* with no plain partner
  in the band. The reflexive forms (*mokosi*, *mokausi*, *nesimoko*) count only for the reflexive
  entry, the plain ones (*moko*, *nemoko*) only for the plain one: teach each in its own sense and
  forms, and do not count one as a use of the other. Exception: *rinkti* 407 holds *rinktis*
  (*renkasi*; *pasirinkti* is 783 on its own). Prefixed reflexives are entries of their own too
  (*susitikti* 484, *atsiprašyti* 337, *prasidėti* 387, *pasirodyti* 370, *prisiminti* 440,
  *atsirasti* 455, *pasiruošti* 1773, *atsisėsti* 3436).
- **Aspect partners are entries of their own**: *daryti* 80 / *padaryti* 100, *sakyti* 52 /
  *pasakyti* 92, *matyti* 84 / *pamatyti* 269, *žiūrėti* 235 / *pažiūrėti* 329, *galvoti* 255 /
  *pagalvoti* 643, *rašyti* 345 / *parašyti* 533, *skambinti* 703 / *paskambinti* 745, *klausti* 936 /
  *paklausti* 842, *pasakoti* 437 / *papasakoti* 685, *laukti* 174 / *palaukti* 270. Teaching one
  does not teach the other; each is taught through its own forms.
- **Participles**: most are filed under their verb and count for it (*naudojamas* under *naudoti*
  143, *skirtas* under *skirti* 151, *tinkamas* "suitable" under *tikti* 340, *negalima* "not
  allowed" under *galėti*): they never teach a word of their own. Those the dictionary has as
  adjectives are entries: *vadinamas* 522 "so-called", *susijęs* 568 "related", *likęs* 594
  "remaining", *pasiruošęs* 646 "ready", *žinomas* 668 "well-known", *įmanomas* 857 and
  *neįmanomas* 882 "possible, impossible"; teach them as adjectives in that sense. Plain
  participles kept as entries that only repeat a verb (*sukurtas* 702, *paskelbtas* 917) are
  skipped as forms.
- **ne- adjectives and adverbs**: a word with a sense of its own is taught (*nemažas* 686 and
  *nemažai* "quite a lot", *neseniai* 1198 "recently", *nemokamas* 1290 "free of charge",
  *nesąmonė* 1335, *nepaprastas* 1477 "extraordinary", *nelaimė* 1488, *nebent* 1605 "unless",
  *netyčia* 2398 "by accident", *nepažįstamas* 2758 "stranger"); a plain "not X" whose X is taught
  is skipped as a form of X (*nesvarbus* 759, *negeras* 1334, *netinkamas* 1668, *neblogas* 2086,
  *nereikalingas* 2173, *nesunkus* 2329, *nelengvas* 2516).
- **Words that hold another word**, so a form counts for a headword it does not teach: *kovas*
  510 is March (*kovo 11 dieną*) and *kova* 362 "struggle" holds *kovą*, *kovos*; *pastato* counts
  for both *pastatas* 414 and *pastatyti* 957; *plaukti* 850 "to swim" holds *plaukus* (hair: *plaukas* 989); *kartas* 82
  "time, occasion" holds *kartus* "bitter"'s forms; *viltis* 904 holds *viliasi*; *šviesti* 716
  holds *švietimas* "education"; *paklausti* 842 "to ask" holds *paklausė*, *paklausiau* "asked",
  and *paklausyti* 1051 "to listen, obey" keeps *paklauso*, *paklausyk*; *ištiesti* 1012 holds
  *išties* "indeed"; *metas* 55 is "time, moment" (*šiuo metu*, *tuo metu*), *metai* 51 "year(s)";
  *namas* 96 "house" holds *namai*, *namuose*, *namų* "home", and *namo*, which is also the adverb
  *namo* 303 "homewards" (*eina namo*): *namo* counts for both, but teach *namo* 303 only as
  "homewards"; *laikas* 63 holds *laiku* "on time"; *labas* 210 holds *labą* (*labą dieną*,
  *labą rytą*, *labas vakaras*); *tėvas* 129 holds *tėvai* "parents"; *senelis* 816 holds *senelė*
  "grandmother", *karalius* 347 *karalienė*, *draugas* 124 *draugė*: feminine nouns filed under
  the masculine count at its rank but never teach it. A few feminines are entries of their own
  (*mokytoja* 962 beside *mokytojas* 749, *žaidėja* beside *žaidėjas* 756): skip them as forms of
  the masculine. Forms kept apart in the old list are now their word's: *pats* 58 holds *pati*,
  *šis* 24 holds *šitas* and *šita*, *pirmas* 68 holds *pirma*, *pirmiau*, *pirmiausia*, *tikras*
  holds *tikra*, *antras* *antra*, *pinigas* 142 *pinigai*, *jūs* *judvi*, *reikėti* 50 the
  colloquial *reiktų*, *reiks*.
- **Number words** decline (*du*, *dvi*, *dviejų*; *trys*, *tris*, *trijų*): *du*, *trys*,
  *keturi*, *penki*, *šeši* are entries, *vienas* holds *viena*, *vieną*. With digits the noun still
  agrees: *2 vaikai*, *5 metai*, *10 metų*, *21 metai*.
- **Common forms the list misses** go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated by `lt_extra.py` in the writers' folder from the English Wiktionary Lithuanian
  dictionary (`dictionaries/lt-en.sqlite`, stress marks removed), the prefixed verbs' bases, the
  regular declensions (taken only where the list's own forms fit them) and the negated verb
  forms, with a few hand-added forms. Every headword in it must be a headword of the list (the
  checker warns otherwise). If a true form is missing, write around it and name the word and form
  in your report.
- **Names** are not in the list: people, places (*Vilnius*, *Kaunas*, *Klaipėda*, *Lietuva*),
  shops and brands go in `names`. The checker does not decline them, so list every form the text
  uses (*Tomas*, *Tomo*, *Tomui*, *Tomą*, *Tomu*, *Tomai*; *Kaunas*, *Kauno*, *Kaune*; *Lietuva*,
  *Lietuvos*, *Lietuvoje*). A name of several words counts word by word: each part that is a list
  word counts at its rank (*Kuršių nerija*: *nerija*; *Senamiestis*; *Gedimino prospektas*:
  *prospektas*), so put only the true name parts in `names`. Keep only true names in `names`,
  never common words, and never a name that is also a list word: as characters avoid *Jonas*
  (715), *Rūta* (2573), *Eglė* (2035, "spruce"), *Greta* (1405, "beside"), *Rokas* (1760),
  *Rasa* (2545, "dew"), *Laima* (4352), *Dalia* (1385), *Aušra* (2120, "dawn"), *Matas* (2707,
  "measure"), *Adomas* (filed under *adyti* 6012).
  Check every name and each of its case forms with `rank.py` before you use it. Safe in the pilot:
  *Ieva*, *Tomas*, *Urtė*, *Rimas*, *Lukas*, *Gabija*, *Jurga*, *Daiva*, *Kajus*, *Vilius*,
  *Nojus*, *Paulius*, *Ema*, *Agnė*, *Ona*, *Tadas*, *Indrė*, *Mantas*, *Simona*, *Aistė*,
  *Dovydas*, *Kotryna*, *Justas*, *Martynas*, *Milda*, *Giedrė*. More safe names: *Petras*,
  *Austėja*, *Domas*, *Jurgis*, *Emilija*, *Saulius*, *Lina*, *Andrius*,
  *Algis*, *Vaida*, *Inga*, *Karolis*, *Ugnė*, *Viktorija*, *Kristina*, *Gintarė*, *Arnas*, *Marius*;
  surnames with their married and maiden endings (*Kazlauskas*, *Kazlauskienė*, *Kazlauskaitė*).
  Day and month names are in the list (*rugsėjis* 582, *liepa* 593 "July" and "linden", *spalis*
  597, *gegužė* 608, *sausis* 600, *birželis* 620, *lapkritis* 633, *balandis* 626, *penktadienis*
  642, *vasaris* 701, *gruodis* 726, *rugpjūtis* 730, *pirmadienis* 753, *šeštadienis* 779,
  *sekmadienis* 802, *trečiadienis* 921, *antradienis* 922, *ketvirtadienis* 965; *kovas* 510, see
  above; *vasara* 400, *žiema* 766, *pavasaris* 891, *ruduo* 1040).
- Everyday words rank later than you would think: *namas* 96, *šeima* 123, *ačiū* 136, *mokykla*
  140, *gyventi* 138, *draugas* 124, *šiandien* 169, *vanduo* 155, *mama* 177, *labas* 210, *sūnus*
  236, *tėtis* 233, *maistas* 264, *telefonas* 266, *brolis* 338, *kambarys* 349, *gydytojas* 416,
  *parduotuvė* 564, *valgyti* 555, *šuo* 525, *senelis* 816, *kava* 837, *dukra* 659, *virtuvė*
  1136, *pienas* 1095, *arbata* 1256, *duktė* 1327, *katė* 1501, *duona* 1608, *močiutė* 1667,
  *sesuo* 830; the dialogue tag *sako* (*sakyti* 52) is in the first band, *klausia* (*klausti*
  936) and *atsako* (*atsakyti* 551) come later.
- Diminutives are words of their own (*berniukas* 601 "boy" beside *bernas*, *upelis* 3571,
  *žvaigždutė* 4706, *staliukas* 3627): teach them in their own band, in their own sense. The
  list's form lists are clean but not perfect (a homograph here and there); a new word must appear
  as itself or a true inflection, never through such a form. Adverbs the dictionary lacks are
  entries of their own (*toli* 942 "far", *aplink* 916 "around", *nelabai* 985 "not very"); others
  are filed under their adjective (*teisingai* under *teisingas* 453, *rimtai* under *rimtas*). Loanwords Lithuanian uses as its own (*internetas*, *kompiuteris*, *futbolas*, *krepšinis*,
  *telefonas*) are taught like any word.
