# Writing a Lao frequency course

Each course teaches one band of 100 words from the Lao word frequency list (`bands/<rankUpTo>.txt`: index,
rank, word, its forms). Course `lo-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300. Ten
lessons, each introducing about ten of the band's words, so the course covers the band; a learner who finishes
it knows the 300 most common words. `themes.tsv` gives each course its theme. The list has 5,018 words: the
courses are `lo-mini-0100` to `lo-mini-5100`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten separate
stories on one broad everyday theme (its title), not one serial story; a lesson never needs another lesson to
make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Everyday life where Lao is spoken: Vientiane (*ວຽງຈັນ*), Luang Prabang (*ຫຼວງພະບາງ*), Pakse
  (*ປາກເຊ*), Savannakhet (*ສະຫວັນນະເຂດ*), Thakhek (*ທ່າແຂກ*), Vang Vieng (*ວັງວຽງ*), Luang Namtha
  (*ຫຼວງນ້ຳທາ*), Phonsavan (*ໂພນສະຫວັນ*), Champasak (*ຈຳປາສັກ*), Attapeu (*ອັດຕະປື*), Xam Neua (*ຊຳເໜືອ*),
  the Mekong (*ແມ່ນ້ຳຂອງ*) and its islands, the rice fields, the hills of the north and the Bolaven plateau;
  now and then Isan across the river (Nong Khai, Udon), Bangkok, Hanoi, Paris or Sydney, where Lao people live
  and travel: home, family, food, the market, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony, jokes that
  depend on wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order (subject, verb,
  object). Lao drops subjects and objects freely; before the pronouns are taught the narration simply repeats
  the name. Mind the homograph *ລາວ* 14: it is both "Laos, Lao" and the everyday "he, she"; the list files both
  under one word, and a lesson may use it either way once taught, but never both ways in one sentence.
  Much of everyday talk comes late in the list: *ຂ້ອຍ* 724, *ແມ່* 440, *ພໍ່* 504, *ລູກ* 265, the question word *ບໍ*
  1066, *ໃຜ* 977, *ຫຍັງ* 993, *ໃສ* 1379, the particles *ເດີ* 1549, *ແດ່* 1936: use them where a story needs them,
  as outside words, until their band.
- **Grammar grows with the bands**, each band using what came before. Lao words do not inflect: what grows is
  the constructions.
  - ranks 1–300: *ເປັນ*, *ແມ່ນ*, *ຢູ່*, *ມີ*; numbers with their classifier after the noun (*ແມວ 2 ໂຕ*,
    *ຄົນ 3 ຄົນ*, *ໝາກມ່ວງ 5 ໜ່ວຍ*); *ຈະ* for what is going to happen, *ແລ້ວ* for what is done, *ໄດ້* before a
    verb for the past or "got to" (*ໄດ້ໄປ*) and after one for "can" (*ໄປໄດ້*); *ບໍ່*; the direction verbs after
    a verb (*ຂຶ້ນໄປ*, *ລົງມາ*, *ອອກໄປ*); *ໃຫ້* "give, for"; *ຂອງ* "of"; *ວ່າ* after a speech verb; the question
    words as outside words;
  - ranks 301–600: *ກຳລັງ* for what is going on, *ເຄີຍ* "ever", *ຍັງບໍ່* "not yet"; comparisons with *ກວ່າ* and
    *ທີ່ສຸດ*; requests with *ຊ່ວຍ ... ແດ່*, *ຂໍ*, *ຢ່າ*; *ເຮັດໃຫ້*; the yes/no question with *ບໍ* (not *ບໍ່*, the
    negator) and the polite particles *ເດີ*, *ແດ່*, *ເຈົ້າ* in dialogue;
  - ranks 601–1000: relative clauses with *ທີ່* and *ເຊິ່ງ*; *ຍ້ອນ*, *ເພາະ*, *ຖ້າ ... ກໍ*, *ເມື່ອ*, *ຕອນ*,
    *ກ່ອນ*, *ຫຼັງຈາກ*; *ຖືກ* for a passive; *ໃຫ້* to make or let someone do something (*ແມ່ໃຫ້ລູກໄປຕະຫຼາດ*);
    reported speech with *ບອກວ່າ*, *ຖາມວ່າ*, *ເວົ້າວ່າ*;
  - ranks 1001–2000: longer, joined sentences (*ທັງ ... ແລະ*, *ຈຶ່ງ*, *ດັ່ງນັ້ນ*, *ເຖິງແມ່ນ ... ກໍ*); *ຄົງ*,
    *ອາດຈະ*, *ຄວນ*; the nouns made with *ການ* and *ຄວາມ* (*ການເດີນທາງ*, *ຄວາມຮູ້*); a first-person narrator
    (*ຂ້ອຍ*), an e-mail, a letter or a diary entry;
  - ranks 2001–4000: a more formal written register where the text type asks for it (*ໂດຍ*, *ໃນຂະນະທີ່*,
    *ນອກຈາກນີ້*); short informative or opinion texts, everyday and calm;
  - ranks 4001 on (C1): everything before, with more abstract and nuanced content: a short essay or column, an
    interview, a portrait, a review; precise vocabulary for feelings, ideas and processes.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would: the new words three times or
  more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ຄຳຖາມ:` with 4–6 simple questions about the
  story, each followed on its own line by a full-sentence answer that reuses the story's words:

  ```
  ຄຳຖາມ:
  ນາງແກ້ວຢູ່ໃສ
  ນາງແກ້ວຢູ່ປາກເຊ
  ```

  The header `ຄຳຖາມ:` is layout: the checker does not count it as a word.
