# Writing a Bengali frequency course

Each course teaches one band of 100 words from the Bengali word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `bn-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 10,000 words: the last course is
`bn-mini-10000`.

## The list

The list is counted from wordfreq's Bengali (news, web text, Wikipedia, subtitles) by
`tools/frequency_bengali.py`: every corpus word is read back to its dictionary form in the
`bn-en` pack (a noun's plain form, a verb's verbal noun), and its forms are listed after it. Words
the pack lacks are found in the corpus itself, with their case and classifier forms (চারপাশ,
ভিড়, ছোটবেলা, শেয়ার, বিশ্বকাপ); names of people, places and parties are left out. So:

- **The ranks lean to the news.** Words of reports rank high (সরকার 70, পুলিশ 95, নির্বাচন 96,
  জেলা 133, উপজেলা 176), some everyday words lower than in speech (বাড়ি 157, বই 277, খুশি 698).
  Write everyday stories anyway: a news word goes into an ordinary scene (a police officer who helps,
  a class election, a district bus).
- **Verbs are listed by the verbal noun** (করা, যাওয়া, দেওয়া, খাওয়া, চালানো), with all their forms
  (করি, করে, করছে, করেছিল, করব, করুন, গেছে, গিয়ে); `newWords` holds the headword as the band has it.
  আছে "is, there is" is a word of its own with ছিল, ছিলাম; রয়েছে is listed as রওয়া; হিসেবে "as" as
  হিসেব. A noun's case, classifier and plural forms (দেশের, দেশে, দেশটি, দেশগুলো, ছেলেরা, মেয়েদের)
  and the pronouns' cases (আমার, তাকে, তাঁর, আপনাকে) are its forms.
- **Spelling variants are folded**: বেশি is listed under বেশী, সরকারি under সরকারী, চাকরি under চাকরী,
  ভাল under ভালো, ঘন্টা under ঘণ্টা, and every other ি / ী spelling under the one the pack has
  (শ্রেণি, শ্রেণির under শ্রেণী; দাদি under দাদী; নানি under নানী), as are common slips (আক্রমন
  under আক্রমণ, খুজে under খোঁজা). Write the standard (reformed) spelling: বেশি, সরকারি, চাকরি,
  ভালো, ঘণ্টা, তৈরি, দাবি, শ্রেণি, দাদি, কোনো, এখনও, আরও; teach the headword the band gives.
- **Ranks 1–1000 keep the order they were written with**; the words the rebuilt list added come
  after them (শেয়ার 1002, বিশ্বকাপ 1003, ঈদ 1020, পূজা 1221, চারপাশ 1489, ভিড় 1779, ছোটবেলা
  2329, বৈশাখ 2654, দাদী 3968, বেলুন 4250, বড়দিন 4614, নানী 5958). Months and festivals are words
  (বৈশাখ, ঈদ, বড়দিন).
- **কি and কী are two words**: কি (rank 29) asks a yes/no question (*তুমি কি আসবে?*); কী (228) is
  "what" (*এটা কী?*). Never write কি for "what". কোন "which" (32) and কোনো "any" (142) are also two.
- **List noise**: a few names and their spellings rank as words (ঢাকা also "to cover", শেখ, রহমান,
  হক, লীগ, সিলেট, লন্ডন), and some short words collect stray forms (মে "May", রে, নাই). Teach a
  place name only as the place, never through a person; skip the rest.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *সুমন কিছু টাকা চায়। কিন্তু তার কাছে টাকা নেই। তাই সে কাজ চায়।*
  Everyday life where Bengali is spoken: Bangladesh (Dhaka, Chattogram, Khulna, Rajshahi, Sylhet,
  Barishal, Rangpur, Mymensingh, Cox's Bazar, Bogura, Cumilla; the Padma, Meghna and Jamuna, the
  Sundarbans, tea gardens, villages and rice fields) and West Bengal and Tripura (Kolkata,
  Shantiniketan, Darjeeling, Siliguri, Agartala); and now and then London, New York, Dubai or
  Kuala Lumpur, where Bengali speakers live: home, family, food, shopping, work, school, the town,
  health, free time, travel, festivals (Pohela Boishakh, Eid, Durga Puja) and the stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  সে / তিনি / তারা is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb (*রিনা চা খায়।*), the verb last. No copula in the present for "X is Y"
  (*ঢাকা একটি শহর। বাড়িটা বড়।*); আছে for "there is / has" (*আমার একটি বই আছে*), নেই for its
  negative, নয় (নই, নও, নন) for "is not (a) Y".
- **Politeness**: তুমি among friends, family and children; আপনি to elders, strangers, in shops and
  offices; তুই only between close friends or to a small child, and sparingly. A respected adult is
  তিনি with the honorific verb (*তিনি বলেন, তিনি আসেন*) all through the story; a child or a friend is
  সে (*সে বলে*). Keep a character's form the same in narration and questions.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the simple present (*করি, করো, করে, করেন*); আছে and ছিল; নেই and নয়; the genitive
    *-র / -এর* (*রিনার, দেশের*), the objective *-কে* (*তাকে, রিনাকে*), the locative *-এ / -তে / -য়*
    (*ঢাকায়, বাড়িতে, দেশে*); the classifiers টা / টি and the plural -রা / -গুলো; postpositions with
    the genitive (*রিনার সাথে, তার কাছে, বাড়ির জন্য*); questions with কি, কে, কোন, কেন and,
    from rank 228, কী and কোথায় (390, so before that, yes/no questions); the clitics -ই "just" and
    -ও "also" (*আমিও, আজই*); the future (*করব, করবে*) where needed;
  - ranks 301–600: the simple past (*করলাম, করল, করলেন, গেল, এল*); the conjunctive participle
    that joins actions (*খেয়ে ঘুমায়, বাড়ি গিয়ে দেখে*); the infinitive (*পড়তে চায়, যেতে পারে*);
    imperatives (*এসো / আসুন, দেখো / দেখুন*); the negative -নি of the past (*করেনি, যায়নি*);
  - ranks 601–1000: the present continuous and perfect (*করছে, করেছে, গেছে*); the conditional
    participle (*বৃষ্টি হলে, টাকা পেলে*); যখন … তখন, যদি … তাহলে, যে … সে; comparison with
    *-এর চেয়ে / থেকে বেশি*; উচিত and দরকার with the genitive (*তোমার যাওয়া উচিত*);
  - ranks 1001–2000: the past continuous and past perfect (*করছিল, করেছিল*); the habitual past
    (*করত, যেত*); compound verbs (*খেয়ে ফেলা, বলে দেওয়া, উঠে পড়া*); the verbal noun with
    endings (*পড়ার জন্য, যাওয়ার আগে*); reported speech with যে and বলে;
  - ranks 2001–3000: longer joined sentences (*কারণ, তাই, যদিও, তবুও, বরং*); causatives in -আনো
    (*দেখানো, শেখানো, করানো*); relative clauses with যে / যা / যেখানে; the written passive with
    হওয়া where Bengali prose uses it (*কাজটি শেষ করা হয়েছে*);
  - ranks 3001–5000: a richer written register: the Sanskrit-derived (তৎসম) words of newspapers
    and books beside their everyday ones, made plain by context; a column, a letter, an interview,
    a short essay that weighs two sides; long sentences that stay easy to parse;
  - ranks 5001–8000 (C1): the same, with the specialised words of the sciences, the arts and public
    life, each made clear by its context;
  - ranks 8001–10000 (C2): essays, reviews and reports on any subject, the rarer words of the
    professions and of literature, each made clear by its context.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `প্রশ্ন:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  প্রশ্ন:
  রিনা কি ঢাকায় থাকে?
  হ্যাঁ, রিনা ঢাকায় থাকে।
  ```

  The header `প্রশ্ন:` is layout: the app and the checker do not count it as a word.
- **Standard colloquial Bengali (চলিত ভাষা)**: the narration, the dialogue and the questions use the
  standard written colloquial of today's books and newspapers, never the old literary সাধু forms
  (*করিয়াছিল, তাহার, যাইতেছে*) and no regional dialect (*করসি, যামু*). English loans Bengali has
  made its own (*বাস, ফোন, অফিস, স্কুল, ডাক্তার, টিভি*) are fine in Bengali script; never write
  English in Latin letters.
- **Spelling and typing**: write the reformed standard spelling (see "The list"). Type ড় ঢ় য় as the
  letter and the nukta (ড + ়), as the list and the dictionary have them: the checker flags the
  one-character forms. Never type a zero-width joiner or non-joiner (র‍্যা, ক‌রে): the reader cuts a
  word there. Write র্যা words another way, or avoid them.
- **Bengali typography**: a sentence ends with the দাঁড়ি `।`, `?` or `!` (never `.`); direct speech
  in double quotation marks, each speaker's line a paragraph of its own. Write numbers as digits,
  Western or Bengali (*৫ জন, 2024 সালে, ১০টা*): the checker counts a number and an ending written on
  it as a number, but a number word that is in your band is spelled out to teach it (*দুই জন*). A
  hyphen splits a word in two for the reader and the checker; write compounds as Bengali writes them.
- **Names are names**: Bengali has no capital letters, so a name that is also a word reads as the
  word. Never put a list word in `names`, and never use one as a character's name (no আশা "hope",
  আলো "light", শান্তি "peace", জয় "victory" as names). Common names are fine (রিনা, সুমন, তানভীর,
  নীলা, রহিম, মিতা, অনিক, তানিয়া, শিলা, অর্পিতা, সৌমিত্র); a country or city name may be taught as
  what it is; language names are everyday words: teach them (বাংলা, ইংরেজি, হিন্দি).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals, history, myths and folk tales, politics, cricket). Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense. Skip list
noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/bn/<id>.json`, the shape of `bn-mini-0100.json`:

```json
{
  "id": "bn-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["রিনা", "ঢাকা"],
  "lessons": [
    {
      "title": "story title in Bengali",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the plain form (ঢাকা, not ঢাকায়):
the checker reads its case endings and clitics (ঢাকায়, রিনার, রিনাকেও). A name of several words
(মিসেস রায়, নিউ ইয়র্ক) counts as one name where it is written whole.

## The rules, checked by `python3 tools/check_course.py tools/courses/bn/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known. A form the list lacks counts when
  `extra_forms.tsv` has it, or when it is a listed form with -ই or -ও on it (দেশেও, আজই).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
