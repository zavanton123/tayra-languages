# Writing an Icelandic frequency course

Each course teaches one band of 100 words from the Icelandic word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `is-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Sara vill fá bók Jóns. Jón vill ekki gefa henni bókina. Hann gefur henni
  aðra bók...* Everyday life in Iceland: Reykjavík, Kópavogur, Hafnarfjörður, Akureyri, Ísafjörður,
  Egilsstaðir, Selfoss, Húsavík, Akranes, Borgarnes, Stykkishólmur, Siglufjörður, Höfn, Vík; the
  Westman Islands (*Vestmannaeyjar*), Mývatn, Þingvellir, the fjords, the moors and the highlands, the
  glaciers, the hot springs and the swimming pools; and now and then Copenhagen
  (*Kaupmannahöfn*), Oslo or the Faroe Islands (*Færeyjar*), where Icelanders live, study and visit:
  home, family, food, shopping, work, school, the town, health, free time, travel, the seasons
  (the light summer nights, the dark winter), the summer house (*sumarbústaður*: an outside word in
  the low bands), the swimming pool (*sundlaug* 2110) and the hot tub (*heiti potturinn*).
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *hann* or *hún* is whenever it could
  be unclear. Give each story its own names: the learner reads the course in one go, and two Söru
  in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order.
  Icelandic puts the finite verb second in a main clause: *Sara kaupir brauð*, but *Í dag kaupir
  Sara brauð*, *Svo fer Jón heim*, *Á morgun ætlar hún að fara*. A yes/no question starts with the
  verb (*Kemur þú?*, *Er Sara heima?*); the verb comes before *ekki* (*Hún kemur ekki*, *Ég veit það
  ekki*). Little dialogue; when there is some, short and simple. **Icelanders say *þú* to
  everyone**, a stranger, an older person, a doctor, a shop assistant, a teacher; the old polite
  *þér* is not used today: never write it. People are named and addressed by their first name
  (*Góðan daginn, Sara!*); there are no family names, only patronymics (*Jón Pétursson*, *Sara
  Jónsdóttir*), and no "Mr" or "Mrs" (*herra*, *frú*: skip them).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense of the common verbs, strong and weak (*kemur*, *fer*, *gerir*,
    *talar*, *býr*), affirmative and negative (*hún kemur ekki*); *vera* and *hafa*; the past tense
    of the common verbs from the first courses, in short sentences (*var*, *kom*, *fór*, *sagði*,
    *gerði*, *sá*); the four cases in their plain uses: the nominative subject, the accusative
    object, *í*/*á* with the dative for where something is (*í húsinu*, *á Akureyri*) and with the
    accusative for where it goes (*í skólann*, *á staðinn*), *til* with the genitive (*til Söru*,
    *til Akureyrar*), *frá*, *úr*, *hjá*, *með* with the dative (*hjá Jóni*, *með Söru*); *heim*
    (going home) and *heima* (at home); the suffixed article (*maðurinn*, *konan*, *húsið*, *í
    húsinu*); possessives after the noun (*bíllinn minn*, *húsið hans*; without the article with
    close family: *mamma mín*, *pabbi hennar*, *bróðir minn*); *ég heiti*, *ég á*, *ég vil*, *ég
    ætla að* + infinitive; questions with the verb first and with *hvað*, *hver*, *hvar*, *hvert*,
    *hvaðan*, *hvenær*, *hvernig*, *af hverju*; the numbers *einn*, *tveir*, *þrír*, *fjórir* with
    their gender (*tveir menn*, *tvær konur*, *tvö börn*), other numbers as digits;
  - ranks 301–600: the imperative with its joined pronoun (*komdu*, *farðu*, *sestu*, *gerðu það*,
    *taktu*, *sjáðu*; *ekki fara*, the plural *komið*, *farið*); the common middle verbs (*finnast*,
    *gerast*, *komast*, *hittast*, *sjást*: *Mér finnst þetta gott*, *Sjáumst!*); the oblique
    subjects (*mig langar að*, *mér finnst*, *mér líður vel*, *mér líkar*, *mig vantar*, *mér þykir
    gaman*); the modal verbs (*ég þarf að fara*, *ég á að fara*, *ég vil fara* (no *að* after
    *vilja*), *ég get farið* (the supine after *geta*), *ég má fara*); *að vera að* + infinitive for
    what is going on (*hún er að lesa*); the plural cases (*í húsunum*, *með börnunum*);
    comparatives (*stærri en*, *betri*, *meira*); the weak adjective with the article (*stóra
    húsið*, *nýi bíllinn*);
  - ranks 601–1000: the perfect with *hafa* and with *vera búinn að* (*hún hefur komið*, *hann er
    búinn að borða*), the pluperfect (*hafði farið*); *myndi* + infinitive and *væri*, *gæti*,
    *mætti*, *vildi* for wishes and polite questions (*Gætirðu hjálpað mér?*, *Það væri gaman*);
    relative clauses with *sem* (*maðurinn sem býr hér*, *bókin sem ég las*); superlatives
    (*stærstur*, *besti*); reported speech with *að* and the subjunctive (*Sara segir að hún sé
    þreytt*, *Hann sagði að hann kæmi*); *af því að*, *þegar*, *ef*, *þótt*; the genitive after
    *til* and in compounds of place (*til Reykjavíkur*);
  - ranks 1001–1300: the passive with *vera* and *verða* + participle (*húsið var byggt árið
    1950*, *búðinni verður lokað*); time clauses (*áður en*, *eftir að*, *þangað til*, *um leið og*,
    *á meðan*); the reflexive *sig*, *sér*, *sín* in their places;
  - ranks 1301–1600: participles as adjectives (*lokuð búð*, *brotinn bolli*, *hlæjandi barn*);
    the indefinites with negation (*enginn kom*, *ekkert*, *aldrei*, *hvergi*, *neinn*, *neitt*);
    *hvor* and *hvorugur*; *báðir*, *sumir*;
  - ranks 1601–2000: the subjunctive in conditions (*ef ég hefði vitað það hefði ég komið*, *ef ég
    væri þú*); indirect questions (*hún spurði hvort hann kæmi*, *ég veit ekki hvar hann býr*); the
    reciprocal middle (*þau hittust*, *við sjáumst*, *þeir kynntust*);
  - ranks 2001–2500: longer sentences joined with *þótt*, *til þess að*, *svo að* (with the
    subjunctive), *nema*, *heldur en*; the present participle (*-andi*) in set uses
    (*sjóðandi vatn*, *komandi vika*); the genitive in fixed phrases (*til fjalla*, *til sjávar*,
    *á milli*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *hins vegar*, *aftur á
    móti*, *engu að síður*, *þar af leiðandi*, *auk þess*, *þar að auki*, *í stuttu máli*; common
    idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*pistill*, a light reflection on everyday life), and
    less common connectors (*þrátt fyrir*, *enda*, *úr því að*, *í ljósi þess*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; the nominal style (*-un*, *-ing*, *-leiki* nouns)
    used with care; long sentences that stay easy to parse; tone and register chosen to suit the
    text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images
    and rhythm, academic and journalistic registers, learned and old words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*af því að*, *þegar*, *þótt*, *að*, *en*, *svo
  að*), dialogue can carry more of the story, and a story may be told by a first-person narrator
  or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short
  informative or opinion text (a column, a letter to a newspaper, a short report), as long as it is
  everyday, calm and easy to follow. Still one clear idea at a time: a learner at the level should
  follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Jón kemur alltaf fyrstur. Svo kemur Sara og svo koma börnin.*).
- **Questions at the end**: after the story, a paragraph starting `Spurningar:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *af hverju* and *hvernig*, with fuller
  answers):

  ```
  Spurningar:
  Hvar búa Sara og Jón?
  Þau búa á Akureyri.
  Er húsið stórt?
  Nei, húsið er ekki stórt, en það er gott.
  ```

  The header `Spurningar:` is layout: the checker does not count it as a word.
