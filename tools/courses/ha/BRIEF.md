# Writing a Hausa frequency course

Each course teaches one band of 100 words from the Hausa word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ha-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 4,996 words, so the last course is
`ha-mini-5000` (96 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Binta tana neman wayarta. Ta je ɗakin girki. Ba ta gan ta ba. Ta je falo...*
  Everyday life where Hausa is spoken: Kano, Kaduna, Zaria, Katsina, Sokoto, Bauchi, Gombe, Jos,
  Maiduguri, Yola, Abuja, Lagos; Niamey, Maradi, Zinder, Agadez, Tahoua in Niger; Accra and the
  zongo quarters of Ghana; the markets (Kasuwar Kurmi, Kasuwar Sabon Gari), the Kano city walls,
  Gidan Makama, the Emir's palace, the Durbar of Sallah, the River Niger, Lake Chad, the Sahel; now
  and then London, Jeddah or Khartoum, where Hausa speakers live: home, family, food, the market,
  work, school, the town, health, free time, travel. Any everyday, cultural or story topic is fine
  (festivals and holidays, history, legends, sport, love, a detective story); only explicit sexual
  content, slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *shi*/*ita* is whenever it could be unclear. Give each
  story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Hausa
  order: subject, the person-aspect word, verb, object (*Audu yana karanta littafi.*), the noun
  before its possessor and most qualifiers (*gidan Audu*, *motar Binta*, *gida babba*), but the
  linked adjective before it (*babban gida*, *babbar mota*). Little dialogue; when there is some,
  short and simple.
- **The person-aspect words carry the verb**: a verb takes a separate word for its subject and
  aspect, written apart: continuous *ina, kana, kina, yana, tana, muna, kuna, suna, ana* (*yana
  karatu*), completive *na, ka, kin, ya, ta, mun, kun, sun, an* (*ya zo*), the relative completive
  of narration and questions *na, ka, kika, ya, ta, muka, kuka, suka, aka* (*da ya zo, sai ya ...*;
  *me suka saya?*), future *zan, za ka, za ki, zai, za ta, za mu, za ku, za su, za a*, habitual
  *nakan, yakan, takan*, subjunctive *in, ka, ki, ya, ta, mu, ku, su, a*. Each is a word of the list.
  Write them apart as the standard does: *ya ce*, *za su*, *shi ne*, *ba ta*, *a nan*, *da ke*,
  *abin da* (never *yace, zasu, shine, bata, anan, dake, abinda*).
- **Gender and the linker**: a noun is masculine or feminine (most feminine nouns end in *-a*:
  *mota, makaranta, rana*); the possessive linker is *-n* after a masculine or plural noun and *-r*
  after a feminine one (*gidan Audu*, *motar Binta*, *littattafan yara*), and *na*/*ta* after a
  pause (*littafin nan na Audu ne*). Possessive endings follow the linker: *gidana, gidanka,
  gidanki, gidansa, gidanta, gidanmu, gidanku, gidansu*; *motata, motarka, motarsa, motarta*. Get
  every gender right, in *ne*/*ce*, *wannan*/*waccan*, the linker and the adjective.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *ne*/*ce* sentences (*Audu malami ne. Binta malama ce.*), the continuous and the
    completive (*yana karatu*, *ya zo*), the future (*zai tafi*), *akwai*/*babu* "there is/is not",
    negation *ba ... ba* (*ba ya nan*, *bai zo ba*, *ba zan tafi ba*), the linker and the
    possessive endings, *wannan*, *waccan*, *waɗannan*, numbers *ɗaya* to *goma*, questions with
    *me/mene ne*, *wa/wane/wace*, *ina* "where", *yaushe*, *nawa*, *yaya*, *me ya sa*;
  - ranks 301–600: the relative completive in narration (*da ya isa gida, sai ya ci abinci*), the
    subjunctive and the imperative (*zo!*, *ka zauna*, *ina so in tafi*, *kada ka manta*), object
    pronouns (*ya gan shi*, *ta kira ni*) and indirect objects (*ya ba ni*, *ta faɗa masa*),
    plurals, adjectives with the linker (*babban gida*, *sabuwar mota*), relative clauses with *da*
    (*mutumin da ya zo*, *abin da ta saya*);
  - ranks 601–1000: verbal nouns (*karatu*, *zuwa*, *tafiya*, *ganin*), the verb forms before an
    object (*ya sayi mota*, *ya saya*), comparison with *fi* (*Audu ya fi Musa tsayi*), the
    habitual (*yakan tashi da wuri*), the relative continuous (*abin da yake so*, *inda suke
    zaune*), conjunctions (*saboda*, *domin*, *amma*, *idan*, *kafin*, *bayan*, *har*);
  - ranks 1001–2000: reported speech with *cewa* (*ya ce mini cewa ...*), conditionals with
    *idan/in ... da*, the negative continuous (*ba ya zuwa*), statives (*zaune*, *tsaye*,
    *kwance*), longer joined sentences;
  - ranks 2001–4000: a richer register: connectors (*duk da haka*, *saboda haka*, *a gefe guda*,
    *bugu da ƙari*), the newspaper column, dialogue, proverbs (*karin magana*) explained by their
    context;
  - ranks 4001 and up (C1): everything before, with more abstract and nuanced content: a short essay
    that weighs two sides, an interview, a portrait, a review; the specialised words of professions,
    sciences and the arts, each made clear by its context.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as a letter or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ta je ɗakin girki. Ba ta gan ta ba. Ta je falo. Ba ta gan ta ba.*).
