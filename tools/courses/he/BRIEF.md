# Writing a Hebrew (Modern Hebrew) frequency course

Each course teaches one band of 100 words from the Hebrew word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `he-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 8,512 words: the courses are `he-mini-0100` to
`he-mini-8500`, 85 of them.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *דניאל רוצה כלב. הוא הולך לחנות של חיות. הוא רואה שלושה כלבים...* Everyday
  life in Israel: Tel Aviv, Herzliya, Ra'anana, Holon, Ashdod, Ashkelon, Akko, Tiberias, Safed, Eilat,
  Be'er Sheva, a moshav in the Galilee, the beach, the Kinneret; and now and then Hebrew-speaking
  families in Paris, London, Berlin, Toronto or New York: home, family, food, shopping, work, school,
  the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *הוא*/*היא* is whenever it could be
  unclear. Each lesson has its own people: one name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Present tense up to rank 500; from rank 600 the story may be told in the
  past (*דנה הלכה לשוק. היה חם מאוד.*). Little dialogue; when there is some, short and simple.
- **Growing difficulty**: the stories stay standalone, everyday and free of twists, but sentences
  get longer and joined, dialogue can carry more of the story, and after rank 1000 a story may be
  told by a first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with
  the bands, each band using what came before:
  - ranks 1–300 (A1): sentences with no verb in the present (*הבית גדול*, *זה הספר של דני*, *היא
    בבית*), *יש* and *אין* for "there is" and "to have" (*יש לו בית*, *אין לה זמן*), the article *ה*
    and the letters written onto a word (*ו*, *ב*, *ל*, *מ*, *ש*, *כ*), gender and the agreement of
    adjectives (*בית גדול*, *עיר גדולה*, *ילדים גדולים*), the present tense of common verbs in its four
    forms (*רוצה*, *רוצים*, *רוצות*; *הולך*, *הולכת*), the infinitive after *רוצה*, *צריך*, *יכול* (*הוא
    רוצה ללכת הביתה*), *את* before a definite object (*היא רואה את הים*), the prepositions with their
    pronouns (*לי*, *לו*, *שלה*, *איתו*, *אותה*, *עליו*), questions with *מה*, *מי*, *איך*, *למה*, *כמה*,
    negation with *לא* and *אין*. Band 1 holds some grammar words that come later in full; use them in
    their simplest fixed uses only: *היה*, *הייתה*, *היו* for "was, were" (*היה יום טוב*), *יהיה* for
    "will be", *אמר* to bring in speech (*הוא אמר: "..."*);
  - ranks 301–600 (A2): the past tense of common verbs in every person (*הלכתי*, *הלכת*, *הלך*,
    *הלכה*, *הלכנו*, *הלכו*), the construct state in common pairs (*בית ספר*, *יום הולדת*, *חדר
    שינה*), the plural patterns of common nouns (*ילדים*, *ערים*, *בתים*, *ימים*), numbers with the
    counted noun in both genders (*שלושה ילדים*, *שלוש ילדות*), *כש*, *כי*, *אבל*, *אז*;
  - ranks 601–1000 (A2): stories told in the past, the future tense of common verbs (*אלך*, *תלך*,
    *ילך*, *נלך*), the imperative as spoken Hebrew makes it (*בוא*, *תבואי*, *תשב*), the comparative
    (*גדול יותר מ...*), the ordinals (*ראשון*, *שנייה*), time and dates;
  - ranks 1001–1300 (B1): the verb patterns in everyday verbs: pi'el (*דיבר*, *סיפר*, *ביקש*),
    hif'il (*הגיע*, *הבין*, *הרגיש*), hitpa'el (*התחיל*, *הסתכל*, *התלבש*), nif'al (*נכנס*, *נשאר*,
    *נפגש*), and their verbal nouns (*דיבור*, *הסבר*, *התחלה*);
  - ranks 1301–1600 (B1): the real conditional with *אם* (*אם יהיה חם, נלך לים*), reported speech
    with *ש* (*היא אמרה שהיא עייפה*), relative clauses with *ש* and the returning pronoun (*הבית שגרנו
    בו*);
  - ranks 1601–2000 (B1): *כדי ש*, *למרות ש*, *בגלל ש*, *אחרי ש*, *לפני ש*, *עד ש*, the passive
    participle as an adjective (*סגור*, *פתוח*, *כתוב*), *היה* + participle for habits in the past
    (*הוא היה הולך לים כל יום*);
  - ranks 2001–2500 (B2): the passive patterns pu'al and hof'al in common forms (*סופר*, *תוקן*, *הוכן*,
    *הוזמן*), the construct state in full, with the article on its second word (*בית הספר*, *יום
    ההולדת*), the possessive endings on common nouns (*ביתו*, *עבודתה*, *חבריהם*) beside *של*;
  - ranks 2501–3000 (B2): the unreal conditional (*אם הייתי יודע, הייתי בא*; *אילו*, *לו*), relative
    clauses with *אשר* in written style, *כאילו*, *בעוד ש*;
  - ranks 3001–4000 (B2): a more literary, descriptive style, the light column on everyday life,
    dialogue that marks formal and informal speech; connectors such as *לעומת זאת*, *אף על פי כן*,
    *מצד אחד ... מצד שני*;
  - ranks 4001–5000 (C1): formal written style: the verbal noun in place of a clause (*עם הגעתו*,
    *לשם השגת*), the infinitive with a preposition (*בבואו*, *לפני צאתה*) where it reads naturally,
    long construct chains kept easy to parse, a short essay or reflective column that weighs two sides,
    an interview, a portrait, a review;
  - ranks 5001–6000 (C1): press language, as calm news and feature writing has it: *נמסר כי*, *לדברי*,
    *על פי*, *בעקבות*, *במסגרת*, *יחד עם זאת*, dates and figures as the press writes them; the
    specialised words of professions, sciences and the arts, each made clear by its context;
  - ranks 6001–8000 (C1): the same, with figurative language and set expressions used more freely,
    words chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–8500 (C2): texts close to what native readers meet: literary prose (the modern short
    story, the essay) with images and rhythm, academic and journalistic registers, learned and
    Biblical-tinged words explained by their context — never religious texts or quotations.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one clear
  idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*הוא מחפש במטבח. הוא מחפש בחדר. הוא מחפש בגינה.*).
- **Questions at the end**: after the story, a paragraph starting `שאלות:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask *למה* and *איך*, with fuller answers):

  ```
  שאלות:
  מה דניאל רוצה?
  דניאל רוצה כלב.
  לאן דניאל הולך?
  דניאל הולך לחנות של חיות.
  ```

