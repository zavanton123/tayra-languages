# Writing a Cornish frequency course

Each course teaches one band of 100 words from the Cornish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `kw-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band. The list has 5,137 words, so there are 52 courses; the last
band (5101–5137) is short. `themes.tsv` gives each course its theme.

## The register and the spelling

**Revived Cornish in Kernewek Kemmyn (KK) spelling, the spelling of the list's headwords.** The list
is counted from Cornish Wikipedia, which mixes KK, the Standard Written Form (SWF) and Late Cornish;
the builder files the other spellings under the KK word (*dhiworth*, *dhort* under *dhyworth*; *radn*
under *rann*; *whedhel* under *hwedhel*), so write the KK headword and its KK forms:

| Point | Write | Not |
|---|---|---|
| Spelling | *kath*, *skol*, *hwedhel*, *penn*, *kemmyn*, *dhyworth* | *cath*, *scol*, *whedhel*, *pedn*, *kebmyn*, *dhort* |
| "is" (identity, quality) | *Tamsin yw benyn.* *An dre yw bian.* | *Tamsin ew…* |
| "is" (place, there is) | *Yma Tamsin yn Truru.* *Yma lies tre…*; questions and negatives *usi* (definite), *eus* (indefinite) | *Ma Tamsin…* |
| Present of a verb | particle *a* + soft mutation: *Hi a gews Kernewek.* *Ev a woer.* *I a dheu.* | *Hi kews…* |
| Progressive | *Yma Jago ow kewsel.* *ow tos*, *ow kul*, *ow talleth* (mixed mutation after *ow*) | |
| Preterite | *Hi a skrifas.* *Ev a dheuth.* *Hi a veu genys.* *Ev a wrug.* | |
| Perfect | *re* + soft: *Hi re dhallathas.* | |
| Negative | *Nyns yw…*, *Nyns usi…*, *Ny yll hi…* (soft after *ny*) | |
| Yes/no answers | repeat the verb: *Yw. / Nag yw.*, *Usi. / Nag usi.*, *Gews. / Na gews.*, *Dheuth.*, *Goer.* | *Ya*, *Na* alone |

Write every initial mutation the grammar requires: soft after *an* with a feminine singular noun (*an
dre*, *an vledhen*), after *y* "his", *dhe*, *a* "of", *re*, *ny*, *pan*, *dew/diw* (*dew vis*, *diw
yeth*) and with an adjective after a feminine noun (*tre vian*, *yeth vras*); aspirate after *hy*
"her", *ow* "my", *aga* "their" (*hy thre*, *aga thre*); mixed after the verbal *ow* and *yn* +
adjective. The checker reads a mutated form as its word but never tells you whether the mutation is
right.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories":
plain, clear and predictable. A course is ten separate stories on one broad theme (its title), not a
serial; a lesson never needs another lesson to make sense.

- **One simple situation per story**, in today's Cornwall where Cornish is learnt and spoken: the
  towns (Truru, Pennsans, Lulyn, Porthia, Aberfal, Bosvenegh, Lyskerrys, Kammbronn, Resrudh,
  Lannstevan), the villages, the coast and the islands, Cornish classes and groups, families who
  speak Cornish at home; now and then Loundres, Breten Vyghan or anywhere else.
- **Nothing confusing**: no twists, flashbacks or open endings; events in order; one to three
  characters, named clearly; say who *ev*/*hi* is whenever it could be unclear.
- **Plain narration**: third person, short sentences, little dialogue. *ty* to a friend, a child or a
  relative; *hwi* to several people and politely.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *yw*, *yma*/*usi*/*eus* and their questions and negatives; the verbal particle *a*
    with the present and preterite of common verbs (*a gews*, *a skrifas*, *a dheuth*, *a veu genys*);
    *ow* + verbal noun; the article and its soft mutation; possessives and their mutations; numbers
    with the singular (*dew vis*, *diw vledhen*); *gans*, *dhe*, *dhyworth*, *war*, *yn* and the
    contractions *y'n*, *dhe'n*, *a'n*, *ha'n*;
  - ranks 301–600: the imperfect (*o*, *esa*, *a wodhya*), the future of *bos* (*bydh*), commands,
    *res yw dhe X* (must), comparatives with *ages* (*brassa ages*), times and dates;
  - ranks 601–1000: the conditional (*a vynsa*, *via*), clauses with *bos* (*Hi a lavar ev dhe vos
    klav*), *drefen* + clause, *pan*, *mar*, *mes*, *ytho*; *gallos* and *mynnes* + verbal noun;
  - ranks 1001–2000: inflected prepositions in full (*ganso*, *dhedhi*, *warnodho*), the passive with
    *-ir*/*-ys* and *bos*, relative clauses, reported speech;
  - ranks 2001–4000: a fuller written style, connectors (*byttegyns*, *ytho*, *awos*), idioms
    explained by their context, first-person narrators, letters, short columns;
  - ranks 4001–5137 (C1): short essays, interviews, portraits, reviews; the specialised words of the
    sciences and the arts, made clear by their context.
- **Repetition on purpose**: each new word three times or more where it reads naturally.
- **Questions at the end**: after the story, a paragraph starting `Govynnow:` with 4–6 simple
  questions, each followed on its own line by a full-sentence answer reusing the story's words:

  ```
  Govynnow:
  Yw Pennsans tre vras?
  Nag yw. Pennsans yw tre vian.
  ```

  The header `Govynnow:` is layout: the checker (and the app) take it off.
- **Typography**: direct speech in curly double quotation marks “…”. The apostrophe of a contraction
  may be straight or curly (*y'n*, *y’n*). Numbers as digits unless the number word is in your band.
- **Names are names**: a list word never goes in `names`. Place names count as names (*Kernow*, *Truru*,
  *Pennsans*: list only the radical; the checker reads *dhe Gernow*, *dre Druru*).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

Since 2026-11-13 there are no topic rules (see WRITER-BRIEF step 4): religion, history, politics, love,
death and so on are fine as everyday topics. Days, months and language names are words: teach them
(*Genver*, *Ebrel*, *Hedra*, *Kernewek*, *Sowsnek* are list words).

## The file

`tools/courses/kw/<id>.json`, the same shape as every other language's (see `kw-mini-0100.json`): `id`,
`rankUpTo`, `level`, `title` (Cornish), `description`, `topic` (English), `names`, `lessons` (each with
`title`, `summary`, `newWords`, `text`). Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`,
up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/kw/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words ranked within the course's `rankUpTo` (93% up to 300,
  90% for the first course); names and numbers count as known.
