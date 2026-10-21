# Writing a Polish frequency course

Each course teaches one band of 100 words from the Polish word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `pl-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Ania chce mieć kota. Idzie do schroniska. Widzi tam trzy koty...* Everyday
  life in Poland: Warsaw, Kraków, Gdańsk, Gdynia and Sopot, Wrocław, Poznań, Łódź, Lublin,
  Białystok, Toruń, Katowice and Silesia, Zakopane and the Tatras, the Masurian lakes, the Baltic
  coast, Bieszczady, villages and allotments (*działka*); and now and then London, Berlin, Dublin,
  Oslo or Chicago, where Polish is spoken at home: home, family, food, shopping, work, school, the
  city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *on*/*ona* is whenever it could be
  unclear. Give each story its own names: the learner reads the course in one go, and two Kubas in
  two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order.
  Present tense up to rank 500, with the future of *być* (*będę*, *będzie*) and *chcieć/móc/musieć*
  + infinitive where natural; from rank 600 the story may be told in the past (*Ania wyszła z domu.
  Padał deszcz.*). Polish drops subject pronouns: write *on*, *ona*, *oni* only where Polish would
  (contrast, emphasis, a new subject). Little dialogue; when there is some, short and simple, with
  *ty* among family and friends and *pan*/*pani* + the third person with strangers, older people
  and in formal settings (*Czy pani tu mieszka?*).
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*bo*, *ponieważ*, *kiedy*, *gdy*, *chociaż*,
  *więc*, *dlatego*, *jednak*), dialogue can carry more of the story, and a story may be told by a
  first-person narrator or as an e-mail, a letter or a diary entry. Grammar grows with the bands,
  each band using what came before:
  - ranks 1–300: the present tense, the past tense of the commonest verbs (*był*, *była*, *miał*),
    the nominative, accusative and locative in plain phrases (*mam kota*, *w domu*, *na stole*),
    the genitive after negation and of possession (*nie ma czasu*, *kot Kasi*), possessives,
    simple questions (*kto*, *co*, *gdzie*, *kiedy*, *jak*, *czy*), negation, *chcieć/móc/musieć*
    + infinitive;
  - ranks 301–600: the past tense for events (*zrobiłem*, *poszła*), the future with *będę* +
    infinitive (*będę pracować*), the dative (*podoba mi się*, *dać bratu*), the instrumental
    (*z bratem*, *jest lekarzem*), the plural, the imperative (*zrób*, *chodź*, *powiedz*),
    *bo*, *jeśli*, *żeby* + infinitive, simple comparatives (*lepszy*, *więcej*);
  - ranks 601–1000: imperfective and perfective verbs side by side (*robić* / *zrobić*, the
    perfective future *zrobię*), the verbs of motion (*iść* / *chodzić*, *jechać* / *jeździć*, and
    with *przy-*, *wy-*, *w-*, *po-*, *do-*), reflexive verbs with *się*, *który* in the nominative
    and accusative, *niż* in comparisons, reported speech with *że* and *czy*, *zanim*, *kiedy*;
  - ranks 1001–1300: every case in the singular and the plural, numerals with nouns (*dwa domy*,
    *pięć domów*, *dwoje dzieci*), the masculine-personal plural (*ci studenci byli*, *te dziewczyny
    były*), comparatives and superlatives (*najlepszy*, *najwięcej*), *który* in every case, the
    conditional (*chciałbym*, *gdybym miał czas, pojechałbym*), *żeby* + past (*chcę, żebyś
    przyszedł*), *aż*, *dopóki nie*, *jak tylko*;
  - ranks 1301–1600: the indefinite pronouns (*ktoś*, *coś*, *ktokolwiek*, *gdziekolwiek*), verbs
    that take a case (*interesować się czymś*, *bać się czegoś*, *pomagać komuś*), *chociaż*,
    *mimo że*, *zamiast*, *oprócz*, *bez względu na*, the present adverbial participle
    (*czytając*, *idąc*);
  - ranks 1601–2000: adjectival participles in everyday use (*pracujący*, *napisana książka*,
    *zbudowany dom*), the impersonal forms (*mówi się*, *zrobiono*, *można*), indirect questions
    with *czy*, *jakby*, *nie tylko... ale także*, *ten, kto*, *jeden z*, *każdy z*;
  - ranks 2001–2500: participles in longer phrases, the perfective adverbial participle
    (*zrobiwszy*, for recognition), the past conditional (*gdyby wiedział, przyszedłby*),
    *im... tym*, verbal nouns (*otwarcie*, *budowanie*), *podczas gdy*, *wskutek*, *dzięki*,
    *zgodnie z*; reported speech keeping the original tense, as Polish does;
  - ranks 2501–3000: a richer, more formal register: connectors such as *niemniej jednak*,
    *zresztą*, *wobec*, *w związku z*, *ze względu na*, *odnośnie do*; long participial phrases;
    common idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the *felieton* (a light reflection on everyday life, as in Polish
    newspapers), dialogue that marks formal and informal speech, less common connectors (*byle*,
    *aby*, *mimo iż*, *wbrew*, *acz*);
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
  From rank 2000 a lesson may also be a short informative or opinion text (a *felieton*, a letter
  to a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Szuka w kuchni. Szuka w pokoju. Szuka w przedpokoju.*).
- **Questions at the end**: after the story, a paragraph starting `Pytania:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *dlaczego* and *jak*, with fuller answers):

  ```
  Pytania:
  Czego chce Ania?
  Ania chce mieć kota.
  Dokąd idzie Ania?
  Ania idzie do schroniska.
  ```

