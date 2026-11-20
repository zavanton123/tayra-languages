# Writing a Belarusian frequency course

Each course teaches one band of 100 words from the Belarusian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `be-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 10,000 words, so the courses run up to
`be-mini-10000` (ranks 9901–10000).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Алеся шукае кнігу. Яна ідзе ў пакой. Там кнігі няма...* Everyday life
  where Belarusian is spoken: Minsk, Hrodna, Brest, Viciebsk, Mahilioŭ, Homiel, Polack, Navahrudak,
  Nias'viž, Pinsk, Lida, Slonim, Baranavičy, Maladzečna, Babrujsk, Orša, Mazyr, Turaŭ; Lake
  Narač and the Braslaŭ lakes, the Biełaviežskaja pušča, the Prypiać and the Paliessie marshes, the
  Nioman, the Dzvina and the Dniapro; villages, a grandmother's house with a garden, a *лецішча*
  (dacha); and now and then Belarusian families in Vilnius, Warsaw, Białystok, Prague, Riga or
  London, where Belarusian is spoken at home: home, family, food, shopping, work, school, the town,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ён*/*яна* is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; adjectives before their noun). Present tense up to rank 500, with the
  future (*буду рабіць*) and *хачу/магу/трэба/можна* + infinitive where natural; from rank 600 the
  story may be told in the past (*Алеся выйшла з дому. Ішоў дождж.*). Little dialogue; when there
  is some, short and simple, with *ты* among family and friends and *вы* with strangers, older
  people and in formal settings (*спадар Іван*, *спадарыня Ганна*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*таму што*, *бо*, *калі*, *пакуль*, *хоць*,
  *таму*, *аднак*, *але ж*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*быў*, *была*, *пайшоў*),
    the nominative, accusative, locative and genitive in plain phrases (*у мяне ёсць...*, *у
    горадзе*, *на стале*, *няма часу*), the vocative of a few names and family words (*Іване!*,
    *Міхасю!*, *сынку!*; most names, *мама* and *тата* keep the nominative), possessives (*мой*,
    *твой*, *свой*, *яго*, *яе*), simple questions (*хто*, *што*, *дзе*, *куды*, *калі*), negation,
    *хачу/магу/трэба/можна* + infinitive;
  - ranks 301–600: the past tense for events, the future (*буду чытаць* and the perfective
    *прачытаю*), the dative (*мне падабаецца*, *даць брату*), the instrumental (*з братам*,
    *працаваць доктарам*), the plural, the imperative (*скажы*, *ідзіце*, *хадзем*, *давай(це)* +
    infinitive or the 1st person plural), *таму што*, *бо*, *калі* ("if"), *каб* + infinitive,
    simple comparatives (*больш*, *лепш*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*рабіў* / *зрабіў*, the
    perfective future *зраблю*), the verbs of motion (*ісці* / *хадзіць*, *ехаць* / *ездзіць*, and
    with *пры-*, *вы-*, *у-*, *за-*, *пера-*, *да-*), reflexive verbs in *-цца*/*-ся* (*вучыцца*,
    *вучыўся*), *які* in the nominative and accusative, *чым* / *за* in comparisons (*старэйшы за
    брата*), reported speech with *што* and *як*, *пакуль*, *калі*;
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*два
    сталы*, *пяць сталоў*), comparatives and superlatives (*найбольшы*, *самы лепшы*, *лепшы*),
    *які* in every case, the conditional with *бы* (*калі б..., я б...*), *каб* + past (*хачу,
    каб ты прыйшоў*), *пакуль не*, *як толькі*;
  - ranks 1301–1600: the indefinite pronouns (*хтосьці*, *штосьці*, *нешта*, *хто-небудзь*), verbs
    that take a case (*цікавіцца чым*, *займацца чым*, *баяцца чаго*, *сумаваць па кім*), short
    adjectives as predicates (*рады*, *павінен*, *вінен*), *хоць*, *нягледзячы на*, *паколькі*,
    *замест*, *акрамя*, the first adverbial participles (*чытаючы*, *прачытаўшы*);
  - ranks 1601–2000: adverbial participles in everyday use (*седзячы*, *ідучы*, *вярнуўшыся*),
    passive participles (*напісаная кніга*, *пабудаваны дом*), the impersonal forms in *-на/-та*
    (*зроблена*, *напісана*, *адчынена*), indirect questions with *ці*, *нібы* / *быццам*, *не
    толькі... але і*, *той, хто*, *адзін з*, *кожны з*;
  - ranks 2001–2500: participle phrases, the conditional of the past (*калі б ён ведаў, ён бы
    прыйшоў*), *чым... тым*, verbal nouns (*адкрыццё*, *будаўніцтва*), *у той час як*,
    *дзякуючы*, *згодна з*, *у выніку*; reported speech with the tense of the original kept;
  - ranks 2501–3000: a richer, more formal register: connectors such as *між тым*, *аднак*, *з
    улікам*, *у сувязі з*, *што тычыцца*; long participle phrases; common idioms explained by their
    context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper *калонка* and the *нарыс* (a light reflection on everyday life),
    dialogue that marks formal and informal speech, less common connectors (*абы толькі*,
    *насуперак*, *хай нават*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose, academic and
    journalistic registers, learned words explained by their context. Still standalone, calm and
    easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a *калонка*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ён шукае на кухні. Ён шукае ў пакоі. Ён шукае ў калідоры.*).
- **Questions at the end**: after the story, a paragraph starting `Пытанні:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *чаму* and *як*, with fuller answers):

  ```
  Пытанні:
  Што шукае Алеся?
  Алеся шукае кнігу.
  Дзе кніга?
  Кніга на стале.
  ```

