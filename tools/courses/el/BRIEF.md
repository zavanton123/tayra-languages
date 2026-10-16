# Writing a Greek frequency course

Each course teaches one band of 100 words from the Greek word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `el-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Greek title).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Η Ελένη θέλει μια γάτα. Τηλεφωνεί στη μαμά της. Βλέπει τρεις γάτες...*
  Everyday life in Greece and Cyprus: home, family, food, shopping, work, school, the town, the
  neighbourhood, health, free time, the sea and the islands, the village, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Greek often drops the subject pronoun, so
  say who acts whenever it could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence; *υπάρχει* / *έχει*
  ("there is") belong to every level. Present tense up to rank 500; from rank 600 the story may be
  told in the past, with the aorist for single events (*πήγε*, *αγόρασε*) and the imperfect for
  background and habit (*πήγαινε*, *έτρωγε*). Little dialogue; when there is some, short and
  simple, with *εσύ* among family and friends and *εσείς* with strangers and older people.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*επειδή*, *ότι*, *όταν*, *αλλά*, *γιατί*), dialogue
  can carry more of the story, and a story may be told by a first-person narrator or as a message
  or diary entry. Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the past (aorist and imperfect), the future with *θα* in both
    aspects (*θα πάει*, *θα πηγαίνει*), comparatives and superlatives (*πιο μεγάλος*,
    *μεγαλύτερος*, *ο πιο ωραίος*), relative clauses with *που* (*ο άντρας που μένει εκεί*),
    *όταν* for past moments and for what keeps happening;
  - ranks 1301–1600: polite wishes and requests (*θα ήθελα*, *μπορείτε να...;*, *θα μπορούσες
    να...;*), the passive voice (*το σπίτι χτίστηκε το 1950*, *η πόρτα ανοίγει στις 9*), indirect
    questions (*ρωτάει αν...*, *δεν ξέρει πού...*), *για να* and *να* clauses;
  - ranks 1601–2000: the perfect and past perfect (*έχει φάει*, *είχε ήδη φύγει*), conditional
    sentences (*αν είχα περισσότερο χρόνο, θα...*), *αφού*, *πριν*, *από τότε που*, *ενώ*,
    *μέχρι να*, relative *ο οποίος*;
  - ranks 2001–2500: the conditional of the past (*αν το ήξερε, θα είχε μείνει*), modal
    phrases (*θα έπρεπε να είχε τηλεφωνήσει*, *μπορεί να έχει φύγει*), past participles in
    *-μένος* (*το καινούργιο, μόλις αγορασμένο ποδήλατο*) and the gerund in *-οντας/-ώντας*
    (*χαμογελώντας*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *ωστόσο*, *αντίθετα*,
    *εφόσον*, *παρόλο που*, *εκτός αν*, *σύμφωνα με*; reported speech (*είπε ότι ήταν
    κουρασμένος*; *σύμφωνα με την εφημερίδα θα έβρεχε*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the newspaper *χρονογράφημα* (a light reflection on everyday
    life), and less common connectors (*παρόλα αυτά*, *επιπλέον*, *μόλις*, *όσο ... τόσο*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone
    and register chosen to suit the text type; the formal *εντούτοις*;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, texts that explain a process or compare
    viewpoints in some depth, and the formal *με την προϋπόθεση ότι* and *δεδομένου ότι*;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a χρονογράφημα, a
  letter to a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ψάχνει στην κουζίνα. Ψάχνει στο μπάνιο. Ψάχνει στο σαλόνι.*).
- **Questions at the end**: after the story, a paragraph starting `Ερωτήσεις:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers). Greek
  questions end with the Greek question mark *;* (a semicolon sign):

  ```
  Ερωτήσεις:
  Τι θέλει η Ελένη;
  Η Ελένη θέλει μια γάτα.
  Τι κάνει μετά η Ελένη;
  Τηλεφωνεί στη μαμά της.
  ```

