# Writing a Bulgarian frequency course

Each course teaches one band of 100 words from the Bulgarian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `bg-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ния иска котка. Тя отива в приюта. Там има три котки...* Everyday life in
  Bulgaria: Sofia and its quarters, Plovdiv, Varna, Burgas, Ruse, Stara Zagora, Veliko Tarnovo,
  Pleven, Gabrovo, Blagoevgrad, Sozopol and Nesebar on the Black Sea, the Rila, Pirin, Rhodope and
  Balkan mountains, Vitosha above Sofia, the Danube, villages and a grandmother's house with a
  garden; and now and then Bulgarian families in London, Berlin, Madrid, Vienna or Chicago, where
  Bulgarian is spoken at home: home, family, food, shopping, work, school, the city, health, free
  time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *той*/*тя* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Ivans in
  two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object), the short pronouns in their place (*Виждам го.* *Той ме вижда.* *Дай
  ми книгата.* *Не го виждам.*: never first in the sentence). Present tense up to rank 500, with
  *ще* + present for the future (*Утре ще ходя на море.*) and *искам/мога/трябва да* + present where
  natural; from rank 600 a story may be told in the past (*Ния излезе от къщи. Валеше дъжд.*).
  Little dialogue; when there is some, short and simple, with *ти* among family and friends and
  *Вие* with strangers, older people and in shops and offices (*господин Петров*, *госпожо
  Иванова*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*защото*, *понеже*, *когато*, *докато*, *въпреки
  че*, *затова*, *обаче*, *но*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: *съм* in the present (*аз съм*, *ти си*, *той е*, *ние сме*, *вие сте*, *те
    са*) and *беше*/*бях* for "was"; *има* and *няма* ("there is", "there isn't", "has"); the
    present tense; *да* + present after *искам*, *мога*, *трябва*, *обичам* (Bulgarian has no
    infinitive: *Искам да ям.*); *ще* + present for the future and *няма да* for its negative; the
    definite article (*къщата*, *детето*, *хората*, *градът* / *града*), on the first word of the
    noun phrase (*голямата къща*, *моята стая*); the short pronouns *ме*, *те*, *го*, *я*, *ни*,
    *ви*, *ги* (whom) and *ми*, *ти*, *му*, *ѝ*, *ни*, *ви*, *им* (to whom, and "my", "your",
    "his"... after a noun: *майка ми*, *къщата ни*); *се* and *си* with their verbs (*казвам се*,
    *връщам се*); *не* and its place before the verb and the short pronouns (*Не го знам.*);
    yes/no questions with *ли* after the word asked about (*Искаш ли кафе?* *Ти ли си?*); *кой*,
    *какво*, *къде*, *кога*, *защо*, *как*, *колко*; the past (aorist) of the commonest verbs only
    as set phrases (*каза*, *видях*, *направи*); numbers;
  - ranks 301–600: the aorist for past events (*отидох*, *видях*, *купи*), the imperfect for what
    was going on or usual (*беше*, *имаше*, *валеше*), aspect pairs side by side (*купувам* /
    *купя*: the perfective after *да* and *ще* for one finished act, *Искам да купя хляб.*), the
    imperative (*кажи*, *ела*, *дай*, *седни*, *елате*), *ми харесва*, *трябва ми*, the vocative
    of names and family words (*Иване!*, *мамо!*, *бабо!*, *Марийо!*), comparatives with *по-* and
    *най-* (*по-голям*, *най-хубав*), *защото*, *ако*, *за да*;
  - ranks 601–1000: the perfect (*бил съм*, *ходила съм*, *ял ли си?*), the future in the past
    (*щях да*, *щеше да*), the relative *който*, *която*, *което*, *които*, reported speech with
    *че* (*Каза, че ще дойде.*), *докато*, *преди да*, *след като*, *когато* + future meaning
    (*Когато дойдеш, ще ядем.*), the doubled pronoun (*Ивана я няма.* *На мен ми харесва.*);
  - ranks 1001–1300: the conditional (*бих искал*, *би било хубаво*, *ако имах време, щях да...*),
    the past perfect (*бях отишъл*), participles as adjectives (*отворен*, *написан*,
    *счупен*), every number with its noun (*двама души*, *три часа*, *пет дни*), the count form of
    masculine nouns (*два стола*);
  - ranks 1301–1600: the indefinite pronouns (*някой*, *нещо*, *никой*, *нищо*, *всеки*, *който и
    да е*, *каквото и да е*), verbal nouns in *-не* (*четене*, *пазаруване*), the passive with
    *се* (*Къщата се строи.*), the active participles (*пишещ*, *дошъл*), *въпреки че*, *макар
    че*, *вместо да*, *освен*;
  - ranks 1601–2000: the renarrated mood (*преизказно наклонение*) in retold stories and
    hearsay (*Казват, че там имало езеро.* *Той бил в Париж.*), the adverbial participle in
    *-йки* (*мислейки*, *вървейки*), the past passive participle in phrases (*построената къща*),
    *не само... но и*, *колкото... толкова*;
  - ranks 2001–2500: participle phrases, the conditional of the past (*ако бях знаел, щях да
    дойда*), the dubitative and the renarrated mood in reported speech, *поради*, *благодарение
    на*, *съгласно*, *вследствие на*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *впрочем*, *все пак*,
    *междувременно*, *с оглед на*, *във връзка с*, *относно*; long participle phrases; common
    idioms and proverbs explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper *рубрика* (a light reflection on everyday life), dialogue that
    marks formal and informal speech (*Вие* against *ти*), less common connectors (*макар и*,
    *въпреки това*, *дори и да*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review; precise
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *рубрика*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Тук ли е? Не, не е тук. Там ли е? Не, не е там.*).
- **Questions at the end**: after the story, a paragraph starting `Въпроси:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *защо* and *как*, with fuller answers):

  ```
  Въпроси:
  Какво иска Ния?
  Ния иска котка.
  Къде отива Ния?
  Ния отива в приюта.
  ```

