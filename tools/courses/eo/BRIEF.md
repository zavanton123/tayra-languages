# Writing an Esperanto frequency course

Each course teaches one band of 100 words from the Esperanto word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `eo-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Sofia ne vidas sian katon. Ŝi iras al Adamo. «Ĉu vi vidas Micin?» «Ne,
  mi ne vidas ĝin.» Ŝi iras al Rita...*
- **Where the stories happen.** Esperanto has no country of its own: it lives in families, friendships
  and meetings all over the world, and the stories show that life, in ordinary places:
  - a family that speaks Esperanto at home (parents from two countries, a child who grows up
    with it), in any city: Roterdamo, Krakovo, Prago, Budapeŝto, Vieno, Zagrebo, Ljubljano,
    Helsinko, Lisbono, Toronto, Seulo, Kioto...;
  - pen friends and online friends who write, call and send each other things (a letter, a photo,
    a link, a recipe);
  - international meetings: a summer youth meeting, a weekend meeting, a congress, a children's
    week, with their courses, excursions, concerts, games and shared meals;
  - travel: staying with Esperanto speakers in another country, being shown their town, learning
    a word of their language;
  - a local club or a course: learning Esperanto, a first conversation, a visitor from abroad;
  - and everyday life anywhere: home, family, food, shopping, work, school, the town, health, free
    time, nature, travel. Not every story needs to say that its people speak Esperanto.
  Keep each place real but ordinary; never present a real organisation, event or person as a
  character (no Zamenhof as a character, no named associations, no real congress years).
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *li*/*ŝi* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Hugos in
  two lessons blur together.
- **Plain narration**: third person, the present tense, short sentences, one idea per sentence,
  ordinary word order (subject, verb, object: *Ana vidas la katon*). Esperanto's free word order is
  real, but the learner needs the plain one; move a word only where any speaker would (*Kion vi
  volas?*, *Tie ŝi atendas.*). Little dialogue; when there is some, short and simple. Esperanto has
  no polite "you": *vi* is everyone, one person or many; politeness is in *bonvolu*, *dankon*,
  *pardonu* and in *sinjoro*/*sinjorino* + surname (*sinjorino Novak*). Never use *ci* (an old,
  rare "thou").
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present *-as*, the past *-is* and the future *-os* from the first courses
    (*venas*, *venis*, *venos*: they are fully regular, so all three are easy early); the infinitive
    after *voli*, *povi*, *devi*, *ŝati*; the imperative *-u* in its everyday uses (*venu!*,
    *vidu*, *bonvolu sidi*, *pardonu*); the article *la* (no indefinite article: *knabino* is "a
    girl"); the plural *-j* and the accusative *-n*, with the adjective agreeing (*bonan tagon*,
    *grandajn domojn*); *esti* + adjective with no *-n* (*la domo estas granda*); possessives *mia*,
    *via*, *lia*, *ŝia*, *ĝia*, *nia*, *ilia* and *sia*; *ĉu* questions and the question
    correlatives (*kio*, *kiu*, *kie*, *kien*, *kiam*, *kiel*, *kial*) with their *ti-* answers;
    *ne* before the word it negates; the numbers; age with *havi* (*ŝi havas 7 jarojn*); time with
    *je* (*je la unua*);
  - ranks 301–600: the rest of the correlative table (*ĉiu*, *neniu*, *iu*, *ĉio*, *nenio*, *io*,
    *ĉiam*, *neniam*, *iam*, *kiom*, *tiom*, *kia*, *tia*, *kies*, *ties*); the accusative of
    direction (*en la domon*, *hejmen*, *tien*) beside place (*en la domo*, *hejme*, *tie*); dates
    and durations with *-n* (*lundon*, *la tutan tagon*); comparatives and superlatives (*pli granda
    ol*, *la plej bona*, *tiel granda kiel*); *-e* adverbs (*rapide*, *bone*); *ke* clauses (*Mi
    scias, ke...*); *-u* after *ke* for wishes and requests (*Panjo volas, ke mi venu*);
  - ranks 601–1000: the conditional *-us* (*mi ŝatus*, *se mi havus tempon, mi venus*); real
    conditions with *se* + future (*se pluvos, ni restos hejme*); relative clauses with *kiu*,
    *kiun*, *kie*, *kiam*, *kies* (*la knabo, kiun mi vidis*); reported speech: Esperanto keeps
    the speaker's tense (*Li diris, ke li venos*, never *venus* for that); the common affixes as
    the list has them (*mal-*, *-ej-*, *-ist-*, *-in-*, *re-*, *-et-*, *-eg-*), each word taught as
    a word of its own (see "Derived words" below);
  - ranks 1001–1300: the active participles as adjectives (*la dormanta bebo*, *la pasinta
    semajno*, *la venonta jaro*) and the adverbial ones (*legante*, *veninte*); *por* + infinitive,
    *sen* + infinitive, *anstataŭ* + infinitive; *-ig-* and *-iĝ-* verbs (*komenci* /
    *komenciĝi*, *edziĝi*, *naskiĝi*) where the list has them;
  - ranks 1301–1600: the passive with *esti* + *-ita* / *-ata* in plain cases (*la ponto estis
    konstruita en 1920*, *la pordo estas fermita*), with *de* for the doer (and *fare de* when the
    doer is a person and *de* could be misread); *oni* for "people, one" (*oni diras, ke...*);
    *por ke* + *-u*; *kvankam*, *ĉar*, *tamen*;
  - ranks 1601–2000: the compound tenses only where a speaker needs them (*mi estas leginta* is
    rare: prefer *jam legis*); the nouns made from participles (*la parolanto*, *la gajninto*);
    *kvazaŭ*, *ju pli ... des pli*, *ĉu ... ĉu*; more word building (*-ebl-*, *-ind-*, *-ad-*,
    *-aĵ-*, *-ec-*, *-ul-*, *ek-*, *dis-*) as the list's own entries;
  - ranks 2001–2500: longer sentences joined with *dum*, *ĝis*, *antaŭ ol*, *post kiam*, *kvankam*,
    *se ne*; indirect questions (*Ŝi demandis, ĉu li venos*, *Mi ne scias, kie ĝi estas*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *tamen*, *cetere*,
    *krome*, *sekve*, *do*, *malgraŭ tio*, *laŭ mi*, *nome*, *ja*; common idioms explained by their
    context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the column (a light reflection on everyday life), dialogue carrying more of
    the story, and less common connectors (*kvankam*, *eĉ se*, *des pli*, *finfine*, *entute*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what fluent readers meet: literary prose with images
    and rhythm, academic and journalistic registers, rare and learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*ĉar*, *kiam*, *dum*, *kvankam*, *sed*, *do*),
  dialogue can carry more of the story, and a story may be told by a first-person narrator or as an
  e-mail, a letter or a diary entry (pen friends make this natural). From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a magazine, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Sofia iras al Adamo. «Ĉu vi vidas Micin?» Sofia iras al Rita. «Ĉu vi vidas Micin?»*).
- **Questions at the end**: after the story, a paragraph starting `Demandoj:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kial* and *kiel*, with fuller answers):

  ```
  Demandoj:
  Kie estas Mici?
  Mici estas sur la lito de Sofia.
  Ĉu Adamo vidas Micin?
  Ne, Adamo ne vidas ĝin.
  ```

  The checker does not strip the header yet: `Demandoj` counts as an outside word (rank 379) in
  every lesson up to rank 300. Write it anyway; the lead has the checker change.
