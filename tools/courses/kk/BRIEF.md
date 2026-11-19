# Writing a Kazakh frequency course

Each course teaches one band of 100 words from the Kazakh word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `kk-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 8,395 words, so there are 84 courses,
`kk-mini-0100` … `kk-mini-8400` (the last teaches ranks 8301–8395).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life in Kazakhstan: Almaty and Astana, the regional cities
  (Shymkent, Taraz, Aktobe, Pavlodar, Karaganda, Kostanay, Kokshetau, Semey, Oskemen, Turkistan,
  Kyzylorda, Uralsk), villages and the steppe, the mountains near Almaty (Medeu, Shymbulak, the
  Kolsai lakes, Charyn canyon), the Caspian and Balkhash, and now and then Kazakhs in Moscow,
  Istanbul, Seoul or Berlin: home, family, food, shopping, work, school, the city, health, free
  time, travel, Nauryz, a toi (family feast), guests and dastarkhan.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, irony, wordplay or open endings.
  Events in the order they happen. Few characters (one to three), named clearly; say who *ол* is
  whenever it could be unclear. One name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  (subject, object, verb: *Асқар Шымкент қаласында тұрады*). Little dialogue; when there is some,
  in guillemets with a dash and *дейді*: *Динара: «Маған жұмыс керек», — дейді.*
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–100 (A1): the bare predicate with a dash (*Асқар — қазақ*), the present-future
    (*тұрады*, *барады*, *біледі*; *тұрамын*, *тұрамыз*), its negative (*бармайды*, *білмейді*),
    the cases (*үйдің*, *үйге*, *үйде*, *үйден*, *үйді*, *балалармен*), the plural, the
    possessives (*үйі*, *атым*, *атыңыз*, *Асқардың үйі*), *бар* / *жоқ* "there is / there is
    not", *емес*, *керек*, *-ғысы келеді* "wants to" (*білгісі келеді*), the question particle
    *ма/ме/ба/бе/па/пе* (16) and *не* (50), *жатыр* / *отыр* / *тұр* / *жүр* for "is …ing"
    (*келе жатыр*), *-ып* joining two verbs (*жолға түсіп*, *алып*), the simple past (*таптым*,
    *келді*), *екен*, *еді*, *деп* and *дейді* for speech. *кім* (165), *қалай* (179), *иә*
    (180), *қайда* (271), *қай* (614) and *неше* (1836) rank later: each question of the first
    course costs one or two outside words, which the first course's 90% allows; ask with
    *ма* and *не* where you can;
  - ranks 101–300 (A1): the question words, *-ған* past participles (*келген адам*), *-атын*
    (*тұратын үй*), the conditional *-са* (*барса*), the imperative and requests (*бар*, *барыңыз*,
    *келіңіздер*, *айтшы*), *-уға* "in order to / to" (*баруға*), *-ып жүр*, *-ып отыр*, *бірге*,
    *үшін*, *кейін*, *соң*, *дейін*;
  - ranks 301–1000 (A2): the past in *-ған* and *-ып*-past (*барған*, *барыпты*), *-атын еді*
    (habit in the past), the converbs *-ғанда* "when", *-ғанша* "until", *-ып болу*, reported
    speech with *деп*, *-уы керек* / *-уға тиіс* "must", comparisons with the ablative and
    *-рақ* (*үлкенірек*), the ordinals (*екінші*), *-дағы* (*қаладағы үй*), *-шы* agent nouns;
  - 1000 and up (B1–C1): the passive, reflexive, reciprocal and causative verbs (*айтылды*,
    *жиналды*, *көрісті*, *көрсетті*), *-ғандықтан* "because", *-са да* "although", participle
    clauses (*мен оқыған кітап*), the written style of news and letters where the theme wants it.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Сұрақтар:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  Сұрақтар:
  Асқар қай қалада тұрады?
  Асқар Шымкент қаласында тұрады.
  ```

  The header `Сұрақтар:` is layout: the app and the checker do not count it as a word.
