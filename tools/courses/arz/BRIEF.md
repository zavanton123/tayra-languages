# Writing an Egyptian Arabic frequency course

Each course teaches one band of 100 words from the Egyptian Arabic word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `arz-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list is small: 595 words, so there are six courses,
`arz-mini-0100` … `arz-mini-0600` (the last teaches ranks 501–595).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *محمود ما عرفش ياخد ايه. هو لف ولف عند الجامع، ورجع لابوه...* Everyday life in
  Egypt: Cairo (Shubra, Maadi, Zamalek, Heliopolis, downtown, Tahrir Square), Giza, Alexandria,
  Aswan, Luxor, Port Said, Suez, Ismailia, Mansoura, Tanta, Zagazig, Minya, Asyut, Sohag, Fayoum,
  Siwa, Hurghada, Sharm el-Sheikh, a village in the Delta or on the Nile; and now and then
  Egyptians in Kuwait, Dubai, London or New Jersey: home, family, food, shopping, work, school, the
  city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *هو*/*هي* is whenever it could be
  unclear. Each lesson has its own people: one name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object: *سامح قال لابوه*, *ليلى بتكتب لابوها كل شهر*). Egyptian tells events in
  the past (*راح*, *قالت*, *جم*) and habits and what is going on with the *ب*-present (*بيروح كل
  يوم*, *بتكتب*); both are fine from the first course. Little dialogue; when there is some, short
  and simple, with *يا* + name (*يا سلمى*), *انت*/*انتي* among family and friends; *حضرتك* is not a
  list word, so address a stranger or an older person with *يا استاذ* (56), *يا مدام* (317), *يا
  باشا* (80) or *يا دكتور* (54).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–100 (A1): nominal sentences with no verb (*منى بنت صغيرة*, *الشركة في ميدان كبير*,
    *ده كتاب كبير*), *ده*, *دي*, *دول* before a noun and after a noun with *ال* (*الكتاب ده*, *السنة
    دي*), the article and the indefinite noun, gender and the agreement of adjectives (*بنت
    صغيرة*, *ولد كبير*), the attached pronouns on nouns, *عند* and prepositions (*اسمها*, *ابوه*,
    *عندها*, *ليه*, *ليك*, *عليه*), *عند* + pronoun for "to have" (*عندها 6 سنين*), *بتاع*, *بتاعة*,
    *بتوع* for "of, belonging to" (*الكتاب بتاع كريم*), the past of the third persons (*راح*, *راحت*,
    *راحوا*) and the *ب*-present (*بيروح*, *بتقول*, *بيشتغلوا*), *مش* before a noun, an adjective or a
    *ب*-present (*مش كبير*, *مش بتروح*), *ما ...ش* with a past (*ما قدرتش*, *ما عرفش*), *لو* for
    "if", *عشان* for "because", questions with *ايه* and *ليه*. The other question words rank
    later (*مين* 125, *كام* 166, *ازاي* 167, *فين* 229, *امتى* 383): a few may cost a word each, and
    *ايه* serves for most questions (*حسن بيشتغل في ايه؟*, *الست عندها ايه؟*). *انا* 119, *انت* 204
    and *احنا* 260 come later too: tell the story in the third person;
  - ranks 101–300 (A1): every person of the past and the present (*رحت*, *رحنا*, *بروح*, *بنروح*,
    *بتروحي*), the pronouns *انا*, *انت*, *انتي*, *احنا*, *هم* (*هما*), the imperative (*روح*, *تعالى*,
    *خد*, *اقعدي*, *حطوا*), the future with *ه* (*هيروح*, *هنروح*, *هتعملي ايه؟*), *عايز* + present
    (*عايزة تروح*), *يقدر* + present (*مش قادر يروح*); *لازم* is not a list word: write around it
    (*عايز*, *عشان*); *كان* + *ب*-present for habits (*كان بيروح كل يوم*);
    the plural of nouns and adjectives (*ولاد*, *بنات*, *كبار*, *كويسين*); the dual in *-ين* (*يومين*,
    *شهرين*, *سنتين*); *اللي* clauses (*الست اللي عند الجامع*);
  - ranks 301–600 (A2): the comparative and superlative (*اكبر من*, *أحلى حاجة*, *أغلى*), reported
    speech with *إن* (*قالت إن الشقة غالية*), time with *بعد ما* (*قبل*, *لما* and *لحد* are not list
    words), the negation in every person with an object (*ماشفتوش*,
    *ماقالتلهاش* — keep these short), the numbers as words where a band teaches them (*حداشر* …
    *تسعتاشر*, *عشرين* … *تسعين*, *تلاتة* … *تسعة*), the ordinals (*تالت*, *رابع*, *خامس* …
    *عاشر*), *كان* + past for the past perfect (*كان خرج*), *يمكن* "maybe", *جايز*, *يادوب*,
    *ياريت*, *ياترى* and the other everyday phrases of the bands.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*هو لف ولف عند الجامع*).
- **Questions at the end**: after the story, a paragraph starting `أسئلة:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words:

  ```
  أسئلة:
  ابو محمود بعت محمود ياخد ايه؟
  ابو محمود بعت محمود ياخد جزر.
  الست عندها ايه؟
  الست عندها نوعين جزر: نوع صغير ونوع كبير.
  ```

  The header `أسئلة:` is layout: the checker does not count it as a word.
