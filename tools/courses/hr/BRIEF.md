# Writing a Croatian frequency course

Each course teaches one band of 100 words from the Croatian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `hr-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## The language of the courses

Standard Croatian, **ijekavian**, in the Latin script, as written in Croatia today: *mlijeko*,
*lijepo*, *dijete*, *rijeka*, *vrijeme*, *vidjeti*, *htjeti*, *djeca*, *gdje*, *ovdje*, *prije*,
*poslije* — never the ekavian *mleko*, *lepo*, *dete*, *reka*, *vreme*, *videti*, *deca*, *gde*,
*ovde*, *pre*, *posle* (Serbian), though the list holds those spellings among the forms of its
words. Write the letters **č ć ž š đ** and the digraphs **lj nj dž** properly, never *c*, *z*, *s*
or *dj* for them, and never any Cyrillic.

Croatian, not Serbian, words and forms: *što* (never *šta*), *tko* (never *ko*), *tisuća* (not
*hiljada*), *vlak* (*voz*), *kruh* (*hleb*), *kava* (*kafa*), *tjedan* (*nedelja*, *sedmica*),
*obitelj* (*porodica*), *kazalište* (*pozorište*), *povijest* (*istorija*), *znanost* (*nauka*),
*zrak* (*vazduh*), *ljekarna* (*apoteka*), *kino* (*bioskop*), *hlače* (*pantalone*), *sveučilište*
(*univerzitet*), *promet* (*saobraćaj*), *točno*, *točka* (*tačno*, *tačka*), *otok* (*ostrvo*),
*žlica*, *vilica*, *boca*, *juha*, *grah*, *krumpir*, *rajčica*, *plaća*, *računalo*, *tvornica*,
*popis*, *također*, *uopće*, *sretan*, *ujutro*, *susjed*, *nogomet*, *dogoditi se* and *događati
se* (not *desiti*, *dešavati*), *napraviti* or *učiniti* (not *uraditi*), *oprosti* / *ispričavam
se* (not *izvini*). The months are Croatian: *siječanj, veljača, ožujak, travanj, svibanj, lipanj,
srpanj, kolovoz, rujan, listopad, studeni, prosinac* (*5. svibnja*), never *januar*, *maj*,
*jun*. Loan verbs end in *-irati*: *organizirati*, *informirati*, *telefonirati*.

Croatian grammar where it differs from Serbian:

- **The infinitive** after a verb: *želim ići*, *moram raditi*, *mogu doći*, *volim čitati*,
  *počinje padati kiša* — not *da* + present (*želim da idem*), which is Serbian.
- **The future is written apart**: the infinitive (losing its *-i* before the clitic) and *ću*,
  *ćeš*, *će*, *ćemo*, *ćete*, *će*: *radit ću*, *vidjet ćemo*, *pisat će*; a verb in *-ći* keeps it
  whole: *doći ću*, *ići ćeš*. With the clitic first the infinitive is whole: *Sutra ću raditi.*
  *Ja ću ići.* Never *radiću*, *videćemo* (Serbian spelling: the checker fails them).
- **Questions**: *Je li Ana kod kuće?*, *Imaš li vremena?*, *Dolaziš li?*, *Gdje je stanica?* —
  never *Da li...?*.
- **The clitics** stand in second place in their fixed order: *li*, then the auxiliary (*sam,
  si, je* — but *je* last), then dative, accusative, *se*: *Dao sam mu ga.*, *Ana mu ga je dala.*,
  *Vidio sam je.*, *Bojim ga se.* Negation is a word of its own, written apart: *ne znam*, *ne
  može*, *ne radi* — except *nisam*, *nije* (of *biti*), *neću*, *neće* (of *htjeti*), *nemam*,
  *nema* (of *nemati*) and *nemoj*, written as one.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Mia traži svoj mobitel. Ide u kuhinju. Nema ga. Ide u sobu...* Everyday life
  in Croatia — Zagreb, Split, Osijek, Zadar, Pula, Varaždin, Dubrovnik, Šibenik, Karlovac, Sisak,
  Čakovec, the islands (Hvar, Brač, Krk, Korčula), Istria, Slavonia, Zagorje, Lika, Gorski kotar,
  the Plitvice lakes — and now and then Croats abroad (Beč, München, Toronto): home, family, food,
  shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could
  be unclear. Give each story its own names: the learner reads the course in one go, and two
  Ivans in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Croatian drops subject pronouns: write *ja*, *ti*, *on*, *ona* only
  where Croatian would (contrast, emphasis, a new subject). Little dialogue; when there is some,
  short and simple, with *ti* among family and friends and *Vi* (capital in address is not
  needed in dialogue: *vi*) with strangers, older people and in shops or offices (*Izvolite.*,
  *gospodin Horvat*, *gospođa Kovač*).
- **Cases** cannot be avoided: use the frequent ones in their simple, regular patterns (*u školu*,
  *u školi*, *s mamom*, *kod bake*, *Mia ima brata*, *nema ključa*, *ide kući*, *daje mami cvijet*),
  and let each band add the patterns it needs.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense, *biti* (*je*, *sam*, *nije*), *imati* / *nemati* (*ima*, *nema*
    "there is / isn't"), *htjeti*, *moći*, *morati*, *trebati*, *željeti*, *voljeti* + infinitive,
    the future with *ću* (*ići ću*, *radit ćemo*), the short pronouns in second place (*vidi ga*,
    *daje joj*), *se* verbs (*zove se*, *vraća se*), simple questions (*tko*, *što*, *gdje*, *kako*,
    *zašto*, *kad*, *je li*, *... li*); the **perfekt** for what happened, in short sentences (*Ana je
    bila u Splitu.*, *Došao sam.*, *Što si rekao?*);
  - ranks 301–600: the perfekt freely beside the present (*Jučer je Ivan kupio kruh.*), the
    imperative, positive and negative (*dođi*, *uzmi*, *sjedni*, *nemoj se brinuti*), possessives
    in every case (*s mojim bratom*, *u našoj kući*), comparatives (*veći od*, *bolji*, *lakše*),
    *jer*, *kad*, *ako*, *dok*;
  - ranks 601–1000: **aspect** contrasted (*čitao je knjigu kad je zazvonio telefon*; *svaki dan
    dolazi* / *sutra će doći*), verbs of motion with prefixes (*doći*, *otići*, *izaći*, *ući*,
    *prijeći*), the relatives *koji*, *što*, *gdje*, reported speech with *da* (*kaže da će doći*),
    numbers with nouns (*dva brata*, *pet kuna* → *pet eura*);
  - ranks 1001–1300: comparatives and superlatives in full (*najbolji*, *najviše*), the
    **kondicional** for wishes and polite requests (*htio bih*, *možete li...*, *bilo bi lijepo*),
    *koji* in every case, indirect questions (*pita je li...*, *ne zna gdje...*);
  - ranks 1301–1600: *da bi* / *kako bi* of purpose, *ako* + present and *ako* + perfekt, the
    vocative (*Ana!*, *Ivane!*, *mama*, *gospodine*), the impersonal *se* (*ovdje se ne puši* — avoid
    smoking: *ovdje se ne parkira*), *trebati* in its forms;
  - ranks 1601–2000: the pluperfect only where clear (*bio je već otišao*), unreal conditions
    (*kad bih imao vremena, putovao bih*), the passive participle and the passive with *se*
    (*prodaju se stanovi*; *kuća je sagrađena 1950.*), reported speech;
  - ranks 2001–2500: past unreal conditions (*da sam znao, došao bih*), the verbal adverbs
    (*radeći*, *došavši*), relative *čiji*, *koji* with prepositions, *koliko... toliko*, collective
    numerals (*dvoje djece*, *troje ljudi*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *međutim*, *dok*, *budući
    da*, *unatoč*, *što se tiče*, *zbog toga*, *stoga*; *nastaviti*, *prestati*, *početi* +
    infinitive; the **aorist** (*reče*, *dođe*, *uzeše*) only in a short tale or an old-style
    passage; common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the newspaper column (*kolumna*, a light reflection on everyday
    life), dialogue that marks *ti* and *vi*, and less common connectors (*pod uvjetom da*, *premda*,
    *naime*, *ipak*);
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
  After rank 1000 sentences get longer and joined (*jer*, *kad*, *dok*, *iako*, *zato što*,
  *međutim*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter to a newspaper, a short report), as long as it is
  everyday, calm and easy to follow. Still one clear idea at a time: a learner at the level should
  follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Traži u kuhinji. Nema ga. Traži u sobi. Nema ga.*).
- **Questions at the end**: after the story, a paragraph starting `Pitanja:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *zašto* and *kako*, with fuller answers):

  ```
  Pitanja:
  Što traži Mia?
  Mia traži svoj mobitel.
  Gdje je mobitel?
  Mobitel je u autu.
  ```

  The header `Pitanja:` is layout: the checker does not count it as a word.
