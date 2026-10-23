# Writing a Galician frequency course

Each course teaches one band of 100 words from the Galician word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `gl-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Xoán non sabe onde está o seu teléfono. Vai á cociña. Non o ve. Vai ao
  salón...* Everyday life where Galician is spoken: Vigo, A Coruña, Lugo, Ourense, Pontevedra,
  Ferrol, Betanzos, Ribadeo, Viveiro, Cangas, Baiona, Cambados, Sanxenxo, O Grove, Allariz, Tui,
  Monforte, Lalín, Verín; the Rías Baixas and the Ría de Arousa, the Illas Cíes and Ons, the Miño
  and the Sil, Os Ancares and O Courel; and now and then London, Buenos Aires, Montevideo, Zürich or
  Lisbon, where Galicians live and Galician is spoken at home: home, family, food, shopping, work,
  school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *el*/*ela* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Brais in
  two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Galician drops subject pronouns: write *el*, *ela*, *eu*, *ti* only where
  Galician would (contrast, emphasis, a new subject). No article before a person's first name in the
  narration (*Uxía vive en Lugo*, *díxolle a Brais*); a personal direct object takes no *a* (*leva
  Noa á escola*, *chama Carme*), except a stressed pronoun (*vexo a ela*) or where it would be
  unclear. Little dialogue; when there is some, short and simple, with *ti* among family and friends
  and *vostede* (+ the third person) with strangers, older people and in shops or offices
  (*Vostede vive aquí?*, *o señor Castro*, *a señora Varela*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense (*fala*, *come*, *vive*; *é*, *son*, *ten*, *vai*, *fai*, *vén*,
    *ve*, *di*, *pon*, *sabe*), *ser* and *estar* (*é de Lugo*, *está na casa*, *está canso*), *hai*,
    *ter*, *querer* / *poder* + infinitive, *ter que* + infinitive (*ten que traballar*), *ir* +
    infinitive for what is about to happen (*vou comer*, *imos ver*), *facer* in the weather (*fai
    sol*, *fai frío*, *chove*); the **pretérito** (the simple past, the only past for events:
    *foi*, *fixo*, *dixo*, *veu*, *chegou*, *comeu*, *viviu*) from the first courses, in short
    sentences: Galician has no compound past, so never *ha comido*, *teño comido* for "has eaten";
    the contractions (*do*, *da*, *no*, *na*, *ao*, *á*, *polo*, *pola*, *co*, *coa*, *dun*, *nunha*,
    *cunha*); the weak pronouns in their places (see below): *chámase*, *levántase*, *dígoche*,
    and *non o sei*, *xa o teño*, *como te chamas?*; simple questions (*que*, *quen*, *onde*, *como*,
    *cando*, *por que*, *canto*);
  - ranks 301–600: possessives with the article (*a miña casa*, *o seu coche*); the imperative
    (*mira*, *ven*, *di*, *fai*, *pon*, *colle*, *senta*) with the pronouns joined to it (*dime*,
    *dálle*, *axúdame*, *mírao*, *séntate*); the object pronouns *o*, *a*, *os*, *as* with their
    other shapes (*facelo*, *collelos*, *vémolo*; *déuno*, *faino*); *che* (to you) beside *te* (you):
    *dígoche*, *véxote*; comparatives (*máis grande ca*, *mellor*, *peor*, *tan alto coma*); the
    **copretérito** (imperfect: *era*, *tiña*, *facía*, *ía*, *vivía*) for background and habits;
    *estar a* + infinitive for what is going on (*está a ler*);
  - ranks 601–1000: the **future** (*farei*, *irá*, *virá*) beside *ir* + infinitive; the
    relatives *que* and *onde*; reported speech with *que* (*di que vén*); the contracted pronoun
    pairs (*xa cho dixen*, *dámo*, *non llo digas*); *cando* and *se* with the present for real
    plans (*se chove, quedamos na casa*);
  - ranks 1001–1300: the **conditional** (*gustaríame*, *poderías*, *sería mellor*), superlatives
    (*o máis alto*, *o mellor*), *o que*, *o cal* and prepositions before relatives (*a casa onde
    vivo*, *coa que fala*), *xa que*, *así que*;
  - ranks 1301–1600: the **present subjunctive** after the common triggers (*quero que veñas*, *é
    mellor que*, *para que*, *antes de que*, *cando* for the future: *cando chegues*, *talvez*), the
    indefinites with double negation (*non vén ninguén*, *non hai nada*, *ningún*), *aínda que*,
    *en vez de*, *ademais de*;
  - ranks 1601–2000: the **antepretérito** (the simple pluperfect of written narration:
    *chegara*, *dixera*, *fixera*), the **imperfect subjunctive** in *se* sentences (*se tivese
    tempo, viaxaría*), the impersonal and passive *se* (*véndense pisos*, *aquí fálase galego*), the
    passive with *ser* in plain cases (*a ponte foi feita en 1920*), indirect questions (*non sabe se
    vén*, *pregunta onde está*), *coma se*;
  - ranks 2001–2500: the **inflected infinitive**, Galician's own (*para faceren*, *ao chegarmos*,
    *antes de saíres*, *é mellor iren cedo*), the subjunctive after *aínda que*, *sen que*, *a non
    ser que* and feelings (*alégrome de que veñas*), reported speech with tense shifts (*dixo que
    viría*), *mentres*, *por mor de*, *grazas a*, *segundo*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *porén*, *non obstante*,
    *polo tanto*, *é dicir*, *dado que*, *malia*, *canto a*; gerund phrases; common idioms
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*artigo de opinión*, a light reflection on everyday
    life), dialogue that marks *ti* and *vostede*, and less common connectors (*se ben*, *de xeito
    que*, *así e todo*, *en definitiva*);
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
  After rank 1000 sentences get longer and joined (*porque*, *cando*, *mentres*, *aínda que*,
  *pois*, *pero*, *así que*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Vai á cociña. Non o ve. Vai ao salón. Non o ve.*).
- **Questions at the end**: after the story, a paragraph starting `Preguntas:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *por que* and *como*, with fuller answers):

  ```
  Preguntas:
  Que non sabe Xoán?
  Xoán non sabe onde está o seu teléfono.
  Onde está o teléfono?
  Está no coche.
  ```

  The header `Preguntas:` is layout: the checker does not count it as a word.
