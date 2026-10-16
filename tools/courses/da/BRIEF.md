# Writing a Danish frequency course

Each course teaches one band of 100 words from the Danish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `da-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Danish title).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Emma vil have en kat. Hun ringer til sin mor. Hun ser tre katte...*
  Everyday life in Denmark: home, family, food, shopping, work, school, the town, the bike,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "han"/"hun" is whenever it could
  be unclear (*de* is "they", *De* the rare formal "you": keep them apart).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position (*I dag går Emma i byen*). Present tense up to rank 500; from rank 600 the story may be
  told in the past (*hun gik*, *han købte*), with the perfect where Danish uses it (*hun har
  spist*, *han er gået*). Little dialogue; when there is some, short and simple, with *du*: Danes
  say *du* to almost everyone, strangers included.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with *ikke* and the other
  sentence adverbs before the verb: *fordi han ikke har tid*, *at hun altid kommer*), dialogue can
  carry more of the story, and a story may be told by a first-person narrator or as a mail or
  diary entry. Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the past, the perfect in dialogue, the future with *skal*,
    *vil* and *kommer til at*, comparatives and superlatives, relative clauses (*manden, som bor
    der*, *huset, de køber*, *der* as the subject: *en pige, der hedder Sara*), *da* for one past
    moment and *når* for what keeps happening;
  - ranks 1301–1600: polite wishes and requests (*jeg vil gerne*, *kunne du...*, *ville du...*),
    the passive with *blive* (*huset blev bygget i 1950*) and with *-s* (*døren åbnes klokken 9*),
    indirect questions (*hun spørger, om...*, *han ved ikke, hvor...*), *for at* and *at* with
    the infinitive;
  - ranks 1601–2000: the past perfect (*han var allerede gået*, *hun havde set det*), conditional
    sentences (*hvis jeg havde mere tid, ville jeg...*), *der* as a formal subject (*der står en
    mand ved døren*), *efter at*, *før*, *siden*, *mens*, *indtil*;
  - ranks 2001–2500: the conditional of the past (*hvis hun havde vidst det, var hun blevet*),
    modal verbs in the perfect (*han skulle have ringet*, *hun burde have sagt det*), the
    *-s* passive with modal verbs (*det kan ikke gøres*), short participle phrases (*den netop
    købte avis*) and the present participle (*smilende*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *dog*, *derimod*,
    *eftersom*, *på trods af*, *medmindre*, *idet*; reported speech (*han sagde, at han var
    træt*; *ifølge avisen skulle det regne*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the *klumme* (a light reflection on everyday life, as in Danish
    newspapers), and less common connectors (*imidlertid*, *så snart*, *jo ... desto*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content (and the
    rarer connector *omend*): a short essay or reflective column that weighs two sides, an
    interview, a profile, a review; precise vocabulary for feelings, ideas and processes; long
    sentences that stay easy to parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, texts that explain a process or compare
    viewpoints in some depth, and the formal *såfremt* and *forudsat at*;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a klumme, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Han leder i køkkenet. Han leder i soveværelset. Han leder i stuen.*).
- **Questions at the end**: after the story, a paragraph starting `Spørgsmål:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Spørgsmål:
  Hvad vil Emma?
  Emma vil have en kat.
  Hvad gør Emma så?
  Hun ringer til sin mor.
  ```

- **Standard Danish**: neutral, correct and natural Danish as written in Denmark, in the current
  official spelling (Retskrivningsordbogen), with *æ*, *ø* and *å*; Danish quotation marks
  (»...«) for speech, never single ones; commas as Danish writes them (a comma before *at*,
  *som*, *fordi* and the other subordinate clauses is fine; be consistent within a course); no
  slang, no dialect in the narration. Settings anywhere in Denmark are welcome; keep the
  standard language and mention a regional word only with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/da/<id>.json`:

```json
{
  "id": "da-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Danish, from themes.tsv, e.g. Mad og indkøb",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Danish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/da/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gik* for *gå*, *store* for *stor*). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Danish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/da/wordlist.tsv`
  (a copy of the app's `da.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built partly from film subtitles, so it is rich in spoken words (*okay*, *hej*,
  *tak*) and some everyday words rank later than you might expect (*cykel* is 1691, *kat* 967,
  *klokken* 709). Check a word's rank rather than guess it.
- The checker compares whole words, lowercased, split at hyphens. The list has no country names
  and few names of places: *Danmark*, *København*, *Jylland*, *Sverige*, *Tyskland*... go in
  `names` when a story needs them (*dansk*, *svensk*, *tysk* are in the list).
- A name's genitive is written without an apostrophe (*Emmas cykel*): put that form in `names`
  too. A name ending in *s*, *x* or *z* takes an apostrophe (*Lars' cykel*), which counts as the
  name. Multi-word names (*Nørre Allé*, *Den Lille Havfrue*) count as known when listed whole in
  `names`; a name listed whole hides its parts, so don't list one whose part is a band word you
  teach.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both (*hold* of *holde* and the noun *hold*, "team"),
  and a form listed under a word may be another word altogether (*gås*, "goose", among the forms
  of *gå*; *får*, "sheep", is counted as *få*); use a new word in its own sense, never through
  its homograph.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Danish writes compounds as one word, and many common ones are in the list
  (*morgenmad*, *aftensmad*); prefer those, or say it another way (*værelset til børnene* for
  an unlisted compound), and keep the unlisted ones few.
- Keep only true names in `names`, never common words. Day and month names are in the list;
  check holiday names (*Sankthans*, *Fastelavn*) and put them in `names` if they are not.
- The list's form lists are noisy (old spellings, other words that share a spelling, Norwegian
  forms); a new word must appear as itself or a true inflection, never through such a form. Some
  entries are names, abbreviations or English words: skip them. Loanwords that Danish uses as its
  own (*computer*, *app*, *weekend*, *film*) are taught like any word; skip only English words
  that are not Danish.
- Write numbers as digits (*7 timer*, *3 børn*), except a number word that is in your band:
  spell that one out to teach it (*syv dage*). Times as *klokken 7* or *klokken 7.30*; dates as
  *5. maj*. The checker splits hyphenated words: *e-mail* becomes *e* + *mail*, so write *mail*
  or *besked*.
