# Writing a Bosnian frequency course

Each course teaches one band of 100 words from the Bosnian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `bs-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## The language of the courses

Standard Bosnian, **ijekavian**, in the **Latin script**, as written in Bosnia and Herzegovina today:
*mlijeko*, *lijepo*, *dijete*, *rijeka*, *vrijeme*, *vidjeti*, *htjeti*, *djeca*, *gdje*, *ovdje*,
*prije*, *poslije*, *sjediti*, *cijeli*, *dio* — never the ekavian *mleko*, *lepo*, *dete*, *reka*,
*vreme*, *videti*, *deca*, *gde*, *ovde*, *pre*, *posle*, *sedeti*, *ceo*, *deo*, *hteo*, *voleo*,
though the list holds those spellings among the forms of its words (it is counted from subtitles in
all three standards). Write the letters **č ć ž š đ** and the digraphs **lj nj dž** properly, never
*c*, *z*, *s* or *dj* for them, and never any Cyrillic.

Bosnian words and forms, where Croatian or Serbian differ:

- **Bosnian has both *šta* and *što***: *šta* is "what" (*Šta radiš?*, *Šta je to?*, *Ne znam šta
  hoće.*); *što* is the relative "that, which" and the conjunction (*knjiga što je čita*, *drago mi je
  što si došao*, *zato što*, *jer*); "why" is *zašto*. **ko** "who" (never *tko*), and so *niko*,
  *neko*, *iko*, *svako* (never *nitko*, *netko*, *itko*, *svatko*).
- Bosnian words: *hiljada* (rather than *tisuća*), *sedmica* (not *tjedan*), *porodica* (not
  *obitelj*), *kafa* (not *kava*), *hljeb* (*kruh* also Bosnian, but not *hleb*), *voz* (not *vlak*),
  *historija* (not *povijest*, not *istorija*), *hemija*, *nauka*, *pozorište*, *univerzitet*,
  *fudbal*, *apoteka*, *kino*, *pantalone*, *ostrvo*, *spisak*, *plata*, *fabrika*, *kompjuter*,
  *komšija* and *komšiluk*, *avlija*, *bašta*, *čaršija*, *sokak*, *dućan*, *pijaca*, *kašika*,
  *viljuška*, *flaša*, *čorba*, *grah*, *krompir*, *paradajz*, *sprat* (not *kat*), *zrak* or
  *vazduh*. Bosnian writes *tačno*, *tačka*, *također*, *uopće*, *opći*, *općina*, *sretan*,
  *ujutro*, *ponovo*, *vjerovatno*, *historija*, *hemija* — the list files *točno*, *takođe*, *uopšte*,
  *srećan*, *istorija* and the like under these, so write the Bosnian spelling. *desiti se* and
  *dogoditi se*, *uraditi* and *napraviti*, *izvinite* and *oprostite* are all Bosnian.
- The months are *januar, februar, mart, april, maj, juni, juli, august, septembar, oktobar,
  novembar, decembar* (*5. maja*). Loan verbs end in *-irati* (*organizirati*, *informirati*,
  *telefonirati*), as the Bosnian standard prefers.

Grammar:

- **The infinitive** after a verb is the standard: *želim ići*, *moram raditi*, *mogu doći*, *volim
  čitati*, *počinje padati kiša*. *da* + present (*hoću da idem*) is Bosnian too, but keep to the
  infinitive in the narration so that the learner sees one pattern.
- **The future is written apart**: the infinitive (losing its *-i* before the clitic) and *ću*,
  *ćeš*, *će*, *ćemo*, *ćete*, *će*: *radit ću*, *vidjet ćemo*, *pisat će*; a verb in *-ći* keeps it
  whole: *doći ću*, *ići ćeš*. With the clitic first the infinitive is whole: *Sutra ću raditi.*
  *Ja ću ići.* Never *radiću*, *videćemo* (the checker fails them).
- **Questions**: *Da li je Amra kod kuće?* and *Je li Amra kod kuće?* are both Bosnian; so are *Imaš
  li vremena?*, *Dolaziš li?*, *Gdje je stanica?*. Use either *da li* or *je li* in a lesson, not a
  mix in one dialogue.
- **The clitics** stand in second place in their fixed order: *li*, then the auxiliary (*sam, si,
  je* — but *je* last), then dative, accusative, *se*: *Dao sam mu ga.*, *Amra mu ga je dala.*,
  *Vidio sam je.*, *Bojim ga se.* Negation is a word of its own, written apart: *ne znam*, *ne može*,
  *ne radi* — except *nisam*, *nije* (of *biti*), *neću*, *neće* (of *htjeti*), *nemam*, *nema* (of
  *nemati*) and *nemoj*, written as one.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Amra traži svoj mobitel. Ide u kuhinju. Nema ga. Ide u sobu...* Everyday
  life in Bosnia and Herzegovina — Sarajevo, Mostar, Tuzla, Zenica, Banja Luka, Bihać, Travnik,
  Jajce, Konjic, Trebinje, Visoko, Goražde, Bijeljina, Brčko, Livno, Neum, the rivers (Una, Neretva,
  Drina, Vrbas, Bosna), the mountains (Jahorina, Bjelašnica, Igman, Vlašić), the old town and its
  *čaršija*, the bridge in Mostar — and now and then people abroad (Beč, Berlin, Stockholm,
  Ljubljana): home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could
  be unclear. Give each story its own names: the learner reads the course in one go, and two
  Tariks in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object). Bosnian drops subject pronouns: write *ja*, *ti*, *on*, *ona* only
  where Bosnian would (contrast, emphasis, a new subject). Little dialogue; when there is some,
  short and simple, with *ti* among family and friends and *vi* with strangers, older people and in
  shops or offices (*Izvolite.*, *gospodin Delić*, *gospođa Kovačević*).
- **Cases** cannot be avoided: use the frequent ones in their simple, regular patterns (*u školu*,
  *u školi*, *sa mamom*, *kod nane*, *Amra ima brata*, *nema ključa*, *ide kući*, *daje mami cvijet*),
  and let each band add the patterns it needs. Bosnian writes *sa* before a word beginning with
  *s, š, z, ž* or a consonant group and where it reads better (*sa sestrom*, *sa mnom*), *s*
  elsewhere (*s Amrom*); both are list words.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present tense, *biti* (*je*, *sam*, *nije*), *imati* / *nemati* (*ima*, *nema*
    "there is / isn't"), *htjeti*, *moći*, *morati*, *trebati*, *željeti*, *voljeti* + infinitive,
    the future with *ću* (*ići ću*, *radit ćemo*), the short pronouns in second place (*vidi ga*,
    *daje joj*), *se* verbs (*zove se*, *vraća se*), simple questions (*ko*, *šta*, *gdje*, *kako*,
    *zašto*, *kad*, *da li*, *... li*); the **perfekt** for what happened, in short sentences (*Amra
    je bila u Mostaru.*, *Došao sam.*, *Šta si rekao?*);
  - ranks 301–600: the perfekt freely beside the present (*Jučer je Tarik kupio hljeb.*), the
    imperative, positive and negative (*dođi*, *uzmi*, *sjedni*, *nemoj se brinuti*), possessives
    in every case (*sa mojim bratom*, *u našoj kući*), comparatives (*veći od*, *bolji*, *lakše*),
    *jer*, *kad*, *ako*, *dok*;
  - ranks 601–1000: **aspect** contrasted (*čitao je knjigu kad je zazvonio telefon*; *svaki dan
    dolazi* / *sutra će doći*), verbs of motion with prefixes (*doći*, *otići*, *izaći*, *ući*,
    *preći*), the relatives *koji*, *što*, *gdje*, reported speech with *da* (*kaže da će doći*),
    numbers with nouns (*dva brata*, *pet maraka*);
  - ranks 1001–1300: comparatives and superlatives in full (*najbolji*, *najviše*), the
    **kondicional** for wishes and polite requests (*htio bih*, *možete li...*, *bilo bi lijepo*),
    *koji* in every case, indirect questions (*pita da li...*, *ne zna gdje...*);
  - ranks 1301–1600: *da bi* / *kako bi* of purpose, *ako* + present and *ako* + perfekt, the
    vocative (*Amra!*, *Tariče!*, *mama*, *gospodine*), the impersonal *se* (*ovdje se ne parkira*),
    *trebati* in its forms;
  - ranks 1601–2000: the pluperfect only where clear (*bio je već otišao*), unreal conditions
    (*kad bih imao vremena, putovao bih*), the passive participle and the passive with *se*
    (*prodaju se stanovi*; *kuća je sagrađena 1950.*), reported speech;
  - ranks 2001–2500: past unreal conditions (*da sam znao, došao bih*), the verbal adverbs
    (*radeći*, *došavši*), relative *čiji*, *koji* with prepositions, *koliko... toliko*, collective
    numerals (*dvoje djece*, *troje ljudi*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *međutim*, *dok*, *budući
    da*, *uprkos*, *što se tiče*, *zbog toga*, *stoga*; *nastaviti*, *prestati*, *početi* +
    infinitive; the **aorist** (*reče*, *dođe*, *uzeše*) only in a short tale or an old-style
    passage; common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise
    adjectives and clear images, the newspaper column (*kolumna*, a light reflection on everyday
    life), dialogue that marks *ti* and *vi*, and less common connectors (*pod uslovom da*, *iako*,
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
  Šta traži Amra?
  Amra traži svoj mobitel.
  Gdje je mobitel?
  Mobitel je u autu.
  ```

  The header `Pitanja:` is layout: the checker does not count it as a word.
