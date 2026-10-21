# Writing a Czech frequency course

Each course teaches one band of 100 words from the Czech word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `cs-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Jana chce kočku. Jde do útulku. Vidí tam tři kočky...* Everyday life in
  Czechia: Prague, Brno, Ostrava, Plzeň, Olomouc, Liberec, České Budějovice, Hradec Králové,
  Pardubice, Zlín, Karlovy Vary and the spa towns, Český Krumlov, the Krkonoše and Šumava
  mountains, South Moravia and its vineyards, the Vltava and the ponds of South Bohemia, villages,
  weekend cottages (*chata*, *chalupa*) and allotment gardens (*zahrádka*); and now and then Vienna,
  Bratislava, London or Berlin, where Czech is spoken at home: home, family, food, shopping, work,
  school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Tomášes
  in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (the verb-second habits of Czech: *Ráno jde Jana do práce.*; the clitics *se*, *si*, *mi*, *ho*,
  *jsem*, *by* in second position: *Jmenuje se Petr. Dal mu ho. Šla jsem domů.*). Present tense up to
  rank 500, with the future of *být* (*budu*, *bude*) and *chtít/moct/muset* + infinitive where
  natural; from rank 600 the story may be told in the past (*Jana šla z domu. Pršelo.*). Czech drops
  subject pronouns: write *on*, *ona*, *oni* only where Czech would (contrast, emphasis, a new
  subject). Little dialogue; when there is some, short and simple, with *ty* (tykání) among family
  and friends and *vy* + the second person plural (vykání) with strangers, older people and in
  formal settings (*Bydlíte tady?*, *pane Nováku*, *paní Dvořáková*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*protože*, *když*, *až*, *i když*, *ale*, *proto*,
  *takže*, *však*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands, each band using
  what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*byl*, *byla*, *měl*,
    *šel*), the nominative, accusative and locative in plain phrases (*mám kočku*, *v Brně*,
    *na stole*), the genitive of possession and after *do*, *z*, *od*, *u*, *bez* (*do práce*,
    *u Jany*), possessives (*můj*, *náš*, *svůj*, and possessive adjectives from names: *Janina
    kočka*, *Petrův byt*), simple questions (*kdo*, *co*, *kde*, *kdy*, *jak*, *proč*, *jestli*),
    negation with *ne-* joined to the verb (*nemám*, *nejde*), *chtít/moct/muset* + infinitive;
  - ranks 301–600: the past tense for events (*udělal jsem*, *šla*), the future with *budu* +
    imperfective infinitive (*budu pracovat*), the dative (*líbí se mi*, *dát bratrovi*), the
    instrumental (*s bratrem*, *je lékařem*), the plural, the imperative (*udělej*, *pojď*,
    *řekni*), *protože*, *když*, *jestli*, simple comparatives (*lepší*, *víc*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*dělat* / *udělat*, the
    perfective future *udělám*), the verbs of motion (*jít* / *chodit*, *jet* / *jezdit*, and with
    *při-*, *od-*, *vy-*, *do-*, *pře-*), reflexive verbs with *se* and *si*, *který* in the
    nominative and accusative, *než* in comparisons, reported speech with *že* and *jestli*,
    *než* ("before"), *až* ("when", future);
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*dva domy*,
    *pět domů*, *dvoje dveře*), the masculine animate plural (*ti studenti byli*, *ty holky byly*),
    comparatives and superlatives (*nejlepší*, *nejvíc*), *který* in every case, the conditional
    (*chtěl bych*, *kdybych měl čas, jel bych*), *aby* + the past form (*chci, abys přišel*),
    *dokud ne*, *jakmile*;
  - ranks 1301–1600: the indefinite pronouns (*někdo*, *něco*, *kdokoli*, *kdekoli*, *nikdo* with
    double negation: *nikdo nic neví*), verbs that take a case (*zajímat se o něco*, *bát se
    něčeho*, *pomáhat někomu*), *i když*, *přestože*, *místo toho, aby*, *kromě*, *bez ohledu na*;
  - ranks 1601–2000: participial adjectives in everyday use (*pracující*, *napsaná kniha*,
    *postavený dům*), the passive (*dům byl postaven*, *je zavřeno*), impersonal forms with *se*
    (*říká se*, *prodává se*, *dá se*), indirect questions with *jestli* and *zda*, *jako by*,
    *nejen... ale i*, *ten, kdo*, *jeden z*, *každý z*;
  - ranks 2001–2500: participles in longer phrases, the past conditional (*kdyby to byl věděl,
    byl by přišel*, for recognition), *čím... tím*, verbal nouns (*otevření*, *stavění*),
    *zatímco*, *kvůli*, *díky*, *podle*; reported speech keeping the original tense, as Czech does;
  - ranks 2501–3000: a richer, more formal register: connectors such as *nicméně*, *ostatně*,
    *vzhledem k*, *v souvislosti s*, *co se týče*, *ohledně*; long participial phrases; common idioms
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *fejeton* (a light reflection on everyday life, as in Czech newspapers
    since Neruda and Čapek), dialogue that marks formal and informal speech (vykání and tykání),
    less common connectors (*ač*, *byť*, *leč*, *jakkoli*), the transgressive (*řka*, *maje*) only
    for recognition, in a set phrase or a literary quotation;
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *fejeton*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Hledá v kuchyni. Hledá v pokoji. Hledá v předsíni.*).
- **Questions at the end**: after the story, a paragraph starting `Otázky:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *proč* and *jak*, with fuller answers):

  ```
  Otázky:
  Co chce Jana?
  Jana chce kočku.
  Kam jde Jana?
  Jana jde do útulku.
  ```

