# Writing a Macedonian frequency course

Each course teaches one band of 100 words from the Macedonian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `mk-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Сара сака куче. Таа оди во градот. Таму има три кучиња...* Everyday life in
  North Macedonia: Skopje and its quarters (Карпош, Аеродром, Центар, Кисела Вода), Ohrid and
  Struga on Lake Ohrid, Bitola, Prilep, Kumanovo, Tetovo, Veles, Štip, Strumica, Kavadarci,
  Gevgelija, Kočani, Berovo, Kriva Palanka, Lake Prespa and Lake Dojran, the Vardar, the Treska
  and the Matka canyon, the Šar, Pelister, Galičica and Vodno mountains, Mavrovo, villages and a
  grandmother's house with a garden and a yard; and now and then Macedonian families in Melbourne,
  Toronto, Berlin, Vienna or Zurich, where Macedonian is spoken at home: home, family, food,
  shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *тој*/*таа* is whenever it could
  be unclear. Give each story its own names: the learner reads the course in one go, and two
  Stefans in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object), the short pronouns in their place (*Го гледам.* *Тој ме гледа.* *Дај
  ми ја книгата.* *Не го гледам.*). Little dialogue; when there is some, short and simple, with
  *ти* among family and friends and *Вие* with strangers, older people and in shops and offices
  (*господине Петровски*, *госпоѓо Стојановска*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *сум* in the present (*јас сум*, *ти си*, *тој е*, *ние сме*, *вие сте*, *тие
    се*) and *беше*/*бев* for "was"; *има* and *нема* ("there is", "there isn't", "has"); the
    present tense of both aspects; *да* + present after *сака*, *може*, *треба*, *мора* (Macedonian
    has no infinitive: *Сака да јаде.*); *ќе* + present for the future (*Утре ќе оди на море.*)
    and *нема да* for its negative; the definite article on the first word of the noun phrase
    (*куќата*, *детето*, *луѓето*, *градот*; *големата куќа*, *мојата соба*), and from the start
    the near and far articles where the story points at something (*куќава* "this house here",
    *куќана* "that house there"); the short pronouns *ме*, *те*, *го*, *ја*, *нѐ*, *ве*, *ги*
    (whom) and *ми*, *ти*, *му*, *ѝ*, *ни*, *ви*, *им* (to whom), with **clitic doubling**:
    a definite or named object is repeated by its short pronoun (*Книгата ја чита.* *Го гледа
    Петар.* *На Сара ѝ дава кафе.*); *се* and *си* with their verbs (*се вика*, *се враќа*, *си
    оди*); *не* before the short pronouns and the verb (*Не го знае.*); yes/no questions with
    *дали* or *ли* (*Дали сакаш кафе?* *Сакаш ли кафе?*); *кој*, *што*, *каде*, *кога*, *зошто*,
    *како*, *колку*; the aorist of the commonest perfective verbs as set phrases (*рече*, *дојде*,
    *направи*); numbers;
  - ranks 301–600: the **aorist** for finished past events (*отиде*, *виде*, *купи*, *дојдоа*) and
    the **imperfect** for what was going on or usual (*беше*, *имаше*, *одеше*, *врнеше*); aspect
    pairs side by side (*купува* / *купи*: the perfective after *да* and *ќе* for one finished act,
    *Сака да купи леб.*; the imperfective for what goes on or is repeated, *Секој ден купува леб.*);
    the imperative (*кажи*, *дојди*, *дај*, *седни*, *дојдете*); *ми се допаѓа*, *ми треба*; the
    vocative of names and family words (*Петре!*, *Ано!*, *мамо!*, *бабо!*); comparatives with *по-*
    and *нај-* written as one word (*поголем*, *најубав*); *затоа што*, *бидејќи*, *ако*, *за да*;
  - ranks 601–1000: the **има-perfect** (*има направено*, *имам видено*) and the **сум-perfect**
    with the l-form (*сум бил*, *сме оделе*, *Дали си јадел?*); the future in the past (*ќе
    одеше*, *ќе дојдеше*); the relatives *што*, *кој*, *која*, *кое*, *кои*; reported speech with
    *дека* (*Рече дека ќе дојде.*); *додека*, *пред да*, *откако*, *кога* + future meaning (*Кога
    ќе дојдеш, ќе јадеме.*);
  - ranks 1001–1300: the conditional with *би* (*би сакал*, *би било убаво*, *ако имав време, ќе
    одев...*), the past perfect (*беше отишол*, *беше направил*), participles as adjectives
    (*отворен*, *напишан*, *скршен*), every number with its noun (*двајца браќа*, *три часа*, *пет
    дена*), the count form of masculine nouns (*два стола*, *три дена*);
  - ranks 1301–1600: the indefinite pronouns (*некој*, *нешто*, *никој*, *ништо*, *секој*,
    *кој било*, *што било*), verbal nouns in *-ње* (*читање*, *купување*), the passive with *се*
    (*Куќата се гради.*) and with *е* + participle (*Продавницата е затворена.*), *иако*, *макар
    што*, *наместо да*, *освен*;
  - ranks 1601–2000: the **renarrative** (l-form) in retold stories and hearsay (*Велат дека таму
    имало езеро.* *Тој бил во Париз.*), the adverbial participle in *-јќи* (*мислејќи*,
    *одејќи*), *не само... туку и*, *колку... толку*;
  - ranks 2001–2500: participle phrases, the conditional of the past (*ако знаев, ќе дојдев*),
    reported speech in longer passages, *поради*, *благодарение на*, *според*, *како резултат на*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *впрочем*, *сепак*,
    *во меѓувреме*, *имајќи предвид*, *во врска со*, *во однос на*; long participle phrases; common
    idioms and proverbs explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper *колумна* (a light reflection on everyday life), dialogue that
    marks formal and informal speech (*Вие* against *ти*), less common connectors (*макар и*,
    *и покрај тоа*, *дури и да*);
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
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*затоа што*, *бидејќи*, *кога*, *додека*,
  *иако*, *затоа*, *но*, *сепак*), dialogue can carry more of the story, and a story may be told by
  a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text (a *колумна*, a letter to a newspaper, a short
  report), as long as it is everyday, calm and easy to follow. Still one clear idea at a time: a
  learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Тука ли е? Не, не е тука. Таму ли е? Не, не е таму.*).
- **Questions at the end**: after the story, a paragraph starting `Прашања:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *зошто* and *како*, with fuller answers):

  ```
  Прашања:
  Што сака Сара?
  Сара сака куче.
  Каде оди Сара?
  Сара оди во градот.
  ```

  The header `Прашања:` is layout: the checker does not count it as a word.
