# Writing an Indonesian frequency course

Each course teaches one band of 100 words from the Indonesian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `id-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *Rina mau kucing. Dia ke tempat penampungan hewan. Di sana ada tiga
  kucing...* Everyday life in Indonesia: Jakarta and its commuter towns (Bogor, Depok, Bekasi,
  Tangerang), Bandung, Yogyakarta, Solo, Semarang, Surabaya, Malang, Medan, Padang, Makassar,
  Manado, Denpasar and the villages of Bali and Lombok, Lake Toba, the Dieng plateau, Mount Bromo,
  the beaches of the islands, rice fields (*sawah*) and villages (*desa*, *kampung*); markets,
  street food stalls (*warung*, *kaki lima*), *ojek* and *angkot*, the KRL commuter train; and now
  and then Kuala Lumpur, Singapore, Tokyo or Amsterdam, where Indonesian is spoken at home: home,
  family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Indonesian *dia* is "he" and "she", so say
  who *dia* is whenever it could be unclear. Give each story its own names: the learner reads the
  course in one go, and two Rinas in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; *ini*/*itu* after the noun: *rumah ini*; the adjective after the noun:
  *rumah besar*, *rumah yang besar*). Indonesian verbs have no tense: time is told by time words
  (*hari ini*, *kemarin*, *besok*, *tadi*, *nanti*) and the aspect words *sudah*, *belum*,
  *sedang*, *masih*, *akan*, *pernah*, *telah*. Up to rank 500 the stories stay in the present,
  with *sudah*, *belum*, *pernah* and *akan* where they are natural; from rank 600 a story may be
  told as past events (*Kemarin Rina pergi ke pasar. Hujan turun.*). Little dialogue; when there
  is some, short and simple. Indonesian avoids *kamu* and *Anda* with older people and strangers:
  it addresses them with a kinship word or title (*Bu*, *Pak*, *Kak*, *Nek*, *Mbak*, *Mas*) and
  uses that word again for "you" (*Ibu mau kopi?*); *kamu* and *aku* among friends and family of
  the same age and to children; *saya* and *Anda* in polite talk with strangers and in shops.
- **Growing difficulty after rank 1000**: the stories stay standalone, everyday and free of
  twists, but sentences get longer and joined (*karena*, *sehingga*, *ketika*, *setelah*,
  *sebelum*, *meskipun*, *tetapi*, *namun*, *jadi*, *agar*), dialogue can carry more of the story,
  and a story may be told by a first-person narrator or as a message, a letter or a diary entry.
  Grammar grows with the bands, each band using what came before:
  - ranks 1–300: sentences without a verb (*Rumah ini besar. Tono suami saya.*); *ada* and *tidak
    ada*; *tidak* before verbs and adjectives, *bukan* before nouns (*Saya bukan guru.*); *yang*
    (*rumah yang besar*, *yang ini*, *Siapa yang tahu?*); possession by order (*rumah Rina*) and
    the possessive endings *-ku*, *-mu*, *-nya* (*rumahku*, *namanya*); the aspect words *sudah*,
    *belum*, *masih*, *pernah*, *akan*, *sedang*; *mau*, *ingin*, *bisa*, *dapat*, *harus* +
    verb; *di*, *ke*, *dari* with places (*di rumah*, *ke Jakarta*, *dari Bogor*); questions with
    *apa*, *siapa*, *di mana*, *ke mana*, *berapa*, *kapan*, and yes/no questions opened by *Apa*
    up to rank 228 and by *Apakah* after it; numbers (*satu*, *dua*, *tiga*) and the commonest
    classifiers (*seorang*, *sebuah*); plural by context, numbers or *banyak*/*semua*, and now and
    then by reduplication (*anak-anak*). The verbs are the ones the list gives as words: a few
    *ber-* verbs (*bernama*, *bermain*, *berjalan*) and *meN-* verbs (*membuat*, *melihat*,
    *menggunakan*), taken as they are, without explaining the prefix;
  - ranks 301–600: *meN-* verbs freely (*membeli*, *mencari*, *menulis*), with the sound changes
    of the prefix (*memakai*, *menulis*, *menyapu*, *mengambil*) and the bare form in commands and
    requests (*Tolong ambil buku itu.*, *Ayo makan!*, *Jangan lupa!*, *Silakan duduk.*); *ber-*
    verbs (*berangkat*, *bekerja*, *berbicara*); the *di-* passive in simple sentences (*Kue itu
    dibuat oleh Ibu. Pintunya dibuka.*); *ter-* for "most" (*terbesar*, *termurah*) and for a state
    (*terbuka*, *tertutup*); comparatives (*lebih besar daripada*, *paling murah*, *sama besar*);
    the time (*jam tujuh*, *pukul 07.00*), days of the week and months; *sedang* for what is going
    on now; more classifiers (*seekor kucing*, *sebuah rumah*, *selembar kertas*); *-lah* to soften a
    command (*Duduklah.*);
  - ranks 601–1000: the passive with a pronoun before the verb, as Indonesian prefers it when the
    doer is a person (*Buku itu sudah saya baca. Surat ini kutulis kemarin.*); verbs in *-kan* and
    *-i* (*membelikan*, *memberikan*, *menemani*, *mendekati*), each a word of its own when its
    meaning moves away from the base (*masuk* / *memasukkan*); nouns in *ke-an*, *pe-*, *pe-an*,
    *per-an* and *-an* (*kesehatan*, *pekerjaan*, *penjual*, *perjalanan*, *makanan*), each a word
    of its own; relative clauses with *yang*; *karena*, *sehingga*, *agar*/*supaya*, *meskipun*,
    *kalau*/*jika*; reported speech with *bahwa* (*Dia bilang bahwa dia lelah.*); past events told
    with time words and *sudah*/*telah*;
  - ranks 1001–1300: *memper-* and *diper-* (*memperbaiki*, *diperbaiki*), *ke-an* verbs of what
    happens to someone (*kehujanan*, *kesiangan*, *ketinggalan*), *-nya* that makes a noun or an
    adverb (*tingginya*, *rupanya*, *sebenarnya*, *biasanya*), *se-* for "as ... as" (*setinggi
    rumah*), conditions (*kalau..., maka...*, *seandainya*), *sambil*, *sementara*, *selama*;
  - ranks 1301–1600: the indefinites (*siapa pun*, *apa pun*, *di mana pun*, *seseorang*,
    *sesuatu*, *tidak ada yang*), *ter-* of what can or cannot be done (*tidak terlihat*,
    *terbaca*), *-an* nouns of results and places (*tulisan*, *pikiran*, *lapangan*),
    *walaupun*, *padahal*, *bahkan*, *justru*, *baik... maupun*, *bukan... melainkan*;
  - ranks 1601–2000: longer passives and object-focus sentences, nominalised verbs as subjects
    (*Membaca membuat saya tenang.*), formal connectors (*oleh karena itu*, *dengan demikian*,
    *selain itu*, *akan tetapi*), *ke-* ordinals and fractions (*kedua*, *setengah*,
    *sepertiga*);
  - ranks 2001–2500: a fuller written style, with *yang* clauses inside one another, *bagi*,
    *terhadap*, *mengenai*, *berdasarkan*, *menurut*, reported speech kept as Indonesian keeps it
    (no change of tense);
  - ranks 2501–3000: a richer, more formal register: connectors such as *namun demikian*,
    *sehubungan dengan*, *mengingat*, *meskipun begitu*, *adapun*; common idioms and proverbs
    (*peribahasa*) explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (*kolom*, a light reflection on everyday life), dialogue
    that marks polite and familiar speech (*Bapak*/*Ibu*/*Anda* against *kamu*/*kau*), less common
    connectors (*kendati*, *sekalipun*, *seraya*, *alih-alih*);
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short
    essay or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse; tone and
    register chosen to suit the text type;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context, and texts that explain a process or compare
    viewpoints in some depth;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text
    gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, regional and learned words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to
  a newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one
  clear idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*Boni ada di rumah Tina? Tidak. Boni ada di rumah Nina? Tidak.*).
- **Questions at the end**: after the story, a paragraph starting `Pertanyaan:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *mengapa* and *bagaimana*, with fuller
  answers):

  ```
  Pertanyaan:
  Apa yang Rina mau?
  Rina mau rumah yang besar.
  Rina dan Andi dari mana?
  Rina dan Andi dari Bogor.
  ```

