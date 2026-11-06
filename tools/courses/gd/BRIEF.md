# Writing a Scottish Gaelic frequency course

Each course teaches one band of 100 words from the Scottish Gaelic word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `gd-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 7,105 words, so the courses stop at rank 7100.

## The register

**One register in every course: modern standard written Gaelic for learners, in the Gaelic
Orthographic Conventions (GOC, *Gnàthasan Litreachaidh na Gàidhlig*).** Plain, correct Gaelic as
learners' books and BBC Alba's pages write it, neutral between the islands and the mainland, with
no dialect forms standing in for the standard. Keep exactly to these choices:

| Point | Write | Not |
|---|---|---|
| Accents | the grave only: *mòr*, *àite*, *fèin*, *dè* | the acute: *mór*, *fhéin*, *dé* |
| Spelling | *a-nis*, *a-mach*, *a-steach*, *a-rithist*, *an-diugh*, *an-dè*, *a-màireach*, *an-dràsta*, *a-nochd* | *a nis*, *an diugh*, *an dràsda* |
| "be" | *Tha / Chan eil / A bheil? / Nach eil?*; *Bha / Cha robh / An robh?*; *Bidh / Cha bhi / Am bi?* | *Thà*, *Tha mi'n*, *Am bheil?* |
| Yes and no | repeat the verb: *A bheil thu sgìth? Tha. / Chan eil.* *An do dh'ith thu? Dh'ith. / Cha do dh'ith.* | *Seadh* for a plain yes |
| "you" | *thu* to a friend, a child or a relative; *sibh* to several people and to anyone you would address politely. One form per pair of speakers | *tu* outside its grammar (*am faca tu*, *is tu*) |
| "have" | *Tha cù aig Mòrag*, *Tha cù agam* | *Tha cù le Mòrag* |
| Feelings | *Tha an t-acras orm*, *Tha eagal orm*, *Tha mi sgìth* | |
| "like" | *Is toigh leam* or *Is math leam*: one in a story | |
| Article | *an taigh*, *am balach*, *a' bhean*, *an t-uisge*, *an t-sràid*, *na h-òrain*; dative *air a' bhòrd*, *anns a' bhaile*; genitive *doras an taighe*, *hata a' bhalaich* | *air an bòrd* |
| "in" | *ann an taigh*, *ann am baile*; with the article *anns an taigh*, *anns a' bhaile* | *'san taigh* in narration |
| Before a vowel | *dh'*: *dh'fhàg*, *dh'òl*, *do dh'Iain*; *b'* with a space: *b' àbhaist*, *b' urrainn*; *m' athair*, *d' athair* | *do Iain*, *do athair* |
| Small words | *agus* in narration; *is* only in fixed pairs (*dubh is geal*), and *'s* for *is* the copula (*'S e...*) | *&* |

Spell everything as GOC does: the accent tells words apart and the checker compares spellings
(*bàta* "boat" / *bata* "stick", *bàs* "death" / *bas* "palm", *càr* "car" / *car* "turn", *rìgh*
"king" / *ri* "to", *ùr* "new" / *ur* "your", *mòr*, *dè* "what" / *de* "of"). Never the old
spellings: *sgoil* stays, but no *dràsda*, *an diugh*, *mór*, *am bheil*. No slang, no English mixed
into a sentence. Careful everyday expressions in dialogue are fine (*madainn mhath*, *feasgar math*,
*ciamar a tha thu?*, *tha gu math*, *tapadh leat*, *tapadh leibh*, *mas e do thoil e*, *slàn leat*).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Modern everyday life where Gaelic is spoken: the Outer Hebrides (Leòdhas, Na
  Hearadh, Uibhist, Barraigh), Skye (An t-Eilean Sgitheanach), Tiriodh, Ìle, Muile, the west coast, the
  towns and cities (Steòrnabhagh, Port Rìgh, An t-Òban, Inbhir Nis, Glaschu, Dùn Èideann, Obar Dheathain),
  Gaelic-medium schools and families who speak Gaelic at home anywhere; and now and then Ceap Breatainn
  in Canada: home, family, food, shopping, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony, jokes
  that depend on wordplay, unreliable narrators or open endings. Events in the order they happen. Few
  characters (one to three), named clearly; say who *e*/*i* is whenever it could be unclear. Give each
  story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, in Gaelic order: verb,
  subject, object (*Dh'òl Màiri tì.* *Tha Iain anns an taigh.* *Chunnaic i an cù.*). Little dialogue;
  when there is some, short and simple. Address people in the vocative with *a* and lenition
  (*a Mhàiri*, *a Sheumais*, *a Chaluim*); no *a* before a vowel or *fh* (*Iain*, *Anna*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *tha / chan eil / a bheil?* and *bha / cha robh / an robh?* for where, how and what is
    going on, and the progressive *ag* / *a'* + verbal noun (*Tha e ag obair.* *Tha i a' dol dhachaigh.*);
    the copula *'s e* for what someone or something is (*'S e tidsear a th' ann.* *'S e Mòrag an t-ainm
    aice.*), *is toigh leam*; the past of the irregular verbs (*chaidh*, *thàinig*, *rinn*, *chunnaic*,
    *fhuair*, *thug*, *thuirt*, *chuala*) with their dependent forms (*an deach?*, *cha deach*, *am faca?*,
    *chan fhaca*) and of regular verbs (*dh'fhàg*, *dh'òl*, *sgrìobh*, *an do sgrìobh?*, *cha do sgrìobh*);
    *bidh* for what happens as a rule; "having" with *aig*, feelings with *air*, the prepositional pronouns
    (*agam*, *orm*, *leam*, *dhomh*, *rium*); the article and its lenition; possessives with theirs (*mo
    thaigh*, *do chù*, *a thaigh* "his", *a taigh* "her", *a h-athair* "her father", *ar taigh*, *ur taigh*,
    *an taigh* "their"); question words (*dè*, *cò*, *càite*, *cuin*, *ciamar*, *carson*); numbers with their
    nouns (*dà thaigh*, *trì taighean*, *aon bhalach*, *trì bliadhna*, *trì latha*);
  - ranks 301–600: the future (*thèid*, *thig*, *nì*, *chì*, *gheibh*, *bheir*, *their*, *ceannaichidh*;
    *cha tèid*, *am faic?*) and the imperative (*suidh*, *thig*, *thalla*, *na dèan*, *suidhibh*);
    comparatives (*nas motha na*, *nas fheàrr na*, *cho mòr ri*); *feumaidh*, *faodaidh*, *is urrainn do*;
    the genitive in plain phrases (*doras an taighe*, *cupa tì*, *meadhan a' bhaile*); the time (*aig trì
    uairean*, *sa mhadainn*, *feasgar*), days and months (names: see below);
  - ranks 601–1000: the conditional (*bu toigh leam*, *rachainn*, *dhèanadh e*, *bhiodh*); relatives
    (*am fear a chunnaic mi*, *an t-àite far a bheil e*, *am boireannach aig a bheil cù*); reported speech
    with *gu* / *nach* (*Thuirt i gu bheil i sgìth.* *Tha mi a' smaoineachadh gun tig e.*); *nuair a*, *ma*
    + future for real plans (*Ma bhios e fuar, fuirichidh sinn aig an taigh.*); *oir*, *ach*, *agus*, *an uair
    sin*;
  - ranks 1001–1300: *b' àbhaist do* and the past habitual (*bhiodh e a' dol*, *rachadh e*), superlatives
    (*am fear as motha*, *an tè as fheàrr*), past participles (*dùinte*, *briste*), *air* + verbal noun for
    the perfect (*Tha e air ithe.*), *gus* and *airson* + verbal noun for purpose;
  - ranks 1301–1600: the impersonal (*chaidh an taigh a thogail*, *thogadh an taigh*, *thathar ag ràdh*);
    *nam* / *nan* + conditional for what is not so (*Nan robh ùine agam, rachainn ann.*), *mura*, *ged a*,
    *mus*, *gus an*; the genitive plural (*taighean nam balach*);
  - ranks 1601–2000: the verbal noun with the object before it (*Tha mi airson an leabhar a leughadh.* *Tha
    agam ris an càr a ghlanadh.*), *ga* / *gam* / *gan* (*Tha mi ga fhaicinn.*), indirect questions (*Chan
    eil fhios agam a bheil i ann.*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another, the genitive after a verbal
    noun (*a' dèanamh na h-obrach*, *ag òl a' bhainne*), reported speech with tense shifts (*Thuirt e gum
    biodh e ann.*), *air sgàth*, *a thaobh*, *a rèir*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *ge-tà*, *a dh'aindeoin sin*, *mar
    sin*, *is e sin*, *air an làimh eile*; common proverbs (*seanfhacail*) and idioms explained by context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and clear
    images, the newspaper column (a light reflection on everyday life);
  - ranks 4001–5000 (C1): more abstract and nuanced content: a short essay or reflective column that weighs
    two sides, an interview, a portrait, a review; long sentences that stay easy to parse;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and the arts,
    each made clear by its context;
  - ranks 6001–7100 (C1): the same, with figurative language and idioms used more freely, and topics that
    need some background, which the text gives.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story may be
  told by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is everyday, calm and easy to follow. A learner
  at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words three times
  or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Ceistean:` with 4–6 simple questions about
  the story, each followed on its own line by a full-sentence answer that reuses the story's words (after rank
  1000 they may also ask *carson* and *ciamar*, with fuller answers):

  ```
  Ceistean:
  A bheil taigh aig Mòrag?
  Tha. Tha taigh aig Mòrag.
  Dè an t-ainm a th' air an eilean?
  'S e Barraigh an t-ainm a th' air.
  ```

  The header `Ceistean:` is layout: the checker does not count it as a word.