- **Standard Bulgarian**: neutral, correct and natural Bulgarian as it is written today, in the
  current spelling (the 2012 *Официален правописен речник*), with all its agreements; no slang,
  no obscenities, no dialect in the narration (*к'во*, *тука*, *нема*, *ше*, *шъ*, *дет*, *оти*
  are spoken or regional spellings: never written). The stories are for readers of every region:
  keep to the standard language and let a regional or colloquial word stand only where its
  context makes it clear. Points a writer gets wrong:
  - **the full and the short article** on masculine nouns and adjectives: *-ът*/*-ят* on the
    subject (*Градът е голям.* *Новият учител дойде.*), *-а*/*-я* elsewhere (*Видях града.* *в
    града*, *с новия учител*), and *-та* on the plural (*градовете*);
  - **ѝ** ("to her", "her") is written with its grave accent, which tells it from *и* ("and"):
    *Дай ѝ книгата.* *майка ѝ*. Write the one character ѝ (U+045D), never *й* for it;
  - **в** and **с** become **във** and **със** before a word that begins with в/ф and с/з (*във
    Варна*, *във филма*, *със сестра си*, *със захар*), and stay *в*, *с* otherwise;
  - **по-** and **най-** are joined with a hyphen (*по-голям*, *най-хубаво*); *не* is written apart
    from verbs (*не знам*) and joined in words that need it (*неделя*, *невъзможно*);
  - the short pronouns never begin a sentence or a clause (*Видях го.*, not *Го видях.*; *Казах
    му.*), and come before the verb after *не*, *да*, *ще* (*Не го знам.* *Искам да ти кажа.* *Ще
    ви видя.*);
  - **Вие** with a capital in polite address to one person (*Как сте Вие?* in a letter); in
    dialogue the polite *вие* follows the same rule (*Много сте добра!* to one woman);
  - never put stress marks (acute accents) in the text, except ѝ: they break words apart for the
    reader.
