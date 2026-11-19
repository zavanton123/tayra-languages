# Writing a Manx frequency course

Each course teaches one band of 100 words from the Manx word frequency list (`bands/<rankUpTo>.txt`: index,
rank, word, its common forms). Course `gv-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300.
Ten lessons, each introducing about ten of the band's words, so the course covers the band. `themes.tsv` gives
each course its theme. The list has 5,884 words, so the courses stop at `gv-mini-5900` (ranks 5801–5884, a short
band).

## The register

**Modern standard Manx (Gaelg) as learners' books and Culture Vannin's materials write it**: the spelling of
Phil Kelly's *Fockleyr Gaelg-Baarle* and of the Manx Wikipedia, neutral, with no Late-Speaker dialect spellings
standing in for the standard. Keep to these choices:

| Point | Write | Not |
|---|---|---|
| "be" | *Ta / Cha nel / Vel?*; *Va / Cha row / Row?*; *Bee / Cha bee / Vel...?* future *Bee eh*; dependent *dy vel*, *nagh vel* | *T'eh* is fine, *Ta eh* too; one per story |
| Yes and no | repeat the verb: *Vel oo skee? Ta. / Cha nel.* *Haink eh? Haink. / Cha daink.* *Ren oo? Ren.* | *Ayyn* / *Aa* for a plain yes |
| "you" | *oo* to one person, *shiu* to several or politely; one form per pair of speakers | |
| "have" | *Ta thie ec Juan*, *Ta thie aym / ayd / echey / eck* | *Ta thie lesh Juan* |
| Feelings and liking | *Ta mee skee*, *Ta aggle orrym*; *S'mie lhiam*, *Cha mie lhiam* | |
| Article | *yn*, *y* before a consonant where usage has it (*y thie*, *y valley*), *ny* plural; *'n* after a vowel (*ta'n*, *jeh'n*, *veih'n*); *'sy* / *'syn* "in the" (*'sy thie*, *'syn ellan*) | *ayns y thie* is fine too |
| Mutations | lenition after *e* "his", feminine nouns, *y* with feminine nouns (*e hie*, *ben vie*, *y ven*), eclipsis after *nyn* (*nyn dhie*), the *h-* and *n-* before vowels (*ny h-ellanyn*) | |
| Short forms | *t'eh*, *t'ee*, *t'ad*, *v'eh*, *s'mie*, *d'eash*, *ta'n*, *chammah's* | |

**Spelling**: the *ç* in *çh* (*çhengey*, *çheer*, *çheet*), *ny*, *nyn*, *jeh*, *veih*. The list holds the forms texts write,
and the checker compares spellings, so a misspelling is an outside word. No English mixed into a sentence. Everyday
expressions in dialogue are fine (*moghrey mie*, *fastyr mie*, *oie vie*, *kys t'ou?*, *gura mie ayd*, *slane lhiat*).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain, clear and
predictable, so the learner's attention goes to the language. A course is ten separate stories on one broad theme (its
title), not one serial story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple ending. Mostly
  modern everyday life on the Isle of Man (Ellan Vannin): Doolish, Rhumsaa, Purt ny h-Inshey, Purt Çhiarn, Balley Chashtal,
  Purt le Moirrey, Pheeil, the villages and farms, the Bunscoill Ghaelgagh, families who speak Manx at home; and now and
  then Ireland, Scotland or England (Nerin, Nalbin, Sostyn). Any everyday or cultural topic is fine (WRITER-BRIEF step 4).
- **Nothing confusing**: no twists, mysteries, flashbacks or open endings. Events in order. One to three characters, named
  clearly; say who *eh*/*ee* is whenever it could be unclear. Each story its own names.
- **Plain narration**: third person, short sentences, Manx order: verb, subject, object (*Ta Juan ayns y thie.* *Hie Breesha
  dys y valley.* *Ren eh y thie y ghlenney.*). Little dialogue early; speech tags *ta X gra*, *dooyrt X*.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *ta / cha nel / vel?* and *va / cha row / row?*; the progressive *ta eh goll*, *ta ee cloie*; the perfect
    with *er* (*t'eh er çheet*); the copula *she* (*She Juan yn ennym echey.* *She ben vie ee.*); the common pasts (*hie*, *haink*,
    *ren*, *hooar*, *hug*, *dooyrt*, *honnick*, *ghow*) with their dependent forms (*cha daink*, *row?*); having with *ec*,
    the prepositional pronouns (*aym*, *orrym*, *lhiam*, *dou*, *rhym*); *e*, *my*, *dty*, *nyn* with their mutations;
    numbers with nouns (*daa vlein*, *three thieyn*);
  - ranks 301–600: the future (*hed*, *hig*, *nee*, *yiow*, *ver*, *jir*; *bee*), the imperative (*tar*, *immee*, *jean*, *ny jean*);
    comparison (*ny smoo na*, *ny share na*, *wheesh as*); *foddym / foddee*, *shegin dou*; the time, days and months;
  - ranks 601–1000: the conditional (*veagh*, *yinnin*, *harragh*); relative clauses (*yn fer va ayns y thie*); reported speech
    with *dy* / *nagh* (*Dooyrt ee dy vel ee skee.*); *tra*, *my* + future, *son*, *agh*;
  - ranks 1001–2000: the past habitual, the passive with *v'eh er ny...* and *-it* participles (*troggit*, *screeuit*), *lurg da*,
    *roish my*, purpose with *dy* + verbal noun, the object before the verbal noun (*Ren eh y lioar y lhaih.*), indirect questions;
  - ranks 2001–4000: a fuller written style: clauses inside one another, connectors (*ansherbee*, *er-y-fa shen*, *er y laue elley*),
    sayings explained by context; first-person stories, letters, diary entries and short opinion texts are fine;
  - ranks 4001–5900 (C1): abstract and specialised content (the essay, the interview, the review, science and the arts), made
    clear by its context.
- **Repetition on purpose**: new words three times or more where it reads naturally.
- **Questions at the end**: after the story, a paragraph starting `Feyshtyn:` with 3–6 simple questions, each followed on its
  own line by a full-sentence answer that reuses the story's words:

  ```
  Feyshtyn:
  Vel thie ec Juan?
  Ta. Ta thie beg ec Juan.
  ```

  The header `Feyshtyn:` is layout: the app and the checker do not count it as a word.
- **Typography**: direct speech in curly double quotation marks “…”, a speaker's line a paragraph of its own. The apostrophe may be
  straight or curly. Numbers as digits unless the number word is in your band.
- **Original texts**: write your own stories; never copy or retell other material.

## The file

`tools/courses/gv/<id>.json`, the shape of `gv-mini-0100.json`: `id`, `rankUpTo`, `level`, `title` (as themes.tsv writes it),
`description`, `topic`, `names`, `lessons` (each `title`, `summary`, `newWords`, `text`). Levels: up to rank 300 `A1`, up to
1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/gv/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words ranked within `rankUpTo` (93% up to rank 300, 90% for the first course). Names
  in `names` and numbers count as known.
- **New words**: each in the band, introduced once in the course, used at least twice in its lesson (any listed form counts).
  `newWords` holds the word as the band file writes it (third column).
- **Length** (story and questions): 80–180 words in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380 up to
  1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim for 90 of the 100 words; skip list noise (see the writers' notes).

## How the checker and the reader cut Manx

As the app's reader (ManxParser) does: *t'eh* is *t'* + *eh* (*t'* is a form of *ta*), *v'ad* is *v'* + *ad*, *s'mie* is *s'* + *mie*,
*d'eash* is *d'* + *eash*, *dt'ennym* is *dt'* + *ennym*, *m'* is *my*'s; *ta'n* is *ta* + *'n* (a form of *yn*), *chammah's* is *chammah* +
*'s* (*as*). An apostrophe at a word's edge is no part of it (*'sy* is *sy*), the *h-*, *n-*, *t-* before a vowel is dropped (*ny h-ellanyn*
is *ny ellanyn*), and a hyphen parts two words (*lught-thie* is *lught* + *thie*). **The checker guesses no mutation**: a mutated spelling
the list lacks is an outside word; check it with `rank.py` and report it so it goes into `extra_forms.tsv`.
