# Writing a Tagalog frequency course

Each course teaches one band of 100 words from the Tagalog word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `tl-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Hindi makita ni Noel ang kaniyang susi. Wala ito sa bag niya. Wala rin ito
  sa mesa...* Modern everyday life in the Philippines: Maynila and Quezon City, Cebu, Davao,
  Iloilo, Bacolod, Baguio, Batangas, Lucena, Legazpi, Vigan, Dumaguete, Tagbilaran, Puerto
  Princesa; the islands (Bohol, Palawan, Mindoro, Siargao, Camiguin), Taal and Mayon, the rice
  terraces of Banaue; and now and then where Filipinos live and work abroad (Dubai, Singapore,
  Toronto, London, Sydney, Los Angeles, Tokyo), with Tagalog spoken at home: home, family, food,
  shopping, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *siya* is whenever it could be
  unclear (Tagalog *siya* is both "he" and "she"). Give each story its own names: the learner reads
  the course in one go, and two Marios in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, in the order Tagalog
  prefers: the predicate first (*Maganda ang bahay.* *Kumain si Ana ng tinapay.* *Nasa Cebu
  sila.*). The *ay* order (*Si Ana ay nasa Davao.* *Ang bahay ay maganda.*) is correct written
  Tagalog, but use it now and then (to open a story, to set a new subject apart), not in every
  sentence. Little dialogue; when there is some, short and simple, with *ikaw*/*ka* among family and
  friends of the same age and to children, and *po*/*opo* with *kayo*/*ninyo*/*inyo* (the respectful
  plural) to parents, older people, strangers and in shops and offices (*Saan po kayo pupunta?*,
  *Salamat po*). Kinship words address older relatives and are used again for "you" (*Lola, kumain
  na po ba kayo?*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: sentences without a verb (*Maganda ang Davao. Nasa bahay si Ana. Guro si
    Ben.*); the markers *ang*/*ng*/*sa* and *si*/*ni*/*kay* (*sina*/*nina*/*kina* for several
    people); the pronouns in their three sets (*ako*/*ko*/*akin*, *ikaw*, *ka*/*mo*/*iyo*,
    *siya*/*niya*/*kaniya*, *tayo*/*natin*, *kami*/*namin*, *kayo*/*ninyo*, *sila*/*nila*/*kanila*);
    possession after the noun (*bahay ni Ana*, *bahay ko*, *bahay natin*) and before it with the
    linker (*aking bahay*, *kaniyang anak*); the linker *na*/*-ng* between a word and the word it
    describes (*bagong bahay*, *magandang araw*, *malaking bansa*, *tatlong anak*, *bahay na
    malaki*); *may*/*mayroon* and *wala* (*May aso si Lea. Mayroon siyang aso. Wala siyang aso.*);
    *hindi*, *huwag*; yes/no questions with *ba* (*Nasa bahay ba si Ana?*) and *ano*, *sino*,
    *saan*, *nasaan*, *bakit*, *paano*, *ilan*; *gusto*, *ayaw*, *kailangan*, *dapat*, *maaari* +
    the infinitive (*Gusto kong kumain.*); actor-focus verbs with *-um-* and *mag-* in their three
    aspects: completed for what happened (*kumain*, *nagluto*), incompleted for what is going on and
    habits (*kumakain*, *nagluluto*), contemplated for what will happen (*kakain*, *magluluto*); the
    infinitive in commands and requests (*Kumain ka na.* *Umupo po kayo.*); the enclitics in their
    place after the first word (*Kumain ka na ba?* *Nasa bahay na rin siya.* *Hindi pa siya
    kumakain.*);
  - ranks 301–600: object-focus verbs, with the doer in *ng* and the thing done in *ang*: *-in*
    (*Kinain ni Ana ang tinapay.* *Bibilhin ko ito.*), *i-* (*Ibinigay niya ang regalo.*), *-an*
    (*Tinawagan ni Ben si Lea.*), and the question that goes with them (*Ano ang binili mo?*);
    comparisons (*mas malaki kaysa sa*, *pinakamalaki*, *kasinlaki ng*); *nang* before a manner
    word (*kumain nang mabilis*) beside *ng* before a noun; the time (*alas-7 ng umaga*, *ng
    hapon*), days and months; *na* and *pa* as "already" and "still / yet"; *dahil*, *kaya*,
    *pero*, *at saka*;
  - ranks 601–1000: the *ma-*/*maka-* verbs of ability and of what happens without trying
    (*nakita*, *narinig*, *nakarating*, *makakatulong*), *magpa-* for having something done
    (*nagpagupit*, *magpapaluto*), relative clauses with the linker (*ang batang kumain*, *ang
    librong binili ko*), reported speech with *na* (*Sinabi niya na pagod siya.*) and *raw*/*daw*,
    time clauses (*nang dumating siya*, *pagdating niya*, *bago kumain*, *pagkatapos kumain*);
    *kung* for real plans (*Kung uulan, sa bahay tayo.*);
  - ranks 1001–1300: the just-completed aspect (*kakakain lang niya*), *pinaka-* and *napaka-*
    (*napakaganda*), *nakaka-* adjectives (*nakakapagod*), nouns in *ka-...-an* and *pag-*
    (*kalusugan*, *pagkain*, *pag-aaral*), *kahit*, *habang*, *bagaman*, *upang*/*para*;
  - ranks 1301–1600: the indefinites (*sinuman*, *anuman*, *kahit saan*, *walang sinuman*), *sana*
    and *baka*, conditions that did not happen (*Kung umalis siya nang maaga, nakarating sana
    siya.*), *maki-*/*makipag-* verbs (*makipaglaro*, *nakikain*), *ipa-*/*ipag-* (*ipinagluto niya
    ako*);
  - ranks 1601–2000: verbal nouns as subjects (*Mahalaga ang pagbabasa.*), *pagka-* (*pagkakita
    niya*, *pagkadating niya*), longer relative clauses, the reciprocal *mag-...-an* (*nag-usap*,
    *nagkita*, *nagtulungan*), formal connectors (*samakatuwid*, *bukod dito*, *gayunpaman*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another, *ayon sa*, *tungkol
    sa*, *hinggil sa*, *dahil sa*, reported speech kept as Tagalog keeps it (no change of aspect);
  - ranks 2501–3000: a richer, more formal register: connectors such as *gayunman*, *sa kabila
    nito*, *sa madaling salita*, *bilang resulta*; common idioms and proverbs (*salawikain*)
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*kolum*, a light reflection on everyday life), dialogue
    that marks polite and familiar speech (*po*, *kayo* against *ka*), less common connectors
    (*samantala*, *sakali*, *bagkus*, *subalit*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone and
    register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, regional and learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*dahil*, *kaya*, *kapag*, *habang*, *kahit*,
  *pero*, *ngunit*, *upang*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as a message, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Wala ito sa bag. Wala rin ito sa mesa. Wala talaga.*).
- **Questions at the end**: after the story, a paragraph starting `Mga tanong:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *bakit* and *paano*, with fuller answers):

  ```
  Mga tanong:
  Ano ang hindi makita ni Noel?
  Hindi niya makita ang kaniyang susi.
  Nasaan ang susi?
  Nasa kamay ni Noel ang susi.
  ```

  The header `Mga tanong:` is layout: the checker does not count it as words.
