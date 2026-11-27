# Writing an Albanian frequency course

Each course teaches one band of 100 words from the Albanian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `sq-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 6,219 words, so there are 63 courses; the
last band (6201–6219) has 19 words. Past rank 3900 the list was rebuilt (2026-11-27) without name
pieces, plurals and forms ranked as words, Gheg and archaic words and noise; ranks 1–3900 are as
they were.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Arta nuk e gjen telefonin. Shkon në kuzhinë. Nuk e sheh. Shkon në dhomë...*
  Everyday life where Albanian is spoken: Tirana, Durrës, Shkodër, Vlorë, Korçë, Elbasan, Berat,
  Gjirokastër, Sarandë, Fier, Lezhë, Pogradec, Kukës, Përmet; Prishtina, Prizren, Peja, Gjakova,
  Ferizaj, Gjilan, Mitrovica in Kosovo; Tetova, Struga and Ohri in North Macedonia; Ulqin in
  Montenegro; the lakes of Ohri and Shkodra, the Alps (Theth, Valbona), the Riviera (Dhërmi,
  Himara, Ksamil), the rivers Drin, Vjosë and Osum; and now and then Milan, Athens, London, Zürich
  or New York, where many Albanians live: home, family, food, shopping, work, school, the town,
  health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ai*/*ajo* is whenever it could be
  unclear. Give each story its own names: two Blerinas in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Albanian drops subject pronouns: write *ai*, *ajo*, *unë*, *ti* only
  where Albanian would (contrast, emphasis, a new subject). Little dialogue; when there is some,
  short and simple, with *ti* among family and friends and *ju* (+ the plural verb) with strangers,
  older people and in shops or offices (*Ku jetoni ju?*, *zoti Hoxha*, *zonja Kola*).
- **Names**: Albanian first names (Arta, Blerina, Ermira, Jonida, Elira, Rina, Drita, Valbona,
  Mimoza, Teuta; Arben, Besnik, Dritan, Ilir, Gëzim, Agron, Fatos, Erion, Kushtrim, Luan) and
  surnames (Hoxha, Kola, Leka, Shehu, Gashi, Krasniqi, Berisha, Dervishi, Murati, Basha). Names are
  declined like nouns, with their article and case endings: *Arta, Artën, Artës; Arbeni, Arbenin,
  Arbenit* (*Arta e takon Arbenin*, *libri i Artës*). Never use a list word as a name: *Drita*,
  *Mimoza*, *Luan*, *Lule*, *Bora*, *Flutura* are words of the list (light, lion, flower, snow,
  butterfly); pick other names.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense (*punon*, *shkon*, *ha*, *pi*, *flet*; *është*, *janë*, *ka*,
    *kanë*, *do*, *bën*, *thotë*, *vjen*, *sheh*, *di*), *jam* and *kam* (*është mjek*, *ka një
    vëlla*, *ka uri*), *dua të* / *mund të* / *duhet të* + subjunctive (*do të hajë*, *mund të vijë*,
    *duhet të punojë*), the future with *do të* (*do të shkojë nesër*); the **aorist** (the simple
    past for events: *shkoi*, *erdhi*, *bëri*, *tha*, *pa*, *hëngri*, *bleu*) from the first courses,
    in short sentences; nouns with their definite endings (*shtëpi*, *shtëpia*, *shtëpinë*; *djalë*,
    *djali*, *djalin*) after *në*, *me*, *nga*, *te*, *për*; the connecting article in its plain uses
    (*i madh*, *e madhe*, *të mëdhenj*; *motra e Artës*, *shoku i Arbenit*); the short pronouns
    before the verb (*e sheh*, *i thotë*, *më pëlqen*, *të dua*); simple questions (*çfarë*, *kush*,
    *ku*, *kur*, *si*, *pse*, *sa*);
  - ranks 301–600: the genitive and dative (*i jep motrës*, *te shtëpia e gjyshes*, *djalit i
    pëlqen*); possessives (*im*, *ime*, *yt*, *jote*, *i tij*, *e saj*, *ynë*, *juaj*, *i tyre*) with
    their articles; the **imperative** (*hajde*, *eja*, *shiko*, *merr*, *ul*, *fol*, *mos fol*); the
    short pronouns joined in pairs (*ma jep*, *ta tregoj*, *ia dha*, *ua tha*); comparatives (*më i
    madh se*, *më mirë*, *aq ... sa*); the **imperfect** (*ishte*, *kishte*, *bënte*, *shkonte*,
    *jetonte*) for background and habits; *po* + present for what is going on (*po lexon*);
  - ranks 601–1000: the **present perfect** (*ka ardhur*, *kam ngrënë*, *s'ka qenë*) beside the
    aorist; relatives *që*, *i cili*, *ku*; reported speech with *se* / *që* (*thotë se vjen*); the
    non-active verbs (*lahet*, *vishet*, *ulet*, *kthehet*, *quhet*, *duket*) in the present and the
    aorist (*u kthye*, *u ul*, *u bë*); *kur* and *nëse* with the present for real plans (*nëse bie
    shi, rrimë në shtëpi*);
  - ranks 1001–1300: the **conditional** (*do të doja*, *do të ishte mirë*, *do të shkonim*),
    superlatives (*më i mirë*, *shumë i bukur*, *më i madhi*), *ndërsa*, *prandaj*, *megjithëse*;
  - ranks 1301–1600: the **subjunctive** after more triggers (*para se të*, *pa*, *që të*, *në
    mënyrë që të*), the indefinites with negation (*nuk vjen askush*, *s'ka asgjë*, *asnjë*), the
    pluperfect (*kishte ardhur*, *kishin ngrënë*), *edhe pse*, *në vend që të*;
  - ranks 1601–2000: the passive with *nga* (*shtëpia u ndërtua nga gjyshi*), indirect questions
    (*nuk di nëse vjen*, *pyet ku është*), the **participle** constructions (*duke ecur*, *pa
    folur*, *për të mësuar*, *me të dalë*), the ablative (*larg shtëpisë*, *pranë detit*, *prej
    druri*);
  - ranks 2001–2500: the **admirative** in its everyday use (surprise: *Sa i madh qenka!*, *Ti
    paske ardhur!*), the **optative** in wishes (*Të lumtë!*, *Gëzuar!*, *Rrofsh!*, *U bëfsh njëqind
    vjeç!*), reported speech with tense shifts (*tha se do të vinte*), *sikur të* + imperfect
    subjunctive (*sikur të kisha kohë, do të udhëtoja*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *megjithatë*, *pra*, *domethënë*,
    *për më tepër*, *nga ana tjetër*, *si rrjedhim*; participle phrases; common idioms explained by
    their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (a light reflection on everyday life), dialogue that
    marks *ti* and *ju*, less common connectors (*ndonëse*, *gjithsesi*, *sidoqoftë*, *me një fjalë*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse;
  - ranks 5001–6219 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, figurative language and idioms used more freely, and
    topics that need some background, which the text gives.
  After rank 1000 sentences get longer and joined (*sepse*, *kur*, *ndërsa*, *edhe pse*, *prandaj*,
  *kështu që*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter to a newspaper, a short report), as long as it is
  everyday, calm and easy to follow. A learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Shkon në kuzhinë. Nuk e sheh. Shkon në dhomë. Nuk e sheh.*).
- **Questions at the end**: after the story, a paragraph starting `Pyetje:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *pse* and *si*, with fuller answers):

  ```
  Pyetje:
  Çfarë nuk gjen Arta?
  Arta nuk e gjen telefonin.
  Ku është telefoni?
  Telefoni është në makinë.
  ```

  The header `Pyetje:` is layout: the checker does not count it as a word.
