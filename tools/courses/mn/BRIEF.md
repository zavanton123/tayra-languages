# Writing a Mongolian frequency course

Each course teaches one band of 100 words from the Mongolian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `mn-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 4,263 words, so there are 43 courses,
`mn-mini-0100` … `mn-mini-4300` (the last teaches ranks 4201–4263).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life in Mongolia: Ulaanbaatar and its districts, the aimag centres
  (Khovd, Ölgii, Ulaangom, Mörön, Arvaikheer, Sainshand, Mandalgovi, Baruun-Urt, Dalanzadgad,
  Zuunmod, Darkhan, Erdenet), the countryside (a ger, herders, the steppe, the Gobi, Lake
  Khövsgöl, Terelj), and now and then Mongolians in Seoul, Beijing, Moscow or Berlin: home, family,
  food, shopping, work, school, the city, health, free time, travel, Naadam and Tsagaan Sar.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, irony, wordplay or open endings.
  Events in the order they happen. Few characters (one to three), named clearly; say who *тэр* is
  whenever it could be unclear. One name is one character in a course.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  (subject, object, verb: *Дорж Ховд хотод ажилладаг*). Little dialogue; when there is some, in
  guillemets before *гэж хэлсэн* / *гэж хэлдэг* (*Номин: «Би хоёр хүүхэдтэй» гэж хэлдэг.*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–100 (A1): *бол* and the bare copula (*Дорж монгол хүн*), *байна*, *байдаг*, *байсан*,
    the habitual *-даг* (*ажилладаг*), the past *-сан* and *-аад*, *-ж*, the future *-на*, the
    negatives *-гүй* (*байдаггүй*, *гараагүй*), the genitive (*Доржийн ажил*, *хотын төв*), the
    dative-locative (*хотод*, *аймагт*), the ablative (*Ховдоос*), the comitative (*хүүхэдтэй*),
    the accusative (*хуулийг*), the reflexive possessive (*ажилдаа*, *ажлаасаа*), *нь* after a
    noun, *гэж хэлэх*, *тул* "because", *ч* "also, even", *л* "only". The question particles
    *уу/үү* (140) and *вэ/бэ* (168), *хэн* (359), *юу* (251), *хэдэн* (196) and *хаана* (889) rank
    later: each question of the first course costs one or two outside words, which the first
    course's 90% allows; ask with *ямар* (100) where you can;
  - ranks 101–300 (A1): the question particles, *та* (132) and *чи* (306), the imperative and
    the requests (*яв*, *ороорой*, *өгөөч*), *-маар байна* "want to", *чадах* (*хийж чадна*), the
    plural (*хүүхдүүд*, *иргэд*), *-х* participles before a noun (*явах зам*), *хэрэгтэй*;
  - ranks 301–1000 (A2): the converbs *-вал/-бал* "if", *-хад* "when", *-тал* "until", *-саар*,
    the participle clauses (*миний уншсан ном*), reported speech with *гэж*, *-х ёстой*,
    comparisons with the ablative (*Ховдоос их*), the ordinals (*хоёрдугаар*);
  - 1000 and up (B1–C1): the passive and causative verbs (*байгуулагдах*, *оруулах*), *-ж
    байгаа*, *-хаар*, *-тай холбоотой*, the formal written style of news and letters where the
    theme wants it.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Асуултууд:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  Асуултууд:
  Дорж ямар хотод байдаг вэ?
  Дорж Ховд хотод байдаг.
  ```

  The header `Асуултууд:` is layout: the app and the checker do not count it as a word.
