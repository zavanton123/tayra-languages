# Writing a Breton frequency course

Each course teaches one band of 100 words from the Breton word frequency list (`bands/<rankUpTo>.txt`:
index, rank, word, its common forms). Course `br-mini-0300` teaches ranks 201–300 and may use every word
ranked 1–300. Ten lessons, each introducing about ten of the band's words, so the course covers the band;
a learner who finishes it knows the 300 most common words. `themes.tsv` gives each course its theme. The
list has 5,170 words, so the courses stop at rank 5200 (the last band has 70 words).

## The register

**One register in every course: modern standard Breton for learners, in the unified spelling
(*peurunvan*)**: plain, correct Breton as learners' books, Diwan schools and the Breton Wikipedia write it,
neutral between the dialects, with no dialect forms standing in for the standard. Keep exactly to these
choices:

| Point | Write | Not |
|---|---|---|
| Spelling | *zh* (*brezhoneg*, *Breizh*, *kreisteiz*), *c'h* (*c'hoazh*, *merc'h*), *ñ* for the nasal vowel (*bremañ*, *kentañ*), *ê* in *kêr*, *gêr*, *stêr* | dialect spellings (*brehoneg*, *get* for *gant*, *àr* for *war*), *ch* for *c'h* |
| Apostrophe | the plain ' or the curly ’: *c'hoazh*, *d'ar*, *n'eo* | the modifier letter ʼ (*cʼhoazh*): the reader cuts the word there |
| Word order | the verb second: one element first, then the verb. *Yann a zebr bara.* *Bara a zebr Yann.* *Debriñ a ra Yann.* After a fronted time, place or clause the verb follows with *e* (*ez* before a vowel): *Hiziv e tebr Yann bara.* *Er gêr emañ Yann.* *Pa zeu Anna e welan anezhi.* | *Hiziv, Yann a zebr bara.* |
| "be" | *emañ* for where someone is and with *o* + verbal noun (*Emañ Yann er gêr.* *Emañ Anna o lenn.*); *eo* after a fronted predicate (*Bras eo.* *Kelenner eo Anna.* *Yann eo e anv.*); *a zo* after the subject (*Yann a zo er gêr.*); *ez eus* / *n'eus ket* for "there is"; *e vez* for what happens as a rule | *eo* for where (*Yann eo er gêr*) |
| "have" | *kaout*: *am eus, ac'h eus, en deus, he deus, hon eus, hoc'h eus, o deus*; past *am boa, en doa, he doa, o doa*; future *am bo, en devo, he devo* | |
| "you" | *te* to a friend, a child or a relative; *c'hwi* to several people and to anyone you would address politely. One form per pair of speakers | |
| Negation | *ne* … *ket*, *n'* before a vowel, with the soft mutation after *ne*: *ne zeu ket*, *ne gav ket*, *ne welan ket*, *n'eo ket*, *n'eus ket*; *den ebet*, *netra*, *biskoazh*, *morse* with *ne* too | *ket* alone |
| Past | the perfect in stories (*Yann en deus gwelet ar mor.* *Anna a zo aet da Vrest.*) and the imperfect for what was going on (*e oa*, *e veze*, *emañ* → *edo*); the preterite (*gwelas*, *a reas*, *a yeas*) only from rank 1000, in written narration | |
| Articles | *an* before a vowel and *d, h, n, t* (*an den, an ti, an hent*), *al* before *l* (*al levr*), *ar* elsewhere (*ar mor*); *un, ul, ur* the same way; *en* for "in a", *er, el* for "in the" | *ar ti*, *ur ti* |
| Questions | *Petra*, *piv*, *pelec'h*, *penaos*, *perak*, *pegoulz*, *pet* + the verb with *e* or *a* (*Pelec'h e vev Yann?* *Piv a zo aze?*); yes/no with *ha* (*Ha bras eo?*) or *daoust ha* | |

**The mutations are grammar, not spelling**: write every soft, spirant, hard and mixed mutation the standard
requires and none that it does not: *ur gêr*, *ar vugale*, *e vab* "his son", *he zad* "her father", *ma
c'hêr*, *ho preur*, *da Vrest*, *daou vab*, *tri c'hi*, *a gav*, *e kav*, *ne gav ket*, *o tont*, *o vont*,
*pa zeu*, *ur vamm vat*, *div wech*, *meur a wech*. The checker reads a mutated form as its word but never tells
you whether it is right: *e mab* and *e vab* pass alike, and only the second is Breton.

