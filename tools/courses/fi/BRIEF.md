# Writing a Finnish frequency course

Each course teaches one band of 100 words from the Finnish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `fi-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Eino haluaa Iidan kirjan. Iida ei anna sitä. Hän antaa Einolle toisen
  kirjan...* Everyday life in Finland: Helsinki, Espoo, Vantaa, Tampere, Turku, Oulu, Jyväskylä,
  Kuopio, Pori, Vaasa, Rovaniemi, Porvoo, Savonlinna, Hämeenlinna, Seinäjoki, Kajaani, Lappeenranta,
  Kokkola, Naantali, Inari, Tornio; the lakes (Saimaa, Päijänne, Näsijärvi), the archipelago, the
  fells and the forests; and now and then Stockholm (*Tukholma*), Tallinn (*Tallinna*) or Berlin
  (*Berliini*), where Finns live and Finnish is spoken at home: home, family, food, shopping, work,
  school, the town, health, free time, travel, the seasons, the summer cottage (*mökki*) and the
  sauna.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *hän* is whenever it could be
  unclear (Finnish *hän* is "he" and "she" alike, so name the person more often than English would).
  Give each story its own names: the learner reads the course in one go, and two Liisas in two
  lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object: *Liisa ostaa leipää*). A sentence that says where something is or that
  someone has something starts with the place or the owner (*Pöydällä on kirja*, *Liisalla on
  auto*, *Talossa on kaksi huonetta*). The first and second person drop their pronoun where written
  Finnish does (*Menen kotiin*, *Tuletko?*), but keep it for contrast or emphasis (*Minä en tule*).
  Little dialogue; when there is some, short and simple, with *sinä* among family, friends and in
  most everyday places (Finland says *sinä* widely) and the polite *te* (+ the plural verb: *Voisitteko
  auttaa?*) only with an older stranger or in a formal office.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense, affirmative and negative (*menee*, *ei mene*, *en tiedä*);
    *olla* with the local cases for where someone is (*on kotona*, *on Tampereella*, *on töissä*)
    and with *-lla* for having (*Liisalla on auto*, *Minulla on aikaa*); the genitive of owners and
    of names (*Liisan auto*, *minun kirjani*); the partitive after numbers, in negation and for
    "some" (*kaksi lasta*, *ei ole aikaa*, *ostaa leipää*, *paljon ihmisiä*); the six local cases in
    their plain senses (*talossa*, *talosta*, *taloon*; *pöydällä*, *pöydältä*, *pöydälle*); yes/no
    questions with *-ko*/*-kö* (*Tuletko?*, *Onko Liisa kotona?*) and the question words (*mikä*,
    *mitä*, *missä*, *mistä*, *mihin*, *kuka*, *miksi*, *miten*, *milloin*, *paljonko*); the simple
    past (*imperfekti*: *meni*, *tuli*, *oli*, *sanoi*, *teki*, *söi*) from the first courses, in
    short sentences; numbers as digits;
  - ranks 301–600: the possessive suffixes with or without the genitive pronoun (*kotini*, *minun
    kotini*, *hänen äitinsä*, *Liisa ja hänen koiransa*); the imperative (*tule*, *mene*, *ota*,
    *istu*; *älä mene*, *tulkaa*); the total and partial object (*Liisa ostaa kirjan* / *Liisa lukee
    kirjaa*); the plural cases (*taloissa*, *kauppoihin*, *lapsille*); comparatives (*isompi kuin*,
    *parempi*, *enemmän*); the common postpositions (*kanssa*, *jälkeen*, *ennen*, *luona*, *luo*,
    *takana*, *vieressä*); *pitää* and *täytyy* + infinitive for "must" (*Minun pitää lähteä*),
    *voida*, *haluta*, *osata* + infinitive;
  - ranks 601–1000: the perfect and pluperfect (*on tehnyt*, *oli tehnyt*); the conditional
    (*haluaisin*, *voisitko*, *olisi hyvä*); the present passive for "people do, one does" (*Täällä
    puhutaan suomea*, *Mökille mennään autolla*); the relative pronoun *joka* in its cases (*talo,
    jossa asumme*; *mies, jonka näin*); the essive and translative (*töissä opettajana*, *tulla
    opettajaksi*); the clitics *-kin* and *-kaan*/*-kään* (*minäkin*, *ei sekään*), *-han*/*-hän*
    lightly in dialogue (*Sinähän tulet?*); reported speech with *että* (*Liisa sanoo, että hän
    tulee*);
  - ranks 1001–1300: superlatives (*suurin*, *paras*, *eniten*); the past passive (*talo
    rakennettiin 1950*); the third infinitive (*menee syömään*, *on syömässä*, *tulee syömästä*);
    time clauses (*kun*, *sitten kun*, *ennen kuin*, *kunnes*); *-kin* and *-kaan* freely;
  - ranks 1301–1600: participles as adjectives (*lukeva tyttö*, *luettu kirja*, *tuleva kesä*) and
    the agent participle (*äidin tekemä kakku*); the second infinitive (*kotiin tullessa*, *hänen
    tullessaan*); the indefinites with negation (*kukaan ei tullut*, *ei mitään*, *ei koskaan*,
    *missään*);
  - ranks 1601–2000: the participial clauses for *että* clauses (*hän sanoi tulevansa*, *näin
    hänen lähtevän*), the temporal construction (*tultuaan kotiin*), the final construction
    (*tehdäkseen*, *ehtiäkseen*), the conditional perfect (*olisin tullut*), indirect questions (*en
    tiedä, tuleeko hän*; *kysyi, missä asema on*);
  - ranks 2001–2500: the rarer cases in their set uses (abessive *rahatta*, *syyttä*; instructive
    *jalan*, *omin silmin*, *kaksin*; comitative *perheineen*), the passive of all tenses, longer
    sentences joined with *vaikka*, *jotta*, *koska*, *mikäli*, *sen sijaan että*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *kuitenkin*, *siksi*,
    *näin ollen*, *toisin sanoen*, *sitä vastoin*, *lisäksi*; the potential mood only where it is
    idiomatic (*lienee*, *mennee*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*kolumni*, a light reflection on everyday life),
    dialogue that marks *sinä* and *te*, and less common connectors (*joskin*, *niin että*, *siitä
    huolimatta*, *kaiken kaikkiaan*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style (*-minen*, *-us* nouns) used
    with care; long sentences that stay easy to parse; tone and register chosen to suit the text
    type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, regional and learned words explained by
    their context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*koska*, *kun*, *vaikka*, *jotta*, *mutta*,
  *joten*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter to a newspaper, a short report), as long as it is
  everyday, calm and easy to follow. Still one clear idea at a time: a learner at the level should
  follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Eero tulee aina ensin. Sitten tulee Sanna, ja sitten tulevat lapset.*).
- **Questions at the end**: after the story, a paragraph starting `Kysymykset:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *miksi* and *miten*, with fuller answers):

  ```
  Kysymykset:
  Mistä Liisa ja Sami ovat?
  He ovat Oulusta.
  Onko talo iso?
  Ei, talo ei ole iso, mutta se on hyvä.
  ```

  The header `Kysymykset:` is layout: the checker does not count it as a word.