- **Standard literary Kazakh in Cyrillic**, as newspapers and schoolbooks write it: the letters
  *ә ғ қ ң ө ұ ү һ і* (never Russian *и* or Latin *i* for *і*, never *у* for *ұ*/*ү*, never Latin
  look-alikes), no Latin-alphabet Kazakh, no Russian words where Kazakh has its own (*рахмет*, not
  *спасибо*), no slang. The question particle and the other little words follow vowel harmony
  and the sound before (*бара ма*, *келе ме*, *бар ма*, *жақсы ба*, *үйде ме*; *Айгүл де*,
  *жұмыс та*; *Асқар мен Айгүл*, *баламен*, *досыммен*).
- **Punctuation**: comma, full stop, question and exclamation marks; speech in guillemets «…»
  followed by a dash (*«…», — дейді*); a dash between subject and noun predicate (*Асқар — қазақ*).
  A hyphen splits a word for the app (*қол-аяқ* is *қол* + *аяқ*): avoid hyphenated words and
  suffixes after digits (*35 жаста*, not *35-те*).
- **Numbers**: digits for years, counts and ages (*2015 жылы*, *5 сөз*, *35 жаста*); no endings
  after a hyphen (*8-де*): write *сағат сегізде* once *сегіз* is in range. Spell a number out only
  when the band teaches it or it is already in range (*бір*, *екі*).
- **Names**: Kazakh names that are no list word (*Асқар*, *Айгүл*, *Ерлан*, *Динара*, *Нұржан*,
  *Гүлназ*, *Бауыржан*, *Әлия*, *Талғат*, *Айжан*, *Ерасыл*, *Мәдина*, *Ержан*, *Жансая*, *Арайлым*,
  *Назерке*, *Нұрсұлтан*, *Ақбота*, *Ербол*); many names are words (*Серік* "partner",
  *Қанат* "wing", *Сұлтан*, *Әсем* "elegant", *Дәурен*, *Айдана*, *Бекзат*, *Мақсат* "goal", *Арман*
  "dream", *Самал* "breeze", *Сәуле* "ray", *Қуаныш* "joy", *Мейрам* "holiday", *Мұрат* "aim", *Абай*): never use
  those. Places too: *Астана* (157), *Атырау* (793), *Ақтау* (739, also the verb's form) are list
  words and count as words of their rank, never as names, and so are *Орал* (*оралу*'s form) and
  *Қарағанды* (*қарау*'s form), which count as known from that word's rank on; *Алматы*, *Шымкент*, *Тараз*, *Ақтөбе*,
  *Павлодар*, *Қостанай*, *Көкшетау*, *Семей*, *Түркістан* are names. A name takes the endings
  joined (*Асқардың*, *Алматыға*, *Айгүлге*, *Ақтөбеден*): the checker counts those as the name.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

Follow step 4 of `tayra-writers/WRITER-BRIEF.md` (2026-11-13: no topic rules). Any normal
everyday, cultural, historical or story topic is fine. Only avoid explicit sexual content, slurs
used as insults and gratuitous gore. Teach ordinary words in their main sense.

## The file

Write `tools/courses/kk/<id>.json`, the shape of `kk-mini-0100.json`: `id`, `rankUpTo`, `level`,
`title` (from `themes.tsv`), `description` (one English sentence), `topic`, `names` (people and
places used in the texts, never a list word), and ten `lessons`, each with `title`, `summary`
(one English line), `newWords` (the band words it introduces, as the list writes them: a verb's
dictionary form in *-у* (*бару*, *айту*, *оқу*, *қою*), a noun's nominative) and `text`
(paragraphs separated by a blank line, each question and answer on its own line).

Levels as in `themes.tsv`: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`,
then `C1`, and `C2` for the last four.

## The rules, checked by `python3 tools/check_course.py tools/courses/kk/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93%
  up to rank 300, 90% for the first course). Names in `names` and numbers count as known.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *тұрады* for *тұру*, *қаласында* for *қала*).
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240
  up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 above.
- **Coverage of the band**: aim to teach at least 90 of the band's words; the checker's last line
  lists the band words not taught yet.

## Things to know about the Kazakh list

- The courses are pinned to `tools/courses/kk/wordlist.tsv` (a copy of the app's `kk.tsv`, built
  from the Leipzig Kazakh news (Azattyq, most of it) and Wikipedia corpora with a tenth from film
  subtitles, bounded by the kk-en dictionary). The English pack lacks many common words (*жету*,
  *қайту*, *бұрын*, *мұндай*, *жиі*, *тірі*), so about 600 words, 120 of the first 1,000, were
  added by hand and have no dictionary entry in the app yet.
- Its forms come from regular paradigms the builder generates and keeps where the corpora have
  them: a noun's cases, plural, possessives and their cases, *-дағы*, *-дай*, *-ғы* (*жылғы*,
  *бүгінгі*), *-лық* adjectives the dictionary lacks (*аудандық* under *аудан*), the personal
  endings (*баламын*); a verb's tenses, participles, converbs, conditional, imperatives, verbal
  noun, all negated. A passive, reflexive, reciprocal or causative verb that the dictionary lacks
  is filed under its verb (*айтылды* under *айту*, *жиналды* under *жинау*, *енгізілді* under
  *ену*): it is a use of that verb. One the list has as a word (*көрсету*, *түсіну*, *көріну*,
  *шығару*, *келтіру*, *өзгерту*) is a word of its own; *жеткізу* is *жету*'s.
- A form the list lacks counts as outside, even when it is regular (*қалғымыз* "we want to
  stay"): the app's reader knows only the list's forms. `extra_forms.tsv` adds the dictionary's
  own inflection tables; report a missing everyday form and it can be added.
- The list is built from news, so its top is formal (*президент* 51, *сот* 74, *билік* 80,
  *халық*, *мемлекет*, *облыс*, *заң*) and some everyday words rank late (*отбасы* 322, *ана* 232,
  *әке* 235, *дүкен* 1231, *нан* 1568, *мысық* 2362). Write stories the bands can carry; a cost of
  a few outside words is normal.
- Little words that change with vowel harmony and the sound before are one word: *ма* (*ме*, *ба*,
  *бе*, *па*, *пе*), *да* (*де*, *та*, *те*), *мен* "and, with" (*бен*, *пен*), *ғой* (*қой*),
  *ғана* (*қана*). The pronouns' cases are their pronoun's (*маған*, *оған*, *мұны*, *соны*).
- Skip list noise: names and titles (*абай*, *астана* as the city, *атырау*, *ақтау* as the city,
  *серік*, *қанат*, *барыс*, *азаттық* as the radio's name), *бақ* when the corpora's БАҚ "mass
  media" made it, root nouns that only stand with an ending (*алд*, *үст*, *аст* — teach them
  through *алдында*, *үстінде*, *астында*, which are their forms), and the rare homographs a common
  word's form made.
