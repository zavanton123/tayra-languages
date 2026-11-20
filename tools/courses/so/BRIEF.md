# Writing a Somali frequency course

Each course teaches one band of 100 words from the Somali word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `so-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 1,098 words (the English Wiktionary knows
about 1,200 Somali words), so the last course is `so-mini-1100` (98 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Faadumo waxay sugaysaa Cali. Way sugtaa. Kadib way sugtaa, mar kale way sugtaa.*
  Everyday life where Somali is spoken: Muqdisho, Hargeysa, Boosaaso, Garoowe, Kismaayo, Baydhabo,
  Beledweyne, Gaalkacyo, Berbera, Burco, Ceerigaabo, Laascaanood, Jowhar, Marka; Jabuuti; Jigjiga and
  Dire Dawa; Garissa, Wajir, Mandera and Nairobi's Eastleigh; now and then London, Minneapolis,
  Toronto, Oslo or Stockholm, where many Somalis live: home, family, food, the market, work, school,
  the town, the countryside and its camels and goats, health, free time, travel. Any everyday,
  cultural or story topic is fine (Ramadan and the Ciid, weddings, poetry and the *gabay*, history,
  folk tales such as Cigaal Shidaad, sport, love, a detective story); only explicit sexual content,
  slurs used as insults and gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, jokes that depend on
  wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly; say who *isaga* or *iyada* is whenever it could be unclear. Give
  each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence. Somali puts the verb
  last (*Cali shaah buu cabbayaa*, *Faadumo waxay iibsatay bariis*). Little dialogue; when there is
  some, short and simple.
- **Every main clause has its focus**, and this is the grammar to get right in every sentence:
  - *waa* before the verb when the news is the verb or a predicate (*Cali waa nin*, *Way sugtaa*,
    *Gurigu waa weyn yahay*), joined with the subject pronoun: *waan* "I", *waad* "you", *wuu* "he",
    *way* "she, they", *waannu/waynu* "we";
  - *baa* or *ayaa* after the noun phrase that is the news (*Cali ayaa yimid*, *Shaah baan cabbay*),
    joined with the pronoun: *ayuu*, *ayay*, *baan*, *buu*, *bay*; when the subject itself is focused
    the verb takes the short form (*Cali ayaa yimid*, *Faadumo ayaa timid*, *Dad badan ayaa jooga*)
    and the subject no *-u*;
  - *waxa(a)* in front of the clause with the news after the verb (*Wuxuu joogaa magaalo weyn*,
    *Waxaa jooga dad badan*): *waxaan*, *waxaad*, *wuxuu*, *waxay*, *waxaynu*;
  - a negative clause has none of them: *ma* + the negative form (*Cali ma yimaado*, *ma uusan
    iman*, *ma tagin*); questions take *ma* (*Ma timid?*) or a question word with *baa*
    (*Maxaad iibsatay?*, *Yaa yimid?*).
- **Nouns and their determiners**: the article and the other determiners are endings: *nin*,
  *ninka*, *ninkii* (the one mentioned), *ninkan*, *ninkaas*, *ninkayga*, *ninkiisa*; *magaalo*,
  *magaalada*; *bil*, *bisha*; *guri*, *guriga*. Masculine nouns take *k-* (*-ka*, *-ga*, *-a*,
  *-ha*), feminine ones *t-* (*-ta*, *-da*, *-sha*, *-dha*): get the gender right. A definite subject
  ends in *-u* (*Ninku wuu yimid*, *Magaaladu waa weyn tahay*), not when it is focused with
  *baa/ayaa*. Many plurals change the gender and the word (*nin*, *niman*; *wiil*, *wiilal*; *buug*,
  *buugag*; *magaalo*, *magaalooyin*; *gabadh*, *gabdho*).
- **The verb**: its persons and tenses are endings on its stem (*qor* "write": *qoraa*, *qortaa*,
  *qornaa*, *qoraan*; past *qoray*, *qortay*, *qornay*, *qoreen*; progressive *qorayaa*,
  *qoraysaa*; *qori doonaa* "will write"; *qori jiray* "used to write"; *qori karaa* "can write";
  negative *ma qoro*, *ma qorin*). The person ending agrees with the subject: *-t-* for "you" and
  "she", *-n-* for "we". The adjective is a predicate with *ahay*: *Gurigu waa weyn yahay*,
  *Magaaladu waa weyn tahay*.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *waa*, *baa/ayaa*, *waxa* with their pronouns; *X waa Y*; the present general
    (*wuu socdaa*) and progressive (*wuu cunayaa*) and the past (*wuu yimid*, *way tagtay*); *ma*
    in questions and negatives; the determiners *-ka/-ta*, *-kii/-tii*, *-kan/-tan*, *-kaas/-taas*
    and the possessives *-kayga/-tayda*, *-kaaga/-taada*, *-kiisa/-tiisa*, *-keeda/-teeda*;
    *leh* "having" (*guri weyn leh*); the subject *-u*; *iyo* and *oo* "and"; numbers one to ten;
    the question words *maxaa*, *yaa*, *xaggee*, *goorma*, *immisa*, *sidee*;
  - ranks 301–600: the imperative (*kaalay*, *fadhiiso*, *i sug*, *keena*), the future with
    *doonaa*, *jiray* "used to", *karaa* "can"; the little words before the verb (*u*, *ku*,
    *ka*, *la*) and their clusters (*ii*, *kuu*, *noo*, *loo*, *lagu*, *uga*, *ugu*, *kaga*);
    the impersonal *la* (*waa la arkay* "he was seen"); *in* + subjunctive (*wuxuu rabaa inuu
    tago*); relative clauses (*ninka yimid*, *buugga aan akhriyay*); *haddii*, *markii*, *marka*;
  - ranks 601–1100: the conditional and the hypothetical with *lahaa* (*waan iman lahaa*),
    comparison with *ka* (*ka weyn*) and *ugu* (*ugu weyn*), reported speech with *in*, longer
    joined sentences (*laakiin*, *waayo*, *inkastoo*, *maadaama*, *kadib*), and from rank 1000
    a story told in the first person, a letter or a short informative text.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would, the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Su'aalo:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  Su'aalo:
  Faadumo yay sugaysaa?
  Faadumo waxay sugaysaa Cali.
  Cali ma yimid?
  Cali wuu yimid.
  ```

  The header `Su'aalo:` is layout: the checker does not count it as a word.
