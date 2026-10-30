# Writing a Latin frequency course

Each course teaches one band of 100 words from the Latin word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `la-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense. The stories are written in classical
Latin but happen **today**: modern people, modern homes, trains, telephones, schools and offices.
They are never translations, retellings or imitations of classical texts, and never about ancient
Rome's gods, wars or emperors.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Iulia et pater domum vident. Domus est magna, sed non est nova. Iulia et
  pater aliam domum vident...* Everyday life in towns that have Latin names: *Roma*, *Mediolanum*
  (Milan), *Neapolis*, *Florentia*, *Bononia*, *Verona*, *Venetiae*, *Pisae*, *Ravenna*, *Patavium*
  (Padua), *Comum*, *Tibur*, *Brundisium*, *Tarentum*; and *Londinium*, *Vindobona* (Vienna),
  *Berolinum*, *Matritum* (Madrid), *Olisipo* (Lisbon), *Bruxellae*, *Lugdunum* (Lyon), *Turicum*
  (Zürich), *Monacum* (Munich), *Edinburgum*, *Varsovia*, *Praga*: home, family, food, shopping,
  work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *is*/*ea* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Marci in
  two lessons blur together.
- **Plain narration**: third person, present tense in the first bands, short sentences, one idea
  per sentence. Latin drops subject pronouns: write *ego*, *tu*, *is*, *ea* only where Latin would
  (contrast, emphasis). Word order is the plain order of classical prose: subject first, the verb
  often last (*Iulia librum videt*), but never a poet's scrambling (no *magnam Iulia videt domum*);
  a short sentence may end with its object or its complement where that reads naturally (*Iulia
  Romae est*, *dicit pater*). Little dialogue; when there is some, short and simple. Latin has no
  polite "you": *tu* to everyone, the person addressed by name in the vocative (*Iulia*, *Tiberi*,
  *Publi*, *pater*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–100: the present indicative, mostly the third person (*est*, *sunt*, *habet*, *videt*,
    *dicit*, *facit*, *dat*), with *videsne?*, *video*, *habeo* in short dialogue; *possum* +
    infinitive; the nominative and accusative, the genitive of possession (*pater Iuliae*, *domus
    Marci*), the dative with *do* and *dico* (*ei dat*), the ablative after *in*, *cum*, *de*,
    *ex*/*e*, *ab*/*a*, the accusative after *ad*, *per*, *post*, *inter*, *apud*; the locative of
    a town (*Romae*, *Londinii*) and *domi*; adjectives agreeing with their nouns; questions with
    *-ne*, *quis*, *quid*, *qui*, *ubi*; *nascitur*, *appellatur* as set phrases;
  - ranks 101–300: the **perfect** for single past events (*fuit*, *fecit*, *dixit*, *vidit*,
    *venit*, *dedit*, *invenit*), beside the present; the present passive of the third person
    (*vocatur*, *dicitur*, *videtur*, *fit*); relative clauses with *qui*, *quae*, *quod*; the
    ablative of time (*eo die*, *hora septima*); *volo*, *debeo*, *soleo* + infinitive; the
    enclitic *-que* (*pater filiaque*); numbers and *mille*;
  - ranks 301–600: the **imperfect** for background and habits (*erat*, *habebat*, *faciebat*);
    the **future** (*erit*, *faciet*, *videbit*, *ibit*); the imperative (*veni*, *dic*, *fac*, *da*,
    *vide*, *i*, and the plural *venite*); *ego*, *tu*, *nos*, *vos* in every case (*mihi*, *tibi*,
    *nobis*), and *meus*, *tuus*, *noster* as they come into the list; *suus* and *se* against
    *eius* and *eum*; comparatives and *quam* (*maior quam*, *melior*);
  - ranks 601–1000: the **pluperfect** and the future perfect in *si* and *cum* clauses; the
    perfect passive (*factum est*, *vocatus est*); the deponent verbs as plain verbs (*loquitur*,
    *sequitur*, *nascitur*, *utitur*); the **present subjunctive** in purpose clauses with *ut* and
    *ne* (*venit ut videat*) and in wishes and exhortations (*eamus*); superlatives (*maximus*,
    *optimus*, *celerrime*);
  - ranks 1001–1300: **indirect statement**, the accusative with the infinitive (*dicit se venire*,
    *putat domum magnam esse*); *cum* with the subjunctive in narration (*cum domum venisset*);
    indirect questions (*rogat ubi sit*); the participles as adjectives and in short phrases
    (*puella legens*, *liber scriptus*);
  - ranks 1301–1600: the imperfect and pluperfect subjunctive with the sequence of tenses; result
    clauses (*tam ... ut*); conditional sentences of every kind (*si tempus haberem, venirem*); the
    **ablative absolute** in plain cases (*cena parata*, *patre absente*);
  - ranks 1601–2000: the **gerund and gerundive** (*ars coquendi*, *ad librum legendum*, *liber
    legendus est*); reported speech with its tense shifts; *dum*, *donec*, *priusquam*, *quamquam*,
    *quamvis*;
  - ranks 2001–3000: a richer, more formal register: connectors such as *igitur*, *enim*,
    *quidem*, *tamen*, *autem*, *nam*, *praeterea*, *denique*; longer periods with participles;
    common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the short column (*commentariolus*, a light reflection on everyday life),
    letters in the classical form (*Marcus Iuliae salutem dicit ... Vale*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long periods that stay easy to parse;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what good Neo-Latin prose offers: literary prose with
    images and rhythm, an academic and a journalistic register, learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*quia*, *cum*, *dum*, *quamquam*, *sed*,
  *itaque*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter, a short report), as long as it is everyday,
  calm and easy to follow. Still one clear idea at a time: a learner at the level should follow
  every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Haec domus magna est, sed non nova. Haec domus nova est, sed non magna.*).
- **Questions at the end**: after the story, a paragraph starting `Quaestiones:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *cur* and *quomodo*, with fuller answers):

  ```
  Quaestiones:
  Quis est Marcus?
  Marcus est pater Iuliae.
  Ubi est liber?
  Liber in mensa est.
  ```

  The header `Quaestiones:` is layout: the checker does not count it as a word.