- **Standard Croatian**: neutral, correct and natural, with all its letters; no slang, no dialect
  in the narration (a Dalmatian or Zagreb word may appear in dialogue, explained by the story,
  from rank 3000). Prefer words understood everywhere in Croatia.
- **Croatian typography**: dialogue as paragraphs that begin with an em dash and a space
  (`— Gdje si?`), the narrator's remarks in the same paragraph after another dash (`— Ovdje — kaže
  Ivan.`); quoted words and titles in „low and high“ quotation marks; no space before `:`, `;`,
  `?` or `!`. Dates as *5. svibnja*, ordinals as digits with a full stop (*3. kat*), times as *u 7
  sati* or *u 7:30*. Prices in euros (*5 eura*).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, church, priests, prayer, heaven and hell, angels and
  devils, souls and ghosts, and every custom or holiday of religious origin, even as a date, a
  school break or a gift season: Christmas (*Božić*, *Badnjak*, the tree, *Sveti Nikola*), Easter
  (*Uskrs*, painted eggs), *Tijelovo*, *Velika Gospa*, *Svi sveti*, a town's patron-saint day
  (*Sveti Duje*, *Sveti Vlaho*, *Sveti Stjepan*), carnival (*poklade*, *fašnik*) and saints' name
  days; *zbogom* (farewell "with God") is avoided too. Folk customs with no church holiday or saint
  named are fine: a summer town *fešta* with music and food, *klapa* singing, a fair (*sajam*),
  the grape harvest (*berba*) as farm work, a birthday, New Year's Eve (*Stara godina*) as a family
  evening, the start of the school year, the summer holidays. Place names with *Sveti* are names,
  but prefer others; never visit a church or a monastery; use *nebo* only as the sky, *moliti*
  only as "ask, please" (*molim*, *molim vas*), *vjerovati* only as believing a person or a fact,
  *čudo* only as a wonder (*nije čudo*);
- **politics**: the state, the government, parliament, elections, parties, ministers,
  presidents, kings and rulers, the nation, independence, flags, and borders as disputes; everyday
  civic life (a library, a school, the town office counter, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office is fine;
  theft, kidnapping, arrests, prisons, trials, fights and shooting are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, curses and words whose main sense is sexual;
- **alcohol** in every form (wine, beer, *rakija*, *gemišt*, a toast, a bar visit for a drink),
  **gambling** (lotteries, betting, casinos), drugs and smoking;
- **deliberate misspellings**: no spellings without *č ć ž š đ*, no ekavian or Serbian forms, no
  colloquial spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a nationality is skipped. Languages may be taught as languages (*govori engleski*).
  Common Croatian first names are fine as characters.

The themes keep to this: the history courses (*Povijest na dohvat ruke*, *Arheologija i
prošlost*, *Istraživači i putnici*) tell the history of everyday life, towns, crafts, trade,
science and travel, never of wars, rulers or politics; *Pučke fešte i sajmovi* and *Narodna
umjetnost i običaji* are folk and family celebrations with no church holiday or saint named;
*Javne službe* is the library, the health centre and the town office's counters, never politics;
*Hrvatski jezik* and *Narječja i krajevi Hrvatske* are about words, dialects and learning, never
about language politics; *Priče i legende* tells folk tales without saints, devils or miracles.

A word whose COMMON sense breaks a rule is skipped even if a story could use another sense.
Skip band words that only fit these topics, and say so in your report. The skip list for the
first 1000 ranks:

- religion: *bog* 97, *duša* 207 (also *dušo* "darling"), *vrag* 473, *duh* 579, *crkva* 728,
  *zbogom* 828, *kleti* 837, *pakao* 886;
- politics: *kralj* 509, *država* 540, *zakon* 552, *predsjednik* 558, *narod* 755, *vlada* 785,
  *gospodar* 824, *rob* 859;
- war, crime, police, courts: *policija* 284, *kriv* 308, *rat* 338, *zatvor* 366, *ubojstvo* 368,
  *oružje* 378, *boriti* 419, *pištolj* 423, *ubojica* 427, *dokaz* 431, *pucati* 441, *žrtva* 454,
  *policajac* 465, *ukrasti* 483, *napad* 523, *borba* 573, *napasti* 583, *sud* 584, *udariti* 593,
  *vojnik* 607, *vojska* 609, *lov* 634, *zločin* 653, *detektiv* 678, *svjedok* 721, *metak* 724,
  *uhititi* 756, *bomba* 762, *general* 809, *istraga* 839, *poručnik* 868, *sudac* 879, *pukovnik*
  923, *šerif* 929, *narednik* 969, *oteti* 970;
- death: *ubiti* 92, *mrtav* 186, *umrijeti* 187, *smrt* 294, *krv* 361, *umirati* 639;
- vulgar, curses and insults: *sranje* 223, *jebati* 262, *dovraga* 537, *prokleti* 608, *seks*
  693, *idiot* 738, *kurac* 764, *jebeno* 796, *kvragu* 932, *guzica* 964, *gad* 975;
- alcohol, drugs, gambling: *droga* 647, *kladiti* 683, *pivo* 790, *vino* 983; *bar* 377 only as
  "at least" (*bar jednom*), never a bar;
- Serbian words: *uraditi* 206, *desiti* 531, *dešavati* 941 (write *napraviti*, *dogoditi*,
  *događati*), *nazad* 865 (write *natrag*), *kola* 848 (write *auto*);
- nationalities: *američki* 769 (*engleski* 697 and *francuski* 860 may be taught as languages);
- forms and variants of other words: *njezin* 416 (teach *njen* 257, its equal, and write *njen*
  throughout), *mlada* 681 (only as "bride", or skip as a form of *mlad*), *ha* 458.

Further on, the same categories hold; for example *Božić*, *anđeo*, *svećenik*, *molitva*,
*sveti*, *grob*, *pogreb*, *metak*, *ranjen*, *lopov*, *krađa*, *sudski*, *vojni*, *rakija*,
*cigareta*, *pušiti*, *lutrija*, *kockati*, *ministar*, *izbori*, *stranka*.

## The file

Write `tools/courses/hr/<id>.json`:

```json
{
  "id": "hr-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Croatian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Croatian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/hr/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *išao* or *idem* for *ići*, *bolji* for *dobar*, *ga* for
  *on*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the
  infinitive of a verb (*vidjeti*, *nemati*, *stignuti*), the nominative singular of a noun, the
  masculine singular of an adjective (*star*, *velik*, *nov*, *cijeli*).
- **Ijekavian and Croatian**: the checker fails a lesson that uses an ekavian spelling or a Serbian
  word (`tools/courses/hr/serbian_forms.tsv`, made from the dictionary pack: *videla*, *mleko*,
  *gde*, *šta*, *hiljadu*, *kafu*...) and a future written joined (*radiću*), and names the
  Croatian word to write.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word, a form or variant of another word, a Serbian word, a word of the content rules);
  the checker's last line lists the band words not taught yet.