- **Standard Khalkha in Cyrillic**, as Mongolian newspapers and schoolbooks write it: *ө* and *ү*
  (never *о*/*у* or Latin look-alikes), *й* and *ь* as the dictionary has them (*ярьсан*, *бий*),
  no traditional script, no Latin transliteration, no slang.
- **Punctuation**: comma, full stop, question and exclamation marks; speech in guillemets «…».
- **Numbers**: digits for years, times, counts and ages (*2010 онд*, *8 цагт*, *21 аймаг*, *3 500
  000 хүн*); spell a number out only when the band teaches it or it is already in range (*хоёр*,
  *нэг*).
- **Names**: Mongolian names that are no list word (*Дорж*, *Номин*, *Билгүүн*, *Уянга*, *Отгон*,
  *Ганбат*, *Дулмаа*, *Тэмүүлэн*, *Ганзориг*, *Мөнхбат*, *Сарнай*, *Энхжин*, *Болормаа*, *Батбаяр*,
  *Нандин*, *Ану*); many names are words (*Бат*, *Болд*, *Оюун*, *Цэцэг*, *Туяа*, *Баяр*, *Энх*,
  *Мөнх*, *Хулан*, *Тунгалаг*, *Даваа*): never use those. Places too: *Улаанбаатар* (414),
  *Дархан* (1057), *Эрдэнэт* (a form of *эрдэнэ*), *Мөрөн*, *Булган*, *Цэцэрлэг* and *Зайсан* are
  list words, so they count as words of their rank, never as names; *Ховд*, *Өлгий*, *Улаангом*,
  *Арвайхээр*, *Сайншанд*, *Мандалговь*, *Зуунмод*, *Баянхонгор*, *Сэлэнгэ*, *Тэрэлж* are names.
  A name takes the case endings joined (*Доржийн*, *Ховдоос*, *Ануг*): the checker counts those
  as the name.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

Follow step 4 of `tayra-writers/WRITER-BRIEF.md` (2026-11-13: no topic rules). Any normal
everyday, cultural, historical or story topic is fine. Only avoid explicit sexual content, slurs
used as insults and gratuitous gore. Teach ordinary words in their main sense.

## The file

Write `tools/courses/mn/<id>.json`, the shape of `mn-mini-0100.json`: `id`, `rankUpTo`, `level`,
`title` (from `themes.tsv`), `description` (one English sentence), `topic`, `names` (people and
places used in the texts, never a list word), and ten `lessons`, each with `title`, `summary`
(one English line), `newWords` (the band words it introduces, as the list writes them: a verb's
dictionary form in *-х*, a noun's nominative) and `text` (paragraphs separated by a blank line,
each question and answer on its own line).

Levels as in `themes.tsv`: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`,
then `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/mn/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93%
  up to rank 300, 90% for the first course). Names in `names` and numbers count as known.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ажилладаг* for *ажиллах*, *хотод* for *хот*).
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240
  up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 above.
- **Coverage of the band**: aim to teach at least 90 of the band's words; the checker's last line
  lists the band words not taught yet.

## Things to know about the Mongolian list

- The courses are pinned to `tools/courses/mn/wordlist.tsv` (a copy of the app's `mn.tsv`, built
  from the Leipzig Mongolian news and Wikipedia corpora, bounded by the mn-en, mn-ru and mn-zh
  dictionaries). Its forms come from regular paradigms the builder generates: every noun's cases,
  plural and reflexive possessive, every verb's tenses, participles, converbs and agent noun in
  *-гч*. A causative or passive verb that no dictionary has is filed under its verb
  (*хөгжүүлэх* under *хөгжих*, *нэмэгдүүлэх* under *нэмэгдэх*): it is a use of that verb.
- The list is built from news and Wikipedia, so its top is formal (*улс*, *засаг*, *иргэн*, *аймаг*,
  *хууль*, *төр*) and some everyday words rank late (*гэр* 130, *ээж* 546, *аав* 550, *эхнэр* 869,
  *нохой* 1378). Write stories the bands can carry; a cost of a few outside words is normal.
- Particles that change with vowel harmony are one word: *уу*/*үү*/*юү*, *вэ*/*бэ*, *даа*/*дээ*/*доо*/*дөө*,
  *руу*/*рүү*/*лүү*, *дэх*/*дахь*. A number's attributive form is the number (*гурван* of *гурав*).
- Skip list noise: names and titles (*чингис*, *улаанбаатар*, *богд*, *байгал*), the
  multi-word entries (*засгийн газар*, *элчин сайд*, *гэр бүл*: the checker counts their words
  one by one) and the rare homographs a common word's form made (*авс*, *эрхий*, *дун*, *монголч*).
- Past rank 3800 the list was rebuilt without that noise: it has no multi-word entries, leaves out
  the names the texts capitalise (places, people, gods, titles, peoples), and files a verb's
  participles and converbs (*хуурсан*, *бололгүй*) and the pack's form entries under their words.
  What is left there is rare but real: plain things, animals, plants, tools, food, science words.