- **Standard Macedonian**: neutral, correct and natural Macedonian as it is written today, in the
  standard spelling (the *Правопис на македонскиот литературен јазик*), with all its agreements;
  no slang, no obscenities, no dialect or Serbian forms in the narration (*шо*, *ке*, *че*, *сеа*,
  *тува*, *сад*, *шчо* are spoken, regional or Serbian spellings: never written). Points a writer
  gets wrong:
  - **the three articles**: *-от*, *-та*, *-то*, *-те* (plain), *-ов*, *-ва*, *-во*, *-ве* (near
    the speaker: *куќава*, *денов*, *годинава* "this year") and *-он*, *-на*, *-но*, *-не* (over
    there: *куќана*, *човекон*); the article goes on the first word of the noun phrase (*новата
    куќа*, *мојот брат*), never on a name, and family words with a possessive short pronoun take
    none (*мајка ми*, *брат ми*, *сестра ѝ*);
  - **clitic doubling**: a definite direct object and every indirect object with *на* are doubled
    by the short pronoun (*Ја видов Сара.* *Писмото го пиша вчера.* *На Петар му рече.*);
    an indefinite object is not (*Купи книга.*);
  - **the order of the short pronouns**: *не*, *да*, *ќе* first, then the indirect, then the direct
    pronoun, then the verb (*Не ми го даде.* *Ќе ти ја донесам.* *Сака да ни ги покаже.*); after an
    imperative they follow it (*Дај ми го!* *Кажи му!*); in a sentence with no *не*/*да*/*ќе* they
    stand before the verb, and may begin the sentence (*Го видов.* *Му реков.*);
  - **ѝ, нѐ, сѐ**: *ѝ* ("to her") is written with its grave accent, which tells it from *и* ("and"):
    *Дај ѝ ја книгата.* *нѐ* ("us") is told from *не* ("not") and *сѐ* ("everything") from *се*
    the same way. Write the one Cyrillic characters ѝ (U+045D) and ѐ (U+0450), never a Latin è or a
    combining accent;
  - **по-** and **нај-** are joined to the word (*поголем*, *најдобро*, *подобро*), and *не* is
    written apart from verbs (*не знам*) and joined in words that need it (*недела*, *невозможно*);
  - **ќе** and **да** are words of their own, written apart (*ќе дојде*, *да оди*);
  - **Вие** with a capital in polite address to one person in a letter; in dialogue the polite
    *вие* takes the plural verb (*Вие сте многу љубезна!* to one woman);
  - never put stress marks (acute accents) in the text, except the ѝ and ѐ above: they break words
    apart for the reader.