- **Questions at the end**: after the story, a paragraph starting `Tambayoyi:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *me ya sa* and *yaya*, with fuller answers):

  ```
  Tambayoyi:
  Me Binta take nema?
  Binta tana neman wayarta.
  Ina wayar take?
  Wayar tana ɗakin girki.
  ```

  The header `Tambayoyi:` is layout: the checker does not count it as a word.
- **Standard Hausa spelling (Boko)**: write the hooked letters *ɓ ɗ ƙ* (*ɗan, ƙasa, ɓata*) — never
  the plain *b d k* for them, which is another word or none (*kasa* "fail" is not *ƙasa* "land") —
  and *'y* with the apostrophe (*'ya, 'yan, 'yar, 'ya'ya*). The reader and the checker read *ƴ*,
  *'y* and the curly *’y* alike, and *k'*, *'d*, *'b* as *ƙ, ɗ, ɓ*, but write the standard ones. Keep
  the glottal stop's apostrophe inside words (*sa'a, jama'a, al'ada, ma'aikaci*). No tone or length
  marks, no Ajami (Arabic letters). The narration is the standard written language of Kano as
  newspapers and radio use it (*ina so*, *yanzu*, *kuma*): no slang, no English where Hausa has its
  own word (*waya*, not *fon*; *makaranta*, not *skul*).
- **Hausa typography**: direct speech in double quotation marks "…", each speaker's line a
  paragraph of its own. Write numbers as digits (*yara 3*, *ƙarfe 8 na safe*) except a number word
  that is in your band: spell that one out to teach it (*yara uku*). Avoid hyphenated words: the
  reader cuts them at the hyphen (*fina-finai* is *fina* + *finai*), so a reduplicated word counts
  twice as its halves.
- **Names are names**: Hausa names and places go in `names`: *Audu, Binta, Hauwa, Hadiza,
  Zainab, Halima, Bilkisu, Asabe, Ladi, Kabiru, Lawal, Garba, Bello, Usman, Ibrahim, Yusuf,
  Abdullahi, Danjuma, Nafisa, Maryam, Aminu, Tanko*. Never use a list word as a name: *Sani*
  ("know"), *Musa* ("deny"), *Kyauta* ("gift"), *Albarka*, *Salama*, *Rabi* ("half"), *Safiya*
  ("morning"), *Rakiya* ("escort"), *Isa* ("arrive"), *Inuwa* ("shade"), *Shehu* (check a name
  against the list first). Words for peoples and languages (*Bahaushe*, *Hausawa*, *Turanci*,
  *Larabci*) count as words at their rank: never teach them as band words, a text may still use one.
  *Allah* is a name.
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Hausa list

- The list is counted from Tatoeba's and Common Voice's everyday sentences, Leipzig's news corpus
  (VOA, Leadership, CRI) and the Hausa Wikipedia. Most news and web text drops the hooks (*dan*,
  *kasa*, *daya* for *ɗan*, *ƙasa*, *ɗaya*): the list counts such spellings for the hooked word and
  lists the hooked spellings only.
- Headwords are written as the English Wiktionary writes them, without tone marks, with the hooked
  letters and *ƴ* for *'y* (*ƴa*, *ƴan*: write *'ya*, *'yan* in the text). Nouns hold their
  plural, linked forms and possessives (*gida* holds *gidaje, gidan, gidansa, gidajensu*; *mace*
  holds *matar*); verbs their forms before an object and the verbal noun's linker (*gani* holds
  *ganin*; *saya* holds *sayi*).
- The person-aspect words are entries of their own (*yana*, *suna*, *zai*, *suka*, *aka*): teach
  each as a word. Some headwords join words written alike: *suna* is "name" and "they are";
  *ya* "he (did)" and "elder sister"; *ta* "she (did)" and "of"; *ba* "not" and "give"; *ga*
  "to" and "look!"; *kan* "on" and the habitual. Teach a word in its common sense.
- About 40% of the first 1,000 words (*na, ne, ce, za, kuma's* friends *cewa, akwai, kamar, don*)
  have no entry in the offline dictionary, which Wiktionary and Wikidata built: the list keeps them
  by their counts. Teach them as any word.
- The checker reads a possessive or linked form the list lacks by the rules the list was built
  with: *gidanmu* counts as *gidan*, *gonar* as *gona*. Only words with no such reading are outside
  words.
- Some entries are Wikipedia or news words (*jiha*, *gwamnati*, *majalisa*, *jam'iyya*): fine in a
  story when they fit. Skip noise (fragments such as *fina*, *finai*, *zanga*).
