# Writing a Khmer frequency course

Each course teaches one band of 100 words from the Khmer word frequency list (`bands/<rankUpTo>.txt`: index,
rank, word, its forms). Course `km-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300. Ten
lessons, each introducing about ten of the band's words, so the course covers the band; a learner who finishes
it knows the 300 most common words. `themes.tsv` gives each course its theme. The list has 7,559 words: the
courses are `km-mini-0100` to `km-mini-7600` (the last band has 59 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten separate
stories on one broad everyday theme (its title), not one serial story; a lesson never needs another lesson to
make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Everyday life where Khmer is spoken: Phnom Penh, Siem Reap, Battambang, Kampot, Kep, Sihanoukville,
  Kampong Cham, Takeo, Kratie, Mondulkiri, the Tonle Sap, the Mekong, the rice fields and the villages; now and
  then Surin in Thailand, Long Beach, Lowell, Paris or Melbourne, where Khmers live: home, family, food, the
  market, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony, open endings. Events in
  the order they happen. Few characters (one to three), named clearly; give each story its own names.
- **Plain narration**: third person, short sentences, the ordinary order (subject, verb, object). Khmer drops
  subjects freely; before a pronoun is taught the narration repeats the name. The pronouns and kinship words
  rank as follows: *ខ្ញុំ* 1, *អ្នក* 10, *វា* 18, *យើង* 20, *គេ* 27, *នាង* 28, *គាត់* 34, *ពួកគេ* 56, *បង* 87,
  *កូន* 107, *ប៉ា* 128, *អូន* 130, *តា* 184, *ម៉ាក់* 188, *ម្តាយ* 379, *ឪពុក* 390, *យាយ* 819; the politeness words
  *បាទ* 90 (a man's "yes"), *ចាស* 566 (a woman's), *អរគុណ* 180, *សុំទោស* 218, *សួស្តី* 337. Use them where a story
  needs them, as outside words, until their band.
- **Grammar grows with the bands**, each band using what came before. Khmer words do not inflect: what grows
  is the constructions.
  - ranks 1–300: *ជា*, *នៅ*, *មាន*; negation *មិន ... ទេ*; yes/no questions with a final *ទេ?*, and *តើ* with the
    question words (*អ្វី*, *ណា*, *នរណា*, *ប៉ុន្មាន*, *ហេតុអ្វី*); *បាន* before a verb for the past and after one for
    "can"; *នឹង* for what will happen; *កំពុង* for what is going on; *ហើយ* "already, and then"; the direction verbs
    after a verb (*ឡើង*, *ចុះ*, *ចេញ*, *ចូល*, *មក*, *ទៅ*); *ឲ្យ* "give, for"; *របស់* "of"; *ថា* after the speech verbs;
    numbers with a classifier after the noun (*កូនពីរនាក់*, *ឆ្មាមួយ*);
  - ranks 301–600: comparisons with *ជាង* and *ជាងគេ*; requests with *សូម*, *ជួយ ... ផង*, *កុំ*; *ត្រូវ*, *អាច*,
    *គួរ*; *ធ្លាប់* "ever"; *មិនទាន់* "not yet"; the politeness words in dialogue (a man says *បាទ*, a woman *ចាស*);
  - ranks 601–1000: relative clauses with *ដែល*; *ព្រោះ*, *បើ*, *ពេល*, *មុន*, *ក្រោយ*, *ដើម្បី*; *ឲ្យ* to make or
    let someone do something (*ម្តាយឲ្យកូនទៅផ្សារ*); *ត្រូវ* for an unpleasant passive; reported speech;
  - ranks 1001–2000: longer, joined sentences (*ទាំង ... ទាំង*, *ទោះបីជា*, *ដូច្នេះ*, *មិនត្រឹមតែ ... ទេ ថែមទាំង*);
    *ប្រហែល*, *ប្រហែលជា*; the nouns made with *ការ*, *សេចក្តី* and *ភាព*; a first-person narrator, a letter, a diary;
  - ranks 2001–4000: a more formal written register where the text type asks for it (*នូវ*, *នៃ*, *ដ៏*, *ដោយ*,
    *ចំពោះ*); short informative or opinion texts;
  - ranks 4001–7600 (C1): everything before, with more abstract and nuanced content: a short essay or column, an
    interview, a portrait, a review; precise vocabulary, each word made clear by its context.
- **Repetition on purpose**: repeat the new words three times or more where it reads naturally, and the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `សំណួរ៖` with 3–6 simple questions about the
  story, each followed on its own line by a full-sentence answer that reuses the story's words:

  ```
  សំណួរ៖
  តើចន្ថាចង់បានអ្វី?
  ចន្ថាចង់បានកាហ្វេមួយ។
  ```

  The header `សំណួរ៖` is layout: the checker does not count it as a word.
- **Standard Khmer**: the narration is standard written Khmer as it is written today, correct, neutral and
  natural; dialogue may be lightly colloquial in what any careful speaker says (*អី*, *ម៉េច*, *អត់ ... ទេ*), but no
  slang. Spell as the dictionaries do (*ឲ្យ*, not *ឱ្យ* or *អោយ*; *ម្ដេច* in narration).
- **Khmer typography**: no spaces between the words of a phrase; a space between phrases and clauses where a
  Khmer writer pauses, and before a name in a list (*គុន្ធា វណ្ណា និងចិន្ដា*); *។* ends a sentence, *?* ends a
  question, *!* an exclamation, *៖* comes before direct speech, which goes in « ». Plain text: never a zero-width
  space. Numbers in Khmer digits (*១*, *២០*), which the reader shows as no words; *ៗ* repeats the word before it
  (*កូនៗ*) and is no word either.
- **Names are names**: place names, words for peoples, nationalities and languages count as names (*ខ្មែរ*,
  *កម្ពុជា*, *ភ្នំពេញ* are not in the list at all); a band word that is a name is skipped. Days of the week and
  the months are everyday words: teach them. Never use a list word as a character's name: *សុភា*, *តារា*,
  *បុប្ផា*, *វាសនា*, *សុផល* are list words. Given names the dictionary pack knows are the safest, as the reader
  reads them as one word: *សុខា*, *វណ្ណា*, *ណារី*, *ពិសិដ្ឋ*, *ផល្លា*, *សុធា*, *ចិន្ដា*, *គុន្ធា*, *ចន្ថា*, *ជាតា*,
  *និមល*, *ភិរុណ*, *រិទ្ធិ*, *វុឌ្ឍី*, *សារិទ្ធ*, *សុផាត*, *ឈួន*.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Monks, pagodas, Pchum Ben and Khmer New Year, the kings of
Angkor, history, love, alcohol, crime stories and the rest are all fine. Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense, as the km-en dictionary
gives it.

## The file

Write `tools/courses/km/<id>.json`:

```json
{
  "id": "km-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Khmer, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and pets used in the texts"],
  "lessons": [
    {
      "title": "Story title in Khmer",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/km/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names in `names` count as known; digits are not counted.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson as the reader cuts the text (see below).
- **No new word before its lesson**: a lesson must not use a band word that a later lesson of the course teaches.
- **Length** (story and questions together, in the reader's words): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line lists the band
  words not taught yet.

## How the reader, and so the checker, cuts Khmer

Khmer is written without spaces between words, so the app cuts each stretch of letters itself: into its
likeliest words, by how common the list says they are, among the words of the list and of the km-en dictionary
pack (a word the list lacks counts as rare), never inside a syllable (not before a vowel sign, nor after a
coeng). A consonant alone is no word; letters no word covers stay one unknown word. Subscript ដ and ត are matched alike (*សប្ដាហ៍*, *សប្តាហ៍*). The checker cuts exactly as the reader.
It follows that:

- **two words the dictionary has together are one word**, counted at the pair's own rank, and not as a use of
  either word: *នៅក្នុង* 55, *គឺជា* 77, *ទីនេះ* 88, *ពួកគេ* 56, *ពួកយើង* 261, *ទីនោះ* 203, *មកពី* 291,
  *ទៅដល់* 568, *មកដល់* 338, *ចេញទៅ* 534, *ថ្ងៃនេះ* 387, *ដូចគ្នា* 376, *ជាមួយគ្នា* 643, *ចូលចិត្ត* 307, *មើលទៅ*
  358, *នៅតែ* 268, *ទាំងពីរ* 286, *មួយទៀត* 625, *ដូច្នោះ* 715, *ពេលណា* 807, *ថ្ងៃអាទិត្យ* 3034, *ពេលល្ងាច* 3368,
  and set phrases such as *មិនអីទេ*, *ខ្ញុំឈ្មោះ*, *ទៅផ្សារ* 4563. Write around a pair when you need its words.
- **but common words are not swallowed by rarer ones**: *តើនរណា* is *តើ* + *នរណា*, *ឆ្កែបាត់* *ឆ្កែ* + *បាត់*,
  *ជាមនុស្ស* *ជា* + *មនុស្ស*. Where a cut still reads wrongly, rephrase, or put a space between the two words where a
  pause reads naturally.
- **a name must start and end where the reader's words do**, or it runs into the words around it and the checker
  fails the lesson; list every name in `names`.
