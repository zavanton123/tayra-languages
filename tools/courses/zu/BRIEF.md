# Writing a Zulu frequency course

Each course teaches one band of 100 words from the Zulu word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `zu-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 2,244 words (every word has an entry in the
zu-en dictionary pack, which is small), so the last course is `zu-mini-2300` (44 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *UKhanyisile ufuna imali yakhe. Ubheka endlini. Imali ayikho lapho. Ubheka
  ngaphandle...* Everyday life where Zulu is spoken: KwaZulu-Natal (eThekwini, uMlazi, eMgungundlovu,
  Ulundi, the Drakensberg, the Thukela and the coast), Johannesburg and Soweto, Pretoria; now and then
  Cape Town or London, where Zulu speakers live: home, family, food, the market, work, school, the
  town, health, free time, travel. Any everyday, cultural or story topic is fine (weddings and
  festivals, history and the old kings, legends and folk tales, church, sport, love, a detective
  story); only explicit sexual content, slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *yena* is whenever it could be unclear. Give each story its
  own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Zulu order
  (subject, verb, object: *UNomsa uthola imali.*; the noun before its adjective and possessive: *indlu
  yami enkulu*; a demonstrative before or after it: *lo muntu*, *umuntu lo*). The verb carries its
  subject: write *yena*, *bona*, *mina* only for contrast or emphasis. Little dialogue; when there is
  some, short and simple.
- **Agreement is the grammar**: every noun belongs to a class, and its adjectives, possessives,
  demonstratives, numbers and verbs agree with it (*ingane encane ihlala endlini*, *izingane ezincane
  zihlala ezindlini*; *umuntu wami*, *abantu bami*; *indlu yami*, *izindlu zami*). Get every agreement
  right; prefer the classes of people (*umu-/aba-*, *u-/o-*), *isi-/izi-*, *i(n)-/izi(n)-* and
  *i(li)-/ama-* early.
- **Write conjunctively, as standard Zulu does**: a prefix is written onto its word and joins its
  vowel (*na + umuntu* is *nomuntu*, *nga + isikhathi* *ngesikhathi*, *na + abantu* *nabantu*), the
  copula too (*ngumuntu*, *yindlu*, *ngabantu*, *ngiyingane*), the locative (*endlini*, *emsebenzini*,
  *ekhaya*) and the possessive (*indlu kaThemba*, *igama lengane*). A name keeps its capital behind its
  prefix: *uThemba*, *kuThemba*, *noThemba*, *kaThemba*, *nguThemba*, *eThekwini*, *eKwaZulu-Natal*.
- **The verb**: subject concord + tense + (object concord) + stem + ending: *ngi-ya-hamba*,
  *wa-hamba*, *u-zo-hamba*, *ngi-hamb-ile*, *a-ka-hamb-i*, *ngi-ya-m-bona*. The list files every such
  form under its verb (*hamba* holds *ukuhamba*, *wahamba*, *ngiyahamba*, *uzohamba*, *akahambi*), the
  infinitive *uku-* too, and the checker reads the object concords and the extensions (*bonwa*,
  *hambela*, *hambisa*, *thandana*) as their verb.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the copula (*nguThemba*, *yindlu*, *ngiyingane*, *akayona ingane*), the present
    (*ngiyahamba*, *ngihamba kahle*), the recent past *-ile/-e* (*ufikile*, *ngimbonile*), the remote
    past *wa-* (*wahamba*), the future *-zo-* (*uzohamba*); "have" with *-na* (*nginemali*,
    *unengane*, *akanayo*), "there is" (*kukhona*, *akukho*); the negative of the present (*angihambi*,
    *akazi*); the locative *e-…-ini* and *ku-* (*endlini*, *kuye*, *kuThemba*); possessives (*-ami*,
    *-akho*, *-akhe*, *-ethu*, *-enu*, *-abo*) with their concord (*indlu yami*, *igama lakhe*);
    demonstratives (*lo*, *le*, *lesi*, *leli*, *lezi*, *labo*); adjectives with their concord
    (*omkhulu*, *enkulu*, *encane*); the absolute pronouns (*mina*, *wena*, *yena*, *thina*, *nina*,
    *bona*); numbers one to five; questions with *na* (yes or no), *-ni* (*wenzani*), *-phi* (*uyaphi*),
    *ubani*, *nini*, *kanjani*, *kungani*;
  - ranks 301–600: the object concords (*ngiyambona*, *ngiyakuthanda*, *uyabazi*), the imperative and
    the subjunctive (*hamba!*, *hambani!*, *ake ngibone*, *ufuna ukuthi ahambe*, *kumele asebenze*), the
    negative past (*angihambanga*), the potential *-nga-* (*ngingahamba*), relatives (*umuntu ohambayo*,
    *ingane engiyibonile*), adverbs with *ka-* (*kahle*, *kancane*, *kakhulu*), comparison with *kuna-*
    (*mkhulu kunami*);
  - ranks 601–1000: the passive (*wabonwa*, *kuthiwa*), the applied, causative and reciprocal verbs
    (*ngimphekela*, *hambisa*, *thandana*), the participial (*ehamba*, *efika*), *-sa-* "still"
    (*usafunda*), *se-* "already" (*usefikile*), *uma* with the participial (*uma efika*);
  - ranks 1001–2000: the neuter verbs (*bonakala*, *enzeka*), the compound tenses (*wayehamba*,
    *bekukhona*, *uzobe esebenza*), reported speech with *ukuthi*, longer joined sentences (*noma*,
    *kanti*, *kepha*, *nokho*, *ngakho*, *ngoba*, *ukuze* with the subjunctive, *ngaphambi kokuthi*,
    *ngemuva kokuthi*);
  - ranks 2001–2244 (B2): a richer register: connectors, the newspaper report, dialogue, common
    proverbs (*izaga*) explained by their context.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as a letter or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Ubheka endlini. Imali ayikho lapho. Ubheka ngaphandle. Nalapho ayikho.*).
- **Questions at the end**: after the story, a paragraph starting `Imibuzo:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (yes-or-no questions with *na* suit the first bands; after rank 1000 they may
  also ask *kungani* and *kanjani*, with fuller answers):

  ```
  Imibuzo:
  UKhanyisile ufunani?
  Ufuna imali yakhe.
  Imali ikhona endlini na?
  Cha, ayikho lapho.
  ```

  The header `Imibuzo:` is layout: the checker does not count it as a word.
