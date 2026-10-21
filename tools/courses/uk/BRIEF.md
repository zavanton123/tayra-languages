# Writing a Ukrainian frequency course

Each course teaches one band of 100 words from the Ukrainian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `uk-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Оксана хоче кішку. Вона йде до притулку. Вона бачить трьох кішок...*
  Everyday life in Ukraine: Kyiv, Lviv, Kharkiv, Odesa, Dnipro, Vinnytsia, Poltava, Chernihiv,
  Zhytomyr, Uzhhorod, Chernivtsi, Ivano-Frankivsk, Ternopil, the Carpathians, villages, a
  grandmother's house with a garden; and now and then Ukrainian families in Warsaw, Kraków,
  Prague, Berlin, Lisbon, Toronto or New York, where Ukrainian is spoken at home: home, family,
  food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *він*/*вона* is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order.
  Present tense up to rank 500, with the future (*буду робити*, *робитиму*) and *хочу/можу/треба*
  + infinitive where natural; from rank 600 the story may be told in the past (*Оксана вийшла з
  дому. Ішов дощ.*). Little dialogue; when there is some, short and simple, with *ти* among family
  and friends and *ви* with strangers, older people and in formal settings (*пане Іване*, *пані
  Ганно*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*тому що*, *бо*, *коли*, *поки*, *хоча*, *тому*,
  *проте*, *однак*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*був*, *була*,
    *пішов*), the nominative, accusative, locative and genitive in plain phrases (*у мене є...*,
    *у місті*, *на столі*, *немає часу*), the vocative of names and family words (*Оксано!*,
    *Петре!*, *мамо!*, *тату!*, *бабусю!*), possessives, simple questions (*хто*, *що*, *де*,
    *куди*, *коли*), negation, *хочу/можу/треба* + infinitive;
  - ranks 301–600: the past tense for events, the future (*буду читати* and *читатиму*), the
    dative (*мені подобається*, *дати братові*), the instrumental (*з братом*, *працювати
    лікарем*), the plural, the imperative (*скажи*, *ідіть*, *ходімо*), *тому що*, *бо*, *якщо*,
    *щоб* + infinitive, simple comparatives (*більше*, *краще*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*робив* / *зробив*, the
    perfective future *зроблю*), the verbs of motion (*іти* / *ходити*, *їхати* / *їздити*, and
    with *при-*, *ви-*, *у-*/*в-*, *за-*, *пере-*), reflexive verbs in *-ся*, *який* in the
    nominative and accusative, *ніж* in comparisons, reported speech with *що* and *як*, *поки*,
    *коли*;
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*два
    будинки*, *п'ять будинків*), comparatives and superlatives (*найбільший*, *кращий*), *який* in
    every case, the conditional with *б/би* (*якби..., я б...*), *щоб* + past (*хочу, щоб ти
    прийшов*), *поки не*, *як тільки*;
  - ranks 1301–1600: the indefinite pronouns (*хтось*, *щось*, *будь-хто*, *що-небудь*), verbs
    that take a case (*цікавитися чим*, *займатися чим*, *боятися чого*, *сумувати за ким*), short
    adjectives as predicates (*радий*, *винен*, *повен*), *хоча*, *незважаючи на*, *оскільки*,
    *замість*, *крім*, the first adverbial participles (*читаючи*, *прочитавши*);
  - ranks 1601–2000: adverbial participles in everyday use (*сидячи*, *ідучи*, *повернувшись*),
    passive participles (*написана книжка*, *збудований дім*), the impersonal forms in *-но/-то*
    that Ukrainian prefers to a passive (*зроблено*, *написано*, *відкрито*), indirect questions
    with *чи*, *ніби* / *наче*, *не тільки... а й*, *той, хто*, *один із*, *кожен із*;
  - ranks 2001–2500: participle phrases, the conditional of the past (*якби він знав, він би
    прийшов*), *чим... тим*, verbal nouns (*відкриття*, *будівництво*), *у той час як*,
    *завдяки*, *згідно з*, *унаслідок*; reported speech with the tense of the original kept;
  - ranks 2501–3000: a richer, more formal register: connectors such as *утім*, *проте*, *тим
    часом*, *з огляду на*, *у зв'язку з*, *щодо*; long participle phrases; common idioms
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *колонка* and the *нарис* (a light reflection on everyday life, as in
    Ukrainian newspapers), dialogue that marks formal and informal speech, less common connectors
    (*аби лише*, *попри*, *всупереч*, *хай навіть*);
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
  (*Він шукає на кухні. Він шукає в кімнаті. Він шукає в коридорі.*).
- **Questions at the end**: after the story, a paragraph starting `Запитання:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *чому* and *як*, with fuller answers):

  ```
  Запитання:
  Що хоче Оксана?
  Оксана хоче кішку.
  Куди йде Оксана?
  Вона йде до притулку.
  ```