- **Classical Latin, standard spelling**: the grammar and idiom of classical prose (Caesar,
  Cicero, Nepos), the way good modern textbooks and Neo-Latin writers use it, with the list's
  spelling: **no macrons** or other marks; **i** for both the vowel and the consonant (*iam*,
  *iubeo*, *Iulia*, *eius*, *maior*; never *j*); **v** for the consonant and **u** for the vowel
  (*vivo*, *venio*, *uxor*, *ut*); *ae* and *oe* written as two letters (*caelum*, *poena*; not
  *æ*, *œ*, nor the medieval *e* for *ae*: *caelum*, not *celum*, *coelum*); the classical forms
  *nihil* (not *nichil*), *mihi*, *littera*, *quattuor*, *silva*, *milia*, *cotidie*. Where the
  list has one spelling as its headword, follow it (*epistola*, not *epistula*). No Church or
  medieval Latin (*ecclesia*, *feria secunda*), no slang, no English words. Modern things take the
  Neo-Latin words the list has (*autocinetum* 1737, *birota* 4081, *tramen* 2631, *telephonum*
  4475, *computatrum* 1963, *interrete* 2416, *televisio* 2323, *pellicula* 332 "film"), at their
  rank; otherwise describe the thing in plain words.
- **Latin typography**: direct speech in the angle quotation marks «…», each speaker's line a
  paragraph of its own; the narrator's comma goes after the closing mark, a question or
  exclamation mark of the quoted sentence inside it:

  ```
  «Video, Iulia», dicit pater.
  «Videsne, pater?» dicit Iulia. «Haec domus magna est!»
  ```

  The questions and answers of the final `Quaestiones:` block are plain lines, as above. Capitals
  only at the start of a sentence and for names (*lingua Latina*, but *Latine* and *Latinus* are
  list words, see below). Write numbers as digits (*3 libros*, *anno 2024*, *hora 7*) or as Roman
  numerals where Latin uses them (*saeculo XIX*, *L annos natus*): both count as numbers; except a
  number word that is in your band: spell that one out to teach it (*tres libros*).
