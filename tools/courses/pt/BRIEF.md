# Writing a Portuguese (Brazil) frequency course

Each course teaches one band of 100 words from the Portuguese word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `pt-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Marta quer um gato. Ela vai a um abrigo. Ela vê três gatos...* Everyday
  life: home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "ele"/"ela" is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, common word order.
  Present tense up to rank 500; from rank 600 the story may be told in the simple past
  (pretérito perfeito / imperfeito). Little dialogue; when there is some, short and simple.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (relative clauses, *porque*, *quando*, *embora*,
  *no entanto*, *portanto*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail or diary entry. Grammar grows with the bands, each band
  using what came before:
  - ranks 1001–1300: all the past tenses with the perfeito/imperfeito contrast, the pretérito
    perfeito composto (*tenho trabalhado*), the future (*vou fazer*, *farei*) and the conditional
    (*faria*), relative clauses with *que*, *onde*, *quem*;
  - ranks 1301–1600: the present subjunctive (*quero que você venha*, *é importante que*,
    *embora seja*), commands, reported speech (*ela disse que estava cansada*), the passive
    (*a casa foi construída*);
  - ranks 1601–2000: the imperfect and future subjunctive (*se eu tivesse tempo*, *quando ele
    chegar*), conditional sentences, the pluperfect (*tinha saído*), the personal infinitive
    (*para eles entenderem*).
  - ranks 2001–2500: the compound tenses of the subjunctive (*espero que tenha gostado*, *se
    tivesse sabido*), the future perfect (*terei terminado*) and conditional perfect (*teria
    ido*), gerund clauses (*chegando em casa, ...*), the passive and impersonal *se* (*aluga-se*,
    *fala-se*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *contudo*, *todavia*,
    *uma vez que*, *à medida que*, *ainda que*; common idioms explained by their context; full
    reported speech with the tenses shifted back.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ele procura na cozinha. Ele procura no quarto. Ele procura na sala.*).
- **Questions at the end**: after the story, a paragraph starting `Perguntas:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Perguntas:
  O que a Marta quer?
  A Marta quer um gato.
  Onde a Marta vai?
  Ela vai a um abrigo.
  ```

- **Standard Brazilian Portuguese**: neutral, correct and natural (você, a gente where natural);
  no slang, no regional spellings, no "pra" in the narration.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/pt/<id>.json`:

```json
{
  "id": "pt-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Portuguese, e.g. Comida e compras",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Portuguese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 3000 `B2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/pt/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *disse* for *dizer*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a plural or a
  name); the checker's last line lists the band words not taught yet.

Run the checker after every change, until every lesson passes. Then read each story once more as
a learner at the level would: every sentence clear on first reading, nothing surprising.