- **Plain everyday Modern Hebrew**: neutral, correct and natural Hebrew as newspapers and graded
  readers write it, with all its agreements; no slang (*סבבה*, *יאללה*, *אחלה*, *וואלה*, *חבל על
  הזמן*), no army or street jargon, no Biblical or rabbinic style below C2. Dialogue is everyday
  Hebrew too, kept simple and correct (*אני רוצה*, never *אני רוצָה* spelled oddly; the future, not
  the imperative of the old grammar, for requests: *תשב*, *תבואי*).
- **No vowel points**: write the text unpointed, in the full spelling (*כתיב מלא*) that newspapers
  and the list use: *ו* and *י* for the vowels (*שולחן*, *חנות*, *סיפור*, *ספרייה*, *אימא* or *אמא* as
  the list has it), *וו* for a consonantal *ו* inside a word (*תקווה*, *מצווה*, *הוועדה*) and *יי* for a
  consonantal *י* (*בניין*, *עלייה*), the final letters *ך ם ן ף ץ*. When the list writes a word in
  both spellings, use the commoner, the one the list has as the headword or first form. The checker
  ignores points and cantillation, but write none.
- **Geresh and gershayim**: a geresh for the sounds Hebrew writes with one (*ג׳ינס*, *צ׳יפס*, *ז׳קט*) and
  in abbreviations (*פרופ׳*); gershayim in acronyms (*ד״ר*); prefer the Hebrew signs ׳ and ״, though
  the reader and the checker read an apostrophe after a letter as a geresh and a double quote between
  two letters as gershayim. Keep acronyms rare (no *צה״ל*, *ארה״ב*: write the words).
- **Punctuation**: the full stop, comma, question mark and exclamation mark as in English. Speech
  follows a colon in double quotation marks: *דנה אמרה: "אני רעבה."* A quotation mark never stands
  between two letters (it would read as gershayim) and a single quotation mark is never used (it
  would read as a geresh). No maqaf: write *בית ספר*, *יום הולדת* with a space, as everyday Hebrew
  does (the reader parts words at a maqaf anyway). The questions and answers of the final `שאלות:`
  block are plain lines, as above.