- **Standard Galician**: the narration is the standard language of the Real Academia Galega and the
  Instituto da Lingua Galega (the *Normas ortográficas e morfolóxicas do idioma galego*), neutral,
  correct and natural Galician as written today. Write the forms the norm gives: *unha*, *ningún*,
  *algunha*, *nós*, *vós*, *grazas*, *tamén*, *non*, adjectives in *-ble* (*posible*, *amable*,
  *imposible*; not the *-bel* variants), adverbs in *-mente* without a written accent
  (*facilmente*, *practicamente*). No castelanismos (*entonces*, *bueno*, *pues*, *hasta*,
  *después*, *todavía*, *ahí*, *hola*, *adiós*, *gracias*, *ventana*, *silla*, *cuchara*: write
  *entón*, *ben*, *pois*, *ata*, *despois*, *aínda*, *aí*, *ola*, *adeus*, *grazas*, *fiestra*,
  *cadeira*, *culler*) and no reintegrationist or Portuguese spellings (*não*, *também*, *obrigado*
  for thanks, *-ção*, *lh*/*nh* for *ll*/*ñ*). No slang and no vulgar words (the list has some: skip
  them). Dialogue may be lightly colloquial only in what any careful speaker says (*ei*, *home*,
  *muller* as a filler, *de acordo*, *moi ben*, *vale*). Always write every accent and *ñ*: the
  checker compares them exactly, and the diacritic accents tell words apart (*é* "is" / *e* "and";
  *á* "to the" / *a*; *máis* "more" / *mais* "but"; *só* "only, alone" / *so* "under"; *dá* "gives" /
  *da* "of the"; *vén* "comes" / *ven* "sees, come!"; *pór* "to put" / *por*; *nós* / *nos*; *vós* /
  *vos*; *té* "tea" / *te*; *pé* "foot"; *cómpre* "it is needed" / *compre*; *fóra*, *ás*, *ós*).