- **Macedonian typography**: direct speech is set out as Macedonian books set it, each speaker's
  line a paragraph of its own beginning with an em dash and a space:

  ```
  — Здраво, Сара! — вели Петар.
  — Здраво! — одговара Сара.
  ```

  The questions and answers of the final `Прашања:` block are plain lines, as above. Macedonian
  quotation marks are „…“ (low, then high): *Таа мисли: „Ќе дојдам.“* A dash inside a sentence is
  the en dash with spaces ( – ). Write numbers as digits (*7 часот*, *во 7:30*, *3 деца*, *2,5
  килограми*, *1.000 денари*), except a number word that is in your band: spell that one out to
  teach it (*седум дена*).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age, every country and every faith. Keep away from:

- **religion** in every form, of every faith: God and gods, the church and its people (*црква*,
  *поп*, *свештеник*, *манастир*, the churches and monasteries of Ohrid as such), prayer, fasting,
  saints and their days; the holidays and customs of religious origin, even as a date, a meal, a
  trip home or a gift season: Christmas (*Божиќ*, *Бадник*, *бадникова вечер*), Easter
  (*Велигден*, painted eggs), *Водици*, *Илинден* (a saint's day and a state holiday), *Ѓурѓовден*,
  *Св. Никола*, *Св. Климент*, *Св. Наум* (the monastery: Ohrid's beach and the springs of the
  Crni Drim may be visited without it), *слава* (a family saint's feast), name days, *Курбан
  Бајрам* and *Рамазан*; religious words and formulas (*боже*, *господи*, *ако даде господ*, *фала
  му на бога*, *ѓавол*, *пекол*, *рај*). Birthdays, New Year's Eve and New Year, weddings without
  their rites, *Мартинки* on 1 March, harvest feasts, village fairs (*собир*, *саем*) without a
  church, folk music and *оро* are fine;
- **politics**, the state, elections, parties, ministers, presidents, mayors, kings, tsars and
  queens (fairy tales and legends included), borders, the country's name, national days and
  nationalism, the language question, the Ottoman and Yugoslav past and every country's disputes
  and conflicts; everyday civic life (a library, a school board, a meeting of a building's
  residents, the *општина* office's opening hours, a new identity card) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  parking attendant is fine; thefts, arrests, prisons, lawyers and trials are not);
- **death**, killing, funerals, mourning, blood and injury beyond a scraped knee; natural
  disasters (the 1963 Skopje earthquake included) only as preparation and calm, never with
  victims; an old person in a story is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, and words whose main sense is sexual;
- **alcohol** (*вино*, *пиво*, *ракија*, *мастика*, *наздравје*, bars, the wine fairs of Kavadarci),
  **gambling** (betting, lotteries, *лото*, casinos), **drugs and smoking** (*цигара*, *пуши*);
- **nationalities and place adjectives** (*македонски*, *Македонец*, *англиски*, *скопски*,
  *охридски*) are not taught as words, nor are language names (*зборува англиски* is fine in a
  text, but *англиски* is never a new word);
- **deliberate misspellings**: no dialect or Serbian spellings, no spellings without ѓ, ќ, ѕ, џ,
  љ, њ, ј, not even in a child's speech or a text message;
- **person names taught as words**: a band word that is a name, a surname, a title, a place
  name, a brand or a word made from a place or a people is skipped.

The themes keep to this: *Историја што може да се допре*, *Археологија и минато* and *Пронајдоци
и пронаоѓачи* tell the history of everyday life, towns, crafts, trade, science and travel, never
of wars, rulers or politics; *Народни веселби и саеми* and *Народна уметност и обичаи* are folk and
family celebrations with no church holiday or saint named; *Јавни услуги* is the library, the
health centre and the municipality's counters, never politics; *Македонскиот јазик* is about words,
dialects, spelling and learning, never about the language's name or politics; *Приказни и
легенди* tells folk tales without saints, miracles or kings.

Skip band words that only fit these topics, and say so in your report. Those of the first 1000
ranks:

- religion: *бог* 383, *црква* 364, *душа* 836, *дух* 847, *свет* 59 only as "world"
  (never "holy", *свети*); *верува* 218 only as believing a person or a fact (*Не ми верува.*);
  *празник* 830 only for secular holidays (*Нова година*);
- death, violence, crime: *смрт* 299, *убие* 372, *умре* 522, *мртов* 645, *крв* 575, *жртва* 595,
  *убиство* 732, *затвор* 388 (the noun: *затвори* "to close" 1418 is a word of its own and fine, *затворен* "closed" too), *полиција* 284,
  *полицаец* 682, *суд* 459, *судија* 844 (also as a referee), *закон* 179, *казна* 940, *доказ*
  642, *војна* 226, *воен* 385, *војник* 741, *војска* 807, *армија* 956, *оружје* 738, *напад*
  318, *одбрана* 688, *борба* 387, *бори* 508, *удар* 849, *повреден* 959, *безбедност* 694 only
  as safety (*безбедност на патот*);
- politics and the state: *држава* 111, *државен* 259, *влада* 164, *претседател* 163, *политика*
  299, *политички* 246, *партија* 268, *министер* 397, *министерство* 605, *власт* 242, *избор* 214
  only as "choice", never elections (*избори*), *избере* 728 only for choosing a thing (*избира боја*),
  *референдум* 900, *устав* 795, *пратеник* 832, *собрание* 721, *комисија* 872, *кандидат* 948,
  *кампања* 868, *протест* 477, *граѓанин* 194, *граѓански* 619, *национален* 252 (except *национален
  парк*), *народ* 278, *народен* 747 only in *народна музика*, *народна приказна*, *народно оро*,
  *нација* 845, *мир* 634, *меѓународен* 269, *територија* 855, *граница* 434, *знаме* 806, *крал*
  454, *премиер* 326, *политичар* 897, *јавност* 631, *режим* 984, *криза* 750;
- vulgar, sexual, alcohol: *секс* 837, *ебе* 949, *вино* 772; *пие* 921 only with water, coffee,
  tea, juice or milk;
- nationalities, place adjectives and languages: *македонски* 96, *американски* 267, *руски* 286,
  *европски* 335, *англиски* 363, *српски* 487, *француски* 521, *бугарски* 526, *грчки* 612,
  *хрватски* 735, *германски* 770, *турски* 783, *кинески* 928, *шпански* 962, *британски* 996;
- forms, variants and noise: *сад* 220 (Serbian "now"; a "dish" or "pot" only if a story needs
  one), *он* 812, *еј* 877, *кос* 917 (a piece of a name), *дама* 756, *медиуми* 646 (the plural of
  *медиум*), *пушти* 607 only as "let go, play (music)", never smoking.

The ranks above are those of the pinned list; check a word with `rank.py` before you rely on it.
Further on, the same categories hold: *дрога* 1005, *оган* 1370 only as a fire in a stove,
*пиво* 1503, *манастир* 1568, *ѓавол* 1676, *поп* 1711, *цигара* 1779, *гроб* 1999, *рај* 2253,
*молитва* 2282, *крадец* 2315, *пекол* 2413, *погреб* 2529, *пијан* 2691, *пуши* 2722, *кражба* 2818,
*коцка* 3326 (except a cube or a sugar lump), *бира* 3625 only as "chooses", *слава* 1557, *правда*
1060, *избори* 5285: check each word of your band against the categories.

## The file

Write `tools/courses/mk/<id>.json`:

```json
{
  "id": "mk-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Macedonian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places and brands used in the texts, and the vocatives used"],
  "lessons": [
    {
      "title": "Story title in Macedonian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/mk/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *реков* or *рекоа* for *рече*, *куќата* for *куќа*,
  *новиот* for *нов*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes
  it: the third person singular present of a verb (*сака*, *оди*, *види*, *каже*), the masculine
  singular of an adjective (*нов*, *голем*), the bare singular of a noun (*куќа*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, a letter, an abbreviation, a list error such as a
  name, or a form of a word already taught); the checker's last line lists the band words not
  taught yet.

Things to know about the Macedonian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/mk/wordlist.tsv`
  (a copy of the app's `mk.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it, and `extra_forms.tsv` adds
  the forms the list's 40 forms per word leave out: the near and far articles of every articled
  form (*куќава*, *куќана*, *новиов*, *новион*) and the rarer person forms of verbs.
- **Verbs are listed in the third person singular present**, as Macedonian dictionaries cite them
  (*сака*, *оди*, *прави*), and every form of the verb counts for it: *рече* holds *реков*,
  *рекол*, *речи*; *дојде* holds *дојдов*, *дојдоа*, *дојди*.
- **Aspect pairs are two words.** The perfective and the imperfective of a verb are entries of
  their own (*направи* 82 and *прави* 97, *дојде* and *доаѓа*, *даде* and *дава*, *стане* and
  *станува*, *почне* and *почнува*, *врати* and *враќа*, *купи* 478 and *купува*). Each is a word to
  teach in its own band: a learner who knows *купи* still has to learn *купува*. Teach each in its
  own sense (the perfective for one finished act, after *да* and *ќе* and in the aorist; the
  imperfective for what goes on or is repeated), and never use one to count for the other.
  *сум* 5 ("to be": *е*, *беше*, *бил*, *сме*, *сте*) and *биде* 29 (*да биде*, *ќе биде*, *бидат*)
  are a pair the same way.
- **Reflexive verbs** are listed without their pronoun: *надева* is *се надева* ("to hope"),
  *сеќава* is *се сеќава* ("to remember"), *смее* is *се смее* ("to laugh"). Teach them with their
  *се* or *си*; *се* 6 and *си* 26 are words of their own.
- **The short pronouns are words of their own**: *го* 12, *ги* 17, *ја* 18, *ми* 31, *му* 54, *им*
  64, *ме* 66, *ни* 76, *те* 79, *ви* 92, *ве* 141, *ѝ* 548, *нѐ* 1417. The long forms belong to
  their pronoun (*мене* to *јас*; *тебе* to *ти*; *него*, *нему* to *тој*; *неа*, *нејзе* to *таа*;
  *нас*, *нам* to *ние*; *вас*, *вам* to *вие*; *нив*, *ним* to *тие*). *ти* is both "you" and "to
  you", *си* "you are", "to oneself" and "one's own", *е* is "is" (a form of *сум*): each counts
  for its one entry. *ќе* 11 and *да* 4 are words.
- **The article is part of the noun**: *куќата*, *куќава*, *куќана*, *детето*, *луѓето*,
  *градот*, *градов*, *градон* are forms of *куќа*, *дете*, *човек*, *град*. *луѓе* ("people")
  is a form of *човек* 37.
- **Words made from a word are words of their own**: the relational adjectives (*годишен*,
  *градски*, *домашен*, *работен*), the nouns made from adjectives and verbs that the dictionary
  has as words (*важност*, *убавина*, *барање*, *мислење*, *знаење*), and the participle
  adjectives with a meaning of their own (*отворен*, *затворен*, *познат*, *минат*, *нареден*).
  Teach each as itself; never count one for its base word.
- **Homographs**: a form shared by two words counts for both, and the checker counts it at the
  better rank. *прави* is "makes" and "straight" (plural of *прав*); *води* "leads" and "waters"
  (*вода*); *мисли* "thinks" and "thoughts"; *свет* "world" and "holy"; *син* "son" and "blue";
  *лек* "medicine" and "light"; *бара* "looks for" and "a puddle"; *бил*, *беше* are *сум*'s. Use a
  new word in its own sense, never through its homograph, and do not lean on a homograph's rank to
  pass the coverage.
- **Everyday words rank later than you would think** (the list is built from news and web text):
  *дете* 87, *мајка* 206, *пријател* 222, *дома* 254, *куќа* 264, *брат* 274, *училиште* 302,
  *татко* 319, *јаде* 469, *вчера* 497, *вечер* 498, *куче* 539, *чита* 541, *ќерка* 571, *соба* 578,
  *маса* 598, *утро* 614, *мама* 644, *сестра* 755, *кафе* 841, *пие* 921, *продавница* 1095, *баба*
  1140, *мачка* 1217, *дедо* 1432, *тато* 1478, *чај* 1611, *леб* 1834. Check a word's rank with
  `rank.py` rather than guess it.
- The checker compares whole words, lowercased, split at hyphens. The `Прашања:` header is not
  counted, nor is a Roman numeral (*XIX век*).
- A **name** goes in `names` once, as the text writes it, and so does each vocative the text uses
  (*Петар* and *Петре*, *Сара* and *Саро*, *Стефан* and *Стефане*): names are not declined
  otherwise. A name of several words (*Крива Паланка*, *Охридско Езеро*, *Кисела Вода*) counts as
  one known word wherever it is written whole; list it whole. Never use a name that is a list
  word or a list word's form: *Ана* (filed under *ан*), *Марко* (*марка*), *Ивана* (*ива*), *Елена*
  (*елен*); check every name with `rank.py` (a name the list does not know prints `-`). Places that
  are list words (check them) may stand in `names` as places, and are skipped as band words.
- The list's form lists are noisy (spoken and dialect spellings such as *ке*, *че*, *шо*, *сеа*,
  *тува*; Serbian forms; unrelated words): a new word must appear as itself or a true form, never
  through such a form. Some entries are not words to teach: abbreviations, interjections, vulgar
  words, names and place adjectives: skip them. Loanwords that Macedonian uses as its own
  (*компјутер*, *телефон*, *фудбал*, *кафе*, *филм*, *интернет*) are taught like any word.
