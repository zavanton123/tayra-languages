# Writing a German frequency course

Each course teaches one band of 100 words from the German word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `de-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Lena möchte eine Katze. Sie geht ins Tierheim. Sie sieht drei Katzen...*
  Everyday life in Germany, Austria and Switzerland: home, family, food, shopping, work, school,
  the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "er"/"sie" is whenever it could be
  unclear (*sie* can mean "she" or "they": make it obvious).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position. Present tense up to rank 500; from rank 600 the story may be told in the Perfekt (with
  *war*, *hatte* and the modal verbs in the Präteritum, as Germans speak). Little dialogue; when
  there is some, short and simple, with *du* among family and friends and *Sie* with strangers.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with the verb at the end:
  *weil*, *dass*, *wenn*, *als*, *obwohl*; *deshalb*, *trotzdem*), dialogue can carry more of the
  story, and a story may be told by a first-person narrator or as an e-mail or diary entry.
  Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the Präteritum (*sie ging*, *er kaufte*), the Perfekt in
    dialogue, the future with *werden*, comparatives and superlatives, relative clauses (*der
    Mann, der dort wohnt*; *die Stadt, in der sie lebt*), *als* and *wenn* for past and repeated
    events;
  - ranks 1301–1600: the Konjunktiv II for wishes and polite requests (*ich hätte gern*, *könnten
    Sie*, *ich würde gern*), the passive in the present and past (*das Haus wurde 1950 gebaut*),
    indirect questions (*sie fragt, ob...*), *um ... zu* and *zu* + infinitive;
  - ranks 1601–2000: the Plusquamperfekt (*er war schon gegangen*), conditional sentences with
    the Konjunktiv II (*wenn ich mehr Zeit hätte, würde ich...*), the genitive (*wegen des
    Wetters*, *das Haus meiner Eltern*), *nachdem*, *bevor*, *seit*, *während*;
  - ranks 2001–2500: the Konjunktiv II of the past (*wenn sie das gewusst hätte, wäre sie...*),
    the Futur II, modal verbs in the passive and the past (*hätte kommen sollen*), extended
    participle phrases kept short (*die gestern gekaufte Zeitung*), more passive;
  - ranks 2501–3000: a richer, more formal register: connectors such as *dennoch*, *allerdings*,
    *wohingegen*, *sofern*, *indem*, *trotz*; reported speech with the Konjunktiv I as newspapers
    use it (*er sagte, er sei müde*); common idioms explained by their context.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the *Glosse* or *Kolumne* (a light reflection on everyday life,
    as in German newspapers), dialogue that marks formal and informal speech, and less common
    connectors (*damit*, *es sei denn*, *falls*, *sodass*, *ungeachtet*).
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style used with care; long
    sentences that stay easy to parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Er sucht in der Küche. Er sucht im Schlafzimmer. Er sucht im Wohnzimmer.*).
- **Questions at the end**: after the story, a paragraph starting `Fragen:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Fragen:
  Was möchte Lena?
  Lena möchte eine Katze.
  Wohin geht Lena?
  Sie geht ins Tierheim.
  ```

- **Standard German**: neutral, correct and natural Hochdeutsch as written in Germany, in the
  current spelling (*dass*, *muss*, *Straße*, *Fluss*), with umlauts and ß, never *ae*/*ss*
  substitutes; German quotation marks („...“); no slang, no dialect in the narration. Austrian
  and Swiss settings are welcome; keep the standard spelling and mention a regional word only
  with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/de/<id>.json`:

```json
{
  "id": "de-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in German, e.g. Essen und Einkaufen",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in German",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/de/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ging* for *gehen*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the German list and the checker:

- The checker compares whole words, lowercased, with ß and ss treated alike (the list mostly
  spells *weiss*, *gross*; always write the correct *weiß*, *groß*). Common forms the list
  misses (*saß*, *aß*, *heiße*, *grüße*) are added in `extra_forms.tsv`; add a line there for
  another true form you need rather than writing around it.
- A **separable verb** split in the sentence (*sie steht um 7 Uhr auf*) counts as *stehen* +
  *auf*, not as *aufstehen*; to teach *aufstehen*, use forms where it stays whole: the infinitive
  (*sie muss früh aufstehen*), the participle (*aufgestanden*), *aufzustehen*, or a subordinate
  clause (*weil sie früh aufsteht*). Check with the checker that the form you use is counted.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Prefer compounds in the list, or say it another way (*das Zimmer der Kinder* for an
  unlisted *Kinderzimmer*), and keep the unlisted ones few.
- A **name's genitive** (*Annas Buch*) is a separate word: add both forms to `names` (*Anna*,
  *Annas*). Day and month names are in the list; holiday names may not be (check). Keep only true
  names in `names`, never common words.
- The list's form lists are noisy (dialect and old spellings, unrelated words); a new word must
  appear as itself or a true inflection, never through such a form. Some entries are names or
  list errors (*Sachs*, *polen*): skip them. Loanwords that German uses as its own nouns (*App*,
  *Ticket*, *Software*, *Shop*, *Blog*) are taught like any word; skip only English words that
  are not German (*top*, *live*, *high*, *sorry*, *my*).
- Write numbers as digits (*7 Uhr*, *3 Kinder*), except a number word that is in your band:
  spell that one out to teach it (*sieben Tage*). The checker splits hyphenated words: *E-Mail*
  becomes *e* + *mail*, and *e* is not in the list, so say *Nachricht* or *Mail* instead.