- **Standard written Finnish** (*yleiskieli*), as a careful native writer writes it today, in
  narration and in dialogue alike: no spoken forms (*puhekieli*). Write *minä*, *sinä*, *minun*,
  *sinun*, *tuo*, *nuo*, *olen*, *olet*, *me olemme*, *me menemme*, *en ole*, *onko*, *täällä*,
  *mennä*, never *mä*, *sä*, *mun*, *sun*, *toi*, *noi*, *oon*, *oot*, *me ollaan*, *me mennään*,
  *en oo*, *onks*, *tääl*, *mennää*, *kattoo*, *tota*, *joo*, *okei*. A passive for "let's" stands
  alone in dialogue only (*Mennään!*, *Syödään nyt.*), never with *me*. No slang and no vulgar words
  (the list, counted from film subtitles and the web, has many: skip them). Every *ä* and *ö* is
  written: the checker compares letters exactly, and the vowels and their length tell words apart
  (*tuli* "came, fire" / *tuuli* "wind"; *tapan* / *tapaan*; *kuka* / *kukka*; *sika* / *siika*;
  *mutta* "but" / *muta*; *jää* "ice, stays"). Days, months, languages and nationalities are written
  in lower case (*maanantai*, *toukokuu*, *suomi*, *suomalainen*); names of places and people with a
  capital (*Tampere*, *Liisa*).