- **Standard Tagalog/Filipino**: the narration is the standard written language, spelled by the
  Komisyon sa Wikang Filipino's *Ortograpiyang Pambansa* and *Manwal sa Masinop na Pagsulat*:
  neutral, correct and natural Tagalog as written today. Write *kaniya*, *kaniyang*, *siya*,
  *iyon*, *iyan*, *puwede*, *kuwento*, *kumusta*, *huwag*, *mayroon*, *ninyo*, *bakit*, *sa iyo*,
  *sa akin* in full (not *kanya*, *sya*, *yun*, *yan*, *pwede*, *kwento*, *wag*, *meron*, *niyo*,
  *ba't*, *sa'yo*, *sa'kin*); *ng* before a noun and *nang* for "when", "so that", before a manner
  word and between repeated verbs (*nang dumating siya*, *kumain nang mabilis*, *tumakbo nang
  tumakbo*); *din*/*daw*/*dito*/*doon* after a consonant and *rin*/*raw*/*rito*/*roon* after a
  vowel or *w*/*y* (*Pagod din ako.* *Pagod na rin siya.*); the linker *-ng* joined to a word
  ending in a vowel (*bagong*), *-g* to one ending in *n* (*ating*, *kaniyang* but *iyong*),
  *na* after other consonants (*mabait na bata*); *mga* for the plural (*mga bata*), never *manga*.
  No Taglish: never mix English into a Tagalog sentence (*Nag-text siya*, *super ganda*, *anyway*,
  *so*, *actually*, *pero okay lang*); English and Spanish loans only where standard Filipino
  writes them, in their Filipino spelling (*telepono*, *kompyuter*, *litrato*, *eskuwela*,
  *trabaho*, *kotse*, *bus*, *internet*, *video*). No text-speak (*lng*, *ksi*, *dba*, *nmn*), no
  slang or gay lingo (*lodi*, *petmalu*, *chika*, *jowa*, *keri*, *pabebe*), no regional forms
  standing in for the standard. *ang ganda*, *ang laki* (how beautiful, how big) and *lang*,
  *naman*, *nga*, *pala*, *kasi* are standard and welcome. Dialogue may be lightly colloquial only
  in what any careful speaker says (*sige*, *tara*, *teka*, *halika*, *naku*, *oo nga*). Always
  write the full spelling: the checker compares spellings exactly, and the list's misspellings
  (*sya*, *nmn*, *tlga*) never count as the word.
- **Tagalog typography**: direct speech in curly double quotation marks “…”, each speaker's line a
  paragraph of its own; the comma, question mark or exclamation mark of the quoted sentence inside
  the marks, the tag after it (*sabi ni*, *tanong ni*, *sagot ni*):

  ```
  “Kumain ka na ba?” tanong ni Lola Cora.
  “Opo, Lola,” sagot ni Ana. “Kumain na po ako.”
  ```

  The questions and answers of the final `Mga tanong:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens (*mag-aral*,
  *pag-ibig*, *araw-araw*, *taga-Cebu*). Write numbers as digits (*3 anak*, *5 taon*, *alas-7 ng
  umaga*, *ika-5 ng Hunyo*, *noong 1995*, *₱50*), except a number word that is in your band: spell
  that one out to teach it (*dalawang taon*, *tatlong anak*).
- **Names are names**: place names, words for peoples and languages (*Pilipino*, *Amerikano*,
  *Intsik*, *Kastila*, *Hapon* as "Japanese", *Ingles*, *Tagalog*, *Bisaya*) count as names: skip
  them as band words even when the list has them as entries; a text may still use one (*marunong
  siyang mag-Ingles* is better written *marunong siya ng Ingles*), counted at its rank. Never use a
  list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*Diyos*, *Panginoon*, *Bathala*), saints and the *Santo Niño*,
  church and chapel, priests and nuns, prayer, mass, heaven and hell, angels and devils, souls and
  spirits, *sin*, the cross, and every custom or holiday of religious origin, even as a date, a
  school break or a gift season: Christmas (*Pasko*, *Noche Buena*, *Simbang Gabi*, *parol* as a
  Christmas lantern), *Semana Santa* and Holy Week, *Undas* and All Saints' Day, *Santacruzan* and
  *Flores de Mayo*, the town *pista* of a patron saint, *Sinulog*, *Ati-Atihan*, *Dinagyang* and
  *Pahiyas* (all saints' feasts), and expressions such as *Diyos ko*, *salamat sa Diyos*, *sa awa
  ng Diyos*; New Year's Eve (*Bagong Taon*) is fine only as a family evening. Folk customs with
  no church holiday or saint named are fine: a harvest or flower festival (*Panagbenga* in Baguio,
  *Kadayawan* in Davao, *MassKara* in Bacolod), a town fair (*perya*), a market (*palengke*), a
  birthday, a wedding told as family news (no church, no mass), *mano po* to an elder, the start of
  the school year, the summer holidays. Use *langit* only as the sky and *maniwala* only as
  believing a person or a fact;
- **politics**: the state and the government (*pamahalaan*, *gobyerno*), Congress, elections,
  parties, the president, mayors and governors as politicians, kings and rulers, nationalism,
  independence, flags, the national language as a political question, and disputes over borders
  and islands; everyday civic life (a library, a school, the *barangay* hall's counter, a
  neighbourhood clean-up, the town's market) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property counter or a
  traffic aide at a school is fine; theft, arrests, prisons and trials are not); typhoons, floods
  and earthquakes stay mild (a storm keeps the boats in port and school closes for a day; nobody is
  hurt or lost);
- **death**, killing, funerals, wakes (*lamay*), graves, blood and injury beyond a scraped knee; an
  old person in a story is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults (*tanga*, *gago*, *bobo*), words whose main sense is sexual,
  and kissing, courtship gone wrong, separation and divorce;
- **alcohol** in every form (beer, *tuba*, *lambanog*, *gin*, an *inuman* drinking session, a
  toast), **gambling** (*sabong* cockfighting, *jueteng*, lotto, betting, casinos, *mahjong*),
  drugs and smoking;
- **deliberate misspellings**: no text-speak, no clipped or colloquial spellings (*sya*, *yung*,
  *kc*, *nmn*, *wag*, *meron*), not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a word for a people or
  a language is skipped. Common Filipino first names are fine as characters.

The themes keep to this: the history courses (*Kasaysayang nahahawakan*, *Arkeolohiya at
nakaraan*, *Mga manlalakbay at manggagalugad*) tell the history of everyday life, towns, crafts,
trade, science and travel, never of wars, colonisers, rulers or politics; *Mga perya at pagdiriwang
ng bayan* and *Sining at kaugaliang bayan* are folk, harvest and family celebrations with no church
holiday or saint named; *Mga serbisyong pampubliko* is the library, the health centre and the
*barangay* and city counters, never politics; *Ang wikang Tagalog* and *Mga wika at kultura ng
kapuluan* are about words, the many languages of the islands and learning, never about language
politics; *Mga kuwentong-bayan at alamat* tells folk tales (how the pineapple got its eyes, clever
animals, giants) without gods, spirits, *aswang*, *kapre*, *tikbalang* or *engkanto*.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *diyos* 102, *simbahan* 217, *santo* 275, *kasalanan* 378, *banal* 549, *panginoon*
  631, *relihiyon* 713, *krus* 746, *pari* 747, *katoliko* 763, *anghel* 998; *langit* 854 only as
  the sky;
- politics: *hari* 204, *pamahalaan* 196, *pangulo* 208, *batas* 227, *opisyal* 237, *pambansa*
  278, *estado* 292, *kapangyarihan* 332, *pinuno* 372, *gobyerno* 401, *karapatan* 432, *kaharian*
  451, *mamamayan* 455, *duke* 494, *partido* 537, *republika* 583, *imperyo* 652, *ministro* 702,
  *kalayaan* 705, *halalan* 889, *emperador* 893, *kampanya* 930, *kandidato* 957; *bayan* 123 only
  as a town, *malaya* 585 only as free time or free to choose;
- war, crime, police, courts: *patayin* 214, *digmaan* 310, *pulis* 338, *pumatay* 361,
  *sundalo* 543, *hukbo* 571, *militar* 576, *kalaban* 785 (except an opponent in a game),
  *iligtas* 794, *lumaban* 845, *biktima* 850, *magnakaw* 904, *pagpatay* 953, *tumakas* 958,
  *pinsala* 997, *baril* 1000, *espada* 471, *labanan* 470; *laban* 188 only as a match or "against"
  in a game, *kaso* 246 only as "case, instance" (*sa kasong ito*);
- death: *mamatay* 159, *patay* 280, *kamatayan* 370, *dugo* 422;
- vulgar and insults: *tanga* 661, *puwit* 777, *bakla* 912, *kalokohan* 954;
- names, peoples, languages: *korea* 595 (and *pilipino* 1108, *ingles*, *tagalog* are not in the
  list);
- list noise, spoken variants and forms of other words: *niyo* 62 (spoken *ninyo*), *de* 166,
  *eh* 165, *ha* 176, *hoy* 177, *pare* 340 (slang "buddy"), *sa'yo* 350 (write *sa iyo*), *ya*
  615, *taga* 688 (a prefix: *taga-Cebu*), *okey* 148 only in dialogue, *maari* 64 (the list's
  spelling: write *maaari*, which counts for it), *naganap* 758 (a form of *maganap* 498: skip it).

Further on, the same categories hold; for example *Pasko*, *dasal*, *magdasal*, *misa*, *kaluluwa*,
*demonyo*, *impiyerno*, *bilangguan*, *kulungan*, *magnanakaw*, *hukom*, *korte*, *serbesa*, *alak*,
*lasing*, *sigarilyo*, *sugal*, *sabong*, *libing*, *lamay*, *patay*, *senador*, *halalan*,
*watawat*.

## The file

Write `tools/courses/tl/<id>.json`:

```json
{
  "id": "tl-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Tagalog, e.g. Pagkain at pamimili",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Tagalog",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/tl/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *kumain*, *kumakain* or *kakain* for *kumain*; *magandang*
  or *ganda* for *maganda*). Reuse earlier lessons' new words too. `newWords` holds the word as the
  list writes it (the third column of the band file): a verb as the list heads it (*kumain*,
  *magluto*, *gawin*, *sabihin*, *makita*), a noun or an adjective in its plain form.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a spoken variant, a vulgar word, a list error such as a name or an English word,
  a form of another word, a word for a people or a language, a word of the content rules); the
  checker's last line lists the band words not taught yet.

Things to know about the Tagalog list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/tl/wordlist.tsv`
  (a copy of the app's `tl.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  film subtitles, the web and Tagalog Wikipedia and news, so talk words and encyclopaedia words
  (*lalawigan* 202, *rehiyon* 233, *populasyon* 250, *siglo* 259, *distrito* 524) both rank high,
  while kitchen and household words rank low (*bahay* 131, *pagkain* 212, *kape* 1381, *kanin*
  1668, *tinapay* 1867, *kusina* 2339).
- **How the checker reads words.** A word with a hyphen is one word, as the list has it
  (*mag-aral* 514, *pag-aaral* 483, *may-ari* 851, *mag-isa* 1142); one the list lacks is an outside
  word (*pang-araw-araw*, *taga-Cebu*), so write such words only where the text needs them. A
  fully repeated word counts as its base: *araw-araw* is *araw*, *bahay-bahay* is *bahay*,
  *gustong-gusto* is *gusto*, *bagong-bago* is *bago*; so the list's own entries *araw-araw* 1325,
  *isa-isa* 3687, *halo-halo* 4680 never count (skip them), and *unti* 1340 and *dahan* 2141 are
  taught through *unti-unti* and *dahan-dahan*. The contractions *'y* and *'t* are *ay* and *at*:
  *ako'y* is *ako* + *ay*, *iba't iba* is *iba* + *at* + *iba*; but *ba't* (written in full:
  *bakit*), *sapagka't*, *bagama't*, *nguni't* and *subali't* are one word each. *ika'y* is *ikaw* +
  *ay*. A word joined by a hyphen to digits (*ika-5*, *alas-7*) is part of the number. The
  linker counts for its word: a word ending in *-ng* or *-g* that the list lacks counts as the word
  before the linker (*kapeng* is *kape*, *tinapay na* needs none); the list already files most
  linked forms (*isang* under *isa*, *kaniyang* under *kaniya*, *aking* under *akin*, *magandang*
  under *maganda*). Some words in *-ng* are words of their own: *lamang*, *habang*, *parang*,
  *malamang*, *maging*, *bilang*, *tulong*, *galing*.
- **Pronouns and little words are entries of their own**, each set apart: *ako* 15, *ko* 11, *akin*
  53 (with *aking*, *sa akin*); *ikaw* 97 (with *ika* of *ika'y*), *ka* 18, *mo* 12, *iyo* 91 (*iyong*, *sa
  iyo*); *siya* 13, *niya* 26, *kaniya* 22 (*kaniyang*, *kanya*); *tayo* 55, *natin* 57, *atin* 151
  (*ating*); *kami* 70, *namin* 75, *amin* 198 (*aming*); *kayo* 47, *ninyo* 240, *inyo* 140; *sila*
  44, *nila* 56, *kanila* 54. Below rank 151 write *bahay natin*, not *ating bahay*, and below 198
  *bahay namin*, not *aming bahay*. *si* 14 holds *sina*; *ni* 16 holds *nina*; *kay* 69 (*kina* is
  its own entry). *ito* 10, *nito* 40, *dito* 42 (with *rito*); *iyan* 73, *niyan* 861, *diyan* 245;
  *iyon* 31, *noon* 21 (with *noong*), *doon* 107 (with *roon*). *iyong* is filed under both *iyon*
  and *iyo*. *may* 17 and *mayroon* 74 are two entries; *wala* 30; *nasa* 66 and *nasaan* 215;
  *maari* 64 is written *maaari*.
- **Verbs.** The list heads a verb with its infinitive and files its aspect forms under it
  (*kumain*, *kumakain*, *kakain*; *nagluto*, *nagluluto*, *magluluto* under *magluto*). The
  focus forms of one root are separate entries, each a word of its own: *gumawa* 96 (actor: *gumawa
  siya ng mesa*) and *gawin* 41 (object: *ginawa niya ang mesa*); *magbigay* 146, *bigyan* 190
  and *ibigay* 220; *magsabi* 360 and *sabihin* 38 (with *sinabi*); *kumuha* 113 and *kunin* 191;
  *tumingin* 511 and *tingnan* 139; *tumawag* 482, *tawagin* 133 and *tawagan* 493; *sumunod* 141;
  *bumili* 294; *makita* 72 (*nakita*, *nakikita*) and *makakita* 683. Teach each one in its own
  focus, through its own forms: the list files some forms of a root under both entries (*ginagawa*,
  *nagawa*, *magagawa* under *gawin* and *gumawa*; *kinukuha* under *kumuha* and *kunin*;
  *sinasabi* under *sabihin* and *magsabi*), and such a shared form counts for both, so a new word
  must also be used in a form that is plainly its own (*gumawa*, *gumagawa*, *gagawa* for *gumawa*;
  *ginawa*, *gagawin* for *gawin*). A noun or adjective from the same root is a word of its own
  (*gawa* 277, *sabi* 132, *tingin* 327, *tulong* 299 beside *tumulong* 391 and *tulungan* 369;
  *laro* 255 beside *maglaro* 339; *pagkain* 212 beside *kumain* 206).
- **Words filed under another word.** Some entries hold a word spelled alike, which counts at the
  entry's rank but never teaches it: *taong* is both *tao* 39 and *taon* 68; *bago* 60 is "new"
  and "before"; *hapon* 320 is "afternoon" (and "Japanese", a name); *baka* 213 is "maybe" and
  "cow"; *puno* 232 "tree" and "full"; *pala* 194 "so, after all" and "shovel"; *doon* 107 holds the English
  *don't*; *santo* 275 holds *san*; *laki* and *ganda* are filed under *malaki* and *maganda*.
  Teach the entry in its own sense only.
- **Missing words.** The list has no days and no months except *linggo* 272 ("week", and *Linggo*
  "Sunday"): *Lunes*, *Martes*, *Miyerkoles*, *Huwebes*, *Biyernes*, *Sabado*, *Enero* ...
  *Disyembre* go in `names`. Nor does it have *Pilipinas*, *Maynila*, *Pilipino* as a people,
  *Ingles*, *Tagalog*: they are names. Generated verbs (about a fifth of the top 10,000) have no
  dictionary gloss in the app; they are real words and taught as any other.
- **Entries of several words** (*at saka* 7468, *punong ministro* 3762, *noong unang panahon*
  9408) can never be counted, since the checker reads words one by one: skip them.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). Every
  headword in it must be a headword of the list (the checker warns otherwise). If a true form is
  missing, write around it and name the word and form in your report.
- **Names** are not in the list: people, places (*Cebu*, *Davao*, *Iloilo*, *Bohol*), shops,
  brands, days and months go in `names`. A name of several words or with a hyphen (*Quezon City*,
  *Puerto Princesa*, *Lapu-Lapu*) counts as one known word wherever it is written whole; list it
  whole. Keep only true names in `names`, never common words, and never a name that is also a
  list word: as characters avoid *Rosa* (*rosas*), *Ligaya*, *Tala*, *Dalisay*, *Mayumi*, *Totoy*,
  *Inday*, *Nina* (*ni*'s *nina*), *Rosario*, *Lito*, *Angel*, *Luz*, *May*, *Hong Kong* and *El
  Nido* (*hong*, *kong*, *el* are list words), and as places *Makati*, *Pasig*, *Tagaytay*,
  *Antipolo*, *Tarlac*, *Naga*, *Bulacan*, *Butuan*, *Bicol*, *Cubao*, *Quiapo*: they count at their
  own rank, so leave them out of `names` and use them only as outside words, or pick another town.
  Check a name with `rank.py` before you use it. Safe in the pilot: *Ana*, *Ben*, *Cora*, *Jun*,
  *Gina*, *Rico*, *Edna*, *Mario*, *Lea*, *Noel*, *Elsa*, *Tess*, *Dante*, *Andres*, *Mia*,
  *Carlo*, *Elena*, *Celso*, *Nilo*, *Joy*, *Lino*; *Davao*, *Iloilo*, *Batangas*, *Maynila*,
  *Bohol*, *Dubai*.
- Everyday words rank later than you would think: *anak* 116, *babae* 115, *lalaki* 125, *bata*
  130, *bahay* 131, *sabi* 132, *kaibigan* 161, *gabi* 173, *salamat* 174, *tubig* 180, *pamilya*
  186, *ama* 195, *kumain* 206, *pagkain* 212, *kapatid* 219, *trabaho* 249, *asawa* 267, *ina*
  284, *paaralan* 325, *masaya* 337, *umaga* 341, *pera* 359, *aso* 475, *tatay* 561, *nanay* 602,
  *kotse* 604, *kuya* 793, *pusa* 865, *lola* 951, *kape* 1381, *lolo* 1509. The dialogue tag
  *sabi* is rank 132, *tanong* 438 and *sagot* 686: in the first course use few tags and let the
  turns of two speakers stand untagged.
- The list's form lists are noisy (spoken and text spellings such as *sya*, *nmn*, *tlga*, *kc*,
  *yung*; Spanish and English look-alikes such as *vacas* under *wakas*, *oui* under *umuwi*,
  *auf* under *hayop*): a new word must appear as itself or a true form, never through such a form.
  Some entries are names, English words or Wikipedia fragments: skip them. Loanwords Filipino uses
  as its own (*telepono*, *kompyuter*, *video*, *internet*, *bus*, *tren*, *kotse*) are taught like
  any word.
