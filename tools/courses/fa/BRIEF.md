# Writing a Persian (Farsi) frequency course

Each course teaches one band of 100 words from the Persian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `fa-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 10,000 words: `fa-mini-0100` to `fa-mini-10000`.

## The register

**One register in every course: standard written Persian of Iran (فارسی معیار نوشتاری)**, plain
and correct, as graded readers, children's books and newspapers write it, neutral between cities,
with no spoken forms standing in for the written ones. The list was counted from subtitles and web
text, so it is full of spoken forms; they are filed under their standard words and are never written:

| Point | Write | Not |
|---|---|---|
| Verbs | *می‌روم, می‌گوید, می‌خواهم, نمی‌دانم, می‌تواند, است* | *میرم, میگه, میخوام, نمیدونم, میتونه, ـه* for *است* |
| Pronouns, words | *آن, این‌ها, آن‌ها, خانه, نان, برای, چه, بله* | *اون, اینا, اونا, خونه, نون, واسه, چی, آره* |
| Object | *را* after the object: *کتاب را دیدم*, *مرا*, *تو را* | spoken *رو*, *منو*, *کتابو* |
| "you" | *تو* to a friend, a child or a relative; *شما* to several people and to anyone you would address politely; one form per pair of speakers | |
| Questions | intonation and a question mark (*لیلا خانه است؟*); *آیا* only from rank 450, in narration | |
| Numbers | Persian digits: *۱۰ سال*, *ساعت ۸*, *۱۴۰۲* | *10*, *٨* (Arabic-Indic forms of ۴ ۵ ۶) |

Careful everyday expressions in dialogue are fine (*سلام*, *خداحافظ*, *ممنون*, *ببخشید*, *لطفاً*,
*خواهش می‌کنم*, *حال شما چطور است؟*, *باشد*). No slang, no English mixed into a sentence.

**Spelling as the Academy writes it (and as the reader needs it):**

- **The non-joiner (نیم‌فاصله, U+200C)**, never a space and never nothing, after *می* and *نمی*
  (*می‌روم*, *نمی‌دانم*), before *ها* (*کتاب‌ها*, *بچه‌ها*), before *ام، ای، است، ایم، اید، اند* after a
  vowel (*خسته‌ام*, *خانه‌ای*, *رفته‌اند*, *رفته‌است* or *رفته است*), before *تر* and *ترین* (*بزرگ‌تر*,
  *بهترین* is written whole), and inside the compounds that are written so (*هیچ‌کس*, *دانش‌آموز*,
  *کتاب‌خانه*). Written with a space, *می روم* is two words (*می* and *روم*) to the reader and the checker.
- **Persian letters**: *ی* and *ک*, never the Arabic *ي* and *ك*; *آ* with its madda.
- **The ezafe is not written**, except after a silent *ه*, where it is *ـهٔ* (*خانهٔ لیلا*); no other vowel
  marks (*واقعاً* keeps its tanvin, as everyone writes it). The checker ignores all marks.
- **Punctuation**: the Persian comma ، and question mark ؟, the semicolon ؛, the full stop and the
  exclamation mark. Speech follows a colon in guillemets, with the closing mark after the full stop:
  *لیلا می‌گوید: «من خسته‌ام.»* Each speaker's line is a paragraph of its own.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life in Iran: Tehran and its neighbourhoods, Isfahan, Shiraz,
  Tabriz, Mashhad, Rasht and the Caspian coast, Yazd, Kerman, Kashan, Hamadan, Kish, a village in
  the mountains, a grandmother's house with a courtyard; and now and then Iranian families in
  Toronto, London or Berlin: home, family, food, shopping, work, school, the city, health, free
  time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Persian has one *او* for "he" and "she",
  so repeat the name wherever *او* could be unclear. Each lesson has its own people: one name is one
  character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, Persian order: subject,
  object with *را*, verb at the end (*سارا کتاب را می‌خواند.*). Present tense up to rank 300; from rank
  400 the story may be told in the past. Little dialogue; when there is some, short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300 (A1): *است / هست / نیست* and the short copula after a word (*خسته‌ام*, *خوبی؟*,
    *کجاست؟*), *داشتن* for having (*دو بچه دارند*), the present with *می‌* (*می‌روم*, *نمی‌دانم*), the
    simple past of common verbs (*رفت*, *آمد*, *دید*, *گفت*) in short sentences, *را*, the plural in *ها*
    (*بچه‌ها*), the ezafe (*خانهٔ لیلا*, *دوست سینا*), the possessive endings (*کتابم*, *خانه‌اش*,
    *دوستشان*), *این / آن*, questions with *چه*, *کی*, *چرا*, *کجا*, *چطور*, *چند*, numbers with the
    singular noun (*دو بچه*, *۱۰ سال*), *باید* and *می‌توانم* + subjunctive (*باید بروم*, *می‌تواند پیدا
    کند*), *می‌خواهم* + subjunctive; compound verbs with *کردن*, *شدن*, *زدن*, *دادن* (*کار می‌کند*,
    *حرف می‌زند*, *پیدا شد*, *انجام می‌دهد*);
  - ranks 301–600 (A2): the past tense in narration with all persons and the negative (*نرفتیم*), the
    imperative (*بیا*, *بنشینید*, *نرو*), the future with *خواهم* only in formal sentences (spoken
    future is the present: *فردا می‌روم*), comparatives (*بزرگ‌تر از*, *بهتر از*), *که* joining clauses
    (*مردی که آمد*), *وقتی*, *چون*, *اگر* + present (*اگر بیایی، می‌رویم*); days, months and the time
    (names, see below);
  - ranks 601–1000 (A2): the present perfect (*رفته‌ام*, *دیده‌است*) and past continuous (*می‌رفت*,
    *داشت می‌رفت*), the subjunctive after *که* and *تا* (*تا ببیند*, *می‌خواهد که بیاید*), superlatives
    (*بزرگ‌ترین*), the plural in *ان* for people (*دوستان*, *مردان*);
  - ranks 1001–1300 (B1): the past perfect (*رفته بود*), the past subjunctive (*رفته باشد*), *اگر* with
    the past for what was not so (*اگر می‌دانستم، می‌آمدم*), the passive with *شدن* (*ساخته شد*);
  - ranks 1301–2000 (B1): reported speech (*گفت که خسته است*), relative clauses of every kind, longer
    sentences joined with *ولی*, *اما*, *بنابراین*, *با این که*, *در حالی که*; a story may be told by a
    first-person narrator or as an e-mail, a letter or a diary entry;
  - ranks 2001–3000 (B2): the Arabic plurals in their everyday words (*اخبار*, *اطلاعات*, *مواد*),
    participles as adjectives (*شکسته*, *نوشته‌شده*), *را* with longer objects; a lesson may also be a
    short informative or opinion text, as long as it is everyday, calm and easy to follow;
  - ranks 3001–4000 (B2): connectors such as *با وجود این*, *از طرف دیگر*, *به همین دلیل*, *در نتیجه*;
    the light column (*یادداشت*) on everyday life, common proverbs and idioms explained by context;
  - ranks 4001–6000 (C1): formal written style, verbal nouns in place of clauses (*پس از رسیدن*),
    a short essay or reflective column that weighs two sides, an interview, a portrait, a review;
    the specialised words of professions, sciences and the arts, each made clear by its context;
  - ranks 6001–10000 (C1–C2): figurative language and set expressions used more freely, words chosen
    for their shades of meaning, literary prose and the essay; never religious texts or quotations.
  A learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `پرسش‌ها:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words (after rank 1000 they may also ask *چرا* and *چطور*, with fuller answers):

  ```
  پرسش‌ها:
  لیلا یک زن است؟
  بله، لیلا یک زن است.
  پریسا کجاست؟
  پریسا در خانه است.
  ```

  The header `پرسش‌ها:` is layout: the checker does not count it as a word.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, prophets, imams, saints, mosque, shrine and church, prayer,
  fasting, pilgrimage, religious dress, mourning months and every holiday of religious origin, and
  the religious formulae of everyday speech, which are never written, not even in dialogue: *خدا را
  شکر*, *ان‌شاءالله*, *ماشاءالله*, *خدا حفظت کند*, *خدایا*, *یا خدا*, *به امید خدا*, *خدا رحمتش کند*,
  *التماس دعا* (say *خداحافظ* only as the plain goodbye it is). Nowruz (*نوروز*), Yalda night (*شب
  یلدا*) and *سیزده‌بدر* are fine as family holidays (the haft-sin table, spring cleaning, a picnic),
  with no religion in them;