- **Standard Zulu**: the narration is standard isiZulu as written today, neutral and correct: no
  isiCamtho or township slang, no English where Zulu has its own word (*kulungile*, not *okay*;
  *ucingo* or *ifoni*, not *phone*), no swearing (skip such list words). Tone and length are not
  written.
- **Zulu typography**: direct speech in double quotation marks "…", each speaker's line a paragraph
  of its own; the narrator's comma goes inside the closing mark. The reader cuts a word at a hyphen
  and at an apostrophe: avoid hyphenated words (*i-ofisi*, *ngo-2010*, *ezi-3*, *ngakho-ke*); write a
  number as a word once your band has it, or recast the sentence (*abantu abahlanu*, *ngonyaka
  odlule*). Write digits only standing alone (*ihora 3*).
- **Names are names**: Zulu names (*Themba*, *Thandi*, *Nomsa*, *Lindiwe*, *Sibusiso*, *Nandi*,
  *Thabo*, *Lungelo*, *Mpho*, *Khanyisile*, *Nomvula*, *Thembeka*, *Bheki*, *Phumzile*, *Sifiso*,
  *Mxolisi*, *Thuli*, *Njabulo*, *Hlengiwe*, *Nompumelelo*) and places go in `names`, written bare
  with their capital (*Themba*, *Thekwini*, *Mgungundlovu*, *KwaZulu-Natal*); the text writes them
  with their prefixes (*uThemba*, *kaThemba*, *eThekwini*), and the checker counts each such word as
  the name. Days, months and languages are words, written with their capital (*uMsombuluko*,
  *ngoLwesibili*, *uMashi*, *isiZulu*, *isiNgisi*): teach those in your band, never put them in
  `names`. Never use a list word as a character's name: *Mandla* (*amandla*), *Sipho* (*isipho*),
  *Sizwe* (*isizwe*), *Lwazi* (*ulwazi*), *Nkosi* (*inkosi*), *Zodwa*, *Sihle*, *Zama*, *Musa*,
  *Mbali*, *Zanele*, *Dumisani*, *Xolani*, *Lindani* are list words or read as one.
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Zulu list

- Headwords are written as the English Wiktionary writes them, without its hyphens: verbs by their
  stem (*hamba*, *bona*, *thi* and *sho* "say", *ba* "be", *za* "come", *ya* "go", *azi* "know",
  *enza* "do"), the agreeing stems by their stem (*khulu*, *ncane*, *hle*, *dala*, *bili*, *ningi*,
  *nye*, *nke* "all", *dwa* "alone, only"). Teach the stem through its forms: *enkulu*, *omkhulu*,
  *amakhulu* all teach *khulu*.
- Nouns hold their plural, locative, possessive and copula forms: *umuntu* holds *abantu*,
  *nomuntu*, *ngumuntu*; *indlu* holds *izindlu*, *endlini*; *isikhathi* holds *ngesikhathi*.
- The absolute pronouns hold the possessives of their person or class: *mina* holds *wami*, *yami*,
  *kimi*, *nami*; *wena* *wakho*, *yakho*, *kuwe*, *nawe*; *yena* *wakhe*, *yakhe*, *kuye*, *naye*;
  *thina* *wethu*; *nina* *wenu*; *bona* *wabo*, *nabo*; *yona*, *wona*, *zona*, *lona*, *khona* those of
  their classes. *khona* also holds *kukhona* "there is" and *akukho* "there is not".
- *ukuthi* "that" is a word of its own, with *ngokuthi*, *nokuthi*, *yokuthi*; *thi* "say" holds
  *uthi*, *wathi*, *bathi*, *uthe*, *kuthiwa*, *ethile*. *ukuqala* "beginning, first" (*yokuqala*,
  *ekuqaleni*) is apart from *qala* "begin".
- Some headwords are added by hand where the pack lacks an everyday word (*kodwa*, *kepha*, *nokho*,
  *hhayi*, *ngakho*, *kanjalo*, *kanje*, *kumele*, *ngisho*, *angeke*, *nakuba*, *ngempela*, *namuhla*,
  *ngaphambi*, *lutho*, *ngani*, *nini*, *sengathi*, *njengoba*, *eduze*, *ndawonye*, *ikakhulukazi*,
  *kuphela*, *ngabe*, *bese*, *yize*, *zange*, *ngapha*): teach them as any word.
- The checker reads a verb, noun or pronoun form the list lacks by the rules the list was built
  with: *ngiyambona* counts as *bona*, *nomngane* as *umngane*, *unemali* as *imali*, *nginayo* as
  *yona*. Only words with no such reading are outside words.
- The dictionary pack is small (about 3,000 entries), so common words it lacks are not in the list
  (*inani* "price", *isicelo* "application", *imininingwane* "details"), and their forms may have gone
  to a verb that looks alike: a few headwords hold forms of another word (*anga*, *amba*, *linga*,
  *baza*, *onga*, *betha*, *otha*, *banga*, *landa*, *thela*). Skip such a headword when its forms do
  not fit; teach a word only in the sense its dictionary entry gives.
