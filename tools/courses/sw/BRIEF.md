# Writing a Swahili frequency course

Each course teaches one band of 100 words from the Swahili word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `sw-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 6,386 words, so the last course is
`sw-mini-6400` (86 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Hamisi anatafuta simu yake. Anaenda jikoni. Haioni. Anaenda sebuleni...* Everyday
  life where Swahili is spoken: Dar es Salaam, Zanzibar (Unguja, Pemba, Mji Mkongwe), Arusha,
  Moshi, Dodoma, Mwanza, Tanga, Morogoro, Mbeya, Iringa, Bagamoyo, Kilwa; Mombasa, Nairobi, Lamu,
  Malindi, Kisumu, Nakuru; Kampala, Goma, Bujumbura; Kilimanjaro, Ziwa Victoria, Ziwa Tanganyika,
  the coast, the islands and the national parks (Serengeti, Ngorongoro, Mikumi); now and then
  London, Muscat or Dubai, where Swahili speakers live: home, family, food, the market, work,
  school, the town, health, free time, travel. Any everyday, cultural or story topic is fine
  (festivals and holidays, history, legends, sport, love, a detective story); only explicit sexual
  content, slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *yeye* is whenever it could be unclear. Give each story its
  own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Swahili
  order (subject, verb, object: *Halima anapika chakula.*; the noun before its adjective,
  demonstrative and possessive: *nyumba yake kubwa*, *mtoto huyu*). Swahili verbs carry their
  subject: write *yeye*, *wao*, *mimi* only for contrast or emphasis. Little dialogue; when there is
  some, short and simple.
- **Agreement is the grammar**: every noun belongs to a class, and its adjectives, possessives,
  demonstratives, numbers and verbs agree with it (*kitabu kizuri kimoja kiko mezani*, *vitabu vizuri
  viwili viko mezani*; *mtoto wangu*, *watoto wangu*; *nyumba yangu*, *nyumba zangu*). Get every
  agreement right; prefer the m-wa (people), ki-vi, n and ji-ma classes early.
- **The verb**: subject marker + tense + (object) + stem: *a-na-soma*, *ni-li-mw-ona*,
  *tu-ta-kwenda*, *ha-ku-ja*. The list files every such form under its verb (*penda* holds
  *anapenda*, *nakupenda*, *ningependa*, *sipendi*), the infinitive *ku-* too (*kusoma*), and the
  one-syllable verbs keep *ku* (*anakula*, *alikuja*, *atakunywa*). The copula *ni* / *si* and *-wa*
  (*kuwa*, *alikuwa*, *atakuwa*) are separate entries; "have" is *wa na* (*nina*, *ana*, *kuna*,
  *hakuna*, *walikuwa na*); "be at a place" is *wapo/wako/wamo* (*yupo*, *yuko*, *niko*, *kuna*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *ni* and *si* (*Salim ni mwalimu*, *Huyu si Asha*), *-na* "have" (*nina*, *ana*,
    *kuna*, *hakuna*), *-ko/-po* (*yuko nyumbani*, *yupo hapa*), the present *-na-* (*anasoma*), the
    near past *-me-* (*amefika*), the past *-li-* (*alikwenda*, *alikula*), the future *-ta-*
    (*atakuja*); the object marker for people (*anampenda*, *ninakuona*); the infinitive (*anataka
    kula*, *anapenda kusoma*); the negative of the present and past (*hasomi*, *sipendi*, *hakuja*,
    *hatukwenda*); the locative *-ni* (*nyumbani*, *sokoni*, *shuleni*); possessives *-angu*, *-ako*,
    *-ake*, *-etu*, *-enu*, *-ao* and *-a* "of" with agreement (*mtoto wa Salim*, *kitabu cha Asha*);
    demonstratives *huyu*, *hii*, *hiki*, *yule*, *ile*, *kile*; numbers one to ten; questions with
    *nini*, *nani*, *wapi*, *lini*, *vipi*, *kwa nini*, *ngapi*, *gani*, *je*;
  - ranks 301–600: the imperative and the subjunctive (*njoo*, *kaa*, *nisaidie*, *tuende*, *anataka
    tuende*, *lazima ufike*), *-ki-* "while, if" (*akifika*, *ukitaka*), the narrative *-ka-*
    (*alifika, akaingia, akakaa*), the habitual *hu-* (*husoma kila siku*), *-me-* states (*amechoka*,
    *amelala*), the negative future and perfect (*hatakuja*, *hajafika*), the other noun classes
    (*u-*, *pa/ku/mu* places), comparisons (*kuliko*, *zaidi ya*, *kama*);
  - ranks 601–1000: relatives (*amba-*: *mtu ambaye*, *kitabu ambacho*; and the short ones joined to
    the verb: *anayesoma*, *kitabu nilichonunua*, *alipofika*, *tulipokuwa*), the conditionals *-nge-*
    and *-ngali-* (*ningekuwa na pesa, ningenunua gari*), the passive (*kilinunuliwa*, *anaitwa*), the
    applied and causative verbs (*nunulia*, *pikia*, *rudisha*);
  - ranks 1001–2000: the stative and reciprocal verbs (*vunjika*, *patikana*, *pendana*,
    *saidiana*), reported speech with *kwamba* / *kuwa*, longer joined sentences (*ingawa*, *ili*,
    *kwa sababu*, *mpaka*, *kabla ya*, *baada ya*), *-po-*, *-ko-*, *-mo-* time and place relatives;
  - ranks 2001–4000: a richer register: connectors (*hata hivyo*, *kwa hiyo*, *aidha*, *licha ya*,
    *kwa upande mwingine*), the newspaper column, dialogue, common proverbs (*methali*) explained
    by their context;
  - ranks 4001–6400 (C1): everything before, with more abstract and nuanced content: a short essay
    that weighs two sides, an interview, a portrait, a review; the specialised words of professions,
    sciences and the arts, each made clear by its context.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as a letter or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Anaenda jikoni. Haioni simu. Anaenda sebuleni. Haioni simu.*).
- **Questions at the end**: after the story, a paragraph starting `Maswali:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kwa nini* and *vipi*, with fuller answers):

  ```
  Maswali:
  Hamisi anatafuta nini?
  Hamisi anatafuta simu yake.
  Simu iko wapi?
  Simu iko jikoni.
  ```

  The header `Maswali:` is layout: the checker does not count it as a word.
