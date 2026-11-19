# Writing an Afrikaans frequency course

Each course teaches one band of 100 words from the Afrikaans word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `af-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The course's theme is in `themes.tsv` (its English topic and Afrikaans title). The list has 7,274
words, so the last course is `af-mini-7300` (ranks 7201–7274).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Anna wil 'n kat hê. Sy gaan na die dierehawe. Sy sien drie katte...*
  Everyday life where Afrikaans is spoken: South Africa (the Western and Northern Cape, the Free
  State, Gauteng, small Karoo towns, farms, the coast) and Namibia: home, family, food, shopping,
  work, school, the town, the farm, the braai, rugby and netball, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *hy*/*sy* is whenever it could be
  unclear (*sy* is "she" and "his": make it obvious).
- **Plain narration**: third person, short sentences, one idea per sentence, the verb in second
  position (*Môre gaan Anna winkel toe.*). Present tense up to rank 500. Afrikaans has one past
  tense, made with *het* and the participle (*sy het geëet*, *hy het geloop*); from rank 600 the
  story may be told in it, with the few verbs that keep a past form of their own: *was*, *had*,
  *kon*, *moes*, *wou*, *sou*. Little dialogue; when there is some, short and simple, with *jy* among
  family and friends and *u* (or *Oom*, *Tannie*) with strangers and older people.
- **Negation from the start**: a negative sentence ends in a second *nie* (*Sy kan nie kom nie.*
  *Hy het niks gekoop nie.* *Ons gaan nooit daar nie.*); only one *nie* when the two would meet
  (*Ek weet nie.*, *Sy kom nie.*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (subordinate clauses with the verb at the end:
  *omdat*, *dat*, *as*, *wanneer*, *toe*, *hoewel*; *daarom*, *tog*), dialogue can carry more of the
  story, and a story may be told by a first-person narrator or as an e-mail, letter or diary entry.
  Grammar grows with the bands, each band using what came before:
  - ranks 1001–1300: narration in the past with *het* throughout, *toe* for "then" with the verb
    straight after it (*Toe gaan sy huis toe.*), the future with *sal* and *gaan*, comparatives and
    superlatives (*groter*, *die grootste*, *meer interessant*), relative clauses with *wat* (*die
    man wat daar woon*, *die huis wat hulle koop*) and *waarin*, *waarmee*, *waaroor*;
  - ranks 1301–1600: *sou* for wishes and polite requests (*ek sou graag wou*, *sou u kon help?*),
    the passive with *word* in the present and *is* in the past (*die huis word gebou*, *die huis
    is in 1950 gebou*), indirect questions (*sy vra of...*, *hy weet nie waar ... nie*), *om ... te*
    with the infinitive (*sy gaan winkel toe om brood te koop*);
  - ranks 1601–2000: something done before another past event (*hy het al klaar geëet*, *toe sy
    kom, was hy reeds weg*), conditional sentences (*as ek meer tyd gehad het, sou ek...*), the
    words with *daar*, *hier* and *waar* (*sy dink daaroor na*, *hy praat daarvan*, *waarmee skryf
    jy?*), *nadat*, *voordat*, *sedert*, *terwyl*, *totdat*;
  - ranks 2001–2500: the conditional of the past (*as sy dit geweet het, sou sy gebly het*), modal
    verbs in the past with *het* (*hy moes gekom het*, *sy kon dit gedoen het*), the impersonal
    *daar word...* (*daar word gesê dat...*), short participle phrases (*die pas gebakte brood*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *egter*, *daarenteen*,
    *aangesien*, *ondanks*, *tensy*, *indien*; reported speech (*hy het gesê dat hy moeg is*;
    *volgens die koerant gaan dit reën*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *rubriek* (a light reflection on everyday life, as in Afrikaans
    newspapers and magazines), dialogue that marks *u* and *jy*, and less common connectors
    (*desondanks*, *sodra*, *boonop*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a profile, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone and
    register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–7300 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text gives.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Hy soek in die kombuis. Hy soek in die slaapkamer. Hy soek in die sitkamer.*).
- **Questions at the end**: after the story, a paragraph starting `Vrae:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  Vrae:
  Wat wil Anna hê?
  Anna wil 'n kat hê.
  Waarheen gaan Anna?
  Sy gaan na die dierehawe.
  ```

- **Standard Afrikaans**: neutral, correct and natural Afrikaans as written today, in the spelling
  of the *Afrikaanse Woordelys en Spelreëls* (AWS): *foto's*, *ma's*, *ideë*, *geëet*, *môre*,
  *wêreld*, *brûe*, with its circumflexes, diaeresis and acute accents. A sentence that starts with
  *'n* capitalises the next word (*'n Man loop in die straat.*). Double quotation marks (“...”) for
  speech, never single ones, so that quotes and apostrophes stay apart. No slang, no
  Anglicisms where Afrikaans has its own word (*rekenaar*, *selfoon*, *winkelsentrum*), no Dutch
  forms (*ik*, *zijn*, *het* as the article) and no dialect in the narration; Kaaps or other
  regional words only in dialogue and with their meaning clear.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/af/<id>.json`:

