# Writing a Maltese frequency course

Each course teaches one band of 100 words from the Maltese word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `mt-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
The list has 6,561 words: the courses are `mt-mini-0100` to `mt-mini-6600` (the last band has 61
words). `themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Pawlu ma jafx fejn hu t-telefon tiegħu. Imur fil-kċina. Ma jarahx. Imur
  fis-salott...* Everyday life where Maltese is spoken: Malta and Gozo (Għawdex) above all, the
  towns and villages (il-Belt, il-Mosta, Birkirkara, il-Ħamrun, Ħal Qormi, iż-Żejtun,
  Marsaxlokk, in-Naxxar, Ħ'Attard, Tas-Sliema, San Ġiljan, Bormla, il-Birgu, l-Isla, Ħal
  Kirkop; in Gozo ir-Rabat/Victoria, Marsalforn, Xlendi, in-Nadur), the harbours, the bays, the
  countryside (l-għelieqi, ir-raba', il-ħitan tas-sejjieħ), the festa band, the pastizzeria; and
  now and then Australia, Canada, the UK or Rome, where Maltese families live. Home, family, food,
  shopping, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  wordplay jokes, unreliable narrators or open endings. Events in the order they happen. Few
  characters (one to three), named clearly; say who *hu*/*hi* is whenever it could be unclear.
  Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; Maltese also puts the subject after the verb, *qal Pawlu*, in dialogue
  tags). Maltese drops subject pronouns: write *hu/huwa*, *hi/hija*, *jien*, *int/inti* only where
  Maltese would (contrast, emphasis, a new subject, and *huwa/hija* as the copula: *Pawlu huwa
  tabib*). Little dialogue; when there is some, short and simple. *Int* to family, friends and
  children; to an older stranger, a customer or an official Maltese speakers use *int* too, or
  the polite *Sur*/*Sinjura* + surname (*Bonġu, Sur Borg*); never a second-person plural of
  politeness.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the imperfect (present: *jaħdem*, *imur*, *jgħid*, *jiġi*, *jaf*, *irid*) and
    the perfect (past: *ħadem*, *mar*, *qal*, *ġie*, *għamel*, *ra*), *għand-* for "have"
    (*għandi*, *għandu*, *għandha*), *kien/kienet* for "was", *hemm* "there is", the copula
    *huwa/hija/huma* or nothing (*Pawlu tabib*), negation *ma ... -x* (*ma jafx*, *ma marx*,
    *m'għandux*) and *mhux/mhix/mhumiex*, the article and its assimilation before the sun letters
    (*il-kelb*, but *id-dar*, *ix-xemx*, *iż-żiemel*, *in-nies*, *ir-raġel*, *is-sena*, *it-tifel*,
    *iċ-ċentru*; *l-* after a vowel: *u l-ħbieb*, *ta' l-*), the prepositions joined to it (*fil-*,
    *mill-*, *tal-*, *għall-*, *bil-*, *mal-*, *sal-*, *lill-*), *ta'* and *tiegħi/tiegħu/tagħha*,
    the kinship suffixes (*missieru*, *ommha*, *ħuh*, *oħtha*), numbers with nouns (*żewġ ulied*,
    *tliet snin*), simple questions (*fejn*, *min*, *x'*, *kif*, *meta*, *kemm*, *għaliex*);
  - ranks 301–600: *se/ser* + imperfect for the future (*se mmur*), *qed/qiegħed* + imperfect for
    what is going on (*qed jaħdem*), *kien* + imperfect for habits (*kien imur kull jum*), the
    imperative (*mur*, *ejja*, *ħu*, *agħti*, *għid*, *ara*), *irid*/*jista'* + imperfect (*irrid
    nara*, *tista' tiġi*), object suffixes (*rajtu*, *qaltlu*, *tah*, *ħadha*: check them with `rank.py`, many are missing), comparatives
    (*akbar minn*, *iżgħar*, *aħjar*, *agħar*);
  - ranks 601–1000: *li* relatives and *qal li* reported speech, *jekk* with the imperfect for real
    plans, the dual (*sentejn*, *jumejn*, *siegħtejn*), the common broken plurals (*djar*, *toroq*,
    *kotba*, *ulied*), *għax*, *meta*, *wara li*, *qabel ma*;
  - ranks 1001–2000: *kien qed* for the past continuous, *kieku* conditionals (*kieku kelli l-ħin,
    kont immur*), the passive-like verbs (*inbena*, *ntbagħat*, *instab*, *tkellem*), superlatives
    (*l-akbar*, *l-aħjar*), indirect questions (*ma jafx fejn hu*), *minkejja*, *biex*, *mingħajr
    ma*;
  - ranks 2001–4000: a richer register: connectors (*madankollu*, *għalhekk*, *filwaqt li*,
    *peress li*, *sakemm*, *għalkemm*), verbal nouns, a short column, a letter, a diary entry;
  - ranks 4001–6600 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes, the specialised words of professions, sciences
    and the arts made clear by their context.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a
  story may be told by a first-person narrator or as an e-mail, a letter or a diary entry. From
  rank 2000 a lesson may also be a short informative or opinion text, as long as it is calm and
  easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Mistoqsijiet:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  Mistoqsijiet:
  X'ma jafx Pawlu?
  Pawlu ma jafx fejn hu t-telefon tiegħu.
  Fejn hu t-telefon?
  It-telefon hu fil-karozza.
  ```

  The header `Mistoqsijiet:` is layout: the checker does not count it as a word.
- **Standard Maltese**: the narration is the standard written language of the Kunsill
  Nazzjonali tal-Ilsien Malti (*Deċiżjonijiet 1* and the current orthography), neutral, correct and
  natural Maltese as written today. Always write every *ċ*, *ġ*, *ħ*, *ż* and *għ*: the checker
  compares spellings exactly, and the letters tell words apart (*ħu* "brother" / *hu* "he"; *dar*
  "house" / *dar* is no *ħdar*; *ġie* "came" / *gie*; *żgħir* "small"). No English words where
  Maltese has a common word of its own (*grazzi*, not *thank you*; *iva*, not *yes*); English
  loans Maltese writes as its own (*kompjuter*, *mowbajl*, *televixin*, *futbol*) are fine in their
  Maltese spelling. No *ghandu*, *hafna*, *zgur* (letters without their marks), no SMS spellings.
- **The article and its hyphen**: the article is always joined with a hyphen and never spaced
  (*il-kelb*, *id-dar*); it assimilates before the sun letters ċ d n r s t x ż z; before a word
  starting with two consonants it takes an *i* (*l-iskola*, *l-ispiżerija*), and after a vowel it
  is *l-* (*u l-iskola*, *fl-aħħar*). Prepositions joined to it are one word with the hyphen
  (*fil-belt*, *tad-dar*, *mill-Mosta*, *għall-ikel*, *bil-karozza*, *mat-tifel*, *sas-7*).
- **The apostrophe**: *ta'*, *ma'*, *seta'*, *baqa'*, *qata'*, *beża'* end in an apostrophe that is
  part of the word (it stands for a lost *għ*); never drop it. *f'*, *b'*, *m'*, *x'*, *s'*, *t'* are
  *fi*, *bi*, *ma*, *xi*, *sa*, *ta'* cut short and written onto the next word (*f'Malta*, *b'mod*,
  *m'għandux*, *x'jagħmel*, *s'issa*); the reader and the checker cut them off as words of their
  own. Write the straight apostrophe (').
- **Maltese typography**: direct speech in double quotation marks, each speaker's line a paragraph
  of its own; the narrator's comma goes inside the closing mark:

  ```
  "Bonġu, Rita," qal Pawlu.
  "Kif int?" staqsiet Rita. "Trid kafè?"
  ```

  The questions and answers of the final `Mistoqsijiet:` block are plain lines. Write numbers as
  digits (*3 ulied*, *fis-7*, *fl-1992*), except a number word that is in your band: spell that one
  out to teach it (*żewġ ulied*, *tliet snin*). Write *is-Sur Borg*, never *Sur.*: the abbreviation
  with its full stop is no list word.
- **Names are names**: place names, adjectives made from place names (*Malti*, *Għawdxi*,
  *Ingliż*, *Taljan*) and words for peoples or languages are skipped as band words when they are
  names; a text may still use one (*jitkellem bil-Malti*): counted at its rank, or put in `names` when the list lacks it (*Malti*). Months, days of the
  week, seasons and language names are everyday words: teach them (*Jannar*, *is-Sibt*, *is-sajf*).
  Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material, and
  not the courses of the other languages either.

## Content

Any normal everyday, cultural or story topic is fine (the writers' brief, step 4): the festa and
the band clubs, the parish church, Christmas and Easter, the history of the knights and the
islands, politics and elections, myths and folk tales, crime and detective stories, sport. Avoid
only explicit sexual content, slurs used as insults and gratuitous gore. Teach ordinary words in
their main sense.

## The file

Write `tools/courses/mt/<id>.json`:

```json
{
  "id": "mt-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme in Maltese, from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The short English topic from themes.tsv",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Maltese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/mt/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *mar*, *imur* or *marret* for *mar*; *kbira* for *kbir*).
  `newWords` holds the word as the list writes it: a verb as its perfect 3rd person masculine
  (*ħadem*, *mar*, *qal*, *seta'*), a noun in the singular, an adjective in the masculine
  singular.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a list error, a name, a form of another word); the checker's last line lists the
  band words not taught yet.

Things to know about the Maltese list and the checker:

- The courses are pinned to `tools/courses/mt/wordlist.tsv` (a copy of the app's `mt.tsv`); the
  bands come from it and `extra_forms.tsv`. The list is counted from Leipzig's Maltese news,
  Wikipedia and web sentences and, at a tenth, the Maltese Wikipedia: news and church words rank
  high (*gvern* 49, *ministru* 77, *partit* 79, *knisja* 88, *qorti* 142), and everyday ones lower
  than you would think (*dar* 105, *tifel* 108, *mara* 134, *jum* 147, *omm* 308, *iva* 830,
  *kafè* 2258).
- **How the checker cuts words** (as the app's reader does): at the hyphen (*il-kelb* is *il* +
  *kelb*, *tad-dar* is *tad* + *dar*, *fl-aħħar* is *fl* + *aħħar*), and *f'*, *b'*, *m'*, *x'*,
  *s'*, *t'* off the word they are written onto. The article's shapes (*il*, *l*, *id*, *ix*, *iż*...)
  are forms of *il* 1; *tal*, *tad*, *tax*... of *ta'* 2; *fil*, *fl*, *fid*, *f'* of *fi* 4; *mill*,
  *mid* of *minn* 7; *għall*, *għad*... of *għal* 8; *bil*, *b'* of *bi* 9; *mal*, *mat* of *ma'* 16;
  *sal*, *sas* of *sa* 54; *x'* of *xi* 26; *m'* of *ma* 11.
- **Verbs are listed by their perfect** and hold the imperfect and the negatives: *qal* 24 holds
  *jgħid*, *tgħid*, *qalet*, *qalu*; *ġie* 19 *jiġi*, *ġiet*, *ġejt*; *mar* 83 *imur*, *jmur*, *marret*,
  *mur*; *ra* 80 *jara*, *rat*, *raw*, *rajt*; *ħa* 56 *jieħu*, *ħadet*; *ta* 48 is "gave" (*jagħti*,
  *tagħti*, *taw*); *kien* 6 holds *jkun*, *ikun*, *kienet*, *kont*; *għand* 22 is "have" (*għandi*,
  *għandu*, *għandhom*), *kell* 45 "had" (*kellu*, *kellha*, *kelli*). The forms with an object
  suffix (*qaltlu*, *tah*, *jarahx*) are often missing: write around them or report them.
- **Words with their own entries**: *huwa* 10 (*hu*), *hija* 27 (*hi*), *huma* 37; *dan* 12
  (*dawn*), *din* 18, *dak* 50, *dawk* 71; *ieħor* 32 (*oħra*, *oħrajn*); *wieħed* 35 (*waħda*);
  *kollu* 44 (*kollha*); *żewġ* 75 "two" before a noun, *tnejn* 238 alone.
- **Homographs**: *għad* (still) is its own entry but also a form of *għal*; *ta* "gave" and *ta'*
  "of" are two entries; *sur* is "Mr" and "city wall"; *qabel* is "before" and "agreed" (*jaqbel*);
  *nnifs-* is the entry for *nnifsu*, *innifsi*, *nfushom* ("himself..."). Use a new word in its own
  sense.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). Every headword
  in it must be a headword of the list. If a true form is missing, write around it and name the
  word and form in your report.
- **Names** are not in the list: people, places, shops and brands go in `names`. A name of several
  words (*San Ġwann*, *Ħal Qormi*, *Tas-Sliema*) counts as one known word wherever it is written
  whole. Keep only true names in `names`, never common words, and never a name that is also a list
  word: *ir-Rabat* (*rabat* 486), *il-Gżira* (*gżira* 184), *iż-Żebbuġ* (*żebbuġ* 995), *il-Belt*
  (*belt* 87: Valletta is taught as the word), *Grazzja* (*grazzja* 987) count at their own rank.
  Check a name's rank with `rank.py` before you use it.
