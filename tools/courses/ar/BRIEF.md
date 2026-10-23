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

- religion: *الله* 35, *عبد* 106, *دين* 112, *مسلم* 190, *شيخ* 267, *إسلامي* 284, *صلى* 413, *إسلام* 433,
  *خلق* 439 (and *أخلاق* through it), *رسول* 445, *نبي* 563, *صلاة* 583, *روح* 568, *مسجد* 640, *مسيح*
  652, *تعالى* 666, *قرآن* 683, *رب* 816, *إمام* 836, *شهيد* 894, *حرام* 944, *رحمة* 951, *كنيسة* 975,
  *مؤمن* 991; *عيد* 476 only as *عيد ميلاد*; *ملك* 118 only as "to own" (*يملك*), never "king" or
  through *ملائكة*;
- politics, war, crime: *رئيس* 59 only as "head, boss" (*رئيسها في العمل*, *رئيس الفريق*), never a
  president; *دولة* 135, *حكم* 151 (only as a referee in sport), *حكومة* 160, *قتل* 155, *سياسي* 164,
  *حرب* 170, *مجلس* 173 (only as a sitting room), *دولي* 183, *ولاية* 189, *وزير* 216, *حزب* 229, *دول*
  227, *شعب* 240, *أمن* 243, *جيش* 258, *اتحاد* 277, *قضية* 282, *عسكري* 288, *وزارة* 304, *منظمة* 317,
  *سلطة* 353 (not "salad" through it), *مملكة* 377, *وطني* 175 (with *وطنية*), *سلاح* 424, *سياسة* 451, *قائد*
  454, *جريمة* 475, *دفاع* 477 (only in sport), *سجن* 500, *انتخاب* 525, *ثورة* 529, *محكمة* 534, *وطن*
  539, *أمير* 614, *هجوم* 623, *ضرب* 622, *جمهورية* 698, *معركة* 723, *سيطرة* 777, *جندي* 819, *مسلح* 824,
  *ضابط* 896, *ضحية* 909, *عدو* 912, *حاكم* 997, *إرهاب* 999; *قوة* 107 only as "strength", never
  through *قوات*;
- death: *موت* 373, *مات* 412, *دم* 466, *وفاة* 684, *ميت* 889;
- names and words from names of places and peoples: *مصر* 78, *بن* 88, *سعودي* 94, *متحد* 152, *أمريكي*
  167, *آل* 246, *سوري* 286, *عرب* 393, *فلسطيني* 417, *مار* 617, *مغرب* 620, *إيراني* 686, *مغربي* 701,
  *لبناني* 734, *قطر* 786, *أوروبي* 788, *يمني* 850, *سام* 871, *إسرائيلي* 570, *بريطاني* 988; a
  language is taught as the language (*عربي* 74, with *عربية*, through *اللغة العربية*; *إنجليزية* 813;
  *فرنسي* 441, with *فرنسية*), never as a people's adjective;
- sexual: *جنس* 659.

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
  before a present-tense verb (*سيذهب*, *سآخذ*). Each piece is a word of its own, in the length too:
  *والكتاب* is *و* + *ال* + *كتاب*, three words, *وسيكتب* is *و* + *س* + *يكتب*. These eight pieces
  are list words of band 1 (*ال* 1, *و* 2, *ب* 5, *ل* 7, *لل* 15, *ف* 20, *س* 40, *ك* 68), taught in
  the first course simply by using them, and known everywhere after. A word with a pronoun after it
  counts as its word (*كتابهم* as *كتاب*), and a name with letters in front (*وسارة*, *بالقاهرة*) is
  known.
