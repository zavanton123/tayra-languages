# Writing an Ancient Greek (Koine) frequency course

Each course teaches one band of 100 words from the Ancient Greek word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `grc-mini-0300` teaches ranks 201–300
and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's words, so
the course covers the band; a learner who finishes it knows the 300 most common words. The list has
4,865 words, so the last course is `grc-mini-4900` (65 words). `themes.tsv` gives each course its
theme.

The list is counted from the **New Testament** (the SBLGNT, as MorphGNT lemmatises it by hand), so
the language of the courses is the **Koine** of the first century: the Greek spoken and written from
Alexandria to Rome, as the New Testament, the papyri, Epictetus and Plutarch's plainer pages write it.
Write Koine, not Attic: the list's citation forms and spellings are Koine (*γίνομαι*, *γινώσκω*,
*θέλω*, *θάλασσα*, *πράσσω*), and its forms are the forms the New Testament uses.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense. The stories happen **in the ancient world of the
first century**: the towns and villages of the eastern Mediterranean under Rome, their markets,
farms, harbours, workshops, schools, baths, inns, roads and ships, and the families who live there.
Myths, gods, temples, festivals and synagogues are ordinary parts of that world and may appear.
The stories are your own: never translations, retellings or imitations of New Testament passages or
of any other text, and never a sermon.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ὁ Νικίας οἶκον ζητεῖ. Ὁ πρῶτος οἶκος μέγας ἐστίν, ἀλλὰ οὐ καινός...*
  Everyday life in the towns of the Greek East: *Ἔφεσος*, *Κόρινθος*, *Ἀθῆναι*, *Θεσσαλονίκη*,
  *Φίλιπποι*, *Ἀντιόχεια*, *Ταρσός*, *Ἀλεξάνδρεια*, *Ῥόδος*, *Κύπρος*, *Κρήτη*,
  *Μίλητος*, *Τρῳάς*, *Βέροια*, *Πέργαμος*, *Ῥώμη*: home, family, food, the market, work, school,
  the town, health, free time, journeys by road and by sea.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *αὐτός*/*αὐτή* is whenever it could
  be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, *καί* and *δέ*
  joining them as Koine narrative does. Greek drops subject pronouns: write *ἐγώ*, *σύ*, *αὐτός*
  only for contrast or emphasis. Plain word order: the subject early, the verb where Koine prose
  puts it (often first: *λέγει ὁ πατήρ*, *ἦλθεν ὁ Νικίας εἰς τὴν ἀγοράν*), the article and its noun
  together, never a poet's scrambling. Little dialogue; when there is some, short and simple. Greek
  has no polite "you": *σύ* to everyone, the person addressed in the vocative (*Νικία*, *πάτερ*,
  *Λυδία*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–100: the present indicative, mostly the third person (*ἐστίν*, *ἔχει*, *λέγει*,
    *ἔρχεται*, *ποιεῖ*, *ὁρᾷ*, *ἀκούει*), with *ἔχω*, *ὁρῶ*, *θέλεις;* in short dialogue; *δύναμαι* and
    *θέλω* + infinitive; the article in all its forms; the nominative, accusative, genitive (of
    possession: *ὁ υἱὸς τοῦ Νικίου*) and dative (with *δίδωμι* and *λέγω*: *λέγει αὐτῷ*); *ἐν* +
    dative, *εἰς* + accusative, *ἐκ*/*ἀπό* + genitive, *πρός* + accusative, *μετά* + genitive, *διά*,
    *κατά*, *περί*, *ἐπί*, *ὑπό*, *παρά* in their plain senses; adjectives agreeing with their nouns;
    questions with *τίς*, *τί*, *πῶς*, *ποῦ*, *ἆρα* or the tone alone; the aorist *εἶπεν* and *ἦλθεν*
    as set words of narration;
  - ranks 101–300: the **aorist** for single past events (*εἶδεν*, *ἐποίησεν*, *ἔδωκεν*, *ἔλαβεν*,
    *ἤκουσεν*, *εὗρεν*, *ἐγένετο*), beside the present; the imperfect *ἦν*, *ἦσαν*; relative clauses
    with *ὅς*, *ἥ*, *ὅ*; the dative of time (*τῇ τρίτῃ ἡμέρᾳ*); numbers; *ἵνα* + subjunctive in plain
    purpose clauses (*ἔρχεται ἵνα ἀγοράσῃ ἄρτον*);
  - ranks 301–600: the **imperfect** for background and habits (*ἔλεγεν*, *εἶχεν*, *ἐποίει*); the
    **future** (*ἔσται*, *ποιήσει*, *ἐλεύσεται*, *ὄψεται*); the imperative, present and aorist
    (*ἐλθέ*, *δός*, *εἰπέ*, *ἄκουε*, *ἔρχου*); *ἐγώ*, *σύ*, *ἡμεῖς*, *ὑμεῖς* in every case (*μοι*,
    *σοι*, *ἡμῖν*, *ὑμᾶς*) and *ἐμός*, *σός*; *ἑαυτοῦ* against *αὐτοῦ*; comparatives with *ἤ* or the
    genitive (*μείζων ἢ ὁ οἶκος*, *μείζων τοῦ οἴκου*);
  - ranks 601–1000: the **perfect** (*γέγονεν*, *ἑώρακα*, *οἶδα*, *ἔγνωκα*); the aorist passive
    (*ἐκλήθη*, *ἐγενήθη*); the middle and deponent verbs as plain verbs (*ἀποκρίνεται*, *πορεύεται*,
    *δύναται*); the **subjunctive** with *ἐάν*, *ὅταν*, *ἵνα* and in prohibitions (*μὴ φοβηθῇς*) and
    exhortations (*ἄγωμεν*); participles as adjectives and nouns (*ὁ ἐρχόμενος*, *ἡ μένουσα*);
    superlatives (*μέγιστος*, *ἐλάχιστος*);
  - ranks 1001–1300: the **circumstantial participle** (*ἐλθὼν εἰς τὴν πόλιν εἶδεν*); the genitive
    absolute in plain cases (*ἡλίου δύνοντος*); the articular infinitive (*ἐν τῷ πορεύεσθαι αὐτόν*,
    *μετὰ τὸ φαγεῖν*, *πρὸ τοῦ ἐλθεῖν*); indirect statement with *ὅτι* and with the accusative and
    infinitive (*λέγει αὐτὸν εἶναι ἐν τῷ οἴκῳ*); indirect questions;
  - ranks 1301–1600: the pluperfect; conditional sentences of every kind (*εἰ* + indicative, *ἐάν* +
    subjunctive, the unreal *εἰ ... ἦν, ... ἂν ἦλθεν*); *ὥστε* + infinitive; *πρίν* and *ἕως*;
  - ranks 1601–2000: periphrastic tenses (*ἦν διδάσκων*); relative and temporal clauses with *ἄν*;
    the optative where Koine still has it (wishes: *μὴ γένοιτο*; *εἴη* in a reported question);
  - ranks 2001–3000: a fuller, more literary Koine, as Luke, Acts or Plutarch write it: *μέν ... δέ*,
    *τε ... καί*, *ὅθεν*, *τοίνυν*, *γοῦν*, *ἄρα*; longer periods built on participles; letters in
    their ancient form (*Νικίας Λυδίᾳ τῇ ἀδελφῇ χαίρειν. ... Ἔρρωσο.*);
  - ranks 3001–4000: everything before, in a more descriptive style: precise adjectives, a short
    essay or a letter that reflects on everyday life, a dialogue in the manner of a teacher and his
    pupil;
  - ranks 4001–4900 (C1): the rarer words of crafts, trade, law, medicine and the sea, each made
    clear by its context, and texts that explain a process or weigh two views.
  After rank 1000 sentences get longer and joined (*ὅτι*, *ἐπεί*, *ὡς*, *καίπερ*, *διότι*), dialogue can
  carry more of the story, and a story may be told by a first-person narrator or as a letter.
  From rank 2000 a lesson may also be a short informative text or a letter, as long as it is calm
  and easy to follow. Still one clear idea at a time: a learner at the level should follow every
  sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Ἐρωτήσεις:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *διὰ τί* and *πῶς*, with fuller answers):

  ```
  Ἐρωτήσεις:
  Τίς ἐστιν ὁ Νικίας;
  Ὁ Νικίας ἐστὶν ὁ πατὴρ τῆς Λυδίας.
  Ποῦ ἐστιν ὁ ἄρτος;
  Ὁ ἄρτος ἐστὶν ἐν τῷ οἴκῳ.
  ```

  The header `Ἐρωτήσεις:` (with its smooth breathing) is layout: the checker does not count it.