- **Standard written Icelandic**, as a careful native writer writes it today, in narration and in
  dialogue alike: no spoken, slang or dialect forms. Never *ókei*, *sko*, *ha*, *ja*, *jamm*,
  *hey*, *vá*, *bæ*, *kúl*, *næs*, *töff*, *geðveikt* as "great", *gaur*, *dúd*, the spoken
  plural question forms with *þið* joined (*eruði*, *hafiði*, *komiði*: write *eruð þið*), *ég
  meina*, *akkuru* for *af hverju*, *mér langar* for *mig langar* (the "dative sickness", *þágufallssýki*:
  write *mig langar*, *mig vantar*, *hana langar*), *ég vill* for *ég vil*. The singular question
  forms with the pronoun joined (*ertu*, *viltu*, *kemurðu*, *hefurðu*, *veistu*) and the
  imperatives with it (*komdu*, *farðu*, *sestu*) are standard and welcome. Every *á*, *é*, *í*,
  *ó*, *ú*, *ý*, *þ*, *ð*, *æ* and *ö* is written, and *y*/*ý* and *i*/*í* as the spelling has
  them: the checker compares letters exactly, and they tell words apart (*á* "on" / *a*; *sá*
  "saw" / *sa*; *ár* "year" / *ar*; *hár* "high, hair" / *har*; *býr* "lives" / *bír*; *ís*
  "ice cream" / *is*; *fer* "goes" / *fér*; *heim* "home" / *heimur* "world"'s forms). Write *é*,
  never *je* (*ég*, *hér*, *ég sé*). Days, months, languages and the adjectives of peoples and
  countries are written in lower case (*mánudagur*, *maí*, *íslenska*, *íslenskur*, *danskur*), and
  so is *ég*; names of places and people, and the nouns for peoples (*Íslendingur*, *Dani*), with a
  capital.
- **Compounds are one word** in Icelandic (*morgunmatur*, *strætóstoppistöð*, *sumarbústaður*,
  *bókabúð*). A compound counts as itself: at its own rank when the list has it as an entry, and as
  an outside word when it does not, even if both its parts are list words (the checker never splits
  a compound). So use compounds the list has (`rank.py` tells), and otherwise write the idea with
  two list words where Icelandic allows it (*bíll fjölskyldunnar*, not *fjölskyldubíll*), or keep
  the compound as one of the lesson's few outside words.
- **Icelandic typography**: direct speech in the Icelandic quotation marks „…“ (low opening, high
  closing), each speaker's line a paragraph of its own; the narrator's comma goes inside the
  closing mark, and the verb comes before the speaker after a quote:

  ```
  „Húsið er gott,“ segir Sara.
  „Hvað er þetta?“ spyr Jón. „Má ég fá það?“
  ```

  The questions and answers of the final `Spurningar:` block are plain lines, as above. A comma
  goes before *að*, *sem*, *þegar*, *ef*, *af því að*, *þótt* only where the clause is long or the
  sense needs it, as Icelandic writers do today; never between a verb and its object. Inside a
  sentence a dash is the en dash with spaces ( – ). Write numbers as digits (*3 börn*, *klukkan
  7*, *klukkan hálf átta* only if *átta* is known, *árið 1992*), ordinals with a full stop (*1.
  maí*, *í 5. bekk*), the decimal comma (*2,5 kíló*), and prices as *500 krónur* (*króna* 985) or
  *500 kr.*, except a number word that is in your band: spell that one out to teach it (*tvö
  börn*). Numbers from 1 to 4 decline with the noun (*tveir menn*, *tvær konur*, *tvö börn*, *með
  tveimur börnum*): write them as words when the case or gender matters to the sentence.
- **Names are names**: place names, the nouns and adjectives of peoples and countries
  (*Íslendingur*, *íslenskur*, *danskur*, *bandarískur*, *enskur*, *franskur*) and the names of
  languages (*íslenska*, *enska*, *danska*) count as names: skip them as band words even when the
  list has them as entries; a text may still use one (*hún talar íslensku*), counted at its rank.
  Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*guð*, *drottinn*), church and chapel (*kirkja*), priests
  (*prestur*), prayer, heaven and hell (*himnaríki*, *helvíti*), angels and devils (*engill*,
  *djöfull*, *fjandi*, *andskoti*), souls and spirits (*sál*, *andi*), Christianity (*kristinn*,
  *trú*), the old gods and their feasts (*Óðinn*, *Þór* as a god, *blót*, *þorrablót*), and every
  custom or holiday of religious origin, even as a date, a school break or a gift season: Christmas
  (*jól*, *aðfangadagur*, *jólasveinn*, the Yule Lads), Easter (*páskar*, *páskaegg*), Lent's days
  (*bolludagur*, *sprengidagur*, *öskudagur*), Whitsun (*hvítasunna*), confirmation
  (*ferming*), name days and saints' days. Folk customs with no church holiday named are fine: a
  birthday, New Year's Eve as a family evening with fireworks (*gamlárskvöld*, *flugeldar*), the
  First Day of Summer (*sumardagurinn fyrsti*), the sheep round-up (*réttir*), Seamen's Day
  (*sjómannadagurinn*), Culture Night in Reykjavík (*Menningarnótt*), the first day of school, a
  market, a town festival (*bæjarhátíð*), a song or a food festival. Never visit a church, and use
  *trúa* only as believing a person or a fact;
