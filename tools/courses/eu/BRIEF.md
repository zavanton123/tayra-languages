# Writing a Basque frequency course

Each course teaches one band of 100 words from the Basque word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `eu-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Mikelek ez daki non dagoen bere telefonoa. Sukaldera joaten da. Ez du ikusten.
  Egongelara joaten da...* Everyday life where Basque is spoken: Bilbo, Donostia, Gasteiz, Iruñea,
  Baiona, Getxo, Zarautz, Tolosa, Durango, Eibar, Bergara, Gernika, Lekeitio, Bermeo, Ondarroa,
  Hondarribia, Irun, Oñati, Azpeitia, Elizondo, Lizarra, Maule, Donibane Lohizune; the coast and
  Urdaibai, the mountains (Gorbeia, Aralar, Aizkorri, Anboto), the rivers (Bidasoa, Nerbioi, Urola);
  and now and then Madrid, Paris, London, Boise or Buenos Aires, where Basques live: home, family,
  food, shopping, work, school, the town, health, free time, travel. Any everyday, cultural or story
  topic is fine (festivals and holidays, history, legends, sport, love, a detective story); only
  explicit sexual content, slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *hura*, *bera* is whenever it could be unclear. Give each
  story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Basque
  order (subject, object, verb: *Anek kafea hartzen du.*; the questioned or new word right before
  the verb: *Non dago Mikel? Mikel etxean dago.*). Basque drops subject pronouns: write *ni*, *zu*,
  *hura* only for contrast or emphasis. Little dialogue; when there is some, short and simple, with
  *zu* to everyone. **Never *hi* (hika)**: no *hik*, *haiz*, *duk*, *dun*, *hator*: the familiar
  forms are regional and hard; the list has some, skip them.
- **The verb**: Basque verbs are mostly a participle (*ikusi*, *etorri*, *jan*) plus an auxiliary
  (*izan* for intransitive verbs: *etorri da*; *ukan*, filed under *izan* in the list, for transitive
  ones: *ikusi du*). The list files every auxiliary form under *izan* (*da*, *naiz*, *dut*, *du*,
  *dio*, *zaio*, *dezake*, *litzateke*), and the synthetic verbs' forms under their verb (*nago*,
  *dago* of *egon*; *noa*, *doa* of *joan*; *dator* of *etorri*; *dabil* of *ibili*; *dakit* of
  *jakin*; *daukat* of *eduki*; *diot*, *dio* of both *esan* and *izan*). The imperfective (*ikusten*, *etortzen*), the future
  (*ikusiko*, *etorriko*, *joango*) and the verbal noun (*ikustea*, *ikusteko*) are forms of the verb.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *izan* and *egon* in the present (*Mikel irakaslea da*, *Ane etxean dago*, *pozik
    dago*), the article *-a*/*-ak* (*etxea*, *etxeak*), the absolutive and the ergative *-k* (*Anek
    ogia erosten du*), the dative *-ri* (*amari*), the inessive *-n*/*-an* (*etxean*, *Bilbon*), the
    allative *-ra* (*etxera*, *Bilbora*), the ablative *-tik*, the genitives *-ren* (*Anen etxea*) and
    *-ko* (*Bilboko*, *etxeko*), the comitative *-rekin*; the habitual present (*jaten du*, *joaten
    da*, *lan egiten du*) and the synthetic presents (*noa*, *dator*, *dabil*, *daki*, *dauka*); the
    near past with the present auxiliary (*gaur Ane etorri da*, *zer egin duzu?*, *ikusi dut*) for what
    happened today; *nahi dut* / *behar dut* + participle (*etxera joan nahi du*, *lan egin behar
    du*); *ez* before the auxiliary (*ez dut ikusten*, *ez da etorri*); questions with *zer*, *nor*,
    *non*, *nora*, *nondik*, *noiz*, *nola*, *zergatik*, *zenbat*;
  - ranks 301–600: the past auxiliaries (*nintzen*, *zen*, *ginen*, *nuen*, *zuen*, *genuen*:
    *atzo etorri zen*, *ikusi nuen*) for what happened before today, and the synthetic pasts (*zegoen*,
    *zihoan*, *zekien*, *zeukan*); the future (*etorriko da*, *joango gara*); the imperative (*zatoz*,
    *emadazu*, *begira*, *esan iezaiozu* only when it reads naturally: prefer *esan*, *etorri*, *eman*);
    the possessives (*nire*, *zure*, *bere*, *gure*, *zuen*); the dative auxiliary (*eman dio*, *esan
    dit*, *gustatzen zaio*); comparatives (*handiagoa*, *Mikel Ane baino altuagoa da*, *bezain*);
  - ranks 601–1000: subordinate clauses: *-(e)la* for what someone says or thinks (*esan du
    etorriko dela*, *uste dut ondo dagoela*), the relative *-(e)n* (*ikusi dudan gizona*, *bizi den
    etxea*), *-(e)nean* "when" (*etxera iristen denean*), *-(e)lako* "because", *ba-* for real
    conditions (*euria ari badu, etxean geratuko gara*); *ahal izan* and *ezin* (*joan ahal naiz?*,
    *ezin du etorri*); *-tzen ari da* for what is going on (*jaten ari da*);
  - ranks 1001–1300: the hypothetical (*etorriko balitz*, *joango nintzateke*, *gustatuko litzaidake*),
    superlatives (*handiena*, *onena*), *-tzeko* for purpose (*ikasteko etorri da*), *-(e)n bezala*,
    *-(e)n arte*, *-ta* / *-rik* for states (*nekatuta dago*, *bizirik*);
  - ranks 1301–1600: the subjunctive after wishes, orders and purpose (*etor dadin nahi dut*, *esan
    diot etor dadila*, *-(e)n* + *dezan*), the potential (*daiteke*, *dezaket*, *ezin dezake*),
    *-(e)nik* after a negation (*ez dut uste etorriko denik*), indirect questions (*ez dakit non
    dagoen*), *nahiz eta*, *-(e)n arren*;
  - ranks 1601–2000: participles as adjectives (*erositako ogia*, *Bilbon jaiotako mutila*), *-(e)larik*,
    the causative *-arazi*, the past hypothetical (*etorri izan balitz*), reported speech in the
    past (*esan zuen etorriko zela*);
  - ranks 2001–3000: a richer, more formal register: connectors such as *hala ere*, *beraz*, *izan ere*,
    *hau da*, *bestalde*, *gainera*, *ordea*, *aldiz*, *-(e)z gero*; nominalised clauses (*etortzea
    erabaki zuen*); common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives and
    clear images, the newspaper column, dialogue, less common connectors;
  - ranks 4001–6000 (C1): everything before, with more abstract and nuanced content: a short essay
    or a column that weighs two sides, an interview, a portrait, a review; the more specialised
    words of professions, sciences and the arts, each made clear by its context;
  - ranks 6001–10000 (C1–C2): figurative language and idioms used more freely, words chosen for
    their shades of meaning, texts close to what native readers meet, still standalone, calm and
    easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000
  a lesson may also be a short informative or opinion text.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Sukaldera joaten da. Ez du ikusten. Egongelara joaten da. Ez du ikusten.*).
- **Questions at the end**: after the story, a paragraph starting `Galderak:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *zergatik* and *nola*, with fuller answers):

  ```
  Galderak:
  Zer ez daki Mikelek?
  Mikelek ez daki non dagoen bere telefonoa.
  Non dago telefonoa?
  Telefonoa autoan dago.
  ```

  The header `Galderak:` is layout: the checker does not count it as a word.
