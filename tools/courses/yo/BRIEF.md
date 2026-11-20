# Writing a Yoruba frequency course

Each course teaches one band of 100 words from the Yoruba word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common spellings). Course `yo-mini-0300` teaches
ranks 201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the
band's words, so the course covers the band; a learner who finishes it knows the 300 most common
words. `themes.tsv` gives each course its theme. The list has 3,050 words, so the last course is
`yo-mini-3100` (50 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Bọ́lá ń wá ìwé rẹ̀. Ó lọ sí yàrá. Kò rí i. Ó lọ sí ìdáná...* Everyday life where
  Yoruba is spoken: Lagos (Èkó: Ìkẹjà, Yaba, Surulere, Lekki), Ìbàdàn, Abẹ́òkúta, Ọ̀yọ́, Òṣogbo,
  Ilé-Ifẹ̀, Ìlọrin, Àkúrẹ́, Adó-Èkìtì, Ìjẹ̀bú-Òde, Ògbómọ̀ṣọ́, Ọ̀ndó; Porto-Novo, Kétu and Ṣábẹ̀ in
  Benin; now and then London, Houston or Salvador da Bahia, where Yoruba speakers live: home,
  family, food, the market, work, school, the town, church and mosque, festivals, free time,
  travel. Any everyday, cultural or story topic is fine (festivals, history, the Ọ̀ọ̀ni and the
  Aláàfin, folk tales of Àjàpá the tortoise, sport, love, a detective story); only explicit sexual
  content, slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *ó* and *òun* are whenever it could be unclear. Give each
  story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Yoruba
  order: subject, verb, object (*Bọ́lá ra ìwé.*), the noun before its modifiers (*ilé ńlá*, *ọmọ
  mi*, *ìwé yìí*, *ilé náà*, *ọmọ méjì*). Little dialogue; when there is some, short and simple.
- **Yoruba has no inflection, so the grammar is particles and word order.** A verb never changes;
  tense and aspect are words before it: *ó lọ* (went), *ó ti lọ* (has gone), *ó ń lọ* (is going),
  *ó máa ń lọ* (usually goes), *ó máa lọ* / *yóò lọ* / *á lọ* (will go), *kò lọ* (did not go),
  *kò ní lọ* (will not go), *kì í lọ* (never goes). Each of these particles is a word of its own in
  the list: teach them in context, not as a grammar lecture.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the subject pronouns *mo, o, ó, a, ẹ, wọ́n* and the emphatic *èmi, ìwọ, òun, àwa,
    ẹ̀yin, àwọn*; the object pronouns (*mi, ọ, wa, yín, wọn*, and the third person, which copies
    the verb's vowel: *rí i*, *gbé e*, *pa á*, *mọ̀ ọ́*); possessives after the noun (*ilé mi, ilé
    rẹ, ilé rẹ̀*); *ni* for "it is" (*Túndé ni*), *jẹ́* "be" (*ó jẹ́ olùkọ́*), *wà* "be at, exist" (*ó wà
    nílé*), *ní* "have" (*mo ní ọmọ méjì*); the aspect particles above, the negatives *kò* and *ò*
    (*mi ò mọ̀*); the plural *àwọn* before the noun; *yìí*, *náà*, *kan*; *sí* and *ní* for "to" and
    "at"; questions with *ṣé*, *ta ni*, *kí ni*, *ibo*, *báwo*, *mélòó*; numbers one to ten;
  - ranks 301–600: the serial verbs (*ó mú ìwé wá* "brought the book", *ó gbé e lọ*, *ó fi ọ̀bẹ gé
    e*), the subjunctive and commands with *kí* and *má* (*ó ní kí n wá*, *má lọ*, *ẹ jẹ́ ká lọ*),
    *tí* clauses (*ọmọ tí mo rí*, *nígbà tí ó dé*), *bí* and *tí ... bá* conditions (*bí o bá fẹ́*),
    comparison with *ju ... lọ* (*ó tóbi ju ilé mi lọ*), the focus with *ni* (*Túndé ni ó wá*, *ìwé ni
    mo rà*);
  - ranks 601–1000: the gerunds made with a repeated consonant (*rírà*, *jíjẹ*, *kíkà*), the noun
    makers *ì-*, *à-*, *oní-*, *olù-* and *aláì-* in the list's words (*ìrìn*, *olùkọ́*, *aláìsàn*),
    *pé* clauses after *sọ*, *rò*, *mọ̀* (*ó sọ pé òun ń bọ̀*), the logophoric *òun* in reported
    speech;
  - ranks 1001–2000: longer joined sentences (*nítorí pé*, *ṣùgbọ́n*, *bí ó tilẹ̀ jẹ́ pé*, *kí ... tó*
    "before", *lẹ́yìn tí* "after"), reported speech, a first-person narrator, a letter or a diary;
  - ranks 2001–3100 (B2): a richer register: connectors (*síbẹ̀síbẹ̀*, *nítorí náà*, *lọ́nà kejì*),
    the newspaper column, dialogue, proverbs (*òwe*) explained by their story.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ó lọ sí yàrá. Kò rí ìwé. Ó lọ sí ìdáná. Kò rí ìwé.*).