- **the supernatural**: elves and the hidden people (*álfar*, *huldufólk*), trolls (*tröll*),
  ghosts (*draugur*), witches and magic (*galdur*, *norn*), sea monsters, curses and miracles. The
  famous rocks and cliffs may be named, never as the home of elves or trolls;
- **politics**: the state (*ríki*), the government (*ríkisstjórn*, *stjórn*), parliament
  (*Alþingi*, *þing*, *þingmaður*), laws (*lög*), elections and voting (*kosning*, *kjósa*,
  *atkvæði*), parties (*flokkur*), committees and party chairmen (*nefnd*, *formaður*,
  *fulltrúi*), presidents, kings and queens (*forseti*, *konungur*), power (*vald*), the nation as a
  political word (*þjóð*), independence and the National Day (*17. júní*, *þjóðhátíðardagurinn*),
  the cod wars, the European Union, the bank crash, the army bases, flags and fishing quotas as a
  dispute; everyday civic life (a library, a school, the town's service desk, a residents'
  meeting, the swimming pool) is fine;
- **war, the army, weapons, violence, crime, police and courts** (*her*, *hermaður*, *stríð*,
  *vopn*, *byssa*, *sprengja*, *skot*, *árás*, *berjast*, *drepa*, *skjóta*, *myrða*, *stela*,
  *ræna*, *lögregla*, *lögga*, *handtaka*, *fangelsi*, *dómur*, *dómari*, *sök*), hunting and
  whaling (a lost-property office or a lifeguard at the pool is fine; theft, arrests, prisons and
  trials are not), and the Viking raids and the sagas' killings; the sea, the glaciers and the
  weather stay mild (a storm keeps the boats ashore and closes the road; nobody falls into a
  crevasse, drowns or is lost; a volcano is a view, never a danger);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee (*deyja*,
  *dauður*, *dauði*, *blóð*, *meiða*); an old person in a story is alive and well, and a pet does
  not die;
