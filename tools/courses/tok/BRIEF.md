# Writing a Toki Pona frequency course

Toki Pona's whole vocabulary is the list: 176 words from lipu Linku's usage survey (`wordlist.tsv`,
pinned). There are two courses: `tok-mini-0100` teaches ranks 1–100 (the core words, used by nearly
every speaker) and `tok-mini-0200` teaches ranks 101–176 (the rest of the pu words, then the words
of ku and the newer, rarer words). Course `tok-mini-0200` may use every word of the list. Ten
lessons each; a learner who finishes both knows the whole language. `themes.tsv` gives each course
its theme; `bands/0100.txt` and `bands/0200.txt` (76 lines) list the band words (index, rank, word;
the forms column is empty: Toki Pona words never change).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories, not one serial story; a lesson never needs another lesson
to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *jan Simon li alasa e soweli. ona li lukin lon insa tomo lili. soweli li lon ala.
  ona li lukin lon monsi tomo...* Toki Pona has no country: its speakers live everywhere, so the
  stories happen anywhere, named in Toki Pona's own forms: *ma Kanata*, *ma Mewika*, *ma Nijon*,
  *ma Kanse*, *ma Tosi*, *ma Epanja*, *ma Italija*, *ma Pasila*, *ma Mesiko*, *ma Inli*,
  *ma Sumi*, *ma Netelan*, *ma Oselija*. Home, family, food, shopping, work, school, the town,
  health, free time, travel, a Toki Pona club; any everyday, cultural or story topic is fine
  (WRITER-BRIEF step 4).
- **Nothing confusing**: no plot twists, flashbacks or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Toki Pona has one pronoun *ona* for he,
  she, it and they, so repeat the name (*jan Mali li...*) whenever *ona* could point at two
  people. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence. Little dialogue; when
  there is some, short and simple.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the
  story's key sentence pattern several times (*ona li lukin lon insa tomo. soweli li lon ala. ona
  li lukin lon monsi tomo. soweli li lon ala.*).
- **Questions at the end**: after the story, a paragraph starting `wile sona:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  wile sona:
  jan Simon li alasa e seme?
  ona li alasa e soweli.
  soweli li lon ala lon insa tomo lili?
  soweli li lon ala.
  ```

  The header `wile sona:` is layout: the checker does not count it as a word, and the app cuts the
  questions off at it.
- **Original texts**: write your own stories; never copy or retell existing material (pu, ku,
  lipu Linku's examples, jan Misali's or any course), and not the other languages' courses.

## Toki Pona as the courses write it

The standard is the language of pu (*Toki Pona: The Language of Good*, Sonja Lang, 2014) as the
community writes it today:

- **Lowercase**: every sentence starts with a small letter (*mi moku. jan Lisa li lape.*); only
  names take a capital. The checker fails a list word written with a capital, because a capital
  is how it knows a name. Titles are lowercase too.
- **li** comes before the predicate, except when the subject is *mi* or *sina* alone: *mi moku*,
  *sina pona*, but *ona li moku*, *jan Lisa li moku*, *mi mute li moku*, *mi en sina li moku*. A
  second predicate takes its own *li* (*ona li moku li lape*).
- **e** comes before each direct object (*ona li moku e kili e telo*); there is no *e* after
  *lon*, *tawa*, *tan*, *kepeken*, *sama* used as prepositions (*ona li lon tomo*, *ona li tawa
  ma*, *ona li pali kepeken ilo*).
- **Modifiers follow the head**: *tomo lili* (small house), *tomo mi* (my house), *jan pona*
  (friend), *tomo moku* (kitchen), *tomo esun* (shop), *telo suli* (sea, lake), *kasi suli*
  (tree), *jan lili* (child), *tenpo suno* (day), *tenpo pimeja* (night). **pi** groups two or more
  modifiers into one (*tomo pi jan Lisa*, *palisa pi kasi suli*); never *pi* before a single word.
- **la** sets the context, time first: *tenpo suno ni la, ...* (today), *tenpo pini la, ...* (in the
  past), *tenpo kama la, ...* (later, in the future), *lon nasin la, ...*; Toki Pona has no tenses,
  so time is given this way only when it matters.
- **o** calls someone (*mama o, lukin!*, *jan Lena o, sina pilin seme?*) and gives orders (*o kama
  moku!*, *o kute!*).
- **Preverbs**: *wile*, *ken*, *kama*, *awen*, *sona*, *lukin* (try to), *alasa* (try to), *open*,
  *pini* before a verb: *mi wile moku*, *ona li ken sitelen*, *moku li kama lete*, *ona li awen
  lape*, *ona li sona ala sitelen*.
- **Questions**: *seme* stands where the answer goes (*sina wile e seme?*, *ona li lon seme?*,
  *tan seme?* = why); a yes-no question doubles the verb with *ala* between (*sina wile ala wile
  moku?*, *ona li lon ala lon tomo?*) or ends in *anu seme?*; the answer repeats the verb (*wile* /
  *wile ala*).