- **Egyptian Arabic, not Standard Arabic**: the colloquial of Cairo (*العامية المصرية*) as Egyptians
  write it in messages, comics, graded readers and on Egyptian Wikipedia: *عايز*, *مش*, *ده*, *دي*,
  *بتاع*, *ازاي*, *فين*, *دلوقتي*, *النهارده*, *بكرة*, *كمان*, *لسه*, *بس*, *زي*, *عشان*, *شوية*, the
  *ب*-present and the *ه*-future. No MSA forms where Egyptian has its own (*ماذا*, *هذا*, *الذي*,
  *سوف*, *لم*, *ليس*, *يريد*, *أين*, *كيف*, *الآن*, *جدا*: write *ايه*, *ده*, *اللي*, *ه*-, *ما ...ش*,
  *مش*, *عايز*, *فين*, *ازاي*, *دلوقتي*, *اوي*), no other dialect (*شو*, *هيك*, *بدي*, *زين*, *وايد*),
  no slang beyond what any careful speaker says, no vulgar words. The list has some MSA and news
  words (*إذا* 304, *نعم* 402, *بلغ* 326, *حرر* 324, *هدد* 343, *احتجاج* 358, *أثبت* 245): teach them
  only in a sentence an Egyptian would say (*نعم؟* is "pardon?"; *إذا* is better skipped).
- **No vowel marks**: write the text unvowelled: no fatha, damma, kasra, sukun, tanween marks,
  shadda, dagger alif or tatweel. The reader keeps a mark inside the word, so a marked word is
  another word; the checker flags any mark.
- **Spelling: write a word as the list writes it.** The reader compares words letter by letter,
  hamza and dots included, so *إنت* and *انت*, *في* and *فى*, *مدرسة* and *مدرسه* are different words
  unless the list (or `extra_forms.tsv`) files both. The conventions that work with the list:
  - the Egyptian spellings, not the Standard ones: *تلاتة*, *تمانية*, *تاني*, *تالت*, *دهب*, *كدب*,
    *ده*, *ضهر*, *كويس*, *عايز*, *جه*;
  - *ي* with its dots at the end of *في*, *اللي*, *تاني*, *مصري*, *ازاي*, *دلوقتي* (never *فى*: the list
    lacks it); *ى* where the list has it: *على*, *حتى*, *بقى*, *مشى*, *اشترى*, *استنى*, *امتى*, *إدى*,
    *تعالى*, *ياترى*;
  - *ة* for the feminine (*مدرسة*, *صغيرة*, *عربية*), not *ه*;
  - the pronouns and the first person of the present without hamza, as the list has them: *انا*,
    *انت*, *انتي*, *احنا*, *اروح*, *اكتب*, *اقدر*, *اعمل*; but *أول*, *أكل*, *أم*, *أرض* with it;
  - *ابو* "father of" (*ابوها*, *ابو سلمى*) and *اخ*, *اخت*, *اخو* without hamza; *أخت*, *أخو* also count;
  - *إن* "that" (*قال إن*); *ان* counts too;
  - the negation: *مش* apart (*مش كبير*, *مش بيروح*), and *ما* apart before a past with *ش* (*ما قدرتش*,
    *ما عرفش*); the joined forms (*ماقدرتش*, *مقدرتش*, *مبيعرفش*) count as well, but keep one way
    through a course.
- **Punctuation**: the Arabic comma ، and question mark ؟, the full stop and the exclamation mark.
  Speech follows a colon in guillemets: *سامح قال: «عيني تعبانة.»* The questions and answers of
  the final `أسئلة:` block are plain lines, as above.
- **Numbers**: Western digits (*عنده 6 سنين*, *20 متر*, *سنة 1967*), never the Eastern ones (٣). Spell
  a number out only when it is a word you teach or one already in range (*كرسي واحد*, *اتنين صحاب*),
  with the Egyptian counted noun (*3 سنين*, *11 سنة*, *2 كيلو*).
- **Names**: common Egyptian first names as characters, never a name that is a list word (see
  below), and names are never taught as words.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

