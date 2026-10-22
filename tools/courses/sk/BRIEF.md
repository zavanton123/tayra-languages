# Writing a Slovak frequency course

Each course teaches one band of 100 words from the Slovak word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `sk-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Jana chce mačku. Ide do útulku. Vidí tam tri mačky...* Everyday life in
  Slovakia: Bratislava, Košice, Prešov, Žilina, Nitra, Banská Bystrica, Trnava, Trenčín, Martin,
  Poprad, Piešťany and the spa towns, the High and Low Tatras, Malá Fatra and Slovenský raj, Orava,
  Liptov and Spiš, the Danube, the Váh and the Hron, the vineyards below the Small Carpathians,
  villages, weekend cottages (*chata*) and allotment gardens (*záhradka*); and now and then Vienna,
  Prague, Budapest or London, where Slovak is spoken at home: home, family, food, shopping, work,
  school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Petrs
  in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (*Ráno ide Jana do práce.*; the clitics *sa*, *si*, *mi*, *ti*, *ho*, *mu*, *som*, *sme*, *ste*,
  *by* in second position: *Volá sa Peter. Dal mu ho. Išla som domov. Rád by som to videl.*).
  Present tense up to rank 500, with the future of *byť* (*budem*, *bude*) and
  *chcieť/môcť/musieť* + infinitive where natural; from rank 600 the story may be told in the past
  (*Jana išla z domu. Pršalo.*). Slovak drops subject pronouns: write *on*, *ona*, *oni* only where
  Slovak would (contrast, emphasis, a new subject); in the past the auxiliary carries the person
  (*Musel som*, *Išla si*), and the third person has none (*Jana išla*). Little dialogue; when there
  is some, short and simple, with *ty* (tykanie) among family and friends and *vy* + the second
  person plural (vykanie) with strangers, older people and in formal settings (*Bývate tu?*,
  *pán Novák*, *pani Horváthová*).
- **Address**: standard Slovak addresses people in the nominative (*Jana, poď sem!*, *Peter, kde
  si?*, *Pán Novák, dobrý deň.*), not in a Czech-style vocative (*Jano*, *Petře* are wrong). Only a
  few set forms of address remain, mostly in speech (*mami*, *oci*); use them sparingly.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*pretože*, *lebo*, *keď*, *ak*, *hoci*, *ale*,
  *preto*, *takže*, *však*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*bol*, *bola*, *mal*,
    *išiel*), the nominative, accusative and locative in plain phrases (*mám mačku*, *v Košiciach*,
    *na stole*), the genitive of possession and after *do*, *z*, *od*, *u*, *bez* (*do práce*,
    *u Jany*), possessives (*môj*, *náš*, *svoj*, and possessive adjectives from names: *Janina
    mačka*, *Petrov byt*), simple questions (*kto*, *čo*, *kde*, *kedy*, *ako*, *prečo*, *či*),
    negation with *ne-* joined to the verb (*nemám*, *nejde*), *chcieť/môcť/musieť* + infinitive;
  - ranks 301–600: the past tense for events (*urobil som*, *išla*), the future with *budem* +
    imperfective infinitive (*budem pracovať*), the dative (*páči sa mi*, *dať bratovi*), the
    instrumental (*s bratom*, *je lekárom*), the plural, the imperative (*urob*, *poď*, *povedz*),
    *pretože*, *keď*, *či*, simple comparatives (*lepší*, *viac*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*robiť* / *urobiť*, the
    perfective future *urobím*), the verbs of motion (*ísť* / *chodiť*, *ísť autom*, and with
    *pri-*, *od-*, *vy-*, *do-*, *pre-*), reflexive verbs with *sa* and *si*, *ktorý* in the
    nominative and accusative, *ako* and *než* in comparisons, reported speech with *že* and *či*,
    *kým* and *skôr ako* ("before"), *keď* with the future;
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*dva domy*,
    *päť domov*, *dvoje dvere*), the masculine personal plural (*tí študenti boli*, *dvaja
    chlapci*), comparatives and superlatives (*najlepší*, *najviac*), *ktorý* in every case, the
    conditional (*chcel by som*, *keby som mal čas, išiel by som*), *aby* + the past form (*chcem,
    aby si prišiel*), *kým ne-*, *len čo*;
  - ranks 1301–1600: the indefinite pronouns (*niekto*, *niečo*, *ktokoľvek*, *kdekoľvek*, *nikto*
    with double negation: *nikto nič nevie*), verbs that take a case (*zaujímať sa o niečo*, *báť
    sa niečoho*, *pomáhať niekomu*), *hoci*, *aj keď*, *namiesto toho, aby*, *okrem*, *bez ohľadu
    na*;
  - ranks 1601–2000: participial adjectives in everyday use (*pracujúci*, *napísaná kniha*,
    *postavený dom*), the passive (*dom bol postavený*, *je zatvorené*), impersonal forms with *sa*
    (*hovorí sa*, *predáva sa*, *dá sa*), indirect questions with *či*, *akoby*, *nielen... ale
    aj*, *ten, kto*, *jeden z*, *každý z*;
  - ranks 2001–2500: participles in longer phrases, the past conditional (*keby to bol vedel, bol by
    prišiel*, for recognition), *čím... tým*, verbal nouns (*otvorenie*, *stavanie*), *zatiaľ čo*,
    *kvôli*, *vďaka*, *podľa*; reported speech keeping the original tense, as Slovak does;
  - ranks 2501–3000: a richer, more formal register: connectors such as *napriek tomu*, *koniec
    koncov*, *vzhľadom na*, *v súvislosti s*, *čo sa týka*, *ohľadom*; long participial phrases;
    common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *fejtón* (a light reflection on everyday life, as in Slovak newspapers),
    dialogue that marks formal and informal speech (vykanie and tykanie), less common connectors
    (*hoc*, *čo aj*, *akokoľvek*), the transgressive (*idúc*, *súc*) only for recognition, in a set
    phrase;
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *fejtón*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Hľadá v kuchyni. Hľadá v izbe. Hľadá v predsieni.*).
- **Questions at the end**: after the story, a paragraph starting `Otázky:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *prečo* and *ako*, with fuller answers):

  ```
  Otázky:
  Čo chce Jana?
  Jana chce mačku.
  Kam ide Jana?
  Jana ide do útulku.
  ```

