# Writing an Armenian frequency course

Each course teaches one band of 100 words from the Armenian word frequency list (`bands/<rankUpTo>.txt`:
index, rank, word, its common forms). Course `hy-mini-0300` teaches ranks 201–300 and may use every word
ranked 1–300. Ten lessons, each introducing about ten of the band's words, so the course covers the band; a
learner who finishes it knows the 300 most common words. `themes.tsv` gives each course its theme. The list
has 10,000 words: the courses are `hy-mini-0100` to `hy-mini-10000`.

## The register

**One register in every course: modern standard Eastern Armenian, as it is written in Armenia today, in the
reformed spelling.** Plain, correct literary Armenian as learners' books, schoolbooks and careful news sites
write it, with no colloquial or dialect forms standing in for the standard and no Western Armenian. Keep
exactly to these choices:

| Point | Write | Not |
|---|---|---|
| Spelling | the reformed (Soviet-era, current) spelling: *և* as one letter (*և*, *նաև*, *կարևոր*, *Երևան*), *վ* (*հավ*, *լավ*), *ու* | the classical: *եւ* inside a word, *լաւ*, *պէտք*, *յետոյ*, *աւելի* |
| Standard, not colloquial | *այս*, *այդ*, *այն*, *այսպես*, *այդպես*, *այստեղ*, *այնտեղ*, *եղբայր*, *երեխա* | *էս*, *էդ*, *էն*, *տենց*, *նենց*, *ստեղ*, *ախպեր*, *էրեխա* |
| Eastern, not Western | *գնում եմ*, *կգնամ*, *նրանք*, *մի գիրք*, *գիրքը* | *կը գնամ*, *պիտի գնամ* for the future, *անոնք*, *գիրք մը* |
| "be" | *եմ, ես, է, ենք, եք, են*; past *էի, էիր, էր, էինք, էիք, էին*; negative *չեմ, չես, չէ*/*չի* + participle | |
| Present and past | present: participle in *-ում* + auxiliary (*Նա գնում է։*, *Ես չեմ գնում։*); past (aorist): *գնացի, գնաց, տեսավ, եկավ, ասաց* | |
| "you" | *դու* to a friend, a child or a relative; *դուք* to several people and to anyone you would address politely (a teacher, a shop assistant, an older stranger). One form per pair of speakers | |
| Article | *-ը* after a consonant, *-ն* after a vowel and before a word beginning with a vowel (*տունը*, *Աննան*, *տունն է*) | leaving it off a definite noun |
| *և* and *ու* | *և* in narration; *ու* in pairs and lists of the spoken kind (*հաց ու պանիր*) | |

**Punctuation is Armenian**, and the checker and the reader both depend on it:

- The full stop is *։* (U+0589), at the end of every sentence, questions and exclamations included.
- The question mark *՞* stands **over the stressed vowel of the word asked about**, inside the word: *Ո՞վ է
  նա։*, *Ինչո՞ւ ես ուշանում։*, *Որտե՞ղ է տունը։*, *Ե՞րբ ես գալիս։*, *Ի՞նչ ես անում։*; in a yes/no question on the
  word in focus, usually the verb or the auxiliary: *Դու տուն ունե՞ս։*, *Նա տանն է՞։* With *ու* the mark goes
  between *ո* and *ւ* (*ինչո՞ւ*, *գնո՞ւմ*). Never a Latin *?*.
- The exclamation mark *՜* (*Ա՜խ, ինչ լավ է։*) and the emphasis mark *՛* (*Արի՛։*, *Գնա՛նք։*, *Նստի՛ր։*) also
  stand over the vowel. Imperatives take *՛*.
- The comma is *,*; the Armenian *՝* (*բութ*) is for a pause where English would put a comma before an
  explanation (*Նա ունի երկու երեխա՝ Արամն ու Աննան։*); the colon is *․* (*միջակետ*), as after *Հարցեր․*.
- Never the apostrophe *՚*. Quotation marks are *«…»*.

The reader and the checker keep a word with *՞ ՛ ՜* whole and match it without them (*ինչո՞ւ* is *ինչու*), so
write them exactly where they belong.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten
separate stories on one broad everyday theme (its title), not one serial story; a lesson never needs another
lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Modern everyday life where Armenian is spoken: Yerevan (*Երևան*) and its districts, Gyumri
  (*Գյումրի*), Vanadzor (*Վանաձոր*), Dilijan (*Դիլիջան*), Lake Sevan (*Սևան*), Goris (*Գորիս*), the villages of
  Armenia; and now and then Armenian families abroad (Moscow, Los Angeles, Paris, Beirut) told as family life,
  never as history: home, family, food, shopping, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony, jokes that
  depend on wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters (one
  to three), named clearly; say who *նա* is whenever it could be unclear (*նա* is both "he" and "she"). Give each
  story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, in the neutral Armenian order:
  subject, object, verb, with the auxiliary after the participle (*Արամը գիրք է կարդում։* *Աննան տանն է։*).
  The auxiliary moves to the word in focus as Armenian has it (*Արամն է գիրքը կարդում։*), but keep that rare at
  low levels. Little dialogue; when there is some, short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *եմ / ես / է* and the negative (*չեմ, չի, չէ*) for where, how and what someone or something
    is; the present (*գնում եմ, չեմ գնում*); the past aorist of common verbs (*գնացի, գնաց, եկավ, տեսավ,
    ասաց, արեց, տվեց*); the article *-ը/-ն*; the plural *-եր/-ներ*; the cases in plain uses: genitive-dative
    *-ի* (*Արամի տունը*, *ասաց Աննային*), locative *-ում* (*քաղաքում*), ablative *-ից* (*տնից*), instrumental
    *-ով* (*մեքենայով*); the personal pronouns and *իմ, քո, նրա, մեր, ձեր, նրանց*; *ունեմ*; question words
    (*ի՞նչ, ո՞վ, որտե՞ղ, ե՞րբ, ինչպե՞ս, ինչո՞ւ, քանի՞*); numbers with the noun in the singular (*երկու տուն*, *երեք
    օր*);
  - ranks 301–600: the future (*կգնամ, չեմ գնա*), the imperative (*գնա՛, եկե՛ք, նստի՛ր, մի՛ գնա*), the perfect
    (*գնացել է, չի գնացել*); *պետք է* + subjunctive (*պետք է գնամ*), *կարող եմ գնալ*, *ուզում եմ գնալ*;
    comparatives (*ավելի մեծ, քան*); possessive suffixes (*տունս, տունդ*); the time (*ժամը երեքին*,
    *առավոտյան*, *երեկոյան*), days and months (names: see below);
  - ranks 601–1000: the past imperfect (*գնում էի*), the subjunctive (*գնամ, գնաս*) after *որ* and *եթե*, the
    future in the past (*կգնայի*); relative clauses with *որ*, *որը*, *որտեղ*, *երբ* (*Գիրքը, որ Աննան գնեց,
    սեղանին է։*); reported speech with *որ* (*Ասաց, որ հոգնած է։*); *երբ*, *եթե* + future for real plans;
    *որովհետև*, *բայց*, *իսկ*, *հետո*;
  - ranks 1001–1300: the pluperfect (*գնացել էր*), superlatives (*ամենամեծը*), the participles in *-ած* and
    *-ող* (*փակված դուռ*, *խոսող մարդը*), purpose with *որպեսզի* + subjunctive and *-ու համար* (*կարդալու համար*);
  - ranks 1301–1600: the passive in *-վ-* (*Տունը կառուցվել է։*), the causative (*-ացնել*: *զարմացնել*,
    *նստեցնել*), unreal conditions (*Եթե ժամանակ ունենայի, կգնայի։*), *թեև*, *մինչև որ*, *քանի որ*;
  - ranks 1601–2000: participial clauses before the noun (*իմ կարդացած գիրքը*, *Աննայի գնած հացը*), indirect
    questions (*Չգիտեմ, թե նա որտեղ է։*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another, the verbal noun (*կարդալը օգտակար
    է*), reported speech with the tenses kept, *ըստ*, *շնորհիվ*, *համաձայն*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *այնուամենայնիվ*, *հետևաբար*,
    *մյուս կողմից*, *այսինքն*; common proverbs and idioms explained by context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and clear
    images, the newspaper column (a light reflection on everyday life);
  - ranks 4001–8000 (C1): more abstract and nuanced content: a short essay or reflective column that weighs two
    sides, an interview, a portrait, a review; long sentences that stay easy to parse; the specialised words of
    professions, sciences and the arts, each made clear by its context;
  - ranks 8001–10000 (C2): the same, with figurative language and idioms used more freely, and topics that need
    some background, which the text gives.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story may be told
  by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a
  short informative or opinion text, as long as it is everyday, calm and easy to follow. A learner at the level
  should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words three times or
  more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Հարցեր․` (with the Armenian colon *․* or
  the full stop *։*) on its own line, then 4–6 simple questions about the story, each followed on its own line
  by a full-sentence answer that reuses the story's words (after rank 1000 they may also ask *ինչո՞ւ* and
  *ինչպե՞ս*, with fuller answers):

  ```
  Հարցեր․
  Ո՞վ է Արամը։
  Արամը Աննայի եղբայրն է։
  Որտե՞ղ է Աննան։
  Աննան տանն է։
  ```

  The header `Հարցեր․` is layout: the checker does not count it as a word.
- **Armenian typography**: direct speech in *«…»*, each speaker's line a paragraph of its own; the tag after
  it with a comma (*«Բարև», – ասաց Աննան։* or *«Բարև», ասաց Աննան։*: pick one and keep it in a course). A dash is
  the en dash with spaces ( – ). Write numbers as digits (*ժամը 8-ին*, *1995 թվականին*, *5000 դրամ*), except a
  number word that is in your band: spell that one out to teach it (*երկու տուն*, *երեք օր*).
- **Names are names**: place names and words for peoples and languages (*Հայաստան*, *հայ*, *հայերեն*,
  *հայկական*, *ռուսական*) are skipped as band words even when the list has them. Never use a list word as a
  character's name.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's or any
  other), and not the courses of the other languages either.

## Names: months, days, people

**Months and days are names here:** do not teach them, and put the ones you use in `names`, written as you write
them (*հունվարին*, *երկուշաբթի օրը*: a name's case endings are counted with it, see below). The list has most of
them as words (*մարտ* 245 is also "battle", *մայիս*, *ապրիլ*, *սեպտեմբեր*...), so a month in `names` counts as
known; do not use one to reach the two uses a new word needs.

**The checker counts a name's article and case endings with it**: list *Արամ* and it reads *Արամը, Արամն,
Արամի, Արամին, Արամից, Արամով, Արամենց*; list *Աննա* and it reads *Աննան, Աննայի, Աննային, Աննայից, Աննայով*;
list *Երևան* and it reads *Երևանում, Երևանից*. A form outside these (a stem that changes) is listed as written.

Names free to use (none is a list word): *Արամ, Աննա, Նարեկ, Տիգրան, Լիլիթ, Մարիամ, Դավիթ, Հայկ, Սոնա, Նունե,
Աշոտ, Գայանե, Լուսինե, Սաթենիկ, Մանե, Էլեն, Վահե, Սամվել, Էդգար, Սուրեն, Ռուբեն, Թամար, Մհեր, Արթուր, Գևորգ,
Լևոն, Նաիրա, Ալինա, Մարինե, Սոֆի, Էմմա, Գրիգոր, Համլետ, Արմինե, Զարա*. **Not** names here, because they are list
words or forms: *Անի* (a form of *անել*), *Կարեն* (*կարել*), *Գոռ*, *Արփի*, *Հասմիկ*, *Մերի*. Places free to use:
*Երևան, Գյումրի, Վանաձոր, Դիլիջան, Սևան, Գորիս, Արագած*; *Ջերմուկ, Աշտարակ, Կապան, Իջևան* are list words: list them
in `names` when you use them, and do not teach them. Check any other name with `rank.py`. Write names in Armenian
letters only: the reader reads no Latin letters as words.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*Աստված*), saints, church (*եկեղեցի*), priests, monasteries as places of faith,
  prayer, heaven and hell, angels and devils, and every custom or holiday of religious origin, even as a date, a
  school break or a gift season: Christmas, Easter, *Վարդավառ*, *Տրնդեզ*, and the sayings that name God (*Աստված իմ*,
  *փառք Աստծո*). An old monastery may not even be a landmark. New Year (*Նոր տարի*) is fine;
- **politics**: the state and the government (*կառավարություն*, *նախագահ*, *վարչապետ*, *նախարար*, *պատգամավոր*),
  elections, parties, kings and rulers, nationalism, borders, Karabakh, the genocide, Turkey and Azerbaijan as
  questions, the Soviet Union as politics, and language as a political question; everyday civic life (a library,
  a school, the post office, a passport office queue) is fine;
- **war, the army, weapons, violence, crime, police and courts**, and the wars of Armenian history;
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a story is alive
  and well, and a pet does not die;
- **vulgar or sexual words**, insults, words whose main sense is sexual, and kissing, courtship gone wrong,
  separation and divorce;
- **alcohol** in every form (cognac, wine, beer, *օղի*, toasts), **gambling**, drugs and smoking (a hookah too);
- **mythical creatures** (dragons, *ալք*, giants, monsters), tattoos, belittling words about people, boxing and
  martial arts;
- **deliberate misspellings**: no classical or Western spellings, no colloquial forms (*էս, էդ, տենց, ախպեր*), no
  marks left out, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, or a word for a people or a language
  is skipped.

Folk culture with no church holiday or saint named is fine: dances and songs, *լավաշ* baked at home, the
*դուդուկ* and other instruments, apricots and the harvest, carpets and crafts, a market, a birthday, a wedding
told as family news (no church, no toasts), the start of the school year (September 1), the summer holidays.
History courses tell the history of everyday life, towns, crafts, the alphabet and books, science and travel,
never wars, rulers or massacres.

Skip band words that only fit these topics, and say so in your report. The skip list for the first 1000 ranks
(rank in the pinned list):

- politics and the state: *նախագահ* 103, *կառավարություն* 155, *վարչապետ* 159, *քաղաքացի* 152, *ազգային* 164,
  *նախարար* 167, *իշխանություն* 203, *հանրապետություն* 206, *իրավունք* 200 only as "a right" in plain everyday
  use, *քաղաքական* 225, *ոստիկանություն* 227, *պետական* 259, *ժողովուրդ* 267, *օրենք* 288, *պատգամավոր* 305,
  *նախարարություն* 346, *պետություն* 354, *անվտանգություն* 373, *կուսակցություն* 456, *իշխանություն*,
  *ընտրություն* 419 only as "choice", *պաշտպանություն* 442, *խմբակցություն*, *սահմանադրական*, *հեղափոխություն*,
  *խորհրդարան*, *դեսպան*, *նահանգ*, *ադրբեջանական*, *ռուսական*, *եվրոպական*;
- war, crime, police, courts: *պատերազմ* 202, *դատարան* 221, *սպանել* 280, *ուժ* 165 only as "strength",
  *բանակ*, *զինվոր*, *զորք*, *ռազմական*, *զենք*, *թշնամի*, *քրեական*, *դատական*, *փաստաբան*, *դատավոր*,
  *սպանություն*, *հարձակում*, *զոհ*, *ոստիկան*, *դավաճան*, *հանցագործություն*, *կռիվ*, *կրակել*, *սուր*, *թուր*,
  *դաշույն*, *մարտ* 245 only as the month (a name), *մարտական*, *արքա*, *արքայազն*, *թագավոր*, *գահ*, *պալատ* only
  as a building;
- religion: *եկեղեցի* 322, *աստված*, *սուրբ*, *տաճար* only as a building of the past;
- death: *մահ* 204, *մահանալ*, *մեռնել*, *գերեզման*, *վերք*, *արյուն*;
- the pandemic of the news years (list noise from the 2020 news corpus): *կորոնավիրուս* 185, *համավարակ*, *վարակ*,
  *դիմակ* only as a carnival mask;
- names and peoples: *հայ* 173, *հայկական* 298, *հայոց*, *Հայաստան*, the months.

Further on, the same categories hold.

## The file

Write `tools/courses/hy/<id>.json`:

```json
{
  "id": "hy-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, as themes.tsv writes it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Armenian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after that `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/hy/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names you list in `names` (with their case endings) and numbers count as known. The
  checker lists every word outside the range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson (any form counts: *գնացի* or *գնում* for *գնալ*; *տան* for *տուն*; *է* for *եմ*). Reuse earlier lessons'
  new words too. `newWords` holds the word as the list writes it (the third column of the band file).
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up to rank 300,
  180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth a lesson (a
  form of another word, a list error such as a name or a piece of one, a word for a people or a language, a word of
  the content rules); the checker's last line lists the band words not taught yet.

Things to know about the Armenian list and the checker:

- The courses are pinned to `tools/courses/hy/wordlist.tsv`, a copy of the app's `hy.tsv`. Two fifths of its weight
  are a small news corpus (2020: the pandemic, the government), a quarter Armenian Wikipedia (each sentence once), a sixth
  a very small subtitle corpus and a fifth Tatoeba's everyday sentences. Even so, kitchen and family words stand
  later than a learner meets them: *հաց* 1084, *թեյ* 1240, *կատու* 569, *տատիկ* 2286, *հեռախոս* 1014, *ավտոբուս*
  1327. Check with `rank.py`; say *ուտելիք*, *սեղան*, *տուն* where the level needs it.
- **Forms are written out**: the list files each form under its word (*գնացի*, *գնում*, *կգնա* under *գնալ*; *տան*,
  *տանը*, *տնից* under *տուն*; *է*, *էր*, *չի* under *եմ*). The negative *չ-* and the future *կ-* are written onto the
  verb (*չգիտեմ*, *կգնամ*): when the checker reads one as outside, the form is missing; write around it or report
  it.
- **Spellings shared by two words**: a form is counted for the word that holds it, so look it up with `rank.py`
  before relying on it. *կա* (there is) holds *կան, կար, չկա*; *մեր* is under *մենք*, *ձեր* under *դուք*, *իր* is
  its own word; *որոշում* is "decision" and also a form of *որոշել*; *գնում* is both *գնալ*'s and "purchase".
- *ի* 97 is the old preposition of fixed phrases (*ի դեպ*, *ի վերջո*, *ի՞նչ* is *ինչ*).
