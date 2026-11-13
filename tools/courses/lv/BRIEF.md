# Writing a Latvian frequency course

Each course teaches one band of 100 words from the Latvian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `lv-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 6,630 words, so the courses run from
`lv-mini-0100` to `lv-mini-6700` (the last band holds 30 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Līga nezina, kur ir Kārlis. Viņa iet pie Andra. Kārlis ir tur...* Everyday life
  where Latvian is spoken: Rīga (Vecrīga, Pārdaugava, Āgenskalns, Mežaparks), Liepāja, Ventspils,
  Jelgava, Daugavpils, Cēsis, Sigulda, Valmiera, Kuldīga, Talsi, Tukums, Ogre, Bauska, Rēzekne,
  Saulkrasti, Salacgrīva, Līgatne, Pāvilosta, Kolka; the Daugava, the Gauja and the Venta, the
  Baltic coast and the Gulf of Riga, the lakes of Latgale, Vidzeme's hills and Kurzeme's forests;
  and now and then Dublin, London, Stockholm, Berlin or Toronto, where Latvians live and Latvian is
  spoken at home. Any everyday, cultural or story topic fits (see the content rules below).
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *viņš*/*viņa* is whenever it could be unclear. Give each
  story its own names: the learner reads the course in one go, and two Jānis in two lessons blur
  together.
- **Plain narration**: third person, short sentences, one idea per sentence, neutral word order
  (subject, verb, object; Latvian word order is free, so keep to the neutral one). Little dialogue;
  when there is some, short and simple, with *tu* among family and friends and *jūs* (with the
  second person plural) with strangers, older people and in shops or offices (*Vai jūs varat
  pateikt...?*, *Bērziņa kungs*, *Ozolas kundze*).
- **Address uses the vocative**: a masculine name in *-s* or *-š* drops it (*Tom!*, *Edgar!*,
  *Mārtiņ!*), one in *-is* ends in *-i* (*Kārli!*, *Gati!*, *Jāni!*), a feminine name keeps its
  form (*Anna!*, *Rūta!*, *Elza!*). Children call their parents *mamm* and *tēt* (*mamma*, *tētis*
  in narration). Never the nominative in address for a masculine name (*Toms, nāc!* is wrong).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present (*ir*, *nav*, *iet*, *nāk*, *grib*, *var*, *zina*, *saka*), the past
    (*bija*, *gāja*, *nāca*, *teica*, *redzēja*) and the simple future (*būs*, *iešu*, *nākšu*);
    negation with *ne-* joined to the verb (*nezinu*, *nav*, *nebija*) and the genitive of absence
    (*man nav laika*); the nominative, the accusative of the object (*redz māju*), the genitive of
    possession and after *no*, *pie*, *bez*, *pēc*, *līdz* (*Annas māja*, *no Rīgas*, *pēc darba*),
    the locative of place (*mājā*, *mājās*, *Rīgā*, *darbā*), the dative of possession and age (*man
    ir māja*, *Tomam ir divi gadi*); *gribēt*, *varēt* + infinitive, *man vajag*; simple questions
    (*vai*, *kas*/*ko*/*kam*, *kur*, *kāpēc*, *kā*, *kāds*, *cik* + genitive plural: *cik bērnu*);
  - ranks 301–600: the imperative (*ej*, *nāc*, *paskaties*, *ejiet*, *nāciet*), the reflexive verbs
    (*mācīties*: *mācos*, *mācies*, *mācās*; *ne-* in front: *nemācās*), the debitive for "must"
    (*man jāiet*, *viņai jāstrādā*, *mums bija jāgaida*), *patikt* with the dative (*man patīk*), the
    plural of every case and the prepositions that take the dative in the plural (*ar draugiem*,
    *uz mājām*, *pa ielām*, *no bērniem*), the comparative (*lielāks*, *labāks*, *vairāk*, *nekā*),
    clauses with *ka*, *kad*, *jo*, *ja*, *lai* (*lai* with the conditional: *lai varētu*);
  - ranks 601–1000: the definite adjectives used on purpose (*jauna māja* / *jaunā māja*; after
    possessives and *šis*, *tas*: *mūsu jaunā māja*, *šī lielā istaba*), *kurš* in every case (*māja,
    kurā viņi dzīvo*), reported speech with *ka* (*teica, ka nāks*), numbers with their nouns (*divi
    bērni*, *divas mājas*, *pieci gadi*, *desmit gadu*, *divdesmit viens gads*), the conditional
    (*būtu*, *gribētu*, *varētu*; *Vai jūs varētu...?*), prefixed verbs for a finished action
    (*aiziet*, *atnākt*, *izdarīt*, *paņemt*, *uzzināt*);
  - ranks 1001–1300: the conditional in *ja* sentences (*ja man būtu laiks, es brauktu*), the
    superlative (*vislabākais*, *visvairāk*), the indefinite and negative pronouns with double
    negation (*kaut kas*, *kāds*, *neviens neko nezina*, *nekur*), the past perfect (*bija aizgājis*)
    where it is plain;
  - ranks 1301–1600: the participles as adjectives and in plain clauses (*strādājošs cilvēks*,
    *pagājušajā nedēļā*, *uzrakstīta vēstule*), the passive with *tikt* and *būt* (*māja tika
    uzcelta 1930. gadā*, *durvis ir aizvērtas*), the half-participle *-dams*, *-dama* for a second
    action of the subject (*iedams mājās, viņš domāja*), the gerund in *-ot* (*ejot mājās*,
    *atgriežoties*), *kaut gan*, *tomēr*;
  - ranks 1601–2000: the debitive in every tense (*būs jāiet*, *bija jādara*), the third-person
    imperative (*lai viņš nāk*), indirect questions with *vai*, *kur*, *kad* (*nezinu, vai viņš
    nāks*), *it kā*, *tā kā*;
  - ranks 2001–2500: the relative mood of reported speech (*viņš esot slims*, *esot teicis*) for
    recognition, explained by the context; verbal nouns (*lasīšana*, *mainīšana*); *neskatoties uz*,
    *pateicoties*, *saskaņā ar*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *tomēr*, *turklāt*,
    *tātad*, *no otras puses*, *ņemot vērā*, *runājot par*; longer participial phrases; common
    idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*sleja*, a light reflection on everyday life), dialogue
    that marks *tu* and *jūs*, and less common connectors (*lai gan*, *jo īpaši*, *tā vietā*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse;
  - ranks 5001–6700 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, figurative language used more freely, and topics
    that need some background, which the text gives.
  After rank 1000 sentences get longer and joined (*jo*, *kad*, *kamēr*, *kaut gan*, *tāpēc*,
  *tomēr*, *bet*, *tātad*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is calm and easy to follow. Still one clear idea at a time: a learner at the level
  should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Līga iet pie Andra. Kārlis ir tur.*).
- **Questions at the end**: after the story, a paragraph starting `Jautājumi:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kāpēc* and *kā*, with fuller answers):

  ```
  Jautājumi:
  Kur ir Kārlis?
  Kārlis ir pie Andra.
  Ar ko ir Kārlis?
  Kārlis ir ar Emīlu.
  ```

  The header is the lesson's layout: the checker leaves it out of the count.
