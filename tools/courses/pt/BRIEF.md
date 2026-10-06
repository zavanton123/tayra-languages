# Writing a Portuguese (Brazil) frequency course

Each course teaches one band of 100 words from the Portuguese word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `pt-freq-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## The file

Write `tools/courses/pt/<id>.json`:

```json
{
  "id": "pt-freq-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "Course title in Portuguese",
  "description": "One or two sentences in English: what the story is about.",
  "topic": "A short English topic, e.g. School life",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Lesson title in Portuguese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The text. Paragraphs separated by a blank line (\n\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/pt/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *disse* for *dizer*). Spread them naturally; reuse earlier
  lessons' new words too.
- **Length**: 50–130 words per lesson in the first course, 80–180 up to rank 300, 130–240 up to
  600, 170–300 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a plural or a
  name); the checker's last line lists the band words not taught yet.

Run the checker after every change, until every lesson passes.

## Making it worth reading

- **One story per course**: the same characters across all ten lessons, each lesson a chapter
  with something happening, and a small hook or surprise at the end that makes one want the next.
- **Natural Brazilian Portuguese**: how people really speak and write in Brazil (você, a gente,
  pra in dialogue where natural), written in Portuguese from the start, never translated from
  English. Set in Brazil, with real places and everyday life.
- **Not a textbook**: no "Ana é uma menina. Ela tem uma casa." chains, no lists of vocabulary
  disguised as sentences, no morals. Dialogue, feelings, humour, a little tension. Vary sentence
  openings; repeat words because the story needs them, not mechanically.
- **Grammar to match the level**: present tense and simple past early on, longer sentences and
  more tenses as the courses go on.
- Keep it suitable for all ages: no violence beyond a crime mystery, nothing sexual.
