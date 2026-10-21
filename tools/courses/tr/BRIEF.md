# Writing a Turkish frequency course

Each course teaches one band of 100 words from the Turkish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `tr-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ayşe bir kedi istiyor. Bir barınağa gidiyor. Orada üç kedi görüyor...*
  Everyday life in Turkey: İstanbul, Ankara, İzmir, Bursa, Antalya, Eskişehir, Konya, Trabzon and
  the Black Sea villages, Kapadokya, the Aegean coast, a village or a summer house (*yazlık*); and
  now and then Turkish-speaking families in Berlin, Amsterdam, London or Lefkoşa: home, family,
  food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Turkish *o* is "he", "she" and "it", so
  say who *o* is whenever it could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb last). The present in *-iyor* up to rank 500 (*Ali eve geliyor.*), with
  *var/yok*, *-mak istiyor* and the past in *-dı* of single finished events where Turkish needs it
  (*Dün eve geldi.*); from rank 600 the story may be told in the past (*Ayşe evden çıktı. Yağmur
  yağıyordu.*). Little dialogue; when there is some, short and simple, with *sen* among family and
  friends and *siz* with strangers, older people and in formal settings (and *Ayşe Hanım*,
  *Ali Bey*, *amca*, *teyze* as Turkish addresses people).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*çünkü*, *ama*, *ve*, *-ken*, *-ınca*, *-dığı
  için*, *bu yüzden*, *oysa*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present in *-iyor*, *var* and *yok*, sentences without a verb and their
    personal endings (*Ali öğretmen.*, *Ben evdeyim.*) with *değil*, the cases in plain phrases:
    the accusative (*evi görüyor*), the dative (*eve gidiyor*), the locative (*evde*), the ablative
    (*evden çıkıyor*), the genitive with the possessive (*Ali'nin evi*, *benim evim*), the
    possessive endings (*evim*, *evin*, *evi*), the plural *-lar*, questions with *mi* and with
    *ne*, *kim*, *nerede*, *nereye*, *nasıl*, *neden*, *kaç*, *hangi*, negation with *-ma-*,
    *-mak istiyor*, *ile* and *-la* (*Ali ile*, *Ali'yle*), *için*, *gibi*, the past in *-dı* of
    the commonest verbs (*geldi*, *dedi*, *buldum*), *olsun*;
  - ranks 301–600: the past in *-dı* for events, the future in *-acak*, the aorist for habits and
    offers (*Her sabah çay içer.*, *Yardım eder misin?*), the imperative (*gel*, *gelin*,
    *gidelim*), *-abil-* and *-ama-* (*gelebilir*, *yapamıyor*), *lazım*, *gerek* and *-malı*,
    *çünkü*, *sonra* and *önce* (*yemekten sonra*, *gitmeden önce*), *-ken*, comparisons with
    *daha* and *en* (*-dan daha büyük*, *en güzel*);
  - ranks 601–1000: the past in *-mış* for what one hears or finds (*Ali gelmiş.*), *-ki*
    (*evdeki*, *yarınki*), the conditional *-sa* (*gelirse*, *istersen*), the converbs *-ıp*,
    *-arak*, *-ınca*, *-madan*, *-dıktan sonra*, the participle in *-an* (*gelen adam*), *-mak
    için*, *-mak zorunda*, compound nouns (*çay bardağı*, *okul çantası*), reflexive and
    reciprocal verbs (*giyinmek*, *görüşmek*);
  - ranks 1001–1300: the participles in *-dık* and *-acak* with the possessive (*okuduğum kitap*,
    *gideceğimiz yer*), reported speech with *-dığını söyledi* and *diye*, *-dığı için*, the verbal
    noun in *-ma* with the possessive (*onun gelmesi*, *gelmesini istiyorum*), the passive
    (*yapılıyor*, *satılır*) and the causative (*yaptırmak*, *getirmek*), *-malı* and *-abilir* for
    duty and possibility, the past conditional (*gelseydi*, *keşke*), *-dıkça*;
  - ranks 1301–1600: the indefinite pronouns (*biri*, *bir şey*, *hiçbir şey*, *herkes*,
    *kimse*), *-dığı halde*, *-masına rağmen*, *-a göre*, *-dan beri*, *-ana kadar*, *hem... hem*,
    *ne... ne*, *ya... ya*, *ister... ister*, *-mış gibi*, *-sa bile*, *-sa da*;
  - ranks 1601–2000: the converbs in everyday use (*-arak*, *-ıp*, *-madan*, *-dıkça*, *-alı*),
    *-makta*, the *-dır* of general statements, the passive in descriptions (*yapılır*,
    *kullanılır*), indirect questions (*gelip gelmediğini bilmiyorum*), *-dığında*, *-mak üzere*;
  - ranks 2001–2500: longer participial phrases, the past conditional in full (*bilseydi
    gelirdi*), *-mış olmak*, *-acak olmak*, verbal nouns (*-ma*, *-ış*, *-lık*), connectors such as
    *dolayısıyla*, *bu nedenle*, *öte yandan*, *buna rağmen*, *üstelik*; reported speech with the
    tense of the original kept;
  - ranks 2501–3000: a richer, more formal register: *-maktadır*, *-mıştır*, *-dığı takdirde*,
    connectors such as *nitekim*, *oysa*, *halbuki*, *-a karşın*, *-a ilişkin*, *-a dair*; long
    participial phrases; common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *köşe yazısı* (a light column on everyday life, as in Turkish
    newspapers), dialogue that marks *sen* and *siz*, less common connectors (*mademki*, *meğer*,
    *-casına*, *-dıkça*);
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
    and rhythm, academic and journalistic registers, regional and learned words (Arabic and
    Persian loans of the written language) explained by their context. Still standalone, calm and
    easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a *köşe yazısı*, a
  letter to a newspaper, a short report), as long as it is everyday, calm and easy to follow.
  Still one clear idea at a time: a learner at the level should follow every sentence on first
  reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Mutfağa bakıyor. Salona bakıyor. Bahçeye bakıyor.*).
- **Questions at the end**: after the story, a paragraph starting `Sorular:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *neden* and *nasıl*, with fuller answers):

  ```
  Sorular:
  Ayşe ne istiyor?
  Ayşe bir kedi istiyor.
  Ayşe nereye gidiyor?
  Ayşe bir barınağa gidiyor.
  ```

