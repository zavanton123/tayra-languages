# Writing an Uzbek frequency course

Each course teaches one band of 100 words from the Uzbek word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `uz-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 10,000 words, so the last course is
`uz-mini-10000`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Dilnozaning mushugi bor. Bugun Dilnoza uni hech qayerda koʻrmayapti...*
  Everyday life in Uzbekistan: Toshkent (Chorsu bozori, the metro, a *mahalla*, a block of flats on
  Chilonzor), Samarqand, Buxoro, Xiva, Fargʻona, Andijon, Namangan, Qarshi, Termiz, Nukus, the
  villages and their *hovli* courtyards with a *soʻri* under the vines; and now and then Uzbek
  families in Moscow, Istanbul, Seoul or New York: home, family, food (*osh*, *somsa*, *non*, green
  tea in a *piyola*), shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Uzbek *u* is "he", "she", "it" and "that",
  so say who *u* is whenever it could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb last). The present-future in *-a/-y-* (*Anvar uyga keladi.*, *Dilnoza
  kitob oʻqiydi.*) and the present continuous in *-yap-* (*Dilnoza choy ichyapti.*) up to rank 500,
  with *bor/yoʻq*, *kerak* and the past in *-di* of single finished events where Uzbek needs it
  (*Kecha uyga keldi.*); from rank 600 the story may be told in the past (*Anvar uydan chiqdi.
  Yomgʻir yogʻayotgan edi.*). Little dialogue; when there is some, short and simple, with *sen*
  among family and close friends and *siz* with strangers, older people and in formal settings (and
  *Dilnoza opa*, *Anvar aka*, *Karim domla*, *xola*, *amaki*, *buvi*, *bobo* as Uzbeks address people).
- **Growing difficulty after rank 1000**: the stories stay standalone and everyday, but sentences
  get longer and joined (*chunki*, *lekin*, *ammo*, *va*, *-ganda*, *-ib*, *shuning uchun*,
  *-gani uchun*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands, each band
  using what came before:
  - ranks 1–300: the present-future in *-a/-y-* with its personal endings (*boraman*, *borasan*,
    *boradi*, *boramiz*, *borasiz*), *bor* and *yoʻq*, sentences without a verb with the predicative
    endings (*Men oʻqituvchiman.*, *Biz uydamiz.*) and *-dir* (*Bu kitob*, *Bu kitobdir*), *emas*
    (*Men bola emasman.*), the cases in plain phrases: the accusative (*kitobni oʻqiydi*), the dative
    (*uyga boradi*), the locative (*uyda*), the ablative (*uydan chiqadi*), the genitive with the
    possessive (*Anvarning uyi*, *mening uyim*), the possessive endings (*uyim*, *uying*, *uyi*), the
    plural *-lar*, questions with *nima*, *kim*, *qayerda*, *qayerga*, *qanday*, *nega*, *necha*,
    *qaysi* and the particle *-mi* (*Uyga borasanmi?*), negation with *-ma-* (*kelmaydi*), *kerak*
    (*borishim kerak*), *-moqchi* (*bormoqchiman*), *bilan* and *uchun*, the past in *-di* of the
    commonest verbs (*keldi*, *dedi*, *topdim*), *-a oladi* (*ola olmaydi*);
  - ranks 301–600: the past in *-di* for events, the present continuous in *-yap-*, the imperative
    (*kel*, *keling*, *kelinglar*, *ketaylik*), *mumkin* and *kerak* with the verbal noun
    (*kirish mumkin*), *chunki*, *keyin* and *oldin* (*ovqatdan keyin*, *ketishdan oldin*),
    *-ganda* (*uyga kelganda*), comparisons with *-roq* and *eng* (*-dan kattaroq*, *eng chiroyli*),
    the numbers with *-ta* (*ikkita non*);
  - ranks 601–1000: the past in *-gan* (*kelgan*, *koʻrganman*), the evidential in *-ibdi* (*Anvar
    kelibdi.*), *-dagi* (*uydagi*, *bugungi*), the conditional *-sa* (*kelsa*, *xohlasang*), the
    converbs *-ib*, *-a/-y*, *-masdan*, *-gach*, *-guncha*, the participle in *-gan* (*kelgan
    odam*), *-ish uchun*, compound nouns with the possessive (*choy piyolasi*, *maktab sumkasi*),
    reflexive and reciprocal verbs (*kiyinmoq*, *koʻrishmoq*), *edi* and *ekan* (*kelgan edi*,
    *yaxshi ekan*);
  - ranks 1001–1300: the participles in *-gan* and *-adigan* with the possessive (*oʻqigan
    kitobim*, *boradigan joyimiz*), reported speech with *deb* and *-ganini aytdi*, *-gani uchun*,
    the verbal noun in *-ish* with the possessive (*uning kelishi*, *kelishini xohlayman*), the
    passive (*qilinadi*, *sotiladi*) and the causative (*qildirmoq*, *oʻqitmoq*), the past
    conditional (*kelsa edi*, *qaniydi*);
  - ranks 1301–1600: the indefinite pronouns (*kimdir*, *nimadir*, *hech narsa*, *hamma*, *hech
    kim*), *-ishiga qaramay*, *-ga koʻra*, *-dan beri*, *-guncha*, *ham... ham*, *na... na*, *yo...
    yo*, *-gandek*, *-sa ham*;
  - ranks 1601–2000: the converbs in everyday use (*-ib*, *-a turib*, *-masdan*, *-gach*), *-moqda*,
    the *-dir* of general statements, the passive in descriptions (*foydalaniladi*, *tashkil
    etiladi*), indirect questions (*kelish-kelmasligini bilmayman*), auxiliary verbs (*-ib qoldi*,
    *-ib boʻldi*, *-ib koʻrdi*, *-a boshladi*);
  - ranks 2001–2500: longer participial phrases, the past conditional in full (*bilganida kelardi*),
    verbal nouns (*-ish*, *-uv*, *-lik*), connectors such as *shuning uchun ham*, *boshqa
    tomondan*, *shunga qaramay*, *bundan tashqari*; reported speech with the tense of the original
    kept;
  - ranks 2501–3000: a richer, more formal register: *-moqda*, *-gan*, *-ganligi*, connectors such
    as *yaʼni*, *holbuki*, *-ga qarshi*, *-ga oid*, *-ga bogʻliq*, *-ga nisbatan*; long participial
    phrases; common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the short newspaper column on everyday life, dialogue that marks *sen* and
    *siz*, less common connectors (*modomiki*, *goʻyo*, *-dek*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone
    and register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, learned words (the Arabic and Persian loans of the
    written language) explained by their context. Still standalone, calm and easy to follow.
  From rank 2000 a lesson may also be a short informative or opinion text (a newspaper column, a
  letter to the editor, a short report), as long as it is calm and easy to follow. Still one clear
  idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*U uyga qaraydi. U yoʻlga qaraydi. Dilnoza hamma narsaga qaraydi.*).
- **Questions at the end**: after the story, a paragraph starting `Savollar:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *nega* and *qanday*, with fuller answers):

  ```
  Savollar:
  Dilnoza nimani koʻrmayapti?
  Dilnoza mushukni koʻrmayapti.
  Mushukni kim topadi?
  Mushukni Anvar topadi.
  ```