- **The weak pronouns** (*me*, *te*, *che*, *lle*, *lles*, *nos*, *vos*, *se*, *o*, *a*, *os*, *as*)
  go after the verb, joined to it with no hyphen, in a plain affirmative main clause: *Chámase
  Uxía.* *Levántase ás 7.* *Díxolle que si.* *Dígoche unha cousa.* *Vémonos mañá.* They go before
  the verb after a negation (*non*, *nunca*, *ninguén*, *nada*: *Non o sei.* *Nunca me chama.*), a
  question or exclamation word (*Como te chamas?* *Que lle dixo?*), a subordinating word (*que*,
  *cando*, *se*, *porque*, *onde*: *Sei que o queres.* *Cando o ve...*), the adverbs *xa*, *aínda*,
  *tamén*, *só*, *sempre*, *ben*, *mal*, *case*, *talvez*, *quizais* (*Xa o teño.* *Tamén lle dá
  algo.*) and an indefinite subject (*alguén*, *todos*, *ninguén*: *Todos o saben.*). Joined to the
  verb, *o*, *a*, *os*, *as* become *lo*, *la*, *los*, *las* after a form ending in *-r* or *-s*, which
  drops (*facer* + *o* = *facelo*; *vemos* + *o* = *vémolo*), and *no*, *na*, *nos*, *nas* after a
  diphthong (*deu* + *o* = *déuno*; *fai* + *o* = *faino*); the *-s* of *-mos* drops before *nos*
  (*vámonos*, *sentémonos*). Two pronouns contract: *me* + *o* = *mo*, *che* + *o* = *cho*, *lle* +
  *o* = *llo*, *lles* + *o* = *llelo*, *nos* + *o* = *nolo*, *vos* + *o* = *volo* (*xa cho dixen*,
  *non llo digas*). The joined form takes the written accent the whole word needs (*di* → *díxolle*,
  *dá* → *dálle*, *chama* → *chámase*). *che* is "to you" (*dígoche*), *te* is "you" (*véxote*,
  *quérote*); do not use the "solidarity" *che* (*vouche contar*) below rank 1000.
