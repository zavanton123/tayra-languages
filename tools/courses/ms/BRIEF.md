# Writing a Malay frequency course

Each course teaches one band of 100 words from the Malay word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ms-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 9,341 words (the ms-en dictionary pack
has about 9,800 headwords; words past it come from the Indonesian packs), so the courses go up to
`ms-mini-9400`, whose band has 41 words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Nora mahu kucing. Dia pergi ke rumah haiwan. Di sana ada tiga kucing...*
  Everyday life in Malaysia: Kuala Lumpur and the Klang Valley (Petaling Jaya, Shah Alam,
  Klang, Kajang, Putrajaya), Seremban, Melaka, Ipoh, Taiping, George Town and the rest of Penang,
  Alor Setar, Kota Bharu, Kuala Terengganu, Kuantan, Johor Bahru, Kuching, Kota Kinabalu, Miri;
  the villages (*kampung*) and their rice fields (*sawah*), rubber and oil-palm estates, the
  Cameron Highlands, Langkawi, the islands of the east coast, the rainforest; night markets
  (*pasar malam*), hawker stalls and *mamak* restaurants, *nasi lemak*, *roti canai*, *teh tarik*,
  the LRT and MRT, buses, *teksi* and e-hailing; and now and then Singapore, Brunei, London or
  Tokyo, where Malay is spoken at home. Malaysia is Malay, Chinese, Indian, Iban, Kadazan and
  more: let the people in the stories be all of them. Home, family, food, shopping, work, school,
  the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; *dia* is "he" and "she", so say who *dia*
  is whenever it could be unclear. Give each story its own names: the learner reads the course in
  one go, and two Noras in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; *ini*/*itu* after the noun: *rumah ini*; the adjective after the noun:
  *rumah besar*, *rumah yang besar*). Malay verbs have no tense: time is told by time words
  (*hari ini*, *semalam*, *esok*, *tadi*, *nanti*) and the aspect words *sudah*, *belum*,
  *sedang*, *masih*, *akan*, *pernah*, *telah*. Up to rank 500 the stories stay in the present,
  with *sudah*, *belum*, *pernah* and *akan* where they are natural; from rank 600 a story may be
  told as past events (*Semalam Nora pergi ke pasar. Hujan turun.*). Little dialogue; when there
  is some, short and simple.
- **Who says "you"**: Malay avoids a bare "you" with older people and strangers. It addresses them
  by a title or kinship word and uses that word again for "you": *Encik*, *Puan*, *Cik* to adults
  in shops and offices, *Pak Cik* and *Mak Cik* to older people, *Abang*/*Bang* and *Kakak*/*Kak*
  to young adults, *Cikgu* to a teacher, *Ayah*/*Ibu*/*Mak*, *Atuk*, *Nenek* in the family
  (*Puan mahu teh?*, *Ibu nak ke mana?*). *Saya* with *awak* is the neutral pair among equals and
  at work; *aku* with *kau*/*engkau* is close friends, siblings and children; *kamu* is used to
  children and to a group; *anda* is polite and impersonal (notices, adverts, a host to a guest).
  *Beliau* is "he/she" said with respect (a teacher, an elder, an official), never "you".
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*kerana*, *sehingga*, *ketika*, *selepas*,
  *sebelum*, *walaupun*, *tetapi*, *namun*, *jadi*, *supaya*), dialogue can carry more of the
  story, and a story may be told by a first-person narrator or as a message, a letter or a diary
  entry. Grammar grows with the bands, each band using what came before:
  - ranks 1–300: sentences without a verb (*Rumah ini besar. Ali suami saya.*); *ada* and *tidak
    ada*; *tidak* before verbs and adjectives, *bukan* before nouns (*Saya bukan guru.*); *yang*
    (*rumah yang besar*, *yang ini*, *Siapa yang tahu?*); possession by order (*rumah Nora*) and
    the possessive endings *-ku*, *-mu*, *-nya* (*rumahku*, *namanya*); *adalah* and *ialah*
    before a noun phrase (*Kuching ialah bandar yang besar.*); the aspect words *sudah*, *belum*,
    *masih*, *pernah*, *akan*, *sedang*; *mahu*, *ingin*, *boleh*, *dapat*, *perlu*, *harus*,
    *mesti* + verb; *di*, *ke*, *dari* with places (*di rumah*, *ke Melaka*, *dari Ipoh*) and
    *kepada*, *daripada* with people; questions with *apa*, *siapa*, *di mana*, *ke mana*,
    *dari mana*, *bila*, *kenapa*, *bagaimana*, *berapa*, and yes/no questions opened by
    *Adakah* (*Adakah rumah itu besar?*); numbers (*satu*, *dua*, *tiga*) and the commonest
    classifiers (*seorang*, *sebuah*); plural by context, numbers or *banyak*/*semua*, and now and
    then by reduplication (*kanak-kanak*, *anak-anak*). The verbs are the ones the list gives as
    words: a few *ber-* verbs (*bermain*, *berjalan*, *bekerja*) and *meN-* verbs (*membuat*,
    *melihat*, *menjadi*), taken as they are, without explaining the prefix;
  - ranks 301–600: *meN-* verbs freely (*membeli*, *mencari*, *menulis*), with the sound changes
    of the prefix (*memakai*, *menulis*, *menyapu*, *mengambil*) and the bare form in commands and
    requests (*Tolong ambil buku itu.*, *Mari makan!*, *Jangan lupa!*, *Sila duduk.*); *ber-*
    verbs (*bertolak*, *berbual*, *bercakap*); the *di-* passive in simple sentences (*Kek itu
    dibuat oleh Ibu. Pintu dibuka.*); *ter-* for "most" (*terbesar*, *termurah*) and for a state
    (*terbuka*, *tertutup*); comparatives (*lebih besar daripada*, *paling murah*, *sama besar*);
    the time (*pukul tujuh pagi*, *pukul 7.30 malam*), days of the week and months; *sedang* for
    what is going on now; more classifiers (*seekor kucing*, *sebiji telur*, *sehelai kertas*,
    *sebatang pen*); *-lah* to soften a command (*Duduklah.*);
  - ranks 601–1000: the passive with a pronoun before the verb, as Malay prefers it when the doer
    is a person (*Buku itu sudah saya baca. Surat ini kutulis semalam.*); verbs in *-kan* and *-i*
    (*membelikan*, *memberikan*, *menemani*, *mendekati*), each a word of its own when its meaning
    moves away from the base (*masuk* / *memasukkan*); nouns in *ke-an*, *pe-*, *pe-an*,
    *per-an* and *-an* (*kesihatan*, *pekerjaan*, *penjual*, *perjalanan*, *makanan*), each a word
    of its own; relative clauses with *yang*; *kerana*, *sehingga*, *supaya*/*agar*, *walaupun*,
    *kalau*/*jika*; reported speech with *bahawa* (*Dia kata bahawa dia penat.*); past events told
    with time words and *sudah*/*telah*;
  - ranks 1001–1300: *memper-* and *diper-* (*memperbaiki*, *diperbaiki*), *ke-an* of what happens
    to someone (*kehujanan*, *kesejukan*, *ketinggalan*), *-nya* that makes a noun or an adverb
    (*tingginya*, *rupanya*, *sebenarnya*, *biasanya*), *se-* for "as ... as" (*setinggi rumah*),
    conditions (*kalau..., maka...*, *sekiranya*), *sambil*, *sementara*, *selama*;
  - ranks 1301–1600: the indefinites (*sesiapa*, *apa-apa*, *di mana-mana*, *seseorang*,
    *sesuatu*, *tiada siapa yang*), *ter-* of what can or cannot be done (*tidak terlihat*,
    *terbaca*), *-an* nouns of results and places (*tulisan*, *fikiran*, *padang*), *walaupun*,
    *padahal*, *malah*, *bahkan*, *baik... mahupun*, *bukan... tetapi*;
  - ranks 1601–2000: longer passives and object-focus sentences, nominalised verbs as subjects
    (*Membaca membuat saya tenang.*), formal connectors (*oleh itu*, *dengan demikian*, *selain
    itu*, *akan tetapi*), *ke-* ordinals and fractions (*kedua*, *setengah*, *suku*);
  - ranks 2001–2500: a fuller written style, with *yang* clauses inside one another, *bagi*,
    *terhadap*, *mengenai*, *berdasarkan*, *menurut*, reported speech kept as Malay keeps it (no
    change of tense);
  - ranks 2501–3000: a richer, more formal register: connectors such as *namun begitu*,
    *sehubungan dengan*, *memandangkan*, *walau bagaimanapun*, *adapun*; common idioms and
    proverbs (*peribahasa*) explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*ruangan*, a light reflection on everyday life),
    dialogue that marks polite and familiar speech (*Encik*/*Puan*/*anda* against *awak*/*kau*),
    less common connectors (*sekalipun*, *seraya*, *sedangkan*, *daripada*... *lebih baik*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse;
  - ranks 5001–8000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, figurative language and idioms used more freely, and
    texts that explain a process or compare viewpoints in some depth;
  - ranks 8001–9400 (C2): the same, with the rarest words of the list, literary and technical,
    each still made clear by its context.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Lim ada di rumah? Tidak. Lim ada di taman? Tidak.*).
- **Questions at the end**: after the story, a paragraph starting `Soalan:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *kenapa*/*mengapa* and *bagaimana*, with
  fuller answers):

  ```
  Soalan:
  Apa yang Nora mahu?
  Nora mahu rumah yang besar.
  Adakah rumah itu baru?
  Ya, rumah itu baru.
  ```

