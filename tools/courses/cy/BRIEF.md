# Writing a Welsh frequency course

Each course teaches one band of 100 words from the Welsh word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `cy-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## The register

**One register in every course: modern standard written Welsh for learners (*Cymraeg safonol
cyfoes*).** It is the plain, correct Welsh of today's learners' books and news sites, neutral
between north and south, neither the literary language nor a dialect. Keep exactly to these choices:

| Point | Write | Not |
|---|---|---|
| "I am" | *Rydw i'n*, *Rwyt ti'n*, *Mae e'n*, *Mae hi'n*, *Rydyn ni'n*, *Rydych chi'n*, *Maen nhw'n* | *Yr wyf*, *Dw i'n*, *Wi'n*, *Dan ni'n*, *Dyn ni'n* |
| Questions | *Ydw i?*, *Wyt ti?*, *Ydy e?*, *Ydyn ni?*, *Ydych chi?*, *Ydyn nhw?* | *Ydwyf?*, *Dach chi?* |
| Negatives | *Dydw i ddim*, *Dwyt ti ddim*, *Dydy hi ddim*, *Dydyn ni ddim*, *Dydych chi ddim*, *Dydyn nhw ddim* | *Nid wyf*, *Dw i ddim*, *Sa i* |
| "he, him" | *e* (*fe* when stressed: *Fe yw'r athro*, *Dyma fe*) | *o*, *fo* |
| "they" | *nhw* | *hwy*, *hwynt* |
| "have" | *gan*: *Mae gan Siân gath*, *Mae gen i gath* | *Mae cath gyda fi*, *Mae gennyf* |
| "with" | *gyda* | *efo* |
| "want" | *eisiau* | *isio*, *moyn* |
| "now", "out" | *nawr*, *allan* | *rŵan*, *mas* |
| Short past | *Prynais i*, *Prynaist ti*, *Prynodd hi*, *Prynon ni*, *Prynoch chi*, *Prynon nhw*; *Es i*, *Aeth hi*, *Ces i*, *Gwnes i*, *Des i* | *Prynes i*, *Prynasant*, *Euthum*, *Mi brynais*, *Fe brynodd* |
| Commands | *Eisteddwch*, *Dewch*, *Ewch*; to one friend *Eistedda*, *Dere*, *Cer* | *Tyrd*, *Dos* |
| Everyday words | *llaeth*, *merch*, *bachgen*, *dannedd*, *gwely* | *llefrith*, *hogan*, *hogyn*, *dannadd* |

The conjugated present and future of ordinary verbs (*prynaf*, *gwelant*), the impersonal in *-ir*
and *-wyd* before rank 1301, and the literary pluperfect stay out. Spell everything as the standard
does: *ŵ*, *ŷ*, *â*, *ê*, *î*, *ô*, *û* always (the checker compares spellings: *tân* "fire" / *tan*
"until", *mêl* "honey", *gŵr* "husband", *dŵr* "water", *tŷ* "house"). No slang, no English mixed
into a sentence, no text-speak. Careful everyday expressions in dialogue are fine (*iawn*, *diolch*,
*da iawn*, *bore da*, *os gwelwch yn dda*, *hwyl*).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Modern everyday life where Welsh is spoken: Gwynedd, Ynys Môn, Ceredigion,
  Sir Gâr and the valleys, the towns and cities (Caerdydd, Abertawe, Bangor, Aberystwyth,
  Caernarfon, Wrecsam, Llanelli, Caerfyrddin, Dolgellau, Pwllheli, Machynlleth), Welsh-medium
  schools and families who speak Welsh at home anywhere; and now and then Lerpwl, Llundain or the
  Welsh-speaking towns of Patagonia (Gaiman): home, family, food, shopping, work, school, the town,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *e*/*hi* is whenever it could be
  unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, in Welsh order. Little
  dialogue; when there is some, short and simple. *ti* to a friend, a child or a relative; *chi* to
  several people and to anyone you would address politely. Keep one form per pair of speakers.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present of *bod* with *yn* + verb-noun (*Mae hi'n darllen.* *Rydw i'n gweithio.*),
    with *yn* + soft mutation for nouns and adjectives (*Mae e'n fawr.* *Mae hi'n athrawes.*); the
    focus sentence (*Merch o Fangor yw Cerys.*); questions and short answers (*Ydy hi? Ydy. / Nac
    ydy.*, *Oes? Oes. / Nac oes.*, *Wyt ti? Ydw. / Nac ydw.*); negatives with *ddim*; *Mae gan X Y*;
    *wedi* for the perfect (*Mae hi wedi mynd.*); the article (*y*, *yr*, *'r*) and soft mutation of a
    feminine singular noun (*y ferch*, *yr ardal*); possessives with their mutations (*fy nhŷ*, *dy
    gar*, *ei gar* "his", *ei char* "her", *ein tŷ*, *eich tŷ*, *eu tŷ*); prepositions and their soft
    mutation (*i Fangor*, *o Gaerdydd*, *ar ben*), *yn* "in" and its nasal mutation (*yng Nghaerdydd*,
    *ym Mangor*), *a* / *gyda* / *â* and the aspirate (*a chi*, *gyda phlant*); numbers with singular
    nouns (*dau blentyn*, *dwy ferch*, *tri phlentyn*); question words (*beth*, *ble*, *pwy*, *pryd*,
    *sut*, *pam*, *faint*);
  - ranks 301–600: the short past of the common irregular verbs and regular verbs (*Es i*, *Aeth hi*,
    *Ces i*, *Daeth e*, *Gwnaeth hi*, *Prynodd e*, *Gwelon nhw*), with its soft mutation of the object
    (*Prynodd hi gar.*), and *ddim* (*Es i ddim*, *Welodd hi mo'r car*); the imperfect of *bod* (*Roedd
    hi'n bwrw glaw.*, *Roeddwn i*, *Oeddet ti?*, *Doedd e ddim*); the future of *bod* (*Bydd hi'n braf.*,
    *Bydda i*, *Byddwn ni*, *Fydd e?*); commands; *rhaid i X* (*Rhaid i mi fynd.*); comparisons (*yn fwy
    na*, *yn well na*, *cystal â*, *mor fawr â*); telling the time and dates (months and days: see
    "Names");
  - ranks 601–1000: the conditional of *hoffi* and *bod* (*Hoffwn i*, *Byddwn i'n mynd*, *Byddai hi*);
    clauses with *bod* and *mai* (*Mae hi'n dweud bod y bws yn hwyr.* *Rydw i'n meddwl mai Nia yw hi.*);
    relative *sy* and *a* (*y dyn sy'n gweithio yma*, *y llyfr a brynodd hi*); *pan*, *os*, *achos*,
    *oherwydd*, *felly*, *ond*; *gallu* and *dylwn* (*Gallwch chi*, *Dylet ti fynd.*);
  - ranks 1001–1300: *arfer* for what used to happen (*Roedd hi'n arfer byw yma.*), superlatives (*y
    tŷ mwyaf*, *y gorau*), *newydd* + verb-noun for what has just happened, *er mwyn* and *i* + verb-noun
    for purpose;
  - ranks 1301–1600: the impersonal (*Cafodd y tŷ ei adeiladu.*, *adeiladwyd*, *gwerthir*); *pe* and
    *petai* for what is not so (*Petai amser gen i, byddwn i'n mynd.*); *er bod*, *cyn i*, *ar ôl i*,
    *nes*; the prepositions with their pronouns in full (*arna i*, *amdani*, *ohonyn nhw*, *wrtho fe*);
  - ranks 1601–2000: the object pronoun around a verb-noun (*Rydw i'n ei weld e.*, *Mae hi wedi eu
    prynu nhw.*), the genitive (*drws y tŷ*, *mam Nia*, *canol y dref*), indirect questions (*Dydw i
    ddim yn gwybod a yw hi yma.*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another and reported speech with
    tense shifts (*Dywedodd hi y byddai hi yno.*), *oherwydd*, *o ran*, *yn ôl*, *o ganlyniad*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *fodd bynnag*, *serch hynny*,
    *felly*, *hynny yw*, *ar y llaw arall*; common proverbs (*diarhebion*) and idioms explained by their
    context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and
    clear images, the newspaper column (a light reflection on everyday life);
  - ranks 4001–5000 (C1): more abstract and nuanced content: a short essay or reflective column that
    weighs two sides, an interview, a portrait, a review; long sentences that stay easy to parse;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and the
    arts, each made clear by its context;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, and topics
    that need some background, which the text gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose, academic and
    journalistic registers, learned words explained by their context. Still standalone, calm and easy
    to follow. The register table still holds for the narrator; a learned text may use the literary
    forms only where its genre requires them, and only from rank 8001.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a
  lesson may also be a short informative or opinion text, as long as it is everyday, calm and easy to
  follow. A learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words three
  times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Cwestiynau:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words (after rank 1000 they may also ask *pam* and *sut*, with fuller answers):

  ```
  Cwestiynau:
  Beth yw enw'r ferch?
  Mared yw enw'r ferch.
  Oes plentyn gan Mair?
  Oes. Mae gan Mair un plentyn.
  ```

  The header `Cwestiynau:` is layout: the checker does not count it as a word.
- **The mutations are grammar, not spelling**: write every soft, nasal and aspirate mutation the
  standard requires and none that it does not. The checker reads a mutated form as its word but never
  tells you whether it is right: *i Fangor* and *i Bangor* pass alike, and only the first is Welsh.
  *ll* and *rh* do not mutate after predicative *yn* (*yn llawn*, *yn rhad*), nor does a feminine noun
  in *ll*/*rh* after *y* (*y llaw*, *y rhaglen*).
- **Welsh typography**: direct speech in curly double quotation marks “…”, each speaker's line a
  paragraph of its own, the comma, question mark or exclamation mark inside the marks. Below rank 300
  the tag is *mae X yn dweud*; from rank 301 it is *dywedodd X* or *meddai X*:

  ```
  “Oes bag gen ti?” mae Gethin yn dweud.
  “Oes,” mae Iolo yn dweud.
  ```

  The questions and answers of the `Cwestiynau:` block are plain lines. A dash is the en dash with
  spaces ( – ). The apostrophe of a contraction may be straight or curly, either: *mae'n*, *i'r*. Write
  numbers as digits (*am 8*, *yn 1995*, *£5*; an ordinal as *y 3ydd*: the checker reads the letters
  after the digits as part of the number), except a number word that is in your band: spell that one
  out to teach it (*dau blentyn*, *tair merch*).
- **Names are names**: place names and words for peoples and languages (*Cymru*, *Cymraeg*, *Saesneg*,
  *Lloegr*, *Cymro*) are skipped as band words even when the list has them. Never use a list word as a
  character's name.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's or
  any other), and not the courses of the other languages either.

## Names: months, days, people

**Months and days are names, as in the Indonesian courses:** the list leaves them out, so put the ones
you use in `names`, and never teach them: *Ionawr*, *Chwefror*, *Mawrth*, *Ebrill*, *Mehefin*,
*Gorffennaf*, *Awst*, *Medi*, *Tachwedd*, *Rhagfyr*; *Llun*, *Mawrth*, *Mercher*, *Iau*, *Gwener*,
*Sadwrn*, *Sul* (*dydd Llun*, *nos Wener*: *dydd* and *nos* are list words). Three are spelled as other
list words, which stay teachable in their own sense only: *mai* 136 is the conjunction "that" (not the month
*Mai*), *hydref* 586 is "autumn" (not *Hydref* "October"), *llun* 223 is "picture" (not Monday), and *iau* 762 is
"younger" or "liver" (not Thursday). A month or day in `names` counts as known; do not use one to reach
the two uses a new word needs.

Names free to use (none is a list word): *Siân, Rhys, Mair, Nia, Cerys, Ifan, Gareth, Huw, Alys, Tomos,
Emrys, Megan, Dafydd, Aled, Rhian, Bethan, Iolo, Lowri, Sioned, Gethin, Mared, Eleri, Owain, Dylan, Catrin,
Angharad, Geraint, Non, Efa, Carys, Rhodri, Hywel, Ioan, Tudur, Betsan, Lleucu, Osian, Macsen, Mabli, Erin,
Guto, Manon, Menna, Heledd, Nerys, Sara, Tegwen, Wil, Elwyn, Arwel*. **Not** names here, because they are
list words or forms: *Elin, Llinos, Cadi, Nest, Gwen, Gwyn, Ffion, Seren, Ceri, Dewi, Glyn, Bryn, Math,
Pryderi, Eirlys*. Places free to use: *Caerdydd, Abertawe, Bangor, Aberystwyth, Caernarfon, Wrecsam,
Llanelli, Caerfyrddin, Dolgellau, Pwllheli, Llangefni, Machynlleth, Aberteifi, Dinbych, Casnewydd, Gaiman,
Patagonia*. Write a name mutated as the grammar requires (*yng Nghaerdydd*, *i Fangor*, *o Gaernarfon*, *i
Batagonia*); list only its radical form in `names`, and the checker reads the mutated ones.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, ministers and priests, prayer, heaven and
  hell, angels and devils, and every custom or holiday of religious origin, even as a date, a school
  break or a gift season: Christmas (*Nadolig*), Easter (*Pasg*), St David's Day (*Dydd Gŵyl Dewi*: a
  saint's day and a national day), St Dwynwen's Day, the *plygain*, and the sayings that name God (*diolch
  i Dduw*, *Duw a ŵyr*): greet with *helo*, *bore da*, *sut wyt ti?*, *sut ydych chi?*. Use *credu* only as
  believing a person or a fact. A chapel may not even be a landmark;
- **politics**: the state and the government, the Senedd, elections, parties, kings and rulers,
  nationalism, independence, flags, the border, and the Welsh language as a political question (language
  rights, protests, *Cymdeithas yr Iaith*, Tryweryn); everyday civic life (a library, a school, the
  council's counter, a residents' meeting, a local club) is fine;
- **war, the army, weapons, violence, crime, police and courts**, and the wars and risings of Welsh
  history (Glyndŵr's and Llywelyn's wars, Rebecca, the Chartists, the mining disasters);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a story
  is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, words whose main sense is sexual, and kissing, courtship gone wrong,
  separation and divorce;
- **alcohol** in every form (beer, wine, the pub as a place to drink), **gambling**, drugs and smoking;
- **deliberate misspellings**: no spellings without their accents, no old or dialect spellings, not even
  in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, or a word for a people or a
  language is skipped. Common Welsh first names are fine as characters.

Folk culture with no church holiday or saint named is fine: an *eisteddfod* told as a contest of singing,
poetry and dance (no bardic prayer), a *noson lawen*, male voice choirs as music, rugby and football, a
market, a fair, a birthday, a wedding told as family news (no chapel), the start of the school year, the
summer holidays. The history courses tell the history of everyday life, towns, crafts, slate and coal as
trades, science and travel, never wars, rulers or disasters. *Yr iaith Gymraeg* and *Geiriau a'u tarddiad*
are about words, dialects and learning, never language politics. *Chwedlau a straeon* tells folk tales
(clever animals, giants, the lake maiden) without ghosts, saints or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first 1000
ranks:

- religion: *eglwys* 152, *sant* 528, *capel* 587, *ysbryd* 927, *duw* 981, *crefyddol* 909;
- politics: *plaid* 208, *llywodraeth* 267, *cyngor* 325 only as advice or a local council's counter, *gwleidyddol* 408, *brenin* 444,
  *unol* 457, *gweinidog* 531, *etholiad* 548, *talaith* 564, *cynghrair* 589 only as a sports league, *senedd* 630, *teyrnas* 704, *deddf* 724,
  *baner* 797, *brenhinol* 848, *seneddol* 906, *gwladwriaeth* 936, *pleidlais* 941, *unedig* 995;
- war, crime, police, courts: *rhyfel* 227, *lladd* 445, *byddin* 514, *llys* 557, *ymladd* 606, *brwydr* 609, *heddlu* 611, *milwrol* 774, *milwr* 969;
- death: *marw* 155, *marwolaeth* 342, *claddu* 728, *mynwent* 777, *gwaed* 893;
- alcohol: *gwin* 977;
- names and pieces of place names: *sir* 239, *caer* 866;
- too abstract for its level: *modd* 90; *rhyw* 262 only as "some" (its other sense is sex).

Further on, the same categories hold; for example *saethu* 999, *plwyf* 1026, *tafarn* 1043, *carchar* 1066, *llywydd* 1092, *pleidleisio* 1119,
*ymosod* 1177, *arf* 1191, *ymerodraeth* 1220, *arglwydd* 1224, *arlywydd* 1273, *cwrw* 1517, *cyffur* 1114, *bedd* 1185, *alcohol* 1264, *esgob* 1401, *trosedd* 1661,
*llofruddiaeth* 1902, *offeiriad* 1931, *angladd* 2281, *heddwas* 2333, *angel* 2471, *gweddi* 2623, *diafol* 3184, *uffern* 4575, *sigarét* 3411, *ysgariad* 3017, *cusan* 5689.

## The file

Write `tools/courses/cy/<id>.json`:

```json
{
  "id": "cy-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Welsh, e.g. Bwyd a siopa",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Welsh",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/cy/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to
  rank 300, 90% for the first course). Names you list in `names` and numbers count as known. The checker
  lists every word outside the range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in
  its lesson (any form counts, mutated too: *aeth*, *fynd* or *es* for *mynd*; *ferch* or *merched* for
  *merch*; *fawr* for *mawr*). Reuse earlier lessons' new words too. `newWords` holds the word as the list
  writes it (the third column of the band file): a verb as its verb-noun (*mynd*, *gwneud*, *prynu*), a noun
  in its singular radical form (*plentyn*, not *plant*; *aderyn*, not *adar*), an adjective in its plain form.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up to rank
  300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000,
  350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth a lesson
  (a form of another word, a list error such as a name, a piece of a place name or an English word, a word for
  a people or a language, a word of the content rules); the checker's last line lists the band words not
  taught yet.