Spell everything as the unified spelling does; the checker compares spellings. No slang and no French mixed
into a sentence. Everyday expressions are fine in dialogue: *demat*, *mat an traoù?*, *trugarez*, *kenavo*,
*mar plij*, *noz vat*, *yec'hed mat!*

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten
separate stories on one broad everyday theme (its title), not one serial story; a lesson never needs another
lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Modern everyday life where Breton is spoken: the towns and the country of Lower Brittany (Kemper,
  Brest, Montroulez, Lannuon, Gwengamp, Karaez, Douarnenez, Kemperle, An Oriant, Gwened, Pondi), Roazhon,
  Naoned and Sant-Brieg, the islands, Diwan schools, families who speak Breton at home, Bretons in Paris or
  abroad: home, family, food, shopping, work, school, the town, health, free time, travel, the *fest-noz*,
  the *bagad*, the sea.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony, jokes that
  depend on wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *eñ*/*hi* is whenever it could be unclear. Give each story its own
  names.
- **Plain narration**: third person, short sentences, one idea per sentence, the verb second (see the table).
  Little dialogue; when there is some, short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present with the subject first (*Yann a zebr*, *Anna a gav*), the fronted verbal noun with
    *ober* (*Debriñ a ra Yann.*), *emañ*, *eo*, *a zo* and *ez eus*; *kaout* in the present (*en deus*, *he deus*,
    *o deus*); *ne* … *ket*; possessives with their mutations (*ma zad*, *da vamm*, *e vab*, *he zad*, *hor bro*,
    *ho preur*, *o mamm*); the article and its mutations; *o* + verbal noun (*Emañ o tont.*); questions with
    *petra*, *piv*, *pelec'h*, *penaos* and *ha*; numbers with the singular noun (*daou vab*, *div verc'h*, *tri
    bloaz*, *teir gwech*); the prepositions with their personal forms (*din*, *dezhañ*, *gantañ*, *ganti*,
    *anezhañ*, *warnañ*);
  - ranks 301–600: the perfect (*en deus gwelet*, *a zo aet*, *on deuet*), the future (*e welo*, *e teuio*, *ez
    aio*, *e vo*), the imperative (*deus!*, *kae!*, *grit!*, *na zeuit ket!*); the personal forms of the verb after
    *e* and *ne* (*e welan*, *ne ouzon ket*, *e komzomp*); comparatives (*brasoc'h eget*, *ken bras ha*), *gallout*,
    *rankout*, *fellout* (*Me am eus c'hoant*, *Ret eo din*); the time, the days (*d'al Lun*, *dilun*) and months
    (*e miz Genver*);
  - ranks 601–1000: the imperfect (*e oa*, *e veze*, *edo*, *e rae*), the conditional (*e vefe*, *e c'hallfen*, *ma
    vefe*); relative clauses (*an den a welan*, *al lec'h ma vev*); reported speech with *e* / *ez* (*Lavaret en
    deus e teuio.*); *pa*, *ma*, *abalamour ma*, *evit ma*;
  - ranks 1001–1300: the preterite in written narration (*a reas*, *a yeas*, *a welas*), the superlative (*ar
    brasañ*, *ar gwellañ*), the past participle as an adjective (*digor*, *serret*), *o vezañ* and *goude bezañ*;
  - ranks 1301–1600: the impersonal (*e vez lavaret*, *graet e oa bet*), *ma* + conditional for what is not so
    (*Ma vefe amzer am bije...*), *daoust ma*, *a-raok ma*, *betek ma*;
  - ranks 1601–2000: the verbal noun with its object (*evit kompren ar gentel*), the reflexive *en em* (*en em
    gavout*, *en em walc'hiñ*), indirect questions (*N'ouzon ket hag-eñ e teuio.*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another and the long forms of the verb (*e
    vezer*, *bez' e vo*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *koulskoude*, *evelkent*, *dre-se*, *e
    gwirionez*, *a-hend-all*; common sayings and idioms explained by the context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and clear
    images, the newspaper column (a light reflection on everyday life);
  - ranks 4001–5200 (C1): more abstract and nuanced content: a short essay or reflective column that weighs two
    sides, an interview, a portrait, a review; the more specialised words of professions, sciences and the arts,
    each made clear by its context; long sentences that stay easy to parse.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story may be told
  by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a
  short informative or opinion text, as long as it is everyday, calm and easy to follow. A learner at the level
  should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words three times or
  more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Goulennoù:` with 4–6 simple questions about
  the story, each followed on its own line by a full-sentence answer that reuses the story's words (after rank
  1000 they may also ask *perak* and *penaos*, with fuller answers):

  ```
  Goulennoù:
  Pelec'h e vev Yann?
  Yann a vev e Kemper.
  Pet mab en deus Yann?
  Yann en deus daou vab.
  ```

  The header `Goulennoù:` is layout: the checker does not count it as a word, and the app cuts the questions
  off into a stage of their own.
- **Breton typography**: direct speech in straight or curly double quotation marks "…" or “…”, each speaker's
  line a paragraph of its own or inside the narration; the tag is *a lavar X* after the quote (*"Demat!" a lavar
  Anna.*). The questions and answers of the `Goulennoù:` block are plain lines. A dash is the en dash with spaces
  ( – ). Write numbers as digits (*da 8 eur*, *e 1995*, *5 euro*), except a number word that is in your band:
  spell that one out to teach it (*daou vab*, *tri bloaz*).
- **Names**: never use a list word as a character's name, nor put one in `names`.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's or any
  other), and not the courses of the other languages either.

## Days, months, people and places

**Days, months and seasons are everyday words: teach them** when your band has them (*lun*, *meurzh*,
*merc'her*, *yaou*, *gwener*, *sadorn*, *sul*; *genver*, *c'hwevrer*, *meurzh*, *ebrel*, *mae*, *mezheven*,
*gouere*, *eost*, *gwengolo*, *here*, *du*, *kerzu*; *nevez-amzer*, *hañv*, *diskar-amzer*, *goañv*). Write the
day and the month with a capital (*d'al Lun*, *e miz Genver*), the forms with *di-* in one word (*dilun*, *disul*).
*meurzh* is both "Tuesday" and "March", *du* both "black" and "November".

Names free to use (none is a list word): *Yann, Anna, Erwan, Tangi, Mona, Loeiz, Riwal, Soazig, Youenn, Katell,
Klervi, Goulven, Enora, Aziliz, Morgan, Nolwenn, Iwan, Ronan, Maela, Brieg, Gwenola, Fañch, Lena, Annaig, Jakez,
Herve, Mari, Yannig, Gwenael, Maiwenn, Bleuenn, Mikael, Glenn, Tudual, Gaid, Visant, Job, Marivonn, Ewen,
Gwendal, Jord, Mael, Kaourantin, Gireg, Oanez, Morwenna, Kristell, Nikolaz, Erell, Loig, Yaouen, Meriadeg, Divi*.
**Not** names here, because they are list words or forms: *Alan, Per, Paol, Malo, Kristen, Armel, Rozenn,
Sterenn, Paskal, Gwenn*. Check any other name with `rank.py`. Places free to use: *Breizh, Kemper, Brest,
Roazhon, Naoned, Gwened, Montroulez, Lannuon, Karaez, Gwengamp, Douarnenez, Landerne, Pondi, Dinan, Kemperle,
Kraozon, Pempoull, Pariz, Iwerzhon, Kembre, Kanada*, and whole in `names`: *Sant-Brieg, An Oriant, Konk-Kerne,
Kastell-Paol, Sant-Maloù, Enez-Vaz, Bro-C'hall*. Write a name as the grammar requires (*da Vrest*, *da Gemper*,
*e Brest*, *da Vaela*); the checker reads a mutated name as the name (*Vrest*, *Gemper*, *Dangi*).

## The file

Write `tools/courses/br/<id>.json`:

```json
{
  "id": "br-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme's Breton title, as themes.tsv writes it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Breton",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/br/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names you list in `names` and numbers count as known. The checker lists every word
  outside the range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson (any form counts, mutated too: *zeu* or *deuet* for *dont*; *vras* for *bras*; *dezhañ* for *da*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it (the third column of the
  band file).
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up to rank 300,
  180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth a lesson (a
  form or a spelling of another word, a list error such as a name or a piece of a place name); the checker's last
  line lists the band words not taught yet.

Things to know about the Breton list and the checker:

- The courses are pinned to `tools/courses/br/wordlist.tsv`, a copy of the app's `br.tsv`. Its only source is
  Leipzig's 100,000 sentences of the Breton Wikipedia (the subtitles have 7,000 forms), so history, kings, wars and
  the Church stand early (*roue* 63, *brezel* 87, *mervel* 78) and kitchen words late (*bara* 984, *kafe* 2437).
  Check with `rank.py`; say *tra*, *ti*, *kêr* where the level needs it.
- **Elided words are two words**: the reader keeps *d'ar*, *n'eo*, *m'en*, *p'edo*, *ma'z* whole, and the checker
  counts them as the two words they are (*da* + *ar*, *ne* + *eo*). *c'h* is one letter.
- **Hyphenated words**: the list has many whole (*e-barzh*, *a-raok*, *war-lerc'h*, *dreist-holl*, *tad-kozh*), and
  the checker counts a hyphenated word whole where the list has it, its parts otherwise (*mamm-gozh* is whole,
  *ar miz-se* is *miz* + *se*).
- **Mutations**: a mutated form the list lacks counts as the word it mutates (*vreudeur* is *breudeur*, *welan* is
  *gwelan*, *Vrest* is *Brest*). A mutated spelling that is itself a list word or form counts as that: *varv* is
  *marv* "death", so *a varv* "dies" does not count for *mervel* (write *mervel a ra*, *marvet eo*).
- **Verb forms**: the list has the forms Wikipedia uses, mostly the third person; `extra_forms.tsv` adds the
  personal forms of the verbs from the dictionary pack (*gwelan*, *kavomp*, *deuin*) and of *e*, *bezañ*, *kaout* and
  *evit* (*ennañ*, *emaoc'h*, *edon*, *am boa*, *evidout*). *ra*, *ran*, *rez*, *reont* are *ober*'s; *ya* is "yes",
  not *mont*'s, and *ez a* is *ez* + *a* (write *mont a ra*, *aet eo*, *ez aio*); *bout* is a spelling of *bezañ* and is skipped.
- **Spellings shared by two words**: a form counts for the word that holds it, so look it up with `rank.py`
  before relying on it: *deus* is *dont*'s and *kaout*'s, *bet* is *bezañ*'s (and *kaout*'s), *gar* is "leg", *c'hall* is *gallout*'s
  (*ne c'hall ket*), not *gall*'s.