- **Correct, standard Esperanto** (the *Fundamento* and the usage of good modern writers, as in PIV
  and the *Plena Manlibro de Esperanta Gramatiko*), neutral and natural, as a careful, fluent
  speaker writes it for adults. In particular:
  - **the six letters ĉ ĝ ĥ ĵ ŝ ŭ, always**: never the x-system (*cx*, *gx*, *ux*) or the h-system
    (*ch*, *gh*, *sh*, *au* for *aŭ*), not even in a text message or a child's note. The list holds
    those spellings as forms of the right words, so the checker counts them silently; `early.py`
    in the writers' folder flags them;
  - **the accusative** on every direct object and on the adjectives that go with it (*Mi vidas
    grandan hundon*), on a name too (*Ŝi vokas Petron*, *mian amikon Hugon*), for direction
    (*Ni iras en la ĝardenon*, *hejmen*) and for dates and lengths of time (*lundon*, *la tutan
    tagon*); never after a preposition otherwise (*kun la hundo*, *al Petro*), never on the
    complement of *esti*, *fariĝi*, *ŝajni* (*Li estas bona instruisto*);
  - ***sia*** for the subject's own (*Ana prenas sian libron*), *ŝia* for someone else's (*Ana
    prenas ŝian libron*: another woman's); *sia* never in the subject itself (*Ana kaj ŝia frato*,
    not *sia frato*);
  - ***ĝi*** for animals and things, *li* / *ŝi* for people; *oni* for people in general;
  - **reported speech keeps the tense** (*Ŝi diris, ke ŝi estas laca*), and a real future
    condition takes the future (*Se vi venos, ni iros*);
  - no elision (*l'*, *dom'*: poetic only), no *ci*, no slang, no vulgar words (the list has some:
    skip them), no neologisms where a root word is common (*rapida*, not *rapidega* for "fast";
    *malfacila*, not *difikulta*); prefer *mal-* words to rare roots (*malbona*, *malgranda*,
    *malvarma*) when both exist, as most speakers do;
  - typography: direct speech in «…» (or "…" consistently: use «…» in these courses), each
    speaker's line a paragraph of its own; the narrator's comma after the closing mark; question
    and exclamation marks of the quoted sentence inside it; a comma before *ke*, *kiu*, *kiam*,
    *ĉar*, *se* and the other subordinators (*Mi scias, ke...*), as Esperanto writes it:

    ```
    «Saluton, Ana,» diras Petro.
    «Kiel vi fartas?» demandas Ana. «Ĉu vi volas kafon?»
    ```
  - write numbers as digits (*3 fratoj*, *en 1992*, *16 jarojn*), but ordinals and clock times in
    words (*la unua*, *je la tria*, *la dua de majo*: *la 3-a* is not checker-safe, see below), and
    spell out a number word that is in your band to teach it (*du fratoj*).
