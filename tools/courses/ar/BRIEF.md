# Writing an Arabic (Modern Standard Arabic) frequency course

Each course teaches one band of 100 words from the Arabic word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ar-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *سارة تريد قطة. هي تذهب إلى بيت للحيوانات. هي ترى ثلاث قطط...* Everyday
  life in the Arab world: Cairo, Alexandria, Amman, Beirut, Damascus, Tunis, Rabat, Casablanca,
  Marrakesh, Riyadh, Dubai, Abu Dhabi, Muscat, Doha, Kuwait, Manama, a village by the Nile, a
  grandmother's house with an olive tree; and now and then Arab families in Paris, London, Berlin,
  Montreal or Detroit: home, family, food, shopping, work, school, the city, health, free time,
  travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *هو*/*هي* is whenever it could be
  unclear. Each lesson has its own people: one name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (both *يذهب يوسف إلى العمل* and *يوسف يذهب إلى العمل* are fine). Present tense up to rank 500;
  from rank 600 the story may be told in the past (*ذهبت مريم إلى السوق. كان الجو حارا.*). Little
  dialogue; when there is some, short and simple.
- **Growing difficulty**: the stories stay standalone, everyday and free of twists, but sentences
  get longer and joined, dialogue can carry more of the story, and after rank 1000 a story may be
  told by a first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with
  the bands, each band using what came before:
  - ranks 1–300 (A1): nominal sentences with no verb (*البيت كبير*, *في البيت قطة*, *هذا يوسف*),
    the article ال and the indefinite noun, gender and the agreement of adjectives (*مدينة
    كبيرة*), the attached pronouns on nouns and prepositions (*بيته*, *عندها*, *معي*, *لك*), *عند*,
    *لدى* and *ل* for "to have", *هذا*/*هذه*, the present tense of common verbs in the singular
    persons (*يعمل*, *تعمل*, *أعمل*), *أن* + present after *يريد* and *يمكن* (*يريد أن يعمل*), questions
    with *من*, *ما*, *ما الذي*, *أي*, *كيف*, *هل*, negation with *لا* before a verb and *ليس* in a nominal
    sentence. Band 1 holds some grammar words that come later in full; use them in their simplest
    fixed uses only: *كان* for "was" (*كان يوما جميلا*), *قال*/*يقول* to bring in speech and reported
    speech with *إن* (*تقول دانة إن يومها كان جميلا*), *لم* + present for "did not / has not" (*لم
    يكن عنده وقت*), *سـ* for "will" (*سأكون معك*), *الذي*/*التي* in *ما الذي* and short phrases (*هي التي
    من عمان*);
  - ranks 301–600 (A2): the past tense for events (*ذهب*, *ذهبت*, *ذهبوا*), the plural pronouns and
    verbs, the dual (*طفلان*, *في سيارتين*), the sound plurals (*معلمون*, *معلمات*) and the commonest
    broken plurals (*بيوت*, *أطفال*, *كتب*, *مدن*), the iḍāfa (*باب البيت*, *مدرسة البنات*, *سيارة
    أبي*), the imperative (*اذهب*, *اجلسي*), *لأن*, *عندما*, *لكن*;
  - ranks 601–1000 (A2): stories told in the past, *كان* + present for habits (*كان يلعب كل يوم*),
    broken plurals of every common pattern, the iḍāfa with an adjective (*باب البيت الكبير*), the
    comparative (*أكبر من*), numbers with the counted noun (*ثلاثة أولاد*, *ثلاث بنات*, *15 طالبا*),
    the ordinals (*الأول*, *الثانية*);
  - ranks 1001–1300 (B1): the derived verb forms II–X in everyday verbs (II *علم*, *غير*; III
    *ساعد*, *شارك*; IV *أرسل*, *أخبر*; V *تكلم*, *تعلم*; VI *تعاون*, *تبادل*; VII *انطلق*, *انكسر*; VIII
    *انتظر*, *اشترى*, *اجتمع*; X *استخدم*, *استطاع*) and their verbal nouns (*تعليم*, *مساعدة*,
    *استخدام*), *أن* + subjunctive in every person;
  - ranks 1301–1600 (B1): the future with *سـ* and *سوف*, negation in every tense: *لا* + present,
    *لم* + jussive for the past (*لم يذهبوا*), *لن* + subjunctive for the future (*لن أنسى*), *ليس* in
    every person, *ما* + past in narration; *كان* and its sisters (*أصبح*, *صار*, *ما زال*, *ليس*);
  - ranks 1601–2000 (B1): *إن* and its sisters (*إن*, *أن*, *لكن*, *لأن*, *كأن*, *لعل*), the active
    participle as adjective and noun (*كاتب*, *ساكن*, *ذاهب*), reported speech (*قالت إنها متعبة*,
    *سألها إذا كانت...*), *حتى*, *كي* and *لكي* + subjunctive, *كلما*, the superlative (*أكبر مدينة*);
  - ranks 2001–2500 (B2): the passive: *تم* + verbal noun (*تم افتتاح المكتبة*), the internal passive
    of common verbs where the context settles the reading (*ولد في عمان*, *يقال*, *يسمى*), the
    passive participle (*مكتوب*, *مفتوح*, *معروف*);
  - ranks 2501–3000 (B2): relative clauses in full: *الذي*, *التي*, *الذين*, *اللواتي*, *ما* and *من*
    with the returning pronoun (*البيت الذي سكنت فيه*), and the relative clause after an indefinite
    noun with no pronoun (*رأيت رجلا يبيع الخبز*); circumstantial clauses (*خرج وهو يضحك*, *جاء
    مسرعا*);
  - ranks 3001–4000 (B2): conditional sentences with *إذا*, *إن* + jussive and *لو ... لـ* (*لو كان
    عندي وقت لسافرت*), the tamyīz (*أكثر جمالا*), connectors such as *بالرغم من*, *مع أن*, *بينما*,
    *أما ... فـ*; a more literary, descriptive style, the light column (*خاطرة*) on everyday life,
    dialogue that marks formal and informal speech;
  - ranks 4001–5000 (C1): formal written style: the verbal noun in place of a clause (*بعد وصوله*,
    *من أجل الحصول على*), long iḍāfa chains kept easy to parse, the absolute object (*فرح فرحا
    كبيرا*) and the object of purpose (*سافر طلبا للعلم*), a short essay or reflective column that
    weighs two sides, an interview, a portrait, a review;
  - ranks 5001–6000 (C1): press language, as calm news and feature writing has it: *قام بـ* +
    verbal noun, *أشار إلى أن*, *أكد أن*, *أعلن عن*, *وذلك*, *فضلا عن*, *بالإضافة إلى*, *في حين*,
    *على الرغم من*, dates and figures as the press writes them; the specialised words of
    professions, sciences and the arts, each made clear by its context;
  - ranks 6001–8000 (C1): the same, with figurative language and set expressions used more freely,
    words chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose (the modern
    short story, the essay) with images and rhythm, academic and journalistic registers, learned
    and classical-tinged words explained by their context — never religious texts or quotations.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one clear
  idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*يبحث في المطبخ. يبحث في الغرفة. يبحث في الحديقة.*).