- **Names are names**: place names, the adjectives of places and peoples (*Latinus*, *Romanus*,
  *Anglicus*, *Italicus*, *Germanicus*, *Hispanicus*, *Americanus*) and the adverbs of languages
  (*Latine*, *Anglice*) count as names: skip them as band words even when the list has them as
  entries; a text may still use one (*lingua Latina*), counted at its rank. Never use a list word
  as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's, *Lingua Latina per se illustrata* or any other), and not the courses of the other
  languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form, ancient or Christian: gods and goddesses (*deus*, *dea*, Jupiter,
  Venus, Mars), myths and heroes, temples and shrines (*templum*, *fanum*), sacrifices, priests
  (*sacerdos*), the Church (*ecclesia*, *episcopus*, *papa*, *monasterium*, *cathedralis*), saints
  (*sanctus*), Christ, souls (*anima*), heaven and hell, prayer, the cross, and every holiday of
  religious origin (*Saturnalia*, Christmas, Easter); the weekday names, which are gods' names
  (*dies Saturni*, *dies Solis*, *dies Veneris*) and the Church's *feria secunda* alike: say *postridie*,
  *tertio die*, *eo die*; use *caelum* only as the sky;
- **politics**: kings, emperors and rulers (*rex*, *imperator*, *princeps*, *regnum*, *imperium*),
  the state and its offices (*civitas*, *res publica*, *senatus*, *senator*, *consul*,
  *magistratus*, *praeses*, *minister*, *legatus*, *parlamentum*, *gubernatio*), parties (*factio*),
  elections (*suffragium*), laws and courts (*lex*, *ius*, *iudex*, *iudicium*), nations and
  nationalism (*natio*, *nationalis*, *politicus*), freedom as a cause (*libertas*); everyday civic
  life (a library, a school, the town's counter, a public garden) is fine;
- **war, the army, weapons, violence, crime, police and courts** (*bellum*, *miles*, *exercitus*,
  *proelium*, *pugna*, *pugno*, *hostis*, *arma*, *legio*, *victoria*, *vinco*, *castra*, *eques*,
  *pedes*, *occido*, *interficio*, *neco*, *carcer*, *accuso*); a game is won with *vincere* only
  when the band makes it plain and the brief allows it (it does not below rank 1000);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee (*mors*, *mortuus*,
  *obeo*); an old person in a story is alive and well, and a pet does not die;
- **slavery** (*servus*, *dominus* as a slave's master);
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (*vinum*, *cervisia*, a toast, a tavern visit for a drink),
  **gambling**, drugs and smoking;
- **deliberate misspellings**: no medieval or Church spellings, no *j*, no macrons, not even in a
  child's speech or a message;
- **names taught as words**: a band word that is a name, a part of names, an adjective of a place
  or a people, or a language adverb is skipped. Latin first names are fine as characters.

The themes keep to this: the history courses (*Historia quae tangi potest*, *Archaeologia et
antiquitas*, *Exploratores et peregrinatores*) tell the history of everyday life, towns, crafts,
trade, science and travel, never of wars, rulers, gods or politics; *Festa popularia et nundinae*
and *Ars et mores populares* are fairs, markets, food festivals and family celebrations with no
religious holiday named; *Officia publica* is the library, the health centre and the town's
counters, never politics; *Lingua Latina* and *Verba et origines* are about words, their history
and learning them, never about the Church; *Litterae Latinae* is about books, reading and
libraries, never a retelling; *Fabulae et narrationes* tells folk tales (clever animals, giants)
without gods, myths, spirits or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *sanctus* 61, *deus* 71, *ecclesia* 81, *religio* 402, *papa* 405, *catholicus* 408,
  *episcopus* 425, *christus* 451, *monasterium* 511, *fanum* 520, *iudaeus* 646, *religiosus*
  741, *sacrum* 836, *sacerdos* 838, *anima* 862, *crux* 905, *cathedralis* 908, *ecclesiasticus*
  933, *vicarius* 940, *virgo* 941; *templum* 381 only as a building in a town plan (better skip);
- politics: *civitas* 51, *rex* 56, *imperium* 121, *dux* 126, *ius* 127, *regnum* 136,
  *imperator* 157, *lex* 166, *praeses* 179, *factio* 189, *politicus* 264, *natio* 267,
  *nationalis* 270, *senator* 276, *princeps* 305, *legatus* 307, *potestas* 320, *minister* 334,
  *libertas* 452, *foedus* 460, *senatus* 486, *praefectus* 510, *magistratus* 547, *iudicium* 556,
  *respublica* 567, *consul* 570, *auctoritas* 573, *gubernatio* 579, *parlamentum* 584, *regia*
  631, *comitatus* 645, *rego* 659, *constitutio* 686, *concilium* 733, *curia* 783, *regno* 834,
  *praesidium* 855, *iudex* 863, *suffragium* 926, *patricius* 977, *plebs* 990;
- war, crime, police: *bellum* 72, *miles* 142, *exercitus* 246, *castrum* 331, *vinco* 366,
  *proelium* 376, *hostis* 393, *militaris* 413, *pugno* 417, *pugna* 439, *victoria* 534, *captus*
  545, *victus* 650, *arma* 671, *defendo* 674, *occido* 676, *interficio* 697, *impetus* 700,
  *eques* 719, *carcer* 738, *victor* 766, *neco* 867, *legio* 873, *inimicus* 891, *accuso* 925,
  *occupo* 945, *pedes* 979;
- death: *mortuus* 100, *mors* 146, *obeo* 913;
- slavery: *servus* 629 (and *dominus* 236 except as the owner of a house, a shop or a dog);
- alcohol: *vinum* 768;
- names, place adjectives, peoples and languages: *latinus* 109, *augustus* 250, *latine* 260,
  *anglicus* 325, *germanicus* 362, *anglice* 370, *hispanicus* 383, *americanus* 429, *theodiscus*
  431, *esperanticus* 459, *russicus* 496, *francogallicus* 501, *italicus* 507, *germanus* 711,
  *francus* 728, *atheniensis* 781, *africus* 871, *asia* 956, *sovieticus* 986, *iaponicus* 999;
- list noise and forms of other words: *geno* 980 (old *gigno*; it holds *genus*'s *genere*,
  *generis*), *altero* 839 (it holds *alter*'s *altera*), *exter* 600 (holds *extremus*: teach the
  sense "outermost, last" only if a story needs it); the split headwords *summa* 419 / *summum*
  851 / *summus* 884 and *alba* 875 / *album* 778 (teach one of each pair in its own sense: *summa*
  "sum, total", *album* "album"; *summus* and *albus* through their forms, never twice).

Further on, the same categories hold; for example *dea*, *templum*, *sacrificium*, *pontifex*,
*imperatorius*, *rebellio*, *gladius*, *militia*, *proditor*, *funus*, *sepultura*, *sepulcrum*,
*cervisia*, *ebrius*, *tabacum*.

## The file

Write `tools/courses/la/<id>.json`:

```json
{
  "id": "la-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Latin, e.g. Cibus et emptio",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in the nominative"],
  "lessons": [
    {
      "title": "Story title in Latin",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/la/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` (with their case
  forms) and numbers count as known. The checker lists every word outside the range with its rank:
  replace them, or keep a few when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *est* or *sunt* for *sum*, *magna* for *magnus*, *vident*
  for *video*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes
  it: the first person singular present of a verb (*video*, *facio*, *nascor*), the nominative
  singular of a noun, the masculine nominative singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a name, a place adjective, a form of another word, a word of the content rules);
  the checker's last line lists the band words not taught yet.

Things to know about the Latin list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/la/wordlist.tsv`
  (a copy of the app's `la.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  **Latin Wikipedia** (Leipzig Corpora Collection), so encyclopedia words rank high (*incola* 93,
  *saeculum* 49, *regio* 73, *appello* 58, *pellicula* 332, *chiliometrum* 787, *orientalis* 317) and
  everyday words, first and second persons and imperatives rank low: *mater* 259, *frater* 227,
  *puer* 397, *puella* 635, *amicus* 372, *ego* 363, *tu* 550, *nos* 774, *vos* 1622, *tuus* 1006,
  *meus* 1271, *cibus* 842, *aqua* 201, *hora* 630, *canis* 1395, *panis* 1619, *mensa* 1793, *cena*
  1468, *nox* 1561, *salve* 2375 (under *salvus*), *vale* 442 (under *valeo*), *cotidie* 2232.
  *heri* is filed under *herus* 8077 and *cras* is 7508: write around "yesterday" and "tomorrow"
  (*pridie*, *postridie*, *hodie*, *die proximo*) until then. The months are not in the list (it
  dropped them as names): put the month you use in `names` (*Maius*, *Iunius*), it then counts
  with its case forms (*mense Maio*).
- **How the checker reads a Latin word.** A word is read as it is written, letter for letter (no
  macrons, so none to compare). A word the list has is that word, whatever it ends in (*itaque*,
  *quoque*, *neque*, *bene*, *ratione*, *sive*). A word the list lacks that ends in the enclitic
  *-que*, *-ne* or *-ve* counts as the word before it when the list has that one (*filiaque* is
  *filia*, *videsne* is *vides*, *estne* is *est*, *plusve* is *plus*), or as a name when the rest
  is a name or its case form (*Marcusque*, *Romamque*). The enclitic itself is no word of its own.
  Forms the list lacks come from `extra_forms.tsv` (every form Wiktionary gives for the headword:
  all persons, tenses, moods, cases, and the declined participles of verbs whose participle is no
  entry of its own), so *ambulo*, *ambulas*, *nobis*, *vocamus*, *venite* count for their words.
- **Pronouns and little words**: *sum* 1 holds every form of "to be" (*est*, *sunt*, *erat*, *fuit*,
  *esse*, *sit*, *eram*, *sumus*); *is* 5 holds *eius*, *ei*, *eum*, *ea*, *eam*, *id*, *eorum*;
  *qui* 4 holds *quis*, *quid*, *quae*, *quod*, *quem*, *cuius*, *cui* (so the question words
  *quis* and *quid* are *qui*); *hic* 8 holds *haec*, *hoc*, *huius*, *huic* and the adverb *hic*
  "here"; *ille* 43, *ipse* 45, *idem* 79, *se* 23 (with *sibi*, *sese*), *suus* 20, *ego* 363
  (with *me*, *mihi*), *tu* 550 (with *te*, *tibi*), *nos* 774 (with *nobis*), *vos* 1622; *ab* 25
  holds *a* and *abs*; *ex* 21 holds *e*; *cum* 9 is "with" and "when"; *ut* 13 holds *uti*;
  *una* 123 is the adverb "together", *primum* 106 and *solum* 108 are adverbs of their own
  beside *primus* 30 and *solus* 223; *multum* 202 is an entry of its own beside *multus* 31;
  *maior* 104 and *minor* 211 are entries of their own beside *magnus* and *parvus*; *domi*,
  *domum*, *domo* are *domus* 84.
- **Participles as headwords**: the dictionary gives some perfect participles an entry of their
  own, and the list keeps them apart from their verbs: *situs* 105, *notus* 263, *editus* 587,
  *editum* 578, *confectus* 522, *captus* 545, *pictus* 546, *missus* 628, *victus* 650, *ductus*
  653, *auctus* 710, *descriptus* 743, *gestus* 753, *traditus* 802, *coactus* 820, *secutus* 864,
  *datus* 895, *relictus* 898 (others, *factus* and *dictus*, are filed under *facio* and *dico*).
  Their forms count for the participle, not for the verb: *pictus* is no use of *pingo*. Teach one
  only as the adjective it is (*situs* "situated", *notus* "known", *pictus* "painted"), in a band
  where its verb is not already taught; otherwise use it as an outside word of its rank.
- **Words filed under a word spelled the same**, which the checker counts at that word's rank:
  *liber* 37 holds the book, the adjective "free" and *liberi* "children"; *vita* 66 holds forms of
  *vitium* "fault" (*vitia*, *vitii*); *vicus* 113 holds *vicis* (*vice*, *vicem*, *vices*); *mos*
  199 holds *mora* "delay"; *dolor* 771 holds *dolus* "trick"; *avis* 942 holds *avia*
  "grandmother" (*avia* counts at 942: say *avia* only in a band past 942, and *avus* is 1608);
  *vas* 961 holds *vada*; *pedes* 979 holds the plural "feet" of *pes* 2222; *late* 876 holds
  *Latium*; *mane* "in the morning" is filed under *manus* 241; *ludo* (verb) gives its dative
  spelling to *ludus* 207; *maius* is *magnus*'s. Use a new word in its own sense, never through
  such a form, and count a new word's uses in forms that are its own.
- **Split headwords and leftovers of the list**: *summa* / *summum* / *summus* and *alba* /
  *album* / *albus* are separate entries (teach each sense once); *tribus* "tribe" is filed
  under *tres* 68 (never count *tribu*, *tribus* as uses of *tres*); agent nouns in *-tor* sit
  under their verbs (*fundator* under *fundo*, *laudator* under *laudo*); *opera* "effort" is under
  *opus* 33.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms), generated
  from the English Wiktionary Latin dictionary (`dictionaries/la-en.sqlite`) by
  `la-writers/make_extra_forms.py`. A form another entry owns is never added. If a true form is
  missing, write around it and name the word and form in your report.
