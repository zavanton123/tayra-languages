# Writing an Azerbaijani frequency course

Each course teaches one band of 100 words from the Azerbaijani word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `az-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 8,338 words, so the last course is
`az-mini-8400` (ranks 8301–8338).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ayselin bir pişiyi var. Bu gün Aysel onu heç yerdə görmür...* Everyday
  life in Azerbaijan: Bakı (İçərişəhər, the bulvar, the metro, a *həyət* of a Soviet block),
  Gəncə, Sumqayıt, Şəki, Quba, Lənkəran, Qəbələ, Naxçıvan, the Abşeron villages and their summer
  *bağ* houses, the Caspian shore; and now and then Azerbaijani families in Moscow, İstanbul,
  Tbilisi or Berlin: home, family, food (*plov*, *dolma*, *çay* with jam), shopping, work, school,
  the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Azerbaijani *o* is "he", "she", "it" and
  "that", so say who *o* is whenever it could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb last). The present in *-ır/-ir/-ur/-ür* up to rank 500 (*Kamran evə
  gəlir.*), with *var/yox*, *-maq istəyir* and the past in *-dı* of single finished events where
  Azerbaijani needs it (*Dünən evə gəldi.*); from rank 600 the story may be told in the past
  (*Aysel evdən çıxdı. Yağış yağırdı.*). Little dialogue; when there is some, short and simple,
  with *sən* among family and friends and *siz* with strangers, older people and in formal
  settings (and *Aysel xanım*, *Rauf müəllim*, *Kamran bəy*, *əmi*, *xala* as Azerbaijanis
  address people).
- **Growing difficulty after rank 1000**: the stories stay standalone and everyday, but sentences
  get longer and joined (*çünki*, *amma*, *və*, *-anda*, *-ıb*, *ona görə ki*, *buna görə*,
  *halbuki*), dialogue can carry more of the story, and a story may be told by a first-person
  narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands, each band
  using what came before:
  - ranks 1–300: the present in *-ır*, *var* and *yox*, sentences without a verb with the copula
    *-dır* and the personal endings (*Kamran müəllimdir.*, *Mən evdəyəm.*) and *deyil*
    (*Mən uşaq deyiləm.*), the cases in plain phrases: the accusative (*evi görür*), the dative
    (*evə gedir*), the locative (*evdə*), the ablative (*evdən çıxır*), the genitive with the
    possessive (*Kamranın evi*, *mənim evim*), the possessive endings (*evim*, *evin*, *evi*), the
    plural *-lar/-lər*, questions with *nə*, *kim*, *harada*, *hara*, *necə*, *niyə*, *neçə*,
    *hansı* (and by intonation: *Sən gəlirsən?*), negation with *-ma-* (*gəlmir*), *-maq istəyir*,
    *ilə* and *-la/-lə* (*Aysel ilə*, *Ayselə*... *atamla*), *üçün*, *kimi*, the past in *-dı* of
    the commonest verbs (*gəldi*, *dedi*, *tapdım*), *-a bilir* (*ala bilmir*);
  - ranks 301–600: the past in *-dı* for events, the future in *-acaq/-əcək*, the aorist for
    habits and offers (*Hər səhər çay içər.*, *Kömək edərsən?*), the imperative (*gəl*, *gəlin*,
    *gedək*), *lazımdır*, *gərək* and *-malı* (*getməliyəm*), *çünki*, *sonra* and *əvvəl*
    (*yeməkdən sonra*, *getməmişdən əvvəl*), *-anda* (*evə gələndə*), comparisons with *daha* and
    *ən* (*-dan daha böyük*, *ən gözəl*);
  - ranks 601–1000: the past in *-mış* for what one hears or finds (*Kamran gəlmiş.*), the perfect
    in *-ıb* (*gəlib*, *gedib*), *-kı* (*evdəki*, *sabahkı*), the conditional *-sa* (*gəlsə*,
    *istəsən*), the converbs *-ıb*, *-araq*, *-madan*, *-dıqdan sonra*, the participle in *-an*
    (*gələn adam*), *-maq üçün*, compound nouns (*çay stəkanı*, *məktəb çantası*), reflexive and
    reciprocal verbs (*geyinmək*, *görüşmək*);
  - ranks 1001–1300: the participles in *-dıq* and *-acaq* with the possessive (*oxuduğum kitab*,
    *gedəcəyimiz yer*), reported speech with *-dığını dedi* and *deyə*, *-dığı üçün*, the verbal
    noun in *-ma* with the possessive (*onun gəlməsi*, *gəlməsini istəyirəm*), the passive
    (*edilir*, *satılır*) and the causative (*etdirmək*, *gətirmək*), the past conditional
    (*gəlsəydi*, *kaş*), *-dıqca*;
  - ranks 1301–1600: the indefinite pronouns (*biri*, *bir şey*, *heç nə*, *hamı*, *heç kim*),
    *-masına baxmayaraq*, *-a görə*, *-dan bəri*, *-ana qədər*, *həm... həm də*, *nə... nə də*,
    *ya... ya da*, *-mış kimi*, *-sa da*;
  - ranks 1601–2000: the converbs in everyday use (*-araq*, *-ıb*, *-madan*, *-dıqca*), *-maqda*,
    the *-dır* of general statements, the passive in descriptions (*edilir*, *istifadə olunur*),
    indirect questions (*gəlib-gəlmədiyini bilmirəm*), *-dıqda*, *-mək üzrə*;
  - ranks 2001–2500: longer participial phrases, the past conditional in full (*bilsəydi
    gələrdi*), *-mış olmaq*, verbal nouns (*-ma*, *-ış*, *-lıq*), connectors such as *buna görə də*,
    *digər tərəfdən*, *buna baxmayaraq*, *üstəlik*; reported speech with the tense of the
    original kept;
  - ranks 2501–3000: a richer, more formal register: *-maqdadır*, *-mışdır*, connectors such as
    *belə ki*, *halbuki*, *-a qarşı*, *-a dair*, *-la bağlı*; long participial phrases; common
    idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *köşə yazısı* (a light column on everyday life), dialogue that marks
    *sən* and *siz*, less common connectors (*madam ki*, *sanki*, *-casına*);
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
  - ranks 8001–8338 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, learned words (Arabic and Persian loans of the
    written language) explained by their context. Still standalone, calm and easy to follow.
  From rank 2000 a lesson may also be a short informative or opinion text (a *köşə yazısı*, a
  letter to a newspaper, a short report), as long as it is calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*O, evə baxır. O, yola baxır. Nərmin hər şeyə baxır.*).
- **Questions at the end**: after the story, a paragraph starting `Suallar:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *niyə* and *necə*, with fuller answers):

  ```
  Suallar:
  Aysel nəyi görmür?
  Aysel pişiyi görmür.
  Pişiyi kim tapır?
  Pişiyi Nərmin tapır.
  ```

