# Writing a Catalan frequency course

Each course teaches one band of 100 words from the Catalan word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ca-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *La Laia no troba el mòbil. Va a la cuina. No el troba. Va a la sala...*
  Everyday life where Catalan is spoken: Barcelona, Girona, Lleida, Tarragona, Manresa, Vic, Olot,
  Sabadell, Mataró, Tortosa, the Costa Brava, the Pyrenees and the Cerdanya, the Montseny, the Ebre
  delta; València, Castelló, Alacant, Gandia, Xàtiva, Dénia, Alcoi and the Albufera; Mallorca,
  Menorca and Eivissa; Andorra; and now and then London, Berlin or Buenos Aires, where Catalan is
  spoken at home: home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ell*/*ella* is whenever it could
  be unclear. Give each story its own names: the learner reads the course in one go, and two
  Jordis in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Catalan drops subject pronouns: write *ell*, *ella*, *jo*, *tu* only
  where Catalan would (contrast, emphasis, a new subject). Names take the personal article: *la
  Laia*, *l'Anna*, *en Biel*, *l'Arnau* (*en* before a man's name that begins with a consonant,
  *l'* before a vowel; no article when calling someone: *«Albert!»*). Little dialogue; when there is
  some, short and simple, with *tu* among family and friends and *vostè* (+ the third person) with
  strangers, older people and in shops or offices (*Vostè viu aquí?*, *el senyor Puig*, *la senyora
  Vidal*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense (*parla*, *viu*, *llegeix*, the *-eix* verbs), *ser* and
    *estar* (*és a casa*, *està cansat*), *hi ha*, *tenir*, *voler* / *poder* + infinitive, *haver
    de* (*ha de treballar*), *fer* in the weather (*fa sol*, *fa fred*, *plou*); the **passat
    perifràstic** for what happened, from the first courses (*ahir la Laia va anar al mercat*, *vaig
    dir*, *vam menjar*): *va*, *vaig*, *vam*, *van* are forms of *anar* (rank 10), so the story may be
    told in the past early, in short sentences; the weak pronouns in their plain places: reflexive
    *es diu*, *s'aixeca*, *em*/*et*/*ens*/*us* before the verb (*em dic*, *et veig*), *ho* (*no ho
    sé*), *hi* in *hi ha* and *hi és*; the contractions *al*, *del*, *pel*, *als*, *dels*, *pels*;
    simple questions (*què*, *qui*, *on*, *com*, *quan*, *per què*);
  - ranks 301–600: the **perfet** for what happened today or this week (*he dinat*, *ha arribat*,
    *hem anat*), with the passat perifràstic for yesterday and before; the imperative (*mira*,
    *vine*, *digues*, *fes*, *seu*) with the pronouns after it (*dona-li*, *fes-ho*, *posa'l*, *mira'm*);
    the object pronouns *el*, *la*, *els*, *les*, *li*, *els* before a conjugated verb (*la veu*,
    *li diu*) and after an infinitive (*vull veure-la*, *anar-hi*); possessives with the article
    (*la meva mare*, *el seu cotxe*); comparatives (*més gran que*, *millor*, *pitjor*); *perquè*,
    *quan*, *si*;
  - ranks 601–1000: the **imperfet** for background and habits (*feia sol*, *quan era petit,
    vivia a Olot*) beside the perfet and the perifràstic for events; the **futur** (*farà*,
    *anirem*, *vindrà*); the pronouns *en* and *hi* (*en vol dos*, *n'hi ha tres*, *hi vaig*,
    *hi pensa*); *estar* + gerund (*està dinant*); the relative *que* and *on*; reported speech with
    *que* (*diu que vindrà*);
  - ranks 1001–1300: the **condicional** (*voldria*, *podries*, *seria millor*), *si* + present
    for real conditions (*si plou, no hi anirem*), superlatives (*el més alt*, *el millor*),
    pronoun pairs (*m'ho*, *t'ho*, *li ho*, *l'hi*, *n'hi*, *s'hi*, *se'n*: *se'n va*, *dona-m'ho*),
    *el qual* and *què* after a preposition (*la casa on visc*, *amb què*);
  - ranks 1301–1600: the **present subjunctive** after the common triggers (*vull que vinguis*,
    *cal que*, *perquè* of purpose, *abans que*, *quan* for the future: *quan arribis*, *potser*),
    the indefinites with double negation (*no ve ningú*, *no hi ha res*, *cap*), *tot i que*,
    *encara que*, *en lloc de*, *a més de*;
  - ranks 1601–2000: the plusquamperfet (*havia sortit*), the **imperfect subjunctive** in *si*
    sentences (*si tingués temps, viatjaria*), the impersonal and passive *es* (*es venen pisos*,
    *aquí es parla català*), the passive with *ser* in plain cases (*el pont va ser construït el
    1920*), indirect questions (*no sap si vindrà*, *pregunta on és*), *com si*;
  - ranks 2001–2500: the past conditional and *si hagués sabut* (*si ho hagués sabut, hauria
    vingut*), the subjunctive after *encara que*, *sense que*, *a menys que* and feelings
    (*m'alegro que vinguis*), reported speech with tense shifts (*va dir que vindria*), *mentre*,
    *a causa de*, *gràcies a*, *segons*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *tanmateix*, *no obstant
    això*, *per tant*, *és a dir*, *atès que*, *malgrat que*, *pel que fa a*; gerund phrases; the
    **passat simple** of written narration (*arribà*, *digué*, *va arribar* in speech), for
    recognition in third-person written narration only, as Catalan books use it; common idioms
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*article d'opinió*, a light reflection on everyday
    life), dialogue that marks *tu* and *vostè*, and less common connectors (*si bé*, *per bé que*,
    *així que*, *de manera que*, *tot plegat*);
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
  After rank 1000 sentences get longer and joined (*perquè*, *quan*, *mentre*, *encara que*,
  *doncs*, *però*, *així que*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Va a la cuina. No el troba. Va a la sala. No el troba.*).