- **Standard Uzbek**: neutral, correct and natural Uzbek as written in Uzbekistan today, in the
  Latin alphabet of 1995 (a b d e f g h i j k l m n o p q r s t u v x y z oʻ gʻ sh ch ng ʼ), never
  the Cyrillic one. Write *oʻ* and *gʻ* with the turned comma ʻ (U+02BB: *oʻzbek*, *togʻ*) and the
  tutuq belgisi with ʼ (U+02BC: *maʼno*, *sanʼat*, *taʼlim*, *sheʼr*); the reader also takes *o'*,
  *o‘*, *ma'no*, but keep the text consistent. Keep the suffix rules: *-ga* is *-ka* after *k* and
  *-qa* after *q* (*yurakka*, *qishloqqa*), *-gan* is *-kan*/*-qan* after *k*/*q* (*ekkan*,
  *chiqqan*); a last *k*/*q* of a word of several syllables turns into *g*/*gʻ* before a vowel
  (*yurak*, *yuragi*; *qishloq*, *qishlogʻi*); a few nouns drop a vowel (*oʻgʻil*, *oʻgʻli*; *burun*,
  *burni*; *shahar*, *shahri*; *oʻrin*, *oʻrni*). Not Kazakh, Turkish or Russian: no Russian words
  where Uzbek has its own in the standard (*daftar*, not *tetrad*; *rahmat*, not *spasibo*). The
  particles *-mi*, *-chi*, *-ku*, *-u/-yu* and *-gina* are written onto the word (*Kelasanmi?*,
  *Senchi?*), *ham* and *da* "too, also" apart (*Men ham boraman.*, *U da keldi.*). The stories are
  for readers of every region: keep to the standard language (*kelyapti*, *qilyapti*, not the
  spoken *kelvotti*, *qilvotti*), and let a regional or colloquial word stand only where its
  context makes it clear.