- **Names are names**: places, country names (*Francio*, *Pollando*, *Usono*: the list leaves them
  out), adjectives and adverbs of peoples and languages (*franca*, *germana*, *pola*, *angla*,
  *france*), and words for peoples count as names: skip them as band words even when the
  list has them as entries; a text may still use one (*Ŝi parolas la francan*), counted at its rank.
  The language's own name, *Esperanto* (*Esperanton*), is left out of the list and goes in `names`;
  *esperanta* (4813), *esperante* (5254) and *esperantisto* (3321) are words: teach them in their
  bands. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's, *Gerda malaperis*, *Ilustrita Gramatiko*, textbooks, or any other), and not the courses
  of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*Dio*), saints, church, priests, prayer, heaven and hell, angels
  and devils, souls and spirits, and every holiday of religious origin, even as a date, a school
  break or a gift season (Christmas, Easter, carnival); *ĉielo* only as the sky; *kredi* only as
  believing a person or a fact. Zamenhof's *homaranismo* and anything about Esperanto as a creed is
  out too, and so are homages to Zamenhof: he is not a character and his day is not a holiday here;
- **politics**: the state, the government, parliament, elections, parties, presidents, kings and
  rulers, nationalism, flags, borders as disputes, and **language politics**: no "final victory",
  no language rights, no campaign for Esperanto, no comparison that runs down another language or
  its speakers. Learning, using and enjoying the language is the topic, never promoting it.
  Everyday civic life (a library, a school, a town office counter, a club's annual meeting with a
  cake) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office is fine;
  theft, arrests, prisons and trials are not); history without wars;
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual; no kissing or divorce;
- **alcohol** in every form (wine, beer, a toast, a bar for a drink, the meeting's late-night bar),
  **gambling** (lotteries, betting, casinos), drugs and smoking;
- **deliberate misspellings**: no x-system or h-system, no missing accusative "for colour", not even
  in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a place adjective or a word for a people
  or a language is skipped. Common first names from any country are fine as characters (check them
  with `rank.py`).

The themes keep to this: the history courses (*Historio tuŝebla*, *Arkeologio kaj pasinteco*,
*Esploristoj kaj vojaĝantoj*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, rulers or politics; *Popolaj festoj kaj foiroj* and *Popola arto kaj moroj*
are folk and family celebrations with no church holiday or saint named; *Publikaj servoj* is the
library, the health centre and the town office's counters, never politics; *La lingvo Esperanto*,
*Literaturo en Esperanto* and *Vortoj kaj ilia deveno* are about words, word building, learning,
books and translation, never about language politics or the movement's ideology; *Vojaĝoj al
renkontiĝoj* is the trips and the people, never the organisations; *Fabeloj kaj legendoj* tells
folk tales (clever animals, giants, wise old women) without saints, souls or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- death and injury: *morti* 108, *mortigi* 181, *morto* 214, *sango* 786, *murdi* 915, *vundi* 949;
- religion and the occult: *dio* 112, *sankta* 376, *animo* 523, *pastro* 654, *eklezio* 748,
  *religio* 863, *spirito* 879, *islama* 932, *kredo* 933, *religia* 968, *sorĉistino* 981,
  *preĝejo* 992 (*ĉielo* 550 only as the sky; *kredi* 148 only as believing a person or a fact);
- politics and royalty: *reĝo* 250, *registaro* 282, *ŝtato* 302, *potenco* 388, *nacia* 407,
  *politika* 452, *leĝo* 459, *moŝto* 504, *prezidanto* 513, *regi* 518, *kamarado* 570, *popolo*
  576, *barono* 584, *reĝa* 601, *partio* 612, *politiko* 622, *nacio* 623, *imperio* 819,
  *princino* 841, *respubliko* 855, *sendependa* 868, *imperiestro* 880, *caro* 960, *princo* 963,
  *revolucio* 986, *regno* 989;
- war, crime, police: *milito* 177, *pafi* 374, *armeo* 428, *liberigi* 439, *soldato* 450, *batalo*
  469, *batali* 484, *detrui* 575, *ataki* 608, *ŝteli* 648, *malamiko* 657, *kapitano* 682,
  *atako* 687, *polico* 744, *armilo* 827, *aresti* 828, *trupo* 867, *sklavo* 887 (*venki* 425 only
  as winning a game or a race; *savi* 390 and *protekti* 586 only in mild everyday senses);
- vulgar and insults: *damne* 666, *stulta* 807, *stultulo* 861, *idioto* 918;
- names, place adjectives, peoples and languages: *franca* 418, *angla* 453, *germana* 458,
  *eŭropa* 569, *brita* 574, *nederlanda* 661, *ĉina* 698, *amerika* 708, *usona* 732, *rusa* 801,
  *romia* 914, *hinda* 982, *greka* 994; *esperi* 651 only through its verb forms (*esperas*,
  *esperis*), never through *esperanta*;
- forms of other words and list faults: *uzita* 192 and *bazita* 539 (participles listed apart
  from *uzi* and *bazi*), *ci* 228 (archaic "thou"), *aĵo* 415 and *ejo* 766 (suffixes used as
  nouns: rare), *kuo* 718 (noise);
- *fare* 79 (the passive's "by", formal; it is in the first band, long before the passive).

The list no longer counts Wikipedia, so its page words left the top thousand; words like *jarcento*
203, *areo* 332, *regiono* 353, *artikolo* 401, *komunumo* 699, *specio* 719, *situi* 723,
*ekstera* 780, *teritorio* 929 and *distrikto* 990 are ordinary words, taught when a story makes
them plain.

Further on, the same categories hold; for example *spirito*, *preĝi*, *anĝelo*, *paradizo*,
*Kristnasko*, *Pasko*, *juĝisto*, *malliberejo*, *drogo*, *tabako*, *fumi*, *vino*, *biero*,
*ebria*, *kazino*, *loterio*, *funebro*, *tombo*, *flago*, *parlamento*, *voĉdoni*, *partio*,
*milito* and its compounds.

## The file

Write `tools/courses/eo/<id>.json`:

```json
{
  "id": "eo-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Esperanto, e.g. Manĝi kaj aĉeti",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, with their -n forms"],
  "lessons": [
    {
      "title": "Story title in Esperanto",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/eo/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *iras*, *iris*, *iru* for *iri*; *bonan*, *bonaj* for
  *bona*; *ŝin*, *ŝia* for *ŝi*). Reuse earlier lessons' new words too. `newWords` holds the word as
  the list writes it: the infinitive of a verb, the *-o* of a noun, the *-a* of an adjective (a few
  entries are plural: *multaj*, *kelkaj*, *pluraj*, *gepatroj*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, an encyclopedia-page word, a word of the content
  rules); the checker's last line lists the band words not taught yet.

Things to know about the Esperanto list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/eo/wordlist.tsv`
  (a copy of the app's `eo.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it and `extra_forms.tsv`. The
  list is counted from film subtitles (OpenSubtitles) and a news crawl (Leipzig), half each, so
  news words rank higher than in the subtitle-only languages and some home words lower:
  *domo* 239, *infano* 142, *patro* 130, *patrino* 212, *amiko* 174, *manĝi* 166, *hodiaŭ* 175,
  *lernejo* 264, *akvo* 211, *kafo* 1211, *pano* 1370, *hundo* 810, *kato* 1115, *lito* 1236.
- **Grammar is in the forms.** Each entry holds its word's regular endings: a noun's *-o -oj -on
  -ojn*, an adjective's *-a -aj -an -ajn*, a verb's *-i -as -is -os -us -u* and its participles
  (*-anta*, *-inta*, *-onta*, *-ata*, *-ita*, *-ota*, with their endings, *-ante*, *-inte*), the
  pronouns' *-n* and possessives (*mi*: *min mia mian miaj miajn*), the correlatives' *-n* and *-j*
  (*kiu*: *kiun kiuj kiujn*; *kie*: *kien*; *tie*: *tien*). `extra_forms.tsv` fills the regular
  endings the list missed. The **adverb in *-e*** is an entry of its own (*bone* 55 beside *bona* 73,
  *rapide* 190, *certe* 163), as are *hejme* 649 and *hejmen* 499, *supre* 625 and *supren* 413.
- **Derived words.** Esperanto builds words freely with affixes (*mal-*, *re-*, *ek-*, *-ej-*,
  *-ist-*, *-in-*, *-et-*, *-eg-*, *-ig-*, *-iĝ-*, *-ad-*, *-aĵ-*, *-ul-*...). The list counts them
  as it finds them:
  - A derived word with **its own entry** is a word of its own: teach it in its band in its own
    sense (*malgranda* 199, *reveni* 146, *lernejo* 264, *patrino* 212, *fariĝi* 397, *knabino*
    358, *komenciĝi* 402, *kuracisto* 683, *manĝaĵo* 510). Its
    forms do not count for its root word, and the root word's do not count for it.
  - A ***mal-* word may be taught after its base** (and the base after it): they are two words with
    opposite meanings, and a story that pairs them (*granda* / *malgranda*, *bona* / *malbona*,
    *fermi* / *malfermi*) teaches both well. The same holds for *-in-* words beside their base
    (*patro* / *patrino*, *frato* / *fratino*, *edzo* / *edzino*), *re-* verbs (*veni* / *reveni*),
    *-iĝ-* and *-ig-* verbs (*komenci* / *komenciĝi*) and the *ge-* plurals (*gepatroj*). `batch_check`
    does not flag them: they are separate entries.
  - Some derived words are **filed as forms of their root word**: the *-eg-* and pet forms
    (*domego* under *domo*, *pordego* "gate" under *pordo*, *montego*, *ĉambrego*, *ŝtonego*,
    *fajrego*, *knaĉjo* under *knabo*) and the nouns made from participles (*lernanto* under
    *lerni*, *loĝanto* under *loĝi*, *vojaĝanto*, *komencanto*). The checker counts them at the
    root's rank, so they look known early; they are never band words and never a use of the root
    word you are teaching; below rank 1000 use them only where the story makes them plain.
  - A **derived word the list does not have** (*hundeto*, *kuireja*, *relegi*) is an outside word:
    the checker shows it as "not in list". Use one when it is the natural word, and count it among
    your outside words; never coin one to dodge an outside word, and never put one in `names`.
- **Homographs and list faults**: some verbs hold a word spelled like one of their participles:
  *kanti* 498 holds *kantato* (a cantata), *bari* holds *Barato* (India), *rabi* holds *rabato* (a
  discount), *esperi* 651 holds *esperanta*'s forms (*esperantaj*, *esperantan*: "of Esperanto"):
  never count those as uses of the verb. (*kredito* 3449 and *formato* 2321 are entries of their
  own; *Esperanto* itself is left out of the list, as a name.) *multa* 77 and *kelka* 86 hold their
  plurals (*multaj*, *kelkaj*), as the dictionary has them: write `newWords` as *multa*, *kelka*.
  *povus*, *devus* and *volus* are forms of *povi*, *devi*, *voli*. *uzita* 192, *bazita* 539,
  *fermita* 1353, *difinita* 1379, *malfermita* 1459 are participles listed apart from their verbs
  (skip them; their forms count for both); *nekonata* 1407, *unuiĝinta* 1472 and *artefarita* 2133
  are real adjectives of their own.
- **How the checker cuts words**: it splits at hyphens, so *Esperanto-komunumo* is *Esperanto* (a
  name) + *komunumo*, and an ordinal in digits (*la 3-a*, *je la 7-a*) leaves a stray *a* that
  counts as an unknown word: write ordinals and clock times in words (*la tria*, *je la sepa*).
  Abbreviations (*s-ro*, *s-ino*, *ekz.*, *k.t.p.*, *t.e.*) split too: write *sinjoro*,
  *sinjorino*, *ekzemple*, *kaj tiel plu*, *tio estas*. Compounds written as one word count whole and
  must be the list's spelling (*matenmanĝo* 2608, *dormĉambro* 3577); `rank.py` shows whether a
  compound is listed.
- **Names** are not in the list: people, places (*Krakovo*, *Zagrebo*, *Seulo*), shops and brands go
  in `names`, **with every case form the text uses**: the checker compares names exactly, so a name
  in *-o* or *-a* used as an object or a direction needs its *-n* form listed too (*Petro*,
  *Petron*; *Ana*, *Anan*; *Parizo*, *Parizon*; *Esperanto*, *Esperanton*). Non-Esperantised names
  (*Kim*, *Lukas*, *Min*) usually stay without *-n*. A name of several words or with a hyphen is
  counted piece by piece: prefer one-word names. Keep only true names in `names`, never common
  words, and never a name that is also a list word: as characters avoid *Klara* (*klara* "clear"),
  *Vera* ("true"), *Marko* ("mark"), *Teo* ("tea"), *Bruno* ("brown"), *Oto*, *Rubeno*, *Iris* (a
  form of *iri*), *Roza* ("pink"), *Sana* ("healthy"), *Bela*, *Flora*, *Dana* ("Danish"), *Ema*:
  check every name with `rank.py` before you use it.
  Used in the pilot (pick others for your stories): *Ana*, *Tomaso*, *Mio*, *Lena*, *Petro*, *Maja*,
  *Jun*, *Kim*, *Sofia*, *Adamo*, *Rita*, *Dario*, *Lea*, *Danielo*, *Eva*, *Jana*, *Lidia*,
  *Johano*, *Min*, *Tereza*, *Emilio*, *Karlo*, *Oskaro*, *Lukas*, *Nora*, *Filipo*, *Leo*, *Olga*,
  *Igor*, *Pedro*, *Elena*, *Bogdan*, *Sara*; *Krakovo*, *Prago*, *Roterdamo*, *Vieno*,
  *Budapeŝto*, *Toronto*, *Seulo*, *Zagrebo*, *Brazilo*, *Japanio*, *Kanado*. Day and month names are in the list:
  *majo* 615 ... *februaro* 869 (all months 615–941), but the days rank late (*dimanĉo* 2616,
  *lundo* 3110, *vendredo* 3992, *sabato* 5288, *ĵaŭdo* 5499, *merkredo* 6104, *mardo* 7142) and so
  do the seasons (*somero* 1253, *vintro* 1283, *printempo* 1929, *aŭtuno* 2655).
- **Pronouns and little words are entries of their own**: *mi* 4, *vi* 5, *li* 11, *ĝi* 16, *ni*
  12, *ili* 20, *ŝi* 32, *oni* 58, each with its *-n* and its possessive; *sia* 38 (with *sian*,
  *siaj*, *siajn*) and *si* 168 (with *sin*) are two entries; the correlatives are each an entry
  (*kiu* 15, *tiu* 17, *tio* 19, *kio* 23, *ĉiu* 34, *kiam* 47, *kie* 56, *tiel* 61, *iu* 78,
  *ĉio* 69, *kial* 70, *io* 89, *tiam* 124, *neniu* 97, *tia* 110, *ĉiam* 123, *neniam* 120, *kia*
  127, *nenio* 121, *iom* 167, *tiom* 155, *kiom* 178, *tial* 278, *kies* 591, *ties* 1964, *iam* 317,
  *ĉie* 568; *ie* 950, *ĉia* 1464, *iel* 1990 later); *ĉi* 26 is its own word (*ĉi tie*, *ĉi tiu*:
  write them as two words, which is standard); *da* 42 after quantities (*multe da*, *glaso da*);
  *je* 72 for clock times and where no other preposition fits.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated by `eo_extra.py` in the writers' folder from the list itself and the dictionary pack's
  parts of speech (`dictionaries/eo-en.sqlite`): only regular endings, never a form the list files
  under another word. Every headword in it must be a headword of the list (the checker warns
  otherwise). If a true form is missing, write around it and name the word and form in your report.
