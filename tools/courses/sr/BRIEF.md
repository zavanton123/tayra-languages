# Writing a Serbian frequency course

Each course teaches one band of 100 words from the Serbian word frequency list
(`bands/<rankUpTo>.txt`: rank, word, its common forms). Course `sr-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## The language of the courses

Standard Serbian of Serbia, **in the Latin script** (the list files its headwords in Latin), in the
**ekavian** pronunciation: *mleko*, *lepo*, *dete*, *reka*, *vreme*, *videti*, *hteti*, *deca* — never the
ijekavian *mlijeko*, *lijepo*, *dijete*, *rijeka*, *vrijeme*, *vidjeti*, *htjeti*, *djeca*, though the list
holds those forms among its own (they are the Bosnian, Croatian and Montenegrin standard, and the checker
cannot tell them apart). Write the letters **č ć ž š đ** and the digraphs **lj nj dž** properly, never *c*, *z*,
*s* or *dj* for them, and never any Cyrillic. Use *ćemo*, *hoću*, *šta*, *kafa*, *voz*, *sat*, *hleb* as in
Serbia, and the infinitive-avoiding *da* + present construction as people speak (*hoću da pojedem*, *moram
da idem*, *mogu da dođem*) beside the infinitive (*hteo bih ići*, *voli čitati*).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Mina želi mačku. Ona ide u prihvatilište. Vidi tri mačke...* Everyday life in
  Serbia — Beograd, Novi Sad, Niš, Kragujevac, Subotica, Čačak, Zlatibor, Tara, Fruška gora, villages of
  Vojvodina and Šumadija — and now and then Serbs abroad (Beč, Štutgart, Čikago, Toronto): home, family,
  food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could
  be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Present tense up to rank 500, with the future (*ću* + infinitive or *da* +
  present) where natural; from rank 600 the story may be told in the past with the *perfekat* (*Ana je
  otišla u školu*; *video sam*), the auxiliary in its usual second place (*Juče je Marko kupio hleb*). Cases
  cannot be avoided in Serbian: use the frequent ones in the simple, regular patterns (*u školu*, *u školi*,
  *sa mamom*, *kod bake*, *Mina ima brata*, *ne vidim ključ*), and let each band add the patterns it needs.
  The short clitic pronouns (*mi*, *ti*, *ga*, *je*, *joj*, *se*) stand in their usual second place. Little
  dialogue; when there is some, short and simple, with *ti* among family and friends and *vi* with
  strangers. Questions in dialogue as people ask them: *Dolaziš li?*, *Da li voliš kafu?*, *Gde je
  stanica?*
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*jer*, *kad*, *dok*, *iako*, *zato što*, *međutim*), dialogue can
  carry more of the story, and a story may be told by a first-person narrator or as an e-mail, a
  letter or a diary entry. Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: comparatives and superlatives (*veći od*, *najbolji*), relatives with *koji*, *što*, *gde*,
    the *perfekat* beside the present contrasted with aspect (*čitao sam kad je zazvonio telefon*), the future
    with *ću* + infinitive beside *da* + present, verbs of motion with prefixes (*doći*, *otići*, *izaći*);
  - ranks 1301–1600: the *potencijal* for wishes and polite requests (*voleo bih*, *možete li...*), the
    imperative, positive and negative (*zatvori vrata*, *nemoj da brineš*), indirect questions (*pita da li...*,
    *ne zna gde...*), *da* clauses of purpose (*da bi*), the vocative;
  - ranks 1601–2000: the *pluskvamperfekat* (*bio je već otišao*), real conditions with *ako* + present,
    unreal ones with *kad bih* (*kad bih imao vremena, putovao bih*), the passive participle and the passive
    with *se* (*prodaju se stanovi*; *kuća je sagrađena 1950.*), reported speech;
  - ranks 2001–2500: the full set of conditionals and past unreal conditions (*da sam znao, bio bih došao*),
    the verbal adverbs (*radeći*, *došavši*), relative *čiji*, *koji* with prepositions, *koliko... toliko*,
    the genitive of quantity and the collective numerals, ordinary use of the instrumental and locative
    in fixed phrases;
  - ranks 2501–3000: a richer, more formal register: connectors such as *međutim*, *dok*, *budući da*,
    *uprkos*, *što se tiče*, *zbog toga*; verbal constructions (*nastaviti da*, *prestati da*, *početi da*);
    the *aorist* and *imperfekat* only in a short tale or an old-style passage; common idioms explained by
    their context.
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and
    clear images, the *kolumna* (a light reflection on everyday life, as in Serbian newspapers), dialogue
    that marks formal and informal speech, and less common connectors (*pod uslovom da*, *premda*, *stoga*,
    *naime*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay or
    reflective column that weighs two sides, an interview, a portrait, a review; precise vocabulary for
    feelings, ideas and processes; the nominal style used with care; long sentences that stay easy to
    parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and the arts,
    each made clear by its context, and texts that explain a process or compare viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words chosen
    for their shades of meaning, and topics that need some background, which the text gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, regional and learned words explained by their context.
    Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a *kolumna*, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one clear idea at
  a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Traži u kuhinji. Traži u sobi. Traži u dnevnoj sobi.*).
- **Questions at the end**: after the story, a paragraph starting `Pitanja:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *zašto* and *kako*, with fuller answers):

  ```
  Pitanja:
  Šta želi Mina?
  Mina želi mačku.
  Kuda ide Mina?
  Ide u prihvatilište.
  ```