- **Questions at the end**: after the story, a paragraph starting `Àwọn ìbéèrè:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kí ló dé* and *báwo*, with fuller answers):

  ```
  Àwọn ìbéèrè:
  Kí ni Bọ́lá ń wá?
  Bọ́lá ń wá ìwé rẹ̀.
  Ibo ni ìwé náà wà?
  Ìwé náà wà ní ìdáná.
  ```

  The header `Àwọn ìbéèrè:` is layout: the checker does not count it as a word.

## Spelling: every tone and every dot

- **Write standard Yoruba with all its marks**: the grave for the low tone (*ọ̀rọ̀*), the acute for
  the high tone (*ọjọ́*), no mark for the mid tone (*ara*), the dot below for *ẹ, ọ, ṣ*, and the
  tone on the syllabic *n* (*ń lọ*, *ǹjẹ́*, *nǹkan*). The list, the reader and the checker match
  words by their marks: *ọkọ* "husband", *ọkọ̀* "vehicle" and *oko* "farm" are three words, and a word
  written without its marks (*awon*, *ati*, *eniyan*) is an outside word, not the list's.
- Write in NFC (the precomposed letters ẹ ọ ṣ á à é è í ì ó ò ú ù ń; *ẹ́, ẹ̀, ọ́, ọ̀* are the dotted
  letter and a combining mark, which is fine). The checker reads either; keep the file NFC.
- **Tone changes are written as they are said**: a low-tone verb of one syllable takes the mid tone
  before its object (*rà* "buy": *mo ra ìwé*; *mọ̀* "know": *mo mọ ọ̀nà*; *gbà*: *gba owó*; *kà*:
  *ka ìwé*): the list counts that spelling as the verb's. Write the vowel the elision leaves as the
  standard spelling does (*nílé* "at home", *lórí*, *nínú*, *lọ́wọ́*, *sílẹ̀*, *lọ́jà*): those joined words
  are words of the list (*ní ilé* written apart is two).
- **Hyphens part words** for the reader and the checker: *orílẹ̀-èdè* is *orílẹ̀* + *èdè*, *gan-an*
  is *gan* + *an*, *ilé-ìwé* is *ilé* + *ìwé*. Prefer the spaced or hyphenated standard forms; the
  list has the parts.
- **Typography**: direct speech in double quotation marks "…", each speaker's line a paragraph of
  its own. Write numbers as digits except a number word that is in your band: spell that one out
  to teach it (*ọmọ méjì*). No apostrophe elisions (*l'ọ́jà*): write the joined word (*lọ́jà*).
- **Politeness**: *ẹ* (plural and respectful "you") to elders, parents and strangers, *o* to a
  child or a friend; greetings *ẹ kú àárọ̀*, *ẹ kú iṣẹ́*, *ẹ ṣé* are everyday words.

## Names

- Yoruba names (*Bọ́lá, Túndé, Fúnkẹ́, Ṣọlá, Kẹ́hìndé, Táíwò, Yẹmí, Dàmí, Ìyábọ̀, Fẹ́mi, Kúnlé,
  Ṣadé, Bùkọ́lá, Tọ́lá, Bísí, Mọ́jí, Ìdòwú, Rẹ̀mí, Gbénga, Kẹ́mi, Tópé, Jùmọ̀kẹ́*) and places (*Èkó,
  Ìbàdàn, Ọ̀yọ́, Abẹ́òkúta, Òṣogbo, Àkúrẹ́, Ìlọrin*) go in `names`, with their marks. Many Yoruba names
  are list words (*Adé* "crown", *Ayọ̀* "joy", *Ìrètí* "hope", *Ìfẹ́* "love", *Ọlá* "honour", *Ṣẹ́gun*
  "conquer", *Délé*, *Wálé* "come home"): never use a list word as a character's name.
- Words for peoples and languages (*Yorùbá*, *Òyìnbó*, *Hausa*, *Gẹ̀ẹ́sì*) count as words at their
  rank: never teach them as band words, a text may still use one.
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Yoruba list

- The English Wiktionary has about 4,400 Yoruba words and lacks hundreds of the commonest (*bá,
  tún, nígbà, tàbí, gẹ́gẹ́, rere, lára, lọ́wọ́*): those were added to the list by hand
  (tools/frequency_yoruba.tsv) and have **no entry** in `dictionaries/yo-en.sqlite`. Check a word
  with an entry against it; a word without one you must know.
- Headwords are the standard toned spellings. A headword joins words written alike: *ní* is "at",
  "have" and "say"; *bí* "if, how, like" and "give birth"; *tó* "reach, enough" and *tí ó*; *wo* is
  "which" (and *wo* before an object, "look at", counts as it too). Teach a word in its common sense.
- The one-vowel words *i, e, á, ọ́, ẹ́, ú, ún, án, ín, í* are the third-person object pronoun (*rí i,
  gbé e, pa á, mọ̀ ọ́, kún ún*) and the *í* of *kì í*: real words, taught through their verb.
- Contractions are words of their own: *ló* (*ni ó*), *tó* (*tí ó*), *màá* (I will), *wàá* (you
  will), *áá* (he will), *àá* (we will).
- Some entries are noise of the sources (scripture and Wikipedia): *ọnnì*, *ìjápọ̀*. Skip noise.
