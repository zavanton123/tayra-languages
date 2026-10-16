# Writing a Swedish frequency course

Each course teaches one band of 100 words from the Swedish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `sv-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Swedish title).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Emma vill ha en katt. Hon ringer till sin mamma. Hon ser tre katter...*
  Everyday life in Sweden: home, family, food, shopping, work, school, the town, the bike,
  health, free time, the outdoors, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who "han"/"hon" is whenever it could
  be unclear (*de* is "they", *ni* "you" to several people: keep them apart).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position (*I dag går Emma till stan*); *det finns* ("there is") belongs to every level. Present
  tense up to rank 500; from rank 600 the story may be told in the past (*hon gick*, *han
  köpte*), with the perfect where Swedish uses it (*hon har ätit*, *han har gått*). Little
  dialogue; when there is some, short and simple, with *du*: Swedes say *du* to almost everyone.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with *inte* and the other
  sentence adverbs before the verb: *eftersom han inte har tid*, *att hon alltid kommer*),
  dialogue can carry more of the story, and a story may be told by a first-person narrator or as
  a message or diary entry. Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the past, the perfect in dialogue, the future with *ska* and
    *kommer att*, comparatives and superlatives, relative clauses with *som* (*mannen som bor
    där*, *huset de köper*), *när* for a past moment and for what keeps happening, *då* for
    "then";
  - ranks 1301–1600: polite wishes and requests (*jag skulle vilja*, *kan du...*, *skulle du
    kunna...*), the passive with *bli* (*huset blev byggt 1950*) and with *-s* (*dörren öppnas
    klockan 9*), indirect questions (*hon frågar om...*, *han vet inte var...*), *för att* and
    *att* with the infinitive;
  - ranks 1601–2000: the past perfect (*han hade redan gått*, *hon hade sett det*), conditional
    sentences (*om jag hade mer tid, skulle jag...*), *det* as a formal subject with verbs other
    than *vara* (*det står en man vid dörren*), *efter att*, *innan*, *sedan*, *medan*, *tills*;
  - ranks 2001–2500: the conditional of the past (*om hon hade vetat det, skulle hon ha
    stannat*), modal verbs in the perfect (*han skulle ha ringt*, *hon borde ha sagt det*), the
    *-s* passive with modal verbs (*det kan inte göras*), short participle phrases (*den nyss
    köpta tidningen*) and the present participle (*leende*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *däremot*, *eftersom*,
    *trots att*, *såvida inte*, *ifall*, *enligt*; reported speech (*han sa att han var trött*;
    *enligt tidningen skulle det regna*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the newspaper *krönika* (a light reflection on everyday life),
    and less common connectors (*emellertid*, *dessutom*, *så snart*, *ju ... desto*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone
    and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences
    and the arts, each made clear by its context, texts that explain a process or compare
    viewpoints in some depth, and the formal *förutsatt att* and *i och med att*;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, topics that need some background, which the text gives,
    and the rare *såvida*;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a krönika, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Han letar i köket. Han letar i badrummet. Han letar i vardagsrummet.*).
- **Questions at the end**: after the story, a paragraph starting `Frågor:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Frågor:
  Vad vill Emma ha?
  Emma vill ha en katt.
  Vad gör Emma sedan?
  Hon ringer till sin mamma.
  ```

- **Standard Swedish**: neutral, correct and natural Swedish as written in Sweden, in the
  current official spelling (Svenska Akademiens ordlista), never dialect in the narration.
  Swedish quotation marks (”...”, the same mark at both ends) for speech, never single ones.
  Commas as Swedish writes them (sparingly; a comma before a subordinate clause that comes first
  is fine). Settings anywhere in Sweden are welcome; mention a regional or Finland-Swedish word
  only with its explanation.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/sv/<id>.json`:

```json
{
  "id": "sv-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Swedish, from themes.tsv, e.g. Mat och att handla",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Swedish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/sv/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gick* for *gå*, *stora* for *stor*). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Swedish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/sv/wordlist.tsv`
  (a copy of the app's `sv.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built partly from film subtitles, so it is rich in spoken words (*okej*, *hej*,
  *tack*), and some everyday words rank later than you might expect (*katt* is 940, *kök* 1407,
  *cykel* 1265, *stuga* 2381). Check a word's rank rather than guess it.
- The checker compares whole words, lowercased. The list has no country or place names and few
  first names: *Sverige*, *Stockholm*, *Göteborg*, *Norge*, *Anna*, *Erik*... go in `names` when
  a story needs them (*svensk*, *norsk*, *dansk*, *tysk* are in the list). Some names are also
  common words (*Per*, "per"; *Eva*): put them in `names` too, and never teach a band word
  through such a name.
- A name's genitive is written without an apostrophe (*Emmas cykel*): put that form in `names`
  too. A name ending in *s*, *x* or *z* takes no ending or an apostrophe (*Lars’ cykel*), which
  counts as the name. Multi-word names (*Gamla stan*, *Kungliga Operan*) count as known when
  listed whole in `names`, with their genitive; a name listed whole hides its parts, so don't
  list one whose part is a band word you teach.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both (*sex*, "six" and "sex"; *får* of *få* and the noun
  *får*, "sheep"), and a form listed under a word may be another word altogether (*gås*,
  "goose", among the forms of *gå*); use a new word in its own sense, never through its
  homograph.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Swedish writes compounds as one word, and many common ones are in the list; prefer
  those, or say it another way, and keep the unlisted ones few.
- Keep only true names in `names`, never common words. Day and month names are in the list
  (except *juli*, which the list lacks: put *juli* in `names` when a story needs it); check
  holiday names (*jul*, *påsk*, *midsommar*, *lucia*, *valborg* and *nationaldag* are in the list,
  some at high ranks: check them) and put a holiday in `names` only if the list lacks it. Never teach a band word only
  through a name that contains it (an institution such as *Riksdagen* for *riksdag*, a place
  such as *Gamla stan*).
- The list's form lists are noisy (old spellings, other words that share a spelling, Norwegian
  and Danish forms); a new word must appear as itself or a true inflection, never through such a
  form. Some entries are names, abbreviations or English words: skip them. Loanwords that Swedish
  uses as its own (*app*, *film*, *tv*, *mejl*) are taught like any word; skip only English words
  that are not Swedish.
- Write numbers as digits (*7 timmar*, *3 barn*), except a number word that is in your band:
  spell that one out to teach it (*sju dagar*). A number word already within the course's range
  (*två* 95, *tre* 211, *tio* 643) may be spelled out where Swedish prose would. Several number
  words are missing from the list (*nitton*, *sextio*, *sjuttio*, *åttio*, *nittio*): write them as
  digits. Times as *klockan 7* or *klockan 7.30* (*klocka* ranks 341); dates as *den 6 juni*;
  decades as *1960-talet*, which counts as a number. The checker splits a hyphenated word into
  its parts unless the list has it whole; *e-post* is not in the list, so write *mejl*.