Things to know about the Welsh list and the checker:

- The courses are pinned to `tools/courses/cy/wordlist.tsv`, a copy of the app's `cy.tsv`. The list is counted
  from Welsh Wikipedia (40%), Common Voice's mixed sentences (25%) and everyday sentences from Tatoeba and
  Common Voice (35%), so encyclopaedia words still rank above kitchen words: *rhywogaeth* 134, *cyhoeddi* 183 and
  *cenedlaethol* 187 stand above *bara* 960, *coffi* 957 and *cegin* 1321.
- **Contractions count as two words**: *mae'n* is *mae* (*bod*) + *yn*; *i'r* is *i* + *y*; *a'i* is *a* + *ei*;
  *wedi'u* is *wedi* + *eu*; *i'w* is *i* + *ei*; *a'm* is *a* + *fy*; *o'th* is *o* + *dy*; *i'ch* is *i* + *eich*.
  Each part counts for its own word, and for the lesson's length.
- **Mutations**: a mutated form the list lacks counts as the word it mutates (*ddefnyddiodd* is *defnyddiodd*,
  *hysgol* is *ysgol*, *Nghaerdydd* is *Caerdydd*). A word with a hyphen is one word where the list has it
  (*pêl-droed*) and its parts otherwise.
- **Spellings shared by two words**: a form is counted for the word that holds it in the list, so look it up with
  `rank.py` before relying on it. *dyn* is "man" (never write *dyn ni*); *oes* is "is there?" as well as "age" and
  has its own entry; *gwell*, *gorau*, *mwy*, *mwyaf*, *llai*, *lleiaf* are entries of their own; *faint* and
  *beth* are entries apart from *maint* and *peth*; *yr* and *'r* are *y*'s; *i* holds the preposition and "I"
  with *iddo*, *iddi*, *imi*; *fi*, *mi*, *ti*, *chi*, *ni*, *nhw*, *hi*, *e*, *fe*, *fo* are entries of their own.