- **Standard Lao**: the narration is standard Lao as Vientiane writes it today, correct, neutral and natural; no
  Thai words or Thai spellings where Lao has its own (*ໂທລະພາບ*, not a Thai loan; *ປຶ້ມ*), no slang. Dialogue may
  be lightly colloquial in what any careful speaker says.
- **Today's spelling**: write *ຳ* as one letter (not *ໍ* + *າ*), and *ໜ*, *ໝ*, *ຫຼ* (not *ຫນ*, *ຫມ*, *ຫລ*). The
  reader reads either way, but the checker fails a lesson in the old spelling. Write *ລ* where today's spelling
  has it (*ລາຄາ*, not *ຣາຄາ*); *ຣ* only in the loanwords and names that keep it (*ຝຣັ່ງ*).
- **Lao typography**: no spaces between words; a space between sentences and where English would put a comma; no
  full stops and no commas; a question may end with *?* or not, but be consistent within a course. Direct speech
  in quotation marks, the speaker's line a paragraph of its own. Numbers in Arabic digits (*ອາຍຸ 9 ປີ*, *2
  ຊົ່ວໂມງ*): the reader shows digits as no words, and the checker counts them neither way. Never Lao digits
  (໑໒໓). *ໆ* repeats the word before it (*ເດັກນ້ອຍໆ*, *ຊ້າໆ*): the reader shows it as no word; write it straight
  after the word.
- **Names are names**: people, places, pets go in `names`. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material, and not the courses
  of the other languages either.

## Content

As WRITER-BRIEF.md step 4 says: no topic rules. Religion and festivals (*ບຸນ*, *ວັດ*, *ພະ*), history, kings,
folk tales, politics, war in history, death are all fine. Only explicit sexual content, slurs and gratuitous gore
are out. Teach ordinary words in their main sense. Days of the week, months and language names are everyday
words: teach them (*ມັງກອນ*, *ອັງກິດ*, *ຈີນ* are list words).

## The file

Write `tools/courses/lo/<id>.json`:

```json
{
  "id": "lo-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Lao, as themes.tsv writes it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and pets used in the texts"],
  "lessons": [
    {
      "title": "Story title in Lao",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/lo/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names you list in `names` count as known; digits are not counted. The checker lists
  every word outside the range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson as the reader cuts the text (see below). Reuse earlier lessons' new words too.
- **No new word before its lesson**: a lesson must not use a band word that a later lesson of the course teaches.
  Plan the lessons so the commonest little words are taught in the first lessons.
- **Length** (story and questions together, in the reader's words): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 85 of the 100 words, skipping list noise and words that are only
  parts of names; the checker's last line lists the band words not taught yet.

## The Lao list and the reader

- The courses are pinned to `tools/courses/lo/wordlist.tsv` (a copy of the app's `lo.tsv`); the bands come from
  it. The list is counted from Lao Wikipedia, the only Lao text source there is, so history, geography, sport
  and government rank high and everyday talk low: *ຂ້ອຍ* 724, *ກິນ* 567, *ແມວ* 2303,
  *ສະບາຍດີ* 2679, *ຂອບໃຈ* 2695. Everyday words outside the band are used as outside words where a story
  needs them.
- The lo-en dictionary pack is small (about 2,300 words): most list words have no entry there. Teach them in the
  sense a careful native speaker gives them first; when the pack has an entry, teach a sense it lists.
- **How the reader, and so the checker, cuts Lao.** Lao is written without spaces, so the app cuts each stretch
  of letters into words itself: into the likeliest words by the list's ranks, never inside a syllable, among the
  words of the list, the app's own Lao word list and the dictionary pack. It follows that:
  - **two words the word list has together are one word**, counted at the pair's own rank (*ຢູ່ໃນ*, *ມາຈາກ*,
    *ເຂົ້າມາ*, *ທີ່ມີ*, *ເຮັດວຽກ*, *ເດັກນ້ອຍ*). Such a pair does not count as a use of either word. Write around it
    when you need the single words.
  - **but some grammatical words always stand apart**: *ບໍ່* joins no word after it, *ວ່າ* and *ຢູ່* none before
    them: *ບໍ່ດີ*, *ຄິດວ່າ*, *ອາໄສຢູ່* are two words each.
  - **a word the lists lack is cut into syllables, together one word**: that is how a name comes out, which is why
    names go in `names`; the checker counts a listed name as one known word. A name must start and end where the
    reader's words do, or it runs into the words around it and the checker fails the lesson. Then choose another
    name or rewrite the sentence around it.
- Run `rank.py` (see INSTRUCTIONS) on a sentence to see how the reader cuts it before you build a lesson on it.