```json
{
  "id": "af-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Afrikaans, from themes.tsv, e.g. Kos en inkopies",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Afrikaans",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 7300 `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/af/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gekoop* for *koop*, *mooie* for *mooi*, *kinders* for
  *kind*). Reuse earlier lessons' new words too.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word);
  the checker's last line lists the band words not taught yet.

Things to know about the Afrikaans list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/af/wordlist.tsv`
  (a copy of the app's `af.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The checker reads words as the app's reader does: letters with their accents and the
  apostrophe, which stays on its word (*'n*, *ma's*, *foto's*, *Anna's*), split at hyphens (*Suid-Afrika*
  is *suid* + *Afrika*; *e-pos* is *e* + *pos*, so write *boodskap* or *brief*). The reader keeps
  the abbreviations *mnr.*, *mev.*, *asb.*, *bv.* with their full stop as words the list lacks: write
  *meneer*, *mevrou*, *asseblief*, *byvoorbeeld*. A single quotation mark would stick to its word
  (*'ja'*), which is why speech takes double ones.
- The verbs are listed under their one form, with the participle and the old past forms:
  *is* and *was* are forms of **wees** (rank 2), *het* and *had* of **hê**, *sou* of *sal*, *kon* of
  *kan*, *moes* of *moet*, *wou* of *wil*. The pronouns are words of their own (*ek*, *my*, *jy*,
  *jou*, *hy*, *hom*, *sy*, *haar*, *ons*, *julle*, *hulle*, *u*), as are *moenie* and *toe*.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms): add a line
  there for another true form you need rather than writing around it, and say so in your report.
- A form shared by two words counts for both (*was* of *wees*, also "to wash"; *sy* "she" and
  "his"); use a new word in its own sense, never through its homograph.
- A **separable verb** split in the sentence (*sy staan om 6 uur op*) counts as *staan* + *op*, not
  as *opstaan*; to teach *opstaan*, use forms where it stays whole: the infinitive (*sy moet vroeg
  opstaan*), the participle (*opgestaan*) or a subordinate clause (*omdat sy vroeg opstaan*). Check
  with the checker that the form you use is counted.
- A **compound noun** counts only if the list has it; the checker does not split it into its
  parts. Prefer compounds in the list, or say it another way (*die kamer van die kinders* for an
  unlisted *kinderkamer*), and keep the unlisted ones few. **Diminutives** (*huisie*, *hondjie*,
  *koppie*) count when the list has them; many common ones are there as forms of their word.
- The days, the months and the language names are written with a capital and are in the list as
  such (*Maandag*, *Januarie*, *Afrikaans*, *Engels*); check holiday and place names and put them in
  `names` if they are not in the list.
- Keep only true names in `names`, never common words. The list's form lists are noisy (made
  plurals and diminutives that are other words, subtitle spellings); a new word must appear as
  itself or a true inflection, never through such a form. Some entries come from Wikipedia and
  costume-drama subtitles (*televisiereeks*, *majesteit*, *keiser*, *graaf*): teach them only where a
  natural story wants them, otherwise skip them. Loanwords that Afrikaans uses as its own (*rugby*,
  *internet*, *video*, *program*) are taught like any word; skip English words that are not
  Afrikaans.
- Write numbers as digits (*om 7 uur*, *3 kinders*, *R50*), except a number word that is in your
  band: spell that one out to teach it (*sewe dae*). A number word already within the course's range
  (*twee*, *drie*, *vier*) may be spelled out where Afrikaans prose would (*twee dae later*). Times as
  *om 7 uur* or *om 07:30*; dates as *5 Mei*.