- **Standard Albanian**: the narration is the standard literary language (*gjuha letrare*, the
  norm of 1972 as Albanian and Kosovo schools teach it), neutral, correct and natural Albanian as
  written today. No Gheg forms (*me punue*, *kam ardhë*, *âsht*, *nuk po du*, *qysh*, *kon*,
  *shpi*: write *të punojë* / *për të punuar*, *kam ardhur*, *është*, *nuk dua*, *si*, *kush*,
  *shtëpi*), no Kosovo or regional words where the standard has its own (*veturë*, *qysh*, *nuk po
  muj*, *shpi*: write *makinë*, *si*, *nuk mundem*, *shtëpi*), no Italianisms or Turkisms of the
  street (*ok*, *bravo* as filler, *rregull* is fine). No slang and no vulgar words (the list has
  some from film subtitles: skip them). Always write **ë** and **ç**: the checker compares them
  exactly, and many words differ only by them (*mirë* "well" / *mire*; *është* / *eshte*; *për* /
  *per*; *çka* / *cka*; *këtë* / *kete*; *vëlla* / *vella*); subtitle spellings without them are not
  words of the list.
- **Apostrophes**: the reader cuts a word at an apostrophe, so *s'kam* is *s* + *kam*, *t'i* is *t* +
  *i*, *ç'ke* is *ç* + *ke*, *m'u* is *m* + *u*. The list files *s* under *nuk*, *t* under *të*, *ç*
  under *çfarë* and *m* under *më*, so these count as known words; but write them sparingly, and
  only where standard Albanian writes them: *t'i* (*të* + *i*: *dua t'i them*), *s'* in dialogue
  (*s'e di*). In narration prefer *nuk* (*nuk e di*) and *çfarë* (*çfarë ke?*).
- **The short pronouns** (*më*, *të*, *e*, *i*, *na*, *ju*, *u*) go before the verb (*e sheh*, *i
  thotë*, *na pret*, *më pëlqen*), also after *mos* (*mos e merr*). An affirmative imperative may
  take *e* or *i* joined to its end (*merre*, *jepi*, *thuaji*), or the pronoun before it (*ma
  jep*, *e merr*): write what the norm writes. A definite or known object takes its short pronoun
  too (*E pashë Artën*, *Librin e lexoi*). The pairs *ma* (*më* + *e*), *ta* (*të* + *e*), *ia* (*i*
  + *e*), *ua*, *jua* are one word each; *ta*, *ia*, *ua* are words of the list.
- **Typography**: direct speech in double quotation marks “…” (or the angle quotes «…» that
  Albanian print also uses; keep one style per course), each speaker's line a paragraph of its own;
  the narrator's comma goes after the closing mark, a question or exclamation mark of the quoted
  sentence inside it:

  ```
  “Mirëmëngjes, Arta”, thotë Ilir.
  “Si je?” pyet Arta. “Do një kafe?”
  ```

  The questions and answers of the final `Pyetje:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ). Write numbers as digits (*3 vëllezër*, *në orën
  7*, *në 1992*), except a number word that is in your band: spell that one out to teach it (*dy
  vëllezër*). Days and months are lower case (*e hënë*, *të dielën*, *në janar*); nationalities and
  languages too (*shqiptar*, *shqip*, *italisht*).
- **Names are names**: place names, adjectives made from place names (*shqiptar*, *kosovar*,
  *italian*, *grek*, *amerikan*) and words for peoples or languages count as names: skip them as
  band words even when the list has them as entries; a text may still use one (*flet shqip*),
  counted at its rank. Days of the week, months and seasons are everyday words: teach them.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any normal everyday, cultural or story topic is
fine (holidays such as Bajrami, Krishtlindjet, Pashkët and Dita e Flamurit, history, myths and folk
tales, crime and detective stories, politics, love, death, sport). Only avoid explicit sexual
content, slurs used as insults and gratuitous gore. Teach ordinary words in their main sense; the
list has film-subtitle swear words (*dreq*, *mut*, *bushtër*, *kurvë*, *bastard*): skip those.

## Words of the list

- The list was counted from film subtitles and, at a tenth of the weight, news. Its headwords are
  the dictionary's citation forms: nouns in the indefinite singular (*shtëpi*, *djalë*), adjectives
  in the masculine without their article (*madh* for *i madh*, *bukur*), verbs in the first person
  singular present (*punoj*, *shkoj*, *jam*, *kam*, *them*, *bëj*). The non-active verbs are
  headwords of their own when the dictionary gives them one (*kthehem*, *ulem*, *dukem*, *quhem*,
  *ndihem*, *gëzohem*): a course that teaches *kthej* has not taught *kthehem*.
- Some little words share a form with another word: *jetë* is "life" and the subjunctive of *jam*
  (*të jetë*), and is listed under both; *ta*, *ia*, *ua*, *iu*, *u* are words of their own.
  *mi* is listed under *im* (*të mi* "my") and as "mouse"; *re* and *reja* under *ri* (*e re* "new"),
  while *retë* is "clouds" (*re*); *luan* under *luaj* ("plays"), while *luani*, *luanin* are under
  both *luaj* and *luan* ("lion").
- Skip as band words: names and nationality words (above), English (*ok*), interjections of the
  street, swear words, and words whose only sense in the dictionary is regional or old.