- **Standard Malaysian Malay**: the narration is *bahasa Melayu baku*, neutral, correct and
  natural Malay as it is written in Malaysia today, spelled as Dewan Bahasa dan Pustaka (DBP)
  spells it — Malaysian, not Indonesian, words and spellings: *wang* (not *uang*), *kereta*
  (not *mobil*), *kerana*, *sahaja*, *fikir*, *faham*, *percuma*, *bas*, *teksi*, *polis*,
  *televisyen*, *universiti*, *klinik*, *pejabat*, *kedai*, *bilik*, *tandas*, *basikal*,
  *telefon bimbit*, *esok*, *semalam*, *sekarang*, *boleh* (not *bisa*), *mahu*, *beritahu*,
  *percaya*, *baharu* or *baru*. *di mana*, *ke mana*, *di sini*, *di rumah* (the preposition
  apart), *dibaca*, *ditulis* (the passive prefix joined); *terima kasih*; *apa-apa*, *sesiapa*;
  *-nya*, *-ku*, *-mu*, *-lah*, *-kah*, *-pun* joined to their word (*walaupun*, *adapun*, but
  *apa pun* and *siapa pun* apart), and *ku-*, *kau-* joined to a verb (*kubaca*, *kauambil*);
  reduplication with a hyphen (*kanak-kanak*, *berjalan-jalan*); *Anda* with a capital only at
  the start of a sentence; titles and kinship words with a capital when they address someone
  (*Encik mahu ke mana?*, *Terima kasih, Mak Cik.*). Spoken short forms (*tak*, *nak*, *dah*,
  *tu*, *ni*, *je*, *kat*, *camne*, *mcm*) and Manglish or slang are not used in the narration;
  dialogue among friends and family may be lightly colloquial in what any careful speaker says
  (*ya*, *oh*, *mari*, *wah*, *okey*, *lah*, *tak*, *nak*), a word or two per story.