- **Standard Basque**: the narration is *euskara batua*, the standard of Euskaltzaindia, neutral,
  correct and natural as written today: *zer*, *ez dut*, *dugu*, *zuek*, *gara*, *nire* (not the
  spoken *ze*, *eztut*, *degu*, *gera*, *nere*, *zan*), *h* where batua writes it (*hau*, *hori*,
  *hemen*, *hiru*, *hitz*, *ahaztu*, *ohe*), *tx*, *tz*, *ts* as the norm spells them. No
  Spanish words where Basque has its own (*bueno*, *vale*, *pues*, *hombre*, *suerte*, *listo*: write
  *beno* only in dialogue, *ados*, *ba*, *zorte*, *prest*); the list has some subtitle colloquialisms
  and Spanish loans: skip them. No slang and no swearing (the list has *kaka*, *puta*, *ostia*,
  *demontre*, *arraio*, *izorratu*, *putakume*: skip them).
- **Basque typography**: direct speech in angle quotation marks «…», each speaker's line a paragraph
  of its own; the narrator's comma goes after the closing mark, a question or exclamation mark of
  the quoted sentence inside it. No opening ¿ or ¡:

  ```
  «Kaixo, Ane», esaten du Mikelek.
  «Zer moduz?», galdetzen du Anek. «Kafe bat nahi duzu?»
  ```

  The questions and answers of the final `Galderak:` block are plain lines, as above. Write numbers
  as digits with their endings joined (*3 anaia*, *7etan*, *2020an*, *goizeko 8ak dira*: the checker
  reads the ending as part of the number), except a number word that is in your band: spell that one
  out to teach it (*bi anaia*).