Things to know about the Croatian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/hr/wordlist.tsv`
  (a copy of the app's `hr.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  subtitles, so everyday spoken words rank high and written ones (*siječanj*, *ljekarna*) late.
- The checker compares whole words, lowercased, split at hyphens. **The clitics are words of
  their own**, as written: *je*, *sam*, *si*, *smo*, *ste*, *su*, *bi*, *bih* and *nije*, *nisam*
  are forms of *biti* 1; *ću*, *ćeš*, *će*, *ćemo*, *ćete* and *neću*, *neće*, *hoću* of *htjeti* 8;
  *nemam*, *nema* of *nemati* 58 (its own word, not *imati*'s); *nemoj* 163 is its own word.
- **Pronouns hold their case forms**: *me*, *mene*, *meni*, *mnom* are *ja* 11; *te*, *tebe*,
  *tebi*, *tobom* *ti* 9; *ga*, *mu*, *njega*, *njemu*, *njim* *on* 14; *je* (her), *ju*, *nju*,
  *joj*, *njom* *ona* 42; *nas*, *nam*, *nama* *mi* 12; *vas*, *vam*, *vama* *vi* 22; *ih*, *im*,
  *njih*, *njima* *oni* 34; *se*, *sebi*, *sobom* are *se* 5 and *sebe* 162; *koga*, *kome*, *kim*
  are *tko* 57; *čega*, *čemu*, *čime* *što* 10. A form shared by two words counts for both
  (*je* "is" and "her"; *te* "you" and "and"; *sam* "am" and "alone"): use a new word in its own
  sense, never through its homograph.
- **Aspect pairs are two words**, each its own entry: *doći* 61 / *dolaziti* 179, *otići* 90 /
  *odlaziti* 388, *vratiti* 113 / *vraćati* 1298, *pokušati* 247 / *pokušavati* 862, *čitati* 484 /
  *pročitati* 636, *jesti* 280 / *pojesti* 644, *piti* 508 / *popiti* 638, *dati* 62 / *davati* 887,
  *početi* 184 / *počinjati* 550. Teach each
  through its own forms (*dolazi* is *dolaziti*'s, never *doći*'s; *došao* is *doći*'s).
  *stignuti* 309 holds *stići*, *stigao*, *stigne* (one verb spelled two ways).
- **Adjectives are filed under the masculine**: *star* 160 holds *stari*, *stara*, *staro*;
  *dobar* 70 holds *bolji*, *najbolji*; *velik* 104 holds *veći*; *cijeli* 174 is how the list
  writes *cio*/*cijel*. Adverbs in *-o* are words of their own (*dobro* 28, *lijepo* 234, *sigurno*
  205, *teško* 295) beside their adjectives. *nikad* 89 holds *nikada*, *ikad* holds *ikada*,
  *ponovno* holds *ponovo*, *hajde* 99 holds *hajdemo*, *vijest* holds *vijesti* "the news".
- **Words filed under a commoner word spelled alike** count at that word's rank and are never
  band words: *dana* "days" is also *dati*'s, *pita* "pie" *pitati*'s, *jelo* "dish" *jesti*'s,
  *rješenje* *riješiti*'s, *tim* "team" *taj*'s, *sto* 299 is "hundred" (write *stol* for a table).
  Below rank 1000 use them only where the story makes their meaning plain.
- Common forms the list misses are added in `extra_forms.tsv` (generated from the dictionary pack:
  each headword's inflected forms the list lacks, no ekavian or Serbian spellings), so use them
  freely; if another true form is missing, write around it and say so in your report.
- A **compound** written as several words (*u redu*, *na primjer*, *svaki dan*) counts as its parts.
- A **name** goes in `names` as written, in the nominative (*Ana*, *Ivan*, *Zagreb*, *Slavonski
  Brod*): the checker accepts its regular case forms (*Ane*, *Ivanu*, *Zagrebu*, *Splita*, *Osijeka*;
  the vowel that drops in *Petar*, *Karlovac*, *Čakovec* too). A name that is not regular may need
  its oblique form listed as well. Keep only true names there, never common words, and never a
  name that is also a list word: avoid *Luka* (*luka* "port"), *Marko* (a form of *marka*), *Maja*,
  *Iva*, *Ivo*, *Lana*, *Vid*, *Zara*, *Rijeka* (*rijeka* "river"), *Brod* alone. Safe: *Ana*, *Ivan*,
  *Petra*, *Ivana*, *Nina*, *Tomislav*, *Josip*, *Lucija*, *Petar*, *Marija*, *Hrvoje*, *Sara*,
  *Karlo*, *Dora*, *Filip*, *Tena*, *Mia*, *Ema*, *Leon*, *Borna*, *Jakov*, *Lea*, *Jana*, *Ante*,
  *Mate*; *Zagreb*, *Split*, *Osijek*, *Zadar*, *Pula*, *Varaždin*, *Dubrovnik*, *Šibenik*,
  *Karlovac*, *Sisak*, *Čakovec*, *Koprivnica*, *Bjelovar*. Check a name with `rank.py` first.
  Day, month and season names are in the list (*nedjelja* 898, *ponedjeljak* 1819, *petak* 1324,
  *subota* 1542, *ljeto* 1550, *zima* 2254, *svibanj* 2937...).
- Everyday words rank later than you would think: *tamo* 103, *prijatelj* 118, *mama* 144, *tata*
  156, *kuća* 170, *sin* 191, *majka* 192, *djeca* 193, *danas* 195, *tri* 204, *auto* 213, *grad*
  227, *soba* 238, *obitelj* 244, *dijete* 249, *brat* 254, *jesti* 280, *sutra* 287, *voda* 313,
  *sestra* 327, *škola* 332, *pas* 365, *knjiga* 389, *hrana* 424, *piti* 508, *kava* 599, *baka*
  810, *djed* 813, *mačka* 1137, *kruh* 2138; the dialogue tag *kaže* (*kazati* 76) is in the first
  band, *pita* (*pitati* 147) and *odgovara* (*odgovarati*) come later.
- The list's form lists are noisy (ekavian spellings, archaic aorists, look-alikes such as
  *zara* under *zar* or *doli* under *do*); a new word must appear as itself or a true inflection,
  never through such a form. Loanwords that Croatian uses as its own (*mobitel*, *internet*,
  *film*, *auto*, *super*) are taught like any word.
- Write numbers as digits (*7 sati*, *3 djece*), except a number word that is in your band: spell
  that one out to teach it (*sedam dana*). Times as *u 7 sati* or *u 7:30*; dates as *5. svibnja*.