- **Standard Ukrainian**: neutral, correct and natural Ukrainian as it is written today, in the
  current spelling (the 2019 *правопис*: *проєкт*, *ґанок*), with all its agreements and cases;
  no slang, no obscenities. The stories are for readers of every region, so keep to the standard
  language and let a regional or colloquial word (*маршрутка*, *хата*, *бабця*) stand only where
  its context makes it clear. **No Russianisms or surzhyk**: write *брати участь* (not *приймати
  участь*), *протягом* (not *на протязі*), *наступний* (not *слідуючий*), *чинний* (not
  *діючий*), *бути* or *є* (not *являтися*), *отримувати* (not *получати*), *найкращий* (not
  *самий кращий*), *оскільки* / *бо* (not *так як* for "because"), *о котрій годині*, *сумувати
  за кимсь* (not *скучати по*), *вибачте* / *перепрошую* (not *вибачаюсь*), *дякую*, never
  *спасибі* as the norm. Follow the euphony rules (*милозвучність*): *у*/*в*, *і*/*й*,
  *з*/*із*/*зі* by the sounds around them (*Оксана в місті*, *брат у місті*; *Оксана й Петро*,
  *брат і сестра*; *з Києва*, *Устим із Торонто*, *зі мною*), and the same for words that begin
  with them (*вдома* / *удома*, *вже* / *уже*). Never put stress marks (acute accents) in the
  text: they break words apart for the reader.
- **Ukrainian typography**: direct speech is set out as Ukrainian writes it, each speaker's line a
  paragraph of its own beginning with an em dash and a space:

  ```
  — Привіт, Оксано! — каже Петро.
  — Привіт! — відповідає Оксана.
  ```

  The questions and answers of the final `Запитання:` block are plain lines, as above. « »
  (guillemets) quote a title or a word (*у журналі «Країна»*). The apostrophe is written straight,
  as the list writes it (*м'ясо*, *п'ять*, *сім'я*). Hyphens inside words (*будь-хто*,
  *по-моєму*) stay hyphens; the em dash is the long one (—). Write numbers as digits (*7
  годин*, *о 7:30*, *3 брати*), except a number word that is in your band: spell that one out to
  teach it (*сім днів*).
- **Suitable for all ages**: no drugs or alcohol as a topic, no sex, violence, war, cruelty or
  gore. Keep away from politics, the army, the state's doings and nationalism, from the war and
  everything around it (air-raid alerts, the front, occupation, refugees, volunteers for the
  army), and from religion taken sides on; everyday civic life (a library, a school board, a
  meeting of the residents of a building) is fine. The list ranks many such words high (*війна*
  118, *військовий*, *армія*, *зброя*, *ворог*, *перемога*, *окупація*, *фронт*): skip band words
  that only fit such topics, and say so in your report.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/uk/<id>.json`:

```json
{
  "id": "uk-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Ukrainian, e.g. Їжа та покупки",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Ukrainian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/uk/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ішла* for *піти*). Reuse earlier lessons' new words too.
  `newWords` holds the word as the list writes it: the infinitive of a verb, the nominative
  singular of a noun, the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name, a Russian
  word or a form filed as a word of its own); the checker's last line lists the band words not
  taught yet.

Things to know about the Ukrainian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/uk/wordlist.tsv`
  (a copy of the app's `uk.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built largely from news and web text, so politics and the war rank high and some
  everyday words rank later than you might expect: *казати* is 119 and *жити* 167 (so *каже* and
  *живе* are outside the first course), *мама* 481, *тато* 1314, *кіт* 1486, *бабуся* 1958, *чай*
  1492, *кухня* 1545. Check a word's rank rather than guess it, and in the first bands keep the
  dialogue tags few (the speaker is often clear without *каже*).
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation and the participles of verbs, often the other aspect (*зробити* is a
  word of its own, but *піти* holds *іти*, *іде*, *ішов*), derived adjectives (*шкільний* under
  *школа*) and comparatives (*краще* under *добре*, *більше* under *багато*). So a word counts at
  the rank of its headword, whatever form the story uses. Some common forms are missing: the
  dictionary's forms of the list's words are added from `extra_forms.tsv`, and a true form that is
  still missing counts as an outside word. Then write around it and say so in your report.