- **Standard Czech**: the narration is spisovná čeština, neutral, correct and natural Czech as
  written today, with all its agreements, cases and aspects. Common spoken Czech (obecná čeština:
  *dobrej*, *mlíko*, *bejt*, *voni*, *velký* for *velké*, *s těma lidma*) is not used, not even in
  dialogue; dialogue may be lightly colloquial only in what any careful speaker says (*jo*, *taky*,
  *teda*, *fajn*, *mami*, *tati*). No slang, no vulgar words (the list has some: skip them). The
  stories are for readers of every age and country, so keep to the standard language and let a
  regional or colloquial word stand only where its context makes it clear. Always write the Czech
  letters (*á č ď é ě í ň ó ř š ť ú ů ý ž*): the checker compares them exactly, and *dekuji* or
  *muze* are not words.
- **Czech typography**: direct speech is set out as Czech books write it, each speaker's line a
  paragraph of its own in Czech quotation marks „…“ (a low opening mark and a high closing one),
  with the comma or the question mark of the quoted sentence inside the marks:

  ```
  „Ahoj, Jano!“ říká Tomáš.
  „Ahoj,“ odpovídá Jana. „Jak se máš?“
  ```

  The questions and answers of the final `Otázky:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*7 hodin*, *v 7.30*, *3 bratři*), ordinals with a full stop (*5. května*,
  *19. století*, as Czech writes them), except a number word that is in your band: spell that one
  out to teach it (*pět dní*). Kings take Roman numerals (*Karel IV.*), which the checker does not
  know: avoid them or count them as outside words.
- **Suitable for all ages**: no drugs or alcohol as a topic (a glass of beer or wine at a family
  table is everyday Czech life, not a topic), no sex, violence, war, cruelty or gore. Keep away
  from politics, the army, the state's doings and nationalism, from current conflicts, and from
  religion taken sides on; everyday civic life (a library, a school board, a housing association
  meeting) and the customs of the holidays (*Vánoce*, *Štědrý den*, *Velikonoce*, *Mikuláš*) are
  fine. Skip band words that only fit such topics, and say so in your report.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/cs/<id>.json`:

