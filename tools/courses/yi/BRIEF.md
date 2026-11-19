# Writing a Yiddish frequency course

Each course teaches one band of 100 words from the Yiddish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `yi-mini-0300` teaches ranks 201–300
and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's words, so
the course covers the band; a learner who finishes it knows the 300 most common words. `themes.tsv`
gives each course its theme. The list has 5,581 words: the courses are `yi-mini-0100` to
`yi-mini-5600`, 56 of them (the last band holds ranks 5501–5581).

The list was counted from Yiddish Wikipedia (Leipzig's sentences and the articles) and Tatoeba's
everyday sentences. Wikipedia's Yiddish is written largely by Hasidic writers in unpointed
spelling; the builder files those spellings under their YIVO words, so the list is in YIVO
spelling and its forms include the unpointed ones (פון under פֿון, האט under האָבן).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *מאָטל האָט אַ קאַץ. די קאַץ איז גרויס...* The Yiddish-speaking world, then and now:
  Vilne, Kovne, Warsaw, Lodz, Krakow, Bialystok, Odessa, Chernovits; New York (the Bronx,
  Williamsburg, Borough Park), Monsey, Montreal, Toronto, Buenos Aires, Melbourne, Antwerp, London,
  Paris, Jerusalem and Bnei Brak. Home, family, food, shopping, work, school, the town, health, free
  time, travel; any everyday, cultural or story topic is fine (WRITER-BRIEF step 4), Jewish life and
  its holidays included.
- **Nothing confusing**: no plot twists, flashbacks, irony or open endings. Events in the order
  they happen. Few characters (one to three), named clearly; say who *ער*/*זי* is whenever it could
  be unclear. Each lesson has its own people: one name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (the verb second: *הײַנט גייט פֿייגע צו דער באַן*). Present tense up to rank 500; the past (the
  perfect, *זי איז געגאַנגען*, *ער האָט געזען*) may tell a whole story from rank 600, and its common
  forms (*איז געווען*, *האָט געהאַט*, *איז געבוירן*) appear in simple uses from the first course.
  Little dialogue; when there is some, short and simple.
- **Growing difficulty**: the stories stay standalone and everyday, but sentences get longer and
  joined, dialogue can carry more of the story, and after rank 1000 a story may be told by a
  first-person narrator or as a letter or a diary entry. Grammar grows with the bands:
  - ranks 1–300 (A1): the articles *דער*, *די*, *דאָס* and *אַ*/*אַן*, *דעם* after a preposition
    (*מיט דעם קינד*, *אין דער שטאָט*) and the joined forms the list has (*צום*, *אויפֿן*), the present
    of common verbs in every person (*איך קום*, *דו קומסט*, *ער קומט*, *מיר קומען*), *זײַן* and
    *האָבן*, *ליב האָבן*, the modals with an infinitive (*וויל עסן*, *קען רעדן*, *דאַרף לערנען*,
    *זאָל גיין*), the possessives (*מײַן*, *דײַן*, *זײַן*, *איר*, *זייער*), the plural of common nouns
    (*קינדער*, *טעג*, *שטעט*, *מענטשן*), adjective endings after the articles (*אַ גרויסע שטאָט*, *אַ
    גוטער טאָג*, *דעם גרויסן*), negation with *נישט* and *קיין* (*ער האָט נישט קיין געלט*), questions
    by putting the verb first (*איז די קאַץ גרויס?*) or with *וואָס*, *וווּ*, *ווען*, *ווי*, *צי*;
  - ranks 301–600 (A2): the perfect of common verbs with *האָבן* and *זײַן*, the separable verbs (*ער
    קומט אָן*, *זי איז אָנגעקומען*), the dative pronouns (*מיר*, *דיר*, *אים*, *איר*, *אונדז*), *אַז*
    and *ווײַל* clauses, numbers with their nouns;
  - ranks 601–1000 (A2): stories told in the past, the future (*איך וועל גיין*), the imperative
    (*קום*, *קומט*, *לאָמיר גיין*), the comparative and superlative (*גרעסער*, *דער בעסטער*), ordinals,
    times and dates;
  - ranks 1001–1300 (B1): reflexive verbs (*זיך פֿרייען*, *זיך לערנען*), the verbal prefixes that
    change a verb's sense (*אַוועקגיין*, *צוריקקומען*, *אויפֿשטיין*), the diminutive (*הײַזל*, *ביכל*);
  - ranks 1301–1600 (B1): the conditional with *וואָלט* (*ער וואָלט געקומען*), relative clauses with
    *וואָס* (*דער מענטש וואָס איך האָב געזען*), reported speech with *אַז*;
  - ranks 1601–2000 (B1): the habitual past with *פֿלעגן* (*זי פֿלעגט קומען יעדן שבת*), participles
    as adjectives (*אַ פֿאַרמאַכטע טיר*), *כּדי צו*, *נאָכדעם ווי*, *איידער*;
  - ranks 2001–2500 (B2): the pluperfect (*ער איז געווען געקומען*), the passive with *ווערן* (*דאָס
    הויז איז געבויט געוואָרן*), the words of the Hebrew-Aramaic component in everyday use (*ממש*,
    *כּמעט*, *בערך*, *לכתּחילה*);
  - ranks 2501–3000 (B2): the unreal conditional (*ווען איך וואָלט געוווּסט, וואָלט איך געקומען*),
    *אויב* and *טאָמער*, *כאָטש*, *אַפֿילו*;
  - ranks 3001–4000 (B2): a more descriptive style, the feuilleton (the light column on everyday
    life), dialogue that marks *דו* and *איר* (the polite form);
  - ranks 4001–5600 (C1): written and press style: essays, portraits, reviews, calm news and feature
    writing, set expressions and proverbs used where they fit, the specialised words of trades,
    sciences and the arts, each made clear by its context.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper), as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally, and the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `פֿראַגעס:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  פֿראַגעס:
  וואָס האָט מאָטל?
  מאָטל האָט אַ קאַץ.
  וווּ לעבן שמואל און לאה?
  זיי לעבן אין ווילנע.
  ```

  The header is layout: the checker does not count it, and the app cuts the questions off at it.