- **Compounds are one word** in Finnish (*kirjakauppa*, *linja-auto*, *aamupala*, *kesämökki*).
  A compound counts as itself: at its own rank when the list has it as an entry, and as an outside
  word when it does not, even if both its parts are list words (the checker never splits a
  compound, nor a word with a hyphen). So use compounds the list has (`rank.py` tells), and
  otherwise write the idea with two list words where Finnish allows it (*Liisan auto*, not
  *perheauto*), or keep the compound as one of the lesson's few outside words.
- **Finnish typography**: direct speech in the Finnish quotation marks ”…” (the same closing mark at
  both ends), each speaker's line a paragraph of its own; the narrator's comma goes after the
  closing mark, a question or exclamation mark of the quoted sentence inside it:

  ```
  ”Pihla on hyvä lapsi”, Liisa sanoo.
  ”Mikä tuo on?” Eino sanoo. ”Saanko minä sen?”
  ```

  The questions and answers of the final `Kysymykset:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ). Write numbers as digits (*3 lasta*, *kello 7*,
  *kello 7.30*, *vuonna 1992*), ordinals with a full stop (*1. päivä*, *5. luokka*), and an ending
  joined to a number or an abbreviation with a colon (*5:een*, *tv:ssä*, *EU:n*: the checker reads
  these as the number or the abbreviation), except a number word that is in your band: spell that
  one out to teach it (*kaksi lasta*).
- **Names are names**: place names, adjectives made from place names (*suomalainen*,
  *ruotsalainen*, *helsinkiläinen*, *amerikkalainen*) and words for peoples or languages (*suomi*,
  *ruotsi*, *saame*) count as names: skip them as band words even when the list has them as
  entries; a text may still use one (*puhuu suomea*), counted at its rank. Never use a list word as
  a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, church and chapel, priests, prayer, heaven and hell, angels and
  devils, souls and spirits, and every custom or holiday of religious origin, even as a date, a
  school break or a gift season: Christmas (*joulu*, *pikkujoulut*, *joulupukki*), Easter
  (*pääsiäinen*, *virpominen*), Midsummer (*juhannus*, St John's day, with its bonfires), Shrovetide
  (*laskiainen*), All Saints' (*pyhäinpäivä*), confirmation camp (*rippikoulu*) and name days
  (*nimipäivä*, from the saints' calendar). Folk customs with no church holiday named are fine: a
  birthday, New Year's Eve as a family evening, a summer evening at the cottage, the sauna, berry
  picking, the first day of school, a market (*tori*, *markkinat*), a food or music festival. Never
  visit a church, and use *usko* and *uskoa* only as believing a person or a fact;
- **politics**: the state, the government (*hallitus*, *eduskunta*), parliament, elections, parties
  (*kokoomus*), ministers, presidents, kings and rulers, nationalism, independence and Independence
  Day (*itsenäisyyspäivä*), May Day (*vappu*, a workers' and a saint's day), flags, language
  politics, and borders as disputes; everyday civic life (a library, a school, the municipality's
  office, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts**, military service (*armeija*,
  *varusmies*) included, and hunting (a lost-property office or a crossing guard at a school is
  fine; theft, arrests, prisons and trials are not); the lake and the sea stay mild (a storm keeps
  the boats ashore; nobody falls through the ice or is lost);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (beer, wine, spirits, *lonkero*, *sima*, *kalja*, a sauna beer, a bar
  visit for a drink), **gambling** (*Veikkaus*, lotteries, betting, slot machines), drugs and
  smoking (*tupakka*, *nuuska*);
- **deliberate misspellings**: no spoken forms, no spellings without *ä* and *ö*, not even in a
  child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Finnish first names are fine as characters.

The themes keep to this: the history courses (*Käsin kosketeltavaa historiaa*, *Arkeologia ja
menneisyys*, *Tutkimusmatkailijat ja matkaajat*) tell the history of everyday life, towns, crafts,
trade, science and travel, never of wars, rulers or politics; *Kansanjuhlat ja markkinat* and
*Kansantaide ja tavat* are folk and family celebrations with no church holiday or saint named (no
*juhannus*, no *vappu*); *Julkiset palvelut* is the library, the health centre and the
municipality's counters, never politics; *Suomen kieli* and *Sanat ja niiden alkuperä* are about
words, dialects and learning, never about language politics; *Sadut ja tarinat* tells folk tales
(clever animals, giants, the forest's creatures) without gods, spirits or magic of the *Kalevala*'s
kind.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *jumala* 611, *kirkko* 570, *helvetti* 971, *henki* 580 (spirit; also "person" in
  set phrases: skip it), *usko* 489 (the noun is mostly faith; *uskoa* 190 "to believe" is fine);
- politics: *hallitus* 186, *valtio* 260, *puolue* 355, *presidentti* 498, *poliittinen* 509,
  *kansa* 524, *politiikka* 651, *eduskunta* 661, *äänestää* 614, *kokoomus* 775, *valta* 806,
  *kansalainen* 832, *kansanedustaja* 840, *pääministeri* 844, *poliitikko* 876, *vihreä* 508 (the
  colour is fine; its band forms are mostly the party), *laki* 430, *liitto* 766, *edustaja* 771,
  *järjestö* 874, *hallita* 891;
- war, crime, police, courts: *poliisi* 178, *sota* 404, *tappaa* 545, *uhri* 658, *armeija* 671,
  *syyttää* 702, *ampua* 714, *ase* 730, *lyödä* 767, *taistelu* 772, *rikos* 833, *tuomita* 901,
  *uhka* 923, *iskeä* 952, *isku* 953, *väkivalta* 964, *sotilas* 977, *taistella* 983, *tuhota*
  911, *pelastaa* 829;
- death: *kuolla* 320, *kuollut* 512, *kuolema* 713;
- vulgar and sexual: *vittu* 251, *paska* 451, *seksi* 783, *helvetti* 971;
- names, place adjectives, peoples and languages: *suomi* 21, *suomalainen* 92, *venäjä* 174,
  *ruotsi* 191, *saksa* 377, *kiina* 560, *englanti* 620, *ranska* 677, *venäläinen* 710, *viro*
  871, *norja* 894, *lappi* 898, *ruotsalainen* 938, *amerikkalainen* 954, *amerikka* 981, *lahti*
  577 (a bay, but its forms are mostly the town), *matti* 812, *pekka* 827, *mari* 892;
- spoken forms, list noise and forms of other words: *mä* 139, *toi* 333 (spoken "that"; also
  *tuoda*'s past), *joo* 375, *sä* 405, *okei* 585, *sun* 640, *ai* 917, *no* 143 (a filler; fine
  in dialogue, not worth a lesson), *oy* 655, *tv* 686 (an abbreviation; use it freely), *jne.* 991,
  *tasa* 518 (only in *tasan*, *tasoissa*: teach *tasan* "exactly" only if a story makes it plain),
  *yleisradio* 882 (a name), *live* 966 (English), *musta* 396 only as the colour "black" (its count
  holds the spoken *musta* "from me, I think"), *palaa* 612 only as "burns" (*palata* 605 is
  "to return").

Further on, the same categories hold; for example *joulu*, *juhannus*, *pääsiäinen*, *vappu*,
*kirkko*, *pappi*, *rukous*, *enkeli*, *olut*, *viini*, *viina*, *tupakka*, *humala*, *vankila*,
*varas*, *murha*, *hautajaiset*, *hauta*, *lippu* only as a ticket (never a flag), *puolue*,
*vaalit*, *sotilas*, *metsästää*.

## The file

Write `tools/courses/fi/<id>.json`:

```json
{
  "id": "fi-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Finnish, e.g. Ruokaa ja kauppaan",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Finnish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/fi/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *menee* or *menimme* for *mennä*, *lasta* or *lapsille* for
  *lapsi*, *kotiinsakin* for *kotona*). Reuse earlier lessons' new words too. `newWords` holds the
  word as the list writes it: the first infinitive of a verb (*mennä*, *tehdä*), the nominative
  singular of a noun and an adjective (*lapsi*, *uusi*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, a place adjective, a spoken form, a word of the content rules); the
  checker's last line lists the band words not taught yet.