- **politics**, the state, the revolution, elections, parties, ministers, presidents, kings and shahs,
  borders, nationalism and every country's disputes; everyday civic life (a library, a school, the
  municipality, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a traffic warden or a lost-property
  office is fine; arrests, prisons and trials are not);
- **death**, killing, funerals, mourning, martyrs, blood and injury beyond a scraped knee;
- **vulgar or sexual words**, insults, words whose main sense is sexual, and kissing, courtship gone
  wrong and divorce;
- **alcohol** (wine, beer), **gambling**, drugs and smoking (the water pipe too);
- **names taught as words**: a band word that is a name, a part of names or a name's derivative,
  and the words for peoples and nationalities (*ایرانی*, *آمریکایی*), are skipped. A language is
  taught as the language (*فارسی*, *انگلیسی*, *عربی* through *زبان فارسی*), never as a people's
  adjective.

Skip band words that only fit these topics, and say so in your report. The subtitle-based list
ranks many of them high; those of the first 1000 ranks:

- religion: *خدا* 105, *امام* 394, *اسلامی* 291, *روح* 515, *دین* 646, *شهید* 796, *مقدس* 889, *دعا* 914;
- politics: *رئیس* 217 only as "head, boss" (*رئیس شرکت*), never a president; *دولت* 246, *ملی* 273, *سیاسی*
  391, *مجلس* 415, *جمهوری* 416, *انقلاب* 518, *وزیر* 543, *شاه* 636, *پادشاه* 679, *سیاست* 762;