- **Standard Yiddish**: neutral, correct YIVO Yiddish as the Forverts and graded readers write it,
  with all its agreements; no dialect spellings or forms (*איד* for *ייִד*, *דאס* for *דאָס*), no
  daytshmerish (*אונד* for *און*), no Hasidic Hebrew
  abbreviations (*וכו'*, *ז"ל*). Dialogue is everyday Yiddish too, simple and correct.
- **Original texts**: write your own stories; never copy or retell existing course material or
  literature (Sholem Aleichem, Peretz, Bashevis Singer...).

## Spelling: YIVO, written as the list writes it

The reader matches words exactly as written, points and all: *פֿון* and *פון* are two different
strings to it. Write every word in YIVO spelling with its points, as the list's headword:

- the pointed letters: *אַ* (pasekh alef), *אָ* (komets alef), *פּ* and *פֿ*, *בֿ*, *כּ*, *תּ*, *שׂ*,
  *וּ* after *ו* (*וווּ*, *פּרוּוון*), *ייִ* where YIVO writes it (*ייִד*, *ייִדיש*, *פֿריִער*);
- *ey* as two yods without a point (*קליין*, *איין*, *ביידע*), *ay* as the ligature *ײַ* with
  its pasekh (*זײַן*, *ווײַל*, *צײַט*), *וו* and *וי* as two letters (*וואָס*, *אויך*);
- words of the Hebrew-Aramaic component in their traditional spelling, with the points the list
  gives them (*משפּחה*, *שׂמחה*, *כּדי*, *חבֿר*, *ספֿר*, *תּמיד*);
- a point after its letter, never a presentation form (one character for *אַ*, *פֿ*, *שׂ*), and never
  the ligatures *װ*, *ױ* or a bare *ײ*: the checker flags all of these. Type the letter, then its
  point. When unsure, copy the word from the band file or check it with `rank.py`.

The unpointed spellings are in the list as forms, so the checker would count *פון* or *האט* as
known: do not lean on that. Write YIVO.

## Punctuation and numbers

- Full stop, comma, question and exclamation mark as in English; a comma before *אַז*, *ווײַל*,
  *ווען* clauses (*ער ווייסט, אַז...*). Speech in low-high quotation marks: *„איך בין דאָ!“*. Never a
  straight apostrophe or quote inside a word, except the *'ס* of a name (*מאָשע'ס*).