- **The mutations are grammar, not spelling**: write every lenition, *h-*, *t-* and *dh'* the standard requires
  and none that it does not. The checker reads a mutated form as its word but never tells you whether it is
  right: *air a' bhòrd* and *air a' bòrd* pass alike, and only the first is Gaelic. *l*, *n*, *r* and *sg*, *sm*,
  *sp*, *st* show no lenition in writing; *d*, *t* do not lenite after *an* in the dative (*anns an taigh*, *air
  an doras*).
- **Gaelic typography**: direct speech in curly double quotation marks “…”, each speaker's line a paragraph of
  its own, the comma, question mark or exclamation mark inside the marks. The tag is *thuirt X*:

  ```
  “A bheil an leabhar agad?” thuirt Iain.
  “Tha,” thuirt Ealasaid.
  ```

  The questions and answers of the `Ceistean:` block are plain lines. A dash is the en dash with spaces ( – ).
  The apostrophe may be straight or curly, either: *a'*, *'s*, *dh'fhàg*. Write numbers as digits (*aig 8*, *ann an
  1995*, *£5*), except a number word that is in your band: spell that one out to teach it (*dà thaigh*, *trì
  bliadhna*).
- **Names are names**: place names and words for peoples and languages (*Alba*, *Gàidhlig*, *Beurla*, *Albannach*,
  *Sasannach*) are skipped as band words even when the list has them. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's or any other),
  and not the courses of the other languages either.

