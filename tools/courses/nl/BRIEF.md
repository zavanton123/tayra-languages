# Writing a Dutch frequency course

Each course teaches one band of 100 words from the Dutch word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `nl-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Dutch title).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Lisa wil een kat. Ze gaat naar het asiel. Ze ziet drie katten...*
  Everyday life in the Netherlands and Flanders: home, family, food, shopping, work, school,
  the city, the bike, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "hij"/"zij" is whenever it could
  be unclear (*ze* can mean "she" or "they": make it obvious).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position. Present tense up to rank 500; from rank 600 the story may be told in the perfectum
  (*ze heeft gegeten*, *hij is gegaan*), with *was*, *had* and the modal verbs in the imperfectum,
  as Dutch speakers tell things. Little dialogue; when there is some, short and simple, with *je*
  among family and friends and *u* with strangers.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with the verb at the end:
  *omdat*, *dat*, *als*, *toen*, *hoewel*; *daarom*, *toch*), dialogue can carry more of the
  story, and a story may be told by a first-person narrator or as an e-mail or diary entry.
  Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the imperfectum (*ze liep*, *hij kocht*), the perfectum in
    dialogue, the future with *gaan* and *zullen*, comparatives and superlatives, relative
    clauses (*de man die daar woont*, *het huis dat ze kopen*, *de stad waarin ze woont*), *toen*
    for a past moment and *als* for what keeps happening;
  - ranks 1301–1600: *zou* and *zouden* for wishes and polite requests (*ik zou graag*, *zou u
    kunnen*), the passive with *worden* in the present and past (*het huis werd in 1950
    gebouwd*), indirect questions (*ze vraagt of...*, *hij weet niet waar...*), *om ... te* and
    *te* with the infinitive;
  - ranks 1601–2000: the voltooid verleden tijd (*hij was al vertrokken*, *ze had het gezien*),
    conditional sentences (*als ik meer tijd had, zou ik...*), the words with *er* (*ze denkt
    erover na*, *hij praat ervan*, *er zijn drie*), *nadat*, *voordat*, *sinds*, *terwijl*;
  - ranks 2001–2500: the conditional of the past (*als ze dat had geweten, was ze gebleven*),
    modal verbs in the perfect (*hij had moeten komen*), the passive with *zijn* (*de deur is
    gesloten*) and the impersonal *er wordt...*, short participle phrases (*de gisteren gekochte
    krant*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *desondanks*, *echter*,
    *daarentegen*, *mits*, *doordat*, *ondanks*; reported speech (*hij zei dat hij moe was*;
    *volgens de krant zou het gaan regenen*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the *column* or *cursiefje* (a light reflection on everyday
    life, as in Dutch newspapers), dialogue that marks *u* and *je*, and less common connectors
    (*zodat*, *tenzij*, *indien*, *opdat*, *zodra*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone
    and register chosen to suit the text type;
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
  (*Hij zoekt in de keuken. Hij zoekt in de slaapkamer. Hij zoekt in de woonkamer.*).
- **Questions at the end**: after the story, a paragraph starting `Vragen:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Vragen:
  Wat wil Lisa?
  Lisa wil een kat.
  Waar gaat Lisa naartoe?
  Ze gaat naar het asiel.
  ```

- **Standard Dutch**: neutral, correct and natural Dutch as written in the Netherlands, in the
  current official spelling (the Groene Boekje: *pannenkoek*, *ideeën*, *café's*, *foto's*), with
  its accents and diaeresis; double quotation marks (“...”) for speech, never single ones, so that
  quotes and apostrophes stay apart; no slang, no dialect in the narration. Flemish settings are
  welcome; keep the standard language and mention a Flemish word only with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/nl/<id>.json`:

```json
{
  "id": "nl-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Dutch, from themes.tsv, e.g. Eten en boodschappen doen",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Dutch",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/nl/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *liep* for *lopen*, *mooie* for *mooi*). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Dutch list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/nl/wordlist.tsv`
  (a copy of the app's `nl.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The checker compares whole words, lowercased, split at hyphens. A word with an apostrophe inside
  stays whole, as the list has it (*auto's*, *foto's*, *zo'n*, *m'n*, *z'n*); the short forms with an
  apostrophe in front count as the words they stand for: *'s* (*'s ochtends*, *'s avonds*) as *des*,
  *'t* as *het*, *'n* as *een*, *'k* as *ik*. In the narration prefer the full words (*het*, *een*,
  *mijn*); keep the short ones for speech and fixed phrases. A name's *'s* counts as the name
  (*Anna's fiets*); an *-s* without an apostrophe (*Peters fiets*) needs that form in `names` too.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both (*was* of *zijn* and of *wassen*, *ben*); use a new
  word in its own sense, never through its homograph.
- A **separable verb** split in the sentence (*ze staat om 7 uur op*) counts as *staan* + *op*, not
  as *opstaan*, and neither does *op te staan*; to teach *opstaan*, use forms where it stays whole:
  the infinitive (*ze moet vroeg opstaan*), the participle (*opgestaan*) or a subordinate clause
  (*omdat ze vroeg opstaat*). Check with the checker that the form you use is counted.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Prefer compounds in the list, or say it another way (*de kamer van de kinderen* for an
  unlisted *kinderkamer*), and keep the unlisted ones few. **Diminutives** (*huisje*, *kopje*,
  *meisje*) count when the list has them; most common ones are there.
- Keep only true names in `names`, never common words. Day and month names are in the list;
  check holiday names (*Sinterklaas*, *Koningsdag*) and put them in `names` if they are not.
- The list's form lists are noisy (old spellings such as *zoo*, dialect, unrelated words); a new
  word must appear as itself or a true inflection, never through such a form. Some entries are
  names, abbreviations or English words: skip them. Loanwords that Dutch uses as its own (*computer*,
  *website*, *app*, *team*, *film*) are taught like any word; skip only English words that are not
  Dutch (*the*, *you*, *my*, *sorry* as English).
- Write numbers as digits (*7 uur*, *3 kinderen*), except a number word that is in your band: spell
  that one out to teach it (*zeven dagen*). Times as *om 7 uur* or *om 7.30 uur*; dates as *5 mei*.
  The checker splits hyphenated words: *e-mail* becomes *e* + *mail*, so write *mail* or *bericht*.