```json
{
  "id": "cs-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Czech, e.g. Jídlo a nákupy",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
  "lessons": [
    {
      "title": "Story title in Czech",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/cs/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *šla* for *jít*, *dobrou* for *dobrý*, *nemám* for *mít*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the
  infinitive of a verb (*moct*, *říct*, not *moci*, *říci*), the nominative singular of a noun, the
  masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word);
  the checker's last line lists the band words not taught yet.

Things to know about the Czech list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/cs/wordlist.tsv`
  (a copy of the app's `cs.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation, the past forms and the participles of verbs, the negated verb forms
  (*nemám* under *mít*, *nejde* under *jít*), the comparative and superlative of adjectives
  (*lepší*, *nejlepší* under *dobrý*; *větší* under *velký*; *menší* under *malý*), some adverbs
  (*česky* under *český*, *nově* under *nový*), and often words made from it: diminutives (*domek*
  under *dům*, *autíčko* under *auto*, *holčička* under *holka*, *pejsek* under *pes*) and
  adjectives made from nouns (*pracovní* under *práce*, *rodinný* under *rodina*, *lidský* under
  *člověk*, *ženský* under *žena*). Such a word counts at its headword's rank, but it is not that
  word: never teach a headword through it. Most adverbs are entries of their own (*dobře* 116,
  *rychle*, *dlouho*, *hodně*, *špatně*), and so are *všichni* and *všechno* (next to *všechen*,
  whose forms are *všechny*, *vše*, *všech*, *všem*...), *tady*, *doma* and *domů*, *ono* and *ony*,
  *pomocí*, *většinou*, *hodiny* ("clock", next to *hodina*) and *vysoká* (*vysoká škola*, next to
  *vysoký*). *lidé* and *lidi* are filed under *člověk*; *bych*, *bys*, *bychom*, *byste* under
  *být* (the particle *by* is an entry of its own, rank 29, holding only *by*); *abych* and
  *abys* under *aby*; *kdybych* under *kdyby*.
- Perfective and imperfective verbs are often entries of their own (*dělat* 91, *udělat* 126;
  *říkat* 100, *říct* 66; *dávat*, *dát*; *chodit*, *jít*; *jezdit*, *jet*; *vracet*, *vrátit*;
  *kupovat*, *koupit*; *ptát*, *zeptat*), so teaching one does not teach the other. Many pairs are
  one entry, filed under the commoner partner, usually the perfective: *dostat* holds *dostávat*,
  *začít* *začínat*, *nechat* *nechávat*, *zůstat* *zůstávat*, *vyjít* *vycházet*, *dojít*
  *docházet*, *přijmout* *přijímat*, *zapomenout* *zapomínat*, *přinést* *přinášet*,
  *navštívit* *navštěvovat*, *podat* *podávat*, *hodit* *házet*, *požádat* *žádat*, *splnit*
  *plnit*, *strávit* *trávit*, *učinit* *činit*, *zničit* *ničit*, *položit* *klást*; *padat*
  holds *padnout*, *poslouchat* *poslechnout*. Such an entry is taught through its own forms
  (*dostal*, *dostane*), never through the partner's (*dostává*), which only count for it. *volat*
  (1187) and *zavolat* (1823) are two entries: teach each through its own forms (*volá*, *volal*;
  *zavolá*, *zavolal*).
- Some headwords are not the word you would expect, and some hold two words: *stát* is "to stand",
  "to become" (*stalo se*) and "the state" (*státu*); *moc* is "very" and "power"; *místo* is
  "place" and "instead of"; *rok* holds *let* ("years", *je mu 10 let*); *mysl* holds *myslím*,
  *myslí* (they count for *myslet* too); *ráno* is the morning, but *rána* (*do rána*, *z rána*) is
  filed under *rána* ("a blow", 995), so write *do rána* only as an outside word; *král* holds
  *královna*; *jen* holds *jenů* ("yen"); *září* ("September") holds *zářit*. Look-alikes the list
  once mixed up are now entries of their own: *zlý* (not *zlo*), *nos* (not *nosit*), *brána* and
  *branka* (not *brát*), *věda* and *vědecký* (not *vědět*), *dav* (not *dát*), *králík* (not
  *král*), *topit* (not *utopit*). Teach the real word in its own sense, writing the headword in `newWords` as the list
  does, and never teach a headword through the other word it holds (*král* through *královna*).
- Pronouns: *ty* ("you", rank 36) is an entry with *tebe*, *tě*, *tobě*, *tebou*; *ty* and *ti*
  are also forms of *ten* (rank 5), so they count for both, but a lesson teaching *ty* needs uses
  that are its own (*tebe*, *tě*, a *ty* that means "you"). *tu* ("here", 176) is an entry of its own
  and also a form of *ten*. *vám*, *vámi* are *vy*; *tvůj* is an entry (432). *se*, *si*, *sobě*,
  *sebou* are *sebe*; *mi*, *mě*, *mně*, *mnou* are *já*; *ho*, *mu*, *jeho*, *něj* are *on*; *ji*,
  *jí*, *ní* are *ona*; *je* ("them") counts for *být*; *jejich* and *její* are entries of their
  own (108, 109).
- A few entries are names or noise: *čína* (389) is the country, *zeman* (806) a surname; the list
  also has colloquial spellings among its forms (*bejt*, *dobrej*, *von*, *eště*), which a text
  never uses. Skip such band entries.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated from the English Wiktionary Czech dictionary (`dictionaries/cs-en.sqlite`) and
  wordfreq: dictionary forms the list lacks (*panem*, *stojíš*), negated verb forms (*nepije*,
  *nepotřebuju*, *nedostala*), past forms in every gender (*nemyslela*), comparatives in every
  ending. Add a line there for another true form you need rather than writing around it, and say
  so in your report.
- The checker compares whole words, lowercased, letter for letter, and splits a word at hyphens
  (*je-li* is *je* + *li*, an entry of its own; *e-mail* is *e* + *mail*, and *e* is not in the
  list, so write *email* (3453) or *zpráva*). `Otázky:` counts as the word
  *otázky* (*otázka*, rank 267).
- A form shared by two words counts for both (*mysli* for *myslet* and *mysl*, *jedu* for *jet* and
  *jed*), or only for the one the list files it under: use a new word in its own sense, never
  through its homograph, and count a new word's uses in forms that are its own.
- **Names** are not in the list: people, places (*Praha*, *Brno*, *Česko*, *Morava*), shops and
  brands go in `names`. The checker does not decline them, so list every form the text uses:
  *Jana*, *Jany*, *Janě*, *Janu*, *Janou*, *Jano*; *Praha*, *Prahy*, *Praze*, *Prahu*, *Prahou*;
  *Petr*, *Petra*, *Petrovi*, *Petrem*, *Petře*; and the possessive adjectives the text makes from
  them (*Janin*, *Janina*, *Petrův*, *Petrova*). The checker counts a name of several words word
  by word: list each part that is not a list word (*Karlovy*; *Budějovice*; *Hradec*),
  and know that a part which is a list form counts at its own rank (*České* is *český* 49,
  *Králové* is *král* 297, *Vary* and *Varech* are *var* 1909). Keep only true names in `names`, never common words, and never a
  name that is also a word (*Růžena* is fine, but not the surnames *Hora*, *Liška*, *Bílá*,
  *Černý*, *Svoboda*, which are list words): then call the character something else. *Čína* is a list entry of its
  own (389): put it in `names` all the same and skip it as a band word. Day and month names are in
  the list (*pátek* 671, *sobota* 665, *neděle* 729, *pondělí* 1002, *středa* 1157, *úterý* 1251;
  *září* 436, *listopad* 572, *březen* 597, *leden* 673: check their ranks). *Vánoce*,
  *Velikonoce* and *Mikuláš* are not in the list: they go in `names` in every form used
  (*Vánoce*, *Vánoc*, *Vánocům*, *Vánocích*). Words missing from the list entirely include *tamten*,
  *nashledanou* and *snídat*.
- The list's form lists are noisy (old and colloquial spellings, spellings without Czech letters,
  unrelated words); a new word must appear as itself or a true inflection, never through such a
  form. Some entries are names, abbreviations or English words (*love*): skip them. Loanwords that
  Czech uses as its own (*počítač*, *film*, *web*, *email*, *víkend*, *super*, *fajn*) are taught
  like any word.