- **Galician typography**: direct speech in Galician angle quotation marks «…», each speaker's line
  a paragraph of its own; the narrator's comma goes after the closing mark, a question or
  exclamation mark of the quoted sentence inside it. No opening ¿ or ¡ (the norm keeps them for
  long or unclear sentences only; these texts never need them):

  ```
  «Ola, Uxía», di Brais.
  «Como estás?», pregunta Uxía. «Queres un café?»
  ```

  The questions and answers of the final `Preguntas:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ). Write numbers as digits (*3 irmáns*, *ás 7*, *ás
  7.30*, *en 1992*), ordinals as the norm abbreviates them (*1.º*, *2.ª*: the checker reads their
  endings as part of the number), centuries in Roman numerals (*o século XIX*: counted as numbers),
  except a number word that is in your band: spell that one out to teach it (*dous irmáns*).
- **Names are names**: place names, adjectives made from place names (*galego*, *español*,
  *portugués*, *francés*, *inglés*, *americano*) and words for peoples or languages count as names:
  skip them as band words even when the list has them as entries; a text may still use one (*fala
  galego*), counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, priests, monks, prayer, heaven and
  hell, angels and devils, souls and spirits, pilgrims and pilgrimages, and every custom or
  holiday of religious origin, even as a date, a school break or a gift season: Christmas (*Nadal*,
  *Reis*; New Year's Eve, *Aninovo*, is fine only as a family evening),
  Easter (*Semana Santa*, *Pascua*), *Entroido* (carnival, with its *filloas* as a carnival dish),
  *San Xoán* (the bonfires, the *cacharela*), *Magosto* and *Samaín*, *Defuntos*, the *romarías* and
  every patron saint's *festa*, the *Camiño de Santiago*, the cathedral and the *Apóstolo*, the *Día
  de Galicia* (25 July, a saint's day and a national day), and saints' name days; a town's patron
  festival is avoided too. Folk customs with no church holiday or saint named are fine: a food
  festival (*festa do polbo*, *festa do pemento*, *festa do marisco*), a fair or market (*feira*,
  *mercado*), the *muiñeira*, *gaitas* and *pandeiretas*, a *foliada* at a summer street party with
  no saint's feast named, a birthday, New Year's Eve as a family evening, the start of the school
  year, the summer holidays, chestnuts roasted at home in autumn. Place names such as *Santiago de
  Compostela* or *Santa Comba* are names, but prefer towns without a saint in their name; Santiago
  may appear only as a city (its university, its market, its rain), never with the cathedral, the
  Camiño or pilgrims; never visit a church, a chapel or a monastery, and use *ceo* only as the sky
  and *crer* only as believing a person or a fact;
- **politics**: the state, the government (*Goberno*, *Xunta*), parliament, elections, parties,
  ministers, presidents, kings and rulers, nationalism, independence, flags, language politics, and
  borders as disputes; everyday civic life (a library, a school, the *concello*'s counter, a
  residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not); the sea's dangers
  stay mild (a storm keeps the boats in port; nobody is lost at sea, so not the *Costa da Morte*
  either);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (wine, *Albariño*, *Ribeiro*, beer, *augardente*, *licor café*, the
  *queimada*, a toast, a wine fair, a bar visit for a drink), **gambling** (lotteries, betting,
  casinos), drugs and smoking;
- **deliberate misspellings**: no spellings without accents, no colloquial, Spanish or Portuguese
  spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Galician first names are fine as characters.

The themes keep to this: the history courses (*A historia ao alcance da man*, *Arqueoloxía e
pasado*, *Exploradores e viaxeiros*) tell the history of everyday life, towns, crafts, trade,
science and travel, never of wars, rulers or politics; *Festas populares e feiras* and *Arte e
costumes populares* are folk and family celebrations with no church holiday or saint named;
*Servizos públicos* is the library, the health centre and the *concello*'s counters, never politics;
*A lingua galega* and *As palabras e a súa orixe* are about words, dialects and learning, never
about language politics; *Contos e lendas* tells folk tales (the *mouras*, giants, clever animals) without saints, souls or
miracles, so not the *Santa Compaña*.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *deus* 146, *santo* 353, *ceo* 489 only as the sky, *espírito* 825, *alma* 643,
  *igrexa* 669, *rezar* 884, *demo* 889, *padre* 986, *xudeu* 653;
- politics: *rei* 192, *pobo* 228 (except a village, which is better *aldea* or *vila*), *goberno*
  355, *liberdade* 388, *comunista* 390, *presidente* 394, *lei* 425, *revolución* 501, *nacional*
  510, *política* 515, *camarada* 589, *sindicato* 593, *comité* 584, *cidadán* 660, *alteza* 693,
  *comuna* 705, *civil* 703, *votar* 717, *príncipe* 764, *folga* 770, *reino* 772, *político* 786,
  *nación* 826, *elección* 827, *autoridade* 863, *revolucionario* 922, *líder* 938;
- war, crime, police, courts: *matar* 115, *guerra* 165, *policía* 252, *disparar* 270, *loitar*
  284, *exército* 298, *arma* 318, *soldado* 323, *roubar* 346, *garda* 377 (holds *gardar*'s 3rd
  person: teach *gardar* 561 through its other forms), *sangue* 378, *loita* 464, *deter* 472,
  *inimigo* 505, *bomba* 526, *militar* 560, *ferir* 567, *destruír* 586, *atacar* 595, *axente*
  600, *combater* 609, *asasinar* 613, *batalla* 629, *xeneral* 638, *defender* 655, *asasino* 678,
  *xurar* 665, *refuxiar* 748, *ataque* 784, *asasinato* 814, *violencia* 823, *espada* 846,
  *escravo* 851, *ladrón* 882, *testemuña* 886, *prisioneiro* 892, *castigar* 913, *arrestar* 942,
  *vitoria* 943, *cometer* 949, *bala* 950, *condenar* 954, *vítima* 969, *xuízo* 976, *comandante*
  987;
- death: *morrer* 133, *morto* 210, *morte* 248;
- vulgar and insults: *merda* 231, *puta* 326, *foder* 439, *carallo* 450, *maldito* 506, *cabrón*
  762, *estúpido* 767, *cu* 804, *bastardo* 856, *idiota* 881;
- alcohol and smoking: *viño* 722, *cigarro* 968 (and *cervexa* 1008 just after);
- names, place adjectives, peoples and languages: *americano* 383, *francés* 428, *alemán* 524,
  *franco* 630, *inglés* 728, *indio* 865, *ruso* 920, *galego* 973;
- list noise and forms of other words: *aló* 342 (on the phone only), *acó* 476 (colloquial for
  *aquí*), *ren* 794 (literary), *subtítulo* 813 (the subtitles the list was counted from), *liscar*
  852 (slang), *graciar* 871 (holds the Spanish *gracias*), *pra* 996 (colloquial *para*),
  *señorito* 508, *dama* 981, *cabaleiro* 529 (old-fashioned titles; teach only if a story makes them
  plain), *amo* 899 and *ama* 977 (they hold *amar*'s forms), *don* 622, *chama* 419 only as a flame
  (it holds *chamar*'s 3rd person: *chama* counts for *chamar* too).

Further on, the same categories hold; for example *cervexa* 1008, *santa*, *bispo*, *misa*,
*pecado*, *anxo*, *inferno*, *Nadal*, *Entroido*, *xuíz*, *cárcere*, *prisión*, *droga*, *tabaco*,
*fumar*, *bebedo*, *casino*, *lotaría*, *funeral*, *cemiterio*, *enterro*, *bandeira*, *Xunta*,
*parlamento*, *partido* (except a match: *un partido de fútbol*).

## The file

Write `tools/courses/gl/<id>.json`:

```json
{
  "id": "gl-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Galician, e.g. Comer e ir á compra",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Galician",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/gl/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *vai* or *imos* for *ir*, *boa* for *bo*, *coñeces* for
  *coñecer*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes
  it: the infinitive of a verb, the singular of a noun, the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, a word of the content rules); the checker's last line
  lists the band words not taught yet.