- **Malay typography**: direct speech is set out as Malaysian books set it, each speaker's line
  a paragraph of its own in curly double quotation marks “…”, with the comma, question mark or
  exclamation mark of the quoted sentence inside the marks:

  ```
  “Hai, Nora!” kata Ali.
  “Hai,” jawab Nora. “Apa khabar?”
  ```

  The questions and answers of the final `Soalan:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*3 orang anak*, *pukul 7*, *pukul 7.30 pagi*, *tahun 1990-an*, *ke-3*),
  except a number word that is in your band: spell that one out to teach it (*lima hari*).
  Malaysia groups thousands with a comma and writes decimals with a full stop (*RM25,000*,
  *2.5 kilogram*), as the app's reader reads them aloud. `Soalan:` is the lesson's layout, not one
  of its words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content

No topic rules (user decision of 2026-11-13; see `tayra-writers/WRITER-BRIEF.md` step 4): any
normal everyday, cultural or story topic is fine, Hari Raya, Chinese New Year and Deepavali
included. Avoid only explicit sexual content, slurs used as insults and gratuitous gore. Teach
ordinary words in their main sense. Days of the week, months and language names are everyday
words, but the list has none of them (see below).

## The file

Write `tools/courses/ms/<id>.json`:

```json
{
  "id": "ms-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Malay, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places, brands, days and months used in the texts, without clitics"],
  "lessons": [
    {
      "title": "Story title in Malay",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 9400 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ms/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *rumahnya* for *rumah*, *dibaca* for *baca*,
  *kanak-kanak* for *kanak*). Reuse earlier lessons' new words too. `newWords` holds the word as
  the list writes it, without clitics.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a spoken short form, an abbreviation, a list error such as a name, a foreign
  word or a form of a word already taught); the checker's last line lists the band words not
  taught yet.