- **Negation**: *ala* after the word it denies (*mi sona ala*, *soweli li lon ala tomo*).
- **Speech**: *ona li toki e ni: “...”* with typographic quotes “ ”, or in the second course *te
  ... to*. Each speaker's line in a paragraph of its own.
- **Numbers** are words, not digits: *wan*, *tu*, *tu wan*, *luka*, *mute*; *nanpa wan* is "first".
  In the second course *san* (three) and *ale* (all) join them.
- **Names are adjectives**: a name follows a head noun, never alone (*jan Sonja*, *ma Kanata*,
  *toki Inli* "English", *tomo Pawan* is wrong for "Pawan's house": write *tomo pi jan Pawan*).
  Names are written in Toki Pona's sounds: syllables of a consonant (j k l m n p s t w) and a vowel
  (a e i o u), with an optional final n; a name may start with a vowel; no *ji*, *ti*, *wo*, *wu*,
  and no *n* before *n* or *m* (*Sonja*, *Mija*, *Pawan*, *Tomasi*, *Kanata*, *Nijon*; not
  *Kristina*, *Towonto*). The checker counts every capitalised word that is not a list word as a
  name, and fails one not shaped as a Toki Pona word. List the names in `names` all the same.
  Never use a list word as a name (*Mama*, *Pona*, *Luka* are words: the checker fails them).
- **One word, one sense family**: Toki Pona's words are broad (*moku* eat and drink and food;
  *lukin* see, look, read, try to; *tawa* go, move, to, for). Teach a word in the sense the story
  makes plain, one of those Linku gives (`dictionaries/tok-en.sqlite`).

## Grammar by course

- **tok-mini-0100** (ranks 1–100): the core of the language: *li*, *e*, *mi*/*sina* without *li*,
  modifiers, *pi*, *la* with time and place, *o*, the preverbs, *lon*, *tawa*, *tan*, *kepeken*,
  *seme* and *X ala X* questions, *anu*, *en* (between subjects only), *taso* (but, only), *a*,
  numbers *wan*, *tu*, *luka*, *mute*, *nanpa*. Lessons 80–180 words with the questions; 90% of a
  lesson's words in ranks 1–100. Toki Pona words are short, so 180 words is less story than it
  looks: keep each story to one small situation.
- **tok-mini-0200** (ranks 101–176): everything, with *sama* as a preposition (*sama jan Lisa*),
  *kin* (also, indeed), *ale* (all, every: *tenpo suno ale*), *meli*/*mije*/*tonsi*, *te ... to*
  quotes, *san*, and the newer and rarer words, each made plain by its story. Lessons 120–240
  words; 93% of a lesson's words in the list.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Avoid only explicit sexual content, slurs used as
insults and gratuitous gore. Band words skipped for that or as variants:

- *unpa* 116: its sense is sex;
- *ojuta* 167: a vulgar meme ("ligma");
- *ali* 141: a spelling of *ale* 117 (a form of a taught word).

Every other word of both bands can be taught. The rarer words of band 101–176 are real words a part of
the community uses: teach them plainly (*kijetesantakalu* raccoon, *sutopatikuna* platypus,
*misikeke* medicine, *linluwi* network, internet), the playful ones in a fitting story (*wuwojiti*,
*kokosila*, *penpo*, *nimisin* in a Toki Pona club; *yupekosi* about a film made worse; *owe* about
Orwell's book). *pu*, *ku* and *su* mean "interacting with" the books *pu*, *ku* and the *su* story
books (*mi pu* "I read pu", *lipu pu*, *lipu ku*, *lipu su*). *oko* "eye" is a word of its own next to
*lukin*.

## The file

Write `tools/courses/tok/<id>.json`:

```json
{
  "id": "tok-mini-0100",
  "rankUpTo": 100,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["Lisa", "Kanata"],
  "lessons": [
    {
      "title": "story title in Toki Pona, lowercase",
      "summary": "One line in English on what happens.",
      "newWords": ["the band words this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Level `A1` for both courses. `names` holds the names only (*Lisa*, not *jan Lisa*).

## The rules, checked by `python3 tools/check_course.py tools/courses/tok/<id>.json`

- **Vocabulary**: at least 90% (first course) or 93% (second) of a lesson's words are ranked within
  the course's `rankUpTo`; names count as known. A word that is not Toki Pona is outside the list:
  write around it (*kafe* is not a word: *telo pimeja seli*).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier lessons' new words freely.
- **Length** (story and questions together): 80–180 words per lesson in `tok-mini-0100`, 120–240
  in `tok-mini-0200`.
- **Capitals**: a list word with a capital fails; a name not shaped as a Toki Pona word fails.
- **Coverage of the band**: teach every band word but the three skipped above; the checker's last
  line lists the band words not taught yet.