Things to know about the Galician list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/gl/wordlist.tsv`
  (a copy of the app's `gl.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  film and TV subtitles (OpenSubtitles), so talk ranks high and some everyday written words rank
  low; war, crime and swearing words rank high and are skipped.
- **How the checker cuts words.** It splits a word at hyphens, and it cuts the weak pronouns off a
  verb they are joined to, by the rules above: *díxolle* is *dixo* + *lle*, *chámase* is *chama* +
  *se*, *facelo* is *facer* + *lo*, *vémonos* is *vemos* + *nos*, *déuno* is *deu* + *no*, *dígollo*
  is *digo* + *llo*. The written accent the joined form needs comes off (*díxo* → *dixo*). A joined
  form the list has whole (*dime*, *dálle*, *díxome*, *síntoo*, *chámase*) stays whole and
  counts for its verb; the pronoun inside it then does not count as a use of the pronoun, so to
  teach *me*, *lle*, *te* or *che* write them on their own too (*non me dás*, *que lle dixo*, *xa che
  dixen*). A name in `names` is never cut. The contractions are forms of their preposition in the
  list: *do*, *da*, *dun*, *dunha*, *deste*, *desta*, *diso*, *disto*, *daquel*, *del*, *dela* of
  *de*; *no*, *na*, *nun*, *nunha*, *neste*, *nesta*, *niso*, *nisto*, *naquel*, *nel*, *nela*,
  *noutro* of *en*; *ao*, *á*, *aos*, *ás* of *a*; *co*, *coa*, *cun*, *cunha* of *con*; *polo*, *pola*
  of *por*. The pronoun pairs are forms of their first pronoun: *mo*, *ma* of *me*; *cho*, *cha* of
  *che*; *llo*, *lla*, *llelo* of *lle*; *nolo* of *nós*; *volo* of *vós*.
- **Pronouns and little words are entries of their own**: *se* 15 (the reflexive *se* and "if"),
  *me* 35, *te* 50, *che* 85, *lle* 55 (with *lles*), *nós* 86 (with *nos*), *vós* 156 (with *vos*);
  *o* 4 holds the article and the pronoun *o*, *os*, *lo*, *los*; *a* 1 holds the article, the pronoun
  and the preposition *a*, *as*, *la*, *las*; *eu* 24 holds *min*; *el* 62, *ela* 96, *eles* 142,
  *elas* 803; *ti* 39 and *vostede* 88. *é*, *son*, *era*, *foi*, *sexa* are *ser* 5 (*foi* is *ir* 13
  too); *hai*, *había*, *houbo* are *haber* 27; *vin* is *ver* 29 and *vir* 40; *ten*, *teño*, *tiña*
  are *ter* 11; *vai*, *vou*, *imos*, *fun* are *ir*. *mellor* 98 and *peor* 434 are entries of their
  own; *boa* is *bo* 64; *moito* 54 and *moi* 53 are two entries; *mais* 82 is "but" (literary) and
  "and also" in *Uxía e mais Brais*; *máis* 30 is "more"; *coma* 100 is "like, as" (*tan alto coma
  ti*, *coma min*).
- **Words that have no entry of their own**, filed under a commoner word spelled the same: *verán*
  ("summer", under *ver*: "they will see"), *visita*, *compra*, *estudo*, *medida*, *chamada*,
  *mirada*, *chegada*, *paseo*, *mercado* (under *mercar* 274) and other nouns made from verbs, under their verbs; *vista* under
  *ver*. The checker counts them at that word's rank, so they look known early; they are never band
  words, and below rank 1000 use them only where the story makes their meaning plain.