- **New words**: in the band, introduced once in the course, used at least twice in their lesson (any
  form, mutated too). `newWords` holds the word as the band file writes it (a verb as its verbal noun).
- **Length** (story and questions): 80–180 words in the first course, 120–240 up to 300, 180–320 up to
  600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim for at least 90 of the 100 words; skip list errors, forms of other words,
  pieces of names.

Things to know about the Cornish list and the checker:

- The courses are pinned to `tools/courses/kw/wordlist.tsv`, a copy of the app's `kw.tsv`, counted from
  the articles of Cornish Wikipedia only, so encyclopaedia words rank high (*poblans* 28, *konteth* 37,
  *restren* 47, *mildir* 72) and kitchen words low.
- **Mutations**: a mutated form the list lacks counts as the word it mutates (*vledhen* is *bledhen*,
  *Gernow* is *Kernow*). But a mutated form that is itself a list word counts as that word: **an dre "the
  town" reads as dre "through"** (38), *va* as *ev* and *ma*. Use *tre* itself for its two uses.
- **Contractions are words of their own**: *y'n* 8, *a'n* 17, *dhe'n* 23, *ha'n* 41, *ha'y* 169, *a'y*,
  *dh'y*, *y'ga*, *ha'ga*, *a's* are list entries; teach them as such.
- **Hyphenated words** are one word where the list has them (*a-dro*, *a-dhia*, *yn-dann*, *war-lergh*)
  and their parts otherwise.
- Look a word up with `rank.py` (in the writers' folder) before relying on a form: *henow* reads as *gans*,
  *kyv* as *gyv*; `extra_forms.tsv` adds forms the list lacks (*goer*/*woer* of *godhvos*, *gyll* of
  *gallos*).