- war, crime, violence: *کشتن* 73, *جنگ* 236, *حمله* 272, *پلیس* 313, *زندان* 492, *قتل* 516, *شلیک* 573,
  *ارتش* 607, *دشمن* 689, *فرمانده* 712, *اسلحه* 731, *نظامی* 735, *سرباز* 784, *دادگاه* 788, *جنگیدن* 860,
  *قاتل* 885;
- death: *مردن* 206, *خون* 231, *مرگ* 237, *مرده* 392;
- insults and sex: *لعنتی* 256, *احمق* 496, *سکس* 861, *جنسی* 867;
- peoples: *ایرانی* 238, *آمریکایی* 846.

## The file

Write `tools/courses/fa/<id>.json`:

```json
{
  "id": "fa-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Persian, e.g. غذا و خرید",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Persian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/fa/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *می‌رود* or *رفتند* for *رفتن*, *بچه‌ها* for *بچه*, *خانه‌اش*
  for *خانه*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  the infinitive of a verb (*رفتن*, *کردن*), the singular of a noun.
- **Length** (story and questions together, counted as the checker cuts the words): 80–180 words
  per lesson in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000,
  260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth a
  lesson (a word of the skip list above, a spoken form, a list error such as a name or a form filed as
  a word of its own); the checker's last line lists the band words not taught yet.

Things to know about the Persian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/fa/wordlist.tsv`
  (a copy of the app's `fa.tsv`); the `bands/*.txt` files come from it. The checker reads words as the
  reader does: a word with the non-joiner inside it is one word (*می‌روم*, *کتاب‌ها*), *ك* and *ي* are
  *ک* and *ی*, marks are ignored, and **the non-joiner itself is ignored in matching**: *می‌روم* and
  *میروم* are the same word. So the list and the bands write every form without it (*میروم*, *کتابها*,
  *رفتهام*); you still write it in the text.
- **Verbs are listed by the infinitive** with every form of the verb under it: the present and the
  subjunctive (*می‌روم*, *بروم*, *نرو*), the past (*رفت*, *نرفتیم*), the perfect (*رفته‌اند*), and the
  spoken forms. A compound verb is its two words: *کار کردن* is *کار* and *کردن*, *پیدا کردن* is *پیدا*
  and *کردن*.
- **A word with an ending counts as its word** even where the list lacks that form: the possessive
  endings (*کتابش*, *خانه‌اش*, *دوستانشان*), *ها* and *های* (*بچه‌های*), the indefinite *ی* (*بچه‌ای*),
  the ezafe *ی* after a vowel (*خانه‌ی*), the short copula (*خسته‌ام*, *کجاست*), *تر* and *ترین*. But a
  form that the list has as a word of its own counts as that word, not as yours: *خوبی* "goodness" 165
  (write *خوب است*, never *غذای خوبی* to use *خوب*), *دوستی* "friendship" 1326, *حالت* "state" 283 (write
  *حال شما*, *حالتان*), *کیست* 3511, *نشست* "session", *گذشته* "past", *نوشته* "text", *بسته* "package",
  *زاده*, *مرده* "dead". Check with `rank.py` when in doubt.
- **Not teachable**: *می* 17, *ها* 29 and *ای* 47 are the prefix and endings written apart, which
  standard spelling never does; *رو* 12, *چی* 32 and *آره* 54 are spoken forms (of *را*/*روی*, *چه* and
  *بله*). Skip them.
- The list was built from subtitles and web text, so everyday conversation ranks high and some
  everyday nouns later than you might expect: *پدر* 137, *مادر* 168, *آب* 178, *کتاب* 185, *غذا* 276,
  *برادر* 297, *اتاق* 301, *مدرسه* 331, *خواهر* 478, *شیر* 856, *قهوه* 873, *میز* 953, *نان* 1189, *چای*
  1297, *گرسنه* 2098. Check a word's rank rather than guess it. In the first bands *چیزی* (59) can stand
  for food or a thing (*چیزی می‌خواهد که بخورد*), and *بله* 147 costs a word each time.
- Homographs: unvowelled Persian writes many words alike, and the list files a spelling under one of
  them: *مرد* "man" and "died", *کرد* (under *کردن*), *شیر* "milk" and "lion", *در* "in" and "door", *کی*
  "when" and "who", *دست* "hand", *تر* "wet" and the comparative ending. Use a new word in its own sense,
  never through its homograph.

## Names: months, days, people

**Days of the week are words** (coordinator 2026-11-11): teach *شنبه*, *یکشنبه*, *دوشنبه*, *سه‌شنبه*,
*چهارشنبه*, *پنجشنبه*, *جمعه* when they are in your band, like any word. **Months are names** only where
the month's spelling is also another list word you would not teach in that sense: *تیر* 871 "arrow, shot",
*مهر* 853 "kindness", *دی* 435, *آذر*, *بهمن*, *آبان* (reads as *آب* when not in `names`): put those in
`names` and never teach them. A month with no such clash is a word like any other. A month or day in
`names` counts as known; do not use one to reach the two uses a new word needs. Friday is a day off like any other, never a day of prayer.

Names free to use (none is a list word): *لیلا, بیژن, پریسا, فرهاد, مهسا, سینا, بیتا, رامین, الناز,
داریوش, ساناز, بهرام, منیژه, آناهیتا, نیما, هومن, پرویز*. **Not** names here, because they are list
words or forms: *سارا* 1378, *مریم* (as *مری*), *امید* 683, *بهار* 1492, *رویا*, *ترانه* 1288, *نسیم*,
*کیان* (as *کی*), *آرش* (as *آوردن*), *شیوا*, *مانی*, *آزاده*, *علی*, *حسن*, *رضا*. Check any other name
with `rank.py`. Places free to use, in `names`: *تهران, اصفهان, تبریز, یزد, شیراز, مشهد, رشت, کاشان,
همدان*; *ایران* reads as *ایر* when it is not in `names`. Keep only true names in `names`, never common
words.