- **Uzbek typography**: direct speech is set out as Uzbek books do it, each speaker's line a
  paragraph of its own beginning with an em dash and a space, the speaker after a second dash:

  ```
  — Assalomu alaykum, Dilnoza! — dedi Anvar.
  — Vaalaykum assalom! — dedi Dilnoza.
  ```

  The questions and answers of the final `Savollar:` block are plain lines, as above; a quoted
  sentence inside an answer goes in double quotation marks («...» or "..."). Proper names take
  their endings straight on, with no apostrophe or hyphen (*Anvarning*, *Dilnozaga*,
  *Toshkentda*, *Samarqanddan*). Write numbers as digits (*3 bola*, *soat 7 da*, *2020-yilda*),
  except a number word that is in your band: spell that one out to teach it (*besh kun*).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(Navroʻz, Ramazon and Qurbon hayit, weddings and the *osh* before them, history, Amir Temur and the
poets, folk tales of Nasriddin Afandi, politics, football). Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense. Skip list
noise: fragments, abbreviations, names and words of other languages.

## The file

Write `tools/courses/uz/<id>.json`, the shape of `uz-mini-0100.json`:

```json
{
  "id": "uz-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Uzbek, e.g. Ovqat va xarid",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Uzbek",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after that `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/uz/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%). `Savollar` is not counted.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *boradi* for *bormoq*, *uyda* for *uy*). Reuse earlier
  lessons' new words too. `newWords` holds the word as the list writes it: the infinitive of a
  verb in *-moq* (*bormoq*), a noun or adjective without endings (*uy*, *katta*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a fragment, an abbreviation, a list error such as a name or a foreign word);
  the checker's last line lists the band words not taught yet.

Things to know about the Uzbek list and the checker:

- **Past rank 2900 the list was rebuilt** (ranks 1–2900 and their bands are as they were, their words
  with more forms): the doers in *-chi* are words of their own (*siyosatchi* 2910, *sababchi*,
  *jinoyatchi*), but for those the earlier courses count under their word (*yordamchi*, *oʻyinchi*,
  *himoyachi*, *baliqchi*, *temirchi*, *politsiyachi* stay under *yordam*, *oʻyin*, *himoya*…); *quti*
  "box" (2904) and *ini* "younger brother" (2909) are words (and still listed under *qut* and *in*);
  *toʻlamoq* "to pay" (2902) has its forms, *toʻladi* under it and *toʻlmoq*; *chaqmoq* no longer
  holds *chaqirmoq*'s passives.
- The courses are pinned to the list they were written against, `tools/courses/uz/wordlist.tsv`
  (a copy of the app's `uz.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was counted from web text (CC-100, pages of Uzbek sites crawled in 2018: news, official
  and institutional pages, blogs), Leipzig's news of 2011 and 2020 and a little Wikipedia; there are
  no Uzbek subtitles to count. So it is rich in the words of the press and of officialdom (*davlat*
  20, *respublika* 281, *viloyat* 42, *tuman* 59, *prezident* 89,
  *taʼlim* 74, *faoliyat* 54, *tashkil* 41), and everyday words rank later than you
  might expect (*bola* 100, *uy* 131, *kitob* 210, *ona* 226, *ota* 280, *doʻst* 635,
  *bobo* 1147, *non* 1498, *choy* 1905, *rahmat* 1673, *salom* 2052, *mushuk* 3267). The
  first courses lean on what the top of the list has: a story can be about a district, its water
  service, school or the news as well as about a family. Check a word's rank rather than guess it.