- **Numbers**: digits, in every course (*לפני 50 שנה*, *בשעה 7:30*, *3 ילדים*). Spell a number out
  only when the number word is a word you teach or one already in range (*בית אחד*, *שני ילדים*,
  *שלושה ימים*), with the gender right (*שלוש בנות*, *שלושה בנים*).
- **Names**: common Israeli first names as characters (see the list below), never a name that is a
  list word, and names are never taught as words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, prophets, the Bible, synagogue, church and mosque, prayer, fasting,
  the commandments, religious customs and dress, the rabbi and the priest, and holidays of religious
  origin (Pesach, Rosh Hashana, Yom Kippur, Sukkot, Hanukkah, Purim, Shavuot, Christmas, Easter,
  Ramadan); and the religious formulae of everyday speech, which are never written, not even in
  dialogue: *ברוך השם*, *בעזרת השם*, *אם ירצה השם*, *תודה לאל*, *חס וחלילה*, *שבת שלום*, *חג שמח*,
  *שנה טובה*, *מזל טוב* (write *כל הכבוד*, *ברכות*), *אמן*. A birthday (*יום הולדת*) is fine; *חג* is
  taught only as a generic holiday or vacation (*חופשת החג*), never a named one. Saturday (*שבת*) is a
  day of the week like any other, the day off, never a day of rest or prayer; Friday dinner is a family
  dinner, nothing more;
- **politics**, the state, the Knesset, elections, parties, ministers, presidents and kings, borders,
  Zionism, nationalism and the region's conflicts; everyday civic life (a library, a school, the
  municipality, a residents' meeting, a committee of parents) is fine;
- **war, the army and military service, weapons, violence, crime, police and courts** (a traffic warden
  or a lost-property office is fine; soldiers, reserve duty, arrests, prisons and trials are not);
- **death**, killing, funerals, mourning, the Holocaust, blood and injury beyond a scraped knee;
- **vulgar or sexual words**, and words whose main sense is sexual (*מין* "sex", *מיני*);
- **alcohol** (wine, beer, bars), **gambling** (casinos, lotteries, betting), drugs and smoking as a
  topic;
- **nationalities and peoples** (*יהודי*, *ישראלי*, *ערבי*, *אמריקאי*): a language is taught as the
  language (*עברית*, *אנגלית*), never as a people's adjective;
- **person names taught as words**: a band word that is a name, a part of names or a name's
  derivative is skipped.

Skip band words that only fit these topics, and say so in your report. The web-based list ranks
many of them high; those of the first 1000 ranks are:

- religion: *יהודי* 80, *תורה* 289, *רבי* 374, *כוהן* 523, *דתי* 537, *אלוהים* 576, *נס* 680, *הלכה*
  763, *דת* 770, *מצווה* 774, *תפילה* 933, *ברית* 400; *שבת* 269 only as Saturday, *חג* 525 only as a
  generic holiday, *רב* 75 only as "much, many, great" (never through *רבנים*), *ברוך* 968 only in
  *ברוך הבא* "welcome", *ברכה* 881 only as a greeting or good wishes;
- politics, war, crime: *מדינה* 83, *חוק* 121 only as an everyday rule (*חוקי המשחק*), *מלחמה* 170,
  *ממשלה* 173, *דין* 187, *כנסת* 261, *לאומי* 319, *נשיא* 381, *צבא* 421, *משטרה* 425, *פוליטי* 458,
  *חייל* 469, *קרב* 471 only as "came near" (*קרב*, *התקרב*), *מפלגה* 493, *שלטון* 552, *ביטחון* 563 only
  as "confidence" (*ביטחון עצמי*), *הגנה* 618, *רצח* 631, *נשק* 673, *צבאי* 730, *ציוני* 746, *מנהיג*
  808, *שוטר* 814, *הרג* 819, *מדיניות* 838, *מאבק* 842, *מפקד* 868, *כלא* 882, *חקירה* 937, *נלחם* 961,
  *פשע* 965, *לוחם* 971; *שר* 209 only as "sings", never a minister; *משפט* 155 only as a sentence of
  words, never a trial; *שופט* 952 only as a referee in sport; *מבצע* 604 only as a sale or special
  offer; *כוח* 128 only as strength; *בחירה* 366 only as a choice;
