# Writing an Italian frequency course

Each course teaches one band of 100 words from the Italian word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `it-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Lucia vuole un gatto. Va al rifugio. Vede tre gatti...* Everyday life in
  the Italian-speaking world — Rome, Milan, Naples, Turin, Florence, Bologna, Venice, Genoa,
  Palermo, Sardinia, Puglia, Trieste, the Alps, and now and then Ticino (Switzerland), San Marino
  or Italian families abroad: home, family, food, shopping, work, school, the city, health, free
  time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *lui*/*lei* is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Present tense up to rank 500, with *stare* + gerund (*sta mangiando*),
  *stare per* + infinitive and *andare a* + infinitive where natural; from rank 600 the story may
  be told in the past, with the *passato prossimo* for events (*è uscita*, *ha comprato*, with the
  right auxiliary and agreement) and the *imperfetto* for background (*faceva sole, lei è uscita*).
  The passato remoto is not used until rank 2500. The short pronouns come before the verb from the
  start (*ti vedo*, *gli dà un libro*, *lo vuole*), and *c'è* / *ci sono* are among the first
  structures. Little dialogue; when there is some, short and simple, with *tu* among family and
  friends and *Lei* with strangers (the formal *Lei* written with a capital L in the middle of a
  sentence). Questions in dialogue as people ask them: *Vieni?*, *Dov'è la stazione?*
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*perché*, *quando*, *mentre*, *anche se*,
  *quindi*, *però*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1001–1300: the *futuro semplice*, comparatives and superlatives (*più grande di*, *il
    migliore*), relatives with *che*, *cui*, *chi*, *quello che*, the *passato prossimo* beside the
    *imperfetto* contrasted (*leggevo quando è suonato il telefono*), object pronouns attached to
    an infinitive or gerund (*vuole vederla*, *sta guardandolo*);
  - ranks 1301–1600: the *condizionale* for wishes and polite requests (*vorrei*, *potrebbe...?*),
    two pronouns together (*me lo dà*, *glielo dico*, *dimmelo*), indirect questions (*chiede
    se...*, *non sa dove...*), *per* + infinitive, the affirmative and negative imperative (*chiudi
    la porta*, *non preoccuparti*);
  - ranks 1601–2000: the *trapassato prossimo* (*era già uscito*), the *congiuntivo presente* after
    the common triggers (*voglio che*, *penso che sia*, *affinché*, *è importante che*, *prima
    che*), conditional sentences with *se* + present, the passive with *essere*/*venire* + participle
    and the *si passivante* (*si vendono case*; *la casa fu costruita nel 1950* is later);
  - ranks 2001–2500: the *congiuntivo imperfetto*, *se* + *congiuntivo imperfetto* + *condizionale*
    (*se avessi tempo, viaggerei*), the *condizionale passato* and *se* + *congiuntivo trapassato*
    (*se lo avessi saputo, sarei venuto*), the *futuro anteriore*, the subjunctive after *benché*,
    *a meno che*, *senza che* and after feelings (*mi fa piacere che tu venga*), reported speech
    with the tense shifts (*ha detto che sarebbe venuto*), *il quale*, *la quale*, *cui*, *il cui*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *tuttavia*, *mentre*,
    *poiché*, *affinché*, *nonostante*, *per quanto riguarda*; verbal periphrases (*continuare a*,
    *tornare a*, *smettere di*, *stare* + gerund, *finire di*); the *passato remoto* in a story's
    background or a short historical passage; common idioms explained by their context.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *rubrica* (a light reflection on everyday life, as in Italian
    newspapers), dialogue that marks formal and informal speech, and less common connectors
    (*purché*, *dato che*, *benché*, *nondimeno*, *di conseguenza*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review;
    precise vocabulary for feelings, ideas and processes; the nominal style used with care; long
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *rubrica*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Cerca in cucina. Cerca in camera. Cerca in salotto.*).
- **Questions at the end**: after the story, a paragraph starting `Domande:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *perché* and *come*, with fuller
  answers):

  ```
  Domande:
  Che cosa vuole Lucia?
  Lucia vuole un gatto.
  Dove va Lucia?
  Va al rifugio.
  ```