- **Questions at the end**: after the story, a paragraph starting `أسئلة:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask *لماذا* and *كيف*, with fuller answers):

  ```
  أسئلة:
  ما الذي تريده سارة؟
  سارة تريد قطة.
  إلى أين تذهب سارة؟
  تذهب سارة إلى بيت للحيوانات.
  ```

- **Plain everyday Modern Standard Arabic**: neutral, correct and natural MSA as newspapers and
  graded readers write it, with all its agreements; no dialect (no *مش*, *شو*, *إيش*, *ده*, *هيك*,
  *كتير*, *عايز*), no slang. Dialogue is MSA too, kept simple.
- **No vowel marks**: write the text unvowelled, as newspapers do: no fatha, damma, kasra, sukun,
  tanween marks, dagger alif or tatweel. The alif of the accusative tanween is a letter and is always
  written (*شكرا*, *أيضا*, *يوما*, *جديدا*; but *شيئا*, *جزءا*, *سماء*, *مساء*, *مدرسة* take none). A
  shadda only where unvowelled text would write one to stop a misreading the context does not
  settle (*مدرّسة* "teacher" next to *مدرسة* "school"); prefer rewording (*معلمة*). The checker
  ignores all marks.
- **Standard spelling**: hamza on its seat (*أن*, *إلى*, *آخر*, *سؤال*, *شيء*, *مسؤول*), *ى* where it
  belongs (*على*, *إلى*, *حتى*, *مستشفى*, *ليلى*), *ة* (*مدينة*, never *مدينه*), *ال* joined to its
  word and *لل* for *ل + ال* (*للمدرسة*). The list keeps the bare-alif, *ي* and *ه* misspellings as
  forms of their words (*ان*, *الى*, *علي*, *مدينه*), so the checker cannot catch them: you must.
- **Punctuation**: the Arabic comma ، semicolon ؛ and question mark ؟, the full stop and the
  exclamation mark. Speech follows a colon in guillemets: *قالت مريم: «أنا جائعة.»* The questions
  and answers of the final `أسئلة:` block are plain lines, as above.
- **Numbers**: Western digits, in every course (*منذ 50 سنة*, *في الساعة 7:30*, *3 أطفال*), never
  the Eastern digits (٣). Spell a number out only when the number word is a word you teach or one
  already in range (*بيت واحد*, *ثلاثة أيام*), and keep the counted noun correct after the digits
  (*3 أطفال*, *15 طالبا*, *100 كتاب*).
- **Names**: common Arabic first names as characters (see the list below), never a name that is a
  list word, and names are never taught as words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, prophets, holy books, mosque and church, prayer, fasting,
  pilgrimage, religious customs and dress, and holidays of religious origin (Ramadan, Eid,
  the Prophet's birthday, Christmas, Easter); and the religious formulae and idioms of everyday
  speech, which are never written, not even in dialogue: *إن شاء الله*, *الحمد لله*, *ما شاء الله*,
  *بسم الله*, *سبحان الله*, *الله أكبر*, *والله*, *يا الله*, *يا رب*, *الله يعطيك العافية*, *بارك
  الله فيك*, *جزاك الله خيرا*, *رحمه الله*, *السلام عليكم* / *وعليكم السلام* (greet with *مرحبا*,
  *أهلا*, *صباح الخير*, *مساء الخير*; take leave with *مع السلامة*, *إلى اللقاء*), *عيد مبارك*,
  *رمضان كريم*, *جمعة مباركة*, *مبروك* (write *تهانينا*). A birthday (*عيد ميلاد*) is fine; *عيد* is
  taught only through it. Friday is a day of the week like any other, never a day of prayer;
- **politics**, the state, elections, parties, ministers, presidents, kings and princes, borders,
  nationalism and every country's disputes; everyday civic life (a library, a school, a
  municipality, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a traffic warden or a lost-property
  office is fine; arrests, prisons and trials are not);
- **death**, killing, funerals, mourning, martyrs, blood and injury beyond a scraped knee;
- **vulgar or sexual words**, and words whose main sense is sexual (*جنس*, *جنسي*, *مثلي*);
- **alcohol** (wine, beer, bars), **gambling** (casinos, lotteries, betting), drugs and smoking as a
  topic;
- **person names taught as words**: a band word that is a name, a part of names or a name's
  derivative is skipped.

Skip band words that only fit these topics, and say so in your report. The news-based list ranks
many of them high; those of the first 1000 ranks are:

- religion: *الله* 35, *عبد* 99, *دين* 108, *مسلم* 199, *شيخ* 256, *إسلامي* 277, *صلى* 408, *إسلام* 429,
  *خلق* 439 (and *أخلاق* through it), *رسول* 441, *نبي* 552, *صلاة* 573, *روح* 584, *مسجد* 628, *مسيح*
  637, *تعالى* 653, *قرآن* 671, *رب* 799, *إمام* 826, *شهيد* 883, *حرام* 941, *رحمة* 949, *كنيسة* 974,
  *مؤمن* 990; *عيد* 471 only as *عيد ميلاد*; *ملك* 116 only as "to own" (*يملك*), never "king" or
  through *ملائكة*;
- politics, war, crime: *رئيس* 59 only as "head, boss" (*رئيسها في العمل*, *رئيس الفريق*), never a
  president; *دولة* 131, *حكم* 148 (only as a referee in sport), *حكومة* 155, *قتل* 156, *سياسي* 160,
  *حرب* 166, *مجلس* 169 (only as a sitting room), *دولي* 176, *ولاية* 183, *وزير* 212, *حزب* 226, *دول*
  225, *شعب* 235, *أمن* 237, *جيش* 249, *اتحاد* 269, *قضية* 274, *عسكري* 282, *وزارة* 300, *منظمة* 310,
  *سلطة* 344 (not "salad" through it), *مملكة* 371, *وطني* 379, *سلاح* 420, *سياسة* 446, *قائد* 449, *وطنية*
  459, *جريمة* 470, *دفاع* 473 (only in sport), *سجن* 496, *انتخاب* 514, *ثورة* 519, *محكمة* 525, *وطن*
  532, *أمير* 602, *هجوم* 613, *ضرب* 612, *جمهورية* 684, *معركة* 711, *سيطرة* 759, *جندي* 806, *مسلح* 813,
  *ضابط* 885, *ضحية* 897, *عدو* 901, *حاكم* 991, *إرهاب* 999; *قوة* 98 only as "strength", never
  through *قوات*;
- death: *موت* 369, *مات* 406, *دم* 462, *وفاة* 672, *ميت* 950;
- names and words from names of places and peoples: *مصر* 75, *بن* 82, *سعودي* 89, *متحد* 147, *أمريكي*
  163, *آل* 242, *سوري* 280, *عرب* 388, *فلسطيني* 414, *مار* 606, *مغرب* 610, *إيراني* 674, *مغربي* 689,
  *لبناني* 722, *قطر* 770, *أوروبي* 772, *يمني* 840, *سام* 859, *إسرائيلي* 924, *بريطاني* 988; language
  names are words of their own and are taught as languages (*عربية* 124 through *اللغة العربية*,
  *إنجليزية* 794, *فرنسية* 811), the masculine adjective of a taught language (*عربي* 243) is then the
  same word;
- sexual: *جنس* 645.

## The file

Write `tools/courses/ar/<id>.json`:

```json
{
  "id": "ar-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Arabic, e.g. الطعام والتسوق",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Arabic",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ar/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *تقول* for *قال*, *أطفال* for *طفل*, *بيتها* for *بيت*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the past
  tense of a verb (*قال*, *أراد*, *أمكن* — the list files *يمكن* under it), the singular of a noun,
  the masculine of an adjective, the letters in front as letters (*ال*, *و*, *ب*, *لل*, *س*).
- **Length** (story and questions together, counted as the checker cuts the words, see below):
  80–180 words per lesson in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380
  up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, a dialect or foreign word, a list error such as a
  name or a form filed as a word of its own); the checker's last line lists the band words not
  taught yet.

