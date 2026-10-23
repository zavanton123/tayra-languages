# Writing a Slovene frequency course

Each course teaches one band of 100 words from the Slovene word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `sl-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ana želi psa. Gre v zavetišče. Tam vidi tri pse...* Everyday life in
  Slovenia: Ljubljana, Maribor, Celje, Kranj, Novo mesto, Ptuj, Velenje, Kamnik, Škofja Loka, the
  coast (Piran, Izola, Portorož), Bohinj and the Julian Alps, Triglav, the Soča valley (Bovec,
  Kobarid), the Karst and its caves (Postojna, Škocjan), Pohorje, Prekmurje, Dolenjska and
  Štajerska, the rivers Sava, Drava, Soča, Krka and Mura, villages, weekend cottages (*vikend*) and
  allotment gardens (*vrtiček*); and now and then Trieste, Zagreb, Vienna, Graz or London, where
  Slovene is spoken at home: home, family, food, shopping, work, school, the city, health, free
  time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Mateji
  in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  with the clitics in second position, in their fixed order: the auxiliary (*sem, si, sva, sta,
  smo, ste, so, bom, bi*…), then *se*/*si*, then the dative, then the accusative or genitive, and
  *je* last of all (*Ime mu je Marko. Dal sem mu ga. Dala mu ga je. Rad bi ga videl. Ana se
  smeje.*). A clitic out of place is the commonest mistake a non-native writer makes. Present tense
  up to rank 500, with the future of *biti* (*bom*, *bo*) and *morati/moči/hoteti/želeti* +
  infinitive where natural; from rank 600 the story may be told in the past (*Ana je šla od doma.
  Deževalo je.*). Slovene drops subject pronouns: write *on*, *ona*, *oni* only where Slovene would
  (contrast, emphasis, a new subject); the auxiliary carries the person (*Bil sem*, *Šla si*).
  Little dialogue; when there is some, short and simple, with *ti* (tikanje) among family and
  friends and *vi* + the plural (vikanje) with strangers, older people and in formal settings
  (*Ali stanujete tukaj?*, *gospod Novak*, *gospa Kos*).
- **The dual**: Slovene speaks of two people or things in the dual, always: *Ana in Marko gresta v
  mesto. Imata dva otroka. Midva sva doma. Vidva sta prišla.* (*sta*, *gresta*, *imata*, *sva*,
  *naju*, *vama*, *dve hiši*, *z rokama*). Never the plural for two; a dual verb with a plural
  subject is just as wrong.
- **Address**: Slovene addresses people in the nominative (*Ana, pridi sem!*, *Marko, kje si?*,
  *Gospa Kos, dober dan.*); there is no vocative.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*ker*, *ko*, *če*, *čeprav*, *ampak*, *vendar*,
  *zato*, *torej*, *saj*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*sem bil*, *je imela*,
    *je šel*), the nominative, accusative and locative in plain phrases (*imam psa*, *v Ljubljani*,
    *na mizi*), the genitive of possession and after *do*, *iz*, *od*, *pri*, *brez*, *za* (*do
    hiše*, *pri Ani*), possessives (*moj*, *naš*, *svoj*, and possessive adjectives from names: *Anin
    pes*, *Markova hiša*), the dual, simple questions (*kdo*, *kaj*, *kje*, *kdaj*, *kako*, *zakaj*,
    *ali*), negation (*ne vem*, *ne gre*; the joined *nisem*, *nimam*, *nočem*), the genitive of
    negation (*nimam časa*), *morati/moči/hoteti* + infinitive, *rad* + a verb (*rad bere*);
  - ranks 301–600: the past tense for events (*naredil sem*, *šla je*), the future with *bom* +
    the l-participle (*bom delal*, *bo prišla*), the dative (*všeč mi je*, *dati bratu*), the
    instrumental (*z bratom*, *pred hišo*), the plural, the imperative (*naredi*, *pridi*, *povej*),
    *ker*, *ko*, *ali*, simple comparatives (*boljši*, *več*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*delati* / *narediti*, the
    perfective present for the future: *ko pridem*), the verbs of motion (*iti* / *hoditi*, *iti
    z avtom*, and with *pri-*, *od-*, *iz-*, *do-*, *pre-*), reflexive verbs with *se* and *si*,
    *ki* and *kateri* (*ženska, ki stanuje zgoraj*), *kot* and *kakor* in comparisons, reported
    speech with *da* and *ali*, *preden*, *dokler ne*, *ko* with the future;
  - ranks 1001–1300: every case in the singular, dual and plural, numerals with nouns (*ena
    hiša*, *dve hiši*, *tri hiše*, *pet hiš*), comparatives and superlatives (*najboljši*,
    *največ*), the conditional (*rad bi šel*, *če bi imel čas, bi šel*), *da bi* (*želim, da bi
    prišel*), *čim*, *takoj ko*;
  - ranks 1301–1600: the indefinite pronouns (*nekdo*, *nekaj*, *kdorkoli*, *kjerkoli*, *nihče*
    with double negation: *nihče nič ne ve*), verbs that take a case (*zanimati se za*, *bati se
    česa*, *pomagati komu*), *čeprav*, *četudi*, *namesto da*, *razen*, *kljub*;
  - ranks 1601–2000: participles as adjectives in everyday use (*napisana knjiga*, *zgrajena
    hiša*, *odprta trgovina*), the passive (*hiša je bila zgrajena*, *trgovina je zaprta*),
    impersonal forms with *se* (*tu se govori slovensko*, *to se proda*, *ne sme se*), indirect
    questions with *ali*, *kot da*, *ne le... ampak tudi*, *tisti, ki*, *eden izmed*, *vsak od*;
  - ranks 2001–2500: participles in longer phrases, the gerunds in *-joč*, *-e*, *-aje* (*smeje
    se*, *molče*, *stoje*) for recognition, the past conditional (*bi bil prišel*) for recognition,
    *čim... tem*, verbal nouns (*odprtje*, *branje*), *medtem ko*, *zaradi*, *po zaslugi*,
    *glede na*; reported speech keeping the original tense, as Slovene does;
  - ranks 2501–3000: a richer, more formal register: connectors such as *kljub temu*,
    *navsezadnje*, *glede na*, *v zvezi s*, *kar zadeva*, *poleg tega*, *skratka*; long participial
    phrases; common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *kolumna* and the *podlistek* (a light reflection on everyday life, as in
    Slovene newspapers), dialogue that marks formal and informal speech (vikanje and tikanje), less
    common connectors (*dasi*, *četudi*, *kakorkoli*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style used with care; long
    sentences that stay easy to parse; tone and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a *kolumna*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Išče v kuhinji. Išče v sobi. Išče na vrtu.*).
- **Questions at the end**: after the story, a paragraph starting `Vprašanja:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *zakaj* and *kako*, with fuller answers).
  The header is the lesson's layout and is not counted:

  ```
  Vprašanja:
  Kaj želi Ana?
  Ana želi psa.
  Kam gre Ana?
  Ana gre v zavetišče.
  ```