## Spelling and typography

- **Polytonic orthography**, as the standard editions print it: every breathing, accent, iota
  subscript and diaeresis in place (*ἡ ἡμέρα*, *τῇ ἀγορᾷ*, *υἱός*, *πρωΐ*). Accent as connected text
  is accented: the acute of a final syllable turns grave before another word (*ὁ καλὸς ἄνθρωπος*,
  *καὶ ἦλθεν*), and an enclitic takes the accents the grammar gives it (*ἄνθρωπός τις*, *εἶπέν
  μοι*, *ἐστίν* or *ἐστιν*). The reader and the checker match each word with the accent it has
  standing alone, so this costs nothing. Write the text in precomposed characters (NFC).
- **Koine spellings, as the list has them**: *θάλασσα*, *γλῶσσα*, *πράσσω*, *τέσσαρες* (σσ, not ττ),
  *γίνομαι*, *γινώσκω*, *θέλω*, *σῴζω*, *νοσσίον*. The movable ν where the list writes it
  (*ἐστιν*, *εἶπεν*, *πᾶσιν*), before a vowel and at the end of a sentence at least.
- **No elision**: write every word in full (*διὰ αὐτοῦ*, *ἀπὸ ἐμοῦ*, *ἀλλὰ ἐγώ*, *μετὰ αὐτόν*,
  *κατὰ ἡμέραν*, *ἐπὶ αὐτόν*, *δὲ ἐγώ*, *παρὰ ἐμοί*), never *δι’*, *ἀπ’*, *ἀλλ’*, *μετ’*, *καθ’*,
  *ἐφ’*: the reader keeps the apostrophe inside the word and finds no word of the list. Crasis
  words the list has are fine (*κἀγώ*, *κἀκεῖ*, *κἄν*).