- The question words rank apart: *nima* 200, *kim* 279, *qanday* 121, *necha* 311, *qaysi*
  590, *nega* 1056, *qayerda* 2110. In the first courses ask with *-mi* (*Anvar ishga
  boradimi?*) and answer with the story's sentence.
- The list files a word's endings under its headword: the cases, possessives and predicative endings
  of nouns, the tenses, persons, participles, converbs and verbal noun of verbs, as the builder makes
  them by rule for every word (`tools/frequency_uzbek.py`). It lists every form the texts have, and
  each word's core paradigm whether texts have the form or not: a noun's plural, possessives
  (*oʻrnagim*, *oʻgʻli*, *vodiysi*) and their cases (*oʻrnagimda*, *gʻayratingga*, *koʻylaklarning*),
  *-dagi* and the predicative endings (*yolgʻizman*); a verb's tenses with every person, positive and
  negated (*koʻrasizlar*, *boʻlardik*, *qilmasdi*, *ishonmas*, *kechiktirmadingiz*, *qurmoqdasiz*,
  *tozalamoqchiman*), its participles in *-gan*, *-ayotgan*, *-adigan* with the possessive
  (*kechiktirmaganingiz*), its verbal noun in *-ish* (*eshittirishim*), conditional, imperatives,
  converbs, the *-ish-* of a plural subject (*sochishdi*) and the question in *-mi* (*shugʻullandingizmi*);
  numbers with *ta* (*ikkita*, *beshtadan*). A form another word has is left to it. `extra_forms.tsv`
  adds what the list still lacks. A word counts at the rank of its headword, whatever form the story
  uses; a form neither file has counts as an outside word, and the particles *-chi*, *-ku*, *-u/-yu*,
  *-gina* are only listed on forms texts have them on. If a true form is missing, add it to
  `extra_forms.tsv` (a line with the headword, a tab and the forms, appended at the end), keep it as an
  outside word, or write around it, and say so in your report.
- Verbs are listed by their infinitive in *-moq*. A passive or reflexive verb is its verb's
  (*qilinadi*, *berildi*, *koʻrinadi* count for *qilmoq*, *bermoq*, *koʻrmoq*) unless it is commoner than
  its verb, as *tugʻilmoq* "to be born" (457), *aylanmoq* "to turn into" (275),
  *oʻtkazilmoq* (147), *hisoblanmoq* (229) are: those are words of their own.
  Causatives are words of their own (*chiqarmoq* 44, *oʻtkazmoq* 175, *oʻqitmoq*
  493), and so are the doers in *-uvchi* (*oʻquvchi* 686 "pupil", *oʻqituvchi*
  491). The verbal noun in *-ish* is the verb's (*oʻqish*, *borish*), except a few that
  texts use as nouns far more than their verb (*yoʻnalish* 195 "direction", *rivojlantirish*
  174 "development"): those are words of their own.
- The copula: *emoq* (33) holds *edi*, *edim*, *edik*, *emish*; teach it through *edi* "was". *emas*
  (69), *ekan* (246) and *esa* (35 "as for", "and") are words of their own.
- The personal and demonstrative pronouns are each their own headword with their own cases: *men*
  (*mening*, *menga*, *meni*), *sen*, *u* (*uning*, *unga*, *uni*, *unda*, *undan*), *biz*, *siz*,
  *ular*, *bu* (*buning*, *bunga*), *shu*, *oʻsha*, *kim*, *nima*; *oʻz* holds *oʻzi*, *oʻzim*, *oʻziga*.