- **Standard Turkish**: neutral, correct and natural Turkish as written today (the spelling of
  the Türk Dil Kurumu), with its vowel harmony, consonant changes (*kitap*, *kitabı*; *renk*,
  *rengi*) and suffix order right in every word; no slang, no obscenities. The stories are for
  readers of every country, so keep to the standard language and let a regional or colloquial
  word (*yazlık*, *dolmuş*, *simit*) stand only where its context makes it clear. Write the
  question particle apart and in harmony with the word before it (*Geliyor musun?*, *Ev büyük
  mü?*), and *de/da* "too" apart (*Ben de geliyorum.*) but the locative joined (*evde*). Write the
  circumflex where the standard spelling has it, as the list's headwords do: *hâlâ* "still"
  (*hala* is "aunt"), *kâr*, *dükkân*, *kâğıt*, *rüzgâr*, *hikâye*, *millî*.
- **Turkish typography**: direct speech is set out as Turkish books do it, each speaker's line a
  paragraph of its own beginning with an em dash and a space, the speaker after a comma:

  ```
  — Merhaba Ayşe! diyor Ali.
  — Merhaba! diyor Ayşe.
  ```

  The questions and answers of the final `Sorular:` block are plain lines, as above; a quoted
  sentence inside an answer goes in double quotation marks (*Ali "Teşekkür ederim" diyor.*).
  Proper names take their endings after an apostrophe (*Ali'nin*, *Ayşe'ye*, *İstanbul'da*),
  common words never. Write numbers as digits (*3 çocuk*, *saat 7'de*, *7.30'da*, *1990'larda*),
  with the ending after an apostrophe, except a number word that is in your band: spell that one
  out to teach it (*beş gün*).
- **Suitable for all ages**: no drugs or alcohol as a topic, no sex, violence, war, cruelty or
  gore. Keep away from politics, the army, the state's doings and nationalism, from current
  conflicts and disputed history, and from religion taken sides on; everyday civic life (a
  library, a *muhtar*, a school meeting) is fine, and so are the holidays as family days (*bayram*
  visits, sweets for the neighbours), without the sacrifice. Skip band words that only fit such
  topics, and say so in your report.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/tr/<id>.json`:

```json
{
  "id": "tr-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Turkish, e.g. Yemek ve alışveriş",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Turkish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/tr/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%). `Sorular` is counted too
  (as *soru*, rank 283), so in the first courses it costs one outside word per lesson.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gidiyor* for *gitmek*, *evde* for *ev*). Reuse earlier
  lessons' new words too. `newWords` holds the word as the list writes it: the infinitive of a
  verb (*gitmek*), a noun or adjective without endings (*ev*, *güzel*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a suffix, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the Turkish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/tr/wordlist.tsv`
  (a copy of the app's `tr.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was built partly from film subtitles, so it is rich in spoken words (*tamam*, *hadi*,
  *efendim*, *teşekkür*), and some everyday words rank later than you might expect (*baba* is 102,
  *anne* 103, *kardeş* 216, *çay* 818, *kedi* 951, *ekmek* 1087, *kahvaltı* 1272, *mutfak* 1317,
  *sandalye* 1750). Check a word's rank rather than guess it.
- The list files a word's endings under its headword: the cases and possessives of nouns, the
  tenses and persons of verbs. It keeps at most 40 forms of a word, so `extra_forms.tsv` adds the
  inflections the English Wiktionary gives for the list's words (the conjugation tables, the cases
  with the possessives) that film subtitles use. A word counts at the rank of its headword,
  whatever form the story uses; a form neither file has counts as an outside word. Still missing
  are some forms of the commonest verbs, above all of *olmak* (*olması*, *olmadan*, *olduğumu*,
  *olduğunda*, *olanlar*, *olabilirim*) and a few of *yapmak* and *istemek* (*yaptığımı*,
  *istediğimi*). If a true form is missing, add it to `extra_forms.tsv` (a line with the
  headword, a tab and the forms, appended at the end), keep it as an outside word, or write
  around it, and say so in your report. *bulamıyor* is filed under a rare word (*bulamak*, rank 9862): write
  around it.
- The headwords of *burada*, *orada*, *nerede* are the stems *bura* (40), *ora* (124) and *nere*
  (113): write `newWords` as *bura*, *ora*, *nere*. The headword *de* (12) is "too" (and holds a
  few forms of *demek*); *da* (15) is its own entry.
- The list files many causative and passive verbs under their base verb: *getirmek* under
  *gelmek*, *bitirmek* under *bitmek*, *kaçırmak* under *kaçmak*, *düşürmek* under *düşmek*,
  *tanıtmak* under *tanımak*, *artırmak* under *artmak*, *uyandırmak* under *uyanmak*,
  *hazırlanmak* under *hazırlamak*, *kullanılmak* under *kullanmak*, *açılmak* under *açmak*. Such a
  form counts at the base verb's rank, but it is another word: never teach *gelmek* through
  *getirdi*, or *bitmek* through *bitirdi*. Many others are entries of their own (*göstermek*,
  *öğretmek*, *öldürmek*, *çıkarmak*, *kapatmak*, *değiştirmek*, *indirmek*).
- A form shared by two words counts for both, and some common words are filed only as a form of
  another word: *yedi* "seven" under *yemek*, *et* "meat" under *etmek*, *altın* under *alt*,
  *yarı* under *yar*, *koyun* "sheep" under *koy*, *karın* "belly" under *kar*, *kalın* "thick"
  under *kalmak*, *boyun* "neck" under *boy*, *arı* "bee" under *ar*, *ton* under *don*, *kuruş*
  under *kurmak*, *birim* under *bir*, *ortam* under *orta*, *konum* under *konu*, *anı* under *an*,
  *deneyim* under *deney*, *yazılım* under *yazılı*, *kapsam* under *kapmak*; *işte* "at work" is
  read as the particle *işte* (115), and *hala* "aunt" is filed with *hâlâ* "still". They count as
  known at that rank, but they cannot be taught, and a headword is never taught through them
  (*kapmak* is not taught by *kapsamında*). *yıkamak* (781) owes its rank to forms of *yaşanmak*
  (*yaşanan*, *yaşandı*): teach it only through its own forms (*yıkıyor*, *yıkadı*). Use a new
  word in its own sense, never through its homograph. Write *7* rather than *yedi*.
- The question particle is four entries, one per vowel: *mi* (14), *mı* (29), *mu* (51), *mü*
  (356), each with its personal forms (*misin*, *mıyım*, *musun*); each is taught as a word of its
  own. Suffix fragments in the list (*te*, *ta*, *le*, *li*, *nun*) and bare verb stems that only
  repeat a verb (*araştır*, *değiş*, *düş*, *oluş*, *gönder*, *girdi*, *ilerle*, *düşür*) are not
  words to teach: skip them.
- Verbs made with *etmek* or *olmak* and a noun written apart (*teşekkür etmek*, *yardım etmek*,
  *devam etmek*, *kabul etmek*) are listed by their noun: teach *teşekkür* through *teşekkür
  ederim*, *yardım* through *yardım ediyor*. Those written as one word (*hissetmek*, *affetmek*,
  *bahsetmek*, *kaybetmek*, *halletmek*) are entries of their own.
- The checker compares whole words, lowercased the Turkish way (*I* is *ı*, *İ* is *i*), so write
  capitals as Turkish does (*Işık*, *İzmir*). It reads *â*, *î*, *û* as *a*, *i*, *u*. A suffix set
  off by an apostrophe goes with the word before it, which is counted alone (*Ali'nin* is *Ali*,
  *TDK'ya* is *TDK*); after digits (*7'de*, *1990'larda*) it belongs to the number, which is not
  counted. A word joined by a hyphen counts as its parts.
- A **name** goes in `names` once, without endings (*Ali*, *Ayşe*, *İstanbul*, *Ankara*): the
  checker accepts it with any suffix after the apostrophe (*Ali'ye*, *Ankara'dan*). A name of
  several words (*Kız Kulesi*, *Kapalıçarşı*, *Ayşe Yılmaz*) listed whole counts as one known word
  wherever it is written whole, with its suffix (*Kız Kulesi'ne*); a name listed whole hides its
  parts, so don't list one whose part is a band word you teach, and list a part on its own when
  the text uses it alone. Words made from a name without an apostrophe are words of their own
  (*İstanbullu*, *Ankaralı*); the list lacks capitalised words such as *Türkiye*, *Türkçe*,
  *İngilizce*, *Türk*, *Alman*: put them in `names` when a story needs them. A title after a name
  is a word: *Ayşe Hanım*, *Ali Bey*, *Mehmet Amca* (list *Ayşe*; *hanım* is 1002, *bey* 400,
  *amca* 1093).
- Keep only true names in `names`, never common words. Many Turkish first names are words
  (*Deniz*, *Can*, *Umut*, *Barış*, *Gül*, *Nur*, *Ay*, *Yıldız*, *Kaya*, *Bulut*) or list entries
  (*Elif*, *Zeynep*, *Ece*, *Kerem*, *Murat*, *Nehir*, *Yağmur*, *Defne*), and some are forms of
  words (*Emre* of *emir*, *Selin* of *sel*, *Arda*, *Oya*): don't use them. Names that are safe
  include *Ali*, *Ayşe*, *Mehmet*, *Ahmet*, *Fatma*, *Hasan*, *Ömer*, *Yusuf*, *Leyla*, *Esra*,
  *Merve*, *Burak*, *Cem*, *Ayla*, *Emine*, *Hatice*, *İbrahim*, *Halil*, *Zehra*, *Sinan*. Day and
  month names are in the list (*pazartesi*, *mayıs*); check holiday names.
- The list's form lists are noisy (spellings without the circumflex, misspellings, unrelated words
  of the same spelling); a new word must appear as itself or a true inflection, never through such
  a form. Some entries are names or words from foreign films (*jack*, *michael*, *george*, *the*),
  Turkish names and places (*kemal*, *aydın*, *fatih*, *murat*, *mısır*, *ırak*, *mersin*),
  abbreviations (*tl*, *km*, *chp*, *akp*, *trt*, *tbmm*, *dna*) or vulgar words (*sikmek*, *bok*,
  *lan*, *kahrolası*): skip them. Loanwords that Turkish uses as its own (*telefon*, *internet*,
  *futbol*, *otobüs*) are taught like any word.