- **Standard Slovene**: the narration is knjižna slovenščina, neutral, correct and natural Slovene
  as written today, with all its agreements, cases, the dual and aspects. No Croatian or Serbian
  words or forms (*hvala lijepa*, *kuća*, *možda*: write *hvala lepa*, *hiša*, *morda*), no dialect
  and no spoken spellings (*tud*, *zdej*, *kva*, *lohk*, *pr*, *tko*: write *tudi*, *zdaj*, *kaj*,
  *lahko*, *pri*, *tako*), no slang (*ful*, *kul* in narration, *frend*, *keš*), no vulgar words
  (the list has some: skip them). Dialogue may be lightly colloquial only in what any careful
  speaker says (*ja*, *no*, *hej*, *živjo*, *adijo*, *super*, *v redu*, *mami*, *oči* "dad"). Always
  write the Slovene letters *č*, *š*, *ž*: the checker compares them exactly, and *cas* or *zelo*
  for *čas* or *želo* are other words or none.
- **Slovene typography**: direct speech is set out as Slovene books write it, each speaker's line a
  paragraph of its own, in the quotation marks »…« (pointing inwards), with the comma, question
  mark or exclamation mark of the quoted sentence inside the marks:

  ```
  »Živjo, Ana!« reče Tomaž.
  »Živjo,« odgovori Ana. »Kako si?«
  ```

  The questions and answers of the final `Vprašanja:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*7 ur*, *ob 7.30*, *3 brati*), ordinals with a full stop (*5. maja*, *19.
  stoletje*, as Slovene writes them), except a number word that is in your band: spell that one out
  to teach it (*pet dni*). Kings take Roman numerals, which the checker does not know: avoid them or
  count them as outside words.
- **Suitable for all ages**: keep strictly to the content rules below.
- **Names are names**: place names, adjectives made from place names (*slovenski*, *ljubljanski*,
  *evropski*, *ameriški*, *nemški*) and words for peoples (*Slovenec*, *Hrvat*) count as names:
  skip them as band words even when the list has them as entries; a text may still use one as an
  outside word (*govori slovensko*). Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the Croatian or Slovak courses either: a new word list deserves
  new stories.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, priests, prayer, heaven and hell,
  angels and devils, and every custom or holiday of religious origin, even as a date, a school
  break or a gift season: Christmas (*božič*, the tree, the gifts, *Miklavž*, *dedek Mraz* as a
  Christmas figure), Easter (*velika noč*, *pirhi*, *potica* as an Easter dish), All Saints
  (*dan mrtvih*), *pust* and *kurentovanje* (Shrovetide, of church origin), Halloween, name days,
  saints' days and church fairs (*žegnanje*). Folk customs with no church holiday named are fine:
  folk costumes and songs, a harvest or village fair (*sejem*), a birthday, New Year's Eve
  (*silvestrovo*) as a family evening, Culture Day (*Prešernov dan*, 8 February), the start of the
  school year, summer holidays. *nebo* is taught only as the sky; *verjeti* only as trusting or
  believing a person or a fact; *duša*, *duh*, *vera*, *svet* in the sense "holy" are skipped
  (*svet* "world" is fine);
- **politics**, the state, elections, parties, ministers, presidents, government, borders as
  disputes, nationalism; everyday civic life (a library, a school, the town hall's counters, a
  residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  traffic warden at a school crossing is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person
  in a story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (beer, wine, spirits, a toast, a wine festival such as *martinovanje*,
  a pub or *gostilna* as a place to drink; a *gostilna* where a family eats lunch is fine),
  **gambling** (casinos, lotteries, betting), drugs and smoking;
- **deliberate misspellings**: no spellings without *č š ž* or colloquial spellings, not even in a
  child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people is skipped. Common Slovene first names are fine as characters.

The themes keep to this: the history courses (*Zgodovina na dosegu roke*, *Arheologija in
preteklost*) tell the history of everyday life, towns, crafts, trade, science and travel, never of
wars, rulers or politics; *Ljudska praznovanja in sejmi* and *Ljudska umetnost in običaji* are folk
and family celebrations with no church holiday named; *Javne službe* and *Vsakdanji uradni
opravki* are the post office, the library and the town hall's counters, never politics.

A word whose COMMON or slang sense breaks a rule is skipped even if a story could use another sense
(*zakon* is "law" before "marriage"; *stranka* is "party" before "customer"; *boj*, *napad* are
skipped even in sport). Never coin unnatural phrases to dodge outside words.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *bog* 173, *cerkev* 470, *hudič* 667, *vrag* 740, *duh* 749, *duša* 801; *nebo* 937
  only as the sky; *verjeti* 192 only as believing a person or a fact;
- politics: *država* 148, *republika* 567, *vlada* 491, *predsednik* 367, *zakon* 378, *stranka*
  482, *političen* 516, *politik* 580, *državen* 332, *zveza* 327 (only as a link or connection:
  *v zvezi s*, *zveza je slaba* on the phone), *oblast* 962, *narod* 987, *mednaroden* 441;
  *občina* 177 only as the town hall's offices and services (*na občini*), never as politics;
- war, crime, police: *ubiti* 159, *vojna* 253, *orožje* 425, *policija* 429, *napad* 431, *vojska*
  528, *umor* 600, *žrtev* 606, *vojak* 649, *zapor* 650, *boriti* 671, *sodišče* 697, *general* 699,
  *pištola* 736, *napasti* 771, *truplo* 799, *boj* 804, *vojaški* 808, *morilec* 880, *ustreliti*
  950, *policist* 967;
- death: *umreti* 157, *smrt* 240, *mrtev* 295, *kri* 462;
- vulgar and insulting: *sranje* 411, *preklet* 449, *neumen* 743, *rit* 836;
- alcohol: *pijača* 816 only as a soft drink (*topla pijača*, juice, tea);
- names, place adjectives and peoples: *slovenski* 133, *evropski* 330, *ameriški* 418, *nemški*
  623, *hrvaški* 687, *ljubljanski* 916, *kitajski* 961; *maj* 628 is the month (fine), but *Maja*
  is a name: never a character;
- list noise: *d* 825, the interjection *hej* 222 only in dialogue.

Further on, the same categories hold; for example *angel* 1076, *vino* 1080, *sodnik* 1044,
*sovražnik* 1081, *vera* 1124, *pivo* 1156, *jebati* 1158, *zločin* 1188, *grob* 1196, *streljati*
1201, *seks* 1211, *bomba* 1234, *ministrstvo* 1334, *duhovnik* 1457, *bedak* 1592, *poslanec* 1623,
*pekel* 1671, *križ* 1677, *parlament* 1681, *moliti* 1777, *alkohol* 1812, *droga* 1853, *kaditi*
1995, *pijan* 2051, *greh* 2127, *nebesa* 2140, *pogreb* 2245, *minister* 2228, *molitev* 2331,
*cigareta* 2471, *stava* 2841.

## The file

Write `tools/courses/sl/<id>.json`:

```json
{
  "id": "sl-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Slovene, e.g. Hrana in nakupi",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Slovene",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`,
up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/sl/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *šla* for *iti*, *dobrega* for *dober*, *nimam* for
  *imeti*, *gresta* for *iti*). Reuse earlier lessons' new words too. `newWords` holds the word as
  the list writes it: the infinitive of a verb (*moči*, *povedati*), the nominative singular of a
  noun, the masculine singular of an adjective (*dober*, *majhen*), and the entries the list keeps
  as they are (*dobro*, *veliko*, *vse*, *drugi*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a place adjective, a content-rule word); the checker's last line lists the band words not taught
  yet.