## Names: months, days, people

**Months and days are names:** the list mostly leaves them out, so put the ones you use in `names`, and never teach
them: *am Faoilleach*, *an Gearran*, *am Màrt*, *an Giblean*, *an Cèitean*, *an t-Ògmhios*, *an t-Iuchar*, *an
Lùnastal*, *an t-Sultain*, *an Dàmhair*, *an t-Samhain*, *an Dùbhlachd*; *Diluain*, *Dimàirt*, *Diciadain*,
*Diardaoin*, *Dihaoine*, *Disathairne*, *Latha na Sàbaid*. Two are list words as well, which you do not teach in that
sense: *gearran* 1599 ("gelding"), *dàmhair* 1552. A month or day in `names` counts as known; do not use one to reach
the two uses a new word needs.

Names free to use (none is a list word): *Màiri, Iain, Calum, Seumas, Mòrag, Catrìona, Ealasaid, Eilidh, Ruairidh,
Anna, Seonag, Dòmhnall, Alasdair, Aonghas, Fionnlagh, Peigi, Oighrig, Raonaid, Murchadh, Tormod, Niall, Uilleam,
Eòghann, Beathag, Flòraidh, Cairistìona, Marsaili, Coinneach, Lachlann, Donnchadh, Eachann, Teàrlach, Iseabail,
Mairead, Ceit, Seònaid*. **Not** names here, because they are list words or forms: *Sìne, Fionn*. Check any other
name with `rank.py`. Places free to use: *Glaschu, Steòrnabhagh, Barraigh, Leòdhas, Uibhist, Muile, Ìle, Tiriodh, Canada,
Sasainn, Alba*, and whole in `names`: *Dùn Èideann, Inbhir Nis, An t-Òban, Port Rìgh, Na Hearadh, An t-Eilean
Sgitheanach, Obar Dheathain, Ceap Breatainn*. Write a name as the grammar requires (*ann am Barraigh*, *à Glaschu*, *a
Mhàiri*, *taigh Mhurchaidh*). The checker reads a lenited or *h-*/*t-* form of a name as the name (*Mhòrag*, *Pheigi*, *na
h-Alba*); a name with a changed ending must be listed as written (*Thormoid*, *Mhurchaidh*, *Sheumais*, *Chaluim*).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church, ministers and priests, prayer, heaven and hell, angels and devils,
  the Sabbath as a rule of life, and every custom or holiday of religious origin, even as a date, a school break or a
  gift season: Christmas (*Nollaig*), Easter (*Càisg*), Hallowe'en (*Oidhche Shamhna*), and the sayings that name God
  (*taing do Dhia*, *a Dhia*, *beannachd leat*: say *slàn leat*). Use *creid* only as believing a person or a fact. A
  church may not even be a landmark. New Year (*a' Bhliadhna Ùr*) is fine;
- **politics**: the state and the government, Holyrood and Westminster, elections, parties, kings and rulers,
  nationalism, independence, flags, the Union, the clans' feuds, and Gaelic as a political question (language rights,
  campaigns); everyday civic life (a library, a school, the council's counter, a *comann* or club) is fine;
- **war, the army, weapons, violence, crime, police and courts**, and the wars and risings of Scottish history
  (Culloden, the Jacobites, the Clearances);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a story is alive and
  well, and a pet does not die;
- **vulgar or sexual words**, insults, words whose main sense is sexual, and kissing, courtship gone wrong, separation
  and divorce;
- **alcohol** in every form (whisky, beer, wine, distilleries, the pub as a place to drink), **gambling**, drugs and
  smoking;
- **mythical creatures** (fairies, kelpies, the *each-uisge*, giants, monsters), tattoos, belittling words about
  people, boxing and martial arts;
- **deliberate misspellings**: no spellings without their accents, no old or dialect spellings, not even in a child's
  speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, or a word for a people or a language is
  skipped. Common Gaelic first names are fine as characters.

Folk culture with no church holiday or saint named is fine: a *cèilidh* told as music and song (no drink), the *Mòd* as
a contest of singing, *fèisean* for children, pipe music, shinty (*iomain*), crofting, peat cutting, fishing, a market, a
birthday, a wedding told as family news (no church), the start of the school year, the summer holidays. The history
courses tell the history of everyday life, towns, crafts, tweed and herring as trades, science and travel, never wars,
rulers or disasters. *A' Ghàidhlig* and *Faclan agus an tùs* are about words, dialects and learning, never language
politics.

Skip band words that only fit these topics, and say so in your report. The skip list for the first 1000 ranks:

- religion: *eaglais* 197, *ministear* 522, *creideamh* 573, *nèamh* 700, *ùrnaigh* 896, *naomh* 922;
- politics: *taghadh* 221 only as "choosing", *stàit* 252, *rìgh* 307, *pàirtidh* 328, *ceann-suidhe* 354, *riaghaltas* 357, *nàiseanta* 444, *poileataigeach* 587, *pàrlamaid* 648, *rìoghail* 657,
  *rìoghachd* 705, *poileataigs* 756, *neo-eisimeileach* 812, *bhòt* 858, *tagraiche* 879, *poblachd* 886;
- war, crime, police, courts: *cogadh* 258, *feachd* 358, *ionnsaigh* 422 only as "attack" (*a dh'ionnsaigh* "towards" is fine),
  *saighdear* 460, *arm* 461, *sabaid* 578, *ar-a-mach* 654, *claidheamh* 684, *blàr* 727, *sgrios* 752, *poileas* 818, *ruaig* 893,
  *prìosan* 895, *laghail* 901, *cùirt* 998; *lagh* 470 only as a subject of study;
- death: *caochail* 245 only as "change", *marbh* 321, *bàs* 363, *bàsaich* 538, *murt* 994;
- alcohol: *leann* 381, *taigh-òsta* 398 only as a hotel, *fìon* 441, *uisge-beatha* 507;
- too bookish for the early bands (Wikipedia's words, list noise): *stèidheachadh* 191, *foillseachadh* 431, *eileamaid* 489,
  *siorrachd* 493, *troigh* 498, *cilemeatair* 620, *ceimigear* 645, *meatair* 694, *ceimigeach* 698, *ùghdarras* 743, *dioplòmasach*
  789, *làrach-lìn* 808, *ùghdar* 810, *ceàrnagach* 828, *atamach* 831, *ceimigeachd* 850, *duilleag* 853, *saidheansail* 947,
  *teòiridh* 969, *tràchdas* 985;
- mythical creatures and always-removed words: see the content rules; *mathan* 943 only in a wildlife story.

Further on, the same categories hold; for example *sagart* 1009, *manach* 1072, *cill* 1078, *ìmpireachd* 1124, *dia* 1130,
*easbaig* 1212, *prionnsa* 1328, *ìmpire* 1469, *crois* 1521, *banrigh* 2108, *tòrradh* 5330.

## The file

Write `tools/courses/gd/<id>.json`:

```json
{
  "id": "gd-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, as themes.tsv writes it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Gaelic",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/gd/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300, 90%
  for the first course). Names you list in `names` and numbers count as known. The checker lists every word outside the
  range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its lesson
  (any form counts, mutated too: *chaidh* or *thèid* for *rach*; *mhòr* for *mòr*; *agam* for *aig*). Reuse earlier
  lessons' new words too. `newWords` holds the word as the list writes it (the third column of the band file).
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up to rank 300,
  180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth a lesson (a form
  of another word, a list error such as a name, a piece of a place name or an English word, a word for a people or a
  language, a word of the content rules); the checker's last line lists the band words not taught yet.

Things to know about the Gaelic list and the checker:

- The courses are pinned to `tools/courses/gd/wordlist.tsv`, a copy of the app's `gd.tsv`. There is little everyday
  Gaelic text to count: half the list's weight is the Gaelic Wikipedia's articles (each sentence once, no headings,
  lists or capitalised words), half just 1,059 Tatoeba sentences and the dictionary pack's 1,595 examples; Common Voice has
  no Gaelic. So some kitchen words stand late: *feòil* 1481, *bracaist* 2616, *cidsin* 3131, *caidil* 5068. Check with
  `rank.py`; say *biadh*, *bòrd*, *taigh* where the level needs it.
- **Edge apostrophes are words**: *a'* is listed under both *ag* (*a' dol*) and *an* (*a' bhean*); *'s* and *b'* under *is*;
  *m'* under *mo*; *d'* under *do*; *th'* and *bh'* under *bi*; *dh'* on its own. A quotation mark round a word is not one.
- **Mutations**: a mutated form the list lacks counts as the word it mutates (*mhòr* is *mòr*, *dh'fhàg* is *fàg*, *t-uisge* is
  *uisge*, *h-uile* is *uile*, *m'athair* is *athair*). A word with a hyphen is one word where the list has it (*an-diugh*,
  *a-màireach*) and its parts otherwise.
- **The verb "be" is one word**, *bi*: *tha*, *bha*, *bheil*, *eil*, *robh*, *bidh*, *bhith*, *bhiodh*. *cha* holds *chan*, *is*
  holds the copula, *thoir* holds *thug* and *bheir*, *dèan* holds *rinn*. Verbal nouns may be entries of their own (*cur*,
  *dèanamh*, *tighinn*, *toirt*, *togail*); *dol* is under *rach*.
- **Prepositional pronouns are their prepositions'**: *agam* under *aig*, *orm* under *air*, *leam* under *le*, *rium* under *ri*,
  *dhomh* under *do*, *annam* under *ann*, *bhuam* under *bho*, *asam* under *à*, *umam* under *mu*, *thugam* under *gu*; the
  emphatic pronouns under their pronouns (*mise* under *mi*). *ris* is *ri*'s. Teach the preposition and use its forms.
- **Spellings shared by two words**: a form is counted for the word that holds it, so look it up with `rank.py` before
  relying on it. *nì* is "thing", though it is also *dèan*'s future; *aiste* is *à*'s, not "essay"; *rinn* is *dèan*'s, not "point";
  *na*, *nan*, *nam* are spellings of several small words.
- **The list has words the dictionary pack lacks** (2,398 of 7,105, 37 in the first 1000: *toigh*, *dòcha*, *mus*, *cùrsa*): they
  came through the simplemma lemmatiser. Many are real and common. See the writers' notes on false radicals.