- **A word the list has whole is never cut**: *وصل*, *ولد*, *بنت*, *كما*, *فيما*, *مما*, *لما*, *بل*,
  *بدون*, *بسبب*, *باسم*, *لدى*, *لأن*, *لكي*, *لذلك*, *كذلك*, *لقد*, *اليوم*, *الآن*, *الذي*, *برد* "cold"
  (1963), *لبن* "milk" (3303) stay whole, and so does a headword with a pronoun after it: *كلنا* counts
  as *كل*, *بابي* as *باب*, *ولدي* as *ولد*. After *و* or *ف* such a word is not cut further (*ولأن*
  is *و* + *لأن*, a use of *لأن*, and *ولقد* is *و* + *لقد*), and after *لل* a word of the list is the
  article's (*للبنك* is *لل* + *بنك*) unless *ل* is on a word of the list (*للجنة* is *ل* + *لجنة*,
  *للعب* is *ل* + *لعب*). *بكن* "with you" (feminine plural) is one word, under *ب*; *وضوء* is *و* +
  *ضوء* "and light"; *كاميرته* is still *ك* + *أميرته*, *للقطة* *ل* + *لقطة* and *وعدتهما* *و* + *عدت*:
  write *كاميرا* with a word after it (*كاميرا الهاتف*), *للقطة* only as "for a shot".