- **Standard Indonesian**: the narration is *bahasa Indonesia baku*, neutral, correct and natural
  Indonesian as written today, spelled as KBBI and the EYD/PUEBI rules spell it: *di mana*, *ke
  mana*, *di sini*, *di rumah* (the preposition apart), *dibaca*, *ditulis* (the passive prefix
  joined); *terima kasih*; *apa pun*, *siapa pun* apart, but *meskipun*, *walaupun*, *adapun*
  joined; *-nya*, *-ku*, *-mu*, *-lah*, *-kah* joined to their word, and *ku-*, *kau-* joined to a
  verb (*kubaca*, *kauambil*); reduplication with a hyphen (*anak-anak*, *berlari-lari*); *Anda*
  with a capital, and kinship words with a capital when they address someone (*Ibu mau ke mana?*,
  *Terima kasih, Pak.*). Jakarta slang and spoken forms (*gue*, *lo*, *nggak*, *enggak*, *gak*,
  *udah*, *aja*, *gimana*, *banget*, *kok*, *sih*, *dong*, *deh*, *bilang* for *berkata*, *-in* for
  *-kan*) are not used in the narration and never written as misspellings to imitate speech;
  dialogue among friends and family may be lightly colloquial in what any careful speaker says
  (*ya*, *oh*, *ayo*, *wah*, *nah*, *oke*, *kok*, *sih*, *nggak*, *aja*, *bilang*), a word or two
  per story, never Jakarta slang pronouns (*gue*, *lo*). The stories are for readers of every
  region: keep to the national standard and let a regional word (*mbak*, *mas*, *bli*, *uda*)
  stand only where its context makes it clear.