- death: *מת* 243, *מוות* 375, *דם* 377;
- sexual, alcohol: *מין* 499, *מיני* 863, *יין* 893, *בר* 345;
- names and words from names of places and peoples: *תל* 242 (only in *תל אביב*, a name), *אביב* 208
  only as "spring", *ערבי* 257, *ישראלי* 271, *חיפה* 527, *ישראלית* 640, *אמריקאי* 665, *שלמה* 681,
  *לוי* 719, *סיני* 924, *דן* 457, *שרה* 892, *דוד* 297 only as "uncle", *יונה* 925 only as "dove",
  *ציון* 623 only as "grade, mark"; *ע״י* 476 and *ד״ר* 688 are abbreviations, never taught.

## The file

Write `tools/courses/he/<id>.json`:

```json
{
  "id": "he-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Hebrew, e.g. אוכל וקניות",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Hebrew",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after that `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/he/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *הולכת* for *הלך*, *ילדים* for *ילד*, *ביתה* for *בית*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the past
  tense of a verb, third person masculine singular (*הלך*, *רצה*, *דיבר*), the singular of a noun,
  the masculine of an adjective, the letters in front as letters (*ה*, *ו*, *ב*, *ל*, *מ*, *ש*, *כ*,
  *כש*).
- **Length** (story and questions together, counted as the checker cuts the words, see below):
  80–180 words per lesson in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380
  up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, a slang or foreign word, a list error such as a
  name or a form filed as a word of its own); the checker's last line lists the band words not
  taught yet.