Things to know about the Arabic list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ar/wordlist.tsv`
  (a copy of the app's `ar.tsv`); the checker reads it for the ranks, so a rebuilt app list does not
  move the bands. The `bands/*.txt` files come from it. It cuts the words with the app's own list
  and the ar-en dictionary pack (`dictionaries/ar-en.sqlite`, which must be present), as the reader
  does.
- **The letters in front are words.** The reader cuts the letters written onto a word's front:
  *و* or *ف*, then *ب*, *ك* or *ل*, the article *ال* or both (*بال*, *كال*), *لل* for *ل + ال*, and *س*
  before a present-tense verb (*سيذهب*). Each piece is a word of its own, in the length too:
  *والكتاب* is *و* + *ال* + *كتاب*, three words, *وسيكتب* is *و* + *س* + *يكتب*. These eight pieces
  are list words of band 1 (*ال* 1, *و* 2, *ب* 5, *ل* 7, *لل* 15, *ف* 20, *س* 40, *ك* 61), taught in
  the first course simply by using them, and known everywhere after. A word with a pronoun after it
  counts as its word (*كتابهم* as *كتاب*), and a name with letters in front (*وسارة*, *بالقاهرة*) is
  known.
- **A word the list has whole is never cut**: *فقد* is "lost" (161), never *ف + قد*; *فهم* is
  "understanding" (468), never *ف + هم* (write *وهم* or rephrase); *وصل*, *ولد*, *بنت*, *كما*, *فيما*,
  *مما*, *لما*, *بل*, *بدون*, *بسبب*, *باسم*, *لدى*, *لأن*, *لكي*, *لذلك*, *كذلك*, *لقد*, *اليوم*, *الآن*,
  *الذي* stay whole. Some cuts go wrong, in the reader too: *كلنا* is *ك* + *لنا* (it does not count as
  *كل*: write *كل واحد منا* or *جميعنا* when you teach *كل*), *ولأن* is *و + ل + أن* (not a use of
  *لأن*), *برد* "cold" is *ب + رد* (write *بارد*), *لبن* is *ل + بن*, *بابي* is *ب + أبي*, *ولدي* is
  *و + لدى*. Run `cut.py` (see the writers' instructions) on any word you are not sure of.
- The list was built from news and web text, so politics rank high and many everyday words rank
  later than you might expect: *أم* 104, *أب* 106, *هل* 113, *بيت* 196, *ماء* 197, *ذهب* 311, *صغير* 428,
  *ماذا* 442, *نعم* 742, *شكرا* 754, *أكل* 796, *أين* 787, *متى* 905, *شرب* 1143, *أخت* 1019, *نام* 1715,
  *قهوة* 1771, *مطبخ* 1919, *شاي* 2140, *خبز* 2398, *قطة* 4661; *طفلة* is missing. Check a word's rank
  rather than guess it. In the first bands, *ما الذي* (all band 1) asks "what" where *ماذا* costs a
  word, *من* and *أي* ask "who" and "which" (*في أي مدينة؟*).
- The list files the forms of a word under its headword: the persons and tenses of verbs under the
  past (*يقول*, *قالت*, *قلنا* under *قال*), plurals and duals under the singular, the feminine under
  the masculine, nouns with their pronouns. A form neither the list nor `extra_forms.tsv` has counts
  as an outside word. The forms are noisy:
  - forms filed under another word: *أمه* was under *ماه*, *تقوله* and *تعمله* under stray headwords,
    *أجده* under *أجد*, *أبا*/*أباه* under *أبا* (all now in `extra_forms.tsv`); *شخصا* counts as 1275,
    not as *شخص*; *أفعل* "I do" counts as 3277, *أعرف* "I know" as 1060 and *أعلم* as 668 (they are
    entries of their own): write *لا علم لي*, or the third person;
  - "my X" is often spelled like the list's adjective in *-ي*, which is a word of its own: *يومي*
    1995, *عملي* 1635, *بلدي* 2278, *هاتفي* 2898, *زوجي* 2381, *رأسي* 3666, *عالمي* 562, *مثلي* 1463
    (never write it: say *مثل حالتي* or rephrase); *اسمي* and *حياتي* now count under *اسم* and *حياة*;
  - feminine adjectives filed as entries of their own: *صغيرة* 338 / *صغير* 428, *طويلة* 643 / *طويل*
    767, *عالمية* / *عالمي*, *سابقة* / *سابق*, *اجتماعية* / *اجتماعي*, *مالية*, *شديدة*, *رائعة*, *غريبة*,
    *طبيعية*, *مدنية*; plurals filed as entries of their own: *طلاب* 691 (*طالب* 304), *مرات* 938 (*مرة*
    121), *قصص* 971 (*قصة* 374), *مواد* 654 (*مادة* 753), *أنفس* 922; number words in two genders
    (*ثلاثة* 279 / *ثلاث* 430, *خمس* / *خمسة*, *ست* / *ستة*); verb forms filed as entries (*ترى* 715,
    *تبقى* 765, *تعد* 815, *توجد* 849, *تصل* 947, *تقرأ* 2710). Each pair is one word: teach the one that
    comes first and never the other (it is a form of a taught word), and remember that the later one
    counts only at its own rank. Lexicalised words are words of their own: *كثيرا* 425 "a lot",
    *مباشرة* "directly", *خاصة* "especially", *عامة* "public", *نساء* "women";
  - **homographs**: unvowelled Arabic writes many words alike, and a form counts for every word
    that has it: *علم* "knowledge", "flag", "knew"; *كتب* "wrote" and "books" (*الكتب* counts as the
    verb, so *كتاب* is never taught through it); *ذهب* "went" and "gold"; *عمر* "age" and the name;
    *سن* "age" and "tooth"; *جد*, *جدة* "grandfather, grandmother" and the city; *مدرسة* "school" and
    "teacher"; *سلطة* "authority" and "salad"; *حكم* "rule" and "referee"; *دور* "role", "turn", "floor";
    *شعر* "hair", "poetry", "felt"; *أحد* "one, anyone" and *الأحد* "Sunday". Use a new word in its
    own sense, never through its homograph, and do not lean on a homograph's rank for coverage.