- **Questions at the end**: after the story, a paragraph starting `Preguntes:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *per què* and *com*, with fuller answers):

  ```
  Preguntes:
  Què no troba la Laia?
  La Laia no troba el mòbil.
  On és el mòbil?
  El mòbil és al cotxe.
  ```

  The header `Preguntes:` is layout: the checker does not count it as a word.
- **Standard Catalan**: the narration is the standard language of the Institut d'Estudis Catalans,
  in its central variety, neutral, correct and natural Catalan as written today, also in stories
  set in Valencia or the Balearics. Use the current spelling (2016): the diacritic accent only on
  the words that keep it (*és*, *són*, *té*, *sé*, *més*, *món*, *bé*, *mà*, *sí*, *què*, *déu*, *ús*,
  *vés*, *pèl*, *sòl*, *vós*), so *soc*, *dona* ("gives"), *venen*, *adeu*. No regional forms
  (*este*, *eixir*, *xiquet*, *vore*, the Balearic articles *es*, *sa*), no castellanismes (*bueno*,
  *vale*, *pues*, *o sigui*, *tenir que*, *hi ha que*, *anar a* + infinitive for the future: write
  *bé*, *d'acord*, *doncs*, *és a dir*, *haver de*, *cal*, *plourà*), no slang and no vulgar words
  (the list has some: skip them). Dialogue may be lightly colloquial only in what any careful
  speaker says (*ei*, *home*, *dona* as a filler, *d'acord*, *molt bé*). Always write the Catalan
  letters and the punt volat (*à è é í ï ò ó ú ü ç*, *l·l* with the middle dot U+00B7: *col·legi*,
  *pel·lícula*): the checker compares them exactly, and *pellicula* or *colegi* are not words.
  Apostrophes and hyphens as Catalan writes them: *l'home*, *d'aquí*, *s'aixeca*, *n'hi ha*, *qu'ha*;
  but *la universitat*, *la idea* (no *l'* before an unstressed *i*/*u*); *donar-li*, *fes-ho*,
  *anar-se'n*, *posa'l*, *dona'ns-ho*.
- **Catalan typography**: direct speech in Catalan quotation marks «…», each speaker's line a
  paragraph of its own; the narrator's comma goes after the closing mark, a question or
  exclamation mark of the quoted sentence inside it:

  ```
  «Hola, Anna», diu en Martí.
  «Com estàs?», pregunta l'Anna. «Vols un cafè?»
  ```

  The questions and answers of the final `Preguntes:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*3 germans*, *a les 7*, *a les 7.30*, *el 1992*), ordinals as Catalan
  abbreviates them (*1r*, *2n*, *3r*, *4t*, *5è*, *1a*, *2a*: the checker reads their endings as
  part of the number), centuries in Roman numerals (*el segle XIX*: counted as numbers), except a
  number word that is in your band: spell that one out to teach it (*dos germans*).
