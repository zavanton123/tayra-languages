# Writing an Irish frequency course

Each course teaches one band of 100 words from the Irish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ga-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Níl a fhón ag Tadhg. Níl sé sa mhála. Níl sé ar an mbord...* Modern
  everyday life where Irish is spoken: the Gaeltacht (Conamara and Árainn, Corca Dhuibhne, Gaoth
  Dobhair and the rest of Donegal, Múscraí, An Rinn, Ráth Cairn), the cities and towns (Gaillimh,
  Corcaigh, Luimneach, Baile Átha Cliath, Port Láirge, Cill Airne, Leitir Ceanainn), Irish-medium
  schools (*Gaelscoil*) and families who speak Irish at home anywhere in Ireland; and now and then
  Londain, Boston, Toronto or Sydney, where Irish families live and speak Irish at home: home,
  family, food, shopping, work, school, the town, health, free time, travel. (Most Irish place
  names are made of list words: see "Names" below.)
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *sé*/*sí* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Seáns in
  two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, in Irish order: verb,
  subject, object (*D'ól Máire tae.* *Tá Tadhg sa bhaile.* *Chonaic sí an madra.*). Little
  dialogue; when there is some, short and simple. Irish has no polite "you": *tú* to one person,
  *sibh* to several, whoever they are; respect shows in the words (*le do thoil*, *go raibh maith
  agat*, *a Mháire*, *a Bhean Uí Bhriain*), not in the pronoun. Address people in the vocative with
  *a* and lenition (*a Sheáin*, *a Mháire*, *a Phádraig*, *a Mhamaí*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *tá* / *níl* / *an bhfuil?* / *bhí* / *beidh* (*bí*) for where, how and what is
    going on (*Tá sí sa chistin.* *Tá sé fuar.*), and the progressive *ag* + verbal noun (*Tá sé
    ag ól tae.* *Bhí siad ag obair.*); the copula *is* for what someone or something is (*Is
    múinteoir í.* *Is madra é.* *Is maith liom tae.* *Ba mhaith liom...*); the present of regular
    verbs (*ólann*, *itheann*, *téann*, *feiceann*), the past (*d'ól*, *chuaigh*, *rinne*, *fuair*,
    *chonaic*, *dúirt*, *tháinig*, *thug*), with *ní*/*níor* and *an*/*ar* in questions (*Ní fhaca
    sé é.* *Ar ith tú?* *An dtéann sí?*), yes and no by repeating the verb (*An bhfuil tú
    tuirseach? – Tá.* *Ar ith tú? – D'ith.*); "having" as *tá X ag Y* (*Tá madra ag Úna.*), feelings
    as *tá X ar Y* (*Tá ocras orm.*); the prepositional pronouns (*agam*, *liom*, *orm*, *dom*,
    *uaim*); the article and its mutations (*an fear*, *an bhean*, *an t-uisce*, *ar an mbord*,
    *sa bhaile*, *na daoine*); possessives with their mutations (*mo theach*, *do mhála*, *a
    theach* "his", *a teach* "her", *a hathair* "her father", *ár dteach*); questions with *cá*,
    *cad*, *cé*, *conas*, *cathain*, *cén*; numbers with their nouns (*dhá chupán*, *trí bhosca*,
    *seacht mbosca*);
  - ranks 301–600: the future (*beidh*, *rachaidh*, *tiocfaidh*, *íosfaidh*, *ólfaidh*) beside
    *tá sé chun* + verbal noun; the imperative (*suigh*, *tar*, *féach*, *éist*, *ná déan*,
    *déanaigí*); the habitual *bíonn* (*Bíonn sé tuirseach gach oíche.*); comparatives (*níos mó
    ná*, *níos fearr ná*, *chomh mór le*); *is féidir le*, *caithfidh*, *ní mór do*; the genitive in
    plain phrases (*doras an tí*, *cupán tae*, *bean an tí*, *lár an bhaile*); the time (*a trí a
    chlog*, *ar maidin*, *tráthnóna*), days and months (names: see below);
  - ranks 601–1000: the conditional (*ba mhaith liom*, *rachainn*, *dhéanfadh sé*, *bheadh*);
    the direct and indirect relative (*an fear a chonaic mé*, *an áit a bhfuil sé*, *an bhean a
    bhfuil a mac anseo*); reported speech with *go* / *nach* (*Deir sí go bhfuil sí tuirseach.*
    *Dúirt sé nach raibh ocras air.*); *nuair a*, *má* + present or future for real plans (*Má
    bhíonn sé fliuch, fanfaimid sa bhaile.*); *mar* "because", *ach*, *agus*, *ansin*;
  - ranks 1001–1300: the past habitual (*bhíodh*, *théadh sé*, *d'itheadh sí*), superlatives
    (*an fear is airde*, *an ceann is fearr*), verbal adjectives (*dúnta*, *déanta*, *briste*),
    *tar éis* + verbal noun for what has just happened (*Tá sé tar éis ithe.*), *chun* / *le* +
    verbal noun for purpose;
  - ranks 1301–1600: the autonomous verb (*deirtear*, *rinneadh*, *tógadh an teach in 1990*,
    *díoltar arán anseo*), *dá* + conditional for what is not so (*Dá mbeadh am agam, rachainn
    ann.*), *mura*, *cé go*, *sula*, *ionas go*; the genitive plural (*hata na mban*, *teach na
    bhfear*);
  - ranks 1601–2000: verbal-noun clauses with the object before them (*Ba mhaith liom an leabhar
    a léamh.* *Tá orm an carr a ghlanadh.*), the copula's past and conditional with its mutations
    (*ba bhreá liom*, *b'fhearr liom*, *ab fhearr*), the subjunctive in set wishes (*go n-éirí
    leat*, *go raibh maith agat*), indirect questions (*Níl a fhios aici an bhfuil sé ann.*);
  - ranks 2001–2500: a fuller written style, with clauses inside one another, the genitive after a
    verbal noun (*ag déanamh na hoibre*, *ag ól an bhainne*), reported speech with tense shifts
    (*Dúirt sé go mbeadh sé ann.*), *de bharr*, *mar gheall ar*, *de réir*, *maidir le*, *i rith*;
  - ranks 2501–3000: a richer, more formal register: connectors such as *áfach*, *mar sin féin*,
    *dá bhrí sin*, *is é sin le rá*, *ar an láimh eile*; common proverbs (*seanfhocail*) and idioms
    explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*colún*, a light reflection on everyday life), dialogue
    that marks the vocative and the plural *sibh*, and less common connectors (*ar aon nós*, *ina
    ainneoin sin*, *dá réir sin*, *ar an gcéad dul síos*);
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
  After rank 1000 sentences get longer and joined (*mar*, *nuair a*, *cé go*, *agus*, *ach*,
  *toisc go*, *ionas go*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a lesson may also
  be a short informative or opinion text (a column, a letter to a newspaper, a short report), as
  long as it is everyday, calm and easy to follow. Still one clear idea at a time: a learner at the
  level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Níl sé sa mhála. Níl sé ar an mbord. Níl sé sa chistin.*).
- **Questions at the end**: after the story, a paragraph starting `Ceisteanna:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *cén fáth* and *conas*, with fuller
  answers):

  ```
  Ceisteanna:
  Cad atá ag teastáil ó Thadhg?
  Tá a fhón ag teastáil ó Thadhg.
  Cá bhfuil an fón?
  Tá an fón sa charr.
  ```

  The header `Ceisteanna:` is layout: the checker does not count it as a word.
- **Standard Irish**: the narration is the standard written language, *An Caighdeán Oifigiúil*
  (the 2017 revision): neutral, correct and natural Irish as written today, with the Caighdeán's
  spelling and forms and no dialect forms standing in for them. Its verb forms: the first person
  singular and plural of the present and future are synthetic (*ólaim*, *téim*, *feicim*,
  *ólaimid*, *rachaimid*), and so are the past first plural (*d'ólamar*, *chuamar*,
  *chonaiceamar*), the conditional's first and second singular and first and third plural
  (*rachainn*, *rachfá*, *rachaimis*, *rachaidís*); the other persons are analytic (*d'ól mé*, *ólann
  tú*, *chuaigh siad*), never the Munster or Connacht synthetic forms (*d'ólas*, *chuadar*,
  *bhíodar*, *deinim*, *dheineas*) or the Ulster *cha*, *char*, *goidé*. *tá mé* and *táim* are
  both standard: keep to one in a story. Write *muid* or *sinn* as the Caighdeán has them (*bhí muid*
  is fine; *chuamar* for "we went"). **The mutations are grammar, not spelling**: write every
  lenition, eclipsis, *h-* and *t-* that the standard requires and none that it does not (*ar an
  mbord*, not the Ulster *ar an bhord*; *sa bhaile*, *sa siopa* (no lenition of *s*, *d*, *t*
  after *sa*); *an t-am* for a masculine noun in a vowel, *an tsráid* for a feminine noun in *s*; *mo
  chara* but *m'athair*; *dhá bhosca*, *seacht mbosca*). The standard hyphen: *t-* and *n-* before a
  lowercase vowel (*an t-uisce*, *ár n-athair*), none before a capital (*i nGaillimh*, *na
  hÉireann*, *an tUachtarán*), and *h* with no hyphen (*a hathair*, *le haghaidh*). Always write
  the *síneadh fada*: the checker compares spellings exactly, and the accent tells words apart
  (*sé* "he" / *se* is no word, *Seán* / *sean* "old", *fear* "man" / *féar* "grass", *cead*
  "permission" / *céad* "hundred", *áit* "place" / *ait* "strange", *bás* / *bas*, *cáca*, *mín* /
  *min*). Never the pre-1945 spelling (*sgoil*, *sgéal*, *Gaedhilge*, *-aidh* for *-aí*): the list
  keeps such spellings as forms of their words, and they never count as the word. No slang, no
  English mixed into a sentence (*Bhí sé so cool*, *okay*), no text-speak. Dialogue may be lightly
  colloquial only in what any careful speaker says (*ceart go leor*, *go hiontach*, *maith go
  leor*, *ar fheabhas*, *haigh*, *slán*).
- **Irish typography**: direct speech in curly double quotation marks “…”, each speaker's line a
  paragraph of its own; the comma, question mark or exclamation mark of the quoted sentence inside
  the marks, the tag after it (*a deir*, *arsa*, *a dúirt*, *a d'fhiafraigh*):

  ```
  “An bhfuil ocras ort?” a deir Máire.
  “Tá,” a deir Tadhg. “Tá ocras an domhain orm.”
  ```

  The questions and answers of the final `Ceisteanna:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); a hyphen stays a hyphen in the words that have
  it (*an t-uisce*, *ár n-athair*, *nua-aimseartha*). The apostrophe is the straight one or the
  curly one, either: *d'ól*, *b'fhéidir*, *m'athair*. Write numbers as digits (*3 bhosca*, *ag a 7 a
  chlog*, *in 1995*, *€5*; an ordinal as *an 3ú lá*: the checker reads the letters after the
  digits as part of the number), except a number word that is in your band: spell that one out to
  teach it (*dhá chupán*, *triúr páistí*).
- **Names are names**: place names, words for peoples and languages (*Gaeilge*, *Béarla*,
  *Éireannach*, *Francach*, *Sasanach*, *Meiriceánach*) count as names: skip them as band words even
  when the list has them as entries; a text may still use one (*Labhraíonn sí Béarla*), counted at
  its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God (*Dia*), saints (*naomh*), church and chapel (*eaglais*,
  *séipéal*), priests and nuns, monasteries, prayer, Mass, heaven and hell, angels and devils,
  souls and spirits, and every custom or holiday of religious origin, even as a date, a school
  break or a gift season: Christmas (*Nollaig*, *Oíche Nollag*), Easter (*Cáisc*), St Patrick's
  Day (*Lá Fhéile Pádraig*: a saint's day and a national day), St Brigid's Day (*Lá Fhéile
  Bríde*, the crosses), Halloween (*Oíche Shamhna*, *Samhain*), First Communion and Confirmation,
  holy wells and patterns, and the greetings and sayings that name God or the saints (*Dia duit*,
  *Dia is Muire duit*, *buíochas le Dia*, *le cúnamh Dé*, *go sábhála Dia sinn*): greet with
  *haigh*, *maidin mhaith*, *conas atá tú?*, *cén chaoi a bhfuil tú?*. New Year's Eve (*Oíche Chinn
  Bliana*) is fine only as a family evening. Folk customs with no church holiday or saint named are
  fine: a *céilí* or *seisiún* of music and dance (not in a pub), a *feis* or *fleadh cheoil*,
  Gaelic games (*peil*, *iománaíocht*, *camógaíocht*, *liathróid láimhe*), a market, a fair, a
  birthday, a wedding told as family news (no church), the start of the school year, the summer
  holidays and the Gaeltacht summer college (*coláiste samhraidh*). Use *creid* only as believing
  a person or a fact;
- **politics**: the state and the government (*stát*, *rialtas*, *an Dáil*, *Taoiseach*,
  *Uachtarán*, *aire* as a minister), parliament, elections, parties, kings and rulers,
  nationalism, independence, flags, the border and the North as a question, the Irish language as
  a political question (language rights, the *Acht Teanga*), the Gaeltacht's boundaries; everyday
  civic life (a library, a school, the council office's counter, a residents' meeting, a local
  club) is fine;
- **war, the army, weapons, violence, crime, police and courts** (*cogadh*, *arm*, *saighdiúir*,
  *gardaí* as police, *príosún*, *cúirt*), and the Famine, the Troubles and every war of Irish
  history (a lost-property counter or a lollipop lady at a school is fine; theft, arrests, prisons
  and trials are not); the sea's dangers stay mild (a storm keeps the boats in the harbour; nobody
  is lost at sea);
- **death**, killing, funerals and wakes (*tórramh*), graves, blood and injury beyond a scraped
  knee; an old person in a story is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, words whose main sense is sexual, and kissing, courtship
  gone wrong, separation and divorce;
- **alcohol** in every form (beer, whiskey, *poitín*, the pub as a place to drink, a toast with
  *sláinte*), **gambling** (betting on horses or matches, the lotto, casinos), drugs and smoking;
- **deliberate misspellings**: no spellings without the *fada*, no old or dialect spellings, not
  even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a word for a people or
  a language is skipped. Common Irish first names are fine as characters.

The themes keep to this: the history courses (*Stair ar leac an dorais*, *Seandálaíocht agus an
t-am atá thart*, *Taiscéalaithe agus taistealaithe*) tell the history of everyday life, towns,
crafts, trade, science and travel, never of wars, rulers, the Famine or politics; *Féilte pobail
agus aonaigh* and *Ealaín agus nósanna an phobail* are folk, music, sport and family celebrations
with no church holiday or saint named; *Seirbhísí poiblí* is the library, the health centre and the
council's counters, never politics; *An Ghaeilge* and *Focail agus a mbunús* are about words,
dialects, the Gaeltacht as a place where people live and learning, never about language politics
or language rights; *Scéalta béaloidis agus finscéalta* tells folk tales (Fionn mac Cumhaill and
the giants, the salmon of knowledge, clever animals) without fairies, the *bean sí*, ghosts, saints
or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *dia* 224 (it holds *Dé* of *Dé Luain*: that use is fine), *naomh* 285, *eaglais*
  347, *paróiste* 582, *mainistir* 701, *creideamh* 747, *sagart* 811, *pápa* 838; *cill* 190 (a
  church, and the *Cill* of place names); *creid* 434 only as believing a person or a fact;
- politics: *rí* 113, *stát* 116, *aontaithe* 156 (*na Stáit Aontaithe*, *na Náisiúin
  Aontaithe*), *rialtas* 181, *páirtí* 206, *dlí* 213, *polaiteoir* 245, *ríocht* 262,
  *náisiúnta* 287, *uachtarán* 330, *cumhacht* 333, *ceannas* 334, *conradh* 335, *aontas* 389,
  *gluaiseacht* 407, *poblacht* 410, *polaitiúil* 412, *aire* 413 only as "care, attention" (*aire
  a thabhairt do*), *toghadh* 452, *parlaimint* 471, *teorainn* 478 only as a limit, *ceannaire*
  517, *tiarna* 532, *údarás* 549, *toghchán* 566, *polaitíocht* 648, *bunreacht* 683, *impireacht*
  687, *neamhspleách* 691, *dáil* 723, *feachtas* 736, *saoirse* 756, *neamhspleáchas* 759,
  *riarachán* 760, *vóta* 804, *coimisiún* 807, *réabhlóid* 818, *náisiún* 825, *acht* 842,
  *daonlathach* 855, *ríoga* 858, *príomh-aire* 883, *coróin* 936, *tionól* 949, *taoiseach* 971,
  *polasaí* 987;
- war, crime, police, courts: *cogadh* 119, *maraigh* 281, *arm* 304, *cath* 307, *fórsa* 371,
  *cúirt* 428, *troid* 508, *marú* 563, *saighdiúir* 574, *míleata* 613, *gortaigh* 641, *ionsaí*
  695, *triail* 713 only as a test or a try, *scrios* 795, *príosún* 908, *ionsaigh* 939, *óglach*
  958; *cosaint* 364 only as protection (from the rain, the sun);
- death: *bás* 94, *marbh* 889, *éag* 899, *fuil* 835;
- alcohol: *beoir* 968; *ól* 200 and *deoch* 998 only for tea, water, milk, juice;
- names, peoples, languages, pieces of place names: *béarla* 152, *áth* 164 and *cliath* 196 (*Baile
  Átha Cliath*), *sóivéadach* 609, *francach* 652, *aodh* 662, *doire* 764, *gall* 770, *rúise*
  799, *ros* 814, *eo* 872, *feirste* 885, *corcach* 895, *cine* 956;
- list noise and forms of other words: *thú* 288 (the object form of *tú*: use it, never teach
  it), *hé* 575 (*é* after *h*), *chóir* 580 (*cóir* lenited, as in *mar is cóir*), *déanta* 359,
  *scríofa* 581, *fágtha* 684 and *caite* 847 (participles of *déan*, *scríobh*, *fág*, *caith*:
  teach the verbs), *dúchais* 704 (the genitive of *dúchas*).

Further on, the same categories hold; for example *Nollaig*, *Cáisc*, *séipéal*, *paidir*,
*aifreann*, *anam*, *aingeal*, *diabhal*, *ifreann*, *neamh* as heaven, *sí* as a fairy, *tórramh*,
*sochraid*, *uaigh*, *reilig*, *garda* as police, *gadaí*, *goid*, *breitheamh*, *fuisce*,
*pórtar*, *poitín*, *tábhairne*, *teach tábhairne*, *toitín*, *tobac*, *geall* as a bet, *bratach*,
*Dáil*, *Seanad*, *toghchán*.

## The file

Write `tools/courses/ga/<id>.json`:

```json
{
  "id": "ga-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Irish, e.g. Bia agus siopadóireacht",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Irish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ga/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts, mutated too: *chuaigh*, *rachaidh* or *dtéann* for
  *téigh*; *bhean* or *mná* for *bean*; *mhór* for *mór*). Reuse earlier lessons' new words too.
  `newWords` holds the word as the list writes it (the third column of the band file): a verb as
  its imperative singular (*téigh*, *déan*, *ceannaigh*), a noun in its nominative singular, an
  adjective in its plain form, a verbal noun as itself (*dul*, *déanamh*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a form of another word, a list error such as a name, a piece of a place name or
  an English word, a word for a people or a language, a word of the content rules); the checker's
  last line lists the band words not taught yet.

Things to know about the Irish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ga/wordlist.tsv`
  (a copy of the app's `ga.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from Irish
  Wikipedia (two thirds) and everyday sentences (Tatoeba and Common Voice), so encyclopaedia words
  rank high (*contae* 72, *bunaigh* 120, *cogadh* 119, *foilsigh* 183, *daonra* 282, *speiceas*
  386) while kitchen and household words rank low (*bia* 248, *tae* 661, *caife* 707, *maidin* 742,
  *cistin* 1384, *leaba* 1623, *ubh* 1815, *bricfeasta* 7808).
- **How the checker reads words: the mutations.** A mutated form the list lacks counts as the word
  it mutates: lenition (*a chat* is *cat*, *d'fhéach* is *féach*), eclipsis (*ar an mbord* is
  *bord*, *a gcarr* is *carr*, *i bhfad* is *fad*), *h* before a vowel (*a haois* is *aois*), *t*
  (*an t-uisce*, *an tsráid*), *d'*, *b'*, *m'* (*d'ól*, *m'athair*); so is a name (*a Mháire* is
  *Máire*, *i nGaillimh* is *Gaillimh*, *ó Chorcaigh* is *Corcaigh*). Most mutated forms are in the
  list anyway (*bhean*, *mbaile*, *t-am*). A capital after the mutation (*nÉirinn*, *tUachtarán*,
  *bPáras*) always marks a mutation. The checker does not check that your mutations are right:
  that is your job, by the standard. A word with a hyphen is one word where the list has it
  (*príomh-aire*, *nua-aimseartha*, *an t-uisce*, *ár n-athair*) and its parts otherwise
  (*sean-nós* is one entry, *sean-bhean* is *sean* + *bean*).
- **Pronouns and little words are entries of their own**: *mé* 15 (with *mise*), *tú* 25 (*tusa*;
  *thú* 288 is an entry of its own), *sé* 12, *sí* 32, *é* 10, *í* 30, *muid* 314, *sinn* 1000,
  *sibh* 242, *siad* 43, *iad* 48. The prepositional pronouns are forms of their prepositions:
  *agam*, *agat*, *aige*, *aici*, *againn*, *acu* of *ag* 7; *orm*, *ort*, *air*, *uirthi* of *ar*
  5; *liom*, *leis*, *léi*, *leo* of *le* 9; *dom*, *duit*, *dó*, *di* of *do* 16 (*dó* also of *de*
  13); *uaim* of *ó* 14; *fúm* of *faoi* 34; *chugam* of *chuig* 153; *romham* of *roimh* 123;
  *tharam* of *thar* 145; *tríd* of *trí* 63; *ionam*, *ionat*, *ann* ("in him, in it", and "there":
  *tá X ann*, *in ann*), *inti*, *ionainn*, *ionaibh*, *iontu* of *i* 4 ("in"), which also holds
  *in* (before a vowel: *in Éirinn*, *in aice*), *sa*, *san* and *sna* ("in the") and *inár* ("in
  our"). *bí* 2 holds *tá*, *bhí*, *bhfuil*, *raibh*, *níl*, *beidh*, *bíonn*, *bheadh*; *is* 6
  holds the copula's *ba* and *ab* (*ba* "cows" too: never count it as *bó*). *an* 1 holds *na*; *a*
  3 is the particle, the possessive and the relative. *don* 55, *lena* 135, *ina* 22 ("in his, her,
  their", and "in which"), *faoina* 726, *dá* 41 (with *dhá* "two"), *gur* 52, *gurb* 430, *gurbh*
  340, *nár* 276, *más* 456 are entries of their own.
- **Verbs.** The list heads a verb with its imperative singular and files its forms under it
  (*téigh*: *chuaigh*, *téann*, *rachaidh*, *ndeachaigh*); the irregulars too: *abair* 86 (*dúirt*,
  *deir*), *feic* 90 (*chonaic*, *feiceann*, *faca*), *tabhair* 28 (*thug*), *déan* 35 (*rinne*,
  *ndearna*), *tar* 36 (*tháinig*, *tagann*), *faigh* 66 (*fuair*, *gheobhaidh*), *beir* 111
  (*rug*), *clois* 620 (*chuala*), *ith* 220 (*d'ith*, *íosfaidh*). The **verbal noun** is an entry
  of its own, a word to teach in its own right: *dul* 76, *teacht* 140, *déanamh* 59, *fáil* 105,
  *rá* 230, *cur* 58, *baint* 141, *ithe* 193, *labhairt* 169, *léamh* 293, *feiceáil* 299, *siúl*
  339, *imirt* 489, *tógáil* 504, *fágáil* 912 (*ól*, *fás*, *rith*, *snámh*, *díol* are verb and
  verbal noun in one entry). *cuireadh* 127 is "an invitation" (and the autonomous past of *cuir*):
  teach it as the noun.
- **Words in set phrases.** Many entries live inside a phrase: teach them in it — *dtí* 81 (*go
  dtí*), *cónaí* 79 (*ina chónaí*, *i gcónaí*), *diaidh* 98 (*i ndiaidh*), *éis* 122 (*tar éis*),
  *réir* 112 (*de réir*), *aghaidh* 106 (*ar aghaidh*, *le haghaidh*), *iarraidh* 108 (*ag iarraidh*),
  *measc* 174 (*i measc*), *feadh* 271 (*ar feadh*), *ais* 272 (*ar ais*), *coinne* 317 (*i
  gcoinne*), *os* 323 (*os cionn*, *os comhair*), *láthair* 325 (*i láthair*), *aice* 480 (*in aice
  le*), *comhair* 492 (*os comhair*), *son* 486 (*ar son*), *aonar* 633 (*ina aonar*), *éineacht* 752
  (*in éineacht le*), *fóill* 766 (*go fóill*), *iúl* 682 (*a chur in iúl*), *bith* 142 (*ar bith*),
  *dála* 990 (*dála an scéil*), *fios* 103 (*tá a fhios agam*), *aithne* 214 (*tá aithne agam ar*),
  *féidir* 50 (*is féidir*), *leor* 132 (*go leor*), *ea* 53 (*is ea*), *á* 126 (*á dhéanamh*).
- **Words filed under another word.** Some entries hold a word spelled alike, which counts at the
  entry's rank but never teaches it: *dóigh* 261 is "way, manner" (*is dóigh liom*) and holds
  *dóite* ("burnt", also under *de* and *do*); *sín* 487 "stretch" holds *tSín* ("China"); *síl* 497
  "think" holds *Síle*; *sí* 32 holds *sidhe*; *ár* 351 is "our" and "slaughter" (teach "our");
  *más* 456 is *má* + *is* (and "buttock": never); *deasc* 931 "desk" holds *de dheasca* ("because
  of"); *toradh* 233 holds *torthaí* ("results" and "fruit"). Teach each entry in its own sense
  only. These are entries of their own, each in its own sense: *bith* 142 (*ar bith*) and *beatha*
  584 ("life, food"); *saor* 393 ("free, cheap") and *saoire* 1167 ("holiday": *laethanta saoire*);
  *lón* 1051 ("lunch") and *luan* 2382 ("Monday"); *má* 225 ("if") and *maigh* 557 ("plain", mostly
  in *Maigh Eo*, *Maigh Nuad*); *riail* 734 ("rule") and the verb *rialaigh* 1207 ("rule, govern"),
  with its verbal noun *rialú* 893; *iúl* 682 is only *in iúl* (*Iúil* "July" is a name).
- **Missing words.** The list has no day or month names but one (*Dé Luain* is *dé* of *dia* 224 +
  *Luain* of *luan* 2382 "Monday"; *Dé Máirt*, *Dé Céadaoin*, *Déardaoin*, *Dé hAoine*, *Dé
  Sathairn*, *Dé Domhnaigh*, *Eanáir* ... *Nollaig* go in `names`; *Meán Fómhair* and *Deireadh
  Fómhair* are list words: *meán* 208, *fómhar* 167, *deireadh* 93), and no *Éire*, *Éireann*,
  *Gaeilge*, *Gaeltacht*: they are names (*Éirinn* is an entry, 1088: write *in Éirinn* only as an
  outside word below that).
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms, unmutated: the
  checker reads their mutations). Every headword in it must be a headword of the list (the checker
  warns otherwise). If a true form is missing, write around it and name the word and form in your
  report.
- **Names** are not in the list: people, places, shops, brands, days and months go in `names`;
  list a name as you write it, mutated or not (*Máire* covers *a Mháire* and *le Máire*); a form the
  checker cannot reach by a mutation (the genitive or vocative with a changed ending: *Seán* →
  *a Sheáin*, *teach Sheáin*; *Tomás* → *a Thomáis*) goes in `names` too (*Seáin* or *Sheáin*). Use
  one-word names only, and never a name that is also a list word or holds one: most Irish place
  names are made of list words (*Baile Átha Cliath* is *baile* 49, *áth* 164, *cliath* 196; *Port
  Láirge*, *Béal Feirste*, *Cill Airne*, *Dún na nGall*, *Gaoth Dobhair*, *Leitir Ceanainn*, *Trá
  Lí*, *An Daingean*): write such a place only as plain words counted at their ranks, or pick
  another place. *Sligeach* (1697) and *Éirinn* (1088) are list words too. As characters avoid
  *Síle* (*síl*), *Saoirse*, *Aisling*, *Fionn*, *Colm*, *Bríd*, *Gráinne*, *Cian*, *Oisín*,
  *Róisín*, *Áine*, *Barra*, *Darach*, *Neasa*, *Rós*, *Ciara*, *Treasa*, *Orla*, *Sorcha*: they
  are list words. A surname's *Ó*, *Ní*, *Mac*, *Nic* are list words (*ó* 14, *ní* 18, *mac* 65):
  they count at their ranks, never as a use of *ó*, *ní* or *mac*; put the rest in `names`
  (*Briain*, *Bhriain*, *Súilleabháin*, *Néill*). Check a name with `rank.py` before you use it.
  Safe in the pilot: *Seán*, *Máire*, *Pádraig*, *Aoife*, *Niamh*, *Ciarán*, *Siobhán*, *Éamonn*,
  *Dónall*, *Clíona*, *Méabh*, *Nóra*, *Peadar*, *Tomás*, *Séamas*, *Liam*, *Eoin*, *Rónán*,
  *Tadhg*, *Cathal*, *Conall*, *Úna*, *Deirdre*, *Éabha*, *Laoise*, *Fiachra*, *Cormac*, *Eibhlín*,
  *Ruairí*, *Sadhbh*, *Ailbhe*; *Gaillimh*, *Corcaigh*, *Luimneach*, *Conamara*, *Árainn*,
  *Ciarraí*, *Londain*, *Boston*, *Toronto*, *Sydney*.
- Everyday words rank later than you would think: *duine* 26, *bean* 80, *fear* 67, *teach* 92,
  *scoil* 102, *athair* 154, *uisce* 155, *madra* 151, *carr* 198, *páiste* 201, *deartháir* 202,
  *cat* 211, *cara* 221, *doras* 228, *bia* 248, *máthair* 256, *abhaile* 278, *inné* 280, *seomra*
  360, *fuinneog* 391, *oíche* 398, *deirfiúr* 429, *arán* 431, *seachtain* 469, *bainne* 540, *siopa*
  562, *tae* 661, *caife* 707, *maidin* 742, *tráthnóna* 1081, *cistin* 1384, *leaba* 1623. The
  dialogue tag *a deir* (*abair* 86) is in the first band; *freagair* and *fiafraigh* come later.
- The list's form lists are noisy (old spellings such as *sgríobh*, *sgéal*, *bliadhain*;
  dialect forms; look-alikes such as *anfaid* under *an*, *breo* under *beo*): a new word must
  appear as itself or a true form, never through such a form. Some entries are names or pieces of
  names: skip them. Loanwords Irish uses as its own (*ríomhaire*, *teilifís*, *raidió*, *bus*,
  *club*, *albam*, *sacar*) are taught like any word.