- **Homographs**: a form shared by two words counts for both, and a form listed under a word may
  be another word altogether. *Мати* is both "to have" and "mother" (one entry, rank 30; *матері*
  counts); *мало* and *менше* are filed under *мати*; *ніж* is "than" and "knife"; *ніс* (nose)
  is under *нести*, *рот* under *рота*, *ручка* under *рука*, *рада* (council) holds *раді*,
  *радо* (of *радий*), *лютий* is February and "fierce". Use a new word in its own sense, never
  through its homograph, and do not lean on a homograph's rank to pass the coverage.
- **Euphonic variants**: both spellings of most words are one entry (*вже* / *уже*, *вдома* /
  *удома*, *усі* / *всі*), but some *у-*/*в-* and *і-*/*й-* variants are entries of their own at a
  later rank (*вчора* 883, *учора* 3161; *вночі* 1571, *уночі* 6106; *ввечері* 2044, *увечері*
  4161; *вбити* / *убити*, *вчити* / *учити*, *ймовірно* / *імовірно*), and a few are missing
  (*узяла*, *удень*, *улітку*). When euphony allows either, write the spelling the list ranks
  lower; never teach such a variant as a new word (it is the same word as the one taught before):
  skip it and say so.
- The checker compares whole words, lowercased, the apostrophe folded (*м'ясо*, *м’ясо* and
  *мʼясо* are one word), split at hyphens (*будь-хто* is *будь* + *хто*, *по-моєму* is *по* +
  *моєму*; *небудь* is not in the list, so *що-небудь* costs one outside word). The ending of an ordinal after its digits (*5-го*, *2-й*) is not counted, nor is
  a Roman numeral (*XXI століття*). The `Запитання:` header is not counted.
- A **name** goes in `names` once, in the nominative (*Оксана*, *Петро*, *Андрій*, *Марія*,
  *Львів*, *Карпати*): the checker also accepts its case forms and its vocative from the regular
  declension (*Оксани*, *Оксані*, *Оксану*, *Оксаною*, *Оксано*; *Ольга*, *Ользі*; *Петра*,
  *Петрові*, *Петре*; *Андрія*, *Андрієві*, *Андрію*; *Марії*, *Марією*, *Маріє*; *Назара*,
  *Назаре*; *Карпат*, *Карпатах*), however short the name; a name in *-ів* or *-їв* also gives the
  stem in *-ов-*/*-єв-* (*Львова*, *Харкова*, *Києва*). A name whose stem changes otherwise
  (*Ірпінь* → *Ірпеня*, *Ріг* → *Рогу*, *Олег* → *Олеже*) needs each such form listed. A name of
  several words or with a hyphen (*Біла Церква*, *Івано-Франківськ*) is taken part by part, each
  part declined. Possessive adjectives of names (*Оксанин*, *Петрів*) are not generated: write *у
  Оксани*, *брат Петра*. Keep only true names there, never common words, and never a name that is
  also a list word (*Надія*, *Віра*, *Любов*, *Роман*, *Лука*, *Зоряна*, *Марко* — *марка* —,
  *Влад* — *влада* —, *Ігор* — *ігор*, of *гра*): then call the character something else.
  The list leaves out *Україна*, *Київ*, *Львів*, *Москва* and *Росія* (they are names), but has
  the adjectives of places (*київський* 272, *львівський* 501, *харківський* 881), which are
  list words, not names. Day and month names are in the list. *Різдво*, *Великдень* and
  *інтернет* are missing (the dictionary files them as names): put them in `names` when a story
  needs them.
- The list's form lists are noisy (old and Russian-style spellings such as *сказать* or *такая*,
  unrelated words, dialect forms in `extra_forms.tsv`): a new word must appear as itself or a
  true inflection, never through such a form, and a Russian-style form the checker accepts is
  still wrong in the text. Some entries are not words to teach: abbreviations (*грн*, *млн*,
  *вул.*), colloquialisms (*типу*, *ага*), vulgar words, Russian words
  and surzhyk (*да*, *но*, *всьо*, *нє*, *власть*, *бизнес*, *грязь*, *ухо*, *рисунок*,
  *мужчина*), word forms filed as words of their own (*посад*, *тура*, *вчителів*), and
  euphonic variants (see above): skip them. Loanwords that Ukrainian uses as its own
  (*комп'ютер*, *футбол*, *кава*, *фото*) are taught like any word.
