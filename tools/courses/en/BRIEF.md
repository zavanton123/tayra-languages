# Writing an English frequency course

Each course teaches one band of 100 words from the English word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `en-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Mary wants a cat. She goes to a shelter. She sees three cats...* Everyday
  life: home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "he"/"she" is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, common word order.
  Present simple and present continuous up to rank 500; from rank 600 the story may be told in
  the past simple. Little dialogue; when there is some, short and simple.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (relative clauses, *because*, *when*, *although*,
  *however*, *so*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail or diary entry. Grammar grows with the bands, each band
  using what came before:
  - ranks 1001–1300: the past continuous with the past simple (*she was cooking when he
    called*), the present perfect (*I have lived here for ten years*), *going to* and *will*,
    *used to*, comparatives and superlatives, relative clauses with *who*, *which*, *that*,
    *where*;
  - ranks 1301–1600: modal verbs for advice, obligation and possibility (*should*, *have to*,
    *might*), the first conditional (*if it rains, we will stay*), the passive in the present
    and past (*the house was built in 1950*), reported speech (*she said she was tired*);
  - ranks 1601–2000: the second conditional (*if I had more time, I would...*), the past perfect
    (*he had already left*), the present perfect continuous, gerunds and infinitives after verbs
    (*enjoy cooking*, *decide to go*), common phrasal verbs made clear by their context;
  - ranks 2001–2500: the third conditional (*if she had known, she would have...*), *wish* and
    *if only*, the future continuous and future perfect, modals in the past (*should have*,
    *must have*), participle clauses (*arriving home, she...*), more passives;
  - ranks 2501–3000: a richer, more formal register: connectors such as *nevertheless*,
    *whereas*, *as long as*, *even though*, *in spite of*; common idioms explained by their
    context; full reported speech with the tenses shifted back.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the column (a light reflection on everyday life, as in a
    newspaper), dialogue that marks formal and informal speech, and less common connectors
    (*so that*, *unless*, *in case*, *provided that*, *notwithstanding*).
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; mixed conditionals, inversion after negative
    adverbials (*not only did she...*), cleft sentences (*what she needed was...*); long
    sentences that stay easy to parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, rare and learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*He looks in the kitchen. He looks in the bedroom. He looks in the living room.*).
- **Questions at the end**: after the story, a paragraph starting `Questions:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Questions:
  What does Mary want?
  Mary wants a cat.
  Where does Mary go?
  She goes to a shelter.
  ```

- **Standard American English**: neutral, correct and natural, with American spelling (*color*,
  *center*, *apartment*) throughout; contractions are fine in dialogue and in light narration,
  written with a straight apostrophe (*don't*, *it's*); no slang.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/en/<id>.json`:

```json
{
  "id": "en-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, e.g. Food and Shopping",
  "description": "One sentence: what the stories are about.",
  "topic": "A short topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title",
      "summary": "One line on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/en/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *went* for *go*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a plural or a
  name); the checker's last line lists the band words not taught yet.

Run the checker after every change, until every lesson passes. Then read each story once more as
a learner at the level would: every sentence clear on first reading, nothing surprising.
