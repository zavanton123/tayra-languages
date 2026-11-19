# Writing a Hawaiian frequency course

Each course teaches one band of 100 words from the Hawaiian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its other spellings and forms). Course `haw-mini-0300`
teaches ranks 201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of
the band's words, so the course covers the band; a learner who finishes it knows the 300 most common
words. `themes.tsv` gives each course its theme. The list has 1,598 words, so the courses go up to
`haw-mini-1600`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories":
plain, clear and predictable, so the learner's attention goes to the language, not the plot. A course
is ten separate stories on one broad theme (its title), not one serial story; a lesson never needs
another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Makemake ʻo Kainoa i kāna puke. Aia ka puke i hea? Hele ʻo ia i ka lumi. ʻAʻole
  ʻo ia i ʻike...* Everyday life where Hawaiian is spoken: the islands of Hawaiʻi, Maui, Oʻahu, Kauaʻi,
  Molokaʻi, Lānaʻi and Niʻihau; Honolulu, Kailua, Kāneʻohe, Lāʻie, Haleʻiwa, Waiʻanae, Kapolei, Mānoa,
  Kalihi; Wailuku, Kahului, Lāhainā, Makawao, Pāʻia; Līhuʻe, Hanalei, Waimea; Kaunakakai; Kailua-Kona,
  Waipiʻo, Pāhoa, Kaʻū; and now and then Las Vegas, Seattle, California or Aotearoa, where many
  Hawaiians live. Any everyday, cultural or story topic is fine (WRITER-BRIEF step 4): home, family,
  food, school (the *kula kaiapuni*, the Hawaiian-medium schools), work, the sea and the land, hula and
  music, history and the aliʻi, legends, sport, love, holidays.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, wordplay jokes or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  *ʻo ia* is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence. Little dialogue; when
  there is some, short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the verb-first sentence with its aspect markers: *ua* + verb for what happened
    (*Ua hele ʻo Kainoa i ke kula.*), the bare verb for habits and general truths (*Noho ʻo Malia ma
    Kailua.*), *ke* + verb + *nei* for what is going on now (*Ke kākau nei ʻo ia i kekahi moʻolelo.*),
    *e* + verb + *ana* for what is going on or will happen (*E hele ana lāua i Maui.*); *ʻo* before a
    name or pronoun that is the subject (*Hele ʻo Keoni*, *ʻO ia*), *i* (*iā* before a name or pronoun)
    for the object and for "to" (*ʻike ʻo ia iā Malia*, *hele i ke kula*), *ma* for "at, in", *me*
    "with", *no* "for, about, from", *o* "of"; the articles *ka*/*ke* (see below), plural *nā*, *he*
    "a"; the sentences without a verb: *He kumu ʻo Malia.* "Malia is a teacher", *ʻO Keola kona inoa.*
    "His name is Keola", *Aia ʻo Keola ma ka hale.* "Keola is at home"; stative verbs as adjectives
    (*Nui ka hale.* "The house is big"; *he hale nui* "a big house"); the possessives *kona*/*kāna*,
    *ko*/*kā* (*ko Keola hale*, *kā Malia puke*) with the a/o rule (see below); the directionals
    *mai*, *aku*, *aʻe*, *iho*; *makemake* and *hiki* + *ke* + verb (*makemake ʻo ia e hele*, *hiki iā
    ia ke heluhelu*); *ʻaʻole* with the pronoun or name before the verb and *i* for the past (*ʻAʻole
    ʻo ia i hele.*); questions with *hea*, *wai*, *aha*, *ʻehia*, *pehea* (*Aia ʻo Keola i hea?*, *ʻO
    wai kona inoa?*, *He aha kēia?*);
  - ranks 301–600: the commands *E* + verb (*E noho!*) and *Mai* + verb (*Mai hele!*); *ʻana* after a
    verb as a noun (*ka heluhelu ʻana* "the reading", *ma hope o ka ʻai ʻana*); the passive with *ʻia*
    (*Ua kākau ʻia ka puke e Malia.*); the possessives of every person (*koʻu*/*kaʻu*, *kou*/*kāu*,
    *ko mākou*/*kā mākou*...) and the dual and plural pronouns (*lāua*, *lākou*, *kāua*, *kākou*,
    *māua*, *mākou*, *ʻolua*, *ʻoukou*); *loaʻa* "to have, get" with *iā* (*Loaʻa iā ia he kaʻa.*);
    *no ka mea* "because"; *inā* "if" with the plain verb;
  - ranks 601–1000: the relative with *ai* (*ka hale āna i noho ai*, *ka lā i hele ai ʻo ia*); *i ka
    wā* "when" and *ma mua o*, *ma hope o* + *ʻana* (*ma hope o kona hoʻi ʻana mai*); comparatives
    (*ʻoi aku ka nui o ... ma mua o ...*); reported speech (*Ua ʻōlelo ʻo ia ua luhi ʻo ia.*); *e like
    me*; *i mea e* for purpose;
  - ranks 1001–1600 (B1): *ke* + verb as "when, if" for the future (*Ke hiki mai ʻo ia...*); *ʻoiai*
    "while, although", *no laila* "so", *akā naʻe*; the causative *hoʻo-* in new verbs; longer joined
    sentences; a story may be told by a first-person narrator or as a letter or a diary entry.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Nā nīnau:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  Nā nīnau:
  Aia ʻo Kainoa i hea?
  Aia ʻo Kainoa ma ka hale.
  ʻO wai ka inoa o kona kumu?
  ʻO Malia ka inoa o kona kumu.
  ```

  The header `Nā nīnau:` is layout: the checker does not count it as words.
- **Modern standard spelling**: write Hawaiian as it is written today (the *Māmaka Kaiao* and the
  Pukui–Elbert dictionary), with every ʻokina and kahakō: *ʻaʻole*, *ʻōlelo*, *kēia*, *lākou*, *nā*,
  *wahi*, *ʻāina*. The ʻokina is the letter U+02BB **ʻ** (MODIFIER LETTER TURNED COMMA), never an
  apostrophe ' , a quotation mark ‘ ’ or a backtick: the list, the checker and the reader only know
  U+02BB, and a word typed with another mark counts as a word outside the list. The kahakō vowels are
  the precomposed letters ā ē ī ō ū. The capital ʻokina is the same letter (*ʻO Keola*, *ʻAʻole*).
  Spellings without the marks (*lakou*, *kela*, *ike*, *oia*) are older or careless writing: the list
  files them under the marked word, but never write them. No Niʻihau *t* forms (*te*, *teia*,
  *tupuna*) and no pidgin.
- **ka and ke**: *ke* before a word starting with k, e, a or o (*ke kula*, *ke ala*, *ke ola*) and
  before some words starting with p or ʻ (*ke poʻo*, *ke pā*, *ke ʻano*); *ka* before all
  others (*ka hale*, *ka ʻāina*, *ka puke*, *ka lā*, *ka ʻohana*, *ka wahine*, *ka mea*). The plural
  *nā* has no such rule. After the preposition *i* or *ma*, the article stays (*i ke kula*, *ma ka
  hale*).
- **The possessives, a and o**: *kāna*, *kā*, *kaʻu*, *kāu* (a-class) for what one makes, controls or
  gets by one's own act: one's children, spouse, food, work, the things one writes, buys or uses
  (*kāna keiki*, *kāna puke*, *kāna ʻai*, *kāna hana*); *kona*, *ko*, *koʻu*, *kou* (o-class) for what
  one is born to or in, or does not control: parents, grandparents, brothers and sisters, the body,
  feelings, one's name, house, land, clothes, car, canoe, school and teacher (*kona makuahine*, *kona
  inoa*, *kona hale*, *kona kaʻa*, *kona kula*, *kona kumu*). Both classes have the same shape after
  *o*/*a* and *no*/*na* (*o kona hale*, *na kāna keiki*). When in doubt, choose a story where the
  class is plain.
- **Word order and the markers**: the verb comes first, then its subject, then its object (*Ua kūʻai
  ʻo Malia i kekahi puke.*). A name or pronoun as subject takes *ʻo* except *au*/*wau* and in some
  set phrases (*Ua hele au*, *Ua hele ʻo ia*). Write *wau* or *au* for "I" as the list ranks them
  (*wau* is the commoner here). The object marker *i* becomes *iā* before a name or pronoun (*ʻike ʻo
  ia iā Kainoa*). After *ʻaʻole* the pronoun moves before the verb (*ʻAʻole au i hele*, *ʻAʻole ʻo
  Malia e hele ana*).
- **Numbers**: write them as digits (*3 keiki*, *i ka hola 7*, *i ka makahiki 1990*), except a number
  word that is in your band: spell that one out to teach it (*ʻelua keiki*). Days of the week and
  months are everyday words and are taught (*Pōʻakahi*, *Ianuali*), with their capital as Hawaiian
  writes them.
- **Names are names**: place names, the names of peoples and languages (*Hawaiʻi*, *Pelekānia*,
  *Kepanī*, *ʻōlelo Pelekānia*) count as names: skip them as band words even when the list has them;
  a text may still use one, counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's
  or any other), and not the courses of the other languages either.

## The file

Write `tools/courses/haw/<id>.json`:

```json
{
  "id": "haw-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Hawaiian, e.g. Ka ʻai a me ke kūʻai ʻana",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Hawaiian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 1600 `B1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/haw/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up
  to rank 300, 90% for the first course). Names you list in `names` and numbers count as known. The
  checker lists every word outside the range with its rank: replace them, or keep a few when the text
  needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form the list gives counts: *kānaka* for *kanaka*, *wāhine* for *wahine*).
  Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up
  to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 1600.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not worth
  a lesson (a name, a word that is only a piece of a set phrase, a rare spelling of a taught word, a
  specialised term that needs an encyclopedia); the checker's last line lists the band words not
  taught yet.

Things to know about the Hawaiian list and the checker:

- The courses are pinned to `tools/courses/haw/wordlist.tsv` (a copy of the app's `haw.tsv`); the
  `bands/*.txt` files come from it. The list is counted from the Hawaiian Wikipedia, the only large
  body of Hawaiian text, so it is an encyclopedia's list: history, government, science and media words
  (*aupuni*, *mōʻī*, *kapikala*, *pelekikena*, *ʻepekema*, *polokalamu*) rank high, and kitchen and
  family words rank later than you would think (*keiki* 112, *ʻohana* 132, *makuakāne* 189, *makuahine*
  204, *ʻai* 292 (*ʻaina* "meal" is a word of its own, 470), *wai* 259, *au* 270, *ʻoe* 411, *ʻīlio* 419, *pōpoki* 667, *inu* 930). A few everyday
  words were given a rank by hand because an encyclopedia hardly has them (*wau*, *ʻolua*, *ʻehia*,
  *nehinei*, *ʻānō*, *hoaaloha*, the days and months).
- **Little words are entries of their own**, and many are homographs: *ka* 1 holds *ke* and *nā*; *i*
  2 is the preposition and object marker (and *i* of the past after *ʻaʻole*); *ʻo* 3 the subject
  marker, *o* 4 "of"; *a* 6 "and, until" and "of" (a-class); *ua* 7 the past marker (and "rain"); *e*
  10 the marker of *e* ... *ana*, of commands and of the agent; *ia* 12 "he, she, it" and "this";
  *iā* 34 the object marker before names; *ʻia* 9 the passive; *ʻana* 14 the noun-making particle; *ana*
  26 the *ana* of *e* ... *ana* (also "cave, to measure"); *ai* 41 is the particle of relative
  clauses (*kahi i noho ai*); the dictionary pack glosses only a vulgar homograph, which is not the
  sense the list counts: teach *ai* as the particle. *mau* 20 is the plural marker (and "always");
  *kona* 21 "his, her" (and the Kona coast); *kahi* 25 "place" and "one"; *pū* 33 "together"; *lā* 39
  "day, sun" (with *la*, the old spelling of the particle); *hiki* 46 "can" and "to arrive"; *hou* 47
  "new, again"; *wā* 53 "time"; *loko* 63 "inside" (with *iloko*, *maloko*); *luna* 68 "top, above";
  *aʻe* 73 "up, next"; *aʻo* 77 "to learn, teach"; *aku* 82 "away"; *ʻē* 84 "different, already"; *hoʻi*
  86 "indeed, also" and "to return"; *nei* 92 "this, here"; *na* 96 "by, for"; *wai* 259 "water" and
  "who". Use each in its own sense, and count a new word's uses in its own sense only.
- **Words the dictionary pack lacks**: the pack (`dictionaries/haw-en.sqlite`) has 2,900 Hawaiian
  words; some of the commonest (*i*, *nei*, *laila*, *wahi*, *wau*, *lilo*, *moʻolelo*, *hoʻokumu*,
  *hoʻokahi*, *inā*, *waho*, *maʻamau*) are not in it. Teach such a word in its main sense as the
  Pukui–Elbert dictionary gives it; do not skip it for lacking an entry.
- **Unmarked and old spellings** are forms of the marked word in the list (*lakou* of *lākou*, *kela*
  of *kēlā*, *ole* of *ʻole*, *aina* of *ʻāina*, *mamua* of *mua*, *te* of *ka*): never write them.
- **Names** are not in the list: people, places and brands go in `names`. Keep only true names in
  `names`, never common words, and never a name that is also a list word: as characters avoid *Mele*,
  *Kai*, *Pua*, *Lani*, *Nalu*, *Mahina*, *Makana*, *Hōkū*, *Koa*, *Nani*, *Ikaika*, *Maile*, *Nohea*,
  *Luana*, *Kaimana*; as places *Kona* (his), *Hilo* (809, "to twist") and *Hāna* (1310) are list words
  too: leave them out of `names` and use another town until their rank is in range. Safe in the pilot:
  *Keola*, *Leilani*, *Kainoa*, *Noelani*, *Kekoa*, *Kaleo*, *Malia*, *Keoni*, *Kawika*, *Kapono*,
  *Haunani*, *Iolani*, *Puanani*, *Keahi*, *Kalani*, *Kanoe*, *Kealoha*, *Lokelani*, *Kahiau*,
  *Kiele*; *Honolulu*, *Kailua*, *Waimea*, *Lāhainā*, *Līhuʻe*, *Kāneʻohe*, *Wailuku*, *Maui*, *Oʻahu*,
  *Kauaʻi*. Check a name's rank with `rank.py` before you use it.