- **Standard Slovak**: the narration is spisovná slovenčina, neutral, correct and natural Slovak
  as written today, with all its agreements, cases and aspects and the rhythmic law (*krásne*,
  *píšu*, *vidia*: no two long syllables in a row). No Czech words or Czech forms (*teď*, *děkuju*,
  *jo* in narration, *ještě*: write *teraz*, *ďakujem*, *áno*, *ešte*), no dialect, no slang
  (*kamoš*, *chalan*, *kecať*, *frajer*), no vulgar words (the list has some: skip them). Dialogue
  may be lightly colloquial only in what any careful speaker says (*hej*, *no*, *fajn*, *super*,
  *okej*, *mami*, *oci*). The stories are for readers of every age and country, so keep to the
  standard language and let a regional or colloquial word stand only where its context makes it
  clear. Always write the Slovak letters (*á ä č ď é í ĺ ľ ň ó ô ŕ š ť ú ý ž*): the checker compares
  them exactly, and *dakujem* or *moze* are not words.
- **Slovak typography**: direct speech is set out as Slovak books write it, each speaker's line a
  paragraph of its own in Slovak quotation marks „…“ (a low opening mark and a high closing one),
  with the comma or the question mark of the quoted sentence inside the marks:

  ```
  „Ahoj, Jana!“ hovorí Tomáš.
  „Ahoj,“ odpovedá Jana. „Ako sa máš?“
  ```

  The questions and answers of the final `Otázky:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*7 hodín*, *o 7.30*, *3 bratia*), ordinals with a full stop (*5. mája*,
  *19. storočie*, as Slovak writes them), except a number word that is in your band: spell that one
  out to teach it (*päť dní*). Kings take Roman numerals, which the checker does not know: avoid
  them or count them as outside words.
- **Suitable for all ages**: keep strictly to the content rules below.
- **Names are names**: place names, adjectives made from place names (*slovenský*, *český*,
  *európsky*, *americký*, *bratislavský*) and words for peoples (*Slovák*, *Nemec*) count as names:
  skip them as band words even when the list has them as entries; a text may still use one as an
  outside word. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the Czech courses either: a new word list deserves new stories.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church and chapel, priests, prayer, heaven and hell,
  angels and devils, and every custom or holiday of religious origin, even as a date, a school
  break or a gift season: Christmas (*Vianoce*, *Štedrý deň*, the tree, the carp, the gifts),
  Easter (*Veľká noc*, painted eggs, *šibačka*), All Saints, Mikuláš, Halloween, Thanksgiving,
  and name days (*meniny*, which come from the calendar of saints). Folk customs with no church
  holiday named are fine: folk costumes and songs, the *fujara*, a harvest festival (*dožinky*), a
  village fair (*jarmok*), raising a maypole (*stavanie mája*), a birthday, New Year's Eve
  (*Silvester*) as a family evening, the start of the school year, summer holidays. *nebo* is
  taught only as the sky; *veriť* only as trusting or believing a person or a fact; *duša*, *duch*
  and *viera* are skipped;
- **politics**, the state, elections, parties, ministers, presidents, government, borders as
  disputes, nationalism; everyday civic life (a library, a school, the *obecný úrad*, a
  residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  traffic warden at a school crossing is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person
  in a story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (beer, wine, spirits, a toast, a wine festival such as *vinobranie*,
  a pub), **gambling** (casinos, lotteries, betting), drugs and smoking;
- **deliberate misspellings**: no spellings without diacritics or colloquial spellings, not even
  in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people is skipped. Common Slovak first names are fine as characters.

The themes keep to this: the history courses (*Z dejín*, *Kapitoly zo svetových dejín*, *História na
dosah*) tell the history of everyday life, towns, crafts, trade, science and travel, never of wars,
rulers or politics; *Ľudové slávnosti a oslavy* and *Ľudové umenie a zvyky* are folk and family
celebrations with no church holiday named; *Úrady a služby* is the post office, the library and the
town hall's counters, never politics.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *boh* 130, *peklo* 580, *svätý* 641, *duša* 666, *duch* 682, *kostol* 754, *cirkev* 873,
  *viera* 992; *nebo* 916 only as the sky;
- politics: *štát* 189, *republika* 397, *národný* 418 (except *národný park*), *vláda* 421,
  *zákon* 384, *prezident* 502, *voľba* 523, *politika* 691, *voliť* 914, *predseda* 911 (except
  the head of a club), *ministerstvo* 949, *mier* 947;
- war, crime, police: *zabiť* 162, *vojna* 315, *zbraň* 342, *polícia* 367, *súd* 480, *boj* 507,
  *bojovať* 512, *vojak* 651, *útok* 657, *armáda* 662, *vražda* 663, *policajt* 791, *ukradnúť*
  836, *veliteľ* 842, *nepriateľ* 855, *vojenský* 918, *väzenie* 961, *vrah* 965, *generál* 978;
- death: *zomrieť* 239, *smrť* 286, *mŕtvy* 338, *krv* 475; *prežiť* 680 only as spending time
  (*prežili pekné leto*);
- vulgar and sexual: *kurva* 437, *sex* 519, *riť* 639;
- slang: *kamoš* 734, *chalan* 960;
- names, place adjectives and peoples: *slovenský* 138, *európsky* 373, *český* 554, *americký*
  572, *nemecký* 759, *čína* 833, *anglický* 935, *ruský* 980;
- list noise: *zob* 640 (holds *mi*), *stop* 681 (English, holds *stopa*), *veko* 944 (ranked by
  *veku*, a form of *vek*), and the interjections *ha* 659 and *hm* 889; *žiaden* 1071 and
  *znovu* 576 are variants of *žiadny* 124 and *znova* 495, taught already.

Further on, the same categories hold; for example *víno* 1075, *pivo* 1169, *alkohol* 1555, *droga*
1146, *bomba* 1144, *zločin* 1369, *kňaz* 1383, *trest* 1385, *anjel* 1400, *sudca* 1429, *kríž*
1529, *čert* 1568, *modliť* 1684, *hrob* 1845, *pápež* 2000, *minister* 2016, *zlodej* 2036, *fajčiť*
2245, *pohreb* 2302, *cintorín* 2418, *stávka* 2866, *kasíno* 3662, *lotéria* 6440, *veľkonočný*
5517, the negated verb entries (below), and the vulgar *sranda* 1035, *idiot* 1389, *hovno* 1752.

## The file

Write `tools/courses/sk/<id>.json`:

```json
{
  "id": "sk-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Slovak, e.g. Jedlo a nákupy",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
  "lessons": [
    {
      "title": "Story title in Slovak",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/sk/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *išla* for *ísť*, *dobrou* for *dobrý*, *nemám* for *mať*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the
  infinitive of a verb (*môcť*, *povedať*), the nominative singular of a noun (but *ľudia*, which
  the list keeps as a word), the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a negated verb, a place adjective); the checker's last line lists the band words not taught yet.

Things to know about the Slovak list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/sk/wordlist.tsv`
  (a copy of the app's `sk.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation, the past forms and the participles of verbs, the negated verb forms
  (*nemám* under *mať*, *nejde* under *ísť*), the comparative and superlative of adjectives
  (*lepší*, *najlepší* under *dobrý*; *väčší* under *veľký*), and often words made from it:
  adjectives made from nouns (*mestský* under *mesto*, *detský* under *dieťa*, *ženský* under
  *žena*, *rodinný* under *rodina*, *štátny* under *štát*, *svetový* under *svet*, *obchodný* under
  *obchod*, *slávny* under *sláva*), feminine nouns under the masculine (*manželka* under *manžel*,
  *priateľka* under *priateľ*, *učiteľka* under *učiteľ*, *herečka* under *herec*, *doktorka* under
  *doktor*, *kráľovná* under *kráľ*, *princezná* under *princ*), diminutives (*obrázok* under
  *obraz*, *lístok* under *list*, *mestečko* under *mesto*, *rybka* under *ryba*, *záhradka* under
  *záhrada*), verbal nouns (*čítanie* under *čítať*, *riadenie* under *riadiť*; but *predstavenie*,
  "a show", is under *predstaviť* too) and some adverbs in -o: *jednoducho* under *jednoduchý*,
  *ťažko* under *ťažký*, *ľahko* under *ľahký*, *ďaleko* under *ďaleký*, *blízko* under *blízky*,
  *vysoko* under *vysoký*, *hlboko* under *hlboký*, *teplo* under *teplý*, *čisto* under *čistý*.
  Such a word counts at its headword's rank, but it is not that word: never teach a headword
  through it (*manžel* through *manželka*, *ďaleký* through *ďaleko*). Most adverbs are entries of
  their own (*dobre* 50, *veľmi* 65, *rýchlo* 249, *dlho* 254, *často* 287, *správne* 347, *zle*
  436, *pekne* 496), and so are *ľudia* (84, next to *človek* 112), *doma* (380) and *domov* (218,
  "home(wards)" and "a home"). *všetci*, *všetko*, *všetky* are filed under *všetok*; *ono*, *ony*,
  *nim*, *nimi* under *on*; *radšej* under *rád*.
- Perfective and imperfective verbs are often entries of their own (*robiť* 64, *urobiť* 89;
  *hovoriť* 68, *povedať* 46; *dávať* 296, *dať* 55; *chodiť* 153, *ísť* 30; *pozerať* 416,
  *pozrieť* 71; *volať* 198, *zavolať* 318; *čakať* 222, *počkať* 203; *písať* 411, *napísať*
  400; *učiť* 537, *naučiť* 521; *brať* 406, *vziať* 228, *zobrať* 357; *meniť* 927, *zmeniť* 304;
  *predávať* 1136, *predať* 555; *vracať* 1031, *vrátiť* 154), so teaching one does not teach the
  other. Many pairs are one entry, filed under the commoner partner, usually the perfective:
  *vidieť* holds *uvidieť*, *prísť* *prichádzať*, *dostať* *dostávať*, *nájsť* *nachádzať*,
  *začať* *začínať*, *pomôcť* *pomáhať*, *zostať* *zostávať*, *odísť* *odchádzať*, *ukázať*
  *ukazovať*, *rozhodnúť* *rozhodovať*, *získať* *získavať*, *otvoriť* *otvárať*, *stretnúť*
  *stretávať*, *prejsť* *prechádzať*, *vytvoriť* *vytvárať*, *kúpiť* *kupovať*, *priniesť*
  *prinášať*, *zabudnúť* *zabúdať*, *vyjsť* *vychádzať*, *vyhrať* *vyhrávať*, *vyrobiť*
  *vyrábať*, *vstať* *vstávať*, *variť* *uvariť*; the other way round *čítať* holds *prečítať*,
  *pýtať* *spýtať*, *rozumieť* *porozumieť*, *počúvať* *počúvnuť*, *jesť* *zjesť*, *spievať*
  *zaspievať*, *chytať* *chytiť*, *niesť* *doniesť*, *klásť* *položiť*, and *zatvoriť* holds
  *zavrieť*. The list had most finite forms of the filed partner nowhere (*začínajú*, *prinášal*,
  *kupujem*); `extra_forms.tsv` files them under the entry, so they count. Such an entry is taught
  through its own forms (*začal*, *začne*), never through the partner's (*začína*), which only count
  for it.
- **Negated verbs as entries**: from rank 1200 on the list keeps about 330 negated verbs as entries
  of their own (*neurobiť* 1257, *neísť* 1615, *nevrátiť* 1653, *nefungovať* 1654, *nepomôcť*,
  *nerozumieť* 2017...). They are forms of verbs taught earlier: never teach them, and skip them in
  your band. `extra_forms.tsv` files their forms under the verb as well (*nešiel*, *nešla* count
  for *ísť* 30). *nechať* (132, "to let"), *nechávať*, *nenávidieť* ("to hate") and *neznášať*
  are real verbs.
- Some headwords are not the word you would expect, and some hold two words: *stať* is "to happen"
  (*stalo sa*) and "to become" (*stal sa lekárom*), *stáť* is "to stand" and "to cost"; *moc* is
  "very much" and "power"; *miesto* is "place" and "instead of"; *rok* holds *rokov*, *rokmi*
  (*je mu 10 rokov*); *večer* holds *večera* ("dinner"): teach *večer* through its own forms
  (*večer*, *večera* as its genitive, *večeri*), never through dinner; *viesť* holds the particle
  *veď* ("after all"), which counts for it; *kráľ* holds *králik* ("rabbit"); *list* holds *lístok*
  ("ticket"); *stop* (681) holds *stopa* ("trace, footprint") and is otherwise English: skip it;
  *dátum* holds *dáta* ("data"); *veko* (944, "lid") is ranked by *veku*, the forms of *vek* ("age",
  1424, which holds only *vek*, *veky*, *vekom*...): skip *veko*, and teach *vek* through *vek* or skip it;
  *sto* (848) shares *ste* with *byť*, so its rank is too high: teach it through *sto*; *zob* (640)
  is list noise holding *mi*: skip it. *mi* is *ja*; *ti*, *ťa*, *teba*, *tebe*, *tebou* are *ty*;
  *vám*, *vás*, *vami* are *vy*; *sa*, *si*, *sebe*, *sebou* are *seba* (*si* is also *byť*: *ty si*);
  *ho*, *mu*, *jeho*, *neho*, *ňom* are *on*; *ju*, *jej*, *nej*, *ňu*, *ňou* are *ona*; *ich*,
  *im*, *nich* are *oni*; *tvoj* (75) and *váš* (62) are entries.
- Slovak writes the conditional and *aby*/*keby* with separate words (*by som*, *aby si*, *keby
  sme*): *som*, *si*, *sme*, *ste* count for *byť*; the particle *by* (24) is an entry of its own,
  as are *aby* (39) and *keby* (190).
- Words the list lacks entirely, because the dictionary has only a surname spelled the same
  (*studený*, *tichý*, *veselý*, *konečný*, *horský*, *tučný*, *olej*, *plech*, *sako*, *krivý*,
  *pekár*, *záhradník*) or no entry at all (*nikde*, *inde*, *niekam*, *nikam*, *kedykoľvek*,
  *zopár*, *dovtedy*, *odvtedy*, *každoročne*, *trikrát*, *zdarma*, *poobede*, *oci*). They are
  outside words at every rank: prefer *chladný* (2051), *zima* (*je mi zima*), *pokojný* (2295),
  or count them as outside words. *lesný* is filed under *les* (714), *košík* under *kôš* (2390).
- Family words rank late: *mama* 186, *otec* 126, *matka* 291, *syn* 223, *brat* 288, *dcéra*
  359, *sestra* 415, *ocko* 1070 (its address form *oci* is not in the list), *babka* 2361,
  *dedko* 3486, *dedo* 4332. In the first courses name the people and keep the family words few.
- Look-alikes filed under another word: *hore* ("up") is a form of *hora* (335), so it counts only
  from 335 and never teaches *hora*; *stane* (*v stane*, "in the tent") is a form of *stať* (102);
  *večera* ("dinner") is under *večer*; *mater*, *materský* are under *mať*; *roba*, *robom* under
  *robiť*; *mamy*, *mamu* are filed by simplemma under *mam* but the list keeps them under *mama*.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated from the English Wiktionary Slovak dictionary (`dictionaries/sk-en.sqlite`), wordfreq
  and simplemma: dictionary forms the list lacks, negated verb forms (*nerobia*, *nezabudni*), the
  finite forms of the filed aspect partner (*začínajú* under *začať*), the forms of the negated
  verb entries under the verb, and the other forms of words filed under a headword (*prírodných*
  of *prírodný*, under *príroda*). Every headword in it must be a headword of the list (the checker
  warns otherwise). Add a line there for another true form you need rather than writing around it,
  and say so in your report (while writing in parallel, report it instead).
- The checker compares whole words, lowercased, letter for letter, and splits a word at hyphens
  (*e-mail* is *e* + *mail*, and *e* is not in the list, so write *mail* (4329) or *správa*).
  `Otázky:` counts as the word *otázky* (*otázka*, rank 262).
- A form shared by two words counts for both (*si* for *byť* and *seba*, *ste* for *byť* and *sto*,
  *kým* for *kto*), or only for the one the list files it under: use a new word in its own sense,
  never through its homograph, and count a new word's uses in forms that are its own.
- **Names** are not in the list: people, places (*Bratislava*, *Košice*, *Slovensko*, *Tatry*),
  shops and brands go in `names`. The checker does not decline them, so list every form the text
  uses: *Jana*, *Jany*, *Jane*, *Janu*, *Janou*; *Bratislava*, *Bratislavy*, *Bratislave*,
  *Bratislavu*; *Peter*, *Petra*, *Petrovi*, *Petrom*; *Košice*, *Košíc*, *Košiciach*; and the
  possessive adjectives the text makes from them (*Janin*, *Janina*, *Petrov*, *Petrova*). The
  checker counts a name of several words word by word: list each part that is not a list word
  (*Banská*, *Bystrica*), and know that a part which is a list form counts at its own rank (*Nové*
  is *nový* 86, *Vysoké* is *vysoký* 146, *Mesto* is *mesto* 109). Keep only true names in `names`,
  never common words, and never a name that is also a list word (*Mária* is fine, but not the
  surnames *Kováč*/*Kováčová*, *Holub*, *Krajčír*, which are list words): then call the character
  something else. Never put a holiday of religious origin in `names` (see the content rules). Day and month names are in the list (*streda* 1213, *sobota* 1427, *piatok* 1601,
  *nedeľa* 1709, *pondelok* 2115, *utorok* 2716, *štvrtok* 2738; *september* 891, *máj* 904,
  *október* 907, *jún* 928, *november* 952, *apríl* 986, *január* 1030, *marec* 1040: check
  their ranks).
- The list's form lists are noisy (unrelated words, look-alikes); a new word must appear as itself
  or a true inflection, never through such a form. Some entries are names, abbreviations or
  English words (*super*, *okej*, *stop*): teach a loanword Slovak uses as its own (*super*, *fajn*,
  *film*, *internet*, *počítač*, *okej* in dialogue), skip the English ones.