Things to know about the Slovene list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/sl/wordlist.tsv`
  (a copy of the app's `sl.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns in all three numbers, the conjugation with the dual (*greva*, *gresta*, *imava*), the
  l-participle of the past and conditional (*bil*, *šla*, *imeli*), the imperative, the negated
  forms of the three verbs that join *ne* (*nisem*, *ni*, *niso* under *biti*; *nimam*, *nima*
  under *imeti*; *nočem*, *noče* under *hoteti*), and the comparative and superlative of
  adjectives and adverbs (*boljši*, *najboljši* under *dober*; *bolje* under *dobro*; *več*,
  *največ* under *veliko*; *večji* under *velik*). Any other verb is negated with a separate *ne*
  (*ne vem*, *ne gre*), and the verb counts as itself.
- **Clitics and pronouns are words of their own, filed under their full word**: *sem, si, je,
  sva, sta, smo, ste, so, bom, boš, bo, bova, bosta, bomo, boste, bodo, bi* are forms of *biti* (1);
  *se* (2) is an entry of its own (*se, si, sebe, sebi, seboj*; *si* counts for *biti* and *se*);
  *me, mi, mene, meni, mano, zame, midva, naju, nama* are *jaz* (10); *te, ti, tebe, tebi, tabo,
  zate, vidva, vaju, vama* are *ti* (13, and *te* counts for *ta* too); *ga, mu, njega, njemu,
  zanj, nanj, onadva, jima* are *on* (15); *jo, ji, nje, njo, njej, zanjo* are *ona* (28); *jih,
  jim, njih, ju, zanje* are *oni* (26); *nas, nam, nami* are *mi* (23); *vas, vam, vami* are *vi*
  (41). The long and the short form of a pronoun are one word: teaching *on* teaches *ga*, *mu*,
  *njega*. *vse* (38, "everything") and *ves* (46: *vsi, vsa, vso, vsega*) are two entries; so are
  *dober* (60) and *dobro* (57), *drug* (59) and *drugi* (199, "second, other").