- **Standard Somali**: the official Latin spelling: long vowels written double (*aa*, *ee*, *ii*,
  *oo*, *uu*), *c* for the pharyngeal (*Cali*, *caano*), *x* for the other (*Xasan*, *xalay*),
  *dh*, *kh*, *sh*, *q*, and the apostrophe for the glottal stop (*su'aal*, *la'aan*, *lo'*,
  *da'*). Somali writes *dh* in the north and *r* in the south for some words (*yidhi*/*yiri*,
  *gabadh*/*gabar*, *odhan*/*oran*, *jidh*/*jir*): use *dh* throughout a course; the list has both
  spellings as one word. No slang, no English where Somali has its own word.
- **Somali typography**: direct speech in double quotation marks "…", each speaker's line a
  paragraph of its own; never single quotes, since the reader reads an apostrophe as part of a
  word. Write numbers as digits (*3 sano*, *saacadda 8*) except a number word that is in your
  band: spell that one out to teach it. Avoid hyphenated words; *2024-ka* is the year and *ka*.
- **Names are names**: Somali names (*Cali*, *Axmed*, *Maxamed*, *Cumar*, *Xasan*, *Yuusuf*,
  *Warsame*, *Ismaaciil*, *Bashiir*, *Cabdi*; *Faadumo*, *Xaliimo*, *Sahra*, *Hodan*, *Maryan*,
  *Nasra*, *Khadiija*, *Caasha*, *Sucaad*, *Hibo*) and places go in `names`. Never use a list word as
  a character's name: *Ayaan* ("luck"), *Ubax* ("flower"), *Nabad* ("peace"), *Barwaaqo*
  ("plenty"), *Hooyo* are list words, and *Muuse* reads as *muu* + *se* (a form of *ma*). Words for peoples and languages (*Soomaali*, *Carab*,
  *Ingiriis*) count as words at their rank: never teach them as band words, a text may still use one.
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Somali list

- Verbs are listed by their imperative stem (*qor*, *keen*, *sheeg*, *samee*, *iibso*,
  *fadhiiso*), as dictionaries give them; four irregular verbs by their infinitive (*iman* "come":
  *yimid*, *timid*, *kaalay*; *odhan* "say": *yidhi*, *tidhi*, *dheh*; *aqoon* "know": *yaqaan*,
  *aqaan*; *ool* "be in a place": *yaal*, *taal*), and *ah* "be" (*yahay*, *tahay*, *ahaa*,
  *yihiin*). *leh* holds *lahaa*, *leeyahay*; *jecel* holds *jeclahay*; *og* holds *ogahay*.
- The focus and verbal particles run together with the pronoun after them, and the list files the
  cluster under its first word: *waan*, *wuu*, *way* are *waa*; *waxaan*, *wuxuu*, *waxay* are
  *waxa*; *ayuu*, *ayay* are *ayaa*; *baan*, *buu*, *bay* are *baa*; *muxuu*, *maxay* are *maxaa*;
  *inuu*, *inay* are *in*; *hadduu* is *haddii*; *markuu* is *mar*; *loo*, *lagu*, *laga* are
  *la*; *ii*, *igu* are *i*; *kuu*, *kula*, *kaga* are *ku*; *noo* is *na*; *uga*, *ula* are *u*;
  *isku*, *iska* are *is*. Teach the plain word; its clusters count as it.
- Nouns hold their determiners and plurals (*magaalo*: *magaalada*, *magaalooyinka*), verbs their
  persons and tenses, adjectives their predicate forms (*weynaa*) and doubled plurals (*waaweyn*).
  The checker reads such a form that the list lacks by the rules the list was built with
  (*gurigayaga* counts as *guri*, *qoraysaa* as *qor*); only words with no such reading are
  outside words.
- Spellings of one word are one entry: *dowlad* (*dawlad*), *waqti* (*wakhti*), *laba* (*labo*),
  *sannad* (*sanad*), *kaliya* (*keliya*), *hadda* (*hada*), *gabadh* (*gabar*), *xir* (*xidh*).
- About 220 of the words, most of them little words and everyday nouns (*ayaa*, *ma*, *in*, *la*,
  *kale*, *badan*, *dal*, *lacag*, *sannad*), have no entry in the dictionary pack
  (`dictionaries/so-en.sqlite`); the writer notes list their meanings.
- The list comes from news, web forums and web text, so politics and the news rank high (*dowlad*,
  *gobol*, *degmo*, *shir*, *dagaal*, *ciidan*): teach them in an ordinary
  sense where you can (*dagaal* "a fight", *shir* "a meeting"), and skip what a story cannot use.