- **vulgar or sexual words**, and words whose main or common slang sense is sexual (*ríða* "to ride"
  is also vulgar slang: skip it and write *fara á hestbak*); insults (*fífl*, *hálfviti*, *asni*
  about a person, *heimskur* about a person); kissing, divorce and romance plots (*ást*,
  *kærasti*, *kærasta* as "boyfriend, girlfriend", *kyssa*); swearing (*helvíti*, *andskoti*,
  *djöfull*, *fjandinn*, *fjári* as oaths);
- **alcohol** in every form (beer *bjór*, wine *vín*, the schnapps *brennivín*, a bar visit for a
  drink, the fermented shark with a drink), **gambling** (lotteries, betting, casinos, *lottó*),
  drugs and smoking (*reykja*, *sígaretta*, *tóbak*);
- **deliberate misspellings**: no spoken forms, no spellings without *þ*, *ð*, *æ*, *ö* and the
  accents, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, an adjective or noun of a
  people or a country, or a language is skipped. Common Icelandic first names are fine as
  characters.

The themes keep to this: the history courses (*Áþreifanleg saga*, *Fornleifar og fortíðin*,
*Landkönnuðir og ferðalangar*) tell the history of everyday life, farms, towns, crafts, fishing,
trade, science and travel, never of wars, Viking raids, rulers, the church or politics;
*Bæjarhátíðir og markaðir* and *Alþýðulist og siðir* are folk and family celebrations with no
church holiday, no *þorrablót* and no *Þjóðhátíð* (a drinking festival) named; *Opinber þjónusta*
is the library, the health centre and the town's service desks, never politics; *Íslensk tunga*
and *Orð og uppruni þeirra* are about words, dialects, spelling and learning, never about language
politics; *Íslenskar bókmenntir* is about writers, books, reading and the saga manuscripts as
objects and stories of travel and farming, never their feuds and killings; *Ævintýri og þjóðsögur*
tells folk tales (clever animals, a poor boy who becomes a good farmer, the outlaw's hut as a
shelter on the moor) without elves, trolls, ghosts, magic or giants who kill.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion and swearing: *guð* 166, *fjandi* 178, *kirkja* 434, *helvíti* 520, *trú* 610,
  *andskoti* 669, *drottinn* 763, *fjári* 817, *jól* 840, *andi* 857 (spirit; the verb *anda* 999
  "breathe" is fine), *prestur* 925, *kristinn* 960, *djöfull* 973;