- **Standard Polish**: neutral, correct and natural Polish as written today, with all its
  agreements, cases and aspects; no slang, no vulgar words (the list has many: skip them). The
  stories are for readers of every age and country, so keep to the standard language and let a
  regional or colloquial word stand only where its context makes it clear. Always write the
  Polish letters (*ą ć ę ł ń ó ś ź ż*): the checker compares them exactly, and *zle* or *wiecej*
  are not words.
- **Polish typography**: direct speech is set out as Polish books write it, each speaker's line a
  paragraph of its own beginning with an em dash and a space:

  ```
  — Cześć, Aniu! — mówi Tomek.
  — Cześć! — odpowiada Ania.
  ```

  The questions and answers of the final `Pytania:` block are plain lines, as above. Polish
  quotation marks are „…” (a low opening mark and a high closing one): *Napisała: „Dziękuję”.*
  Hyphens inside words stay hyphens; the em dash is the long one (—). Write numbers as digits
  (*7 godzin*, *o 7:30*, *3 braci*), except a number word that is in your band: spell that one
  out to teach it (*pięć dni*). Centuries and kings take Roman numerals (*XIX wiek*, *Jan III
  Sobieski*), which count as numbers.
- **Suitable for all ages**: no drugs or alcohol as a topic, no sex, violence, war, cruelty or
  gore. Keep away from politics, the army, the state's doings and nationalism, from current
  conflicts, and from religion taken sides on; everyday civic life (a library, a school board, a
  housing community meeting) and the customs of the holidays (*Wigilia*, Easter baskets) are fine.
  Skip band words that only fit such topics, and say so in your report.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## The file

Write `tools/courses/pl/<id>.json`:

```json
{
  "id": "pl-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Polish, e.g. Jedzenie i zakupy",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, in every form the texts use"],
  "lessons": [
    {
      "title": "Story title in Polish",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/pl/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *szła* for *iść*, *dobrą* for *dobry*). Reuse earlier
  lessons' new words too. `newWords` holds the word as the list writes it: the infinitive of a
  verb, the nominative singular of a noun, the masculine singular of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a letter, an abbreviation, a vulgar word, a list error such as a name or an
  English word); the checker's last line lists the band words not taught yet.

Things to know about the Polish list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/pl/wordlist.tsv`
  (a copy of the app's `pl.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- The list files the forms of a word under its headword: the cases of nouns, adjectives and
  pronouns, the conjugation and the participles of verbs, the comparative and superlative of
  adjectives (*lepszy*, *najlepszy* under *dobry*), and often words made from it: diminutives
  (*domek* under *dom*, *dziewczynka* under *dziewczyna*) and adjectives made from nouns
  (*miejski* under *miasto*, *polityczny* under *polityka*). Such a word counts at its headword's
  rank, but it is not that word: never teach a headword through it. Adverbs are entries of their
  own (*dobrze*, *szybko*, *długo*), and so are *może* ("maybe", next to *móc*), *wszyscy* and
  *wszystkie* (next to *wszystko*), *one* (next to *oni*) and *dane* ("data"). *jaki* is filed
  under *jak*.
- Perfective and imperfective verbs are mostly entries of their own (*robić* 91, *zrobić* 86;
  *mówić*, *powiedzieć*), so teaching one does not teach the other. A few pairs are one entry,
  filed under the rarer partner: *latać* holds the forms of *lecieć*, *zaprosić* of *zapraszać*,
  *zaplanować* of *planować*, *polecić* of *polecać*, *powitać* and *przywitać* of *witać*,
  *zainteresować* of *interesować*, *zamierzyć* of *zamierzać*, *pozdrowić* of *pozdrawiać*,
  *pogratulować* of *gratulować*; for such an entry use the forms Polish actually uses
  (*zamierzam*, *zapraszam*), which count for it. `zostaje` is *zostawać* (982), not *zostać*
  (51): *zostać* has no present tense.
- Some headwords are not the word you would expect, and some hold two words: *lewa* is *lewy*
  ("left", with *w lewo*) and also *lew* ("lion"); *kary* is *kara*; *woli* is *wola* and
  *woleć*; *marek* is *marka*; *przybycie* holds *przybyć*; *technik* and *technikum* hold
  *technika*; *dana* holds *danie* ("dish") and *dany*; *kur* holds *kura* and *kogut*; *łaski*
  is *łaska*; *spory* holds *spór*; *mrzeć* holds *zmarł*. Teach the real word in its own sense,
  writing the headword in `newWords` as the list does. Some entries are only the second half of a
  set phrase: *prostu* (*po prostu*), *pewno* (*na pewno*), *ogół* (*w ogóle*), *wzgląd* (*ze
  względu na*), *szczególność* (*w szczególności*); teach them in that phrase. Entries written
  with an underscore (*tak_zwany*, *i_tym_podobne*, *do_spraw*, *i_tak_dalej*, *na_temat*) stand
  for abbreviations (*tzw.*, *itp.*, *ds.*, *itd.*): skip them.
- A few entries are only a form of a commoner word, linked to it in `extra_forms.tsv` so the
  checker counts them for it: *duże* (*duży*), *złe* (*zły*), *młodych*, *materiały*, *imprezy*,
  *planet*, *strefie*, *ludu*, *podlega*, *pali*, *dba*, *nóg*, *zimne*, *lisa*, *rakiety*,
  *nieznane*, *grosze*, *krewnych*, *korytarzu*, *ręczna*, *ambicje*, *cyfry*, *litra*,
  *awaryjne*, *krzaki*, *niewolnicy*, *pomarańcze*, *popadnie*. Skip such a band entry; an
  earlier course has its word.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). Most of it is
  generated from the Polish Wiktionary dictionary (`dictionaries/pl-en.sqlite`): forms such as
  *bądźcie*, *byłabyś*, *miałabyś*, *lepszą*, *dziewczynki*, *Polsce*. Add a line there for
  another true form you need rather than writing around it, and say so in your report.