- **Standard Azerbaijani**: neutral, correct and natural Azerbaijani as written in Azerbaijan
  today, in the Latin alphabet (ə, ı, ö, ü, ğ, ş, ç, q, x; capital *İ* for *i* and *I* for *ı*),
  with its vowel harmony (*evlər*, *uşaqlar*; *evdə*, *yolda*), consonant changes (*kitab*,
  *kitabı*; *uşaq*, *uşağı*; *çörək*, *çörəyi*) and suffix order right in every word. Not
  Turkish: *gəlir*, not *geliyor*; *üçün*, not *için*; *qız*, *qapı*, *xeyr*, *çox sağ ol*; and
  no Russian loans where Azerbaijani has its own word in the standard (*dəftər*, not *tetrad*).
  The question particle *-mı/-mi* is written onto the word (*Gəlirsənmi?*), but a plain question
  by intonation is more natural in the early courses (*Sən gəlirsən?*). *da/də* "too, also" is
  written apart (*Mən də gəlirəm.*, *Onun atası da evdədir.*) and the locative joined (*evdə*,
  *yolda*). The stories are for readers of every country: keep to the standard language, and let
  a regional or colloquial word stand only where its context makes it clear.
- **Azerbaijani typography**: direct speech is set out as Azerbaijani books do it, each speaker's
  line a paragraph of its own beginning with an em dash and a space, the speaker after a second
  dash:

  ```
  — Salam, Aysel! — Kamran deyir.
  — Salam! — Aysel deyir.
  ```

  The questions and answers of the final `Suallar:` block are plain lines, as above; a quoted
  sentence inside an answer goes in double quotation marks (*Aysel deyir: "Artıq yemək
  vaxtıdır!"*). Proper names take their endings straight on, with no apostrophe (*Kamranın*,
  *Ayselə*, *Bakıda*, *Fransadan*). Write numbers as digits (*3 uşaq*, *saat 7-də*, *1990-cı
  ildə*), except a number word that is in your band: spell that one out to teach it (*beş gün*).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(Novruz and the other holidays, religion, history, the khans and poets, folk tales, politics,
football). Avoid only explicit sexual content, slurs used as insults and gratuitous gore. Teach
ordinary words in their main sense. Skip list noise: fragments, abbreviations, names and words of
other languages, and the subtitle swear words (*pox*, *göt*, *qancıq*).