- **Standard Latvian**: the narration is the literary standard (*latviešu literārā valoda*) as the
  Valsts valodas centrs, the Latviešu valodas aģentūra and *Tēzaurs* (tezaurs.lv) give it: neutral,
  correct and natural Latvian as written today. No dialect forms (Latgalian, the Tamian dialect of
  Kurzeme) and no slang or barbarisms the standard rejects (*čau* aside in a friend's greeting,
  *davai*, *nu jau*, *okei*, *sorry*, *bļin*, *vnk*). Dialogue may be lightly colloquial only in
  what any careful speaker says (*nu*, *labi*, *skaidrs*, *paldies*, *lūdzu*, *sveiks*). Always
  write every letter: the long vowels *ā*, *ē*, *ī*, *ū* and *č*, *š*, *ž*, *ģ*, *ķ*, *ļ*, *ņ* are
  letters of their own and tell words apart (*ka* "that" / *kā* "how, as"; *ja* "if" / *jā* "yes";
  *tā* "so, that (f.)" / *ta*; *māja* "house" / *maja*; *mājā* "in the house" / *mājās* "at home,
  home(wards)"; *gadu* / *gadū*). The checker compares them exactly. *ō* and *ŗ* are not in the
  modern standard.
- **Latvian typography**: direct speech in the quotation marks „…”, each speaker's line a
  paragraph of its own. The safe form puts the narrator first, with a colon: *Anna saka: „Šī ir
  mūsu māja.”*; a quote may also come first, followed by the narrator's words with a comma after
  the closing mark: *„Šī ir mūsu māja”, saka Anna.* (a question or exclamation mark stays inside:
  *„Kur tu esi?” jautā Anna.*). The questions and answers of the final `Jautājumi:` block are plain
  lines, as above. A dash inside a sentence is the en dash with spaces ( – ). Write numbers as
  digits (*7 gadi*, *no 8 līdz 16*, *20 gadu*, *1930. gadā*, *3. maijā*), except a number word that
  is in your band: spell that one out to teach it (*divi bērni*, *trīs dienas*). Write no
  abbreviations (*g.*, *u.c.*, *piem.*, *t.i.*, *plkst.*): write *gadā*, *un citi*, *piemēram*,
  *tas ir*, *pulksten*.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules

User decision 2026-11-13: there are NO topic rules. Any normal everyday, cultural or story topic
is fine: religion and holidays (*Ziemassvētki*, *Lieldienas*, *Jāņi* with its songs, cheese and
bonfires, name days, *Mārtiņdiena*), history, kings and dukes, myths and folk tales (*Lāčplēsis*,
*dainas*), crime and detective stories, courts, politics and elections, war in history, alcohol
(beer at *Jāņi*, *balzams*), smoking, love and kisses, death, sport of every kind. Only avoid
explicit sexual content, slurs used as insults, and gratuitous gore.

Teach ordinary words in their main sense. Days of the week, months, seasons, nationalities and
language words are everyday words, not names: teach them (*pirmdiena*, *marts*, *vasara*,
*latvietis* and *latviešu valoda*, *angliski*). The word rules still apply in full: no duplicates,
no forms of taught words, no word counted through another sense, name or idiom, no list noise.

## The file

Write `tools/courses/lv/<id>.json`:

```json
{
  "id": "lv-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Latvian, as themes.tsv writes it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
  "lessons": [
    {
      "title": "Story title in Latvian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/lv/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gāja* or *ejam* for *iet*, *laba* or *labāk* for *labs*,
  *nezinu* for *zināt*, *bērnu* for *bērns*). Reuse earlier lessons' new words too. `newWords`
  holds the word as the list writes it: the infinitive of a verb (*iet*, *mācīties*), the nominative
  singular of a noun (but *durvis*, *beigas*, *vecāki*, *svētki*, *kāzas*, *dati*, *finanses*,
  *vakariņas*, *mūsdienas*, *vēlēšanas*, which the list keeps in the plural), the masculine singular
  of an adjective (*labs*, *liels*, *pirmais*, *pēdējais*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, list noise such as a form filed as a headword, a
  negated verb form, a comparative, a plain participle, a multi-word entry); the checker's last line
  lists the band words not taught yet.

Things to know about the Latvian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/lv/wordlist.tsv`
  (a copy of the app's `lv.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  subtitles, the web and news (wordfreq, OpenSubtitles and the Leipzig news corpus) and kept only
  words its dictionary pack knows, so it is short (6,630 words) and lacks some everyday words (see
  "Words the list lacks").
- **The checker has no Latvian rules of its own** beyond leaving out the `Jautājumi:` header: it
  cuts words at spaces, punctuation and hyphens, and looks each one up as written (lowercased). A
  form counts only if the list or `extra_forms.tsv` has it under the headword; `rank.py` (in the
  writers' folder) shows where a form is filed.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns (*bērns*: *bērna*, *bērnam*, *bērnu*, *bērnā*, *bērni*, *bērniem*, *bērnus*...), the
  definite endings and the comparative of adjectives (*jaunais*, *jaunākais* under *jauns*), the
  conjugation, the participles and the negated forms of verbs. The list kept at most 40 forms a
  word, so `extra_forms.tsv` adds the rest from the dictionary pack and the regular paradigms:
  every negated form (*nezinu*, *nemācās*, *neatnāca*), the debitive (*jāiet*, *jāmācās*), the
  forms of prefixed and reflexive verbs the pack lacks (*paņēma*, *priecājos*), the declined
  participles, the rarer cases. *es* holds *man*, *mani*, *manis*; *tu* holds *tev*, *tevi*; *mēs*
  holds *mūsu*, *mums*, *mūs*; *jūs* holds *jūsu*, *jums*; *viņš* holds *viņam*, *viņu* and *viņa*
  "his"; *viņa* holds *viņai*, *viņu*, *viņas*; *viņi* holds *viņiem*, *viņus*; *kas* holds *ko*,
  *kam*; *sevi* (117) holds *sev*; *mans* (40), *tavs* (72), *savs* (26) are entries of their own;
  *viss* holds *visi*, *visu*, *visa*; *labi* (39) is an entry of its own beside *labs* (54).
- **Negated verbs**: *ne-* is written joined to the verb (*nezina*, *nav*, *nebija*, *neesmu*), and
  every negated form counts for its verb, reflexive ones too (*nemācās*, *neatceros*). The list
  also keeps some negated forms as headwords of their own (*nespēju* 914, *nedomāju* 925, *neesam*
  969, and many after rank 4000): they are forms of their verbs; never teach them.
- **Reflexive verbs are words of their own**: *mācīties* "to learn" and *mācīt* "to teach",
  *skatīties* 182 and *skatīt*, *beigties* 526 and *beigt* 218, *sākties* 367 and *sākt* 97,
  *mainīties* 809 and *mainīt* 447, *turēties* 904 and *turēt* 495, *likties* 528 and *likt* 173.
  The reflexive forms (*mācās*, *mācos*) count only for the reflexive entry, the plain ones
  (*māca*) only for the plain one: teach each in its own sense and forms. Prefixed verbs are
  entries of their own too (*aiziet* 138, *atrasties* 192, *izdarīt* 177, *paņemt* 743, *pateikt*
  248, *uzzināt* 666): teaching *iet* does not teach *aiziet*, and each is taught through its own
  forms.
- **Forms filed as headwords** (list errors; skip them, their uses count at their own rank):
  *vēlies* 742 (*vēlēties*), *gaida* 597 (*gaidīt*; its forms are the verb's), *zemi* 982 (*zeme*),
  *vakarā* 902 (*vakars*), *ņemot* 626 (*ņemt*, as in *ņemot vērā*), *viņas* 152 (*viņa*), the plain
  participles *plānots* 572, *reģistrēts* 584, *konstatēts* 920, *miris* 328 (*mirt*); *daži* 341 is
  the plural of *dažs* 444 (teach *daži* "some, a few", skip *dažs*); *skolotāja* 571 holds the
  masculine *skolotājs* (teach it as "teacher"). From about rank 5000 the list fills with negated
  forms (*nelasām*), participles (*izceptais*) and comparatives (*melnāks*) kept as headwords, and
  from rank 6495 nearly every entry is a comparative: in those bands fewer real words are
  teachable, which is fine; never pad with noise. The 11 multi-word entries (*mobilais telefons*,
  *latviešu valoda*, *vārda diena*...) are skipped.
- **Words that hold another word**, so a form counts for a headword it does not teach: *kaut* 73
  "though, at least" (*kaut kas*, *kaut gan*) holds the verb *kaut* "to slaughter" (*kauj*); *rīt*
  511 "tomorrow" holds *rīt* "to swallow" (*rij*); *klāt* 419 "present, near" holds *klāt* "to
  cover"; *sniegt* 348 "to reach, give" holds *sniegs*, *sniega*, *sniegu* "snow" (snow is no entry:
  the forms count, but teach *sniegt* in its own sense); *tēvocis* 1324 "uncle" holds *tante*
  "aunt"; *draugs* 115 holds *draudzene*, *karalis* 630 *karaliene*, *saimnieks* 965 *saimniece*:
  feminine nouns filed under the masculine count at its rank but never teach it; *tāds* 44 holds
  *šāds*; *daļa* 121 holds *daļiņa*; *dārzs* 812 *dārziņš*; *vecis* 460 *vecītis*; *lietus* 962
  "rain" holds *lietu*, *lieti* of *lieta* 94; *gars* 976 "spirit" holds *gara*, *garu*, *garā* of
  *garš* 508 "long"; *plāns* 233 "plan" holds *plāno* (*plānot*) and the adjective "thin";
  *pagaidīt* 205 holds *pagaidām* "for now"; *ievērot* 333 holds *ievērojami* "considerably";
  *beigt* 218 holds *beidzot* "finally"; *piedot* 280 holds *piedevām*; *solīt* 892 holds *soli*
  (*solis* "step"); *paiet* 413 holds *pagājušais* "last" (*pagājušajā nedēļā*); *mēness* 269
  "moon" holds *mēnesi*, *mēnesī* of "month" (*mēnesis* 466); *vara* 159 "power" holds *varu*
  (also *varēt*). A new word must appear as itself or a true inflection, never through such a form.
- **Number words** decline (*divi*, *divas*, *divu*, *diviem*; *trīs*, *trim*, *triju*): *divi*,
  *trīs*, *četri*, *pieci*, *seši*... are entries, *viens* holds *viena*, *vienu*. With digits the
  noun still agrees: *2 gadi*, *5 gadi*, *10 gadu*, *21 gads*.
- **Words the list lacks**: the list kept only the words its dictionary pack knows, so some common
  words are missing altogether and count as outside words (*not in list*): *vecmāmiņa*, *labdien*,
  *sveicināti*, *ardievu*, *zvanīt*, *nopirkt*, *uzrakstīt*, *izlasīt*, *apēst*, *izdzert*,
  *pabeigt*, *samaksāt*, *satikt*, *apsolīt*, *apskatīt*, *visur*, *citur*, *labprāt*, *vienreiz*,
  *godīgi*. Write around them: the plain past says a finished action well enough (*Viņa pirka
  maizi*, *Viņš rakstīja vēstuli*, *Mēs ēdām zupu*); greet with *sveiki* or *labrīt*; call with
  *piezvanīt* (1843).
- **Common forms the list misses** go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated by `lv_extra.py` in the writers' folder from the English Wiktionary Latvian dictionary
  (`dictionaries/lv-en.sqlite`), the prefixed and reflexive verbs' bases, the regular conjugations
  and declensions (taken only where the list's own forms fit them) and the negated and debitive
  verb forms, with a few hand-added forms. Every headword in it must be a headword of the list (the
  checker warns otherwise). If a true form is missing, write around it and name the word and form
  in your report.
- **Names** are not in the list: people, places (*Rīga*, *Liepāja*, *Latvija*), shops and brands go
  in `names`. The checker does not decline them, so list every form the text uses (*Kārlis*,
  *Kārļa*, *Kārlim*, *Kārli*; *Anna*, *Annas*, *Annai*, *Annu*; *Rīga*, *Rīgas*, *Rīgā*; *Cēsis*,
  *Cēsīm*, *Cēsīs*). A name of several words counts word by word: each part that is a list word
  counts at its rank, so put only the true name parts in `names`. Keep only true names in
  `names`, never common words, and never a name one of whose forms is a list word: as characters
  avoid *Marta* (*marts*), *Ieva* (1793, "bird cherry"), *Juris* (*Jura*), *Valdis* (*Valdi*),
  *Madara*, *Daina*, *Arta*, *Rota*, *Rasma*, *Mārīte*, *Guna*, *Kalvis*, and the places *Jūrmala*
  (977, "seaside"), *Saldus*, *Valka*, *Ape*, *Ērgļi*. Check every name and each of its case forms
  with `rank.py` before you use it.
- Everyday words rank later than you would think: *draugs* 115, *nauda* 116, *strādāt* 120, *mazs*
  122, *lūdzu* 123, *pilsēta* 136, *skola* 162, *ģimene* 163, *dzīvot* 166, *šodien* 214, *tēvs*
  243, *mamma* 258, *vīrs* 270, *dēls* 322, *sieva* 382, *brālis* 399, *tētis* 431, *māte* 434,
  *ēst* 465, *suns* 467, *istaba* 579, *meita* 591, *dzert* 643, *māsa* 740, *kafija* 875, *kaķis*
  990, *virtuve* 1182, *piens* 1190, *tēja* 1252, *vectēvs* 1506, *maize* 1734. The dialogue tag
  *saka* (*sacīt* 96) and *teica* (*teikt* 71) are in the first band; *jautā* (*jautāt* 838) and
  *atbild* (*atbildēt* 402) come later. Days and months: *marts* 349, *maijs* 406, *septembris*
  425, *aprīlis* 441, *augusts* 504, *novembris* 608, *februāris* 617, *oktobris* 622, *jūlijs*
  625, *jūnijs* 632, *janvāris* 672, *decembris* 763; *svētdiena* 1476, *sestdiena* 1495,
  *piektdiena* 1500, *pirmdiena* 1808, *otrdiena* 2040, *ceturtdiena* 2068, *trešdiena* 2266;
  *vasara* 408, *ziema* 627, *pavasaris* 818, *rudens* 988.
- **Traps for a non-native writer**: going to a person is *pie* + genitive (*iet pie Andra*),
  to a place *uz* + accusative (*iet uz darbu*, *uz Rīgu*); "at home" and "home(wards)" are both
  *mājās* (*ir mājās*, *iet mājās*); every preposition takes the dative in the plural (*ar
  draugiem*, *uz mājām*, *no bērniem*, *par lietām*); age and having take the dative (*Tomam ir
  divi gadi*, *man ir laiks*); "there is no" takes the genitive (*man nav laika*, *mājās nav
  maizes*); *lai* takes the conditional (*lai varētu*, *lai būtu*); after a possessive or *šis*,
  *tas* the adjective takes its definite ending (*mūsu jaunā māja*). Polite requests use *jūs*
  and often the conditional (*Vai jūs varētu pateikt...?*).