Follow step 4 of `tayra-writers/WRITER-BRIEF.md` (2026-11-13: no topic rules). Any normal
everyday, cultural or story topic is fine, religion and holidays included (*الجامع* 57, *رمضان*,
*العيد*, Christmas and the Coptic feasts), history and the war years (*حرب* 81, *مات* 22, *قتل*
86), crime and detective stories, love and kisses (*باس* 198), beer (*بيرة* 279). Only avoid
explicit sexual content, slurs used as insults and gratuitous gore. Teach ordinary words in their
main sense.

## The file

Write `tools/courses/arz/<id>.json`:

```json
{
  "id": "arz-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Egyptian Arabic, from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Egyptian Arabic",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 600 `A2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/arz/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93%
  up to rank 300, 90% for the first course). Names you list in `names` and numbers count as known.
  The checker lists every word outside the range with its rank: replace them, or keep a few when
  the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *قالت* for *قال*, *ولاد* for *ولد*, *اسمها* for *اسم*,
  *والكتاب* for *كتاب*). `newWords` holds the word as the list writes it: the past of a verb (*قال*,
  *راح*, *اشتغل*), the singular of a noun, the masculine of an adjective, the letters in front as
  letters (*ال*, *و*, *ل*, *ب*, *ك*).
- **Length** (story and questions together, every word as the reader has it, see below): 80–180
  words per lesson in the first course, 120–240 up to rank 300, 180–320 up to 600.
- **Coverage of the band**: aim to teach at least 90 of the band's words. Skip a word that is not
  worth a lesson (list noise, a name, a form filed as a word of its own, a duplicate entry); the
  checker's last line lists the band words not taught yet.

## Things to know about the Egyptian list and the checker