- **Indonesian typography**: direct speech is set out as Indonesian books set it, each speaker's
  line a paragraph of its own in curly double quotation marks “…”, with the comma, question mark
  or exclamation mark of the quoted sentence inside the marks:

  ```
  “Halo, Rina!” kata Andi.
  “Halo,” jawab Rina. “Apa kabar?”
  ```

  The questions and answers of the final `Pertanyaan:` block are plain lines, as above. Inside a
  sentence a dash is the en dash with spaces ( – ); hyphens inside words stay hyphens. Write
  numbers as digits (*3 anak*, *jam 7*, *pukul 07.30*, *tahun 1990-an*, *ke-3*), except a number
  word that is in your band: spell that one out to teach it (*lima hari*). Thousands take a full
  stop and decimals a comma (*Rp25.000*, *2,5 kilogram*). `Pertanyaan:` is the lesson's layout,
  not one of its words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).

## Content rules (strict)

The stories are for readers of every age, every country and every faith. Keep away from:

- **religion** in every form, of every faith: God and gods, Islam, Christianity, Hinduism,
  Buddhism and the others; mosques, churches, temples (*masjid*, *musala*, *gereja*, *pura*,
  *candi*, *kuil*, *vihara*, *klenteng*), prayer and its times, fasting, pilgrimage, religious
  teachers, scriptures and schools (*pesantren*, *mengaji*); the holidays and customs of religious
  origin, even as a date, a meal, a trip home or a gift season: Ramadan, *buka puasa*, *sahur*,
  *takjil*, Lebaran and Idul Fitri, *mudik* for Lebaran, *ketupat* as Lebaran food, *THR*, Idul
  Adha and *kurban*, Christmas (*Natal*), Easter, Nyepi, Galungan, Waisak, Imlek's temple rites;
  religious greetings and formulas (*Assalamualaikum*, *Insya Allah*, *Alhamdulillah*, *Masya
  Allah*, *Ya Allah*, *Astaga*, *Puji Tuhan*, *Om Swastiastu*): greet with *halo*, *selamat pagi*,
  *selamat siang*, *selamat malam*; *halal* and *haram*; religious dress as such (*jilbab*,
  *hijab*, *peci* as a religious sign). Birthdays, New Year's Eve and New Year, weddings
  (*pernikahan*, *resepsi*) without their rites, harvest feasts, village fairs, *gotong royong* and
  *kerja bakti*, batik, *wayang* as a craft and a stage art (not its gods), gamelan and dance are
  fine;