- **Standard Bosnian**: neutral, correct and natural, with all its letters; no slang, no dialect
  in the narration (a regional word may appear in dialogue, explained by the story, from rank
  3000). Prefer words understood everywhere in Bosnia and Herzegovina. Bosnian keeps a few
  *h*-spellings (*lahko*, *mehko*, *kahva*); write the list's word (*lako*, *kafa*) so that the
  learner meets the spelling the list counts.
- **Bosnian typography**: dialogue as paragraphs that begin with an em dash and a space
  (`— Gdje si?`), the narrator's remarks in the same paragraph after another dash (`— Ovdje — kaže
  Tarik.`); quoted words and titles in „low and high“ quotation marks; no space before `:`, `;`,
  `?` or `!`. Dates as *5. maja*, ordinals as digits with a full stop (*3. sprat*), times as *u 7
  sati* or *u 7:30*. Prices in marks (*5 KM*, *pet maraka*) or euros abroad.
- **Original texts**: write your own stories; never copy, translate or retell existing course
  material (LingQ's, the Croatian or Serbian courses' or any other).

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form, of every faith: God (*Bog*, *Allah*), prophets, saints, mosques,
  churches, monasteries, priests and imams, prayer, fasting, heaven and hell, angels and devils,
  souls and ghosts, and every custom or holiday of religious origin, even as a date, a school break,
  a family visit or a gift season: *Bajram*, *Ramazan*, *iftar*, *Božić*, *Badnjak*, *Uskrs*, *slava*
  (the family saint's day), *Sveti Nikola*, carnival. This includes the **Turkish and Arabic
  loanwords tied to faith** that Bosnian uses every day: *džamija*, *mesdžid*, *mejtef*, *medresa*,
  *hodža*, *imam*, *efendija*, *dova*, *namaz*, *abdest*, *sevap*, *haram*, *halal*, *dženet*,
  *dženaza*, *mezar*, *turbe*, *tekija*, *hadžija*, *kurban*, *mevlud*, *ilahija*, the prayer times
  *sabah*, *akšam*, *ikindija*, *jacija* (write *jutro*, *veče*, *popodne*), and the greetings and
  phrases *selam*, *merhaba*, *inšallah*, *mašallah*, *ako Bog da*, *hvala Bogu*, *Bože moj*,
  *bujrum* (write *izvolite*), *rahmetli*. Everyday Turkish loanwords with no religious sense are
  Bosnian and welcome: *komšija*, *avlija*, *bašta*, *čaršija*, *sokak*, *dućan*, *jastuk*,
  *čarapa*, *šećer*, *boja*, *sat*, *ćuprija*, *česma*, *džezva*, *fildžan*, *merak*, *ćevapi*,
  *burek*, *pita*, *sarma*, *baklava*. Folk customs with no faith named are fine: a summer town
  festival with music and food, a fair (*vašar*), the plum or apple harvest as farm work, a
  birthday, New Year's Eve as a family evening, the start of the school year, the summer holidays.
  Prefer place names without a faith in them; never visit a mosque, a church, a monastery or a
  *tekija* (Blagaj is its spring and the river Buna); use *nebo* only as the sky, *moliti* only as
  "ask, please" (*molim*, *molim vas*), *vjerovati* only as believing a person or a fact, *čudo* only
  as a wonder (*nije čudo*);
- **politics**: the state and its entities, the government, parliament, elections, parties,
  ministers, presidents, kings, sultans, pashas and beys, the nation, peoples and ethnic groups,
  independence, flags, borders as disputes, and the war of the 1990s in any form; everyday civic
  life (a library, a school, the *općina* counter, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office is fine;
  theft, kidnapping, arrests, prisons, trials, fights, mines and shooting are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, curses and words whose main sense is sexual;
- **alcohol** in every form (beer, wine, *rakija*, a toast, a bar visit for a drink), **gambling**
  (lotteries, betting, casinos), drugs and smoking (also the *nargila*);
- **deliberate misspellings**: no spellings without *č ć ž š đ*, no ekavian forms, no colloquial
  spellings, not even in a child's speech or a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a nationality or people (*Bošnjak*, *Srbin*, *Hrvat*, *bosanski* as a people's
  adjective) is skipped. Languages may be taught as languages (*govori bosanski*, *uči engleski*).
  Common first names are fine as characters.

The themes keep to this: the history courses (*Historija na dohvat ruke*, *Arheologija i
prošlost*, *Istraživači i putnici*) tell the history of everyday life, towns, crafts, trade,
bridges, science and travel, never of wars, empires, rulers, faiths or politics; *Vašari i narodna
slavlja* and *Narodna umjetnost i običaji* are folk and family celebrations with no faith or
holiday of religious origin named; *Javne službe* is the library, the health centre and the
*općina*'s counters, never politics; *Bosanski jezik* and *Govori i krajevi Bosne i Hercegovine* are
about words, dialects and learning, never about language politics or which people speaks what;
*Priče i legende* tells folk tales without saints, devils, miracles or faith.

A word whose COMMON sense breaks a rule is skipped even if a story could use another sense.
Skip band words that only fit these topics, and say so in your report. The skip list for the
first 1000 ranks:

- religion: *bog* 104, *duša* 205 (also *dušo* "darling"), *duh* 592, *vrag* 678, *kleti* 774,
  *crkva* 794, *zbogom* 812, *đavo* 818, *prokletstvo* 837, *pakao* 919;
- politics: *predsjednik* 490, *kralj* 522, *država* 525, *zakon* 561, *narod* 625, *vlada* 711,
  *gospodar* 757, *rob* 789, *kraljica* 909;
- war, crime, police, courts: *policija* 283, *rat* 324, *zatvor* 360, *ubistvo* 386, *kriv* 391,
  *ubica* 405, *oružje* 414, *boriti* 418, *policajac* 443, *pucati* 447, *pištolj* 452, *dokaz*
  474, *ukrasti* 487, *napad* 498, *žrtva* 536, *borba* 571, *udariti* 581, *vojnik* 588, *sud*
  596, *vojska* 597, *napasti* 598, *lov* 620, *general* 726, *detektiv* 732, *bomba* 770, *metak*
  775, *svjedok* 802, *uhapsiti* 820, *zločin* 823, *istraga* 962;
- death: *ubiti* 102, *umrijeti* 190, *mrtav* 192, *smrt* 321, *krv* 382, *umirati* 631,
  *preživjeti* 669;
- vulgar, curses and insults: *sranje* 193, *jebati* 216, *prokleti* 547, *seks* 652, *jebeno*
  675, *idiot* 688, *kurac* 755, *dupe* 801, *kurva* 844, *dovraga* 850, *kreten* 918, *kučka* 971,
  *jebote* 993;
- alcohol, drugs, gambling: *droga* 630, *kladiti* 664, *pivo* 760, *vino* 895; *bar* 363 only as
  "at least" (*bar jednom*), never a bar; *piće* 527 only as a soft drink (*sok*, *voda*, *čaj*);
- Croatian words Bosnian says otherwise: *obitelj* 513 (write *porodica*), *tjedan* 562 (write
  *sedmica*), *kat* 927 (write *sprat*);
- nationalities: *američki* 740 (*engleski* 683, *francuski* 759 and *njemački* 931 may be taught
  as languages).

Further on, the same categories hold; for example *raj*, *anđeo*, *grijeh*, *božji*, *vjera*,
*molitva*, *džamija*, *svećenik*, *sveštenik*, *manastir*, *krst*, *svetac*, *sultan*, *paša*, *aga*,
*grob*, *poginuti*, *nož* (as a weapon; a kitchen knife is fine), *lopov*, *krasti*, *pljačka*,
*sudija*, *kazna*, *pijan*, *pušiti*, *cigareta*, *alkohol*, *ministar*, *politika*, *vlast*.

## The file

Write `tools/courses/bs/<id>.json`:

```json
{
  "id": "bs-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Bosnian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Bosnian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/bs/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *išao* or *idem* for *ići*, *bolji* for *dobar*, *ga* for
  *on*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the
  infinitive of a verb (*vidjeti*, *nemati*, *stignuti*), the nominative singular of a noun, the
  masculine singular of an adjective (*star*, *velik*, *nov*, *cijeli*).
- **Ijekavian**: the checker fails a lesson that uses an ekavian spelling
  (`tools/courses/bs/ekavian_forms.tsv`, made from the dictionary pack and the list: *videla*,
  *mleko*, *gde*, *hteo*, *posle*, *ceo*...) or a future written joined (*radiću*), and names the
  word to write. Serbian words that are Bosnian too (*šta*, *ko*, *hiljada*, *voz*) pass.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word, a form or variant of another word, a Croatian word Bosnian says otherwise, a word
  of the content rules); the checker's last line lists the band words not taught yet.