Things to know about the Malay list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ms/wordlist.tsv`
  (a copy of the app's `ms.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from
  wordfreq, film subtitles and Malaysian web pages, and only words with an entry in the ms-en
  dictionary pack are in it.
- **The checker reads Malay as it reads Indonesian.** It takes the clitics off a word before it
  looks it up: the particles *-lah*, *-kah*, *-pun*, then the possessives *-nya*, *-ku*, *-mu*,
  and the proclitics *ku-* and *kau-*. *rumahnya*, *rumahku*, *dialah*, *duduklah*, *adakah*
  count as *rumah*, *dia*, *duduk*, *ada*. A word that is an entry of its own keeps its own rank
  (*sebenarnya* 229, *biasanya*, *apakah* 335, *walaupun* 223).
- **Reduplication.** A reduplicated word is one word, counted as its first half: *anak-anak* and
  *anak-anaknya* are *anak*, *kanak-kanak* is *kanak* (244), *berjalan-jalan* is *berjalan*.
  The reader cuts the halves apart and shows each as the same word, so the learner sees what the
  checker counts. A reduplicated word with a meaning of its own (*tiba-tiba*, *hati-hati*,
  *kira-kira*, *masing-masing*, *kadang-kadang*, *rama-rama*) counts at its half's rank (*tiba*
  409, *hati* 135, *kira* 243, *masing* 405, *kadang* 692): *masing* and *kadang* are taught
  through *masing-masing* and *kadang-kadang*, but *tiba*, *hati* and *kira* only through their
  own sense (*tiba di rumah*, *hati yang baik*, *saya kira*). The ending after a number
  (*1990-an*, *ke-3*) is not a word.
- **Affixes.** The list files a root's verb forms under the root: *meN-*, *di-*, *ber-*,
  *memper-*/*diper-*, with or without *-kan*/*-i*, and *-kan*/*-i* alone (*guna*: *menggunakan*,
  *digunakan*, *gunakan*; *ambil*: *diambil*, *ambilkan*; *bantu*: *membantu*, *dibantu*; *henti*:
  *berhenti*, *menghentikan*), and *se-* with a time or
  measure word (*sehari*, *seminggu*, *setahun* under *hari*, *minggu*, *tahun*). Nouns and
  adjectives made with *ke-an*, *pe-*, *pe-an*, *per-an*, *-an* and *ter-* are entries of their own
  (*kerajaan*, *makanan* 298, *pekerjaan*, *perjalanan*, *pelajar* 169, *terakhir*, *tersenyum*,
  *tertentu*), and so are the *ber-* adjectives (*berbahaya*, *berkualiti*) and words whose affix
  is no voice of a root (*berani*, *mengaku* "to admit", *menang*, *mengerti*). The bands up to
  1000 were pinned before this rule, so there some voice forms are entries of their own (*menjadi*,
  *lakukan*, *berkata*, *mengajar*, *menjelaskan*, *bertanya*, *selamatkan*); past 1000 the root is
  the entry (*bantu* 1003, *henti* 1004). A word filed under its root still counts at the root's
  rank, but it is not that root: never teach a headword through such a word.
- **Never teach a form of an already taught word.** A verb's voice forms are one word: its *meN-*
  form, its bare form in commands and object-focus sentences, its *di-* passive and its *-kan*/*-i*
  forms. A root past 1000 whose voice form was taught up to 1000 is a form of that taught word
  (*bantu* after *membantu*, *henti* after *berhenti*): skip it. Up to
  1000 the list keeps some as two entries (*lakukan* 78 holds *melakukan*; *beri* and *memberi*
  and *berikan*; *tanya* and *bertanya*): the first one reached was taught. A root or a derived
  word with a meaning of its own is a word of its own: *jadi* "so" and *menjadi* "to become",
  *pelajar* and *belajar*. Spelling variants are forms too: *baharu* 624 after *baru*, *fasal* and
  *pasal*, *emak* and *mak*.