- **Standard Serbian**: neutral, correct and natural, with all its letters; no slang, no dialect.
  Prefer words understood everywhere in Serbia.
- **Serbian typography**: dialogue as paragraphs that begin with an em dash and a space (`— Gde si?` ),
  the narrator's remarks in the same paragraph after another dash (`— Ovde — kaže Marko.`); quoted words
  and titles in „low and high“ quotation marks; no space before `:`, `;`, `?` or `!`. Dates as *5. maja*,
  ordinals as digits with a full stop (*3. sprat*), times as *u 7 sati* or *u 7:30*.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/sr/<id>.json`:

```json
{
  "id": "sr-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Serbian, e.g. Hrana i kupovina",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Serbian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/sr/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *išao* for *ići*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word, a Cyrillic or ijekavian duplicate); the checker's last line lists the band words not
  taught yet.

Things to know about the Serbian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/sr/wordlist.tsv`
  (a copy of the app's `sr.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list's headwords are
  Latin and ekavian; each lists its forms in both scripts and both dialects. A few headwords are
  Cyrillic or ijekavian stragglers (*се*, *оно*): they are list noise, skip them.
- The checker compares whole words, lowercased, split at hyphens. The clitics and the auxiliary
  (*je*, *sam*, *ću*, *bih*, *se*, *li*) are words of their own, as in the list; *nisam*, *neću*, *nemam* are
  forms of *biti*, *hteti*, *imati*.
- Common forms the list misses are added in `extra_forms.tsv` (generated from the dictionary,
  Latin forms only), so use them freely; if another true form is missing, write around it and say so in
  your report.
- A form shared by two words counts for both (*sam* "I am" and *sam* "alone"): use a new word in its
  own sense, never through its homograph. Forms that are list words of their own are left to that
  word, so teach such a verb through its other forms.
- A **compound** written as several words (*na primer*, *u stvari*) counts as its parts.
- A **name** goes in `names` as written, in the nominative (*Ana*, *Marko*, *Beograd*, *Novi Sad*): the
  checker accepts their regular case forms (*Ane*, *Marku*, *Beogradu*, *Novog Sada*; the vowel that drops
  in *Petar*, *Kragujevac*, *Čačak* too). A name that is not regular (*Mile → Mileta*, *Lazar → Lazara*...)
  may need its oblique stem listed as well. Keep only true names there, never common words. Day and
  month names are in the list; check holiday names.
- The list's form lists are noisy (Cyrillic and ijekavian duplicates, informal spellings such as
  *sta* for *šta*, unrelated words); a new word must appear as itself or a true inflection, never
  through such a form. Some entries are names, abbreviations or English words: skip them. Loanwords
  that Serbian uses as its own (*kompjuter*, *internet*, *film*, *fudbal*) are taught like any word.
- Write numbers as digits (*7 sati*, *3 deteta*), except a number word that is in your band: spell
  that one out to teach it (*sedam dana*). Times as *u 7 sati* or *u 7:30*; dates as *5. maja*.