- **Homographs**: some entries hold two words (*como* "how" and "I eat"; *para* "for" and "stops";
  *nada* "nothing"; *canto* "how much" and "song"; *cara* "face" and "dear"; *tarde* "afternoon" and
  "late"; *ollo* "eye"; *entre* "between"; *parte*; *sobre* "on" and "envelope"; *vela* "candle"; *cea*
  "dinner"), and a form shared by two entries counts for both (*vin* of *ver* and *vir*; *vir* is a form of *ver* too, its
  future subjunctive). *vida* "life" 91 and *río* "river" 637 are entries of their own, though
  they are also a form of *vir* and of *rir*: never count them as uses of those verbs.
  Use a new word in its own sense, never through its homograph, and count a new word's uses in
  forms that are its own. A verb's form that is a noun of its own is left to the noun: *casa* is not
  *casar*'s, *porta* not *portar*'s, *pregunta* 372 not *preguntar*'s, *axuda* 366 not *axudar*'s,
  *volta* 279 not *volver*'s or *voltar*'s, *comida* 401 not only *comer*'s. Teach such a verb through
  its other forms (*preguntou*, *preguntar*, *pregunto*). But *chama* counts for *chamar* too, and
  participles such as *sentido*, *saída*, *entrada*, *coidado* for their verbs.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated from the English Wiktionary Galician dictionary (`dictionaries/gl-en.sqlite`): every
  verb's conjugation in the norm's forms (no reintegrationist ones), the gender and number of
  adjectives and nouns, and by hand the pronoun pairs the list lacks (*mos*, *llelos*, *nolos*).
  Every headword in it must be a headword of the list (the checker warns otherwise). If a true form
  is missing, write around it and name the word and form in your report.
- **Names** are not in the list: people, places (*Vigo*, *Lugo*, *Ourense*), shops and brands go in
  `names`. A name of several words (*A Coruña*, *O Grove*, *Santiago de Compostela*) counts as one
  known word wherever it is written whole; after a contraction write the rest as a name too (*na
  Coruña*, *do Grove*: put *Coruña* and *Grove* in `names`). Keep only true names in `names`,
  never common words, and never a name that is also a list word: as characters avoid *Clara* (a
  form of *claro*), *Marta*, *Alba*, *Lucía* (of *lucir*), *Iria*, *Leo*, *Rosa*, *Mar*, *Luz*,
  *Sol*, *Paz*, *Cruz*, *Branca*, *Pilar*, *Ledicia*, *Lúa*, *Mía*, *Marcos*, *Óscar*, *Anxo*; as
  places *Muros*, *Cee*, *Carballo*, *Ribeira*, *Porto*, *O Barco*, *Ares*, *Os Ancares*, *Chantada*
  and *Buenos Aires* are list words too: they count at their own rank, so leave them out of `names`
  and use them only as outside words, or pick another town. Check a name's rank with `rank.py`
  before you use it (it cuts words as if they were no names: a name in `names` is never cut). Safe
  in the pilot: *Uxía*, *Brais*, *Antía*, *Xoán*, *Martiño*, *Noa*, *Nuno*, *Roi*, *Carme*,
  *Tareixa*, *Rosalía*, *Manuel*, *Paula*, *Hugo*, *Xabier*, *Inés*, *Carla*, *Diego*, *Sara*,
  *Lois*. Day and month names are in the list (*maio* 695, *domingo* 765, *setembro* 1128, *abril*
  1186, *xuño* 1202, *outubro* 1234, *marzo* 1293, *sábado* 1301, *luns* 1393, *xullo* 1453,
  *xaneiro* 1507, *agosto* 1654, *febreiro* 1727, *venres* 1757, *decembro* 1770, *novembro* 1847,
  *xoves* 1979, *martes* 2077, *mércores* 2217; *inverno* 860, *primavera* 1140, *outono* 1990;
  *verán* is filed under *ver*).
- Everyday words rank later than you would think: *casa* 74, *pai* 108, *traballo* 110, *fillo*
  113, *grazas* 127, *neno* 135, *amigo* 137, *irmán* 138, *nai* 150, *mañá* 162, *hoxe* 166, *hora*
  168, *ola* 170, *cidade* 182, *comer* 202, *auga* 229, *semana* 256, *rúa* 259, *coche* 299, *adeus*
  311, *filla* 321, *escola* 368, *can* 392, *onte* 404, *café* 546, *pan* 662, *gato* 674, *avoa*
  888, *avó* 935; the dialogue tag *di* (*dicir* 19) is in the first band, *pregunta*/*preguntar*
  (187) and *responde* (*responder* 525) come later.
- The list's form lists are noisy (old and regional spellings, look-alikes); a new word must appear
  as itself or a true inflection, never through such a form. Some entries are names, English words
  or letters: skip them. Loanwords Galician uses as its own (*web*, *vídeo*, *fútbol*, *internet*,
  *móbil*) are taught like any word.