- **Spoken forms in the list.** The band words *tak* (6), *nak* (35), *dah* (45) and *tu* (83) are
  the spoken forms of *tidak*, *hendak*, *sudah* and *itu*: skipped as band words (they may stand in
  light dialogue). Abbreviations are filed as forms of their word (*yg*, *dgn*, *sy*, *org*,
  *sgt*, *mcm*, *je*/*aje* under *sahaja*): a new word must appear as itself or a true form,
  never through such a form.
- **Entries that hold other words.** *apabila* 70 holds *bila* "when?" (a question with *bila*
  counts for *apabila*, and teaches it only in the sense "when"); *sahaja* 73 holds *saja*;
  *orang* 19 holds *seorang*; *buah* 87 holds *sebuah*; *ekor* holds *seekor*; *emak* 158 holds
  *mak*; *baharu* holds *bahru* and *bharu*. *kuala* 199, *lumpur* 323, *pulau* 449, *pinang* 830,
  *kota* 523, *perak* 546 and *datuk* 284 are words: never put *Kuala Lumpur*, *Pulau Pinang*,
  *Kota Bharu*, *Shah Alam* (*alam* 415) or *Perak* in `names`; write *KL*, *Penang*, *George Town*, or teach the word
  (*pulau* "island") in its own sense.
- **Missing words.** The list has no days and no months (*Isnin*, *Selasa*, *Rabu*, *Khamis*,
  *Jumaat*, *Sabtu*; *Januari*... *Disember*; only *Ahad* 1564 and *minggu* 383 "week"): put
  the ones you use in `names`. Country, people and language names are not in it either (*Malaysia*,
  *India*, *Cina*, *Inggeris*): *bahasa* 268 is taught, *Inggeris* goes in `names`.
- **Names** are not in the list: people, places, shops, brands, days and months go in `names`,
  once, without clitics. A name of several words (*Mei Ling*, *George Town*) counts as one known
  word wherever it is written whole; list it whole. Never use a name that is a list word: *Siti*
  1409, *Hana*, *Ipoh* (read as *ipuh*), *Kuching* (a form of *kucing*), *Kuantan* (read as
  *antan*), *Kajang*, *Lumut* are list words. Names free of the list include *Ali*, *Aminah*,
  *Hafiz*, *Danial*, *Farid*, *Adam*, *Irfan*, *Nora*, *Salmah*, *Zul*, *Azman*, *Rosli*, *Lim*,
  *Mei Ling*, *Priya*, *Devi*, *Kumar*, *Faizal*, *Aisyah*, *Rahman*, *Yusof*, *Ahmad*, and the
  places *Seremban*, *Melaka*, *Taiping*, *Kedah*, *Langkawi*, *Johor*, *Pahang*, *Kelantan*,
  *Terengganu*, *Sabah*, *Sarawak*, *Penang*, *Klang*, *Muar*, *Miri*, *Sibu*: check every new one
  with `rank.py`.
- The list's form lists are noisy (spoken spellings such as *ape*, *pon*, *mmg*, *kalo*;
  abbreviations; Indonesian words such as *mau*, *bisa*, *gak* where they are common in web
  text); a new word must appear as itself or a true form. Some entries are names, English words
  or Indonesian spellings: skip them. Loanwords that Malay uses as its own (*komputer*,
  *internet*, *video*, *filem*, *bas*) are taught like any word.