## The file

Write `tools/courses/az/<id>.json`, the shape of `az-mini-0100.json`:

```json
{
  "id": "az-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Azerbaijani, e.g. Yemək və alış-veriş",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Azerbaijani",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after that `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/az/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%). `Suallar` is counted too
  (as *sual*, rank 296), so in the first courses it costs one outside word per lesson.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *gedir* for *getmək*, *evdə* for *ev*). Reuse earlier
  lessons' new words too. `newWords` holds the word as the list writes it: the infinitive of a
  verb (*getmək*), a noun or adjective without endings (*ev*, *böyük*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a fragment, an abbreviation, a vulgar word, a list error such as a name or a
  foreign word); the checker's last line lists the band words not taught yet.

Things to know about the Azerbaijani list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/az/wordlist.tsv`
  (a copy of the app's `az.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list was counted from news (Leipzig's 2013 news crawl), film subtitles (OpenSubtitles via
  OPUS) and a little Wikipedia, so it is rich both in the press's words (*dövlət* 77, *ölkə* 55,
  *prezident*, *nazir*, *rayon*) and in spoken ones (*hə*, *salam*, *məncə*, *yəni*), and some
  everyday words rank later than you might expect (*ata* is 88, *ana* 111, *qardaş* 183, *su*
  225, *kitab* 280, *çay* 481, *həkim* 583, *pişik* 955, *çörək* 1287, *masa* 1328, *mətbəx*
  1461, *stul* 1607, *xala* 1782). Check a word's rank rather than guess it.
- The list files a word's endings under its headword: the cases and possessives of nouns, the
  tenses and persons of verbs. It keeps at most 40 forms of a word, so `extra_forms.tsv` adds the
  inflections the English Wiktionary gives for the list's words that the news and subtitles use. A
  word counts at the rank of its headword, whatever form the story uses; a form neither file has
  counts as an outside word. If a true form is missing, add it to `extra_forms.tsv` (a line with
  the headword, a tab and the forms, appended at the end), keep it as an outside word, or write
  around it, and say so in your report.
- The checker also reads the endings Azerbaijani writes onto any word: the copula and the personal
  endings (*böyükdür*, *müəllimdir*, *evdəyəm*, *uşaqsan*, *buradayıq*, *evdəydi*, *müəllimmiş*),
  the question particle (*gəlirsənmi*), and before them *-la/-lə* "with" and *-kı/-ki*
  (*bizimlə*, *onunla*, *evdəki*): such a word counts as the listed form before them. So *-la*
  "with" written on (*atamla*, *bizimlə*) counts for *ata*, *biz*, never for *ilə*: teach *ilə*
  through the word written apart (*Aysel ilə*).
- The headwords of *burada*, *orada*, *harada* are the stems *bura* (48), *ora* (267) and *hara*
  (196): write `newWords` as *bura*, *ora*, *hara*. *də* (9) holds *da*. The copula verb has three
  headwords: *imək* (29) holds *deyiləm*, *deyildi*, *imiş*, *isə*, *ikən*; *idi* (43) and *deyil*
  (47) are entries of their own. The postpositions are words of their own (*üçün* 15, *ilə* 21,
  *kimi* 28, *görə* 31, *qədər* 44), and so are *tərəfindən* "by" (133), *haqqında* and *arasında*.