- **Aspect pairs are separate words**: *narediti* 101 / *delati* 123; *reči* 62 / *povedati* 75 /
  *govoriti* 102 / *praviti* 269; *videti* 56 / *gledati* 144 / *pogledati* 150; *dati* 70 /
  *dajati* 624; *priti* 49 / *prihajati* 307; *iti* 29 / *hoditi* 388; *iskati* 230 / *poiskati*
  646; *čakati* 190 / *počakati* 282; *vprašati* 361 / *spraševati* 640; *kupiti* 387 / *kupovati*
  1943; *poklicati* 221 / *klicati* 416. Teaching one does not teach the other, and each is taught
  through its own forms (*kupil*, *kupim* for *kupiti*; *kupuje* counts only for *kupovati*).
- Look-alikes filed under another word: *kot* (19, "as, like, than") holds *kotu*, *kota*
  ("corner"); *med* (61, "between, during") holds *medu* ("honey"); *red* (92) is "order" and
  gives the very common *v redu* ("all right"); *dan* (68) holds *dano*, *dani*; *jaz* holds
  *menija*, *meniju* ("menu"); *lahko* (17) holds *laže*; *sreda* ("Wednesday", 796) is ranked by
  *sredi* ("in the middle of"); *razen* (339, "except") holds *razni*, *razne* ("various"); *oko*
  (263) holds *oči* ("eyes"), so *oči* for "dad" counts as *oko* (use it only in dialogue); *star*
  (132) holds *stari* ("old", also "the parents" in speech); *svet* (115) is "world" and holds
  *sveti* ("holy"): teach it as the world; *pravi* (166, "real, right") and *praviti* (269, "to
  say") are two entries. Never teach a headword through such a form.
- Family words rank late: *otrok* 100, *oče* 130, *mama* 161, *sin* 194, *fant* 193, *brat* 243,
  *žena* 264, *mož* 273, *dekle* 372, *sestra* 413, *starš* 422, *punca* 524, *očka* 717, *hči*
  724, *hčerka* 752, *stric* 1063, *babica* 1371, *dedek* 1869, *teta* 1876; *ati* is not in the
  list. In the first courses name the people and keep the family words few.
- Everyday nouns rank later than you would think: *hiša* 183, *voda* 171, *denar* 185, *šola* 202,
  *avto* 302, *soba* 313, *pes* 438, *trgovina* 662, *stanovanje* 679, *kava* 856, *vrt* 1030,
  *kolo* 1176, *kuhinja* 1260, *čaj* 1327, *mačka* 1341, *mleko* 1625, *kruh* 1974, *jabolko*
  3029. Day and month names are in the list (*sreda* 796, *petek* 1488, *sobota* 1240, *nedelja*
  1752, *ponedeljek* 1945, *torek* 2288, *četrtek* 2478; *maj* 628, *oktober* 720, *junij* 747,
  *julij* 748, *avgust* 852, *april* 914, *september* 931, *december* 1025, *november* 1053,
  *marec* 1082, *januar* 1173, *februar* 1338); seasons rank late (*poletje* 1684, *jesen* 1948,
  *zima* 2384, *pomlad* 2916). Check every rank with `rank.py` rather than guess.
- Common forms the list misses are in `extra_forms.tsv` (headword, a tab, the forms), generated
  from the English Wiktionary's Slovene tables: duals and rarer cases and persons the list has
  nowhere (*delava*, *lepa*, *imel*). Every headword in it must be a headword of the list (the
  checker warns otherwise). Writers working in parallel report a missing true form instead of
  adding it.
- The checker compares whole words, lowercased, letter for letter, and splits a word at hyphens.
  `Vprašanja:` at the start of a line is not counted.
- A form shared by two words counts for both (*si* for *biti* and *se*, *te* for *ti* and *ta*,
  *dela* for *delo* and *delati*): use a new word in its own sense, never through its homograph,
  and count a new word's uses in forms that are its own.
- **Names** are not in the list: people, places (*Ljubljana*, *Maribor*, *Slovenija*, *Triglav*),
  shops and brands go in `names`. The checker declines a name regularly (*Ana*: *Ane, Ani, Ano*,
  *Anin, Anina*; *Marko*: *Marka, Marku, Markom, Markov*; *Peter*: *Petra, Petru*; *Jože*:
  *Jožeta*; *Ljubljana*: *Ljubljani*; *Celje*: *Celju*), so list the name in its nominative; list a
  form it misses as well. A name of several words (*Novo mesto*, *Murska Sobota*, *Škofja Loka*)
  counts as one known word where it is written whole as listed; a declined form (*v Novem mestu*)
  counts word by word, and a part that is a list word counts at its own rank (*Novem* is *nov* 90,
  *mestu* is *mesto* 87, *Sobota* is *sobota* 1240, *Gorica* is *gorica* 1382). Keep only true
  names in `names`, never common words, and never a name that is also a list word: *Maja* (*maj*
  628), *Mira* (*mir* 428), *Rok* (*rok* 1071), *Luka* (*luka* 1711), *Vid* (2609), *Jure* (*jura*),
  *Brina* (*brin*); towns *Koper* (*koper*), *Bled*, *Kras* are list words too. Safe: *Ana*, *Nina*,
  *Eva*, *Sara*, *Lara*, *Petra*, *Mojca*, *Urška*, *Tina*, *Nika*, *Špela*, *Katja*, *Barbara*,
  *Mateja*, *Vesna*, *Zala*, *Neža*, *Ajda*, *Lana*, *Hana*, *Ema*, *Kaja*, *Marko*, *Matej*,
  *Miha*, *Jan*, *Žiga*, *Nejc*, *Tomaž*, *Gregor*, *Peter*, *Andrej*, *Janez*, *Primož*, *Blaž*,
  *Aljaž*, *Gal*, *Tim*, *Anže*, *Klemen*, *Boštjan*, *Simon*, *Tilen*, *Žan*, *Martin*, *Lovro*,
  *Jakob*, *Filip*, *Jože*, *Igor*, *Uroš*, *Domen*, *Tadej*, *Matevž*. Never put a holiday of
  religious origin in `names` (see the content rules).
- The list's form lists are noisy (unrelated words, look-alikes); a new word must appear as itself
  or a true inflection, never through such a form. Some entries are names, abbreviations or
  English words: teach a loanword Slovene uses as its own (*super*, *film*, *internet*,
  *računalnik*), skip the English ones.
