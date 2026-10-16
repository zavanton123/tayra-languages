# Writing a Norwegian frequency course

Each course teaches one band of 100 words from the Norwegian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `no-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Norwegian title).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Emma vil ha en katt. Hun ringer til moren sin. Hun ser tre katter...*
  Everyday life in Norway: home, family, food, shopping, work, school, the town, the bike,
  health, free time, the outdoors, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "han"/"hun" is whenever it could
  be unclear (*de* is "they", *De* the rare formal "you": keep them apart).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position (*I dag går Emma i byen*); *det er* ("there is") belongs to every level. Present tense
  up to rank 500; from rank 600 the story may be told in the past (*hun gikk*, *han kjøpte*),
  with the perfect where Norwegian uses it (*hun har spist*, *han har gått*). Little dialogue;
  when there is some, short and simple, with *du*: Norwegians say *du* to almost everyone.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with *ikke* and the other
  sentence adverbs before the verb: *fordi han ikke har tid*, *at hun alltid kommer*), dialogue
  can carry more of the story, and a story may be told by a first-person narrator or as a message
  or diary entry. Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the past, the perfect in dialogue, the future with *skal*,
    *vil* and *kommer til å*, comparatives and superlatives, relative clauses with *som*
    (*mannen som bor der*, *huset de kjøper*), *da* for one past moment and *når* for what keeps
    happening;
  - ranks 1301–1600: polite wishes and requests (*jeg vil gjerne*, *kan du...*, *kunne du...*),
    the passive with *bli* (*huset ble bygget i 1950*) and with *-s* (*døra åpnes klokka 9*),
    indirect questions (*hun spør om...*, *han vet ikke hvor...*), *for å* and *å* with the
    infinitive;
  - ranks 1601–2000: the past perfect (*han hadde allerede gått*, *hun hadde sett det*),
    conditional sentences (*hvis jeg hadde mer tid, ville jeg...*), *det* as a formal subject with
    verbs other than *være* (*det står en mann ved døra*), *etter at*, *før*, *siden*, *mens*,
    *til*;
  - ranks 2001–2500: the conditional of the past (*hvis hun hadde visst det, ville hun ha
    blitt*), modal verbs in the perfect (*han skulle ha ringt*, *hun burde ha sagt det*), the
    *-s* passive with modal verbs (*det kan ikke gjøres*), short participle phrases (*den
    nettopp kjøpte avisa*) and the present participle (*smilende*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *derimot*, *ettersom*,
    *til tross for*, *med mindre*, *dersom*, *ifølge*; reported speech (*han sa at han var
    trøtt*; *ifølge avisa skulle det regne*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the newspaper *spalte* or *kåseri* (a light reflection on
    everyday life), and less common connectors (*imidlertid*, *dessuten*, *så snart*, *jo ...
    desto*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone
    and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, texts that explain a process or compare
    viewpoints in some depth, and the formal *forutsatt at* and *idet*;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, topics that need some background, which the text gives,
    and the rare *såfremt*;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a spalte, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Han leter på kjøkkenet. Han leter på badet. Han leter i stua.*).
- **Questions at the end**: after the story, a paragraph starting `Spørsmål:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Spørsmål:
  Hva vil Emma?
  Emma vil ha en katt.
  Hva gjør Emma så?
  Hun ringer til moren sin.
  ```

- **Standard Norwegian**: neutral, correct and natural Norwegian Bokmål as written in Norway,
  in the current official spelling (Bokmålsordboka), never Nynorsk and never dialect in the
  narration. Use the forms most writers use today: feminine nouns may take *-a* (*jenta*, *boka*,
  *døra*) or *-en*; choose one and keep it within a course, and prefer the forms the list has.
  Weak verbs take *-et* in the past (*kastet*, *snakket*). Norwegian quotation marks («...») for
  speech, never single ones. Settings anywhere in Norway are welcome; mention a dialect or
  Nynorsk word only with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/no/<id>.json`:

```json
{
  "id": "no-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Norwegian, from themes.tsv, e.g. Mat og handling",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Norwegian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/no/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gikk* for *gå*, *store* for *stor*). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Norwegian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/no/wordlist.tsv`
  (a copy of the app's `no.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list mixes Bokmål with some Nynorsk and older forms, and some everyday words rank later
  than you might expect (*katt* is 1528, *kjøkken* 1527, *hytte* 2351). Check a word's rank
  rather than guess it.
- The checker compares whole words, lowercased. The list has no country or place names:
  *Norge*, *Oslo*, *Bergen*, *Sverige*, *Danmark*... go in `names` when a story needs them
  (*norsk*, *svensk*, *dansk*, *tysk* are in the list).
- A name's genitive is written without an apostrophe (*Emmas sykkel*): put that form in `names`
  too. A name ending in *s*, *x* or *z* takes an apostrophe (*Lars' sykkel*), which counts as the
  name. Multi-word names (*Karl Johans gate*, *Den Norske Opera*) count as known when listed whole
  in `names`, with their genitive; a name listed whole hides its parts, so don't list one whose
  part is a band word you teach.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both (*får* of *få* and the noun *får*, "sheep"; *hold*
  of *holde* and the noun *hold*), and a form listed under a word may be another word altogether
  (*gås*, "goose", among the forms of *gå*); use a new word in its own sense, never through its
  homograph.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Norwegian writes compounds as one word, and many common ones are in the list; prefer
  those, or say it another way, and keep the unlisted ones few.
- Keep only true names in `names`, never common words. Day and month names are in the list;
  check holiday names (*Sankthans*, *17. mai*) and put them in `names` if they are not. Never
  teach a band word only through a name that contains it (an institution such as *Stortinget*
  for *storting*, a place such as *Nordkapp*).
- The list's form lists are noisy (old spellings, Nynorsk forms, other words that share a
  spelling); a new word must appear as itself or a true inflection, never through such a form.
  Some entries are names, abbreviations or English words: skip them. Loanwords that Norwegian
  uses as its own (*app*, *film*, *tv*) are taught like any word; skip only English words that
  are not Norwegian.
- Write numbers as digits (*7 timer*, *3 barn*), except a number word that is in your band:
  spell that one out to teach it (*sju dager*). A number word already within the course's range
  (*to*, *tre*, *ti*) may be spelled out where Norwegian prose would. Times as *klokka 7* or
  *klokka 7.30* (*klokka* ranks 1170: before that, write around it: *fra 9 til 16*, *om morgenen*); dates as *17. mai*; decades as *1960-tallet*. The checker splits a hyphenated
  word into its parts unless the list has it whole; *e-post* is not in the list, so write
  *melding* or *post*.