- **Names are names**: place names, adjectives made from place names (*català*, *valencià*,
  *espanyol*, *francès*, *europeu*, *americà*) and words for peoples or languages count as names:
  skip them as band words even when the list has them as entries; a text may still use one (*parla
  català*), counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, priests, monks, prayer, heaven and
  hell, angels and devils, and every custom or holiday of religious origin, even as a date, a
  school break or a gift season: Christmas (*Nadal*, *Sant Esteve*, *Reis*, the *pessebre*, the
  *tió* and the *caga tió*, *torrons*), Easter (*Pasqua*, the *mona de Pasqua*, *Setmana Santa*),
  *Sant Jordi* (also as a day of books and roses: avoid the day altogether), *Sant Joan* (the
  *revetlla*, the *coca*, the bonfires), *Tots Sants* with the *castanyada* and the *panellets*,
  *Carnestoltes*, the *Falles* and *Fogueres*, *la Mercè*, *Moros i cristians*, the *Patum*,
  Halloween, and saints' name days (*sant*, *onomàstica*); a town's *festa major* (a patron saint's
  feast) is avoided too. Folk customs with no church holiday or saint named are fine: *castells*,
  the *sardana*, *gegants* and *capgrossos*, a *correfoc* at a summer street party with no saint's
  feast named, a fair or market (*fira*, *mercat*), a *calçotada*, a birthday, New Year's Eve (*Cap
  d'Any*) as a family evening, the start of the school year, the summer holidays. Place names such
  as *Sant Cugat* or *Montserrat* are names, but prefer towns without a saint in their name, never
  visit a church or a monastery, and use *cel* only as the sky and *creure* only as believing a
  person or a fact;