- politics: *stjórn* 225, *ríki* 261, *flokkur* 353, *lög* 380 (law; *lög* "songs" is *lag*'s, 74),
  *forseti* 391, *þjóð* 426, *konungur* 458, *fulltrúi* 500, *vald* 501, *formaður* 548,
  *ríkisstjórn* 569, *kjósa* 625, *þing* 700, *nefnd* 709, *þingmaður* 742, *kosning* 790,
  *atkvæði* 994;
- war, crime, police, courts, violence: *drepa* 160, *skjóta* 230, *berjast* 409, *her* 416,
  *lögregla* 449, *byssa* 476, *slá* 503 (hit; also "mow"), *stríð* 526, *fangelsi* 587, *verja*
  597, *stela* 611, *handtaka* 668, *sprengja* 670, *myrða* 685, *hermaður* 725, *árás* 729,
  *lögga* 737, *skot* 740, *stinga* 746, *vörður* 761, *vopn* 769, *ræna* 787, *dómur* 848,
  *dómari* 871 (also a sports referee: skip it), *eyðileggja* 886, *sök* 896, *flýja* 955;
- death and injury: *deyja* 184, *dauður* 511, *dauði* 665, *blóð* 798, *meiða* 884;
- vulgar, sexual, romance: *ríða* 643, *ást* 486;
- names, peoples, countries and languages: *íslenskur* 154, *anna* 693 (its forms are mostly the
  name *Anna*), *björn* 707 (bear; it holds the name *Bjarni*'s forms), *bandarískur* 713,
  *enskur* 907, *franskur* 919;
- spoken forms, subtitle words and list noise: *jæja* 266 (a filler; fine in dialogue, not worth a
  lesson), *herra* 157, *frú* 407, *ha* 593, *ókei* 604, *hey* 581, *náungi* 660, *gaur* 815,
  *sko* 830, *dama* 836, *ja* 894, *vá* 956, *fröken* 987;
- duplicates and forms of other words: *hugi* 645 is a second entry for *hugur* 465 (teach
  *hugur*); *lítt* 782 (literary); *mikið* 119 and *lítið* 363 are *mikill*'s and *lítill*'s
  neuters too: teach them as the adverbs "much, a lot" and "little" only if *mikill*, *lítill*
  are not taught in the same sense; *næsta* 560 is *næstur*'s (306), *heldri* 472 holds *helstu*,
  *helsta*, *helst* ("main, most"), *síður* 168 holds *síðasta*, *síðasti*, *síðustu* ("last") and
  *síðari* ("latter"): teach them as the list writes the headword, through those forms.

Further on, the same categories hold; for example *páskar*, *ferming*, *engill*, *sál*, *bæn*,
*álfur*, *huldufólk*, *tröll*, *draugur*, *bjór*, *vín*, *brennivín*, *reykja*, *sígaretta*,
*glæpur*, *þjófur*, *morð*, *jarðarför*, *gröf*, *fáni*, *Alþingi*, *kosningar*, *ráðherra*,
*veiðimaður*, *hvalveiðar*.

## The file

Write `tools/courses/is/<id>.json`:

```json
{
  "id": "is-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Icelandic, e.g. Matur og innkaup",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Icelandic",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/is/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *kom* or *komum* for *koma*, *barnið* or *börnin* for
  *barn*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  the infinitive of a verb (*koma*, *fara*, *gerast*), the nominative singular of a noun (*barn*,
  *kona*) or the plural where the list has it (*foreldrar*), the masculine nominative singular of
  an adjective (*góður*, *nýr*), as the band file has it.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name or an English word,
  a form of another word, an adjective of a people, a spoken form, a word of the content rules);
  the checker's last line lists the band words not taught yet.

Things to know about the Icelandic list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/is/wordlist.tsv`
  (a copy of the app's `is.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  film subtitles (FrequencyWords, OpenSubtitles 2018) blended with wordfreq, so talk words rank
  high and some everyday written words rank low; war, crime, police and swearing words rank high
  and are skipped. Its forms come from BÍN, the Árni Magnússon Institute's Database of Modern
  Icelandic Inflection, so every word carries its own inflection: the cases with and without the
  article, the verb's persons, tenses, moods and participles, the adjective's genders, cases and
  degrees.
- **How the checker reads an Icelandic word.** A word is read as it is written, letter for letter
  (a hyphen cuts it in two). `extra_forms.tsv` holds the standard forms BÍN gives a list word that
  the list lacks (the list keeps at most 40 forms per word); it is made by
  `/Users/zavanton/TabbyProjects/tayra-writers/is-writers/make_extra_forms.py`. A name in `names`
  counts in its regular case forms too (*Sara*, *Söru*; *Jóhanna*, *Jóhönnu*; *Jón*, *Jóni*, *Jóns*; *Guðrún*,
  *Guðrúnu*, *Guðrúnar*; *Sigurður*, *Sigurð*, *Sigurði*, *Sigurðar*; *Pétur*, *Pétri*, *Péturs*;
  *Bjarni*, *Bjarna*; *Páll*, *Pál*, *Páls*; *Reykjavík*, *Reykjavíkur*; *Akureyri*,
  *Akureyrar*; *Selfoss*, *Selfossi*; *Egilsstaðir*, *Egilsstöðum*; *Vestmannaeyjar*,
  *Vestmannaeyjum*; *Hrísey*, *Hríseyjar*; *Jónsdóttir*, *Jónsdóttur*; *Jónsson*, *Jónssyni*),
  when the form is not itself a list word. A name whose stem changes more (*Hafnarfjörður*,
  *Hafnarfirði*, *Hafnarfjarðar*; *Ísafjörður*, *Ísafirði*; *Danmörk*, *Danmörku*, *Danmerkur*;
  *Ingibjörg*, *Ingibjargar*; *Hörður*, *Herði*, *Harðar*) goes into `names` in each form you use.
  A name of several words or with a hyphen (*Bláa lónið*, *Gullni hringurinn*) counts as one word
  wherever it is written whole; its case forms count by their parts.
- **Pronouns and little words**: *ég* 5 holds *mig*, *mér*, *mín*; *þú* 7 holds *þig*, *þér*,
  *þín*; *við* 8 is "we" (*okkur*, *okkar*) and the preposition "by, at, with" in one entry; *þið*
  52 holds *ykkur*, *ykkar*; *hann* 12, *hún* 21, *það* 9 are the singular, and *þeir* 36, *þær*
  98, *þau* 70 the plural, each with *þeim*, *þeirra*; *sig* 72 holds *sér*, *sín*; *þessi* 13
  holds *þetta*, *þessu*, *þennan*; *sá* 76 is the demonstrative (*sá*, *sú*, *þann*, *sá sem*
  "the one who"), while *sá* "saw" counts for *sjá* 30 and *sáð* for "sow"; *hinn* 80 is "the
  other" (*hinn*, *hin*, *hitt*); *allur* 32 holds *allir*, *alla*, *öll*, *öllum*, and *allt* 51
  "everything" is an entry of its own; *margur* 73 holds *margir*, *margt*, *fleiri*, *flestir*;
  *einn* 39 holds *eitt*, *ein*, *einu*; *tveir* 86 holds *tvö*, *tvær*, *tveimur*, *tveggja*;
  *samur* 109 holds *sama*, *sami*, *sömu* ("same"); *eiginn* 358 holds *eigin* ("own").
  Adverbs and prepositions are entries apart: *heim* 200 and *heima* 420 (but both also count as
  *heimur* 167's forms, so they read as rank 167), *út* 63, *úti* 399, *inn* 93, *inni* 338,
  *upp* 66, *uppi* 375, *niður* 151, *niðri* 724, *fram* 88, *frammi* 921, *aftur* 83, *saman*
  107, *heldur* 199, *samt* 294, *mikið* 119, *lítið* 363.
- **Words filed under a commoner word spelled the same**, which the checker counts at that word's
  rank, so they look known early; they are never band words at that rank, and below rank 1000 use
  them only where the story makes their meaning plain: *búinn* "finished, done" is *búa* 99's
  participle (*ég er búinn að borða*), *kominn* and *farinn* are *koma*'s and *fara*'s; *á*
  "river" is filed with the preposition *á* 6 (*ána*, *ánni*; *ár* "rivers" and "oars" also count
  for *ár* "year" 46); *á* "owns" is *eiga* 53's; *vinnu*, *vinnuna*, *vinnan* "work, job" are
  *vinna* 87's; *tölu*, *tölur* "number" are *tala* 89's; *líkar*, *líkaði* "likes" are *líka* 111's
  ("also"); *hætta* 113 is "to stop" and "danger" (*hættan*); *heim*, *heima* also count for
  *heimur* 167; *staða* 527 shares forms with *staður* 96 (*stöðu*, *stöð*); *mun*, *muni*,
  *munum* are *munu* 84's and *munur* 344's; *leið* 140 "way" holds *leiddi* (*leiða* "lead"),
  and *leiður* 340 "sad" holds *leiðum*, *leiða*; *lag* 74 holds *lagi* (*allt í lagi*) and
  *lög* "songs"; *bera* 194 holds *bara* and *barnum*; *sumir* 267 holds *sumar* "some", and
  *sumar* 470 is "summer"; *vegur* 162 "road", *vega* "weigh" and *vegna* 103 "because of" share
  *vegna*, *vega*; *átt* 323 "direction" holds *áttum*, *áttir* (also *eiga*'s past); *átta* 394 is
  "eight" and *átta sig* "realise"; *vona* 895 and *von* 326 "hope" share *vona*, *vonum*; *sæti*
  290 "seat" and *sætur* 496 "sweet" share *sæti*; *dýr* 481 is "animal" and "expensive"; *fé*
  911 is "money" and "sheep"; *bær* 259 is "town" and "farm"; *hár* 278 is "high" and "hair";
  *kanna* 578 "explore" holds *könnu* "jug"; *plata* 637 is "record" and "to trick"; *baka* 664
  "bake" holds *böku* "pie" and shares *baka* with *bak* 350 "back". Use a new word in its own
  sense, never through its homograph, and count a new word's uses in forms that are its own
  (*hætta* "stop" through *hættu*, *hætti*; *sumar* "summer" through *sumarið*, *sumrin*).
- **Headwords as BÍN writes them**: some entries are headed by BÍN's citation form, not the one a
  learner expects; write `newWords` as the list writes them and teach the word through its usual
  forms: *samur* 109 (*sama*, *sami*, *sömu*), *eiginn* 358 (*eigin*), *áhyggja* 393 (*áhyggjur*),
  *upplýsing* 441 (*upplýsingar*), *síður* 168 (*síðasti*, *síðasta*, *síðari*), *heldri* 472
  (*helstu*, *helsta*), *tiltaka* 979 (*tiltekinn*, *tiltekið* "specific"). A verb's participles are
  filed under it (*búinn* under *búa*, *kominn* under *koma*, *ákveðinn* also under *ákveða*), and
  so are an adjective's comparative and superlative (*betri*, *bestur* of *góður*; *stærri* of
  *stór*); they are no band words of their own, except where the list has them as entries
  (*tilbúinn* 303, *ákveðinn* 1100, *fyrstur* 117, *næstur* 306).
- **Names** are not in the list: people, places (*Reykjavík*, *Akureyri*, *Ísland*), shops and
  brands go in `names`. Keep only true names in `names`, never common words, and never a name that
  is also a list word or whose case forms are list words. The list has many Icelandic names as
  words, because so many names are words: as characters avoid *Anna* (*anna* 693), *Björn*,
  *Bjarni* (*Bjarna* is *björn*'s), *Einar* (*einn*'s form), *Helga* (*helgi*), *Gunnar*, *Páll*
  (*páll* "spade"), *Arnar* and *Örn* (*örn* "eagle"), *Óskar* (*óska*), *Ásta* (*ást*), *Dagur*,
  *Sól*, *Von*, *Sigur*, *Steinn*, *Hrafn*, *Rún*, *Rúnar*, *Ari*, *Unnur*, *Hildur*, *Lilja*,
  *Edda*, *Brynja*, *Ragnar*, *Hanna*, *Halla*, *Hulda*, *Bára*, *Logi*, *Bragi*, *Gylfi*, *Björk*,
  *Auður*, *Birna*, *Erla*, *Rósa*, *Hrefna*, *Snorri*, *Andri*, *Sóley*, *Hekla*, *Elding*... and
  the place *Vík* (*víkja*). Check every name with `rank.py` before you use it (`rank.py --names
  Jón,Guðrún Jóni Guðrúnu` also shows the case forms the checker gives the names). Safe in the
  pilot: *Jón*, *Guðrún*, *Pétur*, *Sara*, *Kristín*, *Ólafur*, *Jóhann*, *Davíð*, *Stefán*,
  *Freyja*, *Eva*, *Sigríður*, *Magnús*, *Tómas*, *Inga*, *Elín*, *Kristján*, *Katrín*, *Daníel*,
  *Þóra*, *Þórdís*, *Guðmundur*, *Emil*, *Ásdís*, *Sigurður*, *Agnes*, *Halldóra*, *Jónas*,
  *Embla*, *Jóhanna*, *Haraldur*, *Védís*, *Aron*, *Sigrún*, *Hákon*, *Rakel*, *Viktor*, and
  further *Margrét*, *Ingibjörg*, *Steinunn*, *Þórunn*, *Bryndís*, *Valdís*, *Telma*, *Nanna*,
  *Dóra*, *Elísabet*, *Ragnheiður*, *Svanhildur*, *Eiríkur*, *Þorsteinn*, *Gísli*,
  *Ásgeir*, *Benedikt*, *Baldur*, *Atli*, *Matthías*, *Kári*, *Birkir*, *Bjarki*, *Hörður*;
  *Reykjavík*, *Akureyri*, *Kópavogur*, *Hafnarfjörður*, *Selfoss*, *Húsavík*, *Akranes*,
  *Borgarnes*, *Egilsstaðir*, *Ísafjörður*, *Vestmannaeyjar*, *Mývatn*, *Ísland*. Day and
  month names are in the list but rank late (*sunnudagur* 1426, *föstudagur* 1475, *laugardagur*
  1562, *mánudagur* 1676, *fimmtudagur* 2123, *þriðjudagur* 2804, *miðvikudagur* 3621; *maí* 498,
  *júní* 531, *nóvember* 534, *september* 543, *mars* 545, *október* 553), and so are the seasons
  (*sumar* 470, *vor* 624, *vetur* 914, *haust* 1002).
- Everyday words rank later than you would think: *kona* 108, *barn* 125, *takk* 159, *vinur* 158,
  *mamma* 171, *hús* 176, *pabbi* 177, *kvöld* 180, *bíll* 202, *sonur* 206, *vika* 207, *bók*
  215, *morgunn* 229, *skóli* 246, *matur* 324, *hæ* 349, *bróðir* 376, *herbergi* 404, *systir*
  459, *hundur* 462, *dóttir* 469, *kaffi* 768, *amma* 901, *afi* 932, *köttur* 1152, *eldhús*
  1390, *mjólk* 1829, *brauð* 1909; *heim* and *heima* count at 167, and *góðan daginn* is first
  band words (*góður* 42, *dagur* 71). The dialogue tag *segja* 29 is in the first band, *spyrja*
  224 and *svara* 361 come later.
- The list's form lists are noisy (look-alikes, homographs, subtitle spellings); a new word must
  appear as itself or a true inflection, never through such a form. Some entries are names,
  English words or interjections: skip them. Loanwords Icelandic uses as its own are taught like
  any word, but Icelandic prefers its own words (*sími*, *tölva*, *sjónvarp*, *bíll*): write them.
