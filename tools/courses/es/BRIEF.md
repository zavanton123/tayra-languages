# Writing a Spanish frequency course

Each course teaches one band of 100 words from the Spanish word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `es-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Lucía quiere un gato. Va al refugio. Ve tres gatos...* Everyday life in
  the Spanish-speaking world — Spain, Mexico, Argentina, Colombia, Peru, Chile, and now and then
  Cuba, Uruguay, Central America or the United States: home, family, food, shopping, work,
  school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *él*/*ella* is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Present tense up to rank 500, with *ir a* + infinitive (*va a salir*),
  *acabar de* + infinitive and *estar* + gerund (*está comiendo*) where natural; from rank 600 the
  story may be told in the past, with the *pretérito indefinido* for events and the *imperfecto*
  for background (*hacía sol, ella salió*). The short object pronouns are among the first words,
  so they come before the verb from the start (*te veo*, *le da un libro*, *lo quiere*). Little
  dialogue; when there is some, short and
  simple, with *tú* among family and friends and *usted* with strangers. Questions in dialogue as
  people ask them: *¿Vienes?*, *¿Dónde está la estación?*
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*porque*, *cuando*, *mientras*, *aunque*,
  *así que*, *sin embargo*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1001–1300: the *futuro simple*, comparatives and superlatives (*más grande que*, *el
    mejor*), relative *que*, *quien*, *donde* and *lo que*, the *pretérito perfecto* (*he
    comido*) beside the *indefinido*, the *imperfecto* and the *indefinido* contrasted (*leía
    cuando sonó el teléfono*), object pronouns attached to an infinitive or gerund (*quiere
    verla*, *está mirándolo*);
  - ranks 1301–1600: the *condicional* for wishes and polite requests (*me gustaría*, *¿podría
    usted...?*), two pronouns together (*se lo doy*, *dímelo*), indirect questions (*pregunta
    si...*, *no sabe dónde...*), *para* + infinitive, the affirmative and negative imperative
    (*cierra la puerta*, *no te preocupes*);
  - ranks 1601–2000: the *pluscuamperfecto* (*ya había salido*), the *presente de subjuntivo*
    after the common triggers (*quiero que*, *para que*, *es importante que*, *ojalá*, *cuando*
    for the future: *cuando llegues, llámame*), conditional sentences with *si* + present, the
    passive with *se* (*se venden pisos*) and with *ser* + participle (*la casa fue construida en
    1950*);
  - ranks 2001–2500: the *imperfecto de subjuntivo*, *si* + *imperfecto de subjuntivo* +
    *condicional* (*si tuviera tiempo, viajaría*), the *condicional compuesto* and *si* +
    *pluscuamperfecto de subjuntivo* (*si lo hubiera sabido, habría venido*), the *futuro
    perfecto*, the subjunctive after *aunque*, *a menos que*, *sin que* and after feelings (*me
    alegra que vengas*), reported speech with the tense shifts (*dijo que vendría*), *el cual*,
    *la cual*, *cuyo*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *no obstante*,
    *mientras que*, *puesto que*, *a fin de que*, *a pesar de*, *en cuanto a*; verbal
    periphrases (*llevar* + gerund, *seguir* + gerund, *volver a* + infinitive, *dejar de*);
    common idioms explained by their context.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *columna* (a light reflection on everyday life, as in Spanish-language
    newspapers), dialogue that marks formal and informal speech, and less common connectors
    (*con tal de que*, *de modo que*, *si bien*, *pese a*, *por consiguiente*).
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *columna*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Busca en la cocina. Busca en el dormitorio. Busca en el salón.*).
- **Questions at the end**: after the story, a paragraph starting `Preguntas:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *por qué* and *cómo*, with fuller
  answers):

  ```
  Preguntas:
  ¿Qué quiere Lucía?
  Lucía quiere un gato.
  ¿Adónde va Lucía?
  Va al refugio.
  ```

- **Standard Spanish**: neutral, correct and natural Spanish, with all its accents and marks
  (*á*, *é*, *í*, *ó*, *ú*, *ñ*, *ü*), never without them; no slang. Each story speaks the Spanish
  of its setting within the standard: *vosotros* in a story set in Spain, *ustedes* in Latin
  America; keep *tú* rather than *vos* everywhere, so no *voseo*. Prefer words understood
  everywhere; when a story uses a regional word (*coche*/*carro*, *ordenador*/*computadora*,
  *piso*/*departamento*), let its context make it clear.
- **Spanish typography**: opening and closing question and exclamation marks (*¿Vienes?*,
  *¡Qué bien!*); dialogue in Spanish quotation marks « », with no spaces inside them; no space
  before `:`, `;`, `?` or `!`. Write the accents and marks as the characters themselves.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/es/<id>.json`:

```json
{
  "id": "es-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Spanish, e.g. Comer y hacer la compra",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Spanish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/es/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *iba* for *ir*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the Spanish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/es/wordlist.tsv`
  (a copy of the app's `es.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it.
- The checker compares whole words, lowercased, split at hyphens. The contractions *del* and *al*
  count as *de* and *a*. A verb carrying pronouns is split as a learner reads it: *dándoselo* is
  *dando* + *se* + *lo*, *dímelo* is *di* + *me* + *lo* (the written accent comes off the verb);
  forms the list holds whole, such as *levantarse* or *darle*, count as their verb.
- Common forms the list misses are added in `extra_forms.tsv`: every verb's full conjugation and
  its participle's gender and number (*él pidió*, *ellos vinieron*, *que yo pueda*, *cerrada*),
  and the gender and number of adjectives and nouns, generated from the dictionary, so use them
  freely; if another true form is missing, write around it and say so in your report.
- A form shared by two words counts for both (*fue* for *ser* and *ir*): use a new word in its
  own sense, never through its homograph. Forms that are list words of their own are left to that
  word (*para* is not counted for *parar*, nor *como* for *comer*, *vino* for *venir* or *bajo* for
  *bajar*), so teach such a verb through its other forms.
- A **compound** written as several words (*fin de semana*, *sin embargo*) counts as its parts.
- A **name** goes in `names` as written (*Lucía*, *Sevilla*, *Bogotá*, also of several words:
  *San José*, *La Habana*, *Costa Rica*, which then count as one known word); keep only true
  names there, never common words. Day and month names are in the list; check holiday names.
- The list's form lists are noisy (old spellings, informal spellings such as *pa* or *pq*,
  unrelated words); a new word must appear as itself or a true inflection, never through such a
  form. Some entries are names, abbreviations or English words (*ok*, *the*, *show*): skip them.
  Loanwords that Spanish uses as its own (*fútbol*, *internet*, *wifi*, *sándwich*) are taught
  like any word.
- Write numbers as digits (*7 horas*, *3 hijos*), except a number word that is in your band:
  spell that one out to teach it (*siete días*). Times as *a las 7* or *a las 7:30*.