- **politics**: the state, the government, parliament, elections, parties, ministers,
  presidents, kings and rulers, nationalism, independence, the *procés*, referendums, the *Diada*,
  flags (*senyera*, *estelada*), and borders as disputes; everyday civic life (a library, a school,
  the *ajuntament* counter, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (wine, *cava*, *vermut*, beer, *ratafia*, the *porró*, a toast, a wine
  fair, a bar visit for a drink), **gambling** (lotteries, *la Grossa*, betting, casinos), drugs
  and smoking;
- **deliberate misspellings**: no spellings without accents, no colloquial or Spanish-influenced
  spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Catalan first names are fine as characters.

The themes keep to this: the history courses (*La història a l'abast*, *Arqueologia i passat*,
*Exploradors i viatgers*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, rulers or politics; *Festes populars i fires* and *Art i costums
populars* are folk and family celebrations with no church holiday or saint named; *Serveis
públics* is the library, the health centre and the town hall's counters, never politics; *La
llengua catalana* is about words, dialects and learning, never about language politics; *Contes i
llegendes* tells folk tales without saints, dragons slain by saints or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *sant* 94, *creu* 418 (the cross, holding *creu* "believes"), *déu* 483,
  *església* 661, *religiós* 891; *gràcia* 86 only as *gràcies*; *pau* 443 only as peace and quiet
  (*deixa'm en pau*, *quina pau*);
- politics: *govern* 99, *partit* 108 (except a match or game: *un partit de futbol*, *una
  partida d'escacs*), *dret* 111 (except *a la dreta* and *dret* "standing, straight"),
  *president* 153, *polític* 164, *política* 168, *nacional* 223 (except *parc nacional*), *llei*
  240, *rei* 248, *procés* 265, *ciutadà* 326, *consell* 338 (except "advice": *un bon consell*),
  *campanya* 354, *elecció* 358, *república* 366, *civil* 372, *parlament* 448, *independència*
  548, *ministre* 577, *diputat* 585, *nació* 600, *manifestació* 662, *votar* 677, *vot* 711,
  *alcalde* 725, *democràtic* 729, *assemblea* 820, *congrés* 828, *democràcia* 849, *regne* 876,
  *bandera* 883, *candidat* 896, *constitució* 898, *electoral* 943, *príncep* 946; *independent*
  645 only of a person (*una noia molt independent*);
- war, crime, police, courts: *guerra* 169, *policia* 275, *militar* 384, *presó* 399, *lluitar*
  473, *lluita* 477, *defensa* 482, *atac* 488, *exèrcit* 501, *defensar* 614, *violència* 624,
  *atacar* 656, *tribunal* 731, *condemnar* 782, *conflicte* 796, *judici* 799, *arma* 818, *fiscal*
  832, *batalla* 857, *denunciar* 862, *amenaça* 905, *front* 910, *revolució* 962, *soldat* 965,
  *jutge* 994; *seguretat* 412 only as safety (*el cinturó de seguretat*);
- death: *mort* 130, *morir* 218, *matar* 476, *assassinat* 597, *víctima* 654;
- vulgar: *merda* 970, *fotre* 984;
- alcohol: *vi* 596;
- names, place adjectives, peoples and languages: *català* 79, *espanyol* 156, *europeu* 293,
  *valencià* 381, *francès* 389, *anglès* 401, *americà* 534, *alemanya* 681, *castellà* 697,
  *rus* 760, *indi* 815, *alemany* 918, *grec* 934, *lluís* 950;
- list noise and forms of other words: *qual* 88 (literary at this level; teach it later as
  *el qual* if a band lets you, never through *quals*), *son* 566 only as sleepiness (*tinc son*:
  its other forms are Balearic articles), *ca* 637 (holds *can*, as in names), *ve* 689 (a form of
  *venir*), *mira* 692 (a form of *mirar*), *este* 702 (the Valencian form of *aquest*), *x* 757,
  *val* 780 (a form of *valer*: teach *valer* in its band, through *val* too), *parir* 813 (ranked by
  *París*), *notícia* 749 (teach *notícies* 682, "the news", and skip the singular as its form).

Further on, the same categories hold; for example *Nadal* 1039, *papa* 1022, *vaga* 1042, *sang*
1090, *sexe* 1114, *puta* 1179, *robar* 1242, *esperit* 1313, *àngel* 1436, *collons* 1438,
*droga* 1723, *independentista* 1729, *referèndum* 1754, *ànima* 1860, *tomba* 1941, *diada* 2024,
*verge* 2246, *lladre* 2256, *hòstia* 2352, *bisbe* 2371, *alcohol* 2432, *cervesa* 2451,
*cementiri* 2484, *apostar* 2491, *fumar* 2641, *missa* 2789, *cava* 3190, *infern* 3389, *mona*
3379, *tabac* 3681, *resar* 3779, *pasqua* 4012, *funeral* 4289, *diable* 4952, *senyera* 5209,
*vermut* 5772, *loteria* 6291.

## The file

Write `tools/courses/ca/<id>.json`:

```json
{
  "id": "ca-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Catalan, e.g. Menjar i anar a comprar",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Catalan",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ca/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *va* or *anem* for *anar*, *bona* for *bo*, *coneixes* for
  *conèixer*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes
  it: the infinitive of a verb (*conèixer*, *adonar-se*), the singular of a noun, the masculine
  singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, a word of the content rules); the checker's last line
  lists the band words not taught yet.

Things to know about the Catalan list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ca/wordlist.tsv`
  (a copy of the app's `ca.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it.
- **How the checker cuts words.** As the app's reader does, it splits a word at hyphens and keeps
  the punt volat inside it (*col·legi* is one word; *donar-li* is *donar* + *li*; *anar-se'n* is
  *anar* + *se'n*). It also takes the elided words off at the apostrophe, on either side, because
  the list counts them apart: *l'home* is *l* + *home*, *d'aquí* is *d* + *aquí*, *s'aixeca* is *s*
  + *aixeca*, *n'hi* is *n* + *hi*, *qu'ha* is *qu* + *ha*, *se'n* is *se* + *n*, *posa'l* is *posa* +
  *l*, *dona'ns* is *dona* + *ns*, except the few forms the list has whole (*adona't*, *veure's*).
  The elided and reduced forms count as their words: *l*, *ls*, *lo* as *el*; *d* as *de*; *s*,
  *se* as *es*; *m*, *me* as *em*; *t*, *te* as *et*; *n*, *ne* as *en*; *ns*, *nos* as *ens*; *vos*
  as *us*; *qu* as *que*. The contractions are forms of their preposition: *al*, *als* of *a*; *del*,
  *dels* of *de*; *pel*, *pels* of *per*; *can* of *ca*.
- **The conjunction *i* is not in the list** (the dictionary pack has no entry for it): the
  checker counts it as known everywhere. It is no band word.
- **Pronouns and little words are entries of their own**: *es* 11 (the reflexive *es*, *s'*,
  *se*, *-se*, *'s*), *en* 8 (the preposition *en*, the pronoun *en*/*n'*/*-ne*, and also the personal
  article *en*: teach *en* through the preposition or the pronoun, not through names), *hi* 23,
  *ho* 34, *li* 71, *em* 30, *et* 37, *ens* 31, *us* 69; *el* 1 holds the articles and the object
  pronouns *el*, *la*, *els*, *les*, *l'*, *lo*; *jo* 67 holds *mi* (*amb mi*); *ell* 97 holds
  *ells*, *ella* 296 is its own entry (with *elles*), *tu* 202, *nosaltres* 166, *vosaltres* 576,
  *vostè* 1184. *va*, *vaig*, *vam*, *van*, *vas*, *vau* (the passat perifràstic) are forms of
  *anar* 10, so *va dir* counts for *anar* and *dir*; *ha*, *he*, *hem*, *havia* are *haver* 9 (*hi
  ha* counts for *hi* and *haver*); *és*, *són*, *era*, *soc*, *sigui*, *fou* are *ser* 6; *cal*,
  *calia* are *caldre* 150. *millor* 91, *pitjor* 664, *major* 284 and *menor* 807 are entries of
  their own, not forms of *bo*, *mal*, *gran*, *petit*; *bon*, *bona* are *bo* 58; *tothom* 235 is
  not *tot* 19.
- **Words that have no entry of their own**, filed under a commoner word spelled the same: *estat*
  ("state", under *ser*), *fet* ("fact", under *fer*), *veu* ("voice") and *vista* ("view") under
  *veure*, *dit* ("finger") under *dir*, *té* ("tea") under *tenir*, *pot* ("jar") under *poder*,
  *vas* ("glass") under *anar*, *so* ("sound") under *ser* and *es*, *sa* ("healthy") under *es*,
  *mel* ("honey") under *em*, *sopa* ("soup") under *sopar*, and the nouns made from verbs
  *ajuda*, *sortida*, *arribada*, *trobada*, *tornada*, *compra*, *espera*, *mirada*, *caiguda*,
  *presa* under their verbs. The checker counts them at that word's rank, so they look known early;
  they are never band words, and below rank 1000 use them only where the story makes their meaning
  plain. *superfície* is missing from the list altogether (an outside word).
- **Homographs**: some entries hold two words (*nou* "new" and "nine"; *sol* "sun" and "alone";
  *dur* "to carry" and "hard"; *fi* "end" and "fine"; *cap* "head", "none" and "towards"; *deu*
  "ten" and "owes"; *creu* "cross" and "believes"), and a form shared by two entries counts for both
  (*dona* "woman" 110 and "gives", of *donar* 54). Use a new word in its own sense, never through
  its homograph, and count a new word's uses in forms that are its own. A verb's form that is a
  noun of its own is left to the noun: *porta* is the door (271), so teach *portar* through
  *porto*, *porten*, *portar*, *portat*; *casa* is not *casar*'s, *costa* not *costar*'s,
  *pregunta* not *preguntar*'s, *visita* not *visitar*'s, *creu* not *creure*'s (teach *creure*
  through *crec*, *creus*, *creuen*), *deu* not *deure*'s. But *passat*, *partit*, *entrada*,
  *necessitat* and other participles count for their verb too, and *ve*, *mira*, *val* for
  *venir*, *mirar*, *valer*.
- **Pronominal verbs** are listed with *-se* (*adonar-se* 1774, *emportar-se* 2895, *queixar-se*
  3445): write `newWords` as the list does; their forms count (*s'adona*, *adonar-se*, *adona't*).
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated from the English Wiktionary Catalan dictionary (`dictionaries/ca-en.sqlite`): every
  verb's full conjugation and its participle's gender and number (*vam anar*, *vingueren*,
  *anades*, *que tu vinguis*), the bare infinitive of a pronominal verb (*adonar* of *adonar-se*),
  the gender and number of adjectives and nouns, and by hand the reduced pronouns *ls*, *ns*,
  *qu* and the hour *h*. Every headword in it must be a headword of the list (the checker warns
  otherwise). If a true form is missing, write around it and name the word and form in your report.
- **Names** are not in the list: people, places (*Girona*, *Mallorca*, *Menorca*, *València*),
  shops and brands go in `names`. A name of several words or with an apostrophe (*la Seu
  d'Urgell*, *Sant Cugat del Vallès*, *L'Hospitalet*) counts as one known word wherever it is
  written whole. Keep only true names in `names`, never common words, and never a name that is
  also a list word: as characters avoid *Marta* (a form of *mart*), *Oriol*, *Clara*, *Pau*,
  *Rosa*, *Mar*, *Neus*, *Pol*, *Bernat*, *Gemma*, *Jan*, *Roc*, *Pilar*, *Dolors*, *Lluís*, and
  pets such as *Coco*; as places *Palma*, *Maó*, *Reus*, *Terrassa*, *Figueres*, *Igualada*,
  *Sitges*, *Blanes* and *Ciutadella* are list words too: they count at their own rank, so leave
  them out of `names` and use them only as outside words, or pick another town. Check a name's
  rank with `rank.py` before you use it. Day and month names are in the list (*març* 259,
  *setembre* 277, *octubre* 302, *abril* 322, *juliol* 350, *maig* 370, *juny* 371, *febrer* 382,
  *agost* 413, *novembre* 449, *desembre* 461, *gener* 532; *dissabte* 653, *diumenge* 717,
  *divendres* 748, *dilluns* 1165, *dijous* 1218, *dimarts* 1219, *dimecres* 1250; *estiu* 450,
  *hivern* 1315, *primavera* 1405, *tardor* 1981).
- Everyday words rank later than you would think: *casa* 114, *home* 120, *aquí* 128, *avui* 140,
  *sí* 160, *nen* 167, *escola* 186, *tu* 202, *pare* 208, *amic* 210, *ella* 296, *germà* 311,
  *mare* 313, *cotxe* 459, *noi* 660, *hola* 952, *noia* 951, *gos* 1013, *avi* (holding *àvia*)
  1200, *gat* 1794, *adeu* 2402; the dialogue tag *diu* (*dir* 29) is in the first band,
  *pregunta*/*preguntar* (427) and *respon* (*respondre* 716) come later.
- The list's form lists are noisy (old and regional spellings, look-alikes such as *mansa* under
  *mà* or *mesos* under *metre*); a new word must appear as itself or a true inflection, never
  through such a form. Some entries are names, abbreviations or letters (*x*, *lluís*): skip them.
  Loanwords Catalan uses as its own (*web*, *vídeo*, *futbol*, *internet*, *mòbil*) are taught like
  any word.