Things to know about the Bosnian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/bs/wordlist.tsv`
  (a copy of the app's `bs.tsv`); the checker reads it instead of the app's list, so a rebuilt
  app list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  subtitles in all three standards, so everyday spoken words rank high and written ones late, and
  its forms include ekavian spellings (*videla* under *vidjeti*): the checker fails those.
- The checker compares whole words, lowercased, split at hyphens. **The clitics are words of
  their own**, as written: *je*, *sam*, *si*, *smo*, *ste*, *su*, *bi*, *bih* and *nije*, *nisam*
  are forms of *biti* 1; *ću*, *ćeš*, *će*, *ćemo*, *ćete* and *neću*, *neće*, *hoću* of *htjeti*
  10; *nemam*, *nema* of *nemati* 58 (its own word, not *imati*'s); *nemoj* is its own word.
- **Pronouns hold their case forms**: *me*, *mene*, *meni*, *mnom* are *ja* 9; *te*, *tebe*,
  *tebi*, *tobom* *ti* 8; *ga*, *mu*, *njega*, *njemu*, *njim* *on* 13; *je* (her), *ju*, *nju*,
  *joj*, *njom* *ona* 43; *nas*, *nam*, *nama* *mi* 11; *vas*, *vam*, *vama* *vi* 25; *ih*, *im*,
  *njih*, *njima* *oni* 34; *se* is *se* 4, and *sebe*, *sebi*, *sobom* are *sebe*; *koga*, *kome*,
  *kim* are *ko* 57; *čega*, *čemu*, *čime* *šta* 20. A form shared by two words counts for both
  (*je* "is" and "her"; *te* "you" and "and"; *sam* "am" and "alone"): use a new word in its own
  sense, never through its homograph.
- **Aspect pairs are two words**, each its own entry: *doći* / *dolaziti*, *otići* / *odlaziti*,
  *vratiti* / *vraćati*, *pokušati* / *pokušavati*, *čitati* / *pročitati*, *jesti* / *pojesti*,
  *piti* / *popiti*, *dati* / *davati*, *početi* / *počinjati*. Teach each through its own forms
  (*dolazi* is *dolaziti*'s, never *doći*'s; *došao* is *doći*'s). *stignuti* holds *stići*,
  *stigao*, *stigne* (one verb spelled two ways). Look a verb up with `rank.py` before you teach it.
- **Adjectives are filed under the masculine**: *star* holds *stari*, *stara*, *staro*; *dobar* 70
  holds *bolji*, *najbolji*; *velik* holds *veći*; *cijeli* is how the list writes *cio*/*cijel*.
  Adverbs in *-o* are words of their own (*dobro* 28, *lijepo*, *sigurno*, *teško*) beside their
  adjectives. *nikad* 91 holds *nikada*, *ikad* holds *ikada*, *ponovo* holds *ponovno*, *hajde* 79
  holds *hajdemo*, *vijest* holds *vijesti* "the news".
- **Words filed under a commoner word spelled alike** count at that word's rank and are never
  band words: *dana* "days" is also *dati*'s, *pita* "pie" *pitati*'s, *jelo* "dish" *jesti*'s,
  *imam* "I have" is *imati*'s, *tim* "team" *taj*'s, *sto* is "hundred" (write *stol* or *sto* for a
  table only where the story makes it plain). Below rank 1000 use them only where the story makes
  their meaning plain.
- The infinitive before the future clitic (*radit*, *vidjet*, *pisat*) and the common forms the
  list misses are added in `extra_forms.tsv` (generated from the dictionary pack: each headword's
  inflected forms the list lacks, no ekavian spellings), so use them freely; if another true form
  is missing, write around it and say so in your report.
- A **compound** written as several words (*u redu*, *na primjer*, *svaki dan*) counts as its parts.
- A **name** goes in `names` as written, in the nominative (*Amra*, *Tarik*, *Sarajevo*, *Banja
  Luka*): the checker accepts its regular case forms (*Amre*, *Tariku*, *Mostaru*, *Tuzle*,
  *Zenice*; the vowel that drops in *Bihać* → *Bihaća* is regular too). A name that is not regular
  (*Sarajevo* → *Sarajevu*, neuter) may need its oblique form listed as well. Keep only true names
  there, never common words, and never a name that is also a list word: avoid *Luka*, *Hana*
  (*han*), *Dino* (*dina*), *Lana* (*lan*), *Mila*, *Jasmin*, *Zlatan*, and the towns *Konjic*
  (*konj*), *Visoko* and *Jajce*. Safe: *Amra*, *Tarik*, *Lejla*, *Edin*, *Selma*, *Haris*, *Aida*,
  *Mirza*, *Belma*, *Amer*, *Ajla*, *Faris*, *Kenan*, *Lamija*, *Nermin*, *Vedad*, *Ena*, *Damir*;
  *Sarajevo*, *Mostar*, *Tuzla*, *Zenica*, *Bihać*, *Travnik*, *Neum*, *Una*. Prefer neutral given
  names; avoid names of prophets and saints and surnames with a religious title in them (*Hodžić*,
  *Hadžić*). A name that looks like an ekavian word (*Zenica*, *zjenica*) passes once it is in
  `names`. Check a name with `rank.py` first. Day, month and season names are in the list
  (*nedjelja*, *petak*, *subota*, *ljeto*, *zima*...).
- Everyday words rank later than you would think (look them up with `rank.py`): *tamo* 93, *mama*,
  *tata*, *kuća*, *djeca*, *danas*, *grad*, *soba*, *voda*, *škola*, *pas*, *knjiga*, *hrana*,
  *kafa* 778, *hljeb*; the dialogue tag *kaže* (*kazati* 68) is in the first band, *pita* (*pitati*)
  and *odgovara* (*odgovarati*) come later.
- The list's form lists are noisy (ekavian spellings, archaic aorists, look-alikes such as *zara*
  under *zar* or *doli* under *do*); a new word must appear as itself or a true inflection, never
  through such a form. Loanwords that Bosnian uses as its own (*mobitel*, *internet*, *film*,
  *auto*, *super*) are taught like any word.
- Write numbers as digits (*7 sati*, *3 djece*), except a number word that is in your band: spell
  that one out to teach it (*sedam dana*). Times as *u 7 sati* or *u 7:30*; dates as *5. maja*.