- The personal pronouns are each their own headword with their own case forms: *mən* (*mənim*,
  *mənə*, *məni*), *sən*, *o* (*onun*, *ona*, *onu*, *onda*, *ondan*), *biz*, *siz*, *onlar*. The
  *ad* entry (69, "name") also holds forms of *ada* "island" and *adam*: teach *ad* through *adı*,
  *adım*, *adın*.
- A form shared by two words counts for both, and some forms belong to a homograph: *atam* is
  *ata*'s and *atmaq*'s, *qala* is "castle" and *qalmaq*'s "may stay", *yaz* "spring" and "write!",
  *açar* "key" and "opens", *altı* "six" and *alt*'s "its bottom". Common nouns spelled as a verb's
  form are entries of their own (*gül* "flower", *at* "horse", *yataq* "bed", *ölüm* "death",
  *seçim*, *dayanacaq*, *keçmiş*, *iç*), as are *hissə* "part" (not *hiss*'s) and *tələbə*
  "student" (not *tələb*'s). Use a new word in its own sense, never through its homograph: never
  teach *atmaq* through *atam*, or *qalmaq* through *qala* "castle".
- Verbs made with *etmək* or *olmaq* and a noun written apart (*kömək etmək*, *təşəkkür etmək*,
  *qəbul etmək*, *davam etmək*, *istifadə etmək*) are listed by their noun: teach *kömək* through
  *kömək edir*, *təşəkkür* through *təşəkkür edirəm*.
- The checker compares whole words, lowercased the Azerbaijani way (*I* is *ı*, *İ* is *i*), so
  write capitals as Azerbaijani does (*İlham*, *İçərişəhər*, *Işıq*). An apostrophe inside a word
  keeps it whole (*Qur'an*); a word joined by a hyphen counts as its parts (*alış-veriş* is *alış*
  + *veriş*).
- A **name** goes in `names` once, without endings (*Kamran*, *Aysel*, *Bakı*, *Şəki*): the
  checker accepts it with its endings written on (*Kamranın*, *Ayselə*, *Bakıda*, *Şəkidən*,
  *Bakılı*, *Ayseldir*). A name of several words (*Qız Qalası*, *Aysel Məmmədova*) listed whole
  counts as one known word wherever it is written whole; a name listed whole hides its parts, so
  don't list one whose part is a band word you teach. The list lacks the capitalised *Azərbaycan*,
  *Bakı*, *Rusiya*, *Türkiyə*: put them in `names` when a story needs them; *azərbaycanlı*, *türk*,
  *rus*, *ingilis* are list words. A title after a name is a word: *Aysel xanım* (203), *Kamran
  bəy* (410), *Rauf müəllim* (361).
- Keep only true names in `names`, never common words. Many Azerbaijani first names are words or
  their forms (*Arzu* 515, *Sevinc*, *Könül*, *Mehriban*, *Fidan*, *Nərgiz*, *Ülvi*, *Lalə*,
  *Gül*, *Aydın*, *Səfər*, *İlham*, *Əli* — a form of *əl*, *Elçin* — of *elçi*, *Gəncə* — of
  *gənc*): don't use them. Names that are safe include *Kamran*, *Aysel*, *Nərmin*, *Tural*,
  *Rauf*, *Orxan*, *Rəşad*, *Fərid*, *Səbinə*, *Zaur*, *Samirə*, *Nigar*, *Ramil*, *Vüqar*,
  *Elnur*, *Cavid*, *Elvin*, *Nurlan*, *Aytən*, *Elmar*, *Pərvin*, *Rövşən*, *Emil*, *Rəna*,
  *Kənan*, *Günel*, *Leyla*. Day and month names are in the list (*bazar ertəsi*, *may*,
  *yanvar*): teach them.
- The list's form lists are noisy (Turkish spellings such as *olacak*, old spellings with an
  apostrophe such as *bə'zi*, unrelated words of the same spelling); a new word must appear as
  itself or a true inflection, never through such a form. Some entries are names or words from
  foreign films, names (*aydın*, *ilham*, *səfər* are also words: teach the word), a phrase
  (*kitab əhli*) or vulgar words: skip those that are only noise. Loanwords that Azerbaijani uses
  as its own (*telefon*, *internet*, *futbol*, *avtobus*, *problem*) are taught like any word.