- **فقد and فهم go by the word after them.** *فقد* is "lost" (636) before a word with *ال* or at the
  end of a sentence (*فقد الولد المفتاح*), and *ف* + *قد* before anything else (*فقد قال* is *ف* + *قد*
  + *قال*): to teach "lost", put a word with *ال* after it (*فقد الولد المفتاح*), or use its other
  forms, which are never cut (*فقدت*, *يفقد*, *فقده*). *فهم* is *ف* + *هم* "so they"
  before *لا*, *لم*, *لن*, *قد*, *في*, *ليسوا*, *كانوا*, *أيضا*, *جميعا* or a verb's plural in *-ون*
  (*فهم لا يعرفون*, *فهم يعملون*), and "understanding, understood" (522) everywhere else. Run `cut.py`
  (see the writers' instructions) on any word you are not sure of.
- The list was built from news and web text, so politics rank high and many everyday words rank
  later than you might expect: *أم* 99, *أب* 105, *هل* 117, *بيت* 200, *ماء* 201, *ذهب* 315, *صغير* 149,
  *ماذا* 446, *نعم* 762, *شكرا* 773, *أكل* 780, *أين* 806, *متى* 915, *شرب* 1152, *أخت* 1020, *نام* 1355,
  *قهوة* 1754, *مطبخ* 1886, *شاي* 2136, *خبز* 2364, *قط* 1788 (with *قطة*); *طفلة* counts as *طفل*.
  Check a word's rank rather than guess it. In the first bands, *ما الذي* (all band 1) asks "what"
  where *ماذا* costs a word, *من* and *أي* ask "who" and "which" (*في أي مدينة؟*).
- The list files the forms of a word under its headword: the persons and tenses of verbs under the
  past (*يقول*, *قالت*, *قلنا* under *قال*), plurals and duals under the singular, the feminine under
  the masculine, nouns with their pronouns. A form neither the list nor `extra_forms.tsv` has counts
  as an outside word. The forms are noisy:
  - the feminine of an adjective is under the masculine (*صغيرة* under *صغير* 149, *طويلة* under *طويل*
    329, *عالمية* under *عالمي* 181, *مالية*, *شديدة*, *رائعة*, *غريبة*, *طبيعية*, *مدنية*), sound and
    broken plurals under the singular (*طلاب* under *طالب* 179, *مرات* under *مرة* 100, *قصص*, *مواد*,
    *أنفس*), and the presents the dictionary has as words of their own under their verbs (*ترى* and *أرى*
    under *رأى*, *تبقى* under *بقي*, *توجد* under *وجد*, *تصل* under *وصل*, *تقرأ* under *قرأ*), as are
    *أعرف*, *أعلم* and *أفعل* "I know, I do"; *أمه*, *تقوله*, *أجده*, *أبا*, *أباه*, *شخصا*, *اسمي*,
    *حياتي* are under their words too (`extra_forms.tsv` keeps *تعمله*); so are *ليست* and *لست* (under
    *ليس*), *المرأة* (*ال* + *امرأة*), *يده*, *يدك* (under *يد*), *وحده*, *وحدها*, *وحدي* (under *وحد* "alone",
    1001), and the duals and the verbs' feminine plurals (*نقطتان*, *مرتين* under *مرة*, *عينيك* under *عين*,
    *يأخذان*, *تأخذا*, *قلن*, *يضحكن*, *التقتا*); so are *أصدقائه*, *أصدقاؤه* (under *صديق*, as
    *ب* + *أصدقائه*), *جارة* and *حفيدة* (under *جار*, *حفيد*), *قديما* and *نقدا* (under *قديم*, *نقد*),
    *المدرسية* (*ال* + *مدرسية*, under *مدرسة*), *الحادية* (under *واحد*), *يعد* (*لم يعد*, under *عاد*), and
    the presents that look like another verb's (*يضع* under *وضع*, not *ضاع*; *يرد* under *رد*; *يصح* under
    *صح*; *يكبر*, *تكبر* under *كبر*; *يطول* under *طال*; *يحين* under *حان*; *يسود* under *ساد*; *يمر* under
    *مر*; *تنام* under *نام*; *تعوض* under *عوض*; *يجدد* under *جدد*; *تبيت* under *بات*; *تغني* under *غنى*;
    *دعيني* under *دع*; *تفضلي* under *تفضل*; *يحل* under *حل*; *أعدت* under *أعاد* and *أعد*);
  - "my X" spelled like an adjective in *-ي* (*يومي* "my day" and "daily", *عملي*, *بلدي*, *هاتفي*,
    *زوجي*, *رأسي*, *مثلي*, *قلبي*, *حياتي*) is listed under the noun as well as on its own, and the
    checker counts it as the noun: it is a use of *يوم*, *هاتف*, *مثل*, never of the adjective, which
    can only be taught where "my X" is rare (*عالمي* 181, *علمي* 713, *شعبي* 571); *مالي* "financial" and
    *داخلي* "internal" are words of their own;
  - still entries of their own: number words in two genders (*ثلاثة* 285 / *ثلاث* 435, *خمس* /
    *خمسة*, *ست* / *ستة*) and *تعد* 827 ("is considered", "you return", "she promises"). Each such pair
    is one word: teach the one that comes first and never the other (it is a form of a taught word), and
    remember that the later one counts only at its own rank. Lexicalised words are words of their own:
    *كثيرا* 428 "a lot", *مباشرة* "directly", *خاصة* "especially", *عامة* "public", *نساء* "women",
    *جامعة* "university", *طائرة* "aeroplane", *جنسية* "nationality", *نظرية* "theory", *معلومات*
    "information"; and *زوجة* "wife" (not *زوج*'s), *بدا* "to seem" (not *بدأ*'s), *تحدث* "to talk" (not
    *حدث*'s), *لعب* "to play" and *لعبة* "game", *أخبر* "to tell" (not *خبر*'s); *تغير* "to change"
    (not *غير*'s), *ظلام* "darkness", *حلوى* "sweets", *كريمة* "cream", *السبت* "Saturday" (with *سبت*),
    *الأربعاء* "Wednesday" (whole), *نادرا* "rarely", *عذرا* "sorry", *تماما*, *قريبا*. *ممثلة* "actress"
    1953 is still apart from *ممثل*;
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
  *سلمان*, *منصور*, *ناصر*, *فيصل*, *مصطفى*, *رائد*, *أنس*, *زيد*, *نزار*, *جاد*, *كنان*, *رهف*. Names
  that are safe: *يوسف*, *أحمد*, *خالد*, *حسين*, *طارق*, *زياد*, *مازن*, *إياد*, *بلال*, *إبراهيم*, *محمد*,
  *سارة*, *مريم*, *فاطمة*, *زينب*, *نادية*, *هالة*, *دانة*, *رحاب*, *مها*, *غادة*, *شيماء*,
  *يارا*, *ميرا*; check any other with `name_check.py`. A kunya counts in two parts: *أبو مازن* is
  *أبو* (*أب*, 105) and the name. Cities are mostly not list words (*القاهرة*, *الإسكندرية*, *دبي*,
  *بيروت*, *دمشق*, *عمان*, *تونس*, *الرباط*, *الدوحة*, *الرياض*, *الكويت*, *مراكش*); *مصر*, *المغرب* and
  *قطر* are list words (skipped: write a city instead), and so are *جدة* (the grandmother), *مسقط*,
  *حلب* and *فاس*. Keep only true names in `names`, never common words.
- The `أسئلة:` header is not counted. Digits (both sets) are numbers and count as known.