- **Standard Modern Greek**: neutral, correct and natural Modern Greek (Κοινή Νεοελληνική) in the
  monotonic spelling, with the accent (τόνος) on every word of more than one syllable and the
  diaeresis where it belongs; never katharevousa or polytonic spelling, no slang and no dialect
  in the narration. Greek quotation marks («...») for speech, never single ones; the Greek question
  mark (;) and the upper stop (·) where Greek uses them. Settings anywhere in Greece or Cyprus are
  welcome; mention a dialect word only with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/el/<id>.json`:

```json
{
  "id": "el-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Greek, from themes.tsv, e.g. Φαγητό και ψώνια",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
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

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/el/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *πήγε* for *πάω*, *μεγάλη* for *μεγάλος*). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Greek list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/el/wordlist.tsv`
  (a copy of the app's `el.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built partly from film subtitles, so it is rich in spoken words, and some everyday
  words rank later than you might expect (*γάτα* is 1031, *κουζίνα* 1108, *καφές* 2348, *ψωμί*
  1768). Check a word's rank rather than guess it. Each word is listed under one headword with its
  forms: verbs under the first person present (*πηγαίνω*, *τρώω*, with *πήγε*, *έφαγε*), nouns and
  adjectives under the nominative singular (*σπίτι*, *καλός*).
- The checker compares whole words, lowercased, with their accents: write every accent where
  it belongs. The list has no names: proper names drop out of it, and so do the day and month
  names Greek capitalises (*Δευτέρα*, *Κυριακή*, *Ιούλιος*, *Ιουλίου*...) and the holidays
  (*Χριστούγεννα*, *Πάσχα*). Put each one you use in `names`, in every form the text uses
  (*Μαρία*, *Μαρίας*; *Γιώργος*, *Γιώργου*, *Γιώργο*; *Αθήνα*, *Αθήνας*; *Ελλάδα*, *Ελλάδας*). Some
  day names are also ordinary words (*Πέμπτη* "Thursday" and *πέμπτη* "fifth", *Παρασκευή* and
  *παρασκευή* "preparation"): never teach a band word through such a name.
- Write the second accent a word takes before an enclitic pronoun (*το όνομά μου*, *το τηλέφωνό
  της*): the checker reads it as the word itself.
- Elided forms before a vowel count as their full word: *σ'* as *σε*, *τ'* as *το*, *μ'* as
  *με*, *απ'* as *από*, *γι'* as *για* (*σ' αγαπώ*, *απ' το σπίτι*). In the narration prefer the
  full words; keep the short ones for speech and fixed phrases. *κι* is listed under *και*.
- `extra_forms.tsv` also holds the inflections the Greek Wiktionary lists for each word, and it links
  forms the list files as words of their own to their lemma (*αγοράσει* to *αγοράζω*, *ωραία* to
  *ωραίος*). Such an entry in your band is a form of a word the learner already has: skip it, unless it
  is a word of its own (an adverb such as *καλά*, a language name such as *ελληνικά*).
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both, and a form listed under a word may be another word
  altogether; use a new word in its own sense, never through its homograph or a look-alike.
- Keep only true names in `names`, never common words. Never teach a band word only through a
  name that contains it (an institution such as *Βουλή*, a place such as *Άγιο Όρος*).
- The list's form lists are noisy (old or katharevousa forms, unaccented variants, other words
  that share a spelling); a new word must appear as itself or a true inflection, never through
  such a form. Some entries are names, abbreviations or English words: skip them. Loanwords that
  Greek uses as its own (*σούπερ μάρκετ*, *ίντερνετ*, *κομπιούτερ*) are taught like any word;
  skip English words that are not Greek, and write Greek words in the Greek alphabet.
- Write numbers as digits (*7 ώρες*, *3 παιδιά*), except a number word that is in your band:
  spell that one out to teach it (*πέντε μέρες*). A number word already within the course's range
  (*ένας* 11, *δύο* 80, *τρεις* 164, *δέκα* 661) may be spelled out where Greek prose would. Times
  as *στις 7* or *στις 7:30*; dates as *στις 25 Μαρτίου* (the month in `names`); decades as *τη
  δεκαετία του 1960*; centuries as *τον 19ο αιώνα*. The checker splits a hyphenated word into
  its parts; *email* is not in the list, so write *μήνυμα*.