- **Standard Italian**: neutral, correct and natural Italian, with all its accents and marks
  (*à*, *è*, *é*, *ì*, *ò*, *ù*), never without them (*è*, *perché*, *città*, *più*, *può*, *già*,
  *così*, *caffè*, *papà*); no dialect and no slang. Prefer words understood everywhere; when a
  story uses a regional word (*anguria*/*cocomero*, *sciarpa*), let its context make it clear.
- **Italian typography**: dialogue in the Italian quotation marks « », with no spaces inside
  them; the apostrophe of an elision written as a plain `'` (*l'amico*, *un'amica*, *c'è*, *po'*),
  with no space after it; no space before `:`, `;`, `?` or `!`; a question mark only at the end
  (no ¿). Write the accents and marks as the characters themselves, with grave accents on final
  stressed vowels (*città*, *perché*, *può*) and acute only on *é* in *perché*, *né*, *sé*,
  *benché* and the like.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/it/<id>.json`:

```json
{
  "id": "it-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Italian, e.g. Mangiare e fare la spesa",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Italian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/it/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *andavo* for *andare*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the Italian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/it/wordlist.tsv`
  (a copy of the app's `it.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it.
- **Elisions** come off the front: *l'amico* is *l* + *amico*, *dell'acqua* is *dell* + *acqua*,
  *un'amica* is *un* + *amica*, *c'è* is *c* + *è*, *com'è* and *dov'è* (which the list has whole)
  count as written. The articulated prepositions (*del*, *nella*, *sul*, *dal*) are forms of
  *di*, *in*, *su*, *da* in the list. The apocopated *quest'anno*, *bell'uomo*, *sant'Antonio*,
  *tutt'altro*, *cos'è*, *anch'io* are counted by their parts too.
- A **verb carrying pronouns** is split as a learner reads it: *mangiarlo* is *mangiar* + *lo*,
  *dimmelo* is *di* + *me* + *lo*, *guardandolo* is *guardando* + *lo*, *andarsene* is *andar* +
  *se* + *ne*, *eccolo* is *ecco* + *lo*, *dammi* is *da* + *mi*. The compounds the list holds as
  words of their own beside the real words they resemble (*farmi*, *dirlo*, *alzarsi*) count as
  the verb plus the pronouns; a word like *parola*, *bene* or *portale* is never split.
- Common forms the list misses are added in `extra_forms.tsv`: every verb's full conjugation
  (*fui*, *fummo*, *avemmo*), the gender and number of nouns and adjectives, the short elided
  forms (*quant'*, *sant'*, *bell'*), and *è*, which the list lacks altogether. They come from the
  dictionary, so use them freely; if another true form is missing, write around it and say so in
  your report.
- A form shared by two words counts for both (*sono* for *essere*, *dà* for *dare*): use a new
  word in its own sense, never through its homograph. Forms that are list words of their own are
  left to that word (*come* is not counted for *comare*), so teach such a verb through its other
  forms.
- A **compound** written as several words (*per favore*, *a posto*) counts as its parts.
- A **name** goes in `names` as written (*Lucia*, *Napoli*, *Torino*; also of several words:
  *San Marino*, *Piazza Navona*, *Sant'Angelo*, which then count as one known word); keep only
  true names there, never common words. Day and month names are in the list; check holiday names.
  A name that is an adjective in the text (*romano*, *milanese*) is an ordinary word with its own
  rank, not a name.
- The list's form lists are noisy (old spellings, informal spellings such as *xché* or *nn*,
  unrelated words); a new word must appear as itself or a true inflection, never through such a
  form. Some entries are names, abbreviations or English words (*ok*, *the*, *show*): skip them.
  Loanwords that Italian uses as its own (*computer*, *internet*, *sport*, *film*) are taught like
  any word.
- Write numbers as digits (*7 ore*, *3 figli*), except a number word that is in your band:
  spell that one out to teach it (*sette giorni*). Times as *alle 7* or *alle 7:30*; ordinals as
  *1º*, *2ª* or words.