- **Bulgarian typography**: direct speech is set out as Bulgarian books set it, each speaker's line
  a paragraph of its own beginning with an em dash and a space:

  ```
  — Здравей, Ния! — казва Петър.
  — Здравей! — отговаря Ния.
  ```

  The questions and answers of the final `Въпроси:` block are plain lines, as above. Bulgarian
  quotation marks are „…“ (low, then high): *Тя мисли: „Ще дойда.“* A dash inside a sentence is the
  en dash with spaces ( – ); hyphens inside words (*по-добре*, *най-много*, *кой-където*) stay
  hyphens. Write numbers as digits (*7 часа*, *в 7:30*, *3 деца*, *2,5 килограма*, *10 000 лева*),
  except a number word that is in your band: spell that one out to teach it (*седем дни*).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age, every country and every faith. Keep away from:

- **religion** in every form, of every faith: God and gods, the church and its people (*църква*,
  *поп*, *свещеник*, *манастир*, *Рилският манастир* as a monastery), prayer, fasting, saints and
  their days; the holidays and customs of religious origin, even as a date, a meal, a trip home or
  a gift season: Christmas (*Коледа*, *Бъдни вечер*, *коледари*), Easter (*Великден*, painted eggs,
  *козунак* as Easter bread), *имен ден* (name days are saints' days), *Ивановден*, *Гергьовден*,
  *Еньовден*, *Никулден*, *Тодоровден*, *Лазаровден*, *Сирни заговезни*, *Кукеровден* as a church
  date; religious words and formulas (*боже*, *господи*, *дай боже*, *слава богу*, *дявол*,
  *ад*, *рай*). Birthdays, New Year's Eve and New Year, weddings without their rites, *Баба Марта*
  and the *мартеници* of 1 March, the *кукери* as a folk masquerade (without a saint's day), harvest
  feasts, village fairs (*събор*) without a church, folk music and *хоро* are fine;
- **politics**, the state, elections, parties, ministers, presidents, mayors, kings, tsars and
  queens (fairy tales and legends included), borders, national days and nationalism (3 March, 6
  September, 24 May as a state day), the Ottoman and communist past and every country's disputes
  and conflicts; everyday civic life (a library, a school board, a meeting of the residents of a
  block, the *община* office's opening hours, a new identity card) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  parking attendant is fine; thefts, arrests, prisons, lawyers and trials are not);
- **death**, killing, funerals, mourning, blood and injury beyond a scraped knee; natural
  disasters only as preparation and calm, never with victims;
- **vulgar or sexual words**, insults, and words whose main sense is sexual;
- **alcohol** (*вино*, *бира*, *ракия*, *наздраве*, bars, *Трифон Зарезан*), **gambling**
  (betting, lotteries, *тото*, casinos), **drugs and smoking** (*цигара*, *пуша*);
- **nationalities and place adjectives** (*български*, *българин*, *руски*, *английски*,
  *софийски*, *пловдивски*) are not taught as words, nor are language names (*говоря английски* is
  fine in a text, but *английски* is never a new word);
- **person names taught as words**: a band word that is a name, a surname, a title, a place
  name, a brand or a word made from a place or a people is skipped.

Skip band words that only fit these topics, and say so in your report. Those of the first 1000
ranks:

- religion: *бог* 157, *дявол* 270, *душа* 282 (soul), *господ* 376, *дух* 761, *ад* 881,
  *църква* 617, *празник* 929 only for secular holidays (*рожден ден* is not one; *Нова година* is);
- death, violence, crime: *убия* 162, *умра* 294, *смърт* 313, *мъртъв* 391, *убийство* 484,
  *затвор* 491, *кръв* 503, *бой* 512, *убиец* 673, *полицай* 689, *умирам* 695, *убивам* 747,
  *доказателство* 759, *битка* 765, *оръжие* 435, *арестувам* 936, *войник* 971, *полиция* 385,
  *съд* 312, *дело* 507, *жертва* 576, *бия* 624, *армия* 706, *война* 362, *победа* 599,
  *военен* 552, *адвокат* 742, *агент* 748, *закон* 549, *заповед* 719; *ранен* 850 only as
  "early" (*ранна сутрин*), never "wounded"; *гол* 867 only as a goal in sport;
- politics and the state: *политика* 323, *национален* 338, *държава* 350 only as "country" in
  travel and geography, *президент* 458, *министър* 493, *правителство* 498, *власт* 525, *крал*
  521, *цар* 992, *принц* 921, *гражданин* 562, *държавен* 571, *мир* 574, *международен* 578,
  *граница* 661, *партия* 728, *съюз* 864, *територия* 878, *кмет* 937, *народ* 410, *избор* 371
  only as "choice", never elections;
- vulgar, sexual, alcohol: *секс* 475, *задник* 777, *вино* 505, *бира* 923; *пия* 339 only with
  water, coffee, tea, juice or milk;
- nationalities and place adjectives: *български* 142, *руски* 431, *европейски* 443,
  *американски* 591, *английски* 708, *френски* 958, *българин* 555;
- forms, variants and noise: *със* 89 and *във* 91 (forms of *с* and *в*: teach *с* and *в*);
  *сър* 330, *ей* 334, *у* 269, *ало* 976 (subtitle words: skip); *ща* 243 only as the past of the
  future (*щях да*, *щеше да*), never "to want"; *трябвам* 914 (the list's own *трябва* 30 is the
  word); *франк*, *том*, *ес* (names and abbreviations).

The ranks above are those of the pinned list; check a word with `rank.py` before you rely on it.

## The file

Write `tools/courses/bg/<id>.json`:

```json
{
  "id": "bg-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Bulgarian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places and brands used in the texts, and the vocatives used"],
  "lessons": [
    {
      "title": "Story title in Bulgarian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/bg/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *видях* for *видя*, *къщата* for *къща*, *новият* for
  *нов*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  the first person singular present of a verb (*искам*, *видя*, *правя*), the masculine singular
  of an adjective (*нов*, *голям*), the bare singular of a noun (*къща*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, a letter, an abbreviation, a list error such as a
  name, or a form of a word already taught); the checker's last line lists the band words not
  taught yet.

Things to know about the Bulgarian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/bg/wordlist.tsv`
  (a copy of the app's `bg.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it, and `extra_forms.tsv` adds
  the forms the dictionary gives that the list's 40 forms per word leave out.
- **Verbs are listed in the first person singular present**, as Bulgarian dictionaries cite them,
  and every form of the verb counts for it: *видя* holds *видях*, *видя*, *виж*, *видял*.
- **Aspect pairs are two words.** The perfective and the imperfective of a verb are entries of
  their own (*кажа* 38 and *казвам* 103, *видя* 52 and *виждам* 128, *дам* and *давам*, *купя* and
  *купувам*, *отида* and *отивам*, *дойда* and *идвам*). Each is a word to teach in its own band:
  a learner who knows *купя* still has to learn *купувам*. Teach each in its own sense (the
  perfective for one finished act: *Искам да купя хляб.*; the imperfective for what goes on or
  happens often: *Всеки ден купувам хляб.*), and never use one to count for the other.
- **Reflexive verbs** are listed without their pronoun: *нуждая* is *нуждая се* ("to need"),
  *влюбя* is *влюбя се*, *радвам* is *радвам се*. Teach them with their *се* or *си*; *се* and
  *си* are words of their own (6 and 10).
- **The short pronouns are words of their own**: *ми* 18, *го* 21, *му* 43, *ме* 44, *ви* 45,
  *ни* 50, *я* 56, *ги* 75, *им* 114, *ѝ* 124 (with *й* as its older spelling). The long forms
  belong to their pronoun (*мен*, *мене* to *аз*; *него* to *той*; *нея* to *тя*; *нас* to *ние*;
  *вас* to *вие*; *тях* to *те*; *теб* to *ти*). *те* is both "they" and "you" (whom), *ти* both
  "you" and "to you"/"your", *си* "you are", "to oneself" and "one's own", *е* is "is" (a form of
  *съм*): each counts for its one entry.
- **The article is part of the noun**: *къщата*, *детето*, *хората*, *градът*, *града* are forms
  of *къща*, *дете*, *хора*, *град*. *хора* 92 ("people") is a word of its own next to *човек* 70.
- **Homographs**: a form shared by two words counts for both, and the checker counts it at the
  better rank. *прави* is "makes" (*правя*) and "straight, right" (*прав*); *мина* "passed" and "a
  mine"; *син* "son" and "blue"; *лек* "light" and "a cure"; *пита* "asks" and "a round loaf"; *мил*
  "dear" and *мия* "to wash"; *бил* "was" (*съм*) and *бия* "to beat"; *бях*, *беше* are forms of
  both *съм* and *бъда*. Use a new word in its own sense, never through its homograph, and do not
  lean on a homograph's rank to pass the coverage.
- **Everyday words rank later than you would think** (the list is built from film subtitles and
  web text): *казвам* 103 (so *казва* costs a word in the first course; *каза* "said" is *кажа*
  38), *къде* 106, *нищо* 107, *дете* 101, *колко* 127, *баща* 173, *майка* 152, *брат* 234, *мама*
  280, *татко* 272, *баба* 871, *дядо* 830, *къща* 222, *стая* 336, *вода* 187, *ям* 390, *пия*
  336, *куче* 375, *котка* 1271, *кафе* 639, *чай* 1155, *хляб* 1715. Check a word's rank with
  `rank.py` rather than guess it.
- The checker compares whole words, lowercased, split at hyphens (*по-добре* is *по* + *добре*,
  *най-новият* is *най* + *новият*). The `Въпроси:` header is not counted, nor is a Roman numeral
  (*XIX век*).
- A **name** goes in `names` once, as the text writes it, and so does each vocative the text uses
  (*Иван* and *Иване*, *Петър* and *Петре*, *Мария* and *Марийо*, *Тодор* and *Тодоре*): names
  are not declined otherwise. A name of several words (*Стара Загора*, *Велико Търново*, *Черно
  море*) counts as one known word wherever it is written whole; list it whole. Never use a name
  that is a list word: *Надежда* (*надежда* 1052), *Ели* (*ели*, of *ела* 394), *Светла*, *Слави*,
  *Росица*, *Виктор*, *Христо*, *Николай*, *Владимир*, *Емил*, *Красимир*, *Калин*, *Явор*, *Росен*,
  *Пешо*, *Мила* (*мил*), *Мара* (*мара*), *Елена* (*елен*), *Вера* (*вяра*), *Радост*, *Камен*, *Зорница*, *Невена*. Safe names:
  *Иван*, *Петър*, *Георги*, *Мария*, *Стефан*, *Димитър*, *Тодор*, *Васил*, *Борис*, *Мартин*,
  *Никола*, *Пламен*, *Ивайло*, *Деян*, *Цветан*, *Огнян*, *Божидар*, *Ана*, *Ивана*, *Десислава*,
  *Гергана*, *Цветелина*, *Надя*, *Лиляна*, *Лили*, *Стела*, *Яна*, *Теодора*, *Йоана*, *Милена*,
  *Даниела*, *Катя*, *Ния*, *Ралица*, *Силвия*, *Ваня*, *Мими*. Places that are list words or
  their forms (*Варна*, a form of *варя* 945; *Банско* 895; *Шумен*; *Плевен*, of *плевя*; *Рила*, of *рия*; *Стара* and *Велико* in *Стара Загора*, *Велико
  Търново*) may stand in `names` as places, and are skipped as band words.
- The list's form lists are noisy (spoken and dialect spellings such as *к'во*, *тука*, *нема*,
  *ше*; old forms; unrelated words): a new word must appear as itself or a true form, never through
  such a form. Some entries are not words to teach: abbreviations, interjections from film
  subtitles (*сър*, *ей*, *ало*), vulgar words, names and place adjectives: skip them. Loanwords
  that Bulgarian uses as its own (*компютър*, *телефон*, *футбол*, *кафе*, *филм*) are taught like
  any word.