- **Names are names**: write names in their Basque forms (*Bilbo*, *Donostia*, *Gasteiz*, *Iruñea*,
  *Baiona*; *Mikel*, *Ane*, *Jon*, *Irati*), with their case endings joined (*Mikelek*, *Aneri*,
  *Bilbon*, *Bilbora*, *Donostiako*, *Gasteizetik*): put the bare name in `names`, the checker reads
  the endings. Place adjectives and words for peoples or languages (*euskara*, *gaztelania*,
  *frantses*, *ingeles*, *amerikar*, *euskaldun*) count as words at their rank: never teach them as
  band words, a text may still use one. Never use a list word as a character's name (*Maite*, *Ibai*,
  *Eder*, *Izar*, *Lore*, *Amaia*, *Iker*, *Unai*, *Nahia*, *Haizea*, *Eneko* are list words).
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Basque list

- The list is counted from film subtitles, with the words of the Basque, Polish, French, Russian,
  Spanish and Portuguese dictionary packs: many words have no English dictionary entry (*uste*,
  *arazo*, *lasai*, *gutxi*, *ideia*); check them in `dictionaries/eu-pl.sqlite` or `eu-fr.sqlite`
  if `eu-en.sqlite` has nothing, or teach them in the sense their band forms show.
- A word's forms in the band file are its case forms and, for a verb, its imperfective, future,
  verbal noun and auxiliaries: *etxe* holds *etxea*, *etxean*, *etxera*, *etxeko*, *etxekoak*. A
  stacked form the list lacks (*etxekoekin*) is an outside word: write around it or report it.
- Words the list keeps apart from their verb are words of their own: *bizi* (alive, live) and
  *bizitza* (life), *lan* (work) and *landu*, *dei* and *deitu*, *irudi* and *iruditu*. Teach each
  only in its own sense; never teach a verb through its radical (*har* of *hartu*, *ezagut* of *ezagutu*) or a noun
  through a verb form.
- Question words take their cases inside one entry: *nor* holds *nork*, *nori*, *noren*, *norekin*;
  *zer* holds *zerk*, *zertan*, *zergatik*; *non*, *nora*, *nondik* are entries of their own.
- Some entries are subtitle noise or colloquial: *ze* (spoken *zer*), *ene*, *txo*, *aupa*, *benga*,
  *kaixo* is fine. Skip noise; teach colloquial words only when the story's dialogue needs them.