- The courses are pinned to `tools/courses/arz/wordlist.tsv` (a copy of the app's `arz.tsv`, built
  from the Leipzig Egyptian Wikipedia corpus and bounded by the arz-en dictionary's 1,269 entries);
  `bands/*.txt` come from it and from `extra_forms.tsv`, which adds the verbs' persons and the
  plurals and feminines from the dictionary pack (`dictionaries/arz-en.sqlite`) and by hand.
- **One word is one word.** The app reads Egyptian Arabic as space-delimited text (not with the
  Arabic parser that cuts Standard Arabic): *والكتاب*, *بالليل*, *للبيت*, *ماقالهاش* are one word each,
  in the length too. A word the list lacks counts as a use of the list's word inside it, at the
  rank of its rarest piece: the letters in front at their own ranks (*و* 1, *ف* 12, *ال* 59, *ل* 89,
  *ب* 105, *ك* 473, and *لل* = *ل* + *ال*, *بال* = *ب* + *ال*), the verb's *ب*, *ه* or *ح*, the negation
  *ما ...ش* and the pronouns after a word free. So *والكتاب* counts at 74 (*كتاب*) and is a use of *و*,
  *ال* and *كتاب*; *بالليل* counts at 153 (*ليل*), *بالكتاب* at 105 (*ب*); *مدرسته* is *مدرسة*, *ابوها* is
  *ابو*, *عليا* is *على*, *قالتله* is *قال*, *ما قدرتش* is *ما* + *قدر*. In the first course *ب* as "with,
  in" (105) is outside: write *في* (*في الليل*) or wait for the second course. `cut.py` shows how any
  word counts.
- **Many everyday words are not in the list at all**, because the dictionary behind it lacks them;
  each costs a word every time: *مع*, *بيت*, *ناس*, *حد*, *كتير*, *لازم*, *ممكن*, *يعني*, *لكن*, *أو*,
  *برضه*, *دايما*, *لما*, *قبل*, *لحد*, *جديد*, *صعب*, *قريب*, *بعيد*, *آخر*, *جوه*, *ورا*, *طب*, *أهو*,
  *شاي*, *شارع*, *صاحب*, *ابن*, *قام*, *فضل*, *لقى*, *ساعد*, *بحر*, *أسبوع*, *تليفون*, *موبايل*, *غير*, *نفس*,
  *وبعدين*. Use the list's words instead where Egyptian has one: *ويا* 344 "with" (*ويا ابوه*),
  *بس* "but", *ولا* 127 "or", *جنب* 107, *قدام* 131, *برا* 133, *فوق*, *تحت*, *عند*; *كده* counts as
  *ك* + *ده* (473); *اللي عند الجامع* for "the one near the mosque"; *عمارة* 380, *شقة* 488, *أوضة* 335
  for "house, flat, room".
- **The list's faults**: it is mostly real Egyptian, but some entries are noise and some forms are
  filed under the wrong word. Skip these band words (say so in your report): *جون* (a name), *الله*
  (except where a story needs "God"), *باى*, *كلة* (its forms are *كله*), *نجيب*, *باكو*, *لسا* (the same
  word as *لسه* 95), *عاز* (the same as *عايز* 110), *لب*, *كول*, *تك*, *مت*, *يوزر*, *كش*, *لنج*, *رامي*,
  *مطروح*, *ناو*, *وسط البلد* (two words: the checker cannot count it), *تيل*, *حنك*, *أنبا*, *أفاتار*,
  *امبو*, *فش*, *أمتع*, *متع*, *قلش*, *حدا*, *طاش*, *نبيت*, *سكس*, *كس*, *مزة*. Forms filed under another
  word count as that word, so never teach the word through them and do not lean on them for
  coverage: *بابا* is a form of *ماما* 132, *سوق* "market" of *ساق* 173 "drive", *برد* "cold" of *رد*
  165, *اتعلم* "learn" of *علم* 68 "flag, science", *جمال* of *جمل* 139, *والد*, *ولادة* and *مولود* of
  *ولد* 92, *نية* of *انا*, *أرواح* of *راح*, *خدود* of *خد*, *سنان* of *سنة*, *أبعاد* of *بعد*, *حمى* of *حما*
  172, *ميت* of *مية* 196, *جواز* of *جوز* 197, *تجنب* of *جنب* 107, *اكمام* of *كم* 101, *الف* of *لف* 90,
  *بعتوا* of *باع* 175, *نجد*, *بجد* and *يجد* of *جد* 134, *أسقف* of *سقف*, *الم* of *لم*, *كله* of *كلة* 117
  (write *كلها*, *كلهم*, *كل حاجة*), and *البلد* and *وسط* of *وسط البلد* 435 (*البلد* counts at 435: write
  *بلده*, *بلد كبيرة*, *في البلد دي* only from the fifth course). *ضحكة* 291 "a laugh" holds the verb's
  forms (*ضحكت*, *بيضحك*): teach it as "laugh", noun and verb; both count. Real words that share forms are a pair: *ورق* 246 / *ورقة*
  495 (*أوراق*), *طريق* 47 / *طرقة* 496 (*طرق*), *اتمتع* 482 / *استمتع* 518.
- **Homographs**: unvowelled Arabic writes many words alike, and a form counts for every word that
  has it: *ست* "woman, lady" and "six"; *مارس* "March" and "to practise"; *علم* "flag" and
  "science"; *دور* "floor, storey" and "role, turn"; *مية* "water" and "hundred"; *قطر* "train" and
  "Qatar"; *مرة* "time, once" and "woman" (*مراته* "his wife"); *جد* "grandfather" and "seriously";
  *خد* "take!" and "cheek"; *عين* "eye" and "spring"; *سبع* "seven" and "lion"; *رجل* "leg" and "man";
  *بيه* "bey, sir" and "with it". Use a new word in its own sense, never through its homograph.
- **Names** go in `names` once, as written in the text, with *ال* if it has one (*القاهرة*,
  *الإسكندرية*); the checker also accepts them with letters in front, at the letters' rank (*والقاهرة*
  and *لسارة* in every course, *بأسوان* at 105 from the second), and a
  name of several words (*المتحف المصري*, *شرم الشيخ*) counts as one known word where it is written whole.
  Never put a list word in `names`, not even inside a multi-word name. Names that are list words, or
  that the checker reads as one: *علي* (*على*), *دينا*, *هاني*, *مجدي*, *نجيب*, *رامي*, *دهب*, *مطروح*,
  *المنيا*, *قطر*; check any other with `name_check.py`. Safe: *منى*, *كريم*, *حسن*, *سلمى*, *ياسين*,
  *طارق*, *عمرو*, *هند*, *ليلى*, *سامح*, *سارة*, *شريف*, *عزة*, *محمود*, *فاروق*, *تامر*, *سمير*, *وليد*,
  *رانيا*, *يوسف*, *مريم*, *أحمد*, *محمد*, *عمر*, *نور*, *ياسمين*, *شيماء*, *نادية*, *هدى*,
  *إيمان*, *مينا*, *خالد*, *عادل*, *صلاح*, *سعاد*, *حسين*, *فاطمة*, *زينب*, *مها*, *أمل*, *نبيل*,
  *بسمة*, *هالة*, *رحاب*, *عبير*. Cities: *القاهرة*, *الإسكندرية*, *الجيزة*, *أسوان*, *الأقصر*, *المنصورة*,
  *طنطا*, *بورسعيد*, *السويس*, *دمياط*, *أسيوط*, *سوهاج*, *الفيوم*, *الغردقة*, *شبرا*, *المعادي*,
  *الزمالك*; *مصر* is a name (not a list word: *مصري* 50 is). A kunya counts in two parts: *ابو
  محمود* is *ابو* (48) and the name.
- The `أسئلة:` header is not counted. Digits are numbers and count as known.