- Postpositions and the words made with them are words of their own where the dictionary has them
  (*bilan* 4, *uchun* 11, *haqida* 49, *boʻyicha* 26, *tomonidan* 48,
  *orqali* 135, *kabi* 123); the bound nouns of place are listed bare (*old* 230: *oldida*,
  *oldin* is apart; *ust* 413: *ustida*, *ustidan*; *ost* 944: *ostida*; *ora* 638: *orasida*): teach
  them through their forms.
- *ta* (72) is the counter written apart after digits (*5 ta kitob*); with a number word it is written
  on (*ikkita*, *bitta*), and *bitta* (552) is a word of its own.
- Verbs made with *etmoq* or *qilmoq* and a noun written apart (*tashkil etmoq*, *qabul qilmoq*, *amalga
  oshirmoq*, *davom etmoq*, *eʼlon qilmoq*) are listed by their noun (*tashkil* 41, *qabul* 104,
  *amal* 37, *davom* 80, *eʼlon* 325): teach *tashkil* through *tashkil etadi*.
- A form shared by two words counts for both, and some forms belong to a homograph: *bora* (182,
  "regard": *borasida*) is no form of *bormoq*; *yoz* "summer" and "write!", *ot* "horse, name" and
  "throw!", *tush* "noon, dream" and *tushmoq*, *och* "hungry" and "open!", *kech* "evening" and
  *kechmoq*, *qoʻy* "sheep" and "put!", *yosh* "young, age" and "tear". Use a new word in its own sense,
  never through its homograph.
- The checker compares whole words, lowercased, with *oʻ*, *gʻ* and *ʼ* read however they are typed; a
  word joined by a hyphen counts as its parts (*ota-ona* is *ota* + *ona*).
- A **name** goes in `names` once, without endings (*Anvar*, *Dilnoza*, *Toshkent*, *Fargʻona*): the
  checker accepts it with its endings written on (*Anvarning*, *Dilnozaga*, *Toshkentda*,
  *Fargʻonadan*, *Toshkentlik*). A name of several words listed whole counts as one known word
  wherever it is written whole; a name listed whole hides its parts, so don't list one whose part is a
  band word you teach. The capitalised *Oʻzbekiston*, *Toshkent*, *Samarqand*, *Rossiya* are not list
  words: put them in `names`; *oʻzbek* (94), *rus* (567), *arab* (681) are list words.
- Keep only true names in `names`, never common words. Many Uzbek first names are words (*Umid*
  "hope", *Iroda* "will", *Komil* "perfect", *Ravshan* "bright", *Yulduz* "star", *Laziz*
  "delicious", *Lola* "tulip", *Malika* "queen", *Feruza* "turquoise", *Sardor*, *Botir*, *Jasur*,
  *Aziz*, *Bahodir*): don't use them. Some names are list words only as noise (*Javohir* 4776,
  *Shahlo*, *Barno*, *Lobar* near the end): avoid them too. Names that are safe include
  *Anvar*, *Dilnoza*, *Zarina*, *Bekzod*, *Aziza*, *Gulnora*, *Nodira*, *Kamola*, *Otabek*,
  *Doniyor*, *Farrux*, *Akmal*, *Dildora*, *Madina*, *Sevara*, *Ulugʻbek*, *Bobur*, *Hamid*,
  *Shohruh*, *Nargiza*, *Mohira*, *Zulfiya*, *Gulnoza*, *Saida*, *Behruz*, *Dilfuza*, *Sanjar*,
  *Oybek*, *Munisa* (check each with rank.py). Day and month names are in the list (*dushanba*,
  *yanvar*, *mart*): teach them.
- About 7,217 of the list's 10,000 words, and 372 of the first 1,000, have no entry in the
  dictionary pack (`dictionaries/uz-en.sqlite`, from the English Wiktionary, 3,700 Uzbek words): they
  come from Apertium's Uzbek lexicon, which gives their kind but no meaning. Teach such a word in the
  sense you are sure of; skip it if you are not.
- Some entries are noise: names that slipped through (*ibn* 962, *javohir*, *shavkat*, *navoiy*), forms
  filed as words (*ekanlik* 439, *aytishicha* 843, *yilgi* 966, *boʻlsin* 1099) and
  misspellings (*etishmoq* 1034 is mostly *yetishmoq*): skip them.
