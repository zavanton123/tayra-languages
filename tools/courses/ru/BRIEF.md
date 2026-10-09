# Writing a Russian frequency course

Each course teaches one band of 100 words from the Russian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ru-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Аня хочет кошку. Она идёт в приют. Она видит трёх кошек...* Everyday life
  in the Russian-speaking world: Moscow, Saint Petersburg, Kazan, Novosibirsk, Yekaterinburg,
  Nizhny Novgorod, Sochi, Irkutsk and Lake Baikal, Vladivostok, Kaliningrad, Karelia, villages and
  dachas; and now and then Minsk, Almaty, Tbilisi, Yerevan, Riga, Tallinn, Tel Aviv, Berlin or
  New York, where Russian is spoken at home: home, family, food, shopping, work, school, the city,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *он*/*она* is whenever it could be
  unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order.
  Present tense up to rank 500, with the simple future (*буду* + infinitive) and *хочу/могу/надо*
  + infinitive where natural; from rank 600 the story may be told in the past (*Аня вышла из дома.
  Шёл дождь.*). Little dialogue; when there is some, short and simple, with *ты* among family and
  friends and *вы* with strangers, older people and in formal settings.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*потому что*, *когда*, *пока*, *хотя*, *поэтому*,
  *однако*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands, each band
  using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*был*, *пошёл*), the
    nominative, accusative, prepositional and genitive in plain phrases (*у меня есть...*, *в доме*,
    *на столе*, *нет времени*), possessives, simple questions (*кто*, *что*, *где*, *куда*,
    *когда*), negation, *хочу/могу/надо* + infinitive;
  - ranks 301–600: the past tense for events, the future with *буду* + infinitive, the dative
    (*мне нравится*, *дать другу*), the instrumental (*с братом*, *работать врачом*), the plural,
    the imperative (*скажи*, *идите*), *потому что*, *если*, *чтобы* + infinitive, simple
    comparatives (*больше*, *лучше*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*делал* / *сделал*, the
    perfective future *сделаю*), the verbs of motion (*идти* / *ходить*, *ехать* / *ездить*, and
    with *при-*, *у-*, *вы-*, *за-*), reflexive verbs, *который* in the nominative and accusative,
    *чем* in comparisons, reported speech with *что* and *как*, *пока*, *когда*;
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*два дома*,
    *пять домов*), comparatives and superlatives (*самый большой*, *лучший*), *который* in every
    case, the conditional *бы* (*если бы..., я бы...*), *чтобы* + past (*хочу, чтобы ты пришёл*),
    *пока не*, *как только*;
  - ranks 1301–1600: the indefinite pronouns (*кто-то*, *что-нибудь*), verbs that take a case
    (*интересоваться чем*, *заниматься чем*, *бояться чего*), short adjectives, verbs of motion
    with prefixes, *хотя*, *несмотря на*, *так как*, *вместо*, *кроме*, first present active
    participles and gerunds (*читая*, *идущий*);
  - ranks 1601–2000: gerunds and participles in everyday use (*сидя*, *уходя*, *написанная
    книга*, *построенный дом*), indirect questions with *ли*, *как будто*, *не только... но и*,
    *тот, кто*, the passive with *-ся* (*дом строится*), *один из*, *каждый из*;
  - ranks 2001–2500: past passive and active participles in longer phrases, the conditional in
    the past (*если бы он знал, он бы пришёл*), *чем... тем*, verbal nouns (*открытие*,
    *строительство*), *в то время как*, *вследствие*, *благодаря*, *согласно*; reported speech
    with the tense of the original kept;
  - ranks 2501–3000: a richer, more formal register: connectors such as *тем не менее*,
    *впрочем*, *в то время как*, *ввиду*, *в связи с*, *касательно*; long participial phrases;
    common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *колонка* and the *очерк* (a light reflection on everyday life, as in
    Russian newspapers), dialogue that marks formal and informal speech, less common connectors
    (*лишь бы*, *дабы*, *невзирая на*, *вопреки*);
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *колонка*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Он ищет в кухне. Он ищет в комнате. Он ищет в коридоре.*).
- **Questions at the end**: after the story, a paragraph starting `Вопросы:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *почему* and *как*, with fuller answers):

  ```
  Вопросы:
  Что хочет Аня?
  Аня хочет кошку.
  Куда идёт Аня?
  Она идёт в приют.
  ```

- **Standard Russian**: neutral, correct and natural Russian as written today, with all its
  agreements and cases; no slang, no obscenities, no *мат*. The stories are for readers of every
  country, so keep to the standard language and let a regional or colloquial word (*дача*,
  *маршрутка*, *электричка*) stand only where its context makes it clear. Write the letter **ё**
  wherever the word has it (*всё*, *её*, *ещё*, *идёт*, *жёлтый*, *ёлка*, *пришёл*, *твой*...),
  as books for learners do; the checker reads ё and е alike. Never put stress marks (acute
  accents) in the text: they break words apart for the reader.
- **Russian typography**: direct speech is set out as Russian writes it, each speaker's line a
  paragraph of its own beginning with an em dash and a space:

  ```
  — Привет, Аня! — говорит Саша.
  — Привет! — отвечает Аня.
  ```

  The questions and answers of the final `Вопросы:` block are plain lines, as above. French-style
  « » (guillemets) quote a title or a word (*в газете «Известия»*). Hyphens inside words
  (*по-русски*, *кто-то*) stay hyphens; the em dash is the long one (—). Write numbers as digits
  (*7 часов*, *в 7:30*, *3 брата*), except a number word that is in your band: spell that one out
  to teach it (*семь дней*).
- **Suitable for all ages**: no drugs or alcohol as a topic, no sex, violence, war, cruelty or
  gore. Keep away from politics, the army, the state's doings and nationalism, from current
  conflicts, and from religion taken sides on; everyday civic life (a library, a school board, a
  yard meeting) is fine. Skip band words that only fit such topics, and say so in your report.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/ru/<id>.json`:

```json
{
  "id": "ru-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Russian, e.g. Еда и покупки",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Russian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ru/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *шла* for *идти*). Reuse earlier lessons' new words too.
  `newWords` holds the word as the list writes it: the infinitive of a verb, the nominative
  singular of a noun, the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the Russian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ru/wordlist.tsv`
  (a copy of the app's `ru.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list files every form of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation and the participles of verbs, usually the other aspect (*сказать* under
  *говорить*) and both spellings with and without ё. So a word counts at the rank of its headword,
  whatever form the story uses; a form the list lacks counts as an outside word. If a true form is
  missing, write around it and say so in your report.
- The checker compares whole words, lowercased, ё as е, split at hyphens (*кто-то* is *кто* + *то*,
  *по-русски* is *по* + *русски*; *нибудь* is rank 307, so *кто-нибудь* costs one word at lower
  ranks). The ending of an ordinal after its digits (*5-го*, *20-м*) is not counted, nor is a
  Roman numeral (*XIX век*).
- A form shared by two words counts for both (*лет* for *год* and *лето*, *стали* for *стать* and
  *сталь*, *мою* for *мой* and *мыть*): use a new word in its own sense, never through its homograph.
- A **name** goes in `names` once, in the nominative (*Аня*, *Москва*, *Иван*, *Ольга*): the
  checker also accepts its case forms (*Ани*, *Аню*, *Ане*, *Москве*, *Ивана*, *Ольгой*), up to four
  letters longer than the name's stem, for names whose stem has three letters or more. A name
  whose stem changes in the cases (*Лев* → *Льва*, *Пётр* → *Петра*, *Любовь* → *Любови*,
  *Павел* → *Павла*) needs each stem listed. A name of several words or with a hyphen (*Нижний
  Новгород*, *Санкт-Петербург*) counts whole. Keep only true names there, never common words,
  and never a name that is also a band word (*Вера*, *Надежда*, *Любовь*, *Мир*, *Роза* hide the
  words they spell): then call the character something else. Day and month names are in the list;
  check holidays.
- The list's form lists are noisy (old spellings, informal spellings, unrelated words): a new word
  must appear as itself or a true inflection, never through such a form. Some entries are names,
  abbreviations or foreign words (*ок*, *the*, *spb*): skip them. Loanwords that Russian uses as
  its own (*компьютер*, *интернет*, *футбол*, *кофе*) are taught like any word.