- **Names** go in `names` once, as written in the text, with *ال* if it has one (*القاهرة*,
  *الرياض*); the checker also accepts them with letters in front (*والقاهرة*, *بدبي*, *لسارة*). Many
  Arabic names are list words, and they cannot be used: *عمر*, *علي* (it counts as *على*), *حسن*,
  *سعيد*, *كريم*, *جميل*, *جمال*, *نور*, *أمل*, *إيمان*, *ليلى*, *هدى*, *منى*, *سلمى*, *ريم*, *لينا*,
  *دينا*, *سامي*, *باسم*, *رامي*, *سمير*, *نبيل*, *وليد*, *هشام*, *ياسر*, *ماجد*, *عادل*, *سليم*,
  *سلمان*, *منصور*, *ناصر*, *فيصل*, *مصطفى*, *رائد*, *أنس*, *زيد*, *نزار*, *جاد*, *كنان*. Names that are
  safe: *يوسف*, *أحمد*, *خالد*, *حسين*, *طارق*, *زياد*, *مازن*, *إياد*, *بلال*, *إبراهيم*, *محمد*,
  *سارة*, *مريم*, *فاطمة*, *زينب*, *نادية*, *هالة*, *دانة*, *رحاب*, *مها*, *رهف*, *غادة*, *شيماء*,
  *يارا*, *ميرا*; check any other with `name_check.py`. A kunya counts in two parts: *أبو مازن* is
  *أبو* (*أب*, 106) and the name. Cities are mostly not list words (*القاهرة*, *الإسكندرية*, *دبي*,
  *بيروت*, *دمشق*, *عمان*, *تونس*, *الرباط*, *الدوحة*, *الرياض*, *الكويت*, *مراكش*); *مصر*, *المغرب* and
  *قطر* are list words (skipped: write a city instead), and so are *جدة* (the grandmother), *مسقط*,
  *حلب* and *فاس*. Keep only true names in `names`, never common words.
- The `أسئلة:` header is not counted. Digits (both sets) are numbers and count as known.