- **Standard Belarusian (наркамаўка)**: neutral, correct and natural Belarusian as it is written
  today in the official spelling (the 2008 rules), in Cyrillic, with all its agreements and cases;
  no slang, no obscenities. Write what the spelling shows of the pronunciation: *аканне*
  (unstressed *о* is written *а*: *вада* but *воды*, *малако*, *горад* but *гарады*), *яканне*
  (*е* before the stress is written *я*: *сястра* but *сёстры*, *бяроза*, *дзесяць*), *дз* and *ц*
  for soft *д* and *т* (*дзень*, *дзеці*, *цёця*, *цяпер*), *ё* always written where it belongs
  (*ёсць*, *мёд*, *сёння*), *шч* (*шчаслівы*), the apostrophe after a consonant before *я, е, ё, ю,
  і* (*сям'я*, *з'ява*, *аб'ява*, *п'еса*, *вераб'і*), and *ў* (see below). **Not taraškievica**
  (the classical spelling of some émigré and independent media): no soft sign between consonants
  (*снег*, *свята*, *з'ява*, not *сьнег*, *сьвята*, *зьява*), no *Менск* (write *Мінск*), no *ль*
  in loanwords (*план*, *клас*, *Лондан*, not *плян*, *кляс*, *Лёндан*), no *э* for *е* in loanwords
  where the official spelling has *е* (*парламент*, *газета*, not *парлямэнт*, *газэта*: follow the
  dictionary), and *расійскі* (not *расейскі*). **No Russianisms or trasianka**: *дзякуй* (not *спасіба*), *добры*
  (not *харошы*), *цягнік* (not *поезд*), *крэсла* (not *стул*), *шклянка* (not *стакан*), *страва*
  (not *блюда*), *крок* (not *шаг*), *аловак* (not *карандаш*), *мяжа* (not *граніца*), *хуткасць*
  (not *скорасць*), *вынік* (not *рэзультат*), *надвор'е* for the weather (*пагода* is "fine
  weather"), *цяпер* / *зараз* (not *сейчас*), *таксама* (not *тожа*), *нават* (not *даже*), *чаму*
  (not *пачаму*), *калі ласка*, *вельмі*, *трэба*, *браць удзел*. Watch Belarusian genders that
  differ from Russian: *сабака*, *боль*, *медаль*, *цень*, *насып* are masculine (*вялікі сабака*,
  *моцны боль*). After *два, тры, чатыры* (and *абодва*, *абедзве*) the noun is in the nominative
  plural (*два сталы*, *тры кнігі*, *чатыры гады*), from *пяць* in the genitive plural (*пяць
  гадоў*). Time of day with *а*: *а сёмай гадзіне*, *а палове восьмай*; the date with the
  genitive: *трэцяга траўня*. Never put stress marks (acute accents) in the text: they break words
  apart for the reader.
- **ў after a vowel**: an unstressed *у* at the start of a word becomes *ў* after a word that ends in
  a vowel, also across a comma: *яна ў школе*, *мама ўбачыла*, *на ўроку*, *і ўсё*, *Алеся, ўбачыўшы
  маму,...*; after a consonant, at the start of a sentence and after a full stop it stays *у*:
  *брат у школе*, *У нас ёсць кот.* The preposition and prefix after a vowel too: *пайшла ў лес*,
  *было ўсё*. The conjunction *і* stays *і* after a vowel too (*мама і тата*); never write *й* for it.
  Words that begin with *у* in a name or a loanword keep it (*у Уладзіміра*, *па Уругваі*).
- **Belarusian typography**: direct speech is set out as Belarusian writes it, each speaker's line a
  paragraph of its own beginning with an em dash and a space:

  ```
  — Добры дзень, Алеся! — кажа Пятрусь.
  — Добры дзень! — адказвае Алеся.
  ```

  The questions and answers of the final `Пытанні:` block are plain lines, as above. « »
  (guillemets) quote a title or a word (*у часопісе «Маладосць»*). The apostrophe is written
  straight, as the list writes it (*сям'я*, *з'явіцца*, *аб'ява*). Hyphens inside words
  (*хто-небудзь*, *па-беларуску*) stay hyphens; the em dash is the long one (—). Write numbers as
  digits (*7 гадзін*, *а 7:30*, *3 браты*), except a number word that is in your band: spell that
  one out to teach it (*сем дзён*).
- **Content** (user decision 2026-11-13: no topic rules). Any normal everyday, cultural or story
  topic is fine: religion and holidays (*Каляды*, *Вялікдзень*, *Купалле*, *Дзяды*), history, kings,
  princes and the Grand Duchy, myths and folk tales (*цмок*, *папараць-кветка*), crime and detective
  stories, courts, politics, war in history, alcohol, smoking, love and kisses, death, sport of every
  kind. Only avoid explicit sexual content, slurs used as insults, and gratuitous gore. Teach ordinary
  words in their main sense. Days of the week, months, seasons and language names are everyday
  words, not names: teach them. The word rules below still apply in full: no duplicates, no forms of
  taught words, no word counted through another sense, name or idiom, no noise.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/be/<id>.json`:

```json
{
  "id": "be-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Belarusian, e.g. Ежа і пакупкі",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Belarusian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after that `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/be/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ішла* for *ісці*). Reuse earlier lessons' new words too.
  `newWords` holds the word as the list writes it: the infinitive of a verb, the nominative
  singular of a noun, the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name, a Russian
  word, a taraškievica spelling or a form filed as a word of its own); the checker's last line lists
  the band words not taught yet.

Things to know about the Belarusian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/be/wordlist.tsv`
  (a copy of the app's `be.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. Courses up to `be-mini-1600`
  were written against the first list (3,840 words, built from the English Wiktionary's words alone);
  the rebuilt list (2026-11-20) adds the Russian and Polish Wiktionaries' Belarusian words and the
  words found in the corpus itself (*музей*, *фестываль*, *сцэна*, *квіток*, *транспарт*), and is
  pinned with ranks 1–1600 as they were: rank 1601 on is the rebuilt list's order, so band 1700
  holds its commonest new words (*жа*, *тэрыторыя*, *многі*, *арганізацыя*, *музей*).
- The list was built from news, web and Wikipedia text (Leipzig corpora), so politics, history and
  the state rank high and everyday words rank late: *казаць* is 74 and *жыць* 115 (so *кажа* and
  *жыве* are fine in the first course), but *мама* is 963, *хлеб* 1100, *бабуля* 1185, *кот* 1416,
  *тата* 1508, *кава* 3420. Check a word's rank rather than guess it. Everyday words rare in the
  news (*туалет*, *агурок*, *варона*) are past rank 10,000 and not in the list.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation and the participles of verbs, usually the other aspect (*бачыць* holds
  *убачыць*, *чытаць* holds *прачытаць*, *ісці* holds *ішоў*; *зрабіць* and *рабіць* are entries of
  their own), derived adjectives (*спартыўны* under *спорт*, *сонечны* under *сонца*, *цудоўны*
  under *цуд*, *святы* "holy" under *свята*), feminines (*студэнтка* under *студэнт*) and
  comparatives (*лепш* under *добра*, *большы* under *вялікі*). So a word counts at the rank of its
  headword, whatever form the story uses. Some common forms are missing: the dictionary's forms of
  the list's words are added from `extra_forms.tsv`, and a true form that is still missing counts as
  an outside word. Then write around it and say so in your report.
- **Misfiled forms and homographs**: a form shared by two words counts for both, and a form listed
  under a word may be another word altogether. *Левы* (left) is filed under *леў* (lion) 646;
  *мала* (little) and *май* under *мець* 40; *ручка* (pen) under *рука*; *сталы* (mature) under
  *стол*; *лес* (forest) also under *лёс* (fate); *шкода* under *шкада*; *порах* under *пара*; *душ*
  under *душа*; *марак* (sailor) under *марка*; *паходзіць* under *хадзіць*; *восі* (axles) under
  *вось*; *ракі* is both *рака* and *рак*; *меншы* under both *малы* and *маленькі*. Use a new word in
  its own sense, never through its homograph or a misfiled form, and do not lean on a homograph's
  rank to pass the coverage.
- **ў and у**: the checker reads *ў* at the start of a word as *у* (*ўжо* is *ужо*, *ўбачыла* is
  *убачыла*), so it cannot tell you when you got the rule wrong: check every *у*/*ў* yourself (the writers'
  `uw.py` flags the doubtful ones).
  Spelling variants of one word are entries of their own here and there (*лідар* 515 and *лідэр*
  771; *хаця* 661 and *хоць* 222; *заўсёды* 306 and *заўжды*
  1313; *і* 2 and *й* 664): never teach the second one as a new word (it is the same word as the one
  taught before): skip it and say so.
- The checker compares whole words, lowercased, the apostrophe folded (*сям'я*, *сям’я* and *сямʼя*
  are one word), split at hyphens (*хто-небудзь* is *хто* + *небудзь*, *па-беларуску* is *па* +
  *беларуску*): an entry written with a hyphen (*па-беларуску* 683, *з-за* 253, *які-небудзь* 1062,
  *па-мойму* 5021) can never be counted as used, so skip it as a new word, and *небудзь*, not in the
  list, costs one outside word. The ending of an ordinal after its digits (*5-га*, *2-і*) is not
  counted, nor is a Roman numeral (*XX стагоддзе*). The `Пытанні:` header is not counted.
- A **name** goes in `names` once, in the nominative (*Алеся*, *Пятрусь*, *Андрэй*, *Марыя*,
  *Мінск*, *Гродна*): the checker also accepts its case forms and its vocative from the regular
  declension (*Алесі*, *Алесю*, *Алесяй*; *Ганна*, *Ганны*, *Ганне*, *Ганну*, *Ганнай*; *Вольга*,
  *Вользе*; *Наташа*, *Наташы*, *Наташай*; *Іван*, *Івана*, *Івану*, *Іванам*, *Іване*; *Андрэй*,
  *Андрэя*, *Андрэем*, *Андрэю*; *Міхась*, *Міхася*, *Міхасём*, *Міхасю*; *Мікола*, *Міколу*; *Гродна*,
  *Гродне*, *Гроднам*; *Брэст*, *у Брэсце*; *Мінск*, *у Мінску*; *Магілёў*, *Магілёва*, *у Магілёве*;
  *Гомель*, *Гомеля*, *у Гомелі*), however short the name. A name whose stem changes otherwise
  (*Пётр* → *Пятра*, *Навагрудак* → *Навагрудка*, *Леў* → *Льва*) needs each such form listed. A
  name of several words or with a hyphen (*Белавежская пушча*, *Мар'іна Горка*) is taken part by
  part, each part declined. Possessive adjectives of names (*Алесін*, *Андрэеў*) are not generated:
  write *у Алесі*, *брат Андрэя*. Keep only true names there, never common words, and never a name
  that is also a list word (*Надзея*, *Вера*, *Люба* (*любы*), *Воля*, *Галіна*, *Мірон*, *Зорка*,
  *Мароз*, *Бяроза*, *Колас*, *Купала*): then call the character something else. The list leaves
  out *Беларусь*, *Мінск*, *Гродна*, *Брэст* and the other towns (they are names), but has the
  adjectives of places (*беларускі* 28, *мінскі* 411, *віцебскі* 719, *гомельскі* 861), which are
  list words, not names. Day and month names are in the list (*панядзелак* 1282, *травень* 894).
- The list's form lists are noisy (spellings without *ё* such as *самалет*, *знаемы*; taraškievica
  spellings such as *сыстэма*, *зьмена*, *госьць*, which the rebuilt list files under their official
  words: *пасьля* under *пасля*, *музэй* under *музей*, *прэзыдэнцкі* under *прэзідэнцкі*;
  unrelated words): a new word must appear as itself
  or a true inflection, never through such a form, and a non-standard form the checker accepts is
  still wrong in the text. Some entries are not words to teach: Russian words and Russianisms
  (*харошы*, *скорасць*, *рэзультат*, *пуць*, *ігра*, *рысунак*, *поезд*, *стул*, *стакан*, *шаг*,
  *блюда*, *мелкі*, *крэпкі*, *карандаш*, *водка*, *рабёнак*, *рабяты*, *заложнік*, *граніца*),
  taraškievica and dialect spellings (*расейскі*, *расеец*, *расьліна*, *вывезьці*, *мэнскі*,
  *мажліва*, *лідэр*), colloquial and dialect words (*трэ*, *штось*, *хтось*, *гэны*, *скуль*,
  *файны*), entries of several words (*грамадзянская вайна*, *населены пункт*, *родная мова*),
  abbreviations and brands (*маз*), word forms filed as words of their own (*навіны* beside
  *навіна*, *нагі*, *грыбы*, *зорак*, *голаў*, *сасновыя*, *мроі*, *сурокі*), and spelling variants
  (see above): skip them. Loanwords that Belarusian uses as its own (*камп'ютар*, *футбол*, *кава*,
  *фота*, *тэлефон*) are taught like any word.