Things to know about the Finnish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/fi/wordlist.tsv`
  (a copy of the app's `fi.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  web text, film subtitles, books and news (wordfreq), so talk and news words rank high and some
  everyday written words rank low; war, crime, politics and swearing words rank high and are
  skipped.
- **How the checker reads a Finnish word.** A word the list (with `extra_forms.tsv`) has is read
  as it is. A word it lacks is read without its clitics (*-kin*, *-kaan*/*-kään*, *-han*/*-hän*,
  *-pa*/*-pä*, *-ko*/*-kö* and *-kos*, *-pas*, *-hans*; at most two: *onkohan* is *on*) and then
  without its possessive suffix (*-ni*, *-si*, *-nsa*/*-nsä*, *-mme*, *-nne*, and the third person's
  long vowel + *n*: *talossaan*), putting back the *-n* or *-t* the suffix took (*kotiini* is
  *kotiin*, *taloni* is *talo*, *talon* or *talot*) and the translative's *-ksi* (*talokseni*):
  *kotiinsakin* counts as *kotiin*, *veljensäkin* as *veljensä*, *tuletko* as *tulet*. A name in
  `names` counts in all its case forms, with a possessive suffix or clitics too, even with up to
  two letters of its stem changed by the ending (*Liisan*, *Liisalle*, *Tampereella*,
  *Helsingissä*, *Turussa*, *Mikolla*, *Porvooseen*); a foreign name takes its ending after an
  apostrophe when Finnish writes one (*Joe'n*). A name the checker still misses (a long stem
  change) can go into `names` in that form too. A name of several words or with a hyphen counts as
  one word wherever it is written whole; its case forms count by its parts.
- `extra_forms.tsv` holds every standard form of the list's words that the list lacks (the list
  keeps at most 40 forms per word): the cases in both numbers, the verb forms, and the declension
  of the participles, verbal nouns and comparatives the list files under their word (*tehtävän* of
  *tehdä*, *paremman* of *hyvä*), from the English Wiktionary's Finnish data
  (`dictionaries/kaikki.org-dictionary-Finnish.jsonl.gz`), without the forms Wiktionary marks as
  colloquial, dialectal, rare or archaic. It is made by
  `/Users/zavanton/TabbyProjects/tayra-writers/fi-writers/make_extra_forms.py`. If a true form is
  missing, write around it and name the word and form in your report.
- **Pronouns and little words are entries of their own**: *minä* 38, *sinä* 89, *hän* 8, *me* 41,
  *te* 238, *he* 46 (each with its cases: *minulla*, *sinulle*, *hänet*, *meidän*, *heitä*); *se*
  3 holds *ne* and their cases; *tämä* 6 holds *nämä*, *täällä*, *tänne*, *täältä*; *tuo* 33 holds
  *nuo*, *tuolla*; *mikä* 10 holds *mitä*, *missä*, *mistä*, *mihin*; *kuka* 166 is apart from
  *mikä*; *itse* 78 ("self, oneself") and *itsensä* 131 (*itselleen*, *itseään*, *itsellesi*) are
  two entries; *ei* 4 holds the negative verb (*en*, *et*, *emme*, *älä*); *olla* 1 holds *on*,
  *ovat*, *oli*, *ollut*, *onko*. Some adverbs and postpositions are entries apart from the noun
  they come from: *kotona* 283 holds *kotiin* and *kotoa* (*koti* 563 holds *kodin*, *kodissa*,
  *koti*), *vuonna* 104 is apart from *vuosi* 27, *aamulla* 838 apart from *aamu* 896, *mukaan* 56
  apart from *mukana* 189, *edessä* 637 apart from *eteen* 907, *sisällä* 346, *ulos* 369 (holds
  *ulkona*), *alla* 561, *päälle* 216, *takana* 540, *kesken* 306, *yhteydessä* 581.
- **Words filed under a commoner word spelled the same**, which the checker counts at that word's
  rank, so they look known early; they are never band words, and below rank 1000 use them only
  where the story makes their meaning plain: *tuli* "fire" (under *tulla*), *voi* "butter"
  (*voida*), *tee* "tea" 7842 (its *tee*, *teetä*, *teen* count for *tehdä* too), *kuusi* "spruce"
  (*kuusi* "six"), *pari*, *kerta*. A verb's participles, verbal noun and comparatives are filed
  under it: *tehtävä* "task" and *tekeminen* under *tehdä*, *tuleva* "coming, future" under *tulla*,
  *vanhemmat* "parents" under *vanha*, *enemmän* under *paljon*; they are no band words of their
  own.
- **Homographs and shared spellings**: some entries hold two words (*kuusi* six and spruce, *tuo*
  "that" and "brings", *jää* "ice" and "stays", *palaa* "burns" with *pala* "piece" apart, *lähde*
  "spring, source" and *lähde* "leave!", *tuki* "support" and *tuki* "supported", *muisti* "memory"
  and "remembered"), and a spelling shared by two entries counts for both (*sataa* is *sata*'s
  partitive and *sataa* "it rains"; *ala* "field" and *ala* "don't begin"; *kerros* "floor"). Use a
  new word in its own sense, never through its homograph, and count a new word's uses in forms that
  are its own (*sataa* "rains" through *satoi*, *sataa*, *satanut*; *muisti* through *muistiin*,
  *muistissa*).
- **Names** are not in the list: people, places (*Tampere*, *Oulu*, *Kuopio*), shops and brands go
  in `names`. Keep only true names in `names`, never common words, and never a name that is also a
  list word: as characters avoid *Anna* (*antaa*'s imperative), *Mikko*, *Matti*, *Pekka*, *Jussi*,
  *Kari*, *Jukka*, *Kalle*, *Mari*, *Maria*, *Aino*, *Helmi*, *Onni*, *Toivo*, *Satu*, *Tuuli*,
  *Ilona*, *Lumi*, *Lahja*, *Veikko*, *Oiva*, *Sara*, *Tuomas*, *Kaisa*, *Urho*, *Lassi*; as places
  *Lahti*, *Joensuu*, *Kotka*, *Kemi*, *Hanko*, *Rauma*, *Mikkeli*, *Koli*, *Ruka*, *Lappi*,
  *Lontoo*, *Norja* and *Kauppatori* are list words too: they count at their own rank, so leave
  them out of `names` and use them only as outside words, or pick another. Check a name's rank with
  `rank.py` before you use it. Safe in the pilot: *Liisa*, *Sami*, *Pihla*, *Eero*, *Sanna*,
  *Eetu*, *Ella*, *Eino*, *Iida*, *Juha*, *Leena*, *Veeti*, *Venla*, *Pirjo*, *Siiri*, *Antti*,
  *Tiina*, *Elina*, *Jari*, *Minna*, *Aatu*, *Lauri*, *Laura*, *Ville*, *Raili*, *Emma*, *Petri*,
  *Hanna*; *Oulu*, *Tampere*, *Kuopio*, *Helsinki*, *Esplanadi*, *Porvoo*, *Rovaniemi*, *Tornio*,
  *Haaparanta*, *Jyväskylä*. Day and month names are in the list (*lauantai* 679, *syyskuu* 726,
  *elokuu* 824, *lokakuu* 866, *marraskuu* 987, then *perjantai*, *maanantai*, *kesäkuu*,
  *tammikuu*, *sunnuntai*, *tiistai*, *keskiviikko*, *torstai* after rank 1000); the seasons too
  (*kesä* 340, *kevät* 503, *syksy* 523, *talvi* 1145).
- Everyday words rank later than you would think: *talo* 339, *koti* 563 (but *kotona*, *kotiin*
  283), *äiti* 253, *isä* 338, *perhe* 313, *poika* 182, *tyttö* 244, *ystävä* 475, *kaveri* 325,
  *koira* 416, *kissa* 1065, *koulu* 228, *ruoka* 356, *vesi* 329, *kahvi* 959, *leipä* 1675, *auto*
  124, *kirja* 215, *iso* 125, *pieni* 106, *kiitos* 122, *anteeksi* 1282, *hei* 496, *moi* 2671;
  the dialogue tag *sanoa* 47 is in the first band, *kysyä* 281 and *vastata* 212 come later.
- The list's form lists are noisy (spoken spellings, look-alikes); a new word must appear as itself
  or a true inflection, never through such a form. Some entries are names, English words or
  letters: skip them. Loanwords Finnish uses as its own (*video*, *netti*, *internet*, *media*) are
  taught like any word.