- Numbers as digits (*80 יאָר*, *פֿון 2 ביז 3*). No digit with a suffix (*10טן* puts *טן* in the
  text as an outside word): write the ordinal as a word when it is in range, or write around it.
- Hyphenated compounds take the maqaf *־* (*טאַטע־מאַמע*, *טאָג־טעגלעך*), as the list writes them.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Avoid only explicit sexual content, slurs used as
insults and gratuitous gore. Religious life, rebbes, holidays, history and the Holocaust are normal
topics for Yiddish. Teach ordinary words in their main sense; days of the week, months, seasons and
language names (*ייִדיש*, *ענגליש*, *העברעיִש*) are everyday words, not names: teach them.

## The file

Write `tools/courses/yi/<id>.json`:

```json
{
  "id": "yi-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["Names of people and places used in the texts"],
  "lessons": [
    {
      "title": "Story title in Yiddish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/yi/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93%
  up to rank 300, 90% for the first course). Names you list in `names` and numbers count as known.
  The checker lists every word outside the range with its rank.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson's story (any form counts: *קומט* for *קומען*, *קינדער* for *קינד*). `newWords`
  holds the headword as the list writes it: a verb's infinitive, a noun's singular, an adjective's
  base form.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240
  up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line
  lists the band words not taught yet.
- **Spelling**: presentation forms, the ligatures *װ ױ*, a bare *ײ*, *ייַ* and points out of order
  are reported as problems.

Things to know about the Yiddish list:

- The courses are pinned to `tools/courses/yi/wordlist.tsv`; `bands/*.txt` come from it and from
  `extra_forms.tsv` (present tenses, imperatives, adjective endings and irregular forms the
  Wikipedia counts lack).
- Wikipedia ranks religious and scholarly words high (*רבי* 30, *הרבֿ* 53, *רב* 78, *ישיבֿה* 120,
  *ספֿר* 154, the Hebrew months) and everyday words later than you might expect: *ווער* 126, *קליין*
  128, *הויז* 157, *זאָגן* 158, *איצט* 168, *טאַטע* 192, *יאָ* 197, *מאַמע* 248, *וויפֿל* 357, *ניין* 391,
  *זיידע* 468, *שטוב* 459, *באָבע* 1133, *פֿרעגן* 1092. Check a word's rank rather than guess it.
- One headword can hold two words of one spelling: *זײַן* is "to be" and "his" (*זײַנע* is its
  form); *זון* is "son" and "sun" (*זין* "sons"); *זייער* is "their" and "very". Teach the word in a
  sense its dictionary entry lists.
- *די* (2) and *דאָס* (7) share the forms *דער*, *דעם*, *די*: teaching both is accepted (the duplicate
  check reports it). *ווער* "who" (126) is a word of its own, so *איך ווער* counts for it, not for
  *ווערן*.
- **Names** go in `names` once, as written; the checker also accepts a name with the possessive
  *-ס*, *-עס* or *-'ס* (*חנהס*, *מאָטלס*), and a name of two words or with a maqaf (*בוענאָס־איירעס*,
  *ניו־יאָרק*) whole. Never use a name that is a list word: *ראָזע*, *פּערל*, *בערל*, *לײַבל*,
  *טעמע*, *פֿרומע*, *אײַזיק* are words. Safe: *משה*, *מאָטל*, *שמואל*, *יענקל*, *הערשל*, *מיכל*,
  *שלמה*, *ראובֿן*, *נחמן*, *יאַנקעוו*, *מוטקע*, *פֿײַוול*, *חנה*, *לאה*, *רחל*, *גיטל*, *פֿייגע*,
  *זלאַטע*, *רעכל*, *שיינדל*, *סאָרע*, *גאָלדע*, *ציפּע*, *עסטער*. Other names take the
  names' case endings only as above: write *צו פֿייגע*, not *צו פֿייגען*.