- **Standard Swahili**: the narration is *Kiswahili sanifu* as written in Tanzania and Kenya today,
  neutral and correct: *kuwa* (not *kua* for "be"), *yaani* (not *yani*), *mimi*, *wewe* (not *mm*,
  *ww*), *ndiyo* "yes", *sasa hivi*, *asante*. No Sheng and no slang; no English where Swahili has its
  own word (*sawa*, not *okay*; *simu*, not *phone*). No swearing (skip such list words).
- **Swahili typography**: direct speech in double quotation marks "…", each speaker's line a
  paragraph of its own; the narrator's comma goes inside the closing mark. The apostrophe of
  *ng'ombe*, *ng'ambo* is part of the word. Write numbers as digits (*watoto 3*, *saa 2 asubuhi*)
  except a number word that is in your band: spell that one out to teach it (*watoto wawili*). Avoid
  hyphenated words.
- **Names are names**: Swahili names (*Hamisi*, *Halima*, *Salim*, *Asha*, *Fatuma*, *Mariamu*,
  *Daudi*, *Yusufu*, *Said*, *Hassani*, *Saida*, *Kassim*, *Mwanaisha*; in Kenya also *Wanjiru*,
  *Otieno*, *Akinyi*) and places go in `names`. Words for peoples and languages (*Mtanzania*,
  *Wakenya*, *Kiswahili*, *Kiingereza*) count as words at their rank: never teach them as band words,
  a text may still use one. Never use a list word as a character's name: *Juma* ("week"), *Amina*
  ("amen"), *Baraka*, *Neema*, *Zawadi*, *Rehema*, *Furaha*, *Imani*, *Amani*, *Bahati*, *Upendo*,
  *Tumaini*, *Faraja*, *Subira*, *Mosi* are list words; *Ali* reads as a verb form (*a-li*).
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Swahili list

- Headwords are written as the English Wiktionary writes them, without its hyphens: verbs by their
  stem (*penda*, *enda*, *la* "eat", *ja* "come", *pa* "give", *fa* "die", *nywa* "drink"), and the
  agreeing stems by their stem (*zuri*, *kubwa*, *ake*, *angu*, *ote*, *ingi*, *ingine*, *enye*, *le*
  "that", *pi* "which"). Teach the stem through its forms: *vizuri*, *mzuri*, *nzuri* all teach *zuri*.
- Some headwords join two words that are written alike: *wa* is "be" (*kuwa*, *alikuwa*) and "of"
  (*mtoto wa Salim*); *la* is "eat" (*kula*) and "of" (*jina la*); *cha* is "of" (*kitabu cha*);
  *katika* is "in"; *kama* is "like, if". Teach a word in its common sense.
- *wa na* "have" is one entry with a space; its forms are the joined words *nina*, *una*, *ana*,
  *tuna*, *mna*, *wana*, *kuna*, *hakuna*, *sina*. Plain *na* "and, with" is another entry.
- Nouns hold their plural and locative: *mtoto* holds *watoto*, *kitu* *vitu*, *nyumba* *nyumbani*.
- Derived verbs are often entries of their own (*pikia*, *fundisha*, *jifunza*, *onekana*,
  *patikana*, *andikwa*, *pewa*): teach each in its own sense.
- The checker reads a verb, adjective or locative form the list lacks by the rules the list was
  built with: *tutakapokutana* counts as *kutana*, *vikubwa* as *kubwa*. Only words with no such
  reading are outside words.
- Some entries are subtitle or Wikipedia noise or colloquial: *mm*, *ww*, *lkn*, *vp* (chat
  spellings), *mfalme*, *malkia*, *kasri* (common in dubbed films, fine in a story), place and
  species words of Wikipedia (*kata*, *wilaya*, *spishi*). Skip noise; teach colloquial words only
  when the story's dialogue needs them.