- **Punctuation**: the question mark is the ASCII semicolon `;` (U+003B), the raised dot is `·`
  (U+00B7, the middle dot), never the Greek question mark U+037E or the ano teleia U+0387: the reader
  takes those two as letters of the word before them. Full stop and comma as usual. Direct speech
  in angle quotation marks «…», each speaker's line a paragraph of its own; the narrator's comma
  after the closing mark, a question mark of the quoted sentence inside it:

  ```
  «Ὁρῶ, Λυδία», λέγει ὁ πατήρ.
  «Θέλεις ἄρτον;» λέγει ἡ Λυδία.
  ```

- Capitals at the start of a sentence and for names only. Numbers as digits (*3 ἄρτους*), except a
  number word that is in your band: spell that one out to teach it.

## Names

Names are not in the list (the builder leaves out every lemma MorphGNT capitalises): people, places,
peoples, months and gods go in `names`, in the nominative. The checker gives each name its regular
case forms (*Νικίας*: *Νικίου*, *Νικίᾳ*, *Νικίαν*, *Νικία*; *Δημήτριος*: *Δημητρίου*, *Δημητρίῳ*,
*Δημήτριον*, *Δημήτριε*; *Λυδία*: *Λυδίας*, *Λυδίᾳ*, *Λυδίαν*; *Φοίβη*: *Φοίβης*, *Φοίβην*;
*Ἀθῆναι*: *Ἀθηνῶν*, *Ἀθήναις*; *Φίλιπποι*: *Φιλίππων*, *Φιλίπποις*). A name that is itself a list
word counts as the name in `names`, but never use one as a character or a place: the learner would
read it as the word. Known: *Εἰρήνη*, *Ἐλπίς*, *Χάρις*, *Ζωή*, *Σοφία*, *Ἀγάπη*, *Νίκη*, *Φίλων*
(*φίλος*'s genitive plural), *Στέφανος*, *Σμύρνα* ("myrrh"). Check every name with `rank.py --names`.

Good first names of the period: *Νικίας*, *Δημήτριος*, *Ἀπολλώνιος*, *Θεόδωρος*, *Διονύσιος*,
*Ἀλέξανδρος*, *Κλέων*, *Λυσίας*, *Ἀρίσταρχος*, *Ἐπαφρᾶς*, *Τρόφιμος*, *Ἕρμων*, *Ζήνων*,
*Γλαῦκος*; *Λυδία*, *Φοίβη*, *Χλόη*, *Δάμαρις*, *Ἀρτεμισία*, *Θεοδώρα*, *Μυρτώ*, *Ἑλένη*,
*Ἀγαθοκλέα*, *Ἀπφία*, *Τρύφαινα*, *Εὐοδία*, *Συντύχη*, *Κλεοπάτρα*, *Ἰουλία*, *Πρίσκα*. Roman names in
their Greek form are fine (*Μᾶρκος*, *Λούκιος*, *Γάϊος*, *Ῥοῦφος*, *Κλαυδία*).

## The file

Write `tools/courses/grc/<id>.json`:

```json
{
  "id": "grc-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The title from themes.tsv, in Greek",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["Names of people, places and gods used in the texts, in the nominative"],
  "lessons": [
    {
      "title": "Story title in Greek",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/grc/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names in `names` (with their case forms) and
  numbers count as known. The checker lists every word outside the range with its rank.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ἐστίν* or *ἦσαν* for *εἰμί*, *εἶπεν* for *λέγω*).
  `newWords` holds the word as the list writes it: the first person singular present of a verb
  (*λέγω*, *ἔρχομαι*), the nominative singular of a noun, the masculine nominative singular of an
  adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; skip names, noise and forms of
  other words. The checker's last line lists the band words not taught yet.
- The checker also fails a lesson whose text uses U+037E or U+0387, an elided word, or text that is
  not NFC.

## Things to know about the list and the checker

- The courses are pinned to `tools/courses/grc/wordlist.tsv` (a copy of the app's `grc.tsv`); the
  bands come from it. Its headwords are **MorphGNT's lemmas**, and they follow its conventions:
  - *λέγω* holds *εἶπον* and *ἐρῶ* (*εἶπεν*, *εἰπών*, *ἐρεῖ*); *ὁράω* holds *εἶδον* and *ὄψομαι*
    (*εἶδεν*, *ἰδών*, *ὄψεται*); *ἔρχομαι* holds *ἦλθον*; *ἐσθίω* holds *ἔφαγον*; *φέρω* holds
    *ἤνεγκα*; *εἰμί* holds every form of "to be" (*ἐστίν*, *ἦν*, *ἔσται*, *ὤν*, *ᾖ*, *εἶναι*);
  - *ἐγώ* holds *ἡμεῖς* (*ἡμῶν*, *ἡμῖν*, *ἡμᾶς*) and *σύ* holds *ὑμεῖς* (*ὑμῖν*, *ὑμᾶς*): teaching
    *ἐγώ* teaches "we" too, and there is no separate *ἡμεῖς* to teach;
  - *ὁ* is the article (with *ὅ*, *ἥ*, *οἵ*, *αἵ* where an enclitic follows); *ὅς* the relative;
    *τίς* the question word and *τις* the indefinite are two words; so are *εἰς* "into" and *εἷς*
    "one", *οὐ* "not" and *οὗ* "where", *πῶς* "how?" and *πώς* "somehow", *ποῦ* and *πού*, *πότε*
    and *ποτέ*, *ἄρα* and *ἆρα*;
  - the crasis words are headwords of their own (*κἀγώ* 195, *κἀκεῖνος*, *κἀκεῖ*, *κἄν*);
  - adverbs and prepositions made from a noun or adjective are often entries of their own (*μόνον*,
    *πρῶτον*, *χάριν*, *δωρεάν*, *μακράν*) beside *μόνος*, *πρῶτος*, *χάρις*, *δωρεά*, *μακρός*:
    teach each in its own sense, never one through the other's forms.
- **How the checker reads a word.** As the reader does (`AncientGreekParser`): lowercase, the grave
  read as the acute (*καλὸς* is *καλός*), an enclitic's second accent left out (*ἄνθρωπός* is
  *ἄνθρωπος*). Breathings, circumflexes, iota subscripts and diaereses stay and tell words apart
  (*ἤ* "or", *ἡ* "the", *ᾗ* "where").
- **Forms**: the list has the forms the New Testament uses, up to 40 per word. A correct Koine form it
  lacks (a first person the New Testament never writes, a rarer case) is outside the list: write
  around it, or name the word and form in your report so it goes into `extra_forms.tsv`
  (headword, a tab, the forms). `grc-writers/forms.py` checks a proposed form against the
  dictionary pack's paradigm before it is added.
- **Words of the New Testament's world**: the list ranks religious and Jewish words high (*θεός* 14,
  *κύριος* 21, *πνεῦμα* 45, *ἄγγελος* 87, *ἁμαρτία* 89, *βασιλεία* 93, *προφήτης* 110). They are
  real words of the period with everyday senses too (*κύριος* "master, owner, sir", *ἄγγελος*
  "messenger", *πνεῦμα* "wind, breath", *ἁμαρτία* "mistake, wrong", *βασιλεία* "kingdom, reign"):
  teach each in the sense the story needs, checked against `dictionaries/grc-en.sqlite`.
- **Original texts**: write your own stories; never copy or retell passages of the New Testament,
  the Septuagint, a textbook or the courses of the other languages.