- The checker compares whole words, lowercased, letter for letter, and splits a word at hyphens
  (*polsko-niemiecki* is *polsko* + *niemiecki*; *e-mail* is *e* + *mail*, so write *mejl* or
  *mail*). `Pytania:` counts as the word *pytania* (*pytanie*, rank 208).
- A form shared by two words counts for both (*lat* for *rok* and *lato*, *mieszka* for
  *mieszkać* and *miesiąc*, *dania* for *dać* and *dana*), or only for the one the list files it
  under (*mamy* only for *mieć*, never for *mama*; *stanie* only for *stać*): use a new word in its
  own sense, never through its homograph, and count a new word's uses in forms that are its own.
- **Names** are not in the list: people, places (*Polska*, *Warszawa*, *Kraków*), shops and
  brands go in `names`. The checker does not decline them, so list every form the text uses:
  *Ania*, *Ani*, *Anię*, *Anią*, *Aniu*; *Kraków*, *Krakowa*, *Krakowie*; *Piotr*, *Piotra*,
  *Piotrze*. A name of several words or with a hyphen (*Zielona Góra*, *Bielsko-Biała*,
  *Morskie Oko*) counts as one known word where it is written whole, so list each form whole
  (*Zielona Góra*, *Zielonej Górze*); a name listed whole hides its parts, so don't list one whose
  part is a band word you teach. *Polska* and its forms *Polski*, *Polską*, *Polsce*, *Polskę*
  are already counted (under *polski*): don't put them in `names`, where *polski* and *polską*
  would hide the adjective. Keep only true names there, never common words, and never a name
  that is also a word (*Róża*, *Jagoda*, *Malina*, *Nadzieja*; *Marek* would hide *marek*, the
  list's entry for *marka*): then call the character something else. A few first names are list
  entries of their own (*Piotr* 1534, *Ola*, *Maks*, *Antek*, *Bogdan*, *Wiktoria*; *Zosia* under
  *zofia*): put them in `names` all the same, and skip them as band words. Day and month names are in the list
  (*maj* 345, *styczeń* 572, *piątek* 1024, *sobota* 1331, *wtorek* 2254: check their ranks);
  so are *święta*, *wigilia* and *Boże Narodzenie* (*boże* under *bóg*, *narodzenie* 2778).
  *Wielkanoc* and *internet* are missing from the list.
- The list's form lists are noisy (old and dialect spellings, spellings without Polish letters,
  unrelated words); a new word must appear as itself or a true inflection, never through such a
  form. Some entries are names, abbreviations or English words: skip them. Loanwords that Polish
  uses as its own (*komputer*, *film*, *mejl*, *weekend*) are taught like any word.