- **Names** are not in the list: people, places, months, shops and brands go in `names`, in the
  nominative. The checker gives each name its regular case forms (*Marcus*: *Marci*, *Marco*,
  *Marcum*; *Iulia*: *Iuliae*, *Iuliam*; *Tiberius*: *Tiberi*, *Tiberii*; *Londinium*: *Londinii*,
  *Londinio*; *Neapolis*: *Neapolim*, *Neapoli*; *Athenae*: *Athenis*; *Thomas*: *Thomae*;
  *Olisipo*: *Olisiponis*; *Daniel*: *Danielis*); a form the list has as a word counts as that word
  instead (*Marce* is *marceo*'s, *Paulo* and *Pauli* are words, *Servii* is a word, *Valeri* is
  *valeo*'s): check every name's forms with `rank.py --names` before you use them. Never a name
  that is itself a list word: as characters avoid *Clara*, *Maria*, *Livia*, *Titus*, *Quintus*,
  *Sextus*, *Septimus*, *Decimus*, *Felix*, *Victor*, *Rufus*, *Severus*, *Faustus*, *Prisca*,
  *Leo*, *Laura*, *Sophia*, *Caecilia*, *Brutus*, *Cato*, *Paulus*, *Petrus*; as places *Colonia*,
  *Valentia*, *Genua*, *Lutetia* (*lutetium*), *Taurinum*, *Ostia*, *Senae*, *Parma*, *Gallia*,
  *Basilea*. Safe in the pilot: *Iulia*, *Marcus*, *Publius*, *Claudia*, *Antonia*, *Fabius*,
  *Terentia*, *Lucilla*, *Aemilia*, *Drusus*, *Gnaeus*, *Camilla*, *Aulus*, *Marcellus*,
  *Cassius*, *Agrippa*, *Sabina*, *Daniel*, *Flavia*, *Tiberius*, *Lucius*, *Octavia*, *Marcia*,
  *Albinus*, *Cornelia*, *Horatius*.
