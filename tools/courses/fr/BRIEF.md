# Writing a French frequency course

Each course teaches one band of 100 words from the French word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `fr-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Léa veut un chat. Elle va au refuge. Elle voit trois chats...* Everyday
  life in the French-speaking world — France, Belgium, Switzerland, Quebec, and now and then
  West Africa or the overseas regions: home, family, food, shopping, work, school, the city,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "il"/"elle" is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Present tense up to rank 500, with the *futur proche* (*il va
  partir*) and *venir de* where natural; from rank 600 the story may be told in the past, with
  the *passé composé* for events and the *imparfait* for background (*il faisait beau, elle est
  sortie*). Little dialogue; when there is some, short and simple, with *tu* among family and
  friends and *vous* with strangers. Questions in dialogue as people ask them: *Tu viens ?*,
  *Est-ce que tu viens ?*; inversion (*Viens-tu ?*) only after rank 1000.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*parce que*, *quand*, *pendant que*, *même si*,
  *donc*, *pourtant*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1001–1300: the *futur simple*, comparatives and superlatives (*plus grand que*, *le
    meilleur*), relative pronouns *qui*, *que*, *où*, *dont*, the *imparfait* and the *passé
    composé* contrasted (*il lisait quand le téléphone a sonné*), object pronouns *le*, *la*,
    *lui*, *leur* before the verb;
  - ranks 1301–1600: the *conditionnel présent* for wishes and polite requests (*je voudrais*,
    *pourriez-vous*, *ce serait bien*), the pronouns *y* and *en*, the *gérondif* (*en
    marchant*), indirect questions (*elle demande si...*, *il ne sait pas où...*), *pour* +
    infinitive;
  - ranks 1601–2000: the *plus-que-parfait* (*il était déjà parti*), conditional sentences with
    *si* + *imparfait* (*si j'avais le temps, je voyagerais*), the *subjonctif présent* after
    the common triggers (*il faut que*, *vouloir que*, *pour que*, *avant que*), the passive
    (*la maison a été construite en 1950*), *après avoir* + participle;
  - ranks 2001–2500: the *conditionnel passé* and *si* + *plus-que-parfait* (*si elle l'avait
    su, elle serait venue*), the *futur antérieur*, the *subjonctif* after *bien que*, *à
    condition que*, *à moins que* and after feelings (*je suis content qu'il vienne*), reported
    speech with the tense shifts (*il a dit qu'il viendrait*), relative pronouns *lequel*,
    *auquel*, *duquel*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *cependant*,
    *néanmoins*, *tandis que*, *puisque*, *afin que*, *malgré*, *quant à*; the *participe
    présent*; the *passé simple* in third-person written narration, as French books and
    newspapers use it (*il entra, elle sourit*), with the *passé composé* kept in dialogue;
    common idioms explained by their context.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *chronique* (a light reflection on everyday life, as in French
    newspapers), dialogue that marks formal and informal speech, and less common connectors
    (*pourvu que*, *de sorte que*, *quoique*, *en dépit de*, *dès lors*).
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *chronique*, a
  letter to a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Il cherche dans la cuisine. Il cherche dans la chambre. Il cherche dans le salon.*).
- **Questions at the end**: after the story, a paragraph starting `Questions :` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *pourquoi* and *comment*, with fuller
  answers):

  ```
  Questions :
  Qu'est-ce que Léa veut ?
  Léa veut un chat.
  Où va Léa ?
  Elle va au refuge.
  ```

- **Standard French**: neutral, correct and natural French as written in France, with all its
  accents (*à*, *é*, *è*, *ê*, *ç*, *œ*), never without them; no slang, no *verlan*. Settings in
  Belgium, Switzerland, Quebec or Africa are welcome; keep the standard spelling and mention a
  regional word (*septante*, *dîner* for lunch, *char* for car) only with its explanation.
- **French typography**: French quotation marks « like this » for dialogue, with a no-break space
  (U+00A0) inside them and before `:`, `;`, `!` and `?` (*Tu viens ?*). Elisions as French
  writes them (*l'homme*, *j'ai*, *qu'il*, *c'est*, *aujourd'hui*), with the straight apostrophe
  `'`.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/fr/<id>.json`:

```json
{
  "id": "fr-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in French, e.g. Manger et faire les courses",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in French",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/fr/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *allait* for *aller*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the French list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/fr/wordlist.tsv`
  (a copy of the app's `fr.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it.
- The checker compares whole words, lowercased, and splits them as the app's reader does: at
  apostrophes and hyphens (*l'homme* is *l* + *homme*, *qu'il* is *qu* + *il*, *est-ce* is *est*
  + *ce*, *peut-être* is *peut* + *être*), except the few words the list has whole
  (*aujourd'hui*, *jusqu'à*, *jusqu'au*, *quelqu'un*, *lorsqu'on*, *week-end*,
  *rez-de-chaussée*). The elided forms (*l*, *d*, *j*, *qu*, *n*, *s*, *c*, *m*, *t*, *lorsqu*,
  *puisqu*, *jusqu*) are counted as their words. Common forms the list misses are added in
  `extra_forms.tsv`: every verb's full conjugation and the agreement of its participles (*il
  demande*, *ils sont venus*, *elle est née*, *terminée*), generated from the dictionary, so use
  them freely; if another true form is missing, write around it and say so in your report.
- A form shared by two words counts for both (*porte* for *porter* and the noun *porte*,
  *présente* for *présent* and *présenter*): use a new word in its own sense, never through its
  homograph.
- A **compound** written with a hyphen or as several words (*pomme de terre*, *salle de bains*)
  counts as its parts; to teach a hyphenated word the list does not have whole, teach its parts
  instead.
- A **name** goes in `names` as written (*Léa*, *Lyon*, *Montréal*); keep only true names there,
  never common words. Day and month names are in the list; check holiday names.
- The list's form lists are noisy (old spellings, informal spellings such as *p'tit*, unrelated
  words); a new word must appear as itself or a true inflection, never through such a form. Some
  entries are names, abbreviations or English words (*ok*, *the*, *cool*): skip them. Loanwords
  that French uses as its own (*parking*, *week-end*, *sandwich*, *football*, *internet*) are
  taught like any word.
- Write numbers as digits (*7 heures*, *3 enfants*), except a number word that is in your band:
  spell that one out to teach it (*sept jours*). Times as *7 heures* or *7 h 30*.