Things to know about the Hebrew list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/he/wordlist.tsv`
  (a copy of the app's `he.tsv`); the checker reads it for the ranks, so a rebuilt app list does not
  move the bands. The `bands/*.txt` files come from it. It cuts the words with the app's own list and
  the he-en dictionary pack (`dictionaries/he-en.sqlite`, which must be present), as the reader does.
- **The letters in front are words.** The reader cuts the letters written onto a word's front: *ו*,
  then *ש*, *כש*, *מש* or *לכש*, then *ב*, *כ*, *ל* or *מ*, then the article *ה* (which *ב*, *כ* and *ל*
  swallow: *בבית* is *ב* + *בית*, "in the house"). Each piece is a word of its own, in the length too:
  *ובבית* is *ו* + *ב* + *בית*, three words; *מהבית* *מ* + *ה* + *בית*; *כשהוא* *כש* + *הוא*. The text
  is longer to the checker than it looks: count on about 1.3 words for each written word. These pieces
  are list words of band 1 (*ה* 1, *ב* 2, *ל* 3, *ו* 4, *ש* 5, *מ* 10, *כ* 29; *כש* 117), taught in the
  first course simply by using them, and known everywhere after. A word with a pronoun after it
  counts as its word (*ביתנו* as *בית*), a word whose *ו* is doubled after the letters in front as the
  word (*הוועדה* as *ה* + *ועדה*), and a name with letters in front (*ולדניאל*, *בהרצליה*) is known.
- **A word the list has whole is never cut**: *הלך*, *הגיע*, *הבין*, *הרבה*, *היום*, *הביתה*, *בית*,
  *בדק*, *לפני*, *לבד*, *לקח*, *למד*, *מים*, *מקום*, *משפחה*, *משהו*, *שלום*, *שאל*, *שמע*, *כבר*, *כסף*,
  *באמת*, *בגלל*, *כאילו*, *לכן* stay whole, and so do the infinitives (*להיות*, *לעשות*, *ללכת*), which
  are forms of their verbs, and the prepositions with their pronouns, which are forms of the
  preposition (*לי*, *לו*, *לנו*, *בו*, *בה*, *ממני*, *ממנו*, *שלי*, *שלו*, *כמוני*): *לי* counts as *ל*,
  *שלי* as *של*, *ממנה* as *מ*. A headword with a pronoun after it stays whole too (*ביתה* counts as
  *בית*). After *ו* a word of the list is not cut further (*ולפני* is *ו* + *לפני*). Run `cut.py` (see
  the writers' instructions) on any word you are not sure of.
- **What the list cuts**: *הזה* is *ה* + *זה*, *השנה* *ה* + *שנה*, *הבא* *ה* + *בא*, *שלא* *ש* + *לא*,
  *בזמן* *ב* + *זמן*, *לעולם* *ל* + *עולם*, *הלילה* *ה* + *לילה*, *במקרה* *ב* + *מקרה*. An infinitive
  spelled as *ל* and a noun is read as *ל* and the noun (*לנהג*, *לצבא*), and a rare infinitive the list
  lacks may be cut too: check it with `cut.py`, and write around it when it is cut.
- The list was built from web text, so politics and religion rank high and many everyday words rank
  later than you might expect: *אמא* 303, *אבא* 387, *שלום* 188, *תודה* 282, *אוכל* 283, *ילדה* 757,
  *חתול*, *קפה*, *לחם* far later. Check a word's rank rather than guess it. In the first band there is
  no "mother", "father" or "hello": use names, *אישה*, *איש*, *ילד*, *בן* and *בת* (148).
- The list files the forms of a word under its headword: the tenses and persons of verbs under the
  past (*הולך*, *ללכת*, *הלכה*, *ילך* under *הלך*), the infinitive too (*לעשות* under *עשה*, *לדבר* under
  *דיבר*), plurals under the singular, the feminine under the masculine, nouns with their pronouns. A
  form neither the list nor `extra_forms.tsv` has counts as an outside word. The forms are noisy:
  - participles are under their verbs (*יודע* under *ידע*, *רוצה* under *רצה*), but some are words of
    their own with a sense of their own (*עובד* "worker", *מורה* "teacher", *רואה* "accountant" beside
    "sees", *אומר*, *חוזר*, *נותן*, *עולה*, *קורא*, *כותב*, *מכיר*): count them by the list, not by grammar;
  - **homographs**: unpointed Hebrew writes many words alike, and a form counts for every word that
    has it: *ספר* "book" and "told, counted"; *דבר* "thing" and "spoke"; *שם* "there" and "name";
    *עם* "with" and "people"; *אם* "if" and "mother"; *בן* "son" and *בנו* "his son", "built", "in us";
    *חבר* "friend" and *חברה* "company, society, girlfriend"; *שנה* "year" and "repeated"; *אבל* "but"
    and "mourning"; *ערב* "evening" and "pleasant"; *שער* "gate" and "hair"; *מדבר* "desert" and
    "speaks"; *לבן* "white" and "to the son". Use a new word in its own sense, never through its
    homograph, and do not lean on a homograph's rank for coverage.
- **Names** go in `names` once, as written in the text; the checker also accepts them with letters in
  front (*ולדניאל*, *בהרצליה*), and a name of two words (*תל אביב*, *באר שבע*, *ניו יורק*) is matched
  whole. Many Israeli names are list words or are cut by the reader into letters and a word, and they
  cannot be used: *נועה*, *יעל*, *מיכל*, *דנה*, *תמר*, *עדי*, *דני*, *רון*, *עומר*, *טל*, *שי*, *נטע*,
  *אלון*, *אורי*, *גל*, *אור*, *דוד*, *שרה*, *רחל*, *חנה*, *קרן*, *אסף*, *נתן*, *אדם*, *יובל*, *אייל*,
  *עמית*, *הילה*, *שקד*, *שירה*, *מעיין*, *גיא*, *דור*, *שני*, *אליה*, and the names the reader cuts:
  *שרון* (*ש* + *רון*), *ליאור*, *משה*, *איתן*, *שמעון*, *טליה*, *נתניה*. Names that are safe: *דניאל*,
  *יונתן*, *מיכאל*, *אריאל*, *עידו*, *יוסי*, *אופיר*, *ערן*, *יואב*, *אבנר*, *בועז*, *ליאם*, *יהלי*,
  *ליאת*, *רונית*, *ענת*, *מרים*, *אסתר*, *רבקה*, *אביגיל*, *עדן*; check any other with `cut.py` (it
  must show `-`, outside the list, and stay one piece). Cities are mostly not list words (*הרצליה*,
  *רעננה*, *חולון*, *אשדוד*, *אשקלון*, *עכו*, *טבריה*, *צפת*, *אילת*, *כנרת*, *פריז*, *לונדון*, *ברלין*,
  *רומא*, *טורונטו*); *חיפה* is a list word (skipped: write another city), and *רחובות* is the plural of
  *רחוב* (avoid it). Keep only true names in `names`, never common words.
- The `שאלות:` header is not counted. Digits are numbers and count as known.