- **politics**, the state, elections, parties, ministers, presidents, governors and regents,
  kings, sultans and queens (fairy tales and legends included), borders, national days and
  nationalism (17 Agustus, *Hari Pahlawan*), the history of colonial rule and every country's
  disputes and conflicts; everyday civic life (a library, a school committee, the *RT*/*RW*
  neighbourhood meeting, the *kelurahan* office's opening hours, a new ID card) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  parking attendant is fine; thefts, arrests, prisons, lawyers and trials are not);
- **death**, killing, funerals, mourning, blood and injury beyond a scraped knee; natural
  disasters only as preparation and calm, never with victims;
- **vulgar or sexual words**, insults, and words whose main sense is sexual;
- **alcohol** (*bir*, *anggur* as wine, *tuak*, *arak*, bars), **gambling** (betting, lotteries,
  *togel*, *judi*, cockfights), drugs and smoking (*rokok*, *merokok*);
- **person names taught as words**: a band word that is a name, a surname, a title, a place
  name, a brand or a word made from a place or a people (*bandung*, *medan*, *jawa*, *betawi*) is
  skipped. *bahasa Indonesia*, *bahasa Jawa*, *bahasa Inggris* are taught as *bahasa*; the place
  name goes in `names`.

Skip band words that only fit these topics, and say so in your report. Those of the first 1000
ranks:

- religion: *tuhan* 160, *selam* 321 (it holds *islam*; teach it only as "to dive", through
  *menyelam*, if at all), *kasih* 132 only in *terima kasih* and *kasih sayang*, never "grace";
  *agama* 506, *jiwa* 603 only as "people, inhabitants" (*kota dengan 500.000 jiwa*), *muslim* 765,
  *dewa* 869, *doa* 956, *umat* 964, *masjid* 965, *ayat* 983; *korban* 563 (sacrifice, victim);
  *selamat* 170 only in greetings and "safe" (*selamat pagi*, *selamat jalan*, *sampai dengan
  selamat*), *semoga* 343 as a plain wish (*Semoga cepat sembuh!*);
- death, violence, crime: *mati* 323, *meninggal* 549, *kematian* 613, *tewas* 931, *membunuh* 920,
  *perang* 477, *senjata* 751, *serangan* 766, *menyerang* 812, *tembak* 899, *pasukan* 682,
  *tentara* 962, *polisi* 489, *darah* 495, *kasus* 482 (as a crime case), *bukti* 404 (only "proof"
  in plain use, *bukti pembayaran*), *luka* 525 only for a scraped knee, *hukum* 384, *pasal* 941,
  *jahat* 873 only of a fairy-tale character's mood, never crime; *pukul* 602 only for the time
  (*pukul tujuh*), never "to hit"; *menangkap* 671 only for a ball or a fish; *lawan* 846 and
  *melawan* 557 only in sport and games;
- politics and the state: *negara* 167 only as "country" in travel and geography, *pemerintah*
  267, *raja* 263 (it holds *raya*; *Jalan Raya ...* goes in `names`), *presiden* 411, *nasional*
  486, *bangsa* 605, *rakyat* 616, *partai* 679, *politik* 688, *gubernur* 733, *calon* 725 only
  as "future, to-be" (*calon guru*), *menteri* 785, *wakil* 786 only in school or a club (*wakil
  ketua kelas*), *bupati* 868, *kebijakan* 969, *kerajaan* 970, *perintah* 1000;
- vulgar, insulting, sexual: *bodoh* 831, *gila* 709 only in mild everyday use (*Wah, gila!* is
  not used); *kemaluan* (under *malu* 909) never, *malu* only as "shy, ashamed";
- slang and spoken forms, skipped as band words: *enggak* 208, *bilang* 255 (may stand in light
  dialogue), *cuma* 304, *biar* 278 only as "so that" in dialogue, *nah* 334, *oke* 600, *banget*
  767, *keren* 702, *lo* 966, *gua* 469 only as "cave", never "I"; *lah* 949 and *ku* 203 are
  clitics the checker reads on their word: skip them as band words;
- names, abbreviations and list noise: *baja* 56 (it holds *indonesia*; teach "steel" only if a
  later band needs it), *al* 363, *ons* 368 only as the market weight (*satu ons*), *km* 401, *sang*
  472 (an honorific; skip), *datuk* 491, *bandung* 515, *pt* 541, *sma* 568, *la* 614, *as* 699,
  *de* 761, *misi* 772 (it holds *permisi*), *seri* 752, *android* 884, *sia* 960 (only in
  *sia-sia*).

## The file

Write `tools/courses/id/<id>.json`:

```json
{
  "id": "id-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Indonesian, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, as themes.tsv gives it",
  "names": ["Names of people, places, brands, days and months used in the texts, without clitics"],
  "lessons": [
    {
      "title": "Story title in Indonesian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/id/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *rumahnya* for *rumah*, *dibaca* for *baca*, *anak-anak*
  for *anak*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes
  it, without clitics.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a word of the skip list above, an abbreviation, a list error such as a name, a
  foreign word or a form of a word already taught); the checker's last line lists the band words
  not taught yet.

Things to know about the Indonesian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/id/wordlist.tsv`
  (a copy of the app's `id.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it.
- **Clitics.** The checker takes the clitics off a word before it looks it up: the particles
  *-lah*, *-kah*, *-pun*, then the possessives *-nya*, *-ku*, *-mu*, and the proclitics *ku-* and
  *kau-* (*kubaca*, *kauambil*). *rumahnya*, *rumahku*, *dialah*, *duduklah*, *bukunya* count as
  *rumah*, *dia*, *duduk*, *buku*. A word that is an entry of its own keeps its own rank:
  *lainnya* 185, *akhirnya* 220, *biasanya* 211, *sebenarnya* 291, *katanya* 636, *apakah* 228,
  *meskipun* 403, *walaupun* 580, *masalah* 114, *sekolah* 200, *buku* 258, *ilmu* 465. The list
  files many clitic forms of derived words under the root (*makanannya* under *makan*,
  *perjalanannya* under *jalan*, *kesalahannya* under *salah*, *pemainnya* under *main*): the
  checker counts them for the derived entry (*makanan* 364, *perjalanan* 471, *kesalahan* 598,
  *pemain* 171), which is what they are.
- **Reduplication.** A reduplicated word is one word, counted as its first half: *anak-anak* and
  *anak-anaknya* are *anak*, *teman-temanku* is *teman*, *berlari-lari* is *berlari*,
  *sehari-hari* is *sehari* (1009), *kupu-kupu* is *kupu* (not in the list: an outside word). The reader
  cuts the halves apart and shows each as the same word, so the learner sees what the checker
  counts. A reduplicated word with a meaning of its own (*tiba-tiba* "suddenly", *hati-hati*
  "careful", *kira-kira* "about", *masing-masing* "each", *laki-laki* "man", *kadang-kadang*
  "sometimes", *sia-sia* "in vain") counts at its half's rank (*tiba* 312, *hati* 149, *kira* 519,
  *masing* 355, *laki* 262, *kadang* 540): *masing* and *laki* are taught through *masing-masing*
  and *laki-laki*, but *tiba*, *hati* and *kira* only through their own sense (*tiba di rumah*,
  *hati yang baik*, *saya kira*), never through *tiba-tiba*, *hati-hati*, *kira-kira*. The ending
  after a number (*1990-an*, *ke-3*) is not a word.
- **Affixes.** The list files most affixed forms of a verb under one entry (*membaca*, *dibaca*
  under *baca*; *membeli*, *dibeli* under *beli*), and `extra_forms.tsv` adds the *di-*
  and bare forms of the *meN-* verb entries that the list lacks (*dinilai*, *miliki*, *ikuti*,
  *nikmati*). Nouns made with *ke-an*, *pe-*, *pe-an*, *per-an* and *-an* are entries of their own
  (*kehidupan* 318, *makanan* 364, *pekerjaan* 415, *perjalanan* 471, *pertanyaan* 566,
  *pemain* 171), and so are many *ber-* and *ter-* words (*bermain* 275, *bersama* 124, *berada*
  136, *terjadi* 113, *tersebut* 69, *terlihat* 294). A word filed under its root still counts at
  the root's rank, but it is not that root: never teach a headword through such a word (*berjalan*
  "to walk" is filed under *jalan* "road", *berbagi* "to share" under *bagi* "for", *terbuka*
  under *buka*, *terutama* under *utama*, *menetap* under *tetap*: they count, but *jalan* is
  taught through *jalan*).
- **Never teach a form of an already taught word.** A verb's voice forms are one word: its *meN-*
  form (*melihat*), its bare form in commands and object-focus sentences (*lihat*, *kulihat*,
  *Lihat!*) and its *di-* passive (*dilihat*). The list often keeps two of them as entries:
  *melihat* 143 and *lihat* 163, *membuat* 83 and *buat* 84, *melakukan* 76 and *lakukan* 188,
  *memberikan* 265 and *berikan* 298, *mendapatkan* 197 and *dapatkan* 346, *menggunakan* 99 and
  *gunakan* 701, *mengatakan* 252 and *katakan* 681, *punya* 82 and *mempunyai* 432, *tahu* 97 and
  *mengetahui* 479, *ingin* 75 and *inginkan* 429, *tonton* 692 and *menonton* 1552. Teach the first one reached and skip the other
  as a form (`id-mini-0100` taught *membuat* and skipped *buat*). A root or a derived word with a
  meaning of its own is a word of its own: *kata* "word" and *mengatakan* "to say", *jalan* "road"
  and *berjalan*, *masuk* "to enter" and *memasukkan* "to put in", *ada* and *berada* "to be
  located", *milik* "property" and *memiliki* "to own", *dapat* "can" and *mendapatkan* "to get",
  *jadi* "so" and *menjadi* "to become", *main* and *bermain*, *beri* and *memberi* are one word
  (as are *memberi* and *memberikan*). When in doubt, ask whether a learner who knows the one has
  to learn the other: if not, it is a form.
- **Entries that hold other words.** Some entries hold an unrelated word, which counts at the
  entry's rank but never teaches it: *saya* 17 holds *saja* "only"; *orang* 14 holds *seorang*;
  *kaki* 320 holds *kakek* "grandfather" (an outside word below rank 320, and never a way to
  teach *kaki*); *pohon* 330 holds *mohon* "please"; *raja* 263 holds *raya* "great"; *misi* 772
  holds *permisi* "excuse me"; *mengalami* 281 holds *alami* "natural"; *emas* 459 holds *mas*;
  *kaya* 630 holds *kayak*; *dokter* 461 holds *doktor*; *bisa* 10 holds *berbisa* "venomous";
  *baja* 56 holds *indonesia*; *ikan* 657 holds *berikanlah*; *taruh* holds *taruhan* "a bet".
  Teach the entry in its own sense only. *Indonesia* is a name: put it in `names`, and the checker
  counts it as one.
- **Missing words.** The list has no days and no months (*Senin*, *Selasa*, *Rabu*, *Kamis*,
  *Jumat*, *Sabtu*; *Januari*... *Desember*; *minggu* 379 is "week", and *hari Minggu* counts it):
  put the ones you use in `names`. It also lacks *begini* (write *seperti ini*), *entah*,
  *terkadang* (added under *kadang*) and *kupu-kupu*.
- **Names** are not in the list: people, places (*Jakarta*, *Bogor*, *Bali*, *Danau Toba*),
  shops, brands, days and months go in `names`, once, without clitics. A name of several words or
  with a hyphen (*Danau Toba*, *Jalan Malioboro*, *Gunung Bromo*) counts as one known word wherever
  it is written whole; list it whole. Never use a name that is a list word: *Budi* (1791), *Siti*,
  *Dewi*, *Maya*, *Ayu*, *Bayu*, *Sari*, *Putri*, *Indah*, *Bunga*, *Intan*, *Fajar*, *Rizki*,
  *Ratna*, *Lestari*, *Ani*, *Tika*, *Gita*, *Wulan*, *Tari*... are words or names the list
  carries, and *Ari* is a form of *hari*. Names free of the list include *Rina*, *Andi*, *Dina*,
  *Tono*, *Wati*, *Agus*, *Rudi*, *Yanti*, *Lina*, *Eko*, *Dimas*, *Tina*, *Hendra*, *Joko*,
  *Udin*, *Nur*, *Retno*, *Bambang*, *Susi*, *Wayan*, *Made*, *Ketut*, *Nyoman*: check every new
  one with `rank.py`. Cities that are list words (*Bandung* 515, *Medan* 1082, *Malang* 1003,
  *Solo* 1371, *Padang* 1416, *Lombok* 2277) may stand in `names` as places, and are skipped as
  band words.
- The list's form lists are noisy (spoken spellings such as *tau*, *udah*, *gitu*, *kalo*,
  *dapet*; abbreviations such as *yg*, *dgn*, *utk*; unrelated words); a new word must appear as
  itself or a true form, never through such a form. Some entries are names, abbreviations or
  English words: skip them. Loanwords that Indonesian uses as its own (*komputer*, *internet*,
  *video*, *foto*, *film*, *bisnis*) are taught like any word.
